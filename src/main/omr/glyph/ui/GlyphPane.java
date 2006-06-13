//-----------------------------------------------------------------------//
//                                                                       //
//                           G l y p h P a n e                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph.ui;

import omr.ProcessingException;
import omr.Step;
import omr.glyph.Glyph;
import omr.glyph.GlyphBuilder;
import omr.glyph.GlyphDirectory;
import omr.glyph.GlyphInspector;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphNetwork;
import omr.glyph.Shape;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.stick.Stick;
import omr.ui.Board;
import omr.ui.BoardsPane;
import omr.ui.util.Panel;
import omr.ui.PixelBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;
import omr.ui.util.SwingWorker;
import omr.util.Dumper;
import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Class <code>GlyphPane</code> defines a UI pane from which all glyph
 * processing actions can be launched and checked.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphPane
    implements GlyphDirectory
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(GlyphPane.class);

    //~ Instance variables ------------------------------------------------

    // Repository of known glyphs
    private final GlyphRepository repository = GlyphRepository.getInstance();

    // Glyph builder
    private final GlyphBuilder builder;

    // Glyph inspector
    private final GlyphInspector inspector;

    // Sheet with Loaded glyphs
    private final Sheet sheet;

    // Lag of vertical runs
    private final GlyphLag vLag;

    // Related Lag view
    private final SymbolGlyphView view;

    // Popup menu related to glyph selection
    private final GlyphMenu popup;

    // Panel of glyph evaluator
    private final EvaluatorsPanel evaluatorsPanel;

    // Various actions (for menus, popup, toolbar, ...)
    private RecordAction recordAction = new RecordAction();
    private RefreshAction refreshAction = new RefreshAction();

    // Pointer to related custom board
    private final Board customBoard;

    // Pointer to glyph board
    private final SymbolGlyphBoard glyphBoard;

    // The entity used for display focus
    private ShapeFocus focus;

    // The list of glyphs currently selected (empty, if none)
    private transient List<Glyph> currentGlyphs = new ArrayList<Glyph>();

    // Latest shape assigned if any
    private transient Shape latestShapeAssigned;

    //~ Constructors ------------------------------------------------------

    //-----------//
    // GlyphPane //
    //-----------//
    /**
     * Create a view in the sheet assembly tabs, dedicated to the display
     * and handling of glyphs
     *
     * @param sheet the sheet whose glyphs are considered
     */
    public GlyphPane(Sheet sheet)
    {
        this.sheet = sheet;
        vLag = sheet.getVerticalLag();

        // Allocation of components
        evaluatorsPanel = new EvaluatorsPanel(sheet, this);
        view = new SymbolGlyphView(sheet, vLag, this);
        evaluatorsPanel.setView(view);
        focus = new ShapeFocus(sheet, view, this);
        popup = new GlyphMenu(this, focus);

        // The UI combo
        customBoard = new CustomBoard();
        customBoard.getComponent().setVisible(true);

        glyphBoard = new SymbolGlyphBoard(this,
                                        sheet.getFirstSymbolId(),
                                        vLag);
        BoardsPane boardsPane = new BoardsPane
            (view,
             new PixelBoard(),
             new SectionBoard(vLag.getLastVertexId()),
             glyphBoard,
             customBoard);

        // Link with glyph builder & glyph inspector
        builder = sheet.getGlyphBuilder();
        inspector = sheet.getGlyphInspector();
        builder.setBoard(glyphBoard);

        // Create a hosting pane for the view
        ScrollLagView slv = new ScrollLagView(view);
        sheet.getAssembly().addViewTab("Glyphs", slv, boardsPane);

        // First evaluation -> NOISEs
        GlyphNetwork.getInstance().guessSheet
            (sheet, GlyphInspector.getSymbolMaxGrade());
    }

    //~ Methods -----------------------------------------------------------

    //-------------//
    // assignShape //
    //-------------//
    /**
     * Assign a shape to a glyph or a collection of glyphs. If "asGuessed"
     * is true, this assignment is performed only if the guess of the glyph
     * at hand corresponds to the shape to be assigned.
     *
     * <p>The concerned glyphs are contained in the global currentGlyphs
     * collection.
     *
     * @param shape the shape to be assigned
     * @param asGuessed flag to restrain assignment
     * @param compound flag to build one compound, rather than assign each
     * individual
     */
    public void assignShape(Shape shape,
                            boolean asGuessed,
                            boolean compound)
    {
        if (currentGlyphs.size() > 0) {
            if (asGuessed) {            // Confirmation
                for (Glyph glyph : currentGlyphs) {
                    if (glyph.getGuess() == shape) {
                        setShape(glyph, shape, /* updateUI => */ false);
                    }
                }
                focus.setCurrent(shape);
            } else {                    // Forcing
                if (compound) {
                    // Build & insert a compound
                    Glyph glyph = builder.buildCompound(currentGlyphs);
                    builder.insertCompound(glyph, currentGlyphs);
                    setShape(glyph, shape, /* updateUI => */ false);
                    // Update (new) glyph contours
                    currentGlyphs.clear();
                    currentGlyphs.add(glyph);
                } else {
                    int noiseNb = 0;
                    for (Glyph glyph : currentGlyphs) {
                        if (glyph.getShape() != Shape.NOISE) {
                            setShape(glyph, shape, /* updateUI => */ true);
                        } else {
                            noiseNb ++;
                        }
                    }
                    if (noiseNb > 0) {
                        logger.info(noiseNb + " noise glyphs skipped");
                    }
                }
                refresh();
            }
        }
    }

    //-------------//
    // cancelStems //
    //-------------//
    /**
     * Cancel one or several stems, turning them back to just a set of
     * sections, and rebuilding glyphs from their member sections together
     * with the neighbouring non-assigned sections
     *
     * @param stems a list of stems
     */
    public void cancelStems (List<Glyph> stems)
    {
        /**
         * To remove a stem, several infos need to be modified : shape from
         * COMBINING_STEM to null, result from STEM to null, and the Stem
         * must be removed from system list of stems.
         *
         * The stem glyph must be removed (as well as all other
         * non-recognized glyphs that are connected to the former stem)
         *
         * Then, re-glyph extraction from sections when everything is ready
         * (GlyphBuilder). Should work on a micro scale : just the former
         * stem and the neighboring (non-assigned) glyphs.
         */

        Set<SystemInfo> systems = new HashSet<SystemInfo>();
        for (Glyph stem : stems) {
            SystemInfo system = sheet.getSystemAtY(stem.getContourBox().y);
            inspector.removeGlyph(stem, system, /* cutSections => */ true);
            systems.add(system);
        }

        // Extract brand new glyphs from impacted systems
        for (SystemInfo system : systems) {
            inspector.extractNewSystemGlyphs(system);
        }

        // Update the UI
        refresh();
    }

    //------------------//
    // getCurrentGlyphs //
    //------------------//
    /**
     * Report the list of glyphs currently selected. This list may contain
     * 0, 1 or more glyphs
     *
     * @return the list of current glyphs
     */
    public List<Glyph> getCurrentGlyphs()
    {
        return currentGlyphs;
    }

    //-----------//
    // getEntity //
    //-----------//
    /**
     * Retrieve a glyph knowing its id
     *
     * @param id the glyph id
     * @return the corresponding glyph, or null if not found
     */
    public Glyph getEntity(Integer id)
    {
        return vLag.getGlyph(id);
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the UI display (reset the model values of all spinners,
     * update the colors of the glyphs)
     */
    public void refresh ()
    {
        glyphBoard.resetSpinners();
        focus.colorizeAllGlyphs();
    }

    //~ Methods package private -------------------------------------------

    //--------------------//
    // getEvaluatorsPanel //
    //--------------------//
    /**
     * Give access to the evaluators' panel
     *
     * @return that panel
     */
    EvaluatorsPanel getEvaluatorsPanel()
    {
        return evaluatorsPanel;
    }

    //----------//
    // getPopup //
    //----------//
    /**
     * Give access to the GlyphMenu, in order to customize its content
     *
     * @return the GlyphMenu
     */
    GlyphMenu getPopup()
    {
        return popup;
    }

    //------------------//
    // setCurrentGlyphs //
    //------------------//
    /**
     * Allow to remember a new collection of glyphs as the result of
     * current selection
     *
     * @param glyphs the current collection of glyphs
     */
    void setCurrentGlyphs(List<Glyph> glyphs)
    {
        currentGlyphs = glyphs;
    }

    //----------//
    // setShape //
    //----------//
    /**
     * Actually assign a {@link omr.glyph.Shape} to a glyph, update the
     * data model of the various spinners accordingly, and update the glyph
     * board info.
     *
     * @param glyph the glyph impacted
     * @param shape  the assigned shape
     * @param updateUI should the user interface be updated ?
     */
    public void setShape(Glyph glyph,
                  Shape shape,
                  boolean updateUI)
    {
        glyph.setShape(shape);

        // Remember the latest shape assigned
        if (shape != null) {
            latestShapeAssigned = shape;
        }

        // Update spinners models
        int id = glyph.getId();
        if (glyph.isKnown()) {
            glyphBoard.assignGlyph(id);
        } else {
            glyphBoard.deassignGlyph(id);
        }

        if (updateUI) {
            // Update immediately the glyph info as displayed
            glyphBoard.update(glyph);

            // And the color of the glyph as well
            if (shape != null) {
                focus.setCurrent(shape);
            } else {
                glyph.setGuess(null);
            }
        }
    }

    //------------------------//
    // getLatestShapeAssigned //
    //------------------------//
    /**
     * Report the latest non null shape that was assigned, or null if none
     *
     * @return latest shape assigned, or null if none
     */
    Shape getLatestShapeAssigned()
    {
        return latestShapeAssigned;
    }

    //~ Methods private ---------------------------------------------------

    //-------------------//
    // createGlobalPanel //
    //-------------------//
    private JPanel createGlobalPanel()
    {
        final String buttonWidth    = Panel.getButtonWidth();
        final String fieldInterval  = Panel.getFieldInterval();
        final String fieldInterline = Panel.getFieldInterline();

        FormLayout layout = new FormLayout
            (buttonWidth + "," + fieldInterval + "," +
             buttonWidth + "," + fieldInterval + "," +
             buttonWidth + "," + fieldInterval + "," +
             buttonWidth,
             "pref," + fieldInterline + "," +
             "pref," + fieldInterline + "," +
             "pref");

        Panel panel = new Panel();
        panel.setNoInsets();

        PanelBuilder builder = new PanelBuilder(layout, panel);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int r = 1;                      // --------------------------------
        builder.addSeparator("Global", cst.xyw(1,  r, 7));

        r += 2;                         // --------------------------------
        // Add a Refresh button
        JButton refreshButton = new JButton(refreshAction);
        refreshButton.setToolTipText("Refresh display");
        builder.add(refreshButton,      cst.xy (1,  r));

        // Add an Eval button
        EvalAction evalAction = new EvalAction();
        JButton evalButton = new JButton(evalAction);
        evalButton.setToolTipText("Evaluate unknown glyphs");
        builder.add(evalButton,         cst.xy (3,  r));

        // Add a Save button
        SaveAction saveAction = new SaveAction();
        JButton saveButton = new JButton(saveAction);
        saveButton.setToolTipText("Save current state");
        builder.add(saveButton,         cst.xy (5,  r));

        // Add a Record action
        JButton recordButton = new JButton(recordAction);
        recordButton.setToolTipText("Record glyphs for training");
        builder.add(recordButton,       cst.xy (7,  r));

        r += 2;                         // --------------------------------

        // Add a Verticals action
        VerticalsAction verticalsAction = new VerticalsAction();
        JButton verticalsButton = new JButton(verticalsAction);
        verticalsButton.setToolTipText("Extract Verticals like Stems");
        builder.add(verticalsButton,    cst.xy (1,  r));

        // Add a Leaves action
        LeavesAction leavesAction = new LeavesAction();
        JButton leavesButton = new JButton(leavesAction);
        leavesButton.setToolTipText("Extract stem Leaves");
        builder.add(leavesButton,       cst.xy (3,  r));

        // Add a Compounds action
        CompoundsAction compoundsAction = new CompoundsAction();
        JButton compoundsButton = new JButton(compoundsAction);
        compoundsButton.setToolTipText("Gather remaining stuff as Compounds");
        builder.add(compoundsButton,    cst.xy (5,  r));

        // Add a Cleanup action
        CleanupAction cleanupAction = new CleanupAction();
        JButton cleanupButton = new JButton(cleanupAction);
        cleanupButton.setToolTipText("Cleanup stems with no symbols attached");
        builder.add(cleanupButton,      cst.xy (7,  r));

        return builder.getPanel();
    }

    //---------------//
    // createMenuBar //
    //---------------//
    private JMenuBar createMenuBar()
    {
        // Menus in the frame
        JMenuBar menuBar = new JMenuBar();
        JMenuItem item;

        // Tools menu
        JMenu toolMenu = new JMenu("Tools");
        menuBar.add(toolMenu);

        // Focus mode
        JMenu focusMenu = new JMenu("Focus");
        Shape.addShapeItems
            (focusMenu,
             new ActionListener() {
                 public void actionPerformed(ActionEvent e) {
                     JMenuItem source = (JMenuItem) e.getSource();
                     focus.setCurrent(Shape.valueOf(source.getText()));
                 }
             });
        toolMenu.add(focusMenu);

        // Neural Network
        JMenu networkMenu = new JMenu("Network");
        toolMenu.add(networkMenu);

        // Add a record action
        item = networkMenu.add(recordAction);
        item.setToolTipText("Record known glyphs");

        return menuBar;
    }

    //-------------//
    // CustomBoard // -----------------------------------------------------
    //-------------//
    private class CustomBoard
        extends Board
    {
        public CustomBoard()
        {
            super(Board.Tag.CUSTOM);

            FormLayout layout = new FormLayout
                ("pref",
                 "pref," + Panel.getPanelInterline() + "," +
                 "pref," + Panel.getPanelInterline() + "," +
                 "pref");

            PanelBuilder builder = new PanelBuilder(layout, getComponent());
            builder.setDefaultDialogBorder();

            CellConstraints cst = new CellConstraints();

            int r = 1;                  // --------------------------------
            builder.add(focus, cst.xy (1, r));
            r += 2;                     // --------------------------------
            builder.add(createGlobalPanel(), cst.xy (1, r));
            r += 2;                     // --------------------------------
            builder.add(evaluatorsPanel.getComponent(), cst.xy (1, r));
        }

    }

    //---------------//
    // RefreshAction // ---------------------------------------------------
    //---------------//
    private class RefreshAction
        extends AbstractAction
    {
        public RefreshAction()
        {
            super("Refresh");
        }

        public void actionPerformed(ActionEvent e)
        {
            refresh();
        }
    }

    //------------//
    // EvalAction // ------------------------------------------------------
    //------------//
    private class EvalAction
        extends AbstractAction
    {
        public EvalAction()
        {
            super("Eval");
        }

        public void actionPerformed(ActionEvent e)
        {
            inspector.evaluateGlyphs(inspector.getLeafMaxGrade());
            refresh();
        }
    }

    //------------//
    // SaveAction // ------------------------------------------------------
    //------------//
    private class SaveAction
        extends AbstractAction
    {
        public SaveAction()
        {
            super("Save");
        }

        public void actionPerformed(ActionEvent e)
        {
            final SwingWorker worker = new SwingWorker()
                {
                    public Object construct()
                    {
                        try {
                            sheet.getScore().serialize();
                        } catch (Exception ex) {
                            logger.warning("Could not serialize " +
                                           sheet.getScore());
                        }
                        return null;
                    }
                };
            worker.start();
        }
    }

    //--------------//
    // RecordAction // ----------------------------------------------------
    //--------------//
    private class RecordAction
        extends AbstractAction
    {
        public RecordAction()
        {
            super("Record");
        }

        public void actionPerformed(ActionEvent e)
        {
            final SwingWorker worker = new SwingWorker()
                {
                    public Object construct()
                    {
                        repository.recordSheetGlyphs
                            (sheet,
                             /* emptyStructures => */ sheet.isOnSymbols());
                        return null;
                    }
                };
            worker.start();
        }
    }

    //-----------------//
    // VerticalsAction // -------------------------------------------------
    //-----------------//
    private class VerticalsAction
        extends AbstractAction
    {
        public VerticalsAction()
        {
            super("Verticals");
        }

        public void actionPerformed(ActionEvent e)
        {
            inspector.processVerticals();
            refresh();
        }
    }

    //--------------//
    // LeavesAction // ----------------------------------------------------
    //--------------//
    private class LeavesAction
        extends AbstractAction
    {
        public LeavesAction()
        {
            super("Leaves");
        }

        public void actionPerformed(ActionEvent e)
        {
            inspector.processLeaves();
            inspector.evaluateGlyphs(inspector.getLeafMaxGrade());
            refresh();
        }
    }

    //-----------------//
    // CompoundsAction // -------------------------------------------------
    //-----------------//
    private class CompoundsAction
        extends AbstractAction
    {
        public CompoundsAction()
        {
            super("Compounds");
        }

        public void actionPerformed(ActionEvent e)
        {
            inspector.processCompounds(inspector.getLeafMaxGrade());
            refresh();
        }
    }

    //---------------//
    // CleanupAction // ---------------------------------------------------
    //---------------//
    private class CleanupAction
        extends AbstractAction
    {
        public CleanupAction()
        {
            super("Cleanup");
        }

        public void actionPerformed(ActionEvent e)
        {
            inspector.processUndueStems();
            refresh();
        }
    }
}
