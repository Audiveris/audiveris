//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         O m r F o n t                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.util.UriUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code OmrFont} is meant to simplify the use of rendering symbols when using a
 * Text or a Music font.
 *
 * @author Hervé Bitteur
 */
public abstract class OmrFont
        extends Font
{

    private static final Logger logger = LoggerFactory.getLogger(OmrFont.class);

    /** Default color for images. */
    public static final Color defaultImageColor = Color.BLACK;

    /** Needed for font size computation. */
    protected static final FontRenderContext frc = new FontRenderContext(null, true, true);

    /** Cache for fonts. No style, no size. */
    private static final Map<String, Font> fontCache = new HashMap<>();

    /**
     * Creates a new OmrFont object.
     *
     * @param name      the font name
     * @param style     generally PLAIN
     * @param pointSize the point size of the font
     */
    protected OmrFont (String name,
                       int style,
                       float pointSize)
    {
        super(createFont(name, style, pointSize));
    }

    //----------------//
    // getLineMetrics //
    //----------------//
    public LineMetrics getLineMetrics (String str)
    {
        return this.getLineMetrics(str, frc);
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a String of OmrFont characters
     * (transformed by the provided AffineTransform if any).
     *
     * @param str the string of proper codes
     * @param fat potential affine transformation
     * @return the (sized) TextLayout ready to be drawn
     */
    public TextLayout layout (String str,
                              AffineTransform fat)
    {
        Font font = (fat == null) ? this : this.deriveFont(fat);

        return new TextLayout(str, font, frc);
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a String of OmrFont characters.
     *
     * @param str the string of proper codes
     * @return the TextLayout ready to be drawn
     */
    public TextLayout layout (String str)
    {
        return layout(str, null);
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a ShapeSymbol.
     *
     * @param symbol the symbol to draw
     * @return the TextLayout ready to be drawn
     */
    public TextLayout layout (BasicSymbol symbol)
    {
        return layout(symbol.getString(), null);
    }

    //-------//
    // paint //
    //-------//
    /**
     * This is the general paint method for drawing a symbol layout, at
     * a specified location, using a specified alignment.
     *
     * @param g         the graphics environment
     * @param layout    what: the symbol, perhaps transformed
     * @param location  where: the precise location in the display
     * @param alignment how: the way the symbol is aligned WRT the location
     */
    public static void paint (Graphics2D g,
                              TextLayout layout,
                              Point2D location,
                              Alignment alignment)
    {
        try {
            // Compute symbol origin
            Rectangle2D bounds = layout.getBounds();
            Point2D toTextOrigin = alignment.toTextOrigin(bounds);

            // Draw the symbol
            layout.draw(g, (float) (location.getX() + toTextOrigin.getX()),
                        (float) (location.getY() + toTextOrigin.getY()));
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn("Cannot paint at " + location, ex);
        }
    }

    //------------//
    // createFont //
    //------------//
    private static Font createFont (String fontName,
                                    int style,
                                    float pointSize)
    {
        Font font;

        // Font already registered?
        font = fontCache.get(fontName);

        if (font != null) {
            logger.debug("Using cached font {}", fontName);

            return font.deriveFont(style, pointSize);
        }

        // Lookup our own fonts (defined in "res" folder)
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        try {
            InputStream input = null;

            try {
                URL url = UriUtil.toURI(WellKnowns.RES_URI, fontName + ".ttf").toURL();
                logger.debug("Font url={}", url);
                input = url.openStream();
                font = Font.createFont(Font.TRUETYPE_FONT, input);
                fontCache.put(fontName, font);
                ge.registerFont(font);
                logger.debug("Created custom font {}", fontName);

                return font.deriveFont(style, pointSize);
            } finally {
                if (input != null) {
                    input.close();
                }
            }
        } catch (FontFormatException |
                 IOException ex) {
            logger.debug("Could not create custom font {} " + ex, fontName);
        }

        // Finally, try a platform font
        font = new Font(fontName, style, (int) pointSize);
        fontCache.put(fontName, font);
        logger.debug("Using platform font {}", font.getFamily());

        return font;
    }
}
