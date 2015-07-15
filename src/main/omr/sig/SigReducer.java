//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S i g R e d u c e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.ShapeSet;

import static omr.glyph.ShapeSet.Alterations;
import static omr.glyph.ShapeSet.CoreBarlines;
import static omr.glyph.ShapeSet.Flags;

import omr.math.GeoOrder;
import omr.math.GeoUtil;

import omr.sheet.Scale;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.rhythm.SystemBackup;

import omr.sig.inter.AbstractBeamInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.AbstractNoteInter;
import omr.sig.inter.AlterInter;
import omr.sig.inter.AugmentationDotInter;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.BeamHookInter;
import omr.sig.inter.BlackHeadInter;
import omr.sig.inter.ChordInter;
import omr.sig.inter.FullBeamInter;
import omr.sig.inter.Inter;
import omr.sig.inter.LedgerInter;
import omr.sig.inter.RepeatDotInter;
import omr.sig.inter.SlurInter;
import omr.sig.inter.SmallBeamInter;
import omr.sig.inter.SmallBlackHeadInter;
import omr.sig.inter.StemInter;
import omr.sig.inter.StringSymbolInter;
import omr.sig.inter.TimeInter;
import omr.sig.inter.TimeNumberInter;
import omr.sig.inter.TupletInter;
import omr.sig.inter.VoidHeadInter;
import omr.sig.inter.WordInter;
import omr.sig.relation.AbstractConnection;
import omr.sig.relation.AccidHeadRelation;
import omr.sig.relation.AugmentationRelation;
import omr.sig.relation.BeamHeadRelation;
import omr.sig.relation.BeamPortion;
import omr.sig.relation.BeamStemRelation;
import omr.sig.relation.DoubleDotRelation;
import omr.sig.relation.Exclusion;
import omr.sig.relation.HeadStemRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.RepeatDotDotRelation;
import omr.sig.relation.StemPortion;

import static omr.sig.relation.StemPortion.*;

import omr.sig.relation.TimeNumberRelation;

import omr.util.HorizontalSide;

import static omr.util.HorizontalSide.*;

import omr.util.Navigable;
import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Class {@code SigReducer} deals with SIG reduction.
 * <ul>
 * <li>TODO: A small slur around a tuplet sign should be deleted (no interest).</li>
 * <li>TODO: A small slur around a dot should be deleted (it's a fermata instead).</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class SigReducer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SigReducer.class);

    /** Shapes for which overlap detection is (currently) disabled. */
    private static final EnumSet disabledShapes = EnumSet.copyOf(
            Arrays.asList(Shape.LEDGER, Shape.CRESCENDO, Shape.DIMINUENDO));

    /** Predicate for non-disabled overlap. */
    private static final Predicate<Inter> overlapPredicate = new Predicate<Inter>()
    {
        @Override
        public boolean check (Inter inter)
        {
            return !disabledShapes.contains(inter.getShape());
        }
    };

    /** Shapes that can overlap with a beam. */
    private static final EnumSet beamCompShapes = EnumSet.copyOf(CoreBarlines);

    /** Shapes that can overlap with a slur. */
    private static final EnumSet slurCompShapes = EnumSet.noneOf(Shape.class);

    static {
        slurCompShapes.addAll(Alterations.getShapes());
        slurCompShapes.addAll(CoreBarlines);
        slurCompShapes.addAll(Flags.getShapes());
    }

    /** Shapes that can overlap with a stem. */
    private static final EnumSet stemCompShapes = EnumSet.copyOf(
            Arrays.asList(Shape.SLUR, Shape.CRESCENDO, Shape.DIMINUENDO));

    //~ Enumerations -------------------------------------------------------------------------------
    /** Standard vs Small size. */
    private static enum Size
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        STANDARD,
        SMALL;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** Scale. */
    @Navigable(false)
    private final Scale scale;

    /** The related SIG. */
    private final SIGraph sig;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SigReducer} object.
     *
     * @param system the related system
     */
    public SigReducer (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
        scale = system.getSheet().getScale();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // detectOverlaps //
    //----------------//
    /**
     * Detect all cases where 2 Inters actually overlap and, if there is no support
     * relation between them, insert a mutual exclusion.
     * <p>
     * This method is key!
     *
     * @param inters the collection of inters to process
     */
    public static void detectOverlaps (List<Inter> inters)
    {
        Collections.sort(inters, Inter.byAbscissa);

        for (int i = 0, iBreak = inters.size() - 1; i < iBreak; i++) {
            Inter left = inters.get(i);

            if (left.isDeleted()) {
                continue;
            }

            final Rectangle leftBox = left.getBounds();
            final Inter leftMirror = left.getMirror();

            final List<? extends Inter> mirrorNotes;

            if (leftMirror instanceof ChordInter) {
                mirrorNotes = ((ChordInter) leftMirror).getNotes();
            } else {
                mirrorNotes = null;
            }

            final double xMax = leftBox.getMaxX();

            for (Inter right : inters.subList(i + 1, inters.size())) {
                if (right.isDeleted()) {
                    continue;
                }

                // Mirror entities do not exclude one another
                if (leftMirror == right) {
                    continue;
                }

                if ((mirrorNotes != null) && mirrorNotes.contains(right)) {
                    continue;
                }

                // Overlap is accepted in some cases
                if (compatible(new Inter[]{left, right})) {
                    continue;
                }

                Rectangle rightBox = right.getBounds();

                if (leftBox.intersects(rightBox)) {
                    // Have a more precise look
                    if (left.isVip() && right.isVip()) {
                        ////////logger.info("VIP check overlap {} vs {}", left, right);
                    }

                    if (left.overlaps(right) && right.overlaps(left)) {
                        // Specific case: Word vs "string" Symbol
                        if (left instanceof WordInter && right instanceof StringSymbolInter) {
                            if (wordMatchesSymbol((WordInter) left, (StringSymbolInter) right)) {
                                left.decrease(0.5);
                            }
                        } else if (left instanceof StringSymbolInter && right instanceof WordInter) {
                            if (wordMatchesSymbol((WordInter) right, (StringSymbolInter) left)) {
                                right.decrease(0.5);
                            }
                        }

                        // If there is no support between left & right, insert an exclusion
                        SIGraph sig = left.getSig();

                        if (sig.noSupport(left, right)) {
                            sig.insertExclusion(left, right, Exclusion.Cause.OVERLAP);
                        }
                    }
                } else if (rightBox.x > xMax) {
                    break; // Since inters list is sorted by abscissa
                }
            }
        }
    }

    //---------------//
    // contextualize //
    //---------------//
    /**
     * Compute contextual grades of all SIG inters based on their supporting partners.
     */
    public void contextualize ()
    {
        try {
            for (Inter inter : sig.vertexSet()) {
                sig.computeContextualGrade(inter, false);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    //--------//
    // reduce //
    //--------//
    /**
     * Reduce all the interpretations and relations of the SIG.
     *
     * @param purgeWeaks true for purging weak inters
     */
    public void reduce (boolean purgeWeaks)
    {
        final boolean logging = false;

        // Just for debug
        if (logging) {
            logger.info("S#{} reducing sig ...", system.getId());
        }

        // General exclusions based on overlap
        detectOverlaps(sig.inters(overlapPredicate));

        // Inters that conflict with frozen inters must be deleted
        analyzeFrozenInters();

        // Make sure all inters have their contextual grade up-to-date
        contextualize();

        // Heads & beams compatibility
        analyzeChords();

        int modifs; // modifications done in current iteration
        int reductions; // Count of reductions performed
        int deletions; // Count of deletions performed

        do {
            // First, remove all inters with too low contextual grade
            deletions = purgeWeakInters(purgeWeaks);

            do {
                modifs = 0;
                // Detect lack of mandatory support relation for certain inters
                modifs += checkHeads();
                deletions += purgeWeakInters(purgeWeaks);

                modifs += checkHooks();
                deletions += purgeWeakInters(purgeWeaks);

                modifs += checkBeams();
                deletions += purgeWeakInters(purgeWeaks);

                modifs += checkLedgers();
                deletions += purgeWeakInters(purgeWeaks);

                modifs += checkStems();
                deletions += purgeWeakInters(purgeWeaks);

                if (logging) {
                    logger.info("S#{} modifs: {}", system.getId(), modifs);
                }
            } while (modifs > 0);

            // Remaining exclusions
            reductions = sig.reduceExclusions().size();

            if (logging) {
                logger.info("S#{} reductions: {}", system.getId(), reductions);
            }
        } while ((reductions > 0) || (deletions > 0));
    }

    //---------------//
    // reduceSymbols //
    //---------------//
    /**
     * Reduce interpretations while saving discarded rhythm data.
     *
     * @param optionals (output) where selected inters must be backed up
     * @param classes   selected classes
     */
    public void reduceSymbols (SystemBackup optionals,
                               Class<?>... classes)
    {
        final boolean logging = false;
        final boolean purgeWeaks = false; // TODO: check this
        final Set<Inter> allReductions = new HashSet<Inter>();

        // Just for debug
        if (logging) {
            logger.info("S#{} reduceSymbols sig ...", system.getId());
        }

        // General exclusions based on overlap
        detectOverlaps(sig.inters(overlapPredicate));
        //
        //        // Inters that conflict with frozen inters must be deleted
        //        analyzeFrozenInters();
        //
        // Make sure all inters have their contextual grade up-to-date
        contextualize();

        // Heads & beams compatibility (needed for cue beams!)
        analyzeChords();

        // All inters of selected classes
        List<Inter> selected = sig.inters(classes);
        optionals.save(selected);

        int modifs; // modifications done in current iteration
        int reductions; // Count of reductions performed
        int deletions; // Count of deletions performed

        do {
            // First, remove all inters with too low contextual grade
            deletions = purgeWeakInters(purgeWeaks);

            deletions += checkSlurOnTuplet();

            do {
                modifs = 0;
                // Detect lack of mandatory support relation for certain inters
                modifs += checkDoubleAlters();
                deletions += purgeWeakInters(purgeWeaks);

                modifs += checkTimeNumbers();
                deletions += checkTimeSignatures();
                deletions += purgeWeakInters(purgeWeaks);

                modifs += checkRepeatDots();
                modifs += checkAugmentationDots();
                modifs += checkAugmented();
                deletions += purgeWeakInters(purgeWeaks);

                if (logging) {
                    logger.info("S#{} modifs: {}", system.getId(), modifs);
                }
            } while (modifs > 0);

            // Remaining exclusions
            Set<Inter> red = sig.reduceExclusions();
            reductions = red.size();
            allReductions.addAll(red);

            if (logging) {
                logger.info("S#{} reductions: {}", system.getId(), reductions);
            }
        } while ((reductions > 0) || (deletions > 0));

        // Retain only the relevant inters
        allReductions.retainAll(selected);

        optionals.setSeeds(allReductions);
    }

    //------------//
    // compatible //
    //------------//
    /**
     * Check whether the two provided Inter instance can overlap.
     *
     * @param inters array of exactly 2 instances
     * @return true if overlap is accepted, false otherwise
     */
    private static boolean compatible (Inter[] inters)
    {
        for (int i = 0; i <= 1; i++) {
            Inter inter = inters[i];
            Inter other = inters[1 - i];

            if (inter instanceof AbstractBeamInter) {
                if (other instanceof AbstractBeamInter) {
                    return true;
                }

                if (beamCompShapes.contains(other.getShape())) {
                    return true;
                }
            } else if (inter instanceof SlurInter) {
                if (slurCompShapes.contains(other.getShape())) {
                    return true;
                }
            } else if (inter instanceof StemInter) {
                if (stemCompShapes.contains(other.getShape())) {
                    return true;
                }
            }
        }

        return false;
    }

    //-------------------//
    // wordMatchesSymbol //
    //-------------------//
    /**
     * Check whether the word and the symbol might represent the same thing, after all.
     *
     * @param wordInter text word
     * @param symbol    symbol
     */
    private static boolean wordMatchesSymbol (WordInter wordInter,
                                              StringSymbolInter symbol)
    {
        logger.debug("Comparing {} and {}", wordInter, symbol);

        final String symbolString = symbol.getSymbolString();

        if (wordInter.getValue().equalsIgnoreCase(symbolString)) {
            logger.debug("Math found");

            //TODO: Perhaps more checks on word/sentence?
            return true;
        }

        return false;
    }

    //---------------//
    // analyzeChords //
    //---------------//
    /**
     * Analyze consistency of note heads & beams attached to a (good) stem.
     */
    private void analyzeChords ()
    {
        // All stems of the sig
        List<Inter> stems = sig.inters(Shape.STEM);

        // Heads organized by class (black, void, and small versions)
        Map<Class, Set<Inter>> heads = new HashMap<Class, Set<Inter>>();

        // Beams organized by size (standard vs small versions)
        Map<Size, Set<Inter>> beams = new EnumMap<Size, Set<Inter>>(Size.class);

        for (Inter stem : stems) {
            if (stem.isVip()) {
                logger.info("VIP analyzeChords with {}", stem);
            }

            // Consider only good stems
            if (!stem.isGood()) {
                continue;
            }

            heads.clear();
            beams.clear();

            // Populate the various head & beam classes around this stem
            for (Relation rel : sig.edgesOf(stem)) {
                if (rel instanceof HeadStemRelation) {
                    Inter head = sig.getEdgeSource(rel);
                    Class classe = head.getClass();
                    Set<Inter> set = heads.get(classe);

                    if (set == null) {
                        heads.put(classe, set = new HashSet<Inter>());
                    }

                    set.add(head);
                } else if (rel instanceof BeamStemRelation) {
                    Inter beam = sig.getEdgeSource(rel);
                    Size size = (beam instanceof SmallBeamInter) ? Size.SMALL : Size.STANDARD;
                    Set<Inter> set = beams.get(size);

                    if (set == null) {
                        beams.put(size, set = new HashSet<Inter>());
                    }

                    set.add(beam);
                }
            }

            // Mutual head exclusion based on head class
            List<Class> headClasses = new ArrayList<Class>(heads.keySet());

            for (int ic = 0; ic < (headClasses.size() - 1); ic++) {
                Class c1 = headClasses.get(ic);
                Set set1 = heads.get(c1);

                for (Class c2 : headClasses.subList(ic + 1, headClasses.size())) {
                    Set set2 = heads.get(c2);
                    exclude(set1, set2);
                }
            }

            // Mutual beam exclusion based on beam size
            List<Size> beamSizes = new ArrayList<Size>(beams.keySet());

            for (int ic = 0; ic < (beamSizes.size() - 1); ic++) {
                Size c1 = beamSizes.get(ic);
                Set set1 = beams.get(c1);

                for (Size c2 : beamSizes.subList(ic + 1, beamSizes.size())) {
                    Set set2 = beams.get(c2);
                    exclude(set1, set2);
                }
            }

            // Head/Beam support or exclusion based on size
            for (Entry<Size, Set<Inter>> entry : beams.entrySet()) {
                Size size = entry.getKey();
                Set<Inter> beamSet = entry.getValue();

                if (size == Size.SMALL) {
                    // Small beams exclude standard heads
                    for (Class classe : new Class[]{BlackHeadInter.class, VoidHeadInter.class}) {
                        Set headSet = heads.get(classe);

                        if (headSet != null) {
                            exclude(beamSet, headSet);
                        }
                    }

                    // Small beams support small heads
                    Set<Inter> smallHeadSet = heads.get(SmallBlackHeadInter.class);

                    if (smallHeadSet != null) {
                        for (Inter smallBeam : beamSet) {
                            BeamStemRelation bs = (BeamStemRelation) sig.getRelation(
                                    smallBeam,
                                    stem,
                                    BeamStemRelation.class);

                            for (Inter smallHead : smallHeadSet) {
                                if (sig.getRelation(smallBeam, smallHead, BeamHeadRelation.class) == null) {
                                    // Use average of beam-stem and head-stem relation grades
                                    HeadStemRelation hs = (HeadStemRelation) sig.getRelation(
                                            smallHead,
                                            stem,
                                            HeadStemRelation.class);
                                    double grade = (bs.getGrade() + hs.getGrade()) / 2;
                                    sig.addEdge(smallBeam, smallHead, new BeamHeadRelation(grade));
                                }
                            }
                        }
                    }
                } else {
                    // Standard beams exclude small heads
                    Set<Inter> smallHeadSet = heads.get(SmallBlackHeadInter.class);

                    if (smallHeadSet != null) {
                        exclude(beamSet, smallHeadSet);
                    }

                    // Standard beams support black heads (not void)
                    Set<Inter> blackHeadSet = heads.get(BlackHeadInter.class);

                    if (blackHeadSet != null) {
                        for (Inter beam : beamSet) {
                            BeamStemRelation bs = (BeamStemRelation) sig.getRelation(
                                    beam,
                                    stem,
                                    BeamStemRelation.class);

                            for (Inter head : blackHeadSet) {
                                if (sig.getRelation(beam, head, BeamHeadRelation.class) == null) {
                                    // Use average of beam-stem and head-stem relation grades
                                    HeadStemRelation hs = (HeadStemRelation) sig.getRelation(
                                            head,
                                            stem,
                                            HeadStemRelation.class);
                                    double grade = (bs.getGrade() + hs.getGrade()) / 2;
                                    sig.addEdge(beam, head, new BeamHeadRelation(grade));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //---------------------//
    // analyzeFrozenInters //
    //---------------------//
    /**
     * Browse all the frozen inters and simply delete any inter that conflicts with them.
     */
    private void analyzeFrozenInters ()
    {
        Set<Inter> toDelete = new HashSet<Inter>();

        for (Inter inter : sig.vertexSet()) {
            if (inter.isFrozen()) {
                for (Relation rel : sig.getRelations(inter, Exclusion.class)) {
                    Inter other = sig.getOppositeInter(inter, rel);

                    if (other.isFrozen()) {
                        logger.error("Conflicting frozen inters {} & {}", inter, other);
                    } else {
                        toDelete.add(other);

                        if (other.isVip()) {
                            logger.info("VIP deleting {} conflicting with frozen {}", other, inter);
                        }
                    }
                }
            }
        }

        sig.deleteInters(toDelete);
    }

    //------------------//
    // beamHasBothStems //
    //------------------//
    private boolean beamHasBothStems (FullBeamInter beam)
    {
        boolean hasLeft = false;
        boolean hasRight = false;

        //        if (beam.isVip()) {
        //            logger.info("VIP beamHasBothStems for {}", beam);
        //        }
        //
        for (Relation rel : sig.edgesOf(beam)) {
            if (rel instanceof BeamStemRelation) {
                BeamStemRelation bsRel = (BeamStemRelation) rel;
                BeamPortion portion = bsRel.getBeamPortion();

                if (portion == BeamPortion.LEFT) {
                    hasLeft = true;
                } else if (portion == BeamPortion.RIGHT) {
                    hasRight = true;
                }
            }
        }

        return hasLeft && hasRight;
    }

    //-----------------------//
    // checkAugmentationDots //
    //-----------------------//
    /**
     * Perform checks on augmentation dots.
     * <p>
     * An augmentation dot needs a target to augment (note, rest or another augmentation dot).
     *
     * @return the count of modifications done
     */
    private int checkAugmentationDots ()
    {
        int modifs = 0;
        final List<Inter> dots = sig.inters(AugmentationDotInter.class);

        for (Inter inter : dots) {
            final AugmentationDotInter dot = (AugmentationDotInter) inter;

            // Check whether the augmentation dot has a target (note or rest or other dot)
            if (!sig.hasRelation(dot, AugmentationRelation.class, DoubleDotRelation.class)) {
                if (dot.isVip() || logger.isDebugEnabled()) {
                    logger.info("Deleting augmentation dot lacking target {}", dot);
                }

                dot.delete();
                modifs++;
            }
        }

        return modifs;
    }

    //----------------//
    // checkAugmented //
    //----------------//
    /**
     * Perform checks on augmented entities.
     * <p>
     * An entity (note, rest or augmentation dot) can have at most one augmentation dot.
     * <p>
     * NOTA: This is OK for rests but not always for heads.
     * TODO: We could address head-chords rather than individual heads and handle the chord
     * augmentation dots as a whole (in parallel with chord heads) and link each chord head with its
     * 'facing' dot.
     *
     * @return the count of modifications done
     */
    private int checkAugmented ()
    {
        int modifs = 0;
        List<Inter> entities = sig.inters(AbstractNoteInter.class);

        for (Inter entity : entities) {
            Set<Relation> rels = sig.getRelations(entity, AugmentationRelation.class);

            if (rels.size() > 1) {
                modifs += reduceAugmentations(rels);

                if (entity.isVip() || logger.isDebugEnabled()) {
                    logger.info("Reduced augmentations for {}", entity);
                }
            }
        }

        return modifs;
    }

    //------------//
    // checkBeams //
    //------------//
    /**
     * Perform checks on beams.
     *
     * @return the count of modifications done
     */
    private int checkBeams ()
    {
        int modifs = 0;
        final List<Inter> beams = sig.inters(FullBeamInter.class);

        for (Inter inter : beams) {
            final FullBeamInter beam = (FullBeamInter) inter;

            if (!beamHasBothStems(beam)) {
                if (beam.isVip() || logger.isDebugEnabled()) {
                    logger.info("VIP Deleting beam lacking stem {}", beam);
                }

                beam.delete();
                modifs++;
            }
        }

        return modifs;
    }

    //-------------------//
    // checkDoubleAlters //
    //-------------------//
    /**
     * Perform checks on double alterations (double sharp or double flat).
     * They need a note head nearby.
     *
     * @return the count of modifications done
     */
    private int checkDoubleAlters ()
    {
        int modifs = 0;
        final List<Inter> doubles = sig.inters(
                Arrays.asList(Shape.DOUBLE_FLAT, Shape.DOUBLE_SHARP));

        for (Inter inter : doubles) {
            final AlterInter alter = (AlterInter) inter;

            // Check whether the double-alter is connected to a note head
            if (!sig.hasRelation(alter, AccidHeadRelation.class)) {
                if (alter.isVip() || logger.isDebugEnabled()) {
                    logger.info("Deleting {} lacking note head", alter);
                }

                alter.delete();
                modifs++;
            }
        }

        return modifs;
    }

    //---------------//
    // checkHeadSide //
    //---------------//
    /**
     * If head is on the wrong side of the stem, check if there is a head on the other
     * side, located one or two step(s) further.
     * <p>
     * If the side is wrong and there is no head on the other side, simply remove this head-stem
     * relation and insert exclusion instead.
     *
     * @param head the head inter (black or void)
     * @return the number of modifications done
     */
    private int checkHeadSide (Inter head)
    {
        if (head.isVip()) {
            logger.info("VIP checkHeadSide for {}", head);
        }

        int modifs = 0;

        // Check all connected stems
        Set<Relation> stemRels = sig.getRelations(head, HeadStemRelation.class);

        RelsLoop:
        for (Relation relation : stemRels) {
            HeadStemRelation rel = (HeadStemRelation) relation;
            StemInter stem = (StemInter) sig.getEdgeTarget(rel);

            // What is the stem direction? (up: dir < 0, down: dir > 0, unknown: 0)
            int dir = stem.getDirection();

            if (dir == 0) {
                if (stem.isVip()) {
                    logger.info("VIP deleting {} with no correct head on either end", stem);
                }

                stem.delete();
                modifs++;

                continue;
            }

            // Side is normal?
            HorizontalSide headSide = rel.getHeadSide();

            if (((headSide == LEFT) && (dir > 0)) || ((headSide == RIGHT) && (dir < 0))) {
                continue; // It's OK
            }

            // Pitch of the note head
            int pitch = ((AbstractHeadInter) head).getIntegerPitch();

            // Target side and target pitches of other head
            // Look for presence of head on other side with target pitch
            HorizontalSide targetSide = headSide.opposite();

            for (int targetPitch = pitch - 1; targetPitch <= (pitch + 1); targetPitch++) {
                if (stem.lookupHead(targetSide, targetPitch) != null) {
                    continue RelsLoop; // OK
                }
            }

            // We have a bad head+stem couple, so let's remove the relationship
            if (head.isVip() || logger.isDebugEnabled()) {
                logger.info("Wrong side for {} on {}", head, stem);
            }

            sig.removeEdge(rel);
            sig.insertExclusion(head, stem, Exclusion.Cause.INCOMPATIBLE);
            modifs++;
        }

        return modifs;
    }

    //------------//
    // checkHeads //
    //------------//
    /**
     * Perform checks on heads.
     *
     * @return the count of modifications done
     */
    private int checkHeads ()
    {
        int modifs = 0;
        final List<Inter> heads = sig.inters(ShapeSet.NoteHeads.getShapes());

        for (Inter head : heads) {
            // Check if the head has a stem relation
            if (!sig.hasRelation(head, HeadStemRelation.class)) {
                if (head.isVip() || logger.isDebugEnabled()) {
                    logger.info("No stem for {}", head);
                }

                head.delete();
                modifs++;

                continue;
            }

            modifs += checkHeadSide(head);
        }

        return modifs;
    }

    //------------//
    // checkHooks //
    //------------//
    /**
     * Perform checks on hooks.
     *
     * @return the count of modifications done
     */
    private int checkHooks ()
    {
        int modifs = 0;
        final List<Inter> inters = sig.inters(BeamHookInter.class);

        for (Inter inter : inters) {
            // Check if the hook has a stem relation
            if (!sig.hasRelation(inter, BeamStemRelation.class)) {
                if (inter.isVip() || logger.isDebugEnabled()) {
                    logger.info("No stem for {}", inter);
                }

                inter.delete();
            }
        }

        return modifs;
    }

    //--------------//
    // checkLedgers //
    //--------------//
    /**
     * Perform checks on ledger.
     *
     * @return the count of modifications done
     */
    private int checkLedgers ()
    {
        // All system note heads, sorted by abscissa
        List<Inter> allHeads = sig.inters(
                ShapeSet.shapesOf(ShapeSet.NoteHeads.getShapes(), ShapeSet.Notes.getShapes()));
        Collections.sort(allHeads, Inter.byAbscissa);

        int modifs = 0;
        boolean modified;

        do {
            modified = false;

            for (Staff staff : system.getStaves()) {
                SortedMap<Integer, SortedSet<LedgerInter>> map = staff.getLedgerMap();

                for (Entry<Integer, SortedSet<LedgerInter>> entry : map.entrySet()) {
                    int index = entry.getKey();
                    SortedSet<LedgerInter> ledgers = entry.getValue();
                    List<LedgerInter> toRemove = new ArrayList<LedgerInter>();

                    for (LedgerInter ledger : ledgers) {
                        if (ledger.isVip()) {
                            logger.info("VIP ledger {}", ledger);
                        }

                        if (!ledgerHasHeadOrLedger(staff, index, ledger, allHeads)) {
                            if (ledger.isVip() || logger.isDebugEnabled()) {
                                logger.info("Deleting orphan ledger {}", ledger);
                            }

                            ledger.delete();
                            toRemove.add(ledger);
                            modified = true;
                            modifs++;
                        }
                    }

                    if (!toRemove.isEmpty()) {
                        ledgers.removeAll(toRemove);
                    }
                }
            }
        } while (modified);

        return modifs;
    }

    //-----------------//
    // checkRepeatDots //
    //-----------------//
    /**
     * Perform checks on repeat dots
     *
     * @return the count of modifications done
     */
    private int checkRepeatDots ()
    {
        int modifs = 0;
        final List<Inter> dots = sig.inters(RepeatDotInter.class);

        for (Inter inter : dots) {
            final RepeatDotInter dot = (RepeatDotInter) inter;

            // Check if the repeat dot has a sibling dot
            if (!sig.hasRelation(dot, RepeatDotDotRelation.class)) {
                if (dot.isVip() || logger.isDebugEnabled()) {
                    logger.info("Deleting repeat dot lacking sibling {}", dot);
                }

                dot.delete();
                modifs++;
            }
        }

        return modifs;
    }

    //-------------------//
    // checkSlurOnTuplet //
    //-------------------//
    /**
     * Detect and remove a small slur around a tuplet sign.
     *
     * @return the count of modifications done
     */
    private int checkSlurOnTuplet ()
    {
        int modifs = 0;
        final int maxSlurWidth = scale.toPixels(constants.maxTupletSlurWidth);
        final List<Inter> slurs = sig.inters(
                new Predicate<Inter>()
                {
                    @Override
                    public boolean check (Inter inter)
                    {
                        return !inter.isDeleted() && (inter instanceof SlurInter)
                               && (inter.getBounds().width <= maxSlurWidth);
                    }
                });

        final List<Inter> tuplets = sig.inters(
                new Predicate<Inter>()
                {
                    @Override
                    public boolean check (Inter inter)
                    {
                        return !inter.isDeleted() && (inter instanceof TupletInter)
                               && (inter.isContextuallyGood());
                    }
                });

        for (Iterator<Inter> it = slurs.iterator(); it.hasNext();) {
            final SlurInter slur = (SlurInter) it.next();

            // Look for a tuplet sign embraced
            final int above = slur.getInfo().above();
            Rectangle box = slur.getBounds();
            box.translate(0, above * box.height);

            for (Inter tuplet : tuplets) {
                if (box.intersects(tuplet.getBounds())) {
                    if (slur.isVip() || logger.isDebugEnabled()) {
                        logger.info("VIP deleting tuplet {}", slur);
                    }

                    it.remove();
                    slur.delete();
                    modifs++;

                    break;
                }
            }
        }

        return modifs;
    }

    //------------//
    // checkStems //
    //------------//
    /**
     * Perform checks on stems.
     *
     * @return the count of modifications done
     */
    private int checkStems ()
    {
        int modifs = 0;
        final List<Inter> stems = sig.inters(Shape.STEM);

        for (Inter inter : stems) {
            final StemInter stem = (StemInter) inter;

            if (!stemHasHeadAtEnd(stem)) {
                if (stem.isVip() || logger.isDebugEnabled()) {
                    logger.info("Deleting stem lacking starting head {}", stem);
                }

                stem.delete();
                modifs++;

                continue;
            }

            if (!stemHasSingleHeadEnd(stem)) {
                modifs++;
            }
        }

        return modifs;
    }

    //------------------//
    // checkTimeNumbers //
    //------------------//
    /**
     * Perform checks on time numbers.
     *
     * @return the count of modifications done
     */
    private int checkTimeNumbers ()
    {
        int modifs = 0;
        final List<Inter> numbers = sig.inters(TimeNumberInter.class);

        for (Inter inter : numbers) {
            final TimeNumberInter number = (TimeNumberInter) inter;

            // Check this number has a sibling number
            if (!sig.hasRelation(number, TimeNumberRelation.class)) {
                if (number.isVip() || logger.isDebugEnabled()) {
                    logger.info("Deleting time number lacking sibling {}", number);
                }

                number.delete();
                modifs++;
            }
        }

        return modifs;
    }

    //---------------------//
    // checkTimeSignatures //
    //---------------------//
    /**
     * Perform checks on time signatures.
     * <p>
     * Check there is no note between measure start and time signature.
     *
     * @return the count of deletions made (0)
     */
    private int checkTimeSignatures ()
    {
        List<Inter> systemNotes = sig.inters(AbstractNoteInter.class);

        if (systemNotes.isEmpty()) {
            return 0;
        }

        final List<Inter> systemTimes = sig.inters(TimeInter.class);
        Collections.sort(systemNotes, Inter.byAbscissa);

        for (Staff staff : system.getStaves()) {
            List<Inter> staffTimes = SIGraph.inters(staff, systemTimes);

            if (staffTimes.isEmpty()) {
                continue;
            }

            List<Inter> notes = SIGraph.inters(staff, systemNotes);

            for (Inter inter : staffTimes) {
                TimeInter timeSig = (TimeInter) inter;

                // Position WRT Notes in staff
                int notePrev = -2 - Collections.binarySearch(notes, timeSig, Inter.byAbscissa);

                if (notePrev != -1) {
                    // Position WRT Bars in staff
                    List<BarlineInter> bars = staff.getBars();
                    int barPrev = -2
                                  - Collections.binarySearch(
                                    bars,
                                    timeSig,
                                    Inter.byAbscissa);
                    int xMin = (barPrev != -1) ? bars.get(barPrev).getCenter().x : 0;

                    for (int i = notePrev; i >= 0; i--) {
                        Inter note = notes.get(i);

                        if (note.getCenter().x < xMin) {
                            break;
                        }

                        if (timeSig.isVip() || note.isVip() || logger.isDebugEnabled()) {
                            logger.info("{} preceding {}", note, timeSig);
                        }

                        sig.insertExclusion(note, timeSig, Exclusion.Cause.INCOMPATIBLE);
                    }
                }
            }
        }

        return 0;
    }

    //---------//
    // exclude //
    //---------//
    /**
     * Insert exclusion between (the members of) the 2 sets.
     *
     * @param set1 one set
     * @param set2 the other set
     */
    private void exclude (Set<Inter> set1,
                          Set<Inter> set2)
    {
        for (Inter i1 : set1) {
            for (Inter i2 : set2) {
                sig.insertExclusion(i1, i2, Exclusion.Cause.INCOMPATIBLE);
            }
        }
    }

    //
    //    //-------------//
    //    // hookHasStem //
    //    //-------------//
    //    /**
    //     * Check if a beam hook has a stem.
    //     */
    //    private boolean hookHasStem (BeamHookInter hook)
    //    {
    //        boolean hasLeft = false;
    //        boolean hasRight = false;
    //
    //        if (hook.isVip()) {
    //            logger.info("VIP hookHasStem for {}", hook);
    //        }
    //
    //        for (Relation rel : sig.edgesOf(hook)) {
    //            if (rel instanceof BeamStemRelation) {
    //                BeamStemRelation bsRel = (BeamStemRelation) rel;
    //                BeamPortion portion = bsRel.getBeamPortion();
    //
    //                if (portion == BeamPortion.LEFT) {
    //                    hasLeft = true;
    //                } else if (portion == BeamPortion.RIGHT) {
    //                    hasRight = true;
    //                }
    //            }
    //        }
    //
    //        return hasLeft || hasRight;
    //    }
    //
    //-----------------------//
    // ledgerHasHeadOrLedger //
    //-----------------------//
    /**
     * Check if the provided ledger has either a note head centered on it
     * (or one step further) or another ledger just further.
     *
     * @param staff    the containing staff
     * @param index    the ledger line index
     * @param ledger   the ledger to check
     * @param allHeads the abscissa-ordered list of heads in the system
     * @return true if OK
     */
    private boolean ledgerHasHeadOrLedger (Staff staff,
                                           int index,
                                           LedgerInter ledger,
                                           List<Inter> allHeads)
    {
        Rectangle ledgerBox = new Rectangle(ledger.getBounds());
        ledgerBox.grow(0, scale.getInterline()); // Very high box, but that's OK

        // Check for another ledger on next line
        int nextIndex = index + Integer.signum(index);
        SortedSet<LedgerInter> nextLedgers = staff.getLedgers(nextIndex);

        if (nextLedgers != null) {
            for (LedgerInter nextLedger : nextLedgers) {
                // Check abscissa compatibility
                if (GeoUtil.xOverlap(ledgerBox, nextLedger.getBounds()) > 0) {
                    return true;
                }
            }
        }

        // Else, check for a note centered on ledger, or just on next pitch
        final int ledgerPitch = Staff.getLedgerPitchPosition(index);
        final int nextPitch = ledgerPitch + Integer.signum(index);

        final List<Inter> heads = sig.intersectedInters(allHeads, GeoOrder.BY_ABSCISSA, ledgerBox);

        for (Inter inter : heads) {
            final AbstractHeadInter head = (AbstractHeadInter) inter;
            final int notePitch = head.getIntegerPitch();

            if ((notePitch == ledgerPitch) || (notePitch == nextPitch)) {
                return true;
            }
        }

        return false;
    }

    //-----------------//
    // purgeWeakInters //
    //-----------------//
    /**
     * Update the contextual grade of each Inter in SIG, and remove the weak ones if so
     * desired.
     *
     * @param purgeWeaks true for removing the inters with weak contextual grade
     * @return the number of inters removed
     */
    private int purgeWeakInters (boolean purgeWeaks)
    {
        contextualize();

        if (purgeWeaks) {
            return sig.deleteWeakInters().size();
        }

        return 0;
    }

    //---------------------//
    // reduceAugmentations //
    //---------------------//
    /**
     * Reduce the number of augmentation relations to one.
     *
     * @param rels the augmentation links for the same entity
     * @return the number of relation deleted
     */
    private int reduceAugmentations (Set<Relation> rels)
    {
        int modifs = 0;

        // Simply select the relation with best grade
        double bestGrade = 0;
        AbstractConnection bestLink = null;

        for (Relation rel : rels) {
            AbstractConnection link = (AbstractConnection) rel;
            double grade = link.getGrade();

            if (grade > bestGrade) {
                bestGrade = grade;
                bestLink = link;
            }
        }

        for (Relation rel : rels) {
            if (rel != bestLink) {
                sig.removeEdge(rel);
                modifs++;
            }
        }

        return modifs;
    }

    //------------------//
    // stemHasHeadAtEnd //
    //------------------//
    /**
     * Check if the stem has at least a head at some end.
     *
     * @param stem the stem inter
     * @return true if OK
     */
    private boolean stemHasHeadAtEnd (StemInter stem)
    {
        if (stem.isVip()) {
            logger.info("VIP stemHasHeadAtEnd for {}", stem);
        }

        final Line2D stemLine = sig.getStemLine(stem);

        for (Relation rel : sig.getRelations(stem, HeadStemRelation.class)) {
            // Check stem portion
            HeadStemRelation hsRel = (HeadStemRelation) rel;
            Inter head = sig.getOppositeInter(stem, rel);

            if (hsRel.getStemPortion(head, stemLine, scale) != STEM_MIDDLE) {
                return true;
            }
        }

        return false;
    }

    //----------------------//
    // stemHasSingleHeadEnd //
    //----------------------//
    /**
     * Check if the stem does not have heads at both ends.
     * <p>
     * If heads are found at the "tail side" of the stem, their relations to the stem are removed.
     *
     * @param stem the stem inter
     * @return true if OK
     */
    private boolean stemHasSingleHeadEnd (StemInter stem)
    {
        final Line2D stemLine = sig.getStemLine(stem);
        final int stemDir = stem.getDirection();

        if (stemDir == 0) {
            return true; // We cannot decide
        }

        final StemPortion forbidden = (stemDir > 0) ? STEM_BOTTOM : STEM_TOP;
        final List<Relation> toRemove = new ArrayList<Relation>();

        for (Relation rel : sig.getRelations(stem, HeadStemRelation.class)) {
            // Check stem portion
            HeadStemRelation hsRel = (HeadStemRelation) rel;
            Inter head = sig.getOppositeInter(stem, rel);
            StemPortion portion = hsRel.getStemPortion(head, stemLine, scale);

            if (portion == forbidden) {
                if (stem.isVip() || logger.isDebugEnabled()) {
                    logger.info("Cutting relation between {} and {}", stem, sig.getEdgeSource(rel));
                }

                toRemove.add(rel);
            }
        }

        if (!toRemove.isEmpty()) {
            sig.removeAllEdges(toRemove);
        }

        return toRemove.isEmpty();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction maxTupletSlurWidth = new Scale.Fraction(
                3,
                "Maximum width for slur around tuplet");
    }
}
