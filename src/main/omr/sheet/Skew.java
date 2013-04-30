//----------------------------------------------------------------------------//
//                                                                            //
//                                  S k e w                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Class {@code Skew} handles the skew angle of a given sheet picture.
 *
 * @author Hervé Bitteur
 */
public class Skew
{
    //~ Instance fields --------------------------------------------------------

    /** Skew slope as measured */
    private final double slope;

    /** Corresponding angle (in radians) */
    private final double angle;

    /** Transform to deskew */
    private final AffineTransform at;

    /** Width of deskewed sheet */
    private final double deskewedWidth;

    /** Height of deskewed sheet */
    private final double deskewedHeight;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new Skew object.
     *
     * @param slope the sheet global slope
     * @param sheet the related sheet
     */
    public Skew (double slope,
                 Sheet sheet)
    {
        this.slope = slope;

        angle = Math.atan(slope);

        // Rotation for deskew
        double deskewAngle = -angle;
        at = AffineTransform.getRotateInstance(deskewAngle);

        // Origin translation for deskew
        int w = sheet.getWidth();
        int h = sheet.getHeight();
        Point2D topRight = at.transform(new Point2D.Double(w, 0), null);
        Point2D bottomLeft = at.transform(new Point2D.Double(0, h), null);
        Point2D bottomRight = at.transform(new Point2D.Double(w, h), null);
        double dx = 0;
        double dy = 0;

        if (deskewAngle <= 0) { // Counter-clockwise deskew
            deskewedWidth = bottomRight.getX();
            dy = -topRight.getY();
            deskewedHeight = bottomLeft.getY() + dy;
        } else { // Clockwise deskew
            dx = -bottomLeft.getX();
            deskewedWidth = topRight.getX() + dx;
            deskewedHeight = bottomRight.getY();
        }

        at.translate(dx, dy);
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // deskewed //
    //----------//
    /**
     * Apply rotation OPPOSITE to the measured global angle and use the
     * new sheet origin.
     *
     * @param point the initial (skewed) point
     * @return the deskewed point
     */
    public Point2D deskewed (Point2D point)
    {
        return at.transform(point, null);
    }

    //----------//
    // getAngle //
    //----------//
    /**
     * Report the skew angle.
     *
     * @return the angle value, expressed in radians
     */
    public double getAngle ()
    {
        return angle;
    }

    //-------------------//
    // getDeskewedHeight //
    //-------------------//
    /**
     * @return the deskewedHeight
     */
    public double getDeskewedHeight ()
    {
        return deskewedHeight;
    }

    //------------------//
    // getDeskewedWidth //
    //------------------//
    /**
     * @return the deskewedWidth
     */
    public double getDeskewedWidth ()
    {
        return deskewedWidth;
    }

    //----------//
    // getSlope //
    //----------//
    /**
     * @return the slope (tangent of angle)
     */
    public double getSlope ()
    {
        return slope;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Skew angle=" + angle + "}";
    }
}
