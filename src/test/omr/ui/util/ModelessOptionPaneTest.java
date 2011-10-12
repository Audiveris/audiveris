//----------------------------------------------------------------------------//
//                                                                            //
//                M o d e l e s s O p t i o n P a n e T e s t                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author Etiolles
 */
public class ModelessOptionPaneTest
{
    //~ Static fields/initializers ---------------------------------------------

    private static JFrame frame;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ModelessOptionPaneTest object.
     */
    public ModelessOptionPaneTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    @Before
    public void setUp ()
    {
        frame = new JFrame("This is the Parent Frame");
        frame.setBounds(32, 32, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setVisible(true);
    }

    /**
     * Test of showModelessConfirmDialog method, of class ModelessOptionPane.
     */
    @Test
    public void testShowModelessConfirmDialog ()
    {
        System.out.println("showModelessConfirmDialog");

        Component parentComponent = frame;
        Object    message = "This is the dialog message" +
                            "\nCheck that you can select and move the underlying frame"
                            + "\nWhile this dialog remains on top!";
        String    title = "This is the dialog title";
        int       optionType = 0;
        int       result = ModelessOptionPane.showModelessConfirmDialog(
            parentComponent,
            message,
            title,
            optionType);
        System.out.println("result: " + result);

        String str = null;

        if (result == JOptionPane.YES_OPTION) {
            str = "YES_OPTION";
        } else if (result == JOptionPane.NO_OPTION) {
            str = "NO_OPTION";
        } else if (result == JOptionPane.CLOSED_OPTION) {
            str = "CLOSED_OPTION";
        }

        if (str != null) {
            System.out.println("User choice: " + str);
        } else {
            fail("Unknown result value:" + result);
        }
    }
}
