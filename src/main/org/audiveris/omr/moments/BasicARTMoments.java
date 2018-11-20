//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 B a s i c A R T M o m e n t s                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.moments;

/**
 * Class {@code BasicARTMoments} implements a basic region-based shape descriptor.
 * <p>
 * See MPEG-7 Experimentation Model for the original C++ code
 *
 * @author Hervé Bitteur
 */
public class BasicARTMoments
        implements ARTMoments
{

    /** Module values */
    private final double[][] modules = new double[ANGULAR][RADIAL];

    /** Argument values */
    private final double[][] arguments = new double[ANGULAR][RADIAL];

    /** Imaginary values */
    private final double[][] imags = new double[ANGULAR][RADIAL];

    /** Real values */
    private final double[][] reals = new double[ANGULAR][RADIAL];

    /**
     * Creates a new BasicARTMoments object.
     */
    public BasicARTMoments ()
    {
    }

    //------------//
    // distanceTo //
    //------------//
    /**
     * Implements a Manhattan distance
     *
     * @param that the other ARTMoments descriptor
     * @return the (Manhattan) distance
     */
    @Override
    public double distanceTo (ARTMoments that)
    {
        double distance = 0;

        for (int p = 0; p < ANGULAR; p++) {
            for (int r = 0; r < RADIAL; r++) {
                if ((p != 0) || (r != 0)) {
                    distance += Math.abs(that.getModule(p, r) - getModule(p, r));

                    //                    distance += Math.abs(that.getReal(p, r) - getReal(p, r));
                    //                    distance += Math.abs(that.getImag(p, r) - getImag(p, r));
                }
            }
        }

        return distance;
    }

    //-------------//
    // getArgument //
    //-------------//
    @Override
    public double getArgument (int p,
                               int r)
    {
        return arguments[p][r];
    }

    @Override
    public double getImag (int p,
                           int r)
    {
        return imags[p][r];
    }

    //-----------//
    // getModule //
    //-----------//
    @Override
    public final double getModule (int p,
                                   int r)
    {
        return modules[p][r];
    }

    //-----------//
    // getMoment //
    //-----------//
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
        return reals[p][r];
    }

    //-------------//
    // setArgument //
    //-------------//
    @Override
    public void setArgument (int p,
                             int r,
                             double value)
    {
        arguments[p][r] = value;
    }

    @Override
    public void setImag (int p,
                         int r,
                         double value)
    {
        imags[p][r] = value;
    }

    //-----------//
    // setModule //
    //-----------//
    @Override
    public final void setModule (int p,
                                 int r,
                                 double value)
    {
        modules[p][r] = value;
    }

    //-----------//
    // setMoment //
    //-----------//
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
        reals[p][r] = value;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        for (int p = 0; p < ANGULAR; p++) {
            for (int r = 0; r < RADIAL; r++) {
                if ((p != 0) || (r != 0)) {
                    if (sb.length() > 1) {
                        sb.append(" ");
                    }

                    sb.append(String.format("%3.0f", 1_000 * getMoment(p, r)));
                }
            }
        }

        sb.append("}");

        return sb.toString();
    }
}
