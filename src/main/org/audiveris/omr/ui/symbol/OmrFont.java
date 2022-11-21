//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         O m r F o n t                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class <code>OmrFont</code> is meant to simplify the use of rendering symbols when using a
 * Text or a Music font.
 *
 * @author Hervé Bitteur
 */
public abstract class OmrFont
        extends Font
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(OmrFont.class);

    /** Ratio to be applied for tiny symbols. */
    public static final double RATIO_TINY = constants.tinyRatio.getValue();

    /** Ratio to be applied for small shapes. */
    public static final double RATIO_SMALL = constants.smallRatio.getValue();

    /** AffineTransform for tiny displays. */
    public static final AffineTransform TRANSFORM_TINY = AffineTransform.getScaleInstance(
            RATIO_TINY,
            RATIO_TINY);

    /** AffineTransform for small shapes. */
    public static final AffineTransform TRANSFORM_SMALL = AffineTransform.getScaleInstance(
            RATIO_SMALL,
            RATIO_SMALL);

    /** Default color for images. */
    public static final Color defaultImageColor = Color.BLACK;

    /** Needed for font size computation. */
    public static final FontRenderContext frc = new FontRenderContext(null, true, true);

    /** Cache for created fonts. No style, no size. */
    private static final Map<String, Font> fontCache = new HashMap<>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new OmrFont object.
     *
     * @param fontName  the font name
     * @param fileName  the file name if any
     * @param style     generally PLAIN
     * @param pointSize the point size of the font
     */
    protected OmrFont (String fontName,
                       String fileName,
                       int style,
                       float pointSize)
    {
        super(getFont(fontName, fileName, style, pointSize));
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createFont //
    //------------//
    /**
     * Create and register the desired font.
     *
     * @param fontName  font name (e.g. "Finale Jazz")
     * @param fileName  file name (e.g. "FinaleJazz.otf")
     * @param style     the desired font style (generally 0 for PLAIN)
     * @param pointSize the desired point size for this font
     * @return the cached or created font
     */
    private static Font createFont (String fontName,
                                    String fileName,
                                    int style,
                                    float pointSize)
    {
        logger.debug("OmrFont.createFont fontName: {} fileName: {}", fontName, fileName);

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        if (logger.isTraceEnabled()) {
            for (String familyName : ge.getAvailableFontFamilyNames()) {
                logger.info("Font familyName: {}", familyName);
            }
        }

        Font font;

        // First, lookup our own fonts (defined in "res" folder)
        if (fileName != null)
        try {
            final URL url = UriUtil.toURI(WellKnowns.RES_URI, fileName).toURL();
            logger.debug("Font url={}", url);

            try (InputStream input = url.openStream()) {
                logger.debug("Found file {}", fileName);
                font = Font.createFont(Font.TRUETYPE_FONT, input);
                fontCache.put(fontName, font);
                final boolean added = ge.registerFont(font);
                logger.debug("Created custom font {} added:{}", font, added);

                return font.deriveFont(style, pointSize);
            } catch (FontFormatException |
                     IOException ex) {
                logger.debug("Could not create custom font {} " + ex, fileName);
            }
        } catch (MalformedURLException ex) {
            logger.warn("MalformedURLException", ex);
        }

        // Finally, try a platform font
        font = new Font(fontName, style, (int) pointSize);
        fontCache.put(fontName, font);
        logger.debug("Using platform font {}", font.getFamily());

        return font;
    }

    //---------//
    // getFont //
    //---------//
    /**
     * Retrieve the desired font, either from cache or newly created.
     *
     * @param fontName  font name (e.g. "Finale Jazz")
     * @param fileName  file name (e.g. "FinaleJazz.otf")
     * @param style     the desired font style (generally 0 for PLAIN)
     * @param pointSize the desired point size for this font
     * @return the cached or created font
     */
    private static Font getFont (String fontName,
                                 String fileName,
                                 int style,
                                 float pointSize)
    {
        // Font already registered?
        final Font font = fontCache.get(fontName);

        if (font != null) {
            logger.debug("Using cached font {}", fontName);

            return font.deriveFont(style, pointSize);
        }

        return createFont(fontName, fileName, style, pointSize);
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
     * Build a TextLayout from a String of OmrFont characters,
     * transformed by the provided AffineTransform if any.
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

    //-------//
    // paint //
    //-------//
    /**
     * This is the general paint method for drawing a symbol layout, at a specified location,
     * using a specified alignment.
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio smallRatio = new Constant.Ratio(
                0.67,
                "Ratio applied to small shapes (cue/grace head or clef change)");

        private final Constant.Ratio tinyRatio = new Constant.Ratio(
                0.5,
                "Ratio applied to tiny symbols (generally for buttons)");
    }
}
