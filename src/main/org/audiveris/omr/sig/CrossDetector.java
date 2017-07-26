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
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;
import org.audiveris.omr.sig.inter.DeletedInterException;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.Support;
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
     * @param aboves the collection of inters to process from system above
     * @param belows the collection of inters to process from system below
     */
    private void detectCrossOverlaps (List<Inter> aboves,
                                      List<Inter> belows)
    {
        Collections.sort(aboves, Inter.byAbscissa);
        Collections.sort(belows, Inter.byAbscissa);

        NextLeft:
        for (Inter above : aboves) {
            if (above.isDeleted()) {
                continue;
            }

            final Rectangle aboveBox = above.getBounds();

            final double xMax = aboveBox.getMaxX();

            for (Inter below : belows) {
                if (below.isDeleted()) {
                    continue;
                }

                Rectangle belowBox = below.getBounds();

                if (aboveBox.intersects(belowBox)) {
                    // Have a more precise look
                    try {
                        if (above.overlaps(below) && below.overlaps(above)) {
                            resolveConflict(above, below);
                        }
                    } catch (DeletedInterException diex) {
                        if (diex.inter == above) {
                            continue NextLeft;
                        }
                    }
                } else if (belowBox.x > xMax) {
                    break; // Since below list is sorted by abscissa
                }
            }
        }
    }

    /**
     * Return the distance between the provided inter and its closest partner if any.
     *
     * @param inter provided inter
     * @return distance to its closest partner if any, null otherwise
     */
    private double partnerDistance (Inter inter)
    {
        final Point center = inter.getCenter();
        final SIGraph sig = inter.getSig();
        double bestD2 = Double.MAX_VALUE;

        for (Relation rel : sig.getRelations(inter, Support.class)) {
            Inter partner = sig.getOppositeInter(inter, rel);
            Rectangle box = partner.getBounds();
            double d2 = GeoUtil.ptDistanceSq(box, center.x, center.y);
            bestD2 = Math.min(bestD2, d2);
        }

        return Math.sqrt(bestD2);
    }

    /**
     * Resolve the conflict detected between the two provided inters.
     * <p>
     * We simply delete the weaker if there is any significant difference in grade.
     * Otherwise we deleted the inter farther from its own staff or partnering chord
     *
     * @param above an inter (in system above)
     * @param below an inter (in system below)
     */
    private void resolveConflict (Inter above,
                                  Inter below)
    {
        if (above.isVip() && below.isVip()) {
            logger.info("VIP resolveConflict? {} vs {}", above, below);
        }

        double gradeDiff = Math.abs(above.getBestGrade() - below.getBestGrade());

        if (gradeDiff >= constants.minGradeDiff.getValue()) {
            Inter weaker = (above.getBestGrade() <= below.getBestGrade()) ? above : below;

            if (weaker.isVip()) {
                logger.info("VIP Deleting weaker {}", weaker);
            }

            weaker.delete();
        } else {
            final double aDist = Math.min(staffDistance(above), partnerDistance(above));
            final double bDist = Math.min(staffDistance(below), partnerDistance(below));
            final Inter farther = (aDist >= bDist) ? above : below;

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
