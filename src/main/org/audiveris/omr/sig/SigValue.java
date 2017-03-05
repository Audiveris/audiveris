//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S i g V a l u e                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import org.audiveris.omr.sig.inter.AbstractInter;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarConnectorInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.BraceInter;
import org.audiveris.omr.sig.inter.BracketConnectorInter;
import org.audiveris.omr.sig.inter.BracketInter;
import org.audiveris.omr.sig.inter.ChordNameInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.CodaInter;
import org.audiveris.omr.sig.inter.DynamicsInter;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.FermataArcInter;
import org.audiveris.omr.sig.inter.FermataDotInter;
import org.audiveris.omr.sig.inter.FermataInter;
import org.audiveris.omr.sig.inter.FingeringInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.FretInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.KeyAlterInter;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.PedalInter;
import org.audiveris.omr.sig.inter.PluckingInter;
import org.audiveris.omr.sig.inter.RepeatDotInter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SegmentInter;
import org.audiveris.omr.sig.inter.SegnoInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallBeamInter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.inter.SmallFlagInter;
import org.audiveris.omr.sig.inter.StaccatoInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.AbstractRelation;
import org.audiveris.omr.sig.relation.AlterHeadRelation;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.BarConnectionRelation;
import org.audiveris.omr.sig.relation.BarGroupRelation;
import org.audiveris.omr.sig.relation.BasicExclusion;
import org.audiveris.omr.sig.relation.BeamHeadRelation;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.ChordDynamicsRelation;
import org.audiveris.omr.sig.relation.ChordNameRelation;
import org.audiveris.omr.sig.relation.ChordPedalRelation;
import org.audiveris.omr.sig.relation.ChordSentenceRelation;
import org.audiveris.omr.sig.relation.ChordStaccatoRelation;
import org.audiveris.omr.sig.relation.ChordSyllableRelation;
import org.audiveris.omr.sig.relation.ChordTupletRelation;
import org.audiveris.omr.sig.relation.ChordWedgeRelation;
import org.audiveris.omr.sig.relation.ClefKeyRelation;
import org.audiveris.omr.sig.relation.CodaBarRelation;
import org.audiveris.omr.sig.relation.DotFermataRelation;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.EndingBarRelation;
import org.audiveris.omr.sig.relation.EndingSentenceRelation;
import org.audiveris.omr.sig.relation.FermataBarRelation;
import org.audiveris.omr.sig.relation.FermataChordRelation;
import org.audiveris.omr.sig.relation.FermataNoteRelation;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.HeadHeadRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.KeyAltersRelation;
import org.audiveris.omr.sig.relation.NoExclusion;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.RepeatDotBarRelation;
import org.audiveris.omr.sig.relation.RepeatDotPairRelation;
import org.audiveris.omr.sig.relation.SegnoBarRelation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.sig.relation.StemAlignmentRelation;
import org.audiveris.omr.sig.relation.TimeTopBottomRelation;

import org.jgrapht.Graphs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code SigValue} represents the content of a SIG for use by JAXB.
 * <p>
 * There is a trick to handle separately the Inter instances that appear within the containing
 * system structure (and within the SIG) from the other Inter instances that exist only within the
 * SIG. The former ones are handled as XmlIDREF's in interRefs, the latter ones as XmlElement's in
 * interDefs.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "sig")
@XmlAccessorType(XmlAccessType.NONE)
public class SigValue
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            SigValue.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Inters already defined in system structure, hence gathered here as mere refs. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "inter-refs")
    private final ArrayList<AbstractInter> interRefs = new ArrayList<AbstractInter>();

    /**
     * All CONCRETE inters found in sig, gathered here as true defs. No abstract!
     * For easier review, class names are listed alphabetically.
     */
    @XmlElementWrapper(name = "inter-defs")
    @XmlElementRefs({
        @XmlElementRef(type = AlterInter.class)
        , @XmlElementRef(type = AugmentationDotInter.class)
        , @XmlElementRef(type = BarConnectorInter.class)
        , @XmlElementRef(type = BarlineInter.class)
        , @XmlElementRef(type = BeamHookInter.class)
        , @XmlElementRef(type = BeamInter.class)
        , @XmlElementRef(type = BraceInter.class)
        , @XmlElementRef(type = BracketConnectorInter.class)
        , @XmlElementRef(type = BracketInter.class)
        , @XmlElementRef(type = ChordNameInter.class)
        , @XmlElementRef(type = ClefInter.class)
        , @XmlElementRef(type = CodaInter.class)
        , @XmlElementRef(type = DynamicsInter.class)
        , @XmlElementRef(type = EndingInter.class)
        , @XmlElementRef(type = FermataDotInter.class)
        , @XmlElementRef(type = FermataArcInter.class)
        , @XmlElementRef(type = FermataInter.class)
        , @XmlElementRef(type = FingeringInter.class)
        , @XmlElementRef(type = FlagInter.class)
        , @XmlElementRef(type = FretInter.class)
        , @XmlElementRef(type = HeadChordInter.class)
        , @XmlElementRef(type = HeadInter.class)
        , @XmlElementRef(type = KeyAlterInter.class)
        , @XmlElementRef(type = KeyInter.class)
        , @XmlElementRef(type = LedgerInter.class)
        , // Cannot occur ???
        @XmlElementRef(type = LyricItemInter.class)
        , @XmlElementRef(type = LyricLineInter.class)
        , @XmlElementRef(type = PedalInter.class)
        , @XmlElementRef(type = PluckingInter.class)
        , @XmlElementRef(type = RepeatDotInter.class)
        , @XmlElementRef(type = RestChordInter.class)
        , @XmlElementRef(type = RestInter.class)
        , @XmlElementRef(type = SegmentInter.class)
        , @XmlElementRef(type = SegnoInter.class)
        , @XmlElementRef(type = SentenceInter.class)
        , @XmlElementRef(type = SlurInter.class)
        , @XmlElementRef(type = SmallBeamInter.class)
        , @XmlElementRef(type = SmallChordInter.class)
        , @XmlElementRef(type = SmallFlagInter.class)
        , @XmlElementRef(type = StaccatoInter.class)
        , @XmlElementRef(type = StemInter.class)
        , @XmlElementRef(type = TimeNumberInter.class)
        , @XmlElementRef(type = TimePairInter.class)
        , @XmlElementRef(type = TimeWholeInter.class)
        , @XmlElementRef(type = TupletInter.class)
        , @XmlElementRef(type = WedgeInter.class)
        , @XmlElementRef(type = WordInter.class)
    })
    private final ArrayList<AbstractInter> interDefs = new ArrayList<AbstractInter>();

    /** Sig edges: relations between inters. */
    @XmlElementWrapper(name = "relations")
    @XmlElement(name = "relation")
    private final ArrayList<RelationValue> relations = new ArrayList<RelationValue>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * No-arg constructor meant for JAXB.
     */
    public SigValue ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Method to be called only when SigValue IDREFs have been fully unmarshalled,
     * to populate the target SIG.
     *
     * @param sig the (rather empty) sig to be completed
     */
    public void populateSig (SIGraph sig)
    {
        final InterIndex index = sig.getSystem().getSheet().getInterIndex();

        // Allocate vertices
        Graphs.addAllVertices(sig, interRefs);
        Graphs.addAllVertices(sig, interDefs);

        for (Inter inter : sig.vertexSet()) {
            inter.setSig(sig);
            index.insert(inter);
        }

        // Allocate edges
        for (RelationValue rel : relations) {
            try {
                Inter source = index.getEntity(rel.sourceId);
                Inter target = index.getEntity(rel.targetId);
                sig.addEdge(source, target, rel.relation);
            } catch (Throwable ex) {
                logger.error("Error unmarshalling relation " + rel + " ex:" + ex, ex);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    /**
     * Meant for JAXB handling of SIG.
     */
    public static class Adapter
            extends XmlAdapter<SigValue, SIGraph>
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * Generate a SigValue out of the existing SIG.
         * We separate Inter instances already marshalled (the 'interRefs') from the other
         * instances (the 'interDefs') that are used only in SIG.
         *
         * @param sig the existing SIG whose content is to be stored into a SigValue
         * @return the generated SigValue instance
         * @throws Exception
         */
        @Override
        public SigValue marshal (SIGraph sig)
                throws Exception
        {
            SigValue sigValue = new SigValue();

            // Dispose of interSet: from now on, any marshalling will go directly to interDefs
            InterSet interSet = sig.getSystem().getInterSet();
            LinkedHashSet<AbstractInter> defined = interSet.getInters();
            sig.getSystem().setInterSet(null);

            for (Inter inter : sig.vertexSet()) {
                AbstractInter abstractInter = (AbstractInter) inter;

                if (!defined.contains(abstractInter)) {
                    sigValue.interDefs.add(abstractInter);
                } else {
                    sigValue.interRefs.add(abstractInter);
                }
            }

            for (Relation edge : sig.edgeSet()) {
                sigValue.relations.add(
                        new RelationValue(sig.getEdgeSource(edge), sig.getEdgeTarget(edge), edge));
            }

            return sigValue;
        }

        /**
         * Generate a (rather empty) SIG from this SigValue
         *
         * @param sigValue the value to be converted
         * @return a new SIG instance, to be later populated via {@link #populateSig}
         * @throws Exception
         */
        @Override
        public SIGraph unmarshal (SigValue sigValue)
                throws Exception
        {
            return new SIGraph(sigValue);
        }
    }

    //----------//
    // InterSet //
    //----------//
    /**
     * Class {@code InterSet} allows to separate Inter instances defined in system
     * structure from Inter instances only found in the system SIG.
     */
    public static class InterSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final LinkedHashSet<AbstractInter> defined = new LinkedHashSet<AbstractInter>();

        //~ Methods --------------------------------------------------------------------------------
        public void addInter (AbstractInter inter)
        {
            defined.add(inter);
        }

        public LinkedHashSet<AbstractInter> getInters ()
        {
            return defined;
        }
    }

    //---------------//
    // RelationValue //
    //---------------//
    /**
     * Class {@code RelationValue} represents the content of an inter Relation for JAXB.
     */
    private static class RelationValue
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Relation source vertex ID. */
        @XmlAttribute(name = "source")
        public int sourceId;

        /** Relation target vertex ID. */
        @XmlAttribute(name = "target")
        public int targetId;

        /**
         * The relation instance.
         * <p>
         * Here we list alphabetically all CONCRETE relation types. No abstract!
         * CrossExclusion is not listed here, because it must be handled outside any specific sig.
         */
        @XmlElementRefs({
            @XmlElementRef(type = AlterHeadRelation.class)
            , @XmlElementRef(type = AugmentationRelation.class)
            , @XmlElementRef(type = BarConnectionRelation.class)
            , @XmlElementRef(type = BarGroupRelation.class)
            , @XmlElementRef(type = BasicExclusion.class)
            , @XmlElementRef(type = BeamHeadRelation.class)
            , @XmlElementRef(type = BeamStemRelation.class)
            , @XmlElementRef(type = ChordDynamicsRelation.class)
            , @XmlElementRef(type = ChordNameRelation.class)
            , @XmlElementRef(type = ChordPedalRelation.class)
            , @XmlElementRef(type = ChordSentenceRelation.class)
            , @XmlElementRef(type = ChordStaccatoRelation.class)
            , @XmlElementRef(type = ChordSyllableRelation.class)
            , @XmlElementRef(type = ChordTupletRelation.class)
            , @XmlElementRef(type = ChordWedgeRelation.class)
            , @XmlElementRef(type = ClefKeyRelation.class)
            , @XmlElementRef(type = CodaBarRelation.class)
            , @XmlElementRef(type = DotFermataRelation.class)
            , @XmlElementRef(type = DoubleDotRelation.class)
            , @XmlElementRef(type = EndingBarRelation.class)
            , @XmlElementRef(type = EndingSentenceRelation.class)
            , @XmlElementRef(type = FermataBarRelation.class)
            , @XmlElementRef(type = FermataChordRelation.class)
            , @XmlElementRef(type = FermataNoteRelation.class)
            , @XmlElementRef(type = FlagStemRelation.class)
            , @XmlElementRef(type = HeadHeadRelation.class)
            , @XmlElementRef(type = HeadStemRelation.class)
            , @XmlElementRef(type = KeyAltersRelation.class)
            , @XmlElementRef(type = NoExclusion.class)
            , @XmlElementRef(type = RepeatDotBarRelation.class)
            , @XmlElementRef(type = RepeatDotPairRelation.class)
            , @XmlElementRef(type = SegnoBarRelation.class)
            , @XmlElementRef(type = SlurHeadRelation.class)
            , @XmlElementRef(type = StemAlignmentRelation.class)
            , @XmlElementRef(type = TimeTopBottomRelation.class)
        })
        public AbstractRelation relation;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new {@code RelationValue} object.
         *
         * @param source   source inter
         * @param target   target inter
         * @param relation relation from source to target
         */
        public RelationValue (Inter source,
                              Inter target,
                              Relation relation)
        {
            this.sourceId = source.getId();
            this.targetId = target.getId();
            this.relation = (AbstractRelation) relation;
        }

        /**
         * No-arg constructor meant for JAXB.
         */
        private RelationValue ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("RelationValue{");

            sb.append("src:").append(sourceId);
            sb.append(" tgt:").append(targetId);
            sb.append(" rel:").append(relation);

            sb.append('}');

            return sb.toString();
        }
    }
}
