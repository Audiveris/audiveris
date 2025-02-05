//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                F i n a l e J a z z S y m b o l s                               //
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
import org.audiveris.omr.math.PointUtil;
import static org.audiveris.omr.ui.symbol.Alignment.BOTTOM_LEFT;
import static org.audiveris.omr.ui.symbol.Alignment.TOP_LEFT;
import static org.audiveris.omr.ui.symbol.Symbols.ints;

import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>FinaleJazzSymbols</code> is a set of symbols meant for Jazz scores.
 *
 * @author Hervé Bitteur
 */
public class FinaleJazzSymbols
        extends Symbols
{
    //~ Methods ------------------------------------------------------------------------------------

    @Override
    protected MusicFamily family ()
    {
        return MusicFamily.FinaleJazz;
    }

    @Override
    public int[] getCode (Shape shape)
    {
        return switch (shape) {
            case ACCENT -> ints(0xE4A0);
            ///case ARPEGGIATO -> ints(0xE63C);
            case AUGMENTATION_DOT -> ints(0xE044);

            case BACK_TO_BACK_REPEAT_SIGN -> ints(0xE042);
            ///case BRACE -> ints(0xE000);
            ///case BRACKET -> ints(0xE002);
            ///case BRACKET_LOWER_SERIF -> ints(0xE004);
            ///case BRACKET_UPPER_SERIF -> ints(0xE003);
            case BREATH_MARK -> ints(0xE4CE);
            case BREVE -> ints(0xE0A0);
            ///case BREVE_CROSS -> ints(0xE0A6);
            ///case BREVE_DIAMOND -> ints(0xE0D7);
            ///case BREVE_TRIANGLE_DOWN -> ints(0xE0C3);
            ///case BREVE_CIRCLE_X -> ints(0xE0B0);
            case BREVE_REST -> ints(0xE4E2);

            case CAESURA -> ints(0xE4D1);
            case COMMON_TIME -> ints(0xE08A);
            case CUT_TIME -> ints(0xE08B);
            case C_CLEF -> ints(0xE05C);
            case CODA -> ints(0xE048);
            case CRESCENDO -> ints(0xE53E);

            ///case DAL_SEGNO -> ints(0xE045);
            ///case DA_CAPO -> ints(0xE046);
            case DIMINUENDO -> ints(0xE53F);
            case DOT_set -> ints(0xE044);
            case DOTTED_HALF_NOTE_UP, METRO_DOTTED_HALF -> ints(0xE1D3, 0x0020, 0xE1E7);
            case DOTTED_QUARTER_NOTE_UP, METRO_DOTTED_QUARTER -> ints(0xE1D5, 0x0020, 0xE1E7);
            case DOTTED_EIGHTH_NOTE_UP, METRO_DOTTED_EIGHTH -> ints(0xE1D7, 0xE1E7);
            case DOTTED_SIXTEENTH_NOTE_UP, METRO_DOTTED_SIXTEENTH -> ints(0xE1D9, 0xE1E7);
            //case DOUBLE_BARLINE -> ints(0xE031);
            case DOUBLE_FLAT -> ints(0xE264);
            case DOUBLE_SHARP -> ints(0xE263);
            case DYNAMICS_F -> ints(0xE522);
            case DYNAMICS_FF -> ints(0xE52F);
            //        case DYNAMICS_FFF -> ints(0xE530);
            //        case DYNAMICS_FZ -> ints(0xE535);
            //        case DYNAMICS_FFFF -> ints(0xE531);
            //        case DYNAMICS_FFFFF -> ints(0xE532);
            //        case DYNAMICS_FFFFFF -> ints(0xE533);
            case DYNAMICS_FP -> ints(0xE534);
            case DYNAMICS_MF -> ints(0xE52D);
            case DYNAMICS_MP -> ints(0xE52C);
            case DYNAMICS_P -> ints(0xE520);
            case DYNAMICS_PP -> ints(0xE52B);
            //        case DYNAMICS_PPP -> ints(0xE52A);
            //        case DYNAMICS_PPPP -> ints(0xE529);
            //        case DYNAMICS_PPPPP -> ints(0xE528);
            //        case DYNAMICS_PPPPPP -> ints(0xE527);
            //        case DYNAMICS_RF -> ints(0xE53C);
            //        case DYNAMICS_RFZ -> ints(0xE53D);
            case DYNAMICS_SF -> ints(0xE536);
            //        case DYNAMICS_SFFZ -> ints(0xE53B);
            //        case DYNAMICS_SFP -> ints(0xE537);
            case DYNAMICS_SFZ -> ints(0xE539);
            //        case DYNAMICS_SFPP -> ints(0xE538);

            case EIGHTH_NOTE_UP, METRO_EIGHTH -> ints(0xE1D7);
            case EIGHTH_REST -> ints(0xE4E6);
            case EIGHTH_set, GRACE_NOTE -> ints(0xE562);

            case FERMATA -> ints(0xE4C0);
            case FERMATA_BELOW -> ints(0xE4C1);
            ///case FINAL_BARLINE -> ints(0xE032);
            case FLAG_1 -> ints(0xE250);
            case FLAG_1_DOWN -> ints(0xE251);
            case FLAG_2 -> ints(0xE242);
            case FLAG_2_DOWN -> ints(0xE243);
            //        case FLAG_3 -> ints(0xE244);
            //        case FLAG_3_DOWN -> ints(0xE245);
            //        case FLAG_4 -> ints(0xE246);
            //        case FLAG_4_DOWN -> ints(0xE247);
            //        case FLAG_5 -> ints(0xE248);
            //        case FLAG_5_DOWN -> ints(0xE249);
            case FLAT -> ints(0xE260);
            case F_CLEF -> ints(0xE062);
            ///case F_CLEF_SMALL -> ints(0xE07C); // Use transformed symbol
            case F_CLEF_8VA -> ints(0xE065);
            case F_CLEF_8VB -> ints(0xE064);

            case GRACE_NOTE_DOWN -> ints(0xE563);
            case GRACE_NOTE_SLASH -> ints(0xE560);
            case GRACE_NOTE_SLASH_DOWN -> ints(0xE561);
            case G_CLEF -> ints(0xE050);
            ///case G_CLEF_SMALL -> ints(0xE07A); // Use transformed symbol
            case G_CLEF_8VA -> ints(0xE053);
            case G_CLEF_8VB -> ints(0xE052);

            case HALF_NOTE_DOWN -> ints(0xE1D4);
            case HALF_NOTE_UP, METRO_HALF -> ints(0xE1D3);
            case HALF_REST -> ints(0xE4E4);
            case HW_REST_set -> ints(0xE4E4);

            case KEY_CANCEL -> ints(0xE261);

            case LEDGER -> ints(0xE022);
            case LEFT_REPEAT_SIGN -> ints(0xE040);
            case LONG_REST -> ints(0xE4E1);

            case MORDENT -> ints(0xE56C);
            case MORDENT_INVERTED -> ints(0xE56D); // With bar
            case MULTIPLE_REST -> ints(0xE4EE);
            case MULTIPLE_REST_LEFT -> ints(0xE4EF);
            case MULTIPLE_REST_MIDDLE -> ints(0xE4F0);
            case MULTIPLE_REST_RIGHT -> ints(0xE4F1);

            case NATURAL -> ints(0xE261);
            ///case NON_DRAGGABLE -> ints(0xEA94, 0xEA93);

            case NOTEHEAD_BLACK -> ints(0xE0A4);
            case NOTEHEAD_CROSS -> ints(0xE0A9);
            case NOTEHEAD_DIAMOND_FILLED -> ints(0xE0DB);
            case NOTEHEAD_TRIANGLE_DOWN_FILLED -> ints(0xE0C7);
            case NOTEHEAD_CIRCLE_X -> ints(0xE0B3);

            case NOTEHEAD_VOID -> ints(0xE0A3);
            ///case NOTEHEAD_CROSS_VOID -> ints(0xE0A8);
            case NOTEHEAD_DIAMOND_VOID -> ints(0xE0DA); // 0xE0D9 in Bravura
            case NOTEHEAD_TRIANGLE_DOWN_VOID -> ints(0xE0C6); // 0xE0C5 in Bravura
            case NOTEHEAD_CIRCLE_X_VOID -> ints(0xE0B7); // 0xE0B2 in Bravura

            case ONE_16TH_REST -> ints(0xE4E7);
            case ONE_64TH_REST -> ints(0xE4E9);
            case ONE_32ND_REST -> ints(0xE4E8);
            case ONE_128TH_REST -> ints(0xE4EA);
            case OTTAVA -> ints(0xE510);

            case PEDAL_MARK -> ints(0xE650);
            case PEDAL_UP_MARK -> ints(0xE655);
            case PERCUSSION_CLEF -> ints(0xE069);

            case PLAYING_OPEN -> ints(0xE614); // 0xE870 ?
            case PLAYING_HALF_OPEN -> ints(0xE7F8); // 0xE871 ?
            case PLAYING_CLOSED -> ints(0xE5E5); // 0xE872 ?

            case QUARTER_NOTE_DOWN -> ints(0xE1D6);
            case QUARTER_NOTE_UP, METRO_QUARTER -> ints(0xE1D5);
            case QUARTER_REST -> ints(0xE4E5);
            case QUINDICESIMA -> ints(0xE514);

            case REPEAT_DOT -> ints(0xE044);
            case REPEAT_DOT_PAIR -> ints(0xE043);
            case REPEAT_ONE_BAR -> ints(0xE500);
            case REPEAT_TWO_BARS -> ints(0xE501);
            case REPEAT_FOUR_BARS -> ints(0xE502);
            ///case REVERSE_FINAL_BARLINE -> ints(0xE033);
            case RIGHT_REPEAT_SIGN -> ints(0xE041);

            case SEGNO -> ints(0xE047);
            case SHARP -> ints(0xE262);
            case SIXTEENTH_NOTE_UP, METRO_SIXTEENTH -> ints(0xE1D9);
            case STACCATISSIMO -> ints(0xE4A6);
            case STACCATO -> ints(0xE4A2);
            case STAFF_LINES -> ints(0xE014); // 0xE01A in Bravura
            case STEM -> ints(0xE210);
            case STRONG_ACCENT -> ints(0xE4AC);

            case TENUTO -> ints(0xE4A4);
            ///case THICK_BARLINE -> ints(0xE034);
            case THIN_BARLINE -> ints(0xE030);
            case TIME_ZERO -> ints(0xE080);
            case TIME_ONE -> ints(0xE081);
            case TIME_TWO -> ints(0xE082);
            case TIME_THREE -> ints(0xE083);
            case TIME_FOUR -> ints(0xE084);
            case TIME_FIVE -> ints(0xE085);
            case TIME_SIX -> ints(0xE086);
            case TIME_SEVEN -> ints(0xE087);
            case TIME_EIGHT -> ints(0xE088);
            case TIME_NINE -> ints(0xE089);
            case TIME_TWELVE -> ints(0xE081, 0xE082);
            case TIME_SIXTEEN -> ints(0xE081, 0xE086);
            case TR -> ints(0xE566);
            case TREMOLO_1 -> ints(0xE220);
            case TREMOLO_2 -> ints(0xE221);
            case TREMOLO_3 -> ints(0xE222);
            case TUPLET_SIX -> ints(0xE886);
            case TUPLET_THREE -> ints(0xE883);
            case TURN -> ints(0xE567);
            ///case TURN_INVERTED -> ints(0xE568);
            ///case TURN_SLASH -> ints(0xE569);
            ///case TURN_UP -> ints(0xE56A);

            ///case VENTIDUESIMA -> ints(0xE517);

            case WHOLE_NOTE, METRO_WHOLE -> ints(0xE0A2);
            ///case WHOLE_NOTE_CROSS -> ints(0xE0A7);
            case WHOLE_NOTE_DIAMOND -> ints(0xE0DA);
            case WHOLE_NOTE_TRIANGLE_DOWN -> ints(0xE0C6);
            case WHOLE_NOTE_CIRCLE_X -> ints(0xE0B7);
            case WHOLE_REST -> ints(0xE4E3);

            default -> null;
        };
    }

    //-----------------//
    // populateSymbols //
    //-----------------//
    @Override
    protected void populateSymbols ()
    {
        super.populateSymbols();

        // Additions specific for FinaleJazz
        mapSmall(G_CLEF_SMALL, G_CLEF);
        mapSmall(F_CLEF_SMALL, F_CLEF);

        symbolMap.put(FLAG_3, new FlagsSymbol(FLAG_3, family(), 3));
        symbolMap.put(FLAG_4, new FlagsSymbol(FLAG_4, family(), 4));
        symbolMap.put(FLAG_5, new FlagsSymbol(FLAG_5, family(), 5));

        symbolMap.put(FLAG_3_DOWN, new FlagsDownSymbol(FLAG_3_DOWN, family(), 3));
        symbolMap.put(FLAG_4_DOWN, new FlagsDownSymbol(FLAG_4_DOWN, family(), 4));
        symbolMap.put(FLAG_5_DOWN, new FlagsDownSymbol(FLAG_5_DOWN, family(), 5));
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------------//
    // FlagsDownSymbol //
    //-----------------//
    /**
     * Class <code>FlagsDownSymbol</code> displays a pack of several flags down
     */
    public class FlagsDownSymbol
            extends FlagsSymbol
    {
        /**
         * Creates a new FlagsDownSymbol object.
         *
         * @param shape     the related shape
         * @param family    the selected music font family
         * @param flagCount the number of flags
         */
        public FlagsDownSymbol (Shape shape,
                                MusicFamily family,
                                int flagCount)
        {
            super(shape, family, flagCount);
        }

        @Override
        protected MyParams initParams (MusicFont font)
        {
            MyParams p = new MyParams();

            p.flag1 = font.layoutShapeByCode(Shape.FLAG_1_DOWN);
            p.rect1 = p.flag1.getBounds();
            p.flag2 = font.layoutShapeByCode(Shape.FLAG_2_DOWN);
            p.rect2 = p.flag2.getBounds();
            p.dy = (int) Math.rint(p.rect2.getHeight() * dyF2Ratio);
            p.align = TOP_LEFT;

            return p;
        }
    }

    //-------------//
    // FlagsSymbol //
    //-------------//
    /**
     * Class <code>FlagsSymbol</code> displays a pack of several flags up.
     */
    private static class FlagsSymbol
            extends ShapeSymbol
    {
        /** Delta ordinate as ratio of flag2 height. */
        protected static final double dyF2Ratio = 0.55;

        /** The number of flags. */
        protected final int fn;

        /**
         * Creates a new FlagsSymbol object.
         *
         * @param shape  the related shape
         * @param family the selected music font family
         * @param isIcon true for an icon
         */
        public FlagsSymbol (Shape shape,
                            MusicFamily family,
                            int flagCount)
        {
            super(shape, family);
            this.fn = flagCount;
        }

        @Override
        protected MyParams getParams (MusicFont font)
        {
            MyParams p = initParams(font);

            p.rect = new Rectangle2D.Double(
                    0,
                    0,
                    p.rect2.getWidth(),
                    (((fn / 2) + ((fn + 1) % 2)) * Math.abs(p.dy)) + ((fn % 2) * p.rect1
                            .getHeight()));

            return p;
        }

        protected MyParams initParams (MusicFont font)
        {
            MyParams p = new MyParams();

            p.flag1 = font.layoutShapeByCode(Shape.FLAG_1);
            p.rect1 = p.flag1.getBounds();
            p.flag2 = font.layoutShapeByCode(Shape.FLAG_2);
            p.rect2 = p.flag2.getBounds();
            p.dy = -(int) Math.rint(p.rect2.getHeight() * dyF2Ratio);
            p.align = BOTTOM_LEFT;

            return p;
        }

        @Override
        protected void paint (Graphics2D g,
                              Params params,
                              Point2D location,
                              Alignment alignment)
        {
            MyParams p = (MyParams) params;
            Point2D loc = alignment.translatedPoint(p.align, p.rect, location);

            // We draw from tail to head, double(s) then single if needed
            for (int i = 0; i < (fn / 2); i++) {
                MusicFont.paint(g, p.flag2, loc, p.align);
                PointUtil.add(loc, 0, p.dy);
            }

            if ((fn % 2) != 0) {
                MusicFont.paint(g, p.flag1, loc, p.align);
            }
        }

        protected static class MyParams
                extends ShapeSymbol.Params
        {
            TextLayout flag1;

            Rectangle2D rect1;

            TextLayout flag2;

            Rectangle2D rect2;

            double dy;

            Alignment align;
        }
    }
}
