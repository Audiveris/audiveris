//-----------------------------------------------------------------------//
//                                                                       //
//                  I n t e g e r L i s t S p i n n e r                  //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.ui;

import java.util.List;
import javax.swing.*;

public class IntegerListSpinner
    extends JSpinner
{
    //--------------------//
    // IntegerListSpinner //
    //--------------------//
    public IntegerListSpinner()
    {
        setModel(new SpinnerListModel());

        // Right alignment
        JSpinner.DefaultEditor editor;
        editor = (JSpinner.DefaultEditor) getEditor();
        editor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
    }

    //---------//
    // setList //
    //---------//
    public void setList(List<Integer> values)
    {
        ((SpinnerListModel) getModel()).setList(values);
    }
}
