//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        M u s i c X M L                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.score;

import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import org.audiveris.omr.sig.inter.ChordNameInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.OrnamentInter;
import org.audiveris.omr.sig.inter.TremoloInter;
import org.audiveris.proxymusic.AccidentalText;
import org.audiveris.proxymusic.AccidentalValue;
import org.audiveris.proxymusic.BarStyle;
import org.audiveris.proxymusic.BreathMark;
import org.audiveris.proxymusic.Caesura;
import org.audiveris.proxymusic.DegreeTypeValue;
import org.audiveris.proxymusic.Empty;
import org.audiveris.proxymusic.EmptyPlacement;
import org.audiveris.proxymusic.HorizontalTurn;
import org.audiveris.proxymusic.KindValue;
import org.audiveris.proxymusic.ObjectFactory;
import org.audiveris.proxymusic.RightLeftMiddle;
import org.audiveris.proxymusic.Step;
import org.audiveris.proxymusic.StrongAccent;
import org.audiveris.proxymusic.Syllabic;
import org.audiveris.proxymusic.Tremolo;
import org.audiveris.proxymusic.TremoloType;
import org.audiveris.proxymusic.UpDown;
import org.audiveris.proxymusic.YesNo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import javax.xml.bind.JAXBElement;

/**
 * Class <code>MusicXML</code> gathers convenient methods dealing with MusicXML data
 *
 * @author Hervé Bitteur
 */
public abstract class MusicXML
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MusicXML.class);

    /**
     * Names of the various note types used in MusicXML.
     * <p>
     * NOTA: If this array is modified, check method {@link #getNoteTypeName(Rational) accordingly.
     */
    private static final String[] noteTypeNames = new String[]
    {
            "256th",
            "128th",
            "64th",
            "32nd",
            "16th",
            "eighth",
            "quarter",
            "half",
            "whole",
            "breve",
            "long" };

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Not meant to be instantiated.
     */
    private MusicXML ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // accidentalTextOf //
    //------------------//
    public static AccidentalText accidentalTextOf (Shape shape)
    {
        ObjectFactory factory = new ObjectFactory();
        AccidentalText accidentaltext = factory.createAccidentalText();
        accidentaltext.setValue(accidentalValueOf(shape));

        return accidentaltext;
    }

    //-------------------//
    // accidentalValueOf //
    //-------------------//
    public static AccidentalValue accidentalValueOf (Shape shape)
    {
        ///sharp, natural, flat, double-sharp, sharp-sharp, flat-flat
        // But no double-flat ???
        if (shape == Shape.DOUBLE_FLAT) {
            return AccidentalValue.FLAT_FLAT;
        } else {
            return AccidentalValue.valueOf(shape.toString());
        }
    }

    //------------//
    // barStyleOf //
    //------------//
    /**
     * Report the MusicXML bar style for a recognized Barline style
     *
     * @param style    the Audiveris barline style
     * @param location position of barline with respect to containing measure
     * @return the MusicXML bar style
     */
    public static BarStyle barStyleOf (PartBarline.Style style,
                                       RightLeftMiddle location)
    {
        try {
            // Special trick for back-to-back config
            if (style == PartBarline.Style.LIGHT_HEAVY_LIGHT) {
                switch (location) {
                case LEFT:
                    return BarStyle.HEAVY_LIGHT;

                case MIDDLE:
                    return BarStyle.LIGHT_LIGHT; // What else?

                case RIGHT:
                    return BarStyle.LIGHT_HEAVY;
                }
            }

            return BarStyle.valueOf(style.name());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unknown bar style " + style, ex);
        }
    }

    //-----------------------//
    // getArticulationObject //
    //-----------------------//
    public static JAXBElement<?> getArticulationObject (Shape shape)
    {
        //<!ELEMENT articulations
        //      ((accent | strong-accent | staccato | tenuto |
        //        detached-legato | staccatissimo | spiccato |
        //        scoop | plop | doit | falloff | breath-mark |
        //        caesura | stress | unstress | other-articulation)*)>
        ObjectFactory factory = new ObjectFactory();
        EmptyPlacement ep = factory.createEmptyPlacement();

        switch (shape) {
        case DOT_set:
        case STACCATO:
            return factory.createArticulationsStaccato(ep);

        case ACCENT:
            return factory.createArticulationsAccent(ep);

        case STRONG_ACCENT:
            // Type for strong accent: either up (^) or down (v)
            // For the time being we recognize only up ones
            StrongAccent strongAccent = factory.createStrongAccent();

            if (shape == Shape.STRONG_ACCENT) {
                strongAccent.setType(UpDown.UP);
            }

            return factory.createArticulationsStrongAccent(strongAccent);

        case TENUTO:
            return factory.createArticulationsTenuto(ep);

        case STACCATISSIMO:
            return factory.createArticulationsStaccatissimo(ep);

        case BREATH_MARK:
            BreathMark breathMark = factory.createBreathMark();
            breathMark.setValue("comma");

            return factory.createArticulationsBreathMark(breathMark);

        case CAESURA:
            final Caesura caesura = factory.createCaesura();
            return factory.createArticulationsCaesura(caesura);
        }

        logger.error("Unsupported ornament shape:{}", shape);

        return null;
    }

    //-------------------//
    // getDynamicsObject //
    //-------------------//
    public static JAXBElement<?> getDynamicsObject (Shape shape)
    {
        ObjectFactory factory = new ObjectFactory();
        Empty empty = factory.createEmpty();

        switch (shape) {
        case DYNAMICS_F:
            return factory.createDynamicsF(empty);

        case DYNAMICS_FF:
            return factory.createDynamicsFf(empty);

        //        case DYNAMICS_FFF:
        //            return factory.createDynamicsFff(empty);
        //
        //        case DYNAMICS_FFFF :
        //            return factory.createDynamicsFfff(empty);
        //
        //        case DYNAMICS_FFFFF :
        //            return factory.createDynamicsFffff(empty);
        //
        //        case DYNAMICS_FFFFFF :
        //            return factory.createDynamicsFfffff(empty);
        case DYNAMICS_FP:
            return factory.createDynamicsFp(empty);

        //
        //        case DYNAMICS_FZ:
        //            return factory.createDynamicsFz(empty);
        case DYNAMICS_MF:
            return factory.createDynamicsMf(empty);

        case DYNAMICS_MP:
            return factory.createDynamicsMp(empty);

        case DYNAMICS_P:
            return factory.createDynamicsP(empty);

        case DYNAMICS_PP:
            return factory.createDynamicsPp(empty);

        //
        //        case DYNAMICS_PPP:
        //            return factory.createDynamicsPpp(empty);
        //
        //        case DYNAMICS_PPPP :
        //            return factory.createDynamicsPppp(empty);
        //
        //        case DYNAMICS_PPPPP :
        //            return factory.createDynamicsPpppp(empty);
        //
        //        case DYNAMICS_PPPPPP :
        //            return factory.createDynamicsPppppp(empty);
        //        case DYNAMICS_RF:
        //            return factory.createDynamicsRf(empty);
        //
        //        case DYNAMICS_RFZ:
        //            return factory.createDynamicsRfz(empty);
        //
        case DYNAMICS_SF:
            return factory.createDynamicsSf(empty);

        //
        //        case DYNAMICS_SFFZ:
        //            return factory.createDynamicsSffz(empty);
        //
        //        case DYNAMICS_SFP:
        //            return factory.createDynamicsSfp(empty);
        //
        //        case DYNAMICS_SFPP:
        //            return factory.createDynamicsSfpp(empty);
        //
        case DYNAMICS_SFZ:
            return factory.createDynamicsSfz(empty);
        }

        logger.error("Unsupported dynamics shape:{}", shape);

        return null;
    }

    //-----------------//
    // getNoteTypeName //
    //-----------------//
    /**
     * Report the name for the note type
     *
     * @param note the note whose type name is needed
     * @return proper note type name
     */
    public static String getNoteTypeName (AbstractNoteInter note)
    {
        return getNoteTypeName(note.getChord().getDurationSansDotOrTuplet());
    }

    //-----------------//
    // getNoteTypeName //
    //-----------------//
    /**
     * Report the name for the provided duration (no dot, no tuplet)
     *
     * @param duration note duration
     * @return proper note type name
     */
    public static String getNoteTypeName (Rational duration)
    {
        // Since quarter is at index 6 in noteTypeNames, use 2**6 = 64
        double dur = 64 * duration.divides(Rational.QUARTER).doubleValue();
        int index = (int) Math.rint(Math.log(dur) / Math.log(2));

        return noteTypeNames[index];
    }

    //-------------------//
    // getOrnamentObject //
    //-------------------//
    /**
     * Report the JAXB element for the provided ornament.
     *
     * @param ornament the provided ornament
     * @param defaultY ornament vertical position (relevant only for tremolo)
     * @return the populated JAXB element
     */
    public static JAXBElement<?> getOrnamentObject (OrnamentInter ornament,
                                                    BigDecimal defaultY)
    {
        //      (((trill-mark | turn | delayed-turn | shake |
        //         wavy-line | mordent | inverted-mordent |
        //         schleifer | tremolo | other-ornament),
        //         accidental-mark*)*)>

        final ObjectFactory factory = new ObjectFactory();
        final Shape shape = ornament.getShape();

        switch (shape) {
        case MORDENT ->
        {
            //      note that inverted-mordent in MusicXML refers to a MORDENT
            //      and mordent in MusicXML refers to a MORDENT_INVERTED
            return factory.createOrnamentsInvertedMordent(factory.createMordent());
        }

        case MORDENT_INVERTED ->
        {
            return factory.createOrnamentsMordent(factory.createMordent());
        }

        case TR ->
        {
            return factory.createOrnamentsTrillMark(factory.createEmptyTrillSound());
        }

        case TURN ->
        {
            return factory.createOrnamentsTurn(factory.createHorizontalTurn());
        }

        case TURN_INVERTED ->
        {
            return factory.createOrnamentsInvertedTurn(factory.createHorizontalTurn());
        }

        case TURN_SLASH ->
        {
            HorizontalTurn horizontalTurn = factory.createHorizontalTurn();
            horizontalTurn.setSlash(YesNo.YES);

            return factory.createOrnamentsInvertedTurn(horizontalTurn);
        }

        case TURN_UP ->
        {
            return factory.createOrnamentsVerticalTurn(factory.createEmptyTrillSound());
        }

        case TREMOLO_1, TREMOLO_2, TREMOLO_3 ->
        {
            final Tremolo tremolo = factory.createTremolo();
            tremolo.setDefaultY(defaultY);
            tremolo.setType(TremoloType.SINGLE);
            tremolo.setValue(TremoloInter.getTremoloValue(shape));
            return factory.createOrnamentsTremolo(tremolo);
        }
        }

        logger.error("Unsupported ornament shape: {}", shape);

        return null;
    }

    //-------------//
    // getSyllabic //
    //-------------//
    public static Syllabic getSyllabic (LyricItemInter.SyllabicType type)
    {
        return Syllabic.valueOf(type.toString());
    }

    //--------//
    // kindOf //
    //--------//
    /**
     * Converts from Audiveris <code>ChordNameInter.ChordKind.ChordType</code> type
     * to Proxymusic <code>KindValue</code> type
     *
     * @param type Audiveris enum ChordSymbol.ChordType
     * @return Proxymusic enum KindValue
     */
    public static KindValue kindOf (ChordNameInter.ChordKind.ChordType type)
    {
        return KindValue.valueOf(type.toString());
    }

    //--------//
    // stepOf //
    //--------//
    /**
     * Convert from Audiveris NoteStep type to Proxymusic NoteStep type
     *
     * @param step Audiveris enum step
     * @return Proxymusic enum step
     */
    public static Step stepOf (HeadInter.NoteStep step)
    {
        return Step.fromValue(step.toString());
    }

    //--------//
    // typeOf //
    //--------//
    /**
     * Converts from Audiveris <code>ChordNameInter.ChordDegree.DegreeType</code>
     * to Proxymusic <code>DegreeTypeValue</code>.
     *
     * @param type Audiveris enum DegreeType
     * @return Proxymusic enum DegreeTypeValue
     */
    public static DegreeTypeValue typeOf (ChordNameInter.ChordDegree.DegreeType type)
    {
        return DegreeTypeValue.valueOf(type.toString());
    }
}
