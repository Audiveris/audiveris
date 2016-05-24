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
    protected final StaffPeak topPeak;

    /** Bar peak in the lower staff. */
    protected final StaffPeak bottomPeak;

    /** (Normalized) abscissa shift of de-skewed bottom peak vs de-skewed top peak. */
    protected final double dx;

    /** (Normalized) delta width of bottom peak vs top peak. */
    protected final double dWidth;

    /** Alignment quality. */
    private final GradeImpacts impacts;

    /** Alignment grade. */
    protected double grade;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BarAlignment object.
     *
     * @param topPeak    peak in the upper staff
     * @param bottomPeak peak in the lower staff
     * @param dx         bottomPeak.x - topPeak.x (de-skewed & normalized)
     * @param dWidth     bottomPeak.width - topPeak.width (normalized)
     * @param impacts    the alignment quality
     */
    public BarAlignment (StaffPeak topPeak,
                         StaffPeak bottomPeak,
                         double dx,
                         double dWidth,
                         GradeImpacts impacts)
    {
        this.topPeak = topPeak;
        this.bottomPeak = bottomPeak;
        this.dx = dx;
        this.dWidth = dWidth;
        this.impacts = impacts;

        grade = impacts.getGrade();
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
                if (align instanceof BarConnection && (align.getGrade() > best.getGrade())) {
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

    //----------//
    // getGrade //
    //----------//
    public double getGrade ()
    {
        return grade;
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
    public StaffPeak getPeak (VerticalSide side)
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
        sb.append(String.format("(%.3f)", grade));
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
        sb.append(topPeak).append(" ").append(bottomPeak).append(
                String.format(" dx:%.0f dw:%.0f", dx, dWidth));

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

        private static final String[] NAMES = new String[]{"align", "dWidth"};

        private static final double[] WEIGHTS = new double[]{2, 1};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double align,
                        double dWidth)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, align);
            setImpact(1, dWidth);
        }
    }
}
