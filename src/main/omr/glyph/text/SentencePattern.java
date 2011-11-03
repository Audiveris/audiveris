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
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.pattern.GlyphPattern;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import omr.util.Implement;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>SentencePattern</code> gathers text-shaped glyphs found within
 * a system into proper sentences.
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

    /** The text lines built within this system */
    private List<TextLine> textLines;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // SentencePattern //
    //-----------------//
    /**
     * Creates a new SentencePattern object.
     *
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
     * Aggregate the various text glyphs into horizontal sentences
     * @return the number of recognized textual items
     */
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        int modifs = 0;
        textLines = new ArrayList<TextLine>();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isText()) {
                if (feedLine(glyph)) {
                    modifs++;
                }
            }
        }

        // Extend the text line skeletons
        for (TextLine line : textLines) {
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

        for (TextLine line : textLines) {
            system.getSentences()
                  .addAll(line.extractSentences(language));
        }

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
     * Populate a TextLine with this text glyph, either by aggregating the glyph
     * to an existing TextLine or by creating a new TextLine instance.
     *
     * @param glyph the text item to host in a sentence
     * @return true if a TextLine has been modified or created
     */
    private boolean feedLine (Glyph glyph)
    {
        // First look for an existing sentence that could host the item
        for (TextLine line : textLines) {
            if (line.isCloseTo(glyph)) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Inserting glyph #" + glyph.getId() + " into " + line);
                }

                return line.addGlyph(glyph);
            }
        }

        // No compatible text line found, so create a brand new one
        TextLine line = new TextLine(system, glyph);
        textLines.add(line);

        if (logger.isFineEnabled()) {
            logger.fine("Created new " + line);
        }

        return true;
    }

    //------------//
    // mergeLines //
    //------------//
    /**
     * Merge the text lines that are very close to each other.
     */
    private void mergeLines ()
    {
        boolean finished = false;

        while (!finished) {
            finished = true;

            oneLoop: 
            for (TextLine one : textLines) {
                for (TextLine two : textLines.subList(
                    textLines.indexOf(one) + 1,
                    textLines.size())) {
                    if (one.isCloseTo(two)) {
                        // Check the resulting text is not black-listed
                        Glyph compound = one.mergeOf(two);

                        if (!compound.isShapeForbidden(Shape.TEXT)) {
                            compound.setShape(Shape.TEXT, Evaluation.ALGORITHM);

                            TextLine s = new TextLine(system, compound);
                            textLines.add(s);

                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    " TextLine " + " merging " + one + " & " +
                                    two);
                            }

                            finished = false;
                            textLines.remove(one);
                            textLines.remove(two);

                            break oneLoop;
                        }
                    }
                }
            }
        }
    }
}
