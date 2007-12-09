//----------------------------------------------------------------------------//
//                                                                            //
//                      S c r i p t C o n t r o l l e r                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.util.Logger;

import java.io.*;

import javax.swing.*;

/**
 * Class <code>ScriptController</code> is the part of the user interface dealing
 * with selecting the files for loading and storing scripts.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScriptController
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        ScriptController.class);

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // ScriptController //
    //------------------//
    /**
     * Create an instance of script controller
     */
    public ScriptController ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // checkStored //
    //-------------//
    public void checkStored (Script script)
    {
        if (!script.isStored() && constants.closeConfirmation.getValue()) {
            int answer = JOptionPane.showConfirmDialog(
                null,
                "Save script for sheet " + script.getSheet().getRadix() + "?");

            if (answer == JOptionPane.YES_OPTION) {
                ScriptActions.storeScript(script);
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Default directory for saved scripts */
        Constant.String defaultScriptDirectory = new Constant.String(
            "",
            "Default directory for saved scripts");

        /** User confirmation for closing unsaved script */
        Constant.Boolean closeConfirmation = new Constant.Boolean(
            true,
            "Should we ask confirmation for closing a sheet with unsaved script?");
    }
}
