/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import omr.score.Score;
import omr.score.ui.ScoreController;
import omr.score.ui.ScoreDependent;

import omr.sheet.Sheet;
import org.jdesktop.application.Action;

public class MyClass
    extends ScoreDependent
{
    //~ Methods ----------------------------------------------------------------

    @Action(enabledProperty = "scoreAvailable")
    public void dumpCurrentScore ()
    {
        Score score = ScoreController.getCurrentScore();

        if (score != null) {
            score.dump();
        }
    }

    @Action(enabledProperty = "scoreAvailable")
    public void buildFromSections ()
    {
//        Score score = ScoreController.getCurrentScore();
//
//        if (score != null) {
//            Sheet sheet = score.getSheet();
//            sheet.getVerticalsController().testAssignSectionSet();
//        }
    }
}
