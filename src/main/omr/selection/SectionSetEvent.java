//----------------------------------------------------------------------------//
//                                                                            //
//                       S e c t i o n S e t E v e n t                        //
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
import omr.lag.Sections;

import java.util.Set;

/**
 * Class {@code SectionSetEvent} represents a Section Set selection.
 *
 * @author Hervé Bitteur
 */
public class SectionSetEvent
        extends LagEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected section set, which may be null */
    private final Set<Section> sections;

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // SectionSetEvent //
    //-----------------//
    /**
     * Creates a new SectionSetEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin (or null)
     * @param movement the mouse movement
     * @param sections the selected collection of sections (or null)
     */
    public SectionSetEvent (Object source,
                            SelectionHint hint,
                            MouseMovement movement,
                            Set<Section> sections)
    {
        super(source, hint, movement);
        this.sections = sections;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getEntity //
    //-----------//
    @Override
    public Set<Section> getData ()
    {
        return sections;
    }

    //----------------//
    // internalString //
    //----------------//
    @Override
    protected String internalString ()
    {
        if (sections != null) {
            return Sections.toString(sections);
        } else {
            return "";
        }
    }
}
