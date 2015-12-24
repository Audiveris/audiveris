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
package omr.sheet;

import omr.glyph.Shape;

import omr.math.PointUtil;

import omr.sheet.PartBarline.Style;

import static omr.sheet.PartBarline.Style.*;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractInter;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.Inter;
import omr.sig.inter.RepeatDotInter;
import omr.sig.relation.Relation;
import omr.sig.relation.RepeatDotBarRelation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Class {@code StaffBarline} represents a logical barline for one staff only.
 * <p>
 * Such logical barline may be composed of a horizontal sequence of Inter instances of
 * {@link RepeatDotInter} and {@link BarlineInter} classes.
 * <p>
 * A {@link PartBarline} is a logical barline for one part, that is made of one
 * {@code StaffBarline} for each staff in the part.
 * <p>
 * Reference abscissa (quoting Michael Good): the best approximation that's application-independent
 * would probably be to use the center of the barline. In back-to-back barlines that would be the
 * center of the thick barline. For double barlines like light-light or light-heavy this would be
 * the center of the rightmost barline.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "staff-barline")
public class StaffBarline
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Abscissa-ordered sequence of physical components. */
    @XmlList
    @XmlValue
    @XmlIDREF
    private final ArrayList<AbstractInter> items = new ArrayList<AbstractInter>();

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
        addItem(inter);
    }

    /**
     * Insert a barline inter as part of the logical barline.
     *
     * @param inter the barline inter to insert
     */
    public void addInter (BarlineInter inter)
    {
        addItem(inter);
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
            //            if (inter instanceof RepeatDotInter && (inter.getCenter().x < center.x)) {
            //                return true;
            //            }
            if (inter instanceof BarlineInter) {
                SIGraph sig = inter.getSig();

                for (Relation rel : sig.getRelations(inter, RepeatDotBarRelation.class)) {
                    Inter dot = sig.getOppositeInter(inter, rel);

                    if (dot.getCenter().x < center.x) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    //----------------//
    // hasDotsOnRight //
    //----------------//
    public boolean hasDotsOnRight ()
    {
        final Point center = getCenter();

        for (Inter inter : items) {
            //            if (inter instanceof RepeatDotInter && (inter.getCenter().x > center.x)) {
            //                return true;
            //            }
            if (inter instanceof BarlineInter) {
                SIGraph sig = inter.getSig();

                for (Relation rel : sig.getRelations(inter, RepeatDotBarRelation.class)) {
                    Inter dot = sig.getOppositeInter(inter, rel);

                    if (dot.getCenter().x > center.x) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    //--------------//
    // isLeftRepeat //
    //--------------//
    public boolean isLeftRepeat ()
    {
        return (getStyle() == HEAVY_LIGHT) && hasDotsOnRight();
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

    //---------//
    // addItem //
    //---------//
    /**
     * Maintain the 'items' list as if it was an ordered set.
     * 'Items' is declared as a list to ease JAXB processing
     *
     * @param inter the inter to add
     */
    private void addItem (AbstractInter inter)
    {
        if (!items.contains(inter)) {
            items.add(inter);
            Collections.sort(items, Inter.byFullAbscissa);
        }
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

    //
    //    //~ Inner Classes ------------------------------------------------------------------------------
    //    private static class Adapter
    //            extends XmlAdapter<ItemList, SortedSet<AbstractInter>>
    //    {
    //        //~ Methods --------------------------------------------------------------------------------
    //
    //        @Override
    //        public ItemList marshal (SortedSet<AbstractInter> set)
    //                throws Exception
    //        {
    //            if (set == null)
    //                return null;
    //
    //            return new ItemList(set);
    //        }
    //
    //        @Override
    //        public SortedSet<AbstractInter> unmarshal (ItemList lst)
    //                throws Exception
    //        {
    //            SortedSet<AbstractInter> items = new TreeSet<AbstractInter>(Inter.byFullAbscissa);
    //        }
    //    }
    //
    //    private static class ItemList
    //    {
    //        //~ Instance fields ------------------------------------------------------------------------
    //
    //        @XmlList
    //        @XmlIDREF
    //        @XmlElement(name = "items")
    //        ArrayList<AbstractInter> inters = new ArrayList<AbstractInter>();
    //
    //        //~ Constructors ---------------------------------------------------------------------------
    //        public ItemList (Collection<AbstractInter> inters)
    //        {
    //            this.inters.addAll(inters);
    //        }
    //
    //        public ItemList ()
    //        {
    //        }
    //    }
}
