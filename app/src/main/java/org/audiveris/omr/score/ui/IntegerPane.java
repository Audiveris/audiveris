//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I n t e g e r P a n e                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.score.ui;

import org.audiveris.omr.ui.field.LIntegerField;

import com.jgoodies.forms.builder.FormBuilder;

/**
 * A data pane with just one integer.
 *
 * @author Hervé Bitteur
 */
public class IntegerPane
        extends XactPane<Integer>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Data labeled field. */
    protected final LIntegerField data;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>IntegerPane</code> object.
     *
     * @param title pane title string
     * @param text  data text
     * @param tip   data description
     */
    public IntegerPane (String title,
                        String text,
                        String tip)
    {
        super(title);
        data = new LIntegerField(true, text, tip);
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public int defineLayout (FormBuilder builder,
                             int titleWidth,
                             int r)
    {
        super.defineLayout(builder, titleWidth, r);
        builder.addRaw(data.getLabel()).xyw(5, r, 1);
        builder.addRaw(data.getField()).xyw(titleWidth + 6, r, 1);

        return r + 2;
    }

    @Override
    protected void display (Integer val)
    {
        if (val != null) {
            data.setValue(val);
        } else {
            data.setText("");
        }
    }

    @Override
    protected Integer read ()
    {
        return data.getValue();
    }

    @Override
    public void setEnabled (boolean bool)
    {
        super.setEnabled(bool);
        data.setEnabled(bool);
    }
}
