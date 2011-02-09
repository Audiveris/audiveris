//----------------------------------------------------------------------------//
//                                                                            //
//                       O c r T e x t V e r i f i e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.util.WrappedBoolean;
import omr.util.XmlUtilities;

/**
 * Class {@code OcrTextVerifier} gathers a set of tests to check whether a text
 * content as provided by the Ocr engine looks consistent with the original
 * glyph.
 *
 * @author Hervé Bitteur
 */
public class OcrTextVerifier
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        OcrTextVerifier.class);

    //~ Methods ----------------------------------------------------------------

    //-------//
    // check //
    //-------//
    public static boolean check (Glyph   glyph, OcrLine ocrLine)
    {
        // Check this is not a tuplet
        if (ocrLine.value.equals("3") &&
            (glyph.getShape() == Shape.TUPLET_THREE)) {
            if (logger.isFineEnabled()) {
                logger.fine("This text is a tuplet 3");
            }

            return false;
        }

        if (ocrLine.value.equals("6") &&
            (glyph.getShape() == Shape.TUPLET_SIX)) {
            if (logger.isFineEnabled()) {
                logger.fine("This text is a tuplet 6");
            }

            return false;
        }

        // Check for abnormal characters
        WrappedBoolean stripped = new WrappedBoolean(false);
        XmlUtilities.stripNonValidXMLCharacters(ocrLine.value, stripped);

        if (stripped.isSet()) {
            return false;
        }

        return true;
    }
}
