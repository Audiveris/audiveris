//-----------------------------------------------------------------------//
//                                                                       //
//                              U I T e s t                              //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.selection.SelectionManager;
import omr.util.Logger;

public class UITest
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(UITest.class);

    //~ Instance variables ------------------------------------------------

    //~ Constructors ------------------------------------------------------

    private UITest()
    {
    }


    //~ Methods -----------------------------------------------------------

    public static void test()
    {
        SelectionManager.dumpAllSelections();
    }
}
