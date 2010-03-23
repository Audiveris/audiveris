//----------------------------------------------------------------------------//
//                                                                            //
//                                 S h a p e                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.Constant;

import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.SymbolManager;

import java.awt.Color;

/**
 * Class <code>Shape</code> defines the comprehensive list of glyph shapes. It
 * is organized according to the Unicode Standard 4.0, with a few addition for
 * convenience only.
 *
 * <p>As far as possible, an Icon should be generated for every shape.
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
    // Percussion ST47

    /** Structure of items */
    STRUCTURE("Structure of items"),
    //
    // Pure physical stuff -----------------------------------------------------
    //
    /** Pure clutter */
    CLUTTER("Pure clutter"), 
    /** General dot shape */
    DOT("General dot shape", 46), 
    /** A letter */
    CHARACTER("A letter"), 
    /** Sequence of letters & spaces */
    TEXT("Sequence of letters & spaces"), 

    //
    // Bars --------------------------------------------------------------------
    //
    /** Repeat from the sign */
    DAL_SEGNO("Repeat from the sign", 100), 

    /** Repeat from the beginning */
    DA_CAPO("Repeat from the beginning", 68), 
    /** Sign */
    SEGNO("Sign", 37), 
    /** Closing section */
    CODA("Closing section", 222), FERMATA("Fermata", 85), 

    /** Fermata Below */
    FERMATA_BELOW("Fermata Below", 117),
    /** Breath Mark */
    BREATH_MARK("Breath Mark", 44), 
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
    G_CLEF("Treble Clef", 38), 
    /** Ottava Alta */
    G_CLEF_OTTAVA_ALTA("Ottava Alta"), 
    /** Ottava Bassa */
    G_CLEF_OTTAVA_BASSA("Ottava Bassa"), 
    /** Ut Clef */
    C_CLEF("Ut Clef", 66), 
    /** Bass Clef */
    F_CLEF("Bass Clef", 63), 
    /** Bass Clef Ottava Alta */
    F_CLEF_OTTAVA_ALTA("Bass Clef Ottava Alta"), 

    /** Bass Clef Ottava Bassa */
    F_CLEF_OTTAVA_BASSA("Bass Clef Ottava Bassa"), 
    FLAT("Minus one half step", 98), 
    /** Natural value */
    NATURAL("Natural value", 110), 
    /** Plus one half step */
    SHARP("Plus one half step", 35), 
    /** Double Sharp */
    DOUBLE_SHARP("Double Sharp", 220), 
    /** Double Flat */
    DOUBLE_FLAT("Double Flat", 186), TIME_ZERO("Digit 0", 48), 

    /** Digit 1 */
    TIME_ONE("Digit 1", 49),
    /** Digit 2 */
    TIME_TWO("Digit 2", 50), 
    /** Digit 3 */
    TIME_THREE("Digit 3", 51), 
    /** Digit 4 */
    TIME_FOUR("Digit 4", 52), 
    /** Digit 5 */
    TIME_FIVE("Digit 5", 53), 
    /** Digit 6 */
    TIME_SIX("Digit 6", 54), 
    /** Digit 7 */
    TIME_SEVEN("Digit 7", 55), 
    /** Digit 8 */
    TIME_EIGHT("Digit 8", 56), 
    /** Digit 9 */
    TIME_NINE("Digit 9", 57), 
    /** Number 12 */
    TIME_TWELVE("Number 12", 49, 50), 
    /** Number 16 */
    TIME_SIXTEEN("Number 16", 49, 54), 
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
    COMMON_TIME("Alpha = 4/4", 99), 
    /** Semi-Alpha = 2/4 */
    CUT_TIME("Semi-Alpha = 2/4", 67), 
    /** 8 va */
    OTTAVA_ALTA("8 va", 195), 
    /** 8 vb */
    OTTAVA_BASSA("8 vb", 215), 
    /** Multi Rest */
    MULTI_REST("Rest for multiple measures", 208), 

    /** Same shape for whole or half Rest */
    WHOLE_OR_HALF_REST("Same shape for whole or half Rest", 183), 

    /** Rest for a 1/4 */
    QUARTER_REST("Rest for a 1/4", 206), 
    /** Rest for a 1/8 */
    EIGHTH_REST("Rest for a 1/8", 228), 
    /** Rest for a 1/16 */
    SIXTEENTH_REST("Rest for a 1/16", 197), 

    /** Rest for a 1/32 */
    THIRTY_SECOND_REST("Rest for a 1/32", 168), 

    /** Rest for a 1/64 */
    SIXTY_FOURTH_REST("Rest for a 1/64", 244), 

    /** Rest for a 1/128 */
    ONE_HUNDRED_TWENTY_EIGHTH_REST("Rest for a 1/128"), 

    //
    // Noteheads ---------------------------------------------------------------
    //

    /** Hollow node head for halves */
    VOID_NOTEHEAD("Hollow node head for halves", 250), 

    /** Pack of two hollow node heads for halves */
    VOID_NOTEHEAD_2("Pack of two hollow node heads for halves"), 

    /** Pack of three hollow node heads for halves */
    VOID_NOTEHEAD_3("Pack of three hollow node heads for halves"), 

    /** Filled node head for quarters and less */
    NOTEHEAD_BLACK("Filled node head for quarters and less", 207), 

    /** Pack of two filled node heads for quarters and less */
    NOTEHEAD_BLACK_2("Pack of two filled node heads for quarters and less"), 

    /** Pack of three filled node heads for quarters and less */
    NOTEHEAD_BLACK_3("Pack of three filled node heads for quarters and less"), 

    //
    // Notes -------------------------------------------------------------------
    //

    /** Double Whole */
    BREVE("Double Whole", 87), 
    /** Hollow node head for wholes */
    WHOLE_NOTE("Hollow node head for wholes", 119), 

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
    COMBINING_FLAG_1("Single flag down", 106), 

    /** Double flag down */
    COMBINING_FLAG_2("Double flag down", 107), 

    /** Triple flag down */
    COMBINING_FLAG_3("Triple flag down"), 

    /** Quadruple flag down */
    COMBINING_FLAG_4("Quadruple flag down"), 

    /** Quintuple flag down */
    COMBINING_FLAG_5("Quintuple flag down"), 

    /** Single flag up */
    COMBINING_FLAG_1_UP("Single flag up", 74), 

    /** Double flag up */
    COMBINING_FLAG_2_UP("Double flag up", 75), 

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
    ACCENT(62), TENUTO(45), STACCATISSIMO(137),
    STRONG_ACCENT("Marcato", 94),

    //
    ARPEGGIATO(103),
    //
    // Dynamics ----------------------------------------------------------------
    //
    DYNAMICS_CHAR_M(189), DYNAMICS_CHAR_R(243), DYNAMICS_CHAR_S(115),
    DYNAMICS_CHAR_Z(122),

    //
    DYNAMICS_F(102),DYNAMICS_FF(196), DYNAMICS_FFF(236),
    DYNAMICS_FFFF(236, 102),
    DYNAMICS_FFFFF(236, 196),
    DYNAMICS_FFFFFF(236, 236),
    DYNAMICS_FP(102, 112),
    DYNAMICS_FZ(90),
    DYNAMICS_MF(70),
    DYNAMICS_MP(80),
    DYNAMICS_P(112),
    DYNAMICS_PP(185),
    DYNAMICS_PPP(184),
    DYNAMICS_PPPP(184, 112),
    DYNAMICS_PPPPP(184, 185),
    DYNAMICS_PPPPPP(184, 184),
    DYNAMICS_RF(243, 102),
    DYNAMICS_RFZ(243, 102, 122),
    DYNAMICS_SF(83),
    DYNAMICS_SFFZ(83, 90),
    DYNAMICS_SFP(83, 112),
    DYNAMICS_SFPP(83, 185),
    DYNAMICS_SFZ(83, 122),

    //
    CRESCENDO,DECRESCENDO, 
    //
    // Ornaments ---------------------------------------------------------------
    //

    /** Grace Note with a Slash */
    GRACE_NOTE_SLASH("Grace Note with a Slash", 201),

    /** Grace Note with no Slash */
    GRACE_NOTE_NO_SLASH("Grace Note with no Slash", 59), TR(96), TURN(84),
    INVERTED_TURN(249),
    TURN_SLASH,
    TURN_UP,
    MORDENT(109),
    INVERTED_MORDENT(77),

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
    TUPLET_THREE(163),TUPLET_SIX(164), PEDAL_MARK(161),
    PEDAL_UP_MARK(42),

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
    STEM_NAKED, COMBINING_STEM("Stem", STEM_NAKED), 
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
    /** Multi Rest w/o decoration */
    MULTI_REST_NAKED, 
    //
    // Other stuff -------------------------------------------------------------
    //
    /** A staccato is nothing but a dot */
    STACCATO("Staccato", DOT), 
    /** Meant to indicate a forward in score view */
    FORWARD, 
    /** A glyph which is nothing but part of a larger glyph */
    GLYPH_PART("Part of a larger glyph"), 
    /** A time signature whose values are defined by the user */
    CUSTOM_TIME_SIGNATURE, 
    /**
     * Specific value, meaning that we have not been able to determine a
     * time signature shape
     */
    NO_LEGAL_TIME("No Legal Time Shape");
    static {
        // Static block to workaround forward references */
        MULTI_REST.nakedShape = MULTI_REST_NAKED;
    }

    /**
     * Last physical shape an evaluator should be able to recognize based on
     * their physical characteristics. For example a DOT is a DOT. Also, a DOT
     * plus a FERMATA_BEND together can compose a FERMATA.
     */
    public static final Shape LAST_PHYSICAL_SHAPE = PEDAL_UP_MARK;

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

    /** Potential related symbol */
    private ShapeSymbol symbol;

    /** Potential related naked shape to be used for training or drawing */
    private Shape nakedShape;

    /** Remember the fact that this shape has no related symbol */
    private boolean hasNoSymbol;

    /** Sequence of corresponding point codes in Stoccata font */
    private int[] pointCodes;

    //-------//
    // Shape //
    //-------//
    Shape (int... codes)
    {
        this("", null, codes); // No param
    }

    //-------//
    // Shape //
    //-------//
    Shape (String description,
           int... codes)
    {
        this(description, null, codes);
    }

    //-------//
    // Shape //
    //-------//
    Shape (String description,
           Shape  nakedShape,
           int... codes)
    {
        this.description = description;
        this.nakedShape = nakedShape;
        this.pointCodes = codes;
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
        return ShapeRange.Clefs.contains(this) ||
               ShapeRange.Times.contains(this) ||
               ShapeRange.Accidentals.contains(this);
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
               ((this != STRUCTURE) && (this != NOISE));
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
    void createShapeColor (Color color)
    {
        // Create the underlying constant
        constantColor = new Constant.Color(
            getClass().getName(), // Unit
            name() + ".color", // Name
            "#000000", // DefaultValue: Black
            "Color code for shape " + name());

        // Assign the shape display color
        if (!constantColor.isSourceValue()) {
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
            setSymbol(SymbolManager.getInstance().loadSymbol(toString()));

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

    //---------------//
    // getNakedShape //
    //---------------//
    /**
     * Report the shape to use for training or precise drawing in a view
     * @return generally the same shape, without any decoration
     */
    public Shape getNakedShape ()
    {
        if (nakedShape != null) {
            return nakedShape;
        } else {
            return this;
        }
    }
}
