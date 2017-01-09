//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 M e a s u r e s B u i l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import net.jcip.annotations.NotThreadSafe;

import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffBarline;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.relation.BarGroupRelation;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code MeasuresBuilder} is in charge, at system level, of building all measures
 * from the barlines found.
 *
 * @author Hervé Bitteur
 */
@NotThreadSafe
public class MeasuresBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            MeasuresBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** System SIG. */
    private final SIGraph sig;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MeasuresBuilder} object.
     *
     * @param system the dedicated system
     */
    public MeasuresBuilder (SystemInfo system)
    {
        this.system = system;
        sig = system.getSig();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // buildMeasures //
    //---------------//
    /**
     * Based on barlines found, allocate all measures in the system.
     * <p>
     * Parts and physical BarlineInter's have been identified within the system.
     * Each staff has its BarlineInter's attached.
     * <p>
     * To build the logical StaffBarline's, PartBarline's and Measures, the strategy is: <ol>
     * <li>Staff by staff, gather BarlineInter's into groups.</li>
     * <li>Check & adjust consistency across staves within part</li>
     * <li>In each part, browsing the sequence of groups in first staff which is now the reference,
     * allocate the corresponding PartBarline's and measures.</li>
     * </ol>
     * <p>
     * Grouping strategy:<ul>
     * <li>A group of 2 physical bar lines, whatever their thickness, gives a single logical bar
     * line.</li>
     * <li>A group of 3 or 4 physical barlines (thin | thick | thin) or (thin | thick | thick |
     * thin) gives two logical barlines (thin | thick) and (thick | thin).</li>
     * <li>In the case of 3 verticals, the middle one is "shared" between the two logicals.</li>
     * <li>In the case of 4 verticals, no line is shared between the two logicals.</li>
     * </ul>
     * TODO: should we handle the degraded case of staff with no barline at all?
     */
    public void buildMeasures ()
    {
        for (Part part : system.getParts()) {
            // Determine groups of BarlineInter's for each staff within part
            Map<Staff, List<List<BarlineInter>>> staffMap = new TreeMap<Staff, List<List<BarlineInter>>>(
                    Staff.byId);

            for (Staff staff : part.getStaves()) {
                staffMap.put(staff, getBarGroups(staff.getBars()));
            }

            // Check/adjust groups across staves in part
            // TODO
            //
            // Allocate measures in part
            buildPartMeasures(part, staffMap);
        }
    }

    //-------------------//
    // buildPartMeasures //
    //-------------------//
    /**
     * Here, we build the sequence of PartBarlines in parallel with the StaffBarline
     * sequence of each staff within part.
     * <p>
     * What if the staff sequences don't agree?
     * <p>
     * Missing bar in a staff or additional bar in a staff:<ul>
     * <li> When there is a physical connection across two staves, the bar is certain.</li>
     * <li>
     * Without any physical connection, the choice is less obvious.
     * In parts with more than 2 staves, we could use a voting mechanism?
     * </li>
     * </ul>
     * Bars grouped in a staff but not grouped in another staff: We force the grouping.</li>
     *
     * @param part     the containing part
     * @param staffMap the StaffBarline sequences per staff within part
     * @return the PartBarline sequence
     */
    private void buildPartMeasures (Part part,
                                    Map<Staff, List<List<BarlineInter>>> staffMap)
    {
        // TODO: Simplistic implementation: use only the top staff
        final Staff topStaff = part.getFirstStaff();
        final List<List<BarlineInter>> topGroups = staffMap.get(topStaff);
        final boolean noRightBar = topStaff.getSideBar(HorizontalSide.RIGHT) == null;
        final int igMax = noRightBar ? topGroups.size() : (topGroups.size()
                                                           - 1);
        PartBarline leftBarPending = null;

        for (int ig = 0; ig <= igMax; ig++) {
            List<BarlineInter> topGroup = (ig < topGroups.size()) ? topGroups.get(ig) : null;
            Measure measure = ((topGroup != null)
                               && topGroup.get(0).isStaffEnd(HorizontalSide.LEFT)) ? null
                    : new Measure(part);

            if (measure != null) {
                part.addMeasure(measure);

                final int im = part.getMeasures().size() - 1;

                while (system.getMeasureStacks().size() <= im) {
                    system.getMeasureStacks().add(new MeasureStack(system));
                }
            }

            if ((measure != null) && (leftBarPending != null)) {
                measure.setLeftBarline(leftBarPending);
                leftBarPending = null;
            }

            if (topGroup != null) {
                // Logical barline with at most first 2 bars of the group
                PartBarline partBar = new PartBarline();

                for (Staff s : part.getStaves()) {
                    StaffBarline staffBar = new StaffBarline();
                    partBar.addStaffBarline(staffBar);

                    List<BarlineInter> group = staffMap.get(s).get(ig);

                    for (int i = 0; i < Math.min(2, topGroup.size()); i++) {
                        staffBar.addBar(group.get(i));
                    }

                    //TODO: Dots will appear only in SYMBOLS step!!!!
                    ///addDotsTo(staffBar);
                }

                if (measure == null) {
                    part.setLeftBarline(partBar);
                } else {
                    measure.setRightBarline(partBar);
                }

                if (topGroup.size() > 2) {
                    // We have a second logical barline with last 2 bars of group
                    // And it starts a new measure
                    partBar = new PartBarline();

                    for (Staff s : part.getStaves()) {
                        StaffBarline staffBar = new StaffBarline();
                        partBar.addStaffBarline(staffBar);

                        List<BarlineInter> group = staffMap.get(s).get(ig);

                        for (int i = topGroup.size() - 2; i < topGroup.size(); i++) {
                            staffBar.addBar(group.get(i));
                        }

                        //TODO: Dots will appear only in SYMBOLS step!!!!
                        ///addDotsTo(staffBar);
                    }

                    leftBarPending = partBar;
                }
            }

            if (measure != null) {
                final int im = part.getMeasures().size() - 1;
                MeasureStack stack = system.getMeasureStacks().get(im);
                measure.setStack(stack);
                stack.addMeasure(measure);
            }
        }
    }

    //--------------//
    // getBarGroups //
    //--------------//
    /**
     * Build the sequence of grouped barlines.
     *
     * @param bars the sequence of barlines
     * @return the sequence of groups
     */
    private List<List<BarlineInter>> getBarGroups (List<BarlineInter> bars)
    {
        List<List<BarlineInter>> groups = new ArrayList<List<BarlineInter>>();

        for (int i = 0; i < bars.size(); i++) {
            BarlineInter bLast = bars.get(i);

            for (int j = i + 1; j < bars.size(); j++) {
                BarlineInter bNext = bars.get(j);

                if (sig.getRelation(bLast, bNext, BarGroupRelation.class) != null) {
                    bLast = bNext; // Include bNext in the group and try to move on
                } else {
                    break; // Group has ended
                }
            }

            int ibLast = bars.indexOf(bLast);
            groups.add(bars.subList(i, ibLast + 1));
            i = ibLast;
        }

        return groups;
    }
}
