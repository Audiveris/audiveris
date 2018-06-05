//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  A n n o t a t i o n I n d e x                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright ©  Audiveris 2017. All rights reserved.
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
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.util.BasicIndex;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code AnnotationIndex} is a sheet-level index of all annotations.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "annotations")
public class AnnotationIndex
        extends BasicIndex<Annotation>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AnnotationIndex.class);

    public static final String FILE_NAME = "annotations.xml";

    private static JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    /** To avoid useless marshalling to disk. */
    private boolean modified = false;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AnnotationIndex} object.
     */
    public AnnotationIndex ()
    {
        super(null);
    }

    /**
     * Creates a new {@code AnnotationIndex} object.
     *
     * @param sheet underlying sheet
     */
    public AnnotationIndex (Sheet sheet)
    {
        super(sheet.getPersistentIdGenerator());
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        // ID generator
        lastId = sheet.getPersistentIdGenerator();

        //
        //        // Declared VIP IDs?
        //        List<Integer> vipIds = IntUtil.parseInts(constants.vipGlyphs.getValue());
        //
        //        if (!vipIds.isEmpty()) {
        //            logger.info("VIP glyphs: {}", vipIds);
        //            weakIndex.setVipIds(vipIds);
        //        }
        //
        //        for (Iterator<Glyph> it = iterator(); it.hasNext();) {
        //            Glyph glyph = it.next();
        //
        //            if (glyph != null) {
        //                glyph.setIndex(this);
        //
        //                if (isVipId(glyph.getId())) {
        //                    glyph.setVip(true);
        //                }
        //            }
        //        }
        //
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

    //------//
    // load //
    //------//
    public static AnnotationIndex load (SheetStub stub)
    {
        AnnotationIndex index = null;

        try {
            stub.getBook().getLock().lock();

            Unmarshaller um = getJaxbContext().createUnmarshaller();

            // Open book file system
            Path dataFile = stub.getBook().openSheetFolder(stub.getNumber()).resolve(FILE_NAME);
            logger.debug("path: {}", dataFile);

            InputStream is = Files.newInputStream(dataFile, StandardOpenOption.READ);
            index = (AnnotationIndex) um.unmarshal(is);
            is.close();

            dataFile.getFileSystem().close(); // Close book file system
            index.setModified(false);
            logger.debug("Loaded {}", dataFile);
        } catch (Exception ex) {
            logger.warn("Error unmarshalling from " + FILE_NAME, ex);
        } finally {
            stub.getBook().getLock().unlock();
        }

        return index;
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

                Jaxb.marshal(this, path, getJaxbContext());
                setModified(false);
                logger.info("Stored {}", path);
            } catch (Exception ex) {
                logger.warn("Error in AnnotationIndex.store " + ex, ex);
            }
        }
    }
//
//    //------------//
//    // getContent //
//    //------------//
//    /**
//     * Mean for JAXB marshalling only.
//     *
//     * @return collection of annotations from annotationIndex
//     */
//    @SuppressWarnings("unchecked")
//    @XmlElement(name = "annotation")
//    private ArrayList<Annotation> getContent ()
//    {
//        Collection<Annotation> col = getEntities();
//        logger.info("col@{}{}", Integer.toHexString(col.hashCode()), col);
//
//        ArrayList<Annotation> arr = new ArrayList<Annotation>(col);
//        logger.info("arr@{}{}", Integer.toHexString(arr.hashCode()), arr);
//
//        return arr;
//    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
    {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(AnnotationIndex.class);
            } catch (JAXBException ex) {
                logger.error("Jaxb error " + ex, ex);
            }
        }

        return jaxbContext;
    }
//
//    //------------//
//    // setContent //
//    //------------//
//    /**
//     * Meant for JAXB unmarshalling only.
//     *
//     * @param annotations collection of annotations to feed to the annotationIndex
//     */
//    @SuppressWarnings("unchecked")
//    private void setContent (ArrayList<Annotation> annotations)
//    {
//        logger.info("annotations@{}{}", Integer.toHexString(annotations.hashCode()), annotations);
//
//        setEntities(annotations);
//    }
}
