//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         O m r S h a p e                                        //
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

/**
 * Class {@code OmrShape} is the OMR-Dataset definition of symbol shapes.
 * <p>
 * This is a small subset of the list of symbol names described by SMuFL specification available at
 * <a href="http://www.smufl.org/">http://www.smufl.org/</a>.
 * Symbols are gathered using the same group numbers and names as found in SMuFL specification.
 * <p>
 * This subset is meant to focus on fixed-shape symbols. Accordingly, all symbols with varying size
 * (hair-pins, slurs, beams, etc) have been left out, their recognition is not based on a symbol
 * classifier.
 * The only exception is the <b>brace</b> symbol, which can vary in size, but we hope that a
 * classifier could recognize the brace center part.
 * <p>
 * We added a few names: <ul>
 * <li><b>none</b>.
 * This is a special name to indicate the absence of any valid symbol.
 * <li><b>arpeggiato</b>.
 * We could find only arpeggiato's ending with an arrow head, either {@code arpeggiatoUp} or
 * {@code arpeggiatoDown}.
 * <li><b>keyFlat</b>, <b>keyNatural</b>, <b>keySharp</b>.
 * They represent flat, natural and sharp signs within a key signature.
 * We hope that full-context training will allow to recognize them as such.
 * </ul>
 * All the barline and repeat symbols are meant to be defined for the staff height only.
 *
 * @see <a href="http://www.smufl.org/">http://www.smufl.org/</a>
 *
 * @author Hervé Bitteur
 */
public enum OmrShape
{
    none("No valid shape"),

    //
    // 4.1 Staff brackets and dividers
    //
    brace("Brace"),
    bracketTop("Bracket top"),
    bracketBottom("Bracket bottom"),

    //
    // 4.2 Staves
    //
    legerLine("Leger line"),
    //
    // 4.3 Barlines
    //
    barlineSingle("Single barline"),
    barlineDouble("Double barline"),
    barlineFinal("Final barline"),
    barlineReverseFinal("Reverse final barline"),
    barlineHeavy("Heavy barline"),
    barlineHeavyHeavy("Heavy double barline"),
    barlineDashed("Dashed barline"),
    barlineDotted("Dotted barline"),

    //
    // 4.4 Repeats
    //
    repeatLeft("Left (start) repeat sign"),
    repeatRight("Right (end) repeat sign"),
    repeatRightLeft("Right and left repeat sign"),
    repeatDots("Repeat dots"),
    repeatDot("Repeat dot"),
    dalSegno("Dal segno (D.S.)"),
    daCapo("Da capo (D.C.)"),
    segno("Segno"),
    coda("Coda"),
    codaSquare("Square coda"),

    //
    // 4.5 Clefs
    //
    gClef("G clef"),
    gClef8vb("G clef ottava bassa"),
    gClef8va("G clef ottava alta"),
    gClef15mb("G clef quindicesima bassa"),
    gClef15ma("G clef quindicesima alta"),
    cClef("C clef"),
    fClef("F clef"),
    fClef8vb("F clef ottava bassa"),
    fClef8va("F clef ottava alta"),
    fClef15mb("F clef quindicesima bassa"),
    fClef15ma("F clef quindicesima alta"),
    unpitchedPercussionClef1("Unpitched percussion clef 1"),
    gClefChange("G clef change"),
    cClefChange("C clef change"),
    fClefChange("F clef change"),
    clef8("8 for clefs"),
    clef15("15 for clefs"),

    //
    // 4.6 Time signatures
    //
    timeSig0("Time signature 0"),
    timeSig1("Time signature 1"),
    timeSig2("Time signature 2"),
    timeSig3("Time signature 3"),
    timeSig4("Time signature 4"),
    timeSig5("Time signature 5"),
    timeSig6("Time signature 6"),
    timeSig7("Time signature 7"),
    timeSig8("Time signature 8"),
    timeSig9("Time signature 9"),
    timeSig12("Time signature 12"),
    timeSig16("Time signature 16"),
    timeSigCommon("Common time"),
    timeSigCutCommon("Cut time"),
    timeSig2over4("2/4 time signature"),
    timeSig2over2("2/2 time signature"),
    timeSig3over2("3/2 time signature"),
    timeSig3over4("3/4 time signature"),
    timeSig3over8("3/8 time signature"),
    timeSig4over4("4/4 time signature"),
    timeSig5over4("5/4 time signature"),
    timeSig5over8("5/8 time signature"),
    timeSig6over4("6/4 time signature"),
    timeSig6over8("6/8 time signature"),
    timeSig7over8("7/8 time signature"),
    timeSig9over8("9/8 time signature"),
    timeSig12over8("12/8 time signature"),

    //
    // 4.7 Noteheads
    //
    noteheadBlack("Black notehead"),
    noteheadBlackSmall("Black notehead (small staff)"),
    noteheadHalf("Half (minim) notehead"),
    noteheadHalfSmall("Half (minim) notehead (small staff)"),
    noteheadWhole("Whole (semibreve) notehead"),
    noteheadWholeSmall("Whole notehead (small staff)"),
    noteheadDoubleWhole("Double whole (breve) notehead"),
    noteheadDoubleWholeSmall("Double whole note (breve) (small staff)"),
    noteheadXBlack("X notehead black"),
    noteheadXHalf("X notehead half"),
    noteheadXWhole("X notehead whole"),
    augmentationDot("Augmentation dot"),
    //
    // 4.15 Stems
    //
    stem("Combining stem"),
    //
    // 4.16 Tremolos
    //
    tremolo1("Combining tremolo 1"),
    tremolo2("Combining tremolo 2"),
    tremolo3("Combining tremolo 3"),
    tremolo4("Combining tremolo 4"),
    tremolo5("Combining tremolo 5"),
    //
    // 4.17 Flags
    //
    flag8thUp("Combining flag 1 (8th) above"),
    flag8thUpSmall("Combining flag 1 (8th) above (small staff)"),
    flag16thUp("Combining flag 2 (16th) above"),
    flag32ndUp("Combining flag 3 (32nd) above"),
    flag64thUp("Combining flag 4 (64th) above"),
    flag128thUp("Combining flag 5 (128th) above"),
    flag256thUp("Combining flag 6 (256th) above"),
    flag512thUp("Combining flag 7 (512th) above"),
    flag1024thUp("Combining flag 8 (1024th) above"),
    flag8thDown("Combining flag 1 (8th) below"),
    flag8thDownSmall("Combining flag 1 (8th) below (small staff)"),
    flag16thDown("Combining flag 2 (16th) below"),
    flag32ndDown("Combining flag 3 (32nd) below"),
    flag64thDown("Combining flag 4 (64th) below"),
    flag128thDown("Combining flag 5 (128th) below"),
    flag256thDown("Combining flag 6 (256th) below"),
    flag512thDown("Combining flag 7 (512th) below"),
    flag1024thDown("Combining flag 8 (1024th) below"),
    //
    // 4.18 Standard accidentals
    //
    accidentalFlat("Flat"),
    accidentalFlatSmall("Flat (for small staves)"),
    accidentalNatural("Natural"),
    accidentalNaturalSmall("Natural (for small staves)"),
    accidentalSharp("Sharp"),
    accidentalSharpSmall("Sharp (for small staves)"),
    accidentalDoubleSharp("Double sharp"),
    accidentalDoubleFlat("Double flat"),

    //
    // 4.18bis Alterations for key signatures (NOTA: this is an addition to SMuFL)
    //
    keyFlat("Flat in key signature"),
    keyNatural("Natural in key signature"),
    keySharp("Sharp in key signature"),
    //
    // 4.39 Articulations
    //
    articAccentAbove("Accent above"),
    articAccentBelow("Accent below"),
    articStaccatoAbove("Staccato above"),
    articStaccatoBelow("Staccato below"),
    articTenutoAbove("Tenuto above"),
    articTenutoBelow("Tenuto below"),
    articStaccatissimoAbove("Staccatissimo above"),
    articStaccatissimoBelow("Staccatissimo below"),
    articMarcatoAbove("Marcato above"),
    articMarcatoBelow("Marcato below"),
    articTenutoStaccatoAbove("Louré (tenuto-staccato) above"),
    articTenutoStaccatoBelow("Louré (tenuto-staccato) below"),
    //
    // 4.40 Holds and pauses
    //
    fermataAbove("Fermata above staff"),
    fermataBelow("Fermata below staff"),
    breathMarkComma("Breath mark (comma)"),
    caesura("Caesura"),
    //
    // 4.41 Rests
    //
    restMaxima("Maxima rest"),
    restLonga("Longa rest"),
    restDoubleWhole("Double whole (breve) rest"),
    restWhole("Whole (semibreve) rest"),
    restHalf("Half (minim) rest"),
    restQuarter("Quarter (crotchet) rest"),
    rest8th("Eighth (quaver) rest"),
    rest16th("16th (semiquaver) rest"),
    rest32nd("32nd (demisemiquaver) rest"),
    rest64th("64th (hemidemisemiquaver) rest"),
    rest128th("128th (semihemidemisemiquaver) rest"),
    rest256th("256th rest"),
    rest512th("512th rest"),
    rest1024th("1024th rest"),
    restHBar("Multiple measure rest"),

    //
    // 4.43 Octaves
    //
    ottavaAlta("Ottava alta (8va)"),
    ottavaBassaVb("Ottava bassa (8vb)"),
    //
    // 4.44 Dynamics
    //
    dynamicPiano("Piano (p)"),
    dynamicMezzo("Mezzo (m)"),
    dynamicForte("Forte (f)"),
    dynamicRinforzando("Rinforzando (r)"),
    dynamicSforzando("Sforzando (s)"),
    dynamicZ("Z"),
    dynamicNiente("Niente (n)"),
    dynamicPPPPPP("pppppp"),
    dynamicPPPPP("ppppp"),
    dynamicPPPP("pppp"),
    dynamicPPP("ppp"),
    dynamicPP("pp"),
    dynamicMP("mp"),
    dynamicMF("mf"),
    dynamicPF("pf"),
    dynamicFF("ff"),
    dynamicFFF("fff"),
    dynamicFFFF("ffff"),
    dynamicFFFFF("fffff"),
    dynamicFFFFFF("ffffff"),
    dynamicFortePiano("Forte-piano (fp)"),
    dynamicForzando("Forzando (fz)"),
    dynamicSforzando1("Sforzando 1 (sf)"),
    dynamicSforzandoPiano("Sforzando-piano (sfp)"),
    dynamicSforzandoPianissimo("Sforzando-pianissimo (sfpp)"),
    dynamicSforzato("Sforzato (sfz)"),
    dynamicSforzatoPiano("Sforzato-piano (sfzp)"),
    dynamicSforzatoFF("Sforzatissimo (sffz)"),
    dynamicRinforzando1("Rinforzando 1 (rf)"),
    dynamicRinforzando2("Rinforzando 2 (rfz)"),

    //
    // 4.46 Common ornaments
    //
    graceNoteAcciaccaturaStemUp("Slashed grace note stem up"),
    graceNoteAppoggiaturaStemUp("Grace note stem up"),
    ornamentTrill("Trill"),
    ornamentTurn("Turn"),
    ornamentTurnInverted("Inverted turn"),
    ornamentTurnSlash("Turn with slash"),
    ornamentTurnUp("Turn up"),
    ornamentMordent("Mordent"),
    ornamentMordentInverted("Inverted mordent"),

    //
    // 4.52 String techniques
    //
    stringsDownBow("Down bow"),
    stringsUpBow("Up bow"),
    //
    // 4.53 Plucked techniques (NOTA: I found only arpeggiato down and up, with an arrow head)
    //
    arpeggiato("Arpeggiato"),
    //
    // 4.55 Keyboard techniques
    //
    keyboardPedalPed("Pedal mark"),
    keyboardPedalUp("Pedal up mark"),
    //
    // 4.75 Tuplets
    //
    tuplet3("Tuplet 3"),
    tuplet6("Tuplet 6"),
    //
    // 4.115 Fingering
    //
    fingering0("Fingering 0 (open string)"),
    fingering1("Fingering 1 (thumb)"),
    fingering2("Fingering 2 (index finger)"),
    fingering3("Fingering 3 (middle finger)"),
    fingering4("Fingering 4 (ring finger)"),
    fingering5("Fingering 5 (little finger)"),
    fingeringPLower("Fingering p (pulgar; right-hand thumb "),
    fingeringILower("Fingering i (indicio; right-hand index finger for guitar)"),
    fingeringMLower("Fingering m (medio; right-hand middle finger for guitar)"),
    fingeringALower("Fingering a (anular; right-hand ring finger for guitar)");

    /** Short explanation of the symbol shape. */
    public final String description;

    /**
     * Define a symbol shape
     *
     * @param description textual symbol description
     */
    OmrShape (String description)
    {
        this.description = description;
    }
}
