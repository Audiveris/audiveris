//----------------------------------------------------------------------------//
//                                                                            //
//                                S c r i p t                                 //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.log.Logger;

import omr.sheet.Sheet;

import omr.step.StepException;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * Class <code>Script</code> handles a complete script applied to a sheet. A
 * script is a sequence of tasks. A script can be recorded as the user interacts
 * with the sheet data. It can be stored, reloaded and replayed.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "script")
public class Script
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Script.class);

    //~ Instance fields --------------------------------------------------------

    /** Sheet to which the script is applied */
    private Sheet sheet;

    /** Full path to the Sheet file name */
    @XmlAttribute(name = "sheet")
    private final String sheetPath;

    /** Sequence of tasks that compose the script */
    @XmlElements({@XmlElement(name = "step", type = StepTask.class)
        , @XmlElement(name = "assign", type = AssignTask.class)
        , @XmlElement(name = "section-assign", type = SectionAssignTask.class)
        , @XmlElement(name = "deassign", type = DeassignTask.class)
        , @XmlElement(name = "segment", type = SegmentTask.class)
        , @XmlElement(name = "export", type = ExportTask.class)
        , @XmlElement(name = "midi", type = MidiWriteTask.class)
        , @XmlElement(name = "play", type = PlayTask.class)
        , @XmlElement(name = "slur", type = SlurTask.class)
        , @XmlElement(name = "text", type = TextTask.class)
        , @XmlElement(name = "boundary", type = BoundaryTask.class)
    })
    private final List<ScriptTask> tasks = new ArrayList<ScriptTask>();

    /** Nb of tasks when stored, if any */
    private int storedTasksNb = 0;

    //~ Constructors -----------------------------------------------------------

    //--------//
    // Script //
    //--------//
    /**
     * Create a script
     *
     * @param sheet the related sheet
     */
    public Script (Sheet sheet)
    {
        this.sheet = sheet;
        sheetPath = sheet.getPath();
    }

    //--------//
    // Script //
    //--------//
    /** No-arg constructor for JAXB */
    private Script ()
    {
        sheetPath = null;
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the sheet this script is linked to
     *
     * @return the sheet concerned
     */
    public Sheet getSheet ()
    {
        return sheet;
    }

    //-----------//
    // setStored //
    //-----------//
    /**
     * Flag the script as being currently consistent with its backup
     */
    public void setStored ()
    {
        storedTasksNb = tasks.size();
    }

    //----------//
    // isStored //
    //----------//
    /**
     * Check whether the script is consistent with its backup on disk
     *
     * @return true if OK
     */
    public boolean isStored ()
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "tasks.size()=" + tasks.size() + " storedTasksNb=" +
                storedTasksNb);
        }

        return tasks.size() <= storedTasksNb;
    }

    //---------//
    // addTask //
    //---------//
    /**
     * Add a task to the script
     *
     * @param task the task to add at the end of the current sequence
     */
    public void addTask (ScriptTask task)
    {
        tasks.add(task);

        if (logger.isFineEnabled()) {
            logger.fine("Script: added task " + task);
        }
    }

    //------//
    // dump //
    //------//
    /**
     * Meant for debug, dumps the script to the output console
     */
    public void dump ()
    {
        System.out.println();
        System.out.println(toString());

        for (ScriptTask task : tasks) {
            System.out.println(task.toString());
        }
    }

    //-----//
    // run //
    //-----//
    /**
     * This methods runs sequentially and synchronously the various tasks of the
     * script. It is up to the caller to run this method in a separate thread.
     */
    public void run ()
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "Running " + this +
                ((sheet != null) ? (" on sheet " + sheet.getRadix()) : ""));
        }

        // Make sheet concrete
        if (sheet == null) {
            if (sheetPath == null) {
                logger.warning("No sheet defined in script");

                return;
            }

            try {
                sheet = new omr.sheet.Sheet(new java.io.File(sheetPath), false);
            } catch (StepException ex) {
                logger.warning("Cannot load sheet from " + sheetPath, ex);

                return;
            }
        }

        // Run the tasks in sequence
        try {
            for (ScriptTask task : tasks) {
                // Actually run this task
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Launching " + task + " on sheet " + sheet.getRadix());
                }

                task.run(sheet);
            }

            if (logger.isFineEnabled()) {
                logger.fine("All tasks launched on sheet " + sheet.getRadix());
            }
        } catch (Exception ex) {
            logger.warning("Task aborted", ex);
        } finally {
            // Flag the active script as up-to-date
            sheet.getScript()
                 .setStored();
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Script");

        if (sheetPath != null) {
            sb.append(" ")
              .append(sheetPath);
        } else if (sheet != null) {
            sb.append(" ")
              .append(sheet.getRadix());
        }

        if (tasks != null) {
            sb.append(" tasks:")
              .append(tasks.size());
        }

        sb.append("}");

        return sb.toString();
    }
}
