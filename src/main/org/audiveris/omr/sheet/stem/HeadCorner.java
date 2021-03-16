//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       H e a d C o r n e r                                      //
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
package org.audiveris.omr.sheet.stem;

import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.*;

/**
 * Class {@code HeadCorner} defines the four corners suitable for head connection to stem.
 *
 * @author Hervé Bitteur
 */
public enum HeadCorner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    TOP_RIGHT(TOP, RIGHT),
    BOTTOM_LEFT(BOTTOM, LEFT),
    TOP_LEFT(TOP, LEFT),
    BOTTOM_RIGHT(BOTTOM, RIGHT);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The vertical side. */
    public final VerticalSide vSide;

    /** The horizontal side. */
    public final HorizontalSide hSide;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Corner object.
     *
     * @param vSide vertical side
     * @param hSide horizontal side
     */
    private HeadCorner (VerticalSide vSide,
                        HorizontalSide hSide)
    {
        this.vSide = vSide;
        this.hSide = hSide;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the corner ID.
     *
     * @return id
     */
    public String getId ()
    {
        return "" + vSide.name().charAt(0) + '-' + hSide.name().charAt(0);
    }

    //----//
    // of //
    //----//
    public static HeadCorner of (VerticalSide vSide,
                                 HorizontalSide hSide)
    {
        switch (vSide) {
        case TOP:
            switch (hSide) {
            case LEFT:
                return TOP_LEFT;
            case RIGHT:
                return TOP_RIGHT;
            }
        case BOTTOM:
            switch (hSide) {
            case LEFT:
                return BOTTOM_LEFT;
            case RIGHT:
                return BOTTOM_RIGHT;
            }
        }

        return null; // To keep compiler happy...
    }
}
