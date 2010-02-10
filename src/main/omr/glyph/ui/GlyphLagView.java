//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h L a g V i e w                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.*;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;
import omr.glyph.text.OcrChar;
import omr.glyph.text.OcrLine;
import omr.glyph.text.TextInfo;

import omr.lag.ui.LagView;

import omr.log.Logger;

import omr.math.Circle;
import omr.math.Line;
import omr.math.Line.UndefinedLineException;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.selection.GlyphEvent;
import omr.selection.GlyphIdEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.RunEvent;
import omr.selection.SectionEvent;
import omr.selection.SelectionHint;
import static omr.selection.SelectionHint.*;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.ui.util.UIUtilities;

import omr.util.WeakPropertyChangeListener;

import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * Class <code>GlyphLagView</code> is a specific {@link omr.lag.ui.LagView}
 * dedicated to the display and processing of glyphs.
 *
 * @author Hervé Bitteur
 */
public class GlyphLagView
    extends LagView<GlyphLag, GlyphSection>
    implements PropertyChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphLagView.class);

    /** Events this entity is interested in */
    private static final Collection<Class<?extends UserEvent>> eventClasses;

    static {
        eventClasses = new ArrayList<Class<?extends UserEvent>>();
        eventClasses.add(GlyphEvent.class);
        eventClasses.add(GlyphIdEvent.class);
        eventClasses.add(GlyphSetEvent.class);
    }

    //~ Instance fields --------------------------------------------------------

    /** Specific glyphs for display & lookup */
    protected final Collection<?extends Glyph> specificGlyphs;

    /** Directory of Glyphs */
    protected final GlyphsController controller;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // GlyphLagView //
    //--------------//
    /**
     * Create a GlyphLagView as a LagView, with lag with potentially a
     * collection of specific of sections *OR* a collection of specific glyphs.
     *
     * @param lag              the related lag
     * @param specificSections the specific sections if any, otherwise null
     * @param showingSpecifics dynamic constant for actually showing specifics
     * @param controller       the related glyph controller
     * @param specificGlyphs   the specific glyphs if any, otherwise null
     */
    public GlyphLagView (GlyphLag                   lag,
                         Collection<GlyphSection>   specificSections,
                         Constant.Boolean           showingSpecifics,
                         GlyphsController           controller,
                         Collection<?extends Glyph> specificGlyphs)
    {
        super(
            lag,
            specificSections,
            showingSpecifics,
            controller.getLocationService());

        this.controller = controller;
        controller.addView(this);

        // Remember specific glyphs
        if (specificGlyphs != null) {
            this.specificGlyphs = specificGlyphs;
        } else {
            this.specificGlyphs = new ArrayList<Glyph>(0);
        }

        // (Weakly) listening on ViewParameters properties
        ViewParameters.getInstance()
                      .addPropertyChangeListener(
            new WeakPropertyChangeListener(this));
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getController //
    //---------------//
    public GlyphsController getController ()
    {
        return controller;
    }

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

        if (controller != null) {
            return controller.getGlyphById(id);
        } else {
            return null;
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
    @Override
    public GlyphLag getLag ()
    {
        return (GlyphLag) lag;
    }

    //---------------------//
    // getSpecificSections //
    //---------------------//
    public Collection<?extends Glyph> getSpecificGlyphs ()
    {
        return specificGlyphs;
    }

    //-------------------//
    // colorizeAllGlyphs //
    //-------------------//
    /**
     * Colorize all glyphs of the lag
     */
    public void colorizeAllGlyphs ()
    {
        // Empty by default
    }

    //---------------//
    // colorizeGlyph //
    //---------------//
    /**
     * Colorize a glyph according to its shape current status
     *
     * @param viewIndex index to proper section views
     * @param glyph the glyph at hand
     */
    public void colorizeGlyph (int   viewIndex,
                               Glyph glyph)
    {
        // Empty by default
    }

    //---------------//
    // colorizeGlyph //
    //---------------//
    /**
     * Colorize a glyph with a specific color. If this color is null, then the
     * glyph is actually reset to its default section colors
     *
     * @param viewIndex index to proper section views
     * @param glyph the glyph at hand
     * @param color the specific color (may be null, to trigger a reset)
     */
    public void colorizeGlyph (int   viewIndex,
                               Glyph glyph,
                               Color color)
    {
        if (color != null) {
            glyph.colorize(viewIndex, color);
        } else {
            glyph.recolorize(viewIndex);
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
    public Set<Glyph> lookupGlyphs (Rectangle rect)
    {
        Set<Glyph> found = null;
        GlyphLag   gLag = getLag();

        // Specific glyphs if any
        if (showingSpecifics()) {
            found = gLag.lookupGlyphs(specificGlyphs, rect);
        }

        if ((found == null) || found.isEmpty()) {
            found = gLag.lookupGlyphs(rect);
        }

        return found;
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered on selection notification.  We forward glyph
     * information.
     *
     * @param event the notified event
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            // Keep normal lag behavior, including specific sections
            // (interest in sheet location, section and section ID)
            super.onEvent(event);

            // Additional tasks about glyphs
            if (event instanceof SheetLocationEvent) { // Location => Glyph(s)
                handleEvent((SheetLocationEvent) event);
            } else if (event instanceof SectionEvent) { // Section => Glyph
                handleEvent((SectionEvent<GlyphSection>) event);
            } else if (event instanceof GlyphEvent) { // Glyph => glyph contour & GlyphSet update
                handleEvent((GlyphEvent) event);
            } else if (event instanceof GlyphIdEvent) { // Glyph Id => Glyph
                handleEvent((GlyphIdEvent) event);
            }
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
    }

    //----------------//
    // propertyChange //
    //----------------//
    public void propertyChange (PropertyChangeEvent evt)
    {
        // Whatever the property change, we simply repaint the view
        repaint();
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the  display using the colors of the glyphs
     */
    @Override
    public void refresh ()
    {
        colorizeAllGlyphs();
        repaint();
    }

    //-----------//
    // subscribe //
    //-----------//
    /**
     * Subscribe to location, lag and glyphs events
     */
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
            lag.getSelectionService()
               .subscribe(eventClass, this);
        }
    }

    //-------------//
    // unsubscribe //
    //-------------//
    /**
     * Unsubscribe from location, lag and glyphs events
     */
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
            lag.getSelectionService()
               .unsubscribe(eventClass, this);
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
     * @return true if the glyph area has actually been rendered
     */
    protected boolean renderGlyphArea (Glyph    glyph,
                                       Graphics g)
    {
        // Check the clipping
        Rectangle box = glyph.getContourBox();

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
        // Only in Glyph selection mode (?)
        // TODO: Do we want to combine display of selected glyphs & selected sections???
        if (ViewParameters.getInstance()
                          .isSectionSelectionEnabled()) {
            super.renderItems(g);

            return;
        }

        // Mark the current members of the glyph set
        Set<Glyph> glyphs = getLag()
                                .getSelectedGlyphSet();

        if ((glyphs == null) || glyphs.isEmpty()) {
            return;
        }

        // Decorations first
        Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1f);
        g.setColor(Color.blue);

        for (Glyph glyph : glyphs) {
            // Draw circle arc or stick average line
            if (glyph.getShape() == Shape.SLUR) {
                if (ViewParameters.getInstance()
                                  .isCirclePainting()) {
                    Circle circle = SlurInspector.computeCircle(glyph);

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            String.format(
                                "dist=%g " + circle.toString(),
                                circle.getDistance()));
                    }

                    drawCircle(circle, g);
                }
            } else if (ViewParameters.getInstance()
                                     .isLinePainting()) {
                if (glyph instanceof Stick) {
                    drawStickLine((Stick) glyph, g);
                }
            }

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
        }

        ((Graphics2D) g).setStroke(oldStroke);

        // Glyph areas second, using XOR mode for the area
        g.setColor(Color.black);
        g.setXORMode(Color.darkGray);

        for (Glyph glyph : glyphs) {
            renderGlyphArea(glyph, g);
        }
    }

    //------------//
    // drawCircle //
    //------------//
    /**
     * Draw the approximating circle of a slur
     */
    private void drawCircle (Circle   circle,
                             Graphics g)
    {
        CubicCurve2D.Double curve = circle.getCurve();
        Graphics2D          g2 = (Graphics2D) g;

        if (curve != null) {
            // Draw the bezier arc
            g2.draw(curve);
        } else {
            // Draw the full circle
            int radius = (int) Math.rint(circle.getRadius());
            g.drawOval(
                (int) Math.rint(circle.getCenter().x - radius),
                (int) Math.rint(circle.getCenter().y - radius),
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
                                Graphics g)
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

            g.drawLine(a.x, a.y, b.x, b.y);
        } catch (UndefinedLineException ignored) {
            // Not enough points
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     *  Interest in sheet location => [active] glyph(s)
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

        if ((hint != LOCATION_ADD) && (hint != LOCATION_INIT)) {
            return;
        }

        if (rect == null) {
            return;
        }

        if ((rect.width > 0) || (rect.height > 0)) {
            // This is a non-degenerated rectangle
            // Look for enclosed active glyphs
            if (subscribersCount(GlyphSetEvent.class) > 0) {
                // Look for enclosed glyphs
                Set<Glyph> glyphsFound = lookupGlyphs(rect);

                // Publish Glyph
                Glyph glyph = glyphsFound.isEmpty() ? null
                              : glyphsFound.iterator()
                                           .next();
                publish(new GlyphEvent(this, hint, movement, glyph));

                // Publish GlyphSet
                publish(new GlyphSetEvent(this, hint, movement, glyphsFound));
            }
        } else {
            // This is just a point
            // Give priority to virtual glyph rectangle
            // If a section has just been found, forward its assigned glyph
            if ((subscribersCount(GlyphEvent.class) > 0) &&
                (subscribersCount(SectionEvent.class) > 0)) { // TODO GlyphLag itself

                Glyph glyph = getLag()
                                  .lookupVirtualGlyph(
                    new PixelPoint(rect.getLocation()));

                if (glyph == null) {
                    GlyphSection section = getLag()
                                               .getSelectedSection();

                    if (section != null) {
                        glyph = section.getGlyph();
                    }
                }

                // Publish Glyph
                publish(new GlyphEvent(this, hint, movement, glyph));
            }
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Section => assigned glyph
     * @param sectionEvent
     */
    private void handleEvent (SectionEvent<GlyphSection> sectionEvent)
    {
        SelectionHint hint = sectionEvent.hint;
        MouseMovement movement = sectionEvent.movement;
        GlyphSection  section = sectionEvent.section;

        if (hint == SECTION_INIT) {
            // Select related Glyph if any
            if (section != null) {
                publish(
                    new GlyphEvent(this, hint, movement, section.getGlyph()));
            }
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Glyph => glyph contour & GlyphSet update
     * @param glyphEvent
     */
    private void handleEvent (GlyphEvent glyphEvent)
    {
        SelectionHint hint = glyphEvent.hint;
        MouseMovement movement = glyphEvent.movement;
        Glyph         glyph = glyphEvent.glyph;

        if ((hint == GLYPH_INIT) || (hint == GLYPH_MODIFIED)) {
            // Display glyph contour
            if (glyph != null) {
                PixelRectangle box = glyph.getContourBox();
                publish(new SheetLocationEvent(this, hint, movement, box));
            }
        }

        // In glyph-selection mode, for non-transient glyphs
        // (and only if we have interested subscribers)
        if ((hint != GLYPH_TRANSIENT) &&
            !ViewParameters.getInstance()
                           .isSectionSelectionEnabled() &&
            (subscribersCount(GlyphSetEvent.class) > 0)) {
            // Update (vertical) glyph set
            Set<Glyph> glyphs = getLag()
                                    .getSelectedGlyphSet();

            if (glyphs == null) {
                glyphs = new LinkedHashSet<Glyph>();
            }

            if (hint == LOCATION_ADD) {
                // Adding to (or Removing from) the set of glyphs
                if (glyph != null) {
                    if (glyphs.contains(glyph)) {
                        glyphs.remove(glyph);
                    } else {
                        glyphs.add(glyph);
                    }

                    publish(new GlyphSetEvent(this, hint, movement, glyphs));
                }
            } else {
                // Overwriting the set of glyphs
                if (glyph != null) {
                    // Make a one-glyph set
                    glyphs = Glyphs.sortedSet(glyph);
                } else {
                    glyphs = Glyphs.sortedSet();
                }

                publish(new GlyphSetEvent(this, hint, movement, glyphs));
            }
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Glyph ID => glyph
     * @param glyphIdEvent
     */
    private void handleEvent (GlyphIdEvent glyphIdEvent)
    {
        SelectionHint hint = glyphIdEvent.hint;
        MouseMovement movement = glyphIdEvent.movement;
        int           id = glyphIdEvent.getData();

        // Nullify Run  entity
        publish(new RunEvent(this, hint, movement, null));

        // Nullify Section entity
        publish(new SectionEvent<GlyphSection>(this, hint, movement, null));

        // Report Glyph entity (which may be null)
        publish(new GlyphEvent(this, hint, movement, getLag().getGlyph(id)));
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
