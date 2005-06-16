//-----------------------------------------------------------------------//
//                                                                       //
//                             L o g P a n e                             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.util.MailBox;

import javax.swing.*;

/**
 * Class <code>LogPane</code> defines the pane dedicated to application-level
 * messages, those that are logged using the <code>Logger</code> class.
 *
 * @see omr.util.Logger
 * @see omr.util.LogGuiAppender
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class LogPane
        extends JScrollPane
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance variables ------------------------------------------------

    // Status/log area
    private final JTextArea logArea;

    // Mail box for incoming messages
    private final MailBox logMbx;

    //~ Constructors ------------------------------------------------------

    //---------//
    // LogPane //
    //---------//

    /**
     * Create the log pane, with a standard mailbox.
     */
    public LogPane ()
    {
        // Build the scroll pane
        super();

        // Allocate message mail box for several simultaneous msgs max
        logMbx = new MailBox(constants.msgQueueSize.getValue());

        // log/status area
        logArea = new JTextArea(1, // nb of rows
                                60); // nb of columns
        logArea.setEditable(false);

        //logArea.setMargin (new Insets (5,5,5,5));
        // Let the scroll pane display the log area
        setViewportView(logArea);
    }

    //~ Methods -----------------------------------------------------------

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
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException occurred in log method");

            return;
        }

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run ()
            {
                try {
                    while (logMbx.getCount() != 0) {
                        String msg = (String) logMbx.poll();

                        if (msg != null) {
                            logArea.append(msg);
                            logArea.setCaretPosition(logArea.getDocument()
                                                     .getLength());
                        }
                    }
                } catch (InterruptedException ex) {
                    System.out.println("InterruptedException occurred in log method");
                }
            }
        });
    }

    //~ Classes -----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        //~ Instance variables --------------------------------------------

        Constant.Integer msgQueueSize = new Constant.Integer
                (1000,
                 "Size of message queue");

        Constants ()
        {
            initialize();
        }
    }
}
