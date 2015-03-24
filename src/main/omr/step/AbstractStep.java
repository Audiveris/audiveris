//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A b s t r a c t S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.Main;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

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
        if (Main.getGui() != null) {
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

    //--------//
    // doStep //
    //--------//
    public void doStep (Step step,
                        Collection<SystemInfo> systems,
                        Sheet sheet)
            throws StepException
    {
        try {
            logger.debug("{}Starting {}", sheet.getLogPrefix(), this);
            started(step, sheet);
            Stepping.notifyStep(sheet, step); // Start

            clearErrors(step, sheet);

            // Reset sheet relevant data
            sheet.reset(step);

            doit(systems, sheet);

            sheet.done(step); // Full completion
            logger.debug("{}Finished {}", sheet.getLogPrefix(), step);
        } catch (Throwable ex) {
            logger.warn("doStep error in " + this, ex);
        } finally {
            // Make sure we reset the sheet "current" step, always.
            if (sheet != null) {
                sheet.setCurrentStep(null);
                Stepping.notifyStep(sheet, step); // Stop
            }
        }
    }

    //
    //    //------//
    //    // done //
    //    //------//
    //
    //    public void done (Sheet sheet)
    //    {
    //        sheet.done(this);
    //    }
    //
    //    //----------------//
    //    // getDescription //
    //    //----------------//
    //
    //    public String getDescription ()
    //    {
    //        return description;
    //    }
    //
    //    //---------//
    //    // getName //
    //    //---------//
    //
    //    public String getName ()
    //    {
    //        return name;
    //    }
    //
    //    //--------//
    //    // getTab //
    //    //--------//
    //
    //    public String getTab ()
    //    {
    //        return label;
    //    }
    //
    //    //--------//
    //    // isDone //
    //    //--------//
    //
    //    public boolean isDone (Sheet sheet)
    //    {
    //        return sheet.isDone(this);
    //    }
    //---------//
    // started //
    //---------//
    /**
     * Flag this step as started
     */
    public void started (Step step,
                         Sheet sheet)
    {
        sheet.setCurrentStep(step);
    }

    //    //--------------//
    //    // toLongString //
    //    //--------------//
    //
    //    public String toLongString ()
    //    {
    //        StringBuilder sb = new StringBuilder("{Step");
    //        sb.append(" ").append(name);
    //        sb.append(" label:").append(label);
    //        sb.append("}");
    //
    //        return sb.toString();
    //    }
    //
    //    //----------//
    //    // toString //
    //    //----------//
    //
    //    public String toString ()
    //    {
    //        return name;
    //    }
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
    protected abstract void doit (Collection<SystemInfo> systems,
                                  Sheet sheet)
            throws StepException;
}
