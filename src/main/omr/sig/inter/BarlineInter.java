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

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.sig.GradeImpacts;

import omr.util.HorizontalSide;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BarlineInter} represents an interpretation of bar line (thin or thick
 * vertical segment).
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "barline")
public class BarlineInter
        extends AbstractVerticalInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Does this bar line define a staff side?. */
    @XmlElement(name = "staff-end")
    private HorizontalSide staffEnd;

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
                         Line2D median,
                         double width)
    {
        super(glyph, shape, impacts, median, width);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BarlineInter ()
    {
        super(null, null, null, null, 0);
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

    //--------//
    // delete //
    //--------//
    /**
     * Since a BarlineInter instance is held by its containing staff, make sure staff
     * bar collection is updated.
     */
    @Override
    public void delete ()
    {
        if (staff != null) {
            staff.removeBar(this);
        }

        super.delete();
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
     * @return the abscissa-wise closest barline
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
        return staffEnd == side;
    }

    //-------------//
    // setStaffEnd //
    //-------------//
    public void setStaffEnd (HorizontalSide side)
    {
        staffEnd = side;
    }
}
