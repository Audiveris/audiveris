//-----------------------------------------------------------------------//
//                                                                       //
//                           A u d i v e r i s                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$

/**
 * Class <code>Audiveris</code> is simply the entry point to OMR.
 */
public class Audiveris
{
    //~ Constructors ------------------------------------------------------

    // To avoid instantiation
    private Audiveris ()
    {
    }

    //~ Methods -----------------------------------------------------------

    /**
     * The main entry point, which just calls omr.Main.main()
     *
     * @param args These args are simply passed to Main
     */
    public static void main (String[] args)
    {
        omr.Main.main(args);
    }
}
