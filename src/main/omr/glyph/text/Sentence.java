//----------------------------------------------------------------------------//
//                                                                            //
//                              S e n t e n c e                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.entity.Text.CreatorText.CreatorType;

import omr.sheet.SystemInfo;

import omr.util.Navigable;

import java.util.Collection;
import java.util.SortedSet;

/**
 * Class <code>Sentence</code> encapsulates a consistent ordered set of text
 * glyphs (loosely similar to words) that represents a whole expression.
 *
 * <h4>Textual glyph Data Model:<br/>
 *    <img src="doc-files/Sentence.jpg"/>
 * </h4>
 *
 * <h4>Processing of textual glyphs:<br/>
 *    <img src="doc-files/TextProcessing.jpg"/>
 * </h4>
 *
 * @author Hervé Bitteur
 */
public class Sentence
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Sentence.class);

    //~ Instance fields --------------------------------------------------------

    /** The containing system */
    @Navigable(false)
    private final SystemInfo systemInfo;

    /** The containing system part */
    @Navigable(false)
    private final SystemPart systemPart;

    /** The sentence id */
    private final String id;

    /** The underlying text line */
    private final TextLine line;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // Sentence //
    //----------//
    /**
     * Creates a new Sentence object.
     *
     * @param systemInfo The containing system
     * @param line the underlying text line
     * @param id A unique ID within the containing system
     */
    public Sentence (SystemInfo systemInfo,
                     TextLine   line,
                     int        id)
    {
        this.systemInfo = systemInfo;
        this.line = line;
        this.id = systemInfo.getId() + "." + id;

        ScoreSystem system = systemInfo.getScoreSystem();
        PixelPoint  center = line.getGlyphs()
                                 .first()
                                 .getCentroid();

        // Choose carefully Staff (& then Part )
        Staff staff = system.getTextStaff(
            line.getFirstGlyph().getTextInfo().getTextRole(),
            center);
        systemPart = staff.getPart();

        if (logger.isFineEnabled()) {
            logger.fine("Created " + this);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //----------------------//
    // getContentFromGlyphs //
    //----------------------//
    /**
     * Determine the sentence string out of the glyphs individual strings
     * @return the concatenation of all glyphs strings
     */
    public String getContentFromGlyphs ()
    {
        StringBuilder sb = null;

        // Use each item string
        for (Glyph glyph : line.getGlyphs()) {
            String str = glyph.getTextInfo()
                              .getContent();

            if (str == null) {
                str = glyph.getTextInfo()
                           .getPseudoContent();

                if (str == null) {
                    if (logger.isFineEnabled()) {
                        logger.warning("Flat sentence " + this);
                    }

                    continue;
                }
            }

            if (sb == null) {
                sb = new StringBuilder(str);
            } else {
                sb.append(" ")
                  .append(str);
            }
        }

        if (sb == null) {
            return "";
        } else {
            return sb.toString();
        }
    }

    //-------------//
    // getFontSize //
    //-------------//
    /**
     * Report the font size of this sentence
     * @return the font size
     */
    public Float getFontSize ()
    {
        return line.getFontSize();
    }

    //-----------//
    // getGlyphs //
    //-----------//
    /**
     * Report the x-ordered collection of glyphs in this sentence
     * @return the collection of glyphs (words generally)
     */
    public SortedSet<Glyph> getGlyphs ()
    {
        return line.getGlyphs();
    }

    //----------------------//
    // setGlyphsTranslation //
    //----------------------//
    /**
     * Forward the provided translation to all the items that compose this
     * sentence
     * @param entity the same translation entit for all sentence items
     */
    public void setGlyphsTranslation (Object entity)
    {
        for (Glyph glyph : line.getGlyphs()) {
            glyph.setTranslation(entity);
        }
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the ID of this instance
     * @return the id
     */
    public String getId ()
    {
        return id;
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * Report the system-based starting point of this sentence
     * @return the starting point (x: left side, y: baseline)
     */
    public PixelPoint getLocation ()
    {
        return line.getLocation();
    }

    //------------------//
    // getSystemContour //
    //------------------//
    /**
     * Report the rectangular contour of this sentence
     * @return the sentence contour
     */
    public PixelRectangle getSystemContour ()
    {
        return line.getSystemContour();
    }

    //---------------//
    // getSystemPart //
    //---------------//
    /**
     * Report the system part that contains this sentence
     * @return the containing system part
     */
    public SystemPart getSystemPart ()
    {
        return systemPart;
    }

    //------------//
    // getContent //
    //------------//
    /**
     * Report the string content of this sentence, as computed by OCR or entered
     * manually for example
     * @return the text interpretation of the sentence
     */
    public String getTextContent ()
    {
        return line.getTextContent();
    }

    //---------------//
    // getTextHeight //
    //---------------//
    /**
     * Determine the uniform character height for the whole sentence
     * @return the standard character height in pixels
     */
    public int getTextHeight ()
    {
        return line.getTextHeight();
    }

    //-------------//
    // getTextRole //
    //-------------//
    /**
     * Report the text role of the sentence within the score
     * @return the role of this sentence
     */
    public TextRole getTextRole ()
    {
        return line.getFirstGlyph()
                   .getTextInfo()
                   .getTextRole();
    }

    //-------------//
    // getTextType//
    //-------------//
    /**
     * Report the text type of the sentence if any
     * @return the type of this sentence
     */
    public CreatorType getTextType ()
    {
        return line.getFirstGlyph()
                   .getTextInfo()
                   .getCreatorType();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Sentence #");
        sb.append(getId());

        TextRole role = getTextRole();

        if (role != null) {
            sb.append(" role:")
              .append(role);
        }

        sb.append(" ")
          .append(line);

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the containing system
     * @return the containing system
     */
    SystemInfo getSystem ()
    {
        return systemInfo;
    }

    //----------------//
    // splitIntoWords //
    //----------------//
    /**
     * Split the long glyph of this (Lyrics) sentence into word glyphs
     */
    void splitIntoWords ()
    {
        SortedSet<Glyph> glyphs = line.getGlyphs();

        // Make sure the split hasn't been done yet
        if (glyphs.size() > 1) {
            return;
        } else if (glyphs.isEmpty()) {
            logger.severe("splitIntoWords. Sentence with no items: " + this);
        } else {
            if (logger.isFineEnabled()) {
                logger.fine("Splitting lyrics of " + this);
            }

            Collection<Glyph> words = glyphs.first()
                                            .getTextInfo()
                                            .splitIntoWords();

            // Replace the single long item by this collection of word items
            if ((words != null) && !words.isEmpty()) {
                glyphs.clear();
                glyphs.addAll(words);
            }
        }
    }
}
