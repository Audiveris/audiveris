//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       C h o r d s S t e p                                      //
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
package org.audiveris.omr.sheet.note;

import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UITaskList;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code ChordsStep} gathers notes into chords and handle their relationships.
 *
 * @author Hervé Bitteur
 */
public class ChordsStep
        extends AbstractSystemStep<Void>
{

    private static final Logger logger = LoggerFactory.getLogger(ChordsStep.class);

    /** All impacting classes. */
    private static final Set<Class<?>> impactingClasses;

    static {
        // Inters
        impactingClasses = new HashSet<>();
        impactingClasses.add(AbstractBeamInter.class);
        impactingClasses.add(HeadInter.class);
        impactingClasses.add(StemInter.class);

        // Relations
        impactingClasses.add(BeamStemRelation.class);
        impactingClasses.add(HeadStemRelation.class);
    }

    /**
     * Creates a new ChordsStep object.
     */
    public ChordsStep ()
    {
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
            throws StepException
    {
        // Gather all system notes (heads & rests) into chords
        new ChordsBuilder(system).buildHeadChords();

        // Verify beam-chord connections
        final ChordsLinker linker = new ChordsLinker(system);
        linker.checkBeamChords();

        // Handle chord relationships with other symbols within the same system
        linker.linkChords();
    }

    //--------//
    // impact //
    //--------//
    /**
     * {@inheritDoc}.
     * <p>
     * For CHORDS step, in seq argument, we can have either:
     * <ul>
     * <li>Beam created/removed or linked with stem
     * <li>Head created/removed or linked with stem
     * <li>Stem created/removed
     * </ul>
     *
     * @param seq    the sequence of UI tasks
     * @param opKind which operation is done on seq
     */
    @Override
    public void impact (UITaskList seq,
                        UITask.OpKind opKind)
    {
        logger.debug("CHORDS impact {} {}", opKind, seq);

        // Determine impacted measure
        final SIGraph sig = seq.getSig();
        Measure measure = null;

        for (Inter inter : seq.getInters()) {
            measure = getImpactedMeasure(inter);

            if (measure != null) {
                break;
            }
        }

        if (measure == null) {
            for (Relation rel : seq.getRelations()) {
                if (sig.containsEdge(rel)) {
                    measure = getImpactedMeasure(sig.getEdgeSource(rel));

                    if (measure != null) {
                        break;
                    }

                    measure = getImpactedMeasure(sig.getEdgeTarget(rel));

                    if (measure != null) {
                        break;
                    }
                }
            }
        }

        if (measure != null) {
            logger.debug("CHORDS impact on {}", measure);
            BeamGroupInter.populateMeasure(measure, false); // False for checkGroupSplit
        }
    }

    //--------------//
    // isImpactedBy //
    //--------------//
    @Override
    public boolean isImpactedBy (Class<?> classe)
    {
        return isImpactedBy(classe, impactingClasses);
    }

    //--------------------//
    // getImpactedMeasure //
    //--------------------//
    private Measure getImpactedMeasure (Inter inter)
    {
        if (inter == null) {
            return null;
        }

        final SystemInfo system = inter.getSig().getSystem();
        final Point center = inter.getCenter();

        if (center == null) {
            return null;
        }

        final MeasureStack stack = system.getStackAt(center);

        if (stack == null) {
            return null;
        }

        final Staff staff = inter.getStaff();

        if (staff != null) {
            return stack.getMeasureAt(staff);
        }

        Part part = inter.getPart();

        if (part != null) {
            return stack.getMeasureAt(part);
        }

        return null;
    }
}
