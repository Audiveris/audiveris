//----------------------------------------------------------------------------//
//                                                                            //
//                        P i x e l R e c t a n g l e                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.common;

import java.awt.Rectangle;

/**
 * Class {@code PixelRectangle} is a simple Rectangle that is meant to
 * represent an absolute rectangle in a sheet, with its components specified in
 * pixels, so the name.
 *
 * <p>Oriented rectangles (those which depend on orientation) should not use
 * PixelRectangle but plain generic {@link Rectangle} class.</p>
 *
 * <p> This specialization is used to take benefit of compiler checks, to
 * prevent the use of rectangles with incorrect meaning. </p>
 *
 * @author Hervé Bitteur
 */
public class PixelRectangle
    extends SimpleRectangle
{
    //~ Constructors -----------------------------------------------------------

    //----------------//
    // PixelRectangle //
    //----------------//
    /**
     * Creates an instance of {@code PixelRectangle} with all data set to
     * zero.
     */
    public PixelRectangle ()
    {
    }

    //----------------//
    // PixelRectangle //
    //----------------//
    /**
     * Construct a {@code PixelRectangle} and initialize it with the
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
     * Construct a {@code PixelRectangle} and initialize it with the
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
     * Construct a {@code PixelRectangle} with just a PixelPoint
     *
     * @param pt the specified point
     */
    public PixelRectangle (PixelPoint pt)
    {
        super(pt.x, pt.y, 0, 0);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getCenter //
    //-----------//
    public PixelPoint getCenter ()
    {
        return new PixelPoint(x + (width / 2), y + (height / 2));
    }

    //-------------//
    // getLocation //
    //-------------//
    @Override
    public PixelPoint getLocation ()
    {
        return new PixelPoint(x, y);
    }

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
