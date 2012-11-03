//----------------------------------------------------------------------------//
//                                                                            //
//                              B e a m I t e m                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.math.BasicLine;
import omr.math.Line;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.util.HorizontalSide;
import omr.util.Navigable;
import omr.util.Vip;

import java.util.Collection;

/**
 * Class {@code BeamItem} represents either a single beam hook
 * (stuck to 1 stem) or a beam (stuck to 2 stems).
 * By extension, a BeamItem can be a logical part of thick beam "pack" due to
 * glyphs stuck vertically.
 *
 * @author Hervé Bitteur
 */
public class BeamItem
        implements Comparable<BeamItem>,
                   Vip
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BeamItem.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** (Debug) flag this object as VIP. */
    private boolean vip;

    /** The containing measure. */
    @Navigable(false)
    private final Measure measure;

    /** The underlying glyph. */
    private final Glyph glyph;

    /**
     * Cardinality of the containing beam pack (nb of stuck glyphs).
     * Card = 1 for an isolated beam
     */
    private final int packCard;

    /** Index within the beam pack. Index = 0 for an isolated beam */
    private final int packIndex;

    /** The center of the beam item. */
    private PixelPoint center;

    /** Line equation for the beam item. */
    private Line line;

    /** Left point of beam item. */
    private PixelPoint left;

    /** Right point of beam item. */
    private PixelPoint right;

    //~ Constructors -----------------------------------------------------------
    //
    //----------//
    // BeamItem //
    //----------//
    /** Create a new instance of beam item, as part of a beam pack.
     *
     * @param measure   the containing measure
     * @param glyph     the underlying glyph
     * @param packCard  the number of items in the pack
     * @param packIndex the zero-based index of this item in the pack
     */
    private BeamItem (Measure measure,
                      Glyph glyph,
                      int packCard,
                      int packIndex)
    {
        this.measure = measure;
        this.glyph = glyph;
        this.packCard = packCard;
        this.packIndex = packIndex;

        // Location of left and right points
        // For hooks, the stick line is not reliable
        PixelRectangle box = glyph.getBounds();
        if (glyph.getShape() == Shape.BEAM_HOOK) {
            // Make a simple horizontal beam item
            left = new PixelPoint(box.x, box.y + (box.height / 2));
            right = new PixelPoint(box.x + box.width - 1, box.y + (box.height / 2));
        } else {
            // Check line slope
            Line glyphLine = glyph.getLine();

            if (Math.abs(glyphLine.getSlope()) > constants.maxBeamSlope.getValue()) {
                // Slope is not realistic, use horizontal lines
                double halfHeight = box.height / (packCard * 2);
                int y = box.y + (int) Math.rint(halfHeight * (2 * packIndex + 1));
                left = new PixelPoint(box.x, y);
                right = new PixelPoint(box.x + box.width - 1, y);
            } else {
                double yMidLeft = glyphLine.yAtX((double) box.x);
                double yMidRight = glyphLine.yAtX(
                        (double) (box.x + box.width - 1));
                double deltaMid1 = Math.min(yMidLeft, yMidRight) - box.y;
                double deltaMid2 = (box.y + box.height)
                                   - Math.max(yMidLeft, yMidRight);

                double deltaMid = (deltaMid1 + deltaMid2) / 2.0;
                double deltaY = (((4 * packIndex) + 1) * deltaMid) / ((2 * packCard)
                                                                      - 1);
                int highY = (int) Math.rint(box.y + deltaY);
                int lowY = (int) Math.rint(
                        (box.y + box.height) - (2 * deltaMid) + deltaY);

                if (yMidLeft > yMidRight) {
                    // This is an ascending beam
                    left = new PixelPoint(box.x, lowY);
                    right = new PixelPoint(box.x + box.width - 1, highY);
                } else {
                    // This is a descending beam
                    left = new PixelPoint(box.x, highY);
                    right = new PixelPoint(box.x + box.width - 1, lowY);
                }
            }
        }

        if (glyph.isVip()) {
            setVip();
        }

        if (isVip() || logger.isFineEnabled()) {
            logger.info("{0} Created {1}", measure.getContextString(), this);
        }
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // populate //
    //----------//
    /**
     * Populate a BeamItem with this glyph, or a series of BeamItem's
     * if the glyph is a beam pack.
     *
     * @param glyph   glyph of the beam, or beam pack
     * @param measure the containing measure
     */
    public static void populate (Glyph glyph,
                                 Measure measure)
    {
        createPack(measure, glyph);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the
     * items collection.
     *
     * @param items the collection of beam items
     * @return the string built
     */
    public static String toString (Collection<? extends BeamItem> items)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" items[");

        for (BeamItem item : items) {
            sb.append("#").append(item.glyph.getId());
        }

        sb.append("]");

        return sb.toString();
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Compare (horizontally) to another BeamItem, by delegating to
     * the underlying glyph.
     *
     * @param other the other BeamItem instance
     * @return -1, 0 or 1
     */
    @Override
    public int compareTo (BeamItem other)
    {
        // Delegate to underlying glyph
        return Glyph.byAbscissa.compare(glyph, other.glyph);
    }

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the center of the beam item (which is different from the
     * glyph center in case of a multi-beam pack glyph).
     *
     * @return the system-based center of the beam item
     */
    public PixelPoint getCenter ()
    {
        if (center == null) {
            center = new PixelPoint(
                    (left.x + right.x) / 2,
                    (left.y + right.y) / 2);
        }

        return center;
    }

    //----------//
    // getGlyph //
    //----------//
    /**
     * Report the underlying glyph.
     *
     * @return the underlying glyph
     */
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //---------//
    // getLine //
    //---------//
    /**
     * Report the (horizontal) line equation defined by the beam item.
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
    // getPoint //
    //----------//
    /**
     * Report the point that defines the desired edge of the beam item.
     *
     * @return (a copy) of the point on desired side
     */
    public PixelPoint getPoint (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return new PixelPoint(left);
        } else {
            return new PixelPoint(right);
        }
    }

    //----------//
    // setPoint //
    //----------//
    /**
     * Set the point that defines the desired edge of the beam item.
     *
     * @param side  the desired side
     * @param point the new point on desired side
     */
    public void setPoint (HorizontalSide side,
                          PixelPoint point)
    {
        if (side == HorizontalSide.LEFT) {
            this.left = point;
        } else {
            this.right = point;
        }

        // Invalidate
        line = null;
        center = null;
    }

    //---------//
    // getStem //
    //---------//
    /**
     * Report the stem (if any) of this beam item on the desired side.
     *
     * @return the found stem or null
     */
    public Glyph getStem (HorizontalSide side)
    {
        return glyph.getStem(side);
    }

    //--------//
    // isHook //
    //--------//
    /**
     * Check whether the item is a beam hook.
     *
     * @return true if beam hook, false otherwise
     */
    public boolean isHook ()
    {
        return glyph.getShape() == Shape.BEAM_HOOK;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip ()
    {
        vip = true;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{BeamItem");

        try {
            sb.append(" ").append(glyph.idString());

            if (packCard != 1) {
                sb.append(" [").append(packIndex).append("/").append(packCard).
                        append("]");
            }

            sb.append(" left=[").append(left.x).append(",").append(left.y).
                    append("]");
            sb.append(" center=[").append(getCenter().x).append(",").append(
                    getCenter().y).append("]");
            sb.append(" right=[").append(right.x).append(",").append(right.y).
                    append("]");
            sb.append(" slope=").append((float) getLine().getSlope());
        } catch (NullPointerException e) {
            sb.append(" INVALID");
        }

        sb.append("}");

        return sb.toString();
    }

    //------------//
    // createPack //
    //------------//
    /**
     * Create a bunch of BeamItem instances for one pack.
     *
     * @param measure the containing measure
     * @param glyph   the underlying glyph of the beam pack
     */
    private static void createPack (Measure measure,
                                    Glyph glyph)
    {
        int size = packCardOf(glyph.getShape());
        glyph.clearTranslations();

        try {
            for (int i = 0; i < size; i++) {
                BeamItem item = new BeamItem(measure, glyph, size, i);
                glyph.addTranslation(item);
                Beam.populate(item, measure);
            }
        } catch (Exception ex) {
            logger.warning(measure.getContextString()
                           + " Error creating BeamItem from glyph #"
                           + glyph.getId(),
                    ex);
        }
    }

    //------------//
    // packCardOf //
    //------------//
    /**
     * Report the cardinality inferred from the glyph shape.
     *
     * @param shape the shape of the underlying glyph
     * @return the number of beam items for this shape
     */
    private static int packCardOf (Shape shape)
    {
        switch (shape) {
        case BEAM_3:
            return 3;

        case BEAM_2:
            return 2;

        case BEAM:
        case BEAM_HOOK:
            return 1;

        default:
            logger.severe("Use of BeamItem.packCardOf with shape {0}", shape);

            return 0;
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Double maxBeamSlope = new Constant.Double(
                "slope",
                1.0,
                "Maximum slope for a beam item line");

    }
}
