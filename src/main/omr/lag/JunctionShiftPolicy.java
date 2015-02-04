//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                J u n c t i o n S h i f t P o l i c y                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.run.Run;

/**
 * Class {@code JunctionShiftPolicy} defines a junction policy based on the shift
 * between the candidate run and the last run of the section.
 *
 * @author Hervé Bitteur
 */
public class JunctionShiftPolicy
        implements JunctionPolicy
{
    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Maximum value acceptable for shift.
     */
    private final int maxShift;

    //~ Constructors -------------------------------------------------------------------------------
    //---------------------//
    // JunctionShiftPolicy //
    //---------------------//
    /**
     * Creates an instance of policy based on shift of runs.
     *
     * @param maxShift the maximum possible shift between two consecutive rows
     */
    public JunctionShiftPolicy (int maxShift)
    {
        this.maxShift = maxShift;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // consistentRun //
    //---------------//
    /**
     * Check whether the Run is consistent with the provided Section, according to this
     * junction policy, based on run position and last section run position.
     *
     * @param run     the Run candidate
     * @param section the potentially hosting Section
     * @return true if consistent, false otherwise
     */
    @Override
    public boolean consistentRun (Run run,
                                  Section section)
    {
        // Check based on positions of the two runs
        Run last = section.getLastRun();

        return (Math.abs(run.getStart() - last.getStart()) <= maxShift)
               && (Math.abs(run.getStop() - last.getStop()) <= maxShift);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{JunctionShiftPolicy maxShift=" + maxShift + "}";
    }
}
