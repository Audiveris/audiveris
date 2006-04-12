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

import java.awt.*;
import javax.swing.*;
import java.awt.image.BufferedImage;

/**
 * Class <code>SymbolIcon</code> is an icon, built from a provided image,
 * with consistent width among all defined symbol icons to ease their
 * presentation in menus.
 */
public class SymbolIcon
    extends ImageIcon
{
    //~ Static variables/initializers -------------------------------------

    // The same width for all such icons (to be improved)
    private static int standardWidth = 0;

    //~ Instance variables ------------------------------------------------

    // Related name
    private String name;

    // Symbol size (which must be consistent with image dimensions)
    private Dimension dimension;

    // Mass center
    private Point centroid;

    //~ Constructors ------------------------------------------------------

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
    public SymbolIcon (Image image)
    {
        setImage(image);
    }

    //~ Methods -----------------------------------------------------------

    //----------------//
    // getActualWidth //
    //----------------//
    /**
     * Report the ACTUAL width of the icon (used when storing the icon)
     *
     * @return the real icon width in pixels
     */
    public int getActualWidth()
    {
        if (getImage() != null) {
            return getImage().getWidth(null);
        } else {
            return 0;
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
    @Override
    public int getIconWidth()
    {
        return standardWidth;
    }

    //----------//
    // setImage //
    //----------//
    /**
     * Overridden to allow computation of largest width
     *
     * @param image the icon image
     */
    @Override
    public void setImage(Image image)
    {
        super.setImage(image);

        // Gradually update the common standard width
        if (getActualWidth() > getIconWidth()) {
            setStandardWidth(getActualWidth());
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
            BufferedImage bi = IconManager.toBufferedImage(getImage());
            final int width  = getDimension().width;
            final int height = getDimension().height;
            int[] argbs = new int[width * height];
            bi.getRGB(0,0,width,height,argbs,0,width);

            int sw = 0;           // Total weight of non transparent points
            int sx = 0;           // x moment
            int sy = 0;           // y moment
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = argbs[x + y*width];
                    int a = (argb & 0xff000000) >>> 24; // Alpha
                    if (a != 0) {
                        int b = (argb & 0x000000ff);    // Blue
                        int w = 255 - b;                // Darker = heavier
                        sw += w;
                        sx += x * w;
                        sy += y * w;
                    }
                }
            }

            centroid = new Point((int) Math.rint((double) sx/sw),
                                 (int) Math.rint((double) sy/sw));
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
            if (getImage() != null &&
                getImage().getWidth(null) != 0 &&
                getImage().getHeight(null) != 0 ) {
                dimension = new Dimension(getImage().getWidth(null),
                                          getImage().getHeight(null));
            }
        }

        return dimension;
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
        setImage(IconManager.decodeImage (rows));
    }

    //-----------//
    // getBitmap //
    //-----------//
    /**
     * Report an array of strings that describe the bitmap
     *
     * @return the array of strings for file storing
     */
    public String[] getBitmap()
    {
        if (getIconHeight() == -1) {
            return null;
        }
        // Generate the string array from the icon image
        return IconManager.encodeImage (this);
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
}

