//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       C h e c k a b l e                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.check;

import omr.util.Vip;

/**
 * Interface {@code Checkable} describes a class of objects that can
 * be checked, generally via a suite of individual checks.
 *
 * @author Hervé Bitteur
 */
public interface Checkable
        extends Vip
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Store the check failure directly into the checkable entity.
     *
     * @param failure the failure to be stored
     */
    void addFailure (Failure failure);
}
