//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    R e l a t i o n V a l u e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.sig.inter.AbstractInter;
import omr.sig.inter.Inter;
import omr.sig.relation.AbstractRelation;
import omr.sig.relation.AbstractSupport;
import omr.sig.relation.BarConnectionRelation;
import omr.sig.relation.BarGroupRelation;
import omr.sig.relation.Relation;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlIDREF;

/**
 * Class {@code RelationValue} represents the content of an inter Relation for JAXB.
 *
 * @author Hervé Bitteur
 */
public class RelationValue
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlIDREF
    @XmlAttribute
    public AbstractInter source;

    @XmlIDREF
    @XmlAttribute
    public AbstractInter target;

    //    @XmlElementRef
    @XmlElements({
        @XmlElement(name = "bar-connection", type = BarConnectionRelation.class),
        @XmlElement(name = "bar-group", type = BarGroupRelation.class),
        @XmlElement(name = "support", type = AbstractSupport.class)
    })
    public AbstractRelation relation;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RelationValue} object.
     *
     * @param source   DOCUMENT ME!
     * @param target   DOCUMENT ME!
     * @param relation DOCUMENT ME!
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
     * Creates a new {@code RelationValue} object.
     */
    public RelationValue ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("RelationValue{");

        sb.append(" src:").append(source);

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
