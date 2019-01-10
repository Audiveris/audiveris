//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S h e e t P a r a m e t e r s                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.Item;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.util.param.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Class {@code SheetParameters} is a dialog that allows the user to manage sheet
 * parameters (such as scaling data).
 *
 * @author Hervé Bitteur
 */
public class SheetParameters
{

    private static final Logger logger = LoggerFactory.getLogger(SheetParameters.class);

    /** The swing component of this panel. */
    private final ScopedPanel scopedPanel;

    /** The related sheet. */
    private final Sheet sheet;

    /** Underlying scale structure. */
    private Scale scale;

    /** Map of scaling parameters. */
    private final EnumMap<Item, ScalingParam> scalings = new EnumMap<>(Item.class);

    /**
     * Creates a new {@code SheetParameters} object.
     *
     * @param sheet the underlying sheet
     */
    public SheetParameters (Sheet sheet)
    {
        this.sheet = sheet;
        scale = sheet.getScale();

        // Populate scalings with current scale values
        populateScalings();

        final List<XactDataPane> sheetPanes = new ArrayList<>();

        for (Item key : Item.values()) {
            ScalingParam ip = scalings.get(key);

            if (ip == null) {
                ip = new ScalingParam(key);
            }

            sheetPanes.add(new ScalingPane(key, null, ip));
        }

        scopedPanel = new ScopedPanel("Sheet settings", sheetPanes);

        initialDisplay();
    }

    //--------//
    // commit //
    //--------//
    /**
     * Commit the user actions.
     *
     * @return true if committed, false otherwise
     */
    public boolean commit ()
    {
        try {
            // Commit all specific values, if any, to their model object
            boolean modified = false;

            for (XactDataPane pane : scopedPanel.getPanes()) {
                modified |= pane.commit();
            }

            // Book/Sheet modifications
            if (modified) {
                sheet.getStub().setModified(true);
            }
        } catch (Exception ex) {
            logger.warn("Could not commit sheet parameters", ex);

            return false;
        }

        return true;
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component.
     *
     * @return the concrete component
     */
    public ScopedPanel getComponent ()
    {
        return scopedPanel;
    }

    //----------------//
    // initialDisplay //
    //----------------//
    private void initialDisplay ()
    {
        for (XactDataPane pane : scopedPanel.getPanes()) {
            pane.actionPerformed(null);
        }
    }

    //------------------//
    // populateScalings //
    //------------------//
    private void populateScalings ()
    {
        for (Item key : Item.values()) {
            ScalingParam ip = new ScalingParam(key);
            scalings.put(key, ip);
        }
    }

    //--------------//
    // ScalingParam //
    //--------------//
    /**
     * An integer {@code Param}, backed by Scale structure.
     */
    private class ScalingParam
            extends Param<Integer>
    {

        public final Item key;

        ScalingParam (Item key)
        {
            this.key = key;
        }

        @Override
        public Integer getSourceValue ()
        {
            if (scale == null) {
                return null;
            }

            return scale.getItemValue(key);
        }

        @Override
        public Integer getValue ()
        {
            if (isSpecific()) {
                return getSpecific();
            }

            return getSourceValue();
        }

        @Override
        public boolean setSpecific (Integer specific)
        {
            final Integer value = getValue();
            this.specific = specific;

            if ((specific != null) && !specific.equals(value)) {
                if (scale == null) {
                    sheet.setScale(scale = new Scale());
                }

                scale.setItemValue(key, specific);
                logger.info(key.getDescription() + " set to {}", specific);

                return true;
            }

            return false;
        }
    }

    //-------------//
    // ScalingPane //
    //-------------//
    /**
     * Pane to define a scaling parameter (sheet scope only).
     */
    private static class ScalingPane
            extends IntegerPane
    {

        final Scale.Item key;

        ScalingPane (Scale.Item key,
                     XactDataPane parent,
                     ScalingParam model)
        {
            super(key.getDescription(), parent, "", null, model);
            this.key = key;
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            if ((e != null) && (e.getSource() == data.getField())) {
                display(read());
            } else {
                super.actionPerformed(e);
            }
        }

        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            // Draw the specific/inherit box
            builder.add(selBox, cst.xyw(1, r, 1));
            builder.add(separator, cst.xyw(3, r, 3));
            builder.add(data.getField(), cst.xyw(7, r, 1));

            return r + 2;
        }

        public Scale.Item getKey ()
        {
            return key;
        }

        @Override
        public int getLogicalRowCount ()
        {
            return 1;
        }
    }
}