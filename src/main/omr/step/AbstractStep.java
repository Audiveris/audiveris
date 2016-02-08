//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A b s t r a c t S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.OMR;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.SheetTab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Abstract class {@code AbstractStep} provides a convenient basis for any {@link Step}
 * implementation.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AbstractStep} object.
     */
    public AbstractStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // clearErrors //
    //-------------//
    public void clearErrors (Step step,
                             Sheet sheet)
    {
        if (OMR.gui != null) {
            sheet.getErrorsEditor().clearStep(step);
        }
    }

    //-----------//
    // displayUI //
    //-----------//
    public void displayUI (Step step,
                           Sheet sheet)
    {
        // Void by default
    }

    //------//
    // doit //
    //------//
    /**
     * Actually perform the step.
     * This method must be defined for any concrete Step.
     *
     * @param systems the collection of systems to process, or null
     * @param sheet   the related sheet
     * @throws StepException raised if processing failed
     */
    public abstract void doit (Collection<SystemInfo> systems,
                               Sheet sheet)
            throws StepException;

    //-------------//
    // getSheetTab //
    //-------------//
    /**
     * Report the related assembly view tab, selected when step completes
     *
     * @return the related tab
     */
    public SheetTab getSheetTab ()
    {
        return SheetTab.DATA_TAB;
    }
}
