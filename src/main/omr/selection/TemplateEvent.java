//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T e m p l a t e E v e n t                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.image.Template;

/**
 * Class {@code TemplateEvent} represents a Template selection.
 *
 * @author Hervé Bitteur
 */
public class TemplateEvent
        extends UserEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final Template template;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TemplateEvent} object.
     *
     * @param source   the entity that created this event
     * @param hint     how the event originated
     * @param movement the mouse movement
     * @param template the selected template or null
     */
    public TemplateEvent (Object source,
                          SelectionHint hint,
                          MouseMovement movement,
                          Template template)
    {
        super(source, hint, movement);
        this.template = template;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public Template getData ()
    {
        return template;
    }
}
