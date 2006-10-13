//----------------------------------------------------------------------------//
//                                                                            //
//                         U n i t R e c t a n g l e                          //
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
 * Class <code>UnitRectangle</code> is a simple Rectangle that is meant to
 * represent a rectangle in a sheet, with its components (width and height)
 * specified in units, so the name.
 *
 * <p> This specialization is used to take benefit of compiler checks, to
 * prevent the use of rectangles with incorrect meaning or units. </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class UnitRectangle
    extends Rectangle
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // UnitRectangle //
    //---------------//
    /**
     * Creates an instance of <code>UnitRectangle</code> with all items set to
     * zero.
     */
    public UnitRectangle ()
    {
    }

    //---------------//
    // UnitRectangle //
    //---------------//
    /**
     * Constructs a <code>UnitRectangle</code> and initializes it with the
     * specified data.
     *
     * @param x the specified x
     * @param y the specified y
     * @param width the specified width
     * @param height the specified height
     */
    public UnitRectangle (int x,
                          int y,
                          int width,
                          int height)
    {
        super(x, y, width, height);
    }
}
