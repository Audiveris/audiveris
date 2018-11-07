//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S c o r e P a r a m e t e r s                                  //
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

import org.audiveris.omr.image.AdaptiveDescriptor;
import org.audiveris.omr.image.FilterDescriptor;
import org.audiveris.omr.image.FilterKind;
import org.audiveris.omr.image.GlobalDescriptor;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.ProcessingSwitches;
import org.audiveris.omr.sheet.ProcessingSwitches.Switch;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.text.Language;
import org.audiveris.omr.text.OcrUtil;
import org.audiveris.omr.ui.field.SpinnerUtil;
import org.audiveris.omr.util.param.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

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
 * Class {@code ScoreParameters} is a dialog that allows the user to easily manage the
 * most frequent parameters.
 * <div style="float: right;">
 * <img src="doc-files/ScoreParameters-img.png" alt="Score parameters dialog">
 * </div>
 * <p>
 * <p>
 * It addresses:
 * <ul>
 * <li>Binarization parameters</li>
 * <li>Text language specification</li>
 * <li>Support for specific features</li>
 * <!-- TODO <li>Name and instrument related to each score part</li> -->
 * </ul>
 * <p>
 * <p>
 * The dialog is organized as a scope-based tabbed pane with:
 * <ul>
 * <li>a panel for the <b>default</b> scope,</li>
 * <li>a panel for current <b>book</b> scope (provided that there is a selected book),</li>
 * <li>and one panel for every <b>sheet</b> scope (provided that the book contains more than a
 * single sheet).</li>
 * </ul>
 * <p>
 * <p>
 * A panel is a vertical collection of panes, each pane being introduced by a check box and a label.
 * With no specific information, the box is unchecked, the pane content is disabled.
 * With specific information, the box is checked and the pane content is enabled.
 * <br>Manually checking the box represents a selection and indicates the intention to modify the
 * pane content (and thus enables the pane fields).
 * <br>Un-checking the box reverts the content to the value it had prior to the selection.
 * <p>
 * <p>
 * The selected modifications are performed only when the user presses the OK button.
 * <p>
 * <p>
 * <img src="doc-files/ScoreParameters.png" alt="Score parameters dialog">
 *
 * @author Hervé Bitteur
 */
public class ScoreParameters
        implements ChangeListener
{

    private static final Logger logger = LoggerFactory.getLogger(ScoreParameters.class);

    /** The swing component of this panel. */
    private final JTabbedPane component = new JTabbedPane();

    /** The related book, if any. */
    private final Book book;

    /** The panel dedicated to setting of defaults. */
    private final ScopedPanel defaultPanel;

    /**
     * Create a ScoreParameters object.
     *
     * @param stub the current sheet stub, or null
     */
    public ScoreParameters (SheetStub stub)
    {
        if (stub != null) {
            this.book = stub.getBook();
        } else {
            book = null;
        }

        component.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);

        // Allocate all required panels (default / book? / sheets??)
        final ScopedPanel bookPanel;
        ScopedPanel sheetPanel = null; // Only for multi-sheet book

        // Default panel
        List<XactDataPane> defaultPanes = new ArrayList<XactDataPane>();
        defaultPanes.add(new FilterPane(null, FilterDescriptor.defaultFilter));

        TextPane defaultTextPane = createTextPane(null, Language.ocrDefaultLanguages);

        if (defaultTextPane != null) {
            defaultPanes.add(defaultTextPane);
        }

        ProcessingSwitches defaultSwitches = ProcessingSwitches.getDefaultSwitches();

        for (Switch key : Switch.values()) {
            SwitchPane switchPane = new SwitchPane(key, null, defaultSwitches.getParam(key));
            defaultPanes.add(switchPane);
        }

        defaultPanel = new ScopedPanel("Default settings", defaultPanes);
        component.addTab("Default", null, defaultPanel, defaultPanel.getName());

        // Book panel?
        if (book != null) {
            List<XactDataPane> bookPanes = new ArrayList<XactDataPane>();
            bookPanes.add(new FilterPane((FilterPane) defaultPanel.getPane(FilterPane.class), book
                                         .getBinarizationFilter()));

            TextPane bookTextPane = createTextPane((TextPane) defaultPanel.getPane(TextPane.class),
                                                   book.getOcrLanguages());

            if (bookTextPane != null) {
                bookPanes.add(bookTextPane);
            }

            for (Switch key : Switch.values()) {
                Param<Boolean> bp = book.getProcessingSwitches().getParam(key);
                bookPanes.add(new SwitchPane(key, getPane(defaultPanel, key), bp));
            }

            bookPanel = new ScopedPanel("Book settings", bookPanes);
            component.addTab(book.getRadix(), null, bookPanel, bookPanel.getName());

            // Sheets panels?
            if (book.isMultiSheet()) {
                for (SheetStub s : book.getStubs()) {
                    List<XactDataPane> sheetPanes = new ArrayList<XactDataPane>();
                    sheetPanes.add(new FilterPane((FilterPane) bookPanel.getPane(FilterPane.class),
                                                  s.getBinarizationFilter()));

                    TextPane sheetTextPane = createTextPane((TextPane) bookPanel.getPane(
                            TextPane.class), s.getOcrLanguages());

                    if (sheetTextPane != null) {
                        sheetPanes.add(sheetTextPane);
                    }

                    for (Switch key : Switch.values()) {
                        Param<Boolean> bp = s.getProcessingSwitches().getParam(key);
                        sheetPanes.add(new SwitchPane(key, getPane(bookPanel, key), bp));
                    }

                    ScopedPanel panel = new ScopedPanel("Sheet settings", sheetPanes);
                    String label = "S#" + s.getNumber();

                    if (s == stub) {
                        sheetPanel = panel;
                        label = "*" + label + "*";
                    }

                    component.addTab(label, null, panel, panel.getName());
                }
            }
        } else {
            bookPanel = null;
        }

        // Initially selected tab
        component.addChangeListener(this);
        component.setSelectedComponent((sheetPanel != null) ? sheetPanel : ((bookPanel != null)
                ? bookPanel : defaultPanel));
    }

    //--------//
    // commit //
    //--------//
    /**
     * Commit the user actions.
     *
     * @param stub the related sheet
     * @return true if committed, false otherwise
     */
    public boolean commit (SheetStub stub)
    {
        try {
            // Commit all specific values, if any, to their model object
            for (int t = 0, tBreak = component.getTabCount(); t < tBreak; t++) {
                final ScopedPanel panel = (ScopedPanel) component.getComponentAt(t);
                boolean modified = false;

                for (XactDataPane pane : panel.getPanes()) {
                    modified |= pane.commit();
                }

                // Book/Sheet modifications
                if ((t > 0) && modified) {
                    stub.setModified(true);
                }
            }
        } catch (Exception ex) {
            logger.warn("Could not commit score parameters", ex);

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
    public JTabbedPane getComponent ()
    {
        return component;
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
    public void stateChanged (ChangeEvent e)
    {
        // Refresh the new current panel
        ScopedPanel panel = (ScopedPanel) component.getSelectedComponent();

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
                pane.display(highestPane.model.getSourceValue());
            }
        }
    }

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
    private static SwitchPane getPane (ScopedPanel panel,
                                       Switch key)
    {
        for (XactDataPane pane : panel.getPanes()) {
            if (pane instanceof SwitchPane) {
                SwitchPane switchPane = (SwitchPane) pane;

                if (switchPane.getKey() == key) {
                    return switchPane;
                }
            }
        }

        return null;
    }

    //----------------//
    // createTextPane //
    //----------------//
    /**
     * Factory method to get a TextPane, while handling exception when
     * no OCR is available.
     *
     * @param parent parent pane, if any
     * @param model  underlying model data
     * @return A usable TextPane instance, or null otherwise
     */
    private TextPane createTextPane (TextPane parent,
                                     Param<String> model)
    {
        // The language pane needs Tesseract up & running
        if (OcrUtil.getOcr().isAvailable()) {
            try {
                return new TextPane(parent, model);
            } catch (Throwable ex) {
                logger.warn("Error creating language pane", ex);
            }
        } else {
            logger.info("No language pane for lack of OCR.");
        }

        return null;
    }

    //------------//
    // FilterPane //
    //------------//
    /**
     * Pane to define the pixel binarization parameters.
     * Scope can be: default, book, sheet.
     */
    private static class FilterPane
            extends XactDataPane<FilterDescriptor>
    {

        /** ComboBox for filter kind */
        private final JComboBox<FilterKind> kindCombo = new JComboBox<FilterKind>(FilterKind
                .values());

        private final JLabel kindLabel = new JLabel("Filter", SwingConstants.RIGHT);

        // Data for global
        private final SpinData globalData = new SpinData("Threshold",
                                                         "Global threshold for foreground pixels",
                                                         new SpinnerNumberModel(0, 0, 255, 1));

        // Data for local
        private final SpinData localDataMean = new SpinData("Coeff for Mean",
                                                            "Coefficient for mean pixel value",
                                                            new SpinnerNumberModel(0.5, 0.5, 1.5,
                                                                                   0.1));

        private final SpinData localDataDev = new SpinData("Coeff for StdDev",
                                                           "Coefficient for standard deviation value",
                                                           new SpinnerNumberModel(0.2, 0.2, 1.5, 0.1));

        public FilterPane (FilterPane parent,
                           Param<FilterDescriptor> model)
        {
            super("Binarization", parent, model);

            // ComboBox for filter kind
            kindCombo.setToolTipText("Specific filter on image pixels");
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
            case GLOBAL:
                localDataMean.setVisible(false);
                localDataDev.setVisible(false);
                globalData.setVisible(true);

                break;

            case ADAPTIVE:
                globalData.setVisible(false);
                localDataMean.setVisible(true);
                localDataDev.setVisible(true);

                break;
            }
        }

        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            r = super.defineLayout(builder, cst, r);

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
        public int getLogicalRowCount ()
        {
            return 4;
        }

        @Override
        protected void display (FilterDescriptor desc)
        {
            FilterKind kind = desc.getKind();
            kindCombo.setSelectedItem(kind);

            switch (kind) {
            case GLOBAL:

                GlobalDescriptor globalDesc = (GlobalDescriptor) desc;
                globalData.spinner.setValue(globalDesc.threshold);

                break;

            case ADAPTIVE:

                AdaptiveDescriptor localDesc = (AdaptiveDescriptor) desc;
                localDataMean.spinner.setValue(localDesc.meanCoeff);
                localDataDev.spinner.setValue(localDesc.stdDevCoeff);

                break;

            default:
            }
        }

        @Override
        protected FilterDescriptor read ()
        {
            commitSpinners();

            return (readKind() == FilterKind.GLOBAL) ? new GlobalDescriptor((int) globalData.spinner
                    .getValue())
                    : new AdaptiveDescriptor((double) localDataMean.spinner.getValue(),
                                             (double) localDataDev.spinner.getValue());
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

        /** This is needed to read data manually typed in spinners fields. */
        private void commitSpinners ()
        {
            try {
                switch (readKind()) {
                case GLOBAL:
                    globalData.spinner.commitEdit();

                    break;

                case ADAPTIVE:
                    localDataMean.spinner.commitEdit();
                    localDataDev.spinner.commitEdit();

                    break;

                default:
                }
            } catch (ParseException ignored) {
            }
        }

        private FilterKind readKind ()
        {
            return kindCombo.getItemAt(kindCombo.getSelectedIndex());
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

        public SpinData (String label,
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
     * Scope can be: default, book, sheet.
     */
    private static class SwitchPane
            extends BooleanPane
    {

        final Switch key;

        public SwitchPane (Switch key,
                           XactDataPane parent,
                           Param<Boolean> model)
        {
            super(key.getConstant().getDescription(), parent, "", null, model);
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

        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            // Draw the specific/inherit box
            builder.add(selBox, cst.xyw(1, r, 1));
            builder.add(separator, cst.xyw(3, r, 3));
            builder.add(bbox, cst.xyw(7, r, 1));

            return r + 2;
        }

        public Switch getKey ()
        {
            return key;
        }

        @Override
        public int getLogicalRowCount ()
        {
            return 1;
        }
    }

    //----------//
    // TextPane //
    //----------//
    /**
     * Pane to set the dominant text language specification.
     * Scope can be: default, book, sheet.
     */
    private static class TextPane
            extends XactDataPane<String>
            implements ListSelectionListener
    {

        /** Underlying language list model. */
        final Language.ListModel listModel;

        /** List for choosing elements of language specification. */
        private final JList<String> langList;

        /** Put the list into a scroll pane. */
        private final JScrollPane langScroll;

        /** Resulting visible specification. */
        private final JLabel langSpec = new JLabel("", SwingConstants.RIGHT);

        public TextPane (TextPane parent,
                         Param<String> model)
        {
            super("OCR language(s)", parent, model);

            listModel = new Language.ListModel();

            langList = new JList<String>(listModel);
            langList.setLayoutOrientation(JList.VERTICAL);
            langList.setToolTipText("Dominant languages for textual items");
            langList.setVisibleRowCount(5);
            langList.addListSelectionListener(this);

            langScroll = new JScrollPane(langList);

            langSpec.setToolTipText("Resulting specification");
        }

        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            r = super.defineLayout(builder, cst, r);

            builder.add(langSpec, cst.xyw(1, r, 4));
            builder.add(langScroll, cst.xyw(5, r, 3));

            return r + 2;
        }

        @Override
        public void valueChanged (ListSelectionEvent e)
        {
            langSpec.setText(read());
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
    }
}
//
//    //-----------//
//    // Tempopane //
//    //-----------//
//    /**
//     * Pane to set the dominant tempo value.
//     * Scope can be: default, score.
//     */
//    private class TempoPane
//            extends Pane<Integer>
//    {
//        //~ Instance fields ------------------------------------------------------------------------
//
//        // Tempo value
//        private final SpinData tempo = new SpinData(
//                "Quarters/Min",
//                "Tempo in quarters per minute",
//                new SpinnerNumberModel(20, 20, 400, 1));
//
//        //~ Constructors ---------------------------------------------------------------------------
//        public TempoPane (Score score,
//                          Pane parent,
//                          Param<Integer> model)
//        {
//            super("Tempo", score, null, parent, model);
//        }
//
//        //~ Methods --------------------------------------------------------------------------------
//        @Override
//        public int defineLayout (PanelBuilder builder,
//                                 CellConstraints cst,
//                                 int r)
//        {
//            r = super.defineLayout(builder, cst, r);
//
//            return tempo.defineLayout(builder, cst, r);
//        }
//
//        @Override
//        public boolean isValid ()
//        {
//            task.setTempo(read());
//
//            return true;
//        }
//
//        @Override
//        protected void display (Integer content)
//        {
//            tempo.spinner.setValue(content);
//        }
//
//        @Override
//        protected Integer read ()
//        {
//            commitSpinners();
//
//            return (int) tempo.spinner.getValue();
//        }
//
//        @Override
//        protected void setEnabled (boolean bool)
//        {
//            tempo.setEnabled(bool);
//        }
//
//        private void commitSpinners ()
//        {
//            try {
//                tempo.spinner.commitEdit();
//            } catch (ParseException ignored) {
//            }
//        }
//    }
///TempoPane defaultTempoPane = new TempoPane(null, null, Tempo.defaultTempo);
//            // Tempo: depends on page
//            panes.add(new TempoPane(book, defaultTempoPane, book.getTempoParam()));
//
//            // Parts: depends on score
//            if (book.getPartList() != null) {
//                // Part by part information
//                panes.add(new PartsPane(book));
//            }
//
//    //-----------//
//    // PartsPane //
//    //-----------//
//    /**
//     * Pane to define the details for every part of the score.
//     * Scope can be: score.
//     */
//    private class PartsPane
//            extends Pane<List<PartData>>
//    {
//        //~ Instance fields ------------------------------------------------------------------------
//
//        /** All score part panes */
//        private final List<PartPanel> partPanels = new ArrayList<PartPanel>();
//
//        //~ Constructors ---------------------------------------------------------------------------
//        public PartsPane (Score score)
//        {
//            super("Parts", score, null, null, score.getPartsParam());
//        }
//
//        //~ Methods --------------------------------------------------------------------------------
//        @Override
//        public int defineLayout (PanelBuilder builder,
//                                 CellConstraints cst,
//                                 int r)
//        {
//            r = super.defineLayout(builder, cst, r);
//
//            for (LogicalPart logicalPart : book.getPartList()) {
//                PartPanel partPanel = new PartPanel(logicalPart);
//                r = partPanel.defineLayout(builder, cst, r);
//                partPanels.add(partPanel);
//                builder.add(partPanel, cst.xy(1, r));
//                r += 2;
//            }
//
//            return r;
//        }
//
//        @Override
//        public int getLogicalRowCount ()
//        {
//            return 2 + (PartPanel.logicalRowCount * book.getPartList().size());
//        }
//
//        @Override
//        public boolean isValid ()
//        {
//            // Each score part
//            for (PartPanel partPanel : partPanels) {
//                if (!partPanel.checkPart()) {
//                    return false;
//                }
//            }
//
//            return true;
//        }
//
//        @Override
//        protected void display (List<PartData> content)
//        {
//            for (int i = 0; i < content.size(); i++) {
//                PartPanel partPanel = partPanels.get(i);
//                PartData partData = content.get(i);
//                partPanel.display(partData);
//            }
//        }
//
//        @Override
//        protected List<PartData> read ()
//        {
//            List<PartData> data = new ArrayList<PartData>();
//
//            for (PartPanel partPanel : partPanels) {
//                data.add(partPanel.getData());
//            }
//
//            return data;
//        }
//
//        @Override
//        protected void setEnabled (boolean bool)
//        {
//            for (PartPanel partPanel : partPanels) {
//                partPanel.setItemsEnabled(bool);
//            }
//        }
//    }

//    //-----------//
//    // PartPanel //
//    //-----------//
//    /**
//     * Panel for details of one score part.
//     */
//    private static class PartPanel
//            extends Panel
//    {
//        //~ Static fields/initializers -------------------------------------------------------------
//
//        public static final int logicalRowCount = 3;
//
//        //~ Instance fields ------------------------------------------------------------------------
//        //
//        private final JLabel label;
//
//        /** Id of the part */
//        private final LTextField id = new LTextField("Id", "Id of the score part");
//
//        /** Name of the part */
//        private final LTextField name = new LTextField(true, "Name", "Name for the score part");
//
//        /** Midi Instrument */
//        private final JLabel midiLabel = new JLabel("Midi");
//
//        private final JComboBox<String> midiBox = new JComboBox<String>(
//                MidiAbstractions.getProgramNames());
//
//        //~ Constructors ---------------------------------------------------------------------------
//        public PartPanel (LogicalPart part)
//        {
//            label = new JLabel("Part #" + part.getId());
//
//            // Let's impose the id!
//            id.setText(part.getPid());
//        }
//
//        //~ Methods --------------------------------------------------------------------------------
//        public boolean checkPart ()
//        {
//            // Part name
//            if (name.getText().trim().length() == 0) {
//                logger.warn("Please supply a non empty part name");
//
//                return false;
//            } else {
//                return true;
//            }
//        }
//
//        public PartData getData ()
//        {
//            return new PartData(name.getText(), midiBox.getSelectedIndex() + 1);
//        }
//
//        private int defineLayout (PanelBuilder builder,
//                                  CellConstraints cst,
//                                  int r)
//        {
//            builder.add(label, cst.xyw(5, r, 7));
//
//            r += 2; // --
//
//            builder.add(id.getLabel(), cst.xy(5, r));
//            builder.add(id.getField(), cst.xy(7, r));
//
//            builder.add(name.getLabel(), cst.xy(9, r));
//            builder.add(name.getField(), cst.xy(11, r));
//
//            r += 2; // --
//
//            builder.add(midiLabel, cst.xy(5, r));
//            builder.add(midiBox, cst.xyw(7, r, 5));
//
//            return r;
//        }
//
//        private void display (PartData partData)
//        {
//            // Setting for part name
//            name.setText(partData.name);
//
//            // Setting for part midi program
//            midiBox.setSelectedIndex(partData.program - 1);
//        }
//
//        private void setItemsEnabled (boolean sel)
//        {
//            label.setEnabled(sel);
//            id.setEnabled(sel);
//            name.setEnabled(sel);
//            midiLabel.setEnabled(sel);
//            midiBox.setEnabled(sel);
//        }
//    }
//
//
//    //--------------//
//    // ParallelPane //
//    //--------------//
//    /**
//     * Should we use defaultParallelism as much as possible.
//     * Scope can be: default.
//     */
//    private static class ParallelPane
//            extends BooleanPane
//    {
//        //~ Constructors ---------------------------------------------------------------------------
//
//        public ParallelPane ()
//        {
//            super(
//                    "Parallelism",
//                    null,
//                    "Allowed",
//                    "Should we use parallelism whenever possible",
//                    OmrExecutors.defaultParallelism);
//        }
//    }
