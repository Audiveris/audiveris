//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         X a c t P a n e                                        //
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

import org.audiveris.omr.util.param.Param;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A transactional data pane is able to host data, check data validity and apply
 * the requested modifications on commit.
 *
 * @param <E> specific data type to handle
 * @author Hervé Bitteur
 */
public abstract class XactPane<E>
        implements ActionListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(XactPane.class);

    /** Resource injection. */
    private static final ResourceMap resources =
            Application.getInstance().getContext().getResourceMap(XactPane.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Box for selecting specific vs inherited data. */
    protected final JCheckBox selBox;

    /** Pane title. */
    protected final JLabel title;

    /** Model parameter. */
    protected Param<E> model;

    /** Parent pane, if any. */
    protected XactPane<E> parent;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>XactDataPane</code> object.
     *
     * @param title pane title
     */
    public XactPane (String title)
    {
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
     * @param builder    the shared panel builder
     * @param titleWidth number of cells for title
     * @param r          initial row value
     * @return final row value
     */
    public int defineLayout (FormBuilder builder,
                             int titleWidth,
                             int r)
    {
        // Draw the specific/inherit box (+ line advance?)
        builder.addRaw(selBox).xyw(3, r, 1);
        builder.addRaw(title).xyw(5, r, titleWidth);

        return r + 2;
    }

    /**
     * Write the parameter into the fields content
     *
     * @param content the data to display
     */
    protected abstract void display (E content);

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

    public Param<? extends E> getModel ()
    {
        return model;
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
    public void setEnabled (boolean bool)
    {
        title.setEnabled(bool);
    }

    /**
     * Set the underlying param model.
     *
     * @param model the underlying model
     */
    public void setModel (Param<E> model)
    {
        this.model = model;
    }

    /**
     * Set the parent pane.
     *
     * @param parent the parent pane
     */
    public void setParent (XactPane<E> parent)
    {
        this.parent = parent;
    }

    /**
     * User selects (or deselects) this pane.
     *
     * @param bool true for selection
     */
    public void setSelected (boolean bool)
    {
        selBox.setSelected(bool);
    }

    /**
     * Set the visible flag for all data fields.
     *
     * @param bool the flag value
     */
    public void setVisible (boolean bool)
    {
        selBox.setVisible(bool);
        title.setVisible(bool);
    }

    @Override
    public String toString ()
    {
        return new StringBuilder(getClass().getSimpleName()) //
                .append(" model:").append(model) //
                .append(" parent:").append(parent) //
                .toString();
    }
}
