//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t e p M e n u                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.step.ui;

import org.audiveris.omr.OMR;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.step.ProcessingCancellationException;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.ui.util.AbstractMenuListener;
import org.audiveris.omr.util.VoidTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.MenuEvent;

/**
 * Class {@code StepMenu} encapsulates the user interface needed to deal with
 * application steps.
 * Steps are represented by menu items, each one being a check box, to indicate the current status
 * regarding the execution of the step (done or not done).
 *
 * @author Hervé Bitteur
 */
public class StepMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StepMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The concrete UI menu. */
    private final JMenu menu;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Generates the menu to be inserted in the application pull-down menus.
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

    //~ Methods ------------------------------------------------------------------------------------
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

    //------------//
    // updateMenu //
    //------------//
    /**
     * Update/rebuild the content of menu.
     */
    public final void updateMenu ()
    {
        menu.removeAll();

        // List of Steps classes in proper order
        for (Step step : Step.values()) {
            menu.add(new StepItem(step));
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    private static class StepAction
            extends AbstractAction
    {

        // The related step
        Step step;

        StepAction (Step step)
        {
            super(step.toString());
            this.step = step;
            putValue(SHORT_DESCRIPTION, step.getDescription());
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final SheetStub stub = StubsController.getCurrentStub();
            new VoidTask()
            {
                @Override
                protected Void doInBackground ()
                        throws Exception
                {
                    try {
                        Step sofar = stub.getLatestStep();

                        if ((sofar != null) && (sofar.compareTo(step) >= 0)) {
                            // Make sure proper source is available for LOAD or BINARY target steps
                            if (step == Step.LOAD) {
                                final Path inputPath = stub.getBook().getInputPath();

                                if (!Files.exists(inputPath)) {
                                    OMR.gui.displayWarning(
                                            "Input file not found: " + inputPath,
                                            "No source for " + step + " step");
                                    step = null;

                                    return null;
                                }
                            } else if (step == Step.BINARY) {
                                final Picture picture = stub.getSheet().getPicture();

                                if (null == picture.getSource(Picture.SourceKey.GRAY)) {
                                    OMR.gui.displayWarning(
                                            "Gray source not found.",
                                            "No source for " + step + " step");
                                    step = null;

                                    return null;
                                }
                            }

                            // Refine messages for very first steps (LOAD and BINARY)
                            final String mid = (step.compareTo(Step.BINARY) <= 0) ? ""
                                    : " from binary source";

                            if (!OMR.gui.displayConfirmation(
                                    "About to re-perform step " + step + mid + "."
                                    + "\nDo you confirm?",
                                    "Redo confirmation",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE)) {
                                return null;
                            }
                        }

                        try {
                            LogUtil.start(stub);

                            boolean ok = stub.reachStep(step, true);
                            logger.info("End of sheet step {}", stub.getLatestStep());
                        } finally {
                            LogUtil.stopStub();
                        }
                    } catch (ProcessingCancellationException pce) {
                        logger.info("ProcessingCancellationException detected");
                    }

                    return null;
                }

                @Override
                protected void finished ()
                {
                    if (stub != null) {
                        final Step latestStep = stub.getLatestStep();

                        if (latestStep != null) {
                            // Select the assembly tab related to the latest step
                            StepMonitoring.notifyStep(stub, latestStep);
                        }
                    }
                }
            }.execute();
        }
    }

    //----------//
    // StepItem //
    //----------//
    /**
     * Class {@code StepItem} implements a checkable menu item linked to a given step.
     */
    private static class StepItem
            extends JCheckBoxMenuItem
    {

        StepItem (Step step)
        {
            super(new StepAction(step));
        }

        public void displayState (SheetStub stub,
                                  boolean isIdle)
        {
            StepAction action = (StepAction) getAction();

            if ((stub == null) || !stub.isValid()) {
                setState(false);
                action.setEnabled(false);
            } else {
                action.setEnabled(true);

                final boolean done = stub.isDone(action.step);
                setState(done);

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
     * Class {@code MyMenuListener} is triggered when the whole sub-menu is entered.
     * This is done with respect to currently displayed sheet.
     * The steps already done are flagged as such.
     */
    private class MyMenuListener
            extends AbstractMenuListener
    {

        @Override
        public void menuSelected (MenuEvent e)
        {
            SheetStub stub = StubsController.getCurrentStub();
            boolean isIdle = (stub != null) && (stub.getCurrentStep() == null);

            for (int i = 0; i < menu.getItemCount(); i++) {
                JMenuItem menuItem = menu.getItem(i);

                // Adjust the status for each step
                if (menuItem instanceof StepItem) {
                    StepItem item = (StepItem) menuItem;
                    item.displayState(stub, isIdle);
                }
            }
        }
    }
}
