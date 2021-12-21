//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S p l i t A n d M e r g e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.BookManager;
import org.audiveris.omr.sheet.PlayList;
import org.audiveris.omr.sheet.PlayList.BookExcerpt;
import org.audiveris.omr.sheet.PlayList.Excerpt;
import org.audiveris.omr.sheet.SheetStub;
import static org.audiveris.omr.sheet.ui.BookActions.isTargetConfirmed;
import static org.audiveris.omr.sheet.ui.BookActions.selectBookPath;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.ui.FileDropHandler;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.util.OmrFileFilter;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.util.NaturalSpec;
import org.audiveris.omr.util.PathTask;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationActionMap;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Class <code>SplitAndMerge</code> allows the end-user to split and merge books.
 * <p>
 * The interface presents a table, with one row per BookExcerpt that the user can modify:
 * <ul>
 * <li>Load, include, duplicate, remove, move up/down any BookExcerpt
 * <li>Modify the sheets spec within any BookExcerpt
 * </ul>
 * And finally save the result as a new book.
 *
 * @author Hervé Bitteur
 */
public class SplitAndMerge
        extends AbstractBean
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SplitAndMerge.class);

    private static final ApplicationContext applicationContext
            = Application.getInstance().getContext();

    private static final ResourceMap resources = applicationContext.getResourceMap(
            SplitAndMerge.class);

    /** Column index for book radix. */
    private static final int COL_RADIX = 0;

    /** Column index for sheet specification. */
    private static final int COL_SPEC = 1;

    /** Column index for resulting counts. */
    private static final int COL_COUNTS = 2;

    /** Name of property linked to model empty. */
    private static final String EMPTY = "empty";

    /** Name of property linked to excerpts buildable. */
    private static final String BUILDABLE = "buildable";

    /** Name of property linked to one excerpt selected. */
    private static final String SELECTED = "selected";

    /** Name of property linked to ability to move up. */
    private static final String UP_ENABLED = "upEnabled";

    /** Name of property linked to ability to move down. */
    private static final String DOWN_ENABLED = "downEnabled";

    //~ Instance fields ----------------------------------------------------------------------------
    /** The root component. */
    private final JDialog dialog;

    /** Underlying table model. */
    private final MyTableModel model = new MyTableModel();

    /** Underlying table. */
    private final JTable table = new JTable(model);

    /** Illegal excerpts. */
    private final Set<BookExcerpt> illegals = new HashSet<>();

    // Properties that govern actions enabled/disabled
    //
    private boolean empty = true;

    private boolean buildable = false;

    private boolean selected = false;

    private boolean upEnabled = false;

    private boolean downEnabled = false;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>SplitAndMerge</code> object.
     *
     * @param playListPath path to provided play list, or null
     */
    public SplitAndMerge (Path playListPath)
    {
        dialog = new JDialog(OMR.gui.getFrame());
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // To avoid memory leak when user closes window via the upper right cross
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing (WindowEvent we)
            {
                dialog.dispose();
            }
        });

        defineLayout();

        model.addTableModelListener((TableModelEvent e) -> {
            setEmpty(checkEmpty());
            setBuildable(checkBuildable());
        });

        table.setDropMode(DropMode.INSERT_ROWS);

        table.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            logger.debug("ListSelection valueChanged");
            setSelected(checkSelected());
            setUpEnabled(checkUpEnabled());
            setDownEnabled(checkDownEnabled());
        });

        // Specific renderer for illegal specification
        table.getColumnModel().getColumn(COL_SPEC).setCellRenderer(new SpecRenderer());

        // Can receive dropped files
        table.setTransferHandler(new MyTransferHandler());

        // Start with a specific playlist?
        if (playListPath != null) {
            SwingUtilities.invokeLater(() -> new OpenPlayListTask(playListPath).execute());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // build //
    //-------//
    /**
     * Build a compound book from the current playlist.
     *
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action(enabledProperty = BUILDABLE)
    public Task build (ActionEvent e)
    {
        // First, make sure all books don't need to be saved
        final Set<Book> books = model.getAllBooks();
        final List<String> unsaved = new ArrayList<>();

        for (Book book : books) {
            if (!isSafe(book)) {
                unsaved.add(book.getPath().toString());
            }
        }

        if (!unsaved.isEmpty()) {
            OMR.gui.displayWarning(new JList<>(unsaved.toArray(String[]::new)),
                                   resources.getString("unsavedBooks"));
            return null;
        }

        try {
            // Second, let the user select a book output file
            final Path defaultFolder = Paths.get(constants.lastFolder.getValue());
            final Path targetPath = selectBookPath(true, defaultFolder);

            if ((targetPath != null) && isTargetConfirmed(targetPath)) {
                // Finally, launch building of the compound book
                return new BuildCompoundTask(targetPath);
            }
        } catch (Throwable ex) {
            logger.warn("Error in SplitAndMerge.build {}", ex.toString(), ex);
        }

        return null;
    }

    //--------------//
    // getComponent //
    //--------------//
    public JDialog getComponent ()
    {
        return dialog;
    }

    //-----------//
    // duplicate //
    //-----------//
    /**
     * Duplicate the selected excerpt in the table and move selection to the new one.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SELECTED)
    public void duplicate (ActionEvent e)
    {
        final int row = table.getSelectedRow();
        final BookExcerpt excerpt = model.get(row);
        final BookExcerpt dup = BookExcerpt.create(excerpt.book, excerpt.specification);
        model.add(row + 1, dup);

        table.getSelectionModel().setSelectionInterval(row + 1, row + 1);
        model.fireTableRowsUpdated(row + 1, model.size());
    }

    //---------//
    // include //
    //---------//
    /**
     * Add one or several book excerpts from the books already loaded in application.
     *
     * @param e the event that triggered this action
     */
    @Action
    public void include (ActionEvent e)
    {
        final String frameTitle = resources.getString("includeDialog.title");
        final JDialog includeDialog = new JDialog(OMR.gui.getFrame(), frameTitle, true); // Modal
        final JList<Book> list = new JList<>(OMR.engine.getAllBooks().toArray(Book[]::new));

        // This renderer is needed to replace default book.toString() by the book path.toString()
        list.setCellRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent (
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus)
            {
                final JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                final Book book = (Book) value;
                final Path path = book.getPath();
                label.setText(path.toString());

                return label;
            }
        });

        final JOptionPane optionPane = new JOptionPane(
                new JScrollPane(list),
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        optionPane.addPropertyChangeListener((PropertyChangeEvent e1) -> {
            final String prop = e1.getPropertyName();
            if (includeDialog.isVisible() && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                final int value = (Integer) optionPane.getValue();
                if (value == JOptionPane.OK_OPTION) {
                    for (Book book : list.getSelectedValuesList()) {
                        model.add(BookExcerpt.create(book));
                    }
                }

                includeDialog.setVisible(false);
                includeDialog.dispose();
            }
        });

        includeDialog.setContentPane(optionPane);
        includeDialog.setName("SplitAndMergeIncludeDialog"); // For SAF life cycle
        includeDialog.pack();
        OmrGui.getApplication().show(includeDialog);
    }

    //----------//
    // loadBook //
    //----------//
    /**
     * Load an external book file .
     *
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action
    public Task loadBook (ActionEvent e)
    {
        final Path path = BookActions.selectBookPath(false,
                                                     Paths.get(BookManager.getDefaultBookFolder()));

        if (path == null) {
            return null;
        }

        return new LoadBookTask(path);
    }

    //-----------//
    // loadImage //
    //-----------//
    /**
     * Load an external image file and include its related book.
     *
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action
    public Task loadImage (ActionEvent e)
    {
        final Path path = BookActions.selectImagePath();

        if (path == null) {
            return null;
        }

        return new LoadImageTask(path);
    }

    //----------//
    // moveDown //
    //----------//
    /**
     * Move the selected excerpt one row down.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = DOWN_ENABLED)
    public void moveDown (ActionEvent e)
    {
        final int row = table.getSelectedRow();
        final BookExcerpt rowData = model.get(row);

        final int other = row + 1;
        final BookExcerpt otherData = model.get(other);

        model.set(other, rowData);
        model.set(row, otherData);

        table.getSelectionModel().setSelectionInterval(other, other);
        model.fireTableRowsUpdated(row, other);
    }

    //--------//
    // moveUp //
    //--------//
    /**
     * Move the selected excerpt one row up.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = UP_ENABLED)
    public void moveUp (ActionEvent e)
    {
        final int row = table.getSelectedRow();
        final BookExcerpt rowData = model.get(row);

        final int other = row - 1;
        final BookExcerpt otherData = model.get(other);

        model.set(other, rowData);
        model.set(row, otherData);

        table.getSelectionModel().setSelectionInterval(other, other);
        model.fireTableRowsUpdated(other, row);
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove the selected excerpt.
     *
     * @param e the event that triggered this action
     */
    @Action(enabledProperty = SELECTED)
    public void remove (ActionEvent e)
    {
        final int row = table.getSelectedRow();

        if (row != -1) {
            model.remove(row);
        }
    }

    //------//
    // open //
    //------//
    /**
     * Read a playlist from disk.
     *
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action
    public Task open (ActionEvent e)
    {
        try {
            // Let the user select a playlist input file
            final OmrFileFilter filter = BookActions.filter(".xml");
            final Path defaultFolder = Paths.get(constants.lastFolder.getValue());
            final Path targetPath = BookActions.selectPath(false, defaultFolder, filter);

            if (targetPath != null) {
                return new OpenPlayListTask(targetPath);
            }
        } catch (Throwable ex) {
            logger.warn("Error in open {}", ex.toString(), ex);
        }

        return null;
    }

    //------//
    // save //
    //------//
    /**
     * Save the playlist to disk for future use.
     *
     * @param e the event that triggered this action
     * @return the asynchronous task, or null
     */
    @Action(enabledProperty = BUILDABLE)
    public Task save (ActionEvent e)
    {
        try {
            // Let the user select a playlist output file
            final OmrFileFilter filter = BookActions.filter(".xml");
            final Path defaultFolder = Paths.get(constants.lastFolder.getValue());
            final Path targetPath = BookActions.selectPath(true, defaultFolder, filter);

            if ((targetPath != null) && isTargetConfirmed(targetPath)) {
                return new SavePlayListTask(targetPath);
            }
        } catch (Throwable ex) {
            logger.warn("Error in SavePlayListTask {}", ex.toString(), ex);
        }

        return null;
    }

    public boolean isBuildable ()
    {
        return buildable;
    }

    public final void setBuildable (boolean buildable)
    {
        boolean oldValue = this.buildable;
        this.buildable = buildable;

        if (buildable != oldValue) {
            firePropertyChange(BUILDABLE, oldValue, this.buildable);
        }
    }

    public boolean isEmpty ()
    {
        return empty;
    }

    public final void setEmpty (boolean empty)
    {
        boolean oldValue = this.empty;
        this.empty = empty;

        if (empty != oldValue) {
            firePropertyChange(EMPTY, oldValue, this.empty);
        }
    }

    public boolean isSelected ()
    {
        return selected;
    }

    public void setSelected (boolean selected)
    {
        boolean oldValue = this.selected;
        this.selected = selected;

        if (selected != oldValue) {
            firePropertyChange(SELECTED, oldValue, this.selected);
        }
    }

    public boolean isUpEnabled ()
    {
        return upEnabled;
    }

    public void setUpEnabled (boolean upEnabled)
    {
        boolean oldValue = this.upEnabled;
        this.upEnabled = upEnabled;

        if (upEnabled != oldValue) {
            firePropertyChange(UP_ENABLED, oldValue, this.upEnabled);
        }
    }

    public boolean isDownEnabled ()
    {
        return downEnabled;
    }

    public void setDownEnabled (boolean downEnabled)
    {
        boolean oldValue = this.downEnabled;
        this.downEnabled = downEnabled;

        if (downEnabled != oldValue) {
            firePropertyChange(DOWN_ENABLED, oldValue, this.downEnabled);
        }
    }

    private boolean checkDownEnabled ()
    {
        final int row = table.getSelectedRow();
        return (row != -1) && (row < table.getRowCount() - 1);
    }

    private boolean checkBuildable ()
    {
        return (model.size() > 0) && illegals.isEmpty();
    }

    private boolean checkEmpty ()
    {
        return model.size() == 0;
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
        dialog.setName("SplitAndMergeDialog"); // For SAF life cycle

        // Alternate color for zebra appearance
        final Color zebraColor = new Color(248, 248, 255);
        UIManager.put("Table.alternateRowColor", zebraColor);

        // BookExcerpt's table
        JScrollPane scrollPane = new JScrollPane(table);
        table.setShowGrid(true);
        table.setRowHeight(30);
        table.setIntercellSpacing(new Dimension(10, 5));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Buttons
        final Panel buttonPane = buildButtonPane();

        // Dialog layout
        dialog.setTitle(resources.getString("dialog.title"));
        dialog.setLayout(new BorderLayout());
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPane, BorderLayout.EAST);

        // I18n
        resources.injectComponents(dialog);
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

        final String[] actions = new String[]{"open", "loadBook", "loadImage", "include",
                                              "duplicate", "remove", "moveUp", "moveDown",
                                              "save", "build"};

        final StringBuilder rowSpec = new StringBuilder();
        for (int i = 0; i < actions.length; i++) {
            if (i != 0) {
                rowSpec.append(", 5dlu, ");
            }

            rowSpec.append("pref");
        }

        final FormLayout layout = new FormLayout("pref", rowSpec.toString());
        final CellConstraints cst = new CellConstraints();
        final PanelBuilder builder = new PanelBuilder(layout, buttonPane);

        int r = -1;
        for (String action : actions) {
            builder.add(new JButton(actionMap.get(action)), cst.xy(1, r += 2, "fill,fill"));
        }

        return buttonPane;
    }

    //---------//
    // isImage //
    //---------//
    /**
     * Report whether the provided book is just a book created on-the-fly to represent
     * an image file.
     *
     * @param book the book to check
     * @return true if so
     */
    public static boolean isImage (Book book)
    {
        return book.getBookPath() == null;
    }

    //--------//
    // isSafe //
    //--------//
    /**
     * Report whether the provided book is safe regarding the building of compound file.
     *
     * @param book the book to check
     * @return true if so
     */
    private static boolean isSafe (Book book)
    {
        if (!book.isModified() && !book.isUpgraded()) {
            return true;
        }

        if (!isImage(book)) {
            return false;
        }

        // Book has never been saved
        for (SheetStub stub : book.getStubs()) {
            final OmrStep latestStep = stub.getLatestStep();
            if (latestStep != null && latestStep.compareTo(OmrStep.BINARY) > 0) {
                return false;
            }
        }

        return true;
    }

    //-----------------//
    // setPlayListPath //
    //-----------------//
    /**
     * Records the path to play list and update folder and title accordingly.
     *
     * @param path the path to PlayList file
     */
    private void setPlayListPath (Path path)
    {
        dialog.setTitle(resources.getString("dialog.title") + " - " + path);
        constants.lastFolder.setStringValue(path.getParent().toAbsolutePath().toString());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.String lastFolder = new Constant.String(
                "",
                "Latest folder used for playlist or compound");
    }

    //--------------//
    // MyTableModel //
    //--------------//
    private class MyTableModel
            extends AbstractTableModel
    {

        private final String[] columnNames = {resources.getString("headerBook"),
                                              resources.getString("headerSpec"),
                                              resources.getString("headerCounts")};

        // Underlying data
        private final PlayList pl = new PlayList();

        //~ Methods --------------------------------------------------------------------------------
        public void add (BookExcerpt excerpt)
        {
            add(size(), excerpt);
        }

        public void add (int row,
                         BookExcerpt excerpt)
        {
            pl.excerpts.add(row, excerpt);
            fireTableRowsInserted(row, row);
        }

        public BookExcerpt get (int row)
        {
            return (BookExcerpt) pl.excerpts.get(row);
        }

        public void remove (int row)
        {
            pl.excerpts.remove(row);
            fireTableRowsDeleted(row, row);
        }

        public void set (int row,
                         BookExcerpt excerpt)
        {
            pl.excerpts.set(row, excerpt);
        }

        public int size ()
        {
            return pl.excerpts.size();
        }

        public Set<Book> getAllBooks ()
        {
            final Set<Book> set = new LinkedHashSet<>();

            for (Excerpt excerpt : pl.excerpts) {
                set.add(((BookExcerpt) excerpt).book);
            }

            return set;
        }

        @Override
        public int getColumnCount ()
        {
            return columnNames.length;
        }

        @Override
        public String getColumnName (int column)
        {
            return columnNames[column];
        }

        @Override
        public int getRowCount ()
        {
            return size();
        }

        @Override
        public Object getValueAt (int rowIndex,
                                  int columnIndex)
        {
            final BookExcerpt excerpt = get(rowIndex);

            if (columnIndex == COL_RADIX) {
                return excerpt.bookId;
            }

            if (columnIndex == COL_SPEC) {
                return excerpt.specification;
            }

            return excerpt.counts;
        }

        @Override
        public boolean isCellEditable (int rowIndex,
                                       int columnIndex)
        {
            return columnIndex == COL_SPEC;
        }

        @Override
        public void setValueAt (Object value,
                                int row,
                                int col)
        {
            logger.debug("setValueAt row:{} col:{} value:{}", row, col, value);;;;;;;;;;;;

            if (col == COL_SPEC) {
                // Specification modified
                final BookExcerpt excerpt = get(row);
                final String newSpec = (String) value;
                logger.debug("{} to be set as '{}'", excerpt, newSpec);

                try {
                    final int maxId = excerpt.book.size();
                    excerpt.specification = NaturalSpec.normalized(newSpec, maxId); // This may fail
                    illegals.remove(excerpt);

                    // Update counts accordingly
                    excerpt.counts = NaturalSpec.getCounts(excerpt.specification, maxId);
                } catch (Exception ex) {
                    logger.warn("Illegal specification: " + newSpec);
                    excerpt.specification = newSpec; // So that user can see and modify field data
                    excerpt.counts = "?";
                    illegals.add(excerpt);
                }

                model.fireTableCellUpdated(row, COL_COUNTS);
                setBuildable(checkBuildable());
            }
        }
    }

    //--------------//
    // SpecRenderer //
    //--------------//
    /**
     * This cell renderer allows to highlight any illegal sheets specification.
     */
    private class SpecRenderer
            extends DefaultTableCellRenderer
    {

        @Override
        public Component getTableCellRendererComponent (JTable table,
                                                        Object value,
                                                        boolean isSelected,
                                                        boolean hasFocus,
                                                        int row,
                                                        int column)
        {
            final JLabel comp = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            final String spec = (String) value;
            try {
                // Let's throw an exception if spec is invalid...
                NaturalSpec.normalized(spec, model.get(row).book.size());

                // Spec is OK
                comp.setForeground(table.getForeground());
            } catch (Exception ex) {
                // Spec is not OK
                comp.setForeground(Color.red);
                final Font font = table.getFont();
                comp.setFont(font.deriveFont(Font.BOLD, 1.25f * font.getSize()));
            }

            return comp;
        }
    }

    //-------------------//
    // MyTransferHandler //
    //-------------------//
    /**
     * For the SplitAndMerge dialog, we support transfer from a list of files
     * (book.omr or image input file).
     */
    private class MyTransferHandler
            extends FileDropHandler
    {

        @Override
        public boolean importData (TransferHandler.TransferSupport support)
        {
            if (canImport(support)) {
                try {
                    // Fetch the Transferable and its data
                    final Transferable trsf = support.getTransferable();
                    final Object data = trsf.getTransferData(DataFlavor.javaFileListFlavor);

                    @SuppressWarnings("unchecked")
                    final List<File> fileList = (List<File>) data;
                    final int row = ((JTable.DropLocation) support.getDropLocation()).getRow();

                    // Process every file
                    for (File file : fileList) {
                        final Path path = file.toPath();
                        final String fileName = file.getName();

                        if (fileName.endsWith(OMR.BOOK_EXTENSION)) {
                            new DropBookTask(row, path).execute();
                        } else {
                            new DropImageTask(row, path).execute();
                        }
                    }

                    return true;
                } catch (UnsupportedFlavorException ex) {
                    logger.warn("Unsupported flavor in drag & drop", ex);
                } catch (IOException ex) {
                    logger.warn("IO Exception in drag & drop", ex);
                }
            }

            return false;
        }
    }

    //-------------------//
    // BuildCompoundTask //
    //-------------------//
    /**
     * Task that compiles the sequence of excerpts into a compound book.
     */
    private class BuildCompoundTask
            extends PathTask<Void, Void>
    {

        /**
         * Creates a new <code> BuildCompoundTask</code>, with the target book path.
         *
         * @param targetPath path to resulting compound book file
         */
        public BuildCompoundTask (Path targetPath)
        {
            super(targetPath);
        }

        @Override
        protected Void doInBackground ()
                throws Exception
        {
            model.pl.buildCompound(path);
            return null;
        }

        @Override
        protected void succeeded (Void result)
        {
            constants.lastFolder.setStringValue(path.getParent().toAbsolutePath().toString());
        }
    }

    //--------------//
    // LoadBookTask //
    //--------------//
    private class LoadBookTask
            extends BookActions.LoadBookTask
    {

        public LoadBookTask (Path path)
        {
            super(path);
        }

        @Override
        protected void succeeded (Book book)
        {
            if (book != null) {
                addExcerpt(book);
                super.succeeded(book);
            }
        }

        protected void addExcerpt (Book book)
        {
            model.add(BookExcerpt.create(book));
        }
    }

    //---------------//
    // LoadImageTask //
    //---------------//
    private class LoadImageTask
            extends BookActions.LoadImageTask
    {

        public LoadImageTask (Path path)
        {
            super(path);
        }

        @Override
        protected void succeeded (Book book)
        {
            if (book != null) {
                addExcerpt(book);
                super.succeeded(book);
            }
        }

        protected void addExcerpt (Book book)
        {
            model.add(BookExcerpt.create(book));
        }
    }

    //--------------//
    // DropBookTask //
    //--------------//
    private class DropBookTask
            extends LoadBookTask
    {

        /** Target row in table. */
        private final int row;

        public DropBookTask (int row,
                             Path path)
        {
            super(path);
            this.row = row;
        }

        @Override
        protected void addExcerpt (Book book)
        {
            model.add(row, BookExcerpt.create(book));
        }
    }

    //---------------//
    // DropImageTask //
    //---------------//
    private class DropImageTask
            extends LoadImageTask
    {

        /** Target row in table. */
        private final int row;

        public DropImageTask (int row,
                              Path path)
        {
            super(path);
            this.row = row;
        }

        @Override
        protected void addExcerpt (Book book)
        {
            model.add(row, BookExcerpt.create(book));
        }
    }

    //------------------//
    // OpenPlayListTask //
    //------------------//
    /**
     * Task to unmarshal playlist from disk.
     */
    private class OpenPlayListTask
            extends PathTask<PlayList, Void>
    {

        public OpenPlayListTask (Path targetPath)
        {
            super(targetPath);
        }

        @Override
        protected PlayList doInBackground ()
                throws Exception
        {
            final PlayList playList = PlayList.load(path);

            if (playList != null) {
                playList.injectBooks(); // Ensure books are loaded
            }

            return playList;
        }

        @Override
        protected void succeeded (PlayList playlist)
        {
            if (playlist != null) {
                // Populate excerpts
                for (Excerpt excerpt : playlist.excerpts) {
                    final Path bookPath = excerpt.path;
                    final Book book = OMR.engine.getBook(bookPath);
                    model.add(BookExcerpt.create(book, excerpt.specification));
                }

                setPlayListPath(path);

                // Previews
                final StubsController controller = StubsController.getInstance();

                for (Excerpt excerpt : playlist.excerpts) {
                    final Path bookPath = excerpt.path;
                    final Book book = OMR.engine.getBook(bookPath);

                    if (!controller.isDisplayed(book)) {
                        final int maxId = book.size();

                        String spec = excerpt.specification;
                        if (spec == null) {
                            spec = "";
                        }

                        final List<Integer> ids = NaturalSpec.decode(spec, false, maxId);
                        final Integer first = ids.isEmpty() ? null : ids.get(0);
                        controller.displayStubs(book, first);
                    }
                }
            }
        }
    }

    //------------------//
    // SavePlayListTask //
    //------------------//
    /**
     * Task to marshal playlist to disk.
     */
    private class SavePlayListTask
            extends PathTask<Void, Void>
    {

        public SavePlayListTask (Path targetPath)
        {
            super(targetPath);
        }

        @Override
        protected Void doInBackground ()
                throws Exception
        {
            model.pl.store(path);
            return null;
        }

        @Override
        protected void succeeded (Void result)
        {
            setPlayListPath(path);
        }
    }
}
