//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       D o t F a c t o r y                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.Evaluation;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.math.GeoOrder;

import omr.sig.AugmentationDotInter;
import omr.sig.AugmentationRelation;
import omr.sig.BarlineInter;
import omr.sig.DoubleDotRelation;
import omr.sig.Inter;
import omr.sig.RepeatDotBarRelation;
import omr.sig.RepeatDotDotRelation;
import omr.sig.RepeatDotInter;
import omr.sig.SIGraph;
import omr.sig.StaccatoInter;
import omr.sig.StaccatoNoteRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code DotFactory} is a companion of {@link SymbolFactory}, dedicated to the
 * interpretation of dot-shaped symbols.
 * <p>
 * A dot can be:<ul>
 * <li>an augmentation dot (first or second dot), [TODO: Handle augmentation dot for mirrored notes]
 * <li>a part of a repeat sign (upper or lower dot),
 * <li>a staccato sign,
 * <li>a dot of an ending indication, [TODO: Handle dot in ending]
 * <li>a simple text dot.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class DotFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DotFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The related symbol factory. */
    private final SymbolFactory symbolFactory;

    /** The related system. */
    private final SystemInfo system;
    private final SIGraph       sig;
    private final Scale         scale;

    /** Candidates for augmentation dots. */
    private final List<AugDot> augDots = new ArrayList<AugDot>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new DotFactory object.
     *
     * @param symbolFactory the mother factory
     */
    public DotFactory (SymbolFactory symbolFactory)
    {
        this.symbolFactory = symbolFactory;
        system = symbolFactory.getSystem();
        sig = system.getSig();
        scale = system.getSheet().getScale();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------------//
    // checkAugmentations //
    //--------------------//
    /**
     * Perform check for augmentation dots, once rests symbols have been retrieved.
     */
    public void checkAugmentations ()
    {
        // Phase #1: Tests for first augmentation dots
        for (AugDot augDot : augDots) {
            checkAugmentationFirst(augDot);
        }

        // Collect all (first) repeats found so far in this system
        List<Inter> systemFirsts = sig.inters(Shape.AUGMENTATION_DOT);
        Collections.sort(systemFirsts, Inter.byAbscissa);

        // Phase #2: Tests for second augmentation dots (double dots)
        for (AugDot augDot : augDots) {
            checkAugmentationSecond(augDot, systemFirsts);
        }
    }

    //------------//
    // pairRepeat //
    //------------//
    /**
     * Try to pair each repeat dot with another repeat dot.
     */
    public void pairRepeats ()
    {
        List<Inter> dots = sig.inters(Shape.REPEAT_DOT);
        Collections.sort(dots, Inter.byAbscissa);

        for (int i = 0; i < dots.size(); i++) {
            RepeatDotInter dot = (RepeatDotInter) dots.get(i);
            int            dotPitch = dot.getPitch();
            Rectangle      luBox = dot.getBounds();
            luBox.y -= (scale.getInterline() * dotPitch);

            final int xBreak = luBox.x + luBox.width;

            for (Inter inter : dots.subList(i + 1, dots.size())) {
                Rectangle otherBox = inter.getBounds();

                if (luBox.intersects(otherBox)) {
                    RepeatDotInter other = (RepeatDotInter) inter;
                    logger.debug("Pair {} and {}", dot, other);
                    sig.addEdge(dot, other, new RepeatDotDotRelation());
                } else if (otherBox.x >= xBreak) {
                    break;
                }
            }
        }
    }

    //------------//
    // processDot //
    //------------//
    /**
     * Run the various checks for the provided dot-shaped glyph
     *
     * @param eval  evaluation result
     * @param glyph underlying glyph
     */
    public void processDot (Evaluation eval,
                            Glyph      glyph)
    {
        // Try as repeat dot
        checkRepeat(eval, glyph);

        // Try as staccato dot
        checkStaccato(eval, glyph);

        // Postpone the processing as augmentation dot until all symbols have been retrieved
        // So, simply record the candidate glyph
        augDots.add(new AugDot(eval, glyph));
    }

    //------------------------//
    // checkAugmentationFirst //
    //------------------------//
    /**
     * Try to interpret the glyph as an augmentation dot.
     * <p>
     * An augmentation dot can relate to a note or a rest, therefore this method can be called only
     * after all notes and rests interpretations have been retrieved, and rests are retrieved during
     * symbols step.
     *
     * @param augDot a candidate for augmentation dot
     */
    private void checkAugmentationFirst (AugDot augDot)
    {
        // Look for entities (notes and rests) reachable from this glyph
        final int       maxDx = scale.toPixels(AugmentationRelation.getXOutGapMaximum());
        final int       maxDy = scale.toPixels(AugmentationRelation.getYGapMaximum());
        final Point     dotCenter = augDot.glyph.getAreaCenter();
        final Rectangle luBox = new Rectangle(dotCenter);
        luBox.grow(0, maxDy);
        luBox.x -= maxDx;
        luBox.width += maxDx;

        final List<Inter> entities = sig.intersectedInters(
            symbolFactory.getSystemNotes(),
            GeoOrder.BY_ABSCISSA,
            luBox);

        entities.addAll(
            sig.intersectedInters(symbolFactory.getSystemRests(), GeoOrder.BY_ABSCISSA, luBox));

        if (entities.isEmpty()) {
            return;
        }

        // Notes have already been reduced, but not the rests (created as symbols)
        // So we have to set a relation with all acceptable entities
        // This will be later reduced by the sig solver.
        AugmentationDotInter aug = null;

        for (Inter entity : entities) {
            // Select proper entity reference point (center right)
            Point  refPt = entity.getCenterRight();
            double xGap = dotCenter.x - refPt.x;

            if (xGap > 0) {
                double               yGap = Math.abs(refPt.y - dotCenter.y);
                AugmentationRelation rel = new AugmentationRelation();
                rel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

                if (rel.getGrade() >= rel.getMinGrade()) {
                    if (aug == null) {
                        double grade = Inter.intrinsicRatio * augDot.eval.grade;
                        aug = new AugmentationDotInter(augDot.glyph, grade);
                        sig.addVertex(aug);
                        logger.debug("Created {}", aug);
                    }

                    sig.addEdge(aug, entity, rel);
                }
            }
        }
    }

    //-------------------------//
    // checkAugmentationSecond //
    //-------------------------//
    /**
     * Try to interpret the glyph as a second augmentation dot, composing a double dot.
     * <p>
     * Candidates are dots left over (too far from note/rest) as well as some dots already
     * recognized as (single) repeats.
     *
     * @param augDot       a candidate for repeat dot
     * @param systemFirsts all (first) repeats recognized during phase #1
     */
    private void checkAugmentationSecond (AugDot      augDot,
                                          List<Inter> systemFirsts)
    {
        // Look for repeats reachable from this glyph
        final int       maxDx = scale.toPixels(DoubleDotRelation.getXOutGapMaximum());
        final int       maxDy = scale.toPixels(DoubleDotRelation.getYGapMaximum());
        final Point     dot = augDot.glyph.getAreaCenter();
        final Rectangle luBox = new Rectangle(dot);
        luBox.grow(0, maxDy);
        luBox.x -= maxDx;
        luBox.width += maxDx;

        final List<Inter>    firsts = sig.intersectedInters(
            systemFirsts,
            GeoOrder.BY_ABSCISSA,
            luBox);

        // Remove the repeat, if any, that corresponds to the glyph at hand
        AugmentationDotInter second = null;

        for (Inter first : firsts) {
            if (first.getGlyph() == augDot.glyph) {
                second = (AugmentationDotInter) first;
                firsts.remove(first);

                break;
            }
        }

        if (firsts.isEmpty()) {
            return;
        }

        DoubleDotRelation bestRel = null;
        Inter             bestFirst = null;
        double            bestYGap = Double.MAX_VALUE;

        for (Inter first : firsts) {
            // Select proper entity reference point (center right)
            Point  refPt = first.getCenterRight();
            double xGap = dot.x - refPt.x;

            if (xGap > 0) {
                double            yGap = Math.abs(refPt.y - dot.y);
                DoubleDotRelation rel = new DoubleDotRelation();
                rel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

                if (rel.getGrade() >= rel.getMinGrade()) {
                    if ((bestRel == null) || (bestYGap > yGap)) {
                        bestRel = rel;
                        bestFirst = first;
                        bestYGap = yGap;
                    }
                }
            }
        }

        if (bestRel != null) {
            if (second == null) {
                double grade = Inter.intrinsicRatio * augDot.eval.grade;
                second = new AugmentationDotInter(augDot.glyph, grade);
                sig.addVertex(second);
                logger.debug("Created {}", second);
            } else {
                // Here, we have a second dot with two relations:
                // - First dot is linked to some note/rest entities
                // - Second dot is linked to some note/rest entities and also to first dot
                // Since yGap between dots is very strict, just make second dot focus on double dots
                sig.removeAllEdges(sig.getRelations(second, AugmentationRelation.class));
            }

            sig.addEdge(second, bestFirst, bestRel);
            logger.info("DoubleDot relation {} over {}", second, bestFirst);
        }
    }

    //-------------//
    // checkRepeat //
    //-------------//
    /**
     * Try to interpret the provided glyph as a repeat dot.
     *
     * @param eval  evaluation result
     * @param glyph underlying glyph
     */
    private void checkRepeat (Evaluation eval,
                              Glyph      glyph)
    {
        // Check vertical pitch position within the staff: close to +1 or -1
        double pitchDif = Math.abs(Math.abs(glyph.getPitchPosition()) - 1);
        double maxDif = RepeatDotBarRelation.getYGapMaximum().getValue();

        // Rough sanity check
        if (pitchDif > (2 * maxDif)) {
            return;
        }

        final int       maxDx = scale.toPixels(RepeatDotBarRelation.getXOutGapMaximum());
        final int       maxDy = scale.toPixels(RepeatDotBarRelation.getYGapMaximum());
        final Point     dot = glyph.getAreaCenter();
        final Rectangle luBox = new Rectangle(dot);
        luBox.grow(maxDx, maxDy);

        final List<Inter> bars = sig.intersectedInters(
            symbolFactory.getSystemBars(),
            GeoOrder.BY_ABSCISSA,
            luBox);

        if (bars.isEmpty()) {
            return;
        }

        RepeatDotBarRelation bestRel = null;
        Inter                bestBar = null;
        double               bestXGap = Double.MAX_VALUE;

        for (Inter barInter : bars) {
            BarlineInter         bar = (BarlineInter) barInter;
            Rectangle            box = bar.getBounds();
            Point                center = bar.getCenter();

            // Select proper bar reference point (left or right side and proper vertical side)
            double               barY = center.y +
                                        ((box.height / 8d) * Integer.signum(dot.y - center.y));
            double               barX = bar.getMedian().xAtY(barY) +
                                        ((bar.getWidth() / 2) * Integer.signum(dot.x - center.x));

            double               xGap = Math.abs(barX - dot.x);
            double               yGap = Math.abs(barY - dot.y);
            RepeatDotBarRelation rel = new RepeatDotBarRelation();
            rel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

            if (rel.getGrade() >= rel.getMinGrade()) {
                if ((bestRel == null) || (bestXGap > xGap)) {
                    bestRel = rel;
                    bestBar = bar;
                    bestXGap = xGap;
                }
            }
        }

        if (bestRel != null) {
            double         grade = Inter.intrinsicRatio * eval.grade;
            int            pitch = (glyph.getPitchPosition() > 0) ? 1 : (-1);
            RepeatDotInter repeat = new RepeatDotInter(glyph, grade, pitch);
            sig.addVertex(repeat);
            sig.addEdge(repeat, bestBar, bestRel);

            if (glyph.isVip()) {
                logger.info("VIP Created {} from glyph#{}", repeat, glyph.getId());
            }
        }
    }

    //---------------//
    // checkStaccato //
    //---------------//
    /**
     * Try to interpret the provided glyph as a staccato sign.
     * TODO: Use the method for staccatissimo glyph as well
     *
     * @param eval  evaluation result
     * @param glyph underlying glyph
     */
    private void checkStaccato (Evaluation eval,
                                Glyph      glyph)
    {
        final int       maxDx = scale.toPixels(StaccatoNoteRelation.getXOutGapMaximum());
        final int       maxDy = scale.toPixels(StaccatoNoteRelation.getYGapMaximum());
        final Point     dot = glyph.getAreaCenter();
        final Rectangle luBox = new Rectangle(dot);
        luBox.grow(maxDx, maxDy);

        final List<Inter> notes = sig.intersectedInters(
            symbolFactory.getSystemNotes(),
            GeoOrder.BY_ABSCISSA,
            luBox);

        if (notes.isEmpty()) {
            return;
        }

        StaccatoNoteRelation bestRel = null;
        Inter                bestNote = null;
        double               bestYGap = Double.MAX_VALUE;

        for (Inter note : notes) {
            Rectangle            box = note.getBounds();
            Point                center = note.getCenter();

            // Select proper note reference point (top or bottom)
            Point                notePt = new Point(
                center.x,
                center.y + ((box.height / 2) * Integer.signum(dot.y - center.y)));
            double               xGap = Math.abs(notePt.x - dot.x);
            double               yGap = Math.abs(notePt.y - dot.y);
            StaccatoNoteRelation rel = new StaccatoNoteRelation();
            rel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

            if (rel.getGrade() >= rel.getMinGrade()) {
                if ((bestRel == null) || (bestYGap > yGap)) {
                    bestRel = rel;
                    bestNote = note;
                    bestYGap = yGap;
                }
            }
        }

        if (bestRel != null) {
            double        grade = Inter.intrinsicRatio * eval.grade;
            StaccatoInter staccato = new StaccatoInter(glyph, grade);
            sig.addVertex(staccato);
            sig.addEdge(staccato, bestNote, bestRel);
            logger.debug("Created {}", staccato);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------//
    // AugDot //
    //--------//
    /**
     * Remember a candidate glyph for augmentation dot.
     */
    private static class AugDot
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Evaluation eval; // Evaluation result
        final Glyph      glyph; // Underlying glyph

        //~ Constructors ---------------------------------------------------------------------------

        public AugDot (Evaluation eval,
                       Glyph      glyph)
        {
            this.eval = eval;
            this.glyph = glyph;
        }
    }
}
