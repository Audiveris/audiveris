//-----------------------------------------------------------------------//
//                                                                       //
//                          S y m b o l I c o n                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui.icon;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.util.Implement;
import omr.util.PointFacade;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>SymbolIcon</code> is an icon, built from a provided image,
 * with consistent width among all defined symbol icons to ease their
 * presentation in menus.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "icon")
public class SymbolIcon
    implements Icon
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** The same width for all such icons (to be improved) */
    private static int standardWidth = -1;

    //~ Instance fields --------------------------------------------------------

    /** Related name */
    @XmlAttribute
    private String name;

    /** Symbol size (which must be consistent with image dimensions) */
    ///@XmlElement
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
    @XmlElement(name = "has-ledger")
    private Boolean hasLedger;

    /** Pitch position within staff lines */
    @XmlElement(name = "pitch-position")
    private Double pitchPosition;

    /** Related image */
    @XmlTransient
    private BufferedImage image;

    /** Wrapping of the image for (un)marshalling */
    //@XmlJavaTypeAdapter(BitmapAdapter.class)
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

    //-----------//
    // getBitmap //
    //-----------//
    /**
     * Report an array of strings that describe the bitmap (meant for JAXB)
     *
     * @return the array of strings for file storing
     */

    //    @XmlElementWrapper(name = "image")
    //    @XmlElement(name = "row")
    public String[] getBitmap ()
    {
        System.out.println("getBitmap called");
        System.out.println("getIconHeight=" + getIconHeight());

        if (getIconHeight() != -1) {
            // Generate the string array from the icon image
            bitmap = IconManager.getInstance()
                                .encodeImage(this);
        }

        return bitmap;
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

    //--------------//
    // setHasLedger //
    //--------------//
    /**
     * Assign the connection to a ledger
     *
     * @param hasLedger true if there is a connected ledger
     */
    public void setHasLedger (boolean hasLedger)
    {
        this.hasLedger = hasLedger;
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

    //-----------//
    // setBitmap //
    //-----------//
    /**
     * Allows to define the bitmap, from an array of strings (meant for JAXB)
     *
     * @param rows the array of strings which describe the bitmap
     */
    public void setBitmap (String[] rows)
    {
        System.out.println("setBitmap called rows.length=" + rows.length);
        // Elaborate the image from the string array
        setImage(IconManager.getInstance().decodeImage(rows));
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
    // setPitchPosition //
    //------------------//
    /**
     * Assign the pitch position within staff lines
     *
     * @param pitchPosition the position relative to the staff lines
     */
    public void setPitchPosition (Double pitchPosition)
    {
        this.pitchPosition = pitchPosition;
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
    // setRefPoint //
    //-------------//
    /**
     * Assign a reference point to the symbol
     *
     * @param refPoint the reference point
     */
    public void setRefPoint (Point refPoint)
    {
        this.refPoint = refPoint;
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
    // setStemNumber //
    //---------------//
    /**
     * Report the number of stems that are connected to this entity
     *
     * @param stemNumber the number of stems
     */
    public void setStemNumber (Integer stemNumber)
    {
        this.stemNumber = stemNumber;
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

    //-----------//
    // hasLedger //
    //-----------//
    /**
     * Is this entity connected to a ledger
     *
     * @return true if there is at least one ledger
     */
    public Boolean hasLedger ()
    {
        return hasLedger;
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
        setRefPoint(xp.getPoint());
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
        ///System.out.println("afterUnmarshal");
        // Convert string bitmap -> image
        if (image == null) {
            image = IconManager.getInstance()
                               .decodeImage(bitmap);
        }
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Called immediately before the marshalling of this object begins..
     */
    private void beforeMarshal (Marshaller m)
    {
        System.out.println("beforeMarshal");

        // Dimension
        if (dimension == null) {
            getDimension();
        }

        // Centroid
        if (centroid == null) {
            getCentroid();
        }

        // Convert image -> string bitmap
        if (bitmap == null) {
            bitmap = IconManager.getInstance()
                                .encodeImage(image);
        }

        omr.util.Dumper.dump(this);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        Constant.Boolean useConstantWidth = new Constant.Boolean(
            false,
            "Should all music icons use the same width in menus ?");
    }
}
