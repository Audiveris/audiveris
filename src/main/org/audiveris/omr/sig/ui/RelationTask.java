//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     R e l a t i o n T a s k                                    //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Relation;

/**
 * Class {@code RelationTask} acts on relations, by linking or unlinking inters.
 *
 * @author Hervé Bitteur
 */
public abstract class RelationTask
        extends UITask
{
    //~ Instance fields ----------------------------------------------------------------------------

    protected final Relation relation;

    protected Inter source;

    protected Inter target;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code RelationTask} object.
     *
     * @param sig      the underlying sig
     * @param relation the relation task is focused upon
     */
    public RelationTask (SIGraph sig,
                         Relation relation)
    {
        super(sig);
        this.relation = relation;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * @return the relation
     */
    public Relation getRelation ()
    {
        return relation;
    }

    /**
     * @return the source
     */
    public Inter getSource ()
    {
        return source;
    }

    /**
     * @return the target
     */
    public Inter getTarget ()
    {
        return target;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(actionName());
        sb.append(" ").append(relation);
        sb.append(" src:").append(source);
        sb.append(" tgt:").append(target);

        return sb.toString();
    }
}
