//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S a m p l e S h e e t                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier;

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

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sheet unique ID, negative for artificial sheets. */
    private final int id;

    /** Optional image runTable. */
    private RunTable image;

    /** Samples gathered by shape. */
    private final EnumMap<Shape, ArrayList<Sample>> shapeMap = new EnumMap<Shape, ArrayList<Sample>>(
            Shape.class);

    /** Has this sheet been modified?. */
    private boolean modified;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SampleSheet} object.
     *
     * @param id unique ID
     */
    public SampleSheet (int id)
    {
        this.id = id;
    }

    /**
     * Meant for JAXB.
     */
    private SampleSheet ()
    {
        id = 0;
    }

    /**
     * Creates a new {@code SampleSheet} object from a SampleList parameter.
     *
     * @param value the (unmarshalled) SampleList
     */
    private SampleSheet (SampleList value)
    {
        this.id = value.id;

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
     * @return the unmarshalled instance
     * @throws IOException
     */
    public static SampleSheet unmarshal (Path path)
            throws IOException
    {
        logger.debug("SampleSheet unmarshalling {}", path);

        try {
            InputStream is = Files.newInputStream(path, StandardOpenOption.READ);
            Unmarshaller um = getJaxbContext().createUnmarshaller();
            SampleList sampleList = (SampleList) um.unmarshal(is);
            SampleSheet sampleSheet = new SampleSheet(sampleList);
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
     * Marshal this instance to disk, using 'samplesRoot' for samples and "imagesRoot'
     * for image.
     *
     * @param samplesRoot root for samples
     * @param imagesRoot  root for images
     */
    public void marshal (Path samplesRoot,
                         Path imagesRoot)
    {
        try {
            logger.debug("Marshalling {}", this);

            // Make sure the folder exists for sheet data
            final Path folderPath = samplesRoot.resolve(Integer.toString(id));
            Files.createDirectories(folderPath);

            // Samples
            final Path samplesPath = folderPath.resolve(SAMPLES_FILE_NAME);
            Jaxb.marshal(new SampleList(this), samplesPath, getJaxbContext());
            logger.info("Stored {}", samplesPath);

            // Binary
            if (image != null) {
                final Path imagesPath = imagesRoot.resolve(Integer.toString(id));
                Files.createDirectories(imagesPath);

                final Path imagePath = imagesPath.resolve(IMAGE_FILE_NAME);
                Jaxb.marshal(image, imagePath, getJaxbContext());
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
        sb.append(" shapes:").append(shapeMap.size());

        if (image != null) {
            sb.append(" image:").append(image);
        }

        sb.append("}");

        return sb.toString();
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
    List<Sample> getSamples (Shape shape)
    {
        final List<Sample> samples = shapeMap.get(shape);

        if (samples != null) {
            return samples;
        }

        return Collections.EMPTY_LIST;
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
