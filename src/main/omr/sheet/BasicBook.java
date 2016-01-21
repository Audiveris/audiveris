//--------------------------------------------------------------------------------------------------------------------//
//                                                                                                                    //
//                                                  B a s i c B o o k                                                 //
//                                                                                                                    //
//--------------------------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.OMR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.image.FilterDescriptor;
import omr.image.ImageLoading;

import omr.run.RunTable;

import omr.score.OpusExporter;
import omr.score.Page;
import omr.score.Score;
import omr.score.ScoreExporter;
import omr.score.ScoreReduction;
import omr.score.ui.BookPdfOutput;

import omr.script.BookStepTask;
import omr.script.ExportTask;
import omr.script.PrintTask;
import omr.script.Script;
import static omr.sheet.Sheet.INTERNALS_RADIX;
import omr.sheet.rhythm.Voices;
import omr.sheet.ui.BookBrowser;
import omr.sheet.ui.StubsController;

import omr.step.ProcessingCancellationException;
import omr.step.Step;
import omr.step.StepException;
import static omr.step.ui.StepMonitoring.notifyStart;
import static omr.step.ui.StepMonitoring.notifyStop;

import omr.text.Language;

import omr.util.FileUtil;
import omr.util.Memory;
import omr.util.OmrExecutors;
import omr.util.Param;
import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.zip.ZipOutputStream;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code BasicBook} is a basic implementation of Book interface.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "book")
public class BasicBook
        implements Book
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            Book.class);

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Sub books, if any. */
    @XmlElement(name = "sub-books")
    private final List<Book> subBooks;

    /** Input path of the related image(s) file, if any. */
    @XmlAttribute(name = "path")
    private final Path path;

    /** Sheet offset of image file with respect to full work, if any. */
    @XmlAttribute(name = "offset")
    private Integer offset;

    /** Sequence of all sheets stubs got from image file. */
    @XmlElement(name = "sheet")
    private final List<SheetStub> stubs = new ArrayList<SheetStub>();

    /** Logical scores for this book. */
    @XmlElement(name = "score")
    private final List<Score> scores = new ArrayList<Score>();

    // Transient data
    //---------------
    //
    /** The related file radix (file name without extension). */
    private String radix;

    /** File path where the project is kept. */
    private Path projectPath;

    /** File path where the book is printed. */
    private Path printPath;

    /** File path where the script is stored. */
    private Path scriptPath;

    /** The script of user actions on this book. */
    private Script script;

    /** File path (without extension) where the MusicXML output is stored. */
    private Path exportPathSansExt;

    /** Handling of binarization filter parameter. */
    private final Param<FilterDescriptor> filterParam = new Param<FilterDescriptor>(
            FilterDescriptor.defaultFilter);

    /** Handling of dominant language(s) parameter. */
    private final Param<String> languageParam = new Param<String>(Language.defaultSpecification);

    /** Browser on this book. */
    private BookBrowser bookBrowser;

    /** Flag to indicate this book is being closed. */
    private volatile boolean closing;

    /** Set if the book itself has been modified. */
    private boolean modified = false;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a Book with a path to an input images file.
     *
     * @param path the input image path (which may contain several images)
     */
    public BasicBook (Path path)
    {
        Objects.requireNonNull(path, "Trying to create a Book with null path");

        this.path = path;
        subBooks = null;

        initTransients(FileUtil.getNameSansExtension(path), null);
    }

    /**
     * Create a meta Book, to be later populated with sub-books.
     * <p>
     * NOTA: This meta-book feature is not yet in use.
     *
     * @param radix a radix name for this book
     */
    public BasicBook (String radix)
    {
        Objects.requireNonNull(radix, "Trying to create a meta Book with null radix");

        path = null;
        subBooks = new ArrayList<Book>();

        initTransients(radix, null);
    }

    /**
     * No-arg constructor needed by JAXB.
     */
    public BasicBook ()
    {
        path = null;
        subBooks = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // closeFileSystem //
    //-----------------//
    /**
     * Close the provided (project) file system.
     *
     * @param fileSystem the project file system
     */
    public static void closeFileSystem (FileSystem fileSystem)
    {
        try {
            fileSystem.close();

            logger.info("Project file system closed.");
        } catch (Exception ex) {
            logger.warn("Could not close project file system " + ex, ex);
        }
    }

    //-------------//
    // buildScores //
    //-------------//
    @Override
    public void buildScores ()
    {
        scores.clear();

        // Current score
        Score currentScore = null;

        // Group all sheets pages into scores
        for (SheetStub stub : getValidStubs()) {
            Sheet sheet = stub.getSheet();

            for (Page page : sheet.getPages()) {
                if (page.isMovementStart()) {
                    scores.add(currentScore = new Score());
                    currentScore.setBook(this);
                    currentScore.setId(scores.size());
                } else if (currentScore == null) {
                    scores.add(currentScore = new Score());
                    currentScore.setBook(this);
                    currentScore.setId(scores.size());
                }

                currentScore.addPage(page);
            }
        }

        for (Score score : scores) {
            // Merges pages into their containing movement score (connecting the parts across pages)
            // TODO: this may need the addition of dummy parts in some pages
            new ScoreReduction(score).reduce();
            //
            //            for (Page page : score.getPages()) {
            //                //                // - Retrieve the actual duration of every measure
            //                //                page.accept(new DurationRetriever());
            //                //
            //                //                // - Check all voices timing, assign forward items if needed.
            //                //                // - Detect special measures and assign proper measure ids
            //                //                // If needed, we can trigger a reprocessing of this page
            //                //                page.accept(new MeasureFixer());
            //                //
            //                // Check whether time signatures are consistent accross all pages in score
            //                // TODO: to be implemented
            //                //
            //                // Connect slurs across pages
            //                page.getFirstSystem().connectPageInitialSlurs(score);
            //            }

            // Voices connection
            Voices.refineScore(score);
        }

        setModified(true);

        logger.info("Scores built: {}", scores.size());
    }

    //-------//
    // close //
    //-------//
    @Override
    public void close ()
    {
        setClosing(true);

        // Close contained stubs/sheets
        if (OMR.getGui() != null) {
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    for (SheetStub stub : new ArrayList<SheetStub>(stubs)) {
                        // Close stub UI, if any
                        if (stub.getAssembly() != null) {
                            StubsController.getInstance().deleteAssembly(stub);
                            stub.getAssembly().close();
                        }
                    }
                }
            });
        }

        // Close browser if any
        if (bookBrowser != null) {
            bookBrowser.close();
        }

        // Remove from OMR instances
        OMR.getEngine().removeBook(this);

        // Time for some cleanup...
        Memory.gc();

        logger.info("{} closed.", this);
    }

    //-------------//
    // createStubs //
    //-------------//
    @Override
    public void createStubs (SortedSet<Integer> sheetNumbers)
    {
        ImageLoading.Loader loader = ImageLoading.getLoader(path);

        if (loader != null) {
            final int imageCount = loader.getImageCount();
            loader.dispose();

            if (sheetNumbers == null) {
                sheetNumbers = new TreeSet<Integer>();
            }

            if (sheetNumbers.isEmpty()) {
                for (int i = 1; i <= imageCount; i++) {
                    sheetNumbers.add(i);
                }
            }

            for (int num : sheetNumbers) {
                stubs.add(new BasicStub(this, num));
            }
        }
    }

    //-----------------//
    // createStubsTabs //
    //-----------------//
    @Override
    public void createStubsTabs ()
    {
        SwingUtilities.invokeLater(
                new Runnable()
        {
            @Override
            public void run ()
            {
                final StubsController controller = StubsController.getInstance();

                // Allocate one tab per stub
                for (SheetStub stub : stubs) {
                    controller.addAssembly(stub.getAssembly());
                }

                controller.adjustStubTabs(BasicBook.this);

                // Focus on first valid stub, if any
                SheetStub validStub = getFirstValidStub();

                if (validStub != null) {
                    controller.showAssembly(validStub);
                } else {
                    logger.info("No valid sheet in {}", this);
                }
            }
        });
    }

    //--------------//
    // deleteExport //
    //--------------//
    @Override
    public void deleteExport ()
    {
        // Determine the output path for the provided book: path/to/scores/Book
        Path bookPathSansExt = BookManager.getActualPath(
                getExportPathSansExt(),
                BookManager.getDefaultExportPathSansExt(this));

        // One-sheet book: <bookname>.mxl
        // One-sheet book: <bookname>.mvt<M>.mxl
        // One-sheet book: <bookname>/... (perhaps some day: 1 directory per book)
        //
        // Multi-sheet book: <bookname>-sheet#<N>.mxl
        // Multi-sheet book: <bookname>-sheet#<N>.mvt<M>.mxl
        final Path folder = isMultiSheet() ? bookPathSansExt : bookPathSansExt.getParent();
        final Path bookName = bookPathSansExt.getFileName(); // bookname

        final String dirGlob = "glob:**/" + bookName + "{/**,}";
        final String filGlob = "glob:**/" + bookName + "{/**,.*}";
        final List<Path> paths = FileUtil.walkDown(folder, dirGlob, filGlob);

        if (!paths.isEmpty()) {
            BookManager.deletePaths(bookName + " deletion", paths);
        } else {
            logger.info("Nothing to delete");
        }
    }

    //-----------------//
    // displayAllStubs //
    //-----------------//
    @Override
    public void displayAllStubs ()
    {
        SwingUtilities.invokeLater(
                new Runnable()
        {
            @Override
            public void run ()
            {
                final StubsController controller = StubsController.getInstance();
                controller.displayAllStubs(BasicBook.this);
            }
        });
    }

    //--------//
    // doStep //
    //--------//
    @Override
    public boolean doStep (final Step target,
                           SortedSet<Integer> sheetIds)
    {
        logger.debug("doStep {} on {}", target, this);

        try {
            if (target == null) {
                return true; // Nothing to do
            }

            // Find the least advanced step performed across all book sheets
            Step least = Step.last();

            for (SheetStub stub : getStubs()) {
                Step latest = stub.getLatestStep();

                if (latest == null) {
                    // This sheet has not been processed at all
                    least = null;

                    break;
                }

                if (latest.compareTo(least) < 0) {
                    least = latest;
                }
            }

            if ((least != null) && (least.compareTo(target) >= 0)) {
                return true; // Nothing to do
            }

            // Launch the steps on each sheet
            logger.info("{}Book processing {}", getLogPrefix(), target);

            long startTime = System.currentTimeMillis();

            try {
                notifyStart();

                if (isMultiSheet()) {
                    boolean failure = false;

                    if (OmrExecutors.defaultParallelism.getTarget() == true) {
                        // Process all sheets in parallel
                        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>();

                        for (final SheetStub stub : new ArrayList<SheetStub>(stubs)) {
                            if (!stub.isDone(target)) {
                                tasks.add(
                                        new Callable<Void>()
                                {
                                    @Override
                                    public Void call ()
                                            throws StepException
                                    {
                                        stub.ensureStep(target);

                                        return null;
                                    }
                                });
                            }
                        }

                        try {
                            List<Future<Void>> futures = OmrExecutors.getCachedLowExecutor()
                                    .invokeAll(tasks);

                            for (Future future : futures) {
                                try {
                                    future.get();
                                } catch (Exception ex) {
                                    logger.warn("Future exception", ex);
                                    failure = true;
                                }
                            }

                            return !failure;
                        } catch (InterruptedException ex) {
                            logger.warn("Error in parallel doScoreStepSet", ex);
                            failure = true;
                        }
                    } else {
                        // Process one sheet after the other
                        for (SheetStub stub : new ArrayList<SheetStub>(stubs)) {
                            if (!stub.isDone(target)) {
                                if (!stub.ensureStep(target)) {
                                    failure = true;
                                }
                            }
                        }
                    }

                    return !failure;
                } else {
                    // Process the single sheet
                    return stubs.get(0).ensureStep(target);
                }
            } finally {
                notifyStop();

                long stopTime = System.currentTimeMillis();
                logger.debug("End of step set in {} ms.", (stopTime - startTime));

                // Record the step tasks to script
                getScript().addTask(new BookStepTask(target));
            }
        } catch (ProcessingCancellationException pce) {
            throw pce;
        } catch (Exception ex) {
            logger.warn("Error in performing " + target, ex);
        }

        return false;
    }

    //--------//
    // export //
    //--------//
    @Override
    public void export ()
    {
        // path/to/scores/Book
        Path bookPathSansExt = BookManager.getActualPath(
                getExportPathSansExt(),
                BookManager.getDefaultExportPathSansExt(this));

        final boolean compressed = BookManager.useCompression();
        final String ext = compressed ? OMR.COMPRESSED_SCORE_EXTENSION : OMR.SCORE_EXTENSION;
        final boolean sig = BookManager.useSignature();

        // Export each movement score
        String bookName = bookPathSansExt.getFileName().toString();
        final boolean multiMovements = scores.size() > 1;

        if (BookManager.useOpus()) {
            // Export the book as one opus file
            final Path opusPath = bookPathSansExt.resolveSibling(bookName + OMR.OPUS_EXTENSION);

            try {
                makeReadyForExport();
                new OpusExporter(this).export(opusPath, bookName, sig);
                BookManager.setDefaultExportFolder(bookPathSansExt.getParent().toString());
            } catch (Exception ex) {
                logger.warn("Could not export opus " + opusPath, ex);
            }
        } else {
            // Check all target file(s) can be (over)written
            for (Score score : scores) {
                final String scoreName = (!multiMovements) ? bookName
                        : (bookName + OMR.MOVEMENT_EXTENSION + score.getId());
                final Path scorePath = bookPathSansExt.resolveSibling(scoreName + ext);
            }

            // Do export the book as one or several movement files
            makeReadyForExport();

            for (Score score : scores) {
                final String scoreName = (!multiMovements) ? bookName
                        : (bookName + OMR.MOVEMENT_EXTENSION + score.getId());
                final Path scorePath = bookPathSansExt.resolveSibling(scoreName + ext);

                try {
                    new ScoreExporter(score).export(scorePath, scoreName, sig, compressed);
                    BookManager.setDefaultExportFolder(bookPathSansExt.getParent().toString());
                } catch (Exception ex) {
                    logger.warn("Could not export score " + scoreName, ex);
                }
            }
        }

        // Save task into book script
        getScript().addTask(new ExportTask(bookPathSansExt, null));
    }

    //-----------------//
    // getBrowserFrame //
    //-----------------//
    @Override
    public JFrame getBrowserFrame ()
    {
        if (bookBrowser == null) {
            // Build the BookBrowser on the score
            bookBrowser = new BookBrowser(this);
        }

        return bookBrowser.getFrame();
    }

    //----------------------//
    // getExportPathSansExt //
    //----------------------//
    @Override
    public Path getExportPathSansExt ()
    {
        return exportPathSansExt;
    }

    //----------------//
    // getFilterParam //
    //----------------//
    @Override
    public Param<FilterDescriptor> getFilterParam ()
    {
        return filterParam;
    }

    //-------------------//
    // getFirstValidStub //
    //-------------------//
    @Override
    public SheetStub getFirstValidStub ()
    {
        for (SheetStub stub : stubs) {
            if (stub.isValid()) {
                return stub;
            }
        }

        return null; // No valid stub found!
    }

    //--------------//
    // getInputPath //
    //--------------//
    @Override
    public Path getInputPath ()
    {
        return path;
    }

    //------------------//
    // getLanguageParam //
    //------------------//
    @Override
    public Param<String> getLanguageParam ()
    {
        return languageParam;
    }

    //--------------//
    // getLogPrefix //
    //--------------//
    @Override
    public String getLogPrefix ()
    {
        if (BookManager.isMultiBook()) {
            return "[" + radix + "] ";
        } else {
            return "";
        }
    }

    //-----------//
    // getOffset //
    //-----------//
    @Override
    public Integer getOffset ()
    {
        return offset;
    }

    //--------------//
    // getPrintPath //
    //--------------//
    @Override
    public Path getPrintPath ()
    {
        return printPath;
    }

    //----------------//
    // getProjectPath //
    //----------------//
    @Override
    public Path getProjectPath ()
    {
        return projectPath;
    }

    //----------//
    // getRadix //
    //----------//
    @Override
    public String getRadix ()
    {
        return radix;
    }

    //-----------//
    // getScores //
    //-----------//
    @Override
    public List<Score> getScores ()
    {
        return Collections.unmodifiableList(scores);
    }

    //-----------//
    // getScript //
    //-----------//
    @Override
    public Script getScript ()
    {
        if (script == null) {
            script = new Script(this);
        }

        return script;
    }

    //---------------//
    // getScriptPath //
    //---------------//
    @Override
    public Path getScriptPath ()
    {
        return scriptPath;
    }

    //---------//
    // getStub //
    //---------//
    @Override
    public SheetStub getStub (int sheetId)
    {
        return stubs.get(sheetId - 1);
    }

    //----------//
    // getStubs //
    //----------//
    @Override
    public List<SheetStub> getStubs ()
    {
        return Collections.unmodifiableList(stubs);
    }

    //---------------//
    // getValidStubs //
    //---------------//
    @Override
    public List<SheetStub> getValidStubs ()
    {
        List<SheetStub> valids = new ArrayList<SheetStub>();

        for (SheetStub stub : stubs) {
            if (stub.isValid()) {
                valids.add(stub);
            }
        }

        return valids;
    }

    //------------------//
    // hideInvalidStubs //
    //------------------//
    @Override
    public void hideInvalidStubs ()
    {
        SwingUtilities.invokeLater(
                new Runnable()
        {
            @Override
            public void run ()
            {
                final StubsController controller = StubsController.getInstance();

                for (SheetStub stub : stubs) {
                    if (!stub.isValid()) {
                        controller.removeAssembly(stub);
                    }
                }
            }
        });
    }

    //-------------//
    // includeBook //
    //-------------//
    @Override
    public void includeBook (Book book)
    {
        subBooks.add(book);
    }

    //-----------//
    // isClosing //
    //-----------//
    @Override
    public boolean isClosing ()
    {
        return closing;
    }

    //------------//
    // isModified //
    //------------//
    @Override
    public boolean isModified ()
    {
        if (modified) {
            return true; // The book itself is modified
        }

        for (SheetStub stub : stubs) {
            if (stub.isModified()) {
                return true; // This sheet is modified
            }
        }

        return false;
    }

    //--------------//
    // isMultiSheet //
    //--------------//
    @Override
    public boolean isMultiSheet ()
    {
        return stubs.size() > 1;
    }

    //-------------//
    // loadProject //
    //-------------//
    /**
     * Load a book out of a provided project file.
     *
     * @param projectPath path to the (zipped) project file
     * @return the loaded book if successful
     */
    public static Book loadProject (Path projectPath)
    {
        StopWatch watch = new StopWatch("loadProject " + projectPath);

        try {
            logger.info("Loading project {} ...", projectPath);
            watch.start("book");

            // Open project file
            Path rootPath = openProjectFile(projectPath);

            // Load book internals (just the stubs)
            Path bookPath = rootPath.resolve(Book.BOOK_INTERNALS);
            InputStream is = Files.newInputStream(bookPath, StandardOpenOption.READ);

            Unmarshaller um = getJaxbContext().createUnmarshaller();
            final BasicBook book = (BasicBook) um.unmarshal(is);
            book.initTransients(null, projectPath);
            is.close();
            rootPath.getFileSystem().close(); // Close project file

            return book;
        } catch (Exception ex) {
            logger.warn("Error loading project " + projectPath + " " + ex, ex);

            return null;
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //----------------//
    // loadSheetImage //
    //----------------//
    @Override
    public BufferedImage loadSheetImage (int id)
    {
        try {
            final ImageLoading.Loader loader = ImageLoading.getLoader(path);

            if (loader == null) {
                return null;
            }

            BufferedImage img = loader.getImage(id);
            logger.debug("{} loaded sheet#{} {}x{}", this, id, img.getWidth(), img.getHeight());

            loader.dispose();

            return img;
        } catch (IOException ex) {
            logger.warn("Error in book.readImage", ex);

            return null;
        }
    }

    //-----------------//
    // openProjectFile //
    //-----------------//
    /**
     * Open the project file (supposed to already exist at location provided by
     * '{@code projectPath}' parameter) for reading or writing.
     * <p>
     * When IO operations are finished, the project file must be closed via
     * {@link #closeFileSystem(java.nio.file.FileSystem)}
     *
     * @param projectPath project path name
     * @return the root path of the (zipped) project file system
     */
    public static Path openProjectFile (Path projectPath)
    {
        if (projectPath == null) {
            throw new IllegalStateException("projectPath is null");
        }

        try {
            logger.debug("Project file system opened");

            FileSystem fileSystem = FileSystems.newFileSystem(projectPath, null);

            return fileSystem.getPath(fileSystem.getSeparator());
        } catch (FileNotFoundException ex) {
            logger.warn("File not found: " + projectPath, ex);
        } catch (IOException ex) {
            logger.warn("Error reading project:" + projectPath, ex);
        }

        return null;
    }

    //-----------------//
    // openProjectFile //
    //-----------------//
    /**
     * Open the project file (supposed to already exist at location provided by
     * '{@code projectPath}' member) for reading or writing.
     * <p>
     * When IO operations are finished, the project file must be closed via
     * {@link #closeFileSystem(java.nio.file.FileSystem)}
     *
     * @return the root path of the (zipped) project file system
     */
    public Path openProjectFile ()
    {
        return openProjectFile(projectPath);
    }

    //-----------------//
    // openSheetFolder //
    //-----------------//
    @Override
    public Path openSheetFolder (int number)
    {
        Path root = openProjectFile();

        return root.resolve(INTERNALS_RADIX + number);
    }

    //-------//
    // print //
    //-------//
    @Override
    public void print ()
    {
        // Path to print file
        final Path pdfPath = BookManager.getActualPath(
                getPrintPath(),
                BookManager.getDefaultPrintPath(this));

        try {
            new BookPdfOutput(this, pdfPath.toFile()).write(null);
            logger.info("Book printed to {}", pdfPath);

            setPrintPath(pdfPath);
            BookManager.setDefaultPrintFolder(pdfPath.getParent().toString());
            getScript().addTask(new PrintTask(pdfPath, null));
        } catch (Exception ex) {
            logger.warn("Cannot write PDF to " + pdfPath, ex);
        }
    }

    //------------//
    // removeStub //
    //------------//
    @Override
    public boolean removeStub (SheetStub stub)
    {
        return stubs.remove(stub);
    }

    //------------//
    // setClosing //
    //------------//
    @Override
    public void setClosing (boolean closing)
    {
        this.closing = closing;
    }

    //----------------------//
    // setExportPathSansExt //
    //----------------------//
    @Override
    public void setExportPathSansExt (Path exportPathSansExt)
    {
        this.exportPathSansExt = exportPathSansExt;
    }

    //-------------//
    // setModified //
    //-------------//
    @Override
    public void setModified (boolean val)
    {
        ///logger.info("{} setModified {}", this, val);
        this.modified = val;

        if (OMR.getGui() != null) {
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    StubsController.getInstance().refresh();
                }
            });
        }
    }

    //-----------//
    // setOffset //
    //-----------//
    @Override
    public void setOffset (Integer offset)
    {
        this.offset = offset;

        if (script != null) {
            // Forward to related script
            script.setOffset(offset);
        }
    }

    //--------------//
    // setPrintPath //
    //--------------//
    @Override
    public void setPrintPath (Path printPath)
    {
        this.printPath = printPath;
    }

    //---------------//
    // setScriptPath //
    //---------------//
    @Override
    public void setScriptPath (Path scriptPath)
    {
        this.scriptPath = scriptPath;
    }

    //-------//
    // store //
    //-------//
    @Override
    public void store (Path projectPath)
    {
        Memory.gc(); // Launch garbage collection, to save on weak glyph references ...

        try {
            final Path root;
            checkRadixChange(projectPath);
            logger.info("{}.store", this);

            if ((this.projectPath == null)
                || this.projectPath.toAbsolutePath().equals(projectPath.toAbsolutePath())) {
                if (this.projectPath == null) {
                    root = createProjectFile(projectPath);
                } else {
                    root = openProjectFile(projectPath);
                }

                storeBookXml(root); // Book itself (book.xml)

                // Contained sheets
                for (SheetStub stub : stubs) {
                    if (stub.isModified()) {
                        final Path sheetPath = root.resolve(INTERNALS_RADIX + stub.getNumber());
                        stub.getSheet().store(sheetPath, null);
                    }
                }
            } else {
                // (Store as): Switch from old to new project file
                root = createProjectFile(projectPath);

                storeBookXml(root); // Book itself (book.xml)

                // Contained sheets
                final Path oldRoot = openProjectFile(this.projectPath);

                for (SheetStub stub : stubs) {
                    final Path oldSheetPath = oldRoot.resolve(INTERNALS_RADIX + stub.getNumber());
                    final Path sheetPath = root.resolve(INTERNALS_RADIX + stub.getNumber());

                    if (stub.isModified()) {
                        stub.getSheet().store(sheetPath, oldSheetPath);
                    } else if (Files.exists(oldSheetPath)) {
                        FileUtil.copyTree(oldSheetPath, sheetPath);
                    }
                }

                oldRoot.getFileSystem().close(); // Close old project file
            }

            root.getFileSystem().close();
            this.projectPath = projectPath;

            BookManager.getInstance().getProjectHistory().add(projectPath); // Insert in history
            BookManager.setDefaultProjectFolder(projectPath.getParent().toString());

            logger.info("{} stored into {}", this, projectPath);
        } catch (Throwable ex) {
            logger.warn("Error storing " + this + " to " + projectPath + " ex:" + ex, ex);
        }
    }

    //-------//
    // store //
    //-------//
    @Override
    public void store ()
    {
        if (projectPath == null) {
            logger.warn("Projectpath not defined");
        } else {
            store(projectPath);
        }
    }

    //---------------//
    // swapAllSheets //
    //---------------//
    @Override
    public void swapAllSheets ()
    {
        if (isModified()) {
            logger.info("{} storing", this);
            store();
        }

        SheetStub currentStub = null;

        if (OMR.getGui() != null) {
            currentStub = StubsController.getCurrentStub();
        }

        for (SheetStub stub : stubs) {
            if (stub != currentStub) {
                stub.swapSheet();
            }
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Book{");
        sb.append(radix);

        if ((offset != null) && (offset > 0)) {
            sb.append(" offset:").append(offset);
        }

        sb.append("}");

        return sb.toString();
    }

    //------------//
    // transcribe //
    //------------//
    @Override
    public boolean transcribe (SortedSet<Integer> sheetIds)
    {
        return doStep(Step.last(), sheetIds);
    }

    //-------------------//
    // createProjectFile //
    //-------------------//
    /**
     * Create a new project file system dedicated to this book at the location provided
     * by '{@code projectpath}' member.
     * If such file already exists, it is deleted beforehand.
     * <p>
     * When IO operations are finished, the project file must be closed via
     * {@link #closeFileSystem(java.nio.file.FileSystem)}
     *
     * @return the root path of the (zipped) project file system
     */
    private static Path createProjectFile (Path projectPath)
    {
        if (projectPath == null) {
            throw new IllegalStateException("projectPath is null");
        }

        try {
            if (Files.exists(projectPath)) {
                logger.debug("Project {} found, to be deleted ", projectPath);
            } else {
                logger.debug("Project file not found, creating {}", projectPath);
            }

            Files.deleteIfExists(projectPath);
        } catch (IOException ex) {
            logger.warn("Error deleting project: " + projectPath, ex);
        }

        try {
            // Make sure the containing folder exists
            Files.createDirectories(projectPath.getParent());

            //TODO: Is this write really mandatory to make it a zip file?
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(projectPath.toFile()));
            zos.close();
        } catch (IOException ex) {
            logger.warn("Error creating project:" + projectPath, ex);
        }

        // Finally open the project file just created
        return openProjectFile(projectPath);
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(BasicBook.class, RunTable.class);
        }

        return jaxbContext;
    }

    //------------------//
    // checkRadixChange //
    //------------------//
    /**
     * If the (new) project name does not match current one, update the book radix
     * (and the title of first displayed sheet if any).
     *
     * @param projectPath new project target path
     */
    private void checkRadixChange (Path projectPath)
    {
        // Are we changing the target name WRT the default name?
        final String newRadix = FileUtil.avoidExtensions(
                projectPath.getFileName(),
                OMR.PROJECT_EXTENSION).toString();

        if (!newRadix.equals(radix)) {
            // Update book radix
            radix = newRadix;

            // We are really changing the radix, so nullify all other paths
            exportPathSansExt = printPath = scriptPath = null;

            if (OMR.getGui() != null) {
                // Update UI first sheet tab
                SwingUtilities.invokeLater(
                        new Runnable()
                {
                    @Override
                    public void run ()
                    {
                        StubsController.getInstance().updateFirstStubTitle(BasicBook.this);
                    }
                });
            }
        }
    }

    //----------------//
    // initTransients //
    //----------------//
    private void initTransients (String radix,
                                 Path projectPath)
    {
        if (radix != null) {
            this.radix = radix;
        }

        if (projectPath != null) {
            this.projectPath = projectPath;

            if (radix == null) {
                this.radix = FileUtil.getNameSansExtension(projectPath);
            }
        }
    }

    //--------------------//
    // makeReadyForExport //
    //--------------------//
    private void makeReadyForExport ()
    {
        // Make sure all sheets have been transcribed
        for (SheetStub stub : getValidStubs()) {
            stub.ensureStep(Step.PAGE);
        }

        // Group book pages into scores, if not already done
        if (scores.isEmpty()) {
            buildScores();
        }
    }

    //--------------//
    // storeBookXml //
    //--------------//
    /**
     * Store the book internals into book.xml file.
     *
     * @param root root pat of project file system
     * @throws IOException
     * @throws JAXBException
     */
    private void storeBookXml (Path root)
            throws IOException, JAXBException
    {
        Path bookPath = root.resolve(Book.BOOK_INTERNALS);
        Files.deleteIfExists(bookPath);

        OutputStream os = Files.newOutputStream(bookPath, StandardOpenOption.CREATE);
        Marshaller m = getJaxbContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(this, os);
        os.close();
        setModified(false);
        logger.info("Stored {}", bookPath);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    /**
     * Meant for JAXB handling of Book interface.
     */
    public static class Adapter
            extends XmlAdapter<BasicBook, Book>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public BasicBook marshal (Book s)
        {
            return (BasicBook) s;
        }

        @Override
        public Book unmarshal (BasicBook s)
        {
            return s;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch for book loading?");
    }
}
