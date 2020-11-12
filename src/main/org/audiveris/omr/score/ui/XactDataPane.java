//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     X a c t D a t a P a n e                                    //
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

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A transactional data pane is able to host data, check data validity and apply
 * the requested modifications on commit.
 *
 * @param <E> specific data type
 * @author Hervé Bitteur
 */
public abstract class XactDataPane<E>
        implements ActionListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Resource injection. */
    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(XactDataPane.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Model parameter (cannot be null). */
    protected final Param<E> model;

    /** Parent pane, if any. */
    protected final XactDataPane<E> parent;

    /** Box for selecting specific vs inherited data. */
    protected final JCheckBox selBox;

    /** Pane title. */
    protected final JLabel title;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code XactDataPane} object.
     *
     * @param title  pane title
     * @param parent parent pane if any
     * @param model  underlying model (cannot be null)
     */
    public XactDataPane (String title,
                         XactDataPane<E> parent,
                         Param<E> model)
    {
        if (model == null) {
            throw new IllegalArgumentException("Null model for pane '" + title + "'");
        }

        this.parent = parent;
        this.model = model;
        this.title = new JLabel(title);
        this.title.setHorizontalAlignment(SwingConstants.LEFT);
        this.title.setEnabled(false);

        selBox = new JCheckBox();
        selBox.addActionListener(this);
        selBox.setToolTipText(resources.getString("selBox.toolTipText"));
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void actionPerformed (ActionEvent e)
    {
        // Pane (de)selection (programmatic or manual)
        final boolean sel = isSelected();
        setEnabled(sel);
        title.setEnabled(sel);

        final E value;

        if (e == null) {
            value = model.getValue();
        } else if (!sel) {
            value = (parent != null) ? parent.read() : model.getSourceValue();
        } else {
            return;
        }

        display(value);
    }

    /**
     * Commit the modifications.
     *
     * @return true if underlying model has been modified
     */
    public boolean commit ()
    {
        if (isSelected()) {
            return model.setSpecific(read());
        } else {
            return model.setSpecific(null);
        }
    }

    /**
     * Build the related user interface
     *
     * @param builder the shared panel builder
     * @param cst     the cell constraints
     * @param r       initial row value
     * @return final row value
     */
    public int defineLayout (PanelBuilder builder,
                             CellConstraints cst,
                             int r)
    {
        // Draw the specific/inherit box + separating line
        builder.add(selBox, cst.xyw(1, r, 1));
        builder.add(title, cst.xyw(3, r, 5));

        return r + 2;
    }

    /**
     * Report the count of needed logical rows.
     * Typically 2 (the label separator plus 1 line of data)
     *
     * @return count of layout logical rows
     */
    public int getLogicalRowCount ()
    {
        return 2;
    }

    /**
     * User has selected (and enabled) this pane
     *
     * @return true if selected
     */
    public boolean isSelected ()
    {
        return selBox.isSelected();
    }

    /**
     * User selects (or deselects) this pane
     *
     * @param bool true for selection
     */
    public void setSelected (boolean bool)
    {
        selBox.setSelected(bool);
    }

    /**
     * Write the parameter into the fields content
     *
     * @param content the data to display
     */
    protected abstract void display (E content);

    /**
     * Read the parameter as defined by the fields content.
     *
     * @return the pane parameter
     */
    protected abstract E read ();

    /**
     * Set the enabled flag for all data fields
     *
     * @param bool the flag value
     */
    protected abstract void setEnabled (boolean bool);
}
