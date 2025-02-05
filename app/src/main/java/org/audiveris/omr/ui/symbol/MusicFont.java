//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       M u s i c F o n t                                        //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.TIME_ZERO;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.param.ConstantBasedParam;
import org.audiveris.omr.util.param.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Class <code>MusicFont</code> allows the use of proper music font family and size.
 * <br>
 * Major goals are:
 * <ol>
 * <li>Head template matching
 * <li>Precise rendering of score views and boards
 * <li>Generation of rare training samples
 * </ol>
 * <dl>
 * <dt><b>Font family:</b></dt>
 * <dd>The end-user can select any font family among the ones defined by {@link MusicFamily} enum
 * type.
 * <br>
 * The current default family is the rather exhaustive SMuFL-compliant {@link MusicFamily#Bravura}.
 * <p>
 * Other families are available, notably {@link MusicFamily#FinaleJazz}, which may better fit
 * Jazz-like printed scores.
 * <p>
 * When a symbol is not directly available in a given family, its backup families if any are
 * transitively searched for the shape at hand.
 * </dd>
 * <dt><b>Item sizes:</b></dt>
 * <dd>The strategy is to use properly scaled instances of this class to carry out the drawing of
 * music symbols with the correct size:
 * <ul>
 * <li>A general font size based on current sheet (or even staff) interline value,</li>
 * <li>A specific font size meant for heads rendering, which may differ from pure interline-based
 * scaling.</li>
 * </ul>
 * To get properly scaled instances, use the convenient methods {@link #getBaseFont(Family,int)}
 * and {@link #getHeadFont(Family,Scale,int)}.
 * </dl>
 *
 * @see <a href="https://www.smufl.org/version/">SMuFL web site</a>
 * @author Hervé Bitteur
 */
public class MusicFont
        extends OmrFont
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(MusicFont.class);

    /** Default interline value, consistent with base font. */
    public static final int DEFAULT_INTERLINE = (int) Math.rint(16 * UIUtil.getGlobalFontRatio());

    /** Interline value for shape buttons. */
    public static final int TINY_INTERLINE = (int) Math.rint(DEFAULT_INTERLINE * RATIO_TINY);

    /** Default music font family. */
    public static final Param<MusicFamily> defaultMusicParam = new ConstantBasedParam<>(
            constants.defaultMusicFamily,
            Param.GLOBAL_SCOPE);

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Music family this font belongs to.
     * <p>
     * NOTA: java.awt.Font already has a notion of 'family' in a broader meaning
     * So, we use here the more specific 'musicFamily' field name.
     */
    protected final MusicFamily musicFamily;

    /** Backup font, if any. */
    protected MusicFont backupFont;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>MusicFont</code> object from a provided font.
     *
     * @param font some existing font
     */
    private MusicFont (Font font)
    {
        super(font);
        musicFamily = MusicFamily.valueOfName(font.getFamily().replaceAll(" ", ""));

        if (musicFamily.getBackup() != null) {
            backupFont = MusicFont.getMusicFont(musicFamily.getBackup(), size);
        }
    }

    /**
     * Creates a new <code>MusicFont</code> object, based on the provided family and size.
     *
     * @param family chosen music font family
     * @param size   the point size of the <code>Font</code>
     */
    public MusicFont (MusicFamily family,
                      int size)
    {
        super(family.getFontName(), family.getFileName(), Font.PLAIN, size);
        musicFamily = family;

        if (musicFamily.getBackup() != null) {
            backupFont = MusicFont.getMusicFont(musicFamily.getBackup(), size);
        }
    }

    /**
     * Creates a new <code>MusicFont</code> object, based on the provided FontInfo.
     *
     * @param info the font info
     */
    public MusicFont (FontInfo info)
    {
        this(MusicFamily.valueOfName(info.fontName), info.pointsize);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------------//
    // buildImage //
    //------------//
    /**
     * Build an image from shape definition in font family, using the scaling of this font.
     *
     * @param shape     the desired shape
     * @param decorated true if shape display must use decorations
     * @return the image built with proper scaling, or null
     */
    public BufferedImage buildImage (Shape shape,
                                     boolean decorated)
    {
        final ShapeSymbol symbol = musicFamily.getSymbols().getSymbol(shape, decorated);

        if (symbol == null) {
            return null;
        } else {
            return symbol.buildImage(this);
        }
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
    public Scale.MusicFontScale buildMusicFontScale (double width)
    {
        return new Scale.MusicFontScale(musicFamily.name(), computePointSize(width));
    }

    //------------------//
    // computePointSize // TODO: Could we get rid of this method?
    //------------------//
    /**
     * Using this font family, compute the point size that best matches the provided size
     * of typical NOTEHEAD_BLACK symbol.
     *
     * @param width measured width of black head in score
     * @return the corresponding point size with this font family
     */
    private int computePointSize (double width)
    {
        final Shape shape = Shape.NOTEHEAD_BLACK;

        // Very rough value to start with
        final int v1 = (int) Math.rint(width * 3.3);
        final MusicFont font1 = new MusicFont(musicFamily, v1);
        final TextLayout layout1 = font1.layoutShapeByCode(shape);
        final double w1 = layout1.getBounds().getWidth();

        // Another value
        final int dv = (int) Math.rint(width * 0.25); // Should be different enough
        final int v2 = (w1 < width) ? (v1 + dv) : (v1 - dv);
        final MusicFont font2 = new MusicFont(musicFamily, v2);
        final TextLayout layout2 = font2.layoutShapeByCode(shape);
        final double w2 = layout2.getBounds().getWidth();

        // Interpolation
        final double dw = w2 - w1;
        final int v = (Math.abs(dw) < 0.01) ? v1
                : (int) Math.rint(v1 + ((v2 - v1) * ((width - w1) / (w2 - w1))));

        {
            // Just for verification
            final MusicFont font = new MusicFont(musicFamily, v);
            final TextLayout layout = font.layoutShapeByCode(shape);
            final double w = layout.getBounds().getWidth();
            logger.debug("width:{}", width);
            logger.debug("v1:{},w1:{} v:{},w:{} v2:{},w2:{}", v1, w1, v, w, v2, w2);
        }

        return v;
    }

    //------------//
    // deriveFont //
    //------------//
    @Override
    public MusicFont deriveFont (float size)
    {
        return new MusicFont(super.deriveFont(size));
    }

    //------------//
    // deriveFont //
    //------------//
    @Override
    public MusicFont deriveFont (int style)
    {
        return new MusicFont(super.deriveFont(style));
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

        if (obj instanceof MusicFont that) {
            if (this.musicFamily != that.musicFamily) {
                return false;
            }

            return super.equals(obj);
        }

        return false;
    }

    //-----------//
    // getBackup //
    //-----------//
    /**
     * Report the backup font, if any, for this font.
     *
     * @return the backup font, perhaps null
     */
    public MusicFont getBackup ()
    {
        return backupFont;
    }

    //----------------//
    // getMusicFamily //
    //----------------//
    /**
     * Report the music family of this MusicFont.
     * <p>
     * Not to be mistaken with the inherited {@link Font#getFamily()} method.
     *
     * @return font music family
     */
    public MusicFamily getMusicFamily ()
    {
        return musicFamily;
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
        return (int) Math.rint((size + 2) / 4.0);
    }

    //-----------//
    // getSymbol //
    //-----------//
    /**
     * Return the symbol, if any, defined in this font for the provided shape.
     *
     * @param shape the provided shape
     * @return corresponding symbol in this font, or null
     */
    public ShapeSymbol getSymbol (Shape shape)
    {
        return musicFamily.getSymbols().getSymbol(shape);
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.musicFamily);
        return hash;
    }

    //--------------------//
    // layoutNumberByCode //
    //--------------------//
    /**
     * Report the layout that corresponds to a number (a sequence of numeral digits).
     *
     * @param number the provided number, limited to range [0..999]
     * @return the layout or null
     * @throws IllegalArgumentException if number is beyond range limits
     */
    public TextLayout layoutNumberByCode (int number)
    {
        if (number < 0) {
            throw new IllegalArgumentException(number + " < 0");
        }

        if (number > 999) {
            throw new IllegalArgumentException(number + " > 999");
        }

        MusicFont font = this;
        int[] zeroCode = musicFamily.getSymbols().getCode(TIME_ZERO);

        while (zeroCode == null && font.getBackup() != null) {
            font = font.getBackup();
            zeroCode = font.musicFamily.getSymbols().getCode(TIME_ZERO);
        }

        if (zeroCode == null) {
            return null; // Should not occur, unless [backup] font provides no code for TIME_ZERO
        }

        final int baseCode = zeroCode[0];
        final int[] numberCodes = (number >= 100) ? new int[3]
                : (number >= 10) ? new int[2] : new int[1];
        int index = 0;

        if (number >= 100) {
            numberCodes[index++] = baseCode + (number / 100);
            number = number % 100;
        }

        if (number >= 10) {
            numberCodes[index++] = baseCode + (number / 10);
            number = number % 10;
        }

        numberCodes[index] = baseCode + number;

        return new TextLayout(getString(numberCodes), font, frc);
    }

    //-------------//
    // layoutShape // Used when visiting BRACE in SigPainter
    //-------------//
    /**
     * Build a TextLayout from a Shape, using its corresponding symbol,
     * and sized to fit the provided dimension.
     *
     * @param shape     the shape to be drawn with MusicFont chars
     * @param dimension the dim to fit as much as possible
     * @return the adjusted TextLayout ready to be drawn
     */
    public TextLayout layoutShape (Shape shape,
                                   Dimension dimension)
    {
        final ShapeSymbol symbol = musicFamily.getSymbols().getSymbol(shape);

        if (symbol != null) {
            return layoutSymbol(symbol, dimension);
        } else {
            return null;
        }
    }

    //-------------------//
    // layoutShapeByCode //
    //-------------------//
    /**
     * Build a TextLayout from a Shape, using its related font character codes.
     * <p>
     * NOTA: This method bypasses font shape symbols but uses shape codes from this font
     * (or from its backup font if needed), which may fail.
     *
     * @param shape the shape to be drawn with MusicFont chars
     * @return the TextLayout or null
     */
    public TextLayout layoutShapeByCode (Shape shape)
    {
        return layoutShapeByCode(shape, null);
    }

    //-------------------//
    // layoutShapeByCode //
    //-------------------//
    /**
     * Build a TextLayout from a Shape, using its related font character codes,
     * and potentially sized by an AffineTransform instance.
     * <p>
     * NOTA: This method bypasses font shape symbols but uses shape codes from this font
     * (or from its backup font if needed)
     *
     * @param shape the shape to be drawn with MusicFont chars
     * @param fat   potential affine transformation
     * @return the (sized) TextLayout ready to be drawn or null
     */
    public TextLayout layoutShapeByCode (Shape shape,
                                         AffineTransform fat)
    {
        final String str = getString(musicFamily.getSymbols().getCode(shape));

        if (str != null) {
            return layout(str, fat);
        }

        logger.debug("No {} layout for {}", musicFamily, shape);

        if (backupFont != null) {
            return backupFont.layoutShapeByCode(shape, fat);
        }

        return null;
    }

    //--------------//
    // layoutSymbol //
    //--------------//
    /**
     * Build a TextLayout from a Symbol, sized to fit the provided dimension.
     * <p>
     * This method bypasses font shape symbols but uses font shape codes
     * (or its backup font shape codes if needed)
     *
     * @param symbol    the symbol to be drawn
     * @param dimension the dimension to fit as much as possible
     * @return the adjusted TextLayout ready to be drawn
     */
    public TextLayout layoutSymbol (ShapeSymbol symbol,
                                    Dimension dimension)
    {
        final TextLayout layout = layoutShapeByCode(symbol.getShape());

        if (layout == null) {
            return null;
        }

        // Compute proper affine transformation
        final Rectangle2D rect = layout.getBounds();
        final AffineTransform fat = AffineTransform.getScaleInstance(
                dimension.width / rect.getWidth(),
                dimension.height / rect.getHeight());

        return layoutShapeByCode(symbol.getShape(), fat);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //----------------//
    // checkMusicFont //
    //----------------//
    /**
     * Check whether we have been able to load MusicFont data.
     *
     * @return true if OK
     */
    public static boolean checkMusicFont ()
    {
        populateAllSymbols();

        //        if (baseMusicFont.getFamily().equals("Dialog")) {
        //            String msg = FONT_NAME + " font not found." + " Please install " + FONT_NAME;
        //            logger.error(msg);
        //
        //            if (OMR.gui != null) {
        //                OMR.gui.displayError(msg);
        //            }
        //
        //            return false;
        //        }
        //
        return true;
    }

    //-------------//
    // getBaseFont //
    //-------------//
    /**
     * Report the (perhaps cached) music font based on chosen family and staff interline.
     *
     * @param family         chosen family font
     * @param staffInterline real interline value for related staves
     * @return proper scaled music font
     */
    public static MusicFont getBaseFont (MusicFamily family,
                                         int staffInterline)
    {
        return getMusicFont(family, getPointSize(staffInterline));
    }

    //-------------------//
    // getBaseFontBySize //
    //-------------------//
    /**
     * Report the (perhaps cached) music font based on chosen family and point size.
     *
     * @param family    chosen family font
     * @param pointSize desired font point size
     * @return proper scaled music font
     */
    public static MusicFont getBaseFontBySize (MusicFamily family,
                                               int pointSize)
    {
        return getMusicFont(family, pointSize);
    }

    //------------------//
    // getCurrentFamily //
    //------------------//
    /**
     * Report the music family used in the sheet currently displayed
     *
     * @return the current sheet music family, null if no sheet is displayed
     */
    public static MusicFamily getCurrentFamily ()
    {
        final SheetStub stub = StubsController.getInstance().getSelectedStub();
        return (stub != null) ? stub.getMusicFamily() : null;
    }

    //-----------------------//
    // getDefaultMusicFamily //
    //-----------------------//
    /**
     * Report the family currently defined as the default music family.
     *
     * @return the default music family
     */
    public static MusicFamily getDefaultMusicFamily ()
    {
        return defaultMusicParam.getValue();
    }

    //-------------//
    // getHeadFont //
    //-------------//
    /**
     * Report the (perhaps cached) music font specifically sized for heads.
     *
     * @param family         chosen family font
     * @param scale          scaling information for the sheet
     * @param staffInterline real interline value for related staves.
     * @return proper scaled music font
     */
    public static MusicFont getHeadFont (MusicFamily family,
                                         Scale scale,
                                         int staffInterline)
    {
        return getMusicFont(family, getHeadPointSize(scale, staffInterline));
    }

    //------------------//
    // getHeadPointSize //
    //------------------//
    /**
     * Compute the proper point size for head rendering, based on global sheet scale
     * and provided staff interline.
     *
     * @param scale          sheet scaling information
     * @param staffInterline interline for related staff
     * @return proper point size for font
     */
    public static int getHeadPointSize (Scale scale,
                                        double staffInterline)
    {
        final Scale.MusicFontScale musicFontScale = scale.getMusicFontScale();

        if (musicFontScale != null) {
            // Interpolate from large staff information
            final double ratio = (double) staffInterline / scale.getInterline();
            return (int) Math.rint(ratio * musicFontScale.getPointSize());
        } else {
            // No precise information available, fall back using head ratio constant...
            logger.debug("MusicFont. Using head ratio constant");
            return getPointSize((int) Math.rint(staffInterline * constants.headRatio.getValue()));
        }
    }

    //--------------//
    // getMusicFont //
    //--------------//
    /**
     * Report the music font for the chosen family and properly scaled by point size.
     *
     * @param family font family
     * @param size   point size value (perhaps different from 4 * staffInterline)
     * @return properly scaled music font
     */
    public static MusicFont getMusicFont (MusicFamily family,
                                          int size)
    {
        Font font = getFont(family.getFontName(), family.getFileName(), Font.PLAIN, size);

        if (!(font instanceof MusicFont)) {
            font = new MusicFont(font);
            cacheFont(font);
        }

        return (MusicFont) font;
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
    // getString //
    //-----------//
    /**
     * Report the String defined by the provided Unicode code points.
     *
     * @param codes Unicode code points
     * @return the resulting String, null if code points are null
     */
    public static String getString (int... codes)
    {
        if (codes == null) {
            return null;
        }

        return new String(codes, 0, codes.length);
    }

    //--------------------//
    // populateAllSymbols //
    //--------------------//
    /**
     * Complete Family and Symbols elaboration.
     */
    public static void populateAllSymbols ()
    {
        for (MusicFamily family : MusicFamily.values()) {
            family.symbols.populateSymbols();
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Enum<MusicFamily> defaultMusicFamily = new Constant.Enum<>(
                MusicFamily.class,
                MusicFamily.Bravura,
                "Default font family for music symbols");

        private final Constant.Ratio headRatio = new Constant.Ratio(
                1.0, // Was 1.1,
                "Ratio applied on font size for better head rendering");
    }
}
