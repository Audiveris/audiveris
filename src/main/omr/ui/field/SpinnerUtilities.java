//-----------------------------------------------------------------------//
//                                                                       //
//                    S p i n n e r U t i l i t i e s                    //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.ui.field;

import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.*;

public class SpinnerUtilities
{

    //-------------------//
    // setRightAlignment //
    //-------------------//
    public static void setRightAlignment (JSpinner spinner)
    {
        JSpinner.DefaultEditor editor;
        editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
    }

    //---------//
    // setList //
    //---------//
    public static void setList(JSpinner spinner,
                               List<?>  values)
    {
        SpinnerModel model = spinner.getModel();
        if (model instanceof SpinnerListModel) {
            ((SpinnerListModel) model).setList(values);
        } else {
            throw new IllegalArgumentException
                ("Spinner model is not a SpinnerListModel");
        }
    }

    //----------------//
    // fixIntegerList //
    //----------------//
    public static void fixIntegerList (final JSpinner spinner)
    {
        JSpinner.DefaultEditor editor;
        editor = (JSpinner.DefaultEditor) spinner.getEditor();
        final JFormattedTextField ftf = editor.getTextField();
        ftf.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enterAction");
        ftf.getActionMap().put("enterAction", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        spinner.setValue(Integer.parseInt(ftf.getText()));
                    } catch (Exception ex) {
                        // Reset to last value
                        ftf.setText(ftf.getValue().toString());
                    }
                }
            });
    }

}
