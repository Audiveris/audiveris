//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   A d v a n c e d T o p i c s                                  //
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
package org.audiveris.omr.ui.action;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.plugin.PluginsManager;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.ui.util.Panel;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * Class {@code AdvancedTopics} gathers all topics that are relevant for advanced users
 * or developers only.
 *
 * @author Hervé Bitteur
 */
public abstract class AdvancedTopics
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AdvancedTopics.class);

    /** Layout for 2 items. */
    private static final FormLayout layout2 = new FormLayout("68dlu,15dlu,pref", "pref");

    /** Layout for 3 items. */
    private static final FormLayout layout3 = new FormLayout("12dlu,1dlu,65dlu,2dlu,pref", "pref");

    //~ Enumerations -------------------------------------------------------------------------------
    public static enum Topic
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        SAMPLES(constants.useSamples),
        ANNOTATIONS(constants.useAnnotations),
        PLOTS(constants.usePlots),
        SPECIFIC_VIEWS(constants.useSpecificViews),
        SPECIFIC_ITEMS(constants.useSpecificItems),
        WINDOW_LAYOUT(constants.useWindowLayout),
        DEBUG(constants.useDebug);
        //~ Instance fields ------------------------------------------------------------------------

        /** Underlying constant. */
        private final Constant.Boolean constant;

        //~ Constructors ---------------------------------------------------------------------------
        Topic (Constant.Boolean constant)
        {
            this.constant = constant;
        }

        //~ Methods --------------------------------------------------------------------------------
        public String getDescription ()
        {
            return constant.getDescription();
        }

        public boolean isSet ()
        {
            return constant.isSet();
        }

        public void set (boolean val)
        {
            constant.setValue(val);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the selection dialog to be displayed to the user
     *
     * @return the dialog frame
     */
    public static JFrame getComponent ()
    {
        final JFrame frame = new JFrame();
        frame.setName("topicsFrame");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JComponent framePane = (JComponent) frame.getContentPane();

        Panel panel = new Panel();
        FormLayout layout = new FormLayout("pref", "pref, 1dlu, pref, 1dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout, panel);
        CellConstraints cst = new CellConstraints();
        int r = 1;
        builder.add(new EarlyPane(), cst.xy(1, r));

        r += 2;
        builder.add(new PluginPane(), cst.xy(1, r));

        r += 2;
        builder.add(new AllTopicsPane(), cst.xy(1, r));

        framePane.add(panel);

        // Resources injection
        ResourceMap resource = Application.getInstance().getContext()
                .getResourceMap(AdvancedTopics.class);
        resource.injectComponents(frame);

        return frame;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------//
    // AllTopicsPane //
    //---------------//
    /**
     * Pane for the advanced topic switches.
     */
    private static final class AllTopicsPane
            extends JPanel
    {
        //~ Constructors ---------------------------------------------------------------------------

        public AllTopicsPane ()
        {
            setBorder(BorderFactory.createTitledBorder("These switches require a restart"));

            FormLayout layout = new FormLayout("pref", Panel.makeRows(Topic.values().length));
            PanelBuilder builder = new PanelBuilder(layout, this);
            CellConstraints cst = new CellConstraints();
            int r = 1;

            for (Topic topic : Topic.values()) {
                builder.add(new TopicPane(topic), cst.xy(1, r));
                r += 2;
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean useSamples = new Constant.Boolean(
                false,
                "Handling of samples repositories and classifier");

        private final Constant.Boolean useAnnotations = new Constant.Boolean(
                false,
                "Production of image annotation with symbols");

        private final Constant.Boolean usePlots = new Constant.Boolean(
                false,
                "Display of scale / stem / staves plots");

        private final Constant.Boolean useSpecificViews = new Constant.Boolean(
                false,
                "Display of specific sheet views");

        private final Constant.Boolean useSpecificItems = new Constant.Boolean(
                false,
                "Specific items shown in sheet view");

        private final Constant.Boolean useWindowLayout = new Constant.Boolean(
                false,
                "Handling of main window layout");

        private final Constant.Boolean useDebug = new Constant.Boolean(
                false,
                "Support for debug features");
    }

    //-----------//
    // EarlyPane //
    //-----------//
    /**
     * Which step should we trigger on any input image.
     */
    private static class EarlyPane
            extends Panel
            implements ActionListener
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final JComboBox<Step> box; // ComboBox for desired step

        //~ Constructors ---------------------------------------------------------------------------
        public EarlyPane ()
        {
            box = new JComboBox<Step>(Step.values());
            box.setToolTipText("Which step to trigger on any image input");
            box.setSelectedItem(StubsController.getEarlyStep());
            box.addActionListener(this);

            PanelBuilder builder = new PanelBuilder(layout2, this);
            CellConstraints cst = new CellConstraints();

            final int r = 1;
            builder.add(box, cst.xy(1, r));
            builder.add(new JLabel("Step triggered on image input"), cst.xy(3, r));
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            Step step = box.getItemAt(box.getSelectedIndex());
            StubsController.setEarlyStep(step);
        }
    }

    //------------//
    // PluginPane //
    //------------//
    /**
     * Which plugin should be the default one.
     */
    private static class PluginPane
            extends Panel
            implements ActionListener
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final JComboBox<String> box; // ComboBox for registered plugins

        //~ Constructors ---------------------------------------------------------------------------
        public PluginPane ()
        {
            // ComboBox for triggered step
            box = new JComboBox<String>(
                    PluginsManager.getInstance().getPluginIds().toArray(new String[0]));
            box.setToolTipText("Default plugin to be launched");
            box.setSelectedItem(PluginsManager.defaultPluginId.getValue());

            PanelBuilder builder = new PanelBuilder(layout2, this);
            CellConstraints cst = new CellConstraints();

            final int r = 1;
            builder.add(box, cst.xy(1, r));
            builder.add(new JLabel("Plugin launched on MusicXML output"), cst.xy(3, r));
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            PluginsManager.defaultPluginId.setSpecific(box.getItemAt(box.getSelectedIndex()));
        }
    }

    //-----------//
    // TopicPane //
    //-----------//
    /**
     * Handling of one topic switch.
     */
    private static final class TopicPane
            extends Panel
            implements ActionListener
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Topic topic; // Handled topic

        //~ Constructors ---------------------------------------------------------------------------
        public TopicPane (Topic topic)
        {
            this.topic = topic;

            PanelBuilder builder = new PanelBuilder(layout3, this);
            CellConstraints cst = new CellConstraints();

            JCheckBox box = new JCheckBox();
            box.addActionListener(this);
            box.setSelected(topic.isSet());

            final int r = 1;
            builder.add(box, cst.xy(1, r));
            builder.add(new JLabel(topic.name()), cst.xy(3, r));
            builder.add(new JLabel(topic.getDescription()), cst.xy(5, r));
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            JCheckBox box = (JCheckBox) e.getSource();
            topic.set(box.isSelected());
        }
    }
}
