//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B a s i c S y m b o l                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.math.PointUtil;
import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;
import static org.audiveris.omr.ui.symbol.Alignment.TOP_LEFT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * Class <code>BasicSymbol</code> is the base for implementing instances of {@link SymbolIcon}
 * interface.
 * <p>
 * It does not handle a specific Shape as its subclass {@link ShapeSymbol}, but only handles a
 * sequence of MusicFont codes.
 *
 * @author Hervé Bitteur
 */
public class BasicSymbol
        implements SymbolIcon, Cloneable
{
    //~ Static fields/initializers -----------------------------------------------------------------

    protected static final Logger logger = LoggerFactory.getLogger(BasicSymbol.class);

    /** A transformation to flip horizontally (x' = -x). */
    protected static final AffineTransform horizontalFlip = new AffineTransform(
            -1, // m00 (x' = -x)
            0, //  m01
            0, //  m02
            1, //  m10 (y' = y)
            0, //  m11
            0); // m12

    /** A transformation to flip vertically (y' = -y). */
    protected static final AffineTransform verticalFlip = new AffineTransform(
            1, // m00 (x' = x)
            0, //  m01
            0, //  m02
            -1, //  m10 (y' = -y)
            0, //  m11
            0); // m12

    /** A transformation to turn 1 quadrant clockwise. */
    protected static final AffineTransform quadrantRotateOne = AffineTransform
            .getQuadrantRotateInstance(1);

    /** A transformation to turn 2 quadrants clockwise. */
    protected static final AffineTransform quadrantRotateTwo = AffineTransform
            .getQuadrantRotateInstance(2);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sequence of point codes. */
    public final int[] codes;

    /** Offset of centroid WRT area center, specified in ratio of width and height. */
    protected Point2D centroidOffset;

    /** Related image, corresponding to standard interline. */
    protected SymbolImage image;

    /** Image dimension corresponding to standard interline. */
    protected Dimension dimension;

    /** Indicates this is a tiny version. */
    protected boolean isTiny;

    /** Related tiny version, if any. */
    protected BasicSymbol tinyVersion;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicSymbol object, standard size.
     *
     * @param codes the codes for MusicFont characters
     */
    public BasicSymbol (int... codes)
    {
        this.codes = codes;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // buildImage //
    //------------//
    @Override
    public SymbolImage buildImage (MusicFont font)
    {
        return buildImage(font, null);
    }

    //------------//
    // buildImage //
    //------------//
    @Override
    public SymbolImage buildImage (MusicFont font,
                                   Stroke curveStroke)
    {
        // Params
        Params p = getParams(font);

        // Allocate image of proper size
        Rectangle intRect = p.rect.getBounds();
        SymbolImage img = new SymbolImage(intRect.width,
                                          intRect.height,
                                          PointUtil.rounded(p.offset));

        // Paint the image
        Graphics2D g = (Graphics2D) img.getGraphics();

        if (curveStroke != null) {
            g.setStroke(curveStroke);
        }

        g.setColor(OmrFont.defaultImageColor);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        final Point2D center = new Point2D.Double(intRect.width / 2.0, intRect.height / 2.0);
        paint(g, p, center, AREA_CENTER);

        return img;
    }

    //-------------//
    // getCentroid //
    //-------------//
    @Override
    public Point getCentroid (Rectangle box)
    {
        if (centroidOffset == null) {
            computeCentroidOffset();
        }

        return new Point(
                (int) Math.rint(box.getCenterX() + (box.getWidth() * centroidOffset.getX())),
                (int) Math.rint(box.getCenterY() + (box.getHeight() * centroidOffset.getY())));
    }

    //--------------//
    // getDimension //
    //--------------//
    @Override
    public Dimension getDimension (MusicFont font)
    {
        Params p = getParams(font);

        return new Dimension((int) Math.rint(p.rect.getWidth()),
                             (int) Math.rint(p.rect.getHeight()));
    }

    //---------------//
    // getIconHeight //
    //---------------//
    /**
     * Report the tiny version height.
     *
     * @return the height of the tiny version image in pixels
     */
    @Override
    public int getIconHeight ()
    {
        return getTinyVersion().getHeight();
    }

    //--------------//
    // getIconWidth //
    //--------------//
    /**
     * Report the width of the tiny version (used by swing when painting).
     *
     * @return the tiny version image width in pixels
     */
    @Override
    public int getIconWidth ()
    {
        return getTinyVersion().getWidth();
    }

    //-------------------//
    // getIntrinsicImage //
    //-------------------//
    @Override
    public SymbolImage getIntrinsicImage ()
    {
        if (dimension == null) {
            computeImage();
        }

        return image;
    }

    //-----------//
    // getString //
    //-----------//
    /**
     * Report the String defined by Unicode characters
     *
     * @return the resulting String
     */
    public final String getString ()
    {
        return new String(codes, 0, codes.length);
    }

    //---------------//
    // getHexaString //
    //---------------//
    /**
     * Report the symbol code(s) as an hexadecimal string
     *
     * @return the codes hexadecimal value
     */
    public String getHexaString ()
    {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < codes.length; i++) {
            if (i > 0) {
                sb.append(',');
            }

            sb.append(Long.toHexString(((Number) codes[i]).longValue()));
        }

        return sb.toString();
    }

    //----------------//
    // getTinyVersion //
    //----------------//
    /**
     * Report the tiny version of this symbol.
     *
     * @return the related tiny version for a standard symbol, this symbol if already tiny.
     */
    public BasicSymbol getTinyVersion ()
    {
        if (isTiny) {
            return this;
        }

        if (tinyVersion == null) {
            tinyVersion = createTinyVersion();
        }

        return tinyVersion;
    }

    //-----------//
    // paintIcon //
    //-----------//
    /**
     * Implements Icon interface paintIcon() method.
     *
     * @param c containing component
     * @param g graphic context
     * @param x abscissa
     * @param y ordinate
     */
    @Override
    public void paintIcon (Component c,
                           Graphics g,
                           int x,
                           int y)
    {
        g.drawImage(getTinyVersion().getIntrinsicImage(), x, y, c);
    }

    //-------------//
    // paintSymbol //
    //-------------//
    @Override
    public void paintSymbol (Graphics2D g,
                             MusicFont font,
                             Point2D location,
                             Alignment alignment)
    {
        paint(g, getParams(font), location, alignment);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        sb.append(internals());

        sb.append("}");

        return sb.toString();
    }

    //--------------//
    // computeImage //
    //--------------//
    protected void computeImage ()
    {
        image = buildImage(isTiny ? MusicFont.tinyMusicFont : MusicFont.baseMusicFont);

        dimension = new Dimension(image.getWidth(), image.getHeight());
    }

    //-------------------//
    // createTinyVersion //
    //-------------------//
    /**
     * Generate the tiny version of this symbol.
     *
     * @return the tiny-sized instance of proper symbol class
     */
    protected BasicSymbol createTinyVersion ()
    {
        try {
            final BasicSymbol clone = (BasicSymbol) clone();
            clone.setTiny();

            return clone;
        } catch (CloneNotSupportedException ex) {
            logger.error("Clone not supported");
            return null;
        }
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the normalized bounding dimension of the symbol.
     *
     * @return the size of the symbol
     */
    protected Dimension getDimension ()
    {
        if (dimension == null) {
            computeImage();
        }

        return dimension;
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the height of the symbol, for a standard interline.
     *
     * @return the real image height in pixels
     */
    protected int getHeight ()
    {
        if (dimension == null) {
            computeImage();
        }

        return dimension.height;
    }

    //-----------//
    // getParams //
    //-----------//
    /**
     * Report the specific Params object to draw this symbol.
     *
     * @param font scaled music font
     * @return the specific Params object
     */
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();

        p.layout = layout(font);
        p.rect = p.layout.getBounds();

        return p;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width of the symbol, for a standard interline.
     *
     * @return the real image width in pixels
     */
    protected int getWidth ()
    {
        if (dimension == null) {
            computeImage();
        }

        return dimension.width;
    }

    //-----------//
    // internals //
    //-----------//
    /**
     * Report a string description of internals.
     *
     * @return string of internal informations
     */
    protected String internals ()
    {
        final StringBuilder sb = new StringBuilder();

        if (isTiny) {
            sb.append(" TINY");
        }

        if (codes != null) {
            sb.append(" codes:").append(Arrays.toString(codes));
        }

        return sb.toString();
    }

    //--------//
    // layout //
    //--------//
    /**
     * Report a single layout, based on symbol codes if they exist.
     * This feature can work only with a single "line" of music codes.
     *
     * @param font the specifically-scaled font to use
     * @return the layout ready to be drawn, or null
     */
    protected TextLayout layout (MusicFont font)
    {
        return font.layout(getString());
    }

    //-------//
    // paint //
    //-------//
    /**
     * Actual painting, to be redefined by subclasses if needed.
     *
     * @param g         graphics context
     * @param p         the parameters fed by getParams()
     * @param location  where to paint
     * @param alignment relative position of provided location WRT symbol
     */
    protected void paint (Graphics2D g,
                          Params p,
                          Point2D location,
                          Alignment alignment)
    {
        OmrFont.paint(g, p.layout, location, alignment);
    }

    //-----------------------//
    // computeCentroidOffset //
    //-----------------------//
    private void computeCentroidOffset ()
    {
        if (dimension == null) {
            computeImage();
        }

        double xBar = 0;
        double yBar = 0;
        double weight = 0;

        for (int y = 0, h = image.getHeight(); y < h; y++) {
            for (int x = 0, w = image.getWidth(); x < w; x++) {
                Color pix = new Color(image.getRGB(x, y), true);
                double alpha = pix.getAlpha() / 255.0;

                if (alpha > 0) {
                    xBar += (alpha * x);
                    yBar += (alpha * y);
                    weight += alpha;
                }
            }
        }

        xBar /= weight;
        yBar /= weight;

        centroidOffset = new Point2D.Double(
                (xBar / image.getWidth()) - 0.5,
                (yBar / image.getHeight()) - 0.5);
    }

    //---------//
    // setTiny //
    //---------//
    private void setTiny ()
    {
        isTiny = true;
        computeImage(); // Recompute image and dimension as well
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Params //
    //--------//
    /**
     * The set of parameters used for building an image and for painting a symbol.
     */
    protected static class Params
    {

        /**
         * Specific offset, if any, for focus center off of area center.
         * Since user pointing location is by default taken as center of 'rect' bounds.
         */
        public Point2D offset;

        /** (Main) layout. */
        public TextLayout layout;

        /** By convention, <b>FULL</b> symbol bounds, including decorations if any. */
        public Rectangle2D rect;

        /**
         * Report the vector to translate symbol to the provided focusLocation.
         *
         * @param focusLocation mouse location
         * @return the translation vector to apply
         */
        public Point2D vectorTo (Point2D focusLocation)
        {
            final Point2D topLeft = AREA_CENTER.translatedPoint(TOP_LEFT, rect, focusLocation);

            if (offset != null) {
                topLeft.setLocation(topLeft.getX() - offset.getX(),
                                    topLeft.getY() - offset.getY());
            }

            topLeft.setLocation(topLeft.getX() - rect.getX(),
                                topLeft.getY() - rect.getY());

            return topLeft;
        }
    }
}
