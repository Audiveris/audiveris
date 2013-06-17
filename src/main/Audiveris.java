//----------------------------------------------------------------------------//
//                                                                            //
//                             A u d i v e r i s                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
import omr.WellKnowns;

/**
 * Class {@code Audiveris} is simply the entry point to OMR, which
 * delegates the call to {@link omr.Main#doMain}.
 *
 * @author Hervé Bitteur
 */
public final class Audiveris
{
    //~ Constructors -----------------------------------------------------------

    //-----------//
    // Audiveris //
    //-----------//
    /** To avoid instantiation */
    private Audiveris ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // main //
    //------//
    /**
     * The main entry point, which just calls {@link omr.Main#doMain}.
     *
     * @param args These args are simply passed to Main
     */
    public static void main (final String[] args)
    {
        // We need class WellKnowns to be elaborated before class Main
        WellKnowns.ensureLoaded();

        // Then we call Main...
        omr.Main.doMain(args);
    }
}
