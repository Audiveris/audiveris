//----------------------------------------------------------------------------//
//                                                                            //
//                     J u n c t i o n A l l P o l i c y                      //
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
 * Class {@code JunctionAllPolicy} defines a junction policy which
 * imposes no condition on run consistency, thus taking all runs
 * considered.
 *
 * @author Hervé Bitteur
 */
public class JunctionAllPolicy
        implements JunctionPolicy
{
    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // JunctionAllPolicy //
    //-------------------//
    /**
     * Creates an instance of this policy.
     */
    public JunctionAllPolicy ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // consistentRun //
    //---------------//
    /**
     * Check whether the Run is consistent with the provided Section,
     * according to this junction policy.
     *
     * @param run     the Run candidate
     * @param section the potentially hosting Section
     * @return always true
     */
    @Override
    public boolean consistentRun (Run run,
                                  Section section)
    {
        return true;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{JunctionAllPolicy}";
    }
}
