//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I n t e g e r P a n e                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
import org.audiveris.omr.util.param.Param;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;

/**
 * A data pane with just one integer.
 *
 * @author Hervé Bitteur
 */
public class IntegerPane
        extends XactDataPane<Integer>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Data labeled field. */
    protected final LIntegerField data;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>IntegerPane</code> object.
     *
     * @param title  pane title string
     * @param parent parent pane if any
     * @param text   data text
     * @param tip    data description
     * @param model  underlying data model (cannot be null)
     */
    public IntegerPane (String title,
                        XactDataPane<Integer> parent,
                        String text,
                        String tip,
                        Param<Integer> model)
    {
        super(title, parent, model);
        data = new LIntegerField(true, text, tip);
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public int defineLayout (PanelBuilder builder,
                             CellConstraints cst,
                             int titleWidth,
                             int r)
    {
        super.defineLayout(builder, cst, titleWidth, r);
        builder.add(data.getLabel(), cst.xyw(3, r, 1));
        builder.add(data.getField(), cst.xyw(titleWidth + 4, r, 1));

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
        data.setEnabled(bool);
    }
}
