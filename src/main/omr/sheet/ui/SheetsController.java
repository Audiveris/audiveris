//----------------------------------------------------------------------------//
//                                                                            //
//                      S h e e t s C o n t r o l l e r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.script.ScriptActions;

import omr.selection.SelectionService;
import omr.selection.SheetEvent;

import omr.sheet.Sheet;
import omr.sheet.SheetsManager;

import omr.util.Implement;

import org.bushe.swing.event.EventSubscriber;

import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class <code>SheetsController</code> is the UI Controller, on top of {@link
 * SheetsManager}, in charge of the interactions with the whole set of sheets.
 *
 * <p>Multiple sheets are handled by means of a tabbed pane. For each tab, and
 * thus for each sheet, we have a separate {@link SheetAssembly}.
 *
 * <p>This class encapsulates an event service, which publishes the sheet
 * currently selected by a user interface. See {@link #publish}, {@link
 * #subscribe}, {@link #unsubscribe}, {@link #setSelectedSheet} and {@link
 * #getSelectedSheet}.
 *
 * <p>This class is meant to be a Singleton
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SheetsController
    implements ChangeListener // To be notified of user tab selection

{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        SheetsController.class);

    /** The single instance of this class */
    private static SheetsController INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** Ordered sequence of sheet assemblies */
    private final ArrayList<SheetAssembly> assemblies;

    /** The concrete tabbed pane, one tab per sheet */
    private final JTabbedPane tabbedPane;

    /**
     * The global event service dedicated to publication of the currently
     * selected sheet.
     */
    private final SelectionService sheetSetService;

    //~ Constructors -----------------------------------------------------------

    //--------------------//
    // SheetsController //
    //--------------------//
    /**
     * Create the SheetsController singleton
     */
    private SheetsController ()
    {
        tabbedPane = new JTabbedPane();
        assemblies = new ArrayList<SheetAssembly>();

        // Listener on sheet tab operations
        tabbedPane.addChangeListener(this);

        // We need a cache of at least one sheet for the sheet set service
        sheetSetService = new SelectionService();
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class,
     *
     * @return the single instance
     */
    public static SheetsController getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new SheetsController();
        }

        return INSTANCE;
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the real pane (for insertion in proper UI hierarchy)
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return tabbedPane;
    }

    //------------------//
    // setSelectedSheet //
    //------------------//
    /**
     * Convenient method to inform about the selected sheet if any
     * @param sheet the selected sheet, or null
     */
    public void setSelectedSheet (Sheet sheet)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setSelectedSheet : " + sheet);
        }

        sheetSetService.publish(new SheetEvent(this, sheet));
    }

    //------------------//
    // getSelectedSheet //
    //------------------//
    /**
     * Convenient method to directly access currently selected sheet if any
     *
     * @return the selected sheet, which may be null (if no sheet is selected)
     */
    public Sheet getSelectedSheet ()
    {
        SheetEvent sheetEvent = (SheetEvent) sheetSetService.getLastEvent(
            SheetEvent.class);

        return (sheetEvent != null) ? sheetEvent.getData() : null;
    }

    //---------------//
    // selectedSheet //
    //---------------//
    /**
     * A convenient static method to directly report the currently selected
     * sheet, if any
     * @return the selected sheet, or null
     */
    public static Sheet selectedSheet ()
    {
        return getInstance()
                   .getSelectedSheet();
    }

    //-------//
    // close //
    //-------//
    /**
     * Remove the specified view from the tabbed pane
     *
     * @param sheet the sheet to close
     */
    public void close (Sheet sheet)
    {
        SheetAssembly assembly = sheet.getAssembly();

        if (assembly != null) {
            // Check whether the script has been written correctly
            ScriptActions.checkStored(sheet.getScript());

            int sheetIndex = tabbedPane.indexOfComponent(
                assembly.getComponent());

            if (sheetIndex != -1) {
                if (logger.isFineEnabled()) {
                    logger.fine("closing " + sheet);
                }

                // Remove from assemblies
                assemblies.remove(sheetIndex);
                // Remove from tabs
                tabbedPane.remove(sheetIndex);

                // Forward to SheetsManager
                SheetsManager.getInstance()
                             .close(sheet);

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "closed " + assembly.toString() + " assemblies=" +
                        assemblies);
                }

                // Let others know (if this closing sheet was the current one)
                if (sheet == getSelectedSheet()) {
                    sheetSetService.publish(new SheetEvent(this, null));
                }
            }
        }
    }

    //--------------------------//
    // dumpCurrentSheetServices //
    //--------------------------//
    /**
     * Debug action to dump the current status of all event services related to
     * the selected sheet if any.
     */
    public void dumpCurrentSheetServices ()
    {
        Sheet sheet = getSelectedSheet();
        logger.info("Sheet:" + sheet);

        if (sheet == null) {
            return;
        }

        SelectionService.dumpSubscribers(
            "Sheet events",
            sheet.getSelectionService());

        if (sheet.getHorizontalLag() != null) {
            SelectionService.dumpSubscribers(
                "hLag events",
                sheet.getHorizontalLag().getSelectionService());
        }

        if (sheet.getVerticalLag() != null) {
            SelectionService.dumpSubscribers(
                "vLag events",
                sheet.getVerticalLag().getSelectionService());
        }
    }

    //---------//
    // publish //
    //---------//
    /**
     * Publish on Sheet Set event service
     * @param event the sheet event to publish
     */
    public void publish (SheetEvent event)
    {
        sheetSetService.publish(event);
    }

    //-----------//
    // showSheet //
    //-----------//
    /**
     * Display the assembly that relates to the specified sheet.
     *
     * @param sheet the sheet to be viewed (sheet cannot be null).
     */
    public void showSheet (Sheet sheet)
    {
        if (logger.isFineEnabled()) {
            logger.fine("showSheet " + sheet.getRadix());
        }

        if (sheet != null) {
            // Make sure we have a assembly on this sheet
            SheetAssembly assembly = sheet.getAssembly();

            if (assembly == null) {
                // Build a brand new display on this sheet
                assembly = new SheetAssembly(sheet);

                // Initial zoom ratio
                assembly.setZoomRatio(constants.initialZoomRatio.getValue());
            }

            // Make sure the assembly is part of the tabbed pane
            int sheetIndex = tabbedPane.indexOfComponent(
                assembly.getComponent());

            if (sheetIndex == -1) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Adding assembly for sheet " + sheet.getRadix());
                }

                // Insert in tabbed pane
                assemblies.add(assembly);
                tabbedPane.addTab(
                    sheet.getRadix(),
                    null,
                    assembly.getComponent(),
                    sheet.getPath());
                sheetIndex = tabbedPane.indexOfComponent(
                    assembly.getComponent());
            }

            tabbedPane.setSelectedIndex(sheetIndex);
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * This method is called whenever the sheet selection is modified, whether
     * it's programmatically (by means of setSheetView) of by user action
     * (manual selection of the sheet tab).
     *
     * <p> Set the state (enabled or disabled) of all menu items that depend on
     * status of current sheet.
     */
    @Implement(ChangeListener.class)
    public void stateChanged (ChangeEvent e)
    {
        final Object source = e.getSource();

        if (source == tabbedPane) {
            final int sheetIndex = tabbedPane.getSelectedIndex();

            // User has selected a new sheet tab?
            if (sheetIndex != -1) {
                // Connect the new sheet tab
                sheetTabSelected(sheetIndex);
            }
        }
    }

    //-----------//
    // subscribe //
    //-----------//
    /**
     * Subscribe to the sheet event service (for the SheetEvent class)
     * @param subscriber The subscriber to accept the events when published.
     */
    public void subscribe (EventSubscriber subscriber)
    {
        sheetSetService.subscribeStrongly(SheetEvent.class, subscriber);
    }

    //-------------//
    // unsubscribe //
    //-------------//
    /**
     * Unsubscribe to the sheet event service (for the SheetEvent class)
     * @param subscriber the entity to unsubscribe
     */
    public void unsubscribe (EventSubscriber subscriber)
    {
        sheetSetService.unsubscribe(SheetEvent.class, subscriber);
    }

    //------------------//
    // sheetTabSelected //
    //------------------//
    /**
     * Run when a sheetTab has been selected in the tabbedPane
     * @param sheetIndex the index of the tab
     */
    private void sheetTabSelected (int sheetIndex)
    {
        // Remember the new selected sheet
        SheetAssembly assembly = assemblies.get(sheetIndex);
        Sheet         sheet = assembly.getSheet();

        if (logger.isFineEnabled()) {
            logger.fine(
                "SheetController: sheetTabSelected sheetIndex=" + sheetIndex +
                " sheet=" + sheet);
        }

        // Tell everyone about the new selected sheet
        sheetSetService.publish(new SheetEvent(this, sheet));

        // Tell the selected assembly that it now has the focus...
        assembly.assemblySelected();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Initial zoom ratio for displayed sheet pictures */
        Constant.Ratio initialZoomRatio = new Constant.Ratio(
            1d,
            "Initial zoom ratio for displayed sheet pictures");
    }
}
