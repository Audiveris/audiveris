//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B e a m L i n k e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sheet.stem;

import java.awt.Point;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.dynamic.SectionCompound;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.lag.Sections;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Profiles;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.stem.BeamLinker.BLinker;
import org.audiveris.omr.sheet.stem.BeamLinker.BLinker.VLinker;
import org.audiveris.omr.sheet.stem.HeadLinker.SLinker;
import org.audiveris.omr.sheet.stem.HeadLinker.SLinker.CLinker;
import static org.audiveris.omr.sheet.stem.StemHalfLinker.updateStemLine;
import org.audiveris.omr.sheet.stem.StemItem.GapItem;
import org.audiveris.omr.sheet.stem.StemItem.LinkerItem;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.StemPortion;
import org.audiveris.omr.sig.relation.Relation;
import static org.audiveris.omr.sig.relation.StemPortion.*;
import org.audiveris.omr.sig.relation.Support;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class <code>BeamLinker</code> handles the connections from a beam to the nearby
 * stems and heads.
 * <p>
 * For every beam, we look for connectable stem seeds and for stumps that point outside beam.
 *
 * @author Hervé Bitteur
 */
public class BeamLinker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BeamLinker.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The beam being processed. */
    @Navigable(false)
    private final AbstractBeamInter beam;

    /** The containing beam group. */
    private final BeamGroupInter beamGroup;

    /** Beam median line. */
    private final Line2D median;

    /** Beam bounding box. */
    private final Rectangle beamBox;

    /** All stems seeds in beam vicinity. */
    private final Set<Glyph> neighborSeeds;

    /** All detected stumps on beam, including side stumps if any. */
    private final List<Glyph> stumps = new ArrayList<>();

    /** Map of side stumps. */
    private final Map<HorizontalSide, Glyph> sideStumps = new EnumMap<>(HorizontalSide.class);

    /** List of all BLinker instances. */
    private final List<BLinker> allBLinkers = new ArrayList<>();

    /** Map of side-based BLinkers. */
    private final Map<HorizontalSide, BLinker> sideBLinkers
            = new EnumMap<>(HorizontalSide.class);

    /** List of stump-based linkers. */
    private final List<VLinker> stumpLinkers = new ArrayList<>();

    // System-level information
    // ------------------------
    @Navigable(false)
    private final StemsRetriever retriever;

    @Navigable(false)
    private final SystemInfo system;

    @Navigable(false)
    private final Scale scale;

    private final StemsRetriever.Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>BeamLinker</code> object and populates beam stumps.
     *
     * @param beam      the beam inter to link
     * @param retriever the driving system-level StemsRetriever instance
     */
    public BeamLinker (AbstractBeamInter beam,
                       StemsRetriever retriever)
    {
        this.beam = beam;
        this.median = beam.getMedian();
        this.retriever = retriever;

        beamGroup = beam.getGroup();
        beamBox = beam.getBounds();

        system = beam.getSig().getSystem();
        scale = system.getSheet().getScale();
        params = retriever.getParams();

        // Pre-populate seeds and stumps
        neighborSeeds = retriever.getNeighboringSeeds(beamBox);
        stumps.addAll(retrieveStumps());

        // Allocate needed BLinkers and VLinkers
        equipStumps();
        equipOrphanSides();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // equipOrphanSides //
    //------------------//
    /**
     * For each beam horizontal side without usable stump, equip beam with
     * one VLinker above and one VLinker below.
     */
    private void equipOrphanSides ()
    {
        if (beam.isVip()) {
            logger.info("VIP {} equipOrphanSides", this);
        }

        for (HorizontalSide hSide : HorizontalSide.values()) {
            if (sideBLinkers.get(hSide) == null) {
                final Point2D endPt = (hSide == LEFT) ? median.getP1() : median.getP2();
                final List<AbstractBeamInter> siblings = getSiblingBeamsAt(endPt);
                final AbstractBeamInter b1 = siblings.get(0);
                final AbstractBeamInter b2 = siblings.get(siblings.size() - 1);

                // NOTA: we test beam glyph to cope with beam & hook on same glyph
                if ((beam.getGlyph() != b1.getGlyph()) && (beam.getGlyph() != b2.getGlyph())) {
                    continue; // Beam inside beam group
                }

                final BLinker bLinker = new BLinker(null, hSide, null, false);
                sideBLinkers.put(hSide, bLinker);

                for (VerticalSide vSide : VerticalSide.values()) {
                    bLinker.vLinkers.put(vSide, bLinker.new VLinker(vSide.direction()));
                }
            }
        }
    }

    //-------------//
    // equipStumps //
    //-------------//
    /**
     * Equip each beam-attached stump with a dedicated VLinker.
     * <p>
     * Check beam stumps, they give direction to staff which is useful when beam can be processed by
     * two systems.
     */
    private void equipStumps ()
    {
        for (Glyph stump : stumps) {
            if (stump.isVip()) {
                logger.info("VIP {} equipStump at {}", this, stump);
            }

            // Is this stump located on beam side?
            HorizontalSide hSide = null;

            for (Entry<HorizontalSide, Glyph> entry : sideStumps.entrySet()) {
                if (entry.getValue() == stump) {
                    hSide = entry.getKey();
                }
            }

            final BLinker bLinker = new BLinker(stump, hSide, null, false);

            // Retrieve vertical directions when going away from beam
            final Set<VerticalSide> directions = getStumpDirections(stump);

            if (directions != null) {
                for (VerticalSide vSide : directions) {
                    final VLinker vLinker = bLinker.new VLinker(vSide.direction());
                    bLinker.vLinkers.put(vSide, vLinker);
                    stumpLinkers.add(vLinker);
                }
            }
        }
    }

    //------------//
    // findLinker //
    //------------//
    /**
     * Find out or build a beam linker where the provided stem line hits the beam.
     *
     *
     * @param sLine stem (theoretical?) line
     * @return the related VLinker instance
     */
    public BLinker findLinker (Line2D sLine)
    {
        // Cross point
        final Point2D refPt = LineUtil.intersection(sLine, median);
        final double x0 = refPt.getX();

        // Check with existing linkers
        BLinker bestLinker = null;
        double bestDx = Double.MAX_VALUE;

        for (BLinker linker : allBLinkers) {
            final double dx = Math.abs(linker.refPt.getX() - x0);

            if (bestDx > dx) {
                bestDx = dx;
                bestLinker = linker;
            }
        }

        if (bestDx <= params.maxBeamLinkerDx) {
            return bestLinker;
        }

        // We have to build a brand new (anchord) linker at xp
        return new BLinker(null, /* hSide? */ null, refPt, true);
    }

    //-------------------//
    // getSiblingBeamsAt //
    //-------------------//
    /**
     * Report the top down sequence of beams in current beam group that intersect the
     * vertical at provided point.
     *
     * @param pt provided point
     * @return the sequence of relevant beam group members
     */
    public List<AbstractBeamInter> getSiblingBeamsAt (Point2D pt)
    {
        final Line2D vertical = system.getSheet().getSkew().skewedVertical(pt);
        final List<Inter> members = beamGroup.getMembers();
        final List<AbstractBeamInter> beams = new ArrayList<>();
        final int margin = params.maxBeamSideDx;

        for (Inter inter : members) {
            final AbstractBeamInter b = (AbstractBeamInter) inter;
            final Line2D m = b.getMedian();
            final Point2D xp = LineUtil.intersection(vertical, m);

            if ((m.getX1() - margin <= xp.getX()) && (xp.getX() <= m.getX2() + margin)) {
                beams.add(b);
            }
        }

        // Sort beams top down along the vertical line
        Collections.sort(beams, (b1, b2)
                         -> Double.compare(LineUtil.intersection(vertical, b1.getMedian()).getY(),
                                           LineUtil.intersection(vertical, b2.getMedian()).getY()));
        return beams;
    }

    //-----------------//
    // inspectVLinkers //
    //-----------------//
    public void inspectVLinkers ()
    {
        for (BLinker bLinker : allBLinkers) {
            if (!bLinker.isAnchor) {
                // Maximum possible stemProfile
                final int stemProfile = (bLinker.hSide != null)
                        ? Profiles.BEAM_SIDE
                        : Profiles.BEAM_SEED;

                for (VLinker vLinker : bLinker.vLinkers.values()) {
                    vLinker.inspect(stemProfile);
                }
            }
        }
    }

    //-----------//
    // linkSides //
    //-----------//
    /**
     * Link beam on each horizontal side.
     *
     * @param linkProfile desired profile level for links
     * @return false to delete beam
     */
    public boolean linkSides (int linkProfile)
    {
        if (beam.isVip()) {
            logger.info("VIP {} linkSides", this);
        }

        final BeamHookInter oppoHook = beam.getCompetingHook();
        final EnumSet<HorizontalSide> linkedSides = EnumSet.noneOf(HorizontalSide.class);

        for (HorizontalSide hSide : HorizontalSide.values()) {
            final BLinker bLinker = sideBLinkers.get(hSide);

            if (bLinker == null) {
                logger.info("No BLinker on {} of {}", hSide, this);
                continue;
            }

            if (bLinker.isLinked()) {
                linkedSides.add(hSide);
            } else {
                final int stemProfile = (beam.isHook() || (oppoHook != null))
                        ? linkProfile
                        : Profiles.BEAM_SIDE;
                final boolean ok = bLinker.link(stemProfile, linkProfile);

                if (ok) {
                    linkedSides.add(hSide);
                } else if (!beam.isHook()) {
                    return false;
                }
            }
        }

        if (beam.isHook() && linkedSides.isEmpty()) {
            return false;
        }

        if (!beam.isHook() && (linkedSides.size() == 2)) {
            // Discard the competing hook if any
            if (oppoHook != null) {
                if (oppoHook.isVip()) {
                    logger.info("VIP {} remove competing {}", this, oppoHook);
                }

                oppoHook.remove();
            }
        }

        return true;
    }

    //------------//
    // linkStumps //
    //------------//
    public void linkStumps (int profile)
    {
        if (beam.isVip()) {
            logger.info("VIP {} linkStumps", this);
        }
        for (VLinker vLinker : stumpLinkers) {
            final Glyph stump = vLinker.getStump();

            if (stump.isVip()) {
                logger.info("VIP {} linkStumps at {}", this, stump);
            }

            // Side stumps have already been processed
            if (sideStumps.values().contains(stump)) {
                continue;
            }

            if (!vLinker.isLinked()) {
                // Link from a connected beam seed should find a suitable head
                vLinker.link(Profiles.BEAM_SEED, profile);
            }
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return new StringBuilder(getClass().getSimpleName())
                .append("{beam#").append(beam.getId()).append('}').toString();
    }

    //----------------//
    // buildSideStump //
    //----------------//
    /**
     * Try to build a stump on the specified side of the beam.
     *
     * @param hSide specified beam side
     * @return the created stump or null if failed
     */
    private Glyph buildSideStump (HorizontalSide hSide)
    {
        if (beam.isVip()) {
            logger.info("VIP {} buildSideStump {}", this, hSide);
        }

        final Area area = getStumpArea(hSide);
        final List<Section> sections = new ArrayList<>(
                Sections.intersectedSections(area, system.getVerticalSections()));

        // Sort by distance of centroid abscissa WRT refPt
        final int xDir = hSide.direction();
        final double sideX = (xDir < 0) ? median.getX1() : median.getX2();
        final double refX = sideX - xDir * params.maxStemThickness / 2.0;
        Collections.sort(sections, (s1, s2)
                         -> Double.compare(Math.abs(s1.getAreaCenter().getX() - refX),
                                           Math.abs(s2.getAreaCenter().getX() - refX)));

        if (sections.isEmpty()) {
            return null;
        }

        final SectionCompound compound = new SectionCompound();
        for (Section s : sections) {
            compound.addSection(s);

            if (compound.getWidth() > params.maxStemThickness) {
                compound.removeSection(s);
                break;
            }
        }

        if (compound.getWeight() == 0) {
            return null; // This can occur when we have a single section, but too wide
        }

        // Check the stump clearly points out on one vertical side of beam
        Glyph stumpGlyph = compound.toGlyph(GlyphGroup.STUMP);
        final Set<VerticalSide> directions = getStumpDirections(stumpGlyph);

        if (directions == null || directions.isEmpty()) {
            return null;
        }

        stumpGlyph = system.getSheet().getGlyphIndex().registerOriginal(stumpGlyph);
        logger.debug("{} {}", this, stumpGlyph);

        return stumpGlyph;
    }

    //-------------//
    // getSeedArea //
    //-------------//
    /**
     * Define the lookup area for suitable stem seeds.
     *
     * @return the seed lookup area
     */
    private Area getSeedArea ()
    {
        // Use beam area, slightly expanded in x and y
        final double slope = (median.getY2() - median.getY1()) / (median.getX2() - median.getX1());
        final double dx = params.maxBeamSeedDx;
        final int profile = Math.max(beam.getProfile(), system.getProfile());
        final double dy = params.maxBeamSeedDyRatio
                                  * scale.toPixels(BeamStemRelation.getYGapMaximum(profile));
        final Path2D path = AreaUtil.horizontalParallelogramPath(
                new Point2D.Double(median.getX1() - dx, median.getY1() - slope * dx),
                new Point2D.Double(median.getX2() + dx, median.getY2() + slope * dx),
                beam.getHeight() + 2 * dy);
        beam.addAttachment("seed", path);

        return new Area(path);
    }

    //--------------//
    // getStumpArea //
    //--------------//
    /**
     * Define the lookup area on beam side for suitable stump building.
     *
     * @return the stump lookup area
     */
    private Area getStumpArea (HorizontalSide hSide)
    {
        final int xDir = hSide.direction();
        final double xSide = (xDir < 0) ? median.getX1() : median.getX2();
        final double width = params.maxStemThickness;
        final Point2D innerPt = LineUtil.intersectionAtX(median, xSide - xDir * width);
        final Path2D path = (xDir < 0)
                ? AreaUtil.horizontalParallelogramPath(median.getP1(), innerPt, beam.getHeight())
                : AreaUtil.horizontalParallelogramPath(innerPt, median.getP2(), beam.getHeight());
        final String tag = "s" + ((xDir > 0) ? "R" : "L");
        beam.addAttachment(tag, path);

        return new Area(path);
    }

    //--------------------//
    // getStumpDirections //
    //--------------------//
    /**
     * Determine stump directions (when going away from beam to head).
     *
     * @param stump the candidate stump to check
     * @return null for beam inside group, top, bottom, both or none
     */
    private Set<VerticalSide> getStumpDirections (Glyph stump)
    {
        if (stump.isVip()) {
            logger.info("VIP {} getStumpDirections {}", this, stump);
        }

        final Point2D stumpCenter = stump.getCenter2D();
        final List<AbstractBeamInter> siblings = getSiblingBeamsAt(stumpCenter);
        final AbstractBeamInter b1 = siblings.get(0);
        final AbstractBeamInter b2 = siblings.get(siblings.size() - 1);

        // Beware: we can have beam and beam hook from the same glyph, at end of group
        // Pure list extrema are not reliable, hence we check underlying glyph
        final Glyph glyph = beam.getGlyph();
        if ((beam != b1) && (beam != b2)
                    && (glyph != b1.getGlyph()) && (glyph != b2.getGlyph())) {
            return null; // beam is located inside beam group
        }

        // Look at vertical offsets off of beams
        final Set<VerticalSide> set = EnumSet.noneOf(VerticalSide.class);
        final double x = stumpCenter.getX();
        final Line2D stumpLine = stump.getCenterLine();

        final double dy1 = Math.max(0, LineUtil.yAtX(b1.getBorder(TOP), x) - stumpLine.getY1());
        if (dy1 >= params.minBeamStumpDy) {
            set.add(TOP);
        }

        final double dy2 = Math.max(0, stumpLine.getY2() - LineUtil.yAtX(b2.getBorder(BOTTOM), x));
        if (dy2 >= params.minBeamStumpDy) {
            set.add(BOTTOM);
        }

        return set;
    }

    //------------//
    // purgeSeeds //
    //------------//
    /**
     * Purge the collection of seeds, by filtering out those leading to duplicates.
     *
     * @param seeds the collection to purge (already sorted in abscissa)
     */
    private void purgeSeeds (List<Glyph> seeds)
    {
        NextSeed:
        for (int i = 0; i < seeds.size(); i++) {
            final Glyph s1 = seeds.get(i);
            final Line2D l1 = s1.getCenterLine();
            final Point2D p1 = LineUtil.intersection(l1, median);
            final double x1 = p1.getX();

            for (int j = i + 1; j < seeds.size(); j++) {
                final Glyph s2 = seeds.get(j);
                final Line2D l2 = s2.getCenterLine();
                final Point2D p2 = LineUtil.intersection(l2, median);
                final double x2 = p2.getX();

                if ((x2 - x1) >= params.minBeamStemsDx) {
                    break;
                }

                if (GeoUtil.yOverlap(s1.getBounds(), s2.getBounds()) > 0) {
                    // Vertical overlap, keep the longer
                    if (s1.getHeight() >= s2.getHeight()) {
                        seeds.remove(j); // s2
                    } else {
                        seeds.remove(i--); // s1
                        continue NextSeed;
                    }
                } else {
                    // No overlap, keep the closer to beam
                    if (l1.ptSegDistSq(p1) <= l2.ptSegDistSq(p2)) {
                        seeds.remove(j); // s2
                    } else {
                        seeds.remove(i--); // s1
                        continue NextSeed;
                    }
                }
            }
        }
    }

    //----------------//
    // retrieveStumps //
    //----------------//
    /**
     * Retrieve stumps for the beam.
     * <p>
     * We use stem seeds near the beam and complement with one stump on each side of the beam if
     * so needed.
     *
     * @return the beam connectable seeds
     */
    private List<Glyph> retrieveStumps ()
    {
        if (beam.isVip()) {
            logger.info("VIP {} retrieveStumps", this);
        }

        final List<Glyph> list = new ArrayList<>(Glyphs.intersectedGlyphs(neighborSeeds,
                                                                          getSeedArea()));
        Collections.sort(list, (g1, g2) -> Double.compare(
                LineUtil.intersection(g1.getCenterLine(), median).getX(),
                LineUtil.intersection(g2.getCenterLine(), median).getX()));

        // Perhaps some seeds need to be merged or purged
        purgeSeeds(list);

        // Try to have a stump on both beam sides
        if (!list.isEmpty()) {
            // Check for presence of seed on beam sides
            for (HorizontalSide hSide : HorizontalSide.values()) {
                Glyph seed = list.get(hSide == LEFT ? 0 : list.size() - 1);
                final double x = LineUtil.intersection(seed.getCenterLine(), median).getX();
                BeamPortion portion = BeamStemRelation.computeBeamPortion(beam, x, scale);

                if ((portion != null) && (portion.side() == hSide)) {
                    sideStumps.put(hSide, seed);
                }
            }
        }

        for (HorizontalSide hSide : HorizontalSide.values()) {
            Glyph stump = sideStumps.get(hSide);

            if (stump == null) {
                // Try to build a stump on this side of the beam
                stump = buildSideStump(hSide);

                if (stump != null) {
                    sideStumps.put(hSide, stump);

                    if (hSide == LEFT) {
                        list.add(0, stump);
                    } else {
                        list.add(stump);
                    }
                }
            }
        }

        return list;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // BLinker //
    //---------//
    public class BLinker
            extends StemLinker
    {

        /** To ease debugging. */
        private final int id;

        /** Horizontal side of beam, if any. Null for a link in beam center portion. */
        private final HorizontalSide hSide;

        /** Beam reference point on median line. */
        private Point2D refPt;

        /** Starting beam stump, if any. */
        private final Glyph stump;

        /** Just an anchor, not meant to explore stem items from this point. */
        private boolean isAnchor;

        /** Top and bottom linkers. */
        private final Map<VerticalSide, VLinker> vLinkers = new EnumMap<>(
                VerticalSide.class);

        /** Has been successfully linked. */
        private boolean linked;

        /** Has been closed (no more link attempt). */
        private boolean closed;

        public BLinker (Glyph stump,
                        HorizontalSide hSide,
                        Point2D refPt,
                        boolean isAnchor)
        {
            this.stump = stump;
            this.hSide = hSide;
            this.isAnchor = isAnchor;

            id = register();

            if (refPt == null) {
                if (stump != null) {
                    this.refPt = LineUtil.intersection(stump.getCenterLine(), median);
                } else if (hSide != null) {
                    final int xDir = hSide.direction();
                    final double sideX = (xDir < 0) ? median.getX1() : median.getX2();
                    final double refX = sideX - xDir * params.mainStemThickness / 2.0;
                    this.refPt = LineUtil.intersectionAtX(median, refX);
                }
            } else {
                this.refPt = new Point2D.Double(refPt.getX(), refPt.getY());
            }

            if (hSide != null) {
                sideBLinkers.put(hSide, this);
            }

            if (beam.isVip()) {
                logger.info("VIP new {}", this);
            }

            if (isAnchor) {
                buildAnchor();
            }
        }

        //----------------//
        // getHalfLinkers //
        //----------------//
        @Override
        public Collection<? extends StemHalfLinker> getHalfLinkers ()
        {
            return vLinkers.values();
        }

        //----------//
        // isClosed //
        //----------//
        @Override
        public boolean isClosed ()
        {
            return closed;
        }

        //-----------//
        // setClosed //
        //-----------//
        @Override
        public void setClosed (boolean closed)
        {
            this.closed = closed;
        }

        //----------//
        // isLinked //
        //----------//
        @Override
        public boolean isLinked ()
        {
            return linked;
        }

        //-----------//
        // setLinked //
        //-----------//
        @Override
        public void setLinked (boolean linked)
        {
            this.linked = linked;
        }

        //-------------------//
        // getReferencePoint //
        //-------------------//
        @Override
        public Point2D getReferencePoint ()
        {
            return refPt;
        }

        //-----------//
        // getSource //
        //-----------//
        @Override
        public AbstractBeamInter getSource ()
        {
            return beam;
        }

        //-------------//
        // buildAnchor //
        //-------------//
        private void buildAnchor ()
        {
            // Draw a small circle around refPt
            final double r = scale.getInterline() / 10.0;
            beam.addAttachment(
                    "" + id,
                    new Ellipse2D.Double(refPt.getX() - r, refPt.getY() - r, 2 * r, 2 * r));
        }

        //----------//
        // getStump //
        //----------//
        @Override
        public Glyph getStump ()
        {
            return stump;
        }

        //----------//
        // toString //
        //----------//
        @Override
        public String toString ()
        {
            final StringBuilder asb = new StringBuilder(getClass().getSimpleName())
                    .append("{beam#").append(beam.getId())
                    .append(' ').append(hSide != null ? hSide.name().charAt(0) : 'C')
                    .append('-').append(id);

            if (isAnchor) {
                asb.append(" ANCHOR");
            }

            if (stump != null) {
                asb.append(' ').append(stump);
            } else if (refPt != null) {
                asb.append(" refPt:").append(PointUtil.toString(refPt));
            }

            return asb.append('}').toString();
        }

        //------//
        // link //
        //------//
        /**
         * Try to link beam at this BLinker, based on its StemDraft.
         *
         * @param stemProfile desired profile level for stem building
         * @param linkProfile global desired profile level
         */
        private boolean link (int stemProfile,
                              int linkProfile)
        {
            // BLinker inside beam group or already linked?
            if (vLinkers.isEmpty() || isLinked()) {
                return true;
            }

            for (VLinker vLinker : vLinkers.values()) {
                if (!vLinker.sb.getTargetLinkers().isEmpty()) {
                    if (vLinker.link(stemProfile, linkProfile)) {
                        setLinked(true);
                    }
                }
            }

            return isLinked();
        }

        //----------//
        // register //
        //----------//
        private int register ()
        {
            allBLinkers.add(this);
            return allBLinkers.size();
        }

        //---------//
        // VLinker //
        //---------//
        /**
         * Beam vertical linker.
         * <p>
         * It handles connection from beam at a given point (connected seed or beam side) to
         * relevant heads in the selected vertical direction.
         */
        class VLinker
                extends StemHalfLinker
        {

            /** Vertical side from beam to stems/heads. */
            @Navigable(false)
            private final VerticalSide vSide;

            /** Direction of ordinate values when going away from beam. */
            private final int yDir;

            /** Lookup area for heads and stem items. */
            private Area luArea;

            /** The theoretical line from beam. */
            private Line2D theoLine;

            /** Stem seeds in lookup area. */
            private Set<Glyph> seeds;

            /** Head side for stopping stem expansion. */
            private final HorizontalSide stoppingHeadSide;

            /** Items sequence. */
            private StemBuilder sb;

            /**
             * Create a linker in the provided vertical direction if so desired,
             * with a starting stump or a beam horizontal side or just a refPt (an anchor).
             *
             * @param yDir  vertical direction, if any, when going away from beam
             * @param stump starting glyph, if any
             * @param rePt  reference point, if any
             */
            public VLinker (int yDir)
            {
                this.yDir = yDir;

                vSide = VerticalSide.of(yDir);
                stoppingHeadSide = (yDir < 0) ? LEFT : RIGHT;

                if (isAnchor) {
                    ///buildAnchor();
                } else {
                    buildGeometry();
                }
            }

            //-------------------//
            // getReferencePoint //
            //-------------------//
            @Override
            public Point2D getReferencePoint ()
            {
                return refPt;
            }

            //-----------//
            // getSource //
            //-----------//
            @Override
            public AbstractBeamInter getSource ()
            {
                return beam;
            }

            //----------------//
            // getHalfLinkers //
            //----------------//
            @Override
            public Collection<? extends StemHalfLinker> getHalfLinkers ()
            {
                return Collections.singleton(this);
            }

            //----------//
            // toString //
            //----------//
            @Override
            public String toString ()
            {
                final StringBuilder asb = new StringBuilder(getClass().getSimpleName())
                        .append("{beam#").append(beam.getId())
                        .append(' ')
                        .append((vSide != null) ? vSide.name().charAt(0) : "")
                        .append((hSide != null) ? hSide.name().charAt(0) : 'C')
                        .append('-').append(id);

                if (isAnchor) {
                    asb.append(" ANCHOR");
                }

                return asb.append('}').toString();
            }

            //----------//
            // isClosed //
            //----------//
            @Override
            public boolean isClosed ()
            {
                return closed;
            }

            //-----------//
            // setClosed //
            //-----------//
            @Override
            public void setClosed (boolean closed)
            {
                BLinker.this.setClosed(closed);
            }

            //----------//
            // isLinked //
            //----------//
            @Override
            public boolean isLinked ()
            {
                return linked;
            }

            //-----------//
            // setLinked //
            //-----------//
            @Override
            public void setLinked (boolean linked)
            {
                BLinker.this.setLinked(linked);
            }

            @Override
            public Glyph getStump ()
            {
                return stump;
            }

            @Override
            public Line2D getTheoreticalLine ()
            {
                return theoLine;
            }

            @Override
            public Area getLookupArea ()
            {
                return luArea;
            }

            //---------------//
            // buildGeometry //
            //---------------//
            private void buildGeometry ()
            {
                luArea = buildLuArea(null); // This gives a first value for theoLine

                // Check for closer limit (due to some alien beam, for example)
                final Line2D closer = getCloserLimit();

                if (closer != null) {
                    luArea = buildLuArea(closer); // This shrinks theoline accordingly
                }

                beam.addAttachment("t" + id, theoLine);
                seeds = Glyphs.intersectedGlyphs(neighborSeeds, luArea);
            }

            //-------------//
            // buildLuArea //
            //-------------//
            /**
             * Define the lookup area, knowing the reference point of the beam.
             * <p>
             * The LuArea is relevant only for a VLinker going out on beam group border, not for any
             * intermediate (anchor) linker inside the group.
             * <p>
             * Global slope is used (plus and minus slopeMargin).
             *
             * @param limit the rather horizontal limit for the area, or null to use system limit
             * @return the lookup area
             */
            private Area buildLuArea (Line2D limit)
            {
                final double slope = -system.getSheet().getSkew().getSlope();
                final double dSlope = yDir * params.slopeMargin;

                final double xRef = refPt.getX();
                final Line2D border = beam.getBorder(vSide);
                final Point2D pl = LineUtil.intersectionAtX(border, xRef - params.halfBeamLuDx);
                final Point2D pr = LineUtil.intersectionAtX(border, xRef + params.halfBeamLuDx);

                // Look-up path, start by beam horizontal segment
                final Path2D lu = new Path2D.Double();
                final int profile = Math.max(beam.getProfile(), system.getProfile());
                final double yOffset = yDir * params.maxBeamSeedDyRatio * scale.toPixels(
                        BeamStemRelation.getYGapMaximum(profile));
                lu.moveTo(pl.getX(), pl.getY() + yOffset);
                lu.lineTo(pr.getX(), pr.getY() + yOffset);

                // Then segment away from beam
                double yLimit;
                if (limit == null) {
                    // System limit as starting value
                    final Rectangle systemBox = system.getBounds();
                    yLimit = (yDir < 0) ? systemBox.getMaxY() : systemBox.getMinY();

                    // Use part(s) limit
                    final Point center = beam.getCenter();
                    final List<Staff> staves = system.getStavesAround(center);

                    for (Staff staff : staves) {
                        final Rectangle partBox = staff.getPart().getAreaBounds();
                        yLimit = (yDir > 0)
                                ? Math.max(yLimit, partBox.y + partBox.height - 1)
                                : Math.min(yLimit, partBox.y);
                    }
                } else {
                    // Use provided limit
                    yLimit = LineUtil.yAtX(limit, refPt.getX());
                }

                final double dy = yLimit - refPt.getY();
                lu.lineTo(pr.getX() + ((slope + dSlope) * dy), yLimit);
                lu.lineTo(pl.getX() + ((slope - dSlope) * dy), yLimit);

                lu.closePath();

                // Attachment
                beam.addAttachment("" + id, lu);

                // Compute theoLine
                theoLine = retriever.getTheoreticalLine(refPt, yLimit);
                beam.addAttachment("t" + id, theoLine);

                return new Area(lu);
            }

            //--------//
            // expand //
            //--------//
            /**
             * Expand current stem from beam as much as possible.
             * <p>
             * If <code>stemProfile</code> == Profiles.BEAM_SIDE then we are linking a beam side,
             * so:
             * <ol>
             * <li>At least one head must be reached from the beam,
             * <li>The stem must end with a head on correct side.
             * </ol>
             *
             * @param stemProfile desired profile for inclusion of additional items
             * @param linkProfile desired profile for head-stem link
             * @param relations   (output) to be populated by head-stem relations
             * @param glyphs      (output) to be populated that glyphs that do compose the stem
             * @return index of last item to pick, or -1 if failed
             */
            private int expand (int stemProfile,
                                int linkProfile,
                                Map<StemLinker, Relation> relations,
                                Set<Glyph> glyphs)
            {
                if (beam.isVip()) {
                    logger.info("VIP {} expand {}", this, sb);
                }

                final Scale scale = system.getSheet().getScale();
                final int maxIndex = sb.maxIndex();
                int maxYGap = retriever.getGapMap().get(stemProfile);

                final Line2D stemLine = (yDir > 0) ? theoLine
                        : new Line2D.Double(theoLine.getP2(), theoLine.getP1());
                StemItem stoppingHeadItem = null; // Latest acceptable stopping head
                Set<Glyph> stoppingGlyphs = null; // Glyphs until latest acceptable stopping head

                // Expand until a stop condition is met
                for (int i = 0; i <= maxIndex; i++) {
                    final StemItem ev = sb.get(i);

                    // Show-stopping gap?
                    if ((ev instanceof GapItem) && (ev.contrib > maxYGap)) {
                        if (stoppingHeadItem == null) {
                            return -1;
                        } else {
                            glyphs.clear();
                            glyphs.addAll(stoppingGlyphs);
                            return sb.indexOf(stoppingHeadItem);
                        }
                    } else if (ev instanceof LinkerItem
                                       && ((LinkerItem) ev).linker instanceof CLinker) {
                        // Head encountered
                        final CLinker cl = (CLinker) ((LinkerItem) ev).linker;
                        final HeadInter clHead = cl.getHead();

                        // Can we stop before this head?
                        if (stoppingHeadItem != null) {
                            // Gap close before head?
                            final GapItem gap = sb.getLastGapBefore(i);

                            if (gap != null) {
                                final double y = cl.getReferencePoint().getY();
                                final double dy = (yDir > 0)
                                        ? y - gap.line.getY2()
                                        : gap.line.getY1() - y;
                                if (dy < params.minLinkerLength) {
                                    // We include this coming head only if not tied on other vSide
                                    final CLinker clOpp = clHead.getLinker().getCornerLinker(
                                            cl.getSLinker().getHorizontalSide().opposite(), vSide);
                                    if (clOpp.hasConcreteStart(linkProfile)) {
                                        logger.debug("{} separated from head#{}",
                                                     this, clHead.getId());
                                        glyphs.clear();
                                        glyphs.addAll(stoppingGlyphs);
                                        return sb.indexOf(stoppingHeadItem);
                                    }
                                }
                            }
                        }

                        final HeadStemRelation hsRel = cl.checkStemRelation(stemLine, linkProfile);

                        if (hsRel == null) {
                            logger.debug("No relation for {} from {}", cl, this);
                            continue;
                        }

                        relations.put(cl, hsRel);

                        // Could this head be a stopping head?
                        if ((hsRel.getHeadSide() == stoppingHeadSide) && !glyphs.isEmpty()) {
                            final Glyph stemGlyph = (glyphs.size() > 1)
                                    ? GlyphFactory.buildGlyph(glyphs) : glyphs.iterator().next();
                            final Line2D line = stemGlyph.getCenterLine();
                            final StemPortion sp = hsRel.getStemPortion(clHead, line, scale);
                            final boolean isEnd = (sp == ((yDir > 0) ? STEM_BOTTOM : STEM_TOP));

                            if (isEnd) {
                                stoppingHeadItem = ev;
                                stoppingGlyphs = new LinkedHashSet<>(glyphs);

                                // Once a first stopping head has been reached, use normal maxYGap
                                maxYGap = retriever.getGapMap().get(Profiles.STANDARD);
                            }
                        }
                    }

                    // GapItem: No glyph
                    // GlyphItem: Plain glyph encountered
                    // CLinker: Compatible head encountered
                    // VLinker: (Starting) Beam encountered
                    // BLinker: Beam encountered
                    updateStemLine(ev.glyph, glyphs, stemLine);
                }

                return maxIndex;
            }

            //-------------//
            // filterBeams //
            //-------------//
            /**
             * Collect BLinkers to address relevant beams in beam group.
             * <p>
             * All siblings are mutually connected via BLinker instances.
             *
             * @param siblings relevant beams in beam group
             * @return collection of BLinker instances, one per relevant beam
             */
            private List<BLinker> filterBeams (List<AbstractBeamInter> siblings)
            {
                if (beam.isVip()) {
                    logger.info("VIP {} filterBeams", this);
                }

                final List<BLinker> bLinkers = new ArrayList<>();

                for (Inter bInter : siblings) {
                    AbstractBeamInter b = (AbstractBeamInter) bInter;

                    if (b != beam) {
                        if ((b.getGlyph() != null) && (b.getGlyph() != beam.getGlyph())) {
                            bLinkers.add(b.getLinker().findLinker(theoLine));
                        }
                    }
                }

                return bLinkers;
            }

            //-------------//
            // filterHeads //
            //-------------//
            private List<CLinker> filterHeads (List<AbstractBeamInter> siblings)
            {
                // Last beam border before heads
                final Line2D lastBorder = (yDir > 0)
                        ? siblings.get(siblings.size() - 1).getBorder(BOTTOM)
                        : siblings.get(0).getBorder(TOP);
                final double yLastBorder = LineUtil.yAtX(lastBorder, refPt.getX());

                final List<Inter> headCandidates = Inters.intersectedInters(
                        retriever.getSystemHeads(), GeoOrder.BY_ABSCISSA, luArea);

                for (AbstractBeamInter b : siblings) {
                    headCandidates.removeAll(beam.getSig().getCompetingInters(b));
                }

                final List<CLinker> cLinkers = new ArrayList<>();

                // For void heads
                final HorizontalSide imposedVoidHeadSide = (yDir < 0) ? LEFT : RIGHT;

                for (Inter hInter : headCandidates) {
                    final HeadInter head = (HeadInter) hInter;
                    // Today, standard beams can't link small heads
                    if (head.getShape().isSmall()) {
                        continue;
                    }

                    // Check head is far enough from beam group end
                    final double dy = yDir * (head.getCenter().y - yLastBorder);
                    if (dy < params.minBeamHeadDy) {
                        continue;
                    }

                    for (SLinker sLinker : head.getLinker().getSLinkers().values()) {
                        if (luArea.contains(sLinker.getReferencePoint())) {
                            // For void shape, check head hSide
                            if ((head.getShape() != Shape.NOTEHEAD_VOID)
                                        || (sLinker.getHorizontalSide() == imposedVoidHeadSide)) {
                                // TODO: Check possible relation between head and stem/theo line?
                                cLinkers.add(sLinker.getCornerLinker(vSide.opposite()));
                            }
                        }
                    }
                }

                return cLinkers;
            }

            //------------//
            // getBLinker //
            //------------//
            private BLinker getBLinker ()
            {
                return BLinker.this;
            }

            //----------------//
            // getCloserLimit //
            //----------------//
            /**
             * Report the closer limit, if any, for search.
             * <p>
             * If theoretical line intersects an alien beam, stop there and shrink accordingly
             * (excepted if the alien beam belongs to a sibling beam group).
             *
             * @return the closer limit if any
             */
            private Line2D getCloserLimit ()
            {
                final List<Inter> aliens = retriever
                        .getNeighboringInters(retriever.getSystemBeams(), beamBox);
                aliens.removeAll(beam.getGroup().getMembers());

                // Check concrete beam (no hook) intersection with theoLine
                // But don't count beams with side aligned with ours in vertical neighborhood
                for (Iterator<Inter> it = aliens.iterator(); it.hasNext();) {
                    final AbstractBeamInter b = (AbstractBeamInter) it.next();
                    final Line2D m = b.getMedian();

                    if (b instanceof BeamHookInter) {
                        it.remove();
                    } else if (!m.intersectsLine(theoLine)) {
                        it.remove();
                    } else {
                        final Point2D cross = LineUtil.intersection(theoLine, m);
                        final double dy = Math.abs(cross.getY() - refPt.getY());

                        if (dy <= params.maxBeamGroupDy) {
                            final double xc = cross.getX();
                            final double dx = Math.abs(
                                    xc - ((hSide == LEFT) ? m.getX1() : m.getX2()));
                            if (dx < params.maxBeamSideDx) {
                                it.remove();
                            }
                        }
                    }
                }

                if (aliens.isEmpty()) {
                    return null;
                }

                retriever.sortBeamsFromRef(refPt, yDir, aliens);

                AbstractBeamInter firstAlien = (AbstractBeamInter) aliens.get(0);

                return firstAlien.getBorder(vSide.opposite());
            }

            //---------//
            // inspect //
            //---------//
            /**
             * Look for reachable heads (and other beams in same beam group) in linker area,
             * ordered by vertical distance.
             *
             * <p>
             * A head is considered as reachable if it has a head stump and the center of this stump
             * is located in lookup area.
             * <p>
             * If head has no stump (this can happen e.g. for lack of black pixels), then we
             * consider head refPt instead of head stump center.
             * <p>
             * Since a void head can appear only at the very end of beam stem, it is constrained by
             * its hSide vs beam yDir
             *
             * @param maxStemProfile maximum possible stem profile
             */
            private void inspect (int maxStemProfile)
            {
                if (beam.isVip()) {
                    logger.info("VIP {} inspect maxStemProfile:{}", this, maxStemProfile);
                }

                // Sibling beams
                final List<AbstractBeamInter> siblings = getSiblingBeamsAt(refPt);

                // Relevants beams and heads
                final List<StemLinker> linkers = new ArrayList<>();
                linkers.addAll(filterBeams(siblings));
                linkers.addAll(filterHeads(siblings));

                sb = new StemBuilder(retriever, this, seeds, linkers, maxStemProfile);
            }

            //------//
            // link //
            //------//
            /**
             * Try to link beam, using items for reachable heads.
             * <p>
             * Processing is done from beam to heads.
             * <p>
             * We can stop only at a head for which stem is on correct horizontal head side,
             * that is left of head for stem going up, right of head for stem going down.
             *
             * @param stemProfile profile level for stem building
             * @param linkProfile profile level for stem linking (head and beam)
             * @return true if OK
             */
            private boolean link (int stemProfile,
                                  int linkProfile)
            {
                if (beam.isVip()) {
                    logger.info("VIP {} link", this);
                }

                if (stump != null && stump.isVip()) {
                    logger.info("VIP {} link at {}", this, stump);
                }

                // Some head to reach from beam?
                final List<CLinker> headLinkers = sb.getCLinkers(null);
                if (headLinkers.isEmpty()) {
                    return false;
                }

                // Retrieve all relevant items
                final Map<StemLinker, Relation> relations = new LinkedHashMap<>();
                final Set<Glyph> glyphs = new LinkedHashSet<>();
                final int lastIndex = expand(stemProfile, linkProfile, relations, glyphs);

                if ((lastIndex == -1) || relations.isEmpty()) {
                    return false;
                }

                // Stem built from items
                if (glyphs.isEmpty()) {
                    return false;
                }

                StemInter stem = sb.createStem(glyphs, stemProfile);

                if (stem == null) {
                    return false;
                }

                // Check to reuse stem (rare case of a stem with 2 beam groups)
                for (Entry<StemLinker, Relation> entry : relations.entrySet()) {
                    final CLinker cl = (CLinker) entry.getKey();

                    if (cl.isLinked()) {
                        final HorizontalSide hs = cl.getSLinker().getHorizontalSide();
                        final HeadInter head = cl.getSource();
                        final Set<StemInter> stems = head.getSideStems().get(hs);

                        if (stems.size() == 1) {
                            stem = stems.iterator().next();
                            logger.debug("{} reusing {}", this, stem);
                            break;
                        }
                    }
                }

                // Link between starting beam and stem?
                final Link bsLink = BeamStemRelation.checkLink(
                        beam, stem, vSide.opposite(), scale, stemProfile);
                if (bsLink == null) {
                    logger.info("{} no beam link", this);
                    return false;
                }

                final SIGraph sig = system.getSig();
                if (stem.getId() == 0) {
                    sig.addVertex(stem);
                }

                bsLink.applyTo(beam);
                getBLinker().setLinked(true);

                // Link other sibling beams as well
                linkSiblings(stem, ((Support) bsLink.relation).getGrade());

                // Connections by applying links
                for (Entry<StemLinker, Relation> entry : relations.entrySet()) {
                    // Relation was checked against a temporary stem, perhaps not the final one
                    final CLinker cl = (CLinker) entry.getKey();
                    final HeadInter head = cl.getSource();
                    cl.getSLinker().setLinked(true);

                    if (null == sig.getRelation(head, stem, HeadStemRelation.class)) {
                        sig.addEdge(head, stem, entry.getValue());
                    }
                }

                // Portion of stem draft still to be processed?
                if (lastIndex < sb.maxIndex()) {
                    ///stemDraft.splitAfter(lastIndex);
                }

                return true;
            }

            //--------------//
            // linkSiblings //
            //--------------//
            private void linkSiblings (StemInter stem,
                                       double relGrade)
            {
                if (stem.isVip()) {
                    logger.info("VIP {} linkSiblings with {}", this, stem);
                }

                final Line2D stemMedian = stem.getMedian();
                final SIGraph sig = system.getSig();
                final List<AbstractBeamInter> siblings = getSiblingBeamsAt(refPt);
                siblings.remove(beam);

                for (AbstractBeamInter b : siblings) {
                    if (b.getGlyph() == beam.getGlyph()) {
                        continue;
                    }

                    if (sig.getRelation(b, stem, BeamStemRelation.class) == null) {
                        final BeamStemRelation r = new BeamStemRelation();
                        final Point2D crossPt = LineUtil.intersection(stemMedian,
                                                                      b.getMedian());
                        r.setExtensionPoint(new Point2D.Double(
                                crossPt.getX(),
                                crossPt.getY() + (yDir * (b.getHeight() / 2.0))));

                        // Portion depends on x location of stem WRT beam
                        r.setBeamPortion(BeamStemRelation.computeBeamPortion(
                                b, crossPt.getX(), scale));

                        r.setGrade(relGrade);
                        sig.addEdge(b, stem, r);

                        final StemLinker sl = sb.getLinkerOf(b);
                        if (sl != null) {
                            sl.setLinked(true);
                        }
                    }
                }
            }
        }
    }
}
