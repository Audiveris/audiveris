//-----------------------------------------------------------------------//
//                                                                       //
//                           C h e c k a b l e                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.check;

/**
 * Interface <code>Checkable</code> describes a class that may be checked and
 * then assigned a result for that check.
 */
public interface Checkable
{
    //~ Methods --------------------------------------------------------------

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
