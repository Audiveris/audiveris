//----------------------------------------------------------------------------//
//                                                                            //
//                                S c r i p t                                 //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.script;

import omr.sheet.Sheet;

import omr.step.StepException;

import omr.util.Logger;

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
        , @XmlElement(name = "deassign", type = DeassignTask.class)
        , @XmlElement(name = "segment", type = SegmentTask.class)
    })
    private final List<Task> tasks = new ArrayList<Task>();

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

    //---------//
    // addTask //
    //---------//
    /**
     * Add a task to the script
     *
     * @param task the task to add at the end of the current sequence
     */
    public void addTask (Task task)
    {
        tasks.add(task);
        logger.info("Script: added task " + task);
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

        for (Task task : tasks) {
            System.out.println(task.toString());
        }
    }

    //-----//
    // run //
    //-----//
    public void run ()
    {
        logger.info("Running " + this);

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
            for (Task task : tasks) {
                task.run(sheet);
            }

            // Kludge, to put this tab on top of all others. TBD
            sheet.getAssembly()
                 .selectTab("Glyphs");
        } catch (StepException ex) {
            logger.warning("Task aborted", ex);
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
