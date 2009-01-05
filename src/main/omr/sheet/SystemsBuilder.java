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
import omr.glyph.GlyphModel;
import omr.glyph.Shape;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphLagView;

import omr.lag.RunBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;
import omr.lag.VerticalOrientation;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.visitor.SheetPainter;

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

import org.bushe.swing.event.EventService;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;

/**
 * Class <code>SystemsBuilder</code> is in charge of retrieving the systems
 * (SystemInfo instances) and parts (PartInfo instances) in the provided sheet.
 *
 * <p>Is does so automatically by using barlines glyphs that embrace staves,
 * parts and systems.  It also allows the user to interactively modify the
 * retrieved information.</p>
 *
 * <p>Systems define their own area, which may be more complex than a simple
 * ordinate range, in order to precisely define which glyph belongs to which
 * system. The user has the ability to interactively modify the limits between
 * two adjacent systems.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SystemsBuilder
    extends GlyphModel
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
    private static final Collection<Class<?extends UserEvent>> eventClasses = new ArrayList<Class<?extends UserEvent>>();

    static {
        eventClasses.add(GlyphEvent.class);
    }

    //~ Instance fields --------------------------------------------------------

    /** Companion physical stick barsChecker */
    private final BarsChecker barsChecker;

    /** Lag view on bars, if so desired */
    private GlyphLagView lagView;

    /** Collection of vertical sticks */
    private List<Stick> verticalSticks = new ArrayList<Stick>();

    /** Collection of found bar sticks */
    private List<Stick> barSticks;

    /** Retrieved systems */
    private final List<SystemInfo> systems = new ArrayList<SystemInfo>();

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

        // BarsChecker companion
        barsChecker = new BarsChecker(sheet, lag, verticalSticks);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getBarSticks //
    //--------------//
    public List<Stick> getBarSticks ()
    {
        return barSticks;
    }

    //----------------//
    // getBarsChecker //
    //----------------//
    public BarsChecker getBarsChecker ()
    {
        return barsChecker;
    }

    //-------------//
    // getSystemOf //
    //-------------//
    /**
     * Report the SystemInfo that contains the given bar stick.
     *
     * @param stick the given bar stick
     * @return the containing SystemInfo, null if not found
     */
    public SystemInfo getSystemOf (Stick stick)
    {
        BarsChecker.StaffAnchors pair = barsChecker.getStaffAnchors(stick);

        if (pair == null) {
            return null;
        }

        int topIdx = pair.top;
        int botIdx = pair.bot;

        if (topIdx == -1) {
            topIdx = botIdx;
        }

        if (botIdx == -1) {
            botIdx = topIdx;
        }

        for (SystemInfo system : sheet.getSystems()) {
            if ((system.getStartIdx() <= botIdx) &&
                (system.getStopIdx() >= topIdx)) {
                return system;
            }
        }

        // Not found
        return null;
    }

    //------------------//
    // assignGlyphShape //
    //------------------//
    /**
     * Assign a (barline) Shape to a glyph, which means adding a barline to the
     * system/measure structure
     *
     * @param processing request to run (a)synchronously
     * @param glyph the glyph to be assigned
     * @param shape the assigned shape, which may be null
     * @param record specify whether the action must be recorded in the script
     */
    @Override
    public void assignGlyphShape (Synchronicity   processing,
                                  Glyph           glyph,
                                  Shape           shape,
                                  ScriptRecording record)
    {
        if (glyph != null) {
            //if (logger.isFineEnabled()) {
            logger.info(
                "Bars. shape " + shape + " assigned to glyph #" +
                glyph.getId());
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
        // Stuff to be made available
        sheet.getHorizontals();

        try {
            sheet.setVerticalLag(lag);

            // Retrieve true bar lines and thus SystemInfos
            barSticks = barsChecker.retrieveBarSticks();

            // Build systems and parts on sheet/glyph side
            buildSystemsAndParts();

            // Report number of systems retrieved
            reportResults();

            sheet.computeSystemBoundaries();
            updateSystemEntities();
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
            if ((glyph.getShape() == Shape.THICK_BAR_LINE) ||
                (glyph.getShape() == Shape.THIN_BAR_LINE)) {
                logger.info("Deassigning a " + glyph.getShape());

                Stick bar = (Stick) glyph;

                // Related stick has to be freed
                bar.setShape(null);
                bar.setResult(CANCELLED);

                // Remove from the internal bars list
                if (!barSticks.remove(bar)) {
                    return;
                }

                // Remove from the containing SystemInfo
                SystemInfo system = getSystemOf(bar);

                if (system == null) {
                    return;
                }

                assignGlyphShape(SYNC, glyph, null, NO_RECORDING);

                // Update the view accordingly
                if (lagView != null) {
                    lagView.colorize();
                    lagView.repaint();
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
            for (Glyph glyph : glyphs) {
                deassignGlyphShape(SYNC, glyph, NO_RECORDING);
            }
        }
    }

    //----------------------//
    // buildSystemsAndParts //
    //----------------------//
    /**
     * Knowing the starting staff indice of each staff system, we are able to
     * allocate and describe the proper number of systems & parts in the score.
     *
     * @param systemStarts indexed by any staff, to give the staff index of the
     *                     containing system. For a system with just one staff,
     *                     both indices are equal. For a system of more than 1
     *                     staff, the indices differ.
     * @param partStarts indexed by any staff, to give the staff index of the
     *                   containing part. For a part with just one staff, both
     *                   indices are equal. For a part of more than 1 staff, the
     *                   indices differ.
     * @throws StepException raised if processing failed
     */
    private void buildSystemsAndParts ()
        throws StepException
    {
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

        for (Stick stick : barSticks) {
            if (stick.getResult() == BarsChecker.BAR_PART_DEFINING) {
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
                throw new StepException();
            }
        }

        int        id = 0; // Id for created SystemInfo's
        int        sStart = -1; // Current system start
        SystemInfo system = null; // Current system info
        int        pStart = -1; // Current part start
        PartInfo   part = null; // Current part info

        for (int i = 0; i < systemStarts.length; i++) {
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

        // Finally, store this list into the sheet instance
        sheet.setSystems(systems);
    }

    //--------------//
    // displayFrame //
    //--------------//
    @SuppressWarnings("unchecked")
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

    //---------------//
    // reportResults //
    //---------------//
    private void reportResults ()
    {
        StringBuilder sb = new StringBuilder();
        int           nb = sheet.getSystems()
                                .size();

        if (nb > 0) {
            sb.append(nb)
              .append(" system");

            if (nb > 1) {
                sb.append("s");
            }
        } else {
            sb.append("no system found");
        }

        logger.info(sb.toString());
    }

    //----------------------//
    // updateSystemEntities //
    //----------------------//
    private void updateSystemEntities ()
    {
        // Split everything, including horizontals, per system
        sheet.splitHorizontals();
        sheet.splitVerticalSections();
        sheet.splitBarSticks(barSticks);
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
            false,
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
        }
    }

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends GlyphLagView
    {
        //~ Instance fields ----------------------------------------------------

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
            // Default lag view behavior, including specifics
            if (event.movement != MouseMovement.RELEASING) {
                super.onEvent(event);
            }

            if (event instanceof SheetLocationEvent) {
                // Update system boundary?
                SheetLocationEvent sheetLocation = (SheetLocationEvent) event;

                ///logger.info(sheetLocation.toString());
                if (sheetLocation.hint == SelectionHint.LOCATION_ADD) {
                    Rectangle rect = sheetLocation.rectangle;

                    if (rect != null) {
                        if (event.movement != MouseMovement.RELEASING) {
                            updateBoundary(
                                new Point(
                                    rect.x + (rect.width / 2),
                                    rect.y + (rect.height / 2)));
                        } else {
                            new BasicTask() {
                                    @Override
                                    protected Void doInBackground ()
                                        throws Exception
                                    {
                                        updateSystemEntities();

                                        // Update following steps if any
                                        logger.info(
                                            "updating steps, starting at " +
                                            Step.SYSTEMS.next());

                                        return null;
                                    }
                                }.execute();
                        }
                    }
                }
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
            sheet.accept(new SheetPainter(g, getZoom()));

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
