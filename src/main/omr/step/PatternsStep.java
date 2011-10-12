//----------------------------------------------------------------------------//
//                                                                            //
//                          P a t t e r n s S t e p                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.TreeNode;

import java.util.Collection;
import java.util.Iterator;

/**
 * Class {@code PatternsStep} Process specific patterns at sheet glyph level
 * (true,clefs, sharps, naturals, stems, slurs, ...)
 *
 * @author Hervé Bitteur
 */
public class PatternsStep
    extends AbstractSystemStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(PatternsStep.class);

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // PatternsStep //
    //--------------//
    /**
     * Creates a new PatternsStep object.
     */
    public PatternsStep ()
    {
        super(
            Steps.PATTERNS,
            Level.SHEET_LEVEL,
            Mandatory.MANDATORY,
            Redoable.REDOABLE,
            SYMBOLS_TAB,
            "Specific sheet glyph patterns");
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        Steps.valueOf(Steps.VERTICALS)
             .displayUI(sheet);
        Steps.valueOf(Steps.SYMBOLS)
             .displayUI(sheet);
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
        throws StepException
    {
        // Cleanup system sentences
        system.getSentences()
              .clear();

        // Cleanup system dummy parts
        ScoreSystem scoreSystem = system.getScoreSystem();

        for (Iterator<TreeNode> it = scoreSystem.getParts()
                                                .iterator(); it.hasNext();) {
            SystemPart part = (SystemPart) it.next();

            if (part.isDummy()) {
                it.remove();
            }
        }

        // Iterate
        for (int iter = 1; iter <= constants.MaxPatternsIterations.getValue();
             iter++) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "System#" + system.getId() + " patterns iter #" + iter);
            }

            if (Main.getGui() != null) {
                system.getSheet()
                      .getErrorsEditor()
                      .clearSystem(this, system.getId());
            }

            if (!system.runPatterns()) {
                return; // No more progress made
            }
        }
    }

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Collection<SystemInfo> systems,
                             Sheet                  sheet)
        throws StepException
    {
        // For the very first time, we reperform the VERTICALS step
        if (!sheet.isDone(this)) {
            sheet.done(this);

            // Reperform verticals once
            try {
                Stepping.reprocessSheet(
                    Steps.valueOf("VERTICALS"),
                    sheet,
                    systems,
                    true);
            } catch (Exception ex) {
                logger.warning("Error in re-processing from " + this, ex);
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

        private final Constant.Integer MaxPatternsIterations = new Constant.Integer(
            "count",
            1,
            "Maximum number of iterations for PATTERNS task");
    }
}
