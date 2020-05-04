//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       D o t F a c t o r y                                      //
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
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.Staff.IndexedLedger;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.note.NotePosition;
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
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.RepeatDotInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.DotFermataRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.RepeatDotBarRelation;
import org.audiveris.omr.sig.relation.RepeatDotPairRelation;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Class {@code DotFactory} is a companion of {@link InterFactory}, dedicated to the
 * interpretation of dot-shaped symbols.
 * <p>
 * Some dot processing can be done instantly while the symbol is being built, other dot processing
 * may require symbols nearby and thus can take place only when all other symbols have been built.
 * Hence implementing methods are named "instant*()" or "late*()" respectively.
 * <p>
 * A dot can be:
 * <ul>
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

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(DotFactory.class);

    /** The related inter factory. */
    private final InterFactory interFactory;

    /** The related system. */
    private final SystemInfo system;

    private final SIGraph sig;

    private final Scale scale;

    /** Dot candidates. Sorted top down, then left to right. */
    private final List<Dot> dots = new ArrayList<>();

    /**
     * Creates a new DotFactory object.
     *
     * @param interFactory the mother factory
     * @param system       underlying system
     */
    public DotFactory (InterFactory interFactory,
                       SystemInfo system)
    {
        this.interFactory = interFactory;
        this.system = system;
        sig = system.getSig();
        scale = system.getSheet().getScale();
    }

    //------------------//
    // instantDotChecks //
    //------------------//
    /**
     * Run the various initial checks for the provided dot-shaped glyph.
     * <p>
     * All symbols may not be available yet, so only instant processing is launched on the dot (as
     * a repeat dot, as a staccato dot).
     * <p>
     * Whatever the role of a dot, it cannot be stuck (or too close) to a staff line or ledger.
     * <p>
     * The symbol is also saved as a dot candidate for later processing.
     *
     * @param eval         evaluation result
     * @param glyph        underlying glyph
     * @param closestStaff staff closest to the dot
     */
    public void instantDotChecks (Evaluation eval,
                                  Glyph glyph,
                                  Staff closestStaff)
    {
        // Discard glyph too close to staff line or ledger
        if (!checkDistanceToLine(glyph, closestStaff)) {
            return;
        }

        // Simply record the candidate dot
        Dot dot = new GlyphDot(eval, glyph);
        dots.add(dot);

        // Run instant checks
        instantCheckRepeat(dot); // Repeat dot (relation between the two repeat dots is postponed)
        instantCheckStaccato(dot); // Staccato dot

        // We cannot run augmentation check since rest inters may not have been created yet
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
        // Sort dots carefully
        Collections.sort(dots, Dot.comparator);

        // Run all late checks
        lateAugmentationChecks(); // Note-Dot and Note-Dot-Dot configurations
        lateFermataChecks(); // Dot as part of a fermata sign
        lateRepeatChecks(); // Dot as part of stack repeat (to say the last word)
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
        final List<Inter> repeatDots = sig.inters(Shape.REPEAT_DOT);
        Collections.sort(repeatDots, Inters.byAbscissa);

        for (int i = 0; i < repeatDots.size(); i++) {
            final RepeatDotInter dot = (RepeatDotInter) repeatDots.get(i);
            final Rectangle dotLuBox = dot.getDotLuBox(system);
            final int xBreak = dotLuBox.x + dotLuBox.width;

            for (Inter inter : repeatDots.subList(i + 1, repeatDots.size())) {
                Rectangle otherBox = inter.getBounds();

                if (dotLuBox.intersects(otherBox)) {
                    RepeatDotInter other = (RepeatDotInter) inter;
                    logger.debug("Pair {} and {}", dot, other);
                    sig.addEdge(dot, other, new RepeatDotPairRelation());
                } else if (otherBox.x >= xBreak) {
                    break;
                }
            }
        }
    }

    //---------------------//
    // checkDistanceToLine //
    //---------------------//
    /**
     * Check that dot glyph is vertically distant from staff line or ledger.
     * <p>
     * For a tablature, check is always OK.
     * <p>
     * For a standard staff (5) or for a OneLineStaff (1), distance to closest line or ledger
     * is checked.
     *
     * @param glyph the dot glyph to check
     * @param staff the closest staff
     * @return true if OK
     */
    private boolean checkDistanceToLine (Glyph glyph,
                                         Staff staff)
    {
        if (staff.isTablature()) {
            return true;
        }

        final Point center = glyph.getCenter();
        final double distance; // Specified in interline fraction

        if (staff.isOneLineStaff()) {
            final double dy = Math.abs(center.y - staff.getMidLine().yAt(center.x));
            distance = dy / staff.getSpecificInterline();
        } else {
            final NotePosition notePosition = staff.getNotePosition(center);
            final IndexedLedger ledger = notePosition.getLedger();
            final double pitch = notePosition.getPitchPosition();

            if ((Math.abs(pitch) > staff.getLineCount()) && (ledger == null)) {
                if (glyph.isVip()) {
                    logger.info("VIP glyph#{} dot isolated OK", glyph.getId());
                }

                return true;
            }

            // Distance to line/ledger specified in interline fraction (5-line staff)
            double linePitch = 2 * ((ledger != null) ? (2 + ledger.index) : Math.rint(pitch / 2));
            distance = Math.abs(pitch - linePitch) / 2;
        }

        if (distance >= constants.minDyFromLine.getValue()) {
            if (glyph.isVip()) {
                logger.info(
                        "VIP glyph#{} dot distance:{} OK",
                        glyph.getId(),
                        String.format("%.2f", distance));
            }

            return true;
        } else {
            if (glyph.isVip()) {
                logger.info(
                        "VIP glyph#{} dot distance:{} too close",
                        glyph.getId(),
                        String.format("%.2f", distance));
            }

            return false;
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

                dot.remove();
            }
        }
    }

    //-------------------//
    // checkStackRepeats //
    //-------------------//
    /**
     * Check left and right sides of every stack for (dot-based) repeat indication.
     * If quorum reached, discard all overlapping interpretations (such as augmentation dots).
     */
    private void checkStackRepeats ()
    {
        for (MeasureStack stack : system.getStacks()) {
            for (HorizontalSide side : HorizontalSide.values()) {
                final List<RepeatDotInter> repeatDots = new ArrayList<>();
                int virtualDotCount = 0; // Virtual dots inferred from StaffBarline shape

                for (Measure measure : stack.getMeasures()) {
                    final Part part = measure.getPart();
                    final PartBarline partBarline = measure.getPartBarlineOn(side);

                    if (partBarline == null) {
                        continue;
                    }

                    for (Staff staff : part.getStaves()) {
                        StaffBarlineInter staffBarline = partBarline.getStaffBarline(part, staff);
                        BarlineInter bar = (side == LEFT) ? staffBarline.getRightBar()
                                : staffBarline.getLeftBar();

                        if (bar != null) {
                            // Use bar members
                            Set<Relation> dRels = sig.getRelations(bar, RepeatDotBarRelation.class);

                            if (dRels.isEmpty()) {
                                continue;
                            }

                            for (Relation rel : dRels) {
                                RepeatDotInter dot = (RepeatDotInter) sig.getOppositeInter(
                                        bar,
                                        rel);
                                repeatDots.add(dot);
                                logger.debug("Repeat dot for {}", dot);
                            }
                        } else {
                            // Use StaffBarline shape
                            Shape shape = staffBarline.getShape();

                            if (side == LEFT) {
                                if ((shape == Shape.LEFT_REPEAT_SIGN)
                                            || (shape == Shape.BACK_TO_BACK_REPEAT_SIGN)) {
                                    virtualDotCount += 2;
                                }
                            } else {
                                if ((shape == Shape.RIGHT_REPEAT_SIGN)
                                            || (shape == Shape.BACK_TO_BACK_REPEAT_SIGN)) {
                                    virtualDotCount += 2;
                                }
                            }
                        }
                    }
                }

                int dotCount = repeatDots.size() + virtualDotCount;
                int staffCount = system.getStaves().size() - system.getTablatures().size();
                logger.trace("{} {} staves:{} dots:{}", stack, side, staffCount, dotCount);

                // In theory, we should have dotCount == 2 * staffCount
                // Let's simply use quorum of 50% to decide on true repeat indication
                if (dotCount >= staffCount) {
                    // It's a repeat side, delete inters that conflict with repeat dots
                    // This works for real dots only, not for virtual ones
                    List<Inter> toDelete = new ArrayList<>();

                    for (RepeatDotInter dot : repeatDots) {
                        Rectangle dotBox = dot.getBounds();

                        for (Inter inter : sig.vertexSet()) {
                            if (inter == dot || inter.isImplicit()) {
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
                            inter.remove();
                        }
                    }
                }
            }
        }
    }

    //-------------------//
    // filterMirrorHeads //
    //-------------------//
    /**
     * NO LONGER USED.
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

        Collections.sort(heads, Inters.byId);

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
        // Sanity check on pitch
        final Rectangle dotBounds = dot.getBounds();
        final Point dotPt = GeoUtil.centerOf(dotBounds);

        if (!RepeatDotInter.checkPitch(system, dotPt, false)) {
            return;
        }

        // Allocate inter
        final double pp = system.estimatedPitch(dotPt);
        final double pitch = (pp > 0) ? 1 : (-1);
        final Glyph glyph = dot.getGlyph();
        final Staff staff = system.getClosestStaff(dotPt); // Staff is OK
        final RepeatDotInter repeat = new RepeatDotInter(glyph, 0, staff, pitch);

        // Check barline nearby
        final Link barLink = repeat.lookupBarLink(system, interFactory.getSystemBars());

        if (barLink == null) {
            return;
        }

        repeat.setGrade(Grades.intrinsicRatio * dot.getGrade());

        sig.addVertex(repeat);
        barLink.applyTo(repeat);

        if (dot.isVip()) {
            if (glyph != null) {
                logger.info("VIP Created {} from glyph#{}", repeat, glyph.getId());
            } else {
                int annId = dot.getAnnotationId();
                logger.info("VIP Created {} from annotation#{}", repeat, annId);
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
        Glyph glyph = dot.getGlyph();

        if (glyph != null) {
            ArticulationInter.createValidAdded(
                    glyph,
                    Shape.STACCATO,
                    Grades.intrinsicRatio * dot.getGrade(),
                    system,
                    interFactory.getSystemHeadChords());
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
        // Phase #1: Tests for note augmentation dots
        for (Dot dot : dots) {
            lateNoteAugmentationCheck(dot);
        }

        // Collect all (first) augmentation dots found so far in this system
        List<Inter> systemFirsts = sig.inters(Shape.AUGMENTATION_DOT);
        Collections.sort(systemFirsts, Inters.byAbscissa);

        // Phase #2: Tests for dot augmentation dots (double dots)
        for (Dot dot : dots) {
            lateDotAugmentationCheck(dot, systemFirsts);
        }
    }

    //--------------------------//
    // lateDotAugmentationCheck //
    //--------------------------//
    /**
     * Try to interpret the glyph as a second augmentation dot, composing a double dot.
     * <p>
     * Candidates are dots left over (too far from note/rest) as well as some dots already
     * recognized as (single) dots.
     *
     * @param dot          a candidate for augmentation dot
     * @param systemFirsts all (first) augmentation dots recognized during phase #1
     */
    private void lateDotAugmentationCheck (Dot dot,
                                           List<Inter> systemFirsts)
    {
        if (dot.isVip()) {
            logger.info("VIP lateDotAugmentationCheck for {}", dot);
        }

        double grade = Grades.intrinsicRatio * dot.getGrade();
        Glyph glyph = dot.getGlyph();
        AugmentationDotInter second = new AugmentationDotInter(glyph, grade);
        Link bestDotLink = Link.bestOf(second.lookupDotLinks(systemFirsts, system));

        if (bestDotLink != null) {
            sig.addVertex(second);
            sig.removeAllEdges(sig.getRelations(second, AugmentationRelation.class));
            bestDotLink.applyTo(second);
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
            Glyph glyph = dot.getGlyph();

            if (glyph == null) {
                continue;
            }

            Rectangle dotBox = dot.getBounds();
            FermataDotInter dotInter = null;

            for (Inter arc : arcs) {
                // Box: use lower half for FERMATA_ARC and upper half for FERMATA_ARC_BELOW
                Rectangle halfBox = arc.getBounds();
                halfBox.height /= 2;

                if (arc.getShape() == Shape.FERMATA_ARC) {
                    halfBox.y += halfBox.height;
                }

                if (halfBox.intersects(dotBox)) {
                    final Point dotCenter = GeoUtil.centerOf(dotBox);
                    double xGap = Math.abs(dotCenter.x - (halfBox.x + (halfBox.width / 2)));
                    double yTarget = (arc.getShape() == Shape.FERMATA_ARC_BELOW) ? (halfBox.y
                                                                                            + (halfBox.height
                                                                                               * 0.25))
                            : (halfBox.y + (halfBox.height * 0.75));
                    double yGap = Math.abs(dotCenter.y - yTarget);
                    DotFermataRelation rel = new DotFermataRelation();
                    rel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), false);

                    if (rel.getGrade() >= rel.getMinGrade()) {
                        if (dotInter == null) {
                            double grade = Grades.intrinsicRatio * dot.getGrade();
                            dotInter = new FermataDotInter(glyph, grade);
                            sig.addVertex(dotInter);
                            logger.debug("Created {}", dotInter);
                        }

                        sig.addEdge(dotInter, arc, rel);
                        logger.debug("{} matches dot glyph#{}", arc, glyph.getId());
                    }
                }
            }
        }
    }

    //---------------------------//
    // lateNoteAugmentationCheck //
    //---------------------------//
    /**
     * Try to interpret the glyph as an augmentation dot.
     * <p>
     * An augmentation dot can relate to a note or a rest, therefore this method can be called only
     * after all notes and rests interpretations have been retrieved, and rests are retrieved during
     * symbols step.
     *
     * @param dot a candidate for augmentation dot
     */
    private void lateNoteAugmentationCheck (Dot dot)
    {
        if (dot.isVip()) {
            logger.info("VIP lateNoteAugmentationCheck for {}", dot);
        }

        double grade = Grades.intrinsicRatio * dot.getGrade();
        Glyph glyph = dot.getGlyph();
        AugmentationDotInter aug = new AugmentationDotInter(glyph, grade);

        List<Link> links = new ArrayList<>();
        Link headLink = aug.lookupHeadLink(interFactory.getSystemHeadChords(), system);
        links.addAll(aug.sharedHeadLinks(headLink));
        links.addAll(aug.lookupRestLinks(interFactory.getSystemRests(), system));

        if (!links.isEmpty()) {
            sig.addVertex(aug);
            aug.setStaff(links.get(0).partner.getStaff());

            for (Link link : links) {
                link.applyTo(aug);
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
        checkStackRepeats();
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction minDyFromLine = new Scale.Fraction(
                0.3,
                "Minimum vertical distance between dot center and staff line/ledger");
    }

    //-----//
    // Dot //
    //-----//
    /**
     * Remember a dot candidate, for late processing.
     */
    private abstract static class Dot
    {

        /**
         * Very specific sorting of dots.
         * <p>
         * If the 2 dots overlap vertically, return left one first.
         * Otherwise, return top one first.
         *
         * @param that the other dot
         * @return order sign
         */
        public static final Comparator<Dot> comparator = new Comparator<Dot>()
        {
            @Override
            public int compare (Dot d1,
                                Dot d2)
            {
                if (d1 == d2) {
                    return 0;
                }

                final Rectangle b1 = d1.getBounds();
                final Rectangle b2 = d2.getBounds();

                if (GeoUtil.yOverlap(b1, b2) > 0) {
                    return Integer.compare(b1.x, b2.x);
                } else {
                    return Integer.compare(b1.y, b2.y);
                }
            }
        };

        public abstract int getAnnotationId ();

        public abstract Rectangle getBounds ();

        public abstract Glyph getGlyph ();

        public abstract double getGrade ();

        public abstract OmrShape getOmrShape ();

        public abstract boolean isVip ();
    }

    //----------//
    // GlyphDot //
    //----------//
    /**
     * Glyph-based dot.
     */
    private static class GlyphDot
            extends Dot
    {

        private final Glyph glyph; // Underlying glyph

        private final Evaluation eval; // Evaluation result

        GlyphDot (Evaluation eval,
                  Glyph glyph)
        {
            this.eval = eval;
            this.glyph = glyph;
        }

        @Override
        public int getAnnotationId ()
        {
            return 0;
        }

        @Override
        public Rectangle getBounds ()
        {
            return glyph.getBounds();
        }

        @Override
        public Glyph getGlyph ()
        {
            return glyph;
        }

        @Override
        public double getGrade ()
        {
            return eval.grade;
        }

        @Override
        public OmrShape getOmrShape ()
        {
            return null;
        }

        @Override
        public boolean isVip ()
        {
            return glyph.isVip();
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("GlyphDot{");
            sb.append("glyph#").append(glyph.getId());
            sb.append(" ").append(eval);
            sb.append("}");

            return sb.toString();
        }
    }
}
