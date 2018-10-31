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
import org.audiveris.omr.sig.BasicImpacts;

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
    //~ Instance fields ----------------------------------------------------------------------------

    /** Physical portion of the connection line, excluding portions within staves. */
    private Area area;

    /** Rather vertical median line. */
    private Line2D median;

    //~ Constructors -------------------------------------------------------------------------------
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
                new Impacts((BarAlignment.Impacts) align.getImpacts(), gapImpact, whiteImpact));
    }

    //~ Methods ------------------------------------------------------------------------------------
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
    public Area getArea ()
    {
        if (area == null) {
            area = AreaUtil.verticalRibbon(getMedian(), getWidth());
        }

        return area;
    }

    //-----------/
    // getMedian //
    //-----------/
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{"align", "dWidth", "gap", "white"};

        private static final double[] WEIGHTS = new double[]{2, 1, 2, 2};

        //~ Constructors ---------------------------------------------------------------------------
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

        public Impacts (BarAlignment.Impacts alignImpacts,
                        double gap,
                        double white)
        {
            this(alignImpacts.getAlignImpact(), alignImpacts.getWidthImpact(), gap, white);
        }
    }
}
