//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T i m e C o l u m n                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.time;

import java.awt.Rectangle;
import java.util.Collection;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.score.TimeValue;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.TimeTopBottomRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.audiveris.omr.sig.inter.DeletedInterException;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.util.Predicate;

/**
 * This abstract class provides the basis for management of a system-level column
 * of staff-level TimeBuilder instances since, within a system, a column of time
 * signatures must be complete and contain only identical signatures.
 * <p>
 * Subclasses:<ul>
 * <li>{@link HeaderTimeColumn} works for system header.
 * <li>{@link BasicTimeColumn} works for time signatures found outside of system header.
 * </ul>
 */
public abstract class TimeColumn
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TimeColumn.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Containing system. */
    protected final SystemInfo system;

    /** Best time value found, if any. */
    protected TimeValue timeValue;

    /** Map of time builders. (one per staff) */
    protected final Map<Staff, TimeBuilder> builders = new TreeMap<Staff, TimeBuilder>(Staff.byId);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Column} object.
     *
     * @param system the underlying system
     */
    public TimeColumn (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getMaxDxOffset //
    //----------------//
    /**
     * Report the maximum abscissa shift between de-skewed time items in column.
     *
     * @param sheet containing sheet
     * @return maximum abscissa shift
     */
    public static int getMaxDxOffset (Sheet sheet)
    {
        return sheet.getScale().toPixels(constants.maxDxOffset);
    }

    //---------------//
    // getTimeInters //
    //---------------//
    /**
     * Report the time inter instance for each staff in the column.
     *
     * @return the map: staff &rarr; time inter
     */
    public Map<Staff, AbstractTimeInter> getTimeInters ()
    {
        Map<Staff, AbstractTimeInter> times = new TreeMap<Staff, AbstractTimeInter>(Staff.byId);

        for (Map.Entry<Staff, TimeBuilder> entry : builders.entrySet()) {
            times.put(entry.getKey(), entry.getValue().getTimeInter());
        }

        return times;
    }

    //--------------//
    // retrieveTime //
    //--------------//
    /**
     * This is the main entry point for time signature, it retrieves the column of
     * staves candidates time signatures, and selects the best one at system level.
     *
     * @return 0 if valid, or -1 if invalid
     */
    public int retrieveTime ()
    {
        // Allocate one time-sig builder for each staff within system
        for (Staff staff : system.getStaves()) {
            builders.put(staff, allocateBuilder(staff));
        }

        // Process each staff on turn, to find candidates
        for (TimeBuilder builder : builders.values()) {
            // Retrieve candidates for time items
            builder.findCandidates();

            // This fails if no candidate at all is kept in staff after filtering
            if (!builder.filterCandidates()) {
                cleanup(); // Clean up what has been constructed

                return -1; // We failed to find a time sig in stack
            }
        }

        // Check vertical alignment
        purgeUnaligned();

        // Check time sig consistency at system level
        if (checkConsistency()) {
            discardNeighbors();

            return 0;
        }

        return -1; // Failed
    }

    //------------------//
    // discardNeighbors //
    //------------------//
    protected void discardNeighbors ()
    {
        final SIGraph sig = system.getSig();
        final Collection<AbstractTimeInter> times = getTimeInters().values();
        final Rectangle columnBox = Inters.getBounds(times);
        List<Inter> neighbors = sig.inters(
                new Predicate<Inter>()
        {
            @Override
            public boolean check (Inter inter)
            {
                return inter.getBounds().intersects(columnBox)
                       && !(inter instanceof InterEnsemble);
            }
        });

        // Let's not consider our own time items as overlapping neighbors
        for (AbstractTimeInter time : times) {
            if (time instanceof TimePairInter) {
                TimePairInter pair = (TimePairInter) time;
                neighbors.removeAll(pair.getMembers());
            }

            neighbors.remove(time);
        }

        for (AbstractTimeInter time : times) {
            for (Iterator<Inter> it = neighbors.iterator(); it.hasNext();) {
                Inter neighbor = it.next();

                try {
                    if (neighbor.overlaps(time)) {
                        logger.debug("Deleting time overlapping {}", neighbor);
                        neighbor.remove();
                        it.remove();
                    }
                } catch (DeletedInterException ignored) {
                }
            }
        }
    }

    /**
     * Allocate instance of proper subclass of TimeBuilder
     *
     * @param staff the dedicated staff for this builder
     * @return the created TimeBuilder instance
     */
    protected abstract TimeBuilder allocateBuilder (Staff staff);

    //------------------//
    // checkConsistency //
    //------------------//
    /**
     * Use vertical redundancy within a system column of time signatures to come up
     * with the best selection.
     * <p>
     * The selection is driven from the whole system column point of view, as follows:
     * <ol>
     * <li>For each staff, identify all the possible and supported AbstractTimeInter
     * instances, each with its own grade.</li>
     * <li>Then for each possible AbstractTimeInter value (called TimeValue), make sure it
     * appears in each staff as a AbstractTimeInter instance and assign a global grade (as
     * average of staff-based AbstractTimeInter instances for the same TimeValue).</li>
     * <li>The best system-based TimeValue is then chosen as THE time signature for this
     * system column. </li>
     * <li>All staff non compatible AbstractTimeInter instances are destroyed and the member
     * numbers that don't belong to the chosen AbstractTimeInter are destroyed.</li>
     * </ol>
     *
     * @return true if OK, false otherwise
     */
    protected boolean checkConsistency ()
    {
        // Retrieve all time values found, organized by value and staff
        Map<TimeValue, AbstractTimeInter[]> vectors = getValueVectors();
        Map<TimeValue, Double> grades = new HashMap<TimeValue, Double>();
        TimeLoop:
        for (Map.Entry<TimeValue, AbstractTimeInter[]> entry : vectors.entrySet()) {
            TimeValue time = entry.getKey();
            AbstractTimeInter[] vector = entry.getValue();

            // Check that this time is present in all staves and compute the time mean grade
            double mean = 0;

            for (Inter inter : vector) {
                if (inter == null) {
                    logger.debug(
                            "System#{} TimeValue {} not found in all staves",
                            system.getId(),
                            time);

                    continue TimeLoop;
                }

                mean += inter.getGrade(); // TODO: use contextual?????
            }

            mean /= vector.length;
            grades.put(time, mean);
        }

        logger.debug("System#{} time sig grades {}", system.getId(), grades);

        // Select the best time value at system level
        double bestGrade = 0;

        for (Map.Entry<TimeValue, Double> entry : grades.entrySet()) {
            double grade = entry.getValue();

            if (grade > bestGrade) {
                bestGrade = grade;
                timeValue = entry.getKey();
            }
        }

        if (timeValue == null) {
            return false; // Invalid column
        }

        // Forward the chosen time to each staff
        AbstractTimeInter[] bestVector = vectors.get(timeValue);
        List<Staff> staves = system.getStaves();

        for (int is = 0; is < staves.size(); is++) {
            Staff staff = staves.get(is);
            TimeBuilder builder = builders.get(staff);
            builder.createTimeSig(bestVector[is]);
            builder.discardOthers();
        }

        logger.debug("System#{} TimeSignature: {}", system.getId(), timeValue);

        return true;
    }

    /**
     * This is called when we discover that a column of candidate(s) is wrong,
     * so that all related data inserted in sig is removed.
     */
    protected abstract void cleanup ();

    /**
     * Report the system vector of values for each time value found.
     * A vector is an array, one element per staff, the element being the staff candidate
     * AbstractTimeInter for the desired time value, or null if the time value has no acceptable
     * candidate in this staff.
     *
     * @return the system vectors of candidates found, organized per TimeValue
     */
    protected Map<TimeValue, AbstractTimeInter[]> getValueVectors ()
    {
        // Retrieve all occurrences of time values across staves.
        final Map<TimeValue, AbstractTimeInter[]> values = new HashMap<TimeValue, AbstractTimeInter[]>();

        // Loop on system staves
        final List<Staff> staves = system.getStaves();

        for (int index = 0; index < staves.size(); index++) {
            final Staff staff = staves.get(index);
            final TimeBuilder builder = builders.get(staff);
            final SIGraph sig = builder.sig;

            // Whole candidate signatures, if any, in this staff
            for (Inter inter : builder.wholes) {
                AbstractTimeInter whole = (AbstractTimeInter) inter;
                TimeValue time = whole.getValue();
                AbstractTimeInter[] vector = values.get(time);

                if (vector == null) {
                    values.put(time, vector = new AbstractTimeInter[staves.size()]);
                }

                if ((vector[index] == null) || (inter.getGrade() > vector[index].getGrade())) {
                    vector[index] = whole;
                }
            }

            // Num/Den pair candidate signatures, if any
            for (Inter nInter : builder.nums) {
                TimeNumberInter num = (TimeNumberInter) nInter;

                for (Relation rel : sig.getRelations(num, TimeTopBottomRelation.class)) {
                    TimeNumberInter den = (TimeNumberInter) sig.getOppositeInter(nInter, rel);
                    TimePairInter pair = TimePairInter.createAdded(num, den);
                    TimeValue time = pair.getValue();
                    AbstractTimeInter[] vector = values.get(time);

                    if (vector == null) {
                        values.put(time, vector = new AbstractTimeInter[staves.size()]);
                    }

                    if ((vector[index] == null) || (pair.getGrade() > vector[index].getGrade())) {
                        vector[index] = pair;
                    }
                }
            }
        }

        return values;
    }

    /**
     * Check that all candidates are vertically aligned.
     * When processing system header, candidates are aligned by construction.
     * But, outside headers, candidates within the same stack have to be checked for such
     * alignment.
     */
    protected void purgeUnaligned ()
    {
        // Void by default
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction maxDxOffset = new Scale.Fraction(
                2,
                "Maximum abscissa shift between deskewed time items in a column");
    }
}
