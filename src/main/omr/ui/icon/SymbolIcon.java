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
    public SymbolIcon ()
    {
    }

    //------------//
    // SymbolIcon //
    //------------//
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
    public void setBitmap (String[] rows)
    {
        // Elaborate the image from the string array
        setImage(IconManager.decodeImage (rows));
    }

    //-----------//
    // getBitmap //
    //-----------//
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
    public String getName ()
    {
        return name;
    }

    //---------//
    // setName //
    //---------//
    public void setName (String name)
    {
        this.name = name;
    }
}

