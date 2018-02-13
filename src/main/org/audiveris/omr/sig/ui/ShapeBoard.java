//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S h a p e B o a r d                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.OmrGlassPane;
import org.audiveris.omr.ui.dnd.AbstractGhostDropListener;
import org.audiveris.omr.ui.dnd.GhostDropAdapter;
import org.audiveris.omr.ui.dnd.GhostDropEvent;
import org.audiveris.omr.ui.dnd.GhostDropListener;
import org.audiveris.omr.ui.dnd.GhostGlassPane;
import org.audiveris.omr.ui.dnd.GhostMotionAdapter;
import org.audiveris.omr.ui.dnd.ScreenPoint;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.view.RubberPanel;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.ui.view.Zoom;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;

/**
 * Class {@code ShapeBoard} hosts a palette of shapes for insertion and assignment of
 * inter.
 * <p>
 * Shapes are gathered and presented in separate sets that are mutually exclusive.
 * <p>
 * A special set of shapes, always visible, is dedicated to the latest shapes used to ease the
 * repetition of user actions.
 * <ul>
 * <li>Direct insertion is performed by drag and drop to the target score view or sheet view</li>
 * <li>Assignment of existing glyph is performed by a double-click</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class ShapeBoard
        extends Board
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ShapeBoard.class);

    /** To force the width of the various panels. */
    private static final int BOARD_WIDTH = 317;

    /**
     * To force the height of the various shape panels.
     * This is just a dirty hack, to force Swing FlowLayout to wrap its flow.
     * A better solution might be to use JGoodies Layout, when we have some time to migrate...
     */
    private static final Map<ShapeSet, Integer> heights = new HashMap<ShapeSet, Integer>();

    static {
        heights.put(ShapeSet.Accidentals, 40);
        heights.put(ShapeSet.Articulations, 40);
        heights.put(ShapeSet.Attributes, 60);
        heights.put(ShapeSet.Barlines, 140);
        heights.put(ShapeSet.BeamsAndTuplets, 60);
        heights.put(ShapeSet.Clefs, 140);
        heights.put(ShapeSet.Digits, 40);
        heights.put(ShapeSet.Dynamics, 70);
        heights.put(ShapeSet.Flags, 140);
        heights.put(ShapeSet.Keys, 180);
        heights.put(ShapeSet.Holds, 40);
        heights.put(ShapeSet.Markers, 40);
        heights.put(ShapeSet.HeadsAndDot, 60);
        heights.put(ShapeSet.Ornaments, 70);
        heights.put(ShapeSet.Physicals, 70);
        heights.put(ShapeSet.Pluckings, 40);
        heights.put(ShapeSet.Rests, 120);
        heights.put(ShapeSet.Romans, 60);
        heights.put(ShapeSet.Times, 120);
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** On-going DnD operation, if any. */
    private DndOperation dndOperation;

    /**
     * Called-back when a set is selected: the panel of shape sets is "replaced" by
     * the panel of shapes that compose the selected set.
     */
    private final ActionListener setListener = new ActionListener()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Hide panel of sets
            setsPanel.setVisible(false);

            // Show specific panel of shapes
            String setName = ((JButton) e.getSource()).getName();
            ShapeSet set = ShapeSet.getShapeSet(setName);
            shapesPanel = shapesPanels.get(set);
            shapesPanel.setVisible(true);

            resizeBoard();
        }
    };

    /**
     * Called-back when a panel of shapes is closed: the panel is replaced by the
     * panel of sets to allow the selection of another set.
     */
    private final ActionListener closeListener = new ActionListener()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Hide current panel of shapes
            shapesPanel.setVisible(false);

            // Show panel of sets
            setsPanel.setVisible(true);

            resizeBoard();
        }
    };

    /**
     * Called-back when a shape button is (double-) clicked.
     */
    private final MouseListener mouseListener = new MouseAdapter()
    {
        // Ability to use the button for direct assignment via double-click
        @Override
        public void mouseClicked (MouseEvent e)
        {
            if (e.getClickCount() == 2) {
                Glyph glyph = sheet.getGlyphIndex().getSelectedGlyph();

                if (glyph != null) {
                    ShapeButton button = (ShapeButton) e.getSource();

                    // Actually assign the shape
                    sheet.getInterController().addInter(glyph, button.shape);

                    // Update history
                    shapeHistory.add(button.shape);
                }
            }
        }
    };

    /** Panel of all shape sets. */
    private final Panel setsPanel;

    /** Map of shape panels. */
    private final Map<ShapeSet, Panel> shapesPanels = new HashMap<ShapeSet, Panel>();

    /** History of recently used shapes. */
    private final ShapeHistory shapeHistory;

    /** Current panel of shapes. */
    private Panel shapesPanel;

    /** GlassPane. */
    private final GhostGlassPane glassPane = OMR.gui.getGlassPane();

    /** Update image and forward mouse location. */
    private final MyMotionAdapter motionAdapter = new MyMotionAdapter();

    /** When symbol is dropped. */
    private final GhostDropListener<Shape> dropListener = new MyDropListener();

    /** When mouse is pressed (start) and released (stop). */
    private final MyDropAdapter dropAdapter = new MyDropAdapter();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new ShapeBoard object.
     *
     * @param sheet    the related sheet
     * @param selected true if initially selected
     */
    public ShapeBoard (Sheet sheet,
                       boolean selected)
    {
        super(Board.SHAPE, null, null, selected, false, false, false);
        this.sheet = sheet;

        dropAdapter.addDropListener(dropListener);
        shapeHistory = new ShapeHistory();
        setsPanel = buildSetsPanel();

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // onEvent //
    //---------//
    /**
     * Unused in this board.
     *
     * @param event unused
     */
    @Override
    public void onEvent (UserEvent event)
    {
        // Empty
    }

    //------------//
    // addButtons //
    //------------//
    private void addButtons (Panel panel,
                             List<Shape> shapes)
    {
        for (Shape shape : shapes) {
            ShapeButton button = new ShapeButton(shape);
            button.addMouseListener(mouseListener); // For double-click
            button.addMouseListener(dropAdapter); // For DnD transfer and double-click
            button.addMouseMotionListener(motionAdapter); // For dragging

            panel.add(button);
        }
    }

    //----------------//
    // buildSetsPanel //
    //----------------//
    /**
     * Build the global panel of sets.
     *
     * @return the global panel of sets
     */
    private Panel buildSetsPanel ()
    {
        Panel panel = new Panel();
        panel.setNoInsets();
        panel.setPreferredSize(new Dimension(BOARD_WIDTH, 160));

        FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.LEADING);
        panel.setLayout(layout);
        panel.setBackground(Color.LIGHT_GRAY);

        for (ShapeSet set : ShapeSet.getShapeSets()) {
            Shape rep = set.getRep();

            if (rep != null) {
                JButton button = new JButton();
                button.setIcon(rep.getDecoratedSymbol());
                button.setName(set.getName());
                button.addActionListener(setListener);
                button.setToolTipText(set.getName());
                button.setBorderPainted(false);
                panel.add(button);

                // Create the related shapesPanel
                shapesPanels.put(set, buildShapesPanel(set));
            }
        }

        return panel;
    }

    //------------------//
    // buildShapesPanel //
    //------------------//
    /**
     * Build the panel of shapes for a given set.
     *
     * @param set the given set of shapes
     * @return the panel of shapes for the provided set
     */
    private Panel buildShapesPanel (ShapeSet set)
    {
        Panel panel = new Panel();
        panel.setNoInsets();
        panel.setPreferredSize(new Dimension(BOARD_WIDTH, getSetHeight(set)));

        FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.LEADING);
        panel.setLayout(layout);

        // Button to close this shapes panel and return to sets panel
        JButton close = new JButton(set.getName());
        close.addActionListener(closeListener);
        close.setToolTipText("Back to shape sets");
        close.setBorderPainted(false);
        panel.add(close);

        // One button per shape
        addButtons(panel, set.getSortedShapes());

        return panel;
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        CellConstraints cst = new CellConstraints();
        FormLayout layout = new FormLayout(
                "190dlu",
                "pref," + Panel.getFieldInterline() + ",pref");
        PanelBuilder builder = new PanelBuilder(layout, getBody());

        builder.add(shapeHistory.panel, cst.xy(1, 1));
        builder.add(setsPanel, cst.xy(1, 3));

        for (Panel shapesPanel : shapesPanels.values()) {
            builder.add(shapesPanel, cst.xy(1, 3)); // All overlap setsPanel
            shapesPanel.setVisible(false);
        }
    }

    //--------------//
    // getIconImage //
    //--------------//
    /**
     * Get the image to draw as an icon for the provided shape.
     *
     * @param shape the provided shape
     * @return an image properly sized for an icon
     */
    private BufferedImage getIconImage (Shape shape)
    {
        ShapeSymbol symbol = (shape == Shape.BEAM_HOOK) ? shape.getPhysicalShape().getSymbol()
                : shape.getDecoratedSymbol();

        return symbol.getIconImage();
    }

    //----------------------//
    // getNonDraggableImage //
    //----------------------//
    private BufferedImage getNonDraggableImage (Zoom zoom)
    {
        int zoomedInterline = (int) Math.rint(zoom.getRatio() * sheet.getScale().getInterline());

        return MusicFont.buildImage(Shape.NON_DRAGGABLE, zoomedInterline, true); // Decorated
    }

    //--------------//
    // getSetHeight //
    //--------------//
    /**
     * Safe method to report the preferred panel height for the provided set.
     *
     * @param set provided set
     * @return preferred height (or a default value)
     */
    private int getSetHeight (ShapeSet set)
    {
        Integer height = heights.get(set);

        if (height == null) {
            logger.error("No panel height for set {}", set.getName());
            height = 100;
        }

        return height;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // ShapeButton //
    //-------------//
    /**
     * A button dedicated to a shape.
     */
    public static class ShapeButton
            extends JButton
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Shape shape;

        //~ Constructors ---------------------------------------------------------------------------
        public ShapeButton (Shape shape)
        {
            this.shape = shape;
            setIcon(shape.getDecoratedSymbol());
            setName(shape.toString());
            setToolTipText(shape.toString());

            setBorderPainted(true);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean publishLocationWhileDragging = new Constant.Boolean(
                false,
                "Should we publish the current location while dragging a shape?");

        private final Constant.Integer maxHistoryLength = new Constant.Integer(
                "shapes",
                8,
                "Maximum number of shapes kept in history");
    }

    //---------------//
    // MyDropAdapter //
    //---------------//
    /**
     * DnD adapter called when mouse is pressed and released.
     */
    private class MyDropAdapter
            extends GhostDropAdapter<Shape>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public MyDropAdapter ()
        {
            super(ShapeBoard.this.glassPane, null);
        }

        //~ Methods --------------------------------------------------------------------------------
        public Shape getAction ()
        {
            return action;
        }

        // Start of DnD. (set pay load?)
        @Override
        public void mousePressed (MouseEvent e)
        {
            // Reset the motion adapter
            motionAdapter.reset();

            ShapeButton button = (ShapeButton) e.getSource();
            Shape shape = button.shape;

            // Set shape & image
            if (shape.isDraggable()) {
                // Wait for drag to actually begin...
                action = shape;
                image = getIconImage(shape);
            } else {
                action = Shape.NON_DRAGGABLE;
                image = Shape.NON_DRAGGABLE.getSymbol().getIconImage();
                ((OmrGlassPane) glassPane).setReference(null);
            }

            super.mousePressed(e);
        }

        // End of DnD. Reset pay load
        @Override
        public void mouseReleased (MouseEvent e)
        {
            super.mouseReleased(e);

            OmrGlassPane glass = (OmrGlassPane) glassPane;
            glass.setReference(null);
            dndOperation = null;
        }
    }

    //----------------//
    // MyDropListener //
    //----------------//
    /**
     * Listener called when DnD shape is dropped.
     */
    private class MyDropListener
            extends AbstractGhostDropListener<Shape>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public MyDropListener ()
        {
            // Target will be any view of sheet assembly
            super(null);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void dropped (GhostDropEvent<Shape> e)
        {
            Shape shape = e.getAction();

            if (shape != Shape.NON_DRAGGABLE) {
                ScreenPoint screenPoint = e.getDropLocation();

                // The (zoomed) sheet view
                ScrollView scrollView = sheet.getStub().getAssembly().getSelectedView();

                if (screenPoint.isInComponent(scrollView.getComponent().getViewport())) {
                    RubberPanel view = scrollView.getView();
                    Point localPt = screenPoint.getLocalPoint(view);
                    view.getZoom().unscale(localPt);

                    if (dndOperation != null) {
                        dndOperation.drop(localPt);

                        // Update history
                        shapeHistory.add(dndOperation.getGhost().getShape());
                    }
                }
            }
        }
    }

    //-----------------//
    // MyMotionAdapter //
    //-----------------//
    /**
     * Adapter in charge of forwarding the current mouse location and
     * updating the dragged image according to the target under the mouse.
     */
    private class MyMotionAdapter
            extends GhostMotionAdapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        // Optimization: remember the latest component on target
        private WeakReference<Component> prevComponent;

        //~ Constructors ---------------------------------------------------------------------------
        public MyMotionAdapter ()
        {
            super(ShapeBoard.this.glassPane);
            reset();
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * In this specific implementation, we update the size of the
         * shape image according to the interline scale and to the
         * display zoom of the droppable target underneath.
         *
         * @param e the mouse event
         */
        @Override
        public void mouseDragged (MouseEvent e)
        {
            final ShapeButton button = (ShapeButton) e.getSource();
            final Shape shape = button.shape;
            final ScreenPoint screenPoint = new ScreenPoint(e.getXOnScreen(), e.getYOnScreen());
            final OmrGlassPane glass = (OmrGlassPane) glassPane;

            // The (zoomed) sheet view
            ScrollView scrollView = sheet.getStub().getAssembly().getSelectedView();
            Component component = scrollView.getComponent().getViewport();

            if (screenPoint.isInComponent(component)) {
                final RubberPanel view = scrollView.getView();
                final Zoom zoom = view.getZoom();
                final Point localPt = zoom.unscaled(screenPoint.getLocalPoint(view));
                glass.setOverTarget(true);

                // Moving into this component?
                if (component != prevComponent.get()) {
                    if (shape.isDraggable()) {
                        if (dndOperation == null) {
                            dndOperation = DndOperation.create(sheet, zoom, shape); // Set payload
                        }

                        dndOperation.enteringTarget();
                    } else {
                        glass.setImage(getNonDraggableImage(zoom));
                        glass.setReference(null);
                    }

                    prevComponent = new WeakReference<Component>(component);
                }

                if (shape.isDraggable()) {
                    // Update reference point
                    Point localRef = dndOperation.getReference(localPt);
                    glass.setReference(
                            (localRef != null) ? new ScreenPoint(view, zoom.scaled(localRef)) : null);
                }
            } else if (prevComponent.get() != null) {
                // No longer on a droppable target, reuse initial image & size
                glass.setOverTarget(false);
                glass.setImage(dropAdapter.getImage());
                glass.setReference(null);
                reset();
            }

            glass.setPoint(screenPoint); // This triggers a repaint of glassPane
        }

        public final void reset ()
        {
            prevComponent = new WeakReference<Component>(null);
        }
    }

    //--------------//
    // ShapeHistory //
    //--------------//
    private class ShapeHistory
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final List<Shape> shapes = new ArrayList<Shape>();

        private final Panel panel = new Panel();

        //~ Constructors ---------------------------------------------------------------------------
        public ShapeHistory ()
        {
            panel.setNoInsets();

            FlowLayout layout = new FlowLayout();
            layout.setAlignment(FlowLayout.LEADING);
            panel.setLayout(layout);
        }

        //~ Methods --------------------------------------------------------------------------------
        public void add (Shape shape)
        {
            // Remove duplicate if any
            shapes.remove(shape);

            // Insert at beginning of the list
            shapes.add(0, shape);

            // Check for maximum length
            while (shapes.size() > constants.maxHistoryLength.getValue()) {
                shapes.remove(shapes.size() - 1);
            }

            // Regenerate the buttons
            panel.removeAll();
            addButtons(panel, shapes);

            resizeBoard();
        }
    }
}
