//----------------------------------------------------------------------------//
//                                                                            //
//                         S y m b o l s E d i t o r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.constant.ConstantSet;

import omr.glyph.GlyphEvaluator;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphNetwork;
import omr.glyph.GlyphSection;
import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;

import omr.lag.Sections;
import omr.lag.ui.ScrollLagView;
import omr.lag.ui.SectionBoard;

import omr.log.Logger;

import omr.run.RunBoard;

import omr.score.common.PixelDimension;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.ui.PageMenu;
import omr.score.ui.PagePhysicalPainter;
import omr.score.ui.PaintingParameters;

import omr.selection.GlyphEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.SectionSetEvent;
import omr.selection.SelectionHint;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.SheetPainter;

import omr.step.Step;

import omr.ui.BoardsPane;
import omr.ui.Colors;
import omr.ui.PixelCount;

import omr.util.Implement;
import omr.util.WeakPropertyChangeListener;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

import javax.swing.SwingUtilities;

/**
 * Class <code>SymbolsEditor</code> defines, for a given sheet, a UI pane from
 * which all symbol processing actions can be launched and their results checked
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
    private static final Logger logger = Logger.getLogger(SymbolsEditor.class);

    //~ Instance fields --------------------------------------------------------

    /** Related instance of symbols builder */
    private final SymbolsController symbolsBuilder;

    /** Related sheet */
    private final Sheet sheet;

    /** Evaluator to check for NOISE glyphs */
    private final GlyphEvaluator evaluator = GlyphNetwork.getInstance();

    /** Related Lag view */
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
     * Create a view in the sheet assembly tabs, dedicated to the display and
     * handling of glyphs
     *
     * @param sheet the sheet whose glyphs are considered
     * @param symbolsController the symbols controller for this sheet
     */
    public SymbolsEditor (Sheet             sheet,
                          SymbolsController symbolsController)
    {
        this.sheet = sheet;
        this.symbolsBuilder = symbolsController;

        GlyphLag lag = symbolsController.getLag();

        view = new MyView(lag);
        view.setLocationService(
            sheet.getSelectionService(),
            SheetLocationEvent.class);

        focus = new ShapeFocusBoard(
            sheet,
            symbolsController,
            new ActionListener() {
                    @Implement(ActionListener.class)
                    public void actionPerformed (ActionEvent e)
                    {
                        view.colorizeAllGlyphs();
                    }
                });

        pageMenu = new PageMenu(
            sheet.getPage(),
            new SymbolMenu(symbolsController, evaluator, focus));

        final String  unit = sheet.getId() + ":SymbolsEditor";

        BoardsPane    boardsPane = new BoardsPane(
            new PixelBoard(unit, sheet),
            new RunBoard(unit, lag),
            new SectionBoard(unit, lag),
            new SymbolGlyphBoard(
                unit + "-SymbolGlyphBoard",
                symbolsController,
                0),
            focus,
            new EvaluationBoard(
                unit + "-Evaluation-ActiveBoard",
                symbolsController,
                sheet,
                view),
            new ShapeBoard(symbolsController, sheet));

        // Create a hosting pane for the view
        ScrollLagView slv = new ScrollLagView(view);
        sheet.getAssembly()
             .addViewTab(Step.SYMBOLS_TAB, slv, boardsPane);

        // Listen to painting parameters
        PaintingParameters.getInstance()
                          .addPropertyChangeListener(
            PaintingParameters.ANNOTATION_PAINTING,
            new WeakPropertyChangeListener(this));
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // highLight //
    //-----------//
    /**
     * Highlight the corresponding slot within the score display, using the
     * values of measure and slot.
     * @param measure the measure that contains the highlighted slot
     * @param slot the slot to highlight
     */
    public void highLight (final Measure measure,
                           final Slot    slot)
    {
        SwingUtilities.invokeLater(
            new Runnable() {
                    public void run ()
                    {
                        view.highLight(measure, slot);
                    }
                });
    }

    //----------------//
    // propertyChange //
    //----------------//
    @Implement(PropertyChangeListener.class)
    public void propertyChange (PropertyChangeEvent evt)
    {
        view.repaint();
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the UI display (reset the model values of all spinners, update
     * the colors of the glyphs)
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
        extends GlyphLagView
    {
        //~ Instance fields ----------------------------------------------------

        // Currently highlighted slot & measure, if any
        private Slot    highlightedSlot;
        private Measure highlightedMeasure;

        //~ Constructors -------------------------------------------------------

        private MyView (GlyphLag lag)
        {
            super(lag, null, null, symbolsBuilder, null);
            setName("SymbolsEditor-MyView");
            colorizeAllSections();

            // Use light gray color for past successful entities
            int viewIndex = lag.viewIndexOf(this);
            sheet.colorize(lag, viewIndex, Colors.ENTITY_MINOR);

            // Listen to painting parameters
            PaintingParameters.getInstance()
                              .addPropertyChangeListener(
                PaintingParameters.LAYER_PAINTING,
                new WeakPropertyChangeListener(this));
        }

        //~ Methods ------------------------------------------------------------

        //-------------------//
        // colorizeAllGlyphs //
        //-------------------//
        /**
         * Colorize all the glyphs of the sheet
         */
        @Override
        public void colorizeAllGlyphs ()
        {
            int viewIndex = lag.viewIndexOf(this);

            for (Glyph glyph : sheet.getActiveGlyphs()) {
                colorizeGlyph(viewIndex, glyph);
            }

            repaint();
        }

        //---------------//
        // colorizeGlyph //
        //---------------//
        @Override
        public void colorizeGlyph (int   viewIndex,
                                   Glyph glyph)
        {
            colorizeGlyph(
                viewIndex,
                glyph,
                focus.isDisplayed(glyph) ? glyph.getColor() : Colors.HIDDEN);
        }

        //--------------//
        // contextAdded //
        //--------------//
        @Override
        public void contextAdded (Point         pt,
                                  MouseMovement movement)
        {
            super.contextAdded(pt, movement);

            if (ViewParameters.getInstance()
                              .isSectionSelectionEnabled()) {
            } else {
                // Retrieve the selected glyphs
                Set<Glyph> glyphs = sheet.getVerticalLag()
                                         .getSelectedGlyphSet();

                if (movement == MouseMovement.RELEASING) {
                    if ((glyphs != null) && !glyphs.isEmpty()) {
                        showPagePopup(pt);
                    }
                }
            }
        }

        //-----------------//
        // contextSelected //
        //-----------------//
        @Override
        public void contextSelected (Point         pt,
                                     MouseMovement movement)
        {
            // Section selection?
            if (!ViewParameters.getInstance()
                               .isSectionSelectionEnabled()) {
                pointSelected(pt, movement);
            }

            showPagePopup(pt);
        }

        //-----------//
        // highLight //
        //-----------//
        /**
         * Make the provided slot stand out
         * @param measure the current measure
         * @param slot the current slot
         */
        public void highLight (Measure measure,
                               Slot    slot)
        {
            this.highlightedMeasure = measure;
            this.highlightedSlot = slot;

            // Safer
            if ((measure == null) || (slot == null)) {
                repaint(); // To erase previous highlight

                return;
            }

            ScoreSystem    system = measure.getSystem();
            PixelDimension dimension = system.getDimension();
            PixelRectangle systemBox = new PixelRectangle(
                system.getTopLeft().x,
                system.getTopLeft().y,
                dimension.width,
                dimension.height +
                system.getLastPart().getLastStaff().getHeight());

            // Make the measure rectangle visible
            PixelRectangle rect = measure.getBox();
            int            margin = constants.measureMargin.getValue();
            // Actually, use the whole system height
            rect.y = systemBox.y;
            rect.height = systemBox.height;
            rect.grow(margin, margin);
            showFocusLocation(rect, false);
        }

        //---------//
        // onEvent //
        //---------//
        /**
         * On reception of GLYPH_SET or SECTION_SET information, we build a
         * transient compound glyph which is then dispatched.
         *
         * Such glyph is always generated (a null glyph if the set is null or
         * empty, a simple glyph if the set contains just one glyph, and a true
         * compound glyph when the set contains several glyphs)
         *
         * @param event the notified event
         */
        @Override
        public void onEvent (UserEvent event)
        {
            ///logger.info("*** " + getName() + " " + event);
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                // Default lag view behavior, including specifics
                super.onEvent(event);

                if (event instanceof GlyphSetEvent) { // GlyphSet => Compound
                    handleEvent((GlyphSetEvent) event);
                } else if (event instanceof SectionSetEvent) { // SectionSet => Compound
                    handleEvent((SectionSetEvent) event);
                }
            } catch (Exception ex) {
                logger.warning(getClass().getName() + " onEvent error", ex);
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
                super.render(g);
            } else {
                renderItems(g);
            }
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
                     .accept(new SheetPainter(g, false));

                // Normal display of selected items
                super.renderItems(g);
            }

            if (painting.isOutputPainting()) {
                // Render the recognized score entities
                PagePhysicalPainter painter = new PagePhysicalPainter(
                    g,
                    Colors.MUSIC,
                    painting.isAnnotationPainting());
                sheet.getPage()
                     .accept(painter);

                // The slot being played, if any
                if (highlightedSlot != null) {
                    painter.drawSlot(
                        true,
                        highlightedMeasure,
                        highlightedSlot,
                        Colors.SLOT_CURRENT);
                }
            }
        }

        //-------------//
        // handleEvent //
        //-------------//
        /**
         * Interest in GlyphSet => Compound
         * @param glyphSetEvent
         */
        private void handleEvent (GlyphSetEvent glyphSetEvent)
        {
            if (ViewParameters.getInstance()
                              .isSectionSelectionEnabled()) {
                return;
            }

            MouseMovement movement = glyphSetEvent.movement;
            Set<Glyph>    glyphs = glyphSetEvent.getData();
            Glyph         compound = null;

            if ((glyphs != null) && (glyphs.size() > 1)) {
                try {
                    SystemInfo system = sheet.getSystemOf(glyphs);

                    if (system != null) {
                        compound = system.buildTransientCompound(glyphs);
                        publish(
                            new GlyphEvent(
                                this,
                                SelectionHint.GLYPH_TRANSIENT,
                                movement,
                                compound));
                    }
                } catch (IllegalArgumentException ex) {
                    // All glyphs do not belong to the same system
                    // No compound is allowed and displayed
                    logger.warning(
                        "Glyphs from different systems " +
                        Glyphs.toString(glyphs));
                }
            }
        }

        //-------------//
        // handleEvent //
        //-------------//
        /**
         *  Interest in SectionSetEvent => transient Glyph
         * @param sectionSetEvent
         */
        @SuppressWarnings("unchecked")
        private void handleEvent (SectionSetEvent sectionSetEvent)
        {
            if (!ViewParameters.getInstance()
                               .isSectionSelectionEnabled()) {
                return;
            }

            Set<GlyphSection> sections = sectionSetEvent.getData();

            if ((sections == null) || sections.isEmpty()) {
                return;
            }

            MouseMovement movement = sectionSetEvent.movement;
            Glyph         compound = null;

            try {
                SystemInfo system = sheet.getSystemOfSections(sections);

                if (system != null) {
                    compound = system.buildTransientGlyph(sections);
                }

                publish(
                    new GlyphEvent(
                        this,
                        SelectionHint.GLYPH_TRANSIENT,
                        movement,
                        compound));

                publish(
                    new GlyphSetEvent(
                        this,
                        SelectionHint.GLYPH_TRANSIENT,
                        movement,
                        Glyphs.sortedSet(compound)));
            } catch (IllegalArgumentException ex) {
                // All sections do not belong to the same system
                // No compound is allowed and displayed
                logger.warning(
                    "Sections from different systems " +
                    Sections.toString(sections));
            }
        }

        //---------------//
        // showPagePopup //
        //---------------//
        private void showPagePopup (Point pt)
        {
            pageMenu.updateMenu(new PixelPoint(pt.x, pt.y));
            pageMenu.getPopup()
                    .show(
                this,
                getZoom().scaled(pt.x) + 20,
                getZoom().scaled(pt.y) + 30);
        }
    }
}
