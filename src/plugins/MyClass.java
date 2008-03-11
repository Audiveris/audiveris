/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import omr.score.Score;
import omr.score.ui.ScoreController;
import omr.score.ui.ScoreDependent;

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
}
