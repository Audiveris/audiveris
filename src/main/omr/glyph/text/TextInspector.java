//----------------------------------------------------------------------------//
//                                                                            //
//                         T e x t I n s p e c t o r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text;

import omr.glyph.Evaluation;
import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.lag.HorizontalOrientation;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import java.util.*;

/**
 * Class <code>TextInspector</code> handles the inspection of textual items in
 * a system
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class TextInspector
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TextInspector.class);

    //~ Instance fields --------------------------------------------------------

    /** Dedicated system */
    private final SystemInfo system;

    /** Used to assign a unique ID to system sentences */
    private int sentenceCount = 0;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new TextInspector object.
     *
     * @param system The dedicated system
     */
    public TextInspector (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // retrieveSentences //
    //-------------------//
    /**
     * Aggregate the various text glyphs in horizontal sentences
     * @return the number of recognized textual items
     */
    public int retrieveSentences ()
    {
        int modifs = 0;

        // Keep the previous work! No sentences.clear();
        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isText()) {
                if (feedSentence(glyph)) {
                    modifs++;
                }
            }
        }

        // Extend the sentence skeletons
        for (Sentence sentence : system.getSentences()) {
            // Make sure the various text items do not overlap
            sentence.mergeEnclosedTexts();

            // Be greedy wrt nearby glyphs not (yet) assigned a text shape
            sentence.includeAliens();
        }

        // Merge sentences if needed
        mergeSentences();

        // Recognize content of each sentence
        for (Sentence sentence : system.getSentences()) {
            sentence.recognize();
        }

        // Special handling of lyrics items
        for (Sentence sentence : system.getSentences()) {
            if (sentence.getTextRole() == TextRole.Lyrics) {
                sentence.splitIntoWords();
            }
        }

        return modifs;
    }

    //----------------//
    // runTextPattern //
    //----------------//
    /**
     * Besides the existing text-shaped glyphs, using system area subdivision,
     * try to retrieve additional series of glyphs that could represent text
     * portions in the system at hand
     * @return the number of text glyphs built
     */
    public int runTextPattern ()
    {
        // Create a TextArea on the whole system
        TextArea area = new TextArea(
            system,
            null,
            system.getSheet().getVerticalLag().createAbsoluteRoi(
                system.getBounds()),
            new HorizontalOrientation());

        // Find and build additional text glyphs (words most likely)
        area.subdivide();

        // Process alignments of text items
        return retrieveSentences();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return getClass()
                   .getSimpleName() + " System#" + system.getId();
    }

    //--------------//
    // feedSentence //
    //--------------//
    /**
     * Populate a Sentence with this text glyph, either by aggregating the glyph
     * to an existing sentence or by creating a new sentence
     *
     * @param item the text item to host in a sentence
     * @return true if a sentence has been modified or created
     */
    private boolean feedSentence (Glyph item)
    {
        // First look for an existing sentence that could host the item
        for (Sentence sentence : system.getSentences()) {
            if (sentence.isCloseTo(item)) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Inserting glyph #" + item.getId() + " into " +
                        sentence);
                }

                return sentence.addItem(item);
            }
        }

        // No compatible sentence found, so create a brand new one
        Sentence sentence = new Sentence(system, item, ++sentenceCount);

        system.getSentences()
              .add(sentence);

        if (logger.isFineEnabled()) {
            logger.fine("Created new " + sentence);
        }

        return true;
    }

    //----------------//
    // mergeSentences //
    //----------------//
    /**
     * Merge the sentences that are very close to each other.
     */
    private void mergeSentences ()
    {
        boolean        finished = false;
        List<Sentence> list = new ArrayList<Sentence>(system.getSentences());

        while (!finished) {
            finished = true;

            oneLoop: 
            for (Sentence one : list) {
                for (Sentence two : list.subList(
                    list.indexOf(one) + 1,
                    list.size())) {
                    if (one.isCloseTo(two)) {
                        finished = false;
                        list.remove(one);
                        list.remove(two);

                        Glyph compound = system.addGlyph(one.mergeOf(two));
                        compound.setShape(Shape.TEXT, Evaluation.ALGORITHM);

                        Sentence s = new Sentence(
                            system,
                            compound,
                            ++sentenceCount);
                        list.add(s);

                        if (logger.isFineEnabled()) {
                            logger.fine(
                                " Sentence #" + s.getId() + " merging " + one +
                                " & " + two);
                        }

                        break oneLoop;
                    }
                }
            }
        }

        system.getSentences()
              .clear();
        system.getSentences()
              .addAll(list);
    }
}
