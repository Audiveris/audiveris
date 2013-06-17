//----------------------------------------------------------------------------//
//                                                                            //
//                                   V i p                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

/**
 *
 * Interface {@code Vip} allows to flag an entity as a VIP and
 * generally triggers specific printouts related to this entity.
 *
 * @author Hervé Bitteur
 */
public interface Vip
{
    //~ Methods ----------------------------------------------------------------

    /**
     * (Debug) Report whether this object is flagged as VIP
     *
     * @return true if VIP
     */
    boolean isVip ();

    /**
     * (Debug) Flag this object as VIP
     */
    void setVip ();
}
