//-----------------------------------------------------------------------//
//                                                                       //
//                  I n t e g e r L i s t S p i n n e r                  //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.ui.field;

import java.util.List;

import javax.swing.*;

/**
 * Class <code>IntegerListSpinner</code> is a spinner whose model is a list of
 * integers
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class IntegerListSpinner
    extends JSpinner
{
    //~ Constructors -----------------------------------------------------------

    //--------------------//
    // IntegerListSpinner //
    //--------------------//
    /**
     * Creates a new IntegerListSpinner object.
     */
    public IntegerListSpinner ()
    {
        setModel(new SpinnerListModel());

        // Right alignment
        JSpinner.DefaultEditor editor;
        editor = (JSpinner.DefaultEditor) getEditor();
        editor.getTextField()
              .setHorizontalAlignment(JTextField.RIGHT);
    }
}
