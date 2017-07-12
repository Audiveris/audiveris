//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S a m p l e S h e e t                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.classifier;

import org.audiveris.omr.classifier.SheetContainer.Descriptor;
import org.audiveris.omr.glyph.BasicGlyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SampleSheet} gathers samples from the same sheet.
 * <p>
 * A SampleSheet is just a container for one or several samples belonging to the same sheet.
 * <p>
 * A SampleSheet may also contain the binary run-table of the original image, thus allowing to
 * display each sample within its context.
 * Real sheet uniqueness is provided via its binary image, since several names could refer to the
 * same image.
 */
public class SampleSheet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SampleSheet.class);

    /** File name for sheet samples: {@value}. */
    public static final String SAMPLES_FILE_NAME = "samples.xml";

    /** File name for sheet tribes: {@value}. */
    public static final String TRIBES_FILE_NAME = "tribes.xml";

    /** File name for sheet image: {@value}. */
    public static final String IMAGE_FILE_NAME = "image.xml";

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Enumerations -------------------------------------------------------------------------------
    public enum ImageStatus
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** There is no recorded image for this sheet. */
        NO_IMAGE,
        /** Sheet image is available on disk. */
        ON_DISK,
        /** Sheet image is available in memory. */
        LOADED;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Full descriptor. */
    private final Descriptor descriptor;

    /** Current image status for this sheet. */
    private ImageStatus imageStatus;

    /** Optional image runTable. */
    private RunTable image;

    /** True if image is already on disk. */
    private boolean imageSaved = true;

    /** Samples gathered by shape. */
    private final EnumMap<Shape, ArrayList<Sample>> shapeMap = new EnumMap<Shape, ArrayList<Sample>>(
            Shape.class);

    /** Has this sheet been modified?. */
    private boolean modified;

    /** Tribes for this sheet. */
    private List<Tribe> tribes;

    /** Tribe being created by user. */
    private Tribe currentTribe;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SampleSheet} object.
     *
     * @param descriptor related descriptor
     */
    public SampleSheet (Descriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    /**
     * Creates a new {@code SampleSheet} object from a SampleList parameter.
     *
     * @param value      the (unmarshalled) SampleList
     * @param descriptor the related descriptor
     */
    private SampleSheet (SampleList value,
                         Descriptor descriptor)
    {
        this.descriptor = descriptor;

        for (Sample sample : value.samples) {
            Shape shape = sample.getShape();
            ArrayList<Sample> list = shapeMap.get(shape);

            if (list == null) {
                shapeMap.put(shape, list = new ArrayList<Sample>());
            }

            list.add(sample);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Delete from disk the samples, tribes and image if any of a defunct sheet.
     *
     * @param descriptor  descriptor of the defunct sheet
     * @param samplesRoot root for samples (& tribes)
     * @param imagesRoot  root for images
     */
    public static void delete (SheetContainer.Descriptor descriptor,
                               Path samplesRoot,
                               Path imagesRoot)
    {
        try {
            logger.info("Deleting material for sheet {}", descriptor);

            {
                // Samples (and tribes)
                final Path folderPath = samplesRoot.resolve(descriptor.getName());

                if (Files.exists(folderPath)) {
                    FileUtil.deleteDirectory(folderPath);
                    logger.info("   Samples: deleted {} folder", folderPath);
                }
            }

            {
                // Images
                final Path folderPath = imagesRoot.resolve(descriptor.getName());

                if (Files.exists(folderPath)) {
                    FileUtil.deleteDirectory(folderPath);
                    logger.info("   Images: deleted {} folder", folderPath);
                }
            }
        } catch (Exception ex) {
            logger.error("Error deleting material for sheet " + descriptor + " " + ex, ex);
        }
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Load a SampleSheet instance from the provided path.
     *
     * @param path the source path
     * @param desc sheet descriptor
     * @return the unmarshalled instance
     * @throws IOException
     */
    public static SampleSheet unmarshal (Path path,
                                         Descriptor desc)
            throws IOException
    {
        logger.debug("SampleSheet unmarshalling {}", path);

        try {
            InputStream is = Files.newInputStream(path, StandardOpenOption.READ);
            Unmarshaller um = getJaxbContext().createUnmarshaller();
            SampleList sampleList = (SampleList) um.unmarshal(is);
            SampleSheet sampleSheet = new SampleSheet(sampleList, desc);
            logger.debug("Unmarshalled {}", sampleSheet);
            is.close();

            return sampleSheet;
        } catch (JAXBException ex) {
            logger.warn("Error unmarshalling " + path + " " + ex, ex);

            return null;
        }
    }

    //---------------//
    // getAllSamples //
    //---------------//
    /**
     * Report all samples contained in this sheet.
     *
     * @return the sheet registered samples, whatever their shape
     */
    public List<Sample> getAllSamples ()
    {
        List<Sample> allSamples = new ArrayList<Sample>();

        for (List<Sample> sampleList : shapeMap.values()) {
            allSamples.addAll(sampleList);
        }

        return allSamples;
    }

    //-----------------//
    // getCurrentTribe //
    //-----------------//
    public Tribe getCurrentTribe ()
    {
        return currentTribe;
    }

    //---------------//
    // getDescriptor //
    //---------------//
    /**
     * @return the descriptor
     */
    public Descriptor getDescriptor ()
    {
        return descriptor;
    }

    //----------//
    // getImage //
    //----------//
    /**
     * @return the image
     */
    public RunTable getImage ()
    {
        return image;
    }

    /**
     * Report current image status for this sheet.
     *
     * @param repository the containing repository
     * @return current image status
     */
    public ImageStatus getImageStatus (SampleRepository repository)
    {
        if (imageStatus == null) {
            // Already loaded?
            if (image != null) {
                return imageStatus = ImageStatus.LOADED;
            }

            // Check on disk
            if (repository.diskImageExists(descriptor)) {
                return imageStatus = ImageStatus.ON_DISK;
            }

            return imageStatus = ImageStatus.NO_IMAGE;
        }

        return imageStatus;
    }

    //------------//
    // getSamples //
    //------------//
    /**
     * Report the samples registered in this sheet for the provided shape.
     *
     * @param shape the provided shape
     * @return the sheet samples for the provided shape
     */
    public List<Sample> getSamples (Shape shape)
    {
        final List<Sample> samples = shapeMap.get(shape);

        if (samples != null) {
            return samples;
        }

        return Collections.emptyList();
    }

    //-----------//
    // getShapes //
    //-----------//
    /**
     * Report all shapes for which we have concrete samples in this sheet.
     *
     * @return the concrete shapes in this sheet
     */
    public Set<Shape> getShapes ()
    {
        return shapeMap.keySet();
    }

    //----------//
    // getTribe //
    //----------//
    /**
     * Return (perhaps after creation) the tribe based on provided best sample.
     *
     * @param best best sample
     * @return the related tribe
     */
    public Tribe getTribe (Sample best)
    {
        currentTribe = null;

        // Look for existing tribe
        for (Tribe tribe : getTribes()) {
            Sample tribeBest = tribe.getHead();

            if (((BasicGlyph) tribeBest).equals(best) && (tribeBest.shape == best.shape)) {
                currentTribe = tribe;

                break;
            }
        }

        if (currentTribe == null) {
            // Create a brand new one
            if (tribes == null) {
                tribes = new ArrayList<Tribe>();
            }

            currentTribe = new Tribe(best);
            tribes.add(currentTribe);
            logger.info("Created tribe on {} in {}", best, this);
            setModified(true);
        }

        return currentTribe;
    }

    //-----------//
    // getTribes //
    //-----------//
    /**
     * @return the tribes
     */
    public List<Tribe> getTribes ()
    {
        if (tribes != null) {
            return tribes;
        }

        return Collections.emptyList();
    }

    //------------//
    // isModified //
    //------------//
    /**
     * @return the modified value
     */
    public boolean isModified ()
    {
        return modified || !imageSaved;
    }

    //---------//
    // marshal //
    //---------//
    /**
     * Marshal this instance to disk, using 'samplesRoot' for samples & tribes and
     * "imagesRoot' for image.
     *
     * @param samplesRoot root for samples
     * @param imagesRoot  root for images
     */
    public void marshal (Path samplesRoot,
                         Path imagesRoot)
    {
        logger.debug("Marshalling {}", this);

        try {
            {
                // Samples
                final Path folderPath = samplesRoot.resolve(descriptor.getName());
                Files.createDirectories(folderPath);

                final Path samplesPath = folderPath.resolve(SAMPLES_FILE_NAME);
                Jaxb.marshal(new SampleList(this), samplesPath, getJaxbContext());
                logger.info("Stored {}", samplesPath);

                // Tribes
                if (!getTribes().isEmpty()) {
                    final Path tribesPath = folderPath.resolve(SampleSheet.TRIBES_FILE_NAME);
                    new TribeList(this).marshal(tribesPath);
                }
            }

            // Binary
            if ((image != null) && !imageSaved) {
                final Path folderPath = imagesRoot.resolve(descriptor.getName());
                Files.createDirectories(folderPath);

                final Path imagePath = folderPath.resolve(IMAGE_FILE_NAME);
                Jaxb.marshal(image, imagePath, getJaxbContext());
                imageSaved = true;
                logger.info("Stored {}", imagePath);
            }
        } catch (Exception ex) {
            logger.error("Error marshalling " + this + " " + ex, ex);
        }
    }

    //----------//
    // setImage //
    //----------//
    /**
     * Register the image binary table for this sheet.
     *
     * @param image the image to set (non-null)
     * @param saved true if image already on disk
     */
    public void setImage (RunTable image,
                          boolean saved)
    {
        this.image = image;
        this.imageSaved = saved;
        imageStatus = ImageStatus.LOADED;
    }

    //-------------//
    // setModified //
    //-------------//
    /**
     * @param modified the value to assign
     */
    public void setModified (boolean modified)
    {
        this.modified = modified;
    }

    //-----------//
    // setTribes //
    //-----------//
    /**
     * @param tribes the tribes to set
     */
    public void setTribes (List<Tribe> tribes)
    {
        this.tribes = tribes;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append(getDescriptor().getName());
        sb.append("}");

        return sb.toString();
    }

    //------------------//
    // privateAddSample //
    //------------------//
    /**
     * (Package private) method to add a new sample to this SampleSheet.
     * <p>
     * <b>NOTA:</b> Do not use directly, use {@link SampleRepository#addSample(Sample, SampleSheet)}
     * instead.
     *
     * @param sample the sample to add, non-null
     */
    void privateAddSample (Sample sample)
    {
        Objects.requireNonNull(sample, "Cannot add a null sample");

        Shape shape = sample.getShape();
        ArrayList<Sample> list = shapeMap.get(shape);

        if (list == null) {
            shapeMap.put(shape, list = new ArrayList<Sample>());
        }

        list.add(sample);

        if (!sample.isSymbol()) {
            setModified(true);
        }
    }

    //---------------------//
    // privateRemoveSample //
    //---------------------//
    /**
     * (Package private) method to remove a sample from this SampleSheet.
     * <p>
     * <b>NOTA:</b> Do not use directly, use {@link SampleRepository#removeSample(Sample)} instead.
     *
     * @param sample
     */
    void privateRemoveSample (Sample sample)
    {
        Shape shape = sample.getShape();
        ArrayList<Sample> list = shapeMap.get(shape);

        if ((list == null) || !list.contains(sample)) {
            logger.warn("{} not found in {}", sample, this);

            return;
        }

        list.remove(sample);

        if (list.isEmpty()) {
            shapeMap.remove(shape);
        }

        setModified(true);
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(RunTable.class, SampleList.class);
        }

        return jaxbContext;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // SampleList //
    //------------//
    /**
     * Value class meant for JAXB.
     */
    @XmlAccessorType(value = XmlAccessType.FIELD)
    @XmlRootElement(name = "samples")
    private static class SampleList
    {
        //~ Instance fields ------------------------------------------------------------------------

        // Persistent data
        //----------------
        /** Used only to include sheet-name within the written file. */
        @XmlAttribute(name = "sheet-name")
        private final String name;

        @XmlElement(name = "sample")
        private final ArrayList<Sample> samples = new ArrayList<Sample>();

        //~ Constructors ---------------------------------------------------------------------------
        public SampleList (SampleSheet sampleSheet)
        {
            name = sampleSheet.getDescriptor().getName();

            for (List<Sample> list : sampleSheet.shapeMap.values()) {
                samples.addAll(list);
            }
        }

        // Meant for JAXB
        private SampleList ()
        {
            name = null;
        }
    }
}
