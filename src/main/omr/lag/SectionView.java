//----------------------------------------------------------------------------//
//                                                                            //
//                           S e c t i o n V i e w                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.lag;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.graph.VertexView;

import omr.ui.view.Zoom;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.Arrays;

/**
 * Class <code>SectionView</code> is an abstract class to define common features
 * needed by a vertical or horizontal section view.
 *
 * <p>The sections are displayed using 3 different colors.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SectionView<L extends Lag<L, S>, S extends Section<L, S>>
    implements VertexView
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger    logger = Logger.getLogger(SectionView.class);

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
     * Precise zoomed points
     */
    protected final Polygon dspPolygon;

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
     * Bounding rectangle for the (zoomed) display
     */
    protected Rectangle dspRectangle = new Rectangle();

    /**
     * Display zoom
     */
    protected Zoom zoom;

    /**
     * Assigned color index. This is the permanent default, which is set when
     * the containing {@link LagView} is created, and which is also used when
     * the color is reset by {@link #resetColor}
     */
    protected int colorIndex = -1;

    /**
     * Last zoom ratio. The value is cached to save on the computation of the
     * display polygon.
     */
    private transient double lastZoomRatio;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SectionView //
    //-------------//
    /**
     * Create a section view, with implicit zoom ratio
     *
     * @param section
     */
    public SectionView (S section)
    {
        this(section, null);
    }

    //-------------//
    // SectionView //
    //-------------//
    /**
     * Create a section view, with specified zoom
     *
     * @param section the related section
     * @param zoom the display zoom
     */
    public SectionView (S    section,
                        Zoom zoom)
    {
        this.section = section;

        // Delimitating points
        final int pointNb = 4 * section.getRunNb();
        dspPolygon = new Polygon(new int[pointNb], new int[pointNb], pointNb);

        // Compute zoom-dependent variables
        setZoom(zoom);
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
        return dspRectangle;
    }

    //---------//
    // setZoom //
    //---------//
    /**
     * Specify the display zoom
     *
     * @param zoom the new zoom
     */
    public void setZoom (Zoom zoom)
    {
        // Cache the zoom reference for future rendering
        this.zoom = zoom;

        Polygon polygon = section.getContour();

        if (zoom != null) {
            for (int i = 0; i < polygon.npoints; i++) {
                dspPolygon.xpoints[i] = zoom.scaled(polygon.xpoints[i]);
                dspPolygon.ypoints[i] = zoom.scaled(polygon.ypoints[i]);
            }

            dspRectangle = zoom.scaled(section.getContourBox());

            // Cache last zoom ratio
            lastZoomRatio = zoom.getRatio();
        } else {
            lastZoomRatio = 0;
        }
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
            SectionView v = (SectionView) sct.getViews()
                                             .get(viewIndex);
            int         c = v.colorIndex;

            if (c != -1) {
                colorUsed[c] = true;
            }
        }

        for (S sct : section.getSources()) {
            SectionView v = (SectionView) sct.getViews()
                                             .get(viewIndex);
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
     * @param g the graphics environment
     * @return true if the section is concerned by the clipping rectangle, which
     * means if (part of) the section has been drawn
     */
    @Implement(VertexView.class)
    public boolean render (Graphics g)
    {
        // Has zoom been modified?
        if (zoom.getRatio() != lastZoomRatio) {
            setZoom(zoom); // Update the display polygon
        }

        Rectangle clip = g.getClipBounds();

        if (clip.intersects(dspRectangle)) {
            g.setColor(color);

            double ratio = zoom.getRatio();
            g.fillPolygon(
                dspPolygon.xpoints,
                dspPolygon.ypoints,
                dspPolygon.npoints);

            // Display the section foreground value? To be improved!!!
            if (constants.displayDensity.getValue() && (ratio > 1.0)) {
                g.setColor(Color.black);
                g.drawString(
                    Integer.toString(section.getLevel()),
                    dspRectangle.x + ((dspRectangle.width / 2) - 8),
                    dspRectangle.y + ((dspRectangle.height / 2) + 5));
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
    private static class Constants
        extends ConstantSet
    {
        Constant.Double  brightness = new Constant.Double(
            0.4d,
            "Color brightness (range 0.0 .. 1.0)");
        Constant.Boolean displayDensity = new Constant.Boolean(
            false,
            "Should we render the section foreground density");
        Constant.Double  saturation = new Constant.Double(
            0.4d,
            "Color saturation (range 0.0 .. 1.0)");

        Constants ()
        {
            initialize();
        }
    }
}
