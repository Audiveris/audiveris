//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            A b s t r a c t V e r t i c a l I n t e r                           //
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

import omr.math.AreaUtil;
import omr.math.Line;

import omr.sig.BasicImpacts;
import omr.sig.GradeImpacts;

/**
 * Class {@code AbstractVerticalInter} is the basis for similar vertical inter classes:
 * {@link BarlineInter} and {@link BracketInter}.
 *
 * @author Hervé Bitteur
 */
public class AbstractVerticalInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Median line, perhaps not fully straight. */
    protected final Line median;

    /** Line width. */
    protected final double width;

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
                                  Line median,
                                  double width)
    {
        super(glyph, null, shape, impacts);
        this.median = median;
        this.width = width;

        if (median != null) {
            setArea(AreaUtil.verticalRibbon(median.toPath(), width));

            // Define precise bounds based on this path
            setBounds(getArea().getBounds());
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
    public Line getMedian ()
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{"core", "belt", "gap", "start", "stop"};

        private static final double[] WEIGHTS = new double[]{1, 1, 1, 1, 1};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double core,
                        double belt,
                        double gap,
                        double start,
                        double stop)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, core);
            setImpact(1, belt);
            setImpact(2, gap);
            setImpact(3, start);
            setImpact(4, stop);
        }
    }
}
