//----------------------------------------------------------------------------//
//                                                                            //
//                            G l y p h M o d e l                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.util.Logger;

import java.util.List;

/**
 * Class <code>GlyphModel</code> is a common basis for glyph handling.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class GlyphModel
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(GlyphModel.class);

    //~ Instance fields --------------------------------------------------------

    /** Underlying lag (vertical or horizontal) */
    protected final GlyphLag lag;

    /** Latest shape assigned if any */
    protected Shape latestShapeAssigned;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // GlyphModel //
    //------------//
    /**
     * Create an instance of GlyphModel, with its underlying glyph lag
     *
     * @param lag the related lag
     */
    public GlyphModel (GlyphLag lag)
    {
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

    //------------------//
    // assignGlyphShape //
    //------------------//
    /**
     * Assign a Shape to a glyph
     *
     * @param glyph the glyph to be assigned
     * @param shape the assigned shape, which may be null
     */
    public void assignGlyphShape (Glyph glyph,
                                  Shape shape)
    {
        // Empty by default
        logger.warning("assignGlyphShape not implemented for this glyph model");
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
     */
    public void assignSetShape (List<Glyph> glyphs,
                                Shape       shape,
                                boolean     compound)
    {
        // Empty by default
        logger.warning("assignSetShape not implemented for this glyph model");
    }

    //--------------------//
    // deassignGlyphShape //
    //--------------------//
    /**
     * Deassign the shape of a glyph
     *
     * @param glyph
     */
    public void deassignGlyphShape (Glyph glyph)
    {
        // Empty by default
        logger.warning("deassignGlyphShape not implemented for this glyph model");
    }

    //------------------//
    // deassignSetShape //
    //------------------//
    /**
     * De-Assign a collection of glyphs.
     *
     * @param glyphs the collection of glyphs to be de-assigned
     */
    public void deassignSetShape (List<Glyph> glyphs)
    {
        // Empty by default
        logger.warning("deassignSetShape not implemented for this glyph model");
    }
}
