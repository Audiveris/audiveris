//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 M e a s u r e s B u i l d e r                                  //
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.BasicLine;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.relation.BarGroupRelation;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jcip.annotations.NotThreadSafe;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Class <code>MeasuresBuilder</code> is in charge, at system level, of ensuring barlines
 * consistency and of building all measures.
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class MeasuresBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(MeasuresBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** Sequence of groups of barlines per staff. */
    private final Map<Staff, List<Group>> staffGroupsMap = new TreeMap<>(Staff.byId);

    /** Sequence of StaffBarline's per staff. */
    private final Map<Staff, List<StaffBarlineInter>> staffBarsMap = new TreeMap<>(Staff.byId);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * * Creates a new <code>MeasuresBuilder</code> object.
     *
     * @param system the dedicated system
     */
    public MeasuresBuilder (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------------//
    // buildGroups //
    //-------------//
    /**
     * Build the sequence of groups of barlines for a staff.
     *
     * @param barlines the sequence of barlines of a staff
     * @return the sequence of groups
     */
    private List<Group> buildGroups (List<BarlineInter> barlines)
    {
        final SIGraph sig = system.getSig();
        final List<Group> groups = new ArrayList<>();

        for (int i = 0; i < barlines.size(); i++) {
            BarlineInter bLast = barlines.get(i);

            for (int j = i + 1; j < barlines.size(); j++) {
                BarlineInter bNext = barlines.get(j);

                if (sig.getRelation(bLast, bNext, BarGroupRelation.class) != null) {
                    bLast = bNext; // Include bNext in the group and try to move on
                } else {
                    break; // Group has ended
                }
            }

            int ibLast = barlines.indexOf(bLast);
            groups.add(new Group(barlines.subList(i, ibLast + 1), system));
            i = ibLast;
        }

        return groups;
    }

    //---------------//
    // buildMeasures //
    //---------------//
    /**
     * Based on barlines found, allocate all measures in the system.
     * <p>
     * Parts and physical BarlineInter's have been identified within the system.
     * Each staff has its BarlineInter's attached.
     * <p>
     * To build the logical StaffBarlineInter's, PartBarline's and Measures, the strategy is:
     * <ol>
     * <li>Staff by staff, gather barlines into groups of closely located barlines
     * (StaffBarlineInter's).</li>
     * <li>Check and adjust consistency across all staves within the system</li>
     * <li>In each part, browsing the sequence of groups in first staff which is now the reference,
     * allocate the corresponding PartBarline's and measures.</li>
     * </ol>
     * <p>
     * Strategy for assigning barlines to measures:
     * <ul>
     * <li>A group of 2 physical barlines, whatever their thickness, gives a single logical
     * barline.</li>
     * <li>A group of 3 or 4 physical barlines (thin | thick | thin) or (thin | thick | thick |
     * thin) gives two logical barlines (thin | thick) and (thick | thin).</li>
     * <li>In the case of 3 verticals, the middle one is "shared" between the two logicals.</li>
     * <li>In the case of 4 verticals, no line is shared between the two logicals.</li>
     * </ul>
     * TODO: should we handle the degraded case of staff with no barline at all?
     */
    public void buildMeasures ()
    {
        // Determine groups of BarlineInter's for each staff within system
        for (Staff staff : system.getStaves()) {
            staffGroupsMap.put(staff, buildGroups(staff.getBarlines()));
        }

        // Enforce consistency within system
        enforceSystemConsistency();

        // Create StaffBarlineInter's out of barline groups
        buildStaffBarlines();

        // Allocate measures in each part
        for (Part part : system.getParts()) {
            buildPartMeasures(part);
        }

        // Empty courtesy measure if any
        emptyCourtesyMeasure();
    }

    //-------------------//
    // buildPartMeasures //
    //-------------------//
    /**
     * Here, we build the sequence of PartBarlines and measures, based on StaffBarlineInter's.
     *
     * @param part the containing part
     */
    private void buildPartMeasures (Part part)
    {
        final Staff topStaff = part.getFirstStaff();
        final List<StaffBarlineInter> topBars = staffBarsMap.get(topStaff);
        PartBarline leftBarPending = null;

        for (int i = 0; i < topBars.size(); i++) {
            final StaffBarlineInter topSb = topBars.get(i);
            final Measure measure = topSb.isStaffEnd(HorizontalSide.LEFT) ? null
                    : new Measure(part);

            if (measure != null) {
                part.addMeasure(measure);

                final int im = part.getMeasures().size() - 1;
                while (system.getStacks().size() <= im) {
                    system.addStack(new MeasureStack(system));
                }
            }

            if ((measure != null) && (leftBarPending != null)) {
                measure.setLeftPartBarline(leftBarPending);
                leftBarPending = null;
            }

            // PartBarline
            final PartBarline partBar = new PartBarline();

            for (Staff s : part.getStaves()) {
                partBar.addStaffBarline(staffBarsMap.get(s).get(i));
            }

            if (measure == null) {
                part.setLeftPartBarline(partBar);
            } else {
                measure.setRightPartBarline(partBar);
            }

            if (topSb.isBackToBack()) {
                // We have a back-to-back configuration, and it starts a new measure
                leftBarPending = partBar;
            }

            if (measure != null) {
                final int im = part.getMeasures().size() - 1;
                MeasureStack stack = system.getStacks().get(im);
                measure.setStack(stack);
                stack.addMeasure(measure);
            }
        }

        // Check for specific configurations at staff end:
        // - Staff with no barline at all, we consider we have just one measure
        // - Concrete ending measure with no barline on its right side
        if (topBars.isEmpty() || // No barline at all
                !topBars.get(topBars.size() - 1).isStaffEnd(RIGHT)) // Measure w/o right barline
        {
            final Measure measure = new Measure(part);
            part.addMeasure(measure);
            measure.setLeftPartBarline(leftBarPending); // Perhaps null

            final int im = part.getMeasures().size() - 1;
            while (system.getStacks().size() <= im) {
                system.addStack(new MeasureStack(system));
            }

            final MeasureStack stack = system.getStacks().get(im);
            measure.setStack(stack);
            stack.addMeasure(measure);
        }
    }

    //--------------------//
    // buildStaffBarlines //
    //--------------------//
    /**
     * From groups of barlines, build StaffBarlineInter's.
     * <p>
     * NOTA: Many StaffBarlines will be built from (physical) Barlines.
     * But some may have already been created manually by the end-user, without any physical Barline
     */
    private void buildStaffBarlines ()
    {
        final SIGraph sig = system.getSig();
        final Staff topStaff = system.getFirstStaff();
        final List<Group> topGroups = staffGroupsMap.get(topStaff);

        for (int ig = 0; ig < topGroups.size(); ig++) {
            for (Staff staff : system.getStaves()) {
                final Group group = staffGroupsMap.get(staff).get(ig);
                final StaffBarlineInter sb = new StaffBarlineInter(group);
                sb.setStaff(staff);
                sig.addVertex(sb);
            }
        }

        // Populate staffBarsMap from ALL StaffBarlineInter's in sig
        for (Staff staff : system.getStaves()) {
            staffBarsMap.put(staff, staff.getStaffBarlines());
        }
    }

    //----------------------//
    // emptyCourtesyMeasure //
    //----------------------//
    /**
     * Remove head notes in very short ending measure (courtesy measure) if any.
     */
    private void emptyCourtesyMeasure ()
    {
        MeasureStack lastStack = system.getLastStack();

        Measure topMeasure = lastStack.getFirstMeasure();

        // Check it is a short measure with no ending barline
        if (topMeasure.getRightPartBarline() != null) {
            return;
        }

        int minStandardWidth = system.getSheet().getScale().toPixels(constants.minStandardWidth);

        if (topMeasure.getWidth() >= minStandardWidth) {
            return;
        }

        // Check stack has one measure per part, otherwise simply remove this pseudo stack
        if (lastStack.getMeasures().size() < system.getParts().size()) {
            logger.info("Partial courtesy stack removed {}", lastStack);
            system.removeStack(lastStack);

            return;
        }

        // Min abscissa value for the stack
        int minX = Integer.MAX_VALUE;

        for (Staff staff : system.getStaves()) {
            Measure measure = lastStack.getMeasureAt(staff);
            minX = Math.min(minX, measure.getAbscissa(LEFT, staff));
        }

        // Look for heads in courtesy stack
        for (Inter inter : system.getSig().inters(HeadInter.class)) {
            if (inter.getCenter().x < minX) {
                continue;
            }

            HeadInter head = (HeadInter) inter;
            Part part = head.getPart();
            Measure measure = part.getMeasureAt(head.getCenter());

            if (measure.getStack() == lastStack) {
                if (logger.isDebugEnabled() || head.isVip()) {
                    logger.info("Courtesy {} purging {}", lastStack, head);
                }

                head.remove();
            }
        }
    }

    //--------------------------//
    // enforceSystemConsistency //
    //--------------------------//
    /**
     * Make sure the groups of barlines are consistent within this system.
     * <p>
     * Same number for each staff, and vertically aligned.
     */
    private void enforceSystemConsistency ()
    {
        final Scale scale = system.getSheet().getScale();
        final int maxShift = scale.toPixels(StaffBarlineInter.getMaxStaffBarlineShift());

        // Build list of columns, kept sorted on abscissa
        final List<Column> columns = new ArrayList<>();

        for (Staff staff : system.getStaves()) {
            List<Group> groups = staffGroupsMap.get(staff);

            GroupLoop:
            for (Group group : groups) {
                for (Column column : columns) {
                    if (column.canJoin(group, maxShift)) {
                        column.addGroup(staff, group);

                        continue GroupLoop;
                    }
                }

                // No compatible column found, let's create a brand new one
                Column newCol = new Column();
                newCol.addGroup(staff, group);
                columns.add(newCol);
                Collections.sort(columns);
            }
        }

        // Print out?
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();

            for (Column column : columns) {
                sb.append("\nColumn at dsk: ");
                sb.append(String.format("%4.0f", column.getDeskewedAbscissa()));
                sb.append(" =>");
                sb.append(column);
            }

            logger.info("System#{} barline groups:{}", system.getId(), sb);
        }

        // Check configuration for each column (tablatures excluded)
        final int staffCount = system.getStaves().size() - system.getTablatures().size();

        for (Column column : columns) {
            final int groupCount = column.groups.size();

            if (groupCount < staffCount) {
                final String prefix = system.getLogPrefix();
                logger.info("{}partial barline column:{}", prefix, column);

                if (groupCount < (staffCount - groupCount)) {
                    // Delete the minority
                    logger.info("{}deleting  minor column:{}", prefix, column);
                    column.delete();
                } else if (groupCount > (staffCount - groupCount)) {
                    // Spread the majority
                    logger.info("{}expanding major column:{}", prefix, column);
                    column.expand();
                } else {
                    // Even, we delete.
                    logger.info("{}deleting   half column:{}", prefix, column);
                    column.delete();
                }
            }
        }
    }

    //--------//
    // Column //
    //--------//
    /**
     * A <code>Column</code> is a vertical sequence of (groups of) barlines across the whole
     * system, useful to indicate holes.
     */
    private class Column
            implements Comparable<Column>
    {
        /** In theory, we should have exactly one group per staff. */
        final Map<Staff, Group> groups = new TreeMap<>(Staff.byId);

        /** De-skewed column mean abscissa. */
        Double xDsk;

        /**
         * Populate the cell for provided staff by the provided group.
         *
         * @param staff containing staff
         * @param group provided group of barlines
         */
        public void addGroup (Staff staff,
                              Group group)
        {
            if (groups.get(staff) != null) {
                logger.warn("Column cell occupied in {} staff: {}", this, staff.getId());
            } else {
                groups.put(staff, group);
            }

            xDsk = null; // Invalidate cached data
        }

        /**
         * Tell whether the provided group can be part of this column.
         *
         * @param group    provided group
         * @param maxShift maximum abscissa shift allowed
         * @return true if OK
         */
        public boolean canJoin (Group group,
                                int maxShift)
        {
            return Math.abs(group.getDeskewedAbscissa() - getDeskewedAbscissa()) <= maxShift;
        }

        @Override
        public int compareTo (Column that)
        {
            // Sort by increasing (de-skewed) abscissa.
            return Double.compare(this.getDeskewedAbscissa(), that.getDeskewedAbscissa());
        }

        /**
         * Remove this column.
         */
        private void delete ()
        {
            for (Entry<Staff, Group> entry : groups.entrySet()) {
                Staff staff = entry.getKey();
                Group group = entry.getValue();
                staffGroupsMap.get(staff).remove(group);

                for (BarlineInter barline : group) {
                    barline.remove();
                }
            }
        }

        @Override
        public boolean equals (Object obj)
        {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Column column) {
                return compareTo(column) == 0;
            }

            return false;
        }

        /**
         * Fill the holes in this column.
         */
        private void expand ()
        {
            // What kind of (group of) barline(s) in this column?
            int thins = 0;
            double thinWidths = 0;
            int thicks = 0;
            double thickWidths = 0;
            BasicLine line = new BasicLine();

            for (Group group : groups.values()) {
                for (BarlineInter bar : group) {
                    if (bar.getShape() == Shape.THIN_BARLINE) {
                        thins++;
                        thinWidths += bar.getWidth();
                    } else {
                        thicks++;
                        thickWidths += bar.getWidth();
                    }
                }

                line.includePoint(group.center);
            }

            final Shape shape;
            final double width;

            if (thins >= thicks) {
                shape = Shape.THIN_BARLINE;
                width = thinWidths / thins;
            } else {
                shape = Shape.THICK_BARLINE;
                width = thickWidths / thicks;
            }

            // Fill the staves that lack barline at proper location
            for (Staff staff : system.getStaves()) {
                Group group = groups.get(staff);

                if (group == null) {
                    double xStaffMiddle = (staff.getAbscissa(LEFT) + staff.getAbscissa(RIGHT))
                            / 2.0;
                    double yStaffMiddle = staff.getFirstLine().yAt(xStaffMiddle);
                    double x = line.xAtY(yStaffMiddle); // Roughly
                    double y1 = staff.getFirstLine().yAt(x);
                    double y2 = staff.getLastLine().yAt(x);
                    Line2D median = new Line2D.Double(x, y1, x, y2);
                    BarlineInter barline = new BarlineInter(
                            null,
                            shape,
                            (Double) null,
                            median,
                            width);
                    system.getSig().addVertex(barline);
                    barline.setGrade(Grades.intrinsicRatio);
                    barline.freeze();

                    List<Group> staffGroups = staffGroupsMap.get(staff);
                    staffGroups.add(new Group(Collections.singletonList(barline), system));
                    Collections.sort(staffGroups);
                    staff.addBarline(barline);
                }
            }
        }

        private double getDeskewedAbscissa ()
        {
            if (xDsk == null) {
                double x = 0;

                for (Group group : groups.values()) {
                    x += group.getDeskewedAbscissa();
                }

                xDsk = x / groups.size();
            }

            return xDsk;
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;
            hash = (23 * hash) + Objects.hashCode(this.getDeskewedAbscissa());

            return hash;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();

            for (Entry<Staff, Group> entry : groups.entrySet()) {
                sb.append(" s");
                sb.append(entry.getKey().getId());
                sb.append(":");
                sb.append(entry.getValue());
            }

            return sb.toString();
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction minStandardWidth = new Scale.Fraction(
                4.0,
                "Minimum measure width for not being a courtesy measure");
    }

    //-------//
    // Group //
    //-------//
    /**
     * A group of barlines in a staff.
     */
    private static class Group
            extends ArrayList<BarlineInter>
            implements Comparable<Group>
    {
        /** (Skewed) group center. */
        final Point2D center;

        /** De-skewed group center. */
        final Point2D dsk;

        Group (List<BarlineInter> barlines,
               SystemInfo system)
        {
            addAll(barlines);

            center = computeCenter();
            dsk = system.getSkew().deskewed(center);
        }

        @Override
        public int compareTo (Group that)
        {
            return Double.compare(this.center.getX(), that.center.getX());
        }

        private Point2D computeCenter ()
        {
            final int n = size();
            double xx = 0;
            double yy = 0;

            for (BarlineInter bar : this) {
                Point barCenter = bar.getCenter();
                xx += barCenter.x;
                yy += barCenter.y;
            }

            return new Point2D.Double(xx / n, yy / n);
        }

        public double getDeskewedAbscissa ()
        {
            return dsk.getX();
        }

        public String midString ()
        {
            return String.format("G(x:%.0f,y:%.0f)", center.getX(), center.getY());
        }

        @Override
        public String toString ()
        {
            return midString() + Inters.ids(this);
        }
    }
}
