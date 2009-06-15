//----------------------------------------------------------------------------//
//                                                                            //
//                         U s e T e s s D l l D l l                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text.tesseract;

import omr.glyph.text.OcrChar;
import omr.glyph.text.OcrLine;
import omr.glyph.text.TextInfo;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.util.OmrExecutors;

import net.gencsoy.tesjeract.EANYCodeChar;
import net.gencsoy.tesjeract.Tesjeract;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Class <code>UseTessDllDll</code> is a TesseractOCR companion, which uses
 * Tesseract through its tessdll.dll, which means no other process is launched,
 * but we interact with the DLL through the tesjeract Java interface.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
class UseTessDllDll
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(UseTessDllDll.class);

    static {
        /** Load needed libraries */
        System.load(
            new File(TesseractOCR.ocrHome, "tessdll.dll").getAbsolutePath());
        System.load(
            new File(TesseractOCR.ocrHome, "tesjeract.dll").getAbsolutePath());

        /** Check that TESSDATA_PREFIX environment variable is set */
        String prefix = System.getenv("TESSDATA_PREFIX");

        if (prefix == null) {
            logger.severe(
                "TESSDATA_PREFIX environment variable is not set." +
                " It must point to the parent folder of \"tessdata\"");
        }

        /** Check that tessdata folder is found */
        File tessdata = new File(prefix, "tessdata");

        if (!tessdata.exists()) {
            logger.severe("\"tessdata\" folder should be in " + prefix);
        }
    }

    /** Singleton */
    private static UseTessDllDll INSTANCE = new UseTessDllDll();

    //~ Constructors -----------------------------------------------------------

    //------------//
    // UseTessDllDll //
    //------------//
    private UseTessDllDll ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the singleton
     * @return the UseTessDllDll instance
     */
    public static UseTessDllDll getInstance ()
    {
        return INSTANCE;
    }

    //---------------//
    // retrieveLines //
    //---------------//
    /**
     * Report all the OCR-detected lines out of the provided image file
     * @param imageFile the image file
     * @param languageCode the dominant language, or null
     * @param label optional label
     * @return the sequence of detected lines
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public List<OcrLine> retrieveLines (File         imageFile,
                                        final String languageCode,
                                        final String label)
        throws FileNotFoundException, IOException, InterruptedException,
                   ExecutionException
    {
        if (logger.isFineEnabled()) {
            logger.fine("OCR on " + imageFile);
        }

        FileInputStream         fis = new FileInputStream(imageFile);
        final MappedByteBuffer  buf = fis.getChannel()
                                         .map(
            MapMode.READ_ONLY,
            0,
            imageFile.length());

        Callable<List<OcrLine>> task = new Callable<List<OcrLine>>() {
            public List<OcrLine> call ()
                throws Exception
            {
                Tesjeract      tess = new Tesjeract(languageCode);
                EANYCodeChar[] chars = tess.recognizeAllWords(buf);

                return getLines(chars, label);
            }
        };

        // Launch task and wait for its result ...
        return OmrExecutors.getOcrExecutor()
                           .submit(task)
                           .get();
    }

    //----------//
    // getLines //
    //----------//
    private List<OcrLine> getLines (EANYCodeChar[] chars,
                                    String         label)
    {
        if (logger.isFineEnabled()) {
            dumpChars(chars, label);
        }

        List<OcrLine> lines = new ArrayList<OcrLine>();
        List<OcrChar> lineChars = new ArrayList<OcrChar>();
        int           lastPointSize = -1;

        try {
            for (int index = 0; index < chars.length; index++) {
                EANYCodeChar ch = chars[index];

                // Compute the number of bytes for this UTF8 sequence
                int byteCount = utf8ByteCount(ch.char_code);
                lineChars.add(buildCharDesc(chars, index, byteCount));

                // Let's move to the very last byte of the sequence
                // To use its formatting data
                index += (byteCount - 1);
                ch = chars[index];
                lastPointSize = ch.point_size;

                // End of line?
                if (isNewLine(ch)) {
                    lines.add(new OcrLine(lastPointSize, lineChars, null));
                    lineChars.clear();
                }
            }

            // Debugging: we've found nothing
            if (lines.isEmpty()) {
                dumpChars(chars, label);
            }

            // TODO: (is this useful?) Just in case we've missed the end
            if (!lineChars.isEmpty()) {
                lines.add(new OcrLine(lastPointSize, lineChars, null));
            }

            return lines;
        } catch (Exception ex) {
            logger.warning("Error decoding tesseract output", ex);
            dumpChars(chars, label);

            return null;
        }
    }

    //-----------//
    // isNewLine //
    //-----------//
    /**
     * Report whether this char is the last one of a line
     * @param ch the char descriptor
     * @return true if end of line
     */
    private static boolean isNewLine (EANYCodeChar ch)
    {
        return ((ch.formatting & 0x40) != 0) // newLine
                ||((ch.formatting & 0x80) != 0); // newPara
    }

    //---------------//
    // buildCharDesc //
    //---------------//
    /**
     * Extract and translate the character value out of the UTF8 sequence, while
     * fixing extension character if any
     * @param chars the global char sequence returned by Tesseract
     * @param index the starting index in the chars sequence
     * @param byteCount the number of bytes of the UTF8 sequence
     * @return the proper string value
     * @throws java.io.UnsupportedEncodingException if UTF8 sequence is wrong
     */
    private OcrChar buildCharDesc (EANYCodeChar[] chars,
                                   int            index,
                                   int            byteCount)
        throws UnsupportedEncodingException
    {
        EANYCodeChar   ch = chars[index];

        // Copy char box information (with slight corrections)
        PixelRectangle box = new PixelRectangle(
            ch.left,
            ch.top + 1, // Correction
            ch.right - ch.left,
            ch.bottom - ch.top);

        // Get correct string value
        String str = null;

        // Check for extension character badly recognized, using aspect
        double aspect = (double) box.width / (double) box.height;

        if (aspect >= TextInfo.getMinExtensionAspect()) {
            if (logger.isFineEnabled()) {
                logger.fine("Suspecting an Extension character");
            }

            str = TextInfo.EXTENSION_STRING;
        } else {
            byte[] bytes = new byte[1000];

            for (int i = 0; i < byteCount; i++) {
                bytes[i] = (byte) chars[index + i].char_code;
            }

            str = new String(Arrays.copyOf(bytes, byteCount), "UTF8");
        }

        return new OcrChar(str, box, ch.point_size, ch.blanks);
    }

    //-----------//
    // dumpChars //
    //-----------//
    /**
     * Dump the raw char descriptions as read from Tesseract
     * @param chars the sequence of raw char descriptions
     * @param the optional label
     */
    private static void dumpChars (EANYCodeChar[] chars,
                                   String         label)
    {
        System.out.println(
            "-- " + ((label != null) ? label : "") + " Raw Tesseract output:");
        System.out.println(
            "char     code  left right   top   bot  font  conf  size blanks   format");

        for (EANYCodeChar ch : chars) {
            System.out.println(
                String.format(
                    "%3s %5d=%2Xh %5d %5d %5d %5d %5d %5d %5d %5d %5d=%2Xh",
                    String.copyValueOf(Character.toChars(ch.char_code)),
                    ch.char_code,
                    ch.char_code,
                    ch.left,
                    ch.right,
                    ch.top,
                    ch.bottom,
                    ch.font_index,
                    ch.confidence,
                    ch.point_size,
                    ch.blanks,
                    ch.formatting,
                    ch.formatting));
        }
    }

    //---------------//
    // utf8ByteCount //
    //---------------//
    /**
     * Return the number of bytes of this UTF8 sequence
     * @param code the char code of the first byte of the sequence
     * @return the number of bytes for this sequence (or 0 if the byte is not
     * a sequence starting byte)
     */
    private static int utf8ByteCount (int code)
    {
        // Unicode          Byte1    Byte2    Byte3    Byte4
        // -------          -----    -----    -----    -----
        // U+0000-U+007F        0xxxxxxx
        // U+0080-U+07FF        110yyyxx 10xxxxxx
        // U+0800-U+FFFF        1110yyyy 10yyyyxx 10xxxxxx
        // U+10000-U+10FFFF 11110zzz 10zzyyyy 10yyyyxx 10xxxxxx
        if ((code & 0x80) == 0x00) {
            return 1;
        }

        if ((code & 0xE0) == 0xC0) {
            return 2;
        }

        if ((code & 0xF0) == 0xE0) {
            return 3;
        }

        if ((code & 0xF8) == 0xF0) {
            return 4;
        }

        // This is not a legal sequence start
        return 0;
    }
}
