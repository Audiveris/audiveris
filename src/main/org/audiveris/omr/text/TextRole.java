//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e x t R o l e                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.text;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.score.StaffPosition;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import static org.audiveris.omr.text.TextRole.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code TextRole} describes the role of a piece of text (typically a sentence).
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
    PartName,
    /** A creator (with no precise type). */
    Creator,
    /** A creator (arranger). */
    CreatorArranger,
    /** A creator (composer). */
    CreatorComposer,
    /** A creator (lyricist). */
    CreatorLyricist,
    /** Copyright notice. */
    Rights,
    /** Chord mark. */
    ChordName;

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TextRole.class);

    //-----------//
    // isCreator //
    //-----------//
    /**
     * Report whether this role is a creator role (perhaps with additional type info).
     *
     * @return true if a creator role
     */
    public boolean isCreator ()
    {
        return (this == CreatorArranger) || (this == CreatorComposer)
               || ((this == CreatorLyricist) || (this == Creator));
    }

    //-----------//
    // guessRole //
    //-----------//
    /**
     * Try to infer the role of this textual item.
     * <p>
     * For the time being, this is a simple algorithm based on sentence location within the sheet,
     * augmented by valid chord name, etc.
     *
     * @param line   the sentence
     * @param system the containing system
     * @return the role information inferred for the provided sentence glyph
     */
    public static TextRole guessRole (TextLine line,
                                      SystemInfo system)
    {
        if (line == null) {
            return null;
        }

        if (line.isVip()) {
            logger.info("TextRoleInfo. guessRole for {}", line.getValue());
        }

        Rectangle box = line.getBounds();

        if (box == null) {
            return null;
        }

        int chordCount = 0;

        //
        //        for (TextWord word : line.getWords()) {
        //            //            // At least one word/glyph with a role manually assigned
        //            //            Glyph glyph = word.getGlyph();
        //            //
        //            //            if (glyph != null) {
        //            //                if (glyph.getManualRole() != null) {
        //            //                    return glyph.getManualRole();
        //            //                }
        //            //            }
        //            //
        //            //
        //            //            // Word that could be a chord symbol?
        //            //            if (word.guessChordInfo() != null) {
        //            //                chordCount++;
        //            //            }
        //        }
        //
        // Is line made entirely of potential chord symbols?
        boolean isAllChord = chordCount == line.getWords().size();

        // Is line mainly in italic?
        boolean isMainlyItalic = TextBuilder.isMainlyItalic(line);

        Sheet sheet = system.getSheet();

        ///ScoreSystem system = system.getScoreSystem();
        Scale scale = sheet.getScale();
        Point left = new Point(box.x, box.y + (box.height / 2));
        Point right = new Point(box.x + box.width, box.y + (box.height / 2));

        // First system in page?
        boolean firstSystem = system.getId() == 1;

        // Last system in page?
        boolean lastSystem = sheet.getSystems().size() == system.getId();

        // Vertical position wrt (system) staves
        StaffPosition systemPosition = system.getStaffPosition(left);

        // Vertical position wrt (part) staves
        Part part = system.getPartAtOrAbove(left);
        StaffPosition partPosition = part.getStaffPosition(left);

        // Vertical distance from staff?
        Staff staff = system.getClosestStaff(left);
        int staffDy = staff.distanceTo(box.getLocation());
        boolean closeToStaff = staffDy <= scale.toPixels(constants.maxStaffDy);

        // Begins on left side of the part?
        boolean leftOfStaves = left.x < system.getLeft();

        // At the center of page width?
        int maxCenterDx = scale.toPixels(constants.maxCenterDx);
        int pageCenter = sheet.getWidth() / 2;
        boolean pageCentered = Math.abs((box.x + (box.width / 2)) - pageCenter) <= maxCenterDx;

        // Right aligned with staves?
        int maxRightDx = scale.toPixels(constants.maxRightDx);
        boolean rightAligned = Math.abs(right.x - system.getRight()) <= maxRightDx;

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
                + " pageCentered={} rightAligned={} shortSentence={}" + " highText={10}",
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
        case ABOVE_STAVES: // Title, Number, Creator, Direction, ChordName

            if (tinySentence) {
                if (isAllChord) {
                    return ChordName;
                } else {
                    return UnknownRole;
                }
            }

            if (firstSystem) {
                if (leftOfStaves) {
                    return CreatorLyricist;
                } else if (rightAligned) {
                    return CreatorComposer;
                } else if (closeToStaff) {
                    if (isAllChord) {
                        return ChordName;
                    } else {
                        return Direction;
                    }
                } else if (pageCentered) { // Title, Number

                    if (highText) {
                        return Title;
                    } else {
                        return Number;
                    }
                }
            } else if (isAllChord) {
                return ChordName;
            } else {
                return Direction;
            }

            break;

        case WITHIN_STAVES: // Name, Lyrics, Direction

            if (leftOfStaves) {
                return PartName;
            } else if ((partPosition == StaffPosition.BELOW_STAVES) && !isMainlyItalic) {
                return Lyrics;
            } else if (!tinySentence) {
                return Direction;
            }

        case BELOW_STAVES: // Copyright, Lyrics for single-staff part, Direction

            if (tinySentence) {
                return UnknownRole;
            }

            if (isMainlyItalic) {
                return Direction;
            }

            if (pageCentered && shortSentence && lastSystem) {
                return Rights;
            }

            if (part.getStaves().size() == 1) {
                if ((partPosition == StaffPosition.BELOW_STAVES) && !isMainlyItalic) {
                    return Lyrics;
                }
            }
        }

        // Default
        return UnknownRole;
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {

        private final Scale.Fraction maxRightDx = new Scale.Fraction(
                2,
                "Maximum horizontal distance on the right end of the staff");

        private final Scale.Fraction maxCenterDx = new Scale.Fraction(
                30,
                "Maximum horizontal distance around center of page");

        private final Scale.Fraction maxShortLength = new Scale.Fraction(
                35,
                "Maximum length for a short sentence (no lyrics)");

        private final Scale.Fraction maxTinyLength = new Scale.Fraction(
                2,
                "Maximum length for a tiny sentence (no lyrics)");

        private final Scale.Fraction maxStaffDy = new Scale.Fraction(
                7,
                "Maximum distance above staff for a direction");

        private final Scale.Fraction minTitleHeight = new Scale.Fraction(
                3,
                "Minimum height for a title text");
    }
}
