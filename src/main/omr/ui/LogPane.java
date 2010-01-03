//----------------------------------------------------------------------------//
//                                                                            //
//                               L o g P a n e                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import java.awt.*;
import java.util.concurrent.BlockingQueue;
import java.util.logging.*;

import javax.swing.*;
import javax.swing.text.*;

/**
 * Class <code>LogPane</code> defines the pane dedicated to application-level
 * messages, those that are logged using the <code>Logger</code> class.
 *
 * @see omr.log.Logger
 * @see omr.log.LogGuiHandler
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LogPane
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LogPane.class);

    //~ Instance fields --------------------------------------------------------

    /** The scrolling text area */
    private JScrollPane component;

    /** Status/log area */
    private final JTextPane logArea;
    private final AbstractDocument         document;
    private final SimpleAttributeSet       attributes = new SimpleAttributeSet();

    /** The mailbox where log records are retrieved for display */
    private final BlockingQueue<LogRecord> logMbx = Logger.getMailbox();

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

        // log/status area
        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setMargin(new Insets(5, 5, 5, 5));
        document = (AbstractDocument) logArea.getStyledDocument();

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

    //-----------//
    // notifyLog //
    //-----------//
    /**
     * Tell LogPane that there is one or more log records in the Logger mailbox
     */
    public void notifyLog ()
    {
        SwingUtilities.invokeLater(
            new Runnable() {
                    public void run ()
                    {
                        while (logMbx.size() != 0) {
                            LogRecord record = logMbx.poll();

                            if (record != null) {
                                // Message text
                                StringBuilder sb = new StringBuilder(256);
                                sb.append(record.getLevel().toString())
                                  .append(" - ")
                                  .append(record.getMessage())
                                  .append("\n");

                                // Color
                                StyleConstants.setForeground(
                                    attributes,
                                    getLevelColor(record.getLevel()));

                                // Font name
                                StyleConstants.setFontFamily(
                                    attributes,
                                    constants.fontName.getValue());

                                // Font size
                                StyleConstants.setFontSize(
                                    attributes,
                                    constants.fontSize.getValue());

                                try {
                                    document.insertString(
                                        document.getLength(),
                                        sb.toString(),
                                        attributes);
                                } catch (BadLocationException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                });
    }

    //---------------//
    // getLevelColor //
    //---------------//
    private Color getLevelColor (Level level)
    {
        int val = level.intValue();

        if (val >= Level.SEVERE.intValue()) {
            return Color.RED;
        } else if (val >= Level.WARNING.intValue()) {
            return Color.BLUE;
        } else if (val >= Level.INFO.intValue()) {
            return Color.BLACK;
        } else {
            return Color.GRAY;
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer fontSize = new Constant.Integer(
            "Points",
            10,
            "Font size for log pane");
        Constant.String  fontName = new Constant.String(
            "Lucida Console",
            "Font name for log pane");
    }
}
