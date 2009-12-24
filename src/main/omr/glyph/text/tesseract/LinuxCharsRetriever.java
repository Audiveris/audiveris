//----------------------------------------------------------------------------//
//                                                                            //
//                     L i n u x C h a r s R e t r i e v e r                  //
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

import omr.util.ClassUtil;
import omr.util.Implement;

import net.gencsoy.tesjeract.EANYCodeChar;
import net.gencsoy.tesjeract.Tesjeract;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Class <code>LinuxCharsRetriever</code> is the Linux implementation of
 * the interaction with Tesseract OCR engine.
 *
 * @author James Le Cuirot
 * @version $Id: $
 */
class LinuxCharsRetriever
    implements CharsRetriever
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        LinuxCharsRetriever.class);

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LinuxCharsRetriever object.
     */
    public LinuxCharsRetriever ()
    {
        initialize();
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // retrieveChars //
    //---------------//
    /**
     * {@inheritDoc}
     *
     * This Linux version accesses Tesseract DLL through Tesjeract.
     */
    @Implement(CharsRetriever.class)
    public EANYCodeChar[] retrieveChars (ByteBuffer tifImageBuffer,
                                         String     languageCode)
    {
        Tesjeract tess = new Tesjeract(languageCode);

        return tess.recognizeAllWords(tifImageBuffer);
    }

    //------------//
    // initialize //
    //------------//
    /**
     * Responsible for checking and initializing this retriever
     */
    private void initialize ()
    {
        /** Check that TESSDATA_PREFIX environment variable is set */
        String prefix = System.getenv("TESSDATA_PREFIX");

        /** If not, default to /usr/share/ */
        if (prefix == null) {
            prefix = "/usr/share/";
        }

        /** Check that prefix ends with a "/" */
        if (!prefix.endsWith("/")) {
            throw new RuntimeException(
                "TESSDATA_PREFIX (" + prefix + ") should end with a '/'");
        }

        /** Check that tessdata folder is found */
        File tessdata = new File(prefix, "tessdata");

        if (!tessdata.exists()) {
            throw new RuntimeException(
                "\"tessdata\" folder should be in " + prefix);
        }

        /** Load needed libraries */
        ClassUtil.loadLibrary("tessjnilinux");
    }
}
