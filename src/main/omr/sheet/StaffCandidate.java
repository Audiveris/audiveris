//----------------------------------------------------------------------------//
//                                                                            //
//                        S t a f f C a n d i d a t e                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.log.Logger;

import java.util.List;

/**
 * Class {@code StaffCandidate} is used to build a staff out of a detected
 * sequence of horizontal peaks
 */
public class StaffCandidate
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(StaffCandidate.class);

    //~ Instance fields --------------------------------------------------------

    /** Id meant for debug */
    public final int id;

    /** The sequence of horizontal peaks */
    private final List<Peak> peaks;

    /** The mean vertical interval between the (line) peaks */
    public final double interval;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new StaffCandidate object.
     *
     * @param id identifier for debug
     * @param peaks the sequence of horizontal peaks
     * @param lineInterval the mean interval between the peaks / lines
     */

    //----------------//
    // StaffCandidate //
    //----------------//
    public StaffCandidate (int        id,
                           List<Peak> peaks,
                           double     lineInterval)
    {
        this.id = id;
        this.peaks = peaks;
        this.interval = lineInterval;

        if (logger.isFineEnabled()) {
            logger.fine("new " + this);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getPeaks //
    //----------//
    public List<Peak> getPeaks ()
    {
        return peaks;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());
        sb.append(" #")
          .append(id);

        for (Peak peak : peaks) {
            sb.append(" ")
              .append(peak);
        }

        sb.append(" interval:")
          .append((float) interval);
        sb.append("}");

        return sb.toString();
    }
}
