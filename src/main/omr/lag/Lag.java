//-----------------------------------------------------------------------//
//                                                                       //
//                                 L a g                                 //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.lag;

import omr.graph.Digraph;
import omr.graph.DigraphView;
import omr.util.Logger;
import omr.util.Predicate;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class <code>Lag</code> handles a graph of class {@link Section} (sets of
 * contiguous runs with compatible lengths), linked by Junctions when there
 * is no more contiguous run or when the compatibility is no longer met.
 * Sections are thus vertices of the graph, while junctions are directed
 * edges between sections.
 *
 * @param <L> precise lag (sub)type
 * @param <S> precise section (sub)type
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Lag <L extends Lag <L, S>,
                  S extends Section>
    extends Digraph<L, S>
    implements Oriented
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Lag.class);

    //~ Instance variables ------------------------------------------------

    /** Orientation of the lag */
    protected final Oriented orientation;

    /**
     * List of Runs found in each column. So this is a list of lists of
     * Runs.  It will be allocated in the adapter
     */
    protected List<List<Run>> runs;

    //~ Constructors ------------------------------------------------------

    //-----//
    // Lag //
    //-----//
    /**
     * Constructor with specified orientation
     */
    protected Lag (Oriented orientation)
    {
        this.orientation = orientation;
    }

    //~ Methods -----------------------------------------------------------

    //------------//
    // isVertical //
    //------------//
    public boolean isVertical ()
    {
        return orientation.isVertical();
    }

    //-----------//
    // switchRef //
    //-----------//
    public Point switchRef (Point cp,
                            Point xy)
    {
        return orientation.switchRef(cp, xy);
    }

    //-----------//
    // switchRef //
    //-----------//
    public Rectangle switchRef (Rectangle cplt,
                                Rectangle xywh)
    {
        return orientation.switchRef(cplt, xywh);
    }

    //---------------//
    // createSection //
    //---------------//
    /**
     * Create a section in the lag (using the defined vertexClass)
     *
     * @param firstPos the starting position of the section
     * @param firstRun the very first run of the section
     *
     * @return the created section
     */
    public S createSection (int firstPos,
                            Run firstRun)
    {
        if (firstRun == null) {
            throw new IllegalArgumentException("null first run");
        }

        S section = createVertex();
        section.setFirstPos(firstPos);
        section.append(firstRun);

        return section;
    }

    //---------//
    // setRuns //
    //---------//
    /**
     * Assign the populated runs to the lag. Package private access is
     * provided for LagBuilder
     *
     * @param runs the populated runs
     */
    void setRuns (List<List<Run>> runs)
    {
        this.runs = runs;
    }

    //-----------------//
    // getFirstRectRun //
    //-----------------//
    /**
     * Return the first run (the one with minimum position, then with
     * minimum coordinate) found in the given rectangle
     *
     * @param coordMin min abscissa for horizontal lag
     * @param coordMax max abscissa for horizontal lag
     * @param posMin   min ordinate for horizontal lag
     * @param posMax   max ordinate for horizontal lag
     *
     * @return the run, or null if none found
     */
    public Run getFirstRectRun (int coordMin,
                                int coordMax,
                                int posMin,
                                int posMax)
    {
        Run best = null;

        // Relevant portion of runs
        List<List<Run>> subList = runs.subList(posMin, posMax + 1);
        for (List<Run> runList : subList) {
            for (Run run : runList) {

                if (run.getStart() > coordMax) {
                    break; // Over for this column
                }

                if (run.getStop() < coordMin) {
                    continue;
                }

                if (best == null) {
                    best = run;
                } else if (run.getStart() < best.getStart()) {
                    best = run;
                }
            }
        }

        return best;
    }

    //-------------//
    // getSections //
    //-------------//
    /**
     * Return a view of the collection of sections that are currently part
     * of this lag
     *
     * @return the sections collection
     */
    public Collection<S> getSections ()
    {
        return vertices.values();
    }

    //---------------//
    // getSectionsIn //
    //---------------//
    /**
     * Return the collection of sections which intersect the provided
     * rectangle
     *
     * @param rect the rectangular area to be checked, specified in the
     *             usual (coord, pos) form.
     *
     * @return the list of sections found (may be empty)
     */
    public List<S> getSectionsIn (Rectangle rect)
    {
        int maxPos = rect.y + rect.height;
        List<S> found = new ArrayList<S>();

        // Iterate on (all?) sections
        for (S section : getSections()) {
            // Detect a true ending situation (not too bad)
            // OOPS: ensure the collection of sections is still ordered
            // according to their position value !
//             if (section.getFirstPos() > maxPos) {
//                 break;
//             }

            if (rect.intersects(section.getBounds())) {
                found.add(section);
            }
        }

        return found;
    }

    //---------------//
    // purgeSections //
    //---------------//
    /**
     * Purge the lag of all sections for which provided predicate applies
     *
     * @param predicate means to specify whether a section applies for
     * purge
     *
     * @return the list of sections purged in this call
     */
    public List<S> purgeSections (Predicate<Section> predicate)
    {
        // List of sections to be purged (to avoid concurrent
        // modifications)
        List<S> purges = new ArrayList<S>(2000);

        // Iterate on all sections
        for (S section : getSections()) {
            // Check predicate on the current section
            if (predicate.check(section)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Purging " + section);
                }

                purges.add(section);
            }
        }

        // Now, actually perform the needed removals
        for (S section : purges) {
            section.delete();
        }

        // Return the sections purged
        return purges;
    }

    //-------------------//
    // purgeTinySections //
    //-------------------//
    /**
     * Purge the lag from section with a too small foreground weight,
     * provided they do not cut larger glyphs
     *
     * @return the purged sections
     */
    public List<S> purgeTinySections (final int minForeWeight)
    {
        return purgeSections(new Predicate<Section>()
        {
            public boolean check (Section section)
            {
                return (section.getForeWeight() < minForeWeight)
                       && ((section.getInDegree() == 0)
                           || (section.getOutDegree() == 0));
            }
        });
    }

    //----------//
    // toString //
    //----------//
    /**
     * Return a readable description
     *
     * @return the descriptive string
     */
    @Override
        public String toString ()
    {
        StringBuffer sb = new StringBuffer(256);

        sb.append(super.toString());

        if (this.getClass().getName() == Lag.class.getName()) {
            sb.append("}");
        }

        return sb.toString();
    }

    //-----------//
    // getPrefix //
    //-----------//
    /**
     * Return a distinctive string, to be used as a prefix in toString()
     * for example.
     *
     * @return the prefix string
     */
    @Override
        protected String getPrefix ()
    {
        return "Lag";
    }
}
