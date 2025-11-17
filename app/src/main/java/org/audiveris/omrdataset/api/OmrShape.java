//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         O m r S h a p e                                        //
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
package org.audiveris.omrdataset.api;

import java.util.EnumSet;

/**
 * Class <code>OmrShape</code> is the OMR-Dataset definition of symbol shapes.
 * <p>
 * This is a small subset of the list of symbol names described by SMuFL specification available at
 * <a href="http://www.smufl.org/">http://www.smufl.org/</a>.
 * Symbols are gathered using the same group numbers and names as found in SMuFL specification.
 * <p>
 * We added a few names:
 * <ul>
 * <li><b>keyFlat</b>, <b>keyNatural</b>, <b>keySharp</b>.
 * They represent flat, natural and sharp signs within a key signature.
 * <li>Still to be added: the symbols for drums notation
 * </ul>
 *
 * @see <a href="http://www.smufl.org/">http://www.smufl.org/</a>
 * @see <a href=
 *      "https://w3c.github.io/smufl/latest/tables/index.html">https://w3c.github.io/smufl/latest/tables/index.html</a>
 * @see <a href=
 *      "https://github.com/w3c/smufl/blob/gh-pages/metadata/glyphnames.json">https://github.com/w3c/smufl/blob/gh-pages/metadata/glyphnames.json</a>
 * @author Hervé Bitteur
 */
public enum OmrShape
{
    // 4.1 Staff brackets and dividers
    brace("Brace"),
    bracket("Staff bracket"),

    // 4.2 Staves
    ledgerLine("Ledger line"),

    // 4.3 Barlines
    barlineSingle("Single barline"),
    barlineDouble("Double barline"),
    barlineHeavy("Heavy barline"),

    // 4.4 Repeats
    repeatLeft("Left (start) repeat sign"),
    repeatRight("Right (end) repeat sign"),
    repeatRightLeft("Right and left repeat sign"),
    //    dalSegno("Dal segno (D.S.)"),
    //    daCapo("Da capo (D.C.)"),
    segno("Segno"),
    coda("Coda"),

    // 4.5 Clefs
    gClef("G clef"),
    //    gClef8vb("G clef ottava bassa"),
    //    gClef8va("G clef ottava alta"),
    //    gClef15mb("G clef quindicesima bassa"),
    //    gClef15ma("G clef quindicesima alta"),
    cClefAlto("C clef alto"),
    cClefTenor("C clef tenor"),
    fClef("F clef"),
    //    fClef8vb("F clef ottava bassa"),
    //    fClef8va("F clef ottava alta"),
    //    fClef15mb("F clef quindicesima bassa"),
    //    fClef15ma("F clef quindicesima alta"),
    unpitchedPercussionClef1("Unpitched percussion clef 1"),
    //    gClefChange("G clef change"),
    //    cClefAltoChange("C clef alto change"),
    //    cClefTenorChange("C clef tenor change"),
    //    fClefChange("F clef change"),
    clef8("8 for clefs"),
    clef15("15 for clefs"),

    // 4.6 Time signatures
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
    timeSigCommon("Common time"),
    timeSigCutCommon("Cut time"),

    // 4.7 Noteheads
    noteheadBlack("Black notehead"),
    //    noteheadBlackSmall("Black notehead (small staff)"),
    noteheadHalf("Half (minim) notehead"),
    //    noteheadHalfSmall("Half (minim) notehead (small staff)"),
    noteheadWhole("Whole (semibreve) notehead"),
    //    noteheadWholeSmall("Whole notehead (small staff)"),
    noteheadDoubleWhole("Double whole (breve) notehead"),
    //    noteheadDoubleWholeSmall("Double whole note (breve) (small staff)"),

    // Not yet in YOLO begin
    noteheadXBlack("X notehead black"),
    noteheadXHalf("X notehead half"),
    noteheadXWhole("X notehead whole"),
    noteheadXDoubleWhole("X notehead double whole"),

    noteheadCircleX("notehead circle X"),
    noteheadCircleXHalf("notehead circle X half"),
    noteheadCircleXWhole("notehead circle X whole"),
    noteheadCircleXDoubleWhole("notehead circle X double whole"),

    noteheadDiamondBlack("notehead diamond black"),
    noteheadDiamondHalf("notehead diamond half"),
    noteheadDiamondWhole("notehead diamond whole"),
    noteheadDiamondDoubleWhole("notehead diamond double whole"),

    noteheadTriangleDownBlack("notehead triangle down black"),
    noteheadTriangleDownHalf("notehead triangle down half"),
    noteShapeTriangleUpBlack("note shape triangle up black"),
    noteShapeTriangleUpHalf("note shape triangle up half"),
    // Not yet in YOLO end

    // 4.14
    augmentationDot("Augmentation dot"),

    // 4.15 Stems
    stem("Combining stem"),

    // 4.16 Tremolos
    tremolo1("Combining tremolo 1"),
    tremolo2("Combining tremolo 2"),
    tremolo3("Combining tremolo 3"),
    tremolo4("Combining tremolo 4"),
    tremolo5("Combining tremolo 5"),

    // 4.17 Flags
    flag8thUp("Combining flag 1 (8th) above"),
    //    flag8thUpSmall("Combining flag 1 (8th) above (small staff)"),
    flag8thDown("Combining flag 1 (8th) below"),
    //    flag8thDownSmall("Combining flag 1 (8th) below (small staff)"),
    flag16thUp("Combining flag 2 (16th) above"),
    flag16thDown("Combining flag 2 (16th) below"),
    flag32ndUp("Combining flag 3 (32nd) above"),
    flag32ndDown("Combining flag 3 (32nd) below"),
    flag64thUp("Combining flag 4 (64th) above"),
    flag64thDown("Combining flag 4 (64th) below"),
    flag128thUp("Combining flag 5 (128th) above"),
    flag128thDown("Combining flag 5 (128th) below"),
    //    flag256thUp("Combining flag 6 (256th) above"),
    //    flag256thDown("Combining flag 6 (256th) below"),
    //    flag512thUp("Combining flag 7 (512th) above"),
    //    flag512thDown("Combining flag 7 (512th) below"),
    //    flag1024thUp("Combining flag 8 (1024th) above"),
    //    flag1024thDown("Combining flag 8 (1024th) below"),

    // 4.18 Standard accidentals
    accidentalFlat("Flat"),
    //    accidentalFlatSmall("Flat (for small staves)"),
    accidentalNatural("Natural"),
    //    accidentalNaturalSmall("Natural (for small staves)"),
    accidentalSharp("Sharp"),
    //    accidentalSharpSmall("Sharp (for small staves)"),
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
    //    articTenutoStaccatoAbove("Louré (tenuto-staccato) above"),
    //    articTenutoStaccatoBelow("Louré (tenuto-staccato) below"),

    // 4.40 Holds and pauses
    fermataAbove("Fermata above staff"),
    fermataBelow("Fermata below staff"),
    breathMarkComma("Breath mark (comma)"), // Not yet in YOLO
    caesura("Caesura"),

    // 4.41 Rests
    //    restMaxima("Maxima rest"), // Two longa symbols // Not yet in YOLO
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
    //    rest256th("256th rest"),
    //    rest512th("512th rest"),
    //    rest1024th("1024th rest"),
    restHBar("Multiple measure rest"),

    // 4.42 Bar repeats
    repeat1Bar("Repeat last bar"),
    repeat2Bars("Repeat last two bars"),
    repeat4Bars("Repeat last four bars"),

    // 4.43 Octaves
    ottava("Ottava (8)"),
    quindicesima("Quindicesima (15)"),

    // 4.44 Dynamics
    dynamicRinforzando("Rinforzando (r)"),
    dynamicSforzando("Sforzando (s)"),
    dynamicZ("Z"),
    dynamicNiente("Niente (n)"),

    //    dynamicPPPPPP("pppppp"),
    //    dynamicPPPPP("ppppp"),
    //    dynamicPPPP("pppp"),
    dynamicPPP("Pianississimo"),
    dynamicPP("Pianissimo"),
    dynamicP("Piano"),
    dynamicM("Mezzo"),
    dynamicMP("Mezzo piano"),
    dynamicMF("Mezzo forte"),
    dynamicPF("Piano forte"),
    dynamicF("Forte"),
    dynamicFF("Fortissimo"),
    dynamicFFF("Fortississimo"),
    //    dynamicFFFF("ffff"),
    //    dynamicFFFFF("fffff"),
    //    dynamicFFFFFF("ffffff"),
    dynamicFP("Forte-piano"),
    dynamicFZ("Forzando"),
    dynamicSF("Sforzando"),
    dynamicSFP("Sforzando-piano"),
    dynamicSFPP("Sforzando-pianissimo"),
    dynamicSFZ("Sforzato"),
    dynamicSFZP("Sforzato-piano"),
    dynamicSFFZ("Sforzatissimo"),
    dynamicRF("Rinforzando 1"), // Keep it?
    dynamicRFZ("Rinforzando 2"),
    dynamicCrescendoHairpin("Crescendo"),
    dynamicDiminuendoHairpin("Diminuendo"),

    // 4.46 Common ornaments
    graceNoteAcciaccaturaStemUp("Slashed grace note stem up"),
    graceNoteAppoggiaturaStemUp("Grace note stem up"),
    graceNoteAcciaccaturaStemDown("Slashed grace note stem down"),
    graceNoteAppoggiaturaStemDown("Grace note stem down"),
    ornamentTrill("Trill"),
    ornamentTurn("Turn"),
    ornamentTurnInverted("Inverted turn"),
    ornamentTurnSlash("Turn with slash"),
    ornamentTurnUp("Turn up"),
    ornamentMordent("Mordent"),
    ornamentMordentInverted("Inverted mordent"), // Not yet in YOLO

    // 4.52 String techniques
    stringsDownBow("Down bow"),
    stringsUpBow("Up bow"),

    // 4.53 Plucked techniques
    arpeggiato("Arpeggiato"),

    // 4.55 Keyboard techniques
    keyboardPedalPed("Pedal mark"),
    keyboardPedalUp("Pedal up mark"),

    // 4.75 Tuplets
    tuplet1("Tuplet 1"),
    tuplet2("Tuplet 2"),
    tuplet3("Tuplet 3"),
    tuplet4("Tuplet 4"),
    tuplet5("Tuplet 5"),
    tuplet6("Tuplet 6"),
    tuplet7("Tuplet 7"),
    tuplet8("Tuplet 8"),
    tuplet9("Tuplet 9"),
    tupletBracket("Tuplet bracket"),

    // 4.78 Beams and slurs
    beam("Beam"),
    slur("Slur"),
    tie("Tie"),

    // 4.115 Fingering
    fingering0("Fingering 0 (open string)"),
    fingering1("Fingering 1 (thumb)"),
    fingering2("Fingering 2 (index finger)"),
    fingering3("Fingering 3 (middle finger)"),
    fingering4("Fingering 4 (ring finger)"),
    fingering5("Fingering 5 (little finger)"),

    fingeringPLower("Fingering p (pulgar; right-hand thumb "), // Not yet in YOLO
    fingeringILower("Fingering i (indicio; right-hand index finger for guitar)"), // Not yet in YOLO
    fingeringMLower("Fingering m (medio; right-hand middle finger for guitar)"), // Not yet in YOLO
    fingeringALower("Fingering a (anular; right-hand ring finger for guitar)"), // Not yet in YOLO

    /*
     * //
     * // NOT YET HANDLED symbols (though found in MuseScore input)
     * //
     * unknown("abnormal symbol in MuseScore input"),
     * //
     * bracketedTuplet2("bracketed tuplet 2"),
     * bracketedTuplet3("bracketed tuplet 3"),
     * bracketedTuplet4("bracketed tuplet 4"),
     * bracketedTuplet5("bracketed tuplet 5"),
     * bracketedTuplet6("bracketed tuplet 6"),
     * bracketedTuplet7("bracketed tuplet 7"),
     * bracketedTuplet9("bracketed tuplet 9"),
     * bracketLine("bracket line"),
     * bracketNormal("bracket normal"),
     * bracketSquare("bracket square"),
     * brassMuteClosed("brass mute closed"),
     * brassMuteOpen("brass mute open"),
     * dynamicSforzatoPianissimo("dynamic sforzato pianissimo"),
     * fine("Fine"),
     * graceNote4("grace note 4"),
     * graceNote8("grace note 8"),
     * graceNote8_After("grace note 8 after"),
     * graceNote16("grace note 16"),
     * graceNote16_After("grace note 16 after"),
     * graceNote32("grace note 32"),
     * graceNote32_After("grace note 32 after"),
     * graceNoteSlashStemUp("grace note slash stem up"),
     * guitarFadeIn("guitar fade in"),
     * guitarFadeOut("guitar fade out"),
     * guitarVolumeSwell("guitar volume swell"),
     * luteFingeringRHFirst("lute fingering RH first"),
     * noteheadSlashHorizontalEnds("notehead slash horizontal ends"),
     * noteheadSlashWhiteHalf("notehead slash white half"),
     * noteheadSlashWhiteWhole("notehead slash white whole"),
     * noteShapeDiamondWhite("note shape diamond white"),
     * noteShapeTriangleUpBlack("note shape triangle up black"),
     * ornamentLinePrall("ornament line prall"),
     * ornamentTremblement("ornament tremblement"),
     * toCoda("to coda"),
     * timeSig11("time signature 11"),
     * timeSig13("time signature 13"),
     * timeSig14("time signature 14"),
     * timeSig4over2("4/2 time signature"),
     * timeSig5over2("5/2 time signature"),
     * timeSig6over2("6/2 time signature"),
     * timeSig9over2("9/2 time signature"),
     * timeSig1over4("1/4 time signature"),
     * timeSig7over4("7/4 time signature"),
     * timeSig8over4("8/4 time signature"),
     * timeSig9over4("9/4 time signature"),
     * timeSig14over4("14/4 time signature"),
     * timeSig4over8("4/8 time signature"),
     * timeSig8over8("8/8 time signature"),
     * timeSig11over8("11/8 time signature"),
     * timeSig13over8("13/8 time signature"),
     * tupletBracketStart("tuplet bracket start"),
     * tupletBracketEnd("tuplet bracket end"),
     * wiggleSawtooth("wiggle sawtooth"),
     * wiggleVibratoLargeFaster("wiggle vibrato large faster"),
     * wiggleVibratoLargeSlowest("wiggle vibrato large slowest")
     */
    ;

    private static final EnumSet<OmrShape> BARLINE_SHAPES = EnumSet.of(
            barlineSingle,
            barlineDouble,
            //            barlineFinal,
            //            barlineReverseFinal,
            barlineHeavy
    //            barlineHeavyHeavy,
    //            barlineDashed,
    //            barlineDotted
    );

    private static final EnumSet<OmrShape> IGNORED_SHAPES = EnumSet.of(ledgerLine
    //stem
    );
    ///
    //            bracketedTuplet2,
    //            bracketedTuplet3,
    //            bracketedTuplet4,
    //            bracketedTuplet5,
    //            bracketedTuplet6,
    //            bracketedTuplet7,
    //            bracketedTuplet9,
    //            bracketLine,
    //            bracketNormal,
    //            bracketSquare,
    //            brassMuteClosed,
    //            brassMuteOpen,
    //            dynamicSforzatoPianissimo,
    //            fine,
    //            graceNote4,
    //            graceNote8,
    //            graceNote8_After,
    //            graceNote16,
    //            graceNote16_After,
    //            graceNote32,
    //            graceNote32_After,
    //            graceNoteSlashStemUp,
    //            guitarFadeIn,
    //            guitarFadeOut,
    //            guitarVolumeSwell,
    //            luteFingeringRHFirst,
    //            noteheadCircleX,
    //            noteheadDiamondBlack,
    //            noteheadDiamondHalf,
    //            noteheadDiamondWhole,
    //            noteheadSlashHorizontalEnds,
    //            noteheadSlashWhiteHalf,
    //            noteheadSlashWhiteWhole,
    //            noteheadTriangleDownBlack,
    //            noteShapeDiamondWhite,
    //            noteShapeTriangleUpBlack,
    //            noteShapeTriangleUpWhite,
    //            ornamentLinePrall,
    //            ornamentTremblement,
    //            toCoda,
    //            timeSig11,
    //            timeSig13,
    //            timeSig14,
    //            timeSig4over2,
    //            timeSig5over2,
    //            timeSig6over2,
    //            timeSig9over2,
    //            timeSig1over4,
    //            timeSig7over4,
    //            timeSig8over4,
    //            timeSig9over4,
    //            timeSig14over4,
    //            timeSig4over8,
    //            timeSig8over8,
    //            timeSig11over8,
    //            timeSig13over8,
    //            tuplet2,
    //            tuplet4,
    //            tuplet5,
    //            tuplet7,
    //            tuplet9,
    //            tupletBracketStart,
    //            tupletBracketEnd,
    //            wiggleSawtooth,
    //            wiggleVibratoLargeFaster,
    //            wiggleVibratoLargeSlowest,
    //            //
    //            unknown

    //~ Instance fields ----------------------------------------------------------------------------

    /** Short explanation of the symbol shape. */
    public final String description;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Define a symbol shape
     *
     * @param description textual symbol description
     */
    OmrShape (String description)
    {
        this.description = description;
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report whether this is a barline shape.
     *
     * @return true if so
     */
    public boolean isBarline ()
    {
        return BARLINE_SHAPES.contains(this);
    }

    /**
     * Report whether the shape is to be ignored for standard processing.
     *
     * @return true to ignore
     */
    public boolean isIgnored ()
    {
        return IGNORED_SHAPES.contains(this);
    }
}
