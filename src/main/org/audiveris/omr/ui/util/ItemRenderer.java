//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I t e m R e n d e r e r                                   //
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
package org.audiveris.omr.ui.util;

import java.awt.Graphics2D;

/**
 * Describes a class as able to render specific items of its own upon a provided
 * graphics environment.
 *
 * @author herve
 */
public interface ItemRenderer
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Render items on the provided graphics
     *
     * @param graphics the graphics environment
     */
    void renderItems (Graphics2D graphics);
}
