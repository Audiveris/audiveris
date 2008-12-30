//----------------------------------------------------------------------------//
//                                                                            //
//                          T e s s e r a c t O C R                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.sheet.picture.PictureLoader;

import omr.util.FileUtil;
import omr.util.Implement;

import java.awt.Dimension;
import java.awt.Rectangle;
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
 * Implementation of this class is derived from the project VietOCR developed by
 * Quan Nguyen (nguyenq@users.sf.net)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class TesseractOCR
    implements OCR
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TesseractOCR.class);

    /** Language option for Tesseract */
    private static final String LANGUAGE_OPTION = "-l";

    /** Singleton */
    private static OCR INSTANCE = new TesseractOCR();

    // CodePoint values for various dashes
    private static int dashCodePoint = "-".codePointAt(0);
    private static int longDashCodePoint = "—".codePointAt(0);

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
            String[] dirNames = new File(
                constants.tesseractPath.getValue() + "/tessdata").list(
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

    //------//
    // main //
    //------//
    /**
     * Just to test this class in a standalone manner
     * @param args unused
     */
    public static void main (String... args)
    {
        try {
            BufferedImage img = PictureLoader.loadImageIO(
                new File("examples/tempImageFile0.tif"));

            new TesseractOCR().recognize(img, "fra");
            new TesseractOCR().recognize(img, null);
        } catch (IOException ex) {
            logger.warning("IOException", ex);
        } catch (InterruptedException ex) {
            logger.warning("InterruptedException", ex);
        }
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
    public List<String> recognize (BufferedImage image,
                                   String        languageCode)
        throws IOException, InterruptedException
    {
        // Store the input image on disk, to be later cleaned up
        File imageFile = storeOnDisk(image);
        imageFile.deleteOnExit();

        // Text output, to be later cleaned up as well
        String radix = FileUtil.getNameSansExtension(imageFile);
        File   txtFile = new File(imageFile.getParent(), radix + ".txt");
        txtFile.deleteOnExit();

        Dimension dim = new Dimension(image.getWidth(), image.getHeight());

        if (false) {
            return useTesseract(imageFile, txtFile, languageCode);
        } else {
            return useDllTest(imageFile, txtFile, languageCode, dim);
        }
    }

    //---------------//
    // getCodedLines //
    //---------------//
    /**
     * Read the lines of text as written by dlltest. We use the bounding box of
     * each char to more precisely insert the char into the resulting string, in
     * order to preserve spaces
     * @param txtFile the file written by dlltest
     * @return the list of lines
     * @throws java.io.UnsupportedEncodingException
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    private List<String> getCodedLines (File txtFile)
        throws UnsupportedEncodingException, FileNotFoundException, IOException
    {
        /* -- One example --
           s[73]->[73](556,35)->(568,17)
           i[69]->[69](569,35)->(578,8)
           t[74]->[74](580,35)->(590,14)
           z[7a]->[7a](590,35)->(606,17)
           [e2][80][9d]->[201d](608,17)->(616,9)
           i[69]->[69](645,34)->(654,7)
           c[63]->[63](655,34)->(669,16)
           h[68]->[68](671,34)->(689,7)
         */

        /* -- Another example --
           A[41]->[41](1,28)->(29,-1)
           -[2d]->[2d](79,21)->(91,15)
           -[2d]->[2d](190,21)->(202,15)
           -[2d]->[2d](301,21)->(313,15)
           v[76]->[76](363,29)->(384,8)
           c[63]->[63](383,29)->(401,7)
           M[4d]->[4d](420,28)->(457,-1)
           a[61]->[61](456,29)->(475,7)
           -[2d]->[2d](479,21)->(491,15)
           r[72]->[72](497,28)->(512,7)
           i[69]->[69](511,28)->(522,-2)
           -[2d]->[2d](574,21)->(587,15)
           -[2d]->[2d](691,21)->(704,15)
           -[2d]->[2d](808,21)->(821,15)
           -[2d]->[2d](925,21)->(938,14)
           a[61]->[61](993,30)->(1010,8)
           ![21]->[21](1012,30)->(1019,-2)
           <para>
         */
        BufferedReader in = new BufferedReader(
            new InputStreamReader(new FileInputStream(txtFile)));

        List<String> lines = new ArrayList<String>();
        LineDesc     line = null;
        String       str;
        boolean      afterSpace = false;
        boolean      afterDash = false;

        while ((str = in.readLine()) != null) {
            ///logger.info("str=\"" + str + "\"");
            if (str.equals("<para>")) {
                // End of whole text
                if (line != null) {
                    lines.add(line.toString());
                    line = null;
                }

                afterSpace = afterDash = false;

                break;
            } else if (str.equals("<nl>")) {
                // End of a line, beginning of a New line
                if (line != null) {
                    lines.add(line.toString());
                    line = null;
                }

                afterSpace = afterDash = false;
            } else if (str.length() > 0) {
                // Character definition
                CharDesc charDesc = decodeChar(str, afterSpace, afterDash);
                afterSpace = false;
                afterDash = charDesc.isDash();

                if (line == null) {
                    // Start of a line
                    line = new LineDesc(charDesc);
                } else {
                    // Continuation of a line
                    line.addChar(charDesc);
                }
            } else {
                // Space between words
                afterSpace = true;
                afterDash = false;
            }
        }

        in.close();

        //        for (String aLine : lines) {
        //            logger.info("dll ocr: \"" + aLine + "\"");
        //        }
        return lines;
    }

    //----------//
    // getLines //
    //----------//
    /**
     * Read the lines of text as written by tesseract
     * @param txtFile the file written by tesseract
     * @return the list of lines
     * @throws java.io.UnsupportedEncodingException
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    private List<String> getLines (File txtFile)
        throws UnsupportedEncodingException, FileNotFoundException, IOException
    {
        BufferedReader in = new BufferedReader(
            new InputStreamReader(new FileInputStream(txtFile), "UTF-8"));

        List<String>   lines = new ArrayList<String>();
        String         str;

        while ((str = in.readLine()) != null) {
            if (str.length() > 0) {
                lines.add(str);
            }
        }

        in.close();

        for (String line : lines) {
            logger.info("ocr: \"" + line + "\"");
        }

        return lines;
    }

    //------------//
    // decodeChar //
    //------------//
    private CharDesc decodeChar (String  str,
                                 boolean afterSpace,
                                 boolean afterDash)
    {
        ///logger.info("str=\"" + str + "\"");
        /*
         * z[7a]->[7a](590,35)->(606,17)
         *[e2][80][9d]->[201d](608,17)->(616,9)
         */
        final String ARROW = "->";

        // Skip everything until arrow included
        int    arrowPos = str.indexOf(ARROW);
        String tail = str.substring(arrowPos + ARROW.length());

        // Read codepoint
        int    ketPos = tail.indexOf("]");
        String hexStr = tail.substring(1, ketPos);
        int    codePoint = Integer.parseInt(hexStr, 16);
        tail = tail.substring(ketPos + 1);

        // Kludge for dashes
        if (codePoint == longDashCodePoint) {
            codePoint = dashCodePoint;
        }

        // Read bottom left point
        int leftPos = tail.indexOf("(");
        int commaPos = tail.indexOf(",");
        int rightPos = tail.indexOf(")");
        int blX = Integer.parseInt(tail.substring(leftPos + 1, commaPos));
        int blY = Integer.parseInt(tail.substring(commaPos + 1, rightPos));
        tail = tail.substring(rightPos + 1);

        // Read top right point
        leftPos = tail.indexOf("(");
        commaPos = tail.indexOf(",");
        rightPos = tail.indexOf(")");

        int trX = Integer.parseInt(tail.substring(leftPos + 1, commaPos));
        int trY = Integer.parseInt(tail.substring(commaPos + 1, rightPos));

        // Slight corrections on top right point
        trX -= 1;
        trY += 1;

        Rectangle box = new Rectangle(blX, trY, trX - blX, blY - trY);
        CharDesc  desc = new CharDesc(codePoint, box, afterSpace, afterDash);

        if (logger.isFineEnabled()) {
            logger.fine(str + " : " + desc);
        }

        return desc;
    }

    //--------------//
    // errorMessage //
    //--------------//
    private String errorMessage (int result)
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
     * Store the input image on disk, using a temporary file, with TIFF format
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

    //------------//
    // useDllTest //
    //------------//
    private List<String> useDllTest (File      imageFile,
                                     File      txtFile,
                                     String    languageCode,
                                     Dimension dim)
        throws IOException, InterruptedException
    {
        // Generate the command for dlltest process
        List<String> cmd = new ArrayList<String>();
        cmd.add(constants.tesseractPath.getValue() + "/dlltest");
        cmd.add(imageFile.getName());
        cmd.add(txtFile.getName());

        if (languageCode != null) {
            cmd.add(languageCode);
        }

        if (logger.isFineEnabled()) {
            logger.fine("cmd=" + cmd);
        }

        // Launch dlltest process & wait for result
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(imageFile.getParentFile());
        pb.command(cmd);
        pb.redirectErrorStream(true);

        int result = pb.start()
                       .waitFor();

        if (result == 0) {
            // Read & report the resulting string(s)
            return getCodedLines(txtFile);
        } else {
            throw new RuntimeException(errorMessage(result));
        }
    }

    //--------------//
    // useTesseract //
    //--------------//
    private List<String> useTesseract (File   imageFile,
                                       File   txtFile,
                                       String languageCode)
        throws IOException, InterruptedException
    {
        // Generate the command for tesseract process
        List<String> cmd = new ArrayList<String>();
        cmd.add(constants.tesseractPath.getValue() + "/tesseract");
        cmd.add(imageFile.getName());
        cmd.add(FileUtil.getNameSansExtension(txtFile));

        if (languageCode != null) {
            cmd.add(LANGUAGE_OPTION);
            cmd.add(languageCode);
        }

        if (logger.isFineEnabled()) {
            logger.fine("cmd=" + cmd);
        }

        // Launch tesseract process
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(imageFile.getParentFile());
        pb.command(cmd);
        pb.redirectErrorStream(true);

        int result = pb.start()
                       .waitFor();

        if (result == 0) {
            // Read & report the resulting string(s)
            return getLines(txtFile);
        } else {
            throw new RuntimeException(errorMessage(result));
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

        /** Precise path to the Tesseract tool */
        private final Constant.String tesseractPath = new Constant.String(
            Main.getHomeFolder() + "/tesseract",
            "Path to the Tesseract tool");
    }
}
