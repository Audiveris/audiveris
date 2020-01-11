//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S h e e t R e d u c t i o n                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.SentenceInter;
import static org.audiveris.omr.util.VerticalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code SheetReduction} works at sheet level to reduce the duplicated inters
 * located in inter-system areas (gutters).
 * <p>
 * Since there is no reliable way to decide upfront if a glyph located between systems belongs to
 * the upper or to the lower system, both systems try to find out glyph interpretation(s) with
 * respect to the system at hand.
 * <p>
 * This can lead to duplications that can be solved only when the whole sheet processing has be
 * done, that is at PAGE step.
 *
 * @author Hervé Bitteur
 */
public class SheetReduction
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SheetReduction.class);

    /** Sheet to process. */
    private final Sheet sheet;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /**
     * Create a new {@code SheetReduction} object.
     *
     * @param sheet the sheet to reduce
     */
    public SheetReduction (Sheet sheet)
    {
        this.sheet = sheet;
        params = new Parameters(sheet.getScale());
    }

    //---------//
    // process //
    //---------//
    /**
     * Check all inter-systems gutters to resolve 'duplicated' inters.
     */
    public void process ()
    {
        final SystemManager systemMgr = sheet.getSystemManager();

        for (SystemInfo systemAbove : systemMgr.getSystems()) {
            List<SystemInfo> neighbors = systemMgr.verticalNeighbors(systemAbove, BOTTOM);

            for (SystemInfo systemBelow : neighbors) {
                checkGutter(systemAbove, systemBelow);
            }
        }
    }

    //-------------//
    // checkGutter //
    //-------------//
    /**
     * Check the gutter between the two provided systems.
     * <p>
     * This concerns the area that is below the last line of last staff in system above
     * and above the first line of first staff in system below.
     * <p>
     * Using vertical distance to staff does not work correctly for lyrics lines.
     *
     * @param system1 system above
     * @param system2 system below
     */
    private void checkGutter (SystemInfo system1,
                              SystemInfo system2)
    {
        logger.debug("--- checkGutter {}/{}", system1, system2);
        final Area gutter = new Area(system1.getArea());
        gutter.intersect(system2.getArea());

        // First, lyrics and plain sentences
        checkSentences(system1, system2, gutter);

        // Second, non ensembles
        checkInters(system1, system2, gutter);
    }

    //-------------//
    // checkInters // non-ensembles in fact
    //-------------//
    /**
     * Process remaining inters, now that lyrics/sentences have been processed.
     *
     * @param system1 upper system
     * @param system2 lower system
     * @param gutter  inter-system area
     */
    private void checkInters (SystemInfo system1,
                              SystemInfo system2,
                              Area gutter)
    {
        logger.debug("checkInters");
        final Staff staff1 = system1.getLastStaff();
        final Staff staff2 = system2.getFirstStaff();

        final List<Inter> inters1 = getGutterInters(system1, gutter);
        final List<Inter> inters2 = getGutterInters(system2, gutter);

        Loop1:
        for (Inter inter1 : inters1) {
            final Rectangle box1 = inter1.getBounds();
            final Glyph g1 = inter1.getGlyph();
            final double d1 = Math.abs(staff1.distanceTo(inter1.getCenter()));

            for (Inter inter2 : inters2) {
                final Rectangle box2 = inter2.getBounds();

                if (box1.intersects(box2)) {
                    logger.debug("{} vs {}", inter1, inter2);
                    final Glyph g2 = inter2.getGlyph();

                    if ((g1 != null) && (g1 == g2)) {
                        final double d2 = Math.abs(staff2.distanceTo(inter2.getCenter()));

                        if (d1 <= d2) {
                            logger.debug("Removing lower {}", inter2);
                            inter2.remove();
                        } else {
                            logger.debug("Removing upper {}", inter1);
                            inter1.remove();
                            continue Loop1;
                        }
                    } else {
                        logger.info("Gutter. Different glyphs {}/{} {} vs {}",
                                    system1.getId(), system2.getId(), inter1, inter2);
                    }
                }
            }
        }

    }

    //----------------//
    // checkSentences //
    //----------------//
    /**
     * Process lyrics and plain sentences.
     * <p>
     * The main difficulty with lyrics is that, with a pile of several verses, the last verse
     * can be located very far from its (correct) system above and very close to the (wrong) system
     * below.
     * <p>
     * Also, the sentences that can appear right above the lower system can be in fact legitimate
     * sentences such as directions or chord names.
     * But, from the upper system point of view, they may be perceived as yet another lyrics line.
     * <p>
     * Therefore, we need a way to separate and then dispatch these lines:
     * <ol>
     * <li> From upper system, if we have a first line of lyrics rather close (closer to upper than
     * to lower) then we begin a series of lyric verses.
     * Conflicting sentences from lower system will be discarded.
     * <li> The series will end right before the first "verse" that is both short (low number of
     * characters, comparatively to previous verses) and close to lower system.
     * This breaking line will then be discarded from the upper system.
     * </ol>
     * <p>
     * Rather that distance to closest staff line, we use distance to closest staff "timing
     * inters line" which wraps the staff-related inters like chords, tuplets, beams,
     * that is all inters that can be located between staff line and lyrics line.
     *
     * @param system1 upper system
     * @param system2 lower system
     * @param gutter  inter-system area
     */
    private void checkSentences (SystemInfo system1,
                                 SystemInfo system2,
                                 Area gutter)
    {
        logger.debug("checkSentences");
        final Staff staff1 = system1.getLastStaff();
        final Staff staff2 = system2.getFirstStaff();

        final LineInfo line1 = staff1.getLastLine();
        final int maxDy1 = staff1.getPart().getMaximumDistance(BOTTOM);

        final LineInfo line2 = staff2.getFirstLine();
        final int maxDy2 = staff2.getPart().getMaximumDistance(TOP);

        final List<Inter> sentences1 = getGutterSentences(system1, gutter);
        Collections.sort(sentences1, Inters.byCenterOrdinate);

        final List<Inter> sentences2 = getGutterSentences(system2, gutter);
        Collections.sort(sentences2, Inters.byReverseCenterOrdinate);

        boolean lyricsStarted = false;

        for (Inter inter1 : sentences1) {
            SentenceInter sen1 = (SentenceInter) inter1;

            // Check for a good lyrics line
            // If so, exclude conflicting sentences from system2
            final int lg1 = sen1.getLength();
            final Point center1 = sen1.getCenter();

            if (!lyricsStarted) {
                if (lg1 >= constants.lyricsLowerLength.getValue()) {
                    lyricsStarted = true;
                    removeCompetingSentences(sen1, sentences2);
                } else {
                    break;
                }
            } else {
                if (lg1 < constants.lyricsLowerLength.getValue()) {
                    double dTo2 = Math.abs(staff2.distanceTo(center1) - maxDy2);

                    if (dTo2 < params.minDyToOther) {
                        // This is the end of lyrics serie
                        int index = sentences1.indexOf(inter1);

                        for (Inter in : sentences1.subList(index, sentences1.size())) {
                            logger.debug("Removing upper {}", sen1);
                            in.remove();
                        }

                        break;
                    }
                }

                // Continuing lyrics serie
                removeCompetingSentences(sen1, sentences2);
            }
        }
    }

    //-----------------//
    // getGutterInters //
    //-----------------//
    /**
     * Report all inters of provided system whose center lies within provided area.
     *
     * @param system containing system
     * @param area   provided area
     * @return list of found inters, perhaps empty
     */
    private List<Inter> getGutterInters (SystemInfo system,
                                         Area area)
    {
        List<Inter> found = new ArrayList<>();

        for (Inter inter : system.getSig().vertexSet()) {
            if (!(inter instanceof InterEnsemble) && area.contains(inter.getCenter())) {
                found.add(inter);
            }
        }

        Collections.sort(found, Inters.byCenterAbscissa);

        return found;
    }

    //--------------------//
    // getGutterSentences //
    //--------------------//
    /**
     * Report all sentences of provided system whose center lies within provided area.
     *
     * @param system containing system
     * @param area   provided area
     * @return list of found sentences (lyrics or plain sentences), perhaps empty
     */
    private List<Inter> getGutterSentences (SystemInfo system,
                                            Area area)
    {
        List<Inter> found = new ArrayList<>();

        for (Inter inter : system.getSig().vertexSet()) {
            if ((inter instanceof SentenceInter) && area.contains(inter.getCenter())) {
                found.add(inter);
            }
        }

        return found;
    }

    //---------//
    // overlap //
    //---------//
    /**
     * Check if two sentences overlap, that is at least one word of a sentence overlaps
     * a word of the other sentence.
     *
     * @param s1 one sentence
     * @param s2 another sentence
     * @return true if so
     */
    private boolean overlap (SentenceInter s1,
                             SentenceInter s2)
    {
        for (Inter m1 : s1.getMembers()) {
            Rectangle b1 = m1.getBounds();

            for (Inter m2 : s2.getMembers()) {
                if (b1.intersects(m2.getBounds())) {
                    return true;
                }
            }
        }

        return false;
    }

    //--------------------------//
    // removeCompetingSentences //
    //--------------------------//
    /**
     * Remove the sentences that overlap the provided sentence
     *
     * @param sen       the sentence to keep
     * @param sentences the collection of sentences to check
     */
    private void removeCompetingSentences (SentenceInter sen,
                                           List<Inter> sentences)
    {
        final Rectangle box1 = sen.getBounds();

        for (Inter in2 : sentences) {
            final SentenceInter sen2 = (SentenceInter) in2;
            final Rectangle box2 = sen2.getBounds();

            if (box1.intersects(box2)) {
                // Make sure these two sentences do overlap
                if (overlap(sen, sen2)) {
                    logger.debug("Removing lower {}", sen2);
                    sen2.remove();
                }
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer lyricsLowerLength = new Constant.Integer(
                "Chars",
                10,
                "Minimum count of characters for a good lyrics line");

        private final Scale.Fraction minDyToOther = new Scale.Fraction(
                4,
                "Minimum vertical distance to other system border");
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {

        final int minDyToOther;

        Parameters (Scale scale)
        {
            minDyToOther = scale.toPixels(constants.minDyToOther);
        }
    }
}
