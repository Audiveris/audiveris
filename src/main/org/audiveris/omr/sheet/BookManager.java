//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B o o k M a n a g e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.Main;
import org.audiveris.omr.OMR;
import org.audiveris.omr.OmrEngine;
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.PathHistory;
import org.audiveris.omr.util.SheetPath;
import org.audiveris.omr.util.SheetPathHistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class <code>BookManager</code> is a singleton which provides administrative features
 * for book instances.
 * <p>
 * It handles the collection of all book instances currently loaded, as well as the list of books
 * most recently loaded.
 * <p>
 * It handles where and how to handle inputs (images, books) and outputs (save, export, print,
 * sample).
 * <p>
 * Default input folder:
 * <ul>
 * <li>(image) input: last image input folder used
 * <li>(book) input: base
 * </ul>
 * Default output folder and file:
 * <ul>
 * <li>save: base/radix/radix.omr
 * <li>export: base/radix/radix.mxl
 * <li>print: base/radix/radix.pdf
 * <li>sample: base/radix/radix-samples.zip + base/radix/radix-images.zip
 * </ul>
 * <p>
 * The way books and sheets are exported depends on whether we allow the use of MusicXML
 * <b>Opus</b>:
 * An opus provides gathering features like an archive which is convenient when several items must
 * be exported. Without opus notion, we have to set up some ad-hoc sub-folders organization.
 * However, as of this writing, opus notion is supported by very few software products.
 * <p>
 * For example, Mozart 40th symphony as available on IMSLP web site is made of one PDF file
 * containing 49 images (sheets).
 * Its logical structure is a sequence of 4 movements:
 * <ol>
 * <li>Allegro Molto, starting on sheet #1</li>
 * <li>Andante, starting on sheet #19</li>
 * <li>Allegretto, starting on sheet #30</li>
 * <li>Allegro Assai, starting on sheet #33, system #2 (middle of the sheet)</li>
 * </ol>
 * Assuming Opus is supported, the final result would be a single opus file:
 * <p>
 * Mozart_S40.opus.mxl (with each of the 4 movements included in this opus file)
 * <p>
 * Assuming Opus is NOT supported, the final result would be something like:
 * <ol>
 * <li>Mozart_S40/
 * <li>Mozart_S40/mvt1.mxl
 * <li>Mozart_S40/mvt2.mxl
 * <li>Mozart_S40/mvt3.mxl
 * <li>Mozart_S40/mvt4.mxl
 * </ol>
 * <p>
 * We could process all the 49 sheets in memory (although this is not practically feasible) with a
 * single book, discovering the 4 movements one after the other, and finally creating one MusicXML
 * Opus containing 4 {@link Score} instances, one for each movement.
 * <p>
 * In practice, we will rather process input by physical chunks, say one sheet at a time, and
 * progressively populate the book (Book) file.
 * In a final phase, the various scores will be assembled and exported.
 * <p>
 * <img alt="Cycle img" src="doc-files/Cycle.png">
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
public class BookManager
        implements OmrEngine
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BookManager.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Map of all book instances.
     * Keyed by file path (book path or input path).
     */
    private final Map<Path, Book> books = new LinkedHashMap<>();

    /** Alias patterns. */
    private final AliasPatterns aliasPatterns = new AliasPatterns();

    /**
     * Image file history.
     * (filled only when images are successfully loaded)
     */
    private PathHistory imageHistory;

    /**
     * Book file history.
     * (filled only when books are successfully loaded or saved)
     */
    private SheetPathHistory bookHistory;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Private constructor for a singleton.
     */
    private BookManager ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // addBook //
    //---------//
    @Override
    public void addBook (Book book)
    {
        logger.debug("addBook {}", book);

        synchronized (books) {
            // Choose suitable path
            final Path path = (book.getBookPath() != null) ? book.getBookPath()
                    : book.getInputPath();

            books.put(path.toAbsolutePath(), book);
        }
    }

    //----------//
    // getAlias //
    //----------//
    /**
     * Try to retrieve an alias for the provided name, by applying registered patterns.
     *
     * @param name the full name provided
     * @return the first alias found, or null if none
     */
    public String getAlias (String name)
    {
        return aliasPatterns.getAlias(name);
    }

    //-------------//
    // getAllBooks //
    //-------------//
    @Override
    public Collection<Book> getAllBooks ()
    {
        return Collections.unmodifiableCollection(books.values());
    }

    //---------//
    // getBook //
    //---------//
    @Override
    public Book getBook (Path bookPath)
    {
        return books.get(bookPath.toAbsolutePath());
    }

    //----------------//
    // getBookHistory //
    //----------------//
    /**
     * Get access to the list of previous books.
     *
     * @return the history set of book files
     */
    public SheetPathHistory getBookHistory ()
    {
        if (bookHistory == null) {
            bookHistory = new SheetPathHistory(
                    "Book History",
                    constants.bookHistory,
                    constants.defaultBookFolder,
                    constants.historySize.getValue());
        }

        return bookHistory;
    }

    //-----------------//
    // getImageHistory //
    //-----------------//
    /**
     * Get access to the list of previous image inputs.
     *
     * @return the history set of image input files
     */
    public PathHistory getImageHistory ()
    {
        if (imageHistory == null) {
            imageHistory = new PathHistory(
                    "Input History",
                    constants.imageHistory,
                    constants.defaultImageFolder,
                    constants.historySize.getValue());
        }

        return imageHistory;
    }

    //----------//
    // loadBook //
    //----------//
    @Override
    public Book loadBook (Path bookPath)
    {
        Book book = Book.loadBook(bookPath);

        if (book != null) {
            book.setBookPath(bookPath);
            addBook(book);

            getBookHistory().remove(new SheetPath(bookPath));
        }

        return book;
    }

    //-----------//
    // loadInput //
    //-----------//
    @Override
    public Book loadInput (Path inputPath)
    {
        final Book book = new Book(inputPath);

        // Alias?
        if (AliasPatterns.useAliasPatterns()) {
            final String nameSansExt = FileUtil.getNameSansExtension(inputPath).trim();
            String alias = getAlias(nameSansExt);

            if (alias != null) {
                book.setAlias(alias);
                logger.info("Found alias: {} for {}", alias, nameSansExt);
            }
        }

        book.setModified(true);
        book.setDirty(true);

        addBook(book);

        getImageHistory().add(inputPath); // Insert in input history

        return book;
    }

    //------------//
    // removeBook //
    //------------//
    /**
     * Remove the provided book from the collection of Book instances.
     *
     * @param book        the book to remove
     * @param sheetNumber the current sheet number in book, if any, null otherwise
     * @return true if actually removed
     */
    @Override
    public synchronized boolean removeBook (Book book,
                                            Integer sheetNumber)
    {
        logger.debug("removeBook {}", book);

        final Path bookPath = book.getBookPath();

        if (bookPath != null) {
            getBookHistory().add(new SheetPath(bookPath, sheetNumber)); // Insert in history
            return books.remove(bookPath.toAbsolutePath(), book);
        }

        final Path inputPath = book.getInputPath();

        if (inputPath != null) {
            return books.remove(inputPath.toAbsolutePath(), book);
        }

        return false;
    }

    //------------//
    // renameBook //
    //------------//
    @Override
    public synchronized void renameBook (Book book,
                                         Path oldBookPath)
    {
        final Path oldPath = (oldBookPath != null) ? oldBookPath.toAbsolutePath()
                : book.getInputPath().toAbsolutePath();
        final Path newPath = book.getBookPath().toAbsolutePath();

        if (!oldPath.equals(newPath)) {
            books.put(newPath, book);
            books.remove(oldPath, book);
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // deletePaths //
    //-------------//
    /**
     * Delete the provided files and dirs.
     * If interactive mode, prompt user for confirmation beforehand.
     *
     * @param title scope indication for deletion
     * @param paths the paths to delete
     */
    public static void deletePaths (String title,
                                    List<Path> paths)
    {
        if (!paths.isEmpty()) {
            if (OMR.gui != null) {
                StringBuilder sb = new StringBuilder();

                for (Path p : paths) {
                    if (Files.isDirectory(p)) {
                        sb.append("\ndir  ");
                    } else {
                        sb.append("\nfile ");
                    }

                    sb.append(p);
                }

                if (!OMR.gui.displayConfirmation("Confirm " + title + "?\n" + sb)) {
                    return;
                }
            }

            // Do delete
            int count = 0;

            for (Path path : paths) {
                try {
                    Files.delete(path);
                    count++;
                } catch (IOException ex) {
                    logger.warn("Error deleting " + path + " " + ex, ex);
                }
            }

            logger.info("{} path(s) deleted.", count);
        }
    }

    //---------------//
    // getActualPath //
    //---------------//
    /**
     * Report the actual path to be used as target (the provided target path if any,
     * otherwise the provided default), making sure the path parent folder really exists.
     *
     * @param targetPath  the provided target path, if any
     * @param defaultPath the default target path
     * @return the path to use
     */
    public static Path getActualPath (Path targetPath,
                                      Path defaultPath)
    {
        try {
            if (targetPath == null) {
                targetPath = defaultPath;
            }

            // Make sure the target folder exists
            Path folder = targetPath.getParent();

            if (!Files.exists(folder)) {
                Files.createDirectories(folder);

                logger.info("Creating folder {}", folder);
            }

            return targetPath;
        } catch (IOException ex) {
            logger.warn("Cannot get directory for " + targetPath, ex);

            return null;
        }
    }

    //---------------//
    // getBaseFolder //
    //---------------//
    /**
     * Report the base for output folders.
     *
     * @return the output base folder
     */
    public static Path getBaseFolder ()
    {
        final Path cliOutput = Main.getCli().getOutputFolder();

        if (cliOutput != null) {
            return cliOutput;
        } else {
            return Paths.get(getBaseFolderString());
        }
    }

    //---------------------//
    // getBaseFolderString //
    //---------------------//
    /**
     * Report the (trimmed) base string for output folders.
     * <p>
     * We make sure there is no heading or trailing space in the user-provided folder name.
     *
     * @return the output base folder string
     */
    public static String getBaseFolderString ()
    {
        return constants.baseFolder.getValue().trim();
    }

    //----------------------//
    // getDefaultBookFolder //
    //----------------------//
    /**
     * Report the folder where books should be found.
     *
     * @return the latest book folder
     */
    public static String getDefaultBookFolder ()
    {
        return constants.defaultBookFolder.getValue();
    }

    //----------------------//
    // getDefaultBookFolder //
    //----------------------//
    /**
     * Report the folder where the book should be saved.
     *
     * @param book the book to store
     * @return the default book folder
     */
    public static Path getDefaultBookFolder (Book book)
    {
        // If book already has a target, use it
        if (book.getBookPath() != null) {
            return book.getBookPath().getParent();
        }

        final Path bookFolder = Main.getCli().getOutputFolder() != null //
                ? Main.getCli().getOutputFolder() //
                : constants.useInputBookFolder.isSet() //
                        ? book.getInputPath().getParent() //
                        : (useSeparateBookFolders().isSet() //
                                ? getBaseFolder().resolve(book.getRadix()) //
                                : getBaseFolder());

        try {
            if (!Files.exists(bookFolder)) {
                Files.createDirectories(bookFolder);
            }

            return bookFolder;
        } catch (IOException ex) {
            logger.warn("Cannot create folder {}", bookFolder, ex);

            return null;
        }
    }

    //-----------------------------//
    // getDefaultExportPathSansExt //
    //-----------------------------//
    /**
     * Report the file path (without extension) to which the book should be written.
     *
     * @param book the book to export
     * @return the default book path (without extension) for export
     */
    public static Path getDefaultExportPathSansExt (Book book)
    {
        if (book.getExportPathSansExt() != null) {
            return book.getExportPathSansExt();
        }

        return getDefaultBookFolder(book).resolve(book.getRadix());
    }

    //-----------------------//
    // getDefaultImageFolder //
    //-----------------------//
    /**
     * Report the folder where images should be found.
     *
     * @return the latest image folder
     */
    public static String getDefaultImageFolder ()
    {
        return constants.defaultImageFolder.getValue();
    }

    //---------------------//
    // getDefaultPrintPath //
    //---------------------//
    /**
     * Report the file path to which the book should be printed.
     *
     * @param book the book to export
     * @return the default book path for print
     */
    public static Path getDefaultPrintPath (Book book)
    {
        if (book.getPrintPath() != null) {
            return book.getPrintPath();
        }

        return getDefaultBookFolder(book).resolve(book.getRadix() + OMR.PRINT_EXTENSION);
    }

    //--------------------//
    // getDefaultSavePath //
    //--------------------//
    /**
     * Report the file path to which the book should be saved.
     *
     * @param book the book to store
     * @return the default book path for save
     */
    public static Path getDefaultSavePath (Book book)
    {
        if (book.getBookPath() != null) {
            return book.getBookPath();
        }

        return getDefaultBookFolder(book).resolve(book.getRadix() + OMR.BOOK_EXTENSION);
    }

    //--------------------//
    // getExportExtension //
    //--------------------//
    /**
     * Report the extension to use for book export, depending on the use (or not)
     * of opus and of compression.
     *
     * @return the file extension to use.
     */
    public static String getExportExtension ()
    {
        return useOpus() ? OMR.OPUS_EXTENSION
                : (useCompression() ? OMR.COMPRESSED_SCORE_EXTENSION : OMR.SCORE_EXTENSION);
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of BookManager in the application.
     *
     * @return the instance
     */
    public static BookManager getInstance ()
    {
        return LazySingleton.INSTANCE;
    }

    //-------------//
    // isMultiBook //
    //-------------//
    /**
     * Report whether we are currently handling more than one book.
     *
     * @return true if more than one book
     */
    public static boolean isMultiBook ()
    {
        return getInstance().books.size() > 1;
    }

    //----------------//
    // useCompression //
    //----------------//
    /**
     * Report whether we should use compression (to .MXL files) or not (to .XML files).
     *
     * @return true for compression
     */
    public static boolean useCompression ()
    {
        return constants.useCompression.getValue();
    }

    //--------------------//
    // useInputBookFolder //
    //--------------------//
    public static Constant.Boolean useInputBookFolder ()
    {
        return constants.useInputBookFolder;
    }

    //---------//
    // useOpus //
    //---------//
    /**
     * Tell whether Opus concept can be used
     *
     * @return true if so
     */
    public static boolean useOpus ()
    {
        return constants.useOpus.isSet();
    }

    //------------------------//
    // useSeparateBookFolders //
    //------------------------//
    /**
     * Report whether we use a separate folder for each book (as opposed to a single
     * folder for all books).
     *
     * @return true for separate folders
     */
    public static Constant.Boolean useSeparateBookFolders ()
    {
        return constants.useSeparateBookFolders;
    }

    //--------------//
    // useSignature //
    //--------------//
    /**
     * Tell whether we should inject ProxyMusic signature.
     *
     * @return true if so
     */
    public static boolean useSignature ()
    {
        return constants.defaultSigned.isSet();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean useOpus = new Constant.Boolean(
                false,
                "Should we use Opus notion for export (rather than separate files)?");

        private final Constant.Boolean useCompression = new Constant.Boolean(
                true,
                "Should we compress the MusicXML output?");

        private final Constant.Boolean useSeparateBookFolders = new Constant.Boolean(
                true,
                "Should we use a separate folder for each book?");

        private final Constant.Boolean useInputBookFolder = new Constant.Boolean(
                false,
                "Should we store book outputs next to book input?");

        private final Constant.String baseFolder = new Constant.String(
                WellKnowns.DEFAULT_BASE_FOLDER.toString(),
                "Base for output folders");

        private final Constant.String defaultBookFolder = new Constant.String(
                WellKnowns.DEFAULT_BASE_FOLDER.toString(),
                "Default folder for selection of book files");

        private final Constant.String defaultImageFolder = new Constant.String(
                WellKnowns.EXAMPLES_FOLDER.toString(),
                "Default folder for selection of image files");

        private final Constant.Boolean defaultSigned = new Constant.Boolean(
                true,
                "Should we inject ProxyMusic signature in the exported scores?");

        private final Constant.String imageHistory = new Constant.String(
                "",
                "History of images most recently loaded");

        private final Constant.String bookHistory = new Constant.String(
                "",
                "History of books most recently loaded or saved");

        private final Constant.Integer historySize = new Constant.Integer(
                "count",
                10,
                "Maximum number of files names kept in history");
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {

        static final BookManager INSTANCE = new BookManager();
    }
}
