//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B a r C o l u m n                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * Class {@code BarColumn} handles a system-based column of aligned projection peaks,
 * since column consistency helps handle barline candidates.
 * <ul>
 * <li>By definition, all members (peaks) of a column are aligned.
 * <li>The <b>start column</b> is the column that really indicates the left side of a system, just
 * before the staff lines begin.
 * <li>A column is said to be <b>full</b> when it contains a peak for every staff in its
 * containing system.
 * On the left side of a system, before the start column, we can have partial (non-full) columns
 * made of square / bracket / brace segments.
 * The start column, as well as all the other columns on right side of the start column until the
 * system right end, must be full.
 * <li>A column is said to be <b>fully-connected</b> when it is full and all its peaks are linked
 * by concrete connections. The system start column must be a fully-connected column.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class BarColumn
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BarColumn.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The sheet graph of peaks. */
    private final PeakGraph peakGraph;

    /** The containing system. */
    private final SystemInfo system;

    /** One peak item per staff. */
    private final StaffPeak[] peaks;

    /** De-skewed column abscissa. */
    private Double xDsk;

    /** Mean width. */
    private Double width;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BarColumn} object.
     *
     * @param system    the containing system
     * @param peakGraph the sheet graph of peaks
     */
    public BarColumn (SystemInfo system,
                      PeakGraph peakGraph)
    {
        this.peakGraph = peakGraph;
        this.system = system;
        peaks = new StaffPeak[system.getStaves().size()];
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addChain //
    //----------//
    public void addChain (Chain chain)
    {
        for (StaffPeak peak : chain) {
            addPeak(peak);
        }
    }

    //---------//
    // addPeak //
    //---------//
    public void addPeak (StaffPeak peak)
    {
        int idx = peak.getStaff().getId() - system.getFirstStaff().getId();
        peaks[idx] = peak;
        peak.setColumn(this);

        // Invalidate cached data
        xDsk = null;
        width = null;
    }

    //------------//
    // canInclude //
    //------------//
    public boolean canInclude (Chain chain)
    {
        // Make sure we have room for the provided chain candidate
        for (StaffPeak peak : chain) {
            int idx = peak.getStaff().getId() - system.getFirstStaff().getId();

            if (peaks[idx] != null) {
                logger.debug("{} cannot include {}", this, chain);

                return false;
            }
        }

        return true;
    }

    //----------//
    // getPeaks //
    //----------//
    public StaffPeak[] getPeaks ()
    {
        return peaks;
    }

    //----------//
    // getWidth //
    //----------//
    public double getWidth ()
    {
        if (width == null) {
            int nb = 0;
            double sum = 0;

            for (StaffPeak peak : peaks) {
                if (peak != null) {
                    sum += peak.getWidth();
                    nb++;
                }
            }

            width = sum / nb;
        }

        return width;
    }

    //---------//
    // getXDsk //
    //---------//
    public double getXDsk ()
    {
        if (xDsk == null) {
            int nb = 0;
            double sum = 0;

            for (StaffPeak peak : peaks) {
                if (peak != null) {
                    sum += peak.getDeskewedAbscissa();
                    nb++;
                }
            }

            xDsk = sum / nb;
        }

        return xDsk;
    }

    //--------//
    // isFull //
    //--------//
    public boolean isFull ()
    {
        return computeStatus();
    }

    //------------------//
    // isFullyConnected //
    //------------------//
    /**
     * Report whether this column is full and all its peaks are connected.
     *
     * @return true if full and connected
     */
    public boolean isFullyConnected ()
    {
        if (!isFull()) {
            return false;
        }

        boolean[] connected = new boolean[peaks.length - 1];
        Arrays.fill(connected, false);

        for (int i = 0; i < (peaks.length - 1); i++) {
            StaffPeak top = peaks[i];
            StaffPeak bottom = peaks[i + 1];

            for (BarAlignment align : peakGraph.getAllEdges(top, bottom)) {
                if (align instanceof BarConnection) {
                    connected[i] = true;
                }
            }
        }

        for (boolean bool : connected) {
            if (!bool) {
                return false;
            }
        }

        return true;
    }

    //---------//
    // isStart //
    //---------//
    /**
     * Report whether this column is the "start column" of its system.
     *
     * @return true if this is the start column
     */
    public boolean isStart ()
    {
        final StaffPeak topPeak = peaks[0];

        return (topPeak != null) && topPeak.isStaffEnd(HorizontalSide.LEFT);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Column ");

        for (int i = 0; i < peaks.length; i++) {
            StaffPeak peak = peaks[i];

            if (i > 0) {
                // Print link if any
                BarAlignment link = (BarAlignment) peakGraph.getEdge(peaks[i - 1], peak);

                if (link == null) {
                    sb.append(" X ");
                } else if (link instanceof BarConnection) {
                    sb.append(" | ");
                } else {
                    sb.append(" . ");
                }
            }

            sb.append((peak != null) ? peak : "...");
        }

        return sb.toString();
    }

    //---------------//
    // computeStatus //
    //---------------//
    private boolean computeStatus ()
    {
        int nb = 0;

        for (StaffPeak peak : peaks) {
            if ((peak != null) && !peak.isBrace()) {
                nb++;
            }
        }

        return nb == peaks.length;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Chain //
    //-------//
    /**
     * A chain represent a vertical sequence of peaks linked by alignment or connection.
     */
    public static class Chain
            extends TreeSet<StaffPeak>
    {
        //~ Static fields/initializers -------------------------------------------------------------

        /** To sort by (de-skewed) abscissa. */
        public static final Comparator<Chain> byAbscissa = new Comparator<Chain>()
        {
            @Override
            public int compare (Chain c1,
                                Chain c2)
            {
                return Double.compare(
                        c1.first().getDeskewedAbscissa(),
                        c2.first().getDeskewedAbscissa());
            }
        };

        //~ Constructors ---------------------------------------------------------------------------
        public Chain (Collection<StaffPeak> peaks)
        {
            addAll(peaks);
        }
    }
}
//    //-------------------//
//    // getTargetAbscissa //
//    //-------------------//
//    /**
//     * Report an estimate of abscissa for (missing) peak at provided index.
//     *
//     * @param index the provided index
//     * @return the target abscissa value
//     */
//    public double getTargetAbscissa (int index)
//    {
//        // First peak available above?
//        Integer i1 = null;
//
//        for (int i = index - 1; i >= 0; i--) {
//            if (peaks[i] != null) {
//                i1 = i;
//
//                break;
//            }
//        }
//
//        // First peak available below?
//        Integer i2 = null;
//
//        for (int i = index + 1; i < peaks.length; i++) {
//            if (peaks[i] != null) {
//                i2 = i;
//
//                break;
//            }
//        }
//
//        final Staff staff = system.getStaves().get(index);
//        final LineInfo firstLine = staff.getFirstLine();
//        final LineInfo lastLine = staff.getLastLine();
//
//        if (i1 != null) {
//            StaffPeak p1 = peaks[i1];
//            double p1x = (p1.getStart() + p1.getStop()) / 2.0;
//            double p1y = (p1.getTop() + p1.getBottom()) / 2.0;
//
//            if (i2 != null) {
//                StaffPeak p2 = peaks[i2];
//                double p2x = (p2.getStart() + p2.getStop()) / 2.0;
//
//                // Singular case
//                if (Math.abs(p2x - p1x) < 0.1) {
//                    return (p1x + p2x) / 2.0;
//                }
//
//                double p2y = (p2.getTop() + p2.getBottom()) / 2.0;
//                double y1 = (firstLine.yAt(p1x) + lastLine.yAt(p1x)) / 2.0;
//                double y2 = (firstLine.yAt(p2x) + lastLine.yAt(p2x)) / 2.0;
//                Point2D cross = LineUtil.intersection(p1x, y1, p2x, y2, p1x, p1y, p2x, p2y);
//
//                return cross.getX(); // interpolate
//            } else {
//                double y1 = (firstLine.yAt(p1x) + lastLine.yAt(p1x)) / 2.0;
//                Point2D dsk1 = p1.getDeskewedCenter();
//                Point2D dsk = new Point2D.Double(dsk1.getX(), dsk1.getY() + (y1 - p1y));
//
//                return system.getSheet().getSkew().skewed(dsk).getX(); // extrapolate
//            }
//        } else {
//            StaffPeak p2 = peaks[i2];
//            double p2x = (p2.getStart() + p2.getStop()) / 2.0;
//            double p2y = (p2.getTop() + p2.getBottom()) / 2.0;
//            double y2 = (firstLine.yAt(p2x) + lastLine.yAt(p2x)) / 2.0;
//            Point2D dsk2 = p2.getDeskewedCenter();
//            Point2D dsk = new Point2D.Double(dsk2.getX(), dsk2.getY() + (y2 - p2y));
//
//            return system.getSheet().getSkew().skewed(dsk).getX(); // extrapolate
//        }
//    }
//
