//----------------------------------------------------------------------------//
//                                                                            //
//                        S e c t i o n I d E v e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

/**
 * Class {@code SectionIdEvent} represents a Section Id selection.
 *
 * @author Hervé Bitteur
 */
public class SectionIdEvent
        extends LagEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected section id, which may be null */
    private final Integer id;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new SectionIdEvent object.
     *
     * @param source the entity that created this event
     * @param hint   hint about event origin (or null)
     * @param id     the selected section id (or null)
     */
    public SectionIdEvent (Object source,
                           SelectionHint hint,
                           Integer id)
    {
        super(source, hint, null);
        this.id = id;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getEntity //
    //-----------//
    @Override
    public Integer getData ()
    {
        return id;
    }
}
