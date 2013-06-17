//----------------------------------------------------------------------------//
//                                                                            //
//                                  T e x t                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.facets.Glyph;

import omr.score.visitor.ScoreVisitor;

import omr.text.TextLine;
import omr.text.TextRole;
import omr.text.TextRoleInfo;
import omr.text.TextWord;

import omr.ui.symbol.TextFont;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code Text} handles any textual score entity.
 *
 * <p><b>Nota</b>: There is exactly one Text entity per sentence, except for
 * lyrics items for which we build one {@code LyricsItem} (subclass of Text)
 * for each textual glyph.
 * The reason is that, except for lyrics, only the full sentence
 * is meaningful: for example "Ludwig van Beethoven" is meaningful as a Creator
 * Text, but the various words "Ludwig", "van", "Beethoven" are not.
 * For lyrics, since we can have very long sentences, and since the positioning
 * of every syllable must be done with precision, we handle one LyricsItem Text
 * entity per isolated word.</p>
 *
 * <p>Working at the sentence level also allows a better accuracy in the setting
 * of parameters (such as baseline or font) for the whole sentence.</p>
 *
 * <h4>Synoptic of Text Translation:<br/>
 * <img src="doc-files/TextTranslation.jpg"/>
 * </h4>
 *
 * @author Hervé Bitteur
 */
public abstract class Text
        extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Text.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The containing sentence. */
    private final TextLine sentence;

    //~ Constructors -----------------------------------------------------------
    //------//
    // Text //
    //------//
    /**
     * Creates a new Text object.
     *
     * @param sentence the larger sentence
     */
    public Text (TextLine sentence)
    {
        this(sentence, sentence.getLocation());
    }

    //------//
    // Text //
    //------//
    /**
     * Creates a new Text object, with a specific location, different
     * from the sentence location.
     *
     * @param sentence the larger sentence
     * @param location specific location
     */
    public Text (TextLine sentence,
                 Point location)
    {
        super(sentence.getSystemPart());
        this.sentence = sentence;
        setReferencePoint(location);

        setBox(sentence.getBounds());

        logger.debug("Created {}", this);
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //------------//
    // getContent //
    //------------//
    /**
     * Report the current string value of this text
     *
     * @return the string value of this text
     */
    public String getContent ()
    {
        return sentence.getValue();
    }

    //---------------------//
    // getExportedFontSize //
    //---------------------//
    /**
     * Report the font size to be exported for this text
     *
     * @return the exported font size
     */
    public int getExportedFontSize ()
    {
        return (int) Math.rint(
                sentence.getMeanFont().pointsize * TextFont.TO_POINT);
    }

    //---------------//
    // getLyricsFont //
    //---------------//
    /**
     * Report the font to be used for handling lyrics text
     *
     * @return the lyrics text font
     */
    public static Font getLyricsFont ()
    {
        return TextFont.baseTextFont;
    }

    //-------------------//
    // getLyricsFontSize //
    //-------------------//
    /**
     * Report the font size to be exported for the lyrics
     *
     * @return the exported lyrics font size
     */
    public static int getLyricsFontSize ()
    {
        return TextFont.baseTextFont.getSize();
    }

    //-------------//
    // getSentence //
    //-------------//
    /**
     * Report the sentence that relates to this text
     *
     * @return the related sentence
     */
    public TextLine getSentence ()
    {
        return sentence;
    }

    //---------------------//
    // getTranslationLinks //
    //---------------------//
    @Override
    public List<Line2D> getTranslationLinks (Glyph glyph)
    {
        return Collections.emptyList(); // By default
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width of this text
     *
     * @return the text width
     */
    public int getWidth ()
    {
        return getBox().width;
    }

    //----------//
    // populate //
    //----------//
    /**
     * Allocate the proper score entity (or entities) that correspond
     * to this textual sentence.
     * This word or sequence of words may be:
     * <ul>
     * <li>a Direction</li>
     * <li>a part Name</li>
     * <li>a part Number</li>
     * <li>a Creator</li>
     * <li>a Copyright</li>
     * <li>one or several LyricsItem entities</li>
     * <li>a Chord Statement</li>
     * </ul>
     *
     * @param sentence the whole sentence
     * @param location its starting reference wrt containing system
     */
    public static void populate (TextLine sentence,
                                 Point location)
    {
        final SystemPart systemPart = sentence.getSystemPart();
        final TextRoleInfo roleInfo = sentence.getRole();
        final TextRole role = roleInfo.role;

        if ((role == null) || (role == TextRole.UnknownRole)) {
            systemPart.addError(
                    sentence.getFirstWord().getGlyph(),
                    "Sentence with no role defined");
        }

        logger.debug(
                "Populating {} {} \"{}\"",
                sentence,
                role,
                sentence.getValue());

        if (role == null) {
            return;
        }

        switch (role) {
        case Lyrics:

            // Create as many lyrics items as needed
            for (TextWord word : sentence.getWords()) {
                Glyph glyph = word.getGlyph();
                Rectangle itemBox = word.getBounds();
                String itemStr = word.getValue();

                if (itemStr == null) {
                    // A very rough char count ...
                    //                    int nbChar = (int) Math.rint(
                    //                            (double) itemBox.width / sentence.getTextHeight());
                    int nbChar = 5;
                    itemStr = role.getStringHolder(nbChar);
                }

                Point2D p1 = word.getBaseline()
                        .getP1();
                Point start = new Point(
                        (int) Math.rint(p1.getX()),
                        (int) Math.rint(p1.getY()));
                glyph.setTranslation(
                        new LyricsItem(
                        sentence,
                        start,
                        glyph,
                        itemBox.width,
                        itemStr));
            }

            break;

        case Title:
            sentence.setGlyphsTranslation(new TitleText(sentence));

            break;

        case Direction:

            Measure measure = systemPart.getMeasureAt(location);
            sentence.setGlyphsTranslation(
                    new DirectionStatement(
                    measure,
                    location,
                    measure.getDirectionChord(location),
                    new DirectionText(sentence)));

            break;

        case Number:
            sentence.setGlyphsTranslation(new NumberText(sentence));

            break;

        case Name:
            sentence.setGlyphsTranslation(new NameText(sentence));

            break;

        case Creator:
            sentence.setGlyphsTranslation(new CreatorText(sentence));

            break;

        case Rights:
            sentence.setGlyphsTranslation(new RightsText(sentence));

            break;

        case Chord:
            measure = systemPart.getMeasureAt(location);
            sentence.setGlyphsTranslation(
                    new ChordSymbol(
                    measure,
                    location,
                    measure.getEventChord(location),
                    new ChordText(sentence)));

            break;

        case UnknownRole:
        default:
            sentence.setGlyphsTranslation(new DefaultText(sentence));
        }
    }

    //----------//
    // toString //
    //----------//
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Text");

        if (sentence.getRole() != null) {
            sb.append(" ")
                    .append(sentence.getRole());
        }

        sb.append(internalsString());

        if (getContent() != null) {
            sb.append(" \"")
                    .append(getContent())
                    .append("\"");
        }

        sb.append(" loc:")
                .append(getReferencePoint());

        sb.append(" S")
                .append(getSystem().getId());
        sb.append("P")
                .append(getPart().getId());

        sb.append("}");

        return sb.toString();
    }

    //-----------------------//
    // computeReferencePoint //
    //-----------------------//
    @Override
    protected void computeReferencePoint ()
    {
        setReferencePoint(sentence.getLocation());
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, for inclusion
     * in a toString().
     *
     * @return the string of internals
     */
    protected String internalsString ()
    {
        return ""; // By default
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // ChordText //
    //-----------//
    /** Subclass of Text, dedicated to a chord marker. */
    public static class ChordText
            extends Text
    {
        //~ Constructors -------------------------------------------------------

        public ChordText (TextLine sentence)
        {
            super(sentence);
        }
    }

    //-------------//
    // CreatorText //
    //-------------//
    /** Subclass of Text, dedicated to a Creator (composer, lyricist,
     * etc...). */
    public static class CreatorText
            extends Text
    {
        //~ Enumerations -------------------------------------------------------

        public enum CreatorType
        {
            //~ Enumeration constant initializers ------------------------------

            composer, lyricist, arranger;

        }

        //~ Constructors -------------------------------------------------------
        public CreatorText (TextLine sentence)
        {
            super(sentence);
        }
    }

    //-------------//
    // DefaultText //
    //-------------//
    /**
     * Subclass of Text, with no precise role assigned.
     */
    public static class DefaultText
            extends Text
    {
        //~ Constructors -------------------------------------------------------

        public DefaultText (TextLine sentence)
        {
            super(sentence);
        }
    }

    //---------------//
    // DirectionText //
    //---------------//
    /** Subclass of Text, dedicated to a Direction instruction. */
    public static class DirectionText
            extends Text
    {
        //~ Constructors -------------------------------------------------------

        public DirectionText (TextLine sentence)
        {
            super(sentence);
        }
    }

    //----------//
    // NameText //
    //----------//
    /** Subclass of Text, dedicated to a part Name. */
    public static class NameText
            extends Text
    {
        //~ Constructors -------------------------------------------------------

        public NameText (TextLine sentence)
        {
            super(sentence);

            if (getContent() != null) {
                sentence.getSystemPart()
                        .setName(getContent());
            }
        }
    }

    //------------//
    // NumberText //
    //------------//
    /** Subclass of Text, dedicated to a score Number. */
    public static class NumberText
            extends Text
    {
        //~ Constructors -------------------------------------------------------

        public NumberText (TextLine sentence)
        {
            super(sentence);
        }
    }

    //------------//
    // RightsText //
    //------------//
    /** Subclass of Text, dedicated to a copyright statement. */
    public static class RightsText
            extends Text
    {
        //~ Constructors -------------------------------------------------------

        public RightsText (TextLine sentence)
        {
            super(sentence);
        }
    }

    //-----------//
    // TitleText //
    //-----------//
    /** Subclass of Text, dedicated to a score Title. */
    public static class TitleText
            extends Text
    {
        //~ Constructors -------------------------------------------------------

        public TitleText (TextLine sentence)
        {
            super(sentence);
        }
    }
}
