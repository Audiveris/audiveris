//----------------------------------------------------------------------------//
//                                                                            //
//                          T e s s e r a c t O C R                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text.tesseract;

import omr.Main;

import omr.glyph.text.*;

import omr.log.Logger;

import omr.util.Implement;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Class <code>TesseractOCR</code> is an OCR service built on the Google
 * Tesseract engine.
 *
 * <p>It relies on the <b>tessdll.dll</b> library, accessed through the
 * <b>tesjeract</b> Java interface.</p>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class TesseractOCR
    implements OCR
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TesseractOCR.class);

    /** Singleton */
    private static OCR INSTANCE = new TesseractOCR();

    /** Folder where all OCR material is available */
    public static File ocrHome = Main.getOcrFolder();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new TesseractOCR object.
     */
    private TesseractOCR ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the singleton
     * @return the TesseractCOR instance
     */
    public static OCR getInstance ()
    {
        return INSTANCE;
    }

    //-----------------------//
    // getSupportedLanguages //
    //-----------------------//
    /**
     * {@inheritDoc}
     */
    @Implement(OCR.class)
    public Map<String, String> getSupportedLanguages ()
    {
        Map<String, String> map = new LinkedHashMap<String, String>();

        try {
            // Retrieve correspondences between codes and names
            Properties      langNames = new Properties();
            FileInputStream fis = new FileInputStream(
                new File(Main.getConfigFolder(), "ISO639-3.xml"));
            langNames.loadFromXML(fis);
            fis.close();

            // Retrieve all implemented codes
            String[] dirNames = new File(ocrHome, "tessdata").list(
                new FilenameFilter() {
                        public boolean accept (File   dir,
                                               String name)
                        {
                            return name.endsWith(".inttemp");
                        }
                    });

            // Fill the language map with only the implemented languages
            if (dirNames != null) {
                for (String fileName : dirNames) {
                    String code = fileName.replace(".inttemp", "");
                    map.put(code, langNames.getProperty(code, code));
                }
            }
        } catch (IOException ex) {
            logger.warning("Missing config/ISO639-3.xml file", ex);
        } catch (Exception ex) {
            logger.warning("Error in loading languages", ex);
        }

        return map;
    }

    //-----------//
    // recognize //
    //-----------//
    /**
     * {@inheritDoc}
     * @throws IOException
     * @throws InterruptedException
     */
    @Implement(OCR.class)
    public List<OcrLine> recognize (BufferedImage image,
                                    String        languageCode)
        throws IOException, InterruptedException
    {
        // Store the input image on disk, to be later cleaned up
        File imageFile = storeOnDisk(image);
        imageFile.deleteOnExit();

        // Use the tessdll.dll implementation
        return UseTessDllDll.getInstance()
                            .retrieveLines(imageFile, languageCode);
    }

    //--------------//
    // errorMessage //
    //--------------//
    /**
     * Description of the Tesseract error carried by its process return code
     * @param result the rpocess return code
     * @return the related description
     */
    @SuppressWarnings("unused")
    static String errorMessage (int result)
    {
        switch (result) {
        case 1 :
            return "Errors accessing files. " +
                   "There may be spaces in your image's filename.";

        case 29 :
            return "Cannot recognize the image or its selected region.";

        case 31 :
            return "Unsupported image format.";

        default :
            return "Errors occurred.";
        }
    }

    //-------------//
    // storeOnDisk //
    //-------------//
    /**
     * Store the input image on disk, using a temporary file, with TIFF format.
     * TODO: One day, we should try to avoid the use of an intermediate file,
     * and directly pass a memory buffer to tesseract.
     * @param image the input image
     * @return the written file
     */
    private File storeOnDisk (BufferedImage image)
        throws IOException
    {
        File              outputFile = File.createTempFile("ocr", ".tif");
        ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile);

        // Take the first suitable TIFF writer
        ImageWriter writer = ImageIO.getImageWritersByFormatName("tiff")
                                    .next();
        writer.setOutput(ios);
        writer.write(image);
        ios.close();

        return outputFile;
    }
}
