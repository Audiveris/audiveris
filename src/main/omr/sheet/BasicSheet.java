//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B a s i c S h e e t                                      //
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

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.BasicNest;
import omr.glyph.GlyphNest;
import omr.glyph.SymbolsModel;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.SymbolsController;
import omr.glyph.ui.SymbolsEditor;

import omr.image.FilterDescriptor;
import omr.image.ImageFormatException;

import omr.lag.LagManager;
import omr.lag.Lags;

import omr.math.Population;

import omr.score.Score;
import omr.score.ScoreExporter;
import omr.score.entity.Page;
import omr.score.entity.SystemNode;

import omr.script.ExportTask;

import omr.selection.LocationEvent;
import omr.selection.PixelLevelEvent;
import omr.selection.SelectionService;

import static omr.sheet.BookManager.COMPRESSED_SCORE_EXTENSION;
import static omr.sheet.BookManager.MOVEMENT_EXTENSION;
import static omr.sheet.BookManager.SCORE_EXTENSION;
import static omr.sheet.BookManager.SHEET_PREFIX;

import omr.sheet.stem.StemScale;
import omr.sheet.ui.BinarizationBoard;
import omr.sheet.ui.PictureView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.SheetAssembly;
import omr.sheet.ui.SheetTab;
import omr.sheet.ui.SheetsController;

import omr.sig.InterManager;

import omr.step.ProcessingCancellationException;
import omr.step.Step;
import omr.step.StepException;
import omr.step.ui.StepMonitoring;

import omr.ui.BoardsPane;
import omr.ui.ErrorsEditor;
import omr.ui.util.ItemRenderer;

import omr.util.LiveParam;
import omr.util.Navigable;
import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import omr.score.ui.BookPdfOutput;
import omr.script.PrintTask;
import omr.util.FileUtil;

/**
 * Class {@code BasicSheet} is a basic implementation of {@link Sheet} interface.
 *
 * @author Hervé Bitteur
 */
public class BasicSheet
        implements Sheet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Sheet.class);

    /** Events that can be published on sheet location service. */
    private static final Class<?>[] allowedEvents = new Class<?>[]{
        LocationEvent.class, PixelLevelEvent.class
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Containing book. */
    @Navigable(false)
    private final Book book;

    /** Index of sheet, counted from 1, in the image file. */
    private final int index;

    /** Sheet ID. */
    private final String id;

    /** Corresponding page(s). A single sheet may relate to several pages. */
    private final List<Page> pages = new ArrayList<Page>();

    /** The recording of key processing data. */
    private final SheetBench bench;

    /** Dictionary of sheet lags. */
    private final LagManager lagManager;

    /** All steps already done on this sheet. */
    private final EnumSet<Step> doneSteps = EnumSet.noneOf(Step.class);

    /** Systems management. */
    private final SystemManager systemManager;

    /** Staves. */
    private final StaffManager staffManager;

    /** Inter manager for all systems in this sheet. */
    private final InterManager interManager;

    /** Param for pixel filter. */
    private final LiveParam<FilterDescriptor> filterContext;

    /** Param for text language. */
    private final LiveParam<String> textContext;

    //-- UI ----------------------------------------------------------------------------------------
    //
    /** Selections for this sheet. (SheetLocation, PixelLevel) */
    private final SelectionService locationService;

    /** Registered item renderers, if any. */
    private final Set<ItemRenderer> itemRenderers;

    /** Related assembly instance, if any. */
    private final SheetAssembly assembly;

    /** Related errors editor, if any. */
    private final ErrorsEditor errorsEditor;

    /** Specific builder dealing with glyphs. */
    private volatile SymbolsController symbolsController;

    /** Related symbols editor, if any. */
    private SymbolsEditor symbolsEditor;

    //-- resettable members ------------------------------------------------------------------------
    //
    /** The related picture. */
    private Picture picture;

    /** The step being performed on this sheet. */
    private Step currentStep;

    /** Global scale for this sheet. */
    private Scale scale;

    /** Global stem scale for this sheet. */
    private StemScale stemScale;

    /** Initial skew value. */
    private Skew skew;

    /** Global glyph nest. */
    private GlyphNest nest;

    /** Global measure of beam gaps within groups. */
    private Population beamGaps;

    /** Delta measurements. */
    private SheetDiff sheetDelta;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new {@code Sheet} instance within a book.
     *
     * @param book  the containing book instance
     * @param index index (counted from 1) of sheet in book
     * @param image the already loaded image, if any
     * @throws omr.step.StepException
     */
    public BasicSheet (Book book,
                       int index,
                       BufferedImage image)
            throws StepException
    {
        this.book = book;
        this.index = index;

        if (book.isMultiSheet()) {
            id = book.getRadix() + "#" + index;
        } else {
            id = book.getRadix();
        }

        staffManager = new StaffManager(this);
        systemManager = new SystemManager(this);
        lagManager = new LagManager(this);
        bench = new SheetBench(this);

        if (image != null) {
            setImage(image);
        }

        filterContext = new LiveParam<FilterDescriptor>(book.getFilterParam());
        textContext = new LiveParam<String>(book.getLanguageParam());

        // Update UI information if so needed
        if (Main.getGui() != null) {
            locationService = new SelectionService("sheet", allowedEvents);
            errorsEditor = new ErrorsEditor(this);
            itemRenderers = new HashSet<ItemRenderer>();
            Main.getGui().sheetsController.addAssembly(assembly = new SheetAssembly(this));
            addItemRenderer(staffManager);
        } else {
            locationService = null;
            errorsEditor = null;
            itemRenderers = null;
            assembly = null;
        }

        createNest();
        interManager = new InterManager(this);
        logger.debug("Created {}", this);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addError //
    //----------//
    /**
     * Register an error in the sheet ErrorsWindow
     *
     * @param container the immediate container for the error location
     * @param glyph     the related glyph if any
     * @param text      the error message
     */
    public void addError (SystemNode container,
                          Glyph glyph,
                          String text)
    {
        if (Main.getGui() != null) {
            getErrorsEditor().addError(container, glyph, text);
        }
    }

    //-----------------//
    // addItemRenderer //
    //-----------------//
    @Override
    public boolean addItemRenderer (ItemRenderer renderer)
    {
        if (Main.getGui() != null) {
            ///return itemRenderers.add(new WeakItemRenderer(renderer));
            return itemRenderers.add(renderer);
        }

        return false;
    }

    //---------//
    // addPage //
    //---------//
    @Override
    public void addPage (Page page)
    {
        pages.add(page);
    }

    //-------//
    // close //
    //-------//
    @Override
    public void close (boolean bookIsClosing)
    {
        logger.debug("Closing sheet {} bookIsClosing:{}", this, bookIsClosing);

        // Close the related page
        book.removeSheet(this);

        // Close related UI assembly if any
        if (assembly != null) {
            SheetsController.getInstance().removeAssembly(this);
            assembly.close();
        }

        // If no sheet is left, force book closing
        if (!bookIsClosing) {
            if (!book.getSheets().isEmpty()) {
                logger.info("{}Sheet closed", getLogPrefix());
            } else {
                book.close();
            }
        }
    }

    //--------//
    // doStep //
    //--------//
    @Override
    public boolean doStep (Step target,
                           Collection<SystemInfo> systems)
    {
        if (isDone(target)) {
            return true;
        }

        SortedSet<Step> mySteps = new TreeSet();

        // Add all needed steps
        for (Step step : EnumSet.range(Step.first(), target)) {
            if (!isDone(step)) {
                mySteps.add(step);
            }
        }

        logger.debug("Sheet#{} scheduling {}", index, mySteps);

        StopWatch watch = new StopWatch("doStep " + target);

        try {
            StepMonitoring.notifyStart();

            for (Step step : mySteps) {
                watch.start(step.name());
                StepMonitoring.notifyMsg(getLogPrefix() + step);
                doOneStep(step, systems);
            }

            if (Main.getGui() != null) {
                // Update sheet tab color
                SheetsController.getInstance().markTab(this, Color.BLACK);
            }

            return true; // Normal exit
        } catch (StepException se) {
            logger.info("{}Processing stopped. {}", getLogPrefix(), se.getMessage());
        } catch (ProcessingCancellationException pce) {
            throw pce;
        } catch (Exception ex) {
            logger.warn(getLogPrefix() + " Error in performing " + mySteps, ex);
        } finally {
            StepMonitoring.notifyStop();

            if (constants.printStepWatch.isSet()) {
                watch.print();
            }
        }

        return false;
    }

    //-----------------//
    // dumpSystemInfos //
    //-----------------//
    /**
     * Utility method, to dump all sheet systems
     */
    public void dumpSystemInfos ()
    {
        System.out.println("--- SystemInfos ---");

        int i = 0;

        for (SystemInfo system : getSystems()) {
            Main.dumping.dump(system, "#" + i++);
        }

        System.out.println("--- SystemInfos end ---");
    }

    //------------//
    // ensureStep //
    //------------//
    @Override
    public boolean ensureStep (Step step)
    {
        if (!isDone(step)) {
            return doStep(step, null);
        }

        return true;
    }

    //--------//
    // export //
    //--------//
    @Override
    public void export ()
    {
        if (pages.isEmpty()) {
            return;
        }

        // path/to/scores/Book
        Path bookPath = BookManager.getActualPath(
                book.getExportPath(),
                BookManager.getDefaultExportPath(book));

        // Determine the output path (sans extension) for the provided sheet
        final Path sheetPathSansExt = getSheetPath(bookPath);

        try {
            if (book.isMultiSheet() && !Files.exists(bookPath)) {
                Files.createDirectories(bookPath);
            }

            final String rootName = sheetPathSansExt.getFileName().toString();
            final boolean compressed = BookManager.useCompression();
            final String ext = compressed ? COMPRESSED_SCORE_EXTENSION : SCORE_EXTENSION;
            final boolean sig = BookManager.useSignature();

            if (pages.size() > 1) {
                // Export the sheet multiple pages as separate scores in folder 'pathSansExt'
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
                score.addPage(pages.get(0));

                final Path scorePath = sheetPathSansExt.resolveSibling(rootName + ext);
                new ScoreExporter(score).export(scorePath, rootName, sig, compressed);
            }

            // Remember the book path in the book itself
            book.setExportPath(bookPath);
            BookManager.setDefaultExportDirectory(bookPath.getParent().toString());

            if (!book.isMultiSheet()) {
                book.getScript().addTask(new ExportTask(bookPath.getParent().toFile()));
            }
        } catch (Exception ex) {
            logger.warn("Error exporting " + this + ", " + ex, ex);
        }
    }

    //--------------//
    // deleteExport //
    //--------------//
    @Override
    public void deleteExport ()
    {
        if (!book.isMultiSheet()) {
            book.deleteExport(); // Simply delete the single-sheet book!
        } else {
            // path/to/scores/Book
            Path bookPath = BookManager.getActualPath(
                    book.getExportPath(),
                    BookManager.getDefaultExportPath(book));

            // Determine the output path (sans extension) for the provided sheet
            final Path sheetPathSansExt = getSheetPath(bookPath);

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
                BookManager.deletePaths(bookName + "/" + sheetName + " deletion", paths);
            }
        }
    }

    //-------//
    // print //
    //-------//
    @Override
    public void print ()
    {
        // path/to/prints/Book
        Path bookPath = BookManager.getActualPath(book.getPrintPath(), BookManager.getDefaultPrintPath(book));

        // Determine the output path (sans extension) for the provided sheet
        final Path sheetPathSansExt = getSheetPath(bookPath);
        final String rootName = sheetPathSansExt.getFileName().toString();
        final Path pdfPath = sheetPathSansExt.resolveSibling(rootName + ".pdf");

        // Actually write the PDF
        try {
            // Prompt for overwrite?
            if (!BookManager.confirmed(pdfPath)) {
                return;
            }

            if (book.isMultiSheet() && !Files.exists(bookPath)) {
                Files.createDirectories(bookPath);
            }

            new BookPdfOutput(book, pdfPath.toFile()).write(this);
            logger.info("Sheet printed to {}", pdfPath);

            book.setPrintPath(bookPath);
            BookManager.setDefaultPrintDirectory(bookPath.getParent().toString());

            if (!book.isMultiSheet()) {
                book.getScript().addTask(new PrintTask(bookPath.getParent().toFile()));
            }
        } catch (Exception ex) {
            logger.warn("Cannot write PDF to " + pdfPath, ex);
        }
    }

    //-------------//
    // getAssembly //
    //-------------//
    @Override
    public SheetAssembly getAssembly ()
    {
        return assembly;
    }

    //-------------//
    // getBeamGaps //
    //-------------//
    @Override
    public Population getBeamGaps ()
    {
        return beamGaps;
    }

    //----------//
    // getBench //
    //----------//
    @Override
    public SheetBench getBench ()
    {
        return bench;
    }

    //---------//
    // getBook //
    //---------//
    @Override
    public Book getBook ()
    {
        return book;
    }

    //----------------//
    // getCurrentStep //
    //----------------//
    @Override
    public Step getCurrentStep ()
    {
        return currentStep;
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the dimension of the sheet/page
     *
     * @return the page/sheet dimension in pixels
     */
    public Dimension getDimension ()
    {
        return picture.getDimension();
    }

    //-----------------//
    // getErrorsEditor //
    //-----------------//
    @Override
    public ErrorsEditor getErrorsEditor ()
    {
        return errorsEditor;
    }

    //----------------//
    // getFilterParam //
    //----------------//
    @Override
    public LiveParam<FilterDescriptor> getFilterParam ()
    {
        return filterContext;
    }

    //--------------//
    // getGlyphNest //
    //--------------//
    @Override
    public GlyphNest getGlyphNest ()
    {
        if (nest == null) {
            createNest();
        }

        return nest;
    }

    //-----------//
    // getHeight //
    //-----------//
    @Override
    public int getHeight ()
    {
        return picture.getHeight();
    }

    //-------//
    // getId //
    //-------//
    @Override
    public String getId ()
    {
        return id;
    }

    //----------//
    // getIndex //
    //----------//
    @Override
    public int getIndex ()
    {
        return index;
    }

    //-----------------//
    // getInterManager //
    //-----------------//
    @Override
    public InterManager getInterManager ()
    {
        return interManager;
    }

    //--------------//
    // getInterline //
    //--------------//
    @Override
    public int getInterline ()
    {
        return scale.getInterline();
    }

    //---------------//
    // getLagManager //
    //---------------//
    @Override
    public LagManager getLagManager ()
    {
        return lagManager;
    }

    //------------------//
    // getLanguageParam //
    //------------------//
    @Override
    public LiveParam<String> getLanguageParam ()
    {
        return textContext;
    }

    //-------------//
    // getLastPage //
    //-------------//
    /**
     * Report the last page of the sheet, if any.
     *
     * @return the last page or null
     */
    public Page getLastPage ()
    {
        if (pages.isEmpty()) {
            return null;
        }

        return pages.get(pages.size() - 1);
    }

    //---------------//
    // getLatestStep //
    //---------------//
    @Override
    public Step getLatestStep ()
    {
        Step latest = null;

        for (Step step : Step.values()) {
            if (isDone(step)) {
                latest = step;
            }
        }

        return latest;
    }

    //--------------------//
    // getLocationService //
    //--------------------//
    @Override
    public SelectionService getLocationService ()
    {
        return locationService;
    }

    //--------------//
    // getLogPrefix //
    //--------------//
    @Override
    public String getLogPrefix ()
    {
        if (BookManager.isMultiBook()) {
            return "[" + getId() + "] ";
        } else {
            if (book.isMultiSheet()) {
                return "[#" + getIndex() + "] ";
            } else {
                return "";
            }
        }
    }

    //-------------//
    // getMainStem //
    //-------------//
    @Override
    public int getMainStem ()
    {
        return stemScale.getMainThickness();
    }

    //------------//
    // getMaxStem //
    //------------//
    @Override
    public int getMaxStem ()
    {
        return stemScale.getMaxThickness();
    }

    //----------//
    // getPages //
    //----------//
    @Override
    public List<Page> getPages ()
    {
        return pages;
    }

    //------------//
    // getPicture //
    //------------//
    @Override
    public Picture getPicture ()
    {
        if (picture == null) {
            BufferedImage img = book.loadSheetImage(index);

            try {
                setImage(img);
            } catch (StepException ex) {
                logger.warn("Error setting image id " + index, ex);
            }
        }

        return picture;
    }

    //----------//
    // getScale //
    //----------//
    @Override
    public Scale getScale ()
    {
        return scale;
    }

    //---------------//
    // getSheetDelta //
    //---------------//
    @Override
    public SheetDiff getSheetDelta ()
    {
        return sheetDelta;
    }

    //---------//
    // getSkew //
    //---------//
    @Override
    public Skew getSkew ()
    {
        return skew;
    }

    //-----------------//
    // getStaffManager //
    //-----------------//
    @Override
    public StaffManager getStaffManager ()
    {
        return staffManager;
    }

    //----------------------//
    // getSymbolsController //
    //----------------------//
    @Override
    public SymbolsController getSymbolsController ()
    {
        if (symbolsController == null) {
            createSymbolsControllerAndEditor();
        }

        return symbolsController;
    }

    //------------------//
    // getSymbolsEditor //
    //------------------//
    @Override
    public SymbolsEditor getSymbolsEditor ()
    {
        return symbolsEditor;
    }

    //------------------//
    // getSystemManager //
    //------------------//
    @Override
    public SystemManager getSystemManager ()
    {
        return systemManager;
    }

    //------------//
    // getSystems //
    //------------//
    @Override
    public List<SystemInfo> getSystems ()
    {
        return systemManager.getSystems();
    }

    //----------//
    // getWidth //
    //----------//
    @Override
    public int getWidth ()
    {
        return picture.getWidth();
    }

    //--------//
    // isDone //
    //--------//
    @Override
    public boolean isDone (Step step)
    {
        return doneSteps.contains(step);
    }

    //-------------//
    // renderItems //
    //-------------//
    @Override
    public void renderItems (Graphics2D g)
    {
        if (Main.getGui() != null) {
            for (ItemRenderer renderer : itemRenderers) {
                renderer.renderItems(g);
            }
        }
    }

    //-------------//
    // setBeamGaps //
    //-------------//
    @Override
    public void setBeamGaps (Population beamGaps)
    {
        this.beamGaps = beamGaps;
    }

    //----------//
    // setImage //
    //----------//
    @Override
    public final void setImage (BufferedImage image)
            throws StepException
    {
        // Reset most of members
        reset(Step.LOAD);

        try {
            picture = new Picture(this, image, locationService);
            setPicture(picture);
            getBench().recordImageDimension(picture.getWidth(), picture.getHeight());

            done(Step.LOAD);
        } catch (ImageFormatException ex) {
            String msg = "Unsupported image format in file " + getBook().getInputPath() + "\n"
                         + ex.getMessage();

            if (Main.getGui() != null) {
                Main.getGui().displayWarning(msg);
            } else {
                logger.warn(msg);
            }

            throw new StepException(ex);
        } catch (Throwable ex) {
            logger.warn("Error loading image", ex);
        }
    }

    //----------//
    // setScale //
    //----------//
    @Override
    public void setScale (Scale scale)
    {
        this.scale = scale;
    }

    //---------------//
    // setSheetDelta //
    //---------------//
    public void setSheetDelta (SheetDiff sheetDelta)
    {
        this.sheetDelta = sheetDelta;
    }

    //---------//
    // setSkew //
    //---------//
    @Override
    public void setSkew (Skew skew)
    {
        this.skew = skew;
    }

    //--------------//
    // setStemScale //
    //--------------//
    @Override
    public void setStemScale (StemScale stemScale)
    {
        this.stemScale = stemScale;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Sheet " + id + "}";
    }

    //------//
    // done //
    //------//
    /**
     * Remember that the provided step has been completed on the sheet.
     *
     * @param step the provided step
     */
    private final void done (Step step)
    {
        doneSteps.add(step);
    }

    //------------//
    // createNest //
    //------------//
    private void createNest ()
    {
        // Beware: Glyph nest must subscribe to location before any lag,
        // to allow cleaning up of glyph data, before publication by a lag
        nest = new BasicNest("gNest", this);

        if (Main.getGui() != null) {
            nest.setServices(locationService);
        }
    }

    //----------------------------------//
    // createSymbolsControllerAndEditor //
    //----------------------------------//
    private void createSymbolsControllerAndEditor ()
    {
        SymbolsModel model = new SymbolsModel(this);
        symbolsController = new SymbolsController(model);

        if (Main.getGui() != null) {
            symbolsEditor = new SymbolsEditor(this, symbolsController);
        }
    }

    //-----------//
    // doOneStep //
    //-----------//
    /**
     * Do just one specified step, synchronously, with display of related UI if any
     * and recording of the step into the script.
     *
     * @param step the step to perform
     * @throws StepException
     */
    private void doOneStep (Step step,
                            Collection<SystemInfo> systems)
            throws StepException
    {
        try {
            long startTime = System.currentTimeMillis();
            logger.debug("{}{} starting", getLogPrefix(), step);
            setCurrentStep(step);

            // Reset sheet relevant data
            reset(step);

            // Clear errors for this step
            if (Main.getGui() != null) {
                getErrorsEditor().clearStep(step);
            }

            StepMonitoring.notifyStep(this, step); // Start

            // Standard processing on an existing sheet
            step.doit(systems, this);

            done(step); // Full completion

            final long stopTime = System.currentTimeMillis();
            final long duration = stopTime - startTime;
            logger.debug("{}{} completed in {} ms", getLogPrefix(), step, duration);

            // Record this in sheet->score bench
            bench.recordStep(step, duration);
        } catch (Throwable ex) {
            logger.warn("doOneStep error in " + step, ex);
            throw ex;
        } finally {
            // Make sure we reset the sheet "current" step, always.
            setCurrentStep(null);
            StepMonitoring.notifyStep(this, step); // Stop
        }
    }

    //--------------//
    // getSheetPath //
    //--------------//
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
     * @return the sheet path
     */
    private Path getSheetPath (Path bookPath)
    {
        // Determine the output path (sans extension) for the provided sheet
        // path/to/scores/Book            (for a single-sheet book)
        // path/to/scores/Book/sheet#N    (for a multi-sheet book)
        if (!book.isMultiSheet()) {
            return bookPath;
        } else {
            return bookPath.resolve(SHEET_PREFIX + (book.getOffset() + index));
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reinitialize the sheet members, according to step needs.
     *
     * @param step the starting step
     */
    private void reset (Step step)
    {
        switch (step) {
        case LOAD:
            picture = null;
            doneSteps.clear();
            currentStep = null;

        // Fall-through!
        case BINARY:
        case SCALE:
            scale = null;

        // Fall-through!
        case GRID:

            if (nest != null) {
                if (Main.getGui() != null) {
                    nest.cutServices(locationService);
                }

                createNest();
            }

            skew = null;

            lagManager.setLag(Lags.HLAG, null);
            lagManager.setLag(Lags.VLAG, null);

            staffManager.reset();
            symbolsController = null;
            symbolsEditor = null;

        // Fall-through!
        case LEDGERS:
            lagManager.setLag(Lags.LEDGER_LAG, null);

        // Fall-through!
        case BEAMS:
            lagManager.setLag(Lags.SPOT_LAG, null);

        default:
        }
    }

    //----------------//
    // setCurrentStep //
    //----------------//
    /**
     * This records the starting and stopping of a step.
     *
     * @param step the starting step, or null when step is over
     */
    private void setCurrentStep (Step step)
    {
        currentStep = step;
    }

    //------------//
    // setPicture //
    //------------//
    /**
     * Set the picture of this sheet, that is the image to be processed.
     *
     * @param picture the related picture
     */
    private void setPicture (Picture picture)
    {
        this.picture = picture;

        if (Main.getGui() != null) {
            locationService.subscribeStrongly(LocationEvent.class, picture);

            // Display sheet picture if not batch mode
            PictureView pictureView = new PictureView(this);
            assembly.addViewTab(
                    SheetTab.PICTURE_TAB,
                    pictureView,
                    new BoardsPane(new PixelBoard(this), new BinarizationBoard(this)));
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

        final Constant.Boolean printStepWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch for steps?");
    }
}
