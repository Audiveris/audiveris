//----------------------------------------------------------------------------//
//                                                                            //
//                      S h e e t s C o n t r o l l e r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.Score;
import omr.score.entity.Page;

import omr.selection.SelectionService;
import omr.selection.SheetEvent;

import omr.sheet.Sheet;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code SheetsController} is the UI Controller in charge of
 * user interactions with the sheets.
 *
 * <p>Multiple sheets are handled by means of a tabbed pane. For each tab, and
 * thus for each sheet, we have a separate {@link SheetAssembly}. All methods
 * that access these shared entities (tabbedPane, assemblies) are synchronized.
 * </p>
 *
 * <p>This class encapsulates an event service, which publishes the sheet
 * currently selected by a user interface. See {@link #subscribe},
 * {@link #unsubscribe} and {@link #getSelectedSheet}.</p>
 *
 * <p>This class is meant to be a Singleton</p>
 *
 * @author Hervé Bitteur
 */
public class SheetsController
        implements ChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SheetsController.class);

    /** Events that can be published on sheet service */
    private static final Class<?>[] eventsWritten = new Class<?>[]{
        SheetEvent.class};

    /** The single instance of this class */
    private static volatile SheetsController INSTANCE;

    //~ Instance fields --------------------------------------------------------
    /** Ordered sequence of sheet assemblies */
    private final ArrayList<SheetAssembly> assemblies;

    /** The concrete tabbed pane, one tab per sheet */
    private final JTabbedPane tabbedPane;

    /**
     * The global event service dedicated to publication of the
     * currently selected sheet.
     */
    private final SelectionService sheetService = new SelectionService(
            getClass().getSimpleName(),
            eventsWritten);

    //~ Constructors -----------------------------------------------------------
    //------------------//
    // SheetsController //
    //------------------//
    /**
     * Create the SheetsController singleton.
     */
    private SheetsController ()
    {
        tabbedPane = new JTabbedPane();
        assemblies = new ArrayList<>();

        // Listener on sheet tab operations
        tabbedPane.addChangeListener(this);
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // callAboutSheet //
    //----------------//
    /**
     * Call the attention about the provided sheet, by publishing it on
     * the proper event service.
     *
     * @param sheet the provided sheet, which may be null
     */
    public void callAboutSheet (Sheet sheet)
    {
        sheetService.publish(new SheetEvent(this, null, null, sheet));
    }

    //----------------//
    // createAssembly //
    //----------------//
    /**
     * Create the assembly that relates to the specified sheet.
     *
     * @param sheet the sheet to be viewed (sheet cannot be null).
     * @return the created assembly
     */
    public synchronized SheetAssembly createAssembly (Sheet sheet)
    {
        logger.debug("createAssembly {}", sheet.getId());

        // Create the assembly on this sheet
        SheetAssembly assembly = new SheetAssembly(sheet);

        // Initial zoom ratio
        assembly.setZoomRatio(constants.initialZoomRatio.getValue());

        // Make sure the assembly is part of the tabbed pane
        int sheetIndex = tabbedPane.indexOfComponent(assembly.getComponent());

        if (sheetIndex == -1) {
            logger.debug("Adding assembly for sheet {}", sheet.getId());

            // Insert in tabbed pane
            assemblies.add(assembly);

            JComponent comp = assembly.getComponent();
            tabbedPane.addTab(
                    defineTitleFor(sheet),
                    null,
                    comp,
                    sheet.getScore().getImagePath());
        }

        return assembly;
    }

    //-------------------//
    // dumpAllAssemblies //
    //-------------------//
    @Action
    public synchronized void dumpAllAssemblies ()
    {
        for (SheetAssembly assembly : assemblies) {
            logger.info("Assembly of {} {}", assembly.getSheet(), assembly);
        }
    }

    //--------------------------//
    // dumpCurrentSheetServices //
    //--------------------------//
    /**
     * Debug action to dump the current status of all event services
     * related to the selected sheet if any.
     */
    public void dumpCurrentSheetServices ()
    {
        Sheet sheet = getSelectedSheet();
        logger.info("Sheet:{}", sheet);

        if (sheet == null) {
            return;
        }

        sheet.getLocationService().dumpSubscribers();

        if (sheet.getHorizontalLag() != null) {
            sheet.getHorizontalLag().getSectionService().dumpSubscribers();
            sheet.getHorizontalLag().getRunService().dumpSubscribers();
        }

        if (sheet.getVerticalLag() != null) {
            sheet.getVerticalLag().getSectionService().dumpSubscribers();
            sheet.getVerticalLag().getRunService().dumpSubscribers();
        }

        sheet.getNest().getGlyphService().dumpSubscribers();
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the real pane (to insert in proper UI hierarchy).
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return tabbedPane;
    }

    //-----------------//
    // getCurrentSheet //
    //-----------------//
    /**
     * A convenient static method to directly report the currently
     * selected sheet, if any.
     *
     * @return the selected sheet, or null
     */
    public static Sheet getCurrentSheet ()
    {
        return getInstance().getSelectedSheet();
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class.
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

    //------------------//
    // getSelectedSheet //
    //------------------//
    /**
     * Convenient method to directly access currently selected sheet,
     * if any.
     *
     * @return the selected sheet, which may be null (if no sheet is selected)
     */
    public Sheet getSelectedSheet ()
    {
        SheetEvent sheetEvent = (SheetEvent) sheetService.getLastEvent(
                SheetEvent.class);

        return (sheetEvent != null) ? sheetEvent.getData() : null;
    }

    //----------------//
    // removeAssembly //
    //----------------//
    /**
     * Remove the specified view from the tabbed pane.
     *
     * @param sheet the sheet to close
     */
    public synchronized void removeAssembly (Sheet sheet)
    {
        SheetAssembly assembly = sheet.getAssembly();
        int sheetIndex = tabbedPane.indexOfComponent(
                assembly.getComponent());

        if (sheetIndex != -1) {
            logger.debug("Removing assembly {}", sheet);

            // Let others know (if this closing sheet was the current one)
            if (sheet == getSelectedSheet()) {
                callAboutSheet(null);
            }

            // Remove from assemblies
            assemblies.remove(sheetIndex);

            // Remove from tabs
            tabbedPane.remove(sheetIndex);

            Score score = sheet.getScore();

            // Make sure the first sheet of a multipage score is OK
            // We need to modify the tab label for the score (new) first tab
            if (!score.getPages().isEmpty()) {
                Page firstPage = (Page) score.getPages().get(0);
                Sheet firstSheet = firstPage.getSheet();
                int firstIndex = tabbedPane.indexOfComponent(
                        firstSheet.getAssembly().getComponent());

                if (firstIndex != -1) {
                    tabbedPane.setTitleAt(
                            firstIndex,
                            defineTitleFor(firstSheet));
                }
            }
        }
    }

    //--------------//
    // showAssembly //
    //--------------//
    /**
     * Display the assembly that relates to the specified sheet.
     *
     * @param sheet the sheet to be viewed (sheet cannot be null).
     */
    public synchronized void showAssembly (Sheet sheet)
    {
        logger.debug("showAssembly {}", sheet.getId());

        if (sheet != null) {
            SheetAssembly assembly = sheet.getAssembly();

            // Make sure the assembly is part of the tabbed pane
            int sheetIndex = tabbedPane.indexOfComponent(
                    assembly.getComponent());

            if (sheetIndex != -1) {
                tabbedPane.setSelectedIndex(sheetIndex);
            } else {
                logger.warn("No tab found for {}", sheet);
            }
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * This method is called whenever the sheet selection is modified,
     * whether programmatically (by means of {@link #showAssembly})
     * or by user action (manual selection of the sheet tab).
     *
     * <p> Set the state (enabled or disabled) of all menu items that depend on
     * status of current sheet.
     */
    @Override
    public synchronized void stateChanged (ChangeEvent e)
    {
        if (e.getSource() == tabbedPane) {
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
     * Subscribe to the sheet event service (for the SheetEvent class).
     *
     * @param subscriber The subscriber to accept the events when published.
     */
    public void subscribe (EventSubscriber<SheetEvent> subscriber)
    {
        sheetService.subscribeStrongly(SheetEvent.class, subscriber);
    }

    //-------------//
    // unsubscribe //
    //-------------//
    /**
     * Unsubscribe to the sheet event service (for the SheetEvent class).
     *
     * @param subscriber the entity to unsubscribe
     */
    public void unsubscribe (EventSubscriber<SheetEvent> subscriber)
    {
        sheetService.unsubscribe(SheetEvent.class, subscriber);
    }

    //----------------//
    // defineTitleFor //
    //----------------//
    /**
     * Generate proper tab title for the provided sheet.
     *
     * @param sheet the provided sheet instance
     * @return the title to use for the related tab
     */
    private String defineTitleFor (Sheet sheet)
    {
        Page page = sheet.getPage();
        Score score = page.getScore();
        int index = page.getIndex();
        
        if (score.isMultiPage()) {
            if (page == score.getFirstPage()) {
                return score.getRadix() + "#" + index;
            } else {
                return "#" + index;
            }
        } else {
            if (index != 1) {
                return score.getRadix() + "#" + index;
            } else {
                return score.getRadix();
            }
        }
    }

    //------------------//
    // sheetTabSelected //
    //------------------//
    /**
     * Run when a sheetTab has been selected in the tabbedPane.
     *
     * @param sheetIndex the index of the tab
     */
    private void sheetTabSelected (int sheetIndex)
    {
        // Remember the new selected sheet
        SheetAssembly assembly = assemblies.get(sheetIndex);
        Sheet sheet = assembly.getSheet();

        // Tell everyone about the new selected sheet
        callAboutSheet(sheet);

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

        Constant.Ratio initialZoomRatio = new Constant.Ratio(
                0.5,
                "Initial zoom ratio for displayed sheet pictures");

    }
}
