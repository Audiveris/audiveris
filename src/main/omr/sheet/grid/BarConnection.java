//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B a r C o n n e c t i o n                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.math.AreaUtil;

import omr.sig.BasicImpacts;
import omr.sig.GradeImpacts;

import java.awt.geom.Area;
import java.awt.geom.Line2D;

/**
 * Class {@code BarConnection} represents a physical connection across staves between
 * two bar lines.
 * <p>
 * It indicates that the two linked staves belong to the same system and/or part.
 *
 * @author Hervé Bitteur
 */
public class BarConnection
        extends BarAlignment
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Connection quality. */
    private final GradeImpacts impacts;

    /**
     * The physical portion of the connection line, excluding the portion within staves.
     */
    private Area area;

    /** Median line. */
    private Line2D median;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BarConnection object.
     *
     * @param alignment the underlying bar line alignment
     * @param impacts   the connection quality
     */
    public BarConnection (BarAlignment alignment,
                          GradeImpacts impacts)
    {
        super(alignment.topPeak, alignment.bottomPeak, alignment.dx, alignment.getImpacts());
        this.impacts = impacts;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getArea //
    //---------//
    public Area getArea ()
    {
        if (area == null) {
            area = AreaUtil.verticalRibbon(getMedian(), getWidth());
        }

        return area;
    }

    //----------//
    // getWidth //
    //----------//
    public double getWidth ()
    {
        return (topPeak.getWidth() + bottomPeak.getWidth()) / 2d;
    }

    //------------//
    // getImpacts //
    //------------//
    @Override
    public GradeImpacts getImpacts ()
    {
        return impacts;
    }

    //-----------/
    // getMedian //
    //-----------/
    public Line2D getMedian ()
    {
        if (median == null) {
            double xTop = (topPeak.getStart() + topPeak.getStop()) / 2d;
            double xBot = (bottomPeak.getStart() + bottomPeak.getStop()) / 2d;
            median = new Line2D.Double(xTop, topPeak.getBottom(), xBot, bottomPeak.getTop());
        }

        return median;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" ").append(impacts);

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{"align", "white", "gap"};

        private static final double[] WEIGHTS = new double[]{1, 1, 1};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double align,
                        double white,
                        double gap)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, align);
            setImpact(1, white);
            setImpact(2, gap);
        }
    }
}
