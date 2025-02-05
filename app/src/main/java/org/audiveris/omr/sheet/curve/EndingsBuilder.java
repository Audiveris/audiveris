//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   E n d i n g s B u i l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import static org.audiveris.omr.run.Orientation.VERTICAL;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.glyph.dynamic.FilamentFactory;
import org.audiveris.omr.glyph.dynamic.StraightFilament;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.lag.Sections;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Profiles;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.SegmentInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.relation.EndingBarRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.NoExclusion;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class <code>EndingsBuilder</code> retrieves the endings out of segments found in sheet
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
     * Creates a new EndingsBuilder object.
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
     * <p>
     * Each side is related to a distinct barline.
     */
    public void buildEndings ()
    {
        List<SegmentInter> segments = curves.getSegments();

        for (SegmentInter segment : segments) {
            processSegment(segment);
        }
    }

    //------------//
    // buildGlyph //
    //------------//
    /**
     * Build the underlying glyph for the ending defined by segment and legs.
     *
     * @param segment  ending horizontal segment
     * @param leftLeg  left vertical filament, perhaps null
     * @param rightLeg right vertical filament, perhaps null
     * @return the glyph built
     */
    private Glyph buildGlyph (Glyph baseGlyph,
                              Filament leftLeg,
                              Filament rightLeg)
    {
        final List<Glyph> parts = new ArrayList<>(3);
        parts.add(baseGlyph);

        if (leftLeg != null) {
            parts.add(leftLeg.toGlyph(null));
        }

        if (rightLeg != null) {
            parts.add(rightLeg.toGlyph(null));
        }

        return sheet.getGlyphIndex().registerOriginal(GlyphFactory.buildGlyph(parts));
    }

    //-------------//
    // createInter //
    //-------------//
    private EndingInter createInter (Staff staff,
                                     boolean split,
                                     SegmentInter segment,
                                     Line2D line,
                                     Filament leftLeg,
                                     Filament rightLeg,
                                     GradeImpacts impacts)
    {
        // Create ending inter

        final Line2D leftLine = (leftLeg == null) ? null
                : new Line2D.Double(leftLeg.getStartPoint(), leftLeg.getStopPoint());
        final Line2D rightLine = (rightLeg == null) ? null
                : new Line2D.Double(rightLeg.getStartPoint(), rightLeg.getStopPoint());
        final EndingInter endingInter = new EndingInter(
                line,
                leftLine,
                rightLine,
                line.getBounds(),
                impacts);
        endingInter.setStaff(staff);

        // Underlying glyph
        final Glyph lineGlyph = split ? subGlyph(segment, line) : segment.getGlyph();
        endingInter.setGlyph(buildGlyph(lineGlyph, leftLeg, rightLeg));

        final SIGraph sig = staff.getSystem().getSig();
        sig.addVertex(endingInter);

        // Ending text?
        grabSentences(endingInter);

        return endingInter;
    }

    //--------------//
    // createInters //
    //--------------//
    /**
     * Try to create ending inter(s) from the provided segment, but with yet no link
     * attempt for left or right barline.
     * <p>
     * Each ending is created perhaps with left leg and perhaps with right leg.
     * Sentence number is searched for also.
     *
     * @param segment the horizontal segment
     * @param system  the containing system
     * @return null or the list of created ending inters inserted to SIG,
     *         with sentence link if possible
     */
    private List<EndingInter> createInters (SegmentInter segment,
                                            SystemInfo system)
    {
        final SegmentInfo seg = segment.getInfo();
        final Point leftEnd = seg.getEnd(true);
        final Point rightEnd = seg.getEnd(false);

        // Length
        final double length = seg.getXLength();

        if (length < params.minLengthLow) {
            return null;
        }

        // Slope
        final Line2D line = new Line2D.Double(leftEnd, rightEnd);
        final double slope = Math.abs(LineUtil.getSlope(line) - sheet.getSkew().getSlope());

        if (slope > params.maxSlope) {
            return null;
        }

        // Consider the staff just below the segment
        final Staff staff = system.getStaffAtOrBelow(leftEnd);

        if (staff == null) {
            return null;
        }

        // Check minimum vertical gap with staff below
        final Point center = segment.getCenter();
        final double yGap = staff.distanceTo(center);

        if (yGap < params.minGapFromStaff) {
            return null;
        }

        // Check with related measure length
        final Measure measure = staff.getPart().getMeasureAt(center, staff);

        if (measure == null) {
            return null;
        }

        // Accept a lower ratio for first measure in system (due to room for clef + key? + time?)
        final Constant.Ratio minRatio = (measure.getStack() == system.getFirstStack())
                ? constants.minFirstMeasureRatio
                : constants.minMeasureRatio;

        if (length < (measure.getWidth() * minRatio.getValue())) {
            logger.debug("Ending {} too short compared with related {}", segment, measure);

            return null;
        }

        GradeImpacts segImp = segment.getImpacts();
        double straight = segImp.getGrade() / segImp.getIntrinsicRatio();
        GradeImpacts impacts = new EndingInter.Impacts(
                straight,
                1 - (slope / params.maxSlope),
                (length - params.minLengthLow) / (params.minLengthHigh - params.minLengthLow));

        if (impacts.getGrade() < EndingInter.getMinGrade()) {
            return null;
        }

        // Left leg (optional)
        Filament leftLeg = lookupLeg(seg, seg.getEnd(true), staff);

        // Right leg (optional)
        Filament rightLeg = lookupLeg(seg, seg.getEnd(false), staff);

        // Middle legs if any (each would split segment line into separate endings)
        final List<Filament> middleLegs = getMiddleLegs(segment, staff);

        if (middleLegs.isEmpty()) {
            final EndingInter endingInter = createInter(
                    staff,
                    false,
                    segment,
                    line,
                    leftLeg,
                    rightLeg,
                    impacts);
            return Arrays.asList(endingInter);
        } else {
            final SIGraph sig = staff.getSystem().getSig();
            final List<EndingInter> created = new ArrayList<>();
            Point2D lastPoint = leftEnd;
            Filament lastLeg = leftLeg;
            Inter lastCreated = null;

            for (Filament midLeg : middleLegs) {
                // Define an ending from lastPoint to midLeg
                final Line2D l = new Line2D.Double(lastPoint, midLeg.getStartPoint());
                final EndingInter ending = createInter(
                        staff,
                        true,
                        segment,
                        l,
                        lastLeg,
                        midLeg,
                        impacts);
                created.add(ending);

                lastPoint = midLeg.getStartPoint();
                lastLeg = midLeg;

                if (lastCreated != null)
                    sig.addEdge(lastCreated, ending, new NoExclusion());

                lastCreated = ending;
            }

            // Terminating one
            final Line2D l = new Line2D.Double(lastPoint, rightEnd);
            final EndingInter ending = createInter(
                    staff,
                    true,
                    segment,
                    l,
                    lastLeg,
                    rightLeg,
                    impacts);
            created.add(ending);
            if (lastCreated != null)
                sig.addEdge(lastCreated, ending, new NoExclusion());

            return created;
        }
    }

    //---------------//
    // getMiddleBars //
    //---------------//
    /**
     * Report the abscissa-ordered sequence of StaffBarlineInter located within segment.
     *
     * @param segment the candidate segment
     * @param staff   the related staff
     * @return the list of middle bars, perhaps empty
     */
    private List<StaffBarlineInter> getMiddleBars (SegmentInter segment,
                                                   Staff staff)
    {
        final List<StaffBarlineInter> found = new ArrayList<>();
        final SegmentInfo seg = segment.getInfo();
        final Point leftEnd = seg.getEnd(true);
        final Point rightEnd = seg.getEnd(false);
        final SystemInfo system = staff.getSystem();
        final List<Inter> systemBars = system.getSig().inters(StaffBarlineInter.class);

        for (Inter ib : systemBars) {
            final StaffBarlineInter sBar = (StaffBarlineInter) ib;
            final Point2D center = sBar.getReferenceCenter();

            if ((center.getX() > leftEnd.x + params.maxBarShift) && (center.getX() < rightEnd.x
                    - params.maxBarShift)) {
                found.add(sBar);
            }
        }

        Collections.sort(found, Inters.byCenterAbscissa);

        return found;
    }

    //---------------//
    // getMiddleLegs //
    //---------------//
    private List<Filament> getMiddleLegs (SegmentInter segment,
                                          Staff staff)
    {
        final List<Filament> found = new ArrayList<>();
        final SegmentInfo seg = segment.getInfo();
        final Point leftEnd = seg.getEnd(true);
        final Point rightEnd = seg.getEnd(false);

        for (StaffBarlineInter sBar : getMiddleBars(segment, staff)) {
            final Point center = PointUtil.rounded(sBar.getReferenceCenter());
            final double ratio = (center.x - leftEnd.x) / (double) (rightEnd.x - leftEnd.x);
            final Point segPt = new Point(
                    center.x,
                    (int) Math.rint(leftEnd.y + ratio * (rightEnd.y - leftEnd.y)));
            final Filament leg = lookupLeg(seg, segPt, staff);

            if (leg != null)
                found.add(leg);
        }

        return found;

    }
    //~ Inner Classes ------------------------------------------------------------------------------

    //---------------//
    // grabSentences //
    //---------------//
    /**
     * Try to retrieve the sentence(s) that should lie in left corner of provided ending.
     *
     * @param ending provided ending
     */
    private void grabSentences (EndingInter ending)
    {
        for (Link link : ending.lookupSentenceLinks()) {
            final SentenceInter sentence = (SentenceInter) link.partner;
            ending.getSig().addEdge(ending, sentence, link.relation);

            final String number = ending.getNumber();

            if (sentence.getValue().equals(number)) {
                sentence.setRole(TextRole.EndingNumber);
            } else {
                sentence.setRole(TextRole.EndingText);
            }
        }
    }

    //-----------//
    // lookupLeg //
    //-----------//
    /**
     * Look for a vertical leg near the chosen point on the horizontal segment.
     *
     * @param seg   the horizontal segment
     * @param segPt chosen point on segment
     * @param staff related staff
     * @return the best seed found or null if none.
     */
    private Filament lookupLeg (SegmentInfo seg,
                                Point segPt,
                                Staff staff)
    {
        Rectangle box = new Rectangle(
                segPt.x - params.maxLegXGap,
                segPt.y + params.legYMargin,
                2 * params.maxLegXGap,
                staff.getFirstLine().yAt(segPt.x) - segPt.y - (2 * params.legYMargin));

        SystemInfo system = staff.getSystem();
        Set<Section> sections = Sections.intersectedSections(box, system.getVerticalSections());
        Scale scale = sheet.getScale();
        FilamentFactory<StraightFilament> factory = new FilamentFactory<>(
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

            if ((fil.getLength(VERTICAL) < params.minLegLow) || ((fil.getStartPoint().getY()
                    - segPt.y) > params.maxLegYGap)) {
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
            double dx = top.getX() - segPt.getX();
            double dy = top.getY() - segPt.getY();
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
     * We first create an ending inter with perhaps left leg and perhaps right leg.
     * We then try to link left and right ends with a barline.
     * <p>
     * TODO: Grab text (such as '1.' or '1,2' etc) located in left corner of the ending.
     * Perhaps force local OCR processing there (or symbol extraction/recognition)?
     *
     * @param segment the horizontal segment
     */
    private void processSegment (SegmentInter segment)
    {
        final SegmentInfo seg = segment.getInfo();
        final Point leftEnd = seg.getEnd(true);
        final Point rightEnd = seg.getEnd(false);

        // Relevant system(s)
        final List<SystemInfo> systems = sheet.getSystemManager().getSystemsOf(leftEnd, null);
        systems.retainAll(sheet.getSystemManager().getSystemsOf(rightEnd, null));

        for (SystemInfo system : systems) {
            final List<EndingInter> endings = createInters(segment, system);

            if (endings != null) {
                for (EndingInter ending : endings) {
                    final Collection<Link> links = ending.searchLinks(system);
                    boolean abnormal = true;

                    for (Link link : links) {
                        final EndingBarRelation ebRel = (EndingBarRelation) link.relation;
                        final HorizontalSide side = ebRel.getEndingSide();

                        if (side == HorizontalSide.LEFT) {
                            abnormal = false;
                        }

                        link.applyTo(ending);
                    }

                    ending.setAbnormal(abnormal);
                }
            }
        }
    }

    //----------//
    // subGlyph //
    //----------//
    private Glyph subGlyph (SegmentInter segment,
                            Line2D line)
    {
        final RunTable table = segment.getGlyph().getRunTable();
        final Rectangle bounds = segment.getGlyph().getBounds();
        final int x1 = (int) Math.ceil(line.getX1());
        final int x2 = (int) Math.floor(line.getX2());
        final int i1 = x1 - bounds.x;
        final int i2 = x2 - bounds.x;

        final RunTable t = new RunTable(Orientation.VERTICAL, x2 - x1 + 1, table.getHeight());
        for (int i = i1; i <= i2; i++) {
            final Iterator<Run> it = table.iterator(i);
            if (it.hasNext()) {
                t.addRun(i - i1, it.next());
            }
        }

        final Glyph sg = sheet.getGlyphIndex().registerOriginal(new Glyph(x1, bounds.y, t));
        return sg;
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Ratio minMeasureRatio = new Constant.Ratio(
                0.8,
                "Minimum ending length as ratio of related measure length");

        private final Constant.Ratio minFirstMeasureRatio = new Constant.Ratio(
                0.6,
                "Minimum ending length as ratio of related measure length, for first in system");

        private final Scale.Fraction minLengthLow = new Scale.Fraction(
                6,
                "Low minimum ending length");

        private final Scale.Fraction minLengthHigh = new Scale.Fraction(
                10,
                "High minimum ending length");

        private final Scale.Fraction minLegLow = new Scale.Fraction(1.0, "Low minimum leg length");

        private final Scale.Fraction minGapFromStaff = new Scale.Fraction(
                2.5,
                "Minimum vertical gap between ending line and staff below");

        private final Scale.Fraction legYMargin = new Scale.Fraction(
                0.25,
                "Vertical margin for leg lookup area");

        private final Scale.Fraction maxLegXGap = new Scale.Fraction(
                0.5,
                "Maximum abscissa gap between ending and leg");

        private final Scale.Fraction maxLegYGap = new Scale.Fraction(
                0.5,
                "Maximum ordinate gap between ending and leg");

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
        final int minLengthLow;

        final int minLengthHigh;

        final int minLegLow;

        final int minGapFromStaff;

        final int legYMargin;

        final int maxLegXGap;

        final int maxLegYGap;

        final int maxBarShift;

        final double maxSlope;

        Parameters (Scale scale)
        {
            minLengthLow = scale.toPixels(constants.minLengthLow);
            minLengthHigh = scale.toPixels(constants.minLengthHigh);
            minLegLow = scale.toPixels(constants.minLegLow);
            minGapFromStaff = scale.toPixels(constants.minGapFromStaff);
            legYMargin = scale.toPixels(constants.legYMargin);
            maxLegXGap = scale.toPixels(constants.maxLegXGap);
            maxLegYGap = scale.toPixels(constants.maxLegYGap);
            maxBarShift = scale.toPixels(EndingBarRelation.getXGapMaximum(Profiles.STRICT));
            maxSlope = constants.maxSlope.getValue();

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
