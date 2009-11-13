//----------------------------------------------------------------------------//
//                                                                            //
//                       S y s t e m R e c t a n g l e                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.common;

import java.awt.Rectangle;

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
    extends SimpleRectangle
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

    //-----------------//
    // SystemRectangle //
    //-----------------//
    /**
     * Constructs a <code>SystemRectangle</code> and initializes it with just
     * the upper left point
     *
     * @param sysPt the upper left point in its containing system
     */
    public SystemRectangle (SystemPoint sysPt)
    {
        super(sysPt.x, sysPt.y, 0, 0);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the center of the system rectangle
     * @return the rectangle center
     */
    public SystemPoint getCenter ()
    {
        return new SystemPoint(x + (width / 2), y + (height / 2));
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * Report the upper left corner of the system rectangle
     * @return the upper left corner
     */
    @Override
    public SystemPoint getLocation ()
    {
        return new SystemPoint(x, y);
    }

    //-------//
    // union //
    //-------//
    /**
     * Report the result of the union of this SystemRectangle with another one
     * @param rect the other SystemRectangle
     * @return the union
     */
    public SystemRectangle union (SystemRectangle rect)
    {
        Rectangle r = super.union(rect);

        return new SystemRectangle(r.x, r.y, r.width, r.height);
    }
}
