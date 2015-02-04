//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S c o r e S t e p                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.score.DurationRetriever;
import omr.score.MeasureFixer;
import omr.score.Score;
import omr.score.ScoreReduction;
import omr.score.entity.Page;

import omr.sheet.Book;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code ScoreStep} implements the <b>SCORE</b> step, which merges all pages
 * into one score.
 *
 * @author Hervé Bitteur
 */
public class ScoreStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ScoreStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    //-----------//
    // ScoreStep //
    //-----------//
    /**
     * Creates a new ScoreStep object.
     */
    public ScoreStep ()
    {
        super(
                Steps.SCORE,
                Level.SCORE_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Build the final score");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        Steps.valueOf(Steps.SYMBOLS).displayUI(sheet);
    }

    //--------//
    // doStep //
    //--------//
    /**
     * Notify the completion to ALL sheets of the merge and not just the first one.
     *
     * @param systems
     * @param sheet
     * @throws omr.step.StepException
     */
    @Override
    public void doStep (Collection<SystemInfo> systems,
                        Sheet sheet)
            throws StepException
    {
        super.doStep(systems, sheet);

        // Set completion for all sheets of the book
        for (Sheet sh : sheet.getBook().getSheets()) {
            done(sh);
        }
    }

    //------//
    // doit //
    //------//
    @Override
    protected void doit (Collection<SystemInfo> systems,
                         Sheet sheet)
            throws StepException
    {
        final Book book = sheet.getBook();

        for (Score score : book.getScores()) {
            // Merge the pages (connecting the parts across pages)
            // TODO: this may need the addition of dummy parts in some pages
            new ScoreReduction(score).reduce();

            // This work needs to know which time sig governs any measure, and this
            // time sig may be inherited from a previous page, therefore it cannot
            // be performed on every page in isolation (except when the page starts
            // with an explicit time sig).
            for (Page page : score.getPages()) {
                //                // - Retrieve the actual duration of every measure
                //                page.accept(new DurationRetriever());
                //
                //                // - Check all voices timing, assign forward items if needed.
                //                // - Detect special measures and assign proper measure ids
                //                // If needed, we can trigger a reprocessing of this page
                //                page.accept(new MeasureFixer());
                //
                // Check whether time signatures are consistent accross all pages in score
                // TODO: to be implemented
                //
                // Connect slurs across pages
                page.getFirstSystem().connectPageInitialSlurs();
            }
        }
    }
}
