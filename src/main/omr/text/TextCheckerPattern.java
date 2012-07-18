//----------------------------------------------------------------------------//
//                                                                            //
//                    T e x t C h e c k e r P a t t e r n                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.glyph.facets.Glyph;
import omr.glyph.pattern.GlyphPattern;

import omr.log.Logger;

import omr.sheet.SystemInfo;

/**
 * Class {@code TextCheckerPattern} is a debugging utility used to make
 * a global check of text entities in a system.
 *
 * @author Hervé Bitteur
 */
public class TextCheckerPattern
        extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
            TextCheckerPattern.class);

    //~ Constructors -----------------------------------------------------------
    //
    //--------------------//
    // TextCheckerPattern //
    //--------------------//
    public TextCheckerPattern (
            SystemInfo system)
    {
        super("textChecker", system);
    }

    //~ Methods ----------------------------------------------------------------
    //
    //------------//
    // runPattern //
    //------------//
    /**
     * Check sentences and text glyphs.
     *
     * @return nothing
     */
    @Override
    public int runPattern ()
    {
//        if (system.getId() == 1) {
//            logger.info("{0} Sentences: {1}",
//                        system.idString(), system.getSentences().size());
//            for (TextLine sentence : system.getSentences()) {
//                logger.fine("   {0}", sentence);
//            }
//        }
        int glyphsAltered = 0;
        int glyphsTotal = 0;

        // Check glyphs
        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isText()) {
                glyphsTotal++;
                if (!checkGlyph(glyph)) {
                    glyphsAltered++;
                }
            }
        }

        if (glyphsAltered > 0) {
            logger.info("{0} Text glyphs altered: {1}/{2}",
                        system.idString(), glyphsAltered, glyphsTotal);
        }

        // Check sentences
        int sentencesAltered = 0;

        for (TextLine sentence : system.getSentences()) {
            int modifs = 0;

            for (TextWord word : sentence.getWords()) {
                Glyph glyph = word.getGlyph();
                if (!checkGlyph(glyph)) {
                    modifs++;
                }

            }
            if (modifs > 0) {
                sentencesAltered++;
                logger.fine("{0} modifs in {1}", modifs, sentence);
            }
        }

        if (sentencesAltered > 0) {
            logger.info("{0} Text sentences altered: {1}/{2}",
                        system.idString(),
                        sentencesAltered, system.getSentences().size());
        }

        return 0; // Useless

    }

    //------------//
    // checkGlyph //
    //------------//
    private boolean checkGlyph (Glyph glyph)
    {
        if (glyph == null) {
            logger.fine("No related glyph");
            return false;
        }

        if (!glyph.isActive()) {
            logger.fine("{0} Not active", glyph.idString());
            return false;
        }

        if (!glyph.isText()) {
            logger.fine("{0} Not a text", glyph.idString());
            return false;
        }

        TextWord word = glyph.getTextWord();
        if (word == null) {
            logger.fine("{0} No textWord", glyph.idString());
            return false;
        }

        if (!glyph.getTextValue().equals(word.getInternalValue())) {
            logger.fine("{0} Value modified: \"{1}\" vs \"{2}\"",
                        glyph.idString(),
                        glyph.getTextValue(),
                        word.getInternalValue());
            return false;
        }
        
        TextLine line = word.getTextLine();
        if (!line.getWords().contains(word)) {
            logger.fine("{0} Not in containing line {1}",
                        glyph.idString(), line);
            return false;
        }

        
        return true;
    }
}
