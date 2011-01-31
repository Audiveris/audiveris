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

import omr.score.DurationRetriever;
import omr.score.MeasureFixer;
import omr.score.Score;
import omr.score.ScoreReduction;
import omr.score.entity.Page;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.TreeNode;

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

        // Merge the pages
        ScoreReduction reduction = new ScoreReduction(score);
        reduction.reduce();

        // This work needs to know which time sig governs any measure, and this
        // time sig may be inherited from a previous page, therefore it cannot
        // be performed on every page in isolation.
        for (TreeNode pn : score.getPages()) {
            Page page = (Page) pn;

            // - Retrieve the actual duration of every measure
            page.accept(new DurationRetriever());

            // - Check all voices timing, assign forward items if needed.
            // - Detect special measures and assign proper measure ids
            page.accept(new MeasureFixer());

            // Connect slurs across pages
            page.getFirstSystem()
                .connectPageInitialSlurs();
        }
    }
}
