//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B a s i c S y m b o l                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import static org.audiveris.omr.ui.symbol.Alignment.*;

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
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BasicSymbol} is the base for implementing instances of {@link SymbolIcon}
 * interface.
 * <p>
 * It does not handle a specific Shape as its subclass {@link ShapeSymbol}, but only handles a
 * sequence of MusicFont codes.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "symbol")
public class BasicSymbol
        implements SymbolIcon
{

    protected static final Logger logger = LoggerFactory.getLogger(BasicSymbol.class);

    /** Painting origin for images. */
    protected static final Point ORIGIN = new Point(0, 0);

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

    /** A transformation for really small icon display. */
    protected static final AffineTransform tiny = AffineTransform.getScaleInstance(0.5, 0.5);

    /** Sequence of point codes. */
    public final int[] codes;

    /** To flag an icon symbol. */
    protected final boolean isIcon;

    /** Pre-scaled symbol for icon display. */
    protected BasicSymbol icon;

    /** Offset of centroid WRT area center, specified in ratio of width and height. */
    protected Point2D centroidOffset;

    /** Related image, corresponding to standard interline. */
    private BufferedImage image;

    /** Image dimension corresponding to standard interline. */
    private Dimension dimension;

    /**
     * Creates a new BasicSymbol object.
     *
     * @param isIcon true for an icon, false for standard size
     * @param codes  the codes for MusicFont characters
     */
    public BasicSymbol (boolean isIcon,
                        int... codes)
    {
        this.isIcon = isIcon;
        this.codes = shiftedCodesOf(codes);
    }

    /**
     * Creates a new BasicSymbol object, standard size.
     *
     * @param codes the codes for MusicFont characters
     */
    public BasicSymbol (int... codes)
    {
        this(false, codes);
    }

    /**
     * Useful?
     */
    public BasicSymbol ()
    {
        this.isIcon = false;
        this.codes = null;
    }

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
        SymbolImage img = new SymbolImage((int) Math.rint(p.rect.getWidth()),
                                          (int) Math.rint(p.rect.getHeight()),
                                          PointUtil.rounded(p.offset));

        // Paint the image
        Graphics2D g = (Graphics2D) img.getGraphics();

        if (curveStroke != null) {
            g.setStroke(curveStroke);
        }

        g.setColor(OmrFont.defaultImageColor);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        paint(g, p, ORIGIN, TOP_LEFT);

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
     * Report the icon height.
     *
     * @return the height of the icon image in pixels
     */
    @Override
    public int getIconHeight ()
    {
        return getIcon().getHeight();
    }

    //--------------//
    // getIconImage //
    //--------------//
    @Override
    public BufferedImage getIconImage ()
    {
        return getIcon().getImage();
    }

    //--------------//
    // getIconWidth //
    //--------------//
    /**
     * Report the width of the icon (used by swing when painting).
     *
     * @return the icon image width in pixels
     */
    @Override
    public int getIconWidth ()
    {
        return getIcon().getWidth();
    }

    //-------------//
    // getRefPoint //
    //-------------//
    @Override
    public Point getRefPoint (Rectangle box)
    {
        return new Point(box.x + (box.width / 2), box.y + (box.height / 2));
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
        g.drawImage(getIconImage(), x, y, c);
    }

    //-------------//
    // paintSymbol //
    //-------------//
    @Override
    public void paintSymbol (Graphics2D g,
                             MusicFont font,
                             Point location,
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

    //------------//
    // createIcon //
    //------------//
    /**
     * To be redefined by each subclass to create a icon symbol using the subclass.
     *
     * @return the icon-sized instance of proper symbol class
     */
    protected BasicSymbol createIcon ()
    {
        return new BasicSymbol(true, codes);
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

    //----------//
    // getImage //
    //----------//
    /**
     * Report the underlying image, scaled for standard interline value.
     *
     * @return the underlying image
     */
    protected BufferedImage getImage ()
    {
        if (dimension == null) {
            computeImage();
        }

        return image;
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
        StringBuilder sb = new StringBuilder();

        if (isIcon) {
            sb.append(" icon");
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

    //--------------//
    // computeImage //
    //--------------//
    private void computeImage ()
    {
        image = buildImage(isIcon ? MusicFont.iconMusicFont : MusicFont.baseMusicFont);

        dimension = new Dimension(image.getWidth(), image.getHeight());
    }

    //---------//
    // getIcon //
    //---------//
    /**
     * Report the icon version of this symbol.
     *
     * @return the related icon for a standard symbol, this symbol if already the icon.
     */
    public BasicSymbol getIcon ()
    {
        if (isIcon) {
            return this;
        }

        if (icon == null) {
            icon = createIcon();
        }

        return icon;
    }

    //----------------//
    // shiftedCodesOf //
    //----------------//
    /**
     * Make sure the codes are above the CODE_OFFSET value.
     *
     * @param codes raw codes
     * @return codes suitable for font display
     */
    private int[] shiftedCodesOf (int[] codes)
    {
        int[] values = new int[codes.length];

        for (int i = 0; i < codes.length; i++) {
            if (codes[i] < MusicFont.CODE_OFFSET) {
                values[i] = codes[i] + MusicFont.CODE_OFFSET;
            } else {
                values[i] = codes[i];
            }
        }

        return values;
    }

    //--------//
    // Params //
    //--------//
    /**
     * The set of parameters used for building an image and for painting a symbol.
     */
    protected static class Params
    {

        /**
         * Specific offset, if any, for user pointer off of area center.
         * Since user pointing location is by default taken as center of 'rect' bounds.
         */
        public Point2D offset;

        /** (Main) layout. */
        public TextLayout layout;

        /** By convention, <b>FULL</b> symbol bounds, including decorations if any. */
        public Rectangle2D rect;
    }
}
