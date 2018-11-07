//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B a s i c T a s k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.util;

import org.audiveris.omr.ui.OmrGui;

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

    /**
     * Audiveris application is injected into this task.
     */
    public BasicTask ()
    {
        super(OmrGui.getApplication());
    }
}
