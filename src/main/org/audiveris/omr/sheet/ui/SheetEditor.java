//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S h e e t E d i t o r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.classifier.BasicClassifier;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.glyph.ui.EvaluationBoard;
import org.audiveris.omr.glyph.ui.GlyphsController;
import org.audiveris.omr.glyph.ui.NestView;
import org.audiveris.omr.glyph.ui.SymbolGlyphBoard;
import org.audiveris.omr.lag.BasicLag;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Lags;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.lag.ui.SectionBoard;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.score.ui.EditorMenu;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Slot;
import org.audiveris.omr.sheet.symbol.InterFactory;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.Rhythm;
import org.audiveris.omr.sig.relation.Support;
import org.audiveris.omr.sig.ui.InterBoard;
import org.audiveris.omr.sig.ui.InterController;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.InterService;
import org.audiveris.omr.sig.ui.RelationVector;
import org.audiveris.omr.sig.ui.ShapeBoard;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.ViewParameters.SelectionMode;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Class <code>SheetEditor</code> defines, for a given sheet, a UI pane from which all
 * processing actions can be launched and their results checked.
 *
 * @author Hervé Bitteur
 */
public class SheetEditor
        implements PropertyChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SheetEditor.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Sheet view. */
    private final EditorView view;

    private final ShapeBoard shapeBoard;

    private final EvaluationBoard evaluationBoard;

    /** Pop-up menu related to page selection. */
    private final EditorMenu pageMenu;

    /** View parameters. */
    private final ViewParameters viewParams = ViewParameters.getInstance();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create the DATA_TAB view in the sheet assembly tabs, dedicated to the display and
     * handling of glyphs and inters.
     *
     * @param sheet            the sheet whose glyph instances are considered
     * @param glyphsController the symbols controller for this sheet
     * @param interController  the inter controller for this sheet
     */
    public SheetEditor (Sheet sheet,
                        GlyphsController glyphsController,
                        InterController interController)
    {
        this.sheet = sheet;

        pageMenu = new EditorMenu(sheet);

        view = new EditorView(sheet.getGlyphIndex());
        view.setLocationService(sheet.getLocationService());

        List<Board> boards = new ArrayList<>();
        boards.add(new PixelBoard(sheet, constants.selectPixelBoard.isSet()));

        Lag hLag = sheet.getLagManager().getLag(Lags.HLAG);

        if (hLag == null) {
            hLag = new BasicLag(Lags.HLAG, Orientation.HORIZONTAL);
            sheet.getLagManager().setLag(Lags.HLAG, hLag);
        } else {
            //            RunTable hTable = hLag.getRunTable();
            //
            //            if (hTable != null) {
            //                if (hTable.getRunService() == null) {
            //                    hTable.setRunService(new RunService("hLagRuns", hTable));
            //                }
            //
            //                boards.add(new RunBoard(hLag, false));
            //            }
            //
            boards.add(new SectionBoard(hLag, constants.selectHorizontalSectionBoard.isSet()));
        }

        Lag vLag = sheet.getLagManager().getLag(Lags.VLAG);

        if (vLag == null) {
            vLag = new BasicLag(Lags.VLAG, Orientation.VERTICAL);
            sheet.getLagManager().setLag(Lags.VLAG, vLag);
        } else {
            //            RunTable vTable = vLag.getRunTable();
            //
            //            if (vTable != null) {
            //                if (vTable.getRunService() == null) {
            //                    vTable.setRunService(new RunService("vLagRuns", vTable));
            //                }
            //
            //                boards.add(new RunBoard(vLag, false));
            //            }
            //
            boards.add(new SectionBoard(vLag, constants.selectVerticalSectionBoard.isSet()));
        }

        boards.add(new SymbolGlyphBoard(glyphsController, constants.selectGlyphBoard.isSet()));
        boards.add(new InterBoard(sheet, constants.selectInterBoard.isSet()));
        boards.add(shapeBoard = new ShapeBoard(sheet, this, constants.selectShapeBoard.isSet()));
        boards.add(
                evaluationBoard = new EvaluationBoard(
                        true,
                        sheet,
                        BasicClassifier.getInstance(),
                        sheet.getGlyphIndex().getEntityService(),
                        interController,
                        constants.selectBasicClassifierBoard.isSet()));

        //        boards.add(
        //                new EvaluationBoard(
        //                        true,
        //                        sheet,
        //                        DeepClassifier.getInstance(),
        //                        sheet.getGlyphIndex().getEntityService(),
        //                        interController,
        //                        constants.selectDeepClassifierBoard.isSet()));
        //
        BoardsPane boardsPane = new BoardsPane(boards);

        // Create a hosting pane for the view
        final SheetAssembly assembly = sheet.getStub().getAssembly();
        assembly.addViewTab(SheetTab.DATA_TAB, new ScrollView(view), boardsPane);
        assembly.lockViewTab(SheetTab.DATA_TAB);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // closeEditMode //
    //---------------//
    /**
     * Quit inter edit mode.
     */
    public void closeEditMode ()
    {
        view.objectEditor = null;
        refresh();
    }

    //----------------//
    // getEditedInter //
    //----------------//
    public Inter getEditedInter ()
    {
        if (view.objectEditor instanceof InterEditor interEditor) {
            return interEditor.getInter();
        }

        return null;
    }

    //--------------------//
    // getEvaluationBoard //
    //--------------------//
    /**
     * Report the Evaluation board (basic classifier).
     *
     * @return the evaluation board
     */
    public EvaluationBoard getEvaluationBoard ()
    {
        return evaluationBoard;
    }

    //-----------------//
    // getObjectEditor //
    //-----------------//
    public ObjectEditor getObjectEditor ()
    {
        return view.objectEditor;
    }

    //---------------//
    // getShapeBoard //
    //---------------//
    /**
     * Report the shape palette
     *
     * @return the shapeBoard
     */
    public ShapeBoard getShapeBoard ()
    {
        return shapeBoard;
    }

    //---------------------//
    // getSheetKeyListener //
    //---------------------//
    /**
     * Report the key listener used by sheet editor.
     *
     * @return sheet key listener
     */
    public SheetKeyListener getSheetKeyListener ()
    {
        return view.keyListener;
    }

    //--------------//
    // getSheetView //
    //--------------//
    /**
     * Report the SheetEditor sheet view (the large graphical sheet display).
     *
     * @return the sheet view
     */
    public NestView getSheetView ()
    {
        return view;
    }

    //--------------------//
    // getStrictMeasureAt //
    //--------------------//
    /**
     * Retrieve the measure that contains the provided point.
     * <p>
     * This search is meant for user interface, we impose point to be vertically within part staves.
     *
     * @param point the provided point
     * @return the related measure, or null
     */
    public Measure getStrictMeasureAt (Point2D point)
    {
        // Containing staves: 0 (totally out), 1 (on a staff) or 2 (between staves)
        final List<Staff> staves = sheet.getStaffManager().getStavesOf(point);

        if (staves.isEmpty()) {
            return null;
        }

        final Part part = staves.get(0).getPart();

        if (staves.size() == 2) {
            // Check the 2 staves belong to the same part
            if (part != staves.get(1).getPart()) {
                return null;
            }
        }

        if (part != null) {
            // Make sure point is vertically within part staves
            if (point.getY() < part.getFirstStaff().getFirstLine().yAt(point.getX())) {
                return null;
            }

            if (point.getY() > part.getLastStaff().getLastLine().yAt(point.getX())) {
                return null;
            }

            return part.getMeasureAt(point);
        }

        return null;
    }

    //-----------------//
    // getStrictSlotAt //
    //-----------------//
    /**
     * Retrieve the measure slot closest to the provided point.
     * <p>
     * This search is meant for user interface, we impose point to be vertically within part staves
     * (then choose measure and finally slot using closest abscissa).
     *
     * @param point the provided point
     * @return the related slot, or null
     */
    public Slot getStrictSlotAt (Point2D point)
    {
        final Measure measure = getStrictMeasureAt(point);

        if (measure != null) {
            return measure.getStack().getClosestSlot(point);
        }

        return null;
    }

    //-----------//
    // highLight //
    //-----------//
    /**
     * Highlight the corresponding slot within the score display.
     *
     * @param slot the slot to highlight
     */
    public void highLight (final Slot slot)
    {
        SwingUtilities.invokeLater( () -> view.highLight(slot));
    }

    //-----------//
    // isEditing //
    //-----------//
    /**
     * Report whether the provided Inter is being edited.
     *
     * @param inter provided inter
     * @return true if provided inter is involved in editor activity
     */
    public boolean isEditing (Inter inter)
    {
        if (view.objectEditor instanceof InterEditor interEditor) {
            return interEditor.concerns(inter);
        }

        return false;
    }

    //-----------------------//
    // isRepetitiveInputMode //
    //-----------------------//
    /**
     * Report whether the repetitive input mode is ON.
     *
     * @return true if ON, false if OFF
     */
    public boolean isRepetitiveInputMode ()
    {
        return view.repetitiveInputMode;
    }

    //--------------//
    // openEditMode //
    //--------------//
    /**
     * Set the provided inter into edit mode.
     *
     * @param inter inter to edit
     */
    public void openEditMode (Inter inter)
    {
        view.objectEditor = inter.getEditor();
        BookActions.getInstance().setUndoable(true);
        refresh();
    }

    //--------------//
    // openEditMode //
    //--------------//
    /**
     * Set the provided Staff into edit mode.
     *
     * @param staff  the staff to edit
     * @param global true for global staff edition, false for lines edition
     */
    public void openEditMode (Staff staff,
                              boolean global)
    {
        view.objectEditor = staff.getEditor(global);
        BookActions.getInstance().setUndoable(true);
        refresh();
    }

    //----------------//
    // propertyChange //
    //----------------//
    @Override
    public void propertyChange (PropertyChangeEvent evt)
    {
        view.repaint();
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the UI display (reset the model values of all spinners,
     * update the colors of the glyphs).
     */
    public void refresh ()
    {
        view.repaint();
    }

    //------------------------//
    // setRepetitiveInputMode //
    //------------------------//
    /**
     * Set the specific repetitive input mode to true or false.
     *
     * @param repetitiveInputMode the new value
     */
    public void setRepetitiveInputMode (boolean repetitiveInputMode)
    {
        view.repetitiveInputMode = repetitiveInputMode;
        logger.info(
                "{} Repetitive input mode is {}",
                sheet.getId(),
                view.repetitiveInputMode ? "ON" : "OFF");
    }

    //---------------------------//
    // toggleRepetitiveInputMode //
    //---------------------------//
    /**
     * Toggle the specific repetitive input mode.
     */
    public void toggleRepetitiveInputMode ()
    {
        setRepetitiveInputMode(!view.repetitiveInputMode);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean selectPixelBoard = new Constant.Boolean(
                false,
                "Should we select Pixel board by default?");

        private final Constant.Boolean selectHorizontalSectionBoard = new Constant.Boolean(
                false,
                "Should we select Horizontal Section board by default?");

        private final Constant.Boolean selectVerticalSectionBoard = new Constant.Boolean(
                false,
                "Should we select Vertical Section board by default?");

        private final Constant.Boolean selectGlyphBoard = new Constant.Boolean(
                false,
                "Should we select Glyph board by default?");

        private final Constant.Boolean selectInterBoard = new Constant.Boolean(
                true,
                "Should we select Inter board by default?");

        private final Constant.Boolean selectShapeBoard = new Constant.Boolean(
                true,
                "Should we select Shape board by default?");

        private final Constant.Boolean selectBasicClassifierBoard = new Constant.Boolean(
                true,
                "Should we select Basic Classifier board by default?");
    }

    //------------//
    // EditorView //
    //------------//
    /**
     * This is the main view, displaying the sheet image with sections, glyphs
     * and inters/staffLines for edition.
     */
    private final class EditorView
            extends NestView
    {

        /** Currently highlighted slot, if any. */
        private Slot highlightedSlot;

        /** Current relation vector. */
        private RelationVector relationVector;

        /** Object being edited, if any. */
        private ObjectEditor objectEditor;

        /** Repetitive input mode. */
        private boolean repetitiveInputMode = false;

        /** When sequence of keys are typed. */
        private final SheetKeyListener keyListener = new SheetKeyListener();

        private EditorView (GlyphIndex glyphIndex)
        {
            super(
                    glyphIndex.getEntityService(),
                    Arrays.asList(
                            sheet.getLagManager().getLag(Lags.HLAG),
                            sheet.getLagManager().getLag(Lags.VLAG)),
                    sheet);
            setName("SymbolsEditor-EditorView");

            // Subscribe to all lags for SectionSet events
            for (Lag lag : lags) {
                lag.getEntityService().subscribeStrongly(EntityListEvent.class, this);
            }

            sheet.getInterIndex().getEntityService().subscribeStrongly(EntityListEvent.class, this);

            // Arrow keys + Enter key for inter editor
            bindInterEditionKeys();
            addKeyListener(keyListener);

        }

        //----------------------//
        // bindInterEditionKeys //
        //----------------------//
        private void bindInterEditionKeys ()
        {
            final InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            final ActionMap actionMap = getActionMap();

            // Slight translation of both rubber and editor handle
            // We override default binding of any RubbelPanel to also move editor handle
            inputMap.put(KeyStroke.getKeyStroke("alt UP"), "UpTranslateAction");
            actionMap.put("UpTranslateAction", new EditTranslateAction(0, -1));

            inputMap.put(KeyStroke.getKeyStroke("alt DOWN"), "DownTranslateAction");
            actionMap.put("DownTranslateAction", new EditTranslateAction(0, 1));

            inputMap.put(KeyStroke.getKeyStroke("alt LEFT"), "LeftTranslateAction");
            actionMap.put("LeftTranslateAction", new EditTranslateAction(-1, 0));

            inputMap.put(KeyStroke.getKeyStroke("alt RIGHT"), "RightTranslateAction");
            actionMap.put("RightTranslateAction", new EditTranslateAction(1, 0));

            // End of edition
            inputMap.put(KeyStroke.getKeyStroke("ENTER"), "EndInterEditionAction");
            actionMap.put("EndInterEditionAction", new EndInterEditionAction());
        }

        //--------------//
        // contextAdded //
        //--------------//
        @Override
        public void contextAdded (Point pt,
                                  MouseMovement movement)
        {
            relationVector = null;
            objectEditor = null;

            if (viewParams.getSelectionMode() != SelectionMode.MODE_SECTION) {
                // Glyph or Inter modes
                setFocusLocation(new Rectangle(pt), movement, SelectionHint.CONTEXT_ADD);

                // Update highlighted slot if possible
                if (movement != MouseMovement.RELEASING) {
                    highLight(getStrictSlotAt(pt));
                }
            }
        }

        //-----------------//
        // contextSelected //
        //-----------------//
        @Override
        public void contextSelected (Point pt,
                                     MouseMovement movement)
        {
            relationVector = null;
            objectEditor = null;

            if (viewParams.getSelectionMode() != SelectionMode.MODE_SECTION) {
                // Glyph or Inter mode
                setFocusLocation(new Rectangle(pt), movement, SelectionHint.CONTEXT_INIT);

                // Update highlighted slot if possible
                if (movement != MouseMovement.RELEASING) {
                    highLight(getStrictSlotAt(pt));
                }
            }

            if (movement == MouseMovement.RELEASING) {
                showPagePopup(pt, getRubberRectangle());
            }
        }

        //--------------//
        // createEditor //
        //--------------//
        /**
         * Create an inter at provided location, together with a brand new editor.
         * <p>
         * This is done when repetitive input mode is on.
         *
         * @see #selectEditor(Point)
         */
        private InterEditor createEditor (Point location)
        {
            // Inter is determined by latest history information
            final List<Shape> history = shapeBoard.getHistory();

            if (history.isEmpty()) {
                return null;
            }

            final Shape shape = history.get(0);

            if (!shape.isDraggable()) {
                return null;
            }

            Staff staff = sheet.getStaffManager().getClosestStaff(location);

            if (staff == null) {
                return null;
            }

            Inter inter = InterFactory.createManual(shape, sheet);
            inter.setStaff(staff);

            final MusicFamily family = sheet.getStub().getMusicFamily();
            final int staffInterline = staff.getSpecificInterline();
            final MusicFont font = inter.getShape().isHead() ? MusicFont.getHeadFont(
                    family,
                    sheet.getScale(),
                    staffInterline) : MusicFont.getBaseFont(family, staffInterline);
            inter.deriveFrom(font.getSymbol(shape), sheet, font, location);

            staff.getSystem().getSig().addVertex(inter); // To set inter sig
            sheet.getInterController().addInter(inter); // NOTA: this runs in a background task...

            return inter.getEditor();
        }

        //-----------//
        // highLight //
        //-----------//
        /**
         * Make the provided slot stand out.
         *
         * @param slot the current slot or null
         */
        public void highLight (Slot slot)
        {
            this.highlightedSlot = slot;

            ///repaint(); // To erase previous highlight
            //
            //            // Make the measure visible
            //            // Safer
            //            if ( (slot == null) ||(slot.getMeasure() == null)) {
            //                return;
            //            }
            //
            //            Measure measure = slot.getMeasure();
            //            SystemInfo system = measure.getPart().getSystem();
            //            Dimension dimension = system.getDimension();
            //            Rectangle systemBox = new Rectangle(
            //                    system.getTopLeft().x,
            //                    system.getTopLeft().y,
            //                    dimension.width,
            //                    dimension.height + system.getLastPart().getLastStaff().getHeight());
            //
            //            // Make the measure rectangle visible
            //            Rectangle rect = measure.getBox();
            //            int margin = constants.measureMargin.getValue();
            //            // Actually, use the whole system height
            //            rect.y = systemBox.y;
            //            rect.height = systemBox.height;
            //            rect.grow(margin, margin);
            //            showFocusLocation(rect, false);
        }

        //----------------//
        // objectSelected //
        //----------------//
        /**
         * Selection (by left button double-click), this can start inter edition.
         *
         * @param pt       the selected point in model pixel coordinates
         * @param movement the mouse movement
         */
        @Override
        public void objectSelected (Point pt,
                                    MouseMovement movement)
        {
            relationVector = null;

            // Focus, entity selection
            super.objectSelected(pt, movement);

            // Cancel slot highlighting
            highLight(null);

            // Try to select an inter and put it into edit mode
            objectEditor = selectEditor(pt);

            if (objectEditor != null) {
                if (objectEditor instanceof InterEditor interEditor) {
                    Inter inter = interEditor.getInter();
                    inter.getSig().publish(inter, SelectionHint.ENTITY_TRANSIENT);
                }

                BookActions.getInstance().setUndoable(true);
            }
        }

        //------------//
        // pointAdded //
        //------------//
        @Override
        public void pointAdded (Point pt,
                                MouseMovement movement)
        {
            relationVector = null;
            objectEditor = null;

            // Cancel slot highlighting
            highLight(null);

            super.pointAdded(pt, movement);
        }

        //---------------//
        // pointSelected //
        //---------------//
        @Override
        public void pointSelected (Point pt,
                                   MouseMovement movement)
        {
            // Cancel slot highlighting
            highLight(null);

            // Request focus to allow key handling
            requestFocusInWindow();

            // Specific repetitive input mode?
            if (repetitiveInputMode && (movement == MouseMovement.PRESSING)) {
                objectEditor = createEditor(pt);
            }

            // On-going inter edition?
            if (objectEditor != null) {
                // Publish (transient) location to allow shifting when getting close to view borders
                locationService.publish(
                        new LocationEvent(
                                this,
                                SelectionHint.ENTITY_TRANSIENT,
                                MouseMovement.DRAGGING,
                                new Rectangle(pt)));

                if (objectEditor.processMouse(pt, movement)) {
                    return;
                }

                objectEditor = null;
            }

            // Handle relation vector
            processRelationVector(pt, movement);

            // Publish location (and let all subscribers react to location value)
            super.pointSelected(pt, movement);
        }

        //-----------------------//
        // processRelationVector //
        //-----------------------//
        private void processRelationVector (Point pt,
                                            MouseMovement movement)
        {
            switch (movement) {
            case PRESSING:
                relationVector = tryVector(pt); // Starting vector, perhaps null

                break;

            case DRAGGING:
                if (relationVector != null) {
                    relationVector.extendTo(pt); // Extension
                }

                break;

            case RELEASING:
                if ((relationVector != null)) {
                    relationVector.process(); // Handle end of vector
                    relationVector = null; // This is the end
                }

                break;

            default:
                break;
            }
        }

        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics2D g)
        {
            final Color oldColor = g.getColor();

            if (viewParams.isErrorPainting()) {
                // Use specific background for stacks/measures in error
                for (SystemInfo system : sheet.getSystems()) {
                    final Rectangle clip = g.getClipBounds();
                    final Rectangle systemRect = system.getBounds();

                    if ((clip == null) || clip.intersects(systemRect)) {
                        for (MeasureStack stack : system.getStacks()) {
                            boolean measureSignalled = false;

                            // New measure-level area is preferred
                            for (Measure measure : stack.getMeasures()) {
                                if (measure.isAbnormal()) {
                                    measure.renderArea(g, Colors.AREA_ABNORMAL);
                                    measureSignalled = true;
                                }
                            }

                            // Old stack-level area for retro-compatibility
                            if (!measureSignalled && stack.isAbnormal()) {
                                stack.renderArea(g, Colors.AREA_ABNORMAL);
                            }
                        }
                    }
                }
            }

            if (viewParams.isInputPainting()) {
                // Sections
                final boolean drawBorders = viewParams
                        .getSelectionMode() == SelectionMode.MODE_SECTION;
                final Stroke oldStroke = (drawBorders) ? UIUtil.setAbsoluteStroke(g, 1f) : null;

                for (Lag lag : lags) {
                    // Render all sections, using H/V assigned colors
                    for (Section section : lag.getEntities()) {
                        section.render(g, drawBorders, null);
                    }
                }

                if (oldStroke != null) {
                    g.setStroke(oldStroke);
                }

                // Inters (with graded colors)
                new SheetGradedPainter(
                        sheet,
                        g,
                        viewParams.isVoicePainting(),
                        viewParams.isTranslucentPainting()).process();

                // Display staff line splines?
                if (ViewParameters.getInstance().isStaffLinePainting()) {
                    g.setColor(Color.LIGHT_GRAY);
                    UIUtil.setAbsoluteStroke(g, 1f);

                    for (SystemInfo system : sheet.getSystems()) {
                        for (Staff staff : system.getStaves()) {
                            staff.render(g);
                        }
                    }
                }
            }

            if (viewParams.isOutputPainting()) {
                // Inters (with opaque colors)
                g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

                boolean mixed = viewParams.isInputPainting();
                g.setColor(mixed ? Colors.MUSIC_SYMBOLS : Colors.MUSIC_ALONE);

                final boolean coloredVoices = mixed ? false : viewParams.isVoicePainting();
                final boolean annots = viewParams.isAnnotationPainting();
                new SheetResultPainter(sheet, g, coloredVoices, false, annots).process();
            }

            g.setColor(oldColor);
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        protected void renderItems (Graphics2D g)
        {
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

            if (viewParams.isInputPainting()) {
                // Normal display of selected glyphs
                super.renderItems(g);

                // Selected filaments
                EntityService<Filament> filService = sheet.getFilamentIndex().getEntityService();
                Filament filament = filService.getSelectedEntity();

                if (filament != null) {
                    for (Section section : filament.getMembers()) {
                        section.render(g, false, Color.BLACK);
                    }
                }

                // Selected inter(s)
                InterService interService = (InterService) sheet.getInterIndex().getEntityService();
                List<Inter> inters = interService.getSelectedEntityList();

                if (!inters.isEmpty()) {
                    SelectionPainter painter = new SelectionPainter(sheet, g);

                    for (Inter inter : inters) {
                        if ((inter != null) && !inter.isRemoved()) {
                            // Highlight selected inter
                            painter.render(inter);

                            // Inter: attachments for selected inter, if any
                            Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
                            inter.renderAttachments(g);
                            g.setStroke(oldStroke);

                            if (!isEditing(inter)) {
                                // Inter: main links
                                SIGraph sig = inter.getSig();

                                if (sig != null) {
                                    final Set<Relation> links = sig.getRelations(
                                            inter,
                                            Support.class,
                                            Rhythm.class);
                                    for (Relation rel : links) {
                                        Inter opp = sig.getOppositeInter(inter, rel);
                                        painter.drawLink(inter, opp, rel);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (viewParams.isOutputPainting()) {
                // Selected slot, if any
                if (highlightedSlot != null) {
                    boolean mixed = viewParams.isInputPainting();
                    g.setColor(Color.BLACK);
                    final boolean coloredVoices = mixed ? false : viewParams.isVoicePainting();
                    final boolean annots = viewParams.isAnnotationPainting();
                    new SheetResultPainter(sheet, g, coloredVoices, false, annots).highlightSlot(
                            highlightedSlot);
                }
            }

            // Relation vector?
            if (relationVector != null) {
                relationVector.render(g);
            }

            // Inter editor?
            if (objectEditor != null) {
                objectEditor.render(g);
            }
        }

        //--------------//
        // selectEditor //
        //--------------//
        /**
         * Try to select an inter, by looking for a suitable inter at provided location
         * and create an editor on this inter.
         * <p>
         * This is done when repetitive input mode if off.
         *
         * @param location provided location
         * @return the create editor, if proper Inter was found at location.
         * @see #createEditor(Point)
         */
        private InterEditor selectEditor (Point location)
        {
            List<Inter> inters = sheet.getInterIndex().getContainingEntities(location);

            if (inters.isEmpty()) {
                return null;
            }

            if (inters.size() > 1) {
                Collections.sort(inters, Inters.membersFirst);
            }

            Inter first = inters.get(0);

            if (!first.isEditable()) {
                return null;
            }

            return first.getEditor();
        }

        //---------------//
        // showPagePopup //
        //---------------//
        /**
         * Update the popup menu with current selection, then display the popup
         * on south east of current location.
         *
         * @param pt   current location
         * @param rect current rubber rectangle
         */
        private void showPagePopup (Point pt,
                                    Rectangle rect)
        {
            if (pageMenu.updateMenu(new Rectangle(rect))) {
                JPopupMenu popup = pageMenu.getPopup();
                popup.show(this, getZoom().scaled(pt.x), getZoom().scaled(pt.y));
            }
        }

        //-----------//
        // tryVector //
        //-----------//
        /**
         * Try to create a relation vector, from just a starting point, which requires to
         * find an inter at this location.
         *
         * @param p1 starting point
         * @return the created vector, if any Inter was found at p1 location
         */
        private RelationVector tryVector (Point p1)
        {
            // Look for required starting inters
            List<Inter> starts = sheet.getInterIndex().getContainingEntities(p1);

            if (starts.size() > 1) {
                Collections.sort(starts, Inters.membersFirst);
            }

            return (!starts.isEmpty()) ? new RelationVector(p1, starts) : null;
        }

        //---------------------//
        // EditTranslateAction //
        //---------------------//
        private class EditTranslateAction
                extends TranslateAction
        {

            public EditTranslateAction (int dx,
                                        int dy)
            {
                super(dx, dy);
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                super.actionPerformed(e);

                if (objectEditor != null) {
                    objectEditor.processKeyboard(new Point(dx, dy));
                    refresh();
                }
            }
        }

        //-----------------------//
        // EndInterEditionAction //
        //-----------------------//
        private class EndInterEditionAction
                extends AbstractAction
        {

            @Override
            public void actionPerformed (ActionEvent e)
            {
                if (objectEditor != null) {
                    objectEditor.endProcess();
                    refresh();
                }
            }
        }
    }

    //------------------//
    // SheetKeyListener //
    //------------------//
    /**
     * Listener in charge of retrieving the sequence of keys typed by the user.
     */
    public class SheetKeyListener
            extends KeyAdapter
    {

        /** First character typed, if any. */
        Character firstChar = null;

        @Override
        public void keyTyped (KeyEvent e)
        {
            final char c = e.getKeyChar();

            if (firstChar == null) {
                // Shape family selection?
                if (shapeBoard.isSelected() && shapeBoard.checkInitial(c)) {
                    firstChar = c;
                    return;
                }

                // Direct use of classifier buttons?
                if (evaluationBoard.isSelected()) {
                    final char maxId = (char) ('0' + EvaluationBoard.evalCount());

                    if (c >= '1' && c <= maxId) {
                        final int id = c - '0';
                        evaluationBoard.selectButton(id);
                        return;
                    }
                }
            } else {
                // Second character (shape within family)
                final String str = String.valueOf(new char[]
                { firstChar, c });
                shapeBoard.processString(str);
                reset();
            }
        }

        /**
         * Reset the key input sequence.
         */
        public void reset ()
        {
            firstChar = null;
        }
    }
}
