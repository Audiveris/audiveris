//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          R e l a t i o n                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.sig.inter.InterPair;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.util.Jaxb;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Abstract class <code>Relation</code> describes a relation (edge) between two Inter
 * instances (vertices) within the same SIG (which implies the same SystemInfo).
 * <p>
 * There are many concrete classes that derive from this abstract class.
 * Most of them are named according to the same pattern:
 * <p>
 * <code>FooBarRelation</code> is a relation class with:
 * <ul>
 * <li><code>FooInter</code> as a <b>source</b> Inter of the relation
 * <li><code>BarInter</code> as a <b>target</b> Inter of the relation
 * </ul>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class Relation
        implements Cloneable
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Relation.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** If "true", this relation was set manually. */
    @XmlAttribute(name = "manual")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean manual;

    //~ Methods ------------------------------------------------------------------------------------

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

    //--------//
    // dumpOf //
    //--------//
    /**
     * Report a dump of this relation in the provided sig context.
     *
     * @param sig containing sig
     * @return the dump as a string
     */
    public String dumpOf (SIGraph sig)
    {
        return new StringBuilder().append("  Source: ").append(sig.getEdgeSource(this)).append("\n")
                .append("Relation: ").append(this).append(" ").append(getDetails()).append("\n")
                .append("  Target: ").append(sig.getEdgeTarget(this)).append("\n").toString();
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

    //-----------//
    // internals //
    //-----------//
    /**
     * Report a description string of class internals.
     *
     * @return description string of internals
     */
    protected String internals ()
    {
        return isManual() ? "MANUAL" : "";
    }

    //-------------//
    // isForbidden //
    //-------------//
    /**
     * Report whether this relation is <b>explicitly</b> forbidden between the provided
     * source and target Inter instances.
     * <p>
     * <b>WARNING</b>: Not being explicitly forbidden does not imply being allowed.
     *
     * @param source source Inter instance
     * @param target target Inter instance
     * @return true if forbidden
     */
    public boolean isForbidden (Inter source,
                                Inter target)
    {
        return false;
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
    // preLink //
    //---------//
    /**
     * Prepare the manual linking of provided source and target inters.
     *
     * @param pair (input/output) the {source,target} pair of inters
     * @return the sequence of additional UI tasks to perform, perhaps empty but not null.
     */
    public List<? extends UITask> preLink (InterPair pair)
    {
        return Collections.emptyList();
    }

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
}
