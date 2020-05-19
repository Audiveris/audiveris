//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B o o l e a n P a n e                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;

import org.audiveris.omr.util.param.Param;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A data pane for just one boolean.
 *
 * @author Hervé Bitteur
 */
public class BooleanPane
        extends XactDataPane<Boolean>
{

    /** Boolean box. */
    protected final JCheckBox bbox = new JCheckBox();

    /** Label for the boolean. */
    protected final JLabel label;

    /**
     * Creates a new {@code BooleanPane} object.
     *
     * @param title  pane title string
     * @param parent parent pane if any
     * @param text   data text
     * @param tip    data description
     * @param model  underlying data model (cannot be null)
     */
    public BooleanPane (String title,
                        XactDataPane<Boolean> parent,
                        String text,
                        String tip,
                        Param<Boolean> model)
    {
        super(title, parent, model);
        this.label = new JLabel(text, SwingConstants.RIGHT);

        if (tip != null) {
            bbox.setToolTipText(tip);
        }
    }

    @Override
    public int defineLayout (PanelBuilder builder,
                             CellConstraints cst,
                             int r)
    {
        r = super.defineLayout(builder, cst, r);
        builder.add(label, cst.xyw(3, r, 1));
        builder.add(bbox, cst.xyw(7, r, 1));

        return r + 2;
    }

    @Override
    public void setEnabled (boolean bool)
    {
        bbox.setEnabled(bool);
        label.setEnabled(bool);
    }

    @Override
    protected void display (Boolean content)
    {
        bbox.setSelected(content);
    }

    @Override
    protected Boolean read ()
    {
        return bbox.isSelected();
    }
}
