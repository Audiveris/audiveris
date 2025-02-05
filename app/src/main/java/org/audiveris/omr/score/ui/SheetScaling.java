//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S h e e t S c a l i n g                                    //
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

import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.Item;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.util.param.Param;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.EnumMap;

/**
 * Class <code>SheetScaling</code> is a dialog that allows the user to display and modify sheet
 * scaling data.
 *
 * @author Hervé Bitteur
 */
public class SheetScaling
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetScaling.class);

    // JGoodies columns specification:     Topic      SelBox     Item             Value
    private static final String colSpec = "10dlu,1dlu,10dlu,1dlu,80dlu,1dlu,right:15dlu";

    /** Resource injection. */
    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(SheetScaling.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The swing component of this entity. */
    private final TopicsPanel topicsPanel;

    /** The related sheet. */
    private final Sheet sheet;

    /** Underlying scale structure. */
    private Scale scale;

    /** Map of scaling parameters. */
    private final EnumMap<Item, ScalingParam> scalings = new EnumMap<>(Item.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>SheetParameters</code> object.
     *
     * @param sheet the underlying sheet
     */
    public SheetScaling (Sheet sheet)
    {
        this.sheet = sheet;
        scale = sheet.getScale();

        // Populate scalings with current scale values
        populateScalings();

        final XactTopic topic = new XactTopic(resources.getString("Scaling.text"));

        for (Item key : Item.values()) {
            ScalingParam ip = scalings.get(key);

            if (ip == null) {
                ip = new ScalingParam(key);
            }

            final ScaledPane scaledPane = new ScaledPane(key);
            scaledPane.setModel(ip);
            topic.add(scaledPane);
        }

        topicsPanel = new TopicsPanel(
                "Sheet settings",
                Arrays.asList(topic),
                resources,
                colSpec,
                1);

        initialDisplay();
    }

    //~ Methods ------------------------------------------------------------------------------------

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

            for (XactPane pane : topicsPanel.getPanes()) {
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
    public TopicsPanel getComponent ()
    {
        return topicsPanel;
    }

    //----------//
    // getTitle //
    //----------//
    public String getTitle ()
    {
        final String pattern = resources.getString("SheetScaling.titlePattern");

        return MessageFormat.format(pattern, sheet.getId());
    }

    //----------------//
    // initialDisplay //
    //----------------//
    private void initialDisplay ()
    {
        for (XactPane pane : topicsPanel.getPanes()) {
            pane.actionPerformed(null);
        }
    }

    //------------------//
    // populateScalings //
    //------------------//
    private void populateScalings ()
    {
        for (Item key : Item.values()) {
            scalings.put(key, new ScalingParam(key));
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //------------//
    // ScaledPane //
    //------------//
    /**
     * Pane to define a scaled parameter.
     */
    private static class ScaledPane
            extends IntegerPane
    {
        /** The scale item handled by this ScaledPane. */
        final Scale.Item key;

        ScaledPane (Scale.Item key)
        {
            super(description(key), "", null);
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
        public int defineLayout (FormBuilder builder,
                                 int titleWidth,
                                 int r)
        {
            super.defineLayout(builder, 1, r);

            builder.addRaw(title).xyw(5, r, 1);
            builder.addRaw(data.getField()).xyw(7, r, 1);

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

        private static String description (Scale.Item key)
        {
            String desc = resources.getString("Item." + key + ".toolTipText");

            if (desc != null) {
                return desc;
            }

            return key.getDescription();
        }
    }

    //--------------//
    // ScalingParam //
    //--------------//
    /**
     * An integer <code>Param</code>, backed by Scale structure.
     */
    private class ScalingParam
            extends Param<Integer>
    {
        public final Item key;

        ScalingParam (Item key)
        {
            super(sheet);
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
                logger.info(key.getDescription() + " set to {}", scale.getItemValue(key));

                return true;
            }

            return false;
        }
    }
}
