//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B a s i c T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.OMR;

import org.jdesktop.application.Task;

/**
 * Class {@code BasicTask} is an Application Framework Task for Audiveris application.
 *
 * @author Hervé Bitteur
 *
 * @param <T> the result type returned by this {@code SwingWorker's}
 *            {@code doInBackground} and {@code get} methods
 * @param <V> the type used for carrying out intermediate results by this
 *            {@code SwingWorker's} {@code publish} and {@code process} methods
 */
public abstract class BasicTask<T, V>
        extends Task<T, V>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Audiveris application is injected into this task.
     */
    public BasicTask ()
    {
        super(OMR.gui.getApplication());
    }
}
