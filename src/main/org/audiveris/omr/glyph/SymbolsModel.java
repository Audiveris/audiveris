//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l s M o d e l                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.glyph;

import org.audiveris.omr.classifier.Classifier;
import org.audiveris.omr.classifier.ShapeClassifier;
import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.text.TextRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Class {@code SymbolsModel} is a GlyphsModel specifically meant for symbol glyphs.
 *
 * @author Hervé Bitteur
 */
public class SymbolsModel
        extends GlyphsModel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolsModel.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SymbolsModel object.
     *
     * @param sheet the related sheet
     */
    public SymbolsModel (Sheet sheet)
    {
        super(sheet, sheet.getGlyphIndex().getEntityService(), Step.SYMBOLS);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // assignText //
    //------------//
    /**
     * Assign a collection of glyphs as textual element
     *
     * @param glyphs      the collection of glyphs
     * @param role        the text role
     * @param textContent the ASCII content
     * @param grade       the grade WRT this assignment
     */
    public void assignText (Collection<Glyph> glyphs,
                            TextRole role,
                            String textContent,
                            double grade)
    {
        //        SystemManager systemManager = sheet.getSystemManager();
        //
        //        for (Glyph glyph : glyphs) {
        //            final Point centroid = glyph.getCentroid();
        //
        //            for (SystemInfo system : systemManager.getSystemsOf(centroid)) {
        //                String language = system.getSheet().getLanguageParam().getTarget();
        //                TextBuilder textBuilder = system.getTextBuilder();
        //                TextWord word = glyph.getTextWord();
        //                List<TextLine> lines = new ArrayList<TextLine>();
        //
        //                if (word == null) {
        //                    word = TextWord.createManualWord(glyph, textContent);
        //                    glyph.setTextWord(language, word);
        //
        //                    TextLine line = new TextLine(Arrays.asList(word));
        //                    lines = Arrays.asList(line);
        //                    lines = textBuilder.recomposeLines(lines);
        //                    system.getTextLines().remove(line);
        //                    system.getTextLines().addAll(lines);
        //                } else if (word.getTextLine() != null) {
        //                    lines = Arrays.asList(word.getTextLine());
        //                }
        //
        //                // Force text role
        //                glyph.setManualRole(role);
        //
        //                for (TextLine line : lines) {
        //                    // For Chord role, we don't spread the role to other words
        //                    // but rather trigger a line split
        //                    if ((role == TextRole.ChordName) && (line.getWords().size() > 1)) {
        //                        line.setRole(role);
        //
        //                        List<TextLine> subLines = textBuilder.recomposeLines(Arrays.asList(line));
        //                        system.getTextLines().remove(line);
        //
        //                        for (TextLine l : subLines) {
        //                            if (!l.getWords().contains(word)) {
        //                                l.setRole(null);
        //                            }
        //                        }
        //
        //                        system.getTextLines().addAll(subLines);
        //                    } else {
        //                        line.setRole(role);
        //                    }
        //                }
        //            }
        //
        //            // Force text only if it is not empty
        //            if ((textContent != null) && (textContent.length() > 0)) {
        //                glyph.setManualValue(textContent);
        //            }
        //        }
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
        //        // Do the job
        //        for (Glyph glyph : glyphs) {
        //            glyph.setTimeRational(timeRational);
        //        }
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
        logger.error("No yet implemented");

        //        /**
        //         * To remove a stem, several infos need to be modified : shape from
        //         * STEM to null, result from STEM to null, and the Stem must
        //         * be removed from system list of stems.
        //         *
        //         * The stem glyph must be removed (as well as all other non-recognized
        //         * glyphs that are connected to the former stem)
        //         *
        //         * Then, re-glyph extraction from sections when everything is ready
        //         * (GlyphBuilder). Should work on a micro scale : just the former stem
        //         * and the neighboring (non-assigned) glyphs.
        //         */
        //        Set<SystemInfo> impactedSystems = new HashSet<SystemInfo>();
        //
        //        for (Glyph stem : stems) {
        //            SystemInfo system = sheet.getSystemOf(stem);
        //            system.removeGlyph(stem);
        //            super.deassignGlyph(stem);
        //            impactedSystems.add(system);
        //        }
        //
        //        // Extract brand new glyphs from impactedSystems
        //        for (SystemInfo system : impactedSystems) {
        //            system.extractNewGlyphs();
        //        }
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
        //        // Safer
        //        if (glyph.getShape() == null) {
        //            return;
        //        }
        //
        //        // Processing depends on shape at hand
        //        switch (glyph.getShape()) {
        //        case STEM:
        //            logger.debug("Deassigning a Stem as glyph {}", glyph.getId());
        //            cancelStems(Collections.singletonList(glyph));
        //
        //            break;
        //
        //        case NOISE:
        //            logger.info("Skipping Noise as glyph {}", glyph.getId());
        //
        //            break;
        //
        //        default:
        //            super.deassignGlyph(glyph);
        //
        //            break;
        //        }
    }

    //---------------//
    // segmentGlyphs //
    //---------------//
    public void segmentGlyphs (Collection<Glyph> glyphs)
    {
        logger.error("No yet implemented");

        //        deassignGlyphs(glyphs);
        //
        //        for (Glyph glyph : new ArrayList<Glyph>(glyphs)) {
        //            SystemInfo system = sheet.getSystemOf(glyph);
        //            system.segmentGlyphOnStems(glyph);
        //        }
    }

    //-----------//
    // trimSlurs //
    //-----------//
    public void trimSlurs (Collection<Glyph> glyphs)
    {
        logger.error("No yet implemented");

        //        List<Glyph> slurs = new ArrayList<Glyph>();
        //
        //        for (Glyph glyph : new ArrayList<Glyph>(glyphs)) {
        //            SystemInfo system = sheet.getSystemOf(glyph);
        //            Glyph slur = system.trimSlur(glyph);
        //
        //            if (slur != null) {
        //                slurs.add(slur);
        //            }
        //        }
        //
        //        if (!slurs.isEmpty()) {
        //            assignGlyphs(slurs, Shape.SLUR, false, Evaluation.MANUAL);
        //        }
    }

    //-------------//
    // assignGlyph //
    //-------------//
    @Override
    protected Glyph assignGlyph (Glyph glyph,
                                 int interline,
                                 Shape shape,
                                 double grade)
    {
        if (glyph == null) {
            return null;
        }

        // Test on glyph weight (noise-like)
        // To prevent to assign a non-noise shape to a noise glyph
        /** Standard classifier */
        final Classifier classifier = ShapeClassifier.getInstance();

        if ((shape == Shape.NOISE) || classifier.isBigEnough(glyph, interline)) {
            return super.assignGlyph(glyph, interline, shape, grade);
        }

        return glyph;
    }
}
