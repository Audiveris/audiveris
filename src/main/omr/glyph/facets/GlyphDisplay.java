//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h D i s p l a y                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.lag.Section;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Map;

/**
 * Interface {@code GlyphDisplay} defines the facet which handles the
 * way a glyph is displayed (its color, its image).
 *
 * @author Hervé Bitteur
 */
interface GlyphDisplay
    extends GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report a view on the map of attachments.
     * @return a (perhaps empty) map of attachments
     */
    Map<String, java.awt.Shape> getAttachments ();

    /**
     * Report the color to be used to colorize the provided glyph,
     * according to the color policy which is based on the glyph shape.
     * @return the related shape color of the glyph, or the predefined {@link
     * omr.ui.Colors#SHAPE_UNKNOWN} if the glyph has no related shape
     */
    Color getColor ();

    /**
     * Report an image of the glyph (which can be handed to the OCR)
     * @return a black & white image (contour box size )
     */
    BufferedImage getImage ();

    /**
     * Flag the glyph with a key and a rectangle.
     * This is meant to add arbitrary awt shapes to a glyph, mainly for display
     * and analysis purposes.
     * @param id the attachment ID
     * @param attachment awt shape to attach. If null, attachment is ignored.
     */
    void addAttachment (String         id,
                        java.awt.Shape attachment);

    /**
     * Set the display color of all sections that compose this glyph.
     * @param color     color for the whole glyph
     */
    void colorize (Color color);

    /**
     * Set the display color of all sections in provided collection.
     * @param sections  the collection of sections
     * @param color     the display color
     */
    void colorize (Collection<Section> sections,
                   Color               color);

    /**
     * Draw a basic representation of the glyph, using ascii characters.
     */
    void drawAscii ();

    /**
     * Reset the display color of all sections that compose this glyph.
     */
    void recolorize ();
}
