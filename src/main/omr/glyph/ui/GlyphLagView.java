//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h L a g V i e w                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.*;

import omr.lag.LagView;

import omr.selection.Selection;
import omr.selection.SelectionHint;

import omr.ui.view.Zoom;

import omr.util.Logger;

import java.awt.*;
import java.awt.event.*;
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

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphLagView.class);

    //~ Instance fields --------------------------------------------------------

    /** Directory of Glyphs */
    protected final transient GlyphModel model;

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
     * @param lag the related lag
     * @param specificSections the specific sections if any, otherwise null
     * @param model the related glyph model
     */
    public GlyphLagView (GlyphLag                 lag,
                         Collection<GlyphSection> specificSections,
                         GlyphModel               model)
    {
        super(lag, specificSections);
        this.model = model;
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
        colorizeGlyph(glyph, glyph.getColor());
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
            model.deassignGlyphShape(glyph);
        }
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
        case PIXEL :
        case VERTICAL_SECTION_ID :
        case HORIZONTAL_SECTION_ID :
        case VERTICAL_GLYPH_ID :
        case HORIZONTAL_GLYPH_ID :

            // Check for glyph information
            if (showingSpecifics &&
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
                    glyph.renderBoxArea(g, z);
                }
            }
        }
    }
}
