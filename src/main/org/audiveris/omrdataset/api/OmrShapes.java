//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        O m r S h a p e s                                       //
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
package org.audiveris.omrdataset.api;

import static org.audiveris.omrdataset.api.OmrShape.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Class {@code OmrShapes} complements enum {@link OmrShape} with related features.
 *
 * @author Hervé Bitteur
 */
public abstract class OmrShapes
{

    /** OmrShapes as a list a strings. */
    public static final List<String> NAMES = getNames();

    /** All note heads. */
    public static final EnumSet<OmrShape> HEADS = EnumSet.of(
            noteheadBlack,
            noteheadBlackSmall,
            noteheadHalf,
            noteheadHalfSmall,
            noteheadWhole,
            noteheadWholeSmall,
            noteheadDoubleWhole,
            noteheadDoubleWholeSmall,
            noteheadXBlack,
            noteheadXHalf,
            noteheadXWhole);

    /** Predefined numbers for time signatures. */
    public static final EnumSet<OmrShape> TIME_NUMBERS = EnumSet.of(
            timeSig0,
            timeSig1,
            timeSig2,
            timeSig3,
            timeSig4,
            timeSig5,
            timeSig6,
            timeSig7,
            timeSig8,
            timeSig9,
            timeSig12,
            timeSig16);

    /** Partial numbers for time signatures. */
    public static final EnumSet<OmrShape> TIME_PARTIALS = EnumSet.of(
            timeSig1,
            timeSig2,
            timeSig3,
            timeSig4,
            timeSig5,
            timeSig6,
            timeSig7,
            timeSig8,
            timeSig9,
            timeSig12,
            timeSig16);

    /** Predefined commons for time signatures. */
    public static final EnumSet<OmrShape> TIME_COMMONS = EnumSet.of(
            timeSigCommon,
            timeSigCutCommon);

    /** Predefined combos for time signatures. */
    public static final EnumSet<OmrShape> TIME_COMBOS = EnumSet.of(
            timeSig2over4,
            timeSig2over2,
            timeSig3over2,
            timeSig3over4,
            timeSig3over8,
            timeSig4over4,
            timeSig5over4,
            timeSig5over8,
            timeSig6over4,
            timeSig6over8,
            timeSig7over8,
            timeSig9over8,
            timeSig12over8);

    /** All time signature items. */
    public static final EnumSet<OmrShape> TIMES = EnumSet.noneOf(OmrShape.class);

    static {
        TIMES.addAll(TIME_PARTIALS);
        TIMES.addAll(TIME_COMMONS);
        TIMES.addAll(TIME_COMBOS);
    }

    /** Clef shapes (not including changes, nor 8 and 15 separate entities). */
    public static final EnumSet<OmrShape> CLEFS = EnumSet.of(
            gClef,
            gClef8vb,
            gClef8va,
            gClef15mb,
            gClef15ma,
            cClefAlto,
            cClefTenor,
            fClef,
            fClef8vb,
            fClef8va,
            fClef15mb,
            fClef15ma,
            unpitchedPercussionClef1);

    /** Clef changes (usually smaller). */
    public static final EnumSet<OmrShape> CLEF_CHANGES = EnumSet.of(
            gClefChange,
            cClefAltoChange,
            cClefTenorChange,
            fClefChange);

    /** Key items (including keyNatural). */
    public static final EnumSet<OmrShape> KEY_ALTERS = EnumSet.of(keyFlat, keyNatural, keySharp);

    /** Map of predefined combos to num/den integer pairs. */
    public static final Map<OmrShape, NumDen> COMBO_MAP = buildComboMap();

    /** Articulations. */
    public static final EnumSet<OmrShape> ARTICULATIONS = EnumSet.of(
            articAccentAbove,
            articAccentBelow,
            articStaccatoAbove,
            articStaccatoBelow,
            articTenutoAbove,
            articTenutoBelow,
            articStaccatissimoAbove,
            articStaccatissimoBelow,
            articMarcatoAbove,
            articMarcatoBelow,
            articTenutoStaccatoAbove,
            articTenutoStaccatoBelow);

    /** Flags. */
    public static final EnumSet<OmrShape> FLAGS = EnumSet.of(
            flag8thUp,
            flag8thUpSmall,
            flag16thUp,
            flag32ndUp,
            flag64thUp,
            flag128thUp,
            flag256thUp,
            flag512thUp,
            flag1024thUp,
            flag8thDown,
            flag8thDownSmall,
            flag16thDown,
            flag32ndDown,
            flag64thDown,
            flag128thDown,
            flag256thDown,
            flag512thDown,
            flag1024thDown);

    /** Rests. */
    public static final EnumSet<OmrShape> RESTS = EnumSet.of(
            restMaxima,
            restLonga,
            restDoubleWhole,
            restWhole,
            restHalf,
            restQuarter,
            rest8th,
            rest16th,
            rest32nd,
            rest64th,
            rest128th,
            rest256th,
            rest512th,
            rest1024th,
            restHBar);

    /** Accidentals. */
    public static final EnumSet<OmrShape> ACCIDENTALS = EnumSet.of(
            accidentalFlat,
            accidentalFlatSmall,
            accidentalNatural,
            accidentalNaturalSmall,
            accidentalSharp,
            accidentalSharpSmall,
            accidentalDoubleSharp,
            accidentalDoubleFlat);

    /** Dynamics. */
    public static final EnumSet<OmrShape> DYNAMICS = EnumSet.of(
            dynamicPiano,
            dynamicMezzo,
            dynamicForte,
            dynamicRinforzando,
            dynamicSforzando,
            dynamicZ,
            dynamicNiente,
            dynamicPPPPPP,
            dynamicPPPPP,
            dynamicPPPP,
            dynamicPPP,
            dynamicPP,
            dynamicMP,
            dynamicMF,
            dynamicPF,
            dynamicFF,
            dynamicFFF,
            dynamicFFFF,
            dynamicFFFFF,
            dynamicFFFFFF,
            dynamicFortePiano,
            dynamicForzando,
            dynamicSforzando1,
            dynamicSforzandoPiano,
            dynamicSforzandoPianissimo,
            dynamicSforzato,
            dynamicSforzatoPiano,
            dynamicSforzatoFF,
            dynamicRinforzando1,
            dynamicRinforzando2
    );

    /**
     * Report the list of OmrShape values, to be used by DL4J.
     *
     * @return OmrShape values, as a List
     */
    public static final List<String> getNames ()
    {
        List<String> list = new ArrayList<>();

        for (OmrShape shape : OmrShape.values()) {
            list.add(shape.toString());
        }

        return list;
    }

    /**
     * Report the integer value, if any, conveyed by the provided shape
     *
     * @param shape provided shape
     * @return related value or null
     */
    public static Integer integerValueOf (OmrShape shape)
    {
        switch (shape) {
        case timeSig0:
            return 0;

        case timeSig1:
            return 1;

        case timeSig2:
            return 2;

        case timeSig3:
            return 3;

        case timeSig4:
            return 4;

        case timeSig5:
            return 5;

        case timeSig6:
            return 6;

        case timeSig7:
            return 7;

        case timeSig8:
            return 8;

        case timeSig9:
            return 9;

        case timeSig12:
            return 12;

        case timeSig16:
            return 16;
        }

        return null;
    }

    /**
     * Print out the omrShape ordinal and name.
     */
    public static void printOmrShapes ()
    {
        for (OmrShape shape : OmrShape.values()) {
            System.out.printf("%3d %s%n", shape.ordinal(), shape.toString());
        }
    }

    private static Map<OmrShape, NumDen> buildComboMap ()
    {
        final Map<OmrShape, NumDen> map = new EnumMap<>(OmrShape.class);
        map.put(OmrShape.timeSig2over4, new NumDen(2, 4));
        map.put(OmrShape.timeSig2over2, new NumDen(2, 2));
        map.put(OmrShape.timeSig3over2, new NumDen(3, 2));
        map.put(OmrShape.timeSig3over4, new NumDen(3, 4));
        map.put(OmrShape.timeSig3over8, new NumDen(3, 8));
        map.put(OmrShape.timeSig4over4, new NumDen(4, 4));
        map.put(OmrShape.timeSig5over4, new NumDen(5, 4));
        map.put(OmrShape.timeSig5over8, new NumDen(5, 8));
        map.put(OmrShape.timeSig6over4, new NumDen(6, 4));
        map.put(OmrShape.timeSig6over8, new NumDen(6, 8));
        map.put(OmrShape.timeSig7over8, new NumDen(7, 8));
        map.put(OmrShape.timeSig9over8, new NumDen(9, 8));
        map.put(OmrShape.timeSig12over8, new NumDen(12, 8));

        return map;
    }

    //--------//
    // NumDen //
    //--------//
    /**
     * Handles the numerator and denominator structure as a spair of integer values.
     */
    public static class NumDen
    {

        public final int num;

        public final int den;

        public NumDen (int num,
                       int den)
        {
            this.num = num;
            this.den = den;
        }
    }
}
