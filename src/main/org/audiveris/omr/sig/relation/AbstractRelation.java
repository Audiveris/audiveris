//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 A b s t r a c t R e l a t i o n                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.util.Jaxb;

import org.jgrapht.event.GraphEdgeChangeEvent;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code AbstractRelation} is the abstract basis for any Relation implementation.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractRelation
        implements Relation, Cloneable
{
    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------
    //
    /** Indicates that this relation was set manually. */
    @XmlAttribute(name = "manual")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean manual;

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // added //
    //-------//
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        // No-op by default
    }

    //-----------//
    // duplicate //
    //-----------//
    @Override
    public Relation duplicate ()
    {
        try {
            return (Relation) super.clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        return internals();
    }

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return Relations.nameOf(getClass());
    }

    //----------//
    // isManual //
    //----------//
    /**
     * @return the manual
     */
    @Override
    public boolean isManual ()
    {
        return manual;
    }

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        // No-op by default
    }

    //----------//
    // seenFrom //
    //----------//
    @Override
    public String seenFrom (Inter inter)
    {
        final StringBuilder sb = new StringBuilder(toString());
        final SIGraph sig = inter.getSig();

        if (sig != null) {
            final Inter source = sig.getEdgeSource(this);

            if (source != inter) {
                sb.append("<-").append(source);
            } else {
                final Inter target = sig.getEdgeTarget(this);

                if (target != inter) {
                    sb.append("->").append(target);
                }
            }
        }

        return sb.toString();
    }

    //-----------//
    // setManual //
    //-----------//
    /**
     * @param manual the manual to set
     */
    @Override
    public void setManual (boolean manual)
    {
        this.manual = manual;
    }

    //--------------//
    // toLongString //
    //--------------//
    @Override
    public String toLongString (SIGraph sig)
    {
        final Inter source = sig.getEdgeSource(this);
        final Inter target = sig.getEdgeTarget(this);
        final StringBuilder sb = new StringBuilder();

        sb.append(source);
        sb.append("-");
        sb.append(getName());
        sb.append(":");
        sb.append(getDetails());
        sb.append("-");
        sb.append(target);

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return getName();
    }

    //-----------//
    // internals //
    //-----------//
    protected String internals ()
    {
        return isManual() ? "MANUAL" : "";
    }
}
