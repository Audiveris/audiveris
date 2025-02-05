//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S y m b o l s                                          //
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

import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;
import static org.audiveris.omr.ui.symbol.OmrFont.TRANSFORM_METRO;
import static org.audiveris.omr.ui.symbol.OmrFont.TRANSFORM_SMALL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/**
 * Class <code>Symbols</code> manages all {@link ShapeSymbol} instances for a given font family.
 * <p>
 * A ShapeSymbol can be:
 * <ul>
 * <li>A symbol defined via its font code point. See {@link CodedSymbol}.
 * <li>A symbol built from various components, often to alleviate font undefined symbols.
 * See {@link SlurSymbol} for example.
 * </ul>
 * Beside its plain version, any symbol can appear in:
 * <ul>
 * <li>A <b>decorated</b> version which presents a symbol with contextual additions.
 * See {@link AugmentationSymbol} or {@link RestSymbol} for example.
 * <li>A <b>tiny</b> version which presents a symbol in reduced size, for its use in UI buttons.
 * </ul>
 * <img alt="Symbols diagram" src="doc-files/Symbols.png">
 *
 * @author Hervé Bitteur
 */
public abstract class Symbols
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Symbols.class);

    public static final CodeRange PRIVATE_USE_AREA = new CodeRange(0xE000, 0xF8FF);

    /** Code for a space character. */
    public static final int SPACE = 0x20;

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * For the related font family, this is the map of plain symbol per shape.
     */
    protected final EnumMap<Shape, ShapeSymbol> symbolMap = new EnumMap<>(Shape.class);

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // family //
    //--------//
    /**
     * Report the Family enum these symbols definitions are related to.
     *
     * @return the family value
     */
    protected abstract MusicFamily family ();

    //---------//
    // getCode //
    //---------//
    /**
     * Report the code point, if any, defined in font family for the provided music shape.
     * <p>
     * From one font family to the other, a given music shape can be undefined or mapped to a
     * different code.
     * SMuFL-compliant fonts use the same code (for the music shapes they define).
     *
     * @param shape the provided music shape
     * @return code point or null if not supported
     */
    public abstract int[] getCode (Shape shape);

    //---------------//
    // getCodeRanges //
    //---------------//
    /**
     * Report the ranges of usable codes in a given font family.
     *
     * @return a list of CodeRange instances
     */
    public List<CodeRange> getCodeRanges ()
    {
        // Just the typical Private Use Area
        return Collections.singletonList(PRIVATE_USE_AREA);
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
    public ShapeSymbol getSymbol (Shape shape)
    {
        // First, try a prepopulated symbol
        final ShapeSymbol symbol = symbolMap.get(shape);
        if (symbol != null) {
            return symbol;
        }

        // Second, try a code-based symbol dynamically built
        final int[] codes = getCode(shape);
        if (codes != null) {
            return new CodedSymbol(shape, family(), codes);
        }

        // None
        return null;
    }

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
    public ShapeSymbol getSymbol (Shape shape,
                                  boolean decorated)
    {
        final ShapeSymbol symbol = getSymbol(shape);

        if (symbol == null) {
            return null;
        }

        if (decorated) {
            return symbol.getDecoratedVersion();
        }

        return symbol;
    }

    //------------//
    // mapFlatKey //
    //------------//
    protected void mapFlatKey (Shape shape,
                               int key)
    {
        symbolMap.put(shape, new KeyFlatSymbol(key, shape, family()));
    }

    //----------//
    // mapMetro //
    //----------//
    protected void mapMetro (Shape shape,
                             Shape root)
    {
        symbolMap.put(shape, new TransformedSymbol(shape, root, TRANSFORM_METRO, family()));
    }

    //-----------//
    // mapNumDen //
    //-----------//
    protected void mapNumDen (Shape shape,
                              int num,
                              int den)
    {
        symbolMap.put(shape, new NumDenSymbol(shape, family(), num, den));
    }

    //-------------//
    // mapSharpKey //
    //-------------//
    protected void mapSharpKey (Shape shape,
                                int key)
    {
        symbolMap.put(shape, new KeySharpSymbol(key, shape, family()));
    }

    //----------//
    // mapSmall //
    //----------//
    protected void mapSmall (Shape shape,
                             Shape root)
    {
        symbolMap.put(shape, new TransformedSymbol(shape, root, TRANSFORM_SMALL, family()));
    }

    //---------//
    // mapText //
    //---------//
    /**
     * Map shape to a TextSymbol instance.
     *
     * @param shape  the shape to put in 'symbolMap' map
     * @param string string to use
     */
    protected void mapText (Shape shape,
                            String string)
    {
        symbolMap.put(shape, new TextSymbol(shape, family(), string));
    }

    //-----------------//
    // populateSymbols //
    //-----------------//
    protected void populateSymbols ()
    {
        // Decorated symbols
        symbolMap.put(ACCENT, new ArticulationSymbol(ACCENT, family()));
        symbolMap.put(STACCATISSIMO, new ArticulationSymbol(STACCATISSIMO, family()));
        symbolMap.put(STACCATO, new ArticulationSymbol(STACCATO, family()));
        symbolMap.put(STRONG_ACCENT, new ArticulationSymbol(STRONG_ACCENT, family()));
        symbolMap.put(TENUTO, new ArticulationSymbol(TENUTO, family()));

        symbolMap.put(AUGMENTATION_DOT, new AugmentationSymbol(family()));

        symbolMap.put(BEAM, new BeamSymbol(family()));
        symbolMap.put(BEAM_SMALL, new SmallBeamSymbol(family()));
        symbolMap.put(BEAM_HOOK, new BeamHookSymbol(family()));

        symbolMap.put(LEDGER, new LedgerSymbol(family()));

        symbolMap.put(OTTAVA, new OctaveShiftSymbol(OTTAVA, family()));
        symbolMap.put(QUINDICESIMA, new OctaveShiftSymbol(QUINDICESIMA, family()));
        symbolMap.put(VENTIDUESIMA, new OctaveShiftSymbol(VENTIDUESIMA, family()));

        symbolMap.put(REPEAT_DOT, new RepeatDotSymbol(family()));

        symbolMap.put(LONG_REST, new RestSymbol(LONG_REST, family()));
        symbolMap.put(BREVE_REST, new RestSymbol(BREVE_REST, family()));
        symbolMap.put(WHOLE_REST, new RestSymbol(WHOLE_REST, family()));
        symbolMap.put(HALF_REST, new RestSymbol(HALF_REST, family()));
        symbolMap.put(HW_REST_set, new RestSymbol(HW_REST_set, family()));

        symbolMap.put(STEM, new StemSymbol(family()));

        symbolMap.put(TREMOLO_1, new TremoloSymbol(TREMOLO_1, family()));
        symbolMap.put(TREMOLO_2, new TremoloSymbol(TREMOLO_2, family()));
        symbolMap.put(TREMOLO_3, new TremoloSymbol(TREMOLO_3, family()));

        // Small symbols
        mapSmall(NOTEHEAD_BLACK_SMALL, NOTEHEAD_BLACK);
        mapSmall(NOTEHEAD_VOID_SMALL, NOTEHEAD_VOID);
        mapSmall(WHOLE_NOTE_SMALL, WHOLE_NOTE);
        mapSmall(BREVE_SMALL, BREVE);
        mapSmall(SMALL_FLAG, FLAG_1);
        mapSmall(SMALL_FLAG_DOWN, FLAG_1_DOWN);

        // Metronome symbols
        mapMetro(METRO_WHOLE, WHOLE_NOTE);
        mapMetro(METRO_HALF, HALF_NOTE_UP);
        mapMetro(METRO_DOTTED_HALF, DOTTED_HALF_NOTE_UP);
        mapMetro(METRO_QUARTER, QUARTER_NOTE_UP);
        mapMetro(METRO_DOTTED_QUARTER, DOTTED_QUARTER_NOTE_UP);
        mapMetro(METRO_EIGHTH, EIGHTH_NOTE_UP);
        mapMetro(METRO_DOTTED_EIGHTH, DOTTED_EIGHTH_NOTE_UP);
        mapMetro(METRO_SIXTEENTH, SIXTEENTH_NOTE_UP);
        mapMetro(METRO_DOTTED_SIXTEENTH, DOTTED_SIXTEENTH_NOTE_UP);

        // Specific symbols
        symbolMap.put(BRACE, new BraceSymbol(family()));
        symbolMap.put(ENDING, new EndingSymbol(false, family()));
        symbolMap.put(ENDING_WRL, new EndingSymbol(true, family()));

        symbolMap.put(SMALL_FLAG_SLASH, new SlashedFlagSymbol(SMALL_FLAG_SLASH, family()));
        symbolMap
                .put(SMALL_FLAG_SLASH_DOWN, new SlashedFlagSymbol(SMALL_FLAG_SLASH_DOWN, family()));

        symbolMap.put(FLAT, new FlatSymbol(FLAT, family()));
        symbolMap.put(DOUBLE_FLAT, new FlatSymbol(DOUBLE_FLAT, family()));

        symbolMap.put(HALF_NOTE_UP, new CompoundNoteSymbol(HALF_NOTE_UP, family()));
        symbolMap.put(HALF_NOTE_DOWN, new CompoundNoteSymbol(HALF_NOTE_DOWN, family()));
        symbolMap.put(QUARTER_NOTE_UP, new CompoundNoteSymbol(QUARTER_NOTE_UP, family()));
        symbolMap.put(QUARTER_NOTE_DOWN, new CompoundNoteSymbol(QUARTER_NOTE_DOWN, family()));

        symbolMap.put(METRONOME, new MetronomeSymbol(METRO_QUARTER, family()));

        symbolMap.put(NON_DRAGGABLE, new NonDraggableSymbol(family()));

        symbolMap.put(REPEAT_TWO_BARS, new RepeatBarSymbol(REPEAT_TWO_BARS, family()));
        symbolMap.put(REPEAT_FOUR_BARS, new RepeatBarSymbol(REPEAT_FOUR_BARS, family()));

        symbolMap.put(SLUR_ABOVE, new SlurSymbol(true, family()));
        symbolMap.put(SLUR_BELOW, new SlurSymbol(false, family()));

        // Text symbols
        mapText(CLUTTER, "?");

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

        mapText(LYRICS, "lyric");

        mapText(PLUCK_P, "p");
        mapText(PLUCK_I, "i");
        mapText(PLUCK_M, "m");
        mapText(PLUCK_A, "a");

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

        mapText(TEXT, "text");

        // Keys
        mapFlatKey(KEY_FLAT_7, -7);
        mapFlatKey(KEY_FLAT_6, -6);
        mapFlatKey(KEY_FLAT_5, -5);
        mapFlatKey(KEY_FLAT_4, -4);
        mapFlatKey(KEY_FLAT_3, -3);
        mapFlatKey(KEY_FLAT_2, -2);
        mapFlatKey(KEY_FLAT_1, -1);
        symbolMap.put(KEY_CANCEL, new KeyCancelSymbol(family()));
        mapSharpKey(KEY_SHARP_1, 1);
        mapSharpKey(KEY_SHARP_2, 2);
        mapSharpKey(KEY_SHARP_3, 3);
        mapSharpKey(KEY_SHARP_4, 4);
        mapSharpKey(KEY_SHARP_5, 5);
        mapSharpKey(KEY_SHARP_6, 6);
        mapSharpKey(KEY_SHARP_7, 7);

        // Predefined time signatures as: Numerator / Denominator
        mapNumDen(TIME_CUSTOM, 0, 0);
        mapNumDen(TIME_FIVE_FOUR, 5, 4);
        mapNumDen(TIME_FOUR_FOUR, 4, 4);
        mapNumDen(TIME_SIX_EIGHT, 6, 8);
        mapNumDen(TIME_SIX_FOUR, 6, 4);
        mapNumDen(TIME_THREE_EIGHT, 3, 8);
        mapNumDen(TIME_THREE_FOUR, 3, 4);
        mapNumDen(TIME_TWELVE_EIGHT, 12, 8);
        mapNumDen(TIME_TWO_FOUR, 2, 4);
        mapNumDen(TIME_TWO_TWO, 2, 2);

        // Custom number
        symbolMap.put(NUMBER_CUSTOM, new NumberSymbol(NUMBER_CUSTOM, family(), 0));
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------//
    // ints //
    //------//
    /**
     * Meant to simplify code writing.
     *
     * @param codes sequence of one int or more parameters
     * @return the int array
     */
    public static int[] ints (int... codes)
    {
        return codes;
    }

    //--------//
    // shrink //
    //--------//
    /**
     * Report an array where the space characters have been removed.
     *
     * @param codes the input array
     * @return the purged array
     */
    public static int[] shrink (int... codes)
    {
        final int[] output = new int[codes.length];

        int j = 0;
        for (int i = 0; i < codes.length; i++) {
            final int code = codes[i];
            if (code != SPACE) {
                output[j++] = code;
            }
        }

        if (j == codes.length) {
            return codes;
        }

        return Arrays.copyOf(output, j);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // CodeRange //
    //-----------//
    /**
     * This class defines the range of usable codes for font symbols.
     */
    public static class CodeRange
    {
        final int start;

        final int stop;

        public CodeRange (int start,
                          int stop)
        {
            this.start = start;
            this.stop = stop;
        }

        @Override
        public String toString ()
        {
            return new StringBuilder().append('[').append(start).append("..").append(stop)
                    .append(']').toString();
        }
    }
}
