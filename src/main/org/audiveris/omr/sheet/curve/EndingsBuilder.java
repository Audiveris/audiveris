//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   E n d i n g s B u i l d e r                                  //
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
package org.audiveris.omr.sheet.curve;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.glyph.dynamic.FilamentFactory;
import org.audiveris.omr.glyph.dynamic.StraightFilament;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.lag.Sections;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.LineUtil;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.SegmentInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.relation.EndingBarRelation;
import org.audiveris.omr.sig.relation.EndingSentenceRelation;
import org.audiveris.omr.util.Dumping;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class {@code EndingsBuilder} retrieves the endings out of segments found in sheet
 * skeleton.
 *
 * @author Hervé Bitteur
 */
public class EndingsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(EndingsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related sheet. */
    @Navigable(false)
    protected final Sheet sheet;

    /** Curves environment. */
    protected final Curves curves;

    /** Scale-dependent parameters. */
    private final Parameters params;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new WedgesBuilder object.
     *
     * @param curves curves environment
     */
    public EndingsBuilder (Curves curves)
    {
        this.curves = curves;
        sheet = curves.getSheet();
        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // buildEndings //
    //--------------//
    /**
     * Retrieve the endings among the sheet segment curves.
     * Endings are long horizontal segments, with a downward leg on the left side and optionally
     * another leg on the right side.
     * Each side is related to a distinct bar line.
     */
    public void buildEndings ()
    {
        List<SegmentInter> segments = curves.getSegments();

        for (SegmentInter segment : segments) {
            processSegment(segment);
        }
    }

    //--------------//
    // grabSentence //
    //--------------//
    /**
     * Try to retrieve the text that should lie in the left corner of provided ending.
     *
     * @param ending provided ending
     */
    private void grabSentence (EndingInter ending)
    {
        Rectangle box = ending.getBounds();
        SIGraph sig = ending.getSig();
        List<SentenceInter> found = new ArrayList<SentenceInter>();
        List<Inter> systemSentences = sig.inters(SentenceInter.class);

        for (Inter si : systemSentences) {
            ///if (box.intersects(si.getBounds())) {
            if (box.contains(si.getBounds())) {
                found.add((SentenceInter) si);
            }
        }

        if (!found.isEmpty()) {
            Collections.sort(found, Inters.byFullAbscissa);

            SentenceInter sentence = found.get(0);
            sig.addEdge(ending, sentence, new EndingSentenceRelation());
        }
    }

    //-----------//
    // lookupBar //
    //-----------//
    /**
     * Look for a bar line vertically aligned with the ending side.
     * <p>
     * It is not very important to select a precise bar line within a group, since for left end we
     * choose the right-most bar and the opposite for right end.
     * We simply have to make sure that the lookup area is wide enough.
     * <p>
     * An ending which starts a staff may have its left side after the clef and key signature, which
     * means far after the starting barline (if any).
     * Perhaps we should consider the staff header in such case.
     *
     * @param seg        the horizontal segment
     * @param reverse    which side is at stake
     * @param staff      related staff
     * @param systemBars the collection of bar lines in the containing system
     * @return the selected bar line, or null if none
     */
    private BarlineInter lookupBar (SegmentInfo seg,
                                    boolean reverse,
                                    Staff staff,
                                    List<Inter> systemBars)
    {
        Point end = seg.getEnd(reverse);
        Rectangle box = new Rectangle(end);
        box.grow(params.maxBarShift, 0);
        box.height = staff.getLastLine().yAt(end.x) - end.y;

        List<Inter> bars = SIGraph.intersectedInters(systemBars, GeoOrder.NONE, box);
        Collections.sort(bars, Inters.byAbscissa);

        if (bars.isEmpty()) {
            return null;
        }

        return (BarlineInter) bars.get(reverse ? (bars.size() - 1) : 0);
    }

    //-----------//
    // lookupLeg //
    //-----------//
    /**
     * Look for a vertical leg on the desired side of the horizontal segment.
     *
     * @param seg     the horizontal segment
     * @param reverse the desired side
     * @param staff   related staff
     * @return the best seed found or null if none.
     */
    private Filament lookupLeg (SegmentInfo seg,
                                boolean reverse,
                                Staff staff)
    {
        Point end = seg.getEnd(reverse);
        Rectangle box = new Rectangle(
                end.x - params.maxLegXGap,
                end.y + params.legYMargin,
                2 * params.maxLegXGap,
                staff.getFirstLine().yAt(end.x) - end.y - (2 * params.legYMargin));

        SystemInfo system = staff.getSystem();
        Set<Section> sections = Sections.intersectedSections(
                box,
                system.getVerticalSections());
        Scale scale = sheet.getScale();
        FilamentFactory<StraightFilament> factory = new FilamentFactory<StraightFilament>(
                scale,
                sheet.getFilamentIndex(),
                VERTICAL,
                StraightFilament.class);

        // Adjust factory parameters
        factory.setMaxThickness(
                (int) Math.ceil(sheet.getScale().getMaxStem() * constants.stemRatio.getValue()));
        factory.setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);
        factory.setMaxOverlapSpace(constants.maxOverlapSpace);
        factory.setMaxCoordGap(constants.maxCoordGap);

        if (system.getId() == 1) {
            factory.dump("EndingsBuilder factory");
        }

        // Retrieve candidates
        List<StraightFilament> filaments = factory.retrieveFilaments(sections);

        // Purge filaments
        for (Iterator<StraightFilament> it = filaments.iterator(); it.hasNext();) {
            StraightFilament fil = it.next();

            if ((fil.getLength(VERTICAL) < params.minLegLow)
                || ((fil.getStartPoint().getY() - end.y) > params.maxLegYGap)) {
                it.remove();
            }
        }

        if (filaments.isEmpty()) {
            return null;
        }

        // Choose the seed whose top end is closest to segment end
        Filament bestFil = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Filament filament : filaments) {
            Point2D top = filament.getStartPoint();
            double dx = top.getX() - end.getX();
            double dy = top.getY() - end.getY();
            double distSq = (dx * dx) + (dy * dy);

            if ((bestFil == null) || (bestDistSq > distSq)) {
                bestDistSq = distSq;
                bestFil = filament;
            }
        }

        return bestFil;
    }

    //----------------//
    // processSegment //
    //----------------//
    /**
     * Check the horizontal segment for being an ending.
     * <p>
     * TODO: Grab text (such as '1.' or '1,2' etc) located in left corner of the ending.
     * Perhaps force local OCR processing there (or symbol extraction/recognition)?
     *
     * @param segment the horizontal segment
     */
    private void processSegment (SegmentInter segment)
    {
        // Check segment characteristics: length, slope, bar line alignments, legs.
        SegmentInfo seg = segment.getInfo();
        Point leftEnd = seg.getEnd(true);
        Point rightEnd = seg.getEnd(false);

        // Length
        double length = seg.getXLength();

        if (length < params.minLengthLow) {
            return;
        }

        // Slope
        Line2D line = new Line2D.Double(leftEnd, rightEnd);
        double slope = Math.abs(LineUtil.getSlope(line) - sheet.getSkew().getSlope());

        if (slope > params.maxSlope) {
            return;
        }

        // Relevant system(s)
        List<SystemInfo> systems = sheet.getSystemManager().getSystemsOf(leftEnd, null);
        systems.retainAll(sheet.getSystemManager().getSystemsOf(rightEnd, null));

        for (SystemInfo system : systems) {
            SIGraph sig = system.getSig();

            // Consider the staff just below the segment
            Staff staff = system.getStaffAtOrBelow(leftEnd);

            if (staff == null) {
                continue;
            }

            List<Inter> systemBars = sig.inters(BarlineInter.class);

            // Left leg (mandatory)
            Filament leftLeg = lookupLeg(seg, true, staff);

            if (leftLeg == null) {
                continue;
            }

            // Left bar (or header)
            BarlineInter leftBar = lookupBar(seg, true, staff, systemBars);
            final Double leftDist;

            if (leftBar == null) {
                // Check the special case of a staff start (with header?, with no barline?)
                MeasureStack firstStack = system.getFirstMeasureStack();
                Measure firstMeasure = firstStack.getMeasureAt(staff);

                if (leftEnd.x >= firstMeasure.getAbscissa(RIGHT, staff)) {
                    continue; // segment starts after end of first measure
                }

                leftDist = null;
            } else {
                leftDist = Math.abs(LineUtil.xAtY(leftBar.getMedian(), leftEnd.y) - leftEnd.x);
            }

            // Right leg (optional)
            Filament rightLeg = lookupLeg(seg, false, staff);

            // Right bar
            BarlineInter rightBar = lookupBar(seg, false, staff, systemBars);

            if (rightBar == null) {
                continue;
            }

            double rightDist = Math.abs(
                    LineUtil.xAtY(rightBar.getMedian(), rightEnd.y) - rightEnd.x);

            // Create ending inter
            GradeImpacts segImp = segment.getImpacts();
            double straight = segImp.getGrade() / segImp.getIntrinsicRatio();
            double leftImpact = (leftDist != null) ? (1 - (leftDist / params.maxBarShift)) : 0.5;

            GradeImpacts impacts = new EndingInter.Impacts(
                    straight,
                    1 - (slope / params.maxSlope),
                    (length - params.minLengthLow) / (params.minLengthHigh - params.minLengthLow),
                    leftImpact,
                    1 - (rightDist / params.maxBarShift));

            if (impacts.getGrade() >= EndingInter.getMinGrade()) {
                Line2D leftLine = new Line2D.Double(
                        leftLeg.getStartPoint(),
                        leftLeg.getStopPoint());
                Line2D rightLine = (rightLeg == null) ? null
                        : new Line2D.Double(
                                rightLeg.getStartPoint(),
                                rightLeg.getStopPoint());
                EndingInter endingInter = new EndingInter(
                        segment,
                        line,
                        leftLine,
                        rightLine,
                        segment.getBounds(),
                        impacts);
                sig.addVertex(endingInter);

                Scale scale = sheet.getScale();

                if (leftBar != null) {
                    sig.addEdge(
                            endingInter,
                            leftBar,
                            new EndingBarRelation(LEFT, scale.pixelsToFrac(leftDist)));
                }

                sig.addEdge(
                        endingInter,
                        rightBar,
                        new EndingBarRelation(RIGHT, scale.pixelsToFrac(rightDist)));

                // Ending text?
                grabSentence(endingInter);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction minLengthLow = new Scale.Fraction(
                6,
                "Low minimum ending length");

        private final Scale.Fraction minLengthHigh = new Scale.Fraction(
                10,
                "High minimum ending length");

        private final Scale.Fraction minLegLow = new Scale.Fraction(
                1.0,
                "Low minimum leg length");

        private final Scale.Fraction minLegHigh = new Scale.Fraction(
                2.5,
                "High minimum leg length");

        private final Scale.Fraction legYMargin = new Scale.Fraction(
                0.25,
                "Vertical margin for leg lookup area");

        private final Scale.Fraction maxLegXGap = new Scale.Fraction(
                0.5,
                "Maximum abscissa gap between ending and leg");

        private final Scale.Fraction maxLegYGap = new Scale.Fraction(
                0.5,
                "Maximum ordinate gap between ending and leg");

        private final Scale.Fraction maxBarShift = new Scale.Fraction(
                2.0,
                "High maximum abscissa shift between ending and barline");

        private final Constant.Double maxSlope = new Constant.Double(
                "tangent",
                0.02,
                "Maximum ending slope");

        private final Constant.Ratio stemRatio = new Constant.Ratio(
                1.4,
                "Maximum leg thickness as ratio of stem thickness");

        private final Scale.LineFraction maxOverlapDeltaPos = new Scale.LineFraction(
                1.0,
                "Maximum delta position between two overlapping filaments");

        private final Scale.LineFraction maxOverlapSpace = new Scale.LineFraction(
                0.3,
                "Maximum space between overlapping filaments");

        private final Scale.Fraction maxCoordGap = new Scale.Fraction(
                0.5,
                "Maximum delta coordinate for a gap between filaments");
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

        final int minLengthLow;

        final int minLengthHigh;

        final int minLegLow;

        final int minLegHigh;

        final int legYMargin;

        final int maxLegXGap;

        final int maxLegYGap;

        final int maxBarShift;

        final double maxSlope;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            minLengthLow = scale.toPixels(constants.minLengthLow);
            minLengthHigh = scale.toPixels(constants.minLengthHigh);
            minLegLow = scale.toPixels(constants.minLegLow);
            minLegHigh = scale.toPixels(constants.minLegHigh);
            legYMargin = scale.toPixels(constants.legYMargin);
            maxLegXGap = scale.toPixels(constants.maxLegXGap);
            maxLegYGap = scale.toPixels(constants.maxLegYGap);
            maxBarShift = scale.toPixels(constants.maxBarShift);
            maxSlope = constants.maxSlope.getValue();

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
