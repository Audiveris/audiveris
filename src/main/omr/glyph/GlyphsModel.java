//----------------------------------------------------------------------------//
//                                                                            //
//                           G l y p h s M o d e l                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.lag.Section;

import omr.log.Logger;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.Step;

import java.util.*;

/**
 * Class <code>GlyphsModel</code> is a common model for synchronous glyph
 * handling.
 *
 * <p>Nota: User gesture should trigger actions in GlyphsController which will
 * asynchronously delegate to this model.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphsModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphsModel.class);

    //~ Instance fields --------------------------------------------------------

    /** Underlying lag (vertical or horizontal) */
    protected final GlyphLag lag;

    /** Related Sheet */
    protected final Sheet sheet;

    /** Related Step */
    protected final Step step;

    /** Latest shape assigned if any */
    protected Shape latestShape;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // GlyphsModel //
    //-------------//
    /**
     * Create an instance of GlyphsModel, with its underlying glyph lag
     *
     * @param sheet the related sheet (can be null)
     * @param lag the related lag (cannot be null)
     * @param step the step after which update should be perform (can be null)
     */
    public GlyphsModel (Sheet    sheet,
                        GlyphLag lag,
                        Step     step)
    {
        // Null sheet is allowed (for GlyphVerifier use)
        this.sheet = sheet;

        if (lag == null) {
            throw new IllegalArgumentException(
                "Attempt to create a GlyphsModel with null underlying Lag");
        } else {
            this.lag = lag;
        }

        this.step = step;
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

    //----------------//
    // getLatestShape //
    //----------------//
    /**
     * Report the latest non null shape that was assigned, or null if none
     *
     * @return latest shape assigned, or null if none
     */
    public Shape getLatestShape ()
    {
        return latestShape;
    }

    //----------------//
    // getRelatedStep //
    //----------------//
    /**
     * Report the step this GlyphsModel is used for, so that we know from which
     * step updates must be propagated (we have to update the steps that follow
     * this one)
     * @return the step related to this glyphs model
     */
    public Step getRelatedStep ()
    {
        return step;
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the model underlying sheet
     * @return the unerlying sheet instance
     */
    public Sheet getSheet ()
    {
        return sheet;
    }

    //-------------//
    // assignGlyph //
    //-------------//
    /**
     * Assign a Shape to a glyph
     *
     * @param glyph the glyph to be assigned
     * @param shape the assigned shape, which may be null
     * @param doubt the doubt about shape (Evaluation.MANUAL?)
     */
    public void assignGlyph (Glyph  glyph,
                             Shape  shape,
                             double doubt)
    {
        if (glyph == null) {
            return;
        }

        // Do a manual assignment of the shape to the glyph
        glyph.setShape(shape, doubt);

        if (shape != null) {
            boolean isTransient = glyph.getId() == 0;

            // If this is a transient glyph, insert it
            if (isTransient) {
                SystemInfo system = sheet.getSystemOf(glyph);
                glyph = system.addGlyph(glyph);
                logger.info(
                    "Inserted compound #" + glyph.getId() + " as " + shape);
            }

            logger.info(
                "Assign " + (isTransient ? "transient " : "") + "glyph#" +
                glyph.getId() + " to " + shape);

            // Remember the latest shape assigned
            latestShape = shape;
        }
    }

    //----------------//
    // assignGlyphSet //
    //----------------//
    /**
     * Assign a shape to the selected collection of glyphs.
     *
     * @param glyphs the collection of glyphs to be assigned
     * @param shape the shape to be assigned
     * @param compound flag to build one compound, rather than assign each
     *                 individual glyph
     * @param doubt the doubt we have wrt the assigned shape
     */
    public void assignGlyphSet (Collection<Glyph> glyphs,
                                Shape             shape,
                                boolean           compound,
                                double            doubt)
    {
        if (compound) {
            // Build & insert one compound
            SystemInfo system = sheet.getSystemOf(glyphs);
            Glyph      glyph = system.buildCompound(glyphs);
            assignGlyph(glyph, shape, doubt);
        } else {
            // Assign each glyph individually
            for (Glyph glyph : new ArrayList<Glyph>(glyphs)) {
                if (glyph.getShape() != Shape.NOISE) {
                    assignGlyph(glyph, shape, doubt);
                }
            }
        }
    }

    //------------------//
    // assignSectionSet //
    //------------------//
    /**
     * Assign a shape to the selected collection of sections.
     *
     * @param sections the collection of sections to be aggregated as a glyph
     * @param shape the shape to be assigned
     * @param doubt the doubt we have wrt the assigned shape
     */
    public void assignSectionSet (Collection<GlyphSection> sections,
                                  Shape               shape,
                                  double              doubt)
    {
        // Build & insert one glyph out of the sections
        SystemInfo system = sheet.getSystemOfSections(sections);
        Glyph      glyph = system.buildGlyph(sections);
        assignGlyph(glyph, shape, doubt);
    }

    //---------------//
    // deassignGlyph //
    //---------------//
    /**
     * Deassign the shape of a glyph
     *
     * @param glyph the glyph to deassign
     */
    public void deassignGlyph (Glyph glyph)
    {
        // Assign the null shape to the glyph
        assignGlyph(glyph, null, Evaluation.ALGORITHM);
    }

    //------------------//
    // deassignGlyphSet //
    //------------------//
    /**
     * De-Assign a collection of glyphs.
     *
     * @param glyphs the collection of glyphs to be de-assigned
     */
    public void deassignGlyphSet (Collection<Glyph> glyphs)
    {
        for (Glyph glyph : new ArrayList<Glyph>(glyphs)) {
            deassignGlyph(glyph);
        }
    }
}
