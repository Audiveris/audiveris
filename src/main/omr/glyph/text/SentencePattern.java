//----------------------------------------------------------------------------//
//                                                                            //
//                       S e n t e n c e P a t t e r n                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.glyph.Evaluation;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.pattern.GlyphPattern;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code SentencePattern} gathers text-shaped glyphs found
 * within a system into proper sentences.
 *
 * @author Hervé Bitteur
 */
public class SentencePattern
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        SentencePattern.class);

    //~ Instance fields --------------------------------------------------------

    /** The physical lines built within this system */
    private List<Sentence> physicals;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // SentencePattern //
    //-----------------//
    /**
     * Creates a new SentencePattern object.
     * @param system The dedicated system
     */
    public SentencePattern (SystemInfo system)
    {
        super("Sentence", system);
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // runPattern //
    //------------//
    /**
     * Aggregate the various text glyphs into sentences.
     * @return the number of recognized textual items
     */
    @Override
    public int runPattern ()
    {
        int modifs = 0;
        physicals = new ArrayList<Sentence>();

        // Isolated characters
        List<Glyph> chars = new ArrayList<Glyph>();

        for (Glyph glyph : system.getGlyphs()) {
            // Focus on true text, excluding isolated characters for now
            if (glyph.isText()) {
                if (glyph.getShape() == Shape.CHARACTER) {
                    chars.add(glyph);
                } else if (feedLine(glyph)) {
                    modifs++;
                }
            }
        }

        // Extend the sentence skeletons
        for (Sentence line : physicals) {
            // Make sure the various text items do not overlap
            line.mergeEnclosedTexts();

            // Be greedy wrt nearby glyphs not (yet) assigned a text shape
            line.includeAliens();
        }

        // Merge text lines if needed
        mergeLines();

        // Recognize content of each text line
        // Each such line may lead to zero, one or several sentences
        String language = system.getScoreSystem()
                                .getScore()
                                .getLanguage();

        system.resetSentences();

        for (Sentence line : physicals) {
            system.getSentences()
                  .addAll(line.extractLogicals(language));
        }

        // Make each isolated character as a stand-alone text line
        if (logger.isFineEnabled()) {
            logger.info(
                "S#" + system.getId() +
                Glyphs.toString(" Initial chars: ", chars));
        }

        for (Glyph ch : chars) {
            // Check whether the char is still isolated
            if (ch.isActive()) {
                Sentence line = new Sentence(system, ch);
                system.getSentences()
                      .addAll(line.extractLogicals(language));
            }
        }

        // Extend lyrics line portions aggressively
        
        
        // Special handling of lyrics items
        // We split the underlying long glyph into word glyphs
        for (Sentence sentence : system.getSentences()) {
            if (sentence.getTextRole() == TextRole.Lyrics) {
                sentence.splitIntoWords();
            }
        }

        return modifs;
    }

    //----------//
    // feedLine //
    //----------//
    /**
     * Populate a Sentence with this text glyph, either by aggregating
     * the glyph to an existing Sentence or by creating a new Sentence
     * instance.
     * @param glyph the text item to host in a sentence
     * @return true if a Sentence has been modified or created
     */
    private boolean feedLine (Glyph glyph)
    {
        // First look for an existing sentence that could host the item
        for (Sentence line : physicals) {
            if (line.isCloseTo(glyph)) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Inserting glyph #" + glyph.getId() + " into " + line);
                }

                return line.addGlyph(glyph);
            }
        }

        // No compatible text line found, so create a brand new one
        Sentence line = new Sentence(system, glyph);
        physicals.add(line);

        if (logger.isFineEnabled()) {
            logger.fine("Created new " + line);
        }

        return true;
    }

    //------------//
    // mergeLines //
    //------------//
    /**
     * Merge the lines that are very close to each other.
     */
    private void mergeLines ()
    {
        boolean finished = false;

        while (!finished) {
            finished = true;

            oneLoop: 
            for (Sentence one : physicals) {
                for (Sentence two : physicals.subList(
                    physicals.indexOf(one) + 1,
                    physicals.size())) {
                    if (one.isCloseTo(two)) {
                        // Check the resulting text is not black-listed
                        Glyph compound = one.mergeOf(two);

                        if (!compound.isShapeForbidden(Shape.TEXT)) {
                            compound.setShape(Shape.TEXT, Evaluation.ALGORITHM);

                            Sentence s = new Sentence(system, compound);
                            physicals.add(s);

                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    " Sentence " + " merging " + one + " & " +
                                    two);
                            }

                            finished = false;
                            physicals.remove(one);
                            physicals.remove(two);

                            break oneLoop;
                        }
                    }
                }
            }
        }
    }
}
