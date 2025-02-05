//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  B o o k P a r a m e t e r s                                   //
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

import org.audiveris.omr.image.AdaptiveDescriptor;
import org.audiveris.omr.image.FilterDescriptor;
import org.audiveris.omr.image.FilterKind;
import static org.audiveris.omr.image.FilterKind.ADAPTIVE;
import static org.audiveris.omr.image.FilterKind.GLOBAL;
import org.audiveris.omr.image.GlobalDescriptor;
import org.audiveris.omr.score.ui.IntegerSpinPane.SpinData;
import org.audiveris.omr.sheet.BarlineHeight;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.ProcessingSwitch;
import org.audiveris.omr.sheet.ProcessingSwitches;
import org.audiveris.omr.sheet.Profiles;
import org.audiveris.omr.sheet.Profiles.InputQuality;
import org.audiveris.omr.sheet.ScaleBuilder;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.SheetAssembly;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.sheet.ui.SheetView;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.text.Language;
import org.audiveris.omr.text.OcrUtil;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.ui.util.ComboBoxTipRenderer;
import static org.audiveris.omr.util.param.Param.GLOBAL_SCOPE;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Class <code>BookParameters</code> provides a user dialog to manage specifications at
 * global, book and sheet scopes.
 * <p>
 * The specifications contain:
 * <ul>
 * <li>Music font</li>
 * <li>Text font</li>
 * <li>Input quality</li>
 * <li>OCR languages</li>
 * <li>Binarization</li>
 * <li>Scaling</li>
 * <li>Staves</li>
 * <li>Processing switches</li>
 * </ul>
 * <div style="float: right;">
 * <img src="doc-files/ScoreParameters-img.png" alt="Score parameters dialog">
 * </div>
 * <p>
 * The dialog is organized as a scope-based tabbed pane with:
 * <ol>
 * <li>A panel for the <b>default</b> scope,</li>
 * <li>A panel for current <b>book</b> scope (provided that there is a selected book),</li>
 * <li>And one panel for every <b>sheet</b> scope (provided that the current book contains more than
 * a single sheet).</li>
 * </ol>
 * Panel interface:
 * <ul>
 * <li>A panel is a vertical collection of panes, each pane being introduced by a check box
 * and a label.
 * <li>With no specific information, the box is unchecked, the pane content is disabled.
 * <li>With specific information, the box is checked and the pane content is enabled.
 * <li>
 * Manually checking the box represents a selection and indicates the intention to modify the
 * pane content (and thus enables the pane fields).
 * <li>
 * Un-checking the box reverts the content to the value it had prior to the selection, that is the
 * value inherited from the upper scope.
 * </ul>
 * <p>
 * NOTA: The selected modifications are actually applied only when the user presses either the OK or
 * the APPLY button.
 * <p>
 * <img src="doc-files/ScoreParameters.png" alt="Score parameters dialog">
 *
 * @author Hervé Bitteur
 */
public class BookParameters
        implements ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BookParameters.class);

    /** Resource injection. */
    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(BookParameters.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The swing container of this entity. */
    private final JScrollPane scrollPane = new JScrollPane();

    /** One tab per scope. */
    private final JTabbedPane component = new JTabbedPane();

    /** The related book, if any. */
    private final Book book;

    /** The topics panels. */
    private final Map<Object, TopicsPanel> panels = new HashMap<>();

    /** The XactPanes, per scope. */
    private final Map<Object, XactPanes> xactPanes = new HashMap<>();

    /** The pane for barline height on 1-line staves, per scope. */
    private final Map<Object, XactPane> barlinePanes = new HashMap<>();

    /** The pane for interline, per scope. */
    private final Map<Object, XactPane> interlinePanes = new HashMap<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a BookParameters object.
     *
     * @param stub the current sheet stub, or null
     */
    public BookParameters (SheetStub stub)
    {
        scrollPane.setViewportView(component);

        // Default panel
        final XactPanes defaultPanes = new DefaultPanes();
        xactPanes.put(GLOBAL_SCOPE, defaultPanes);

        final TopicsPanel defaultPanel = new TopicsPanel(
                resources.getString("defaultTab.toolTipText"),
                buildTopics(GLOBAL_SCOPE, null, defaultPanes, false, false), // No interline, no beam
                resources);
        final String defaultTitle = resources.getString("defaultTab.text");
        component.addTab(defaultTitle, null, defaultPanel, defaultPanel.getName());
        panels.put(GLOBAL_SCOPE, defaultPanel);

        // Book panel?
        book = (stub != null) ? stub.getBook() : null;
        if (book != null) {
            final XactPanes bookPanes = new BookPanes(book);
            xactPanes.put(book, bookPanes);

            final TopicsPanel bookPanel = new TopicsPanel(
                    resources.getString("bookTab.toolTipText"),
                    buildTopics(book, GLOBAL_SCOPE, bookPanes, true, true),
                    resources);
            component.addTab(book.getRadix(), null, bookPanel, bookPanel.getName());
            panels.put(book, bookPanel);

            // Sheets panels?
            if (book.isMultiSheet()) {
                for (SheetStub s : book.getStubs()) {
                    final XactPanes sheetPanes = new SheetPanes(s);
                    xactPanes.put(s, sheetPanes);

                    final TopicsPanel sheetPanel = new TopicsPanel(
                            MessageFormat.format(
                                    resources.getString("sheetTab.toolTipText"),
                                    s.getNum()),
                            buildTopics(s, book, sheetPanes, true, true),
                            resources);
                    final String initial = resources.getString("sheetInitialChar");
                    String label = initial + "#" + s.getNumber();

                    if (s == stub) {
                        label = "*" + label + "*"; // Currently selected stub
                    }

                    component.addTab(label, null, sheetPanel, sheetPanel.getName());
                    panels.put(s, sheetPanel);
                }
            }
        }

        // component.setName("BookParametersPane"); <== NO!
        // NOTA: no name is set to component to avoid BSAF to store/restore tab selection
        component.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);

        // Initially selected tab
        component.addChangeListener(this);
        component.setSelectedComponent(
                (panels.get(stub) != null) ? panels.get(stub)
                        : ((panels.get(book) != null) ? panels.get(book)
                                : panels.get(GLOBAL_SCOPE)));
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------------//
    // buildTopics //
    //-------------//
    /**
     * Build the structure of topics and panes for a given scope.
     *
     * @param scope         scope of the structure
     * @param parentScope   scope of the parent structure
     * @param xactPanes     the populated panes for the scope
     * @param withInterline with interline pane?
     * @param withBeam      with beam pane?
     * @return the sequence of topics
     */
    private List<XactTopic> buildTopics (Object scope,
                                         Object parentScope,
                                         XactPanes xactPanes,
                                         boolean withInterline,
                                         boolean withBeam)
    {
        final EnumMap<Tag, XactPane> tagMap = xactPanes.tagMap;
        final List<XactTopic> topics = new ArrayList<>();

        { // General
            final XactTopic topic = new XactTopic(Topic.General.name());
            topics.add(topic);

            topic.add(tagMap.get(Tag.Music));
            topic.add(tagMap.get(Tag.Text));
            topic.add(tagMap.get(Tag.Quality));
        }

        { // Languages
            XactTopic topic = new XactTopic(Topic.Languages.name());
            topics.add(topic);

            final XactPane langPane = tagMap.get(Tag.Lang);
            if (langPane != null) {
                topic.add(langPane);
            }
        }

        { // Binarization
            final XactTopic topic = new XactTopic(Topic.Binarization.name());
            topics.add(topic);

            topic.add(tagMap.get(Tag.Filter));
        }

        { //Scaling
            final XactTopic topic = new XactTopic(Topic.Scaling.name());
            topics.add(topic);

            if (withInterline) {
                final XactPane iPane = tagMap.get(Tag.Interline);
                iPane.setVisible(false);
                interlinePanes.put(scope, iPane);
                topic.add(iPane);
            }

            final XactPane bPane = tagMap.get(Tag.Barline);
            bPane.setVisible(false);
            barlinePanes.put(scope, bPane);
            topic.add(bPane);

            if (withBeam) {
                topic.add(tagMap.get(Tag.Beam));
            }
        }

        final EnumMap<ProcessingSwitch, SwitchPane> switchMap = xactPanes.switchMap;

        { // Staves
            final XactTopic topic = new XactTopic(Topic.Staves.name());
            topics.add(topic);

            for (ProcessingSwitch key : ProcessingSwitch.staffSwitches) {
                topic.add(switchMap.get(key));
            }
        }

        { // Items
            final XactTopic topic = new XactTopic(Topic.Items.name());
            topics.add(topic);

            for (ProcessingSwitch key : ProcessingSwitch.itemSwitches) {
                topic.add(switchMap.get(key));
            }
        }

        { // Processing
            final XactTopic topic = new XactTopic(Topic.Processing.name());
            topics.add(topic);

            for (ProcessingSwitch key : ProcessingSwitch.standardSwitches) {
                topic.add(switchMap.get(key));
            }
        }

        xactPanes.setModels();
        xactPanes.setParents(parentScope);

        return topics;
    }

    //--------//
    // commit //
    //--------//
    /**
     * Commit the user actions.
     *
     * @param book the related book if any, perhaps null
     * @return true if committed, false otherwise
     */
    public boolean commit (Book book)
    {
        try {
            boolean defaultModified = false;
            boolean bookModified = false;
            final Set<Object> modifiedScopes = new LinkedHashSet<>();

            // Commit all specific values, if any, to their model object
            for (int tab = 0, tBreak = component.getTabCount(); tab < tBreak; tab++) {
                final TopicsPanel panel = (TopicsPanel) component.getComponentAt(tab);
                boolean modified = false;

                for (XactPane pane : panel.getPanes()) {
                    if (pane.commit()) {
                        logger.debug(
                                "BookParameters modified tab:{} {} {}",
                                tab,
                                panel.getName(),
                                pane);
                        modifiedScopes.add(pane.getModel().getScope());
                        modified = true;
                    }
                }

                // Default / Book modifications
                if (modified) {
                    if (tab == 0) {
                        defaultModified = true;
                    } else {
                        // Test on book not really needed (unless tabs order gets changed some day)
                        if (book != null) {
                            book.setModified(true);
                            bookModified = true;
                        }
                    }
                }
            }

            if (defaultModified) {
                logger.info("Default parameters committed");
            }

            if (bookModified) {
                logger.info("Book parameters committed");
            }

            if (!modifiedScopes.isEmpty()) {
                // Immediate GUI update if current selected sheet is impacted
                // Non-selected ones will be updated when they get selected
                final SheetStub stub = StubsController.getInstance().getSelectedStub();

                if (stub != null) {
                    final SheetAssembly assembly = stub.getAssembly();
                    final SheetView dataView = assembly.getView(SheetTab.DATA_TAB.label);

                    if (dataView != null) {
                        dataView.getBoardsPane().updateAllBoards();
                        dataView.getScrollView().getView().repaint();
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("Could not commit book parameters {}", ex.toString(), ex);

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
    public JScrollPane getComponent ()
    {
        return scrollPane;
    }

    //----------//
    // getTitle //
    //----------//
    public String getTitle ()
    {
        if (book != null) {
            return MessageFormat.format(resources.getString("bookTitlePattern"), book.getRadix());
        } else {
            return resources.getString("defaultTitle");
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * Method called when a new tab/Panel is selected
     *
     * @param e the event
     */
    @Override
    @SuppressWarnings("unchecked")
    public void stateChanged (ChangeEvent e)
    {
        // Refresh the new current panel
        TopicsPanel panel = (TopicsPanel) component.getSelectedComponent();

        PaneLoop:
        for (XactPane pane : panel.getPanes()) {
            if (!pane.isSelected()) {
                // Use the first parent with any specific value
                XactPane highestPane = pane;
                XactPane p = pane.parent;

                while (p != null) {
                    if (p.isSelected()) {
                        pane.display(p.read());

                        continue PaneLoop;
                    }

                    highestPane = p;
                    p = p.parent;
                }

                // No specific data found higher in hierarchy, use source value of highest pane
                final Object srcValue = highestPane.model.getSourceValue();
                pane.display(srcValue);
            }
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //----------------//
    // createLangPane //
    //----------------//
    /**
     * Factory method to get a LangPane, while handling exception when no OCR is available.
     *
     * @return A usable LangPane instance, or null otherwise
     */
    private static LangPane createLangPane ()
    {
        // The language pane needs Tesseract up & running
        if (OcrUtil.getOcr().isAvailable()) {
            try {
                return new LangPane();
            } catch (Throwable ex) {
                logger.warn("Error creating language pane", ex);
            }
        } else {
            logger.info("No language pane for lack of OCR.");
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //------------//
    // FilterPane //
    //------------//
    /**
     * Pane to define the pixel binarization parameters.
     */
    private static class FilterPane
            extends XactPane<FilterDescriptor>
    {
        /** ComboBox for filter kind */
        private final JComboBox<FilterKind> kindCombo = new JComboBox<>(FilterKind.values());

        private final JLabel kindLabel = new JLabel(
                resources.getString("FilterPane.kindLabel.text"),
                SwingConstants.RIGHT);

        // Data for global
        private final SpinData globalData = new SpinData(
                resources.getString("FilterPane.globalData.text"),
                resources.getString("FilterPane.globalData.toolTipText"),
                new SpinnerNumberModel(
                        0,
                        GlobalDescriptor.MINTHRESHOLD,
                        GlobalDescriptor.MAXTHRESHOLD,
                        1));

        // Data for local
        private final SpinData localDataMean = new SpinData(
                resources.getString("FilterPane.localDataMean.text"),
                resources.getString("FilterPane.localDataMean.toolTipText"),
                new SpinnerNumberModel(
                        0.5,
                        AdaptiveDescriptor.MINMEAN,
                        AdaptiveDescriptor.MAXMEAN,
                        0.1));

        private final SpinData localDataDev = new SpinData(
                resources.getString("FilterPane.localDataDev.text"),
                resources.getString("FilterPane.localDataDev.toolTipText"),
                new SpinnerNumberModel(
                        0.2,
                        AdaptiveDescriptor.MINSTDDEV,
                        AdaptiveDescriptor.MAXSTDDEV,
                        0.1));

        FilterPane ()
        {
            super(resources.getString("FilterPane.title"));

            // ComboBox for filter kind
            kindCombo.setToolTipText(resources.getString("FilterPane.kindCombo.toolTipText"));

            // Retrieve the tooltip for each combo value
            final FilterKind[] values = FilterKind.values();
            final String[] tooltips = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                tooltips[i] = resources.getString(
                        "FilterPane.kindCombo." + values[i] + ".toolTipText");
            }

            kindCombo.setRenderer(new ComboBoxTipRenderer(tooltips));
            kindCombo.addActionListener(this);
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            if ((e != null) && (e.getSource() == kindCombo)) {
                FilterDescriptor desc = (readKind() == FilterKind.GLOBAL) ? GlobalDescriptor
                        .getDefault() : AdaptiveDescriptor.getDefault();
                display(desc);
            } else {
                super.actionPerformed(e);
            }

            // Adjust visibility of parameter fields
            switch (readKind()) {
                case GLOBAL -> {
                    localDataMean.setVisible(false);
                    localDataDev.setVisible(false);
                    globalData.setVisible(true);
                }

                case ADAPTIVE -> {
                    globalData.setVisible(false);
                    localDataMean.setVisible(true);
                    localDataDev.setVisible(true);
                }
            }
        }

        /** This is needed to read data manually typed in spinners fields. */
        private void commitSpinners ()
        {
            try {
                switch (readKind()) {
                    case GLOBAL -> globalData.spinner.commitEdit();

                    case ADAPTIVE -> {
                        localDataMean.spinner.commitEdit();
                        localDataDev.spinner.commitEdit();
                    }
                }
            } catch (ParseException ignored) {}
        }

        @Override
        public int defineLayout (FormBuilder builder,
                                 int titleWidth,
                                 int r)
        {
            super.defineLayout(builder, 1, r); // sel + title, no advance

            builder.addRaw(kindLabel).xyw(3, r, 3);
            builder.addRaw(kindCombo).xyw(7, r, 3);
            r += 2;

            // Layout global and local data as mutual overlays
            globalData.defineLayout(builder, r);
            r = localDataMean.defineLayout(builder, r);
            r = localDataDev.defineLayout(builder, r);

            return r;
        }

        @Override
        protected void display (FilterDescriptor desc)
        {
            FilterKind kind = desc.getKind();
            kindCombo.setSelectedItem(kind);

            switch (kind) {
                case GLOBAL -> {
                    GlobalDescriptor globalDesc = (GlobalDescriptor) desc;
                    globalData.spinner.setValue(globalDesc.threshold);
                }

                case ADAPTIVE -> {
                    AdaptiveDescriptor localDesc = (AdaptiveDescriptor) desc;
                    localDataMean.spinner.setValue(localDesc.meanCoeff);
                    localDataDev.spinner.setValue(localDesc.stdDevCoeff);
                }
            }
        }

        @Override
        public int getLogicalRowCount ()
        {
            return 4;
        }

        @Override
        protected FilterDescriptor read ()
        {
            commitSpinners();

            return (readKind() == FilterKind.GLOBAL) ? new GlobalDescriptor(
                    (int) globalData.spinner.getValue())
                    : new AdaptiveDescriptor(
                            (double) localDataMean.spinner.getValue(),
                            (double) localDataDev.spinner.getValue());
        }

        private FilterKind readKind ()
        {
            return kindCombo.getItemAt(kindCombo.getSelectedIndex());
        }

        @Override
        public void setEnabled (boolean bool)
        {
            kindCombo.setEnabled(bool);
            kindLabel.setEnabled(bool);
            globalData.setEnabled(bool);
            localDataMean.setEnabled(bool);
            localDataDev.setEnabled(bool);
        }
    }

    //----------//
    // LangPane //
    //----------//
    /**
     * Pane to set the dominant text language specification.
     */
    private static class LangPane
            extends XactPane<String>
            implements ListSelectionListener
    {
        /** List for choosing elements of language specification. */
        private final JList<String> langList;

        /** Put the list into a scroll pane. */
        private final JScrollPane langScroll;

        /** Resulting visible specification. */
        private final JLabel langSpec = new JLabel("", SwingConstants.RIGHT);

        /** Underlying language list model. */
        final Language.ListModel listModel;

        public LangPane ()
        {
            super("");

            listModel = new Language.ListModel();

            title.setToolTipText(resources.getString("LangPane.toolTipText"));

            langList = new JList<>(listModel);
            langList.setLayoutOrientation(JList.VERTICAL);
            langList.setToolTipText(resources.getString("LangPane.langList.toolTipText"));
            langList.setVisibleRowCount(5);
            langList.addListSelectionListener(this);

            langScroll = new JScrollPane(langList);

            langSpec.setToolTipText(resources.getString("LangPane.langSpec.toolTipText"));
        }

        @Override
        public int defineLayout (FormBuilder builder,
                                 int titleWidth,
                                 int r)
        {
            super.defineLayout(builder, titleWidth, r); // sel + title, no advance

            builder.addRaw(langSpec).xyw(3, r, 3);
            builder.addRaw(langScroll).xyw(7, r, 3);

            return r + 2;
        }

        @Override
        protected void display (String spec)
        {
            final int[] indices = listModel.indicesOf(spec);

            if ((indices.length > 0) && (indices[0] != -1)) {
                // Scroll to first index found?
                String firstElement = listModel.getElementAt(indices[0]);
                langList.setSelectedValue(firstElement, true);

                // Flag all selected indices
                langList.setSelectedIndices(indices);
            }

            langSpec.setText(spec);
        }

        @Override
        protected String read ()
        {
            return Language.specOf(langList.getSelectedValuesList());
        }

        @Override
        public void setEnabled (boolean bool)
        {
            super.setEnabled(bool);
            langList.setEnabled(bool);
            langSpec.setEnabled(bool);
        }

        @Override
        public void valueChanged (ListSelectionEvent e)
        {
            langSpec.setText(read());
        }
    }

    //------------//
    // SwitchPane //
    //------------//
    /**
     * A pane for one processing switch.
     */
    private class SwitchPane
            extends BooleanPane
    {
        final ProcessingSwitch key; // The related switch

        public SwitchPane (ProcessingSwitch key)
        {
            super(textOf(key), tipOf(key));
            this.key = key;
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            if ((e != null) && (e.getSource() == boolBox)) {
                display(read());
            } else {
                super.actionPerformed(e);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void display (Boolean content)
        {
            super.display(content);

            switch (key) {
                case oneLineStaves -> {
                    // We display the barline height specification if and only if
                    // the switch for 1-line staves is ON
                    final Object scope = model.getScope();
                    final XactPane barlinePane = barlinePanes.get(scope);
                    barlinePane.setVisible((content != null) && content);
                }
                case fiveLineStaves, drumNotation -> {
                    // We display the interline pane if and only if both switches
                    // for 5-line standard staves and for 5-line percussion staves are OFF.
                    // Otherwise, it is safer to set the interline spec to zero (i.e. disabled)
                    final Object scope = model.getScope();
                    final XactPane interlinePane = interlinePanes.get(scope);

                    if (interlinePane != null) {
                        final ProcessingSwitch other = (key == ProcessingSwitch.fiveLineStaves)
                                ? ProcessingSwitch.drumNotation
                                : ProcessingSwitch.fiveLineStaves;
                        boolean bothOff = (content == null) || !content;
                        bothOff &= !isSet(scope, other);
                        interlinePane.setVisible(bothOff);

                        if (!bothOff) {
                            interlinePane.display(0); // Warning here
                        }
                    }
                }
            }
        }

        public ProcessingSwitch getKey ()
        {
            return key;
        }

        @Override
        public int getLogicalRowCount ()
        {
            return 1;
        }

        /**
         * Report whether, in the given scope, the provided switch is set.
         *
         * @param scope the given scope
         * @param key   the switch to read
         * @return true if switch is set
         */
        private boolean isSet (Object scope,
                               ProcessingSwitch key)
        {
            final TopicsPanel panel = panels.get(scope);

            if (panel == null) {
                return false;
            }

            for (XactPane pane : panel.getPanes()) {
                if (pane instanceof SwitchPane switchPane) {
                    if (switchPane.key == key) {
                        final Boolean bool = switchPane.read();
                        return (bool != null) && bool;
                    }
                }
            }

            return false; // This statement should never be reached!
        }

        private static String textOf (ProcessingSwitch key)
        {
            // Priority is given to text in resources file if any
            final String desc = resources.getString("Switch." + key + ".text");

            // Fallback using constant description text
            return (desc != null) ? desc : key.getConstant().getDescription();
        }

        private static String tipOf (ProcessingSwitch key)
        {
            return resources.getString("Switch." + key + ".toolTipText");
        }
    }

    //-----//
    // Tag //
    //-----//
    /** Category of pane in each scoped panel. */
    public static enum Tag
    {
        Music,
        Text,
        Quality,
        Lang,
        Filter,
        Interline,
        Barline,
        Beam;
    }

    //-------//
    // Topic //
    //-------//
    /** Just a way to gather parameters by topics. */
    private static enum Topic
    {
        General,
        Languages,
        Binarization,
        Scaling,
        Staves,
        Items,
        Processing;
    }

    //-----------//
    // XactPanes //
    //-----------//
    /**
     * The base class for default, book and sheet collection of panes.
     * <p>
     * It populates the maps of spec panes and switch panes, then connects each individual pane
     * to its underlying param model and to its parent pane if any in the scope above.
     */
    private abstract class XactPanes
    {
        /** Map of specification panes, per tag. */
        public final EnumMap<Tag, XactPane> tagMap = new EnumMap<>(Tag.class);

        /** Map of switch panes, per switch key. */
        public final EnumMap<ProcessingSwitch, SwitchPane> switchMap = new EnumMap<>(
                ProcessingSwitch.class);

        protected XactPanes ()
        {
            tagMap.put(Tag.Music, new EnumPane<>(Tag.Music, MusicFamily.values(), resources));
            tagMap.put(Tag.Text, new EnumPane<>(Tag.Text, TextFamily.values(), resources));
            tagMap.put(Tag.Quality, new EnumPane<>(Tag.Quality, InputQuality.values(), resources));

            tagMap.put(Tag.Filter, new FilterPane());

            final LangPane langPane = createLangPane();
            if (langPane != null) {
                tagMap.put(Tag.Lang, langPane);
            }

            final SpinData ilSpin = new SpinData(
                    new SpinnerNumberModel(0, 0, ScaleBuilder.getMaxInterline(), 1));
            tagMap.put(Tag.Interline, new IntegerSpinPane<>(Tag.Interline, ilSpin, resources));

            tagMap.put(Tag.Barline, new EnumPane<>(Tag.Barline, BarlineHeight.values(), resources));

            final SpinData beamSpin = new SpinData(
                    new SpinnerNumberModel(0, 0, ScaleBuilder.getMaxInterline(), 1));
            tagMap.put(Tag.Beam, new IntegerSpinPane<>(Tag.Beam, beamSpin, resources));

            for (ProcessingSwitch key : ProcessingSwitch.supportedSwitches) {
                switchMap.put(key, new SwitchPane(key));
            }
        }

        /** Connect each pane to its underlying param model. */
        public abstract void setModels ();

        /** Connect each switch pane to its underlying switch model. */
        protected void setModels (ProcessingSwitches switches)
        {
            for (SwitchPane switchPane : switchMap.values()) {
                switchPane.setModel(switches.getParam(switchPane.getKey()));
            }
        }

        /** Connect each pane to its parent pane in the scope above. */
        @SuppressWarnings("unchecked")
        public void setParents (Object parentScope)
        {
            if (parentScope != null) {
                final XactPanes parentPanes = xactPanes.get(parentScope);

                final EnumMap<Tag, XactPane> parentMap = parentPanes.tagMap;
                for (Entry<Tag, XactPane> entry : tagMap.entrySet()) {
                    entry.getValue().setParent(parentMap.get(entry.getKey()));
                }

                final EnumMap<ProcessingSwitch, SwitchPane> parentSwitches = parentPanes.switchMap;
                for (SwitchPane switchPane : switchMap.values()) {
                    switchPane.setParent(parentSwitches.get(switchPane.getKey()));
                }
            }
        }
    }

    //--------------//
    // DefaultPanes // A XactPanes for the global scope
    //--------------//
    private class DefaultPanes
            extends XactPanes
    {
        public DefaultPanes ()
        {
            tagMap.remove(Tag.Interline);
            tagMap.remove(Tag.Beam);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setModels ()
        {
            for (Entry<Tag, XactPane> entry : tagMap.entrySet()) {
                switch (entry.getKey()) {
                    case Music -> entry.getValue().setModel(MusicFont.defaultMusicParam);
                    case Text -> entry.getValue().setModel(TextFont.defaultTextParam);
                    case Quality -> entry.getValue().setModel(Profiles.defaultQualityParam);
                    case Lang -> entry.getValue().setModel(Language.ocrDefaultLanguages);
                    case Filter -> entry.getValue().setModel(FilterDescriptor.defaultFilter);
                    // No Interline
                    case Barline -> entry.getValue().setModel(BarlineHeight.defaultParam);
                    // No Beam
                }
            }

            setModels(ProcessingSwitches.getDefaultSwitches());
        }
    }

    //-----------//
    // BookPanes // A XactPanes for the book scope
    //-----------//
    private class BookPanes
            extends XactPanes
    {
        private final Book book;

        public BookPanes (Book book)
        {
            this.book = book;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setModels ()
        {
            for (Entry<Tag, XactPane> entry : tagMap.entrySet()) {
                entry.getValue().setModel(switch (entry.getKey()) {
                    case Music -> book.getMusicFamilyParam();
                    case Text -> book.getTextFamilyParam();
                    case Quality -> book.getInputQualityParam();
                    case Lang -> book.getOcrLanguagesParam();
                    case Filter -> book.getBinarizationParam();
                    case Interline -> book.getInterlineSpecificationParam();
                    case Barline -> book.getBarlineHeightParam();
                    case Beam -> book.getBeamSpecificationParam();
                });
            }

            setModels(book.getProcessingSwitches());
        }
    }

    //------------//
    // SheetPanes // A XactPanes for a sheet stub
    //------------//
    private class SheetPanes
            extends XactPanes
    {
        private final SheetStub stub;

        public SheetPanes (SheetStub stub)
        {
            this.stub = stub;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setModels ()
        {
            for (Entry<Tag, XactPane> entry : tagMap.entrySet()) {
                entry.getValue().setModel(switch (entry.getKey()) {
                    case Music -> stub.getMusicFamilyParam();
                    case Text -> stub.getTextFamilyParam();
                    case Quality -> stub.getInputQualityParam();
                    case Lang -> stub.getOcrLanguagesParam();
                    case Filter -> stub.getBinarizationFilterParam();
                    case Interline -> stub.getInterlineSpecificationParam();
                    case Barline -> stub.getBarlineHeightParam();
                    case Beam -> stub.getBeamSpecificationParam();
                });
            }

            setModels(stub.getProcessingSwitches());
        }
    }
}
