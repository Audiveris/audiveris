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

import org.audiveris.omr.image.AdaptiveDescriptor;
import org.audiveris.omr.image.FilterDescriptor;
import org.audiveris.omr.image.FilterKind;
import org.audiveris.omr.image.GlobalDescriptor;
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
import org.audiveris.omr.ui.action.AdvancedTopics;
import org.audiveris.omr.ui.field.SpinnerUtil;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.util.param.Param;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Class <code>BookParameters</code> is a dialog that allows the user to easily manage the
 * most frequent parameters.
 * <p>
 * It addresses:
 * <ul>
 * <li>Binarization parameters</li>
 * <li>Music font specification</li>
 * <li>Text font specification</li>
 * <li>Input quality specification</li>
 * <li>Beam thickness</li>
 * <li>Language specification</li>
 * <li>Support for specific features</li>
 * </ul>
 * <div style="float: right;">
 * <img src="doc-files/ScoreParameters-img.png" alt="Score parameters dialog">
 * </div>
 * <p>
 * The dialog is organized as a scope-based tabbed pane with:
 * <ul>
 * <li>A panel for the <b>default</b> scope,</li>
 * <li>A panel for current <b>book</b> scope (provided that there is a selected book),</li>
 * <li>And one panel for every <b>sheet</b> scope (provided that the current book contains more than
 * a single sheet).</li>
 * </ul>
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

    /** The swing component of this panel. */
    private final JTabbedPane component = new JTabbedPane();

    /** The related book, if any. */
    private final Book book;

    /** The panel dedicated to setting of defaults. */
    private final TaggedScopedPanel defaultPanel;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a BookParameters object.
     *
     * @param stub the current sheet stub, or null
     */
    public BookParameters (SheetStub stub)
    {
        book = (stub != null) ? stub.getBook() : null;

        // component.setName("BookParametersPane"); <== NO!
        // NOTA: no name is set to component to avoid SAF to store/restore tab selection
        component.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);

        // Allocate all required panels (default / book? / sheets??)
        final TaggedScopedPanel bookPanel;
        TaggedScopedPanel sheetPanel = null; // Only for multi-sheet book

        // Default panel
        //--------------

        final List<XactDataPane> defaultPanes = new ArrayList<>();

        if (AdvancedTopics.Topic.SPECIFIC_ITEMS.isSet()) {
            defaultPanes.add(new FilterPane(null, FilterDescriptor.defaultFilter));
        }

        defaultPanes.add(
                new EnumPane<>(
                        Tag.MusicFamily,
                        MusicFamily.values(),
                        null,
                        MusicFont.defaultMusicParam));

        defaultPanes.add(
                new EnumPane<>(
                        Tag.TextFamily,
                        TextFamily.values(),
                        null,
                        TextFont.defaultTextParam));

        defaultPanes.add(
                new EnumPane<>(
                        Tag.Quality,
                        InputQuality.values(),
                        null,
                        Profiles.defaultQualityParam));

        final LangPane defaultLangPane = createLangPane(null, Language.ocrDefaultLanguages);

        if (defaultLangPane != null) {
            defaultPanes.add(defaultLangPane);
        }

        final ProcessingSwitches defaultSwitches = ProcessingSwitches.getDefaultSwitches();

        for (ProcessingSwitch key : ProcessingSwitch.supportedSwitches) {
            SwitchPane switchPane = new SwitchPane(key, null, defaultSwitches.getParam(key));
            defaultPanes.add(switchPane);
        }

        defaultPanel = new TaggedScopedPanel(
                resources.getString("defaultTab.toolTipText"),
                defaultPanes);
        component.addTab(
                resources.getString("defaultTab.text"),
                null,
                defaultPanel,
                defaultPanel.getName());

        // Book panel?
        //------------

        if (book != null) {
            final List<XactDataPane> bookPanes = new ArrayList<>();
            if (AdvancedTopics.Topic.SPECIFIC_ITEMS.isSet()) {
                bookPanes.add(
                        new FilterPane(
                                (FilterPane) defaultPanel.getPane(Tag.Filter),
                                book.getBinarizationFilterParam()));
            }

            bookPanes.add(
                    new EnumPane<>(
                            Tag.MusicFamily,
                            MusicFamily.values(),
                            defaultPanel,
                            book.getMusicFamilyParam()));

            bookPanes.add(
                    new EnumPane<>(
                            Tag.TextFamily,
                            TextFamily.values(),
                            defaultPanel,
                            book.getTextFamilyParam()));

            bookPanes.add(
                    new EnumPane<>(
                            Tag.Quality,
                            InputQuality.values(),
                            defaultPanel,
                            Profiles.defaultQualityParam));

            bookPanes.add(new BeamPane(null, book.getBeamSpecificationParam()));

            final LangPane bookLangPane = createLangPane(
                    (LangPane) defaultPanel.getPane(Tag.Lang),
                    book.getOcrLanguages());
            if (bookLangPane != null) {
                bookPanes.add(bookLangPane);
            }

            for (ProcessingSwitch key : ProcessingSwitch.supportedSwitches) {
                final Param<Boolean> bp = book.getProcessingSwitches().getParam(key);
                bookPanes.add(new SwitchPane(key, getPane(defaultPanel, key), bp));
            }

            bookPanel = new TaggedScopedPanel(
                    resources.getString("bookTab.toolTipText"),
                    bookPanes);
            component.addTab(book.getRadix(), null, bookPanel, bookPanel.getName());

            // Sheets panels?
            //---------------

            if (book.isMultiSheet()) {
                for (SheetStub s : book.getStubs()) {
                    final List<XactDataPane> sheetPanes = new ArrayList<>();
                    if (AdvancedTopics.Topic.SPECIFIC_ITEMS.isSet()) {
                        sheetPanes.add(
                                new FilterPane(
                                        (FilterPane) bookPanel.getPane(Tag.Filter),
                                        s.getBinarizationFilterParam()));
                    }

                    sheetPanes.add(
                            new EnumPane<>(
                                    Tag.MusicFamily,
                                    MusicFamily.values(),
                                    bookPanel,
                                    s.getMusicFamilyParam()));

                    sheetPanes.add(
                            new EnumPane<>(
                                    Tag.TextFamily,
                                    TextFamily.values(),
                                    bookPanel,
                                    s.getTextFamilyParam()));

                    sheetPanes.add(
                            new EnumPane<>(
                                    Tag.Quality,
                                    InputQuality.values(),
                                    bookPanel,
                                    Profiles.defaultQualityParam));

                    sheetPanes.add(
                            new BeamPane(
                                    (BeamPane) bookPanel.getPane(Tag.Beam),
                                    s.getBeamSpecificationParam()));

                    final LangPane sheetLangPane = createLangPane(
                            (LangPane) bookPanel.getPane(Tag.Lang),
                            s.getOcrLanguagesParam());

                    if (sheetLangPane != null) {
                        sheetPanes.add(sheetLangPane);
                    }

                    for (ProcessingSwitch key : ProcessingSwitch.supportedSwitches) {
                        Param<Boolean> bp = s.getProcessingSwitches().getParam(key);
                        sheetPanes.add(new SwitchPane(key, getPane(bookPanel, key), bp));
                    }

                    final TaggedScopedPanel sPanel = new TaggedScopedPanel(
                            MessageFormat.format(
                                    resources.getString("sheetTab.toolTipText"),
                                    s.getNum()),
                            sheetPanes);
                    final String initial = resources.getString("sheetInitialChar");
                    String label = initial + "#" + s.getNumber();

                    if (s == stub) {
                        sheetPanel = sPanel;
                        label = "*" + label + "*";
                    }

                    component.addTab(label, null, sPanel, sPanel.getName());
                }
            }
        } else {
            bookPanel = null;
        }

        // Initially selected tab
        component.addChangeListener(this);
        component.setSelectedComponent(
                (sheetPanel != null) ? sheetPanel
                        : ((bookPanel != null) ? bookPanel : defaultPanel));
    }

    //~ Methods ------------------------------------------------------------------------------------

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
                final TaggedScopedPanel panel = (TaggedScopedPanel) component.getComponentAt(tab);
                boolean modified = false;

                for (XactDataPane pane : panel.getPanes()) {
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
        TaggedScopedPanel panel = (TaggedScopedPanel) component.getSelectedComponent();

        PaneLoop:
        for (XactDataPane pane : panel.getPanes()) {
            if (!pane.isSelected()) {
                // Use the first parent with any specific value
                XactDataPane highestPane = pane;
                XactDataPane p = pane.parent;

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
    private static SwitchPane getPane (TaggedScopedPanel panel,
                                       ProcessingSwitch key)
    {
        for (XactDataPane pane : panel.getPanes()) {
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
            extends TaggedXactDataPane<Integer>
    {
        private final SpinData thickness = new SpinData(
                new SpinnerNumberModel(10, 0, ScaleBuilder.getMaxInterline(), 1));

        BeamPane (BeamPane parent,
                  Param<Integer> model)
        {
            super(Tag.Beam, resources.getString("BeamPane.title"), parent, model);
            title.setToolTipText(resources.getString("BeamPane.toolTipText"));
        }

        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int titleWidth,
                                 int r)
        {
            super.defineLayout(builder, cst, 1, r); // sel + title, no advance
            thickness.defineLayout(builder, cst, r);

            return r + 2;
        }

        @Override
        protected void display (Integer content)
        {
            if (content != null) {
                thickness.spinner.setValue(content);
            } else {
                thickness.spinner.setValue(0);
            }
        }

        @Override
        protected void setEnabled (boolean bool)
        {
            thickness.setEnabled(bool);
        }

        @Override
        protected Integer read ()
        {
            try {
                thickness.spinner.commitEdit();
            } catch (ParseException ignored) {
            }

            return (int) thickness.spinner.getValue();
        }
    }

    //----------//
    // EnumPane //
    //----------//
    /**
     * Pane to select an enum.
     */
    private static class EnumPane<E extends Enum>
            extends TaggedXactDataPane<E>
    {

        /** ComboBox for enum. */
        private final JComboBox<E> enumCombo;

        @SuppressWarnings("unchecked")
        public EnumPane (Tag tag,
                         E[] values,
                         TaggedScopedPanel parentPanel,
                         Param<E> model)
        {
            super(
                    tag,
                    resources.getString(tag + "Pane.title"),
                    (parentPanel != null) ? parentPanel.getPane(tag) : null,
                    model);

            title.setToolTipText(resources.getString(tag + "Pane.toolTipText"));

            enumCombo = new JComboBox<>(values);
            enumCombo.setToolTipText(resources.getString(tag + "Pane.combo.toolTipText"));
            enumCombo.addActionListener(this);
        }

        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int titleWidth,
                                 int r)
        {
            super.defineLayout(builder, cst, 1, r); // sel + title, no advance

            builder.add(enumCombo, cst.xyw(5, r, 3));

            return r + 2;
        }

        @Override
        protected void display (E family)
        {
            enumCombo.setSelectedItem(family);
        }

        @Override
        protected E read ()
        {
            return enumCombo.getItemAt(enumCombo.getSelectedIndex());
        }

        @Override
        protected void setEnabled (boolean bool)
        {
            enumCombo.setEnabled(bool);
        }
    }

    //------------//
    // FilterPane //
    //------------//
    /**
     * Pane to define the pixel binarization parameters.
     */
    private static class FilterPane
            extends TaggedXactDataPane<FilterDescriptor>
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
                new SpinnerNumberModel(0, 0, 255, 1));

        // Data for local
        private final SpinData localDataMean = new SpinData(
                resources.getString("FilterPane.localDataMean.text"),
                resources.getString("FilterPane.localDataMean.toolTipText"),
                new SpinnerNumberModel(0.5, 0.5, 1.5, 0.1));

        private final SpinData localDataDev = new SpinData(
                resources.getString("FilterPane.localDataDev.text"),
                resources.getString("FilterPane.localDataDev.toolTipText"),
                new SpinnerNumberModel(0.2, 0.2, 1.5, 0.1));

        FilterPane (FilterPane parent,
                    Param<FilterDescriptor> model)
        {
            super(Tag.Filter, resources.getString("FilterPane.title"), parent, model);

            // ComboBox for filter kind
            kindCombo.setToolTipText(resources.getString("FilterPane.kindCombo.toolTipText"));
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
            case GLOBAL ->
            {
                localDataMean.setVisible(false);
                localDataDev.setVisible(false);
                globalData.setVisible(true);
            }

            case ADAPTIVE ->
            {
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

                case ADAPTIVE ->
                {
                    localDataMean.spinner.commitEdit();
                    localDataDev.spinner.commitEdit();
                }
                }
            } catch (ParseException ignored) {
            }
        }

        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int titleWidth,
                                 int r)
        {
            super.defineLayout(builder, cst, 1, r); // sel + title, no advance

            builder.add(kindLabel, cst.xyw(3, r, 1));
            builder.add(kindCombo, cst.xyw(5, r, 3));
            r += 2;

            // Layout global and local data as mutual overlays
            globalData.defineLayout(builder, cst, r);
            r = localDataMean.defineLayout(builder, cst, r);
            r = localDataDev.defineLayout(builder, cst, r);

            return r;
        }

        @Override
        protected void display (FilterDescriptor desc)
        {
            FilterKind kind = desc.getKind();
            kindCombo.setSelectedItem(kind);

            switch (kind) {
            case GLOBAL ->
            {
                GlobalDescriptor globalDesc = (GlobalDescriptor) desc;
                globalData.spinner.setValue(globalDesc.threshold);
            }

            case ADAPTIVE ->
            {
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
        protected void setEnabled (boolean bool)
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
            extends TaggedXactDataPane<String>
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

            builder.add(langSpec, cst.xyw(1, r, 3));
            builder.add(langScroll, cst.xyw(5, r, 3));

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
        protected void setEnabled (boolean bool)
        {
            langList.setEnabled(bool);
            langSpec.setEnabled(bool);
        }

        @Override
        public void valueChanged (ListSelectionEvent e)
        {
            langSpec.setText(read());
        }
    }

    //----------//
    // SpinData //
    //----------//
    /**
     * A line with a labeled spinner.
     */
    private static class SpinData
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

        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            builder.add(label, cst.xyw(3, r, 1));
            builder.add(spinner, cst.xyw(5, r, 3));

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

    //------------//
    // SwitchPane //
    //------------//
    /**
     * A pane for one boolean switch.
     */
    private static class SwitchPane
            extends BooleanPane
    {

        final ProcessingSwitch key;

        SwitchPane (ProcessingSwitch key,
                    XactDataPane<Boolean> parent,
                    Param<Boolean> model)
        {
            super(textOf(key), parent, tipOf(key), model);
            this.key = key;
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            if ((e != null) && (e.getSource() == bbox)) {
                display(read());
            } else {
                super.actionPerformed(e);
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

        private static String textOf (ProcessingSwitch key)
        {
            // Priority given to text in resources file if any
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
    private static enum Tag
    {
        Filter,
        MusicFamily,
        TextFamily,
        Quality,
        Lang,
        Beam;
    }

    //-------------------//
    // TaggedScopedPanel //
    //-------------------//
    private static class TaggedScopedPanel
            extends ScopedPanel
    {
        public TaggedScopedPanel (String name,
                                  List<XactDataPane> panes)
        {
            super(name, panes);
        }

        /**
         * Report the contained pane of proper tag.
         *
         * @param tag desired tag
         * @return the pane found or null
         */
        public TaggedXactDataPane getPane (Tag tag)
        {
            for (XactDataPane pane : panes) {
                if (pane instanceof TaggedXactDataPane tagged) {
                    if (tagged.tag == tag)
                        return tagged;
                }
            }

            return null;
        }
    }

    //--------------------//
    // TaggedXactDataPane //
    //--------------------//
    private static abstract class TaggedXactDataPane<E>
            extends XactDataPane<E>
    {
        public final Tag tag;

        public TaggedXactDataPane (Tag tag,
                                   String title,
                                   XactDataPane<E> parent,
                                   Param<E> model)
        {
            super(title, parent, model);
            this.tag = tag;
        }
    }
}
