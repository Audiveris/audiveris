//----------------------------------------------------------------------------//
//                                                                            //
//                            S h a p e B o a r d                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.script.InsertTask;

import omr.selection.MouseMovement;
import omr.selection.UserEvent;

import omr.sheet.Sheet;

import omr.ui.Board;
import omr.ui.dnd.AbstractGhostDropListener;
import omr.ui.dnd.GhostDropAdapter;
import omr.ui.dnd.GhostDropEvent;
import omr.ui.dnd.GhostDropListener;
import omr.ui.dnd.GhostGlassPane;
import omr.ui.dnd.GhostMotionAdapter;
import omr.ui.dnd.ScreenPoint;
import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.util.Panel;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * Class {@code ShapeBoard} hosts a palette of shapes for insertion and
 * assignment of glyph.
 * <ul>
 * <li>Direct insertion is performed by drag and drop to the target score
 * view or sheet view</li>
 * <li>Assignment of existing glyph is performed by a double-click</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class ShapeBoard
        extends Board
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(ShapeBoard.class);

    /** To force the width of the various panels */
    private static final int BOARD_WIDTH = 280;

    /**
     * To force the height of the various shape panels (just a dirty hack)
     */
    private static final Map<ShapeSet, Integer> heights = new HashMap<>();

    static {
        heights.put(ShapeSet.Accidentals, 40);
        heights.put(ShapeSet.Articulations, 60);
        heights.put(ShapeSet.Attributes, 40);
        heights.put(ShapeSet.Barlines, 100);
        heights.put(ShapeSet.Beams, 60);
        heights.put(ShapeSet.Clefs, 140);
        heights.put(ShapeSet.Dynamics, 220);
        heights.put(ShapeSet.Flags, 130);
        heights.put(ShapeSet.Keys, 220);
        heights.put(ShapeSet.NoteHeads, 40);
        heights.put(ShapeSet.Markers, 120);
        heights.put(ShapeSet.Notes, 40);
        heights.put(ShapeSet.Ornaments, 80);
        heights.put(ShapeSet.Rests, 120);
        heights.put(ShapeSet.Times, 130);
        heights.put(ShapeSet.Physicals, 150);
    }

    //~ Instance fields --------------------------------------------------------
    /** Related sheet */
    private final Sheet sheet;

    /** The controller in charge of symbol assignments */
    private final SymbolsController symbolsController;

    /**
     * Method called when a range is selected: the panel of ranges is
     * replaced by the panel of shapes that compose the selected range.
     */
    private ActionListener rangeListener = new ActionListener()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Remove panel of ranges
            getBody()
                    .remove(rangesPanel);

            // Replace by proper panel of range shapes
            String rangeName = ((JButton) e.getSource()).getName();
            ShapeSet range = ShapeSet.getShapeSet(rangeName);
            shapesPanel = shapesPanels.get(range);

            if (shapesPanel == null) {
                // Lazily populate the map of shapesPanel instances
                shapesPanels.put(range, shapesPanel = defineShapesPanel(range));
            }

            getBody()
                    .add(shapesPanel);

            // Perhaps this is too much ... TODO
            JFrame frame = Main.getGui()
                    .getFrame();
            frame.invalidate();
            frame.validate();
            frame.repaint();
        }
    };

    /**
     * Method called when a panel of shapes is closed: the panel is
     * replaced by the panel of ranges to allow the selection of another
     * range.
     */
    private ActionListener closeListener = new ActionListener()
    {
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Remove current panel of shapes
            getBody()
                    .remove(shapesPanel);

            // Replace by panel of ranges
            getBody()
                    .add(rangesPanel);

            // Perhaps this is too much ... TODO
            JFrame frame = Main.getGui()
                    .getFrame();
            frame.invalidate();
            frame.validate();
            frame.repaint();
        }
    };

    /**
     * Method called when a shape button is clicked.
     */
    private MouseListener mouseListener = new MouseAdapter()
    {
        // Ability to use the button for direct assignment via double-click
        @Override
        public void mouseClicked (MouseEvent e)
        {
            if (e.getClickCount() == 2) {
                Glyph glyph = sheet.getNest()
                        .getSelectedGlyph();

                if (glyph != null) {
                    ShapeButton button = (ShapeButton) e.getSource();

                    // Actually assign the shape
                    symbolsController.asyncAssignGlyphs(
                            Glyphs.sortedSet(glyph),
                            button.shape,
                            false);
                }
            }
        }
    };

    /** Panel of all ranges */
    private final Panel rangesPanel;

    /** Map of shape panels */
    private final Map<ShapeSet, Panel> shapesPanels = new HashMap<>();

    /** Current panel of shapes */
    private Panel shapesPanel;

    /** GlassPane */
    private GhostGlassPane glassPane = Main.getGui()
            .getGlassPane();

    // Update image and forward mouse location
    private final MyMotionAdapter motionAdapter = new MyMotionAdapter(
            glassPane);

    // When symbol is dropped
    private final GhostDropListener<Shape> dropListener = new MyDropListener();

    // When mouse pressed (start) and released (stop)
    private final GhostDropAdapter<Shape> dropAdapter = new MyDropAdapter(
            glassPane);

    //~ Constructors -----------------------------------------------------------
    //------------//
    // ShapeBoard //
    //------------//
    /**
     * Create a new ShapeBoard object.
     *
     * @param sheet             the related sheet
     * @param symbolsController the UI controller for symbols
     * @param expanded          true if initially expanded
     */
    public ShapeBoard (Sheet sheet,
                       SymbolsController symbolsController,
                       boolean expanded)
    {
        super(Board.SHAPE, null, null, false, expanded);
        this.symbolsController = symbolsController;
        this.sheet = sheet;

        dropAdapter.addDropListener(dropListener);

        getBody()
                .add(rangesPanel = defineRangesPanel());
    }

    //~ Methods ----------------------------------------------------------------
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

    //-------------------//
    // defineRangesPanel //
    //-------------------//
    /**
     * Define the global panel of ranges.
     *
     * @return the global panel of ranges
     */
    private Panel defineRangesPanel ()
    {
        Panel panel = new Panel();
        panel.setNoInsets();
        panel.setPreferredSize(new Dimension(BOARD_WIDTH, 180));

        FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.LEADING);
        panel.setLayout(layout);

        for (ShapeSet range : ShapeSet.getShapeSets()) {
            Shape rep = range.getRep();

            if (rep != null) {
                JButton button = new JButton();
                button.setIcon(rep.getDecoratedSymbol());
                button.setName(range.getName());
                button.addActionListener(rangeListener);
                button.setToolTipText(range.getName());
                button.setBorderPainted(false);
                panel.add(button);
            }
        }

        return panel;
    }

    //-------------------//
    // defineShapesPanel //
    //-------------------//
    /**
     * Define the panel of shapes for a given range.
     *
     * @param range the given range of shapes
     * @return the panel of shapes for the provided range
     */
    private Panel defineShapesPanel (ShapeSet range)
    {
        Panel panel = new Panel();
        panel.setNoInsets();
        panel.setPreferredSize(new Dimension(BOARD_WIDTH, heights.get(range)));

        FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.LEADING);
        panel.setLayout(layout);

        // Button to close this shapes panel and return to ranges panel
        JButton close = new JButton(range.getName());
        close.addActionListener(closeListener);
        close.setToolTipText("Back to ranges");
        close.setBorderPainted(false);
        panel.add(close);

        // One button per shape
        for (Shape shape : range.getSortedShapes()) {
            ShapeButton button = new ShapeButton(shape);
            button.addMouseListener(dropAdapter); // For DnD transfer
            button.addMouseListener(mouseListener); // For double-click
            button.addMouseMotionListener(motionAdapter); // For dragging
            panel.add(button);
        }

        return panel;
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
        ShapeSymbol symbol = (shape == Shape.BEAM_HOOK)
                ? shape.getPhysicalShape()
                .getSymbol() : shape.getDecoratedSymbol();

        return symbol.getIconImage();
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean publishLocationWhileDragging = new Constant.Boolean(
                false,
                "Should we publish the current location while dragging a shape?");

    }

    //-------------//
    // ShapeButton //
    //-------------//
    /**
     * A button dedicated to a shape.
     */
    private static class ShapeButton
            extends JButton
    {
        //~ Instance fields ----------------------------------------------------

        final Shape shape;

        //~ Constructors -------------------------------------------------------
        public ShapeButton (Shape shape)
        {
            this.shape = shape;
            setIcon(shape.getDecoratedSymbol());
            setName(shape.toString());
            setToolTipText(shape.toString());

            setBorderPainted(true);
        }
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
        //~ Constructors -------------------------------------------------------

        public MyDropAdapter (GhostGlassPane glassPane)
        {
            super(glassPane, null);
        }

        //~ Methods ------------------------------------------------------------
        /** Start of DnD, set pay load */
        @Override
        public void mousePressed (MouseEvent e)
        {
            // Reset the motion adapter
            motionAdapter.reset();

            ShapeButton button = (ShapeButton) e.getSource();
            Shape shape = button.shape;

            // Set shape & image
            if (shape.isDraggable()) {
                action = shape;
                image = getIconImage(shape);
            } else {
                action = Shape.NON_DRAGGABLE;
                image = Shape.NON_DRAGGABLE.getSymbol()
                        .getIconImage();
            }

            super.mousePressed(e);
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
        //~ Constructors -------------------------------------------------------

        public MyDropListener ()
        {
            // Target will be any view of sheet assembly
            super(null);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void dropped (GhostDropEvent<Shape> e)
        {
            Shape shape = e.getAction();

            if (shape != Shape.NON_DRAGGABLE) {
                ScreenPoint screenPoint = e.getDropLocation();

                // The (zoomed) sheet view
                ScrollView scrollView = sheet.getAssembly()
                        .getSelectedView();

                if (screenPoint.isInComponent(
                        scrollView.getComponent().getViewport())) {
                    RubberPanel view = scrollView.getView();
                    Point localPt = screenPoint.getLocalPoint(view);
                    view.getZoom()
                            .unscale(localPt);

                    // Asynchronously insert the desired shape at proper location
                    new InsertTask(
                            sheet,
                            shape,
                            Collections.singleton(
                            new Point(localPt.x, localPt.y))).launch(
                            sheet);
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
        //~ Instance fields ----------------------------------------------------

        // Optimization: remember the latest component on target
        private WeakReference<Component> prevComponent;

        //~ Constructors -------------------------------------------------------
        public MyMotionAdapter (GhostGlassPane glassPane)
        {
            super(glassPane);
            reset();
        }

        //~ Methods ------------------------------------------------------------
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
            ShapeButton button = (ShapeButton) e.getSource();
            Shape shape = button.shape;
            Point absPt = e.getLocationOnScreen();
            ScreenPoint screenPoint = new ScreenPoint(absPt.x, absPt.y);

            // The (zoomed) sheet view
            ScrollView scrollView = sheet.getAssembly()
                    .getSelectedView();
            Component component = scrollView.getComponent()
                    .getViewport();

            if (screenPoint.isInComponent(component)) {
                RubberPanel view = scrollView.getView();

                // Publish the current location?
                if (constants.publishLocationWhileDragging.getValue()) {
                    Point localPt = screenPoint.getLocalPoint(view);
                    view.getZoom()
                            .unscale(localPt);
                    view.pointSelected(localPt, MouseMovement.DRAGGING);
                }

                // Moving into this component?
                if (component != prevComponent.get()) {
                    glassPane.setOverTarget(true);

                    // Try to use full image size, adapted to current zoom
                    int zoomedInterline = (int) Math.rint(
                            view.getZoom().getRatio() * sheet.getScale().getInterline());
                    Shape displayedShape = shape.isDraggable() ? shape
                            : Shape.NON_DRAGGABLE;
                    BufferedImage image = MusicFont.buildImage(
                            displayedShape,
                            zoomedInterline,
                            true); // Decorated

                    if (image != null) {
                        // Use of perfectly sized font-based image
                        glassPane.setImage(image);
                    }

                    prevComponent = new WeakReference<>(component);
                }
            } else if (prevComponent.get() != null) {
                // No longer on a droppable target, reuse initial image & size
                glassPane.setOverTarget(false);
                glassPane.setImage(dropAdapter.getImage());
                reset();
            }

            // This triggers a repaint of glassPane
            glassPane.setPoint(screenPoint);
        }

        public final void reset ()
        {
            prevComponent = new WeakReference<>(null);
        }
    }
}
