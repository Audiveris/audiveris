//-----------------------------------------------------------------------//
//                                                                       //
//                              L e d g e r                              //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.sheet;

import omr.stick.Stick;

/**
 * Class <code>Ledger</code> is a physical {@link Dash} which is logically
 * a Ledger (to represents portions of virtual staff lines)
 */
public class Ledger
    extends Dash
{
    //~ Constructors ------------------------------------------------------

    //--------//
    // Ledger //
    //--------//
    /**
     * Create a Ledger, from its underlying horizontal stick
     *
     * @param stick the related retrieved stick
     */
    public Ledger (Stick stick)
    {
        super(stick);
    }
}
