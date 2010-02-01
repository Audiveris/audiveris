//----------------------------------------------------------------------------//
//                                                                            //
//                             L C o m b o B o x                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import java.awt.event.ActionListener;

import javax.swing.*;

/**
 * Class <code>LComboBox</code> is a logical combination of a JLabel and a
 * JComboBox, a "Labelled Combo", where the label describes
 * the dynamic content of the combo.
 *
 * @author Hervé Bitteur
 */
public class LComboBox
    extends LField<JComboBox>
{
    //~ Constructors -----------------------------------------------------------

    //-----------//
    // LComboBox //
    //-----------//
    /**
     * Create an editable labelled combo with provided
     * characteristics.
     *
     * @param label the string to be used as label text
     * @param tip the related tool tip text
     * @param items the items handled by the combo
     */
    public LComboBox (String   label,
                      String   tip,
                      Object[] items)
    {
        super(label, tip, new JComboBox(items));
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // setSelectedItem //
    //-----------------//
    public void setSelectedItem (Object object)
    {
        getField()
            .setSelectedItem(object);
    }

    //-----------------//
    // getSelectedItem //
    //-----------------//
    public Object getSelectedItem ()
    {
        return getField()
                   .getSelectedItem();
    }

    //-------------------//
    // addActionListener //
    //-------------------//
    /**
     * Add an action listener to the combo
     *
     * @param listener
     */
    public void addActionListener (ActionListener listener)
    {
        getField()
            .addActionListener(listener);
    }
}
