//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B a r l i n e I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.math.Line;

import omr.sig.GradeImpacts;

import omr.util.HorizontalSide;

import java.awt.Point;
import java.util.Collection;
import java.util.EnumSet;

/**
 * Class {@code BarlineInter} represents an interpretation of bar line (thin or thick
 * vertical segment).
 *
 * @author Hervé Bitteur
 */
public class BarlineInter
        extends AbstractVerticalInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Does this bar line define a staff side?. */
    private final EnumSet<HorizontalSide> staffEnd = EnumSet.noneOf(HorizontalSide.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BarlineInter object.
     *
     * @param glyph   the underlying glyph
     * @param shape   the assigned shape
     * @param impacts the assignment details
     * @param median  the median line
     * @param width   the bar line width
     */
    public BarlineInter (Glyph glyph,
                         Shape shape,
                         GradeImpacts impacts,
                         Line median,
                         double width)
    {
        super(glyph, shape, impacts, median, width);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------------------//
    // getClosestBarline //
    //-------------------//
    /**
     * From a provided Barline collection, report the one which has the closest abscissa
     * to a provided point.
     *
     * @param bars  the collection of bars to browse
     * @param point the reference point
     * @return the abscissa-wise closest bar-line
     */
    public static BarlineInter getClosestBarline (Collection<BarlineInter> bars,
                                                  Point point)
    {
        BarlineInter bestBar = null;
        int bestDx = Integer.MAX_VALUE;

        for (BarlineInter bar : bars) {
            int dx = Math.abs(bar.getCenter().x - point.x);

            if (dx < bestDx) {
                bestDx = dx;
                bestBar = bar;
            }
        }

        return bestBar;
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        if (isStaffEnd(HorizontalSide.LEFT)) {
            return super.getDetails() + "/STAFF_LEFT_END";
        } else if (isStaffEnd(HorizontalSide.RIGHT)) {
            return super.getDetails() + "/STAFF_RIGHT_END";
        } else {
            return super.getDetails();
        }
    }

    //--------//
    // isGood //
    //--------//
    @Override
    public boolean isGood ()
    {
        return getGrade() >= 0.6; // TODO, quick & dirty
    }

    //------------//
    // isStaffEnd //
    //------------//
    public boolean isStaffEnd (HorizontalSide side)
    {
        return staffEnd.contains(side);
    }

    //-------------//
    // setStaffEnd //
    //-------------//
    public void setStaffEnd (HorizontalSide side)
    {
        staffEnd.add(side);
    }
}
