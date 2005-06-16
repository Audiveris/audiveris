//-----------------------------------------------------------------------//
//                                                                       //
//                        S h e e t M a n a g e r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.Step;
import omr.util.Dumper;
import omr.util.Logger;
import omr.util.NameSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>SheetManager</code> handles the list of sheet instances in
 * memory as well as the related history.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SheetManager
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(SheetManager.class);

    // The single instance of this class
    private static SheetManager INSTANCE;

    //~ Instance variables ------------------------------------------------

    // Sheet file history
    private NameSet history;

    // Instances of sheet
    private List<Sheet> instances = new ArrayList<Sheet>();

    // List of steps
    private List<Step> steps;

    //~ Constructors ------------------------------------------------------

    //--------------//
    // SheetManager //
    //--------------//
    /**
     * Creates a Sheet Manager.
     */
    private SheetManager ()
    {
        INSTANCE = this;
    }

    //~ Methods -----------------------------------------------------------

    //-------//
    // close //
    //-------//
    /**
     * Close a sheet instance
     */
    public void close (Sheet sheet)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("close " + sheet);
        }

        // Remove from list of instance
        if (instances.contains(sheet)) {
            instances.remove(sheet);
        }
    }

    //----------//
    // closeAll //
    //----------//
    /**
     * Close all the sheet instances, with their views if any
     */
    public void closeAll ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("closeAll");
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
            java.lang.System.out.println("-----------------------------------------------------------------------");
            java.lang.System.out.println(sheet.toString());
            Dumper.dump(sheet);
        }

        java.lang.System.out.println("-----------------------------------------------------------------------");
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
        if (logger.isDebugEnabled()) {
            logger.debug("insertInstance " + sheet);
        }

        // Remove duplicate if any
        for (Iterator<Sheet> it = instances.iterator(); it.hasNext();) {
            Sheet s = it.next();

            if (s.getPath().equals(sheet.getPath())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removing duplicate " + s);
                }

                it.remove();
                s.close();

                break;
            }
        }

        // Insert new sheet instances
        instances.add(sheet);
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
    public static SheetManager getInstance ()
    {
        if (INSTANCE == null) {
            new SheetManager();
        }
        return INSTANCE;
    }

    //-----------//
    // getSheets //
    //-----------//
    /**
     * Get the collection of sheets currently handled by OMR
     *
     * @return The collection
     */
    public List<Sheet> getSheets ()
    {
        return instances;
    }
}
