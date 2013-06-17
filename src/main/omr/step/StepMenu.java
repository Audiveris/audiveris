//----------------------------------------------------------------------------//
//                                                                            //
//                              S t e p M e n u                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.script.StepTask;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetsController;

import omr.util.BasicTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * Class {@code StepMenu} encapsulates the user interface needed to
 * deal with application steps.
 * Steps are represented by menu items, each one being a check box, to indicate
 * the current status regarding the execution of the step (done or not done).
 *
 * @author Hervé Bitteur
 */
public class StepMenu
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            StepMenu.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The concrete UI menu. */
    private final JMenu menu;

    //~ Constructors -----------------------------------------------------------
    //----------//
    // StepMenu //
    //----------//
    /**
     * Generates the menu to be inserted in the application pull-down
     * menus.
     *
     * @param menu the hosting menu, or null
     */
    public StepMenu (JMenu menu)
    {
        if (menu == null) {
            menu = new JMenu();
        }

        this.menu = menu;

        // Build the menu content
        updateMenu();

        // Listener to modify attributes on-the-fly
        menu.addMenuListener(new MyMenuListener());
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // updateMenu //
    //------------//
    /**
     * Update/rebuild the content of menu.
     */
    public final void updateMenu ()
    {
        menu.removeAll();

        Step prevStep = null;

        // List of Steps classes in proper order
        for (Step step : Steps.values()) {
            if ((prevStep != null)
                && (prevStep.isMandatory() != step.isMandatory())) {
                menu.addSeparator();
            }

            menu.add(new StepItem(step));
            prevStep = step;
        }
    }

    //---------//
    // getMenu //
    //---------//
    /**
     * Report the concrete UI menu.
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
     * Action to be performed when the related step item is selected.
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
        @Override
        public void actionPerformed (ActionEvent e)
        {
            final Sheet sheet = SheetsController.getCurrentSheet();
            new BasicTask()
            {
                @Override
                protected Void doInBackground ()
                        throws Exception
                {
                    Step sofar = Stepping.getLatestMandatoryStep(sheet);

                    if ((sofar == null)
                        || (Steps.compare(sofar, step) <= 0)) {
                        // Here we progress on all sheets of the score
                        new StepTask(step).run(sheet);
                    } else {
                        // There we rebuild just the current sheet
                        Stepping.reprocessSheet(step, sheet, null, true);
                    }

                    return null;
                }

                @Override
                protected void finished ()
                {
                    // Select the assembly tab related to the target step
                    if (sheet != null) {
                        Stepping.notifyStep(sheet, step);
                    }
                }
            }.execute();
        }
    }

    //----------//
    // StepItem //
    //----------//
    /**
     * Class {@code StepItem} implements a checkable menu item linked
     * to a given step.
     */
    private static class StepItem
            extends JCheckBoxMenuItem
    {
        //~ Constructors -------------------------------------------------------

        public StepItem (Step step)
        {
            super(new StepAction(step));
        }

        //~ Methods ------------------------------------------------------------
        public void displayState (Sheet sheet,
                                  boolean isIdle)
        {
            StepAction action = (StepAction) getAction();

            if (sheet == null) {
                setState(false);
                action.setEnabled(false);
            } else {
                action.setEnabled(true);

                if (action.step.isMandatory()) {
                    final boolean done = action.step.isDone(sheet);
                    setState(done);
                } else {
                    setState(false);
                    action.setEnabled(true);
                }

                if (!isIdle) {
                    action.setEnabled(false);
                }
            }
        }
    }

    //----------------//
    // MyMenuListener //
    //----------------//
    /**
     * Class {@code MyMenuListener} is triggered when the whole sub-menu
     * is entered.
     * This is done with respect to currently displayed sheet.
     * The steps already done are flagged as such.
     */
    private class MyMenuListener
            implements MenuListener
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public void menuCanceled (MenuEvent e)
        {
        }

        @Override
        public void menuDeselected (MenuEvent e)
        {
        }

        @Override
        public void menuSelected (MenuEvent e)
        {
            Sheet sheet = SheetsController.getCurrentSheet();
            boolean isIdle = (sheet != null)
                             && (sheet.getCurrentStep() == null);

            for (int i = 0; i < menu.getItemCount(); i++) {
                JMenuItem menuItem = menu.getItem(i);

                // Adjust the status for each step
                if (menuItem instanceof StepItem) {
                    StepItem item = (StepItem) menuItem;
                    item.displayState(sheet, isIdle);
                }
            }
        }
    }
}
