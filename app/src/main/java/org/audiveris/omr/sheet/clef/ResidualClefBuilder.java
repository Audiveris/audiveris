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
import java.util.LinkedHashSet;
import java.util.List;
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
 * staff, at whatever ink is still completely unclaimed (no Inter uses it), groups that unclaimed
 * ink by simple proximity, and asks the classifier to judge each resulting group as a whole. This
 * directly implements the idea that a bigger, more complete candidate should be preferred over
 * smaller fragments of the same ink: a real symbol's pieces don't get names of their own once every
 * *other* explanation for them has already been tried and rejected by the main pipeline, so by the
 * time this pass runs, any ink still sitting unclaimed is exactly the ink no smaller interpretation
 * ever fit well enough to claim -- the one thing left to try is the complete union.
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

        for (SystemInfo system : sheet.getSystems()) {
            if (processSystem(system)) {
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
     * @param system the system to inspect
     * @return true if at least one clef was recovered
     */
    private boolean processSystem (SystemInfo system)
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
            if (isClaimedAnywhere(glyph)) {
                continue; // Already explained by some other Inter
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

            final Evaluation[] evals;

            try {
                evals = classifier.evaluate(
                        compound,
                        system,
                        1,
                        constants.residualClefMinGrade.getValue(),
                        EnumSet.of(Classifier.Condition.CHECKED));
            } catch (Exception ex) {
                continue;
            }

            if ((evals.length == 0) || !CANDIDATE_SHAPES.contains(evals[0].shape)) {
                continue;
            }

            final Shape shape = evals[0].shape;
            final double grade = Grades.intrinsicRatio * evals[0].grade;
            final ClefInter clef = ClefInter.createValid(compound, shape, grade, bestStaff);

            if (clef == null) {
                continue;
            }

            system.getSig().addVertex(clef);
            logger.info(
                    "Recovered residual clef {} in staff#{} ({})",
                    clef,
                    bestStaff.getId(),
                    evals[0]);
            recovered = true;
        }

        return recovered;
    }

    //------------------//
    // isClaimedAnywhere //
    //------------------//
    /**
     * Report whether the given glyph is already used by some Inter, in any system -- checked
     * sheet-wide (not just the glyph's own nearest system) since a glyph near a system boundary
     * can end up claimed under a neighboring system, as confirmed with the very same trap while
     * building the accordion-registration residual recoveries.
     *
     * @param glyph the glyph to check
     * @return true if some Inter, anywhere, already uses this glyph
     */
    private boolean isClaimedAnywhere (Glyph glyph)
    {
        for (SystemInfo system : sheet.getSystems()) {
            for (Inter inter : system.getSig().vertexSet()) {
                if (inter.getGlyph() == glyph) {
                    return true;
                }
            }
        }

        return false;
    }

    //~ Inner Classes --------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Evaluation.Grade residualClefMinGrade = new Evaluation.Grade(
                0.10,
                "Minimum grade for a residual (mid-staff) clef -- deliberately lower than the"
                        + " general Grades#symbolMinGrade (0.15). Confirmed on real scores: even a"
                        + " correctly, completely reassembled G_CLEF_SMALL/F_CLEF_SMALL compound"
                        + " -- the full union of every otherwise-unclaimed connected part in its"
                        + " neighborhood, never a fragment of it -- routinely grades only 0.14-0.17,"
                        + " straddling the general floor by a coin flip. Safe to relax here"
                        + " specifically because (1) this method only ever asks about the single,"
                        + " maximal candidate for a given patch of ink, never a competing subset of"
                        + " it, and (2) acceptance is additionally restricted to the clef family"
                        + " alone (CANDIDATE_SHAPES) -- so a lower floor only makes this pass more"
                        + " willing to call a shape a clef, never more willing to call it anything"
                        + " else.");

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
