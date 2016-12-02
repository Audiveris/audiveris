//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S a m p l e L i s t i n g                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.classifier.ui;

import omr.classifier.Sample;
import omr.classifier.SampleRepository;
import static omr.classifier.SampleRepository.STANDARD_INTERLINE;
import omr.classifier.SampleSheet;
import omr.classifier.SheetContainer;

import omr.glyph.Shape;

import omr.run.RunTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;

/**
 * Class {@code SampleListing} is a private companion of {@link SampleBrowser},
 * it is in charge of a list of samples, gathered by shape.
 * It is implemented as a list of ShapePane instances, one per shape, each ShapePane instance
 * handling a list of samples (all of the same shape).
 *
 * @author Hervé Bitteur
 */
class SampleListing
        extends JScrollPane
        implements ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SampleListing.class);

    private static final Border SAMPLE_BORDER = BorderFactory.createEtchedBorder();

    private static final Color SYMBOL_BACKGROUND = new Color(255, 220, 220);

    private static final Color SAMPLE_BACKGROUND = new Color(220, 255, 220);

    private static final int SAMPLE_MARGIN = 10;

    private static final Point SAMPLE_OFFSET = new Point(SAMPLE_MARGIN, SAMPLE_MARGIN);

    //~ Instance fields ----------------------------------------------------------------------------
    private final String title = "Samples";

    private final ScrollablePanel scrollablePanel = new ScrollablePanel();

    /** Underlying sample repository. */
    private final SampleRepository repository;

    /** SampleBrowser instance. */
    private final SampleBrowser browser;

    /** Listener on all shape lists. */
    private final ListMouseListener listener = new ListMouseListener();

    /** Sample popup. */
    private final SamplePopup popup;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SampleListing} object.
     */
    public SampleListing (SampleBrowser browser,
                          SampleRepository repository)
    {
        this.browser = browser;
        this.repository = repository;

        popup = new SamplePopup();
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

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void stateChanged (ChangeEvent e)
    {
        // Called from shapeSelector: Gather all samples of selected shapes in selected sheets
        final List<Sample> allSamples = new ArrayList<Sample>();
        final List<SheetContainer.Descriptor> descriptors = browser.getSelectedSheets();

        for (Shape shape : browser.getSelectedShapes()) {
            final ArrayList<Sample> shapeSamples = new ArrayList<Sample>();

            for (SheetContainer.Descriptor desc : descriptors) {
                shapeSamples.addAll(repository.getSamples(desc.getName(), shape));
            }

            if (!shapeSamples.isEmpty()) {
                allSamples.addAll(shapeSamples);
            }
        }

        populateWith(allSamples);
    }

    /**
     * Add a sample to the listing, only if there is already a ShapePane for the shape.
     *
     * @param sample the sample to potentially add
     */
    void addSample (Sample sample)
    {
        for (Component comp : scrollablePanel.getComponents()) {
            ShapePane shapePane = (ShapePane) comp;

            if (shapePane.getShape() == sample.getShape()) {
                shapePane.model.addElement(sample);

                return;
            }
        }
    }

    /**
     * Empty and regenerate the whole content of SampleListing.
     *
     * @param samples the whole sequence of samples to display (assumed to be ordered by shape)
     */
    void populateWith (List<Sample> samples)
    {
        scrollablePanel.removeAll(); // Remove all ShapePane instances

        browser.publishSample(null); // Deselect any sample

        // Rebuild ShapePane instances as needed
        Shape currentShape = null;
        List<Sample> shapeSamples = new ArrayList<Sample>();

        for (Sample sample : samples) {
            final Shape shape = sample.getShape();

            // End of a shape collection?
            if ((currentShape != null) && (currentShape != shape)) {
                scrollablePanel.add(new ShapePane(currentShape, shapeSamples));
                shapeSamples.clear();
            }

            currentShape = shape;
            shapeSamples.add(sample);
        }

        // Last shape
        if ((currentShape != null) && !shapeSamples.isEmpty()) {
            scrollablePanel.add(new ShapePane(currentShape, shapeSamples));
        }

        TitledBorder border = (TitledBorder) getBorder();
        int sampleCount = samples.size();
        border.setTitle(title + ((sampleCount > 0) ? (": " + sampleCount) : ""));
        validate();
        repaint();

        // Pre-select the very first sample of the very first ShapePane
        if (!samples.isEmpty()) {
            ShapePane shapePane = (ShapePane) scrollablePanel.getComponent(0);
            shapePane.list.setSelectedIndex(0);
        }
    }

    /**
     * Remove a sample (if contained by a ShapePane).
     *
     * @param sample the sample to potentially remove
     */
    void removeSample (Sample sample)
    {
        for (Component comp : scrollablePanel.getComponents()) {
            ShapePane shapePane = (ShapePane) comp;

            if (shapePane.getShape() == sample.getShape()) {
                DefaultListModel model = shapePane.model;
                int idx = model.indexOf(sample);

                if (idx != -1) {
                    model.removeElementAt(idx);

                    if (model.isEmpty()) {
                        scrollablePanel.remove(shapePane);
                        browser.publishSample(null); // Deselect any sample
                    } else if (idx <= (model.getSize() - 1)) {
                        // Move selection to next item in shapePane
                        shapePane.list.setSelectedIndex(idx);
                    } else {
                        // Move selection to previous item in shapePane
                        shapePane.list.setSelectedIndex(idx - 1);
                    }
                }

                return;
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------------//
    // ListMouseListener //
    //-------------------//
    /** Listener to avoid selection across lists and to detect Alt key pressed. */
    private class ListMouseListener
            extends MouseInputAdapter
            implements ListSelectionListener
    {
        //~ Instance fields ------------------------------------------------------------------------

        boolean alt; // True if Alt key is pressed down

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void mousePressed (MouseEvent e)
        {
            //            {
            //                JList<Sample> selectedList = (JList<Sample>) e.getSource();
            //                Point point = e.getPoint();
            //                int index = selectedList.locationToIndex(point);
            //                logger.info("index:{}", index);
            //            }
            //
            alt = e.isAltDown();

            if (alt) {
                JList<Sample> selectedList = (JList<Sample>) e.getSource();
                Sample sample = selectedList.getSelectedValue();
                checkAlternative(sample); // Look for good alternative
            }
        }

        @Override
        public void mouseReleased (MouseEvent e)
        {
            if (alt) {
                JList<Sample> selectedList = (JList<Sample>) e.getSource();
                Sample sample = selectedList.getSelectedValue();
                browser.publishSample(sample);
            }

            alt = false;
        }

        @Override
        public void valueChanged (ListSelectionEvent e)
        {
            JList<Sample> selectedList = (JList<Sample>) e.getSource();
            Sample sample = selectedList.getSelectedValue();

            if (e.getValueIsAdjusting()) {
                // Nullify selection in other lists
                for (Component comp : scrollablePanel.getComponents()) {
                    ShapePane shapePane = (ShapePane) comp;
                    JList<Sample> list = shapePane.list;

                    if (list != selectedList) {
                        list.clearSelection();
                    }
                }

                if (alt) {
                    checkAlternative(sample); // Look for good alternative
                }
            } else if (sample != null) {
                browser.publishSample(sample);
            }
        }

        /**
         * Try to find out and publish a "good alternative" to this (bad?) sample.
         *
         * @param sample the sample to check for related good sample
         */
        private void checkAlternative (Sample sample)
        {
            List<Sample> alternatives = new ArrayList<Sample>();

            if (sample.getShape() == Shape.CLUTTER) {
                final Rectangle box = sample.getBounds();
                final SampleSheet sampleSheet = repository.getSampleSheet(sample);

                for (Shape shape : sampleSheet.getShapes()) {
                    if (shape != Shape.CLUTTER) {
                        for (Sample alternative : sampleSheet.getSamples(shape)) {
                            Rectangle common = alternative.getBounds().intersection(box);

                            if (!common.isEmpty()
                                && (common.width >= (box.width / 2))
                                && (common.height >= (box.height / 2))) {
                                logger.debug("alternative: {}", alternative);
                                alternatives.add(alternative);
                            }
                        }
                    }
                }
            }

            // Pick up the most suitable alternative, using best weight similarity
            if (!alternatives.isEmpty()) {
                final int sampleWeight = sample.getWeight();
                Sample bestAlternative = null;
                Integer bestDiff = null;

                for (Sample alternative : alternatives) {
                    int diff = Math.abs(alternative.getWeight() - sampleWeight);

                    if ((bestDiff == null) || (diff < bestDiff)) {
                        bestDiff = diff;
                        bestAlternative = alternative;
                    }
                }

                browser.publishSample(bestAlternative);
            }
        }
    }

    //-------------//
    // SamplePopup //
    //-------------//
    /**
     * Popup menu to play with sample at hand.
     */
    private class SamplePopup
            extends JPopupMenu
    {
        //~ Constructors ---------------------------------------------------------------------------

        public SamplePopup ()
        {
            super("SamplePopup");
            add(new JMenuItem(browser.getSampleController().getRemoveAction()));
            add(browser.getSampleController().getAssignAction().menu);
        }
    }

    //----------------//
    // SampleRenderer //
    //----------------//
    /**
     * Render a sample cell within a ShapePane.
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
         * "Element" could be the next/previous shapePane?
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
            if (orientation == SwingConstants.HORIZONTAL) {
                return visibleRect.width;
            } else {
                return visibleRect.height;
            }
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
            return 40; // Minimum cell height. TODO: Could be improved.
        }
    }

    //-----------//
    // ShapePane //
    //-----------//
    /**
     * Handles the display of a list of samples assigned to the same shape.
     */
    private class ShapePane
            extends SampleBrowser.TitledPanel
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Shape shape;

        private final DefaultListModel<Sample> model = new DefaultListModel<Sample>();

        /** Underlying list of all samples for the shape. */
        private final JList<Sample> list = new JList<Sample>(model);

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Build a ShapePane instance for the provided shape.
         *
         * @param shape   provided shape
         * @param samples all samples (within selected sheets) for that shape
         */
        public ShapePane (Shape shape,
                          List<Sample> samples)
        {
            super(shape + " (" + samples.size() + ")");
            this.shape = shape;
            setLayout(new BorderLayout());

            for (Sample sample : samples) {
                model.addElement(sample);
            }

            list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
            list.setVisibleRowCount(0);
            list.setSelectionMode(SINGLE_SELECTION);
            list.addMouseListener(listener);
            list.addListSelectionListener(listener);
            list.setName("ShapePane.JList-" + shape);
            list.setComponentPopupMenu(popup);

            // One renderer for all samples of same shape
            list.setCellRenderer(new SampleRenderer(maxDimensionOf(samples)));

            // Specific left/right keys to go through the whole list (and not only the current row)
            list.addKeyListener(
                    new KeyAdapter()
            {
                @Override
                public void keyPressed (KeyEvent ke)
                {
                    final int size = list.getModel().getSize();
                    final int index = list.getSelectedIndex();

                    if ((ke.getKeyCode() == KeyEvent.VK_LEFT) && (index > 0)) {
                        ke.consume();
                        list.setSelectedIndex(index - 1);
                        list.ensureIndexIsVisible(index - 1);
                    }

                    if ((ke.getKeyCode() == KeyEvent.VK_RIGHT) && (index < (size - 1))) {
                        ke.consume();
                        list.setSelectedIndex(index + 1);
                        list.ensureIndexIsVisible(index + 1);
                    }
                }
            });

            add(list, BorderLayout.CENTER);
        }

        //~ Methods --------------------------------------------------------------------------------
        public Shape getShape ()
        {
            return shape;
        }

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
}
