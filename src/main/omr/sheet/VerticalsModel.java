//----------------------------------------------------------------------------//
//                                                                            //
//                        V e r t i c a l s M o d e l                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.Main;

import omr.check.CheckBoard;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphsModel;
import omr.glyph.Shape;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphLagView;

import omr.lag.RunBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;

import omr.log.Logger;

import omr.score.visitor.SheetPainter;

import omr.script.ScriptRecording;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.UserEvent;

import omr.sheet.ui.PixelBoard;

import omr.step.StepException;

import omr.stick.Stick;

import omr.ui.BoardsPane;

import omr.util.Synchronicity;

import org.bushe.swing.event.EventService;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Class <code>VerticalsModel</code> is in charge of handling (de)assignment
 * of vertical entities at sheet level
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class VerticalsModel
    extends GlyphsModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(VerticalsModel.class);

    /** Events this entity is interested in */
    private static final Collection<Class<?extends UserEvent>> eventClasses = new ArrayList<Class<?extends UserEvent>>();

    static {
        eventClasses.add(GlyphEvent.class);
    }

    //~ Instance fields --------------------------------------------------------

    /** Related user display if any */
    private MyView view;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // VerticalsModel //
    //----------------//
    /**
     * Creates a new VerticalsModel object.
     *
     * @param sheet the related sheet
     */
    public VerticalsModel (Sheet sheet)
    {
        // We work with the sheet vertical lag
        super(sheet, sheet.getVerticalLag());
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // deassignGlyphShape //
    //--------------------//
    /**
     * This method is limited to deassignment of stems
     *
     * @param glyph the glyph to deassign
     * @param record request to record this action in the script
     */
    @Override
    public void deassignGlyphShape (Synchronicity         processing,
                                    final Glyph           glyph,
                                    final ScriptRecording record)
    {
        Shape shape = glyph.getShape();

        switch (shape) {
        case COMBINING_STEM :
            sheet.getSymbolsModel()
                 .deassignGlyphShape(processing, glyph, record);

            break;

        default :
        }
    }

    //------------------//
    // deassignSetShape //
    //------------------//
    /**
     * This method is limited to deassignment of stems
     *
     * @param glyphs the collection of glyphs to be de-assigned
     * @param record true if this action is to be recorded in the script
     */
    @Override
    public void deassignSetShape (Synchronicity     processing,
                                  Collection<Glyph> glyphs,
                                  ScriptRecording   record)
    {
        sheet.getSymbolsModel()
             .deassignSetShape(processing, glyphs, record);
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the display if any, with proper colors for sections
     */
    public void refresh ()
    {
        if (Main.getGui() != null) {
            if ((view == null) && constants.displayFrame.getValue()) {
                displayFrame();
            } else if (view != null) {
                view.colorize();
                view.repaint();
            }
        }
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        // Specific rubber display
        view = new MyView(lag);
        view.colorize();

        // Create a hosting frame for the view
        final String unit = sheet.getRadix() + ":VerticalsBuilder";

        sheet.getAssembly()
             .addViewTab(
            "Verticals",
            new ScrollLagView(view),
            new BoardsPane(
                sheet,
                view,
                new PixelBoard(unit, sheet),
                new RunBoard(unit, lag),
                new SectionBoard(unit, lag.getLastVertexId(), lag),
                new GlyphBoard(unit, this, null),
                new MyCheckBoard(unit, lag.getEventService(), eventClasses)));
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean displayFrame = new Constant.Boolean(
            true,
            "Should we display a frame on the stem sticks");
    }

    //--------------//
    // MyCheckBoard //
    //--------------//
    private class MyCheckBoard
        extends CheckBoard<Stick>
    {
        //~ Constructors -------------------------------------------------------

        public MyCheckBoard (String                                unit,
                             EventService                          eventService,
                             Collection<Class<?extends UserEvent>> eventList)
        {
            super(unit, null, eventService, eventList);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void onEvent (UserEvent event)
        {
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                if (event instanceof GlyphEvent) {
                    GlyphEvent glyphEvent = (GlyphEvent) event;
                    Glyph      glyph = glyphEvent.getData();

                    if (glyph instanceof Stick) {
                        try {
                            Stick      stick = (Stick) glyph;
                            SystemInfo system = sheet.getSystemOf(stick);
                            // Get a fresh suite
                            setSuite(system.createStemCheckSuite(true));
                            tellObject(stick);
                        } catch (StepException ex) {
                            logger.warning("Glyph cannot be processed");
                        }
                    } else {
                        tellObject(null);
                    }
                }
            } catch (Exception ex) {
                logger.warning(getClass().getName() + " onEvent error", ex);
            }
        }
    }

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends GlyphLagView
    {
        //~ Constructors -------------------------------------------------------

        public MyView (GlyphLag lag)
        {
            super(lag, null, null, VerticalsModel.this, null);
            setName("VerticalsBuilder-MyView");
        }

        //~ Methods ------------------------------------------------------------

        //----------//
        // colorize //
        //----------//
        @Override
        public void colorize ()
        {
            super.colorize();

            // Use light gray color for past successful entities
            sheet.colorize(lag, viewIndex, Color.lightGray);

            // Use bright yellow color for recognized stems
            for (Glyph glyph : sheet.getActiveGlyphs()) {
                if (glyph.isStem()) {
                    Stick stick = (Stick) glyph;
                    stick.colorize(lag, viewIndex, Color.yellow);
                }
            }
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        public void renderItems (Graphics g)
        {
            // Render all physical info known so far
            sheet.accept(new SheetPainter(g));

            super.renderItems(g);
        }
    }
}
