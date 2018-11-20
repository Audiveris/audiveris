//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t e p M o n i t o r i n g                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.step.Step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

/**
 * Class {@code StepMonitoring} handles the step progress notification to user, when
 * running in interactive mode.
 *
 * @author Hervé Bitteur
 */
public abstract class StepMonitoring
{

    private static final Logger logger = LoggerFactory.getLogger(StepMonitoring.class);

    /** Related progress monitor when used in interactive mode. */
    private static volatile StepMonitor monitor;

    /**
     * Not meant to be instantiated.
     */
    private StepMonitoring ()
    {
    }

    //---------//
    // animate //
    //---------//
    /**
     * When running interactively, animate the progress bar.
     */
    public static void animate ()
    {
        if (monitor != null) {
            monitor.animate();
        }
    }

    //---------------//
    // createMonitor //
    //---------------//
    /**
     * Allows to couple the steps with a UI.
     *
     * @return the monitor to deal with steps
     */
    public static StepMonitor createMonitor ()
    {
        return monitor = new StepMonitor();
    }

    //-----------//
    // notifyMsg //
    //-----------//
    /**
     * Notify a simple message, which may be not related to any step.
     *
     * @param msg the message to display on the UI window, or to write in the log if there is no UI.
     */
    public static void notifyMsg (String msg)
    {
        if (monitor != null) {
            monitor.notifyMsg(msg);
        } else {
            logger.info(msg);
        }
    }

    //-------------//
    // notifyStart //
    //-------------//
    /**
     * When running interactively, start the progress bar animation.
     */
    public static void notifyStart ()
    {
        // "Activate" the progress bar
        if (monitor != null) {
            monitor.displayAnimation(true);
        }
    }

    //------------//
    // notifyStep //
    //------------//
    /**
     * Notify the UI part that the provided step has started or stopped in the provided
     * sheet.
     *
     * @param stub the sheet stub concerned
     * @param step the step notified
     */
    public static void notifyStep (final SheetStub stub,
                                   final Step step)
    {
        if (monitor != null) {
            final boolean finished = stub.getCurrentStep() == null;
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run ()
                {
                    // Update sheet view for this step?
                    if (finished) {
                        if (stub.isValid()) {
                            step.displayUI(stub.getSheet());
                            stub.getAssembly().selectViewTab(step.getSheetTab());
                        }
                    }

                    // Call attention to this sheet (only if displayed),
                    // so that score-dependent actions can get enabled.
                    StubsController ctrl = StubsController.getInstance();
                    SheetStub currentStub = ctrl.getSelectedStub();

                    if (currentStub == stub) {
                        ctrl.callAboutStub(currentStub);
                    }
                }
            });
        }
    }

    //------------//
    // notifyStop //
    //------------//
    /**
     * When running interactively, stop the progress bar animation.
     */
    public static void notifyStop ()
    {
        // Reset the progress bar?
        if (monitor != null) {
            notifyMsg("");
            monitor.displayAnimation(false);
        }
    }
}
