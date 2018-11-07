//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S y m b o l s                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

    /** Map of (simple) symbols */
    private static final EnumMap<Shape, ShapeSymbol> sym = new EnumMap<Shape, ShapeSymbol>(
            Shape.class);

    /** Map of decorated symbols */
    private static final EnumMap<Shape, ShapeSymbol> dec = new EnumMap<Shape, ShapeSymbol>(
            Shape.class);

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

    static {
        assignSymbols();
        assignDecoratedSymbols();
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

    //------------------------//
    // assignDecoratedSymbols //
    //------------------------//
    /**
     * Populate the 'dec' map: Shape -> decorated Symbol.
     */
    private static void assignDecoratedSymbols ()
    {
        dec.put(ACCENT, new ArticulationSymbol(ACCENT, true, 62));
        dec.put(AUGMENTATION_DOT, new AugmentationSymbol(true));
        dec.put(BEAM, new BeamSymbol(1, BEAM));
        dec.put(BEAM_HOOK, new BeamHookSymbol());
        //TODO: add BEAM_HOOK_SMALL?
        //TODO: add BEAM_SMALL?
        dec.put(BREVE_REST, new RestSymbol(BREVE_REST, true, 227));
        dec.put(CHARACTER, new TextSymbol(CHARACTER, "x"));
        dec.put(CLUTTER, new TextSymbol(CLUTTER, "?"));
        dec.put(ENDING, new EndingSymbol());
        dec.put(HALF_REST, new RestSymbol(HALF_REST, true, 238));
        dec.put(LEDGER, new LedgerSymbol(true));
        dec.put(LYRICS, new TextSymbol(LYRICS, "lyrics"));
        dec.put(LONG_REST, new LongRestSymbol(true));
        dec.put(NON_DRAGGABLE, new NonDraggableSymbol(192));
        dec.put(REPEAT_DOT, new RepeatDotSymbol(REPEAT_DOT, true, 46));
        dec.put(SLUR, new SlurSymbol());
        dec.put(STACCATISSIMO, new ArticulationSymbol(STACCATISSIMO, true, 174));
        dec.put(STACCATO, new ArticulationSymbol(STACCATO, true, 46));
        dec.put(STEM, new StemSymbol(true));
        dec.put(STRONG_ACCENT, new ArticulationSymbol(STRONG_ACCENT, true, 94));
        dec.put(TENUTO, new ArticulationSymbol(TENUTO, true, 45));
        dec.put(TEXT, new TextSymbol(TEXT, "text"));
        dec.put(WHOLE_REST, new RestSymbol(WHOLE_REST, true, 183));
    }

    //---------------//
    // assignSymbols //
    //---------------//
    /**
     * Populate the 'sym' map: Shape -> (simple) Symbol.
     * These symbols are draggable
     */
    private static void assignSymbols ()
    {
        // Instances to be defined first, because others depend on them
        //------------------------------
        map(DOT_set, 46);
        map(NOTEHEAD_BLACK, 207);
        map(REPEAT_DOT_PAIR, 123);
        map(RIGHT_REPEAT_SIGN, 125);
        map(THICK_BARLINE, 91);
        map(THIN_BARLINE, 108);
        map(TIME_ZERO, 48);

        // Other instances, alphabetical shape order for easier browsing
        //----------------
        sym.put(ACCENT, new ArticulationSymbol(ACCENT, false, 62));
        sym.put(ARPEGGIATO, new ArpeggiatosSymbol(2, false));
        map(AUGMENTATION_DOT, 46);

        sym.put(BACK_TO_BACK_REPEAT_SIGN, new BackToBackSymbol(false));
        sym.put(BRACE, new BraceSymbol(false));
        sym.put(BRACKET, new BracketSymbol(false));
        map(BREATH_MARK, 44);
        map(BREVE, 87);
        sym.put(BREVE_REST, new RestSymbol(BREVE_REST, false, 227));

        map(CAESURA, 34);
        map(COMMON_TIME, 99);
        sym.put(CUSTOM_TIME, new CustomNumDenSymbol());
        map(CUT_TIME, 67);
        map(C_CLEF, 66);
        map(CODA, 222);
        sym.put(CRESCENDO, new CrescendoSymbol(false, CRESCENDO));

        map(DAL_SEGNO, 100);
        map(DA_CAPO, 68);
        sym.put(DIGIT_0, new TextSymbol(DIGIT_0, "0"));
        sym.put(DIGIT_1, new TextSymbol(DIGIT_1, "1"));
        sym.put(DIGIT_2, new TextSymbol(DIGIT_2, "2"));
        sym.put(DIGIT_3, new TextSymbol(DIGIT_3, "3"));
        sym.put(DIGIT_4, new TextSymbol(DIGIT_4, "4"));
        sym.put(DIGIT_5, new TextSymbol(DIGIT_5, "5"));
        //        sym.put(DIGIT_6, new TextSymbol(DIGIT_6, "6"));
        //        sym.put(DIGIT_7, new TextSymbol(DIGIT_7, "7"));
        //        sym.put(DIGIT_8, new TextSymbol(DIGIT_8, "8"));
        //        sym.put(DIGIT_9, new TextSymbol(DIGIT_9, "9"));
        sym.put(DIMINUENDO, new DecrescendoSymbol(false, DIMINUENDO));
        sym.put(DOUBLE_BARLINE, new DoubleBarlineSymbol(false));
        sym.put(DOUBLE_FLAT, new FlatSymbol(DOUBLE_FLAT, 186));
        map(DOUBLE_SHARP, 220);
        //        map(DYNAMICS_CHAR_M, 189);
        //        map(DYNAMICS_CHAR_R, 243);
        //        map(DYNAMICS_CHAR_S, 115);
        //        map(DYNAMICS_CHAR_Z, 122);
        //        slanted(DYNAMICS_FFFF, 236, 102);
        map(DYNAMICS_F, 102);
        map(DYNAMICS_FF, 196);
        //        map(DYNAMICS_FFF, 236);
        //        map(DYNAMICS_FZ, 90);
        //        slanted(DYNAMICS_FFFFF, 236, 196);
        //        slanted(DYNAMICS_FFFFFF, 236, 236);
        slanted(DYNAMICS_FP, 102, 112);
        map(DYNAMICS_MF, 70);
        map(DYNAMICS_MP, 80);
        map(DYNAMICS_P, 112);
        map(DYNAMICS_PP, 185);
        //        map(DYNAMICS_PPP, 184);
        //        slanted(DYNAMICS_PPPP, 184, 112);
        //        slanted(DYNAMICS_PPPPP, 184, 185);
        //        slanted(DYNAMICS_PPPPPP, 184, 184);
        //        slanted(DYNAMICS_RF, 243, 102);
        //        slanted(DYNAMICS_RFZ, 243, 102, 122);
        map(DYNAMICS_SF, 83);
        //        slanted(DYNAMICS_SFFZ, 83, 90);
        //        slanted(DYNAMICS_SFP, 83, 112);
        slanted(DYNAMICS_SFZ, 83, 122);
        //        slanted(DYNAMICS_SFPP, 83, 185);
        map(EIGHTH_REST, 228);
        //        map(ENDING_HORIZONTAL);
        //        map(ENDING_VERTICAL);
        map(FERMATA, 85);
        sym.put(FERMATA_ARC, new FermataArcSymbol(FERMATA_ARC, false, 85));
        sym.put(FERMATA_ARC_BELOW, new FermataArcSymbol(FERMATA_ARC_BELOW, false, 117));
        map(FERMATA_BELOW, 117);
        map(FERMATA_DOT, 46);
        map(FINAL_BARLINE, 211);
        flagsDown(1, FLAG_1);
        flagsUp(1, FLAG_1_UP);
        flagsDown(2, FLAG_2);
        flagsUp(2, FLAG_2_UP);
        flagsDown(3, FLAG_3);
        flagsUp(3, FLAG_3_UP);
        flagsDown(4, FLAG_4);
        flagsUp(4, FLAG_4_UP);
        flagsDown(5, FLAG_5);
        flagsUp(5, FLAG_5_UP);
        sym.put(FLAT, new FlatSymbol(FLAT, 98));
        map(F_CLEF, 63);
        small(F_CLEF_SMALL, 63);
        ottava(true, F_CLEF_8VA, 63);
        ottava(false, F_CLEF_8VB, 63);

        map(GRACE_NOTE, 59);
        map(GRACE_NOTE_SLASH, 201);
        map(G_CLEF, 38);
        small(G_CLEF_SMALL, 38);
        ottava(true, G_CLEF_8VA, 38);
        ottava(false, G_CLEF_8VB, 38);

        sym.put(HALF_REST, new RestSymbol(HALF_REST, false, 238));
        sym.put(HW_REST_set, new RestSymbol(HW_REST_set, false, 238));

        flatKey(-1, KEY_FLAT_1);
        flatKey(-2, KEY_FLAT_2);
        flatKey(-3, KEY_FLAT_3);
        flatKey(-4, KEY_FLAT_4);
        flatKey(-5, KEY_FLAT_5);
        flatKey(-6, KEY_FLAT_6);
        flatKey(-7, KEY_FLAT_7);
        sharpKey(1, KEY_SHARP_1);
        sharpKey(2, KEY_SHARP_2);
        sharpKey(3, KEY_SHARP_3);
        sharpKey(4, KEY_SHARP_4);
        sharpKey(5, KEY_SHARP_5);
        sharpKey(6, KEY_SHARP_6);
        sharpKey(7, KEY_SHARP_7);

        sym.put(LEDGER, new LedgerSymbol(false));
        map(LEFT_REPEAT_SIGN, 93);
        sym.put(LONG_REST, new LongRestSymbol(false));

        map(MORDENT, 109);
        map(MORDENT_INVERTED, 77);

        map(NATURAL, 110);
        sym.put(NON_DRAGGABLE, new NonDraggableSymbol(192));
        small(NOTEHEAD_BLACK_SMALL, 207);
        map(NOTEHEAD_VOID, 250);
        small(NOTEHEAD_VOID_SMALL, 250);
        //        map(NO_LEGAL_TIME);

        //        sym.put(
        //            OLD_QUARTER_REST,
        //            new TransformedSymbol(
        //                false,
        //                OLD_QUARTER_REST,
        //                EIGHTH_REST,
        //                ShapeSymbol.horizontalFlip));
        map(ONE_16TH_REST, 197);
        map(ONE_64TH_REST, 244);
        map(ONE_32ND_REST, 168);
        map(ONE_128TH_REST, 229);
        map(OTTAVA_ALTA, 195);
        map(OTTAVA_BASSA, 215);

        map(PEDAL_MARK, 161);
        map(PEDAL_UP_MARK, 42);
        map(PERCUSSION_CLEF, 47);
        sym.put(PLUCK_P, new TextSymbol(PLUCK_P, "p"));
        sym.put(PLUCK_I, new TextSymbol(PLUCK_I, "i"));
        sym.put(PLUCK_M, new TextSymbol(PLUCK_M, "m"));
        sym.put(PLUCK_A, new TextSymbol(PLUCK_A, "a"));

        map(QUARTER_REST, 206);

        map(REPEAT_DOT, 46);
        map(REVERSE_FINAL_BARLINE, 210);
        sym.put(ROMAN_I, new TextSymbol(ROMAN_I, "I"));
        sym.put(ROMAN_II, new TextSymbol(ROMAN_II, "II"));
        sym.put(ROMAN_III, new TextSymbol(ROMAN_III, "III"));
        sym.put(ROMAN_IV, new TextSymbol(ROMAN_IV, "IV"));
        sym.put(ROMAN_V, new TextSymbol(ROMAN_V, "V"));
        sym.put(ROMAN_VI, new TextSymbol(ROMAN_VI, "VI"));
        sym.put(ROMAN_VII, new TextSymbol(ROMAN_VII, "VII"));
        sym.put(ROMAN_VIII, new TextSymbol(ROMAN_VIII, "VIII"));
        sym.put(ROMAN_IX, new TextSymbol(ROMAN_IX, "IX"));
        sym.put(ROMAN_X, new TextSymbol(ROMAN_X, "X"));
        sym.put(ROMAN_XI, new TextSymbol(ROMAN_XI, "XI"));
        sym.put(ROMAN_XII, new TextSymbol(ROMAN_XII, "XII"));

        map(SEGNO, 37);
        map(SHARP, 35);
        // SLUR?
        small(SMALL_FLAG, 106);
        sym.put(SMALL_FLAG_SLASH, new SlashedFlagSymbol(false));
        sym.put(STEM, new StemSymbol(false));
        sym.put(STACCATISSIMO, new ArticulationSymbol(STACCATISSIMO, false, 174));
        sym.put(STACCATO, new ArticulationSymbol(STACCATO, false, 46));
        sym.put(STRONG_ACCENT, new ArticulationSymbol(STRONG_ACCENT, false, 94));

        sym.put(TENUTO, new ArticulationSymbol(TENUTO, false, 45));
        map(TIME_EIGHT, 56);
        map(TIME_FIVE, 53);
        map(TIME_FOUR, 52);
        numDen(TIME_FOUR_FOUR, 4, 4);
        map(TIME_NINE, 57);
        map(TIME_ONE, 49);
        map(TIME_SEVEN, 55);
        map(TIME_SIX, 54);
        map(TIME_SIXTEEN, 49, 54);
        numDen(TIME_THREE_EIGHT, 3, 8);
        numDen(TIME_SIX_EIGHT, 6, 8);
        map(TIME_THREE, 51);
        numDen(TIME_THREE_FOUR, 3, 4);
        numDen(TIME_FIVE_FOUR, 5, 4);
        map(TIME_TWELVE, 49, 50);
        map(TIME_TWO, 50);
        numDen(TIME_TWO_FOUR, 2, 4);
        numDen(TIME_TWO_TWO, 2, 2);
        map(TR, 96);
        map(TUPLET_SIX, 164);
        map(TUPLET_THREE, 163);
        map(TURN, 84);
        sym.put(TURN_INVERTED, new TransformedSymbol(false, TURN_INVERTED, TURN,
                                                     ShapeSymbol.verticalFlip));
        sym.put(TURN_SLASH, new TurnSlashSymbol(false));
        sym.put(TURN_UP, new TransformedSymbol(false, TURN_UP, TURN, ShapeSymbol.quadrantRotateOne));

        map(WHOLE_NOTE, 119);
        small(WHOLE_NOTE_SMALL, 119);
        sym.put(WHOLE_REST, new RestSymbol(WHOLE_REST, false, 183));
    }

    //- Convenient methods -----------------------------------------------------
    //-----------//
    // flagsDown //
    //-----------//
    private static void flagsDown (int count,
                                   Shape shape)
    {
        sym.put(shape, new FlagsDownSymbol(count, false, shape));
    }

    //---------//
    // flagsUp //
    //---------//
    private static void flagsUp (int count,
                                 Shape shape)
    {
        sym.put(shape, new FlagsUpSymbol(count, false, shape));
    }

    //---------//
    // flatKey //
    //---------//
    private static void flatKey (int key,
                                 Shape shape)
    {
        sym.put(shape, new KeyFlatSymbol(key, false, shape));
    }

    //-----//
    // map //
    //-----//
    private static void map (Shape shape,
                             int... codes)
    {
        sym.put(shape, new ShapeSymbol(shape, codes));
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
    private static void ottava (boolean isAlta,
                                Shape shape,
                                int... codes)
    {
        sym.put(shape, new OttavaClefSymbol(isAlta, false, shape, codes));
    }

    //----------//
    // sharpKey //
    //----------//
    private static void sharpKey (int key,
                                  Shape shape)
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
        sym.put(shape, new ResizedSymbol(shape, 0.67d, codes));
    }
}
//
//    //-------//
//    // heads //
//    //-------//
//    private static void heads (int count,
//                               Shape shape,
//                               int... codes)
//    {
//        sym.put(shape, new HeadsSymbol(count, false, shape, codes));
//    }
//
