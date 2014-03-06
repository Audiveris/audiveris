//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t B e a m I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.math.AreaUtil;

import omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;

/**
 * Abstract class {@code AbstractBeamInter} is the basis for {@link FullBeamInter} and
 * {@link BeamHookInter} classes.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractBeamInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractBeamInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Median line. */
    private final Line2D median;

    /** Beam height. */
    private final double height;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractBeamInter object.
     *
     * @param glyph   the underlying glyph
     * @param shape   BEAM or BEAM_HOOK
     * @param impacts the grade details
     * @param median  median beam line
     * @param height  beam height
     */
    protected AbstractBeamInter (Glyph glyph,
                                 Shape shape,
                                 GradeImpacts impacts,
                                 Line2D median,
                                 double height)
    {
        super(glyph, shape, impacts.getGrade());
        this.median = median;
        this.height = height;

        setImpacts(impacts);

        setArea(AreaUtil.horizontalParallelogram(median.getP1(), median.getP2(), height));

        // Define precise bounds based on this path
        setBounds(getArea().getBounds());
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
        final double dy = (side == VerticalSide.TOP) ? (-height / 2) : (height / 2);

        return new Line2D.Double(
                median.getX1(),
                median.getY1() + dy,
                median.getX2(),
                median.getY2() + dy);
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

    //--------//
    // isGood //
    //--------//
    @Override
    public boolean isGood ()
    {
        return grade >= 0.35; // TODO: revise this!
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{"width", "core", "belt", "dist"};

        private static final double[] WEIGHTS = new double[]{0.5, 2, 2, 2};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double width,
                        double core,
                        double belt,
                        double dist)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, width);
            setImpact(1, core);
            setImpact(2, belt);
            setImpact(3, dist);
        }

        //~ Methods --------------------------------------------------------------------------------
        public double getDistImpact ()
        {
            return getImpact(3);
        }
    }
}
