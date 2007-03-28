//----------------------------------------------------------------------------//
//                                                                            //
//                                 S h a p e                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
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
public enum Shape
    implements java.io.Serializable
{
    //~ Static variables/initializers ------------------------------------------

    // =========================================================================
    // Physical shapes, whose physical characteristics can be stored for
    // evaluator training.
    // =========================================================================

    // Garbage
    //
        /*-----*/ NOISE("Too small stuff"),
        /*-----*/ STRUCTURE("Structure of items"),

    // Pure physical stuff
    //
        /*-----*/ CLUTTER("Pure clutter"),
        /*-----*/ DOT("General dot shape"),
        /*-----*/ DASH("General dash shape"),

        // Bars
        //
        /*1D109*/ DAL_SEGNO("Repeat from the sign"),
        /*1D10A*/ DA_CAPO("Repeat from the beginning"),
        /*1D10B*/ SEGNO("Sign"),
        /*1D10C*/ CODA("Closing section"),
        /*1D10D*/ // REPEATED_FIGURE_1,
        /*1D10E*/ // REPEATED_FIGURE_2,
        /*1D10F*/ // REPEATED_FIGURE_3,
        /*1D110*/ FERMATA("Fermata"),
        /*1D111*/ FERMATA_BELOW("Fermata Below"),
        /*1D112*/ BREATH_MARK("Breath Mark"),
        /*1D113*/ CAESURA("Caesura"),
        /*1D114*/ BRACE("Brace"),
        /*1D115*/ BRACKET("Bracket"),

        // Staves
        //
        /*1D116*/ // ONE_LINE_STAFF,
        /*1D117*/ // TWO_LINE_STAFF,
        /*1D118*/ // THREE_LINE_STAFF,
        /*1D119*/ // FOUR_LINE_STAFF,
        /*1D11A*/ // FIVE_LINE_STAFF,
        /*1D11B*/ // SIX_LINE_STAFF,

        // Tablature
        //
        /*1D11C*/ // SIX_STRING_FRETBOARD,
        /*1D11D*/ // FOUR_STRING_FRETBOARD,

        // Clefs
        //
        /*1D11E*/ G_CLEF("Treble Clef"),
        /*1D11F*/ G_CLEF_OTTAVA_ALTA("Ottava Alta"),
        /*1D120*/ G_CLEF_OTTAVA_BASSA("Ottava Bassa"),
        /*1D121*/ C_CLEF("Ut Clef"),
        /*1D122*/ F_CLEF("Bass Clef"),
        /*1D123*/ F_CLEF_OTTAVA_ALTA("Bass Clef Ottava Alta"),
        /*1D124*/ F_CLEF_OTTAVA_BASSA("Bass Clef Ottava Bassa"),
        /*1D125*/ // DRUM_CLEF_1,
        /*1D126*/ // DRUM_CLEF_2,

        // Accidentals
        //
        /*1D127*/ FLAT("Minus one half step"),
        /*1D128*/ NATURAL("Natural value"),
        /*1D129*/ SHARP("Plus one half step"),
        /*1D12A*/ DOUBLE_SHARP("Double Sharp"),
        /*1D12B*/ DOUBLE_FLAT("Double Flat"),
        /*1D12C*/ // FLAT_UP,
        /*1D12D*/ // FLAT_DOWN,
        /*1D12E*/ // NATURAL_UP,
        /*1D12F*/ // NATURAL_DOWN,
        /*1D130*/ // SHARP_UP,
        /*1D131*/ // SHARP_DOWN,
        /*1D132*/ // QUARTER_TONE_SHARP,
        /*1D133*/ // QUARTER_TONE_FLAT,

        // Time signatures
        //
        /*-----*/ TIME_ZERO("Digit 0"),
        /*-----*/ TIME_ONE("Digit 1"),
        /*-----*/ TIME_TWO("Digit 2"),
        /*-----*/ TIME_THREE("Digit 3"),
        /*-----*/ TIME_FOUR("Digit 4"),
        /*-----*/ TIME_FIVE("Digit 5"),
        /*-----*/ TIME_SIX("Digit 6"),
        /*-----*/ TIME_SEVEN("Digit 7"),
        /*-----*/ TIME_EIGHT("Digit 8"),
        /*-----*/ TIME_NINE("Digit 9"),
        /*-----*/ TIME_TWELVE("Number 12"),
        /*-----*/ TIME_SIXTEEN("Number 16"),
        /*-----*/ TIME_FOUR_FOUR("Rational 4/4"),
        /*-----*/ TIME_TWO_TWO("Rational 2/2"),
        /*-----*/ TIME_TWO_FOUR("Rational 2/4"),
        /*-----*/ TIME_THREE_FOUR("Rational 3/4"),
        /*-----*/ TIME_SIX_EIGHT("Rational 6/8"),
        /*1D134*/ COMMON_TIME("Alpha = 4/4"),
        /*1D135*/ CUT_TIME("Semi-Alpha = 2/4"),

        // Octaves
        //
        /*1D136*/ OTTAVA_ALTA("8 va"),
        /*1D137*/ OTTAVA_BASSA("8 vb"),
        /*1D138*/ // QUINDICESIMA_ALTA,
        /*1D139*/ // QUINDICESIMA_BASSA,

        // Rests
        //
        /*1D13A*/ MULTI_REST("Rest for multiple measures"),
        /*1D13B*/ WHOLE_OR_HALF_REST("Same shape for whole or half Rest"),
//        /*1D13B*/ WHOLE_REST("Rest for whole measure"),
//        /*1D13C*/ HALF_REST("Rest for a 1/2"),
        /*1D13D*/ QUARTER_REST("Rest for a 1/4"),
        /*1D13E*/ EIGHTH_REST("Rest for a 1/8"),
        /*1D13F*/ SIXTEENTH_REST("Rest for a 1/16"),
        /*1D140*/ THIRTY_SECOND_REST("Rest for a 1/32"),
        /*1D141*/ SIXTY_FOURTH_REST("Rest for a 1/64"),
        /*1D142*/ ONE_HUNDRED_TWENTY_EIGHTH_REST("Rest for a 1/128"),

        // Noteheads
        //
        /*1D143*/ // X_NOTEHEAD,
        /*1D144*/ // PLUS_NOTEHEAD,
        /*1D145*/ // CIRCLE_X_NOTEHEAD,
        /*1D146*/ // SQUARE_NOTEHEAD_WHITE,
        /*1D147*/ // SQUARE_NOTEHEAD_BLACK,
        /*1D148*/ // TRIANGLE_NOTEHEAD_UP_WHITE,
        /*1D149*/ // TRIANGLE_NOTEHEAD_UP_BLACK,
        /*1D14A*/ // TRIANGLE_NOTEHEAD_LEFT_WHITE,
        /*1D14B*/ // TRIANGLE_NOTEHEAD_LEFT_BLACK,
        /*1D14C*/ // TRIANGLE_NOTEHEAD_RIGHT_WHITE,
        /*1D14D*/ // TRIANGLE_NOTEHEAD_RIGHT_BLACK,
        /*1D14E*/ // TRIANGLE_NOTEHEAD_DOWN_WHITE,
        /*1D14F*/ // TRIANGLE_NOTEHEAD_DOWN_BLACK,
        /*1D150*/ // TRIANGLE_NOTEHEAD_UP_RIGHT_WHITE,
        /*1D151*/ // TRIANGLE_NOTEHEAD_UP_RIGHT_BLACK,
        /*1D152*/ // MOON_NOTEHEAD_WHITE,
        /*1D153*/ // MOON_NOTEHEAD_BLACK,
        /*1D154*/ // TRIANGLEROUND_NOTEHEAD_DOWN_WHITE,
        /*1D155*/ // TRIANGLEROUND_NOTEHEAD_DOWN_BLACK,
        /*1D156*/ // PARENTHESIS_NOTEHEAD,
        /*1D157*/ VOID_NOTEHEAD("Hollow node head for halves"),
        /*-----*/ VOID_NOTEHEAD_2("Pack of two hollow node heads for halves"),
        /*-----*/ VOID_NOTEHEAD_3("Pack of three hollow node heads for halves"),
        /*1D158*/ NOTEHEAD_BLACK("Filled node head for quarters and less"),
        /*-----*/ NOTEHEAD_BLACK_2("Pack of two filled node heads for quarters and less"),
        /*-----*/ NOTEHEAD_BLACK_3("Pack of three filled node heads for quarters and less"),
        /*1D159*/ // NULL_NOTEHEAD,
        /*1D15A*/ // CLUSTER_NOTEHEAD_WHITE,
        /*1D15B*/ // CLUSTER_NOTEHEAD_BLACK,

        // Notes
        //
        /*1D15C*/ BREVE("Double Whole"), // ||O||
        /*1D15D*/ WHOLE_NOTE("Hollow node head for wholes"),
        /*-----*/ WHOLE_NOTE_2("Pack of two hollow node heads for wholes"),
        /*-----*/ WHOLE_NOTE_3("Pack of three hollow node heads for wholes"),
        /*1D15E*/ // HALF_NOTE, //= 1D157 + 1D165
        /*1D15F*/ // QUARTER_NOTE, //= 1D158 +1D165
        /*1D160*/ // EIGHTH_NOTE, //= 1D15F + 1D16E
        /*1D161*/ // SIXTEENTH_NOTE, //= 1D15F + 1D16F
        /*1D162*/ // THIRTY_SECOND_NOTE, //= 1D15F + 1D170
        /*1D163*/ // SIXTY_FOURTH_NOTE, //= 1D15F + 1D171
        /*1D164*/ // ONE_HUNDRED_TWENTY_EIGHTH_NOTE, //= 1D15F + 1D172

        // Tremolos
        //
        /*1D167*/ // COMBINING_TREMOLO_1,
        /*1D168*/ // COMBINING_TREMOLO_2,
        /*1D169*/ // COMBINING_TREMOLO_3,
        /*1D16A*/ // FINGERED_TREMOLO_1,
        /*1D16B*/ // FINGERED_TREMOLO_2,
        /*1D16C*/ // FINGERED_TREMOLO_3,

        // Flags
        //
        /*1D16E*/ COMBINING_FLAG_1("Single flag down"),
        /*1D16F*/ COMBINING_FLAG_2("Double flag down"),
        /*1D170*/ COMBINING_FLAG_3("Triple flag down"),
        /*1D171*/ COMBINING_FLAG_4("Quadruple flag down"),
        /*1D172*/ COMBINING_FLAG_5("Quintuple flag down"),

        /*-----*/ COMBINING_FLAG_1_UP("Single flag up"),
        /*-----*/ COMBINING_FLAG_2_UP("Double flag up"),
        /*-----*/ COMBINING_FLAG_3_UP("Triple flag up"),
        /*-----*/ COMBINING_FLAG_4_UP("Quadruple flag up"),
        /*-----*/ COMBINING_FLAG_5_UP("Quintuple flag up"),

        // Connected head and flags
        //
        /*-----*/ HEAD_AND_FLAG_1("Black notehead with single flag down"),
        /*-----*/ HEAD_AND_FLAG_2("Black notehead with double flag down"),
        /*-----*/ HEAD_AND_FLAG_3("Black notehead with triple flag down"),
        /*-----*/ HEAD_AND_FLAG_4("Black notehead with quadruple flag down"),
        /*-----*/ HEAD_AND_FLAG_5("Black notehead with quintuple flag down"),

        /*-----*/ HEAD_AND_FLAG_1_UP("Black notehead with single flag up"),
        /*-----*/ HEAD_AND_FLAG_2_UP("Black notehead with double flag up"),
        /*-----*/ HEAD_AND_FLAG_3_UP("Black notehead with triple flag up"),
        /*-----*/ HEAD_AND_FLAG_4_UP("Black notehead with quadruple flag up"),
        /*-----*/ HEAD_AND_FLAG_5_UP("Black notehead with quintuple flag up"),

        // Beams and slurs
        //
        /*-----*/ BEAM("Beam between two stems"),
        /*-----*/ BEAM_HOOK("Hook of a beam attached on one stem"),
        /*-----*/ SLUR("Slur tying notes"),
        /*1D173*/ // BEGIN_BEAM,
        /*1D174*/ // END_BEAM,
        /*1D175*/ // BEGIN_TIE,
        /*1D176*/ // END_TIE,
        /*1D177*/ // BEGIN_SLUR,
        /*1D178*/ // END_SLUR,
        /*1D179*/ // BEGIN_PHRASE,
        /*1D17A*/ // END_PHRASE,

        // Articulation
        //
        /*1D17B*/ // COMBINING_ACCENT,
        /*1D17C*/ // COMBINING_STACCATO,
        /*1D17D*/ // COMBINING_TENUTO,
        /*1D17E*/ // COMBINING_STACCATISSIMO,
        /*1D17F*/ // COMBINING_MARCATO,
        /*1D180*/ // COMBINING_MARCATO_STACCATO,
        /*1D181*/ // COMBINING_ACCENT_STACCATO,
        /*1D182*/ // COMBINING_LOURE,
        /*1D183*/ ARPEGGIATO_UP,
        /*1D184*/ ARPEGGIATO_DOWN,

        /*1D185*/ // COMBINING_DOIT,
        /*1D186*/ // COMBINING_RIP,
        /*1D187*/ // COMBINING_FLIP,
        /*1D188*/ // COMBINING_SMEAR,
        /*1D189*/ // COMBINING_BEND,
        /*1D18A*/ // COMBINING_DOUBLE_TONGUE,
        /*1D18B*/ // COMBINING_TRIPLE_TONGUE,
        /*1D18C*/ // RINFORZANDO,
        /*1D18D*/ // SUBITO,
        /*1D18E*/ // Z,

        // Dynamics
        //
        /*-----*/ DYNAMICS_CHAR_M,
        /*-----*/ DYNAMICS_CHAR_R,
        /*-----*/ DYNAMICS_CHAR_S,
        /*-----*/ DYNAMICS_CHAR_Z,
        //
        /*-----*/ DYNAMICS_F,
        /*-----*/ DYNAMICS_FF,
        /*-----*/ DYNAMICS_FFF,
        /*-----*/ DYNAMICS_FFFF,
        /*-----*/ DYNAMICS_FFFFF,
        /*-----*/ DYNAMICS_FFFFFF,
        /*-----*/ DYNAMICS_FP,
        /*-----*/ DYNAMICS_FZ,
        /*-----*/ DYNAMICS_MF,
        /*-----*/ DYNAMICS_MP,
        /*-----*/ DYNAMICS_P,
        /*-----*/ DYNAMICS_PP,
        /*-----*/ DYNAMICS_PPP,
        /*-----*/ DYNAMICS_PPPP,
        /*-----*/ DYNAMICS_PPPPP,
        /*-----*/ DYNAMICS_PPPPPP,
        /*-----*/ DYNAMICS_RF,
        /*-----*/ DYNAMICS_RFZ,
        /*-----*/ DYNAMICS_SF,
        /*-----*/ DYNAMICS_SFFZ,
        /*-----*/ DYNAMICS_SFP,
        /*-----*/ DYNAMICS_SFPP,
        /*-----*/ DYNAMICS_SFZ,
        //
        /*1D192*/ CRESCENDO,                            // <
        /*1D193*/ DECRESCENDO,                          // >

        // Ornaments
        //
        /*1D194*/ GRACE_NOTE_SLASH("Grace Note with a Slash"),
        /*1D195*/ GRACE_NOTE_NO_SLASH("Grace Note with no Slash"),
        /*1D196*/ TR,
        /*1D197*/ TURN,
        /*1D198*/ INVERTED_TURN,
        /*1D199*/ TURN_SLASH,
        /*1D19A*/ TURN_UP,
        /*-----*/ MORDENT,              // Lower
        /*-----*/ INVERTED_MORDENT,     // Upper
        /*1D19B*/ // ORNAMENT_STROKE_1,
        /*1D19C*/ // ORNAMENT_STROKE_2,
        /*1D19D*/ // ORNAMENT_STROKE_3,
        /*1D19E*/ // ORNAMENT_STROKE_4,
        /*1D19F*/ // ORNAMENT_STROKE_5,
        /*1D1A0*/ // ORNAMENT_STROKE_6,
        /*1D1A1*/ // ORNAMENT_STROKE_7,
        /*1D1A2*/ // ORNAMENT_STROKE_8,
        /*1D1A3*/ // ORNAMENT_STROKE_9,
        /*1D1A4*/ // ORNAMENT_STROKE_10,
        /*1D1A5*/ // ORNAMENT_STROKE_11,

        // Analytics
        //
        /*1D1A6*/ // HAUPTSTIMME,
        /*1D1A7*/ // NEBENSTIMME,
        /*1D1A8*/ // END_OF_STIMME,
        /*1D1A9*/ // DEGREE_SLASH,

        // Instrumentation
        //
        /*1D1AA*/ // COMBINING_DOWN_BOW,
        /*1D1AB*/ // COMBINING_UP_BOW,
        /*1D1AC*/ // COMBINING_HARMONIC,
        /*1D1AD*/ // COMBINING_SNAP_PIZZICATO,

        // Pedals
        //
        /*1D1AE*/ PEDAL_MARK,           // Ped
        /*1D1AF*/ PEDAL_UP_MARK,        // *
        /*1D1B0*/ // HALF_PEDAL_MARK,


        // =====================================================================
         // Pure Logical shapes, that cannot be inferred only from their
         // physical characteristics.
         // ====================================================================

        // Bars
        //
        /*1D100*/ SINGLE_BARLINE("Single thin bar line"),
        /*1D101*/ DOUBLE_BARLINE("Double thin bar line"),
        /*1D102*/ FINAL_BARLINE("Thin / Thick bar line"),
        /*1D103*/ REVERSE_FINAL_BARLINE("Thick / Thin bar line"),
        /*1D104*/ // DASHED_BARLINE,
        /*1D105*/ // SHORT_BARLINE,
        /*1D106*/ LEFT_REPEAT_SIGN("Thick / Thin bar line + REPEAT_DOTS"),
        /*1D107*/ RIGHT_REPEAT_SIGN("REPEAT_DOTS + Thin / Thick bar line"),
        /*-----*/ BACK_TO_BACK_REPEAT_SIGN("REPEAT_DOTS + Thin / Thick / Thin + REPEAT_DOTS"),
        /*-----*/ THICK_BAR_LINE("Thick bar line"),
        /*-----*/ THIN_BAR_LINE("Thin bar line"),
        /*1D108*/ REPEAT_DOTS("Vertical dots"),

        // Augmentation dot
        /*1D16D*/ COMBINING_AUGMENTATION_DOT("Augmentation Dot"),

        // Alternate ending indication
        /*-----*/ ENDING("Alternate ending"),

        // Miscellaneous
        //
        /*-----*/ LEDGER("Ledger"),
        /*-----*/ STAFF_LINE("Staff Line"),
        /*-----*/ ENDING_HORIZONTAL("Horizontal part of ending"),
        /*-----*/ ENDING_VERTICAL("Vertical part of ending"),

        // Stems
        //
        /*1D165*/ COMBINING_STEM,
        /*1D166*/ // COMBINING_SPRECHGESANG_STEM,

        // Key signatures
        //
        /*-----*/ KEY_FLAT_7("Seven Flats"),    // -7
        /*-----*/ KEY_FLAT_6("Six Flats"),      // -6
        /*-----*/ KEY_FLAT_5("Five Flats"),     // -5
        /*-----*/ KEY_FLAT_4("Four Flats"),     // -4
        /*-----*/ KEY_FLAT_3("Three Flats"),    // -3
        /*-----*/ KEY_FLAT_2("Two Flats"),      // -2
        /*-----*/ KEY_FLAT_1("One Flat"),       // -1
        //
        /*-----*/ KEY_SHARP_1("One Sharp"),     // +1
        /*-----*/ KEY_SHARP_2("Two Sharps"),    // +2
        /*-----*/ KEY_SHARP_3("Three Sharps"),  // +3
        /*-----*/ KEY_SHARP_4("Four Sharps"),   // +4
        /*-----*/ KEY_SHARP_5("Five Sharps"),   // +5
        /*-----*/ KEY_SHARP_6("Six Sharps"),    // +6
        /*-----*/ KEY_SHARP_7("Seven Sharps"),  // +7

        /*-----*/ CHARACTER("A letter"),

        /*1D13B*/ WHOLE_REST("Rest for whole measure", WHOLE_OR_HALF_REST),
        /*1D13C*/ HALF_REST("Rest for a 1/2", WHOLE_OR_HALF_REST),

        /** This shape is a kludge to get proper icon, to be improved */
        /*-----*/ MULTI_REST_DISPLAY,

        /*-----*/ FORWARD,

        /**
         * Specific value, meaning that we have not been able to determine a
         * legal shape
         */
        /*-----*/ NO_LEGAL_SHAPE("No Legal Shape");

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
    public static final Shape FirstLogicalShape = SINGLE_BARLINE;

    /** Color for unknown shape */
    public static final Color missedColor = Color.red;

    /** Color for glyphs tested as OK (color used temporarily) */
    public static final Color okColor = Color.green;

    //~ Instance variables -----------------------------------------------------

    /** Explanation of the glyph shape */
    private final String   description;

    /** Related temporary display color */
    private Color          color;

    /** Related permanent display color */
    private Constant.Color constantColor;

    /** Potential related icon */
    private Icon    icon;

    /** Potential related shape to be used for training */
    private Shape trainingShape;

    /** Remember the fact that this shape has no related icon */
    private boolean hasNoIcon;

    //~ Constructors -----------------------------------------------------------

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
           Shape trainingShape)
    {
        this.description = description;
        this.trainingShape = trainingShape;
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // isTrainable //
    //-------------//
    /**
     * Report whether this shape can be used to train an evaluator
     *
     * @return true if trainable, false otherwise
     */
    public boolean isTrainable()
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
            null, // DefaultValue
            "Color code for shape " + name());

        // Assign the shape display color
        if (constantColor.getValue() != null) {
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
    public Shape getTrainingShape()
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
        // Map for all defined ranges
        private static Map<String, Range> rangeMap;
        private String                    name; // Name of the range
        private final EnumSet<Shape>      shapes; // Contained shapes
        private Color                     color; // For current color
        private Constant.Color            constantColor; // For specific color

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
            // Build the range map in a lazy way
            if (rangeMap == null) {
                rangeMap = new HashMap<String, Range>();

                for (Field field : Shape.class.getDeclaredFields()) {
                    if (field.getType() == Range.class) {
                        try {
                            Range range = (Range) field.get(null);
                            rangeMap.put(field.getName(), range);
                        } catch (IllegalAccessException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

            return rangeMap.get(str);
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
                null, // DefaultValue
                "Color code for range " + name);

            // Assign the range display color
            if (constantColor.getValue() != null) {
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
    public static final Range Garbage       = new Range(EnumSet.range(NOISE, STRUCTURE));
    public static final Range Physicals     = new Range(EnumSet.range(CLUTTER, DASH));
    //
    public static final Range Bars          = new Range(EnumSet.range(DAL_SEGNO, BRACKET));
    public static final Range Clefs         = new Range(EnumSet.range(G_CLEF, F_CLEF_OTTAVA_BASSA));
    public static final Range Accidentals   = new Range(EnumSet.range(FLAT, DOUBLE_FLAT));
    public static final Range Times         = new Range(EnumSet.range(TIME_ZERO, CUT_TIME));
    public static final Range Octaves       = new Range(EnumSet.range(OTTAVA_ALTA, OTTAVA_BASSA));
    public static final Range Rests         = new Range(EnumSet.of(MULTI_REST,
                                                                   WHOLE_REST,
                                                                   HALF_REST,
                                                                   QUARTER_REST,
                                                                   EIGHTH_REST,
                                                                   SIXTEENTH_REST,
                                                                   THIRTY_SECOND_REST,
                                                                   SIXTY_FOURTH_REST,
                                                                   ONE_HUNDRED_TWENTY_EIGHTH_REST));
    public static final Range NoteHeads     = new Range(EnumSet.range(VOID_NOTEHEAD, NOTEHEAD_BLACK_3));
    public static final Range Notes         = new Range(EnumSet.range(BREVE, WHOLE_NOTE_3));
    public static final Range Stems         = new Range(EnumSet.range(COMBINING_STEM, COMBINING_STEM));
    public static final Range Flags         = new Range(EnumSet.range(COMBINING_FLAG_1, COMBINING_FLAG_5_UP));
    public static final Range HeadAndFlags  = new Range(EnumSet.range(HEAD_AND_FLAG_1, HEAD_AND_FLAG_5_UP));
    public static final Range Beams         = new Range(EnumSet.range(BEAM, SLUR));
    public static final Range Articulations = new Range(EnumSet.range(ARPEGGIATO_UP, ARPEGGIATO_DOWN));
    public static final Range Dynamics      = new Range(EnumSet.range(DYNAMICS_CHAR_M, DECRESCENDO));
    public static final Range Ornaments     = new Range(EnumSet.range(GRACE_NOTE_SLASH, INVERTED_MORDENT));
    public static final Range Pedals        = new Range(EnumSet.range(PEDAL_MARK, PEDAL_UP_MARK));
    //
    public static final Range Barlines      = new Range(EnumSet.range(SINGLE_BARLINE, THIN_BAR_LINE));
    public static final Range Logicals      = new Range(EnumSet.range(REPEAT_DOTS, ENDING));

    // Assign proper name to all ranges and proper color to their contained
    // shapes
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
        StemSymbols.add(Shape.BEAM_HOOK);

        StemSymbols.addAll(NoteHeads.getShapes());
        StemSymbols.addAll(Flags.getShapes());
        StemSymbols.addAll(HeadAndFlags.getShapes());
    }

    /** Specific single symbol for part of time signature (such as 4) */
    public static final EnumSet<Shape> SingleTimes = EnumSet.range(
        TIME_ZERO,
        TIME_SIXTEEN);

    /** Specific multi-symbol for entiretime signature (such as 4/4 */
    public static final EnumSet<Shape> MultiTimes = EnumSet.range(
        TIME_FOUR_FOUR,
        CUT_TIME);
}
