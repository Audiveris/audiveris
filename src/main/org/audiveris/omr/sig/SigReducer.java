//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S i g R e d u c e r                                      //
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
package org.audiveris.omr.sig;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import static org.audiveris.omr.glyph.ShapeSet.Accidentals;
import static org.audiveris.omr.glyph.ShapeSet.CoreBarlines;
import static org.audiveris.omr.glyph.ShapeSet.Flags;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.header.StaffHeader;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.SystemBackup;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.DeletedInterException;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.KeyAlterInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallBeamInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.StringSymbolInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.AbstractConnection;
import org.audiveris.omr.sig.relation.AlterHeadRelation;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.BeamHeadRelation;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.Exclusion;
import org.audiveris.omr.sig.relation.HeadHeadRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.StemPortion;
import static org.audiveris.omr.sig.relation.StemPortion.*;
import org.audiveris.omr.sig.relation.TimeTopBottomRelation;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

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
    private static final EnumSet<Shape> disabledShapes = EnumSet.copyOf(
            Arrays.asList(Shape.LEDGER, Shape.CRESCENDO, Shape.DIMINUENDO));

    /** Predicate for non-disabled overlap. */
    private static final Predicate<Inter> overlapPredicate = new Predicate<Inter>()
    {
        @Override
        public boolean check (Inter inter)
        {
            // Take all non-disabled shapes
            // Excluding inters within headers
            return !disabledShapes.contains(inter.getShape());
        }
    };

    /** Shapes that can overlap with a beam. */
    private static final EnumSet<Shape> beamCompShapes = EnumSet.copyOf(CoreBarlines);

    /** Shapes that can overlap with a slur. */
    private static final EnumSet<Shape> slurCompShapes = EnumSet.noneOf(Shape.class);

    static {
        slurCompShapes.addAll(Accidentals.getShapes());
        slurCompShapes.addAll(CoreBarlines);
        slurCompShapes.addAll(Flags.getShapes());
    }

    /** Shapes that can overlap with a stem. */
    private static final EnumSet<Shape> stemCompShapes = EnumSet.copyOf(
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

    /** Should we purge weak inter instances?. */
    private final boolean purgeWeaks;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SigReducer} object.
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

    //---------------//
    // reduceRhythms //
    //---------------//
    /**
     * NOT USED!
     * Reduce interpretations while saving reduced rhythm data.
     * This is meant for the RHYTHMS step.
     *
     * @param systemPoorFrats (output) where selected inters must be backed up
     * @param classes         FRAT classes
     */
    @Deprecated
    public void reduceRhythms (SystemBackup systemPoorFrats,
                               Class<?>... classes)
    {
        AdapterForRhythms adapter = new AdapterForRhythms(systemPoorFrats, classes);
        Set<Inter> allRemoved = reduce(adapter);
        allRemoved.retainAll(adapter.selected);
        systemPoorFrats.setSeeds(allRemoved);
    }

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
     * </ul>
     */
    private void analyzeChords ()
    {
        // All stems of the sig
        List<Inter> stems = sig.inters(Shape.STEM);

        // Heads organized by shape (black, void, and small versions)
        Map<Shape, Set<Inter>> heads = new HashMap<Shape, Set<Inter>>();

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
                    Shape shape = head.getShape();
                    Set<Inter> set = heads.get(shape);

                    if (set == null) {
                        heads.put(shape, set = new LinkedHashSet<Inter>());
                    }

                    set.add(head);
                } else if (rel instanceof BeamStemRelation) {
                    Inter beam = sig.getEdgeSource(rel);
                    Size size = (beam instanceof SmallBeamInter) ? Size.SMALL : Size.STANDARD;
                    Set<Inter> set = beams.get(size);

                    if (set == null) {
                        beams.put(size, set = new LinkedHashSet<Inter>());
                    }

                    set.add(beam);
                }
            }

            // Mutual head exclusion based on head shape
            // But perhaps the joining stem is the problem, so check the two conflicting heads
            // are not linked to any other stem than the one at hand.
            List<Shape> headShapes = new ArrayList<Shape>(heads.keySet());

            for (int ic = 0; ic < (headShapes.size() - 1); ic++) {
                Shape c1 = headShapes.get(ic);
                Set<Inter> set1 = heads.get(c1);

                for (Inter h1 : set1) {
                    HeadInter head1 = (HeadInter) h1;

                    if (head1.getStems().size() == 1) {
                        for (Shape c2 : headShapes.subList(ic + 1, headShapes.size())) {
                            Set<Inter> set2 = heads.get(c2);

                            for (Inter h2 : set2) {
                                HeadInter head2 = (HeadInter) h2;

                                if (head2.getStems().size() == 1) {
                                    sig.insertExclusion(h1, h2, Exclusion.Cause.INCOMPATIBLE);
                                }
                            }
                        }
                    }
                }
            }

            // Mutual head support within same shape
            for (Shape shape : heads.keySet()) {
                List<Inter> list = new ArrayList<Inter>(heads.get(shape));

                for (int i = 0; i < list.size(); i++) {
                    HeadInter h1 = (HeadInter) list.get(i);

                    for (Inter other : list.subList(i + 1, list.size())) {
                        HeadInter h2 = (HeadInter) other;
                        sig.insertSupport(h1, h2, HeadHeadRelation.class);
                    }
                }
            }

            // Mutual beam exclusion based on beam size
            List<Size> beamSizes = new ArrayList<Size>(beams.keySet());

            for (int ic = 0; ic < (beamSizes.size() - 1); ic++) {
                Size c1 = beamSizes.get(ic);
                Set<Inter> set1 = beams.get(c1);

                for (Size c2 : beamSizes.subList(ic + 1, beamSizes.size())) {
                    Set<Inter> set2 = beams.get(c2);
                    exclude(set1, set2);
                }
            }

            // Head/Beam support or exclusion based on size
            for (Entry<Size, Set<Inter>> entry : beams.entrySet()) {
                Size size = entry.getKey();
                Set<Inter> beamSet = entry.getValue();

                if (size == Size.SMALL) {
                    // Small beams exclude standard heads
                    for (Shape shape : new Shape[]{Shape.NOTEHEAD_BLACK, Shape.NOTEHEAD_VOID}) {
                        Set<Inter> headSet = heads.get(shape);

                        if (headSet != null) {
                            exclude(beamSet, headSet);
                        }
                    }

                    // Small beams support small black heads
                    Set<Inter> smallHeadSet = heads.get(Shape.NOTEHEAD_BLACK_SMALL);

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
                    Set<Inter> smallHeadSet = heads.get(Shape.NOTEHEAD_BLACK_SMALL);

                    if (smallHeadSet != null) {
                        exclude(beamSet, smallHeadSet);
                    }

                    // Standard beams support black heads (not void)
                    Set<Inter> blackHeadSet = heads.get(Shape.NOTEHEAD_BLACK);

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
        Set<Inter> toDelete = new LinkedHashSet<Inter>();

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
    // analyzeHeadStems //
    //------------------//
    /**
     * Check any head has at most one stem on left side and one stem on right side.
     * If not, the various stems on a same side are mutually exclusive.
     */
    private void analyzeHeadStems ()
    {
        final List<Inter> heads = sig.inters(ShapeSet.StemHeads);

        for (Inter hi : heads) {
            HeadInter head = (HeadInter) hi;

            // Retrieve connected stems into left and right sides
            Map<HorizontalSide, Set<StemInter>> map = head.getSideStems();

            // Check each side
            for (Entry<HorizontalSide, Set<StemInter>> entry : map.entrySet()) {
                Set<StemInter> set = entry.getValue();

                if (set.size() > 1) {
                    //////////////////////////////////////////////////HB sig.insertExclusions(set, Exclusion.Cause.OVERLAP);
                    //TODO:
                    // Instead of stem exclusion, we should disconnect head from some of these stems
                    // Either all the stems above or all the stems below
                }
            }
        }
    }

    //------------------//
    // beamHasBothStems //
    //------------------//
    private boolean beamHasBothStems (BeamInter beam)
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

            if (dot.isVip() || logger.isDebugEnabled()) {
                logger.info("Deleting {} lacking target", dot);
            }

            dot.delete();
            modifs++;
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
        final List<Inter> beams = sig.inters(BeamInter.class);

        for (Inter inter : beams) {
            final BeamInter beam = (BeamInter) inter;

            if (!beamHasBothStems(beam)) {
                if (beam.isVip() || logger.isDebugEnabled()) {
                    logger.info("VIP deleting beam lacking stem {}", beam);
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
            if (!sig.hasRelation(alter, AlterHeadRelation.class)) {
                if (alter.isVip() || logger.isDebugEnabled()) {
                    logger.info("VIP deleting {} lacking note head", alter);
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
            int dir = stem.computeDirection();

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
            int pitch = ((HeadInter) head).getIntegerPitch();

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
                logger.info("VIP wrong side for {} on {}", head, stem);
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
        final List<Inter> heads = sig.inters(ShapeSet.StemHeads);

        for (Inter head : heads) {
            if (head.isVip()) {
                logger.info("VIP checkheads for {}", head);
            }

            // Check if the head has a stem relation
            if (!sig.hasRelation(head, HeadStemRelation.class)) {
                if (head.isVip() || logger.isDebugEnabled()) {
                    logger.info("VIP no stem for {}", head);
                }

                head.delete();
                modifs++;

                continue;
            }

            // Check head location relative to stem
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
                    logger.info("VIP no stem for {}", inter);
                }

                inter.delete();
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

            if ((measure != null)
                && (measure == part.getLastMeasure())
                && (measure.getBarline(HorizontalSide.RIGHT) == null)) {
                // Empty measure?
                // Measure width?
                continue;
            }

            if (alter.isVip() || logger.isDebugEnabled()) {
                logger.info("VIP deleting isolated {}", alter);
            }

            alter.delete();
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
        // All system note heads, sorted by abscissa
        List<Inter> allHeads = sig.inters(ShapeSet.Heads);
        Collections.sort(allHeads, Inter.byAbscissa);

        final List<LedgerInter> toDelete = new ArrayList<LedgerInter>();
        boolean modified;

        do {
            modified = false;

            for (Staff staff : system.getStaves()) {
                SortedMap<Integer, List<LedgerInter>> map = staff.getLedgerMap();

                // Need a set copy to avoid concurrent modifications
                Set<Entry<Integer, List<LedgerInter>>> setCopy;
                setCopy = new LinkedHashSet<Entry<Integer, List<LedgerInter>>>(map.entrySet());

                for (Entry<Integer, List<LedgerInter>> entry : setCopy) {
                    int index = entry.getKey();

                    // Need a list copy to avoid concurrent modifications
                    List<LedgerInter> ledgersCopy = new ArrayList<LedgerInter>(entry.getValue());

                    for (LedgerInter ledger : ledgersCopy) {
                        if (!ledgerHasHeadOrLedger(staff, index, ledger, allHeads)) {
                            if (ledger.isVip() || logger.isDebugEnabled()) {
                                logger.info("VIP deleting orphan ledger {}", ledger);
                            }

                            ledger.delete();
                            modified = true;
                        }
                    }
                }
            }
        } while (modified);

        if (!toDelete.isEmpty()) {
            for (LedgerInter ledger : toDelete) {
                ledger.delete(); // This updates the ledgerMap in relevant staves
            }
        }

        return toDelete.size();
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
        Set<Inter> deleted = new LinkedHashSet<Inter>();
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

        for (Inter slurInter : slurs) {
            final SlurInter slur = (SlurInter) slurInter;

            // Look for a tuplet sign embraced
            final int above = slur.isAbove() ? 1 : (-1);
            Rectangle box = slur.getBounds();
            box.translate(0, above * box.height);

            for (Inter tuplet : tuplets) {
                if (box.intersects(tuplet.getBounds())) {
                    if (slur.isVip() || logger.isDebugEnabled()) {
                        logger.info("VIP deleting tuplet-slur {}", slur);
                    }

                    slur.delete();
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

    //------------------//
    // checkStemLengths //
    //------------------//
    /**
     * Perform checks on stem length from tail to closest head anchor.
     *
     * @return the count of modifications done
     */
    private int checkStemLengths ()
    {
        final int minStemExtension = scale.toPixels(constants.minStemExtension);
        final List<Inter> stems = sig.inters(Shape.STEM);
        int modifs = 0;

        for (Inter inter : stems) {
            final StemInter stem = (StemInter) inter;
            final Rectangle stemBox = stem.getBounds();
            Rectangle headsBox = null;

            for (Relation rel : sig.getRelations(stem, HeadStemRelation.class)) {
                final HeadInter head = (HeadInter) sig.getOppositeInter(stem, rel);
                final Rectangle headBox = head.getBounds();

                if (headsBox == null) {
                    headsBox = headBox;
                } else {
                    headsBox.add(headBox);
                }
            }

            if (headsBox == null) {
                stem.delete();
                modifs++;
            } else {
                final int above = headsBox.y - stemBox.y;
                final int below = (stemBox.y + stemBox.height) - (headsBox.y + headsBox.height);
                final int extension = Math.max(above, below);

                if (extension < minStemExtension) {
                    stem.delete();
                    modifs++;
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
                    logger.info("VIP deleting stem lacking starting head {}", stem);
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
            if (!sig.hasRelation(number, TimeTopBottomRelation.class)) {
                if (number.isVip() || logger.isDebugEnabled()) {
                    logger.info("VIP deleting time number lacking sibling {}", number);
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
     * Check there is no note between measure start and time signature, otherwise insert exclusion.
     */
    private void checkTimeSignatures ()
    {
        List<Inter> systemNotes = sig.inters(AbstractNoteInter.class);

        if (systemNotes.isEmpty()) {
            return;
        }

        final List<Inter> systemTimes = sig.inters(AbstractTimeInter.class);
        Collections.sort(systemNotes, Inter.byAbscissa);

        for (Staff staff : system.getStaves()) {
            List<Inter> staffTimes = SIGraph.inters(staff, systemTimes);

            if (staffTimes.isEmpty()) {
                continue;
            }

            List<Inter> notes = SIGraph.inters(staff, systemNotes);

            for (Inter inter : staffTimes) {
                AbstractTimeInter timeSig = (AbstractTimeInter) inter;

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
                            logger.info("VIP {} preceding {}", note, timeSig);
                        }

                        sig.insertExclusion(note, timeSig, Exclusion.Cause.INCOMPATIBLE);
                    }
                }
            }
        }
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
                                 Adapter adapter)
    {
        Collections.sort(inters, Inter.byAbscissa);

        NextLeft:
        for (int i = 0, iBreak = inters.size() - 1; i < iBreak; i++) {
            Inter left = inters.get(i);

            if (left.isDeleted()) {
                continue;
            }

            final Rectangle leftBox = left.getBounds();
            Set<Inter> mirrors = null;

            final Inter leftMirror = left.getMirror();

            if (leftMirror != null) {
                mirrors = new LinkedHashSet<Inter>();
                mirrors.add(leftMirror);

                final AbstractChordInter leftChord = (AbstractChordInter) left.getEnsemble();

                if (leftChord != null) {
                    Inter leftChordMirror = leftChord.getMirror();

                    if (leftChordMirror != null) {
                        mirrors.add(leftChordMirror);
                        mirrors.addAll(((AbstractChordInter) leftChordMirror).getNotes());
                    }
                } else if (leftMirror instanceof AbstractChordInter) {
                    mirrors.addAll(((AbstractChordInter) leftMirror).getNotes());
                }
            }

            final double xMax = leftBox.getMaxX();

            for (Inter right : inters.subList(i + 1, inters.size())) {
                if (right.isDeleted()) {
                    continue;
                }

                // Mirror entities do not exclude one another
                if ((mirrors != null) && mirrors.contains(right)) {
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
                        logger.info("VIP check overlap {} vs {}", left, right);
                    }

                    try {
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

                            exclude(left, right);
                        }
                    } catch (DeletedInterException diex) {
                        if (diex.inter == left) {
                            continue NextLeft;
                        }
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

    //---------//
    // exclude //
    //---------//
    private void exclude (Inter left,
                          Inter right)
    {
        // Special overlap case between a stem and a standard-size note head
        if ((left instanceof StemInter && right instanceof HeadInter
             && !right.getShape().isSmall())
            || (right instanceof StemInter && left instanceof HeadInter && !left.getShape().isSmall())) {
            return;
        }

        // If there is no support between left & right, insert an exclusion
        final SIGraph leftSig = left.getSig();

        if (leftSig.noSupport(left, right)) {
            leftSig.insertExclusion(left, right, Exclusion.Cause.OVERLAP);
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
        List<Inter> inters = new ArrayList<Inter>();

        for (Staff staff : system.getStaves()) {
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

                if (header.time instanceof InterEnsemble) {
                    inters.addAll(((InterEnsemble) header.time).getMembers());
                }
            }
        }

        logger.debug("S#{} headers inters: {}", system.getId(), inters);

        return inters;
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
        int nextIndex = index + Integer.signum(index);
        List<LedgerInter> nextLedgers = staff.getLedgers(nextIndex);

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
            final HeadInter head = (HeadInter) inter;
            final int notePitch = head.getIntegerPitch();

            if ((notePitch == ledgerPitch) || (notePitch == nextPitch)) {
                return true;
            }
        }

        return false;
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
            //            logger.info("VIP pruneStemHeads for {}", stem);
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
                HeadInter head = (HeadInter) sig.getEdgeSource(rel);
                HeadStemRelation link = (HeadStemRelation) rel;
                StemPortion portion = link.getStemPortion(head, stemLine, scale);
                HorizontalSide headSide = link.getHeadSide();

                if (((portion == STEM_BOTTOM) && (headSide != RIGHT))
                    || ((portion == STEM_TOP) && (headSide != LEFT))) {
                    sig.removeEdge(rel);
                    links.remove(rel);
                    modifs++;
                    modified = true;

                    if (stem.isVip() || head.isVip() || logger.isDebugEnabled()) {
                        logger.info("VIP pruned {} from {}", head, stem);
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
    private Set<Inter> reduce (Adapter adapter)
    {
        final Set<Inter> allRemoved = new LinkedHashSet<Inter>();

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

        Set<Inter> reduced = new LinkedHashSet<Inter>(); // Reduced inters
        Set<Inter> deleted = new LinkedHashSet<Inter>(); // Deleted inters

        do {
            reduced.clear();
            deleted.clear();

            // First, remove all inters with too low contextual grade
            deleted.addAll(updateAndPurge());

            deleted.addAll(adapter.checkSlurs());
            allRemoved.addAll(deleted);

            int modifs; // modifications done in current iteration

            while ((modifs = adapter.checkConsistencies()) > 0) {
                logger.debug("S#{} modifs: {}", system.getId(), modifs);
            }

            // Remaining exclusions
            reduced.addAll(sig.reduceExclusions());
            allRemoved.addAll(reduced);

            while ((modifs = adapter.checkLateConsistencies()) > 0) {
                logger.debug("S#{} late modifs: {}", system.getId(), modifs);
            }

            logger.debug("S#{} reductions: {}", system.getId(), reduced);
        } while (!reduced.isEmpty() || !deleted.isEmpty());

        return allRemoved;
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
            //            logger.info("VIP stemHasHeadAtEnd for {}", stem);
        }

        final Line2D stemLine = stem.computeExtendedLine();

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
        final Line2D stemLine = stem.computeExtendedLine();
        final int stemDir = stem.computeDirection();

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
                    logger.info(
                            "VIP cutting relation between {} and {}",
                            stem,
                            sig.getEdgeSource(rel));
                }

                toRemove.add(rel);
            }
        }

        if (!toRemove.isEmpty()) {
            sig.removeAllEdges(toRemove);
        }

        return toRemove.isEmpty();
    }

    //----------------//
    // updateAndPurge //
    //----------------//
    /**
     * Update the contextual grade of each Inter in SIG, and remove the weak ones if so
     * desired.
     *
     * @return the set of inters removed
     */
    private Set<Inter> updateAndPurge ()
    {
        sig.contextualize();

        if (purgeWeaks) {
            return sig.deleteWeakInters();
        }

        return Collections.emptySet();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    private abstract static class Adapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        Set<Inter> deleted = new LinkedHashSet<Inter>();

        Set<Inter> reduced = new LinkedHashSet<Inter>();

        //~ Methods --------------------------------------------------------------------------------
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

    //-----------------------//
    // AdapterForFoundations //
    //-----------------------//
    private class AdapterForFoundations
            extends Adapter
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public int checkConsistencies ()
        {
            int modifs = 0;

            modifs += checkStemEndingHeads();
            deleted.addAll(updateAndPurge());

            modifs += checkHeads();
            deleted.addAll(updateAndPurge());

            modifs += checkHooks();
            deleted.addAll(updateAndPurge());

            modifs += checkBeams();
            deleted.addAll(updateAndPurge());

            modifs += checkLedgers();
            deleted.addAll(updateAndPurge());

            modifs += checkStems();
            deleted.addAll(updateAndPurge());

            return modifs;
        }

        @Override
        public int checkLateConsistencies ()
        {
            int modifs = 0;

            modifs += checkStemLengths();
            deleted.addAll(updateAndPurge());

            return modifs;
        }

        @Override
        public void prolog ()
        {
            analyzeHeadStems(); // Check there is at most one stem on each side of any head

            analyzeChords(); // Heads & beams compatibility
        }
    }

    //-----------------//
    // AdapterForLinks //
    //-----------------//
    private class AdapterForLinks
            extends Adapter
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public int checkConsistencies ()
        {
            int modifs = 0;

            modifs += checkStemEndingHeads();
            deleted.addAll(updateAndPurge());

            modifs += checkHeads();
            deleted.addAll(updateAndPurge());

            modifs += checkDoubleAlters();
            deleted.addAll(updateAndPurge());

            modifs += checkTimeNumbers();
            checkTimeSignatures();
            deleted.addAll(updateAndPurge());

            modifs += checkAugmentationDots();
            modifs += checkAugmented();
            deleted.addAll(updateAndPurge());

            modifs += checkIsolatedAlters();
            deleted.addAll(updateAndPurge());

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

    //-------------------//
    // AdapterForRhythms //
    //-------------------//
    /**
     * NOT USED.
     * Adapter meant for RHYTHMS step.
     *
     * @deprecated no longer used
     */
    @Deprecated
    private class AdapterForRhythms
            extends Adapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final SystemBackup systemPoorFrats;

        private final Class[] classes;

        public List<Inter> selected;

        //~ Constructors ---------------------------------------------------------------------------
        public AdapterForRhythms (SystemBackup systemPoorFrats,
                                  Class[] classes)
        {
            this.systemPoorFrats = systemPoorFrats;
            this.classes = classes;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int checkConsistencies ()
        {
            int modifs = 0;

            modifs += checkDoubleAlters();
            deleted.addAll(updateAndPurge());

            modifs += checkTimeNumbers();
            checkTimeSignatures();
            deleted.addAll(updateAndPurge());

            modifs += checkAugmentationDots();
            modifs += checkAugmented();
            deleted.addAll(updateAndPurge());

            return modifs;
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

            // All inters of selected classes (with all their relations)
            selected = sig.inters(classes);
            systemPoorFrats.save(selected);
        }
    }

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

        private final Scale.Fraction minStemExtension = new Scale.Fraction(
                1.5,
                "Minimum vertical extension of a stem beyond last head");
    }
}
