//----------------------------------------------------------------------------//
//                                                                            //
//                       S y s t e m R e c t a n g l e                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import java.awt.*;

/**
 * Class <code>SystemRectangle</code> is a simple Rectangle that is meant to
 * represent a rectangle in a system, with its components (width and height)
 * specified in units, and its origin being the system top left corner.
 *
 * <p> This specialization is used to take benefit of compiler checks, to
 * prevent the use of rectangles with incorrect meaning or units. </p>
 *
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SystemRectangle
    extends Rectangle
{
    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // SystemRectangle //
    //-----------------//
    /**
     * Creates an instance of <code>SystemRectangle</code> with all items set to
     * zero.
     */
    public SystemRectangle ()
    {
    }

    //-----------------//
    // SystemRectangle //
    //-----------------//
    /**
     * Constructs a <code>SystemRectangle</code> and initializes it with the
     * specified data.
     *
     * @param x the specified x
     * @param y the specified y
     * @param width the specified width
     * @param height the specified height
     */
    public SystemRectangle (int x,
                            int y,
                            int width,
                            int height)
    {
        super(x, y, width, height);
    }
}
