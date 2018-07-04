//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e l a t i o n A c t i o n                                  //
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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import static javax.swing.Action.SHORT_DESCRIPTION;

/**
 * Class {@code RelationAction} displays a relation and selects the other interpretation
 * (source or target).
 *
 * @author Hervé Bitteur
 */
public class RelationAction
        extends AbstractAction
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Originating inter. */
    private final Inter inter;

    /** Underlying relation. */
    private final Relation relation;

    /** The other inter, if any. */
    private final Inter other;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new RelationAction object.
     *
     * @param inter    originating inter
     * @param relation underlying relation
     */
    public RelationAction (Inter inter,
                           Relation relation)
    {
        this.inter = inter;
        this.relation = relation;

        SIGraph sig = inter.getSig();
        Inter source = sig.getEdgeSource(relation);
        Inter target = sig.getEdgeTarget(relation);

        if (source != inter) {
            other = source;
        } else if (target != inter) {
            other = target;
        } else {
            other = null;
        }

        putValue(NAME, relation.seenFrom(inter));

        final String details = relation.getDetails();

        if (!details.isEmpty()) {
            putValue(SHORT_DESCRIPTION, details);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // actionPerformed //
    //-----------------//
    @Override
    public void actionPerformed (ActionEvent e)
    {
        publish();
    }

    //---------//
    // publish //
    //---------//
    public void publish ()
    {
        if (other != null) {
            other.getSig().publish(other);
        }
    }

    /**
     * @return the inter
     */
    public Inter getInter ()
    {
        return inter;
    }

    /**
     * @return the relation
     */
    public Relation getRelation ()
    {
        return relation;
    }

    /**
     * @return the other
     */
    public Inter getOther ()
    {
        return other;
    }
}
