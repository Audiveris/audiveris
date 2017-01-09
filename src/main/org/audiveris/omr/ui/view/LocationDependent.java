//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                L o c a t i o n D e p e n d e n t                               //
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
package org.audiveris.omr.ui.view;

import java.awt.Rectangle;

/**
 * Interface {@code LocationDependent} indicates an entity whose behavior may depend
 * on location currently defined (such as via user interface).
 *
 * @author Hervé Bitteur
 */
public interface LocationDependent
{
    //~ Methods ------------------------------------------------------------------------------------

    /** Update the entity with user current location.
     *
     * @param rect the user selected location
     */
    void updateUserLocation (Rectangle rect);
}
