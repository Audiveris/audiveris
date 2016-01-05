//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B a s i c R o i                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.math.Histogram;

import omr.run.Orientation;
import omr.run.Run;
import omr.run.RunTable;

import java.awt.Rectangle;
import java.util.Iterator;

/**
 * Class {@code BasicRoi} implements an Roi
 *
 * @author Hervé Bitteur
 */
public class BasicRoi
        implements Roi
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Region of interest with absolute coordinates */
    final Rectangle absContour;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Define a region of interest
     *
     * @param absoluteContour the absolute contour of the region of interest,
     *                        specified in the usual (x, y, width, height) form.
     */
    public BasicRoi (Rectangle absoluteContour)
    {
        this.absContour = absoluteContour;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------------//
    // getAbsoluteContour //
    //--------------------//
    @Override
    public Rectangle getAbsoluteContour ()
    {
        return new Rectangle(absContour);
    }

    //-----------------//
    // getRunHistogram //
    //-----------------//
    @Override
    public Histogram<Integer> getRunHistogram (Orientation projection,
                                               RunTable table)
    {
        final Orientation tableOrient = table.getOrientation();
        final boolean alongTheRuns = projection == tableOrient;
        final Histogram<Integer> histo = new Histogram<Integer>();
        final Rectangle tableContour = new Rectangle(table.getDimension());
        final Rectangle inter = new Rectangle(absContour.intersection(tableContour));
        final Rectangle oriInter = tableOrient.oriented(inter);
        final int minPos = oriInter.y;
        final int maxPos = (oriInter.y + oriInter.height) - 1;
        final int minCoord = oriInter.x;
        final int maxCoord = (oriInter.x + oriInter.width) - 1;

        for (int pos = minPos; pos <= maxPos; pos++) {
            for (Iterator<Run> it = table.iterator(pos); it.hasNext();) {
                Run run = it.next();
                final int cMin = Math.max(minCoord, run.getStart());
                final int cMax = Math.min(maxCoord, run.getStop());

                // Clipping on coord
                if (cMin <= cMax) {
                    if (alongTheRuns) {
                        // Along the runs
                        histo.increaseCount(pos, cMax - cMin + 1);
                    } else {
                        // Across the runs
                        for (int i = cMin; i <= cMax; i++) {
                            histo.increaseCount(i, 1);
                        }
                    }
                }
            }
        }

        return histo;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "Roi " + getAbsoluteContour();
    }
}
