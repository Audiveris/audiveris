//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S a m p l e V e r i f i e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier.ui;

import omr.classifier.NeuralClassifier;
import omr.classifier.Sample;
import omr.classifier.SampleRepository;
import omr.classifier.SampleRepository.AdditionEvent;
import static omr.classifier.SampleRepository.STANDARD_INTERLINE;
import omr.classifier.SheetContainer.Descriptor;
import omr.classifier.WekaClassifier;

import omr.glyph.Glyph;
import omr.glyph.GlyphsModel;
import omr.glyph.Shape;
import omr.glyph.ui.EvaluationBoard;
import omr.glyph.ui.GlyphsController;

import omr.run.RunTable;

import omr.ui.BoardsPane;
import omr.ui.OmrGui;
import omr.ui.selection.EntityListEvent;
import omr.ui.selection.EntityService;
import omr.ui.selection.MouseMovement;
import omr.ui.selection.SelectionHint;
import omr.ui.selection.SelectionService;
import omr.ui.util.FixedWidthIcon;
import omr.ui.util.Panel;
import omr.ui.util.UILookAndFeel;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import static javax.swing.JSplitPane.VERTICAL_SPLIT;
import javax.swing.ListCellRenderer;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import javax.swing.Scrollable;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Class {@code SampleVerifier} provides a user interface to browse through all samples
 * recorded for classifier training, in order to visually check the correctness of their
 * assigned shape, and to remove or reassign spurious samples when necessary.
 * <p>
 * The user can select sheets, shapes and samples:<ul>
 * <li> For the current sample, detailed characteristics are listed, while the top 5 shape
 * evaluations are computed on-line by the shape classifier.
 * <li> If the binary image of containing sheet is available, the sample is displayed with its
 * context.
 * <li> The user is able to remove the sample or to re-assign it to a different shape.
 * </ul>
 * This class can also be launched as a stand-alone program.
 *
 * @author Hervé Bitteur
 */
public class SampleVerifier
        extends SingleFrameApplication
        implements ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SampleVerifier.class);

    /** The unique class instance. */
    private static volatile SampleVerifier INSTANCE;

    /** Stand-alone run (vs part of Audiveris). */
    private static boolean standAlone = false;

    private static final Border SAMPLE_BORDER = BorderFactory.createEtchedBorder();

    private static final Color SYMBOL_BACKGROUND = new Color(255, 220, 220);

    private static final Color SAMPLE_BACKGROUND = new Color(220, 255, 220);

    private static final int SAMPLE_MARGIN = 10;

    private static final Point SAMPLE_OFFSET = new Point(SAMPLE_MARGIN, SAMPLE_MARGIN);

    /** Events that can be published on the local sample service. */
    private static final Class<?>[] eventsAllowed = new Class<?>[]{EntityListEvent.class};

    //~ Instance fields ----------------------------------------------------------------------------
    /** Repository of training samples. */
    private final SampleRepository repository = SampleRepository.getInstance();

    /** The dedicated frame. */
    private JFrame frame;

    /** Local service for sample events. */
    private final EntityService<Sample> sampleService;

    /** View of sample in sheet context. */
    private final SampleContext sampleContext = new SampleContext();

    /** Boards filled by the current sample. (sampleBoard + evaluationBoard) */
    private final BoardsPane boardsPane;

    /** Panel for samples display. */
    private final SampleListing sampleListing = new SampleListing(null);

    /** Panel for shapes selection. */
    private final ShapeSelector shapeSelector = new ShapeSelector(sampleListing);

    /** Panel for sheets selection. */
    private final SheetSelector sheetSelector = new SheetSelector(shapeSelector);

    /** Has repository been checked for duplications?. */
    private boolean dupliChecked;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an instance of {@code SampleVerifier}.
     */
    private SampleVerifier ()
    {
        sampleService = new EntityService<Sample>("sample service", null, eventsAllowed);
        sampleContext.connect(sampleService);

        SampleController controller = new SampleController(sampleService);
        boardsPane = new BoardsPane(
                new SampleBoard(controller),
                new EvaluationBoard(NeuralClassifier.getInstance(), controller, true),
                new EvaluationBoard(WekaClassifier.getInstance(), controller, true));

        // Stay informed of repository dynamic updates
        repository.addListener(this);

        if (!standAlone) {
            if (!repository.isLoaded()) {
                repository.loadRepository(true);
            }

            frame = defineLayout(new JFrame());
        } else {
            INSTANCE = this;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
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

    //------//
    // main //
    //------//
    /**
     * Just to allow stand-alone running of this class
     *
     * @param args not used
     */
    public static void main (String... args)
    {
        standAlone = true;

        // Load repository, with binaries
        SampleRepository.getInstance().loadRepository(true);

        // Set UI Look and Feel
        UILookAndFeel.setUI(null);
        Locale.setDefault(Locale.ENGLISH);

        // Off we go...
        Application.launch(SampleVerifier.class, args);
    }

    //------------//
    // setVisible //
    //------------//
    /**
     * Make the UI frame visible.
     */
    public void setVisible ()
    {
        OmrGui.getApplication().show(frame);

        if (!dupliChecked) {
            checkRepository();
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    @Override
    public void stateChanged (ChangeEvent event)
    {
        // Called by repository
        if (event instanceof AdditionEvent) {
            AdditionEvent addition = (AdditionEvent) event;
            verify(Arrays.asList(addition.sample));
        } else {
            sheetSelector.stateChanged(event);
        }
    }

    //--------//
    // verify //
    //--------//
    /**
     * Focus the verifier on a provided collection of samples.
     * <p>
     * (Typically these are the samples that are not correctly recognized by the classifier during
     * its training)
     *
     * @param samples the collection of samples to inspect
     */
    public void verify (List<Sample> samples)
    {
        // Sort samples by shape
        Collections.sort(samples, Sample.byShape);

        // Select proper Sheets
        Set<Descriptor> descSet = new HashSet<Descriptor>();

        for (Sample sample : samples) {
            descSet.add(repository.getSheetDescriptor(sample));
        }

        sheetSelector.select(descSet);

        // Select proper shapes
        EnumSet<Shape> shapeSet = EnumSet.noneOf(Shape.class);

        for (Sample sample : samples) {
            shapeSet.add(sample.getShape());
        }

        shapeSelector.select(shapeSet);

        // Samples
        sampleListing.populateWith(samples);
    }

    //------------//
    // initialize // Method called only when in stand-alone mode
    //------------//
    @Override
    protected void initialize (String[] args)
    {
        logger.debug("SampleVerifier. 1/initialize");
    }

    //-------//
    // ready // Method called only when in stand-alone mode
    //-------//
    @Override
    protected void ready ()
    {
        logger.debug("SampleVerifier. 3/ready");

        frame.addWindowListener(
                new WindowAdapter()
        {
            @Override
            public void windowClosing (WindowEvent e)
            {
                System.exit(0); // That's all folks !
            }
        });

        // Set application exit listener
        addExitListener(repository.getExitListener());

        if (!dupliChecked) {
            checkRepository();
        }
    }

    //---------//
    // startup // Method called only when in stand-alone mode
    //---------//
    @Override
    protected void startup ()
    {
        logger.debug("SampleVerifier. 2/startup");

        frame = defineLayout(getMainFrame());

        show(frame); // Here we go...
    }

    //-----------------//
    // checkRepository //
    //-----------------//
    private void checkRepository ()
    {
        dupliChecked = true;

        // Look for duplications
        List<Sample> toPurge = repository.checkAllSamples();

        if (!toPurge.isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(
                    frame,
                    "Purge repository of " + toPurge.size() + " duplications?",
                    "Duplications found in sample repository",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (answer == JOptionPane.YES_OPTION) {
                for (Sample sample : toPurge) {
                    repository.removeSample(sample);
                }
            } else if (answer != JOptionPane.NO_OPTION) {
                dupliChecked = false; // The user made no decision yet
            }
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout of components within the provided frame.
     *
     * @param frame the bare frame
     * @return the populated frame
     *
     */
    private JFrame defineLayout (JFrame frame)
    {
        frame.setName("SampleVerifierFrame"); // For SAF life cycle

        /*
         * |- left --||-- center ---|--------------- right ---------------|
         *
         * +=========++=============+=====================================+
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . sheet . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . sample. . . . . . . . . . . . . |
         * | . . . . || . selector. | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . population. . . . . . . . . . . |
         * | . . . . ||=============| . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . sample. . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . |=====================================|
         * | . . . . || . board . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | shape . ||-------------| . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | selector|| . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . eval. . . | . . . . sample. . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . board . . | . . . . context . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * | . . . . || . . . . . . | . . . . . . . . . . . . . . . . . . |
         * +=========++=============+=====================================+
         */
        // Left = shapeSelector
        shapeSelector.setName("shapeSelector");

        // Center
        JSplitPane centerPane = new JSplitPane(
                VERTICAL_SPLIT,
                sheetSelector,
                boardsPane.getComponent());
        centerPane.setBorder(null);
        centerPane.setOneTouchExpandable(true);
        centerPane.setName("centerPane");

        // Right
        JSplitPane rightPane = new JSplitPane(
                VERTICAL_SPLIT,
                sampleListing,
                sampleContext.getComponent());
        rightPane.setBorder(null);
        rightPane.setOneTouchExpandable(true);
        rightPane.setName("rightPane");

        // Center + Right
        JSplitPane centerPlusRightPane = new JSplitPane(HORIZONTAL_SPLIT, centerPane, rightPane);
        centerPlusRightPane.setBorder(null);
        centerPlusRightPane.setOneTouchExpandable(true);
        centerPlusRightPane.setName("centerPlusRightPane");

        // Global
        JSplitPane mainPane = new JSplitPane(HORIZONTAL_SPLIT, shapeSelector, centerPlusRightPane);
        mainPane.setBorder(null);
        mainPane.setOneTouchExpandable(true);
        mainPane.setResizeWeight(0d); // Give all free space to center+right part
        mainPane.setName("mainPane");

        frame.add(mainPane);

        // Resource injection
        ResourceMap resource = OmrGui.getApplication().getContext().getResourceMap(getClass());
        resource.injectComponents(frame);

        // Wiring
        boardsPane.connect();

        // Initialization
        sheetSelector.stateChanged(null);

        return frame;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------------//
    // SampleController //
    //------------------//
    /**
     * A very basic sample controller, with no location service.
     */
    public class SampleController
            extends GlyphsController
    {
        //~ Constructors ---------------------------------------------------------------------------

        public SampleController (EntityService<Sample> sampleService)
        {
            super(new SampleModel(sampleService));
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public SelectionService getLocationService ()
        {
            return null;
        }

        public void removeSample (Sample sample)
        {
            // Modify repository
            ((SampleModel) model).removeSample(sample);

            // Update UI
            sampleListing.stateChanged(null);
        }
    }

    //---------------//
    // SampleListing //
    //---------------//
    /**
     * Display a list of samples, gathered by shape.
     */
    private class SampleListing
            extends JScrollPane
            implements ChangeListener
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final String title = "Samples";

        private final ScrollablePanel scrollablePanel = new ScrollablePanel();

        /** Selection listener to avoid multi-selections across all lists in the panel. */
        private final ListSelectionListener selectionListener = new ListSelectionListener()
        {
            @Override
            public void valueChanged (ListSelectionEvent e)
            {
                JList<Sample> selectedList = (JList<Sample>) e.getSource();

                if (e.getValueIsAdjusting()) {
                    // Nullify selection in other lists
                    for (Component comp : scrollablePanel.getComponents()) {
                        ShapePane shapePane = (ShapePane) comp;
                        JList<Sample> list = shapePane.list;

                        if (list != selectedList) {
                            list.clearSelection();
                        }
                    }
                } else {
                    // Publish selected sample
                    final Sample sample = selectedList.getSelectedValue();

                    ///logger.info("valueChanged sample:{}", sample);
                    if (sample != null) {
                        sampleService.publish(
                                new EntityListEvent<Sample>(
                                        this,
                                        SelectionHint.ENTITY_INIT,
                                        MouseMovement.PRESSING,
                                        Arrays.asList(sample)));
                    }
                }
            }
        };

        private boolean selfUpdating; // To avoid circular updating

        //~ Constructors ---------------------------------------------------------------------------
        public SampleListing (ChangeListener listener)
        {
            setBorder(
                    BorderFactory.createTitledBorder(
                            new EmptyBorder(20, 5, 0, 0), // TLBR
                            title,
                            TitledBorder.LEFT,
                            TitledBorder.TOP));

            scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));

            setViewportView(scrollablePanel);
            setPreferredSize(new Dimension(800, 500));
            setAlignmentX(LEFT_ALIGNMENT);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void stateChanged (ChangeEvent e)
        {
            if (selfUpdating) {
                return;
            }

            // Gather all samples of selected shapes in selected sheets
            final List<Sample> allSamples = new ArrayList<Sample>();
            final List<Descriptor> descriptors = sheetSelector.list.getSelectedValuesList();
            final JList<Shape> shapeList = shapeSelector.list;
            final DefaultListModel<Shape> model = (DefaultListModel<Shape>) shapeList.getModel();

            for (Shape shape : shapeList.getSelectedValuesList()) {
                final ArrayList<Sample> shapeSamples = new ArrayList<Sample>();

                for (Descriptor desc : descriptors) {
                    shapeSamples.addAll(repository.getSamples(desc.id, shape));
                }

                if (shapeSamples.isEmpty()) {
                    // Remove this shape from shapeSelector
                    // SampleListing listens to ShapeSelector, so stateChanged() will be recalled
                    selfUpdating = true;
                    model.remove(model.indexOf(shape));
                    selfUpdating = false;
                }

                allSamples.addAll(shapeSamples);
            }

            populateWith(allSamples);
        }

        private void populateWith (List<Sample> samples)
        {
            // NB: samples are assumed to be ordered by shape
            scrollablePanel.removeAll();
            sampleService.publish(
                    new EntityListEvent<Sample>(
                            this,
                            SelectionHint.ENTITY_INIT,
                            MouseMovement.PRESSING,
                            Arrays.asList((Sample) null)));

            Shape currentShape = null;
            List<Sample> shapeSamples = new ArrayList<Sample>();

            for (Sample sample : samples) {
                final Shape shape = sample.getShape();

                // End of shape collection?
                if ((currentShape != null) && (currentShape != shape)) {
                    scrollablePanel.add(
                            new ShapePane(currentShape, shapeSamples, selectionListener));
                    shapeSamples.clear();
                }

                currentShape = shape;
                shapeSamples.add(sample);
            }

            // Last shape
            if ((currentShape != null) && !shapeSamples.isEmpty()) {
                scrollablePanel.add(new ShapePane(currentShape, shapeSamples, selectionListener));
            }

            TitledBorder border = (TitledBorder) getBorder();
            int sampleCount = samples.size();
            border.setTitle(title + ((sampleCount > 0) ? (": " + sampleCount) : ""));
            validate();

            // Pre-select the very first sample of the very first shape pane
            if (!samples.isEmpty()) {
                ShapePane shapePane = (ShapePane) scrollablePanel.getComponent(0);
                shapePane.list.setSelectedIndex(0);
            }
        }
    }

    //-------------//
    // SampleModel //
    //-------------//
    /**
     * A very basic samples model, used to handle the deletion of samples.
     */
    private class SampleModel
            extends GlyphsModel
    {
        //~ Constructors ---------------------------------------------------------------------------

        public SampleModel (EntityService<Sample> sampleService)
        {
            super(null, sampleService, null);
        }

        //~ Methods --------------------------------------------------------------------------------
        // Certainly not called ...
        @Override
        public void deassignGlyph (Glyph sample)
        {
            logger.error("Implement deassignGlyph");
        }

        public void removeSample (Sample sample)
        {
            repository.removeSample(sample);
        }
    }

    //----------------//
    // SampleRenderer //
    //----------------//
    /**
     * Render a sample cell within a ShapePane in ShapeListing.
     */
    private static class SampleRenderer
            extends JPanel
            implements ListCellRenderer<Sample>
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The sample being rendered. */
        private Sample sample;

        //~ Constructors ---------------------------------------------------------------------------
        public SampleRenderer (Dimension maxDimension)
        {
            setOpaque(true);
            setPreferredSize(
                    new Dimension(
                            maxDimension.width + (2 * SAMPLE_MARGIN),
                            maxDimension.height + (2 * SAMPLE_MARGIN)));
            setBorder(SAMPLE_BORDER);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public Component getListCellRendererComponent (JList<? extends Sample> list,
                                                       Sample sample,
                                                       int index,
                                                       boolean isSelected,
                                                       boolean cellHasFocus)
        {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
            } else {
                setBackground(sample.isSymbol() ? SYMBOL_BACKGROUND : SAMPLE_BACKGROUND);
            }

            this.sample = sample;

            return this;
        }

        @Override
        protected void paintComponent (Graphics g)
        {
            super.paintComponent(g); // Paint background

            RunTable table = sample.getRunTable();
            g.translate(SAMPLE_OFFSET.x, SAMPLE_OFFSET.y);

            // Draw the (properly scaled) run table over a white rectangle of same bounds
            final double ratio = (double) STANDARD_INTERLINE / sample.getInterline();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.scale(ratio, ratio);

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, table.getWidth(), table.getHeight());

            g2.setColor(Color.BLACK);
            table.render(g2, new Point(0, 0));

            g2.dispose();

            g.translate(-SAMPLE_OFFSET.x, -SAMPLE_OFFSET.y);
        }
    }

    //-----------------//
    // ScrollablePanel //
    //-----------------//
    private static class ScrollablePanel
            extends JPanel
            implements Scrollable
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public Dimension getPreferredScrollableViewportSize ()
        {
            return getPreferredSize();
        }

        /**
         * Returns the distance to scroll to expose the next or previous block.
         * <p>
         * For JList:
         * <ul>
         * <li>if scrolling down, returns the distance to scroll so that the last
         * visible element becomes the first completely visible element
         * <li>if scrolling up, returns the distance to scroll so that the first
         * visible element becomes the last completely visible element
         * <li>returns {@code visibleRect.height} if the list is empty
         * </ul>
         * <p>
         * For us:
         * <p>
         * "Element" could be the next/previous shape listPane?
         *
         * @param visibleRect the view area visible within the viewport
         * @param orientation {@code SwingConstants.HORIZONTAL} or {@code SwingConstants.VERTICAL}
         * @param direction   less or equal to zero to scroll up, greater than zero for down
         * @return the "block" increment for scrolling in the specified direction; always positive
         */
        @Override
        public int getScrollableBlockIncrement (Rectangle visibleRect,
                                                int orientation,
                                                int direction)
        {
            return visibleRect.height; // The whole window height. TODO: Could be improved.
        }

        @Override
        public boolean getScrollableTracksViewportHeight ()
        {
            return false;
        }

        @Override
        public boolean getScrollableTracksViewportWidth ()
        {
            return true;
        }

        /**
         * Returns the distance to scroll to expose the next or previous row.
         * <p>
         * JList
         *
         * @param visibleRect the view area visible within the viewport
         * @param orientation {@code SwingConstants.HORIZONTAL} or {@code SwingConstants.VERTICAL}
         * @param direction   less or equal to zero to scroll up, greater than zero for down
         * @return the "unit" increment for scrolling in the specified direction; always positive
         */
        @Override
        public int getScrollableUnitIncrement (Rectangle visibleRect,
                                               int orientation,
                                               int direction)
        {
            return 20; // Minimum cell height. TODO: Could be improved.
        }
    }

    //----------//
    // Selector //
    //----------//
    /**
     * Class {@code Selector} is the basis for sheet and shape selectors.
     * Each selector is made of a list of names, which can be selected and deselected at will.
     */
    private abstract static class Selector<E>
            extends TitledPanel
            implements ChangeListener
    {
        //~ Instance fields ------------------------------------------------------------------------

        // The title base for this selector
        private final String title;

        // Other entity interested in items selected by this selector
        private ChangeListener listener;

        // Buttons
        protected JButton selectAll = new JButton("Select All");

        protected JButton cancelAll = new JButton("Cancel All");

        // Underlying list model
        protected final DefaultListModel<E> model = new DefaultListModel<E>();

        // Underlying list
        protected JList<E> list = new JList<E>(model);

        // ScrollPane around the list
        protected JScrollPane scrollPane = new JScrollPane(list);

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create a selector.
         *
         * @param title    label for this selector
         * @param listener potential (external) listener for changes, if any
         */
        public Selector (String title,
                         ChangeListener listener)
        {
            super(title);
            this.title = title;
            this.listener = listener;

            setLayout(new BorderLayout());
            setMinimumSize(new Dimension(0, 200));
            setPreferredSize(new Dimension(180, 200));

            // To be informed of mouse (de)selections (not programmatic)
            list.addListSelectionListener(
                    new ListSelectionListener()
            {
                @Override
                public void valueChanged (ListSelectionEvent e)
                {
                    update(); // Brute force !!!
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
                    list.clearSelection();
                    update();
                }
            });

            JPanel buttons = new JPanel(new GridLayout(1, 2));

            buttons.add(selectAll);
            buttons.add(cancelAll);

            // All buttons are initially disabled
            selectAll.setEnabled(false);
            cancelAll.setEnabled(false);

            add(scrollPane, BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);
        }

        //~ Methods --------------------------------------------------------------------------------
        public void populateWith (Collection<E> items)
        {
            model.removeAllElements();

            for (E item : items) {
                model.addElement(item);
            }

            update();
        }

        public void select (Collection<E> items)
        {
            list.clearSelection();

            for (int i = 0, iBreak = model.size(); i < iBreak; i++) {
                E item = model.get(i);

                if (items.contains(item)) {
                    list.addSelectionInterval(i, i);
                }
            }
        }

        public void selectAll ()
        {
            list.setSelectionInterval(0, model.size() - 1);
            update();
        }

        protected void update ()
        {
            final int selectionCount = list.getSelectedIndices().length;

            // Title
            final TitledBorder border = (TitledBorder) getBorder();
            border.setTitle(title + ((selectionCount > 0) ? (": " + selectionCount) : ""));
            repaint();

            // Buttons
            selectAll.setEnabled(model.size() > 0);
            cancelAll.setEnabled(selectionCount > 0);

            // Notify listener if any
            if (listener != null) {
                listener.stateChanged(null);
            }
        }
    }

    //-----------//
    // ShapePane //
    //-----------//
    /**
     * Handles the display of a list of samples assigned to the same shape.
     */
    private static class ShapePane
            extends TitledPanel
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Underlying list of all samples for the shape. */
        private final JList<Sample> list;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Build a ShapePane instance for the provided shape.
         *
         * @param shape             provided shape
         * @param samples           all samples (within selected sheets) for that shape
         * @param selectionListener listener on user selection
         */
        public ShapePane (Shape shape,
                          List<Sample> samples,
                          ListSelectionListener selectionListener)
        {
            super(shape + " (" + samples.size() + ")");
            setLayout(new BorderLayout());

            list = new JList(samples.toArray(new Sample[samples.size()]));
            list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
            list.setVisibleRowCount(0);
            list.setSelectionMode(SINGLE_SELECTION);
            list.addListSelectionListener(selectionListener);

            // One renderer for all samples of same shape
            list.setCellRenderer(new SampleRenderer(maxDimensionOf(samples)));

            add(list, BorderLayout.CENTER);
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Determine the maximum dimension to accommodate all samples for this shape,
         * once they are scaled to the standard interline value.
         *
         * @param samples the population of samples (same shape)
         * @return the largest dimension observed
         */
        private Dimension maxDimensionOf (List<Sample> samples)
        {
            double w = 0;
            double h = 0;

            for (Sample sample : samples) {
                final double ratio = (double) STANDARD_INTERLINE / sample.getInterline();
                w = Math.max(w, ratio * sample.getWidth());
                h = Math.max(h, ratio * sample.getHeight());
            }

            return new Dimension((int) Math.ceil(w), (int) Math.ceil(h));
        }
    }

    //---------------//
    // ShapeRenderer //
    //---------------//
    /**
     * Render a shape item within the ShapeSelector list.
     */
    private class ShapeRenderer
            extends JLabel
            implements ListCellRenderer<Shape>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public ShapeRenderer ()
        {
            setOpaque(true);
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Find the image and text corresponding to the provided shape and returns the
         * label, ready to be displayed.
         */
        @Override
        public Component getListCellRendererComponent (JList<? extends Shape> list,
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
            setIcon(new FixedWidthIcon(shape.getDecoratedSymbol()));

            return this;
        }
    }

    //---------------//
    // ShapeSelector //
    //---------------//
    /**
     * Display a list of available shapes (within selected sheets) and let user make a
     * selection.
     */
    private class ShapeSelector
            extends Selector<Shape>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public ShapeSelector (ChangeListener listener)
        {
            super("Shapes", listener);
            setMinimumSize(new Dimension(100, 0));

            list.setCellRenderer(new ShapeRenderer());
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void stateChanged (ChangeEvent e)
        {
            // Populate with shape names found in selected folders
            final EnumSet<Shape> shapeSet = EnumSet.noneOf(Shape.class);

            for (Descriptor desc : sheetSelector.list.getSelectedValuesList()) {
                shapeSet.addAll(repository.getShapes(desc));
            }

            populateWith(shapeSet);
        }
    }

    //---------------//
    // SheetSelector //
    //---------------//
    /**
     * Display a list of available sheets and let user make a selection.
     */
    private class SheetSelector
            extends Selector<Descriptor>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public SheetSelector (ChangeListener listener)
        {
            super("Sheets", listener);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void stateChanged (ChangeEvent e)
        {
            populateWith(repository.getAllDescriptors());
        }
    }

    //-------------//
    // TitledPanel //
    //-------------//
    /**
     * A panel surrounded by an EmptyBorder and a title.
     */
    private static class TitledPanel
            extends Panel
    {
        //~ Constructors ---------------------------------------------------------------------------

        public TitledPanel (String title)
        {
            setBorder(
                    BorderFactory.createTitledBorder(
                            new EmptyBorder(20, 5, 0, 0), // TLBR
                            title,
                            TitledBorder.LEFT,
                            TitledBorder.TOP));
            this.setInsets(25, 5, 0, 0);
        }
    }
}
