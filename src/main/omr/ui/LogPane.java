//----------------------------------------------------------------------------//
//                                                                            //
//                               L o g P a n e                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.util.Logger;

import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.*;

/**
 * Class <code>LogPane</code> defines the pane dedicated to application-level
 * messages, those that are logged using the <code>Logger</code> class.
 *
 * @see omr.util.Logger
 * @see omr.util.LogGuiHandler
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LogPane
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants           constants = new Constants();
    private static final Logger              logger = Logger.getLogger(
        LogPane.class);

    //~ Instance fields --------------------------------------------------------

    /** Mail box for incoming messages */
    private final ArrayBlockingQueue<String> logMbx;

    /** The scrolling text area */
    private JScrollPane component;

    /** Status/log area */
    private final JTextArea logArea;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // LogPane //
    //---------//
    /**
     * Create the log pane, with a standard mailbox.
     */
    public LogPane ()
    {
        // Build the scroll pane
        component = new JScrollPane();

        // Allocate message mail box for several simultaneous msgs max
        logMbx = new ArrayBlockingQueue<String>(
            constants.msgQueueSize.getValue());

        // log/status area
        logArea = new JTextArea(
            1, // nb of rows
            60); // nb of columns
        logArea.setEditable(false);

        //logArea.setMargin (new Insets (5,5,5,5));
        // Let the scroll pane display the log area
        component.setViewportView(logArea);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Give access to the real component
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return component;
    }

    //----------//
    // clearLog //
    //----------//
    /**
     * Clear the current content of the log
     */
    public void clearLog ()
    {
        logArea.setText("");
        logArea.setCaretPosition(0);
        component.repaint();
    }

    //-----//
    // log //
    //-----//
    /**
     * Display the given message in the dedicated status area.
     *
     * @param msg a message to log/display
     */
    public void log (String msg)
    {
        try {
            logMbx.put(msg);
        } catch (Exception ex) {
            logger.warning(ex.getMessage());

            return;
        }

        SwingUtilities.invokeLater(
            new Runnable() {
                    public void run ()
                    {
                        while (logMbx.size() != 0) {
                            String msg = logMbx.poll();

                            if (msg != null) {
                                logArea.append(msg);
                                logArea.setCaretPosition(
                                    logArea.getDocument().getLength());
                            }
                        }
                    }
                });
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        Constant.Integer msgQueueSize = new Constant.Integer(
            1000,
            "Size of message queue");
    }
}
