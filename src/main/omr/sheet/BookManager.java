//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B o o k M a n a g e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
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

import omr.ui.OmrGui;

import omr.util.NameSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
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
 * It handles the collection of all book instances currently loaded, as well as the recent history
 * of books previously loaded.
 * <p>
 * It handles where and how to export, print, bench books and sheets.
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
 * In practice, we will rather process input by physical chunks, say 1 sheet (or 5 sheets) at a
 * time, and assemble the logical items in a second phase.
 * The final result will be the same, but this approach will require the handling of intermediate
 * Book and Score instances.
 * <p>
 * Intermediate items, with book chunks of 1 sheet, could be structured as follows:
 * <blockquote>
 * <pre>
 * Mozart_S40/
 * Mozart_S40/book-1/sheet#1.mxl
 * Mozart_S40/book-2/sheet#2.mxl
 * [...]
 * Mozart_S40/book-33/sheet#33.mvt1.mxl
 * Mozart_S40/book-33/sheet#33.mvt2.mxl
 * [...]
 * Mozart_S40/book-49/sheet#49.mxl
 * </pre>
 * </blockquote>
 * Intermediate items, with book chunks of 5 sheets, could be structured as follows:
 * <blockquote>
 * <pre>
 * Mozart_S40/
 * Mozart_S40/book-1-5/sheet#1.mxl
 * Mozart_S40/book-1-5/sheet#2.mxl
 * Mozart_S40/book-1-5/sheet#3.mxl
 * Mozart_S40/book-1-5/sheet#4.mxl
 * Mozart_S40/book-1-5/sheet#5.mxl
 *
 * Mozart_S40/book-6-10/sheet#6.mxl
 * Mozart_S40/book-6-10/sheet#7.mxl
 * [...]
 * Mozart_S40/book-26-30/sheet#30.mxl
 *
 * Mozart_S40/book-31-35/sheet#31.mxl
 * Mozart_S40/book-31-35/sheet#32.mxl
 * Mozart_S40/book-31-35/sheet#33.mvt1.mxl
 * Mozart_S40/book-31-35/sheet#33.mvt2.mxl
 * Mozart_S40/book-31-35/sheet#34.mxl
 * Mozart_S40/book-31-35/sheet#35.mxl
 *
 * Mozart_S40/book-36-40/sheet#36.mxl
 * [...]
 * Mozart_S40/book-41-45/sheet#45.mxl
 *
 * Mozart_S40/book-46-49/sheet#46.mxl
 * Mozart_S40/book-46-49/sheet#47.mxl
 * Mozart_S40/book-46-49/sheet#48.mxl
 * Mozart_S40/book-46-49/sheet#49.mxl
 * </pre>
 * </blockquote>
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

    static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BookManager.class);

    /** The single instance of this class. */
    private static volatile BookManager INSTANCE;

    //~ Instance fields ----------------------------------------------------------------------------
    /** OMR master gui, if any. */
    private OmrGui gui;

    /** All book instances. */
    final List<Book> books = new ArrayList<Book>();

    /** Image file history. (filled only when images (books) are successfully loaded) */
    private NameSet history;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Private constructor for a singleton.
     */
    private BookManager ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // confirmed //
    //-----------//
    /**
     * Check whether we have user confirmation to overwrite the target path.
     * This is a no-op is there is no Gui or if target does not already exist.
     *
     * @param target the path to be checked
     * @return false if explicitly not confirmed, true otherwise
     */
    public static boolean confirmed (Path target)
    {
        return ((OMR.getGui() == null) || !Files.exists(target))
               || OMR.getGui().displayConfirmation("Overwrite " + target + "?");
    }

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
            if (OMR.getGui() != null) {
                StringBuilder sb = new StringBuilder();

                for (Path p : paths) {
                    if (Files.isDirectory(p)) {
                        sb.append("\ndir  ");
                    } else {
                        sb.append("\nfile ");
                    }

                    sb.append(p);
                }

                if (!OMR.getGui().displayConfirmation("Confirm " + title + "?\n" + sb)) {
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

    //-------------//
    // getAllBooks //
    //-------------//
    /**
     * Report the current collection of books.
     *
     * @return an unmodifiable view of the books list
     */
    @Override
    public List<Book> getAllBooks ()
    {
        return Collections.unmodifiableList(books);
    }

    //---------------------//
    // getDefaultBenchPath //
    //---------------------//
    /**
     * Report the path to which the bench data would be written by default.
     *
     * @param book the book to export
     * @return the default file
     */
    public static Path getDefaultBenchPath (Book book)
    {
        final String child = book.getRadix() + OMR.BENCH_EXTENSION;
        final Path mainPath = Main.getCli().getBenchFolder();

        if (mainPath != null) {
            return mainPath.resolve(child);
        }

        return Paths.get(constants.defaultBenchDirectory.getValue(), child);
    }

    //---------------------------//
    // getDefaultDewarpDirectory //
    //---------------------------//
    /**
     * Report the directory to which dewarped images would be saved by default.
     *
     * @return the default file
     */
    public static String getDefaultDewarpDirectory ()
    {
        return constants.defaultDewarpDirectory.getValue();
    }

    //----------------------//
    // getDefaultExportPath //
    //----------------------//
    /**
     * Report the path to which the book would be written by default.
     *
     * @param book the book to export
     * @return the default book path for export
     */
    public static Path getDefaultExportPath (Book book)
    {
        if (book.getExportPath() != null) {
            return book.getExportPath();
        }

        Path mainPath = Main.getCli().getExportFolder();

        if (mainPath != null) {
            if (Files.isDirectory(mainPath)) {
                return mainPath.resolve(book.getRadix());
            } else {
                return mainPath;
            }
        }

        return Paths.get(constants.defaultExportDirectory.getValue(), book.getRadix());
    }

    //--------------------------//
    // getDefaultInputDirectory //
    //--------------------------//
    /**
     * Report the directory where images should be found.
     *
     * @return the latest image directory
     */
    public static String getDefaultInputDirectory ()
    {
        return constants.defaultInputDirectory.getValue();
    }

    //---------------------//
    // getDefaultPrintPath //
    //---------------------//
    /**
     * Report the path to which the book PDF data would be written by default.
     *
     * @param book the book to export
     * @return the default book path for print
     */
    public static Path getDefaultPrintPath (Book book)
    {
        if (book.getPrintPath() != null) {
            return book.getPrintPath();
        }

        final Path mainPath = Main.getCli().getPrintFolder();

        if (mainPath != null) {
            if (Files.isDirectory(mainPath)) {
                return mainPath.resolve(book.getRadix());
            } else {
                return mainPath;
            }
        }

        return Paths.get(constants.defaultPrintDirectory.getValue(), book.getRadix());
    }

    //--------------------//
    // getExportExtension //
    //--------------------//
    /**
     * Report the extension to use for exported score, depending on the use (or not) of
     * compression.
     *
     * @return the file extension to use.
     */
    public static String getExportExtension ()
    {
        if (useCompression()) {
            return OMR.COMPRESSED_SCORE_EXTENSION;
        } else {
            return OMR.SCORE_EXTENSION;
        }
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

    //---------------------------//
    // setDefaultExportDirectory //
    //---------------------------//
    public static void setDefaultExportDirectory (String value)
    {
        constants.defaultExportDirectory.setValue(value);
    }

    //--------------------------//
    // setDefaultInputDirectory //
    //--------------------------//
    /**
     * Remember the directory where images should be found.
     *
     * @param value the latest image directory
     */
    public static void setDefaultInputDirectory (String value)
    {
        constants.defaultInputDirectory.setValue(value);
    }

    //------------//
    // storeBench //
    //------------//
    /**
     * Store the book bench.
     *
     * @param bench    the bench to write to disk
     * @param complete true if we need to complete the bench data
     */
    public static void storeBench (BookBench bench,
                                   boolean complete)
    {
        // Check if we do save bench data
        if ((Main.getCli().getBenchFolder() == null) && !constants.saveBenchToDisk.isSet()) {
            return;
        }

        Path target = getActualPath(null, getDefaultBenchPath(bench.getBook()));

        // Actually store the book bench
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(target.toString());
            bench.store(fos, complete);

            if (complete) {
                logger.info("Complete book bench stored to {}", target);
            }

            // Remember (even across runs) the selected directory
            constants.defaultBenchDirectory.setValue(target.getParent().toString());
        } catch (Exception ex) {
            logger.warn("Error storing book bench to " + target, ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
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

    //--------------//
    // useSignature //
    //--------------//
    public static boolean useSignature ()
    {
        return constants.defaultSigned.isSet();
    }

    //------------//
    // getHistory //
    //------------//
    /**
     * Get access to the list of previously handled images.
     *
     * @return the history set of image files
     */
    public NameSet getHistory ()
    {
        if (history == null) {
            history = new NameSet(
                    "Images History",
                    constants.imagesHistory,
                    constants.historySize.getValue());
        }

        return history;
    }

    //------------//
    // initialize //
    //------------//
    @Override
    public void initialize ()
    {
        // void
    }

    //-----------//
    // loadInput //
    //-----------//
    @Override
    public Book loadInput (Path path)
    {
        final Book book = new BasicBook(path);
        addBook(book);

        return book;
    }

    //-------------//
    // loadProject //
    //-------------//
    @Override
    public Book loadProject (Path path)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //------------//
    // loadScript //
    //------------//
    @Override
    public Book loadScript (Path path)
    {
        return ScriptManager.getInstance().loadAndRun(path.toFile(), false);
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

    //--------------------------//
    // setDefaultPrintDirectory //
    //--------------------------//
    public static void setDefaultPrintDirectory (String value)
    {
        constants.defaultPrintDirectory.setValue(value);
    }

    //-----------//
    // terminate //
    //-----------//
    @Override
    public void terminate ()
    {
        // void
    }

    //---------//
    // useOpus //
    //---------//
    public static boolean useOpus ()
    {
        return constants.useOpus.isSet();
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

        final Constant.Boolean useOpus = new Constant.Boolean(
                false,
                "Should we use Opus notion for export (rather than separate files)?");

        final Constant.Boolean useCompression = new Constant.Boolean(
                true,
                "Should we compress the MusicXML output?");

        final Constant.String defaultExportDirectory = new Constant.String(
                WellKnowns.DEFAULT_SCORES_FOLDER.toString(),
                "Default directory for saved scores");

        final Constant.Boolean saveBenchToDisk = new Constant.Boolean(
                false,
                "Should we save bench data to disk");

        final Constant.String defaultBenchDirectory = new Constant.String(
                WellKnowns.DEFAULT_BENCHES_FOLDER.toString(),
                "Default directory for saved benches");

        final Constant.String defaultPrintDirectory = new Constant.String(
                WellKnowns.DEFAULT_PRINT_FOLDER.toString(),
                "Default directory for printing sheet files");

        final Constant.Boolean defaultSigned = new Constant.Boolean(
                true,
                "Should we inject ProxyMusic signature in the exported scores?");

        final Constant.String imagesHistory = new Constant.String(
                "",
                "History of books most recently loaded");

        final Constant.Integer historySize = new Constant.Integer(
                "count",
                10,
                "Maximum number of files names kept in history");

        final Constant.String defaultInputDirectory = new Constant.String(
                WellKnowns.EXAMPLES_FOLDER.toString(),
                "Default directory for selection of image files");

        final Constant.String defaultDewarpDirectory = new Constant.String(
                WellKnowns.TEMP_FOLDER.toString(),
                "Default directory for saved dewarped images");
    }
}
