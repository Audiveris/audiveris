//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M e a s u r e s S t e p                                     //
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.score.Page;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code MeasuresStep} allocates the measures from the relevant bar lines.
 *
 * @author Hervé Bitteur
 */
public class MeasuresStep
        extends AbstractSystemStep<Void>
{

    private static final Logger logger = LoggerFactory.getLogger(MeasuresStep.class);

    /** All impacting classes. */
    private static final Set<Class<?>> impactingClasses;

    static {
        impactingClasses = new HashSet<>();
        impactingClasses.add(StaffBarlineInter.class);
    }

    /**
     * Creates a new {@code MeasuresStep} object.
     */
    public MeasuresStep ()
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

            // Determine impacted measure stack
            Point centerLeft = oneBar.getCenterLeft();
            MeasureStack stack = system.getStackAt(centerLeft);
            final boolean isAddition = isAddition(seq);

            if ((!isAddition && (opKind != UITask.OpKind.UNDO))
                        || (isAddition && (opKind == UITask.OpKind.UNDO))) {
                // Remove barlines
                MeasureStack rightStack = stack.getNextSibling();

                if (rightStack != null) {
                    logger.info("Merging {} with right", stack);
                    stack.mergeWithRight(rightStack);
                    system.removeStack(rightStack);
                }
            } else {
                // Insert barlines
                logger.info("Splitting stack at {}", oneBar);

                // Retrieve all involved barlines
                List<PartBarline> systemBarline = buildFromStaffBarlines(staffBarlines);
                MeasureStack leftStack = stack.splitAtBarline(systemBarline);
                leftStack.setExpectedDuration(stack.getExpectedDuration());
            }

            // Renumber measures
            for (Page page : system.getSheet().getPages()) {
                page.numberMeasures();
            }
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

    //------------------------//
    // buildFromStaffBarlines //
    //------------------------//
    /**
     * Build the proper list of PartBarline that corresponds to the StaffBarlines in seq.
     * <p>
     * Assumption: all staffBarlines must be present and in proper order!
     *
     * @param staffBarlines list of staffBarline instances
     * @return the "system" barline construction
     */
    private List<PartBarline> buildFromStaffBarlines (List<Inter> staffBarlines)
    {
        final List<PartBarline> systemBarline = new ArrayList<>();

        Part lastPart = null;
        PartBarline partBarline = null;
        Staff lastStaff = null;

        for (Inter inter : staffBarlines) {
            StaffBarlineInter staffBarline = (StaffBarlineInter) inter;
            Staff staff = staffBarline.getStaff();
            Part part = staff.getPart();

            if (part != lastPart) {
                partBarline = new PartBarline();
                systemBarline.add(partBarline);
                lastPart = part;
            }

            if (staff != lastStaff) {
                partBarline.addStaffBarline(staffBarline);
                lastStaff = staff;
            }
        }

        return systemBarline;
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
}
