//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S h a p e                                            //
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
package org.audiveris.omr.glyph;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.glyph.ShapeSet.HeadMotif;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.symbol.FontSymbol;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Enum <code>Shape</code> defines the comprehensive enumeration of glyph shapes.
 * <p>
 * The enumeration begins with physical shapes (which are the only ones usable for training of the
 * glyph classifier) and ends with additional (logical) shapes.
 * <p>
 * <b>NOTA</b>: All the physical shapes <b>MUST</b> have different characteristics for the glyph
 * classifier training to work correctly.
 * The same physical shape can lead to different logical shapes according to the context.
 * Three physical shapes are in this case (their name ends with "<i>_set</i>" to make this clear):
 * <ul>
 * <li>Physical DOT_set: only the context can disambiguate between:
 * <ul>
 * <li>an augmentation dot (first or second dot),
 * <li>a part of a repeat sign (upper or lower dot),
 * <li>a staccato sign,
 * <li>a dot of an ending indication,
 * <li>a simple text dot.
 * </ul>
 * </li>
 * <li>Physical HW_REST_set: depending on the precise pitch position within the staff, it can mean:
 * <ul>
 * <li>HALF_REST</li>
 * <li>WHOLE_REST</li>
 * </ul>
 * <li>Physical EIGHTH_set: depending on the context, it can mean:
 * <ul>
 * <li>GRACE_NOTE</li>
 * <li>METRO_EIGHTH</li>
 * </ul>
 * </ul>
 * As far as possible, a display symbol should be generated for every shape.
 * <p>
 * A shape may have a related "decorated" symbol. For example the BREVE_REST is similar to a black
 * rectangle which is used for training / recognition and the related symbol is used for drawing in
 * score view. However, in menu items, it is displayed as a black rectangle surrounded by a staff
 * line above and a staff line below.
 * The method {@link #getDecoratedSymbol(MusicFamily)} returns the symbol to use in menu items.
 *
 * @author Hervé Bitteur
 */
public enum Shape
{
    /**
     * =============================================================================================
     * Beginning of physical shapes, they are recognized by trainable classifiers
     * NOTA: Order of physicals is relevant, its modification would silently impact the classifiers,
     * and you would have to retrain them on your own!
     * =============================================================================================
     */

    //
    // Sets ---
    //
    DOT_set("Dot set"),
    HW_REST_set("Half & Whole Rest set"),
    EIGHTH_set("Grace & beat unit set"),

    //
    // Bars ---
    //
    DAL_SEGNO("D.S.: Repeat from the sign"),
    DA_CAPO("D.C.: Repeat from the beginning"),
    SEGNO("Sign"),
    CODA("Closing section"),
    BREATH_MARK("Breath Mark"),
    CAESURA("Caesura"),
    FERMATA("Fermata arc + dot"),
    FERMATA_BELOW("Fermata below, arc + dot"),
    REPEAT_ONE_BAR("Repeat last bar"),
    REPEAT_TWO_BARS("Repeat last two bars"),
    REPEAT_FOUR_BARS("Repeat last four bars"),

    //
    // Clefs ---
    //
    G_CLEF("Treble Clef"),
    G_CLEF_SMALL("Small Treble Clef"),
    G_CLEF_8VA("Treble Clef Ottava Alta"),
    G_CLEF_8VB("Treble Clef Ottava Bassa"),
    C_CLEF("Ut Clef"),
    F_CLEF("Bass Clef"),
    F_CLEF_SMALL("Small Bass Clef"),
    F_CLEF_8VA("Bass Clef Ottava Alta"),
    F_CLEF_8VB("Bass Clef Ottava Bassa"),
    PERCUSSION_CLEF("Percussion Clef"),
    //CLEF_OTTAVA("Clef 8"), Not handled without its related clef

    //
    // Accidentals ---
    //
    FLAT("Minus one half step"),
    NATURAL("Natural value"),
    SHARP("Plus one half step"),
    DOUBLE_SHARP("Double Sharp"),
    DOUBLE_FLAT("Double Flat"),

    //
    // Time ---
    //
    TIME_ZERO("Time digit 0"),
    TIME_ONE("Time digit 1"),
    TIME_TWO("Time digit 2"),
    TIME_THREE("Time digit 3"),
    TIME_FOUR("Time digit 4"),
    TIME_FIVE("Time digit 5"),
    TIME_SIX("Time digit 6"),
    TIME_SEVEN("Time digit 7"),
    TIME_EIGHT("Time digit 8"),
    TIME_NINE("Time digit 9"),
    TIME_TWELVE("Time number 12"),
    TIME_SIXTEEN("Time number 16"),

    // Whole time sigs
    COMMON_TIME("Alpha = 4/4"),
    CUT_TIME("Semi-Alpha = 2/2"),

    // Predefined time combos
    TIME_FOUR_FOUR("Rational 4/4"),
    TIME_TWO_TWO("Rational 2/2"),
    TIME_TWO_FOUR("Rational 2/4"),
    TIME_THREE_FOUR("Rational 3/4"),
    TIME_FIVE_FOUR("Rational 5/4"),
    TIME_SIX_FOUR("Rational 6/4"),
    TIME_THREE_EIGHT("Rational 3/8"),
    TIME_SIX_EIGHT("Rational 6/8"),
    TIME_TWELVE_EIGHT("Rational 12/8"),

    //
    // Octave shifts ---
    //
    OTTAVA("8"),
    QUINDICESIMA("15"),
    VENTIDUESIMA("22"),

    //
    // Rests ---
    //
    LONG_REST("Rest for 4 measures"),
    BREVE_REST("Rest for 2 measures"),
    QUARTER_REST("Rest for a 1/4"),
    EIGHTH_REST("Rest for a 1/8"),
    ONE_16TH_REST("Rest for a 1/16"),
    ONE_32ND_REST("Rest for a 1/32"),
    ONE_64TH_REST("Rest for a 1/64"),
    ONE_128TH_REST("Rest for a 1/128"),

    //
    // Flags ---
    //
    FLAG_1("Single flag"),
    FLAG_1_DOWN("Single flag down"),
    FLAG_2("Double flag"),
    FLAG_2_DOWN("Double flag down"),
    FLAG_3("Triple flag"),
    FLAG_3_DOWN("Triple flag down"),
    FLAG_4("Quadruple flag"),
    FLAG_4_DOWN("Quadruple flag down"),
    FLAG_5("Quintuple flag"),
    FLAG_5_DOWN("Quintuple flag down"),

    //
    // Small Flags
    //
    SMALL_FLAG("Flag for grace note"),
    SMALL_FLAG_DOWN("Flag for grace note down"),
    SMALL_FLAG_SLASH("Slashed flag for grace note"),
    SMALL_FLAG_SLASH_DOWN("Slashed flag for grace note down"),

    //
    // Grace notes ---
    //
    //GRACE_NOTE("Grace Note with no slash"), // Handled by EIGHTH_set
    GRACE_NOTE_DOWN("Grace Note down with no slash"),
    GRACE_NOTE_SLASH("Grace Note with a slash"),
    GRACE_NOTE_SLASH_DOWN("Grace Note down with a slash"),

    //
    // Notes for metronome indication ---
    //
    METRO_WHOLE("Metronome whole note", Colors.SCORE_PHYSICALS),
    METRO_HALF("Metronome half note", Colors.SCORE_PHYSICALS),
    METRO_QUARTER("Metronome quarter note", Colors.SCORE_PHYSICALS),
    //METRO_EIGHTH("Metronome 8th note"),  // Handled by EIGHTH_set
    METRO_SIXTEENTH("Metronome 16th note", Colors.SCORE_PHYSICALS),
    METRO_DOTTED_HALF("Metronome dotted half note", Colors.SCORE_PHYSICALS),
    METRO_DOTTED_QUARTER("Metronome dotted quarter note", Colors.SCORE_PHYSICALS),
    METRO_DOTTED_EIGHTH("Metronome dotted 8th note", Colors.SCORE_PHYSICALS),
    METRO_DOTTED_SIXTEENTH("Metronome dotted 16th note", Colors.SCORE_PHYSICALS),

    //
    // Articulations ---
    //
    ACCENT,
    TENUTO,
    STACCATISSIMO,
    STRONG_ACCENT("Marcato"),
    ARPEGGIATO,

    //
    // Dynamics ---
    //
    //    DYNAMICS_CHAR_M("m character"),
    //    DYNAMICS_CHAR_R("r character"),
    //    DYNAMICS_CHAR_S("s character"),
    //    DYNAMICS_CHAR_Z("z character"),
    //    DYNAMICS_FFF("Fortississimo"),
    //    DYNAMICS_FZ("Forzando"),
    //    DYNAMICS_PPP("Pianississimo"),
    //    DYNAMICS_RF,
    //    DYNAMICS_RFZ("Rinforzando"),
    //    DYNAMICS_SFFZ,
    //    DYNAMICS_SFP("Subito fortepiano"),
    //    DYNAMICS_SFPP,
    DYNAMICS_P("Piano"),
    DYNAMICS_PP("Pianissimo"),
    DYNAMICS_MP("Mezzo piano"),
    DYNAMICS_F("Forte"),
    DYNAMICS_FF("Fortissimo"),
    DYNAMICS_MF("Mezzo forte"),
    DYNAMICS_FP("FortePiano"),
    DYNAMICS_SF("Subito forte"),
    DYNAMICS_SFZ("Sforzando"),

    //
    // Ornaments ---
    //
    TR("Trill"),
    TURN("Turn"),
    TURN_INVERTED("Inverted Turn"),
    TURN_UP("Turn Up"),
    TURN_SLASH("Turn with a Slash"),
    MORDENT("Lower mordent (with bisecting vertical line)"),
    MORDENT_INVERTED("Upper mordent (no vertical line)"),

    //
    // Tuplets ---
    //
    TUPLET_THREE("3"),
    TUPLET_SIX("6"),
    PEDAL_MARK("Pedal down"),
    PEDAL_UP_MARK("Pedal downup"),

    //
    // Small digits ---
    //
    DIGIT_0("Digit 0"),
    DIGIT_1("Digit 1"),
    DIGIT_2("Digit 2"),
    DIGIT_3("Digit 3"),
    DIGIT_4("Digit 4"),
    DIGIT_5("Digit 5"),
    //    DIGIT_6("Digit 6"),
    //    DIGIT_7("Digit 7"),
    //    DIGIT_8("Digit 8"),
    //    DIGIT_9("Digit 9"),

    //
    // Roman numerals ---
    //
    ROMAN_I("Roman number 1"),
    ROMAN_II("Roman number 2"),
    ROMAN_III("Roman number 3"),
    ROMAN_IV("Roman number 4"),
    ROMAN_V("Roman number 5"),
    ROMAN_VI("Roman number 6"),
    ROMAN_VII("Roman number 7"),
    ROMAN_VIII("Roman number 8"),
    ROMAN_IX("Roman number 9"),
    ROMAN_X("Roman number 10"),
    ROMAN_XI("Roman number 11"),
    ROMAN_XII("Roman number 12"),

    //
    // Plucking ---
    //
    PLUCK_P("Plucking pouce/pulgar/thumb"),
    PLUCK_I("Plucking index/indicio/index"),
    PLUCK_M("Plucking majeur/medio/middle"),
    PLUCK_A("Plucking annulaire/anular/ring"),

    //
    // Percussion playing technique ---
    //
    PLAYING_OPEN("Pict open: o"),
    PLAYING_HALF_OPEN("Pict half-open: ø"),
    PLAYING_CLOSED("Pict closed: +"),

    //
    // Tremolos
    //
    TREMOLO_1("Single tremolo"),
    TREMOLO_2("Double tremolo"),
    TREMOLO_3("Triple tremolo"),

    //
    // Miscellaneous ---
    //
    CLUTTER("Pure clutter", Colors.SHAPE_UNKNOWN),

    /**
     * =============================================================================================
     * End of physical shapes
     * Beginning of logical shapes, their order is irrelevant
     * All head shapes are among them, they are recognized by template matching
     * =============================================================================================
     */

    TEXT("Sequence of letters & spaces"),
    CHARACTER("Any letter"),

    //
    // Shapes based on physical DOT_set ---
    //
    REPEAT_DOT("Repeat dot", DOT_set),
    AUGMENTATION_DOT("Augmentation Dot", DOT_set),
    STACCATO("Staccato dot", DOT_set),

    //
    // Shapes based on physical HW_REST_set ---
    //
    WHOLE_REST("Rest for a 1", HW_REST_set),
    HALF_REST("Rest for a 1/2", HW_REST_set),

    //
    // Shapes based on physical EIGHTH_set ---
    //
    GRACE_NOTE("Grace Note with no slash", EIGHTH_set),
    METRO_EIGHTH("Metronome 8th note", EIGHTH_set),

    //
    // StemLessHeads duration 2 ---
    //
    BREVE("Double Whole"),
    BREVE_SMALL("Small Double Whole"),
    BREVE_CROSS("Double Whole Cross"),
    BREVE_DIAMOND("Double Whole Diamond"),
    BREVE_TRIANGLE_DOWN("Double Whole Triangle Down"),
    BREVE_CIRCLE_X("Double Whole Circle X"),

    //
    // StemLessHeads duration 1 ---
    //
    WHOLE_NOTE("Hollow node head for wholes"),
    WHOLE_NOTE_SMALL("Small hollow node head for grace or cue wholes"),
    WHOLE_NOTE_CROSS("Hollow cross shape note head for unpitched percussion wholes"),
    WHOLE_NOTE_DIAMOND("Hollow diamond-shaped note head for unpitched percussion wholes"),
    WHOLE_NOTE_TRIANGLE_DOWN("Hollow point-down triangle shape for unpitched percussion wholes"),
    WHOLE_NOTE_CIRCLE_X("Stemless circle-x head shape for unpitched percussion wholes"),

    //
    // Noteheads duration 1/2 ---
    //
    NOTEHEAD_VOID("Hollow note head for halves"),
    NOTEHEAD_VOID_SMALL("Small hollow note head for grace or cue"),
    NOTEHEAD_CROSS_VOID("Hollow cross shape note head for unpitched percussion"),
    NOTEHEAD_DIAMOND_VOID("Hollow diamond shape note head for unpitched percussion"),
    NOTEHEAD_TRIANGLE_DOWN_VOID(
            "Hollow point-down triangle shape note head for unpitched percussion"),
    NOTEHEAD_CIRCLE_X_VOID("Hollow circle-x shape note head for unpitched percussion"),

    //
    // Noteheads duration 1/4 ---
    //
    NOTEHEAD_BLACK("Filled note head for quarters and less"),
    NOTEHEAD_BLACK_SMALL("Small filled note head for grace or cue"),
    NOTEHEAD_CROSS("Ghost note with rhythmic value but no discernible pitch"),
    NOTEHEAD_DIAMOND_FILLED("Filled diamond shape note head for unpitched percussion"),
    NOTEHEAD_TRIANGLE_DOWN_FILLED(
            "Filled point-down triangle shape note head for unpitched percussion"),
    NOTEHEAD_CIRCLE_X("Circle-x shape note head for unpitched percussion"),

    //
    // Compound notes ---
    //
    SIXTEENTH_NOTE_UP("Filled head plus its up stem and two flags"),
    DOTTED_SIXTEENTH_NOTE_UP("Filled head plus its up stem, two flag and dot"),
    EIGHTH_NOTE_UP("Filled head plus its up stem and flag"),
    DOTTED_EIGHTH_NOTE_UP("Filled head plus its up stem, flag and dot"),
    QUARTER_NOTE_UP("Filled head plus its up stem"),
    QUARTER_NOTE_DOWN("Filled head plus its down stem"),
    DOTTED_QUARTER_NOTE_UP("Filled head plus its up stem and dot"),
    HALF_NOTE_UP("Hollow head plus its up stem"),
    HALF_NOTE_DOWN("Hollow head plus its down stem"),
    DOTTED_HALF_NOTE_UP("Hollow head plus its up stem and dot"),

    //
    // Beams and slurs ---
    //
    BEAM("Beam between two stems"),
    BEAM_SMALL("Small beam for cue notes"),
    BEAM_HOOK("Hook of a beam attached on one stem"),
    BEAM_HOOK_SMALL("Small hook of a beam for cue notes"),
    SLUR("Slur above or below notes"),
    SLUR_ABOVE("Slur above notes"),
    SLUR_BELOW("Slur below notes"),
    MULTIPLE_REST("Multiple measure rest"),
    MULTIPLE_REST_LEFT("Multiple measure rest left"),
    MULTIPLE_REST_MIDDLE("Multiple measure rest middle"),
    MULTIPLE_REST_RIGHT("Multiple measure rest right"),

    //
    // Key signatures ---
    //
    KEY_FLAT_7("Seven Flats"),
    KEY_FLAT_6("Six Flats"),
    KEY_FLAT_5("Five Flats"),
    KEY_FLAT_4("Four Flats"),
    KEY_FLAT_3("Three Flats"),
    KEY_FLAT_2("Two Flats"),
    KEY_FLAT_1("One Flat"),
    KEY_CANCEL("Key Cancel"),
    KEY_SHARP_1("One Sharp"),
    KEY_SHARP_2("Two Sharps"),
    KEY_SHARP_3("Three Sharps"),
    KEY_SHARP_4("Four Sharps"),
    KEY_SHARP_5("Five Sharps"),
    KEY_SHARP_6("Six Sharps"),
    KEY_SHARP_7("Seven Sharps"),

    //
    // Bars ---
    //
    THIN_BARLINE("Thin bar line"),
    THIN_CONNECTOR("Connector between thin barlines", Colors.SCORE_FRAME),
    THICK_BARLINE("Thick bar line"),
    THICK_CONNECTOR("Connector between thick barlines", Colors.SCORE_FRAME),
    BRACKET_CONNECTOR("Connector between bracket items", Colors.SCORE_FRAME),
    DOUBLE_BARLINE("Double thin bar line"),
    FINAL_BARLINE("Thin / Thick bar line"),
    REVERSE_FINAL_BARLINE("Thick / Thin bar line"),
    LEFT_REPEAT_SIGN("Thick / Thin bar line + Repeat dots"),
    RIGHT_REPEAT_SIGN("Repeat dots + Thin / Thick bar line"),
    BACK_TO_BACK_REPEAT_SIGN("Repeat dots + Thin / Thick / Thin + REPEAT_DOTS"),
    ENDING("Alternate ending"),
    ENDING_WRL("Alternate ending with right leg"),

    //
    // Wedges ---
    //
    CRESCENDO("Crescendo"),
    DIMINUENDO("Diminuendo"),

    //
    // Miscellaneous ---
    //
    BRACE("Brace"),
    BRACKET("Bracket"),
    REPEAT_DOT_PAIR("Pair of repeat dots"),
    NOISE("Too small stuff", Colors.SHAPE_UNKNOWN),
    LEDGER("Ledger"),
    SEGMENT("Wedge or ending segment"),
    LYRICS("Lyrics", Colors.SCORE_LYRICS),
    METRONOME("Text-based notes", Colors.SCORE_PHYSICALS),

    //
    // Stems ---
    //
    STEM("Stem"),
    VERTICAL_SERIF("Vertical serif"),

    //
    // Other stuff ---
    //
    FORWARD("To indicate a forward"),
    NON_DRAGGABLE("Non draggable shape"),
    GLYPH_PART("Part of a larger glyph"),
    NUMBER_CUSTOM("Number defined by user"),
    TIME_CUSTOM("Time signature defined by user"),
    NO_LEGAL_TIME("No Legal Time Shape"),
    BRACKET_UPPER_SERIF("Top serif of a bracket"),
    BRACKET_LOWER_SERIF("Bottom serif of a bracket"),
    STAFF_LINES("5-line staff"),

    //
    // Obsolete, kept for backward compatibility ---
    //
    FLAG_1_UP("OBSOLETE Single flag up"),
    FLAG_2_UP("OBSOLETE Double flag up"),
    FLAG_3_UP("OBSOLETE Triple flag up"),
    FLAG_4_UP("OBSOLETE Quadruple flag up"),
    FLAG_5_UP("OBSOLETE Quintuple flag up"),
    FERMATA_DOT("Fermata dot"),
    FERMATA_ARC("Fermata arc, without dot"),
    FERMATA_ARC_BELOW("Fermata arc below, without dot");

    // =============================================================================================
    // This is the end of shape enumeration
    // =============================================================================================

    private static final Logger logger = LoggerFactory.getLogger(Shape.class);

    /** Last physical shape. */
    public static final Shape LAST_PHYSICAL_SHAPE = CLUTTER;

    /** A comparator based on shape name. */
    public static final Comparator<Shape> alphaComparator = (o1,
                                                             o2) -> o1.name().compareTo(o2.name());

    //~ Instance fields ----------------------------------------------------------------------------

    /** Explanation of the glyph shape. */
    private final String description;

    /** Potential related physical shape. */
    private Shape physicalShape;

    /** Related color. */
    private Color color;

    /** Related color constant. */
    private Constant.Color constantColor;

    //~ Constructors -------------------------------------------------------------------------------

    Shape ()
    {
        this("", null, null);
    }

    Shape (String description)
    {
        this(description, null, null);
    }

    Shape (String description,
           Color color)
    {
        this(description, null, color);
    }

    Shape (String description,
           Shape physicalShape)
    {
        this(description, physicalShape, null);
    }

    Shape (String description,
           Shape physicalShape,
           Color color)
    {
        this.description = description;
        this.physicalShape = physicalShape;
        this.color = color;

        // Create the underlying constant
        constantColor = new Constant.Color(
                getClass().getName(), // Unit
                name() + ".color", // Name
                Constant.Color.encodeColor((color != null) ? color : Color.BLACK),
                "Color for shape " + name());
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------------------//
    // createShapeColor //
    //------------------//
    void createShapeColor (Color color)
    {
        // Assign the shape display color
        if (!constantColor.isSourceValue()) {
            setColor(constantColor.getValue()); // Use the shape specific color
        } else if (this.color == null) {
            setColor(color); // Use the provided (range) default color
        }
    }

    //----------//
    // getColor //
    //----------//
    /**
     * Report the color assigned to the shape, if any.
     *
     * @return the related color, or null
     */
    public Color getColor ()
    {
        return color;
    }

    //--------------------//
    // getDecoratedSymbol //
    //--------------------//
    /**
     * Report the symbol to use for menu items.
     *
     * @param family the selected MusicFont family
     * @return the shape symbol, with decorations if any, perhaps null
     */
    public ShapeSymbol getDecoratedSymbol (MusicFamily family)
    {
        final ShapeSymbol symbol = getSymbol(family);

        if (symbol == null) {
            return null;
        }

        return symbol.getDecoratedVersion();
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report a user-friendly description of this shape.
     *
     * @return the shape description
     */
    public String getDescription ()
    {
        if (description == null) {
            return toString(); // Could be improved
        } else {
            return description;
        }
    }

    //---------------//
    // getFontSymbol //
    //---------------//
    /**
     * Report the couple font/symbol for this shape and the provided music font family.
     * <p>
     * DEFAULT_INTERLINE is used as staff interline.
     *
     * @param family preferred font family
     * @return a non-null FontSymbol structure, populated by the first compatible family if any
     */
    public FontSymbol getFontSymbol (MusicFamily family)
    {
        return getFontSymbolByInterline(family, MusicFont.DEFAULT_INTERLINE);
    }

    //---------------//
    // getFontSymbol //
    //---------------//
    /**
     * Report the couple font/symbol for this shape and the provided music font.
     *
     * @param font preferred font
     * @return a FontSymbol structure, populated by the first compatible font, or null
     */
    public FontSymbol getFontSymbol (MusicFont font)
    {
        ShapeSymbol symbol = font.getSymbol(this);

        while (symbol == null && font.getBackup() != null) {
            font = font.getBackup();
            symbol = font.getSymbol(this);
        }

        if (symbol == null)
            return null;

        return new FontSymbol(font, symbol);
    }

    //--------------------------//
    // getFontSymbolByInterline //
    //--------------------------//
    /**
     * Report the couple font/symbol for this shape and the provided music font family
     * and staff interline.
     *
     * @param family    preferred font family
     * @param interline specified interline value
     * @return a FontSymbol structure, populated by the first compatible family, or null
     */
    public FontSymbol getFontSymbolByInterline (MusicFamily family,
                                                int interline)
    {
        return getFontSymbol(MusicFont.getBaseFont(family, interline));
    }

    //---------------------//
    // getFontSymbolBySize //
    //---------------------//
    /**
     * Report the couple font/symbol for this shape and the provided music font family
     * and desired font point size.
     *
     * @param family    preferred font family
     * @param pointSize specified interline value
     * @return a FontSymbol structure, populated by the first compatible family, or null
     */
    public FontSymbol getFontSymbolBySize (MusicFamily family,
                                           int pointSize)
    {
        return getFontSymbol(MusicFont.getBaseFontBySize(family, pointSize));
    }

    //--------------//
    // getHeadMotif //
    //--------------//
    public HeadMotif getHeadMotif ()
    {
        if (ShapeSet.HeadsOval.contains(this)) {
            return HeadMotif.oval;
        }

        if (ShapeSet.HeadsOvalSmall.contains(this)) {
            return HeadMotif.small;
        }

        if (ShapeSet.HeadsCross.contains(this)) {
            return HeadMotif.cross;
        }

        if (ShapeSet.HeadsDiamond.contains(this)) {
            return HeadMotif.diamond;
        }

        if (ShapeSet.HeadsTriangle.contains(this)) {
            return HeadMotif.triangle;
        }

        if (ShapeSet.HeadsCircle.contains(this)) {
            return HeadMotif.circle;
        }

        return null;
    }

    //-----------------//
    // getNoteDuration //
    //-----------------//
    /**
     * Report the intrinsic duration of the note shape.
     * This is the head or rest duration, regardless of any tuplet, beam/flag or augmentation
     * dot.
     *
     * @return the duration as a rational value, or null if this shape is not a note shape
     */
    public Rational getNoteDuration ()
    {
        return switch (this) {
            case LONG_REST -> new Rational(4, 1);
            case BREVE_REST, BREVE, BREVE_SMALL, BREVE_CROSS, BREVE_DIAMOND, BREVE_TRIANGLE_DOWN, //
                    BREVE_CIRCLE_X //
                    -> Rational.TWO;
            case WHOLE_REST, WHOLE_NOTE, WHOLE_NOTE_SMALL, WHOLE_NOTE_CROSS, WHOLE_NOTE_DIAMOND, //
                    WHOLE_NOTE_TRIANGLE_DOWN, WHOLE_NOTE_CIRCLE_X //
                    -> Rational.ONE;
            case HALF_REST, NOTEHEAD_VOID, NOTEHEAD_VOID_SMALL, NOTEHEAD_CROSS_VOID, //
                    NOTEHEAD_DIAMOND_VOID, NOTEHEAD_TRIANGLE_DOWN_VOID, NOTEHEAD_CIRCLE_X_VOID //
                    -> Rational.HALF;
            case QUARTER_REST, NOTEHEAD_BLACK, NOTEHEAD_BLACK_SMALL, NOTEHEAD_CROSS, //
                    NOTEHEAD_DIAMOND_FILLED, NOTEHEAD_TRIANGLE_DOWN_FILLED, NOTEHEAD_CIRCLE_X //
                    -> Rational.QUARTER;
            case EIGHTH_REST -> new Rational(1, 8);
            case ONE_16TH_REST -> new Rational(1, 16);
            case ONE_32ND_REST -> new Rational(1, 32);
            case ONE_64TH_REST -> new Rational(1, 64);
            case ONE_128TH_REST -> new Rational(1, 128);
            default -> null;
        };
    }

    //------------------//
    // getPhysicalShape //
    //------------------//
    /**
     * Report the shape to use for training or precise drawing.
     *
     * @return the related physical shape, if different
     */
    public Shape getPhysicalShape ()
    {
        if (physicalShape != null) {
            return physicalShape;
        } else {
            return this;
        }
    }

    //---------------//
    // getSlashCount //
    //---------------//
    /**
     * Report the number of slashes in this shape (currently effective on RepeatBars only).
     *
     * @return count of slashes
     */
    public int getSlashCount ()
    {
        return switch (this) {
            case REPEAT_ONE_BAR -> 1;
            case REPEAT_TWO_BARS -> 2;
            case REPEAT_FOUR_BARS -> 4;
            default -> 0;
        };
    }

    //-----------//
    // getSymbol //
    //-----------//
    /**
     * Report the symbol to use for this shape.
     *
     * @param family the selected MusicFont family
     * @return the shape symbol, perhaps null
     */
    public ShapeSymbol getSymbol (MusicFamily family)
    {
        final FontSymbol fs = getFontSymbol(family);

        return (fs != null) ? fs.symbol : null;
    }

    //-------------//
    // isDraggable //
    //-------------//
    /**
     * Report whether this shape can be dragged (in a DnD gesture).
     *
     * @return true if shape can be dragged
     */
    public boolean isDraggable ()
    {
        return !ShapeSet.Undraggables.contains(this);
    }

    //-------------//
    // isFlatBased //
    //-------------//
    /**
     * Check whether the shape is a flat or a key-sig sequence of flats.
     *
     * @return true if flat or flat key sig
     */
    public boolean isFlatBased ()
    {
        return (this == FLAT) || ShapeSet.FlatKeys.contains(this);
    }

    //---------//
    // isGrace //
    //---------//
    /**
     * Check whether the shape is a grace
     *
     * @return true if grace
     */
    public boolean isGrace ()
    {
        return ShapeSet.Graces.contains(this);
    }

    //--------//
    // isHead //
    //--------//
    /**
     * Check whether the shape is a head.
     *
     * @return true if head
     */
    public boolean isHead ()
    {
        return ShapeSet.Heads.contains(this);
    }

    //--------------//
    // isPercussion //
    //--------------//
    /**
     * Check whether the shape represents an un-pitched percussion.
     *
     * @return true if so
     */
    public boolean isPercussion ()
    {
        return ShapeSet.HeadsCross.contains(this) || ShapeSet.HeadsDiamond.contains(this)
                || ShapeSet.HeadsTriangle.contains(this) || ShapeSet.HeadsCircle.contains(this);
    }

    //--------//
    // isRest //
    //--------//
    /**
     * Check whether the shape is a rest.
     *
     * @return true if rest
     */
    public boolean isRest ()
    {
        return ShapeSet.Rests.contains(this);
    }

    //--------------//
    // isSharpBased //
    //--------------//
    /**
     * Check whether the shape is a sharp or a key-sig sequence of sharps.
     *
     * @return true if sharp or sharp key sig
     */
    public boolean isSharpBased ()
    {
        return (this == SHARP) || ShapeSet.SharpKeys.contains(this);
    }

    //-------------//
    // isSmallFlag //
    //-------------//
    /**
     * Check whether the shape is a small flag, meant for cue or grace.
     *
     * @return true if small flag
     */
    public boolean isSmallFlag ()
    {
        return ShapeSet.SmallFlagsUp.contains(this) || ShapeSet.SmallFlagsDown.contains(this);
    }

    //-------------//
    // isSmallHead //
    //-------------//
    /**
     * Check whether the shape is a small note head, meant for cue or grace.
     *
     * @return true if small (black/void/whole/breve)
     */
    public boolean isSmallHead ()
    {
        return ShapeSet.HeadsOvalSmall.contains(this);
    }

    //----------------//
    // isStemLessHead //
    //----------------//
    /**
     * Check whether the shape is a stem-less head, that is whole or breve.
     *
     * @return true if so
     */
    public boolean isStemLessHead ()
    {
        return ShapeSet.StemLessHeads.contains(this);
    }

    //--------//
    // isText //
    //--------//
    /**
     * Check whether the shape is a text (or a simple character).
     *
     * @return true if text or character
     */
    public boolean isText ()
    {
        return (this == TEXT) || (this == CHARACTER);
    }

    //-------------//
    // isTrainable //
    //-------------//
    /**
     * Report whether this shape can be used to train an classifier.
     *
     * @return true if trainable, false otherwise
     */
    public boolean isTrainable ()
    {
        return ordinal() <= LAST_PHYSICAL_SHAPE.ordinal();
    }

    //----------//
    // setColor //
    //----------//
    /**
     * Assign a color for this shape.
     *
     * @param color the display color
     */
    public void setColor (Color color)
    {
        this.color = color;
    }

    //------------------//
    // setConstantColor //
    //------------------//
    /**
     * Define a specific color for the shape.
     *
     * @param color the specified color
     */
    public void setConstantColor (Color color)
    {
        constantColor.setValue(color);
        setColor(color);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-----------------//
    // dumpShapeColors //
    //-----------------//
    /**
     * Dump the color of every shape.
     */
    public static void dumpShapeColors ()
    {
        List<String> names = new ArrayList<>();

        for (Shape shape : Shape.values()) {
            names.add(shape + " " + Constant.Color.encodeColor(shape.getColor()));
        }

        Collections.sort(names);

        for (String str : names) {
            System.out.println(str);
        }
    }
}
