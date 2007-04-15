//-----------------------------------------------------------------------//
//                                                                       //
//                       L I n t e g e r F i e l d                       //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui.field;

import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Class <code>LIntegerField</code> is an {@link LField}, whose field is
 * meant to handle an integer value.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LIntegerField
    extends LField
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // LIntegerField //
    //---------------//
    /**
     * Create a (constant) integer labelled field
     *
     * @param label string to be used as label text
     * @param tip related tool tip text
     */
    public LIntegerField (String label,
                          String tip)
    {
        super(true, label, tip);
    }

    //---------------//
    // LIntegerField //
    //---------------//
    /**
     * Create an integer labelled field
     *
     * @param editable tells whether the field is editable
     * @param label string to be used as label text
     * @param tip related tool tip text
     */
    public LIntegerField (boolean editable,
                          String  label,
                          String  tip)
    {
        super(editable, label, tip);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // setValue //
    //----------//
    /**
     * Modify the current field
     *
     * @param val the integer value to be used
     */
    public void setValue (int val)
    {
        field.setText(Integer.toString(val));
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Extract the current integer value form the text field (TBD: better
     * handling of exceptions)
     *
     * @return current integer value
     */
    public int getValue ()
    {
        int val = 0;

        try {
            val = Integer.parseInt(field.getText());
        } catch (NumberFormatException ex) {
        }

        return val;
    }
}
