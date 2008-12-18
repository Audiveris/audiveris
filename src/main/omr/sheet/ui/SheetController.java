//----------------------------------------------------------------------------//
//                                                                            //
//                       S h e e t C o n t r o l l e r                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.script.ScriptActions;

import omr.selection.SheetEvent;

import omr.sheet.*;

import omr.util.Implement;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>SheetController</code> encapsulates the display of (possibly
 * several) sheet(s). It is the UI on top of SheetManager.
 *
 * <p>Multiple sheets are handled by means of a tabbed pane. For each tab, and
 * thus for each sheet, we have a separate {@link SheetAssembly}.
 *
 * <p>This class is meant to be a Singleton
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SheetController
    implements ChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        SheetController.class);

    /** Flag a tab sheetIndex that is not yet available */
    public static final int DIFFERED_INDEX = -2;

    //~ Instance fields --------------------------------------------------------

    /** Ordered list of sheet assemblies */
    private final ArrayList<SheetAssembly> assemblies;

    /** The concrete tabbed pane, one tab per sheet */
    private final JTabbedPane component;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // SheetController //
    //-----------------//
    /**
     * Create the SheetController, within the gui frame.
     */
    public SheetController ()
    {
        component = new JTabbedPane();
        assemblies = new ArrayList<SheetAssembly>();

        // Listener on sheet tab operations
        component.addChangeListener(this);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the real pane
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return component;
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

    //------------------//
    // setSheetAssembly //
    //------------------//
    /**
     * Prepare the parameters of the assembly that relates to the specified
     * sheet.  This method is called by setScoreSheetView() above for an
     * immediate preparation and by Sheet constructor for a differed preparation
     * (when the sheet has finished loading).
     *
     * @param sheet the sheet to be viewed. If no sheet is provided, then an
     *              empty assembly is desired.
     *
     * @return the sheetIndex in the tabbed pane of the prepared assembly
     */
    public int setSheetAssembly (Sheet sheet)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setSheetAssembly for sheet " + sheet.getRadix());
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
            int sheetIndex = component.indexOfComponent(
                assembly.getComponent());

            if (sheetIndex == -1) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Adding assembly for sheet " + sheet.getRadix());
                }

                // Insert in tabbed pane
                assemblies.add(assembly);
                component.addTab(
                    sheet.getRadix(),
                    null,
                    assembly.getComponent(),
                    sheet.getPath());
                sheetIndex = component.indexOfComponent(
                    assembly.getComponent());
            }

            return sheetIndex;
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
            logger.fine("closing " + assembly.toString());
        }

        // Check whether the script has been written correctly
        final Sheet sheet = assembly.getSheet();
        ScriptActions.checkStored(sheet.getScript());

        int sheetIndex = component.indexOfComponent(assembly.getComponent());

        if (sheetIndex != -1) {
            // Remove from assemblies
            assemblies.remove(sheetIndex);
            // Remove from tabs
            component.remove(sheetIndex);
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                "closed " + assembly.toString() + " assemblies=" + assemblies);
        }
    }

    //---------------//
    // showSheetView //
    //---------------//
    /**
     * Make the (preset) SheetView actually visible.
     *
     * @param sheetIndex the index to the SheetView to be made current, with its
     *                potential focus point, or -1 to show a blank panel.
     */
    public void showSheetView (int sheetIndex)
    {
        component.setSelectedIndex(sheetIndex);
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

        if (source == component) {
            final int sheetIndex = component.getSelectedIndex();

            // User has selected a new sheet tab?
            if (sheetIndex != -1) {
                // Connect the new sheet tab
                sheetTabSelected(sheetIndex);
            }
        }
    }

    //---------------------//
    // getSelectedAssembly //
    //---------------------//
    /**
     * Report the current sheet assembly
     *
     * @return the sheet assembly currently selected
     */
    private SheetAssembly getSelectedAssembly ()
    {
        int sheetIndex = component.getSelectedIndex();

        if (sheetIndex != -1) {
            return assemblies.get(sheetIndex);
        } else {
            return null;
        }
    }

    //------------------//
    // sheetTabSelected //
    //------------------//
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
        SheetManager.getEventService()
                    .publish(new SheetEvent(this, sheet));

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

        /** Default directory for selection of sheet image files */
        Constant.String defaultSheetDirectory = new Constant.String(
            "",
            "Default directory for selection of sheet image files");

        /** Initial zoom ratio for displayed sheet pictures */
        Constant.Ratio initialZoomRatio = new Constant.Ratio(
            1d,
            "Initial zoom ratio for displayed sheet pictures");
    }
}
