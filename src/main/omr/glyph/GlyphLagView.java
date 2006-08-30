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
 * <li>*_GLYPH
 * <li>*_GLYPH_ID
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
    //~ Static variables/initializers -------------------------------------

    protected static final Logger logger = Logger.getLogger(GlyphLagView.class);

    //~ Instance variables ------------------------------------------------

    /** Directory of Glyphs */
    protected final transient GlyphDirectory directory;

    /** Flag indicating that point is being added */
    protected transient volatile boolean addingGlyph = false;

    /** Output selection for Glyph information */
    protected Selection glyphSelection;

    /** Output selection for Glyph Set information */
    protected Selection glyphSetSelection;

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
     * @param directory where to get glyph from its id
     */
    public GlyphLagView (GlyphLag                  lag,
                         Collection<GlyphSection>  specificSections,
                         GlyphDirectory            directory)
    {
        super(lag, specificSections);
        this.directory = directory;
    }

    //~ Methods -----------------------------------------------------------

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
    // renderItems //
    //-------------//
    @Override
        protected void renderItems (Graphics g)
    {
        Zoom z = getZoom();

        // Mark the current members of the glyph set
        if (glyphSetSelection != null) {
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity();
            if (glyphs != null && glyphs.size() > 0) {
                g.setColor(Color.black);
                g.setXORMode(Color.darkGray);
                for (Glyph glyph : glyphs) {
                    glyph.renderBoxArea(g, z);
                }
            }
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

        addingGlyph = true;

        // First, provide info related to designated point
        super.pointAdded(e, pt);

        // Then, look for a glyph selection
        Glyph glyph = null;

        final GlyphSection section = lookupSection(pt);
        if (section != null) {
            glyph = section.getGlyph();
        }

        glyphAdded(glyph, pt);

        addingGlyph = false;
    }

    //------------//
    // glyphAdded //
    //------------//
    /**
     * Addition of a glyph to a collection of selected glyphs. Il the
     * collection already contains that glyph, the glyph is in fact removed
     * from the collection
     *
     * @param glyph the to-be-added glyph, which may be null
     * @param pt the designated point
     */
    protected void glyphAdded (Glyph glyph,
                               Point pt)
    {
        if (logger.isFineEnabled()) {
            logger.fine ("GlyphLagView glyphAdded. glyph=" + glyph);
        }

        if (glyphSetSelection != null) {
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity();
            if (glyph != null) {
                // Add to or remove from the collection of selected glyphs
                if (glyphs.contains(glyph)) {
                    glyphs.remove(glyph);
                } else {
                    glyphs.add(glyph);
                }
            }
            glyphSetSelection.setEntity(glyphs, null);
        }
    }

    //---------//
    // update  //
    //---------//
    @Override
        public void update(Selection selection,
                           SelectionHint hint)
    {
        ///logger.info("GlyphLagView. selection=" + selection + " hint=" + hint);

        // Default lag view behavior, including specifics
        super.update(selection, hint);

        // Check for glyph information
        if (showingSpecifics &&
            sectionSelection != null &&
            glyphSelection != null) {
            switch (selection.getTag()) {
            case PIXEL :
            case VERTICAL_SECTION_ID :
            case HORIZONTAL_SECTION_ID :
            case VERTICAL_GLYPH_ID :
            case HORIZONTAL_GLYPH_ID :
                // Current Section (perhaps null) is in Section Selection
                if (sectionSelection != null) {
                    GlyphSection section =
                            (GlyphSection) sectionSelection.getEntity();
                    if (section != null) {
                        glyphSelection.setEntity(section.getGlyph(), hint);
                    }
                }
                break;

            default :
            }
        }
    }
}
