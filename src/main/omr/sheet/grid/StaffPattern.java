//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S t a f f P a t t e r n                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

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
    //~ Instance fields ----------------------------------------------------------------------------

    /** The number of lines expected in staff. */
    private final int count;

    /** The width of probe used to measure fit. */
    private final int width;

    /** The typical line thickness. */
    private final int line;

    /** The precise interline value. */
    private final double interline;

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
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
        int trials = 0;
        int matches = 0;

        for (int index = 0; index < count; index++) {
            double yMid = location.getY() + (index * interline);
            int yMin = (int) Math.ceil(yMid - (line / 2.0));
            int yMax = (int) Math.floor(yMid + (line / 2.0));

            for (int y = yMin; y <= yMax; y++) {
                int xMin = (int) Math.rint(location.getX());

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
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());
        sb.append(" count:").append(count);
        sb.append(" width:").append(width);
        sb.append(" line:").append(line);
        sb.append(String.format(" interline:%.2f", interline));
        sb.append("}");

        return sb.toString();
    }
}
