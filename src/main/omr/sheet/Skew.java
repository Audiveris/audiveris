//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            S k e w                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code Skew} handles the skew angle of a given sheet picture.
 *
 * @author Hervé Bitteur
 */
public class Skew
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Skew.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Skew slope as measured. */
    @XmlAttribute(name = "slope")
    private final double slope;

    /** Corresponding angle (in radians) */
    private double angle;

    /** Transform to de-skew */
    private AffineTransform at;

    /** Width of de-skewed sheet */
    private double deskewedWidth;

    /** Height of de-skewed sheet */
    private double deskewedHeight;

    //~ Constructors -------------------------------------------------------------------------------
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

        initTransients(sheet);
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    public Skew ()
    {
        this.slope = 0;
        this.angle = 0;
        this.at = null;
        this.deskewedWidth = 0;
        this.deskewedHeight = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // deskewed //
    //----------//
    /**
     * Apply rotation OPPOSITE to the measured global angle and use the new sheet origin.
     *
     * @param point the initial (skewed) point
     * @return the de-skewed point
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

    //--------//
    // skewed //
    //--------//
    /**
     * Apply inverse of deskewed().
     *
     * @param point the initial (deskewed) point
     * @return the skewed point
     */
    public Point2D skewed (Point2D point)
    {
        try {
            return at.inverseTransform(point, null);
        } catch (NoninvertibleTransformException ex) {
            // Should never occur
            logger.error("NoninvertibleTransformException in Skew");

            return null;
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Skew angle=" + angle + "}";
    }

    //----------------//
    // initTransients //
    //----------------//
    private void initTransients (Sheet sheet)
    {
        angle = Math.atan(slope);

        // Rotation for deskew
        final double deskewAngle = -angle;
        at = AffineTransform.getRotateInstance(deskewAngle);

        // Origin translation for deskew
        final int w = sheet.getWidth();
        final int h = sheet.getHeight();
        final Point2D topRight = at.transform(new Point2D.Double(w, 0), null);
        final Point2D bottomLeft = at.transform(new Point2D.Double(0, h), null);
        final Point2D bottomRight = at.transform(new Point2D.Double(w, h), null);
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

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled for this object,
     * but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        initTransients((Sheet) parent);
    }
}
