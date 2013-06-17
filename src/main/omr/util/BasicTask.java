//----------------------------------------------------------------------------//
//                                                                            //
//                             B a s i c T a s k                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.ui.MainGui;

import org.jdesktop.application.Task;

/**
 * Class {@code BasicTask} is an Application Framework Task for
 * Audiveris application, with no generic parameters to handle
 *
 * @author Hervé Bitteur
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
