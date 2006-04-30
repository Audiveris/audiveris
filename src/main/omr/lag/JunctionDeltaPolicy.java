//-----------------------------------------------------------------------//
//                                                                       //
//                 J u n c t i o n D e l t a P o l i c y                 //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.lag;

/**
 * Class <code>JunctionDeltaPolicy</code> defined a junction policy based
 * on the delta between the length of the candidate run and the length of
 * the last run of the section.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class JunctionDeltaPolicy
    extends JunctionPolicy
{
    //~ Instance variables ------------------------------------------------

    /**
     * Maximum value acceptable for delta length, for a delta criteria
     */
    private final int maxDeltaLength;

    //~ Constructors ------------------------------------------------------

    //---------------------//
    // JunctionDeltaPolicy //
    //---------------------//
    /**
     * Creates an instance of policy based on delta run length
     *
     * @param maxDeltaLength the maximum possible length gap between two
     *                       consecutive rows
     */
    public JunctionDeltaPolicy (int maxDeltaLength)
    {
        this.maxDeltaLength = maxDeltaLength;
    }

    //~ Methods -----------------------------------------------------------

    //---------------//
    // consistentRun //
    //---------------//
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
    /**
     * Report a readable description of the policy
     *
     * @return a descriptive string
     */
    @Override
    public String toString ()
    {
        return "{JunctionDeltaPolicy" + " maxDeltaLength=" + maxDeltaLength
               + "}";
    }
}
