//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B a s i c S h e e t                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
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

import omr.glyph.GlyphIndex;
import omr.glyph.SymbolsModel;
import omr.glyph.dynamic.FilamentIndex;
import omr.glyph.ui.SymbolsController;
import omr.glyph.ui.SymbolsEditor;

import omr.image.FilterDescriptor;
import omr.image.ImageFormatException;

import omr.lag.LagManager;
import omr.lag.Lags;

import omr.score.Page;
import omr.score.Score;
import omr.score.ScoreExporter;
import omr.score.ui.BookPdfOutput;

import omr.script.ExportTask;

import omr.sheet.ui.BinarizationBoard;
import omr.sheet.ui.PictureView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.SheetAssembly;
import omr.sheet.ui.SheetTab;
import omr.sheet.ui.StubsController;

import omr.sig.InterIndex;
import omr.sig.inter.Inter;
import omr.sig.relation.CrossExclusion;

import omr.step.ProcessingCancellationException;
import omr.step.Step;
import omr.step.StepException;
import omr.step.ui.StepMonitoring;

import omr.ui.BoardsPane;
import omr.ui.ErrorsEditor;
import omr.ui.selection.LocationEvent;
import omr.ui.selection.PixelEvent;
import omr.ui.selection.SelectionService;
import omr.ui.util.ItemRenderer;
import omr.ui.util.WeakItemRenderer;

import omr.util.Dumping;
import omr.util.FileUtil;
import omr.util.LiveParam;
import omr.util.Navigable;
import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
 * Class {@code BasicSheet} is our implementation of {@link Sheet} interface.
 * <p>
 * The picture below represents the data model used for marshalling/unmarshalling a sheet to/from
 * a sheet#n.xml file within a project .omr file
 * <p>
 * Most entities are represented here. Some Inter instances are listed only via their containing
 * entity, such as tuplets in MeasureStack, slurs and lyrics in Part, ledgers and bars in Staff,
 * graceChords and restChords in Measure, wholeChord in Voice.
 * <p>
 * Once an instance of BasicSheet has been unmarshalled, transient members of some entities need to
 * be properly set. This is the purpose of the "afterReload()" methods which are called in a certain
 * order as mentioned by the "(ar #n)" indications on these entities.
 * <p>
 * <img alt="Sheet Binding" src="doc-files/SheetBinding.png">
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "sheet")
public class BasicSheet
        implements Sheet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            Sheet.class);

    /** Events that can be published on sheet location service. */
    private static final Class<?>[] allowedEvents = new Class<?>[]{
        LocationEvent.class, PixelEvent.class
    };

    /** Un/marshalling context for use with JAXB. */
    private static volatile JAXBContext jaxbContext;

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Sheet 1-based number within book. (it duplicates stub number for clarity in XML) */
    @XmlAttribute(name = "number")
    private int number;

    /** The related picture. */
    @XmlElement(name = "picture")
    private Picture picture;

    /** Global scale for this sheet. */
    @XmlElement(name = "scale")
    private Scale scale;

    /** Global skew. */
    @XmlElement(name = "skew")
    private Skew skew;

    /** Corresponding page(s). A single sheet may relate to several pages. */
    @XmlElement(name = "page")
    private final List<Page> pages = new ArrayList<Page>();

    /** Cross-system exclusions. */
    //TODO: use ConcurrentHashMap?
    //TODO: handle persistency
    private final Map<Inter, List<CrossExclusion>> crossExclusions = new HashMap<Inter, List<CrossExclusion>>();

    /** Inter index for all systems in this sheet. */
    @XmlElement(name = "inter-index")
    private InterIndex interIndex;

    /** Global glyph index. */
    @XmlElement(name = "glyph-index")
    private GlyphIndex glyphIndex;

    // Transient data
    //---------------
    //
    /** Containing book. */
    @Navigable(false)
    private Book book;

    /** Corresponding sheet stub. */
    @Navigable(false)
    private SheetStub stub;

    /** Staves. */
    private StaffManager staffManager;

    /** Systems management. */
    private SystemManager systemManager;

    /** Dictionary of sheet lags. */
    private LagManager lagManager;

    //-- UI ----------------------------------------------------------------------------------------
    //
    /** Selections for this sheet. (SheetLocation, PixelLevel) */
    private SelectionService locationService;

    /** Registered item renderers, if any. */
    private Set<ItemRenderer> itemRenderers;

    /** Related errors editor, if any. */
    private ErrorsEditor errorsEditor;

    /** Specific builder dealing with glyphs. */
    private volatile SymbolsController symbolsController;

    /** Related symbols editor, if any. */
    private SymbolsEditor symbolsEditor;

    //-- resettable members ------------------------------------------------------------------------
    //
    /** Global filaments index. */
    private FilamentIndex filamentIndex;

    /** Delta measurements. */
    private SheetDiff sheetDelta;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new {@code Sheet} instance within a book.
     *
     * @param stub  the related sheet stub
     * @param image the already loaded image, if any
     * @throws omr.step.StepException
     */
    public BasicSheet (SheetStub stub,
                       BufferedImage image)
            throws StepException
    {
        Objects.requireNonNull(stub, "Cannot create a sheet in a null stub");

        glyphIndex = new GlyphIndex();

        initTransients(stub);

        interIndex = new InterIndex(this);

        if (image != null) {
            setImage(image);
        }
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private BasicSheet ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // getSheetFileName //
    //------------------//
    public static String getSheetFileName (int number)
    {
        return Sheet.INTERNALS_RADIX + number + ".xml";
    }

    //-----------------//
    // addItemRenderer //
    //-----------------//
    @Override
    public boolean addItemRenderer (ItemRenderer renderer)
    {
        if ((renderer != null) && (OMR.gui != null)) {
            return itemRenderers.add(new WeakItemRenderer(renderer));

            ///return itemRenderers.add(renderer);
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

    //-------------//
    // afterReload //
    //-------------//
    @Override
    public void afterReload (SheetStub stub)
    {
        try {
            // Complete sheet initialization
            initTransients(stub);

            // Make sure hLag & vLag are available and their sections dispatched to relevant systems
            if (stub.isDone(Step.GRID)) {
                systemManager.dispatchHorizontalSections();
                systemManager.dispatchVerticalSections();
            }

            for (SystemInfo system : getSystems()) {
                // Forward reload request down system hierarchy
                system.afterReload();
            }

            // Finally, complete inters index, now that all sigs have been populated
            interIndex.initTransients(this);
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //-------//
    // close //
    //-------//
    @Override
    public void close ()
    {
        stub.close();
    }

    //-------------------//
    // createPictureView //
    //-------------------//
    /**
     * Create and display the picture view.
     */
    public void createPictureView ()
    {
        locationService.subscribeStrongly(LocationEvent.class, picture);

        // Display sheet picture
        PictureView pictureView = new PictureView(this);
        stub.getAssembly().addViewTab(
                SheetTab.PICTURE_TAB,
                pictureView,
                new BoardsPane(new PixelBoard(this), new BinarizationBoard(this)));
    }

    //-------------//
    // createSheet //
    //-------------//
    @Override
    public Sheet createSheet ()
    {
        return this;
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
            Path bookPathSansExt = BookManager.getActualPath(
                    book.getExportPathSansExt(),
                    BookManager.getDefaultExportPathSansExt(book));

            // Determine the output path (sans extension) for the provided sheet
            final Path sheetPathSansExt = getSheetPathSansExt(bookPathSansExt);

            // Multi-sheet book: <bookname>-sheet#<N>.mvt<M>.mxl
            // Multi-sheet book: <bookname>-sheet#<N>.mxl
            final Path folder = sheetPathSansExt.getParent();
            final Path sheetName = sheetPathSansExt.getFileName(); // book-sheet#N

            final String dirGlob = "glob:**/" + sheetName + "{/**,}";
            final String filGlob = "glob:**/" + sheetName + "{/**,.*}";
            final List<Path> paths = FileUtil.walkDown(folder, dirGlob, filGlob);

            if (!paths.isEmpty()) {
                BookManager.deletePaths(sheetName + " deletion", paths);
            }
        }
    }

    //----------------//
    // displayDataTab //
    //----------------//
    @Override
    public void displayDataTab ()
    {
        symbolsEditor = new SymbolsEditor(this, getSymbolsController());
    }

    //-----------------//
    // displayMainTabs //
    //-----------------//
    @Override
    public void displayMainTabs ()
    {
        if (stub.isDone(Step.GRID)) {
            // Display DATA tab
            displayDataTab();
        } else {
            // Display BINARY tab
            createPictureView();
        }

        StubsController.getInstance().markTab(this, Color.BLACK);
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
            new Dumping().dump(system, "#" + i++);
        }

        System.out.println("--- SystemInfos end ---");
    }

    //------------//
    // ensureStep //
    //------------//
    @Override
    public boolean ensureStep (Step step)
    {
        return stub.ensureStep(step);
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
        Path bookPathSansExt = BookManager.getActualPath(
                book.getExportPathSansExt(),
                BookManager.getDefaultExportPathSansExt(book));

        // Determine the output path (sans extension) for the provided sheet
        final Path sheetPathSansExt = getSheetPathSansExt(bookPathSansExt);

        try {
            if (book.isMultiSheet() && !Files.exists(bookPathSansExt)) {
                Files.createDirectories(bookPathSansExt);
            }

            final String rootName = sheetPathSansExt.getFileName().toString();
            final boolean compressed = BookManager.useCompression();
            final String ext = compressed ? OMR.COMPRESSED_SCORE_EXTENSION : OMR.SCORE_EXTENSION;
            final boolean sig = BookManager.useSignature();

            if (pages.size() > 1) {
                // Export the sheet multiple pages as separate scores in folder 'sheetPathSansExt'
                Files.createDirectories(sheetPathSansExt);

                for (Page page : pages) {
                    final Score score = new Score();
                    score.addPage(page);

                    final int idx = 1 + pages.indexOf(page);
                    score.setId(idx);

                    final String scoreName = rootName + OMR.MOVEMENT_EXTENSION + idx;
                    final Path scorePath = sheetPathSansExt.resolve(scoreName + ext);
                    new ScoreExporter(score).export(scorePath, scoreName, sig, compressed);
                }
            } else {
                // Export the sheet single page as a score
                final Score score = new Score();
                score.setBook(book);
                score.setId(1);
                score.addPage(pages.get(0));

                final Path scorePath = sheetPathSansExt.resolveSibling(rootName + ext);
                new ScoreExporter(score).export(scorePath, rootName, sig, compressed);
            }

            // Remember the book path in the book itself
            book.setExportPathSansExt(bookPathSansExt);
            BookManager.setDefaultExportFolder(bookPathSansExt.getParent().toString());

            if (!book.isMultiSheet()) {
                book.getScript().addTask(new ExportTask(bookPathSansExt, null));
            }
        } catch (Exception ex) {
            logger.warn("Error exporting " + this + ", " + ex, ex);
        }
    }

    //-------------//
    // getAssembly //
    //-------------//
    @Override
    public SheetAssembly getAssembly ()
    {
        return stub.getAssembly();
    }

    //---------//
    // getBook //
    //---------//
    @Override
    public Book getBook ()
    {
        return book;
    }

    //--------------------//
    // getCrossExclusions //
    //--------------------//
    /**
     * @return the crossExclusions
     */
    @Override
    public Map<Inter, List<CrossExclusion>> getCrossExclusions ()
    {
        return crossExclusions;
    }

    //----------------//
    // getCurrentStep //
    //----------------//
    @Override
    public Step getCurrentStep ()
    {
        return stub.getCurrentStep();
    }

    //-----------------//
    // getErrorsEditor //
    //-----------------//
    @Override
    public ErrorsEditor getErrorsEditor ()
    {
        return errorsEditor;
    }

    //----------//
    // setImage //
    //----------//
    @Override
    public final void setImage (BufferedImage image)
            throws StepException
    {
        try {
            picture = new Picture(this, image, locationService);

            if (OMR.gui != null) {
                createPictureView();
            }

            done(Step.LOAD);
        } catch (ImageFormatException ex) {
            String msg = "Unsupported image format in file " + getBook().getInputPath() + "\n"
                         + ex.getMessage();

            if (OMR.gui != null) {
                OMR.gui.displayWarning(msg);
            } else {
                logger.warn(msg);
            }

            throw new StepException(ex);
        } catch (Throwable ex) {
            logger.warn("Error loading image", ex);
        }
    }

    //------------------//
    // getFilamentIndex //
    //------------------//
    @Override
    public FilamentIndex getFilamentIndex ()
    {
        if (filamentIndex == null) {
            filamentIndex = new FilamentIndex(this);
        }

        return filamentIndex;
    }

    //----------------//
    // getFilterParam //
    //----------------//
    @Override
    public LiveParam<FilterDescriptor> getFilterParam ()
    {
        return stub.getFilterParam();
    }

    //---------------//
    // getGlyphIndex //
    //---------------//
    @Override
    public GlyphIndex getGlyphIndex ()
    {
        if (glyphIndex == null) {
            glyphIndex = new GlyphIndex();
        }

        return glyphIndex;
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
        if (stub != null) {
            return stub.getId();
        }

        return null;
    }

    //---------------//
    // getInterIndex //
    //---------------//
    @Override
    public InterIndex getInterIndex ()
    {
        return interIndex;
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
        return stub.getLanguageParam();
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
        return stub.getLatestStep();
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
        return stub.getLogPrefix();
    }

    //-----------//
    // getNumber //
    //-----------//
    @Override
    public int getNumber ()
    {
        return stub.getNumber();
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
            BufferedImage img = book.loadSheetImage(getNumber());

            try {
                setImage(img);
            } catch (StepException ex) {
                logger.warn("Error setting image id " + getNumber(), ex);
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

    //----------//
    // getSheet //
    //----------//
    @Override
    public Sheet getSheet ()
    {
        return this;
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

    //---------//
    // getStub //
    //---------//
    @Override
    public SheetStub getStub ()
    {
        return stub;
    }

    //----------------------//
    // getSymbolsController //
    //----------------------//
    @Override
    public SymbolsController getSymbolsController ()
    {
        if (symbolsController == null) {
            createSymbolsController();
        }

        return symbolsController;
    }

    //------------------//
    // getSymbolsEditor //
    //------------------//
    @Override
    public SymbolsEditor getSymbolsEditor ()
    {
        if (symbolsEditor == null) {
            symbolsEditor = new SymbolsEditor(this, getSymbolsController());
        }

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

    //------------//
    // hasPicture //
    //------------//
    @Override
    public boolean hasPicture ()
    {
        return picture != null;
    }

    //----------//
    // hasSheet //
    //----------//
    @Override
    public boolean hasSheet ()
    {
        return true;
    }

    //------------//
    // invalidate //
    //------------//
    @Override
    public void invalidate ()
    {
        stub.invalidate();
    }

    //--------//
    // isDone //
    //--------//
    @Override
    public boolean isDone (Step step)
    {
        return stub.isDone(step);
    }

    //------------//
    // isModified //
    //------------//
    @Override
    public boolean isModified ()
    {
        return stub.isModified();
    }

    //---------//
    // isValid //
    //---------//
    @Override
    public boolean isValid ()
    {
        return stub.isValid();
    }

    //-------//
    // print //
    //-------//
    @Override
    public void print (Path sheetPrintPath)
    {
        // Actually write the PDF
        try {
            Path parent = sheetPrintPath.getParent();

            if (!Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            new BookPdfOutput(book, sheetPrintPath.toFile()).write(this);
            logger.info("Sheet printed to {}", sheetPrintPath);

            BookManager.setDefaultPrintFolder(parent.toString());
        } catch (Exception ex) {
            logger.warn("Cannot print sheet to " + sheetPrintPath + " " + ex, ex);
        }
    }

    //-------------//
    // renderItems //
    //-------------//
    @Override
    public void renderItems (Graphics2D g)
    {
        if (OMR.gui != null) {
            for (ItemRenderer renderer : itemRenderers) {
                renderer.renderItems(g);
            }
        }
    }

    //-------//
    // reset //
    //-------//
    @Override
    public void reset ()
    {
        stub.reset();
    }

    //----------------//
    // setCurrentStep //
    //----------------//
    @Override
    public void setCurrentStep (Step step)
    {
        stub.setCurrentStep(step);
    }

    //-------------//
    // setModified //
    //-------------//
    @Override
    public void setModified (boolean val)
    {
        stub.setModified(val);
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

    //-------//
    // store //
    //-------//
    @Override
    public void store (Path sheetFolder,
                       Path oldSheetFolder)
    {
        // Picture internals, if any
        if (picture != null) {
            try {
                // Make sure the folder exists for sheet internals
                Files.createDirectories(sheetFolder);

                // Save picture tables
                picture.store(sheetFolder, oldSheetFolder);
            } catch (IOException ex) {
                logger.warn("IOException on storing " + this, ex);
            }
        }

        // Sheet structure (sheet#n.xml)
        try {
            Path structurePath = sheetFolder.resolve(getSheetFileName(getNumber()));
            Files.deleteIfExists(structurePath);
            Files.createDirectories(sheetFolder);

            OutputStream os = Files.newOutputStream(structurePath, StandardOpenOption.CREATE);
            Marshaller m = getJaxbContext().createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(this, os);
            os.close();
            stub.setModified(false);
            logger.info("Stored {}", structurePath);
        } catch (Exception ex) {
            logger.warn("Error in saving sheet structure " + ex, ex);
        }
    }

    //------------//
    // storeSheet //
    //------------//
    @Override
    public void storeSheet ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //-----------//
    // swapSheet //
    //-----------//
    @Override
    public void swapSheet ()
    {
        stub.swapSheet();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "Sheet{" + getId() + "}";
    }

    //------------//
    // transcribe //
    //------------//
    @Override
    public boolean transcribe ()
    {
        return ensureStep(Step.last());
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the provided XML stream to allocate the corresponding sheet.
     *
     * @param in the input stream that contains the sheet in XML format.
     *           The stream is not closed by this method
     *
     * @return the allocated sheet.
     * @exception JAXBException raised when unmarshalling goes wrong
     */
    public static Sheet unmarshal (InputStream in)
            throws JAXBException
    {
        Unmarshaller um = getJaxbContext().createUnmarshaller();

        ///um.setListener(new Jaxb.UnmarshalLogger());
        SheetIdResolver resolver = new SheetIdResolver();
        um.setProperty(SheetIdResolver.getPropertyName(), resolver);

        BasicSheet sheet = (BasicSheet) um.unmarshal(in);
        logger.debug("Sheet unmarshalled");

        return sheet;
    }

    //--------//
    // doStep //
    //--------//
    /**
     * Perform a step, including intermediate ones if any, with online progress monitor
     * (method to be called from synchronized {@link #ensureStep(omr.step.Step)} only).
     * If any step throws {@link StepException} the processing is stopped.
     *
     * @param target  the targeted step
     * @param systems the impacted systems (null for all of them)
     * @return true if OK
     */
    boolean doStep (Step target,
                    Collection<SystemInfo> systems)
    {
        if (isDone(target)) {
            return true;
        }

        stub.setModified(true); // At beginning of processing

        SortedSet<Step> mySteps = new TreeSet();

        // Add all needed steps
        for (Step step : EnumSet.range(Step.first(), target)) {
            if (!isDone(step)) {
                mySteps.add(step);
            }
        }

        logger.debug("Sheet#{} scheduling {}", stub.getNumber(), mySteps);

        StopWatch watch = new StopWatch("doStep " + target);

        try {
            StepMonitoring.notifyStart();

            for (Step step : mySteps) {
                watch.start(step.name());
                StepMonitoring.notifyMsg(getLogPrefix() + step);
                doOneStep(step, systems);
            }

            if (OMR.gui != null) {
                // Update sheet tab color
                StubsController.getInstance().markTab(this, Color.BLACK);
            }

            return true; // Normal exit
        } catch (StepException se) {
            logger.info("{}Processing stopped. {}", getLogPrefix(), se.getMessage());
        } catch (ProcessingCancellationException pce) {
            throw pce;
        } catch (Exception ex) {
            logger.warn(getLogPrefix() + "Error in performing " + mySteps + " " + ex, ex);
        } finally {
            stub.setModified(true); // At end of processing
            StepMonitoring.notifyStop();

            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }

        return false;
    }

    //-------------------------//
    // createSymbolsController //
    //-------------------------//
    private void createSymbolsController ()
    {
        SymbolsModel model = new SymbolsModel(this);
        symbolsController = new SymbolsController(model);
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
            if (OMR.gui != null) {
                getErrorsEditor().clearStep(step);
            }

            StepMonitoring.notifyStep(this, step); // Start

            // Standard processing on an existing sheet
            step.doit(systems, this);

            final long stopTime = System.currentTimeMillis();
            final long duration = stopTime - startTime;
            logger.debug("{}{} completed in {} ms", getLogPrefix(), step, duration);

            done(step); // Full completion
        } catch (StepException ex) {
            throw ex;
        } catch (Throwable ex) {
            logger.warn("doOneStep error in " + step + " " + ex, ex);
            throw ex;
        } finally {
            setCurrentStep(null);
            StepMonitoring.notifyStep(this, step); // Stop
        }
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
            throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(BasicSheet.class);
        }

        return jaxbContext;
    }

    //------//
    // done //
    //------//
    /**
     * Remember that the provided step has been completed on the sheet.
     *
     * @param step the provided step
     */
    private void done (Step step)
    {
        ((BasicStub) stub).done(step);
    }

    //---------------------//
    // getSheetPathSansExt //
    //---------------------//
    /**
     * Report the path (sans extension) to which the sheet will be written.
     * <ul>
     * <li>If this sheet is the only one in the containing book, we use:<br/>
     * &lt;book-name&gt;</li>
     * <li>If the book contains several sheets, we use:<br/>
     * &lt;book-name&gt;-sheet#&lt;N&gt;</li>
     * </ul>
     *
     * @param bookPathSansExt the non-null bookPath (without extension)
     * @return the sheet path (without extension)
     */
    private Path getSheetPathSansExt (Path bookPathSansExt)
    {
        // Determine the output path (sans extension) for the provided sheet
        // path/to/scores/Book            (for a single-sheet book)
        // path/to/scores/Book-sheet#N    (for a multi-sheet book)
        if (!book.isMultiSheet()) {
            return bookPathSansExt;
        } else {
            final Integer offset = book.getOffset();
            final int globalNumber = getNumber() + ((offset != null) ? offset : 0);

            return Paths.get(bookPathSansExt + OMR.SHEET_SUFFIX + globalNumber);
        }
    }

    //----------------//
    // initTransients //
    //----------------//
    /**
     * Initialize needed transient members.
     * (which by definition have not been set by the unmarshalling).
     *
     * @param stub the related stub
     */
    private void initTransients (SheetStub stub)
    {
        logger.debug("BasicSheet#{} initTransients", stub.getNumber());

        this.stub = stub;
        this.number = stub.getNumber();

        book = stub.getBook();

        // Update UI information if so needed
        if (OMR.gui != null) {
            locationService = new SelectionService("locationService", allowedEvents);
            errorsEditor = new ErrorsEditor(this);
            itemRenderers = new HashSet<ItemRenderer>();
            addItemRenderer(staffManager);
        }

        if (picture != null) {
            picture.initTransients(this);
        }

        if (glyphIndex != null) {
            ((GlyphIndex) glyphIndex).initTransients(this);
        }

        for (Page page : pages) {
            page.initTransients(this);
        }

        if (systemManager == null) {
            systemManager = new SystemManager(this);
        } else {
            systemManager.initTransients(this);
        }

        // systemManager
        List<SystemInfo> systems = new ArrayList<SystemInfo>();

        for (Page page : pages) {
            for (SystemInfo system : page.getSystems()) {
                system.initTransients(this, page);
                systems.add(system);

                List<Staff> systemStaves = new ArrayList<Staff>();

                for (Part part : system.getParts()) {
                    part.setSystem(system);

                    for (Staff staff : part.getStaves()) {
                        systemStaves.add(staff);
                    }
                }

                system.setStaves(systemStaves);
            }
        }

        systemManager.setSystems(systems);

        staffManager = new StaffManager(this);

        lagManager = new LagManager(this);
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

        // Fall-through!
        case BINARY:
        case SCALE:
            scale = null;

        // Fall-through!
        case GRID:
            skew = null;

            lagManager.setLag(Lags.HLAG, null);
            lagManager.setLag(Lags.VLAG, null);

            staffManager.reset();
            symbolsController = null;
            symbolsEditor = null;

        default:
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    /**
     * Meant for JAXB handling of Sheet interface.
     */
    public static class Adapter
            extends XmlAdapter<BasicSheet, Sheet>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public BasicSheet marshal (Sheet s)
        {
            return (BasicSheet) s;
        }

        @Override
        public Sheet unmarshal (BasicSheet s)
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
                "Should we print out the stop watch for steps?");
    }
}
//    //----------//
//    // addError //
//    //----------//
//    /**
//     * Register an error in the sheet ErrorsWindow
//     *
//     * @param container the immediate container for the error location
//     * @param glyph     the related glyph if any
//     * @param text      the error message
//     */
//    public void addError (OldSystemNode container,
//                          Glyph glyph,
//                          String text)
//    {
//        if (OMR.gui != null) {
//            getErrorsEditor().addError(container, glyph, text);
//        }
//    }
//
