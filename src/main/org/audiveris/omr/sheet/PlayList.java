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

import org.audiveris.omr.OMR;
import static org.audiveris.omr.sheet.Sheet.INTERNALS_RADIX;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.NaturalSpec;
import static org.audiveris.omr.util.NaturalSpec.getCounts;
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
    /**
     * The <code>excerpts</code> element is the sequence of (book or image) excerpts
     * as chosen by the user.
     */
    @XmlElement(name = "excerpt")
    public final ArrayList<Excerpt> excerpts = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------
    public PlayList ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // buildCompound //
    //---------------//
    /**
     * Build a compound .omr file from this playlist.
     * <ul>
     * <li>A Book excerpt is directly copied from disk to disk.
     * <li>An Image excerpt is marshalled from the temporary book created on-the-fly to retrieve
     * the GRAY and BINARY data from the image.
     * </ul>
     * <p>
     * If the compound book is needed, it can be unmarshalled from the created .omr file.
     *
     * @param tgtPath path to the target book file
     */
    public void buildCompound (Path tgtPath)
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

                final int maxId = srcBook.size();
                final List<Integer> ids = NaturalSpec
                        .decode(excerpt.specification, false, maxId);

                // Two possibilities:
                // - Image (non-saved) book: we marshal from memory to target zip
                // - Standard  (saved) book: we copy directly from srcRoot zip system
                final Path srcRoot = (srcBook.getBookPath() == null) ? null
                        : ZipFileSystem.open(srcBook.getBookPath());

                for (int srcId : ids) {
                    final SheetStub srcStub = srcBook.getStub(srcId);
                    logger.info("   Processing {}", srcStub);
                    srcStub.reachStep(OmrStep.BINARY, false);

                    final SheetStub tgtStub = new SheetStub(tgtBook, ++tgtId, srcStub);
                    tgtBook.addStub(tgtStub);

                    if (srcRoot == null) {
                        // Marshal
                        final Sheet srcSheet = srcStub.getSheet();
                        final Path tgtSheetPath = tgtRoot.resolve(INTERNALS_RADIX + tgtId);
                        logger.info("   Marshalling {} as {}", srcSheet, tgtSheetPath);
                        srcSheet.store(tgtSheetPath, null);
                    } else {
                        // Copy all sheet items that exist (images, sheet)
                        final Path srcSheetPath = srcRoot.resolve(INTERNALS_RADIX + srcId);

                        if (Files.exists(srcSheetPath)) {
                            final Path tgtSheetPath = tgtRoot.resolve(INTERNALS_RADIX + tgtId);
                            logger.info("Copying tree from {}{} to {}{}",
                                        srcBook.getBookPath(), srcSheetPath,
                                        tgtPath, tgtSheetPath);
                            FileUtil.copyTree(srcSheetPath, tgtSheetPath);

                            // File sheet#srcId.xml, if any, must be renamed as sheet#tgtId.xml
                            final Path tgtSheetXmlPath = tgtSheetPath.resolve(
                                    srcSheetPath.getFileName() + ".xml");
                            if (Files.exists(tgtSheetXmlPath)) {
                                final Path newPath = tgtSheetPath.resolve(
                                        tgtSheetPath.getFileName() + ".xml");
                                Files.move(tgtSheetXmlPath, newPath);
                            }
                        } else {
                            logger.warn("No {} in {}", srcSheetPath, srcBook.getBookPath());
                        }
                    }
                }

                if (srcRoot == null) {
                    // Check if the (image) source book can be discarded
                    boolean srcBookModified = false;
                    for (SheetStub stub : srcBook.getStubs()) {
                        srcBookModified |= stub.isModified();
                    }
                    if (!srcBookModified) {
                        srcBook.setModified(false);
                    }
                } else {
                    srcRoot.getFileSystem().close();
                }
            }

            tgtBook.setVersionValue(oldestVersion.value);
            tgtBook.storeBookInfo(tgtRoot);
            logger.info("Compound book created as {}", tgtPath);
        } catch (Exception ex) {
            logger.warn("Error building " + this + " as " + tgtPath + " ex:" + ex, ex);
        } finally {
            if (tgtRoot != null) {
                try {
                    tgtRoot.getFileSystem().close();
                } catch (IOException ignored) {
                }
            }

            if (tgtBook != null) {
                tgtBook.getLock().unlock();
            }
        }
    }

    //-------//
    // store //
    //-------//
    /**
     * Marshal this playlist to a file.
     *
     * @param targetPath path to the target file
     */
    public void store (Path targetPath)
    {
        try {
            Jaxb.marshal(this, targetPath, getJaxbContext());
            logger.info("Playlist stored as {}", targetPath);
        } catch (Exception ex) {
            logger.warn("Error storing playlist to {} {}", targetPath, ex.toString(), ex);
        }
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

    //-------------//
    // injectBooks //
    //-------------//
    /**
     * Make sure any Excerpt in this playlist is a duly populated BookExcerpt.
     */
    public void injectBooks ()
    {
        for (int i = 0; i < excerpts.size(); i++) {
            final Excerpt excerpt = excerpts.get(i);

            if (excerpt instanceof BookExcerpt) {
                continue;
            }

            // Replace excerpt with a populated BookExcerpt
            Book book = OMR.engine.getBook(excerpt.path);

            if (book == null) {
                final String ext = FileUtil.getExtension(excerpt.path);

                if (ext.equalsIgnoreCase(OMR.BOOK_EXTENSION)) {
                    // Book, to be unmarshalled from disk
                    book = OMR.engine.loadBook(excerpt.path);
                } else {
                    // Image file assumed, to be read and wrapped in a book
                    book = OMR.engine.loadInput(excerpt.path);
                    book.createStubs();
                }
            }

            final BookExcerpt bookExcerpt = BookExcerpt.create(book, excerpt.specification);
            excerpts.set(i, bookExcerpt);
        }
    }

    //------//
    // load //
    //------//
    /**
     * Unmarshal a playlist out of the provided source file path.
     *
     * @param sourcePath path to the source file
     * @return the unmarshalled playlist
     */
    public static PlayList load (Path sourcePath)
    {

        try {
            logger.info("Loading playlist {}", sourcePath);

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

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Excerpt //
    //---------//
    /**
     * Class <code>Excerpt</code> defines a precise selection of sheets within a container
     * file which can be either a Book file or an Image file.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class Excerpt
    {

        /**
         * This mandatory element is the path to the container file (book or image).
         * <ul>
         * <li>A path ending with <code>.omr</code> extension represents a book file.
         * <li>A path ending with a different extension, like <code>.pdf</code>,
         * <code>.tif</code>, <code>.png</code>, etc, represents an image file.
         * </ul>
         */
        @XmlElement(name = "path")
        @XmlJavaTypeAdapter(Jaxb.PathAdapter.class)
        public Path path;

        /**
         * This string, if any, is a specification of sheets selection.
         * <p>
         * For example, "1-3,5,10-12,30-" will limit the processing of this book to those sheets:
         * <ul>
         * <li>#1 through #3
         * <li>#5
         * <li>#10 through #12
         * <li>#30 through last sheet in book
         * </ul>
         * NOTA: The playlist accepts <b>valid</b> as well as <b>invalid</b> sheets.
         * <br>
         * Doing so, the resulting compound book can still contain some sheets like illustration
         * sheets that are generally considered as invalid by the OMR engine.
         * <p>
         * If this specification is null or empty, all sheets (valid or not) are selected by
         * default.
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

    //-------------//
    // BookExcerpt //
    //-------------//
    /**
     * Class <code>BookExcerpt</code> is an Excerpt with its related Book instance,
     * and augmented of bookId and counts to ease its display.
     */
    public static class BookExcerpt
            extends PlayList.Excerpt
    {

        /** Containing book. */
        public final Book book;

        /** Flag to indicate a book created on-the-fly to represent an image file. */
        public boolean isImage;

        /** Displayed book id. */
        public final String bookId;

        /** Resulting counts. */
        public String counts;

        /**
         * Creates a <code>BookExcerpt</code> initialized with provided sheets
         * specification.
         *
         * @param book          the containing book
         * @param isImage       true for a book just created upon an image
         * @param path          book related path
         * @param specification the sheets specification
         */
        public BookExcerpt (Book book,
                            boolean isImage,
                            Path path,
                            String specification)
        {
            super(path, specification);
            this.book = book;

            bookId = path.getFileName().toString();
            counts = getCounts(specification, book.size());
        }

        /**
         * Creates a <code>BookExcerpt</code> based on the provided book,
         * using its defined sheets specification.
         *
         * @param book the provided book
         * @return the book-based excerpt
         */
        public static BookExcerpt create (Book book)
        {
            return create(book, book.getSheetsSelection());
        }

        /**
         * Creates a <code>BookExcerpt</code> based on the provided book,
         * and the provided sheets specification.
         *
         * @param book          the provided book
         * @param specification the provided sheets specification
         *                      (which can be different for book own spec)
         * @return the created book excerpt
         */
        public static BookExcerpt create (Book book,
                                          String specification)
        {
            final boolean isImage = book.getBookPath() == null;

            return new BookExcerpt(book,
                                   isImage,
                                   book.getPath(),
                                   NaturalSpec.normalized(specification, book.size()));
        }

        @Override
        public String toString ()
        {
            return new StringBuilder(getClass().getSimpleName())
                    .append('{')
                    .append(book.getRadix()).append(" spec:").append(specification)
                    .append('}').toString();
        }
    }
}
