//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            B a s i c L e g e n d r e M o m e n t s                             //
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

import java.util.Locale;

/**
 * Class {@code BasicLegendreMoments} implements a descriptor for orthogonal Legendre
 * moments.
 *
 * @author Hervé Bitteur
 */
public class BasicLegendreMoments
        implements LegendreMoments
{

    /** Resulting moments. */
    protected double[][] moments = new double[ORDER + 1][ORDER + 1];

    /**
     * Creates a new BasicLegendreMoments object.
     */
    public BasicLegendreMoments ()
    {
    }

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
                    distance += Math.abs(that.getMoment(m, n) - getMoment(m, n));
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
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        for (int m = 0; m <= ORDER; m++) {
            for (int n = 0; n <= ORDER; n++) {
                if ((m + n) <= ORDER) {
                    if (sb.length() > 1) {
                        sb.append(" ");
                    }

                    sb.append(String.format(Locale.US, "%04.0f", 1000 * getMoment(m, n)));
                }
            }
        }

        sb.append("}");

        return sb.toString();
    }
}
