//----------------------------------------------------------------------------//
//                                                                            //
//                      G l y p h s C o n t r o l l e r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Evaluation;
import omr.glyph.Glyphs;
import omr.glyph.GlyphsModel;
import omr.glyph.Nest;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.script.AssignTask;
import omr.script.BarlineTask;
import omr.script.DeleteTask;

import omr.selection.GlyphEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.SelectionHint;
import omr.selection.SelectionService;

import omr.sheet.Sheet;

import org.jdesktop.application.Task;

import java.util.Collection;
import java.util.Set;

/**
 * Class {@code GlyphsController} is a common basis for glyph handling,
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
 * @author Hervé Bitteur
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

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // GlyphsController //
    //------------------//
    /**
     * Create an instance of GlyphsController, with its underlying
     * GlyphsModel instance.
     * @param model the related glyphs model
     */
    public GlyphsController (GlyphsModel model)
    {
        this.model = model;

        sheet = model.getSheet();
    }

    //~ Methods ----------------------------------------------------------------

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
    public Task asyncAssignGlyphs (Collection<Glyph> glyphs,
                                   Shape             shape,
                                   boolean           compound)
    {
        // Safety check: we cannot alter virtual glyphs
        for (Glyph glyph : glyphs) {
            if (glyph.isVirtual()) {
                logger.warning("Cannot alter VirtualGlyph#" + glyph.getId());

                return null;
            }
        }

        if (ShapeSet.Barlines.contains(shape) ||
            Glyphs.containsBarline(glyphs)) {
            // Special case for barlines
            return new BarlineTask(sheet, shape, compound, glyphs).launch(
                sheet);
        } else {
            // Normal symbol processing
            return new AssignTask(sheet, shape, compound, glyphs).launch(sheet);
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
    public Task asyncDeassignGlyphs (Collection<Glyph> glyphs)
    {
        return asyncAssignGlyphs(glyphs, null, false);
    }

    //--------------------------//
    // asyncDeleteVirtualGlyphs //
    //--------------------------//
    public Task asyncDeleteVirtualGlyphs (Collection<Glyph> glyphs)
    {
        return new DeleteTask(sheet, glyphs).launch(sheet);
    }

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
     * Report the event service to use for LocationEvent
     * When no sheet is available, override this method to point to another
     * service
     * @return the event service to use for LocationEvent
     */
    public SelectionService getLocationService ()
    {
        return model.getSheet()
                    .getLocationService();
    }

    //----------//
    // getModel //
    //----------//
    /**
     * Report the underlying model
     * @return the underlying glpyhs model
     */
    public GlyphsModel getModel ()
    {
        return model;
    }

    //---------//
    // getNest //
    //---------//
    /**
     * Report the underlying glyph nest
     * @return the related glyph nest
     */
    public Nest getNest ()
    {
        return model.getNest();
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
                             // Persistent?
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

    //------------//
    // syncDelete //
    //------------//
    /**
     * Process synchronously the deletion defined in the provided context
     * @param context the context of the deletion
     */
    public void syncDelete (DeleteTask context)
    {
        if (logger.isFineEnabled()) {
            logger.fine("syncDelete" + context);
        }

        model.deleteGlyphs(context.getInitialGlyphs());

        publish((Glyph) null);
    }

    //---------//
    // publish //
    //---------//
    protected void publish (Glyph glyph)
    {
        // Update immediately the glyph info as displayed
        if (model.getSheet() != null) {
            getNest()
                .getGlyphService()
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
            getNest()
                .getGlyphService()
                .publish(
                new GlyphSetEvent(
                    this,
                    SelectionHint.GLYPH_MODIFIED,
                    null,
                    glyphs));
        }
    }
}
