//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   M u s i c a l S y m b o l s                                  //
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

import java.util.Collections;
import java.util.List;

/**
 * Class <code>MusicalSymbols</code> was the first general set of symbols, it can still be used
 * for template symbols.
 *
 * @author Hervé Bitteur
 */
public class MusicalSymbols
        extends Symbols
{
    //~ Methods ------------------------------------------------------------------------------------

    @Override
    protected MusicFamily family ()
    {
        return MusicFamily.MusicalSymbols;
    }

    @Override
    public int[] getCode (Shape shape)
    {
        return switch (shape) {
            case ACCENT -> ints(0xF03E);
            case ARPEGGIATO -> ints(0xF067); // x2
            case AUGMENTATION_DOT -> ints(0xF02E);

            ///case BACK_TO_BACK_REPEAT_SIGN -> ints(0xE042); // composed
            ///case BRACE -> ints(0xE000); // 0xF0A7 + OxF0EA top + down
            ///case BRACKET -> ints(0xE002); // Composed with serifs
            case BREATH_MARK -> ints(0xF02C);
            case BREVE -> ints(0xF057);
            case BREVE_REST -> ints(0xF0E3);

            case CAESURA -> ints(0xF022);
            case COMMON_TIME -> ints(0xF063);
            case CUT_TIME -> ints(0xF043);
            case C_CLEF -> ints(0xF042);
            case CODA -> ints(0xF0DE);
            ///case CRESCENDO -> ints(0xE53E);

            case DAL_SEGNO -> ints(0xF064);
            case DA_CAPO -> ints(0xF044);
            ///case DIMINUENDO -> ints(0xE53F);
            case DOT_set -> ints(0xF02E);
            ///case DOUBLE_BARLINE -> ints(0xE031); // Composed
            case DOUBLE_FLAT -> ints(0xF0BA);
            case DOUBLE_SHARP -> ints(0xF0DC);
            case DYNAMICS_F -> ints(0xF066);
            case DYNAMICS_FF -> ints(0xF0C4);
            //        case DYNAMICS_FFF -> ints(0xF0EC);
            //        case DYNAMICS_FZ -> ints(0xF05A);
            //        case DYNAMICS_FFFF -> ints(0xE531);
            //        case DYNAMICS_FFFFF -> ints(0xE532);
            //        case DYNAMICS_FFFFFF -> ints(0xE533);
            ///case DYNAMICS_FP -> ints(0xE534); // Composed F + P
            case DYNAMICS_MF -> ints(0xF046);
            case DYNAMICS_MP -> ints(0xF050);
            case DYNAMICS_P -> ints(0xF070);
            case DYNAMICS_PP -> ints(0xF0B9);
            //        case DYNAMICS_PPP -> ints(0xF0B8);
            //        case DYNAMICS_PPPP -> ints(0xE529); // Composed
            //        case DYNAMICS_PPPPP -> ints(0xE528); // Composed
            //        case DYNAMICS_PPPPPP -> ints(0xE527); // Composed
            //        case DYNAMICS_RF -> ints(0xE53C); // Composed
            //        case DYNAMICS_RFZ -> ints(0xE53D); // Composed
            case DYNAMICS_SF -> ints(0xF053);
            //        case DYNAMICS_SFFZ -> ints(0xE53B); // Composed
            //        case DYNAMICS_SFP -> ints(0xE537); // Composed
            ///case DYNAMICS_SFZ -> ints(0xE539); // Composed SF:0xF053 Z:0xF07A
            //        case DYNAMICS_SFPP -> ints(0xE538);

            case EIGHTH_REST -> ints(0xF0E4);

            case FERMATA -> ints(0xF055);
            case FERMATA_BELOW -> ints(0xF075);
            case FINAL_BARLINE -> ints(0xF0D3);
            case FLAG_1 -> ints(0xF06A);
            case FLAG_1_DOWN -> ints(0xF04A);
            case FLAG_2 -> ints(0xF06B);
            case FLAG_2_DOWN -> ints(0xF04B);
            //        case FLAG_3 -> ints(0xE244);
            //        case FLAG_3_DOWN -> ints(0xE245);
            //        case FLAG_4 -> ints(0xE246);
            //        case FLAG_4_DOWN -> ints(0xE247);
            //        case FLAG_5 -> ints(0xE248);
            //        case FLAG_5_DOWN -> ints(0xE249);
            case FLAT -> ints(0xF062);
            case F_CLEF -> ints(0xF03F);
            ///case F_CLEF_SMALL -> ints(0xE07C);
            ///case F_CLEF_8VA -> ints(0xE065);
            ///case F_CLEF_8VB -> ints(0xE064);

            case EIGHTH_set, GRACE_NOTE, METRO_EIGHTH -> ints(0xF03B);
            case GRACE_NOTE_DOWN -> ints(0xF03A);
            case GRACE_NOTE_SLASH -> ints(0xF0C9);
            ///case GRACE_NOTE_SLASH_DOWN -> ints(0xF0C9); // Use vertical mirror of GRACE_NOTE_SLASH?
            case G_CLEF -> ints(0xF026);
            //        case G_CLEF_SMALL -> ints(0xE07A);
            //        case G_CLEF_8VA -> ints(0xE053);
            //        case G_CLEF_8VB -> ints(0xE052);

            case HALF_NOTE_DOWN -> ints(0xF048);
            case HALF_NOTE_UP -> ints(0xF068);
            case HALF_REST -> ints(0xF0EE);
            case HW_REST_set -> ints(0xF0EE);

            case LEDGER -> ints(0xF05F);
            case LEFT_REPEAT_SIGN -> ints(0xF05D);
            //        case LONG_REST -> ints(0xE4E1);

            case MORDENT -> ints(0xF06D);
            case MORDENT_INVERTED -> ints(0xF04D); // With bar

            case NATURAL -> ints(0xF06E);
            case NOTEHEAD_BLACK -> ints(0xF0CF);
            //        case NOTEHEAD_CIRCLE_X -> ints(0xE0B3);
            case NOTEHEAD_CROSS -> ints(0xF0C0);
            //        case NOTEHEAD_CROSS_VOID -> ints(0xE0A8);
            //        case NOTEHEAD_DIAMOND_FILLED -> ints(0xE0DB);
            //        case NOTEHEAD_DIAMOND_VOID -> ints(0xE0D9);
            //        case NOTEHEAD_TRIANGLE_DOWN_FILLED -> ints(0xE0C7);
            //        case NOTEHEAD_TRIANGLE_DOWN_VOID -> ints(0xE0C5);
            case NOTEHEAD_VOID -> ints(0xF0FA);

            //        case OLD_QUARTER_REST -> ints(0xE4F2);
            case ONE_16TH_REST -> ints(0xF0C5);
            case ONE_32ND_REST -> ints(0xF0A8);
            case ONE_64TH_REST -> ints(0xF0F4);
            case ONE_128TH_REST -> ints(0xF0E5);
            ///case OTTAVA -> ints(0xE510);

            case PEDAL_MARK -> ints(0xF0A1);
            case PEDAL_UP_MARK -> ints(0xF02A);
            case PERCUSSION_CLEF -> ints(0xF02F);

            case QUARTER_NOTE_DOWN -> ints(0xF051);
            case QUARTER_NOTE_UP -> ints(0xF071);
            case QUARTER_REST -> ints(0xF0CE);
            ///case QUINDICESIMA -> ints(0xE514);

            case REPEAT_DOT -> ints(0xF02E);
            case REPEAT_DOT_PAIR -> ints(0xF07B);
            case REPEAT_ONE_BAR -> ints(0xF0D4);
            //        case REPEAT_TWO_BARS -> ints(0xE501);
            //        case REPEAT_FOUR_BARS -> ints(0xE502);
            case REVERSE_FINAL_BARLINE -> ints(0xF0D2);
            case RIGHT_REPEAT_SIGN -> ints(0xF07D);

            case SEGNO -> ints(0xF025);
            case SHARP -> ints(0xF023);
            case STACCATISSIMO -> ints(0xF0AE);
            case STACCATO -> ints(0xF02E);
            case STEM -> ints(0xF06C);
            case STRONG_ACCENT -> ints(0xF05E);

            case TENUTO -> ints(0xF02D);
            case THICK_BARLINE -> ints(0xF05B);
            case THIN_BARLINE -> ints(0xF05C);
            case TIME_ZERO -> ints(0xF030);
            case TIME_ONE -> ints(0xF031);
            case TIME_TWO -> ints(0xF032);
            case TIME_THREE -> ints(0xF033);
            case TIME_FOUR -> ints(0xF034);
            case TIME_FIVE -> ints(0xF035);
            case TIME_SIX -> ints(0xF036);
            case TIME_SEVEN -> ints(0xF037);
            case TIME_EIGHT -> ints(0xF038);
            case TIME_NINE -> ints(0xF039);
            case TIME_TWELVE -> ints(0xF031, 0xF032);
            case TIME_SIXTEEN -> ints(0xF031, 0xF036);
            case TR -> ints(0xF060);
            case TUPLET_SIX -> ints(0xF0A4);
            case TUPLET_THREE -> ints(0xF0A3);
            case TURN -> ints(0xF054);
            //        case TURN_INVERTED -> ints(0xE568);
            //        case TURN_SLASH -> ints(0xE569);
            //        case TURN_UP -> ints(0xE56A);

            ///case VENTIDUESIMA -> ints(0xE517);
            case WHOLE_NOTE -> ints(0xF077);
            //        case WHOLE_NOTE_CROSS -> ints(0xE0A7);
            //        case WHOLE_NOTE_CIRCLE_X -> ints(0xE0B1);
            //        case WHOLE_NOTE_DIAMOND -> ints(0xE0D8);
            //        case WHOLE_NOTE_TRIANGLE_DOWN -> ints(0xE0C4);
            case WHOLE_REST -> ints(0xF0B7);

            default -> null;
        };
    }

    //---------------//
    // getCodeRanges //
    //---------------//
    @Override
    public List<CodeRange> getCodeRanges ()
    {
        // Range is much smaller than PRIVATE_USE_AREA
        return Collections.singletonList(new CodeRange(0xF021, 0xF0FF));
    }

    //-----------------//
    // populateSymbols //
    //-----------------//
    @Override
    protected void populateSymbols ()
    {
        super.populateSymbols();

        // Additions specific for MusicalSymbols
        mapSmall(G_CLEF_SMALL, G_CLEF);
        mapSmall(F_CLEF_SMALL, F_CLEF);
    }
}
