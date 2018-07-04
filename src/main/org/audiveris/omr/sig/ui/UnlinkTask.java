//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       U n l i n k T a s k                                      //
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
import org.audiveris.omr.sig.relation.Relation;

/**
 * Class {@code UnlinkTask}
 *
 * @author Hervé Bitteur
 */
public class UnlinkTask
        extends RelationTask
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code UnlinkTask} object.
     *
     * @param sig      the underlying sig
     * @param relation the relation task is focused upon
     */
    public UnlinkTask (SIGraph sig,
                       Relation relation)
    {
        super(sig, relation);
        source = sig.getEdgeSource(relation);
        target = sig.getEdgeTarget(relation);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void performDo ()
    {
        sig.removeEdge(getRelation());

//        // Source inter may have been removed when publication is seen on UI...
//        if (!source.isRemoved()) {
//            sheet.getInterIndex().publish(source);
//        }
    }

    @Override
    public void performUndo ()
    {
        sig.addEdge(getSource(), getTarget(), getRelation());

        sheet.getInterIndex().publish(source);
    }

    @Override
    protected String actionName ()
    {
        return "unlink";
    }
}
