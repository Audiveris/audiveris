//----------------------------------------------------------------------------//
//                                                                            //
//                        S y s t e m s B u i l d e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.check.CheckBoard;
import omr.check.CheckSuite;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphsModel;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;
import omr.glyph.ui.BarMenu;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphLagView;
import omr.glyph.ui.GlyphsController;

import omr.lag.VerticalOrientation;
import omr.lag.ui.RunBoard;
import omr.lag.ui.ScrollLagView;
import omr.lag.ui.SectionBoard;

import omr.log.Logger;

import omr.score.MeasureFixer;

import omr.script.BoundaryTask;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import omr.selection.SelectionService;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.sheet.BarsChecker.BarCheckSuite;
import omr.sheet.SystemsBuilder.BarsController;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.SheetPainter;

import omr.step.Step;
import omr.step.StepException;
import omr.step.Steps;

import omr.stick.StickSection;

import omr.ui.BoardsPane;

import omr.util.BrokenLine;

import org.jdesktop.application.Task;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>SystemsBuilder</code> is in charge of retrieving the systems
 * (SystemInfo instances) and parts (PartInfo instances) in the provided sheet
 * and to allocate the corresponding instances on the Score side (the Score
 * instance, and the various instances of ScoreSystem, SystemPart and Staff).
 * The result is visible in the ScoreView.
 *
 * <p>Is does so automatically by using barlines glyphs that embrace staves,
 * parts and systems.  It also allows the user to interactively modify the
 * retrieved information.</p>
 *
 * <p>Systems define their own area, which may be more complex than a simple
 * ordinate range, in order to precisely define which glyph belongs to which
 * system. The user has the ability to interactively modify the broken line
 * that defines the limit between two adjacent systems.</p>
 *
 * <p>This class has close relationships with {@link MeasuresBuilder} in charge
 * of building and checking the measures, because barlines are used both to
 * define systems and parts, and to define measures.</p>
 *
 * <p>From the related view, the user has the ability to assign or to deassign
 * a barline glyph, with subsequent impact on the related measures.</p>
 *
 * <p>TODO: Implement a way for the user to tell whether a bar glyph is or not
 * a BAR_PART_DEFINING (i.e. if it is anchored on top and bottom).</p>
 *
 * @author Hervé Bitteur
 */
public class SystemsBuilder
    extends GlyphsModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SystemsBuilder.class);

    /** Events this entity is interested in */
    private static final Collection<Class<?extends UserEvent>> eventClasses;

    static {
        eventClasses = new ArrayList<Class<?extends UserEvent>>();
        eventClasses.add(GlyphEvent.class);
    }

    //~ Instance fields --------------------------------------------------------

    /** Companion physical stick barsChecker */
    private final BarsChecker barsChecker;

    /** Lag view on bars, if so desired */
    private GlyphLagView lagView;

    /** Glyphs controller */
    private BarsController barsController;

    /** Sheet retrieved systems */
    private final List<SystemInfo> systems;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // SystemsBuilder //
    //----------------//
    /**
     * Creates a new SystemsBuilder object.
     *
     * @param sheet the related sheet
     */
    public SystemsBuilder (Sheet sheet)
    {
        super(
            sheet,
            new GlyphLag("vLag", StickSection.class, new VerticalOrientation()),
            Steps.valueOf(Steps.SYSTEMS));

        systems = sheet.getSystems();

        // BarsChecker companion, in charge of purely physical tests
        barsChecker = new BarsChecker(sheet, lag);
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getController //
    //---------------//
    public BarsController getController ()
    {
        return (BarsController) lagView.getController();
    }

    //--------------//
    // buildSystems //
    //--------------//
    /**
     * Process the sheet information (the vertical lag) to retrieve bars and
     * then the systems.
     *
     * @throws StepException raised when step processing must stop, due to
     *                             encountered error
     */
    public void buildSystems ()
        throws StepException
    {
        try {
            // Remember old vLag if any, while setting the new one
            GlyphLag oldLag = sheet.setVerticalLag(lag);

            // Retrieve the initial collection of good barline candidates
            // Results are updated shapes in vLag glyphs
            barsChecker.retrieveCandidates();

            // Transfer manual glyphs from old lag if at all possible
            if (oldLag != null) {
                lag.transferManualGlyphs(oldLag);
            }

            // Processing
            doBuildSystems();
        } finally {
            // Display the resulting stickarea if so asked for
            if (constants.displayFrame.getValue() && (Main.getGui() != null)) {
                displayFrame();
            }
        }
    }

    //-------------------//
    // rebuildAllSystems //
    //-------------------//
    public void rebuildAllSystems ()
    {
        // Update the retrieved systems
        try {
            doBuildSystems();
        } catch (StepException ex) {
            logger.warning("Error rebuilding systems info", ex);
        }

        // Update the view accordingly
        if (lagView != null) {
            lagView.colorizeAllGlyphs();
            lagView.repaint();
        }
    }

    //---------------//
    // useBoundaries //
    //---------------//
    public void useBoundaries ()
    {
        // Split the entities (horizontals sections, vertical sections,
        // vertical sticks) to the system they belong to.
        splitSystemEntities();
    }

    //-------------------//
    // getSpecificGlyphs //
    //-------------------//
    private Collection<Glyph> getSpecificGlyphs ()
    {
        List<Glyph> specifics = new ArrayList<Glyph>();

        for (Glyph stick : lag.getAllGlyphs()) {
            if (!stick.isBar()) {
                specifics.add(stick);
            }
        }

        return specifics;
    }

    //------------------------//
    // allocateScoreStructure //
    //------------------------//
    /**
     * For each SystemInfo, build the corresponding System entity with all its
     * depending Parts and Staves
     */
    private void allocateScoreStructure ()
        throws StepException
    {
        // Clear Score -> Systems
        sheet.getPage()
             .resetSystems();

        for (SystemInfo system : systems) {
            system.allocateScoreStructure(); // ScoreSystem, Parts & Staves
        }
    }

    //----------------------//
    // buildSystemsAndParts //
    //----------------------//
    /**
     * Knowing the starting staff indice of each staff system, we are able to
     * allocate and describe the proper number of systems & parts in the score.
     *
     * @throws StepException raised if processing failed
     */
    private void buildSystemsAndParts ()
        throws StepException
    {
        systems.clear();

        final int staffNb = sheet.getStaves()
                                 .size();

        // A way to tell the containing System for each staff, by providing the
        // staff index of the starting staff of the containing system.
        int[] systemStarts = new int[staffNb];
        Arrays.fill(systemStarts, -1);

        // A way to tell the containing Part for each staff, by providing the
        // staff index of the starting staff of the containing part.
        int[] partStarts = new int[staffNb];
        Arrays.fill(partStarts, -1);

        // We need an abscissa-ordered collection of glyphs, so that system
        // defining bars are seen first
        List<Glyph> glyphs = new ArrayList<Glyph>(lag.getAllGlyphs());
        Collections.sort(glyphs, Glyph.globalComparator);

        for (Glyph glyph : glyphs) {
            Stick stick = (Stick) glyph;

            if (stick.isBar() &&
                ((stick.getResult() == BarsChecker.BAR_PART_DEFINING) ||
                (stick.getShape() == Shape.PART_DEFINING_BARLINE))) {
                BarsChecker.StaffAnchors pair = barsChecker.getStaffAnchors(
                    stick);

                for (int i = pair.top; i <= pair.bot; i++) {
                    if (systemStarts[i] == -1) {
                        systemStarts[i] = pair.top;
                    }

                    partStarts[i] = pair.top;
                }
            }
        }

        // Sanity check on the systems found
        for (int i = 0; i < systemStarts.length; i++) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "staff=" + i + " systemStart=" + systemStarts[i] +
                    " partStart=" + partStarts[i]);
            }

            if (systemStarts[i] == -1) {
                logger.warning("No system found for staff " + i);
            }
        }

        int        id = 0; // Id for created SystemInfo's
        int        sStart = -1; // Current system start
        SystemInfo system = null; // Current system info
        int        pStart = -1; // Current part start
        PartInfo   part = null; // Current part info

        for (int i = 0; i < systemStarts.length; i++) {
            // Skip staves with no system
            if (systemStarts[i] == -1) {
                continue;
            }

            // System break ?
            if (systemStarts[i] != sStart) {
                system = new SystemInfo(++id, sheet);
                systems.add(system);
                sStart = i;
            }

            system.addStaff(i);

            // Part break ?
            if (partStarts[i] != pStart) {
                part = new PartInfo();
                system.addPart(part);
                pStart = i;
            }

            part.addStaff(sheet.getStaves().get(i));
        }

        // Specific degraded case, just one staff, no bar stick
        if (systems.isEmpty() && (staffNb == 1)) {
            system = new SystemInfo(++id, sheet);
            systems.add(system);
            system.addStaff(0);
            part = new PartInfo();
            system.addPart(part);
            part.addStaff(sheet.getStaves().get(0));
            logger.warning("Created one system, one part, one staff");
        }

        if (logger.isFineEnabled()) {
            for (SystemInfo systemInfo : systems) {
                Main.dumping.dump(systemInfo);

                int i = 0;

                for (PartInfo partInfo : systemInfo.getParts()) {
                    Main.dumping.dump(partInfo, "Part #" + ++i, 1);
                }
            }
        }
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        barsController = new BarsController();
        lagView = new MyView(lag, getSpecificGlyphs(), barsController);
        lagView.colorizeAllGlyphs();

        final String  unit = sheet.getId() + ":BarsBuilder";
        BoardsPane    boardsPane = new BoardsPane(
            sheet,
            lagView,
            new PixelBoard(unit, sheet),
            new RunBoard(unit, lag),
            new SectionBoard(unit, lag.getLastVertexId(), lag),
            new GlyphBoard(unit, lagView.getController(), lag.getAllGlyphs()),
            new MyCheckBoard(
                unit,
                barsChecker.getSuite(),
                lag.getSelectionService(),
                eventClasses));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(lagView);
        sheet.getAssembly()
             .addViewTab(Step.SYSTEMS_TAB, slv, boardsPane);
    }

    //----------------//
    // doBuildSystems //
    //----------------//
    private void doBuildSystems ()
        throws StepException
    {
        // Build systems and parts on sheet/glyph side
        buildSystemsAndParts();

        // Create score counterparts
        allocateScoreStructure();

        // Report number of systems retrieved
        reportResults();

        // Define precisely the systems boundaries
        sheet.computeSystemBoundaries();

        useBoundaries();
    }

    //---------------//
    // reportResults //
    //---------------//
    private void reportResults ()
    {
        StringBuilder sb = new StringBuilder();
        int           partNb = 0;

        for (SystemInfo system : sheet.getSystems()) {
            partNb = Math.max(partNb, system.getParts().size());
        }

        int sysNb = systems.size();

        if (partNb > 0) {
            sb.append(partNb)
              .append(" part");

            if (partNb > 1) {
                sb.append("s");
            }
        } else {
            sb.append("no part found");
        }

        sheet.getBench()
             .recordPartCount(partNb);

        if (sysNb > 0) {
            sb.append(", ")
              .append(sysNb)
              .append(" system");

            if (sysNb > 1) {
                sb.append("s");
            }
        } else {
            sb.append("no system found");
        }

        sheet.getBench()
             .recordSystemCount(sysNb);

        logger.info(sb.toString());
    }

    //---------------------//
    // splitSystemEntities //
    //---------------------//
    /**
     * Split horizontals, vertical sections, glyphs per system
     * @return the set of modified systems
     */
    private SortedSet<SystemInfo> splitSystemEntities ()
    {
        // Split everything, including horizontals, per system
        SortedSet<SystemInfo> modified = new TreeSet<SystemInfo>();
        modified.addAll(sheet.splitHorizontals());
        modified.addAll(sheet.splitVerticalSections());
        modified.addAll(sheet.splitBarSticks(lag.getAllGlyphs()));

        if (!modified.isEmpty()) {
            logger.info("Systems impact: " + modified);
        }

        return modified;
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------------//
    // BarsController //
    //----------------//
    /**
     * A glyphs controller specifically meant for barlines
     */
    public class BarsController
        extends GlyphsController
    {
        //~ Constructors -------------------------------------------------------

        public BarsController ()
        {
            super(SystemsBuilder.this);
        }

        //~ Methods ------------------------------------------------------------

        public Task asyncModifyBoundaries (final BrokenLine      brokenLine,
                                           final Set<SystemInfo> modifiedSystems)
        {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "asyncModifyBoundaries " + brokenLine +
                    " modifiedSystems:" + modifiedSystems);
            }

            if (brokenLine != null) {
                // Retrieve containing system
                for (SystemInfo system : sheet.getSystems()) {
                    SystemBoundary boundary = system.getBoundary();

                    for (SystemBoundary.Side side : SystemBoundary.Side.values()) {
                        if (boundary.getLimit(side) == brokenLine) {
                            return new BoundaryTask(system, side, brokenLine).launch(
                                sheet);
                        }
                    }
                }
            }

            return null;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Should we display a frame on the vertical sticks */
        Constant.Boolean displayFrame = new Constant.Boolean(
            true,
            "Should we display a frame on the vertical sticks");

        /** Windox enlarging ratio when dragging a boundary reference point */
        Constant.Ratio draggingRatio = new Constant.Ratio(
            5.0,
            "Windox enlarging ratio when dragging a boundary reference point");

        /** Maximum cotangent for checking a barline candidate */
        Constant.Double maxCoTangentForCheck = new Constant.Double(
            "cotangent",
            0.1,
            "Maximum cotangent for checking a barline candidate");
    }

    //--------------//
    // MyCheckBoard //
    //--------------//
    /**
     * A specific board dedicated to physical checks of bar sticks
     */
    private class MyCheckBoard
        extends CheckBoard<BarsChecker.GlyphContext>
    {
        //~ Constructors -------------------------------------------------------

        public MyCheckBoard (String                                unit,
                             CheckSuite<BarsChecker.GlyphContext>  suite,
                             SelectionService                      eventService,
                             Collection<Class<?extends UserEvent>> eventList)
        {
            super(unit, suite, eventService, eventList);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                if (event instanceof GlyphEvent) {
                    BarsChecker.GlyphContext context = null;
                    GlyphEvent               glyphEvent = (GlyphEvent) event;
                    Glyph                    entity = glyphEvent.getData();

                    if (entity instanceof Stick) {
                        Stick stick = (Stick) entity;

                        // Make sure this is a rather vertical stick
                        if (Math.abs(stick.getLine().getSlope()) <= constants.maxCoTangentForCheck.getValue()) {
                            // To get a fresh suite
                            setSuite(barsChecker.new BarCheckSuite());
                            context = new BarsChecker.GlyphContext(stick);
                        }
                    }

                    tellObject(context);
                }
            } catch (Exception ex) {
                logger.warning(getClass().getName() + " onEvent error", ex);
            }
        }
    }

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends GlyphLagView
    {
        //~ Instance fields ----------------------------------------------------

        /** Popup menu related to glyph selection */
        private BarMenu barMenu;

        /** Acceptable distance since last reference point (while dragging) */
        private int maxDraggingDelta = (int) Math.rint(
            constants.draggingRatio.getValue() * BrokenLine.getDefaultStickyDistance());

        // Latest designated reference point, if any */
        private Point      lastPoint = null;

        // Latest information, meaningful only if lastPoint is not null */
        private BrokenLine lastLine = null;

        //~ Constructors -------------------------------------------------------

        private MyView (GlyphLag                   lag,
                        Collection<?extends Glyph> specificGlyphs,
                        BarsController             barsController)
        {
            super(lag, null, null, barsController, specificGlyphs);
            setName("SystemsBuilder-View");
            barMenu = new BarMenu(getController());
        }

        //~ Methods ------------------------------------------------------------

        //----------//
        // colorize //
        //----------//
        @Override
        public void colorizeAllGlyphs ()
        {
            int viewIndex = lag.viewIndexOf(this);

            // Non recognized bar lines
            for (Glyph glyph : lag.getAllGlyphs()) {
                Stick stick = (Stick) glyph;

                if (!stick.isBar()) {
                    stick.colorize(lag, viewIndex, Color.red);
                }
            }

            // Recognized bar lines
            for (Glyph glyph : lag.getAllGlyphs()) {
                Stick stick = (Stick) glyph;

                if (stick.isBar()) {
                    stick.colorize(lag, viewIndex, Color.yellow);
                }
            }
        }

        //-----------------//
        // contextSelected //
        //-----------------//
        @Override
        public void contextSelected (Point         pt,
                                     MouseMovement movement)
        {
            // Retrieve the selected glyphs
            Set<Glyph> glyphs = getLag()
                                    .getSelectedGlyphSet();

            // To display point information
            if ((glyphs == null) || glyphs.isEmpty()) {
                pointSelected(pt, movement); // This may change glyph selection
                glyphs = getLag()
                             .getSelectedGlyphSet();
            }

            if ((glyphs != null) && !glyphs.isEmpty()) {
                // Update the popup menu according to selected glyphs
                barMenu.updateMenu();

                // Show the popup menu
                barMenu.getPopup()
                       .show(
                    this,
                    getZoom().scaled(pt.x),
                    getZoom().scaled(pt.y));
            } else {
                // Popup with no glyph selected ?
            }
        }

        //---------//
        // onEvent //
        //---------//
        /**
         * Notification about selection objects
         *
         * @param event the notified event
         */
        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Default lag view behavior, including specifics
                if (event.movement != MouseMovement.RELEASING) {
                    super.onEvent(event);
                }

                if (event instanceof SheetLocationEvent) {
                    // Update system boundary?
                    SheetLocationEvent sheetLocation = (SheetLocationEvent) event;

                    if (sheetLocation.hint == SelectionHint.LOCATION_INIT) {
                        Rectangle rect = sheetLocation.rectangle;

                        if (rect != null) {
                            if (event.movement != MouseMovement.RELEASING) {
                                // While user is dragging, simply modify the line
                                updateBoundary(
                                    new Point(
                                        rect.x + (rect.width / 2),
                                        rect.y + (rect.height / 2)));
                            } else if (lastLine != null) {
                                // User has released the mouse with a known line

                                // Perform boundary modifs synchronously
                                Set<SystemInfo> modifs = splitSystemEntities();

                                // If modifs, launch updates asynchronously
                                ///if (!modifs.isEmpty()) {
                                barsController.asyncModifyBoundaries(
                                    lastLine,
                                    modifs);
                                lastPoint = null;
                                lastLine = null;

                                ///}
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logger.warning(getClass().getName() + " onEvent error", ex);
            }
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        public void renderItems (Graphics2D g)
        {
            // Render all physical info known so far, which is just the staff
            // line info, lineset by lineset
            sheet.getPage()
                 .accept(new SheetPainter(g, true));

            super.renderItems(g);
        }

        //----------------//
        // updateBoundary //
        //----------------//
        /**
         * Try to update system boundary if any
         * @param pt Current location of user mouse
         */
        private void updateBoundary (Point pt)
        {
            Point refPoint = null; // New ref point

            // Are we close to the latest refPoint?
            if (lastPoint != null) {
                Rectangle rect = new Rectangle(lastPoint);
                rect.grow(maxDraggingDelta, maxDraggingDelta);

                if (rect.contains(pt)) {
                    refPoint = lastPoint;
                }
            }

            if (refPoint == null) {
                // Are we close to any existing refPoint?
                SystemInfo system = sheet.getSystemsNear(pt)
                                         .iterator()
                                         .next();

                for (BrokenLine line : system.getBoundary()
                                             .getLimits()) {
                    refPoint = line.findPoint(pt);

                    if (refPoint != null) {
                        lastLine = line;

                        break;
                    }
                }
            }

            if (refPoint != null) {
                // Move the current ref point to user pt
                lastLine.movePoint(refPoint, pt);

                // If we get colinear segments, let's merge them
                if (lastLine.isColinear(refPoint)) {
                    lastLine.removePoint(refPoint);
                    refPoint = null;
                }
            } else {
                // Are we close to a segment, to define a new ref point?
                SystemInfo system = sheet.getSystemsNear(pt)
                                         .iterator()
                                         .next();

                for (BrokenLine line : system.getBoundary()
                                             .getLimits()) {
                    Point segmentStart = line.findSegment(pt);

                    if (segmentStart != null) {
                        // Add a new ref point
                        refPoint = pt;
                        lastLine = line;
                        line.insertPointAfter(pt, segmentStart);

                        break;
                    }
                }
            }

            lastPoint = refPoint;
        }
    }
}
