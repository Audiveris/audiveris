//----------------------------------------------------------------------------//
//                                                                            //
//                                 S h e e t                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.glyph.BasicNest;
import omr.glyph.Glyphs;
import omr.glyph.Nest;
import omr.glyph.Shape;
import omr.glyph.SymbolsModel;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.SymbolsController;
import omr.glyph.ui.SymbolsEditor;

import omr.grid.GridBuilder;
import omr.grid.StaffManager;
import omr.grid.TargetBuilder;


import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;
import omr.lag.Sections;

import omr.run.RunsTable;

import omr.score.Score;
import omr.score.ScoresManager;
import omr.score.entity.Page;
import omr.score.entity.SystemNode;

import omr.selection.InterListEvent;
import omr.selection.InterIdEvent;
import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.PixelLevelEvent;
import omr.selection.SelectionHint;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.image.DistanceTable;
import omr.image.ImageFormatException;
import omr.image.Picture;
import omr.image.PictureView;

import omr.sheet.ui.BinarizationBoard;
import omr.sheet.ui.BoundaryEditor;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.RunsViewer;
import omr.sheet.ui.SheetAssembly;
import omr.sheet.ui.SheetsController;

import omr.sig.Inter;
import omr.sig.SigManager;
import omr.sig.SIGraph;

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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class {@code Sheet} is the central hub for Sheet processing,
 * keeping pointers to all processings related to the image, and to
 * their results.
 *
 * @author Hervé Bitteur
 */
public class Sheet
        implements EventSubscriber<UserEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Sheet.class);

    /** Events that can be published on sheet location service. */
    public static final Class<?>[] allowedEvents = new Class<?>[]{
        LocationEvent.class,
        PixelLevelEvent.class,
        InterListEvent.class,
        InterIdEvent.class
    };

    /** Events read by sheet on location service. */
    public static final Class<?>[] eventsRead = new Class<?>[]{
        LocationEvent.class,
        InterListEvent.class,
        InterIdEvent.class
    };

    //~ Instance fields --------------------------------------------------------
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
    private final List<SystemInfo> systems = new ArrayList<>();

    /** SIG manager for all systems. */
    private final SigManager sigManager = new SigManager();

    //-- resettable members ----------------------------------------------------
    //
    /** The related picture */
    private Picture picture;

    /** All steps already done on this sheet */
    private Set<Step> doneSteps = new HashSet<>();

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
    private Nest nest;

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

    /** Bars checker */
    private volatile BarsChecker barsChecker;

    /** A bar line extractor for this sheet */
    private volatile SystemsBuilder systemsBuilder;

    /** Specific builder dealing with glyphs */
    private volatile SymbolsController symbolsController;

    /** Related target builder */
    private volatile TargetBuilder targetBuilder;

    /** Related symbols editor */
    private SymbolsEditor symbolsEditor;

    /** Related boundary editor */
    private BoundaryEditor boundaryEditor; // ??????????????

    /** Id of last long horizontal section */
    private int lastLongHSectionId = -1;

    /** Have systems their boundaries? */
    private boolean hasSystemBoundaries = false;

    /** Registered item renderers, if any */
    private Set<ItemRenderer> itemRenderers = new HashSet<>();

    /** Display of runs tables. */
    private RunsViewer runsViewer;

    /** Display of all spots. */
    private SpotsController spotsController;

    //~ Constructors -----------------------------------------------------------
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

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // getRunsViewer //
    //---------------//
    public RunsViewer getRunsViewer ()
    {
        return runsViewer;
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

    //----------------------//
    // createSystemsBuilder //
    //----------------------//
    public void createSystemsBuilder ()
    {
        page.resetSystems();
        systemsBuilder = new SystemsBuilder(this);
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

    //-----------------//
    // getActiveGlyphs //
    //-----------------//
    /**
     * Export the active glyphs of the vertical lag.
     *
     * @return the collection of glyphs for which at least a section is assigned
     */
    public Collection<Glyph> getActiveGlyphs ()
    {
        return getNest().getActiveGlyphs();
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

    //----------------//
    // getBarsChecker //
    //----------------//
    /**
     * @return the barsChecker
     */
    public BarsChecker getBarsChecker ()
    {
        return barsChecker;
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

    //-------------------//
    // getBoundaryEditor //
    //-------------------//
    /**
     * @return the boundaryEditor
     */
    public BoundaryEditor getBoundaryEditor ()
    {
        return boundaryEditor;
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

    //-----------------//
    // getErrorsEditor //
    //-----------------//
    public ErrorsEditor getErrorsEditor ()
    {
        return errorsEditor;
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
    public Nest getNest ()
    {
        return nest;
    }

    //------------//
    // createNest //
    //------------//
    public Nest createNest ()
    {
        // Beware: Glyph nest must subscribe to location before any lag,
        // to allow cleaning up of glyph data, before publication by a lag
        nest = new BasicNest("gNest", this);
        nest.setServices(locationService);

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
        return (List<Inter>) locationService
                .getSelection(InterListEvent.class);
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

    //-----------------//
    // getShapedGlyphs //
    //-----------------//
    /**
     * Report the collection of glyphs whose shape is identical to the provided
     * shape
     *
     * @param shape the imposed shape
     * @return the (perhaps empty) collection of active glyphs with right shape
     */
    public Collection<Glyph> getShapedGlyphs (Shape shape)
    {
        List<Glyph> found = new ArrayList<>();

        for (Glyph glyph : getActiveGlyphs()) {
            if (glyph.getShape() == shape) {
                found.add(glyph);
            }
        }

        return found;
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

    //-------------//
    // getSystemOf //
    //-------------//
    /**
     * Report the system info that contains the provided point
     *
     * @param point the provided pixel point
     * @return the containing system info
     *         (or null if there is no enclosing system)
     */
    public SystemInfo getSystemOf (Point point)
    {
        for (SystemInfo info : getSystems()) {
            SystemBoundary boundary = info.getBoundary();

            if ((boundary != null) && boundary.contains(point)) {
                return info;
            }
        }

        return null;
    }

    //-------------//
    // getSystemOf //
    //-------------//
    /**
     * Report the system, if any, which contains the provided glyph
     * (as determined by the first section of the glyph)
     *
     * @param glyph the provided glyph
     * @return the containing system, or null
     */
    public SystemInfo getSystemOf (Glyph glyph)
    {
        if (glyph.isVirtual() || glyph.getMembers().isEmpty()) {
            return getSystemOf(glyph.getAreaCenter());
        } else {
            SystemInfo system = glyph.getMembers().first().getSystem();
            if (system != null) {
                return system;
            } else {
                return getSystemOf(glyph.getAreaCenter());
            }
        }
    }

    //-------------//
    // getSystemOf //
    //-------------//
    /**
     * Report the system, if any, which contains the provided vertical section
     *
     * @param section the provided section
     * @return the containing system, or null
     */
    public SystemInfo getSystemOf (Section section)
    {
        return section.getSystem();
    }

    //-------------//
    // getSystemOf //
    //-------------//
    /**
     * Report the system that contains ALL glyphs provided.
     * If all glyphs do not belong to the same system, exception is thrown
     *
     * @param glyphs the collection of glyphs
     * @return the containing system
     * @throws IllegalArgumentException raised if glyphs collection is not OK
     */
    public SystemInfo getSystemOf (Collection<Glyph> glyphs)
    {
        if ((glyphs == null) || glyphs.isEmpty()) {
            throw new IllegalArgumentException(
                    "getSystemOf. Glyphs collection is null or empty");
        }

        SystemInfo system = null;
        Collection<Glyph> toRemove = new ArrayList<>();

        for (Glyph glyph : glyphs) {
            SystemInfo glyphSystem = glyph.isVirtual()
                    ? getSystemOf(glyph.getAreaCenter())
                    : getSystemOf(glyph);

            if (glyphSystem == null) {
                toRemove.add(glyph);
            } else {
                if (system == null) {
                    system = glyphSystem;
                } else {
                    // Make sure we are still in the same system
                    if (glyphSystem != system) {
                        throw new IllegalArgumentException(
                                "getSystemOf. Glyphs from different systems ("
                                + getSystemOf(glyph) + " and " + system + ") "
                                + Glyphs.toString(glyphs));
                    }
                }
            }
        }

        if (!toRemove.isEmpty()) {
            logger.warn("No system for {}", Glyphs.toString(toRemove));
            glyphs.removeAll(toRemove);
        }

        return system;
    }

    //---------------------//
    // getSystemOfSections //
    //---------------------//
    /**
     * Report the system that contains ALL sections provided.
     * If all sections do not belong to the same system, exception is thrown
     *
     * @param sections the collection of sections
     * @return the containing system
     * @throws IllegalArgumentException raised if section collection is not
     *                                  OK
     */
    public SystemInfo getSystemOfSections (Collection<Section> sections)
    {
        if ((sections == null) || sections.isEmpty()) {
            throw new IllegalArgumentException(
                    "getSystemOfSections. Sections collection is null or empty");
        }

        SystemInfo system = null;
        Collection<Section> toRemove = new ArrayList<>();

        for (Section section : sections) {
            SystemInfo sectionSystem = section.getSystem();

            if (sectionSystem == null) {
                toRemove.add(section);
            } else {
                if (system == null) {
                    system = sectionSystem;
                } else {
                    // Make sure we are still in the same system
                    if (sectionSystem != system) {
                        throw new IllegalArgumentException(
                                "getSystemOfSections. Sections from different systems ("
                                + section.getSystem() + " and " + system + ") "
                                + Sections.toString(sections));
                    }
                }
            }
        }

        if (!toRemove.isEmpty()) {
            logger.warn("No system for {}", Sections.toString(toRemove));
            sections.removeAll(toRemove);
        }

        return system;
    }

    //-------------------//
    // getSystemsBuilder //
    //-------------------//
    /**
     * Give access to the builder in charge of bars & systems computation
     *
     * @return the builder instance
     */
    public SystemsBuilder getSystemsBuilder ()
    {
        return systemsBuilder;
    }

    //----------------//
    // getSystemsNear //
    //----------------//
    /**
     * Report the ordered list of systems containing or close to the
     * provided point.
     *
     * @param point the provided point
     * @return a collection of systems ordered by increasing distance from the
     *         provided point
     */
    public List<SystemInfo> getSystemsNear (final Point point)
    {
        List<SystemInfo> neighbors = new ArrayList<>(systems);
        Collections.sort(
                neighbors,
                new Comparator<SystemInfo>()
                {
                    @Override
                    public int compare (SystemInfo s1,
                                        SystemInfo s2)
                    {
                        int y1 = (s1.getTop() + s1.getBottom()) / 2;
                        int d1 = Math.abs(point.y - y1);
                        int y2 = (s2.getTop() + s2.getBottom()) / 2;
                        int d2 = Math.abs(point.y - y2);

                        return Integer.signum(d1 - d2);
                    }
                });

        return neighbors;
    }

    //------------------//
    // getTargetBuilder //
    //------------------//
    /**
     * @return the targetBuilder
     */
    public TargetBuilder getTargetBuilder ()
    {
        return targetBuilder;
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

    //---------------------//
    // hasSystemBoundaries //
    //---------------------//
    /**
     * Report whether the systems have their boundaries defined yet.
     *
     * @return true if already defined
     */
    public boolean hasSystemBoundaries ()
    {
        return hasSystemBoundaries;
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
                logger.info("{}Removed page #{}",
                        page.getScore().getLogPrefix(), page.getIndex());
            } else {
                score.close();
            }
        }
    }

    //----------------//
    // setBarsChecker //
    //----------------//
    /**
     * @param barsChecker the barsChecker
     */
    public void setBarsChecker (BarsChecker barsChecker)
    {
        this.barsChecker = barsChecker;
    }

    //-------------------//
    // setBoundaryEditor //
    //-------------------//
    /**
     * @param boundaryEditor the boundaryEditor to set
     */
    public void setBoundaryEditor (BoundaryEditor boundaryEditor)
    {
        this.boundaryEditor = boundaryEditor;
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
            getBench().recordImageDimension(picture.getWidth(), picture.
                    getHeight());

            done(Steps.valueOf(Steps.LOAD));
        } catch (ImageFormatException ex) {
            String msg = "Unsupported image format in file "
                         + getScore().getImagePath() + "\n" + ex.getMessage();

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

    //---------------------//
    // setSystemBoundaries //
    //---------------------//
    /**
     * Set the flag about systems boundaries.
     */
    public void setSystemBoundaries ()
    {
        hasSystemBoundaries = true;
    }

    //------------------//
    // setTargetBuilder //
    //------------------//
    /**
     * @param targetBuilder the targetBuilder to set
     */
    public void setTargetBuilder (TargetBuilder targetBuilder)
    {
        this.targetBuilder = targetBuilder;
    }

    //----------------//
    // dispatchGlyphs //
    //----------------//
    /**
     * Dispatch the sheet glyphs among systems
     *
     * @param glyphs the collection of glyphs to dispatch among sheet systems.
     *               If null, the nest glyphs are used.
     */
    public void dispatchGlyphs (Collection<Glyph> glyphs)
    {
        if (glyphs == null) {
            glyphs = nest.getActiveGlyphs();
        }

        // Assign the glyphs to the proper system glyphs collection
        for (Glyph glyph : glyphs) {
            if (glyph.isActive()) {
                SystemInfo system = getSystemOf(glyph);

                if (system != null) {
                    system.addGlyph(glyph);
                } else {
                    glyph.setShape(null);
                }
            }
        }
    }

    //----------------------------//
    // dispatchHorizontalSections //
    //----------------------------//
    /**
     * Dispatch the various horizontal sections among systems
     */
    public void dispatchHorizontalSections ()
    {
        for (SystemInfo system : systems) {
            system.getMutableHorizontalSections().clear();
        }

        // Now dispatch the lag sections among the systems
        for (Section section : getLag(Lags.HLAG).getSections()) {
            SystemInfo system = getSystemOf(section.getCentroid());
            // Link section -> system
            section.setSystem(system);

            if (system != null) {
                // Link system <>-> section
                system.getMutableHorizontalSections().add(section);
            }
        }
    }

    //--------------------------------//
    // dispatchHorizontalHugeSections //
    //--------------------------------//
    /**
     * Dispatch the various horizontal huge sections among systems
     */
    public void dispatchHorizontalHugeSections ()
    {
        for (SystemInfo system : systems) {
            system.getMutableHorizontalFullSections().clear();
        }

        // Now dispatch the lag huge sections among the systems
        for (Section section : getLag(Lags.FULL_HLAG).getSections()) {
            SystemInfo system = getSystemOf(section.getCentroid());
            // Link section -> system
            section.setSystem(system);

            if (system != null) {
                // Link system <>-> section
                system.getMutableHorizontalFullSections().add(section);
            }
        }
    }

    //--------------------------//
    // dispatchVerticalSections //
    //--------------------------//
    /**
     * Dispatch the various vertical sections among systems
     */
    public void dispatchVerticalSections ()
    {
        // Take a snapshot of sections collection per system and clear it
        Map<SystemInfo, Collection<Section>> sections = new HashMap<>();
        for (SystemInfo system : systems) {
            Collection<Section> systemSections = system.
                    getMutableVerticalSections();
            sections.put(system, new ArrayList<>(systemSections));
            systemSections.clear();
        }

        // Now dispatch the lag sections among the systems
        for (Section section : getLag(Lags.VLAG).getSections()) {
            SystemInfo system = getSystemOf(section.getCentroid());
            // Link section -> system
            section.setSystem(system);

            if (system != null) {
                // Link system <>-> section
                system.getMutableVerticalSections().add(section);
            }
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Sheet " + page.getId() + "}";
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
            doneSteps = new HashSet<>();
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
            barsChecker = null;
            systemsBuilder = null;
            symbolsController = null;
            targetBuilder = null;
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
                    new BoardsPane(
                            new PixelBoard(this),
                            new BinarizationBoard(this)));
        }
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

    //--------------------//
    // getSpotsController //
    //--------------------//
    /**
     * @return the spotsController
     */
    public SpotsController getSpotsController ()
    {
        return spotsController;
    }

    //--------------------//
    // setSpotsController //
    //--------------------//
    /**
     * @param spotsController the spotsController to set
     */
    public void setSpotsController (SpotsController spotsController)
    {
        this.spotsController = spotsController;
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

        if (rect == null) {
            return;
        }

        SystemInfo system = getSystemOf(rect.getLocation());
        if (system == null) {
            return;
        }

        SIGraph sig = system.getSig();

        final List<Inter> inters;
        if ((rect.width > 0) && (rect.height > 0)) {
            // This is a non-degenerated rectangle
            // Look for contained interpretations
            inters = sig.containedInters(rect);
        } else {
            // This is just a point
            // Look for intersected interpretations
            inters = sig.containingInters(rect.getLocation());
        }

        // Publish inters found (perhaps none)
        locationService.publish(new InterListEvent(this, hint, movement, inters));
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
            if (inters != null && !inters.isEmpty()) {
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
        locationService.publish(new InterListEvent(this, hint, movement,
                inter != null ? Arrays.asList(inter) : null));

    }
}
