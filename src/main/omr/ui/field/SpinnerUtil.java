//----------------------------------------------------------------------------//
//                                                                            //
//                            S p i n n e r U t i l                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;

/**
 * Class {@code SpinnerUtil} gathers a few utilities for JSpinner
 * entities
 *
 * @author Hervé Bitteur
 */
public class SpinnerUtil
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific value to indicate that spinner value is invalid : {@value} */
    public static final int NO_VALUE = 0;

    //~ Constructors -----------------------------------------------------------
    private SpinnerUtil ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // fixIntegerList //
    //----------------//
    /**
     * Workaround for a swing bug : when the user enters an illegal value, the
     * text is forced to the last value.
     *
     * @param spinner the spinner to update
     */
    public static void fixIntegerList (final JSpinner spinner)
    {
        JSpinner.DefaultEditor editor;
        editor = (JSpinner.DefaultEditor) spinner.getEditor();

        final JFormattedTextField ftf = editor.getTextField();
        ftf.getInputMap()
                .put(KeyStroke.getKeyStroke("ENTER"), "enterAction");
        ftf.getActionMap()
                .put(
                "enterAction",
                new AbstractAction()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                try {
                    spinner.setValue(Integer.parseInt(ftf.getText()));
                } catch (Exception ex) {
                    // Reset to last value
                    ftf.setText(ftf.getValue().toString());
                }
            }
        });
    }

    //-------------//
    // setEditable //
    //-------------//
    /**
     * Make the spinner text field editable, or not
     *
     * @param spinner the spinner to update
     * @param bool    true if editable, false otherwise
     */
    public static void setEditable (JSpinner spinner,
                                    boolean bool)
    {
        JSpinner.DefaultEditor editor;
        editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField()
                .setEditable(bool);
    }

    //---------//
    // setList //
    //---------//
    /**
     * Assign the List model (for a list-based spinner)
     *
     * @param spinner the spinner to update
     * @param values  the model list values
     */
    public static void setList (JSpinner spinner,
                                List<?> values)
    {
        SpinnerModel model = spinner.getModel();

        if (model instanceof SpinnerListModel) {
            ((SpinnerListModel) model).setList(values);
        } else {
            throw new IllegalArgumentException(
                    "Spinner model is not a SpinnerListModel");
        }
    }

    //-------------------//
    // setRightAlignment //
    //-------------------//
    /**
     * Align the spinner display to the right
     *
     * @param spinner the spinner to update
     */
    public static void setRightAlignment (JSpinner spinner)
    {
        JSpinner.DefaultEditor editor;
        editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField()
                .setHorizontalAlignment(JTextField.RIGHT);
    }
}
