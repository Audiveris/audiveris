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
import omr.score.entity.Page;
import omr.score.ui.BookPdfOutput;

import omr.script.ScriptActions;

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
 * The way books and sheets are exported depends on whether we allow the use of MusicXML Opus:
 * An opus provides gathering features like an archive which is convenient when several items must
 * be exported. Without opus notion, we have to set up on some ad-hoc sub-folders organization.
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
 * We could process all the 49 sheets in memory (although this is not really feasible) with a single
 * book, discovering the 4 movements one after the other, and finally creating one MusicXML Opus
 * containing 4 {@link Score} instances, one for each movement.
 * <p>
 * In practice, we will rather process input by physical chunks, say 1 sheet (or 5 sheets) at a
 * time, and assemble the logical items in a second phase.
 * The final result will be the same, but this approach will require the handling of intermediate
 * Book and Score instances.
 * <p>
 * Intermediate items, with book chunks of 1 sheet, could be structured as follows:
 * <pre>
 * Mozart_S40/
 * Mozart_S40/book-1/sheet#1.xml
 * Mozart_S40/book-2/sheet#2.xml
 * [...]
 * Mozart_S40/book-33/sheet#33.mvt1.xml
 * Mozart_S40/book-33/sheet#33.mvt2.xml
 * [...]
 * Mozart_S40/book-49/sheet#49.xml
 * </pre>
 * <p>
 * Intermediate items, with book chunks of 5 sheets, could be structured as follows:
 * <pre>
 * Mozart_S40/
 * Mozart_S40/book-1-5/sheet#1.xml
 * Mozart_S40/book-1-5/sheet#2.xml
 * Mozart_S40/book-1-5/sheet#3.xml
 * Mozart_S40/book-1-5/sheet#4.xml
 * Mozart_S40/book-1-5/sheet#5.xml
 *
 * Mozart_S40/book-6-10/sheet#6.xml
 * Mozart_S40/book-6-10/sheet#7.xml
 * [...]
 * Mozart_S40/book-26-30/sheet#30.xml
 *
 * Mozart_S40/book-31-35/sheet#31.xml
 * Mozart_S40/book-31-35/sheet#32.xml
 * Mozart_S40/book-31-35/sheet#33.mvt1.xml
 * Mozart_S40/book-31-35/sheet#33.mvt2.xml
 * Mozart_S40/book-31-35/sheet#34.xml
 * Mozart_S40/book-31-35/sheet#35.xml
 *
 * Mozart_S40/book-36-40/sheet#36.xml
 * [...]
 * Mozart_S40/book-41-45/sheet#45.xml
 *
 * Mozart_S40/book-46-49/sheet#46.xml
 * Mozart_S40/book-46-49/sheet#47.xml
 * Mozart_S40/book-46-49/sheet#48.xml
 * Mozart_S40/book-46-49/sheet#49.xml
 * </pre>
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
public class BookManager
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BookManager.class);

    /** The extension used for score output files: {@value} */
    public static final String SCORE_EXTENSION = ".xml";

    /** The extension used for compressed score output files: {@value} */
    public static final String COMPRESSED_SCORE_EXTENSION = ".mxl";

    /** The (double) extension used for opus output files: {@value} */
    public static final String OPUS_EXTENSION = ".opus.mxl";

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
     * Private constructor for a singleton
     */
    private BookManager ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        return getInstance().instances.size() > 1;
    }

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

    //--------//
    // export //
    //--------//
    /**
     * Export a book in MusicXML.
     * <p>
     * The output is structured differently according to whether the book contains one or several
     * scores.<ul>
     * <li>A single-score book results in one score output.</li>
     * <li>A multi-score book results in one opus output (if useOpus is set) or a folder of scores
     * (is useOpus is not set).</li>
     * </ul>
     *
     * @param book       the book to export
     * @param targetPath target path name (sans extension) for a specific location, or null to let
     *                   the program choose (based on book, previous or default location)
     * @param signed     should we inject our signature?, may be null to use default
     * @param compressed true for (compressed) MXL, false for (standard) XML
     */
    public void export (Book book,
                        Path targetPath,
                        Boolean signed,
                        boolean compressed)
    {
        final List<Score> scores = book.getScores();

        if (scores.isEmpty()) {
            return;
        }

        // Determine the output path (sans extension) for the provided book
        final Path pathSansExt = getActualPath(targetPath, getDefaultExportPath(book));
        final boolean sig = (signed != null) ? signed : constants.defaultSigned.isSet();
        final String rootName = pathSansExt.getFileName().toString();

        try {
            if (scores.size() > 1) {
                if (constants.useOpus.isSet()) {
                    // Export the book multiple scores as one opus
                    final Path opusPath = pathSansExt.resolveSibling(rootName + OPUS_EXTENSION);
                    new OpusExporter(book).export(opusPath, rootName, sig);
                } else {
                    // Export the book multiple scores as separate scores in folder 'pathSansExt'
                    final String ext = compressed ? COMPRESSED_SCORE_EXTENSION : SCORE_EXTENSION;
                    Files.createDirectories(pathSansExt);

                    for (Score score : scores) {
                        final int idx = 1 + scores.indexOf(score);
                        final String scoreName = rootName + ".mvt" + idx;
                        final Path scorePath = pathSansExt.resolve(scoreName + ext);
                        new ScoreExporter(score).export(scorePath, scoreName, sig, compressed);
                    }
                }
            } else {
                // Single score to export (compressed or not)
                final Score score = book.getLastScore();
                final String ext = compressed ? COMPRESSED_SCORE_EXTENSION : SCORE_EXTENSION;
                final Path scorePath = pathSansExt.resolveSibling(rootName + ext);
                new ScoreExporter(score).export(scorePath, rootName, sig, compressed);
            }

            // Remember the book path in the book itself
            book.setExportPath(pathSansExt);

            // Remember (even across runs) the selected directory for books
            constants.defaultExportDirectory.setValue(pathSansExt.getParent().toString());
        } catch (Exception ex) {
            logger.warn("Error storing " + book + ", " + ex, ex);
        }
    }

    //--------//
    // export //
    //--------//
    /**
     * Export a sheet in MusicXML.
     * <p>
     * The output is structured differently according to whether the sheet contains one or several
     * pages.<ul>
     * <li>A single-page sheet results in one score output.</li>
     * <li>A multi-page sheet results in one opus output (if useOpus is set) or a folder of scores
     * (is useOpus is not set).</li>
     * </ul>
     *
     * @param sheet      the sheet to export
     * @param targetPath target path name (sans extension) for a specific location, or null to let
     *                   the program choose (based on sheet, previous or default location)
     * @param signed     should we inject ProxyMusic signature?, null to use default
     * @param compressed true for compressed output (.mxl) rather than (.xml)
     */
    public void export (Sheet sheet,
                        Path targetPath,
                        Boolean signed,
                        boolean compressed)
    {
        final List<Page> pages = sheet.getPages();

        if (pages.isEmpty()) {
            return;
        }

        // Determine the output path (sans extension) for the provided sheet
        final Path pathSansExt = getActualPath(targetPath, getDefaultExportPath(sheet));
        final String rootName = pathSansExt.getFileName().toString();
        final boolean sig = (signed != null) ? signed : constants.defaultSigned.isSet();

        try {
            if (pages.size() > 1) {
                if (constants.useOpus.isSet()) {
                    // Export the sheet multiple pages as one opus
                    final int offset = sheet.getBook().getOffset() + (sheet.getIndex() - 1);
                    final Book book = new Book(sheet, offset);

                    for (Page page : sheet.getPages()) {
                        Score score = new Score();
                        score.addChild(page);
                        book.addScore(score);
                    }

                    final Path opusPath = pathSansExt.resolveSibling(rootName + OPUS_EXTENSION);
                    new OpusExporter(book).export(opusPath, rootName, sig);
                } else {
                    // Export the sheet multiple pages as separate scores in folder 'pathSansExt'
                    final String ext = compressed ? COMPRESSED_SCORE_EXTENSION : SCORE_EXTENSION;
                    Files.createDirectories(pathSansExt);

                    for (Page page : pages) {
                        final Score score = new Score();
                        score.addChild(page);

                        final int idx = 1 + pages.indexOf(page);
                        final String scoreName = rootName + ".mvt" + idx;
                        final Path scorePath = pathSansExt.resolve(scoreName + ext);
                        new ScoreExporter(score).export(scorePath, scoreName, sig, compressed);
                    }
                }
            } else {
                // Export the sheet single page as a score
                final Score score = new Score();
                score.addChild(sheet.getLastPage());

                final String ext = compressed ? COMPRESSED_SCORE_EXTENSION : SCORE_EXTENSION;
                final Path scorePath = pathSansExt.resolveSibling(rootName + ext);
                new ScoreExporter(score).export(scorePath, rootName, sig, compressed);
            }

            // Remember the book path in the book itself
            final Path bookPath = pathSansExt.getParent();
            sheet.getBook().setExportPath(bookPath);

            // Remember (even across runs) the selected directory for books
            final Path exportsPath = bookPath.getParent();
            constants.defaultExportDirectory.setValue(exportsPath.toString());
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
        Path mainPath = Main.getBenchPath();

        String child = book.getRadix() + BENCH_EXTENSION;

        if (mainPath != null) {
            if (Files.isDirectory(mainPath)) {
                return mainPath.resolve(child);
            } else {
                return mainPath;
            }
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
     * @return the default book path
     */
    public Path getDefaultExportPath (Book book)
    {
        if (book.getExportPath() != null) {
            return book.getExportPath();
        }

        Path mainPath = Main.getExportPath();

        if (mainPath != null) {
            if (Files.isDirectory(mainPath)) {
                return mainPath.resolve(book.getRadix());
            } else {
                return mainPath;
            }
        }

        return Paths.get(constants.defaultExportDirectory.getValue(), book.getRadix());
    }

    //----------------------//
    // getDefaultExportPath //
    //----------------------//
    /**
     * Report the path to which the sheet would be written by default.
     *
     * @param sheet the sheet to export
     * @return the default sheet path
     */
    public Path getDefaultExportPath (Sheet sheet)
    {
        final Book book = sheet.getBook();
        final Path bookPath = getDefaultExportPath(book);

        return bookPath.resolve("sheet#" + (book.getOffset() + sheet.getIndex()));
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
     * @return the default file
     */
    public Path getDefaultPrintPath (Book book)
    {
        if (book.getPrintPath() != null) {
            return book.getPrintPath();
        }

        final String child = book.getRadix() + ".sheet.pdf";
        final Path mainPath = Main.getPrintPath();

        if (mainPath != null) {
            if (Files.isDirectory(mainPath)) {
                return mainPath.resolve(child);
            } else {
                return mainPath;
            }
        }

        return Paths.get(constants.defaultPrintDirectory.getValue(), child);
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
        if ((Main.getBenchPath() == null) && !constants.saveBenchToDisk.isSet()) {
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

    //------------------//
    // writePhysicalPdf //
    //------------------//
    /**
     * Print the book physical appearance into the provided PDF file.
     *
     * @param book   the provided book
     * @param target the PDF path to write
     */
    public void writePhysicalPdf (Book book,
                                  Path target)
    {
        target = getActualPath(target, getDefaultPrintPath(book));

        // Actually write the PDF
        try {
            new BookPdfOutput(book, target.toFile()).write();
            book.setPrintPath(target);
            logger.info("Score printed to {}", target);

            // Remember (even across runs) the selected directory
            constants.defaultPrintDirectory.setValue(target.getParent().toString());
        } catch (Exception ex) {
            logger.warn("Cannot write PDF to " + target, ex);
        }
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
                true,
                "Should we use Opus notion for export (rather than sub-folders)?");

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
