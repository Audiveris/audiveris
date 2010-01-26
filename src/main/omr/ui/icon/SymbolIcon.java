//----------------------------------------------------------------------------//
//                                                                            //
//                            S y m b o l I c o n                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.icon;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.util.Implement;
import omr.util.PointFacade;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

/**
 * Class <code>SymbolIcon</code> is an icon, built from a provided image,
 * with consistent width among all defined symbol icons to ease their
 * presentation in menus.
 *
 * <p>A SymbolIcon may also be used to train the glyph evaluator when we don't
 * have enough "real" glyphs available.
 *
 * <p>A SymbolIcon is also used to convey information on the related shape and
 * especially the reference point of that shape. Most of shapes have no
 * reference point, and thus we use their area center, which is the center of
 * their bounding box. However, a few shapes (clefs to precisely position them
 * on the staff, head/flags combos to handle the precise position of the head
 * part) need a very precise reference center (actually the y ordinate) which is
 * somewhat different from the area center. This is the difference between the
 * {@link #getAreaCenter} and the {@link #getCenter} methods.
 *
 * @author Herv&eacute; Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "", propOrder =  {
    "baseColor", "xmlRefPoint", "stemNumber", "withLedger", "pitchPosition", "bitmap"}
)
@XmlRootElement(name = "icon")
public class SymbolIcon
    implements Icon
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SymbolIcon.class);

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** The same width for all such icons (to be improved) */
    private static int standardWidth = -1;

    //~ Instance fields --------------------------------------------------------

    /** Related name */
    @XmlAttribute
    private String name;

    /** Base color */
    @XmlElement(name = "base-color")
    private BaseColor baseColor;

    /** Symbol size (which must be consistent with image dimensions) */
    private Dimension dimension;

    /** Mass center */
    private Point centroid;

    /** Reference point, if any. (Un)Marshalling is done through
       getXmlRefPoint */
    private Point refPoint;

    /** How many stems is it connected to ? */
    @XmlElement(name = "stem-number")
    private Integer stemNumber;

    /** Connected to Ledger ? */
    @XmlElement(name = "with-ledger")
    private Boolean withLedger;

    /** Pitch position within staff lines */
    @XmlElement(name = "pitch-position")
    private Double pitchPosition;

    /** Related image */
    @XmlTransient
    private BufferedImage image;

    /** Wrapping of the image for (un)marshalling */
    @XmlElementWrapper(name = "bitmap")
    @XmlElement(name = "row")
    public String[] bitmap;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SymbolIcon //
    //------------//
    /**
     * No-arg constructor for the XML mapper
     */
    public SymbolIcon ()
    {
    }

    //------------//
    // SymbolIcon //
    //------------//
    /**
     * Create a symbol icon with the provided image
     *
     * @param image the icon image
     */
    public SymbolIcon (BufferedImage image)
    {
        setImage(image);
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getActualWidth //
    //----------------//
    /**
     * Report the ACTUAL width of the icon (used when storing the icon)
     *
     * @return the real icon width in pixels
     */
    public int getActualWidth ()
    {
        if (getImage() != null) {
            return getImage()
                       .getWidth();
        } else {
            return 0;
        }
    }

    //---------------//
    // getAreaCenter //
    //---------------//
    /**
     * Report the area center for the symbol
     * @return the area center (coordinates with origin at upper left)
     */
    public Point getAreaCenter ()
    {
        return new Point(getDimension().width / 2, getDimension().height / 2);
    }

    //--------------//
    // getBaseColor //
    //--------------//
    public BaseColor getBaseColor ()
    {
        return baseColor;
    }

    //-----------//
    // getBitmap //
    //-----------//
    /**
     * Report an array of strings that describe the bitmap (meant for JAXB)
     *
     * @return the array of strings for file storing
     */
    public String[] getBitmap ()
    {
        if (getIconHeight() != -1) {
            // Generate the string array from the icon image
            bitmap = IconManager.getInstance()
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
            if (getImage() == null) {
                return null;
            }

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

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the bounding dimension of the symbol
     * @return the size of the symbol
     */
    public Dimension getDimension ()
    {
        if (dimension == null) {
            if ((image != null) &&
                (image.getWidth() != 0) &&
                (image.getHeight() != 0)) {
                dimension = new Dimension(image.getWidth(), image.getHeight());
            }
        }

        return dimension;
    }

    //---------------//
    // getIconHeight //
    //---------------//
    /**
     * Report the icon height
     *
     * @return the height of the underlying image
     */
    @Implement(Icon.class)
    public int getIconHeight ()
    {
        if (getImage() != null) {
            return getImage()
                       .getHeight();
        } else {
            return -1;
        }
    }

    //--------------//
    // getIconWidth //
    //--------------//
    /**
     * Report the actual or the STANDARD width of the icon (used by swing when
     * painting), depending on the current value of constant useConstantWidth
     *
     * @return the standard width in pixels
     */
    @Implement(Icon.class)
    public int getIconWidth ()
    {
        if (constants.useConstantWidth.getValue()) {
            return standardWidth;
        } else {
            return getActualWidth();
        }
    }

    //----------//
    // setImage //
    //----------//
    /**
     * Assign the image
     *
     * @param image the icon image
     */
    public void setImage (BufferedImage image)
    {
        this.image = image;

        // Invalidate cached data
        dimension = null;
        centroid = null;

        // Gradually update the common standard width
        if (getActualWidth() > getIconWidth()) {
            setStandardWidth(getActualWidth());
        }
    }

    //----------//
    // getImage //
    //----------//
    /**
     * Report the underlying image
     *
     * @return the underlying image
     */
    public BufferedImage getImage ()
    {
        return image;
    }

    //------------------//
    // setStandardWidth //
    //------------------//
    /**
     * Define the STANDARD width for all icons
     *
     * @param standardWidth the standard width in pixels for all such icons
     */
    public static void setStandardWidth (int standardWidth)
    {
        SymbolIcon.standardWidth = standardWidth;
    }

    //
    //    //-----------//
    //    // setBitmap //
    //    //-----------//
    //    /**
    //     * Allows to define the bitmap, from an array of strings (meant for JAXB)
    //     *
    //     * @param rows the array of strings which describe the bitmap
    //     */
    //    public void setBitmap (String[] rows)
    //    {
    //        // Elaborate the image from the string array
    //        setImage(IconManager.getInstance().decodeImage(rows));
    //    }

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
        g.drawImage(image, x, y, c);
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
        // Convert string bitmap -> image
        if (image == null) {
            Color base;

            if (baseColor != null) {
                base = new Color(baseColor.R, baseColor.G, baseColor.B);
            } else {
                base = Color.BLACK;
            }

            image = IconManager.getInstance()
                               .decodeImage(bitmap, base);
        }
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
            bitmap = IconManager.getInstance()
                                .encodeImage(image);
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

        Constant.Boolean useConstantWidth = new Constant.Boolean(
            false,
            "Should all music icons use the same width in menus ?");
    }
}
