//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B a r A l i g n m e n t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.sig.BasicImpacts;
import omr.sig.GradeImpacts;

import omr.util.VerticalSide;

import java.util.Objects;

/**
 * Class {@code BarAlignment} represents an alignment found between a bar peak in one
 * staff and another bar peak in the staff below.
 *
 * @author Hervé Bitteur
 */
public class BarAlignment
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Bar peak in the upper staff. */
    protected final BarPeak topPeak;

    /** Bar peak in the lower staff. */
    protected final BarPeak bottomPeak;

    /**
     * Abscissa shift in pixels between de-skewed bottom peak and de-skewed top peak.
     */
    protected final double dx;

    /** Connection quality. */
    private final GradeImpacts impacts;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BarAlignment object.
     *
     * @param topPeak    peak in the upper staff
     * @param bottomPeak peak in the lower staff
     * @param dx         bottomPeak.x - topPeak.x (de-skewed abscissae)
     * @param impacts    the alignment quality
     */
    public BarAlignment (BarPeak topPeak,
                         BarPeak bottomPeak,
                         double dx,
                         GradeImpacts impacts)
    {
        this.topPeak = topPeak;
        this.bottomPeak = bottomPeak;
        this.dx = dx;
        this.impacts = impacts;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof BarAlignment) {
            BarAlignment that = (BarAlignment) obj;

            return (topPeak == that.topPeak) && (bottomPeak == that.bottomPeak);
        } else {
            return false;
        }
    }

    //------------//
    // getImpacts //
    //------------//
    public GradeImpacts getImpacts ()
    {
        return impacts;
    }

    //---------//
    // getPeak //
    //---------//
    public BarPeak getPeak (VerticalSide side)
    {
        if (side == VerticalSide.TOP) {
            return topPeak;
        } else {
            return bottomPeak;
        }
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (97 * hash) + Objects.hashCode(this.topPeak);
        hash = (97 * hash) + Objects.hashCode(this.bottomPeak);

        return hash;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("{");
        sb.append(internals());
        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // internals //
    //-----------//
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("top:Staff#").append(topPeak.getStaff().getId()).append("-").append(topPeak);
        sb.append(" bot:Staff#").append(bottomPeak.getStaff().getId()).append("-").append(
                bottomPeak);
        sb.append(String.format(" dx:%.1f", dx));

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

        private static final String[] NAMES = new String[]{"align"};

        private static final double[] WEIGHTS = new double[]{1};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double align)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, align);
        }
    }
}
