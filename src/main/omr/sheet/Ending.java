//----------------------------------------------------------------------------//
//                                                                            //
//                                E n d i n g                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.stick.Stick;

/**
 * Class <code>Ending</code> is a physical {@link Dash} that is the horizontal
 * part of an alternate ending.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Ending
    extends Dash
{
    //~ Constructors -----------------------------------------------------------

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
