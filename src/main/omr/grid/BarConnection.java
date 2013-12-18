//----------------------------------------------------------------------------//
//                                                                            //
//                          B a r C o n n e c t i o n                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.math.AreaUtil;

import omr.sig.BasicImpacts;
import omr.sig.GradeImpacts;

import java.awt.geom.Area;
import java.awt.geom.Line2D;

/**
 * DOCUMENT ME!
 *
 * @author TBD
 * @version TBD
 */
public class BarConnection
        extends BarsRetriever.Alignment
{
    //~ Instance fields --------------------------------------------------------

    /** Main staff line height. */
    private final int mainFore;

    /** Connection quality: white ratio. */
    private final double whiteRatio;

    /** Connection quality: largest gap (in interline). */
    private final double gap;

    private Area area;

    private final GradeImpacts impacts;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BarConnection object.
     *
     * @param alignment  DOCUMENT ME!
     * @param mainFore   DOCUMENT ME!
     * @param whiteRatio DOCUMENT ME!
     * @param gap        DOCUMENT ME!
     * @param impacts    DOCUMENT ME!
     */
    public BarConnection (BarsRetriever.Alignment alignment,
                          int mainFore,
                          double whiteRatio,
                          double gap,
                          GradeImpacts impacts)
    {
        super(alignment.topPeak, alignment.bottomPeak, alignment.dx);
        this.mainFore = mainFore;
        this.whiteRatio = whiteRatio;
        this.gap = gap;
        this.impacts = impacts;
    }

    //~ Methods ----------------------------------------------------------------
    public Area getArea ()
    {
        if (area == null) {
            double width = Math.max(topPeak.getWidth(), bottomPeak.getWidth());
            double xTop = (topPeak.getStart() + topPeak.getStop()) / 2d;
            double xBot = (bottomPeak.getStart() + bottomPeak.getStop()) / 2d;
            Line2D line = new Line2D.Double(
                    xTop,
                    topPeak.getBottom(),
                    xBot,
                    bottomPeak.getTop());
            area = AreaUtil.verticalRibbon(line, width);
        }

        return area;
    }

    public GradeImpacts getImpacts ()
    {
        return impacts;
    }

    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(String.format(" white:%.0f", 100 * whiteRatio))
                .append("%");
        sb.append(String.format(" gap:%.1f", gap));

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -----------------------------------------

        private static final String[] NAMES = new String[]{"white", "gap"};

        private static final double[] WEIGHTS = new double[]{1, 1};

        //~ Constructors -------------------------------------------------------
        public Impacts (double white,
                        double gap)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, white);
            setImpact(1, gap);
        }
    }
}
