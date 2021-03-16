//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           I n t e r s                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.math.GeoOrder;
import static org.audiveris.omr.math.GeoOrder.BY_ABSCISSA;
import static org.audiveris.omr.math.GeoOrder.BY_ORDINATE;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.util.Entities;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Class {@code Inters} gathers utilities on inter instances.
 *
 * @author Hervé Bitteur
 */
public abstract class Inters
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /**
     * Comparator to put members first and ensembles last.
     */
    public static final Comparator<Inter> membersFirst = (Inter o1, Inter o2) -> {
        if (o1 instanceof InterEnsemble) {
            if (o2 instanceof InterEnsemble) {
                if (((InterEnsemble) o1).getMembers().contains(o2)) {
                    return 1;
                }

                if (((InterEnsemble) o2).getMembers().contains(o1)) {
                    return -1;
                }

                return 0;
            }

            return 1;
        } else {
            if (o2 instanceof InterEnsemble) {
                return -1;
            }

            return 0;
        }
    };

    /**
     * For comparing interpretations by id.
     */
    public static final Comparator<Inter> byId = (Inter i1, Inter i2)
            -> Integer.compare(i1.getId(), i2.getId());

    /**
     * For comparing interpretations by left abscissa.
     */
    public static final Comparator<Inter> byAbscissa = (Inter i1, Inter i2)
            -> Integer.compare(i1.getBounds().x, i2.getBounds().x);

    /**
     * For comparing interpretations by center abscissa.
     */
    public static final Comparator<Inter> byCenterAbscissa = (Inter i1, Inter i2)
            -> Integer.compare(i1.getCenter().x, i2.getCenter().x);

    /**
     * For comparing interpretations by reverse center abscissa.
     */
    public static final Comparator<Inter> byReverseCenterAbscissa = (Inter i1, Inter i2)
            -> Integer.compare(i2.getCenter().x, i1.getCenter().x);

    /**
     * For comparing interpretations by center ordinate.
     */
    public static final Comparator<Inter> byCenterOrdinate = (Inter i1, Inter i2)
            -> Integer.compare(i1.getCenter().y, i2.getCenter().y);

    /**
     * For comparing interpretations by reverse center ordinate.
     */
    public static final Comparator<Inter> byReverseCenterOrdinate = (Inter i1, Inter i2)
            -> Integer.compare(i2.getCenter().y, i1.getCenter().y);

    /**
     * For comparing interpretations by right abscissa.
     */
    public static final Comparator<Inter> byRightAbscissa = (Inter i1, Inter i2) -> {
        Rectangle b1 = i1.getBounds();
        Rectangle b2 = i2.getBounds();

        return Integer.compare(b1.x + b1.width, b2.x + b2.width);
    };

    /**
     * For comparing interpretations by abscissa, ensuring that only identical
     * interpretations are found equal.
     * This comparator can thus be used for a TreeSet.
     */
    public static final Comparator<Inter> byFullAbscissa = (Inter o1, Inter o2) -> {
        if (o1 == o2) {
            return 0;
        }

        Point loc1 = o1.getBounds().getLocation();
        Point loc2 = o2.getBounds().getLocation();

        // Are x values different?
        int dx = loc1.x - loc2.x;

        if (dx != 0) {
            return dx;
        }

        // Vertically aligned, so use ordinates
        int dy = loc1.y - loc2.y;

        if (dy != 0) {
            return dy;
        }

        // Finally, use id ...
        return Integer.compare(o1.getId(), o2.getId());
    };

    /**
     * For comparing interpretations by abscissa, ensuring that only identical
     * interpretations are found equal.
     * This comparator can thus be used for a TreeSet.
     */
    public static final Comparator<Inter> byFullCenterAbscissa = (Inter o1, Inter o2) -> {
        if (o1 == o2) {
            return 0;
        }

        Point loc1 = o1.getCenter();
        Point loc2 = o2.getCenter();

        // Are x values different?
        int dx = loc1.x - loc2.x;

        if (dx != 0) {
            return dx;
        }

        // Vertically aligned, so use ordinates
        int dy = loc1.y - loc2.y;

        if (dy != 0) {
            return dy;
        }

        // Finally, use id ...
        return Integer.compare(o1.getId(), o2.getId());
    };

    /**
     * For comparing interpretations by ordinate.
     */
    public static final Comparator<Inter> byOrdinate = (Inter i1, Inter i2)
            -> Integer.compare(i1.getBounds().y, i2.getBounds().y);

    /**
     * For comparing interpretations by reverse width.
     */
    public static final Comparator<Inter> byReverseWidth = (Inter i1, Inter i2)
            -> Integer.compare(i2.getBounds().width, i1.getBounds().width);

    /**
     * For comparing interpretations by increasing grade.
     */
    public static final Comparator<Inter> byGrade = (Inter i1, Inter i2)
            -> Double.compare(i1.getGrade(), i2.getGrade());

    /**
     * For comparing interpretations by decreasing grade.
     */
    public static final Comparator<Inter> byReverseGrade = (Inter i1, Inter i2)
            -> Double.compare(i2.getGrade(), i1.getGrade());

    /**
     * For comparing interpretations by best grade.
     */
    public static final Comparator<Inter> byBestGrade = (Inter i1, Inter i2)
            -> Double.compare(i1.getBestGrade(), i2.getBestGrade());

    /**
     * For comparing interpretations by decreasing best grade.
     */
    public static final Comparator<Inter> byReverseBestGrade = (Inter i1, Inter i2)
            -> Double.compare(i2.getBestGrade(), i1.getBestGrade());

    /**
     * For comparing inter instances by decreasing mean grade.
     */
    public static final Comparator<Collection<? extends Inter>> byReverseMeanGrade
            = (Collection<? extends Inter> c1, Collection<? extends Inter> c2)
            -> Double.compare(getMeanGrade(c2), getMeanGrade(c1));

    /**
     * For comparing inter instances by decreasing mean contextual grade.
     */
    public static final Comparator<Collection<? extends Inter>> byReverseMeanContextualGrade
            = (Collection<? extends Inter> c1, Collection<? extends Inter> c2)
            -> Double.compare(getMeanBestGrade(c2), getMeanBestGrade(c1));

    //~ Constructors -------------------------------------------------------------------------------
    /** Not meant to be instantiated. */
    private Inters ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // getBounds //
    //-----------//
    /**
     * Return the bounding box of a collection of Inter instances.
     *
     * @param inters the provided collection of inter instances
     * @return the bounding contour
     */
    public static Rectangle getBounds (Collection<? extends Inter> inters)
    {
        Rectangle box = null;

        for (Inter inter : inters) {
            if (box == null) {
                box = new Rectangle(inter.getBounds());
            } else {
                box.add(inter.getBounds());
            }
        }

        return box;
    }

    //-----------------//
    // getClosestChord //
    //-----------------//
    /**
     * From a provided Inter collection, report the one with the lowest euclidean
     * distance to the provided point.
     *
     * @param inters the collection of inters to browse
     * @param point  the provided point
     * @return the closest inter
     */
    public static Inter getClosestInter (Collection<? extends Inter> inters,
                                         Point2D point)
    {
        Inter bestInter = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Inter inter : inters) {
            final Point center = inter.getCenter();
            final double dx = center.x - point.getX();
            final double dy = center.y - point.getY();
            final double distSq = (dx * dx) + (dy * dy);

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestInter = inter;
            }
        }

        return bestInter;
    }

    //------------------//
    // getMeanBestGrade //
    //------------------//
    /**
     * Report the average of inters best grade value.
     *
     * @param col inters collection
     * @return average best grade, null if collection is empty
     */
    public static Double getMeanBestGrade (Collection<? extends Inter> col)
    {
        if (col.isEmpty()) {
            return null;
        }

        double sum = 0;

        for (Inter inter : col) {
            sum += inter.getBestGrade();
        }

        return sum / col.size();
    }

    //----------------------------//
    // computeMeanContextualGrade //
    //----------------------------//
    /**
     * Compute contextual grade of each collection member, then report the mean value.
     *
     * @param col inters collection
     * @return mean contextual grade, or null if no value could be computed
     */
    public static Double computeMeanContextualGrade (Collection<? extends Inter> col)
    {
        double sum = 0;
        int count = 0;

        for (Inter inter : col) {
            final SIGraph sig = inter.getSig();

            if ((sig != null) && sig.containsVertex(inter)) {
                sum += sig.computeContextualGrade(inter);
                count++;
            }
        }

        if (count > 0) {
            return sum / count;
        } else {
            return null;
        }
    }

    //--------------//
    // getMeanGrade //
    //--------------//
    /**
     * Report the average grade of provided inters.
     *
     * @param col inters collection
     * @return average grade
     */
    public static Double getMeanGrade (Collection<? extends Inter> col)
    {
        if (col.isEmpty()) {
            return null;
        }

        double sum = 0;

        for (Inter inter : col) {
            sum += inter.getGrade();
        }

        return sum / col.size();
    }

    //---------------//
    // hasGoodMember //
    //---------------//
    /**
     * Check whether the provided collection of Inter instance contains at least one
     * good inter.
     *
     * @param inters the collection to check
     * @return true if a good inter was found
     */
    public static boolean hasGoodMember (Collection<? extends Inter> inters)
    {
        for (Inter inter : inters) {
            if (inter.isGood()) {
                return true;
            }
        }

        return false;
    }

    //-----//
    // ids //
    //-----//
    /**
     * Report a string of inter IDs.
     *
     * @param label  a string to introduce the list of IDs
     * @param inters collection of inters
     * @return string of IDs
     */
    public static String ids (String label,
                              Collection<? extends Inter> inters)
    {
        return Entities.ids(label, inters);
    }

    //-----//
    // ids //
    //-----//
    /**
     * Report a string of inter IDs.
     *
     * @param inters collection of inters
     * @return string of IDs
     */
    public static String ids (Collection<? extends Inter> inters)
    {
        return Entities.ids(inters);
    }

    //--------//
    // inters //
    //--------//
    /**
     * Lookup for interpretations for which the provided predicate applies within the
     * provided collection.
     *
     * @param collection the collection of inters to browse
     * @param predicate  the predicate to apply, or null
     * @return the list of compliant interpretations, perhaps empty but not null
     */
    public static List<Inter> inters (Collection<? extends Inter> collection,
                                      Predicate<Inter> predicate)
    {
        return collection.stream()
                .filter(inter -> predicate == null || predicate.test(inter))
                .collect(Collectors.toList());
    }

    //--------//
    // inters //
    //--------//
    /**
     * Lookup for interpretations of the specified class within the provided collection.
     *
     * @param collection the provided collection to browse
     * @param classe     the class to search for
     * @return the interpretations of desired class, perhaps empty but not null
     */
    public static List<Inter> inters (Collection<? extends Inter> collection,
                                      final Class classe)
    {
        return inters(collection, new ClassPredicate(classe));
    }

    //--------//
    // inters //
    //--------//
    /**
     * Select in the provided collection the inters that relate to the specified staff.
     *
     * @param staff  the specified staff
     * @param inters the collection to filter
     * @return the list of interpretations, perhaps empty but not null
     */
    public static List<Inter> inters (Collection<? extends Inter> inters,
                                      Staff staff)
    {
        List<Inter> filtered = new ArrayList<>();

        for (Inter inter : inters) {
            if (inter.getStaff() == staff) {
                filtered.add(inter);
            }
        }

        return filtered;
    }

    //-------------------//
    // intersectedInters //
    //-------------------//
    /**
     * Look up the provided list of interpretations for those whose bounds intersect the
     * given area.
     *
     * @param inters the list of interpretations to search
     * @param order  if the list is already sorted by some order, this may speed up the search
     * @param area   the intersecting area
     * @return the list of intersected interpretations found, perhaps empty but not null
     */
    public static List<Inter> intersectedInters (List<Inter> inters,
                                                 GeoOrder order,
                                                 Area area)
    {
        final List<Inter> found = new ArrayList<>();
        final Rectangle bounds = area.getBounds();
        final double xMax = bounds.getMaxX();
        final double yMax = bounds.getMaxY();

        for (Inter inter : inters) {
            if (inter.isRemoved()) {
                continue;
            }

            final Rectangle iBox = inter.getBounds();

            if (area.intersects(iBox)) {
                found.add(inter);
            } else {
                switch (order) {
                case BY_ABSCISSA:

                    if (iBox.x > xMax) {
                        return found;
                    }

                    break;

                case BY_ORDINATE:

                    if (iBox.y > yMax) {
                        return found;
                    }

                    break;

                case NONE:
                }
            }
        }

        return found;
    }

    //-------------------//
    // intersectedInters //
    //-------------------//
    /**
     * Lookup the provided list of interpretations for those whose bounds
     * intersect the given box.
     *
     * @param inters the list of interpretations to search for
     * @param order  if the list is already sorted by some order, this may speedup the search
     * @param box    the intersecting box
     * @return the intersected interpretations found, perhaps empty but not null
     */
    public static List<Inter> intersectedInters (List<? extends Inter> inters,
                                                 GeoOrder order,
                                                 Rectangle box)
    {
        List<Inter> found = new ArrayList<>();
        int xMax = (box.x + box.width) - 1;
        int yMax = (box.y + box.height) - 1;

        for (Inter inter : inters) {
            if (inter.isRemoved()) {
                continue;
            }

            Rectangle iBox = inter.getBounds();

            if (box.intersects(iBox)) {
                found.add(inter);
            } else if ((order == BY_ABSCISSA) && (iBox.x > xMax)) {
                break;
            } else if ((order == BY_ORDINATE) && (iBox.y > yMax)) {
                break;
            }
        }

        return found;
    }

    //~ Inner classes ------------------------------------------------------------------------------
    //----------------//
    // ClassPredicate //
    //----------------//
    /**
     * Predicate to filter Inter instance of a certain class (or subclass thereof).
     */
    public static class ClassPredicate
            implements Predicate<Inter>
    {

        private final Class classe;

        /**
         * Create class predicate
         *
         * @param classe Filtering class
         */
        public ClassPredicate (Class classe)
        {
            this.classe = classe;
        }

        @Override
        public boolean test (Inter inter)
        {
            return !inter.isRemoved() && (classe.isInstance(inter));
        }
    }

    //------------------//
    // ClassesPredicate //
    //------------------//
    /**
     * Predicate to filter Inter instance of provided classes.
     */
    public static class ClassesPredicate
            implements Predicate<Inter>
    {

        private final Class[] classes;

        /**
         * Create classes predicate
         *
         * @param classes filtering classes
         */
        public ClassesPredicate (Class[] classes)
        {
            this.classes = classes;
        }

        @Override
        public boolean test (Inter inter)
        {
            for (Class classe : classes) {
                if (classe.isInstance(inter)) {
                    return true;
                }
            }

            return false;
        }
    }
}
