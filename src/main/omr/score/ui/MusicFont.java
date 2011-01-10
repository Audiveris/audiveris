//----------------------------------------------------------------------------//
//                                                                            //
//                             M u s i c F o n t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.log.Logger;

import omr.score.ui.MusicFont.Alignment.Horizontal;
import omr.score.ui.MusicFont.Alignment.Vertical;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.Map.Entry;

/**
 * Class <code>MusicFont</code> is meant to simplify the use of the underlying
 * music font when rendering picture or score views.
 *
 * <p>The underlying font is either Maestro, SToccata or MusicalSymbols.
 * We define a map between each shape and the corresponding point code
 * (or sequence of point codes) in this font.
 * The chosen font name is defined by the public member {@link #fontName}.</p>
 *
 * <p>For Maestro:
 * See http://www.fontyukle.net/en/</p>
 *
 * <p>for SToccata:
 * Download from http://fonts.goldenweb.it/download2.php?d2=Freeware_fonts&c=s&file2=SToccata.ttf
 * See http://fonts.goldenweb.it/pan_file/l/en/font2/SToccata.ttf/d2/Freeware_fonts/c/s/default.html</p>
 *
 * <p>for MusicalSymbols:
 * Download from http://simplythebest.net/fonts/fonts/musical_symbols.html</p>
 *
 * @author Hervé Bitteur
 */
public class MusicFont
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MusicFont.class);

    /** First possible music font name (non-standard) */
    private static final String maestro = "Maestro";

    /** Second possible music font name (standard) */
    private static final String stoccata = "SToccata";

    /** Third possible music font name (standard) */
    private static final String musical = "MusicalSymbols";

    /** Name of the chosen underlying music font */
    private static final String fontName = constants.musicFontName.getValue();

    /** Convenient boolean for tests */
    public static final boolean useMaestro = fontName.toLowerCase()
                                                     .contains(
        maestro.toLowerCase());

    /** Convenient boolean for tests */
    public static final boolean useStoccata = fontName.toLowerCase()
                                                      .contains(
        stoccata.toLowerCase());

    /** Convenient boolean for tests */
    public static final boolean useMusical = fontName.toLowerCase()
                                                     .contains(
        musical.toLowerCase());

    /** Convenient boolean for tests */
    public static final boolean useStandard = useStoccata || useMusical;

    /** Underlying music font, initialized with some size */
    public static final Font genericFont = new Font(fontName, Font.PLAIN, 100);

    /** Descriptor of '8' char for alta & bassa (for standard)*/
    public static final CharDesc ALTA_BASSA_DESC = new CharDesc(165);

    /** Descriptor for upper part of brace (MusicalSymbols) */
    public static final CharDesc BRACE_UPPER_HALF = new CharDesc(167);

    /** Descriptor for lower part of brace (MusicalSymbols) */
    public static final CharDesc BRACE_LOWER_HALF = new CharDesc(234);

    /** Descriptor for a user mark */
    public static final CharDesc MARK_DESC = new CharDesc(205);

    /** Needed for font size computation */
    private static final FontRenderContext frc = new FontRenderContext(
        null,
        true,
        true);

    /** Cache of map according to desired staff height */
    private static final Map<Integer, Font> sizeMap = new HashMap<Integer, Font>();

    /** Empty array returned when no point codes are defined for a shape */
    private static final int[] NO_CODES = new int[0];

    /** Map Shape -> point codes for music font */
    private static final EnumMap<Shape, CharDesc> descMap = new EnumMap<Shape, CharDesc>(
        Shape.class);

    static {
        map(ACCENT, 62);
        map(ARPEGGIATO, 103);
        //        map(BACK_TO_BACK_REPEAT_SIGN);
        //        map(BEAM);
        //        map(BEAM_2);
        //        map(BEAM_3);
        //        map(BEAM_HOOK);
        //        map(BRACE);
        //        map(BRACKET);
        map(BREATH_MARK, 44);
        map(BREVE, 87);
        map(BREVE_REST, 208);
        //        map(CAESURA);
        //        map(CHARACTER);
        //        map(CLUTTER);
        map(CODA, 222);
        //        map(COMBINING_AUGMENTATION_DOT);
        map(COMBINING_FLAG_1, 106);
        map(COMBINING_FLAG_1_UP, 74);
        map(COMBINING_FLAG_2, useMaestro ? 144 : 107);
        map(COMBINING_FLAG_2_UP, useMaestro ? 146 : 75);
        //        map(COMBINING_FLAG_3);
        //        map(COMBINING_FLAG_3_UP);
        //        map(COMBINING_FLAG_4);
        //        map(COMBINING_FLAG_4_UP);
        //        map(COMBINING_FLAG_5);
        //        map(COMBINING_FLAG_5_UP);
        //        map(COMBINING_STEM);
        map(COMMON_TIME, 99);
        //        map(CRESCENDO);
        //        map(CUSTOM_TIME_SIGNATURE);
        map(CUT_TIME, 67);
        map(C_CLEF, 66);

        if (useStandard) {
            map(DAL_SEGNO, 100);
            map(DA_CAPO, 68);
        }

        //        map(DECRESCENDO);
        map(DOT, 46);
        //        map(DOUBLE_BARLINE);
        map(DOUBLE_FLAT, 186);
        map(DOUBLE_SHARP, 220);
        map(DYNAMICS_CHAR_M, 189);
        map(DYNAMICS_CHAR_R, useMaestro ? 142 : 243);
        map(DYNAMICS_CHAR_S, 115);
        map(DYNAMICS_CHAR_Z, 122);
        map(DYNAMICS_F, 102);
        map(DYNAMICS_FF, 196);
        map(DYNAMICS_FFF, 236);
        map(DYNAMICS_FFFF, 236, 102);
        map(DYNAMICS_FFFFF, 236, 196);
        map(DYNAMICS_FFFFFF, 236, 236);

        if (useMaestro) {
            map(DYNAMICS_FP, 234);
        } else {
            map(DYNAMICS_FP, 102, 112);
        }

        map(DYNAMICS_FZ, 90);
        map(DYNAMICS_MF, 70);
        map(DYNAMICS_MP, 80);
        map(DYNAMICS_P, 112);
        map(DYNAMICS_PP, 185);
        map(DYNAMICS_PPP, 184);
        map(DYNAMICS_PPPP, 184, 112);
        map(DYNAMICS_PPPPP, 184, 185);
        map(DYNAMICS_PPPPPP, 184, 184);
        map(DYNAMICS_RF, 243, 102);
        map(DYNAMICS_RFZ, 243, 102, 122);
        map(DYNAMICS_SF, 83);
        map(DYNAMICS_SFFZ, 83, 90);
        map(DYNAMICS_SFP, 83, 112);
        map(DYNAMICS_SFPP, 83, 185);
        map(DYNAMICS_SFZ, 83, 122);
        map(EIGHTH_REST, 228);
        //        map(ENDING);
        //        map(ENDING_HORIZONTAL);
        //        map(ENDING_VERTICAL);
        map(FERMATA, 85);
        map(FERMATA_BELOW, 117);

        if (useStandard) {
            map(FINAL_BARLINE, 211);
        }

        map(FLAT, 98);
        //        map(FORWARD);
        map(F_CLEF, 63); // Maestro: F line is on baseline
        map(F_CLEF_OTTAVA_ALTA, useMaestro ? 230 : 63);
        map(F_CLEF_OTTAVA_BASSA, useMaestro ? 116 : 63);
        //        map(GLYPH_PART);
        map(GRACE_NOTE_NO_SLASH, 59);
        map(GRACE_NOTE_SLASH, 201);
        map(G_CLEF, 38);
        map(G_CLEF_OTTAVA_ALTA, useMaestro ? 160 : 38);
        map(G_CLEF_OTTAVA_BASSA, useMaestro ? 86 : 38);
        //        map(HALF_REST);
        //        map(HEAD_AND_FLAG_1);
        //        map(HEAD_AND_FLAG_1_UP);
        //        map(HEAD_AND_FLAG_2);
        //        map(HEAD_AND_FLAG_2_UP);
        //        map(HEAD_AND_FLAG_3);
        //        map(HEAD_AND_FLAG_3_UP);
        //        map(HEAD_AND_FLAG_4);
        //        map(HEAD_AND_FLAG_4_UP);
        //        map(HEAD_AND_FLAG_5);
        //        map(HEAD_AND_FLAG_5_UP);
        map(INVERTED_MORDENT, 77);

        if (useStoccata) {
            map(INVERTED_TURN, 249); // Specific to SToccata
        }

        //        map(KEY_FLAT_1);
        //        map(KEY_FLAT_2);
        //        map(KEY_FLAT_3);
        //        map(KEY_FLAT_4);
        //        map(KEY_FLAT_5);
        //        map(KEY_FLAT_6);
        //        map(KEY_FLAT_7);
        //        map(KEY_SHARP_1);
        //        map(KEY_SHARP_2);
        //        map(KEY_SHARP_3);
        //        map(KEY_SHARP_4);
        //        map(KEY_SHARP_5);
        //        map(KEY_SHARP_6);
        //        map(KEY_SHARP_7);
        //        map(LEDGER);
        if (useStandard) {
            map(LEFT_REPEAT_SIGN, 93);
        }

        map(LONG_REST, 227);
        map(MORDENT, 109);
        map(NATURAL, 110);
        //        map(NOISE);
        //        map(NON_DRAGGABLE);
        map(NOTEHEAD_BLACK, 207);
        //        map(NOTEHEAD_BLACK_2);
        //        map(NOTEHEAD_BLACK_3);
        //        map(NO_LEGAL_TIME);
        map(OLD_QUARTER_REST, 228); // To be flipped horizontally

        if (useMaestro || useMusical) {
            map(ONE_HUNDRED_TWENTY_EIGHTH_REST, 229);
        }

        map(OTTAVA_ALTA, 195);
        map(OTTAVA_BASSA, 215);
        //        map(PART_DEFINING_BARLINE);
        map(PEDAL_MARK, 161);
        map(PEDAL_UP_MARK, 42);
        map(PERCUSSION_CLEF, useMaestro ? 139 : 47);
        map(QUARTER_REST, 206);

        if (useStandard) {
            map(REPEAT_DOTS, 123);
        }

        if (useStandard) {
            map(REVERSE_FINAL_BARLINE, 210);
            map(RIGHT_REPEAT_SIGN, 125);
        }

        map(SEGNO, 37);
        map(SHARP, 35);
        map(SIXTEENTH_REST, 197);
        map(SIXTY_FOURTH_REST, 244);
        //        map(SLUR);
        map(STACCATISSIMO, useMaestro ? 171 : (useStoccata ? 137 : 174));
        map(STACCATO, 46);
        //        map(STAFF_LINE);
        map(STRONG_ACCENT, 94);
        //        map(STRUCTURE);
        map(TENUTO, 45);

        //        map(TEXT);
        if (useStandard) {
            map(THICK_BARLINE, 91);
        }

        map(THIN_BARLINE, useMaestro ? 92 : 108);
        map(THIRTY_SECOND_REST, 168);
        map(TIME_EIGHT, 56);
        map(TIME_FIVE, 53);
        map(TIME_FOUR, 52);
        //        map(TIME_FOUR_FOUR);
        map(TIME_NINE, 57);
        map(TIME_ONE, 49);
        map(TIME_SEVEN, 55);
        map(TIME_SIX, 54);
        map(TIME_SIXTEEN, 49, 54);
        //        map(TIME_SIX_EIGHT);
        map(TIME_THREE, 51);
        //        map(TIME_THREE_FOUR);
        map(TIME_TWELVE, 49, 50);
        map(TIME_TWO, 50);
        //        map(TIME_TWO_FOUR);
        //        map(TIME_TWO_TWO);
        map(TIME_ZERO, 48);
        map(TR, useMaestro ? 217 : 96);
        map(TUPLET_SIX, 164);
        map(TUPLET_THREE, 163);
        map(TURN, 84);
        //        map(TURN_SLASH);
        //        map(TURN_UP);
        map(VOID_NOTEHEAD, 250);
        //        map(VOID_NOTEHEAD_2);
        //        map(VOID_NOTEHEAD_3);
        map(WHOLE_NOTE, 119);
        //        map(WHOLE_NOTE_2);
        //        map(WHOLE_NOTE_3);
        map(WHOLE_OR_HALF_REST, 238);

        //        map(WHOLE_REST);
    }

    static {
        // Debug: print out a table of (first) code and offsets
        if (false) {
            for (Entry<Shape, CharDesc> entry : descMap.entrySet()) {
                CharDesc    desc = entry.getValue();
                String      str = new String(desc.codes, 0, desc.codes.length);
                GlyphVector glyphVector = genericFont.createGlyphVector(
                    frc,
                    str);
                Rectangle2D rect = glyphVector.getVisualBounds();
                System.out.format(
                    "%-20s => %3d %s\n",
                    entry.getKey(),
                    desc.codes[0] - 0xf000,
                    rect.toString());
            }
        }

        if (false) {
            int[] code = new int[1];

            for (int i = 0xf000; i <= 0xf0ff; i++) {
                code[0] = i;

                String      str = new String(code, 0, 1);
                GlyphVector glyphVector = genericFont.createGlyphVector(
                    frc,
                    str);
                Rectangle2D rect = glyphVector.getVisualBounds();
                System.out.format("%3d %s\n", i - 0xf000, rect.toString());
            }
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getCharDesc //
    //-------------//
    public static CharDesc getCharDesc (Shape shape)
    {
        return descMap.get(shape);
    }

    //----------//
    // getCodes //
    //----------//
    /**
     * Report the sequence of pointCodes to use to draw the provided shape using
     * the music font
     * @return the array of codes, which may be empty but not null
     */
    public static int[] getCodes (Shape shape)
    {
        CharDesc desc = getCharDesc(shape);

        if (desc == null) {
            return NO_CODES;
        } else {
            return desc.codes;
        }
    }

    //---------//
    // getFont //
    //---------//
    /**
     * Report the (cached) best font according to the desired staff height
     * @param staffHeight the desired staff height in pixels
     * @return the font with proper size
     */
    public static Font getFont (int staffHeight)
    {
        Font font = sizeMap.get(staffHeight);

        if (font == null) {
            font = determineSizedMusicFont(staffHeight);
            sizeMap.put(staffHeight, font);
        }

        return font;
    }

    //----------------//
    // checkMusicFont //
    //----------------//
    public static boolean checkMusicFont ()
    {
        // Check we have been able to load the font
        if (genericFont.getFamily()
                       .equals("Dialog")) {
            String msg = "*** " + fontName + " font not found." +
                         " Please install " + fontName + ".ttf ***";
            logger.severe(msg);

            if (Main.getGui() != null) {
                Main.getGui()
                    .displayError(msg);
            }

            return false;
        }

        return true;
    }

    //----------//
    // toOrigin //
    //----------//
    /**
     * Report the translation to go from the point specified by the alignment
     * parameter to the drawing origin of the character bounds
     * @param bounds the character drawing bounds
     * @param alignment the horizontal and vertical alignments used by the
     * calling program
     * @return the translation vector which goes from the program point to
     * the corresponding drawing origin
     */
    public static Point2D toOrigin (Rectangle2D bounds,
                                    Alignment   alignment)
    {
        return new Point2D.Double(
            dxToOrigin(alignment.horizontal, bounds),
            dyToOrigin(alignment.vertical, bounds));
    }

    //-------------------------//
    // determineSizedMusicFont //
    //-------------------------//
    /**
     * Determine the music font with best size. This is based on the
     * C_CLEF symbol which must match the staff height as exactly as possible.
     * @param scale the global sheet scale
     * @return the font ready to use
     */
    private static Font determineSizedMusicFont (int staffHeight)
    {
        int[]       codes = getCodes(C_CLEF);
        String      str = new String(codes, 0, codes.length);
        GlyphVector glyphVector = genericFont.createGlyphVector(frc, str);
        Rectangle2D rect = glyphVector.getVisualBounds();
        Font        font = genericFont.deriveFont(
            (genericFont.getSize2D() * staffHeight) / (float) rect.getHeight());

        if (logger.isFineEnabled()) {
            logger.fine(
                "staffHeight:" + staffHeight + " size:" + font.getSize2D() +
                " rect:" + font.createGlyphVector(frc, str).getVisualBounds());
        }

        return font;
    }

    //------------//
    // dxToOrigin //
    //------------//
    private static double dxToOrigin (Horizontal  horizontal,
                                      Rectangle2D rect)
    {
        switch (horizontal) {
        case LEFT :
            return -rect.getX();

        case CENTER :
            return -rect.getX() - (rect.getWidth() / 2);

        case RIGHT :
            return -rect.getX() - rect.getWidth();
        }

        return 0; // For the compiler ...
    }

    //------------//
    // dyToOrigin //
    //------------//
    private static double dyToOrigin (Vertical    vertical,
                                      Rectangle2D rect)
    {
        switch (vertical) {
        case TOP :
            return -rect.getY();

        case MIDDLE :
            return -rect.getY() - (rect.getHeight() / 2);

        case BOTTOM :
            return -rect.getY() - rect.getHeight();

        case BASELINE :
            return 0;
        }

        return 0; // For the compiler ...
    }

    //-----//
    // map //
    //-----//
    /**
     *
     * @param shape the shape symbol
     * @param vAlign where the (vertical) reference point is WRT the shape
     * @param codes the sequence of codes (perhaps specific to a font)
     */
    private static void map (Shape  shape,
                             int... codes)
    {
        if ((codes != null) && (codes.length > 0)) {
            descMap.put(shape, new CharDesc(codes));
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Alignment //
    //-----------//
    /**
     * Class {@code Alignment} defines how a given symbol must be drawn
     * (horizontally and vertically) with respect to a point of reference.
     */
    public static class Alignment
    {
        //~ Enumerations -------------------------------------------------------

        /** Where is the reference x line for this font character */
        public static enum Horizontal {
            //~ Enumeration constant initializers ------------------------------

            LEFT,CENTER, RIGHT;
        }

        /** Where is the reference y line for this font character */
        public static enum Vertical {
            //~ Enumeration constant initializers ------------------------------

            TOP,MIDDLE, BOTTOM,
            BASELINE;
        }

        //~ Instance fields ----------------------------------------------------

        /** The horizontal alignment */
        public final Horizontal horizontal;

        /** The vertical alignment */
        public final Vertical vertical;

        //~ Constructors -------------------------------------------------------

        public Alignment (Horizontal horizontal,
                          Vertical   vertical)
        {
            this.horizontal = horizontal;
            this.vertical = vertical;
        }
    }

    //----------//
    // CharDesc //
    //----------//
    /**
     * Class {@code CharDesc} gathers the parameters of a given character
     * within the music font
     */
    public static class CharDesc
    {
        //~ Instance fields ----------------------------------------------------

        /** Sequence of point codes */
        public final int[] codes;

        //~ Constructors -------------------------------------------------------

        public CharDesc (int... codes)
        {
            this.codes = shiftedCodesOf(codes);
        }

        //~ Methods ------------------------------------------------------------

        public String getString ()
        {
            return new String(codes, 0, codes.length);
        }

        private int[] shiftedCodesOf (int... codes)
        {
            int[] values = new int[codes.length];

            for (int i = 0; i < codes.length; i++) {
                values[i] = codes[i] + 0xf000;
            }

            return values;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.String musicFontName = new Constant.String(
            "Maestro",
            "Name of font for music symbols (Maestro/SToccata/MusicalSymbols)");
    }
}
