//----------------------------------------------------------------------------//
//                                                                            //
//                       S e n t e n c e P a t t e r n                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.pattern.GlyphPattern;
import omr.glyph.text.Sentence.Stripe;
import static omr.glyph.text.Sentence.Stripe.Kind.*;

import omr.grid.StaffInfo;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code SentencePattern} gathers text-shaped glyphs found
 * within a system into proper sentences.
 *
 * At the end, the underlying compound of each sentence is split into
 * "word glyphs" (and for the specific case of Lyrics they are split into
 * "syllable glyphs"), to allow precise display positioning and user edition.
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
    //
    /** The physical text lines built within this system. */
    private final List<Sentence> physicals = new ArrayList<>();

    /** The isolated characters. */
    private final List<Glyph> characters = new ArrayList<>();

    /** The logical sentences built from physical lines and chars. */
    private final List<Sentence> logicals = new ArrayList<>();

    /** Candidate lyrics sentences. */
    private final List<Sentence> lyrics = new ArrayList<>();

    /** Blacklisted glyphs from degraded lyrics lines. */
    private final List<Glyph> noLyrics = new ArrayList<>();

    /** Score language */
    private final String language;

    //~ Constructors -----------------------------------------------------------
    //
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

        language = system.getScoreSystem().getScore().getLanguage();
    }

    //~ Methods ----------------------------------------------------------------
    //
    //------------//
    // runPattern //
    //------------//
    /**
     * Aggregate the various text glyphs into sentences.
     *
     * @return nothing
     */
    @Override
    public int runPattern ()
    {
        logger.fine("\n SentencePattern on {0}", system.idString());

        // Retrieve physical sentences
        buildPhysicals();

        // Extract non-lyrics & lyrics sentences
        buildLogicals();

        // Assign isolated characters as non-lyrics or lyrics
        processChars();

        // Lyrics extraction
        extendLyrics();

        // Check each logical sentence has just one text content
        checkContents();

        // Split every logical sentence by words or syllables
        splitSentences();

        // Record the final logicals as the system sentences
        system.resetSentences();
        system.getSentences().addAll(logicals);

        return 0; // Useless
    }

    //---------------//
    // buildLogicals //
    //---------------//
    /**
     * Recognize the content of each physical text line.
     * Each such line may lead to zero, one or several sentences
     */
    private void buildLogicals ()
    {
        for (Sentence line : physicals) {
            if (line.getTextRole() == TextRole.Lyrics) {
                lyrics.add(line);
            } else {
                logicals.addAll(line.extractLogicals(language));
            }
        }

        dump("buildLogicals. logicals w/o lyrics", logicals);
        dump("buildLogicals. lyrics", lyrics);
    }

    //----------------//
    // buildPhysicals //
    //----------------//
    /**
     * Aggregate text glyphs into physical lines and keep isolated
     * characters apart for now.
     */
    private void buildPhysicals ()
    {
        for (Glyph glyph : system.getGlyphs()) {
            glyph.setProcessed(false);
        }

        // Build skeletons with true text glyphs, excluding characters for now
        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.isText()) {
                if (glyph.getShape() == Shape.CHARACTER) {
                    characters.add(glyph);
                } else {
                    feedLine(glyph);
                }
            }
        }

        dump("buildPhysicals. initial physicals", physicals);

        // Extend the sentence skeletons
        for (Sentence physical : physicals) {
            // Make sure the various text items do not overlap
            physical.mergeEnclosedTexts();

            // Be greedy wrt nearby glyphs not (yet) assigned a text shape
            physical.includeAliens();
        }

        // Merge physical lines whenever possible
        mergePhysicals();

        dump("buildPhysicals. final physicals", physicals);
    }

    //---------------//
    // checkContents //
    //---------------//
    /**
     * Make sure every logical has its full textual content.
     */
    private void checkContents ()
    {
        List<Sentence> toAdd = new ArrayList<>();

        for (Sentence sentence : logicals) {
            toAdd.addAll(sentence.extractLogicals(language));
        }

        logicals.clear();
        logicals.addAll(toAdd);
        dump("checkContents. logicals", logicals);
    }

    //-------------------//
    // checkLinesOverlap //
    //-------------------//
    /**
     * Check whether core stripes overlap.
     * If so, the upper one is a direction and only the lower one is a lyrics
     *
     * @param lines the collection of senetnce lyrics, to be purged.
     * @return true if there was some overlap
     */
    private boolean checkLinesOverlap (List<Sentence> lines)
    {
        boolean overlap = false;
        // Sort by increasing ordinate
        Collections.sort(lines, Sentence.ordinateComparator);

        for (Iterator<Sentence> it = lines.iterator(); it.hasNext();) {
            Sentence upper = it.next();
            Stripe core = upper.getStripe(CORE);
            int index = lines.indexOf(upper);

            for (Sentence lower : lines.subList(index + 1, lines.size())) {
                if (lower.getStripe(CORE).intersects(core)) {
                    logger.fine("{0} overlaps {1}", new Object[]{
                                upper.idString(), lower.idString()});

                    it.remove();
                    degradeLine(upper);
                    overlap = true;

                    break;
                }
            }
        }

        return overlap;
    }

    //-------------//
    // degradeLine //
    //-------------//
    /**
     * A former lyrics sentence instance turns out to be just a
     * direction sentence.
     *
     * @param line the instance to degrade
     */
    private void degradeLine (Sentence line)
    {
        line.setTextRole(TextRole.Direction);
        logicals.add(line);

        // Blacklist the corresponding glyphs
        noLyrics.addAll(line.getItems());

        // Remove staff attachments
        line.removeAttachments();
    }

    //------//
    // dump //
    //------//
    /**
     * Convenient method to printout a collection of sentences.
     *
     * @param title     a label
     * @param sentences the sentences to print
     */
    private void dump (String title,
                       Collection<Sentence> sentences)
    {
        if (logger.isFineEnabled()) {
            StringBuilder sb = new StringBuilder("\n" + system.idString());
            sb.append(" ").append(title).append(":");

            for (Sentence sentence : sentences) {
                sb.append("\n > ").append(sentence);
            }

            logger.fine(sb.toString());
        }
    }

    //--------------//
    // extendLyrics //
    //--------------//
    /**
     * Extend lyrics line portions aggressively.
     */
    private void extendLyrics ()
    {
        // Sort by decreasing item weight
        Collections.sort(lyrics, Sentence.reverseWeightComparator);

        // Aggregate items by deskewed ordinate
        List<Sentence> lines = new ArrayList<>();

        for (Sentence sentence : lyrics) {
            boolean found = false;

            // Look for a compatible lyrics line
            for (Sentence line : lines) {
                if (line.matches(sentence)) {
                    line.addAllItems(sentence.getItems());
                    found = true;

                    break;
                }
            }

            if (!found) {
                // Start a new lyrics line
                lines.add(sentence);
            }
        }

        // Define line polygons
        for (StaffInfo staff : system.getStaves()) {
            staff.removeAttachments(Sentence.ATT_PREFIX);
        }

        for (Sentence line : lines) {
            line.buildStripes();
        }

        dump("extendLyrics. lines", lines);

        // Check whether core stripes overlap
        // (this purges and sorts lines by increasing ordinates)
        if (checkLinesOverlap(lines)) {
            dump("extendLyrics. after overlap lines", lines);
        }

        // Retrieve intersected glyphs, per line stripe
        for (Sentence line : lines) {
            line.lookupCandidateGlyphs(noLyrics);
        }

        // Beware of glyphs that appear in different lines
        for (int i = 0, iMax = lines.size() - 1; i <= iMax; i++) {
            Sentence line = lines.get(i);

            if (i < iMax) {
                line.checkCommonGlyphs(lines.get(i + 1));
            }
        }

        // Gather suitable intersected glyphs into (new) sentences
        for (Sentence line : lines) {
            // Filter candidates using stripe intersection
            line.setItems(line.getCandidates());

            List<Sentence> sentences = line.extractLogicals(language);

            for (Sentence s : sentences) {
                logger.fine("From line {0}: {1}", new Object[]{line.getId(),
                                                               s});

                logicals.add(s);
            }
        }

        dump("extendLyrics. final logicals", logicals);
    }

    //----------//
    // feedLine //
    //----------//
    /**
     * Populate a sentence with this text glyph, either by aggregating
     * the glyph to an existing sentence or by creating a new instance.
     *
     * @param glyph the text glyph to host in a sentence
     */
    private void feedLine (Glyph glyph)
    {
        // First look for the first sentence that could host the item
        for (Sentence line : physicals) {
            if (line.isCloseTo(glyph)) {
                line.addItem(glyph);
                logger.fine("Inserted {0} into {1}",
                            new Object[]{glyph.idString(), line});
                return;
            }
        }

        // No compatible text line found, so create a brand new one
        Sentence line = new Sentence(glyph);
        physicals.add(line);
    }

    //----------------//
    // mergePhysicals //
    //----------------//
    /**
     * Merge the physical lines that are very close to each other.
     */
    private void mergePhysicals ()
    {
        for (Sentence current : physicals) {
            current.setProcessed(false);
        }

        for (Sentence current : physicals) {
            Sentence candidate = current;

            // Keep on working while we do have a candidate to check for merge
            CandidateLoop:
            while (true) {
                // Check the candidate vs all entities until current excluded
                HeadsLoop:
                for (Sentence head : physicals) {
                    if (head == current) {
                        break CandidateLoop; // Actual end of sub-list
                    }

                    if ((head != candidate) && (!head.isProcessed())) {
                        // Check for a possible merge
                        if (head.isCloseTo(candidate)) {
                            Glyph compound = system.registerGlyph(
                                    head.mergeOf(candidate));

                            if (!compound.isShapeForbidden(Shape.TEXT)) {
                                if (logger.isFineEnabled()
                                        || head.isVip()
                                        || candidate.isVip()) {
                                    logger.info("Merged  {0} into {1}",
                                                new Object[]{
                                                candidate.idString(),
                                                head.idString()});

                                    if (candidate.isVip()) {
                                        head.setVip();
                                    }
                                }

                                head.addAllItems(candidate.getItems());
                                candidate.setProcessed(true);
                                candidate = head; // This is a new candidate

                                break HeadsLoop;
                            }
                        }
                    }
                }
            }
        }

        // Discard the merged sentences
        for (Iterator<Sentence> it = physicals.iterator(); it.hasNext();) {
            if (it.next().isProcessed()) {
                it.remove();
            }
        }
    }

    //--------------//
    // processChars //
    //--------------//
    /**
     * Make each isolated character as a stand-alone text line.
     * Either a lyrics for further processing or directly a logical.
     */
    private void processChars ()
    {
        if (!characters.isEmpty()) {
            logger.fine("{0}{1}", new Object[]{system.idString(), Glyphs.
                        toString(" processing chars: ", characters)});

            for (Glyph ch : characters) {
                if (ch.isActive() && (!ch.isProcessed())) {
                    Sentence sentence = new Sentence(ch);
                    ch.setProcessed(true);

                    if (ch.getTextRole() == TextRole.Lyrics) {
                        lyrics.add(sentence);
                    } else {
                        logicals.add(sentence);
                    }
                }
            }
        }
    }

    //----------------//
    // splitSentences //
    //----------------//
    /**
     * For each sentence, replace the underlying series of items by a
     * normalized sequence of word glyphs or syllable glyphs.
     */
    private void splitSentences ()
    {
        for (Sentence sentence : logicals) {
            ///logger.info("before splitSentences for {0}", sentence);
            List<Glyph> wordGlyphs = sentence.getCompound().retrieveSubGlyphs(
                    sentence.getTextRole() == TextRole.Lyrics);

            sentence.setItems(wordGlyphs);
            sentence.computePreciseFontSize();
            
            // Link back Glyph -> Sentence
            for (Glyph wordGlyph : wordGlyphs) {
                wordGlyph.setSentence(sentence);
            }

            ///logger.info("after  splitSentences for {0}", sentence);
        }

        dump("splitLyrics. final logicals", logicals);
    }
}
