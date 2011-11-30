//----------------------------------------------------------------------------//
//                                                                            //
//                              N e s t V i e w                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Nest;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.pattern.SlurInspector;
import omr.glyph.text.OcrChar;
import omr.glyph.text.OcrLine;
import omr.glyph.text.TextInfo;

import omr.graph.DigraphView;

import omr.lag.Lag;
import omr.lag.Section;

import omr.log.Logger;

import omr.math.Circle;

import omr.score.ui.PaintingParameters;

import omr.ui.util.UIUtilities;
import omr.ui.view.RubberPanel;

import omr.util.Implement;
import omr.util.WeakPropertyChangeListener;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.CubicCurve2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code NestView} is a  view that combines the display of several lags
 * to represent a nest of glyphs
 *
 * @author Hervé Bitteur
 */
public class NestView
    extends RubberPanel
    implements DigraphView, PropertyChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(NestView.class);

    //~ Instance fields --------------------------------------------------------

    /** The underlying nest */
    protected final Nest nest;

    /** Related glyphs controller */
    protected final GlyphsController controller;

    /** The sequence of lags */
    protected final List<Lag> lags;

    /** Additional items rendering */
    protected final List<ItemRenderer> itemRenderers = new ArrayList<ItemRenderer>();

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // NestView //
    //-----------//
    /**
     * Create a nest view.
     * @param nest the underlying nest of glyphs
     * @param controller the related glyphs controller
     * @param lags the various lags to be displayed
     */
    public NestView (Nest             nest,
                     GlyphsController controller,
                     List<Lag>        lags)
    {
        this.nest = nest;
        this.controller = controller;
        this.lags = lags;

        setName(nest.getName() + "-View");

        setBackground(Color.white);

        // (Weakly) listening on ViewParameters and PaintingParameters
        PropertyChangeListener listener = new WeakPropertyChangeListener(this);
        ViewParameters.getInstance()
                      .addPropertyChangeListener(listener);
        PaintingParameters.getInstance()
                          .addPropertyChangeListener(listener);
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getController //
    //---------------//
    public GlyphsController getController ()
    {
        return controller;
    }

    //-----------------//
    // addItemRenderer //
    //-----------------//
    /**
     * Register an items renderer to render items.
     * @param renderer the additional renderer
     */
    public void addItemRenderer (ItemRenderer renderer)
    {
        itemRenderers.add(renderer);
    }

    //----------------//
    // propertyChange //
    //----------------//
    @Implement(PropertyChangeListener.class)
    public void propertyChange (PropertyChangeEvent evt)
    {
        // Whatever the property change, we simply repaint the view
        repaint();
    }

    //---------//
    // refresh //
    //---------//
    public void refresh ()
    {
        repaint();
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the nest in the provided Graphics context, which may be already
     * scaled.
     * @param g the graphics context
     */
    @Override
    public void render (Graphics2D g)
    {
        // Should we draw the section borders?
        final boolean drawBorders = ViewParameters.getInstance()
                                                  .isSectionSelectionEnabled();

        // Stroke for borders
        final Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1f);

        if (lags != null) {
            for (Lag lag : lags) {
                // Render all sections, using the colors they have been assigned
                for (Section section : lag.getVertices()) {
                    section.render(g, drawBorders);
                }
            }
        }

        // Paint additional items, such as recognized items, etc...
        renderItems(g);

        // Restore stroke
        g.setStroke(oldStroke);
    }

    //-----------------//
    // renderGlyphArea //
    //-----------------//
    /**
     * Render the box area of a glyph, using inverted color.
     * @param glyph the glyph whose area is to be rendered
     * @param g the graphic context
     */
    protected void renderGlyphArea (Glyph      glyph,
                                    Graphics2D g)
    {
        // Check the clipping
        Rectangle box = glyph.getContourBox();

        if (box.intersects(g.getClipBounds())) {
            g.fillRect(box.x, box.y, box.width, box.height);
        }
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Room for rendering additional items, on top of the basic nest itself.
     * This default implementation paints the selected section set if any
     * @param g the graphic context
     */
    protected void renderItems (Graphics2D g)
    {
        // Additional renderers if any
        for (ItemRenderer renderer : itemRenderers) {
            renderer.renderItems(g);
        }

        // Render the selected glyph(s) if any
        Set<Glyph> glyphs = nest.getSelectedGlyphSet();

        if ((glyphs == null) || glyphs.isEmpty()) {
            return;
        }

        // Decorations first
        Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1f);
        g.setColor(Color.blue);

        for (Glyph glyph : glyphs) {
            // Draw circle arc or stick average line
            //            if (glyph.getShape() == Shape.SLUR) {
            //                if (ViewParameters.getInstance()
            //                                  .isCirclePainting()) {
            //                    Circle circle = SlurInspector.computeCircle(glyph);
            //
            //                    if (logger.isFineEnabled()) {
            //                        logger.fine(
            //                            String.format(
            //                                "dist=%g " + circle.toString(),
            //                                circle.getDistance()));
            //                    }
            //
            //                    drawCircle(circle, g);
            //                }
            //
            //                //            } else if (ViewParameters.getInstance()
            //                //                                     .isLinePainting()) {
            //                //                if (glyph instanceof Stick) {
            //                //                    drawStickLine((Stick) glyph, g);
            //                //                }
            //            }

            // Draw character boxes for textual glyphs?
            if (glyph.isText()) {
                if (ViewParameters.getInstance()
                                  .isLetterBoxPainting()) {
                    TextInfo info = glyph.getTextInfo();
                    OcrLine  ocrLine = info.getOcrLine();

                    if (ocrLine != null) {
                        for (OcrChar ch : ocrLine.getChars()) {
                            Rectangle b = ch.getBox();
                            g.drawRect(b.x, b.y, b.width, b.height);
                        }
                    }
                }
            }

            // Draw attachments, if any, with their key name
            if (ViewParameters.getInstance()
                              .isAttachmentPainting() &&
                !glyph.getAttachments()
                      .isEmpty()) {
                Font oldFont = g.getFont();
                g.setFont(oldFont.deriveFont(5f));

                for (Map.Entry<String, java.awt.Shape> entry : glyph.getAttachments()
                                                                    .entrySet()) {
                    java.awt.Shape shape = entry.getValue();
                    g.draw(shape);

                    String    key = entry.getKey();
                    Rectangle rect = shape.getBounds();
                    g.drawString(
                        key,
                        rect.x + (rect.width / 2),
                        rect.y + (rect.height / 2));
                }

                g.setFont(oldFont);
            }
        }

        g.setStroke(oldStroke);

        // Glyph areas second, using XOR mode for the area
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Color.black);
        g2.setXORMode(Color.darkGray);

        for (Glyph glyph : glyphs) {
            renderGlyphArea(glyph, g2);
        }

        g2.dispose();
    }

    //~ Inner Interfaces -------------------------------------------------------

    //--------------//
    // ItemRenderer //
    //--------------//
    /**
     * Used to plug additional items renderers to this view.
     */
    public static interface ItemRenderer
    {
        //~ Methods ------------------------------------------------------------

        void renderItems (Graphics2D g);
    }
}
