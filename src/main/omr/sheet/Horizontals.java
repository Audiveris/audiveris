//----------------------------------------------------------------------------//
//                                                                            //
//                           H o r i z o n t a l s                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.stick.Stick;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>Horizontals</code> gathers horizontal dashes in the given
 * sheet. We use it for all horizontal glyphs (ledgers of course, but also
 * legato signs or alternate endings)
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Horizontals
    implements java.io.Serializable
{
    //~ Instance fields --------------------------------------------------------

    private final List<Ending> endings = new ArrayList<Ending>();

    // The whole list of horizontals (ledgers, legato signs, endings) found
    private final List<Ledger> ledgers = new ArrayList<Ledger>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Horizontals object.
     */
    public Horizontals ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // getEndings //
    //------------//
    /**
     * Exports the list of horizontals that have been recognized as endings
     *
     * @return the list of ledger sticks
     */
    public List<Ending> getEndings ()
    {
        return endings;
    }

    //------------//
    // getLedgers //
    //------------//
    /**
     * Exports the list of horizontals that have been recognized as ledgers
     *
     * @return the list of ledgers
     */
    public List<Ledger> getLedgers ()
    {
        return ledgers;
    }
}
