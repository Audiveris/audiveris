//----------------------------------------------------------------------------//
//                                                                            //
//                              S t e p M e n u                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.log.Logger;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetsController;

import omr.util.BasicTask;
import omr.util.Implement;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>StepMenu</code> encapsulates the user interface needed to deal
 * with application steps.  Steps are represented by menu items, each one being
 * a check box, to indicate the current status regarding the execution of the
 * step (done or not done).
 *
 * @author Hervé Bitteur
 */
public class StepMenu
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(StepMenu.class);

    //~ Instance fields --------------------------------------------------------

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

        Step prevStep = null;

        // List of Steps classes in proper order
        for (Step step : Step.values()) {
            if ((prevStep != null) &&
                (prevStep.isMandatory != step.isMandatory)) {
                menu.addSeparator();
            }

            menu.add(new StepItem(step));
            prevStep = step;
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
            putValue(SHORT_DESCRIPTION, step.description);
        }

        //~ Methods ------------------------------------------------------------

        @Implement(AbstractAction.class)
        public void actionPerformed (ActionEvent e)
        {
            final Sheet      sheet = SheetsController.selectedSheet();
            final SheetSteps steps = sheet.getSheetSteps();
            new BasicTask() {
                    @Override
                    protected Void doInBackground ()
                        throws Exception
                    {
                        Step sofar = steps.getLatestMandatoryStep();

                        if ((sofar == null) || (sofar.compareTo(step) <= 0)) {
                            step.performUntil(sheet);
                        } else {
                            steps.rebuildFrom(step, null, true);
                        }

                        return null;
                    }

                    @Override
                    protected void finished ()
                    {
                        // Select the assembly tab related to the target step
                        if (sheet != null) {
                            sheet.getAssembly()
                                 .selectTab(steps.getLatestMandatoryStep());
                        }
                    }
                }.execute();
        }
    }

    //----------//
    // StepItem //
    //----------//
    /**
     * Class <code>StepItem</code> implements a checkable menu item
     * linked to a given step
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

        public void displayState (Sheet sheet)
        {
            StepAction action = (StepAction) getAction();

            if (sheet == null) {
                setState(false);
                action.setEnabled(false);
            } else {
                if (action.step.isMandatory) {
                    final boolean done = sheet.getSheetSteps()
                                              .isDone(action.step);

                    setState(done);

                    if (action.step.isRedoable) {
                        action.setEnabled(true);
                    } else {
                        action.setEnabled(!done);
                    }
                } else {
                    setState(false);

                    action.setEnabled(true);
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
            Sheet sheet = SheetsController.selectedSheet();

            for (int i = 0; i < menu.getItemCount(); i++) {
                JMenuItem menuItem = menu.getItem(i);

                // Adjust the status for each step
                if (menuItem instanceof StepItem) {
                    StepItem item = (StepItem) menuItem;
                    item.displayState(sheet);
                }
            }
        }
    }
}
