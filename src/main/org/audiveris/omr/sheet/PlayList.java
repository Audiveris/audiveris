//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P l a y L i s t                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sheet;

import static org.audiveris.omr.sheet.Sheet.INTERNALS_RADIX;
import org.audiveris.omr.sheet.ui.SplitAndMerge.BookExcerpt;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.NaturalSpec;
import org.audiveris.omr.util.Version;
import org.audiveris.omr.util.ZipFileSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>PlayList</code> handles a sequence of book excerpts that the end-user can
 * define, modify and then use to build a compound book.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "play-list")
public class PlayList
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PlayList.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    @XmlElement(name = "excerpt")
    public final List<Excerpt> excerpts = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------
    public PlayList ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // load //
    //------//
    public static PlayList load (Path sourcePath)
    {

        try {
            logger.debug("Loading playlist {}", sourcePath);

            try (InputStream is = Files.newInputStream(sourcePath, StandardOpenOption.READ)) {
                final JAXBContext ctx = getJaxbContext();
                final Unmarshaller um = ctx.createUnmarshaller();
                final PlayList playlist = (PlayList) um.unmarshal(is);

                return playlist;
            }
        } catch (IOException |
                 JAXBException ex) {
            logger.warn("Error loading playlist " + sourcePath + " " + ex, ex);

            return null;
        }
    }

    //-------//
    // store //
    //-------//
    public void store (Path targetPath)
            throws Exception
    {
        logger.debug("Storing playlist {}", targetPath);
        Jaxb.marshal(this, targetPath, getJaxbContext());
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('{');
        boolean first = true;

        for (Excerpt excerpt : excerpts) {
            if (!first) {
                sb.append(';');
            }

            sb.append(excerpt);
            first = false;
        }

        return sb.append('}').toString();
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    public static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(PlayList.class);
        }

        return jaxbContext;
    }

    //-------------------//
    // buildCompoundBook //
    //-------------------//
    /**
     * Build a compound book from a collection of book excerpts.
     *
     * @param tgtPath path to the target book file
     * @return the created compound book
     * @throws Exception if something goes wrong
     */
    public Book buildCompoundBook (Path tgtPath)
            throws Exception
    {

        Book tgtBook = null;
        Path tgtRoot = null;

        try {
            tgtBook = Book.createBook(tgtPath);
            tgtBook.getLock().lock();
            tgtRoot = ZipFileSystem.create(tgtPath);
            Version oldestVersion = Versions.CURRENT_SOFTWARE; // Lowest version found so far
            int tgtId = 0; // Stub # in target book

            for (Excerpt excerpt : excerpts) {
                logger.info("{}", excerpt);
                final Book srcBook = ((BookExcerpt) excerpt).book;
                oldestVersion = Version.minWithLabel(oldestVersion, srcBook.getVersion());
                final Path srcRoot = ZipFileSystem.open(srcBook.getBookPath());

                // Process each selected sheet of book
                final int maxId = srcBook.size();
                final List<Integer> ids = NaturalSpec.decode(excerpt.specification, false, maxId);
                for (int srcId : ids) {
                    logger.info("   Process sheet #{}", srcId);
                    final SheetStub srcStub = srcBook.getStub(srcId);
                    final SheetStub tgtStub = new SheetStub(tgtBook, ++tgtId, srcStub);
                    tgtBook.addStub(tgtStub);

                    // Copy sheet items if they exist (images, sheet)
                    final Path srcSheetPath = srcRoot.resolve(INTERNALS_RADIX + srcId);

                    if (Files.exists(srcSheetPath)) {
                        final Path tgtSheetPath = tgtRoot.resolve(INTERNALS_RADIX + tgtId);
                        logger.debug("Copying tree from {}{} to {}{}",
                                     srcBook.getBookPath(), srcSheetPath,
                                     tgtPath, tgtSheetPath);
                        FileUtil.copyTree(srcSheetPath, tgtSheetPath);

                        // Trick: file sheet#srcId.xml if any must be renamed as sheet#tgtId.xml
                        final Path tgtSheetXmlPath = tgtSheetPath.resolve(
                                srcSheetPath.getFileName() + ".xml");
                        if (Files.exists(tgtSheetXmlPath)) {
                            final Path newPath = tgtSheetPath.resolve(
                                    tgtSheetPath.getFileName() + ".xml");
                            Files.move(tgtSheetXmlPath, newPath);
                        }

                        logger.debug("Copy done");
                    } else {
                        logger.info("No {} in {}", srcSheetPath, srcBook.getBookPath());
                    }
                }

                srcRoot.getFileSystem().close();
            }

            tgtBook.setVersionValue(oldestVersion.value);
            tgtBook.storeBookInfo(tgtRoot);
        } catch (Exception ex) {
            logger.warn("Error storing " + this + " to " + tgtPath + " ex:" + ex, ex);
        } finally {
            if (tgtRoot != null) {
                try {
                    tgtRoot.getFileSystem().close();
                } catch (IOException ignored) {
                }
            }

            tgtBook.getLock().unlock();
        }

        return null;
    }
    //~ Inner Classes ------------------------------------------------------------------------------

    //---------//
    // Excerpt //
    //---------//
    /**
     * Class <code>Excerpt</code> defines a sheets selection within a container which
     * can be a Book or an Image file.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class Excerpt
    {

        /**
         * Mandatory path to the container (book or image file).
         */
        @XmlElement(name = "path")
        @XmlJavaTypeAdapter(Jaxb.PathAdapter.class)
        public Path path;

        /**
         * This string, if any, is a specification of sheets selection.
         * <p>
         * If there is no selection specification, all sheets are selected.
         */
        @XmlElement(name = "sheets-selection")
        public String specification;

        public Excerpt ()
        {
        }

        public Excerpt (Path path,
                        String specification)
        {
            this.path = path;
            this.specification = specification;
        }

        @Override
        public String toString ()
        {
            return new StringBuilder(getClass().getSimpleName())
                    .append('{')
                    .append(path)
                    .append(" spec:").append(specification)
                    .append('}').toString();
        }
    }
}
