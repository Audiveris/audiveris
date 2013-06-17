//----------------------------------------------------------------------------//
//                                                                            //
//                    I n t e g e r L i s t S p i n n e r                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;

/**
 * Class {@code IntegerListSpinner} is a spinner whose model is a list
 * of integers.
 *
 * @author Hervé Bitteur
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
