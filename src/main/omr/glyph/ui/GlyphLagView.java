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

import omr.log.Logger;

import omr.math.Circle;
import omr.math.Line;
import omr.math.Line.UndefinedLineException;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.script.ScriptRecording;

import omr.selection.GlyphEvent;
import omr.selection.GlyphIdEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.SectionEvent;
import omr.selection.SectionIdEvent;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.stick.Stick;

import omr.ui.view.Zoom;
import static omr.util.Synchronicity.*;

import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.util.*;
import java.util.List;

/**
 * Class <code>GlyphLagView</code> is a specific {@link omr.lag.LagView}
 * dedicated to the display and processing of glyphs.
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

    /** Events this entity is interested in */
    private static final Collection<Class<?extends UserEvent>> eventClasses = new ArrayList<Class<?extends UserEvent>>();

    static {
        eventClasses.add(GlyphEvent.class);
        eventClasses.add(GlyphIdEvent.class);
        eventClasses.add(GlyphSetEvent.class);
    }

    //~ Instance fields --------------------------------------------------------

    /** Specific glyphs for display & lookup */
    protected final Collection<?extends Glyph> specificGlyphs;

    /** Directory of Glyphs */
    protected final GlyphModel model;

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
        super(
            lag,
            model.getLocationService(),
            specificSections,
            showingSpecifics);
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
            model.deassignGlyphShape(ASYNC, glyph, ScriptRecording.RECORDING);
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

    //---------//
    // onEvent //
    //---------//
    /**
     * Notification about selection objects (for specific sections if any, for
     * color of a modified glyph, for display of selected glyph set).
     *
     * @param event the notified event
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onEvent (UserEvent event)
    {
        // Ignore RELEASING
        if (event.movement == MouseMovement.RELEASING) {
            return;
        }

        //        logger.info(
        //            "GlyphLagView/" + getClass().getSimpleName() + " " + getName() +
        //            ":" + event);

        // Default lag view behavior, including specifics
        super.onEvent(event);

        if (event instanceof SheetLocationEvent) {
            // If event originates from a user-provided location, publish the
            // designated glyph set if any (and also publish the last glyph in
            // the set)
            SheetLocationEvent sheetLocation = (SheetLocationEvent) event;

            if ((sheetLocation.hint == LOCATION_ADD) ||
                (sheetLocation.hint == LOCATION_INIT)) {
                Rectangle rect = sheetLocation.rectangle;

                if (rect != null) {
                    if ((rect.width > 0) || (rect.height > 0)) {
                        // Look for enclosed glyphs
                        List<Glyph> glyphsFound = lookupGlyphs(rect);

                        if (glyphsFound.size() > 0) {
                            lag.publish(
                                new GlyphEvent(
                                    this,
                                    sheetLocation.hint,
                                    sheetLocation.movement,
                                    glyphsFound.get(glyphsFound.size() - 1)));
                        } else {
                            lag.publish(
                                new GlyphEvent(
                                    this,
                                    sheetLocation.hint,
                                    sheetLocation.movement,
                                    null));
                        }

                        lag.publish(
                            new GlyphSetEvent(
                                this,
                                sheetLocation.hint,
                                sheetLocation.movement,
                                glyphsFound));
                    }
                }
            }
        } else if (event instanceof GlyphIdEvent) {
            // Lookup a specific glyph with proper ID
            GlyphIdEvent glyphIdEvent = (GlyphIdEvent) event;
            int          id = glyphIdEvent.getData();

            for (Glyph glyph : specificGlyphs) {
                if (glyph.getId() == id) {
                    lag.publish(
                        new GlyphEvent(this, glyphIdEvent.hint, null, glyph));

                    break;
                }
            }
        } else if (event instanceof SectionIdEvent) {
            // Beware: this is a bit tricky, it depends on order of event
            // notification. To be carefully checked ===========================
            // Check for glyph information with proper section id
            // This has already been done in the super LagView and the section
            // result must be in the SectionEvent
            // With this section, looku for a corresponding glyph
            if ((showingSpecifics != null) && showingSpecifics.getValue()) {
                // Current Section (perhaps null) is in Section Selection
                SectionEvent<GlyphSection> sectionEvent = (SectionEvent<GlyphSection>) lag.getLastEvent(
                    SectionEvent.class);
                GlyphSection               section = (sectionEvent != null)
                                                     ? sectionEvent.section : null;

                if (section != null) {
                    lag.publish(
                        new GlyphEvent(
                            this,
                            sectionEvent.hint,
                            null,
                            section.getGlyph()));
                }
            }
        } else if (event instanceof GlyphEvent) {
            // Glyph: just repaint the view if the glyph has been modified
            GlyphEvent glyphEvent = (GlyphEvent) event;

            if (glyphEvent.hint == SelectionHint.GLYPH_MODIFIED) {
                // Update display of this glyph
                Glyph glyph = glyphEvent.getData();

                if (glyph != null) {
                    colorizeGlyph(glyph);
                    repaint();
                }
            }
        } else if (event instanceof GlyphSetEvent) {
            repaint();
        }
    }

    //-----------//
    // subscribe //
    //-----------//
    @Override
    public void subscribe ()
    {
        super.subscribe();

        // Subscribe to glyph (lag) events
        if (logger.isFineEnabled()) {
            logger.fine(
                getClass().getName() + " GLV subscribing to " + eventClasses);
        }

        for (Class<?extends UserEvent> eventClass : eventClasses) {
            lag.subscribeStrongly(eventClass, this);
        }
    }

    //-------------//
    // unsubscribe //
    //-------------//
    @Override
    public void unsubscribe ()
    {
        super.unsubscribe();

        // Unsubscribe to glyph (lag) events
        if (logger.isFineEnabled()) {
            logger.fine(
                getClass().getName() + " GLV unsubscribing from " +
                eventClasses);
        }

        for (Class<?extends UserEvent> eventClass : eventClasses) {
            lag.unsubscribe(eventClass, this);
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
        Zoom          z = getZoom();

        // Mark the current members of the glyph set
        GlyphSetEvent glyphsEvent = (GlyphSetEvent) lag.getLastEvent(
            GlyphSetEvent.class);
        List<Glyph>   glyphs = (glyphsEvent != null) ? glyphsEvent.getData()
                               : null;

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
