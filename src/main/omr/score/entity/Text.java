//----------------------------------------------------------------------------//
//                                                                            //
//                                  T e x t                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.facets.Glyph;
import omr.glyph.text.Sentence;
import omr.glyph.text.TextRole;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.visitor.ScoreVisitor;

import omr.ui.symbol.TextFont;

import java.awt.Font;

/**
 * Class {@code Text} handles any textual score entity.
 *
 * <p><b>Nota</b>: There is exactly one Text entity per sentence, except for
 * lyrics items for which we build one LyricsItem (subclass of Text) for each
 * textual glyph. The reason is that, except for lyrics, only the full sentence
 * is meaningful: for example "Ludwig van Beethoven" is meaningful as a Creator
 * Text, but the various glyphs "Ludwig", "van", "Beethoven" are not.
 * For lyrics, since we can have very long sentences, and since the positioning
 * of every syllable must be done with precision, we handle one LyricsItem Text
 * entity per isolated textual glyph.</p>
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
    private static final Logger logger = Logger.getLogger(Text.class);

    //~ Instance fields --------------------------------------------------------
    /** The containing sentence */
    private final Sentence sentence;

    /** The font to render this text entity */
    protected Font font;

    //~ Constructors -----------------------------------------------------------
    //------//
    // Text //
    //------//
    /**
     * Creates a new Text object.
     *
     * @param sentence the larger sentence
     */
    public Text (Sentence sentence)
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
    public Text (Sentence sentence,
                 PixelPoint location)
    {
        super(sentence.getSystemPart());
        this.sentence = sentence;
        setReferencePoint(location);

        setBox(sentence.getBounds());

        // Proper font ?
        if (sentence.getFontSize() != null) {
            font = TextFont.baseTextFont.deriveFont(
                    (float) sentence.getFontSize());
        } else {
            addError("Text with no sentence font size at " + location);
            font = TextFont.baseTextFont;
        }

        ///determineFontSize();
        logger.fine("Created {0}", this);
    }

    //~ Methods ----------------------------------------------------------------
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
    public static void populate (Sentence sentence,
                                 PixelPoint location)
    {
        final SystemPart systemPart = sentence.getSystemPart();
        final TextRole role = sentence.getTextRole();

        if ((role == null) || (role == TextRole.UnknownRole)) {
            systemPart.addError(
                    sentence.getCompound(),
                    "Sentence with no role defined");
        }

        logger.fine("Populating {0} {1} \"{2}\"", new Object[]{sentence, role,
                                                               sentence.
                    getTextContent()});

        if (role == null) {
            return;
        }

        switch (role) {
            case Lyrics:

                // Create as many lyrics items as needed
                for (Glyph item : sentence.getItems()) {
                    PixelRectangle itemBox = item.getBounds();
                    String itemStr = item.getTextValue();

                    if (itemStr == null) {
                        int nbChar = (int) Math.rint(
                                (double) itemBox.width / sentence.getTextHeight());
                        itemStr = role.getStringHolder(nbChar);
                    }

                    item.setTranslation(
                            new LyricsItem(
                            sentence,
                            new PixelPoint(itemBox.x, location.y),
                            item,
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
                        sentence,
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
                        new ChordStatement(
                        measure,
                        location,
                        measure.getEventChord(location),
                        sentence,
                        new ChordText(sentence)));

                break;

            case UnknownRole:
            default:
                sentence.setGlyphsTranslation(new DefaultText(sentence));
        }
    }

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
        return sentence.getTextContent();
    }

    //---------//
    // getFont //
    //---------//
    /**
     * Report the font to render this text
     *
     * @return the font to render this text
     */
    public Font getFont ()
    {
        return font;
    }

    //-------------//
    // getFontSize //
    //-------------//
    /**
     * Report the font size to be exported for this text
     *
     * @return the exported font size
     */
    public float getFontSize ()
    {
        return font.getSize2D();
    }

    //-------------//
    // getSentence //
    //-------------//
    /**
     * Report the sentence that relates to this text
     *
     * @return the related sentence
     */
    public Sentence getSentence ()
    {
        return sentence;
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
    // toString //
    //----------//
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Text ").append(sentence.getTextRole()).append(
                internalsString());

        if (font != null) {
            sb.append(" font:").append(font.getSize());
        }

        if (getContent() != null) {
            sb.append(" \"").append(getContent()).append("\"");
        }

        sb.append(" loc:").append(getReferencePoint());

        sb.append(" S").append(getSystem().getId()).append("P").append(getPart().
                getId());
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

            arranger,
            composer,
            lyricist,
            poet,
            transcriber,
            translator;
        }

        //~ Instance fields ----------------------------------------------------
        /** Creator type, if any */
        private CreatorType creatorType;

        //~ Constructors -------------------------------------------------------
        public CreatorText (Sentence sentence)
        {
            super(sentence);
            setCreatorType(sentence.getTextType());
        }

        //~ Methods ------------------------------------------------------------
        public CreatorType getCreatorType ()
        {
            return creatorType;
        }

        public void setCreatorType (CreatorType creatorType)
        {
            this.creatorType = creatorType;
        }

        @Override
        protected String internalsString ()
        {
            if (creatorType != null) {
                return " " + creatorType;
            } else {
                return "";
            }
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

        public DefaultText (Sentence sentence)
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

        public DirectionText (Sentence sentence)
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

        public NameText (Sentence sentence)
        {
            super(sentence);

            if (getContent() != null) {
                sentence.getSystemPart().setName(getContent());
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

        public NumberText (Sentence sentence)
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

        public RightsText (Sentence sentence)
        {
            super(sentence);
        }
    }

    //------------//
    // ChordText //
    //------------//
    /** Subclass of Text, dedicated to a chord marker. */
    public static class ChordText
            extends Text
    {
        //~ Constructors -------------------------------------------------------

        public ChordText (Sentence sentence)
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

        public TitleText (Sentence sentence)
        {
            super(sentence);
        }
    }
}
