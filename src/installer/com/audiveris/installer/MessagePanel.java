//----------------------------------------------------------------------------//
//                                                                            //
//                           M e s s a g e P a n e l                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import ch.qos.logback.classic.Level;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * Class {@code MessagePanel} handles a panel meant to display log
 * messages.
 *
 * @author Hervé Bitteur
 */
public class MessagePanel
{
    //~ Static fields/initializers ---------------------------------------------

    private static final String      LOG_FONT = "Lucida Console";

    //~ Instance fields --------------------------------------------------------

    private final JTextPane          textPane = new JTextPane();
    private final AbstractDocument   document = (AbstractDocument) textPane.getStyledDocument();
    private final SimpleAttributeSet attributes = new SimpleAttributeSet();

    /** Host the text in a scroll pane. */
    private JScrollPane panel = new JScrollPane();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MessagePanel object.
     */
    public MessagePanel ()
    {
        textPane.setEditable(false);
        textPane.setMargin(new Insets(5, 5, 5, 5));
        panel.setViewportView(textPane);

        // Font name & size
        StyleConstants.setFontFamily(attributes, LOG_FONT);
        StyleConstants.setFontSize(attributes, 10);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // clearLog //
    //----------//
    /**
     * Clear the display.
     */
    public void clear ()
    {
        textPane.setText("");
        textPane.setCaretPosition(0);
        panel.repaint();
    }

    //---------//
    // display //
    //---------//
    public void display (final Level  level,
                         final String str)
    {
        SwingUtilities.invokeLater(
            new Runnable() {
                    @Override
                    public void run ()
                    {
                        // Color
                        StyleConstants.setForeground(
                            attributes,
                            getLevelColor(level));

                        try {
                            document.insertString(
                                document.getLength(),
                                str + "\n",
                                attributes);
                        } catch (BadLocationException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
    }

    //--------------//
    // getComponent //
    //--------------//
    public JScrollPane getComponent ()
    {
        return panel;
    }

    //---------------//
    // getLevelColor //
    //---------------//
    private Color getLevelColor (Level level)
    {
        if (level.isGreaterOrEqual(Level.ERROR)) {
            return Color.RED;
        } else if (level.isGreaterOrEqual(Level.WARN)) {
            return Color.BLUE;
        } else if (level.isGreaterOrEqual(Level.INFO)) {
            return Color.BLACK;
        } else {
            return Color.GRAY;
        }
    }
}
