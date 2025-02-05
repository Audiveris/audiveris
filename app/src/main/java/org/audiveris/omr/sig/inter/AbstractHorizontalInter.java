//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                          A b s t r a c t H o r i z o n t a l I n t e r                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.TOP;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>AbstractHorizontalInter</code> is the basis for rather horizontal inter classes.
 * <ul>
 * <li>{@link AbstractBeamInter}
 * <li>{@link MultipleRestInter}
 * </ul>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class AbstractHorizontalInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /**
     * The ribbon average thickness, specified in pixels with 1 digit maximum after the dot.
     */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double1Adapter.class)
    protected double height;

    /** The ribbon median line, defined from left to right. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    protected Line2D median;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new AbstractHorizontalInter <b>ghost</b> object.
     * Median and height must be assigned later
     *
     * @param shape BEAM or BEAM_HOOK or BEAM_SMALL or MULTIPLE_REST
     * @param grade the grade
     */
    protected AbstractHorizontalInter (Shape shape,
                                       Double grade)
    {
        super(null, null, shape, grade);
    }

    /**
     * Creates a new AbstractHorizontalInter object.
     * Note there is no underlying glyph, cleaning will be based on beam area.
     *
     * @param shape  BEAM or BEAM_HOOK or BEAM_SMALL or MULTIPLE_REST
     * @param grade  evaluated grade
     * @param median median beam line
     * @param height beam height
     */
    protected AbstractHorizontalInter (Shape shape,
                                       Double grade,
                                       Line2D median,
                                       double height)
    {
        super(null, null, shape, grade);
        this.median = median;
        this.height = height;

        if (median != null) {
            computeArea();
        }
    }

    /**
     * Creates a new AbstractHorizontalInter object.
     * Note there is no underlying glyph, cleaning will be based on ribbon area.
     *
     * @param shape   BEAM or BEAM_HOOK or BEAM_SMALL or MULTIPLE_REST
     * @param impacts the grade details
     * @param median  median ribbon line
     * @param height  beam height
     */
    protected AbstractHorizontalInter (Shape shape,
                                       GradeImpacts impacts,
                                       Line2D median,
                                       double height)
    {
        super(null, null, shape, impacts);
        this.median = median;
        this.height = height;

        if (median != null) {
            computeArea();
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled for this object,
     * but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        if (median != null) {
            computeArea();
        }
    }

    //-------------//
    // computeArea //
    //-------------//
    /**
     * Compute the beam area.
     */
    protected final void computeArea ()
    {
        setArea(AreaUtil.horizontalParallelogram(median.getP1(), median.getP2(), height));

        // Define precise bounds based on this path
        // NOTA: these bounds may go slightly beyond the sheet image limits...
        bounds = getArea().getBounds();
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        getBounds();

        if ((bounds != null) && !bounds.contains(point)) {
            return false;
        }

        if ((glyph != null) && glyph.contains(point)) {
            return true;
        }

        return getArea().contains(point);
    }

    //---------//
    // getArea //
    //---------//
    @Override
    public Area getArea ()
    {
        if (area == null) {
            computeArea();
        }

        return area;
    }

    //-----------//
    // getBorder //
    //-----------//
    /**
     * Report the beam border line on desired side
     *
     * @param side the desired side
     * @return the beam border line on desired side
     */
    public Line2D getBorder (VerticalSide side)
    {
        final double dy = (side == TOP) ? (-height / 2) : (height / 2);

        return new Line2D.Double(
                median.getX1(),
                median.getY1() + dy,
                median.getX2(),
                median.getY2() + dy);
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds != null) {
            return new Rectangle(bounds);
        }

        if (glyph != null) {
            return new Rectangle(bounds = glyph.getBounds());
        }

        return new Rectangle(bounds = getArea().getBounds());
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * @return the height
     */
    public double getHeight ()
    {
        return height;
    }

    //-----------//
    // getMedian //
    //-----------//
    /**
     * Report the median line
     *
     * @return the beam median line
     */
    public Line2D getMedian ()
    {
        return median;
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    @Override
    public Point2D getRelationCenter ()
    {
        return PointUtil.middle(median);
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        if ((median == null) && (glyph != null)) {
            // Case of manual ribbon: Compute height and median parameters and area
            height = (int) Math.rint(glyph.getMeanThickness(Orientation.HORIZONTAL));
            median = glyph.getCenterLine();

            computeArea();
        }
    }
}
