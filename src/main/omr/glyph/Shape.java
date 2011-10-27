//----------------------------------------------------------------------------//
//                                                                            //
//                                 S h a p e                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.Constant;

import omr.log.Logger;

import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class <code>Shape</code> defines the comprehensive list of glyph shapes. It
 * is organized according to the Unicode Standard 4.0, with a few addition for
 * convenience only.
 *
 * <p>The enumeration begins with physical shapes (which are the
 * only ones usable for training) and end with the logical shapes. The
 * method {@link #isTrainable()} can be used to disambiguate between physical
 * and logical shapes</p>
 *
 * <p>A logical shape, such as STACCATO, may use the same physical shape
 * (in this case DOT) as other shapes. In that case we say that the physical
 * shape of STACCATO is DOT. The method {@link #getPhysicalShape()} returns the
 * shape to use for training and for drawing.
 *
 * <p>As far as possible, a symbol should be generated for every shape.</p>
 *
 * <p>A shape may have a related "decorated" symbol. For example the BREVE_REST
 * is similar to a black rectangle which is used for training / recognition and
 * the related symbol is used for drawing in score view. However, in menu items,
 * it is displayed as a black rectangle surrounded by a staff line above and a
 * staff line below, a symbol which is known as BREVE_REST_DECORATED. The method
 * {@link #getDecoratedSymbol()} returns the symbol to use in menu items.</p>
 *
 * @author Hervé Bitteur
 */
public enum Shape {
    // =========================================================================
    // Physical shapes, whose physical characteristics can be stored for
    // evaluator training.
    // Nota: All the physical shapes MUST have different characteristics
    // for the training to work.
    // Nota: Do not change the order of these physical shapes, otherwise the
    // evaluators won't detect this and you'll have to retrain them on your own.
    // =========================================================================

    /** Structure of items */
    STRUCTURE("Structure of items"),
    //
    // Pure physical stuff -----------------------------------------------------
    //
    /** Pure clutter */
    CLUTTER("Pure clutter"), 
    /** General dot shape */
    DOT("General dot shape"), 
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

    /** Percussion (neutral) clef */
    PERCUSSION_CLEF("Percussion Clef"), 
    /** Flat */
    FLAT("Minus one half step"), 
    /** Natural value */
    NATURAL("Natural value"), 
    /** Plus one half step */
    SHARP("Plus one half step"), 
    /** Double Sharp */
    DOUBLE_SHARP("Double Sharp"), 
    /** Double Flat */
    DOUBLE_FLAT("Double Flat"), 
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
    /** 8 va */
    OTTAVA_ALTA("8 va"), 
    /** 8 vb */
    OTTAVA_BASSA("8 vb"), 
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

    /** Long Rest */
    LONG_REST("Rest for 4 measures"), 
    /** Breve Rest */
    BREVE_REST("Rest for 2 measures"), 

    /** Same shape for whole or half Rest */
    WHOLE_OR_HALF_REST("Same shape for whole or half Rest"), 

    /** Rest for a 1/4 (old) =  flipped 1/8 */
    OLD_QUARTER_REST("(old) Rest for a 1/4"), 
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
    ACCENT, 
    //
    TENUTO, 
    //
    STACCATISSIMO, 
    //
    STRONG_ACCENT("Marcato"), 
    //
    ARPEGGIATO, 
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
    GRACE_NOTE_NO_SLASH("Grace Note with no Slash"), 
    /** Trille */
    TR, TURN, INVERTED_TURN,
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
    TUPLET_THREE,TUPLET_SIX, PEDAL_MARK,
    PEDAL_UP_MARK,

    //
    //
    // =========================================================================
    // =========================================================================
    // This is the end of physical shapes.
    // Next shapes are pure logical shapes, that CANNOT be inferred only from
    // their physical characteristics.
    // =========================================================================
    // =========================================================================
    //
    /** Noise */
    NOISE("Too small stuff"),

    //
    // Bars --------------------------------------------------------------------
    //

    /** Part defining bar line */
    PART_DEFINING_BARLINE("Bar line that defines a part"), 

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

    /** Staff Line */
    STAFF_LINE("Staff Line"), 
    /** Ledger */
    LEDGER("Ledger"), 
    /** Horizontal part of ending */
    ENDING_HORIZONTAL("Horizontal part of ending"), 

    /** Vertical part of ending */
    ENDING_VERTICAL("Vertical part of ending"), 
    // Stems
    //
    COMBINING_STEM("Stem"), 
    //     COMBINING_SPRECHGESANG_STEM,

    //
    // Key signatures ----------------------------------------------------------
    //

    /** One Flat */
    KEY_FLAT_1("One Flat"), 
    /** One Sharp */
    KEY_SHARP_1("One Sharp"), 

    //
    // Rests -------------------------------------------------------------------
    //
    WHOLE_REST("Rest for whole measure", WHOLE_OR_HALF_REST), 

    /** Rest for a 1/2 */
    HALF_REST("Rest for a 1/2", WHOLE_OR_HALF_REST), 
    //
    // Other stuff -------------------------------------------------------------
    //
    /** A staccato is nothing but a dot */
    STACCATO("Staccato", DOT), 
    /** Meant to indicate a forward in score view */
    FORWARD, 
    /** Non-draggable shape */
    NON_DRAGGABLE, 
    /** A glyph which is nothing but part of a larger glyph */
    GLYPH_PART("Part of a larger glyph"), 
    /** A time signature whose values are defined by the user */
    CUSTOM_TIME_SIGNATURE, 
    /**
     * Specific value, meaning that we have not been able to determine a
     * time signature shape
     */
    NO_LEGAL_TIME("No Legal Time Shape");
    //
    //
    // =========================================================================
    // =========================================================================
    // This is the end of shape enumeration
    // =========================================================================
    // =========================================================================
    //

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Shape.class);

    /** Last physical shape */
    public static final Shape LAST_PHYSICAL_SHAPE = PEDAL_UP_MARK;

    static {
        // Make sure all the shape colors are defined
        ShapeRange.defineAllShapeColors();

        // Debug
        ///dumpShapeColors();
    }

    /** A comparator based on shape name */
    public static Comparator<Shape> alphaComparator = new Comparator<Shape>() {
        public int compare (Shape o1,
                            Shape o2)
        {
            return o1.name()
                     .compareTo(o2.name());
        }
    };

    //--------------------------------------------------------------------------

    /** Explanation of the glyph shape */
    private final String description;

    /** Related display color */
    private Color color;

    /** Potential related symbol */
    private ShapeSymbol symbol;

    /** Potential related decorated symbol for menus */
    private ShapeSymbol decoratedSymbol;

    /** Remember the fact that this shape has no related symbol */
    private boolean hasNoSymbol;

    /** Remember the fact that this shape has no related decorated symbol */
    private boolean hasNoDecoratedSymbol;

    /** Potential related physical shape */
    private Shape physicalShape;

    //--------------------------------------------------------------------------

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
           Shape  physicalShape)
    {
        this.description = description;
        this.physicalShape = physicalShape;
    }

    //--------------------------------------------------------------------------

    //---------------//
    // isMeasureRest //
    //---------------//
    /**
     * Check whether the shape is a whole (or multi) rest, for which no duration
     * can be specified
     *
     * @return true if whole or multi rest
     */
    public boolean isMeasureRest ()
    {
        return (this == WHOLE_REST) || (this == BREVE_REST) ||
               (this == LONG_REST);
    }

    //--------------//
    // isPersistent //
    //--------------//
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
        return ShapeRange.Clefs.contains(this) ||
               ShapeRange.Times.contains(this) ||
               ShapeRange.Accidentals.contains(this);
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

    //--------------//
    // isSharpBased //
    //--------------//
    /**
     * Check whether the shape is a sharp or a key-sig sequence of sharps
     *
     * @return true if sharp or sharp key sig
     */
    public boolean isSharpBased ()
    {
        return (this == SHARP) || ShapeRange.SharpKeys.contains(this);
    }

    //-------------//
    // isFlatBased //
    //-------------//
    /**
     * Check whether the shape is a flat or a key-sig sequence of flatss
     *
     * @return true if flat or flat key sig
     */
    public boolean isFlatBased ()
    {
        return (this == FLAT) || ShapeRange.FlatKeys.contains(this);
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
        return ordinal() <= LAST_PHYSICAL_SHAPE.ordinal();
    }

    //-------------//
    // isWellKnown //
    //-------------//
    /**
     * Report whether this shape is well known, that is a non-garbage symbol
     *
     * @return true if non-garbage, false otherwise
     */
    public boolean isWellKnown ()
    {
        return (this != NO_LEGAL_TIME) && (this != GLYPH_PART) &&
               (this != STRUCTURE) && (this != NOISE);
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
     * Report the color assigned to the shape, if any
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
     * Assign a color for this shape
     * @param color the display color
     */
    public void setColor (java.awt.Color color)
    {
        this.color = color;
    }

    //------------------//
    // createShapeColor //
    //------------------//
    void createShapeColor (Color color)
    {
        // Create the underlying constant
        Constant.Color constantColor = new Constant.Color(
            getClass().getName(), // Unit
            name() + ".color", // Name
            "#000000", // DefaultValue: Black
            "Color for shape " + name());

        // Assign the shape display color
        if (!constantColor.isSourceValue()) {
            setColor(constantColor.getValue()); // Use the shape specific color
        } else {
            setColor(color); // Use the provided (range) default color
        }
    }

    //-----------//
    // getSymbol //
    //-----------//
    /**
     * Report the symbol related to the shape, if any
     *
     * @return the related symbol, or null
     */
    public ShapeSymbol getSymbol ()
    {
        if (hasNoSymbol) {
            return null;
        }

        if (symbol == null) {
            symbol = Symbols.getSymbol(this);

            if (symbol == null) {
                hasNoSymbol = true;
            }
        }

        return symbol;
    }

    //-----------//
    // setSymbol //
    //-----------//
    /**
     * Assign a symbol to this shape
     *
     * @param symbol the assigned symbol, which may be null
     */
    public void setSymbol (ShapeSymbol symbol)
    {
        this.symbol = symbol;
    }

    //--------------------//
    // getDecoratedSymbol //
    //--------------------//
    /**
     * Report the symbol to use for menu items
     * @return the shape symbol, with decorations if any
     */
    public ShapeSymbol getDecoratedSymbol ()
    {
        // Avoid a new search, just use the undecorated symbol instead
        if (hasNoDecoratedSymbol) {
            return getSymbol();
        }

        // Try to build / load a decorated symbol
        if (decoratedSymbol == null) {
            setDecoratedSymbol(Symbols.getSymbol(this, true));

            if (decoratedSymbol == null) {
                hasNoDecoratedSymbol = true;

                return getSymbol();
            }
        }

        // Simply return the cached decorated symbol
        return decoratedSymbol;
    }

    //--------------------//
    // setDecoratedSymbol //
    //--------------------//
    /**
     * Assign a decorated symbol to this shape
     *
     * @param decoratedSymbol the assigned decorated symbol, which may be null
     */
    public void setDecoratedSymbol (ShapeSymbol decoratedSymbol)
    {
        this.decoratedSymbol = decoratedSymbol;
    }

    //------------------//
    // getPhysicalShape //
    //------------------//
    /**
     * Report the shape to use for training or precise drawing in a view
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

    //-------------//
    // isDraggable //
    //-------------//
    /**
     * Report whether this shape can be dragged (in a DnD gesture)
     * @return true if draggable
     */
    public boolean isDraggable ()
    {
        return getPhysicalShape()
                   .getSymbol() != null;
    }

    //-----------------//
    // dumpShapeColors //
    //-----------------//
    /**
     * Dump the color of every shape
     */
    public static void dumpShapeColors ()
    {
        List<String> names = new ArrayList<String>();

        for (Shape shape : Shape.values()) {
            names.add(
                shape + " " + Constant.Color.encodeColor(shape.getColor()));
        }

        Collections.sort(names);

        for (String str : names) {
            System.out.println(str);
        }
    }
}
