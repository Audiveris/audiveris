//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S h a p e S y m b o l                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import static org.audiveris.omr.classifier.SampleRepository.STANDARD_INTERLINE;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.ui.ObjectUIModel;
import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;
import static org.audiveris.omr.ui.symbol.Alignment.TOP_LEFT;
import static org.audiveris.omr.ui.symbol.MusicFont.TINY_INTERLINE;
import static org.audiveris.omr.ui.symbol.OmrFont.defaultImageColor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import javax.swing.Icon;

/**
 * Class <code>ShapeSymbol</code> handles a graphic symbol for a related {@link Shape}.
 * <p>
 * A ShapeSymbol provides several features:
 * <ul>
 * <li>It can be used as an <b>icon</b> for buttons, menus, etc. For that purpose,
 * it implements the {@link Icon} interface.</li>
 * <li>It can be used for Drag n' Drop operations, since it implements the {@link Transferable}
 * interface.
 * <li>It can be used as an <b>image</b> for precise drawing on score views, whatever the desired
 * scale and display ratio. See {@link #buildImage} and {@link #paintSymbol} methods.</li>
 * <li>It may also be used to <b>train</b> the glyph classifier when we don't have enough "real"
 * glyphs available.</li>
 * <li>It may also be used to convey the <b>reference point</b> of that shape.
 * Most shapes have no reference point, and thus we use their area center, which is the center of
 * their bounding box.
 * However, a few shapes (e.g. clefs to precisely position them on the staff) need a very precise
 * reference center (actually the y ordinate) which is somewhat different from the area center.</li>
 * </ul>
 * Beside the plain shape image, a ShapeSymbol may also provide a <b>decorated</b> version
 * whose image represents the shape within a larger decoration.
 *
 * @author Hervé Bitteur
 */
public class ShapeSymbol
        implements Icon, Cloneable, Transferable
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ShapeSymbol.class);

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

    /** The symbol meta data. */
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(ShapeSymbol.class, "shape-symbol");

    /** Composite used for decoration. */
    protected static final AlphaComposite decoComposite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            0.15f);

    //~ Instance fields ----------------------------------------------------------------------------

    /** MusicFont family. */
    protected final MusicFamily musicFamily;

    /** Related shape. */
    protected final Shape shape;

    /** Offset of centroid WRT area center, specified in ratio of width and height. */
    protected Point2D centroidOffset;

    /** Related image, corresponding to standard interline. */
    protected SymbolImage image;

    /** Image dimension corresponding to standard interline. */
    protected Dimension dimension;

    /** Indicates whether this is a tiny version (for icon). */
    protected boolean isTiny;

    /** Related tiny version, if any, for icon. */
    protected ShapeSymbol tinyVersion;

    /** Is this a decorated symbol. (shape with additional decoration) */
    protected boolean isDecorated;

    /** Decorated version if any. */
    protected ShapeSymbol decoratedVersion;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a standard ShapeSymbol with the provided shape and font family.
     *
     * @param shape       the related shape
     * @param musicFamily the MusicFont family
     */
    public ShapeSymbol (Shape shape,
                        MusicFamily musicFamily)
    {
        this.shape = shape;
        this.musicFamily = musicFamily;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------------//
    // buildImage //
    //------------//
    /**
     * Build an image that represents the related shape, using the provided specific font.
     *
     * @param font properly-scaled font (for interline and zoom)
     * @return the image built, or null if failed
     */
    public SymbolImage buildImage (MusicFont font)
    {
        return buildImage(font, null);
    }

    //------------//
    // buildImage //
    //------------//
    /**
     * Build an image that represents the related shape, using the provided specific font
     * and specific stroke for curves.
     *
     * @param font        properly-scaled font (for interline and zoom)
     * @param curveStroke optional stroke for slurs, wedges and endings, or null
     * @return the image built, or null if failed
     */
    public SymbolImage buildImage (MusicFont font,
                                   Stroke curveStroke)
    {
        // Params
        Params p = getParams(font);

        // Allocate image of proper size
        Rectangle intRect = p.rect.getBounds();
        SymbolImage img = new SymbolImage(
                intRect.width,
                intRect.height,
                PointUtil.rounded(p.offset));

        // Paint the image
        Graphics2D g = (Graphics2D) img.getGraphics();

        if (curveStroke != null) {
            g.setStroke(curveStroke);
        }

        g.setColor(logger.isDebugEnabled() ? Color.MAGENTA : defaultImageColor);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        paint(g, p, new Point(0, 0), TOP_LEFT);

        return img;
    }

    //-----------------------//
    // computeCentroidOffset //
    //-----------------------//
    private void computeCentroidOffset ()
    {
        if (image == null) {
            computeImage();
        }

        double xBar = 0;
        double yBar = 0;
        double weight = 0;

        for (int y = 0, h = image.getHeight(); y < h; y++) {
            for (int x = 0, w = image.getWidth(); x < w; x++) {
                final Color pix = new Color(image.getRGB(x, y), true);
                final double alpha = pix.getAlpha() / 255.0;

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
    /**
     * Compute the symbol image, according to its font family, and to its potential decorated
     * and tiny attributes.
     */
    protected void computeImage ()
    {
        logger.trace("computeImage for {}", this);
        final int interline = isTiny ? TINY_INTERLINE : STANDARD_INTERLINE;
        final MusicFont font = MusicFont.getBaseFont(musicFamily, interline);
        image = buildImage(font);
        dimension = new Dimension(image.getWidth(), image.getHeight());
    }

    //------------------------//
    // createDecoratedVersion //
    //------------------------//
    /**
     * Create symbol with proper decorations.
     * <p>
     * To be redefined by each subclass that does provide a specific decorated version
     *
     * @return the decorated version, which may be the same symbol if no decoration exists
     */
    protected ShapeSymbol createDecoratedVersion ()
    {
        if (!supportsDecoration()) {
            return null; // No decoration by default
        }

        try {
            final ShapeSymbol clone = (ShapeSymbol) clone();
            clone.isDecorated = true;
            clone.computeImage(); // Recompute image and dimension as well

            return clone;
        } catch (CloneNotSupportedException ex) {
            logger.warn("No decorated glyph for shape: {}", shape);

            return null;
        }
    }

    //-------------------//
    // createTinyVersion //
    //-------------------//
    /**
     * Generate the tiny version of this symbol.
     *
     * @return the tiny-sized instance of proper symbol class
     */
    protected ShapeSymbol createTinyVersion ()
    {
        try {
            final ShapeSymbol clone = (ShapeSymbol) clone();
            clone.isTiny = true;
            clone.computeImage(); // Recompute image and dimension as well

            return clone;
        } catch (CloneNotSupportedException ex) {
            logger.error("Clone not supported");
            return null;
        }
    }

    //-------------//
    // getCentroid //
    //-------------//
    /**
     * Report the symbol mass center.
     *
     * @param box the contour box of the entity (symbol or glyph)
     * @return the mass center
     */
    public Point getCentroid (Rectangle box)
    {
        if (centroidOffset == null) {
            computeCentroidOffset();
        }

        return new Point(
                (int) Math.rint(box.getCenterX() + (box.getWidth() * centroidOffset.getX())),
                (int) Math.rint(box.getCenterY() + (box.getHeight() * centroidOffset.getY())));
    }

    //---------------------//
    // getDecoratedVersion //
    //---------------------//
    /**
     * Report the (preferably decorated) version of this ShapeSymbol.
     *
     * @return the decorated version if any, otherwise the plain version
     */
    public ShapeSymbol getDecoratedVersion ()
    {
        if (isDecorated) {
            return this;
        }

        if (!supportsDecoration()) {
            return this; // Fallback using the non-decorated version
        } else {
            if (decoratedVersion == null) {
                decoratedVersion = createDecoratedVersion();
            }

            return decoratedVersion;
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

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the bounding dimension of this symbol for the provided font.
     *
     * @param font (scaled) music font
     * @return the bounding dimension
     */
    public Dimension getDimension (MusicFont font)
    {
        Params p = getParams(font);

        if (p.rect == null) {
            return null;
        }

        return new Dimension(
                (int) Math.rint(p.rect.getWidth()),
                (int) Math.rint(p.rect.getHeight()));
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

    //-----------//
    // getLayout //
    //-----------//
    /**
     * Report this symbol layout when using the provided font.
     *
     * @param font the provided font
     * @return symbol layout
     */
    public TextLayout getLayout (MusicFont font)
    {
        return getParams(font).layout;
    }

    //----------//
    // getModel //
    //----------//
    /**
     * Get inter geometry from this (dropped) symbol.
     *
     * @param font     properly scaled font
     * @param location dropping location
     * @return the data model for inter being shaped
     * @throws UnsupportedOperationException if operation is not explicitly supported by the symbol
     */
    public ObjectUIModel getModel (MusicFont font,
                                   Point location)
    {
        throw new UnsupportedOperationException(); // By default
    }

    //----------------//
    // getMusicFamily //
    //----------------//
    public MusicFamily getMusicFamily ()
    {
        return musicFamily;
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

        p.layout = font.layoutShapeByCode(shape);
        p.rect = p.layout.getBounds();

        return p;
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the shape of the symbol.
     *
     * @return the shape
     */
    public Shape getShape ()
    {
        return shape;
    }

    //----------------//
    // getTinyVersion //
    //----------------//
    /**
     * Report the tiny version of this symbol.
     *
     * @return the related tiny version for a standard symbol, this symbol if already tiny.
     */
    public ShapeSymbol getTinyVersion ()
    {
        if (isTiny) {
            return this;
        }

        if (tinyVersion == null) {
            tinyVersion = createTinyVersion();
        }

        return tinyVersion;
    }

    //--------//
    // getTip //
    //--------//
    /**
     * Report the tip text to display for this symbol.
     *
     * @return the tip text
     */
    public String getTip ()
    {
        return shape.toString(); // By default, use the shape name
    }

    //-----------------//
    // getTransferData //
    //-----------------//
    @Override
    public Object getTransferData (DataFlavor flavor)
        throws UnsupportedFlavorException, IOException
    {
        if (isDataFlavorSupported(flavor)) {
            return this;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    //------------------------//
    // getTransferDataFlavors //
    //------------------------//
    @Override
    public DataFlavor[] getTransferDataFlavors ()
    {
        return new DataFlavor[] { DATA_FLAVOR };
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
        sb.append(" @").append(Integer.toHexString(this.hashCode()));
        sb.append(' ').append(shape);

        if (isDecorated) {
            sb.append(" DECORATED");
        }

        if (isTiny) {
            sb.append(" TINY");
        }

        sb.append(' ').append(musicFamily);

        return sb.toString();
    }

    //-----------------------//
    // isDataFlavorSupported //
    //-----------------------//
    @Override
    public boolean isDataFlavorSupported (DataFlavor flavor)
    {
        return flavor == DATA_FLAVOR;
    }

    //-------------//
    // isDecorated //
    //-------------//
    /**
     * Tell whether the image represents the shape with additional decorations.
     *
     * @return true if decorated
     */
    public boolean isDecorated ()
    {
        return isDecorated;
    }

    //-------//
    // paint //
    //-------//
    /**
     * Actual painting, to be redefined by subclasses if needed.
     * <p>
     * This default implementation paints only the 'params.layout' item.
     *
     * @param g         graphics context
     * @param params    the parameters fed by getParams()
     * @param location  where to paint
     * @param alignment relative position of provided location WRT symbol
     */
    protected void paint (Graphics2D g,
                          Params params,
                          Point2D location,
                          Alignment alignment)
    {
        logger.trace("ShapeSymbol.paint {}", this);
        OmrFont.paint(g, params.layout, location, alignment);
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
        logger.trace("ShapeSymbol.paintIcon {} family:{}", this, musicFamily);
        final MusicFont font = MusicFont.getBaseFont(musicFamily, TINY_INTERLINE);
        final Graphics2D g2 = (Graphics2D) g;
        g.setColor(logger.isDebugEnabled() ? Color.BLUE : defaultImageColor);
        paint(g2, getParams(font), new Point(x, y), Alignment.TOP_LEFT);
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint the symbol that represents the related shape, using the scaled font and
     * context, the symbol being aligned at provided location.
     *
     * @param g         graphic context
     * @param font      properly-scaled font (for interline &amp; zoom)
     * @param location  where to paint the shape with provided alignment
     * @param alignment the way the symbol is aligned WRT the location
     */
    public void paintSymbol (Graphics2D g,
                             MusicFont font,
                             Point2D location,
                             Alignment alignment)
    {
        logger.trace("ShapeSymbol.paintSymbol {}", this);
        paint(g, getParams(font), location, alignment);
    }

    //--------------------//
    // supportsDecoration //
    //--------------------//
    /**
     * Report whether this symbol class can have a decorated version.
     * <p>
     * Answer is false by default and thus must be overridden by any ShapeSymbol subclass that does
     * provide support for decoration.
     *
     * @return true if so
     */
    protected boolean supportsDecoration ()
    {
        return false; // By default
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return new StringBuilder(getClass().getSimpleName()).append("{").append(internals()).append(
                "}").toString();
    }

    //-------------//
    // updateModel //
    //-------------//
    /**
     * Tell the symbol that it can update its model with sheet informations.
     * <p>
     * This is useful when the dragged item enters a sheet view, since it can adapt itself to
     * sheet informations (such as the typical beam thickness).
     *
     * @param sheet underlying sheet
     */
    public void updateModel (Sheet sheet)
    {
        // Void, by default
    }

    //-------------//
    // updateModel //
    //-------------//
    /**
     * Tell the symbol that it can update its model with staff informations.
     * <p>
     * This is useful when the dragged item enters a staff, since it can adapt itself to
     * staff information (such as the typical beam thickness for small staff).
     *
     * @param staff underlying staff
     */
    public void updateModel (Staff staff)
    {
        // Void, by default
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
                topLeft.setLocation(topLeft.getX() - offset.getX(), topLeft.getY() - offset.getY());
            }

            topLeft.setLocation(topLeft.getX() - rect.getX(), topLeft.getY() - rect.getY());

            return topLeft;
        }
    }
}
