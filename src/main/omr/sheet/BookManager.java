//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B o o k M a n a g e r                                     //
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
package omr.sheet;

import omr.Main;
import omr.OMR;
import omr.OmrEngine;
import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.Score;

import omr.script.ScriptManager;

import omr.util.FileUtil;
import omr.util.PathHistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code BookManager} is a singleton which provides administrative features
 * for book instances.
 * <p>
 * It handles the collection of all book instances currently loaded, as well as the list of books
 * most recently loaded.
 * <p>
 * It handles where and how to handle inputs (images, books, scripts) and outputs (books, exports,
 * prints and scripts).
 * <br>
 * Default input folder:<ul>
 * <li>(image) input: last image input folder used
 * <li>(book) input: base
 * <li>(script) input: base
 * </ul>
 * Default output folder and file:<ul>
 * <li>book: base/radix/radix.omr
 * <li>export: base/radix/radix.mxl
 * <li>print: base/radix/radix.pdf
 * <li>script: base/radix/radix.script.xml
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
 * Its logical structure is a sequence of 4 movements:<ol>
 * <li>Allegro Molto, starting on sheet #1</li>
 * <li>Andante, starting on sheet #19</li>
 * <li>Allegretto, starting on sheet #30</li>
 * <li>Allegro Assai, starting on sheet #33, system #2 (middle of the sheet)</li>
 * </ol>
 * <p>
 * Assuming Opus is supported, the final result would be a single opus file:
 * <blockquote>
 * <pre>
 * Mozart_S40.opus.mxl (with each of the 4 movements included in this opus file)
 * </pre>
 * </blockquote>
 * Assuming Opus is NOT supported, the final result would be something like:
 * <blockquote>
 * <pre>
 * Mozart_S40/
 * Mozart_S40/mvt1.mxl
 * Mozart_S40/mvt2.mxl
 * Mozart_S40/mvt3.mxl
 * Mozart_S40/mvt4.mxl
 * </pre>
 * </blockquote>
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

    /** The single instance of this class. */
    private static volatile BookManager INSTANCE;

    //~ Instance fields ----------------------------------------------------------------------------
    /** All book instances. */
    private final List<Book> books = new ArrayList<Book>();

    /** Alias patterns. */
    private final AliasPatterns aliasPatterns = new AliasPatterns();

    /** Image file history. (filled only when images are successfully loaded) */
    private PathHistory imageHistory;

    /** Book file history. (filled only when books are successfully loaded or saved) */
    private PathHistory bookHistory;

    /** Script file history. (filled only when scripts are successfully loaded or saved) */
    private PathHistory scriptHistory;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Private constructor for a singleton.
     */
    private BookManager ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // deletePaths //
    //-------------//
    /**
     * Delete the provided files & dirs.
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
    public List<Book> getAllBooks ()
    {
        return Collections.unmodifiableList(books);
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
        return Paths.get(getBaseFolderString());
    }

    //---------------------//
    // getBaseFolderString //
    //---------------------//
    /**
     * Report the base string for output folders.
     *
     * @return the output base folder string
     */
    public static String getBaseFolderString ()
    {
        return constants.baseFolder.getValue();
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

    //----------------------//
    // getDefaultScriptPath //
    //----------------------//
    /**
     * Report the default path where the script should be written to
     *
     * @param book the containing book
     * @return the default path for saving the script
     */
    public static Path getDefaultScriptPath (Book book)
    {
        // If book already has a target, use it
        if (book.getScriptPath() != null) {
            return book.getScriptPath();
        }

        final Path folder = getTargetFolder(book);

        return folder.resolve(book.getRadix() + OMR.SCRIPT_EXTENSION);
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class.
     *
     * @return the single instance
     */
    public static BookManager getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new BookManager();
        }

        return INSTANCE;
    }

    //------------------//
    // getScriptHistory //
    //------------------//
    /**
     * Get access to the list of previous scripts.
     *
     * @return the history set of script files
     */
    public PathHistory getScriptHistory ()
    {
        if (scriptHistory == null) {
            scriptHistory = new PathHistory(
                    "Script History",
                    constants.scriptHistory,
                    null,
                    constants.historySize.getValue());
        }

        return scriptHistory;
    }

    //-------------------//
    // getSeparateFolder //
    //-------------------//
    /**
     * Report the separate folder for the provided book
     *
     * @param book provided book
     * @return separate folder, or null
     */
    public static Path getSeparateFolder (Book book)
    {
        if (useSeparateBookFolders()) {
            // Define target based on base + book folder and book name
            Path folder = getBaseFolder().resolve(book.getRadix());

            try {
                if (!Files.exists(folder)) {
                    Files.createDirectories(folder);
                }

                return folder;
            } catch (IOException ex) {
                logger.warn("Cannot create {}", folder, ex);
            }
        }

        return null;
    }

    //----------//
    // loadBook //
    //----------//
    @Override
    public Book loadBook (Path bookPath)
    {
        Book book = BasicBook.loadBook(bookPath);

        if (book != null) {
            addBook(book);

            getBookHistory().add(bookPath); // Insert in book history
        }

        return book;
    }

    //-----------//
    // loadInput //
    //-----------//
    @Override
    public Book loadInput (Path path)
    {
        final Book book = new BasicBook(path);

        // Alias?
        if (AliasPatterns.useAliasPatterns()) {
            final String nameSansExt = FileUtil.getNameSansExtension(path);
            String alias = getAlias(nameSansExt);

            if (alias != null) {
                book.setAlias(alias);
                logger.info("Found alias: {} for {}", alias, nameSansExt);
            }
        }

        book.setModified(true);
        addBook(book);

        getImageHistory().add(path); // Insert in input history

        return book;
    }

    //------------//
    // removeBook //
    //------------//
    /**
     * Remove the provided book from the collection of Book instances.
     *
     * @param book the book to remove
     * @return true if actually removed
     */
    @Override
    public synchronized boolean removeBook (Book book)
    {
        logger.debug("removeBook {}", book);

        return books.remove(book);
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

    //------------------------//
    // useSeparateBookFolders //
    //------------------------//
    /**
     * Report whether we use a separate folder for each book (as opposed to a single
     * folder for all books).
     *
     * @return true for separate folders
     */
    public static boolean useSeparateBookFolders ()
    {
        return constants.useSeparateBookFolders.isSet();
    }

    //----------------//
    // getBookHistory //
    //----------------//
    /**
     * Get access to the list of previous books.
     *
     * @return the history set of book files
     */
    public PathHistory getBookHistory ()
    {
        if (bookHistory == null) {
            bookHistory = new PathHistory(
                    "Book History",
                    constants.bookHistory,
                    null,
                    constants.historySize.getValue());
        }

        return bookHistory;
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

        if (useSeparateBookFolders()) {
            // Define folder based on base + book radix
            Path bookFolder = getBaseFolder().resolve(book.getRadix());

            try {
                if (!Files.exists(bookFolder)) {
                    Files.createDirectories(bookFolder);
                }

                return bookFolder;
            } catch (IOException ex) {
                logger.warn("Cannot create {}", bookFolder, ex);

                return null;
            }
        } else {
            // Use global base folder
            return getBaseFolder();
        }
    }

    //--------------------//
    // getDefaultBookPath //
    //--------------------//
    /**
     * Report the file path to which the book should be saved.
     *
     * @param book the book to store
     * @return the default book path for save
     */
    public static Path getDefaultBookPath (Book book)
    {
        // If book already has a target, use it
        if (book.getBookPath() != null) {
            return book.getBookPath();
        }

        final Path bookFolder = getDefaultBookFolder(book);

        return bookFolder.resolve(book.getRadix() + OMR.BOOK_EXTENSION);
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

        // File?
        final Path file = Main.getCli().getExportAs();

        if (file != null) {
            return ExportPattern.getPathSansExt(file);
        }

        // Folder?
        final Path exportFolder = Main.getCli().getExportFolder();

        if (exportFolder != null) {
            return exportFolder.resolve(book.getRadix());
        }

        // Default
        if (useSeparateBookFolders()) {
            // Define target based on base + book folder and book name
            Path folder = getBaseFolder().resolve(book.getRadix());

            try {
                if (!Files.exists(folder)) {
                    Files.createDirectories(folder);
                }

                return folder.resolve(book.getRadix());
            } catch (IOException ex) {
                logger.warn("Cannot create {}", folder, ex);

                return null;
            }
        } else {
            // Define target based on global folder and book name
            return getBaseFolder().resolve(book.getRadix());
        }
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

        // File?
        final Path file = Main.getCli().getPrintAs();

        if (file != null) {
            return file;
        }

        // Folder?
        Path folder = Main.getCli().getPrintFolder();

        if (folder == null) {
            folder = getTargetFolder(book); // Default
        }

        return folder.resolve(book.getRadix() + OMR.PDF_EXTENSION);
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

    //------------//
    // loadScript //
    //------------//
    @Override
    public Book loadScript (Path path)
    {
        return ScriptManager.getInstance().loadAndRun(path.toFile(), false);
    }

    //---------//
    // useOpus //
    //---------//
    public static boolean useOpus ()
    {
        return constants.useOpus.isSet();
    }

    //--------------//
    // useSignature //
    //--------------//
    public static boolean useSignature ()
    {
        return constants.defaultSigned.isSet();
    }

    //-----------------//
    // getInputHistory //
    //-----------------//
    /**
     * Get access to the list of previous inputs.
     *
     * @return the history set of input files
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

    //-----------------//
    // getTargetFolder //
    //-----------------//
    /**
     * Report the default target folder for the provided book
     *
     * @param book provided book
     * @return default target folder
     */
    private static Path getTargetFolder (Book book)
    {
        if (useSeparateBookFolders()) {
            // Define target based on base + book folder and book name
            Path folder = getBaseFolder().resolve(book.getRadix());

            try {
                if (!Files.exists(folder)) {
                    Files.createDirectories(folder);
                }

                return folder;
            } catch (IOException ex) {
                logger.warn("Cannot create {}", folder, ex);

                return null;
            }
        } else {
            // Use base folder
            return getBaseFolder();
        }
    }

    //---------//
    // addBook //
    //---------//
    /**
     * Insert this new book in the set of book instances.
     *
     * @param book the book to insert
     */
    private void addBook (Book book)
    {
        logger.debug("addBook {}", book);

        //
        //        // Remove duplicate if any
        //        for (Iterator<Book> it = books.iterator(); it.hasNext();) {
        //            Book b = it.next();
        //            Path path = b.getInputPath();
        //
        //            if (path.equals(book.getInputPath())) {
        //                logger.debug("Removing duplicate {}", b);
        //                it.remove();
        //                b.close();
        //
        //                break;
        //            }
        //        }
        //
        // Insert new book instance
        synchronized (books) {
            books.add(book);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean useOpus = new Constant.Boolean(
                false,
                "Should we use Opus notion for export (rather than separate files)?");

        private final Constant.Boolean useCompression = new Constant.Boolean(
                true,
                "Should we compress the MusicXML output?");

        private final Constant.Boolean useSeparateBookFolders = new Constant.Boolean(
                true,
                "Should we use a separate folder for each book?");

        private final Constant.String baseFolder = new Constant.String(
                WellKnowns.DEFAULT_BASE_FOLDER.toString(),
                "Base for output folders");

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

        private final Constant.String scriptHistory = new Constant.String(
                "",
                "History of scripts most recently loaded or saved");

        private final Constant.Integer historySize = new Constant.Integer(
                "count",
                10,
                "Maximum number of files names kept in history");
    }
}
