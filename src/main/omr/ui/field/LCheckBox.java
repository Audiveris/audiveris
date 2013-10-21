//----------------------------------------------------------------------------//
//                                                                            //
//                              L C h e c k B o x                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

/**
 * Class {@code LCheckBox} is a logical combination of a JLabel and a
 * JCheckBox, a "Labeled Check", where the label describes
 * the dynamic content of the check box.
 *
 * @author Hervé Bitteur
 */
public class LCheckBox
        extends LField<JCheckBox>
{
    //~ Constructors -----------------------------------------------------------

    //-----------//
    // LCheckBox //
    //-----------//
    /**
     * Creates a new LCheckBox object.
     *
     * @param label the string to be used as label text
     * @param tip   the related tool tip text
     */
    public LCheckBox (String label,
                      String tip)
    {
        super(label, tip, new JCheckBox());
    }

    //~ Methods ----------------------------------------------------------------
    //-------------------//
    // addActionListener //
    //-------------------//
    /**
     * Add an action listener to the box.
     *
     * @param listener
     */
    public void addActionListener (ActionListener listener)
    {
        getField()
                .addActionListener(listener);
    }
}
