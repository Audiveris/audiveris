//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S y m b o l s E d i t o r                                    //
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
package org.audiveris.omr.glyph.ui;

import org.audiveris.omr.classifier.BasicClassifier;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.dynamic.Filament;
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
import org.audiveris.omr.sheet.ui.BookActions;
import org.audiveris.omr.sheet.ui.PixelBoard;
import org.audiveris.omr.sheet.ui.SelectionPainter;
import org.audiveris.omr.sheet.ui.SheetAssembly;
import org.audiveris.omr.sheet.ui.SheetGradedPainter;
import org.audiveris.omr.sheet.ui.SheetResultPainter;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.relation.Relation;
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
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
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
 * Class {@code SymbolsEditor} defines, for a given sheet, a UI pane from which all
 * symbol processing actions can be launched and their results checked.
 *
 * @author Hervé Bitteur
 */
public class SymbolsEditor
        implements PropertyChangeListener
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SymbolsEditor.class);

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Related nest view. */
    private final MyView view;

    private final ShapeBoard shapeBoard;

    private final EvaluationBoard evaluationBoard;

    /** Pop-up menu related to page selection. */
    private final EditorMenu pageMenu;

    /** View parameters. */
    private final ViewParameters viewParams = ViewParameters.getInstance();

    /**
     * Create the DATA_TAB view in the sheet assembly tabs, dedicated to the display and
     * handling of glyphs and inters.
     *
     * @param sheet            the sheet whose glyph instances are considered
     * @param glyphsController the symbols controller for this sheet
     * @param interController  the inter controller for this sheet
     */
    public SymbolsEditor (Sheet sheet,
                          GlyphsController glyphsController,
                          InterController interController)
    {
        this.sheet = sheet;

        pageMenu = new EditorMenu(sheet);

        view = new MyView(sheet.getGlyphIndex());
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

        boards.add(
                new SymbolGlyphBoard(glyphsController, constants.selectGlyphBoard.isSet(), true));
        boards.add(new InterBoard(sheet, constants.selectInterBoard.isSet()));
        boards.add(shapeBoard = new ShapeBoard(sheet, this, constants.selectShapeBoard.isSet()));
        boards.add(evaluationBoard = new EvaluationBoard(
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

    //---------------//
    // closeEditMode //
    //---------------//
    /**
     * Quit inter edit mode.
     */
    public void closeEditMode ()
    {
        view.interEditor = null;
        refresh();
    }

    //--------------------//
    // getEvaluationBoard //
    //--------------------//
    /**
     * Report the Evaluation board (basic classifier).
     */
    public EvaluationBoard getEvaluationBoard ()
    {
        return evaluationBoard;
    }

    //----------------//
    // getEditedInter //
    //----------------//
    public Inter getEditedInter ()
    {
        if (view.interEditor == null) {
            return null;
        }

        return view.interEditor.getInter();
    }

    //----------------//
    // getInterEditor //
    //----------------//
    public InterEditor getInterEditor ()
    {
        return view.interEditor;
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
    public Measure getStrictMeasureAt (Point point)
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
            if (point.y < part.getFirstStaff().getFirstLine().yAt(point.x)) {
                return null;
            }

            if (point.y > part.getLastStaff().getLastLine().yAt(point.x)) {
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
    public Slot getStrictSlotAt (Point point)
    {
        final Measure measure = getStrictMeasureAt(point);

        if (measure != null) {
            return measure.getStack().getClosestSlot(point);
        }

        return null;
    }

    public NestView getView ()
    {
        return view;
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
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run ()
            {
                view.highLight(slot);
            }
        });
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
        view.interEditor = inter.getEditor();
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

    //--------//
    // MyView //
    //--------//
    private final class MyView
            extends NestView
    {

        /** Currently highlighted slot, if any. */
        private Slot highlightedSlot;

        /** Current relation vector. */
        private RelationVector relationVector;

        /** Inter being edited, if any. */
        private InterEditor interEditor;

        private MyView (GlyphIndex glyphIndex)
        {
            super(
                    glyphIndex.getEntityService(),
                    Arrays.asList(
                            sheet.getLagManager().getLag(Lags.HLAG),
                            sheet.getLagManager().getLag(Lags.VLAG)),
                    sheet);
            setName("SymbolsEditor-MyView");

            // Subscribe to all lags for SectionSet events
            for (Lag lag : lags) {
                lag.getEntityService().subscribeStrongly(EntityListEvent.class, this);
            }

            sheet.getInterIndex().getEntityService().subscribeStrongly(EntityListEvent.class, this);

            // Arrow keys + Enter key for inter editor
            bindInterEditionKeys();
        }

        //--------------//
        // contextAdded //
        //--------------//
        @Override
        public void contextAdded (Point pt,
                                  MouseMovement movement)
        {
            relationVector = null;
            interEditor = null;

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
            interEditor = null;

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

            repaint(); // To erase previous highlight
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
            interEditor = tryEditor(pt);

            if (interEditor != null) {
                Inter inter = interEditor.getInter();
                inter.getSig().publish(inter, SelectionHint.ENTITY_TRANSIENT);
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
            interEditor = null;

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
            super.pointSelected(pt, movement);

            // Cancel slot highlighting
            highLight(null);

            // Request focus to allow key handling
            requestFocusInWindow();

            // Look for an editor handle
            if (interEditor != null) {
                if (interEditor.process(pt, movement)) {
                    return;
                }

                interEditor = null;
            }

            // Handle relation vector
            if (null != movement) {
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

                    if (clip == null || clip.intersects(systemRect)) {
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
                        if ((inter != null) && !inter.isRemoved()
                                    && (interEditor == null || inter != interEditor.getInter())) {
                            // Highlight selected inter
                            painter.render(inter);

                            // Inter: attachments for selected inter, if any
                            Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
                            inter.renderAttachments(g);
                            g.setStroke(oldStroke);

                            // Inter: main links
                            SIGraph sig = inter.getSig();

                            if (sig != null) {
                                Set<Relation> supports = sig.getRelations(inter, Support.class);

                                if (!supports.isEmpty()) {
                                    for (Relation rel : supports) {
                                        Inter opp = sig.getOppositeInter(inter, rel);
                                        painter.drawSupport(inter, opp, rel.getClass());
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
            if (interEditor != null) {
                interEditor.render(g);
            }
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

        //-----------//
        // tryEditor //
        //-----------//
        /**
         * Try to create an inter editor, by looking for a suitable inter at provided
         * location.
         *
         * @param p provided location
         * @return the create editor, if proper Inter was found at p location.
         */
        private InterEditor tryEditor (Point p)
        {
            List<Inter> inters = sheet.getInterIndex().getContainingEntities(p);

            if (inters.size() > 1) {
                Collections.sort(inters, Inters.membersFirst);
            }

            return (!inters.isEmpty()) ? inters.get(0).getEditor() : null;
        }

        //----------------------//
        // bindInterEditionKeys //
        //----------------------//
        private void bindInterEditionKeys ()
        {
            final InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            final ActionMap actionMap = getActionMap();

            // Slight translation
            inputMap.put(KeyStroke.getKeyStroke("alt UP"), "UpTranslateAction");
            actionMap.put("UpTranslateAction", new MyTranslateAction(0, -1));

            inputMap.put(KeyStroke.getKeyStroke("alt DOWN"), "DownTranslateAction");
            actionMap.put("DownTranslateAction", new MyTranslateAction(0, 1));

            inputMap.put(KeyStroke.getKeyStroke("alt LEFT"), "LeftTranslateAction");
            actionMap.put("LeftTranslateAction", new MyTranslateAction(-1, 0));

            inputMap.put(KeyStroke.getKeyStroke("alt RIGHT"), "RightTranslateAction");
            actionMap.put("RightTranslateAction", new MyTranslateAction(1, 0));

            // End of edition
            inputMap.put(KeyStroke.getKeyStroke("ENTER"), "EndInterEditionAction");
            actionMap.put("EndInterEditionAction", new EndInterEditionAction());
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
                if (interEditor != null) {
                    interEditor.processEnd();
                    refresh();
                }
            }
        }

        //-------------------//
        // MyTranslateAction //
        //-------------------//
        private class MyTranslateAction
                extends TranslateAction
        {

            public MyTranslateAction (int dx,
                                      int dy)
            {
                super(dx, dy);
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                super.actionPerformed(e);

                if (interEditor != null) {
                    interEditor.process(new Point(dx, dy));
                    refresh();
                }
            }
        }
    }
}
