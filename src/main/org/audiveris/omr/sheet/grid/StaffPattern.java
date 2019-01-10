//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S t a f f P a t t e r n                                    //
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
package org.audiveris.omr.sheet.grid;

import ij.process.ByteProcessor;

import java.awt.geom.Point2D;

/**
 * Class {@code StaffPattern} is used to retrieve the vertical position that best fits
 * the lines of a staff.
 *
 * @author Hervé Bitteur
 */
public class StaffPattern
{

    /** The number of lines expected in staff. */
    private final int count;

    /** The width of probe used to measure fit. */
    private final int width;

    /** The typical line thickness. */
    private final int line;

    /** The precise interline value. */
    private final double interline;

    /**
     * Creates a new {@code StaffPattern} object.
     *
     * @param count     number of lines
     * @param width     width of probe
     * @param line      typical line thickness
     * @param interline precise interline value
     */
    public StaffPattern (int count,
                         int width,
                         int line,
                         double interline)
    {
        this.count = count;
        this.width = width;
        this.line = line;
        this.interline = interline;
    }

    //----------//
    // evaluate //
    //----------//
    /**
     * Measure how well the pattern fits the pixel buffer at upper left location.
     *
     * @param location location of upper left corner of pattern
     * @param buffer   the image buffer to read foreground pixels from
     * @return the ratio of foreground pixels matched by the pattern
     */
    public double evaluate (Point2D location,
                            ByteProcessor buffer)
    {
        final int xMin = (int) Math.rint(location.getX());
        int trials = 0;
        int matches = 0;

        for (int index = 0; index < count; index++) {
            final double yMid = location.getY() + (index * interline);
            final int yMin = (int) Math.ceil(yMid - (line / 2.0));
            final int yMax = (int) Math.floor(yMid + (line / 2.0));

            for (int y = yMin; y <= yMax; y++) {
                for (int x = xMin; x < (xMin + width); x++) {
                    trials++;

                    if (0 == buffer.get(x, y)) {
                        matches++;
                    }
                }
            }
        }

        double ratio = (double) matches / trials;

        return ratio;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append(" count:").append(count);
        sb.append(" width:").append(width);
        sb.append(" line:").append(line);
        sb.append(String.format(" interline:%.2f", interline));
        sb.append("}");

        return sb.toString();
    }
}
