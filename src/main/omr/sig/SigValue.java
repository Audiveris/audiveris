//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S i g V a l u e                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.sig.inter.AbstractInter;
import omr.sig.inter.AlterInter;
import omr.sig.inter.AugmentationDotInter;
import omr.sig.inter.BarConnectorInter;
import omr.sig.inter.BeamHookInter;
import omr.sig.inter.BeamInter;
import omr.sig.inter.BlackHeadInter;
import omr.sig.inter.BraceInter;
import omr.sig.inter.ChordNameInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.CodaInter;
import omr.sig.inter.DynamicsInter;
import omr.sig.inter.EndingInter;
import omr.sig.inter.FermataDotInter;
import omr.sig.inter.FermataInter;
import omr.sig.inter.FlagInter;
import omr.sig.inter.HeadChordInter;
import omr.sig.inter.Inter;
import omr.sig.inter.LyricItemInter;
import omr.sig.inter.LyricLineInter;
import omr.sig.inter.PedalInter;
import omr.sig.inter.RepeatDotInter;
import omr.sig.inter.RestChordInter;
import omr.sig.inter.RestInter;
import omr.sig.inter.SegnoInter;
import omr.sig.inter.SentenceInter;
import omr.sig.inter.SlurInter;
import omr.sig.inter.SmallBlackHeadInter;
import omr.sig.inter.SmallChordInter;
import omr.sig.inter.SmallFlagInter;
import omr.sig.inter.SmallVoidHeadInter;
import omr.sig.inter.SmallWholeInter;
import omr.sig.inter.StaccatoInter;
import omr.sig.inter.StemInter;
import omr.sig.inter.TupletInter;
import omr.sig.inter.VoidHeadInter;
import omr.sig.inter.WedgeInter;
import omr.sig.inter.WholeInter;
import omr.sig.inter.WordInter;
import omr.sig.relation.AbstractRelation;
import omr.sig.relation.AccidHeadRelation;
import omr.sig.relation.AugmentationRelation;
import omr.sig.relation.BarConnectionRelation;
import omr.sig.relation.BarGroupRelation;
import omr.sig.relation.BasicExclusion;
import omr.sig.relation.BeamHeadRelation;
import omr.sig.relation.BeamStemRelation;
import omr.sig.relation.ChordDynamicsRelation;
import omr.sig.relation.ChordNameRelation;
import omr.sig.relation.ChordPedalRelation;
import omr.sig.relation.ChordSentenceRelation;
import omr.sig.relation.ChordStaccatoRelation;
import omr.sig.relation.ChordSyllableRelation;
import omr.sig.relation.ChordTupletRelation;
import omr.sig.relation.ChordWedgeRelation;
import omr.sig.relation.ClefKeyRelation;
import omr.sig.relation.CodaBarRelation;
import omr.sig.relation.DotFermataRelation;
import omr.sig.relation.DoubleDotRelation;
import omr.sig.relation.EndingBarRelation;
import omr.sig.relation.FermataBarRelation;
import omr.sig.relation.FermataChordRelation;
import omr.sig.relation.FermataNoteRelation;
import omr.sig.relation.FlagStemRelation;
import omr.sig.relation.HeadStemRelation;
import omr.sig.relation.KeyAltersRelation;
import omr.sig.relation.NoExclusion;
import omr.sig.relation.Relation;
import omr.sig.relation.RepeatDotBarRelation;
import omr.sig.relation.RepeatDotPairRelation;
import omr.sig.relation.SegnoBarRelation;
import omr.sig.relation.SlurHeadRelation;
import omr.sig.relation.StemAlignmentRelation;
import omr.sig.relation.TimeTopBottomRelation;

import org.jgrapht.Graphs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;

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
     * Standalone inters found in sig, hence gathered here as true defs.
     * For easier review, class names are listed alphabetically.
     */
    @XmlElementWrapper(name = "inter-defs")
    @XmlElementRefs({
        @XmlElementRef(type = AlterInter.class),
        @XmlElementRef(type = AugmentationDotInter.class),
        @XmlElementRef(type = BarConnectorInter.class),
        @XmlElementRef(type = BeamHookInter.class),
        @XmlElementRef(type = BeamInter.class),
        @XmlElementRef(type = BlackHeadInter.class),
        @XmlElementRef(type = BraceInter.class),
        @XmlElementRef(type = ChordNameInter.class),
        @XmlElementRef(type = ClefInter.class),
        @XmlElementRef(type = CodaInter.class),
        @XmlElementRef(type = DynamicsInter.class),
        @XmlElementRef(type = EndingInter.class),
        @XmlElementRef(type = FermataDotInter.class),
        @XmlElementRef(type = FermataInter.class),
        @XmlElementRef(type = FlagInter.class),
        @XmlElementRef(type = HeadChordInter.class),
        @XmlElementRef(type = LyricItemInter.class),
        @XmlElementRef(type = LyricLineInter.class),
        @XmlElementRef(type = PedalInter.class),
        @XmlElementRef(type = RepeatDotInter.class),
        @XmlElementRef(type = RestChordInter.class),
        @XmlElementRef(type = RestInter.class),
        @XmlElementRef(type = SegnoInter.class),
        @XmlElementRef(type = SentenceInter.class),
        @XmlElementRef(type = SlurInter.class),
        @XmlElementRef(type = SmallBlackHeadInter.class),
        @XmlElementRef(type = SmallChordInter.class),
        @XmlElementRef(type = SmallFlagInter.class),
        @XmlElementRef(type = SmallVoidHeadInter.class),
        @XmlElementRef(type = SmallWholeInter.class),
        @XmlElementRef(type = StemInter.class),
        @XmlElementRef(type = StaccatoInter.class),
        @XmlElementRef(type = TupletInter.class),
        @XmlElementRef(type = VoidHeadInter.class),
        @XmlElementRef(type = WedgeInter.class),
        @XmlElementRef(type = WholeInter.class),
        @XmlElementRef(type = WordInter.class)
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
        // Allocate vertices
        Graphs.addAllVertices(sig, interRefs);
        Graphs.addAllVertices(sig, interDefs);

        for (Inter inter : sig.vertexSet()) {
            inter.setSig(sig);
        }

        // Allocate edges
        for (RelationValue rel : relations) {
            try {
                sig.addEdge(rel.source, rel.target, rel.relation);
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
         * We separate Inter instances already marshaled (the 'usedVertices') from the other
         * instances (the 'tempVertices) that are not used out of the SIG.
         *
         * @param sig the existing SIG whose content is to stored into a SigValue
         * @return the generated SigValue instance
         * @throws Exception
         */
        @Override
        public SigValue marshal (SIGraph sig)
                throws Exception
        {
            SigValue sigValue = new SigValue();

            // Dispose of interSet: from now on, any marshalling will go to interDefs
            InterSet interSet = sig.getSystem().getInterSet();
            HashSet<String> defined = interSet.getInters();
            sig.getSystem().setInterSet(null);

            for (Inter inter : sig.vertexSet()) {
                if (defined.contains(inter.getId())) {
                    sigValue.interRefs.add((AbstractInter) inter);
                } else {
                    sigValue.interDefs.add((AbstractInter) inter);
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
         * @param sigValue the value to be converted (can reportedly be null???)
         * @return a new SIG instance, to be later populated via {@link #populateSig}
         * @throws java.lang.Exception
         */
        @Override
        public SIGraph unmarshal (SigValue sigValue)
                throws Exception
        {
            if (sigValue == null) {
                logger.error("Trying to unmarshal a null SigValue");

                return null;
            }

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

        private final HashSet<String> defined = new HashSet<String>();

        //~ Methods --------------------------------------------------------------------------------
        public void addInter (Inter inter)
        {
            defined.add(inter.getId());
        }

        public HashSet<String> getInters ()
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

        /** Relation source vertex. */
        @XmlIDREF
        @XmlAttribute
        public AbstractInter source;

        /** Relation target vertex. */
        @XmlIDREF
        @XmlAttribute
        public AbstractInter target;

        /**
         * The relation instance.
         * <p>
         * Here we list alphabetically all concrete relation types. No abstract!
         */
        @XmlElementRefs({
            @XmlElementRef(type = AccidHeadRelation.class),
            @XmlElementRef(type = AugmentationRelation.class),
            @XmlElementRef(type = BarConnectionRelation.class),
            @XmlElementRef(type = BarGroupRelation.class),
            @XmlElementRef(type = BasicExclusion.class),
            @XmlElementRef(type = BeamHeadRelation.class),
            @XmlElementRef(type = BeamStemRelation.class),
            @XmlElementRef(type = CodaBarRelation.class),
            @XmlElementRef(type = ChordNameRelation.class),
            @XmlElementRef(type = ChordPedalRelation.class),
            @XmlElementRef(type = ChordSentenceRelation.class),
            @XmlElementRef(type = ChordSyllableRelation.class),
            @XmlElementRef(type = ChordDynamicsRelation.class),
            @XmlElementRef(type = ChordStaccatoRelation.class),
            @XmlElementRef(type = ChordTupletRelation.class),
            @XmlElementRef(type = ChordWedgeRelation.class),
            @XmlElementRef(type = ClefKeyRelation.class),
            @XmlElementRef(type = DotFermataRelation.class),
            @XmlElementRef(type = DoubleDotRelation.class),
            @XmlElementRef(type = EndingBarRelation.class),
            @XmlElementRef(type = FermataBarRelation.class),
            @XmlElementRef(type = FermataChordRelation.class),
            @XmlElementRef(type = FermataNoteRelation.class),
            @XmlElementRef(type = FlagStemRelation.class),
            @XmlElementRef(type = HeadStemRelation.class),
            @XmlElementRef(type = KeyAltersRelation.class),
            @XmlElementRef(type = NoExclusion.class),
            @XmlElementRef(type = RepeatDotBarRelation.class),
            @XmlElementRef(type = RepeatDotPairRelation.class),
            @XmlElementRef(type = SegnoBarRelation.class),
            @XmlElementRef(type = SlurHeadRelation.class),
            @XmlElementRef(type = StemAlignmentRelation.class),
            @XmlElementRef(type = TimeTopBottomRelation.class)
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
            this.source = (AbstractInter) source;
            this.target = (AbstractInter) target;
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

            sb.append("src:").append(source);

            if (source != null) {
                sb.append('@').append(Integer.toHexString(source.hashCode()));
            }

            sb.append(" tgt:").append(target);

            if (target != null) {
                sb.append('@').append(Integer.toHexString(target.hashCode()));
            }

            sb.append(" rel:").append(relation);
            sb.append('}');

            return sb.toString();
        }
    }
}
