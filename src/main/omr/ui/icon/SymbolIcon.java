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

import omr.util.Implement;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

/**
 * Class <code>SymbolIcon</code> is an icon, built from a provided image,
 * with consistent width among all defined symbol icons to ease their
 * presentation in menus.
 *
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SymbolIcon
    implements Icon
{
    //~ Static fields/initializers ---------------------------------------------

    /** The same width for all such icons (to be improved) */
    private static int    standardWidth = -1;

    //~ Instance fields --------------------------------------------------------

    /** Connected to Ledger ? */
    private Boolean       hasLedger;

    /** Related image */
    private BufferedImage image;

    /** Symbol size (which must be consistent with image dimensions) */
    private Dimension dimension;

    /** Pitch position within staff lines */
    private Double  pitchPosition;

    /** How many stems is it connected to ? */
    private Integer stemNumber;

    /** Mass center */
    private Point  centroid;

    /** Reference point, if any */
    private Point  refPoint;

    /** Related name */
    private String name;

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
     * Report an array of strings that describe the bitmap
     *
     * @return the array of strings for file storing
     */
    public String[] getBitmap ()
    {
        if (getIconHeight() == -1) {
            return null;
        }

        // Generate the string array from the icon image
        return IconManager.encodeImage(this);
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
     * Report the STANDARD width of the icon (used by swing when painting)
     *
     * @return the standard width in pixels
     */
    @Implement(Icon.class)
    public int getIconWidth ()
    {
        return standardWidth;
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
     * Allows to define the bitmap, from an array of strings
     *
     * @param rows the array of strings which describe the bitmap
     */
    public void setBitmap (String[] rows)
    {
        // Elaborate the image from the string array
        setImage(IconManager.decodeImage(rows));
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
    public void setStemNumber (int stemNumber)
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
    public int getStemNumber ()
    {
        return stemNumber;
    }

    //-----------//
    // hasLedger //
    //-----------//
    /**
     * Is this entity connected to a ledger
     *
     * @return true if ther is at least one ledger
     */
    public boolean hasLedger ()
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
}
