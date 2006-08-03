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
import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.ui.view.RubberZoomedPanel;
import omr.ui.view.Zoom;
import omr.util.Logger;

import java.awt.*;
import java.util.*;

/**
 * Class <code>LagView</code> derives {@link omr.ui.view.RubberZoomedPanel}
 * to provide an implementation of a comprehensive display for lags,
 * whether they are vertical or horizontal.
 * 
 * <p>This view has the ability to handle a collection of "specific"
 * sections, provided in the constructor. These sections are supposed not
 * part (no longer part perhaps) of the lag sections which allows for a
 * special handling: depending on the current value of the boolean
 * <code>showingSpecifics</code>, these specific sections are displayed or not
 * (and can be lookedup or not).
 * 
 * <p><b>Nota</b>: For the time being, we've chosen to not draw the
 * edges/junctions but just the vertices/sections.
 * 
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>PIXEL Location (if LOCATION_INIT & specifics)
 * <li>*_SECTION_ID (if specifics)
 * </ul>
 * 
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>*_RUN
 * <li>*_SECTION
 * </ul>
 * </dl>
 * 
 * 
 * @author Herv&eacute; Bitteur
 * @version $Id$
 * @param <L> the type of lag this view displays
 * @param <S> the type of section the related lag handles
 */
public class LagView <L extends Lag     <L, S>,
                      S extends Section <L, S>>
    extends RubberZoomedPanel
    implements DigraphView
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(LagView.class);

    // Color used when rendering specific sections
    private static final Color SPECIFIC_COLOR = Color.yellow;

    //~ Instance variables ------------------------------------------------

    /** Related lag model */
    protected final L lag;

    /** Specific sections for display & lookup */
    protected final Collection<S> specifics;

    /** Boolean used to trigger handling of specific sections */
    protected boolean showingSpecifics = false;

    /** Global size of displayed image */
    protected Rectangle lagContour = new Rectangle();

    /** Index of this view in the ordered list of views of the related
        lag */
    protected final transient int viewIndex;

    /** Output selection for specific section if any */
    protected Selection sectionSelection;

    /** Output selection for specific run if any */
    protected Selection runSelection;

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

    //-----------------------//
    // setSpecificSelections //
    //-----------------------//
    /**
     * Inject dependency on where this lag view should write Run and
     * Section information regarding specific sections.
     *
     * @param runSelection the Run selection
     * @param sectionSelection the Section selection
     */
    public void setSpecificSelections (Selection runSelection,
                                       Selection sectionSelection)
    {
        this.runSelection     = runSelection;
        this.sectionSelection = sectionSelection;
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
        lagContour.add(sectionView.getRectangle());

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
        if (showingSpecifics) {
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
        if (showingSpecifics) {
            // Look up in specifics first
            S section = lag.lookupSection(specifics, pt);

            if (section != null) {
                return section;
            }
        }

        return lag.lookupSection(lag.getVertices(), pt);
    }

    //-----------------------//
    // lookupSpecificSection //
    //-----------------------//
    public S lookupSpecificSection (Point pt)
    {
        return lag.lookupSection(specifics, pt);
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
        if (showingSpecifics) {
            // Look up in specifics first
            S section = lookupSection(specifics, rect);

            if (section != null) {
                return section;
            }
        }

        return lookupSection(lag.getVertices(), rect);
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
        if (showingSpecifics) {
            renderCollection(g, specifics, viewIndex);
        }

        // Paint additional items, such as recognized items, etc...
        renderItems(g);
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

    //------------------//
    // showingSpecifics //
    //------------------//
    /**
     * Are we showing specifics ?
     *
     * @return true is specifics are being shown, false otherwise
     */
    public boolean showingSpecifics ()
    {
        return showingSpecifics;
    }

    //--------//
    // toggle //
    //--------//
    /**
     * This method is used to toggle the boolean <code>showingSpecifics</code>
     *
     *
     * @see #showingSpecifics
     */
    public void toggle ()
    {
        showingSpecifics = !showingSpecifics;

        // Since search rules are modified, invalidate cache
        lag.invalidateLookupCache();

        // Force update
        locationSelection.reNotifyObservers(null);

        repaint();
    }

    //---------//
    // update  //
    //---------//
    /**
     * Call-back triggered by selection notification. Besides PIXEL
     * location selection, this class reacts to section ids.
     *
     * @param selection the notified selection
     * @param hint potential notification hint
     */
    @Override
        public void update(Selection selection,
                           SelectionHint hint)
    {
        ///logger.info("LagView. selection=" + selection + " hint=" + hint);

        // Default behavior : making point visible & drawing the markers
        super.update(selection, hint);

        // Check for specific section if any
        if (showingSpecifics &&
            sectionSelection != null) {
            switch (selection.getTag()) {
            case PIXEL :
                if (hint == SelectionHint.LOCATION_INIT) {
                    Rectangle rect = (Rectangle) selection.getEntity();
                    if (rect != null) {
                        Point pt = rect.getLocation();
                        lag.invalidateLookupCache();
                        S section = lookupSpecificSection(pt);
                        if (section != null) {
                            sectionSelection.setEntity(section, hint);
                            Point apt = lag.switchRef(pt, null);
                            runSelection.setEntity(section.getRunAt(apt.y),
                                                   hint);
                        }
                    }
                }
                break;

            case SKEW_SECTION_ID :
            case HORIZONTAL_SECTION_ID :
            case VERTICAL_SECTION_ID :
                // Lookup a specific section with proper ID
                int id = (Integer) selection.getEntity();
                for (S section : specifics) {
                    if (section.getId() == id) {
                        sectionSelection.setEntity(section, hint);
                        runSelection.setEntity(null, hint);
                        break;
                    }
                }
                break;

            default :
                ///logger.warning("Unexpected selection event from " + selection);
            }
        }
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
            return lagContour.getSize();
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
