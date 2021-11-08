//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       H e a d L i n k e r                                      //
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
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.dynamic.SectionCompound;
import org.audiveris.omr.lag.DynamicSection;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.lag.Sections;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.sheet.Profiles;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.stem.BeamLinker.BLinker;
import org.audiveris.omr.sheet.stem.HeadLinker.SLinker.CLinker;
import static org.audiveris.omr.sheet.stem.StemHalfLinker.updateStemLine;
import org.audiveris.omr.sheet.stem.StemItem.GapItem;
import org.audiveris.omr.sheet.stem.StemItem.GlyphItem;
import org.audiveris.omr.sheet.stem.StemItem.LinkerItem;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.*;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
 * Class <code>HeadLinker</code> handles the connections from a head to nearby stems,
 * processing the four corners around head.
 * <p>
 * We have to handle the case where stem pixels between a head and a compatible beam are reduced
 * to almost nothing because of poor image quality.
 * In this case, we may have no concrete stem inter candidate available, and thus have to
 * directly inspect the rather vertical segment area between head reference point and potential
 * beam, looking for a few pixels there.
 * <ol>
 * <li>For every head, we look for one connectable stem seed at LEFT_STEM anchor and one connectable
 * stem seed at RIGHT_STEM anchor.
 * <li>When no <b>seed</b> can be connected on one head side, we build a head <b>stump</b> there
 * from suitable section(s) near head anchor.
 * <li>From now on, the seed or the stump will be considered by stem candidates as the head portion
 * to connect to.
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class HeadLinker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HeadLinker.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The head being processed. */
    @Navigable(false)
    private final HeadInter head;

    /** Head bounding box. */
    private final Rectangle headBox;

    /** All beams and hooks interpretations in head vicinity (in no particular order). */
    private final List<Inter> neighborBeams;

    /** All stems seeds in head vicinity. */
    private final Set<Glyph> neighborSeeds;

    /** Side linkers. */
    private final Map<HorizontalSide, SLinker> sLinkers = new EnumMap<>(HorizontalSide.class);

    // System-level information
    // ------------------------
    @Navigable(false)
    private final StemsRetriever retriever;

    @Navigable(false)
    private final SIGraph sig;

    @Navigable(false)
    private final SystemInfo system;

    @Navigable(false)
    private final Scale scale;

    private final StemsRetriever.Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>HeadLinker</code> object.
     *
     * @param head      the head inter to link
     * @param retriever the driving system-level StemsRetriever
     */
    public HeadLinker (HeadInter head,
                       StemsRetriever retriever)
    {
        this.head = head;
        this.retriever = retriever;

        headBox = head.getBounds();
        sig = head.getSig();
        system = sig.getSystem();
        scale = system.getSheet().getScale();
        params = retriever.getParams();

        neighborBeams = retriever.getNeighboringInters(retriever.getSystemBeams(), headBox);
        neighborSeeds = retriever.getNeighboringSeeds(headBox);

        for (HorizontalSide hSide : HorizontalSide.values()) {
            sLinkers.put(hSide, new SLinker(hSide));
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // getCornerLinker //
    //-----------------//
    public CLinker getCornerLinker (HorizontalSide hSide,
                                    VerticalSide vSide)
    {
        return sLinkers.get(hSide).getCornerLinker(vSide);
    }

    //-------------//
    // getSLinkers //
    //-------------//
    /**
     * Report the two side linkers.
     *
     * @return the hSide Linkers
     */
    public Map<HorizontalSide, SLinker> getSLinkers ()
    {
        return sLinkers;
    }

    //-----------------//
    // inspectCLinkers //
    //-----------------//
    /**
     * Inspect head on all corners.
     */
    public void inspectCLinkers ()
    {
        if (head.isVip()) {
            logger.info("VIP {} inspectCLinkers", this);
        }

        // Maximum possible stemProfile
        final int stemProfile = isRatherGood(head) ? Profiles.RATHER_GOOD_HEAD : Profiles.POOR;

        // Look for targets in all corners
        for (HeadCorner corner : HeadCorner.values()) {
            sLinkers.get(corner.hSide).cLinkers.get(corner.vSide).inspect(stemProfile);
        }
    }

    //-----------//
    // linkSides //
    //-----------//
    /**
     * Try to link head on its both horizontal sides.
     * <p>
     * When this method is called, linking from beams to related (stems and) heads has already been
     * done.
     * What is left is the linking of:
     * <ul>
     * <li>Heads <b>related to beam</b> but for which there was no beam stump and the head was not
     * located on beam side, hence there has been no attempt to link head starting from the beam.
     * Starting from such heads, the beam is a hard target.
     * <li>Heads <b>not related to beam</b>.
     * For these heads, we'll try to reach the typical stem length as a soft target.
     * </ul>
     *
     * This method is called on each and every head not yet linked from a beam.
     * Using higher and higher profiles, on left and right head sides, we check both top and bottom
     * CLinker's:
     * <ol>
     * <li>If no significant length is found on either top or bottom, we consider no link can be
     * found on this horizontal side of head.
     * <li>If top <b>or</b> bottom (exclusively) exhibits a significant length, then a link can be
     * searched on the vertical direction found.
     * <li>If both top <b>and</b> bottom exhibit a significant length, then this head is assumed to
     * be located within a longer "column of heads".
     * No link is searched for this head, we expect that either the starting or the terminating head
     * of the column will fall in the case 2 above.
     * This head horizontal side is recorded in the 'undefs' collection of undefined heads, to be
     * later checked when all other attempts have been made.
     * </ol>
     *
     * Policies:
     * <ul>
     * <li>A <b>rather good</b> head should have at least one corner connection.
     * <li>
     * A <b>void</b> head linked (to a stem) to a <b>beam</b> should have a connection on the
     * opposite corner.
     * </ul>
     *
     * @param stemProfile desired profile level for stem
     * @param linkProfile global profile for links
     * @param undefs      (output) populate with undefined head sides
     * @param append      true for appending to already linked heads
     * @return true if linked
     */
    public boolean linkSides (int stemProfile,
                              int linkProfile,
                              LinkedHashMap<Inter, Set<HorizontalSide>> undefs,
                              boolean append)
    {
        if (head.isVip()) {
            logger.info("VIP {} linkSides sp:{} lp:{} {}", this, stemProfile, linkProfile, append);
        }

        boolean linked = false;

        for (HorizontalSide hSide : HorizontalSide.values()) {
            final SLinker sLinker = sLinkers.get(hSide);

            if (sLinker.isLinked()) {
                linked = true;
                continue;
            }

            if (!append && sLinker.isClosed()) {
                continue;
            }

            // Check top and bottom cLinker's
            final CLinker clTop = sLinker.getCornerLinker(TOP);
            final boolean topOk = clTop.canLink(stemProfile, append);

            final CLinker clBot = sLinker.getCornerLinker(BOTTOM);
            final boolean botOk = clBot.canLink(stemProfile, append);

            if (head.isVip()) {
                logger.info("VIP {} stemProfile:{} top:{} bottom:{}",
                            sLinker, stemProfile, topOk, botOk);
            }

            if (topOk) {
                if (!botOk) {
                    if (clTop.link(stemProfile, linkProfile, append)) {
                        logger.debug("{} linked", clTop);
                        linked = true;
                    }
                } else {
                    // Here, there seems to be potential connections on both vertical sides
                    // So, stop processing this head immediately for both horizontal sides
                    // But stay open to a link coming later (from above or below)
                    Set<HorizontalSide> hSides = undefs.get(head);

                    if (hSides == null) {
                        undefs.put(head, hSides = EnumSet.noneOf(HorizontalSide.class));
                    }

                    hSides.add(hSide);
                }
            } else if (botOk) {
                if (clBot.link(stemProfile, linkProfile, append)) {
                    logger.debug("{} linked", clBot);
                    linked = true;
                }
            }
        }

        if (!linked) {
            // Retry with higher profile?
            if (isRatherGood(head) && (stemProfile < Profiles.RATHER_GOOD_HEAD)) {
                if (head.isVip()) {
                    logger.info("VIP {} ratherGood", this);
                }

                return linkSides(++stemProfile, linkProfile, undefs, append);
            } else {
                for (SLinker sLinker : sLinkers.values()) {
                    sLinker.setClosed(true);
                }

                return false;
            }
        } else {
            // Close linked SLinker, except for this starting head
            for (SLinker sLinker : sLinkers.values()) {
                if (sLinker.isLinked()) {
                    for (Relation rel : sig.getRelations(head, HeadStemRelation.class)) {
                        final StemInter stem = (StemInter) sig.getOppositeInter(head, rel);

                        for (Relation r : sig.getRelations(stem, HeadStemRelation.class)) {
                            final HeadInter h = (HeadInter) sig.getOppositeInter(stem, r);

                            if (h != head) {
                                for (SLinker sl : h.getLinker().sLinkers.values()) {
                                    sl.setClosed(true);
                                }
                            }
                        }
                    }
                }
            }

            return true;
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return new StringBuilder(getClass().getSimpleName())
                .append("{head#").append(head.getId()).append('}').toString();
    }

    //------------------//
    // lookupBeamGroups //
    //------------------//
    /**
     * Look for (groups of) beam interpretations.
     *
     * @param beams         provided collection of candidate beams
     * @param refPt         starting reference point
     * @param yDir          vertical direction from reference point
     * @param minBeamHeadDy minimum vertical distance between head and beam
     * @return the list of groups, ordered by distance from head
     */
    public static List<BeamGroupInter> lookupBeamGroups (List<Inter> beams,
                                                         Point2D refPt,
                                                         int yDir,
                                                         int minBeamHeadDy)
    {
        if (beams.isEmpty()) {
            return Collections.emptyList();
        }

        final double slope = beams.get(0).getSig().getSystem().getSheet().getSkew().getSlope();

        // Reject beam candidates which are not in corner direction
        // (this can happen because of beam bounding rectangle)
        for (Iterator<Inter> it = beams.iterator(); it.hasNext();) {
            final AbstractBeamInter b = (AbstractBeamInter) it.next();
            final Line2D limit = b.getBorder(VerticalSide.of(-yDir));

            if ((yDir * (StemsRetriever.getTargetPt(refPt, limit, slope).getY() - refPt.getY()))
                        <= 0) {
                it.remove();
            }
        }

        StemsRetriever.sortBeamsFromRef(refPt, yDir, beams);

        // Build the (ordered) list of beam groups
        Set<BeamGroupInter> groups = new LinkedHashSet<>();

        for (Inter inter : beams) {
            AbstractBeamInter beam = (AbstractBeamInter) inter;

            if (groups.isEmpty()) {
                // Check if beam is far enough from head
                final Line2D limit = beam.getBorder(VerticalSide.of(-yDir));
                final Point2D beamPt = StemsRetriever.getTargetPt(refPt, limit, slope);
                final double distToBeam = yDir * (beamPt.getY() - refPt.getY());

                if (distToBeam < minBeamHeadDy) {
                    continue;
                }
            }

            groups.add(beam.getGroup());
        }

        return new ArrayList<>(groups);
    }

    //--------------//
    // isRatherGood //
    //--------------//
    /**
     * A rather good head for which at least connection should be found.
     *
     * @param head the head at stake
     * @return true if rather good
     */
    private boolean isRatherGood (HeadInter head)
    {
        return head.getGrade() >= Grades.ratherGoodHeadGrade;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // SLinker //
    //---------//
    /**
     * Head side linker to handle connection on one head side to suitable stem(s).
     */
    public class SLinker
            extends StemLinker
    {

        /** Head side considered for stem. */
        @Navigable(false)
        private final HorizontalSide hSide;

        /** Direction of abscissa values when going away from head. */
        private final int xDir;

        /** The head reference point for this head side. */
        private final Point2D refPt;

        /** Max outside point. */
        private final Point2D outPt;

        /** Max inside point. */
        private final Point2D inPt;

        /** The stump or seed on this head side, if any. */
        private final Glyph stump;

        /** Top and bottom corner linkers. */
        private final Map<VerticalSide, CLinker> cLinkers = new EnumMap<>(
                VerticalSide.class);

        /** Has been successfully linked. */
        private boolean linked;

        /** Has been closed (no more link attempt). */
        private boolean closed;

        /**
         * Creates a <code>SLinker</code> object and populates head side stump.
         *
         * @param hSide dedicated head horizontal side
         */
        public SLinker (HorizontalSide hSide)
        {
            this.hSide = hSide;

            xDir = hSide.direction();
            refPt = head.getStemReferencePoint(hSide);
            outPt = getOutPoint();
            inPt = getInPoint();
            stump = retrieveStump();

            for (VerticalSide vSide : VerticalSide.values()) {
                cLinkers.put(vSide, new CLinker(vSide));
            }
        }

        //-----------------//
        // getCornerLinker //
        //-----------------//
        public CLinker getCornerLinker (VerticalSide vSide)
        {
            return cLinkers.get(vSide);
        }

        //----------------//
        // getHalfLinkers //
        //----------------//
        @Override
        public Collection<? extends StemHalfLinker> getHalfLinkers ()
        {
            return cLinkers.values();
        }

        //---------//
        // getHead //
        //---------//
        /**
         * Report the underlying head.
         *
         * @return the head
         */
        public HeadInter getHead ()
        {
            return head;
        }

        //-------------------//
        // getHorizontalSide //
        //-------------------//
        /**
         * Report the horizontal side with respect to head
         *
         * @return hSide
         */
        public HorizontalSide getHorizontalSide ()
        {
            return hSide;
        }

        //------------//
        // getInPoint //
        //------------//
        /**
         * Report the reference point slightly translated to the interior of the head,
         * to catch stem candidates.
         *
         * @return the inner refPt
         */
        private Point2D getInPoint ()
        {
            return new Point2D.Double(refPt.getX() - (xDir * params.maxHeadInDx), refPt.getY());
        }

        //-------------//
        // getOutPoint //
        //-------------//
        /**
         * Report the reference point slightly translated to the exterior of the head,
         * to catch stem candidates.
         *
         * @return the outer refPt
         */
        private Point2D getOutPoint ()
        {
            return new Point2D.Double(refPt.getX() + (xDir * params.maxHeadOutDx), refPt.getY());
        }

        //-------------------//
        // getReferencePoint //
        //-------------------//
        /**
         * Report the head reference point for this horizontal side.
         *
         * @return head hSide reference point
         */
        @Override
        public Point2D getReferencePoint ()
        {
            return refPt;
        }

        //-----------//
        // getSource //
        //-----------//
        @Override
        public HeadInter getSource ()
        {
            return head;
        }

        //----------//
        // getStump //
        //----------//
        /**
         * Report the head stump, if any, on this horizontal side of the head.
         *
         * @return the head side stump, or null
         */
        @Override
        public Glyph getStump ()
        {
            return stump;
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

        //----------//
        // toString //
        //----------//
        @Override
        public String toString ()
        {
            final StringBuilder asb = new StringBuilder(getClass().getSimpleName())
                    .append("{head#").append(head.getId())
                    .append(' ').append(hSide.name().charAt(0));

            if (stump != null) {
                asb.append(' ').append(stump);
            } else if (refPt != null) {
                asb.append(" refPt:").append(PointUtil.toString(refPt));
            }

            return asb.append('}').toString();
        }

        //------------//
        // buildStump //
        //------------//
        /**
         * Build the head stump for this horizontal side, using sections around the
         * stem reference point.
         * <p>
         * We consider only vertical sections around the refPt
         *
         * @return the stump glyph, perhaps null
         */
        private Glyph buildStump ()
        {
            final List<Section> sections = new ArrayList<>(
                    Sections.intersectedSections(getStumpArea(), system.getVerticalSections()));

            // Sort by distance of centroid abscissa WRT refPt
            Collections.sort(sections, (s1, s2)
                             -> Double.compare(Math.abs(s1.getAreaCenter().getX() - refPt.getX()),
                                               Math.abs(s2.getAreaCenter().getX() - refPt.getX())));

            if (sections.isEmpty()) {
                return null;
            }

            // Pick up first section if any which contains refPt
            final SectionCompound compound = new SectionCompound();
            final Point p = new Point((int) refPt.getX(), (int) refPt.getY());
            for (Section section : sections) {
                if (section.contains(p)) {
                    compound.addSection(section);
                    break;
                }
            }

            // Then include sections as much as possible
            for (Section s : sections) {
                compound.addSection(s);

                if (compound.getWidth() > params.mainStemThickness) {
                    compound.removeSection(s);
                }
            }

            if (compound.getWeight() == 0) {
                // This can occur when we have a single section, but too wide
                // so, we pickup a few runs of the section, around refPt abscissa
                // This can occur also if no section was kept
                logger.debug("{} nothing kept from {}", this, Sections.ids(sections));
                Section subSection = getSubSection(sections.get(0));

                if (subSection.getWeight() == 0) {
                    return null;
                }

                compound.addSection(subSection);
            }

            Glyph stumpGlyph = compound.toGlyph(GlyphGroup.STUMP);
            stumpGlyph = system.getSheet().getGlyphIndex().registerOriginal(stumpGlyph);
            logger.debug("{} {}", this, stumpGlyph);

            return stumpGlyph;
        }

        //-------------//
        // getSeedArea //
        //-------------//
        /**
         * Define the lookup area on head side for suitable stem seed.
         *
         * @return the seed lookup area
         */
        private Area getSeedArea ()
        {
            final double dy = params.maxHeadSeedDy;
            final Point2D left = (xDir > 0) ? inPt : outPt;
            final Point2D right = (xDir > 0) ? outPt : inPt;
            final String tag = "v" + ((xDir > 0) ? "R" : "L");
            final Rectangle2D rect = new Rectangle2D.Double(left.getX(),
                                                            left.getY() - dy,
                                                            right.getX() - left.getX(),
                                                            2 * dy);
            head.addAttachment(tag, rect);
            return new Area(rect);
        }

        //--------------//
        // getStumpArea //
        //--------------//
        /**
         * Define the lookup area on head side for suitable stump building.
         *
         * @return the stump lookup area
         */
        private Area getStumpArea ()
        {
            final double rx = refPt.getX();
            final double dy = params.stumpAreaDyHalf;
            final double left = (xDir > 0) ? rx - params.stumpAreaDxIn : rx - params.stumpAreaDxOut;
            final double right = (xDir > 0) ? rx + params.stumpAreaDxOut : rx + params.stumpAreaDxIn;
            final String tag = "s" + ((xDir > 0) ? "R" : "L");
            final Rectangle2D rect = new Rectangle2D.Double(left,
                                                            refPt.getY() - dy,
                                                            right - left,
                                                            2 * dy);
            head.addAttachment(tag, rect);
            return new Area(rect);
        }

        //---------------//
        // getSubSection //
        //---------------//
        /**
         * Extract a thinner section from the provided (too wide) section.
         *
         * @param wide the too wide section
         * @return thinner section extracted around refPt
         */
        private Section getSubSection (Section wide)
        {
            final int stemWidth = scale.getStemThickness();
            final int x0 = (int) Math.rint(refPt.getX() - stemWidth / 2.0);
            final int i0 = Math.max(0, x0 - wide.getFirstPos());
            final int x1 = x0 + stemWidth;
            final int i1 = Math.min(x1 - wide.getFirstPos(), wide.getRunCount());

            final DynamicSection ds = new DynamicSection(Orientation.VERTICAL);

            if (i1 > i0) {
                final List<Run> runs = wide.getRuns().subList(i0, i1);
                ds.setFirstPos(x0);

                for (Run run : runs) {
                    ds.append(new Run(run));
                }
            }

            return ds;
        }

        //---------------//
        // retrieveStump //
        //---------------//
        /**
         * Retrieve a suitable seed near reference point or try to build a stump if no
         * seed could be selected.
         *
         * @return the stump if any: a seed found or a brand-new stump or null
         */
        private Glyph retrieveStump ()
        {
            if (head.isVip()) {
                logger.info("VIP {} retrieveStump", this);
            }

            // Look for suitable stem seed if any
            final List<Glyph> seeds = new ArrayList<>(Glyphs.intersectedGlyphs(neighborSeeds,
                                                                               getSeedArea()));
            if (seeds.size() > 1) {
                // Choose the one closest to refPt
                Collections.sort(seeds, (g1, g2) -> Double.compare(
                        g1.getCenterLine().ptSegDistSq(refPt),
                        g2.getCenterLine().ptSegDistSq(refPt)));
            }

            if (!seeds.isEmpty()) {
                final Glyph bestSeed = seeds.get(0);

                // Impose a maximum dx between refPt and seed
                final double seedX = LineUtil.xAtY(bestSeed.getCenterLine(), refPt.getY());
                final int dx = (int) Math.round(xDir * (seedX - refPt.getX()));

                if ((dx >= 0) && (dx <= params.maxHeadOutDx)) {
                    return bestSeed;
                }

                if ((dx <= 0) && (-dx <= params.maxHeadInDx)) {
                    return bestSeed;
                }
            }

            // No suitable seed, so let's build a stump from suitable runs/sections
            return buildStump();
        }

        //---------//
        // CLinker //
        //---------//
        /**
         * A CLinker handles head to stem links in a given corner of the head.
         */
        public class CLinker
                extends StemHalfLinker
        {

            /** Vertical side from head to beam/tail. */
            @Navigable(false)
            private final VerticalSide vSide;

            /** Direction of ordinates when going away from head. */
            private final int yDir;

            /** The distant target point for the stem. (stem opposite end of refPt) */
            private final Point2D targetPt;

            /** The look up area for the corner. */
            private Area luArea;

            /** The stems seeds found in the corner. */
            private List<Glyph> seeds;

            /** The theoretical line from head. */
            private Line2D theoLine;

            /** Ordinate range between refPt and limit. */
            private final Rectangle yRange;

            private final List<BeamGroupInter> beamGroups;

            /** Targeted beam if any in this corner. */
            private AbstractBeamInter targetBeam;

            /** Items sequence. */
            private StemBuilder sb;

            public CLinker (VerticalSide vSide)
            {
                this.vSide = vSide;
                yDir = vSide.direction();
                luArea = buildLuArea(null); // This also computes theoLine

                // Look for beams and beam hooks in the corner
                List<Inter> beamCandidates = Inters.intersectedInters(
                        neighborBeams, GeoOrder.BY_ABSCISSA, luArea);

                // Look for suitable beam groups
                beamGroups = lookupBeamGroups(beamCandidates);

                // Compute target end of stem using either system limit
                // or beam group limit if such beam group intersects the theoretical Line.
                targetPt = computeTargetPoint(beamGroups);
                theoLine.setLine(refPt, targetPt);
                yRange = getYRange(targetPt.getY());
            }

            //---------//
            // canLink //
            //---------//
            /**
             * Report whether we can link from this CLinker, based on its StemBuilder items
             * and the provided stemProfile level.
             *
             * @param stemProfile provided level for stem profile
             * @param append      true to allow appending to already linked head
             * @return true if possible
             */
            public boolean canLink (int stemProfile,
                                    boolean append)
            {
                // Check we have a long enough sequence of items
                if (!sb.headHasLength(stemProfile)) {
                    return false;
                }

                // If we hit a close head, check if we can link with it or not
                final int myIndex = sb.indexOf(this);
                final CLinker cl = sb.getFirstCLinkerAfter(myIndex);

                if (cl == null) {
                    return true;
                }

                if (!append && cl.isLinked()) {
                    return false;
                }

                // If there is no stem gap between these 2 heads, accept the link
                final int icl = sb.indexOf(cl);
                Integer gapIndex = null;

                for (int i = myIndex + 1; i < icl; i++) {
                    if (sb.get(i) instanceof GapItem) {
                        gapIndex = i;
                    }
                }

                if (gapIndex == null) {
                    return true;
                }

                // Gap: let's check our own opposite corner
                final CLinker myDiag = getCornerOpposite();
                if (myDiag.hasConcreteStart(Profiles.STRICT)) {
                    // Use length just before gap
                    return sb.getLengthAt(gapIndex - 1) >= params.minLinkerLength;
                }

                // Gap: let's check other head corner in opposite horizontal side
                final CLinker diag = cl.getSource().getLinker().getCornerLinker(
                        hSide.opposite(), vSide);
                if (diag.canLink(Profiles.STRICT, false)) {
                    // Use length just before gap
                    return sb.getLengthAt(gapIndex - 1) >= params.minLinkerLength;
                }

                return true;
            }

            //-------------------//
            // checkStemRelation //
            //-------------------//
            public HeadStemRelation checkStemRelation (Line2D stemLine,
                                                       int profile)
            {
                return HeadStemRelation.checkRelation(head, stemLine, stump, vSide, scale, profile);
            }

            //-------------------//
            // getCornerOpposite //
            //-------------------//
            /**
             * Report the CLinker that handles the opposite corner of this one.
             *
             * @return the corner-opposite CLinker
             */
            public CLinker getCornerOpposite ()
            {
                return sLinkers.get(hSide.opposite()).getCornerLinker(vSide.opposite());
            }

            //----------------//
            // getHalfLinkers //
            //----------------//
            @Override
            public Collection<? extends StemHalfLinker> getHalfLinkers ()
            {
                return Collections.singleton(this);
            }

            //---------//
            // getHead //
            //---------//
            public HeadInter getHead ()
            {
                return head;
            }

            //---------------//
            // getLookupArea //
            //---------------//
            @Override
            public Area getLookupArea ()
            {
                return luArea;
            }

            //-------------------//
            // getReferencePoint //
            //-------------------//
            @Override
            public Point2D getReferencePoint ()
            {
                return refPt;
            }

            //------------//
            // getSLinker //
            //------------//
            /**
             * Report the containing head side linker.
             *
             * @return containing SLinker
             */
            public SLinker getSLinker ()
            {
                return SLinker.this;
            }

            //-----------//
            // getSource //
            //-----------//
            @Override
            public HeadInter getSource ()
            {
                return head;
            }

            //----------//
            // getStump //
            //----------//
            @Override
            public Glyph getStump ()
            {
                return stump;
            }

            //--------------------//
            // getTheoreticalLine //
            //--------------------//
            @Override
            public Line2D getTheoreticalLine ()
            {
                return theoLine;
            }

            //------------------//
            // hasConcreteStart //
            //------------------//
            public boolean hasConcreteStart (int profile)
            {
                return sb.headHasConcreteStart(profile);
            }

            //---------//
            // inspect //
            //---------//
            /**
             * Look for reachable targets in head corner and store them in StemBuilder.
             * <p>
             * Targets can be: beam, other head, stem end
             * (based on seed or chunk or head stump or beam stump)
             *
             * @param maxStemProfile maximum possible stem profile
             */
            public void inspect (int maxStemProfile)
            {
                if (head.isVip()) {
                    logger.info("VIP {} inspect maxStemProfile:{}", this, maxStemProfile);
                }

                // Collect suitable seeds (on top of head stump)
                seeds = retrieveSeeds();

                // Other head linkers
                final List<StemLinker> linkers = new ArrayList<>();
                linkers.addAll(lookupOtherHeads());

                // Beam linker at end?
                if (targetBeam != null) {
                    if ((head.getShape() != Shape.NOTEHEAD_VOID)
                                || yDir != hSide.direction()) {
                        // Include all relevant beams in beam group
                        final Point2D xp = LineUtil.intersection(targetBeam.getMedian(), theoLine);
                        final List<AbstractBeamInter> siblings = targetBeam.getLinker()
                                .getSiblingBeamsAt(xp);

                        for (AbstractBeamInter b : siblings) {
                            final BLinker bLinker = b.getLinker().findLinker(theoLine);
                            linkers.add(bLinker);
                        }
                    }
                }

                sb = new StemBuilder(retriever, this, seeds, linkers, maxStemProfile);

                if (head.isVip()) {
                    logger.info("VIP {} {}", this, sb);
                }
            }

            //------//
            // link //
            //------//
            /**
             * Try to link head to reachable heads and beams.
             * <p>
             * Processing is done from head to tail or beam.
             * <p>
             * Stop the search at the first good beam found or at the expected stem length or
             * at the first non acceptable vertical gap, whichever comes first.
             *
             * @param stemProfile desired profile level for stem building
             * @param linkProfile desired profile level for stem linking
             * @param append      true to allow appending to already linked head
             * @return true if OK
             */
            public boolean link (int stemProfile,
                                 int linkProfile,
                                 boolean append)
            {
                if (head.isVip()) {
                    logger.info("VIP {} link", this);
                }

                final double yHard = refPt.getY() + yDir * params.minStemTailLg;
                final double ySoft = refPt.getY() + yDir * params.bestStemTailLg;
                final Map<StemLinker, Relation> relations = new LinkedHashMap<>();
                final Set<Glyph> glyphs = new LinkedHashSet<>();
                final int lastIndex = expand(yHard, ySoft, stemProfile, linkProfile,
                                             relations, glyphs);
                if (lastIndex == -1) {
                    return false;
                }

                // Stem built from glyphs
                if (glyphs.isEmpty()) {
                    return false;
                }

                StemInter stem = null;

                // Extending existing stem?
                if (append) {
                    stem = reuseStem(lastIndex);
                }

                if (stem == null) {
                    stem = sb.createStem(glyphs, stemProfile);
                }

                if (stem == null) {
                    return false;
                }

                final SIGraph sig = system.getSig();
                if (stem.getId() == 0) {
                    sig.addVertex(stem);
                }

                // Connections by "applying" links (head-stem and beam-stem)
                for (Entry<StemLinker, Relation> entry : relations.entrySet()) {
                    final Relation relation = entry.getValue();

                    if (relation instanceof HeadStemRelation) {
                        final CLinker cl = (CLinker) entry.getKey();
                        final HeadInter h = cl.getSource();

                        if (null == sig.getRelation(h, stem, HeadStemRelation.class)) {
                            sig.addEdge(h, stem, relation);
                        }

                        cl.getSLinker().setLinked(true);
                    } else if (relation instanceof BeamStemRelation) {
                        final BLinker bl = (BLinker) entry.getKey();
                        final AbstractBeamInter beam = bl.getSource();

                        if (!beam.isRemoved()) { // To prevent step crash
                            if (null == sig.getRelation(beam, stem, BeamStemRelation.class)) {
                                sig.addEdge(beam, stem, relation);
                            }

                            bl.setLinked(true);
                        }
                    }
                }

                // At this point, we have successfully linked  a stem w/ heads
                // Sequence of items still to be processed?
                if (lastIndex < sb.maxIndex()) {
                    // Pickup first remaining CLinker if any
                    final CLinker first = sb.getFirstCLinkerAfter(lastIndex);

                    if ((first != null) && !first.isClosed()) {
                        final HeadInter h = first.getHead();
                        ///h.getLinker().linkSides(0, linkProfile);
                        int maxProf = isRatherGood(h) ? Profiles.RATHER_GOOD_HEAD : linkProfile;

                        for (int prof = Profiles.STRICT; prof <= maxProf; prof++) {
                            if (first.link(prof, linkProfile, append)) {
                                break;
                            }
                        }
                    }
                }

                return true;
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
                SLinker.this.setClosed(closed);
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
                SLinker.this.setLinked(linked);
            }

            //----------//
            // toString //
            //----------//
            @Override
            public String toString ()
            {
                return new StringBuilder(getClass().getSimpleName())
                        .append("{head#").append(head.getId())
                        .append(' ').append(getId())
                        .append('}').toString();
            }

            //-------------//
            // buildLuArea //
            //-------------//
            /**
             * Define the lookup area on given corner, knowing the reference point of the
             * entity (head).
             * Global slope is used (plus and minus slopeMargin).
             * <p>
             * Side effect: compute theoLine
             *
             * @param limit rather horizontal limit for the area, or null to use part limit
             * @return the lookup area
             */
            private Area buildLuArea (Line2D limit)
            {
                // Beware: vertical slope is the opposite of sheet slope
                final double slope = -system.getSheet().getSkew().getSlope();
                final double dSlope = xDir * yDir * params.slopeMargin;

                // Look-up path, start by head horizontal segment
                final Path2D lu = new Path2D.Double();
                lu.moveTo(outPt.getX(), outPt.getY());
                lu.lineTo(inPt.getX(), inPt.getY());

                // Then segment away from head
                final double yLimit;
                if (limit == null) {
                    // Use part limit
                    final Rectangle partBox = head.getStaff().getPart().getAreaBounds();
                    yLimit = (yDir > 0) ? partBox.y + partBox.height - 1 : partBox.y;
                } else {
                    // Use provided (beam) limit
                    yLimit = LineUtil.yAtX(limit, refPt.getX());
                }

                final double dy = yLimit - outPt.getY();
                lu.lineTo(inPt.getX() + ((slope - dSlope) * dy), yLimit);
                lu.lineTo(outPt.getX() + ((slope + dSlope) * dy), yLimit);

                lu.closePath();

                // Attachment
                head.addAttachment(getId(), lu);

                // Compute theoLine
                theoLine = retriever.getTheoreticalLine(refPt, yLimit);
                head.addAttachment("t" + getId(), theoLine);

                return new Area(lu);
            }

            //--------------------//
            // computeTargetPoint //
            //--------------------//
            /**
             * Determine the target end point of stem.
             * <p>
             * This is based on part limit, unless a beam group intersects corner line, in which
             * case the beam group farthest limit is used and the corner area truncated accordingly.
             *
             * @param beamGroups the relevant beam groups, ordered by distance from head
             * @return the target stem end point
             */
            private Point2D computeTargetPoint (List<BeamGroupInter> beamGroups)
            {
                if (!beamGroups.isEmpty()) {
                    // Find the first group which really intersects the theoretical line
                    for (BeamGroupInter group : beamGroups) {
                        // Order its beams by distance from head
                        final List<Inter> beams = group.getMembers();
                        retriever.sortBeamsFromRef(refPt, yDir, beams);

                        for (Inter bInter : beams) {
                            final AbstractBeamInter beam = (AbstractBeamInter) bInter;
                            final Line2D median = beam.getMedian();

                            // TODO: perhaps intersecting theoLine is too strict?
                            if (median.intersectsLine(theoLine)) {
                                if (head.getShape().isSmall()) {
                                    // Exclude beam, stop just before group
                                    AbstractBeamInter b = (AbstractBeamInter) beams.get(0);
                                    final Line2D border = b.getBorder(vSide.opposite());
                                    luArea = buildLuArea(border);

                                    return getTargetPt(border);
                                } else {
                                    // Select farthest beam in group
                                    targetBeam = (AbstractBeamInter) beams.get(beams.size() - 1);
                                    final Line2D border = targetBeam.getBorder(vSide);

                                    // Redefine lookup area
                                    final double margin = targetBeam.getHeight(); // Should be enough
                                    final Line2D limit = new Line2D.Double(
                                            border.getX1(),
                                            border.getY1() + yDir * margin,
                                            border.getX2(),
                                            border.getY2() + yDir * margin);
                                    luArea = buildLuArea(limit);

                                    return getTargetPt(border);
                                }
                            }
                        }
                    }
                }

                return theoLine.getP2();
            }

            //--------//
            // expand //
            //--------//
            /**
             * Expand current stem from head as much as possible.
             * <p>
             * This means until reachable beam if any, otherwise until the stem free portion since
             * last head reaches at least yHard (min length) and at best ySoft (target length).
             *
             * @param yHard       hard target ordinate
             * @param ySoft       soft target ordinate
             * @param stemProfile desired profile for inclusion of additional items
             * @param linkProfile desired profile for head-stem linking
             * @param relations   (output) to be populated by head-stem and beam-stem relations
             * @param glyphs      (output) to be populated by glyphs that do compose the stem
             * @return index of last item to pick, or -1 if failed
             */
            private int expand (double yHard,
                                double ySoft,
                                int stemProfile,
                                int linkProfile,
                                Map<StemLinker, Relation> relations,
                                Set<Glyph> glyphs)
            {
                if (head.isVip()) {
                    logger.info("VIP {} expand {}", this, sb);
                }

                double lastY = theoLine.getY1(); // Last ordinate reached so far

                // Do we have a target beam?
                BLinker bLinker = null;
                final List<StemLinker> targets = sb.getTargetLinkers();

                if (!targets.isEmpty()) {
                    final StemLinker last = targets.get(targets.size() - 1);

                    if (last instanceof BLinker) {
                        bLinker = (BLinker) last;
                    }
                }

                final Scale scale = system.getSheet().getScale();
                final int maxIndex = sb.maxIndex();
                final int maxYGap = retriever.getGapMap().get(stemProfile);

                // Expand until a stop condition is met
                // Stem tail length is measured from ordinate of last (good) head encountered
                final Line2D stemLine = (yDir > 0) ? theoLine
                        : new Line2D.Double(theoLine.getP2(), theoLine.getP1());

                for (int i = 0; i <= maxIndex; i++) {
                    final StemItem ev = sb.get(i);

                    if (ev instanceof GapItem) {
                        // Show-stopping gap?
                        if (ev.contrib > maxYGap) {
                            if (yDir * Double.compare(lastY, yHard) < 0) {
                                return -1; // We failed before hard length target
                            }

                            // No hard target missed, we just stop expansion before too large gap
                            return i - 1;
                        }

                        if (bLinker == null) {
                            // Soft target reached?
                            if (yDir * Double.compare(lastY, ySoft) >= 0) {
                                // We can stop here, but let's check for a plain glyph right after
                                if (i < maxIndex) {
                                    final StemItem nextEv = sb.get(i + 1);
                                    if (nextEv instanceof GlyphItem) {
                                        updateStemLine(nextEv.glyph, glyphs, stemLine);
                                        return i + 1;
                                    }
                                }

                                return i - 1;
                            }
                        }
                    } else if (ev instanceof LinkerItem
                                       && ((LinkerItem) ev).linker instanceof CLinker) {
                        // Head encountered
                        final CLinker cl = (CLinker) ((LinkerItem) ev).linker;
                        final HeadInter clHead = cl.getHead();

                        if (cl != this) {
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
                                        return sb.indexOf(gap) - 1;
                                    }
                                }
                            }
                        }

                        final HeadStemRelation hsRel = cl.checkStemRelation(stemLine, linkProfile);

                        if (hsRel == null) {
                            continue;
                        }

                        relations.put(cl, hsRel);
                        updateStemLine(ev.glyph, glyphs, stemLine);

                        // Check that resulting contextual head grade is sufficient
                        // to reset stem free soft portion at this head ordinate
                        final double cg = retriever.getMaxHeadContextualGrade(cl.getHead(),
                                                                              hsRel);
                        if (cg >= Grades.minContextualGrade) {
                            final double cly = cl.getReferencePoint().getY();
                            ySoft = cly + yDir * params.bestStemTailLg;

//                            // Allow to push yHard to include new segments
//                            // But, if last segment fails, we should step back to the last good one
//                            if (stemProfile < Profiles.MAX_VALUE) {
//                                final double yHardNew = cly + yDir * params.minStemTailLg;
//
//                                if (yDir * Double.compare(yHardNew, yHard) > 0) {
//                                    yHard = yHardNew;
//                                }
//                            }
                        }
                    } else if (ev instanceof LinkerItem
                                       && ((LinkerItem) ev).linker instanceof BLinker) {
                        // Beam encountered
                        final BLinker bl = (BLinker) ((LinkerItem) ev).linker;
                        final AbstractBeamInter beam = bl.getSource();
                        updateStemLine(ev.glyph, glyphs, stemLine);
                        final BeamStemRelation bsRel = BeamStemRelation.checkRelation(
                                beam, stemLine, vSide, scale, stemProfile);
                        relations.put(bl, bsRel);

                        return i;
                    } else if (ev instanceof GlyphItem) {
                        // Plain glyph encountered
                        updateStemLine(ev.glyph, glyphs, stemLine);
                    }

                    if (!(ev instanceof GapItem) && (ev != null) && (ev.line != null)) {
                        lastY = (yDir > 0)
                                ? Math.max(lastY, ev.line.getY2())
                                : Math.min(lastY, ev.line.getY1());
                    }
                }

                // All items seen, check we have reached minimum of free tail length
                if (yDir * Double.compare(lastY, yHard) < 0) {
                    return -1; // We failed before hard length target
                }

                // Check initial head-stem relation with final stemLine
                final HeadStemRelation hsRel = this.checkStemRelation(stemLine, linkProfile);
                if (hsRel == null) {
                    return -1;
                }

                relations.put(this, hsRel);
                return maxIndex;
            }

            //------------//
            // getContrib //
            //------------//
            /**
             * Report the (vertical) contribution of a rectangle to the filling of white
             * space above or below the head.
             *
             * @param box the rectangle to check
             * @return the corresponding height within white space
             */
            private int getContrib (Rectangle box)
            {
                return Math.max(0, GeoUtil.yOverlap(yRange, box));
            }

            //-------//
            // getId //
            //-------//
            /**
             * Report the corner ID.
             *
             * @return id
             */
            private String getId ()
            {
                return new StringBuilder()
                        .append(vSide == TOP ? 'T' : 'B').append('-')
                        .append(hSide == LEFT ? 'L' : 'R').toString();
            }

            //----------//
            // getLimit //
            //----------//
            /**
             * Report closer beam limit, according to corner vertical direction.
             *
             * @param beam the beam or hook of interest
             * @return the top or bottom beam limit, according to dir
             */
            private Line2D getLimit (AbstractBeamInter beam)
            {
                return beam.getBorder(vSide.opposite());
            }

            //-------------//
            // getTargetPt //
            //-------------//
            /**
             * Compute the point where the (skewed) vertical from reference point
             * crosses the provided limit.
             *
             * @param limit the end of the white space (a rather horizontal line)
             * @return the limit crossing point with skewed vertical at reference point
             */
            private Point2D getTargetPt (Line2D limit)
            {
                return StemsRetriever.getTargetPt(
                        refPt, limit, system.getSheet().getSkew().getSlope());
            }

            //-----------//
            // getYRange //
            //-----------//
            /**
             * Compute the range to be covered by stem items
             *
             * @param yLimit the limit farthest from head
             * @return a range rectangle
             */
            private Rectangle getYRange (double yLimit)
            {
                return new Rectangle(
                        0, // x is irrelevant
                        (int) Math.rint((yDir > 0) ? refPt.getY() : yLimit),
                        0, // width is irrelevant
                        (int) Math.rint(Math.abs(yLimit - refPt.getY())));
            }

            //------------------//
            // lookupBeamGroups //
            //------------------//
            /**
             * Look for (groups of) beam interpretations in the lookup area.
             *
             * @param beams provided collection of candidate beams
             * @return the list of groups, ordered by distance from head
             */
            private List<BeamGroupInter> lookupBeamGroups (List<Inter> beams)
            {
                return HeadLinker.lookupBeamGroups(beams, refPt, yDir, params.minBeamHeadDy);
            }

            //------------------//
            // lookupOtherHeads //
            //------------------//
            private List<CLinker> lookupOtherHeads ()
            {
                if (head.isVip()) {
                    logger.info("VIP {} lookupOtherHeads", this);
                }

                final List<CLinker> list = new ArrayList<>();

                // Last ordinate before candidates
                final double yLast = refPt.getY() + yDir * params.minHeadHeadDy;

                // Filter head candidates
                final List<Inter> headCandidates = Inters.intersectedInters(
                        retriever.getSystemHeads(), GeoOrder.BY_ABSCISSA, luArea);
                headCandidates.remove(head);
                headCandidates.removeAll(sig.getCompetingInters(head));

                for (Inter hInter : headCandidates) {
                    final HeadInter h = (HeadInter) hInter;

                    // Check other head shape is compatible with initial head shape
                    if (h.getShape() != head.getShape()) {
                        continue;
                    }

                    // Check head is far enough from start
                    final double dy = yDir * (h.getCenter().y - yLast);
                    if (dy < 0) {
                        continue;
                    }

                    for (SLinker sLinker : h.getLinker().getSLinkers().values()) {
                        if (luArea.contains(sLinker.getReferencePoint())) {
                            list.add(sLinker.getCornerLinker(vSide));
                        }
                    }
                }

                return list;
            }

            //---------------//
            // retrieveSeeds //
            //---------------//
            /**
             * Look for suitable stem seeds.
             */
            private List<Glyph> retrieveSeeds ()
            {
                // Collect all seeds that intersect corner lookup area.
                final Set<Glyph> set = Glyphs.intersectedGlyphs(neighborSeeds, luArea);
                final Rectangle stumpBox = (stump != null) ? stump.getBounds() : null;

                for (Iterator<Glyph> it = set.iterator(); it.hasNext();) {
                    final Glyph seed = it.next();
                    final Rectangle seedBox = seed.getBounds();

                    // Purge seeds that overlap ordinate-wise the head stump, if any
                    if ((stumpBox != null) && GeoUtil.yOverlap(seedBox, stumpBox) > 0) {
                        it.remove();
                    } else if (getContrib(seedBox) < params.minSeedContrib) {
                        // Purge seeds that do not contribute enough to ordinate range
                        it.remove();
                    } else {
                        // Purge seeds that are too far abscissa-wise from theoretical line
                        Point2D seedCenter = seed.getCentroid();
                        double dist = theoLine.ptLineDist(seedCenter);

                        if (dist > params.maxLineSeedDx) {
                            it.remove();
                        }
                    }
                }

                // In case of seeds overlap, simply keep the most contributive
                final List<Glyph> kept = new ArrayList<>();
                final List<Glyph> list = new ArrayList<>(set);
                Collections.sort(list, (g1, g2) -> Integer.compare(
                        getContrib(g2.getBounds()), getContrib(g1.getBounds())));

                StemLoop:
                for (Glyph seed : list) {
                    Rectangle stemBox = seed.getBounds();

                    for (Glyph k : kept) {
                        if (GeoUtil.yOverlap(stemBox, k.getBounds()) > 0) {
                            continue StemLoop;
                        }
                    }

                    // No overlap
                    kept.add(seed);
                }

                return kept;
            }

            //-----------//
            // reuseStem //
            //-----------//
            private StemInter reuseStem (int lastIndex)
            {
                final List<CLinker> headLinkers = sb.getCLinkers(lastIndex);

                for (CLinker cl : headLinkers) {
                    final HeadInter h = cl.getSource();

                    for (Relation r : sig.getRelations(h, HeadStemRelation.class)) {
                        HeadStemRelation hsRel = (HeadStemRelation) r;

                        if (hsRel.getHeadSide() == cl.getSLinker().getHorizontalSide()) {
                            return (StemInter) sig.getOppositeInter(h, r);
                        }
                    }
                }

                return null;
            }
        }
    }
}
