//----------------------------------------------------------------------------//
//                                                                            //
//                                 S h a p e                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
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
 * Class {@code Shape} defines the comprehensive list of glyph shapes.
 * It is organized according to the Unicode Standard 4.0, with a few addition
 * for convenience only.
 *
 * <p>The enumeration begins with physical shapes (which are the
 * only ones usable for training) and ends with the logical shapes.
 * The method {@link #isTrainable()} can be used to disambiguate between
 * physical and logical shapes.</p>
 *
 * <p><b>Nota</b>: All the physical shapes <b>MUST</b> have different
 * characteristics for the training to work correctly.
 * The ART evaluator uses moments that are invariant to translation, scaling
 * and rotation (and to symmetry as well).
 * Shapes that exhibit some symmetry (like FERMATA vs FERMATA_BELOW) would
 * be considered as the same shape by the ART evaluator.
 * Therefore, the strategy is to define a single shape (FERMATA_SET) for the
 * evaluator, leaving the final disambiguation between FERMATA_BELOW and
 * FERMATA to tests performed beyond the ART evaluator.
 * FERMATA_SET belongs to the physical shapes, while FERMATA_BELOW and
 * FERMATA belong to the logical shapes.
 * All shapes whose name ends with "_set" are in this case.</p>
 *
 * <p>As far as possible, a symbol should be generated for every shape.</p>
 *
 * <p>A shape may have a related "decorated" symbol. For example the BREVE_REST
 * is similar to a black rectangle which is used for training / recognition and
 * the related symbol is used for drawing in score view. However, in menu items,
 * it is displayed as a black rectangle surrounded by a staff line above and a
 * staff line below.
 * The method {@link #getDecoratedSymbol()} returns the symbol to use in menu
 * items.</p>
 *
 * @author Hervé Bitteur
 */
public enum Shape {
    /**
     * ================================================================
     * Nota: Avoid changing the order of these physical shapes,
     * otherwise the evaluators won't detect this and you'll have to
     * retrain them on your own.
     * =========================================================================
     */

    //
    // Sets --------------------------------------------------------------------
    //
    /** Dot set */
    DOT_set("Dot set"),
    /** Fermata set */
    FERMATA_set("Set of Fermata's"), 
    /** Half & Whole Rest set */
    HW_REST_set("Half & Whole Rest set"), 
    /** Time 6 & 9 set */
    TIME_69_set("Time 6 & 9 set"), 
    /** Single flag set */
    FLAG_1_set("Single flag set"), 
    /** Double flag set */
    FLAG_2_set("Double flag set"), 
    /** Triple flag set */
    FLAG_3_set("Triple flag set"), 
    /** Quadruple flag set */
    FLAG_4_set("Quadruple flag set"), 
    /** Quintuple flag set */
    FLAG_5_set("Quintuple flag set"), 
    /** Crescendo & Decrescendo set */
    WEDGE_set("Crescendo & Decrescendo set"), 
    /** Turn set */
    TURN_set("Turn set"), 
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
    /** Breath Mark */
    BREATH_MARK("Breath Mark"), 
    /** Caesura */
    CAESURA("Caesura"), 
    /** Brace */
    BRACE("Brace"), 
    /** Bracket */
    BRACKET("Bracket"), 
    //
    // Clefs -------------------------------------------------------------------
    //

    /** Treble Clef */
    G_CLEF("Small Treble Clef"), 
    /** Treble Clef */
    G_CLEF_SMALL("Small Treble Clef"), 
    /** Ottava Alta */
    G_CLEF_8VA("Ottava Alta"), 
    /** Ottava Bassa */
    G_CLEF_8VB("Ottava Bassa"), 
    /** Ut Clef */
    C_CLEF("Ut Clef"), 
    /** Bass Clef */
    F_CLEF("Small Bass Clef"), 
    /** Bass Clef */
    F_CLEF_SMALL("Small Bass Clef"), 
    /** Bass Clef Ottava Alta */
    F_CLEF_8VA("Bass Clef Ottava Alta"), 
    /** Bass Clef Ottava Bassa */
    F_CLEF_8VB("Bass Clef Ottava Bassa"), 
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
    /** Digit 7 */
    TIME_SEVEN("Digit 7"), 
    /** Digit 8 */
    TIME_EIGHT("Digit 8"), 
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
    CUT_TIME("Semi-Alpha = 2/2"), 
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
    /** Rest for a 1/4 */
    QUARTER_REST("Rest for a 1/4"), 
    /** Rest for a 1/8 */
    EIGHTH_REST("Rest for a 1/8"), 
    /** Rest for a 1/16 */
    ONE_16TH_REST("Rest for a 1/16"), 
    /** Rest for a 1/32 */
    ONE_32ND_REST("Rest for a 1/32"), 
    /** Rest for a 1/64 */
    ONE_64TH_REST("Rest for a 1/64"), 
    /** Rest for a 1/128 */
    ONE_128TH_REST("Rest for a 1/128"), 

    //
    // Noteheads ---------------------------------------------------------------
    //
    /** Hollow node head for halves */
    NOTEHEAD_VOID("Hollow node head for halves"), 

    /** Pack of two hollow node heads for halves */
    NOTEHEAD_VOID_2("Pack of two hollow node heads for halves"), 

    /** Pack of three hollow node heads for halves */
    NOTEHEAD_VOID_3("Pack of three hollow node heads for halves"), 

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
    //
    // Articulation ------------------------------------------------------------
    //
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
    /** Dynamics m */
    DYNAMICS_CHAR_M("m character"), 
    /** Dynamics r */
    DYNAMICS_CHAR_R("r character"), 
    /** Dynamics s */
    DYNAMICS_CHAR_S("c character"), 
    /** Dynamics z */
    DYNAMICS_CHAR_Z("z character"), 
    /** Forte */
    DYNAMICS_F("Forte"), 
    /** Fortissimo */
    DYNAMICS_FF("Fortissimo"), 
    /** Fortississimo */
    DYNAMICS_FFF("Fortississimo"), 
    /** FortePiano */
    DYNAMICS_FP("FortePiano"), 
    /** Forzando */
    DYNAMICS_FZ("Forzando"), 
    /** Mezzo forte */
    DYNAMICS_MF("Mezzo forte"), 
    /** Mezzo forte */
    DYNAMICS_MP("Mezzo piano"), 
    /** Piano */
    DYNAMICS_P("Piano"), 
    /** Pianissimo */
    DYNAMICS_PP("Pianissimo"), 
    /** Pianississimo */
    DYNAMICS_PPP("Pianississimo"), 
    /** ??? */
    DYNAMICS_RF, 
    /** Rinforzando */
    DYNAMICS_RFZ("Rinforzando"), 
    /** ??? */
    DYNAMICS_SF, 
    /** ??? */
    DYNAMICS_SFFZ, 
    /** Subito fortepiano */
    DYNAMICS_SFP("Subito fortepiano"), 
    /** ??? */
    DYNAMICS_SFPP, 
    /** Sforzando */
    DYNAMICS_SFZ("Sforzando"), 
    //
    // Ornaments ---------------------------------------------------------------
    //
    /** Grace Note with a Slash */
    GRACE_NOTE_SLASH("Grace Note with a Slash"), 
    /** Grace Note with no Slash */
    GRACE_NOTE("Grace Note"), 
    /** Trill */
    TR("Trill"), 
    /** Turn with slash */
    TURN_SLASH("Turn with a Slash"), 
    /** Mordent */
    MORDENT("Mordent"), 
    /** Mordent with Slash */
    INVERTED_MORDENT("Mordent with a Slash"), 
    //
    // Tuplets -----------------------------------------------------------------
    //
    /** 3 */
    TUPLET_THREE, 
    /** 6 */
    TUPLET_SIX, 
    /** Pedal down */
    PEDAL_MARK, 
    /** Pedal up */
    PEDAL_UP_MARK, 
    //
    // Miscellaneous -----------------------------------------------------------
    //
    /** Pure clutter */
    CLUTTER("Pure clutter"), 
    /** A letter */
    CHARACTER("A letter"), 
    /** Sequence of letters & spaces */
    TEXT("Sequence of letters & spaces"), 
    //
    // =========================================================================
    // This is the end of physical shapes.
    // Next shapes are pure logical shapes, that CANNOT be inferred only from
    // their physical characteristics.
    // =========================================================================
    //

    //
    // Shapes from shape sets --------------------------------------------------
    //

    /** Repeat dot */
    REPEAT_DOT("Repeat dot", DOT_set), 

    /** Augmentation dot */
    AUGMENTATION_DOT("Augmentation Dot", DOT_set), 

    /** Staccato dot */
    STACCATO("Staccato dot", DOT_set), 
    //
    /** Fermata */
    FERMATA("Fermata", FERMATA_set), 

    /** Fermata Below */
    FERMATA_BELOW("Fermata Below", FERMATA_set), 

    /** Whole Rest */
    WHOLE_REST("Rest for whole measure", HW_REST_set), 

    /** Rest for a 1/2 */
    HALF_REST("Rest for a 1/2", HW_REST_set), 
    /** Digit 6 */
    TIME_SIX("Digit 6", TIME_69_set), 
    /** Digit 9 */
    TIME_NINE("Digit 9", TIME_69_set), 
    /** Single flag down */
    FLAG_1("Single flag down", FLAG_1_set), 

    /** Single flag up */
    FLAG_1_UP("Single flag up", FLAG_1_set), 

    /** Double flag down */
    FLAG_2("Double flag down", FLAG_2_set), 

    /** Double flag up */
    FLAG_2_UP("Double flag up", FLAG_2_set), 

    /** Triple flag down */
    FLAG_3("Triple flag down", FLAG_3_set), 

    /** Triple flag up */
    FLAG_3_UP("Triple flag up", FLAG_3_set), 

    /** Quadruple flag down */
    FLAG_4("Quadruple flag down", FLAG_4_set), 

    /** Quadruple flag up */
    FLAG_4_UP("Quadruple flag up", FLAG_4_set), 

    /** Quintuple flag down */
    FLAG_5("Quintuple flag down", FLAG_5_set), 

    /** Quintuple flag up */
    FLAG_5_UP("Quintuple flag up", FLAG_5_set), 

    /** Crescendo */
    CRESCENDO("Crescendo", WEDGE_set), 
    /** Decrescendo */
    DECRESCENDO("Decrescendo", WEDGE_set), 
    /** Turn */
    TURN("Turn", TURN_set), 
    /** Inverted Turn */
    INVERTED_TURN("Inverted Turn", TURN_set), 
    /** Turn Up */
    TURN_UP("Turn Up", TURN_set), 

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

    /** Thick / Thin bar line + Repeat dots */
    LEFT_REPEAT_SIGN("Thick / Thin bar line + Repeat dots"), 

    /** REPEAT_DOT + Thin / Thick bar line */
    RIGHT_REPEAT_SIGN("Repeat dots + Thin / Thick bar line"), 

    /** Repeat dots + Thin / Thick / Thin + REPEAT_DOT */
    BACK_TO_BACK_REPEAT_SIGN("Repeat dots + Thin / Thick / Thin + REPEAT_DOTS"), 

    /** Alternate ending */
    ENDING("Alternate ending"), 
    //
    // Miscellaneous
    //

    /** Pair of repeat dots */
    REPEAT_DOT_PAIR("Pair of repeat dots"), 
    /** Noise */
    NOISE("Too small stuff"), 
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
    /** Stem */
    STEM("Stem"), 
    //     COMBINING_SPRECHGESANG_STEM,

    //
    // Key signatures ----------------------------------------------------------
    //

    /** One Flat */
    KEY_FLAT_1("One Flat"), 
    /** One Sharp */
    KEY_SHARP_1("One Sharp"), 
    //
    // Other stuff -------------------------------------------------------------
    //
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
    // This is the end of shape enumeration
    // =========================================================================
    //

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Shape.class);

    /** Last physical shape */
    public static final Shape LAST_PHYSICAL_SHAPE = TEXT;

    /** A comparator based on shape name */
    public static Comparator<Shape> alphaComparator = new Comparator<Shape>() {
        @Override
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

    /** Related permanent color */
    private Constant.Color constantColor;

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

        // Create the underlying constant
        constantColor = new Constant.Color(
            getClass().getName(), // Unit
            name() + ".color", // Name
            "#000000", // DefaultValue: Black
            "Color for shape " + name());
    }

    //--------------------------------------------------------------------------

    //---------------//
    // isMeasureRest //
    //---------------//
    /**
     * Check whether the shape is a whole (or multi) rest, for which
     * no duration can be specified.
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
     * Report whether the impact of this shape persists across system
     * (actually measure) borders (clefs, time signatures, key signatures).
     * Based on just the shape, we cannot tell whether an accidental is part of
     * a key signature or not, so we take a conservative approach.
     *
     * @return true if persistent, false otherwise
     */
    public boolean isPersistent ()
    {
        return ShapeSet.Clefs.contains(this) || ShapeSet.Times.contains(this) ||
               ShapeSet.Accidentals.contains(this);
    }

    //--------//
    // isText //
    //--------//
    /**
     * Check whether the shape is a text (or a simple character).
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
     * Check whether the shape is a sharp or a key-sig sequence of
     * sharps.
     * @return true if sharp or sharp key sig
     */
    public boolean isSharpBased ()
    {
        return (this == SHARP) || ShapeSet.SharpKeys.contains(this);
    }

    //-------------//
    // isFlatBased //
    //-------------//
    /**
     * Check whether the shape is a flat or a key-sig sequence of
     * flats.
     * @return true if flat or flat key sig
     */
    public boolean isFlatBased ()
    {
        return (this == FLAT) || ShapeSet.FlatKeys.contains(this);
    }

    //-------------//
    // isTrainable //
    //-------------//
    /**
     * Report whether this shape can be used to train an evaluator.
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
     * Report whether this shape is well known, that is a non-garbage
     * symbol.
     * @return true if non-garbage, false otherwise
     */
    public boolean isWellKnown ()
    {
        return (this != NO_LEGAL_TIME) && (this != GLYPH_PART) &&
               (this != NOISE);
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report a user-friendly description of this shape.
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
     * Report the color assigned to the shape, if any.
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
     * Assign a color for this shape.
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
     * Define a specific color for the shape.
     * @param color the specified color
     */
    public void setConstantColor (Color color)
    {
        constantColor.setValue(color);
        setColor(color);
    }

    //------------------//
    // createShapeColor //
    //------------------//
    void createShapeColor (Color color)
    {
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
     * Report the symbol related to the shape, if any.
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
     * Assign a symbol to this shape.
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
     * Report the symbol to use for menu items.
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
     * Assign a decorated symbol to this shape.
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
     * Report the shape to use for training or precise drawingw.
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
     * Report whether this shape can be dragged (in a DnD gesture).
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
     * Dump the color of every shape.
     */
    public static void dumpShapeColors ()
    {
        List<String> names = new ArrayList<>();

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
