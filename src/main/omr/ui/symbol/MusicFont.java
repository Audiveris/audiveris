//----------------------------------------------------------------------------//
//                                                                            //
//                             M u s i c F o n t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.Main;

import omr.glyph.Shape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code MusicFont} is meant to simplify the use of the
 * underlying music font when rendering picture or score views.
 *
 * <p>The strategy is to use a properly scaled instance of this class to carry
 * out the drawing of music symbols with the correct size. The scaling should
 * combine two factors:<ul>
 * <li>An interline value different from default {@link #DEFAULT_INTERLINE}</p>
 * <li>A zoom ratio different from 1</li></ul>
 *
 * <p>To get a properly scaled instance, use the convenient method
 * {@link #getFont(int)} which expects a staff height expressed in a count of
 * screen pixels (which thus combines the 2 factors listed above).</p>
 *
 * <p>There are two well-known pre-scaled instances of this class:<ul>
 * <li>{@link #baseMusicFont} for standard symbols (scale = 1)</li>
 * <li>{@link #iconMusicFont} for icon symbols (scale = 1/2)</li></ul>
 *
 * <p>The underlying font is <b>MusicalSymbols</b>
 * downloadable from http://simplythebest.net/fonts/fonts/musical_symbols.html
 * </p>
 *
 * @author Hervé Bitteur
 */
public class MusicFont
        extends OmrFont
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(MusicFont.class);

    /**
     * The music font name: {@value} (no other one is used).
     * Possibilities: MusicalSymbols, Symbola, Bravura
     */
    public static final String FONT_NAME = "MusicalSymbols";

    /**
     * Offset for code range.
     * 0xf000 for MusicalSymbols
     * 0x1d100 for Symbola or Bravura
     */
    public static final int CODE_OFFSET = 0xf000;

    /** Cache of font according to desired interline value */
    private static final Map<Integer, MusicFont> sizeMap = new HashMap<>();

    /** The music font used for default interline and no zoom */
    public static final MusicFont baseMusicFont = getFont(DEFAULT_INTERLINE);

    /** The music font used for icons (half-size) */
    public static final MusicFont iconMusicFont = getFont(
            DEFAULT_INTERLINE / 2);

    //~ Instance fields --------------------------------------------------------
    /** Precise interline height with this font */
    private final int fontInterline;

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // MusicFont //
    //-----------//
    /**
     * Creates a new MusicFont object.
     *
     * @param sizePts       the point size of the {@code Font}
     * @param fontInterline the (zoomed?) interline value for this font
     */
    MusicFont (int sizePts,
               int fontInterline)
    {
        super(FONT_NAME, Font.PLAIN, sizePts);
        this.fontInterline = fontInterline;
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // buildImage //
    //------------//
    /**
     * Build an image from the shape definition in MusicFont, using the
     * scaling determined by the provided interline value.
     *
     * @param shape     the desired shape
     * @param interline the related interline value
     * @param decorated true if shape display must use decorations
     * @return the image built with proper scaling, or null
     */
    public static BufferedImage buildImage (Shape shape,
                                            int interline,
                                            boolean decorated)
    {
        MusicFont font = getFont(interline);

        return font.buildImage(shape, decorated);
    }

    //------------//
    // buildImage //
    //------------//
    /**
     * Build an image from the shape definition in MusicFont, using the
     * intrinsic scaling of this font.
     *
     * @param shape     the desired shape
     * @param decorated true if shape display must use decorations
     * @return the image built with proper scaling, or null
     */
    public BufferedImage buildImage (Shape shape,
                                     boolean decorated)
    {
        ShapeSymbol symbol = Symbols.getSymbol(shape, decorated);

        if (symbol == null) {
            return null;
        } else {
            return symbol.buildImage(this);
        }
    }

    //----------------//
    // checkMusicFont //
    //----------------//
    /**
     * Check whether we have been able to load the font.
     *
     * @return true if OK
     */
    public static boolean checkMusicFont ()
    {
        if (baseMusicFont.getFamily().equals("Dialog")) {
            String msg = FONT_NAME + " font not found." + " Please install "
                         + FONT_NAME + ".ttf";
            logger.error(msg);

            if (Main.getGui() != null) {
                Main.getGui().displayError(msg);
            }

            return false;
        }

        return true;
    }

    //---------//
    // getFont //
    //---------//
    /**
     * Report the (cached) best font according to the desired interline
     * value.
     *
     * @param interline the desired (zoomed) interline in pixels
     * @return the font with proper size
     */
    public static MusicFont getFont (int interline)
    {
        MusicFont font = sizeMap.get(interline);

        if (font == null) {
            font = new MusicFont(4 * interline, interline);
            logger.debug("Adding music font for interline {}", interline);
            sizeMap.put(interline, font);
        }

        return font;
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a Shape, using its related String of
     * MusicFont characters, and potentially sized by an
     * AffineTransform instance.
     *
     * @param shape the shape to be drawn with MusicFont chars
     * @param fat   potential affine transformation
     * @return the (sized) TextLayout ready to be drawn
     */
    public TextLayout layout (Shape shape,
                              AffineTransform fat)
    {
        ShapeSymbol symbol = Symbols.getSymbol(shape);

        if (symbol == null) {
            logger.debug("No MusicFont symbol for {}", shape);
            return null;
        }

        return layout(symbol.getString(), fat);
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a Shape, using its related String of
     * MusicFont characters, and sized to fit the provided dimension.
     *
     * @param shape     the shape to be drawn with MusicFont chars
     * @param dimension the dim to fit as much as possible
     * @return the adjusted TextLayout ready to be drawn
     */
    public TextLayout layout (Shape shape,
                              Dimension dimension)
    {
        ShapeSymbol symbol = Symbols.getSymbol(shape);

        if (symbol != null) {
            return layout(symbol, dimension);
        } else {
            return null;
        }
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a symbol, using its related String of
     * MusicFont characters, and sized to fit the provided dimension.
     *
     * @param symbol    the symbol to be drawn with MusicFont chars
     * @param dimension the dim to fit as much as possible
     * @return the adjusted TextLayout ready to be drawn
     */
    public TextLayout layout (BasicSymbol symbol,
                              Dimension dimension)
    {
        String str = symbol.getString();
        TextLayout layout = new TextLayout(str, this, frc);

        // Compute proper affine transformation
        Rectangle2D rect = layout.getBounds();
        AffineTransform fat = AffineTransform.getScaleInstance(
                dimension.width / rect.getWidth(),
                dimension.height / rect.getHeight());

        return layout(str, fat);
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a Shape, using its related String of
     * MusicFont character codes.
     *
     * @param shape the shape to be drawn with MusicFont chars
     * @return the TextLayout ready to be drawn
     */
    public TextLayout layout (Shape shape)
    {
        return layout(shape, (AffineTransform) null);
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a String of MusicFont character codes.
     *
     * @param codes the MusicFont codes
     * @return the TextLayout ready to be drawn
     */
    public TextLayout layout (int... codes)
    {
        return layout(new String(codes, 0, codes.length));
    }

    //------------------//
    // getFontInterline //
    //------------------//
    /**
     * Report the number of pixels of the interline that corresponds to
     * this font.
     *
     * @return the scaled interline for this font
     */
    protected int getFontInterline ()
    {
        return fontInterline;
    }
}
