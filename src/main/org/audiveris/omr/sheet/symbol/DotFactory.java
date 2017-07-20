//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       D o t F a c t o r y                                      //
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
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffBarline;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.ArticulationInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.DeletedInterException;
import org.audiveris.omr.sig.inter.FermataArcInter;
import org.audiveris.omr.sig.inter.FermataDotInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.RepeatDotInter;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.DotFermataRelation;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.RepeatDotBarRelation;
import org.audiveris.omr.sig.relation.RepeatDotPairRelation;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class {@code DotFactory} is a companion of {@link SymbolFactory}, dedicated to the
 * interpretation of dot-shaped symbols.
 * <p>
 * Some processing can be done instantly while the symbol is being built, other dot processing
 * may require symbols nearby and thus can take place only when all other symbols have been built.
 * Hence implementing methods are named "instant*()" or "late*()" respectively.
 * <p>
 * A dot can be:<ul>
 * <li>a part of a repeat sign (upper or lower dot),
 * <li>a staccato sign,
 * <li>an augmentation dot (first or second dot), [TODO: Handle augmentation dot for mirrored notes]
 * <li>a part of a fermata sign,
 * <li>a dot of an ending indication, [TODO: Handle dot in ending]
 * <li>a simple text dot. [TODO: Anything to be done here?]
 * <li>or just some stain...
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
    //------------------//
    // instantDotChecks //
    //------------------//
    /**
     * Run the various initial checks for the provided dot-shaped glyph.
     * <p>
     * All symbols may not be available yet, so only instant processing is launched on the dot (as
     * a repeat dot, as a staccato dot).
     * <p>
     * The symbol is also saved as a dot candidate for later processing.
     *
     * @param eval  evaluation result
     * @param glyph underlying glyph
     */
    public void instantDotChecks (Evaluation eval,
                                  Glyph glyph)
    {
        // Simply record the candidate dot
        Dot dot = new Dot(eval, glyph);
        dots.add(dot);

        // Run instant checks
        instantCheckRepeat(dot); // Repeat dot (relation between the two repeat dots is postponed)
        instantCheckStaccato(dot); // Staccato dot

        // We cannot run augmentation repeat check since rest inters may not have been created yet
        // and, in any case, no rest inter will be validated before RHYTHMS step.
    }

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
        lateAugmentationChecks(); // Note-Dot and Note-Dot-Dot configurations
        lateFermataChecks(); // Dot as part of a fermata sign
        lateRepeatChecks(); // Dot as part of stack repeat (to say the last word)
    }

    //--------------------//
    // assignStackRepeats //
    //--------------------//
    /**
     * Check left and right sides of every stack for (dot-based) repeat indication.
     * If quorum reached, discard all other dot interpretations (such as augmentation dots).
     */
    private void assignStackRepeats ()
    {
        for (MeasureStack stack : system.getMeasureStacks()) {
            for (HorizontalSide side : HorizontalSide.values()) {
                final List<RepeatDotInter> repeatDots = new ArrayList<RepeatDotInter>();
                int barCount = 0;

                for (Measure measure : stack.getMeasures()) {
                    final Part part = measure.getPart();
                    final PartBarline partBarline = measure.getBarline(side);

                    if (partBarline == null) {
                        continue;
                    }

                    for (Staff staff : part.getStaves()) {
                        StaffBarline staffBarline = partBarline.getBarline(part, staff);
                        BarlineInter bar = (side == LEFT) ? staffBarline.getRightBar()
                                : staffBarline.getLeftBar();

                        if (bar == null) {
                            continue;
                        }

                        barCount++;

                        Set<Relation> dRels = sig.getRelations(bar, RepeatDotBarRelation.class);

                        if (dRels.isEmpty()) {
                            continue;
                        }

                        for (Relation rel : dRels) {
                            RepeatDotInter dot = (RepeatDotInter) sig.getOppositeInter(bar, rel);
                            repeatDots.add(dot);
                            logger.debug("Repeat dot for {}", dot);
                        }
                    }
                }

                int dotCount = repeatDots.size();
                logger.trace("{} {} bars:{} dots:{}", stack, side, barCount, dotCount);

                if ((dotCount != 0) && (dotCount >= barCount)) {
                    // It's a repeat side, enforce it!
                    stack.addRepeat(side);

                    // Delete inters that conflict with repeat dots
                    List<Inter> toDelete = new ArrayList<Inter>();

                    for (RepeatDotInter dot : repeatDots) {
                        Rectangle dotBox = dot.getBounds();

                        for (Inter inter : sig.vertexSet()) {
                            if (inter == dot) {
                                continue;
                            }

                            try {
                                if (dotBox.intersects(inter.getBounds()) && dot.overlaps(inter)) {
                                    toDelete.add(inter);
                                }
                            } catch (DeletedInterException ignored) {
                            }
                        }
                    }

                    if (!toDelete.isEmpty()) {
                        for (Inter inter : toDelete) {
                            inter.delete();
                        }
                    }
                }
            }
        }
    }

    //------------------//
    // buildRepeatPairs //
    //------------------//
    /**
     * Try to pair each repeat dot with another repeat dot, once all dot-repeat symbols
     * have been retrieved.
     */
    private void buildRepeatPairs ()
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
                    sig.addEdge(dot, other, new RepeatDotPairRelation());
                } else if (otherBox.x >= xBreak) {
                    break;
                }
            }
        }
    }

    //------------------//
    // checkRepeatPairs //
    //------------------//
    /**
     * Delete RepeatDotInter instances that are not paired.
     */
    private void checkRepeatPairs ()
    {
        final List<Inter> repeatDots = sig.inters(RepeatDotInter.class);

        for (Inter inter : repeatDots) {
            final RepeatDotInter dot = (RepeatDotInter) inter;

            // Check if the repeat dot has a sibling dot
            if (!sig.hasRelation(dot, RepeatDotPairRelation.class)) {
                if (dot.isVip() || logger.isDebugEnabled()) {
                    logger.info("Deleting repeat dot lacking sibling {}", dot);
                }

                dot.delete();
            }
        }
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
                HeadInter head = (HeadInter) inter;
                Inter mirrorInter = head.getMirror();

                if ((mirrorInter != null) && heads.contains(mirrorInter)) {
                    HeadInter mirror = (HeadInter) mirrorInter;
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

    //---------------//
    // filterOnStack //
    //---------------//
    private void filterOnStack (List<Inter> inters,
                                MeasureStack dotStack)
    {
        for (Iterator<Inter> it = inters.iterator(); it.hasNext();) {
            if (!dotStack.contains(it.next().getCenter())) {
                it.remove();
            }
        }
    }

    //--------------------//
    // instantCheckRepeat //
    //--------------------//
    /**
     * Try to interpret the provided dot glyph as a repeat dot.
     * This method can be called during symbols step since barlines are already available.
     * <p>
     * For a dot properly located WRT barlines, this method creates an instance of RepeatDotInter
     * as well as a RepeatDotBarRelation between the inter and the related barline.
     *
     * @param dot the candidate dot
     */
    private void instantCheckRepeat (Dot dot)
    {
        // Check vertical pitch position within the staff: close to +1 or -1
        final Point center = dot.glyph.getCenter();
        final double pp = system.estimatedPitch(center);
        double pitchDif = Math.abs(Math.abs(pp) - 1);
        double maxDif = RepeatDotBarRelation.getYGapMaximum().getValue();

        // Rough sanity check
        if (pitchDif > (2 * maxDif)) {
            return;
        }

        final int maxDx = scale.toPixels(RepeatDotBarRelation.getXOutGapMaximum());
        final int maxDy = scale.toPixels(RepeatDotBarRelation.getYGapMaximum());
        final Point dotPt = dot.glyph.getCenter();
        final Rectangle luBox = new Rectangle(dotPt);
        luBox.grow(maxDx, maxDy);

        final List<Inter> bars = SIGraph.intersectedInters(
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
            Point barCenter = bar.getCenter();

            // Select proper bar reference point (left or right side and proper vertical side)
            double barY = barCenter.y
                          + ((box.height / 8d) * Integer.signum(dotPt.y - barCenter.y));
            double barX = LineUtil.xAtY(bar.getMedian(), barY)
                          + ((bar.getWidth() / 2) * Integer.signum(
                    dotPt.x - barCenter.x));

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
            final Staff staff = system.getClosestStaff(center); // Staff is OK
            double grade = Inter.intrinsicRatio * dot.eval.grade;
            double pitch = (pp > 0) ? 1 : (-1);
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
     *
     * @param dot the candidate dot
     */
    private void instantCheckStaccato (Dot dot)
    {
        ArticulationInter.create(
                dot.glyph,
                Shape.STACCATO,
                Inter.intrinsicRatio * dot.eval.grade,
                system,
                symbolFactory.getSystemHeadChords());
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
        // Collection of fermata arc candidates in the system
        List<Inter> arcs = sig.inters(FermataArcInter.class);

        if (arcs.isEmpty()) {
            return;
        }

        for (Dot dot : dots) {
            Rectangle dotBox = dot.glyph.getBounds();
            FermataDotInter dotInter = null;

            for (Inter arc : arcs) {
                // Box: use lower half for FERMATA_ARC and upper half for FERMATA_ARC_BELOW
                Rectangle halfBox = arc.getBounds();
                halfBox.height /= 2;

                if (arc.getShape() == Shape.FERMATA_ARC) {
                    halfBox.y += halfBox.height;
                }

                if (halfBox.intersects(dotBox)) {
                    final Point dotCenter = dot.glyph.getCenter();
                    double xGap = Math.abs(
                            dotCenter.x - (halfBox.x + (halfBox.width / 2)));
                    double yTarget = (arc.getShape() == Shape.FERMATA_ARC_BELOW)
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

                        sig.addEdge(dotInter, arc, rel);
                        logger.debug("{} matches dot glyph#{}", arc, dot.glyph.getId());
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
        // Look for entities (heads and rests) reachable from this glyph
        final Point dotCenter = dot.glyph.getCenter();
        final MeasureStack dotStack = system.getMeasureStackAt(dotCenter);
        final int maxDx = scale.toPixels(AugmentationRelation.getXOutGapMaximum());
        final int maxDy = scale.toPixels(AugmentationRelation.getYGapMaximum());
        final Rectangle luBox = new Rectangle(dotCenter);
        luBox.grow(0, maxDy);
        luBox.x -= maxDx;
        luBox.width += maxDx;

        // Relevant heads?
        final List<Inter> heads = SIGraph.intersectedInters(
                symbolFactory.getSystemHeads(),
                GeoOrder.BY_ABSCISSA,
                luBox);
        filterOnStack(heads, dotStack);

        // Beware of mirrored heads: link only to the head with longer duration
        filterMirrorHeads(heads);

        // Relevant rests?
        final List<Inter> rests = SIGraph.intersectedInters(
                symbolFactory.getSystemRests(),
                GeoOrder.BY_ABSCISSA,
                luBox);
        filterOnStack(rests, dotStack);
        heads.addAll(rests);

        if (heads.isEmpty()) {
            return;
        }

        // Heads have already been reduced, but not the rests (created as symbols)
        // So we have to set a relation with all acceptable entities
        // This will be later solved by the sig reducer.
        AugmentationDotInter augInter = null;

        for (Inter note : heads) {
            // Select proper note reference point (center right)
            Point refPt = note.getCenterRight();
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
                        augInter.setStaff(note.getStaff());
                        logger.debug("Created {}", augInter);
                    }

                    sig.addEdge(augInter, note, rel);
                }
            }
        }
    }

    //------------------//
    // lateRepeatChecks //
    //------------------//
    private void lateRepeatChecks ()
    {
        // Try to establish a relation between the two repeat dots of a barline
        buildRepeatPairs();

        // Purge non-paired repeat dots
        checkRepeatPairs();

        // Assign repeats per stack
        assignStackRepeats();
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
        final Point dotCenter = dot.glyph.getCenter();
        final Rectangle luBox = new Rectangle(dotCenter);
        luBox.grow(0, maxDy);
        luBox.x -= maxDx;
        luBox.width += maxDx;

        final List<Inter> firsts = SIGraph.intersectedInters(
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
            return Glyphs.byAbscissa.compare(glyph, that.glyph);
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
