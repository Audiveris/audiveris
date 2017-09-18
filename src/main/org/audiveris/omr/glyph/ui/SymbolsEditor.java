//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S y m b o l s E d i t o r                                    //
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
package org.audiveris.omr.glyph.ui;

import org.audiveris.omr.classifier.BasicClassifier;
import org.audiveris.omr.classifier.DeepClassifier;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
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
import org.audiveris.omr.sheet.ui.PixelBoard;
import org.audiveris.omr.sheet.ui.SheetGradedPainter;
import org.audiveris.omr.sheet.ui.SheetResultPainter;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.NoExclusion;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.Support;
import org.audiveris.omr.sig.ui.InterBoard;
import org.audiveris.omr.sig.ui.InterController;
import org.audiveris.omr.sig.ui.ShapeBoard;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.PixelCount;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.ViewParameters.SelectionMode;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.MouseMovement;
import static org.audiveris.omr.ui.selection.SelectionHint.*;
import org.audiveris.omr.ui.selection.UserEvent;
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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPopupMenu;
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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SymbolsEditor.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Related nest view. */
    private final MyView view;

    /** Pop-up menu related to page selection. */
    private final EditorMenu pageMenu;

    /** The entity used for display focus. */
    private final ShapeFocusBoard focus;

    /** View parameters. */
    private final ViewParameters viewParams = ViewParameters.getInstance();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a view in the sheet assembly tabs, dedicated to the
     * display and handling of glyphs.
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

        focus = null;
        pageMenu = new EditorMenu(sheet);

        List<Board> boards = new ArrayList<Board>();
        boards.add(new PixelBoard(sheet));

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
            boards.add(new SectionBoard(hLag, false));
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
            boards.add(new SectionBoard(vLag, false));
        }

        boards.add(new SymbolGlyphBoard(glyphsController, true, true));
        boards.add(new InterBoard(sheet));
        boards.add(new ShapeBoard(sheet, true));
        boards.add(
                new EvaluationBoard(
                        true,
                        sheet,
                        BasicClassifier.getInstance(),
                        sheet.getGlyphIndex().getEntityService(),
                        interController,
                        true));
        boards.add(
                new EvaluationBoard(
                        true,
                        sheet,
                        DeepClassifier.getInstance(),
                        sheet.getGlyphIndex().getEntityService(),
                        interController,
                        true));

        BoardsPane boardsPane = new BoardsPane(boards);

        view = new MyView(sheet.getGlyphIndex());
        view.setLocationService(sheet.getLocationService());

        // Create a hosting pane for the view
        ScrollView slv = new ScrollView(view);
        sheet.getStub().getAssembly().addViewTab(SheetTab.DATA_TAB, slv, boardsPane);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // getMeasureAt //
    //--------------//
    /**
     * Retrieve the measure closest to the provided point.
     * <p>
     * This search is meant for user interface, so we can pick up the part which is vertically
     * closest to point ordinate (then choose measure).
     *
     * @param point the provided point
     * @return the related measure, or null
     */
    public Measure getMeasureAt (Point point)
    {
        final Staff staff = sheet.getStaffManager().getClosestStaff(point);

        if (staff != null) {
            final Part part = staff.getPart();

            if (part != null) {
                return part.getMeasureAt(point);
            }
        }

        return null;
    }

    //-----------//
    // getSlotAt //
    //-----------//
    /**
     * Retrieve the measure slot closest to the provided point.
     * <p>
     * This search is meant for user interface, so we can pick up the part which is vertically
     * closest to point ordinate (then choose measure and finally slot using closest abscissa).
     *
     * @param point the provided point
     * @return the related slot, or null
     */
    public Slot getSlotAt (Point point)
    {
        final Measure measure = getMeasureAt(point);

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
        SwingUtilities.invokeLater(
                new Runnable()
        {
            @Override
            public void run ()
            {
                view.highLight(slot);
            }
        });
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final PixelCount measureMargin = new PixelCount(
                10,
                "Number of pixels as margin when highlighting a measure");
    }

    //--------//
    // MyView //
    //--------//
    private final class MyView
            extends NestView
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Currently highlighted slot, if any. */
        private Slot highlightedSlot;

        //~ Constructors ---------------------------------------------------------------------------
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
        }

        //~ Methods --------------------------------------------------------------------------------
        //--------------//
        // contextAdded //
        //--------------//
        @Override
        public void contextAdded (Point pt,
                                  MouseMovement movement)
        {
            if (viewParams.getSelectionMode() != SelectionMode.MODE_SECTION) {
                // Glyph or Inter modes
                setFocusLocation(new Rectangle(pt), movement, CONTEXT_ADD);

                // Update highlighted slot if possible
                if (movement != MouseMovement.RELEASING) {
                    highLight(getSlotAt(pt));
                }
            }

            // Regardless of the selection mode (section or glyph)
            // we let the user play with the current glyph if so desired.
            List<Glyph> glyphs = glyphIndex.getSelectedGlyphList();

            if (movement == MouseMovement.RELEASING) {
                if ((glyphs != null) && !glyphs.isEmpty()) {
                    showPagePopup(pt, getRubberRectangle());
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
            if (viewParams.getSelectionMode() != SelectionMode.MODE_SECTION) {
                // Glyph or Inter mode
                setFocusLocation(new Rectangle(pt), movement, CONTEXT_INIT);

                // Update highlighted slot if possible
                if (movement != MouseMovement.RELEASING) {
                    highLight(getSlotAt(pt));
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

        //---------//
        // onEvent //
        //---------//
        /**
         * Handling of specific events: Location and SectionSet.
         *
         * @param event the notified event
         */
        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                // Default nest view behavior (locationEvent)
                super.onEvent(event);

                //
                //                if (event instanceof SectionSetEvent) { // SectionSet => Compound
                //                    handleEvent((SectionSetEvent) event);
                //                }
            } catch (Exception ex) {
                logger.warn(getClass().getName() + " onEvent error", ex);
            }
        }

        //------------//
        // pointAdded //
        //------------//
        @Override
        public void pointAdded (Point pt,
                                MouseMovement movement)
        {
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

            super.pointSelected(pt, movement);
        }

        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics2D g)
        {
            final Color oldColor = g.getColor();

            if (viewParams.isErrorPainting()) {
                // Use specific background for stacks in error
                for (SystemInfo system : sheet.getSystems()) {
                    for (MeasureStack stack : system.getMeasureStacks()) {
                        if (stack.isAbnormal()) {
                            stack.render(g, Colors.STACK_ABNORMAL);
                        }
                    }
                }
            }

            if (viewParams.isInputPainting()) {
                // Sections
                final boolean drawBorders = viewParams.getSelectionMode() == SelectionMode.MODE_SECTION;
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
                new SheetGradedPainter(sheet, g).process();

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

                Inter inter = (Inter) sheet.getInterIndex().getEntityService().getSelectedEntity();

                if ((inter != null) && !inter.isDeleted()) {
                    // Inter: attachments for selected inter, if any
                    Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
                    inter.renderAttachments(g);
                    g.setStroke(oldStroke);

                    // Inter: main partnerships
                    SIGraph sig = inter.getSig();

                    for (Relation rel : sig.getRelations(inter, Support.class)) {
                        Inter opp = sig.getOppositeInter(inter, rel);
                        drawSupport(g, inter, opp, rel);
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
        }

        private void drawSupport (Graphics2D g,
                                  Inter one,
                                  Inter two,
                                  Relation rel)
        {
            final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
            g.setColor((rel instanceof NoExclusion) ? Color.GRAY : Color.GREEN);

            final double r = 2; // Radius
            final Point oneCenter = one.getRelationCenter();
            Ellipse2D e1 = new Ellipse2D.Double(oneCenter.x - r, oneCenter.y - r, 2 * r, 2 * r);
            final Point twoCenter = two.getRelationCenter();
            Ellipse2D e2 = new Ellipse2D.Double(twoCenter.x - r, twoCenter.y - r, 2 * r, 2 * r);
            g.fill(e1);
            g.fill(e2);
            g.draw(new Line2D.Double(oneCenter, twoCenter));
        }

        //---------------//
        // showPagePopup //
        //---------------//
        private void showPagePopup (Point pt,
                                    Rectangle rect)
        {
            if (pageMenu.updateMenu(new Rectangle(rect))) {
                JPopupMenu popup = pageMenu.getPopup();
                popup.show(this, getZoom().scaled(pt.x) + 50, getZoom().scaled(pt.y) + 50);
            }
        }
    }
}
