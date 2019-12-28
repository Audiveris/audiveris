//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S h a p e B o a r d                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import org.audiveris.omr.glyph.ui.SymbolsEditor;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.symbol.InterFactory;
import org.audiveris.omr.sheet.ui.BookActions;
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
import org.audiveris.omr.ui.symbol.Symbols;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.ui.util.WrapLayout;
import org.audiveris.omr.ui.view.RubberPanel;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.ui.view.Zoom;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

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
 * <li>Assignment from an existing glyph is performed by a double-click</li>
 * </ul>
 * <p>
 * A few 2-char strings typed by the user trigger the selection of a shape and its assignment.
 *
 * @author Hervé Bitteur
 */
public class ShapeBoard
        extends Board
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ShapeBoard.class);

    /** Map first typed char to selected shape set. */
    private static final Map<Character, ShapeSet> setMap = new HashMap<>();

    /** Reverse of setMap. */
    private static final Map<ShapeSet, Character> reverseSetMap = new HashMap<>();

    /** Map 2-char typed string to selected shape. */
    private static final Map<String, Shape> shapeMap = new HashMap<>();

    /** Reverse of shapeMap. */
    private static final Map<Shape, String> reverseShapeMap = new HashMap<>();

    static {
        populateCharMaps();
        populateReverseCharMaps();
    }

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
            String setName = ((JButton) e.getSource()).getName();
            ShapeSet set = ShapeSet.getShapeSet(setName);
            selectShapeSet(set);
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
            closeShapeSet();
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
                    assignGlyph(glyph, button.getShape());
                }
            }
        }
    };

    /** Panel of all shape sets. */
    private final Panel setsPanel;

    /** Map of shape panels, indexed by shapeSet. */
    private final Map<ShapeSet, Panel> shapesPanels = new HashMap<>();

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

    /** To handle sequence of keys typed. */
    private final MyKeyListener keyListener = new MyKeyListener();

    /**
     * Create a new ShapeBoard object.
     *
     * @param sheet    the related sheet
     * @param editor   symbols editor, needed for its view
     * @param selected true if initially selected
     */
    public ShapeBoard (Sheet sheet,
                       SymbolsEditor editor,
                       boolean selected)
    {
        super(Board.SHAPE, null, null, selected, false, false, false);
        this.sheet = sheet;

        dropAdapter.addDropListener(dropListener);
        shapeHistory = new ShapeHistory();
        setsPanel = buildSetsPanel();

        OMR.gui.getAppPane().addPropertyChangeListener(
                JSplitPane.DIVIDER_LOCATION_PROPERTY,
                new PropertyChangeListener()
        {
            @Override
            public void propertyChange (
                    PropertyChangeEvent pce)
            {
                resizeBoard();
            }
        });

        defineLayout();

        // Support for user shape keys
        editor.getView().addKeyListener(keyListener);
        getComponent().addKeyListener(keyListener);
    }

    //--------------//
    // addToHistory //
    //--------------//
    /**
     * Add a shape to recent history.
     *
     * @param shape the shape just used
     */
    public void addToHistory (Shape shape)
    {
        shapeHistory.add(shape);
    }

    //------------//
    // getHistory //
    //------------//
    /**
     * Report the recent shapes.
     *
     * @return list of most recent shapes
     */
    public List<Shape> getHistory ()
    {
        return shapeHistory.getShapes();
    }

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

    //-------------//
    // resizeBoard //
    //-------------//
    @Override
    public void resizeBoard ()
    {
        Integer space = UIUtil.getSplitSpace(getComponent());
        logger.debug("resizeBoard space:{}", space);

        if (space != null) {
            // Resize all visible panels in this board
            if (setsPanel.isVisible()) {
                setsPanel.setSize(space, 1);
            }

            if (shapeHistory.panel.isVisible()) {
                shapeHistory.panel.setSize(space, 1);
            }

            for (Panel panel : shapesPanels.values()) {
                if (panel.isVisible()) {
                    panel.setSize(space, 1);
                }
            }
        }

        super.resizeBoard();
    }

    //------------------//
    // populateCharMaps //
    //------------------//
    private static void populateCharMaps ()
    {
        char c;

        setMap.put(c = 'a', ShapeSet.Accidentals);
        shapeMap.put("" + c + 'f', Shape.FLAT);
        shapeMap.put("" + c + 'n', Shape.NATURAL);
        shapeMap.put("" + c + 's', Shape.SHARP);

        setMap.put(c = 'b', ShapeSet.BeamsAndTuplets);
        shapeMap.put("" + c + 'f', Shape.BEAM);
        shapeMap.put("" + c + 'h', Shape.BEAM_HOOK);
        shapeMap.put("" + c + '3', Shape.TUPLET_THREE);

        setMap.put(c = 'd', ShapeSet.Dynamics);
        shapeMap.put("" + c + 'p', Shape.DYNAMICS_P);
        shapeMap.put("" + c + 'm', Shape.DYNAMICS_MF);
        shapeMap.put("" + c + 'f', Shape.DYNAMICS_F);

        setMap.put(c = 'f', ShapeSet.Flags);
        shapeMap.put("" + c + 'u', Shape.FLAG_1_UP);
        shapeMap.put("" + c + 'd', Shape.FLAG_1);

        setMap.put(c = 'h', ShapeSet.HeadsAndDot);
        shapeMap.put("" + c + 'w', Shape.WHOLE_NOTE);
        shapeMap.put("" + c + 'v', Shape.NOTEHEAD_VOID);
        shapeMap.put("" + c + 'b', Shape.NOTEHEAD_BLACK);
        shapeMap.put("" + c + 'd', Shape.AUGMENTATION_DOT);

        setMap.put(c = 'r', ShapeSet.Rests);
        shapeMap.put("" + c + '1', Shape.WHOLE_REST);
        shapeMap.put("" + c + '2', Shape.HALF_REST);
        shapeMap.put("" + c + '4', Shape.QUARTER_REST);
        shapeMap.put("" + c + '8', Shape.EIGHTH_REST);

        setMap.put(c = 'p', ShapeSet.Physicals);
        shapeMap.put("" + c + 'l', Shape.LYRICS);
        shapeMap.put("" + c + 't', Shape.TEXT);
        shapeMap.put("" + c + 'a', Shape.SLUR_ABOVE);
        shapeMap.put("" + c + 'b', Shape.SLUR_BELOW);
        shapeMap.put("" + c + 's', Shape.STEM);
    }

    //-------------------------//
    // populateReverseCharMaps //
    //-------------------------//
    private static void populateReverseCharMaps ()
    {
        // Build reverse of setMap
        for (Entry<Character, ShapeSet> entry : setMap.entrySet()) {
            reverseSetMap.put(entry.getValue(), entry.getKey());
        }

        // Build reverse of shapeMap
        for (Entry<String, Shape> entry : shapeMap.entrySet()) {
            reverseShapeMap.put(entry.getValue(), entry.getKey());
        }
    }

    //------------//
    // addButtons //
    //------------//
    private void addButtons (Panel p,
                             List<Shape> shapes)
    {
        for (Shape shape : shapes) {
            // Preference for plain symbol
            ShapeSymbol symbol = Symbols.getSymbol(shape);

            if (symbol == null) {
                // Fall back to decorated symbol
                symbol = Symbols.getSymbol(shape, true);
            }

            if (symbol != null) {
                addButton(p, new ShapeButton(symbol));
            }
        }
    }

    //-----------//
    // addButton //
    //-----------//
    private void addButton (Panel panel,
                            ShapeButton button)
    {
        button.addMouseListener(mouseListener); // For double-click
        button.addMouseListener(dropAdapter); // For DnD transfer and double-click
        button.addMouseMotionListener(motionAdapter); // For dragging

        button.addKeyListener(keyListener);

        panel.add(button);
    }

    //-------------//
    // assignGlyph //
    //-------------//
    private void assignGlyph (Glyph glyph,
                              Shape shape)
    {
        // Actually assign the shape
        sheet.getInterController().assignGlyph(glyph, shape);
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
        panel.setName("setsPanel");
        panel.setNoInsets();
        panel.setLayout(new WrapLayout(FlowLayout.LEADING));
        panel.setBackground(Color.LIGHT_GRAY);

        for (ShapeSet set : ShapeSet.getShapeSets()) {
            Shape rep = set.getRep();

            if (rep != null) {
                JButton button = new JButton();
                button.setIcon(rep.getDecoratedSymbol());
                button.setName(set.getName());
                button.addActionListener(setListener);
                button.setBorderPainted(false);
                final Character shortcut = reverseSetMap.get(set);
                button.setToolTipText(set.getName() + standardized(shortcut));
                panel.add(button);

                // Create the child shapesPanel
                shapesPanels.put(set, buildShapesPanel(set));
            }
        }

        panel.addKeyListener(keyListener);

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
        panel.setName(set.getName());
        panel.setNoInsets();
        panel.setLayout(new WrapLayout(FlowLayout.LEADING));

        // Button to close this family and return to all-families panel
        JButton close = new JButton("<");
        close.addActionListener(closeListener);
        close.setToolTipText("Back to all families");
        close.setBorderPainted(false); // To avoid visual confusion with draggable items
        panel.add(close);

        // Title for this family
        panel.add(new JLabel(set.getName()));

        // One button (or more) per shape
        addButtons(panel, set.getSortedShapes());

        // Specific listener for keyboard
        panel.addKeyListener(keyListener);

        return panel;
    }

    //---------------//
    // closeShapeSet //
    //---------------//
    private void closeShapeSet ()
    {
        // Hide current panel of shapes
        if (shapesPanel != null) {
            shapesPanel.setVisible(false);
        }

        // Show panel of sets
        setsPanel.setVisible(true);

        resizeBoard();
        setsPanel.requestFocusInWindow();
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        CellConstraints cst = new CellConstraints();
        FormLayout layout = new FormLayout("pref",
                                           "pref," + Panel.getFieldInterline() + ",pref");
        PanelBuilder builder = new PanelBuilder(layout, getBody());
        getBody().setName("ShapeBody");

        builder.add(shapeHistory.panel, cst.xy(1, 1));
        builder.add(setsPanel, cst.xy(1, 3));

        for (Panel sp : shapesPanels.values()) {
            builder.add(sp, cst.xy(1, 3)); // All overlap setsPanel
            sp.setVisible(false);
        }
    }

    //--------------//
    // getIconImage //
    //--------------//
    private BufferedImage getIconImage (ShapeSymbol symbol)
    {
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

    //----------------//
    // selectShapeSet //
    //----------------//
    /**
     * Display the panel dedicated to the provided ShapeSet
     *
     * @param set the provided shape set
     */
    private void selectShapeSet (ShapeSet set)
    {
        // Hide panel of sets
        setsPanel.setVisible(false);

        // Show specific panel of shapes
        shapesPanel = shapesPanels.get(set);
        shapesPanel.setVisible(true);

        resizeBoard();
        shapesPanel.requestFocusInWindow();
    }

    //--------------//
    // standardized //
    //--------------//
    static String standardized (Character shortcut)
    {
        if (shortcut == null) {
            return "";
        }

        return standardized(shortcut.toString());
    }

    //--------------//
    // standardized //
    //--------------//
    static String standardized (String shortcut)
    {
        if (shortcut == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder("   (");

        for (int i = 0; i < shortcut.length(); i++) {
            if (i > 0) {
                sb.append('-');
            }

            sb.append(shortcut.charAt(i));
        }

        sb.append(')');

        return sb.toString().toUpperCase();
    }

    //-------------//
    // ShapeButton //
    //-------------//
    /**
     * A button dedicated to a shape.
     */
    public static class ShapeButton
            extends JButton
    {

        /**
         * Precise symbol, since some shapes may exhibit variants.
         * Example: slur above and slur below.
         * <p>
         * This non-decorated symbol will be used at drop time to shape the created inter.
         */
        final ShapeSymbol symbol;

        /**
         * Create a button for a shape.
         *
         * @param symbol precise plain (non-decorated) symbol
         */
        public ShapeButton (ShapeSymbol symbol)
        {
            this.symbol = symbol;
            setIcon(symbol.getDecoratedSymbol().getIcon());
            setName(symbol.getShape().toString());

            final String shortcut = reverseShapeMap.get(symbol.getShape());
            setToolTipText(symbol.getTip() + standardized(shortcut));

            setBorderPainted(true);
        }

        public Shape getShape ()
        {
            return symbol.getShape();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

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

        public MyDropAdapter ()
        {
            super(ShapeBoard.this.glassPane, null);
        }

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
            Shape shape = button.getShape();

            // Set shape & image
            if (shape.isDraggable()) {
                // Wait for drag to actually begin...
                action = shape;
                image = getIconImage(button.symbol);
            } else {
                action = Shape.NON_DRAGGABLE;
                image = Shape.NON_DRAGGABLE.getSymbol().getIconImage();
                ((OmrGlassPane) glassPane).setStaffReference(null);
            }

            super.mousePressed(e);
        }

        // End of DnD. Reset pay load
        @Override
        public void mouseReleased (MouseEvent e)
        {
            super.mouseReleased(e);

            OmrGlassPane glass = (OmrGlassPane) glassPane;
            glass.setStaffReference(null);
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

        public MyDropListener ()
        {
            // Target will be any view of sheet assembly
            super(null);
        }

        @Override
        public void dropped (GhostDropEvent<Shape> e)
        {
            Shape shape = e.getAction();

            if (dndOperation != null) {
                if (shape != Shape.NON_DRAGGABLE) {
                    ScreenPoint screenPoint = e.getDropLocation();

                    // The (zoomed) sheet view
                    ScrollView scrollView = sheet.getStub().getAssembly().getSelectedView();

                    if (screenPoint.isInComponent(scrollView.getComponent().getViewport())) {
                        RubberPanel view = scrollView.getView();
                        Point localPt = screenPoint.getLocalPoint(view);
                        view.getZoom().unscale(localPt);

                        dndOperation.drop(localPt);

                        // Update history
                        addToHistory(dndOperation.getGhost().getShape());
                    }
                }
            }

            keyListener.reset();
        }
    }

    //---------------//
    // MyKeyListener //
    //---------------//
    /**
     * Listener in charge of retrieving the keys typed by the user in editor view.
     */
    private class MyKeyListener
            implements KeyListener
    {

        /** First character typed. */
        Character c1 = null;

        @Override
        public void keyPressed (KeyEvent e)
        {
            if (c1 == null) {
                // Zoom level?
                if (e.isControlDown()) {
                    char c = e.getKeyChar();

                    if (c == '+') {
                        sheet.getSymbolsEditor().getView().getRubber().modifyZoomRatio(1);
                        return;
                    }

                    if (c == '-') {
                        sheet.getSymbolsEditor().getView().getRubber().modifyZoomRatio(-1);
                        return;
                    }

                    if (c == '0') {
                        sheet.getSymbolsEditor().getView().getRubber().modifyZoomRatio(0);
                        return;
                    }
                }
            }
        }

        @Override
        public void keyReleased (KeyEvent e)
        {
        }

        @Override
        public void keyTyped (KeyEvent e)
        {
            char c = e.getKeyChar();

            if (c1 == null) {
                // First character (family)
                ShapeSet set = setMap.get(c);
                closeShapeSet();

                if (set != null) {
                    logger.debug("set:{}", set.getName());
                    selectShapeSet(set);
                    c1 = c;
                } else {
                    reset();

                    // Enter/exit repetitive input mode?
                    if (c == 'n') {
                        BookActions.getInstance().toggleRepetitiveInput(null);
                        return;
                    }

                    // Direct use of classifier buttons?
                    if (c >= '1' && c <= '5') {
                        int id = c - '0';
                        sheet.getSymbolsEditor().getEvaluationBoard().selectButton(id);
                        return;
                    }
                }
            } else {
                // Second character (shape within family)
                String str = String.valueOf(new char[]{c1, c});
                Shape shape = shapeMap.get(str);
                logger.debug("shape:{}", shape);

                if (shape != null) {
                    Glyph glyph = sheet.getGlyphIndex().getSelectedGlyph();

                    if (glyph != null) {
                        assignGlyph(glyph, shape);
                    } else {
                        // Set focus on proper shape button
                        addToHistory(shape);
                        shapeHistory.setFocus();
                    }
                } else {
                    closeShapeSet();
                }

                reset();
            }
        }

        public void reset ()
        {
            c1 = null;
            logger.debug("---Reset---");
        }
    }

    //-----------------//
    // MyMotionAdapter //
    //-----------------//
    /**
     * Adapter in charge of forwarding the current mouse location and
     * updating the dragged image according to the target under the mouse.
     * <p>
     * Generally, the ghost image is displayed exactly on the mouse location.
     * But we can also display the image at a slightly different location than the current user
     * dragging point, for example for a head shape, we can snap the image on the grid of staff
     * lines and ledgers.
     */
    private class MyMotionAdapter
            extends GhostMotionAdapter
    {

        // Optimization: remember the latest component on target
        private WeakReference<Component> prevComponent;

        private ScreenPoint prevScreenPoint;

        public MyMotionAdapter ()
        {
            super(ShapeBoard.this.glassPane);
            reset();
        }

        public final void reset ()
        {
            prevComponent = new WeakReference<>(null);
        }

        /**
         * In this specific implementation, we update the size of the shape image
         * according to the interline scale and to the display zoom of the sheet view.
         *
         * @param e the mouse event
         */
        @Override
        public void mouseDragged (MouseEvent e)
        {
            ScreenPoint screenPoint = new ScreenPoint(e.getXOnScreen(), e.getYOnScreen());

            if (screenPoint.equals(prevScreenPoint)) {
                return;
            }

            prevScreenPoint = screenPoint;

            final ShapeButton button = (ShapeButton) e.getSource();
            final Shape shape = button.getShape();
            final OmrGlassPane glass = (OmrGlassPane) glassPane;

            // The (zoomed) sheet view
            ScrollView scrollView = sheet.getStub().getAssembly().getSelectedView();
            Component component = scrollView.getComponent().getViewport();

            if (screenPoint.isInComponent(component)) {
                final RubberPanel view = scrollView.getView();
                final Zoom zoom = view.getZoom();
                final Point sheetPt = zoom.unscaled(screenPoint.getLocalPoint(view));
                glass.setOverTarget(true);

                // Moving into this component?
                if (component != prevComponent.get()) {
                    // Set glass transform to fit current sheet view status
                    glass.setTargetTransform(view.getTransformToGlass(glass));

                    if (shape.isDraggable()) {
                        if (dndOperation == null) {
                            // Set payload
                            dndOperation = new DndOperation(
                                    sheet,
                                    zoom,
                                    InterFactory.createManual(shape, sheet),
                                    button.symbol);
                        }

                        dndOperation.enteringTarget();
                        glass.setGhostTracker(dndOperation.getGhostTracker());
                    } else {
                        glass.setImage(getNonDraggableImage(zoom));
                        glass.setStaffReference(null);
                        glass.setGhostTracker(null);
                    }

                    prevComponent = new WeakReference<>(component);
                }

                if (shape.isDraggable()) {
                    // Update staff reference point
                    Point sheetRef = dndOperation.getStaffReference(sheetPt);
                    Point glassRef = (sheetRef == null) ? null
                            : SwingUtilities.convertPoint(view, zoom.scaled(sheetRef), glass);
                    glass.setStaffReference(glassRef);

                    // Recompute screenPoint from modified sheetPt
                    Point pt = new Point(zoom.scaled(sheetPt));
                    SwingUtilities.convertPointToScreen(pt, view);
                    screenPoint = new ScreenPoint(pt.x, pt.y);
                }
            } else if (prevComponent.get() != null) {
                // No longer on a droppable target, reuse initial image & size
                glass.setOverTarget(false);
                glass.setImage(dropAdapter.getImage());
                glass.setStaffReference(null);
                glass.setGhostTracker(null);
                prevScreenPoint = null;
                reset();
            }

            glass.setScreenPoint(screenPoint); // This triggers a repaint of glassPane
        }
    }

    //--------------//
    // ShapeHistory //
    //--------------//
    /**
     * This class handles the recent history of shape selection, providing a convenient
     * way to reuse of a shape recently selected.
     */
    private class ShapeHistory
    {

        /** Shapes recently used, ordered from most to less recent. */
        private final List<Shape> shapes = new ArrayList<>();

        private final Panel panel = new Panel();

        public ShapeHistory ()
        {
            panel.setNoInsets();
            panel.setName("history");
            panel.setVisible(false);
            panel.setLayout(new WrapLayout(FlowLayout.LEADING));
        }

        /**
         * Insert a shape in history.
         * <p>
         * This dynamically modifies the display of recently used shapes, and thus must be performed
         * from EDT (and not from a background thread).
         *
         * @param shape the most recent shape
         */
        public void add (final Shape shape)
        {
            if (!SwingUtilities.isEventDispatchThread()) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable()
                    {
                        @Override
                        public void run ()
                        {
                            add(shape);
                        }
                    });
                } catch (InterruptedException |
                         InvocationTargetException ex) {
                    logger.warn("invokeAndWait error", ex);
                }
            } else {
                shapes.remove(shape); // Remove duplicate if any
                shapes.add(0, shape); // Insert at beginning of the list

                // Check for maximum length
                while (shapes.size() > constants.maxHistoryLength.getValue()) {
                    shapes.remove(shapes.size() - 1);
                }

                // Regenerate the buttons
                panel.removeAll();
                addButtons(panel, shapes);

                panel.setVisible(true);
                resizeBoard();
            }
        }

        public List<Shape> getShapes ()
        {
            return Collections.unmodifiableList(shapes);
        }

        /**
         * Pre-select the first button of the history.
         */
        public void setFocus ()
        {
            for (Component comp : panel.getComponents()) {
                if (comp instanceof ShapeButton) {
                    ShapeButton button = (ShapeButton) comp;
                    button.requestFocusInWindow();

                    return;
                }
            }
        }
    }
}
