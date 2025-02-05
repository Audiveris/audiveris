//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  I n t e g e r S p i n P a n e                                 //
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

import org.audiveris.omr.ui.field.SpinnerUtil;

import org.jdesktop.application.ResourceMap;

import com.jgoodies.forms.builder.FormBuilder;

import java.text.ParseException;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SwingConstants;

/**
 * Class <code>IntegerSpinPane</code> is a pane to handle an integer via a spinner.
 *
 * @param <T> specific category
 * @author Hervé Bitteur
 */
public class IntegerSpinPane<T>
        extends XactPane<Integer>
{
    //~ Instance fields ----------------------------------------------------------------------------

    protected final SpinData spinData;

    //~ Constructors -------------------------------------------------------------------------------

    public IntegerSpinPane (T tag,
                            SpinData spinData,
                            ResourceMap resources)
    {
        super(resources.getString(tag + "Pane.title"));
        title.setToolTipText(resources.getString(tag + "Pane.toolTipText"));
        this.spinData = spinData;
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public int defineLayout (FormBuilder builder,
                             int titleWidth,
                             int r)
    {
        super.defineLayout(builder, 1, r); // sel + title, no advance
        spinData.defineLayout(builder, r);

        return r + 2;
    }

    @Override
    protected void display (Integer content)
    {
        if (content != null) {
            spinData.spinner.setValue(content);
        } else {
            spinData.spinner.setValue(0);
        }
    }

    @Override
    protected Integer read ()
    {
        try {
            spinData.spinner.commitEdit();
        } catch (ParseException ignored) {
        }

        return (int) spinData.spinner.getValue();
    }

    @Override
    public void setEnabled (boolean bool)
    {
        super.setEnabled(bool);
        spinData.setEnabled(bool);
    }

    @Override
    public void setVisible (boolean bool)
    {
        super.setVisible(bool);
        spinData.setVisible(bool);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // SpinData //
    //----------//
    /**
     * A line with a labeled spinner.
     */
    public static class SpinData
    {
        protected final JLabel label;

        protected final JSpinner spinner;

        SpinData (SpinnerModel model)
        {
            this("", "", model);
        }

        SpinData (String label,
                  String tip,
                  SpinnerModel model)
        {
            this.label = new JLabel(label, SwingConstants.RIGHT);

            spinner = new JSpinner(model);
            SpinnerUtil.setRightAlignment(spinner);
            SpinnerUtil.setEditable(spinner, true);
            spinner.setToolTipText(tip);
        }

        public int defineLayout (FormBuilder builder,
                                 int r)
        {
            builder.addRaw(label).xyw(5, r, 1);
            builder.addRaw(spinner).xyw(7, r, 3);

            r += 2;

            return r;
        }

        public void setEnabled (boolean bool)
        {
            label.setEnabled(bool);
            spinner.setEnabled(bool);
        }

        public void setVisible (boolean bool)
        {
            label.setVisible(bool);
            spinner.setVisible(bool);
        }
    }
}
