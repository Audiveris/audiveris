//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S i g R e d u c e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sig;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.header.StaffHeader;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractFlagInter;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import org.audiveris.omr.sig.inter.AbstractPitchedInter;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.EnsembleHelper;
import org.audiveris.omr.sig.inter.FretInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.KeyAlterInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.MeasureCountInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallBeamInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.StringSymbolInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.AbstractConnection;
import org.audiveris.omr.sig.relation.AlterHeadRelation;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.Exclusion;
import static org.audiveris.omr.sig.relation.Exclusion.ExclusionCause.INCOMPATIBLE;
import static org.audiveris.omr.sig.relation.Exclusion.ExclusionCause.OVERLAP;
import org.audiveris.omr.sig.relation.HeadHeadRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.MeasureRepeatCountRelation;
import org.audiveris.omr.sig.relation.MultipleRestCountRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.StemPortion;
import static org.audiveris.omr.sig.relation.StemPortion.STEM_BOTTOM;
import static org.audiveris.omr.sig.relation.StemPortion.STEM_TOP;
import org.audiveris.omr.sig.relation.TimeTopBottomRelation;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Predicate;

/**
 * Class <code>SigReducer</code> deals with SIG reduction.
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

    /** Inter classes for which overlap detection is (currently) disabled. */
    private static final Class<?>[] disabledClasses = new Class<?>[]
    { BeamGroupInter.class, LedgerInter.class, SentenceInter.class, WedgeInter.class };

    /** Predicate for non-disabled overlap. */
    private static final Predicate<Inter> overlapPredicate = (Inter inter) ->
    {
        final Class<?> interClass = inter.getClass();

        for (Class<?> classe : disabledClasses) {
            if (classe.isAssignableFrom(interClass)) {
                return false;
            }
        }

        return true;
    };

    /**
     * Inter classes that can overlap with a beam.
     * NOTA: Barlines are no longer allowed to overlap with a beam
     */
    private static final Class<?>[] beamCompClasses = new Class<?>[]
    { AbstractBeamInter.class };

    /** Inter classes that can overlap with a slur. */
    private static final Class<?>[] slurCompClasses = new Class<?>[]
    { AbstractFlagInter.class, AlterInter.class, BarlineInter.class, StaffBarlineInter.class };

    /** Inter classes that can overlap with a stem. */
    private static final Class<?>[] stemCompClasses = new Class<?>[]
    { SlurInter.class, WedgeInter.class };

    //~ Instance fields ----------------------------------------------------------------------------

    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** Scale. */
    @Navigable(false)
    private final Scale scale;

    /** The related SIG. */
    private final SIGraph sig;

    /** Should we purge weak inter instances?. */
    private final boolean purgeWeaks;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>SigReducer</code> object.
     *
     * @param system     the related system
     * @param purgeWeaks true for purging weak inters
     */
    public SigReducer (SystemInfo system,
                       boolean purgeWeaks)
    {
        this.system = system;
        this.purgeWeaks = purgeWeaks;

        sig = system.getSig();
        scale = system.getSheet().getScale();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // analyzeChords //
    //---------------//
    /**
     * Analyze consistency of note heads & beams attached to a (good) stem.
     * <ul>
     * <li>All (standard) black, all (standard) void, all small black or all small void.
     * <li>All (standard) beams or all small beams.
     * <li>(standard) beams with (standard) heads only.
     * <li>Small beams with small heads only.
     * <li>A stem is in exclusion with any intersected head it is not linked with.
     * </ul>
     */
    private void analyzeChords ()
    {
        logger.debug("S#{} analyzeChords", system.getId());

        // All stems of the sig
        final List<Inter> stems = sig.inters(Shape.STEM);
        Collections.sort(stems, Inters.byReverseGrade);

        // All heads of the sig
        final List<Inter> allHeads = sig.inters(HeadInter.class);
        Collections.sort(allHeads, Inters.byAbscissa);

        // Stem Heads organized by shape
        final Map<Shape, Set<Inter>> heads = new EnumMap<>(Shape.class);

        // Stem Heads organized by shape intrinsic duration
        final Map<Rational, Set<Inter>> durs = new LinkedHashMap<>();

        // Stem Beams organized by size (standard vs small versions)
        final Map<Size, Set<Inter>> beams = new EnumMap<>(Size.class);

        for (Inter stem : stems) {
            if (stem.isVip()) {
                logger.info("VIP analyzeChords with {}", stem);
            }

            // Consider only good stems
            if (!stem.isGood()) {
                break; // Since stems collection is ordered by decreasing grade
            }

            heads.clear();
            durs.clear();
            beams.clear();

            // Heads intersected by current stem
            final List<Inter> intersectedHeads = intersectedHeads((StemInter) stem, allHeads);

            // Populate the various head & beam classes around this stem
            for (Relation rel : sig.edgesOf(stem)) {
                if (rel instanceof HeadStemRelation) {
                    final Inter head = sig.getEdgeSource(rel);
                    intersectedHeads.remove(head);

                    // By shape
                    final Shape shape = head.getShape();
                    Set<Inter> sSet = heads.get(shape);

                    if (sSet == null) {
                        heads.put(shape, sSet = new LinkedHashSet<>());
                    }

                    sSet.add(head);

                    // By duration
                    final Rational duration = shape.getNoteDuration();
                    Set<Inter> dSet = durs.get(duration);

                    if (dSet == null) {
                        durs.put(duration, dSet = new LinkedHashSet<>());
                    }

                    dSet.add(head);

                } else if (rel instanceof BeamStemRelation) {
                    Inter beam = sig.getEdgeSource(rel);
                    Size size = (beam instanceof SmallBeamInter) ? Size.SMALL : Size.STANDARD;
                    Set<Inter> set = beams.get(size);

                    if (set == null) {
                        beams.put(size, set = new LinkedHashSet<>());
                    }

                    set.add(beam);
                }
            }

            // Exclusion between stem and each intersected (non-linked) head
            for (Inter ih : intersectedHeads) {
                sig.insertExclusion(stem, ih, Exclusion.ExclusionCause.OVERLAP);
            }

            // Mutual head exclusion between shapes with different size (small vs standard)
            final Set<Inter> smallHeads = new LinkedHashSet<>();
            final Set<Inter> stdHeads = new LinkedHashSet<>();
            for (Entry<Shape, Set<Inter>> headEntry : heads.entrySet()) {
                final Set<Inter> headSet = headEntry.getValue();
                if (headSet != null) {
                    if (headEntry.getKey().isSmallHead()) {
                        smallHeads.addAll(headSet);
                    } else {
                        stdHeads.addAll(headSet);
                    }
                }
            }
            exclude(smallHeads, stdHeads, INCOMPATIBLE);

            // Mutual head exclusion between shapes with different DURATION
            // Mutual head support within shapes with same DURATION
            final List<Set<Inter>> allDurSets = new ArrayList<>(durs.values());
            for (int is = 0; is < allDurSets.size(); is++) {
                final Set<Inter> oneDurSet = allDurSets.get(is);

                // Exclusion with other duration sets
                for (Set<Inter> otherDurSet : allDurSets.subList(is + 1, allDurSets.size())) {
                    exclude(oneDurSet, otherDurSet, INCOMPATIBLE);
                }

                // Support within same duration set
                final List<Inter> oneDurList = new ArrayList<>(oneDurSet);
                for (int i = 0; i < oneDurList.size(); i++) {
                    final Inter head = oneDurList.get(i);

                    for (Inter sameDur : oneDurList.subList(i + 1, oneDurList.size())) {
                        sig.insertSupport(head, sameDur, HeadHeadRelation.class);
                    }
                }
            }

            // Mutual beam exclusion based on beam size
            List<Size> beamSizes = new ArrayList<>(beams.keySet());

            for (int ic = 0; ic < (beamSizes.size() - 1); ic++) {
                Size c1 = beamSizes.get(ic);
                Set<Inter> set1 = beams.get(c1);

                for (Size c2 : beamSizes.subList(ic + 1, beamSizes.size())) {
                    Set<Inter> set2 = beams.get(c2);
                    exclude(set1, set2, INCOMPATIBLE);
                }
            }

            // Head/Beam support or exclusion based on size
            for (Entry<Size, Set<Inter>> beamEntry : beams.entrySet()) {
                final Size beamSize = beamEntry.getKey();
                final Set<Inter> beamSet = beamEntry.getValue();

                if (beamSize == Size.SMALL) {
                    // Small beams exclude all but small (oval) heads
                    for (Entry<Shape, Set<Inter>> headEntry : heads.entrySet()) {
                        if (!headEntry.getKey().isSmallHead()) {
                            final Set<Inter> headSet = headEntry.getValue();

                            if (headSet != null) {
                                exclude(beamSet, headSet, INCOMPATIBLE);
                            }
                        }
                    }
                } else {
                    // Standard beams exclude small (oval) heads
                    for (Shape small : ShapeSet.HeadsOvalSmall) {
                        final Set<Inter> smallHeadSet = heads.get(small);

                        if (smallHeadSet != null) {
                            exclude(beamSet, smallHeadSet, INCOMPATIBLE);
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
        logger.debug("S#{} analyzeFrozenInters", system.getId());

        Set<Inter> toDelete = new LinkedHashSet<>();

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
    //
    //    //------------------//
    //    // analyzeHeadStems //
    //    //------------------//
    //    /**
    //     * Check any head has at most one stem on left side and one stem on right side.
    //     * If not, the various stems on a same side are mutually exclusive.
    //     */
    //    private void analyzeHeadStems ()
    //    {
    //        logger.debug("S#{} analyzeHeadStems", system.getId());
    //
    //        final List<Inter> heads = sig.inters(ShapeSet.StemHeads);
    //
    //        for (Inter hi : heads) {
    //            HeadInter head = (HeadInter) hi;
    //
    //            // Retrieve connected stems into left and right sides
    //            Map<HorizontalSide, Set<StemInter>> map = head.getSideStems();
    //
    //            // Check each side
    //            for (Entry<HorizontalSide, Set<StemInter>> entry : map.entrySet()) {
    //                Set<StemInter> set = entry.getValue();
    //
    //                if (set.size() > 1) {
    //                    //////////////////////////////////////////////////HB sig.insertExclusions(set, Exclusion.ExclusionCause.OVERLAP);
    //                    //TODO:
    //                    // Instead of stem exclusion, we should disconnect head from some of these stems
    //                    // Either all the stems above or all the stems below
    //                }
    //            }
    //        }
    //    }
    //

    //-----------------------//
    // checkAugmentationDots //
    //-----------------------//
    /**
     * Perform checks on augmentation dots.
     * <p>
     * An augmentation dot needs a target to augment (note, rest or a first augmentation dot).
     *
     * @return the count of modifications done
     */
    private int checkAugmentationDots ()
    {
        logger.debug("S#{} checkAugmentationDots", system.getId());

        int modifs = 0;
        final List<Inter> dots = sig.inters(AugmentationDotInter.class);

        DotLoop:
        for (Inter inter : dots) {
            final AugmentationDotInter dot = (AugmentationDotInter) inter;

            // Look for a target head or rest
            if (sig.hasRelation(dot, AugmentationRelation.class)) {
                continue;
            }

            // Look for a target dot on left side
            final int dotCenterX = dot.getCenter().x;

            for (Relation rel : sig.getRelations(dot, DoubleDotRelation.class)) {
                Inter opposite = sig.getOppositeInter(dot, rel);

                if (opposite.getCenter().x < dotCenterX) {
                    continue DotLoop;
                }
            }

            if (dot.isVip()) {
                logger.info("VIP deleting {} lacking target", dot);
            }

            dot.remove();
            modifs++;
        }

        return modifs;
    }

    //--------------------//
    // checkAugmentedDots //
    //--------------------//
    /**
     * Perform checks on augmented dots (double dots).
     *
     * @return the count of modifications done
     */
    private int checkAugmentedDots ()
    {
        logger.debug("S#{} checkAugmentedDots", system.getId());

        int modifs = 0;
        List<Inter> entities = sig.inters(AugmentationDotInter.class);

        for (Inter entity : entities) {
            Set<Relation> rels = sig.getRelations(entity, DoubleDotRelation.class);

            if (rels.size() > 1) {
                modifs += reduceAugmentations(rels);

                if (entity.isVip()) {
                    logger.info("VIP reduced augmentations for {}", entity);
                }
            }
        }

        return modifs;
    }

    //---------------------//
    // checkAugmentedHeads //
    //---------------------//
    /**
     * Perform checks on augmented heads.
     * <p>
     * Here we work headChord by headChord.
     *
     * @return the count of modifications done
     */
    private int checkAugmentedHeads ()
    {
        logger.debug("S#{} checkAugmentedHeads", system.getId());

        int modifs = 0;
        final List<Inter> headChords = sig.inters(HeadChordInter.class);

        for (Inter hc : headChords) {
            final HeadChordInter chord = (HeadChordInter) hc;

            // Heads, ordered top down
            final List<? extends Inter> heads = EnsembleHelper.getMembers(
                    chord,
                    Inters.byCenterOrdinate);

            for (Inter entity : heads) {
                final Set<Relation> rels = sig.getRelations(entity, AugmentationRelation.class);

                if (rels.size() > 1) {
                    modifs += reduceHeadAugmentations((HeadInter) entity, rels);

                    if (entity.isVip()) {
                        logger.info("VIP reduced augmentations for {}", entity);
                    }
                }
            }
        }

        return modifs;
    }

    //---------------------//
    // checkAugmentedRests //
    //---------------------//
    /**
     * Perform checks on augmented rests.
     *
     * @return the count of modifications done
     */
    private int checkAugmentedRests ()
    {
        logger.debug("S#{} checkAugmentedRests", system.getId());

        int modifs = 0;
        List<Inter> entities = sig.inters(RestInter.class);

        for (Inter entity : entities) {
            Set<Relation> rels = sig.getRelations(entity, AugmentationRelation.class);

            if (rels.size() > 1) {
                modifs += reduceAugmentations(rels);

                if (entity.isVip()) {
                    logger.info("VIP reduced augmentations for {}", entity);
                }
            }
        }

        return modifs;
    }

    //-------------------------//
    // checkBeamsHaveBothStems //
    //-------------------------//
    /**
     * Perform checks on beams.
     *
     * @return the count of modifications done
     */
    private int checkBeamsHaveBothStems ()
    {
        logger.debug("S#{} checkBeamsHaveBothStems", system.getId());

        int modifs = 0;
        final List<Inter> beams = sig.inters(BeamInter.class);

        for (Inter inter : beams) {
            final BeamInter beam = (BeamInter) inter;

            for (BeamPortion portion : new BeamPortion[]
            { BeamPortion.LEFT, BeamPortion.RIGHT }) {
                if (beam.getStemOn(portion) == null) {
                    if (beam.isVip()) {
                        logger.info("VIP deleting beam lacking stem {} on {}", beam, portion);
                    }

                    beam.remove();
                    modifs++;

                    break;
                }
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
        logger.debug("S#{} checkDoubleAlters", system.getId());

        int modifs = 0;
        final List<Inter> doubles = sig.inters(
                Arrays.asList(Shape.DOUBLE_FLAT, Shape.DOUBLE_SHARP));

        for (Inter inter : doubles) {
            final AlterInter alter = (AlterInter) inter;

            // Check whether the double-alter is connected to a note head
            if (!sig.hasRelation(alter, AlterHeadRelation.class)) {
                if (alter.isVip()) {
                    logger.info("VIP deleting {} lacking note head", alter);
                }

                alter.remove();
                modifs++;
            }
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
        logger.debug("S#{} checkHeads (headHasStem + checkHeadSide)", system.getId());

        int modifs = 0;
        final List<Inter> heads = sig.inters(ShapeSet.StemHeads);

        for (Inter head : heads) {
            if (head.isVip()) {
                logger.info("VIP checkheads for {}", head);
            }

            // Check if the head has a stem relation
            if (!headHasStem(head)) {
                head.remove();
                modifs++;

                continue;
            }

            // Check head location relative to stem
            modifs += checkHeadSide(head);
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
     * relation.
     * TODO: If head and stem really overlap, insert exclusion between them.
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
            int dir = stem.computeDirection();

            if (dir == 0) {
                if (stem.isVip()) {
                    logger.info("VIP deleting {} with no correct head on either end", stem);
                }

                stem.remove();
                modifs++;

                continue;
            }

            // Side is normal?
            HorizontalSide headSide = rel.getHeadSide();

            if (((headSide == LEFT) && (dir > 0)) || ((headSide == RIGHT) && (dir < 0))) {
                continue; // It's OK
            }

            // Pitch of the note head
            // Beware for merged grand staff of pitches in the gutter between the two staves
            final Staff staff = head.getStaff();
            final int pitch = ((AbstractPitchedInter) head).getIntegerPitch();

            // Target side and target pitches of other head
            // Look for presence of head on other side with target pitches
            HorizontalSide targetSide = headSide.opposite();

            for (int targetPitch = pitch - 1; targetPitch <= (pitch + 1); targetPitch++) {
                if (lookupHead(stem, targetSide, targetPitch, staff) != null) {
                    continue RelsLoop; // OK
                }
            }

            // We have a bad head+stem couple, so let's remove the relationship
            if (head.isVip()) {
                logger.info("VIP wrong side for {} on {}", head, stem);
            }

            sig.removeEdge(rel);

            // Should we insert an exclusion between head inter and stem inter?
            if (rel.isInvading()) {
                if (stem.isVip() || head.isVip()) {
                    logger.info("VIP invasion between {} & {}", head, stem);
                }

                sig.insertExclusion(head, stem, Exclusion.ExclusionCause.OVERLAP);
            }

            modifs++;
        }

        return modifs;
    }

    //--------------------//
    // checkHooksHaveStem //
    //--------------------//
    /**
     * Perform checks on hooks that must have a stem relation on one horizontal side.
     *
     * @return the count of modifications done
     */
    private int checkHooksHaveStem ()
    {
        logger.debug("S#{} checkHooksHaveStem", system.getId());

        int modifs = 0;
        final List<Inter> inters = sig.inters(BeamHookInter.class);

        for (Inter inter : inters) {
            BeamPortion sidePortion = null;

            for (Relation rel : sig.getRelations(inter, BeamStemRelation.class)) {
                final BeamStemRelation bsRel = (BeamStemRelation) rel;
                final BeamPortion portion = bsRel.getBeamPortion();

                if (portion != BeamPortion.CENTER) {
                    sidePortion = portion;
                }
            }

            if (sidePortion == null) {
                if (inter.isVip()) {
                    logger.info("VIP no side stem for {}", inter);
                }

                inter.remove();
                modifs++;
            }
        }

        return modifs;
    }

    //---------------------//
    // checkIsolatedAlters //
    //---------------------//
    /**
     * Perform checks on isolated alterations.
     * <p>
     * An isolated (not head-related) alter should be allowed only in some cases:
     * <ul>
     * <li>turn sign
     * <li>part of stand-alone key signature
     * <li>key signature cancel in cautionary measure
     * </ul>
     * They are discarded if there is no relation with a nearby head (or ).
     * TODO: rework this when stand-alone key signatures are supported.
     *
     * @return the count of modifications done
     */
    private int checkIsolatedAlters ()
    {
        logger.debug("S#{} checkIsolatedAlters", system.getId());

        int modifs = 0;
        final List<Inter> alters = sig.inters(ShapeSet.Accidentals.getShapes());

        for (Inter inter : alters) {
            if (inter instanceof KeyAlterInter) {
                continue; // Don't consider alters within a key-sig
            }

            final AlterInter alter = (AlterInter) inter;

            // Connected to a note head?
            if (sig.hasRelation(alter, AlterHeadRelation.class)) {
                continue;
            }

            // Within a cautionary measure?
            Point center = inter.getCenter();
            Staff staff = alter.getStaff();

            if (staff == null) {
                List<Staff> stavesAround = system.getStavesAround(center); // 1 or 2 staves
                staff = stavesAround.get(0);
            }

            Part part = staff.getPart();
            Measure measure = part.getMeasureAt(center);

            if ((measure != null) && (measure == part.getLastMeasure()) && (measure
                    .getPartBarlineOn(HorizontalSide.RIGHT) == null)) {
                // Empty measure?
                // Measure width?
                continue;
            }

            if (alter.isVip()) {
                logger.info("VIP deleting isolated {}", alter);
            }

            alter.remove();
            modifs++;
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
        logger.debug("S#{} checkLedgers", system.getId());

        // All system note heads, sorted by abscissa
        List<Inter> allHeads = sig.inters(ShapeSet.Heads);
        Collections.sort(allHeads, Inters.byAbscissa);

        final List<LedgerInter> toDelete = new ArrayList<>();
        boolean modified;

        do {
            modified = false;

            for (Staff staff : system.getStaves()) {
                if (staff.isTablature()) {
                    continue;
                }

                SortedMap<Integer, List<LedgerInter>> map = staff.getLedgerMap();

                // Need a read-only copy to avoid concurrent modifications
                List<Entry<Integer, List<LedgerInter>>> staffLedgers;
                staffLedgers = new ArrayList<>(map.entrySet());

                for (Entry<Integer, List<LedgerInter>> entry : staffLedgers) {
                    int index = entry.getKey();

                    // Need a list copy to avoid concurrent modifications
                    List<LedgerInter> lineLedgers = new ArrayList<>(entry.getValue());

                    for (LedgerInter ledger : lineLedgers) {
                        if (!ledgerHasHeadOrLedger(staff, index, ledger, allHeads)) {
                            if (ledger.isVip()) {
                                logger.info("VIP deleting orphan ledger {}", ledger);
                            }

                            ledger.remove();
                            modified = true;
                        }
                    }
                }
            }
        } while (modified);

        if (!toDelete.isEmpty()) {
            for (LedgerInter ledger : toDelete) {
                ledger.remove(); // This updates the ledgerMap in relevant staves
            }
        }

        return toDelete.size();
    }

    //--------------------//
    // checkMeasureCounts //
    //--------------------//
    /**
     * Check all measure counts are linked to multiple-rest or measure repeat sign.
     *
     * @return the count of modifications done
     */
    private int checkMeasureCounts ()
    {
        logger.debug("S#{} checkMeasureCounts", system.getId());

        int modifs = 0;
        final List<Inter> inters = sig.inters(MeasureCountInter.class);

        for (Inter inter : inters) {
            if (!sig.hasRelation(
                    inter,
                    MultipleRestCountRelation.class,
                    MeasureRepeatCountRelation.class)) {
                if (inter.isVip()) {
                    logger.info("VIP no link for {}", inter);
                }

                inter.remove();
                modifs++;
            }
        }

        return modifs;
    }

    //-------------//
    // checkRomans //
    //-------------//
    /**
     * Check overlapping roman digits and delete the shorter.
     */
    private int checkRomans ()
    {
        int modifs = 0;
        final List<Inter> inters = sig.inters(ShapeSet.Romans.getShapes());
        final List<FretInter> romans = new ArrayList<>();
        for (Inter inter : inters) {
            romans.add((FretInter) inter);
        }

        Collections.sort(romans, FretInter.byDecreasingLength);

        for (int i = 0; i < romans.size(); i++) {
            final FretInter f1 = romans.get(i);
            final Rectangle r1 = f1.getBounds();
            final int lg1 = f1.getSymbolString().length();

            for (int j = i + 1; j < romans.size(); j++) {
                final FretInter f2 = romans.get(j);
                final Rectangle r2 = f2.getBounds();

                if (r1.intersects(r2)) {
                    final int lg2 = f2.getSymbolString().length();

                    if (lg1 > lg2) {
                        f2.remove();
                        romans.remove(f2);
                        modifs++;
                    }
                }
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
     * @return the slurs removed
     */
    private Set<Inter> checkSlurOnTuplet ()
    {
        logger.debug("S#{} checkSlurOnTuplet", system.getId());

        Set<Inter> deleted = new LinkedHashSet<>();
        final int maxSlurWidth = scale.toPixels(constants.maxTupletSlurWidth);
        final List<Inter> slurs = sig.inters(
                (Inter inter) -> !inter.isRemoved() && (inter instanceof SlurInter) && (inter
                        .getBounds().width <= maxSlurWidth));

        final List<Inter> tuplets = sig.inters(
                (Inter inter) -> !inter.isRemoved() && !inter.isImplicit()
                        && (inter instanceof TupletInter) && (inter.isContextuallyGood()));

        for (Inter slurInter : slurs) {
            final SlurInter slur = (SlurInter) slurInter;

            // Look for a tuplet sign embraced
            final int above = slur.isAbove() ? 1 : (-1);
            Rectangle box = slur.getBounds();
            box.translate(0, above * box.height);

            for (Inter tuplet : tuplets) {
                if (box.intersects(tuplet.getBounds())) {
                    if (slur.isVip()) {
                        logger.info("VIP deleting tuplet-slur {}", slur);
                    }

                    slur.remove();
                    deleted.add(slur);

                    break;
                }
            }
        }

        return deleted;
    }

    //----------------------//
    // checkStemEndingHeads //
    //----------------------//
    /**
     * Perform checks on correct side for ending heads of stems.
     *
     * @return the count of modifications done
     */
    private int checkStemEndingHeads ()
    {
        logger.debug("S#{} checkStemEndingHeads (pruneStemHeads)", system.getId());

        int modifs = 0;
        final List<Inter> stems = sig.inters(Shape.STEM);

        for (Inter inter : stems) {
            final StemInter stem = (StemInter) inter;

            // Cut links to ending heads on wrong stem side
            if (pruneStemHeads(stem)) {
                modifs++;
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
        logger.debug("S#{} checkStems (stemHasHeads + stemHasSingleHeadEnd)", system.getId());

        int modifs = 0;
        final List<Inter> stems = sig.inters(Shape.STEM);

        for (Inter inter : stems) {
            final StemInter stem = (StemInter) inter;

            if (stem.isVip()) {
                logger.info("VIP checkStem {}", stem);
            }

            if (stem.getHeads().isEmpty()) {
                if (stem.isVip()) {
                    logger.info("VIP deleting stem lacking heads {}", stem);
                }

                stem.remove();
                modifs++;

                continue;
            }

            //
            //            if (!stemHasHeadAtEnd(stem)) {
            //                if (stem.isVip()) {
            //                    logger.info("VIP deleting stem lacking starting head {}", stem);
            //                }
            //
            //                stem.remove();
            //                modifs++;
            //
            //                continue;
            //            }
            //
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
        logger.debug("S#{} checkTimeNumbers", system.getId());

        int modifs = 0;
        final List<Inter> numbers = sig.inters(TimeNumberInter.class);

        for (Inter inter : numbers) {
            final TimeNumberInter number = (TimeNumberInter) inter;

            // Check this number has a sibling number
            if (!sig.hasRelation(number, TimeTopBottomRelation.class)) {
                if (number.isVip()) {
                    logger.info("VIP deleting time number lacking sibling {}", number);
                }

                number.remove();
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
     * Check there is no note between measure start and time signature, otherwise insert exclusion.
     */
    private void checkTimeSignatures ()
    {
        logger.debug("S#{} checkTimeSignatures", system.getId());

        List<Inter> systemNotes = sig.inters(AbstractNoteInter.class);

        if (systemNotes.isEmpty()) {
            return;
        }

        final List<Inter> systemTimes = sig.inters(AbstractTimeInter.class);
        Collections.sort(systemNotes, Inters.byAbscissa);

        for (Staff staff : system.getStaves()) {
            if (staff.isTablature()) {
                continue;
            }

            List<Inter> staffTimes = Inters.inters(systemTimes, staff);

            if (staffTimes.isEmpty()) {
                continue;
            }

            List<Inter> notes = Inters.inters(systemNotes, staff);

            for (Inter inter : staffTimes) {
                AbstractTimeInter timeSig = (AbstractTimeInter) inter;

                // Position WRT Notes in staff
                int notePrev = -2 - Collections.binarySearch(notes, timeSig, Inters.byAbscissa);

                if (notePrev != -1) {
                    // Position WRT Bars in staff
                    List<BarlineInter> bars = staff.getBarlines();
                    int barPrev = -2 - Collections.binarySearch(bars, timeSig, Inters.byAbscissa);
                    int xMin = (barPrev != -1) ? bars.get(barPrev).getCenter().x : 0;

                    for (int i = notePrev; i >= 0; i--) {
                        Inter note = notes.get(i);

                        if (note.getCenter().x < xMin) {
                            break;
                        }

                        if (timeSig.isVip() || note.isVip()) {
                            logger.info("VIP {} preceding {}", note, timeSig);
                        }

                        sig.insertExclusion(note, timeSig, INCOMPATIBLE);
                    }
                }
            }
        }
    }

    //-----------------------//
    // contextualizeAndPurge //
    //-----------------------//
    /**
     * Update the contextual grade of each Inter in SIG, and remove the weak ones if so
     * desired.
     *
     * @return the set of inters removed
     */
    private Set<Inter> contextualizeAndPurge ()
    {
        sig.contextualize();

        if (purgeWeaks) {
            return sig.deleteWeakInters();
        }

        return Collections.emptySet();
    }

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
    private void detectOverlaps (List<Inter> inters,
                                 ReductionAdapter adapter)
    {
        logger.debug("S#{} detectOverlaps", system.getId());
        Collections.sort(inters, Inters.byAbscissa);

        NextLeft:
        for (int i = 0, iBreak = inters.size() - 1; i < iBreak; i++) {
            Inter left = inters.get(i);

            if (left.isRemoved() || left.isImplicit()) {
                continue;
            }

            final Rectangle leftBox = left.getBounds();
            final Set<Inter> mirrors = new LinkedHashSet<>();

            if (left instanceof HeadInter) {
                HeadInter headMirror = (HeadInter) left.getMirror();

                if (headMirror != null) {
                    mirrors.add(headMirror);

                    HeadChordInter chordMirror = headMirror.getChord();

                    if (chordMirror != null) {
                        mirrors.add(chordMirror);
                        mirrors.addAll(chordMirror.getNotes());
                    }
                }
            } else if (left instanceof AbstractChordInter) {
                HeadChordInter chordMirror = (HeadChordInter) left.getMirror();

                if (chordMirror != null) {
                    mirrors.add(chordMirror);
                    mirrors.addAll(chordMirror.getNotes());
                }
            }

            final double xMax = leftBox.getMaxX();

            for (Inter right : inters.subList(i + 1, inters.size())) {
                if (right.isRemoved() || right.isImplicit()) {
                    continue;
                }

                // Mirror entities do not exclude one another
                if (mirrors.contains(right)) {
                    continue;
                }

                // Overlap is accepted in some cases
                if (compatible(left, right) || compatible(right, left)) {
                    continue;
                }

                Rectangle rightBox = right.getBounds();

                if (leftBox.intersects(rightBox)) {
                    // Have a more precise look
                    if (left.isVip() && right.isVip()) {
                        logger.info("VIP check overlap {} vs {}", left, right);
                    }

                    if (left.overlaps(right) && right.overlaps(left)) {
                        // Specific case: Word vs "string" Symbol
                        if (left instanceof WordInter && right instanceof StringSymbolInter) {
                            if (wordMatchesSymbol((WordInter) left, (StringSymbolInter) right)) {
                                left.decrease(0.5);
                            }
                        } else if (left instanceof StringSymbolInter
                                && right instanceof WordInter) {
                            if (wordMatchesSymbol((WordInter) right, (StringSymbolInter) left)) {
                                right.decrease(0.5);
                            }
                        }

                        excludeOverlap(left, right);
                    }
                } else if (rightBox.x > xMax) {
                    break; // Since inters list is sorted by abscissa
                }
            }
        }
    }

    //---------//
    // exclude //
    //---------//
    /**
     * Insert exclusion between (the members of) the 2 sets.
     *
     * @param set1  one set
     * @param set2  the other set
     * @param cause reason for exclusion
     */
    private void exclude (Set<Inter> set1,
                          Set<Inter> set2,
                          Exclusion.ExclusionCause cause)
    {
        for (Inter i1 : set1) {
            for (Inter i2 : set2) {
                sig.insertExclusion(i1, i2, cause);
            }
        }
    }

    //---------//
    // exclude //
    //---------//
    private void excludeOverlap (Inter left,
                                 Inter right)
    {
        // Special overlap case between a stem and a standard-size note head
        if ((left instanceof StemInter && right instanceof HeadInter && !right.getShape()
                .isSmallHead()) || (right instanceof StemInter && left instanceof HeadInter && !left
                        .getShape().isSmallHead())) {
            return;
        }

        // If there is no support between left & right, insert an exclusion
        final SIGraph leftSig = left.getSig();

        if (leftSig.noSupport(left, right)) {
            leftSig.insertExclusion(left, right, OVERLAP);
        }
    }

    //------------------//
    // getHeadersInters //
    //------------------//
    /**
     * Collect inters that belong to staff headers in this system.
     *
     * @return the headers inters
     */
    private List<Inter> getHeadersInters ()
    {
        List<Inter> inters = new ArrayList<>();

        for (Staff staff : system.getStaves()) {
            if (staff.isTablature()) {
                continue;
            }

            StaffHeader header = staff.getHeader();

            if (header.clef != null) {
                inters.add(header.clef);
            }

            if (header.key != null) {
                inters.add(header.key);
                inters.addAll(header.key.getMembers());
            }

            if (header.time != null) {
                inters.add(header.time);

                if (header.time instanceof InterEnsemble interEnsemble) {
                    inters.addAll(interEnsemble.getMembers());
                }
            }
        }

        logger.trace("S#{} headers inters: {}", system.getId(), inters);

        return inters;
    }

    //-------------//
    // headHasStem //
    //-------------//
    /**
     * Check the (stem) head has a link to a stem
     *
     * @param head the head to check
     * @return true if OK
     */
    private boolean headHasStem (Inter head)
    {
        if (head.isVip()) {
            logger.info("VIP checkHeadHasStem for {}", head);
        }

        // Check if the head has a stem relation
        if (!sig.hasRelation(head, HeadStemRelation.class)) {
            if (head.isVip()) {
                logger.info("VIP no stem for {}", head);
            }

            return false;
        }

        return true;
    }

    //------------------//
    // intersectedHeads //
    //------------------//
    /**
     * Report the heads that intersect the provided stem.
     *
     * @param stem     provided stem
     * @param allHeads all heads in sig
     * @return the intersected heads
     */
    private List<Inter> intersectedHeads (StemInter stem,
                                          List<Inter> allHeads)
    {
        final Line2D median = stem.getMedian();
        final List<Inter> found = new ArrayList<>();

        for (Inter ih : allHeads) {
            if (median.intersects(ih.getBounds())) {
                final double iou = GeoUtil.iou(stem.getBounds(), ih.getBounds());

                if (iou >= constants.minIouStemHead.getSourceValue()) {
                    found.add(ih);
                }
            }
        }

        return found;
    }

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
        Rectangle ledgerBox = ledger.getBounds();
        ledgerBox.grow(0, scale.getInterline()); // Very high box, but that's OK

        // Check for another ledger on next line
        final int nextIndex = index + Integer.signum(index);
        final List<LedgerInter> nextLedgers = staff.getLedgers(nextIndex);

        if (nextLedgers != null) {
            for (LedgerInter nextLedger : nextLedgers) {
                // Check abscissa compatibility
                if (GeoUtil.xOverlap(ledgerBox, nextLedger.getBounds()) > 0) {
                    return true;
                }
            }
        }

        // Else, check for a note head centered on ledger, or just on next pitch
        final int ledgerPitch = Staff.getLedgerPitchPosition(index);
        final int nextPitch = ledgerPitch + Integer.signum(index);
        final List<Inter> heads = Inters.intersectedInters(
                allHeads,
                GeoOrder.BY_ABSCISSA,
                ledgerBox);

        for (Inter inter : heads) {
            final HeadInter head = (HeadInter) inter;
            final int notePitch = head.getIntegerPitch();

            if ((notePitch == ledgerPitch) || (notePitch == nextPitch)) {
                return true;
            }
        }

        return false;
    }

    //------------//
    // lookupHead //
    //------------//
    /**
     * Lookup a head connected to provided stem, with proper head side and pitch values.
     * Beware side is defined WRT head, not WRT stem.
     * <p>
     * For merged grand staff configuration, search that extend past the middle ledger is handled
     * in the context of the other staff and with a different pitch value.
     *
     * @param stem  the provided stem
     * @param side  desired head side
     * @param pitch desired pitch position
     * @param staff staff the pitch is related to
     * @return the head instance if found, null otherwise
     */
    private Inter lookupHead (StemInter stem,
                              HorizontalSide side,
                              int pitch,
                              Staff staff)
    {
        if (stem.isRemoved()) {
            return null;
        }

        // Very specific handling for merged grand staff
        final Part staffPart = staff.getPart();

        if (staffPart.isMerged()) {
            if ((staff == staffPart.getFirstStaff()) && (pitch == 7)) {
                staff = staffPart.getLastStaff();
                pitch = -5;
            } else if ((staff == staffPart.getLastStaff()) && (pitch == -6)) {
                staff = staffPart.getFirstStaff();
                pitch = 5;
            }
        }

        for (Relation rel : sig.getRelations(stem, HeadStemRelation.class)) {
            HeadStemRelation hsRel = (HeadStemRelation) rel;

            // Check side
            if (hsRel.getHeadSide() == side) {
                // Check pitch in proper staff
                HeadInter head = (HeadInter) sig.getEdgeSource(rel);

                if ((head.getStaff() == staff) && (head.getIntegerPitch() == pitch)) {
                    return head;
                }
            }
        }

        return null;
    }

    //----------------//
    // pruneStemHeads //
    //----------------//
    /**
     * Cut links between the provided stem and its ending head(s) on wrong side.
     *
     * @param stem the stem to process
     * @return true if one or several heads were pruned from the stem
     */
    private boolean pruneStemHeads (StemInter stem)
    {
        if (stem.isVip()) {
            logger.info("VIP pruneStemHeads for {}", stem);
        }

        final Set<Relation> links = sig.getRelations(stem, HeadStemRelation.class);

        int modifs = 0;
        boolean modified;
        StemGeometryScan:
        do {
            modified = false;

            final Line2D stemLine = stem.computeExtendedLine(); // Update geometry

            for (Relation rel : links) {
                // Retrieve the stem portion for this Head -> Stem link
                final HeadInter head = (HeadInter) sig.getEdgeSource(rel);

                // This policy does not apply to percussion heads
                if (head.getShape().isPercussion()) {
                    continue;
                }

                final HeadStemRelation link = (HeadStemRelation) rel;
                final StemPortion portion = link.getStemPortion(head, stemLine, scale);
                final HorizontalSide headSide = link.getHeadSide();

                if (((portion == STEM_BOTTOM) && (headSide != RIGHT)) || ((portion == STEM_TOP)
                        && (headSide != LEFT))) {
                    if (stem.isVip() || head.isVip()) {
                        logger.info("VIP pruned {} from {}", head, stem);
                    }

                    sig.removeEdge(rel);
                    links.remove(rel);
                    modifs++;
                    modified = true;

                    if (link.isInvading()) {
                        if (stem.isVip() || head.isVip()) {
                            logger.info("VIP invasion between {} & {}", head, stem);
                        }

                        sig.insertExclusion(head, stem, Exclusion.ExclusionCause.OVERLAP);
                    }

                    continue StemGeometryScan;
                }
            }
        } while (modified);

        return modifs > 0;
    }

    //--------//
    // reduce //
    //--------//
    /**
     * Reduce all the interpretations and relations of the SIG.
     *
     * @return the collection of removed inters
     */
    private Set<Inter> reduce (ReductionAdapter adapter)
    {
        final Set<Inter> allRemoved = new LinkedHashSet<>();

        logger.debug("S#{} reducing sig ...", system.getId());

        // General exclusions based on overlap
        List<Inter> inters = sig.inters(overlapPredicate);
        inters.removeAll(getHeadersInters());
        detectOverlaps(inters, adapter);

        // Inters that conflict with frozen inters must be deleted
        adapter.checkFrozens();

        // Make sure all inters have their contextual grade up-to-date
        sig.contextualize();

        adapter.prolog();

        Set<Inter> reduced = new LinkedHashSet<>(); // Reduced inters
        Set<Inter> deleted = new LinkedHashSet<>(); // Deleted inters

        int epoch = 0;

        do {
            logger.debug("S#{} epoch: {}", system.getId(), ++epoch);

            reduced.clear();
            deleted.clear();

            // First, remove all inters with too low contextual grade
            deleted.addAll(contextualizeAndPurge());
            allRemoved.addAll(deleted);

            deleted.addAll(adapter.checkSlurs());
            allRemoved.addAll(deleted);

            int modifs; // modifications done in current iteration

            while ((modifs = adapter.checkConsistencies()) > 0) {
                logger.trace("S#{} modifs: {}", system.getId(), modifs);
            }

            // Remaining exclusions
            reduced.addAll(sig.reduceExclusions());
            allRemoved.addAll(reduced);

            while ((modifs = adapter.checkLateConsistencies()) > 0) {
                logger.trace("S#{} late modifs: {}", system.getId(), modifs);
            }

            logger.trace("S#{} reductions: {}", system.getId(), reduced);
        } while (!reduced.isEmpty() || !deleted.isEmpty());

        return allRemoved;
    }

    //---------------------//
    // reduceAugmentations //
    //---------------------//
    /**
     * Reduce the number of augmentation relations to one, by keeping the best relation.
     *
     * @param rels the augmentation links for the same entity
     * @return the number of relation deleted
     */
    private int reduceAugmentations (Set<Relation> rels)
    {
        int modifs = 0;

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

    //-------------------//
    // reduceFoundations //
    //-------------------//
    /**
     * Reduce all the interpretations and relations of the SIG, on the founding inters
     * (Heads, Stems and Beams).
     * This is meant for the REDUCTION step.
     */
    public void reduceFoundations ()
    {
        reduce(new AdapterForFoundations());
    }

    //-------------------------//
    // reduceHeadAugmentations //
    //-------------------------//
    /**
     * Reduce the number of augmentation relations to one, by keeping the augmentation
     * dot located higher in sheet.
     *
     * @param head the related head
     * @param rels the augmentation links for the same head
     * @return the number of relation deleted
     */
    private int reduceHeadAugmentations (HeadInter head,
                                         Set<Relation> rels)
    {
        if (head.isVip()) {
            logger.info(" VIP S#{} reduceHeadAugmentations for {}", system.getId(), head);
        }

        // Sort related dots by ordinate
        final List<Inter> dots = new ArrayList<>();

        for (Relation rel : rels) {
            dots.add(sig.getEdgeSource(rel));
        }

        Collections.sort(dots, Inters.byCenterOrdinate);

        // Select the "best" dot
        AugmentationDotInter selected = null;

        {
            // Try dot above
            final AugmentationDotInter dotAbove = (AugmentationDotInter) dots.get(0);
            final List<AbstractNoteInter> notes = dotAbove.getAugmentedNotes();
            boolean taken = false;

            for (AbstractNoteInter note : notes) {
                if (note instanceof HeadInter && ((note.getIntegerPitch() % 2) != 0)) {
                    taken = true;

                    break;
                }
            }

            if (!taken) {
                selected = dotAbove;
            }
        }

        if (selected == null) {
            // Try dot below
            final AugmentationDotInter dotBelow = (AugmentationDotInter) dots.get(dots.size() - 1);
            final List<AbstractNoteInter> notes = dotBelow.getAugmentedNotes();
            boolean taken = false;

            for (AbstractNoteInter note : notes) {
                if (note instanceof HeadInter && ((note.getIntegerPitch() % 2) != 0)) {
                    taken = true;

                    break;
                }
            }

            if (!taken) {
                selected = dotBelow;
            }
        }

        int modifs = 0;

        for (Relation rel : rels) {
            if (sig.getEdgeSource(rel) != selected) {
                sig.removeEdge(rel);
                modifs++;
            }
        }

        return modifs;
    }

    //-------------//
    // reduceLinks //
    //-------------//
    /**
     * Final global reduction.
     * This is meant for the LINKS step.
     */
    public void reduceLinks ()
    {
        reduce(new AdapterForLinks());
    }

    //
    //    //------------------//
    //    // stemHasHeadAtEnd //
    //    //------------------//
    //    /**
    //     * Check if the stem has at least a head at some end.
    //     *
    //     * @param stem the stem inter
    //     * @return true if OK
    //     */
    //    private boolean stemHasHeadAtEnd (StemInter stem)
    //    {
    //        if (stem.isVip()) {
    //            logger.info("VIP stemHasHeadAtEnd for {}", stem);
    //        }
    //
    //        final Line2D stemLine = stem.computeExtendedLine();
    //
    //        for (Relation rel : sig.getRelations(stem, HeadStemRelation.class)) {
    //            // Check stem portion
    //            HeadStemRelation hsRel = (HeadStemRelation) rel;
    //            Inter head = sig.getOppositeInter(stem, rel);
    //
    //            if (hsRel.getStemPortion(head, stemLine, scale) != STEM_MIDDLE) {
    //                return true;
    //            }
    //        }
    //
    //        return false;
    //    }
    //
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
        final Line2D stemLine = stem.computeExtendedLine();
        final int stemDir = stem.computeDirection();

        if (stemDir == 0) {
            return true; // We cannot decide
        }

        final StemPortion forbidden = (stemDir > 0) ? STEM_BOTTOM : STEM_TOP;
        final List<Relation> toRemove = new ArrayList<>();
        final List<Inter> toExclude = new ArrayList<>();

        for (Relation rel : sig.getRelations(stem, HeadStemRelation.class)) {
            HeadStemRelation hsRel = (HeadStemRelation) rel;

            // Check stem portion
            Inter head = sig.getEdgeSource(hsRel);
            StemPortion portion = hsRel.getStemPortion(head, stemLine, scale);

            if (portion == forbidden) {
                if (stem.isVip()) {
                    logger.info("VIP cutting relation between {} and {}", stem, head);
                }

                toRemove.add(hsRel);

                if (hsRel.isInvading()) {
                    if (stem.isVip() || head.isVip()) {
                        logger.info("VIP invasion between {} & {}", head, stem);
                    }

                    toExclude.add(head);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            sig.removeAllEdges(toRemove);

            for (Inter head : toExclude) {
                sig.insertExclusion(head, stem, Exclusion.ExclusionCause.OVERLAP);
            }
        }

        return toRemove.isEmpty();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------//
    // compatible //
    //------------//
    /**
     * Check whether the two provided Inter instance can overlap.
     *
     * @param left  an inter
     * @param right another inter
     * @return true if overlap is accepted, false otherwise
     */
    private static boolean compatible (Inter left,
                                       Inter right)
    {
        final Class<?> oneClass = left.getClass();

        if (right instanceof AbstractBeamInter) {
            for (Class<?> classe : beamCompClasses) {
                if (classe.isAssignableFrom(oneClass)) {
                    return true;
                }
            }
        } else if (right instanceof SlurInter) {
            for (Class<?> classe : slurCompClasses) {
                if (classe.isAssignableFrom(oneClass)) {
                    return true;
                }
            }
        } else if (right instanceof StemInter) {
            for (Class<?> classe : stemCompClasses) {
                if (classe.isAssignableFrom(oneClass)) {
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
            logger.trace("Match found");

            //TODO: Perhaps more checks on word/sentence?
            return true;
        }

        return false;
    }

    //-----------------------//
    // AdapterForFoundations //
    //-----------------------//
    private class AdapterForFoundations
            extends ReductionAdapter
    {

        @Override
        public int checkConsistencies ()
        {
            logger.debug("S#{} checkConsistencies", system.getId());

            int modifs = 0;

            modifs += checkStemEndingHeads();
            deleted.addAll(contextualizeAndPurge());

            modifs += checkHeads();
            deleted.addAll(contextualizeAndPurge());

            modifs += checkHooksHaveStem();
            deleted.addAll(contextualizeAndPurge());

            modifs += checkBeamsHaveBothStems();
            deleted.addAll(contextualizeAndPurge());

            modifs += checkLedgers();
            deleted.addAll(contextualizeAndPurge());

            modifs += checkStems();
            deleted.addAll(contextualizeAndPurge());

            return modifs;
        }

        @Override
        public int checkLateConsistencies ()
        {
            logger.debug("S#{} checkLateConsistencies", system.getId());

            int modifs = 0;

            analyzeChords(); // Heads size compatibility & beams size compatibility
            reduced.addAll(sig.reduceExclusions());
            deleted.addAll(contextualizeAndPurge());

            //
            //            modifs += checkStemsLengths();
            //            deleted.addAll(contextualizeAndPurge());
            //
            return modifs;
        }

        @Override
        public void prolog ()
        {
            logger.debug("S#{} prolog", system.getId());

            ///analyzeHeadStems(); // Check there is at most one stem on each side of any head
            analyzeChords(); // Heads & beams compatibility
        }
    }

    //-----------------//
    // AdapterForLinks //
    //-----------------//
    private class AdapterForLinks
            extends ReductionAdapter
    {

        @Override
        public int checkConsistencies ()
        {
            int modifs = 0;

            modifs += checkRomans();
            deleted.addAll(contextualizeAndPurge());

            modifs += checkStemEndingHeads();
            deleted.addAll(contextualizeAndPurge());

            modifs += checkHeads();
            deleted.addAll(contextualizeAndPurge());

            modifs += checkDoubleAlters();
            deleted.addAll(contextualizeAndPurge());

            modifs += checkTimeNumbers();
            checkTimeSignatures();
            deleted.addAll(contextualizeAndPurge());

            modifs += checkAugmentationDots();
            modifs += checkAugmentedHeads();
            modifs += checkAugmentedRests();
            modifs += checkAugmentedDots();
            deleted.addAll(contextualizeAndPurge());

            modifs += checkIsolatedAlters();
            modifs += checkMeasureCounts();
            deleted.addAll(contextualizeAndPurge());

            return modifs;
        }

        @Override
        public void checkFrozens ()
        {
            analyzeFrozenInters();
        }

        @Override
        public Set<Inter> checkSlurs ()
        {
            return checkSlurOnTuplet();
        }

        @Override
        public void prolog ()
        {
            // Still needed because of cue beams
            analyzeChords();
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction maxTupletSlurWidth = new Scale.Fraction(
                3,
                "Maximum width for slur around tuplet");

        private final Constant.Ratio minIouStemHead = new Constant.Ratio(
                0.02,
                "Minimum IOU between stem and intersected heads");
    }

    //------------------//
    // ReductionAdapter //
    //------------------//
    private abstract static class ReductionAdapter
    {

        Set<Inter> deleted = new LinkedHashSet<>();

        Set<Inter> reduced = new LinkedHashSet<>();

        public int checkConsistencies ()
        {
            return 0; // Void by default
        }

        public void checkFrozens ()
        {
            // Void by default
        }

        public int checkLateConsistencies ()
        {
            return 0; // Void by default
        }

        public Set<Inter> checkSlurs ()
        {
            return Collections.emptySet();
        }

        public void prolog ()
        {
            // Void by default
        }
    }

    //~ Enumerations -------------------------------------------------------------------------------

    /** Standard vs Small size. */
    private static enum Size
    {
        STANDARD,
        SMALL;
    }
}
