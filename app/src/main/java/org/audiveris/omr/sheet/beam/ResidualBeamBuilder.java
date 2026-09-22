//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              R e s i d u a l B e a m B u i l d e r                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.sheet.beam;

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class <code>ResidualBeamBuilder</code> recovers a beam that {@link SpotsBuilder} never even
 * produced a candidate glyph for, and evicts the spurious note head its ink got mistaken for.
 * <p>
 * {@link SpotsBuilder}'s spot pipeline (strip stem-width runs from the binary "no-staff" image,
 * blur, morphologically close with a disk sized for a <b>typical</b> beam, then a single global
 * re-threshold) is tuned for near-horizontal, reasonably long beam strokes. Confirmed on a real
 * score: a short, steep/diagonal 16th-note beam connecting two closely-spaced stems can be smoothed
 * away entirely by that pipeline before it ever becomes spot ink -- not merged with a neighboring
 * beam and rejected as too thick (the case {@link BeamStructure#splitLines} already handles), but
 * never turned into a {@code BEAM_SPOT} glyph in the first place. Grey-scale inspection of the same
 * region shows the two beam strokes, and the gap between them, are still visible in the original
 * scan -- the information is lost upstream of anything beam-specific.
 * <p>
 * With no beam Inter ever created there, {@link org.audiveris.omr.sheet.note.NoteHeadsBuilder}'s
 * "skip locations already claimed by a good Inter" protection never engages -- there is nothing to
 * protect against. The merged ink then matches a head template and becomes a spurious
 * {@link HeadInter}, sitting near the stem's <b>far/beam-side</b> tip rather than the near/staff-side
 * end where the stem's real, primary head already sits -- much too far from that primary head to be
 * an ordinary stacked-chord notehead (which would sit within about an interline of it), yet right at
 * the tip where nothing beyond it (no beam, no flag) explains why a head would be there at all.
 * Confirmed on a real score: the spurious head's own grade was clearly the weakest of every head
 * nearby, and its distance from the stem's genuine head (roughly 3.5 interlines) was far beyond any
 * plausible same-chord spacing.
 * <p>
 * This targeted pass runs once per system, after stems and heads have settled (see call site in
 * {@link org.audiveris.omr.step.ReductionStep#doEpilog}): for every stem with a head sitting close
 * to its outer/tail tip yet far from its primary (near-end) head, and with no beam or flag already
 * attached to that stem at all, it looks for a geometrically compatible neighboring stem --
 * horizontal extent bounded by the two stems themselves (median reaching each stem's own
 * centerline, exactly as {@link BeamStemRelation#checkLink} expects of any real beam-stem
 * attachment; the narrower gap between their facing edges is what actually gets scanned for ink)
 * -- confirms real ink is actually present there in the grey source (never fabricating a beam from
 * an empty gap), and only then reconstructs the beam and evicts the spurious head whose area it
 * now explains. The recovery is all-or-nothing: if the suspect head's own grade turns out too
 * good to plausibly be that spurious head, nothing is created at all -- a beam left behind
 * alongside a kept head would be a new inconsistency, not a fix.
 */
public class ResidualBeamBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ResidualBeamBuilder.class);

    //~ Instance fields ------------------------------------------------------------------------------

    private final SystemInfo system;

    private final Scale scale;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>ResidualBeamBuilder</code> object.
     *
     * @param system the system to process
     */
    public ResidualBeamBuilder (SystemInfo system)
    {
        this.system = system;
        this.scale = system.getSheet().getScale();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // process //
    //---------//
    /**
     * Look for a residual (never-spotted) beam among this system's stems.
     */
    public void process ()
    {
        final SIGraph sig = system.getSig();
        final List<StemInter> stems = new ArrayList<>();

        for (Inter inter : sig.inters(StemInter.class)) {
            stems.add((StemInter) inter);
        }

        // Every suspect head found, computed once up front: pairing below is O(n^2) over
        // suspects only, not over every stem in the system.
        final List<Suspect> suspects = new ArrayList<>();

        for (StemInter stem : stems) {
            final Suspect suspect = findSuspect(stem);

            if (suspect != null) {
                suspects.add(suspect);
            }
        }

        final Set<StemInter> resolved = new LinkedHashSet<>();

        for (Suspect suspect : suspects) {
            if (resolved.contains(suspect.stem)) {
                continue; // Already paired (and possibly already recovered) as someone's neighbor
            }

            final StemInter neighbor = findNeighbor(suspect, stems, resolved);

            if (neighbor == null) {
                continue;
            }

            if (attemptRecovery(suspect, neighbor)) {
                resolved.add(suspect.stem);
                resolved.add(neighbor);
            }
        }
    }

    //-------------//
    // findSuspect //
    //-------------//
    /**
     * Check whether the given stem carries a head sitting close to its outer/tail tip yet far
     * from its primary (near-end) head, with no beam or flag already attached to explain ink at
     * that tip.
     *
     * @param stem the stem to check
     * @return the suspect description, or null if this stem raises no suspicion
     */
    private Suspect findSuspect (StemInter stem)
    {
        if (!stem.getBeams().isEmpty()) {
            return null; // Already has a real beam -- not a residual case
        }

        final SIGraph sig = stem.getSig();

        if (!sig.getRelations(stem, FlagStemRelation.class).isEmpty()) {
            return null; // Already has a flag -- not a residual case
        }

        final int dir = stem.computeDirection();

        if (dir == 0) {
            return null; // Not enough signal to know which end is "outer" -- stay conservative
        }

        final List<HeadInter> heads = new ArrayList<>(stem.getHeads());

        if (heads.isEmpty()) {
            return null;
        }

        final VerticalSide tailSide = (dir < 0) ? VerticalSide.TOP : VerticalSide.BOTTOM;
        final Line2D stemLine = stem.getMedian();
        final double tailY = (tailSide == VerticalSide.TOP) ? stemLine.getY1() : stemLine.getY2();

        // Primary head: the one closest to the near (non-tail) end -- i.e. farthest from tailY.
        HeadInter primary = null;
        double bestPrimaryDist = -1;

        for (HeadInter head : heads) {
            final double dist = Math.abs(head.getCenter().y - tailY);

            if (dist > bestPrimaryDist) {
                bestPrimaryDist = dist;
                primary = head;
            }
        }

        final int maxTipDist = scale.toPixels(constants.maxTipDistanceForSuspicion);
        final int minGap = scale.toPixels(constants.minHeadGapForSuspicion);

        for (HeadInter head : heads) {
            if (head == primary) {
                continue;
            }

            final double tipDist = Math.abs(head.getCenter().y - tailY);
            final double gapFromPrimary = Math.abs(head.getCenter().y - primary.getCenter().y);

            if ((tipDist <= maxTipDist) && (gapFromPrimary >= minGap)) {
                return new Suspect(stem, head, tailSide);
            }
        }

        return null;
    }

    //--------------//
    // findNeighbor //
    //--------------//
    /**
     * Look for a stem geometrically compatible with the suspect stem: close facing edges, a
     * matching outer tip level, and no beam or flag of its own already explaining ink there.
     *
     * @param suspect  the suspect stem/head pair
     * @param stems    every stem in the system
     * @param resolved stems already paired by an earlier suspect in this pass
     * @return the compatible neighbor stem, or null if none qualifies
     */
    private StemInter findNeighbor (Suspect suspect,
                                    List<StemInter> stems,
                                    Set<StemInter> resolved)
    {
        final int maxXGap = scale.toPixels(constants.maxStemPairXGap);
        final int maxYTol = scale.toPixels(constants.maxTipYTolerance);
        final Rectangle suspectBox = suspect.stem.getBounds();
        final double suspectTipY = tipY(suspect.stem, suspect.tailSide);

        StemInter best = null;
        int bestGap = Integer.MAX_VALUE;

        for (StemInter other : stems) {
            if ((other == suspect.stem) || resolved.contains(other) || !other.getBeams()
                    .isEmpty() || !system.getSig().getRelations(other, FlagStemRelation.class)
                            .isEmpty()) {
                continue;
            }

            final Rectangle otherBox = other.getBounds();
            final int xGap = (otherBox.x > suspectBox.x)
                    ? (otherBox.x - (suspectBox.x + suspectBox.width))
                    : (suspectBox.x - (otherBox.x + otherBox.width));

            if ((xGap < 0) || (xGap > maxXGap)) {
                continue;
            }

            final double otherTipY = tipY(other, suspect.tailSide);

            if (Math.abs(otherTipY - suspectTipY) > maxYTol) {
                continue;
            }

            if (xGap < bestGap) {
                bestGap = xGap;
                best = other;
            }
        }

        return best;
    }

    //------//
    // tipY //
    //------//
    private double tipY (StemInter stem,
                         VerticalSide side)
    {
        final Line2D median = stem.getMedian();

        return (side == VerticalSide.TOP) ? median.getY1() : median.getY2();
    }

    //-----------------//
    // attemptRecovery //
    //-----------------//
    /**
     * Verify grey ink between the two stems and, if present, build and link the recovered beam,
     * then evict the spurious head that motivated the search. Aborts before touching the sig at
     * all if the suspect head's own grade is too good to plausibly be that spurious head -- a
     * beam left behind alongside a kept head would be a new inconsistency, not a fix.
     *
     * @param suspect  the suspect stem/head pair
     * @param neighbor the paired neighbor stem
     * @return true if a beam was actually recovered
     */
    private boolean attemptRecovery (Suspect suspect,
                                     StemInter neighbor)
    {
        if (suspect.head.getGrade() > constants.headEvictionGradeCeiling.getValue()) {
            // The whole premise of this recovery is that the suspect head is spurious ink that
            // really belongs to the beam. If it grades too well to plausibly be that, creating a
            // beam here anyway would leave both a beam and a (kept) head explaining the same
            // ink -- a new inconsistency, not a fix -- so abort before touching the sig at all.
            return false;
        }

        final StemInter leftStem;
        final StemInter rightStem;

        if (suspect.stem.getBounds().x <= neighbor.getBounds().x) {
            leftStem = suspect.stem;
            rightStem = neighbor;
        } else {
            leftStem = neighbor;
            rightStem = suspect.stem;
        }

        final Rectangle leftBox = leftStem.getBounds();
        final Rectangle rightBox = rightStem.getBounds();
        final int xLeft = leftBox.x + leftBox.width;
        final int xRight = rightBox.x;

        if (xRight <= xLeft) {
            return false; // Stems overlap horizontally -- not a plausible beam span
        }

        final double yLeft = tipY(leftStem, suspect.tailSide);
        final double yRight = tipY(rightStem, suspect.tailSide);

        // The beam's median must reach each stem's own centerline (not just stop at the facing
        // edge used for the ink-scan bounds above) -- BeamStemRelation.checkLink expects the
        // stem to sit at/within the beam's own x-range, with only a small out-gap tolerance.
        final double xLeftCenter = leftStem.getCenter().x;
        final double xRightCenter = rightStem.getCenter().x;

        final Integer typicalHeight = scale.getBeamThickness();
        final double height = (typicalHeight != null) ? typicalHeight
                : scale.toPixelsDouble(constants.beamHeightFraction);

        final ByteProcessor grey = system.getSheet().getPicture().getSource(
                Picture.SourceKey.GRAY);

        if ((grey == null) || !hasRealInk(grey, xLeft, xRight, yLeft, yRight, height)) {
            return false; // Never fabricate a beam from an empty/near-empty gap
        }

        final Point2D left = new Point2D.Double(xLeftCenter, yLeft);
        final Point2D right = new Point2D.Double(xRightCenter, yRight);
        final Line2D median = new Line2D.Double(left, right);
        final BeamInter beam = new BeamInter(
                constants.recoveredBeamGrade.getValue(),
                median,
                height);

        if (beam.getGrade() < BeamInter.getMinGrade()) {
            return false;
        }

        final int profile = system.getProfile();
        final Link leftLink = BeamStemRelation.checkLink(
                beam,
                leftStem,
                suspect.tailSide,
                scale,
                profile);
        final Link rightLink = BeamStemRelation.checkLink(
                beam,
                rightStem,
                suspect.tailSide,
                scale,
                profile);

        if ((leftLink == null) || (rightLink == null)) {
            return false; // Real gap/consistency validation disagrees with the geometric pairing
        }

        final SIGraph sig = system.getSig();
        sig.addVertex(beam);
        leftLink.applyTo(beam);
        rightLink.applyTo(beam);

        final boolean evicted = evictHead(beam, suspect.head);

        logger.info(
                "Recovered residual beam {} between stem#{} and stem#{}{}",
                beam,
                leftStem.getId(),
                rightStem.getId(),
                evicted ? (", evicted spurious head " + suspect.head)
                        : (" -- spurious head " + suspect.head + " was not actually covered,"
                                + " left alone"));

        return true;
    }

    //-----------//
    // hasRealInk //
    //-----------//
    /**
     * Scan the grey source, column by column, in the rectangle bounded by the two stems' facing
     * edges and a vertical band around the suspected beam level, to confirm real ink is present
     * before ever creating a beam from it.
     *
     * @param grey   the grey source
     * @param xLeft  left bound (right edge of the left stem)
     * @param xRight right bound (left edge of the right stem)
     * @param yLeft  beam level at xLeft
     * @param yRight beam level at xRight
     * @param height beam height, used to size the scan band
     * @return true if enough dark ink was found, without too large a gap
     */
    private boolean hasRealInk (ByteProcessor grey,
                                int xLeft,
                                int xRight,
                                double yLeft,
                                double yRight,
                                double height)
    {
        final int margin = (int) Math.rint(height * constants.bandMargin.getValue());
        final int threshold = constants.greyDarknessThreshold.getValue();
        final int width = xRight - xLeft;

        if (width <= 0) {
            return false;
        }

        int darkColumns = 0;
        int currentGap = 0;
        int largestGap = 0;

        for (int x = xLeft; x < xRight; x++) {
            final double ratio = (double) (x - xLeft) / width;
            final int yCenter = (int) Math.rint(yLeft + (ratio * (yRight - yLeft)));
            final int yTop = yCenter - margin;
            final int yBottom = yCenter + margin;

            boolean dark = false;

            for (int y = yTop; y <= yBottom; y++) {
                if (grey.get(x, y) < threshold) {
                    dark = true;

                    break;
                }
            }

            if (dark) {
                darkColumns++;
                largestGap = Math.max(largestGap, currentGap);
                currentGap = 0;
            } else {
                currentGap++;
            }
        }

        largestGap = Math.max(largestGap, currentGap);

        final double darkRatio = (double) darkColumns / width;

        return (darkRatio >= constants.minDarkColumnRatio.getValue())
                && (largestGap <= scale.toPixels(constants.maxCoreGap));
    }

    //-----------//
    // evictHead //
    //-----------//
    /**
     * Remove the spurious head now explained by the recovered beam. The caller has already
     * confirmed the head's grade is low enough (see the check at the top of
     * {@link #attemptRecovery}) for this to plausibly be a false-positive beam recovery -- this
     * only guards against the beam's own area not actually reaching the head, a purely geometric
     * sanity check.
     *
     * @param beam the recovered beam
     * @param head the spurious head
     * @return true if the head was actually removed
     */
    private boolean evictHead (AbstractBeamInter beam,
                               HeadInter head)
    {
        final Area beamArea = beam.getArea();

        if ((beamArea == null) || (head.getBounds() == null)
                || !beamArea.intersects(head.getBounds())) {
            return false; // Recovered beam does not actually cover this head -- leave it alone
        }

        head.remove();

        return true;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------//
    // Suspect //
    //---------//
    /**
     * A stem carrying a head close to its outer/tail tip yet far from its primary head.
     */
    private static class Suspect
    {
        final StemInter stem;

        final HeadInter head;

        /** Which end of the stem (its tail, away from the primary head) the suspect head is near. */
        final VerticalSide tailSide;

        Suspect (StemInter stem,
                HeadInter head,
                VerticalSide tailSide)
        {
            this.stem = stem;
            this.head = head;
            this.tailSide = tailSide;
        }

        @Override
        public String toString ()
        {
            return "Suspect{head#" + head.getId() + " tailSide=" + tailSide + "}";
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction maxStemPairXGap = new Scale.Fraction(
                2.5,
                "Maximum gap between two stems' facing edges to consider pairing them for a"
                        + " residual beam -- this is the distance between two adjacent NOTE"
                        + " STEMS in a beamed group (roughly a note-spacing), not to be confused"
                        + " with BeamsBuilder's own much smaller maxItemXGap (0.5), which bounds"
                        + " gaps within a single beam item's own ink. Confirmed on a real score:"
                        + " the real facing-edge gap was about 1.9 interlines");

        private final Scale.Fraction maxTipYTolerance = new Scale.Fraction(
                1.0,
                "Maximum y-difference between two stems' outer tips to treat them as spanned by"
                        + " one beam -- generous enough to allow for a sloped/diagonal beam over"
                        + " a typical stem-pair gap. Confirmed on a real score: the real"
                        + " (sloped) difference was about 0.7 interline");

        private final Scale.Fraction maxTipDistanceForSuspicion = new Scale.Fraction(
                0.8,
                "Maximum distance from a stem's outer/tail tip for one of its heads to count as"
                        + " sitting \"at the tip\". Confirmed on a real score: the spurious head's"
                        + " center sat about 0.6 interline from the tip");

        private final Scale.Fraction minHeadGapForSuspicion = new Scale.Fraction(
                1.5,
                "Minimum distance between a stem's primary head and a candidate suspect head for"
                        + " the pair to be implausible as an ordinary stacked-chord spacing."
                        + " Confirmed on a real score: the real gap was about 3.5 interlines, far"
                        + " beyond normal chord-note spacing");

        private final Scale.Fraction beamHeightFraction = new Scale.Fraction(
                0.5,
                "Fallback beam height, used only if Scale#getBeamThickness is unavailable");

        private final Constant.Ratio bandMargin = new Constant.Ratio(
                0.6,
                "Half-height of the vertical scan band, as a ratio of beam height");

        private final Constant.Ratio minDarkColumnRatio = new Constant.Ratio(
                0.6,
                "Minimum fraction of columns, between the two stems, that must show real ink in"
                        + " the grey source before a beam is ever created from it");

        private final Scale.Fraction maxCoreGap = new Scale.Fraction(
                0.3,
                "Maximum run of consecutive ink-free columns tolerated within the scanned band");

        private final Constant.Integer greyDarknessThreshold = new Constant.Integer(
                "grey level",
                140,
                "Grey level below which a pixel counts as ink for the residual-beam scan --"
                        + " matches SpotsBuilder's own beamBinarizationThreshold, the standard"
                        + " ink/paper boundary already used for beam-related pixel decisions");

        private final Constant.Ratio recoveredBeamGrade = new Constant.Ratio(
                0.4,
                "Grade assigned to a beam recovered purely from grey-scale evidence (no template"
                        + " evaluation available) -- comfortably above the acceptance floor while"
                        + " staying conservative, since it is not backed by classifier confidence");

        private final Constant.Ratio headEvictionGradeCeiling = new Constant.Ratio(
                0.55,
                "A suspect head is evicted once a beam is recovered over it only if its own grade"
                        + " is at or below this ceiling. Confirmed on a real score: the spurious"
                        + " head that motivated this whole pass graded 0.487, clearly below every"
                        + " genuine nearby head (0.65-0.80+) -- this ceiling sits safely between"
                        + " the two, as a sanity net against evicting a real head on a coincidence");
    }
}
