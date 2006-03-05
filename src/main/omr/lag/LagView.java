//-----------------------------------------------------------------------//
//                                                                       //
//                             L a g V i e w                             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.lag;

import omr.graph.DigraphView;
import omr.ui.RubberZoomedPanel;
import omr.ui.Zoom;
import omr.util.Logger;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Class <code>LagView</code> derives {@link omr.ui.RubberZoomedPanel} to
 * provide an implementation of a comprehensive display for lags, whether
 * they are vertical or horizontal.
 *
 * <p>This view has the ability to handle a collection of "specific"
 * sections, provided in the constructor. These sections are supposed not
 * part (no longer part perhaps) of the lag sections which allows for a
 * special handling: depending on the current value of the boolean
 * <code>showSpecifics</code>, these specific sections are displayed or not
 * (and can be lookedup or not).
 *
 * <p><b>Nota</b>: For the time being, we've chosen to not draw the
 * edges/junctions but just the vertices/sections.
 *
 * <p>A LagView implements the {@link SectionFocus} interface, meaning that
 * it is possible to ask this LagView to focus on a given section
 *
 * <p> Instances of interface {@link SectionObserver} can be connected to
 * this LagView, for example to display detailed info related to a given
 * section
 *
 * @param <L> the type of lag this view displays
 * @param <S> the type of section the related lag handles
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LagView <L extends Lag     <L, S>,
                      S extends Section <L, S>>
    extends RubberZoomedPanel
    implements DigraphView,
               SectionFocus<S>
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(LagView.class);

    // Color used when rendering specific sections
    private static final Color SPECIFIC_COLOR = Color.yellow;

    //~ Instance variables ------------------------------------------------

    /** Related lag model */
    protected final L lag;

    /** Specific sections for display */
    protected final Collection<S> specifics;

    /** Boolean used to trigger display of the collection of specific
        sections */
    protected boolean showSpecifics = false;

    /** Global size of displayed image */
    protected Rectangle rectangle = new Rectangle();

    /** Subject for section/run observers if any */
    protected DefaultSectionSubject sectionSubject
        = new DefaultSectionSubject();

    /** Trick to cache the result of previous lookupSection */
    protected S previouslyFound;

    /** Index of this view in the ordered list of views of the related
        lag */
    protected final transient int viewIndex;

    //~ Constructors ------------------------------------------------------

    //---------//
    // LagView //
    //---------//
    /**
     * Create a view on the provided lag, building the related section
     * view.
     *
     * @param lag       the lag to be displayed
     * @param specifics the collection of 'specific' sections, or null
     */
    public LagView (L             lag,
                    Collection<S> specifics)
    {
        // Self-register this view in the related lag
        this.lag = lag;
        lag.addView(this);

        // Remember specific sections
        if (specifics != null) {
            this.specifics = specifics;
        } else {
            this.specifics = new ArrayList<S>(0);
        }

        // Process vertices
        for (S section : lag.getVertices()) {
            addSectionView(section);
        }

        // Process also all specific sections
        for (S section : this.specifics) {
            addSectionView(section);
        }

        setBackground(Color.white);

        // Colorize all sections of the lag
        viewIndex = lag.getViews().indexOf(this);
        colorize();
    }

    //~ Methods -----------------------------------------------------------

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
        SectionView<L, S> sectionView =
            new SectionView<L, S>(section, zoom);
        section.addView(sectionView);

        // Extend the lag bounding rectangle
        rectangle.add(sectionView.getRectangle());

        return sectionView;
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
        for (S section : this.specifics) {
            view = (SectionView) section.getViews().get(viewIndex);
            view.setColor(SPECIFIC_COLOR);
        }
    }

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
     * Retrieve a section knowing its id. This method is located here,
     * rather than in the Lag class, because of the handling of "specific"
     * sections to hide or to show
     *
     * @param id the key to be used
     * @return the section found, or null otherwise
     */
    public Section getSectionById (int id)
    {
        if (showSpecifics) {
            // Look up in specifics first
            for (S section : specifics) {
                if (section.getId() == id) {
                    return section;
                }
            }
        }

        // Now look into 'normal' sections
        return lag.getVertexById(id);
    }

    //---------------//
    // lookupSection //
    //---------------//
    /**
     * Given an absolute point, retrieve a containing section if any, first
     * looking into the specifics if view is currently displaying them,
     * then looking into the standard sections
     *
     * @param pt coordinates of the given point
     *
     * @return the (first) section found, or null otherwise
     */
    public S lookupSection (Point pt)
    {
        if (showSpecifics) {
            // Look up in specifics first
            S section = lookupSection(specifics, pt);

            if (section != null) {
                return section;
            }
        }

        return lookupSection(lag.getVertices(), pt);
    }

    //---------------//
    // lookupSection //
    //---------------//
    /**
     * Given an absolute rectangle, retrieve the first contained section if
     * any, first looking into the specifics if view is currently
     * displaying them, then looking into the standard sections
     *
     * @param rect the given Rectangle
     *
     * @return the (first) section found, or null otherwise
     */
    public S lookupSection (Rectangle rect)
    {
        if (showSpecifics) {
            // Look up in specifics first
            S section = lookupSection(specifics, rect);

            if (section != null) {
                return section;
            }
        }

        return lookupSection(lag.getVertices(), rect);
    }

    //---------------//
    // setFocusPoint //
    //---------------//
    /**
     * Selection of a Section and a Run by point designation. Registered
     * observers are notified of the Section and Run informations.
     *
     * @param pt the selected point in model pixel coordinates
     */
    @Override
        public void setFocusPoint (Point pt)
    {
        ///logger.info(getClass() + " setFocusPoint " + pt);

        // Update pixel board if any
        super.setFocusPoint(pt);

        if (countObservers() > 0) {

            // Section information?
            Section section = lookupSection(pt);

            // Run information?
            Run run = null;
            if (section != null) {
                // Switch to point oriented according to the lag
                // orientation
                Point apt = getLag().switchRef(pt, null);
                run = section.getRunAt(apt.y);
            }

            notifyObservers(section);
            notifyObservers(run);
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Actually render the whole lag on the provided graphics
     *
     * @param g the graphic context
     */
    @Override
    public void render (Graphics g)
    {
        // Determine my view index in the lag views
        final int viewIndex = lag.getViews().indexOf(this);

        // Render all sections, using the colors they have been assigned
        renderCollection(g, lag.getVertices(), viewIndex);

        // Render also all specific sections ?
        if (showSpecifics) {
            renderCollection(g, specifics, viewIndex);
        }

        // Paint additional items, such as recognized items, etc...
        renderItems(g);
    }

    //-----------------//
    // setFocusSection //
    //-----------------//
    public void setFocusSection (Section section)
    {
        if (section != null) {
            Rectangle rect = section.getContourBox();
            setFocusRectangle(rect);

            // Update section info
            notifyObservers(section);
        } else {
            logger.warning ("setFocusSection. Section is null");
        }
    }

    //-----------------//
    // setFocusSection //
    //-----------------//
    public void setFocusSection (int id)
    {
        Section section = getSectionById(id);
        if (section != null) {
            setFocusSection(section);
        } else {
            logger.warning ("Section " + id + " not found.");
        }
    }

    //-------------//
    // addObserver //
    //-------------//
    /**
     * register an observer on section info
     *
     * @param observer the observing entity
     */
    public void addObserver (SectionObserver observer)
    {
        sectionSubject.addObserver(observer);
    }

    //----------------//
    // removeObserver //
    //----------------//
    /**
     * Remove an observing entity
     *
     * @param observer the entity to unregister
     */
    public void removeObserver (SectionObserver observer)
    {
        sectionSubject.removeObserver(observer);
    }

    //-----------------//
    // notifyObservers //
    //-----------------//
    /**
     * Push the section info to registered observers
     *
     * @param section the new section info
     */
    public void notifyObservers (Section section)
    {
        sectionSubject.notifyObservers(section);
    }

    //-----------------//
    // notifyObservers //
    //-----------------//
    /**
     * Push the run info to registered observers
     *
     * @param run the new run info
     */
    public void notifyObservers (Run run)
    {
        sectionSubject.notifyObservers(run);
    }

    //----------------//
    // countObservers //
    //----------------//
    /**
     * Report the number of registered observers
     *
     * @return the observers number
     */
    public int countObservers()
    {
        return sectionSubject.countObservers();
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
        int viewIndex = lag.getViews().indexOf(this);

        // Update standard vertices as well as specific ones
        setCollectionZoom(lag.getVertices(), viewIndex, zoom);
        setCollectionZoom(specifics, viewIndex, zoom);

        super.setZoom(zoom);
    }

    //---------------//
    // showSpecifics //
    //---------------//
    /**
     * Selector on showSpecifics
     *
     * @return true is specifics are being shown, false otherwise
     */
    public boolean showSpecifics ()
    {
        return showSpecifics;
    }

    //--------//
    // toggle //
    //--------//
    /**
     * This method is used to toggle the boolean <code>showSpecifics</code>
     *
     * @see #showSpecifics
     */
    public void toggle ()
    {
        showSpecifics = !showSpecifics;

        // Since search rules are modified, invalidate cache
        previouslyFound = null;

        repaint();
    }

    //~ Methods Protected -------------------------------------------------

    //--------------//
    // getModelSize //
    //--------------//
    /**
     * Return the overall dimension of the lag graph (zooming put apart).
     *
     * @return the global bounding box of the lag
     */
    @Override
    protected Dimension getModelSize ()
    {
        if (super.getModelSize() != null)
            return super.getModelSize();
        else
            return rectangle.getSize();
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
    }

    //~ Methods Private ---------------------------------------------------

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
        SectionView view = (SectionView) section.getViews().get(viewIndex);
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

    //---------------//
    // lookupSection //
    //---------------//
    /**
     * Given an absolute point, retrieve a containing section if any, using
     * the provided collection of sections
     *
     * @param collection the desired collection of sections
     * @param pt         coordinates of the given point
     *
     * @return the (first) section found, or null otherwise
     */
    private S lookupSection (Collection<S> collection,
                             Point   pt)
    {
        Point target = lag.switchRef(pt, null); // Involutive!

        // Just in case...
        if ((previouslyFound != null)
            && previouslyFound.contains(target.x, target.y)) {
            return previouslyFound;
        }

        // Too bad, let's browse the whole stuff
        for (S section : collection) {
            if (section.contains(target.x, target.y)) {
                previouslyFound = section;

                return section;
            }
        }

        return null;
    }

    //---------------//
    // lookupSection //
    //---------------//
    /**
     * Given an absolute rectangle, retrieve the first contained section if
     * any, using the provided collection of sections
     *
     * @param collection the desired collection of sections
     * @param rect       the given rectangle
     *
     * @return the (first) section found, or null otherwise
     */
    private S lookupSection (Collection<S>  collection,
                             Rectangle      rect)
    {
        Rectangle target = lag.switchRef(rect, null); // Involutive!

        for (S section : collection) {
            if (target.contains(section.getContourBox())) {
                previouslyFound = section;
                return section;
            }
        }

        return null;
    }

    //------------------//
    // renderCollection //
    //------------------//
    private void renderCollection (Graphics g,
                                   Collection<S> collection,
                                   int index)
    {
        for (Section section : collection) {
            SectionView view = (SectionView) section.getViews().get(index);
            view.render(g);
        }
    }

    //-------------------//
    // setCollectionZoom //
    //-------------------//
    private void setCollectionZoom (Collection<S> collection,
                                    int index,
                                    Zoom zoom)
    {
        for (Section section : collection) {
            SectionView view = (SectionView) section.getViews().get(index);
            view.setZoom(zoom);
        }
    }
}
