//----------------------------------------------------------------------------//
//                                                                            //
//                             B a s i c T a s k                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.util;

import omr.ui.MainGui;

import org.jdesktop.application.Task;

/**
 * Class <code>BasicTask</code> is an Application Framework Task for Audiveris
 * application, with no generic parameters to handle
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class BasicTask
    extends Task<Void, Void>
{
    //~ Constructors -----------------------------------------------------------

    //-----------//
    // BasicTask //
    //-----------//
    /**
     * Audiveris application is injected into this task
     */
    public BasicTask ()
    {
        super(MainGui.getInstance());
    }
}
