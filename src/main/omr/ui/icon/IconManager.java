//-----------------------------------------------------------------------//
//                                                                       //
//                         I c o n M a n a g e r                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui.icon;

import omr.Main;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.ui.*;
import omr.util.Dumper;
import omr.util.Logger;
import omr.util.XmlMapper;

import java.awt.*;
import java.awt.image.*;
import java.awt.image.MemoryImageSource;
import java.io.*;
import java.net.URL;
import javax.swing.*;

/**
 * Class <code>IconManager</code> manages icons in their loading and
 * storing. It thus handles the location where icon definition files are
 * kept, as well as the marshalling and unmarshalling to and from these
 * files. The binding is implemented by means of JiBX.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class IconManager
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(IconManager.class);
    private static final Constants constants = new Constants();

    // mapper for XML persistency
    private static XmlMapper xmlMapper;

    // Dedicated file extension for our icon files
    private static final String FILE_EXTENSION = ".xml";

    // Characters used for encoding bitmaps with 8 levels of gray (this is
    // sufficient for our symbol display)
    private static char WHITE = '-';    // And transparent
    private static char[] charTable = new char[]
    {
        '#',  // 0 Black
        '$',  // 1
        '*',  // 2
        '0',  // 3
        'o',  // 4
        '+',  // 5
        '.',  // 6
        WHITE // 7
    };

    //~ Instance variables ------------------------------------------------

    //~ Constructors ------------------------------------------------------

    // Not meant to be instantiated
    private IconManager()
    {
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // buttonIconOf //
    //--------------//
    /**
     * Build an icon, searched in the button directory.
     *
     * @param fname name of the icon
     *
     * @return the newly built icon
     */
    public static Icon buttonIconOf (String fname)
    {
        return iconOf("/toolbarButtonGraphics",
                      fname + constants.buttonIconSize.getValue());
    }

    //--------//
    // iconOf //
    //--------//
    /**
     * Build an icon, given its name and size.
     *
     * @param path the directory path where the image is to be found
     * @param fname name of the icon
     *
     * @return the newly built icon
     */
    public static Icon iconOf (String path,
                               String fname)
    {
        final String resName = path + "/" + fname + ".gif";
        final URL iconUrl = IconManager.class.getResource(resName);
        if (iconUrl == null) {
            logger.warning("iconOf. Could not load icon from " + resName);

            return null;
        }

        return new ImageIcon(iconUrl);
    }

    //----------//
    // loadIcon //
    //----------//
    /**
     * Load an icon from its textual representation
     *
     * @param name the icon name
     * @return the icon built, or null if failed
     */
    public static SymbolIcon loadIcon (String name)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Trying to load Icon '" + name + "'");
        }


        InputStream is = Main.class.getResourceAsStream
            ("/icons/" + name + FILE_EXTENSION);

        if (is == null) {
            if (logger.isFineEnabled()) {
                logger.fine("No file for icon " + name);
            }
            return null;
        }

        // Then we de-serialize the icon
        SymbolIcon icon = loadFromXmlStream(is);
        try {
            is.close();
        } catch (IOException ignored) {
        }

        if (icon == null) {
            logger.warning("Could not load icon '" + name + "'");
        } else {
            icon.setName(name);
            if (logger.isFineEnabled()) {
                logger.fine("Icon '" + name + "' loaded");
            }
        }

        return icon;
    }

    //-----------//
    // storeIcon //
    //-----------//
    /**
     * Store the textual representation of an icon, using the internal icon
     * name
     *
     * @param icon the icon to store
     */
    public static void storeIcon (SymbolIcon icon)
    {
        storeIcon(icon, icon.getName());
    }

    //-----------//
    // storeIcon //
    //-----------//
    /**
     * Store the textual representation of an icon
     *
     * @param icon the icon to store
     * @param name the icon name
     */
    public static void storeIcon (SymbolIcon icon,
                                  String     name)
    {
        OutputStream os;

        // We store only into the local dir
        try {
            os = getIconOutputStream(name);
        } catch (FileNotFoundException ex) {
            logger.warning("Cannot store icon " + name);
            return;
        }

        // Just serialize into this stream
        if (storeToXmlStream(icon, os)) {
            logger.info("Icon '" + name + "' successfully stored");
        } else {
            logger.warning("Could not store icon " + name);
        }

        try {
            os.close();
        } catch (IOException ignored) {
        }
    }

    //-------------//
    // encodeImage //
    //-------------//
    /**
     * Build an array of strings from a given image. This is meant for JiBX
     * marshalling.
     *
     * @param icon the icon, whose image is to be used
     * @return the array of strings
     */
    public static String[] encodeImage (SymbolIcon icon)
    {
        BufferedImage image = toBufferedImage(icon.getImage());

        // Retrieve proper image width & height values
        final int width  = image.getWidth();
        final int height = image.getHeight();
        String[] rows = new String[height];

        StringBuilder sb = new StringBuilder();

        // Bitmap
        int[] argbs = new int[width * height];
        image.getRGB(0,0,width,height,argbs,0,width);

        for (int y = 0; y < height; y++) {
            sb.delete(0, sb.length());
            for (int x = 0; x < width; x++) {
                int argb = argbs[x + y*width];
                sb.append(encodeARGB(argb));
            }
            rows[y] = sb.toString();
        }

        return rows;
    }

    //-------------//
    // decodeImage //
    //-------------//
    /**
     * Build an image out of an array of strings. This is meant for JiBX
     * unmarshalling.
     *
     * @param rows the lines of characters
     * @return the decoded image
     */
    public static Image decodeImage (String[] rows)
    {
        final int width = rows[0].length();
        final int height = rows.length;
        int[] pix = new int[width * height];
        int index = 0;
        for (String row : rows) {
            if (logger.isFineEnabled ()) {
                logger.finest ("Row='" + row + "'");
            }
            for (int x = 0; x < width; x++) {
                pix[index++] = decodeARGB (row.charAt (x));
            }
        }

        // Create the proper image icon
        Toolkit tk = Toolkit.getDefaultToolkit ();
        return tk.createImage
                (new MemoryImageSource (width, height, pix, 0, width));
    }

    //-----------------//
    // toBufferedImage //
    //-----------------//
    /**
     * Make sure we have a BufferedImage
     *
     * @param image the Image
     * @return a true BufferedImage
     */
    public static BufferedImage toBufferedImage (Image image)
    {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        } else {
            // Make sure image is fully loaded
            image = new ImageIcon(image).getImage();

            // Generate new image
            BufferedImage bufferedImage
                = new BufferedImage(image.getWidth(null),
                                    image.getHeight(null),
                                    BufferedImage.TYPE_INT_ARGB);
            Graphics g = bufferedImage.createGraphics();
            g.drawImage(image,0,0,null);
            g.dispose();

            return bufferedImage;
        }
    }

    //~ Methods private ---------------------------------------------------

    //--------------//
    // getXmlMapper //
    //--------------//
    private static XmlMapper getXmlMapper()
    {
        if (xmlMapper == null) {
            xmlMapper = new XmlMapper(SymbolIcon.class);
        }
        return xmlMapper;
    }

    //------------//
    // decodeARGB //
    //------------//
    /**
     * Compute the pixel gray level that corresponds to the given char
     *
     * @param c the char
     * @return the corresponding pixel value (ARGB format)
     */
    private static int decodeARGB (char c)
    {
        // Check the char
        if (c == WHITE) {
            return 0;
        } else {
            for (int i = charTable.length -1; i >= 0; i--) {
                if (charTable[i] == c) {
                    int level = 3 + i * 36; // Range 3 .. 255 (not too bad)
                    return
                        255   << 24 |      // Alpha (opaque)
                        level << 16 |      // R
                        level <<  8 |      // G
                        level;             // B
                }
            }
        }

        logger.warning("Invalid pixel encoding char : '" + c + "'");
        return 0;
    }

    //------------//
    // encodeARGB //
    //------------//
    /**
     * Encode a pixel value using a table of 8 different chars for
     * different gray levels
     *
     * @param argb the pixel value, in the ARGB format
     * @return the proper char
     */
    private static char encodeARGB (int argb)
    {
        int a = (argb & 0xff000000) >>> 24; // Alpha
        if (a == 0) {
            return WHITE;               // White / transparent
        } else {
            int r = (argb & 0x00ff0000) >>> 16;
            int g = (argb & 0x0000ff00) >>> 8;
            int b = (argb & 0x000000ff);
            int index = (int) Math.rint((r+g+b) / 108.0); // 3 * 36

            return charTable[index];
        }
    }

    //---------------------//
    // getIconOutputStream //
    //---------------------//
    private static OutputStream getIconOutputStream (String name)
        throws FileNotFoundException
    {
        File folder = Main.getIconsFolder();
        if (!folder.exists()) {
            logger.info("Creating directory " + folder);
            folder.mkdirs();
        }

        File file = new File (folder, name + FILE_EXTENSION);
        try {
            return new FileOutputStream (file);
        } catch (FileNotFoundException ex) {
            logger.warning("Cannot open output stream to icon " + file);
            throw ex;
        }
    }

    //-------------------//
    // loadFromXmlStream //
    //-------------------//
    /// Turn it back to private !!!
    public static SymbolIcon loadFromXmlStream (InputStream is)
    {
        try {
            return (SymbolIcon) getXmlMapper().load(is);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    //------------------//
    // storeToXmlStream //
    //------------------//
    /// Turn it back to private !!!
    public static boolean storeToXmlStream (SymbolIcon   icon,
                                            OutputStream os)
    {
        try {
            getXmlMapper().store(icon, os);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    //~ Classes -----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        Constant.Integer buttonIconSize = new Constant.Integer
                (16,
                 "Size of toolbar icons (16 or 24)");

        Constants ()
        {
            initialize();
        }
    }
}
