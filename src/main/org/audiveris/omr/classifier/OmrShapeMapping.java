//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  O m r S h a p e M a p p i n g                                 //
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
package org.audiveris.omr.classifier;

import java.util.EnumMap;
import java.util.Map;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.OmrShapes;

import static org.audiveris.omrdataset.api.OmrShapes.COMBO_MAP;

/**
 * Class {@code OmrShapeMapping} handles mappings between Shape and OmrShape.
 *
 * @author Hervé Bitteur
 */
public abstract class OmrShapeMapping
{
    //~ Static fields/initializers -----------------------------------------------------------------

    public static final Map<Shape, OmrShape> SHAPE_TO_OMRSHAPE = buildShapeMap();

    public static final Map<OmrShape, Shape> OMRSHAPE_TO_SHAPE = buildOmrShapeMap();

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the mapped OmrShape, if any, for a given TimePairInter.
     *
     * @param timePair the provided TimePairInter instance
     * @return the corresponding OmrShape or null
     */
    public static OmrShape getTimeCombo (TimePairInter timePair)
    {
        int num = timePair.getNum().getValue();
        int den = timePair.getDen().getValue();

        for (Map.Entry<OmrShape, OmrShapes.NumDen> entry : COMBO_MAP.entrySet()) {
            OmrShapes.NumDen nd = entry.getValue();

            if ((nd.num == num) && (nd.den == den)) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * Build the map (OmrShape -> Shape) as the reverse of SHAPE_TO_OMRSHAPE.
     *
     * @return the initialized map
     */
    private static Map<OmrShape, Shape> buildOmrShapeMap ()
    {
        final Map<OmrShape, Shape> map = new EnumMap<OmrShape, Shape>(OmrShape.class);

        for (Map.Entry<Shape, OmrShape> entry : SHAPE_TO_OMRSHAPE.entrySet()) {
            final Shape shape = entry.getKey();
            final OmrShape omrShape = entry.getValue();

            if (omrShape != null) {
                map.put(omrShape, shape);
            }
        }

        return map;
    }

    /**
     * Build the map (Shape -> OmrShape).
     *
     * @return the initialized map
     */
    private static Map<Shape, OmrShape> buildShapeMap ()
    {
        final Map<Shape, OmrShape> map = new EnumMap<Shape, OmrShape>(Shape.class);

        map.put(Shape.DAL_SEGNO, OmrShape.dalSegno);
        map.put(Shape.DA_CAPO, OmrShape.daCapo);
        map.put(Shape.SEGNO, OmrShape.segno);
        map.put(Shape.CODA, OmrShape.coda);
        map.put(Shape.BREATH_MARK, OmrShape.breathMarkComma);
        map.put(Shape.CAESURA, OmrShape.caesura);
        map.put(Shape.G_CLEF, OmrShape.gClef);
        map.put(Shape.G_CLEF_SMALL, OmrShape.gClefChange);
        map.put(Shape.G_CLEF_8VA, OmrShape.gClef8va);
        map.put(Shape.G_CLEF_8VB, OmrShape.gClef8vb);
        map.put(Shape.C_CLEF, OmrShape.cClef);
        map.put(Shape.F_CLEF, OmrShape.fClef);
        map.put(Shape.F_CLEF_SMALL, OmrShape.fClefChange);
        map.put(Shape.F_CLEF_8VA, OmrShape.fClef8va);
        map.put(Shape.F_CLEF_8VB, OmrShape.fClef8vb);
        map.put(Shape.PERCUSSION_CLEF, OmrShape.unpitchedPercussionClef1);
        map.put(Shape.FLAT, OmrShape.accidentalFlat); // If not in key
        map.put(Shape.NATURAL, OmrShape.accidentalNatural); // If not in key
        map.put(Shape.SHARP, OmrShape.accidentalSharp); // If not in key
        map.put(Shape.DOUBLE_SHARP, OmrShape.accidentalDoubleSharp);
        map.put(Shape.DOUBLE_FLAT, OmrShape.accidentalDoubleFlat);
        map.put(Shape.TIME_ZERO, OmrShape.timeSig0);
        map.put(Shape.TIME_ONE, OmrShape.timeSig1);
        map.put(Shape.TIME_TWO, OmrShape.timeSig2);
        map.put(Shape.TIME_THREE, OmrShape.timeSig3);
        map.put(Shape.TIME_FOUR, OmrShape.timeSig4);
        map.put(Shape.TIME_FIVE, OmrShape.timeSig5);
        map.put(Shape.TIME_SIX, OmrShape.timeSig6);
        map.put(Shape.TIME_SEVEN, OmrShape.timeSig7);
        map.put(Shape.TIME_EIGHT, OmrShape.timeSig8);
        map.put(Shape.TIME_NINE, OmrShape.timeSig9);
        map.put(Shape.TIME_TWELVE, OmrShape.timeSig12);
        map.put(Shape.TIME_SIXTEEN, OmrShape.timeSig16);
        map.put(Shape.COMMON_TIME, OmrShape.timeSigCommon);
        map.put(Shape.CUT_TIME, OmrShape.timeSigCutCommon);
        map.put(Shape.TIME_FOUR_FOUR, OmrShape.timeSig4over4);
        map.put(Shape.TIME_TWO_TWO, OmrShape.timeSig2over2);
        map.put(Shape.TIME_TWO_FOUR, OmrShape.timeSig2over4);
        map.put(Shape.TIME_THREE_FOUR, OmrShape.timeSig3over4);
        map.put(Shape.TIME_FIVE_FOUR, OmrShape.timeSig5over4);
        map.put(Shape.TIME_THREE_EIGHT, OmrShape.timeSig3over8);
        map.put(Shape.TIME_SIX_EIGHT, OmrShape.timeSig6over8);
        map.put(Shape.OTTAVA_ALTA, OmrShape.ottavaAlta);
        map.put(Shape.OTTAVA_BASSA, OmrShape.ottavaBassaVb);
        map.put(Shape.LONG_REST, OmrShape.restLonga);
        map.put(Shape.BREVE_REST, OmrShape.restDoubleWhole);
        map.put(Shape.QUARTER_REST, OmrShape.restQuarter);
        map.put(Shape.EIGHTH_REST, OmrShape.rest8th);
        map.put(Shape.ONE_16TH_REST, OmrShape.rest16th);
        map.put(Shape.ONE_32ND_REST, OmrShape.rest32nd);
        map.put(Shape.ONE_64TH_REST, OmrShape.rest64th);
        map.put(Shape.ONE_128TH_REST, OmrShape.rest128th);
        map.put(Shape.FLAG_1, OmrShape.flag8thUp);
        map.put(Shape.FLAG_1_UP, OmrShape.flag8thDown);
        map.put(Shape.FLAG_2, OmrShape.flag16thUp);
        map.put(Shape.FLAG_2_UP, OmrShape.flag16thDown);
        map.put(Shape.FLAG_3, OmrShape.flag32ndUp);
        map.put(Shape.FLAG_3_UP, OmrShape.flag32ndDown);
        map.put(Shape.FLAG_4, OmrShape.flag64thUp);
        map.put(Shape.FLAG_4_UP, OmrShape.flag64thDown);
        map.put(Shape.FLAG_5, OmrShape.flag128thUp);
        map.put(Shape.FLAG_5_UP, OmrShape.flag128thDown);
        map.put(Shape.SMALL_FLAG, OmrShape.flag8thUpSmall);
        //        map.put(Shape.SMALL_FLAG_SLASH, OmrShape.none);
        map.put(Shape.BREVE, OmrShape.noteheadDoubleWhole);
        //        map.put(Shape.ACCENT, OmrShape.none); // articAccentAbove or articAccentBelow
        //        map.put(Shape.TENUTO, OmrShape.none); // articTenutoAbove or articTenutoBelow
        //        map.put(Shape.STACCATISSIMO, OmrShape.none); // articStaccatissimoAbove or articStaccatissimoBelow
        //        map.put(Shape.STRONG_ACCENT, OmrShape.none); // articMarcatoAbove or articMarcatoBelow
        //        map.put(Shape.STACCATO, OmrShape.none); // articStaccatoAbove or articStaccatoBelow
        map.put(Shape.ARPEGGIATO, OmrShape.arpeggiato);
        map.put(Shape.DYNAMICS_P, OmrShape.dynamicPiano);
        map.put(Shape.DYNAMICS_PP, OmrShape.dynamicPP);
        map.put(Shape.DYNAMICS_MP, OmrShape.dynamicMP);
        map.put(Shape.DYNAMICS_F, OmrShape.dynamicForte);
        map.put(Shape.DYNAMICS_FF, OmrShape.dynamicFF);
        map.put(Shape.DYNAMICS_MF, OmrShape.dynamicMF);
        map.put(Shape.DYNAMICS_FP, OmrShape.dynamicFortePiano);
        map.put(Shape.DYNAMICS_SF, OmrShape.dynamicSforzando1);
        map.put(Shape.DYNAMICS_SFZ, OmrShape.dynamicSforzato);
        map.put(Shape.TR, OmrShape.ornamentTrill);
        map.put(Shape.TURN, OmrShape.ornamentTurn);
        map.put(Shape.TURN_INVERTED, OmrShape.ornamentTurnInverted);
        map.put(Shape.TURN_UP, OmrShape.ornamentTurnUp);
        map.put(Shape.TURN_SLASH, OmrShape.ornamentTurnSlash);
        map.put(Shape.MORDENT, OmrShape.ornamentMordent);
        map.put(Shape.MORDENT_INVERTED, OmrShape.ornamentMordentInverted);
        map.put(Shape.TUPLET_THREE, OmrShape.tuplet3);
        map.put(Shape.TUPLET_SIX, OmrShape.tuplet6);
        map.put(Shape.PEDAL_MARK, OmrShape.keyboardPedalPed);
        map.put(Shape.PEDAL_UP_MARK, OmrShape.keyboardPedalUp);
        map.put(Shape.DIGIT_0, OmrShape.fingering0);
        map.put(Shape.DIGIT_1, OmrShape.fingering1);
        map.put(Shape.DIGIT_2, OmrShape.fingering2);
        map.put(Shape.DIGIT_3, OmrShape.fingering3);
        map.put(Shape.DIGIT_4, OmrShape.fingering4);
        map.put(Shape.DIGIT_5, OmrShape.fingering5);
        //        map.put(Shape.ROMAN_I, OmrShape.none);
        //        map.put(Shape.ROMAN_II, OmrShape.none);
        //        map.put(Shape.ROMAN_III, OmrShape.none);
        //        map.put(Shape.ROMAN_IV, OmrShape.none);
        //        map.put(Shape.ROMAN_V, OmrShape.none);
        //        map.put(Shape.ROMAN_VI, OmrShape.none);
        //        map.put(Shape.ROMAN_VII, OmrShape.none);
        //        map.put(Shape.ROMAN_VIII, OmrShape.none);
        //        map.put(Shape.ROMAN_IX, OmrShape.none);
        //        map.put(Shape.ROMAN_X, OmrShape.none);
        //        map.put(Shape.ROMAN_XI, OmrShape.none);
        //        map.put(Shape.ROMAN_XII, OmrShape.none);
        map.put(Shape.PLUCK_P, OmrShape.fingeringPLower);
        map.put(Shape.PLUCK_I, OmrShape.fingeringILower);
        map.put(Shape.PLUCK_M, OmrShape.fingeringMLower);
        map.put(Shape.PLUCK_A, OmrShape.fingeringALower);
        map.put(Shape.CLUTTER, OmrShape.none);
        //        map.put(Shape.TEXT, OmrShape.none);
        //        map.put(Shape.CHARACTER, OmrShape.none);
        map.put(Shape.REPEAT_DOT, OmrShape.repeatDot);
        map.put(Shape.AUGMENTATION_DOT, OmrShape.augmentationDot);
        map.put(Shape.WHOLE_REST, OmrShape.restWhole);
        map.put(Shape.HALF_REST, OmrShape.restHalf);
        map.put(Shape.NOTEHEAD_BLACK, OmrShape.noteheadBlack);
        map.put(Shape.NOTEHEAD_BLACK_SMALL, OmrShape.noteheadBlackSmall);
        map.put(Shape.NOTEHEAD_VOID, OmrShape.noteheadHalf);
        map.put(Shape.NOTEHEAD_VOID_SMALL, OmrShape.noteheadHalfSmall);
        map.put(Shape.WHOLE_NOTE, OmrShape.noteheadWhole);
        map.put(Shape.WHOLE_NOTE_SMALL, OmrShape.noteheadWholeSmall);
        //        map.put(Shape.BEAM, OmrShape.none);
        //        map.put(Shape.BEAM_SMALL, OmrShape.none);
        //        map.put(Shape.BEAM_HOOK, OmrShape.none);
        //        map.put(Shape.BEAM_HOOK_SMALL, OmrShape.none);
        //        map.put(Shape.SLUR, OmrShape.none);
        //        map.put(Shape.KEY_FLAT_7, OmrShape.none);
        //        map.put(Shape.KEY_FLAT_6, OmrShape.none);
        //        map.put(Shape.KEY_FLAT_5, OmrShape.none);
        //        map.put(Shape.KEY_FLAT_4, OmrShape.none);
        //        map.put(Shape.KEY_FLAT_3, OmrShape.none);
        //        map.put(Shape.KEY_FLAT_2, OmrShape.none);
        //        map.put(Shape.KEY_SHARP_2, OmrShape.none);
        //        map.put(Shape.KEY_SHARP_3, OmrShape.none);
        //        map.put(Shape.KEY_SHARP_4, OmrShape.none);
        //        map.put(Shape.KEY_SHARP_5, OmrShape.none);
        //        map.put(Shape.KEY_SHARP_6, OmrShape.none);
        //        map.put(Shape.KEY_SHARP_7, OmrShape.none);
        map.put(Shape.THIN_BARLINE, OmrShape.barlineSingle);
        //        map.put(Shape.THIN_CONNECTOR, OmrShape.none);
        map.put(Shape.THICK_BARLINE, OmrShape.barlineHeavy);
        //        map.put(Shape.THICK_CONNECTOR, OmrShape.none);
        //        map.put(Shape.BRACKET_CONNECTOR, OmrShape.none);
        map.put(Shape.DOUBLE_BARLINE, OmrShape.barlineDouble);
        map.put(Shape.FINAL_BARLINE, OmrShape.barlineFinal);
        map.put(Shape.REVERSE_FINAL_BARLINE, OmrShape.barlineReverseFinal);
        map.put(Shape.LEFT_REPEAT_SIGN, OmrShape.repeatLeft);
        map.put(Shape.RIGHT_REPEAT_SIGN, OmrShape.repeatRight);
        map.put(Shape.BACK_TO_BACK_REPEAT_SIGN, OmrShape.repeatRightLeft);
        //        map.put(Shape.ENDING, OmrShape.none);
        //        map.put(Shape.CRESCENDO, OmrShape.none);
        //        map.put(Shape.DIMINUENDO, OmrShape.none);
        map.put(Shape.BRACE, OmrShape.brace);
        //        map.put(Shape.BRACKET, OmrShape.none);
        map.put(Shape.REPEAT_DOT_PAIR, OmrShape.repeatDots);
        //map.put(Shape.NOISE, OmrShape.none);
        map.put(Shape.LEDGER, OmrShape.legerLine);
        //        map.put(Shape.ENDING_HORIZONTAL, OmrShape.none);
        //        map.put(Shape.ENDING_VERTICAL, OmrShape.none);
        //        map.put(Shape.SEGMENT, OmrShape.none);
        map.put(Shape.STEM, OmrShape.stem);
        map.put(Shape.KEY_FLAT_1, OmrShape.keyFlat);
        map.put(Shape.KEY_SHARP_1, OmrShape.keySharp);
        map.put(Shape.GRACE_NOTE_SLASH, OmrShape.graceNoteAcciaccaturaStemUp);
        map.put(Shape.GRACE_NOTE, OmrShape.graceNoteAppoggiaturaStemUp);
        map.put(Shape.FERMATA, OmrShape.fermataAbove);
        map.put(Shape.FERMATA_BELOW, OmrShape.fermataBelow);

        return map;
    }
}
