//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S y m b o l s                                          //
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

import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;

/**
 * Class {@code Symbols} manages all {@link ShapeSymbol} instances, both for the simple
 * symbols and for a few decorated symbols.
 *
 * @author Hervé Bitteur
 */
public abstract class Symbols
{

    private static final Logger logger = LoggerFactory.getLogger(Symbols.class);

    /** Symbol of '8' char for ottava sign (alta or bassa) on F &amp; G clefs */
    public static final BasicSymbol SYMBOL_OTTAVA = new BasicSymbol(false, 165);

    /** Symbol for upper part of brace */
    public static final BasicSymbol SYMBOL_BRACE_UPPER_HALF = new BasicSymbol(167);

    /** Symbol for lower part of brace */
    public static final BasicSymbol SYMBOL_BRACE_LOWER_HALF = new BasicSymbol(234);

    /** Symbol for upper serif of bracket */
    public static final BasicSymbol SYMBOL_BRACKET_UPPER_SERIF = new BasicSymbol(194);

    /** Symbol for lower serif of bracket */
    public static final BasicSymbol SYMBOL_BRACKET_LOWER_SERIF = new BasicSymbol(76);

    /** Symbol for staff lines */
    public static final BasicSymbol SYMBOL_STAFF_LINES = new BasicSymbol(61);

    /** Symbol for stem */
    public static final BasicSymbol SYMBOL_STEM = new BasicSymbol(92);

    /** Symbol for ledger */
    public static final BasicSymbol SYMBOL_LEDGER = new BasicSymbol(45);

    /** Symbol for a complete quarter (head + stem) */
    public static final BasicSymbol SYMBOL_QUARTER = new BasicSymbol(113);

    /** Symbol for a user mark */
    public static final BasicSymbol SYMBOL_MARK = new BasicSymbol(205);

    /** Symbol for FLAG_1 */
    public static final ShapeSymbol SYMBOL_FLAG_1 = new ShapeSymbol(FLAG_1, 106);

    /** Symbol for FLAG_2 */
    public static final ShapeSymbol SYMBOL_FLAG_2 = new ShapeSymbol(FLAG_2, 107);

    /** Symbol for FLAG_1_UP */
    public static final ShapeSymbol SYMBOL_FLAG_1_UP = new ShapeSymbol(FLAG_1_UP, 74);

    /** Symbol for FLAG_2_UP */
    public static final ShapeSymbol SYMBOL_FLAG_2_UP = new ShapeSymbol(FLAG_2_UP, 75);

    /** Map of (simple) symbols. */
    private static final EnumMap<Shape, ShapeSymbol> sym = new EnumMap<>(Shape.class);

    /** Map of decorated symbols. */
    private static final EnumMap<Shape, ShapeSymbol> dec = new EnumMap<>(Shape.class);

    static {
        assignSymbols();
    }

    /** This is just a functional class, no instance is needed. */
    private Symbols ()
    {
    }

    //-----------//
    // getSymbol //
    //-----------//
    /**
     * Return a symbol from its definition in MusicFont.
     *
     * @param shape     the symbol related shape
     * @param decorated true for a decorated symbol, false for a simple one
     * @return the desired symbol, or null if none found
     */
    public static ShapeSymbol getSymbol (Shape shape,
                                         boolean decorated)
    {
        if (decorated) {
            ShapeSymbol symbol = dec.get(shape);

            if (symbol != null) {
                return symbol;
            }
        }

        return getSymbol(shape);
    }

    //-----------//
    // getSymbol //
    //-----------//
    /**
     * Return a simple symbol from its definition in MusicFont.
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
        mapShape(DOT_set, 46);
        mapShape(NOTEHEAD_BLACK, 207);
        mapShape(REPEAT_DOT_PAIR, 123);
        mapShape(RIGHT_REPEAT_SIGN, 125);
        mapShape(THICK_BARLINE, 91);
        mapShape(THIN_BARLINE, 108);
        mapShape(TIME_ZERO, 48);

        // Other instances, in alphabetical shape order for easier browsing
        //----------------
        sym.put(ACCENT, new ArticulationSymbol(ACCENT, false, 62));
        sym.put(ARPEGGIATO, new ArpeggiatosSymbol(2));
        sym.put(AUGMENTATION_DOT, new AugmentationSymbol(false));

        sym.put(BACK_TO_BACK_REPEAT_SIGN, new BackToBackSymbol());
        sym.put(BEAM, new BeamSymbol(false));
        sym.put(BEAM_HOOK, new BeamHookSymbol(false));
        sym.put(BRACE, new BraceSymbol());
        sym.put(BRACKET, new BracketSymbol());
        mapShape(BREATH_MARK, 44);
        mapShape(BREVE, 87);
        sym.put(BREVE_REST, new RestSymbol(BREVE_REST, false, 227));

        mapShape(CAESURA, 34);
        sym.put(CLUTTER, new TextSymbol(CLUTTER, "?"));
        mapShape(COMMON_TIME, 99);
        sym.put(CUSTOM_TIME, new CustomNumDenSymbol());
        mapShape(CUT_TIME, 67);
        mapShape(C_CLEF, 66);
        mapShape(CODA, 222);
        sym.put(CRESCENDO, new WedgeSymbol(CRESCENDO));

        mapShape(DAL_SEGNO, 100);
        mapShape(DA_CAPO, 68);
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
        sym.put(DIMINUENDO, new WedgeSymbol(DIMINUENDO));
        sym.put(DOUBLE_BARLINE, new DoubleBarlineSymbol());
        sym.put(DOUBLE_FLAT, new FlatSymbol(DOUBLE_FLAT, 186));
        mapShape(DOUBLE_SHARP, 220);
        //        mapShape(DYNAMICS_CHAR_M, 189);
        //        mapShape(DYNAMICS_CHAR_R, 243);
        //        mapShape(DYNAMICS_CHAR_S, 115);
        //        mapShape(DYNAMICS_CHAR_Z, 122);
        //        slanted(DYNAMICS_FFFF, 236, 102);
        mapShape(DYNAMICS_F, 102);
        mapShape(DYNAMICS_FF, 196);
        //        mapShape(DYNAMICS_FFF, 236);
        //        mapShape(DYNAMICS_FZ, 90);
        //        slanted(DYNAMICS_FFFFF, 236, 196);
        //        slanted(DYNAMICS_FFFFFF, 236, 236);
        slanted(DYNAMICS_FP, 102, 112);
        mapShape(DYNAMICS_MF, 70);
        mapShape(DYNAMICS_MP, 80);
        mapShape(DYNAMICS_P, 112);
        mapShape(DYNAMICS_PP, 185);
        //        mapShape(DYNAMICS_PPP, 184);
        //        slanted(DYNAMICS_PPPP, 184, 112);
        //        slanted(DYNAMICS_PPPPP, 184, 185);
        //        slanted(DYNAMICS_PPPPPP, 184, 184);
        //        slanted(DYNAMICS_RF, 243, 102);
        //        slanted(DYNAMICS_RFZ, 243, 102, 122);
        mapShape(DYNAMICS_SF, 83);
        //        slanted(DYNAMICS_SFFZ, 83, 90);
        //        slanted(DYNAMICS_SFP, 83, 112);
        slanted(DYNAMICS_SFZ, 83, 122);
        //        slanted(DYNAMICS_SFPP, 83, 185);

        sym.put(ENDING, new EndingSymbol(false));
        sym.put(ENDING_WRL, new EndingSymbol(true));
        mapShape(EIGHTH_REST, 228);

        mapShape(FERMATA, 85);
        sym.put(FERMATA_ARC, new FermataArcSymbol(FERMATA_ARC, false, 85));
        sym.put(FERMATA_ARC_BELOW, new FermataArcSymbol(FERMATA_ARC_BELOW, false, 117));
        mapShape(FERMATA_BELOW, 117);
        mapShape(FERMATA_DOT, 46);
        mapShape(FINAL_BARLINE, 211);
        flagsDown(FLAG_1, 1);
        flagsUp(FLAG_1_UP, 1);
        flagsDown(FLAG_2, 2);
        flagsUp(FLAG_2_UP, 2);
        flagsDown(FLAG_3, 3);
        flagsUp(FLAG_3_UP, 3);
        flagsDown(FLAG_4, 4);
        flagsUp(FLAG_4_UP, 4);
        flagsDown(FLAG_5, 5);
        flagsUp(FLAG_5_UP, 5);
        sym.put(FLAT, new FlatSymbol(FLAT, 98));
        mapShape(F_CLEF, 63);
        small(F_CLEF_SMALL, 63);
        ottava(F_CLEF_8VA, true, 63);
        ottava(F_CLEF_8VB, false, 63);

        mapShape(GRACE_NOTE, 59);
        mapShape(GRACE_NOTE_SLASH, 201);
        mapShape(G_CLEF, 38);
        small(G_CLEF_SMALL, 38);
        ottava(G_CLEF_8VA, true, 38);
        ottava(G_CLEF_8VB, false, 38);

        sym.put(HALF_REST, new RestSymbol(HALF_REST, false, 238));
        sym.put(HW_REST_set, new RestSymbol(HW_REST_set, false, 238));

        flatKey(KEY_FLAT_1, -1);
        flatKey(KEY_FLAT_2, -2);
        flatKey(KEY_FLAT_3, -3);
        flatKey(KEY_FLAT_4, -4);
        flatKey(KEY_FLAT_5, -5);
        flatKey(KEY_FLAT_6, -6);
        flatKey(KEY_FLAT_7, -7);
        sharpKey(KEY_SHARP_1, 1);
        sharpKey(KEY_SHARP_2, 2);
        sharpKey(KEY_SHARP_3, 3);
        sharpKey(KEY_SHARP_4, 4);
        sharpKey(KEY_SHARP_5, 5);
        sharpKey(KEY_SHARP_6, 6);
        sharpKey(KEY_SHARP_7, 7);

        sym.put(LEDGER, new LedgerSymbol(false));
        mapShape(LEFT_REPEAT_SIGN, 93);
        mapText(LYRICS, "lyric");
        sym.put(LONG_REST, new LongRestSymbol(false));

        mapShape(MORDENT, 109);
        mapShape(MORDENT_INVERTED, 77);

        mapShape(NATURAL, 110);
        sym.put(NON_DRAGGABLE, new NonDraggableSymbol(192));
        small(NOTEHEAD_BLACK_SMALL, 207);
        mapShape(NOTEHEAD_VOID, 250);
        small(NOTEHEAD_VOID_SMALL, 250);
        //        mapShape(NO_LEGAL_TIME);

        //        sym.put(OLD_QUARTER_REST,
        //            new TransformedSymbol(
        //                OLD_QUARTER_REST,
        //                EIGHTH_REST,
        //                ShapeSymbol.horizontalFlip));
        mapShape(ONE_16TH_REST, 197);
        mapShape(ONE_64TH_REST, 244);
        mapShape(ONE_32ND_REST, 168);
        mapShape(ONE_128TH_REST, 229);
        mapShape(OTTAVA_ALTA, 195);
        mapShape(OTTAVA_BASSA, 215);

        mapShape(PEDAL_MARK, 161);
        mapShape(PEDAL_UP_MARK, 42);
        mapShape(PERCUSSION_CLEF, 47);
        mapText(PLUCK_P, "p");
        mapText(PLUCK_I, "i");
        mapText(PLUCK_M, "m");
        mapText(PLUCK_A, "a");

        mapShape(QUARTER_REST, 206);

        sym.put(REPEAT_DOT, new RepeatDotSymbol(false));
        mapShape(REVERSE_FINAL_BARLINE, 210);
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

        mapShape(SEGNO, 37);
        mapShape(SHARP, 35);
        sym.put(SLUR_ABOVE, new SlurSymbol(true));
        sym.put(SLUR_BELOW, new SlurSymbol(false));
        small(SMALL_FLAG, 106);
        sym.put(SMALL_FLAG_SLASH, new SlashedFlagSymbol());
        sym.put(STACCATISSIMO, new ArticulationSymbol(STACCATISSIMO, false, 174));
        sym.put(STACCATO, new ArticulationSymbol(STACCATO, false, 46));
        sym.put(STEM, new StemSymbol(false));
        sym.put(STRONG_ACCENT, new ArticulationSymbol(STRONG_ACCENT, false, 94));

        sym.put(TENUTO, new ArticulationSymbol(TENUTO, false, 45));
        mapText(TEXT, "text");
        mapShape(TIME_EIGHT, 56);
        mapShape(TIME_FIVE, 53);
        mapShape(TIME_FOUR, 52);
        numDen(TIME_FOUR_FOUR, 4, 4);
        mapShape(TIME_NINE, 57);
        mapShape(TIME_ONE, 49);
        mapShape(TIME_SEVEN, 55);
        mapShape(TIME_SIX, 54);
        mapShape(TIME_SIXTEEN, 49, 54);
        numDen(TIME_THREE_EIGHT, 3, 8);
        numDen(TIME_SIX_EIGHT, 6, 8);
        mapShape(TIME_THREE, 51);
        numDen(TIME_THREE_FOUR, 3, 4);
        numDen(TIME_FIVE_FOUR, 5, 4);
        mapShape(TIME_TWELVE, 49, 50);
        mapShape(TIME_TWO, 50);
        numDen(TIME_TWO_FOUR, 2, 4);
        numDen(TIME_TWO_TWO, 2, 2);
        mapShape(TR, 96);
        mapShape(TUPLET_SIX, 164);
        mapShape(TUPLET_THREE, 163);
        mapShape(TURN, 84);
        sym.put(TURN_INVERTED, new TransformedSymbol(TURN_INVERTED, TURN, ShapeSymbol.verticalFlip));
        sym.put(TURN_SLASH, new TurnSlashSymbol());
        sym.put(TURN_UP, new TransformedSymbol(TURN_UP, TURN, ShapeSymbol.quadrantRotateOne));

        mapShape(WHOLE_NOTE, 119);
        small(WHOLE_NOTE_SMALL, 119);
        sym.put(WHOLE_REST, new RestSymbol(WHOLE_REST, false, 183));
    }

    //- Convenient methods -------------------------------------------------------------------------
    //
    //-----------//
    // flagsDown //
    //-----------//
    private static void flagsDown (Shape shape,
                                   int count)
    {
        sym.put(shape, new FlagsDownSymbol(count, false, shape));
    }

    //---------//
    // flagsUp //
    //---------//
    private static void flagsUp (Shape shape,
                                 int count)
    {
        sym.put(shape, new FlagsUpSymbol(count, false, shape));
    }

    //---------//
    // flatKey //
    //---------//
    private static void flatKey (Shape shape,
                                 int key)
    {
        sym.put(shape, new KeyFlatSymbol(key, false, shape));
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

    //--------//
    // numDen //
    //--------//
    private static void numDen (Shape shape,
                                int num,
                                int den)
    {
        sym.put(shape, new NumDenSymbol(shape, num, den));
    }

    //--------//
    // ottava //
    //--------//
    private static void ottava (Shape shape,
                                boolean isAlta,
                                int... codes)
    {
        sym.put(shape, new OttavaClefSymbol(isAlta, false, shape, codes));
    }

    //----------//
    // sharpKey //
    //----------//
    private static void sharpKey (Shape shape,
                                  int key)
    {
        sym.put(shape, new KeySharpSymbol(key, false, shape));
    }

    //---------//
    // slanted //
    //---------//
    private static void slanted (Shape shape,
                                 int... codes)
    {
        sym.put(shape, new SlantedSymbol(shape, codes));
    }

    //-------//
    // small //
    //-------//
    private static void small (Shape shape,
                               int... codes)
    {
        sym.put(shape, new ResizedSymbol(shape, 0.67, codes));
    }
}
//
//    //-------//
//    // heads //
//    //-------//
//    private static void heads (Shape shape,
//                               int count,
//                               int... codes)
//    {
//        sym.put(shape, new HeadsSymbol(count, false, shape, codes));
//    }
//
