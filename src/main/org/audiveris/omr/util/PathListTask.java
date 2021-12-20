//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     P a t h L i s t T a s k                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class <code>PathListTask</code> is a <code>BasicTask</code> that operates on a list
 * of Path instances.
 * <p>
 * Compared with <code>PathTask</code>, this class is useful when the order of the provided
 * Path instances must be preserved.
 *
 * @param <T> the result type returned by this <code>SwingWorker's</code>
 *            <code>doInBackground</code> and <code>get</code> methods
 * @param <V> the type used for carrying out intermediate results by this
 *            <code>SwingWorker's</code> <code>publish</code> and <code>process</code> methods
 *
 * @author Hervé Bitteur
 */
public abstract class PathListTask<T, V>
        extends BasicTask<T, V>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Underlying path list. */
    protected List<Path> pathList = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>PathListTask</code> object, starting with no path.
     */
    protected PathListTask ()
    {
    }

    /**
     * Creates a new <code>PathListTask</code> object, with a starting collection.
     *
     * @param paths the collection of path's to be added
     */
    protected PathListTask (Collection<? extends Path> paths)
    {
        pathList.addAll(paths);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // addAllPaths //
    //------------//
    /**
     * Add a bunch of path instances to the task list.
     *
     * @param paths the collection of path's to be added
     */
    public void addAllPaths (Collection<? extends Path> paths)
    {
        pathList.addAll(paths);
    }

    //---------//
    // addPath //
    //---------//
    /**
     * Add a path to the task list.
     *
     * @param path the path to be added
     */
    public void addPath (Path path)
    {
        pathList.add(path);
    }
}
