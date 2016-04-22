//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            A b s t r a c t V e r t i c a l I n t e r                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.math.AreaUtil;

import omr.sig.BasicImpacts;
import omr.sig.GradeImpacts;

import omr.util.Jaxb;

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code AbstractVerticalInter} is the basis for similar vertical inter classes:
 * {@link BarlineInter} and {@link BracketInter}.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractVerticalInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Line width. */
    @XmlElement
    @XmlJavaTypeAdapter(value = Jaxb.Double1Adapter.class, type = double.class)
    protected final double width;

    /** Median line, perhaps not fully straight. */
    @XmlElement
    protected final Line2D median;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AbstractVerticalInter} object.
     *
     * @param glyph   the underlying glyph
     * @param shape   the assigned shape
     * @param impacts the assignment details
     * @param median  the median line
     * @param width   the line width
     */
    public AbstractVerticalInter (Glyph glyph,
                                  Shape shape,
                                  GradeImpacts impacts,
                                  Line2D median,
                                  double width)
    {
        super(glyph, null, shape, impacts);
        this.median = median;
        this.width = width;

        if (median != null) {
            computeArea();
        }
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
    // getMedian //
    //-----------//
    /**
     * @return the median
     */
    public Line2D getMedian ()
    {
        return median;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * @return the width
     */
    public double getWidth ()
    {
        return width;
    }

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
    private void computeArea ()
    {
        setArea(AreaUtil.verticalRibbon(new Path2D.Double(median), width));

        // Define precise bounds based on this path
        setBounds(getArea().getBounds());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{"core", "gap", "start", "stop"};

        private static final double[] WEIGHTS = new double[]{1, 1, 1, 1};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double core,
                        double gap,
                        double start,
                        double stop)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, core);
            setImpact(1, gap);
            setImpact(2, start);
            setImpact(3, stop);
        }
    }
}
