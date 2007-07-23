//----------------------------------------------------------------------------//
//                                                                            //
//                           S t e p M o n i t o r                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.step;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.constant.Constant.Ratio;

import omr.sheet.Sheet;

import omr.ui.util.UIUtilities;

import omr.util.Implement;
import omr.util.Logger;

import java.awt.Graphics;
import java.util.concurrent.*;

import javax.swing.*;

/**
 * Class <code>StepMonitor</code> is the user interface entity that allows to
 * monitor step progression, and to require manually that a step be performed.
 *
 * @author Herv&eacute; Bitteur and Brenton Partridge
 * @version $Id$
 */
public class StepMonitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(StepMonitor.class);

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    //~ Instance fields --------------------------------------------------------

    /** Single threaded executor for lengthy tasks */
    private final ExecutorService executor;

    /** Progress bar for actions performed on sheet */
    private final JProgressBar bar = new MyJProgressBar();

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // StepMonitor //
    //-------------//
    /**
     * Create a user monitor on step processing. This also starts a background
     * working task to take care of all lengthy processing.
     */
    public StepMonitor ()
    {
        // Progress Bar
        if (!omr.Main.MAC_OS_X) 
        	bar.setBorder(UIUtilities.getToolBorder());
        bar.setToolTipText("On going Step activity");
        bar.setStringPainted(true);
        animate(false);
        bar.setString("");

        // Launch the worker
        executor = Executors.newSingleThreadExecutor();
    }

    //~ Methods ----------------------------------------------------------------

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

    //---------//
    // animate //
    //---------//
    /**
     *
     * @param animating If false, deactivates all animation of the progress
     *                  bar.  If true, activates an indeterminate or
     *                  pseudo-indeterminate animation.
     */
    public void animate (final boolean animating)
    {
        animate(animating ? constants.ratio.getValue() : 0);
    }

    //---------//
    // animate //
    //---------//
    /**
     * Sets the progress bar to show a percentage.
     * @param amount percentage, in decimal form, from 0.0 to 1.0
     */
    public void animate (final double amount)
    {
        SwingUtilities.invokeLater(
            new Runnable() {
            		@Implement(Runnable.class)
                	public void run ()
                    {
                        bar.setIndeterminate(false);

                        int divisions = constants.divisions.getValue();
                        bar.setMinimum(0);
                        bar.setMaximum(divisions);
                        bar.setValue((int) Math.round(divisions * amount));
                    }
                });
    }

    //---------//
    // animate //
    //---------//
    /**
     * Sets the progress bar to show a percentage a certain amount above
     * the previous percentage value (or above 0 if the bar had been
     * indeterminate).
     */
    public void animate ()
    {
        SwingUtilities.invokeLater(
            new Runnable() {
                    @Implement(Runnable.class)
                    public void run ()
                    {
                        int old = bar.getValue();
						if (old > bar.getMinimum())
						{
							int diff = bar.getMaximum() - old;
							int increment = (int)Math.round(diff
								* constants.ratio.getValue());
							bar.setIndeterminate(false);
							bar.setValue(old + increment);
						}
                    }
                });
    }

    //-----------//
    // notifyMsg //
    //-----------//
    /**
     * Call this to display a simple message in the progress window.
     *
     * @param msg the message to display
     */
    public void notifyMsg (final String msg)
    {
        SwingUtilities.invokeLater(
            new Runnable() {
            		@Implement(Runnable.class)
                	public void run ()
                    {
                        bar.setString(msg);
                    }
                });
    }

    //---------//
    // perform //
    //---------//
    /**
     * Start the performance of a step, with an online display of a progress
     * monitor.
     *
     * @param step the target step
     * @param sheet the sheet being analyzed
     * @param param an eventual parameter for targeted step
     */
    public void perform (final Step   step,
                         final Sheet  sheet,
                         final Object param)
    {
        // Post the request
        executor.execute(
            new Runnable() {
            		@Implement(Runnable.class)
                	public void run ()
                    {

                        if (logger.isFineEnabled()) {
                            logger.fine(
                                step + " Executing sheet=" + sheet + " param=" +
                                param + " ...");
                        }

                        try {
                            // "Activate" the progress bar
                            if (bar != null) {
                                animate(true);
                            }

                            if (sheet != null) {
                                sheet.setBusy(true);
                            }

                            step.doStep(sheet, param);

                            // Update title of the frame
                            Main.getGui()
                                .updateGui();

                            if (sheet != null) {
                                sheet.setBusy(false);
                            }
                        } catch (Exception ex) {
                            logger.warning("Processing aborted", ex);
                        } finally {
                            // Reset the progress bar
                            if (bar != null) {
                                notifyMsg("");
                                animate(false);
                            }

                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    step + " Ending sheet=" + sheet +
                                    " param=" + param + ".");
                            }
                        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        Constant.Integer divisions = new Constant.Integer(
            "divisions",
            1000,
            "Number of divisions (amount of precision) of step monitor, minimum 10");
        Ratio ratio = new Ratio(
            1.0 / 10.0,
            "Amount by which to increase step monitor percentage per animation, between 0 and 1");
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
                logger.warning("StepMonitor. Ignored: " + ex);
                repaint(); // To trigger another painting
            }
        }
    }
}
