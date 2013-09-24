//----------------------------------------------------------------------------//
//                                                                            //
//                         R e l a t i o n A c t i o n                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.selection.InterListEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;

import omr.sig.Inter;
import omr.sig.Relation;
import omr.sig.SIGraph;

import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import static javax.swing.Action.SHORT_DESCRIPTION;

/**
 * Class {@code RelationAction} displays a relation and selects the
 * other interpretation (source or target).
 *
 * @author Hervé Bitteur
 */
class RelationAction
        extends AbstractAction
{
    //~ Instance fields --------------------------------------------------------

    /** Originating inter. */
    private final Inter inter;

    /** Underlying relation. */
    private final Relation relation;

    /** The other inter, if any. */
    private final Inter other;

    //~ Constructors -----------------------------------------------------------
    //----------------//
    // RelationAction //
    //----------------//
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
        
        StringBuilder sb = new StringBuilder();
        sb.append(relation);

        if (source != inter) {
            other = source;
            sb.append(" <- ")
                    .append(source);
        } else if (target != inter) {
            other = target;
            sb.append(" -> ")
                    .append(target);
        } else {
            other = null;
        }

        //        if (relation instanceof AbstractConnection) {
        //            AbstractConnection rel = (AbstractConnection) relation;
        //            double cp = sig.getContextualGrade(rel);
        //            sb.append(" CP:")
        //                    .append(String.format("%.2f", cp));
        //
        //            if (rel instanceof HeadStemRelation && (beamStemRel != null)) {
        //                double cp2 = sig.getContextualGrade(inter, rel, beamStemRel);
        //                sb.append(" CP2:")
        //                        .append(String.format("%.2f", cp2));
        //            }
        //        }
        putValue(NAME, sb.toString());

        final String details = relation.getDetails();

        if (!details.isEmpty()) {
            putValue(SHORT_DESCRIPTION, details);
        }
    }

    //~ Methods ----------------------------------------------------------------
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
