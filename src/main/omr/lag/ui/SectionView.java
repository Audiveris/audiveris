//----------------------------------------------------------------------------//
//                                                                            //
//                           S e c t i o n V i e w                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.lag.ui;

import omr.lag.*;
import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.graph.VertexView;

import omr.log.Logger;

import omr.util.Implement;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Class <code>SectionView</code> defines one view meant for display of a
 * given section.
 *
 * <p>An important characteristic is the default color to be used when
 * rendering the related section: this color is determined by looking at the
 * adjacent sections in the same view, to make sure we select a different color.
 * On top of this, we may use a temporary color, for example to highlight the
 * section.
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
     * Default color. This is the permanent default, which is set when
     * the containing {@link LagView} is created, and which is also used when
     * the color is reset by {@link #resetColor}
     */
    protected Color defaultColor;

    /**
     * Color currently used. By defaut, the color is the defaultColor chosen out
     * of the palette. But, temporarily, a section can be assigned a different
     * color, for example to highlight the section.
     */
    protected Color color;

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

        ///defaultColor = determineDefaultColor();
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

    //-------------//
    // isColorized //
    //-------------//
    /**
     * Report whether a default color has been assigned
     * @return trur if defaultColor is no longer null
     */
    public boolean isColorized ()
    {
        return defaultColor != null;
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

    //-----------------------//
    // determineDefaultColor //
    //-----------------------//
    /**
     * Determine the proper default color in the palette to assign to this
     * section view. We choose the first color in the palette which is not
     * already used by any of the connected sections (sources and targets).
     *
     * @param viewIndex the index of this view in the collection of views for
     * this section, which is identical to the index of the containing lag view
     * in the collection of views for the lag.
     */
    public void determineDefaultColor (int viewIndex)
    {
        // Check if work has already been done
        if (defaultColor != null) {
            return;
        }

        // Choose a correct color, by first looking at adjacent sections
        Set<Color> usedColors = new HashSet<Color>();

        for (S sct : section.getTargets()) {
            SectionView v = (SectionView) sct.getView(viewIndex);
            Color       c = v.defaultColor;

            if (c != null) {
                usedColors.add(c);
            }
        }

        for (S sct : section.getSources()) {
            SectionView v = (SectionView) sct.getView(viewIndex);
            Color       c = v.defaultColor;

            if (c != null) {
                usedColors.add(c);
            }
        }

        // Take the first color avaliable
        Set<Color> availableColors = new HashSet<Color>(Arrays.asList(palette));
        availableColors.removeAll(usedColors);

        // This should not happen: thanks to a theorem I forgot, we just need 3
        // colors for areas on a 2-D plan.
        if (availableColors.isEmpty()) {
            logger.severe("Color collision for section " + section.getId());
            defaultColor = palette[0]; // We need a color!
        }

        defaultColor = availableColors.iterator()
                                      .next();
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
        setColor(defaultColor);
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
            0.7d,
            "Color brightness (range 0.0 .. 1.0)");
        Constant.Ratio   saturation = new Constant.Ratio(
            0.4d,
            "Color saturation (range 0.0 .. 1.0)");
    }
}
