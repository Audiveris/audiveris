//----------------------------------------------------------------------------//
//                                                                            //
//                         S y m b o l s E d i t o r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.constant.ConstantSet;

import omr.glyph.GlyphNetwork;
import omr.glyph.Glyphs;
import omr.glyph.Nest;
import omr.glyph.ShapeEvaluator;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.NestView.ItemRenderer;

import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.Sections;
import omr.lag.ui.SectionBoard;

import omr.run.RunBoard;

import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.entity.SystemPart;
import omr.score.ui.PageMenu;
import omr.score.ui.PagePhysicalPainter;
import omr.score.ui.PaintingParameters;

import omr.selection.GlyphEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.NestEvent;
import omr.selection.SectionSetEvent;
import static omr.selection.SelectionHint.*;
import omr.selection.UserEvent;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.BoundaryEditor;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.SheetPainter;

import omr.step.Step;

import omr.ui.BoardsPane;
import omr.ui.Colors;
import omr.ui.PixelCount;
import omr.ui.util.UIUtil;
import omr.ui.view.ScrollView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 * Class {@code SymbolsEditor} defines, for a given sheet, a UI pane
 * from which all symbol processing actions can be launched and their
 * results checked.
 *
 * @author Hervé Bitteur
 */
public class SymbolsEditor
        implements PropertyChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(SymbolsEditor.class);

    //~ Instance fields --------------------------------------------------------
    /** Related instance of symbols builder */
    private final SymbolsController symbolsController;

    /** Related sheet */
    private final Sheet sheet;

    /** BoundaryEditor companion */
    private final BoundaryEditor boundaryEditor;

    /** Evaluator to check for NOISE glyphs */
    private final ShapeEvaluator evaluator = GlyphNetwork.getInstance();

    /** Related nest view */
    private final MyView view;

    /** Popup menu related to page selection */
    private PageMenu pageMenu;

    /** The entity used for display focus */
    private ShapeFocusBoard focus;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // SymbolsEditor //
    //---------------//
    /**
     * Create a view in the sheet assembly tabs, dedicated to the
     * display and handling of glyphs.
     *
     * @param sheet             the sheet whose glyphs are considered
     * @param symbolsController the symbols controller for this sheet
     */
    public SymbolsEditor (Sheet sheet,
                          SymbolsController symbolsController)
    {
        this.sheet = sheet;
        this.symbolsController = symbolsController;
        sheet.setBoundaryEditor(boundaryEditor = new BoundaryEditor(sheet));

        Nest nest = symbolsController.getNest();

        view = new MyView(nest);
        view.setLocationService(sheet.getLocationService());

        focus = new ShapeFocusBoard(
                sheet,
                symbolsController,
                new ActionListener()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                view.repaint();
            }
        },
                false);

        pageMenu = new PageMenu(
                sheet.getPage(),
                new SymbolMenu(symbolsController, evaluator, focus));

        BoardsPane boardsPane = new BoardsPane(
                new PixelBoard(sheet),
                new RunBoard(sheet.getHorizontalLag(), false),
                new SectionBoard(sheet.getHorizontalLag(), false),
                new RunBoard(sheet.getVerticalLag(), false),
                new SectionBoard(sheet.getVerticalLag(), false),
                new SymbolGlyphBoard(symbolsController, true, true),
                focus,
                new EvaluationBoard(sheet, symbolsController, true),
                new ShapeBoard(sheet, symbolsController, false));

        // Create a hosting pane for the view
        ScrollView slv = new ScrollView(view);
        sheet.getAssembly()
                .addViewTab(Step.DATA_TAB, slv, boardsPane);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // addItemRenderer //
    //-----------------//
    /**
     * Register an items renderer to render items.
     *
     * @param renderer the additional renderer
     */
    public void addItemRenderer (ItemRenderer renderer)
    {
        view.addItemRenderer(renderer);
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

    //-----------//
    // getSlotAt //
    //-----------//
    /**
     * Retrieve the measure slot closest to the provided point.
     *
     * @param point the provided point
     * @return the related slot, or null
     */
    public Slot getSlotAt (Point point)
    {
        List<SystemInfo> systems = sheet.getSystems();

        if (systems != null) {
            SystemInfo systemInfo = sheet.getSystemOf(point);

            if (systemInfo != null) {
                ScoreSystem system = systemInfo.getScoreSystem();

                SystemPart part = system.getPartAt(point);
                Measure measure = part.getMeasureAt(point);

                if (measure != null) {
                    return measure.getClosestSlot(point);
                }
            }
        }

        return null;
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
        view.refresh();
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        PixelCount measureMargin = new PixelCount(
                10,
                "Number of pixels as margin when highlighting a measure");

    }

    //--------//
    // MyView //
    //--------//
    private final class MyView
            extends NestView
    {
        //~ Instance fields ----------------------------------------------------

        /** Currently highlighted slot, if any. */
        private Slot highlightedSlot;

        //~ Constructors -------------------------------------------------------
        private MyView (Nest nest)
        {
            super(
                    nest,
                    symbolsController,
                    Arrays.asList(sheet.getHorizontalLag(), sheet.getVerticalLag()));
            setName("SymbolsEditor-MyView");

            // Subscribe to all lags for SectionSet events
            for (Lag lag : lags) {
                lag.getSectionService()
                        .subscribeStrongly(SectionSetEvent.class, this);
            }
        }

        //~ Methods ------------------------------------------------------------
        //
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

            super.pointSelected(pt, movement);
        }

        //--------------//
        // contextAdded //
        //--------------//
        @Override
        public void contextAdded (Point pt,
                                  MouseMovement movement)
        {
            if (!ViewParameters.getInstance().isSectionMode()) {
                // Glyph mode
                setFocusLocation(new Rectangle(pt), movement, CONTEXT_ADD);

                // Update highlighted slot if possible
                if (movement != MouseMovement.RELEASING) {
                    highLight(getSlotAt(pt));
                }
            }

            // Regardless of the selection mode (section or glyph)
            // we let the user play with the current glyph if so desired.
            Set<Glyph> glyphs = nest.getSelectedGlyphSet();

            if (movement == MouseMovement.RELEASING) {
                if ((glyphs != null) && !glyphs.isEmpty()) {
                    showPagePopup(pt);
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
            if (!ViewParameters.getInstance().isSectionMode()) {
                // Glyph mode
                setFocusLocation(new Rectangle(pt), movement, CONTEXT_INIT);

                // Update highlighted slot if possible
                if (movement != MouseMovement.RELEASING) {
                    highLight(getSlotAt(pt));
                }
            }

            if (movement == MouseMovement.RELEASING) {
                showPagePopup(pt);
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

            //            // Make the measure visible
            //            // Safer
            //            if ((measure == null) || (slot == null)) {
            //
            //                return;
            //            }
            //
            //            ScoreSystem system = measure.getSystem();
            //            Dimension dimension = system.getDimension();
            //            Rectangle systemBox = new Rectangle(system.getTopLeft().x,
            //                    system.getTopLeft().y, dimension.width,
            //                    dimension.height
            //                    + system.getLastPart().getLastStaff().getHeight());
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
         * On reception of SECTION_SET information, we build a
         * transient compound glyph which is then dispatched.
         * Such glyph is always generated (a null glyph if the set is null or
         * empty, a simple glyph if the set contains just one glyph, and a true
         * compound glyph when the set contains several glyphs)
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

                if (event instanceof LocationEvent) { // Location => Boundary
                    handleEvent((LocationEvent) event);
                } else if (event instanceof SectionSetEvent) { // SectionSet => Compound
                    handleEvent((SectionSetEvent) event);
                }
            } catch (Exception ex) {
                logger.warn(getClass().getName() + " onEvent error", ex);
            }
        }

        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics2D g)
        {
            PaintingParameters painting = PaintingParameters.getInstance();

            if (painting.isInputPainting()) {
                // Should we draw the section borders?
                final boolean drawBorders = ViewParameters.getInstance()
                        .isSectionMode();

                // Stroke for borders
                final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);

                if (lags != null) {
                    for (Lag lag : lags) {
                        // Render all sections, using assigned colors
                        for (Section section : lag.getVertices()) {
                            Glyph glyph = section.getGlyph();

                            if (focus.isDisplayed(glyph)) {
                                section.render(g, drawBorders);
                            }
                        }
                    }
                }

                // Restore stroke
                g.setStroke(oldStroke);
            }

            // Paint additional items, such as recognized items, etc...
            renderItems(g);
        }

        //---------//
        // publish //
        //---------//
        protected void publish (NestEvent event)
        {
            nest.getGlyphService()
                    .publish(event);
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        protected void renderItems (Graphics2D g)
        {
            PaintingParameters painting = PaintingParameters.getInstance();

            if (painting.isInputPainting()) {
                // Render all sheet physical info known so far
                sheet.getPage()
                        .accept(
                        new SheetPainter(g, boundaryEditor.isSessionOngoing()));

                // Normal display of selected items
                super.renderItems(g);
            }

            if (painting.isOutputPainting()) {
                boolean mixed = painting.isInputPainting();

                // Render the recognized score entities
                PagePhysicalPainter painter = new PagePhysicalPainter(
                        g,
                        mixed ? Colors.MUSIC_SYMBOLS : Colors.MUSIC_ALONE,
                        mixed ? false : painting.isVoicePainting(),
                        false,
                        painting.isAnnotationPainting());
                sheet.getPage()
                        .accept(painter);

                // The slot being played, if any
                if (highlightedSlot != null) {
                    painter.highlightSlot(highlightedSlot);
                }
            }
        }

        //-------------//
        // handleEvent //
        //-------------//
        /**
         * Interest in LocationEvent => system boundary modification?
         *
         * @param locationEvent location event
         */
        @SuppressWarnings("unchecked")
        private void handleEvent (LocationEvent locationEvent)
        {
            super.onEvent(locationEvent);

            // Update system boundary?
            if ((locationEvent.hint == LOCATION_INIT)
                && boundaryEditor.isSessionOngoing()) {
                Rectangle rect = locationEvent.getData();

                if ((rect != null) && (rect.width == 0) && (rect.height == 0)) {
                    boundaryEditor.inspectBoundary(rect.getLocation());
                }
            }
        }

        //-------------//
        // handleEvent //
        //-------------//
        /**
         * Interest in SectionSetEvent => transient Glyph.
         *
         * @param sectionSetEvent
         */
        @SuppressWarnings("unchecked")
        private void handleEvent (SectionSetEvent sectionSetEvent)
        {
            if (!ViewParameters.getInstance()
                    .isSectionMode()) {
                // Glyph selection mode
                return;
            }

            // Section selection mode
            MouseMovement movement = sectionSetEvent.movement;

            if (sectionSetEvent.hint.isLocation()) {
                // Collect section sets from all lags
                List<Section> allSections = new ArrayList<>();

                for (Lag lag : lags) {
                    Set<Section> selected = lag.getSelectedSectionSet();

                    if (selected != null) {
                        allSections.addAll(selected);
                    }
                }

                try {
                    Glyph compound = null;

                    if (!allSections.isEmpty()) {
                        SystemInfo system = sheet.getSystemOfSections(
                                allSections);

                        if (system != null) {
                            compound = system.buildTransientGlyph(allSections);
                        }
                    }

                    logger.debug("Editor. Publish glyph {}", compound);
                    publish(
                            new GlyphEvent(
                            this,
                            GLYPH_TRANSIENT,
                            movement,
                            compound));

                    if (compound != null) {
                        publish(
                                new GlyphSetEvent(
                                this,
                                GLYPH_TRANSIENT,
                                movement,
                                Glyphs.sortedSet(compound)));
                    } else {
                        publish(
                                new GlyphSetEvent(
                                this,
                                GLYPH_TRANSIENT,
                                movement,
                                null));
                    }
                } catch (IllegalArgumentException ex) {
                    // All sections do not belong to the same system
                    // No compound is allowed and displayed
                    logger.warn(
                            "Sections from different systems {}",
                            Sections.toString(allSections));
                }
            }
        }

        //---------------//
        // showPagePopup //
        //---------------//
        private void showPagePopup (Point pt)
        {
            pageMenu.updateMenu(new Point(pt.x, pt.y));

            JPopupMenu popup = pageMenu.getPopup();

            popup.show(
                    this,
                    getZoom().scaled(pt.x) + 20,
                    getZoom().scaled(pt.y) + 30);
        }
    }
}
