//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S t a f f E d i t o r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Lags;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.NaturalSpline;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffLine;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.util.BasicIndex;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.TOP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class <code>StaffEditor</code> allows the end-user to manually edit the geometry of a staff.
 * <p>
 * There are two edition modes:
 * <ul>
 * <li><b>Lines</b> mode.
 * In this mode, all staff lines can be modified individually.
 * Any staff line defining point becomes a handle that can be moved vertically only.
 * <li><b>Global</b> mode.
 * In this mode, the staff lines can only be modified as a whole, meaning that the staff lines stay
 * parallel with each other.
 * The midLine defining points become handles that can be moved vertically, and the side defining
 * points can be moved both vertically and horizontally, allowing to extend or shrink the staff.
 * </ul>
 * In both modes, as staff geometry is being modified, remaining horizontal sections can get removed
 * (or reinserted) if they are detected as new staff line members.
 * <p>
 * TODO: Similarly, we could envision that remaining vertical sections get removed (or reinserted)
 * if they are detected as new barline members.
 * For the time being, manual barline insertion is a separate (but easy) task for the end-user.
 *
 * @author Hervé Bitteur
 */
public abstract class StaffEditor
        extends ObjectEditor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StaffEditor.class);

    /** Minimum section width/height ratio for line core sections. */
    protected static final double minWidthHeightRatio = constants.minWidthHeightRatio.getValue();

    //~ Instance fields ----------------------------------------------------------------------------

    protected StaffModel originalModel;

    protected StaffModel model;

    protected final BasicIndex<Section> removedSections = new BasicIndex<>(new AtomicInteger(0));

    /** Staff lines. */
    protected final List<LineInfo> lines;

    /** Maximum line thickness. */
    protected final int maxLineThickness;

    /** Sheet horizontal lag. */
    protected final Lag hLag;

    /** Relevant horizontal sections for staff edition, ordered by starting ordinate. */
    protected final SortedSet<Section> staffSections;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a <code>StaffEditor</code> instance.
     *
     * @param staff the staff to edit
     */
    protected StaffEditor (Staff staff)
    {
        super(staff, staff.getSystem());
        lines = staff.getLines();
        maxLineThickness = system.getSheet().getScale().getMaxFore();
        hLag = system.getSheet().getLagManager().getLag(Lags.HLAG);

        staffSections = collectRelevantSections();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------------//
    // applyModel //
    //------------//
    /**
     * Apply the geometry of provided model to the staff lines.
     *
     * @param model the model to apply
     */
    protected abstract void applyModel (StaffModel model);

    //-------------------------//
    // collectRelevantSections //
    //-------------------------//
    /**
     * Collect just the horizontal sections that may be impacted by staff edition.
     *
     * @return the set of relevant sections, ordered by starting ordinate
     */
    private SortedSet<Section> collectRelevantSections ()
    {
        final Staff staff = getStaff();
        final Rectangle staffBox = staff.getAreaBounds().getBounds();

        // Side abscissae may not be the ultimate ones, so let's widen staffBox until sheet limits
        final Sheet sheet = staff.getSystem().getSheet();
        staffBox.setBounds(0, staffBox.y, sheet.getWidth(), staffBox.height);

        final List<Section> sections = new ArrayList<>();
        hLag.getEntities().forEach(section ->
        {
            if (section.getBounds().intersects(staffBox)) {
                sections.add(section);
            }
        });

        final SortedSet<Section> set = new TreeSet<>(Section.byFullPosition);
        set.addAll(sections);

        return set;
    }

    //------//
    // doit //
    //------//
    @Override
    public void doit ()
    {
        // Reset sections to their initial status
        hLag.insertSections(model.removedSections.getEntities());
        model.removedSections.reset();

        applyModel(model); // Change lines geometry

        // Detect and remove sections on staff lines
        lines.forEach(line ->
        {
            final StaffLine staffLine = (StaffLine) line;
            final List<Section> lineRemovedSections = getRemovals(staffLine);
            model.removedSections.setEntities(lineRemovedSections);
            hLag.removeSections(lineRemovedSections);
        });
    }

    //------------//
    // endProcess //
    //------------//
    @Override
    public void endProcess ()
    {
        if (hasMoved) {
            system.getSheet().getInterController().editObject(this);
        }

        system.getSheet().getSheetEditor().closeEditMode();
    }

    /**
     * Retrieve the horizontal sections to remove on the provided staff line.
     *
     * @param staffLine the staff line to process
     * @return the sections to remove
     */
    private List<Section> getRemovals (StaffLine staffLine)
    {
        final NaturalSpline spline = staffLine.getSpline();
        final List<Section> toRemove = new ArrayList<>();

        // First, retrieve sections intersected by the staff spline
        for (Section section : staffSections) {
            final Rectangle box = section.getBounds();

            if (spline.intersects(box)) {
                // Refine intersection check
                final double yLeft = staffLine.yAt((double) box.x);
                final double yRight = staffLine.yAt((double) (box.x + box.width));
                if (yLeft >= box.y && yLeft <= box.y + box.height || (yRight >= box.y
                        && yRight <= box.y + box.height)) {

                    // Check minimum ratio width / height
                    final double wh = box.width / (double) box.height;
                    //                logger.info(" {} w:{} h:{} w/h: {}",
                    //                            section, box.width, box.height, String.format("%.1f", wh));
                    if (wh >= minWidthHeightRatio) {
                        toRemove.add(section);
                    }
                }
            }
        }

        // Second, retrieve stuck sections
        toRemove.addAll(getStickers(toRemove));

        return toRemove;
    }

    //----------//
    // getStaff //
    //----------//
    protected Staff getStaff ()
    {
        return (Staff) object;
    }

    /**
     * Retrieve the sections stuck on the provided core sections
     *
     * @param coreSections the provided core sections
     * @return the sticker sections
     */
    private Set<Section> getStickers (List<Section> coreSections)
    {
        final Set<Section> stickers = new HashSet<>();
        final List<Section> candidates = new ArrayList<>(staffSections);
        candidates.removeAll(coreSections);

        for (Section core : coreSections) {
            final int maxCandThickness = maxLineThickness - core.getRunCount();

            for (VerticalSide vSide : VerticalSide.values()) {
                final Run run = (vSide == TOP) ? core.getFirstRun() : core.getLastRun();
                final int pos = (vSide == TOP) ? core.getFirstPos() : core.getLastPos();
                final int nextPos = (vSide == TOP) ? pos - 1 : pos + 1;
                final int xMin = run.getStart();
                final int xMax = run.getStop();

                for (Iterator<Section> it = candidates.iterator(); it.hasNext();) {
                    final Section cand = it.next();

                    // Check ordinate adjacency
                    final int candPos = (vSide == TOP) ? cand.getLastPos() : cand.getFirstPos();

                    if (candPos == nextPos) {
                        // Check abscissa overlap
                        final Run candRun = (vSide == TOP) ? cand.getLastRun() : cand.getFirstRun();

                        if (candRun.getStart() <= xMax && candRun.getStop() >= xMin) {
                            // Check resulting line thickness
                            if (cand.getRunCount() <= maxCandThickness) {
                                stickers.add(cand);
                                it.remove();
                            }
                        }
                    }
                }
            }
        }

        return stickers;
    }

    //------//
    // undo //
    //------//
    @Override
    public void undo ()
    {
        applyModel(originalModel); // Reset lines geometry

        // Cancel the sections removal
        hLag.insertSections(model.removedSections.getEntities());
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio minWidthHeightRatio = new Constant.Ratio(
                2.0,
                "Minimum width/height ratio for a core section on a staff line");
    }

    //--------//
    // Global //
    //--------//
    /**
     * Editor to edit the staff as a whole, where staff lines are kept parallel.
     * <p>
     * There is a horizontal sequence of handles on staff midline, all moving vertically,
     * except for the side handles which can move both horizontally and vertically.
     */
    public static class Global
            extends StaffEditor
    {

        /** Staff middle line. */
        private final StaffLine midLine;

        /**
         * Map of vertical distances for every defining point WRT staff midLine.
         * <ul>
         * <li>Key: line index in staff lines
         * <li>Value: list of ordinate deltas, one for each defining point of the line
         * </ul>
         */
        private final SortedMap<Integer, List<Double>> dyMap = new TreeMap<>();

        public Global (Staff staff)
        {
            super(staff);

            midLine = (StaffLine) staff.getMidLine();

            // Compute the delta ordinates
            populateDyMap();

            // Models
            originalModel = new GlobalModel(midLine);
            model = new GlobalModel(midLine);

            final List<Point2D> points = ((GlobalModel) model).midModel.points;
            final int nb = points.size();

            // Side handles
            Arrays.asList(points.get(0), points.get(nb - 1)).forEach(p -> handles.add(new Handle(p)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    // Move in any direction
                    PointUtil.add(p, dx, dy);

                    return true;
                }
            }));

            // Inside handles
            points.subList(1, nb - 1).forEach(p -> handles.add(new Handle(p)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    // Move only vertically
                    if (dy == 0) {
                        return false;
                    }

                    PointUtil.add(p, 0, dy);

                    return true;
                }
            }));
        }

        @Override
        protected void applyModel (StaffModel model)
        {
            final List<Point2D> midPoints = ((GlobalModel) model).midModel.points;
            final Staff staff = getStaff();

            // Special policy for a side handle: all lines ends are kept aligned vertically
            final double left = midPoints.get(0).getX();
            final double right = midPoints.get(midPoints.size() - 1).getX();

            // Adjust midLine
            midLine.setPoints(midPoints);
            midLine.getSpline();

            // Apply the dys for the other lines
            for (Entry<Integer, List<Double>> entry : dyMap.entrySet()) {
                final StaffLine line = (StaffLine) lines.get(entry.getKey());
                final List<Point2D> points = line.getPoints();
                final int ipLast = points.size() - 1; // Index to last line point
                final List<Double> dys = entry.getValue();
                final List<Point2D> newPoints = new ArrayList<>();

                // Left
                newPoints.add(new Point2D.Double(left, midLine.yAt(left) + dys.get(0)));

                // Inside
                for (int ip = 1; ip < ipLast; ip++) {
                    final Point2D p = points.get(ip);
                    final double x = p.getX();
                    final double midY = midLine.yAt(x);
                    final double dy = dys.get(ip);
                    newPoints.add(new Point2D.Double(x, midY + dy));
                }

                // Right
                newPoints.add(new Point2D.Double(right, midLine.yAt(right) + dys.get(ipLast)));

                line.setPoints(newPoints);
            }

            staff.setAbscissa(LEFT, (int) Math.rint(left));
            staff.setAbscissa(RIGHT, (int) Math.rint(right));
            staff.setArea(null);
        }

        /**
         * Compute the vertical deltas of every line with respect to the staff middle line.
         */
        private void populateDyMap ()
        {
            for (int il = 0; il < lines.size(); il++) {
                final StaffLine line = (StaffLine) lines.get(il);

                if (line != midLine) {
                    final List<Double> dys = new ArrayList<>();
                    line.getPoints().forEach(p -> dys.add(p.getY() - midLine.yAt(p.getX())));
                    dyMap.put(il, dys);
                }
            }
        }

        /** Driven by midLine model. */
        private static class GlobalModel
                extends StaffModel
        {

            public final LineModel midModel;

            public GlobalModel (StaffLine staffLine)
            {
                midModel = new LineModel(staffLine);
            }
        }
    }

    //-----------//
    // LineModel //
    //-----------//
    protected static class LineModel
    {

        public final List<Point2D> points = new ArrayList<>();

        public LineModel (StaffLine staffLine)
        {
            // Make a deep copy of provided points, to avoid live sharing of points
            points.addAll(staffLine.getPointsDeepCopy());
        }
    }

    //-------//
    // Lines //
    //-------//
    /**
     * Editor to edit all lines individually.
     * <p>
     * There are as many handles as lines defining points, all moving only vertically.
     */
    public static class Lines
            extends StaffEditor
    {

        public Lines (Staff staff)
        {
            super(staff);

            // Models
            originalModel = new LineArrayModel();
            model = new LineArrayModel();

            // Handles
            ((LineArrayModel) model).lineModels.forEach(
                    lineModel -> lineModel.points.forEach(p -> handles.add(new Handle(p)
                    {
                        @Override
                        public boolean move (int dx,
                                             int dy)
                        {
                            // Move only vertically
                            if (dy == 0) {
                                return false;
                            }

                            PointUtil.add(p, 0, dy);

                            return true;
                        }
                    })));
        }

        @Override
        protected void applyModel (StaffModel model)
        {
            final LineArrayModel lam = (LineArrayModel) model;

            for (int il = 0; il < lines.size(); il++) {
                final StaffLine staffLine = (StaffLine) lines.get(il);
                staffLine.setPoints(lam.lineModels.get(il).points);
            }
        }

        /** One model per staff line. */
        private class LineArrayModel
                extends StaffModel
        {

            public final List<LineModel> lineModels = new ArrayList<>();

            public LineArrayModel ()
            {
                lines.forEach(line -> lineModels.add(new LineModel((StaffLine) line)));
            }
        }
    }

    //------------//
    // StaffModel //
    //------------//
    protected static abstract class StaffModel
            implements ObjectUIModel
    {

        public final BasicIndex<Section> removedSections = new BasicIndex<>(new AtomicInteger(0));
    }
}
