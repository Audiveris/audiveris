//----------------------------------------------------------------------------//
//                                                                            //
//                   J u n c t i o n D e l t a P o l i c y                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.run.Run;

/**
 * Class {@code JunctionDeltaPolicy} defines a junction policy based
 * on the delta between the length of the candidate run and the length
 * of the last run of the section.
 *
 * @author Hervé Bitteur
 */
public class JunctionDeltaPolicy
        implements JunctionPolicy
{
    //~ Instance fields --------------------------------------------------------

    /**
     * Maximum value acceptable for delta length, for a delta criteria
     */
    private final int maxDeltaLength;

    //~ Constructors -----------------------------------------------------------
    //---------------------//
    // JunctionDeltaPolicy //
    //---------------------//
    /**
     * Creates an instance of policy based on delta run length.
     *
     * @param maxDeltaLength the maximum possible length gap between two
     *                       consecutive rows
     */
    public JunctionDeltaPolicy (int maxDeltaLength)
    {
        this.maxDeltaLength = maxDeltaLength;
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // consistentRun //
    //---------------//
    /**
     * Check whether the Run is consistent with the provided Section,
     * according to this junction policy, based on run length and last
     * section run length.
     *
     * @param run     the Run candidate
     * @param section the potentially hosting Section
     * @return true if consistent, false otherwise
     */
    @Override
    public boolean consistentRun (Run run,
                                  Section section)
    {
        // Check based on absolute differences between the two runs
        Run last = section.getLastRun();

        return Math.abs(run.getLength() - last.getLength()) <= maxDeltaLength;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{JunctionDeltaPolicy" + " maxDeltaLength=" + maxDeltaLength
               + "}";
    }
}
