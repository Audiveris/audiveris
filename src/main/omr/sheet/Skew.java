//----------------------------------------------------------------------------//
//                                                                            //
//                                  S k e w                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.sheet;


/**
 * Class <code>Skew</code> handles the skew angle of a given sheet picture, and
 * provides rotation methods for easy conversion of point coordinates.
 *
 * @see SkewBuilder
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Skew
    implements java.io.Serializable
{
    //~ Instance fields --------------------------------------------------------

    /** Skew angle as computed */
    private double angle;

    //~ Constructors -----------------------------------------------------------

    //     // To perform (un)rotations: sine and cosine of the skew angle
    //     private double sin;
    //     private double cos;

    //     // Shift of the origin due to image rotation
    //     private int shiftDx;
    //     private int shiftDy;

    //------//
    // Skew //
    //------//
    /**
     * This is meant to generate a skew entity, when its key informations (the
     * skew angle) is already known.
     *
     * @param angle the skew angle
     */
    public Skew (double angle)
    {
        this.angle = angle;

        // Computation of trigo values
        //         computeTrigo(sheet.getPicture().getOrigWidth(),
        //                      sheet.getPicture().getOrigHeight());
    }

    //~ Methods ----------------------------------------------------------------

    //-------//
    // angle //
    //-------//
    /**
     * Report the skew angle
     *
     * @return the angle value, expressed in radians
     */
    public double angle ()
    {
        return angle;
    }

    //--------------//
    // computeTrigo //
    //--------------//
    //     private void computeTrigo (int origwidth,
    //                                int origheight)
    //     {
    //         // Compute the trigonometric parameters (sine and cosine), as well as
    //         // the shift of origin.
    //         sin = Math.sin(angle);
    //         cos = Math.cos(angle);

    //         // Origin shift if rotated
    //         if (angle < 0) {
    //             shiftDx = -(int) Math.rint(origheight * sin);

    //             if (logger.isDebugEnabled()) {
    //                 logger.debug("shiftDx=" + shiftDx);
    //             }
    //         } else if (angle > 0) {
    //             shiftDy = (int) Math.rint(origwidth * sin);

    //             if (logger.isDebugEnabled()) {
    //                 logger.debug("shiftDy=" + shiftDy);
    //             }
    //         }
    //     }
}
