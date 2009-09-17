//----------------------------------------------------------------------------//
//                                                                            //
//                  W i n d o w s C h a r R e t r i e v e r                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text.tesseract;

import omr.log.Logger;

import omr.util.Implement;

import net.gencsoy.tesjeract.EANYCodeChar;
import net.gencsoy.tesjeract.Tesjeract;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Class <code>WindowsCharsRetriever</code> is the Windows implementation of
 * the interaction with Tesseract OCR engine.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
class WindowsCharsRetriever
    implements CharsRetriever
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        WindowsCharsRetriever.class);

    static {
        /** Check that TESSDATA_PREFIX environment variable is set */
        String prefix = System.getenv("TESSDATA_PREFIX");

        if (prefix == null) {
            logger.severe(
                "TESSDATA_PREFIX environment variable is not set." +
                " It must point to the parent folder of \"tessdata\"" +
                ", typically \"" + TesseractOCR.ocrHome + '"');
        } else {
            /** Check that tessdata folder is found */
            File tessdata = new File(prefix, "tessdata");

            if (!tessdata.exists()) {
                logger.severe("\"tessdata\" folder should be in " + prefix);
            }
        }

        /** Load needed libraries */
        System.load(
            new File(TesseractOCR.ocrHome, "tessdll.dll").getAbsolutePath());
        System.load(
            new File(TesseractOCR.ocrHome, "tesjeract.dll").getAbsolutePath());
    }

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new WindowsCharsRetriever object.
     */
    public WindowsCharsRetriever ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // retrieveChars //
    //---------------//
    /**
     * {@inheritDoc}
     *
     * This Windows version accesses Tesseract DLL through Tesjeract.
     */
    @Implement(CharsRetriever.class)
    public EANYCodeChar[] retrieveChars (ByteBuffer tifImageBuffer,
                                         String     languageCode)
    {
        Tesjeract tess = new Tesjeract(languageCode);

        return tess.recognizeAllWords(tifImageBuffer);
    }
}
