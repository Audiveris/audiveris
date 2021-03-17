//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            S k e w                                             //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Objects;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
    //
    // Persistent data
    //----------------
    //
    /** Skew slope as measured. */
    @XmlAttribute(name = "slope")
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double5Adapter.class)
    private final double slope;

    // Transient data
    //---------------
    //
    /** Corresponding angle (in radians). */
    private double angle;

    /** Transform to de-skew. */
    private AffineTransform at;

    /** Width of de-skewed sheet. */
    private double deskewedWidth;

    /** Height of de-skewed sheet. */
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
        Objects.requireNonNull(point, "Null point argument");

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
}
