//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              R e s i d u a l C l e f B u i l d e r                             //
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
package org.audiveris.omr.sheet.clef;

import org.audiveris.omr.classifier.Classifier;
import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.classifier.ShapeClassifier;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.GlyphLink;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureFiller;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.util.HorizontalSide;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class <code>ResidualClefBuilder</code> recovers clef changes located anywhere past a staff's
 * header -- as opposed to {@link ClefBuilder}, which only ever looks at the very beginning of a
 * staff.
 * <p>
 * A clef appearing at the beginning of a staff is protected end-to-end: {@link ClefBuilder} looks
 * for it directly, and both {@link org.audiveris.omr.sheet.stem.VerticalsBuilder} (stem seeds) and
 * {@link org.audiveris.omr.sheet.note.NoteHeadsBuilder} (note heads) explicitly skip anything
 * before {@link Staff#getHeaderStop()} so that ink can never be misread as stem or head material.
 * A clef change later in the same staff -- routine in Russian bayan/accordion scores, where the
 * right or left hand regularly switches register mid-piece -- gets none of that protection: it
 * relies entirely on the same general-purpose combinatorial glyph-clustering search used for
 * every other floating symbol ({@code GlyphCluster.decompose()}, driven from
 * {@code SymbolsBuilder}).
 * <p>
 * Confirmed on a real score: that search does not reliably reassemble a mid-staff clef change as
 * one clean candidate. Its ink gets split into several small pieces at symbol-detection time, some
 * of which score confidently as unrelated shapes (a curl as {@code FLAG_1_DOWN} at 0.9+, a dot as
 * {@code DOT_set}), while the one candidate that does merge back into the correct, complete clef
 * shape scores {@code G_CLEF_SMALL}/{@code F_CLEF_SMALL} only marginally -- typically right on top
 * of {@link Grades#symbolMinGrade}, sometimes a hair under it, sometimes a hair over -- and in
 * every case observed, that correct candidate was never even offered to
 * {@link org.audiveris.omr.sheet.symbol.InterFactory} by the general search in the first place.
 * <p>
 * Rather than reworking that shared, general-purpose search (used by every fixed-shape symbol, not
 * just clefs), this targeted pass runs late, once the main pipeline has had its say: it looks, per
 * staff, at whatever ink is still unclaimed (no Inter uses it), groups that ink by simple
 * proximity, and asks the classifier to judge each resulting group as a whole. This directly
 * implements the idea that a bigger, more complete candidate should be preferred over smaller
 * fragments of the same ink: a real symbol's pieces don't get names of their own once every
 * *other* explanation for them has already been tried and rejected by the main pipeline, so by the
 * time this pass runs, any ink still sitting unclaimed is exactly the ink no smaller interpretation
 * ever fit well enough to claim -- the one thing left to try is the complete union.
 * <p>
 * "Unclaimed" is not always literal, though. Confirmed on a real score: a mid-staff clef's own ink
 * can itself be mis-claimed by the main pipeline -- a spurious {@code STEM} plus a spurious
 * {@code NOTEHEAD_VOID}, each graded well below any genuine note nearby (roughly 0.3-0.6, versus
 * 0.77+ for real notes in the same area) -- which then blocks this pass from ever seeing that ink
 * at all, since it no longer reads as unclaimed. {@link #findWeakClaimant} treats ink claimed only
 * by such a low-grade, plausibly-clef-shaped Inter as reclaimable: it is gathered into the same
 * candidate pool as genuinely unclaimed ink, and if the resulting compound classifies confidently
 * as a clef, the weak claimant is evicted in favor of the recovered clef -- mirroring how
 * {@link org.audiveris.omr.sheet.beam.ResidualBeamBuilder} evicts a spurious head once a beam is
 * recovered over it. A strong claim (a normal, confidently-graded Inter) is never touched.
 * <p>
 * A reclaimed compound is not always a clean, full clef shape either: merging a handful of
 * simplified fragments (e.g. two round blobs plus a connecting stroke -- literally the same shapes
 * a real half note is made of) could plausibly score some other, unrelated shape as the
 * classifier's single best guess, with the correct clef shape still a strong runner-up. Acceptance
 * therefore searches a few ranked guesses deep for a clef-family shape, rather than requiring it
 * to win outright -- see {@code candidateSearchDepth}.
 * <p>
 * Runs from {@link org.audiveris.omr.sheet.symbol.LinksStep#doEpilog}, sheet-wide, for the same
 * reason the accordion-registration residual recoveries there do: by this point every system's own
 * {@code SigReducer} reduction has already run, so competing near-duplicate candidates have already
 * been resolved and won't still be cluttering the glyph index.
 */
public class ResidualClefBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ResidualClefBuilder.class);

    /** The only shapes this recovery pass will ever create -- clef family only. */
    private static final EnumSet<Shape> CANDIDATE_SHAPES = EnumSet.of(
            Shape.G_CLEF,
            Shape.G_CLEF_SMALL,
            Shape.F_CLEF,
            Shape.F_CLEF_SMALL);

    /**
     * Shapes a weak, low-grade Inter may hold while still being reclaimable as candidate clef
     * ink -- see {@link #findWeakClaimant}. Confirmed on a real score: a mid-staff clef's own ink
     * fragmented into exactly these shapes (a spurious {@code STEM} plus a spurious
     * {@code NOTEHEAD_VOID}), each graded well below any genuine note observed nearby.
     */
    private static final EnumSet<Shape> RECLAIMABLE_SHAPES = EnumSet.of(
            Shape.STEM,
            Shape.NOTEHEAD_VOID);

    //~ Instance fields ------------------------------------------------------------------------------

    private final Sheet sheet;

    private final Scale scale;

    private final Classifier classifier = ShapeClassifier.getInstance();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>ResidualClefBuilder</code> object.
     *
     * @param sheet the sheet to process
     */
    public ResidualClefBuilder (Sheet sheet)
    {
        this.sheet = sheet;
        this.scale = sheet.getScale();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // process //
    //---------//
    /**
     * Look for a residual (mid-staff) clef change in every staff of every system, and refresh
     * each impacted system's measures so the recovered clef is taken into account for pitch.
     */
    public void process ()
    {
        final Set<SystemInfo> impactedSystems = new LinkedHashSet<>();

        // Glyphs already offered to some system's candidate pool, whether that attempt recovered
        // a clef or not -- shared across every processSystem() call in this pass. Without this,
        // a glyph sitting in the narrow overlap between two adjacent systems' own staffVerticalMargin
        // zones (confirmed on a real score: the gap between one system's bottom staff and the next
        // system's top staff) gets offered a second time once the first system's attempt leaves it
        // unclaimed -- and the classifier throws when asked to evaluate a glyph under a system it
        // does not actually belong to, since checks run at evaluation time assume system-consistent
        // geometry. Retrying such a glyph could never succeed differently anyway: the ink itself did
        // not change between attempts, only which system asked.
        final Set<Glyph> examinedGlyphs = new LinkedHashSet<>();

        for (SystemInfo system : sheet.getSystems()) {
            if (processSystem(system, examinedGlyphs)) {
                impactedSystems.add(system);
            }
        }

        // A newly recovered clef must be taken into account by each measure's own cached clef
        // list -- see Measure#getClefBefore, populated by MeasureFiller from a snapshot of the
        // SIG -- otherwise pitch of subsequent notes would silently keep ignoring it.
        for (SystemInfo system : impactedSystems) {
            new MeasureFiller(system).process();
        }
    }

    //---------------//
    // processSystem //
    //---------------//
    /**
     * Look for residual clef changes anywhere in the given system, past each staff's header.
     * <p>
     * Every staff of the system is considered together, in one pass, rather than one straight
     * scan per staff: adjacent staves' own vertical search margins routinely overlap (the same
     * way adjacent systems' bounds do -- see the cross-system duplication this exact trap caused
     * for accordion-registration grids), so scanning per staff let the very same clef ink be
     * independently picked up and recovered under two or three neighboring staves at once. Here,
     * a candidate cluster is assigned to the single staff whose nearest line is truly closest
     * (via {@link Staff#doubleDistanceTo}), never to every staff whose margin happens to reach it.
     *
     * @param system          the system to inspect
     * @param examinedGlyphs  glyphs already offered to some system's candidate pool in this same
     *                        {@link #process()} pass -- never offered again, see {@link #process()}
     * @return true if at least one clef was recovered
     */
    private boolean processSystem (SystemInfo system,
                                    Set<Glyph> examinedGlyphs)
    {
        final List<Staff> staves = system.getStaves();

        if (staves.isEmpty()) {
            return false;
        }

        final int vMargin = scale.toPixels(constants.staffVerticalMargin);
        final List<Rectangle> zones = new ArrayList<>();

        for (Staff staff : staves) {
            final Rectangle zone = staff.getAreaBounds().getBounds();
            zone.grow(0, vMargin);
            zone.x = staff.getHeaderStop(); // Header itself is already handled elsewhere
            zone.width = staff.getAbscissa(HorizontalSide.RIGHT) - zone.x;
            zones.add(zone);
        }

        final int minPartWeight = scale.toPixels(constants.minPartWeight);
        final int maxPartSide = scale.toPixels(constants.maxClefWidth);
        final List<Glyph> candidates = new ArrayList<>();

        // Weak claimant (if any) behind each candidate glyph that isn't genuinely unclaimed --
        // evicted only if the cluster it ends up part of actually recovers a clef.
        final Map<Glyph, Inter> weakClaimants = new LinkedHashMap<>();

        for (Glyph glyph : sheet.getGlyphIndex().getEntities()) {
            final Rectangle gb = glyph.getBounds();

            if (glyph.getWeight() < minPartWeight) {
                continue; // Ignore pixel-level noise
            }
            if ((gb.width > maxPartSide) || (gb.height > maxPartSide)) {
                continue; // Not a plausible clef part (e.g. a staff-line remnant)
            }

            boolean inAnyZone = false;

            for (Rectangle zone : zones) {
                if (zone.intersects(gb)) {
                    inAnyZone = true;

                    break;
                }
            }

            if (!inAnyZone) {
                continue;
            }

            if (!examinedGlyphs.add(glyph)) {
                continue; // Already offered to (and, if unclaimed, already rejected by) another system
            }

            final Inter claimant = findClaimingInter(glyph);

            if (claimant != null) {
                final Inter weak = findWeakClaimant(claimant);

                if (weak == null) {
                    continue; // Strongly claimed, or claimed by a non-reclaimable shape
                }

                weakClaimants.put(glyph, weak);
            }

            candidates.add(glyph);
        }

        if (candidates.isEmpty()) {
            return false;
        }

        final double maxGap = scale.toPixelsDouble(constants.maxPartGap);
        final SimpleGraph<Glyph, GlyphLink> graph = Glyphs.buildLinks(candidates, maxGap);
        final ConnectivityInspector<Glyph, GlyphLink> inspector = new ConnectivityInspector<>(
                graph);

        final int minWeight = scale.toPixels(constants.minClefWeight);
        final int maxWeight = scale.toPixels(constants.maxClefWeight);
        final int minHeight = scale.toPixels(constants.minClefHeight);
        final int maxHeight = scale.toPixels(constants.maxClefHeight);
        final int minWidth = scale.toPixels(constants.minClefWidth);
        final int maxWidth = scale.toPixels(constants.maxClefWidth);

        boolean recovered = false;

        for (Set<Glyph> cluster : inspector.connectedSets()) {
            // NOTA: weight/bounds are measured on the actual built compound, not summed/unioned
            // from its constituent parts -- the glyph index routinely holds several overlapping
            // combinatorial variants of nearly the same ink (left over from the main pipeline's
            // own, unrelated candidate search), and naively summing Glyph#getWeight() across a
            // cluster that happens to include more than one such variant of the same pixels
            // over-counts wildly, since foreground pixels shared by two overlapping candidates
            // get counted once per candidate. GlyphFactory#buildGlyph paints every part into one
            // shared buffer, so the same pixel only ever counts once in the result.
            Glyph compound = (cluster.size() > 1) ? GlyphFactory.buildGlyph(cluster) : cluster
                    .iterator().next();
            final int weight = compound.getWeight();

            if ((weight < minWeight) || (weight > maxWeight)) {
                continue;
            }

            final Rectangle box = compound.getBounds();

            if ((box.width < minWidth) || (box.width > maxWidth) || (box.height < minHeight)
                    || (box.height > maxHeight)) {
                continue;
            }

            // Assign to the single closest staff -- not every staff whose margin reaches here
            final Point2D center = compound.getCenter2D();
            Staff bestStaff = null;
            double bestDistance = Double.MAX_VALUE;

            for (Staff staff : staves) {
                final double distance = staff.doubleDistanceTo(center);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestStaff = staff;
                }
            }

            if (bestStaff == null) {
                continue;
            }

            compound = sheet.getGlyphIndex().registerOriginal(compound);

            final double minGrade = constants.residualClefMinGrade.getValue();
            final int depth = constants.candidateSearchDepth.getValue();
            final Evaluation[] evals = classifier.evaluate(
                    compound,
                    system,
                    depth,
                    minGrade,
                    EnumSet.of(Classifier.Condition.CHECKED));

            // A clef need not be the single top guess -- see class javadoc: a reclaimed compound
            // built from just a couple of simplified fragments (e.g. two round blobs plus a
            // connecting stroke) can resemble a generic note shape at least as much as it
            // resembles the clef those fragments actually came from. Take the first clef-family
            // shape found among the top-ranked, grade-qualifying guesses.
            Evaluation clefEval = null;

            for (Evaluation eval : evals) {
                if (CANDIDATE_SHAPES.contains(eval.shape)) {
                    clefEval = eval;

                    break;
                }
            }

            if (clefEval == null) {
                continue;
            }

            final Shape shape = clefEval.shape;
            final double grade = Grades.intrinsicRatio * clefEval.grade;
            final ClefInter clef = ClefInter.createValid(compound, shape, grade, bestStaff);

            if (clef == null) {
                continue;
            }

            // Only now, with a confirmed recovery, evict any weak claimant this cluster
            // reclaimed ink from -- a rejected cluster must never evict anything.
            final Set<Inter> evictions = new LinkedHashSet<>();

            for (Glyph part : cluster) {
                final Inter weak = weakClaimants.get(part);

                if (weak != null) {
                    evictions.add(weak);
                }
            }

            for (Inter weak : evictions) {
                weak.remove();
            }

            system.getSig().addVertex(clef);
            logger.info(
                    "Recovered residual clef {} in staff#{} ({}){}",
                    clef,
                    bestStaff.getId(),
                    clefEval,
                    evictions.isEmpty() ? "" : (", evicted " + evictions));
            recovered = true;
        }

        return recovered;
    }

    //------------------//
    // findClaimingInter //
    //------------------//
    /**
     * Report the Inter, if any, that already uses the given glyph -- checked sheet-wide (not just
     * the glyph's own nearest system) since a glyph near a system boundary can end up claimed
     * under a neighboring system, as confirmed with the very same trap while building the
     * accordion-registration residual recoveries.
     *
     * @param glyph the glyph to check
     * @return the claiming Inter, or null if the glyph is genuinely unclaimed
     */
    private Inter findClaimingInter (Glyph glyph)
    {
        for (SystemInfo system : sheet.getSystems()) {
            for (Inter inter : system.getSig().vertexSet()) {
                if (inter.getGlyph() == glyph) {
                    return inter;
                }
            }
        }

        return null;
    }

    //-----------------//
    // findWeakClaimant //
    //-----------------//
    /**
     * Check whether the given claiming Inter is weak and plausibly-clef-shaped enough for its
     * glyph to still be treated as reclaimable candidate clef ink -- see the class-level javadoc.
     *
     * @param claimant the Inter currently claiming this glyph (never null)
     * @return the same claimant if it qualifies as a weak, reclaimable claim, else null
     */
    private Inter findWeakClaimant (Inter claimant)
    {
        if (!RECLAIMABLE_SHAPES.contains(claimant.getShape())) {
            return null; // Not a shape family this ink is ever expected to fragment into
        }

        final Double claimantGrade = claimant.getGrade();

        if ((claimantGrade == null)
                || (claimantGrade > constants.weakClaimGradeCeiling.getValue())) {
            return null; // Missing grade, or confidently explained by something else -- hands off
        }

        return claimant;
    }

    //~ Inner Classes --------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Evaluation.Grade residualClefMinGrade = new Evaluation.Grade(
                0.35,
                "Minimum grade for a residual (mid-staff) clef. A much lower floor (0.10) was"
                        + " tried first, on the premise that even a correctly, completely"
                        + " reassembled compound \"routinely\" grades only 0.14-0.17 -- but real"
                        + " data (T061 Subitsky, Music for Children) contradicts that premise:"
                        + " every genuine recovery in that book graded 0.53-0.75, while the 0.10"
                        + " floor let through a real false positive -- a quarter rest recovered"
                        + " as a G_CLEF, confirmed by inspecting the source image at its exact"
                        + " bounds -- graded only 0.084, a coin flip away from that floor. This"
                        + " restores real margin on both sides: comfortably below every genuine"
                        + " recovery actually observed, comfortably above the confirmed false"
                        + " positive. Restricting acceptance to the clef family alone"
                        + " (CANDIDATE_SHAPES) still holds and is unaffected by this change.");

        private final Constant.Integer candidateSearchDepth = new Constant.Integer(
                "shapes",
                5,
                "How many ranked classifier guesses to search for a clef-family shape, rather"
                        + " than requiring it to be the single best guess -- a reclaimed compound"
                        + " built from just a couple of simplified fragments is not guaranteed to"
                        + " have the correct clef shape as its single top guess, only somewhere"
                        + " near the top.");

        private final Evaluation.Grade weakClaimGradeCeiling = new Evaluation.Grade(
                0.65,
                "An Inter of a shape in RECLAIMABLE_SHAPES claiming a glyph at or below this"
                        + " grade is treated as a weak, reclaimable claim rather than a genuine"
                        + " explanation of that ink. Confirmed on a real score: a mid-staff"
                        + " clef's own ink, mis-claimed by the main pipeline, produced a spurious"
                        + " STEM (grade 0.446-0.562) and a spurious NOTEHEAD_VOID (grade"
                        + " 0.301-0.337), while genuine notes immediately nearby in the same area"
                        + " graded 0.77 and above -- set with real margin below the latter and"
                        + " above the former.");

        private final Scale.Fraction staffVerticalMargin = new Scale.Fraction(
                2.0,
                "Vertical margin added above/below a staff when looking for a residual clef");

        private final Scale.Fraction maxPartGap = new Scale.Fraction(
                0.8,
                "Maximum distance between two parts to be considered part of the same clef"
                        + " candidate (matches SymbolsBuilder's own maxGap)");

        private final Scale.AreaFraction minPartWeight = new Scale.AreaFraction(
                0.005,
                "Minimum weight for a single part to be considered (excludes pixel noise)");

        private final Scale.AreaFraction minClefWeight = new Scale.AreaFraction(
                0.5,
                "Minimum total weight for a residual clef candidate");

        private final Scale.AreaFraction maxClefWeight = new Scale.AreaFraction(
                6.0,
                "Maximum total weight for a residual clef candidate");

        private final Scale.Fraction minClefWidth = new Scale.Fraction(
                0.6,
                "Minimum width for a residual clef candidate");

        private final Scale.Fraction maxClefWidth = new Scale.Fraction(
                4.0,
                "Maximum width for a residual clef candidate");

        private final Scale.Fraction minClefHeight = new Scale.Fraction(
                1.5,
                "Minimum height for a residual clef candidate");

        private final Scale.Fraction maxClefHeight = new Scale.Fraction(
                5.0,
                "Maximum height for a residual clef candidate");
    }
}
