//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             V i p                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

/**
 * Interface {@code Vip} allows to flag an object as a VIP and generally triggers
 * specific debugging printouts related to this object.
 *
 * @author Hervé Bitteur
 */
public interface Vip
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * (Debug) Report whether this object is flagged as VIP
     *
     * @return true if VIP
     */
    boolean isVip ();

    /**
     * (Debug) Assign the VIP flag
     *
     * @param vip true if VIP, false if not
     */
    void setVip (boolean vip);
}
