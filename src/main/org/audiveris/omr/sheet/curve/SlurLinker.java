//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S l u r L i n k e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.curve;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.GeoPath;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import static org.audiveris.omr.math.LineUtil.bisector;
import static org.audiveris.omr.math.LineUtil.intersection;
import static org.audiveris.omr.math.LineUtil.intersectionAtX;
import org.audiveris.omr.math.PointUtil;
import static org.audiveris.omr.math.PointUtil.*;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class {@code SlurLinker} retrieves the potential left and right links of a single slur
 * with the most suitable heads found in slur side areas.
 * <ul>
 * <li>Rather <b>horizontal</b> slurs have specific side areas, select intersected chords,
 * then select the closest head within those chords.
 * <li>Rather <b>vertical</b> slurs have specific side areas, select intersected chords and also
 * check that heads centers are contained by side areas, then select the closest head.
 * Head center must be on the same side (same half plane) of slur bisector than the slur end.
 * </ul>
 * In both cases, head center location is checked with respect to slur concavity.
 * <p>
 * <img alt="Areas image" src="doc-files/SlurAreas.png">
 *
 * @author Hervé Bitteur
 */
public class SlurLinker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SlurLinker.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Scale-dependent parameters. */
    private final Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SlurLinker} object.
     *
     * @param sheet the containing sheet
     */
    public SlurLinker (Sheet sheet)
    {
        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // defineAreaPair //
    //----------------//
    /**
     * Define the pair of look-up areas for the slur, one on first end and
     * another on last end.
     * <p>
     * These areas will be looked up for intersection with head-based chords (rather than heads).
     * The strategy to define look-up areas differs between "horizontal" and "vertical" slurs.
     * <p>
     * <img alt="Areas image" src="doc-files/SlurAreas.png">
     *
     * @param slur the slur being processed
     * @return the area on both slur sides
     */
    public Map<HorizontalSide, Area> defineAreaPair (SlurInter slur)
    {
        final SlurInfo info = slur.getInfo();
        final Point first = info.getEnd(true);
        final Point last = info.getEnd(false);
        final int slurWidth = abs(last.x - first.x);
        final int vDir = info.above();
        final int hDir = Integer.signum(last.x - first.x);
        final Point2D mid = new Point2D.Double((first.x + last.x) / 2d, (first.y + last.y) / 2d);
        final Point2D firstExt;
        final Point2D lastExt;
        final GeoPath firstPath;
        final GeoPath lastPath;
        final Line2D baseLine;
        final Point2D firstBase;
        final Point2D lastBase;

        // Qualify the slur as horizontal or vertical
        if ((abs(LineUtil.getSlope(first, last)) <= params.slopeSeparator)
            || (slurWidth >= params.wideSlurWidth)) {
            // Horizontal: Use base parallel to slur
            info.setHorizontal(true);
            firstExt = extension(mid, first, params.coverageHExt);
            lastExt = extension(mid, last, params.coverageHExt);
            firstBase = new Point2D.Double(
                    firstExt.getX(),
                    firstExt.getY() + (vDir * params.coverageHDepth));
            lastBase = new Point2D.Double(
                    lastExt.getX(),
                    lastExt.getY() + (vDir * params.coverageHDepth));
            baseLine = new Line2D.Double(firstBase, lastBase);

            if (slurWidth > (2 * params.coverageHIn)) {
                // Wide slur: separate first & last areas
                Point2D firstIn = extension(mid, first, -params.coverageHIn);
                firstPath = new GeoPath(new Line2D.Double(firstIn, firstExt));
                firstPath.append(
                        new Line2D.Double(firstBase, intersectionAtX(baseLine, firstIn.getX())),
                        true);

                Point2D lastIn = extension(mid, last, -params.coverageHIn);
                lastPath = new GeoPath(new Line2D.Double(lastIn, lastExt));
                lastPath.append(
                        new Line2D.Double(lastBase, intersectionAtX(baseLine, lastIn.getX())),
                        true);
            } else {
                // Narrow slur: just one vertical separation
                Point2D midBase = intersectionAtX(baseLine, mid.getX());
                firstPath = new GeoPath(new Line2D.Double(mid, firstExt));
                firstPath.append(new Line2D.Double(firstBase, midBase), true);
                lastPath = new GeoPath(new Line2D.Double(mid, lastExt));
                lastPath.append(new Line2D.Double(lastBase, midBase), true);
            }
        } else {
            // Vertical: Use slanted separation
            info.setHorizontal(false);
            firstExt = extension(mid, first, params.coverageVExt);
            lastExt = extension(mid, last, params.coverageVExt);

            Point2D bisUnit = info.getBisUnit();
            double vDepth = (slurWidth <= params.maxSmallSlurWidth) ? params.coverageVDepthSmall
                    : params.coverageVDepth;
            Point2D depth = new Point2D.Double(vDepth * bisUnit.getX(), vDepth * bisUnit.getY());
            firstBase = new Point2D.Double(
                    firstExt.getX() + depth.getX(),
                    firstExt.getY() + depth.getY());
            lastBase = new Point2D.Double(
                    lastExt.getX() + depth.getX(),
                    lastExt.getY() + depth.getY());
            baseLine = new Line2D.Double(firstBase, lastBase);

            if (first.distance(last) > (2 * params.coverageVIn)) {
                // Tall slur, separate first & last areas
                Point2D firstIn = extension(firstExt, first, params.coverageVIn);
                Point2D firstBaseIn = new Point2D.Double(
                        firstIn.getX() + depth.getX(),
                        firstIn.getY() + depth.getY());
                firstPath = new GeoPath(new Line2D.Double(firstIn, firstExt));
                firstPath.append(new Line2D.Double(firstBase, firstBaseIn), true);

                Point2D lastIn = extension(lastExt, last, params.coverageVIn);
                Point2D lastBaseIn = new Point2D.Double(
                        lastIn.getX() + depth.getX(),
                        lastIn.getY() + depth.getY());
                lastPath = new GeoPath(new Line2D.Double(lastIn, lastExt));
                lastPath.append(new Line2D.Double(lastBase, lastBaseIn), true);
            } else {
                // Small slur, just one slanted separation
                firstPath = new GeoPath(new Line2D.Double(mid, firstExt));
                lastPath = new GeoPath(new Line2D.Double(mid, lastExt));

                Line2D bisector = (vDir == hDir) ? bisector(first, last) : bisector(last, first);
                Point2D baseInter = intersection(baseLine, bisector);
                firstPath.append(new Line2D.Double(firstBase, baseInter), true);
                lastPath.append(new Line2D.Double(lastBase, baseInter), true);
            }
        }

        firstPath.closePath();
        lastPath.closePath();

        Map<HorizontalSide, Area> areaMap = new EnumMap<HorizontalSide, Area>(HorizontalSide.class);
        Area firstArea = new Area(firstPath);
        ///info.setArea(firstArea, true);
        areaMap.put(LEFT, firstArea);
        slur.addAttachment("F", firstArea);

        Area lastArea = new Area(lastPath);
        ///info.setArea(lastArea, false);
        areaMap.put(RIGHT, lastArea);
        slur.addAttachment("L", lastArea);

        return areaMap;
    }

    //----------------//
    // lookupLinkPair //
    //----------------//
    /**
     * Look precisely at content of lookup areas on left and right, and select best
     * pair of links if any.
     * <ul>
     * <li>The same chord (including mirror) cannot belong to both areas.</li>
     * <li>Left and right chords must differ enough in abscissa.</li>
     * <li>To be really accepted, a chord candidate must contain a head suitable to be linked on
     * proper slur side.</li>
     *
     * <li>Special heuristics for mirrored chords:<ul>
     * <li>If the slur goes to another staff, select the mirror chord whose stem points towards
     * the other staff.</li>
     * <li>If the slur stays in its staff, select the mirror chord with same stem direction as
     * the chord found at the other slur end.
     * If there is no chord as the other slur end, select the mirror chord with the smallest
     * number of beams.</li>
     * </ul>
     * </ul>
     * Possible orphan slurs are accepted for those close to staff side and rather horizontal.
     * The other slurs must exhibit links on both sides.
     *
     * @param slur   the slur candidate to check for links
     * @param areas  the lookup area on each slur side
     * @param system the containing system
     * @param chords the potential candidate chords on left and right side
     * @return the pair of links if acceptable (only half-filled for orphan), null if not
     */
    public Map<HorizontalSide, SlurHeadLink> lookupLinkPair (SlurInter slur,
                                                             Map<HorizontalSide, Area> areas,
                                                             SystemInfo system,
                                                             Map<HorizontalSide, List<Inter>> chords)
    {
        // The pair to populate
        final Map<HorizontalSide, SlurHeadLink> linkPair = new EnumMap<HorizontalSide, SlurHeadLink>(
                HorizontalSide.class);

        // Slur target locations on each side
        final Point leftTarget = getTargetPoint(slur, LEFT);
        final Point rightTarget = getTargetPoint(slur, RIGHT);

        // Chords candidates on each side
        Map<Inter, SlurHeadLink> lefts = lookup(slur, LEFT, areas.get(LEFT), chords.get(LEFT));
        Map<Inter, SlurHeadLink> rights = lookup(slur, RIGHT, areas.get(RIGHT), chords.get(RIGHT));

        // The same chord cannot be linked to both slur ends
        // Keep it only where it is closer to slur target
        Set<Inter> commons = new LinkedHashSet<Inter>();
        commons.addAll(lefts.keySet());
        commons.retainAll(rights.keySet());

        for (Inter common : commons) {
            Rectangle chordBox = common.getBounds();
            Point chordCenter = GeoUtil.centerOf(chordBox); // TODO: choose a better ref point?

            if (chordCenter.distance(leftTarget) > chordCenter.distance(rightTarget)) {
                lefts.remove(common);
            } else {
                rights.remove(common);
            }
        }

        // Reduce each side, except for couple [chord / mirrored chord]
        Map<HorizontalSide, SlurHeadLink> mirrors = new EnumMap<HorizontalSide, SlurHeadLink>(
                HorizontalSide.class);

        for (HorizontalSide side : HorizontalSide.values()) {
            Map<Inter, SlurHeadLink> links = (side == LEFT) ? lefts : rights;
            List<SlurHeadLink> list = new ArrayList<SlurHeadLink>(links.values());

            if (!list.isEmpty()) {
                Collections.sort(list, SlurHeadLink.byEuclidean);

                SlurHeadLink best = list.get(0);

                // Mirror?
                Inter mirror = best.getChord().getMirror();

                if ((mirror != null) && links.keySet().contains(mirror)) {
                    mirrors.put(side, best); // We have a conflict to solve
                } else {
                    linkPair.put(side, best); // The best link is OK
                }
            }
        }

        // Process mirrors conflicts if any
        if (!mirrors.isEmpty()) {
            for (Entry<HorizontalSide, SlurHeadLink> entry : mirrors.entrySet()) {
                // This side of the slur
                final HorizontalSide side = entry.getKey();
                final SlurHeadLink link = entry.getValue();
                final Map<Inter, SlurHeadLink> links = (side == LEFT) ? lefts : rights;
                final SlurHeadLink mirrorLink = links.get(link.getChord().getMirror());
                final boolean linkOk;

                // The other side of the slur
                final HorizontalSide otherSide = side.opposite();
                final SlurHeadLink otherLink = linkPair.get(otherSide);

                if (otherLink != null) {
                    // Compare with other side of the slur
                    final Staff otherStaff = otherLink.getChord().getTopStaff();
                    final Staff staff = link.getChord().getTopStaff();
                    final int dir = link.getChord().getStemDir();

                    if (staff != otherStaff) {
                        // Select mirror according to direction to other staff
                        linkOk = (dir * Staff.byId.compare(otherStaff, staff)) > 0;
                    } else {
                        // Select mirror with same stem dir as at other slur end
                        // (This may be called into question later when looking for ties)
                        int otherDir = otherLink.getChord().getStemDir();
                        linkOk = dir == otherDir;
                    }
                } else {
                    // No link found on other side (orphan? or slur truncated?)
                    // Not too stupid: select the mirror with less beams
                    final SIGraph sig = system.getSig();
                    Inter stem = link.getChord().getStem();
                    int nb = sig.getRelations(stem, BeamStemRelation.class).size();
                    Inter mStem = mirrorLink.getChord().getStem();
                    int mNb = sig.getRelations(mStem, BeamStemRelation.class).size();
                    linkOk = nb <= mNb;
                }

                linkPair.put(side, linkOk ? link : mirrorLink);
            }
        }

        // Check we don't have a chord on one slur side and its mirror on the other side
        SlurHeadLink leftLink = linkPair.get(LEFT);
        SlurHeadLink rightLink = linkPair.get(RIGHT);

        if ((leftLink != null) && (rightLink != null)) {
            Inter leftMirror = leftLink.getChord().getMirror();

            if ((leftMirror != null) && (leftMirror == rightLink.getChord())) {
                logger.debug("{} chord and its mirror linked by the same slur!", slur);

                return null;
            }
        }

        // No link on left and on right?
        if (linkPair.isEmpty()) {
            return null;
        }

        // One link is missing, check whether this slur candidate can be an orphan
        for (HorizontalSide side : HorizontalSide.values()) {
            if ((linkPair.get(side) == null) && !canBeOrphan(slur, side, system)) {
                return null; // TODO: Too strict for manual usage
            }
        }

        return linkPair;
    }

    //-------------//
    // canBeOrphan //
    //-------------//
    /**
     * Check whether the provided slur can be a legal orphan on the specified side.
     *
     * @param slur   the slur to check
     * @param side   which side is orphaned
     * @param system containing system
     * @return true if legal
     */
    private boolean canBeOrphan (SlurInter slur,
                                 HorizontalSide side,
                                 SystemInfo system)
    {
        final SlurInfo info = slur.getInfo();

        // Check if slur is rather horizontal
        if (abs(LineUtil.getSlope(info.getEnd(true), info.getEnd(false))) > params.maxOrphanSlope) {
            logger.debug("{} too sloped orphan", slur);

            return false;
        }

        // A left orphan must start in first measure.
        // A right orphan must stop in last measure.
        Point slurEnd = info.getEnd(side == LEFT);
        Staff staff = system.getClosestStaff(slurEnd);
        Part part = staff.getPart();
        Measure sideMeasure = (side == LEFT) ? part.getFirstMeasure() : part.getLastMeasure();
        Measure endMeasure = part.getMeasureAt(slurEnd);

        if (endMeasure != sideMeasure) {
            logger.debug("{} orphan side not in part side measure", slur);

            return false;
        }

        // Also, check horizontal gap to staff limit
        int staffEnd = (side == LEFT) ? staff.getHeaderStop() : staff.getAbscissa(side);

        if (abs(slurEnd.x - staffEnd) > params.maxOrphanDx) {
            logger.debug("{} too far orphan", slur);

            return false;
        }

        return true;
    }

    //----------------//
    // getTargetPoint //
    //----------------//
    /**
     * Report the precise target point for a head connection on desired side of a slur.
     *
     * @param slur the slur to process
     * @param side the desired slur side
     * @return the target connection point, slightly away from slur end
     */
    private Point getTargetPoint (SlurInter slur,
                                  HorizontalSide side)
    {
        final boolean rev = side == LEFT;
        final SlurInfo info = slur.getInfo();
        final Point end = info.getEnd(rev);
        final Point2D vector = info.getEndVector(rev);
        final double ext = params.targetExtension;

        return PointUtil.rounded(PointUtil.addition(end, PointUtil.times(vector, ext)));
    }

    //--------//
    // lookup //
    //--------//
    /**
     * Retrieve the best head embraced by the slur side.
     *
     * @param slur   the provided slur
     * @param side   desired side
     * @param area   lookup area on slur side
     * @param chords candidate chords on desired side
     * @return the map of heads found, perhaps empty, with their data
     */
    private Map<Inter, SlurHeadLink> lookup (SlurInter slur,
                                             HorizontalSide side,
                                             Area area,
                                             List<Inter> chords)
    {
        final Map<Inter, SlurHeadLink> found = new HashMap<Inter, SlurHeadLink>();
        final SlurInfo info = slur.getInfo();
        final Point end = info.getEnd(side == LEFT);
        final Point target = getTargetPoint(slur, side);
        final Point2D bisUnit = info.getBisUnit();

        // Look for intersected chords
        for (Inter chordInter : chords) {
            AbstractChordInter chord = (AbstractChordInter) chordInter;
            Rectangle chordBox = chord.getBounds();

            if (area.intersects(chordBox)) {
                // Check the chord contains at least one suitable head on desired slur side
                HeadInter head = selectBestHead(slur, chord, end, target, bisUnit, area);

                if (head != null) {
                    found.put(chord, SlurHeadLink.create(target, side, chord, head));
                }
            }
        }

        return found;
    }

    //----------------//
    // selectBestHead //
    //----------------//
    /**
     * Select the best note head in the selected head-based chord.
     * We select the compatible note head which is closest to slur target end.
     *
     * @param chord   the selected chord
     * @param end     the slur end point
     * @param target  the slur target point
     * @param bisUnit the direction from slur middle to slur center (unit length)
     * @param area    target area
     * @return the best note head or null
     */
    private HeadInter selectBestHead (SlurInter slur,
                                      AbstractChordInter chord,
                                      Point end,
                                      Point target,
                                      Point2D bisUnit,
                                      Area area)
    {
        final boolean horizontal = slur.getInfo().isHorizontal();
        final boolean above = slur.isAbove();

        double bestDist = Double.MAX_VALUE;
        HeadInter bestHead = null;

        for (Inter head : chord.getNotes()) {
            Point center = head.getCenter();

            if (!horizontal) {
                // We require head center to be contained by lookup area
                if (!area.contains(center)) {
                    continue;
                }
            }

            // Check head reference point WRT slur concavity
            Rectangle bounds = head.getBounds();
            Point refPt = new Point(center.x, bounds.y + (above ? (bounds.height - 1) : 0));

            if (dotProduct(subtraction(refPt, end), bisUnit) <= 0) {
                continue;
            }

            // Keep the closest head
            final double dist = center.distanceSq(target);

            if (dist < bestDist) {
                bestDist = dist;
                bestHead = (HeadInter) head;
            }
        }

        return bestHead;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction coverageHExt = new Scale.Fraction(
                1.25,
                "Length of extension for horizontal slur coverage");

        private final Scale.Fraction coverageHIn = new Scale.Fraction(
                0.5,
                "Internal abscissa of horizontal slur coverage");

        private final Scale.Fraction coverageHDepth = new Scale.Fraction(
                3.0,
                "Vertical extension of horizontal slur coverage");

        private final Scale.Fraction coverageVExt = new Scale.Fraction(
                2.0,
                "Length of extension for vertical slur coverage");

        private final Scale.Fraction coverageVIn = new Scale.Fraction(
                1.5,
                "Internal abscissa of vertical slur coverage");

        private final Scale.Fraction coverageVDepth = new Scale.Fraction(
                2.5,
                "Vertical extension of vertical slur coverage");

        private final Scale.Fraction coverageVDepthSmall = new Scale.Fraction(
                1.5,
                "Vertical extension of small vertical slur coverage");

        private final Scale.Fraction targetExtension = new Scale.Fraction(
                0.5,
                "Extension length from slur end to slur target point");

        private final Constant.Double slopeSeparator = new Constant.Double(
                "tangent",
                0.5,
                "Slope that separates vertical slurs from horizontal slurs");

        private final Constant.Double maxOrphanSlope = new Constant.Double(
                "tangent",
                0.5,
                "Maximum slope for an orphan slur");

        private final Scale.Fraction maxOrphanDx = new Scale.Fraction(
                6.0,
                "Maximum dx to staff end for an orphan slur");

        private final Scale.Fraction wideSlurWidth = new Scale.Fraction(
                6.0,
                "Minimum width to be a wide slur");

        private final Scale.Fraction maxSmallSlurWidth = new Scale.Fraction(
                1.5,
                "Maximum width for a small slur");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * All pre-scaled constants.
     */
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int coverageHExt;

        final int coverageVExt;

        final int coverageHIn;

        final int coverageVIn;

        final int coverageHDepth;

        final int coverageVDepth;

        final int coverageVDepthSmall;

        final int targetExtension;

        final double slopeSeparator;

        final double maxOrphanSlope;

        final int maxOrphanDx;

        final int wideSlurWidth;

        final int maxSmallSlurWidth;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            coverageHExt = scale.toPixels(constants.coverageHExt);
            coverageHIn = scale.toPixels(constants.coverageHIn);
            coverageHDepth = scale.toPixels(constants.coverageHDepth);
            coverageVExt = scale.toPixels(constants.coverageVExt);
            coverageVIn = scale.toPixels(constants.coverageVIn);
            coverageVDepth = scale.toPixels(constants.coverageVDepth);
            coverageVDepthSmall = scale.toPixels(constants.coverageVDepthSmall);
            targetExtension = scale.toPixels(constants.targetExtension);
            slopeSeparator = constants.slopeSeparator.getValue();
            maxOrphanSlope = constants.maxOrphanSlope.getValue();
            maxOrphanDx = scale.toPixels(constants.maxOrphanDx);
            wideSlurWidth = scale.toPixels(constants.wideSlurWidth);
            maxSmallSlurWidth = scale.toPixels(constants.maxSmallSlurWidth);

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
