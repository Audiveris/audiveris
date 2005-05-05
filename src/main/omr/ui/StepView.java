//-----------------------------------------------------------------------//
//                                                                       //
//                            S t e p V i e w                            //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.ui;

import omr.Main;
import omr.Step;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.sheet.Sheet;
import omr.util.Logger;
import omr.util.MailBox;

import javax.swing.*;

/**
 * Class <code>StepView</code> is the user interface entity that allows to
 * monitor step progression, and require manually a step series to be
 * performed.
 */
public class StepView
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(StepView.class);

    //~ Instance variables ------------------------------------------------

    // Progress Monitor
    private ProgressMonitor progressMonitor;
    private int progress = 0;

    // Mail box for steps
    private final MailBox msgs = new MailBox(20);

    // Mail box for orders
    private final MailBox orders = new MailBox(20);

    //----------------//
    // updateProgress //
    //----------------//
    private final Runnable updateProgress = new Runnable()
    {
        public void run ()
        {
            JProgressBar bar = Main.getJui().progressBar;

            try {
                while (msgs.getCount() != 0) {
                    Object obj = msgs.poll();

                    if (obj instanceof String) {
                        bar.setString((String) obj);

                        if (constants.useDeterminateProgress.getValue()) {
                            int value = bar.getValue();
                            value++;
                            bar.setValue(value);
                        }
                    } else {
                        logger.severe("What is " + obj);
                    }
                }
            } catch (InterruptedException ex) {
                logger.warning("Error in updating ProgressBar", ex);
            }
        }
    };


    //~ Constructors ------------------------------------------------------

    //----------//
    // StepView //
    //----------//

    /**
     * Create a user view on step processing. This also starts a background
     * working task to take care of all lengthy processing.
     */
    public StepView ()
    {
        // Launch the worker
        new Worker().start();
    }

    //~ Methods -----------------------------------------------------------

    //-----------//
    // notifyMsg //
    //-----------//

    /**
     * Call this to display a simple message in the progress window.
     *
     * @param msg the message to display
     */
    public void notifyMsg (String msg)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("notifyMsg. msg=" + msg);
        }

        try {
            msgs.put(msg);
            SwingUtilities.invokeLater(updateProgress);
        } catch (InterruptedException ex) {
            logger.warning("InterruptedException occurred in notifyMsg", ex);
        }
    }

    //---------//
    // perform //
    //---------//

    /**
     * Start the performance of a series of steps, with an online display
     * of a progress monitor.
     *
     * @param step     the target step, all intermediate steps will be
     *                 performed beforehand if any.
     * @param sheet    the sheet being analyzed
     * @param param    an eventual parameter for targeted step
     * @param fromStep the current starting step
     */
    public void perform (final Step step,
                         final Sheet sheet,
                         final Object param,
                         Step fromStep)
    {
        // Post the request
        try {
            Order order = new Order(step, fromStep, sheet, param);

            if (sheet != null) {
                sheet.setOrder(order);
            }

            orders.put(order);
        } catch (InterruptedException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("InterruptedException in perform put");
            }
        }
    }

    //~ Classes -----------------------------------------------------------

    //-------//
    // Order //
    //-------//

    /**
     * Class <code>Order</code> is an opaque class that handles a
     * processing order to be done on a given sheet instance.
     */
    public static class Order
    {
        //~ Instance variables --------------------------------------------

        private final Step step; // Target step
        private final Step fromStep; // Original step
        private final Sheet sheet; // Sheet to be processed
        private final Object param; // Eventual additional parameter;

        //~ Constructors --------------------------------------------------

        //-------//
        // Order //
        //-------//

        /**
         * Build a brand new order, with needed parameters.
         *
         * @param step     the targeted step (last step in the series)
         * @param fromStep the first step in the series
         * @param sheet    the sheet instance to be processed
         * @param param    an eventual parameter for processing
         */
        public Order (Step step,
                      Step fromStep,
                      Sheet sheet,
                      Object param)
        {
            this.step = step;
            this.fromStep = fromStep;
            this.sheet = sheet;
            this.param = param;
        }

        //~ Methods -------------------------------------------------------

        //----------//
        // toString //
        //----------//

        /**
         * A readable description of this order
         *
         * @return a description string
         */
        public String toString ()
        {
            StringBuffer sb = new StringBuffer();

            sb.append("{Order");
            sb.append(" Sheet=").append(sheet);

            if (step == fromStep) {
                sb.append(" Step=").append(step);
            } else {
                sb.append(" From=").append(fromStep);
                sb.append(" To=").append(step);
            }

            sb.append(" Param=").append(param);
            sb.append("}");

            return sb.toString();
        }
    }

    //--------//
    // Worker //
    //--------//
    private class Worker
            extends Thread
    {
        //~ Methods -------------------------------------------------------

        //-----//
        // run //
        //-----//
        public void run ()
        {
            while (true) {
                Order order = null;
                JProgressBar bar = null;

                try {
                    // Hold on here, waiting for the next incoming order
                    order = (Order) orders.get();

                    if (logger.isDebugEnabled()) {
                        logger.debug("Got " + order);
                    }

                    // ProgressBar global parameters
                    bar = Main.getJui().progressBar;
                    bar.setMinimum(0);
                    bar.setStringPainted(true);
                    bar.setIndeterminate(!constants.useDeterminateProgress
                                          .getValue());

                    // Set up the progress bar
                    if (constants.useDeterminateProgress.getValue()) {
                        bar.setValue(0);

                        int togo = order.step.getPosition()
                                   - order.fromStep.getPosition() + 1;
                        bar.setMaximum(togo);
                    }

                    // Launch execution
                    if (logger.isDebugEnabled()) {
                        logger.debug("starting " + order.step);
                    }

                    order.step.doPerform(order.sheet, order.param);

                    // Update title of the frame
                    Main.getJui().updateTitle();

                    if (logger.isDebugEnabled()) {
                        logger.debug("done");
                    }
                } catch (InterruptedException ex) {
                    logger.warning("InterruptedException in worker get", ex);
                } catch (omr.ProcessingException ex) {
                    logger.severe("Should not occur", ex);
                } finally {
                    // Make the sheet available again
                    if (order != null) {
                        if (order.sheet != null) {
                            order.sheet.setOrder(null);
                        }
                    }

                    // Reset the progress bar
                    if (bar != null) {
                        bar.setString("");
                        bar.setIndeterminate(false);
                        bar.setValue(0);
                    }
                }
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        //~ Instance variables --------------------------------------------

        Constant.Boolean useDeterminateProgress = new Constant.Boolean
                (true,
                 "Should we use progress bar with determinate state?");

        Constants ()
        {
            initialize();
        }
    }
}
