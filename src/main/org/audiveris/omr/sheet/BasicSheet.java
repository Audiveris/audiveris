//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B a s i c S h e e t                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.OMR;
import org.audiveris.omr.classifier.Annotations;
import org.audiveris.omr.classifier.AnnotationsBuilder;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.SampleSheet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.GlyphsModel;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.dynamic.FilamentIndex;
import org.audiveris.omr.glyph.ui.GlyphsController;
import org.audiveris.omr.glyph.ui.SymbolsEditor;
import org.audiveris.omr.image.ImageFormatException;
import org.audiveris.omr.lag.LagManager;
import org.audiveris.omr.lag.Lags;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.score.ScoreExporter;
import org.audiveris.omr.score.ScoreReduction;
import org.audiveris.omr.score.ui.BookPdfOutput;
import org.audiveris.omr.sheet.ui.BinarizationBoard;
import org.audiveris.omr.sheet.ui.PictureView;
import org.audiveris.omr.sheet.ui.PixelBoard;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.sig.InterIndex;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractPitchedInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.ui.InterController;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.ErrorsEditor;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.PixelEvent;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.util.ItemRenderer;
import org.audiveris.omr.ui.util.WeakItemRenderer;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.CREATE;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code BasicSheet} is our implementation of {@link Sheet} interface.
 * <p>
 * The picture below represents the data model used for marshalling/unmarshalling a sheet to/from
 * a sheet#n.xml file within a book .omr file
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
    /** Global id used to uniquely identify a persistent entity instance. */
    @XmlAttribute(name = "last-persistent-id")
    @XmlJavaTypeAdapter(Jaxb.AtomicIntegerAdapter.class)
    private final AtomicInteger lastPersistentId = new AtomicInteger(0);

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

    /** Global glyph index.
     * See annotated get/set methods: {@link #getGlyphIndexContent()}
     */
    private GlyphIndex glyphIndex;

    // Transient data
    //---------------
    //
    /** Corresponding sheet stub. */
    @Navigable(false)
    private SheetStub stub;

    /** Inter index for all systems in this sheet. */
    private InterIndex interIndex;

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
    private volatile GlyphsController glyphsController;

    /** Specific UI manager dealing with inters. */
    private volatile InterController interController;

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
     * Creates a new {@code BasicSheet} object with a binary table.
     *
     * @param stub        the related sheet stub
     * @param binaryTable the binary table, if any
     */
    public BasicSheet (SheetStub stub,
                       RunTable binaryTable)
    {
        this(stub);

        if (binaryTable != null) {
            setBinary(binaryTable);
        }
    }

    /**
     * Create a new {@code Sheet} instance with an image.
     *
     * @param stub  the related sheet stub
     * @param image the already loaded image, if any
     * @throws StepException if processing failed at this step
     */
    public BasicSheet (SheetStub stub,
                       BufferedImage image)
            throws StepException
    {
        this(stub);

        if (image != null) {
            setImage(image);
        }
    }

    /**
     * Create a new {@code Sheet} instance within a book.
     *
     * @param stub the related sheet stub
     */
    private BasicSheet (SheetStub stub)
    {
        Objects.requireNonNull(stub, "Cannot create a sheet in a null stub");

        glyphIndex = new GlyphIndex();

        initTransients(stub);

        interIndex = new InterIndex();
        interIndex.initTransients(this);
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
            // Predefined StaffHolder's are no longer useful
            Staff.StaffHolder.clearStaffHolders();

            // Complete sheet initialization
            initTransients(stub);

            // Make sure hLag & vLag are available and their sections dispatched to relevant systems
            if (stub.isDone(Step.GRID)) {
                systemManager.dispatchHorizontalSections();
                systemManager.dispatchVerticalSections();
            }

            interIndex = new InterIndex();

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

    //----------//
    // annotate //
    //----------//
    @Override
    public void annotate ()
    {
        try {
            final Book book = stub.getBook();
            final Path bookFolder = BookManager.getDefaultBookFolder(book);
            annotate(bookFolder);
        } catch (Exception ex) {
            logger.warn("Annotations failed {}", ex);
        }
    }

    //----------//
    // annotate //
    //----------//
    @Override
    public void annotate (Path sheetFolder)
    {
        OutputStream os = null;

        try {
            // Sheet annotations
            Path annPath = sheetFolder.resolve(getId() + Annotations.SHEET_ANNOTATIONS_SUFFIX);
            new AnnotationsBuilder(this, annPath).processSheet();

            // Sheet image
            Path imgPath = sheetFolder.resolve(getId() + Annotations.SHEET_IMAGE_SUFFIX);
            RunTable runTable = picture.getTable(Picture.TableKey.BINARY);
            BufferedImage img = runTable.getBufferedImage();
            os = Files.newOutputStream(imgPath, CREATE);
            ImageIO.write(img, Annotations.SHEET_IMAGE_FORMAT, os);
        } catch (Exception ex) {
            logger.warn("Error annotating {} {}", stub, ex.toString(), ex);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    //------------------//
    // createBinaryView //
    //------------------//
    /**
     * Create and display the binary view.
     */
    public void createBinaryView ()
    {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(
                        new Runnable()
                {
                    @Override
                    public void run ()
                    {
                        createBinaryView();
                    }
                });
            } catch (Exception ex) {
                logger.warn("invokeAndWait error", ex);
            }
        } else {
            if (stub.getAssembly().getPane(SheetTab.BINARY_TAB.label) != null) {
                return;
            }

            locationService.subscribeStrongly(LocationEvent.class, picture);

            // Display sheet binary
            PictureView pictureView = new PictureView(this);
            stub.getAssembly().addViewTab(
                    SheetTab.BINARY_TAB,
                    pictureView,
                    new BoardsPane(new PixelBoard(this), new BinarizationBoard(this)));
        }
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

    //
    //    //--------------//
    //    // deleteExport //
    //    //--------------//
    //    public void deleteExport ()
    //    {
    //        final Book book = getBook();
    //
    //        if (!book.isMultiSheet()) {
    //            book.deleteExport(); // Simply delete the single-sheet book!
    //        } else {
    //            // path/to/scores/Book
    //            Path bookPathSansExt = BookManager.getActualPath(
    //                    book.getExportPathSansExt(),
    //                    BookManager.getDefaultExportPathSansExt(book));
    //
    //            // Determine the output path (sans extension) for the provided sheet
    //            final Path sheetPathSansExt = getSheetPathSansExt(bookPathSansExt);
    //
    //            // Multi-sheet book: <bookname>-sheet#<N>.mvt<M>.mxl
    //            // Multi-sheet book: <bookname>-sheet#<N>.mxl
    //            final Path folder = sheetPathSansExt.getParent();
    //            final Path sheetName = sheetPathSansExt.getFileName(); // book-sheet#N
    //
    //            final String dirGlob = "glob:**/" + sheetName + "{/**,}";
    //            final String filGlob = "glob:**/" + sheetName + "{/**,.*}";
    //            final List<Path> paths = FileUtil.walkDown(folder, dirGlob, filGlob);
    //
    //            if (!paths.isEmpty()) {
    //                BookManager.deletePaths(sheetName + " deletion", paths);
    //            }
    //        }
    //    }
    //
    //----------------//
    // displayDataTab //
    //----------------//
    @Override
    public void displayDataTab ()
    {
        try {
            getSymbolsEditor();
        } catch (Throwable ex) {
            logger.warn("Error in displayDataTab " + ex, ex);
        }
    }

    //-----------------//
    // displayMainTabs //
    //-----------------//
    @Override
    public void displayMainTabs ()
    {
        if (stub.isDone(Step.GRID)) {
            displayDataTab(); // Display DATA tab
        } else if (stub.isDone(Step.BINARY)) {
            createBinaryView(); // Display BINARY tab
        } else {
            createPictureView(); // Display Picture tab
        }

        if (!stub.isValid()) {
            StubsController.getInstance().markTab(stub, Colors.SHEET_INVALID);
        }
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

    //--------//
    // export //
    //--------//
    @Override
    public void export (Path path)
    {
        if (pages.isEmpty()) {
            return;
        }

        final Book book = getBook();

        try {
            Path folder = path.getParent();

            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }

            final String ext = FileUtil.getExtension(path);
            final String sheetName = FileUtil.getNameSansExtension(path.getFileName());
            final boolean compressed = (ext.equals(OMR.COMPRESSED_SCORE_EXTENSION)) ? true
                    : ((ext.equals(OMR.SCORE_EXTENSION)) ? false
                    : BookManager.useCompression());
            final boolean useSig = BookManager.useSignature();

            int modifs = 0; // Count of modifications

            if (pages.size() > 1) {
                // One file per page
                for (PageRef pageRef : stub.getPageRefs()) {
                    final Score score = new Score();
                    score.setBook(book);
                    score.addPageRef(stub.getNumber(), pageRef);
                    modifs += new ScoreReduction(score).reduce();

                    final int idx = pageRef.getId();
                    final String scoreName = sheetName + OMR.MOVEMENT_EXTENSION + idx;
                    final Path scorePath = path.resolveSibling(scoreName + ext);
                    new ScoreExporter(score).export(scorePath, sheetName, useSig, compressed);
                }
            } else {
                // Export the sheet single page as a score
                final Score score = new Score();
                score.setBook(book);
                score.addPageRef(stub.getNumber(), stub.getFirstPageRef());
                modifs += new ScoreReduction(score).reduce();

                final String scoreName = sheetName;
                final Path scorePath = path.resolveSibling(scoreName + ext);
                new ScoreExporter(score).export(scorePath, scoreName, useSig, compressed);
            }

            if (modifs > 0) {
                book.setModified(true);
            }

            // Remember the book export path in the book itself
            book.setExportPathSansExt(folder.resolve(book.getRadix()));
        } catch (Exception ex) {
            logger.warn("Error exporting " + this + ", " + ex, ex);
        }
    }

    //-----------------//
    // getErrorsEditor //
    //-----------------//
    @Override
    public ErrorsEditor getErrorsEditor ()
    {
        return errorsEditor;
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

    //----------------------//
    // getGlyphsController //
    //----------------------//
    @Override
    public GlyphsController getGlyphsController ()
    {
        if (glyphsController == null) {
            createGlyphsController();
        }

        return glyphsController;
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

    //--------------------//
    // getInterController //
    //--------------------//
    @Override
    public InterController getInterController ()
    {
        if (interController == null) {
            interController = new InterController(this);
        }

        return interController;
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

    //--------------------//
    // getLocationService //
    //--------------------//
    @Override
    public SelectionService getLocationService ()
    {
        return locationService;
    }

    //----------//
    // getPages //
    //----------//
    @Override
    public List<Page> getPages ()
    {
        return pages;
    }

    //--------------------------//
    // getPersistentIdGenerator //
    //--------------------------//
    @Override
    public AtomicInteger getPersistentIdGenerator ()
    {
        return lastPersistentId;
    }

    //------------//
    // getPicture //
    //------------//
    @Override
    public Picture getPicture ()
    {
        if (picture == null) {
            BufferedImage img = getBook().loadSheetImage(stub.getNumber());

            try {
                setImage(img);
            } catch (StepException ex) {
                logger.warn("Error setting image id {}", stub.getNumber(), ex);
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
            String msg = "Unsupported image format in file " + stub.getBook().getInputPath()
                         + "\n" + ex.getMessage();

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
    public static BasicSheet unmarshal (InputStream in)
            throws JAXBException
    {
        Unmarshaller um = getJaxbContext().createUnmarshaller();

        ///um.setListener(new Jaxb.UnmarshalLogger());
        BasicSheet sheet = (BasicSheet) um.unmarshal(in);
        logger.debug("Sheet unmarshalled");

        return sheet;
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

    //------------------//
    // getSymbolsEditor //
    //------------------//
    @Override
    public SymbolsEditor getSymbolsEditor ()
    {
        if (symbolsEditor == null) {
            interController = new InterController(this);
            symbolsEditor = new SymbolsEditor(this, getGlyphsController(), interController);
            interController.setSymbolsEditor(symbolsEditor);
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

            new BookPdfOutput(getBook(), sheetPrintPath.toFile()).write(this);
            logger.info("Sheet printed to {}", sheetPrintPath);
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

    //--------//
    // sample //
    //--------//
    @Override
    public void sample ()
    {
        final Book book = getBook();
        final SampleRepository repository = book.getSpecificSampleRepository();
        final SampleSheet sampleSheet = repository.findSampleSheet(this);

        for (SystemInfo system : getSystems()) {
            SIGraph sig = system.getSig();

            for (Inter inter : sig.vertexSet()) {
                Shape shape = inter.getShape();
                Staff staff = inter.getStaff();
                Glyph glyph = inter.getGlyph();

                if ((shape != null) && (staff != null) && (glyph != null)) {
                    Double pitch = (inter instanceof AbstractPitchedInter)
                            ? ((AbstractPitchedInter) inter).getPitch() : null;
                    repository.addSample(
                            inter.getShape(),
                            glyph,
                            staff.getSpecificInterline(),
                            sampleSheet,
                            pitch);
                }
            }
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
            Path structurePath = sheetFolder.resolve(getSheetFileName(stub.getNumber()));
            Files.deleteIfExists(structurePath);
            Files.createDirectories(sheetFolder);
            Jaxb.marshal(this, structurePath, getJaxbContext());
            stub.setModified(false);
            logger.info("Stored {}", structurePath);
        } catch (Exception ex) {
            logger.warn("Error in saving sheet structure " + ex, ex);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "Sheet{" + getId() + "}";
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reinitialize the sheet members, according to step needs.
     *
     * @param step the starting step
     */
    void reset (Step step)
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
            pages.clear();
            stub.clearPageRefs();
            skew = null;

            lagManager.setLag(Lags.HLAG, null);
            lagManager.setLag(Lags.VLAG, null);

            staffManager.reset();
            systemManager.reset();
            glyphsController = null;
            symbolsEditor = null;

        default:
        }

        // Clear errors for this step
        if (OMR.gui != null) {
            getErrorsEditor().clearStep(step);
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

    //------------------------//
    // createGlyphsController //
    //------------------------//
    private void createGlyphsController ()
    {
        GlyphsModel model = new GlyphsModel(this, getGlyphIndex().getEntityService());
        glyphsController = new GlyphsController(model);
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

    //---------//
    // getBook //
    //---------//
    private Book getBook ()
    {
        return stub.getBook();
    }

    //----------------------//
    // getGlyphIndexContent //
    //----------------------//
    /**
     * Mean for JAXB marshalling only.
     *
     * @return collection of glyphs from glyphIndex.weakIndex
     */
    @SuppressWarnings("unchecked")
    @XmlElement(name = "glyph-index")
    @XmlJavaTypeAdapter(GlyphListAdapter.class)
    private ArrayList<Glyph> getGlyphIndexContent ()
    {
        if (glyphIndex == null) {
            return null;
        }

        return glyphIndex.getEntities();
    }

    //-----------//
    // getNumber //
    //-----------//
    /** Sheet 1-based number within book. */
    @XmlAttribute(name = "number")
    private int getNumber ()
    {
        return stub.getNumber();
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
        final Book book = getBook();

        if (!book.isMultiSheet()) {
            return bookPathSansExt;
        } else {
            final Integer offset = book.getOffset();
            final int globalNumber = stub.getNumber() + ((offset != null) ? offset : 0);

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

        // Update UI information if so needed
        if (OMR.gui != null) {
            locationService = new SelectionService("locationService", allowedEvents);
            errorsEditor = new ErrorsEditor(this);
            itemRenderers = new LinkedHashSet<ItemRenderer>();
            addItemRenderer(staffManager);
        }

        if (picture != null) {
            picture.initTransients(this);
        }

        if (glyphIndex != null) {
            glyphIndex.initTransients(this);
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

    //-----------//
    // setBinary //
    //-----------//
    private void setBinary (RunTable binaryTable)
    {
        try {
            picture = new Picture(this, binaryTable);

            if (OMR.gui != null) {
                createBinaryView();
            }

            done(Step.LOAD);
            done(Step.BINARY);
        } finally {
        }
    }

    //----------------------//
    // setGlyphIndexContent //
    //----------------------//
    /**
     * Meant for JAXB unmarshalling only.
     *
     * @param glyphs collection of glyphs to feed to the glyphIndex.weakIndex
     */
    @SuppressWarnings("unchecked")
    private void setGlyphIndexContent (ArrayList<Glyph> glyphs)
    {
        getGlyphIndex().setEntities(glyphs);
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
    // GlyphList // For glyphIndex (un)marshalling
    //-----------//
    private static class GlyphList
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlElement(name = "glyph")
        public ArrayList<Glyph> glyphs;

        //~ Constructors ---------------------------------------------------------------------------
        public GlyphList ()
        {
        }

        public GlyphList (ArrayList<Glyph> glyphs)
        {
            this.glyphs = glyphs;
        }
    }

    //------------------//
    // GlyphListAdapter // For glyphIndex (un)marshalling
    //------------------//
    private static class GlyphListAdapter
            extends XmlAdapter<GlyphList, ArrayList<Glyph>>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public GlyphList marshal (ArrayList<Glyph> glyphs)
                throws Exception
        {
            return new GlyphList(glyphs);
        }

        @Override
        public ArrayList<Glyph> unmarshal (GlyphList list)
                throws Exception
        {
            return list.glyphs;
        }
    }
}
