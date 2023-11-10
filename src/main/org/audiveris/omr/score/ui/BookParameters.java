//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  B o o k P a r a m e t e r s                                   //
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
import org.audiveris.omr.util.param.Param;
import static org.audiveris.omr.util.param.Param.GLOBAL_SCOPE;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * default, book and sheet scopes.
 * <p>
 * The specifications contain:
 * <ul>
 * <li>Music font</li>
 * <li>Text font</li>
 * <li>Input quality</li>
 * <li>OCR languages</li>
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

    /** Just a way to gather parameters by topics. */
    private static enum Topic
    {
        General,
        Languages,
        Scaling,
        Staves,
        Processing;
    }

    /** Category of pane in each scoped panel. */
    private static enum Tag
    {
        MusicFamily,
        TextFamily,
        Quality,
        Lang,
        Interline,
        Barline,
        Beam;
    }

    //~ Instance fields ----------------------------------------------------------------------------

    /** The swing component of this entity. */
    private final JTabbedPane component = new JTabbedPane();

    /** The related book, if any. */
    private final Book book;

    /** The scoped panels. */
    private final Map<Object, ScopedPanel<Tag>> panels = new HashMap<>();

    /** The scoped panes for barline height on 1-line staves. */
    private final Map<Object, EnumPane> barlinePanes = new HashMap<>();

    /** The scoped panes for interline. */
    private final Map<Object, InterlinePane> interlinePanes = new HashMap<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a BookParameters object.
     *
     * @param stub the current sheet stub, or null
     */
    public BookParameters (SheetStub stub)
    {
        // Default panel
        final ScopedPanel<Tag> defaultPanel = new ScopedPanel<>(
                resources.getString("defaultTab.toolTipText"),
                buildDefaultTopics(),
                resources);
        component.addTab(
                resources.getString("defaultTab.text"),
                null,
                defaultPanel,
                defaultPanel.getName());
        panels.put(GLOBAL_SCOPE, defaultPanel);

        // Book panel?
        book = (stub != null) ? stub.getBook() : null;

        if (book != null) {
            final ScopedPanel<Tag> bookPanel = new ScopedPanel<>(
                    resources.getString("bookTab.toolTipText"),
                    buildBookTopics(defaultPanel),
                    resources);
            component.addTab(book.getRadix(), null, bookPanel, bookPanel.getName());
            panels.put(book, bookPanel);

            // Sheets panels?
            if (book.isMultiSheet()) {
                for (SheetStub s : book.getStubs()) {
                    final ScopedPanel<Tag> sheetPanel = new ScopedPanel<>(
                            MessageFormat.format(
                                    resources.getString("sheetTab.toolTipText"),
                                    s.getNum()),
                            buildSheetTopics(s, bookPanel),
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
        // NOTA: no name is set to component to avoid SAF to store/restore tab selection
        component.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);

        // Initially selected tab
        component.addChangeListener(this);
        component.setSelectedComponent(
                (panels.get(stub) != null) ? panels.get(stub)
                        : ((panels.get(book) != null) ? panels.get(book)
                                : panels.get(GLOBAL_SCOPE)));
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------------//
    // buildBookTopics //
    //-----------------//
    private List<XactTopic> buildBookTopics (ScopedPanel<Tag> parentPanel)
    {
        final List<XactTopic> topics = new ArrayList<>();

        { // General
            XactTopic topic = new XactTopic(Topic.General.name());
            topics.add(topic);

            topic.add(
                    new EnumPane<>(
                            Tag.MusicFamily,
                            MusicFamily.values(),
                            parentPanel,
                            book.getMusicFamilyParam(),
                            resources));

            topic.add(
                    new EnumPane<>(
                            Tag.TextFamily,
                            TextFamily.values(),
                            parentPanel,
                            book.getTextFamilyParam(),
                            resources));

            topic.add(
                    new EnumPane<>(
                            Tag.Quality,
                            InputQuality.values(),
                            parentPanel,
                            Profiles.defaultQualityParam,
                            resources));
        }

        { // Languages
            XactTopic topic = new XactTopic(Topic.Languages.name());
            topics.add(topic);

            final LangPane langPane = createLangPane(
                    (LangPane) parentPanel.getPane(Tag.Lang),
                    book.getOcrLanguagesParam());

            if (langPane != null) {
                topic.add(langPane);
            }
        }

        { //Scaling
            final XactTopic topic = new XactTopic(Topic.Scaling.name());
            topics.add(topic);

            final InterlinePane iPane = new InterlinePane(
                    null,
                    book.getInterlineSpecificationParam());
            iPane.setVisible(false);
            interlinePanes.put(book, iPane);
            topic.add(iPane);

            final EnumPane bPane = new EnumPane<>(
                    Tag.Barline,
                    BarlineHeight.values(),
                    parentPanel,
                    book.getBarlineHeightParam(),
                    resources);
            bPane.setVisible(false);
            barlinePanes.put(book, bPane);
            topic.add(bPane);

            topic.add(new BeamPane(null, book.getBeamSpecificationParam()));
        }

        final ProcessingSwitches switches = book.getProcessingSwitches();

        { // Staves
            final XactTopic topic = new XactTopic(Topic.Staves.name());
            topics.add(topic);

            for (ProcessingSwitch key : ProcessingSwitch.staffSwitches) {
                final Param<Boolean> bp = switches.getParam(key);
                topic.add(new SwitchPane(key, getPane(parentPanel, key), bp));
            }
        }

        { // Processing
            final XactTopic topic = new XactTopic(Topic.Processing.name());
            topics.add(topic);

            for (ProcessingSwitch key : ProcessingSwitch.standardSwitches) {
                final Param<Boolean> bp = switches.getParam(key);
                topic.add(new SwitchPane(key, getPane(parentPanel, key), bp));
            }
        }

        return topics;
    }

    //--------------------//
    // buildDefaultTopics //
    //--------------------//
    private List<XactTopic> buildDefaultTopics ()
    {
        final List<XactTopic> topics = new ArrayList<>();

        { // General
            final XactTopic topic = new XactTopic(Topic.General.name());
            topics.add(topic);

            topic.add(
                    new EnumPane<>(
                            Tag.MusicFamily,
                            MusicFamily.values(),
                            null,
                            MusicFont.defaultMusicParam,
                            resources));

            topic.add(
                    new EnumPane<>(
                            Tag.TextFamily,
                            TextFamily.values(),
                            null,
                            TextFont.defaultTextParam,
                            resources));

            topic.add(
                    new EnumPane<>(
                            Tag.Quality,
                            InputQuality.values(),
                            null,
                            Profiles.defaultQualityParam,
                            resources));
        }

        { // Languages
            XactTopic topic = new XactTopic(Topic.Languages.name());
            topics.add(topic);

            final LangPane langPane = createLangPane(null, Language.ocrDefaultLanguages);
            if (langPane != null) {
                topic.add(langPane);
            }
        }

        { // Scaling
            final XactTopic topic = new XactTopic(Topic.Scaling.name());
            topics.add(topic);

            // NOTA: No interline specification for global scope

            final EnumPane bPane = new EnumPane<>(
                    Tag.Barline,
                    BarlineHeight.values(),
                    null,
                    BarlineHeight.defaultParam,
                    resources);
            bPane.setVisible(false);
            barlinePanes.put(GLOBAL_SCOPE, bPane);
            topic.add(bPane);

            // NOTA: No beam specification for global scope
        }

        final ProcessingSwitches switches = ProcessingSwitches.getDefaultSwitches();

        { // Staves
            final XactTopic topic = new XactTopic(Topic.Staves.name());
            topics.add(topic);

            for (ProcessingSwitch key : ProcessingSwitch.staffSwitches) {
                topic.add(new SwitchPane(key, null, switches.getParam(key)));
            }
        }

        { // Processing
            final XactTopic topic = new XactTopic(Topic.Processing.name());
            topics.add(topic);

            for (ProcessingSwitch key : ProcessingSwitch.standardSwitches) {
                topic.add(new SwitchPane(key, null, switches.getParam(key)));
            }
        }

        return topics;
    }

    //------------------//
    // buildSheetTopics //
    //------------------//
    private List<XactTopic> buildSheetTopics (SheetStub s,
                                              ScopedPanel<Tag> parentPanel)
    {
        final List<XactTopic> topics = new ArrayList<>();

        { // General
            final XactTopic topic = new XactTopic(Topic.General.name());
            topics.add(topic);

            topic.add(
                    new EnumPane<>(
                            Tag.MusicFamily,
                            MusicFamily.values(),
                            parentPanel,
                            s.getMusicFamilyParam(),
                            resources));

            topic.add(
                    new EnumPane<>(
                            Tag.TextFamily,
                            TextFamily.values(),
                            parentPanel,
                            s.getTextFamilyParam(),
                            resources));

            topic.add(
                    new EnumPane<>(
                            Tag.Quality,
                            InputQuality.values(),
                            parentPanel,
                            Profiles.defaultQualityParam,
                            resources));
        }

        { // Languages
            XactTopic topic = new XactTopic(Topic.Languages.name());
            topics.add(topic);

            final LangPane langPane = createLangPane(
                    (LangPane) parentPanel.getPane(Tag.Lang),
                    s.getOcrLanguagesParam());

            if (langPane != null) {
                topic.add(langPane);
            }
        }

        { // Scaling
            final XactTopic topic = new XactTopic(Topic.Scaling.name());
            topics.add(topic);

            final InterlinePane iPane = new InterlinePane(
                    (InterlinePane) parentPanel.getPane(Tag.Interline),
                    s.getInterlineSpecificationParam());
            iPane.setVisible(false);
            interlinePanes.put(book, iPane);
            topic.add(iPane);

            final EnumPane bPane = new EnumPane<>(
                    Tag.Barline,
                    BarlineHeight.values(),
                    parentPanel,
                    s.getBarlineHeightParam(),
                    resources);
            bPane.setVisible(false);
            barlinePanes.put(s, bPane);
            topic.add(bPane);

            topic.add(
                    new BeamPane(
                            (BeamPane) parentPanel.getPane(Tag.Beam),
                            s.getBeamSpecificationParam()));
        }

        final ProcessingSwitches switches = s.getProcessingSwitches();

        { // Staves
            final XactTopic topic = new XactTopic(Topic.Staves.name());
            topics.add(topic);

            for (ProcessingSwitch key : ProcessingSwitch.staffSwitches) {
                final Param<Boolean> bp = switches.getParam(key);
                topic.add(new SwitchPane(key, getPane(parentPanel, key), bp));
            }
        }

        { // Processing
            final XactTopic topic = new XactTopic(Topic.Processing.name());
            topics.add(topic);

            for (ProcessingSwitch key : ProcessingSwitch.standardSwitches) {
                final Param<Boolean> bp = switches.getParam(key);
                topic.add(new SwitchPane(key, getPane(parentPanel, key), bp));
            }
        }

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
                @SuppressWarnings("unchecked")
                final ScopedPanel<Tag> panel = (ScopedPanel) component.getComponentAt(tab);
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

    //----------------//
    // createLangPane //
    //----------------//
    /**
     * Factory method to get a LangPane, while handling exception when no OCR is available.
     *
     * @param parent parent pane, if any
     * @param model  underlying model data
     * @return A usable LangPane instance, or null otherwise
     */
    private LangPane createLangPane (LangPane parent,
                                     Param<String> model)
    {
        // The language pane needs Tesseract up & running
        if (OcrUtil.getOcr().isAvailable()) {
            try {
                return new LangPane(parent, model);
            } catch (Throwable ex) {
                logger.warn("Error creating language pane", ex);
            }
        } else {
            logger.info("No language pane for lack of OCR.");
        }

        return null;
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component.
     *
     * @return the concrete component
     */
    public JTabbedPane getComponent ()
    {
        return component;
    }

    //----------//
    // getTitle //
    //----------//
    public String getTitle ()
    {
        if (book != null) {
            return MessageFormat.format(resources.getString("bookTitlePattern"), book.getRadix());
        } else {
            return resources.getString("globalTitle");
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
        ScopedPanel<Tag> panel = (ScopedPanel) component.getSelectedComponent();

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

    //---------//
    // getPane //
    //---------//
    /**
     * Retrieve a SwitchPane knowing its key.
     *
     * @param panel containing panel
     * @param key   desired key
     * @return corresponding pane
     */
    private static SwitchPane getPane (ScopedPanel<Tag> panel,
                                       ProcessingSwitch key)
    {
        for (XactPane pane : panel.getPanes()) {
            if (pane instanceof SwitchPane switchPane) {
                if (switchPane.getKey() == key) {
                    return switchPane;
                }
            }
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // BeamPane //
    //----------//
    /**
     * Pane to define beam thickness.
     */
    private static class BeamPane
            extends IntegerSpinPane<Tag>
    {
        BeamPane (BeamPane parent,
                  Param<Integer> model)
        {
            super(
                    Tag.Beam,
                    new SpinData(new SpinnerNumberModel(10, 0, ScaleBuilder.getMaxInterline(), 1)),
                    parent,
                    model,
                    resources);
        }
    }

    //---------------//
    // InterlinePane //
    //---------------//
    /**
     * Pane to define interline value.
     */
    private static class InterlinePane
            extends IntegerSpinPane<Tag>
    {
        InterlinePane (InterlinePane parent,
                       Param<Integer> model)
        {
            super(
                    Tag.Interline,
                    new SpinData(new SpinnerNumberModel(10, 0, ScaleBuilder.getMaxInterline(), 1)),
                    parent,
                    model,
                    resources);
        }
    }

    //----------//
    // LangPane //
    //----------//
    /**
     * Pane to set the dominant text language specification.
     */
    private static class LangPane
            extends XactPane<Tag, String>
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

        LangPane (LangPane parent,
                  Param<String> model)
        {
            super(Tag.Lang, resources.getString("LangPane.title"), parent, model);

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
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int titleWidth,
                                 int r)
        {
            r = super.defineLayout(builder, cst, titleWidth, r); // sel + title, DO advance

            builder.add(langSpec, cst.xyw(3, r, 3));
            builder.add(langScroll, cst.xyw(7, r, 3));

            return r + 2;
        }

        @Override
        protected void display (String spec)
        {
            int[] indices = listModel.indicesOf(spec);

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
            return listModel.specOf(langList.getSelectedValuesList());
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
            extends BooleanPane<Tag>
    {
        final ProcessingSwitch key; // The related switch

        SwitchPane (ProcessingSwitch key,
                    BooleanPane<Tag> parent,
                    Param<Boolean> model)
        {
            super(null, textOf(key), parent, tipOf(key), model);
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
        protected void display (Boolean content)
        {
            super.display(content);

            switch (key) {
            case oneLineStaves ->
            {
                // We display the barline height specification if and only if
                // the switch for 1-line staves is ON
                final Object scope = model.getScope();
                final EnumPane ep = barlinePanes.get(scope);
                ep.setVisible((content != null) && content);
            }
            case fiveLineStaves, drumNotation ->
            {
                // We display the interline pane if and only if
                // both switches for 5-line standard staves and for 5-line percussion staves are OFF
                // Otherwise, it is safer to set the interline spec to zero (i.e. disabled)
                final Object scope = model.getScope();
                final InterlinePane pane = interlinePanes.get(scope);

                if (pane != null) {
                    final ProcessingSwitch other = (key == ProcessingSwitch.fiveLineStaves)
                            ? ProcessingSwitch.drumNotation
                            : ProcessingSwitch.fiveLineStaves;
                    boolean bothOff = (content == null) || !content;
                    bothOff &= !isSet(scope, other);
                    pane.setVisible(bothOff);

                    if (!bothOff) {
                        pane.display(0);
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
            final ScopedPanel<Tag> panel = panels.get(scope);

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
}
