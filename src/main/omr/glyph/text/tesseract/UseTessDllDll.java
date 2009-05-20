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

import omr.glyph.text.OcrLine;

import omr.log.Logger;

import omr.util.Worker;

import net.gencsoy.tesjeract.EANYCodeChar;
import net.gencsoy.tesjeract.Tesjeract;

import java.awt.Rectangle;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.*;

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
     * @return the sequence of detected lines
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public List<OcrLine> retrieveLines (File   imageFile,
                                        String languageCode)
        throws FileNotFoundException, IOException
    {
        //new File("u:/soft/JnaTests/examples/tempImageFile0.tif");
        //new File("u:/soft/JnaTests/examples/Binary-8.png");
        //new File("u:/soft/JnaTests/examples/tempImageFile-8.bmp");
        MappedByteBuffer buf = new FileInputStream(imageFile).getChannel()
                                                             .map(
            MapMode.READ_ONLY,
            0,
            imageFile.length());

        // Serialize access to OCR external utility
        synchronized (UseTessDllDll.class) {
            ///logger.info("Launching OcrWorker");
            OcrWorker worker = new OcrWorker(buf, languageCode);
            worker.start();

            List<OcrLine> lines = worker.get();

            ///logger.info("Getting lines from OcrWorker");
            return lines;
        }
    }

    //----------//
    // getLines //
    //----------//
    private List<OcrLine> getLines (EANYCodeChar[] chars)
    {
        List<OcrLine> lines = new ArrayList<OcrLine>();
        LineDesc      lineDesc = null;
        int           lastPointSize = -1;
        byte[]        bytes = new byte[1000];

        try {
            for (int index = 0; index < chars.length; index++) {
                EANYCodeChar ch = chars[index];

                // Compute the number of bytes for this UTF8 sequence
                int byteCount = utf8ByteCount(ch.char_code);

                for (int i = 0; i < byteCount; i++) {
                    bytes[i] = (byte) chars[index + i].char_code;
                }

                // Get correct string value
                String   str = new String(
                    Arrays.copyOf(bytes, byteCount),
                    "UTF8");

                // Slight corrections on top right point
                //        trX -= 1;
                //        trY += 1;
                CharDesc charDesc = new CharDesc(
                    str,
                    new Rectangle(
                        ch.left,
                        ch.top + 1,
                        ch.right - ch.left,
                        ch.bottom - ch.top),
                    ch.point_size,
                    ch.blanks);

                if (lineDesc == null) {
                    lineDesc = new LineDesc(charDesc);
                } else {
                    lineDesc.addChar(charDesc);
                }

                // Let's move to the very last byte of the sequence
                // To use its formatting data
                index += (byteCount - 1);
                ch = chars[index];
                lastPointSize = ch.point_size;

                // End of line?
                if (isNewLine(ch)) {
                    lines.add(
                        new OcrLine(
                            lastPointSize,
                            lineDesc.getContent(),
                            lineDesc));
                    lineDesc = null;
                }
            }

            // Debugging: we've found nothing
            if (lines.isEmpty()) {
                dumpChars(chars);
            }

            // Just in case we've missed the end (useful? TBD)
            if (lineDesc != null) {
                lines.add(
                    new OcrLine(lastPointSize, lineDesc.getContent(), lineDesc));
            }

            return lines;
        } catch (Exception ex) {
            logger.warning("Error decoding tesseract output", ex);
            dumpChars(chars);

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

    //-----------//
    // dumpChars //
    //-----------//
    /**
     * Dump the raw char descriptions as read from Tesseract
     * @param chars the sequence of raw char descriptions
     */
    private static void dumpChars (EANYCodeChar[] chars)
    {
        System.out.println("--Raw Tesseract output:");
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
        // U+0000-U+007F 	0xxxxxxx
        // U+0080-U+07FF 	110yyyxx 10xxxxxx
        // U+0800-U+FFFF 	1110yyyy 10yyyyxx 10xxxxxx
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

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // OcrWorker //
    //-----------//
    /**
     * An asynchronous Worker for OCR work with sufficient stack size
     */
    private class OcrWorker
        extends Worker<List<OcrLine>>
    {
        //~ Instance fields ----------------------------------------------------

        private final ByteBuffer buffer;
        private final String     languageCode;

        //~ Constructors -------------------------------------------------------

        public OcrWorker (ByteBuffer buffer,
                          String     languageCode)
        {
            // Sufficient stack size
            super(10000000L);
            this.buffer = buffer;
            this.languageCode = languageCode;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public List<OcrLine> construct ()
        {
            Tesjeract      tess = new Tesjeract(languageCode);
            EANYCodeChar[] chars = tess.recognizeAllWords(buffer);
            List<OcrLine>  lines = getLines(chars);

            return lines;
        }
    }
}
