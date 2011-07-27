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

import omr.math.Histogram;
import omr.math.Line;

import omr.run.Orientation;
import omr.run.Oriented;
import omr.run.Run;
import omr.run.RunsTable;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.selection.SectionEvent;
import omr.selection.SectionSetEvent;
import omr.selection.SelectionService;

import omr.util.Implement;
import omr.util.Predicate;

import java.awt.*;
import java.awt.geom.Point2D;
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

    /** Underlying runs table */
    private RunsTable runsTable;

    /** Cache of last section found through a lookup action */
    private S cachedSection;

    /**
     * Hosted event service for UI events related to this lag
     * (Run, Section, SectionSet and more)
     */
    protected final SelectionService lagSelectionService;

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
        lagSelectionService = new SelectionService(name);
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
        Run best = null;

        // Relevant portion of runs
        for (int pos = posMin; pos <= posMax; pos++) {
            List<Run> runList = runsTable.getSequence(pos);

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
    public Orientation getOrientation ()
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
            if (rect.intersects(section.getOrientedBounds())) {
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
     * Report the lag selection service
     * @return the lag selection service
     */
    public SelectionService getSelectionService ()
    {
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

    //-----------//
    // absolute //
    //-----------//
    /**
     * Retrieve absolute coordinates of a rectangle, based on lag orientation
     *
     * @param cplt the rectangle values (coordinate, position, length,
     * thickness) relative to lag orientation
     * @return the absolute rectangle values
     */
    @Implement(Oriented.class)
    public PixelRectangle absolute (Rectangle cplt)
    {
        return orientation.absolute(cplt);
    }

    //----------//
    // absolute //
    //----------//
    /**
     * Retrieve absolute coordinates of a point, based on lag orientation
     *
     * @param cp the coordinate / position values (relative to lag orientation)
     * @return the absolute abscissa and ordinate values
     */
    @Implement(Oriented.class)
    public PixelPoint absolute (Point cp)
    {
        return orientation.absolute(cp);
    }

    //----------//
    // absolute //
    //----------//
    /**
     * Retrieve absolute coordinates of a point, based on lag orientation
     *
     * @param cp the coordinate / position values (relative to lag orientation)
     * @return the absolute abscissa and ordinate values
     */
    @Implement(Oriented.class)
    public Point2D.Double absolute (Point2D cp)
    {
        return orientation.absolute(cp);
    }

    //-------------------//
    // createAbsoluteRoi //
    //-------------------//
    /**
     * Create a ROI from a contour specified as an absolute PixelRectangle
     * @param absoluteContour the absolute contour
     * @return the ROI created
     */
    public Roi createAbsoluteRoi (PixelRectangle absoluteContour)
    {
        return new Roi(orientation.oriented(absoluteContour));
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
                            PixelPoint    pt)
    {
        Point target = oriented(pt);

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
    public Set<S> lookupSections (PixelRectangle rect)
    {
        Rectangle target = oriented(rect);
        Set<S>    found = new LinkedHashSet<S>();

        for (S section : getSections()) {
            if (target.contains(section.getOrientedBounds())) {
                found.add(section);
            }
        }

        return found;
    }

    //------------//
    // oriented //
    //------------//
    /**
     * Retrieve oriented coordinates of a rectangle, based on lag orientation
     *
     * @param xywh the absolute rectangle values (x, y, width, height)
     * @return the oriented rectangle values relative to lag orientation
     */
    @Implement(Oriented.class)
    public Rectangle oriented (PixelRectangle xywh)
    {
        return orientation.oriented(xywh);
    }

    //----------//
    // oriented //
    //----------//
    @Implement(Oriented.class)
    public Point oriented (PixelPoint cp)
    {
        return orientation.oriented(cp);
    }

    //----------//
    // oriented //
    //----------//
    @Implement(Oriented.class)
    public Point2D oriented (Point2D cp)
    {
        return orientation.oriented(cp);
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
     * Given an oriented line, return the corresponding absolute line, or vice
     * versa.
     *
     * @param relLine the oriented line
     *
     * @return the corresponding absolute line.
     */
    @Implement(Oriented.class)
    public Line switchRef (Line relLine)
    {
        return orientation.switchRef(relLine);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

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
     * Assign the populated runs table to the lag. Package private access is
     * provided for SectionsBuilder
     *
     * @param runs the populated runs
     */
    void setRuns (RunsTable runs)
    {
        this.runsTable = runs;
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
         * @param orientedContour the oriented contour of the region of interest,
         * specified in the usual (coord, pos) form.
         *
         * @see Lag#createAbsoluteRoi(omr.score.common.PixelRectangle)
         */
        public Roi (Rectangle orientedContour)
        {
            this.contour = orientedContour;
        }

        /**
         * Define a region of interest within the lag
         * @param absoluteContour the absolute contour of the region of interest,
         * specified in the usual (x, y, width, height) form.
         *
         * @see Lag#createAbsoluteRoi(omr.score.common.PixelRectangle)
         */
        public Roi (PixelRectangle absoluteContour)
        {
            this.contour = orientation.oriented(absoluteContour);
        }

        //~ Methods ------------------------------------------------------------

        public PixelRectangle getAbsoluteContour ()
        {
            return orientation.absolute(contour);
        }

        /**
         * Report the histogram obtained in the provided projection orientation
         * @param projection the orientation of the projection
         * @return the computed histogram
         */
        public Histogram<Integer> getHistogram (Oriented projection)
        {
            // Build the sequences of runs & positions
            final List<Run>     runList = new ArrayList<Run>();
            final List<Integer> posList = new ArrayList<Integer>();

            for (int pos = contour.y; pos < (contour.y + contour.height);
                 pos++) {
                List<Run> alignedRuns = runsTable.getSequence(pos);

                for (Run run : alignedRuns) {
                    runList.add(run);
                    posList.add(pos);
                }
            }

            return getHistogram(
                orientation.isVertical() == projection.isVertical(),
                runList,
                posList);
        }

        /**
         * Report the histogram obtained in the provided projection orientation
         * of the runs contained in the provided sections
         * @param projection the orientation of the projection
         * @param sections the provided sections
         * @return the computed histogram
         */
        public Histogram<Integer> getHistogram (Oriented      projection,
                                                Collection<S> sections)
        {
            // Build the sequences of runs & positions
            final List<Run>     runList = new ArrayList<Run>();
            final List<Integer> posList = new ArrayList<Integer>();

            for (S section : sections) {
                final List<Run> sectionRuns = section.getRuns();
                int             pos = section.getFirstPos();

                for (Run run : sectionRuns) {
                    runList.add(run);
                    posList.add(pos++);
                }
            }

            return getHistogram(
                orientation.isVertical() == projection.isVertical(),
                runList,
                posList);
        }

        /**
         * Report the containing lag
         * @return the containing lag
         */
        public Lag getLag ()
        {
            return Lag.this;
        }

        public Rectangle getOrientedContour ()
        {
            return contour;
        }

        @Override
        public String toString ()
        {
            return "Roi absContour:" + getAbsoluteContour();
        }

        /**
         * Report the histogram obtained in the specified projection orientation
         * of the intersection between the provided runs and the roi
         * @param alongTheRuns true for a projection along the runs, false for
         * a projection across the runs
         * @param runList the provided sequence of runs
         * @param posList the provided sequence of runs positions
         * @return the computed histogram
         */
        private Histogram<Integer> getHistogram (boolean       alongTheRuns,
                                                 List<Run>     runList,
                                                 List<Integer> posList)
        {
            // Check parameters
            if ((posList == null) || (runList == null)) {
                throw new IllegalArgumentException(
                    "Null sequence of runs or positions");
            }

            // Check consistency
            if (posList.size() != runList.size()) {
                throw new IllegalArgumentException(
                    "Inconsistent sequences of runs and positions");
            }

            final int                minPos = contour.y;
            final int                maxPos = (contour.y + contour.height) - 1;
            final int                minCoord = contour.x;
            final int                maxCoord = (contour.x + contour.width) -
                                                1;
            final Histogram<Integer> histo = new Histogram<Integer>();
            int                      index = 0;

            for (Run run : runList) {
                int pos = posList.get(index++);

                // Clipping on y
                if ((pos < minPos) || (pos > maxPos)) {
                    continue;
                }

                final int iMin = Math.max(minCoord, run.getStart());
                final int iMax = Math.min(maxCoord, run.getStop());

                // Clipping on x
                if (iMin <= iMax) {
                    if (alongTheRuns) {
                        // Along the runs
                        histo.increaseCount(pos, iMax - iMin + 1);
                    } else {
                        // Across the runs
                        for (int i = iMin; i <= iMax; i++) {
                            histo.increaseCount(i, 1);
                        }
                    }
                }
            }

            return histo;
        }
    }
}
