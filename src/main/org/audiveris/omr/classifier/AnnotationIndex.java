//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  A n n o t a t i o n I n d e x                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.audiveris.omr.OMR;
import org.audiveris.omr.classifier.ui.AnnotationService;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.util.BasicIndex;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code AnnotationIndex} is a sheet-level index of all annotations.
 *
 * @author Hervé Bitteur
 */
//@XmlAccessorType(XmlAccessType.NONE)
//@XmlRootElement(name = "annotations")
public class AnnotationIndex
        extends BasicIndex<Annotation>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AnnotationIndex.class);

    public static final String FILE_NAME = "annotations.xml";

    //~ Instance fields ----------------------------------------------------------------------------
    /** To avoid useless marshalling to disk. */
    private boolean modified = false;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AnnotationIndex} object.
     */
    public AnnotationIndex ()
    {
        // The index has its own scope of IDs
        super(new AtomicInteger(0));
    }

    /**
     * Creates a new {@code AnnotationIndex} object from a flat value.
     *
     * @param value flat index value
     */
    private AnnotationIndex (AnnotationsValue value)
    {
        this();

        for (Annotation annotation : value.list) {
            insert(annotation);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // load //
    //------//
    /**
     * Unmarshal to a temporary AnnotationsValue instance, then build index from it.
     *
     * @param stub related sheet stub
     * @return the loaded index
     */
    public static AnnotationIndex load (SheetStub stub)
    {
        AnnotationIndex index = null;

        try {
            stub.getBook().getLock().lock();

            Unmarshaller um = AnnotationsValue.getJaxbContext().createUnmarshaller();

            // Open book file system
            Path dataFile = stub.getBook().openSheetFolder(stub.getNumber()).resolve(FILE_NAME);
            logger.debug("path: {}", dataFile);

            InputStream is = Files.newInputStream(dataFile, StandardOpenOption.READ);
            AnnotationsValue value = (AnnotationsValue) um.unmarshal(is);
            is.close();
            dataFile.getFileSystem().close(); // Close book file system

            index = new AnnotationIndex(value);
            index.setModified(false);
            logger.debug("Loaded {}", dataFile);
        } catch (Exception ex) {
            logger.warn("Error unmarshalling from " + FILE_NAME, ex);
        } finally {
            stub.getBook().getLock().unlock();
        }

        return index;
    }

    //-----------------//
    // filterNegatives //
    //-----------------//
    /**
     * Report the annotations that don't belong to any of the provided sets of OmrShape.
     *
     * @param shapeSets the sets of omr shapes
     * @return the annotations found
     */
    public List<Annotation> filterNegatives (EnumSet<OmrShape>... shapeSets)
    {
        final List<Annotation> negatives = new ArrayList<Annotation>(getEntities());
        final List<Annotation> positives = filterPositives(shapeSets);
        negatives.removeAll(positives);

        return negatives;
    }

    //-----------------//
    // filterPositives //
    //-----------------//
    /**
     * Report the annotations that belong to one of the provided sets of OmrShape.
     *
     * @param shapeSets the sets of omr shapes
     * @return the annotations found
     */
    public List<Annotation> filterPositives (EnumSet<OmrShape>... shapeSets)
    {
        final List<Annotation> positives = new ArrayList<Annotation>();

        for (Annotation annotation : getEntities()) {
            final OmrShape omrShape = annotation.getOmrShape();

            for (EnumSet<OmrShape> set : shapeSets) {
                if (set.contains(omrShape)) {
                    positives.add(annotation);

                    break;
                }
            }
        }

        return positives;
    }

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "annotationIndex";
    }

    //----------------//
    // initTransients //
    //----------------//
    public void initTransients (Sheet sheet)
    {
        // Declared VIP IDs?
        setVipIds(constants.vipAnnotations.getValue());

        // User annotation service?
        if (OMR.gui != null) {
            SelectionService locationService = sheet.getLocationService();
            setEntityService(new AnnotationService(this, locationService));
        }
    }

    //------------//
    // isModified //
    //------------//
    public boolean isModified ()
    {
        return modified;
    }

    //-------------//
    // setModified //
    //-------------//
    public void setModified (boolean bool)
    {
        modified = bool;
    }

    //-------//
    // store //
    //-------//
    /**
     * Build a temporary AnnotationsValue instance from index, then marshal value.
     *
     * @param sheetFolder    target sheet folder within .omr file
     * @param oldSheetFolder sheet folder in old .omr file if any
     */
    public void store (Path sheetFolder,
                       Path oldSheetFolder)
    {
        final Path path = sheetFolder.resolve(FILE_NAME);

        if (entities.isEmpty()) {
            if (oldSheetFolder != null) {
                try {
                    // Copy from old book file to new
                    Path oldPath = oldSheetFolder.resolve(FILE_NAME);
                    Files.copy(oldPath, path);
                    logger.info("Copied {}", path);
                } catch (IOException ex) {
                    logger.warn("Error in AnnotationIndex.store " + ex, ex);
                }
            }
        } else if (isModified()) {
            try {
                Files.deleteIfExists(path);

                AnnotationsValue value = new AnnotationsValue();
                value.list.addAll(entities.values());
                Jaxb.marshal(value, path, AnnotationsValue.getJaxbContext());
                setModified(false);
                logger.info("Stored {}", path);
            } catch (Exception ex) {
                logger.warn("Error in AnnotationIndex.store " + ex, ex);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------------//
    // AnnotationsValue //
    //------------------//
    /**
     * Temporary structure meant for JAXB only.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    @XmlRootElement(name = "annotations")
    private static class AnnotationsValue
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static JAXBContext jaxbContext;

        //~ Instance fields ------------------------------------------------------------------------
        @XmlElement(name = "annotation")
        ArrayList<Annotation> list = new ArrayList<Annotation>();

        //~ Constructors ---------------------------------------------------------------------------
        /** No-arg constructor needed by JAXB. */
        public AnnotationsValue ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        static JAXBContext getJaxbContext ()
        {
            if (jaxbContext == null) {
                try {
                    jaxbContext = JAXBContext.newInstance(AnnotationsValue.class);
                } catch (JAXBException ex) {
                    logger.error("Jaxb error " + ex, ex);
                }
            }

            return jaxbContext;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String vipAnnotations = new Constant.String(
                "",
                "(Debug) Comma-separated values of VIP annotation IDs");
    }
}
