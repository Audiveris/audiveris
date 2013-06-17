//----------------------------------------------------------------------------//
//                                                                            //
//                    T e x t C h e c k e r P a t t e r n                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.glyph.facets.Glyph;
import omr.glyph.pattern.GlyphPattern;

import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(
            TextCheckerPattern.class);

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new TextCheckerPattern object.
     *
     * @param system DOCUMENT ME!
     */
    public TextCheckerPattern (SystemInfo system)
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
        //            logger.info("{} Sentences: {}",
        //                        system.idString(), system.getSentences().size());
        //            for (TextLine sentence : system.getSentences()) {
        //                logger.debug("   {}", sentence);
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
            logger.info(
                    "{} Text glyphs altered: {}/{}",
                    system.idString(),
                    glyphsAltered,
                    glyphsTotal);
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
                logger.debug("{} modifs in {}", modifs, sentence);
            }
        }

        if (sentencesAltered > 0) {
            logger.info(
                    "{} Text sentences altered: {}/{}",
                    system.idString(),
                    sentencesAltered,
                    system.getSentences().size());
        }

        return 0; // Useless
    }

    //------------//
    // checkGlyph //
    //------------//
    private boolean checkGlyph (Glyph glyph)
    {
        if (glyph == null) {
            logger.debug("No related glyph");

            return false;
        }

        if (!glyph.isActive()) {
            logger.debug("{} Not active", glyph.idString());

            return false;
        }

        if (!glyph.isText()) {
            logger.debug("{} Not a text", glyph.idString());

            return false;
        }

        TextWord word = glyph.getTextWord();

        if (word == null) {
            logger.debug("{} No textWord", glyph.idString());

            return false;
        }

        if (!glyph.getTextValue()
                .equals(word.getInternalValue())) {
            logger.debug(
                    "{} Value modified: \"{}\" vs \"{}\"",
                    glyph.idString(),
                    glyph.getTextValue(),
                    word.getInternalValue());

            return false;
        }

        TextLine line = word.getTextLine();

        if (!line.getWords()
                .contains(word)) {
            logger.debug(
                    "{} Not in containing line {}",
                    glyph.idString(),
                    line);

            return false;
        }

        return true;
    }
}
