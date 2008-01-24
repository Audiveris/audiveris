//----------------------------------------------------------------------------//
//                                                                            //
//                                   L a g                                    //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.lag;

import omr.graph.Digraph;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionObserver;

import omr.util.Implement;
import omr.util.Logger;
import omr.util.Predicate;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>Lag</code> handles a graph of class {@link Section} (sets of
 * contiguous runs with compatible lengths), linked by Junctions when there is
 * no more contiguous run or when the compatibility is no longer met.  Sections
 * are thus vertices of the graph, while junctions are directed edges between
 * sections.
 *
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>PIXEL Location (if LOCATION_INIT or LOCATION_ADD)
 * <li>*_SECTION (if SECTION_INIT)
 * <li>*_SECTION_ID
 * </ul>
 *
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>PIXEL Contour
 * <li>*_RUN
 * <li>*_SECTION
 * </ul>
 * </dl>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 * @param <L> precise lag (sub)type
 * @param <S> precise section (sub)type
 */
public class Lag<L extends Lag<L, S>, S extends Section>
    extends Digraph<L, S>
    implements Oriented, SelectionObserver
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Lag.class);

    //~ Instance fields --------------------------------------------------------

    /** Orientation of the lag */
    private final Oriented orientation;

    /**
     * List of Runs found in each column. So this is a list of lists of Runs.
     * It will be allocated in the adapter
     */
    private List<List<Run>> runs;

    /** Selection object where selected pixel location is to be written */
    protected Selection locationSelection;

    /** Selection object where selected Run is to be written */
    protected Selection runSelection;

    /** Selection object where selected Section is to be written */
    protected Selection sectionSelection;

    //~ Constructors -----------------------------------------------------------

    //-----//
    // Lag //
    //-----//
    /**
     * Constructor with specified orientation
     * @param name the distinguished name for this instance
     * @param orientation the desired orientation of the lag
     */
    protected Lag (String   name,
                   Oriented orientation)
    {
        super(name);
        this.orientation = orientation;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getFirstRectRun //
    //-----------------//
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
    public Run getFirstRectRun (int coordMin,
                                int coordMax,
                                int posMin,
                                int posMax)
    {
        Run             best = null;

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

    //----------------------//
    // setLocationSelection //
    //----------------------//
    /**
     * Inject the selection object where location must be written to, when
     * triggered through the update method.
     *
     * @param locationSelection the output selection object
     */
    public void setLocationSelection (Selection locationSelection)
    {
        this.locationSelection = locationSelection;
    }

    //-----------------//
    // setRunSelection //
    //-----------------//
    /**
     * Inject the selection object where run must be written to, when triggered
     * through the update method.
     *
     * @param runSelection the output selection object
     */
    public void setRunSelection (Selection runSelection)
    {
        this.runSelection = runSelection;
    }

    //---------------------//
    // setSectionSelection //
    //---------------------//
    /**
     * Inject the selection object where section must be written to, when
     * triggered through the update method.
     *
     * @param sectionSelection the output selection object
     */
    public void setSectionSelection (Selection sectionSelection)
    {
        this.sectionSelection = sectionSelection;
    }

    //-------------//
    // getSections //
    //-------------//
    /**
     * Return a view of the collection of sections that are currently part of
     * this lag
     *
     * @return the sections collection
     */
    public final Collection<S> getSections ()
    {
        return getVertices();
    }

    //---------------//
    // getSectionsIn //
    //---------------//
    /**
     * Return the collection of sections which intersect the provided rectangle
     *
     * @param rect the rectangular area to be checked, specified in the usual
     *             (coord, pos) form.
     *
     * @return the list of sections found (may be empty)
     */
    public List<S> getSectionsIn (Rectangle rect)
    {
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

    //------------//
    // isVertical //
    //------------//
    /**
     * Predicate on lag orientation
     *
     * @return true if vertical, false if horizontal
     */
    @Implement(Oriented.class)
    public boolean isVertical ()
    {
        return orientation.isVertical();
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

    //-----------------------//
    // invalidateLookupCache //
    //-----------------------//
    /**
     * Forget the last reference to selected Section, since context conditions
     * have changed (typically, the toggle about "specific" sections in a
     * related lag view).
     */
    public void invalidateLookupCache ()
    {
        sectionSelection.setEntity(
            null, /* hint => */
            null, /* notify => */
            false);
    }

    //---------------//
    // lookupSection //
    //---------------//
    /**
     * Given an absolute point, retrieve the <b>first</b> containing section if
     * any, using the provided collection of sections
     *
     * @param collection the desired collection of sections
     * @param pt         coordinates of the given point
     *
     * @return the (first) section found, or null otherwise
     */
    public S lookupSection (Collection<S> collection,
                            Point         pt)
    {
        Point target = switchRef(pt, null); // Involutive!

        // Just in case we have not moved a lot since previous lookup ...
        S foundSection = (S) sectionSelection.getEntity(); // Compiler warning

        if ((foundSection != null) &&
            foundSection.contains(target.x, target.y)) {
            return foundSection;
        }

        // Too bad, let's browse the whole stuff
        foundSection = null;

        for (S section : collection) {
            if (section.contains(target.x, target.y)) {
                foundSection = section;

                break;
            }
        }

        sectionSelection.setEntity(
            foundSection, /* hint => */
            null, /* notify => */
            false);

        return foundSection;
    }

    //---------------//
    // purgeSections //
    //---------------//
    /**
     * Purge the lag of all sections for which provided predicate applies
     *
     * @param predicate means to specify whether a section applies for purge
     *
     * @return the list of sections purged in this call
     */
    public List<S> purgeSections (Predicate<Section> predicate)
    {
        // List of sections to be purged (to avoid concurrent modifications)
        List<S> purges = new ArrayList<S>(2000);

        // Iterate on all sections
        for (S section : getSections()) {
            // Check predicate on the current section
            if (predicate.check(section)) {
                if (logger.isFineEnabled()) {
                    logger.fine("Purging " + section);
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
     * Purge the lag from section with a too small foreground weight, provided
     * they do not cut larger glyphs
     *
     * @return the purged sections
     */
    public List<S> purgeTinySections (final int minForeWeight)
    {
        return purgeSections(
            new Predicate<Section>() {
                    public boolean check (Section section)
                    {
                        return (section.getForeWeight() < minForeWeight) &&
                               ((section.getInDegree() == 0) ||
                               (section.getOutDegree() == 0));
                    }
                });
    }

    //-----------//
    // switchRef //
    //-----------//
    /**
     * Retrieve absolute coordinates of a point, based on lag orientation
     *
     * @param cp the coordinate / position values (relative to lag orientation)
     * @param xy the output variable for absolute abscissa and ordinate values,
     * or null if not yet allocated
     *
     * @return the absolute abscissa and ordinate values
     */
    @Implement(Oriented.class)
    public Point switchRef (Point cp,
                            Point xy)
    {
        return orientation.switchRef(cp, xy);
    }

    //-----------//
    // switchRef //
    //-----------//
    /**
     * Retrieve absolute coordinates of a rectangle, based on lag orientation
     *
     * @param cplt the rectangle values (coordinate, position, length,
     * thickness) relative to lag orientation
     * @param xywh the output variable for absolute rectangle values (abscissa,
     * ordinate, width, height), or null if not yet allocated
     *
     * @return the absolute rectangle values
     */
    @Implement(Oriented.class)
    public Rectangle switchRef (Rectangle cplt,
                                Rectangle xywh)
    {
        return orientation.switchRef(cplt, xywh);
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

        // Orientation
        if (orientation.isVertical()) {
            sb.append(" VERTICAL");
        } else {
            sb.append(" HORIZONTAL");
        }

        if (this.getClass()
                .getName()
                .equals(Lag.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
    }

    //--------//
    // update //
    //--------//
    /**
     * Call-back triggered when selection of sheet location, section or section
     * id, has been modified.
     * We forward the related run and section informations.
     *
     * @param selection the notified Selection
     * @param hint potential notification hint
     */
    @Implement(SelectionObserver.class)
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        switch (selection.getTag()) {
        // Interest in sheet location
        case SHEET_RECTANGLE :

            // Lookup for Run/Section pointed by this pixel location
            if ((hint == SelectionHint.LOCATION_ADD) ||
                (hint == SelectionHint.LOCATION_INIT)) {
                // Search and forward run & section info
                // Optimization : do the lookup only if observers other
                // than this lag are present
                if (((runSelection != null) &&
                    (runSelection.countObservers() > 0)) ||
                    ((sectionSelection != null) &&
                    (sectionSelection.countObservers() > 1))) { // Lag itself !

                    Run       run = null;
                    S         section = null;
                    Rectangle rect = (Rectangle) selection.getEntity();

                    if ((rect != null) &&
                        (rect.width == 0) &&
                        (rect.height == 0)) {
                        Point pt = rect.getLocation();
                        section = lookupSection(getVertices(), pt);

                        if (section != null) {
                            Point apt = switchRef(pt, null);
                            run = section.getRunAt(apt.y);
                        }
                    }

                    runSelection.setEntity(run, hint);
                    sectionSelection.setEntity(section, hint);
                }
            }

            break;

        // Interest in section
        case SKEW_SECTION :
        case HORIZONTAL_SECTION :
        case VERTICAL_SECTION :

            if (hint == SelectionHint.SECTION_INIT) {
                // Display section contour
                S section = (S) selection.getEntity(); // Compiler warning
                locationSelection.setEntity(
                    (section != null) ? section.getContourBox() : null,
                    hint);
            }

            break;

        // Interest in section ID
        case SKEW_SECTION_ID :
        case HORIZONTAL_SECTION_ID :
        case VERTICAL_SECTION_ID :

            // Lookup a section with proper ID
            if (sectionSelection != null) {
                Integer id = (Integer) selection.getEntity();
                runSelection.setEntity(null, hint);
                sectionSelection.setEntity(getVertexById(id), hint);
            }

            break;

        default :
        }
    }

    //-----------//
    // getPrefix //
    //-----------//
    /**
     * Return a distinctive string, to be used as a prefix in toString() for
     * example.
     *
     * @return the prefix string
     */
    @Override
    protected String getPrefix ()
    {
        return "Lag";
    }

    //---------//
    // setRuns //
    //---------//
    /**
     * Assign the populated runs to the lag. Package private access is provided
     * for SectionsBuilder
     *
     * @param runs the populated runs
     */
    void setRuns (List<List<Run>> runs)
    {
        this.runs = runs;
    }
}
