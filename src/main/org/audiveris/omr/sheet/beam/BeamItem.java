//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         B e a m I t e m                                        //
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
package org.audiveris.omr.sheet.beam;

import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.util.Vip;

import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Class {@code BeamItem} represents one beam candidate, using a very simple
 * parallelogram definition.
 */
public class BeamItem
        implements Vip
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The median line of the beam item. */
    final Line2D median;

    /** The constant height of the item. */
    final double height;

    /** VIP flag. */
    private boolean vip;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BeamItem object.
     *
     * @param median beam median line
     * @param height beam height
     */
    public BeamItem (Line2D median,
                     double height)
    {
        this.median = median;
        this.height = height;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getBeltArea //
    //-------------//
    public Area getBeltArea (Area coreArea,
                             int dx,
                             int topDy,
                             int bottomDy)
    {
        final double shiftY = (bottomDy - topDy) / 2;
        final double beltHeight = height + topDy + bottomDy;

        Point2D p1 = LineUtil.intersectionAtX(median, median.getX1() - dx);
        p1.setLocation(p1.getX(), p1.getY() + shiftY);

        Point2D p2 = LineUtil.intersectionAtX(median, median.getX2() + dx);
        p2.setLocation(p2.getX(), p2.getY() + shiftY);

        Area beltArea = AreaUtil.horizontalParallelogram(p1, p2, beltHeight);
        beltArea.subtract(coreArea);

        return beltArea;
    }

    //-------------//
    // getCoreArea //
    //-------------//
    public Area getCoreArea ()
    {
        return AreaUtil.horizontalParallelogram(median.getP1(), median.getP2(), height);
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip (boolean vip)
    {
        this.vip = vip;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return String.format(
                "{item median:(%.1f,%.1f)-(%.1f,%.1f) height:%.1f}",
                median.getX1(),
                median.getY1(),
                median.getX2(),
                median.getY2(),
                height);
    }
}
