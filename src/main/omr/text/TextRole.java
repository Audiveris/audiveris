//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t R o l e                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;

import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemNode.StaffPosition;
import omr.score.entity.SystemPart;
import omr.score.entity.Text;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code TextRole} describes the role of a piece of text
 * (typically a sentence).
 *
 * @author Hervé Bitteur
 */
public enum TextRole
{

    /** No role known. */
    UnknownRole,
    /** (Part of) lyrics. */
    Lyrics,
    /** Title of the opus. */
    Title,
    /** Playing instruction. */
    Direction,
    /** Number for this opus. */
    Number,
    /** Name for the part. */
    Name,
    /** A creator (composer, etc...). */
    Creator,
    /** Copyright notice. */
    Rights,
    /** Chord mark. */
    Chord;
    //

    /** Specific application parameters. */
    private static final Constants constants = new Constants();

    /** Usual logger utility. */
    private static final Logger logger = LoggerFactory.getLogger(
            TextRole.class);

    //
    //-----------------//
    // getStringHolder //
    //-----------------//
    /**
     * Forge a string to be used in lieu of real text value.
     *
     * @param NbOfChars the number of characters desired
     *
     * @return a dummy string of NbOfChars chars
     */
    public String getStringHolder (int NbOfChars)
    {
        if (NbOfChars > 1000) {
            logger.warn("Abnormal text length:{}", NbOfChars);

            return "<<" + Integer.toString(NbOfChars) + ">>";
        } else {
            StringBuilder sb = new StringBuilder("[");

            while (sb.length() < (NbOfChars - 1)) {
                sb.append(toString())
                        .append("-");
            }

            return sb.substring(0, Math.max(NbOfChars - 1, 0)) + "]";
        }
    }

    //-----------//
    // guessRole //
    //-----------//
    /**
     * Try to infer the role of this textual item.
     * For the time being, this is a simple algorithm based on sentence location
     * within the page, augmented by valid chord name, etc.
     *
     * @param line       the sentence
     * @param systemInfo the containing system
     * @return the role information inferred for the provided sentence glyph
     */
    public static TextRoleInfo guessRole (TextLine line,
                                          SystemInfo systemInfo)
    {
        if (line == null) {
            return null;
        }

        if (line.isVip()) {
            logger.info("TextRoleInfo. guessRole for {}", line.getValue());
        }

        int chordCount = 0;

        for (TextWord word : line.getWords()) {
            // At least one word/glyph with a role manually assigned
            Glyph glyph = word.getGlyph();

            if (glyph != null) {
                if (glyph.getManualRole() != null) {
                    return glyph.getManualRole();
                }
            }

            // Word that could be a chord symbol?
            if (word.guessChordInfo() != null) {
                chordCount++;
            }
        }

        // Is line made entirely of potential chord symbols?
        boolean isAllChord = chordCount == line.getWords()
                .size();

        Rectangle box = line.getBounds();

        if (box == null) {
            return null;
        }

        // Is line mainly in italic? 
        boolean isMainlyItalic = systemInfo.getTextBuilder()
                .isMainlyItalic(line);

        Sheet sheet = systemInfo.getSheet();
        ScoreSystem system = systemInfo.getScoreSystem();
        Scale scale = system.getScale();
        Point left = new Point(box.x, box.y + (box.height / 2));
        Point right = new Point(
                box.x + box.width,
                box.y + (box.height / 2));

        // First system in page?
        boolean firstSystem = system.getId() == 1;

        // Last system in page?
        boolean lastSystem = sheet.getSystems()
                .size() == system.getId();

        // Vertical position wrt (system) staves
        StaffPosition systemPosition = system.getStaffPosition(left);

        // Vertical position wrt (part) staves
        SystemPart part = system.getPartAbove(left);
        StaffPosition partPosition = part.getStaffPosition(left);

        // Vertical distance from staff?
        Staff staff = system.getStaffAt(left);
        int staffDy = Math.abs(staff.getTopLeft().y - box.y);
        boolean closeToStaff = staffDy <= scale.toPixels(constants.maxStaffDy);

        // Begins on left side of the part?
        boolean leftOfStaves = system.isLeftOfStaves(left);

        // At the center of page width?
        int maxCenterDx = scale.toPixels(constants.maxCenterDx);
        int pageCenter = sheet.getWidth() / 2;
        boolean pageCentered = Math.abs((box.x + (box.width / 2)) - pageCenter) <= maxCenterDx;

        // Right aligned with staves?
        int maxRightDx = scale.toPixels(constants.maxRightDx);
        boolean rightAligned = Math.abs(
                right.x - system.getTopLeft().x - system.getDimension().width) <= maxRightDx;

        // Short Sentence?
        int maxShortLength = scale.toPixels(constants.maxShortLength);
        boolean shortSentence = box.width <= maxShortLength;

        // Tiny Sentence?
        int maxTinyLength = scale.toPixels(constants.maxTinyLength);
        boolean tinySentence = box.width <= maxTinyLength;

        // High text?
        int minTitleHeight = scale.toPixels(constants.minTitleHeight);
        boolean highText = box.height >= minTitleHeight;

        logger.debug(
                "{} firstSystem={} lastSystem={} systemPosition={}"
                + " partPosition={} closeToStaff={} leftOfStaves={}"
                + " pageCentered={} rightAligned={} shortSentence={}"
                + " highText={10}",
                box,
                firstSystem,
                lastSystem,
                systemPosition,
                partPosition,
                closeToStaff,
                leftOfStaves,
                pageCentered,
                rightAligned,
                shortSentence,
                highText);

        // Decisions ...
        switch (systemPosition) {
        case ABOVE_STAVES: // Title, Number, Creator, Direction, Chord

            if (tinySentence) {
                if (isAllChord) {
                    return new TextRoleInfo(TextRole.Chord);
                } else {
                    return new TextRoleInfo(TextRole.UnknownRole);
                }
            }

            if (firstSystem) {
                if (leftOfStaves) {
                    return new TextRoleInfo(
                            TextRole.Creator,
                            Text.CreatorText.CreatorType.lyricist);
                } else if (rightAligned) {
                    return new TextRoleInfo(
                            TextRole.Creator,
                            Text.CreatorText.CreatorType.composer);
                } else if (closeToStaff) {
                    if (isAllChord) {
                        return new TextRoleInfo(TextRole.Chord);
                    } else {
                        return new TextRoleInfo(TextRole.Direction);
                    }
                } else if (pageCentered) { // Title, Number

                    if (highText) {
                        return new TextRoleInfo(TextRole.Title);
                    } else {
                        return new TextRoleInfo(TextRole.Number);
                    }
                }
            } else {
                if (isAllChord) {
                    return new TextRoleInfo(TextRole.Chord);
                } else {
                    return new TextRoleInfo(TextRole.Direction);
                }
            }

            break;

        case WITHIN_STAVES: // Name, Lyrics, Direction

            if (leftOfStaves) {
                return new TextRoleInfo(TextRole.Name);
            } else if ((partPosition == StaffPosition.BELOW_STAVES)
                       && !isMainlyItalic) {
                return new TextRoleInfo(TextRole.Lyrics);
            } else {
                return new TextRoleInfo(TextRole.Direction);
            }

        case BELOW_STAVES: // Copyright, Lyrics for single-staff part

            if (tinySentence) {
                return new TextRoleInfo(TextRole.UnknownRole);
            }

            if (pageCentered && shortSentence && lastSystem) {
                return new TextRoleInfo(TextRole.Rights);
            }

            if (part.getStaves()
                    .size() == 1) {
                if ((partPosition == StaffPosition.BELOW_STAVES)
                    && !isMainlyItalic) {
                    return new TextRoleInfo(TextRole.Lyrics);
                }
            }
        }

        // Default
        return new TextRoleInfo(TextRole.UnknownRole);
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {

        Scale.Fraction maxRightDx = new Scale.Fraction(
                2,
                "Maximum horizontal distance on the right end of the staff");

        Scale.Fraction maxCenterDx = new Scale.Fraction(
                30,
                "Maximum horizontal distance around center of page");

        Scale.Fraction maxShortLength = new Scale.Fraction(
                35,
                "Maximum length for a short sentence (no lyrics)");

        Scale.Fraction maxTinyLength = new Scale.Fraction(
                2,
                "Maximum length for a tiny sentence (no lyrics)");

        Scale.Fraction maxStaffDy = new Scale.Fraction(
                7,
                "Maximum distance above staff for a direction");

        Scale.Fraction minTitleHeight = new Scale.Fraction(
                3,
                "Minimum height for a title text");

    }
}
