//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         B e a m I t e m                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.beam;

import omr.math.AreaUtil;
import omr.math.LineUtil;

import omr.util.Vip;

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
