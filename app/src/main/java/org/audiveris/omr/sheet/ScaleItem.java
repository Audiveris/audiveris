//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S c a l e I t e m                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

/**
 * Scale information item.
 */
public enum ScaleItem
{
    line,
    interline,
    smallInterline,
    beam,
    smallBeam,
    stem;

    //~ Static fields/initializers -----------------------------------------------------------------

    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(ScaleItem.class);

    //~ Instance fields ----------------------------------------------------------------------------

    private String text; // Cached

    private String tip; // Cached

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report item text
     *
     * @return the item text
     */
    public String getText ()
    {
        if (text == null) {
            text = resources.getString(name() + ".text");
        }

        return text;
    }

    /**
     * Report item tip, if any
     *
     * @return the item tip, or null
     */
    public String getTip ()
    {
        if (tip == null) {
            tip = resources.getString(name() + ".toolTipText");
        }

        return tip;
    }
}
