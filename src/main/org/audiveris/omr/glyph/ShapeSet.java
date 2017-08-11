//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S h a p e S e t                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
import org.audiveris.omr.constant.ConstantSet;
import static org.audiveris.omr.glyph.Shape.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Class {@code ShapeSet} defines a set of related shapes, for example the "Rests" set
 * gathers all rest shapes from MULTI_REST down to ONE_128TH_REST.
 * <p>
 * It handles additional properties over a simple EnumSet, especially assigned colors and its
 * automatic insertion in shape menus and palette hierarchy.
 * So don't remove any of the ShapeSet's, unless you know what you are doing.
 */
public class ShapeSet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ShapeSet.class);

    /** Half time numbers. These shapes are used for upper or lower part of a time signature.
     */
    public static final List<Shape> PartialTimes = Arrays.asList(
            TIME_TWO,
            TIME_THREE,
            TIME_FOUR,
            TIME_FIVE,
            TIME_SIX,
            TIME_SEVEN,
            TIME_EIGHT,
            TIME_NINE,
            TIME_TWELVE,
            TIME_SIXTEEN);

    /** Measure counts.
     * These time-looking shapes may appear right above a staff containing just a long measure rest,
     * to indicate the number of measures the rest represents.
     */
    public static final List<Shape> MeasureCounts = Arrays.asList(
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
            TIME_SIXTEEN);

    /** Single-symbols for whole time signature */
    public static final EnumSet<Shape> SingleWholeTimes = EnumSet.of(COMMON_TIME, CUT_TIME);

    /** Single-symbols and predefined combos for whole time signature */
    public static final EnumSet<Shape> WholeTimes = EnumSet.of(
            COMMON_TIME,
            CUT_TIME,
            TIME_FOUR_FOUR,
            TIME_TWO_TWO,
            TIME_TWO_FOUR,
            TIME_THREE_FOUR,
            TIME_FIVE_FOUR,
            TIME_THREE_EIGHT,
            TIME_SIX_EIGHT);

    /** All sorts of F clefs */
    public static final EnumSet<Shape> BassClefs = EnumSet.of(
            F_CLEF,
            F_CLEF_SMALL,
            F_CLEF_8VA,
            F_CLEF_8VB);

    /** All sorts of G clefs */
    public static final EnumSet<Shape> TrebleClefs = EnumSet.of(
            G_CLEF,
            G_CLEF_SMALL,
            G_CLEF_8VA,
            G_CLEF_8VB);

    /** All flags down */
    public static final EnumSet<Shape> FlagsDown = EnumSet.of(
            FLAG_1,
            FLAG_2,
            FLAG_3,
            FLAG_4,
            FLAG_5);

    /** Small flags */
    public static final EnumSet<Shape> SmallFlags = EnumSet.of(SMALL_FLAG, SMALL_FLAG_SLASH);

    /** All flags up */
    public static final EnumSet<Shape> FlagsUp = EnumSet.of(
            FLAG_1_UP,
            FLAG_2_UP,
            FLAG_3_UP,
            FLAG_4_UP,
            FLAG_5_UP);

    /** All SHARP-based keys */
    public static final EnumSet<Shape> SharpKeys = EnumSet.of(
            KEY_SHARP_1,
            KEY_SHARP_2,
            KEY_SHARP_3,
            KEY_SHARP_4,
            KEY_SHARP_5,
            KEY_SHARP_6,
            KEY_SHARP_7);

    /** All FLAT-based keys */
    public static final EnumSet<Shape> FlatKeys = EnumSet.of(
            KEY_FLAT_1,
            KEY_FLAT_2,
            KEY_FLAT_3,
            KEY_FLAT_4,
            KEY_FLAT_5,
            KEY_FLAT_6,
            KEY_FLAT_7);

    /** All black note heads */
    public static final EnumSet<Shape> BlackNoteHeads = EnumSet.of(
            NOTEHEAD_BLACK,
            NOTEHEAD_BLACK_SMALL);

    /** All void note heads */
    public static final EnumSet<Shape> VoidNoteHeads = EnumSet.of(
            NOTEHEAD_VOID,
            NOTEHEAD_VOID_SMALL);

    /** All supported small notes. (for cue/grace) */
    public static final EnumSet<Shape> SmallNotes = EnumSet.noneOf(Shape.class);

    static {
        if (supportSmallHeadNotes()) {
            SmallNotes.add(NOTEHEAD_BLACK_SMALL);
        }

        if (supportSmallVoidNotes()) {
            SmallNotes.add(NOTEHEAD_VOID_SMALL);
        }

        if (supportSmallWholeNotes()) {
            SmallNotes.add(WHOLE_NOTE_SMALL);
        }
    }

    /** All heads without a stem. */
    public static final EnumSet<Shape> StemLessHeads = EnumSet.of(
            BREVE,
            WHOLE_NOTE,
            WHOLE_NOTE_SMALL);

    /** All heads with a stem. */
    public static final EnumSet<Shape> StemHeads = EnumSet.of(
            NOTEHEAD_BLACK,
            NOTEHEAD_BLACK_SMALL,
            NOTEHEAD_VOID,
            NOTEHEAD_VOID_SMALL);

    /** All heads. */
    public static final List<Shape> Heads = Arrays.asList(
            BREVE,
            WHOLE_NOTE,
            WHOLE_NOTE_SMALL,
            NOTEHEAD_BLACK,
            NOTEHEAD_BLACK_SMALL,
            NOTEHEAD_VOID,
            NOTEHEAD_VOID_SMALL);

    /** All notes handled by template matching. */
    public static final EnumSet<Shape> TemplateNotes = EnumSet.noneOf(Shape.class);

    static {
        TemplateNotes.addAll(Arrays.asList(NOTEHEAD_BLACK, NOTEHEAD_VOID, WHOLE_NOTE));

        if (supportSmallHeadNotes()) {
            TemplateNotes.add(NOTEHEAD_BLACK_SMALL);
        }

        if (supportSmallVoidNotes()) {
            TemplateNotes.add(NOTEHEAD_VOID_SMALL);
        }

        if (supportSmallWholeNotes()) {
            TemplateNotes.add(WHOLE_NOTE_SMALL);
        }
    }

    /** All stem-based notes handled by template matching. */
    public static final EnumSet<Shape> StemTemplateNotes = EnumSet.noneOf(Shape.class);

    static {
        StemTemplateNotes.addAll(Arrays.asList(NOTEHEAD_BLACK, NOTEHEAD_VOID));

        if (supportSmallHeadNotes()) {
            StemTemplateNotes.add(NOTEHEAD_BLACK_SMALL);
        }

        if (supportSmallVoidNotes()) {
            StemTemplateNotes.add(NOTEHEAD_VOID_SMALL);
        }
    }

    /** All black notes handled by template matching. */
    public static final EnumSet<Shape> BlackTemplateNotes = EnumSet.noneOf(Shape.class);

    static {
        BlackTemplateNotes.addAll(Arrays.asList(NOTEHEAD_BLACK));

        if (supportSmallHeadNotes()) {
            BlackTemplateNotes.add(NOTEHEAD_BLACK_SMALL);
        }
    }

    /** All void notes handled by template matching. */
    public static final EnumSet<Shape> VoidTemplateNotes = EnumSet.noneOf(Shape.class);

    static {
        VoidTemplateNotes.addAll(Arrays.asList(NOTEHEAD_VOID, WHOLE_NOTE));

        if (supportSmallVoidNotes()) {
            VoidTemplateNotes.add(NOTEHEAD_VOID_SMALL);
        }
    }

    /** FermataArcs. */
    public static final EnumSet<Shape> FermataArcs = EnumSet.of(FERMATA_ARC, FERMATA_ARC_BELOW);

    /** Core shapes for barlines. */
    public static final EnumSet<Shape> CoreBarlines = EnumSet.copyOf(
            Arrays.asList(
                    Shape.THICK_BARLINE,
                    Shape.THICK_CONNECTOR,
                    Shape.THIN_BARLINE,
                    Shape.THIN_CONNECTOR));

    //----------------------------------------------------------------------------------------------
    // Below are predefined instances of ShapeSet, meant mainly for UI packaging.
    //
    // By being defined here, they will lead to corresponding gathering in user menus & palette.
    // So, double-check before removing or modifying any one of them.
    //
    // Nota: Do not use EnumSet.range() since this could lead to subtle errors should Shape enum
    // order be modified. Prefer the use of shapesOf() which lists precisely all set members.
    //----------------------------------------------------------------------------------------------
    public static final ShapeSet Accidentals = new ShapeSet(
            SHARP,
            new Color(0x9933ff),
            shapesOf(FLAT, NATURAL, SHARP, DOUBLE_SHARP, DOUBLE_FLAT));

    public static final ShapeSet Articulations = new ShapeSet(
            ACCENT,
            new Color(0xff6699),
            shapesOf(ACCENT, TENUTO, STACCATO, STACCATISSIMO, STRONG_ACCENT));

    public static final ShapeSet Attributes = new ShapeSet(
            PEDAL_MARK,
            new Color(0x000000),
            shapesOf(
                    OTTAVA_ALTA,
                    OTTAVA_BASSA,
                    PEDAL_MARK,
                    PEDAL_UP_MARK,
                    TUPLET_THREE,
                    TUPLET_SIX,
                    ARPEGGIATO));

    public static final ShapeSet Barlines = new ShapeSet(
            LEFT_REPEAT_SIGN,
            new Color(0x0000ff),
            shapesOf(
                    CoreBarlines,
                    shapesOf(
                            DOUBLE_BARLINE,
                            FINAL_BARLINE,
                            REVERSE_FINAL_BARLINE,
                            LEFT_REPEAT_SIGN,
                            RIGHT_REPEAT_SIGN,
                            BACK_TO_BACK_REPEAT_SIGN,
                            BRACE,
                            BRACKET,
                            BRACKET_CONNECTOR,
                            REPEAT_DOT)));

    public static final ShapeSet Beams = new ShapeSet(
            BEAM,
            new Color(0x33ffff),
            shapesOf(BEAM, BEAM_SMALL, BEAM_HOOK));

    public static final ShapeSet Clefs = new ShapeSet(
            G_CLEF,
            new Color(0xff99ff),
            shapesOf(TrebleClefs, BassClefs, shapesOf(C_CLEF, PERCUSSION_CLEF)));

    /**
     * <img src="http://0.tqn.com/y/piano/1/U/m/G/-/-/musical-dynamics_MUSIC.png">
     * <br>
     * see <a href="http://piano.about.com/od/musicaltermssymbols/ss/2Int_SheetMusic_2.htm">Musical
     * Dynamics</a>
     */
    public static final ShapeSet Dynamics = new ShapeSet(
            DYNAMICS_F,
            new Color(0x009999),
            shapesOf(
                    DYNAMICS_P,
                    DYNAMICS_PP,
                    DYNAMICS_MP,
                    DYNAMICS_F,
                    DYNAMICS_FF,
                    DYNAMICS_MF,
                    DYNAMICS_FP,
                    DYNAMICS_SF,
                    DYNAMICS_SFZ,
                    CRESCENDO,
                    DIMINUENDO));

    public static final ShapeSet Flags = new ShapeSet(
            FLAG_1,
            new Color(0x99cc00),
            shapesOf(new ArrayList<Shape>(FlagsDown), SmallFlags, FlagsUp));

    public static final ShapeSet Holds = new ShapeSet(
            FERMATA,
            new Color(0x888888),
            shapesOf(BREATH_MARK, CAESURA, FERMATA, FERMATA_BELOW));

    public static final ShapeSet Keys = new ShapeSet(
            KEY_SHARP_3,
            new Color(0x00ffff),
            shapesOf(new ArrayList<Shape>(FlatKeys), SharpKeys));

    public static final ShapeSet HeadsAndDot = new ShapeSet(
            NOTEHEAD_BLACK,
            new Color(0xffcc00),
            shapesOf(Heads, shapesOf(AUGMENTATION_DOT)));

    public static final ShapeSet Markers = new ShapeSet(
            CODA,
            new Color(0x888888),
            shapesOf(DAL_SEGNO, DA_CAPO, SEGNO, CODA));

    public static final ShapeSet Ornaments = new ShapeSet(
            MORDENT,
            new Color(0xcc3300),
            shapesOf(
                    GRACE_NOTE_SLASH,
                    GRACE_NOTE,
                    TR,
                    TURN,
                    TURN_INVERTED,
                    TURN_UP,
                    TURN_SLASH,
                    MORDENT,
                    MORDENT_INVERTED));

    public static final ShapeSet Rests = new ShapeSet(
            QUARTER_REST,
            new Color(0x99ff66),
            shapesOf(
                    LONG_REST,
                    BREVE_REST,
                    WHOLE_REST,
                    HALF_REST,
                    QUARTER_REST,
                    EIGHTH_REST,
                    ONE_16TH_REST,
                    ONE_32ND_REST,
                    ONE_64TH_REST,
                    ONE_128TH_REST));

    public static final ShapeSet Times = new ShapeSet(
            TIME_FOUR_FOUR,
            new Color(0xcc3300),
            shapesOf(PartialTimes, WholeTimes)); //, shapesOf(TIME_ZERO, TIME_ONE, CUSTOM_TIME)));

    public static final ShapeSet Digits = new ShapeSet(
            DIGIT_1,
            new Color(0xcc3388),
            shapesOf(
                    DIGIT_0,
                    DIGIT_1,
                    DIGIT_2,
                    DIGIT_3,
                    DIGIT_4 //                    ,
            //                    DIGIT_5,
            //                    DIGIT_6,
            //                    DIGIT_7,
            //                    DIGIT_8,
            //                    DIGIT_9
            ));

    public static final ShapeSet Pluckings = new ShapeSet(
            PLUCK_P,
            new Color(0xcc3388),
            shapesOf(PLUCK_P, PLUCK_I, PLUCK_M, PLUCK_A));

    public static final ShapeSet Romans = new ShapeSet(
            ROMAN_V,
            new Color(0xcc3388),
            shapesOf(
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
                    ROMAN_XII));

    public static final ShapeSet Physicals = new ShapeSet(
            LEDGER,
            new Color(0x9999ff),
            shapesOf(TEXT, CHARACTER, CLUTTER, SLUR, LEDGER, STEM, ENDING));

    // =========================================================================
    // Below are EnumSet instances, used programmatically.
    // They do not lead to shape submenus as the ShapeSet instances do.
    // =========================================================================
    /** All physical shapes. Here the use of EnumSet.range is OK */
    public static final EnumSet<Shape> allPhysicalShapes = EnumSet.range(
            Shape.values()[0],
            LAST_PHYSICAL_SHAPE);

    /** Symbols that can be attached to a stem. */
    public static final EnumSet<Shape> StemSymbols = EnumSet.copyOf(
            shapesOf(StemHeads, Flags.getShapes(), Beams.getShapes()));

    /** Pedals */
    public static final EnumSet<Shape> Pedals = EnumSet.of(PEDAL_MARK, PEDAL_UP_MARK);

    /** Tuplets */
    public static final EnumSet<Shape> Tuplets = EnumSet.of(TUPLET_THREE, TUPLET_SIX);

    /** All variants of dot */
    public static final EnumSet<Shape> Dots = EnumSet.of(
            DOT_set,
            AUGMENTATION_DOT,
            STACCATO,
            REPEAT_DOT);

    /** Clefs ottava (alta or bassa) */
    public static final EnumSet<Shape> OttavaClefs = EnumSet.of(
            G_CLEF_8VA,
            G_CLEF_8VB,
            F_CLEF_8VA,
            F_CLEF_8VB);

    /** Small Clefs */
    public static final EnumSet<Shape> SmallClefs = EnumSet.of(G_CLEF_SMALL, F_CLEF_SMALL);

    static {
        // Make sure all the shape colors are defined
        ShapeSet.defineAllShapeColors();

        // Debug
        ///dumpShapeColors();
    }

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Name of the set. */
    private String name;

    /** Underlying shapes. */
    private final EnumSet<Shape> shapes;

    /** Specific sequence of shapes, if any. */
    private final List<Shape> sortedShapes;

    /** The representative shape for this set. */
    private final Shape rep;

    /** Assigned color. */
    private Color color;

    /** Related color constant. */
    private Constant.Color constantColor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ShapeSet object from a collection of shapes.
     *
     * @param rep    the representative shape
     * @param color  the default color assigned
     * @param shapes the provided collection of shapes
     */
    public ShapeSet (Shape rep,
                     Color color,
                     Collection<Shape> shapes)
    {
        // The representative shape
        this.rep = rep;

        // The default color
        this.color = (color != null) ? color : Color.BLACK;

        // The set of shapes
        this.shapes = EnumSet.noneOf(Shape.class);
        this.shapes.addAll(shapes);

        // Keep a specific order?
        if (shapes instanceof List) {
            this.sortedShapes = new ArrayList<Shape>(shapes);
        } else {
            this.sortedShapes = null;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // addAllShapeSets //
    //-----------------//
    /**
     * Populate the given menu with all ShapeSet instances defined
     * in this class.
     *
     * @param top      the JComponent to populate (typically a JMenu or a
     *                 JPopupMenu)
     * @param listener the listener for notification of user selection
     */
    public static void addAllShapeSets (JComponent top,
                                        ActionListener listener)
    {
        // All ranges of glyph shapes
        for (Field field : ShapeSet.class.getDeclaredFields()) {
            if (field.getType() == ShapeSet.class) {
                JMenuItem menuItem = new JMenuItem(field.getName());
                ShapeSet set = valueOf(field.getName());
                addColoredItem(top, menuItem, set.getColor());
                menuItem.addActionListener(listener);
            }
        }
    }

    //--------------//
    // addAllShapes //
    //--------------//
    /**
     * Populate the given menu with a hierarchy of all shapes,
     * organized by defined ShapeSets.
     *
     * @param top      the JComponent to populate (typically a JMenu or a JPopupMenu)
     * @param listener the listener for notification of user selection
     */
    public static void addAllShapes (JComponent top,
                                     ActionListener listener)
    {
        // All ranges of glyph shapes
        for (Field field : ShapeSet.class.getDeclaredFields()) {
            if (field.getType() == ShapeSet.class) {
                ShapeSet set = ShapeSet.valueOf(field.getName());
                JMenu menu = new JMenu(field.getName());

                if (set.rep != null) {
                    menu.setIcon(set.rep.getDecoratedSymbol());
                }

                addColoredItem(top, menu, Color.black);

                // Add menu items for this range
                addSetShapes(set, menu, listener);
            }
        }
    }

    //--------------//
    // addSetShapes //
    //--------------//
    /**
     * Populate the given menu with a list of all shapes that belong
     * to the given ShapeSet.
     *
     * @param set      the set for which shape menu items must be buit
     * @param top      the JComponent to populate (typically a JMenu or a
     *                 JPopupMenu)
     * @param listener the listener for notification of user selection
     */
    public static void addSetShapes (ShapeSet set,
                                     JComponent top,
                                     ActionListener listener)
    {
        // All shapes in the given range
        for (Shape shape : set.getSortedShapes()) {
            JMenuItem menuItem = new JMenuItem(shape.toString(), shape.getDecoratedSymbol());
            addColoredItem(top, menuItem, shape.getColor());

            menuItem.setToolTipText(shape.getDescription());
            menuItem.addActionListener(listener);
        }
    }

    //-----------------------//
    // getPhysicalShapeNames //
    //-----------------------//
    /**
     * Report the names of all the physical shapes.
     *
     * @return the array of names for shapes up to LAST_PHYSICAL_SHAPE
     */
    public static String[] getPhysicalShapeNames ()
    {
        int shapeCount = 1 + LAST_PHYSICAL_SHAPE.ordinal();
        String[] names = new String[shapeCount];

        for (Shape shape : allPhysicalShapes) {
            names[shape.ordinal()] = shape.name();
        }

        return names;
    }

    //-----------------------------//
    // getPhysicalShapeNamesString //
    //-----------------------------//
    /**
     * Report a formatted string with the names of all the physical shapes.
     *
     * @return a global string
     */
    public static String getPhysicalShapeNamesString ()
    {
        final List<String> names = Arrays.asList(getPhysicalShapeNames());
        StringBuilder sb = new StringBuilder("{ //\n");

        for (int i = 0; i < names.size(); i++) {
            String comma = (i < (names.size() - 1)) ? "," : "";
            sb.append(String.format("\"%-18s // %3d\n", names.get(i) + "\"" + comma, i));
        }

        sb.append("};");

        return sb.toString();
    }

    //----------//
    // contains //
    //----------//
    /**
     * Convenient method to check if encapsulated shapes set does
     * contain the provided object.
     *
     * @param shape the Shape object to check for inclusion
     * @return true if contained, false otherwise
     */
    public boolean contains (Shape shape)
    {
        return shapes.contains(shape);
    }

    //----------//
    // getColor //
    //----------//
    /**
     * Report the color currently assigned to the range, if any.
     *
     * @return the related color, or null
     */
    public Color getColor ()
    {
        return color;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name of the set.
     *
     * @return the set name
     */
    public String getName ()
    {
        return name;
    }

    //--------//
    // getRep //
    //--------//
    /**
     * Report the representative shape of the set, if any.
     *
     * @return the rep shape, or null
     */
    public Shape getRep ()
    {
        return rep;
    }

    //-------------//
    // getShapeSet //
    //-------------//
    public static ShapeSet getShapeSet (String name)
    {
        return Sets.map.get(name);
    }

    //--------------//
    // getShapeSets //
    //--------------//
    public static List<ShapeSet> getShapeSets ()
    {
        return Sets.setList;
    }

    //----------//
    // shapesOf //
    //----------//
    /**
     * Convenient way to build a collection of shapes.
     *
     * @param col a collection of shapes
     * @return a single collection
     */
    public static Collection<Shape> shapesOf (Collection<Shape> col)
    {
        Collection<Shape> shapes = (col instanceof List) ? new ArrayList<Shape>()
                : EnumSet.noneOf(Shape.class);

        shapes.addAll(col);

        return shapes;
    }

    //----------//
    // shapesOf //
    //----------//
    /**
     * Convenient way to build a collection of shapes.
     *
     * @param col1 a first collection of shapes
     * @param col2 a second collection of shapes
     * @return a single collection
     */
    public static Collection<Shape> shapesOf (Collection<Shape> col1,
                                              Collection<Shape> col2)
    {
        Collection<Shape> shapes = (col1 instanceof List) ? new ArrayList<Shape>()
                : EnumSet.noneOf(Shape.class);

        shapes.addAll(col1);
        shapes.addAll(col2);

        return shapes;
    }

    //----------//
    // shapesOf //
    //----------//
    /**
     * Convenient way to build a collection of shapes.
     *
     * @param col1 a first collection of shapes
     * @param col2 a second collection of shapes
     * @param col3 a third collection of shapes
     * @return a single collection
     */
    public static Collection<Shape> shapesOf (Collection<Shape> col1,
                                              Collection<Shape> col2,
                                              Collection<Shape> col3)
    {
        Collection<Shape> shapes = (col1 instanceof List) ? new ArrayList<Shape>()
                : EnumSet.noneOf(Shape.class);

        shapes.addAll(col1);
        shapes.addAll(col2);
        shapes.addAll(col3);

        return shapes;
    }

    //----------//
    // shapesOf //
    //----------//
    /**
     * Convenient way to build a collection of shapes.
     *
     * @param col1 a first collection of shapes
     * @param col2 a second collection of shapes
     * @param col3 a third collection of shapes
     * @param col4 a fourth collection of shapes
     * @return a single collection
     */
    public static Collection<Shape> shapesOf (Collection<Shape> col1,
                                              Collection<Shape> col2,
                                              Collection<Shape> col3,
                                              Collection<Shape> col4)
    {
        Collection<Shape> shapes = (col1 instanceof List) ? new ArrayList<Shape>()
                : EnumSet.noneOf(Shape.class);

        shapes.addAll(col1);
        shapes.addAll(col2);
        shapes.addAll(col3);
        shapes.addAll(col4);

        return shapes;
    }

    //----------//
    // shapesOf //
    //----------//
    /**
     * Convenient way to build a collection of shapes.
     *
     * @param shapes an array of shapes
     * @return a single collection
     */
    public static Collection<Shape> shapesOf (Shape... shapes)
    {
        return Arrays.asList(shapes);
    }

    //-------------------//
    // supportFingerings //
    //-------------------//
    public static boolean supportFingerings ()
    {
        return constants.supportFingerings.isSet();
    }

    //--------------//
    // supportFrets //
    //--------------//
    public static boolean supportFrets ()
    {
        return constants.supportFrets.isSet();
    }

    //------------------//
    // supportPluckings //
    //------------------//
    public static boolean supportPluckings ()
    {
        return constants.supportPluckings.isSet();
    }

    //-----------------------//
    // supportSmallHeadNotes //
    //-----------------------//
    public static boolean supportSmallHeadNotes ()
    {
        return constants.supportSmallHeadNotes.isSet();
    }

    //-----------------------//
    // supportSmallVoidNotes //
    //-----------------------//
    public static boolean supportSmallVoidNotes ()
    {
        return constants.supportSmallVoidNotes.isSet();
    }

    //------------------------//
    // supportSmallWholeNotes //
    //------------------------//
    public static boolean supportSmallWholeNotes ()
    {
        return constants.supportSmallWholeNotes.isSet();
    }

    //---------//
    // valueOf //
    //---------//
    /**
     * Retrieve a set knowing its name (just like an enumeration).
     *
     * @param str the provided set name
     * @return the range found, or null otherwise
     */
    public static ShapeSet valueOf (String str)
    {
        return Sets.map.get(str);
    }

    //-----------//
    // getShapes //
    //-----------//
    /**
     * Exports the set of shapes.
     *
     * @return the proper enum set
     */
    public EnumSet<Shape> getShapes ()
    {
        return shapes;
    }

    //-----------------//
    // getSortedShapes //
    //-----------------//
    /**
     * Exports the sorted collection of shapes.
     *
     * @return the proper enum set
     */
    public List<Shape> getSortedShapes ()
    {
        if (sortedShapes != null) {
            return sortedShapes;
        } else {
            return new ArrayList<Shape>(shapes);
        }
    }

    //------------------//
    // setConstantColor //
    //------------------//
    /**
     * Define a specific color for the set.
     *
     * @param color the specified color
     */
    public void setConstantColor (Color color)
    {
        constantColor.setValue(color);
        setColor(color);
    }

    //----------------------//
    // defineAllShapeColors //
    //----------------------//
    /**
     * (package private access meant from Shape class)
     * Assign a color to every shape, using the color of the containing
     * set when no specific color is defined for a shape.
     */
    static void defineAllShapeColors ()
    {
        EnumSet<Shape> colored = EnumSet.noneOf(Shape.class);

        // Define shape colors, using their containing range as default
        for (Field field : ShapeSet.class.getDeclaredFields()) {
            if (field.getType() == ShapeSet.class) {
                try {
                    ShapeSet set = (ShapeSet) field.get(null);
                    set.setName(field.getName());

                    // Create shape color for all contained shapes
                    for (Shape shape : set.shapes) {
                        shape.createShapeColor(set.getColor());
                        colored.add(shape);
                    }
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
            }
        }

        /** Sets of similar shapes */
        HW_REST_set.createShapeColor(Rests.getColor());
        colored.add(HW_REST_set);

        // Directly assign colors for shapes in no range
        EnumSet<Shape> leftOver = EnumSet.allOf(Shape.class);
        leftOver.removeAll(colored);

        for (Shape shape : leftOver) {
            shape.createShapeColor(Color.BLACK);
        }
    }

    //----------------//
    // addColoredItem //
    //----------------//
    private static void addColoredItem (JComponent top,
                                        JMenuItem item,
                                        Color color)
    {
        if (color != null) {
            item.setForeground(color);
        } else {
            item.setForeground(Color.black);
        }

        top.add(item);
    }

    //----------//
    // setColor //
    //----------//
    /**
     * Assign a display color to the shape set.
     *
     * @param color the display color
     */
    private void setColor (Color color)
    {
        this.color = color;
    }

    //---------//
    // setName //
    //---------//
    private void setName (String name)
    {
        this.name = name;

        constantColor = new Constant.Color(
                getClass().getName(),
                name + ".color",
                Constant.Color.encodeColor(color),
                "Color code for set " + name);

        // Check for a user-modified value
        if (!constantColor.isSourceValue()) {
            setColor(constantColor.getValue());
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean supportSmallHeadNotes = new Constant.Boolean(
                true,
                "Should we support NOTEHEAD_BLACK_SMALL shape?");

        private final Constant.Boolean supportSmallVoidNotes = new Constant.Boolean(
                false,
                "Should we support NOTEHEAD_VOID_SMALL shape?");

        private final Constant.Boolean supportSmallWholeNotes = new Constant.Boolean(
                false,
                "Should we support WHOLE_NOTE_SMALL shape?");

        private final Constant.Boolean supportPluckings = new Constant.Boolean(
                true,
                "Should we support Pluckings (guitar right-hand)?");

        private final Constant.Boolean supportFingerings = new Constant.Boolean(
                true,
                "Should we support Fingerings (guitar left-hand)?");

        private final Constant.Boolean supportFrets = new Constant.Boolean(
                true,
                "Should we support Fret indications (guitar left-hand)?");
    }

    //------//
    // Sets //
    //------//
    /** Build the set map in a lazy way */
    private static class Sets
    {
        //~ Static fields/initializers -------------------------------------------------------------

        static final Map<String, ShapeSet> map = new HashMap<String, ShapeSet>();

        static final List<ShapeSet> setList = new ArrayList<ShapeSet>();

        static {
            for (Field field : ShapeSet.class.getDeclaredFields()) {
                if (field.getType() == ShapeSet.class) {
                    try {
                        ShapeSet set = (ShapeSet) field.get(null);
                        map.put(field.getName(), set);
                        setList.add(set);
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        //~ Constructors ---------------------------------------------------------------------------
        private Sets ()
        {
        }
    }
}
