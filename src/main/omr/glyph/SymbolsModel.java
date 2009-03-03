//----------------------------------------------------------------------------//
//                                                                            //
//                          S y m b o l s M o d e l                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.glyph.text.Sentence;
import omr.glyph.text.TextType;

import omr.log.Logger;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.Step;

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
 * @author Herv&eacute Bitteur
 * @version $Id$
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
        super(sheet, lag, Step.SYMBOLS);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // assignGlyph //
    //-------------//
    /**
     * Assign a Shape to a glyph
     *
     * @param glyph the glyph to be assigned
     * @param shape the assigned shape, which may be null
     * @param doubt the doubt about shape (Evaluation.MANUAL_NO_DOUBT?)
     */
    @Override
    public void assignGlyph (Glyph  glyph,
                             Shape  shape,
                             double doubt)
    {
        if (glyph == null) {
            return;
        }

        // Test on glyph weight (noise-like)
        // To prevent to assign a non-noise shape to a noise glyph
        if ((shape == Shape.NOISE) || Evaluator.isBigEnough(glyph)) {
            // Force a recomputation of glyph parameters
            // (since environment may have changed since the time they
            // have been computed)
            SystemInfo system = sheet.getSystemOf(glyph);

            if (system != null) {
                system.computeGlyphFeatures(glyph);
                super.assignGlyph(glyph, shape, doubt);
            }
        }
    }

    //------------//
    // assignText //
    //------------//
    /**
     * Assign a collection of glyphs as textual element
     * @param glyphs the collection of glyphs
     * @param textType the text role
     * @param textContent the ascii content
     * @param doubt the doubt wrt this assignment
     */
    public void assignText (Collection<Glyph> glyphs,
                            TextType          textType,
                            String            textContent,
                            double            doubt)
    {
        // Do the job
        for (Glyph glyph : glyphs) {
            // Assign text type
            Sentence sentence = glyph.getTextInfo()
                                     .getSentence();

            if (sentence != null) {
                sentence.setTextType(textType);
            }

            // Assign text only if it is not empty
            if ((textContent != null) && (textContent.length() > 0)) {
                glyph.getTextInfo()
                     .setManualContent(textContent);
            }
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
            assignGlyphSet(slurs, Shape.SLUR, false, Evaluation.MANUAL);
        }
    }

    //-----------------//
    // segmentGlyphSet //
    //-----------------//
    public void segmentGlyphSet (Collection<Glyph> glyphs,
                                 boolean           isShort)
    {
        deassignGlyphSet(glyphs);

        for (Glyph glyph : new ArrayList<Glyph>(glyphs)) {
            SystemInfo system = sheet.getSystemOf(glyph);
            system.segmentGlyphOnStems(glyph, isShort);
        }
    }
}
