//----------------------------------------------------------------------------//
//                                                                            //
//                     J u n c t i o n A l l P o l i c y                      //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.lag;


/**
 * Class <code>JunctionAllPolicy</code> defines a junction policy which imposes
 * no condition on run consistency, thus taking all runs considered.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class JunctionAllPolicy
    extends JunctionPolicy
{
    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // JunctionAllPolicy //
    //-------------------//
    /**
     * Creates an instance of this policy
     */
    public JunctionAllPolicy ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // consistentRun //
    //---------------//
    /**
     * Check whether the Run is consistent with the provided Section, according
     * to this junction policy
     *
     * @param run the Run candidate
     * @param section the potentially hosting Section
     *
     * @return always true
     */
    public boolean consistentRun (Run     run,
                                  Section section)
    {
        return true;
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
        return "{JunctionNoPolicy}";
    }
}
