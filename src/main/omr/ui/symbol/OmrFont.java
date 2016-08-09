//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         O m r F o n t                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.ui.symbol;

import omr.WellKnowns;

import omr.util.UriUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(OmrFont.class);

    /** Default interline value, consistent with base font: {@value}. */
    public static final int DEFAULT_INTERLINE = 16;

    /** Default staff height value, consistent with base font: {@value}. */
    public static final int DEFAULT_STAFF_HEIGHT = 67;

    /** Needed for font size computation. */
    protected static final FontRenderContext frc = new FontRenderContext(null, true, true);

    /** Default color for images. */
    public static final Color defaultImageColor = Color.BLACK;

    /** Cache for fonts. */
    private static final Map<String, Font> fontCache = new HashMap<String, Font>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new OmrFont object.
     *
     * @param name  the font name
     * @param style generally PLAIN
     * @param size  the point size of the font
     */
    protected OmrFont (String name,
                       int style,
                       int size)
    {
        super(createFont(name, style, size));
    }

    //~ Methods ------------------------------------------------------------------------------------
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
                              Point location,
                              Alignment alignment)
    {
        try {
            // Compute symbol origin
            Rectangle2D bounds = layout.getBounds();
            Point2D toTextOrigin = alignment.toTextOrigin(bounds);
            Point2D origin = new Point2D.Double(
                    location.x + toTextOrigin.getX(),
                    location.y + toTextOrigin.getY());

            // Draw the symbol
            layout.draw(g, (float) origin.getX(), (float) origin.getY());
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn("Cannot paint at " + location, ex);
        }
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

    //------------//
    // createFont //
    //------------//
    private static Font createFont (String fontName,
                                    int style,
                                    int size)
    {
        Font font;

        // Font already registered?
        font = fontCache.get(fontName);

        if (font != null) {
            logger.debug("Using cached font {}", fontName);

            return font.deriveFont(style, size);
        }

        // Lookup our own fonts (defined in "res" folder)
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try (InputStream input = UriUtil.toURI(WellKnowns.RES_URI, fontName + ".ttf").toURL().openStream()) {
            font = Font.createFont(Font.TRUETYPE_FONT, input);
            fontCache.put(fontName, font);
            ge.registerFont(font);
            logger.debug("Created custom font {}", fontName);
            return font.deriveFont(style, size);
        } catch (Exception ex) {
            logger.debug("Could not create custom font {} {}", fontName, ex);
        }

        // Finally, try a platform font
        font = new Font(fontName, style, size);
        fontCache.put(fontName, font);
        logger.debug("Using platform font {}", font.getFamily());

        return font;
    }
}
