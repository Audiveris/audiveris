//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   D y n a m i c S e c t i o n                                  //
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
package org.audiveris.omr.lag;

import org.audiveris.omr.math.Line;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.Run;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

/**
 * Class {@code DynamicSection} is a section that can evolve by adding runs or
 * translating its location.
 *
 * @author Hervé Bitteur
 */
public class DynamicSection
        extends BasicSection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DynamicSection.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code DynamicSection} object.
     *
     * @param orientation provided orientation for the section
     */
    public DynamicSection (Orientation orientation)
    {
        super(orientation);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // append //
    //--------//
    /**
     * Extend a section with the given run.
     * This new run is assumed to be contiguous to the current last run of the section,
     * no check is performed.
     *
     * @param run the new last run
     */
    public void append (Run run)
    {
        run = new Run(run);
        runs.add(run);
        addRun(run);

        logger.debug("Appended {} to {}", run, this);
    }

    //-------------//
    // getCentroid //
    //-------------//
    @Override
    public Point getCentroid ()
    {
        if (centroid == null) {
            centroid = computeCentroid();
        }

        return centroid;
    }

    //-------------------//
    // getOrientedBounds //
    //-------------------//
    @Override
    public Rectangle getOrientedBounds ()
    {
        if (orientedBounds == null) {
            orientedBounds = orientation.oriented(getBounds());
        }

        return orientedBounds;
    }

    //-----------------//
    // getOrientedLine //
    //-----------------//
    @Override
    public Line getOrientedLine ()
    {
        if ((orientedLine == null) && (getWeight() > 1)) {
            orientedLine = computeOrientedLine();
        }

        return orientedLine;
    }

    //------------//
    // getPolygon //
    //------------//
    @Override
    public Polygon getPolygon ()
    {
        if (polygon == null) {
            polygon = computePolygon();
        }

        return polygon;
    }

    //-----------//
    // getWeight //
    //-----------//
    @Override
    public int getWeight ()
    {
        if (weight == 0) {
            computeParameters();
        }

        return weight;
    }

    //---------//
    // prepend //
    //---------//
    /**
     * Add a run at the beginning rather than at the end of the section.
     *
     * @param run the new first run
     */
    public void prepend (Run run)
    {
        run = new Run(run);
        logger.debug("Prepending {} to {}", run, this);

        firstPos--;
        runs.add(0, run);
        addRun(run);

        logger.debug("Prepended {}", this);
    }

    //-------------//
    // setFirstPos //
    //-------------//
    /**
     * Set the position of the first run of the section.
     *
     * @param firstPos position of the first run, abscissa for a vertical run,
     *                 ordinate for a horizontal run.
     */
    public void setFirstPos (int firstPos)
    {
        this.firstPos = firstPos;
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply an absolute translation vector to this section.
     *
     * @param vector the translation vector
     */
    public void translate (Point vector)
    {
        // Get the coord/pos equivalent of dx/dy vector
        Point cp = orientation.oriented(vector);
        int dc = cp.x;
        int dp = cp.y;

        // Apply the needed modifications
        firstPos += dp;

        for (Run run : runs) {
            run.translate(dc);
        }

        // Force update
        invalidateCache();
    }

    //--------//
    // addRun //
    //--------//
    /**
     * Compute incrementally the cached parameters.
     *
     * @param run the run to be processed
     */
    protected void addRun (Run run)
    {
        // Invalidate cached data
        invalidateCache();

        // Compute contribution of this run
        computeRunContribution(run);
    }

    //-----------------//
    // computeCentroid //
    //-----------------//
    protected Point computeCentroid ()
    {
        Point orientedPoint = new Point(0, 0);
        int y = firstPos;

        for (Run run : runs) {
            final int length = run.getLength();
            orientedPoint.y += (length * (2 * y));
            orientedPoint.x += (length * ((2 * run.getStart()) + length));
            y++;
        }

        orientedPoint.x /= (2 * getWeight());
        orientedPoint.y /= (2 * getWeight());

        return orientation.absolute(orientedPoint);
    }

    //-------------------//
    // computeParameters //
    //-------------------//
    protected void computeParameters ()
    {
        // weight & maxRunLength
        weight = 0;
        maxRunLength = 0;

        // maxRunLength
        for (Run run : runs) {
            computeRunContribution(run);
        }

        // Invalidate cached data
        invalidateCache();

        logger.debug(
                "Parameters of {} maxRunLength={} meanRunLength={}" + " weight={}",
                this,
                maxRunLength,
                getMeanRunLength(),
                weight);
    }

    //----------------//
    // computePolygon //
    //----------------//
    /**
     * Compute the arrays of points needed to draw the section runs.
     * This is an absolute definition.
     *
     * @return the created polygon that represents the section geometry
     */
    protected Polygon computePolygon ()
    {
        final int maxNb = 1 + (4 * getRunCount()); // Upper value
        final int[] xx = new int[maxNb];
        final int[] yy = new int[maxNb];
        int idx = 0; // Current filling index in xx & yy arrays

        if (isVertical()) {
            idx = populatePolygon(yy, xx, idx, 1);
            idx = populatePolygon(yy, xx, idx, -1);
        } else {
            idx = populatePolygon(xx, yy, idx, 1);
            idx = populatePolygon(xx, yy, idx, -1);
        }

        Polygon poly = new Polygon(xx, yy, idx);

        return poly;
    }

    //------------------------//
    // computeRunContribution //
    //------------------------//
    protected void computeRunContribution (Run run)
    {
        final int length = run.getLength();
        weight += length;
        maxRunLength = Math.max(maxRunLength, length);
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    protected void invalidateCache ()
    {
        orientedBounds = null;
        centroid = null;
        polygon = null;
        orientedLine = null;
    }

    //-----------------//
    // populatePolygon //
    //-----------------//
    /**
     * Compute the arrays of points needed to draw the section runs
     *
     * @param xpoints to receive abscissae
     * @param ypoints to receive coordinates
     * @param dir     direction for browsing runs
     * @param index   first index available in arrays
     * @return last index value
     */
    protected int populatePolygon (int[] xpoints,
                                   int[] ypoints,
                                   int index,
                                   int dir)
    {
        // Precise delimitating points
        int runNb = getRunCount();
        int iStart = (dir > 0) ? 0 : (runNb - 1);
        int iBreak = (dir > 0) ? runNb : (-1);
        int y = (dir > 0) ? getFirstPos() : (getFirstPos() + runNb);
        int xPrev = -1;

        for (int i = iStart; i != iBreak; i += dir) {
            Run run = runs.get(i);

            // +----------------------------+
            // +--+-------------------------+
            //    +----------------------+--+
            //    +----------------------+
            //
            // Order of the 4 angle points for a run is
            // Vertical lag:    Horizontal lag:
            //     1 2              1 4
            //     4 3              2 3
            int x = (dir > 0) ? run.getStart() : (run.getStop() + 1);

            if (x != xPrev) {
                if (xPrev != -1) {
                    // Insert last vertex
                    xpoints[index] = xPrev;
                    ypoints[index] = y;
                    index++;
                }

                // Insert new vertex
                xpoints[index] = x;
                ypoints[index] = y;
                index++;
                xPrev = x;
            }

            y += dir;
        }

        // Complete the sequence, with a new vertex
        xpoints[index] = xPrev;
        ypoints[index] = y;
        index++;

        if (dir < 0) {
            // Finish with starting point
            xpoints[index] = runs.get(0).getStart();
            ypoints[index] = getFirstPos();
            index++;
        }

        return index;
    }
}
