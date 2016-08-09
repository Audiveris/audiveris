//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 A b s t r a c t R e l a t i o n                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
