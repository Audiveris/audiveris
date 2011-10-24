//----------------------------------------------------------------------------//
//                                                                            //
//                            S h a p e R a n g e                             //
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
import static omr.glyph.Shape.*;

import omr.log.Logger;

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
 * Class {@code ShapeRange} defines a set of related shapes, for example
 * the "Rests" range gathers all rest shapes from MULTI_REST down to
 * ONE_HUNDRED_TWENTY_EIGHTH_REST.
 *
 * <p>It handles additional properties over a simple EnumSet, especially
 * assigned colors and its automatic insertion in shape menu hierarchy.
 * So don't remove any of these sets, unless you know what you are doing.
 */
public class ShapeRange
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ShapeRange.class);

    // Predefined instances of ShapeRange. Double-check before removing any one.
    //
    public static final ShapeRange     Accidentals = new ShapeRange(
        SHARP,
        EnumSet.range(FLAT, DOUBLE_FLAT));
    public static final ShapeRange     Articulations = new ShapeRange(
        ARPEGGIATO,
        shapesOf(EnumSet.range(ACCENT, STRONG_ACCENT), STACCATO, ARPEGGIATO));
    public static final ShapeRange     Barlines = new ShapeRange(
        LEFT_REPEAT_SIGN,
        EnumSet.range(PART_DEFINING_BARLINE, BACK_TO_BACK_REPEAT_SIGN));
    public static final ShapeRange     Beams = new ShapeRange(
        BEAM,
        shapesOf(EnumSet.range(BEAM, BEAM_HOOK), COMBINING_STEM));
    public static final ShapeRange     Clefs = new ShapeRange(
        G_CLEF,
        EnumSet.range(G_CLEF, PERCUSSION_CLEF));
    public static final ShapeRange     Dynamics = new ShapeRange(
        DYNAMICS_F,
        EnumSet.range(DYNAMICS_CHAR_M, DECRESCENDO));
    public static final ShapeRange     Flags = new ShapeRange(
        COMBINING_FLAG_1,
        EnumSet.range(COMBINING_FLAG_1, COMBINING_FLAG_5_UP));
    public static final ShapeRange     HeadAndFlags = new ShapeRange(
        HEAD_AND_FLAG_1,
        EnumSet.range(HEAD_AND_FLAG_1, HEAD_AND_FLAG_5_UP));
    public static final ShapeRange     Keys = new ShapeRange(
        KEY_SHARP_3,
        shapesOf(
            KEY_FLAT_1,
            KEY_FLAT_2,
            KEY_FLAT_3,
            KEY_FLAT_4,
            KEY_FLAT_5,
            KEY_FLAT_6,
            KEY_FLAT_7,
            KEY_SHARP_1,
            KEY_SHARP_2,
            KEY_SHARP_3,
            KEY_SHARP_4,
            KEY_SHARP_5,
            KEY_SHARP_6,
            KEY_SHARP_7));
    public static final ShapeRange     NoteHeads = new ShapeRange(
        NOTEHEAD_BLACK,
        EnumSet.range(VOID_NOTEHEAD, NOTEHEAD_BLACK_3));
    public static final ShapeRange     Markers = new ShapeRange(
        CODA,
        EnumSet.range(DAL_SEGNO, BRACKET));
    public static final ShapeRange     Notes = new ShapeRange(
        BREVE,
        EnumSet.range(BREVE, WHOLE_NOTE_3));
    public static final ShapeRange     Ornaments = new ShapeRange(
        MORDENT,
        EnumSet.range(GRACE_NOTE_SLASH, INVERTED_MORDENT));
    public static final ShapeRange     Rests = new ShapeRange(
        QUARTER_REST,
        shapesOf(
            LONG_REST,
            BREVE_REST,
            WHOLE_REST,
            HALF_REST,
            OLD_QUARTER_REST,
            QUARTER_REST,
            EIGHTH_REST,
            SIXTEENTH_REST,
            THIRTY_SECOND_REST,
            SIXTY_FOURTH_REST,
            ONE_HUNDRED_TWENTY_EIGHTH_REST));
    public static final ShapeRange     Times = new ShapeRange(
        TIME_FOUR_FOUR,
        shapesOf(EnumSet.range(TIME_ZERO, CUT_TIME), CUSTOM_TIME_SIGNATURE));

    /** A bag of miscellaneous shapes */
    public static final ShapeRange Others = new ShapeRange(
        PEDAL_MARK,
        shapesOf(
            OTTAVA_ALTA,
            OTTAVA_BASSA,
            PEDAL_MARK,
            PEDAL_UP_MARK,
            TUPLET_THREE,
            TUPLET_SIX,
            SLUR));

    //
    public static final ShapeRange     Physicals = new ShapeRange(
        TEXT,
        shapesOf(TEXT, CHARACTER, CLUTTER, DOT, STRUCTURE));
    public static final ShapeRange     Logicals = new ShapeRange(
        REPEAT_DOTS,
        shapesOf(REPEAT_DOTS, COMBINING_AUGMENTATION_DOT, ENDING));

    // =========================================================================
    // Below are EnumSet instances, used programmatically.
    // They do not lead to shape submenus as the ShapeRange instances do.
    // =========================================================================

    /** All physical shapes */
    public static final EnumSet<Shape> allSymbols = EnumSet.range(
        Shape.values()[0],
        LAST_PHYSICAL_SHAPE);

    /** Symbols that can be attached to a stem */
    public static final EnumSet<Shape> StemSymbols = EnumSet.noneOf(
        Shape.class);

    static {
        StemSymbols.add(BEAM);
        StemSymbols.add(BEAM_2);
        StemSymbols.add(BEAM_3);
        StemSymbols.add(BEAM_HOOK);

        StemSymbols.addAll(NoteHeads.getShapes());
        StemSymbols.addAll(Flags.getShapes());
        StemSymbols.addAll(HeadAndFlags.getShapes());
    }

    /** Pedals */
    public static final EnumSet<Shape> Pedals = EnumSet.range(
        PEDAL_MARK,
        PEDAL_UP_MARK);

    /** Tuplets */
    public static final EnumSet<Shape> Tuplets = EnumSet.range(
        TUPLET_THREE,
        TUPLET_SIX);

    /** Specific single symbol for part of time signature (such as 4) */
    public static final EnumSet<Shape> PartialTimes = EnumSet.range(
        TIME_ZERO,
        TIME_SIXTEEN);

    /** Specific multi-symbol for full time signature (such as 4/4 */
    public static final EnumSet<Shape> FullTimes = EnumSet.range(
        TIME_FOUR_FOUR,
        CUT_TIME);

    static {
        FullTimes.add(CUSTOM_TIME_SIGNATURE);
    }

    /** All variants of dot */
    public static final EnumSet<Shape> Dots = EnumSet.of(
        DOT,
        COMBINING_AUGMENTATION_DOT,
        STACCATO,
        REPEAT_DOTS);

    /** All sorts of F clefs */
    public static final EnumSet<Shape> BassClefs = EnumSet.of(
        F_CLEF,
        F_CLEF_OTTAVA_ALTA,
        F_CLEF_OTTAVA_BASSA);

    /** All sorts of G clefs */
    public static final EnumSet<Shape> TrebleClefs = EnumSet.of(
        G_CLEF,
        G_CLEF_OTTAVA_ALTA,
        G_CLEF_OTTAVA_BASSA);

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

    /** Clefs ottava (alta or bassa) */
    public static final EnumSet<Shape> OttavaClefs = EnumSet.of(
        G_CLEF_OTTAVA_ALTA,
        G_CLEF_OTTAVA_BASSA,
        F_CLEF_OTTAVA_ALTA,
        F_CLEF_OTTAVA_BASSA);

    /** Head/Flag combinations with flags down */
    public static final EnumSet<Shape> HeadAndFlagsDown = EnumSet.of(
        HEAD_AND_FLAG_1,
        HEAD_AND_FLAG_2,
        HEAD_AND_FLAG_3,
        HEAD_AND_FLAG_4,
        HEAD_AND_FLAG_5);

    /** Head/Flag combinations with flags up */
    public static final EnumSet<Shape> HeadAndFlagsUp = EnumSet.of(
        HEAD_AND_FLAG_1_UP,
        HEAD_AND_FLAG_2_UP,
        HEAD_AND_FLAG_3_UP,
        HEAD_AND_FLAG_4_UP,
        HEAD_AND_FLAG_5_UP);

    //~ Instance fields --------------------------------------------------------

    /** Name of the range */
    private String name;

    /** Underlying shapes */
    private final EnumSet<Shape> shapes;

    /** Specific sequence of shapes, if any */
    private final List<Shape> sortedShapes;

    /** The representative shape for this range */
    private final Shape rep;

    /** Current color */
    private Color color;

    /** Related permanent color */
    private Constant.Color constantColor;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // ShapeRange //
    //------------//
    /**
     * Creates a new ShapeRange object from a collection of shapes
     * @param rep the representative shape
     * @param shapes the provided collection of shapes
     */
    public ShapeRange (Shape             rep,
                       Collection<Shape> shapes)
    {
        // The representative shape
        this.rep = rep;

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

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getRange //
    //----------//
    public static ShapeRange getRange (String name)
    {
        return Ranges.map.get(name);
    }

    //-----------//
    // getRanges //
    //-----------//
    public static List<ShapeRange> getRanges ()
    {
        return Ranges.rangeList;
    }

    //----------//
    // setColor //
    //----------//
    /**
     * Assign a display color to the shape range
     *
     * @param color the display color
     */
    public void setColor (Color color)
    {
        this.color = color;
    }

    //----------//
    // getColor //
    //----------//
    /**
     * Report the color currently assigned to the range, if any
     *
     * @return the related color, or null
     */
    public Color getColor ()
    {
        return color;
    }

    //------------------//
    // setConstantColor //
    //------------------//
    /**
     * Define a specific color for the range
     *
     * @param color the specified color
     */
    public void setConstantColor (Color color)
    {
        constantColor.setValue(color);
        setColor(color);
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

    //--------//
    // getRep //
    //--------//
    /**
     * Report the representative shape of the range, if any
     * @return the rep shape, or null
     */
    public Shape getRep ()
    {
        return rep;
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

    //-----------------//
    // getSortedShapes //
    //-----------------//
    /**
     * Exports the sorted collection of shapes
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
    // addAllRangeItems //
    //------------------//
    /**
     * Populate the given menu with a hierarchy of all ranges defined in
     * ShapeRange classs
     *
     * @param top the JComponent to populate (typically a JMenu or a
     * JPopupMenu)
     * @param listener the listener for notification of user selection
     */
    public static void addAllRangeItems (JComponent     top,
                                         ActionListener listener)
    {
        // All ranges of glyph shapes
        for (Field field : ShapeRange.class.getDeclaredFields()) {
            if (field.getType() == ShapeRange.class) {
                JMenuItem  menuItem = new JMenuItem(field.getName());
                ShapeRange range = valueOf(field.getName());
                addColoredItem(top, menuItem, range.getColor());
                menuItem.addActionListener(listener);
            }
        }
    }

    //---------------//
    // addRangeItems //
    //---------------//
    /**
     * Populate the given menu with a list of all shapes that belong to the
     * given range
     *
     * @param range the range for which shape menu items must be buit
     * @param top the JComponent to populate (typically a JMenu or a JPopupMenu)
     * @param listener the listener for notification of user selection
     */
    public static void addRangeItems (ShapeRange     range,
                                      JComponent     top,
                                      ActionListener listener)
    {
        // All shapes in the given range
        for (Shape shape : range.getSortedShapes()) {
            JMenuItem menuItem = new JMenuItem(
                shape.toString(),
                shape.getDecoratedSymbol());
            addColoredItem(top, menuItem, shape.getColor());

            menuItem.setToolTipText(shape.getDescription());
            menuItem.addActionListener(listener);
        }
    }

    //---------------//
    // addShapeItems //
    //---------------//
    /**
     * Populate the given menu with a hierarchy of all shapes, organized by
     * defined ranges.
     *
     * @param top the JComponent to populate (typically a JMenu or a JPopupMenu)
     * @param listener the listener for notification of user selection
     */
    public static void addShapeItems (JComponent     top,
                                      ActionListener listener)
    {
        // All ranges of glyph shapes
        for (Field field : ShapeRange.class.getDeclaredFields()) {
            if (field.getType() == ShapeRange.class) {
                ShapeRange range = ShapeRange.valueOf(field.getName());
                JMenu      menu = new JMenu(field.getName());

                if (range.rep != null) {
                    menu.setIcon(range.rep.getDecoratedSymbol());
                }

                addColoredItem(top, menu, Color.black);

                // Add menu items for this range
                addRangeItems(range, menu, listener);
            }
        }
    }

    //----------//
    // contains //
    //----------//
    /**
     * Convenient method to check if encapsulated shapes set does contain
     * the provided object
     *
     * @param shape the Shape object to check for inclusion
     * @return true if contained, false otherwise
     */
    public boolean contains (Shape shape)
    {
        return shapes.contains(shape);
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
    public static ShapeRange valueOf (String str)
    {
        return Ranges.map.get(str);
    }

    //----------------------//
    // defineAllShapeColors //
    //----------------------//
    /**
     * (package private access meant from Shape class)
     * Assign a color to every shape, using the color of the containing range
     * when no specific color is defined for a shape.
     */
    static void defineAllShapeColors ()
    {
        EnumSet<Shape> colored = EnumSet.noneOf(Shape.class);

        // Define shape colors, using their containing range as default
        for (Field field : ShapeRange.class.getDeclaredFields()) {
            if (field.getType() == ShapeRange.class) {
                try {
                    ShapeRange range = (ShapeRange) field.get(null);
                    range.setName(field.getName());

                    // Create shape color for all contained shapes
                    for (Shape shape : range.shapes) {
                        shape.createShapeColor(range.getColor());
                        colored.add(shape);
                    }
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
            }
        }

        WHOLE_OR_HALF_REST.createShapeColor(Rests.getColor());
        colored.add(WHOLE_OR_HALF_REST);

        // Directly assign colors for shapes in no range
        EnumSet<Shape> leftOver = EnumSet.allOf(Shape.class);
        leftOver.removeAll(colored);

        for (Shape shape : leftOver) {
            shape.createShapeColor(Color.BLACK);
        }
    }

    //----------//
    // shapesOf //
    //----------//
    private static Collection<Shape> shapesOf (Collection<Shape> shapes,
                                               Shape... added)
    {
        return shapesOf(shapes, shapesOf(added));
    }

    //----------//
    // shapesOf //
    //----------//
    private static Collection<Shape> shapesOf (Collection<Shape> c1,
                                               Collection<Shape> c2)
    {
        Collection<Shape> shapes = (c1 instanceof List)
                                   ? new ArrayList<Shape>()
                                   : EnumSet.noneOf(Shape.class);

        shapes.addAll(c1);
        shapes.addAll(c2);

        return shapes;
    }

    //----------//
    // shapesOf //
    //----------//
    private static Collection<Shape> shapesOf (Shape... shapes)
    {
        return Arrays.asList(shapes);
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
            "#000000",
            "Color code for range " + name);

        // Assign the range display color
        if (!constantColor.isSourceValue()) {
            setColor(constantColor.getValue());
        } else {
            // Use the default color
            setColor(Color.black);
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

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // Ranges //
    //--------//
    /** Build the range map in a lazy way */
    private static class Ranges
    {
        //~ Static fields/initializers -----------------------------------------

        static final Map<String, ShapeRange> map = new HashMap<String, ShapeRange>();
        static final List<ShapeRange>        rangeList = new ArrayList<ShapeRange>();

        static {
            for (Field field : ShapeRange.class.getDeclaredFields()) {
                if (field.getType() == ShapeRange.class) {
                    try {
                        ShapeRange range = (ShapeRange) field.get(null);
                        map.put(field.getName(), range);
                        rangeList.add(range);
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
