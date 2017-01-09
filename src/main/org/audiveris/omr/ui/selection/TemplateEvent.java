//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T e m p l a t e E v e n t                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import org.audiveris.omr.image.Template;

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
