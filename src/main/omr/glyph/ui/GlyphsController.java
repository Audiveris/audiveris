//----------------------------------------------------------------------------//
//                                                                            //
//                      G l y p h s C o n t r o l l e r                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.*;

import omr.log.Logger;

import omr.script.AssignTask;
import omr.script.DeassignTask;

import omr.selection.GlyphEvent;
import omr.selection.SelectionHint;
import omr.selection.SelectionService;

import omr.sheet.Sheet;

import omr.util.BasicTask;

import org.jdesktop.application.Task;

import java.util.*;

/**
 * Class <code>GlyphsController</code> is a common basis for glyph handling,
 * used by any user interface which needs to act on the actual glyph data.
 *
 * <p>There are two main methods in this class ({@link #asyncAssignGlyphSet} and
 * {@link #asyncDeassignGlyphSet}). They share common characteristics:
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

    /** The controlled views */
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

    //---------//
    // addView //
    //---------//
    public void addView (GlyphLagView view)
    {
        views.add(view);
    }

    //---------------------//
    // asyncAssignGlyphSet //
    //---------------------//
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
    public Task asyncAssignGlyphSet (final Collection<Glyph> glyphs,
                                     final Shape             shape,
                                     final boolean           compound)
    {
        // Special case for bars, delegate to SystemsBuilder if needed
        if ((Shape.Barlines.contains(shape) || hasBars(glyphs)) &&
            (getModel() != sheet.getSystemsBuilder()
                                .getController()
                                .getModel())) {
            return sheet.getSystemsBuilder()
                        .getController()
                        .asyncAssignGlyphSet(glyphs, shape, compound);
        } else {
            return launch(
                new AssignTask(shape, compound, glyphs),
                glyphs,
                new GlyphsRunnable() {
                        public Collection<Glyph> run ()
                        {
                            return syncAssignGlyphSet(glyphs, shape, compound);
                        }
                    });
        }
    }

    //-----------------------//
    // asyncDeassignGlyphSet //
    //-----------------------//
    /**
     * Asynchronously de-Assign a collection of glyphs and record this action
     * in the script
     *
     * @param glyphs the collection of glyphs to be de-assigned
     * @return the task that carries out the processing
     */
    public Task asyncDeassignGlyphSet (final Collection<Glyph> glyphs)
    {
        // Special case for bars, delegate to SystemsBuilder if needed
        if (hasBars(glyphs) &&
            (getModel() != sheet.getSystemsBuilder()
                                .getController()
                                .getModel())) {
            return sheet.getSystemsBuilder()
                        .getController()
                        .asyncDeassignGlyphSet(glyphs);
        } else {
            return launch(
                new DeassignTask(glyphs),
                glyphs,
                new GlyphsRunnable() {
                        public Collection<Glyph> run ()
                        {
                            return syncDeassignGlyphSet(glyphs);
                        }
                    });
        }
    }

    //------------//
    // removeView //
    //------------//
    public void removeView (GlyphLagView view)
    {
        views.remove(view);
    }

    //-------------//
    // GlyphsModel //
    //-------------//
    /**
     * Report the underlying model
     * @return the underlying glpyhs model
     */
    protected GlyphsModel getModel ()
    {
        return model;
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

    //--------//
    // launch //
    //--------//
    /**
     * Launch an asynchronous task on glyphs
     * @param scriptTask the sheet task to be registered in the script
     * @param glyphs the set of glyphs concerned by the action
     * @param runnable the (synchronous) job
     * @return the launched Swing Application Framework task
     */
    protected Task launch (final omr.script.Task   scriptTask,
                           final Collection<Glyph> glyphs,
                           final GlyphsRunnable    runnable)
    {
        Task task = new BasicTask() {
            protected Void doInBackground ()
                throws Exception
            {
                // Record this task into the sheet script
                sheet.getScript()
                     .addTask(scriptTask);

                // Remember impacted glyphs & their related shapes
                Set<Shape> shapes = Glyph.shapesOf(glyphs);

                // Do the job & Update following steps
                sheet.rebuildAfter(
                    model.getRelatedStep(),
                    runnable.run(),
                    shapes);

                // Refresh the controlled glyphlag views
                refreshViews();

                return null;
            }
        };

        task.execute();

        return task;
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

    //--------------------//
    // syncAssignGlyphSet //
    //--------------------//
    /**
     * Synchronously assign a shape to the selected collection of glyphs and
     * record this action in the script
     *
     * @param glyphs the collection of glyphs to be assigned
     * @param shape the shape to be assigned
     * @param compound flag to build one compound, rather than assign each
     *                 individual glyph
     * @return the collection of impacted glyphs, or null if all systems need
     * to be updated in the following steps
     */
    protected Collection<Glyph> syncAssignGlyphSet (Collection<Glyph> glyphs,
                                                    Shape             shape,
                                                    boolean           compound)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "syncAssignGlyphSet " + (compound ? "compound " : "") +
                Glyph.toString(glyphs) + " to " + shape);
        }

        model.assignGlyphSet(glyphs, shape, compound, Evaluation.MANUAL);

        // Publish modifications
        Glyph firstGlyph = glyphs.iterator()
                                 .next();
        publish(compound ? firstGlyph.getPartOf() : firstGlyph);

        return glyphs;
    }

    //----------------------//
    // syncDeassignGlyphSet //
    //----------------------//
    /**
     * {@inheritDoc}
     */
    protected Collection<Glyph> syncDeassignGlyphSet (Collection<Glyph> glyphs)
    {
        if (logger.isFineEnabled()) {
            logger.fine("syncDeassignGlyphSet " + Glyph.toString(glyphs));
        }

        model.deassignGlyphSet(glyphs);

        // Publish modifications
        publish(glyphs.iterator().next());

        return glyphs;
    }

    //~ Inner Interfaces -------------------------------------------------------

    //----------------//
    // GlyphsRunnable //
    //----------------//
    /**
     * Describes the processing done on a set of glyphs
     */
    protected static interface GlyphsRunnable
    {
        //~ Methods ------------------------------------------------------------

        /**
         * Do the glyphs processing
         * @return the collection of glyphs used to limit the update of
         * following steps, a null value indicates no limitation so that all
         * systems get updated.
         */
        Collection<Glyph> run ();
    }
}
