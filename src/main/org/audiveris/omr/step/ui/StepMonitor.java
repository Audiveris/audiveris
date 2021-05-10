//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S t e p M o n i t o r                                      //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.Constant.Ratio;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.ui.Colors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * Class {@code StepMonitor} is the user interface entity that allows to monitor step
 * progression.
 *
 * @author Hervé Bitteur
 * @author Brenton Partridge
 */
public class StepMonitor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StepMonitor.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Progress bar for actions performed on sheet. */
    private final JProgressBar bar = new MyJProgressBar();

    /** Total active actions. */
    private int actives = 0;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a user monitor on step processing.
     * There is exactly one instance of this class (and no instance when running in batch mode)
     */
    public StepMonitor ()
    {
        bar.setToolTipText("On going Step activity");
        bar.setStringPainted(true);
        displayAnimation(false);
        bar.setString("");
        bar.setForeground(Colors.PROGRESS_BAR);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the monitoring bar
     *
     * @return the step progress bar
     */
    public JProgressBar getComponent ()
    {
        return bar;
    }

    //-----------//
    // notifyMsg //
    //-----------//
    /**
     * Display a simple message in the progress window.
     *
     * @param msg the message to display
     */
    public void notifyMsg (final String msg)
    {
        logger.debug("notifyMsg '{}'", msg);
        SwingUtilities.invokeLater(() -> bar.setString(msg));
    }

    //--------//
    // setBar //
    //--------//
    /**
     * Sets the progress bar to show a percentage.
     *
     * @param amount percentage, in decimal form, from 0.0 to 1.0
     */
    private void setBar (final double amount)
    {
        logger.debug("setBar amount:{}", amount);
        SwingUtilities.invokeLater(() -> {
            int divisions = constants.divisions.getValue();
            bar.setMinimum(0);
            bar.setMaximum(divisions);

            int val = (int) Math.round(divisions * amount);
            bar.setValue(val);
        });
    }

    //------------------//
    // displayAnimation //
    //------------------//
    /**
     * Switch on or off the display of the progress bar
     *
     * @param animating If false, deactivates all animation of the progress bar.
     *                  If true, activates an indeterminate or pseudo-indeterminate animation.
     */
    final synchronized void displayAnimation (final boolean animating)
    {
        logger.debug("displayAnimation animating:{}", animating);

        if (animating) {
            actives++;
            setBar(constants.ratio.getValue());

            if (constants.useIndeterminate.isSet()) {
                bar.setIndeterminate(true);
            }
        } else {
            if (actives > 0) {
                actives--;
            }

            if (actives <= 0) {
                setBar(0);
                bar.setIndeterminate(false);
            }
        }
    }

    //---------//
    // animate //
    //---------//
    /**
     * Sets the progress bar to show a percentage a certain amount above the previous
     * percentage value (or above 0 if the bar had been indeterminate).
     * This method is called on every message logged (see LogStepMonitorHandler)
     */
    void animate ()
    {
        if (!constants.useIndeterminate.isSet()) {
            logger.debug("animate");
            SwingUtilities.invokeLater(() -> {
                int old = bar.getValue();

                if (old > bar.getMinimum()) {
                    int diff = bar.getMaximum() - old;
                    int increment = (int) Math.round(diff * constants.ratio.getValue());

                    bar.setValue(old + increment);
                }
            });
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer divisions = new Constant.Integer(
                "divisions",
                1_000,
                "Number of divisions (amount of precision) of step monitor, minimum 10");

        private final Ratio ratio = new Ratio(
                0.1,
                "Amount by which to increase step monitor percentage per animation, between 0 and 1");

        private final Constant.Boolean useIndeterminate = new Constant.Boolean(
                true,
                "Should we use an indeterminate step progress bar?");
    }

    //----------------//
    // MyJProgressBar //
    //----------------//
    private static class MyJProgressBar
            extends JProgressBar
    {

        @Override
        public void paintComponent (Graphics g)
        {
            try {
                super.paintComponent(g);
            } catch (Exception ex) {
                // Nearly ignored
                repaint(); // To trigger another painting
            }
        }
    }
}
