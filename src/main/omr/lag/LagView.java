//----------------------------------------------------------------------------//
//                                                                            //
//                               L a g V i e w                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.lag;

import omr.constant.Constant;

import omr.graph.DigraphView;

import omr.selection.RunEvent;
import omr.selection.SectionEvent;
import omr.selection.SectionIdEvent;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.ui.view.RubberZoomedPanel;
import omr.ui.view.Zoom;

import omr.util.Logger;

import org.bushe.swing.event.EventService;
import org.bushe.swing.event.EventSubscriber;

import java.awt.*;
import java.util.*;

/**
 * Class <code>LagView</code> derives {@link omr.ui.view.RubberZoomedPanel} to
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
    extends RubberZoomedPanel
    implements DigraphView, EventSubscriber<UserEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LagView.class);

    /** (Lag) events this entity is interested in */
    private static final Collection<Class<?extends UserEvent>> eventClasses = new ArrayList<Class<?extends UserEvent>>();

    static {
        eventClasses.add(SectionIdEvent.class);
    }

    // Color used when rendering specific sections
    private static final Color       SPECIFIC_COLOR = Color.yellow;

    //~ Instance fields --------------------------------------------------------

    /** Specific sections for display & lookup */
    protected final Collection<S> specificSections;

    /** Related lag model */
    protected final L lag;

    /** Global size of displayed image */
    protected Rectangle lagContour = new Rectangle();

    /** Boolean used to trigger handling of specific sections */
    protected final Constant.Boolean showingSpecifics;

    /** Index of this view in the ordered list of views of the related lag */
    protected final int viewIndex;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // LagView //
    //---------//
    /**
     * Create a view on the provided lag, building the related section view.
     *
     * @param lag              the lag to be displayed
     * @param locationService the service which notifies location events
     * @param specificSections the collection of 'specific' sections, or null
     * @param showingSpecifics (ref of) a flag for showing specifics or not
     */
    public LagView (L                lag,
                    EventService     locationService,
                    Collection<S>    specificSections,
                    Constant.Boolean showingSpecifics)
    {
        // Self-register this view in the related lag
        this.lag = lag;
        lag.addView(this);
        this.showingSpecifics = showingSpecifics;

        // Remember specific sections
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

        setBackground(Color.white);

        // Colorize all sections of the lag
        viewIndex = lag.viewIndexOf(this);
        colorize();

        // Subscribe to lag events
        for (Class<?extends UserEvent> eventClass : eventClasses) {
            lag.subscribeStrongly(eventClass, this);
        }

        // Subscribe to location events
        locationService.subscribeStrongly(SheetLocationEvent.class, this);
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

    //--------------//
    // getModelSize //
    //--------------//
    /**
     * Return the overall dimension of the lag graph (zooming put apart).
     *
     * @return the global bounding box of the lag
     */
    @Override
    public Dimension getModelSize ()
    {
        if (super.getModelSize() != null) {
            return super.getModelSize();
        } else {
            return lagContour.getSize();
        }
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
    public Section getSectionById (int id)
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

    //---------//
    // setZoom //
    //---------//
    /**
     * Allows to programmatically modify the display zoom
     *
     * @param zoom the new zoom
     */
    @Override
    public void setZoom (Zoom zoom)
    {
        // What is my view Index in all views on this lag
        int viewIndex = lag.viewIndexOf(this);

        // Update standard vertices as well as specific ones
        setCollectionZoom(lag.getVertices(), viewIndex, zoom);
        setCollectionZoom(specificSections, viewIndex, zoom);

        super.setZoom(zoom);
    }

    //----------------//
    // addSectionView //
    //----------------//
    /**
     * Add a view on a section, using the zoom of this lag view
     *
     * @param section the section to display
     * @return the view on the section
     */
    public SectionView<L, S> addSectionView (S section)
    {
        return addSectionView(section, zoom);
    }

    //----------//
    // colorize //
    //----------//
    /**
     * Colorize the whole lag of sections, by assigning proper color index
     */
    public void colorize ()
    {
        // Colorize (normal) vertices
        for (S section : lag.getVertices()) {
            colorize(section);
        }

        // Colorize specific sections, with a different color
        SectionView view;

        for (S section : specificSections) {
            view = (SectionView) section.getView(viewIndex);
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
     * Notification about selection objects. In addition to normal view
     * behavior(making location visible), use the specific sections if any to
     * retrieve Run and Section, based on location or identity.
     *
     * @param event the notified event
     */
    @Override
    public void onEvent (UserEvent event)
    {
        ///logger.info("LagView. selection=" + selection + " hint=" + hint);

        // Default behavior : making point visible & drawing the markers
        super.onEvent(event);

        // Check for specific section if any
        if ((showingSpecifics != null) && showingSpecifics.getValue()) {
            if (event instanceof SheetLocationEvent) {
                SheetLocationEvent sheetLocation = (SheetLocationEvent) event;

                // Default behavior : making point visible & drawing the markers
                super.onEvent(sheetLocation);

                Rectangle rect = sheetLocation.getData();

                if (rect != null) {
                    Point pt = rect.getLocation();
                    lag.invalidateLookupCache();

                    S section = lookupSpecificSection(pt);

                    if (section != null) {
                        lag.publish(
                            new SectionEvent<S>(
                                this,
                                sheetLocation.hint,
                                section));

                        Point apt = lag.switchRef(pt, null);
                        lag.publish(
                            new RunEvent(this, section.getRunAt(apt.y)));
                    }
                }
            } else if (event instanceof SectionIdEvent) {
                // Lookup a specific section with proper ID
                SectionIdEvent idEvent = (SectionIdEvent) event;
                Integer        id = idEvent.id;

                if ((id != null) & (id != 0)) {
                    for (S section : specificSections) {
                        if (section.getId() == id) {
                            lag.publish(
                                new SectionEvent<S>(
                                    this,
                                    idEvent.hint,
                                    section));
                            lag.publish(new RunEvent(this, null));

                            break;
                        }
                    }
                }
            }
        }
    }

    //
    //    //------------------------//
    //    // notifySpecificsVisible //
    //    //------------------------//
    //    public void notifySpecificsVisible ()
    //    {
    //        // Since search rules are modified, invalidate cache
    //        lag.invalidateLookupCache();
    //
    //        // Force update
    //        SheetLocationEvent locationEvent = (SheetLocationEvent) locationSelection.getEntity();
    //        PixelRectangle     location = (locationEvent != null)
    //                                      ? locationEvent.getData() : null;
    //
    //        if (location != null) {
    //            locationSelection.setEntity(
    //                new SheetLocationEvent(this, null, null, location));
    //        }
    //
    //        repaint();
    //    }

    //--------//
    // render //
    //--------//
    @Override
    public void render (Graphics g)
    {
        // Determine my view index in the lag views
        final int viewIndex = lag.viewIndexOf(this);

        // Render all sections, using the colors they have been assigned
        renderCollection(g, lag.getVertices(), viewIndex);

        // Render also all specific sections ?
        if ((showingSpecifics != null) && showingSpecifics.getValue()) {
            renderCollection(g, specificSections, viewIndex);
        }

        // Paint additional items, such as recognized items, etc...
        renderItems(g);
    }

    //----------------//
    // addSectionView //
    //----------------//
    /**
     * Add a view on a section, with specified zoom
     *
     * @param section the section to display
     * @param zoom the specified zoom
     * @return the view on the section
     */
    protected SectionView<L, S> addSectionView (S    section,
                                                Zoom zoom)
    {
        // Build the related section view
        SectionView<L, S> sectionView = new SectionView<L, S>(section, zoom);
        section.addView(sectionView);

        // Extend the lag bounding rectangle
        lagContour.add(sectionView.getRectangle());

        return sectionView;
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
        // Empty
    }

    //-------------------//
    // setCollectionZoom //
    //-------------------//
    private void setCollectionZoom (Collection<S> collection,
                                    int           index,
                                    Zoom          zoom)
    {
        for (Section section : collection) {
            SectionView view = (SectionView) section.getView(index);
            view.setZoom(zoom);
        }
    }

    //----------//
    // colorize //
    //----------//
    /**
     * Colorize one section, with a color not already used by the adjacent
     * sections
     *
     * @param section the section to colorize
     */
    private void colorize (S section)
    {
        SectionView view = (SectionView) section.getView(viewIndex);

        if (view.getColorIndex() == -1) {
            // Determine suitable color for this section view
            view.determineColorIndex(viewIndex);

            // Recursive processing of Targets
            for (S sct : section.getTargets()) {
                colorize(sct);
            }

            // Recursive processing of Sources
            for (S sct : section.getSources()) {
                colorize(sct);
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
        for (Section section : collection) {
            SectionView view = (SectionView) section.getView(index);
            view.render(g);
        }
    }
}
