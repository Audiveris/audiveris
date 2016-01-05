//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 A b s t r a c t R e l a t i o n                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.sig.SIGraph;
import omr.sig.inter.Inter;

/**
 * Class {@code AbstractRelation} is the abstract basis for any Relation implementation.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractRelation
        implements Relation, Cloneable
{
    //~ Methods ------------------------------------------------------------------------------------

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
        return getClass().getSimpleName().replaceFirst("Relation", "");
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
        return "";
    }
}
