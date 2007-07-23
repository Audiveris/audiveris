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

import omr.selection.SelectionHint;
import static omr.selection.SelectionTag.*;

import omr.sheet.Sheet;

import omr.util.Logger;

import java.util.*;

/**
 * Class <code>GlyphModel</code> is a common basis for glyph handling.
 *
 * <dl>
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>*_GLYPH (flagged with GLYPH_INIT hint)
 * </ul>
 * </dl>
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

    //------------------//
    // assignGlyphShape //
    //------------------//
    /**
     * Assign a Shape to a glyph
     *
     * @param glyph the glyph to be assigned
     * @param shape the assigned shape, which may be null
     * @param record true if this action is to be recorded in the script
     */
    public void assignGlyphShape (Glyph   glyph,
                                  Shape   shape,
                                  boolean record)
    {
        if (glyph != null) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "shape " + shape + " assigned to glyph #" + glyph.getId());
            }

            // First, do a manual assignment of the shape to the glyph
            glyph.setShape(shape, Evaluation.MANUAL_NO_DOUBT);

            // Remember the latest shape assigned
            if (shape != null) {
                latestShapeAssigned = shape;
            }

            // Update immediately the glyph info as displayed
            if (sheet != null) {
                sheet.getSelection(
                    lag.isVertical() ? VERTICAL_GLYPH : HORIZONTAL_GLYPH)
                     .setEntity(glyph, SelectionHint.GLYPH_MODIFIED);
            }
        }
    }

    //----------------//
    // assignSetShape //
    //----------------//
    /**
     * Assign a shape to the selected collection of glyphs.
     *
     * @param glyphs the collection of glyphs to be assigned
     * @param shape the shape to be assigned
     * @param compound flag to build one compound, rather than assign each
     *                 individual glyph
     * @param record true if this action is to be recorded in the script
     */
    public void assignSetShape (Collection<Glyph> glyphs,
                                             Shape             shape,
                                             boolean           compound,
                                             boolean           record)
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
     * @param glyph the glyph to deassign
     * @param record true if this action is to be recorded in the script
     */
    public void deassignGlyphShape (Glyph   glyph,
                                    boolean record)
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
     * @param glyphs the collection of glyphs to be de-assigned
     * @param record true if this action is to be recorded in the script
     */
    public void deassignSetShape (Collection<Glyph> glyphs,
                                  boolean           record)
    {
        // Empty by default
        logger.warning("No deassignSetShape in current model");
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
}
