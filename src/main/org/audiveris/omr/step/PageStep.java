//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         P a g e S t e p                                        //
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
package org.audiveris.omr.step;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.score.MeasureFixer;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.PageReduction;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.SystemManager;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Voices;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SameTimeRelation;
import org.audiveris.omr.sig.relation.SameVoiceRelation;
import org.audiveris.omr.sig.relation.SeparateTimeRelation;
import org.audiveris.omr.sig.relation.SeparateVoiceRelation;
import org.audiveris.omr.sig.ui.InterTask;
import org.audiveris.omr.sig.ui.PageTask;
import org.audiveris.omr.sig.ui.RelationTask;
import org.audiveris.omr.sig.ui.StackTask;
import org.audiveris.omr.sig.ui.SystemMergeTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UITask.OpKind;
import org.audiveris.omr.sig.ui.UITaskList;
import org.audiveris.omr.text.TextRole;
import static org.audiveris.omr.util.VerticalSide.BOTTOM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class {@code PageStep} handles connections between systems in a page.
 * <ul>
 * <li>Duplicated inters between systems are resolved.</li>
 * <li>Physical system Part instances are abstracted into LogicalPart instances.</li>
 * <li>Slurs are connected across systems.</li>
 * <li>Tied voices.</li>
 * <li>Refined lyric syllables.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class PageStep
        extends AbstractStep
{

    private static final Logger logger = LoggerFactory.getLogger(PageStep.class);

    /** Classes that may impact voices. */
    private static final Set<Class> forVoices;

    /** Classes that may impact lyrics. */
    private static final Set<Class> forLyrics;

    /** Classes that may impact slurs. */
    private static final Set<Class> forSlurs;

    /** Classes that may impact parts. */
    private static final Set<Class> forParts;

    /** Classes that may impact measures. */
    private static final Set<Class> forMeasures;

    /** All impacting classes. */
    private static final Set<Class> impactingClasses;

    static {
        forVoices = new HashSet<>();
        // Inters
        forVoices.add(AugmentationDotInter.class);
        forVoices.add(BarlineInter.class);
        forVoices.add(BeamHookInter.class);
        forVoices.add(BeamInter.class);
        forVoices.add(FlagInter.class);
        forVoices.add(HeadChordInter.class);
        forVoices.add(HeadInter.class);
        forVoices.add(MeasureStack.class);
        forVoices.add(RestChordInter.class);
        forVoices.add(RestInter.class);
        forVoices.add(SlurInter.class);
        forVoices.add(StemInter.class);
        forVoices.add(StaffBarlineInter.class);
        forVoices.add(TimeNumberInter.class);
        forVoices.add(TimePairInter.class);
        forVoices.add(TimeWholeInter.class);
        forVoices.add(TupletInter.class);
        // Relations
        forVoices.add(AugmentationRelation.class);
        forVoices.add(SameTimeRelation.class);
        forVoices.add(SameVoiceRelation.class);
        forVoices.add(SeparateTimeRelation.class);
        forVoices.add(SeparateVoiceRelation.class);
        forVoices.add(DoubleDotRelation.class);
        // Tasks
        forVoices.add(SystemMergeTask.class);
    }

    static {
        forLyrics = new HashSet<>();
        forLyrics.add(LyricItemInter.class);
        forLyrics.add(LyricLineInter.class);
    }

    static {
        forSlurs = new HashSet<>();
        forSlurs.add(SlurInter.class);
    }

    static {
        forParts = new HashSet<>();
        forParts.add(SentenceInter.class);
    }

    static {
        forMeasures = new HashSet<>();
        // Inters
        forMeasures.add(BarlineInter.class);
        forMeasures.add(StaffBarlineInter.class);
        // Tasks
        forMeasures.add(SystemMergeTask.class);
    }

    static {
        impactingClasses = new HashSet<>();
        impactingClasses.addAll(forVoices);
        impactingClasses.addAll(forLyrics);
        impactingClasses.addAll(forSlurs);
        impactingClasses.addAll(forParts);
        impactingClasses.addAll(forMeasures);
    }

    /**
     * Creates a new {@code PageStep} object.
     */
    public PageStep ()
    {
    }

    //------//
    // doit //
    //------//
    @Override
    public void doit (Sheet sheet)
            throws StepException
    {
        // Check gutter between systems one under the other
        checkSystemGutters(sheet);

        for (Page page : sheet.getPages()) {
            // Connect parts across systems in the page
            new PageReduction(page).reduce();

            // Inter-system slurs connections
            page.connectOrphanSlurs(true); // True for tie checking

            // Lyrics
            refineLyrics(page);

            // Merge / renumber measure stacks within the page
            new MeasureFixer().process(page);

            // Refine voices IDs (and thus colors) across all systems of the page
            Voices.refinePage(page);
        }
    }

    //--------//
    // impact //
    //--------//
    @Override
    public void impact (UITaskList seq,
                        OpKind opKind)
    {
        logger.debug("PAGE impact {} {}", opKind, seq);

        final SIGraph sig = seq.getSig();

        // First, determine what will be impacted
        Map<Page, Impact> map = new LinkedHashMap<>();

        for (UITask task : seq.getTasks()) {
            if (task instanceof InterTask) {
                InterTask interTask = (InterTask) task;
                Inter inter = interTask.getInter();
                Page page = inter.getSig().getSystem().getPage();
                Impact impact = map.get(page);

                if (impact == null) {
                    map.put(page, impact = new Impact());
                }

                Class classe = inter.getClass();

                if (isImpactedBy(classe, forParts)) {
                    if (inter instanceof SentenceInter) {
                        SentenceInter sentence = (SentenceInter) inter;

                        if (sentence.getRole() == TextRole.PartName) {
                            impact.onParts = true;
                        }
                    }
                }

                if (isImpactedBy(classe, forSlurs)) {
                    impact.onSlurs = true;
                }

                if (isImpactedBy(classe, forLyrics)) {
                    impact.onLyrics = true;
                }

                if (isImpactedBy(classe, forVoices)) {
                    impact.onVoices = true;
                }

                if (isImpactedBy(classe, forMeasures)) {
                    impact.onMeasures = true;
                }
            } else if (task instanceof StackTask) {
                MeasureStack stack = ((StackTask) task).getStack();
                Class classe = stack.getClass();
                Page page = stack.getSystem().getPage();
                Impact impact = map.get(page);

                if (impact == null) {
                    map.put(page, impact = new Impact());
                }

                if (isImpactedBy(classe, forVoices)) {
                    impact.onVoices = true;
                }
            } else if (task instanceof PageTask) {
                Page page = ((PageTask) task).getPage();
                Impact impact = map.get(page);

                if (impact == null) {
                    map.put(page, impact = new Impact());
                }

                impact.onParts = true; // Safer
                impact.onMeasures = true;
                impact.onVoices = true;
            } else if (task instanceof SystemMergeTask) {
                Page page = ((SystemMergeTask) task).getSystem().getPage();
                Impact impact = map.get(page);

                if (impact == null) {
                    map.put(page, impact = new Impact());
                }

                impact.onParts = true;
                impact.onMeasures = true;
                impact.onVoices = true;
            } else if (task instanceof RelationTask) {
                RelationTask relationTask = (RelationTask) task;
                Relation relation = relationTask.getRelation();
                Class classe = relation.getClass();

                if (isImpactedBy(classe, forVoices)) {
                    Inter source = relationTask.getSource();
                    SystemInfo system = sig.getSystem();
                    MeasureStack stack = system.getStackAt(source.getCenter());
                    Page page = stack.getSystem().getPage();
                    Impact impact = map.get(page);

                    if (impact == null) {
                        map.put(page, impact = new Impact());
                    }

                    impact.onVoices = true;
                }
            }
        }

        logger.debug("map: {}", map);

        // Second, handle each page impact
        for (Entry<Page, Impact> entry : map.entrySet()) {
            Page page = entry.getKey();
            Impact impact = entry.getValue();

            if (impact.onParts) {
                new PageReduction(page).reduce();
            }

            if (impact.onMeasures) {
                new MeasureFixer().process(page);
            }

            if (impact.onSlurs) {
                page.connectOrphanSlurs(true); // True for tie checking
            }

            if (impact.onLyrics) {
                refineLyrics(page);
            }

            if (impact.onVoices) {
                Voices.refinePage(page);
            }
        }
    }

    //--------------//
    // isImpactedBy //
    //--------------//
    @Override
    public boolean isImpactedBy (Class classe)
    {
        return isImpactedBy(classe, impactingClasses);
    }

    //--------------//
    // refineLyrics //
    //--------------//
    /**
     * Refine syllables across systems in page
     *
     * @param page provided page
     */
    private void refineLyrics (Page page)
    {
        for (SystemInfo system : page.getSystems()) {
            for (Inter inter : system.getSig().inters(LyricLineInter.class)) {
                LyricLineInter line = (LyricLineInter) inter;
                line.refineLyricSyllables();
            }
        }
    }

    //--------------------//
    // checkSystemGutters //
    //--------------------//
    /**
     * Check all inter-systems gutters to resolve 'duplicated' inters.
     *
     * @param sheet containing sheet
     */
    private void checkSystemGutters (Sheet sheet)
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
     *
     * @param system1 system above
     * @param system2 system below
     */
    private void checkGutter (SystemInfo system1,
                              SystemInfo system2)
    {
        final Area gutter = new Area(system1.getArea());
        gutter.intersect(system2.getArea());

        final List<Inter> inters1 = getGutterInters(system1, gutter);
        final List<Inter> inters2 = getGutterInters(system2, gutter);

        final Staff staff1 = system1.getLastStaff();
        final Staff staff2 = system2.getFirstStaff();

        Loop1:
        for (Inter inter1 : inters1) {
            final Rectangle box1 = inter1.getBounds();
            final Glyph g1 = inter1.getGlyph();
            final double d1 = Math.abs(staff1.distanceTo(inter1.getCenter()));

            for (Inter inter2 : inters2) {
                final Rectangle box2 = inter2.getBounds();

                if (box1.intersects(box2)) {
                    logger.debug("Gutter {}/{} {} vs {}", system1.getId(), system2.getId(),
                                 inter1, inter2);
                    final Glyph g2 = inter2.getGlyph();

                    if ((g1 != null) && (g1 == g2)) {
                        final double d2 = Math.abs(staff2.distanceTo(inter2.getCenter()));

                        if (d1 <= d2) {
                            logger.debug("Removing {}", inter2);
                            inter2.remove();
                        } else {
                            logger.debug("Removing {}", inter1);
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

    //--------//
    // Impact //
    //--------//
    private static class Impact
    {

        boolean onParts = false;

        boolean onSlurs = false;

        boolean onLyrics = false;

        boolean onVoices = false;

        boolean onMeasures = false;

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("PageImpact{");
            sb.append("parts:").append(onParts);
            sb.append(" slurs:").append(onSlurs);
            sb.append(" lyrics:").append(onLyrics);
            sb.append(" voices:").append(onVoices);
            sb.append(" measures:").append(onMeasures);
            sb.append("}");

            return sb.toString();
        }
    }
}
