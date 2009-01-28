//----------------------------------------------------------------------------//
//                                                                            //
//                        S y s t e m s B u i l d e r                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.Main;

import omr.check.CheckBoard;
import omr.check.CheckSuite;
import omr.check.FailureResult;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphsModel;
import omr.glyph.Shape;
import omr.glyph.ui.BarMenu;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphLagView;

import omr.lag.RunBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;
import omr.lag.VerticalOrientation;

import omr.log.Logger;

import omr.score.entity.ScorePart;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;
import omr.score.ui.ScoreView;
import omr.score.visitor.ScoreFixer;
import omr.score.visitor.SheetPainter;

import omr.script.AssignTask;
import omr.script.ScriptRecording;
import static omr.script.ScriptRecording.*;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.sheet.ui.PixelBoard;

import omr.step.Step;
import omr.step.StepException;

import omr.stick.Stick;
import omr.stick.StickSection;

import omr.ui.BoardsPane;

import omr.util.BasicTask;
import omr.util.BrokenLine;
import omr.util.Dumper;
import omr.util.Synchronicity;
import static omr.util.Synchronicity.*;
import omr.util.TreeNode;

import org.bushe.swing.event.EventService;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;

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
 * <p>This class has close relationships with {@link MeasuresModel} in charge
 * of building and checking the measures, because barlines are used both to
 * define systems and parts, and to define measures.</p>
 *
 * <p>From the related view, the user has the ability to assign or to deassign
 * a barline glyph, with subsequent impact on the related measures.</p>
 *
 * <p>TODO: Implement a way for the user to tell whether a bar glyph is or not
 * a BAR_PART_DEFINING (i.e. if it is anchored on top and bottom).</p>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SystemsBuilder
    extends GlyphsModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SystemsBuilder.class);

    /** Failure */
    private static final FailureResult CANCELLED = new FailureResult(
        "Bar-Cancelled");

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

    /** Collection of vertical sticks */
    private List<Stick> verticalSticks = new ArrayList<Stick>();

    /** Sorted set of found bar sticks */
    private SortedSet<Stick> barSticks;

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
            new GlyphLag("vLag", StickSection.class, new VerticalOrientation()));

        systems = sheet.getSystems();

        // BarsChecker companion
        barsChecker = new BarsChecker(sheet, lag, verticalSticks);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getBarSticks //
    //--------------//
    public Set<Stick> getBarSticks ()
    {
        return barSticks;
    }

    //------------------//
    // assignGlyphShape //
    //------------------//
    /**
     * Assign a Shape to a glyph
     *
     * @param processing specify whether we should run (a)synchronously
     * @param glyph the glyph to be assigned
     * @param shape the assigned shape, which may be null
     * @param record specify whether the action must be recorded in the script
     */
    @Override
    public void assignGlyphShape (Synchronicity         processing,
                                  final Glyph           glyph,
                                  final Shape           shape,
                                  final ScriptRecording record)
    {
        logger.info("assignGlyphShape #" + glyph.getId() + " to " + shape);
        super.assignGlyphShape(processing, glyph, shape, record);

        // Move from the internal bars list to the unlucky verticals
        if (shape == null) {
            verticalSticks.add((Stick) glyph);
            barSticks.remove(glyph);
        } else {
            verticalSticks.remove(glyph);
            barSticks.add((Stick) glyph);
        }

        // Update the view accordingly
        if (lagView != null) {
            lagView.colorize();
            lagView.repaint();
        }

        // Record this task to the sheet script?
        if (record == RECORDING) {
            sheet.getScript()
                 .addTask(new AssignTask(shape, false, Arrays.asList(glyph)));
            sheet.rebuildFrom(Step.MEASURES, null, null);
        }
    }

    //----------------//
    // assignSetShape //
    //----------------//
    /**
     * Assign a shape to the selected collection of glyphs.
     *
     * @param processing specify whether we should run (a)synchronously
     * @param glyphs the collection of glyphs to be assigned
     * @param shape the shape to be assigned
     * @param compound flag to build one compound, rather than assign each
     *                 individual glyph
     * @param record specify whether the action must be recorded in the script
     */
    @Override
    public void assignSetShape (Synchronicity           processing,
                                final Collection<Glyph> glyphs,
                                final Shape             shape,
                                final boolean           compound,
                                final ScriptRecording   record)
    {
        if ((glyphs != null) && (glyphs.size() > 0)) {
            if (processing == ASYNC) {
                new BasicTask() {
                        @Override
                        protected Void doInBackground ()
                            throws Exception
                        {
                            assignSetShape(
                                SYNC,
                                glyphs,
                                shape,
                                compound,
                                record);

                            return null;
                        }
                    }.execute();
            } else {
                logger.info(
                    "assignSetShape " + Glyph.toString(glyphs) + " to " +
                    shape);

                if (compound) {
                    // Build & insert a compound
                    SystemInfo system = sheet.getSystemOf(glyphs);
                    Glyph      glyph = system.buildCompound(glyphs);
                    system.addGlyph(glyph);
                    assignGlyphShape(SYNC, glyph, shape, NO_RECORDING);
                } else {
                    int              noiseNb = 0;
                    ArrayList<Glyph> glyphsCopy = new ArrayList<Glyph>(glyphs);

                    for (Glyph glyph : glyphsCopy) {
                        if (glyph.getShape() != Shape.NOISE) {
                            assignGlyphShape(SYNC, glyph, shape, NO_RECORDING);
                        } else {
                            noiseNb++;
                        }
                    }

                    if (logger.isFineEnabled() && (noiseNb > 0)) {
                        logger.fine(noiseNb + " noise glyphs skipped");
                    }
                }

                // Record this task to the sheet script?
                if (record == RECORDING) {
                    sheet.getScript()
                         .addTask(new AssignTask(shape, compound, glyphs));

                    try {
                        rebuildInfo();
                    } catch (StepException ex) {
                        logger.warning("Error rebuilding systems info", ex);
                    }

                    sheet.rebuildFrom(Step.MEASURES, glyphs, null);
                }
            }
        }
    }

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Process the sheet information (the vertical lag) to retrieve bars and
     * then the systems.
     *
     * @throws StepException raised when step processing must stop, due to
     *                             encountered error
     */
    public void buildInfo ()
        throws StepException
    {
        try {
            sheet.setVerticalLag(lag);
            sheet.createScore();

            // Retrieve true bar lines and thus SystemInfos
            barSticks = barsChecker.retrieveBarSticks();

            rebuildInfo();
        } finally {
            // Display the resulting stickarea if so asked for
            if (constants.displayFrame.getValue() && (Main.getGui() != null)) {
                displayFrame();
            }
        }
    }

    //--------------------//
    // deassignGlyphShape //
    //--------------------//
    /**
     * Remove a bar together with all its related entities. This means removing
     * reference in the bars list of this builder, reference in the containing
     * SystemInfo. The related stick must also be assigned a failure result.
     *
     * @param processing specify whether the method should run (a)synchronously
     * @param glyph the (false) bar glyph to deassign
     * @param record specify whether the action must be recorded in the script
     */
    @Override
    public void deassignGlyphShape (Synchronicity         processing,
                                    final Glyph           glyph,
                                    final ScriptRecording record)
    {
        if (processing == ASYNC) {
            new BasicTask() {
                    @Override
                    protected Void doInBackground ()
                        throws Exception
                    {
                        deassignGlyphShape(SYNC, glyph, record);

                        return null;
                    }
                }.execute();
        } else {
            if (glyph.isBar()) {
                logger.info(
                    "deassignGlyphShape #" + glyph.getId() + " was " +
                    glyph.getShape());

                Stick bar = (Stick) glyph;

                // Related stick has to be freed
                /////////////////////////////////////////////////:::bar.setResult(CANCELLED);

                // Move from the internal bars list to the unlucky verticals
                verticalSticks.add(bar);
                barSticks.remove(bar);

                assignGlyphShape(SYNC, glyph, null, NO_RECORDING);

                // Record?
                if (record == RECORDING) {
                    // TBD: Need to record deassign in the script ???
                    ///////////////////////////////////////////////////////////////////////

                    // Update following steps
                    sheet.rebuildFrom(Step.MEASURES, null, null);
                }
            } else {
                logger.warning(
                    "No deassign meant for " + glyph.getShape() + " glyph");
            }
        }
    }

    //------------------//
    // deassignSetShape //
    //------------------//
    /**
     * Remove a set of bars
     *
     * @param processing specify whether the method must run (a)synchronously
     * @param glyphs the collection of glyphs to be de-assigned
     * @param record specify whether the action must be recorded in the script
     */
    @Override
    public void deassignSetShape (Synchronicity           processing,
                                  final Collection<Glyph> glyphs,
                                  final ScriptRecording   record)
    {
        if (processing == ASYNC) {
            new BasicTask() {
                    @Override
                    protected Void doInBackground ()
                        throws Exception
                    {
                        deassignSetShape(SYNC, glyphs, record);

                        return null;
                    }
                }.execute();
        } else {
            logger.info("deassignSetShape " + Glyph.toString(glyphs));

            // Use a copy of glyphs collection
            for (Glyph glyph : new ArrayList<Glyph>(glyphs)) {
                deassignGlyphShape(SYNC, glyph, NO_RECORDING);
            }

            if (record == RECORDING) {
                try {
                    rebuildInfo();
                } catch (StepException ex) {
                    logger.warning("Error rebuilding systems info", ex);
                }

                // TBD: Need to record deassign in the script ???
                ///////////////////////////////////////////////////////////////////////////////////////

                // Update following steps
                sheet.rebuildFrom(Step.MEASURES, null, null);
            }
        }
    }

    //------------------------//
    // allocateScoreStructure //
    //------------------------//
    /**
     * For each SystemInfo, build the corresponding System entity with all its
     * depending Parts and Staves
     */
    private void allocateScoreStructure (Collection<SystemInfo> systems)
        throws StepException
    {
        // Clear Score -> Systems
        sheet.getScore()
             .getSystems()
             .clear();

        // Systems to (re)allocate
        Collection<SystemInfo> systemsToAllocate = (systems != null) ? systems
                                                   : this.systems;

        for (SystemInfo system : systemsToAllocate) {
            system.allocateScoreStructure();
        }

        // Define score parts
        defineScoreParts();
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

        for (Stick bar : barSticks) {
            bar.setShape(
                barsChecker.isThickBar(bar) ? Shape.THICK_BAR_LINE
                                : Shape.THIN_BAR_LINE);

            if (bar.getResult() == BarsChecker.BAR_PART_DEFINING) {
                BarsChecker.StaffAnchors pair = barsChecker.getStaffAnchors(
                    bar);

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

        if (logger.isFineEnabled()) {
            for (SystemInfo systemInfo : systems) {
                Dumper.dump(systemInfo);

                int i = 0;

                for (PartInfo partInfo : systemInfo.getParts()) {
                    Dumper.dump(partInfo, "Part #" + ++i, 1);
                }
            }
        }
    }

    //-----------------//
    // chooseRefSystem //
    //-----------------//
    /**
     * Look for the first largest system (according to its number of parts)
     * @return the largest system
     * @throws omr.step.StepException
     */
    private SystemInfo chooseRefSystem ()
        throws StepException
    {
        int        NbOfParts = 0;
        SystemInfo refSystem = null;

        for (SystemInfo systemInfo : systems) {
            int nb = systemInfo.getScoreSystem()
                               .getParts()
                               .size();

            if (nb > NbOfParts) {
                NbOfParts = nb;
                refSystem = systemInfo;
            }
        }

        if (refSystem == null) {
            throw new StepException("No system found");
        }

        return refSystem;
    }

    //------------------//
    // defineScoreParts //
    //------------------//
    /**
     * From system part, define the score parts
     * @throws StepException
     */
    private void defineScoreParts ()
        throws StepException
    {
        // Take the best representative system
        ScoreSystem refSystem = chooseRefSystem()
                                    .getScoreSystem();

        // Build the ScorePart list based on the parts of the ref system
        sheet.getScore()
             .createPartListFrom(refSystem);

        // Now examine each system as compared with the ref system
        // We browse through the parts "bottom up"
        List<ScorePart> partList = sheet.getScore()
                                        .getPartList();
        final int       nbScoreParts = partList.size();

        for (SystemInfo systemInfo : systems) {
            ScoreSystem    system = systemInfo.getScoreSystem();
            List<TreeNode> systemParts = system.getParts();
            final int      nbp = systemParts.size();

            for (int ip = 0; ip < nbp; ip++) {
                ScorePart  global = partList.get(nbScoreParts - 1 - ip);
                SystemPart sp = (SystemPart) systemParts.get(nbp - 1 - ip);
                sp.setScorePart(global);
                sp.setId(global.getId());
            }
        }
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        lagView = new MyView(lag);
        lagView.colorize();

        final String  unit = sheet.getRadix() + ":BarsBuilder";
        BoardsPane    boardsPane = new BoardsPane(
            sheet,
            lagView,
            new PixelBoard(unit, sheet),
            new RunBoard(unit, lag),
            new SectionBoard(unit, lag.getLastVertexId(), lag),
            new GlyphBoard(unit, this, verticalSticks),
            new MyCheckBoard(
                unit,
                barsChecker.getSuite(),
                lag.getEventService(),
                eventClasses));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(lagView);
        sheet.getAssembly()
             .addViewTab("Systems", slv, boardsPane);
    }

    //-------------//
    // rebuildInfo //
    //-------------//
    private void rebuildInfo ()
        throws StepException
    {
        // Build systems and parts on sheet/glyph side
        buildSystemsAndParts();

        // Create score counterparts
        allocateScoreStructure(null);

        // Report number of systems retrieved
        reportResults();

        // Define precisely the systems boundaries
        sheet.computeSystemBoundaries();

        // Finally split the entities (horizontals sections, vertical
        // sections, bar sticks) to the system they belong to
        splitSystemEntities();

        // Update score internal data
        sheet.getScore()
             .accept(new ScoreFixer(true));

        // Update score view if any
        ScoreView scoreView = sheet.getScore()
                                   .getView();

        if (scoreView != null) {
            scoreView.computeModelSize();
            scoreView.repaint();
        }
    }

    //---------------//
    // reportResults //
    //---------------//
    private void reportResults ()
    {
        StringBuilder sb = new StringBuilder();
        int           partNb = sheet.getScore()
                                    .getPartList()
                                    .size();
        int           sysNb = systems.size();

        if (partNb > 0) {
            sb.append(partNb)
              .append(" part");

            if (partNb > 1) {
                sb.append("s");
            }
        } else {
            sb.append("no part found");
        }

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
        modified.addAll(sheet.splitBarSticks(barSticks));

        if (modified.size() > 0) {
            logger.info("Systems impact: " + modified);
        }

        return modified;
    }

    //~ Inner Classes ----------------------------------------------------------

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
                             EventService                          eventService,
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
                        // To get a fresh suite
                        setSuite(barsChecker.new BarCheckSuite());

                        Stick stick = (Stick) entity;
                        context = new BarsChecker.GlyphContext(stick);
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

        private MyView (GlyphLag lag)
        {
            super(lag, null, null, SystemsBuilder.this, verticalSticks);
            setName("SystemsBuilder-View");
            barMenu = new BarMenu(sheet, SystemsBuilder.this, lag);
        }

        //~ Methods ------------------------------------------------------------

        //----------//
        // colorize //
        //----------//
        @Override
        public void colorize ()
        {
            super.colorize();

            // All remaining vertical sticks clutter
            for (Stick stick : verticalSticks) {
                stick.colorize(lag, viewIndex, Color.red);
            }

            // Recognized bar lines
            for (Stick stick : barSticks) {
                stick.colorize(lag, viewIndex, Color.yellow);
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
            Set<Glyph> glyphs = sheet.getVerticalLag()
                                     .getCurrentGlyphSet();

            // To display point information
            if ((glyphs == null) || (glyphs.size() == 0)) {
                pointSelected(pt, movement); // This may change glyph selection
                glyphs = sheet.getVerticalLag()
                              .getCurrentGlyphSet();
            }

            if ((glyphs != null) && (glyphs.size() > 0)) {
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

                    ///logger.info(sheetLocation.toString());
                    if (sheetLocation.hint == SelectionHint.LOCATION_INIT) {
                        Rectangle rect = sheetLocation.rectangle;

                        if (rect != null) {
                            if (event.movement != MouseMovement.RELEASING) {
                                updateBoundary(
                                    new Point(
                                        rect.x + (rect.width / 2),
                                        rect.y + (rect.height / 2)));
                            } else if (lastPoint != null) {
                                new BasicTask() {
                                        @Override
                                        protected Void doInBackground ()
                                            throws Exception
                                        {
                                            Set<SystemInfo> modifs = splitSystemEntities();

                                            // Update following steps if any
                                            if (modifs.size() > 0) {
                                                logger.info(
                                                    "TBD: updating steps, starting at " +
                                                    Step.SYSTEMS.next());
                                            }

                                            return null;
                                        }
                                    }.execute();
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
        public void renderItems (Graphics g)
        {
            // Render all physical info known so far, which is just the staff
            // line info, lineset by lineset
            sheet.accept(new SheetPainter(g));

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
                if ((Math.abs(lastPoint.x - pt.x) <= maxDraggingDelta) &&
                    (Math.abs(lastPoint.y - pt.y) <= maxDraggingDelta)) {
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
