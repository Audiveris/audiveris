//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               L o g i c a l P a r t s E d i t o r                              //
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

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.score.LogicalPart;
import org.audiveris.omr.score.PageNumber;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.score.PartRef;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.score.StaffConfig;
import org.audiveris.omr.score.SystemRef;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.ui.util.Panel;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationActionMap;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Class <code>LogicalPartsEditor</code> is a dialog meant to review and organize the
 * definition of all logical parts in a score.
 *
 * @author Hervé Bitteur
 */
public class LogicalPartsEditor
        extends AbstractBean
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LogicalPartsEditor.class);

    private static final ApplicationContext applicationContext = Application.getInstance()
            .getContext();

    private static final ResourceMap resources = applicationContext.getResourceMap(
            LogicalPartsEditor.class);

    private static final Map<Header, ColumnInfo> infos = new EnumMap<>(Header.class);

    static {
        for (Header header : Header.values()) {
            infos.put(header, new ColumnInfo(header));
        }
    }

    /** Name of property linked to one logical selected. */
    private static final String SELECTED = "selected";

    /** Name of property linked to ability to move up. */
    private static final String UP_ENABLED = "upEnabled";

    /** Name of property linked to ability to move down. */
    private static final String DOWN_ENABLED = "downEnabled";

    /** Name of property linked to ability to save. */
    private static final String MODIFIED = "modified";

    /** Name of property linked to ability to lock. */
    private static final String LOCKED = resources.getString("locked");

    private static final String UNLOCKED = resources.getString("unlocked");

    /** Icon for status locked. */
    private static final Icon lockedIcon = resources.getIcon("lockedIcon");

    /** Icon for status unlocked. */
    private static final Icon unlockedIcon = resources.getIcon("unlockedIcon");

    /** The root component. */
    private final JDialog dialog;

    //~ Instance fields ----------------------------------------------------------------------------

    /** The containing score. */
    private final Score score;

    /** Underlying table model. */
    private final MyTableModel model = new MyTableModel();

    /** Underlying table. */
    private final JTable table = new MyTable(model);

    /** The button used to lock/unlock the logicals. */
    private JButton lockButton;

    private boolean selected = false;

    // Properties that govern actions enabled/disabled

    private boolean upEnabled = false;

    private boolean downEnabled = false;

    private boolean modified = false;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>LogicalPartsEditor</code> object.
     *
     * @param score the edited score
     */
    public LogicalPartsEditor (Score score)
    {
        this.score = score;

        // Non-modal dialog
        dialog = new JDialog(OMR.gui.getFrame(), null, false);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // To avoid memory leak when user closes dialog via the upper right cross
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing (WindowEvent we)
            {
                score.setLogicalsEditor(null);
                dialog.dispose();
            }
        });

        table.getSelectionModel().addListSelectionListener( (ListSelectionEvent e) -> {
            logger.debug("ListSelection valueChanged");
            setSelected(checkSelected());
            setUpEnabled(checkUpEnabled());
            setDownEnabled(checkDownEnabled());
        });

        defineLayout();

        // Populate model
        for (LogicalPart logical : score.getLogicalParts()) {
            model.logicals.add(logical.copy());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----//
    // add //
    //-----//
    /**
     * Add one logical and move selection to the new one.
     *
     * @param e the event that triggered this action
     */
    @Action
    public void add (ActionEvent e)
    {
        // Use a rather standard config
        final LogicalPart logical = new LogicalPart(0, 1, Arrays.asList(new StaffConfig(5, false)));
        logical.setName("<empty>");
        model.add(logical);

        final int row = model.size() - 1;
        table.getSelectionModel().setSelectionInterval(row, row);
        model.fireTableRowsUpdated(row, row);

        setModified(true);
    }

    //-----------------//
    // buildButtonPane //
    //-----------------//
    /**
     * Build the whole pane of buttons, one button per declared action.
     *
     * @return the button pane
     */
    private Panel buildButtonPane ()
    {
        final ApplicationActionMap actionMap = applicationContext.getActionMap(this);
        final Panel buttonPane = new Panel();
        buttonPane.setInsets(10, 10, 10, 10);

        // NOTA: Changing a name here implies to change the name of the corresponding method
        final String[] actions = new String[]
        { "add", "duplicate", "remove", "moveUp", "moveDown", "save", "lock" };

        final StringBuilder rowSpec = new StringBuilder();
        for (int i = 0; i < actions.length; i++) {
            if (i != 0) {
                rowSpec.append(", 2dlu, ");
            }

            rowSpec.append("pref");
        }

        final FormLayout layout = new FormLayout("pref", rowSpec.toString());
        final FormBuilder builder = FormBuilder.create().layout(layout).panel(buttonPane);

        int r = -1;
        for (String action : actions) {
            final JButton button = new JButton(actionMap.get(action));

            // Specific customization for the Lock/Unlock button
            if (action.equals("lock")) {
                lockButton = button;
                lockButton.setText(isLocked() ? LOCKED : UNLOCKED);
                lockButton.setIcon(isLocked() ? lockedIcon : unlockedIcon);
            }

            builder.addRaw(button).xy(1, r += 2, "fill,fill");
        }

        return buttonPane;
    }

    private boolean checkDownEnabled ()
    {
        final int row = table.getSelectedRow();
        return (row != -1) && (row < table.getRowCount() - 1);
    }

    private boolean checkSelected ()
    {
        return table.getSelectedRowCount() > 0;
    }

    private boolean checkUpEnabled ()
    {
        return table.getSelectedRow() > 0;
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        dialog.setName("LogicalPartsEditor"); // For SAF life cycle

        // Alternate color for zebra appearance
        final Color zebraColor = new Color(248, 248, 255);
        UIManager.put("Table.alternateRowColor", zebraColor);

        // LogicalPart's table
        JScrollPane scrollPane = new JScrollPane(table);
        table.setShowGrid(true);
        table.setRowHeight(30);
        table.setIntercellSpacing(new Dimension(10, 5));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Table preferred columns width
        final TableColumnModel columnModel = table.getColumnModel();
        for (Header header : Header.values()) {
            columnModel.getColumn(header.ordinal()).setPreferredWidth(header.width);
        }

        // Text centered in cells
        final DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);

        // Buttons
        final Panel buttonPane = buildButtonPane();

        // Dialog layout
        dialog.setTitle(
                MessageFormat.format(
                        resources.getString("dialog.title"),
                        score.getId(),
                        score.getBook().getRadix()));
        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPane, BorderLayout.EAST);

        // I18n
        resources.injectComponents(dialog);
    }

    //-----------//
    // duplicate //
    //-----------//
    /**
     * Duplicate the selected logical in the table and move selection to the new one.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SELECTED)
    public void duplicate (ActionEvent e)
    {
        final int row = table.getSelectedRow();
        final LogicalPart log = model.get(row);
        final LogicalPart dup = new LogicalPart(0, log.getStaffCount(), log.getStaffConfigs());
        model.add(row + 1, dup);

        table.getSelectionModel().setSelectionInterval(row + 1, row + 1);
        model.fireTableRowsUpdated(row + 1, model.size());
        setModified(true);
    }

    //--------------//
    // getComponent //
    //--------------//
    public JDialog getComponent ()
    {
        return dialog;
    }

    public boolean isDownEnabled ()
    {
        return downEnabled;
    }

    public boolean isLocked ()
    {
        return score.isLogicalsLocked();
    }

    public boolean isModified ()
    {
        return modified;
    }

    public boolean isSelected ()
    {
        return selected;
    }

    public boolean isUpEnabled ()
    {
        return upEnabled;
    }

    //------//
    // lock //
    //------//
    /**
     * Lock / Unlock the logicals.
     *
     * @param e the event that triggered this action
     */
    @Action
    public void lock (ActionEvent e)
    {
        setLocked(!isLocked());
    }

    //----------//
    // moveDown //
    //----------//
    /**
     * Move the selected logical one row down.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = DOWN_ENABLED)
    public void moveDown (ActionEvent e)
    {
        final int row = table.getSelectedRow();
        final LogicalPart rowData = model.get(row);

        final int other = row + 1;
        final LogicalPart otherData = model.get(other);

        model.set(other, rowData);
        model.set(row, otherData);

        table.getSelectionModel().setSelectionInterval(other, other);
        model.fireTableRowsUpdated(row, other);
        setModified(true);
    }

    //--------//
    // moveUp //
    //--------//
    /**
     * Move the selected logical one row up.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = UP_ENABLED)
    public void moveUp (ActionEvent e)
    {
        final int row = table.getSelectedRow();
        final LogicalPart rowData = model.get(row);

        final int other = row - 1;
        final LogicalPart otherData = model.get(other);

        model.set(other, rowData);
        model.set(row, otherData);

        table.getSelectionModel().setSelectionInterval(other, other);
        model.fireTableRowsUpdated(other, row);
        setModified(true);
    }

    private void remap (int oldId,
                        Integer newId,
                        LogicalPart logicalPart,
                        Set<PartRef> updated)
    {
        for (PageNumber pageNumber : score.getPageNumbers()) {
            final SheetStub stub = score.getStub(pageNumber);
            final boolean isLoaded = stub.hasSheet();
            final PageRef pageRef = pageNumber.getPageRef(score.getBook());

            for (SystemRef systemRef : pageRef.getSystems()) {
                for (PartRef partRef : systemRef.getParts()) {
                    if (!updated.contains(partRef)) {
                        final Integer logId = partRef.getLogicalId();
                        if ((logId != null) && (logId == oldId)) {
                            logger.debug(
                                    "{}/ P{} S{} {}/{} new logicalId: {}",
                                    oldId,
                                    pageNumber.sheetNumber,
                                    systemRef.getId(),
                                    partRef.getIndex() + 1,
                                    partRef,
                                    newId);
                            if (isLoaded) {
                                final Part part = partRef.getRealPart();
                                part.setLogicalPart(newId, logicalPart);
                            }
                            partRef.setLogicalId(newId);
                            updated.add(partRef);

                            if (newId == null) {
                                partRef.setManual(false);
                            }
                        }
                    }
                }
            }
        }
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove the selected logical.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SELECTED)
    public void remove (ActionEvent e)
    {
        final int row = table.getSelectedRow();

        if (row != -1) {
            model.remove(row);
            setModified(true);
        }
    }

    //------//
    // save //
    //------//
    /**
     * Save the user editings.
     * <p>
     * Keep assignments, especially the manual ones, as much as possible.
     * <ul>
     * <li>Modified logicals have kept the same id.
     * <li>Added logicals have id 0.
     * <li>Removed logicals are gone with their id.
     * </ul>
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = MODIFIED)
    public void save (ActionEvent e)
    {
        final List<LogicalPart> olds = new ArrayList<>(score.getLogicalParts());
        final Set<PartRef> updated = new HashSet<>(); // PartRef's updated so far

        for (LogicalPart old : olds) {
            final int oldId = old.getId();
            final LogicalPart mod = model.getById(oldId);
            final Integer newId = (mod != null) ? model.getRank(mod) : null;
            remap(oldId, newId, mod, updated);
        }

        model.renumberLogicals();
        score.setLogicalParts(model.logicals);

        final Book book = score.getBook();
        book.setModified(true);

        final SheetStub currentStub = StubsController.getCurrentStub();
        if (currentStub.getBook() == book) {
            currentStub.getSheet().getSheetEditor().refresh();
        }

        setLocked(true);
        setModified(false);
    }

    public void setDownEnabled (boolean downEnabled)
    {
        boolean oldValue = this.downEnabled;
        this.downEnabled = downEnabled;

        if (downEnabled != oldValue) {
            firePropertyChange(DOWN_ENABLED, oldValue, this.downEnabled);
        }
    }

    public void setLocked (boolean locked)
    {
        boolean oldValue = score.isLogicalsLocked();
        score.setLogicalsLocked(locked);

        if (locked != oldValue) {
            firePropertyChange(LOCKED, oldValue, score.isLogicalsLocked());

            // Update related button text and icon accordingly
            lockButton.setText(isLocked() ? LOCKED : UNLOCKED);
            lockButton.setIcon(isLocked() ? lockedIcon : unlockedIcon);
        }
    }

    public void setModified (boolean modified)
    {
        boolean oldValue = this.modified;
        this.modified = modified;

        if (modified != oldValue) {
            firePropertyChange(MODIFIED, oldValue, this.modified);
        }
    }

    public void setSelected (boolean selected)
    {
        boolean oldValue = this.selected;
        this.selected = selected;

        if (selected != oldValue) {
            firePropertyChange(SELECTED, oldValue, this.selected);
        }
    }

    public void setUpEnabled (boolean upEnabled)
    {
        boolean oldValue = this.upEnabled;
        this.upEnabled = upEnabled;

        if (upEnabled != oldValue) {
            firePropertyChange(UP_ENABLED, oldValue, this.upEnabled);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //------------//
    // ColumnInfo //
    //------------//
    /**
     * Gathers information about a table column.
     */
    private static class ColumnInfo
    {
        public final String text;

        public final String toolTipText;

        public ColumnInfo (Header header)
        {
            this.text = resources.getString("header" + header + ".text");
            this.toolTipText = resources.getString("header" + header + ".toolTipText");
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer maxMidi = new Constant.Integer(
                "id",
                128,
                "Maximum MIDI program number");

        private final Constant.Integer maxLineCount = new Constant.Integer(
                "count",
                6,
                "Maximum staff line count");
    }

    //--------//
    // Header //
    //--------//
    /** Sequence and description of columns. */
    private static enum Header
    {
        Staves(25),
        Name(80),
        Abbrev(30),
        Midi(20);

        public final int width; // Preferred column width

        Header (int width)
        {
            this.width = width;
        }
    }

    //---------//
    // MyTable //
    //---------//
    /**
     * Sub-class for specific columns headers tool tips.
     */
    private static class MyTable
            extends JTable
    {
        public MyTable (TableModel model)
        {
            super(model);
        }

        @Override
        protected JTableHeader createDefaultTableHeader ()
        {
            return new JTableHeader(columnModel)
            {
                @Override
                public String getToolTipText (MouseEvent e)
                {
                    final Point p = e.getPoint();
                    final int index = columnModel.getColumnIndexAtX(p.x);
                    final int realIndex = columnModel.getColumn(index).getModelIndex();
                    final Header header = Header.values()[realIndex];

                    return infos.get(header).toolTipText;
                }
            };
        }
    }

    //--------------//
    // MyTableModel //
    //--------------//
    /**
     * Underlying model for the dialog table.
     */
    private class MyTableModel
            extends AbstractTableModel
    {
        private final List<LogicalPart> logicals = new ArrayList<>(); // Underlying data

        public void add (int row,
                         LogicalPart logical)
        {
            logicals.add(row, logical);
            fireTableRowsInserted(row, row);
        }

        public void add (LogicalPart logical)
        {
            add(size(), logical);
        }

        public LogicalPart get (int row)
        {
            return logicals.get(row);
        }

        /**
         * Report the logical, if any, that has the provided id.
         *
         * @param id desired id
         * @return the corresponding logical or null if not found
         */
        public LogicalPart getById (int id)
        {
            for (LogicalPart log : logicals) {
                if (log.getId() == id) {
                    return log;
                }
            }

            return null;
        }

        @Override
        public int getColumnCount ()
        {
            return infos.size();
        }

        @Override
        public String getColumnName (int col)
        {
            final Header header = Header.values()[col];
            return infos.get(header).text;
        }

        /**
         * Report what will be the new id (that is the rank) for the provided logical.
         *
         * @param logical provided logical
         * @return its rank in model logicals
         */
        public int getRank (LogicalPart logical)
        {
            return 1 + logicals.indexOf(logical);
        }

        @Override
        public int getRowCount ()
        {
            return size();
        }

        @Override
        public Object getValueAt (int row,
                                  int col)
        {
            final LogicalPart logical = get(row);
            final Header header = Header.values()[col];

            return switch (header) {
                case Staves -> StaffConfig.toCsvString(logical.getStaffConfigs());
                case Name -> logical.getName();
                case Abbrev -> logical.getAbbreviation();
                case Midi -> logical.getMidiProgram();
            };
        }

        @Override
        public boolean isCellEditable (int row,
                                       int col)
        {
            return true;
        }

        public void remove (int row)
        {
            logicals.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public void renumberLogicals ()
        {
            for (int i = 0; i < logicals.size(); i++) {
                logicals.get(i).setId(i + 1);
            }
        }

        public void set (int row,
                         LogicalPart logical)
        {
            logicals.set(row, logical);
        }

        @Override
        public void setValueAt (Object value,
                                int row,
                                int col)
        {
            final LogicalPart logical = get(row);
            final Header header = Header.values()[col];
            logger.debug("setValueAt row:{} col:{} value:{}", row, col, value);

            switch (header) {
                case Name -> {
                    final String newName = (String) value;
                    logical.setName(newName);
                }
                case Abbrev -> {
                    final String newAbbrev = (String) value;
                    logical.setAbbreviation(newAbbrev);
                }
                case Midi -> {
                    final String newMidi = (String) value;
                    if (newMidi.isBlank()) {
                        logical.setMidiProgram(null);
                    } else {
                        try {
                            final int midi = Integer.decode(newMidi);
                            if (midi < 1 || midi > constants.maxMidi.getValue()) {
                                logger.warn("Illegal midi value: {}", midi);
                            } else {
                                logical.setMidiProgram(midi);
                            }
                        } catch (NumberFormatException ex) {
                            logger.warn("Illegal midi value: '{}'", newMidi);
                        }
                    }
                }
                case Staves -> {
                    final String str = (String) value;
                    final String[] tokens = str.split("\\s*,\\s*");
                    final List<StaffConfig> newConfigs = new ArrayList<>();
                    boolean ok = true;

                    for (String rawToken : tokens) {
                        final String token = rawToken.trim();

                        if (token.isEmpty()) {
                            logger.warn("Illegal staff config: '{}'", str);
                            ok = false;
                            break;
                        } else {
                            try {
                                final StaffConfig config = StaffConfig.decode(token);
                                final int count = config.count;
                                if (count < 1 || count > constants.maxLineCount.getValue()) {
                                    logger.warn("Illegal count value: '{}'", count);
                                    ok = false;
                                    break;
                                }

                                newConfigs.add(config);
                            } catch (Exception ex) {
                                logger.warn("Illegal config: '{}'", token);
                                ok = false;
                                break;
                            }
                        }
                    }

                    if (ok) {
                        logical.setStaffConfigs(newConfigs);
                    }
                }
            }

            fireTableCellUpdated(row, col);
            setModified(true);
        }

        public int size ()
        {
            return logicals.size();
        }
    }
}
