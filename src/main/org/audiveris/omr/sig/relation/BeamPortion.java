//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B e a m P o r t i o n                                     //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.util.HorizontalSide;

/**
 * Enum {@code BeamPortion} defines which portion of a beam is used in a relation with
 * a stem.
 *
 * @author Hervé Bitteur
 */
public enum BeamPortion
{
    LEFT,
    CENTER,
    RIGHT;

    /**
     * Report the HorizontalSide of this portion.
     *
     * @return LEFT or RIGHT, null for CENTER
     */
    public HorizontalSide side ()
    {
        switch (this) {
        case LEFT:
            return HorizontalSide.LEFT;

        case RIGHT:
            return HorizontalSide.RIGHT;

        default:
        case CENTER:
            return null;
        }
    }
}
