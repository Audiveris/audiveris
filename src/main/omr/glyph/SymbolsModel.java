//----------------------------------------------------------------------------//
//                                                                            //
//                          S y m b o l s M o d e l                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;
import omr.glyph.text.TextInfo;
import omr.glyph.text.TextRole;

import omr.log.Logger;

import omr.score.entity.Text.CreatorText.CreatorType;
import omr.score.entity.TimeRational;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.Steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class <code>SymbolsModel</code> is a GlyphsModel specifically meant for
 * symbol glyphs
 *
 * @author Hervé Bitteur
 */
public class SymbolsModel
    extends GlyphsModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SymbolsModel.class);

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // SymbolsModel //
    //--------------//
    /**
     * Creates a new SymbolsModel object.
     *
     * @param sheet the related sheet (can be null)
     * @param lag the related lag (cannot be null)
     */
    public SymbolsModel (Sheet    sheet,
                         GlyphLag lag)
    {
        super(sheet, lag, Steps.valueOf(Steps.SYMBOLS));
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // assignText //
    //------------//
    /**
     * Assign a collection of glyphs as textual element
     * @param glyphs the collection of glyphs
     * @param textType Creator type if relevant
     * @param textRole the text role
     * @param textContent the ascii content
     * @param doubt the doubt wrt this assignment
     */
    public void assignText (Collection<Glyph> glyphs,
                            CreatorType       textType,
                            TextRole          textRole,
                            String            textContent,
                            double            doubt)
    {
        // Do the job
        for (Glyph glyph : glyphs) {
            TextInfo info = glyph.getTextInfo();

            // Assign creator type?
            if (textRole == TextRole.Creator) {
                info.setCreatorType(textType);
            }

            // Assign text role
            info.setTextRole(textRole);

            // Assign text only if it is not empty
            if ((textContent != null) && (textContent.length() > 0)) {
                info.setManualContent(textContent);
            }
        }
    }

    //--------------------//
    // assignTimeRational //
    //--------------------//
    /**
     * Assign a time rational value to collection of glyphs
     * @param glyphs the collection of glyphs
     * @param timeRational the time rational value
     * @param doubt the doubt wrt this assignment
     */
    public void assignTimeRational (Collection<Glyph> glyphs,
                                    TimeRational      timeRational,
                                    double            doubt)
    {
        // Do the job
        for (Glyph glyph : glyphs) {
            glyph.setTimeRational(timeRational);
        }
    }

    //-------------//
    // cancelStems //
    //-------------//
    /**
     * Cancel one or several stems, turning them back to just a set of sections,
     * and rebuilding glyphs from their member sections together with the
     * neighbouring non-assigned sections
     *
     * @param stems a list of stems
     */
    public void cancelStems (List<Glyph> stems)
    {
        /**
         * To remove a stem, several infos need to be modified : shape from
         * COMBINING_STEM to null, result from STEM to null, and the Stem must
         * be removed from system list of stems.
         *
         * The stem glyph must be removed (as well as all other non-recognized
         * glyphs that are connected to the former stem)
         *
         * Then, re-glyph extraction from sections when everything is ready
         * (GlyphBuilder). Should work on a micro scale : just the former stem
         * and the neighboring (non-assigned) glyphs.
         */
        Set<SystemInfo> impactedSystems = new HashSet<SystemInfo>();

        for (Glyph stem : stems) {
            SystemInfo system = sheet.getSystemOf(stem);
            system.removeGlyph(stem);
            super.deassignGlyph(stem);
            impactedSystems.add(system);
        }

        // Extract brand new glyphs from impactedSystems
        for (SystemInfo system : impactedSystems) {
            system.extractNewGlyphs();
        }
    }

    //---------------//
    // deassignGlyph //
    //---------------//
    /**
     * Deassign the shape of a glyph. This overrides the basic deassignment, in
     * order to delegate the handling of some specific shapes.
     *
     * @param glyph the glyph to deassign
     */
    @Override
    public void deassignGlyph (Glyph glyph)
    {
        // Safer
        if (glyph.getShape() == null) {
            return;
        }

        // Processing depends on shape at hand
        switch (glyph.getShape()) {
        case COMBINING_STEM :

            if (logger.isFineEnabled()) {
                logger.fine("Deassigning a Stem as glyph " + glyph.getId());
            }

            cancelStems(Collections.singletonList(glyph));

            break;

        case NOISE :
            logger.info("Skipping Noise as glyph " + glyph.getId());

            break;

        default :
            super.deassignGlyph(glyph);

            break;
        }
    }

    //---------------//
    // fixLargeSlurs //
    //---------------//
    public void fixLargeSlurs (Collection<Glyph> glyphs)
    {
        List<Glyph> slurs = new ArrayList<Glyph>();

        for (Glyph glyph : new ArrayList<Glyph>(glyphs)) {
            SystemInfo system = sheet.getSystemOf(glyph);
            Glyph      slur = system.fixLargeSlur(glyph);

            if (slur != null) {
                slurs.add(slur);
            }
        }

        if (!slurs.isEmpty()) {
            assignGlyphs(slurs, Shape.SLUR, false, Evaluation.MANUAL);
        }
    }

    //---------------//
    // segmentGlyphs //
    //---------------//
    public void segmentGlyphs (Collection<Glyph> glyphs,
                               boolean           isShort)
    {
        deassignGlyphs(glyphs);

        for (Glyph glyph : new ArrayList<Glyph>(glyphs)) {
            SystemInfo system = sheet.getSystemOf(glyph);
            system.segmentGlyphOnStems(glyph, isShort);
        }
    }

    //-------------//
    // assignGlyph //
    //-------------//
    /**
     * Assign a Shape to a glyph, inserting the glyph to its containing system
     * and lag if it is still transient
     *
     * @param glyph the glyph to be assigned
     * @param shape the assigned shape, which may be null
     * @param doubt the doubt about shape
     */
    @Override
    protected Glyph assignGlyph (Glyph  glyph,
                                 Shape  shape,
                                 double doubt)
    {
        if (glyph == null) {
            return null;
        }

        // Test on glyph weight (noise-like)
        // To prevent to assign a non-noise shape to a noise glyph
        if ((shape == Shape.NOISE) || GlyphEvaluator.isBigEnough(glyph)) {
            // Force a recomputation of glyph parameters
            // (since environment may have changed since the time they
            // have been computed)
            SystemInfo system = sheet.getSystemOf(glyph);

            if (system != null) {
                system.computeGlyphFeatures(glyph);

                return super.assignGlyph(glyph, shape, doubt);
            }
        }

        return glyph;
    }
}
