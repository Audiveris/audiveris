//----------------------------------------------------------------------------//
//                                                                            //
//                        S a m p l e V e r i f i e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.WellKnowns;

import omr.glyph.GlyphRepository;
import omr.glyph.Shape;

import omr.ui.MainGui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Class {@code SampleVerifier} provides a user interface to browse
 * through all glyphs samples recorded for evaluator training,
 * to visually check the correctness of their assigned shape,
 * and to remove spurious samples when necessary.
 *
 * <p>One, several or all recorded sheets can be selected.
 *
 * <p>Within the contained glyphs, one, several or all can be selected, the
 * selected glyphs can then be browsed in any direction.
 *
 * <p>The current glyph is displayed, with its appearance in a properly
 * translated Nest view, and its characteristics in a dedicated panel.
 * If the user wants to discard the glyph, it can be removed from the repository
 * of training material.
 *
 * @author Hervé Bitteur
 */
public class SampleVerifier
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(SampleVerifier.class);

    /** The unique instance */
    private static volatile SampleVerifier INSTANCE;

    //~ Instance fields --------------------------------------------------------
    /** Repository of known glyphs */
    private final GlyphRepository repository = GlyphRepository.getInstance();

    /** The dedicated frame */
    private final JFrame frame;

    /** The panel in charge of the current glyph */
    private GlyphBrowser glyphBrowser = new GlyphBrowser(this);

    /** The panel in charge of the glyphs selection */
    private GlyphSelector glyphSelector = new GlyphSelector(glyphBrowser);

    /** The panel in charge of the shapes selection */
    private ShapeSelector shapeSelector = new ShapeSelector(glyphSelector);

    /** The panel in charge of the sheets (or icons folder) selection */
    private FolderSelector folderSelector = new FolderSelector(shapeSelector);

    /** Sheets folder */
    private final File sheetsFolder = repository.getSheetsFolder();

    /** Samples folder */
    private final File samplesFolder = repository.getSamplesFolder();

    //~ Constructors -----------------------------------------------------------
    //----------------//
    // SampleVerifier //
    //----------------//
    /**
     * Create an instance of SampleVerifier.
     */
    private SampleVerifier ()
    {
        // Pane split vertically: selectors then browser
        JSplitPane vertSplitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                getSelectorsPanel(),
                glyphBrowser.getComponent());
        vertSplitPane.setName("SampleVerifierSplitPane");
        vertSplitPane.setDividerSize(1);

        // Hosting frame
        frame = new JFrame();
        frame.setName("SampleVerifierFrame");
        frame.add(vertSplitPane);

        // Resource injection
        ResourceMap resource = MainGui.getInstance().getContext().
                getResourceMap(
                getClass());
        resource.injectComponents(frame);
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    /**
     * Give access to the single instance of this class.
     *
     * @return the SampleVerifier instance
     */
    public static SampleVerifier getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new SampleVerifier();
        }

        return INSTANCE;
    }

    //------------//
    // setVisible //
    //------------//
    /**
     * Make the UI frame visible or not.
     *
     * @param bool true for visible, false for hidden
     */
    public void setVisible (boolean bool)
    {
        MainGui.getInstance().show(frame);
    }

    //--------//
    // verify //
    //--------//
    /**
     * Focus the verifier on a provided collection of glyphs.
     * (typically this collection are the glyphs that are not recognized,
     * or mistaken, by the evaluator)
     *
     * @param glyphNames the names of the specific glyphs to inspect
     */
    public void verify (Collection<String> glyphNames)
    {
        // Glyphs
        glyphSelector.populateWith(glyphNames);
        glyphSelector.selectAll();

        // Shapes
        EnumSet<Shape> shapeSet = EnumSet.noneOf(Shape.class);

        for (String gName : glyphNames) {
            File file = new File(gName);
            shapeSet.add(Shape.valueOf(radixOf(file.getName())));
        }

        shapeSelector.populateWith(shapeSet);
        shapeSelector.selectAll();

        // Sheets / Icons folder
        SortedSet<String> folderSet = new TreeSet<>();

        for (String gName : glyphNames) {
            File file = new File(gName);
            folderSet.add(file.getParent());
        }

        folderSelector.populateWith(folderSet);
        folderSelector.selectAll();

        // Load the first glyph in the browser
        glyphBrowser.loadGlyphNames();
    }

    //-----------------//
    // deleteGlyphName //
    //-----------------//
    /**
     * Remove a glyph name from the current selection.
     *
     * @param gName the glyph name to remove
     */
    void deleteGlyphName (String gName)
    {
        // Remove entry from glyph list
        glyphSelector.model.removeElement(gName);
    }

    //---------------//
    // getGlyphCount //
    //---------------//
    /**
     * Report the number of currently selected glyphs names.
     *
     * @return the number of selected glyphs names
     */
    int getGlyphCount ()
    {
        return glyphSelector.list.getSelectedIndices().length;
    }

    //---------------//
    // getGlyphNames //
    //---------------//
    /**
     * Report the collection of currently selected glyphs names.
     *
     * @return an list of glyphs names
     */
    List<String> getGlyphNames ()
    {
        return glyphSelector.list.getSelectedValuesList();
    }

    //---------//
    // radixOf //
    //---------//
    private static String radixOf (String path)
    {
        int i = path.indexOf('.');

        if (i >= 0) {
            return path.substring(0, i);
        } else {
            return "";
        }
    }

    //--------------//
    // getActualDir //
    //--------------//
    /**
     * Report the real directory that corresponds to a given folder name.
     * (either the sheets or samples directory or the symbols directory)
     *
     * @param folder the folder name, such as 'symbols' or 'sheets/batuque' or
     *               'samples/batuque'
     * @return the concrete directory
     */
    private File getActualDir (String folder)
    {
        if (repository.isIconsFolder(folder)) {
            return WellKnowns.SYMBOLS_FOLDER;
        } else {
            int slashPos = folder.indexOf(File.separatorChar);
            String root = folder.substring(0, slashPos);
            String name = folder.substring(slashPos + 1);

            if (root.equals(sheetsFolder.getName())) {
                return new File(sheetsFolder, name);
            } else if (root.equals(samplesFolder.getName())) {
                return new File(samplesFolder, name);
            } else {
                throw new IllegalArgumentException("Unexpected root: " + root);
            }
        }
    }

    //-------------------//
    // getSelectorsPanel //
    //-------------------//
    private JPanel getSelectorsPanel ()
    {
        FormLayout layout = new FormLayout(
                "max(100dlu;pref),max(150dlu;pref),max(200dlu;pref):grow", // Cols
                "pref:grow"); // Rows

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------
        builder.add(folderSelector, cst.xy(1, r));
        builder.add(shapeSelector, cst.xy(2, r));
        builder.add(glyphSelector, cst.xy(3, r));

        return builder.getPanel();
    }

    //~ Inner Classes ----------------------------------------------------------
    //----------------//
    // FolderSelector //
    //----------------//
    private class FolderSelector
            extends Selector<String>
    {
        //~ Constructors -------------------------------------------------------

        public FolderSelector (ChangeListener listener)
        {
            super("Folders", listener, 300);
            load.setEnabled(true);
        }

        //~ Methods ------------------------------------------------------------
        // Triggered by load button
        @Override
        public void actionPerformed (ActionEvent e)
        {
            model.removeAllElements();

            // First insert the dedicated icons folder
            model.addElement(WellKnowns.SYMBOLS_FOLDER.getName());

            // Then the sheets folders
            String root = repository.getSheetsFolder().getName();
            ArrayList<String> folders = new ArrayList<>();

            for (File file : repository.getSheetDirectories()) {
                folders.add(root + File.separator + file.getName());
            }

            // Finally, the samples folders
            root = repository.getSamplesFolder().getName();

            for (File file : repository.getSampleDirectories()) {
                folders.add(root + File.separator + file.getName());
            }

            Collections.sort(folders);

            for (String folder : folders) {
                model.addElement(folder);
            }

            updateCardinal();
        }
    }

    //---------------//
    // GlyphSelector //
    //---------------//
    private class GlyphSelector
            extends Selector<String>
    {
        //~ Constructors -------------------------------------------------------

        public GlyphSelector (ChangeListener listener)
        {
            super("Glyphs", listener, 300);
        }

        //~ Methods ------------------------------------------------------------
        // Triggered by the load button
        @Override
        public void actionPerformed (ActionEvent e)
        {
            final List<String> folders = folderSelector.list.
                    getSelectedValuesList();
            final List<Shape> shapes = shapeSelector.list.
                    getSelectedValuesList();

            // Debug
            if (logger.isDebugEnabled()) {
                logger.debug("Glyph Selector. Got Folders:");

                for (String fName : folders) {
                    logger.debug(fName);
                }

                logger.debug("Glyph Selector. Got Shapes:");

                for (Shape shape : shapes) {
                    logger.debug(shape.toString());
                }
            }

            if (shapes.isEmpty()) {
                logger.warn("No shapes selected in Shape Selector");
            } else {
                model.removeAllElements();

                // Populate with all possible glyphs, sorted by gName
                for (String folder : folders) {
                    // Add proper glyphs files from this directory
                    ArrayList<String> gNames = new ArrayList<>();
                    File dir = getActualDir(folder);

                    for (File file : repository.getGlyphsIn(dir)) {
                        String shapeName = radixOf(file.getName());
                        Shape shape = Shape.valueOf(shapeName);

                        if (shapes.contains(shape)) {
                            gNames.add(
                                    folder + File.separator + file.getName());
                        }
                    }

                    Collections.sort(gNames);

                    for (String gName : gNames) {
                        model.addElement(gName);
                    }
                }

                updateCardinal();
            }
        }
    }

    //----------//
    // Selector //
    //----------//
    /**
     * Class {@code Selector} defines the common properties of sheet,
     * shape and glyph selectors.
     * Each selector is made of a list of names, which can be selected and
     * deselected at will.
     */
    private abstract static class Selector<E>
            extends TitledPanel
            implements ActionListener,
                       ChangeListener
    {
        //~ Instance fields ----------------------------------------------------

        /** The title base for this selector */
        private final String title;

        /** Other entity interested in items selected by this selector */
        private ChangeListener listener;

        /** Change event, lazily created */
        private ChangeEvent changeEvent;

        // Buttons
        protected JButton load = new JButton("Load");

        protected JButton selectAll = new JButton(
                "Select All");

        protected JButton cancelAll = new JButton(
                "Cancel All");

        // List of items, with its model
        protected final DefaultListModel<E> model = new DefaultListModel<>();

        protected JList<E> list = new JList<>(model);

        // ScrollPane around the list
        protected JScrollPane scrollPane = new JScrollPane(list);

        //~ Constructors -------------------------------------------------------
        //----------//
        // Selector //
        //----------//
        /**
         * Create a selector.
         *
         * @param title     label for this selector
         * @param listener  potential (external) listener for changes
         * @param preferred width
         */
        public Selector (String title,
                         ChangeListener listener,
                         int width)
        {
            super(title, width);
            this.title = title;
            this.listener = listener;

            // Precise action to be specified in each subclass
            load.addActionListener(this);

            ///list.setVisibleRowCount(10);
            ///scrollPane.setMinimumSize(new Dimension(250, 300));

            // To be informed of mouse (de)selections (not programmatic)
            list.addListSelectionListener(
                    new ListSelectionListener()
            {
                @Override
                public void valueChanged (ListSelectionEvent e)
                {
                    updateCardinal(); // Brute force !!!
                }
            });

            // Same action whatever the subclass : select all items
            selectAll.addActionListener(
                    new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    selectAll();
                }
            });

            // Same action whatever the subclass : deselect all items
            cancelAll.addActionListener(
                    new ActionListener()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    list.setSelectedIndices(new int[0]);
                    updateCardinal();
                }
            });

            JPanel buttons = new JPanel(new GridLayout(3, 1));
            buttons.add(load);
            buttons.add(selectAll);
            buttons.add(cancelAll);

            // All buttons are initially disabled
            load.setEnabled(false);
            selectAll.setEnabled(false);
            cancelAll.setEnabled(false);

            add(buttons, BorderLayout.WEST);
            add(scrollPane, BorderLayout.CENTER);
        }

        //~ Methods ------------------------------------------------------------
        //--------------//
        // populateWith //
        //--------------//
        public void populateWith (Collection<E> items)
        {
            model.removeAllElements();

            for (E item : items) {
                model.addElement(item);
            }

            updateCardinal();
        }

        //-----------//
        // selectAll //
        //-----------//
        public void selectAll ()
        {
            list.setSelectionInterval(0, model.size() - 1);
            updateCardinal();
        }

        //--------------//
        // stateChanged //
        //--------------//
        @Override
        public void stateChanged (ChangeEvent e)
        {
            Selector<?> selector = (Selector<?>) e.getSource();
            int selNb = selector.list.getSelectedIndices().length;
            load.setEnabled(selNb > 0);
        }

        //----------------//
        // updateCardinal //
        //----------------//
        protected void updateCardinal ()
        {
            int[] selection = list.getSelectedIndices();
            int selectNb = selection.length;

            TitledBorder border = (TitledBorder) getBorder();

            if (selectNb > 0) {
                border.setTitle(title + ": " + selectNb);
            } else {
                border.setTitle(title);
            }

            // Buttons
            selectAll.setEnabled(model.size() > 0);
            cancelAll.setEnabled(selection.length > 0);

            // Notify other entity
            if (listener != null) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }

                listener.stateChanged(changeEvent);
            }

            repaint();
        }
    }

    //-------------------//
    // ShapeCellRenderer //
    //-------------------//
    private class ShapeCellRenderer
            extends JLabel
            implements ListCellRenderer<Shape>
    {
        //~ Constructors -------------------------------------------------------

        public ShapeCellRenderer ()
        {
            setOpaque(true);
        }

        //~ Methods ------------------------------------------------------------

        /*
         * This method finds the image and text corresponding
         * to the selected value and returns the label, set up
         * to display the text and image.
         */
        @Override
        public Component getListCellRendererComponent (
                JList<? extends Shape> list,
                Shape shape,
                int index,
                boolean isSelected,
                boolean cellHasFocus)
        {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(shape.getColor());
            }

            setFont(list.getFont());
            setText(shape.toString());
            setIcon(shape.getDecoratedSymbol());

            return this;
        }
    }

    //---------------//
    // ShapeSelector //
    //---------------//
    private class ShapeSelector
            extends Selector<Shape>
    {
        //~ Constructors -------------------------------------------------------

        public ShapeSelector (ChangeListener listener)
        {
            super("Shapes", listener, 150);
            list.setCellRenderer(new ShapeCellRenderer());

            ///list.setFixedCellHeight(60);
        }

        //~ Methods ------------------------------------------------------------
        // Triggered by load button
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Populate with shape names found in selected folders
            List<String> folders = folderSelector.list.getSelectedValuesList();

            if (folders.isEmpty()) {
                logger.warn("No folders selected in Folder Selector");
            } else {
                EnumSet<Shape> shapeSet = EnumSet.noneOf(Shape.class);

                for (String folder : folders) {
                    File dir = getActualDir(folder);

                    // Add all glyphs files from this directory
                    for (File file : repository.getGlyphsIn(dir)) {
                        shapeSet.add(Shape.valueOf(radixOf(file.getName())));
                    }
                }

                populateWith(shapeSet);
            }
        }
    }

    //-------------//
    // TitledPanel //
    //-------------//
    private static class TitledPanel
            extends JPanel
    {
        //~ Instance fields ----------------------------------------------------

        protected final int height = 200;

        //~ Constructors -------------------------------------------------------
        public TitledPanel (String title,
                            int width)
        {
            setBorder(
                    BorderFactory.createTitledBorder(
                    new EtchedBorder(),
                    title,
                    TitledBorder.LEFT,
                    TitledBorder.TOP));
            setLayout(new BorderLayout());
            setMinimumSize(new Dimension(200, height));
            setPreferredSize(new Dimension(width, height));
        }
    }
}
