//----------------------------------------------------------------------------//
//                                                                            //
//                             C h e c k a b l e                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.check;


/**
 * Interface <code>Checkable</code> describes a class that may be checked and
 * then assigned a result for that check.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface Checkable
{
    //~ Methods ----------------------------------------------------------------

    //-----------//
    // setResult //
    //-----------//
    /**
     * Store the check result directly into the checkable entity.
     *
     * @param result the result to be stored
     */
    void setResult (Result result);
}
