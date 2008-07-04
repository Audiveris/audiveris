//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h L a g V i e w                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.*;
import omr.glyph.Shape;

import omr.lag.LagView;

import omr.math.Circle;
import omr.math.Line;
import omr.math.Line.UndefinedLineException;

import omr.selection.Selection;
import omr.selection.SelectionHint;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.stick.Stick;

import omr.ui.view.Zoom;

import omr.util.Logger;

import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.util.*;
import java.util.List;

/**
 * Class <code>GlyphLagView</code> is a specific {@link omr.lag.LagView}
 * dedicated to the display and processing of glyphs.
 *
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>PIXEL
 * <li>*_SECTION_ID
 * <li>*_GLYPH_ID
 * <li>GLYPH_SET
 * </ul>
 *
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>*_GLYPH
 * </ul>
 * </dl>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphLagView
    extends LagView<GlyphLag, GlyphSection>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphLagView.class);

    //~ Instance fields --------------------------------------------------------

    /** Specific glyphs for display & lookup */
    protected final Collection<?extends Glyph> specificGlyphs;

    /** Directory of Glyphs */
    protected final GlyphModel model;

    /** Output selection for Glyph information */
    protected Selection glyphSelection;

    /** Output selection for Glyph Set information */
    protected Selection glyphSetSelection;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // GlyphLagView //
    //--------------//
    /**
     * Create a GlyphLagView as a LagView, with lag and potential specific
     * collection of sections
     *
     * @param lag              the related lag
     * @param specificSections the specific sections if any, otherwise null
     * @param showingSpecifics dynamic constants for actually showing specifics
     * @param model            the related glyph model
     * @param specificGlyphs   the specific glyphs if any, otherwise null
     */
    public GlyphLagView (GlyphLag                   lag,
                         Collection<GlyphSection>   specificSections,
                         Constant.Boolean           showingSpecifics,
                         GlyphModel                 model,
                         Collection<?extends Glyph> specificGlyphs)
    {
        super(lag, specificSections, showingSpecifics);
        this.model = model;

        // Remember specific glyphs
        if (specificGlyphs != null) {
            this.specificGlyphs = specificGlyphs;
        } else {
            this.specificGlyphs = new ArrayList<Glyph>(0);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getGlyphById //
    //--------------//
    /**
     * Give access to a glyph, knowing its id.
     *
     * @param id the glyph id
     *
     * @return the corresponding glyph, or null if none
     */
    public Glyph getGlyphById (int id)
    {
        // Look up in specific glyphs first
        for (Glyph glyph : specificGlyphs) {
            if (glyph.getId() == id) {
                return glyph;
            }
        }

        if (model != null) {
            return model.getGlyphById(id);
        } else {
            return null;
        }
    }

    //-------------------//
    // setGlyphSelection //
    //-------------------//
    /**
     * Inject dependency on where we should write glyph information.
     *
     * @param glyphSelection the Glyph selection
     */
    public void setGlyphSelection (Selection glyphSelection)
    {
        this.glyphSelection = glyphSelection;
    }

    //----------------------//
    // setGlyphSetSelection //
    //----------------------//
    /**
     * Inject dependency on where we should write glyph set information.
     *
     * @param glyphSetSelection the Glyph Set selection
     */
    public void setGlyphSetSelection (Selection glyphSetSelection)
    {
        this.glyphSetSelection = glyphSetSelection;
    }

    //-------------------//
    // colorizeAllGlyphs //
    //-------------------//
    /**
     *
     */
    public void colorizeAllGlyphs ()
    {
        // Empty
    }

    //---------------//
    // colorizeGlyph //
    //---------------//
    /**
     * Colorize a glyph according to its shape current status
     *
     * @param glyph the glyph at hand
     */
    public void colorizeGlyph (Glyph glyph)
    {
        // Empty
    }

    //---------------//
    // colorizeGlyph //
    //---------------//
    /**
     * Colorize a glyph with a specific color. If this color is null, then the
     * glyph is actually reset to its default section colors
     *
     * @param glyph the glyph at hand
     * @param color the specific color (may be null, to trigger a reset)
     */
    public void colorizeGlyph (Glyph glyph,
                               Color color)
    {
        if (color != null) {
            glyph.colorize(viewIndex, color);
        } else {
            glyph.recolorize(viewIndex);
        }
    }

    //---------------//
    // deassignGlyph //
    //---------------//
    /**
     * Deassign the shape of a glyph
     *
     * @param glyph the glyph to be deassigned
     */
    public void deassignGlyph (Glyph glyph)
    {
        if (model != null) {
            model.deassignGlyphShape(glyph, true);
        }
    }

    //--------------//
    // lookupGlyphs //
    //--------------//
    /**
     * Lookup for <b>all</b> glyphs, view-specific ones if such collection
     * exists, otherwise lag glyphs, that are contained in the provided
     * rectangle
     *
     * @param rect the given rectangle
     *
     * @return the list of glyphs found, which may be empty
     */
    public List<Glyph> lookupGlyphs (Rectangle rect)
    {
        List<Glyph> found;

        // Specific glyphs if any
        if (specificGlyphs.size() > 0) {
            found = lag.lookupGlyphs(specificGlyphs, rect);
        } else {
            found = lag.lookupGlyphs(rect);
        }

        return found;
    }

    //--------//
    // update //
    //--------//
    /**
     * Notification about selection objects (for specific sections if any, for
     * color of a modified glyph, for display of selected glyph set).
     *
     * @param selection the notified selection
     * @param hint the processing hint if any
     */
    @Override
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        ///logger.info(getName() + " update. " + selection + " hint=" + hint);

        // Default lag view behavior, including specifics
        super.update(selection, hint);

        switch (selection.getTag()) {
        case SHEET_RECTANGLE :

            if ((hint == SelectionHint.LOCATION_ADD) ||
                (hint == SelectionHint.LOCATION_INIT)) {
                Rectangle rect = (Rectangle) selection.getEntity();

                if (rect != null) {
                    if ((rect.width > 0) || (rect.height > 0)) {
                        // Look for enclosed glyphs
                        List<Glyph> glyphsFound = lookupGlyphs(rect);

                        if (glyphsFound.size() > 0) {
                            glyphSelection.setEntity(
                                glyphsFound.get(glyphsFound.size() - 1),
                                hint);
                        } else {
                            glyphSelection.setEntity(null, hint);
                        }

                        if (glyphSetSelection != null) {
                            glyphSetSelection.setEntity(glyphsFound, hint);
                        }
                    }
                }
            }

            break;

        case VERTICAL_GLYPH_ID :
        case HORIZONTAL_GLYPH_ID :

            // Lookup a specific glyph with proper ID
            int id = (Integer) selection.getEntity();

            for (Glyph glyph : specificGlyphs) {
                if (glyph.getId() == id) {
                    glyphSelection.setEntity(glyph, hint);

                    break;
                }
            }

            break;

        case VERTICAL_SECTION_ID :
        case HORIZONTAL_SECTION_ID :

            // Check for glyph information
            if ((showingSpecifics != null) &&
                showingSpecifics.getValue() &&
                (sectionSelection != null) &&
                (glyphSelection != null)) {
                // Current Section (perhaps null) is in Section Selection
                if (sectionSelection != null) {
                    GlyphSection section = (GlyphSection) sectionSelection.getEntity();

                    if (section != null) {
                        glyphSelection.setEntity(section.getGlyph(), hint);
                    }
                }
            }

            break;

        case VERTICAL_GLYPH :

            if (hint == SelectionHint.GLYPH_MODIFIED) {
                // Update display of this glyph
                Glyph glyph = (Glyph) selection.getEntity();

                if (glyph != null) {
                    colorizeGlyph(glyph);
                    repaint();
                }
            }

            break;

        case GLYPH_SET :
            repaint();

            break;

        default :
        }
    }

    //-----------------//
    // renderGlyphArea //
    //-----------------//
    /**
     * Render the box area of a glyph, using inverted color
     *
     * @param glyph the glyph whose area is to be rendered
     * @param g the graphic context
     * @param z the display zoom
     * @return true if the glyph area has actually been rendered
     */
    protected boolean renderGlyphArea (Glyph    glyph,
                                       Graphics g,
                                       Zoom     z)
    {
        // Check the clipping
        Rectangle box = new Rectangle(glyph.getContourBox());
        z.scale(box);

        if (box.intersects(g.getClipBounds())) {
            g.fillRect(box.x, box.y, box.width, box.height);

            return true;
        } else {
            return false;
        }
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Render the collection of selected glyphs, if any
     *
     * @param g the graphic context
     */
    @Override
    protected void renderItems (Graphics g)
    {
        Zoom z = getZoom();

        // Mark the current members of the glyph set
        if (glyphSetSelection != null) {
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity(); // Compiler warning

            if ((glyphs != null) && (glyphs.size() > 0)) {
                g.setColor(Color.black);
                g.setXORMode(Color.darkGray);

                for (Glyph glyph : glyphs) {
                    renderGlyphArea(glyph, g, z);

                    // Draw circle arc here ?
                    if (glyph.getShape() == Shape.SLUR) {
                        if (ViewParameters.getInstance()
                                          .isCirclePainting()) {
                            Circle circle = SlurGlyph.computeCircle(glyph);

                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    String.format(
                                        "dist=%g " + circle.toString(),
                                        circle.getDistance()));
                            }

                            drawCircle(circle, g, z);
                        }
                    } else if (ViewParameters.getInstance()
                                             .isLinePainting()) {
                        if (glyph instanceof Stick) {
                            drawStickLine((Stick) glyph, g, z);
                        }
                    }
                }
            }
        }
    }

    //------------//
    // drawCircle //
    //------------//
    /**
     * Draw the approximating circle of a slur
     */
    private void drawCircle (Circle   circle,
                             Graphics g,
                             Zoom     z)
    {
        CubicCurve2D.Double curve = circle.getCurve();
        Graphics2D          g2 = (Graphics2D) g;

        if (curve != null) {
            // Draw the bezier arc
            g2.draw(
                new CubicCurve2D.Double(
                    z.scaled(curve.getX1()),
                    z.scaled(curve.getY1()),
                    z.scaled(curve.getCtrlX1()),
                    z.scaled(curve.getCtrlY1()),
                    z.scaled(curve.getCtrlX2()),
                    z.scaled(curve.getCtrlY2()),
                    z.scaled(curve.getX2()),
                    z.scaled(curve.getY2())));
        } else {
            // Draw the full circle
            int radius = z.scaled(circle.getRadius());
            g2.drawOval(
                z.scaled(circle.getCenter().x) - radius,
                z.scaled(circle.getCenter().y) - radius,
                2 * radius,
                2 * radius);
        }
    }

    //---------------//
    // drawStickLine //
    //---------------//
    /**
     * Draw the mean line of a stick
     */
    private void drawStickLine (Stick    stick,
                                Graphics g,
                                Zoom     z)
    {
        try {
            Line           line = stick.getLine();
            PixelRectangle box = stick.getContourBox();
            int            ext = constants.lineExtension.getValue();
            PixelPoint     a = new PixelPoint();
            PixelPoint     b = new PixelPoint();

            // Beware, these are vertical glyphs
            if (Math.abs(line.getInvertedSlope()) <= (Math.PI / 4)) {
                // Rather horizontal
                a.x = box.x - ext;
                a.y = line.xAt(a.x);
                b.x = box.x + box.width + ext;
                b.y = line.xAt(b.x);
            } else {
                // Rather vertical
                a.y = box.y - ext;
                a.x = line.yAt(a.y);
                b.y = box.y + box.height + ext;
                b.x = line.yAt(b.y);
            }

            g.drawLine(
                z.scaled(a.x),
                z.scaled(a.y),
                z.scaled(b.x),
                z.scaled(b.y));
        } catch (UndefinedLineException ignored) {
            // Not enough points
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

        /** Extension of line beyond the stick */
        final Constant.Integer lineExtension = new Constant.Integer(
            "Pixels",
            10,
            "Extension of line beyond the stick");
    }
}
