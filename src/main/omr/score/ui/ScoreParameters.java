//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S c o r e P a r a m e t e r s                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.image.AdaptiveDescriptor;
import omr.image.FilterDescriptor;
import omr.image.FilterKind;
import omr.image.GlobalDescriptor;

import omr.plugin.PluginManager;

import omr.score.entity.LogicalPart;
import omr.score.midi.MidiAbstractions;

import omr.script.ParametersTask;
import omr.script.ParametersTask.PartData;
import omr.script.ScriptActions;

import omr.sheet.Book;
import omr.sheet.Sheet;

import omr.step.Step;

import omr.text.Language;
import omr.text.OCR.UnavailableOcrException;

import omr.ui.FileDropHandler;
import omr.ui.field.LTextField;
import omr.ui.field.SpinnerUtil;
import omr.ui.util.Panel;

import omr.util.OmrExecutors;
import omr.util.Param;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JCheckBox;
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
 * <p>
 * <div style="float: right;">
 * <img src="doc-files/ScoreParameters.png" />
 * </div>
 *
 * <p>
 * It addresses:
 * <ul>
 * <li>Text language specification</li>
 * <li>Binarization parameters</li>
 * <li>Step triggered by drag and drop</li>
 * <li>Prompt for saving script on closing</li>
 * <li>Call-stack printed on exception</li>
 * <li>Parallelism allowed or not</li>
 * <li>Name and instrument related to each score part</li>
 * </ul>
 *
 * <p>
 * The dialog is organized as a scope-based tabbed pane with:
 * <ul>
 * <li>a panel for the <b>default</b> scope,</li>
 * <li>a panel for current <b>score</b> scope (provided that there is a selected score),</li>
 * <li>and one panel for every <b>page</b> scope (provided that the score contains more than a
 * single page).</li>
 * </ul>
 *
 * <p>
 * A panel is a vertical collection of panes, each pane being introduced by a check box and a label.
 * Initially the box is unchecked and the pane content is disabled.
 * <br/>Manually checking the box represents a selection and indicates the intention to modify the
 * pane content (and thus enables the pane fields).
 * <br/>Unchecking the box reverts the content to the value it had prior to the selection.
 *
 * <p>
 * The selected modifications are actually performed (and this may launch some costly re-processing)
 * only when the user presses the OK button.
 *
 * @author Hervé Bitteur
 */
public class ScoreParameters
        implements ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ScoreParameters.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The swing component of this panel. */
    private final JTabbedPane component = new JTabbedPane();

    /** The related book, if any. */
    private final Book book;

    //    /** The related page, if any. */
    //    private final Page page;
    //
    /** The panel dedicated to setting of defaults. */
    private final MyPanel defaultPanel;

    /** Related script task. */
    private ParametersTask task;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a ScoreParameters object.
     *
     * @param sheet the current sheet, or null
     */
    public ScoreParameters (Sheet sheet)
    {
        if (sheet != null) {
            this.book = sheet.getBook();
        } else {
            book = null;
        }

        component.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // Allocate all required panels (default / book? / sheets??)
        MyPanel scorePanel;
        MyPanel pagePanel = null;

        // Default panel
        TextPane defaultTextPane = createTextPane(
                null,
                null,
                null,
                Language.defaultSpecification);
        FilterPane defaultFilterPane = new FilterPane(
                null,
                null,
                null,
                FilterDescriptor.defaultFilter);
        ///TempoPane defaultTempoPane = new TempoPane(null, null, Tempo.defaultTempo);
        defaultPanel = new MyPanel(
                "Default settings",
                defaultTextPane,
                defaultFilterPane,
                ///defaultTempoPane,
                new PluginPane(),
                new DnDPane(),
                new ScriptPane(),
                new ParallelPane());

        component.addTab("Default", null, defaultPanel, defaultPanel.getName());

        // Book panel?
        if (book != null) {
            List<Pane> panes = new ArrayList<Pane>();

            TextPane scoreTextPane = createTextPane(
                    book,
                    null,
                    defaultTextPane,
                    book.getTextParam());
            panes.add(scoreTextPane);

            FilterPane scoreFilterPane = new FilterPane(
                    book,
                    null,
                    defaultFilterPane,
                    book.getFilterParam());
            panes.add(scoreFilterPane);

            //            // Tempo: depends on page
            //            panes.add(new TempoPane(book, defaultTempoPane, book.getTempoParam()));
            //
            //            // Parts: depends on score
            //            if (book.getPartList() != null) {
            //                // Part by part information
            //                panes.add(new PartsPane(book));
            //            }
            scorePanel = new MyPanel("Score settings", panes);
            component.addTab(book.getRadix(), null, scorePanel, scorePanel.getName());

            // Sheets panels?
            if (book.isMultiSheet()) {
                for (Sheet sh : book.getSheets()) {
                    MyPanel panel = new MyPanel(
                            "Page settings",
                            createTextPane(null, sh, scoreTextPane, sh.getTextParam()),
                            new FilterPane(null, sh, scoreFilterPane, sh.getFilterParam()));
                    component.addTab("P#" + sh.getIndex(), null, panel, panel.getName());

                    if (sh == sheet) {
                        pagePanel = panel;
                    }
                }
            }
        } else {
            scorePanel = null;
        }

        // Initially selected tab
        component.addChangeListener(this);
        component.setSelectedComponent(
                (pagePanel != null) ? pagePanel : ((scorePanel != null) ? scorePanel : defaultPanel));
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // commit //
    //--------//
    /**
     * Check the values and commit them if all are OK.
     *
     * @param sheet the related sheet
     * @return true if committed, false otherwise
     */
    public boolean commit (Sheet sheet)
    {
        if (dataIsValid()) {
            try {
                // Commit all specific values, if any, to their backup object
                // Do this ONLY for the Default panel
                MyPanel panel = defaultPanel;

                //logger.info("{}", panel.getName());
                for (Pane pane : panel.panes) {
                    pane.commit();
                }

                // Launch the prepared task (for score & pages)
                if (sheet != null) {
                    task.launch(sheet);
                }
            } catch (Exception ex) {
                logger.warn("Could not run ParametersTask", ex);

                return false;
            }

            return true;
        } else {
            return false;
        }
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
        MyPanel panel = (MyPanel) component.getSelectedComponent();

        for (Pane pane : panel.panes) {
            pane.display(pane.getTarget());
        }
    }

    //----------------//
    // createTextPane //
    //----------------//
    /**
     * Factory method to get a TextPane, while handling exception when
     * no OCR is available.
     *
     * @param book
     * @param sheet
     * @param parent
     * @return A usable TextPane instance, or null otherwise
     */
    private TextPane createTextPane (Book book,
                                     Sheet sheet,
                                     TextPane parent,
                                     Param<String> backup)
    {
        // Caution: The language pane needs Tesseract up & running
        try {
            return new TextPane(book, sheet, parent, backup);
        } catch (UnavailableOcrException ex) {
            logger.info("No language pane for lack of OCR");
        } catch (Throwable ex) {
            logger.warn("Error creating language pane", ex);
        }

        return null;
    }

    //-------------//
    // dataIsValid //
    //-------------//
    /**
     * Make sure every user-entered data is valid, and while doing so,
     * feed a ParametersTask to be later run on the related score/sheet
     *
     * @return true if everything is OK, false otherwise
     */
    private boolean dataIsValid ()
    {
        task = new ParametersTask();

        // Loop on all panes of all panels
        for (int t = 0, tBreak = component.getTabCount(); t < tBreak; t++) {
            MyPanel panel = (MyPanel) component.getComponentAt(t);

            for (Pane pane : panel.panes) {
                if (pane.isSelected() && !pane.isValid()) {
                    task = null; // Cleaner

                    return false;
                }
            }
        }

        return true;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //
    //---------//
    // MyPanel //
    //---------//
    /**
     * A panel corresponding to a tab.
     */
    private final class MyPanel
            extends Panel
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Collection of individual data panes */
        private final List<Pane> panes = new ArrayList<Pane>();

        //~ Constructors ---------------------------------------------------------------------------
        public MyPanel (String name,
                        Pane... panes)
        {
            this(name, Arrays.asList(panes));
        }

        public MyPanel (String name,
                        List<Pane> panes)
        {
            setName(name);

            for (Pane pane : panes) {
                if (pane != null) {
                    this.panes.add(pane);
                }
            }

            defineLayout();

            // Initially, all panes are deselected
            for (Pane pane : this.panes) {
                pane.box.setSelected(false);
                pane.actionPerformed(null);
            }
        }

        //~ Methods --------------------------------------------------------------------------------
        public void defineLayout ()
        {
            // Compute the total number of logical rows
            int logicalRowCount = 0;

            for (Pane pane : panes) {
                logicalRowCount += pane.getLogicalRowCount();
            }

            FormLayout layout = Panel.makeFormLayout(
                    logicalRowCount,
                    3,
                    "right:",
                    "30dlu",
                    "35dlu");
            PanelBuilder builder = new PanelBuilder(layout, this);
            builder.setDefaultDialogBorder();

            CellConstraints cst = new CellConstraints();
            int r = 1;

            for (Pane pane : panes) {
                r = pane.defineLayout(builder, cst, r);
            }
        }
    }

    //-------------//
    // BooleanPane //
    //-------------//
    /**
     * A template for pane with just one global boolean, and
     * no score or page relationship.
     * Scope can be: default.
     */
    private abstract class BooleanPane
            extends Pane<Boolean>
    {
        //~ Instance fields ------------------------------------------------------------------------

        /**
         * Use a ComboBox for boolean, since current status is more readable
         * than a plain CheckBox
         */
        private final JComboBox<Boolean> box = new JComboBox(
                new Boolean[]{Boolean.FALSE, Boolean.TRUE});

        private final JLabel label;

        //~ Constructors ---------------------------------------------------------------------------
        public BooleanPane (String label,
                            String text,
                            String tip,
                            Param<Boolean> backup)
        {
            super(label, null, null, null, backup);

            this.label = new JLabel(text, SwingConstants.RIGHT);
            box.setToolTipText(tip);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            r = super.defineLayout(builder, cst, r);

            builder.add(label, cst.xyw(5, r, 3));
            builder.add(box, cst.xyw(9, r, 3));

            return r + 2;
        }

        @Override
        public void setEnabled (boolean bool)
        {
            box.setEnabled(bool);
            label.setEnabled(bool);
        }

        @Override
        protected void display (Boolean content)
        {
            box.setSelectedItem(content ? Boolean.TRUE : Boolean.FALSE);
        }

        @Override
        protected Boolean read ()
        {
            return box.getItemAt(box.getSelectedIndex());
        }
    }

    //------//
    // Pane //
    //------//
    /**
     * A pane is able to host data, check data validity and apply the
     * requested modifications.
     */
    private abstract class Pane<E>
            extends Param<E>
            implements ActionListener
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Backup parameter (cannot be null). */
        protected final Param<E> backup;

        /** Related book, if any. */
        protected final Book book;

        /** Related sheet, if any. */
        protected final Sheet sheet;

        /** Box for selecting specific vs inherited data. */
        private final JCheckBox box;

        /** Title for the pane. */
        private final String title;

        //~ Constructors ---------------------------------------------------------------------------
        public Pane (String title,
                     Book book,
                     Sheet sheet,
                     Pane parent,
                     Param<E> backup)
        {
            super(parent);

            if (backup == null) {
                throw new IllegalArgumentException("Null backup for pane '" + title + "'");
            }

            this.backup = backup;
            this.title = title;
            this.book = book;
            this.sheet = sheet;

            box = new JCheckBox();
            box.addActionListener(this);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Pane (de)selection (programmatic or manual)
            boolean sel = isSelected();

            setEnabled(sel);

            if (!sel) {
                display(getTarget());
            }
        }

        /**
         * Commit the modifications, for the items that are not handled
         * by the ParametersTask, which means all actions related to
         * default values.
         */
        public void commit ()
        {
            if (isSelected()) {
                //logger.info("   {}: {}", title, read());
                backup.setSpecific(read());
            }
        }

        /**
         * Build the related user interface
         *
         * @param builder the shared panel builder
         * @param cst     the cell constraints
         * @param r       initial row value
         * @return final row value
         */
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            // Draw the specific/inherit box + separating line
            builder.add(box, cst.xyw(1, r, 1));
            builder.addSeparator(title, cst.xyw(3, r, 9));
            r += 2;

            return r;
        }

        /**
         * Report the count of needed logical rows.
         * Typically 2 (the label separator plus 1 line of data)
         */
        public int getLogicalRowCount ()
        {
            return 2;
        }

        /**
         * Report the specific value for this pane, if any.
         *
         * @return the fields content when selected, otherwise the backup
         *         specific data if any.
         */
        @Override
        public E getSpecific ()
        {
            if (isSelected()) {
                return read();
            } else if (backup != null) {
                return backup.getSpecific();
            } else {
                return null;
            }
        }

        /**
         * User has selected (and enabled) this pane
         *
         * @return true if selected
         */
        public boolean isSelected ()
        {
            return box.isSelected();
        }

        /**
         * Check whether all the pane data are valid, and feed the
         * ParametersTask accordingly with score or page information.
         *
         * @return true if everything is OK, false otherwise
         */
        public boolean isValid ()
        {
            return true; // By default
        }

        /**
         * User selects (or deselects) this pane
         *
         * @param bool true for selection
         */
        public void setSelected (boolean bool)
        {
            box.setSelected(bool);
        }

        /**
         * Write the parameter into the fields content
         *
         * @param content the data to display
         */
        protected abstract void display (E content);

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
        protected abstract void setEnabled (boolean bool);
    }

    //---------//
    // DnDPane //
    //---------//
    /**
     * Which step should we trigger on Drag n' Drop?.
     * Scope can be: default.
     */
    private class DnDPane
            extends Pane<Step>
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** ComboBox for desired step */
        private final JComboBox<Step> stepCombo;

        private final JLabel stepLabel = new JLabel("Triggered step", SwingConstants.RIGHT);

        //~ Constructors ---------------------------------------------------------------------------
        public DnDPane ()
        {
            super("Drag n' Drop", null, null, null, FileDropHandler.defaultStep);

            // ComboBox for triggered step
            stepCombo = new JComboBox<Step>(Step.values());
            stepCombo.setToolTipText("Step to trigger on Drag n' Drop");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            r = super.defineLayout(builder, cst, r);

            builder.add(stepLabel, cst.xyw(5, r, 3));
            builder.add(stepCombo, cst.xyw(9, r, 3));

            return r + 2;
        }

        @Override
        protected void display (Step content)
        {
            stepCombo.setSelectedItem(content);
        }

        @Override
        protected Step read ()
        {
            return stepCombo.getItemAt(stepCombo.getSelectedIndex());
        }

        @Override
        protected void setEnabled (boolean bool)
        {
            stepCombo.setEnabled(bool);
            stepLabel.setEnabled(bool);
        }
    }

    //------------//
    // FilterPane //
    //------------//
    /**
     * Pane to define the pixel binarization parameters.
     * Scope can be: default, score, page.
     */
    private class FilterPane
            extends Pane<FilterDescriptor>
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** ComboBox for filter kind */
        private final JComboBox<FilterKind> kindCombo = new JComboBox<FilterKind>(
                FilterKind.values());

        private final JLabel kindLabel = new JLabel("Filter", SwingConstants.RIGHT);

        // Data for global
        private final SpinData globalData = new SpinData(
                "Threshold",
                "Global threshold for foreground pixels",
                new SpinnerNumberModel(0, 0, 255, 1));

        // Data for local
        private final SpinData localDataMean = new SpinData(
                "Coeff for Mean",
                "Coefficient for mean pixel value",
                new SpinnerNumberModel(0.5, 0.5, 1.5, 0.1));

        private final SpinData localDataDev = new SpinData(
                "Coeff for StdDev",
                "Coefficient for standard deviation value",
                new SpinnerNumberModel(0.2, 0.2, 1.5, 0.1));

        //~ Constructors ---------------------------------------------------------------------------
        public FilterPane (Book book,
                           final Sheet sheet,
                           FilterPane parent,
                           Param<FilterDescriptor> backup)
        {
            super("Binarization", book, sheet, parent, backup);

            // ComboBox for filter kind
            kindCombo.setToolTipText("Specific filter on image pixels");
            kindCombo.addActionListener(this);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            if ((e != null) && (e.getSource() == kindCombo)) {
                // KindCombo: new kind
                switch (readKind()) {
                case GLOBAL:
                    localDataMean.setVisible(false);
                    localDataDev.setVisible(false);
                    globalData.setVisible(true);

                    // Use proper global data
                    display(GlobalDescriptor.getDefault());

                    break;

                case ADAPTIVE:
                    globalData.setVisible(false);
                    localDataMean.setVisible(true);
                    localDataDev.setVisible(true);

                    // Use proper adaptive data
                    display(AdaptiveDescriptor.getDefault());

                    break;

                default:
                }
            } else {
                super.actionPerformed(e);
            }
        }

        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            r = super.defineLayout(builder, cst, r);

            builder.add(kindLabel, cst.xyw(5, r, 3));
            builder.add(kindCombo, cst.xyw(9, r, 3));
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
        public boolean isValid ()
        {
            task.setFilter(read(), sheet);

            return true;
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

            return (readKind() == FilterKind.GLOBAL)
                    ? new GlobalDescriptor((int) globalData.spinner.getValue())
                    : new AdaptiveDescriptor(
                            (double) localDataMean.spinner.getValue(),
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

        /** This is needed to read data manually typed in spinners fields */
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

    //--------------//
    // ParallelPane //
    //--------------//
    /**
     * Should we use defaultParallelism as much as possible.
     * Scope can be: default.
     */
    private class ParallelPane
            extends BooleanPane
    {
        //~ Constructors ---------------------------------------------------------------------------

        public ParallelPane ()
        {
            super(
                    "Parallelism",
                    "Allowed",
                    "Should we use parallelism whenever possible",
                    OmrExecutors.defaultParallelism);
        }
    }

    //-----------//
    // PartPanel //
    //-----------//
    /**
     * Panel for details of one score part.
     */
    private class PartPanel
            extends Panel
    {
        //~ Static fields/initializers -------------------------------------------------------------

        public static final int logicalRowCount = 3;

        //~ Instance fields ------------------------------------------------------------------------
        //
        private final JLabel label;

        /** Id of the part */
        private final LTextField id = new LTextField("Id", "Id of the score part");

        /** Name of the part */
        private LTextField name = new LTextField(true, "Name", "Name for the score part");

        /** Midi Instrument */
        private JLabel midiLabel = new JLabel("Midi");

        private JComboBox<String> midiBox = new JComboBox<String>(
                MidiAbstractions.getProgramNames());

        //~ Constructors ---------------------------------------------------------------------------
        public PartPanel (LogicalPart logicalPart)
        {
            label = new JLabel("Part #" + logicalPart.getId());

            // Let's impose the id!
            id.setText(logicalPart.getPid());
        }

        //~ Methods --------------------------------------------------------------------------------
        public boolean checkPart ()
        {
            // Part name
            if (name.getText().trim().length() == 0) {
                logger.warn("Please supply a non empty part name");

                return false;
            } else {
                task.addPart(name.getText(), midiBox.getSelectedIndex() + 1);

                return true;
            }
        }

        public PartData getData ()
        {
            return new PartData(name.getText(), midiBox.getSelectedIndex() + 1);
        }

        private int defineLayout (PanelBuilder builder,
                                  CellConstraints cst,
                                  int r)
        {
            builder.add(label, cst.xyw(5, r, 7));

            r += 2; // --

            builder.add(id.getLabel(), cst.xy(5, r));
            builder.add(id.getField(), cst.xy(7, r));

            builder.add(name.getLabel(), cst.xy(9, r));
            builder.add(name.getField(), cst.xy(11, r));

            r += 2; // --

            builder.add(midiLabel, cst.xy(5, r));
            builder.add(midiBox, cst.xyw(7, r, 5));

            return r;
        }

        private void display (PartData partData)
        {
            // Setting for part name
            name.setText(partData.name);

            // Setting for part midi program
            midiBox.setSelectedIndex(partData.program - 1);
        }

        private void setItemsEnabled (boolean sel)
        {
            label.setEnabled(sel);
            id.setEnabled(sel);
            name.setEnabled(sel);
            midiLabel.setEnabled(sel);
            midiBox.setEnabled(sel);
        }
    }

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
    //------------//
    // PluginPane //
    //------------//
    /**
     * Which Plugin should be the default one.
     * Scope can be: default.
     */
    private class PluginPane
            extends Pane<String>
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** ComboBox for registered plugins */
        private final JComboBox<String> pluginCombo;

        private final JLabel pluginLabel = new JLabel("Default plugin", SwingConstants.RIGHT);

        //~ Constructors ---------------------------------------------------------------------------
        public PluginPane ()
        {
            super("Plugin", null, null, null, PluginManager.defaultPluginId);

            // ComboBox for triggered step
            pluginCombo = new JComboBox<String>(
                    PluginManager.getInstance().getPluginIds().toArray(new String[0]));
            pluginCombo.setToolTipText("Default plugin to be launched");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            r = super.defineLayout(builder, cst, r);

            builder.add(pluginLabel, cst.xyw(5, r, 3));
            builder.add(pluginCombo, cst.xyw(9, r, 3));

            return r + 2;
        }

        @Override
        protected void display (String content)
        {
            pluginCombo.setSelectedItem(content);
        }

        @Override
        protected String read ()
        {
            return pluginCombo.getItemAt(pluginCombo.getSelectedIndex());
        }

        @Override
        protected void setEnabled (boolean bool)
        {
            pluginCombo.setEnabled(bool);
            pluginLabel.setEnabled(bool);
        }
    }

    //------------//
    // ScriptPane //
    //------------//
    /**
     * Should we prompt the user for saving the script when sheet is
     * closed?.
     * Scope can be: default.
     */
    private class ScriptPane
            extends BooleanPane
    {
        //~ Constructors ---------------------------------------------------------------------------

        public ScriptPane ()
        {
            super(
                    "Script",
                    "Prompt for save",
                    "Should we prompt for saving the script on score closing",
                    ScriptActions.defaultPrompt);
        }
    }

    //----------//
    // SpinData //
    //----------//
    /**
     * A line with a labeled spinner.
     */
    private class SpinData
    {
        //~ Instance fields ------------------------------------------------------------------------

        protected final JLabel label;

        protected final JSpinner spinner;

        //~ Constructors ---------------------------------------------------------------------------
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

        //~ Methods --------------------------------------------------------------------------------
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            builder.add(label, cst.xyw(7, r, 3));
            builder.add(spinner, cst.xyw(11, r, 1));

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
    //                          Param<Integer> backup)
    //        {
    //            super("Tempo", score, null, parent, backup);
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
    //----------//
    // TextPane //
    //----------//
    /**
     * Pane to set the dominant text language specification.
     * Scope can be: default, book, sheet.
     */
    private class TextPane
            extends Pane<String>
            implements ListSelectionListener
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Underlying language list model. */
        Language.ListModel model = new Language.ListModel();

        /** List for choosing elements of language specification. */
        private final JList<String> langList = new JList<String>(model);

        /** Put the list into a scroll pane. */
        private final JScrollPane langScroll = new JScrollPane(langList);

        /** Resulting visible specification. */
        private final JLabel langSpec = new JLabel("", SwingConstants.RIGHT);

        //~ Constructors ---------------------------------------------------------------------------
        public TextPane (Book book,
                         Sheet sheet,
                         TextPane parent,
                         Param<String> backup)
        {
            super("Language", book, sheet, parent, backup);

            langList.setLayoutOrientation(JList.VERTICAL);
            langList.setToolTipText("Dominant languages for textual items");
            langList.setVisibleRowCount(5);
            langList.addListSelectionListener(this);

            langSpec.setToolTipText("Resulting specification");
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            r = super.defineLayout(builder, cst, r);

            builder.add(langSpec, cst.xyw(1, r, 7));
            builder.add(langScroll, cst.xyw(9, r, 3));

            return r + 2;
        }

        @Override
        public boolean isValid ()
        {
            task.setLanguage(read(), sheet);

            return true;
        }

        @Override
        public void valueChanged (ListSelectionEvent e)
        {
            langSpec.setText(read());
        }

        @Override
        protected void display (String spec)
        {
            int[] indices = model.indicesOf(spec);

            if ((indices.length > 0) && (indices[0] != -1)) {
                // Scroll to first index found?
                String firstElement = model.getElementAt(indices[0]);
                langList.setSelectedValue(firstElement, true);

                // Flag all selected indices
                langList.setSelectedIndices(indices);
            }

            langSpec.setText(spec);
        }

        @Override
        protected String read ()
        {
            return model.specOf(langList.getSelectedValuesList());
        }

        @Override
        protected void setEnabled (boolean bool)
        {
            langList.setEnabled(bool);
            langSpec.setEnabled(bool);
        }
    }
}
