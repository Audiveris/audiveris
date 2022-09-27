//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S y m b o l s                                          //
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

import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;

import java.awt.geom.AffineTransform;
import java.util.EnumMap;

/**
 * Class <code>Symbols</code> manages all {@link ShapeSymbol} instances, both for the simple
 * symbols and for a few decorated symbols.
 * <p>
 * This class is highly dependent on the chosen music font.
 * In fact, there should be <b>NO</b> explicit font code value outside this class.
 * And it is now assumed that we use a SMuFL-compliant font.
 * <p>
 * <img alt="Symbols diagram" src="doc-files/Symbols.png">
 *
 * @author Hervé Bitteur
 */
public abstract class Symbols
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Code for NOTEHEAD_CROSS */
    public static final int CODE_NOTEHEAD_CROSS = 0xE0A9;

    /** Code for NOTEHEAD_CROSS_VOID */
    public static final int CODE_NOTEHEAD_CROSS_VOID = 0xE0A8;

    /** Code for NOTEHEAD_DIAMOND_FILLED */
    public static final int CODE_NOTEHEAD_DIAMOND_FILLED = 0xE0DB;

    /** Code for NOTEHEAD_DIAMOND_VOID */
    public static final int CODE_NOTEHEAD_DIAMOND_VOID = 0xE0D9;

    /** Code for NOTEHEAD_TRIANGLE_DOWN_FILLED */
    public static final int CODE_NOTEHEAD_TRIANGLE_DOWN_FILLED = 0xE0C7;

    /** Code for NOTEHEAD_TRIANGLE_DOWN_VOID */
    public static final int CODE_NOTEHEAD_TRIANGLE_DOWN_VOID = 0xE0C5;

    /** Code for NOTEHEAD_BLACK */
    public static final int CODE_NOTEHEAD_BLACK = 0xE0A4;

    /** Code for NOTEHEAD_VOID */
    public static final int CODE_NOTEHEAD_VOID = 0xE0A3;

    /** Code for WHOLE_NOTE */
    public static final int CODE_WHOLE_NOTE = 0xE0A2;

    /** Code for BREVE */
    public static final int CODE_BREVE = 0xE0A0;

    /** Code for WHOLE_NOTE_CROSS */
    public static final int CODE_WHOLE_NOTE_CROSS = 0xE0A7;

    /** Code for WHOLE_NOTE_DIAMOND */
    public static final int CODE_WHOLE_NOTE_DIAMOND = 0xE0D8;

    /** Code for WHOLE_NOTE_TRIANGLE_DOWN */
    public static final int CODE_WHOLE_NOTE_TRIANGLE_DOWN = 0xE0C4;

    /** Code for AUGMENTATION_DOT */
    public static final int CODE_AUGMENTATION_DOT = 0xE044;

    /** Code for FLAT */
    public static final int CODE_FLAT = 0xE260;

    /** Code for NATURAL */
    public static final int CODE_NATURAL = 0xE261;

    /** Code for SHARP */
    public static final int CODE_SHARP = 0xE262;

    /** Code for THIN_BARLINE */
    public static final int CODE_THIN_BARLINE = 0xE030;

    /** Symbol of small '8' char for ottava sign (alta or bassa) on F and G clefs */
    public static final BasicSymbol SYMBOL_CLEF_OTTAVA = new BasicSymbol(0xE07D);

    /** Symbol for upper serif of bracket */
    public static final BasicSymbol SYMBOL_BRACKET_UPPER_SERIF = new BasicSymbol(0xE003);

    /** Symbol for lower serif of bracket */
    public static final BasicSymbol SYMBOL_BRACKET_LOWER_SERIF = new BasicSymbol(0xE004);

    /** Symbol for staff lines */
    public static final BasicSymbol SYMBOL_STAFF_LINES = new BasicSymbol(0xE01A);

    /** Symbol for a complete quarter (head + stem) */
    public static final BasicSymbol SYMBOL_QUARTER = new BasicSymbol(0xE1D5);

    /** Symbol for stem */
    public static final StemSymbol SYMBOL_STEM = new StemSymbol(0xE210);

    /** Map of plain symbol per shape. */
    private static final EnumMap<Shape, ShapeSymbol> sym = new EnumMap<>(Shape.class);

    static {
        // Populate 'sym' map
        assignSymbols();
    }

    //~ Constructors -------------------------------------------------------------------------------
    /** This is just a functional class, no instance is needed. */
    private Symbols ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // getSymbol //
    //-----------//
    /**
     * Return a symbol, perhaps decorated, from its definition in MusicFont.
     *
     * @param shape     the symbol related shape
     * @param decorated true for a decorated symbol, false for a plain one
     * @return the desired symbol, or null if none found
     */
    public static ShapeSymbol getSymbol (Shape shape,
                                         boolean decorated)
    {
        final ShapeSymbol symbol = getSymbol(shape);

        if (symbol == null) {
            return null;
        }

        return symbol.getDecoratedVersion();
    }

    //-----------//
    // getSymbol //
    //-----------//
    /**
     * Return a plain symbol from its definition in MusicFont.
     *
     * @param shape the symbol shape
     * @return the desired symbol, or null if none
     */
    public static ShapeSymbol getSymbol (Shape shape)
    {
        return sym.get(shape);
    }

    //---------------//
    // assignSymbols //
    //---------------//
    /**
     * Populate the 'sym' mapShape: Shape -> (plain) Symbol.
     * <p>
     * All symbols in this map are draggable by definition.
     */
    private static void assignSymbols ()
    {
        // Instances to be defined first, because others depend on them
        //------------------------------
        mapShape(DOT_set, CODE_AUGMENTATION_DOT);
        mapShape(NOTEHEAD_BLACK, CODE_NOTEHEAD_BLACK);
        mapShape(REPEAT_DOT_PAIR, 0xE043);
        mapShape(RIGHT_REPEAT_SIGN, 0xE041);
        mapShape(THICK_BARLINE, 0xE034);
        mapShape(THIN_BARLINE, CODE_THIN_BARLINE);
        mapShape(TIME_ZERO, 0xE080);

        // Other instances, in alphabetical shape order for easier browsing
        //----------------
        sym.put(ACCENT, new ArticulationSymbol(ACCENT, 0xE4A0));
        mapShape(ARPEGGIATO, 0xE63C);
        sym.put(AUGMENTATION_DOT, new AugmentationSymbol());

        mapShape(BACK_TO_BACK_REPEAT_SIGN, 0xE042);
        sym.put(BEAM, new BeamSymbol());
        sym.put(BEAM_HOOK, new BeamHookSymbol());
        sym.put(BRACE, new BraceSymbol(0xE000));
        mapShape(BRACKET, 0xE002);
        mapShape(BREATH_MARK, 0xE4CE);
        mapShape(BREVE, CODE_BREVE); // a.k.a. DoubleWhole
        sym.put(BREVE_REST, new RestSymbol(BREVE_REST, 0xE4E2));

        mapShape(CAESURA, 0xE4D1);
        sym.put(CLUTTER, new TextSymbol(CLUTTER, "?"));
        mapShape(COMMON_TIME, 0xE08A);
        mapShape(CUT_TIME, 0xE08B);
        mapShape(C_CLEF, 0xE05C);
        mapShape(CODA, 0xE048);
        mapShape(CRESCENDO, 0xE53E);

        mapShape(DAL_SEGNO, 0xE045);
        mapShape(DA_CAPO, 0xE046);
        mapText(DIGIT_0, "0");
        mapText(DIGIT_1, "1");
        mapText(DIGIT_2, "2");
        mapText(DIGIT_3, "3");
        mapText(DIGIT_4, "4");
        mapText(DIGIT_5, "5");
        //        mapText(DIGIT_6, "6");
        //        mapText(DIGIT_7, "7");
        //        mapText(DIGIT_8, "8");
        //        mapText(DIGIT_9, "9");
        mapShape(DIMINUENDO, 0xE53F);
        mapShape(DOUBLE_BARLINE, 0xE031);
        sym.put(DOUBLE_FLAT, new FlatSymbol(DOUBLE_FLAT, 0xE264));
        mapShape(DOUBLE_SHARP, 0xE263);
        //        mapShape(DYNAMICS_CHAR_M, 0xE521);
        //        mapShape(DYNAMICS_CHAR_R, 0xE523);
        //        mapShape(DYNAMICS_CHAR_S, 0xE524);
        //        mapShape(DYNAMICS_CHAR_Z, 0xE525);
        //        mapShape(DYNAMICS_FFFF, 0xE531);
        mapShape(DYNAMICS_F, 0xE522);
        mapShape(DYNAMICS_FF, 0xE52F);
        //        mapShape(DYNAMICS_FFF, 0xE530);
        //        mapShape(DYNAMICS_FZ, 0xE535);
        //        mapShape(DYNAMICS_FFFFF, 0xE532);
        //        mapShape(DYNAMICS_FFFFFF, 0xE533);
        mapShape(DYNAMICS_FP, 0xE534);
        mapShape(DYNAMICS_MF, 0xE52D);
        mapShape(DYNAMICS_MP, 0xE52C);
        mapShape(DYNAMICS_P, 0xE520);
        mapShape(DYNAMICS_PP, 0xE52B);
        //        mapShape(DYNAMICS_PPP, 0xE52A);
        //        mapShape(DYNAMICS_PPPP, 0xE529);
        //        mapShape(DYNAMICS_PPPPP, 0xE528);
        //        mapShape(DYNAMICS_PPPPPP, 0xE527);
        //        mapShape(DYNAMICS_RF, 0xE53C);
        //        mapShape(DYNAMICS_RFZ, 0xE53D);
        mapShape(DYNAMICS_SF, 0xE536);
        //        mapShape(DYNAMICS_SFFZ, 0xE53B);
        //        mapShape(DYNAMICS_SFP, 0xE537);
        mapShape(DYNAMICS_SFZ, 0xE539);
        //        mapShape(DYNAMICS_SFPP, 0xE538);
        sym.put(ENDING, new EndingSymbol(false));
        sym.put(ENDING_WRL, new EndingSymbol(true));
        mapShape(EIGHTH_REST, 0xE4E6);

        mapShape(FERMATA, 0xE4C0);
        sym.put(FERMATA_ARC, new FermataArcSymbol(FERMATA_ARC, 0xE4C0));
        sym.put(FERMATA_ARC_BELOW, new FermataArcSymbol(FERMATA_ARC_BELOW, 0xE4C1));
        mapShape(FERMATA_BELOW, 0xE4C1);
        mapShape(FERMATA_DOT, 0xE044);
        mapShape(FINAL_BARLINE, 0xE032);
        mapShape(FLAG_1, 0xE240);
        mapShape(FLAG_1_UP, 0xE241);
        mapShape(FLAG_2, 0xE242);
        mapShape(FLAG_2_UP, 0xE243);
        mapShape(FLAG_3, 0xE244);
        mapShape(FLAG_3_UP, 0xE245);
        mapShape(FLAG_4, 0xE246);
        mapShape(FLAG_4_UP, 0xE247);
        mapShape(FLAG_5, 0xE248);
        mapShape(FLAG_5_UP, 0xE249);
        sym.put(FLAT, new FlatSymbol(FLAT, CODE_FLAT));
        mapShape(F_CLEF, 0xE062);
        mapShape(F_CLEF_SMALL, 0xE07C);
        mapShape(F_CLEF_8VA, 0xE065);
        mapShape(F_CLEF_8VB, 0xE064);

        mapShape(GRACE_NOTE, 0xE562);
        mapShape(GRACE_NOTE_SLASH, 0xE560);
        mapShape(G_CLEF, 0xE050);
        mapShape(G_CLEF_SMALL, 0xE07A);
        mapShape(G_CLEF_8VA, 0xE053);
        mapShape(G_CLEF_8VB, 0xE052);

        sym.put(HALF_NOTE_DOWN, new CompoundNoteSymbol(HALF_NOTE_DOWN, 0xE1D4));
        sym.put(HALF_NOTE_UP, new CompoundNoteSymbol(HALF_NOTE_UP, 0xE1D3));
        sym.put(HALF_REST, new RestSymbol(HALF_REST, 0xE4E4));
        sym.put(HW_REST_set, new RestSymbol(HW_REST_set, 0xE4E4));

        flatKey(KEY_FLAT_7, -7);
        flatKey(KEY_FLAT_6, -6);
        flatKey(KEY_FLAT_5, -5);
        flatKey(KEY_FLAT_4, -4);
        flatKey(KEY_FLAT_3, -3);
        flatKey(KEY_FLAT_2, -2);
        flatKey(KEY_FLAT_1, -1);
        sym.put(KEY_CANCEL, new KeyCancelSymbol());
        sharpKey(KEY_SHARP_1, 1);
        sharpKey(KEY_SHARP_2, 2);
        sharpKey(KEY_SHARP_3, 3);
        sharpKey(KEY_SHARP_4, 4);
        sharpKey(KEY_SHARP_5, 5);
        sharpKey(KEY_SHARP_6, 6);
        sharpKey(KEY_SHARP_7, 7);

        sym.put(LEDGER, new LedgerSymbol(0xE022));
        mapShape(LEFT_REPEAT_SIGN, 0xE040);
        mapText(LYRICS, "lyric");
        sym.put(LONG_REST, new RestSymbol(LONG_REST, 0xE4E1));

        mapShape(MORDENT, 0xE56C);
        mapShape(MORDENT_INVERTED, 0xE56D);

        mapShape(NATURAL, CODE_NATURAL);
        sym.put(NON_DRAGGABLE, new NonDraggableSymbol(0xEA94, 0xEA93));
        small(NOTEHEAD_BLACK_SMALL, CODE_NOTEHEAD_BLACK);
        mapShape(NOTEHEAD_CROSS, CODE_NOTEHEAD_CROSS);
        mapShape(NOTEHEAD_CROSS_VOID, CODE_NOTEHEAD_CROSS_VOID);
        mapShape(NOTEHEAD_DIAMOND_FILLED, CODE_NOTEHEAD_DIAMOND_FILLED);
        mapShape(NOTEHEAD_DIAMOND_VOID, CODE_NOTEHEAD_DIAMOND_VOID);
        mapShape(NOTEHEAD_TRIANGLE_DOWN_FILLED, CODE_NOTEHEAD_TRIANGLE_DOWN_FILLED);
        mapShape(NOTEHEAD_TRIANGLE_DOWN_VOID, CODE_NOTEHEAD_TRIANGLE_DOWN_VOID);
        mapShape(NOTEHEAD_VOID, CODE_NOTEHEAD_VOID);
        small(NOTEHEAD_VOID_SMALL, CODE_NOTEHEAD_VOID);

        //        mapShape(OLD_QUARTER_REST, 0xE4F2);
        mapShape(ONE_16TH_REST, 0xE4E7);
        mapShape(ONE_64TH_REST, 0xE4E9);
        mapShape(ONE_32ND_REST, 0xE4E8);
        mapShape(ONE_128TH_REST, 0xE4EA);
        sym.put(OTTAVA, new OctaveShiftSymbol(OTTAVA, 0xE510));

        mapShape(PEDAL_MARK, 0xE650);
        mapShape(PEDAL_UP_MARK, 0xE655);
        mapShape(PERCUSSION_CLEF, 0xE069);
        mapText(PLUCK_P, "p");
        mapText(PLUCK_I, "i");
        mapText(PLUCK_M, "m");
        mapText(PLUCK_A, "a");

        sym.put(QUARTER_NOTE_DOWN, new CompoundNoteSymbol(QUARTER_NOTE_DOWN, 0xE1D6));
        sym.put(QUARTER_NOTE_UP, new CompoundNoteSymbol(QUARTER_NOTE_UP, 0xE1D5));
        mapShape(QUARTER_REST, 0xE4E5);
        sym.put(QUINDICESIMA, new OctaveShiftSymbol(QUINDICESIMA, 0xE514));

        sym.put(REPEAT_DOT, new RepeatDotSymbol(0xE044));
        mapShape(REPEAT_ONE_BAR, 0xE500);
        sym.put(REPEAT_TWO_BARS, new RepeatBarSymbol(REPEAT_TWO_BARS, 0xE501));
        sym.put(REPEAT_FOUR_BARS, new RepeatBarSymbol(REPEAT_FOUR_BARS, 0xE502));
        mapShape(REVERSE_FINAL_BARLINE, 0xE033);
        mapText(ROMAN_I, "I");
        mapText(ROMAN_II, "II");
        mapText(ROMAN_III, "III");
        mapText(ROMAN_IV, "IV");
        mapText(ROMAN_V, "V");
        mapText(ROMAN_VI, "VI");
        mapText(ROMAN_VII, "VII");
        mapText(ROMAN_VIII, "VIII");
        mapText(ROMAN_IX, "IX");
        mapText(ROMAN_X, "X");
        mapText(ROMAN_XI, "XI");
        mapText(ROMAN_XII, "XII");

        mapShape(SEGNO, 0xE047);
        mapShape(SHARP, CODE_SHARP);
        sym.put(SLUR_ABOVE, new SlurSymbol(true));
        sym.put(SLUR_BELOW, new SlurSymbol(false));
        small(SMALL_FLAG, 0xE240);
        sym.put(SMALL_FLAG_SLASH, new SlashedFlagSymbol());
        sym.put(STACCATISSIMO, new ArticulationSymbol(STACCATISSIMO, 0xE4A6));
        sym.put(STACCATO, new ArticulationSymbol(STACCATO, 0xE4A2));
        sym.put(STEM, SYMBOL_STEM);
        sym.put(STRONG_ACCENT, new ArticulationSymbol(STRONG_ACCENT, 0xE4AC));

        sym.put(TENUTO, new ArticulationSymbol(TENUTO, 0xE4A4));
        mapText(TEXT, "text");

        mapShape(TIME_EIGHT, 0xE088);
        mapShape(TIME_FIVE, 0xE085);
        mapShape(TIME_FOUR, 0xE084);
        mapShape(TIME_NINE, 0xE089);
        mapShape(TIME_ONE, 0xE081);
        mapShape(TIME_SEVEN, 0xE087);
        mapShape(TIME_SIX, 0xE086);
        mapShape(TIME_SIXTEEN, 0xE081, 0xE086);
        mapShape(TIME_THREE, 0xE083);
        mapShape(TIME_TWELVE, 0xE081, 0xE082);
        mapShape(TIME_TWO, 0xE082);

        // Predefined time signatures as: Numerator / Denominator
        numDen(TIME_CUSTOM, 0, 0);
        numDen(TIME_FIVE_FOUR, 5, 4);
        numDen(TIME_FOUR_FOUR, 4, 4);
        numDen(TIME_SIX_EIGHT, 6, 8);
        numDen(TIME_SIX_FOUR, 6, 4);
        numDen(TIME_THREE_EIGHT, 3, 8);
        numDen(TIME_THREE_FOUR, 3, 4);
        numDen(TIME_TWELVE_EIGHT, 12, 8);
        numDen(TIME_TWO_FOUR, 2, 4);
        numDen(TIME_TWO_TWO, 2, 2);

        mapShape(TR, 0xE566);
        mapShape(TUPLET_SIX, 0xE886);
        mapShape(TUPLET_THREE, 0xE883);
        mapShape(TURN, 0xE567);
        mapShape(TURN_INVERTED, 0xE568);
        mapShape(TURN_SLASH, 0xE569);
        mapShape(TURN_UP, 0xE56A);

        sym.put(VENTIDUESIMA, new OctaveShiftSymbol(VENTIDUESIMA, 0xE517));

        mapShape(WHOLE_NOTE, CODE_WHOLE_NOTE);
        mapShape(WHOLE_NOTE_DIAMOND, CODE_WHOLE_NOTE_DIAMOND);
        mapShape(WHOLE_NOTE_CROSS, CODE_WHOLE_NOTE_CROSS);
        mapShape(WHOLE_NOTE_TRIANGLE_DOWN, CODE_WHOLE_NOTE_TRIANGLE_DOWN);
        small(WHOLE_NOTE_SMALL, CODE_WHOLE_NOTE);
        sym.put(WHOLE_REST, new RestSymbol(WHOLE_REST, 0xE4E3));

        // TO BE REMOVED
        mapShape(OTTAVA_ALTA, 0xE511);
        mapShape(OTTAVA_BASSA, 0xE512);
    }

    //- Convenient methods -------------------------------------------------------------------------
    //
    //---------//
    // flatKey //
    //---------//
    private static void flatKey (Shape shape,
                                 int key)
    {
        sym.put(shape, new KeyFlatSymbol(key, shape));
    }

    //----------//
    // mapShape //
    //----------//
    /**
     * Map shape to a ShapeSymbol instance.
     *
     * @param shape the shape to put in 'sym' map
     * @param codes font codes to use
     */
    private static void mapShape (Shape shape,
                                  int... codes)
    {
        sym.put(shape, new ShapeSymbol(shape, codes));
    }

    //---------//
    // mapText //
    //---------//
    /**
     * Map shape to a TextSymbol instance.
     *
     * @param shape  the shape to put in 'sym' map
     * @param string string to use
     */
    private static void mapText (Shape shape,
                                 String string)
    {
        sym.put(shape, new TextSymbol(shape, string));
    }

    //-------------//
    // numberCodes //
    //-------------//
    /**
     * Report the codes for a time number symbol.
     *
     * @param number integer value of the symbol (must be within [0..99] range)
     * @return corresponding codes in music font
     */
    public static int[] numberCodes (int number)
    {
        if (number < 0) {
            throw new IllegalArgumentException(number + " < 0");
        }

        if (number > 99) {
            throw new IllegalArgumentException(number + " > 99");
        }

        ShapeSymbol symbol = getSymbol(TIME_ZERO);
        int base = symbol.codes[0];
        int[] numberCodes = (number > 9) ? new int[2] : new int[1];
        int index = 0;

        if (number > 9) {
            numberCodes[index++] = base + (number / 10);
        }

        numberCodes[index] = base + (number % 10);

        return numberCodes;
    }

    //--------//
    // numDen //
    //--------//
    private static void numDen (Shape shape,
                                int num,
                                int den)
    {
        sym.put(shape, new NumDenSymbol(shape, num, den));
    }

    //----------//
    // sharpKey //
    //----------//
    private static void sharpKey (Shape shape,
                                  int key)
    {
        sym.put(shape, new KeySharpSymbol(key, shape));
    }

    //-------//
    // small //
    //-------//
    private static void small (Shape shape,
                               int... codes)
    {
        sym.put(shape, new TransformedSymbol(shape, OmrFont.TRANSFORM_SMALL, codes));
    }
}
