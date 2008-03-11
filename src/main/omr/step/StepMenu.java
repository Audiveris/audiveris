//----------------------------------------------------------------------------//
//                                                                            //
//                              S t e p M e n u                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.step;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.util.Implement;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import omr.util.Logger;

/**
 * Class <code>StepMenu</code> encapsulates the user interface needed to deal
 * with application steps.  Steps are represented by menu items, each one being
 * a check box, to indicate the current status regarding the execution of the
 * step (done or not done).
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StepMenu
{
    //~ Instance fields --------------------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(StepMenu.class);

    /** The concrete UI menu */
    private final JMenu menu;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // StepMenu //
    //----------//
    /**
     * Generates the menu to be inserted in the application menu hierarchy.
     *
     * @param menu the hosting menu, or null
     */
    public StepMenu (JMenu menu)
    {
        if (menu == null) {
            menu = new JMenu();
        }

        // List of Steps classes in proper order
        for (Step step : Step.values()) {
            menu.add(new StepItem(step));
        }

        // Listener to modify attributes on-the-fly
        menu.addMenuListener(new MyMenuListener());

        this.menu = menu;
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getMenu //
    //---------//
    /**
     * Report the concrete UI menu
     *
     * @return the menu entity
     */
    public JMenu getMenu ()
    {
        return menu;
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // StepAction //
    //------------//
    /**
     * Action to be performed when the related step item is selected
     */
    private static class StepAction
        extends AbstractAction
    {
        //~ Instance fields ----------------------------------------------------

        // The related step
        final Step step;

        //~ Constructors -------------------------------------------------------

        public StepAction (Step step)
        {
            super(step.toString());
            this.step = step;
            putValue(SHORT_DESCRIPTION, step.getDescription());
        }

        //~ Methods ------------------------------------------------------------

        @Implement(AbstractAction.class)
        public void actionPerformed (ActionEvent e)
        {
            Sheet sheet = SheetManager.getSelectedSheet();
            step.performParallel(sheet, null);
        }
    }

    //----------//
    // StepItem //
    //----------//
    /**
     * Class <code>StepItem</code> implements a menu item linked to a given step
     */
    private static class StepItem
        extends JCheckBoxMenuItem
    {
        //~ Constructors -------------------------------------------------------

        //----------//
        // StepItem //
        //----------//
        public StepItem (Step step)
        {
            super(new StepAction(step));
        }

        //~ Methods ------------------------------------------------------------

        //--------------//
        // displayState //
        //--------------//
        public void displayState (Sheet sheet)
        {
            StepAction action = (StepAction) getAction();

            if (sheet == null) {
                setState(false);
                action.setEnabled(false);
            } else {
                if (sheet.isBusy()) {
                    setState(false);
                    setEnabled(false);
                } else {
                    boolean bool = sheet.getSheetSteps()
                                        .isDone(action.step);
                    setState(bool);
                    setEnabled(!bool);
                }
            }
        }
    }

    //----------------//
    // MyMenuListener //
    //----------------//
    /**
     * Class <code>MyMenuListener</code> is triggered when the whole sub-menu
     * is entered. This is done with respect to currently displayed sheet. The
     * steps already done are flagged as such.
     */
    private class MyMenuListener
        implements MenuListener
    {
        //~ Methods ------------------------------------------------------------

        @Implement(MenuListener.class)
        public void menuCanceled (MenuEvent e)
        {
        }

        @Implement(MenuListener.class)
        public void menuDeselected (MenuEvent e)
        {
        }

        @Implement(MenuListener.class)
        public void menuSelected (MenuEvent e)
        {
            Sheet sheet = SheetManager.getSelectedSheet();

            for (int i = 0; i < menu.getItemCount(); i++) {
                StepItem item = (StepItem) menu.getItem(i);

                // Adjust the status for each step
                item.displayState(sheet);
            }
        }
    }
}
