//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       M u s i c F o n t                                        //
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

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.MusicFontScale;

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
 * Class {@code MusicFont} is meant to simplify the use of the underlying music font
 * when rendering score views.
 * <p>
 * The strategy is to use a properly scaled instance of this class to carry out the drawing of music
 * symbols with the correct size. The scaling should address:
 * <ul>
 * <li>An interline value adapted to current sheet (or even staff) scaling,</li>
 * <li>A specific font size meant for heads rendering, which may significantly differ from pure
 * interline-based scaling.</li>
 * </ul>
 * To get properly scaled instances, use the convenient methods {@link #getBaseFont(int)} and
 * {@link #getHeadFont(Scale, int)}.
 * <p>
 * There are two well-known pre-scaled instances of this class:
 * <ul>
 * <li>{@link #baseMusicFont} for standard symbols (scale = 1)</li>
 * <li>{@link #iconMusicFont} for icon symbols (scale = 1/2)</li>
 * </ul>
 * The current underlying font is <b>MusicalSymbols</b>.
 *
 * @see
 * <a href=
 *      "http://fonts.simplythebest.net/font/108/musical_symbols-font.font">http://fonts.simplythebest.net/font/108/musical_symbols-font.font</a>
 * @author Hervé Bitteur
 */
public class MusicFont
        extends OmrFont
{

    private static final Constants constants = new Constants();

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

    /** Cache of font according to scaling value (pointSize, staffInterline). */
    private static final Map<Scaling, MusicFont> scalingMap = new HashMap<>();

    /** Default interline value, consistent with base font: {@value}. */
    public static final int DEFAULT_INTERLINE = 16;

    /** The music font used for default interline. */
    public static final MusicFont baseMusicFont = getPointFont(
            getPointSize(DEFAULT_INTERLINE),
            DEFAULT_INTERLINE);

    /** The music font used just for icons (half-size). */
    public static final MusicFont iconMusicFont = getPointFont(
            getPointSize(DEFAULT_INTERLINE) / 2,
            DEFAULT_INTERLINE / 2);

    /** Interline value of the staves where this font is used. */
    private final int staffInterline;

    /**
     * Creates a new MusicFont object.
     *
     * @param sizePts        the point size of the {@code Font}
     * @param staffInterline the staff interline value where this font is used
     */
    private MusicFont (int sizePts,
                       int staffInterline)
    {
        super(FONT_NAME, Font.PLAIN, sizePts);
        this.staffInterline = staffInterline;
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

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof MusicFont) {
            MusicFont that = (MusicFont) obj;

            if (this.staffInterline != that.staffInterline) {
                return false;
            }

            return super.equals(obj);
        }

        return false;
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (71 * hash) + this.staffInterline;

        return hash;
    }

    //--------//
    // layout //
    //--------//
    /**
     * Build a TextLayout from a Shape, using its related String of MusicFont characters,
     * and potentially sized by an AffineTransform instance.
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

    //-------------------//
    // getStaffInterline //
    //-------------------//
    /**
     * Report the number of pixels of the interline that corresponds to the staves
     * where this font is used.
     *
     * @return the scaled interline of the related staff size.
     */
    protected int getStaffInterline ()
    {
        return staffInterline;
    }

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
        MusicFont font = getBaseFont(interline);

        return font.buildImage(shape, decorated);
    }

    //---------------------//
    // buildMusicFontScale //
    //---------------------//
    /**
     * Build scaling information for music font, based on provided head width.
     *
     * @param width with of black heads
     * @return the music font scaling info
     */
    public static MusicFontScale buildMusicFontScale (double width)
    {
        return new Scale.MusicFontScale(MusicFont.FONT_NAME, computePointSize(width));
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
            String msg = FONT_NAME + " font not found." + " Please install " + FONT_NAME + ".ttf";
            logger.error(msg);

            if (OMR.gui != null) {
                OMR.gui.displayError(msg);
            }

            return false;
        }

        return true;
    }

    //------------------//
    // computePointSize //
    //------------------//
    /**
     * Compute the point size for the FONT_NAME font that matches the provided size
     * of typical symbol (NOTEHEAD_BLACK).
     *
     * @param width actual width of black head
     * @return the corresponding point size with FONT_NAME font
     */
    public static int computePointSize (double width)
    {
        final Shape shape = Shape.NOTEHEAD_BLACK;
        final ShapeSymbol symbol = Symbols.getSymbol(shape);
        final int dv = (int) Math.rint(width * 0.25); // Should be OK

        final int v1 = (int) Math.rint(width * 3.3); // Very rough value to start with
        final MusicFont font1 = new MusicFont(v1, 0);
        final TextLayout layout1 = symbol.layout(font1);
        final double w1 = layout1.getBounds().getWidth();

        final int v2 = (w1 < width) ? (v1 + dv) : (v1 - dv); // Other value for interpolation
        final MusicFont font2 = new MusicFont(v2, 0);
        final TextLayout layout2 = symbol.layout(font2);
        final double w2 = layout2.getBounds().getWidth();

        final double dw = w2 - w1;
        final int v = (Math.abs(dw) < 0.01) ? v1
                : (int) Math.rint(v1 + ((v2 - v1) * ((width - w1) / (w2 - w1))));

        // Verification
        final MusicFont font = new MusicFont(v, 0);
        final TextLayout layout = symbol.layout(font);
        final double w = layout.getBounds().getWidth();
        logger.debug("width:{}", width);
        logger.debug("v1:{},w1:{} v:{},w:{} v2:{},w2:{}", v1, w1, v, w, v2, w2);

        return v;
    }

    //-------------//
    // getBaseFont //
    //-------------//
    /**
     * Report the (cached) music font based on predefined font.
     *
     * @param staffInterline real interline value for related staves
     * @return proper scaled music font
     */
    public static MusicFont getBaseFont (int staffInterline)
    {
        return getPointFont(getPointSize(staffInterline), staffInterline);
    }

    //-------------//
    // getHeadFont //
    //-------------//
    /**
     * Report the (cached) music font meant for heads.
     *
     * @param scale          scaling information for the sheet
     * @param staffInterline real interline value for related staves.
     * @return proper scaled music font
     */
    public static MusicFont getHeadFont (Scale scale,
                                         int staffInterline)
    {
        return getPointFont(getHeadPointSize(scale, staffInterline), staffInterline);
    }

    //------------------//
    // getHeadPointSize //
    //------------------//
    /**
     * Compute the proper point size for head rendering.
     *
     * @param scale          sheet scaling information
     * @param staffInterline interline for related staff
     * @return proper point size for font
     */
    public static int getHeadPointSize (Scale scale,
                                        int staffInterline)
    {
        final Scale.MusicFontScale musicFontScale = scale.getMusicFontScale();
        final Scale smallScale = scale.getSmallScale();
        final boolean isSmallStaff = (smallScale != null) && (smallScale
                .getInterline() == staffInterline);

        if (musicFontScale != null) {
            if (isSmallStaff) {
                MusicFontScale smallFontScale = smallScale.getMusicFontScale();

                if (smallFontScale != null) {
                    // Precise small information
                    return smallFontScale.getPointSize();
                } else {
                    // No precise small information, interpolate from large information
                    final double smallRatio = (double) staffInterline / scale.getInterline();

                    return (int) Math.rint(smallRatio * musicFontScale.getPointSize());
                }
            } else {
                // Precise large information
                return musicFontScale.getPointSize();
            }
        } else {
            // No precise information available, fall back using head ratio...
            return getPointSize((int) Math.rint(staffInterline * getHeadRatio()));
        }
    }

    //--------------//
    // getHeadRatio //
    //--------------//
    /**
     * Report an increase in head size, a trick for better head rendering.
     *
     * @return ratio slightly over 1.0
     */
    public static double getHeadRatio ()
    {
        return constants.headRatio.getValue();
    }

    //--------------//
    // getPointFont //
    //--------------//
    /**
     * Report the music font properly scaled by point size and staffInterline.
     *
     * @param pointSize      precise point size value (perhaps different from 4 * staffInterline)
     * @param staffInterline real interline value for related staves
     * @return proper scaled music font
     */
    public static MusicFont getPointFont (int pointSize,
                                          int staffInterline)
    {
        final Scaling scaling = new Scaling(pointSize, staffInterline);
        MusicFont font = scalingMap.get(scaling);

        if (font == null) {
            scalingMap.put(scaling, font = new MusicFont(pointSize, staffInterline));
        }

        return font;
    }

    //--------------//
    // getPointSize //
    //--------------//
    /**
     * Report the point size that generally corresponds to the provided staff interline,
     * with the exception of note heads that may need a slightly larger point size.
     *
     * @param staffInterline the provided staff interline
     * @return the general point size value
     */
    public static int getPointSize (int staffInterline)
    {
        return 4 * staffInterline;
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio headRatio = new Constant.Ratio(
                1.1,
                "Ratio applied on font size for better head rendering");
    }

    //---------//
    // Scaling //
    //---------//
    private static class Scaling
    {

        public int pointSize;

        public int staffInterline;

        Scaling (int pointSize,
                 int staffInterline)
        {
            this.pointSize = pointSize;
            this.staffInterline = staffInterline;
        }

        @Override
        public boolean equals (Object obj)
        {
            if (!(obj instanceof Scaling)) {
                return false;
            }

            Scaling that = (Scaling) obj;

            return (this.pointSize == that.pointSize)
                           && (this.staffInterline == that.staffInterline);
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;
            hash = (43 * hash) + this.pointSize;
            hash = (43 * hash) + this.staffInterline;

            return hash;
        }

        @Override
        public String toString ()
        {
            return pointSize + "/" + staffInterline;
        }
    }
}
