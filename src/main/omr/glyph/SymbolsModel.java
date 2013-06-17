//----------------------------------------------------------------------------//
//                                                                            //
//                          S y m b o l s M o d e l                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

import omr.score.entity.TimeRational;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.Steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import omr.text.TextBuilder;
import omr.text.TextLine;
import omr.text.TextRole;
import omr.text.TextRoleInfo;
import omr.text.TextWord;

/**
 * Class {@code SymbolsModel} is a GlyphsModel specifically meant for
 * symbol glyphs.
 *
 * @author Hervé Bitteur
 */
public class SymbolsModel
        extends GlyphsModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(SymbolsModel.class);

    //~ Instance fields --------------------------------------------------------
    /** Standard evaluator */
    private ShapeEvaluator evaluator = GlyphNetwork.getInstance();

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // SymbolsModel //
    //--------------//
    /**
     * Creates a new SymbolsModel object.
     *
     * @param sheet the related sheet
     */
    public SymbolsModel (Sheet sheet)
    {
        super(sheet, sheet.getNest(), Steps.valueOf(Steps.SYMBOLS));
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // assignText //
    //------------//
    /**
     * Assign a collection of glyphs as textual element
     *
     * @param glyphs      the collection of glyphs
     * @param roleInfo    the text role
     * @param textContent the ascii content
     * @param grade       the grade wrt this assignment
     */
    public void assignText (Collection<Glyph> glyphs,
                            TextRoleInfo roleInfo,
                            String textContent,
                            double grade)
    {
        // Do the job
        for (Glyph glyph : glyphs) {
            SystemInfo system = glyph.getSystem();
            String language = system.getSheet().getPage().getTextParam().getTarget();
            TextBuilder textBuilder = system.getTextBuilder();
            TextWord word = glyph.getTextWord();
            List<TextLine> lines = new ArrayList<>();

            if (word == null) {
                word = TextWord.createManualWord(glyph, textContent);
                glyph.setTextWord(language, word);
                TextLine line = new TextLine(system, Arrays.asList(word));
                lines = Arrays.asList(line);
                lines = textBuilder.recomposeLines(lines);
                system.getSentences().remove(line);
                system.getSentences().addAll(lines);
            } else if (word.getTextLine() != null) {
                lines = Arrays.asList(word.getTextLine());
            }

            // Force text role
            glyph.setManualRole(roleInfo);
            for (TextLine line : lines) {
                // For Chord role, we don't spread the role to other words
                // but rather trigger a line split
                if (roleInfo.role == TextRole.Chord
                    && line.getWords().size() > 1) {
                    line.setRole(roleInfo);
                    List<TextLine> subLines = textBuilder.recomposeLines(
                            Arrays.asList(line));
                    system.getSentences().remove(line);
                    for (TextLine l : subLines) {
                        if (!l.getWords().contains(word)) {
                            l.setRole(null);
                        }
                    }
                    system.getSentences().addAll(subLines);
                } else {
                    line.setRole(roleInfo);
                }
            }

            // Force text only if it is not empty
            if ((textContent != null) && (textContent.length() > 0)) {
                glyph.setManualValue(textContent);
            }
        }
    }

    //--------------------//
    // assignTimeRational //
    //--------------------//
    /**
     * Assign a time rational value to collection of glyphs
     *
     * @param glyphs       the collection of glyphs
     * @param timeRational the time rational value
     * @param grade        the grade wrt this assignment
     */
    public void assignTimeRational (Collection<Glyph> glyphs,
                                    TimeRational timeRational,
                                    double grade)
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
         * STEM to null, result from STEM to null, and the Stem must
         * be removed from system list of stems.
         *
         * The stem glyph must be removed (as well as all other non-recognized
         * glyphs that are connected to the former stem)
         *
         * Then, re-glyph extraction from sections when everything is ready
         * (GlyphBuilder). Should work on a micro scale : just the former stem
         * and the neighboring (non-assigned) glyphs.
         */
        Set<SystemInfo> impactedSystems = new HashSet<>();

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
     * Deassign the shape of a glyph.
     * This overrides the basic deassignment, in order to delegate the handling
     * of some specific shapes.
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
        case STEM:
            logger.debug("Deassigning a Stem as glyph {}", glyph.getId());
            cancelStems(Collections.singletonList(glyph));

            break;

        case NOISE:
            logger.info("Skipping Noise as glyph {}", glyph.getId());

            break;

        default:
            super.deassignGlyph(glyph);

            break;
        }
    }

    //---------------//
    // segmentGlyphs //
    //---------------//
    public void segmentGlyphs (Collection<Glyph> glyphs,
                               boolean isShort)
    {
        deassignGlyphs(glyphs);

        for (Glyph glyph : new ArrayList<>(glyphs)) {
            SystemInfo system = sheet.getSystemOf(glyph);
            system.segmentGlyphOnStems(glyph, isShort);
        }
    }

    //-----------//
    // trimSlurs //
    //-----------//
    public void trimSlurs (Collection<Glyph> glyphs)
    {
        List<Glyph> slurs = new ArrayList<>();

        for (Glyph glyph : new ArrayList<>(glyphs)) {
            SystemInfo system = sheet.getSystemOf(glyph);
            Glyph slur = system.trimSlur(glyph);

            if (slur != null) {
                slurs.add(slur);
            }
        }

        if (!slurs.isEmpty()) {
            assignGlyphs(slurs, Shape.SLUR, false, Evaluation.MANUAL);
        }
    }

    //-------------//
    // assignGlyph //
    //-------------//
    /**
     * Assign a Shape to a glyph, inserting the glyph to its containing
     * system and lag if it is still transient
     *
     * @param glyph the glyph to be assigned
     * @param shape the assigned shape, which may be null
     * @param grade the grade about shape
     */
    @Override
    protected Glyph assignGlyph (Glyph glyph,
                                 Shape shape,
                                 double grade)
    {
        if (glyph == null) {
            return null;
        }

        // Test on glyph weight (noise-like)
        // To prevent to assign a non-noise shape to a noise glyph
        if ((shape == Shape.NOISE) || evaluator.isBigEnough(glyph)) {
            // Force a recomputation of glyph parameters
            // (since environment may have changed since the time they
            // have been computed)
            SystemInfo system = sheet.getSystemOf(glyph);

            if (system != null) {
                system.computeGlyphFeatures(glyph);

                return super.assignGlyph(glyph, shape, grade);
            }
        }

        return glyph;
    }
}
