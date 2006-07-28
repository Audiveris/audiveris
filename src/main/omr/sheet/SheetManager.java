//-----------------------------------------------------------------------//
//                                                                       //
//                        S h e e t M a n a g e r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.selection.Selection;
import omr.util.Dumper;
import omr.util.Logger;
import omr.util.NameSet;

import java.util.*;

/**
 * Class <code>SheetManager</code> handles the list of sheet instances in
 * memory as well as the related history.
 *
 * @author Herv&eacute; Bitteur
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

    // Current sheet selection
    private final static Selection selection
            = Selection.makeSheetSelection();

    //~ Constructors ------------------------------------------------------

    //--------------//
    // SheetManager //
    //--------------//
    /**
     * Creates a Sheet Manager.
     */
    private SheetManager ()
    {
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
        if (logger.isFineEnabled()) {
            logger.fine("close " + sheet);
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
        if (logger.isFineEnabled()) {
            logger.fine("insertInstance " + sheet);
        }

        // Remove duplicate if any
        for (Iterator<Sheet> it = instances.iterator(); it.hasNext();) {
            Sheet s = it.next();

            if (s.getPath().equals(sheet.getPath())) {
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
            INSTANCE = new SheetManager();
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

    //--------------//
    // getSelection //
    //--------------//
    /**
     * Convenient method to access sheet selection, and potentially
     * register observer
     *
     * @return the sheet selection
     */
    public static Selection getSelection()
    {
        if (logger.isFineEnabled()) {
            logger.fine("getSelection called");
        }

        return selection;
    }

    //------------------//
    // getSelectedSheet //
    //------------------//
    /**
     * Convenient method to directly access currently selected sheet if any
     *
     * @return the selected sheet, which may be null (case of no
     * sheet selected yet)
     */
    public static Sheet getSelectedSheet()
    {
        if (logger.isFineEnabled()) {
            logger.fine("getSelectedSheet : " + selection.getEntity());
        }

        return (Sheet) selection.getEntity();
    }
}
