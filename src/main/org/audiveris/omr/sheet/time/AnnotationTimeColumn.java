//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A n n o t a t i o n T i m e C o l u m n                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright ©  Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.classifier.Annotation;
import org.audiveris.omr.classifier.AnnotationIndex;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omrdataset.api.OmrShape;
import org.audiveris.omrdataset.api.OmrShapes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class meant to handle a column of annotation-based time signatures in a system.
 * <p>
 * No distinction is made between header and rest of staff.
 *
 * @author Hervé Bitteur
 */
public class AnnotationTimeColumn
        extends TimeColumn
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AnnotationTimeColumn.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Relevant time symbols found in column. */
    final Set<Inter> timeSet = new LinkedHashSet<Inter>();

    /** Global skew. */
    final Skew skew;

    /** Mean de-skewed abscissa. */
    Double meanDskX;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AnnotationTimeColumn} object.
     *
     * @param system  underlying system
     * @param timeSet initial collection of time items
     */
    public AnnotationTimeColumn (SystemInfo system,
                                 Collection<Inter> timeSet)
    {
        super(system);
        this.skew = system.getSheet().getSkew();

        if (timeSet != null) {
            this.timeSet.addAll(timeSet);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addInter //
    //----------//
    /**
     * Include a time item in the time column.
     *
     * @param inter a whole or partial time item
     */
    public void addInter (Inter inter)
    {
        timeSet.add(inter);

        // Invalidate cache
        meanDskX = null;
    }

    //-------------//
    // getMeanDskX //
    //-------------//
    /**
     * Report the mean de-skewed abscissa computed on all time items of the column.
     *
     * @return the mean de-skewed abscissa
     */
    public Double getMeanDskX ()
    {
        if (meanDskX == null) {
            if (!timeSet.isEmpty()) {
                double sum = 0;

                for (Inter inter : timeSet) {
                    Point center = inter.getCenter();
                    sum += skew.deskewed(center).getX();
                }

                meanDskX = sum / timeSet.size();
            }
        }

        return meanDskX;
    }

    @Override
    protected TimeBuilder allocateBuilder (Staff staff)
    {
        return new AnnotationTimeBuilder(staff, this);
    }

    @Override
    protected void cleanup ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------------------//
    // AnnotationRetriever //
    //---------------------//
    public static class AnnotationRetriever
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final SystemInfo system;

        private final SIGraph sig;

        /** Time inters gathered by staff, and sorted by abscissa. */
        private final Map<Staff, List<Inter>> interMap = new TreeMap<Staff, List<Inter>>(
                Staff.byId);

        /** Columns detected so far. */
        private final List<AnnotationTimeColumn> columns = new ArrayList<AnnotationTimeColumn>();

        //~ Constructors ---------------------------------------------------------------------------
        public AnnotationRetriever (SystemInfo system)
        {
            this.system = system;
            sig = system.getSig();
        }

        //~ Methods --------------------------------------------------------------------------------
        public void process ()
        {
            // Filter the time annotations for this system to create inters
            populateInters();

            // Organize the inters per column
            buildColumns();

            // Validate each column
            validateColumns();

            logger.debug("{} Times done.", system);
        }

        //--------------//
        // buildColumns //
        //--------------//
        private void buildColumns ()
        {
            final Sheet sheet = system.getSheet();
            final Skew skew = sheet.getSkew();
            final double maxDx = TimeColumn.getMaxDxOffset(sheet);

            for (Staff staff : system.getStaves()) {
                List<Inter> staffList = interMap.get(staff);

                if (staffList == null) {
                    continue;
                }

                InterLoop:
                for (Inter inter : staffList) {
                    // Look for a suitable column
                    double xDsk = skew.deskewed(inter.getCenter()).getX();
                    AnnotationTimeColumn column = null;

                    for (AnnotationTimeColumn col : columns) {
                        if (Math.abs(col.getMeanDskX() - xDsk) <= maxDx) {
                            column = col;

                            break;
                        }
                    }

                    // No suitable column found, create a brand new one
                    if (column == null) {
                        columns.add(column = new AnnotationTimeColumn(system, null));
                    }

                    column.addInter(inter);
                }
            }

            // Sort columns by abscissa
            Collections.sort(
                    columns,
                    new Comparator<AnnotationTimeColumn>()
            {
                @Override
                public int compare (AnnotationTimeColumn c1,
                                    AnnotationTimeColumn c2)
                {
                    return Double.compare(c1.getMeanDskX(), c2.getMeanDskX());
                }
            });
        }

        //----------------//
        // populateInters //
        //----------------//
        private void populateInters ()
        {
            // Filter the time annotations for this system to create inters
            final AnnotationIndex index = system.getSheet().getAnnotationIndex();
            final Rectangle systemBounds = system.getArea().getBounds();

            for (Annotation annotation : index.getEntities()) {
                final OmrShape omrShape = annotation.getOmrShape();

                if (OmrShapes.TIMES.contains(omrShape)) {
                    // Determine staff
                    final Rectangle bounds = annotation.getBounds();

                    if (bounds.intersects(systemBounds)) {
                        final Point center = GeoUtil.centerOf(bounds);
                        final Staff staff = system.getClosestStaff(center);

                        if ((staff != null) && (staff.distanceTo(center) < 0)) {
                            // We've got the containing staff
                            final double grade = annotation.getConfidence() * Grades.intrinsicRatio;
                            final int id = annotation.getId();
                            final Inter inter;

                            if (OmrShapes.TIME_COMMONS.contains(omrShape)
                                || OmrShapes.TIME_COMBOS.contains(omrShape)) {
                                // Whole time sig
                                inter = new TimeWholeInter(id, bounds, omrShape, grade);
                            } else {
                                // Partial time sig: num or den
                                inter = TimeNumberInter.create(id, bounds, omrShape, grade, staff);
                            }

                            List<Inter> staffList = interMap.get(staff);

                            if (staffList == null) {
                                interMap.put(staff, staffList = new ArrayList<Inter>());
                            }

                            sig.addVertex(inter);
                            staffList.add(inter);
                        }
                    }
                }
            }

            // Sort each staff list per abscissa
            for (List<Inter> staffList : interMap.values()) {
                Collections.sort(staffList, Inters.byCenterAbscissa);
            }
        }

        //-----------------//
        // validateColumns //
        //-----------------//
        /**
         * Validate all the columns of annotation-based time inters.
         * <p>
         * TODO: what if a column is not valid?
         * <ol>
         * <li>Try to complete it (using patch classifier in proper locations)
         * <li>If it can't be completed, discard the whole column
         * </ol>
         */
        private void validateColumns ()
        {
            for (AnnotationTimeColumn column : columns) {
                int res = column.retrieveTime();
            }
        }
    }
}
