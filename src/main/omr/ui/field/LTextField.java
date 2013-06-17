//----------------------------------------------------------------------------//
//                                                                            //
//                            L T e x t F i e l d                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import javax.swing.JTextField;

/**
 * Class {@code LTextField} is a {@link LField}, where the field
 * is a text field.
 *
 * @author Hervé Bitteur
 */
public class LTextField
        extends LField<JTextField>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Defaut number of characters in the text field : {@value} */
    private static final int FIELD_WIDTH = 6;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // LTextField //
    //------------//
    /**
     * Creates a new LTextField object.
     *
     * @param editable Specifies whether this field will be editable
     * @param label    the string to be used as label text
     * @param tip      the related tool tip text
     */
    public LTextField (boolean editable,
                       String label,
                       String tip)
    {
        super(label, tip, new JTextField(FIELD_WIDTH));

        JTextField textField = getField();
        textField.setEditable(editable);

        textField.setHorizontalAlignment(JTextField.CENTER);

        textField.setEditable(editable);
    }

    //------------//
    // LTextField //
    //------------//
    /**
     * Creates a new non-editable LTextField object.
     *
     * @param label the string to be used as label text
     * @param tip   the related tool tip text
     */
    public LTextField (String label,
                       String tip)
    {
        this(false, label, tip);
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // getText //
    //---------//
    /**
     * Report the current content of the field
     *
     * @return the field content
     */
    public String getText ()
    {
        return getField()
                .getText();
    }

    //---------//
    // setText //
    //---------//
    /**
     * Modify the content of the field
     *
     * @param text
     */
    public void setText (String text)
    {
        getField()
                .setText(text);
    }
}
