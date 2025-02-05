//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        M u s i c X M L                                         //
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
package org.audiveris.omr.score;

import org.audiveris.omr.glyph.Shape;
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

import jakarta.xml.bind.JAXBElement;

import java.math.BigDecimal;

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
                return switch (location) {
                    case LEFT -> BarStyle.HEAVY_LIGHT;
                    case MIDDLE -> BarStyle.LIGHT_LIGHT; // What else?
                    case RIGHT -> BarStyle.LIGHT_HEAVY;
                };
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

        return switch (shape) {
            case DOT_set, STACCATO -> factory.createArticulationsStaccato(ep);
            case ACCENT -> factory.createArticulationsAccent(ep);
            case STRONG_ACCENT -> {
                // Type for strong accent: either up (^) or down (v)
                // For the time being we recognize only up ones
                final StrongAccent strongAccent = factory.createStrongAccent();
                strongAccent.setType(UpDown.UP);
                yield factory.createArticulationsStrongAccent(strongAccent);
            }
            case TENUTO -> factory.createArticulationsTenuto(ep);
            case STACCATISSIMO -> factory.createArticulationsStaccatissimo(ep);
            case BREATH_MARK -> {
                BreathMark breathMark = factory.createBreathMark();
                breathMark.setValue("comma");
                yield factory.createArticulationsBreathMark(breathMark);
            }
            case CAESURA -> {
                final Caesura caesura = factory.createCaesura();
                yield factory.createArticulationsCaesura(caesura);
            }

            default -> {
                logger.error("Unsupported articulation shape:{}", shape);
                yield null;
            }
        };
    }

    //-------------------//
    // getDynamicsObject //
    //-------------------//
    public static JAXBElement<?> getDynamicsObject (Shape shape)
    {
        final ObjectFactory factory = new ObjectFactory();
        final Empty empty = factory.createEmpty();

        return switch (shape) {
            case DYNAMICS_F -> factory.createDynamicsF(empty);
            case DYNAMICS_FF -> factory.createDynamicsFf(empty);
            //        case DYNAMICS_FFF -> factory.createDynamicsFff(empty);
            //        case DYNAMICS_FFFF -> factory.createDynamicsFfff(empty);
            //        case DYNAMICS_FFFFF -> factory.createDynamicsFffff(empty);
            //        case DYNAMICS_FFFFFF ->factory.createDynamicsFfffff(empty);
            case DYNAMICS_FP -> factory.createDynamicsFp(empty);
            //        case DYNAMICS_FZ -> factory.createDynamicsFz(empty);
            case DYNAMICS_MF -> factory.createDynamicsMf(empty);
            case DYNAMICS_MP -> factory.createDynamicsMp(empty);
            case DYNAMICS_P -> factory.createDynamicsP(empty);
            case DYNAMICS_PP -> factory.createDynamicsPp(empty);
            //        case DYNAMICS_PPP -> factory.createDynamicsPpp(empty);
            //        case DYNAMICS_PPPP -> factory.createDynamicsPppp(empty);
            //        case DYNAMICS_PPPPP -> factory.createDynamicsPpppp(empty);
            //        case DYNAMICS_PPPPPP -> factory.createDynamicsPppppp(empty);
            //        case DYNAMICS_RF -> factory.createDynamicsRf(empty);
            //        case DYNAMICS_RFZ -> factory.createDynamicsRfz(empty);
            case DYNAMICS_SF -> factory.createDynamicsSf(empty);
            //        case DYNAMICS_SFFZ -> factory.createDynamicsSffz(empty);
            //        case DYNAMICS_SFP -> factory.createDynamicsSfp(empty);
            //        case DYNAMICS_SFPP -> factory.createDynamicsSfpp(empty);
            case DYNAMICS_SFZ -> factory.createDynamicsSfz(empty);

            default -> {
                logger.error("Unsupported dynamics shape:{}", shape);
                yield null;
            }
        };
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
        final double dur = 64 * duration.divides(Rational.QUARTER).doubleValue();
        final int index = (int) Math.rint(Math.log(dur) / Math.log(2));

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

        return switch (shape) {
            // NOTA: inverted-mordent in MusicXML refers to a MORDENT
            // while mordent in MusicXML refers to a MORDENT_INVERTED
            case MORDENT -> factory.createOrnamentsInvertedMordent(factory.createMordent());
            case MORDENT_INVERTED -> factory.createOrnamentsMordent(factory.createMordent());

            case TR -> factory.createOrnamentsTrillMark(factory.createEmptyTrillSound());
            case TURN -> factory.createOrnamentsTurn(factory.createHorizontalTurn());
            case TURN_INVERTED -> factory.createOrnamentsInvertedTurn(
                    factory.createHorizontalTurn());
            case TURN_SLASH -> {
                final HorizontalTurn horizontalTurn = factory.createHorizontalTurn();
                horizontalTurn.setSlash(YesNo.YES);
                yield factory.createOrnamentsInvertedTurn(horizontalTurn);
            }
            case TURN_UP -> factory.createOrnamentsVerticalTurn(factory.createEmptyTrillSound());
            case TREMOLO_1, TREMOLO_2, TREMOLO_3 -> {
                final Tremolo tremolo = factory.createTremolo();
                tremolo.setDefaultY(defaultY);
                tremolo.setType(TremoloType.SINGLE);
                tremolo.setValue(TremoloInter.getTremoloValue(shape));
                yield factory.createOrnamentsTremolo(tremolo);
            }

            default -> {
                logger.error("Unsupported ornament shape: {}", shape);
                yield null;
            }
        };
    }

    //----------------//
    // getPauseObject //
    //----------------//
    public static JAXBElement<?> getPauseObject (Shape shape)
    {
        //<!ELEMENT articulations
        //      ((accent | strong-accent | staccato | tenuto |
        //        detached-legato | staccatissimo | spiccato |
        //        scoop | plop | doit | falloff | breath-mark |
        //        caesura | stress | unstress | other-articulation)*)>

        final ObjectFactory factory = new ObjectFactory();

        return switch (shape) {
            case BREATH_MARK -> {
                final BreathMark breathMark = factory.createBreathMark();
                breathMark.setValue("comma");
                yield factory.createArticulationsBreathMark(breathMark);
            }
            case CAESURA -> factory.createArticulationsCaesura(factory.createCaesura());

            default -> {
                logger.error("Unsupported pause shape:{}", shape);
                yield null;
            }
        };
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
     * to Proxymusic <code>KindValue</code> type.
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
     * Convert from Audiveris NoteStep type to Proxymusic NoteStep type.
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
