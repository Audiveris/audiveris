//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          R e l a t i o n                                       //
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
 * Abstract class {@code Relation} describes a relation between two Inter instances.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class Relation
        implements Cloneable
{

    // Persistent data
    //----------------
    //
    /** Indicates that this relation was set manually. */
    @XmlAttribute(name = "manual")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean manual;

    //-------//
    // added //
    //-------//
    /**
     * Notifies that this relation has been added to the sig.
     *
     * @param e the relation event.
     */
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        // No-op by default
    }

    //-----------//
    // duplicate //
    //-----------//
    /**
     * Clone a relation.
     *
     * @return the cloned relation
     */
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
    /**
     * Details for tip.
     *
     * @return relation details
     */
    public String getDetails ()
    {
        return internals();
    }

    //---------//
    // getName //
    //---------//
    /**
     * Short name.
     *
     * @return the relation short name
     */
    public String getName ()
    {
        return Relations.nameOf(getClass());
    }

    //----------//
    // isManual //
    //----------//
    /**
     * Report whether this relation has been set manually.
     *
     * @return true if manual
     */
    public boolean isManual ()
    {
        return manual;
    }

    /**
     * Tell if, seen from a given target, there can be at most one source.
     *
     * @return true if source number is limited to 1, false by default
     */
    public abstract boolean isSingleSource ();

    /**
     * Tell if, seen from a given source, there can be at most one target.
     *
     * @return true if target number is limited to 1, false by default
     */
    public abstract boolean isSingleTarget ();

    //---------//
    // removed //
    //---------//
    /**
     * Notifies that this relation has been removed from the sig.
     *
     * @param e the relation event.
     */
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        // No-op by default
    }

    //----------//
    // seenFrom //
    //----------//
    /**
     * Relation description when seen from one of its involved inters
     *
     * @param inter the interpretation point of view
     * @return the inter-based description
     */
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
     * Set this relation as a manual one.
     *
     * @param manual new value
     */
    public void setManual (boolean manual)
    {
        this.manual = manual;
    }

    //--------------//
    // toLongString //
    //--------------//
    /**
     * Report a long description of the relation
     *
     * @param sig the containing sig
     * @return long description
     */
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
