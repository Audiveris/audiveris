//----------------------------------------------------------------------------//
//                                                                            //
//                               S y m b o l s                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;

/**
 * Class {@code Symbols} manages all {@link ShapeSymbol} instances,
 * both for the simple symbols and for a few decorated symbols.
 *
 * @author Hervé Bitteur
 */
public class Symbols
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Symbols.class);

    /** Map of (simple) symbols */
    private static final EnumMap<Shape, ShapeSymbol> sym = new EnumMap<>(
            Shape.class);

    /** Map of decorated symbols */
    private static final EnumMap<Shape, ShapeSymbol> dec = new EnumMap<>(
            Shape.class);

    /** Symbol of '8' char for ottava sign (alta or bassa) on F & G clefs */
    public static final BasicSymbol SYMBOL_OTTAVA = new BasicSymbol(false, 165);

    /** Symbol for upper part of brace */
    public static final BasicSymbol SYMBOL_BRACE_UPPER_HALF = new BasicSymbol(
            167);

    /** Symbol for lower part of brace */
    public static final BasicSymbol SYMBOL_BRACE_LOWER_HALF = new BasicSymbol(
            234);

    /** Symbol for upper serif of bracket */
    public static final BasicSymbol SYMBOL_BRACKET_UPPER_SERIF = new BasicSymbol(
            194);

    /** Symbol for lower serif of bracket */
    public static final BasicSymbol SYMBOL_BRACKET_LOWER_SERIF = new BasicSymbol(
            76);

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
    public static final ShapeSymbol SYMBOL_FLAG_1 = new ShapeSymbol(
            FLAG_1,
            106);

    /** Symbol for FLAG_2 */
    public static final ShapeSymbol SYMBOL_FLAG_2 = new ShapeSymbol(
            FLAG_2,
            107);

    /** Symbol for FLAG_1_UP */
    public static final ShapeSymbol SYMBOL_FLAG_1_UP = new ShapeSymbol(
            FLAG_1_UP,
            74);

    /** Symbol for FLAG_2_UP */
    public static final ShapeSymbol SYMBOL_FLAG_2_UP = new ShapeSymbol(
            FLAG_2_UP,
            75);

    static {
        assignSymbols();
        assignDecoratedSymbols();
    }

    //~ Constructors -----------------------------------------------------------
    //---------//
    // Symbols // This is just a functional class, no instance is needed
    //---------//
    private Symbols ()
    {
    }

    //~ Methods ----------------------------------------------------------------
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
        dec.put(BEAM, new BeamSymbol(1, BEAM));
        dec.put(BEAM_2, new BeamSymbol(2, BEAM_2));
        dec.put(BEAM_3, new BeamSymbol(3, BEAM_3));
        dec.put(BEAM_HOOK, new BeamHookSymbol());
        dec.put(BRACE, new BraceSymbol(false));
        dec.put(BRACKET, new BracketSymbol(false));
        dec.put(BREVE_REST, new RestSymbol(BREVE_REST, true, 227));
        dec.put(CHARACTER, new TextSymbol(CHARACTER, "a"));
        dec.put(CLUTTER, new TextSymbol(CLUTTER, "?"));
        dec.put(STEM, new StemSymbol());
        dec.put(CRESCENDO, new CrescendoSymbol(false, CRESCENDO));
        dec.put(DECRESCENDO, new DecrescendoSymbol(false, DECRESCENDO));
        dec.put(ENDING, new EndingSymbol());
        dec.put(HALF_REST, new RestSymbol(HALF_REST, true, 238));
        dec.put(LEDGER, new LedgerSymbol(true));
        dec.put(LONG_REST, new LongRestSymbol(true));
        dec.put(NON_DRAGGABLE, new NonDraggableSymbol(192));
        dec.put(SLUR, new SlurSymbol());
        dec.put(TEXT, new TextSymbol(TEXT, "txt"));
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
        map(DYNAMICS_F, 102);
        map(DYNAMICS_FF, 196);
        map(DYNAMICS_FFF, 236);
        map(DYNAMICS_FZ, 90);
        map(DYNAMICS_SF, 83);
        map(NOTEHEAD_BLACK, 207);
        map(TIME_ZERO, 48);
        map(EIGHTH_REST, 228);
        map(RIGHT_REPEAT_SIGN, 125);
        map(THICK_BARLINE, 91);
        map(THIN_BARLINE, 108);
        map(WHOLE_REST, 238);
        map(HALF_REST, 238);
        map(REPEAT_DOT_PAIR, 123);

        // Other instances, in any order
        map(DOT_set, 46);
        map(AUGMENTATION_DOT, 46);
        map(STACCATO, 46);
        map(REPEAT_DOT, 46);

        map(ACCENT, 62);
        map(ARPEGGIATO, 103);
        sym.put(BACK_TO_BACK_REPEAT_SIGN, new BackToBackSymbol(false));
        map(BREATH_MARK, 44);
        map(BREVE, 87);
        sym.put(BREVE_REST, new RestSymbol(BREVE_REST, false, 227));
        map(CAESURA, 34);
        map(CODA, 222);

        sym.put(FLAG_1_set, new FlagsDownSymbol(1, false, FLAG_1_set));
        sym.put(FLAG_2_set, new FlagsDownSymbol(2, false, FLAG_2_set));
        sym.put(FLAG_3_set, new FlagsDownSymbol(3, false, FLAG_3_set));
        sym.put(FLAG_4_set, new FlagsDownSymbol(4, false, FLAG_4_set));
        sym.put(FLAG_5_set, new FlagsDownSymbol(5, false, FLAG_5_set));
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

        //        STEM?
        map(COMMON_TIME, 99);
        sym.put(CUSTOM_TIME, new CustomNumDenSymbol());
        map(CUT_TIME, 67);
        map(C_CLEF, 66);
        map(DAL_SEGNO, 100);
        map(DA_CAPO, 68);
        sym.put(DOUBLE_BARLINE, new DoubleBarlineSymbol(false));
        sym.put(DOUBLE_FLAT, new FlatSymbol(DOUBLE_FLAT, 186));
        map(DOUBLE_SHARP, 220);
        map(DYNAMICS_CHAR_M, 189);
        map(DYNAMICS_CHAR_R, 243);
        map(DYNAMICS_CHAR_S, 115);
        map(DYNAMICS_CHAR_Z, 122);
        //        slanted(DYNAMICS_FFFF, 236, 102);
        //        slanted(DYNAMICS_FFFFF, 236, 196);
        //        slanted(DYNAMICS_FFFFFF, 236, 236);
        slanted(DYNAMICS_FP, 102, 112);
        map(DYNAMICS_MF, 70);
        map(DYNAMICS_MP, 80);
        map(DYNAMICS_P, 112);
        map(DYNAMICS_PP, 185);
        map(DYNAMICS_PPP, 184);
        //        slanted(DYNAMICS_PPPP, 184, 112);
        //        slanted(DYNAMICS_PPPPP, 184, 185);
        //        slanted(DYNAMICS_PPPPPP, 184, 184);
        slanted(DYNAMICS_RF, 243, 102);
        slanted(DYNAMICS_RFZ, 243, 102, 122);
        slanted(DYNAMICS_SFFZ, 83, 90);
        slanted(DYNAMICS_SFP, 83, 112);
        slanted(DYNAMICS_SFPP, 83, 185);
        slanted(DYNAMICS_SFZ, 83, 122);

        //        map(ENDING_HORIZONTAL);
        //        map(ENDING_VERTICAL);
        map(FERMATA_set, 85);
        map(FERMATA, 85);
        map(FERMATA_BELOW, 117);
        map(FINAL_BARLINE, 211);
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
        sym.put(HW_REST_set, new RestSymbol(HALF_REST, false, 238));
        sym.put(HALF_REST, new RestSymbol(HALF_REST, false, 238));
        map(INVERTED_MORDENT, 77);
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
        map(NATURAL, 110);
        sym.put(NON_DRAGGABLE, new NonDraggableSymbol(192));
        heads(2, NOTEHEAD_BLACK_2, 207);
        heads(3, NOTEHEAD_BLACK_3, 207);
        //        map(NO_LEGAL_TIME);
        //        sym.put(
        //            OLD_QUARTER_REST,
        //            new TransformedSymbol(
        //                false,
        //                OLD_QUARTER_REST,
        //                EIGHTH_REST,
        //                ShapeSymbol.horizontalFlip));
        map(ONE_128TH_REST, 229);
        map(OTTAVA_ALTA, 195);
        map(OTTAVA_BASSA, 215);
        //        map(PART_DEFINING_BARLINE);
        map(PEDAL_MARK, 161);
        map(PEDAL_UP_MARK, 42);
        map(PERCUSSION_CLEF, 47);
        map(QUARTER_REST, 206);
        map(REVERSE_FINAL_BARLINE, 210);
        map(SEGNO, 37);
        map(SHARP, 35);
        map(ONE_16TH_REST, 197);
        map(ONE_64TH_REST, 244);
        //  SLUR?
        map(STACCATISSIMO, 174);
        map(STRONG_ACCENT, 94);
        //  STRUCTURE (for training?)
        map(TENUTO, 45);
        map(ONE_32ND_REST, 168);
        map(TIME_EIGHT, 56);
        map(TIME_FIVE, 53);
        map(TIME_FOUR, 52);
        numDen(TIME_FOUR_FOUR, 4, 4);
        map(TIME_NINE, 57);
        map(TIME_ONE, 49);
        map(TIME_SEVEN, 55);
        map(TIME_69_set, 54);
        map(TIME_SIX, 54);
        map(TIME_SIXTEEN, 49, 54);
        numDen(TIME_SIX_EIGHT, 6, 8);
        map(TIME_THREE, 51);
        numDen(TIME_THREE_FOUR, 3, 4);
        map(TIME_TWELVE, 49, 50);
        map(TIME_TWO, 50);
        numDen(TIME_TWO_FOUR, 2, 4);
        numDen(TIME_TWO_TWO, 2, 2);
        map(TR, 96);
        map(TUPLET_SIX, 164);
        map(TUPLET_THREE, 163);

        map(TURN_set, 84);
        map(TURN, 84);
        sym.put(
                INVERTED_TURN,
                new TransformedSymbol(
                false,
                INVERTED_TURN,
                TURN,
                ShapeSymbol.verticalFlip));
        sym.put(TURN_SLASH, new TurnSlashSymbol(false));
        sym.put(
                TURN_UP,
                new TransformedSymbol(
                false,
                TURN_UP,
                TURN,
                ShapeSymbol.quadrantRotateOne));

        heads(1, NOTEHEAD_VOID, 250);
        heads(2, NOTEHEAD_VOID_2, 250);
        heads(3, NOTEHEAD_VOID_3, 250);
        heads(1, WHOLE_NOTE, 119);
        heads(2, WHOLE_NOTE_2, 119);
        heads(3, WHOLE_NOTE_3, 119);
        sym.put(WHOLE_REST, new RestSymbol(WHOLE_REST, false, 183));
        sym.put(WEDGE_set, new CrescendoSymbol(false, CRESCENDO));
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

    //----------//
    // flatKey //
    //----------//
    private static void flatKey (int key,
                                 Shape shape)
    {
        sym.put(shape, new KeyFlatSymbol(key, false, shape));
    }

    //-------//
    // heads //
    //-------//
    private static void heads (int count,
                               Shape shape,
                               int... codes)
    {
        sym.put(shape, new HeadsSymbol(count, false, shape, codes));
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
