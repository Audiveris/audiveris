//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             O C R                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.text;

import org.audiveris.omr.sheet.Sheet;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

/**
 * Interface <code>OCR</code> defines the interaction with an OCR engine.
 *
 * @author Hervé Bitteur
 */
public interface OCR
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Standard NO_OCR message: {@value}. */
    static String NO_OCR = "No OCR is available!";

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the set of supported language codes
     *
     * @return the set of supported 3-letter codes
     */
    Set<String> getLanguages ();

    /**
     * Report the minimum confidence for an OCR'ed item.
     *
     * @return minimum confidence value
     */
    double getMinConfidence ();

    /**
     * Return OCR engine identification.
     *
     * @return string containing the name and the version of the OCR engine.
     */
    String identify ();

    /**
     * Report whether the OCR engine is available.
     *
     * @return true if OCR is OK
     */
    boolean isAvailable ();

    /**
     * Launch the recognition of the provided image, whose language is specified.
     *
     * @param sheet        the containing sheet
     * @param image        the provided image
     * @param topLeft      absolute coordinates of the image top left corner, or null
     * @param languageCode language specification or null
     * @param layoutMode   how the image layout should be analyzed
     * @param label        an optional label related to the image, null otherwise.
     *                     This is meant for keeping track of the temporary image files.
     * @return a list of TextLine instances, or null.
     *         The coordinates of any returned TextLine are absolute coordinates thanks to the
     *         topLeft parameter.
     */
    List<TextLine> recognize (Sheet sheet,
                              BufferedImage image,
                              Point topLeft,
                              String languageCode,
                              LayoutMode layoutMode,
                              String label);

    //~ Enumerations -------------------------------------------------------------------------------

    //------------//
    // LayoutMode //
    //------------//
    /** Handling of image layout. */
    static enum LayoutMode
    {
        /** Automatic discovery of multi block layout */
        MULTI_BLOCK,
        /** No layout processing, a single block is assumed */
        SINGLE_BLOCK;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-------------------------//
    // UnavailableOcrException //
    //-------------------------//
    /**
     * Exception used to signal that no OCR is actually available.
     */
    static class UnavailableOcrException
            extends RuntimeException
    {

        /**
         * Create a UnavailableOcrException.
         *
         * @param msg related message
         */
        public UnavailableOcrException (String msg)
        {
            super(msg);
        }

        /**
         * Create a UnavailableOcrException.
         *
         * @param msg   related message
         * @param cause exception cause
         */
        public UnavailableOcrException (String msg,
                                        Throwable cause)
        {
            super(msg, cause);
        }
    }
}
