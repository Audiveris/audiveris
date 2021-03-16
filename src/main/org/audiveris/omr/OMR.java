//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                              O M R                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr;

import org.audiveris.omr.ui.OmrGui;

/**
 * Class {@code OMR} gathers definitions and dependencies for an OMR application.
 * <p>
 * <img alt="OMR diagram" src="doc-files/Omr.png">
 *
 * @author Hervé Bitteur
 */
public abstract class OMR
{

    /** The extension used for compressed score output files: {@value}. */
    public static final String COMPRESSED_SCORE_EXTENSION = ".mxl";

    /** The extension prefix used for movement output files: {@value}. */
    public static final String MOVEMENT_EXTENSION = ".mvt";

    /** The (double) extension used for opus output files: {@value}. */
    public static final String OPUS_EXTENSION = ".opus.mxl";

    /** The extension used for score print files: {@value}. */
    public static final String PRINT_EXTENSION = "-print.pdf";

    /** The extension used for Audiveris book files: {@value}. */
    public static final String BOOK_EXTENSION = ".omr";

    /** The extension used for score output files: {@value}. */
    public static final String SCORE_EXTENSION = ".xml";

    /** The suffix used for a single sheet output in a multi-sheet book: {@value}. */
    public static final String SHEET_SUFFIX = "#";

    /** OMR engine. */
    public static OmrEngine engine;

    /** Master view, if any. */
    public static OmrGui gui;

    /** Do not instantiate. */
    private OMR ()
    {
    }
}
