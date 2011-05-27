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
import omr.glyph.facets.Stick;
import omr.glyph.ui.GlyphLagView;
import omr.glyph.ui.GlyphsController;
import omr.glyph.ui.ViewParameters;

import omr.lag.ui.SectionView;

import omr.log.Logger;

import omr.math.NaturalSpline;

import omr.run.Run;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.ui.PagePainter;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.RunEvent;
import omr.selection.SectionEvent;
import omr.selection.SelectionHint;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.sheet.Sheet;

import omr.ui.util.UIUtilities;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;
import omr.sheet.Scale;

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
class GridView
    extends GlyphLagView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GridBuilder.class);

    /** Stroke for drawing filaments curves */
    private static final Stroke splineStroke = new BasicStroke(
        (float) constants.splineThickness.getValue(),
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND);

    //~ Instance fields --------------------------------------------------------

    // Companion for horizontals (staff lines)
    private final LinesRetriever   linesRetriever;

    // Companion for verticals (barlines)
    private final BarsRetriever    barsRetriever;

    // Related sheet
    private final Sheet            sheet;

    // Additional lag (Vertical)
    private final GlyphLag         vLag;

    /** Record precise max section length per lag */
    private Map<GlyphLag, Integer> maxLengths = new LinkedHashMap<GlyphLag, Integer>();

    /** Used to determine section display color */
    private final int minHorizontalSectionLength;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // GridView //
    //----------//
    /**
     * Creates a new GridView object.
     *
     * @param linesRetriever the related lines retriever
     * @param sheet the related sheet
     * @param hLag horizontal lag
     * @param vLag vertical lag
     * @param specifics specific sections if any
     * @param controller glyphs controller
     */
    public GridView (Sheet              sheet,
                       LinesRetriever     linesRetriever,
                       GlyphLag           hLag,
                       BarsRetriever      barsRetriever,
                       GlyphLag           vLag,
                       List<GlyphSection> specifics,
                       GlyphsController   controller)
    {
        super(hLag, specifics, constants.displaySpecifics, controller, null);

        setName("Frames-View");
        this.sheet = sheet;
        this.linesRetriever = linesRetriever;
        this.barsRetriever = barsRetriever;
        this.minHorizontalSectionLength = linesRetriever.getMinSectionLength();

        // Additional stuff for vLag
        this.vLag = vLag;
        vLag.addView(this);

        for (GlyphSection section : vLag.getVertices()) {
            addSectionView(section);
        }

        // Remember max section length, per lag
        maxLengths.put(hLag, maxLengthOf(hLag));
        maxLengths.put(vLag, maxLengthOf(vLag));
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // colorizeAllGlyphs //
    //-------------------//
    @Override
    public void colorizeAllGlyphs ()
    {
        int viewIndex = vLag.viewIndexOf(this);

        // Recognized bar lines
        for (Glyph glyph : vLag.getAllGlyphs()) {
            Stick stick = (Stick) glyph;

            if (barsRetriever.isLongBar(stick)) {
                ///logger.info("long: " + stick);
                stick.colorize(vLag, viewIndex, Color.red);
            } else if (stick.getShape() != null) {
                ///logger.info("shaped: " + stick);
                stick.colorize(vLag, viewIndex, Color.yellow);
            } else {
                ///logger.info("none: " + stick);
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
        GlyphLag theLag = section.getGraph();
        int      maxLength = maxLengths.get(theLag);
        int      length = section.getLength();
        Color    color;

        if (theLag.isVertical()) {
            // vLag
            int level = (int) Math.rint(
                240 * (1 - (length / (double) maxLength)));
            color = new Color(level, level, 255); // Blue gradient
        } else {
            // hLag
            // Flag too thick sections
            if (linesRetriever.isSectionFat(section)) {
                color = Color.GRAY;
            } else {
                if (section.getLength() < minHorizontalSectionLength) {
                    color = Color.LIGHT_GRAY;
                } else {
                    int level = (int) Math.rint(
                        200 * (1 - (length / (double) maxLength)));
                    color = new Color(255, level, level); // Red Gradient
                }
            }
        }

        SectionView view = (SectionView) section.getView(viewIndex);
        view.setColor(color);
    }

    //-------------//
    // renderItems //
    //-------------//
    @Override
    protected void renderItems (Graphics2D g)
    {
        // Draw curve for the remaining filaments
        g.setColor(PagePainter.musicColor);

        Stroke oldStroke = g.getStroke();
        g.setStroke(splineStroke);

        for (Filament filament : linesRetriever.getFilaments()) {
            filament.renderLine(g);
        }

        // Draw tangent at each ending point
        g.setColor(Color.BLACK);

        double dx = sheet.getScale()
                         .toPixels(constants.tangentDx);

        for (Filament filament : linesRetriever.getFilaments()) {
            NaturalSpline curve = filament.getCurve();

            if (curve != null) {
                PixelPoint p = filament.getStartPoint();
                double     der = curve.derivativeAt(p.x);
                g.draw(new Line2D.Double(p.x, p.y, p.x - dx, p.y - (der * dx)));
                p = filament.getStopPoint();
                der = curve.derivativeAt(p.x);
                g.draw(new Line2D.Double(p.x, p.y, p.x + dx, p.y + (der * dx)));
            }
        }

        g.setStroke(oldStroke);

        // Clusters stuff
        if (linesRetriever.getClustersRetriever() != null) {
            linesRetriever.getClustersRetriever()
                          .renderItems(g);
        }
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
        if (ViewParameters.getInstance()
                          .isSectionSelectionEnabled()) {
            return;
        }

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
            Point        pt = rect.getLocation();

            // No specifics, look into lag
            GlyphSection section = vLag.lookupSection(vLag.getVertices(), pt);

            // Publish Run information
            Point orientedPt = vLag.switchRef(pt, null);
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

    //-------------//
    // maxLengthOf //
    //-------------//
    private int maxLengthOf (GlyphLag lag)
    {
        // Retrieve max section length in the lag
        int maxLength = 0;

        for (GlyphSection section : lag.getVertices()) {
            maxLength = Math.max(maxLength, section.getLength());
        }

        maxLengths.put(lag, maxLength);
        logger.info(lag + " maxLength:" + maxLength);

        return maxLength;
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
        Constant.Double  splineThickness = new Constant.Double(
            "thickness",
            0.5,
            "Stroke thickness to draw filaments curves");
        Scale.Fraction   tangentDx = new Scale.Fraction(
            4,
            "Typical length to display tangents at ending points");
    }
}
