//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S l u r s L i n k e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.skeleton;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.math.GeoPath;
import omr.math.GeoUtil;
import omr.math.LineUtil;
import static omr.math.LineUtil.*;
import static omr.math.PointUtil.*;

import omr.sig.AbstractNoteInter;
import omr.sig.HeadStemRelation;
import omr.sig.Inter;
import omr.sig.Relation;
import omr.sig.SIGraph;
import omr.sig.SlurInter;
import omr.sig.StemInter;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import static java.lang.Math.abs;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.SystemManager;

/**
 * Class {@code SlursLinker} works at sheet level, to handle the connections between
 * slurs and embraced notes.
 * <p>
 * Its primary feature is to help select the best slur in a clump of slurs, by evaluating the
 * connections to embraced notes.
 *
 * @author Hervé Bitteur
 */
public class SlursLinker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SlursLinker.class);

    //~ Instance fields ----------------------------------------------------------------------------
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
     * Prune a clump of slurs and select the candidate with best
     * average distance to embraced notes.
     *
     * @param clump a bunch of aggregated slurs
     * @return the best slur, if any
     */
    public SlurInter prune (Set<SlurInter> clump)
    {
        // Compute lookup areas for each slur in clump
        for (SlurInter slur : clump) {
            defineAreaPair(slur);
        }

        // Define global clump bounds
        Map<HorizontalSide, Rectangle> bounds = getBounds(clump);
        Rectangle clumpBox = new Rectangle(bounds.get(LEFT));
        clumpBox.add(bounds.get(RIGHT));

        // Determine the impacted system(s)
        //TODO: should we keep both system results?
        SystemManager mgr = sheet.getSystemManager();
        List<SystemInfo> systems = mgr.getSystemsOf(clumpBox, null);

        for (SystemInfo system : systems) {
            ClumpLinker linker = new ClumpLinker(system, clump, bounds);

            // Select the slur with best notes links, if any
            SlurInter selected = linker.process();

            if (selected != null) {
                system.getSig().addVertex(selected);

                return selected;
            }
        }

        return null;
    }

    //----------------//
    // defineAreaPair //
    //----------------//
    /**
     * Define the pair of look-up areas for a slur candidate,
     * one on first end and another on last end.
     * The strategy to define look-up areas differs between "horizontal"
     * and "vertical" slurs.
     */
    private void defineAreaPair (SlurInter slur)
    {
        SlurInfo info = slur.getInfo();
        Point first = info.getEnd(true);
        Point last = info.getEnd(false);
        int vDir = info.above();
        int hDir = Integer.signum(last.x - first.x);
        Point2D mid = new Point2D.Double((first.x + last.x) / 2d, (first.y + last.y) / 2d);
        Line2D bisector = (vDir == hDir) ? bisector(first, last) : bisector(last, first);
        Point2D firstExt;
        Point2D lastExt;
        GeoPath firstPath;
        GeoPath lastPath;
        Line2D baseLine;
        Point2D firstBase;
        Point2D lastBase;

        // Qualify the slur as horizontal or vertical
        if ((abs(LineUtil.getSlope(first, last)) <= params.slopeSeparator)
            || (abs(first.getX() - last.getX()) >= params.wideSlurWidth)) {
            // Horizontal: Use base parallel to slur
            info.setHorizontal(true);
            firstExt = extension(mid, first, params.coverageHExt);
            lastExt = extension(mid, last, params.coverageHExt);
            firstPath = new GeoPath(new Line2D.Double(mid, firstExt));
            lastPath = new GeoPath(new Line2D.Double(mid, lastExt));
            firstBase = new Point2D.Double(
                    firstExt.getX(),
                    firstExt.getY() + (vDir * params.coverageHDepth));
            lastBase = new Point2D.Double(
                    lastExt.getX(),
                    lastExt.getY() + (vDir * params.coverageHDepth));
            baseLine = new Line2D.Double(firstBase, lastBase);

            if (abs(last.x - first.x) > (2 * params.coverageHIn)) {
                // Wide slur: separate first & last areas
                double firstInX = first.x + (hDir * params.coverageHIn);
                Point2D firstIn = intersectionAtX(first, last, firstInX);
                firstPath = new GeoPath(new Line2D.Double(firstIn, firstExt));
                firstPath.append(
                        new Line2D.Double(firstBase, intersectionAtX(baseLine, firstInX)),
                        true);

                double lastInX = last.x - (hDir * params.coverageHIn);
                Point2D lastIn = intersectionAtX(first, last, lastInX);
                lastPath = new GeoPath(new Line2D.Double(lastIn, lastExt));
                lastPath.append(
                        new Line2D.Double(lastBase, intersectionAtX(baseLine, lastInX)),
                        true);
            } else {
                // Narrow slur: just one vertical separation
                Point2D midInter = intersectionAtX(baseLine, mid.getX());
                firstPath.append(new Line2D.Double(firstBase, midInter), true);
                lastPath.append(new Line2D.Double(lastBase, midInter), true);
            }
        } else {
            // Vertical: Use slanted separation
            info.setHorizontal(false);
            firstExt = extension(mid, first, params.coverageVExt);
            lastExt = extension(mid, last, params.coverageVExt);

            Point2D bisUnit = info.getBisUnit();
            Point2D depth = new Point2D.Double(
                    params.coverageVDepth * bisUnit.getX(),
                    params.coverageVDepth * bisUnit.getY());
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
    private Map<HorizontalSide, Rectangle> getBounds (Set<SlurInter> clump)
    {
        Map<HorizontalSide, Rectangle> bounds = new EnumMap<HorizontalSide, Rectangle>(
                HorizontalSide.class);

        for (HorizontalSide side : HorizontalSide.values()) {
            // Take union of areas for this side
            Rectangle box = null;

            for (SlurInter slur : clump) {
                Rectangle b = slur.getInfo().getArea(side).getBounds();

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
    //----------//
    // NoteLink //
    //----------//
    public static class NoteLink
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final Inter note; // Note linked to slur end

        public final Double distance; // Distance between note & slur end

        public final boolean direct; // False if via stem

        //~ Constructors ---------------------------------------------------------------------------
        public NoteLink (Inter note,
                         Double distance,
                         boolean direct)
        {
            this.note = note;
            this.distance = distance;
            this.direct = direct;
        }
    }

    //-------------//
    // ClumpLinker //
    //-------------//
    /**
     * Handles the links to notes for a whole clump of slurs, in the
     * context of one system.
     */
    private class ClumpLinker
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final SystemInfo system;

        private final SIGraph sig;

        private final Set<SlurInter> clump;

        private final Map<HorizontalSide, List<Inter>> notes = new EnumMap<HorizontalSide, List<Inter>>(
                HorizontalSide.class);

        private final Map<HorizontalSide, List<Inter>> stems = new EnumMap<HorizontalSide, List<Inter>>(
                HorizontalSide.class);

        //~ Constructors ---------------------------------------------------------------------------
        public ClumpLinker (SystemInfo system,
                            Set<SlurInter> clump,
                            Map<HorizontalSide, Rectangle> bounds)
        {
            this.system = system;
            this.clump = clump;
            sig = system.getSig();

            // Pre-select notes & stems according to clump side
            preselect(bounds);
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Select the best slur in clump (within current system)
         *
         * @return the map of slurs best links (for this system)
         */
        public SlurInter process ()
        {
            Map<SlurInter, Map<HorizontalSide, NoteLink>> map = new HashMap<SlurInter, Map<HorizontalSide, NoteLink>>();

            for (Inter inter : clump) {
                SlurInter slur = (SlurInter) inter;
                Map<Inter, NoteLink> lefts = lookup(slur, LEFT);
                Map<Inter, NoteLink> rights = lookup(slur, RIGHT);

                // Check this slur for embraced notes
                if (checkNotes(slur, lefts, rights)) {
                    // Record best link on left & best link on right
                    Map<HorizontalSide, NoteLink> links = new EnumMap<HorizontalSide, NoteLink>(
                            HorizontalSide.class);
                    map.put(slur, links);

                    for (HorizontalSide side : HorizontalSide.values()) {
                        Map<Inter, NoteLink> m = (side == LEFT) ? lefts : rights;
                        Entry<Inter, NoteLink> best = null;

                        for (Entry<Inter, NoteLink> entry : m.entrySet()) {
                            if (best == null) {
                                best = entry;
                            } else if (best.getValue().distance > entry.getValue().distance) {
                                best = entry;
                            }
                        }

                        if (best != null) {
                            links.put(side, best.getValue());
                        }
                    }
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
         * Look precisely at contents of lookup areas.
         * Typically a same note cannot belong to both areas.
         * This applies to stems as well: stem heads belong to the stem chord.
         * If the same stem "appears" on both side, it is discarded.
         * <p>
         * Possible orphan slurs are accepted but flagged as such
         *
         * @param slur   the slur to purge
         * @param lefts  items in left area
         * @param rights items in right area
         * @return true if slur is acceptable, false if not
         */
        private boolean checkNotes (SlurInter slur,
                                    Map<Inter, NoteLink> lefts,
                                    Map<Inter, NoteLink> rights)
        {
            // Notes in common are discarded
            Set<Inter> commons = new HashSet<Inter>(lefts.keySet());
            commons.retainAll(rights.keySet());

            for (Inter common : commons) {
                // Note may be direct on a side and via stem for the other
                // In that case, keep the direct & discard the stem-based
                boolean leftDirect = lefts.get(common).direct;

                if (!leftDirect) {
                    lefts.remove(common);
                }

                boolean rightDirect = rights.get(common).direct;

                if (!rightDirect) {
                    rights.remove(common);
                }
            }

            // Check left & right don't share notes on the same stem/chord
            commons.clear();

            for (Inter nil : lefts.keySet()) {
                for (Inter si : stemsOf(Collections.singleton(nil))) {
                    for (Inter nir : rights.keySet()) {
                        if (sig.getRelation(nir, si, HeadStemRelation.class) != null) {
                            commons.add(nir);
                            commons.add(nil);
                        }
                    }
                }
            }

            for (Inter common : commons) {
                lefts.remove(common);
                rights.remove(common);
            }

            if (lefts.isEmpty() && rights.isEmpty()) {
                return false;
            }

            SlurInfo info = slur.getInfo();

            // Can slur be an orphan?
            for (HorizontalSide side : HorizontalSide.values()) {
                Map<Inter, NoteLink> map = (side == LEFT) ? lefts : rights;

                if (map.isEmpty()) {
                    // Check if slur is rather horizontal
                    if (abs(LineUtil.getSlope(info.getEnd(true), info.getEnd(false))) > params.maxOrphanSlope) {
                        logger.debug("{} too sloped orphan", slur);

                        return false;
                    }

                    // Check horizontal gap to end of staff
                    Point slurEnd = info.getPoint(side);
                    StaffInfo staff = system.getStaffAt(slurEnd);
                    int staffEnd = (side == LEFT) ? staff.getDmzEnd() : staff.getAbscissa(
                            side);

                    if (abs(slurEnd.x - staffEnd) > params.maxOrphanDx) {
                        logger.debug("{} too far orphan", slur);

                        return false;
                    }

                    // Here we have a possible orphan side, let it live
                }
            }

            return true;
        }

        /**
         * Select the slurs that are not orphan.
         *
         * @param map the connection map to browse
         * @return the set of slurs which have links on both sides
         */
        private Set<SlurInter> getNonOrphans (Map<SlurInter, Map<HorizontalSide, NoteLink>> map)
        {
            Set<SlurInter> nonOrphans = new HashSet<SlurInter>();
            EntryLoop:
            for (Entry<SlurInter, Map<HorizontalSide, NoteLink>> entry : map.entrySet()) {
                Map<HorizontalSide, NoteLink> m = entry.getValue();

                for (HorizontalSide side : HorizontalSide.values()) {
                    if (m.get(side) == null) {
                        continue EntryLoop;
                    }
                }

                nonOrphans.add(entry.getKey());
            }

            return nonOrphans;
        }

        /**
         * Retrieve the note(s) embraced by the slur side.
         *
         * @param slur the provided slur
         * @param side desired side
         * @return the map of notes found, perhaps empty, with their data
         */
        private Map<Inter, NoteLink> lookup (SlurInter slur,
                                             HorizontalSide side)
        {
            Map<Inter, NoteLink> found = new HashMap<Inter, NoteLink>();
            SlurInfo info = slur.getInfo();
            Point2D bisUnit = info.getBisUnit();
            Area area = info.getArea(side);
            Point slurEnd = info.getPoint(side);

            // Look for notes center contained directly
            for (Inter in : notes.get(side)) {
                Rectangle noteBox = in.getBounds();

                if (area.contains(GeoUtil.centerOf(noteBox))) {
                    Point center = GeoUtil.centerOf(in.getBounds());
                    double distSq = slurEnd.distanceSq(center);
                    found.put(in, new NoteLink(in, distSq, true));
                }
            }

            if (info.isHorizontal()) {
                // Determine the stems related to notes found
                Set<Inter> relatedStems = stemsOf(found.keySet());

                // Look for stems intersected (if not related to any note found)
                for (Inter is : stems.get(side)) {
                    if (!relatedStems.contains(is)) {
                        Glyph glyph = is.getGlyph();

                        if (area.intersects(glyph.getBounds())) {
                            // Find out the best note on the stem
                            NoteLink noteLink = pickupStemNote(is, slurEnd, bisUnit);

                            if (noteLink != null) {
                                NoteLink direct = found.get(noteLink.note);

                                if (direct == null) {
                                    found.put(noteLink.note, noteLink);
                                }
                            }
                        }
                    }
                }
            }

            return found;
        }

        /**
         * Select the best note designated by the intersected stem.
         * We select the note which is closest to slur end.
         *
         * @param stem    the intersected stem
         * @param slurEnd the slur ending point
         * @param bisUnit unity vector pointing to slur center
         * @return the best note or null
         */
        private NoteLink pickupStemNote (Inter stem,
                                         Point slurEnd,
                                         Point2D bisUnit)
        {
            Set<Relation> hsRels = sig.getRelations(stem, HeadStemRelation.class);
            Inter bestNote = null;
            double bestDistSq = Double.MAX_VALUE;

            for (Relation rel : hsRels) {
                Inter note = (AbstractNoteInter) sig.getEdgeSource(rel);
                Point center = GeoUtil.centerOf(note.getBounds());

                // Check relative position WRT slur end
                if (dotProduct(subtraction(center, slurEnd), bisUnit) > 0) {
                    double distSq = slurEnd.distanceSq(center);

                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        bestNote = note;
                    }
                }
            }

            if (bestNote != null) {
                return new NoteLink(bestNote, bestDistSq, false);
            } else {
                return null;
            }
        }

        /**
         * Filter the notes and stems that could be relevant for clump.
         */
        private void preselect (Map<HorizontalSide, Rectangle> bounds)
        {
            List<Inter> sysNotes = sig.inters(AbstractNoteInter.class);
            List<Inter> sysStems = sig.inters(StemInter.class);

            for (HorizontalSide side : HorizontalSide.values()) {
                Rectangle box = bounds.get(side);

                // Filter via box intersection
                notes.put(side, sig.intersectedInters(sysNotes, null, box));
                stems.put(side, sig.intersectedInters(sysStems, null, box));
            }
        }

        //-------------//
        // selectAmong //
        //-------------//
        private SlurInter selectAmong (Set<SlurInter> inters,
                                       Map<SlurInter, Map<HorizontalSide, NoteLink>> map)
        {
            // None or all are orphans (and on the same side)
            SlurInter bestSlur = null;
            double bestDist = Double.MAX_VALUE;

            for (SlurInter inter : inters) {
                Map<HorizontalSide, NoteLink> m = map.get(inter);
                double slurDist = 0;

                for (HorizontalSide side : HorizontalSide.values()) {
                    NoteLink link = m.get(side);

                    if (link != null) {
                        slurDist += link.distance;
                    }
                }

                if (bestDist > slurDist) {
                    bestDist = slurDist;
                    bestSlur = inter;
                }
            }

            return bestSlur;
        }

        /**
         * Retrieve the set of stems that relate to the provided notes.
         *
         * @param notes
         * @return the set of related stems
         */
        private Set<Inter> stemsOf (Collection<Inter> notes)
        {
            Set<Inter> related = new HashSet<Inter>();

            for (Inter ni : notes) {
                Set<Relation> hsRels = sig.getRelations(ni, HeadStemRelation.class);

                for (Relation rel : hsRels) {
                    StemInter si = (StemInter) sig.getEdgeTarget(rel);
                    related.add(si);
                }
            }

            return related;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Scale.Fraction coverageHExt = new Scale.Fraction(
                2.0,
                "Length of extension for horizontal slur coverage");

        final Scale.Fraction coverageHIn = new Scale.Fraction(
                1.5,
                "Internal abscissa of horizontal slur coverage");

        final Scale.Fraction coverageHDepth = new Scale.Fraction(
                4.0,
                "Vertical extension of horizontal slur coverage");

        final Scale.Fraction coverageVExt = new Scale.Fraction(
                2.0,
                "Length of extension for vertical slur coverage");

        final Scale.Fraction coverageVIn = new Scale.Fraction(
                1.5,
                "Internal abscissa of vertical slur coverage");

        final Scale.Fraction coverageVDepth = new Scale.Fraction(
                2.0,
                "Vertical extension of vertical slur coverage");

        final Constant.Double slopeSeparator = new Constant.Double(
                "tangent",
                0.5,
                "Slope that separates vertical slurs from horizontal slurs");

        final Constant.Double maxOrphanSlope = new Constant.Double(
                "tangent",
                0.5,
                "Maximum slope for an orphan slur");

        final Scale.Fraction maxOrphanDx = new Scale.Fraction(
                5.0,
                "Maximum dx to staff end for an orphan slur");

        final Scale.Fraction wideSlurWidth = new Scale.Fraction(
                6.0,
                "Minimum width to be a wide slur");
    }

    ///Map<SlurInfo, Map<HorizontalSide, NoteLink>> connections;
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

        final double slopeSeparator;

        final double maxOrphanSlope;

        final int maxOrphanDx;

        final int wideSlurWidth;

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
            slopeSeparator = constants.slopeSeparator.getValue();
            maxOrphanSlope = constants.maxOrphanSlope.getValue();
            maxOrphanDx = scale.toPixels(constants.maxOrphanDx);
            wideSlurWidth = scale.toPixels(constants.wideSlurWidth);

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }
}
