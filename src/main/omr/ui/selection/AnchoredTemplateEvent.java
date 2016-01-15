//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            A n c h o r e d T e m p l a t e E v e n t                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.selection;

import omr.image.AnchoredTemplate;

/**
 * Class {@code AnchoredTemplateEvent} represents a selection of Template with a
 * specific anchor.
 *
 * @author Hervé Bitteur
 */
public class AnchoredTemplateEvent
        extends UserEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final AnchoredTemplate anchoredTemplate;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AnchoredTemplateEvent} object.
     *
     * @param source           the entity that created this event
     * @param hint             how the event originated
     * @param movement         the mouse movement
     * @param anchoredTemplate the selected anchored template or null
     */
    public AnchoredTemplateEvent (Object source,
                                  SelectionHint hint,
                                  MouseMovement movement,
                                  AnchoredTemplate anchoredTemplate)
    {
        super(source, hint, movement);
        this.anchoredTemplate = anchoredTemplate;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public AnchoredTemplate getData ()
    {
        return anchoredTemplate;
    }
}
