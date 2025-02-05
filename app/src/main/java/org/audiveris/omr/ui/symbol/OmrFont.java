//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         O m r F o n t                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
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
import java.util.Objects;

/**
 * Class <code>OmrFont</code> is meant to simplify the use of rendering symbols when using a
 * Text font or a Music font.
 *
 * @author Hervé Bitteur
 */
public abstract class OmrFont
        extends Font
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(OmrFont.class);

    /** Ratio to be applied for tiny symbols (as used in shape buttons). */
    public static final double RATIO_TINY = constants.tinyRatio.getValue();

    /** Ratio to be applied for small shapes. */
    public static final double RATIO_SMALL = constants.smallRatio.getValue();

    /** Ratio to be applied for metronome shapes. */
    public static final double RATIO_METRO = constants.metroRatio.getValue();

    //    /** AffineTransform for tiny displays. */
    //    public static final AffineTransform TRANSFORM_TINY =
    //            AffineTransform.getScaleInstance(RATIO_TINY, RATIO_TINY);

    /** AffineTransform for small shapes. */
    public static final AffineTransform TRANSFORM_SMALL = AffineTransform.getScaleInstance(
            RATIO_SMALL,
            RATIO_SMALL);

    /** AffineTransform for metronome shapes. */
    public static final AffineTransform TRANSFORM_METRO = AffineTransform.getScaleInstance(
            RATIO_METRO,
            RATIO_METRO);

    /** Default color for images. */
    public static final Color defaultImageColor = Color.BLACK;

    /** Needed for font size computation. */
    public static final FontRenderContext frc = new FontRenderContext(null, true, true);

    /**
     * Cache for all created fonts (music and text), based on name and point size.
     * <p>
     * Only the PLAIN style is cached.
     * If a different style is desired, the caller must derive it from the cached plain one.
     */
    private static final Map<String, Map<Integer, Font>> fontCache = new HashMap<>();

    //~ Constructors -------------------------------------------------------------------------------

    protected OmrFont (Font font)
    {
        super(font);
    }

    /**
     * Creates a new OmrFont object.
     *
     * @param fontName the font name
     * @param fileName the file name if any
     * @param style    generally PLAIN
     * @param size     the integer point size of the font
     */
    protected OmrFont (String fontName,
                       String fileName,
                       int style,
                       int size)
    {
        this(getFont(fontName, fileName, style, size));
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------------//
    // computeSize //
    //-------------//
    /**
     * Compute the point size so that the content would fit the target dimension.
     *
     * @param content the provided content string
     * @param dim     the target dimension
     * @return the best font size
     */
    public int computeSize (String content,
                            Dimension dim)
    {
        Objects.requireNonNull(content, "OmrFont.computeSize. Content is null");
        Objects.requireNonNull(content, "OmrFont.computeSize. Dimension is null");

        final GlyphVector glyphVector = createGlyphVector(frc, content);
        final Rectangle2D rect = glyphVector.getVisualBounds();
        final float ratio = (dim.width >= dim.height) //
                ? dim.width / (float) rect.getWidth()
                : dim.height / (float) rect.getHeight();
        final float s2d = getSize2D();
        final int sz = (int) Math.rint(ratio * s2d);
        logger.debug(
                "OmrFont.computeSize {} f: {} dim: {} ratio: {} size: {} content: {}",
                getFontName(),
                s2d,
                dim,
                ratio,
                sz,
                content);
        return sz;
    }

    //-----------------//
    // computeLocation //
    //-----------------//
    /**
     * Compute the baseline location, based on content and bounds.
     *
     * @param content the provided content string
     * @param bounds  the underlying glyph bounding box
     * @return the computed location
     */
    public Point computeLocation (String content,
                                  Rectangle bounds)
    {
        Objects.requireNonNull(content, "OmrFont.computeLocation. Content is null");
        Objects.requireNonNull(content, "OmrFont.computeLocation. Bounds are null");

        final GlyphVector glyphVector = createGlyphVector(frc, content);
        final Rectangle2D rect = glyphVector.getVisualBounds();
        final double rectDy = rect.getY() / rect.getHeight();
        final double ratio = (bounds.width >= bounds.height) //
                ? bounds.width / rect.getWidth()
                : bounds.height / rect.getHeight();
        final int y = bounds.y + (int) Math.rint(rectDy * ratio);

        return new Point(bounds.x, y);
    }

    //----------------//
    // getLineMetrics //
    //----------------//
    public LineMetrics getLineMetrics (String str)
    {
        return getLineMetrics(str, frc);
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
        final Font font = (fat == null) ? this : this.deriveFont(fat);

        return new TextLayout(str, font, frc);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-----------//
    // cacheFont //
    //-----------//
    /**
     * Cache the provided font into the global font cache.
     *
     * @param font the font to cache
     */
    protected static void cacheFont (Font font)
    {
        final String key = font.getName().replaceAll(" ", "");
        logger.debug("Caching font: {} key:{}", font, key);
        Map<Integer, Font> sizeMap = fontCache.get(key);

        if (sizeMap == null) {
            fontCache.put(key, sizeMap = new HashMap<>());
        }

        sizeMap.put(font.getSize(), font);
    }

    //------------//
    // createFont //
    //------------//
    /**
     * Create and register the desired font.
     *
     * @param fontName font name (e.g. "Finale Jazz")
     * @param fileName file name (e.g. "FinaleJazz.otf")
     * @param size     the desired point size for this font
     * @return the cached or created font
     */
    private static Font createFont (String fontName,
                                    String fileName,
                                    int size)
    {
        logger.debug("Creating fontName: {} fileName: {} size: {}", fontName, fileName, size);

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        if (logger.isTraceEnabled()) {
            for (String familyName : ge.getAvailableFontFamilyNames()) {
                logger.trace("Font familyName: {}", familyName);
            }
        }

        // First, lookup our own fonts (defined in "res" folder)
        if (fileName != null) {
            try {
                final URL url = UriUtil.toURI(WellKnowns.RES_URI, fileName).toURL();
                logger.debug("Font url={}", url);

                try (InputStream input = url.openStream()) {
                    logger.debug("Found file {}", fileName);
                    final Font font = Font.createFont(Font.TRUETYPE_FONT, input).deriveFont(
                            (float) size);
                    cacheFont(font);

                    final boolean added = ge.registerFont(font);
                    logger.debug("Created custom font: {} added:{}", font, added);

                    return font;
                } catch (FontFormatException | IOException ex) {
                    logger.debug("Could not create custom font: {} " + ex, fileName);
                }
            } catch (MalformedURLException ex) {
                logger.warn("MalformedURLException", ex);
            }
        }

        // Finally, try a platform font
        final Font font = new Font(fontName, Font.PLAIN, size);
        cacheFont(font);
        logger.debug("Using platform font: {}", font.getFamily());

        return font;
    }

    //---------------//
    // getCachedFont //
    //---------------//
    /**
     * Try to retrieve the font defined by its name and size from the font cache.
     *
     * @param fontName font name (family name actually)
     * @param size     desired font point size
     * @return the cached font or null
     */
    private static Font getCachedFont (String fontName,
                                       int size)
    {
        final String key = fontName.replaceAll(" ", "");
        final Map<Integer, Font> sizeMap = fontCache.get(key);

        if (sizeMap == null) {
            logger.debug("No sizeMap for {}", key);
            return null;
        }

        final Font font = sizeMap.get(size);

        return font;
    }

    //----------------------//
    // getCachedFontAnySize //
    //----------------------//
    /**
     * Try to retrieve a font defined by its name, whatever its size, from the font cache.
     *
     * @param fontName font name (family name actually)
     * @return the cached font or null
     */
    private static Font getCachedFontAnySize (String fontName)
    {
        final String key = fontName.replaceAll(" ", "");
        final Map<Integer, Font> sizeMap = fontCache.get(key);

        if (sizeMap == null || sizeMap.isEmpty()) {
            logger.debug("Null or empty sizeMap for {}", key);
            return null;
        }

        return sizeMap.entrySet().iterator().next().getValue();
    }

    //---------//
    // getFont //
    //---------//
    /**
     * Retrieve the desired font, either from font cache or newly created.
     *
     * @param fontName font name (e.g. "Finale Jazz")
     * @param fileName (optional) file name (e.g. "FinaleJazz.otf")
     * @param style    the desired font style (generally 0 for PLAIN)
     * @param size     the desired size for this font
     * @return the cached or created font
     */
    protected static Font getFont (String fontName,
                                   String fileName,
                                   int style,
                                   int size)
    {
        Font font = getCachedFont(fontName, size);

        if (font != null) {
            logger.trace("Using cached font {} {}", fontName, size);
        } else {
            // Try to derive from another size
            final Font any = getCachedFontAnySize(fontName);

            if (any != null) {
                font = any.deriveFont((float) size);
            }

            if (font == null) {
                // We need to create it
                font = createFont(fontName, fileName, size);
            }
        }

        return (style == Font.PLAIN) ? font : font.deriveFont(style);
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
            layout.draw(
                    g,
                    (float) (location.getX() + toTextOrigin.getX()),
                    (float) (location.getY() + toTextOrigin.getY()));
        } catch (ConcurrentModificationException ignored) { //
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
        private final Constant.Ratio metroRatio = new Constant.Ratio(
                0.5,
                "Ratio applied to metronome note shapes");

        private final Constant.Ratio smallRatio = new Constant.Ratio(
                0.67,
                "Ratio applied to small shapes (cue/grace head or clef change)");

        private final Constant.Ratio tinyRatio = new Constant.Ratio(
                0.5,
                "Ratio applied to tiny symbols (generally for buttons)");
    }
}
