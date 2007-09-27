//----------------------------------------------------------------------------//
//                                                                            //
//                          S h e e t M a n a g e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.selection.Selection;

import omr.util.Dumper;
import omr.util.Logger;
import omr.util.NameSet;

import java.util.*;

import javax.swing.event.*;

/**
 * Class <code>SheetManager</code> handles the list of sheet instances in memory
 * as well as the related history.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SheetManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetManager.class);

    /** The single instance of this class */
    private static SheetManager INSTANCE;

    /** Current sheet selection */
    private static final Selection selection = Selection.makeSheetSelection();

    //~ Instance fields --------------------------------------------------------

    /** Instances of sheet */
    private List<Sheet> instances = new ArrayList<Sheet>();

    /** Sheet file history */
    private NameSet history;

    /** Slot for one potential change listener */
    private ChangeListener changeListener;

    /** Unique change event */
    private final ChangeEvent changeEvent;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // SheetManager //
    //--------------//
    /**
     * Creates a Sheet Manager.
     */
    private SheetManager ()
    {
        changeEvent = new ChangeEvent(this);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // setChangeListener //
    //-------------------//
    /**
     * Register one change listener
     *
     * @param changeListener the entity to be notified of any change
     */
    public void setChangeListener (ChangeListener changeListener)
    {
        this.changeListener = changeListener;
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

    //------------------//
    // getSelectedSheet //
    //------------------//
    /**
     * Convenient method to directly access currently selected sheet if any
     *
     * @return the selected sheet, which may be null (if no sheet selected yet)
     */
    public static Sheet getSelectedSheet ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("getSelectedSheet : " + selection.getEntity());
        }

        return (Sheet) selection.getEntity();
    }

    //------------------//
    // setSelectedSheet //
    //------------------//
    /**
     * Convenient method to inform about the selected sheet if any
     *
     * @return the selected sheet, which may be null (if no sheet selected yet)
     */
    public static void setSelectedSheet (Sheet sheet)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setSelectedSheet : " + sheet);
        }

        selection.setEntity(sheet, null);
    }

    //--------------//
    // getSelection //
    //--------------//
    /**
     * Convenient method to access sheet selection, and potentially register
     * observer
     *
     * @return the sheet selection
     */
    public static Selection getSelection ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("getSelection called");
        }

        return selection;
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

            if (changeListener != null) {
                changeListener.stateChanged(changeEvent);
            }
        }

        // Remove from selection if needed
        if (selection.getEntity() == sheet) {
            selection.setEntity(null, null);
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

            if (changeListener != null) {
                changeListener.stateChanged(changeEvent);
            }
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

        if (changeListener != null) {
            changeListener.stateChanged(changeEvent);
        }
    }
}
