//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S h a p e                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.Constant;

import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code Shape} defines the comprehensive list of glyph shapes.
 * <p>
 * It is organized according to the Unicode Standard 4.0, with a few addition for convenience only.
 * <p>
 * The enumeration begins with physical shapes (which are the only ones usable for training) and
 * ends with the logical shapes. The method {@link #isTrainable()} can be used to disambiguate
 * between physical and logical shapes.
 * <p>
 * <b>Nota</b>: All the physical shapes <b>MUST</b> have different characteristics for the training
 * to work correctly.
 * The ART evaluator uses moments that are invariant to translation, scaling
 * and rotation (and to symmetry as well).
 * Shapes that exhibit some symmetry (like FERMATA vs FERMATA_BELOW) would be considered as the same
 * shape by the ART evaluator.
 * Therefore, the strategy is to define a single shape (FERMATA_set) for the evaluator, leaving the
 * final disambiguation between FERMATA_BELOW and FERMATA to tests performed beyond the ART
 * evaluator.
 * FERMATA_set belongs to the physical shapes, while FERMATA_BELOW and FERMATA belong to the logical
 * shapes.
 * All shapes whose name ends with "_set" are in this case.
 * <p>
 * TODO: Perhaps we could simply combine the ART moments and some of the GEO moments (at least n11,
 * n12, n21), plus perhaps aspect, for a more comprehensive set of features but a simpler approach.
 * <p>
 * As far as possible, a symbol should be generated for every shape.
 * <p>
 * A shape may have a related "decorated" symbol. For example the BREVE_REST is similar to a black
 * rectangle which is used for training / recognition and the related symbol is used for drawing in
 * score view. However, in menu items, it is displayed as a black rectangle surrounded by a staff
 * line above and a staff line below.
 * The method {@link #getDecoratedSymbol()} returns the symbol to use in menu items.
 *
 * @author Hervé Bitteur
 */
public enum Shape
{

    /**
     * =================================================================================
     * Nota: Avoid changing the order of these physical shapes, otherwise the evaluators
     * won't detect this and you'll have to retrain them on your own.
     * =============================================================================================
     */

    //
    // Sets --------------------------------------------------------------------
    //
    DOT_set("Dot set", new Color(0x0cccc)),
    HW_REST_set("Half & Whole Rest set"),

    //
    // Bars --------------------------------------------------------------------
    //
    DAL_SEGNO("Repeat from the sign"),
    DA_CAPO("Repeat from the beginning"),
    SEGNO("Sign"),
    CODA("Closing section"),
    BREATH_MARK("Breath Mark"),
    CAESURA("Caesura"),
    BRACE("Brace"),
    BRACKET("Bracket"),
    FERMATA("Fermata"),
    FERMATA_BELOW("Fermata Below"),

    //
    // Clefs -------------------------------------------------------------------
    //
    G_CLEF("Treble Clef", new Color(0xff99ff)),
    G_CLEF_SMALL("Small Treble Clef", new Color(0xff99ff)),
    G_CLEF_8VA("Treble Clef Ottava Alta", new Color(0xff99ff)),
    G_CLEF_8VB("Treble Clef Ottava Bassa", new Color(0xff99ff)),
    C_CLEF("Ut Clef", new Color(0xff99ff)),
    F_CLEF("Bass Clef"),
    F_CLEF_SMALL("Small Bass Clef"),
    F_CLEF_8VA("Bass Clef Ottava Alta"),
    F_CLEF_8VB("Bass Clef Ottava Bassa"),
    PERCUSSION_CLEF("Percussion Clef"),

    //
    // Alterations -------------------------------------------------------------
    //
    FLAT("Minus one half step", new Color(0x00aaaa)),
    NATURAL("Natural value", new Color(0x0066ff)),
    SHARP("Plus one half step", new Color(0x3399ff)),
    DOUBLE_SHARP("Double Sharp"),
    DOUBLE_FLAT("Double Flat"),
    //
    // Time --------------------------------------------------------------------
    //
    TIME_ZERO("Digit 0"),
    TIME_ONE("Digit 1"),
    TIME_TWO("Digit 2"),
    TIME_THREE("Digit 3"),
    TIME_FOUR("Digit 4"),
    TIME_FIVE("Digit 5"),
    TIME_SIX("Digit 6"),
    TIME_SEVEN("Digit 7"),
    TIME_EIGHT("Digit 8"),
    TIME_NINE("Digit 9"),
    TIME_TWELVE("Number 12"),
    TIME_SIXTEEN("Number 16"),
    TIME_FOUR_FOUR("Rational 4/4"),
    TIME_TWO_TWO("Rational 2/2"),
    TIME_TWO_FOUR("Rational 2/4"),
    TIME_THREE_FOUR("Rational 3/4"),
    TIME_FIVE_FOUR("Rational 5/4"),
    TIME_SIX_EIGHT("Rational 6/8"),
    COMMON_TIME("Alpha = 4/4", new Color(0xcc6600)),
    CUT_TIME("Semi-Alpha = 2/2"),
    OTTAVA_ALTA("8 va", new Color(0xcc66ff)),
    OTTAVA_BASSA("8 vb", new Color(0xcc66ff)),

    //
    // Rests -------------------------------------------------------------------
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
    // Flags -------------------------------------------------------------------
    //
    FLAG_1("Single flag down"),
    FLAG_1_UP("Single flag up"),
    FLAG_2("Double flag down"),
    FLAG_2_UP("Double flag up"),
    FLAG_3("Triple flag down"),
    FLAG_3_UP("Triple flag up"),
    FLAG_4("Quadruple flag down"),
    FLAG_4_UP("Quadruple flag up"),
    FLAG_5("Quintuple flag down"),
    FLAG_5_UP("Quintuple flag up"),

    //
    // Notes -------------------------------------------------------------------
    //
    BREVE("Double Whole"),
    //
    // Articulation ------------------------------------------------------------
    //
    ACCENT,
    TENUTO,
    STACCATISSIMO,
    STRONG_ACCENT("Marcato"),
    ARPEGGIATO,

    //
    // Dynamics ----------------------------------------------------------------
    //
    DYNAMICS_CHAR_M("m character"),
    DYNAMICS_CHAR_R("r character"),
    DYNAMICS_CHAR_S("c character"),
    DYNAMICS_CHAR_Z("z character"),
    DYNAMICS_F("Forte"),
    DYNAMICS_FF("Fortissimo"),
    DYNAMICS_FFF("Fortississimo"),
    DYNAMICS_FP("FortePiano"),
    DYNAMICS_FZ("Forzando"),
    DYNAMICS_MF("Mezzo forte"),
    DYNAMICS_MP("Mezzo piano"),
    DYNAMICS_P("Piano"),
    DYNAMICS_PP("Pianissimo"),
    DYNAMICS_PPP("Pianississimo"),
    DYNAMICS_RF,
    DYNAMICS_RFZ("Rinforzando"),
    DYNAMICS_SF,
    DYNAMICS_SFFZ,
    DYNAMICS_SFP("Subito fortepiano"),
    DYNAMICS_SFPP,
    DYNAMICS_SFZ("Sforzando"),

    //
    // Ornaments ---------------------------------------------------------------
    //
    GRACE_NOTE_SLASH("Grace Note with a Slash"),
    GRACE_NOTE("Grace Note with no slash"),
    TR("Trill"),
    TURN("Turn"),
    TURN_INVERTED("Inverted Turn"),
    TURN_UP("Turn Up"),
    TURN_SLASH("Turn with a Slash"),
    MORDENT("Mordent"),
    MORDENT_INVERTED("Mordent with a Slash"),

    //
    // Tuplets -----------------------------------------------------------------
    //
    TUPLET_THREE("3", new Color(0xcc00cc)),
    TUPLET_SIX("6", new Color(0xcc00cc)),
    PEDAL_MARK("Pedal down", new Color(0x009999)),
    PEDAL_UP_MARK("Pedal downup", new Color(0x009999)),
    //
    // Small digits ------------------------------------------------------------
    //
    DIGIT_0("Digit 0"),
    DIGIT_1("Digit 1"),
    DIGIT_2("Digit 2"),
    DIGIT_3("Digit 3"),
    DIGIT_4("Digit 4"),
    DIGIT_5("Digit 5"),
    DIGIT_6("Digit 6"),
    DIGIT_7("Digit 7"),
    DIGIT_8("Digit 8"),
    DIGIT_9("Digit 9"),

    //
    // Miscellaneous -----------------------------------------------------------
    //
    CHARACTER("Any letter"),
    TEXT("Sequence of letters & spaces", new Color(0x9999ff)),
    CLUTTER("Pure clutter", new Color(0x999900)),

    //
    // =============================================================================================
    // This is the end of physical shapes.
    // Next shapes are logical shapes, that CANNOT be inferred only from their physical characteristics.
    // =============================================================================================
    //
    //
    // Shapes from shape sets --------------------------------------------------
    //
    REPEAT_DOT("Repeat dot", DOT_set, new Color(0x0000ff)),
    AUGMENTATION_DOT("Augmentation Dot", DOT_set),
    STACCATO("Staccato dot", DOT_set),
    WHOLE_REST("Rest for whole measure", HW_REST_set),
    HALF_REST("Rest for a 1/2", HW_REST_set),
    CRESCENDO("Crescendo"),
    DECRESCENDO("Decrescendo"),

    //
    // Noteheads ---------------------------------------------------------------
    //
    NOTEHEAD_VOID("Hollow node head for halves", new Color(0xff9966)),
    NOTEHEAD_VOID_SMALL("Small hollow note head for grace or cue", new Color(0xff4400)),
    NOTEHEAD_BLACK("Filled node head for quarters and less", new Color(0xffcc00)),
    NOTEHEAD_BLACK_SMALL("Small filled note head for grace or cue", new Color(0xff4400)),

    //
    // Notes -------------------------------------------------------------------
    //
    WHOLE_NOTE("Hollow node head for wholes"),
    WHOLE_NOTE_SMALL("Small hollow node head for grace or cue wholes"),

    //
    // Beams and slurs ---------------------------------------------------------
    //
    BEAM("Beam between two stems"),
    BEAM_SMALL("Small beam for cue notes"),
    BEAM_HOOK("Hook of a beam attached on one stem"),
    BEAM_HOOK_SMALL("Small hook of a beam for cue notes"),
    SLUR("Slur tying notes", new Color(0xff4444)),

    //
    // Key signatures ----------------------------------------------------------
    //
    KEY_FLAT_7("Seven Flats"),
    KEY_FLAT_6("Six Flats"),
    KEY_FLAT_5("Five Flats"),
    KEY_FLAT_4("Four Flats"),
    KEY_FLAT_3("Three Flats"),
    KEY_FLAT_2("Two Flats"),
    KEY_SHARP_2("Two Sharps"),
    KEY_SHARP_3("Three Sharps"),
    KEY_SHARP_4("Four Sharps"),
    KEY_SHARP_5("Five Sharps"),
    KEY_SHARP_6("Six Sharps"),
    KEY_SHARP_7("Seven Sharps"),

    //
    // Bars --------------------------------------------------------------------
    //
    PART_DEFINING_BARLINE("Bar line that defines a part"),
    THIN_BARLINE("Thin bar line"),
    THIN_CONNECTION("Connection between thin barlines"),
    THICK_BARLINE("Thick bar line"),
    THICK_CONNECTION("Connection between thick barlines"),
    DOUBLE_BARLINE("Double thin bar line"),
    FINAL_BARLINE("Thin / Thick bar line"),
    REVERSE_FINAL_BARLINE("Thick / Thin bar line"),
    LEFT_REPEAT_SIGN("Thick / Thin bar line + Repeat dots"),
    RIGHT_REPEAT_SIGN("Repeat dots + Thin / Thick bar line"),
    BACK_TO_BACK_REPEAT_SIGN("Repeat dots + Thin / Thick / Thin + REPEAT_DOTS"),
    ENDING("Alternate ending", new Color(0x993300)),
    //
    // Miscellaneous
    //
    REPEAT_DOT_PAIR("Pair of repeat dots"),
    NOISE("Too small stuff", new Color(0xcccccc)),
    STAFF_LINE("Staff Line", new Color(0xffffcc)),
    LEDGER("Ledger", new Color(0xaaaaaa)),
    LEDGER_CANDIDATE("Ledger candidate", new Color(0xaaffaa)),
    ENDING_HORIZONTAL("Horizontal part of ending"),
    ENDING_VERTICAL("Vertical part of ending"),
    SEGMENT("Wedge or ending segment"),
    //
    // Stems
    //
    STEM("Stem", new Color(0xccff66)),
    VERTICAL_SEED("Vertical seed", new Color(0xccffcc)),

    //
    // Key signatures ----------------------------------------------------------
    //
    KEY_FLAT_1("One Flat"),
    KEY_SHARP_1("One Sharp"),
    //
    // Other stuff -------------------------------------------------------------
    //
    FORWARD("To indicate a forward"),
    NON_DRAGGABLE("Non draggable shape"),
    GLYPH_PART("Part of a larger glyph"),
    CUSTOM_TIME("Time signature defined by user"),
    NO_LEGAL_TIME("No Legal Time Shape"),
    BEAM_SPOT("Beam-oriented spot", new Color(0xaaaaaa));

    //
    // =============================================================================================
    // This is the end of shape enumeration
    // =============================================================================================
    //
    private static final Logger logger = LoggerFactory.getLogger(Shape.class);

    /** Last physical shape */
    public static final Shape LAST_PHYSICAL_SHAPE = CLUTTER;

    /** A comparator based on shape name */
    public static Comparator<Shape> alphaComparator = new Comparator<Shape>()
    {
        @Override
        public int compare (Shape o1,
                            Shape o2)
        {
            return o1.name().compareTo(o2.name());
        }
    };

    //
    /** Explanation of the glyph shape */
    private final String description;

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

    /** Related color */
    private Color color;

    /** Related color constant */
    private Constant.Color constantColor;

    //--------------------------------------------------------------------------
    //
    //-------//
    // Shape //
    //-------//
    Shape ()
    {
        this("", null, null);
    }

    //-------//
    // Shape //
    //-------//
    Shape (String description)
    {
        this(description, null, null);
    }

    //-------//
    // Shape //
    //-------//
    Shape (String description,
           Color color)
    {
        this(description, null, color);
    }

    //-------//
    // Shape //
    //-------//
    Shape (String description,
           Shape physicalShape)
    {
        this(description, physicalShape, null);
    }

    //-------//
    // Shape //
    //-------//
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

    //--------------------------------------------------------------------------
    //---------------//
    // isMeasureRest //
    //---------------//
    /**
     * Check whether the shape is a whole (or multi) rest, for which
     * no duration can be specified.
     *
     * @return true if whole or multi rest
     */
    public boolean isMeasureRest ()
    {
        return (this == WHOLE_REST) || (this == BREVE_REST) || (this == LONG_REST);
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
        return ShapeSet.Clefs.contains(this) || ShapeSet.Times.contains(this)
               || ShapeSet.Alterations.contains(this);
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

    //--------------//
    // isSharpBased //
    //--------------//
    /**
     * Check whether the shape is a sharp or a key-sig sequence of
     * sharps.
     *
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
     *
     * @return true if flat or flat key sig
     */
    public boolean isFlatBased ()
    {
        return (this == FLAT) || ShapeSet.FlatKeys.contains(this);
    }

    //---------//
    // isSmall //
    //---------//
    /**
     * Check whether the shape is a small note, meant for cue or grace.
     *
     * @return true if small (black/void/whole)
     */
    public boolean isSmall ()
    {
        return ShapeSet.SmallNotes.contains(this);
    }

    //-------------//
    // isTrainable //
    //-------------//
    /**
     * Report whether this shape can be used to train an evaluator.
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
     * Report whether this shape is well known, that is a non-garbage
     * symbol.
     *
     * @return true if non-garbage, false otherwise
     */
    public boolean isWellKnown ()
    {
        return (this != NO_LEGAL_TIME) && (this != GLYPH_PART) && (this != NOISE);
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

    //-----------//
    // getSymbol //
    //-----------//
    /**
     * Report the symbol related to the shape, if any.
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
     * Assign a symbol to this shape.
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
     * Report the symbol to use for menu items.
     *
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

    //-------------//
    // isDraggable //
    //-------------//
    /**
     * Report whether this shape can be dragged (in a DnD gesture).
     *
     * @return true if draggable
     */
    public boolean isDraggable ()
    {
        return getPhysicalShape().getSymbol() != null;
    }

    //-----------------//
    // dumpShapeColors //
    //-----------------//
    /**
     * Dump the color of every shape.
     */
    public static void dumpShapeColors ()
    {
        List<String> names = new ArrayList<String>();

        for (Shape shape : Shape.values()) {
            names.add(shape + " " + Constant.Color.encodeColor(shape.getColor()));
        }

        Collections.sort(names);

        for (String str : names) {
            System.out.println(str);
        }
    }
}
