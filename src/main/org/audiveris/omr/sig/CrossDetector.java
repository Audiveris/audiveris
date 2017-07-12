//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    C r o s s D e t e c t o r                                   //
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
package org.audiveris.omr.sig;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;
import org.audiveris.omr.sig.inter.DeletedInterException;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.util.Predicate;
import org.audiveris.omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code CrossDetector} browses the horizontal gutters between systems for inters
 * which overlap across systems.
 *
 * @author Hervé Bitteur
 */
public class CrossDetector
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(CrossDetector.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code CrossDetector} object.
     *
     * @param sheet the underlying sheet
     */
    public CrossDetector (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    public void process ()
    {
        final SystemManager manager = sheet.getSystemManager();

        for (SystemInfo above : sheet.getSystems()) {
            for (SystemInfo below : manager.verticalNeighbors(above, VerticalSide.BOTTOM)) {
                detect(above, below);
            }
        }
    }

    //--------//
    // detect //
    //--------//
    private void detect (SystemInfo aboveSystem,
                         SystemInfo belowSystem)
    {
        // Gutter area
        final Area gutter = new Area(aboveSystem.getArea());
        gutter.intersect(belowSystem.getArea());

        final Rectangle gutterBounds = gutter.getBounds();

        // Build lists of candidates for above and for below
        Predicate<Inter> predicate = new Predicate<Inter>()
        {
            @Override
            public boolean check (Inter inter)
            {
                if (inter instanceof SentenceInter) {
                    return false;
                }

                final Point center = inter.getCenter();

                return gutterBounds.contains(center) && gutter.contains(center);
            }
        };

        List<Inter> aboveInters = aboveSystem.getSig().inters(predicate);

        if (aboveInters.isEmpty()) {
            return;
        }

        List<Inter> belowInters = belowSystem.getSig().inters(predicate);

        if (belowInters.isEmpty()) {
            return;
        }

        // Cross exclusions
        logger.debug(
                "Cross detection between {}: {} inters and {}: {} inters",
                aboveSystem,
                aboveInters.size(),
                belowSystem,
                belowInters.size());
        detectCrossOverlaps(aboveInters, belowInters);
    }

    //---------------------//
    // detectCrossOverlaps //
    //---------------------//
    /**
     * Detect all cases where 2 Inters actually overlap while being in separate systems.
     *
     * @param list1 the collection of inters to process from one system
     * @param list2 the collection of inters to process from another system
     */
    private void detectCrossOverlaps (List<Inter> list1,
                                      List<Inter> list2)
    {
        Collections.sort(list2, Inter.byAbscissa);

        NextLeft:
        for (Inter left : list1) {
            if (left.isDeleted()) {
                continue;
            }

            final Rectangle leftBox = left.getBounds();

            final double xMax = leftBox.getMaxX();

            for (Inter right : list2) {
                if (right.isDeleted()) {
                    continue;
                }

                Rectangle rightBox = right.getBounds();

                if (leftBox.intersects(rightBox)) {
                    // Have a more precise look
                    if (left.isVip() && right.isVip()) {
                        logger.info("VIP check cross overlap {} vs {}", left, right);
                    }

                    try {
                        if (left.overlaps(right) && right.overlaps(left)) {
                            resolveConflict(left, right);
                        }
                    } catch (DeletedInterException diex) {
                        if (diex.inter == left) {
                            continue NextLeft;
                        }
                    }
                } else if (rightBox.x > xMax) {
                    break; // Since right list is sorted by abscissa
                }
            }
        }
    }

    /**
     * Resolve the conflict detected between the two provided inters.
     * <p>
     * We simply delete the weaker if there is any significant difference in grade.
     * Otherwise we deleted the inter farther from its own staff
     *
     * @param left  an inter (in one system)
     * @param right an inter (in another system)
     */
    private void resolveConflict (Inter left,
                                  Inter right)
    {
        logger.debug("resolveConflict {} & {}", left, right);

        double gradeDiff = Math.abs(left.getBestGrade() - right.getBestGrade());

        if (gradeDiff >= constants.minGradeDiff.getValue()) {
            Inter weaker = (left.getBestGrade() <= right.getBestGrade()) ? left : right;

            if (weaker.isVip()) {
                logger.info("VIP Deleting weaker {}", weaker);
            }

            weaker.delete();
        } else {
            final double lDist = staffDistance(left);
            final double rDist = staffDistance(right);
            final Inter farther = (lDist >= rDist) ? left : right;

            if (farther.isVip()) {
                logger.info("Deleting farther {}", farther);
            }

            farther.delete();
        }
    }

    /**
     * Return the distance between the provided inter and "its" staff.
     *
     * @param inter provided inter
     * @return distance to its staff
     */
    private double staffDistance (Inter inter)
    {
        final Point center = inter.getCenter();
        Staff staff = inter.getStaff();

        if (staff == null) {
            staff = inter.getSig().getSystem().getClosestStaff(center);
        }

        return staff.distanceTo(center);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Evaluation.Grade minGradeDiff = new Evaluation.Grade(
                0.1,
                "Minimum difference in ctx-grade to be relevant");
    }
}
