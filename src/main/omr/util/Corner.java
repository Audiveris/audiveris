//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           C o r n e r                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.image.Anchored.Anchor;
import static omr.util.HorizontalSide.*;
import static omr.util.VerticalSide.*;

import java.util.Arrays;
import java.util.List;

/**
 * Class {@code Corner} defines the 4 corners.
 *
 * @author Hervé Bitteur
 */
public class Corner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    public static final Corner TOP_LEFT = new Corner(TOP, LEFT);

    public static final Corner TOP_RIGHT = new Corner(TOP, RIGHT);

    public static final Corner BOTTOM_LEFT = new Corner(BOTTOM, LEFT);

    public static final Corner BOTTOM_RIGHT = new Corner(BOTTOM, RIGHT);

    /** Most popular connection corners are listed first. */
    public static final List<Corner> values = Arrays.asList(
            TOP_RIGHT,
            BOTTOM_LEFT,
            TOP_LEFT,
            BOTTOM_RIGHT);

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
    private Corner (VerticalSide vSide,
                    HorizontalSide hSide)
    {
        this.vSide = vSide;
        this.hSide = hSide;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
