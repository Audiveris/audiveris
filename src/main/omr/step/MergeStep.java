//----------------------------------------------------------------------------//
//                                                                            //
//                             M e r g e S t e p                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ScoreReduction;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code MergeStep} merges all pages into one score
 *
 * @author Hervé Bitteur
 */
public class MergeStep
    extends AbstractStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MergeStep.class);

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // MergeStep //
    //-----------//
    /**
     * Creates a new MergeStep object.
     */
    public MergeStep ()
    {
        super(
            Steps.MERGE,
            Level.SCORE_LEVEL,
            Mandatory.MANDATORY,
            Redoable.REDOABLE,
            SYMBOLS_TAB,
            "Merges all pages into one score");
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        Steps.valueOf(Steps.SYMBOLS)
             .displayUI(sheet);
    }

    //------//
    // doit //
    //------//
    @Override
    protected void doit (Collection<SystemInfo> systems,
                         Sheet                  sheet)
        throws StepException
    {
        Score          score = sheet.getScore();
        ScoreReduction reduction = new ScoreReduction(score);
        reduction.reduce();
    }
}
