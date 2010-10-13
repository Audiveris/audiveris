//----------------------------------------------------------------------------//
//                                                                            //
//                           S h a p e S y m b o l                            //
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


import omr.util.Implement;
import omr.util.PointFacade;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.IOException;

import javax.swing.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

/**
 * Class {@code ShapeSymbol} handles the appearance of a monochrome music
 * symbol. A ShapeSymbol can provide several features:<ul>
 *
 * <li>It can be used as an <b>icon</b> for buttons, menus, etc. For that
 * purpose, the {@code ShapeSymbol} implements the {@link Icon} interface.</li>
 *
 * <li>It can be used as an <b>image</b> for precise drawing on score and
 * sheet views.
 * The {@code ShapeSymbol} provides {@link #getDimension()}, as well as
 * {@link #getWidth()} and {@link #getHeight()} methods that report the
 * <b>normalized</> size of the symbol for a standard interline value, even if
 * the size of the underlying image is different. This allows better symbol
 * definitions.</li>
 *
 * <li>It may also be used to <b>train</> the glyph evaluator when we don't
 * have enough "real" glyphs available.</li>
 *
 * <li>It may also be used to convey information on the related shape and
 * especially the <b>reference point</b> of that shape.
 * Most of shapes have no reference point, and thus we use their area center,
 * which is the center of their bounding box.
 * However, a few shapes (clefs to precisely position them  on the staff,
 * head/flags combos to handle the precise position of the head part) need a
 * very precise reference center (actually the y ordinate) which is somewhat
 * different from the area center. This is the difference between the
 * {@link #getAreaCenter} and the {@link #getCenter} methods.</li></ul>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "", propOrder =  {
    "baseColor", "xmlRefPoint", "stemNumber", "withLedger", "pitchPosition", "bitmap"}
)
@XmlRootElement(name = "symbol")
public class ShapeSymbol
    implements Icon, Transferable
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ShapeSymbol.class);

    /** The symbol meta data  */
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(
        ShapeSymbol.class,
        "symbol");

    /** Ratio applied to a normalized image to get the related icon */
    public static double iconRatio = 0.5d;

    //~ Instance fields --------------------------------------------------------

    /** Image related interline value */
    @XmlAttribute
    private Integer interline;

    /** Related name (generally the name of the related shape if any) */
    @XmlAttribute
    private String name;

    /** Base color */
    @XmlElement(name = "base-color")
    private BaseColor baseColor;

    /** How many stems is it connected to ? */
    @XmlElement(name = "stem-number")
    private Integer stemNumber;

    /** Connected to Ledger ? */
    @XmlElement(name = "with-ledger")
    private Boolean withLedger;

    /** Pitch position within staff lines */
    @XmlElement(name = "pitch-position")
    private Double pitchPosition;

    /** Wrapping of the image for (un)marshalling */
    @XmlElementWrapper(name = "bitmap")
    @XmlElement(name = "row")
    public String[] bitmap;

    /**
     * Reference point, if any. (Un)Marshalling is done through getXmlRefPoint()
     */
    private Point refPoint;

    /** Mass center */
    private Point centroid;

    /**
     * Related image, full scale. Generally interline = {link
     * omr.score.ui.ScoreConstants#INTER_LINE}, but it can be larger for better
     * display.
     */
    private BufferedImage image;

    /** Image dimension normalized at INTER_LINE value */
    private Dimension dimension;

    /** Pre-scaled image for icon display (half-scale) */
    private BufferedImage iconImage;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // ShapeSymbol //
    //-------------//
    /**
     * No-arg constructor for the XML mapper
     */
    public ShapeSymbol ()
    {
    }

    //-------------//
    // ShapeSymbol //
    //-------------//
    /**
     * Create a symbol icon with the provided image
     *
     * @param image the icon image
     * @param interline the interline value used
     */
    public ShapeSymbol (BufferedImage image,
                        int           interline)
    {
        this.interline = interline;
        this.image = image;

        computeData();
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getAreaCenter //
    //---------------//
    /**
     * Report the area center for the symbol
     * @return the area center (coordinates with origin at upper left)
     */
    public Point getAreaCenter ()
    {
        return new Point(dimension.width / 2, dimension.height / 2);
    }

    //-----------//
    // getBitmap //
    //-----------//
    /**
     * Report an array of strings that describe the bitmap (needed for JAXB)
     *
     * @return the array of strings for file storing
     */
    public String[] getBitmap ()
    {
        if (getIconHeight() != -1) {
            // Generate the string array from the icon image
            bitmap = SymbolManager.getInstance()
                                  .encodeImage(this);
        }

        return bitmap;
    }

    //-----------//
    // getCenter //
    //-----------//
    public Point getCenter ()
    {
        if (refPoint != null) {
            return refPoint;
        } else {
            return getAreaCenter();
        }
    }

    //-------------//
    // getCentroid //
    //-------------//
    /**
     * Report the mass center for the symbol
     * @return the mass center (coordinates with origin at upper left)
     */
    public Point getCentroid ()
    {
        if (centroid == null) {
            final int width = getDimension().width;
            final int height = getDimension().height;
            int[]     argbs = new int[width * height];
            image.getRGB(0, 0, width, height, argbs, 0, width);

            int sw = 0; // Total weight of non transparent points
            int sx = 0; // x moment
            int sy = 0; // y moment

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = argbs[x + (y * width)];
                    int a = (argb & 0xff000000) >>> 24; // Alpha

                    if (a != 0) {
                        int b = (argb & 0x000000ff); // Blue
                        int w = 255 - b; // Darker = heavier
                        sw += w;
                        sx += (x * w);
                        sy += (y * w);
                    }
                }
            }

            centroid = new Point(
                (int) Math.rint((double) sx / sw),
                (int) Math.rint((double) sy / sw));
        }

        return centroid;
    }

    //-----------------------//
    // isDataFlavorSupported //
    //-----------------------//
    @Implement(Transferable.class)
    public boolean isDataFlavorSupported (DataFlavor flavor)
    {
        return flavor == DATA_FLAVOR;
    }

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the normalized bounding dimension of the symbol
     * @return the size of the symbol
     */
    public Dimension getDimension ()
    {
        return dimension;
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the normalized height of the symbol
     *
     * @return the real image height in pixels
     */
    public int getHeight ()
    {
        return dimension.height;
    }

    //---------------//
    // getIconHeight //
    //---------------//
    /**
     * Report the icon height
     *
     * @return the height of the icon image in pixels
     */
    @Implement(Icon.class)
    public int getIconHeight ()
    {
        return iconImage.getHeight();
    }

    //--------------//
    // getIconImage //
    //--------------//
    /**
     * Report the icon image, suitable for icon display
     *
     * @return the image meant for icon display
     */
    public BufferedImage getIconImage ()
    {
        return iconImage;
    }

    //--------------//
    // getIconWidth //
    //--------------//
    /**
     * Report the actual or the STANDARD width of the icon (used by swing when
     * painting), depending on the current value of constant useConstantWidth
     *
     * @return the icon image width in pixels
     */
    @Implement(Icon.class)
    public int getIconWidth ()
    {
        return iconImage.getWidth();
    }

    //--------------//
    // getInterline //
    //--------------//
    /**
     * Report the scale of the underlying image
     * @return the interline reference of the image
     */
    public int getInterline ()
    {
        return interline;
    }

    //---------//
    // setName //
    //---------//
    /**
     * Assign a name to the symbol
     *
     * @param name the related (shape) name
     */
    public void setName (String name)
    {
        this.name = name;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name (generally the shape name) of the symbol
     *
     * @return the symbol name
     */
    public String getName ()
    {
        return name;
    }

    //------------------//
    // getPitchPosition //
    //------------------//
    /**
     * Report the pitch position within the staff lines
     *
     * @return the pitch position
     */
    public Double getPitchPosition ()
    {
        return pitchPosition;
    }

    //-------------//
    // getRefPoint //
    //-------------//
    /**
     * Report the assigned reference point
     *
     * @return the ref point, which may be null
     */
    public Point getRefPoint ()
    {
        return refPoint;
    }

    //---------------//
    // getStemNumber //
    //---------------//
    /**
     * Report the number of stems this entity is connected to
     *
     * @return the number of stems
     */
    public Integer getStemNumber ()
    {
        return stemNumber;
    }

    //-----------------//
    // getTransferData //
    //-----------------//
    @Implement(Transferable.class)
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
    @Implement(Transferable.class)
    public DataFlavor[] getTransferDataFlavors ()
    {
        return new DataFlavor[] { DATA_FLAVOR };
    }

    //--------------------//
    // getUnderlyingImage //
    //--------------------//
    /**
     * Report the underlying image
     *
     * @return the underlying image
     */
    public BufferedImage getUnderlyingImage ()
    {
        return image;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the normalized width of the symbol
     *
     * @return the real image width in pixels
     */
    public int getWidth ()
    {
        return dimension.width;
    }

    //--------------//
    // isWithLedger //
    //--------------//
    /**
     * Is this entity connected to a ledger
     *
     * @return true if there is at least one ledger
     */
    public Boolean isWithLedger ()
    {
        return withLedger;
    }

    //------//
    // draw //
    //------//
    /**
     * Draw this symbol image on the provided graphics environment (which may
     * be scaled) using the topLeft point. We of course use the most suitable
     * image that we have. The image is rendered with {@link
       ppppp     * omr.score.ui.ScoreConstants#INTER_LINE} normalized size.
     * @param g the graphics context
     * @param topLeft the upper left corner of the image, using the coordinate
     * references of the display (PixelPoint for sheet, PixelPoint for score)
     */
    public void draw (Graphics g,
                      Point    topLeft)
    {
        draw(g, topLeft, interline);
    }

    //------//
    // draw //
    //------//
    /**
     * Draw this symbol image on the provided graphics environment (which may
     * be scaled) using the topLeft point. We of course use the most suitable
     * image that we have.
     * @param g the graphics context
     * @param topLeft the upper left corner of the image, using the coordinate
     * references of the display (PixelPoint for sheet, PixelPoint for score)
     * @param contextInterline the scale of the target context
     */
    public void draw (Graphics g,
                      Point    topLeft,
                      int      contextInterline)
    {
        double ratio = (double) contextInterline / interline;
        draw(g, topLeft, ratio, ratio);
    }

    //------//
    // draw //
    //------//
    /**
     * Draw this symbol image on the provided graphics environment (which may
     * be scaled) using the topLeft point. We of course use the most suitable
     * image that we have.
     * @param g the graphics context
     * @param topLeft the upper left corner of the image, using the coordinate
     * references of the display (PixelPoint for sheet, PixelPoint for score)
     * @param xRatio the scaling ratio in abscissa
     * @param yRatio the scaling ratio in ordinate
     */
    public void draw (Graphics g,
                      Point    topLeft,
                      double   xRatio,
                      double   yRatio)
    {
        Graphics2D      g2 = (Graphics2D) g;
        AffineTransform at = g2.getTransform();

        g2.scale(1 / at.getScaleX(), 1 / at.getScaleY());
        g2.drawImage(
            getScaledImage(at.getScaleX() * xRatio, at.getScaleY() * yRatio),
            (int) Math.rint(topLeft.x * at.getScaleX()),
            (int) Math.rint(topLeft.y * at.getScaleY()),
            null);
        g2.scale(at.getScaleX(), at.getScaleY());
    }

    //-----------//
    // paintIcon //
    //-----------//
    /**
     * Implements Icon interface paintIcon() method
     *
     * @param c containing component (???)
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
        g.drawImage(iconImage, x, y, c);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Symbol");
        sb.append(" ")
          .append(name);
        sb.append(" interline:")
          .append(interline);
        sb.append("}");

        return sb.toString();
    }

    //----------------//
    // getScaledImage //
    //----------------//
    private BufferedImage getScaledImage (double xRatio,
                                          double yRatio)
    {
        AffineTransform tx = new AffineTransform();
        tx.setToScale(xRatio, yRatio);

        AffineTransformOp op = new AffineTransformOp(
            tx,
            AffineTransformOp.TYPE_BILINEAR);

        return op.filter(image, null);
    }

    //----------------//
    // setXmlRefPoint //
    //----------------//
    @XmlElement(name = "ref-point")
    private void setXmlRefPoint (PointFacade xp)
    {
        refPoint = xp.getPoint();
    }

    //----------------//
    // getXmlRefPoint //
    //----------------//
    private PointFacade getXmlRefPoint ()
    {
        if (refPoint != null) {
            return new PointFacade(refPoint);
        } else {
            return null;
        }
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled for this
     * object, but before this object is set to the parent object.
     */
    private void afterUnmarshal (Unmarshaller um,
                                 Object       parent)
    {
        // Use a default interline value of 8
        if (interline == null) {
            logger.warning("Symbol " + name + " with no interline. 8 assumed.");
            interline = 8;
        }

        // Color base
        Color base;

        if (baseColor != null) {
            base = new Color(baseColor.R, baseColor.G, baseColor.B);
        } else {
            base = Color.BLACK;
        }

        // Convert string bitmap -> image
        image = SymbolManager.getInstance()
                             .decodeImage(bitmap, base);
        computeData();
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Called immediately before the marshalling of this object begins.
     */
    private void beforeMarshal (Marshaller m)
    {
        // Convert image -> string bitmap
        if (bitmap == null) {
            bitmap = SymbolManager.getInstance()
                                  .encodeImage(image);
        }
    }

    //-------------//
    // computeData //
    //-------------//
    private void computeData ()
    {
        iconImage = getScaledImage(iconRatio, iconRatio);
        dimension = new Dimension(
            (int) Math.rint(image.getWidth()),
            (int) Math.rint(image.getHeight()));

        if (refPoint != null) {
            refPoint.x = (int) Math.rint(refPoint.x);
            refPoint.y = (int) Math.rint(refPoint.y);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // BaseColor //
    //-----------//
    /**
     * Class {@code BaseColor} is used to handle an RGB color definition
     */
    @XmlRootElement(name = "base-color")
    private static class BaseColor
    {
        //~ Instance fields ----------------------------------------------------

        /** Red */
        @XmlAttribute(name = "R")
        public int R;

        /** Green */
        @XmlAttribute(name = "G")
        public int G;

        /** Blue */
        @XmlAttribute(name = "B")
        public int B;

        //~ Constructors -------------------------------------------------------

        /** Creates a new instance of BaseColor */
        public BaseColor ()
        {
        }
    }
}
