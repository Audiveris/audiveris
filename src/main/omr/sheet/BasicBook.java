//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B a s i c B o o k                                       //
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

import omr.OMR;
import omr.ProgramId;

import omr.classifier.SampleRepository;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.image.FilterDescriptor;
import omr.image.ImageLoading;

import omr.log.LogUtil;

import omr.run.RunTable;

import omr.score.OpusExporter;
import omr.score.PageRef;
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
import omr.util.Zip;

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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
    /** Related Audiveris version. */
    @XmlAttribute(name = "software-version")
    private String version;

    /** Related Audiveris revision. */
    @XmlAttribute(name = "software-revision")
    private String revision;

    /** Sub books, if any. */
    @XmlElement(name = "sub-books")
    private final List<Book> subBooks;

    /** Book alias, if any. */
    @XmlAttribute(name = "alias")
    private String alias;

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
    /** Project file lock. */
    private final Lock lock = new ReentrantLock();

    /** The related file radix (file name without extension). */
    private String radix;

    /** File path where the book is kept. */
    private Path bookPath;

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

    /** Book-level sample repository. */
    private SampleRepository repository;

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
     * @param nameSansExt a name (sans extension) for this book
     */
    public BasicBook (String nameSansExt)
    {
        Objects.requireNonNull(nameSansExt, "Trying to create a meta Book with null name");

        path = null;
        subBooks = new ArrayList<Book>();

        initTransients(nameSansExt, null);
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
    //------------//
    // checkAlias //
    //------------//
    private static String checkAlias (Path path)
    {
        // Alias?
        if (AliasPatterns.useAliasPatterns()) {
            final String nameSansExt = FileUtil.getNameSansExtension(path);
            return BookManager.getInstance().getAlias(nameSansExt);

        }
        return null;
    }

    //-------------//
    // buildScores //
    //-------------//
    @Override
    public void buildScores ()
    {
        for (Score score : scores) {
            // (re) build the score logical parts
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
        if (OMR.gui != null) {
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    try {
                        LogUtil.start(BasicBook.this);

                        for (SheetStub stub : new ArrayList<SheetStub>(stubs)) {
                            LogUtil.start(stub);

                            // Close stub UI, if any
                            if (stub.getAssembly() != null) {
                                StubsController.getInstance().deleteAssembly(stub);
                                stub.getAssembly().close();
                            }
                        }
                    } finally {
                        LogUtil.stopBook();
                    }
                }
            });
        }

        // Close browser if any
        if (bookBrowser != null) {
            bookBrowser.close();
        }

        // Remove from OMR instances
        OMR.engine.removeBook(this);

        // Time for some cleanup...
        Memory.gc();

        logger.info("Book closed.");
    }

    //-----------------//
    // closeFileSystem //
    //-----------------//
    /**
     * Close the provided (book) file system.
     *
     * @param fileSystem the book file system
     */
    public static void closeFileSystem (FileSystem fileSystem)
    {
        try {
            fileSystem.close();

            logger.info("Book file system closed.");
        } catch (Exception ex) {
            logger.warn("Could not close book file system " + ex, ex);
        }
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
            logger.info("{} sheet{} in {}", imageCount, ((imageCount > 1) ? "s" : ""), path);

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
    public void createStubsTabs (final Integer focus)
    {
        Runnable doRun = new Runnable()
        {
            @Override
            public void run ()
            {
                try {
                    LogUtil.start(BasicBook.this);

                    final StubsController controller = StubsController.getInstance();

                    // Determine which stub should get the focus
                    SheetStub focusStub = null;

                    if (focus != null) {
                        if ((focus > 0) && (focus <= stubs.size())) {
                            focusStub = stubs.get(focus - 1);
                        } else {
                            logger.warn("Illegal focus id: {}", focus);
                        }
                    }

                    if (focusStub == null) {
                        focusStub = getFirstValidStub(); // Focus on first valid stub, if any

                        if (focusStub == null) {
                            logger.info("No valid sheet in {}", this);
                        }
                    }

                    // Allocate one tab per stub, beginning by focusStub if any
                    Integer focusIndex = null;

                    if (focusStub != null) {
                        controller.addAssembly(focusStub.getAssembly(), null);
                        focusIndex = controller.getIndex(focusStub);

                        if (focusIndex == -1) {
                            focusIndex = null; // Safer
                        }
                    }

                    for (SheetStub stub : stubs) {
                        if (focusIndex == null) {
                            controller.addAssembly(stub.getAssembly(), null);
                        } else if (stub != focusStub) {
                            if (stub.getNumber() < focusStub.getNumber()) {
                                controller.addAssembly(
                                        stub.getAssembly(),
                                        controller.getLastIndex());
                            } else {
                                controller.addAssembly(stub.getAssembly(), null);
                            }
                        }
                    }

                    controller.adjustStubTabs(BasicBook.this);
                } finally {
                    LogUtil.stopBook();
                }
            }
        };

        try {
            SwingUtilities.invokeAndWait(doRun);
        } catch (Exception ex) {
            logger.warn("Error in createStubsTabs, {}", ex, ex);
        }
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
                } catch (Exception ex) {
                    logger.warn("Could not export score " + scoreName, ex);
                }
            }
        }

        // Save task into book script
        getScript().addTask(new ExportTask(bookPathSansExt, null));
    }

    //----------//
    // getAlias //
    //----------//
    /**
     * @return the alias
     */
    @Override
    public String getAlias ()
    {
        return alias;
    }

    //-------------//
    // getBookPath //
    //-------------//
    @Override
    public Path getBookPath ()
    {
        return bookPath;
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

    //---------//
    // getLock //
    //---------//
    @Override
    public Lock getLock ()
    {
        return lock;
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

    //----------//
    // getRadix //
    //----------//
    @Override
    public String getRadix ()
    {
        return radix;
    }

    //-----------------------------//
    // getSpecificSampleRepository //
    //-----------------------------//
    @Override
    public SampleRepository getSpecificSampleRepository ()
    {
        if (repository == null) {
            repository = SampleRepository.getInstance(this, true);
        }

        return repository;
    }

    //-----------------------//
    // hasSpecificRepository //
    //-----------------------//
    @Override
    public boolean hasSpecificRepository ()
    {
        if (repository != null) {
            return true;
        }

        // Look for needed files
        return SampleRepository.repositoryExists(this);
    }

    //---------------------//
    // getSampleRepository //
    //---------------------//
    @Override
    public SampleRepository getSampleRepository ()
    {
        SampleRepository repo = getSpecificSampleRepository();

        if (repo != null) {
            return repo;
        }

        // No specific repository is possible, so use global
        return SampleRepository.getGlobalInstance(true);
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

    //------------------------//
    // hasAllocatedRepository //
    //------------------------//
    @Override
    public boolean hasAllocatedRepository ()
    {
        return repository != null;
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

    //-----//
    // ids //
    //-----//
    /**
     * Build a string with just the IDs of the stub collection.
     *
     * @param stubs the collection of stub instances
     * @return the string built
     */
    public static String ids (List<SheetStub> stubs)
    {
        if (stubs == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (SheetStub entity : stubs) {
            sb.append("#").append(entity.getNumber());
        }

        sb.append("]");

        return sb.toString();
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

        if (repository != null && repository.isModified()) {
            return true; // The book sample repository is modified
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

    //----------//
    // loadBook //
    //----------//
    /**
     * Load a book out of a provided book file.
     *
     * @param bookPath path to the (zipped) book file
     * @return the loaded book if successful
     */
    public static Book loadBook (Path bookPath)
    {
        StopWatch watch = new StopWatch("loadBook " + bookPath);
        BasicBook book = null;

        try {
            logger.info("Loading book {} ...", bookPath);
            watch.start("book");

            // Open book file
            Path rootPath = Zip.openFileSystem(bookPath);

            // Load book internals (just the stubs) out of book.xml
            Path internalsPath = rootPath.resolve(Book.BOOK_INTERNALS);
            InputStream is = Files.newInputStream(internalsPath, StandardOpenOption.READ);

            Unmarshaller um = getJaxbContext().createUnmarshaller();
            book = (BasicBook) um.unmarshal(is);
            book.getLock().lock();
            LogUtil.start(book);
            book.initTransients(null, bookPath);
            is.close();
            rootPath.getFileSystem().close(); // Close book file

            book.checkScore(); // TODO: remove ASAP

            return book;
        } catch (Exception ex) {
            logger.warn("Error loading book " + bookPath + " " + ex, ex);

            return null;
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }

            if (book != null) {
                book.getLock().unlock();
            }

            LogUtil.stopBook();
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
            logger.info("Loaded image size: {}x{}", img.getWidth(), img.getHeight());

            loader.dispose();

            return img;
        } catch (IOException ex) {
            logger.warn("Error in book.loadSheetImage", ex);

            return null;
        }
    }

    //--------------//
    // openBookFile //
    //--------------//
    /**
     * Open the book file (supposed to already exist at location provided by
     * '{@code bookPath}' parameter) for reading or writing.
     * <p>
     * When IO operations are finished, the book file must be closed via
     * {@link #closeFileSystem(java.nio.file.FileSystem)}
     *
     * @param bookPath book path name
     * @return the root path of the (zipped) book file system
     */
    public static Path openBookFile (Path bookPath)
    {
        if (bookPath == null) {
            throw new IllegalStateException("bookPath is null");
        }

        try {
            logger.debug("Book file system opened");

            FileSystem fileSystem = FileSystems.newFileSystem(bookPath, null);

            return fileSystem.getPath(fileSystem.getSeparator());
        } catch (FileNotFoundException ex) {
            logger.warn("File not found: " + bookPath, ex);
        } catch (IOException ex) {
            logger.warn("Error reading book:" + bookPath, ex);
        }

        return null;
    }

    //--------------//
    // openBookFile //
    //--------------//
    /**
     * Open the book file (supposed to already exist at location provided by
     * '{@code bookPath}' member) for reading or writing.
     * <p>
     * When IO operations are finished, the book file must be closed via
     * {@link #closeFileSystem(java.nio.file.FileSystem)}
     *
     * @return the root path of the (zipped) book file system
     * @throws java.io.IOException
     */
    public Path openBookFile ()
            throws IOException
    {
        return Zip.openFileSystem(bookPath);
    }

    //-----------------//
    // openSheetFolder //
    //-----------------//
    @Override
    public Path openSheetFolder (int number)
            throws IOException
    {
        Path root = openBookFile();

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
            LogUtil.start(BasicBook.this);
            new BookPdfOutput(BasicBook.this, pdfPath.toFile()).write(null);
            setPrintPath(pdfPath);
            getScript().addTask(new PrintTask(pdfPath, null));
        } catch (Exception ex) {
            logger.warn("Cannot write PDF to " + pdfPath, ex);
        } finally {
            LogUtil.stopBook();
        }
    }

    //---------------//
    // reachBookStep //
    //---------------//
    @Override
    public boolean reachBookStep (final Step target,
                                  final boolean force,
                                  final Set<Integer> sheetIds)
    {
        try {
            final List<SheetStub> concernedStubs = getConcernedStubs(sheetIds);
            logger.debug("reachStep {} force:{} sheetIds:{}", target, force, sheetIds);

            if (!force) {
                // Check against the least advanced step performed across all sheets concerned
                Step least = getLeastStep(concernedStubs);

                if ((least != null) && (least.compareTo(target) >= 0)) {
                    return true; // Nothing to do
                }
            }

            // Launch the steps on each sheet
            long startTime = System.currentTimeMillis();
            logger.info(
                    "Book reaching {}{} on sheets:{}",
                    target,
                    force ? " force" : "",
                    ids(concernedStubs));

            try {
                boolean someFailure = false;
                notifyStart();

                if (isMultiSheet()
                    && constants.processAllStubsInParallel.isSet()
                    && (OmrExecutors.defaultParallelism.getTarget() == true)) {
                    // Process all stubs in parallel
                    List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();

                    for (final SheetStub stub : concernedStubs) {
                        tasks.add(
                                new Callable<Boolean>()
                        {
                            @Override
                            public Boolean call ()
                                    throws StepException
                            {
                                LogUtil.start(stub);

                                try {
                                    return stub.reachStep(target, force);
                                } finally {
                                    LogUtil.stopStub();
                                }
                            }
                        });
                    }

                    try {
                        List<Future<Boolean>> futures = OmrExecutors.getCachedLowExecutor()
                                .invokeAll(tasks);

                        for (Future<Boolean> future : futures) {
                            try {
                                if (!future.get()) {
                                    someFailure = true;
                                }
                            } catch (Exception ex) {
                                logger.warn("Future exception", ex);
                                someFailure = true;
                            }
                        }

                        return !someFailure;
                    } catch (InterruptedException ex) {
                        logger.warn("Error in parallel reachBookStep", ex);
                        someFailure = true;
                    }
                } else {
                    // Process one stub after the other
                    for (SheetStub stub : concernedStubs) {
                        LogUtil.start(stub);

                        try {
                            if (stub.reachStep(target, force)) {
                                // At end of each sheet processing:
                                // Save sheet to disk (including global book info)
                                if (OMR.gui == null) {
                                    stub.swapSheet();
                                }

                                ///stub.storeSheet();
                            } else {
                                someFailure = true;
                            }
                        } catch (Exception ex) {
                            // Exception (such as timeout) raised on stub
                            // Let processing continue for the other stubs
                            logger.warn("Error processing stub");
                            someFailure = true;
                        } finally {
                            LogUtil.stopStub();
                        }
                    }
                }

                return !someFailure;
            } finally {
                LogUtil.stopStub();
                notifyStop();

                long stopTime = System.currentTimeMillis();
                logger.debug("End of step set in {} ms.", (stopTime - startTime));

                // Record the step task into script
                getScript().addTask(new BookStepTask(target));
            }
        } catch (ProcessingCancellationException pce) {
            throw pce;
        } catch (Exception ex) {
            logger.warn("Error in performing " + target, ex);
        }

        return false;
    }

    //------------//
    // removeStub //
    //------------//
    @Override
    public boolean removeStub (SheetStub stub)
    {
        return stubs.remove(stub);
    }

    //----------//
    // setAlias //
    //----------//
    /**
     * @param alias the alias to set
     */
    @Override
    public void setAlias (String alias)
    {
        this.alias = alias;
        radix = alias;
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

        if (OMR.gui != null) {
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    final StubsController controller = StubsController.getInstance();
                    final SheetStub stub = controller.getSelectedStub();

                    if ((stub != null) && (stub.getBook() == BasicBook.this)) {
                        controller.refresh();
                    }
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
    public void store (Path bookPath,
                       boolean withBackup)
    {
        Memory.gc(); // Launch garbage collection, to save on weak glyph references ...

        // Backup existing book file?
        if (withBackup && Files.exists(bookPath)) {
            Path backup = FileUtil.backup(bookPath);

            if (backup != null) {
                logger.info("Previous book file renamed as {}", backup);
            }
        }

        try {
            final Path root;
            getLock().lock();
            checkRadixChange(bookPath);
            logger.info("Storing book...");

            if ((this.bookPath == null)
                || this.bookPath.toAbsolutePath().equals(bookPath.toAbsolutePath())) {
                if (this.bookPath == null) {
                    root = Zip.createFileSystem(bookPath);
                } else {
                    root = Zip.openFileSystem(bookPath);
                }

                if (modified) {
                    storeBookInfo(root); // Book info (book.xml)
                }

                // Contained sheets
                for (SheetStub stub : stubs) {
                    if (stub.isModified()) {
                        final Path sheetFolder = root.resolve(INTERNALS_RADIX + stub.getNumber());
                        stub.getSheet().store(sheetFolder, null);
                    }
                }

                // Separate repository
                if (repository != null && repository.isModified()) {
                    repository.storeRepository();
                }
            } else {
                // (Store as): Switch from old to new book file
                root = createBookFile(bookPath);

                if (modified) {
                    storeBookInfo(root); // Book info (book.xml)
                }

                // Contained sheets
                final Path oldRoot = openBookFile(this.bookPath);

                for (SheetStub stub : stubs) {
                    final Path oldSheetPath = oldRoot.resolve(INTERNALS_RADIX + stub.getNumber());
                    final Path sheetPath = root.resolve(INTERNALS_RADIX + stub.getNumber());

                    if (stub.isModified()) {
                        stub.getSheet().store(sheetPath, oldSheetPath);
                    } else if (Files.exists(oldSheetPath)) {
                        FileUtil.copyTree(oldSheetPath, sheetPath);
                    }
                }

                oldRoot.getFileSystem().close(); // Close old book file
            }

            root.getFileSystem().close();
            this.bookPath = bookPath;

            BookManager.getInstance().getBookHistory().add(bookPath); // Insert in history

            logger.info("Book stored as {}", bookPath);
        } catch (Throwable ex) {
            logger.warn("Error storing " + this + " to " + bookPath + " ex:" + ex, ex);
        } finally {
            getLock().unlock();
        }
    }

    //-------//
    // store //
    //-------//
    @Override
    public void store ()
    {
        if (bookPath == null) {
            logger.warn("Bookpath not defined");
        } else {
            store(bookPath, false);
        }
    }

    //---------------//
    // storeBookInfo //
    //---------------//
    @Override
    public void storeBookInfo (Path root)
            throws Exception
    {
        Path bookInternals = root.resolve(Book.BOOK_INTERNALS);
        Files.deleteIfExists(bookInternals);

        OutputStream os = Files.newOutputStream(bookInternals, StandardOpenOption.CREATE);
        Marshaller m = getJaxbContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(this, os);
        os.close();

        setModified(false);
        logger.info("Stored {}", bookInternals);
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

        if (OMR.gui != null) {
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
    public boolean transcribe ()
    {
        return reachBookStep(Step.last(), false, null);
    }

    //--------------//
    // updateScores //
    //--------------//
    /**
     * {@inheritDoc}
     * <p>
     * The question is which scores should we update.
     * Clearing all and rebuilding all is OK for pageRefs of all scores without loading sheets.
     * But doing so, we lose logicalPart information of <b>all</b> scores, and to rebuild it we'll
     * need to reload all valid sheets.
     * <p>
     * A better approach is to check the stub before and the stub after the current one.
     * This may result in the addition or the removal of scores.
     *
     * @param currentStub the current stub
     */
    @Override
    public synchronized void updateScores (SheetStub currentStub)
    {
        if (scores.isEmpty()) {
            // Easy: allocate scores based on all book stubs
            createScores();
        } else {
            try {
                // Determine just the impacted pageRefs
                final SortedSet<PageRef> impactedRefs = new TreeSet<PageRef>();
                final int stubNumber = currentStub.getNumber();

                if (!currentStub.getPageRefs().isEmpty()) {
                    // Look in stub before current stub?
                    final PageRef firstPageRef = currentStub.getFirstPageRef();

                    if (!firstPageRef.isMovementStart()) {
                        final SheetStub prevStub = (stubNumber > 1) ? stubs.get(stubNumber - 2) : null;

                        if (prevStub != null) {
                            final PageRef prevPageRef = prevStub.getLastPageRef();

                            if (prevPageRef != null) {
                                impactedRefs.addAll(getScore(prevPageRef).getPageRefs()); // NPE
                            }
                        }
                    }

                    // Take pages of current stub
                    impactedRefs.addAll(currentStub.getPageRefs());

                    // Look in stub after current stub?
                    final SheetStub nextStub = (stubNumber < stubs.size()) ? stubs.get(stubNumber) : null;

                    if (nextStub != null) {
                        final PageRef nextPageRef = nextStub.getFirstPageRef();

                        if ((nextPageRef != null) && !nextPageRef.isMovementStart()) {
                            impactedRefs.addAll(getScore(nextPageRef).getPageRefs()); // NPE
                        }
                    }
                }

                // Determine and remove the impacted scores
                final List<Score> impactedScores = scoresOf(impactedRefs);
                Integer scoreIndex = null;

                if (!impactedScores.isEmpty()) {
                    scoreIndex = scores.indexOf(impactedScores.get(0));
                } else {
                    for (Score score : scores) {
                        if (score.getFirstPageRef().getSheetNumber() > stubNumber) {
                            scoreIndex = scores.indexOf(score);

                            break;
                        }
                    }
                }

                if (scoreIndex == null) {
                    scoreIndex = scores.size();
                }

                logger.debug("Impacted pages:{} scores:{}", impactedRefs, impactedScores);
                scores.removeAll(impactedScores);

                // Insert new score(s) to replace the impacted one(s)?
                if (!currentStub.isValid()) {
                    impactedRefs.removeAll(currentStub.getPageRefs());
                }

                insertScores(currentStub, impactedRefs, scoreIndex);
            } catch (Exception ex) {
                // This seems to result from inconsistency between scores info and stubs info.
                // Initial cause can be a sheet not marshalled (because of use by another process)
                // followed by a reload of now non-consistent book.xml

                // Workaround: Clear all scores and rebuild them from stubs info
                // (Doing so, we may lose logical-part informations)
                logger.warn("Error updating scores " + ex, ex);
                logger.warn("Rebuilding them from stubs info.");
                scores.clear();
                createScores();
            }
        }
    }

    //------------------//
    // checkRadixChange //
    //------------------//
    /**
     * If the (new) book name does not match current one, update the book radix
     * (and the title of first displayed sheet if any).
     *
     * @param bookPath new book target path
     */
    private void checkRadixChange (Path bookPath)
    {
        // Are we changing the target name WRT the default name?
        final String newRadix = FileUtil.avoidExtensions(
                bookPath.getFileName(),
                OMR.BOOK_EXTENSION).toString();

        if (!newRadix.equals(radix)) {
            // Update book radix
            radix = newRadix;

            // We are really changing the radix, so nullify all other paths
            exportPathSansExt = printPath = scriptPath = null;

            if (OMR.gui != null) {
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
    // createBookFile //
    //----------------//
    /**
     * Create a new book file system dedicated to this book at the location provided
     * by '{@code bookpath}' member.
     * If such file already exists, it is deleted beforehand.
     * <p>
     * When IO operations are finished, the book file must be closed via
     * {@link #closeFileSystem(java.nio.file.FileSystem)}
     *
     * @return the root path of the (zipped) book file system
     */
    private static Path createBookFile (Path bookPath)
            throws IOException
    {
        if (bookPath == null) {
            throw new IllegalStateException("bookPath is null");
        }

        try {
            Files.deleteIfExists(bookPath);
        } catch (IOException ex) {
            logger.warn("Error deleting book: " + bookPath, ex);
        }

        // Make sure the containing folder exists
        Files.createDirectories(bookPath.getParent());

        // Make it a zip file
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(bookPath.toFile()));
        zos.close();

        // Finally open the book file just created
        return Zip.openFileSystem(bookPath);
    }

    //------------//
    // checkScore // Dirty hack, to be removed ASAP
    //------------//
    private void checkScore ()
    {
        for (Score score : scores) {
            PageRef ref = score.getFirstPageRef();
            if (ref == null) {
                logger.warn("Discarding invalid score data.");
                scores.clear();
                break;
            }
        }
    }

    //--------------//
    // createScores //
    //--------------//
    /**
     * Create scores out of all book stubs.
     */
    private void createScores ()
    {
        Score score = null;

        // Group provided sheets pages into scores
        for (SheetStub stub : stubs) {
            // An invalid or not-yet-processed stub triggers a score break
            if (stub.getPageRefs().isEmpty()) {
                score = null;
            } else {
                for (PageRef pageRef : stub.getPageRefs()) {
                    if ((score == null) || pageRef.isMovementStart()) {
                        scores.add(score = new Score());
                        score.setBook(this);
                    }

                    score.addPageRef(stub.getNumber(), pageRef);
                }
            }
        }

        logger.debug("Created scores:{}", scores);
    }

    //-------------------//
    // getConcernedStubs //
    //-------------------//
    private List<SheetStub> getConcernedStubs (Set<Integer> sheetIds)
    {
        List<SheetStub> list = new ArrayList<SheetStub>();

        for (SheetStub stub : getValidStubs()) {
            if ((sheetIds == null) || sheetIds.contains(stub.getNumber())) {
                list.add(stub);
            }
        }

        return list;
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

    //--------------//
    // getLeastStep //
    //--------------//
    /**
     * Report the least advanced step reached among all provided stubs.
     *
     * @return the least step, null if any stub has not reached the first step (LOAD)
     */
    private Step getLeastStep (List<SheetStub> stubs)
    {
        Step least = Step.last();

        for (SheetStub stub : stubs) {
            Step latest = stub.getLatestStep();

            if (latest == null) {
                return null; // This sheet has not been processed at all
            }

            if (latest.compareTo(least) < 0) {
                least = latest;
            }
        }

        return least;
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Report the score if any that contains the provided PageRef.
     *
     * @param pageRef the provided page ref (sheet#, page#)
     * @return the containing score or null if not found
     */
    private Score getScore (PageRef pageRef)
    {
        for (Score score : scores) {
            PageRef ref = score.getPageRef(pageRef.getSheetNumber());

            if ((ref != null) && (ref.getId() == pageRef.getId())) {
                return score;
            }
        }

        return null;
    }

    //----------------//
    // initTransients //
    //----------------//
    private void initTransients (String nameSansExt,
                                 Path bookPath)
    {
        if (version == null) {
            version = ProgramId.PROGRAM_VERSION;
        }

        if (revision == null) {
            revision = ProgramId.PROGRAM_REVISION;
        }

        if (alias == null) {
            alias = checkAlias(getInputPath());
            if (alias != null) {
                nameSansExt = alias;
            }
        }

        if (nameSansExt != null) {
            this.radix = nameSansExt;
        }

        if (bookPath != null) {
            this.bookPath = bookPath;

            if (nameSansExt == null) {
                this.radix = FileUtil.getNameSansExtension(bookPath);
            }
        }
    }

    //--------------//
    // insertScores //
    //--------------//
    /**
     * Insert scores out of provided sequence of PageRef's.
     *
     * @param currentStub stub being processed
     * @param pageRefs    sequence of pageRefs
     * @param insertIndex insertion index in scores list
     */
    private void insertScores (SheetStub currentStub,
                               SortedSet<PageRef> pageRefs,
                               int insertIndex)
    {
        Score score = null;
        Integer stubNumber = null;
        int index = insertIndex;

        for (PageRef ref : pageRefs) {
            if (stubNumber == null) {
                // Very first
                score = null;
            } else if (stubNumber < (ref.getSheetNumber() - 1)) {
                // One or several stubs missing
                score = null;
            }

            if (ref.isMovementStart()) {
                // Movement start
                score = null;
            }

            if (score == null) {
                scores.add(index++, score = new Score());
                score.setBook(this);
            }

            score.addPageRef(ref.getSheetNumber(), ref);
            stubNumber = ref.getSheetNumber();
        }

        logger.debug("Inserted scores:{}", scores.subList(insertIndex, index));
    }

    //--------------------//
    // makeReadyForExport //
    //--------------------//
    private void makeReadyForExport ()
    {
        // Make sure all sheets have been transcribed
        for (SheetStub stub : getValidStubs()) {
            stub.reachStep(Step.PAGE, false);
        }

        // Group book pages into scores, if not already done
        if (scores.isEmpty()) {
            buildScores();
        }
    }

    //----------//
    // scoresOf //
    //----------//
    /**
     * Retrieve the list of scores that embrace the provided sequence of pageRefs.
     *
     * @param refs the provided pageRefs (sorted)
     * @return the impacted scores
     */
    private List<Score> scoresOf (SortedSet<PageRef> refs)
    {
        final List<Score> impacted = new ArrayList<Score>();

        if (!refs.isEmpty()) {
            final int firstNumber = refs.first().getSheetNumber();
            final int lastNumber = refs.last().getSheetNumber();

            for (Score score : scores) {
                if (score.getLastPageRef().getSheetNumber() < firstNumber) {
                    continue;
                }

                if (score.getFirstPageRef().getSheetNumber() > lastNumber) {
                    break;
                }

                List<PageRef> scoreRefs = new ArrayList<PageRef>(score.getPageRefs());
                scoreRefs.retainAll(refs);

                if (!scoreRefs.isEmpty()) {
                    impacted.add(score);
                }
            }
        }

        return impacted;
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

        private final Constant.Boolean processAllStubsInParallel = new Constant.Boolean(
                false,
                "Should we process all stubs of a book in parallel? (beware of many stubs)");
    }
}
