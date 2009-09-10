//----------------------------------------------------------------------------//
//                                                                            //
//                                L e d g e r                                 //
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
 * Class <code>Ledger</code> is a physical {@link Dash} which is logically a
 * Ledger (to represents portions of virtual staff lines)
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Ledger
    extends Dash
{
    //~ Constructors -----------------------------------------------------------

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
