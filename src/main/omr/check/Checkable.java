//----------------------------------------------------------------------------//
//                                                                            //
//                             C h e c k a b l e                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.check;

import omr.util.Vip;

/**
 * Interface <code>Checkable</code> describes a class that may be checked and
 * then assigned a result for that check.
 *
 * @author Hervé Bitteur
 */
public interface Checkable
    extends Vip
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Store the check result directly into the checkable entity.
     *
     * @param result the result to be stored
     */
    void setResult (Result result);
}
