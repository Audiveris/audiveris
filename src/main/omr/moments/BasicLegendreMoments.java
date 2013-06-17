//----------------------------------------------------------------------------//
//                                                                            //
//                  B a s i c L e g e n d r e M o m e n t s                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.moments;

import java.util.Locale;

/**
 * Class {@code BasicLegendreMoments} implements a descriptor for
 * orthogonal Legendre moments.
 *
 * @author Hervé Bitteur
 */
public class BasicLegendreMoments
        implements LegendreMoments
{
    //~ Instance fields --------------------------------------------------------

    /** Resulting moments. */
    protected double[][] moments = new double[ORDER + 1][ORDER + 1];

    //~ Constructors -----------------------------------------------------------
    //----------------------//
    // BasicLegendreMoments //
    //----------------------//
    /**
     * Creates a new BasicLegendreMoments object.
     */
    public BasicLegendreMoments ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // distanceTo //
    //------------//
    @Override
    public double distanceTo (LegendreMoments that)
    {
        double distance = 0;

        for (int m = 0; m <= ORDER; m++) {
            for (int n = 0; n <= ORDER; n++) {
                if ((m + n) <= ORDER) {
                    distance += Math.abs(
                            that.getMoment(m, n) - getMoment(m, n));
                }
            }
        }

        return distance;
    }

    //-----------//
    // getMoment //
    //-----------//
    @Override
    public double getMoment (int m,
                             int n)
    {
        return moments[m][n];
    }

    //-----------//
    // setMoment //
    //-----------//
    @Override
    public void setMoment (int m,
                           int n,
                           double value)
    {
        moments[m][n] = value;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");

        for (int m = 0; m <= ORDER; m++) {
            for (int n = 0; n <= ORDER; n++) {
                if ((m + n) <= ORDER) {
                    if (sb.length() > 1) {
                        sb.append(" ");
                    }

                    sb.append(
                            String.format(
                            Locale.US,
                            "%04.0f",
                            1000 * getMoment(m, n)));
                }
            }
        }

        sb.append("}");

        return sb.toString();
    }
}
