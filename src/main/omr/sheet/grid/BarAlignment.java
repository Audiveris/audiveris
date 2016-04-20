//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B a r A l i g n m e n t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.sig.BasicImpacts;
import omr.sig.GradeImpacts;

import omr.util.VerticalSide;

import java.util.Collection;
import java.util.Objects;

/**
 * Class {@code BarAlignment} represents an alignment found between a bar peak in one
 * staff and another bar peak in the staff below.
 *
 * @author Hervé Bitteur
 */
public class BarAlignment
        implements Comparable<BarAlignment>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Bar peak in the upper staff. */
    protected final StaffPeak.Bar topPeak;

    /** Bar peak in the lower staff. */
    protected final StaffPeak.Bar bottomPeak;

    /**
     * Abscissa shift between de-skewed bottom peak and de-skewed top peak.
     * (Normalized in interline fraction)
     */
    protected final double dx;

    /** Alignment quality. */
    private final GradeImpacts impacts;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BarAlignment object.
     *
     * @param topPeak    peak in the upper staff
     * @param bottomPeak peak in the lower staff
     * @param dx         bottomPeak.x - topPeak.x (de-skewed & normalized)
     * @param impacts    the alignment quality
     */
    public BarAlignment (StaffPeak.Bar topPeak,
                         StaffPeak.Bar bottomPeak,
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
    // bestOf //
    //--------//
    /**
     * Report the best (connection or alignment) among the provided collection.
     *
     * @param alignments the collection to filter
     * @return the best one, or null if collection is empty
     */
    public static BarAlignment bestOf (Collection<? extends BarAlignment> alignments)
    {
        BarAlignment best = null;

        for (final BarAlignment align : alignments) {
            if (best == null) {
                best = align;
            } else if (best instanceof BarConnection) {
                if (align instanceof BarConnection
                    && (align.getImpacts().getGrade() > best.getImpacts().getGrade())) {
                    best = align;
                }
            } else if (align instanceof BarConnection) {
                best = align;
            } else if (Math.abs(align.dx) <= Math.abs(best.dx)) {
                best = align;
            }
        }

        return best;
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Allows to sort first vertically by staff, then horizontally by abscissa.
     *
     * @param that the other alignment to compare with.
     * @return comparison result
     */
    @Override
    public int compareTo (BarAlignment that)
    {
        if (this == that) {
            return 0;
        }

        final StaffPeak p1 = this.topPeak;
        final StaffPeak p2 = that.topPeak;

        if (p1.getStaff() != p2.getStaff()) {
            return Integer.compare(p1.getStaff().getId(), p2.getStaff().getId());
        }

        return Integer.compare(p1.getStart(), p2.getStart());
    }

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
    public StaffPeak.Bar getPeak (VerticalSide side)
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
