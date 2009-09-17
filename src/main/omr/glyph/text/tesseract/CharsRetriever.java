//----------------------------------------------------------------------------//
//                                                                            //
//                        C h a r s R e t r i e v e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text.tesseract;

import net.gencsoy.tesjeract.EANYCodeChar;

import java.nio.ByteBuffer;

/**
 * Interface <code>CharsRetriever</code> is meant to isolate OS-dependent parts
 * when interfacing Tesseract OCR.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
interface CharsRetriever
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Retrieve all char descriptions out of the provided (TIFF) image buffer.
     * @param tifImageBuffer the input buffer to process
     * @param languageCode the 3-letter language code
     * @return the array of retrieved char descriptors
     */
    EANYCodeChar[] retrieveChars (ByteBuffer tifImageBuffer,
                                  String     languageCode);
}
