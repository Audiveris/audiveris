//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             B o o k                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.classifier.Annotations;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.image.FilterParam;
import org.audiveris.omr.image.ImageLoading;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.score.OpusExporter;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.PageNumber;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.score.ScoreExporter;
import org.audiveris.omr.score.ScoreReduction;
import org.audiveris.omr.score.ui.BookPdfOutput;
import org.audiveris.omr.sheet.Params.BookParams;
import static org.audiveris.omr.sheet.Sheet.INTERNALS_RADIX;
import org.audiveris.omr.sheet.SheetStub.SheetInput;
import org.audiveris.omr.sheet.Versions.CheckResult;
import org.audiveris.omr.sheet.rhythm.Voices;
import org.audiveris.omr.sheet.ui.BookActions;
import org.audiveris.omr.sheet.ui.BookBrowser;
import org.audiveris.omr.sheet.ui.SheetResultPainter;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.step.ProcessingCancellationException;
import org.audiveris.omr.step.StepPause;
import org.audiveris.omr.step.ui.StepMonitoring;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Jaxb.OmrSchemaOutputResolver;
import org.audiveris.omr.util.Memory;
import org.audiveris.omr.util.NaturalSpec;
import org.audiveris.omr.util.OmrExecutors;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.Version;
import org.audiveris.omr.util.ZipFileSystem;
import org.audiveris.omr.util.param.IntegerParam;
import org.audiveris.omr.util.param.Param;
import org.audiveris.omr.util.param.StringParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>Book</code> is the root class for handling a physical set of image input
 * files, resulting in one or several logical MusicXML scores.
 * <p>
 * A book instance generally corresponds to an input file containing one or several images, each
 * image resulting in a separate {@link Sheet} instance.
 * <p>
 * A sheet generally contains one or several systems.
 * An indented system (sometimes prefixed by part names) usually indicates a new movement called
 * "Score" in MusicXML.
 * Such indented system may appear in the middle of a sheet, thus (logical) score frontiers do
 * not always match (physical) sheet frontiers.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "book")
public class Book
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Book.class);

    /** File name for book internals in book file system: {@value}. */
    public static final String BOOK_INTERNALS = "book.xml";

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** Audiveris version that last operated on this book. */
    @XmlAttribute(name = "software-version")
    private String version;

    /** Audiveris build number that last operated on this book. */
    @XmlAttribute(name = "software-build")
    private String build;

    /** Book alias, if any. */
    @XmlAttribute(name = "alias")
    private String alias;

    /**
     * This is the path to the input image(s) file that led to this book.
     * <p>
     * The path was valid at book creation and recorded in the book .omr project file.
     * It may not be relevant when the same .omr file is used on another machine.
     * NOTA: It is null for a compound book.
     */
    @XmlAttribute(name = "path")
    @XmlJavaTypeAdapter(Jaxb.PathAdapter.class)
    private final Path path;

    /** This boolean indicates if the book score(s) must be updated. */
    @XmlAttribute(name = "dirty")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean dirty = false;

    /**
     * All Book parameters, editable via the BookParameters dialog.
     * This structure replaces the deprecated individual Param instances.
     */
    @XmlElement(name = "parameters")
    private BookParams parameters;

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
     * NOTA: Among this selection, only the <b>valid</b> sheets will be processed.
     * <p>
     * If this specification is null or empty, all (valid) sheets will be processed.
     */
    @XmlElement(name = "sheets-selection")
    private volatile String sheetsSelection;

    /**
     * This is the sequence of all sheets stubs.
     * <p>
     * There is one small stub per image in the input file.
     * <p>
     * Every stub remains in memory until the book is closed.
     * The corresponding sheet can be loaded or swapped on demand.
     */
    @XmlElement(name = "sheet")
    private final List<SheetStub> stubs = new ArrayList<>();

    /**
     * This is the sequence of logical scores detected in this book.
     * <p>
     * A logical score (also known as 'movement') generally begins with an indented system and can
     * span several physical sheets.
     */
    @XmlElement(name = "score")
    private final List<Score> scores = new ArrayList<>();

    // Transient data
    //---------------

    /** Project file lock. */
    private final Lock lock = new ReentrantLock();

    /** The related file radix (file name without extension). */
    private String radix;

    /** File path where the book is kept. */
    private Path bookPath;

    /** File path where the book is printed. */
    private Path printPath;

    /** File path (without extension) where the MusicXML output is stored. */
    private Path exportPathSansExt;

    /** Browser on this book. */
    private BookBrowser bookBrowser;

    /** Flag to signal that book processing should pause. */
    private volatile boolean pauseRequired;

    /** Flag to indicate this book is being closed. */
    private volatile boolean closing;

    /** Set if the book itself has been modified. */
    private volatile boolean modified = false;

    /** Book-level sample repository. */
    private volatile SampleRepository repository;

    /** Has book already been prompted for upgrade?. */
    private boolean promptedForUpgrade = false;

    /** Set of stubs that need to be upgraded. */
    private Set<SheetStub> stubsToUpgrade;

    /** Active parameter dialog, if any. */
    private JDialog parameterDialog;

    /** A trick to keep parameters intact, even when nullified at marshal time. */
    private BookParams parametersMirror;

    /** Has the book itself been upgraded?. */
    private boolean bookUpgraded = false;

    // Deprecated persistent data
    //---------------------------

    /** Deprecated, replaced by book parameters structure. */
    @Deprecated
    @XmlElement(name = "music-font")
    private volatile MusicFamily.MyParam old_musicFamily;

    /** Deprecated, replaced by book parameters structure. */
    @Deprecated
    @XmlElement(name = "text-font")
    private volatile TextFamily.MyParam old_textFamily;

    /** Deprecated, replaced by book parameters structure. */
    @Deprecated
    @XmlElement(name = "input-quality")
    private volatile InputQualityParam old_inputQuality;

    /** Deprecated, replaced by book parameters structure. */
    @XmlElement(name = "binarization")
    @XmlJavaTypeAdapter(FilterParam.JaxbAdapter.class)
    private volatile FilterParam old_binarizationFilter;

    /** Deprecated, replaced by book parameters structure. */
    @Deprecated
    @XmlElement(name = "beam-specification")
    private volatile IntegerParam old_beamSpecification;

    /** Deprecated, replaced by book parameters structure. */
    @Deprecated
    @XmlElement(name = "ocr-languages")
    private volatile StringParam old_ocrLanguages;

    /** Deprecated, replaced by book parameters structure. */
    @Deprecated
    @XmlElement(name = "processing")
    private volatile ProcessingSwitches old_switches;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor needed by JAXB.
     */
    @SuppressWarnings("unused")
    private Book ()
    {
        path = null;
    }

    /**
     * Create a Book with a path to an input images file.
     *
     * @param inputPath the input image path (which may contain several images)
     */
    public Book (Path inputPath)
    {
        Objects.requireNonNull(inputPath, "Trying to create a Book with null path");

        this.path = inputPath;

        initTransients(FileUtil.getNameSansExtension(inputPath).trim(), null);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // addStub //
    //---------//
    public void addStub (SheetStub stub)
    {
        stubs.add(stub);
    }

    //--------------//
    // afterMarshal //
    //--------------//
    @SuppressWarnings("unused")
    private void afterMarshal (Marshaller m)
    {
        parameters = parametersMirror.duplicate();
    }

    //----------//
    // annotate //
    //----------//
    /**
     * Write the book symbol annotations.
     * <p>
     * Generate a whole zip file, in which each valid sheet is represented by a pair
     * composed of sheet image (.png) and sheet annotations (.xml).
     *
     * @param theStubs the stubs to process
     */
    public void annotate (List<SheetStub> theStubs)
    {
        Path root = null;

        try {
            final Path bookFolder = BookManager.getDefaultBookFolder(this);
            final Path annotationsPath = bookFolder.resolve(
                    getRadix() + Annotations.BOOK_ANNOTATIONS_EXTENSION);
            root = ZipFileSystem.create(annotationsPath);

            for (SheetStub stub : theStubs) {
                try {
                    LogUtil.start(stub);

                    final Path sheetFolder = root.resolve(INTERNALS_RADIX + stub.getNumber());
                    final Sheet sheet = stub.getSheet();
                    sheet.annotate(sheetFolder);
                } catch (Exception ex) {
                    logger.warn("Error annotating {} {}", stub, ex.toString(), ex);
                } finally {
                    LogUtil.stopStub();
                }
            }

            logger.info("Book annotated as {}", annotationsPath);
        } catch (IOException ex) {
            logger.warn("Error annotating book {} {}", this, ex.toString(), ex);
        } finally {
            if (root != null) {
                try {
                    root.getFileSystem().close();
                } catch (IOException ignored) {}
            }
        }
    }

    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        if ((parameters != null) && parameters.prune()) {
            parameters = null;
        }
    }

    //------------------//
    // checkRadixChange //
    //------------------//
    /**
     * If the (new) book name does not match the current one, update the book radix
     * (and the title of the first displayed sheet if any).
     *
     * @param bookPath new book target path
     */
    private void checkRadixChange (Path bookPath)
    {
        // Are we changing the target name WRT the default name?
        final String newRadix = FileUtil.avoidExtensions(bookPath.getFileName(), OMR.BOOK_EXTENSION)
                .toString().trim();

        if (!newRadix.equals(radix)) {
            // Update book radix
            radix = newRadix;

            // We are really changing the radix, so nullify all other paths
            exportPathSansExt = printPath = null;

            if (OMR.gui != null) {
                // Update UI first sheet tab
                SwingUtilities.invokeLater(
                        () -> StubsController.getInstance().updateFirstStubTitle(Book.this));
            }
        }
    }

    //-------------//
    // clearScores //
    //-------------//
    /**
     * Reset the book scores.
     */
    public void clearScores ()
    {
        scores.clear();
    }

    //-------//
    // close //
    //-------//
    /**
     * Delete this book instance, as well as its related resources.
     *
     * @param sheetNumber current sheet number in book, if any
     */
    public void close (Integer sheetNumber)
    {
        setClosing(true);

        // Close contained stubs/sheets
        if (OMR.gui != null) {
            SwingUtilities.invokeLater( () -> {
                try {
                    LogUtil.start(Book.this);

                    for (SheetStub stub : new ArrayList<>(stubs)) {
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
            });
        }

        // Close parameter dialog if any
        if (parameterDialog != null) {
            parameterDialog.dispose();
        }

        // Close browser if any
        if (bookBrowser != null) {
            bookBrowser.close();
        }

        // Close score logicalsEditor if any
        for (Score score : scores) {
            score.close();
        }

        // Remove from OMR instances
        OMR.engine.removeBook(this, sheetNumber);

        // Time for some cleanup...
        Memory.gc();

        logger.debug("Book closed.");
    }

    //--------------//
    // createScores //
    //--------------//
    /**
     * Create scores out of (all or selected) valid book stubs.
     *
     * @param validSelectedStubs valid selected stubs, or null
     * @param theScores          (output) the scores to populate
     */
    private void createScores (List<SheetStub> validSelectedStubs,
                               List<Score> theScores)
    {
        if (validSelectedStubs == null) {
            validSelectedStubs = getValidSelectedStubs();
        }

        Score score = null;

        // Group provided sheets pages into scores
        for (SheetStub stub : stubs) {
            // An invalid or not-selected or not-yet-processed stub triggers a score break
            if (!validSelectedStubs.contains(stub) || stub.getPageRefs().isEmpty()) {
                score = null;
            } else {
                for (PageRef pageRef : stub.getPageRefs()) {
                    if ((score == null) || pageRef.isMovementStart()) {
                        theScores.add(score = new Score());
                        score.setBook(this);
                    }

                    score.addPageNumber(stub.getNumber(), pageRef);
                    if (stub.hasSheet()) {
                        final Page page = pageRef.getRealPage();
                        page.setScore(score);
                    }
                }
            }
        }

        logger.info("Created scores: {}", theScores);
    }

    //-------------//
    // createStubs //
    //-------------//
    /**
     * Create as many sheet stubs as there are images in the input image file.
     * A created stub is nearly empty, the related image will have to be loaded later.
     */
    public void createStubs ()
    {
        final ImageLoading.Loader loader = ImageLoading.getLoader(path);

        if (loader != null) {
            final int imageCount = loader.getImageCount();
            loader.dispose();
            logger.info("{} sheet{} in {}", imageCount, ((imageCount > 1) ? "s" : ""), path);

            for (int i = 1; i <= imageCount; i++) {
                stubs.add(new SheetStub(this, i));
            }
        }
    }

    //--------//
    // export //
    //--------//
    /**
     * Export this book scores using MusicXML format.
     * <p>
     * Assuming 'BOOK' is the radix of book name, several outputs can be considered:
     * <ul>
     * <li>If we don't use opus and the book contains a single score, it is exported as "BOOK.ext"
     * where "ext" is either "mxl" or "xml" depending upon whether compression is used.</li>
     * <li>If we don't use opus and the book contains several scores, it is exported as several
     * "BOOK.mvt#.ext" files, where "#" stands for the movement number and "ext" is either "mxl" or
     * "xml" depending upon whether compression is used.</li>
     * <li>If we use opus, everything goes into "BOOK.opus.mxl" as a single container file.</li>
     * </ul>
     *
     * @param theStubs  the valid selected stubs
     * @param theScores (output) the scores to populate
     * @return true if successful
     */
    public boolean export (List<SheetStub> theStubs,
                           List<Score> theScores)
    {
        // Make sure material is ready
        final boolean swap = (OMR.gui == null) || Main.getCli().isSwap() || BookActions
                .swapProcessedSheets();
        final boolean ok = transcribe(theStubs, theScores, swap);

        if (!ok) {
            logger.info("Could not export since transcription did not complete successfully");
            return false;
        }

        // path/to/scores/Book
        final Path bookPathSansExt = BookManager.getActualPath(
                getExportPathSansExt(),
                BookManager.getDefaultExportPathSansExt(this));
        final boolean sig = BookManager.useSignature();

        // Export each movement score
        final String bookName = bookPathSansExt.getFileName().toString();

        if (BookManager.useOpus()) {
            // Export the book as one opus file
            final Path opusPath = getOpusExportPath();

            try {
                new OpusExporter(this).export(opusPath, bookName, sig, theScores);
            } catch (Exception ex) {
                logger.warn("Could not export opus " + opusPath, ex);
            }
        } else {
            // Export the book as one or several movement files
            final Map<Score, Path> scoreMap = getScoreExportPaths(theScores);
            final boolean compressed = BookManager.useCompression();

            for (Entry<Score, Path> entry : scoreMap.entrySet()) {
                final Score score = entry.getKey();
                final Path scorePath = entry.getValue();
                final String scoreName = (!isMultiMovement()) ? bookName
                        : (bookName + OMR.MOVEMENT_EXTENSION + score.getId());

                try {
                    new ScoreExporter(score).export(scorePath, scoreName, sig, compressed);
                } catch (Exception ex) {
                    logger.warn("Could not export score " + scoreName, ex);
                }
            }
        }

        return true;
    }

    //----------//
    // getAlias //
    //----------//
    /**
     * Report the book name alias if any.
     *
     * @return book alias or null
     */
    public String getAlias ()
    {
        return alias;
    }

    //-----------------------//
    // getBarlineHeightParam //
    //-----------------------//
    /**
     * Report the barline height defined at book level.
     *
     * @return the barline height parameter
     */
    public BarlineHeight.MyParam getBarlineHeightParam ()
    {
        return parameters.barlineSpecification;
    }

    //----------------------//
    // getBeamSpecification //
    //----------------------//
    public Integer getBeamSpecification ()
    {
        return getBeamSpecificationParam().getValue();
    }

    //---------------------------//
    // getBeamSpecificationParam //
    //---------------------------//
    public IntegerParam getBeamSpecificationParam ()
    {
        return parameters.beamSpecification;
    }

    //----------------------//
    // getBinarizationParam //
    //----------------------//
    public FilterParam getBinarizationParam ()
    {
        return parameters.binarizationFilter;
    }

    //-------------//
    // getBookPath //
    //-------------//
    /**
     * Report where the book is kept.
     *
     * @return the path to book .omr file
     */
    public Path getBookPath ()
    {
        return bookPath;
    }

    //-----------------//
    // getBrowserFrame //
    //-----------------//
    /**
     * Create a dedicated frame, where book hierarchy can be browsed interactively.
     *
     * @return the created frame
     */
    public JFrame getBrowserFrame ()
    {
        if (bookBrowser == null) {
            // Build the BookBrowser on the score
            bookBrowser = new BookBrowser(this);
        }

        return bookBrowser.getFrame();
    }

    //-------------------//
    // getConcernedStubs //
    //-------------------//
    private List<SheetStub> getConcernedStubs (Set<Integer> sheetIds)
    {
        List<SheetStub> list = new ArrayList<>();

        for (SheetStub stub : getValidSelectedStubs()) {
            if ((sheetIds == null) || sheetIds.contains(stub.getNumber())) {
                list.add(stub);
            }
        }

        return list;
    }

    //----------------------//
    // getExportPathSansExt //
    //----------------------//
    /**
     * Report the path (without extension) where book is to be exported.
     *
     * @return the book export path without extension, or null
     */
    public Path getExportPathSansExt ()
    {
        return exportPathSansExt;
    }

    //-------------------//
    // getFirstValidStub //
    //-------------------//
    /**
     * Report the first non-discarded stub in this book
     *
     * @return the first non-discarded stub, or null
     */
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
    /**
     * Report the path to the book image(s) input.
     *
     * @return the image input path
     */
    public Path getInputPath ()
    {
        return path;
    }

    //----------------------//
    // getInputQualityParam //
    //----------------------//
    /**
     * Report the input quality defined at book level.
     *
     * @return the input quality parameter
     */
    public InputQualityParam getInputQualityParam ()
    {
        return parameters.inputQuality;
    }

    //---------------------------//
    // getInterlineSpecification //
    //---------------------------//
    public Integer getInterlineSpecification ()
    {
        return getInterlineSpecificationParam().getValue();
    }

    //--------------------------------//
    // getInterlineSpecificationParam //
    //--------------------------------//
    public IntegerParam getInterlineSpecificationParam ()
    {
        return parameters.interlineSpecification;
    }

    //--------------//
    // getLeastStep //
    //--------------//
    /**
     * Report the least advanced step reached among all provided stubs.
     *
     * @param theStubs the provided stubs
     * @return the least step, null if any stub has not reached the first step (LOAD)
     */
    private OmrStep getLeastStep (List<SheetStub> theStubs)
    {
        OmrStep least = OmrStep.last();

        for (SheetStub stub : theStubs) {
            OmrStep latest = stub.getLatestStep();

            if (latest == null) {
                return null; // This sheet has not been processed at all
            }

            if (latest.compareTo(least) < 0) {
                least = latest;
            }
        }

        return least;
    }

    //---------//
    // getLock //
    //---------//
    /**
     * Report the lock that protects book project file.
     *
     * @return book project lock
     */
    public Lock getLock ()
    {
        return lock;
    }

    //---------------------//
    // getMusicFamilyParam //
    //---------------------//
    /**
     * Report the music font family defined at book level.
     *
     * @return the music font family parameter
     */
    public MusicFamily.MyParam getMusicFamilyParam ()
    {
        return parameters.musicFamily;
    }

    //----------------------//
    // getOcrLanguagesParam //
    //----------------------//
    /**
     * Report the OCR language(s) specification Param at book level.
     *
     * @return the OCR language(s) param
     */
    public Param<String> getOcrLanguagesParam ()
    {
        return parameters.ocrLanguages;
    }

    //-----------------------//
    // getOldestSheetVersion //
    //-----------------------//
    /**
     * Report the oldest (aka: lowest) version among all (valid) sheet stubs.
     *
     * @return the oldest version found or null if none found
     */
    private Version getOldestSheetVersion ()
    {
        Version oldest = null;

        for (SheetStub stub : stubs) {
            if (stub.isValid()) {
                final Version stubVersion;
                final String stubVersionValue = stub.getVersionValue();

                if (stubVersionValue != null) {
                    stubVersion = new Version(stubVersionValue);
                } else {
                    // Stub without explicit version is assumed to have book version
                    stubVersion = getVersion();
                }

                if ((oldest == null) || oldest.compareWithLabelTo(stubVersion) > 0) {
                    oldest = stubVersion;
                }
            }
        }

        return oldest;
    }

    //-------------------//
    // getOpusExportPath //
    //-------------------//
    /**
     * Report the opus export path.
     * <p>
     * Using opus, everything goes into "BOOK.opus.mxl" as a single container file
     *
     * @return the target opus path
     */
    public Path getOpusExportPath ()
    {
        final Path bookPathSansExt = BookManager.getActualPath(
                getExportPathSansExt(),
                BookManager.getDefaultExportPathSansExt(this));
        final String bookName = bookPathSansExt.getFileName().toString();

        return bookPathSansExt.resolveSibling(bookName + OMR.OPUS_EXTENSION);
    }

    //--------------------//
    // getParameterDialog //
    //--------------------//
    /**
     * Report the active parameter dialog, if any.
     *
     * @return the active parameter dialog, perhaps null
     */
    public JDialog getParameterDialog ()
    {
        return parameterDialog;
    }

    //---------//
    // getPath //
    //---------//
    /**
     * Report the path which best identifies the book.
     *
     * @return the best path or null if none
     */
    public Path getPath ()
    {
        // Book path?
        if (bookPath != null) {
            return bookPath;
        }

        // Input path?
        if (path != null) {
            return path;
        }

        return null;
    }

    //--------------//
    // getPrintPath //
    //--------------//
    /**
     * Report the path, if any, where book is to be printed.
     *
     * @return the print path, or null
     */
    public Path getPrintPath ()
    {
        return printPath;
    }

    //-----------------------//
    // getProcessingSwitches //
    //-----------------------//
    /**
     * Report the processing switches defined at book level, if any.
     *
     * @return the processing switches
     */
    public ProcessingSwitches getProcessingSwitches ()
    {
        return parameters.switches;
    }

    //----------//
    // getRadix //
    //----------//
    /**
     * Report the radix of the file that corresponds to the book.
     * It is based on the simple file name of the book, with no path and no extension.
     *
     * @return the book input file radix
     */
    public String getRadix ()
    {
        return radix;
    }

    //---------------------//
    // getSampleRepository //
    //---------------------//
    /**
     * Report the sample repository (specific or global) to populate for this book
     *
     * @return a specific book repository if possible, otherwise the global one
     */
    public SampleRepository getSampleRepository ()
    {
        SampleRepository repo = getSpecificSampleRepository();

        if (repo != null) {
            return repo;
        }

        // No specific repository is possible, so use global
        return SampleRepository.getGlobalInstance();
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Report the score which contains the provided page.
     *
     * @param page provided page
     * @return containing score (can it be null?)
     */
    public Score getScore (Page page)
    {
        for (Score score : scores) {
            int pageIndex = score.getPageIndex(page);

            if (pageIndex != -1) {
                return score;
            }
        }

        logger.warn("Book.getScore. No score found for {}", page);
        return null;
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
    public Score getScore (PageRef pageRef)
    {
        for (Score score : scores) {
            PageRef ref = score.getPageRef(pageRef.getSheetNumber());

            if ((ref != null) && (ref.getId() == pageRef.getId())) {
                return score;
            }
        }

        return null;
    }

    //---------------------//
    // getScoreExportPaths //
    //---------------------//
    /**
     * Report the export path for each exported score (using no opus).
     * <ul>
     * <li>A <i>single-movement</i> book is exported as one "BOOK.ext" file.
     * <li>A <i>multi-movement</i> book is exported as several "BOOK.mvt#.ext" files,
     * where "#" stands for the movement number.
     * </ul>
     * Extension 'ext' is either "mxl" or "xml" depending upon whether compression is used or not.
     *
     * @param theScores the scores to export
     * @return the populated map of export paths, one per score
     */
    public Map<Score, Path> getScoreExportPaths (List<Score> theScores)
    {
        final Path bookPathSansExt = BookManager.getActualPath(
                getExportPathSansExt(),
                BookManager.getDefaultExportPathSansExt(this));
        final String bookName = bookPathSansExt.getFileName().toString();
        final Map<Score, Path> pathMap = new LinkedHashMap<>();
        final boolean compressed = BookManager.useCompression();
        final String ext = compressed ? OMR.COMPRESSED_SCORE_EXTENSION : OMR.SCORE_EXTENSION;

        for (Score score : theScores) {
            final String scoreName = (!isMultiMovement()) ? bookName
                    : (bookName + OMR.MOVEMENT_EXTENSION + score.getId());
            pathMap.put(score, bookPathSansExt.resolveSibling(scoreName + ext));
        }

        return pathMap;
    }

    //------------------------//
    // getScoreInsertionIndex //
    //------------------------//
    /**
     * Report the index in scores list where the score containing the provided page
     * should be inserted.
     *
     * @param pageRef the provided page
     * @return proper index in scores list
     */
    private int getScoreInsertionIndex (PageRef pageRef)
    {
        for (int i = 0; i < scores.size(); i++) {
            final Score s = scores.get(i);
            final PageRef r = s.getFirstPageRef();

            if ((r != null) && pageRef.compareTo(r) <= 0) {
                return i;
            }
        }

        return scores.size();
    }

    //-----------//
    // getScores //
    //-----------//
    /**
     * Report the scores (movements) detected in this book.
     *
     * @return the live list of scores
     */
    public List<Score> getScores ()
    {
        return scores;
    }

    //------------------//
    // getSelectedStubs //
    //------------------//
    /**
     * Report the selected stubs according to selection specification.
     *
     * @return (copy of) the list of selected stubs (valid or not)
     */
    public List<SheetStub> getSelectedStubs ()
    {
        if (sheetsSelection == null) {
            return new ArrayList<>(stubs);
        }

        return getStubs(NaturalSpec.decode(sheetsSelection, true));
    }

    //--------------------//
    // getSheetsSelection //
    //--------------------//
    /**
     * Report the specification for sheets selection.
     *
     * @return the sheetsSelection string, perhaps null
     */
    public String getSheetsSelection ()
    {
        return sheetsSelection;
    }

    //------------------//
    // getSomeInputPath //
    //------------------//
    /**
     * Report the path to some book image(s) input.
     *
     * @return some input path
     */
    public Path getSomeInputPath ()
    {
        if (path != null) {
            return path;
        }

        if (bookPath != null) {
            return bookPath;
        }

        for (SheetStub stub : stubs) {
            final SheetInput input = stub.getSheetInput();

            if (input != null) {
                return input.path;
            }
        }

        return null;
    }

    //-----------------------------//
    // getSpecificSampleRepository //
    //-----------------------------//
    /**
     * Report (after allocation if needed) the book <b>specific</b> sample repository
     *
     * @return the repository instance with material for this book only, or null
     */
    public SampleRepository getSpecificSampleRepository ()
    {
        if (repository == null) {
            repository = SampleRepository.getInstance(this, true);
        }

        return repository;
    }

    //---------//
    // getStub //
    //---------//
    /**
     * Report the sheet stub with provided id (counted from 1).
     *
     * @param sheetId the desired value for sheet id
     * @return the proper sheet stub, or null if not found
     */
    public SheetStub getStub (int sheetId)
    {
        return stubs.get(sheetId - 1);
    }

    //----------//
    // getStubs //
    //----------//
    /**
     * Report all the sheets stubs contained in this book.
     *
     * @return the immutable list of sheets stubs, list may be empty but is never null
     */
    public List<SheetStub> getStubs ()
    {
        return Collections.unmodifiableList(stubs);
    }

    //----------//
    // getStubs //
    //----------//
    /**
     * Report the sheets stubs corresponding to the provided sheet IDs.
     *
     * @param sheetIds list of IDs of desired stubs, perhaps null
     * @return the immutable list of selected sheets stubs, list may be empty but is never null
     */
    public List<SheetStub> getStubs (Collection<Integer> sheetIds)
    {
        if (sheetIds == null) {
            return getStubs();
        }

        final List<SheetStub> found = new ArrayList<>();

        for (int id : sheetIds) {
            if (id < 1 || id > stubs.size()) {
                logger.warn("No sheet #{} in {}", id, this);
            } else {
                found.add(stubs.get(id - 1));
            }
        }

        return found;
    }

    //-------------------//
    // getStubsToUpgrade //
    //-------------------//
    /**
     * Gather the sheet stubs that still need an upgrade.
     * <p>
     * We use book/sheet version and presence of old table files.
     *
     * @return the set of sheet stubs to upgrade
     */
    public Set<SheetStub> getStubsToUpgrade ()
    {
        if (stubsToUpgrade == null) {
            stubsToUpgrade = new LinkedHashSet<>();
            stubsToUpgrade.addAll(getStubsWithOldVersion());
            stubsToUpgrade.addAll(getStubsWithTableFiles());

            if (!stubsToUpgrade.isEmpty()) {
                logger.info(
                        "{} stub{} to upgrade in {}",
                        stubsToUpgrade.size(),
                        (stubsToUpgrade.size() > 1) ? "s" : "",
                        this);
            }
        }

        return stubsToUpgrade;
    }

    //------------------------//
    // getStubsWithOldVersion //
    //------------------------//
    private Set<SheetStub> getStubsWithOldVersion ()
    {
        final Set<SheetStub> found = new LinkedHashSet<>();
        final Version bookVersion = new Version(getVersionValue());

        for (Version.UpgradeVersion upgradeVersion : Versions.UPGRADE_VERSIONS) {
            if (bookVersion.compareTo(upgradeVersion) < 0) {
                // Check each and every sheet stub, even if invalid
                for (SheetStub stub : stubs) {
                    if (upgradeVersion.upgradeNeeded(stub)) {
                        found.add(stub);
                    }
                }
            }
        }

        return found;
    }

    //------------------------//
    // getStubsWithTableFiles //
    //------------------------//
    /**
     * Report the stubs that still have table files.
     *
     * @return the set of stubs with table files, perhaps empty but not null
     */
    private Set<SheetStub> getStubsWithTableFiles ()
    {
        final Set<SheetStub> found = new LinkedHashSet<>();
        final Lock bookLock = getLock();
        bookLock.lock();

        try {
            final Path theBookPath = BookManager.getDefaultSavePath(this);

            if (!Files.exists(theBookPath)) {
                // No book project file yet
                return found;
            }

            final Path root = ZipFileSystem.open(theBookPath);
            for (SheetStub stub : stubs) {
                final Path sheetFolder = root.resolve(INTERNALS_RADIX + stub.getNumber());

                for (Picture.TableKey key : Picture.TableKey.values()) {
                    final Path tablePath = sheetFolder.resolve(key + ".xml");
                    logger.debug("Checking existence of {}", tablePath);

                    if (Files.exists(tablePath)) {
                        found.add(stub);
                        break;
                    }
                }
            }

            root.getFileSystem().close();
        } catch (Exception ex) {
            logger.warn("Error browsing project file of {} {}", this, ex.toString(), ex);
        } finally {
            bookLock.unlock();
        }

        return found;
    }

    //--------------------//
    // getTextFamilyParam //
    //--------------------//
    /**
     * Report the text font family defined at book level.
     *
     * @return the text font family parameter
     */
    public TextFamily.MyParam getTextFamilyParam ()
    {
        return parameters.textFamily;
    }

    //-----------------------//
    // getValidSelectedStubs //
    //-----------------------//
    /**
     * Report the valid sheets among the sheets selection.
     *
     * @return the valid selected stubs
     */
    public List<SheetStub> getValidSelectedStubs ()
    {
        final List<SheetStub> sel = getSelectedStubs();

        for (Iterator<SheetStub> it = sel.iterator(); it.hasNext();) {
            final SheetStub stub = it.next();

            if (!stub.isValid()) {
                it.remove();
            }
        }

        return sel;
    }

    //---------------//
    // getValidStubs //
    //---------------//
    /**
     * Report the non-discarded sheets stubs in this book.
     *
     * @return the list of valid sheets stubs
     */
    public List<SheetStub> getValidStubs ()
    {
        return getValidStubs(stubs);
    }

    //------------//
    // getVersion //
    //------------//
    public Version getVersion ()
    {
        return new Version(version);
    }

    //-----------------//
    // getVersionValue //
    //-----------------//
    public String getVersionValue ()
    {
        return version;
    }

    //------------------------//
    // hasAllocatedRepository //
    //------------------------//
    /**
     * Tell whether the book has allocated a dedicated sample repository.
     *
     * @return true if allocated
     */
    public boolean hasAllocatedRepository ()
    {
        return repository != null;
    }

    //-----------------------//
    // hasSpecificRepository //
    //-----------------------//
    /**
     * Tell whether the book has an existing specific sample repository.
     *
     * @return true if specific repository exists
     */
    public boolean hasSpecificRepository ()
    {
        if (repository != null) {
            return true;
        }

        // Look for needed files
        return SampleRepository.repositoryExists(this);
    }

    //------------------//
    // hideInvalidStubs //
    //------------------//
    /**
     * Hide stub assemblies of invalid sheets.
     */
    public void hideInvalidStubs ()
    {
        SwingUtilities.invokeLater( () -> {
            final StubsController controller = StubsController.getInstance();

            for (SheetStub stub : stubs) {
                if (!stub.isValid()) {
                    controller.removeAssembly(stub);
                }
            }
        });
    }

    //----------------//
    // initParameters //
    //----------------//
    /**
     * Make sure the parameters are properly allocated, but their parents not yet set.
     */
    public void initParameters ()
    {
        // Migrate old Params, if any
        migrateOldParams();

        // At this point in time, parameters contains only the params with specific value
        if (parameters == null) {
            parameters = new BookParams();
        }

        parameters.completeParams();
        parameters.setScope(this);
    }

    //----------------//
    // initTransients //
    //----------------//
    /**
     * Initialize transient data.
     *
     * @param nameSansExt book name without extension, if any
     * @param bookPath    full path to book .omr file, if any
     * @return true if OK
     */
    private boolean initTransients (String nameSansExt,
                                    Path bookPath)
    {
        initParameters();
        setParamParents();

        for (SheetStub stub : stubs) {
            stub.initTransients(this);
        }

        if (alias == null) {
            final Path inputPath = getInputPath();

            // NOTA: inputPath can be null for a compound .omr file
            if (inputPath != null) {
                alias = checkAlias(inputPath);

                if (alias != null) {
                    nameSansExt = alias;
                }
            }
        }

        if (nameSansExt != null) {
            radix = nameSansExt.trim();
        }

        if (bookPath != null) {
            this.bookPath = bookPath;

            if (nameSansExt == null) {
                radix = FileUtil.getNameSansExtension(bookPath).trim();
            }
        }

        if (build == null) {
            build = WellKnowns.TOOL_BUILD;
        }

        if (version == null) {
            version = WellKnowns.TOOL_REF;
        } else {
            if (constants.checkBookVersion.isSet()) {
                // Check compatibility between book file version and program version
                final CheckResult status = Versions.check(new Version(version));

                switch (status) {
                    case BOOK_TOO_OLD -> {
                        final String msg = bookPath + " version " + version;
                        logger.warn(msg);

                        // Reset book sheets to binary?
                        if (((OMR.gui == null) && constants.resetOldBooks.isSet())
                                || ((OMR.gui != null) && OMR.gui.displayConfirmation(
                                        msg + "\nConfirm reset to binary?",
                                        "Too old book version"))) {
                            resetTo(OmrStep.BINARY);
                            logger.info("Book {} reset to binary.", radix);
                            version = WellKnowns.TOOL_REF;
                            build = WellKnowns.TOOL_BUILD;

                            return true;
                        } else {
                            logger.info("Too old book version, ignored.");
                        }

                        return false;
                    }

                    case PROGRAM_TOO_OLD -> {
                        final String msg = bookPath + " version " + version
                                + "\nPlease use a more recent Audiveris version";
                        logger.warn(msg);

                        if (OMR.gui != null) {
                            OMR.gui.displayWarning(msg, "Too old Audiveris software version");
                        }

                        return false;
                    }

                    case COMPATIBLE -> {
                        return true;
                    }
                }
            }
        }

        stubsToUpgrade = getStubsToUpgrade();

        return true;
    }

    //--------------//
    // insertScores //
    //--------------//
    /**
     * Insert scores out of provided sequence of PageRef's.
     *
     * @param currentStub   stub being processed
     * @param pageRefs      sequence of pageRefs
     * @param insertIndex   insertion index in scores list
     * @param removedScores the scores removed
     */
    private void insertScores (SheetStub currentStub,
                               SortedSet<PageRef> pageRefs,
                               int insertIndex,
                               List<Score> removedScores)
    {
        Score score = null;
        Integer stubNumber = null;
        int index = insertIndex;

        for (PageRef pageRef : pageRefs) {
            if (stubNumber == null) {
                // Very first
                score = null;
            } else if (stubNumber < (pageRef.getSheetNumber() - 1)) {
                // One or several stubs missing
                score = null;
            }

            if (pageRef.isMovementStart()) {
                // Movement start
                score = null;
            }

            if (score == null) {
                scores.add(index++, score = new Score());
                score.setBook(this);
            }

            score.addPageNumber(pageRef.getSheetNumber(), pageRef);
            stubNumber = pageRef.getSheetNumber();
        }

        logger.debug("Inserted scores:{}", scores.subList(insertIndex, index));
    }

    //-----------//
    // isClosing //
    //-----------//
    /**
     * Report whether this book is being closed.
     *
     * @return the closing flag
     */
    public boolean isClosing ()
    {
        return closing;
    }

    //---------//
    // isDirty //
    //---------//
    /**
     * Report whether the book scores need to be reduced.
     *
     * @return true if dirty
     */
    public boolean isDirty ()
    {
        return dirty;
    }

    //---------//
    // isImage //
    //---------//
    /**
     * Report whether this book has just been created on-the-fly to represent an image file.
     *
     * @return true if so
     */
    public boolean isImage ()
    {
        return bookPath == null;
    }

    //------------//
    // isModified //
    //------------//
    /**
     * Report whether the book has been modified with respect to its persisted data.
     *
     * @return true if modified
     */
    public boolean isModified ()
    {
        if (modified) {
            return true; // The book itself is modified
        }

        if ((repository != null) && repository.isModified()) {
            return true; // The book sample repository is modified
        }

        for (SheetStub stub : stubs) {
            if (stub.isModified()) {
                return true; // This sheet is modified
            }
        }

        return false;
    }

    //-----------------//
    // isMultiMovement //
    //-----------------//
    /**
     * Report whether this book contains several movements (scores).
     *
     * @return true if multi scores
     */
    public boolean isMultiMovement ()
    {
        return scores.size() > 1;
    }

    //--------------//
    // isMultiSheet //
    //--------------//
    /**
     * Report whether this book contains several sheets.
     *
     * @return true for several sheets
     */
    public boolean isMultiSheet ()
    {
        return stubs.size() > 1;
    }

    //-----------------//
    // isPauseRequired //
    //-----------------//
    /**
     * Should this book pause ASAP?
     *
     * @return true if so
     */
    public boolean isPauseRequired ()
    {
        return pauseRequired;
    }

    //------------//
    // isUpgraded //
    //------------//
    /**
     * Report whether the book has been upgraded with respect to its persisted data.
     *
     * @return true if upgraded
     */
    public boolean isUpgraded ()
    {
        for (SheetStub stub : stubs) {
            if (stub.isUpgraded()) {
                return true; // This sheet is upgraded
            }
        }

        return bookUpgraded;
    }

    //------------------//
    // migrateOldParams //
    //------------------//
    /**
     * If an old param exists, it is put into the parameters structure.
     */
    private void migrateOldParams ()
    {
        if (old_musicFamily != null) {
            upgradeParameters().musicFamily = old_musicFamily;
            old_musicFamily = null;
        }

        if (old_textFamily != null) {
            upgradeParameters().textFamily = old_textFamily;
            old_textFamily = null;
        }

        if (old_inputQuality != null) {
            upgradeParameters().inputQuality = old_inputQuality;
            old_inputQuality = null;
        }

        if (old_beamSpecification != null) {
            upgradeParameters().beamSpecification = old_beamSpecification;
            old_beamSpecification = null;
        }

        if (old_ocrLanguages != null) {
            upgradeParameters().ocrLanguages = old_ocrLanguages;
            old_ocrLanguages = null;
        }

        if (old_binarizationFilter != null) {
            upgradeParameters().binarizationFilter = old_binarizationFilter;
            old_binarizationFilter = null;
        }

        if (old_switches != null) {
            upgradeParameters().switches = old_switches;
            old_switches = null;
        }
    }

    //--------------//
    // openBookFile //
    //--------------//
    /**
     * Open the book file (supposed to already exist at location provided by
     * '<code>bookPath</code>' member) for reading or writing.
     * <p>
     * When IO operations are finished, the book file must be closed via
     * {@link #closeFileSystem(java.nio.file.FileSystem)}
     *
     * @return the root path of the (zipped) book file system
     * @throws java.io.IOException if anything goes wrong
     */
    public Path openBookFile ()
        throws IOException
    {
        return ZipFileSystem.open(bookPath);
    }

    //-----------------//
    // openSheetFolder //
    //-----------------//
    /**
     * Open (in the book zipped file) the folder for provided sheet number
     *
     * @param number sheet number (1-based) within the book
     * @return the path to sheet folder
     * @throws IOException if anything goes wrong
     */
    public Path openSheetFolder (int number)
        throws IOException
    {
        Path root = openBookFile();

        return root.resolve(INTERNALS_RADIX + number);
    }

    //-------//
    // print //
    //-------//
    /**
     * Print this book in PDF format.
     *
     * @param theStubs the valid selected stubs
     */
    public void print (List<SheetStub> theStubs)
    {
        // Path to print file
        final Path pdfPath = BookManager.getActualPath(
                getPrintPath(),
                BookManager.getDefaultPrintPath(this));

        try {
            new BookPdfOutput(this, pdfPath.toFile()).write(
                    theStubs,
                    new SheetResultPainter.PdfResultPainter());
            setPrintPath(pdfPath);
            logger.info("Book printed to {}", pdfPath);
        } catch (Exception ex) {
            logger.warn("Cannot print to {} {}", pdfPath, ex.toString(), ex);
        }
    }

    //--------------------//
    // promptedForUpgrade //
    //--------------------//
    /**
     * @return true if already prompted For Upgrade
     */
    public boolean promptedForUpgrade ()
    {
        return promptedForUpgrade;
    }

    //---------------//
    // reachBookStep //
    //---------------//
    /**
     * Reach a specific step (and all needed intermediate steps) on valid selected
     * sheets of this book.
     *
     * @param target   the targeted step
     * @param force    if true and step already reached, sheet is reset and processed until step
     * @param theStubs the valid selected stubs
     * @param swap     if true, swap out processed sheets
     * @return true if OK on all sheet actions
     */
    public boolean reachBookStep (final OmrStep target,
                                  final boolean force,
                                  final List<SheetStub> theStubs,
                                  final boolean swap)
    {
        try {
            logger.debug("reachStep {} force:{} stubs:{} swap:{}", target, force, theStubs, swap);

            if (!force) {
                // Check against the least advanced step performed across all sheets concerned
                OmrStep least = getLeastStep(theStubs);

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
                    ids(theStubs));

            try {
                boolean someFailure = false;
                StepMonitoring.notifyStart();

                if (isMultiSheet() && constants.processAllStubsInParallel.isSet()
                        && (OmrExecutors.defaultParallelism.getValue() == true)) {
                    // Process all stubs in parallel
                    List<Callable<Boolean>> tasks = new ArrayList<>();

                    for (final SheetStub stub : theStubs) {
                        tasks.add( () -> {
                            LogUtil.start(stub);

                            try {
                                boolean ok = stub.reachStep(target, force);

                                if (ok && (OMR.gui == null)) {
                                    stub.swapSheet(); // Save sheet & global book info to disk
                                }

                                return ok;
                            } finally {
                                LogUtil.stopStub();
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
                            } catch (InterruptedException | ExecutionException ex) {
                                if (ex.getCause() instanceof StepPause) {
                                    logger.debug("StepPause in Future {}", future);
                                } else {
                                    logger.warn("Future exception", ex);
                                }
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
                    for (SheetStub stub : theStubs) {
                        LogUtil.start(stub);

                        try {
                            if (stub.reachStep(target, force)) {} else {
                                someFailure = true;
                            }
                        } catch (StepPause ex) {
                            // Book pause required
                            // Stop processing for the other stubs
                            logger.info("Book processing stopped by user.");
                            someFailure = true;
                            break;
                        } catch (ProcessingCancellationException ex) {
                            // Exception (such as timeout) raised on stub
                            // Let processing continue for the other stubs
                            logger.warn("Error processing stub");
                            someFailure = true;
                        } catch (Exception ex) {
                            // Exception raised on stub
                            // Let processing continue for the other stubs
                            logger.warn("Error processing stub {}", ex);
                            someFailure = true;
                        } finally {
                            if (swap) {
                                swapAllSheets(); // Save sheet(s) & global book info to disk
                                logger.info("End of {} memory: {}", stub, Memory.getValue());
                            }

                            LogUtil.stopStub();
                        }
                    }

                    logger.info("Book processed.");
                }

                return !someFailure;
            } finally {
                LogUtil.stopStub();
                StepMonitoring.notifyStop();

                long stopTime = System.currentTimeMillis();
                logger.debug("End of step set in {} ms.", (stopTime - startTime));
            }
        } catch (ProcessingCancellationException pce) {
            throw pce;
        } catch (Exception ex) {
            logger.warn("Error in performing " + target, ex);
        }

        return false;
    }

    //--------------//
    // reduceScores //
    //--------------//
    /**
     * Determine the logical parts of each score.
     *
     * @param theStubs  the valid selected stubs
     * @param theScores the scores to populate
     * @return the count of modifications done
     */
    public int reduceScores (List<SheetStub> theStubs,
                             List<Score> theScores)
    {
        int modifs = 0;

        for (Score score : theScores) {
            // (re) build the score logical parts?
            if (score.needsPartCollation()) {
                modifs += new ScoreReduction(score).reduce(theStubs);
            }

            // Voices connection across pages in score
            modifs += Voices.refineScore(score, theStubs);
        }

        if (modifs > 0) {
            if (theScores == this.scores) {
                setModified(true);
            }

            logger.info("Scores built: {}", theScores.size());
        }

        if (theScores == this.scores) {
            setDirty(false);
        }

        return modifs;
    }

    //------------//
    // removeStub //
    //------------//
    /**
     * Remove the specified sheet stub from the containing book.
     * <p>
     * Typically, when the sheet carries no music information, it can be removed from the book
     * (without changing the IDs of the sibling sheets in the book)
     *
     * @param stub the sheet stub to remove
     * @return true if actually removed
     */
    public boolean removeStub (SheetStub stub)
    {
        return stubs.remove(stub);
    }

    //---------//
    // resetTo //
    //---------//
    /**
     * Reset all valid selected sheets of this book to their gray or binary images.
     *
     * @param step either LOAD or BINARY step only
     */
    public void resetTo (OmrStep step)
    {
        if (step != OmrStep.LOAD && step != OmrStep.BINARY) {
            logger.error("Method resetTo is reserved to LOAD and BINARY steps");
            return;
        }

        final StubsController ctrl = (OMR.gui != null) ? StubsController.getInstance() : null;

        for (SheetStub stub : getValidSelectedStubs()) {
            if (stub.isDone(step)) {
                if (step == OmrStep.LOAD) {
                    stub.resetToGray();
                } else {
                    stub.resetToBinary();
                }

                if (ctrl != null) {
                    ctrl.markTab(stub, Colors.SHEET_OK);
                }
            }
        }

        clearScores();
    }

    //--------//
    // sample //
    //--------//
    /**
     * Write the book symbol samples into its sample repository.
     *
     * @param theStubs the selected valid stubs
     */
    public void sample (List<SheetStub> theStubs)
    {
        for (SheetStub stub : theStubs) {
            Sheet sheet = stub.getSheet();
            sheet.sample();
        }
    }

    //---------//
    // scoreOf //
    //---------//
    /**
     * Retrieve the score, if any, that embraces the provided PageRef.
     *
     * @param ref the provided PageRef
     * @return the containing score if any
     */
    private Score scoreOf (PageRef ref)
    {
        final PageNumber pageNumber = ref.getPageNumber();

        for (Score score : scores) {
            if (score.contains(pageNumber))
                return score;
        }

        return null;
    }

    //----------//
    // setAlias //
    //----------//
    /**
     * Set the book alias
     *
     * @param alias the book alias
     */
    public void setAlias (String alias)
    {
        this.alias = alias;
        radix = alias;
    }

    //-------------//
    // setBookPath //
    //-------------//
    /**
     * Assign bookPath to the book.
     *
     * @param bookPath the path to book file on disk
     */
    public void setBookPath (Path bookPath)
    {
        this.bookPath = bookPath;
    }

    //------------//
    // setClosing //
    //------------//
    /**
     * Flag this book as closing.
     *
     * @param closing the closing to set
     */
    public void setClosing (boolean closing)
    {
        this.closing = closing;
    }

    //----------//
    // setDirty //
    //----------//
    /**
     * Set the dirty flag.
     *
     * @param dirty the new flag value
     */
    public void setDirty (boolean dirty)
    {
        this.dirty = dirty;
    }

    //----------------------//
    // setExportPathSansExt //
    //----------------------//
    /**
     * Remember the path (without extension) where the book is to be exported.
     *
     * @param exportPathSansExt the book export path (without extension)
     */
    public void setExportPathSansExt (Path exportPathSansExt)
    {
        this.exportPathSansExt = exportPathSansExt;
    }

    //-------------//
    // setModified //
    //-------------//
    /**
     * Set the modified flag.
     *
     * @param modified the new flag value
     */
    public void setModified (boolean modified)
    {
        this.modified = modified;

        if (OMR.gui != null) {
            SwingUtilities.invokeLater( () -> {
                final StubsController controller = StubsController.getInstance();
                final SheetStub stub = controller.getSelectedStub();

                if ((stub != null) && (stub.getBook() == Book.this) && stub.hasSheet()) {
                    controller.refresh();
                }
            });
        }
    }

    //--------------------//
    // setParameterDialog //
    //--------------------//
    /**
     * Register the provided dialog as the active parameter dialog.
     *
     * @param dialog new parameter dialog, perhaps null
     */
    public void setParameterDialog (JDialog dialog)
    {
        parameterDialog = dialog;
    }

    //-----------------//
    // setParamParents //
    //-----------------//
    /**
     * Connect every book parameter to proper global parameter.
     */
    public void setParamParents ()
    {
        // 1/ Make sure parameters are available
        initParameters();

        // 2/ set parents
        parameters.setParents(null);

        // 3/ set parametersMirror
        parametersMirror = parameters.duplicate();
    }

    //------------------//
    // setPauseRequired //
    //------------------//
    /**
     * Setter for pauseRequired.
     *
     * @param bool new value
     */
    public void setPauseRequired (boolean bool)
    {
        pauseRequired = bool;
    }

    //--------------//
    // setPrintPath //
    //--------------//
    /**
     * Remember to which path book print data is to be written.
     *
     * @param printPath the print path
     */
    public void setPrintPath (Path printPath)
    {
        this.printPath = printPath;
    }

    //-----------------------//
    // setPromptedForUpgrade //
    //-----------------------//
    /**
     * Set promptedForUpgrade to true.
     */
    public void setPromptedForUpgrade ()
    {
        promptedForUpgrade = true;
    }

    //--------------------//
    // setSheetsSelection //
    //--------------------//
    /**
     * Remember a new specification for sheets selection.
     *
     * @param sheetsSelection the sheetsSelection to set, perhaps null
     * @return true if the spec was actually modified
     */
    public boolean setSheetsSelection (String sheetsSelection)
    {
        boolean modif = false;

        if (sheetsSelection == null) {
            if (this.sheetsSelection != null) {
                modif = true;
            }
        } else {
            if (!sheetsSelection.equals(this.sheetsSelection)) {
                modif = true;
            }
        }

        this.sheetsSelection = sheetsSelection;

        if (modif) {
            setModified(true); // Book has been modified
        }

        return modif;
    }

    //-----------------//
    // setVersionValue //
    //-----------------//
    public void setVersionValue (String version)
    {
        this.version = version;
    }

    //------//
    // size //
    //------//
    /**
     * Report the total number of sheet stubs in this book, regardless of their
     * selection or validity.
     *
     * @return the stubs count
     */
    public int size ()
    {
        return stubs.size();
    }

    //-------//
    // store //
    //-------//
    /**
     * Store book to disk, using its current book path.
     */
    public void store ()
    {
        if (bookPath == null) {
            logger.warn("Bookpath not defined");
        } else {
            store(bookPath, false);
        }
    }

    //-------//
    // store //
    //-------//
    /**
     * Store book to disk.
     *
     * @param bookPath   target path for storing the book
     * @param withBackup if true, rename beforehand any existing target as a backup
     */
    public void store (Path bookPath,
                       boolean withBackup)
    {
        Memory.gc(); // Launch garbage collection, to save on weak glyph references ...

        boolean diskWritten = false; // Has disk actually been written?

        // Backup existing book file?
        if (withBackup && Files.exists(bookPath)) {
            Path backup = FileUtil.backup(bookPath);

            if (backup != null) {
                logger.info("Previous book file renamed as {}", backup);
            }
        }

        Path root = null; // Root of the zip file system

        try {
            getLock().lock();
            checkRadixChange(bookPath);
            logger.debug("Storing book...");

            if ((this.bookPath == null) || this.bookPath.toAbsolutePath().equals(
                    bookPath.toAbsolutePath())) {
                if (this.bookPath == null) {
                    root = ZipFileSystem.create(bookPath);
                    diskWritten = true;
                } else {
                    root = ZipFileSystem.open(bookPath);
                }

                if (isModified() || isUpgraded()) {
                    storeBookInfo(root); // Book info (book.xml)
                    diskWritten = true;
                }

                // Contained sheets
                for (SheetStub stub : stubs) {
                    if (stub.isModified() || stub.isUpgraded()) {
                        final Path sheetFolder = root.resolve(INTERNALS_RADIX + stub.getNumber());
                        stub.getSheet().store(sheetFolder, null);
                        diskWritten = true;
                    }
                }

                // Separate repository
                if ((repository != null) && repository.isModified()) {
                    repository.storeRepository();
                }
            } else {
                // Switch from old to new book file
                root = ZipFileSystem.create(bookPath);

                diskWritten = true;

                storeBookInfo(root); // Book info (book.xml)

                // Contained sheets
                final Path oldRoot = openBookFile(this.bookPath);

                for (SheetStub stub : stubs) {
                    final Path oldSheetFolder = oldRoot.resolve(INTERNALS_RADIX + stub.getNumber());
                    final Path sheetFolder = root.resolve(INTERNALS_RADIX + stub.getNumber());

                    // By default, copy existing sheet files
                    if (Files.exists(oldSheetFolder)) {
                        FileUtil.copyTree(oldSheetFolder, sheetFolder);
                    }

                    // Update modified sheet files
                    if (stub.isModified() || stub.isUpgraded()) {
                        stub.getSheet().store(sheetFolder, oldSheetFolder);
                    }
                }

                oldRoot.getFileSystem().close(); // Close old book file
            }

            this.bookPath = bookPath;

            if (diskWritten) {
                logger.info("Book stored as {}", bookPath);
            }
        } catch (Exception ex) {
            logger.warn("Error storing " + this + " to " + bookPath + " ex:" + ex, ex);
        } finally {
            if (root != null) {
                try {
                    root.getFileSystem().close();
                } catch (IOException ignored) {}
            }

            getLock().unlock();
        }
    }

    //---------------//
    // storeBookInfo //
    //---------------//
    /**
     * Store the book information (global info + stub steps) into book file system.
     *
     * @param root root path of book file system
     * @throws Exception if anything goes wrong
     */
    public void storeBookInfo (Path root)
        throws Exception
    {
        // Book version should always be the oldest (i.e. lowest) of all sheets versions
        Version oldest = getOldestSheetVersion();

        if (oldest != null) {
            setVersionValue(oldest.value);
        }

        Path bookInternals = root.resolve(BOOK_INTERNALS);
        Files.deleteIfExists(bookInternals);
        Jaxb.marshal(this, bookInternals, getJaxbContext());

        setModified(false);
        bookUpgraded = false;
        logger.info("Stored {}", bookInternals);
    }

    //------------//
    // storeSheet //
    //------------//
    public void storeSheet ()
    {
    }

    //---------------//
    // swapAllSheets //
    //---------------//
    /**
     * Swap out all sheets, except the current one if any.
     */
    public void swapAllSheets ()
    {
        if (isModified() || isUpgraded()) {
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
        return new StringBuilder("Book{").append(radix).append("}").toString();
    }

    //------------//
    // transcribe //
    //------------//
    /**
     * Convenient method to perform all needed transcription steps on selected valid sheets
     * of this book and building the book score(s).
     *
     * @param theStubs  the valid selected stubs
     * @param theScores (output) the collection of scores to populate
     * @param swap      if true, processed sheets are swapped out
     * @return true if OK
     */
    public boolean transcribe (List<SheetStub> theStubs,
                               List<Score> theScores,
                               boolean swap)
    {
        boolean ok = reachBookStep(OmrStep.last(), false, theStubs, swap);

        if (ok) {
            if (theScores.isEmpty()) {
                createScores(theStubs, theScores);
            }

            reduceScores(theStubs, theScores);
        }

        return ok;
    }

    //--------------//
    // updateScores //
    //--------------//
    /**
     * Update the gathering of pages into scores, according to the provided sheet stub.
     *
     * @param currentStub the stub being processed
     */
    public synchronized void updateScores (SheetStub currentStub)
    {
        if (scores.isEmpty()) {
            // Easy: allocate scores based on all relevant book stubs
            createScores(null, scores);
            return;
        }

        final int stubNumber = currentStub.getNumber();

        // 1- Purge scores from any page references to current stub
        for (Score score : scores) {
            score.removeSheetPageNumbers(stubNumber);
        }

        // 2- Insert stub PageNumber's in proper scores at proper index
        final List<PageRef> pageRefs = currentStub.getPageRefs();

        Score score = null;

        for (int i = 0; i < pageRefs.size(); i++) {
            final PageRef pageRef = pageRefs.get(i);

            if (!pageRef.isMovementStart()) {
                // Extend score if possible
                if (i == 0) {
                    // Check with last page of previous sheet stub if any
                    if (stubNumber > 1) {
                        final SheetStub stubAbove = getStub(stubNumber - 1);

                        if (getValidSelectedStubs().contains(stubAbove)) {
                            final PageRef refAbove = stubAbove.getLastPageRef();
                            if (refAbove != null) {
                                score = scoreOf(refAbove);
                            }
                        }
                    }

                    if (score == null) {
                        score = new Score();
                        score.setBook(this);
                        scores.add(getScoreInsertionIndex(pageRef), score);
                    }
                }

                score.insertPage(pageRef);
            } else {
                // Movement start => different score
                score = new Score();
                score.setBook(this);
                score.insertPage(pageRef);
                scores.add(getScoreInsertionIndex(pageRef), score);
            }
        }

        // 3- Merge with following score if applicable
        if ((score != null) && stubNumber < stubs.size()) {
            final SheetStub stubBelow = getStub(stubNumber + 1);

            if (getValidSelectedStubs().contains(stubBelow)) {
                final PageRef refBelow = stubBelow.getFirstPageRef();

                if ((refBelow != null) && !refBelow.isMovementStart()) {
                    final Score nextScore = scoreOf(refBelow);
                    if (score != nextScore) {
                        score.mergeWith(nextScore);
                    }
                }
            }
        }

        // 4- Purge empty scores if any
        for (Iterator<Score> it = scores.iterator(); it.hasNext();) {
            if (it.next().getPageCount() == 0) {
                it.remove();
            }
        }
    }

    //-------------------//
    // upgradeParameters //
    //-------------------//
    /**
     * Get/create the parameters structure for upgrading.
     *
     * @return the existing or created structure
     */
    private BookParams upgradeParameters ()
    {
        bookUpgraded = true;

        if (parameters == null) {
            parameters = new BookParams();
        }

        return parameters;
    }

    //--------------//
    // upgradeStubs //
    //--------------//
    /**
     * Upgrade the book sheets.
     * <p>
     * Among book stubs:
     * <ul>
     * <li>Some may have already been loaded, and thus perhaps upgraded but not necessarily stored.
     * <li>Some may not have been loaded yet, so we need to check/load/swap them.
     * </ul>
     */
    public void upgradeStubs ()
    {
        final StopWatch watch = new StopWatch("upgradeStubs for " + this);
        try {
            final List<SheetStub> upgraded = new ArrayList<>();

            // Current GUI stub, if any
            SheetStub currentStub = (OMR.gui != null) ? StubsController.getCurrentStub() : null;

            for (SheetStub stub : stubsToUpgrade) {
                logger.debug("check " + stub);
                watch.start("check " + stub);
                stub.getSheet(); // Load sheet if needed, this performs the upgrade w/in sheet
                upgraded.add(stub);

                // Store (or swap=store+dispose). This also cleans table files
                if (stub == currentStub) {
                    stub.storeSheet();
                } else {
                    stub.swapSheet();
                }
            }

            if (!upgraded.isEmpty()) {
                logger.info("Upgraded book {}", bookPath);
                stubsToUpgrade.removeAll(upgraded);
            }

            if (OMR.gui != null) {
                BookActions.getInstance().setBookModifiedOrUpgraded(isModified() || isUpgraded());
            }
        } catch (Exception ex) {
            logger.warn("Error upgrading stubs", ex);
        }

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------------//
    // batchUpgradeBooks //
    //-------------------//
    /**
     * In batch, should we automatically upgrade book sheets?
     *
     * @return true if so
     */
    public static boolean batchUpgradeBooks ()
    {
        return constants.batchUpgradeBooks.isSet();
    }

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
        } catch (IOException ex) {
            logger.warn("Could not close book file system " + ex, ex);
        }
    }

    //------------//
    // createBook //
    //------------//
    /**
     * Creates a book, to be stored at bookPath.
     *
     * @param bookPath the target path for the book
     * @return the created book
     */
    public static Book createBook (Path bookPath)
    {
        final Book book = new Book();
        book.setBookPath(bookPath);

        book.initTransients(null, null);

        return book;
    }

    //----------------//
    // generateSchema //
    //----------------//
    /**
     * Generate the XSD schema definition file rooted at Book class.
     *
     * @param outputFileName full schema file name
     */
    public static void generateSchema (String outputFileName)
    {
        try {
            SchemaOutputResolver sor = new OmrSchemaOutputResolver(outputFileName);
            getJaxbContext().generateSchema(sor);
        } catch (Exception ex) {
            logger.warn("Error generating schema for Book {}", ex.toString(), ex);
        }
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    public static JAXBContext getJaxbContext ()
        throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(Book.class);
        }

        return jaxbContext;
    }

    //---------------//
    // getValidStubs //
    //---------------//
    /**
     * Report the valid stubs among the provided stubs.
     *
     * @param theStubs the provided stubs
     * @return the list of valid sheets stubs
     */
    public static List<SheetStub> getValidStubs (List<SheetStub> theStubs)
    {
        List<SheetStub> valids = new ArrayList<>();

        for (SheetStub stub : theStubs) {
            if (stub.isValid()) {
                valids.add(stub);
            }
        }

        return valids;
    }

    //-----//
    // ids //
    //-----//
    /**
     * Build a string with just the IDs of the stub collection.
     *
     * @param theStubs the collection of stub instances
     * @return the string built
     */
    public static String ids (List<SheetStub> theStubs)
    {
        if (theStubs == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (SheetStub entity : theStubs) {
            sb.append("#").append(entity.getNumber());
        }

        sb.append("]");

        return sb.toString();
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
        final StopWatch watch = new StopWatch("loadBook " + bookPath);
        Book book = null;

        try {
            if (!Files.exists(bookPath)) {
                logger.warn("The file {} does not exist", bookPath);
                return null;
            } else {
                logger.info("Loading book {}", bookPath);
            }

            watch.start("book");

            // Open book file
            Path rootPath = ZipFileSystem.open(bookPath);

            // Load book internals (just the stubs) out of book.xml
            Path internalsPath = rootPath.resolve(BOOK_INTERNALS);

            try (InputStream is = Files.newInputStream(internalsPath, StandardOpenOption.READ)) {
                JAXBContext ctx = getJaxbContext();
                Unmarshaller um = ctx.createUnmarshaller();
                book = (Book) um.unmarshal(is);
                LogUtil.start(book);
                book.getLock().lock();
                rootPath.getFileSystem().close();

                boolean ok = book.initTransients(null, bookPath);

                if (!ok) {
                    logger.info("Discarded {}", bookPath);

                    return null;
                }

                // Book successfully loaded (but sheets may need upgrade later).
                return book;
            }
        } catch (IOException | JAXBException ex) {
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

    //--------------//
    // openBookFile //
    //--------------//
    /**
     * Open the book file (supposed to already exist at location provided by
     * '<code>bookPath</code>' parameter) for reading or writing.
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

            FileSystem fileSystem = FileSystems.newFileSystem(bookPath, (ClassLoader) null);

            return fileSystem.getPath(fileSystem.getSeparator());
        } catch (FileNotFoundException ex) {
            logger.warn("File not found: " + bookPath, ex);
        } catch (IOException ex) {
            logger.warn("Error reading book:" + bookPath, ex);
        }

        return null;
    }

    //~ Inner classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Boolean processAllStubsInParallel = new Constant.Boolean(
                false,
                "Should we process all stubs of a book in parallel? (beware of many stubs)");

        private final Constant.Boolean checkBookVersion = new Constant.Boolean(
                true,
                "Should we check version of loaded book files?");

        private final Constant.Boolean resetOldBooks = new Constant.Boolean(
                false,
                "In batch, should we reset to binary the too old book files?");

        private final Constant.Boolean batchUpgradeBooks = new Constant.Boolean(
                false,
                "In batch, should we automatically upgrade all book sheets?");
    }
}
