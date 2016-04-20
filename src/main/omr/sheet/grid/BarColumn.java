//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B a r C o l u m n                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;

import org.jgrapht.Graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

/**
 * Class {@code BarColumn} handles a column of peaks as barline candidates, since
 * column consistency helps handle candidates properly.
 *
 * @author Hervé Bitteur
 */
public class BarColumn
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BarColumn.class);

    //~ Enumerations -------------------------------------------------------------------------------
    public enum Status
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** A full barline with one peak on each staff. */
        FULL,
        /** Some peaks missing. */
        PART,
        /** Not a full barline. Group / brace on left side, otherwise plain garbage. */
        NONE;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** The containing system. */
    final SystemInfo system;

    /** One peak item per staff. */
    private final StaffPeak[] peaks;

    /** De-skewed column abscissa. */
    private Double xDsk;

    /** Mean width. */
    private Double width;

    /** Current status. */
    private Status status;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BarColumn} object.
     *
     * @param system the containing system
     */
    public BarColumn (SystemInfo system)
    {
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
        xDsk = null;
        width = null;
        status = null;
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

    //------------//
    // dumpString //
    //-----------//
    public String dumpString (Graph<StaffPeak, BarAlignment> peakGraph)
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
                    sb.append(" ? ");
                }
            }

            sb.append((peak != null) ? peak : "...");
        }

        return sb.toString();
    }

    //----------//
    // getPeaks //
    //----------//
    public StaffPeak[] getPeaks ()
    {
        return peaks;
    }

    //-----------//
    // getStatus //
    //-----------//
    public Status getStatus ()
    {
        if (status == null) {
            status = computeStatus();
        }

        return status;
    }

    //-----------------//
    // getStatusString //
    //-----------------//
    public String getStatusString ()
    {
        switch (getStatus()) {
        case FULL:
            return "---";

        case PART:
            return " ? ";

        case NONE:
        default:
            return "   ";
        }
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
        return getStatus() == Status.FULL;
    }

    //------------------//
    // isFullyConnected //
    //------------------//
    /**
     * Report whether this column is full and all its peaks are connected.
     *
     * @param peakGraph the graph of all peaks connections
     * @return true if full and connected
     */
    public boolean isFullyConnected (Graph<StaffPeak, BarAlignment> peakGraph)
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

        for (StaffPeak peak : peaks) {
            sb.append(" | ").append(peak);
        }

        return sb.toString();
    }

    //---------------//
    // computeStatus //
    //---------------//
    private Status computeStatus ()
    {
        int nb = 0;

        for (StaffPeak peak : peaks) {
            if (peak != null) {
                nb++;
            }
        }

        if (nb == peaks.length) {
            return Status.FULL; // Perfect
        } else if (nb < (peaks.length / 2)) {
            return Status.NONE; // Minority number of peaks in column, let's give up
        } else {
            return Status.PART; // Not perfect, but recovery could be tried?
        }
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
            implements Comparable<Chain>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public Chain (Collection<StaffPeak> peaks)
        {
            addAll(peaks);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int compareTo (Chain that)
        {
            return Double.compare(
                    this.first().getDeskewedAbscissa(),
                    that.first().getDeskewedAbscissa());
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
