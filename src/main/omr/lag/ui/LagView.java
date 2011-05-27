//----------------------------------------------------------------------------//
//                                                                            //
//                               L a g V i e w                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag.ui;

import omr.run.Run;
import omr.constant.Constant;

import omr.glyph.ui.ViewParameters;

import omr.graph.DigraphView;

import omr.lag.*;

import omr.log.Logger;

import omr.selection.MouseMovement;
import omr.selection.RunEvent;
import omr.selection.SectionEvent;
import omr.selection.SectionIdEvent;
import omr.selection.SectionSetEvent;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SelectionService;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.ui.util.UIUtilities;
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
 * @author Hervé Bitteur
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
        eventClasses.add(SectionEvent.class);
        eventClasses.add(SectionIdEvent.class);
        eventClasses.add(SectionSetEvent.class);
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
     * @param locationService which service provides location information
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
        setLocationService(locationService, SheetLocationEvent.class);

        // Remember specific sections
        this.showingSpecifics = showingSpecifics;

        if (specificSections != null) {
            this.specificSections = specificSections;
        } else {
            this.specificSections = new ArrayList<S>(0);
        }

        for (S section : lag.getVertices()) {
            addSectionView(section);
        }

        // Process also all specific sections
        for (S section : this.specificSections) {
            addSectionView(section);
        }

        // Set background color
        setBackground(Color.white);
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
    @SuppressWarnings("unchecked")
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            // Default behavior : making point visible & drawing the markers
            super.onEvent(event);

            if (event instanceof SheetLocationEvent) { // Location => Section(s) & Run
                handleEvent((SheetLocationEvent) event);
            } else if (event instanceof SectionIdEvent) { // Section ID => Section
                handleEvent((SectionIdEvent) event);
            } else if (event instanceof SectionEvent) { // Section => contour & SectionSet update
                handleEvent((SectionEvent) event);
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
    public void render (Graphics2D g)
    {
        // Should we draw the section borders?
        boolean      drawBorders = ViewParameters.getInstance()
                                                 .isSectionSelectionEnabled();

        // Determine my view index in the sequence of lag views
        final int    vIndex = lag.viewIndexOf(this);
        final Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1f);

        // Render all sections, using the colors they have been assigned
        renderCollection(g, lag.getVertices(), vIndex, drawBorders);

        // Render also all specific sections?
        if ((showingSpecifics != null) && showingSpecifics.getValue()) {
            renderCollection(g, specificSections, vIndex, drawBorders);
        }

        // Paint additional items, such as recognized items, etc...
        renderItems(g);

        // Restore stroke
        g.setStroke(oldStroke);
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
     * This default implementation paints the selected section set if any
     *
     * @param g the graphic context
     */
    protected void renderItems (Graphics2D g)
    {
        // Only in Section selection mode
        if (!ViewParameters.getInstance()
                           .isSectionSelectionEnabled()) {
            return;
        }

        // Check we have a non-empty collection of selected sections
        Set<S> sections = getLag()
                              .getSelectedSectionSet();

        if ((sections == null) || sections.isEmpty()) {
            return;
        }

        // Determine my view index in the sequence of lag views
        final int vIndex = lag.viewIndexOf(this);

        for (S section : sections) {
            SectionView view = (SectionView) section.getView(vIndex);
            view.renderSelected(g);
        }
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
     * @return the number of subscribers interested in the specific class
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
    protected void colorizeSection (S   section,
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

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Section => section contour + update SectionSet
     * @param sectionEvent
     */
    private void handleEvent (SectionEvent<S> sectionEvent)
    {
        SelectionHint hint = sectionEvent.hint;
        MouseMovement movement = sectionEvent.movement;
        S             section = sectionEvent.section;

        if (hint == SelectionHint.SECTION_INIT) {
            // Publish section contour
            publish(
                new SheetLocationEvent(
                    this,
                    hint,
                    null,
                    (section != null) ? section.getContourBox() : null));
        }

        // In section-selection mode, update section set
        if (ViewParameters.getInstance()
                          .isSectionSelectionEnabled() &&
            (subscribersCount(SectionSetEvent.class) > 0)) {
            // Update section set
            Set<S> sections = getLag()
                                  .getSelectedSectionSet();

            if (sections == null) {
                sections = new LinkedHashSet<S>();
            }

            if (hint == LOCATION_ADD) {
                if (section != null) {
                    if (movement == MouseMovement.PRESSING) {
                        // Adding to (or Removing from) the set of glyphs
                        if (sections.contains(section)) {
                            sections.remove(section);
                        } else {
                            sections.add(section);
                        }
                    } else if (movement == MouseMovement.DRAGGING) {
                        // Always adding to the set of glyphs
                        sections.add(section);
                    }

                    publish(
                        new SectionSetEvent<S>(this, hint, movement, sections));
                }
            } else {
                // Overwriting the set of sections
                if (section != null) {
                    // Make a one-section set
                    sections.clear();
                    sections.add(section);
                } else if (!sections.isEmpty()) {
                    // Empty the section set
                    sections.clear();
                }

                publish(new SectionSetEvent<S>(this, hint, movement, sections));
            }
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in SectionId => Section
     * @param idEvent
     */
    private void handleEvent (SectionIdEvent idEvent)
    {
        Integer id = idEvent.id;

        if ((id == null) || (id == 0)) {
            return;
        }

        SelectionHint hint = idEvent.hint;
        MouseMovement movement = idEvent.movement;

        // Always publish a null Run
        publish(new RunEvent(this, hint, movement, null));

        // Publish proper Section
        if (!specificSections.isEmpty()) {
            // Lookup a specific section with proper ID
            for (S section : specificSections) {
                if (section.getId() == id) {
                    publish(new SectionEvent<S>(this, hint, movement, section));

                    return;
                }
            }
        }

        // Lookup a lag section with proper ID
        publish(
            new SectionEvent<S>(this, hint, movement, lag.getVertexById(id)));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in SheetLocation => Section(s) & Run
     * @param sheetLocation
     */
    private void handleEvent (SheetLocationEvent sheetLocationEvent)
    {
        if (logger.isFineEnabled()) {
            logger.fine("LagView. sheetLocation:" + sheetLocationEvent);
        }

        // Lookup for Run/Section pointed by this pixel location
        // Search and forward run & section info
        Rectangle rect = sheetLocationEvent.getData();

        if (rect == null) {
            return;
        }

        SelectionHint hint = sheetLocationEvent.hint;
        MouseMovement movement = sheetLocationEvent.movement;

        //        // Optimization : do the lookup only if observers other
        //        // than this controller are present
        //        if ((subscribersCount(RunEvent.class) == 0) &&
        //            (subscribersCount(SectionEvent.class) <= 1) &&
        //            (subscribersCount(GlyphEvent.class) == 0)) {
        //            return;
        //        }
        if ((hint != SelectionHint.LOCATION_ADD) &&
            (hint != SelectionHint.LOCATION_INIT)) {
            return;
        }

        // Section selection mode?
        if (ViewParameters.getInstance()
                          .isSectionSelectionEnabled()) {
            // Non-degenerated rectangle? look for enclosed sections
            if ((rect.width > 0) && (rect.height > 0)) {
                if (subscribersCount(SectionSetEvent.class) > 0) {
                    // Look for enclosed glyphs
                    Set<S> sectionsFound = getLag()
                                               .lookupSections(rect);
                    // Publish no Run information
                    publish(new RunEvent(this, hint, movement, null));

                    // Publish (first) Section found
                    S section = sectionsFound.isEmpty() ? null
                                : sectionsFound.iterator()
                                               .next();
                    publish(new SectionEvent<S>(this, hint, movement, section));

                    // Publish whole SectionSet
                    publish(
                        new SectionSetEvent<S>(
                            this,
                            hint,
                            movement,
                            sectionsFound));
                }
            } else {
                // Just section addition / removal
                Point pt = rect.getLocation();

                // No specifics, look into lag
                S     section = lag.lookupSection(lag.getVertices(), pt);

                // Publish Run information
                Point apt = lag.switchRef(pt, null);
                Run   run = (section != null) ? section.getRunAt(apt.y) : null;
                publish(new RunEvent(this, hint, movement, run));

                // Publish Section information
                publish(new SectionEvent<S>(this, hint, movement, section));
            }
        } else {
            // We are in glyph selection mode
            Point pt = rect.getLocation();
            S     section = null;

            // Should we first look in specific sections?
            if (showingSpecifics()) {
                lag.invalidateLookupCache();
                section = lookupSpecificSection(pt);
            }

            // If not found in specifics, now look into lag
            if (section == null) {
                section = lag.lookupSection(lag.getVertices(), pt);
            }

            // Publish Run information
            Point apt = lag.switchRef(pt, null);
            Run   run = (section != null) ? section.getRunAt(apt.y) : null;
            publish(new RunEvent(this, hint, movement, run));

            // Publish Section information
            publish(new SectionEvent<S>(this, hint, movement, section));

            // Publish no SectionSet
            publish(new SectionSetEvent<S>(this, hint, movement, null));
        }
    }

    //------------------//
    // renderCollection //
    //------------------//
    protected void renderCollection (Graphics2D    g,
                                   Collection<S> collection,
                                   int           index,
                                   boolean       drawBorders)
    {
        for (S section : collection) {
            SectionView view = (SectionView) section.getView(index);
            view.render(g, drawBorders);
        }
    }
}
