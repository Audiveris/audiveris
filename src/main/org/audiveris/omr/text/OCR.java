//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             O C R                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
    //~ Enumerations -------------------------------------------------------------------------------

    /** Handling of image layout. */
    enum LayoutMode
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Automatic discovery of multi block layout */
        MULTI_BLOCK,
        /** No layout processing, a single block is assumed */
        SINGLE_BLOCK;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    /**
     * Report the set of supported language codes
     *
     * @return the set of supported 3-letter codes
     */
    Set<String> getLanguages ();

    /**
     * Report whether the OCR engine is available.
     *
     * @return true if OCR is OK
     */
    boolean isAvailable ();

    /**
     * Return OCR engine identification.
     *
     * @return string containing the name and the version of the OCR engine.
     */
    String identify();

    /**
     * Launch the recognition of the provided image, whose language is specified.
     *
     * @param interline    typical interline value
     * @param image        the provided image
     * @param topLeft      absolute coordinates of the image top left corner, or null
     * @param languageCode language specification or null
     * @param layoutMode   how the image layout should be analyzed
     * @param label        an optional label related to the image, null otherwise.
     *                     This is meant for keeping track of the temporary image files.
     * @return a list of TextLine instances, or null.
     *         The coordinates of any returned TextLine are absolute coordinates thanks to the topLeft
     *         parameter.
     */
    List<TextLine> recognize (int interline,
                              BufferedImage image,
                              Point topLeft,
                              String languageCode,
                              LayoutMode layoutMode,
                              String label);

    //~ Inner Classes ------------------------------------------------------------------------------
    /**
     * Exception used to signal that no OCR is actually available.
     */
    static class UnavailableOcrException
            extends RuntimeException
    {
    }
}
