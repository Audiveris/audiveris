//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       W e d g e I n t e r                                      //
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
package omr.sig.inter;

import omr.glyph.Shape;

import omr.sig.BasicImpacts;
import omr.sig.GradeImpacts;

import omr.util.HorizontalSide;

import java.awt.Rectangle;
import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code WedgeInter} represents a wedge (crescendo or diminuendo).
 * <p>
 * <img alt="Wedge image"
 * src="http://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Music_hairpins.svg/296px-Music_hairpins.svg.png">
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "wedge")
public class WedgeInter
        extends AbstractDirectionInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    //
    //    private final SegmentInter s1;
    //
    //    private final SegmentInter s2;
    //
    /** Top line. */
    @XmlElement
    private final Line2D l1;

    /** Bottom line. */
    @XmlElement
    private final Line2D l2;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new WedgeInter object.
     *
     * @param l1      precise first line
     * @param l2      precise second line
     * @param bounds  bounding bounds
     * @param shape   CRESCENDO or DECRESCENDO
     * @param impacts assignments details
     */
    public WedgeInter (Line2D l1,
                       Line2D l2,
                       Rectangle bounds,
                       Shape shape,
                       GradeImpacts impacts)
    {
        super(null, bounds, shape, impacts);
        this.l1 = l1;
        this.l2 = l2;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private WedgeInter ()
    {
        super(null, null, null, 0);
        this.l1 = null;
        this.l2 = null;
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

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        Rectangle box = super.getBounds();

        if (box != null) {
            return box;
        }

        return new Rectangle(bounds = l1.getBounds().union(l2.getBounds()));
    }

    //----------//
    // getLine1 //
    //----------//
    public Line2D getLine1 ()
    {
        return l1;
    }

    //----------//
    // getLine2 //
    //----------//
    public Line2D getLine2 ()
    {
        return l2;
    }

    //-----------//
    // getSpread //
    //-----------//
    public double getSpread (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return l2.getY1() - l1.getY1();
        } else {
            return l2.getY2() - l1.getY2();
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{
            "s1", "s2", "closedDy", "openDy", "openBias"
        };

        private static final double[] WEIGHTS = new double[]{1, 1, 1, 1, 1};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double s1,
                        double s2,
                        double closedDy,
                        double openDy,
                        double openBias)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, s1);
            setImpact(1, s2);
            setImpact(2, closedDy);
            setImpact(3, openDy);
            setImpact(4, openBias);
        }
    }
}
