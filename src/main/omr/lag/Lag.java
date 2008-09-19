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

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.selection.RunEvent;
import omr.selection.SectionEvent;
import omr.selection.SectionIdEvent;
import omr.selection.SelectionHint;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.util.Implement;
import omr.util.Logger;
import omr.util.Predicate;

import org.bushe.swing.event.EventService;
import org.bushe.swing.event.EventSubscriber;
import org.bushe.swing.event.ThreadSafeEventService;

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
 * @author Herv&eacute; Bitteur
 * @version $Id$
 * @param <L> precise lag (sub)type
 * @param <S> precise section (sub)type
 */
public class Lag<L extends Lag<L, S>, S extends Section>
    extends Digraph<L, S>
    implements Oriented, EventSubscriber<UserEvent>
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

    /** Events related to this lag (Run, Section) */
    protected EventService eventService = new ThreadSafeEventService();

    /** Event service where selected location is to be written */
    protected EventService locationService;

    /** Cache of last section found through a lookup action */
    private S cachedSection;

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
        eventService.setDefaultCacheSizePerClassOrTopic(1);
        selfSubscribe(SectionEvent.class);
        selfSubscribe(SectionIdEvent.class);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getEventService //
    //-----------------//
    public EventService getEventService ()
    {
        return eventService;
    }

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

    //--------------//
    // getLastEvent //
    //--------------//
    /**
     * Report the last event (if any) in the provided class
     * @return the last event in provided class, or null
     */
    public Object getLastEvent (Class<?extends UserEvent> eventClass)
    {
        return eventService.getLastEvent(eventClass);
    }

    //--------------------//
    // setLocationService //
    //--------------------//
    /**
     * Inject the event service where location must be written to, when
     * triggered through the update method.
     *
     * @param locationService the output selection object
     */
    public void setLocationService (EventService locationService)
    {
        this.locationService = locationService;
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

        // Just in case we have not moved a lot since previous lookup ...
        if ((cachedSection != null) &&
            cachedSection.contains(target.x, target.y) &&
            collection.contains(cachedSection)) {
            return cachedSection;
        } else {
            cachedSection = null;
        }

        // Too bad, let's browse the whole stuff
        for (S section : collection) {
            if (section.contains(target.x, target.y)) {
                cachedSection = section;

                break;
            }
        }

        return cachedSection;
    }

    //---------//
    // unEvent //
    //---------//
    /**
     * Call-back triggered when selection of sheet location, section or section
     * id, has been modified.
     * We forward the related run and section informations.
     *
     * @param event the notified Selection
     */
    @Implement(EventSubscriber.class)
    public void onEvent (UserEvent event)
    {
        if (event instanceof SheetLocationEvent) { // Sheet location

            SheetLocationEvent sheetLocation = (SheetLocationEvent) event;

            if ((sheetLocation.hint == SelectionHint.LOCATION_ADD) ||
                (sheetLocation.hint == SelectionHint.LOCATION_INIT)) {
                // Lookup for Run/Section pointed by this pixel location
                // Search and forward run & section info
                // Optimization : do the lookup only if observers other
                // than this lag are present
                if ((subscribersCount(RunEvent.class) > 0) ||
                    (subscribersCount(SectionEvent.class) > 1)) { // Lag itself !

                    Run run = null;
                    S   section = null;

                    if (sheetLocation != null) {
                        Rectangle rect = sheetLocation.rectangle;

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
                    }

                    publish(new RunEvent(this, run));
                    publish(
                        new SectionEvent<S>(this, sheetLocation.hint, section));
                }
            }
        } else if (event instanceof SectionEvent) { // Section

            SectionEvent sectionEvent = (SectionEvent) event;

            if (sectionEvent.hint == SelectionHint.SECTION_INIT) {
                // Display section contour
                Section section = sectionEvent.section;

                if (section != null) {
                    locationService.publish(
                        new SheetLocationEvent(
                            this,
                            sectionEvent.hint,
                            null,
                            section.getContourBox()));
                } else {
                    locationService.publish(
                        new SheetLocationEvent(
                            this,
                            sectionEvent.hint,
                            null,
                            null));
                }
            }
        } else if (event instanceof SectionIdEvent) { // Section ID
            publish(new RunEvent(this, null));

            SectionIdEvent idEvent = (SectionIdEvent) event;
            Integer        id = idEvent.id;

            if (id != null) {
                // Lookup a section with proper ID
                publish(
                    new SectionEvent<S>(this, idEvent.hint, getVertexById(id)));
            }
        }
    }

    //---------//
    // publish //
    //---------//
    /**
     * Publish on lag event service
     * @param event the event to publish
     */
    public void publish (UserEvent event)
    {
        eventService.publish(event);
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

    //-------------------//
    // subscribeStrongly //
    //-------------------//
    /**
     * Subscribe to the lag event service for a specific class
     * @param eventClass the class of published objects to subscriber listen to
     * @param subscriber The subscriber that will accept the events of the event
     * class when published.
     */
    public void subscribeStrongly (Class<?extends UserEvent> eventClass,
                           EventSubscriber           subscriber)
    {
        eventService.subscribeStrongly(eventClass, subscriber);
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

    //---------------//
    // selfSubscribe //
    //---------------//
    /**
     * Convenient method to auto-subscribe the lag instance on its event service
     * for a specific class
     * @param eventClass the specific classe
     */
    protected void selfSubscribe (Class<?extends UserEvent> eventClass)
    {
        eventService.subscribeStrongly(eventClass, this);
    }

    //------------------//
    // subscribersCount //
    //------------------//
    /**
     * Convenient method to retrieve the number of subscribers on the lag event
     * service for a specific class
     * @param classe the specific classe
     */
    protected int subscribersCount (Class<?extends UserEvent> classe)
    {
        return eventService.getSubscribers(classe)
                           .size();
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
