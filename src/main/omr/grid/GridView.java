//----------------------------------------------------------------------------//
//                                                                            //
//                              G r i d V i e w                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.GlyphLagView;
import omr.glyph.ui.GlyphsController;
import omr.glyph.ui.ViewParameters;

import omr.lag.ui.SectionView;

import omr.log.Logger;

import omr.selection.GlyphEvent;
import omr.selection.GlyphIdEvent;
import omr.selection.MouseMovement;
import omr.selection.RunEvent;
import omr.selection.SectionEvent;
import omr.selection.SectionIdEvent;
import omr.selection.SelectionHint;
import omr.selection.UserEvent;

import omr.ui.Colors;
import omr.ui.util.UIUtilities;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

/**
 * Class {@code GridView} is a special {@link GlyphLagView}, meant as a
 * companion of {@link GridBuilder} with its 2 lags (horizontal & vertical).
 * <p>We paint on the same display the vertical and horizontal sections.
 *
 * @author Hervé Bitteur
 */
public class GridView
    extends GlyphLagView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GridBuilder.class);

    //~ Instance fields --------------------------------------------------------

    // Companion for horizontals (staff lines)
    private final LinesRetriever     linesRetriever;

    // Companion for verticals (barlines)
    private final BarsRetriever      barsRetriever;

    // Main lag (Horizontal)
    private final GlyphLag           hLag;

    // Additional lag (Vertical)
    private final GlyphLag           vLag;

    // Separate end-point for vLag events
    private final VerticalSubscriber verticalSubscriber;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // GridView //
    //----------//
    /**
     * Creates a new GridView object.
     *
     * @param linesRetriever the related lines retriever
     * @param hLag horizontal lag
     * @param barsRetriever the related bars retriever
     * @param vLag vertical lag
     * @param hController horizontal glyphs controller
     * @param vController vertical glyphs controller
     */
    public GridView (LinesRetriever   linesRetriever,
                     GlyphLag         hLag,
                     BarsRetriever    barsRetriever,
                     GlyphLag         vLag,
                     GlyphsController hController,
                     GlyphsController vController)
    {
        super(hLag, null, constants.displaySpecifics, hController, null);

        setName("Grid-View");
        this.linesRetriever = linesRetriever;
        this.barsRetriever = barsRetriever;
        this.hLag = hLag;

        // Additional stuff for vLag
        this.vLag = vLag;
        vLag.addView(this);

        for (GlyphSection section : vLag.getVertices()) {
            addSectionView(section);
        }

        // Companion listening to vLag events
        verticalSubscriber = new VerticalSubscriber(vController);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // colorizeAllGlyphs //
    //-------------------//
    @Override
    public void colorizeAllGlyphs ()
    {
        { // Horizontal:

            int viewIndex = lag.viewIndexOf(this);

            // All staff glyphs candidates
            for (Glyph glyph : lag.getActiveGlyphs()) {
                glyph.colorize(viewIndex, Colors.GRID_HORIZONTAL_ACTIVE);
            }

            // Glyphs actually parts of true staff lines
            for (Glyph glyph : linesRetriever.getStafflineGlyphs()) {
                glyph.colorize(viewIndex, Colors.HIDDEN);
            }
        }

        { // Vertical:

            int viewIndex = vLag.viewIndexOf(this);

            // All bar glyphs candidates
            for (Glyph glyph : vLag.getActiveGlyphs()) {
                Color color = glyph.isBar() ? Colors.GRID_VERTICAL_SHAPED
                              : Colors.GRID_VERTICAL;
                glyph.colorize(viewIndex, color);
            }

            // Glyphs actually parts of true bar lines
            for (Glyph glyph : barsRetriever.getBarlineGlyphs()) {
                glyph.colorize(viewIndex, Colors.GRID_BARLINE);
            }
        }
    }

    //---------------------//
    // colorizeAllSections //
    //---------------------//
    @Override
    public void colorizeAllSections ()
    {
        // For hLag
        super.colorizeAllSections();

        // For vLag
        int viewIndex = vLag.viewIndexOf(this);

        for (GlyphSection section : vLag.getVertices()) {
            colorizeSection(section, viewIndex);
        }
    }

    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            if (event.movement == MouseMovement.RELEASING) {
                return; // Ignore RELEASING
            }

            // Nullify the other side (if entering ids on this side)
            nullifyData(event, otherLag(getLag()));

            super.onEvent(event);
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
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
        final Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1.0F);

        // First render the vertical lag
        final int vIndex = vLag.viewIndexOf(this);
        renderCollection(g, vLag.getVertices(), vIndex, drawBorders);

        // Then standard rendering for hLag (on top of vLag)
        super.render(g);

        g.setStroke(oldStroke);
    }

    //-----------//
    // subscribe //
    //-----------//
    /**
     * Overidden to trigger the subscription of vertical companion
     */
    @Override
    public void subscribe ()
    {
        super.subscribe(); // GridView (hLag + location)
        verticalSubscriber.subscribe(); // Vertical (vLag + location)
    }

    //-------------//
    // unsubscribe //
    //-------------//
    /**
     * Overidden to trigger the unsubscription of vertical companion
     */
    @Override
    public void unsubscribe ()
    {
        super.unsubscribe(); // GridView (hLag + location)
        verticalSubscriber.unsubscribe(); // Vertical (vLag + location)
    }

    //-----------------//
    // colorizeSection //
    //-----------------//
    @Override
    protected void colorizeSection (GlyphSection section,
                                    int          viewIndex)
    {
        SectionView view = (SectionView) section.getView(viewIndex);
        Glyph       glyph = section.getGlyph();

        // Determine section color
        Color color;

        if (section.getGraph()
                   .isVertical()) {
            color = Colors.GRID_VERTICAL;

            if (glyph != null) {
                Shape shape = glyph.getShape();

                if ((shape == Shape.THICK_BARLINE) ||
                    (shape == Shape.THIN_BARLINE)) {
                    color = Colors.GRID_VERTICAL_SHAPED;
                }
            }
        } else {
            if (section.isGlyphMember()) {
                color = Colors.HIDDEN; ///horizontalColor;
            } else {
                color = Colors.ENTITY_MINOR;
            }
        }

        view.setColor(color);
    }

    //-------------//
    // renderItems //
    //-------------//
    @Override
    protected void renderItems (Graphics2D g)
    {
        linesRetriever.renderItems(
            g,
            constants.showTangents.getValue(),
            constants.showCombs.getValue());
        barsRetriever.renderItems(g, constants.showTangents.getValue());
    }

    //-------------//
    // nullifyData //
    //-------------//
    /**
     * Publish null values for Run, Section and Glyph events on the provided lag
     * @param lag the lag to nullify
     */
    private void nullifyData (UserEvent event,
                              GlyphLag  lag)
    {
        if (event instanceof SectionIdEvent || event instanceof GlyphIdEvent) {
            SelectionHint hint = event.hint;
            MouseMovement movement = event.movement;

            // Nullify Run  entity
            lag.getRunSelectionService()
               .publish(new RunEvent(this, hint, movement, null));

            // Nullify Section entity
            lag.getSelectionService()
               .publish(
                new SectionEvent<GlyphSection>(this, hint, movement, null));

            // Nullify Glyph entity
            lag.getSelectionService()
               .publish(new GlyphEvent(this, hint, movement, null));
        }
    }

    //----------//
    // otherLag //
    //----------//
    /**
     * Report the lag for the other orientation
     * @param lag the current lag
     * @return the other lag
     */
    private GlyphLag otherLag (GlyphLag lag)
    {
        if (lag == vLag) {
            return hLag;
        }

        if (lag == hLag) {
            return vLag;
        }

        throw new IllegalArgumentException("Invalid lag: " + lag);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean displaySpecifics = new Constant.Boolean(
            false,
            "Dummy stuff");
        Constant.Boolean showTangents = new Constant.Boolean(
            true,
            "Should we show filament ending tangents?");
        Constant.Boolean showCombs = new Constant.Boolean(
            true,
            "Should we show staff lines combs?");
    }

    //--------------------//
    // VerticalSubscriber //
    //--------------------//
    /**
     * A subscriber dedicated to vLag related events, since we need an endpoint
     * kept separate from standard GlyphLagView used for hLag.
     */
    private class VerticalSubscriber
        extends GlyphLagView
    {
        //~ Constructors -------------------------------------------------------

        public VerticalSubscriber (GlyphsController vController)
        {
            super(vLag, null, constants.displaySpecifics, vController, null);
            setName("Vertical");
        }

        //~ Methods ------------------------------------------------------------

        //---------//
        // onEvent //
        //---------//
        @Override
        public void onEvent (UserEvent event)
        {
            try {
                if (event.movement == MouseMovement.RELEASING) {
                    return; // Ignore RELEASING
                }

                // Nullify the other side (if entering ids on this side)
                nullifyData(event, otherLag(getLag()));

                super.onEvent(event);
            } catch (Exception ex) {
                logger.warning(getClass().getName() + " onEvent error", ex);
            }
        }
    }
}
