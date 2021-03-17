//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               O r t h o g o n a l M o m e n t s                                //
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
package org.audiveris.omr.moments;

/**
 * Interface {@code OrthogonalMoments} is a general definition for a descriptor of
 * orthogonal moments.
 *
 * @param <D> the descriptor type
 * @author Hervé Bitteur
 */
public interface OrthogonalMoments<D extends OrthogonalMoments<D>>
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the distance to another descriptor instance.
     *
     * @param that the other instance
     * @return the measured distance
     */
    double distanceTo (D that);

    /**
     * Report the moment for m and n orders.
     *
     * @param m m order
     * @param n n order
     * @return moments(m, n)
     */
    double getMoment (int m,
                      int n);

    /**
     * Assign the moment for m and n orders.
     *
     * @param m     m order
     * @param n     n order
     * @param value the moment value
     */
    void setMoment (int m,
                    int n,
                    double value);

    //    /**
    //     * Report a label for the (m,n) moment.
    //     * @param m m order
    //     * @param n n order
    //     * @return label for (m, n)
    //     */
    //    String getLabel (int m,
    //                     int n);
}
