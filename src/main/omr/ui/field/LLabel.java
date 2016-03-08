//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           L L a b e l                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.field;

import javax.swing.JLabel;

/**
 * Class {@code LLabel} is a labeled label.
 *
 * @author Hervé Bitteur
 */
public class LLabel
        extends LField<JLabel>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code LLabel} object.
     *
     * @param label the string to be used as label text
     * @param tip   the related tool tip text
     */
    public LLabel (String label,
                   String tip)
    {
        super(label, tip, new JLabel());
        getField().setHorizontalAlignment(JLabel.CENTER);
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
        return getField().getText();
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
        getField().setText(text);
    }
}
