//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             L a g                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.lag;

import org.audiveris.omr.run.Oriented;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.util.EntityIndex;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.Set;

/**
 * Interface {@code Lag} defines a graph of {@link Section} instances (sets of
 * contiguous runs with compatible lengths), linked by Junctions when there is no more
 * contiguous run or when the compatibility is no longer met.
 * <p>
 * Sections are thus vertices of the graph, while junctions are directed edges between sections. All
 * the sections (and runs) have the same orientation shared by the lag.
 * <p>
 * A lag may have a related UI selection service accessible through {@link #getEntityService}.
 * <p>
 * Run selection is provided by a separate selection service hosted by the underlying RunTable
 * instance. For convenience, one can use the method {@link #getRunService()} to get access to this
 * run service.
 *
 * @author Hervé Bitteur
 */
public interface Lag
        extends EntityIndex<Section>, Oriented
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Include the content of runs table to the lag.
     *
     * @param runTable the related table of runs
     */
    void addRunTable (RunTable runTable);

    /**
     * Report the run found at given coordinates, if any.
     *
     * @param x absolute abscissa
     * @param y absolute ordinate
     * @return the run found, or null otherwise
     */
    Run getRunAt (int x,
                  int y);

    /**
     * Report the selection service for runs.
     *
     * @return the run selection service
     */
    SelectionService getRunService ();

    /**
     * Report the underlying runs table, if any.
     *
     * @return the table of runs or null
     */
    RunTable getRunTable ();

    /**
     * Lookup for lag sections that are <b>intersected</b> by the
     * provided rectangle.
     * Specific sections are not considered.
     *
     * @param rect the given rectangle
     * @return the set of lag sections intersected, which may be empty
     */
    Set<Section> intersectedSections (Rectangle rect);

    /**
     * Remove the provided sections from the lag.
     *
     * @param sections the collection of sections to remove
     */
    void removeSections (Collection<Section> sections);

    /**
     * Use the provided runs table as the lag underlying table.
     *
     * @param runsTable the populated runs
     */
    void setRuns (RunTable runsTable);
}
