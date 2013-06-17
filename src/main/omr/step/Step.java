//----------------------------------------------------------------------------//
//                                                                            //
//                                  S t e p                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Interface {@code Step} describes a sheet processing step.
 *
 * <p>Implementation note: {@code Step} is no longer an enum type, to allow a
 * better decoupling between code parts of the application, since all steps
 * no longer need to be available at the same build time. To some extent,
 * different steps could be provided by separate modules.
 *
 * @author Hervé Bitteur
 */
public interface Step
{
    //~ Static fields/initializers ---------------------------------------------

    /** Labels for view in tabbed panel */
    public static final String PICTURE_TAB = "Picture";

    public static final String DATA_TAB = "Data";

    //~ Enumerations -----------------------------------------------------------
    public enum Mandatory
    {
        //~ Enumeration constant initializers ----------------------------------

        /** Must be performed before any output */
        MANDATORY,
        /** Non mandatory */
        OPTIONAL;

    }

    public enum Level
    {
        //~ Enumeration constant initializers ----------------------------------

        /** Step makes sense at score level only */
        SCORE_LEVEL,
        /** The step can be
         * performed at sheet level */
        SHEET_LEVEL;

    }

    //~ Methods ----------------------------------------------------------------
    //
    /**
     * Clear the errors that relate to this step on the provided sheet.
     *
     * @param sheet the sheet to work upon
     */
    public void clearErrors (Sheet sheet);

    /**
     * Make the related user interface visible for this step.
     *
     * @param sheet the sheet to work upon
     */
    public void displayUI (Sheet sheet);

    /**
     * Run the step and mark it as started then done
     *
     * @param systems systems to process (null means all systems)
     * @param sheet   the sheet to work upon
     * @throws StepException if processing had to stop at this step
     */
    public void doStep (Collection<SystemInfo> systems,
                        Sheet sheet)
            throws StepException;

    /**
     * Flag this step as done for the provided sheet.
     *
     * @param sheet the sheet to work upon
     */
    public void done (Sheet sheet);

    /**
     * Report a description of the step.
     *
     * @return a short description
     */
    public String getDescription ();

    /**
     * Name of the step.
     *
     * @return the name of the step
     */
    public String getName ();

    /**
     * Related assembly view tab, selected when steps completes
     *
     * @return the related view tab
     */
    public String getTab ();

    /**
     * Check whether this step has been done for the specified sheet.
     *
     * @return true if started/done, false otherwise
     */
    public boolean isDone (Sheet sheet);

    /**
     * Is the step mandatory?.
     *
     * @return true for mandatory
     */
    public boolean isMandatory ();

    /**
     * Does the step need to be performed at score level only?
     *
     * @return true for score-level step, false for sheet-level step
     */
    public boolean isScoreLevel ();

    /**
     * A detailed description.
     *
     * @return a tip for the step
     */
    public String toLongString ();
}
