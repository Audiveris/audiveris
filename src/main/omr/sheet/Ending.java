//-----------------------------------------------------------------------//
//                                                                       //
//                              E n d i n g                              //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.stick.Stick;

/**
 * Class <code>Ending</code> is a physical {@link Dash} that is the
 * horizontal part of an alternate ending.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Ending
    extends Dash
{
    //~ Constructors ------------------------------------------------------

    //--------//
    // Ending //
    //--------//
    /**
     * Create an Ending entity, with its underlying horizontal stick.
     *
     * @param stick the related stick
     */
    public Ending (Stick stick)
    {
        super(stick);
    }
}
