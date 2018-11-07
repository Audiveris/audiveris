//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           C o r n e r                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.util;

import org.audiveris.omr.image.Anchored.Anchor;
import static org.audiveris.omr.util.HorizontalSide.*;
import static org.audiveris.omr.util.VerticalSide.*;

import java.util.Arrays;
import java.util.List;

/**
 * Class {@code Corner} defines the 4 corners.
 *
 * @author Hervé Bitteur
 */
public class Corner
{

    public static final Corner TOP_LEFT = new Corner(TOP, LEFT);

    public static final Corner TOP_RIGHT = new Corner(TOP, RIGHT);

    public static final Corner BOTTOM_LEFT = new Corner(BOTTOM, LEFT);

    public static final Corner BOTTOM_RIGHT = new Corner(BOTTOM, RIGHT);

    /** Most popular connection corners are listed first. */
    public static final List<Corner> values = Arrays.asList(TOP_RIGHT, BOTTOM_LEFT, TOP_LEFT,
                                                            BOTTOM_RIGHT);

    /** The vertical side. */
    public final VerticalSide vSide;

    /** The horizontal side. */
    public final HorizontalSide hSide;

    /**
     * Creates a new Corner object.
     *
     * @param vSide vertical side
     * @param hSide horizontal side
     */
    private Corner (VerticalSide vSide,
                    HorizontalSide hSide)
    {
        this.vSide = vSide;
        this.hSide = hSide;
    }

    public int getId ()
    {
        return values.indexOf(this);
    }

    public Anchor stemAnchor ()
    {
        if (this == TOP_LEFT) {
            return Anchor.TOP_LEFT_STEM;
        }

        if (this == TOP_RIGHT) {
            return Anchor.TOP_RIGHT_STEM;
        }

        if (this == BOTTOM_LEFT) {
            return Anchor.BOTTOM_LEFT_STEM;
        }

        if (this == BOTTOM_RIGHT) {
            return Anchor.BOTTOM_RIGHT_STEM;
        }

        return null;
    }

    @Override
    public String toString ()
    {
        return vSide + "-" + hSide;
    }
}
