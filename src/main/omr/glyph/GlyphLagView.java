//-----------------------------------------------------------------------//
//                                                                       //
//                        G l y p h L a g V i e w                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph;

import omr.lag.Lag;
import omr.lag.LagView;
import omr.lag.Section;
import omr.util.Logger;
import omr.util.Observer;
import omr.util.Subject;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Collection;

/**
 * Class <code>GlyphLagView</code> is a specific {@link omr.lag.LagView}
 * dedicated to the display and processing of glyphs.
 *
 * <p> One or several instances of {@link GlyphObserver} interface can be
 * connected to this view, for example to display information about the
 * processed glyph.
 *
 * <p> This class implements the {@link GlyphFocus} interface, so this view
 * can programmatically focus on a specified glyph.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphLagView
    extends LagView<GlyphLag, GlyphSection>
    implements GlyphFocus,
               GlyphSubject
{
    //~ Static variables/initializers -------------------------------------

    protected static final Logger logger = Logger.getLogger(GlyphLagView.class);

    //~ Instance variables ------------------------------------------------

    /** Directory of Glyphs */
    protected final transient GlyphDirectory directory;

    /** Subject for glyph observers if any */
    protected final transient DefaultGlyphSubject glyphSubject 
            = new DefaultGlyphSubject();

    //~ Constructors -----------------------------------------------------

    //--------------//
    // GlyphLagView //
    //--------------//
    /**
     * Create a GlyphLagView as a LagView, with lag and potential specific
     * collection of sections
     *
     * @param lag the related lag
     * @param specificSections the specific sections if any, otherwise null
     */
    public GlyphLagView (GlyphLag                  lag,
                         Collection<GlyphSection>  specificSections,
                         GlyphDirectory            directory)
    {
        super(lag, specificSections);
        this.directory = directory;
    }

    //~ Methods -----------------------------------------------------------

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
        colorizeGlyph(glyph, glyph.getColor());
    }

    //---------------//
    // colorizeGlyph //
    //---------------//
    /**
     * Colorize a glyph with a specific color. If this color is null, then
     * the glyph is actually reset to its default section colors
     *
     * @param glyph the glyph at hand
     * @param color the specific color (may be null, to trigger a reset)
     */
    public void colorizeGlyph(Glyph glyph,
                              Color color)
    {
        if (color != null) {
            glyph.colorize(viewIndex, color);
        } else {
            glyph.recolorize(viewIndex);
        }
    }

    //-------------//
    // addObserver //
    //-------------//
    /**
     * Connect a GlyphObserver to this view, in order to display related
     * glyph information
     *
     * @param glyphObserver the observer to connect
     */
    public void addObserver (GlyphObserver glyphObserver)
    {
        glyphSubject.addObserver(glyphObserver);
    }

    //----------------//
    // removeObserver //
    //----------------//
    public void removeObserver (GlyphObserver glyphObserver)
    {
        glyphSubject.removeObserver(glyphObserver);
    }

    //-----------------//
    // notifyObservers //
    //-----------------//
    public void notifyObservers (Glyph glyph)
    {
        glyphSubject.notifyObservers(glyph);
    }

    //---------------//
    // setFocusGlyph //
    //---------------//
    public void setFocusGlyph (Glyph glyph)
    {
        Rectangle rect = null;
        if (glyph != null) {
            rect = glyph.getContourBox();
            glyphSelected(glyph, rect.getLocation());
        } else {
            glyphSelected(null, null);
        }

        setFocusRectangle(rect);

        notifyObservers(glyph);

        // Empty section info
        notifyObservers((Section) null);
    }

    //---------------//
    // setFocusGlyph //
    //---------------//
    public void setFocusGlyph (int id)
    {
        Glyph glyph = getGlyphById(id);
        if (glyph != null) {
            setFocusGlyph(glyph);
        } else {
            logger.warning ("Glyph " + id + " not found.");
        }
    }

    //-----------------//
    // setFocusSection //
    //-----------------//
    public void setFocusSection (GlyphSection section)
    {
        super.setFocusSection(section);
        if (section != null) {
            notifyObservers(section.getGlyph());
        }
    }

    //--------------//
    // getGlyphById //
    //--------------//
    public Glyph getGlyphById (int id)
    {
        return directory.getEntity(id);
    }

    //---------------//
    // deassignGlyph //
    //---------------//
    public void deassignGlyph (Glyph glyph)
    {
        logger.warning("Deassign action is not yet implemented for a " +
                       glyph.getShape() + " glyph.");
    }

    //---------------//
    // setFocusPoint //
    //---------------//
    /**
     * Selection of a glyph by point designation. Registered observers are
     * notified of the glyph information. The method {@link #glyphSelected}
     * is called with the glyph information, whether the glyph lookup has
     * succeeded or not.
     *
     * @param pt the selected point in model pixel coordinates
     */
    @Override
        public void setFocusPoint (Point pt)
    {
        ///logger.info(getClass() + " setFocusPoint " + pt);

        // First, provide info related to designated point
        super.setFocusPoint(pt);

        // Then, look for a glyph selection
        Glyph glyph = null;

        final GlyphSection section = lookupSection(pt);
        if (section != null) {
            glyph = section.getGlyph();
        }

        glyphSelected(glyph, pt);       // glyph may be null
        notifyObservers(glyph);
    }

    //------------//
    // pointAdded //
    //------------//
    @Override
        public void pointAdded (MouseEvent e,
                                Point pt)
    {
        if (logger.isFineEnabled()) {
            logger.fine("GlyphLagView pointAdded");
        }

        // First, provide info related to designated point
        super.pointSelected(e, pt);

        // Then, look for a glyph selection
        Glyph glyph = null;

        final GlyphSection section = lookupSection(pt);
        if (section != null) {
            glyph = section.getGlyph();
        }

        glyphAdded(glyph, pt);
    }

    //---------------//
    // glyphSelected //
    //---------------//
    /**
     * Meant to be overridden by subclasses when a real processing is
     * desired for the selected glyph.
     *
     * @param glyph the selected glyph, which may be null
     * @param pt the designated point
     */
    protected void glyphSelected (Glyph glyph,
                                  Point pt)
    {
        ///logger.info(getClass() + " glyphSelected " + glyph);
        // Empty by default
    }

    //------------//
    // glyphAdded //
    //------------//
    /**
     * Meant to be overridden by subclasses when a real processing is
     * desired for the added glyph
     *
     * @param glyph the added glyph, which may be null
     * @param pt the designated point
     */
    protected void glyphAdded (Glyph glyph,
                               Point pt)
    {
        // Empty by default
        if (logger.isFineEnabled()) {
            logger.fine ("Empty GlyphLagView glyphAdded " + glyph);
        }
    }
}
