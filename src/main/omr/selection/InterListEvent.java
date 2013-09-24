//----------------------------------------------------------------------------//
//                                                                            //
//                         I n t e r L i s t E v e n t                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.sig.Inter;

import java.util.List;
import java.util.Set;

/**
 * Class {@code InterListEvent} represents an Inter list event
 *
 * @author Hervé Bitteur
 */
public class InterListEvent
        extends UserEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected inter list, which may be null */
    private final List<Inter> inters;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new InterListEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin (or null)
     * @param movement the user movement
     * @param inters   the selected collection of inters (or null)
     */
    public InterListEvent (Object source,
                           SelectionHint hint,
                           MouseMovement movement,
                           List<Inter> inters)
    {
        super(source, hint, movement);
        this.inters = inters;
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public List<Inter> getData ()
    {
        return inters;
    }

    //----------------//
    // internalString //
    //----------------//
    @Override
    protected String internalString ()
    {
        if (inters != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");

            for (Inter inter : inters) {
                sb.append(inter)
                        .append(" ");
            }

            sb.append("]");

            return sb.toString();
        } else {
            return "";
        }
    }
}
