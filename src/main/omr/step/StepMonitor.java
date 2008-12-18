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

import omr.constant.Constant;
import omr.constant.Constant.Ratio;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.ui.util.UIUtilities;

import omr.util.Implement;

import java.awt.Graphics;
import java.util.EnumSet;
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

    /** Progress bar for actions performed on sheet */
    private final JProgressBar bar = new MyJProgressBar();

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // StepMonitor //
    //-------------//
    /**
     * Create a user monitor on step processing. This also starts a background
     * working task to take care of all lengthy processing. This is exactly one
     * instance of this class (and zero instance when running in batch mode)
     */
    public StepMonitor ()
    {
        // Progress Bar
        if (!omr.Main.MAC_OS_X) {
            bar.setBorder(UIUtilities.getToolBorder());
        }

        bar.setToolTipText("On going Step activity");
        bar.setStringPainted(true);
        animate(false);
        bar.setString("");
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
    // animate //
    //---------//
    /**
     * Sets the progress bar to show a percentage a certain amount above
     * the previous percentage value (or above 0 if the bar had been
     * indeterminate).
     */
    void animate ()
    {
        SwingUtilities.invokeLater(
            new Runnable() {
                    @Implement(Runnable.class)
                    public void run ()
                    {
                        int old = bar.getValue();

                        if (old > bar.getMinimum()) {
                            int diff = bar.getMaximum() - old;
                            int increment = (int) Math.round(
                                diff * constants.ratio.getValue());
                            bar.setIndeterminate(false);
                            bar.setValue(old + increment);
                        }
                    }
                });
    }

    //---------//
    // animate //
    //---------//
    /**
     * Sets the progress bar to show a percentage.
     * @param amount percentage, in decimal form, from 0.0 to 1.0
     */
    private void animate (final double amount)
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
     *
     * @param animating If false, deactivates all animation of the progress
     *                  bar.  If true, activates an indeterminate or
     *                  pseudo-indeterminate animation.
     */
    void animate (final boolean animating)
    {
        animate(animating ? constants.ratio.getValue() : 0);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer divisions = new Constant.Integer(
            "divisions",
            1000,
            "Number of divisions (amount of precision) of step monitor, minimum 10");
        Ratio            ratio = new Ratio(
            1.0 / 10.0,
            "Amount by which to increase step monitor percentage per animation, between 0 and 1");
    }

    //----------------//
    // MyJProgressBar //
    //----------------//
    private static class MyJProgressBar
        extends JProgressBar
    {
        //~ Methods ------------------------------------------------------------

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
