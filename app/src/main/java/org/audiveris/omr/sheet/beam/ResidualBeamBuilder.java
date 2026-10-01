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
import org.audiveris.omr.sig.inter.BeamGroupInter;
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class <code>ResidualBeamBuilder</code> recovers a beam that {@link SpotsBuilder} never even
 * produced a candidate glyph for. Two distinct symptoms are handled, sharing all of the actual
 * recovery machinery ({@link #scanBands}, {@link BeamStemRelation#checkLink}, group creation):
 * the dropped ink sometimes gets misread downstream as a spurious note head (evicted on success),
 * and sometimes leaves no trace anywhere at all (nothing to evict -- see {@link #findBareCandidate}).
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
 * attachment; the narrower gap between their facing edges is what actually gets scanned for ink).
 * <p>
 * Critically, the ink between the two stems is not always a single beam: a genuine 16th-note beam
 * pair merges into what looks, at first glance, like one over-thick stroke, but a raw grey-scale
 * profile through it reveals two distinct dark bands with a real gap between them -- confirmed on
 * a real score. {@link #scanBands} measures this directly (never fabricating a beam from an empty
 * gap) and reconstructs however many bands are actually present -- one beam, or two stacked beams
 * sharing a fresh {@link BeamGroupInter} -- rather than always averaging the ink into a single
 * beam, which would silently halve the note value (16th read as 8th). The recovery is
 * all-or-nothing across every band found: if the suspect head's own grade turns out too good to
 * plausibly be that spurious head, or any band fails its own grade/link validation, nothing is
 * created at all -- a beam left behind alongside a kept head, or only one of two required beams,
 * would be a new inconsistency, not a fix.
 * <p>
 * The second symptom -- a completely clean pair of single-headed stems, with no spurious head
 * anywhere to raise suspicion -- cannot be found by looking at heads at all. Confirmed on a real
 * score: such a pair's stems show no reliable geometric tell either (in particular, stem free
 * length is indistinguishable from an ordinary unbeamed note's). {@link #findBareCandidate} pairs
 * such stems by geometry alone (widened tolerances, since there is no corroborating signal to
 * offset the wider net), and relies entirely on {@link #scanBands}' grey-ink evidence -- the same
 * gate used for the spurious-head case -- to decide whether a beam actually belongs there. A wider
 * trigger is safe specifically because the acceptance gate is unchanged: the grey evidence
 * requirement never gets more permissive just because more candidates reach it.
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

            final StemInter neighbor = findNeighbor(suspect, stems, resolved, false);

            if (neighbor == null) {
                continue;
            }

            if (attemptRecovery(suspect, neighbor)) {
                resolved.add(suspect.stem);
                resolved.add(neighbor);
            }
        }

        // Second pass: completely clean, single-headed "bare" stems left unresolved above. There
        // is no spurious head here to raise suspicion -- the missing-beam ink was simply dropped
        // by SpotsBuilder with nothing downstream ever claiming it -- so pairing is geometry-only
        // and scanBands' grey-ink evidence is the sole acceptance gate (see attemptRecovery).
        final List<Suspect> bareCandidates = new ArrayList<>();

        for (StemInter stem : stems) {
            if (resolved.contains(stem)) {
                continue;
            }

            final Suspect bare = findBareCandidate(stem);

            if (bare != null) {
                bareCandidates.add(bare);
            }
        }

        for (Suspect bare : bareCandidates) {
            if (resolved.contains(bare.stem)) {
                continue;
            }

            final StemInter neighbor = findNeighbor(bare, stems, resolved, true);

            if (neighbor == null) {
                continue;
            }

            if (attemptRecovery(bare, neighbor)) {
                resolved.add(bare.stem);
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

    //-------------------//
    // findBareCandidate //
    //-------------------//
    /**
     * Check whether the given stem is a completely clean, single-headed, beam-less, flag-less
     * stem -- the "no trace at all" case, where the missing-beam ink was dropped by
     * {@link SpotsBuilder} with nothing downstream ever claiming it. Unlike {@link #findSuspect},
     * there is no spurious head to corroborate the guess: geometry only picks a candidate pair,
     * and {@link #scanBands}' grey-ink evidence (via {@link #attemptRecovery}) is what actually
     * decides whether a beam gets created.
     *
     * @param stem the stem to check
     * @return a headless suspect description, or null if this stem is not a bare candidate
     */
    private Suspect findBareCandidate (StemInter stem)
    {
        if (!stem.getBeams().isEmpty()) {
            return null;
        }

        final SIGraph sig = stem.getSig();

        if (!sig.getRelations(stem, FlagStemRelation.class).isEmpty()) {
            return null;
        }

        final int dir = stem.computeDirection();

        if (dir == 0) {
            return null;
        }

        if (stem.getHeads().size() != 1) {
            return null; // Excludes chords and the already-handled spurious-second-head case
        }

        final VerticalSide tailSide = (dir < 0) ? VerticalSide.TOP : VerticalSide.BOTTOM;

        return new Suspect(stem, null, tailSide);
    }

    //--------------//
    // findNeighbor //
    //--------------//
    /**
     * Look for a stem geometrically compatible with the suspect stem: close facing edges, a
     * matching outer tip level, and no beam or flag of its own already explaining ink there.
     *
     * @param suspect          the suspect stem/head pair
     * @param stems            every stem in the system
     * @param resolved         stems already paired by an earlier suspect in this pass
     * @param requireSingleHeadOther for a bare-candidate pair, require the other stem to also be
     *                         single-headed (both sides need to independently qualify -- there is
     *                         no spurious head on either side to corroborate the pairing)
     * @return the compatible neighbor stem, or null if none qualifies
     */
    private StemInter findNeighbor (Suspect suspect,
                                    List<StemInter> stems,
                                    Set<StemInter> resolved,
                                    boolean requireSingleHeadOther)
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

            if (requireSingleHeadOther && (other.getHeads().size() != 1)) {
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

            if (sharesHeadColumn(suspect, other)) {
                continue;
            }

            if (xGap < bestGap) {
                bestGap = xGap;
                best = other;
            }
        }

        return best;
    }

    //------------------//
    // sharesHeadColumn //
    //------------------//
    /**
     * Check whether a head of the other stem sits in the same column as a real head of the
     * suspect stem (its spurious head, if any, excluded).
     * <p>
     * Two beamed notes follow each other in time, so their heads never share a column -- heads
     * stacked in one column make a chord. Confirmed on a real score: a flag misread as a spurious
     * void head plus a short spurious stem, right below the note's own head, got paired with that
     * note's real stem only 0.75 interline away (the flag's ink then passing the grey-ink check),
     * while the tightest genuine pair observed has its two heads more than a notehead apart.
     *
     * @param suspect the suspect stem/head pair
     * @param other   the candidate neighbor stem
     * @return true if pairing them would beam two heads of the same column
     */
    private boolean sharesHeadColumn (Suspect suspect,
                                      StemInter other)
    {
        final double minDx = scale.toPixelsDouble(constants.minHeadXDistance);

        for (HeadInter head : suspect.stem.getHeads()) {
            if (head == suspect.head) {
                continue;
            }

            final double x = head.getCenter2D().getX();

            for (HeadInter otherHead : other.getHeads()) {
                if (Math.abs(otherHead.getCenter2D().getX() - x) < minDx) {
                    return true;
                }
            }
        }

        return false;
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
     * then (for a spurious-head suspect) evict the head that motivated the search. Aborts before
     * touching the sig at all if the suspect head's own grade is too good to plausibly be that
     * spurious head -- a beam left behind alongside a kept head would be a new inconsistency, not
     * a fix. For a bare pair (no head at all), this eviction gate simply does not apply.
     *
     * @param suspect  the suspect stem/head pair, or a headless bare-pair candidate
     * @param neighbor the paired neighbor stem
     * @return true if a beam was actually recovered
     */
    private boolean attemptRecovery (Suspect suspect,
                                     StemInter neighbor)
    {
        if ((suspect.head != null)
                && (suspect.head.getGrade() > constants.headEvictionGradeCeiling.getValue())) {
            // The whole premise of this recovery is that the suspect head is spurious ink that
            // really belongs to the beam. If it grades too well to plausibly be that, creating a
            // beam here anyway would leave both a beam and a (kept) head explaining the same
            // ink -- a new inconsistency, not a fix -- so abort before touching the sig at all.
            // (A bare pair has no head at all, so this gate simply doesn't apply to it.)
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

        // The beam's median must reach past each stem's own centerline -- not just stop exactly
        // at it -- because BeamStemRelation.checkLink's out-gap tolerance is tight (a fraction of
        // an interline) and a median ending exactly on the centerline leaves only half the stem's
        // own width as margin, a knife's-edge amount a real, organically-detected beam (whose
        // median reaches the glyph's actual ink edge, past the stem) never has to contend with.
        final double margin = scale.toPixelsDouble(constants.medianEndExtension);
        final double xLeftCenter0 = leftStem.getCenter().x;
        final double xRightCenter0 = rightStem.getCenter().x;
        final double slope = (yRight - yLeft) / (xRightCenter0 - xLeftCenter0);
        final double xLeftCenter = xLeftCenter0 - margin;
        final double xRightCenter = xRightCenter0 + margin;
        final double yLeftExt = yLeft - (slope * margin);
        final double yRightExt = yRight + (slope * margin);

        final Integer typicalHeight = scale.getBeamThickness();
        final double height = (typicalHeight != null) ? typicalHeight
                : scale.toPixelsDouble(constants.beamHeightFraction);

        final ByteProcessor grey = system.getSheet().getPicture().getSource(
                Picture.SourceKey.GRAY);

        if (grey == null) {
            return false;
        }

        final List<Band> bands = scanBands(grey, xLeft, xRight, yLeft, yRight, height);

        if (bands.isEmpty()) {
            return false; // Never fabricate a beam from an empty/near-empty gap
        }

        final int profile = system.getProfile();
        final List<Candidate> candidates = new ArrayList<>();

        for (Band band : bands) {
            final Point2D left = new Point2D.Double(xLeftCenter, yLeftExt + band.offset);
            final Point2D right = new Point2D.Double(xRightCenter, yRightExt + band.offset);
            final Line2D median = new Line2D.Double(left, right);
            final BeamInter beam = new BeamInter(
                    constants.recoveredBeamGrade.getValue(),
                    median,
                    band.height);

            if (beam.getGrade() < BeamInter.getMinGrade()) {
                return false; // All-or-nothing: one bad band aborts the whole recovery
            }

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
                return false; // Real gap/consistency validation disagrees with this band's geometry
            }

            candidates.add(new Candidate(beam, leftLink, rightLink));
        }

        final SIGraph sig = system.getSig();
        final BeamGroupInter group = new BeamGroupInter();
        sig.addVertex(group);

        boolean evicted = false;

        for (Candidate candidate : candidates) {
            sig.addVertex(candidate.beam);
            candidate.leftLink.applyTo(candidate.beam);
            candidate.rightLink.applyTo(candidate.beam);
            group.addMember(candidate.beam);

            if (suspect.head != null) {
                evicted |= evictHead(candidate.beam, suspect.head);
            }
        }

        logger.info(
                "Recovered {} residual beam(s) between stem#{} and stem#{}{}",
                candidates.size(),
                leftStem.getId(),
                rightStem.getId(),
                (suspect.head == null) ? " (bare pair, no head to evict)"
                        : (evicted ? (", evicted spurious head " + suspect.head)
                                : (" -- spurious head " + suspect.head + " was not actually"
                                        + " covered, left alone")));

        return true;
    }

    //-----------//
    // scanBands //
    //-----------//
    /**
     * Scan the grey source, column by column, in the rectangle bounded by the two stems' facing
     * edges, to measure however many distinct dark bands (candidate beams) actually sit between
     * them -- a genuine 16th-note beam pair merges into ink that looks, at a glance, like one
     * over-thick stroke, but the raw grey profile still shows two separate bands with a real gap
     * between them. Never guesses: an ambiguous profile (no band, or an implausible band height)
     * yields no bands at all, rather than a fabricated beam.
     *
     * @param grey          the grey source
     * @param xLeft         left bound (right edge of the left stem)
     * @param xRight        right bound (left edge of the right stem)
     * @param yLeft         expected single-beam level at xLeft
     * @param yRight        expected single-beam level at xRight
     * @param typicalHeight the sheet's typical single-beam height, used to size the scan window
     *                      and to judge whether a found band is a plausible beam
     * @return the bands found (empty if inconclusive), ordered by offset
     */
    private List<Band> scanBands (ByteProcessor grey,
                                  int xLeft,
                                  int xRight,
                                  double yLeft,
                                  double yRight,
                                  double typicalHeight)
    {
        final List<Band> none = Collections.emptyList();
        final int width = xRight - xLeft;

        if (width <= 0) {
            return none;
        }

        final int threshold = constants.greyDarknessThreshold.getValue();
        final int halfWindow = (int) Math.rint(typicalHeight * constants.scanHalfWindowRatio
                .getValue());

        // Per-offset dark-column counts, offset in [-halfWindow, +halfWindow].
        final int[] darkCounts = new int[(2 * halfWindow) + 1];

        for (int x = xLeft; x < xRight; x++) {
            final double ratio = (double) (x - xLeft) / width;
            final double yCenter = yLeft + (ratio * (yRight - yLeft));

            for (int o = -halfWindow; o <= halfWindow; o++) {
                final int y = (int) Math.rint(yCenter + o);

                if (grey.get(x, y) < threshold) {
                    darkCounts[o + halfWindow]++;
                }
            }
        }

        final double minRatio = constants.minDarkColumnRatio.getValue();
        final boolean[] dark = new boolean[darkCounts.length];

        for (int i = 0; i < darkCounts.length; i++) {
            dark[i] = ((double) darkCounts[i] / width) >= minRatio;
        }

        // No separate gap-bridging step: the per-offset dark ratio (aggregated across every
        // sampled column) is already robust to column-level noise on its own -- confirmed on a
        // real score, where the true gap between two stacked beams was only a single offset-unit
        // wide, so any bridging tolerance wide enough to be "safe" against noise would have erased
        // that real gap too. A single light offset between two dark runs is trusted as real.

        // Extract runs of dark offsets as candidate bands.
        final List<Band> bands = new ArrayList<>();
        final double minHeight = typicalHeight * constants.minBandHeightRatio.getValue();
        final double maxHeight = typicalHeight * constants.maxBandHeightRatio.getValue();

        for (int i = 0; i < dark.length;) {
            if (!dark[i]) {
                i++;

                continue;
            }

            int j = i;

            while ((j < dark.length) && dark[j]) {
                j++;
            }

            final double bandHeight = j - i;

            if (bandHeight > maxHeight) {
                // Too tall to be one beam -- genuinely ambiguous (could be a third stuck beam, or
                // something else entirely), not a case to guess at.
                return none;
            } else if (bandHeight >= minHeight) {
                final double offset = (((i + j) / 2.0) - halfWindow);
                bands.add(new Band(offset, bandHeight));
            }
            // else: too short to be a real beam -- just noise (e.g. a stray dark pixel from
            // nearby unrelated ink caught by the wide scan window), skip it rather than aborting
            // the whole scan over it.

            i = j;
        }

        if (bands.size() > 2) {
            return none; // More structure than a single/double beam explains -- stay conservative
        }

        return bands;
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
     * A stem flagged as one side of a candidate residual-beam pair: either a stem carrying a
     * spurious head close to its outer/tail tip yet far from its primary head ({@code head} set),
     * or a completely clean, single-headed bare stem paired by geometry and confirmed only by
     * grey ink ({@code head} null -- nothing to evict).
     */
    private static class Suspect
    {
        final StemInter stem;

        /** The spurious head to evict on success, or null for a bare (headless-trigger) pair. */
        final HeadInter head;

        /** Which end of the stem (its tail, away from the primary head) the beam attaches at. */
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
            return "Suspect{stem#" + stem.getId()
                    + ((head != null) ? (" head#" + head.getId()) : " bare")
                    + " tailSide=" + tailSide + "}";
        }
    }

    //------//
    // Band //
    //------//
    /**
     * One dark band found by {@link #scanBands}, describing one candidate beam: its vertical
     * offset from the stem-tip centerline (positive = toward the tail side) and its own measured
     * height.
     */
    private static class Band
    {
        final double offset;

        final double height;

        Band (double offset,
             double height)
        {
            this.offset = offset;
            this.height = height;
        }
    }

    //-----------//
    // Candidate //
    //-----------//
    /**
     * A validated beam ready to be added to the sig, paired with the two stem links that already
     * passed {@link BeamStemRelation#checkLink}, so they don't need recomputing after the
     * all-or-nothing validation loop in {@link #attemptRecovery}.
     */
    private static class Candidate
    {
        final BeamInter beam;

        final Link leftLink;

        final Link rightLink;

        Candidate (BeamInter beam,
                  Link leftLink,
                  Link rightLink)
        {
            this.beam = beam;
            this.leftLink = leftLink;
            this.rightLink = rightLink;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction minHeadXDistance = new Scale.Fraction(
                0.7,
                "Minimum horizontal distance between a real head of each of the two paired stems"
                        + " (heads in one column make a chord, never two beamed notes)."
                        + " Confirmed on real scores: flag-induced false pairs had their heads 0.09"
                        + " and 0.17 interline apart, the tightest genuine pair 1.39.");

        private final Scale.Fraction maxStemPairXGap = new Scale.Fraction(
                4.6,
                "Maximum gap between two stems' facing edges to consider pairing them for a"
                        + " residual beam -- this is the distance between two adjacent NOTE"
                        + " STEMS in a beamed group (roughly a note-spacing), not to be confused"
                        + " with BeamsBuilder's own much smaller maxItemXGap (0.5), which bounds"
                        + " gaps within a single beam item's own ink. Confirmed on real scores:"
                        + " facing-edge gaps of about 1.9 and 2.9 interlines (the latter matching"
                        + " this same score's own organically-detected same-beat stem spacing,"
                        + " an existing correctly-linked beam spanning stems at the identical"
                        + " ~2.9 interline gap), and a bare-pair case at 4.27 interlines -- this"
                        + " coarse pre-filter is not itself the acceptance gate (scanBands' grey"
                        + " evidence and BeamStemRelation#checkLink are), so widening it only"
                        + " admits more candidates to be actually checked");

        private final Scale.Fraction maxTipYTolerance = new Scale.Fraction(
                1.2,
                "Maximum y-difference between two stems' outer tips to treat them as spanned by"
                        + " one beam -- generous enough to allow for a sloped/diagonal beam over"
                        + " a typical stem-pair gap. Confirmed on real scores: sloped differences"
                        + " of about 0.7 and 1.09 interlines (the latter a spurious-head suspect"
                        + " pair that was otherwise correctly identified but fell 1 pixel outside"
                        + " the previous 1.0 interline tolerance)");

        private final Scale.Fraction maxTipDistanceForSuspicion = new Scale.Fraction(
                1.05,
                "Maximum distance from a stem's outer/tail tip for one of its heads to count as"
                        + " sitting \"at the tip\". Confirmed on real scores: spurious heads whose"
                        + " centers sat about 0.6 and 1.0 interlines from the tip (the latter a"
                        + " genuine 16th-note double-beam pair, otherwise identical in shape to"
                        + " the already-handled cases, that the previous 0.8 interline ceiling"
                        + " missed)");

        private final Scale.Fraction minHeadGapForSuspicion = new Scale.Fraction(
                1.5,
                "Minimum distance between a stem's primary head and a candidate suspect head for"
                        + " the pair to be implausible as an ordinary stacked-chord spacing."
                        + " Confirmed on a real score: the real gap was about 3.5 interlines, far"
                        + " beyond normal chord-note spacing");

        private final Scale.Fraction beamHeightFraction = new Scale.Fraction(
                0.5,
                "Fallback beam height, used only if Scale#getBeamThickness is unavailable");

        private final Scale.Fraction medianEndExtension = new Scale.Fraction(
                0.25,
                "How far past each stem's own centerline the recovered beam's median is extended"
                        + " -- comfortably beyond BeamStemRelation's own tight out-gap tolerance"
                        + " (a fraction of an interline), matching how an organically-detected"
                        + " beam's median reaches the glyph's actual ink edge rather than stopping"
                        + " exactly on the stem");

        private final Constant.Ratio minDarkColumnRatio = new Constant.Ratio(
                0.6,
                "Minimum fraction of sampled columns that must be dark at a given vertical offset"
                        + " for that offset to count as part of a band");

        private final Constant.Ratio scanHalfWindowRatio = new Constant.Ratio(
                2.2,
                "Half-height of the vertical scan window, as a ratio of typical single-beam"
                        + " height -- wide enough to comfortably contain two stacked beams plus a"
                        + " gutter plus margin. Confirmed on a real score: two bands of ~6-8px each"
                        + " (typical height 7px) separated by a ~3px gap fit well inside this");

        private final Constant.Ratio minBandHeightRatio = new Constant.Ratio(
                0.5,
                "Minimum plausible band height, as a ratio of typical single-beam height, for a"
                        + " dark run to be trusted as a real beam rather than noise");

        private final Constant.Ratio maxBandHeightRatio = new Constant.Ratio(
                1.8,
                "Maximum plausible band height, as a ratio of typical single-beam height, for a"
                        + " dark run to be trusted as one beam rather than an unreliable read");

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
                0.70,
                "A suspect head is evicted once a beam is recovered over it only if its own grade"
                        + " is at or below this ceiling. Confirmed on two independent real cases"
                        + " (both verified against the actual score, not just the classifier):"
                        + " spurious heads graded 0.487 and 0.661 -- the classifier can be fairly"
                        + " confident in a wrong guess, so this ceiling is set with real margin"
                        + " above the higher of the two, while every genuine head observed nearby"
                        + " in either case still scored 0.72+, leaving a real gap on both sides");
    }
}
