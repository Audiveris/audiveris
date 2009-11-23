//----------------------------------------------------------------------------//
//                                                                            //
//                           H o r i z o n t a l s                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

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
{
    //~ Instance fields --------------------------------------------------------

    /** The collection of endings found */
    private final List<Ending> endings = new ArrayList<Ending>();

    /** The collection of ledgers found */
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
