//----------------------------------------------------------------------------//
//                                                                            //
//                        P i x e l R e c t a n g l e                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.common;

import java.awt.Rectangle;

/**
 * Class <code>PixelRectangle</code> is a simple Rectangle that is meant to
 * represent a rectangle in a sheet, with its components specified in pixels, so
 * the name.
 *
 * <p> This specialization is used to take benefit of compiler checks, to
 * prevent the use of rectangles with incorrect meaning or units. </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class PixelRectangle
    extends SimpleRectangle
{
    //~ Constructors -----------------------------------------------------------

    //----------------//
    // PixelRectangle //
    //----------------//
    /**
     * Creates an instance of <code>PixelRectangle</code> with all data set to
     * zero.
     */
    public PixelRectangle ()
    {
    }

    //----------------//
    // PixelRectangle //
    //----------------//
    /**
     * Construct a <code>PixelRectangle</code> and initialize it with the
     * specified data
     *
     * @param x the specified x
     * @param y the specified y
     * @param width the specified width
     * @param height the specified height
     */
    public PixelRectangle (int x,
                           int y,
                           int width,
                           int height)
    {
        super(x, y, width, height);
    }

    //----------------//
    // PixelRectangle //
    //----------------//
    /**
     * Construct a <code>PixelRectangle</code> and initialize it with the
     * specified generic rectangle
     *
     * @param rect the specified generic rectangle
     */
    public PixelRectangle (Rectangle rect)
    {
        super(rect.x, rect.y, rect.width, rect.height);
    }

    //----------------//
    // PixelRectangle //
    //----------------//
    /**
     * Construct a <code>PixelRectangle</code> with just a PixelPoint
     *
     * @param pt the specified point
     */
    public PixelRectangle (PixelPoint pt)
    {
        super(pt.x, pt.y, 0, 0);
    }

    //~ Methods ----------------------------------------------------------------

    //-------//
    // union //
    //-------//
    /**
     * Union with another PixelRectangle
     *
     * @param other another PixelRectangle
     * @return the union of these 2 rectangles
     */
    public PixelRectangle union (PixelRectangle other)
    {
        Rectangle r = super.union(other);

        return new PixelRectangle(r.x, r.y, r.width, r.height);
    }
}
