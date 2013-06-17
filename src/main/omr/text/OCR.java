//----------------------------------------------------------------------------//
//                                                                            //
//                                   O C R                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.sheet.SystemInfo;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

/**
 * Interface {@code OCR} defines the interaction with an OCR engine.
 *
 * @author Hervé Bitteur
 */
public interface OCR
{
    //~ Enumerations -----------------------------------------------------------

    /** Handling of image layout. */
    enum LayoutMode
    {
        //~ Enumeration constant initializers ----------------------------------

        /** Automatic discovery of multi block layout */
        MULTI_BLOCK,
        /** No layout
         * processing, a single block is assumed */
        SINGLE_BLOCK;

    }

    //~ Methods ----------------------------------------------------------------
    //
    /**
     * Report the set of supported language codes
     *
     * @return the set of supported 3-letter codes
     */
    Set<String> getLanguages ();

    /**
     * Report whether the OCR engine is available.
     */
    boolean isAvailable ();

    /**
     * Launch the recognition of the provided image, whose language is
     * specified.
     *
     * @param image        the provided image
     * @param topLeft      absolute coordinates of the image top left corner
     * @param languageCode language specification or null
     * @param layoutMode   how the image layout should be analyzed
     * @param system       the containing system
     * @param label        an optional label related to the image, null
     *                     otherwise. This is meant for keeping track of the
     *                     temporary image files.
     * @return a list of TextLine instances, or null.
     *         The coordinates of any returned TextLine are absolute coordinates
     *         thanks to the topLeft parameter.
     */
    List<TextLine> recognize (BufferedImage image,
                              Point topLeft,
                              String languageCode,
                              LayoutMode layoutMode,
                              SystemInfo system,
                              String label);

    //~ Inner Classes ----------------------------------------------------------
    /**
     * Exception used to signal that no OCR is actually available.
     */
    static class UnavailableOcrException
            extends RuntimeException
    {
    }
}
