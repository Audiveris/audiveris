//----------------------------------------------------------------------------//
//                                                                            //
//                                 S h a p e                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.constant.Constant;

import omr.ui.icon.IconManager;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.*;

import javax.swing.*;

/**
 * Class <code>Shape</code> defines the comprehensive list of glyph shapes, as
 * well as pre-defined ranges (see nested class {@link Range}) of the shapes
 * list (such as rests, heads, etc...) to ease user interactions. It is
 * organized according to the Unicode Standard 4.0, with a few addition for
 * convenience only.
 *
 * <p>As far as possible, an Icon should be generated for every shape.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public enum Shape {
    // =========================================================================
    // Physical shapes, whose physical characteristics can be stored for
    // evaluator training.
    // =========================================================================

    // Garbage -----------------------------------------------------------------
    //
    /** Too small stuff */
    NOISE("Too small stuff"),
    /** Structure of items */
    STRUCTURE("Structure of items"), 
    //
    // Pure physical stuff -----------------------------------------------------
    //
    /** Pure clutter */
    CLUTTER("Pure clutter"), 
    /** General dot shape */
    DOT("General dot shape"), 
    /** General dash shape */
    DASH("General dash shape"), 
    /** A letter */
    CHARACTER("A letter"), 
    /** Sequence of letters & spaces */
    TEXT("Sequence of letters & spaces"), 
    //
    // Bars --------------------------------------------------------------------
    //
    /** Repeat from the sign */
    DAL_SEGNO("Repeat from the sign"), 
    /** Repeat from the beginning */
    DA_CAPO("Repeat from the beginning"), 
    /** Sign */
    SEGNO("Sign"), 
    /** Closing section */
    CODA("Closing section"), 
    //     REPEATED_FIGURE_1,
    //     REPEATED_FIGURE_2,
    //     REPEATED_FIGURE_3,
    /** Fermata */
    FERMATA("Fermata"), 
    /** Fermata Below */
    FERMATA_BELOW("Fermata Below"), 
    /** Breath Mark */
    BREATH_MARK("Breath Mark"), 
    /** Caesura */
    CAESURA("Caesura"), 
    /** Brace */
    BRACE("Brace"), 
    /** Bracket */
    BRACKET("Bracket"), 
    //
    // Staves ------------------------------------------------------------------
    //
    //     ONE_LINE_STAFF,
    //     TWO_LINE_STAFF,
    //     THREE_LINE_STAFF,
    //     FOUR_LINE_STAFF,
    //     FIVE_LINE_STAFF,
    //     SIX_LINE_STAFF,
    //
    // Tablature ---------------------------------------------------------------
    //
    //     SIX_STRING_FRETBOARD,
    //     FOUR_STRING_FRETBOARD,
    //
    // Clefs -------------------------------------------------------------------
    //
    /** Treble Clef */
    G_CLEF("Treble Clef"), 
    /** Ottava Alta */
    G_CLEF_OTTAVA_ALTA("Ottava Alta"), 
    /** Ottava Bassa */
    G_CLEF_OTTAVA_BASSA("Ottava Bassa"), 
    /** Ut Clef */
    C_CLEF("Ut Clef"), 
    /** Bass Clef */
    F_CLEF("Bass Clef"), 
    /** Bass Clef Ottava Alta */
    F_CLEF_OTTAVA_ALTA("Bass Clef Ottava Alta"), 

    /** Bass Clef Ottava Bassa */
    F_CLEF_OTTAVA_BASSA("Bass Clef Ottava Bassa"), 
    //     DRUM_CLEF_1,
    //     DRUM_CLEF_2,
    //
    // Accidentals -------------------------------------------------------------
    //
    /** Minus one half step */
    FLAT("Minus one half step"), 
    /** Natural value */
    NATURAL("Natural value"), 
    /** Plus one half step */
    SHARP("Plus one half step"), 
    /** Double Sharp */
    DOUBLE_SHARP("Double Sharp"), 
    /** Double Flat */
    DOUBLE_FLAT("Double Flat"), 
    //     FLAT_UP,
    //     FLAT_DOWN,
    //     NATURAL_UP,
    //     NATURAL_DOWN,
    //     SHARP_UP,
    //     SHARP_DOWN,
    //     QUARTER_TONE_SHARP,
    //     QUARTER_TONE_FLAT,
    //
    // Time signatures ---------------------------------------------------------
    //
    /** Digit 0 */
    TIME_ZERO("Digit 0"), 
    /** Digit 1 */
    TIME_ONE("Digit 1"), 
    /** Digit 2 */
    TIME_TWO("Digit 2"), 
    /** Digit 3 */
    TIME_THREE("Digit 3"), 
    /** Digit 4 */
    TIME_FOUR("Digit 4"), 
    /** Digit 5 */
    TIME_FIVE("Digit 5"), 
    /** Digit 6 */
    TIME_SIX("Digit 6"), 
    /** Digit 7 */
    TIME_SEVEN("Digit 7"), 
    /** Digit 8 */
    TIME_EIGHT("Digit 8"), 
    /** Digit 9 */
    TIME_NINE("Digit 9"), 
    /** Number 12 */
    TIME_TWELVE("Number 12"), 
    /** Number 16 */
    TIME_SIXTEEN("Number 16"), 
    /** Rational 4/4 */
    TIME_FOUR_FOUR("Rational 4/4"), 
    /** Rational 2/2 */
    TIME_TWO_TWO("Rational 2/2"), 
    /** Rational 2/4 */
    TIME_TWO_FOUR("Rational 2/4"), 
    /** Rational 3/4 */
    TIME_THREE_FOUR("Rational 3/4"), 
    /** Rational 6/8 */
    TIME_SIX_EIGHT("Rational 6/8"), 
    /** Alpha = 4/4 */
    COMMON_TIME("Alpha = 4/4"), 
    /** Semi-Alpha = 2/4 */
    CUT_TIME("Semi-Alpha = 2/4"), 
    //
    // Octaves -----------------------------------------------------------------
    //
    /** 8 va */
    OTTAVA_ALTA("8 va"), 
    /** 8 vb */
    OTTAVA_BASSA("8 vb"), 
    //     QUINDICESIMA_ALTA,
    //     QUINDICESIMA_BASSA,
    //
    // Rests -------------------------------------------------------------------
    //
    /** Rest for multiple measures */
    MULTI_REST("Rest for multiple measures"), 

    /** Same shape for whole or half Rest */
    WHOLE_OR_HALF_REST("Same shape for whole or half Rest"), 

    /** Rest for whole measure */
    //        WHOLE_REST("Rest for whole measure"),
    /** Rest for a 1/2 */
    //        HALF_REST("Rest for a 1/2"),
    /** Rest for a 1/4 */
    QUARTER_REST("Rest for a 1/4"), 
    /** Rest for a 1/8 */
    EIGHTH_REST("Rest for a 1/8"), 
    /** Rest for a 1/16 */
    SIXTEENTH_REST("Rest for a 1/16"), 
    /** Rest for a 1/32 */
    THIRTY_SECOND_REST("Rest for a 1/32"), 
    /** Rest for a 1/64 */
    SIXTY_FOURTH_REST("Rest for a 1/64"), 

    /** Rest for a 1/128 */
    ONE_HUNDRED_TWENTY_EIGHTH_REST("Rest for a 1/128"), 

    //
    // Noteheads ---------------------------------------------------------------
    //
    //     X_NOTEHEAD,
    //     PLUS_NOTEHEAD,
    //     CIRCLE_X_NOTEHEAD,
    //     SQUARE_NOTEHEAD_WHITE,
    //     SQUARE_NOTEHEAD_BLACK,
    //     TRIANGLE_NOTEHEAD_UP_WHITE,
    //     TRIANGLE_NOTEHEAD_UP_BLACK,
    //     TRIANGLE_NOTEHEAD_LEFT_WHITE,
    //     TRIANGLE_NOTEHEAD_LEFT_BLACK,
    //     TRIANGLE_NOTEHEAD_RIGHT_WHITE,
    //     TRIANGLE_NOTEHEAD_RIGHT_BLACK,
    //     TRIANGLE_NOTEHEAD_DOWN_WHITE,
    //     TRIANGLE_NOTEHEAD_DOWN_BLACK,
    //     TRIANGLE_NOTEHEAD_UP_RIGHT_WHITE,
    //     TRIANGLE_NOTEHEAD_UP_RIGHT_BLACK,
    //     MOON_NOTEHEAD_WHITE,
    //     MOON_NOTEHEAD_BLACK,
    //     TRIANGLEROUND_NOTEHEAD_DOWN_WHITE,
    //     TRIANGLEROUND_NOTEHEAD_DOWN_BLACK,
    //     PARENTHESIS_NOTEHEAD,

    /** Hollow node head for halves */
    VOID_NOTEHEAD("Hollow node head for halves"), 

    /** Pack of two hollow node heads for halves */
    VOID_NOTEHEAD_2("Pack of two hollow node heads for halves"), 

    /** Pack of three hollow node heads for halves */
    VOID_NOTEHEAD_3("Pack of three hollow node heads for halves"), 

    /** Filled node head for quarters and less */
    NOTEHEAD_BLACK("Filled node head for quarters and less"), 

    /** Pack of two filled node heads for quarters and less */
    NOTEHEAD_BLACK_2("Pack of two filled node heads for quarters and less"), 

    /** Pack of three filled node heads for quarters and less */
    NOTEHEAD_BLACK_3("Pack of three filled node heads for quarters and less"), 

    //     NULL_NOTEHEAD,
    //     CLUSTER_NOTEHEAD_WHITE,
    //     CLUSTER_NOTEHEAD_BLACK,

    //
    // Notes -------------------------------------------------------------------
    //

    /** Double Whole */
    BREVE("Double Whole"), 
    /** Hollow node head for wholes */
    WHOLE_NOTE("Hollow node head for wholes"), 

    /** Pack of two hollow node heads for wholes */
    WHOLE_NOTE_2("Pack of two hollow node heads for wholes"), 

    /** Pack of three hollow node heads for wholes */
    WHOLE_NOTE_3("Pack of three hollow node heads for wholes"), 

    //     HALF_NOTE, //= 1D157 + 1D165
    //     QUARTER_NOTE, //= 1D158 +1D165
    //     EIGHTH_NOTE, //= 1D15F + 1D16E
    //     SIXTEENTH_NOTE, //= 1D15F + 1D16F
    //     THIRTY_SECOND_NOTE, //= 1D15F + 1D170
    //     SIXTY_FOURTH_NOTE, //= 1D15F + 1D171
    //     ONE_HUNDRED_TWENTY_EIGHTH_NOTE, //= 1D15F + 1D172

    //
    // Tremolos ----------------------------------------------------------------
    //
    //     COMBINING_TREMOLO_1,
    //     COMBINING_TREMOLO_2,
    //     COMBINING_TREMOLO_3,
    //     FINGERED_TREMOLO_1,
    //     FINGERED_TREMOLO_2,
    //     FINGERED_TREMOLO_3,

    //
    // Flags -------------------------------------------------------------------
    //

    /** Single flag down */
    COMBINING_FLAG_1("Single flag down"), 
    /** Double flag down */
    COMBINING_FLAG_2("Double flag down"), 
    /** Triple flag down */
    COMBINING_FLAG_3("Triple flag down"), 

    /** Quadruple flag down */
    COMBINING_FLAG_4("Quadruple flag down"), 

    /** Quintuple flag down */
    COMBINING_FLAG_5("Quintuple flag down"), 

    /** Single flag up */
    COMBINING_FLAG_1_UP("Single flag up"), 

    /** Double flag up */
    COMBINING_FLAG_2_UP("Double flag up"), 

    /** Triple flag up */
    COMBINING_FLAG_3_UP("Triple flag up"), 

    /** Quadruple flag up */
    COMBINING_FLAG_4_UP("Quadruple flag up"), 

    /** Quintuple flag up */
    COMBINING_FLAG_5_UP("Quintuple flag up"), 

    //
    // Connected head and flags ------------------------------------------------
    //

    /** Black notehead with single flag down */
    HEAD_AND_FLAG_1("Black notehead with single flag down"), 

    /** Black notehead with double flag down */
    HEAD_AND_FLAG_2("Black notehead with double flag down"), 

    /** Black notehead with triple flag down */
    HEAD_AND_FLAG_3("Black notehead with triple flag down"), 

    /** Black notehead with quadruple flag down */
    HEAD_AND_FLAG_4("Black notehead with quadruple flag down"), 

    /** Black notehead with quintuple flag down */
    HEAD_AND_FLAG_5("Black notehead with quintuple flag down"), 

    /** Black notehead with single flag up */
    HEAD_AND_FLAG_1_UP("Black notehead with single flag up"), 

    /** Black notehead with double flag up */
    HEAD_AND_FLAG_2_UP("Black notehead with double flag up"), 

    /** Black notehead with triple flag up */
    HEAD_AND_FLAG_3_UP("Black notehead with triple flag up"), 

    /** Black notehead with quadruple flag up */
    HEAD_AND_FLAG_4_UP("Black notehead with quadruple flag up"), 

    /** Black notehead with quintuple flag up */
    HEAD_AND_FLAG_5_UP("Black notehead with quintuple flag up"), 

    //
    // Beams and slurs ---------------------------------------------------------
    //

    /** Beam between two stems */
    BEAM("Beam between two stems"), 
    /** Pack of 2 beams */
    BEAM_2("Pack of 2 beams"), 
    /** Pack of 3 beams */
    BEAM_3("Pack of 3 beams"), 
    /** Hook of a beam attached on one stem */
    BEAM_HOOK("Hook of a beam attached on one stem"), 
    /** Slur tying notes */
    SLUR("Slur tying notes"), 
    //     BEGIN_BEAM,
    //     END_BEAM,
    //     BEGIN_TIE,
    //     END_TIE,
    //     BEGIN_SLUR,
    //     END_SLUR,
    //     BEGIN_PHRASE,
    //     END_PHRASE,

    //
    // Articulation ------------------------------------------------------------
    //
    //     COMBINING_ACCENT,
    //     COMBINING_STACCATO,
    //     COMBINING_TENUTO,
    //     COMBINING_STACCATISSIMO,
    //     COMBINING_MARCATO,
    //     COMBINING_MARCATO_STACCATO,
    //     COMBINING_ACCENT_STACCATO,
    //     COMBINING_LOURE,
    ARPEGGIATO, 
    //     ARPEGGIATO_UP,
    //     ARPEGGIATO_DOWN,
    //     COMBINING_DOIT,
    //     COMBINING_RIP,
    //     COMBINING_FLIP,
    //     COMBINING_SMEAR,
    //     COMBINING_BEND,
    //     COMBINING_DOUBLE_TONGUE,
    //     COMBINING_TRIPLE_TONGUE,
    //     RINFORZANDO,
    //     SUBITO,
    //     Z,

    //
    // Dynamics ----------------------------------------------------------------
    //
    DYNAMICS_CHAR_M, DYNAMICS_CHAR_R, DYNAMICS_CHAR_S,
    DYNAMICS_CHAR_Z,

    //
    DYNAMICS_F,DYNAMICS_FF, DYNAMICS_FFF,
    DYNAMICS_FFFF,
    DYNAMICS_FFFFF,
    DYNAMICS_FFFFFF,
    DYNAMICS_FP,
    DYNAMICS_FZ,
    DYNAMICS_MF,
    DYNAMICS_MP,
    DYNAMICS_P,
    DYNAMICS_PP,
    DYNAMICS_PPP,
    DYNAMICS_PPPP,
    DYNAMICS_PPPPP,
    DYNAMICS_PPPPPP,
    DYNAMICS_RF,
    DYNAMICS_RFZ,
    DYNAMICS_SF,
    DYNAMICS_SFFZ,
    DYNAMICS_SFP,
    DYNAMICS_SFPP,
    DYNAMICS_SFZ,

    //
    CRESCENDO,DECRESCENDO, 
    //
    // Ornaments ---------------------------------------------------------------
    //

    /** Grace Note with a Slash */
    GRACE_NOTE_SLASH("Grace Note with a Slash"),

    /** Grace Note with no Slash */
    GRACE_NOTE_NO_SLASH("Grace Note with no Slash"), TR, TURN,
    INVERTED_TURN,
    TURN_SLASH,
    TURN_UP,
    MORDENT,
    INVERTED_MORDENT,

    // Analytics ---------------------------------------------------------------
    //
    //     HAUPTSTIMME,
    //     NEBENSTIMME,
    //     END_OF_STIMME,
    //     DEGREE_SLASH,

    //
    // Instrumentation ---------------------------------------------------------
    //
    //     COMBINING_DOWN_BOW,
    //     COMBINING_UP_BOW,
    //     COMBINING_HARMONIC,
    //     COMBINING_SNAP_PIZZICATO,

    //
    // Tuplets -----------------------------------------------------------------
    //
    TUPLET_THREE,TUPLET_SIX, 
    //
    // Pedals ------------------------------------------------------------------
    //
    PEDAL_MARK,PEDAL_UP_MARK, 
    //
    // =========================================================================
    // Pure Logical shapes, that cannot be inferred only from their
    // physical characteristics.
    // =========================================================================

    //
    // Bars --------------------------------------------------------------------
    //

    /** Thin bar line */
    THIN_BARLINE("Thin bar line"),

    /** Thick bar line */
    THICK_BARLINE("Thick bar line"),

    /** Double thin bar line */
    DOUBLE_BARLINE("Double thin bar line"), 

    /** Thin / Thick bar line */
    FINAL_BARLINE("Thin / Thick bar line"), 

    /** Thick / Thin bar line */
    REVERSE_FINAL_BARLINE("Thick / Thin bar line"), 

    /** Thick / Thin bar line + REPEAT_DOTS */
    LEFT_REPEAT_SIGN("Thick / Thin bar line + REPEAT_DOTS"), 

    /** REPEAT_DOTS + Thin / Thick bar line */
    RIGHT_REPEAT_SIGN("REPEAT_DOTS + Thin / Thick bar line"),

    /** REPEAT_DOTS + Thin / Thick / Thin + REPEAT_DOTS */
    BACK_TO_BACK_REPEAT_SIGN("REPEAT_DOTS + Thin / Thick / Thin + REPEAT_DOTS"),

    /** Vertical dots */
    REPEAT_DOTS("Vertical dots"), 

    // Augmentation dot

    /** Augmentation Dot */
    COMBINING_AUGMENTATION_DOT("Augmentation Dot"), 
    // Alternate ending indication

    /** Alternate ending */
    ENDING("Alternate ending"), 
    // Miscellaneous
    //

    /** Ledger */
    LEDGER("Ledger"), 
    /** Staff Line */
    STAFF_LINE("Staff Line"), 
    /** Horizontal part of ending */
    ENDING_HORIZONTAL("Horizontal part of ending"), 

    /** Vertical part of ending */
    ENDING_VERTICAL("Vertical part of ending"), 
    // Stems
    //
    COMBINING_STEM, 
    //     COMBINING_SPRECHGESANG_STEM,

    //
    // Key signatures ----------------------------------------------------------
    //

    /** Seven Flats */
    KEY_FLAT_7("Seven Flats"), 
    /** Six Flats */
    KEY_FLAT_6("Six Flats"), 
    /** Five Flats */
    KEY_FLAT_5("Five Flats"), 
    /** Four Flats */
    KEY_FLAT_4("Four Flats"), 
    /** Three Flats */
    KEY_FLAT_3("Three Flats"), 
    /** Two Flats */
    KEY_FLAT_2("Two Flats"), 
    /** One Flat */
    KEY_FLAT_1("One Flat"), 
    /** One Sharp */
    KEY_SHARP_1("One Sharp"), 
    /** Two Sharps */
    KEY_SHARP_2("Two Sharps"), 
    /** Three Sharps */
    KEY_SHARP_3("Three Sharps"), 
    /** Four Sharps */
    KEY_SHARP_4("Four Sharps"), 
    /** Five Sharps */
    KEY_SHARP_5("Five Sharps"), 
    /** Six Sharps */
    KEY_SHARP_6("Six Sharps"), 
    /** Seven Sharps */
    KEY_SHARP_7("Seven Sharps"), 

    //
    // Rests -------------------------------------------------------------------
    //

    /** Rest for whole measure */
    WHOLE_REST("Rest for whole measure", WHOLE_OR_HALF_REST), 

    /** Rest for a 1/2 */
    HALF_REST("Rest for a 1/2", WHOLE_OR_HALF_REST), 
    /** This shape is a kludge to get proper icon, to be improved */
    MULTI_REST_DISPLAY, FORWARD, 
    /**
     * Specific value, meaning that we have not been able to determine a
     * legal shape
     */

    /** No Legal Shape */
    NO_LEGAL_SHAPE("No Legal Shape");
    //
    // =========================================================================
    /**
     * First physical shape an evaluator should be able to recognize based on
     * their physical characteristics. For example a DOT is a DOT. Also, a DOT
     * plus a FERMATA_BEND together can compose a FERMATA.
     */
    public static final Shape FirstPhysicalShape = NOISE;

    /**
     * Last physical shape.
     */
    public static final Shape LastPhysicalShape = PEDAL_UP_MARK;

    /**
     * First logical shape, that are more precisely assigned.
     */
    public static final Shape FirstLogicalShape = THIN_BARLINE;

    /** Color for unknown shape */
    public static final Color missedColor = Color.red;

    /** Color for glyphs tested as OK (color used temporarily) */
    public static final Color okColor = Color.green;

    /** Explanation of the glyph shape */
    private final String description;

    /** Related temporary display color */
    private Color color;

    /** Related permanent display color */
    private Constant.Color constantColor;

    /** Potential related icon */
    private Icon icon;

    /** Potential related shape to be used for training */
    private Shape trainingShape;

    /** Remember the fact that this shape has no related icon */
    private boolean hasNoIcon;

    //-------//
    // Shape //
    //-------//
    Shape ()
    {
        this("", null); // No param
    }

    //-------//
    // Shape //
    //-------//
    Shape (String description)
    {
        this(description, null);
    }

    //-------//
    // Shape //
    //-------//
    Shape (String description,
           Shape  trainingShape)
    {
        this.description = description;
        this.trainingShape = trainingShape;
    }

    /**
     * Report whether the impact of this shape persists across system (actually
     * measure) borders (clefs, time signatures, key signatures).
     * Based on just the shape, we cannot tell whether an accidental is part of
     * a key signature or not, so we take a conservative approach.
     *
     * @return true if persistent, false otherwise
     */
    public boolean isPersistent ()
    {
        return Clefs.contains(this) || Times.contains(this) ||
               Accidentals.contains(this);
    }

    //-------------//
    // isTrainable //
    //-------------//
    /**
     * Report whether this shape can be used to train an evaluator
     *
     * @return true if trainable, false otherwise
     */
    public boolean isTrainable ()
    {
        return ordinal() <= LastPhysicalShape.ordinal();
    }

    //-------------//
    // isWellKnown //
    //-------------//
    /**
     * Report whether this shape is well known, that is a symbol not part of the
     * Garbage range
     *
     * @return true if not part of garbage range, false otherwise
     */
    public boolean isWellKnown ()
    {
        return (this != NO_LEGAL_SHAPE) && !Garbage.getShapes()
                                                   .contains(this);
    }

    //-------------//
    // isWholeRest //
    //-------------//
    /**
     * Check whether the shape is a whole (or multi) rest, for which no duration
     * can be specified
     *
     * @return true if whole or multi rest
     */
    public boolean isWholeRest ()
    {
        return (this == WHOLE_REST) || (this == MULTI_REST);
    }

    //--------//
    // isText //
    //--------//
    /**
     * Check whether the shape is a text or a character
     *
     * @return true if text or character
     */
    public boolean isText ()
    {
        return (this == TEXT) || (this == CHARACTER);
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report a user-friendly description of this shape
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

    //----------//
    // getColor //
    //----------//
    /**
     * Report the color currently assigned to the shape, if any
     *
     * @return the related color, or null
     */
    public java.awt.Color getColor ()
    {
        return color;
    }

    //----------//
    // setColor //
    //----------//
    /**
     * Assign a color for current display. This is the specific shape color if
     * any, otherwise it is the default color of the containing range.
     *
     * @param color the display color
     */
    public void setColor (java.awt.Color color)
    {
        this.color = color;
    }

    //------------------//
    // createShapeColor //
    //------------------//
    private void createShapeColor (Color color)
    {
        // Create the underlying constant
        constantColor = new Constant.Color(
            getClass().getName(), // Unit
            name() + ".color", // Name
            "#000000", // DefaultValue: Black
            "Color code for shape " + name());

        // Assign the shape display color
        if (!constantColor.isDefaultValue()) {
            // Use the shape specific color
            setColor(constantColor.getValue());
        } else {
            // Use the provided default color
            setColor(color);
        }
    }

    //------------------//
    // getConstantColor //
    //------------------//
    /**
     * Report the color that is specifically assigned to this shape, if any
     *
     * @return the specific color if any, null otherwise
     */
    public Color getConstantColor ()
    {
        return constantColor.getValue();
    }

    //------------------//
    // setConstantColor //
    //------------------//
    /**
     * Assign a specific color to the shape
     *
     * @param color the color to be assigned
     */
    public void setConstantColor (java.awt.Color color)
    {
        constantColor.setValue(color);
        setColor(color);
    }

    //--------------------//
    // resetConstantColor //
    //--------------------//
    /**
     * Remove the shape specific color, and reset the shape color using the
     * provided color (typically the range default color)
     *
     * @param color the default color
     */
    public void resetConstantColor (Color color)
    {
        constantColor.remove();
        createShapeColor(color); // Use range color !!!
    }

    //---------//
    // getIcon //
    //---------//
    /**
     * Report the icon related to the shape, if any
     *
     * @return the related icon, or null
     */
    public Icon getIcon ()
    {
        if (hasNoIcon) {
            return null;
        }

        if (icon == null) {
            setIcon(IconManager.getInstance().loadSymbolIcon(toString()));

            if (icon == null) {
                hasNoIcon = true;
            }
        }

        return icon;
    }

    //---------//
    // setIcon //
    //---------//
    /**
     * Assign a font to this shape
     *
     * @param icon the assigned icon, which may be null
     */
    public void setIcon (Icon icon)
    {
        this.icon = icon;
    }

    //------------------//
    // getTrainingShape //
    //------------------//
    public Shape getTrainingShape ()
    {
        if (trainingShape != null) {
            return trainingShape;
        } else {
            return this;
        }
    }

    //--------------------//
    // addRangeShapeItems //
    //--------------------//
    /**
     * Populate the given menu with a list of all shapes that belong to the
     * given range
     *
     * @param range the range for which shape menu items must be buit
     * @param top the JComponent to populate (typically a JMenu or a JPopupMenu)
     * @param listener the listener for notification of user selection
     */
    public static void addRangeShapeItems (Range          range,
                                           JComponent     top,
                                           ActionListener listener)
    {
        // All shapes in the given range
        for (Shape shape : range.shapes) {
            JMenuItem menuItem = new JMenuItem(
                shape.toString(),
                shape.getIcon());
            addColoredItem(top, menuItem, shape.getColor());

            menuItem.setToolTipText(shape.description);
            menuItem.addActionListener(listener);
        }
    }

    //---------------//
    // addShapeItems //
    //---------------//
    /**
     * Populate the given menu with a hierarchy of all shapes
     *
     * @param top the JComponent to populate (typically a JMenu or a JPopupMenu)
     * @param listener the listener for notification of user selection
     */
    public static void addShapeItems (JComponent     top,
                                      ActionListener listener)
    {
        // All ranges of glyph shapes
        for (Field field : Shape.class.getDeclaredFields()) {
            if (field.getType() == Range.class) {
                JMenu menu = new JMenu(field.getName());
                Range range = Range.valueOf(field.getName());
                addColoredItem(top, menu, Color.black);

                // Add menu items for this range
                addRangeShapeItems(range, menu, listener);
            }
        }
    }

    //----------------//
    // addColoredItem //
    //----------------//
    private static void addColoredItem (JComponent top,
                                        JMenuItem  item,
                                        Color      color)
    {
        if (color != null) {
            item.setForeground(color);
        } else {
            item.setForeground(Color.black);
        }

        top.add(item);
    }

    //-------//
    // Range //
    //-------//
    /**
     * Class <code>Range</code> defines a range of related shapes, for example
     * the "Rests" range gathers all rest shapes from MULTI_REST down to
     * ONE_HUNDRED_TWENTY_EIGHTH_REST.  It handles additional properties over a
     * simple EnumSet, especially assigned colors and its automatic insertion in
     * shape menu hierarchy.
     */
    public static class Range
    {
        private String               name; // Name of the range
        private final EnumSet<Shape> shapes; // Contained shapes
        private Color                color; // For current color
        private Constant.Color       constantColor; // For specific color

        //-------//
        // Range //
        //-------//
        /**
         * Create a Range from an EnumSet of Shapes
         *
         * @param shapes the contained shapes
         */
        public Range (EnumSet<Shape> shapes)
        {
            this.shapes = shapes;
        }

        //-----------//
        // getShapes //
        //-----------//
        /**
         * Exports the set of shapes in the range
         *
         * @return the proper enum set
         */
        public EnumSet<Shape> getShapes ()
        {
            return shapes;
        }

        //----------//
        // contains //
        //----------//
        /**
         * Convenient method to check if encapsulated shapes set does contain
         * the provided object
         *
         * @return true if contained, false otherwise
         */
        public boolean contains (Object obj)
        {
            return shapes.contains(obj);
        }

        //----------//
        // getColor //
        //----------//
        /**
         * Report the color currently assigned to the range, if any
         *
         * @return the related color, or null
         */
        public java.awt.Color getColor ()
        {
            return color;
        }

        //----------//
        // setColor //
        //----------//
        /**
         * Assign a display color to the shape range
         *
         * @param color the display color
         */
        public void setColor (java.awt.Color color)
        {
            this.color = color;
        }

        //------------------//
        // setConstantColor //
        //------------------//
        /**
         * Define a specific color for the range
         *
         * @param color the specified color
         */
        public void setConstantColor (java.awt.Color color)
        {
            constantColor.setValue(color);
            setColor(color);
        }

        //---------//
        // valueOf //
        //---------//
        /**
         * Retrieve a range knowing its name (just like an enumeration)
         *
         * @param str the provided range name
         * @return the range found, or null otherwise
         */
        public static Range valueOf (String str)
        {
            return Ranges.map.get(str);
        }

        //--------//
        // Ranges //
        //--------//
        /** Build the range map in a lazy way */
        private static class Ranges
        {
            public static Map<String, Range> map = new HashMap<String, Range>();

            static {
                for (Field field : Shape.class.getDeclaredFields()) {
                    if (field.getType() == Range.class) {
                        try {
                            Range range = (Range) field.get(null);
                            map.put(field.getName(), range);
                        } catch (IllegalAccessException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }

        //---------------//
        // addRangeItems //
        //---------------//
        /**
         * Populate the given menu with a hierarchy of all ranges
         *
         * @param top the JComponent to populate (typically a JMenu or a
         * JPopupMenu)
         * @param listener the listener for notification of user selection
         */
        public static void addRangeItems (JComponent     top,
                                          ActionListener listener)
        {
            // All ranges of glyph shapes
            for (Field field : Shape.class.getDeclaredFields()) {
                if (field.getType() == Range.class) {
                    JMenuItem menuItem = new JMenuItem(field.getName());
                    Range     range = valueOf(field.getName());
                    addColoredItem(top, menuItem, range.getColor());

                    menuItem.addActionListener(listener);
                }
            }
        }

        //---------//
        // getName //
        //---------//
        /**
         * Report the name of the range
         *
         * @return the range name
         */
        public String getName ()
        {
            return name;
        }

        //---------//
        // setName //
        //---------//
        private void setName (String name)
        {
            this.name = name;

            constantColor = new Constant.Color(
                getClass().getName(), // Unit
                name + ".color", // Name
                "#000000", // DefaultValue: black
                "Color code for range " + name);

            // Assign the range display color
            if (!constantColor.isDefaultValue()) {
                // Use the range specific color
                setColor(constantColor.getValue());
            } else {
                // Use the default color
                setColor(Color.black);
            }
        }
    }

    // Predefined shape ranges
    //
    public static final Range Garbage = new Range(
        EnumSet.range(NOISE, STRUCTURE));
    public static final Range Physicals = new Range(
        EnumSet.range(CLUTTER, TEXT));

    //
    public static final Range Bars = new Range(
        EnumSet.range(DAL_SEGNO, BRACKET));
    public static final Range Clefs = new Range(
        EnumSet.range(G_CLEF, F_CLEF_OTTAVA_BASSA));
    public static final Range Accidentals = new Range(
        EnumSet.range(FLAT, DOUBLE_FLAT));
    public static final Range Times = new Range(
        EnumSet.range(TIME_ZERO, CUT_TIME));
    public static final Range Octaves = new Range(
        EnumSet.range(OTTAVA_ALTA, OTTAVA_BASSA));
    public static final Range Rests = new Range(
        EnumSet.of(
            MULTI_REST,
            WHOLE_REST,
            HALF_REST,
            QUARTER_REST,
            EIGHTH_REST,
            SIXTEENTH_REST,
            THIRTY_SECOND_REST,
            SIXTY_FOURTH_REST,
            ONE_HUNDRED_TWENTY_EIGHTH_REST));
    public static final Range NoteHeads = new Range(
        EnumSet.range(VOID_NOTEHEAD, NOTEHEAD_BLACK_3));
    public static final Range Notes = new Range(
        EnumSet.range(BREVE, WHOLE_NOTE_3));
    public static final Range Stems = new Range(
        EnumSet.range(COMBINING_STEM, COMBINING_STEM));
    public static final Range Flags = new Range(
        EnumSet.range(COMBINING_FLAG_1, COMBINING_FLAG_5_UP));
    public static final Range HeadAndFlags = new Range(
        EnumSet.range(HEAD_AND_FLAG_1, HEAD_AND_FLAG_5_UP));
    public static final Range Beams = new Range(EnumSet.range(BEAM, SLUR));
    public static final Range Articulations = new Range(
        EnumSet.range(ARPEGGIATO, ARPEGGIATO));
    public static final Range Dynamics = new Range(
        EnumSet.range(DYNAMICS_CHAR_M, DECRESCENDO));
    public static final Range Ornaments = new Range(
        EnumSet.range(GRACE_NOTE_SLASH, INVERTED_MORDENT));
    public static final Range Tuplets = new Range(
        EnumSet.range(TUPLET_THREE, TUPLET_SIX));
    public static final Range Pedals = new Range(
        EnumSet.range(PEDAL_MARK, PEDAL_UP_MARK));

    //
    public static final Range Barlines = new Range(
        EnumSet.range(THIN_BARLINE, BACK_TO_BACK_REPEAT_SIGN));
    public static final Range Logicals = new Range(
        EnumSet.range(REPEAT_DOTS, ENDING));

    static {
        for (Field field : Shape.class.getDeclaredFields()) {
            if (field.getType() == Range.class) {
                try {
                    Range range = (Range) field.get(null);
                    range.setName(field.getName());

                    // Create shape color for all contained shapes
                    for (Shape shape : range.shapes) {
                        shape.createShapeColor(range.getColor());
                    }
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
            }
        }

        WHOLE_OR_HALF_REST.createShapeColor(Rests.getColor());
    }

    /** Symbols that can be attached to a stem */
    public static final EnumSet<Shape> StemSymbols = EnumSet.noneOf(
        Shape.class);

    static {
        StemSymbols.add(Shape.BEAM);
        StemSymbols.add(Shape.BEAM_2);
        StemSymbols.add(Shape.BEAM_3);
        StemSymbols.add(Shape.BEAM_HOOK);

        StemSymbols.addAll(NoteHeads.getShapes());
        StemSymbols.addAll(Flags.getShapes());
        StemSymbols.addAll(HeadAndFlags.getShapes());
    }

    /** Specific single symbol for part of time signature (such as 4) */
    public static final EnumSet<Shape> SingleTimes = EnumSet.range(
        TIME_ZERO,
        TIME_SIXTEEN);

    /** Specific multi-symbol for entire time signature (such as 4/4 */
    public static final EnumSet<Shape> MultiTimes = EnumSet.range(
        TIME_FOUR_FOUR,
        CUT_TIME);
}
