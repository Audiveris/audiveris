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
import static omr.score.entity.Barline.Style.*;

import omr.sig.BarlineInter;
import omr.sig.Inter;
import omr.sig.RepeatDotInter;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code StaffBarline} represents a logical barline for one staff.
 * <p>
 * Such logical barline may be composed of several Inters instances of {@link RepeatDotInter} and
 * {@link BarlineInter} classes.
 * <p>
 * A {@link Barline} is a logical barline for one part, that is made of one {@code StaffBarline}
 * for each staff in the part.
 * <p>
 * Reference abscissa (Michael Good): the best approximation that's application-independent would
 * probably be to use the center of the barline. In back-to-back barlines that would be the center
 * of the thick barline. For double barlines like light-light or light-heavy this would be the
 * center of the rightmost barline.
 *
 * @author Hervé Bitteur
 */
public class StaffBarline
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Abscissa-ordered sequence of physical components. */
    private SortedSet<Inter> items = new TreeSet<Inter>(Inter.byFullAbscissa);

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
     * Insert a barline inter as part of the logical barline.
     *
     * @param inter the barline inter to insert
     */
    public void addInter (BarlineInter inter)
    {
        items.add(inter);
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
        for (Inter inter : items) {
            if (inter instanceof BarlineInter) {
                return inter.getCenter().x;
            }
        }

        throw new IllegalStateException("No abscissa computable for " + this);
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
        Integer x = null;

        for (Inter inter : items) {
            if (inter instanceof BarlineInter) {
                x = inter.getCenter().x;
            }
        }

        if (x != null) {
            return x;
        } else {
            throw new IllegalStateException("No abscissa computable for " + this);
        }
    }

    //----------//
    // getStyle //
    //----------//
    public Barline.Style getStyle ()
    {
        switch (items.size()) {
        case 0:
            return NONE;

        case 1:
            return (items.first().getShape() == Shape.THIN_BARLINE) ? REGULAR : HEAVY;

        case 2: {
            if (items.first().getShape() == Shape.THIN_BARLINE) {
                return (items.last().getShape() == Shape.THIN_BARLINE) ? LIGHT_LIGHT : LIGHT_HEAVY;
            } else {
                return (items.last().getShape() == Shape.THIN_BARLINE) ? HEAVY_LIGHT : HEAVY_HEAVY;
            }
        }

        default:
            return null;
        }
    }
}
