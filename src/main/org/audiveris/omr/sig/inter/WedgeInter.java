//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       W e d g e I n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code WedgeInter} represents a wedge (crescendo or diminuendo).
 * <p>
 * <img alt="Wedge image"
 * src=
 * "http://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Music_hairpins.svg/296px-Music_hairpins.svg.png">
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "wedge")
public class WedgeInter
        extends AbstractDirectionInter
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(WedgeInter.class);

    /** Top line. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private Line2D l1;

    /** Bottom line. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private Line2D l2;

    /**
     * Creates a new WedgeInter object.
     *
     * @param l1      precise first line
     * @param l2      precise second line
     * @param bounds  bounding bounds
     * @param shape   CRESCENDO or DIMINUENDO
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
     * Creates a new {@code WedgeInter} object, meant for manual assignment.
     *
     * @param glyph underlying glyph, if any
     * @param shape CRESCENDO or DIMINUENDO
     * @param grade assigned grade
     */
    public WedgeInter (Glyph glyph,
                       Shape shape,
                       double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, shape, grade);

        if (glyph != null) {
            setGlyph(glyph);
        }
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
    /**
     * Report wedge top line.
     *
     * @return top line
     */
    public Line2D getLine1 ()
    {
        return l1;
    }

    //----------//
    // getLine2 //
    //----------//
    /**
     * Report wedge bottom line.
     *
     * @return bottom line
     */
    public Line2D getLine2 ()
    {
        return l2;
    }

    //-----------//
    // getSpread //
    //-----------//
    /**
     * Report vertical gap between ending points or provided side.
     *
     * @param side provided horizontal side
     * @return vertical gap in pixels
     */
    public double getSpread (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return l2.getY1() - l1.getY1();
        } else {
            return l2.getY2() - l1.getY2();
        }
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        if (glyph == null) {
            return;
        }

        Rectangle b = glyph.getBounds();

        if ((l1 == null) || (l2 == null)) {
            switch (shape) {
            case CRESCENDO:
                l1 = new Line2D.Double(b.x, b.y + (b.height / 2), b.x + b.width, b.y);
                l2 = new Line2D.Double(b.x, b.y + (b.height / 2), b.x + b.width, b.y + b.height);

                break;

            case DIMINUENDO:
                l1 = new Line2D.Double(b.x, b.y, b.x + b.width, b.y + (b.height / 2));
                l2 = new Line2D.Double(b.x, b.y + b.height, b.x + b.width, b.y + (b.height / 2));

                break;

            default:
                logger.warn("Unknown wedge shape: {}", shape);

                break;
            }
        }
    }

    //------------------------//
    // getStackAbscissaMargin //
    //------------------------//
    /**
     * Report margin beyond stack abscissa limits.
     *
     * @return margin constant
     */
    public static Scale.Fraction getStackAbscissaMargin ()
    {
        return constants.stackAbscissaMargin;
    }

    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends GradeImpacts
    {

        private static final String[] NAMES = new String[]{
            "s1",
            "s2",
            "closedDy",
            "openDy",
            "openBias"};

        private static final double[] WEIGHTS = new double[]{1, 1, 1, 1, 1};

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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction stackAbscissaMargin = new Scale.Fraction(
                1.0,
                "Margin beyond stack abscissa limits");
    }
}
