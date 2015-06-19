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
package omr.sheet.symbol;

import omr.glyph.Evaluation;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.math.GeoOrder;
import omr.math.Rational;

import omr.sheet.Scale;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.AugmentationDotInter;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.FermataDotInter;
import omr.sig.inter.FermataInter;
import omr.sig.inter.Inter;
import omr.sig.inter.RepeatDotInter;
import omr.sig.inter.StaccatoInter;
import omr.sig.relation.AugmentationRelation;
import omr.sig.relation.DotFermataRelation;
import omr.sig.relation.DoubleDotRelation;
import omr.sig.relation.RepeatDotBarRelation;
import omr.sig.relation.RepeatDotDotRelation;
import omr.sig.relation.StaccatoChordRelation;

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
 * Some processing can be done instantly while the symbol is being built, other dot processing
 * require symbols nearby and thus can take place only when all other symbols have been built.
 * <p>
 * A dot can be:<ul>
 * <li>a part of a repeat sign (upper or lower dot),
 * <li>a staccato sign,
 * <li>an augmentation dot (first or second dot), [TODO: Handle augmentation dot for mirrored notes]
 * <li>a part of fermata sign,
 * <li>a dot of an ending indication, [TODO: Handle dot in ending]
 * <li>a simple text dot. [TODO: Anything to be done here?]
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

    private final SIGraph sig;

    private final Scale scale;

    /** Dot candidates. */
    private final List<Dot> dots = new ArrayList<Dot>();

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
    //---------------//
    // lateDotChecks //
    //---------------//
    /**
     * Launch all processing that can take place only after all symbols interpretations
     * have been retrieved for the system.
     */
    public void lateDotChecks ()
    {
        // Sort dots by abscissa
        Collections.sort(dots);

        // Run all late checks
        lateRepeatPairChecks(); // Relation between the two repeat dots of a barline
        lateAugmentationChecks(); // Note-Dot and Note-Dot-Dot configurations
        lateFermataChecks(); // Dot as part of a fermata sign
    }

    //------------//
    // processDot //
    //------------//
    /**
     * Run the various checks for the provided dot-shaped glyph.
     * <p>
     * All symbols may not be available yet, so only instant processing is launched on the dot (as
     * a repeat dot, as a staccato dot).
     * <p>
     * The symbol is also saved as a dot candidate for later processing.
     *
     * @param eval  evaluation result
     * @param glyph underlying glyph
     */
    public void processDot (Evaluation eval,
                            Glyph glyph)
    {
        // Simply record the candidate dot
        Dot dot = new Dot(eval, glyph);
        dots.add(dot);

        // Run instant checks
        instantCheckRepeat(dot); // Repeat dot (relation between the two repeat dots is postponed)
        instantCheckStaccato(dot); // Staccato dot
    }

    //-------------------//
    // filterMirrorHeads //
    //-------------------//
    /**
     * If the collection of (dot-related) heads contains mirrored heads, keep only the
     * head with longer duration
     *
     * @param heads the heads looked up near a candidate augmentation dot
     */
    private void filterMirrorHeads (List<Inter> heads)
    {
        if (heads.size() < 2) {
            return;
        }

        Collections.sort(heads, Inter.byId);

        boolean modified;

        do {
            modified = false;

            InterLoop:
            for (Inter inter : heads) {
                AbstractHeadInter head = (AbstractHeadInter) inter;
                Inter mirrorInter = head.getMirror();

                if ((mirrorInter != null) && heads.contains(mirrorInter)) {
                    AbstractHeadInter mirror = (AbstractHeadInter) mirrorInter;
                    Rational hDur = head.getChord().getDurationSansDotOrTuplet();
                    Rational mDur = mirror.getChord().getDurationSansDotOrTuplet();

                    switch (mDur.compareTo(hDur)) {
                    case -1:
                        heads.remove(mirror);
                        modified = true;

                        break InterLoop;

                    case +1:
                        heads.remove(head);
                        modified = true;

                        break InterLoop;

                    case 0:
                        // Same duration (but we don't have flags yet!)
                        // Keep the one with lower ID
                        heads.remove(mirror);
                        modified = true;

                        break InterLoop;
                    }
                }
            }
        } while (modified);
    }

    //--------------------//
    // instantCheckRepeat //
    //--------------------//
    /**
     * Try to interpret the provided glyph as a repeat dot.
     * This method can be called during symbols step since bar-lines are already available.
     *
     * @param dot the candidate dot
     */
    private void instantCheckRepeat (Dot dot)
    {
        // Check vertical pitch position within the staff: close to +1 or -1
        double pitchDif = Math.abs(Math.abs(dot.glyph.getPitchPosition()) - 1);
        double maxDif = RepeatDotBarRelation.getYGapMaximum().getValue();

        // Rough sanity check
        if (pitchDif > (2 * maxDif)) {
            return;
        }

        final int maxDx = scale.toPixels(RepeatDotBarRelation.getXOutGapMaximum());
        final int maxDy = scale.toPixels(RepeatDotBarRelation.getYGapMaximum());
        final Point dotPt = dot.glyph.getAreaCenter();
        final Rectangle luBox = new Rectangle(dotPt);
        luBox.grow(maxDx, maxDy);

        final List<Inter> bars = sig.intersectedInters(
                symbolFactory.getSystemBars(),
                GeoOrder.BY_ABSCISSA,
                luBox);

        if (bars.isEmpty()) {
            return;
        }

        RepeatDotBarRelation bestRel = null;
        Inter bestBar = null;
        double bestXGap = Double.MAX_VALUE;

        for (Inter barInter : bars) {
            BarlineInter bar = (BarlineInter) barInter;
            Rectangle box = bar.getBounds();
            Point center = bar.getCenter();

            // Select proper bar reference point (left or right side and proper vertical side)
            double barY = center.y
                          + ((box.height / 8d) * Integer.signum(dotPt.y - center.y));
            double barX = bar.getMedian().xAtY(barY)
                          + ((bar.getWidth() / 2) * Integer.signum(dotPt.x - center.x));

            double xGap = Math.abs(barX - dotPt.x);
            double yGap = Math.abs(barY - dotPt.y);
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
            final Point center = dot.glyph.getLocation();
            final Staff staff = system.getClosestStaff(center); // Staff is OK
            double grade = Inter.intrinsicRatio * dot.eval.grade;
            int pitch = (dot.glyph.getPitchPosition() > 0) ? 1 : (-1);
            RepeatDotInter repeat = new RepeatDotInter(dot.glyph, grade, staff, pitch);
            sig.addVertex(repeat);
            sig.addEdge(repeat, bestBar, bestRel);

            if (dot.glyph.isVip()) {
                logger.info("VIP Created {} from glyph#{}", repeat, dot.glyph.getId());
            }
        }
    }

    //----------------------//
    // instantCheckStaccato //
    //----------------------//
    /**
     * Try to interpret the provided glyph as a staccato sign related to note head.
     * This method can be called during symbols step, since only head-chords (not rest-chords) are
     * concerned and head-based chords are already available.
     * <p>
     * TODO: Use the method for staccatissimo glyph as well
     *
     * @param dot the candidate dot
     */
    private void instantCheckStaccato (Dot dot)
    {
        final int maxDx = scale.toPixels(StaccatoChordRelation.getXOutGapMaximum());
        final int maxDy = scale.toPixels(StaccatoChordRelation.getYGapMaximum());
        final Rectangle dotBox = dot.glyph.getBounds();
        final Point dotPt = dot.glyph.getAreaCenter();
        final Rectangle luBox = new Rectangle(dotPt);
        luBox.grow(maxDx, maxDy);

        final List<Inter> chords = sig.intersectedInters(
                symbolFactory.getSystemHeadChords(),
                GeoOrder.BY_ABSCISSA,
                luBox);

        if (chords.isEmpty()) {
            return;
        }

        StaccatoChordRelation bestRel = null;
        Inter bestChord = null;
        double bestYGap = Double.MAX_VALUE;

        for (Inter chord : chords) {
            Rectangle chordBox = chord.getBounds();

            // The staccato dot cannot intersect the chord
            if (chordBox.intersects(dotBox)) {
                continue;
            }

            Point center = chord.getCenter();

            // Select proper chord reference point (top or bottom)
            int yRef = (dotPt.y > center.y) ? (chordBox.y + chordBox.height)
                    : chordBox.y;
            double xGap = Math.abs(center.x - dotPt.x);
            double yGap = Math.abs(yRef - dotPt.y);
            StaccatoChordRelation rel = new StaccatoChordRelation();
            rel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

            if (rel.getGrade() >= rel.getMinGrade()) {
                if ((bestRel == null) || (bestYGap > yGap)) {
                    bestRel = rel;
                    bestChord = chord;
                    bestYGap = yGap;
                }
            }
        }

        if (bestRel != null) {
            double grade = Inter.intrinsicRatio * dot.eval.grade;
            StaccatoInter staccato = new StaccatoInter(dot.glyph, grade);
            sig.addVertex(staccato);
            sig.addEdge(staccato, bestChord, bestRel);
            logger.debug("Created {}", staccato);
        }
    }

    //------------------------//
    // lateAugmentationChecks //
    //------------------------//
    /**
     * Perform check for augmentation dots, once rests symbols have been retrieved.
     */
    private void lateAugmentationChecks ()
    {
        // Phase #1: Tests for first augmentation dots
        for (Dot dot : dots) {
            lateFirstAugmentationCheck(dot);
        }

        // Collect all (first) augmentation dots found so far in this system
        List<Inter> systemFirsts = sig.inters(Shape.AUGMENTATION_DOT);
        Collections.sort(systemFirsts, Inter.byAbscissa);

        // Phase #2: Tests for second augmentation dots (double dots)
        for (Dot dot : dots) {
            lateSecondAugmentationCheck(dot, systemFirsts);
        }
    }

    //-------------------//
    // lateFermataChecks //
    //-------------------//
    /**
     * Try to include the dot in a fermata symbol.
     */
    private void lateFermataChecks ()
    {
        // Collection of fermata candidates in the system
        List<Inter> fermatas = sig.inters(FermataInter.class);

        if (fermatas.isEmpty()) {
            return;
        }

        for (Dot dot : dots) {
            Rectangle dotBox = dot.glyph.getBounds();
            FermataDotInter dotInter = null;

            for (Inter fermata : fermatas) {
                // Box: use lower half for FERMATA and upper half for FERMATA_BELOW
                Rectangle halfBox = fermata.getBounds();
                halfBox.height /= 2;

                if (fermata.getShape() == Shape.FERMATA) {
                    halfBox.y += halfBox.height;
                }

                if (halfBox.intersects(dotBox)) {
                    final Point dotCenter = dot.glyph.getAreaCenter();
                    double xGap = Math.abs(
                            dotCenter.x - (halfBox.x + (halfBox.width / 2)));
                    double yTarget = (fermata.getShape() == Shape.FERMATA_BELOW)
                            ? (halfBox.y + (halfBox.height * 0.25))
                            : (halfBox.y + (halfBox.height * 0.75));
                    double yGap = Math.abs(dotCenter.y - yTarget);
                    DotFermataRelation rel = new DotFermataRelation();
                    rel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

                    if (rel.getGrade() >= rel.getMinGrade()) {
                        if (dotInter == null) {
                            double grade = Inter.intrinsicRatio * dot.eval.grade;
                            dotInter = new FermataDotInter(dot.glyph, grade);
                            sig.addVertex(dotInter);
                            logger.debug("Created {}", dotInter);
                        }

                        sig.addEdge(dotInter, fermata, rel);
                        logger.debug("{} matches dot glyph#{}", fermata, dot.glyph.getId());
                    }
                }
            }
        }
    }

    //----------------------------//
    // lateFirstAugmentationCheck //
    //----------------------------//
    /**
     * Try to interpret the glyph as an augmentation dot.
     * <p>
     * An augmentation dot can relate to a note or a rest, therefore this method can be called only
     * after all notes and rests interpretations have been retrieved, and rests are retrieved during
     * symbols step.
     *
     * @param dot a candidate for augmentation dot
     */
    private void lateFirstAugmentationCheck (Dot dot)
    {
        // Look for entities (notes and rests) reachable from this glyph
        final int maxDx = scale.toPixels(AugmentationRelation.getXOutGapMaximum());
        final int maxDy = scale.toPixels(AugmentationRelation.getYGapMaximum());
        final Point dotCenter = dot.glyph.getAreaCenter();
        final Rectangle luBox = new Rectangle(dotCenter);
        luBox.grow(0, maxDy);
        luBox.x -= maxDx;
        luBox.width += maxDx;

        // Relevant heads?
        final List<Inter> entities = SIGraph.intersectedInters(
                symbolFactory.getSystemHeads(),
                GeoOrder.BY_ABSCISSA,
                luBox);

        // Beware of mirrored heads: link only to the head with longer duration
        filterMirrorHeads(entities);

        // Relevant rests?
        entities.addAll(
                SIGraph.intersectedInters(symbolFactory.getSystemRests(), GeoOrder.BY_ABSCISSA, luBox));

        if (entities.isEmpty()) {
            return;
        }

        // Heads have already been reduced, but not the rests (created as symbols)
        // So we have to set a relation with all acceptable entities
        // This will be later solved by the sig reducer.
        AugmentationDotInter augInter = null;

        for (Inter entity : entities) {
            // Select proper entity reference point (center right)
            Point refPt = entity.getCenterRight();
            double xGap = dotCenter.x - refPt.x;

            if (xGap > 0) {
                double yGap = Math.abs(refPt.y - dotCenter.y);
                AugmentationRelation rel = new AugmentationRelation();
                rel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

                if (rel.getGrade() >= rel.getMinGrade()) {
                    if (augInter == null) {
                        double grade = Inter.intrinsicRatio * dot.eval.grade;
                        augInter = new AugmentationDotInter(dot.glyph, grade);
                        sig.addVertex(augInter);
                        logger.debug("Created {}", augInter);
                    }

                    sig.addEdge(augInter, entity, rel);

                    // We cannot yet safely assign a containing staff to the augmentation dot
                    // (Plus it would be useless!)
                }
            }
        }
    }

    //----------------------//
    // lateRepeatPairChecks //
    //----------------------//
    /**
     * Try to pair each repeat dot with another repeat dot, once all dot-repeat symbols
     * have been retrieved.
     */
    private void lateRepeatPairChecks ()
    {
        List<Inter> repeatDots = sig.inters(Shape.REPEAT_DOT);
        Collections.sort(repeatDots, Inter.byAbscissa);

        for (int i = 0; i < repeatDots.size(); i++) {
            RepeatDotInter dot = (RepeatDotInter) repeatDots.get(i);
            int dotPitch = dot.getIntegerPitch();
            Rectangle luBox = dot.getBounds();
            luBox.y -= (scale.getInterline() * dotPitch);

            final int xBreak = luBox.x + luBox.width;

            for (Inter inter : repeatDots.subList(i + 1, repeatDots.size())) {
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

    //-----------------------------//
    // lateSecondAugmentationCheck //
    //-----------------------------//
    /**
     * Try to interpret the glyph as a second augmentation dot, composing a double dot.
     * <p>
     * Candidates are dots left over (too far from note/rest) as well as some dots already
     * recognized as (single) dots.
     *
     * @param dot          a candidate for augmentation dot
     * @param systemFirsts all (first) augmentation dots recognized during phase #1
     */
    private void lateSecondAugmentationCheck (Dot dot,
                                              List<Inter> systemFirsts)
    {
        if (dot.glyph.isVip()) {
            logger.info("VIP lateSecondAugmentationCheck for {}", dot);
        }

        // Look for augmentation dots reachable from this glyph
        final int maxDx = scale.toPixels(DoubleDotRelation.getXOutGapMaximum());
        final int maxDy = scale.toPixels(DoubleDotRelation.getYGapMaximum());
        final Point dotCenter = dot.glyph.getAreaCenter();
        final Rectangle luBox = new Rectangle(dotCenter);
        luBox.grow(0, maxDy);
        luBox.x -= maxDx;
        luBox.width += maxDx;

        final List<Inter> firsts = sig.intersectedInters(
                systemFirsts,
                GeoOrder.BY_ABSCISSA,
                luBox);

        // Remove the augmentation dot, if any, that corresponds to the glyph at hand
        AugmentationDotInter second = null;

        for (Inter first : firsts) {
            if (first.getGlyph() == dot.glyph) {
                second = (AugmentationDotInter) first;
                firsts.remove(first);

                break;
            }
        }

        if (firsts.isEmpty()) {
            return;
        }

        DoubleDotRelation bestRel = null;
        Inter bestFirst = null;
        double bestYGap = Double.MAX_VALUE;

        for (Inter first : firsts) {
            // Select proper entity reference point (center right)
            Point refPt = first.getCenterRight();
            double xGap = dotCenter.x - refPt.x;

            if (xGap > 0) {
                double yGap = Math.abs(refPt.y - dotCenter.y);
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
                double grade = Inter.intrinsicRatio * dot.eval.grade;
                second = new AugmentationDotInter(dot.glyph, grade);
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
            logger.debug("DoubleDot relation {} over {}", second, bestFirst);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----//
    // Dot //
    //-----//
    /**
     * Remember a dot candidate, for late processing.
     * For augmentation dot, for fermata dot. TODO: Perhaps others
     */
    private static class Dot
            implements Comparable<Dot>
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Evaluation eval; // Evaluation result

        final Glyph glyph; // Underlying glyph

        //~ Constructors ---------------------------------------------------------------------------
        public Dot (Evaluation eval,
                    Glyph glyph)
        {
            this.eval = eval;
            this.glyph = glyph;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int compareTo (Dot that)
        {
            return Glyph.byAbscissa.compare(glyph, that.glyph);
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{Dot");
            sb.append(" glyph#").append(glyph.getId());
            sb.append(" ").append(eval);
            sb.append("}");

            return sb.toString();
        }
    }
}
