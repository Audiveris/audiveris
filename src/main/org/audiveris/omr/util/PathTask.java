//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P a t h T a s k                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import java.nio.file.Path;

/**
 * Class <code>PathTask</code> is a BasicTask that operates on a path.
 *
 * @param <T> the result type returned by this <code>SwingWorker's</code>
 *            <code>doInBackground</code> and <code>get</code> methods
 * @param <V> the type used for carrying out intermediate results by this
 *            <code>SwingWorker's</code> <code>publish</code> and <code>process</code> methods
 * @author Hervé Bitteur
 */
public abstract class PathTask<T, V>
        extends BasicTask<T, V>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Underlying path. */
    protected Path path;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>PathTask</code> object.
     */
    public PathTask ()
    {
    }

    /**
     * Creates a new <code>PathTask</code> object.
     *
     * @param path the related path
     */
    protected PathTask (Path path)
    {
        this.path = path;
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Set the path value.
     *
     * @param path the path used by the task
     */
    public void setPath (Path path)
    {
        this.path = path;
    }
}
