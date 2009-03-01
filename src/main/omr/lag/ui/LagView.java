//----------------------------------------------------------------------------//
//                                                                            //
//                               L a g V i e w                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.lag.ui;

import omr.constant.Constant;

import omr.graph.DigraphView;

import omr.lag.*;

import omr.log.Logger;

import omr.selection.MouseMovement;
import omr.selection.RunEvent;
import omr.selection.SectionEvent;
import omr.selection.SectionIdEvent;
import omr.selection.SelectionHint;
import omr.selection.SelectionService;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.ui.view.RubberPanel;

import org.bushe.swing.event.EventSubscriber;

import java.awt.*;
import java.util.*;

/**
 * Class <code>LagView</code> derives {@link omr.ui.view.RubberPanel} to
 * provide an implementation of a comprehensive display for lags, whether they
 * are vertical or horizontal.
 *
 * <p>This view has the ability to handle a collection of "specific" sections,
 * provided in the constructor. These sections are supposed not part (no longer
 * part perhaps) of the lag sections which allows for a special handling:
 * depending on the current value of the boolean <code>showingSpecifics</code>,
 * these specific sections are displayed or not (and can be lookedup or not).
 *
 * <p><b>Nota</b>: For the time being, we've chosen to not draw the
 * edges/junctions but just the vertices/sections.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 *
 * @param <L> the type of lag this view displays
 * @param <S> the type of section the related lag handles
 */
public class LagView<L extends Lag<L, S>, S extends Section<L, S>>
    extends RubberPanel
    implements DigraphView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LagView.class);

    /** Color used when rendering specific sections */
    private static final Color SPECIFIC_COLOR = Color.yellow;

    /** (Lag) events this entity is interested in */
    private static final Collection<Class<?extends UserEvent>> eventClasses;

    static {
        eventClasses = new ArrayList<Class<?extends UserEvent>>();
        eventClasses.add(SectionIdEvent.class);
    }

    //~ Instance fields --------------------------------------------------------

    /** Related lag model */
    protected final L lag;

    /** Lag Event service (Run, Section) */
    protected final SelectionService lagSelectionService;

    /** Boolean used to trigger handling of specific sections */
    protected final Constant.Boolean showingSpecifics;

    /** Specific sections for display & lookup */
    protected final Collection<S> specificSections;

    /** Global size of displayed image */
    protected Rectangle lagContour = new Rectangle();

    //~ Constructors -----------------------------------------------------------

    //---------//
    // LagView //
    //---------//
    /**
     * Create a view on the provided lag, building the related sections views.
     *
     * @param lag              the lag to be displayed
     * @param specificSections the collection of 'specific' sections, or null
     * @param showingSpecifics (ref of) a flag for showing specifics sections
     */
    public LagView (L                lag,
                    Collection<S>    specificSections,
                    Constant.Boolean showingSpecifics,
                    SelectionService locationService)
    {
        // Self-register this view in the related lag
        this.lag = lag;
        lag.addView(this);

        // Location service        
        this.locationService = locationService;
        lagSelectionService = lag.getSelectionService();
        selfSubscribe(SectionEvent.class);
        selfSubscribe(SectionIdEvent.class);
        setLocationService(locationService, SheetLocationEvent.class);

        // Remember specific sections
        this.showingSpecifics = showingSpecifics;

        if (specificSections != null) {
            this.specificSections = specificSections;
        } else {
            this.specificSections = new ArrayList<S>(0);
        }

        // Process vertices
        for (S section : lag.getVertices()) {
            addSectionView(section);
        }

        // Process also all specific sections
        for (S section : this.specificSections) {
            addSectionView(section);
        }

        // Colorize all sections of the lag
        setBackground(Color.white);
        colorizeAllSections();
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // getLag //
    //--------//
    /**
     * A selector to related lag.
     *
     * @return the lag this view describes
     */
    public L getLag ()
    {
        return lag;
    }

    //----------------//
    // getSectionById //
    //----------------//
    /**
     * Retrieve a section knowing its id. This method is located here, rather
     * than in the Lag class, because of the handling of "specific" sections to
     * hide or to show
     *
     * @param id the key to be used
     * @return the section found, or null otherwise
     */
    @SuppressWarnings("unchecked")
    public S getSectionById (int id)
    {
        if ((showingSpecifics != null) && showingSpecifics.getValue()) {
            // Look up in specifics first
            for (S section : specificSections) {
                if (section.getId() == id) {
                    return section;
                }
            }
        }

        // Now look into 'normal' sections
        return lag.getVertexById(id);
    }

    //---------------------//
    // getSpecificSections //
    //---------------------//
    public Collection<S> getSpecificSections ()
    {
        return specificSections;
    }

    //    //--------------//
    //    // getModelSize //
    //    //--------------//
    //    /**
    //     * Return the overall dimension of the lag graph (zooming put apart).
    //     *
    //     * @return the global bounding box of the lag
    //     */
    //    @Override
    //    public Dimension getModelSize ()
    //    {
    //        if (super.getModelSize() != null) {
    //            return super.getModelSize();
    //        } else {
    //            return lagContour.getSize();
    //        }
    //    }

    //----------------//
    // addSectionView //
    //----------------//
    /**
     * Add a view on a section
     *
     * @param section the section to display
     * @return the view on the section
     */
    public SectionView<L, S> addSectionView (S section)
    {
        // Build the related section view
        SectionView<L, S> sectionView = new SectionView<L, S>(section);
        section.addView(sectionView);

        // Extend the lag bounding rectangle
        lagContour.add(sectionView.getRectangle());

        return sectionView;
    }

    //---------------------//
    // colorizeAllSections //
    //---------------------//
    /**
     * Colorize the whole lag of sections, by assigning proper color index
     */
    public void colorizeAllSections ()
    {
        int viewIndex = lag.viewIndexOf(this);

        // Colorize (normal) vertices and transitively all the connected ones
        for (S section : lag.getVertices()) {
            colorizeSection(section, viewIndex);
        }

        // Colorize specific sections, with a different color
        for (S section : specificSections) {
            SectionView view = (SectionView) section.getView(viewIndex);
            view.setColor(SPECIFIC_COLOR);
        }
    }

    //---------------//
    // lookupSection //
    //---------------//
    /**
     * Given an absolute point, retrieve a containing section if any, first
     * looking into the specifics if view is currently displaying them, then
     * looking into the standard sections
     *
     * @param pt coordinates of the given point
     *
     * @return the (first) section found, or null otherwise
     */
    public S lookupSection (Point pt)
    {
        if ((showingSpecifics != null) && showingSpecifics.getValue()) {
            // Look up in specifics first
            S section = lookupSpecificSection(pt);

            if (section != null) {
                return section;
            }
        }

        return lag.lookupSection(lag.getVertices(), pt);
    }

    //-----------------------//
    // lookupSpecificSection //
    //-----------------------//
    /**
     * Lookup for a section, within the collection of specific sections, that
     * contains the given point
     *
     * @param pt the given point
     *
     * @return the containing specific section, or null if not found
     */
    public S lookupSpecificSection (Point pt)
    {
        return lag.lookupSection(specificSections, pt);
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Notification about selection objects. We catch:
     *
     * SheetLocation (-> Run & Section) in specifics and lag
     * SectionId (-> Section) in specifics and lag
     * Section (-> section contour box) whatever the section
     *
     * @param event the notified event
     */
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            ///logger.info("LagView. event=" + event);

            // Default behavior : making point visible & drawing the markers
            super.onEvent(event);

            // Lookup for Run/Section pointed by this pixel location
            // Search and forward run & section info
            // Optimization : do the lookup only if observers other
            // than this controller are present
            if (event instanceof SheetLocationEvent) { // Location

                if ((subscribersCount(RunEvent.class) > 0) ||
                    (subscribersCount(SectionEvent.class) > 1)) { // myself!

                    SheetLocationEvent sheetLocation = (SheetLocationEvent) event;
                    Rectangle          rect = sheetLocation.getData();
                    S                  section = null;

                    if (rect != null) {
                        if ((sheetLocation.hint == SelectionHint.LOCATION_ADD) ||
                            (sheetLocation.hint == SelectionHint.LOCATION_INIT)) {
                            Point pt = rect.getLocation();

                            if (showingSpecifics()) {
                                // First look in specific sections
                                lag.invalidateLookupCache();
                                section = lookupSpecificSection(pt);
                            } else {
                                // Look into lag
                                section = lag.lookupSection(
                                    lag.getVertices(),
                                    pt);
                            }

                            publish(
                                new SectionEvent<S>(
                                    this,
                                    sheetLocation.hint,
                                    section));

                            Point apt = lag.switchRef(pt, null);
                            publish(
                                new RunEvent(
                                    this,
                                    (section != null) ? section.getRunAt(apt.y)
                                                                        : null));
                        }
                    }
                }
            } else if (event instanceof SectionIdEvent) { // Section ID

                SectionIdEvent idEvent = (SectionIdEvent) event;
                Integer        id = idEvent.id;

                if ((id != null) & (id != 0)) {
                    publish(new RunEvent(this, null));

                    if (!specificSections.isEmpty()) {
                        // Lookup a specific section with proper ID
                        for (S section : specificSections) {
                            if (section.getId() == id) {
                                publish(
                                    new SectionEvent<S>(
                                        this,
                                        idEvent.hint,
                                        section));

                                break;
                            }
                        }
                    } else {
                        // Lookup a lag section with proper ID
                        publish(
                            new SectionEvent<S>(
                                this,
                                idEvent.hint,
                                lag.getVertexById(id)));
                    }
                }
            } else if (event instanceof SectionEvent) { // Section

                SectionEvent sectionEvent = (SectionEvent) event;

                if (sectionEvent.hint == SelectionHint.SECTION_INIT) {
                    // Display section contour
                    Section section = sectionEvent.section;
                    locationService.publish(
                        new SheetLocationEvent(
                            this,
                            sectionEvent.hint,
                            null,
                            (section != null) ? section.getContourBox() : null));
                }
            }
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
    }

    //---------//
    // publish //
    //---------//
    /**
     * Publish on LagController event service
     * @param event the event to publish
     */
    public void publish (UserEvent event)
    {
        lagSelectionService.publish(event);
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the  display using the colors of the sections
     */
    public void refresh ()
    {
        colorizeAllSections();
        repaint();
    }

    //--------//
    // render //
    //--------//
    /**
     * Render this lag in the provided Graphics context, which may be already
     * scaled
     * @param g the graphics context
     */
    @Override
    public void render (Graphics g)
    {
        // Determine my view index in the sequence of lag views
        final int vIndex = lag.viewIndexOf(this);

        // Render all sections, using the colors they have been assigned
        renderCollection(g, lag.getVertices(), vIndex);

        // Render also all specific sections ?
        if ((showingSpecifics != null) && showingSpecifics.getValue()) {
            renderCollection(g, specificSections, vIndex);
        }

        // Paint additional items, such as recognized items, etc...
        renderItems(g);
    }

    //-----------//
    // subscribe //
    //-----------//
    /**
     * Subscribe to location and lag events
     */
    @Override
    public void subscribe ()
    {
        // Subscribe to location events
        super.subscribe();

        // Subscribe to lag events
        if (logger.isFineEnabled()) {
            logger.fine(
                getClass().getName() + " LC subscribing to " + eventClasses);
        }

        for (Class<?extends UserEvent> eventClass : eventClasses) {
            subscribe(eventClass, this);
        }
    }

    //-----------//
    // subscribe //
    //-----------//
    /**
     * Subscribe to the LagController event service for a specific class
     * @param eventClass the class of published objects to subscriber listen to
     * @param subscriber The subscriber that will accept the events of the event
     * class when published.
     */
    public void subscribe (Class<?extends UserEvent> eventClass,
                           EventSubscriber           subscriber)
    {
        lagSelectionService.subscribe(eventClass, subscriber);
    }

    //-------------//
    // unsubscribe //
    //-------------//
    /**
     * Unsubscribe from location and lag events
     */
    @Override
    public void unsubscribe ()
    {
        // Unsubscribe to location events
        super.unsubscribe();

        // Unsubscribe to lag events
        if (logger.isFineEnabled()) {
            logger.fine(
                getClass().getName() + " LC unsubscribing from " +
                eventClasses);
        }

        for (Class<?extends UserEvent> eventClass : eventClasses) {
            unsubscribe(eventClass, this);
        }
    }

    //-------------//
    // unsubscribe //
    //-------------//
    /**
     * Unsubscribe to the LagController event service for a specific class
     * @param eventClass the class of interesting events
     * @param subscriber the entity to unsubscribe
     */
    public void unsubscribe (Class<?extends UserEvent> eventClass,
                             EventSubscriber           subscriber)
    {
        lagSelectionService.unsubscribe(eventClass, subscriber);
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Room for rendering additional items, on top of the basic lag itself.
     * Default implementation is void of course.
     *
     * @param g the graphic context
     */
    protected void renderItems (Graphics g)
    {
        // Just a placeholder
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
        lagSelectionService.subscribe(eventClass, this);
    }

    //------------------//
    // showingSpecifics //
    //------------------//
    protected boolean showingSpecifics ()
    {
        return (showingSpecifics != null) && showingSpecifics.getValue();
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
        return lagSelectionService.subscribersCount(classe);
    }

    //-----------------//
    // colorizeSection //
    //-----------------//
    /**
     * Colorize one section, with a color not already used by the adjacent
     * sections
     *
     * @param section the section to colorize
     * @param viewIndex the index of this view in the lag list of views
     */
    private void colorizeSection (S   section,
                                  int viewIndex)
    {
        SectionView view = (SectionView) section.getView(viewIndex);

        // Determine suitable color for this section view
        if (!view.isColorized()) {
            view.determineDefaultColor(viewIndex);

            // Recursive processing of Targets
            for (S sct : section.getTargets()) {
                colorizeSection(sct, viewIndex);
            }

            // Recursive processing of Sources
            for (S sct : section.getSources()) {
                colorizeSection(sct, viewIndex);
            }
        }
    }

    //------------------//
    // renderCollection //
    //------------------//
    private void renderCollection (Graphics      g,
                                   Collection<S> collection,
                                   int           index)
    {
        for (S section : collection) {
            SectionView view = (SectionView) section.getView(index);
            view.render(g);
        }
    }
}
