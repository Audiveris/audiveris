//----------------------------------------------------------------------------//
//                                                                            //
//                           H o r i z o n t a l s                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.facets.Glyph;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code Horizontals} gathers horizontal dashes in the given
 * sheet. We use it for all horizontal glyphs (ledgers of course, but also
 * legato signs or alternate endings)
 *
 * @author Hervé Bitteur
 */
public class Horizontals
{
    //~ Instance fields --------------------------------------------------------

    /** The collection of endings found */
    private final List<Ending> endings = new ArrayList<>();

    /** The collection of ledgers found */
    private final List<Ledger> ledgers = new ArrayList<>();

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new Horizontals object.
     */
    public Horizontals ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // getEndingOf //
    //-------------//
    /**
     * Report the ending which is based on the provided glyph
     *
     * @param glyph the provided glyph
     * @return the encapsulating ending, or null if not found
     */
    public Ending getEndingOf (Glyph glyph)
    {
        for (Ending ending : endings) {
            if (ending.isDashOf(glyph)) {
                return ending;
            }
        }

        return null;
    }

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

    //-------------//
    // getLedgerOf //
    //-------------//
    /**
     * Report the ledger which is based on the provided glyph
     *
     * @param glyph the provided glyph
     * @return the encapsulating ledger, or null if not found
     */
    public Ledger getLedgerOf (Glyph glyph)
    {
        for (Ledger ledger : ledgers) {
            if (ledger.isDashOf(glyph)) {
                return ledger;
            }
        }

        return null;
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
