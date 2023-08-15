//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   A d v a n c e d T o p i c s                                  //
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
package org.audiveris.omr.ui.action;

import org.audiveris.omr.Main;
import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.plugin.PluginsManager;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.LabeledEnum;

import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class <code>AdvancedTopics</code> gathers all topics that are relevant for advanced users
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
    private static final FormLayout layout2 = new FormLayout("pref,15dlu,pref", "pref");

    /** Layout for 3 items. */
    private static final FormLayout layout3 = new FormLayout("12dlu,1dlu,pref,10dlu,pref", "pref");

    private static final FormLayout layout3_2 = new FormLayout(
            "12dlu,1dlu,pref,10dlu,pref",
            "pref,pref,pref");

    private static final ApplicationContext context = Application.getInstance().getContext();

    private static final ResourceMap resource = context.getResourceMap(AdvancedTopics.class);

    //~ Constructors -------------------------------------------------------------------------------

    /** Not meant to be instantiated. */
    private AdvancedTopics ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------//
    // getMessage //
    //------------//
    /**
     * Report the message panel to be displayed to the user
     *
     * @return the panel
     */
    private static JPanel getMessage ()
    {
        Panel panel = new Panel();
        panel.setName("AdvancedTopicsPanel");

        FormLayout layout = new FormLayout("pref", "pref, 1dlu, pref, 1dlu, pref, 1dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout, panel);
        CellConstraints cst = new CellConstraints();
        int r = 1;
        builder.add(new EarlyPane(), cst.xy(1, r));

        r += 2;
        builder.add(new PluginPane(), cst.xy(1, r));

        r += 2;
        builder.add(new OutputsPane(), cst.xy(1, r));

        r += 2;
        builder.add(new AllTopicsPane(), cst.xy(1, r));

        return panel;
    }

    //------//
    // show //
    //------//
    public static void show ()
    {
        OMR.gui.displayMessage(getMessage(), resource.getString("AdvancedTopics.title"));
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------------//
    // AllTopicsPane //
    //---------------//
    /**
     * Pane for the advanced topic switches.
     */
    private static class AllTopicsPane
            extends JPanel
    {

        AllTopicsPane ()
        {
            final String className = getClass().getSimpleName();
            setBorder(
                    BorderFactory.createTitledBorder(
                            resource.getString(className + ".titledBorder.text")));

            // Localized values of Topic enum type
            final LabeledEnum<Topic>[] localeTopics = LabeledEnum.values(
                    Topic.values(),
                    resource,
                    Topic.class);

            // Layout
            FormLayout layout = new FormLayout("pref", Panel.makeRows(2 + Topic.values().length));
            PanelBuilder builder = new PanelBuilder(layout, this);
            CellConstraints cst = new CellConstraints();
            int r = 1;

            // Scaling
            builder.add(new ScalingPane(), cst.xy(1, r));
            r += 2;

            // Locale
            builder.add(new LocalePane(), cst.xy(1, r));
            r += 2;

            // Switches
            for (Topic topic : Topic.values()) {
                String topicName = LabeledEnum.valueOf(topic, localeTopics).label;
                builder.add(new TopicPane(topic, topicName), cst.xy(1, r));
                r += 2;
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

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

        // ComboBox for desired step
        private final JComboBox<OmrStep> stepBox;

        EarlyPane ()
        {
            final String className = getClass().getSimpleName();
            final String tip = resource.getString(className + ".stepBox.toolTipText");

            // Define stepBox
            stepBox = new JComboBox<>(OmrStep.values());
            stepBox.setToolTipText(tip);
            stepBox.addActionListener(this);

            // Layout
            PanelBuilder builder = new PanelBuilder(layout2, this);
            CellConstraints cst = new CellConstraints();
            builder.add(stepBox, cst.xy(1, 1));
            builder.add(new JLabel(tip), cst.xy(3, 1));

            // Initial status
            stepBox.setSelectedItem(StubsController.getEarlyStep());
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            OmrStep step = stepBox.getItemAt(stepBox.getSelectedIndex());
            StubsController.setEarlyStep(step);
        }
    }

    //------------//
    // LocalePane //
    //------------//
    /**
     * Which Locale should be used in application.
     */
    private static class LocalePane
            extends Panel
            implements ActionListener
    {

        private static final List<Locale> locales = Main.getSupportedLocales();

        // ComboBox for supported locales
        private final JComboBox<Locale> localeBox;

        LocalePane ()
        {
            final String className = getClass().getSimpleName();
            final String tip = resource.getString(className + ".localeBox.toolTipText");

            // Define localeBox
            localeBox = new JComboBox<>(locales.toArray(new Locale[locales.size()]));
            localeBox.setToolTipText(tip);
            localeBox.addActionListener(this);

            // Layout
            PanelBuilder builder = new PanelBuilder(layout3, this);
            CellConstraints cst = new CellConstraints();
            builder.add(localeBox, cst.xyw(1, 1, 3));
            builder.add(new JLabel(tip), cst.xy(5, 1));

            // Initial status
            localeBox.setSelectedItem(Locale.getDefault());
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            Locale locale = localeBox.getItemAt(localeBox.getSelectedIndex());
            Main.setLocale(locale);
        }
    }

    //------------//
    // OutputPane //
    //------------//
    /**
     * Handling of output switch.
     */
    private static abstract class OutputPane
            extends Panel
            implements ActionListener
    {

        final JCheckBox box;

        final JLabel name;

        final JLabel desc;

        OutputPane ()
        {
            box = new JCheckBox();
            box.addActionListener(this);

            final String className = getClass().getSimpleName();
            name = new JLabel(resource.getString(className + ".text"));
            desc = new JLabel(resource.getString(className + ".desc"));
            name.setToolTipText(resource.getString(className + ".toolTipText"));

            // Layout
            PanelBuilder builder = new PanelBuilder(layout3, this);
            CellConstraints cst = new CellConstraints();
            builder.add(box, cst.xy(1, 1));
            builder.add(name, cst.xy(3, 1));
            builder.add(desc, cst.xy(5, 1));
        }

        @Override
        public void setEnabled (boolean enabled)
        {
            super.setEnabled(enabled);
            box.setEnabled(enabled);
            name.setEnabled(enabled);
            desc.setEnabled(enabled);
        }
    }

    //-------------//
    // OutputsPane //
    //-------------//
    /**
     * Where should outputs be stored.
     */
    private static class OutputsPane
            extends Panel
    {
        final SeparatePane separatePane = new SeparatePane();

        final SiblingPane siblingPane = new SiblingPane(separatePane);

        OutputsPane ()
        {
            setInsets(12, 6, 6, 6);
            final String className = getClass().getSimpleName();
            setBorder(
                    BorderFactory.createTitledBorder(
                            resource.getString(className + ".titledBorder.text")));

            // Layout
            FormLayout layout = new FormLayout("pref", Panel.makeRows(2));
            PanelBuilder builder = new PanelBuilder(layout, this);
            CellConstraints cst = new CellConstraints();
            int r = 1;
            builder.add(siblingPane, cst.xy(1, r));

            r += 2;
            builder.add(separatePane, cst.xy(1, r));
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

        // ComboBox for registered plugins
        private final JComboBox<String> pluginBox;

        PluginPane ()
        {
            final String className = getClass().getSimpleName();
            final String tip = resource.getString(className + ".pluginBox.toolTipText");

            // Define pluginBox
            final Collection<String> ids = PluginsManager.getInstance().getPluginIds();
            pluginBox = new JComboBox<>(ids.toArray(new String[ids.size()]));
            pluginBox.setToolTipText(tip);
            pluginBox.addActionListener(this);

            // Layout
            PanelBuilder builder = new PanelBuilder(layout2, this);
            CellConstraints cst = new CellConstraints();
            builder.add(pluginBox, cst.xy(1, 1));
            builder.add(new JLabel(tip), cst.xy(3, 1));

            // Initial status
            pluginBox.setSelectedItem(PluginsManager.defaultPluginId.getValue());
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            PluginsManager.defaultPluginId.setSpecific(
                    pluginBox.getItemAt(pluginBox.getSelectedIndex()));
        }
    }

    //-------------//
    // ScalingPane //
    //-------------//
    /**
     * Handling of global font ratio at application level.
     */
    private static class ScalingPane
            extends Panel
            implements ChangeListener
    {

        private final int defaultSize = UIUtil.getDefaultFontSize();

        private final double min = UIUtil.getMinGlobalFontRatio();

        private final double max = UIUtil.getMaxGlobalFontRatio();

        private final JSlider slider = new JSlider(0, 100); // 0 for min, 100 for max

        private final JLabel label = new JLabel();

        private final String sliderText;

        ScalingPane ()
        {
            String className = getClass().getSimpleName();
            sliderText = resource.getString(className + ".slider.text");

            // Define slider
            slider.setToolTipText(sliderText);
            slider.addChangeListener(this);
            slider.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseReleased (MouseEvent e)
                {
                    // Register ratio value
                    final double ratio = ratioOf(slider.getValue());
                    UIUtil.setGlobalFontRatio(ratio);
                }
            });

            // Layout
            PanelBuilder builder = new PanelBuilder(layout3, this);
            CellConstraints cst = new CellConstraints();
            builder.add(slider, cst.xyw(1, 1, 3));
            builder.add(label, cst.xy(5, 1));

            // Initial status
            final double ratio = UIUtil.getGlobalFontRatio();
            slider.setValue(tickOf(ratio));
            adjustLabelFont(ratio);
        }

        private void adjustLabelFont (double ratio)
        {
            label.setFont(label.getFont().deriveFont((float) ratio * defaultSize));

            final int percent = (int) Math.rint(ratio * 100);
            label.setText(sliderText + " " + percent + "%");
        }

        private double ratioOf (int tick)
        {
            return min + ((tick * (max - min)) / 100);
        }

        @Override
        public void stateChanged (ChangeEvent e)
        {
            final double ratio = ratioOf(slider.getValue());
            adjustLabelFont(ratio);
        }

        private int tickOf (double ratio)
        {
            return (int) Math.rint((100 * (ratio - min)) / (max - min));
        }
    }

    //--------------//
    // SeparatePane //
    //--------------//
    /**
     * Handling of separate switch.
     */
    private static class SeparatePane
            extends OutputPane
    {
        SeparatePane ()
        {
            box.setSelected(BookManager.useSeparateBookFolders().isSet());
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            BookManager.useSeparateBookFolders().setValue(box.isSelected());
        }
    }

    //-------------//
    // SiblingPane //
    //-------------//
    /**
     * Handling of sibling switch.
     */
    private static class SiblingPane
            extends OutputPane
    {

        final SeparatePane separatePane;

        SiblingPane (SeparatePane separatePane)
        {
            this.separatePane = separatePane;
            final boolean isSet = BookManager.useInputBookFolder().isSet();
            box.setSelected(isSet);
            separatePane.setEnabled(!isSet);
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            final boolean isSet = box.isSelected();
            BookManager.useInputBookFolder().setValue(isSet);
            separatePane.setEnabled(!isSet);
        }
    }

    //~ Enumerations -------------------------------------------------------------------------------

    //-------//
    // Topic //
    //-------//
    /**
     * All advanced topics.
     */
    public static enum Topic
    {
        SAMPLES(constants.useSamples),
        ANNOTATIONS(constants.useAnnotations),
        PLOTS(constants.usePlots),
        SPECIFIC_VIEWS(constants.useSpecificViews),
        SPECIFIC_ITEMS(constants.useSpecificItems),
        DEBUG(constants.useDebug);

        /** Underlying constant. */
        private final Constant.Boolean constant;

        Topic (Constant.Boolean constant)
        {
            this.constant = constant;
        }

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

    //-----------//
    // TopicPane //
    //-----------//
    /**
     * Handling of one topic switch.
     */
    private static class TopicPane
            extends Panel
            implements ActionListener
    {

        // Handled topic
        private final Topic topic;

        /**
         * Build a pane for one topic.
         *
         * @param topic     enum Topic
         * @param topicName translated value of enum topic
         */
        TopicPane (Topic topic,
                   String topicName)
        {
            this.topic = topic;

            String desc = resource.getString("Topic." + topic + ".toolTipText");

            if (desc == null) {
                desc = topic.getDescription();
            }

            JCheckBox box = new JCheckBox();
            box.addActionListener(this);
            box.setSelected(topic.isSet());

            // Layout
            PanelBuilder builder = new PanelBuilder(layout3, this);
            CellConstraints cst = new CellConstraints();
            builder.add(box, cst.xy(1, 1));
            builder.add(new JLabel(topicName), cst.xy(3, 1));
            builder.add(new JLabel(desc), cst.xy(5, 1));
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            JCheckBox box = (JCheckBox) e.getSource();
            topic.set(box.isSelected());
        }
    }
}
