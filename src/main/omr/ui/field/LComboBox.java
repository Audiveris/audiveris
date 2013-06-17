//----------------------------------------------------------------------------//
//                                                                            //
//                             L C o m b o B o x                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import java.awt.event.ActionListener;

import javax.swing.JComboBox;

/**
 * Class {@code LComboBox} is a logical combination of a JLabel and a
 * JComboBox, a "Labelled Combo", where the label describes
 * the dynamic content of the combo.
 *
 * @param <E> type of combo entity
 *
 * @author Hervé Bitteur
 */
public class LComboBox<E>
        extends LField<JComboBox<E>>
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
     * @param tip   the related tool tip text
     * @param items the items handled by the combo
     */
    public LComboBox (String label,
                      String tip,
                      E[] items)
    {
        super(label, tip, new JComboBox<>(items));
    }

    //~ Methods ----------------------------------------------------------------
    //-------------------//
    // addActionListener //
    //-------------------//
    /**
     * Add an action listener to the combo.
     *
     * @param listener
     */
    public void addActionListener (ActionListener listener)
    {
        getField()
                .addActionListener(listener);
    }

    //-----------------//
    // getSelectedItem //
    //-----------------//
    @SuppressWarnings("unchecked")
    public E getSelectedItem ()
    {
        return (E) getField()
                .getSelectedItem();
    }

    //-----------------//
    // setSelectedItem //
    //-----------------//
    public void setSelectedItem (E item)
    {
        getField()
                .setSelectedItem(item);
    }
}
