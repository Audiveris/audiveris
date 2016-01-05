//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P a t h T a s k                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.nio.file.Path;

/**
 * Class {@code PathTask} is a BasicTask that operates on a path.
 *
 * @author Hervé Bitteur
 */
public abstract class PathTask
        extends BasicTask
{
    //~ Instance fields ----------------------------------------------------------------------------

    protected Path path;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PathTask} object.
     */
    public PathTask ()
    {
    }

    /**
     * Creates a new {@code PathTask} object.
     *
     * @param path DOCUMENT ME!
     */
    protected PathTask (Path path)
    {
        this.path = path;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void setPath (Path path)
    {
        this.path = path;
    }
}
