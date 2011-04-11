//----------------------------------------------------------------------------//
//                                                                            //
//                       M u l t i p l e L a g V i e w                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag.ui;

import omr.glyph.ui.ViewParameters;

import omr.graph.DigraphView;

import omr.lag.*;

import omr.log.Logger;

import omr.ui.util.UIUtilities;
import omr.ui.view.RubberPanel;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>MultipleLagView</code> is a LagView that combines the display
 * of several lags
 *
 * @author Hervé Bitteur
 *
 * @param <L> the type of lag this view displays
 * @param <S> the type of section the related lag handles
 */
public class MultipleLagView<L extends Lag<L, S>, S extends Section<L, S>>
    extends RubberPanel
    implements DigraphView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        MultipleLagView.class);

    //~ Instance fields --------------------------------------------------------

    /** The collection of lags */
    protected final List<L> lags;

    /** Global size of displayed image */
    protected Rectangle lagContour = new Rectangle();

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // MultipleLagView //
    //-----------------//
    /**
     * Create a view on the provided lags, building the related sections views.
     *
     * @param lags              the lags to be displayed
     */
    public MultipleLagView (List<L> lags)
    {
        this.lags = lags;

        // Self-register this view in the related lags
        for (L lag : lags) {
            lag.addView(this);

            for (S section : lag.getVertices()) {
                addSectionView(section);
            }

            // Colorize all sections of the lag
            setBackground(Color.white);
            colorizeAllSections(lag);
        }
    }

    //~ Methods ----------------------------------------------------------------

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
    public void colorizeAllSections (L lag)
    {
        int viewIndex = lag.viewIndexOf(this);

        // Colorize (normal) vertices and transitively all the connected ones
        for (S section : lag.getVertices()) {
            colorizeSection(section, viewIndex);
        }
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
        final Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1f);

        for (L lag : lags) {
            // Determine my view index in the sequence of lag views
            final int vIndex = lag.viewIndexOf(this);

            // Render all sections, using the colors they have been assigned
            renderCollection(g, lag.getVertices(), vIndex, drawBorders);
        }

        // Paint additional items, such as recognized items, etc...
        renderItems(g);

        // Restore stoke
        g.setStroke(oldStroke);
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
        // Void by default
    }

    //------------------//
    // renderCollection //
    //------------------//
    private void renderCollection (Graphics2D    g,
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
