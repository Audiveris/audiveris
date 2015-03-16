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
import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.OpusExporter;
import omr.score.Score;
import omr.score.ScoreExporter;
import omr.score.ScoreReduction;
import omr.score.entity.Page;
import omr.score.ui.BookPdfOutput;

import omr.script.ScriptActions;

import omr.step.Stepping;
import omr.step.Steps;

import omr.util.FileUtil;
import omr.util.NameSet;

import org.jdesktop.application.Application.ExitListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
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
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BookManager.class);

    /** The extension used for score output files: {@value}. */
    public static final String SCORE_EXTENSION = ".xml";

    /** The extension used for compressed score output files: {@value}. */
    public static final String COMPRESSED_SCORE_EXTENSION = ".mxl";

    /** The extension used for compressed score print files: {@value}. */
    public static final String PDF_EXTENSION = ".pdf";

    /** The (double) extension used for opus output files: {@value}. */
    public static final String OPUS_EXTENSION = ".opus.mxl";

    /** The extension prefix used for movement output files: {@value}. */
    public static final String MOVEMENT_EXTENSION = ".mvt";

    /** The prefix used for sheet output files in a multi-sheet book: {@value}. */
    public static final String SHEET_PREFIX = "sheet#";

    /** The extension used for bench files: {@value}. */
    public static final String BENCH_EXTENSION = ".bench.properties";

    /** The single instance of this class. */
    private static volatile BookManager INSTANCE;

    //~ Instance fields ----------------------------------------------------------------------------
    /** All book instances. */
    private final List<Book> instances = new ArrayList<Book>();

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
    //-------------//
    // addInstance //
    //-------------//
    /**
     * Insert this new book in the set of book instances.
     *
     * @param book the book to insert
     */
    public synchronized void addInstance (Book book)
    {
        logger.debug("addInstance {}", book);

        // Remove duplicate if any
        for (Iterator<Book> it = instances.iterator(); it.hasNext();) {
            Book b = it.next();
            Path path = b.getImagePath();

            if (path.equals(book.getImagePath())) {
                logger.debug("Removing duplicate {}", b);
                it.remove();
                b.close();

                break;
            }
        }

        // Insert new book instance
        instances.add(book);
    }

    //-----------//
    // cleanBook //
    //-----------//
    /**
     * Cleanup the existing outputs of provided book.
     *
     * @param book     the book whose outputs must be cleaned up
     * @param bookPath export book path (sans extension) for a specific location, or null to let
     *                 the program choose
     */
    public void cleanBook (Book book,
                           Path bookPath)
    {
        // Determine the output path for the provided book
        if (bookPath == null) {
            bookPath = getDefaultExportPath(book);
        }

        // One-sheet book: <bookname>.mxl
        // One-sheet book: <bookname>.mvt<M>.mxl
        // One-sheet book: <bookname>/... (perhaps some day: 1 directory per book)
        //
        // Multi-sheet book: <bookname>/sheet#<N>.mxl
        // Multi-sheet book: <bookname>/sheet#<N>.mvt<M>.mxl
        // Multi-sheet book: <bookname>/sheet#<N>/... (perhaps some day: 1 directory per sheet)
        final Path folder = book.isMultiSheet() ? bookPath : bookPath.getParent();
        final Path bookName = bookPath.getFileName(); // bookname

        final String dirGlob = "glob:**/" + bookName + "{/**,}";
        final String filGlob = "glob:**/" + bookName + "{/**,.*}";
        final List<Path> paths = FileUtil.walkDown(folder, dirGlob, filGlob);

        if (!paths.isEmpty()) {
            deletePaths(bookName + " deletion", paths);
        } else {
            logger.info("Nothing to delete");
        }
    }

    //------------//
    // cleanSheet //
    //------------//
    /**
     * Cleanup the existing output of provided sheet.
     *
     * @param sheet    the sheet to clean up
     * @param bookPath export book name (sans extension) for a specific location, or null to let
     *                 the program choose
     */
    public void cleanSheet (Sheet sheet,
                            Path bookPath)
    {
        final Book book = sheet.getBook();

        if (!book.isMultiSheet()) {
            cleanBook(book, null); // Simply delete the single-sheet book!
        } else {
            // path/to/scores/Book
            bookPath = getActualPath(bookPath, getDefaultExportPath(book));

            // Determine the output path (sans extension) for the provided sheet
            final Path sheetPathSansExt = getSheetExportPath(bookPath, sheet);

            // Multi-sheet book: <bookname>/sheet#<N>.mvt<M>.mxl
            // Multi-sheet book: <bookname>/sheet#<N>.mxl
            // Multi-sheet book: <bookname>/sheet#<N>/... (perhaps some day: 1 directory per sheet)
            final Path folder = sheetPathSansExt.getParent();
            final Path bookName = folder.getFileName(); // bookname
            final Path sheetName = sheetPathSansExt.getFileName(); // sheet#N

            final String dirGlob = "glob:**/" + bookName + "/" + sheetName + "{/**,}";
            final String filGlob = "glob:**/" + bookName + "/" + sheetName + "{/**,.*}";
            final List<Path> paths = FileUtil.walkDown(folder, dirGlob, filGlob);

            if (!paths.isEmpty()) {
                deletePaths(bookName + "/" + sheetName + " deletion", paths);
            }
        }
    }

    //------------//
    // exportBook //
    //------------//
    /**
     * Export a whole book in MusicXML.
     * <p>
     * The output is structured differently according to whether the book contains one or several
     * scores.<ul>
     * <li>A single-score book results in one score output.</li>
     * <li>A multi-score book results in one opus output (if useOpus is set) or a series of scores
     * (is useOpus is not set).</li>
     * </ul>
     *
     * @param book     the book to export
     * @param bookPath target book path (sans extension) for a specific location, or null to let
     *                 the program choose
     * @param signed   should we inject our signature?, may be null to use default
     */
    public void exportBook (Book book,
                            Path bookPath,
                            Boolean signed)
    {
        // Make sure all sheets have been transcribed
        for (Sheet sheet : book.getSheets()) {
            Stepping.ensureSheetStep(Steps.valueOf(Steps.PAGE), sheet);
        }

        // Group book pages into scores
        book.retrieveScores();

        final List<Score> scores = book.getScores();

        for (Score score : scores) {
            // Merges pages into their containing movement score (connecting the parts across pages)
            // TODO: this may need the addition of dummy parts in some pages
            new ScoreReduction(score).reduce();

            for (Page page : score.getPages()) {
                //                // - Retrieve the actual duration of every measure
                //                page.accept(new DurationRetriever());
                //
                //                // - Check all voices timing, assign forward items if needed.
                //                // - Detect special measures and assign proper measure ids
                //                // If needed, we can trigger a reprocessing of this page
                //                page.accept(new MeasureFixer());
                //
                // Check whether time signatures are consistent accross all pages in score
                // TODO: to be implemented
                //
                // Connect slurs across pages
                page.getFirstSystem().connectPageInitialSlurs(score);
            }
        }

        // path/to/scores/Book
        bookPath = getActualPath(bookPath, getDefaultExportPath(book));

        final boolean compressed = useCompression();
        final String ext = compressed ? COMPRESSED_SCORE_EXTENSION : SCORE_EXTENSION;
        final boolean sig = (signed != null) ? signed : constants.defaultSigned.isSet();

        // Export each movement score
        String bookName = bookPath.getFileName().toString();
        final boolean multiMovements = scores.size() > 1;

        if (constants.useOpus.isSet()) {
            // Export the book as one opus
            final Path opusPath = bookPath.resolveSibling(bookName + OPUS_EXTENSION);

            try {
                if (!confirmed(opusPath)) {
                    return;
                }

                new OpusExporter(book).export(opusPath, bookName, sig);
                constants.defaultExportDirectory.setValue(bookPath.getParent().toString());
            } catch (Exception ex) {
                logger.warn("Could not export opus " + opusPath, ex);
            }
        } else {
            for (Score score : scores) {
                final String scoreName = (!multiMovements) ? bookName
                        : (bookName + MOVEMENT_EXTENSION + score.getId());
                final Path scorePath = bookPath.resolveSibling(scoreName + ext);

                try {
                    if (!confirmed(scorePath)) {
                        return;
                    }

                    new ScoreExporter(score).export(scorePath, scoreName, sig, compressed);
                    constants.defaultExportDirectory.setValue(bookPath.getParent().toString());
                } catch (Exception ex) {
                    logger.warn("Could not export score " + scoreName, ex);
                }
            }
        }
    }

    //-------------//
    // exportSheet //
    //-------------//
    /**
     * Export a single sheet in MusicXML.
     * <p>
     * The output is structured differently according to whether the sheet contains one or several
     * pages.<ul>
     * <li>A single-page sheet results in one score output.</li>
     * <li>A multi-page sheet results in one opus output (if useOpus is set) or a folder of scores
     * (is useOpus is not set).</li>
     * </ul>
     *
     * @param sheet    the sheet to export
     * @param bookPath export book path (sans extension) for a specific location, or null to let
     *                 the program choose (based on book or default location).
     *                 Typically, assuming book name is "Book", something like: path/to/scores/Book
     * @param signed   should we inject ProxyMusic signature?, null to use default
     */
    public void exportSheet (Sheet sheet,
                             Path bookPath,
                             Boolean signed)
    {
        final List<Page> pages = sheet.getPages();

        if (pages.isEmpty()) {
            return;
        }

        final boolean compressed = useCompression();
        final Book book = sheet.getBook();

        // path/to/scores/Book
        bookPath = getActualPath(bookPath, getDefaultExportPath(book));

        // Determine the output path (sans extension) for the provided sheet
        final Path sheetPathSansExt = getSheetExportPath(bookPath, sheet);

        try {
            if (book.isMultiSheet() && !Files.exists(bookPath)) {
                Files.createDirectories(bookPath);
            }

            final String rootName = sheetPathSansExt.getFileName().toString();
            final boolean sig = (signed != null) ? signed : constants.defaultSigned.isSet();

            if (pages.size() > 1) {
                // Export the sheet multiple pages as separate scores in folder 'pathSansExt'
                final String ext = compressed ? COMPRESSED_SCORE_EXTENSION : SCORE_EXTENSION;
                Files.createDirectories(sheetPathSansExt);

                for (Page page : pages) {
                    final Score score = new Score();
                    score.addPage(page);

                    final int idx = 1 + pages.indexOf(page);
                    score.setId(idx);

                    final String scoreName = rootName + MOVEMENT_EXTENSION + idx;
                    final Path scorePath = sheetPathSansExt.resolve(scoreName + ext);
                    new ScoreExporter(score).export(scorePath, scoreName, sig, compressed);
                }
            } else {
                // Export the sheet single page as a score
                final Score score = new Score();
                score.setId(1);
                score.addPage(sheet.getLastPage());

                final String ext = compressed ? COMPRESSED_SCORE_EXTENSION : SCORE_EXTENSION;
                final Path scorePath = sheetPathSansExt.resolveSibling(rootName + ext);
                new ScoreExporter(score).export(scorePath, rootName, sig, compressed);
            }

            // Remember the book path in the book itself
            sheet.getBook().setExportPath(bookPath);

            constants.defaultExportDirectory.setValue(bookPath.getParent().toString());
        } catch (Exception ex) {
            logger.warn("Error storing " + sheet + ", " + ex, ex);
        }
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
    public Path getDefaultBenchPath (Book book)
    {
        final String child = book.getRadix() + BENCH_EXTENSION;
        final Path mainPath = Main.getBenchFolder();

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
    public File getDefaultDewarpDirectory ()
    {
        return new File(constants.defaultDewarpDirectory.getValue());
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
    public Path getDefaultExportPath (Book book)
    {
        if (book.getExportPath() != null) {
            return book.getExportPath();
        }

        Path mainPath = Main.getExportFolder();

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
    public String getDefaultInputDirectory ()
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
    public Path getDefaultPrintPath (Book book)
    {
        if (book.getPrintPath() != null) {
            return book.getPrintPath();
        }

        final Path mainPath = Main.getPrintFolder();

        if (mainPath != null) {
            if (Files.isDirectory(mainPath)) {
                return mainPath.resolve(book.getRadix());
            } else {
                return mainPath;
            }
        }

        return Paths.get(constants.defaultPrintDirectory.getValue(), book.getRadix());
    }

    //-----------------//
    // getExitListener //
    //-----------------//
    /**
     * The Exit listener meant for GUI.
     *
     * @return the listener to register
     */
    public ExitListener getExitListener ()
    {
        return new ExitListener()
        {
            @Override
            public boolean canExit (EventObject e)
            {
                // Are all scripts stored (or explicitly ignored)?
                for (Book book : instances) {
                    if (!ScriptActions.checkStored(book.getScript())) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public void willExit (EventObject e)
            {
                // Close all sheets, to record their bench data
                closeAllBooks();
            }
        };
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
            return COMPRESSED_SCORE_EXTENSION;
        } else {
            return SCORE_EXTENSION;
        }
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

    //--------------------//
    // getSheetExportPath //
    //--------------------//
    /**
     * Report the path (sans extension) to which the sheet will be written.
     * <ul>
     * <li>If this sheet is the only one in the containing book, we use:<br/>
     * &lt;book-name&gt;</li>
     * <li>If the book contains several sheets, we use:<br/>
     * &lt;book-name&gt;/sheet#&lt;N&gt;</li>
     * </ul>
     *
     * @param bookPath the non-null bookPath
     * @param sheet    the sheet to export
     * @return the sheet path
     */
    public Path getSheetExportPath (Path bookPath,
                                    Sheet sheet)
    {
        final Book book = sheet.getBook();

        // Determine the output path (sans extension) for the provided sheet
        // path/to/scores/Book            (for a single-sheet book)
        // path/to/scores/Book/sheet#N    (for a multi-sheet book)
        if (!book.isMultiSheet()) {
            return bookPath;
        } else {
            return bookPath.resolve(SHEET_PREFIX + (book.getOffset() + sheet.getIndex()));
        }
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
        return getInstance().instances.size() > 1;
    }

    //-----------//
    // printBook //
    //-----------//
    /**
     * Print the book physical appearance into the provided PDF file.
     *
     * @param book     the provided book
     * @param bookPath print book path (sans extension) for a specific location, or null to let
     *                 the program choose (based on book or default location).
     *                 Typically, assuming book name is "Book", something like: path/to/prints/Book
     */
    public void printBook (Book book,
                           Path bookPath)
    {
        // Make sure all sheets have been transcribed
        for (Sheet sheet : book.getSheets()) {
            Stepping.ensureSheetStep(Steps.valueOf(Steps.PAGE), sheet);
        }

        // path/to/prints/Book
        bookPath = getActualPath(bookPath, getDefaultPrintPath(book));

        final String rootName = bookPath.getFileName().toString();
        final Path pdfPath = bookPath.resolveSibling(rootName + ".pdf");

        // Actually write the PDF
        try {
            // Prompt for overwrite?
            if (!confirmed(pdfPath)) {
                return;
            }

            new BookPdfOutput(book, pdfPath.toFile()).write(null);
            logger.info("Book printed to {}", pdfPath);

            // Remember (even across runs) the selected print directory
            book.setPrintPath(bookPath);
            constants.defaultPrintDirectory.setValue(bookPath.getParent().toString());
        } catch (Exception ex) {
            logger.warn("Cannot write PDF to " + pdfPath, ex);
        }
    }

    //------------//
    // printSheet //
    //------------//
    /**
     * Print the sheet physical appearance into the provided PDF file.
     *
     * @param sheet    the provided sheet
     * @param bookPath print book path (sans extension) for a specific location, or null to let
     *                 the program choose (based on book or default location).
     *                 Typically, assuming book name is "Book", something like: path/to/prints/Book
     */
    public void printSheet (Sheet sheet,
                            Path bookPath)
    {
        final Book book = sheet.getBook();
        // path/to/prints/Book
        bookPath = getActualPath(bookPath, getDefaultPrintPath(book));

        // Determine the output path (sans extension) for the provided sheet
        final Path sheetPathSansExt = getSheetExportPath(bookPath, sheet);
        final String rootName = sheetPathSansExt.getFileName().toString();
        final Path pdfPath = sheetPathSansExt.resolveSibling(rootName + ".pdf");

        // Actually write the PDF
        try {
            // Prompt for overwrite?
            if (!confirmed(pdfPath)) {
                return;
            }

            if (book.isMultiSheet() && !Files.exists(bookPath)) {
                Files.createDirectories(bookPath);
            }

            new BookPdfOutput(book, pdfPath.toFile()).write(sheet);
            logger.info("Sheet printed to {}", pdfPath);

            // Remember (even across runs) the selected print directory
            book.setPrintPath(bookPath);
            constants.defaultPrintDirectory.setValue(bookPath.getParent().toString());
        } catch (Exception ex) {
            logger.warn("Cannot write PDF to " + pdfPath, ex);
        }
    }

    //----------------//
    // removeInstance //
    //----------------//
    /**
     * Remove the provided book from the collection of instances.
     *
     * @param book the book to remove
     */
    public synchronized void removeInstance (Book book)
    {
        logger.debug("removeInstance {}", book);
        instances.remove(book);
    }

    //--------------------------//
    // setDefaultInputDirectory //
    //--------------------------//
    /**
     * Remember the directory where images should be found.
     *
     * @param directory the latest image directory
     */
    public void setDefaultInputDirectory (String directory)
    {
        constants.defaultInputDirectory.setValue(directory);
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
    public void storeBench (BookBench bench,
                            boolean complete)
    {
        // Check if we do save bench data
        if ((Main.getBenchFolder() == null) && !constants.saveBenchToDisk.isSet()) {
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

    //---------------//
    // closeAllBooks //
    //---------------//
    /**
     * Close all book instances.
     */
    private void closeAllBooks ()
    {
        int count = 0;

        // NB: Use a COPY of instances, to avoid concurrent modification
        for (Book book : new ArrayList<Book>(instances)) {
            book.close();
            count++;
        }

        logger.debug("{} book(s) closed", count);
    }

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
    private boolean confirmed (Path target)
    {
        return ((Main.getGui() == null) || !Files.exists(target))
               || Main.getGui().displayConfirmation("Overwrite " + target + "?");
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
    private void deletePaths (String title,
                              List<Path> paths)
    {
        if (!paths.isEmpty()) {
            if (Main.getGui() != null) {
                StringBuilder sb = new StringBuilder();

                for (Path p : paths) {
                    if (Files.isDirectory(p)) {
                        sb.append("\ndir  ");
                    } else {
                        sb.append("\nfile ");
                    }

                    sb.append(p);
                }

                if (!Main.getGui().displayConfirmation("Confirm " + title + "?\n" + sb)) {
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
    private Path getActualPath (Path targetPath,
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

        Constant.String defaultExportDirectory = new Constant.String(
                WellKnowns.DEFAULT_SCORES_FOLDER.toString(),
                "Default directory for saved scores");

        Constant.Boolean saveBenchToDisk = new Constant.Boolean(
                false,
                "Should we save bench data to disk");

        Constant.String defaultBenchDirectory = new Constant.String(
                WellKnowns.DEFAULT_BENCHES_FOLDER.toString(),
                "Default directory for saved benches");

        Constant.String defaultPrintDirectory = new Constant.String(
                WellKnowns.DEFAULT_PRINT_FOLDER.toString(),
                "Default directory for printing sheet files");

        Constant.Boolean defaultSigned = new Constant.Boolean(
                true,
                "Should we inject ProxyMusic signature in the exported scores?");

        Constant.String imagesHistory = new Constant.String(
                "",
                "History of books most recently loaded");

        Constant.Integer historySize = new Constant.Integer(
                "count",
                10,
                "Maximum number of files names kept in history");

        Constant.String defaultInputDirectory = new Constant.String(
                WellKnowns.EXAMPLES_FOLDER.toString(),
                "Default directory for selection of image files");

        Constant.String defaultDewarpDirectory = new Constant.String(
                WellKnowns.TEMP_FOLDER.toString(),
                "Default directory for saved dewarped images");
    }
}
