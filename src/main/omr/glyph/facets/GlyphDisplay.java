//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h D i s p l a y                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.GlyphSection;

import omr.lag.Lag;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Collection;

/**
 * Interface {@code GlyphDisplay} defines the facet which handles the way a
 * glyph is displayed (its color, its image).
 *
 * @author Hervé Bitteur
 */
interface GlyphDisplay
    extends GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    //----------//
    // getColor //
    //----------//
    /**
     * Report the color to be used to colorize the provided glyph, according to
     * the color policy which is based on the glyph shape
     *
     * @return the related shape color of the glyph, or the predefined {@link
     * omr.glyph.Shape#missedColor} if the glyph has no related shape
     */
    Color getColor ();

    //----------//
    // getImage //
    //----------//
    /**
     * Report an image of the glyph (which can be handed to the OCR)
     * @return a black & white image (contour box size )
     */
    BufferedImage getImage ();

    //----------//
    // colorize //
    //----------//
    /**
     * Set the display color of all sections that compose this glyph.
     *
     * @param viewIndex index in the view list
     * @param color     color for the whole glyph
     */
    void colorize (int   viewIndex,
                   Color color);

    //----------//
    // colorize //
    //----------//
    /**
     * Set the display color of all sections that compose this stick.
     *
     * @param lag the containing lag
     * @param viewIndex index in the view list
     * @param color     color for the whole stick
     */
    void colorize (Lag   lag,
                   int   viewIndex,
                   Color color);

    //----------//
    // colorize //
    //----------//
    /**
     * Set the display color of all sections gathered by the provided list
     *
     * @param viewIndex the proper view index
     * @param sections  the collection of sections
     * @param color     the display color
     */
    void colorize (int                      viewIndex,
                   Collection<GlyphSection> sections,
                   Color                    color);

    //-----------//
    // drawAscii //
    //-----------//
    /**
     * Draw a basic representation of the glyph, using ascii characters
     */
    void drawAscii ();

    //------------//
    // recolorize //
    //------------//
    /**
     * Reset the display color of all sections that compose this glyph.
     *
     * @param viewIndex index in the view list
     */
    void recolorize (int viewIndex);
}
