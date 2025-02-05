//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M e a s u r e s S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.score.Page;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.InterTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UITaskList;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class <code>MeasuresStep</code> allocates the measures from the relevant bar lines.
 *
 * @author Hervé Bitteur
 */
public class MeasuresStep
        extends AbstractSystemStep<Void>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MeasuresStep.class);

    /** All impacting classes. */
    private static final Set<Class<?>> impactingClasses;

    static {
        impactingClasses = new HashSet<>();
        impactingClasses.add(StaffBarlineInter.class);
    }

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>MeasuresStep</code> object.
     */
    public MeasuresStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------------------------//
    // buildFromStaffBarlines //
    //------------------------//
    /**
     * Build the list of PartBarline's that corresponds to the StaffBarline's in seq.
     * <p>
     * Assumption: all staffBarlines must be present, one per staff and in proper order!
     *
     * @param staffBarlines list of StaffBarline instances, to be gathered per part
     * @return the corresponding list of PartBarline instances
     */
    private List<PartBarline> buildFromStaffBarlines (List<Inter> staffBarlines)
    {
        final List<PartBarline> partBarlineList = new ArrayList<>();

        Part previousPart = null;
        PartBarline partBarline = null;

        for (Inter inter : staffBarlines) {
            final StaffBarlineInter staffBarline = (StaffBarlineInter) inter;
            final Part part = staffBarline.getStaff().getPart();

            if (part != previousPart) {
                // New part encountered, create its PartBarline
                partBarline = new PartBarline();
                partBarlineList.add(partBarline);
                previousPart = part;
            }

            // Group each StaffBarline into its PartBarline
            partBarline.addStaffBarline(staffBarline);
        }

        return partBarlineList;
    }

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Sheet sheet,
                             Void context)
        throws StepException
    {
        // Assign basic measure ids
        for (Page page : sheet.getPages()) {
            page.numberMeasures();
            page.dumpMeasureCounts();
        }
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
        throws StepException
    {
        new MeasuresBuilder(system).buildMeasures();
    }

    //--------//
    // impact //
    //--------//
    /**
     * {@inheritDoc}
     * <p>
     * For MEASURES step, in seq argument, we can have either:
     * <ul>
     * <li>BarlineInter instances
     * <li>StaffBarlineInter instances
     * </ul>
     *
     * @param seq    the sequence of UI tasks
     * @param opKind which operation is done on seq
     */
    @Override
    public void impact (UITaskList seq,
                        UITask.OpKind opKind)
    {
        logger.debug("MEASURES impact {} {}", opKind, seq);

        final List<Inter> staffBarlines = seq.getInters(StaffBarlineInter.class);

        if (!staffBarlines.isEmpty()) {
            Collections.sort(staffBarlines, Inters.byCenterOrdinate);

            final StaffBarlineInter oneBar = (StaffBarlineInter) staffBarlines.get(0);
            final SystemInfo system = oneBar.getStaff().getSystem();
            final boolean isAddition = isAddition(seq);

            // Determine impacted measure stack
            final Point centerRight = oneBar.getCenterRight();
            final Scale scale = system.getSheet().getScale();
            final int margin = scale.toPixels(StaffBarlineInter.getMaxStaffBarlineShift());
            final MeasureStack stack = system.getStackAt(centerRight, margin);

            if ((!isAddition && (opKind != UITask.OpKind.UNDO)) //
                    || (isAddition && (opKind == UITask.OpKind.UNDO))) {
                if (stack == null) {
                    // Safeguard on removal
                    if (opKind != UITask.OpKind.UNDO) {
                        logger.warn("Barlines located too far outside system.");
                    }
                } else {
                    // Remove barlines
                    final HorizontalSide side = stack.checkSystemSide(staffBarlines);

                    if (side != null) {
                        // Simply remove related PartBarlines
                        logger.info("Removing PartBarlines on {} side", side);
                        stack.removePartBarlines(staffBarlines, side);
                    } else {
                        // Merge with next stack
                        MeasureStack rightStack = stack.getNextSibling();

                        if (rightStack != null) {
                            logger.info("Merging {} with right", stack);
                            stack.mergeWithRight(rightStack);
                            system.removeStack(rightStack);
                        }
                    }
                }
            } else {
                if (stack == null) {
                    // Safeguard on insertion
                    if (opKind != UITask.OpKind.UNDO) {
                        logger.warn("Barlines located too far outside system. Please undo!");
                    }
                } else {
                    // Gather StaffBarline instances into PartBarline instances
                    final List<PartBarline> partBarlines = buildFromStaffBarlines(staffBarlines);

                    // Side insertion or split?
                    final HorizontalSide side = stack.checkSystemSide(staffBarlines);

                    if (side != null) {
                        logger.info("{}-Insertion of {}", side, oneBar);
                        stack.sideInsertBarlines(partBarlines, side);
                    } else {
                        logger.info("Insertion of {} inside {}", oneBar, stack);
                        final MeasureStack leftStack = stack.splitAtBarline(partBarlines);
                        leftStack.setExpectedDuration(stack.getExpectedDuration());
                    }
                }
            }

            // Renumber measures
            for (Page page : system.getSheet().getPages()) {
                page.numberMeasures();
            }
        }
    }

    //------------//
    // isAddition //
    //------------//
    private boolean isAddition (UITaskList seq)
    {
        for (UITask task : seq.getTasks()) {
            if (task instanceof InterTask) {
                final Inter inter = ((InterTask) task).getInter();

                if (inter instanceof StaffBarlineInter && task instanceof AdditionTask) {
                    return true;
                }
            }
        }

        return false;
    }

    //--------------//
    // isImpactedBy //
    //--------------//
    @Override
    public boolean isImpactedBy (Class<?> classe)
    {
        return isImpactedBy(classe, impactingClasses);
    }
}
