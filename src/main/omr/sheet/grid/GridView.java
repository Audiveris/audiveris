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
package omr.sheet.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.GlyphLagView;
import omr.glyph.ui.GlyphsController;
import omr.glyph.ui.ViewParameters;

import omr.lag.ui.SectionView;

import omr.log.Logger;

import omr.run.Run;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.RunEvent;
import omr.selection.SectionEvent;
import omr.selection.SelectionHint;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.ui.util.UIUtilities;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.List;
import java.util.Set;

/**
 * Class {@code GridView} is a special {@link GlyphLagView}, meant as a
 * companion of {@link GridBuilder} with its 2 lags (horizontal & vertical).
 *
 * <p>We paint on the same display the vertical and horizontal sections.
 * The color depends on the section length, darker for the longest and
 * brighter for the shortest.
 *
 * <p>TODO: The handling of two lags is still rudimentary both for display and
 * for boards. To be improved.
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

    /** Level for minor color */
    private static final int minor = 220;

    /** Level for major color */
    private static final int major = 150;

    /** Default color for vertical stuff */
    public static final Color verticalColor = new Color(minor, minor, 255);

    /** Color for barline-shape glyphs */
    public static final Color vertShapeColor = new Color(major, major, 255);

    /** Default color for horizontal stuff */
    private static final Color horizontalColor = new Color(255, minor, minor);

    /** Color for staffline-shape glyphs */
    public static final Color horiShapeColor = new Color(255, major, major);

    //~ Instance fields --------------------------------------------------------

    // Companion for horizontals (staff lines)
    private final LinesRetriever linesRetriever;

    // Companion for verticals (barlines)
    private final BarsRetriever barsRetriever;

    // Additional lag (Vertical)
    private final GlyphLag vLag;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // GridView //
    //----------//
    /**
     * Creates a new GridView object.
     *
     * @param linesRetriever the related lines retriever
     * @param hLag horizontal lag
     * @param vLag vertical lag
     * @param specifics specific sections if any
     * @param controller glyphs controller
     */
    public GridView (LinesRetriever     linesRetriever,
                     GlyphLag           hLag,
                     BarsRetriever      barsRetriever,
                     GlyphLag           vLag,
                     List<GlyphSection> specifics,
                     GlyphsController   controller)
    {
        super(hLag, specifics, constants.displaySpecifics, controller, null);

        setName("Grid-View");
        this.linesRetriever = linesRetriever;
        this.barsRetriever = barsRetriever;

        // Additional stuff for vLag
        this.vLag = vLag;
        vLag.addView(this);

        for (GlyphSection section : vLag.getVertices()) {
            addSectionView(section);
        }
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
//                Color color = (glyph.getShape() == Shape.STAFF_LINE)
//                              ? horiShapeColor : horizontalColor;
                glyph.colorize(viewIndex, horiShapeColor);
            }

            // Glyphs actually parts of true staff lines
            for (Glyph glyph : linesRetriever.getStafflineGlyphs()) {
                glyph.colorize(viewIndex, Color.WHITE); ///Color.RED);
            }
        }

        { // Vertical:

            int viewIndex = vLag.viewIndexOf(this);

            // All bar glyphs candidates
            for (Glyph glyph : vLag.getActiveGlyphs()) {
                Color color = glyph.isBar() ? vertShapeColor : verticalColor;
                glyph.colorize(viewIndex, color);
            }

            // Glyphs actually parts of true bar lines
            for (Glyph glyph : barsRetriever.getBarlineGlyphs()) {
                glyph.colorize(viewIndex, Color.BLUE);
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
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            // Let's work on hLag first
            super.onEvent(event);

            // Then additional stuff for vLag, if any
            if (event instanceof SheetLocationEvent) {
                // Location => ...
                handleEvent((SheetLocationEvent) event);
            }
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

    //-----------------//
    // colorizeSection //
    //-----------------//
    @Override
    protected void colorizeSection (GlyphSection section,
                                    int          viewIndex)
    {
        SectionView view = (SectionView) section.getView(viewIndex);

        // Determine section color
        Color color;

        if (section.getGraph()
                   .isVertical()) {
            color = verticalColor;
        } else {
            if (section.isGlyphMember()) {
                color = Color.WHITE; ///horizontalColor;
            } else {
                color = Color.LIGHT_GRAY;
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
            constants.showPatterns.getValue());
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in sheet location => run, section, glyph
     *
     * This is meant for vLag only, however the various boards (run, section,
     * glyph) are tied to the hLag! This is OK for output, not for user input!
     * @param sheetLocation
     */
    private void handleEvent (SheetLocationEvent sheetLocationEvent)
    {
        SelectionHint  hint = sheetLocationEvent.hint;
        MouseMovement  movement = sheetLocationEvent.movement;
        PixelRectangle rect = sheetLocationEvent.rectangle;

        if ((hint != SelectionHint.LOCATION_ADD) &&
            (hint != SelectionHint.LOCATION_INIT)) {
            return;
        }

        if (rect == null) {
            return;
        }

        // Let's not overwrite hLag stuff if any
        if (getLag()
                .getSelectedSection() != null) {
            return;
        }

        Glyph glyph = null;

        if ((rect.width > 0) || (rect.height > 0)) {
            // This is a non-degenerated rectangle
            // Look for enclosed glyph
            Set<Glyph> glyphsFound = vLag.lookupGlyphs(rect);
            // Publish Glyph (and the related 1-glyph GlyphSet)
            glyph = glyphsFound.isEmpty() ? null : glyphsFound.iterator()
                                                              .next();
            ////////////////////////////////vLag.getSelectionService().
            publish(new GlyphEvent(this, hint, movement, glyph));
        } else {
            // This is just a point, look for section & glyph
            PixelPoint   pt = rect.getLocation();

            // No specifics, look into lag
            GlyphSection section = vLag.lookupSection(vLag.getVertices(), pt);

            // Publish Run information
            Point orientedPt = vLag.oriented(pt);
            Run   run = (section != null) ? section.getRunAt(orientedPt.y) : null;
            ////////////////////////////////vLag.getSelectionService().
            publish(new RunEvent(this, hint, movement, run));

            // Publish Section information
            ////////////////////////////////vLag.getSelectionService().
            publish(
                new SectionEvent<GlyphSection>(this, hint, movement, section));

            if (section != null) {
                // Publish Glyph information
                glyph = section.getGlyph();
                ////////////////////////////////vLag.getSelectionService().
                publish(new GlyphEvent(this, hint, movement, glyph));
            }
        }
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
        Constant.Boolean showPatterns = new Constant.Boolean(
            true,
            "Should we show staff lines patterns?");
    }
}
