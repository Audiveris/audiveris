//-----------------------------------------------------------------------//
//                                                                       //
//                     S h e e t C o n t r o l l e r                     //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.Main;
import omr.Step;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.score.PagePoint;
import omr.score.Score;
import omr.ui.Jui;
import omr.ui.SheetAssembly;
import omr.ui.icon.IconManager;
import omr.ui.util.FileFilter;
import omr.ui.util.UIUtilities;
import omr.util.Logger;

import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>SheetController</code> encapsulates the display of (possibly
 * several) sheet(s).
 *
 * <p>Multiple sheets are handled by means of a tabbed pane. For each tab,
 * and thus for each sheet, we have a separate {@link SheetAssembly}.
 *
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SheetController
    implements ChangeListener
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(SheetController.class);

    /** Flag a tab index that is not yet available */
    public static final int DIFFERED_INDEX = -2;

    //~ Instance variables ------------------------------------------------

    // The concrete tabbed pane
    private final JTabbedPane component;

    // Ordered list of sheet assemblies
    private final ArrayList<SheetAssembly> assemblies;

    // Ref of toolbar where buttons are inserted
    private final JToolBar toolBar;

    // General purpose toggle
    private final JButton  toggleButton;

    // Menu dedicated to sheet-related actions
    private final JMenu menu = new JMenu("Sheet");

    // Collection of sheet-dependent actions
    private final Collection<AbstractAction> sheetDependentActions
        = new ArrayList<AbstractAction>();

    // Should we synchronize the (score) pane on the other side ?
    private boolean synchroWanted = true;

    //~ Constructors ------------------------------------------------------

    //-----------//
    // SheetController //
    //-----------//
    /**
     * Create the SheetController, within the jui frame.
     *
     *
     * @param jui     the enclosing jui entity
     * @param toolBar the jui tool bar
     */
    public SheetController (Jui jui,
                      JToolBar toolBar)
    {
        component = new JTabbedPane();
        assemblies = new ArrayList<SheetAssembly>();

        this.toolBar = toolBar;

        toolBar.addSeparator();

        // History menu
        JMenuItem historyMenu = SheetManager.getInstance().getHistory()
                .menu("History",
                      new HistoryListener());
        historyMenu.setToolTipText("List of previous sheet files");
        historyMenu.setIcon(IconManager.buttonIconOf("general/History"));
        menu.add(historyMenu);
        menu.addSeparator();

        // Various actions
        new SelectSheetAction();
        new CloseAction();
        new ZoomWidthAction();
        new ZoomHeightAction();

        // Toggle button
        toggleButton = new JButton
            (IconManager.buttonIconOf("general/Refresh"));
        toolBar.add(toggleButton);
        toggleButton.setBorder(UIUtilities.getToolBorder());
        toggleButton.setEnabled(false);

        menu.addSeparator();
        new DumpAllAction();

        // Tool actions
        menu.addSeparator();

        new ScalePlotAction();
        new SkewPlotAction();
        new LinePlotAction();

        // Listener on tab operations
        component.addChangeListener(this);

        // Initially disabled actions
        UIUtilities.enableActions(sheetDependentActions, false);
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the real pane
     *
     * @return the concrete component
     */
    public JComponent getComponent()
    {
        return component;
    }

    //---------------------//
    // getSelectedAssembly //
    //---------------------//
    /**
     * Report the current sheet assembly
     *
     * @return the sheet assembly currently selected
     */
    public SheetAssembly getSelectedAssembly()
    {
        int index = component.getSelectedIndex();
        if (index != -1) {
            return assemblies.get(index);
        } else {
            return null;
        }
    }

    //-----------------//
    // getCurrentSheet //
    //-----------------//
    /**
     * Report the currently processed sheet
     *
     * @return the current sheet, or null otherwise
     */
    public Sheet getCurrentSheet ()
    {
        SheetAssembly assembly = getSelectedAssembly();

        if (assembly != null) {
            return assembly.getSheet();
        } else {
            return null;
        }
    }

    //-----------------//
    // getToggleButton //
    //-----------------//
    /**
     * Give access to the button where display of specific entities can be
     * toggled
     *
     * @return the toggle button
     */
    public JButton getToggleButton()
    {
        return toggleButton;
    }

    //-------------------//
    // setScoreSheetView //
    //-------------------//
    /**
     * Preset (before info is actually shown by another explicit order
     * showSheetView) the view parameters for the sheet that relates to the
     * provided score. This method is called from the score side, to prepare
     * the display of the counter part on sheet side.
     *
     * @param score the score for which related sheet view is to be
     *              prepared.
     * @param pagPt The desired focus point in the virtual page
     *              coordinates.  If no focus point is explicitely given
     *              (pagPt value is null), then there is no attempt to
     *              actually load the related sheet if it is not yet
     *              loaded. So, this may end up is just preparing a blank
     *              tab, when we have no sheet information available, and
     *              no explicit desire to load and show the sheet.
     *
     * @return The index in the tabbed pane of the prepared view, so that
     *         the following showSheetView can point directly to this
     *         index. Since sheet loading is performed asynchronously, a
     *         special index value (DIFFERED_INDEX) is used to signal that
     *         the tab index is not yet known.
     */
//     public int setScoreSheetView (Score score,
//                                   PagePoint pagPt)
//     {
//         if (logger.isFineEnabled()) {
//             logger.debug("setScoreSheetView " + score + " pagPt=" + pagPt);
//         }

//         // Remember that user is interested in seeing this sheet
//         Main.getJui().setTarget(score.getImageFPath());

//         Sheet sheet = score.getSheet();

//         if (sheet == null) {
//             // Simply post the asynchronous loading of the sheet image
//             Step.getLoadStep().perform(null, new File(score.getImageFPath()));

//             return DIFFERED_INDEX;
//         } else {
//             // Prepare the view parameters immediately
//             return setSheetView(sheet, pagPt);
//         }
//     }

    //------------------//
    // setSheetAssembly //
    //------------------//
    /**
     * Prepare the parameters of the assembly that relates to the specified
     * sheet.  This method is called by setScoreSheetView() above for an
     * immediate preparation and by Sheet constructor for a differed
     * preparation (when the sheet has finished loading).
     *
     * @param sheet the sheet to be viewed. If no sheet is provided, then
     *              an empty assembly is desired.
     * @param pagPt the eventual focus point desired, null otherwise
     *
     * @return the index in the tabbed pane of the prepared assembly
     */
    public int setSheetAssembly (Sheet sheet,
                                 PagePoint pagPt)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setSheetAssembly for sheet " + sheet.getName() +
                         " pagPt=" + pagPt);
        }

        if (sheet != null) {
            // Make sure that scale and skew info is available for the
            // sheet
            Score score = sheet.getScore();

            if (score != null) {
                sheet.checkScaleAndSkew(score);
            }

            // Make sure we have a assembly on this sheet
            SheetAssembly assembly = sheet.getAssembly();

            if (assembly == null) {
                // Build a brand new display on this sheet
                assembly = new SheetAssembly(sheet);

                // Initial zoom ratio
                assembly.setZoomRatio(constants.initialZoomRatio.getValue());
            }

            // Make sure the assembly is part of the tabbed pane
            int index = component.indexOfComponent
                (assembly.getComponent());

            if (index == -1) {
                if (logger.isFineEnabled()) {
                    logger.fine("Adding assembly for sheet " +
                                 sheet.getName());
                }

                // Insert in tabbed pane
                assemblies.add(assembly);
                component.addTab
                    (sheet.getName(), null, assembly.getComponent(),
                     sheet.getPath());
                index = component.indexOfComponent(assembly.getComponent());
            }

            // Focus info
            if (pagPt == null) {
                if (logger.isFineEnabled()) {
                    logger.fine("No focus specified on Sheet side");
                }

//                 // Does the score view has a defined focus ?
//                 if (score != null) {
//                     ScoreView scoreView = score.getView();

//                     if (scoreView != null) {
//                         pagPt = scoreView.getFocus();
//                     }
//                 }
            }

//            if (pagPt != null) {
                // Use this focus information in the assembly
                //                 assembly.setFocus(pagPt);
//            }

            return index;
        } else {
            return -1; // Index of the empty tab
        }
    }

    //-------//
    // close //
    //-------//
    /**
     * Remove the specified view from the tabbed pane
     *
     * @param assembly the sheet assembly to close
     */
    public void close (SheetAssembly assembly)
    {
        if (logger.isFineEnabled()) {
            logger.fine("close " + assembly.toString());
        }
        int index = component.indexOfComponent
            (assembly.getComponent());

        if (index != -1) {
            // Remove from assemblies
            assemblies.remove(index);
            // Remove from tabs
            component.remove(index);
        }
    }

    //---------------//
    // showSheetView //
    //---------------//
    /**
     * Make the (preset) SheetView actually visible.
     *
     * @param index the index to the SheetView to be made current, with its
     *                potential focus point, or -1 to show a blank panel.
     * @param synchro specify whether other side is to be shown also
     */
    public void showSheetView (int index,
                               boolean synchro)
    {
        setSynchroWanted(synchro);
        component.setSelectedIndex(index);
        setSynchroWanted(true);
    }

//     //---------------//
//     // showSheetView //
//     //---------------//
//     /**
//      * Make the (preset) SheetView actually visible.
//      *
//      * @param index the index to the SheetView to be made current, with its
//      *              potential focus point, or -1 to show a blank panel.
//      */
//     public void showSheetView (int index)
//     {
//         showSheetView(index, false);
//     }

//     //---------------//
//     // showSheetView //
//     //---------------//
//     /**
//      * Make the (preset) SheetView actually visible.
//      *
//      * @param view the SheetView to show, or null
//      */
//     public void showSheetView (SheetView view)
//     {
//         showSheetView(indexOfComponent(view), false);
//     }

    //---------//
    // getMenu //
    //---------//
    /**
     * Report the menu dedicated to sheet handling.
     *
     * @return the sheet menu
     */
    public JMenu getMenu ()
    {
        return menu;
    }

    //------------------//
    // setSynchroWanted //
    //------------------//
    /**
     * Allow to register the need (or lack of) for synchronization of the
     * other side (score view).
     *
     * @param synchroWanted the value to set to the flag
     */
    private synchronized void setSynchroWanted (boolean synchroWanted)
    {
        this.synchroWanted = synchroWanted;
    }

    //-----------------//
    // isSynchroWanted //
    //-----------------//
    /**
     * Check if synchronization of the other side view (score) is
     * wanted. This is usually true, except in specific case, where the
     * initial order already comes from the other side, so there is no need
     * to go back there.
     *
     * @return the flag value
     */
    private boolean isSynchroWanted ()
    {
        return synchroWanted;
    }

    //-------------//
    // selectSheet //
    //-------------//
    /**
     * User dialog, to allow the selection and load of a sheet image file.
     */
    private void selectSheet ()
    {
        // Let the user select a sheet file
        final JFileChooser fc
                = new JFileChooser(constants.initImgDir.getValue());
        fc.addChoosableFileFilter(new FileFilter("Major image files",
                                                 new String[]{
                                                     ".bmp",
                                                     ".gif",
                                                     ".jpg",
                                                     ".png",
                                                     ".tif"
                                                 }));

        if (fc.showOpenDialog(component) == JFileChooser.APPROVE_OPTION) {
            final File file = fc.getSelectedFile();

            if (file.exists()) {
                // Register that as a user target
                try {
                    Main.getJui().setTarget(file.getCanonicalPath());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                // Actually load the sheet picture
                Step.getLoadStep().perform(null, file);

                // Remember (even across runs) the parent directory
                constants.initImgDir.setValue(file.getParent());
            } else {
                logger.warning("File not found " + file);
            }
        }
    }

//     //---------------//
//     // showSheetView //
//     //---------------//
//     /**
//      * Make the (preset) SheetView actually visible.
//      *
//      * @param view    the SheetView to show, or null
//      * @param synchro specify whether other side is to be shown also
//      */
//     private void showSheetView (SheetView view,
//                                 boolean synchro)
//     {
//         showSheetView(indexOfComponent(view), synchro);
//     }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * Set the state (enabled or disabled) of all menu items that depend on
     * status of current sheet. UI frame title is updated accordingly.
     * This method is called whenever the tab selection is modified,
     * whether it's programmatically (by means of setSheetView) of by user
     * action (manual selection of the tab).
     */
    public void stateChanged (ChangeEvent e)
    {
        // Enable actions ?
        Sheet sheet = getCurrentSheet();

        if (logger.isFineEnabled()) {
            logger.fine("stateChanged for " + sheet);
        }

        UIUtilities.enableActions(sheetDependentActions, sheet != null);

//         // Synchronize the score side ?
//         SheetView view = (SheetView) getSelectedComponent();

//         if ((view != null) && isSynchroWanted()) {
//             view.showRelatedScore();
//         }

        // Make UI frame title consistent
        Main.getJui().updateTitle();

        // Tell the selected assembly that it now has the focus...
        SheetAssembly assembly = getSelectedAssembly();
        if (assembly != null) {
            assembly.assemblySelected();
        }
    }

    //~ Classes -----------------------------------------------------------

    //-----------------//
    // HistoryListener //
    //-----------------//
    /**
     * Class <code>HistoryListener</code> is used to reload a sheet file,
     * when selected from the history of previous sheets.
     */
    private static class HistoryListener
        implements ActionListener
    {
        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            String fileName = e.getActionCommand();

            if (logger.isFineEnabled()) {
                logger.fine("HistoryListener for " + fileName);
            }

            Main.getJui().setTarget(fileName);
            Step.getLoadStep().perform(null, new File(fileName));

            if (logger.isFineEnabled()) {
                logger.fine("End of HistoryListener");
            }

            // Other UI actions will be triggered when the sheet has
            // finished loading
        }
    }

    //-------------//
    // SheetAction //
    //-------------//
    /**
     * Class <code>SheetAction</code> is a template for any sheet-related
     * action : it builds the action, registers it in the list of
     * sheet-dependent actions if needed, inserts the action in the sheet
     * menu, and inserts a button in the toolbar if an icon is provided.
     */
    private abstract class SheetAction
            extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public SheetAction (boolean enabled,
                            String  label,
                            String  tip,
                            Icon    icon,
                            boolean onToolBar)
        {
            super(label, icon);

            // Sheet-dependent action ?
            if (!enabled) {
                sheetDependentActions.add(this);
            }

            // Menu item
            menu.add(this).setToolTipText(tip);

            // Tool bar
            if (onToolBar) {
                final JButton button = toolBar.add(this);
                button.setBorder(UIUtilities.getToolBorder());
                button.setToolTipText(tip);
            }
        }
    }

    //-------------//
    // CloseAction //
    //-------------//
    /**
     * Class <code>CloseAction</code> handles the closing of the currently
     * selected sheet.
     */
    private class CloseAction
            extends SheetAction
    {
        //~ Constructors --------------------------------------------------

        public CloseAction ()
        {
            super(false, "Close Sheet", "Close the current sheet",
                  IconManager.buttonIconOf("general/Remove"), true);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            Sheet sheet = getCurrentSheet();
            if (sheet != null) {
                Score score = sheet.getScore();
                if (score != null) {
                    score.close();
                }
                sheet.close();
            }
        }
    }

    //-------------------//
    // SelectSheetAction //
    //-------------------//
    /**
     * Class <code>SelectSheetAction</code> let the user select a sheet
     * file interactively.
     */
    private class SelectSheetAction
            extends SheetAction
    {
        //~ Constructors --------------------------------------------------

        public SelectSheetAction ()
        {
            super(true, "Open Sheet", "Open a sheet file",
                  IconManager.buttonIconOf("general/Open"), true);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            selectSheet();
        }
    }

    //------------------//
    // ZoomHeightAction //
    //------------------//
    /**
     * Class <code>ZoomHeightAction</code> allows to adjust the display
     * zoom, so that the full height is shown.
     */
    private class ZoomHeightAction
            extends SheetAction
    {
        //~ Constructors --------------------------------------------------

        public ZoomHeightAction ()
        {
            super(false, "Height Fit", "Fit image to window height",
                  IconManager.buttonIconOf("general/AlignJustifyVertical"), true);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            SheetAssembly assembly = getSelectedAssembly();
            assembly.getSelectedView().fitHeight();
        }
    }

    //-----------------//
    // ZoomWidthAction //
    //-----------------//
    /**
     * Class <code>ZoomWidthAction</code> allows to adjust the display
     * zoom, so that the full width is shown.
     */
    private class ZoomWidthAction
            extends SheetAction
    {
        //~ Constructors --------------------------------------------------

        public ZoomWidthAction ()
        {
            super(false, "Width Fit", "Fit image to window width",
                  IconManager.buttonIconOf("general/AlignJustifyHorizontal"), true);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            SheetAssembly assembly = getSelectedAssembly();
            assembly.getSelectedView().fitWidth();
        }
    }

    //-----------------//
    // ScalePlotAction //
    //-----------------//
    /**
     * Class <code>ScalePlotAction</code> allows to display the plot of
     * Scale Builder.
     */
    private class ScalePlotAction
        extends SheetAction
    {
        //~ Constructors --------------------------------------------------

        public ScalePlotAction ()
        {
            super(false, "Scale Plot", "Display chart from Scale builder",
                  IconManager.buttonIconOf("general/PrintPreview"), false);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            Sheet sheet = getCurrentSheet();
            if (sheet != null) {
                sheet.getScale().displayChart();
            }
        }
    }

    //----------------//
    // SkewPlotAction //
    //----------------//
    /**
     * Class <code>SkewPlotAction</code> allows to display the plot of
     * Skew Builder.
     */
    private class SkewPlotAction
        extends SheetAction
    {
        //~ Constructors --------------------------------------------------

        public SkewPlotAction ()
        {
            super(false, "Skew Plot", "Display chart from Skew builder",
                  IconManager.buttonIconOf("general/PrintPreview"), false);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            Sheet sheet = getCurrentSheet();
            if (sheet != null) {
                if (sheet.getSkewBuilder() != null) {
                    sheet.getSkewBuilder().displayChart();
                } else {
                    logger.warning("Data from skew builder" +
                                   " is not available");
                }
            }
        }
    }

    //-----------------//
    // LinePlotAction //
    //-----------------//
    /**
     * Class <code>LinePlotAction</code> allows to display the plot of
     * Line Builder.
     */
    private class LinePlotAction
        extends SheetAction
    {
        //~ Constructors --------------------------------------------------

        public LinePlotAction ()
        {
            super(false, "Line Plot", "Display chart from Line builder",
                  IconManager.buttonIconOf("general/PrintPreview"), false);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            Sheet sheet = getCurrentSheet();
            if (sheet != null) {
                if (sheet.getLinesBuilder() != null) {
                    sheet.getLinesBuilder().displayChart();
                } else {
                    logger.warning("Data from staff line builder" +
                                   " is not available");
                }
            }
        }
    }

    //---------------//
    // DumpAllAction //
    //---------------//
    private class DumpAllAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public DumpAllAction ()
        {
            super("Dump all sheets", IconManager.buttonIconOf("general/Find"));

            final String tiptext = "Dump all sheet instances";

            menu.add(this).setToolTipText(tiptext);
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            SheetManager.getInstance().dumpAllSheets();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        Constant.String initImgDir = new Constant.String
                ("c:/",
                 "Default directory for selection of image files");

        Constant.Double initialZoomRatio = new Constant.Double
                (1d,
                 "Initial zoom ratio for displayed sheet pictures");

        Constants ()
        {
            initialize();
        }
    }
}
