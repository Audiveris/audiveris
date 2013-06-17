//----------------------------------------------------------------------------//
//                                                                            //
//                                   L U T                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.moments;

/**
 * Interface {@code LUT} defines a lookup table.
 *
 * @author Hervé Bitteur
 */
public interface LUT
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Set the value for integer coordinates (x,y).
     *
     * @param x     integer abscissa
     * @param y     integer ordinate
     * @param value the known value for (x,y) point
     */
    void assign (int x,
                 int y,
                 double value);

    /**
     * Check whether the provided radius lies within the LUT.
     *
     * @param radius the radius to check
     * @return true if OK
     */
    boolean contains (double radius);

    /**
     * Check whether the provided coordinates lies within the LUT
     * range ([0, SIZE[).
     *
     * @param x provided abscissa
     * @param y provided ordinate
     * @return true if OK
     */
    boolean contains (double x,
                      double y);

    /**
     * Report the LUT radius (LUT implements (-radius,+radius).
     *
     * @return the defined radius
     */
    int getRadius ();

    /**
     * Report the LUT size (typically 2*radius +1).
     *
     * @return the LUT size
     */
    int getSize ();

    /**
     * Report the value for precise point (px,yy) by interpolation
     * of values defined for integer coordinates.
     *
     * @param px precise abscissa
     * @param py precise ordinate
     * @return the interpolated value
     */
    double interpolate (double px,
                        double py);
}
