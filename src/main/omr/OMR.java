//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                              O M R                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.ui.OmrGui;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * Class {@code OMR} gathers definitions and dependencies for an OMR application.
 * <p>
 * <img alt="OMR diagram" src="doc-files/Omr.png" />
 *
 * @author Hervé Bitteur
 */
public class OMR
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

    /** The extension used for score output files: {@value}. */
    public static final String SCORE_EXTENSION = ".xml";

    /** The prefix used for sheet output files in a multi-sheet book: {@value}. */
    public static final String SHEET_PREFIX = "sheet#";

    /** OMR engine. */
    private static OmrEngine engine;

    /** Master view, if any. */
    private static OmrGui gui;

    //~ Constructors -------------------------------------------------------------------------------
    /** Do not instantiate. */
    private OMR ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getApplication //
    //----------------//
    /**
     * Report the single instance of this GUI SAF application.
     *
     * @return the SingleFrameApplication instance
     */
    public static SingleFrameApplication getApplication ()
    {
        return (SingleFrameApplication) Application.getInstance();
    }

    //-----------//
    // getEngine //
    //-----------//
    /**
     * Report the omr engine.
     *
     * @return the engine
     */
    public static OmrEngine getEngine ()
    {
        return engine;
    }

    //--------//
    // getGui //
    //--------//
    /**
     * Report the omr Gui, if any.
     *
     * @return the main gui, or null when in batch mode
     */
    public static OmrGui getGui ()
    {
        return gui;
    }

    //-----------//
    // setEngine //
    //-----------//
    /**
     * Assign the omr engine.
     *
     * @param engine the engine to set
     */
    public static void setEngine (OmrEngine engine)
    {
        OMR.engine = engine;
    }

    //--------//
    // setGui //
    //--------//
    /**
     * Assign the omr Gui.
     *
     * @param gui the main gui.
     */
    public static void setGui (OmrGui gui)
    {
        OMR.gui = gui;
    }
}
