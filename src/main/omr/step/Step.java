//----------------------------------------------------------------------------//
//                                                                            //
//                                  S t e p                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
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
    public static final String GRID_TAB = "Grid";
    public static final String SKEW_TAB = "Skew";
    public static final String LINES_TAB = "Lines";
    public static final String HORIZONTALS_TAB = "Horizontals";
    public static final String SYSTEMS_TAB = "Systems";
    public static final String SYMBOLS_TAB = "Symbols";
    public static final String VERTICALS_TAB = "Verticals";

    //~ Enumerations -----------------------------------------------------------

    public enum Mandatory {
        //~ Enumeration constant initializers ----------------------------------


        /** Must be performed before any output */
        MANDATORY,
        /** Non mandatory */
        OPTIONAL;
    }
    public enum Redoable {
        //~ Enumeration constant initializers ----------------------------------


        /** Step can be redone at will */
        REDOABLE,
        /** Step cannot be redone at will (but a previous step may be) */
        NON_REDOABLE;
    }
    public enum Level {
        //~ Enumeration constant initializers ----------------------------------


        /** Step makes sense at score level only */
        SCORE_LEVEL,
        /** The step can be performed at sheet level */
        SHEET_LEVEL;
    }

    //~ Methods ----------------------------------------------------------------

    /** Report a description of the step */
    public String getDescription ();

    /** Check whether this step has been done for the specified sheet */
    public boolean isDone (Sheet sheet);

    /** Is the step mandatory? */
    public boolean isMandatory ();

    /** Name of the step */
    public String getName ();

    /** Is the step repeatable at will? */
    public boolean isRedoable ();

    /** Does the step need to be performed at score level only? */
    public boolean isScoreLevel ();

    /** Related short tab */
    public String getTab ();

    /** Make the related user interface visible for this step */
    public void displayUI (Sheet sheet);

    /**
     * Run the step and mark it as started then done
     * @param systems systems to process (null means all systems)
     * @param sheet the sheet to work upon
     * @throws StepException if processing had to stop at this step
     */
    public void doStep (Collection<SystemInfo> systems,
                        Sheet                  sheet)
        throws StepException;

    /** Flag this step as done */
    public void done (Sheet sheet);

    /** A detailed description */
    public String toLongString ();
}
