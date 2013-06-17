//----------------------------------------------------------------------------//
//                                                                            //
//                   Q u a n t i z e d A R T M o m e n t s                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.moments;

import java.util.Arrays;

/**
 * Class {@code QuantizedARTMoments} handles a quantized region-based
 * shape descriptor.
 *
 * See MPEG-7 Experimentation Model for the original C++ code
 *
 * @author Hervé Bitteur
 */
public class QuantizedARTMoments
        implements ARTMoments
{
    //~ Static fields/initializers ---------------------------------------------

    // Quantization table (17 cells)
    private static double[] quantTable = {
        0.000000000, 0.003585473,
        0.007418411, 0.011535520,
        0.015982337, 0.020816302,
        0.026111312, 0.031964674,
        0.038508176, 0.045926586,
        0.054490513, 0.064619488,
        0.077016351, 0.092998687,
        0.115524524, 0.154032694,
        1.000000000
    };

    // Inverse quantization table (16 cells)
    private static double[] iQuantTable = {
        0.001763817, 0.005468893,
        0.009438835, 0.013714449,
        0.018346760, 0.023400748,
        0.028960940, 0.035140141,
        0.042093649, 0.050043696,
        0.059324478, 0.070472849,
        0.084434761, 0.103127662,
        0.131506859, 0.192540857
    };

    //~ Instance fields --------------------------------------------------------
    // double QuantizedARTMoments::QuantTable[17] = {0.000000000, 0.001898192, 0.003927394, 0.006107040, 0.008461237, 0.011020396, 0.013823636, 0.016922475, 0.020386682, 0.024314076, 0.028847919, 0.034210318, 0.040773364, 0.049234601, 0.061160045, 0.081546727, 1.0};
    // double QuantizedARTMoments::IQuantTable[16] = {0.000933785, 0.002895296, 0.004997030, 0.007260591, 0.009712991, 0.012388631, 0.015332262, 0.018603605, 0.022284874, 0.026493722, 0.031407077, 0.037309157, 0.044700757, 0.054597000, 0.069621283, 0.101933409};
    //
    /** Actual values */
    private final short[][] values = new short[ANGULAR][RADIAL];

    //~ Constructors -----------------------------------------------------------
    //---------------------//
    // QuantizedARTMoments //
    //---------------------//
    /**
     * Creates a new QuantizedARTMoments object.
     */
    public QuantizedARTMoments ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getModule //
    //-----------//
    @Override
    public final double getModule (int p,
                                   int r)
    {
        return iQuantTable[values[p][r]];
    }

    //-----------//
    // setModule //
    //-----------//
    @Override
    public final void setModule (int p,
                                 int r,
                                 double value)
    {
        /*
         * index of the search key, if it is contained in the array; otherwise,
         * (-(insertion point) - 1). The insertion point is defined as the point
         * at which the key would be inserted into the array: the index of the
         * first element greater than the key, or a.length if all elements in
         * the array are less than the specified key. Note that this guarantees
         * that the return value will be >= 0 if and only if the key is found.
         */
        int idx = Arrays.binarySearch(quantTable, value);

        if (idx < 0) {
            idx = -idx - 2;
        }

        values[p][r] = (short) idx;
    }

    //------------//
    // distanceTo //
    //------------//
    @Override
    public double distanceTo (ARTMoments that)
    {
        double distance = 0;

        for (int p = 0; p < ANGULAR; p++) {
            for (int r = 0; r < RADIAL; r++) {
                if ((p != 0) || (r != 0)) {
                    distance += Math.abs(
                            that.getModule(p, r) - getModule(p, r));
                }
            }
        }

        return distance;
    }

    @Override
    public double getArgument (int p,
                               int r)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getImag (int p,
                           int r)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getMoment (int m,
                             int n)
    {
        return getModule(m, n);
    }

    @Override
    public double getReal (int p,
                           int r)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setArgument (int p,
                             int r,
                             double value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setImag (int p,
                         int r,
                         double value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setMoment (int m,
                           int n,
                           double value)
    {
        setModule(m, n, value);
    }

    @Override
    public void setReal (int p,
                         int r,
                         double value)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");

        for (int p = 0; p < ANGULAR; p++) {
            for (int r = 0; r < RADIAL; r++) {
                if ((p != 0) || (r != 0)) {
                    if (sb.length() > 1) {
                        sb.append(" ");
                    }

                    sb.append(values[p][r]);
                }
            }
        }

        sb.append("}");

        return sb.toString();
    }
}
