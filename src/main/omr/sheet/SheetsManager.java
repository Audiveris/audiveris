//----------------------------------------------------------------------------//
//                                                                            //
//                         S h e e t s M a n a g e r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.log.Logger;

import omr.script.ScriptActions;

import omr.sheet.ui.SheetsController;

import omr.util.Dumper;
import omr.util.Memory;
import omr.util.NameSet;

import java.util.*;

/**
 * Class <code>SheetsManager</code> handles the set of sheet instances in
 * memory as well as the related history.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SheetsManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetsManager.class);

    /** The single instance of this class */
    private static SheetsManager INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** Instances of sheet */
    private List<Sheet> instances = new ArrayList<Sheet>();

    /** Sheet file history */
    private NameSet history;

    /** The UI controller, if any */
    private SheetsController controller;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // SheetsManager //
    //---------------//
    /**
     * Creates a SheetsManager.
     */
    private SheetsManager ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // setController //
    //---------------//
    public void setController (SheetsController controller)
    {
        this.controller = controller;
    }

    //------------//
    // getHistory //
    //------------//
    /**
     * Get access to the list of previously handled sheets
     *
     * @return the history set of sheet files
     */
    public NameSet getHistory ()
    {
        if (history == null) {
            history = new NameSet("omr.sheet.Sheet.history", 10);
        }

        return history;
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class,
     *
     * @return the single instance
     */
    public static SheetsManager getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new SheetsManager();
        }

        return INSTANCE;
    }

    //---------------------//
    // areAllScriptsStored //
    //---------------------//
    /**
     * Report whether all the sheet scripts have been stored
     * @return true if OK
     */
    public boolean areAllScriptsStored ()
    {
        for (Sheet sheet : instances) {
            if (!ScriptActions.checkStored(sheet.getScript())) {
                return false;
            }
        }

        return true;
    }

    //-------//
    // close //
    //-------//
    /**
     * Close a sheet instance
     */
    public void close (Sheet sheet)
    {
        if (logger.isFineEnabled()) {
            logger.fine("close " + sheet);
        }

        // Remove from list of instances
        if (instances.contains(sheet)) {
            instances.remove(sheet);
        }

        // Remove from user selection if any
        if (controller != null) {
            controller.close(sheet);
        }

        // Suggestion to run the garbage collector
        Memory.gc();
    }

    //----------//
    // closeAll //
    //----------//
    /**
     * Close all the sheet instances, with their views if any
     */
    public void closeAll ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("closeAll");
        }

        for (Iterator<Sheet> it = instances.iterator(); it.hasNext();) {
            Sheet sheet = it.next();
            it.remove(); // Done here to avoid concurrent modification
            sheet.close();
        }
    }

    //---------------//
    // dumpAllSheets //
    //---------------//
    /**
     * Dump all sheet instances
     */
    public void dumpAllSheets ()
    {
        java.lang.System.out.println("\n");
        java.lang.System.out.println("* All Sheets *");

        for (Sheet sheet : instances) {
            java.lang.System.out.println(
                "-----------------------------------------------------------------------");
            java.lang.System.out.println(sheet.toString());
            Dumper.dump(sheet);
        }

        java.lang.System.out.println(
            "-----------------------------------------------------------------------");
        logger.info(instances.size() + " sheet(s) dumped");
    }

    //----------------//
    // insertInstance //
    //----------------//
    /**
     * Insert this new sheet in the set of sheet instances
     *
     * @param sheet the sheet to insert
     */
    public void insertInstance (Sheet sheet)
    {
        if (logger.isFineEnabled()) {
            logger.fine("insertInstance " + sheet);
        }

        // Remove duplicate if any
        for (Iterator<Sheet> it = instances.iterator(); it.hasNext();) {
            Sheet s = it.next();

            if (s.getPath()
                 .equals(sheet.getPath())) {
                if (logger.isFineEnabled()) {
                    logger.fine("Removing duplicate " + s);
                }

                it.remove();
                s.close();

                break;
            }
        }

        // Insert new sheet instances
        instances.add(sheet);

        // Insert in sheet history
        getHistory()
            .add(sheet.getPath());
    }
}
