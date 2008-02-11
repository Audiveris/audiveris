//----------------------------------------------------------------------------//
//                                                                            //
//                                  T e x t                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.score.entity;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;

import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;

import omr.util.Logger;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>Text</code> handles any textual score entity
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class Text
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Text.class);

    /** The font used for text entities */
    protected static Font lyricFont = new Font(
        constants.lyricFontName.getValue(),
        Font.PLAIN,
        constants.lyricFontSize.getValue());

    /** A common graphics entity, just to get font metrics, TO BE IMPROVED! */
    protected static Graphics graphics = omr.Main.getGui()
                                                 .getFrame()
                                                 .getGraphics();

    /** Font metrics */
    protected static FontMetrics fontMetrics = graphics.getFontMetrics(
        lyricFont);

    //~ Instance fields --------------------------------------------------------

    /** The containing system part */
    protected final SystemPart systemPart;

    /** The item location (left side, base line) */
    protected final SystemPoint location;

    /** The item center */
    protected final SystemPoint center;

    /** The carried text */
    protected final String content;

    /** The bounding box of the content in the score */
    protected final SystemRectangle box;

    /** The font to display this text entity */
    protected Font displayFont;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Text //
    //------//
    /**
     * Creates a new Text object.
     *
     * @param systemPart the related system part
     * @param center the center of this text within the containing system
     * @param location the reference point of this text within the system
     * @param content the related string
     * @param box the bounding box in units
     */
    public Text (SystemPart      systemPart,
                 SystemPoint     center,
                 SystemPoint     location,
                 String          content,
                 SystemRectangle box)
    {
        this.systemPart = systemPart;
        this.center = center;
        this.location = location;
        this.content = content;
        this.box = box;

        systemPart.addText(this);

        // Proper font
        determineFont();
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // getContent //
    //------------//
    public String getContent ()
    {
        return content;
    }

    //-------------//
    // getLocation //
    //-------------//
    public SystemPoint getLocation ()
    {
        return location;
    }

    //--------------//
    // getLyricFont //
    //--------------//
    public static Font getLyricFont ()
    {
        return lyricFont;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the width in units of this text
     *
     * @return the text width in units
     */
    public int getWidth ()
    {
        return box.width;
    }

    //----------//
    // populate //
    //----------//
    /**
     * Allocate the proper score entity (or entities) that correspond to this
     * textual glyph. This word or sequence of words may be: <ul>
     * <li>a Direction</li>
     * <li>a part Name</li>
     * <li>a part Number</li>
     * <li>a Creator/li>
     * <li>a Copyright/li>
     * <li>one or several Lyric items</li>
     *
     * @param glyph the textual glyph
     * @param systemPart its containing system part
     * @param center its box center
     * @param location its starting reference wrt containing system
     */
    public static void populate (Glyph       glyph,
                                 SystemPart  systemPart,
                                 SystemPoint center,
                                 SystemPoint location)
    {
        if (glyph.getTextType() != null) {
            SystemRectangle box = systemPart.getSystem()
                                            .toSystemRectangle(
                glyph.getContourBox());

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Populating text glyph#" + glyph.getId() + " " +
                    glyph.getTextType() + " \"" + glyph.getTextContent() +
                    "\"");
            }

            String str = glyph.getTextContent();

            switch (glyph.getTextType()) {
            case Lyrics :
                LyricItem.createLyricItems(
                    glyph,
                    systemPart,
                    location,
                    str,
                    box);

                break;

            case Title :
                glyph.setTranslation(
                    new TitleText(systemPart, center, location, str, box));

                break;

            case Direction :

                Measure measure = systemPart.getMeasureAt(location);
                glyph.setTranslation(
                    new Words(
                        measure,
                        location,
                        measure.getEventChord(location),
                        glyph,
                        new DirectionText(
                            systemPart,
                            center,
                            location,
                            str,
                            box)));

                break;

            case Number :
                glyph.setTranslation(
                    new NumberText(systemPart, center, location, str, box));

                break;

            case Name :
                glyph.setTranslation(
                    new NameText(systemPart, center, location, str, box));

                break;

            case Creator :
                glyph.setTranslation(
                    new CreatorText(systemPart, center, location, str, box));

                break;

            case Rights :
                glyph.setTranslation(
                    new RightsText(systemPart, center, location, str, box));

                break;
            }
        }
    }

    //----------------//
    // getDisplayFont //
    //----------------//
    /**
     * Report the font to be used for score display
     *
     * @return properly defined and sized font for score display
     */
    public Font getDisplayFont ()
    {
        return displayFont;
    }

    //-------------//
    // getFontSize //
    //-------------//
    /**
     * Report the font size to be exported for this text
     *
     * @return the exported font size
     */
    public int getFontSize ()
    {
        return (int) Math.rint(displayFont.getSize2D() / 1.8);
    }

    //------------------//
    // getLyricFontSize //
    //------------------//
    /**
     * Report the font size to be exported for the lyrics
     *
     * @return the exported lyric font size
     */
    public static int getLyricFontSize ()
    {
        return (int) Math.rint(lyricFont.getSize2D() / 1.8);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Text ")
          .append(internalsString())
          .append(" \"")
          .append(content)
          .append("\" loc[")
          .append(location.x)
          .append(",")
          .append(location.y)
          .append("]}");

        return sb.toString();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, for inclusion in a
     * toString
     *
     * @return the string of internals
     */
    protected abstract String internalsString ();

    //---------------//
    // determineFont //
    //---------------//
    /**
     * Determine the proper font size, based on the ratio between the width of
     * the underlying glyph and the theoretical length of this text in the
     * default font
     */
    protected void determineFont ()
    {
        Rectangle2D rect = fontMetrics.getStringBounds(content, graphics);
        double      fontRatio = getWidth() / rect.getWidth();
        displayFont = lyricFont.deriveFont(
            new Float((fontRatio * lyricFont.getSize()) / 2));
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------------//
    // CreatorText //
    //-------------//
    public static class CreatorText
        extends Text
    {
        //~ Enumerations -------------------------------------------------------

        public enum CreatorType {
            //~ Enumeration constant initializers ------------------------------

            arranger,composer, lyricist,
            poet,
            transcriber,
            translator;
        }

        //~ Instance fields ----------------------------------------------------

        /** Creator type, if any */
        private CreatorType creatorType;

        //~ Constructors -------------------------------------------------------

        public CreatorText (SystemPart      systemPart,
                            SystemPoint     center,
                            SystemPoint     location,
                            String          content,
                            SystemRectangle box)
        {
            super(systemPart, center, location, content, box);
        }

        //~ Methods ------------------------------------------------------------

        public void setCreatorType (CreatorType creatorType)
        {
            this.creatorType = creatorType;
        }

        public CreatorType getCreatorType ()
        {
            return creatorType;
        }

        @Override
        protected String internalsString ()
        {
            if (creatorType != null) {
                return "creator " + creatorType;
            } else {
                return "creator";
            }
        }
    }

    //---------------//
    // DirectionText //
    //---------------//
    public static class DirectionText
        extends Text
    {
        //~ Constructors -------------------------------------------------------

        public DirectionText (SystemPart      systemPart,
                              SystemPoint     center,
                              SystemPoint     location,
                              String          content,
                              SystemRectangle box)
        {
            super(systemPart, center, location, content, box);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected String internalsString ()
        {
            return "direction";
        }
    }

    //----------//
    // NameText //
    //----------//
    public static class NameText
        extends Text
    {
        //~ Constructors -------------------------------------------------------

        public NameText (SystemPart      systemPart,
                         SystemPoint     center,
                         SystemPoint     location,
                         String          content,
                         SystemRectangle box)
        {
            super(systemPart, center, location, content, box);

            systemPart.getScorePart()
                      .setName(content);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected String internalsString ()
        {
            return "name";
        }
    }

    //------------//
    // NumberText //
    //------------//
    public static class NumberText
        extends Text
    {
        //~ Constructors -------------------------------------------------------

        public NumberText (SystemPart      systemPart,
                           SystemPoint     center,
                           SystemPoint     location,
                           String          content,
                           SystemRectangle box)
        {
            super(systemPart, center, location, content, box);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected String internalsString ()
        {
            return "number";
        }
    }

    //------------//
    // RightsText //
    //------------//
    public static class RightsText
        extends Text
    {
        //~ Constructors -------------------------------------------------------

        public RightsText (SystemPart      systemPart,
                           SystemPoint     center,
                           SystemPoint     location,
                           String          content,
                           SystemRectangle box)
        {
            super(systemPart, center, location, content, box);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected String internalsString ()
        {
            return "rights";
        }
    }

    //-----------//
    // TitleText //
    //-----------//
    public static class TitleText
        extends Text
    {
        //~ Constructors -------------------------------------------------------

        public TitleText (SystemPart      systemPart,
                          SystemPoint     center,
                          SystemPoint     location,
                          String          content,
                          SystemRectangle box)
        {
            super(systemPart, center, location, content, box);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected String internalsString ()
        {
            return "title";
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer lyricFontSize = new Constant.Integer(
            "points",
            17,
            "Standard font point size for lyrics");
        Constant.String  lyricFontName = new Constant.String(
            "Sans Serif",
            "Standard font name for lyrics");
    }
}
