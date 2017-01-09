//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B a r A l i g n m e n t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.sheet.grid;

import org.audiveris.omr.sig.BasicImpacts;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.util.VerticalSide;

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

    /** Slope of alignment from de-skewed bottom peak to de-skewed top peak. */
    protected final double slope;

    /** Delta width of bottom peak vs top peak. */
    protected final double dWidth;

    /** Alignment quality. */
    protected final GradeImpacts impacts;

    /** Alignment grade. */
    protected double grade;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BarAlignment object.
     *
     * @param topPeak    peak in the upper staff
     * @param bottomPeak peak in the lower staff
     * @param slope      best slope of alignment of left and right sides of peaks
     * @param dWidth     bottomPeak.width - topPeak.width
     * @param impacts    the alignment quality
     */
    public BarAlignment (StaffPeak topPeak,
                         StaffPeak bottomPeak,
                         double slope,
                         double dWidth,
                         GradeImpacts impacts)
    {
        this.topPeak = topPeak;
        this.bottomPeak = bottomPeak;
        this.slope = slope;
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
     * @param side       which side of alignment to consider
     * @return the best one, or null if collection is empty
     */
    public static BarAlignment bestOf (Collection<? extends BarAlignment> alignments,
                                       VerticalSide side)
    {
        BarAlignment best = null;
        double bestCtxGrade = 0;

        for (final BarAlignment align : alignments) {
            final StaffPeak partner = (side == VerticalSide.TOP) ? align.topPeak : align.bottomPeak;
            final double ctxGrade = align.getGrade() * partner.getImpacts().getGrade();

            if (best == null) {
                best = align;
                bestCtxGrade = ctxGrade;
            } else if (best instanceof BarConnection) {
                if (align instanceof BarConnection && (ctxGrade > bestCtxGrade)) {
                    best = align;
                    bestCtxGrade = ctxGrade;
                }
            } else if (align instanceof BarConnection || (ctxGrade > bestCtxGrade)) {
                best = align;
                bestCtxGrade = ctxGrade;
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
                String.format(" slope:%.2f dw:%.0f", slope, dWidth));

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
