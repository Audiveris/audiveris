//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e l a t i o n A c t i o n                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.selection.InterListEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;

import omr.sig.SIGraph;
import omr.sig.inter.Inter;
import omr.sig.relation.Relation;

import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import static javax.swing.Action.SHORT_DESCRIPTION;

/**
 * Class {@code RelationAction} displays a relation and selects the other interpretation
 * (source or target).
 *
 * @author Hervé Bitteur
 */
class RelationAction
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
            SIGraph sig = other.getSig();
            InterListEvent event = new InterListEvent(
                    this,
                    SelectionHint.INTER_INIT,
                    MouseMovement.PRESSING,
                    Arrays.asList(other));
            sig.publish(event);
        }
    }
}
