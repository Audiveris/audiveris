//----------------------------------------------------------------------------//
//                                                                            //
//                              B e a m I t e m                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.math.BasicLine;
import omr.math.Line;

import omr.sheet.PixelPoint;
import omr.sheet.PixelRectangle;

import omr.stick.Stick;

import omr.util.Logger;

import java.util.Collection;

/**
 * Class <code>BeamItem</code> represents a single beam hook or beam or a beam
 * item part of a larger beam pack.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class BeamItem
    implements Comparable<BeamItem>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BeamItem.class);

    //~ Instance fields --------------------------------------------------------

    /** The containing measure */
    private final Measure measure;

    /** The underlying glyph */
    private final Glyph glyph;

    /**
     * Cardinality of the beam pack (nb of stuck glyphs) this item is part of.
     * Card = 1 for an isolated beam
     */
    private final int packCard;

    /* Index within the beam pack. Index = 0 for an isolated beam */
    private final int packIndex;

    /** The center of the beam item */
    private SystemPoint center;

    /** Line equation for the beam item */
    private Line line;

    /** Left point of beam item */
    private final SystemPoint left;

    /** Right point of beam item */
    private final SystemPoint right;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // BeamItem //
    //----------//
    /**
     * Create a beam item that correspond to a simple glyph (either beam hook or
     * beam)
     *
     * @param measure the containing measure
     * @param glyph the underlying glyph
     */
    public BeamItem (Measure measure,
                     Glyph   glyph)
    {
        this(measure, glyph, 1, 0);
        glyph.setTranslation(this);
    }

    //------//
    // Note //
    //------//
    /** Create a new instance of beam item, as a chunk of a larger beam pack.
     *
     * @param measure the containing measure
     * @param glyph the underlying glyph
     * @param packCard the number of items in the pack
     * @param packIndex the zero-based index of this item in the pack
     */
    public BeamItem (Measure measure,
                     Glyph   glyph,
                     int     packCard,
                     int     packIndex)
    {
        this.measure = measure;
        this.glyph = glyph;
        this.packCard = packCard;
        this.packIndex = packIndex;

        // Location of left and right points
        System         system = measure.getSystem();
        Stick          stick = (Stick) glyph;
        PixelRectangle box = stick.getContourBox();
        double         yMidLeft = stick.getLine()
                                       .xAt((double) box.x); // Vertical stick
        double         yMidRight = stick.getLine()
                                        .xAt((double) (box.x + box.width));
        double         deltaMid1 = Math.min(yMidLeft, yMidRight) - box.y;
        double         deltaMid2 = (box.y + box.height) -
                                   Math.max(yMidLeft, yMidRight);
        double         deltaMid = (deltaMid1 + deltaMid2) / 2.0;
        double         deltaY = (((4 * packIndex) + 1) * deltaMid) / ((2 * packCard) -
                                                                     1);
        int            highY = (int) Math.rint(box.y + deltaY);
        int            lowY = (int) Math.rint(
            (box.y + box.height) - (2 * deltaMid) + deltaY);

        if (yMidLeft > yMidRight) {
            // This is an ascending beam
            left = system.toSystemPoint(new PixelPoint(box.x, lowY));
            right = system.toSystemPoint(
                new PixelPoint(box.x + box.width, highY));
        } else {
            // This is a descending beam
            left = system.toSystemPoint(new PixelPoint(box.x, highY));
            right = system.toSystemPoint(
                new PixelPoint(box.x + box.width, lowY));
        }
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // createPack //
    //------------//
    /**
     * Create a bunch of beam item instances for one beam pack
     *
     * @param measure the containing measure
     * @param glyph the underlying glyph of the beam pack
     */
    public static void createPack (Measure measure,
                                   Glyph   glyph)
    {
        int size = packCardOf(glyph.getShape());
        glyph.clearTranslations();

        for (int i = 0; i < size; i++) {
            BeamItem item = new BeamItem(measure, glyph, size, i);
            glyph.addTranslation(item);
            Beam.populate(item, measure);
        }
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Compare to another BeamItem, by delegating to the underlying glyph
     *
     * @param other the other BeamItem instance
     * @return -1, 0 or 1
     */
    public int compareTo (BeamItem other)
    {
        // Delegate to underlying glyph
        return glyph.compareTo(other.glyph);
    }

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the center of the beam item (which is different from the
     * glyph center in case of a multi-beam glyph)
     *
     * @return the system-based center of the beam item
     */
    public SystemPoint getCenter ()
    {
        if (center == null) {
            center = new SystemPoint(
                (left.x + right.x) / 2,
                (left.y + right.y) / 2);
        }

        return center;
    }

    //---------//
    // getLeft //
    //---------//
    /**
     * Report the point that define the left edge of the beam item
     *
     * @return the SystemPoint coordinates of the rigleftht point
     */
    public SystemPoint getLeft ()
    {
        return left;
    }

    //-------------//
    // getLeftStem //
    //-------------//
    /**
     * Report the left stem (if any) of this beam item
     *
     * @return the left stem or null
     */
    public Glyph getLeftStem ()
    {
        return glyph.getLeftStem();
    }

    //---------//
    // getLine //
    //---------//
    /**
     * Report the (horizontal) line equation defined by the beam item
     *
     * @return the line equation
     */
    public Line getLine ()
    {
        if (line == null) {
            line = new BasicLine();

            // Take left and right points of this beam item
            line.includePoint(left.x, left.y);
            line.includePoint(right.x, right.y);
        }

        return line;
    }

    //----------//
    // getRight //
    //----------//
    /**
     * Report the point that define the right edge of the beam item
     *
     * @return the SystemPoint coordinates of the right point
     */
    public SystemPoint getRight ()
    {
        return right;
    }

    //--------------//
    // getRightStem //
    //--------------//
    /**
     * Report the right stem (if any) of this beam item
     *
     * @return the right stem or null
     */
    public Glyph getRightStem ()
    {
        return glyph.getRightStem();
    }

    //--------//
    // isHook //
    //--------//
    /**
     * Check whether the item is a beam hook
     *
     * @return true if beam hook, false otherwise
     */
    public boolean isHook ()
    {
        return glyph.getShape() == Shape.BEAM_HOOK;
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate a (series of) beam item with this glyph
     *
     * @param glyph glyph of the beam, or beam pack
     * @param measure the containing measure
     */
    public static void populate (Glyph   glyph,
                                 Measure measure)
    {
        createPack(measure, glyph);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the items
     * collection
     *
     * @param items the collection of beam items
     * @return the string built
     */
    public static String toString (Collection<?extends BeamItem> items)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" items[");

        for (BeamItem item : items) {
            sb.append("#")
              .append(item.glyph.getId());
        }

        sb.append("]");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{BeamItem");
        sb.append(" glyph#")
          .append(glyph.getId());

        if (packCard != 1) {
            sb.append(" [")
              .append(packIndex)
              .append("/")
              .append(packCard)
              .append("]");
        }

        sb.append(" left=[")
          .append(left.x)
          .append(",")
          .append(left.y)
          .append("]");

        sb.append(" center=[")
          .append(getCenter().x)
          .append(",")
          .append(getCenter().y)
          .append("]");

        sb.append(" right=[")
          .append(right.x)
          .append(",")
          .append(right.y)
          .append("]");

        sb.append(" slope=")
          .append((float) getLine().getSlope());

        return sb.toString();
    }

    //------------//
    // packCardOf //
    //------------//
    /**
     * Report the cardinality inferred from the glyph shape
     *
     * @param shape the shape of the underlying glyph
     * @return the number of beam items for this shape
     */
    private static int packCardOf (Shape shape)
    {
        switch (shape) {
        case BEAM_3 :
            return 3;

        case BEAM_2 :
            return 2;

        case BEAM :
        case BEAM_HOOK :
            return 1;

        default :
            logger.severe("Use of BeamItem.packSizeOf with shape " + shape);

            return 0;
        }
    }
}
