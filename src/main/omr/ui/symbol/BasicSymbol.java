//----------------------------------------------------------------------------//
//                                                                            //
//                           B a s i c S y m b o l                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.log.Logger;

import omr.score.common.PixelDimension;
import omr.score.common.PixelPoint;
import static omr.ui.symbol.Alignment.*;

import omr.util.Implement;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.Icon;

/**
 * Class {@code BasicSymbol} is the base for implementing instances of Symbol
 * interface. It does not handle a specific Shape as its subclass ShapeSymbol,
 * but only handles a sequence of MusicFont codes.
 *
 * @author Hervé Bitteur
 */
public class BasicSymbol
    implements Symbol
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    protected static final Logger logger = Logger.getLogger(BasicSymbol.class);

    /** Painting origin for images */
    protected static final PixelPoint ORIGIN = new PixelPoint(0, 0);

    /** A transformation to flip horizontally (x' = -x) */
    protected static final AffineTransform horizontalFlip = new AffineTransform(
        -1, // m00 (x' = -x)
        0, //  m01
        0, //  m02
        1, //  m10 (y' = y)
        0, //  m11
        0); // m12

    /** A transformation to flip vertically (y' = -y) */
    protected static final AffineTransform verticalFlip = new AffineTransform(
        1, // m00 (x' = x)
        0, //  m01
        0, //  m02
        -1, //  m10 (y' = -y)
        0, //  m11
        0); // m12

    /** A transformation to turn 1 quadrant clockwise  */
    protected static final AffineTransform quadrantRotateOne = AffineTransform.getQuadrantRotateInstance(
        1);

    /** A transformation to turn 2 quadrants clockwise  */
    protected static final AffineTransform quadrantRotateTwo = AffineTransform.getQuadrantRotateInstance(
        2);

    /** A transformation  for really small icon display */
    protected static AffineTransform tiny = AffineTransform.getScaleInstance(
        0.5,
        0.5);

    //~ Instance fields --------------------------------------------------------

    /** To flag an icon symbol */
    protected final boolean isIcon;

    /** Sequence of point codes */
    public final int[] codes;

    /** Related image, corresponding to standard interline */
    private BufferedImage image;

    /** Pre-scaled symbol for icon display */
    protected BasicSymbol icon;

    /** Image dimension corresponding to standard interline */
    private PixelDimension dimension;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // BasicSymbol //
    //-------------//
    /**
     * Creates a new BasicSymbol object.
     * @param isIcon true for an icon
     * @param codes the codes for MusicFont characters
     */
    public BasicSymbol (boolean isIcon,
                        int... codes)
    {
        this.isIcon = isIcon;
        this.codes = shiftedCodesOf(codes);
    }

    //-------------//
    // BasicSymbol //
    //-------------//
    /**
     * Creates a new BasicSymbol object, standard size
     * @param codes the codes for MusicFont characters
     */
    public BasicSymbol (int... codes)
    {
        this(false, codes);
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getIconHeight //
    //---------------//
    /**
     * Report the icon height
     * @return the height of the icon image in pixels
     */
    @Implement(Icon.class)
    public int getIconHeight ()
    {
        return getIcon()
                   .getHeight();
    }

    //--------------//
    // getIconImage //
    //--------------//
    /**
     * Report the icon image, suitable for icon display
     * @return the image meant for icon display
     */
    public BufferedImage getIconImage ()
    {
        return getIcon()
                   .getImage();
    }

    //--------------//
    // getIconWidth //
    //--------------//
    /**
     * Report the width of the icon (used by swing when painting)
     * @return the icon image width in pixels
     */
    @Implement(Icon.class)
    public int getIconWidth ()
    {
        return getIcon()
                   .getWidth();
    }

    //-------------//
    // getRefPoint //
    //-------------//
    /**
     * Report the symbol reference point if any, otherwise the box center
     * @return the ref point
     */
    public PixelPoint getRefPoint (Rectangle box)
    {
        return new PixelPoint(
            box.x + (box.width / 2),
            box.y + (box.height / 2));
    }

    //-----------//
    // getString //
    //-----------//
    public final String getString ()
    {
        return new String(codes, 0, codes.length);
    }

    //------------//
    // buildImage //
    //------------//
    /**
     * Build the image that represents the related shape, using the specified
     * font. The main difficulty is to determine upfront the size of the image
     * to allocate.
     * @param font properly scaled font (for interline & zoom)
     * @return the image built, or null if failed
     */
    public SymbolImage buildImage (MusicFont font)
    {
        // Params
        Params      p = getParams(font);

        // Allocate image of proper size
        SymbolImage img = new SymbolImage(
            p.rect.width,
            p.rect.height,
            p.origin);

        // Paint the image
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(OmrFont.defaultImageColor);

        // Anti-aliasing
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

        paint(g, p, ORIGIN, TOP_LEFT);

        return img;
    }

    //-----------//
    // paintIcon //
    //-----------//
    /**
     * Implements Icon interface paintIcon() method
     *
     * @param c containing component
     * @param g graphic context
     * @param x abscissa
     * @param y ordinate
     */
    @Implement(Icon.class)
    public void paintIcon (Component c,
                           Graphics  g,
                           int       x,
                           int       y)
    {
        g.drawImage(getIconImage(), x, y, c);
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint the symbol that represents the related shape, using the specified
     * font and context, the symbol being aligned at provided location
     * @param g graphic context
     * @param location where to paint the shape with provided alignment
     * @param font properly-scaled font (for interline & zoom)
     * @param alignment the way the symbol is aligned wrt the location
     */
    public void paintSymbol (Graphics2D g,
                             MusicFont  font,
                             PixelPoint location,
                             Alignment  alignment)
    {
        paint(g, getParams(font), location, alignment);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        sb.append(internalsString());

        sb.append("}");

        return sb.toString();
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the normalized bounding dimension of the symbol
     * @return the size of the symbol
     */
    protected PixelDimension getDimension ()
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
     * Report what would be the bounding dimension of the symbol, if painted
     * with the provided font
     * @return the potential size of the painted symbol
     */
    protected PixelDimension getDimension (MusicFont font)
    {
        Dimension dim = getDimension();
        double    ratio = font.getSize2D() / MusicFont.baseMusicFont.getSize2D();

        return new PixelDimension(
            (int) Math.ceil(dim.width * ratio),
            (int) Math.ceil(dim.height * ratio));
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the height of the symbol, for a standard interline
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
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();

        p.layout = layout(font);

        Rectangle2D r = p.layout.getBounds();

        p.rect = new Rectangle(
            (int) Math.ceil(r.getWidth()),
            (int) Math.ceil(r.getHeight()));

        return p;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width of the symbol, for a standard interline
     * @return the real image width in pixels
     */
    protected int getWidth ()
    {
        if (dimension == null) {
            computeImage();
        }

        return dimension.width;
    }

    //------------//
    // createIcon //
    //------------//
    /**
     * To be redefined by each subclass in order to create a icon symbol using
     * the subclass
     * @return the icon-sized instance of proper symbol class
     */
    protected BasicSymbol createIcon ()
    {
        return new BasicSymbol(true, codes);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();

        if (isIcon) {
            sb.append(" icon");
        }

        //        sb.append(" dim:")
        //          .append(getWidth())
        //          .append("x")
        //          .append(getHeight());
        //
        if (codes != null) {
            sb.append(" codes:")
              .append(Arrays.toString(codes));
        }

        return sb.toString();
    }

    //--------//
    // layout //
    //--------//
    /**
     * Report a single layout, based on symbol codes if they exist. This feature
     * can work only with a single "line" of music codes.
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
     * Actual painting, to be redefined by subclasses if needed
     * @param g graphics context
     * @param p the parameters fed by getParams()
     * @param location where to paint
     * @param alignment relative position of provided location wrt symbol
     */
    protected void paint (Graphics2D g,
                          Params     p,
                          PixelPoint location,
                          Alignment  alignment)
    {
        OmrFont.paint(g, p.layout, location, alignment);
    }

    //---------//
    // getIcon //
    //---------//
    private BasicSymbol getIcon ()
    {
        if (isIcon) {
            return null;
        } else {
            if (icon == null) {
                icon = createIcon();
            }

            return icon;
        }
    }

    //--------------//
    // computeImage //
    //--------------//
    private void computeImage ()
    {
        image = buildImage(
            isIcon ? MusicFont.iconMusicFont : MusicFont.baseMusicFont);

        dimension = new PixelDimension(image.getWidth(), image.getHeight());
    }

    //----------------//
    // shiftedCodesOf //
    //----------------//
    /**
     * Make sure the codes are above the '0xf000' value
     * @param codes raw codes
     * @return codes suitable for font display
     */
    private int[] shiftedCodesOf (int... codes)
    {
        int[] values = new int[codes.length];

        for (int i = 0; i < codes.length; i++) {
            if (codes[i] < 0xf000) {
                values[i] = codes[i] + 0xf000;
            } else {
                values[i] = codes[i];
            }
        }

        return values;
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // Params //
    //--------//
    /**
     * A set of parameters used for building an image and for painting a symbol
     */
    protected class Params
    {
        //~ Instance fields ----------------------------------------------------

        /** Specific origin if any */
        Point origin;

        /** (Main) layout */
        TextLayout layout;

        /** Image bounds */
        Rectangle rect;
    }
}
