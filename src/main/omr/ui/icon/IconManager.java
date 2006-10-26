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

import omr.util.Logger;
import omr.util.XmlMapper;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import javax.xml.bind.*;

/**
 * Class <code>IconManager</code> manages icons in their loading and storing. It
 * thus handles the location where icon definitions are kept, as well as (if
 * needed) the marshalling and unmarshalling to and from these definitions.
 *
 * <p>There are two populations of icons:<ul>
 *
 * <li>The predefined image icons used for user interface buttons in various
 * toolbars. These icons come from the <b>Java Look and Feel Graphics
 * Repository</b> and are kept in binary (.gif) form in the <b>jlfgr-1_0.jar</b>
 * archive. There is no custom (un)marshalling per se. They are instances of
 * standard class {@link ImageIcon}. Access to these image icons is provided
 * via {@link #loadImageIcon}.
 *
 * <li>The custom symbol icons which represent music symbols. Purpose of these
 * icons is twofold: they are used in menus and other UI artifacts which need
 * such music icons, they are used also to build <i>artificial glyphs</i>,
 * instances of the {@link omr.glyph.IconGlyph} class, which can be used to
 * train the evaluator when no other real glyph is available for a given
 * shape. These symbol icons use a custom (un)marshalling technique (using JAXB)
 * to ASCII descriptions that can be edited manually. They are loaded as
 * instances of specific class {@link SymbolIcon}. Access to these symbol icons
 * is provided via {@link #loadSymbolIcon} and {@link #storeSymbolIcon}. This
 * symbol population is cached in a dedicated map, to speed up subsequent access
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class IconManager
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants        constants = new Constants();
    private static final Logger           logger = Logger.getLogger(
        IconManager.class);

    /** Mapper for XML persistency */
    private static XmlMapper xmlMapper;

    /** Dedicated file extension for our symbol icon files */
    private static final String FILE_EXTENSION = ".xml";

    /**
     * Characters used for encoding bitmaps with 8 levels of gray (this is
     * sufficient for our symbol display)
     */
    private static final char WHITE = '-'; // And transparent
    private static final char[]           charTable = new char[] {
                                                          '#', // 0 Black
    '$', // 1
    '*', // 2
    '0', // 3
    'o', // 4
    '+', // 5
    '.', // 6
    WHITE // 7
                                                      };

    /** The single class instance */
    private static IconManager INSTANCE;

    /** Un/marshalling context for use with JAXB */
    private static JAXBContext jaxbContext;

    //~ Instance fields --------------------------------------------------------

    /** Map for symbol icons */
    private final Map<String, SymbolIcon> symbolIcons = new HashMap<String, SymbolIcon>();

    //~ Constructors -----------------------------------------------------------

    /** Not meant to be instantiated */
    private IconManager ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this singleton class
     *
     * @return the icon manager
     */
    public static IconManager getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new IconManager();
        }

        return INSTANCE;
    }

    //-------------------//
    // loadFromXmlStream //
    //-------------------//
    /**
     * Load an icon description from an XML stream.
     * This private method is declared package private to allow its unitary test
     *
     * @param is the input stream
     *
     * @return a new SymbolIcon, or null if loading has failed
     */
    public SymbolIcon loadFromXmlStream (InputStream is)
    {
        try {
            return (SymbolIcon) jaxbUnmarshal(is);
        } catch (Exception ex) {
            ex.printStackTrace();

            // User already notified
            return null;
        }
    }

    //---------------//
    // loadImageIcon //
    //---------------//
    /**
     * Load an icon from the toolbarButtonGraphics resource.
     *
     * @param fname name of the icon, using format "category/Name". For example
     *              loadImageIcon("general/Find") will return the "Find icon"
     *              located in the "general" category of the Java Look and Feel
     *              Graphics Repository
     * @return the newly built icon, or null if load failure
     */
    public Icon loadImageIcon (String fname)
    {
        final String resName = "/toolbarButtonGraphics/" + fname +
                               constants.buttonIconSize.getValue() + ".gif";
        final URL    iconUrl = IconManager.class.getResource(resName);

        if (iconUrl == null) {
            logger.warning("Could not load icon from " + resName);

            return null;
        } else {
            return new ImageIcon(iconUrl);
        }
    }

    //----------------//
    // loadSymbolIcon //
    //----------------//
    /**
     * Load a symbol icon from its textual representation in the Audiveris music
     * icons resource
     *
     * @param name the icon name, using formats such as "HALF_REST.02" or
     *             "HALF_REST"
     * @return the icon built, or null if failed
     */
    public SymbolIcon loadSymbolIcon (String name)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Asking for SymbolIcon '" + name + "'");
        }

        // Do we have a loaded instance yet?
        SymbolIcon icon = symbolIcons.get(name);

        if (icon == null) {
            // Look for description file
            String resName = "/icons/" + name + FILE_EXTENSION;

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Trying to load Icon '" + name + "' from resource " +
                    resName);
            }

            InputStream is = Main.class.getResourceAsStream(resName);

            if (is != null) {
                // Then we de-serialize the icon description
                icon = loadFromXmlStream(is);

                try {
                    is.close();
                } catch (IOException ignored) {
                }

                if (icon == null) {
                    logger.warning("Could not load icon '" + name + "'");
                } else {
                    if (logger.isFineEnabled()) {
                        logger.fine("Icon '" + name + "' loaded");
                    }

                    // Cache the icon for future reuse
                    symbolIcons.put(name, icon);
                }
            } else {
                if (logger.isFineEnabled()) {
                    logger.fine("No resource file for icon " + name);
                }
            }
        }

        return icon;
    }

    //-----------------//
    // storeSymbolIcon //
    //-----------------//
    /**
     * Store the textual representation of a symbol icon, using the internal
     * icon name
     *
     * @param icon the icon to store
     */
    public void storeSymbolIcon (SymbolIcon icon)
    {
        String name = icon.getName();

        if ((name == null) || name.equals("")) {
            logger.warning("Cannot store icon with no name defined");
        } else {
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
    }

    //------------------//
    // storeToXmlStream //
    //------------------//
    /**
     * Store an icon description to an XML stream.
     * This private method is declared package private to allow its unitary test
     *
     * @param icon the icon to store
     * @param os the output stream
     *
     * @return true if successful, false otherwise
     */
    public boolean storeToXmlStream (SymbolIcon   icon,
                                     OutputStream os)
    {
        try {
            jaxbMarshal(icon, os);

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();

            return false;
        }
    }

    //-------------//
    // decodeImage //
    //-------------//
    /**
     * Build an image out of an array of strings. This is meant to ease JAXB
     * unmarshalling.
     *
     * @param rows the lines of characters
     * @return the decoded image
     */
    BufferedImage decodeImage (String[] rows)
    {
        // Create the DataBuffer to hold the pixel samples
        final int  width = rows[0].length();
        final int  height = rows.length;

        // Create Raster
        Raster     raster = Raster.createPackedRaster(
            DataBuffer.TYPE_INT,
            width,
            height,
            new int[] { 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000 }, // bandMasks RGBA
            null);

        // Populate the data buffer
        DataBuffer dataBuffer = raster.getDataBuffer();
        int        index = 0;

        for (String row : rows) {
            for (int x = 0; x < width; x++) {
                dataBuffer.setElem(index++, toARGB(row.charAt(x)));
            }
        }

        // Create the image
        BufferedImage bufferedImage = new BufferedImage(
            width,
            height,
            BufferedImage.TYPE_INT_ARGB);
        bufferedImage.setData(raster);

        return bufferedImage;
    }

    //-------------//
    // encodeImage //
    //-------------//
    /**
     * Build an array of strings from a given image. This is meant to ease JAXB
     * marshalling.
     *
     * @param icon the icon, whose image is to be used
     * @return the array of strings
     */
    String[] encodeImage (SymbolIcon icon)
    {
        return encodeImage(icon.getImage());
    }

    //-------------//
    // encodeImage //
    //-------------//
    /**
     * Build an array of strings from a given image. This is meant to ease JAXB
     * marshalling.
     *
     * @param image image to be used
     * @return the array of strings
     */
    String[] encodeImage (BufferedImage image)
    {
        // Retrieve proper image width & height values
        final int     width = image.getWidth();
        final int     height = image.getHeight();
        String[]      rows = new String[height];

        StringBuilder sb = new StringBuilder();

        // Bitmap
        int[] argbs = new int[width * height];
        image.getRGB(0, 0, width, height, argbs, 0, width);

        for (int y = 0; y < height; y++) {
            sb.delete(0, sb.length());

            for (int x = 0; x < width; x++) {
                int argb = argbs[x + (y * width)];
                sb.append(ARGBtoChar(argb));
            }

            rows[y] = sb.toString();
        }

        return rows;
    }

    //---------------------//
    // getIconOutputStream //
    //---------------------//
    private OutputStream getIconOutputStream (String name)
        throws FileNotFoundException
    {
        File folder = Main.getIconsFolder();

        if (!folder.exists()) {
            logger.info("Creating directory " + folder);
            folder.mkdirs();
        }

        File file = new File(folder, name + FILE_EXTENSION);

        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException ex) {
            logger.warning("Cannot open output stream to icon " + file);
            throw ex;
        }
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private JAXBContext getJaxbContext ()
        throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(SymbolIcon.class);
        }

        return jaxbContext;
    }

    //--------------//
    // getXmlMapper //
    //--------------//
    private XmlMapper getXmlMapper ()
    {
        if (xmlMapper == null) {
            xmlMapper = new XmlMapper(SymbolIcon.class);
        }

        return xmlMapper;
    }

    //------------//
    // ARGBtoChar //
    //------------//
    /**
     * Encode a pixel value using a table of 8 different chars for
     * different gray levels
     *
     * @param argb the pixel value, in the ARGB format
     * @return the proper char
     */
    private char ARGBtoChar (int argb)
    {
        int a = (argb & 0xff000000) >>> 24; // Alpha

        if (a == 0) {
            return WHITE; // White / transparent
        } else {
            int r = (argb & 0x00ff0000) >>> 16;
            int g = (argb & 0x0000ff00) >>> 8;
            int b = (argb & 0x000000ff);
            int index = (int) Math.rint((r + g + b) / 108.0); // 3 * 36

            return charTable[index];
        }
    }

    //-------------//
    // jaxbMarshal //
    //-------------//
    private void jaxbMarshal (SymbolIcon   icon,
                              OutputStream os)
        throws JAXBException
    {
        Marshaller m = getJaxbContext()
                           .createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(icon, os);
    }

    //---------------//
    // jaxbUnmarshal //
    //---------------//
    private Object jaxbUnmarshal (InputStream is)
        throws JAXBException
    {
        Unmarshaller um = getJaxbContext()
                           .createUnmarshaller();

        return um.unmarshal(is);
    }

    //--------//
    // toARGB //
    //--------//
    /**
     * Compute the ARGB pixel that corresponds to the given char
     *
     * @param c the char
     * @return the corresponding pixel value (ARGB format)
     */
    private int toARGB (char c)
    {
        final int level = toGray(c);

        if (level == 255) {
            return 0x00ffffff; // Totally transparent / white
        } else {
            return (255 << 24) | // Alpha (opaque)
                   (level << 16) | // R
                   (level << 8) | // G
                   level; // B
        }
    }

    //--------//
    // toGray //
    //--------//
    /**
     * Compute the pixel gray level that corresponds to the given char
     *
     * @param c the char
     * @return the corresponding pixel value ( 0 .. 255)
     */
    private int toGray (char c)
    {
        // Check the char
        if (c == WHITE) {
            return 255;
        } else {
            for (int i = charTable.length - 1; i >= 0; i--) {
                if (charTable[i] == c) {
                    int level = 3 + (i * 36); // Range 3 .. 255 (not too bad)

                    return level;
                }
            }
        }

        // Unknown -> white
        logger.warning("Invalid pixel encoding char : '" + c + "'");

        return 255;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        Constant.Integer buttonIconSize = new Constant.Integer(
            16,
            "Size of toolbar icons (16 or 24)");
    }
}
