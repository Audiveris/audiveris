//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R e l a t i o n s                                       //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.ArpeggiatoInter;
import org.audiveris.omr.sig.inter.ArticulationInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.ChordNameInter;
import org.audiveris.omr.sig.inter.DynamicsInter;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.FermataArcInter;
import org.audiveris.omr.sig.inter.FermataDotInter;
import org.audiveris.omr.sig.inter.FermataInter;
import org.audiveris.omr.sig.inter.FingeringInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.GraceChordInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.MarkerInter;
import org.audiveris.omr.sig.inter.MeasureCountInter;
import org.audiveris.omr.sig.inter.MeasureRepeatInter;
import org.audiveris.omr.sig.inter.MultipleRestInter;
import org.audiveris.omr.sig.inter.OrnamentInter;
import org.audiveris.omr.sig.inter.PedalInter;
import org.audiveris.omr.sig.inter.PlayingInter;
import org.audiveris.omr.sig.inter.PluckingInter;
import org.audiveris.omr.sig.inter.RepeatDotInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallFlagInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TremoloInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.inter.WedgeInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Class <code>Relations</code> gathers utilities for Relation classes and instances.
 *
 * @author Hervé Bitteur
 */
public abstract class Relations
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Relations.class);

    private static final Map<Class<? extends Inter>, Set<Class<? extends Relation>>> src =
            new LinkedHashMap<>();

    private static final Map<Class<? extends Inter>, Set<Class<? extends Relation>>> tgt =
            new LinkedHashMap<>();

    static {
        buildMaps();
    }

    //~ Constructors -------------------------------------------------------------------------------

    /** Not meant to be instantiated. */
    private Relations ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-----------//
    // buildMaps //
    //-----------//
    /**
     * Build the maps of possible relation classes for a source inter class and for a
     * target inter class.
     * <p>
     * A few relations are used only for support during reduction, rather than symbolic relation.
     * They are thus excluded for lack of usefulness at UI level:
     * <ul>
     * <li>BarConnectionRelation
     * <li>BeamHeadRelation
     * <li>ClefKeyRelation
     * <li>HeadHeadRelation
     * <li>KeyAltersRelation
     * <li>NoExclusion
     * <li>StemAlignmentRelation
     * </ul>
     */
    private static void buildMaps ()
    {
        map(AbstractBeamInter.class, BeamStemRelation.class, StemInter.class);

        map(AbstractChordInter.class, ChordDynamicsRelation.class, DynamicsInter.class);
        map(AbstractChordInter.class, ChordPedalRelation.class, PedalInter.class);
        map(AbstractChordInter.class, ChordTupletRelation.class, TupletInter.class);
        map(AbstractChordInter.class, ChordWedgeRelation.class, WedgeInter.class);

        map(AlterInter.class, AlterHeadRelation.class, HeadInter.class);

        map(AugmentationDotInter.class, AugmentationRelation.class, AbstractNoteInter.class);
        map(AugmentationDotInter.class, DoubleDotRelation.class, AugmentationDotInter.class);

        map(BeamInter.class, BeamRestRelation.class, RestInter.class);

        map(EndingInter.class, EndingBarRelation.class, BarlineInter.class); // Old
        map(EndingInter.class, EndingBarRelation.class, StaffBarlineInter.class);
        map(EndingInter.class, EndingSentenceRelation.class, SentenceInter.class);

        map(FermataDotInter.class, DotFermataRelation.class, FermataArcInter.class); // Temporary!

        map(FermataInter.class, FermataBarRelation.class, BarlineInter.class); // Old
        map(FermataInter.class, FermataBarRelation.class, StaffBarlineInter.class);
        map(FermataInter.class, FermataChordRelation.class, AbstractChordInter.class);

        map(FlagInter.class, FlagStemRelation.class, StemInter.class);
        map(SmallFlagInter.class, FlagStemRelation.class, StemInter.class);

        map(HeadChordInter.class, ChordArpeggiatoRelation.class, ArpeggiatoInter.class);
        map(HeadChordInter.class, ChordArticulationRelation.class, ArticulationInter.class);
        map(HeadChordInter.class, ChordGraceRelation.class, GraceChordInter.class);
        map(HeadChordInter.class, ChordNameRelation.class, ChordNameInter.class);
        map(HeadChordInter.class, ChordOrnamentRelation.class, OrnamentInter.class);
        map(HeadChordInter.class, ChordSentenceRelation.class, SentenceInter.class);
        map(HeadChordInter.class, ChordStemRelation.class, StemInter.class);
        map(HeadChordInter.class, ChordSyllableRelation.class, LyricItemInter.class);

        map(HeadInter.class, HeadFingeringRelation.class, FingeringInter.class);
        map(HeadInter.class, HeadPlayingRelation.class, PlayingInter.class);
        map(HeadInter.class, HeadPluckingRelation.class, PluckingInter.class);
        map(HeadInter.class, HeadStemRelation.class, StemInter.class);

        map(MarkerInter.class, MarkerBarRelation.class, BarlineInter.class); // Old
        map(MarkerInter.class, MarkerBarRelation.class, StaffBarlineInter.class);

        map(MultipleRestInter.class, MultipleRestCountRelation.class, MeasureCountInter.class);

        map(RepeatDotInter.class, RepeatDotBarRelation.class, BarlineInter.class);
        map(RepeatDotInter.class, RepeatDotPairRelation.class, RepeatDotInter.class);

        map(MeasureRepeatInter.class, MeasureRepeatCountRelation.class, MeasureCountInter.class);

        map(SlurInter.class, SlurHeadRelation.class, HeadInter.class);

        map(TimeNumberInter.class, TimeTopBottomRelation.class, TimeNumberInter.class);

        map(TremoloInter.class, TremoloStemRelation.class, StemInter.class);
        map(TremoloInter.class, TremoloWholeRelation.class, HeadInter.class);
    }

    //-------------------------//
    // definedRelationsBetween //
    //-------------------------//
    /**
     * Report the defined relation classes between the provided source and target inter classes.
     *
     * @param sourceClass provided inter class as source
     * @param targetClass provided inter class as target
     * @return the list of defined relation classes, perhaps empty
     */
    private static Set<Class<? extends Relation>> definedRelationsBetween (
    // @formatter:off
        Class<? extends Inter> sourceClass,
        Class<? extends Inter> targetClass)
    // @formatter:on
    {
        final Set<Class<? extends Relation>> defined = new LinkedHashSet<>();
        Set<Class<? extends Relation>> from = definedRelationsFrom(sourceClass);
        Set<Class<? extends Relation>> to = definedRelationsTo(targetClass);
        defined.addAll(from);
        defined.retainAll(to);

        return !defined.isEmpty() ? defined : Collections.emptySet();
    }

    //----------------------//
    // definedRelationsFrom //
    //----------------------//
    /**
     * Report the defined relation classes from the provided source inter class.
     *
     * @param sourceClass provided inter class as source
     * @return the list of defined relation classes, perhaps empty
     */
    private static Set<Class<? extends Relation>> definedRelationsFrom (
    // @formatter:off
        Class<? extends Inter> sourceClass)
    // @formatter:on
    {
        Objects.requireNonNull(sourceClass, "Source class is null");

        final Set<Class<? extends Relation>> defined = new LinkedHashSet<>();

        // Walk up inheritance hierarchy starting from sourceClass
        Class classe = sourceClass;
        while (true) {
            if ((classe == null) || !Inter.class.isAssignableFrom(classe)) {
                break;
            }

            final Set<Class<? extends Relation>> set = src.get(classe);

            if (set != null) {
                defined.addAll(set);
            }

            classe = classe.getSuperclass();
        }

        return !defined.isEmpty() ? Collections.unmodifiableSet(defined) : Collections.emptySet();
    }

    //--------------------//
    // definedRelationsTo //
    //--------------------//
    /**
     * Report the defined relation classes to the provided target inter class.
     *
     * @param targetClass provided inter class as target
     * @return the list of defined relation classes, perhaps empty
     */
    private static Set<Class<? extends Relation>> definedRelationsTo (
    // @formatter:off
        Class<? extends Inter> targetClass)
    // @formatter:on
    {
        Objects.requireNonNull(targetClass, "Target class is null");

        final Set<Class<? extends Relation>> defined = new LinkedHashSet<>();
        Class classe = targetClass;

        while (true) {
            if ((classe == null) || !Inter.class.isAssignableFrom(classe)) {
                break;
            }

            Set<Class<? extends Relation>> set = tgt.get(classe);

            if (set != null) {
                defined.addAll(set);
            }

            classe = classe.getSuperclass();
        }

        return !defined.isEmpty() ? Collections.unmodifiableSet(defined) : Collections.emptySet();
    }

    //--------//
    // getSet //
    //--------//
    private static Set<Class<? extends Relation>> getSet (
    // @formatter:off
        Map<Class<? extends Inter>, Set<Class<? extends Relation>>> map,
        Class<? extends Inter> classe)
    // @formatter:on
    {
        Set<Class<? extends Relation>> set = map.get(classe);

        if (set == null) {
            map.put(classe, set = new LinkedHashSet<>());
        }

        return set;
    }

    //-----//
    // map //
    //-----//
    /**
     * Register the fact the provided relationClass can have sourceClass as source and
     * targetClass as target.
     *
     * @param sourceClass   source class
     * @param relationClass relation class
     * @param targetClass   target class
     */
    private static void map (Class<? extends Inter> sourceClass,
                             Class<? extends Relation> relationClass,
                             Class<? extends Inter> targetClass)
    {
        getSet(src, sourceClass).add(relationClass);
        getSet(tgt, targetClass).add(relationClass);
    }

    //--------//
    // nameOf //
    //--------//
    /**
     * Report a simple name for the provided relation class.
     *
     * @param relationClass provided relation class
     * @return simple name
     */
    public static String nameOf (Class<? extends Relation> relationClass)
    {
        return relationClass.getSimpleName().replaceFirst("Relation", "");
    }

    //-----------//
    // relations //
    //-----------//
    /**
     * Lookup for relations for which the provided predicate applies within the
     * provided collection.
     *
     * @param collection the collection of relations to browse
     * @param predicate  the predicate to apply, or null
     * @return the list of compliant relations, perhaps empty but not null
     */
    public static List<Relation> relations (Collection<? extends Relation> collection,
                                            Predicate<Relation> predicate)
    {
        List<Relation> found = new ArrayList<>();

        for (Relation relation : collection) {
            if ((predicate == null) || predicate.test(relation)) {
                found.add(relation);
            }
        }

        return found;
    }

    //---------------------------//
    // suggestedRelationsBetween //
    //---------------------------//
    /**
     * Report the suggested relation classes between the provided source and target inters.
     *
     * @param source provided inter as source
     * @param target provided inter as target
     * @return the possible relation classes
     */
    public static Set<Class<? extends Relation>> suggestedRelationsBetween (Inter source,
                                                                            Inter target)
    {
        // Check inputs
        Objects.requireNonNull(source, "Source is null");
        Objects.requireNonNull(target, "Target is null");

        SIGraph sig = source.getSig();
        Objects.requireNonNull(sig, "Source has no sig");

        if (target.getSig() != sig) {
            logger.info("Source and Target do not share the same sig");

            return Collections.emptySet();
        }

        // Suggestions
        Set<Class<? extends Relation>> suggestions = new LinkedHashSet<>(
                definedRelationsBetween(source.getClass(), target.getClass()));

        // Skip existing relation, if any, between source & target
        final Relation edge = sig.getEdge(source, target);
        if (edge != null) {
            suggestions.remove(edge.getClass());
        }

        return !suggestions.isEmpty() ? suggestions : Collections.emptySet();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //------------------------//
    // RelationClassPredicate //
    //------------------------//
    /**
     * Predicate to filter Relation instance of certain classes (or subclasses thereof).
     */
    public static class RelationClassPredicate
            implements Predicate<Relation>
    {

        private final Class[] classes;

        /**
         * Create relation class predicate.
         *
         * @param classes array of Filtering class
         */
        public RelationClassPredicate (Class[] classes)
        {
            this.classes = classes;
        }

        @Override
        public boolean test (Relation relation)
        {
            for (Class classe : classes) {
                if (classe.isInstance(relation)) {
                    return true;
                }
            }

            return false;
        }
    }
}
