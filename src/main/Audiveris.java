//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       A u d i v e r i s                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>

/**
 * Class {@code Audiveris} is simply a convenient entry point to OMR, which
 * delegates the call to {@link omr.Main#main}.
 *
 * @author Hervé Bitteur
 */
public final class Audiveris
{
    //~ Constructors -------------------------------------------------------------------------------

    /** To avoid instantiation. */
    private Audiveris ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // main //
    //------//
    /**
     * The main entry point, which just calls {@link omr.Main#main}.
     *
     * @param args These args are simply passed to Main
     */
    public static void main (final String[] args)
    {
        omr.Main.main(args);
    }
}
