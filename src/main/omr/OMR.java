//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                              O M R                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.ui.OmrGui;

/**
 * Class {@code OMR} gathers definitions and dependencies for an OMR application.
 * <p>
 * <img alt="OMR diagram" src="doc-files/Omr.png">
 *
 * @author Hervé Bitteur
 */
public abstract class OMR
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The extension used for bench files: {@value}. */
    public static final String BENCH_EXTENSION = ".bench.properties";

    /** The extension used for compressed score output files: {@value}. */
    public static final String COMPRESSED_SCORE_EXTENSION = ".mxl";

    /** The extension prefix used for movement output files: {@value}. */
    public static final String MOVEMENT_EXTENSION = ".mvt";

    /** The (double) extension used for opus output files: {@value}. */
    public static final String OPUS_EXTENSION = ".opus.mxl";

    /** The extension used for compressed score print files: {@value}. */
    public static final String PDF_EXTENSION = ".pdf";

    /** The extension used for Audiveris project files: {@value}. */
    public static final String PROJECT_EXTENSION = ".omr";

    /** The extension used for score output files: {@value}. */
    public static final String SCORE_EXTENSION = ".xml";

    /** The (double) extension used for script files: {@value}. */
    public static final String SCRIPT_EXTENSION = ".script.xml";

    /** The suffix used for a single sheet output in a multi-sheet book: {@value}. */
    public static final String SHEET_SUFFIX = "-sheet#";

    /** OMR engine. */
    public static OmrEngine engine;

    /** Master view, if any. */
    public static OmrGui gui;

    //~ Constructors -------------------------------------------------------------------------------
    /** Do not instantiate. */
    private OMR ()
    {
    }
}
