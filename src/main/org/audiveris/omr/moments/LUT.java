//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             L U T                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
 * Interface {@code LUT} defines a lookup table.
 *
 * @author Hervé Bitteur
 */
public interface LUT
{

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
     * Check whether the provided coordinates lies within the LUT range ([0, SIZE[).
     *
     * @param x provided abscissa
     * @param y provided ordinate
     * @return true if OK
     */
    boolean contains (double x,
                      double y);

    /**
     * Report the LUT radius, since LUT implements (-radius,+radius).
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
     * Report the value for precise point (px,py) by interpolation of values defined
     * for integer coordinates.
     *
     * @param px precise abscissa
     * @param py precise ordinate
     * @return the interpolated value
     */
    double interpolate (double px,
                        double py);
}
