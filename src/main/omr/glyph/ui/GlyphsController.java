//----------------------------------------------------------------------------//
//                                                                            //
//                      G l y p h s C o n t r o l l e r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.*;

import omr.log.Logger;

import omr.script.AssignTask;
import omr.script.BarlineTask;

import omr.selection.GlyphEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.SelectionHint;
import omr.selection.SelectionService;

import omr.sheet.Sheet;

import org.jdesktop.application.Task;

import java.util.*;

/**
 * Class <code>GlyphsController</code> is a common basis for glyph handling,
 * used by any user interface which needs to act on the actual glyph data.
 *
 * <p>There are two main methods in this class ({@link #asyncAssignGlyphs} and
 * {@link #asyncDeassignGlyphs}). They share common characteristics:
 * <ul>
 * <li>They are processed asynchronously</li>
 * <li>Their action is recorded in the sheet script</li>
 * <li>They update the following steps, if any</li>
 * </ul>
 *
 * <p>Since the bus of user selections is used, the methods of this class are
 * meant to be used from within a user action, otherwise you must use a direct
 * access to similar synchronous actions in the underlying {@link GlyphsModel}.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphsController
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        GlyphsController.class);

    //~ Instance fields --------------------------------------------------------

    /** Related model */
    protected final GlyphsModel model;

    /** Cached sheet */
    protected final Sheet sheet;

    /** All the controlled views */
    protected Set<GlyphLagView> views = new LinkedHashSet<GlyphLagView>();

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // GlyphsController //
    //------------------//
    /**
     * Create an instance of GlyphsController, with its underlying GlyphsModel
     * instance
     *
     * @param model the related glyphs model
     */
    public GlyphsController (GlyphsModel model)
    {
        this.model = model;

        sheet = model.getSheet();
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getGlyphById //
    //--------------//
    /**
     * Retrieve a glyph, knowing its id
     *
     * @param id the glyph id
     * @return the glyph found, or null if not
     */
    public Glyph getGlyphById (int id)
    {
        return model.getGlyphById(id);
    }

    //--------//
    // getLag //
    //--------//
    /**
     * Report the underlying glyph lag
     *
     * @return the related glyph lag
     */
    public GlyphLag getLag ()
    {
        return model.getLag();
    }

    //----------------//
    // setLatestShape //
    //----------------//
    /**
     * Assign the latest shape
     *
     * @param shape the latest shape
     */
    public void setLatestShapeAssigned (Shape shape)
    {
        model.setLatestShape(shape);
    }

    //----------------//
    // getLatestShape //
    //----------------//
    /**
     * Report the latest non null shape that was assigned, or null if none
     *
     * @return latest shape assigned, or null if none
     */
    public Shape getLatestShapeAssigned ()
    {
        return model.getLatestShape();
    }

    //--------------------//
    // getLocationService //
    //--------------------//
    /**
     * Report the event service to use for SheetLocationEvent
     * When no sheet is available, override this method to point to another
     * service
     * @return the event service to use for SheetLocationEvent
     */
    public SelectionService getLocationService ()
    {
        return model.getSheet()
                    .getSelectionService();
    }

    //-------------//
    // GlyphsModel //
    //-------------//
    /**
     * Report the underlying model
     * @return the underlying glpyhs model
     */
    public GlyphsModel getModel ()
    {
        return model;
    }

    //---------//
    // addView //
    //---------//
    public void addView (GlyphLagView view)
    {
        views.add(view);
    }

    //-------------------//
    // asyncAssignGlyphs //
    //-------------------//
    /**
     * Asynchronouly assign a shape to the selected collection of glyphs and
     * record this action in the script
     *
     * @param glyphs the collection of glyphs to be assigned
     * @param shape the shape to be assigned
     * @param compound flag to build one compound, rather than assign each
     *                 individual glyph
     * @return the task that carries out the processing
     */
    public Task asyncAssignGlyphs (final Collection<Glyph> glyphs,
                                   final Shape             shape,
                                   final boolean           compound)
    {
        if (ShapeRange.Barlines.contains(shape) || hasBars(glyphs)) {
            // Special case for barlines
            return new BarlineTask(shape, compound, glyphs).launch(sheet);
        } else {
            // Normal symbol processing
            return new AssignTask(
                shape,
                compound,
                glyphs,
                getLag().getOrientation()).launch(sheet);
        }
    }

    //---------------------//
    // asyncDeassignGlyphs //
    //---------------------//
    /**
     * Asynchronously de-Assign a collection of glyphs and record this action
     * in the script
     *
     * @param glyphs the collection of glyphs to be de-assigned
     * @return the task that carries out the processing
     */
    public Task asyncDeassignGlyphs (final Collection<Glyph> glyphs)
    {
        return asyncAssignGlyphs(glyphs, null, false);
    }

    //------------//
    // removeView //
    //------------//
    public void removeView (GlyphLagView view)
    {
        views.remove(view);
    }

    //------------//
    // syncAssign //
    //------------//
    /**
     * Process synchronously the assignment defined in the provided context
     * @param context the context of the assignment
     */
    public void syncAssign (AssignTask context)
    {
        final boolean compound = context.isCompound();
        final Shape   shape = context.getAssignedShape();

        if (logger.isFineEnabled()) {
            logger.fine("syncAssign " + context + " compound:" + compound);
        }

        Set<Glyph> glyphs = context.getInitialGlyphs();

        if (shape != null) { // Assignment
            model.assignGlyphs(
                glyphs,
                context.getAssignedShape(),
                compound,
                Evaluation.MANUAL);

            // Publish modifications
            if (compound) {
                publish(
                    glyphs.iterator().next().getMembers().first().getGlyph());
            } else {
                Glyph glyph = glyphs.iterator()
                                    .next();

                if (glyph != null) {
                    publish(glyph.getMembers().first().getGlyph());
                }
            }
        } else { // Deassignment
            model.deassignGlyphs(glyphs);

            // Publish modifications
            publish(glyphs.iterator().next());
        }
    }

    //---------//
    // hasBars //
    //---------//
    /**
     * Check whether the collection of glyphs contains at least one barline
     * @param glyphs the collection to check
     * @return true if one or several glyphs are barlines components
     */
    protected boolean hasBars (Collection<Glyph> glyphs)
    {
        // Do we have at least one bar?
        for (Glyph glyph : glyphs) {
            if (glyph.isBar()) {
                return true;
            }
        }

        return false;
    }

    //---------//
    // publish //
    //---------//
    protected void publish (Glyph glyph)
    {
        // Update immediately the glyph info as displayed
        if (model.getSheet() != null) {
            getLag()
                .getSelectionService()
                .publish(
                new GlyphEvent(this, SelectionHint.GLYPH_MODIFIED, null, glyph));
        }
    }

    //---------//
    // publish //
    //---------//
    protected void publish (Set<Glyph> glyphs)
    {
        // Update immediately the glyph info as displayed
        if (model.getSheet() != null) {
            getLag()
                .getSelectionService()
                .publish(
                new GlyphSetEvent(
                    this,
                    SelectionHint.GLYPH_MODIFIED,
                    null,
                    glyphs));
        }
    }

    //--------------//
    // refreshViews //
    //--------------//
    protected void refreshViews ()
    {
        for (GlyphLagView view : views) {
            view.colorizeAllGlyphs();
            view.repaint();
        }
    }
}
