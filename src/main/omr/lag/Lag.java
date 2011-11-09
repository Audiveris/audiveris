//----------------------------------------------------------------------------//
//                                                                            //
//                                   L a g                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.graph.Digraph;

import omr.run.Oriented;
import omr.run.Run;
import omr.run.RunsTable;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.selection.SectionEvent;
import omr.selection.SectionIdEvent;
import omr.selection.SectionSetEvent;
import omr.selection.SelectionService;

import omr.util.Predicate;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Interface <code>Lag</code> defines a graph of {@link Section} instances
 * (sets of contiguous runs with compatible lengths), linked by Junctions when
 * there is no more contiguous run or when the compatibility is no longer met.
 *
 * Sections are thus vertices of the graph, while junctions are directed edges
 * between sections. All the sections (and runs) have the same orientation
 * shared by the lag.
 *
 * <p>A lag may have a related UI selection service accessible through {@link
 * #getSectionService}. This selection service handles Section, SectionId and
 * SectionSet events. The {@link #getSelectedSection} and
 * {@link #getSelectedSectionSet} methods are just convenient ways to retrieve
 * the last selected section, sectionId or sectionSet from the lag selection
 * service.</p>
 *
 * <p>Run selection is provided by a separate selection service hosted by the
 * underlying RunsTable instance. For convenience, one can use the method
 * {@link #getRunService()} to get access to this run service.</p>
 *
 * @author Hervé Bitteur
 */
public interface Lag
    extends Digraph<Lag, Section>, Oriented
{
    //~ Static fields/initializers ---------------------------------------------

    /** Events that can be published on lag section service */
    static final Class[] eventsWritten = new Class[] {
                                             SectionIdEvent.class,
                                             SectionEvent.class,
                                             SectionSetEvent.class
                                         };

    //~ Methods ----------------------------------------------------------------

    /**
     * Return the first run (the one with minimum position, then with minimum
     * coordinate) found in the given rectangle
     *
     * @param coordMin min abscissa for horizontal lag
     * @param coordMax max abscissa for horizontal lag
     * @param posMin   min ordinate for horizontal lag
     * @param posMax   max ordinate for horizontal lag
     *
     * @return the run, or null if none found
     */
    Run getFirstRectRun (int coordMin,
                         int coordMax,
                         int posMin,
                         int posMax);

    /**
     * Report the run found at given coordinates, if any.
     *
     * @param x absolute abscissa
     * @param y absolute ordinate
     * @return  the run found, or null otherwise
     */
    Run getRunAt (int x,
                  int y);

    /**
     * Report the selection service for runs
     * @return the run selection service
     */
    SelectionService getRunService ();

    /**
     * Use the provided runs table as the lag underlying table
     * @param runsTable the populated runs
     */
    void setRuns (RunsTable runsTable);

    /**
     * Report the provided runs table
     * @return the table of runs
     */
    RunsTable getRuns ();

    /**
     * Report the section selection service
     * @return the section selection service
     */
    SelectionService getSectionService ();

    /**
     * Return a view of the collection of sections that are currently part of
     * this lag
     *
     * @return the sections collection
     */
    Collection<Section> getSections ();

    /**
     * Return the collection of sections which intersect the provided rectangle
     *
     * @param rect the rectangular area to be checked, specified in the usual
     * (coord, pos) form.
     *
     * @return the list of sections found (may be empty)
     */
    List<Section> getSectionsIn (Rectangle rect);

    /**
     * Convenient method to report the UI currently selected Section, if any, in
     * this lag
     * @return the UI selected section, or null if none
     */
    Section getSelectedSection ();

    /**
     * Convenient method to report the UI currently selected set of Sections,
     * if any, in this lag
     * @return the UI selected section set, or null if none
     */
    Set<Section> getSelectedSectionSet ();

    /**
     * Inject dependency about other services for lag
     * @param locationService the location service to read & write
     * @param sceneService the glyphservice to write
     */
    void setServices (SelectionService locationService,
                      SelectionService sceneService);

    /**
     * Include the content of runs table to the lag.
     * @param runsTable the populated runs
     */
    void addRuns (RunsTable runsTable);

    /**
     * Create a section in the lag (using the defined vertexClass)
     *
     * @param firstPos the starting position of the section
     * @param firstRun the very first run of the section
     *
     * @return the created section
     */
    Section createSection (int firstPos,
                           Run firstRun);

    /**
     * Lookup for sections that are contained in the provided
     * rectangle. Specific sections are not considered.
     *
     * @param rect the given rectangle
     *
     * @return the set of sections found, which may be empty
     */
    Set<Section> lookupSections (PixelRectangle rect);

    /**
     * Purge the lag of all sections for which provided predicate applies
     *
     * @param predicate means to specify whether a section applies for purge
     *
     * @return the list of sections purged in this call
     */
    List<Section> purgeSections (Predicate<Section> predicate);

    /**
     * Purge the lag from section with a too small foreground weight, provided
     * they do not cut larger glyphs
     *
     * @param minForeWeight
     * @return the purged sections
     */
    List<Section> purgeTinySections (final int minForeWeight);
}
