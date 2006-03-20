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
import omr.util.Logger;

import java.awt.*;
import java.awt.image.MemoryImageSource;
import java.io.*;
import javax.swing.*;
import java.awt.image.*;
import java.net.URL;

/**
 * Class <code>IconManager</code> manages icons in their loading and storing.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class IconManager
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(IconManager.class);
    private static final Constants constants = new Constants();

    // Singleton
    private static IconManager INSTANCE;

    // Dedicated file extension for our icons
    private static final String FILE_EXTENSION = ".txt";

    // Characters used for encoding bitmaps with 8 levels of gray
    private static char[] charTable = new char[]
    {
        '#', // 0 Black
        '$', // 1
        '*', // 2
        '0', // 3
        'o', // 4
        '+', // 5
        '.', // 6
        '-'  // 7 White
    };

    //~ Instance variables ------------------------------------------------

    //~ Constructors ------------------------------------------------------

    // Singleton
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

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of IconManager
     *
     * @return the IconManager singleton
     */
    public static IconManager getInstance()
    {
        if (INSTANCE == null) {
            INSTANCE = new IconManager();
        }

        return INSTANCE;
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
        final URL iconUrl = Jui.class.getResource(resName);
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
    public Icon loadIcon (String name)
    {
        logger.fine("Trying to load Icon '" + name + "'");

        // First, find out the proper input stream
        InputStream is;

        // We first look into local dir
        try {
            is = new FileInputStream(getIconFile(name));
        } catch (FileNotFoundException fnfe) {
            // If not found, we look into the application jar file
            try {
                is = IconManager.class.getResourceAsStream
                    (Main.getIconsResource() + "/" +
                     name + FILE_EXTENSION);
            } catch (Exception e) {
                logger.warning("Cannot load icon " + name);
                return null;
            }
        }
        if (is == null) {
            logger.fine("No file for icon " + name);
            return null;
        }

        // Then we de-serialize the icon
        Icon icon = loadFromStream(is);

        if (icon == null) {
            logger.warning("Could not load icon '" + name + "'");
        } else {
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
     * Store the textual representation of an icon
     *
     * @param icon the icon to store
     * @param name the icon name
     */
    public void storeIcon (Icon   icon,
                           String name)
    {
        OutputStream os;

        // We store only into the local dir
        try {
            os = new FileOutputStream(getIconFile(name));
        } catch (FileNotFoundException ex) {
            logger.warning("Cannot store icon " + name);
            return;
        }

        // Just serialize into this stream
        if (storeToStream(icon, name, os)) {
            logger.info("Icon '" + name + "' successfully stored");
        } else {
            logger.warning("Could not store icon " + name);
        }
    }

    //~ Methods private ---------------------------------------------------

    //------------//
    // decodeARGB //
    //------------//
    /**
     * Compute the pixel gray level that corresponds to the given char
     *
     * @param c the char
     * @return the corresponding pixel value (ARGB format)
     */
    private int decodeARGB (char c)
    {
        // Check the char
        for (int i = charTable.length -1; i >= 0; i--) {
            if (charTable[i] == c) {
                int level = 3 + i * 36; // Range 3 .. 255 (not too bad)
                if (level == 255) {     // White = background
                    return 0;           // transparent
                } else {
                    return
                        255   << 24 |      // Alpha
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
    private char encodeARGB (int argb)
    {
        int r = (argb & 0x00ff0000) >>> 16;
        int g = (argb & 0x0000ff00) >>> 8;
        int b = (argb & 0x000000ff);
        int index = (int) Math.rint((r+g+b) / 96);

        return charTable[index];
    }

    //-------------//
    // getIconFile //
    //-------------//
    private File getIconFile (String name)
    {
        File folder = Main.getIconsFolder();
        if (!folder.exists()) {
            logger.info("Creating directory " + folder);
            folder.mkdirs();
        }

        return new File(folder, name + FILE_EXTENSION);
    }

    //----------------//
    // loadFromStream //
    //----------------//
    private Icon loadFromStream (InputStream is)
    {
        try {
            // Read the file that contains the symbol graphical definition
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            StreamTokenizer st = new StreamTokenizer(r);

            // Parameterize the Tokenizer
            st.ordinaryChar ('_'); st.wordChars ('_', '_');

            String name;
            int width;
            int height;

            // Parsing
            st.nextToken(); st.nextToken();
            name   = st.sval;
            logger.fine("name='" + name + "'");

            st.nextToken(); st.nextToken();
            width  = (int) st.nval;
            logger.fine("width='" + width + "'");

            st.nextToken(); st.nextToken();
            height = (int) st.nval;
            logger.fine("height='" + height + "'");

            r.readLine();               // Finish current ligne

            int[] pix = new int[width * height];
            int index = 0;
            for (int y = 0; y < height; y++) {
                String line = r.readLine();
                if (logger.isFineEnabled()) {
                    logger.fine("Line='" + line + "'");
                }
                for (int x = 0; x < width; x++) {
                    int argb = decodeARGB(line.charAt(x));
                    pix[index++] = argb;
                }
            }

            // This is the end ...
            r.close();

            // Create the proper image icon
            Toolkit tk = Toolkit.getDefaultToolkit();
            return new SymbolIcon
                (tk.createImage
                 (new MemoryImageSource(width, height, pix, 0, width)));
        } catch (IOException ex) {
            logger.warning("IconManager.loadFromStream : " + ex);
            ex.printStackTrace();
            return null;
        }
    }

    //---------------//
    // storeToStream //
    //---------------//
    private boolean storeToStream (Icon         icon,
                                   String       name,
                                   OutputStream os)
    {
        // Safer
        if (!(icon instanceof SymbolIcon)) {
            logger.warning("Trying to store a non SymbolIcon : " + name);
            return false;
        }

        SymbolIcon symbolIcon = (SymbolIcon) icon;
        BufferedImage image = (BufferedImage) symbolIcon.getImage();
        DataBuffer dataBuffer = image.getData().getDataBuffer();

        // Retrieve proper image width & height values
        final int width = ((SymbolIcon) icon).getActualWidth();
        final int height = image.getHeight();

        PrintWriter out = new PrintWriter(os);

        // Parameters
        out.printf("name %s%nwidth %d%nheight %d%n", name, width, height);

        // Bitmap
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                out.print(encodeARGB(argb));
            }
            out.println();
        }
        out.close();

        return true;
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
