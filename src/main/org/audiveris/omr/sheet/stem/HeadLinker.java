//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       H e a d L i n k e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.dynamic.CompoundFactory;
import org.audiveris.omr.glyph.dynamic.SectionCompound;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.run.Orientation;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.util.Corner;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import static org.audiveris.omr.util.VerticalSide.*;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Class {@code HeadLinker} tries to establish links from a head to nearby stem
 * interpretations, processing the four corners around head.
 * <p>
 * We have to handle the case where stem pixels between a head and a compatible beam are reduced
 * to almost nothing because of poor image quality.
 * In this case, we may have no concrete stem inter candidate available, and thus have to
 * directly inspect the rather vertical segment area between head reference point and beam,
 * looking for a few pixels there.
 *
 * @author Hervé Bitteur
 */
public class HeadLinker
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(HeadLinker.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The head interpretation being processed. */
    private final HeadInter head;

    /** Head bounding box. */
    private final Rectangle headBox;

    /** All beams and hooks interpretations in head vicinity. */
    private List<Inter> neighborBeams;

    /** All stems seeds in head vicinity. */
    private Set<Glyph> neighborSeeds;

    /** All stems interpretations in head vicinity. */
    private List<Inter> neighborStems;

    // System-level information
    // ------------------------
    private final StemsBuilder builder;

    private final SIGraph sig;

    private final SystemInfo system;

    private final Scale scale;

    private final StemsBuilder.Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code HeadLinker} object.
     *
     * @param head    the head inter to link
     * @param builder the driving system-level StemsBuilder
     */
    public HeadLinker (HeadInter head,
                       StemsBuilder builder)
    {
        this.head = head;
        this.builder = builder;

        headBox = head.getBounds();

        sig = head.getSig();
        system = sig.getSystem();
        scale = system.getSheet().getScale();
        params = builder.getParams();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // linkCueCorner //
    //---------------//
    public void linkCueCorner (Corner corner,
                               List<Inter> beams,
                               StemInter stem)
    {
        new CornerLinker(corner).linkCue(beams, stem);
    }

    //-----------------//
    // linkStemCorners //
    //-----------------//
    public void linkStemCorners ()
    {
        if (head.isVip()) {
            logger.info("VIP linkStemCorners? {}", head);
        }

        neighborBeams = getNeighboringInters(builder.getSystemBeams());
        neighborSeeds = getNeighboringSeeds();

        for (Corner corner : Corner.values) {
            new CornerLinker(corner).link();
        }
    }

    //------------------//
    // reuseStemCorners //
    //------------------//
    public void reuseStemCorners ()
    {
        if (head.isVip()) {
            logger.info("VIP reuseStemCorners? {}", head);
        }

        neighborStems = getNeighboringInters(builder.getSystemStems());

        for (Corner corner : Corner.values) {
            new CornerLinker(corner).reuse();
        }
    }

    //----------------------//
    // getNeighboringInters //
    //----------------------//
    /**
     * From the provided collection of interpretations, retrieve all those located
     * in head vicinity.
     *
     * @param inters the collection of interpretations to search
     * @return the set of neighboring interpretations
     */
    private List<Inter> getNeighboringInters (List<? extends Inter> inters)
    {
        // Retrieve neighboring inters, using a box of system height and sufficiently wide,
        // just to play with a limited number of inters.
        Rectangle systemBox = system.getBounds();
        Rectangle fatBox = new Rectangle(
                headBox.x,
                systemBox.y,
                headBox.width,
                systemBox.height);
        fatBox.grow(params.vicinityMargin, 0);

        return Inters.intersectedInters(inters, GeoOrder.BY_ABSCISSA, fatBox);
    }

    //---------------------//
    // getNeighboringSeeds //
    //---------------------//
    /**
     * Retrieve all vertical seeds in head vicinity.
     *
     * @return the set of neighboring seeds
     */
    private Set<Glyph> getNeighboringSeeds ()
    {
        // Retrieve neighboring stem seeds, using a box of system height and sufficiently wide,
        // just to play with a limited number of seeds.
        Rectangle systemBox = system.getBounds();
        Rectangle fatBox = new Rectangle(
                headBox.x,
                systemBox.y,
                headBox.width,
                systemBox.height);
        fatBox.grow(params.vicinityMargin, 0);

        return Glyphs.intersectedGlyphs(builder.getSystemSeeds(), fatBox);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // CornerLinker //
    //--------------//
    /**
     * A CornerLinker searches for all acceptable head -> stem links in a given corner.
     */
    private class CornerLinker
    {

        /** The corner being processed. */
        private final Corner corner;

        /** Direction of abscissae when going away from head. */
        private final int xDir;

        /** Direction of ordinates when going away from head. */
        private final int yDir;

        /** The head reference point for the corner. */
        private final Point2D refPt;

        /** The distant target point for the stem. (stem opposite end of refPt) */
        private Point2D targetPt;

        /** The look up area for the corner. */
        private Area area;

        /** The stems seeds found in the corner. */
        private List<Glyph> seeds;

        /** The most probable stem target line. */
        private Line2D targetLine;

        /** Ordinate range between refPt and limit. */
        private Rectangle yRange;

        /** Targeted beam if any in this corner. */
        private AbstractBeamInter targetBeam;

        CornerLinker (Corner corner)
        {
            this.corner = corner;

            xDir = (corner.hSide == RIGHT) ? 1 : (-1);
            yDir = (corner.vSide == BOTTOM) ? 1 : (-1);
            refPt = getReferencePoint();
        }

        //------//
        // link //
        //------//
        /**
         * Look for all acceptable stems interpretations that can be connected to
         * the head in the desired corner.
         * <p>
         * Stop the search at the first good beam found or at the first non acceptable yGap,
         * whichever comes first.
         */
        public void link ()
        {
            if (head.isVip()) {
                logger.info("VIP link? {} {}", head, corner);
            }

            area = getLuArea(null);

            // Look for beams and beam hooks in the corner
            List<Inter> beamCandidates = Inters.intersectedInters(neighborBeams,
                                                                  GeoOrder.BY_ABSCISSA, area);

            // Look for suitable beam groups
            List<List<AbstractBeamInter>> beamGroups = lookupBeamGroups(beamCandidates);

            // Compute target end of stem using either system limit
            // or beam group limit if such beam group intersects corner area.
            targetPt = computeTargetPoint(beamGroups);

            Line2D line = new Line2D.Double(refPt, targetPt);
            head.addAttachment("t" + corner.getId(), line);

            yRange = getYRange(targetPt.getY());

            // Define the best target line (and collect suitable seeds)
            targetLine = getTargetLine();

            // Look for additional chunks built out of sections found.
            // Assign special role to a fat section part of head (if any)
            Wrapper<Section> fatHeadSection = new Wrapper<>(null);
            List<Glyph> chunks = lookupChunks(fatHeadSection);

            // Aggregate seeds and chunks up to the limit
            List<Glyph> items = new ArrayList<>(seeds);

            if (!chunks.isEmpty()) {
                items.addAll(chunks);
                sortByDistance(items);
            }

            double refY = refPt.getY(); // Reference ordinate

            if (fatHeadSection.value != null) {
                // Shift the reference ordinate accordingly
                Rectangle runBox = getRunBox(fatHeadSection.value, corner.hSide);
                int contrib = getContrib(runBox);

                if (contrib > 0) {
                    refY += (yDir * contrib);
                }
            }

            // Include each item (seed / chunk) until limit is reached
            List<StemInter> stems = includeItems(items, refY, fatHeadSection.value);

            // Beam - Stem connection(s)?
            if (!beamGroups.isEmpty() && !stems.isEmpty()) {
                linkBeamsAndStems(beamGroups, stems);
            }
        }

        //---------//
        // linkCue //
        //---------//
        /**
         * Specific link for cue (head & beam).
         */
        public void linkCue (List<Inter> candidates,
                             StemInter stem)
        {
            // Look for beams in the corner
            List<List<AbstractBeamInter>> beamGroups = lookupBeamGroups(candidates);
            linkBeamsAndStems(beamGroups, Collections.singletonList(stem));
        }

        //-------//
        // reuse //
        //-------//
        /**
         * Check the stems interpretations in the vicinity and try to connect the
         * head to them, if not already done.
         */
        public void reuse ()
        {
            area = getLuArea(null);

            // Look for stems inters that intersect the lookup area
            List<Inter> stems = Inters.intersectedInters(neighborStems, GeoOrder.BY_ABSCISSA, area);

            for (Inter inter : stems) {
                StemInter stemInter = (StemInter) inter;
                // (try to) connect
                connectHeadStem(null, stemInter);
            }
        }

        //--------------------//
        // areGroupCompatible //
        //--------------------//
        /**
         * Check whether the two beams can be consecutive beams in the same beam
         * group, using ordinate gap.
         *
         * @param one current beam
         * @param two following beam, in 'dir' direction
         * @return true if OK
         */
        private boolean areGroupCompatible (AbstractBeamInter one,
                                            AbstractBeamInter two)
        {
            // Vertical gap?
            Point2D onePt = getTargetPt(one.getMedian());
            Point2D twoPt = getTargetPt(two.getMedian());
            final double yDistance = Math.abs(onePt.getY() - twoPt.getY());

            if (yDistance > params.maxBeamDistance) {
                logger.debug("{} & {} are too distant", one, two);

                return false;
            }

            return true;
        }

        //--------------------//
        // computeTargetPoint //
        //--------------------//
        /**
         * Determine the target end point of stem.
         * This is based on system limit, unless a beam group intersects corner area, in which case
         * the beam group limit is used and the corner area truncated accordingly.
         *
         * @param beamGroups the relevant beam groups, ordered by distance from head
         * @return the target stem end point
         */
        private Point2D computeTargetPoint (List<List<AbstractBeamInter>> beamGroups)
        {
            Rectangle systemBox = system.getBounds();
            int sysY = (yDir > 0) ? (systemBox.y + systemBox.height) : systemBox.y;

            if (!beamGroups.isEmpty()) {
                List<AbstractBeamInter> beamGroup = beamGroups.get(0);
                targetBeam = beamGroup.get(beamGroup.size() - 1);
            }
            return getTargetPt(new Line2D.Double(0, sysY, 100, sysY));
//            }
//
//            // Stop at end of first beam group
//            Line2D limit = (yDir > 0) ? targetBeam.getBorder(BOTTOM) : targetBeam.getBorder(TOP);
//            area = getLuArea(limit);
//
//            return getTargetPt(limit);
        }

        //-----------------//
        // connectBeamStem //
        //-----------------//
        /**
         * (Try to) connect beam and stem.
         *
         * @param beam the beam or hook interpretation
         * @param stem the stem interpretation
         * @return the beam stem relation if successful, null otherwise
         */
        private BeamStemRelation connectBeamStem (AbstractBeamInter beam,
                                                  StemInter stem)
        {
            if (beam.isVip() && stem.isVip()) {
                logger.info("VIP connectBeamStem? {} & {}", beam, stem);
            }

            // Relation beam -> stem (if not yet present)
            BeamStemRelation bRel;
            bRel = (BeamStemRelation) sig.getRelation(beam, stem, BeamStemRelation.class);

            if (bRel == null) {
                final Line2D beamLimit = getLimit(beam);
                bRel = new BeamStemRelation();

                // Precise cross point
                Point2D start = stem.getTop();
                Point2D stop = stem.getBottom();
                Point2D crossPt = crossing(stem, beam);

                // Extension point
                bRel.setExtensionPoint(
                        new Point2D.Double(
                                crossPt.getX(),
                                crossPt.getY() + (yDir * (beam.getHeight() - 1))));

                // Abscissa -> beamPortion
                // toLeft & toRight are >0 if within beam, <0 otherwise
                double toLeft = crossPt.getX() - beamLimit.getX1();
                double toRight = beamLimit.getX2() - crossPt.getX();
                final double xGap;

                if (beam instanceof BeamInter && (Math.min(
                        toLeft,
                        toRight) > params.maxBeamInDx)) {
                    // It's a beam center connection
                    bRel.setBeamPortion(BeamPortion.CENTER);
                    xGap = 0;
                } else if (toLeft < toRight) {
                    bRel.setBeamPortion(BeamPortion.LEFT);
                    xGap = Math.max(0, -toLeft);
                } else {
                    bRel.setBeamPortion(BeamPortion.RIGHT);
                    xGap = Math.max(0, -toRight);
                }

                // Ordinate
                final double yGap = (yDir > 0) ? Math.max(0, crossPt.getY() - stop.getY())
                        : Math.max(0, start.getY() - crossPt.getY());

                bRel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), false);

                if (bRel.getGrade() >= bRel.getMinGrade()) {
                    sig.addEdge(beam, stem, bRel);
                    logger.debug("{} {} {}", head, corner, bRel);
                } else {
                    bRel = null;
                }
            }

            return bRel;
        }

        //-----------------//
        // connectHeadStem //
        //-----------------//
        /**
         * (Try to) connect head and stem.
         *
         * @param headSection the head section found, if any
         * @param stemInter   the stem interpretation to connect
         * @return the head-stem relation or null
         */
        private HeadStemRelation connectHeadStem (Section headSection,
                                                  StemInter stemInter)
        {
            // New relation head -> stem (if not yet present)
            HeadStemRelation hRel = (HeadStemRelation) sig.getRelation(
                    head,
                    stemInter,
                    HeadStemRelation.class);

            if (hRel == null) {
                hRel = new HeadStemRelation();
                hRel.setHeadSide(corner.hSide);

                if (head.isVip() && stemInter.isVip()) {
                    logger.info("VIP connectHeadStem? {} & {}", head, stemInter);
                }

                final Glyph stemGlyph = stemInter.getGlyph();
                final Rectangle stemBox = stemGlyph.getBounds();
                final double xGap;
                final double yGap;
                final double xAnchor;

                if (headSection != null) {
                    // xGap computed on head section
                    // yGap measured between head section and stem glyph
                    Rectangle runBox = getRunBox(headSection, corner.hSide);
                    xGap = xDir * (runBox.x - refPt.getX());
                    xAnchor = runBox.x;

                    int overlap = GeoUtil.yOverlap(runBox, stemBox);
                    yGap = Math.abs(Math.min(overlap, 0));
                } else {
                    // Use stem line to compute both xGap and yGap
                    Point2D start = stemGlyph.getStartPoint(VERTICAL);
                    Point2D stop = stemGlyph.getStopPoint(VERTICAL);
                    xAnchor = LineUtil.xAtY(start, stop, refPt.getY());
                    xGap = xDir * (xAnchor - refPt.getX());

                    if (refPt.getY() < start.getY()) {
                        yGap = start.getY() - refPt.getY();
                    } else if (refPt.getY() > stop.getY()) {
                        yGap = refPt.getY() - stop.getY();
                    } else {
                        yGap = 0;
                    }
                }

                hRel.setInOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), false);

                if (hRel.getGrade() >= hRel.getMinGrade()) {
                    hRel.setExtensionPoint(
                            new Point2D.Double(
                                    xAnchor,
                                    (yDir > 0) ? headBox.y
                                            : ((headBox.y + headBox.height) - 1)));
                    sig.addEdge(head, stemInter, hRel);

                    if (stemInter.isVip()) {
                        logger.info("VIP linked {} {} {} to {}", head, corner, hRel, stemInter);
                    }
                } else {
                    if (stemInter.isVip()) {
                        logger.info(
                                "VIP failed link {} to {} {} {}",
                                stemInter,
                                head,
                                corner,
                                hRel.getDetails());
                    }

                    hRel = null;
                }
            }

            return hRel;
        }

        //-----------------//
        // createStemInter //
        //-----------------//
        /**
         * (Try to) create stem interpretation with proper grade.
         *
         * @param items the sequence of items (seeds / chunks) that compose the stem
         * @return the proper stem interpretation or null if too weak
         */
        private StemInter createStemInter (List<Glyph> items)
        {
            Glyph stem = (items.size() == 1) ? items.get(0) : GlyphFactory.buildGlyph(items);
            stem = system.getSheet().getGlyphIndex().registerOriginal(stem);

            if (stem.isVip()) {
                logger.info("VIP createStemInter? {}", stem);
            }

            // Stem interpretation (if not yet present for this glyph)
            StemInter stemInter = getStemInter(stem);

            if (stemInter == null) {
//                GradeImpacts impacts = builder.getVerticalsBuilder().checkStem(stem);
//                double grade = impacts.getGrade();
//
//                if (grade >= StemInter.getMinGrade()) {
//                    stemInter = new StemInter(stem, impacts);
//                    sig.addVertex(stemInter);
//                    builder.getSystemStems().add(stemInter);
//                }
                stemInter = new StemInter(stem, 0.5);
                sig.addVertex(stemInter);
                builder.getSystemStems().add(stemInter);
            }

            return stemInter;
        }

        //----------//
        // crossing //
        //----------//
        /**
         * Compute the crossing point between a stem and a beam.
         *
         * @param stem the stem
         * @param beam the beam
         * @return the precise crossing point
         */
        private Point2D crossing (StemInter stem,
                                  AbstractBeamInter beam)
        {
            Point2D start = stem.getTop();
            Point2D stop = stem.getBottom();
            Line2D beamLimit = getLimit(beam);

            return LineUtil.intersection(start, stop, beamLimit.getP1(), beamLimit.getP2());
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
            return beam.getBorder(corner.vSide.opposite());
        }

        //-----------//
        // getLuArea //
        //-----------//
        /**
         * Define the lookup area on given corner, knowing the reference point of the
         * entity (head).
         * Global slope is used (plus and minus slopeMargin).
         *
         * @param the rather horizontal limit for the area, or null to use system limit
         * @return the lookup area
         */
        private Area getLuArea (Line2D limit)
        {
            final double slope = system.getSheet().getSkew().getSlope();
            final double dSlope = -xDir * yDir * params.slopeMargin;

            final Point2D outPt = getOutPoint();
            final Point2D inPt = getInPoint();

            // Look Up path, start by head horizontal segment
            final Path2D lu = new Path2D.Double();
            lu.moveTo(outPt.getX(), outPt.getY());
            lu.lineTo(inPt.getX(), inPt.getY());

            // Then segment away from head
            final double yLimit;
            if (limit == null) {
                // Use system limit
                final Rectangle systemBox = system.getBounds();
                yLimit = (yDir > 0) ? systemBox.getMaxY() : systemBox.getMinY();
            } else {
                // Use provided (beam) limit
                yLimit = LineUtil.yAtX(limit, refPt.getX());
            }

            final double dy = yLimit - outPt.getY();
            lu.lineTo(inPt.getX() + ((slope + dSlope) * dy), yLimit);
            lu.lineTo(outPt.getX() + ((slope - dSlope) * dy), yLimit);

            lu.closePath();

            // Attachment
            StringBuilder sb = new StringBuilder();
            sb.append((corner.vSide == TOP) ? "T" : "B");
            sb.append((corner.hSide == LEFT) ? "L" : "R");
            head.addAttachment(sb.toString(), lu);

            return new Area(lu);
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
         * Compute head reference point for this corner (the point where a stem could
         * be connected).
         * For best precision, we use the related shape descriptor.
         *
         * @return the refPt
         */
        private Point2D getReferencePoint ()
        {
            return head.getStemReferencePoint(corner.stemAnchor(), scale.getInterline());
        }

        //-----------//
        // getRunBox //
        //-----------//
        /**
         * Report the run box of the first or last run of the provided section
         * according to the desired side.
         *
         * @param section the section for which the side run is retrieved
         * @param side    the desired side
         * @return the run bounding box
         */
        private Rectangle getRunBox (Section section,
                                     HorizontalSide side)
        {
            final Run run = (xDir < 0) ? section.getFirstRun() : section.getLastRun();
            final int pos = (xDir < 0) ? section.getFirstPos() : section.getLastPos();

            return new Rectangle(pos, run.getStart(), 1, run.getLength());
        }

        //----------//
        // getInter //
        //----------//
        /**
         * Report the first stem interpretation if any for the glyph at hand.
         *
         * @param glyph the underlying glyph
         * @return the existing stem interpretation if any, or null
         */
        private StemInter getStemInter (Glyph glyph)
        {
            final List<StemInter> systemStems = builder.getSystemStems();

            for (ListIterator<StemInter> it = systemStems.listIterator(systemStems.size()); it
                    .hasPrevious();) {
                StemInter inter = it.previous();

                if (inter.getGlyph() == glyph) {
                    return inter;
                }
            }

            return null;
        }

        //---------------//
        // getTargetLine //
        //---------------//
        /**
         * Build the best possible target line.
         * First, we use (head) refPt and (distant) targetPt to define a theoretical line.
         * Then, we look for suitable seeds if any to refine the line.
         * The non-suitable seeds are removed from the collection.
         *
         * @return the best target line for stem, oriented from head to stem tail
         */
        private Line2D getTargetLine ()
        {
            // Theoretical line
            Line2D theory = new Line2D.Double(refPt, targetPt);

            // Look for stems seeds
            seeds = new ArrayList<>(Glyphs.intersectedGlyphs(neighborSeeds, area));

            if (!seeds.isEmpty()) {
                // Purge seeds that do not contribute to ordinate range
                // or that are too for abscissa-wise from theoretical line
                for (Iterator<Glyph> it = seeds.iterator(); it.hasNext();) {
                    Glyph seed = it.next();
                    int contrib = getContrib(seed.getBounds());

                    if (contrib == 0) {
                        it.remove();

                        continue;
                    }

                    Point2D seedCenter = seed.getCentroid();
                    double dist = theory.ptLineDist(seedCenter);

                    if (dist > params.maxSeedJitter) {
                        it.remove();
                    }
                }

                // In case of overlap, simply keep the most contributive
                List<Glyph> kept = new ArrayList<>();
                sortByContrib(seeds);

                StemLoop:
                for (Glyph seed : seeds) {
                    Rectangle stemBox = seed.getBounds();

                    for (Glyph k : kept) {
                        if (GeoUtil.yOverlap(stemBox, k.getBounds()) > 0) {
                            continue StemLoop;
                        }
                    }

                    // No overlap
                    kept.add(seed);
                }

                seeds.retainAll(kept);

                // Finally, define line based on seed(s) kept if any
                if (!seeds.isEmpty()) {
                    sortByDistance(seeds);

                    final Glyph s1 = seeds.get(0);
                    final Glyph s2 = seeds.get(seeds.size() - 1);

                    return new Line2D.Double(
                            (yDir > 0) ? s1.getStartPoint(VERTICAL) : s1.getStopPoint(VERTICAL),
                            (yDir > 0) ? s2.getStopPoint(VERTICAL) : s2.getStartPoint(VERTICAL));
                }
            }

            return theory;
        }

        //-------------//
        // getTargetPt //
        //-------------//
        /**
         * Compute the point where the (skewed) vertical from head reference point
         * crosses the provided limit.
         *
         * @param limit the end of the white space (a rather horizontal line)
         * @return the limit crossing point with skewed vertical at head reference point
         */
        private Point2D getTargetPt (Line2D limit)
        {
            final double slope = system.getSheet().getSkew().getSlope();
            final Point2D refPt2 = new Point2D.Double(
                    refPt.getX() - (100 * slope),
                    refPt.getY() + 100);

            return LineUtil.intersection(refPt, refPt2, limit.getP1(), limit.getP2());
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

        //--------------//
        // includeItems //
        //--------------//
        /**
         * Include the stem items, one after the other.
         * We may have insufficient clean value for first items (resulting in no intermediate
         * StemInter created) but we must go on.
         * <p>
         * If we have a target beam, let's accept long vertical gaps.
         *
         * @param items          the sequence of stem items, sorted by distance from head
         * @param refY           the ordinate of head ref point
         * @param fatHeadSection the fat head section if any
         * @return the list of StemInter instances built
         */
        private List<StemInter> includeItems (List<Glyph> items,
                                              double refY,
                                              Section fatHeadSection)
        {
            // Modulate gaps according to sheet quality and targeted beam
            double maxYGap = params.maxYGapPoor;
            double maxStemHeadGapY = params.maxStemHeadGapY;

            if (targetBeam != null) {
                maxYGap *= params.beamRatio;
                maxStemHeadGapY *= params.beamRatio;
            }

            double lastY = refY; // Current end of stem
            List<StemInter> allStemInters = new ArrayList<>();

            for (int i = 0; i < items.size(); i++) {
                Glyph item = items.get(i);
                Rectangle itemBox = item.getBounds();

                // Are we past the beam limit (if any)?
                if (getContrib(itemBox) == 0) {
                    break;
                }

                // Is gap with previous item acceptable?
                final int itemY = (yDir > 0) ? itemBox.y : ((itemBox.y + itemBox.height) - 1);
                final double itemStart = (yDir > 0) ? Math.max(itemY, refY)
                        : Math.min(itemY, refY);
                final double yGap = yDir * (itemStart - lastY);

                if (yGap > maxYGap) {
                    break; // Too large gap
                }

                if ((i == 0) && (yGap > maxStemHeadGapY)) {
                    break; // Initial item too far from head
                }

                // Check minimum stem extension from head
                double itemStop = itemY + (yDir * (itemBox.height - 1));
                lastY = (yDir > 0) ? Math.max(lastY, itemStop) : Math.min(lastY, itemStop);

                final double extension = Math.abs(lastY - refY);

                if (extension < params.minStemExtension) {
                    continue;
                }

                // OK, build a stem interpretation with all items so far
                List<Glyph> stemItems = items.subList(0, i + 1);
                StemInter stemInter = createStemInter(stemItems);

                if (stemInter != null) {
                    if (null != connectHeadStem(fatHeadSection, stemInter)) {
                        allStemInters.add(stemInter);
                    }
                }
            }

            return allStemInters;
        }

        //-------------------//
        // linkBeamsAndStems //
        //-------------------//
        /**
         * Try to build links between the provided beams and the provided stems.
         *
         * @param beamGroups groups of beam candidates
         * @param stems      stem candidates
         */
        private void linkBeamsAndStems (List<List<AbstractBeamInter>> beamGroups,
                                        List<StemInter> stems)
        {
            for (List<AbstractBeamInter> group : beamGroups) {
                AbstractBeamInter firstBeam = group.get(0);

                for (StemInter stem : stems) {
                    // Try to connect first beam & stem
                    BeamStemRelation rel = connectBeamStem(firstBeam, stem);

                    if (rel != null) {
                        // Extend stem connection till end of current beam group, if relevant
                        if (firstBeam.isGood() && (group.size() > 1)) {
                            for (AbstractBeamInter next : group.subList(1, group.size())) {
                                if (sig.getRelation(
                                        next,
                                        stem,
                                        BeamStemRelation.class) == null) {
                                    BeamStemRelation r = new BeamStemRelation();
                                    r.setBeamPortion(rel.getBeamPortion());

                                    Point2D crossPt = crossing(stem, next);
                                    r.setExtensionPoint(
                                            new Point2D.Double(
                                                    crossPt.getX(),
                                                    crossPt.getY() + (yDir * (next.getHeight()
                                                                                      - 1))));
                                    r.setGrade(rel.getGrade());
                                    sig.addEdge(next, stem, r);
                                }
                            }
                        }
                    }
                }
            }
        }

        //------------------//
        // lookupBeamGroups //
        //------------------//
        /**
         * Look for (groups of) beam interpretations in the lookup area.
         *
         * @param candidates provided collection of candidate beams
         * @return the list of groups, ordered by distance from head
         */
        private List<List<AbstractBeamInter>> lookupBeamGroups (List<Inter> candidates)
        {
            // Reject beam candidates which are not in corner direction
            // (this can happen because of beam bounding rectangle)
            for (Iterator<Inter> it = candidates.iterator(); it.hasNext();) {
                AbstractBeamInter b = (AbstractBeamInter) it.next();

                if ((yDir * (getTargetPt(getLimit(b)).getY() - refPt.getY())) <= 0) {
                    it.remove();
                }
            }

            // Sort candidates by distance from head
            Collections.sort(candidates, (Inter i1, Inter i2) -> {
                         AbstractBeamInter b1 = (AbstractBeamInter) i1;
                         AbstractBeamInter b2 = (AbstractBeamInter) i2;

                         return Double.compare(
                                 yDir * (getTargetPt(getLimit(b1)).getY() - refPt.getY()),
                                 yDir * (getTargetPt(getLimit(b2)).getY() - refPt.getY()));
                     });

            // Build the list of (groups of) beams
            List<List<AbstractBeamInter>> groups = new ArrayList<>();
            List<AbstractBeamInter> group = null;
            AbstractBeamInter prevBeam = null;
            boolean groupIsGood = false;

            for (Inter inter : candidates) {
                AbstractBeamInter beam = (AbstractBeamInter) inter;

                if (groups.isEmpty()) {
                    // Check if beam is far enough from head
                    final Point2D beamPt = getTargetPt(getLimit(beam));
                    final double distToBeam = yDir * (beamPt.getY() - refPt.getY());

                    if (distToBeam < params.minHeadBeamDistance) {
                        continue;
                    }
                }

                if (groupIsGood && areGroupCompatible(prevBeam, beam)) {
                    // Grow the current good group
                    group.add(beam);
                } else {
                    // Start a brand new group
                    groups.add(group = new ArrayList<>());
                    group.add(beam);
                    groupIsGood = beam.isGood();
                }

                prevBeam = beam;
            }

            return groups;
        }

        //--------------//
        // lookupChunks //
        //--------------//
        /**
         * Retrieve chunks of stems out of additional compatible sections (not part
         * of stem seeds) found in the corner.
         *
         * @param fatHeadSection (output) specific section, part of head rather than stem
         * @return the collection of chunks found
         */
        private List<Glyph> lookupChunks (Wrapper<Section> fatHeadSection)
        {
            // Look up suitable sections
            List<Section> sections = lookupSections(fatHeadSection);

            // Aggregate these sections into glyphs & check them
            List<SectionCompound> chunks = CompoundFactory.buildCompounds(
                    sections,
                    builder.getStemConstructor());

            // Remove useless glyphs and put wide glyphs apart
            List<SectionCompound> wides = new ArrayList<>();

            for (Iterator<SectionCompound> it = chunks.iterator(); it.hasNext();) {
                SectionCompound chunk = it.next();
                Rectangle chunkBox = chunk.getBounds();

                if (getContrib(chunkBox) == 0) {
                    it.remove();
                } else if (chunk.getWeight() < params.minChunkWeight) {
                    it.remove();
                } else {
                    int meanWidth = (int) Math.rint(
                            chunk.getMeanThickness(Orientation.VERTICAL));

                    if (meanWidth > params.maxStemThickness) {
                        wides.add(chunk);
                        it.remove();
                    }
                }
            }

            // For too wide chunks we just keep the biggest section
            if (!wides.isEmpty()) {
                for (SectionCompound wide : wides) {
                    List<Section> members = new ArrayList<>(wide.getMembers());
                    Collections.sort(members, Section.reverseWeightComparator);

                    SectionCompound compound = CompoundFactory.buildCompound(
                            Arrays.asList(members.get(0)),
                            builder.getStemConstructor());
                    chunks.add(compound);
                }
            }

            // Convert section compounds to glyphs
            List<Glyph> glyphs = new ArrayList<>(chunks.size());

            for (SectionCompound chunk : chunks) {
                glyphs.add(chunk.toGlyph(null));
            }

            return glyphs;
        }

        //----------------//
        // lookupSections //
        //----------------//
        /**
         * To complement stem seeds, look up for relevant sections in the lookup area
         * that could be part of a global stem.
         *
         * @param fatHeadSection (potential output) a thick section, part of head, that accounts
         *                       for stem range
         * @return the collection of additional sections found
         */
        private List<Section> lookupSections (Wrapper<Section> fatHeadSection)
        {
            // Horizontal line around refPt
            final Point2D outPt = getOutPoint();
            final Point2D inPt = getInPoint();
            final Line2D hLine = (corner.hSide == LEFT) ? new Line2D.Double(outPt, inPt)
                    : new Line2D.Double(inPt, outPt);
            final int refY = (int) Math.rint(refPt.getY());
            final List<Section> sections = new ArrayList<>();
            final List<Section> headSections = new ArrayList<>();

            // Widen head box with max stem width
            final Rectangle wideHeadBox = head.getBounds();
            wideHeadBox.grow(system.getSheet().getScale().getMaxStem(), 0);

            // Browse both vertical and horizontal sections in the system
            for (Collection<Section> collection : Arrays.asList(
                    system.getVerticalSections(),
                    system.getHorizontalSections())) {
                SectionLoop:
                for (Section section : collection) {
                    Rectangle sectBox = section.getBounds();

                    if (section.isVip()) {
                        logger.info("VIP {}", section);
                    }

                    // Check intersection at least
                    if (!area.intersects(sectBox)) {
                        continue;
                    }

                    // Containment is mandatory except for a head section
                    // (a section that intersects head glyph)
                    if (!area.contains(sectBox)) {
                        if (!sectBox.intersects(wideHeadBox) || !GeoUtil.yEmbraces(sectBox, refY)) {
                            continue;
                        }

                        // Section is likely to be part of head itself.
                        // Even if too thick, use part of its length as stem portion
                        // (if it does not overlap stem seeds)
                        if (section.isVertical() && (sectBox.width > params.maxStemThickness)) {
                            // Consider the touching run
                            Rectangle runBox = getRunBox(section, corner.hSide);

                            for (Glyph seed : seeds) {
                                if (GeoUtil.yOverlap(runBox, seed.getBounds()) > 0) {
                                    continue SectionLoop;
                                }
                            }

                            // Make sure this run is within area width
                            if (GeoUtil.xEmbraces(hLine, runBox.x)) {
                                // Use head section that brings best contribution
                                if (fatHeadSection.value != null) {
                                    Rectangle otherBox = getRunBox(fatHeadSection.value,
                                                                   corner.hSide);

                                    if (getContrib(runBox) > getContrib(otherBox)) {
                                        fatHeadSection.value = section;
                                    }
                                } else {
                                    fatHeadSection.value = section;
                                }
                            }

                            continue;
                        }

                        // A headSection must provide significant contribution
                        // otherwise it belongs to the head, not to the stem.
                        int sectContrib = getContrib(sectBox);

                        if (sectContrib < params.minHeadSectionContribution) {
                            logger.debug("Discarding tiny headSection {}", section);

                            continue;
                        }

                        headSections.add(section);
                    }

                    // Contraint section width <= stem width
                    if (sectBox.width > params.maxStemThickness) {
                        continue;
                    }

                    // A section which intersects an existing seed is useless
                    for (Glyph seed : seeds) {
                        if (GeoUtil.yOverlap(sectBox, seed.getBounds()) > 0) {
                            continue SectionLoop;
                        }
                    }

                    // Check section distance to target line
                    Point center = section.getCentroid();
                    double dist = targetLine.ptLineDist(center);

                    if (dist <= params.maxSectionJitter) {
                        sections.add(section);
                    }
                }
            }

            // Handle overlap between standard sections and fatHeadSection if any,
            // by keeping the most contributive one
            if (fatHeadSection.value != null) {
                final Rectangle runBox = getRunBox(fatHeadSection.value, corner.hSide);
                final int runContrib = getContrib(runBox);

                for (Iterator<Section> it = sections.iterator(); it.hasNext();) {
                    final Section section = it.next();
                    final Rectangle sctBox = section.getBounds();

                    if (GeoUtil.yOverlap(runBox, sctBox) > 0) {
                        if (getContrib(sctBox) <= runContrib) {
                            it.remove();
                        } else {
                            logger.debug("Cancelling fatHeadSection {}", fatHeadSection);
                            fatHeadSection.value = null;

                            break;
                        }
                    }
                }
            }

            // Handle the case of several head sections that might result in a too thick glyph
            headSections.retainAll(sections);

            if (headSections.size() > 1) {
                // Keep only the most contributive section
                Section bestSection = null;
                int bestContrib = Integer.MIN_VALUE;

                for (Section section : headSections) {
                    int contrib = getContrib(section.getBounds());

                    if (contrib > bestContrib) {
                        bestContrib = contrib;
                        bestSection = section;
                    }
                }

                sections.removeAll(headSections);
                headSections.clear();
                headSections.add(bestSection);
                sections.addAll(headSections);
            }

            return sections;
        }

        //---------------//
        // sortByContrib //
        //---------------//
        /**
         * Sort stem items by their decreasing contribution.
         */
        private void sortByContrib (List<Glyph> glyphs)
        {
            Collections.sort(glyphs, (Glyph o1, Glyph o2) -> {
                         return Integer.signum(getContrib(o2.getBounds()) - getContrib(o1
                                 .getBounds()));
                     });
        }

        //----------------//
        // sortByDistance //
        //----------------//
        /**
         * Sort stem items by their increasing vertical distance from head.
         */
        private void sortByDistance (List<Glyph> glyphs)
        {
            Collections.sort(glyphs, (yDir > 0) ? Glyphs.byOrdinate : Glyphs.byReverseBottom);
        }
    }
}
