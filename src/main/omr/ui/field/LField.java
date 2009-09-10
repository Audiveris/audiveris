//----------------------------------------------------------------------------//
//                                                                            //
//                                L F i e l d                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Class <code>LField</code> is a logical combination of a JLabel and an
 * {@link SField}, a "Labelled Field", where the label describes the
 * dynamic content of the field.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LField
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The related label
     */
    protected JLabel label;

    /**
     * The underlying field
     */
    protected SField field;

    //~ Constructors -----------------------------------------------------------

    //--------//
    // LField //
    //--------//
    /**
     * Create an editable labelled field with provided
     * characteristics. Note that the field can later be explicitly set as
     * non editable
     *
     * @param label the string to be used as label text
     * @param tip the related tool tip text
     */
    public LField (String label,
                   String tip)
    {
        this(true, label, tip);
    }

    //--------//
    // LField //
    //--------//
    /**
     * Create a labelled field with initial characteristics
     *
     * @param editable tells whether the field is editable
     * @param label the string to be used as label text
     * @param tip the related tool tip text
     */
    public LField (boolean editable,
                   String  label,
                   String  tip)
    {
        this.label = new JLabel(label, SwingConstants.RIGHT);

        if (tip != null) {
            this.label.setToolTipText(tip);
        }

        field = new SField(editable, tip);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // setEnabled //
    //------------//
    /**
     * Enable or disable the whole label + field structure
     * @param bool
     */
    public void setEnabled (boolean bool)
    {
        label.setEnabled(bool);
        field.setEnabled(bool);
    }

    //----------//
    // getField //
    //----------//
    /**
     * Report the field part
     *
     * @return the field
     */
    public SField getField ()
    {
        return field;
    }

    //----------//
    // getLabel //
    //----------//
    /**
     * Report the label part
     *
     * @return the label
     */
    public JLabel getLabel ()
    {
        return label;
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
        field.setText(text);
    }

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
        return field.getText();
    }

    //------------//
    // setVisible //
    //------------//
    public void setVisible (boolean bool)
    {
        field.setVisible(bool);
        label.setVisible(bool);
    }
}
