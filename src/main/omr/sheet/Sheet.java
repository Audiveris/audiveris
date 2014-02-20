//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S h e e t                                            //
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

import omr.glyph.BasicNest;
import omr.glyph.GlyphNest;
import omr.glyph.SymbolsModel;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.SymbolsController;
import omr.glyph.ui.SymbolsEditor;

import omr.grid.GridBuilder;
import omr.grid.StaffManager;

import omr.image.DistanceTable;
import omr.image.ImageFormatException;

import omr.lag.Lag;
import omr.lag.Lags;

import omr.run.RunsTable;

import omr.score.Score;
import omr.score.ScoresManager;
import omr.score.entity.Page;
import omr.score.entity.SystemNode;

import omr.selection.InterIdEvent;
import omr.selection.InterListEvent;
import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.PixelLevelEvent;
import omr.selection.SelectionHint;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.sheet.ui.BinarizationBoard;
import omr.sheet.ui.PictureView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.RunsViewer;
import omr.sheet.ui.SheetAssembly;
import omr.sheet.ui.SheetsController;

import omr.sig.Inter;
import omr.sig.SIGraph;
import omr.sig.SigManager;

import omr.step.Step;
import omr.step.StepException;
import omr.step.Stepping;
import omr.step.Steps;

import omr.ui.BoardsPane;
import omr.ui.ErrorsEditor;
import omr.ui.util.ItemRenderer;
import omr.ui.util.WeakItemRenderer;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class {@code Sheet} is the central hub for Sheet processing, keeping pointers to all
 * processing related to the image, and to their results.
 *
 * @author Hervé Bitteur
 */
public class Sheet
        implements EventSubscriber<UserEvent>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Sheet.class);

    /** Events that can be published on sheet location service. */
    public static final Class<?>[] allowedEvents = new Class<?>[]{
        LocationEvent.class, PixelLevelEvent.class,
        InterListEvent.class, InterIdEvent.class
    };

    /** Events read by sheet on location service. */
    public static final Class<?>[] eventsRead = new Class<?>[]{
        LocationEvent.class, InterListEvent.class,
        InterIdEvent.class
    };

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Corresponding page. */
    private final Page page;

    /** Containing score. */
    private final Score score;

    /**
     * Selections for this sheet.
     * (SheetLocation, PixelLevel, InterList)
     */
    private final SelectionService locationService;

    /** The recording of key processing data. */
    private final SheetBench bench;

    /** Related assembly instance, if any. */
    private final SheetAssembly assembly;

    /** Related errors editor, if any. */
    private final ErrorsEditor errorsEditor;

    /** Retrieved systems. */
    private final List<SystemInfo> systems = new ArrayList<SystemInfo>();

    /** SIG manager for all systems. */
    private final SigManager sigManager = new SigManager();

    /** Global sheet SIG. */
    private final SIGraph sheetSig;

    //-- resettable members ----------------------------------------------------
    //
    /** The related picture */
    private Picture picture;

    /** All steps already done on this sheet */
    private Set<Step> doneSteps = new HashSet<Step>();

    /** The step being done on this sheet */
    private Step currentStep;

    /** Global scale for this sheet */
    private Scale scale;

    /** Table of all vertical (foreground) runs. */
    private RunsTable wholeVerticalTable;

    /** Image of distances to foreground. */
    private DistanceTable distanceImage;

    /** Initial skew value */
    private Skew skew;

    /** Map of all public lags. */
    Map<String, Lag> lagMap = new TreeMap<String, Lag>();

    /** Global glyph nest */
    private GlyphNest nest;

    // Companion processors
    //
    /** Scale */
    private ScaleBuilder scaleBuilder;

    /** Spots */
    private SpotsBuilder spotsBuilder;

    /** Staves */
    private final StaffManager staffManager;

    /** Grid */
    private GridBuilder gridBuilder;

    /** Systems management. */
    private volatile SystemManager systemManager;

    /** Specific builder dealing with glyphs */
    private volatile SymbolsController symbolsController;

    /** Related symbols editor */
    private SymbolsEditor symbolsEditor;

    /** Delta measurements. */
    private SheetDiff sheetDelta;

    /** Id of last long horizontal section */
    private int lastLongHSectionId = -1;

    /** Registered item renderers, if any */
    private final Set<ItemRenderer> itemRenderers = new HashSet<ItemRenderer>();

    /** Display of runs tables. */
    private final RunsViewer runsViewer;

    //~ Constructors -------------------------------------------------------------------------------
    //
    //-------//
    // Sheet //
    //-------//
    /**
     * Create a new {@code Sheet} instance, based on a couple made of
     * an image (the original pixel input) and a page (the score
     * entities output).
     *
     * @param page  the related score page
     * @param image the already loaded image
     * @throws omr.step.StepException
     */
    public Sheet (Page page,
                  BufferedImage image)
            throws StepException
    {
        this.page = page;
        this.score = page.getScore();

        sheetSig = new SIGraph(this);

        locationService = new SelectionService("sheet", allowedEvents);

        for (Class<?> eventClass : eventsRead) {
            locationService.subscribeStrongly(eventClass, this);
        }

        staffManager = new StaffManager(this);
        bench = new SheetBench(this);

        // Update UI information if so needed
        if (Main.getGui() != null) {
            errorsEditor = new ErrorsEditor(this);
            assembly = Main.getGui().sheetsController.createAssembly(this);
        } else {
            errorsEditor = null;
            assembly = null;
        }

        setImage(image);

        runsViewer = (Main.getGui() != null) ? new RunsViewer(this) : null;

        logger.debug("Created {}", this);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // done //
    //------//
    /**
     * Remember that the provided step has been completed on the sheet.
     *
     * @param step the provided step
     */
    public final void done (Step step)
    {
        if (step.isMandatory()) {
            doneSteps.add(step);
        }
    }

    //----------//
    // setImage //
    //----------//
    public final void setImage (BufferedImage image)
            throws StepException
    {
        // Reset most of members
        reset(Steps.valueOf(Steps.LOAD));

        try {
            picture = new Picture(this, image, locationService);
            setPicture(picture);
            getBench().recordImageDimension(picture.getWidth(), picture.getHeight());

            done(Steps.valueOf(Steps.LOAD));
        } catch (ImageFormatException ex) {
            String msg = "Unsupported image format in file " + getScore().getImagePath() + "\n"
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
    /**
     * Register an items renderer to renderAttachments items.
     *
     * @param renderer the additional renderer
     */
    public void addItemRenderer (ItemRenderer renderer)
    {
        itemRenderers.add(new WeakItemRenderer(renderer));
    }

    //------------//
    // createNest //
    //------------//
    public GlyphNest createNest ()
    {
        // Beware: Glyph nest must subscribe to location before any lag,
        // to allow cleaning up of glyph data, before publication by a lag
        nest = new BasicNest("gNest", this);
        nest.setServices(locationService);

        return nest;
    }

    //----------------------------------//
    // createSymbolsControllerAndEditor //
    //----------------------------------//
    public void createSymbolsControllerAndEditor ()
    {
        SymbolsModel model = new SymbolsModel(this);
        symbolsController = new SymbolsController(model);

        if (Main.getGui() != null) {
            symbolsEditor = new SymbolsEditor(this, symbolsController);
        }
    }

    //---------------------//
    // createSystemManager //
    //---------------------//
    public void createSystemManager ()
    {
        page.resetSystems();
        systemManager = new SystemManager(this);
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
    // getAllLags //
    //------------//
    /**
     * Report all currently registered lags at this sheet instance.
     *
     * @return the collection of all registered lags, some of which may be null
     */
    public Collection<Lag> getAllLags ()
    {
        return lagMap.values();
    }

    //-------------//
    // getAssembly //
    //-------------//
    /**
     * Report the related SheetAssembly for GUI
     *
     * @return the assembly, or null otherwise
     */
    public SheetAssembly getAssembly ()
    {
        return assembly;
    }

    //----------//
    // getBench //
    //----------//
    /**
     * Report the related sheet bench
     *
     * @return the related bench
     */
    public SheetBench getBench ()
    {
        return bench;
    }

    //----------------//
    // getCurrentStep //
    //----------------//
    /**
     * Retrieve the step being processed "as we speak"
     *
     * @return the current step
     */
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

    //------------------//
    // getDistanceImage //
    //------------------//
    /**
     * Get access to the distance transform image
     *
     * @return the image of distances (to foreground)
     */
    public DistanceTable getDistanceImage ()
    {
        return distanceImage;
    }

    //-----------------//
    // getErrorsEditor //
    //-----------------//
    public ErrorsEditor getErrorsEditor ()
    {
        return errorsEditor;
    }

    //----------------//
    // getGridBuilder //
    //----------------//
    /**
     * @return the gridBuilder
     */
    public GridBuilder getGridBuilder ()
    {
        if (gridBuilder == null) {
            gridBuilder = new GridBuilder(this);
        }

        return gridBuilder;
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the picture height in pixels
     *
     * @return the picture height
     */
    public int getHeight ()
    {
        return picture.getHeight();
    }

    //-------//
    // getId //
    //-------//
    public String getId ()
    {
        return page.getId();
    }

    //--------------//
    // getInterline //
    //--------------//
    /**
     * Convenient method to report the scaling information of the sheet
     *
     * @return the scale interline
     */
    public int getInterline ()
    {
        return scale.getInterline();
    }

    //------------------//
    // getItemRenderers //
    //------------------//
    /**
     * Report the (live) collection of item renderers
     *
     * @return the live set if item renderers
     */
    public Set<ItemRenderer> getItemRenderers ()
    {
        return itemRenderers;
    }

    //--------//
    // getLag //
    //--------//
    /**
     * Report the desired lag.
     *
     * @param key the lag name
     * @return the lag if already registered, null otherwise
     */
    public Lag getLag (String key)
    {
        return lagMap.get(key);
    }

    //--------------------//
    // getLocationService //
    //--------------------//
    /**
     * Report the sheet selection service (for LocationEvent & PixelLevelEvent)
     *
     * @return the sheet dedicated event service
     */
    public SelectionService getLocationService ()
    {
        return locationService;
    }

    //--------------//
    // getLogPrefix //
    //--------------//
    /**
     * Report the proper prefix to use when logging a message
     *
     * @return the proper prefix
     */
    public String getLogPrefix ()
    {
        if (ScoresManager.isMultiScore()) {
            return "[" + getId() + "] ";
        } else {
            if (score.isMultiPage()) {
                return "[#" + page.getIndex() + "] ";
            } else {
                return "";
            }
        }
    }

    //---------------------//
    // getLongSectionMaxId //
    //---------------------//
    /**
     * Report the id of the last long horizontal section
     *
     * @return the id of the last long horizontal section
     */
    public int getLongSectionMaxId ()
    {
        return lastLongHSectionId;
    }

    //---------//
    // getNest //
    //---------//
    /**
     * Report the global nest for glyphs of this sheet, or null
     *
     * @return the nest for glyphs, perhaps null
     */
    public GlyphNest getNest ()
    {
        return nest;
    }

    //---------//
    // getPage //
    //---------//
    /**
     * @return the page
     */
    public Page getPage ()
    {
        return page;
    }

    //------------//
    // getPicture //
    //------------//
    /**
     * Report the picture of this sheet, that is the image to be processed.
     *
     * @return the related picture
     */
    public Picture getPicture ()
    {
        return picture;
    }

    //---------------//
    // getRunsViewer //
    //---------------//
    public RunsViewer getRunsViewer ()
    {
        return runsViewer;
    }

    //----------//
    // getScale //
    //----------//
    /**
     * Report the computed scale of this sheet.
     * This drives several processing thresholds.
     *
     * @return the sheet scale
     */
    public Scale getScale ()
    {
        return scale;
    }

    //-----------------//
    // getScaleBuilder //
    //-----------------//
    /**
     * @return the scaleBuilder
     */
    public ScaleBuilder getScaleBuilder ()
    {
        if (scaleBuilder == null) {
            scaleBuilder = new ScaleBuilder(this);
        }

        return scaleBuilder;
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Return the eventual Score that gathers in a score the information
     * retrieved from this sheet.
     *
     * @return the related score, or null if not available
     */
    public Score getScore ()
    {
        return score;
    }

    //----------------------//
    // getSelectedInterList //
    //----------------------//
    /**
     * Report the currently selected list of interpretations if any
     *
     * @return the current list or null
     */
    @SuppressWarnings("unchecked")
    public List<Inter> getSelectedInterList ()
    {
        return (List<Inter>) locationService.getSelection(InterListEvent.class);
    }

    //---------------//
    // getSheetDelta //
    //---------------//
    /**
     * @return the sheetDelta
     */
    public SheetDiff getSheetDelta ()
    {
        return sheetDelta;
    }

    //-------------//
    // getSheetSig //
    //-------------//
    public SIGraph getSheetSig ()
    {
        return sheetSig;
    }

    //---------------//
    // getSigManager //
    //---------------//
    /**
     * Report the SIG manager for this sheet
     *
     * @return the sheet SIG's manager
     */
    public SigManager getSigManager ()
    {
        return sigManager;
    }

    //---------//
    // getSkew //
    //---------//
    /**
     * Report the skew information for this sheet.
     *
     * @return the skew information
     */
    public Skew getSkew ()
    {
        return skew;
    }

    //-----------------//
    // getSpotsBuilder //
    //-----------------//
    /**
     * @return the spotsBuilder
     */
    public SpotsBuilder getSpotsBuilder ()
    {
        if (spotsBuilder == null) {
            spotsBuilder = new SpotsBuilder(this);
        }

        return spotsBuilder;
    }

    //-----------------//
    // getStaffManager //
    //-----------------//
    /**
     * @return the staffManager
     */
    public StaffManager getStaffManager ()
    {
        return staffManager;
    }

    //----------------------//
    // getSymbolsController //
    //----------------------//
    /**
     * Give access to the module dealing with symbol management
     *
     * @return the symbols model
     */
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
    /**
     * Give access to the UI dealing with symbol recognition
     *
     * @return the symbols symbolsEditor
     */
    public SymbolsEditor getSymbolsEditor ()
    {
        return symbolsEditor;
    }

    //---------------//
    // getSystemById //
    //---------------//
    /**
     * Report the system info for which id is provided
     *
     * @param id id of desired system
     * @return the desired system info
     */
    public SystemInfo getSystemById (int id)
    {
        return systems.get(id - 1);
    }

    //------------------//
    // getSystemManager //
    //------------------//
    /**
     * Give access to the system manager
     *
     * @return the SystemManager instance
     */
    public SystemManager getSystemManager ()
    {
        return systemManager;
    }

    //------------//
    // getSystems //
    //------------//
    /**
     * Report an unmodifiable view on current systems.
     *
     * @return a view on systems list
     */
    public List<SystemInfo> getSystems ()
    {
        return Collections.unmodifiableList(systems);
    }

    //-----------------------//
    // getWholeVerticalTable //
    //-----------------------//
    /**
     * Get access to the whole table of vertical runs.
     *
     * @return the wholeVerticalTable
     */
    public RunsTable getWholeVerticalTable ()
    {
        return wholeVerticalTable;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the picture width in pixels
     *
     * @return the picture width
     */
    public int getWidth ()
    {
        return picture.getWidth();
    }

    //--------//
    // isDone //
    //--------//
    /**
     * Report whether the specified step has been performed onn this sheet
     *
     * @param step the step to check
     * @return true if already performed
     */
    public boolean isDone (Step step)
    {
        return doneSteps.contains(step);
    }

    //--------------//
    // isOnPatterns //
    //--------------//
    /**
     * Check whether current step is SYMBOLS.
     *
     * @return true if on SYMBOLS
     */
    public boolean isOnPatterns ()
    {
        return Stepping.getLatestStep(this) == Steps.valueOf(Steps.SYMBOLS);
    }

    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            if (event instanceof LocationEvent) {
                // Location => InterList
                handleEvent((LocationEvent) event);
            } else if (event instanceof InterListEvent) {
                // InterList => contour
                handleEvent((InterListEvent) event);
            } else if (event instanceof InterIdEvent) {
                // InterId => inter
                handleEvent((InterIdEvent) event);
            }
        } catch (Throwable ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove this sheet from the containing score
     */
    public void remove (boolean closing)
    {
        logger.debug("remove sheet {} closing:{}", this, closing);

        // Close the related page
        getScore().remove(page);

        // Close related UI assembly if any
        if (assembly != null) {
            SheetsController.getInstance().removeAssembly(this);
            assembly.close();
        }

        // If no sheet is left, force score closing
        if (!closing) {
            if (!score.getPages().isEmpty()) {
                logger.info("{}Removed page #{}", page.getScore().getLogPrefix(), page.getIndex());
            } else {
                score.close();
            }
        }
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Render all the registered items
     *
     * @param g the graphics environment
     */
    public void renderItems (Graphics2D g)
    {
        for (ItemRenderer renderer : itemRenderers) {
            renderer.renderItems(g);
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
    public void reset (Step step)
    {
        switch (step.getName()) {
        case Steps.LOAD:
            picture = null;
            doneSteps = new HashSet<Step>();
            currentStep = null;

        // Fall-through!
        case Steps.BINARY:
        case Steps.SCALE:
            scaleBuilder = null;
            scale = null;
            wholeVerticalTable = null;

        // Fall-through!
        case Steps.GRID:

            if (nest != null) {
                nest.cutServices(locationService);
                nest = null;
            }

            skew = null;

            setLag(Lags.HLAG, null);
            setLag(Lags.VLAG, null);

            systems.clear();
            gridBuilder = null;

            staffManager.reset();
            systemManager = null;
            symbolsController = null;
            symbolsEditor = null;

        // Fall-through!
        case Steps.LEDGERS:
            setLag(Lags.FULL_HLAG, null);

        // Fall-through!
        case Steps.BEAMS:
            setLag(Lags.SPOT_LAG, null);

        default:
        }
    }

    //----------------//
    // setCurrentStep //
    //----------------//
    /**
     * This records the starting of a step.
     *
     * @param step the starting step
     */
    public void setCurrentStep (Step step)
    {
        currentStep = step;
    }

    //------------------//
    // setDistanceImage //
    //------------------//
    /**
     * Remember the distance transform image
     *
     * @param distanceImage the image of distances (to foreground)
     */
    public void setDistanceImage (DistanceTable distanceImage)
    {
        this.distanceImage = distanceImage;

        // Save this distance image on disk for visual check
        //TableUtil.store(getId() + ".dist", distanceImage);
    }

    //--------//
    // setLag //
    //--------//
    /**
     * Register the provided lag.
     *
     * @param key the registered key for the lag
     * @param lag the lag to register, perhaps null
     */
    public void setLag (String key,
                        Lag lag)
    {
        Lag oldLag = getLag(key);

        if (oldLag != null) {
            oldLag.cutServices();
        }

        lagMap.put(key, lag);

        if (lag != null) {
            lag.setServices(locationService);
        }
    }

    //---------------------//
    // setLongSectionMaxId //
    //---------------------//
    /**
     * Remember the id of the last long horizontal section
     *
     * @param id the id of the last long horizontal section
     */
    public void setLongSectionMaxId (int id)
    {
        lastLongHSectionId = id;
    }

    //----------//
    // setScale //
    //----------//
    /**
     * Link scale information to this sheet
     *
     * @param scale the computed (or read from score file) scale
     * @throws StepException
     */
    public void setScale (Scale scale)
            throws StepException
    {
        this.scale = scale;
        page.setScale(scale);
    }

    //---------------//
    // setSheetDelta //
    //---------------//
    /**
     * @param sheetDelta the sheetDelta to set
     */
    public void setSheetDelta (SheetDiff sheetDelta)
    {
        this.sheetDelta = sheetDelta;
    }

    //---------//
    // setSkew //
    //---------//
    /**
     * Link skew information to this sheet
     *
     * @param skew the skew information
     */
    public void setSkew (Skew skew)
    {
        this.skew = skew;
    }

    //------------//
    // setSystems //
    //------------//
    /**
     * Assign the whole sequence of systems
     *
     * @param systems the (new) systems
     */
    public void setSystems (Collection<SystemInfo> systems)
    {
        if (this.systems != systems) {
            this.systems.clear();
            this.systems.addAll(systems);
        }
    }

    //-----------------------//
    // setWholeVerticalTable //
    //-----------------------//
    /**
     * Remember the whole table of vertical runs.
     *
     * @param wholeVerticalTable the wholeVerticalTable to set
     */
    public void setWholeVerticalTable (RunsTable wholeVerticalTable)
    {
        this.wholeVerticalTable = wholeVerticalTable;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Sheet " + page.getId() + "}";
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in sheet location => interpretation(s)
     *
     * @param locationEvent
     */
    private void handleEvent (LocationEvent locationEvent)
    {
        SelectionHint hint = locationEvent.hint;
        MouseMovement movement = locationEvent.movement;
        Rectangle rect = locationEvent.getData();

        if (!hint.isLocation() && !hint.isContext()) {
            return;
        }

        if ((rect == null) || (systemManager == null)) {
            return;
        }

        final Set<Inter> inters = new LinkedHashSet<Inter>();

        for (SystemInfo system : systemManager.getSystemsOf(rect.getLocation())) {
            SIGraph sig = system.getSig();

            if ((rect.width > 0) && (rect.height > 0)) {
                // This is a non-degenerated rectangle
                // Look for contained interpretations
                inters.addAll(sig.containedInters(rect));
            } else {
                // This is just a point
                // Look for intersected interpretations
                inters.addAll(sig.containingInters(rect.getLocation()));
            }
        }

        // Publish inters found (perhaps none)
        locationService.publish(
                new InterListEvent(this, hint, movement, new ArrayList<Inter>(inters)));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Inter => inter contour
     *
     * @param interListEvent
     */
    private void handleEvent (InterListEvent interListEvent)
    {
        SelectionHint hint = interListEvent.hint;
        MouseMovement movement = interListEvent.movement;
        List<Inter> inters = interListEvent.getData();

        if (hint == SelectionHint.INTER_INIT) {
            // Display (last) inter contour
            if ((inters != null) && !inters.isEmpty()) {
                Inter inter = inters.get(inters.size() - 1);
                Rectangle box = inter.getBounds();
                locationService.publish(new LocationEvent(this, hint, movement, box));
            }
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in InterId => inter
     *
     * @param interIdEvent
     */
    private void handleEvent (InterIdEvent interIdEvent)
    {
        SelectionHint hint = interIdEvent.hint;
        MouseMovement movement = interIdEvent.movement;
        int id = interIdEvent.getData();

        Inter inter = sigManager.getInter(id);
        locationService.publish(
                new InterListEvent(this, hint, movement, (inter != null) ? Arrays.asList(inter) : null));
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

        locationService.subscribeStrongly(LocationEvent.class, picture);

        // Display sheet picture if not batch mode
        if (Main.getGui() != null) {
            PictureView pictureView = new PictureView(Sheet.this);
            assembly.addViewTab(
                    Step.PICTURE_TAB,
                    pictureView,
                    new BoardsPane(new PixelBoard(this), new BinarizationBoard(this)));
        }
    }
}
