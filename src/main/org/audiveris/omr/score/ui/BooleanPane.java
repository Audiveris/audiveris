//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B o o l e a n P a n e                                     //
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

import org.audiveris.omr.util.param.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;

import javax.swing.JCheckBox;

/**
 * A data pane for just one boolean.
 *
 * @param <T> specific category
 * @author Hervé Bitteur
 */
public class BooleanPane<T extends Enum<T>>
        extends XactPane<T, Boolean>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BooleanPane.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Boolean box. */
    protected final JCheckBox boolBox = new JCheckBox();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>BooleanPane</code> object.
     *
     * @param tag    unique in scope
     * @param title  pane title string
     * @param parent parent pane if any
     * @param tip    data description
     * @param model  underlying data model (cannot be null)
     */
    public BooleanPane (T tag,
                        String title,
                        BooleanPane<T> parent,
                        String tip,
                        Param<Boolean> model)
    {
        super(tag, title, parent, model);

        if ((tip != null) && !tip.isBlank()) {
            boolBox.setToolTipText(tip);
            this.title.setToolTipText(tip);
        }

        boolBox.addActionListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public int defineLayout (PanelBuilder builder,
                             CellConstraints cst,
                             int titleWidth,
                             int r)
    {
        super.defineLayout(builder, cst, titleWidth, r); // No advance
        builder.add(boolBox, cst.xyw(9, r, 1));

        return r + 2;
    }

    @Override
    protected void display (Boolean content)
    {
        if (content != null) {
            boolBox.setSelected(content);
        }
    }

    @Override
    protected Boolean read ()
    {
        return boolBox.isSelected();
    }

    @Override
    public void setEnabled (boolean bool)
    {
        super.setEnabled(bool);
        boolBox.setEnabled(bool);
    }

    @Override
    public void setVisible (boolean bool)
    {
        super.setVisible(bool);
        boolBox.setVisible(bool);
    }
}
