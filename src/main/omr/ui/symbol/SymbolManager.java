//----------------------------------------------------------------------------//
//                                                                            //
//                         S y m b o l M a n a g e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.WellKnowns;

import omr.log.Logger;

import omr.util.ClassUtil;

import java.awt.Color;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.xml.bind.*;

/**
 * Class {@code SymbolManager} manages {@link ShapeSymbol} instances in their
 * loading and storing. It thus handles the location where symbol definitions
 * are kept, as well as the marshalling and unmarshalling to and from these
 * definitions.
 *
 * <p>The custom symbol represent music symbols. Purpose of these
 * symbols is twofold: <ol>
 *
 * <li>They provide icons to be used in menus and other UI
 * artifacts which need such music icons</li>
 *
 * <li>They are used also to build <i>artificial glyphs</i>, instances of the
 * {@link omr.glyph.IconGlyph} class, which can be used to train the evaluator
 * when no other real glyph is available for a given shape.</li></p>
 *
 * <p>These symbols use a custom (un)marshalling technique (using JAXB)
 * to ASCII descriptions that can be edited manually. Access to these symbol
 * is provided via {@link #loadSymbol} and {@link #storeSymbol}. This
 * symbol population is cached in a dedicated map, to speed up subsequent
 * accesses</p>
 *
 * @author Hervé Bitteur
 */
public class SymbolManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SymbolManager.class);

    /** Dedicated file extension for our symbol definition files */
    private static final String FILE_EXTENSION = ".xml";

    /** Specila character for transparent */
    private static final char TRANSPARENT = '-';

    /**
     * Characters used for encoding bitmaps with 8 levels of gray (this is
     * sufficient for our symbol display)
     */
    private static final char[] charTable = new char[] {
                                                '#', // 0 Black
    '$', // 1
    '*', // 2
    '0', // 3
    'o', // 4
    '+', // 5
    '.', // 6
    TRANSPARENT // 7
                                            };

    /** The single class instance */
    private static SymbolManager INSTANCE;

    /** Un/marshalling context for use with JAXB */
    private static JAXBContext jaxbContext;

    //~ Instance fields --------------------------------------------------------

    /** Map of loaded symbols */
    private final Map<String, ShapeSymbol> symbols = new HashMap<String, ShapeSymbol>();

    //~ Constructors -----------------------------------------------------------

    /** Not meant to be instantiated */
    private SymbolManager ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this singleton class
     *
     * @return the SymbolManager instance
     */
    public static SymbolManager getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new SymbolManager();
        }

        return INSTANCE;
    }

    //-------------------//
    // loadFromXmlStream //
    //-------------------//
    /**
     * Load a symbol description from an XML stream.
     *
     * @param is the input stream
     * @return a new ShapeSymbol, or null if loading has failed
     */
    public ShapeSymbol loadFromXmlStream (InputStream is)
    {
        try {
            return (ShapeSymbol) jaxbUnmarshal(is);
        } catch (Exception ex) {
            ex.printStackTrace();

            // User already notified
            return null;
        }
    }

    //------------//
    // loadSymbol //
    //------------//
    /**
     * Load a symbol from its textual representation in the Audiveris music
     * symbols resource
     *
     * @param name the symbol name, using formats such as "HALF_REST.02" or
     *             "HALF_REST"
     * @return the symbol built, or null if failed
     */
    public ShapeSymbol loadSymbol (String name)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Asking for Symbol '" + name + "'");
        }

        // Do we have a loaded instance yet?
        ShapeSymbol symbol = symbols.get(name);

        if (symbol == null) {
            InputStream is = ClassUtil.getProperStream(
                WellKnowns.SYMBOLS_FOLDER,
                name + FILE_EXTENSION);

            if (is != null) {
                // Then we de-serialize the symbol description
                symbol = loadFromXmlStream(is);

                try {
                    is.close();
                } catch (IOException ignored) {
                }

                if (symbol == null) {
                    logger.warning("Could not load symbol '" + name + "'");
                } else {
                    if (logger.isFineEnabled()) {
                        logger.fine("Symbol '" + name + "' loaded");
                    }

                    // Cache the symbol for future reuse
                    symbols.put(name, symbol);
                }
            } else {
                if (logger.isFineEnabled()) {
                    logger.fine("No resource file for symbol " + name);
                }
            }
        }

        return symbol;
    }

    //-------------//
    // storeSymbol //
    //-------------//
    /**
     * Store the textual representation of a symbol, using the internal name
     *
     * @param symbol the symbol to store
     */
    public void storeSymbol (ShapeSymbol symbol)
    {
        String name = symbol.getName();

        if ((name == null) || name.equals("")) {
            logger.warning("Cannot store symbol with no name defined");
        } else {
            OutputStream os;

            // We store only into the local dir
            try {
                os = getIconOutputStream(name);
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot store symbol " + name);

                return;
            }

            // Just serialize into this stream
            if (storeToXmlStream(symbol, os)) {
                logger.info("Icon '" + name + "' successfully stored");
            } else {
                logger.warning("Could not store symbol " + name);
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
     * Store an symbol description to an XML stream.
     *
     * @param symbol the symbol to store
     * @param os the output stream
     *
     * @return true if successful, false otherwise
     */
    public boolean storeToXmlStream (ShapeSymbol  symbol,
                                     OutputStream os)
    {
        try {
            jaxbMarshal(symbol, os);

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
     * Build a monochrome image out of an array of strings. This is meant to
     * ease JAXB unmarshalling.
     *
     * @param rows the lines of characters
     * @param baseColor the base color
     *
     * @return the decoded image
     */
    BufferedImage decodeImage (String[] rows,
                               Color    baseColor)
    {
        // Create the DataBuffer to hold the pixel samples
        final int      width = rows[0].length();
        final int      height = rows.length;

        // Create Raster
        WritableRaster raster = Raster.createPackedRaster(
            DataBuffer.TYPE_INT,
            width,
            height,
            new int[] { 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000 }, // bandMasks RGBA
            null); // origin

        // Populate the data buffer
        DataBuffer dataBuffer = raster.getDataBuffer();
        int        base = baseColor.getRGB() & 0x00ffffff; // No alpha kept here
        int        index = 0;

        for (String row : rows) {
            for (int x = 0; x < width; x++) {
                dataBuffer.setElem(
                    index++,
                    (getAlpha(row.charAt(x)) << 24) | base);
            }
        }

        // Create the image
        ColorModel    model = ColorModel.getRGBdefault();
        BufferedImage bufferedImage = new BufferedImage(
            model,
            raster,
            false,
            null);

        return bufferedImage;
    }

    //-------------//
    // encodeImage //
    //-------------//
    /**
     * Build an array of strings from a given image. This is meant to ease JAXB
     * marshalling.
     *
     * @param symbol the symbol, whose image is to be used
     * @return the array of strings
     */
    String[] encodeImage (ShapeSymbol symbol)
    {
        return encodeImage(symbol.getUnderlyingImage());
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

    //----------//
    // getAlpha //
    //----------//
    /**
     * Compute the alpha that corresponds to the given char
     *
     * @param c the char
     * @return the corresponding pixel alpha level
     */
    private int getAlpha (char c)
    {
        return 255 - toLevel(c);
    }

    //---------------------//
    // getIconOutputStream //
    //---------------------//
    private OutputStream getIconOutputStream (String name)
        throws FileNotFoundException
    {
        File folder = WellKnowns.SYMBOLS_FOLDER;

        if (folder.mkdirs()) {
            logger.info("Creating directory " + folder);
        }

        File file = new File(folder, name + FILE_EXTENSION);

        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException ex) {
            logger.warning("Cannot open output stream to symbol " + file);
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
            jaxbContext = JAXBContext.newInstance(ShapeSymbol.class);
        }

        return jaxbContext;
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
        int r = (argb & 0x00ff0000) >>> 16; // Red
        int g = (argb & 0x0000ff00) >>> 8; // Green
        int b = (argb & 0x000000ff) >>> 0; // Blue
        int index = (int) Math.rint((a * (r + g + b)) / (108.0 * 255)); // 3 * 36

        return charTable[index];
    }

    //-------------//
    // jaxbMarshal //
    //-------------//
    private void jaxbMarshal (ShapeSymbol  symbol,
                              OutputStream os)
        throws JAXBException
    {
        Marshaller m = getJaxbContext()
                           .createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(symbol, os);
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

    //---------//
    // toLevel //
    //---------//
    /**
     * Compute the pixel gray level that corresponds to the given char
     *
     * @param c the char
     * @return the corresponding pixel value ( 0 .. 255)
     */
    private int toLevel (char c)
    {
        // Check the char
        if (c == TRANSPARENT) {
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
}
