//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B a r C o n n e c t i o n                                   //
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
package org.audiveris.omr.sheet.grid;

import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.sig.GradeImpacts;

import java.awt.geom.Area;
import java.awt.geom.Line2D;

/**
 * Class {@code BarConnection} represents a concrete connection across staves between
 * two barlines.
 * <p>
 * It indicates that the two linked staves belong to the same system and/or part.
 *
 * @author Hervé Bitteur
 */
public class BarConnection
        extends BarAlignment
{

    /** Physical portion of the connection line, excluding portions within staves. */
    private Area area;

    /** Rather vertical median line. */
    private Line2D median;

    /**
     * Creates a new BarConnection object.
     *
     * @param align       the underlying barline alignment
     * @param gapImpact   impact of gap
     * @param whiteImpact impact of white ratio
     */
    public BarConnection (BarAlignment align,
                          double gapImpact,
                          double whiteImpact)
    {
        super(
                align.topPeak,
                align.bottomPeak,
                align.slope,
                align.dWidth,
                new Impacts(
                        ((BarAlignment.Impacts) align.getImpacts()).getAlignImpact(),
                        ((BarAlignment.Impacts) align.getImpacts()).getWidthImpact(),
                        gapImpact,
                        whiteImpact));
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof BarConnection) {
            return super.equals(obj);
        }

        return false;
    }

    //---------//
    // getArea //
    //---------//
    /**
     * Report the underlying area.
     *
     * @return connector area
     */
    public Area getArea ()
    {
        if (area == null) {
            area = AreaUtil.verticalRibbon(getMedian(), getWidth());
        }

        return area;
    }

    //------------/
    // getMedian //
    //------------/
    /**
     * Report the defining line of this connection.
     *
     * @return connection line
     */
    public Line2D getMedian ()
    {
        if (median == null) {
            double xTop = (topPeak.getStart() + topPeak.getStop()) / 2d;
            double xBot = (bottomPeak.getStart() + bottomPeak.getStop()) / 2d;
            median = new Line2D.Double(xTop, topPeak.getBottom(), xBot, bottomPeak.getTop());
        }

        return median;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the average with of the connector.
     *
     * @return width in pixels
     */
    public double getWidth ()
    {
        return (topPeak.getWidth() + bottomPeak.getWidth()) / 2d;
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        return super.hashCode();
    }

    //---------//
    // Impacts //
    //---------//
    /**
     * Evaluation details as a connector.
     */
    public static class Impacts
            extends GradeImpacts
    {

        private static final String[] NAMES = new String[]{"align", "dWidth", "gap", "white"};

        private static final double[] WEIGHTS = new double[]{2, 1, 2, 2};

        /**
         * Create Impacts.
         *
         * @param align  impact for alignment
         * @param dWidth impact for width consistency
         * @param gap    impact for maximum vertical gap
         * @param white  impact for ratio of white
         */
        public Impacts (double align,
                        double dWidth,
                        double gap,
                        double white)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, align);
            setImpact(1, dWidth);
            setImpact(2, gap);
            setImpact(3, white);
        }
    }
}
