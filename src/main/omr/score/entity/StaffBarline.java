//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S t a f f B a r l i n e                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Shape;

import omr.math.PointUtil;

import omr.sheet.PartBarline;
import omr.sheet.PartBarline.Style;
import static omr.sheet.PartBarline.Style.*;

import omr.sig.inter.BarlineInter;
import omr.sig.inter.Inter;
import omr.sig.inter.RepeatDotInter;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code StaffBarline} represents a logical bar-line for one staff only.
 * <p>
 * Such logical bar-line may be composed of a horizontal sequence of Inter instances of
 * {@link RepeatDotInter} and {@link BarlineInter} classes.
 * <p>
 * A {@link PartBarline} is a logical bar-line for one part, that is made of one
 * {@code StaffBarline} for each staff in the part.
 * <p>
 * Reference abscissa (quoting Michael Good): the best approximation that's application-independent
 * would probably be to use the center of the barline. In back-to-back barlines that would be the
 * center of the thick barline. For double barlines like light-light or light-heavy this would be
 * the center of the rightmost barline.
 *
 * @author Hervé Bitteur
 */
public class StaffBarline
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Abscissa-ordered sequence of physical components. */
    private final SortedSet<Inter> items = new TreeSet<Inter>(Inter.byFullAbscissa);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StaffBarline} object.
     */
    public StaffBarline ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Insert a repeat dot inter as part of the logical barline.
     *
     * @param inter the repeat dot inter to insert
     */
    public void addInter (RepeatDotInter inter)
    {
        items.add(inter);
    }

    /**
     * Insert a bar-line inter as part of the logical barline.
     *
     * @param inter the bar-line inter to insert
     */
    public void addInter (BarlineInter inter)
    {
        items.add(inter);
    }

    //---------//
    // getBars //
    //---------//
    public List<BarlineInter> getBars ()
    {
        List<BarlineInter> bars = new ArrayList<BarlineInter>();

        for (Inter inter : items) {
            if (inter instanceof BarlineInter) {
                bars.add((BarlineInter) inter);
            }
        }

        return bars;
    }

    //-----------//
    // getCenter //
    //-----------//
    public Point getCenter ()
    {
        switch (getStyle()) {
        case REGULAR:
        case HEAVY:
        case HEAVY_LIGHT:
            return getLeftBar().getCenter();

        case LIGHT_LIGHT:
        case LIGHT_HEAVY:
        case HEAVY_HEAVY:
            return getRightBar().getCenter();

        case NONE:
        default:
            return null;
        }
    }

    //------------//
    // getLeftBar //
    //------------//
    public BarlineInter getLeftBar ()
    {
        for (Inter inter : items) {
            if (inter instanceof BarlineInter) {
                return (BarlineInter) inter;
            }
        }

        return null;
    }

    //----------//
    // getLeftX //
    //----------//
    /**
     * Report the center abscissa of the left bar
     *
     * @return abscissa of the left side
     */
    public int getLeftX ()
    {
        final BarlineInter leftBar = getLeftBar();

        if (leftBar != null) {
            return leftBar.getCenter().x;
        }

        throw new IllegalStateException("No abscissa computable for " + this);
    }

    //-------------//
    // getRightBar //
    //-------------//
    public BarlineInter getRightBar ()
    {
        Inter last = null;

        for (Inter inter : items) {
            if (inter instanceof BarlineInter) {
                last = inter;
            }
        }

        return (BarlineInter) last;
    }

    //-----------//
    // getRightX //
    //-----------//
    /**
     * Report the center abscissa of the right bar
     *
     * @return abscissa of the right side
     */
    public int getRightX ()
    {
        final BarlineInter rightBar = getRightBar();

        if (rightBar != null) {
            return rightBar.getCenter().x;
        }

        throw new IllegalStateException("No abscissa computable for " + this);
    }

    //----------//
    // getStyle //
    //----------//
    public PartBarline.Style getStyle ()
    {
        switch (getBarCount()) {
        case 0:
            return NONE;

        case 1:
            return (getLeftBar().getShape() == Shape.THIN_BARLINE) ? REGULAR : HEAVY;

        case 2: {
            if (getLeftBar().getShape() == Shape.THIN_BARLINE) {
                return (getRightBar().getShape() == Shape.THIN_BARLINE) ? LIGHT_LIGHT : LIGHT_HEAVY;
            } else {
                return (getRightBar().getShape() == Shape.THIN_BARLINE) ? HEAVY_LIGHT : HEAVY_HEAVY;
            }
        }

        default:
            return null;
        }
    }

    //---------------//
    // hasDotsOnLeft //
    //---------------//
    public boolean hasDotsOnLeft ()
    {
        final Point center = getCenter();

        for (Inter inter : items) {
            if (inter instanceof RepeatDotInter && (inter.getCenter().x < center.x)) {
                return true;
            }
        }

        return false;
    }

    //---------------//
    // isRightRepeat //
    //---------------//
    public boolean isRightRepeat ()
    {
        return (getStyle() == LIGHT_HEAVY) && hasDotsOnLeft();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{StaffBarline");
        Style style = getStyle();
        sb.append(" ").append(style);
        sb.append(" ").append(PointUtil.toString(getCenter()));
        sb.append("}");

        return sb.toString();
    }

    //-------------//
    // getBarCount //
    //-------------//
    private int getBarCount ()
    {
        int count = 0;

        for (Inter inter : items) {
            if (inter instanceof BarlineInter) {
                count++;
            }
        }

        return count;
    }
}
