//----------------------------------------------------------------------------//
//                                                                            //
//                         S h e e t s M a n a g e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.sheet.ui.SheetsController;

import omr.util.Memory;
import omr.util.NameSet;

import org.jdesktop.application.Action;

import java.util.*;

/**
 * Class <code>SheetsManager</code> handles the set of sheet instances in
 * memory as well as the related history.
 *
 * @author Hervé Bitteur
 */
public class SheetsManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetsManager.class);

    /** The single instance of this class */
    private static volatile SheetsManager INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** Instances of sheet */
    private List<Sheet> instances = new ArrayList<Sheet>();

    /** Sheet file history  (filled only when sheets are successfully loaded) */
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

    //--------------------------//
    // setDefaultSheetDirectory //
    //--------------------------//
    /**
     * Remember the directory where sheets should be found
     * @param directory the latest sheet directory
     */
    public void setDefaultSheetDirectory (String directory)
    {
        constants.defaultSheetDirectory.setValue(directory);
    }

    //--------------------------//
    // getDefaultSheetDirectory //
    //--------------------------//
    /**
     * Report the directory where sheets should be found
     * @return the latest sheet directory
     */
    public String getDefaultSheetDirectory ()
    {
        return constants.defaultSheetDirectory.getValue();
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
            history = new NameSet(
                "Sheets History",
                constants.sheetsHistory,
                10);
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

    //-------//
    // close //
    //-------//
    /**
     * Close a sheet instance
     * @param sheet the sheet to close
     * @return true if we have done the closing
     */
    public synchronized boolean close (Sheet sheet)
    {
        if (logger.isFineEnabled()) {
            logger.fine("close " + sheet);
        }

        // Remove from user selection if any
        if (controller != null) {
            if (!controller.close(sheet)) {
                return false;
            }
        }

        // Remove from list of instances
        if (instances.contains(sheet)) {
            instances.remove(sheet);
        }

        // Suggestion to run the garbage collector
        Memory.gc();

        return true;
    }

    //----------------//
    // closeAllSheets //
    //----------------//
    /**
     * Close all sheet instances
     */
    public void closeAllSheets ()
    {
        int count = 0;

        // NB: Use a COPY of instances, to avoid concurrent modification
        for (Sheet sheet : new ArrayList<Sheet>(instances)) {
            sheet.close();
            count++;
        }

        logger.info(count + " sheet(s) closed");
    }

    //---------------//
    // dumpAllSheets //
    //---------------//
    /**
     * Dump all sheet instances
     */
    @Action
    public void dumpAllSheets ()
    {
        java.lang.System.out.println("\n");
        java.lang.System.out.println("* All Sheets *");

        for (Sheet sheet : instances) {
            java.lang.System.out.println(
                "-----------------------------------------------------------------------");
            java.lang.System.out.println(sheet.toString());
            Main.dumping.dump(sheet);
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
    public synchronized void insertInstance (Sheet sheet)
    {
        if (logger.isFineEnabled()) {
            logger.fine("insertInstance " + sheet);
        }

        // Remove duplicate if any
        for (Iterator<Sheet> it = instances.iterator(); it.hasNext();) {
            Sheet  s = it.next();
            String path = s.getPath();

            if (path.equals(sheet.getPath())) {
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
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Backing constant for sheet history */
        Constant.String sheetsHistory = new Constant.String(
            "",
            "History of loaded sheets");

        /** Default directory for selection of sheet image files */
        Constant.String defaultSheetDirectory = new Constant.String(
            "",
            "Default directory for selection of sheet image files");
    }
}
