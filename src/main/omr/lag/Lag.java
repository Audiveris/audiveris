//----------------------------------------------------------------------------//
//                                                                            //
//                                   L a g                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.graph.Digraph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.selection.SectionEvent;
import omr.selection.SectionSetEvent;
import omr.selection.SelectionService;

import omr.util.Implement;
import omr.util.Predicate;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>Lag</code> handles a graph of {@link Section} instances (sets of
 * contiguous runs with compatible lengths), linked by Junctions when there is
 * no more contiguous run or when the compatibility is no longer met.  Sections
 * are thus vertices of the graph, while junctions are directed edges between
 * sections.
 *
 * <p>A lag may have a related UI selection service accessible through {@link
 * #getSelectionService}. This selection service handles Run, Section and
 * SectionSet events, a derived class such as GlyphLag being able to add other
 * events. The {@link #getSelectedSection} and {@link #getSelectedSectionSet}
 * methods are just convenient ways to retrieve the last selected section or
 * sectionSet from the lag selection service.</p>
 *
 * @author Hervé Bitteur
 *
 * @param <L> precise lag (sub)type
 * @param <S> precise section (sub)type
 */
public class Lag<L extends Lag<L, S>, S extends Section>
    extends Digraph<L, S, SectionSignature>
    implements Oriented
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

    /** Cache of last section found through a lookup action */
    private S cachedSection;

    /**
     * Hosted event service for UI events related to this lag
     * (Run, Section, SectionSet and more)
     */
    protected SelectionService lagSelectionService;

    //~ Constructors -----------------------------------------------------------

    //-----//
    // Lag //
    //-----//
    /**
     * Constructor with specified orientation
     * @param name the distinguished name for this instance
     * @param sectionClass the class to be used when instantiating sections
     * @param orientation the desired orientation of the lag
     */
    protected Lag (String            name,
                   Class<?extends S> sectionClass,
                   Oriented          orientation)
    {
        super(name, sectionClass);
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

    //----------------//
    // getOrientation //
    //----------------//
    @Implement(Oriented.class)
    public LagOrientation getOrientation ()
    {
        return orientation.getOrientation();
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
            if (rect.intersects(section.getBounds())) {
                found.add(section);
            }
        }

        return found;
    }

    //--------------------//
    // getSelectedSection //
    //--------------------//
    /**
     * Convenient method to report the UI currently selected Section, if any, in
     * this lag
     * @return the UI selected section, or null if none
     */
    @SuppressWarnings("unchecked")
    public S getSelectedSection ()
    {
        return (S) getSelectionService()
                       .getSelection(SectionEvent.class); // Unchecked
    }

    //-----------------------//
    // getSelectedSectionSet //
    //-----------------------//
    /**
     * Convenient method to report the UI currently selected set of Sections,
     * if any, in this lag
     * @return the UI selected section set, or null if none
     */
    @SuppressWarnings("unchecked")
    public Set<S> getSelectedSectionSet ()
    {
        return (Set<S>) getSelectionService()
                            .getSelection(SectionSetEvent.class); // Unchecked
    }

    //---------------------//
    // getSelectionService //
    //---------------------//
    /**
     * Report the lag selection service, lazily created
     * @return the lag selection service
     */
    public SelectionService getSelectionService ()
    {
        if (lagSelectionService == null) {
            lagSelectionService = new SelectionService();
        }

        return lagSelectionService;
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

    //-------------------//
    // createAbsoluteRoi //
    //-------------------//
    public Roi createAbsoluteRoi (Rectangle absoluteContour)
    {
        return new Roi(orientation.switchRef(absoluteContour, null));
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
        cachedSection = null;
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

        // Local copy (in case of concurrent accesses)
        S cached = cachedSection;

        // Just in case we have not moved a lot since previous lookup ...
        if ((cached != null) &&
            cached.contains(target.x, target.y) &&
            collection.contains(cached)) {
            return cached;
        } else {
            cached = null;
        }

        // Too bad, let's browse the whole stuff
        for (S section : collection) {
            if (section.contains(target.x, target.y)) {
                cached = section;

                break;
            }
        }

        cachedSection = cached;

        return cached;
    }

    //----------------//
    // lookupSections //
    //----------------//
    /**
     * Lookup for sections that are contained in the provided
     * rectangle. Specific sections are not considered.
     *
     * @param rect the given rectangle
     *
     * @return the set of sections found, which may be empty
     */
    public Set<S> lookupSections (Rectangle rect)
    {
        Rectangle target = switchRef(rect, null);
        Set<S>    found = new LinkedHashSet<S>();

        for (S section : getSections()) {
            if (target.contains(section.getBounds())) {
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
     * @param predicate means to specify whether a section applies for purge
     *
     * @return the list of sections purged in this call
     */
    public List<S> purgeSections (Predicate<S> predicate)
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
     * @param minForeWeight
     * @return the purged sections
     */
    public List<S> purgeTinySections (final int minForeWeight)
    {
        return purgeSections(
            new Predicate<S>() {
                    public boolean check (S section)
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
    public PixelPoint switchRef (Point      cp,
                                 PixelPoint xy)
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
    public PixelRectangle switchRef (Rectangle      cplt,
                                     PixelRectangle xywh)
    {
        return orientation.switchRef(cplt, xywh);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuffer sb = new StringBuffer(super.internalsString());

        // Orientation
        if (orientation.isVertical()) {
            sb.append(" VERTICAL");
        } else {
            sb.append(" HORIZONTAL");
        }

        return sb.toString();
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

    //~ Inner Classes ----------------------------------------------------------

    //-----//
    // Roi //
    //-----//
    /**
     * Region of Interest
     */
    public class Roi
    {
        //~ Instance fields ----------------------------------------------------

        /** Region of interest within the containing lag */
        private final Rectangle contour;

        //~ Constructors -------------------------------------------------------

        /**
         * Define a region of interest within the lag
         * @param contour the contour of the region of interest, specified in
         * the usual (coord, pos) form
         */
        public Roi (Rectangle contour)
        {
            this.contour = contour;
        }

        //~ Methods ------------------------------------------------------------

        public Rectangle getAbsoluteContour ()
        {
            return orientation.switchRef(contour, null);
        }

        public Rectangle getContour ()
        {
            return contour;
        }

        /**
         * Report the histogram obtained in the provided projection orientation
         * @param projection the orientation of the projection
         * @return the computed histogram
         */
        public int[] getHistogram (Oriented projection)
        {
            int[] histo;

            if ((orientation.isVertical() && projection.isVertical()) ||
                (!orientation.isVertical() && !projection.isVertical())) {
                // Same orientations : we project along the runs
                histo = new int[contour.height];
                Arrays.fill(histo, 0);

                int bucket = 0;

                for (List<Run> alignedRuns : runs.subList(
                    contour.y,
                    contour.y + contour.height)) {
                    for (Run run : alignedRuns) {
                        final int iMin = Math.max(contour.x, run.getStart());
                        final int iMax = Math.min(
                            (contour.x + contour.width) - 1,
                            run.getStop());

                        if (iMin <= iMax) {
                            histo[bucket] += (iMax - iMin + 1);
                        }
                    }

                    bucket++;
                }
            } else {
                // Different orientations : we project across the runs
                histo = new int[contour.width];
                Arrays.fill(histo, 0);

                for (List<Run> alignedRuns : runs.subList(
                    contour.y,
                    contour.y + contour.height)) {
                    for (Run run : alignedRuns) {
                        final int iMin = Math.max(contour.x, run.getStart());
                        final int iMax = Math.min(
                            (contour.x + contour.width) - 1,
                            run.getStop());

                        for (int i = iMin; i <= iMax; i++) {
                            histo[i - contour.x]++;
                        }
                    }
                }
            }

            return histo;
        }

        /**
         * Report the containing lag
         * @return the containing lag
         */
        public Lag getLag ()
        {
            return Lag.this;
        }

        @Override
        public String toString ()
        {
            return "Roi absContour:" + getAbsoluteContour();
        }
    }
}
