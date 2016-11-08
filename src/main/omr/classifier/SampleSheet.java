//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S a m p l e S h e e t                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.classifier;

import omr.classifier.SheetContainer.Descriptor;

import omr.glyph.BasicGlyph;
import omr.glyph.Shape;

import omr.run.RunTable;

import omr.util.Jaxb;

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
 * Real sheet uniqueness is provided via its binary image, since several names could refer to the
 * same image.
 * <p>
 * A negative ID indicates an artificial font-based SampleSheet.
 * <p>
 * A SampleSheet may also contain the binary run-table of the original image, thus allowing to
 * display each sample within its context.
 */
public class SampleSheet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            SampleSheet.class);

    /** File name for sheet samples: {@value}. */
    public static final String SAMPLES_FILE_NAME = "samples.xml";

    /** File name for sheet image: {@value}. */
    public static final String IMAGE_FILE_NAME = "image.xml";

    /** File name for sheet flocks: {@value}. */
    public static final String FLOCKS_FILE_NAME = "flocks.xml";

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sheet unique ID, negative for artificial sheets. */
    private final int id;

    /** Full descriptor. */
    private final Descriptor descriptor;

    /** Optional image runTable. */
    private RunTable image;

    /** Samples gathered by shape. */
    private final EnumMap<Shape, ArrayList<Sample>> shapeMap = new EnumMap<Shape, ArrayList<Sample>>(
            Shape.class);

    /** Has this sheet been modified?. */
    private boolean modified;

    /** Flocks for this sheet. */
    private List<Flock> flocks;

    private Flock currentFlock;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SampleSheet} object.
     *
     * @param id         unique ID
     * @param descriptor related descriptor
     */
    public SampleSheet (int id,
                        Descriptor descriptor)
    {
        this.id = id;
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
        this.id = value.id;
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
     * Delete from disk the samples and image if any of a defunct sheet
     *
     * @param descriptor  descriptor of the defunct sheet
     * @param samplesRoot root for samples
     * @param imagesRoot  root for images
     * @param flocksRoot  root for flocks
     */
    public static void delete (SheetContainer.Descriptor descriptor,
                               Path samplesRoot,
                               Path imagesRoot,
                               Path flocksRoot)
    {
        final String idStr = Integer.toString(descriptor.id);

        try {
            logger.info("Deleting material for sheet {}", descriptor);

            {
                // Samples
                final Path folderPath = samplesRoot.resolve(idStr);

                if (Files.exists(folderPath)) {
                    // Files(s)
                    final Path samplesPath = folderPath.resolve(SAMPLES_FILE_NAME);

                    if (Files.deleteIfExists(samplesPath)) {
                        logger.info("   Samples: deleted {}", samplesPath);
                    }

                    // Then folder
                    Files.delete(folderPath);
                    logger.info("   Samples: deleted {}", folderPath);
                }
            }

            {
                // Image?
                final Path imagesPath = imagesRoot.resolve(idStr);

                if (Files.exists(imagesPath)) {
                    // File(s)
                    final Path imagePath = imagesPath.resolve(IMAGE_FILE_NAME);

                    if (Files.deleteIfExists(imagePath)) {
                        logger.info("   Images: deleted {}", imagePath);
                    }

                    // Then folder
                    Files.delete(imagesPath);
                    logger.info("   Images: deleted {}", imagesPath);
                }
            }

            // Flocks?
            if (flocksRoot != null) {
                final Path folderPath = flocksRoot.resolve(idStr);

                if (Files.exists(folderPath)) {
                    // File(s)
                    final Path flocksPath = folderPath.resolve(FLOCKS_FILE_NAME);

                    if (Files.deleteIfExists(flocksPath)) {
                        logger.info("   Flocks: deleted {}", flocksPath);
                    }

                    // Then folder
                    Files.delete(folderPath);
                    logger.info("   Flocks: deleted {}", folderPath);
                }
            }
        } catch (Exception ex) {
            logger.error("Error deleting material for sheet " + descriptor + " " + ex, ex);
        }
    }

    //-----------//
    // isSymbols //
    //-----------//
    /**
     * Report whether the provided sheet ID is a font-based symbols sheet
     *
     * @param id provided sheet id
     * @return true if font-based symbols
     */
    public static boolean isSymbols (int id)
    {
        return id < 0;
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
    // getCurrentFlock //
    //-----------------//
    public Flock getCurrentFlock ()
    {
        return currentFlock;
    }

    //----------//
    // getFlock //
    //----------//
    /**
     * Return (perhaps after creation) the flock based on provided best sample.
     *
     * @param best best sample
     * @return the related flock
     */
    public Flock getFlock (Sample best)
    {
        currentFlock = null;

        // Look for existing flock
        for (Flock flock : getFlocks()) {
            Sample flockBest = flock.getBest();

            if (((BasicGlyph) flockBest).equals(best) && (flockBest.shape == best.shape)) {
                currentFlock = flock;

                break;
            }
        }

        if (currentFlock == null) {
            // Create a brand new one
            if (flocks == null) {
                flocks = new ArrayList<Flock>();
            }

            currentFlock = new Flock(best);
            flocks.add(currentFlock);
            logger.info("Created flock on {} in {}", best, this);
            setModified(true);
        }

        return currentFlock;
    }

    //-----------//
    // getFlocks //
    //-----------//
    /**
     * @return the flocks
     */
    public List<Flock> getFlocks ()
    {
        if (flocks != null) {
            return flocks;
        }

        return Collections.emptyList();
    }

    //-------//
    // getId //
    //-------//
    /**
     * @return the id
     */
    public int getId ()
    {
        return id;
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

    //------------//
    // isModified //
    //------------//
    /**
     * @return the modified value
     */
    public boolean isModified ()
    {
        return modified;
    }

    //-----------//
    // isSymbols //
    //-----------//
    /**
     * Tell whether this SampleSheet instance is based on artificial font-based symbols.
     *
     * @return true if artificial
     */
    public boolean isSymbols ()
    {
        return isSymbols(id);
    }

    //---------//
    // marshal //
    //---------//
    /**
     * Marshal this instance to disk, using 'samplesRoot' for samples, "imagesRoot'
     * for image and 'flocksRoot' for flocks.
     *
     * @param samplesRoot root for samples
     * @param imagesRoot  root for images
     * @param flocksRoot  root for flocks, perhaps null
     */
    public void marshal (Path samplesRoot,
                         Path imagesRoot,
                         Path flocksRoot)
    {
        try {
            logger.debug("Marshalling {}", this);
            // Samples
            {
                final Path folderPath = samplesRoot.resolve(Integer.toString(id));
                Files.createDirectories(folderPath);

                final Path samplesPath = folderPath.resolve(SAMPLES_FILE_NAME);
                Jaxb.marshal(new SampleList(this), samplesPath, getJaxbContext());
                logger.info("Stored {}", samplesPath);
            }

            // Binary
            if (image != null) {
                final Path folderPath = imagesRoot.resolve(Integer.toString(id));
                Files.createDirectories(folderPath);

                final Path imagePath = folderPath.resolve(IMAGE_FILE_NAME);
                Jaxb.marshal(image, imagePath, getJaxbContext());
                logger.info("Stored {}", imagePath);
            }

            // Flocks
            if ((flocksRoot != null) && !getFlocks().isEmpty()) {
                final Path folderPath = flocksRoot.resolve(Integer.toString(id));
                Files.createDirectories(folderPath);

                final Path flocksPath = folderPath.resolve(FLOCKS_FILE_NAME);
                Jaxb.marshal(new FlockList(this), flocksPath, getJaxbContext());
                logger.info("Stored {}", flocksPath);
            }
        } catch (Exception ex) {
            logger.error("Error marshalling " + this + " " + ex, ex);
        }
    }

    //-----------//
    // setFlocks //
    //-----------//
    /**
     * @param flocks the flocks to set
     */
    public void setFlocks (List<Flock> flocks)
    {
        this.flocks = flocks;
    }

    //----------//
    // setImage //
    //----------//
    /**
     * Register the image binary table for this sheet.
     *
     * @param image the image to set
     */
    public void setImage (RunTable image)
    {
        this.image = image;
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append("id:").append(id);
        sb.append(" ").append(descriptor.getName());
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
            logger.info("Added {} to {}", sample, this);
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
        logger.info("Removed {} from {}", sample, this);
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(
                    RunTable.class,
                    SampleList.class,
                    FlockList.class);
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

        @XmlAttribute(name = "id")
        private final int id;

        @XmlElement(name = "sample")
        private final ArrayList<Sample> samples = new ArrayList<Sample>();

        //~ Constructors ---------------------------------------------------------------------------
        public SampleList (int id,
                           SampleSheet sampleSheet)
        {
            this.id = id;

            for (List<Sample> list : sampleSheet.shapeMap.values()) {
                samples.addAll(list);
            }
        }

        public SampleList (SampleSheet sampleSheet)
        {
            this.id = sampleSheet.getId();

            for (List<Sample> list : sampleSheet.shapeMap.values()) {
                samples.addAll(list);
            }
        }

        // Meant for JAXB
        private SampleList ()
        {
            id = 0;
        }
    }
}
