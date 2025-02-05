//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S h a p e B o a r d                                       //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.glyph.ShapeSet.HeadMotif;
import static org.audiveris.omr.glyph.ShapeSet.HeadMotif.*;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.symbol.InterFactory;
import org.audiveris.omr.sheet.ui.SheetEditor;
import org.audiveris.omr.sheet.ui.SheetEditor.SheetKeyListener;
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
import org.audiveris.omr.ui.symbol.FontSymbol;
import org.audiveris.omr.ui.symbol.MusicFamily;
import static org.audiveris.omr.ui.symbol.MusicFont.TINY_INTERLINE;
import static org.audiveris.omr.ui.symbol.MusicFont.getPointSize;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.WrapLayout;
import org.audiveris.omr.ui.view.RubberPanel;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.ui.view.Zoom;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

/**
 * Class <code>ShapeBoard</code> hosts a palette of shapes for insertion and assignment of
 * inter.
 * <p>
 * Shapes are gathered and presented in separate sets.
 * <ol>
 * <li>The <b>history</b> panel, always present but perhaps empty, caches the latest shapes
 * actually used.
 * <li>The <b>global</b> panel allows to choose among shape sets.
 * It is then dynamically replaced by the selected set panel.
 * <li>A <b>set</b> panel is dedicated to the shapes of the selected set.
 * The user can always quit this set panel and go back to the global panel.
 * </ol>
 * User gestures:
 * <ul>
 * <li>Direct insertion is performed by<b> drag n' drop</b> from ShapeBoard (history panel or set
 * panel) to the target location in sheet view</li>
 * <li>Assignment of the currently selected glyph is performed by a <b>double-click</b> on proper
 * shape button (in history panel or set panel)</li>
 * </ul>
 * Keyboard mapping:
 * <p>
 * A few 2-char strings typed by the user trigger the selection of a shape and its assignment.
 *
 * @author Hervé Bitteur
 */
public class ShapeBoard
        extends Board
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ShapeBoard.class);

    /** Unicode value for black up-pointing triangle sign: {@value}. */
    private static final String BACK = "\u25B2";

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

    //~ Instance fields ----------------------------------------------------------------------------

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** On-going DnD operation, if any. */
    private InterDnd dnd;

    /**
     * Called-back when a set is selected:
     * The global panel is replaced by the selected set panel.
     */
    private final ActionListener setListener = (ActionEvent e) -> {
        String setName = ((Component) e.getSource()).getName();
        ShapeSet set = ShapeSet.getShapeSet(setName);
        selectSet(set);
    };

    /**
     * Called-back when a set panel is closed:
     * It is replaced by the global panel to allow the selection of another set.
     */
    private final ActionListener closeListener = (ActionEvent e) -> {
        closeSet();
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

    /** The global panel. */
    private final Panel globalPanel;

    /** Map of set panels, indexed by shapeSet. */
    private final Map<ShapeSet, Panel> setPanels = new HashMap<>();

    /** History of recently used shapes. */
    private final ShapeHistory shapeHistory;

    /** Current set panel. */
    private Panel currentSetPanel;

    /** GlassPane. */
    private final GhostGlassPane glassPane = OMR.gui.getGlassPane();

    /** Update image and forward mouse location. */
    private final MyMotionAdapter motionAdapter = new MyMotionAdapter();

    /** When symbol is dropped. */
    private final GhostDropListener<Shape> dropListener = new MyDropListener();

    /** When mouse is pressed (start) and released (stop). */
    private final MyDropAdapter dropAdapter = new MyDropAdapter();

    /** When sequence of keys are typed. */
    private final SheetKeyListener keyListener;

    /** When split container is resized, we reshape this board. */
    private final PropertyChangeListener dividerListener = (PropertyChangeEvent pce) -> {
        resizeBoard();
    };

    /** Cached list of HeadsAndDot shapes, if any. To trigger board update only when needed. */
    private List<Shape> cachedHeads;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a new ShapeBoard object.
     *
     * @param sheet       the related sheet
     * @param sheetEditor related sheet editor, needed for its view, keyListener and evaluator
     * @param selected    true if initially selected
     */
    public ShapeBoard (Sheet sheet,
                       SheetEditor sheetEditor,
                       boolean selected)
    {
        super(Board.SHAPE, null, null, selected, false, false, false);
        this.sheet = sheet;

        keyListener = sheetEditor.getSheetKeyListener();
        getComponent().addKeyListener(keyListener);

        dropAdapter.addDropListener(dropListener);
        shapeHistory = new ShapeHistory();
        globalPanel = buildAllPanels();

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------

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

        if (panel != null) {
            panel.add(button);
        }
    }

    //------------//
    // addButtons //
    //------------//
    /**
     * Add one button for every shape in provided set.
     *
     * @param p      the child panel to populate
     * @param shapes the set shapes
     */
    private void addButtons (Panel p,
                             List<Shape> shapes)
    {
        for (Shape shape : shapes) {
            final ShapeSymbol symbol = getDecoratedSymbol(shape);

            if (symbol != null) {
                try {
                    addButton(p, new ShapeButton(symbol));
                } catch (Exception ex) {
                    logger.warn("No music glyph for shape: {}", shape, ex);
                }
            } else {
                logger.warn("Panel. No button symbol for {}", shape);
            }
        }
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
    // buildAllPanels //
    //----------------//
    /**
     * Build the global panel and populate the catalog of set panels.
     *
     * @return the global panel
     */
    private Panel buildAllPanels ()
    {
        final Panel panel = new Panel();
        panel.setName("globalPanel");
        panel.setNoInsets();
        panel.setLayout(new WrapLayout(FlowLayout.LEADING));
        panel.setBackground(Color.LIGHT_GRAY);

        setPanels.clear();

        for (ShapeSet set : ShapeSet.getShapeSets()) {
            final Shape rep = set.getRep(); // Representative shape for the shape set

            if (rep != null) {
                final ShapeSymbol symbol = getDecoratedSymbol(rep);

                if (symbol != null) {
                    final ShapeButton button = new ShapeButton(symbol);
                    button.setName(set.getName());
                    button.addActionListener(setListener);
                    button.setBorderPainted(false);

                    final Character shortcut = reverseSetMap.get(set);
                    button.setToolTipText(set.getName() + standardized(shortcut));
                    panel.add(button);

                    // Create the child set Panel
                    setPanels.put(set, buildSetPanel(set));
                } else {
                    logger.error("GlobalPanel. No button symbol for {}", rep);
                }
            }
        }

        panel.addKeyListener(keyListener);

        cachedMusicFamily = sheet.getStub().getMusicFamily();
        cachedTextFamily = sheet.getStub().getTextFamily();

        return panel;
    }

    //---------------//
    // buildSetPanel //
    //---------------//
    /**
     * Build the panel for a given set.
     *
     * @param set the given set of shapes
     * @return the set panel created
     */
    private Panel buildSetPanel (ShapeSet set)
    {
        final Panel panel = new Panel();
        panel.setName(set.getName());
        panel.setNoInsets();
        panel.setLayout(new WrapLayout(FlowLayout.LEADING));

        // Button to close this set and return to global panel
        final JButton close = new JButton(BACK);
        close.addActionListener(closeListener);
        close.setToolTipText("Back to all-sets");
        close.setBorderPainted(false); // To avoid visual confusion with draggable items
        panel.add(close);

        // Title for this set
        panel.add(new JLabel(set.getName()));

        // One button (or more) per filtered shape
        final List<Shape> filtered = filteredShapes(set);

        if (set == ShapeSet.HeadsAndDot) {
            cachedHeads = filtered; // Keep in cache for potential future update
            new HeadButtons().build(panel, filtered);
        } else if (set == ShapeSet.Barlines) {
            new ButtonsTable(8).build(panel, filtered);
        } else if (set == ShapeSet.BeamsEtc) {
            new ButtonsTable(6).build(panel, filtered);
        } else if (set == ShapeSet.ClefsAndShifts) {
            new ButtonsTable(5).build(panel, filtered);
        } else if (set == ShapeSet.Dynamics) {
            new ButtonsTable(6).build(panel, filtered);
        } else if (set == ShapeSet.Flags) {
            new ButtonsTable(7).build(panel, filtered);
        } else if (set == ShapeSet.GraceAndOrnaments) {
            new ButtonsTable(4).build(panel, filtered);
        } else if (set == ShapeSet.Rests) {
            new ButtonsTable(6).build(panel, filtered);
        } else if (set == ShapeSet.Times) {
            new ButtonsTable(6).build(panel, filtered);
        } else if (set == ShapeSet.Romans) {
            new ButtonsTable(6).build(panel, filtered);
        } else if (set == ShapeSet.Physicals) {
            new ButtonsTable(4).build(panel, filtered);
        } else {
            addButtons(panel, filtered);
        }

        // Specific listener for keyboard
        panel.addKeyListener(keyListener);

        return panel;
    }

    //--------------//
    // checkInitial //
    //--------------//
    /**
     * Check if provided char is the initial of a shape set.
     *
     * @param c provided char
     * @return true if OK
     */
    public boolean checkInitial (char c)
    {
        if (!isSelected()) {
            return false;
        }

        closeSet();

        // First character (set)
        ShapeSet set = setMap.get(c);

        if (set == null) {
            return false;
        }

        logger.debug("set:{}", set.getName());
        selectSet(set);

        return true;
    }

    //----------//
    // closeSet //
    //----------//
    private void closeSet ()
    {
        if (currentSetPanel != null) {
            currentSetPanel.setVisible(false);
        }

        globalPanel.setVisible(true);

        resizeBoard();
        globalPanel.requestFocusInWindow();
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        final FormLayout layout = new FormLayout(
                "pref",
                "pref," + Panel.getFieldInterline() + ",pref");
        final FormBuilder builder = FormBuilder.create().layout(layout).panel(getBody());
        getBody().setName("ShapeBody");

        builder.addRaw(shapeHistory.panel).xy(1, 1);
        builder.addRaw(globalPanel).xy(1, 3);

        for (Panel sp : setPanels.values()) {
            builder.addRaw(sp).xy(1, 3); // Global panel and all set panels overlap!
            sp.setVisible(false);
        }
    }

    //----------------//
    // filteredShapes //
    //----------------//
    /**
     * A hack to filter shapes according to processing switches.
     *
     * @param set the set of shapes to filter
     * @return the shapes kept
     */
    private List<Shape> filteredShapes (ShapeSet set)
    {
        final List<Shape> all = set.getSortedShapes();

        if (set != ShapeSet.HeadsAndDot) {
            return all;
        }

        return ShapeSet.getProcessedShapes(sheet, all);
    }

    //--------------------//
    // getDecoratedSymbol //
    //--------------------//
    /**
     * Report the decorated symbol in standard size for the desired shape,
     * according to current music font preferences.
     *
     * @param shape the desired shape
     * @return the standard symbol found, in its decorated version
     */
    private ShapeSymbol getDecoratedSymbol (Shape shape)
    {
        final MusicFamily family = sheet.getStub().getMusicFamily();
        final FontSymbol fs = shape.getFontSymbol(family);

        if (fs == null) {
            logger.warn("No symbol for {}", shape);
            return null;
        }

        return fs.symbol.getDecoratedVersion();
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

    //----------------------//
    // getNonDraggableImage //
    //----------------------//
    private BufferedImage getNonDraggableImage (Zoom zoom)
    {
        final Shape shape = Shape.NON_DRAGGABLE;
        final MusicFamily fontFamily = sheet.getStub().getMusicFamily();
        final int interline = sheet.getScale().getInterline();
        final int zoomedInterline = (int) Math.rint(zoom.getRatio() * interline);
        final FontSymbol fs = shape.getFontSymbolByInterline(
                fontFamily,
                getPointSize(zoomedInterline));

        if (fs.symbol == null) {
            logger.warn("No symbol for non-draggable shape");
            return null;
        }

        return fs.symbol.getDecoratedVersion().buildImage(fs.font);
    }

    //---------------//
    // getSplitSpace //
    //---------------//
    /**
     * Report the available space for component within containing JSplitPane.
     * <p>
     * We take into account the JSplitPane size, the divider location and size and all
     * the insets of ancestors until the JSplitPane included.
     *
     * @return the available width
     */
    private int getSplitSpace ()
    {
        final JComponent comp = getComponent();

        synchronized (comp.getTreeLock()) {
            final JSplitPane sp = getSplitContainer();
            int space = sp.getSize().width;
            space -= sp.getDividerSize();

            final int divLoc = sp.getDividerLocation();
            space -= divLoc;

            // Remove horizontal insets
            Container parent = comp.getParent();

            while (parent != null) {
                Insets insets = parent.getInsets();
                space -= insets.left;
                space -= insets.right;

                if (parent instanceof JScrollPane scrollPane) {
                    // Remove bar width if any
                    JScrollBar bar = scrollPane.getVerticalScrollBar();

                    if (bar.isShowing()) {
                        space -= bar.getWidth();
                    }
                }

                if (parent == sp) {
                    break;
                }

                parent = parent.getParent();
            }

            if (parent == null) {
                // Not currently connected to splitContainer, include its insets anyway
                Insets insets = sp.getInsets();
                space -= insets.left;
                space -= insets.right;
            }

            return space;
        }
    }

    //------------------------//
    // getTinyDecoratedSymbol //
    //------------------------//
    /**
     * Report the tiny decorated symbol for the desired shape,
     * according to current music font preferences.
     *
     * @param shape the desired shape
     * @return the symbol found, in its tiny decorated version
     */
    private ShapeSymbol getTinyDecoratedSymbol (Shape shape)
    {
        final ShapeSymbol decoSymbol = getDecoratedSymbol(shape);

        if (decoSymbol == null) {
            return null;
        }

        return decoSymbol.getTinyVersion();
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

    //---------------//
    // processString //
    //---------------//
    /**
     * Try to process the provided string as a shape selection.
     *
     * @param str the 2-char string
     */
    public void processString (String str)
    {
        if (isSelected()) {
            final Shape shape = shapeMap.get(str);
            logger.debug("shape:{}", shape);

            if (shape != null) {
                final Glyph glyph = sheet.getGlyphIndex().getSelectedGlyph();

                if (glyph != null) {
                    assignGlyph(glyph, shape);
                } else {
                    // Just set focus on proper shape button
                    addToHistory(shape);
                    shapeHistory.setFocus();
                }
            } else {
                closeSet();
            }
        }
    }

    //-------------//
    // resizeBoard //
    //-------------//
    @Override
    public void resizeBoard ()
    {
        final int space = getSplitSpace();

        // Resize all visible panels in this board
        if (globalPanel.isVisible()) {
            globalPanel.setSize(space, 1);
        }

        if (shapeHistory.panel.isVisible()) {
            shapeHistory.panel.setSize(space, 1);
        }

        for (Panel panel : setPanels.values()) {
            if (panel.isVisible()) {
                panel.setSize(space, 1);
            }
        }

        super.resizeBoard();
    }

    //-----------//
    // selectSet //
    //-----------//
    /**
     * Display the selected set panel.
     *
     * @param fPanel the provided set panel
     */
    private void selectSet (Panel fPanel)
    {
        globalPanel.setVisible(false);

        currentSetPanel = fPanel;
        currentSetPanel.setVisible(true);
        resizeBoard();
        currentSetPanel.requestFocusInWindow();
    }

    //-----------//
    // selectSet //
    //-----------//
    /**
     * Display the set panel dedicated to the provided ShapeSet
     *
     * @param set the provided shape set
     */
    private void selectSet (ShapeSet set)
    {
        selectSet(setPanels.get(set));
    }

    //-------------------//
    // setSplitContainer //
    //-------------------//
    /**
     * {@inheritDoc}.
     * <p>
     * Start listening to splitPane being resized.
     *
     * @param sp the related split container
     */
    @Override
    public void setSplitContainer (JSplitPane sp)
    {
        // NOTA: To avoid duplicate listeners in JSplitPane, let's remove listener before adding it
        sp.removePropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, dividerListener);
        sp.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, dividerListener);

        super.setSplitContainer(sp);
    }

    //--------//
    // update //
    //--------//
    /**
     * Update ShapeBoard content.
     * <ul>
     * <li>Perhaps a new music font family or a new text font family
     * <li>Perhaps new filtered shapes according to effective processing switches.
     * </ul>
     */
    @Override
    public void update ()
    {
        final MusicFamily musicFamily = sheet.getStub().getMusicFamily();
        final TextFamily textFamily = sheet.getStub().getTextFamily();

        if (musicFamily != cachedMusicFamily || textFamily != cachedTextFamily) {
            // We can update each shape button icon in situ.
            shapeHistory.update();
            updateAllPanels();
            cachedMusicFamily = musicFamily;
            cachedTextFamily = textFamily;
        }

        final ShapeSet headSet = ShapeSet.HeadsAndDot;
        final List<Shape> newHeads = filteredShapes(headSet);

        if (!newHeads.equals(cachedHeads)) {
            // We have to deal with addition / removal of shape buttons
            final boolean isGlobal = globalPanel.isVisible();
            final String currentSetName = isGlobal ? null : currentSetPanel.getName();

            setPanels.put(headSet, buildSetPanel(headSet));

            defineLayout();

            if (isGlobal) {
                closeSet();
            } else {
                if (!headSet.getName().equals(currentSetName)) {
                    selectSet(currentSetPanel);
                } else {
                    closeSet();
                    selectSet(headSet);
                }
            }
        }
    }

    //-----------------//
    // updateAllPanels //
    //-----------------//
    private void updateAllPanels ()
    {
        updatePanel(globalPanel);

        for (Panel setPanel : setPanels.values()) {
            updatePanel(setPanel);
        }
    }

    //-------------//
    // updatePanel //
    //-------------//
    private void updatePanel (Panel panel)
    {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof ShapeButton shapeButton) {
                shapeButton.update();
            } else if (comp instanceof Panel p) {
                updatePanel(p);
            }
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

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

        setMap.put(c = 'b', ShapeSet.BeamsEtc);
        shapeMap.put("" + c + 'f', Shape.BEAM);
        shapeMap.put("" + c + 'h', Shape.BEAM_HOOK);
        shapeMap.put("" + c + '3', Shape.TUPLET_THREE);

        setMap.put(c = 'd', ShapeSet.Dynamics);
        shapeMap.put("" + c + 'p', Shape.DYNAMICS_P);
        shapeMap.put("" + c + 'm', Shape.DYNAMICS_MF);
        shapeMap.put("" + c + 'f', Shape.DYNAMICS_F);

        setMap.put(c = 'f', ShapeSet.Flags);
        shapeMap.put("" + c + 'u', Shape.FLAG_1);
        shapeMap.put("" + c + 'd', Shape.FLAG_1_DOWN);

        setMap.put(c = 'h', ShapeSet.HeadsAndDot);
        shapeMap.put("" + c + 'w', Shape.WHOLE_NOTE);
        shapeMap.put("" + c + 'v', Shape.NOTEHEAD_VOID);
        shapeMap.put("" + c + 'b', Shape.NOTEHEAD_BLACK);
        shapeMap.put("" + c + 'd', Shape.AUGMENTATION_DOT);
        shapeMap.put("" + c + 'h', Shape.HALF_NOTE_UP);
        shapeMap.put("" + c + 'q', Shape.QUARTER_NOTE_UP);

        setMap.put(c = 'r', ShapeSet.Rests);
        shapeMap.put("" + c + '1', Shape.WHOLE_REST);
        shapeMap.put("" + c + '2', Shape.HALF_REST);
        shapeMap.put("" + c + '4', Shape.QUARTER_REST);
        shapeMap.put("" + c + '8', Shape.EIGHTH_REST);

        setMap.put(c = 't', ShapeSet.Texts);
        shapeMap.put("" + c + 'l', Shape.LYRICS);
        shapeMap.put("" + c + 't', Shape.TEXT);
        shapeMap.put("" + c + 'm', Shape.METRONOME);

        setMap.put(c = 'p', ShapeSet.Physicals);
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

        return shortcut.chars() // a stream of int values
                .mapToObj(c -> (char) c) // map int to Character
                .map(c -> c.toString()) //  map Character to String (needed for joining)
                .collect(Collectors.joining("-", "   (", ")")) //
                .toUpperCase();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------------//
    // ButtonsTable //
    //--------------//
    /**
     * Populate a panel with buttons presented in a rectangular table.
     */
    public class ButtonsTable
    {
        protected final int cols;

        protected final Panel table = new Panel();

        protected final CellConstraints cst = new CellConstraints();

        /**
         * Create the <code>ButtonsTable</code> with a maximum number of columns.
         *
         * @param cols the table number of columns
         */
        public ButtonsTable (int cols)
        {
            this.cols = cols;
            table.setNoInsets();
        }

        /**
         * Build the buttons for provided shapes.
         *
         * @param panel  the hosting panel
         * @param shapes the set shapes, perhaps filtered according to processing switches
         */
        public void build (Panel panel,
                           List<Shape> shapes)
        {
            final int rows = (int) Math.ceil((double) shapes.size() / cols);
            final FormLayout layout = new FormLayout(colSpec(cols), rowSpec(rows));
            final FormBuilder builder = FormBuilder.create().layout(layout).panel(table);

            int row = 1;
            int col = -1;

            for (Shape shape : shapes) {
                col += 2;
                if (col > 2 * cols) {
                    // New line
                    row += 2;
                    col = 1;
                }

                final ShapeSymbol symbol = getDecoratedSymbol(shape);

                if (symbol != null) {
                    final ShapeButton button = new ShapeButton(symbol);
                    addButton(null, button);
                    builder.addRaw(button).xy(col, row);
                } else {
                    logger.warn("Panel. No button symbol for {}", shape);
                }
            }

            panel.add(table);
        }

        protected String colSpec (int cols)
        {
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < cols; i++) {
                if (i != 0) {
                    sb.append(",").append(Panel.getFieldInterval()).append(",");
                }

                sb.append("pref");
            }

            return sb.toString();
        }

        protected String rowSpec (int rows)
        {
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < rows; i++) {
                if (i != 0) {
                    sb.append(",").append(Panel.getFieldInterline()).append(",");
                }

                sb.append("pref");
            }

            return sb.toString();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer maxHistoryLength = new Constant.Integer(
                "shapes",
                8,
                "Maximum number of shapes kept in history");
    }

    //-------------//
    // HeadButtons //
    //-------------//
    /**
     * The heads panel is organized as a table for better readability.
     * <p>
     * We use one row per head motif and one column per head duration,
     * except for last two rows dedicated to augmentation dot plus compound notes then playing signs
     *
     * @param panel  the containing panel
     * @param shapes he filtered shapes to display
     */
    private class HeadButtons
            extends ButtonsTable
    {
        public HeadButtons ()
        {
            super(5); // Room for 5 columns (1 motif, 4 durations)
        }

        /**
         * Build the buttons for heads shape.
         *
         * @param panel    the hosting panel
         * @param filtered the head shapes, filtered according to processing switches
         */
        @Override
        public void build (Panel panel,
                           List<Shape> filtered)
        {
            final int rows = 8; // A maximum of 8 rows
            final FormLayout layout = new FormLayout(colSpec(cols), rowSpec(rows));
            final FormBuilder builder = FormBuilder.create().layout(layout).panel(table);
            final EnumSet<HeadMotif> motifs = EnumSet.noneOf(HeadMotif.class);

            for (Shape shape : filtered) {
                final ShapeSymbol symbol = getTinyDecoratedSymbol(shape);
                if (symbol == null) {
                    logger.warn("Panel. No button symbol for {}", shape);
                    continue;
                }

                final HeadMotif motif = shape.getHeadMotif();
                final int row = headRow(motif, shape);
                final int col;
                final Rational dur = shape.getNoteDuration();
                if (dur != null) {
                    // It's a real head
                    col = headCol(dur);

                    // Add motif label if still needed
                    if (!motifs.contains(motif)) {
                        builder.addROLabel(motif.name()).xy(1, row);
                        motifs.add(motif);
                    }
                } else {
                    col = shapeCol(shape);

                    // Add a label for the row of playings
                    if (shape == Shape.PLAYING_OPEN) {
                        builder.addROLabel("sign").xy(1, row);
                    }
                }

                final ShapeButton button = new ShapeButton(symbol);
                addButton(null, button);
                builder.addRaw(button).xy(col, row);
            }

            panel.add(table);
        }

        @Override
        protected String colSpec (int cols)
        {
            final StringBuilder sb = new StringBuilder();

            for (int i = 0; i < cols; i++) {
                if (i == 0) {
                    sb.append("right:35dlu"); // Head motif here (or augmentation dot)
                } else {
                    sb.append(",").append(Panel.getFieldInterval()).append(",pref");
                }
            }

            return sb.toString();
        }

        private int headCol (Rational dur)
        {
            if (dur.equals(Rational.QUARTER)) {
                return 3;
            }
            if (dur.equals(Rational.HALF)) {
                return 5;
            }
            if (dur.equals(Rational.ONE)) {
                return 7;
            }
            if (dur.equals(Rational.TWO)) {
                return 9;
            }

            return -1; // To please the compiler
        }

        private int headRow (HeadMotif motif,
                             Shape shape)
        {
            return switch (motif) {
                case null -> ShapeSet.Playings.contains(shape) ? 15 : 13;
                case oval -> 1;
                case small -> 3;
                case cross -> 5;
                case diamond -> 7;
                case triangle -> 9;
                case circle -> 11;
                default -> throw new IllegalArgumentException("No headRow for head motif " + motif);
            };
        }

        private int shapeCol (Shape shape)
        {
            return switch (shape) {
                case AUGMENTATION_DOT -> 1;
                case QUARTER_NOTE_UP -> 3;
                case QUARTER_NOTE_DOWN -> 5;
                case HALF_NOTE_UP -> 7;
                case HALF_NOTE_DOWN -> 9;

                case PLAYING_OPEN -> 3;
                case PLAYING_HALF_OPEN -> 5;
                case PLAYING_CLOSED -> 7;

                default -> throw new IllegalArgumentException("No shapeCol for " + shape);
            };
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

            final ShapeButton button = (ShapeButton) e.getSource();
            final Shape shape = button.getShape();

            // Set shape
            if (shape.isDraggable()) {
                // Wait for drag to actually begin...
                action = shape;
            } else {
                action = Shape.NON_DRAGGABLE;
                ((OmrGlassPane) glassPane).setInterDnd(null);
            }

            // Set image
            final MusicFamily fontFamily = sheet.getStub().getMusicFamily();
            final FontSymbol fs = shape.getFontSymbolByInterline(fontFamily, TINY_INTERLINE);

            if (fs.symbol != null) {
                image = fs.symbol.buildImage(fs.font);
            } else {
                logger.warn("No symbol for shape {}", shape);
            }

            super.mousePressed(e);
        }

        // End of DnD. Reset pay load
        @Override
        public void mouseReleased (MouseEvent e)
        {
            super.mouseReleased(e);

            OmrGlassPane glass = (OmrGlassPane) glassPane;
            glass.setInterDnd(null);
            dnd = null;
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

            if (dnd != null) {
                if (shape != Shape.NON_DRAGGABLE) {
                    ScreenPoint screenPoint = e.getDropLocation();

                    // The (zoomed) sheet view
                    ScrollView scrollView = sheet.getStub().getAssembly().getSelectedScrollView();

                    if (screenPoint.isInComponent(scrollView.getComponent().getViewport())) {
                        RubberPanel view = scrollView.getView();
                        Point localPt = screenPoint.getLocalPoint(view);
                        view.getZoom().unscale(localPt);

                        dnd.drop(localPt);

                        // Update history
                        addToHistory(dnd.getGhost().getShape());
                    }
                }
            }

            keyListener.reset();
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

        /**
         * In this specific implementation, we update the size of the shape image
         * according to the interline scale and to the display zoom of the sheet view.
         *
         * @param e the mouse event
         */
        @Override
        public void mouseDragged (MouseEvent e)
        {
            try {
                ScreenPoint screenPoint = new ScreenPoint(e.getXOnScreen(), e.getYOnScreen());

                if (screenPoint.equals(prevScreenPoint)) {
                    return;
                }

                prevScreenPoint = screenPoint;

                final ShapeButton button = (ShapeButton) e.getSource();
                final Shape shape = button.getShape();
                final OmrGlassPane glass = (OmrGlassPane) glassPane;

                // The (zoomed) sheet view
                ScrollView scrollView = sheet.getStub().getAssembly().getSelectedScrollView();
                Component component = scrollView.getComponent().getViewport();

                if (screenPoint.isInComponent(component)) {
                    // We are over sheet view (our target)
                    final RubberPanel view = scrollView.getView();
                    final Zoom zoom = view.getZoom();
                    final Point sheetPt = zoom.unscaled(screenPoint.getLocalPoint(view));
                    glass.setOverTarget(true);

                    // Moving into this component?
                    if (component != prevComponent.get()) {
                        // Set glass transform to fit current sheet view status
                        glass.setTargetTransform(view.getTransformToGlass(glass));

                        if (shape.isDraggable()) {
                            if (dnd == null) {
                                // Set payload
                                dnd = new InterDnd(
                                        InterFactory.createManual(shape, sheet),
                                        sheet,
                                        button.getSymbol());
                            }

                            dnd.enteringTarget();
                            glass.setInterDnd(dnd);
                        } else {
                            glass.setImage(getNonDraggableImage(zoom));
                            glass.setInterDnd(null);
                        }

                        prevComponent = new WeakReference<>(component);
                    }

                    if (dnd != null) {
                        dnd.move(sheetPt); // This may slightly modify sheetPt

                        // Recompute screenPoint from (perhaps modified) sheetPt
                        Point pt = new Point(zoom.scaled(sheetPt));
                        SwingUtilities.convertPointToScreen(pt, view);
                        screenPoint = new ScreenPoint(pt.x, pt.y);
                    }
                } else if (prevComponent.get() != null) {
                    // No longer on a droppable target, reuse initial image & size
                    glass.setOverTarget(false);
                    glass.setImage(dropAdapter.getImage());
                    glass.setInterDnd(null);
                    prevScreenPoint = null;
                    reset();
                }

                glass.setScreenPoint(screenPoint); // This triggers a repaint of glassPane
            } catch (Exception ex) {
                logger.warn("mouseDragged error: {}", ex.toString(), ex);
            }
        }

        public final void reset ()
        {
            prevComponent = new WeakReference<>(null);
        }
    }

    //-------------//
    // ShapeButton //
    //-------------//
    /**
     * A button dedicated to a shape.
     */
    public class ShapeButton
            extends JButton
    {
        // Symbol to be passed to DnD, standard size, perhaps decorated
        private ShapeSymbol decoSymbol;

        /**
         * Create a button for a shape.
         *
         * @param decoSymbol related symbol
         */
        public ShapeButton (ShapeSymbol decoSymbol)
        {
            this.decoSymbol = decoSymbol;

            setIcon(decoSymbol.getTinyVersion());
            setName(decoSymbol.getShape().toString());

            final String shortcut = reverseShapeMap.get(decoSymbol.getShape());
            setToolTipText(decoSymbol.getTip() + standardized(shortcut));

            setBorderPainted(true);

            // Display custom shapes with a specific background color
            final Shape shape = decoSymbol.getShape();
            if (shape == Shape.NUMBER_CUSTOM || shape == Shape.TIME_CUSTOM) {
                setBackground(Color.PINK);
            }
        }

        public Shape getShape ()
        {
            return decoSymbol.getShape();
        }

        public ShapeSymbol getSymbol ()
        {
            return decoSymbol;
        }

        public void update ()
        {
            // Update decoSymbol and icon
            decoSymbol = getDecoratedSymbol(decoSymbol.getShape());
            setIcon(decoSymbol.getTinyVersion());
            repaint();
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

            panel.addKeyListener(keyListener);
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
                    SwingUtilities.invokeAndWait( () -> add(shape));
                } catch (InterruptedException | InvocationTargetException ex) {
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

                if (isSelected()) {
                    panel.setVisible(true);
                    resizeBoard();
                }
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
                if (comp instanceof ShapeButton button) {
                    button.requestFocusInWindow();

                    return;
                }
            }
        }

        /**
         * Update each history button according to the new font family.
         */
        public void update ()
        {
            for (Component comp : panel.getComponents()) {
                final ShapeButton button = (ShapeButton) comp;
                final Shape shape = button.getShape();
                final ShapeSymbol symbol = getTinyDecoratedSymbol(shape);

                if (symbol != null) {
                    button.setIcon(symbol);
                } else {
                    logger.warn("History. No button symbol for {}", shape);
                }
            }

            panel.repaint();
        }
    }
}
