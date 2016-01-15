//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S l u r s L i n k e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.math.GeoPath;
import omr.math.GeoUtil;
import omr.math.LineUtil;

import static omr.math.LineUtil.*;

import omr.math.PointUtil;

import static omr.math.PointUtil.*;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.SystemManager;
import omr.sheet.beam.BeamGroup;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.HeadChordInter;
import omr.sig.inter.Inter;
import omr.sig.inter.SlurInter;
import omr.sig.inter.SmallChordInter;
import omr.sig.relation.BeamStemRelation;
import omr.sig.relation.SlurHeadRelation;

import omr.util.Dumping;
import omr.util.HorizontalSide;

import static omr.util.HorizontalSide.*;

import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static java.lang.Math.abs;
import static java.lang.Math.abs;
import static java.lang.Math.abs;
import static java.lang.Math.abs;

/**
 * Class {@code SlursLinker} works at sheet level, to handle the connections between
 * slurs and embraced chords/heads.
 * <p>
 * Its primary feature is to help select the best slur in a clump of slurs, by evaluating the
 * connections to embraced chords.
 * <p>
 * <img alt="Areas image" src="doc-files/SlurAreas.png">
 *
 * @author Hervé Bitteur
 */
public class SlursLinker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SlursLinker.class);

    //~ Instance fields ----------------------------------------------------------------------------
    @Navigable(false)
    private final Sheet sheet;

    /** Scale-dependent parameters. */
    private final Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SlursLinker object.
     *
     * @param sheet the underlying sheet
     */
    public SlursLinker (Sheet sheet)
    {
        this.sheet = sheet;

        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // prune //
    //-------//
    /**
     * Process a clump of slur candidates and select the candidate with best average
     * "distance" to embraced head-based chords on left and right sides.
     *
     * @param clump a bunch of aggregated slurs
     * @return the best slur, if any
     */
    public SlurInter prune (Set<Inter> clump)
    {
        // Compute lookup areas for each slur in clump
        for (Inter inter : clump) {
            SlurInter slur = (SlurInter) inter;
            defineAreaPair(slur);
        }

        // Define global clump bounds
        Map<HorizontalSide, Rectangle> bounds = getBounds(clump);
        Rectangle clumpBox = new Rectangle(bounds.get(LEFT));
        clumpBox.add(bounds.get(RIGHT));

        // Determine the impacted system(s) and stop at first system with good result.
        SystemManager mgr = sheet.getSystemManager();
        List<SystemInfo> systems = mgr.getSystemsOf(clumpBox, null);

        SystemLoop:
        for (SystemInfo system : systems) {
            ClumpLinker linker = new ClumpLinker(system, clump, bounds);

            // Select the slur with best chord links, if any
            SlurInter selected = linker.process();

            if (selected != null) {
                // Make sure we do have connection to heads or slur is a legal orphan
                Map<HorizontalSide, AbstractHeadInter> heads = new EnumMap<HorizontalSide, AbstractHeadInter>(
                        HorizontalSide.class);

                for (HorizontalSide side : HorizontalSide.values()) {
                    AbstractHeadInter head = selected.getHead(side);

                    if ((head == null) && !canBeOrphan(selected, side, system)) {
                        selected.delete();

                        continue SystemLoop;
                    }

                    heads.put(side, head);
                }

                // Is this a tie? (Assumption: a tie within a system does not cross a clef change)
                // Make sure there is no forbidden chord within the abscissa range
                // NOTA: either linked head may have a mirror head, so select proper head for tie
                final AbstractHeadInter leftHead = heads.get(LEFT);
                final AbstractHeadInter rightHead = heads.get(RIGHT);

                if ((leftHead != null) && (rightHead != null)) {
                    final AbstractHeadInter leftMirror = (AbstractHeadInter) leftHead.getMirror();
                    final AbstractHeadInter rightMirror = (AbstractHeadInter) rightHead.getMirror();

                    if ((leftHead.getIntegerPitch() == rightHead.getIntegerPitch())
                        && (leftHead.getStaff() == rightHead.getStaff())) {
                        // Check there is no other chords in between
                        if (linker.isSpaceClear(leftHead, rightHead)) {
                            selected.setTie();
                        } else if (linker.isSpaceClear(leftMirror, rightHead)) {
                            selected.setTie();
                            selected.switchMirrorHead(LEFT);
                        } else if (linker.isSpaceClear(leftHead, rightMirror)) {
                            selected.setTie();
                            selected.switchMirrorHead(RIGHT);
                        } else if (linker.isSpaceClear(leftMirror, rightMirror)) {
                            selected.setTie();
                            selected.switchMirrorHead(LEFT);
                            selected.switchMirrorHead(RIGHT);
                        }
                    }
                }

                return selected;
            }
        }

        return null; // No acceptable candidate found
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

        // Check horizontal gap to staff limit
        Point slurEnd = info.getEnd(side == LEFT);
        Staff staff = system.getClosestStaff(slurEnd);
        int staffEnd = (side == LEFT) ? staff.getHeaderStop() : staff.getAbscissa(side);

        if (abs(slurEnd.x - staffEnd) > params.maxOrphanDx) {
            logger.debug("{} too far orphan", slur);

            return false;
        }

        return true;
    }

    //----------------//
    // defineAreaPair //
    //----------------//
    /**
     * Define the pair of look-up areas for a slur candidate, one on first end and
     * another on last end.
     * <p>
     * These areas will be looked up for intersection with head-based chords (rather than heads).
     * The strategy to define look-up areas differs between "horizontal" and "vertical" slurs.
     * <p>
     * <img alt="Areas image" src="doc-files/SlurAreas.png" />
     */
    private void defineAreaPair (SlurInter slur)
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
            // Vertical: Use slanted separation (TODO: OK with chords???)
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

        Area firstArea = new Area(firstPath);
        info.setArea(firstArea, true);
        slur.addAttachment("F", firstArea);

        ///info.addAttachment("F", firstArea); // Useful?
        Area lastArea = new Area(lastPath);
        info.setArea(lastArea, false);
        slur.addAttachment("L", lastArea);

        ///info.addAttachment("L", lastArea); // Useful?
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Report the rectangular bounds of the clump of slurs.
     *
     * @param clump the aggregated slurs
     * @return the global bounds
     */
    private Map<HorizontalSide, Rectangle> getBounds (Set<Inter> clump)
    {
        Map<HorizontalSide, Rectangle> bounds = new EnumMap<HorizontalSide, Rectangle>(
                HorizontalSide.class);

        for (HorizontalSide side : HorizontalSide.values()) {
            // Take union of areas for this side
            Rectangle box = null;

            for (Inter inter : clump) {
                SlurInter slur = (SlurInter) inter;
                Rectangle b = slur.getInfo().getChordArea(side == LEFT).getBounds();

                if (box == null) {
                    box = b;
                } else {
                    box.add(b);
                }
            }

            bounds.put(side, box);
        }

        return bounds;
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
                2.5,
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

    //-----------//
    // ChordLink //
    //-----------//
    /**
     * Formalizes a relation between a slur end and a (head-based) chord nearby.
     * <p>
     * Rather than chord box center point, we use chord box middle vertical line.
     */
    private static class ChordLink
    {
        //~ Static fields/initializers -------------------------------------------------------------

        public static Comparator<ChordLink> byEuclidean = new Comparator<ChordLink>()
        {
            @Override
            public int compare (ChordLink o1,
                                ChordLink o2)
            {
                return Double.compare(o1.euclidean, o2.euclidean);
            }
        };

        //~ Instance fields ------------------------------------------------------------------------
        /** Chord linked to slur end. */
        public final AbstractChordInter chord;

        /** Euclidean distance from slur end to chord middle vertical. */
        public final double euclidean;

        //~ Constructors ---------------------------------------------------------------------------
        public ChordLink (Point slurEnd,
                          AbstractChordInter chord)
        {
            this.chord = chord;

            // Define middle vertical line of chord box
            Rectangle box = chord.getBounds();
            Line2D vert = new Line2D.Double(
                    box.x + (box.width / 2),
                    box.y,
                    box.x + (box.width / 2),
                    box.y + box.height);
            euclidean = vert.ptSegDist(slurEnd);
        }
    }

    //-------------//
    // ClumpLinker //
    //-------------//
    /**
     * Handles the links to chords for a whole clump of slurs, in the context of a system.
     */
    private class ClumpLinker
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final SystemInfo system;

        private final SIGraph sig;

        private final Set<Inter> clump;

        /** All head-chords in the system. */
        private final List<Inter> sysChords;

        /** Head-chords found around clump. */
        private final Map<HorizontalSide, List<Inter>> chords = new EnumMap<HorizontalSide, List<Inter>>(
                HorizontalSide.class);

        //~ Constructors ---------------------------------------------------------------------------
        public ClumpLinker (SystemInfo system,
                            Set<Inter> clump,
                            Map<HorizontalSide, Rectangle> bounds)
        {
            this.system = system;
            this.clump = clump;
            sig = system.getSig();

            sysChords = sig.inters(new Class[]{HeadChordInter.class, SmallChordInter.class});

            // Pre-select chords candidates according to clump side
            preselect(bounds);
        }

        //~ Methods --------------------------------------------------------------------------------
        //--------------//
        // isSpaceClear //
        //--------------//
        /**
         * Check the space between leftHead and rightHead is clear of other heads.
         * <p>
         * This is meant to allow a tie.
         *
         * @param leftHead  left side head
         * @param rightHead right side head
         * @return true if clear
         */
        public boolean isSpaceClear (AbstractHeadInter leftHead,
                                     AbstractHeadInter rightHead)
        {
            if ((leftHead == null) || (rightHead == null)) {
                return false;
            }

            final AbstractChordInter leftChord = leftHead.getChord();
            final AbstractChordInter rightChord = rightHead.getChord();

            final BeamGroup leftGroup = leftChord.getBeamGroup();
            final BeamGroup rightGroup = rightChord.getBeamGroup();

            // Define a lookup box limited to heads and stems tail ends
            Rectangle box = leftHead.getCoreBounds().getBounds();
            box.add(rightHead.getCoreBounds().getBounds());
            box.add(leftChord.getTailLocation());
            box.add(rightChord.getTailLocation());

            List<Inter> found = SIGraph.intersectedInters(sysChords, null, box);

            // Exclude left & right chords
            found.remove(leftChord);
            found.remove(rightChord);

            // Exclude mirrors if any
            if (leftHead.getMirror() != null) {
                found.remove(leftHead.getMirror().getEnsemble());
            }

            if (rightHead.getMirror() != null) {
                found.remove(rightHead.getMirror().getEnsemble());
            }

            ChordLoop:
            for (Iterator it = found.iterator(); it.hasNext();) {
                AbstractChordInter chord = (AbstractChordInter) it.next();

                // This intersected chord cannot be in the same beam group as left or right chords
                final BeamGroup group = chord.getBeamGroup();

                if ((group != null) && ((group == leftGroup) || (group == rightGroup))) {
                    logger.debug("Tie forbidden across {}", chord);

                    return false;
                }

                //
                //                // Check for heads of intersected chords (not their stem)
                //                for (Inter nInter : chord.getNotes()) {
                //                    if (box.intersects(nInter.getCoreBounds().getBounds())) {
                //                        return false;
                //                    }
                //                }
                //
                //                // For this chord, no head intersects lookup box, so ignore this chord
                //                it.remove();
            }

            //            return found.isEmpty();
            return true;
        }

        /**
         * Select the best slur in clump (within current system)
         *
         * @return the map of slurs best links (for this system)
         */
        public SlurInter process ()
        {
            // Determine the pair of best links for every slur candidate
            Map<SlurInter, Map<HorizontalSide, ChordLink>> map = new HashMap<SlurInter, Map<HorizontalSide, ChordLink>>();

            for (Inter inter : clump) {
                SlurInter slur = (SlurInter) inter;

                // Determine the pair of best links (left & right) for this slur candidate
                Map<HorizontalSide, ChordLink> linkPair = getLinkPair(slur);

                if (linkPair != null) {
                    map.put(slur, linkPair);
                }
            }

            // Make a selection among clump slurs
            // Choose (among the longest ones) the slur with best links
            // Accept orphan only if quorum slurs agree
            // Retrieve non-orphans
            Set<SlurInter> nonOrphans = getNonOrphans(map);
            SlurInter bestSlur = selectAmong(nonOrphans, map);

            if (bestSlur != null) {
                return bestSlur;
            }

            Set<SlurInter> orphans = new HashSet<SlurInter>(map.keySet());
            orphans.removeAll(nonOrphans);

            return selectAmong(orphans, map);
        }

        /**
         * Make sure a chord exhibits at least one head on the desired side on the
         * provided slur.
         *
         * @param chord chord to check
         * @param slur  provided slur
         * @param side  which side
         * @return true if OK
         */
        private boolean chordHasHead (AbstractChordInter chord,
                                      SlurInter slur,
                                      HorizontalSide side)
        {
            SlurInfo info = slur.getInfo();
            CircleModel model = (CircleModel) info.getModel();
            Point2D mid = model.getMidPoint();
            Point2D center = model.getCircle().getCenter();
            int ccw = model.ccw();
            int expected = (side == LEFT) ? ccw : (-ccw);

            for (Inter hInter : chord.getNotes()) {
                if (hInter instanceof AbstractHeadInter) {
                    Point point = hInter.getCenter();
                    int pos = Line2D.relativeCCW(
                            mid.getX(),
                            mid.getY(),
                            center.getX(),
                            center.getY(),
                            point.x,
                            point.y);

                    if (pos == expected) {
                        return true;
                    }
                }
            }

            return false;
        }

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
         * @param slur the slur candidate to check for links
         * @return the pair of links if acceptable (only half-filled for orphan), null if not
         */
        private Map<HorizontalSide, ChordLink> getLinkPair (SlurInter slur)
        {
            // The pair to populate
            final Map<HorizontalSide, ChordLink> linkPair = new EnumMap<HorizontalSide, ChordLink>(
                    HorizontalSide.class);

            // Slur target locations on each side
            final Point leftTarget = getTargetPoint(slur, LEFT);
            final Point rightTarget = getTargetPoint(slur, RIGHT);

            // Chords candidates on each side
            final Map<Inter, ChordLink> lefts = lookup(slur, LEFT);
            final Map<Inter, ChordLink> rights = lookup(slur, RIGHT);

            // The same chord cannot be linked to both slur ends
            // Keep it only where it is closer to slur target
            Set<Inter> commons = new HashSet<Inter>();
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
            Map<HorizontalSide, ChordLink> mirrors = new EnumMap<HorizontalSide, ChordLink>(
                    HorizontalSide.class);

            for (HorizontalSide side : HorizontalSide.values()) {
                Map<Inter, ChordLink> links = (side == LEFT) ? lefts : rights;
                List<ChordLink> list = new ArrayList<ChordLink>(links.values());

                if (!list.isEmpty()) {
                    Collections.sort(list, ChordLink.byEuclidean);

                    ChordLink best = list.get(0);

                    // Mirror?
                    Inter mirror = best.chord.getMirror();

                    if ((mirror != null) && links.keySet().contains(mirror)) {
                        mirrors.put(side, best); // We have a conflict to solve
                    } else {
                        linkPair.put(side, best); // The best link is OK
                    }
                }
            }

            // Process mirrors conflicts if any
            if (!mirrors.isEmpty()) {
                for (Entry<HorizontalSide, ChordLink> entry : mirrors.entrySet()) {
                    // This side of the slur
                    final HorizontalSide side = entry.getKey();
                    final ChordLink link = entry.getValue();
                    final Map<Inter, ChordLink> links = (side == LEFT) ? lefts : rights;
                    final ChordLink mirrorLink = links.get(link.chord.getMirror());
                    final boolean linkOk;

                    // The other side of the slur
                    final HorizontalSide otherSide = side.opposite();
                    final ChordLink otherLink = linkPair.get(otherSide);

                    if (otherLink != null) {
                        // Compare with other side of the slur
                        final Staff otherStaff = otherLink.chord.getTopStaff();
                        final Staff staff = link.chord.getTopStaff();
                        final int dir = link.chord.getStemDir();

                        if (staff != otherStaff) {
                            // Select mirror according to direction to other staff
                            linkOk = (dir * Staff.byId.compare(otherStaff, staff)) > 0;
                        } else {
                            // Select mirror with same stem dir as at other slur end
                            // (This may be called into question later when looking for ties)
                            int otherDir = otherLink.chord.getStemDir();
                            linkOk = dir == otherDir;
                        }
                    } else {
                        // No link found on other side (orphan? or slur truncated?)
                        // Not too stupid: select the mirror with less beams
                        Inter stem = link.chord.getStem();
                        int nb = sig.getRelations(stem, BeamStemRelation.class).size();
                        Inter mStem = mirrorLink.chord.getStem();
                        int mNb = sig.getRelations(mStem, BeamStemRelation.class).size();
                        linkOk = nb <= mNb;
                    }

                    linkPair.put(side, linkOk ? link : mirrorLink);
                }
            }

            // Check we don't have a chord on one slur side and its mirror on the other side
            ChordLink leftLink = linkPair.get(LEFT);
            ChordLink rightLink = linkPair.get(RIGHT);

            if ((leftLink != null) && (rightLink != null)) {
                Inter leftMirror = leftLink.chord.getMirror();

                if ((leftMirror != null) && (leftMirror == rightLink.chord)) {
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
                    return null;
                }
            }

            return linkPair;
        }

        /**
         * Select the slurs that are not orphan.
         *
         * @param map the connection map to browse
         * @return the set of slurs which have links on both sides
         */
        private Set<SlurInter> getNonOrphans (Map<SlurInter, Map<HorizontalSide, ChordLink>> map)
        {
            Set<SlurInter> nonOrphans = new HashSet<SlurInter>();
            EntryLoop:
            for (Entry<SlurInter, Map<HorizontalSide, ChordLink>> entry : map.entrySet()) {
                Map<HorizontalSide, ChordLink> m = entry.getValue();

                for (HorizontalSide side : HorizontalSide.values()) {
                    if (m.get(side) == null) {
                        continue EntryLoop;
                    }
                }

                nonOrphans.add(entry.getKey());
            }

            return nonOrphans;
        }

        //----------------//
        // getTargetPoint //
        //----------------//
        private Point getTargetPoint (SlurInter slur,
                                      HorizontalSide side)
        {
            final boolean rev = side == LEFT;
            final SlurInfo info = slur.getInfo();
            Point target = info.getTargetPoint(rev);

            if (target == null) {
                final Point end = info.getEnd(rev);
                final Point2D vector = info.getSideModel(rev).getEndVector(rev);
                final double ext = params.targetExtension;
                target = PointUtil.rounded(PointUtil.addition(end, PointUtil.times(vector, ext)));
                info.setTargetPoint(rev, target);
                slur.addAttachment(rev ? "t1" : "t2", new Line2D.Double(end, target));
            }

            return target;
        }

        //------//
        // link //
        //------//
        /**
         * Insert relations between slur and linked chord.
         *
         * @param slur      chosen slur
         * @param leftLink  link to left chord, if any
         * @param rightLink link to right chord, if any
         */
        private void link (SlurInter slur,
                           ChordLink leftLink,
                           ChordLink rightLink)
        {
            sig.addVertex(slur);

            SlurInfo info = slur.getInfo();

            for (HorizontalSide side : HorizontalSide.values()) {
                ChordLink link = (side == LEFT) ? leftLink : rightLink;

                if (link != null) {
                    Point end = info.getEnd(side == LEFT);
                    Point target = info.getTargetPoint(side == LEFT);
                    Inter head = selectBestHead(link.chord, end, target, info.getBisUnit());

                    if (head != null) {
                        sig.addEdge(slur, head, new SlurHeadRelation(side));
                    }
                }
            }
        }

        /**
         * Retrieve the chord(s) embraced by the slur side.
         *
         * @param slur the provided slur
         * @param side desired side
         * @return the map of chords found, perhaps empty, with their data
         */
        private Map<Inter, ChordLink> lookup (SlurInter slur,
                                              HorizontalSide side)
        {
            Map<Inter, ChordLink> found = new HashMap<Inter, ChordLink>();
            SlurInfo info = slur.getInfo();
            Area area = info.getChordArea(side == LEFT);
            Point slurTarget = info.getTargetPoint(side == LEFT);

            // Look for intersected chords
            for (Inter chord : chords.get(side)) {
                Rectangle chordBox = chord.getBounds();

                if (area.intersects(chordBox)) {
                    // Check the chord contains at least one suitable head on desired slur side
                    if (chordHasHead((AbstractChordInter) chord, slur, side)) {
                        found.put(chord, new ChordLink(slurTarget, (AbstractChordInter) chord));
                    }
                }
            }

            return found;
        }

        /**
         * Compute the mean euclidian-distance on the ChordLinks
         *
         * @param map the chord links of a slur
         * @return means euclidian distance
         */
        private double meanEuclidianDist (Map<HorizontalSide, ChordLink> map)
        {
            double dist = 0;
            int n = 0;

            for (HorizontalSide side : HorizontalSide.values()) {
                ChordLink link = map.get(side);

                if (link != null) {
                    dist += link.euclidean;
                    n++;
                }
            }

            return dist / n;
        }

        /**
         * Filter the chords that could be relevant for clump.
         */
        private void preselect (Map<HorizontalSide, Rectangle> bounds)
        {
            for (HorizontalSide side : HorizontalSide.values()) {
                Rectangle box = bounds.get(side);

                // Filter via box intersection
                chords.put(side, SIGraph.intersectedInters(sysChords, null, box));
            }
        }

        //-------------//
        // selectAmong //
        //-------------//
        /**
         * Make a selection among the competing slurs.
         * None or all of those slurs are orphans (and on the same side).
         * <p>
         * First, sort the slurs by increasing mean X-distance to their heads.
         * Second, among the first ones that share the same heads, select the one with shortest
         * mean Euclidean-distance.
         * (TODO: this 2nd point is very questionable, we could use slur grade as well).
         *
         * @param inters the competitors
         * @param map    their links to notes
         * @return the best selected
         */
        private SlurInter selectAmong (Set<SlurInter> inters,
                                       final Map<SlurInter, Map<HorizontalSide, ChordLink>> map)
        {
            if ((inters == null) || inters.isEmpty()) {
                return null;
            } else if (inters.size() == 1) {
                SlurInter slur = inters.iterator().next();
                Map<HorizontalSide, ChordLink> m = map.get(slur);
                link(slur, m.get(LEFT), m.get(RIGHT));

                return slur;
            }

            ///logger.info("selectAmong {}", toFullString(inters));
            List<SlurInter> list = new ArrayList<SlurInter>(inters);

            // Sort by mean euclidian distance
            // TODO: this may be too rigid!
            Collections.sort(
                    list,
                    new Comparator<SlurInter>()
            {
                @Override
                public int compare (SlurInter s1,
                                    SlurInter s2)
                {
                    return Double.compare(
                            meanEuclidianDist(map.get(s1)),
                            meanEuclidianDist(map.get(s2)));
                }
            });

            // Now, select between slurs with same embraced heads, if any
            SlurInter bestSlur = null;
            double bestDist = Double.MAX_VALUE;
            ChordLink bestLeft = null;
            ChordLink bestRight = null;

            for (SlurInter slur : list) {
                Map<HorizontalSide, ChordLink> m = map.get(slur);

                if (bestSlur == null) {
                    bestSlur = slur;
                    bestLeft = m.get(LEFT);
                    bestRight = m.get(RIGHT);
                    bestDist = meanEuclidianDist(m);
                    logger.debug("   {} {} euclide:{}", slur, slur.getInfo(), bestDist);
                } else {
                    // Check whether embraced chords are still the same
                    ChordLink left = m.get(LEFT);

                    if ((bestLeft != null) && (left != null) && (left.chord != bestLeft.chord)) {
                        break;
                    }

                    ChordLink right = m.get(RIGHT);

                    if ((bestRight != null) && (right != null) && (right.chord != bestRight.chord)) {
                        break;
                    }

                    // We do have the same embraced chords as slurs above, so use Euclidian distance
                    double dist = meanEuclidianDist(m);
                    logger.debug("   {} {} euclide:{}", slur, slur.getInfo(), dist);

                    if (dist < bestDist) {
                        bestDist = dist;
                        bestSlur = slur;
                    }
                }
            }

            // Formalize relationships between slur and notes
            if (bestSlur != null) {
                link(bestSlur, bestLeft, bestRight);
            }

            return bestSlur;
        }

        /**
         * Select the best note head in the selected head-based chord.
         * We select the note head which is closest to slur target end.
         *
         * @param chord   the selected chord
         * @param end     the slur end point
         * @param target  the slur target point
         * @param bisUnit unity vector pointing to slur center
         * @return the best note head or null
         */
        private Inter selectBestHead (AbstractChordInter chord,
                                      Point end,
                                      Point target,
                                      Point2D bisUnit)
        {
            double bestDist = Double.MAX_VALUE;
            Inter bestHead = null;

            for (Inter head : chord.getNotes()) {
                Point center = head.getCenter();

                // Check relative position WRT slur
                if (dotProduct(subtraction(center, end), bisUnit) > 0) {
                    double dist = center.distance(target);

                    if (dist < bestDist) {
                        bestDist = dist;
                        bestHead = head;
                    }
                }
            }

            return bestHead;
        }
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
