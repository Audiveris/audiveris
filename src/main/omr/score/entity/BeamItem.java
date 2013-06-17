//----------------------------------------------------------------------------//
//                                                                            //
//                              B e a m I t e m                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.math.BasicLine;
import omr.math.Line;

import omr.util.HorizontalSide;
import omr.util.TreeNode;
import omr.util.Vip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.Comparator;

/**
 * Class {@code BeamItem} represents either a single beam hook
 * (stuck to 1 stem) or a beam (stuck to 2 stems).
 * By extension, a BeamItem can be a logical part of thick beam "pack" due to
 * glyphs stuck vertically.
 *
 * @author Hervé Bitteur
 */
public class BeamItem
        extends MeasureNode
        implements Vip
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            BeamItem.class);

    /** Compare two BeamItem instances by abscissa within a Beam. */
    public static final Comparator<TreeNode> byNodeAbscissa = new Comparator<TreeNode>()
    {
        @Override
        public int compare (TreeNode tn1,
                            TreeNode tn2)
        {
            BeamItem item1 = (BeamItem) tn1;
            BeamItem item2 = (BeamItem) tn2;

            // Delegate to underlying glyph
            return Glyph.byAbscissa.compare(item1.getGlyph(), item2.getGlyph());
        }
    };

    //~ Instance fields --------------------------------------------------------
    //
    /** (Debug) flag this object as VIP. */
    private boolean vip;

    /**
     * Cardinality of the containing beam pack (nb of stuck items).
     * Card = 1 for an isolated beam
     */
    private final int packCard;

    /** Index within the beam pack. Index = 0 for an isolated beam */
    private final int packIndex;

    /** Line equation for the beam item. */
    private Line line;

    /** Left point of beam item. */
    private Point left;

    /** Right point of beam item. */
    private Point right;

    //~ Constructors -----------------------------------------------------------
    //
    //----------//
    // BeamItem //
    //----------//
    /** Create a new instance of beam item, as part of a beam item pack.
     *
     * @param Beam      the containing beam instance
     * @param glyph     the underlying glyph
     * @param left      the left defining point
     * @param right     the right defining point
     * @param packCard  the number of items in the pack
     * @param packIndex the zero-based index of this item in the pack
     */
    private BeamItem (Beam beam,
                      Glyph glyph,
                      Point left,
                      Point right,
                      int packCard,
                      int packIndex)
    {
        super(beam);

        addGlyph(glyph);

        this.packCard = packCard;
        this.packIndex = packIndex;
        this.left = new Point(left);
        this.right = new Point(right);

        // Keep the items sorted by abscissa in Beam container
        Collections.sort(beam.getItems(), BeamItem.byNodeAbscissa);

        if (glyph.isVip()) {
            setVip();
            beam.setVip();
        }

        if (isVip() || logger.isDebugEnabled()) {
            logger.info("{} Created {}", beam.getContextString(), this);
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
        if (glyph.isVip()) {
            logger.info("BeamItem. populate {}", glyph.idString());
        }

        createPack(measure, glyph);
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
        return glyphs.first();
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
    public Point getPoint (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return new Point(left);
        } else {
            return new Point(right);
        }
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
        return getGlyph()
                .getStem(side);
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
        return getGlyph()
                .getShape() == Shape.BEAM_HOOK;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
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
                          Point point)
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
            sb.append(" ")
                    .append(getGlyph().idString());

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
        } catch (NullPointerException e) {
            sb.append(" INVALID");
        }

        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // computeCenter //
    //---------------//
    /**
     * Compute the center of this beam item (which is different from the
     * glyph center in case of a multi-beam pack glyph).
     */
    @Override
    protected void computeCenter ()
    {
        setCenter(new Point((left.x + right.x) / 2, (left.y + right.y) / 2));
    }

    //------------//
    // createPack //
    //------------//
    /**
     * Create a bunch of BeamItem instances for one pack, and create
     * or augment the containing Beam instance.
     *
     * @param measure the containing measure
     * @param glyph   the underlying glyph of the beam pack
     */
    private static void createPack (Measure measure,
                                    Glyph glyph)
    {
        int card = packCardOf(glyph.getShape());
        glyph.clearTranslations();

        try {
            for (int i = 0; i < card; i++) {
                // Compute item defining points
                Point left;
                Point right;

                // For hooks, the stick line is not reliable
                Rectangle box = glyph.getBounds();

                if (glyph.getShape() == Shape.BEAM_HOOK) {
                    // Make a simple horizontal beam item
                    left = new Point(box.x, box.y + (box.height / 2));
                    right = new Point(
                            (box.x + box.width) - 1,
                            box.y + (box.height / 2));
                } else {
                    // Check line slope
                    Line glyphLine = glyph.getLine();

                    if (Math.abs(glyphLine.getSlope()) > constants.maxBeamSlope.getValue()) {
                        // Slope is not realistic, use horizontal lines
                        double halfHeight = box.height / (card * 2);
                        int y = box.y
                                + (int) Math.rint(halfHeight * ((2 * i) + 1));
                        left = new Point(box.x, y);
                        right = new Point((box.x + box.width) - 1, y);
                    } else {
                        double yMidLeft = glyphLine.yAtX((double) box.x);
                        double yMidRight = glyphLine.yAtX(
                                (double) ((box.x + box.width) - 1));
                        double deltaMid1 = Math.min(yMidLeft, yMidRight)
                                           - box.y;
                        double deltaMid2 = (box.y + box.height)
                                           - Math.max(yMidLeft, yMidRight);

                        double deltaMid = (deltaMid1 + deltaMid2) / 2.0;
                        double deltaY = (((4 * i) + 1) * deltaMid) / ((2 * card)
                                                                      - 1);
                        int highY = (int) Math.rint(box.y + deltaY);
                        int lowY = (int) Math.rint(
                                (box.y + box.height) - (2 * deltaMid) + deltaY);

                        if (yMidLeft > yMidRight) {
                            // This is an ascending beam
                            left = new Point(box.x, lowY);
                            right = new Point((box.x + box.width) - 1, highY);
                        } else {
                            // This is a descending beam
                            left = new Point(box.x, highY);
                            right = new Point((box.x + box.width) - 1, lowY);
                        }
                    }
                }

                // Retrieve/create the hosting Beam instance
                Beam beam = Beam.populate(left, right, measure);

                // Finally, allocate the BeamItem instance
                BeamItem item = new BeamItem(beam, glyph, left, right, card, i);
                glyph.addTranslation(item);
            }
        } catch (Exception ex) {
            logger.warn(
                    measure.getContextString() + " Error creating BeamItem from "
                    + glyph.idString(),
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
            logger.error("Use of BeamItem.packCardOf with shape {}", shape);

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
