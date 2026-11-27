//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S h a p e                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
import org.audiveris.omr.util.ResourceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

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
    DOT_set,
    HW_REST_set,
    EIGHTH_set,

    //
    // Bars ---
    //
    DAL_SEGNO,
    DA_CAPO,
    SEGNO,
    CODA,
    BREATH_MARK,
    CAESURA,
    FERMATA,
    FERMATA_BELOW,
    REPEAT_ONE_BAR,
    REPEAT_TWO_BARS,
    REPEAT_FOUR_BARS,

    //
    // Clefs ---
    //
    G_CLEF,
    G_CLEF_SMALL,
    G_CLEF_8VA,
    G_CLEF_8VB,
    C_CLEF,
    F_CLEF,
    F_CLEF_SMALL,
    F_CLEF_8VA,
    F_CLEF_8VB,
    PERCUSSION_CLEF,
    //CLEF_OTTAVA("Clef 8"), Not handled without its related clef

    //
    // Accidentals ---
    //
    FLAT,
    NATURAL,
    SHARP,
    DOUBLE_SHARP,
    DOUBLE_FLAT,

    //
    // Time ---
    //
    TIME_ZERO,
    TIME_ONE,
    TIME_TWO,
    TIME_THREE,
    TIME_FOUR,
    TIME_FIVE,
    TIME_SIX,
    TIME_SEVEN,
    TIME_EIGHT,
    TIME_NINE,
    TIME_TWELVE,
    TIME_SIXTEEN,

    // Whole time sigs
    COMMON_TIME,
    CUT_TIME,

    // Predefined time combos
    TIME_FOUR_FOUR,
    TIME_TWO_TWO,
    TIME_TWO_FOUR,
    TIME_THREE_FOUR,
    TIME_FIVE_FOUR,
    TIME_SIX_FOUR,
    TIME_THREE_EIGHT,
    TIME_SIX_EIGHT,
    TIME_TWELVE_EIGHT,

    //
    // Octave shifts ---
    //
    OTTAVA,
    QUINDICESIMA,
    VENTIDUESIMA,

    //
    // Rests ---
    //
    LONG_REST,
    BREVE_REST,
    QUARTER_REST,
    EIGHTH_REST,
    ONE_16TH_REST,
    ONE_32ND_REST,
    ONE_64TH_REST,
    ONE_128TH_REST,

    //
    // Flags ---
    //
    FLAG_1,
    FLAG_1_DOWN,
    FLAG_2,
    FLAG_2_DOWN,
    FLAG_3,
    FLAG_3_DOWN,
    FLAG_4,
    FLAG_4_DOWN,
    FLAG_5,
    FLAG_5_DOWN,

    //
    // Small Flags
    //
    SMALL_FLAG,
    SMALL_FLAG_DOWN,
    SMALL_FLAG_SLASH,
    SMALL_FLAG_SLASH_DOWN,

    //
    // Grace notes ---
    //
    //GRACE_NOTE("Grace Note with no slash"), // Handled by EIGHTH_set
    GRACE_NOTE_DOWN,
    GRACE_NOTE_SLASH,
    GRACE_NOTE_SLASH_DOWN,

    //
    // Notes for metronome indication ---
    //
    METRO_WHOLE(Colors.SCORE_PHYSICALS),
    METRO_HALF(Colors.SCORE_PHYSICALS),
    METRO_QUARTER(Colors.SCORE_PHYSICALS),
    //METRO_EIGHTH("Metronome 8th note"),  // Handled by EIGHTH_set
    METRO_SIXTEENTH(Colors.SCORE_PHYSICALS),
    METRO_DOTTED_HALF(Colors.SCORE_PHYSICALS),
    METRO_DOTTED_QUARTER(Colors.SCORE_PHYSICALS),
    METRO_DOTTED_EIGHTH(Colors.SCORE_PHYSICALS),
    METRO_DOTTED_SIXTEENTH(Colors.SCORE_PHYSICALS),

    //
    // Articulations ---
    //
    ACCENT,
    TENUTO,
    STACCATISSIMO,
    STACCATISSIMO_BELOW,
    MARCATO,
    MARCATO_BELOW,
    ARPEGGIATO,

    //
    // Dynamics ---
    //
    //    DYNAMICS_CHAR_M("m character"),
    //    DYNAMICS_CHAR_R("r character"),
    //    DYNAMICS_CHAR_S("s character"),
    //    DYNAMICS_CHAR_Z("z character"),
    //    DYNAMICS_FZ("Forzando"),
    //    DYNAMICS_RF,
    //    DYNAMICS_RFZ("Rinforzando"),
    //    DYNAMICS_SFFZ,
    //    DYNAMICS_SFP("Subito fortepiano"),
    //    DYNAMICS_SFPP,
    DYNAMICS_P,
    DYNAMICS_PP,
    DYNAMICS_PPP,
    DYNAMICS_MP,
    DYNAMICS_F,
    DYNAMICS_FF,
    DYNAMICS_FFF,
    DYNAMICS_MF,
    DYNAMICS_FP,
    DYNAMICS_FZ,
    DYNAMICS_SF,
    DYNAMICS_SFZ,

    //
    // Ornaments ---
    //
    TR,
    TURN,
    TURN_INVERTED,
    TURN_UP,
    TURN_SLASH,
    MORDENT,
    MORDENT_INVERTED,

    //
    // Tuplets ---
    //
    TUPLET_THREE,
    TUPLET_SIX,

    //
    // Techniques ---
    //
    BOW_DOWN,
    BOW_UP,
    PEDAL_MARK,
    PEDAL_UP_MARK,

    //
    // Small digits ---
    //
    DIGIT_0,
    DIGIT_1,
    DIGIT_2,
    DIGIT_3,
    DIGIT_4,
    DIGIT_5,
    //    DIGIT_6("Digit 6"),
    //    DIGIT_7("Digit 7"),
    //    DIGIT_8("Digit 8"),
    //    DIGIT_9("Digit 9"),

    //
    // Roman numerals ---
    //
    ROMAN_I,
    ROMAN_II,
    ROMAN_III,
    ROMAN_IV,
    ROMAN_V,
    ROMAN_VI,
    ROMAN_VII,
    ROMAN_VIII,
    ROMAN_IX,
    ROMAN_X,
    ROMAN_XI,
    ROMAN_XII,

    //
    // Plucking ---
    //
    PLUCK_P,
    PLUCK_I,
    PLUCK_M,
    PLUCK_A,

    //
    // Percussion playing technique ---
    //
    PLAYING_OPEN,
    PLAYING_HALF_OPEN,
    PLAYING_CLOSED,

    //
    // Tremolos
    //
    TREMOLO_1,
    TREMOLO_2,
    TREMOLO_3,

    //
    // Miscellaneous ---
    //
    CLUTTER(Colors.SHAPE_UNKNOWN),

    /**
     * =============================================================================================
     * End of physical shapes
     * Beginning of logical shapes, their order is irrelevant
     * All head shapes are among them, they are recognized by template matching
     * =============================================================================================
     */

    TEXT,
    CHARACTER,

    //
    // Shapes based on physical DOT_set ---
    //
    REPEAT_DOT(DOT_set),
    AUGMENTATION_DOT(DOT_set),
    STACCATO(DOT_set),

    //
    // Shapes based on physical HW_REST_set ---
    //
    WHOLE_REST(HW_REST_set),
    HALF_REST(HW_REST_set),

    //
    // Shapes based on physical EIGHTH_set ---
    //
    GRACE_NOTE(EIGHTH_set),
    METRO_EIGHTH(EIGHTH_set),

    //
    // StemLessHeads duration 2 ---
    //
    BREVE,
    BREVE_SMALL,
    BREVE_CROSS,
    BREVE_DIAMOND,
    BREVE_TRIANGLE_DOWN,
    BREVE_CIRCLE_X,

    //
    // StemLessHeads duration 1 ---
    //
    WHOLE_NOTE,
    WHOLE_NOTE_SMALL,
    WHOLE_NOTE_CROSS,
    WHOLE_NOTE_DIAMOND,
    WHOLE_NOTE_TRIANGLE_DOWN,
    WHOLE_NOTE_CIRCLE_X,

    //
    // Noteheads duration 1/2 ---
    //
    NOTEHEAD_VOID,
    NOTEHEAD_VOID_SMALL,
    NOTEHEAD_CROSS_VOID,
    NOTEHEAD_DIAMOND_VOID,
    NOTEHEAD_TRIANGLE_DOWN_VOID,
    NOTEHEAD_CIRCLE_X_VOID,

    //
    // Noteheads duration 1/4 ---
    //
    NOTEHEAD_BLACK,
    NOTEHEAD_BLACK_SMALL,
    NOTEHEAD_CROSS,
    NOTEHEAD_DIAMOND_FILLED,
    NOTEHEAD_TRIANGLE_DOWN_FILLED,
    NOTEHEAD_CIRCLE_X,

    //
    // Compound notes ---
    //
    SIXTEENTH_NOTE_UP,
    DOTTED_SIXTEENTH_NOTE_UP,
    EIGHTH_NOTE_UP,
    DOTTED_EIGHTH_NOTE_UP,
    QUARTER_NOTE_UP,
    QUARTER_NOTE_DOWN,
    DOTTED_QUARTER_NOTE_UP,
    HALF_NOTE_UP,
    HALF_NOTE_DOWN,
    DOTTED_HALF_NOTE_UP,

    //
    // Beams and slurs ---
    //
    BEAM,
    BEAM_SMALL,
    BEAM_HOOK,
    BEAM_HOOK_SMALL,
    SLUR,
    SLUR_ABOVE,
    SLUR_BELOW,
    MULTIPLE_REST,
    MULTIPLE_REST_LEFT,
    MULTIPLE_REST_MIDDLE,
    MULTIPLE_REST_RIGHT,

    //
    // Key signatures ---
    //
    KEY_FLAT_7,
    KEY_FLAT_6,
    KEY_FLAT_5,
    KEY_FLAT_4,
    KEY_FLAT_3,
    KEY_FLAT_2,
    KEY_FLAT_1,
    KEY_CANCEL,
    KEY_SHARP_1,
    KEY_SHARP_2,
    KEY_SHARP_3,
    KEY_SHARP_4,
    KEY_SHARP_5,
    KEY_SHARP_6,
    KEY_SHARP_7,

    //
    // Bars ---
    //
    DUMMY_BARLINE,
    THIN_BARLINE,
    THIN_CONNECTOR(Colors.SCORE_FRAME),
    THICK_BARLINE,
    THICK_CONNECTOR(Colors.SCORE_FRAME),
    BRACKET_CONNECTOR(Colors.SCORE_FRAME),
    DOUBLE_BARLINE,
    FINAL_BARLINE,
    REVERSE_FINAL_BARLINE,
    LEFT_REPEAT_SIGN,
    RIGHT_REPEAT_SIGN,
    BACK_TO_BACK_REPEAT_SIGN,
    ENDING,
    ENDING_WRL,

    //
    // Wedges ---
    //
    CRESCENDO,
    DIMINUENDO,

    //
    // Miscellaneous ---
    //
    BRACE,
    BRACKET,
    REPEAT_DOT_PAIR,
    NOISE(Colors.SHAPE_UNKNOWN),
    LEDGER,
    SEGMENT,
    LYRICS(Colors.SCORE_LYRICS),
    METRONOME(Colors.SCORE_PHYSICALS),

    //
    // Stems ---
    //
    STEM,
    VERTICAL_SERIF,

    //
    // Other stuff ---
    //
    FORWARD,
    NON_DRAGGABLE,
    GLYPH_PART,
    NUMBER_CUSTOM,
    TIME_CUSTOM,
    NO_LEGAL_TIME,
    BRACKET_UPPER_SERIF,
    BRACKET_LOWER_SERIF,
    STAFF_LINES,

    //
    // Obsolete, kept for backward compatibility ---
    //
    FLAG_1_UP,
    FLAG_2_UP,
    FLAG_3_UP,
    FLAG_4_UP,
    FLAG_5_UP,
    FERMATA_DOT,
    FERMATA_ARC,
    FERMATA_ARC_BELOW,
    STRONG_ACCENT;

    // =============================================================================================
    // This is the end of shape enumeration
    // =============================================================================================

    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Shape.class);

    private static final ResourceBundle resources = ResourceUtil.getBundle(Shape.class);

    /** Last physical shape. */
    public static final Shape LAST_PHYSICAL_SHAPE = CLUTTER;

    /** A comparator based on shape name. */
    public static final Comparator<Shape> byName = (o1,
                                                    o2) -> o1.name().compareTo(o2.name());

    /** The list of trainable shapes, sorted alphabetically. */
    public static final List<Shape> ALPHA_TRAINABLES = buildAlphaTrainables();

    //~ Instance fields ----------------------------------------------------------------------------

    /** Localized name of the shape. */
    private String localizedName;

    /** Localized tip of the shape. */
    private String tip;

    /** Potential related physical shape. */
    private Shape physicalShape;

    /** Related color. */
    private Color color;

    /** Related color constant. */
    private Constant.Color constantColor;

    //~ Constructors -------------------------------------------------------------------------------

    Shape ()
    {
        this(null, null);
    }

    Shape (Color color)
    {
        this(null, color);
    }

    Shape (Shape physicalShape)
    {
        this(physicalShape, null);
    }

    Shape (Shape physicalShape,
           Color color)
    {
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

    //------------------//
    // getLocalizedName //
    //------------------//
    /**
     * Report the localized name of this shape.
     *
     * @return the shape localized name
     */
    public String getLocalizedName ()
    {
        if (localizedName == null) {
            localizedName = resources.getString(name() + ".text");

            if (localizedName == null) {
                localizedName = toString(); // Better than nothing!
            }
        }

        return localizedName;
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

    //--------//
    // getTip //
    //--------//
    /**
     * Report a user-friendly explanation of this shape.
     *
     * @return the shape tip
     */
    public String getTip ()
    {
        if (tip == null) {
            try {
                tip = resources.getString(name() + ".toolTipText");
            } catch (MissingResourceException mre) {
                logger.debug(mre.getMessage());
                tip = ResourceUtil.NO_RESOURCE;
            }
        }

        return (tip != ResourceUtil.NO_RESOURCE) ? tip : null;
    }

    //---------//
    // isAbove //
    //---------//
    /**
     * Report whether this shape is always located above the related staff/head
     *
     * @return true if above
     */
    public boolean isAbove ()
    {
        return switch (this) {
            case CODA, DAL_SEGNO, DA_CAPO, FERMATA, MARCATO, SLUR_ABOVE -> true;
            default -> false;
        };
    }

    //---------//
    // isBelow //
    //---------//
    /**
     * Report whether this shape is always located below the related staff/head
     *
     * @return true if below
     */
    public boolean isBelow ()
    {
        return switch (this) {
            case FERMATA_BELOW, MARCATO_BELOW, PEDAL_MARK, PEDAL_UP_MARK, //
                    STACCATISSIMO_BELOW, SLUR_BELOW -> true;
            default -> false;
        };
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
     * Report whether this shape can be used to train a classifier.
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
        final List<String> names = new ArrayList<>();

        for (Shape shape : Shape.values()) {
            names.add(shape + " " + Constant.Color.encodeColor(shape.getColor()));
        }

        Collections.sort(names);

        for (String str : names) {
            System.out.println(str);
        }
    }

    //----------------------//
    // buildAlphaTrainables //
    //----------------------//
    /**
     * Build the list of trainable shapes, sorted by name.
     *
     * @return the sorted list of trainable shapes
     */
    private static List<Shape> buildAlphaTrainables ()
    {
        final List<Shape> list = new ArrayList<>();

        for (Shape shape : EnumSet.range(Shape.values()[0], LAST_PHYSICAL_SHAPE)) {
            list.add(shape);
        }

        Collections.sort(list, byName);

        return list;
    }
}
