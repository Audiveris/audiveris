//----------------------------------------------------------------------------//
//                                                                            //
//                                L F i e l d                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Class {@code LField} is a kind of "Labelled Field", a logical
 * composition of a label and a component, which are handled as a whole.
 *
 * @param <C> the precise subtype of the JComponent field
 * <img src="doc-files/Fields.jpg" />
 *
 * @author Hervé Bitteur
 */
public class LField<C extends JComponent>
{
    //~ Instance fields --------------------------------------------------------

    /** The label */
    private final JLabel label;

    /** The field */
    private final C field;

    //~ Constructors -----------------------------------------------------------
    //--------//
    // LField //
    //--------//
    /**
     * Creates a new LField object.
     *
     * @param label the string to be used as label text
     * @param tip   the related tool tip text
     * @param field the field instance
     */
    public LField (String label,
                   String tip,
                   C field)
    {
        this.label = new JLabel(label, SwingConstants.RIGHT);

        if (tip != null) {
            this.label.setToolTipText(tip);
        }

        this.field = field;
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // getField //
    //----------//
    /**
     * Getter for the field.
     *
     * @return the field
     */
    public C getField ()
    {
        return field;
    }

    //----------//
    // getLabel //
    //----------//
    /**
     * Getter for the label.
     *
     * @return the label
     */
    public JLabel getLabel ()
    {
        return label;
    }

    //------------//
    // setEnabled //
    //------------//
    /**
     * Enable or disable the whole label + field structure.
     *
     * @param bool true for enable, false for disable
     */
    public void setEnabled (boolean bool)
    {
        label.setEnabled(bool);
        field.setEnabled(bool);
    }

    //------------//
    // setVisible //
    //------------//
    /**
     * Make the whole label + field structure visible or not.
     *
     * @param bool true for visible, false for non visible
     */
    public void setVisible (boolean bool)
    {
        label.setVisible(bool);
        field.setVisible(bool);
    }
}
