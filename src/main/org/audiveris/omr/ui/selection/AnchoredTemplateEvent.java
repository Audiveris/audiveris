//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            A n c h o r e d T e m p l a t e E v e n t                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.ui.selection;

import org.audiveris.omr.image.AnchoredTemplate;

/**
 * Class {@code AnchoredTemplateEvent} represents a selection of Template with a
 * specific anchor.
 *
 * @author Hervé Bitteur
 */
public class AnchoredTemplateEvent
        extends UserEvent<AnchoredTemplate>
{

    private final AnchoredTemplate anchoredTemplate;

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

    //---------//
    // getData //
    //---------//
    @Override
    public AnchoredTemplate getData ()
    {
        return anchoredTemplate;
    }
}
