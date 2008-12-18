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

import omr.log.Logger;

import omr.script.ScriptActions;

import omr.selection.GlyphEvent;
import omr.selection.GlyphIdEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.LocationEvent;
import omr.selection.PixelLevelEvent;
import omr.selection.RunEvent;
import omr.selection.ScoreLocationEvent;
import omr.selection.SectionEvent;
import omr.selection.SectionIdEvent;
import omr.selection.SheetEvent;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.util.Dumper;
import omr.util.Memory;
import omr.util.NameSet;

import org.bushe.swing.event.EventService;
import org.bushe.swing.event.ThreadSafeEventService;

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

    /**
     * The global event service dedicated to publication of the currently
     * selected sheet.
     */
    private static final EventService sheetService = new ThreadSafeEventService();

    static {
        // We need a cache of at least one sheet for this service
        sheetService.setDefaultCacheSizePerClassOrTopic(1);
    }

    /** Catalog of really all Eventclasses, meant only for debugging */
    private static final Collection<Class<?extends UserEvent>> allEventClasses = new ArrayList<Class<?extends UserEvent>>();

    static {
        allEventClasses.add(GlyphEvent.class);
        allEventClasses.add(GlyphIdEvent.class);
        allEventClasses.add(GlyphSetEvent.class);
        allEventClasses.add(LocationEvent.class);
        allEventClasses.add(PixelLevelEvent.class);
        allEventClasses.add(RunEvent.class);
        allEventClasses.add(ScoreLocationEvent.class);
        allEventClasses.add(SectionEvent.class);
        allEventClasses.add(SectionIdEvent.class);
        allEventClasses.add(SheetEvent.class);
        allEventClasses.add(SheetLocationEvent.class);
        allEventClasses.add(UserEvent.class);
    }

    /** The single instance of this class */
    private static SheetManager INSTANCE;

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

    //-----------------//
    // getEventService //
    //-----------------//
    /**
     * Convenient method to access sheet selection, and potentially register
     * observer
     *
     * @return the sheet selection
     */
    public static EventService getEventService ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("getSelection called");
        }

        return sheetService;
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
    // setSelectedSheet //
    //------------------//
    /**
     * Convenient method to inform about the selected sheet if any
     * @param sheet the selected sheet, or null
     */
    public static void setSelectedSheet (Sheet sheet)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setSelectedSheet : " + sheet);
        }

        getEventService()
            .publish(new SheetEvent(SheetManager.class, sheet));
    }

    //------------------//
    // getSelectedSheet //
    //------------------//
    /**
     * Convenient method to directly access currently selected sheet if any
     *
     * @return the selected sheet, which may be null (if no sheet is selected)
     */
    public static Sheet getSelectedSheet ()
    {
        SheetEvent sheetEvent = (SheetEvent) sheetService.getLastEvent(
            SheetEvent.class);

        return (sheetEvent != null) ? sheetEvent.getData() : null;
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

    //---------------------//
    // areAllScriptsStored //
    //---------------------//
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

            if (changeListener != null) {
                changeListener.stateChanged(changeEvent);
            }
        }

        // Remove from selection if needed
        if (sheet == getSelectedSheet()) {
            sheetService.publish(new SheetEvent(this, null));
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

    //-------------------//
    // dumpEventServices //
    //-------------------//
    /**
     * Debug action to dump the current status of all event services
     */
    public static void dumpEventServices ()
    {
        Sheet sheet = SheetManager.getSelectedSheet();
        logger.info("Sheet:" + sheet);

        if (sheet == null) {
            return;
        }

        dumpSubscribers("Sheet events", sheet.getEventService());
        dumpSubscribers(
            "hLag events",
            sheet.getHorizontalLag().getEventService());
        dumpSubscribers(
            "vLag events",
            sheet.getVerticalLag().getEventService());
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

    //-----------------//
    // dumpSubscribers //
    //-----------------//
    @SuppressWarnings("unchecked")
    private static void dumpSubscribers (String       title,
                                         EventService service)
    {
        logger.info(title);

        for (Class<?extends UserEvent> eventClass : allEventClasses) {
            List subscribers = service.getSubscribers(eventClass);

            if (subscribers.size() > 0) {
                Object last = service.getLastEvent(eventClass);
                logger.info(
                    "-- " + eventClass.getSimpleName() + ": " +
                    subscribers.size() + ((last != null) ? (" " + last) : ""));

                for (Object obj : subscribers) {
                    logger.info("      " + obj);
                }
            }
        }
    }
}
