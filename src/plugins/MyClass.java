//----------------------------------------------------------------------------//
//                                                                            //
//                               M y C l a s s                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
import omr.score.Score;
import omr.score.ui.ScoreController;
import omr.score.ui.ScoreDependent;

import omr.script.Script;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetsController;

import org.jdesktop.application.Action;

/**
 * Class <code>MyClass</code> is meant as just an example of user plugin
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class MyClass
    extends ScoreDependent
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Dump the score currently selected
     */
    @Action(enabledProperty = "scoreAvailable")
    public void dumpCurrentScore ()
    {
        Score score = ScoreController.getCurrentScore();

        if (score != null) {
            score.dump();
        }
    }

    /**
     * Dump the script of the sheet currently selected
     */
    @Action(enabledProperty = "sheetAvailable")
    public void dumpCurrentScript ()
    {
        Sheet sheet = SheetsController.selectedSheet();

        if (sheet != null) {
            Script script = sheet.getScript();

            if (script != null) {
                script.dump();
            }
        }
    }
}
