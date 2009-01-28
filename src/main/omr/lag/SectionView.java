//----------------------------------------------------------------------------//
//                                                                            //
//                           S e c t i o n V i e w                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.lag;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.graph.VertexView;

import omr.log.Logger;

import omr.util.Implement;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Arrays;

/**
 * Class <code>SectionView</code> defines one view meant for display of a
 * given section. No zoom is defined for this view, since the provided Graphics
 * context can be used for this when rendering.
 *
 * @param L the precise lag type
 * @param S the precise section type
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SectionView<L extends Lag<L, S>, S extends Section<L, S>>
    implements VertexView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SectionView.class);

    // Build a palette with a limited nb of colors
    private static final int     COLOR_NB = 3;
    private static final Color[] palette = new Color[COLOR_NB];

    static {
        for (int i = 0; i < COLOR_NB; i++) {
            palette[i] = Color.getHSBColor(
                (float) (i + 1) / (float) COLOR_NB,
                (float) constants.saturation.getValue(),
                (float) constants.brightness.getValue());
        }
    }

    //~ Instance fields --------------------------------------------------------

    /**
     * Related section
     */
    protected final S section;

    /**
     * Color currently used. By defaut, the color corresponds to the colorIndex
     * of the palette. But, temporarily, a section can be assigned a different
     * color, for example to highlight the section.
     */
    protected Color color;

    /**
     * Assigned color index. This is the permanent default, which is set when
     * the containing {@link LagView} is created, and which is also used when
     * the color is reset by {@link #resetColor}
     */
    protected int colorIndex = -1;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SectionView //
    //-------------//
    /**
     * Create a section view, with specified zoom
     *
     * @param section the related section
     */
    public SectionView (S section)
    {
        this.section = section;
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // setColor //
    //----------//
    /**
     * Allow to modify the display color of a given section.
     *
     * @param color the new color
     */
    public void setColor (Color color)
    {
        this.color = color;
    }

    //---------------//
    // getColorIndex //
    //---------------//
    /**
     * Report the color index assigned to this section view. A -1 value means no
     * value has been assigned yet.
     *
     * @return the assigned index, or -1 if none.
     */
    public int getColorIndex ()
    {
        return colorIndex;
    }

    //--------------//
    // getRectangle //
    //--------------//
    /**
     * Report the bounding rectangle of the view.
     *
     * @return the properly oriented display rectangle that fits to the section
     */
    @Implement(VertexView.class)
    public Rectangle getRectangle ()
    {
        return section.getContourBox();
    }

    //---------------------//
    // determineColorIndex //
    //---------------------//
    /**
     * Determine the proper color index in the palette to assign to this section
     * view. We choose the first color in the palette which is not already used
     * by any of the connected sections (sources and targets).
     *
     * @param viewIndex the index of this view in the collection of views for
     * this section, which is identical to the index of the containing lag view
     * in the collection of views for the lag.
     */
    public void determineColorIndex (int viewIndex)
    {
        // Choose a correct color, by first looking at adjacent sections
        boolean[] colorUsed = new boolean[COLOR_NB];
        Arrays.fill(colorUsed, false);

        for (S sct : section.getTargets()) {
            SectionView v = (SectionView) sct.getView(viewIndex);
            int         c = v.colorIndex;

            if (c != -1) {
                colorUsed[c] = true;
            }
        }

        for (S sct : section.getSources()) {
            SectionView v = (SectionView) sct.getView(viewIndex);
            int         c = v.colorIndex;

            if (c != -1) {
                colorUsed[c] = true;
            }
        }

        // Take the first color not already used in the palette
        int best = -1;

        for (int i = 0; i < colorUsed.length; i++) {
            if (!colorUsed[i]) {
                best = i;

                break;
            }
        }

        // This should not happen: thanks to a theorem I forgot, we just need 3
        // colors for areas on a plan.
        if (best == -1) {
            logger.warning("Color collision for section " + section.getId());
            best = SectionView.COLOR_NB - 1;
        }

        colorIndex = best;
        resetColor();
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the section using the provided graphics object.
     *
     * @param g the graphics environment (which may be applying transformation
     * such as scale)
     * @return true if the section is concerned by the clipping rectangle, which
     * means if (part of) the section has been drawn
     */
    @Implement(VertexView.class)
    public boolean render (Graphics g)
    {
        Rectangle clip = g.getClipBounds();
        Rectangle rect = getRectangle();

        if (clip.intersects(rect)) {
            g.setColor(color);

            Polygon polygon = section.getContour();
            ///g.drawPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
            g.fillPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);

            // Display the section foreground value? To be improved!!!
            if (constants.displayDensity.getValue()) {
                g.setColor(Color.black);
                g.drawString(
                    Integer.toString(section.getLevel()),
                    rect.x + ((rect.width / 2) - 8),
                    rect.y + ((rect.height / 2) + 5));
            }

            return true;
        } else {
            return false;
        }
    }

    //------------//
    // resetColor //
    //------------//
    /**
     * Allow to reset to default the display color of a given section
     */
    public void resetColor ()
    {
        setColor(palette[colorIndex]);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean displayDensity = new Constant.Boolean(
            false,
            "Should we render the section foreground density");
        Constant.Ratio   brightness = new Constant.Ratio(
            0.4d,
            "Color brightness (range 0.0 .. 1.0)");
        Constant.Ratio   saturation = new Constant.Ratio(
            0.4d,
            "Color saturation (range 0.0 .. 1.0)");
    }
}
