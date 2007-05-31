//----------------------------------------------------------------------------//
//                                                                            //
//                           S t e p M o n i t o r                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr;

import omr.sheet.Sheet;

import omr.ui.*;
import omr.ui.util.UIUtilities;

import omr.util.Logger;

import java.awt.Graphics;
import java.util.concurrent.*;

import javax.swing.*;

/**
 * Class <code>StepMonitor</code> is the user interface entity that allows to
 * monitor step progression, and require manually a step series to be performed.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StepMonitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(StepMonitor.class);

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
        bar.setBorder(UIUtilities.getToolBorder());
        bar.setToolTipText("On going Step activity");
        bar.setStringPainted(true);
        bar.setIndeterminate(false);
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
     * Start the performance of a series of steps, with an online display of a
     * progress monitor.
     *
     * @param step the target step, all intermediate steps will be performed
     *                 beforehand if any.
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
                    public void run ()
                    {
                        // This is supposed to run in the background, so...
                        Thread.currentThread()
                              .setPriority(Thread.MIN_PRIORITY);

                        if (logger.isFineEnabled()) {
                            logger.fine(
                                "Executing " + step + " sheet=" + sheet +
                                " param=" + param + " ...");
                        }

                        try {
                            // "Activate" the progress bar
                            if (bar != null) {
                                bar.setIndeterminate(true);
                            }

                            if (sheet != null) {
                                sheet.setBusy(true);
                            }

                            step.doPerform(sheet, param);

                            // Update title of the frame
                            Main.getGui()
                                .updateTitle();

                            if (sheet != null) {
                                sheet.setBusy(false);
                            }
                        } catch (ProcessingException ex) {
                            logger.warning("Processing aborted");
                        } finally {
                            // Reset the progress bar
                            if (bar != null) {
                                bar.setString("");
                                bar.setIndeterminate(false);
                            }

                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    "Ending " + step + " sheet=" + sheet +
                                    " param=" + param + ".");
                            }
                        }
                    }
                });
    }

    //~ Inner Classes ----------------------------------------------------------

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
