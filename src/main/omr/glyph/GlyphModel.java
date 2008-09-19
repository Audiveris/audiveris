//----------------------------------------------------------------------------//
//                                                                            //
//                            G l y p h M o d e l                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.script.ScriptRecording;

import omr.selection.GlyphEvent;
import omr.selection.SelectionHint;

import omr.sheet.Sheet;

import omr.util.BasicTask;
import omr.util.Logger;
import omr.util.Synchronicity;
import static omr.util.Synchronicity.*;

import org.bushe.swing.event.EventService;

import java.util.*;

/**
 * Class <code>GlyphModel</code> is a common basis for glyph handling, used by
 * any user interface which needs to act on the actual glyph data.
 *
 * <p>Nota: Since it triggers updates of user selections, it is supposed to be
 * used from within a user action. If not, glyphs should be accessed directly
 * instead, through the 'setShape()' method in 'Glyph' class.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphModel.class);

    //~ Instance fields --------------------------------------------------------

    /** Underlying lag (vertical or horizontal) */
    protected final GlyphLag lag;

    /** Related Sheet */
    protected final Sheet sheet;

    /** Latest shape assigned if any */
    protected Shape latestShapeAssigned;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // GlyphModel //
    //------------//
    /**
     * Create an instance of GlyphModel, with its underlying glyph lag
     *
     * @param sheet the related sheet (can be null)
     * @param lag the related lag (cannot be null)
     */
    public GlyphModel (Sheet    sheet,
                       GlyphLag lag)
    {
        // Null sheet is allowed (for GlyphVerifier use)
        this.sheet = sheet;

        if (lag == null) {
            throw new IllegalArgumentException(
                "Attempt to create a GlyphModel with null underlying Lag");
        } else {
            this.lag = lag;
        }
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
        return lag.getGlyph(id);
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
        return lag;
    }

    //------------------------//
    // getLatestShapeAssigned //
    //------------------------//
    /**
     * Report the latest non null shape that was assigned, or null if none
     *
     * @return latest shape assigned, or null if none
     */
    public Shape getLatestShapeAssigned ()
    {
        return latestShapeAssigned;
    }

    //----------//
    // getSheet //
    //----------//
    public Sheet getSheet ()
    {
        return sheet;
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
    public EventService getLocationService ()
    {
        return getSheet()
                   .getEventService();
    }

    //------------------//
    // assignGlyphShape //
    //------------------//
    /**
     * Assign a Shape to a glyph
     *
     * @param processing specify whether we should run (a)synchronously
     * @param glyph the glyph to be assigned
     * @param shape the assigned shape, which may be null
     * @param record specify whether the action must be recorded in the script
     */
    public void assignGlyphShape (Synchronicity         processing,
                                  final Glyph           glyph,
                                  final Shape           shape,
                                  final ScriptRecording record)
    {
        if (processing == ASYNC) {
            new BasicTask() {
                    @Override
                    protected Void doInBackground ()
                        throws Exception
                    {
                        assignGlyphShape(SYNC, glyph, shape, record);

                        return null;
                    }
                }.execute();
        } else {
            if (glyph != null) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "shape " + shape + " assigned to glyph #" +
                        glyph.getId());
                }

                // First, do a manual assignment of the shape to the glyph
                glyph.setShape(shape, Evaluation.MANUAL_NO_DOUBT);

                // Remember the latest shape assigned
                if (shape != null) {
                    latestShapeAssigned = shape;
                }

                // Update immediately the glyph info as displayed
                if (sheet != null) {
                    lag.publish(
                        new GlyphEvent(
                            this,
                            SelectionHint.GLYPH_MODIFIED,
                            null,
                            glyph));
                }
            }
        }
    }

    //----------------//
    // assignSetShape //
    //----------------//
    /**
     * Assign a shape to the selected collection of glyphs.
     *
     * @param processing specify whether we should run (a)synchronously
     * @param glyphs the collection of glyphs to be assigned
     * @param shape the shape to be assigned
     * @param compound flag to build one compound, rather than assign each
     *                 individual glyph
     * @param record specify whether the action must be recorded in the script
     */
    public void assignSetShape (Synchronicity     processing,
                                Collection<Glyph> glyphs,
                                Shape             shape,
                                boolean           compound,
                                ScriptRecording   record)
    {
        // Empty by default
        logger.warning("No assignSetShape in current model for " + shape);
    }

    //--------------------//
    // deassignGlyphShape //
    //--------------------//
    /**
     * Deassign the shape of a glyph
     *
     * @param processing specify whether we should run (a)synchronously
     * @param glyph the glyph to deassign
     * @param record specify whether the action must be recorded in the script
     */
    public void deassignGlyphShape (Synchronicity   processing,
                                    Glyph           glyph,
                                    ScriptRecording record)
    {
        // Empty by default
        logger.warning(
            "No deassignGlyphShape in current model for " + glyph.getShape());
    }

    //------------------//
    // deassignSetShape //
    //------------------//
    /**
     * De-Assign a collection of glyphs.
     *
     * @param processing specify whether we should run (a)synchronously
     * @param glyphs the collection of glyphs to be de-assigned
     * @param record specify whether the action must be recorded in the script
     */
    public void deassignSetShape (Synchronicity     processing,
                                  Collection<Glyph> glyphs,
                                  ScriptRecording   record)
    {
        // Empty by default
        logger.warning("No deassignSetShape in current model");
    }
}
