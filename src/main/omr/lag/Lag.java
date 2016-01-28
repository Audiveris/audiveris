//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             L a g                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.run.Oriented;
import omr.run.Run;
import omr.run.RunTable;

import omr.ui.selection.SelectionService;

import omr.util.EntityIndex;

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
