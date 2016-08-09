//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B a r C o n n e c t i o n                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.grid;

import omr.math.AreaUtil;

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
     * @param align the underlying barline alignment
     */
    public BarConnection (BarAlignment align)
    {
        super(align.topPeak, align.bottomPeak, align.slope, align.dWidth, align.getImpacts());
    }

    //~ Methods ------------------------------------------------------------------------------------
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
}
