//----------------------------------------------------------------------------//
//                                                                            //
//                          S e c t i o n E v e n t                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.lag.Section;

/**
 * Class {@code SectionEvent} represents a Section selection.
 *
 * @author Hervé Bitteur
 */
public class SectionEvent
        extends LagEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected section, which may be null */
    private final Section section;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // SectionEvent //
    //--------------//
    /**
     * Creates a new SectionEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin
     * @param movement the mouse movement
     * @param section  the selected section (or null)
     */
    public SectionEvent (Object source,
                         SelectionHint hint,
                         MouseMovement movement,
                         Section section)
    {
        super(source, hint, movement);
        this.section = section;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getEntity //
    //-----------//
    @Override
    public Section getData ()
    {
        return section;
    }
}
