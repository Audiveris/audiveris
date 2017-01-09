//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S I G r a p h                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.sig;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import static org.audiveris.omr.math.GeoOrder.*;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.header.StaffHeader;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.BasicExclusion;
import org.audiveris.omr.sig.relation.Exclusion;
import org.audiveris.omr.sig.relation.Exclusion.Cause;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.Support;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Predicate;

import org.jgrapht.Graphs;
import org.jgrapht.graph.Multigraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code SIGraph} represents the Symbol Interpretation Graph that aims at
 * finding the best global interpretation of all symbols in a system.
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(SigValue.Adapter.class)
public class SIGraph
        extends Multigraph<Inter, Relation>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SIGraph.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Dedicated system. */
    @Navigable(false)
    private SystemInfo system;

    /** Content for differed populating after unmarshalling. */
    private SigValue sigValue;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SIGraph object at system level.
     *
     * @param system the containing system
     */
    public SIGraph (SystemInfo system)
    {
        super(Relation.class);

        Objects.requireNonNull(system, "A sig needs a non-null system");
        this.system = system;
    }

    /**
     * Special creation of a SIG to be later populated via the provided SigValue.
     *
     * @param sigValue the SIG content, with IDREFs not yet filled
     */
    SIGraph (SigValue sigValue)
    {
        this();
        this.sigValue = sigValue;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private SIGraph ()
    {
        super(Relation.class);
        this.system = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // addVertex //
    //-----------//
    /**
     * {@inheritDoc}
     * <p>
     * Overridden so that all interpretations keep a pointer to their hosting sig.
     *
     * @param inter the brand new interpretation
     * @return true if the inter was actually added, false if it existed before
     */
    @Override
    public boolean addVertex (Inter inter)
    {
        inter.undelete();

        // Update sig
        boolean res = super.addVertex(inter);
        inter.setSig(this);

        // Update index
        if (inter.getId() == 0) {
            system.getSheet().getInterIndex().register(inter);
        } else {
            system.getSheet().getInterIndex().insert(inter);
        }

        return res;
    }

    //-------------//
    // afterReload //
    //-------------//
    /**
     * Complete SIG reload now that it's safe to use the (fully unmarshalled) sigValue.
     *
     * @param system the system for this sig
     */
    public void afterReload (SystemInfo system)
    {
        try {
            this.system = system;
            sigValue.populateSig(this);
            sigValue = null; // SigValue is no longer useful and can be disposed of

            // Link members to their ensemble
            for (Inter inter : inters(InterEnsemble.class)) {
                InterEnsemble ensemble = (InterEnsemble) inter;

                for (Inter member : ensemble.getMembers()) {
                    if (member.getEnsemble() == null) {
                        member.setEnsemble(ensemble);
                    } else {
                        logger.warn("Ensemble already set for member {}", member);
                    }
                }
            }

            // Link inters to their related staff
            final StaffManager mgr = system.getSheet().getStaffManager();

            for (Inter inter : vertexSet()) {
                Staff.StaffHolder.checkStaffHolder(inter, mgr);
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //------------------------//
    // computeContextualGrade //
    //------------------------//
    public double computeContextualGrade (Inter inter)
    {
        final List<Support> supports = getSupports(inter);
        final double cg = supports.isEmpty() ? inter.getGrade()
                : computeContextualGrade(inter, supports);
        inter.setContextualGrade(cg);

        return cg;
    }

    //-----------------//
    // containedInters //
    //-----------------//
    /**
     * Lookup the sig collection of interpretations for those which are contained in the
     * provided rectangle.
     *
     * @param rect the containing rectangle
     * @return the contained interpretations
     */
    public List<Inter> containedInters (Rectangle rect)
    {
        List<Inter> found = new ArrayList<Inter>();

        for (Inter inter : vertexSet()) {
            if (rect.contains(inter.getBounds())) {
                found.add(inter);
            }
        }

        return found;
    }

    //------------------//
    // containingInters //
    //------------------//
    /**
     * Lookup the sig collection of interpretations for those which contain the provided
     * point.
     *
     * @param point provided point
     * @return the containing interpretations
     */
    public List<Inter> containingInters (Point point)
    {
        List<Inter> found = new ArrayList<Inter>();

        for (Inter inter : vertexSet()) {
            Rectangle bounds = inter.getBounds();

            if ((bounds != null) && bounds.contains(point)) {
                // More precise test if we know inter area
                Area area = inter.getArea();

                if ((area == null) || area.contains(point)) {
                    found.add(inter);
                }
            }
        }

        return found;
    }

    //---------------//
    // contextualize //
    //---------------//
    /**
     * (Re)compute the contextual grade of all inters based on their supporting partners.
     */
    public void contextualize ()
    {
        for (Inter inter : vertexSet()) {
            computeContextualGrade(inter);
        }
    }

    //--------------//
    // deleteInters //
    //--------------//
    public void deleteInters (Collection<? extends Inter> inters)
    {
        for (Inter inter : inters) {
            inter.delete();
        }
    }

    //------------------//
    // deleteWeakInters //
    //------------------//
    /**
     * Purge the inter instances for which the contextual grade is lower than minimum
     * threshold.
     *
     * @return the set of inter instances purged
     */
    public Set<Inter> deleteWeakInters ()
    {
        Set<Inter> removed = new HashSet<Inter>();

        for (Inter inter : vertexSet()) {
            if (inter.getContextualGrade() < Inter.minContextualGrade) {
                // Staff headers are preserved, even with low grade
                Staff staff = inter.getStaff();

                if (staff != null) {
                    StaffHeader header = staff.getHeader();

                    if ((header.clef == inter) || (header.key == inter) || (header.time == inter)) {
                        if (inter.isVip()) {
                            logger.info("VIP header {} preserved", inter);
                        }

                        continue;
                    }
                }

                if (inter.isVip()) {
                    logger.info("VIP deleted weak {}", inter);
                }

                removed.add(inter);
            }
        }

        deleteInters(removed);

        return removed;
    }

    //------------//
    // exclusions //
    //------------//
    /**
     * Report the set of exclusion relations currently present in SIG
     *
     * @return the set of exclusions
     */
    public Set<Relation> exclusions ()
    {
        Set<Relation> exclusions = new HashSet<Relation>();

        for (Relation rel : edgeSet()) {
            if (rel instanceof Exclusion) {
                exclusions.add(rel);
            }
        }

        return exclusions;
    }

    //--------------//
    // getExclusion //
    //--------------//
    /**
     * Report the (first found) exclusion if any between the two provided inters.
     *
     * @param i1 an inter
     * @param i2 another inter
     * @return the (first) exclusion if any
     */
    public Exclusion getExclusion (Inter i1,
                                   Inter i2)
    {
        Set<Relation> exclusions = getExclusions(i1);
        exclusions.retainAll(getExclusions(i2));

        if (exclusions.isEmpty()) {
            return null;
        } else {
            return (Exclusion) exclusions.iterator().next();
        }
    }

    //---------------//
    // getExclusions //
    //---------------//
    /**
     * Report the set of conflicting relations the provided inter is involved in.
     *
     * @param inter the provided interpretation
     * @return the set of exclusions that involve inter, perhaps empty but not null
     */
    public Set<Relation> getExclusions (Inter inter)
    {
        return getRelations(inter, Exclusion.class);
    }

    //------------------//
    // getOppositeInter //
    //------------------//
    /**
     * Report the opposite inter across the given relation of the provided inter
     *
     * @param inter    one side of the relation
     * @param relation the relation to cross
     * @return the vertex at the opposite side of the relation
     */
    public Inter getOppositeInter (Inter inter,
                                   Relation relation)
    {
        return Graphs.getOppositeVertex(this, relation, inter);
    }

    //---------------//
    // getPartitions //
    //---------------//
    /**
     * Report all largest partitions of non-conflicting inters within the provided
     * collection of interpretations.
     *
     * @param focus  the inter instance, if any, for which partners are looked up
     * @param inters the provided collection of interpretations, with perhaps some mutual exclusion
     *               relations.
     * @return all the possible consistent partitions, with no pair of conflicting interpretations
     *         in the same partition
     */
    public List<List<Inter>> getPartitions (Inter focus,
                                            List<Inter> inters)
    {
        Collections.sort(inters, Inter.byReverseGrade);

        final int n = inters.size();
        final List<Inter> stems = (focus instanceof HeadInter) ? stemsOf(inters) : null;
        final List<List<Inter>> result = new ArrayList<List<Inter>>();

        // Map inter -> concurrents of inter (that appear later within the provided list)
        List<Set<Integer>> concurrentSets = new ArrayList<Set<Integer>>();
        boolean conflictDetected = false;

        for (int i = 0; i < n; i++) {
            Inter inter = inters.get(i);
            Set<Integer> concurrents = new HashSet<Integer>();
            concurrentSets.add(concurrents);

            for (Relation rel : getExclusions(inter)) {
                Inter concurrent = getOppositeInter(inter, rel);

                // Check whether this concurrent belongs to (and appears later in) the inters list
                int ic = inters.indexOf(concurrent);

                if (ic > i) {
                    concurrents.add(ic);
                    conflictDetected = true;
                }
            }

            //TODO: this is a hack that should be removed when
            // multiple stems for a head are correctly filtered out.
            // We assume that the various stems are potential partners of the focused head
            // and thus all stems are concurrent of one another
            if (focus instanceof HeadInter && inter instanceof StemInter) {
                // Flag all other stems, if any, as concurrents of this one
                for (Inter stem : stems) {
                    int ic = inters.indexOf(stem);

                    if (ic > i) {
                        concurrents.add(ic);
                        conflictDetected = true;
                    }
                }
            }
        }

        // If no conflict was detected, the provided collection is a single partition
        if (!conflictDetected) {
            result.add(inters);

            return result;
        }

        // Define all possible sequences
        List<Sequence> seqs = new ArrayList<Sequence>();
        seqs.add(new Sequence(n));

        for (int i = 0; i < n; i++) {
            Set<Integer> concurrents = concurrentSets.get(i);

            for (int is = 0, isBreak = seqs.size(); is < isBreak; is++) {
                Sequence seq = seqs.get(is);

                if (seq.line[i] != -1) {
                    seq.line[i] = 1;

                    if (!concurrents.isEmpty()) {
                        // Duplicate line
                        Sequence newSeq = seq.copy();
                        newSeq.line[i] = 0;
                        seqs.add(newSeq);

                        // Forbid dependent locations
                        for (Integer ic : concurrents) {
                            seq.line[ic] = -1;
                        }
                    }
                }
            }
        }

        // Build resulting partitions
        for (Sequence seq : seqs) {
            List<Inter> list = new ArrayList<Inter>();

            for (int i = 0; i < n; i++) {
                if (seq.line[i] == 1) {
                    list.add(inters.get(i));
                }
            }

            result.add(list);
        }

        return result;
    }

    //-------------//
    // getRelation //
    //-------------//
    /**
     * Report the first relation if any of desired class between the provided source and
     * target vertices.
     *
     * @param source provided source
     * @param target provided target
     * @param classe desired class of relation
     * @return the existing relation if any, or null
     */
    public Relation getRelation (Inter source,
                                 Inter target,
                                 Class classe)
    {
        for (Relation rel : getAllEdges(source, target)) {
            if (classe.isInstance(rel)) {
                return rel;
            }
        }

        return null;
    }

    //--------------//
    // getRelations //
    //--------------//
    /**
     * Report the set of relations of desired class out of the provided relations
     *
     * @param relations the provided relation collection
     * @param classe    the desired class of relation
     * @return the set of filtered relations, perhaps empty but not null
     */
    public static Set<Relation> getRelations (Collection<? extends Relation> relations,
                                              Class classe)
    {
        Set<Relation> found = new LinkedHashSet<Relation>();

        for (Relation rel : relations) {
            if (classe.isInstance(rel)) {
                found.add(rel);
            }
        }

        return found;
    }

    //--------------//
    // getRelations //
    //--------------//
    /**
     * Report the set of relations of desired class the provided inter is involved in.
     *
     * @param inter  the provided interpretation
     * @param classe the desired class of relation
     * @return the set of involving relations, perhaps empty but not null
     */
    public Set<Relation> getRelations (Inter inter,
                                       Class classe)
    {
        Set<Relation> relations = new LinkedHashSet<Relation>();

        for (Relation rel : edgesOf(inter)) {
            if (classe.isInstance(rel)) {
                relations.add(rel);
            }
        }

        return relations;
    }

    //--------------//
    // getRelations //
    //--------------//
    /**
     * Report the set of relations of desired classes the provided inter is involved in.
     *
     * @param inter   the provided interpretation
     * @param classes the desired classes of relation
     * @return the set of involving relations, perhaps empty but not null
     */
    public Set<Relation> getRelations (Inter inter,
                                       Class... classes)
    {
        Set<Relation> relations = new LinkedHashSet<Relation>();

        for (Relation rel : edgesOf(inter)) {
            for (Class classe : classes) {
                if (classe.isInstance(rel)) {
                    relations.add(rel);
                }
            }
        }

        return relations;
    }

    //-------------//
    // getSupports //
    //-------------//
    /**
     * Report the set of supporting relations the provided inter is involved in.
     *
     * @param inter the provided interpretation
     * @return set of supporting relations for inter, maybe empty but not null
     */
    public List<Support> getSupports (Inter inter)
    {
        List<Support> supports = new ArrayList<Support>();

        for (Relation rel : edgesOf(inter)) {
            if (rel instanceof Support) {
                supports.add((Support) rel);
            }
        }

        return supports;
    }

    //--------//
    // system //
    //--------//
    /**
     * @return the related system
     */
    public SystemInfo getSystem ()
    {
        return system;
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
     * @return the list of compliant interpretations
     */
    public static List<Inter> inters (Collection<? extends Inter> collection,
                                      Predicate<Inter> predicate)
    {
        List<Inter> found = new ArrayList<Inter>();

        for (Inter inter : collection) {
            if ((predicate == null) || predicate.check(inter)) {
                found.add(inter);
            }
        }

        return found;
    }

    //--------//
    // inters //
    //--------//
    /**
     * Select in the provided collection the inters that relate to the specified staff.
     *
     * @param staff  the specified staff
     * @param inters the collection to filter
     * @return the list of interpretations
     */
    public static List<Inter> inters (Staff staff,
                                      Collection<? extends Inter> inters)
    {
        List<Inter> filtered = new ArrayList<Inter>();

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
     * Lookup the provided list of interpretations for those whose related glyph
     * intersect the given box.
     *
     * @param inters the list of interpretations to search for
     * @param order  if the list is already sorted by some order, this may speedup the search
     * @param box    the intersecting box
     * @return the intersected interpretations found
     */
    public static List<Inter> intersectedInters (List<? extends Inter> inters,
                                                 GeoOrder order,
                                                 Rectangle box)
    {
        List<Inter> found = new ArrayList<Inter>();
        int xMax = (box.x + box.width) - 1;
        int yMax = (box.y + box.height) - 1;

        for (Inter inter : inters) {
            if (inter.isDeleted()) {
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

    //-------------------//
    // intersectedInters //
    //-------------------//
    /**
     * Lookup the provided list of interpretations for those whose related glyph
     * intersect the given area.
     *
     * @param inters the list of interpretations to search for
     * @param order  if the list is already sorted by some order, this may speedup the search
     * @param area   the intersecting area
     * @return the intersected interpretations found
     */
    public static List<Inter> intersectedInters (List<Inter> inters,
                                                 GeoOrder order,
                                                 Area area)
    {
        List<Inter> found = new ArrayList<Inter>();
        Rectangle bounds = area.getBounds();
        double xMax = bounds.getMaxX();
        double yMax = bounds.getMaxY();

        for (Inter inter : inters) {
            if (inter.isDeleted()) {
                continue;
            }

            Rectangle iBox = inter.getBounds();

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

    //-------------//
    // hasRelation //
    //-------------//
    /**
     * Check whether the provided Inter is involved in a relation of one of the
     * provided relation classes.
     *
     * @param inter           the inter instance to check
     * @param relationClasses the provided classes
     * @return true if such relation is found, false otherwise
     */
    public boolean hasRelation (Inter inter,
                                Class... relationClasses)
    {
        for (Relation rel : edgesOf(inter)) {
            for (Class classe : relationClasses) {
                if (classe.isInstance(rel)) {
                    return true;
                }
            }
        }

        return false;
    }

    //-----------------//
    // insertExclusion //
    //-----------------//
    /**
     * Insert an exclusion relation between two provided inters, unless there is a
     * support relation between them or unless an exclusion already exists.
     * <p>
     * Nota: We always insert exclusion from lower id to higher id.
     *
     * @param inter1 provided inter #1
     * @param inter2 provided inter #2
     * @param cause  exclusion cause (for creation only)
     * @return the concrete exclusion relation, found or created
     */
    public Exclusion insertExclusion (Inter inter1,
                                      Inter inter2,
                                      Cause cause)
    {
        final boolean direct = inter1.getId() < inter2.getId();
        final Inter source = direct ? inter1 : inter2;
        final Inter target = direct ? inter2 : inter1;

        {
            // Look for existing exclusion
            Relation rel = getRelation(source, target, Exclusion.class);

            if (rel != null) {
                return (Exclusion) rel;
            }
        }

        // Check no support relation exists, in either direction
        if (getRelation(source, target, Support.class) != null) {
            return null;
        }

        if (getRelation(target, source, Support.class) != null) {
            return null;
        }

        // Do insert an exclusion
        Exclusion exc = new BasicExclusion(cause);
        addEdge(source, target, exc);

        if (inter1.isVip() || inter2.isVip()) {
            logger.info("VIP exclusion {}", exc.toLongString(this));
        }

        return exc;
    }

    //------------------//
    // insertExclusions //
    //------------------//
    /**
     * Formalize mutual exclusion within a collection of inters
     *
     * @param inters the set of inters to mutually exclude
     * @param cause  the exclusion cause
     * @return the exclusions inserted
     */
    public List<Relation> insertExclusions (List<? extends Inter> inters,
                                            Cause cause)
    {
        ///logger.warn("insertExclusions for size: {}", inters.size());
        List<Relation> exclusions = new ArrayList<Relation>();

        for (int i = 0, iBreak = inters.size(); i < iBreak; i++) {
            Inter inter = inters.get(i);

            for (Inter other : inters.subList(i + 1, inters.size())) {
                exclusions.add(insertExclusion(inter, other, cause));
            }
        }

        return exclusions;
    }

    //--------//
    // inters //
    //--------//
    /**
     * Lookup for interpretations of the provided collection of shapes.
     *
     * @param shapes the shapes to check for
     * @return the interpretations of desired shapes
     */
    public List<Inter> inters (final Collection<Shape> shapes)
    {
        return inters(new ShapesPredicate(shapes));
    }

    //--------//
    // inters //
    //--------//
    /**
     * Select the inters that relate to the specified staff.
     *
     * @param staff the specified staff
     * @return the list of selected inters
     */
    public List<Inter> inters (Staff staff)
    {
        return inters(staff, vertexSet());
    }

    //--------//
    // inters //
    //--------//
    /**
     * Lookup for interpretations for which the provided predicate applies.
     *
     * @param predicate the predicate to apply, or null
     * @return the list of compliant interpretations
     */
    public List<Inter> inters (Predicate<Inter> predicate)
    {
        return inters(vertexSet(), predicate);
    }

    //--------//
    // inters //
    //--------//
    /**
     * Lookup for interpretations of the provided class.
     *
     * @param classe the class to search for
     * @return the interpretations of desired class
     */
    public List<Inter> inters (final Class classe)
    {
        return inters(new ClassPredicate(classe));
    }

    //--------//
    // inters //
    //--------//
    /**
     * Lookup for interpretations of the specified class within the provided collection.
     *
     * @param collection the provided collection to browse
     * @param classe     the class to search for
     * @return the interpretations of desired class
     */
    public List<Inter> inters (Collection<? extends Inter> collection,
                               final Class classe)
    {
        return inters(collection, new ClassPredicate(classe));
    }

    //--------//
    // inters //
    //--------//
    /**
     * Lookup for interpretations of the provided shape.
     *
     * @param shape the shape to check for
     * @return the interpretations of desired shape
     */
    public List<Inter> inters (final Shape shape)
    {
        return inters(new ShapePredicate(shape));
    }

    //--------//
    // inters //
    //--------//
    /**
     * Lookup for interpretations of the provided classes.
     *
     * @param classes array of desired classes
     * @return the interpretations of desired classes
     */
    public List<Inter> inters (final Class[] classes)
    {
        return inters(new ClassesPredicate(classes));
    }

    //--------//
    // inters //
    //--------//
    /**
     * Lookup for interpretations of the provided class, attached to the specified staff.
     *
     * @param staff  the specified staff
     * @param classe the class to search for
     * @return the list of interpretations found
     */
    public List<Inter> inters (final Staff staff,
                               final Class classe)
    {
        return inters(new StaffClassPredicate(staff, classe));
    }

    //-----------//
    // noSupport //
    //-----------//
    /**
     * Check for no existing support relation between the provided inters, regardless
     * of their order.
     *
     * @param one an inter
     * @param two another inter
     * @return true if no support exists between them, in either direction
     */
    public boolean noSupport (Inter one,
                              Inter two)
    {
        Set<Relation> rels = new HashSet<Relation>();
        rels.addAll(getAllEdges(one, two));
        rels.addAll(getAllEdges(two, one));

        for (Relation rel : rels) {
            if (rel instanceof Support) {
                return false;
            }
        }

        return true;
    }

    //---------//
    // publish //
    //---------//
    /**
     * Convenient method to publish an Inter instance.
     *
     * @param inter the inter to publish (can be null)
     */
    public void publish (final Inter inter)
    {
        system.getSheet().getInterIndex().publish(inter);
    }

    //------------------//
    // reduceExclusions //
    //------------------//
    /**
     * Reduce the provided exclusions as much as possible by removing the source or
     * target vertex of lower contextual grade.
     * <p>
     * Strategy is as follows:<ol>
     * <li>Pick up among all current exclusions the one whose high inter has the highest contextual
     * grade contribution among all exclusions,</li>
     * <li>Remove the weaker inter in this chosen exclusion relation,</li>
     * <li>Recompute all impacted contextual grades values,</li>
     * <li>Iterate until no more exclusion is left.</li>
     * </ol>
     *
     * @param exclusions the collection of exclusions to process
     * @return the set of vertices removed
     */
    public Set<Inter> reduceExclusions (Collection<? extends Relation> exclusions)
    {
        final Set<Inter> removed = new HashSet<Inter>();
        Relation bestRel;

        do {
            // Choose exclusion with the highest source or target grade
            double bestCP = 0;
            bestRel = null;

            for (Iterator<? extends Relation> it = exclusions.iterator(); it.hasNext();) {
                Relation rel = it.next();

                if (containsEdge(rel)) {
                    final double cp = Math.max(
                            getEdgeSource(rel).getBestGrade(),
                            getEdgeTarget(rel).getBestGrade());

                    if (bestCP < cp) {
                        bestCP = cp;
                        bestRel = rel;
                    }
                } else {
                    it.remove();
                }
            }

            // Remove the weaker branch of the selected exclusion
            if (bestRel != null) {
                final Inter source = getEdgeSource(bestRel);
                final double scp = source.getBestGrade();
                final Inter target = getEdgeTarget(bestRel);
                final double tcp = target.getBestGrade();
                final Inter weaker = (scp < tcp) ? source : target;

                if (weaker.isVip()) {
                    logger.info(
                            "VIP conflict {} deleting weaker {}",
                            bestRel.toLongString(this),
                            weaker);
                }

                // Which inters were involved in some support relation with this weaker inter?
                final Set<Inter> involved = involvedInters(getSupports(weaker));
                involved.remove(weaker);

                // Remove the weaker inter
                removed.add(weaker);
                weaker.delete();

                // If removal of weaker has resulted in removal of its ensemble, count ensemble
                if ((weaker.getEnsemble() != null) && weaker.getEnsemble().isDeleted()) {
                    removed.add(weaker.getEnsemble());
                }

                // Update contextual values for all inters that were involved with 'weaker'
                for (Inter inter : involved) {
                    computeContextualGrade(inter);
                }

                exclusions.remove(bestRel);
            }
        } while (bestRel != null);

        return removed;
    }

    //------------------//
    // reduceExclusions //
    //------------------//
    /**
     * Reduce each exclusion in the SIG.
     *
     * @return the set of reduced inters
     */
    public Set<Inter> reduceExclusions ()
    {
        return reduceExclusions(exclusions());
    }

    //--------------//
    // removeVertex //
    //--------------//
    @Override
    public boolean removeVertex (Inter inter)
    {
        if (!inter.isDeleted()) {
            logger.error("Do not use removeVertex() directly. Use inter.delete() instead.");
            throw new IllegalStateException("Do not use removeVertex() directly");
        }

        // Remove from inter index
        system.getSheet().getInterIndex().remove(inter);

        return super.removeVertex(inter);
    }

    //--------------//
    // sortBySource //
    //--------------//
    /**
     * Sort the provided list of relations by decreasing contextual grade of the
     * relations sources.
     *
     * @param rels the relations to sort
     */
    public void sortBySource (List<Relation> rels)
    {
        Collections.sort(
                rels,
                new Comparator<Relation>()
        {
            @Override
            public int compare (Relation r1,
                                Relation r2)
            {
                Inter s1 = getEdgeSource(r1);
                Inter s2 = getEdgeSource(r2);

                return Double.compare(s2.getBestGrade(), s1.getBestGrade());
            }
        });
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append("S#").append(system.getId());
        sb.append(" inters:").append(vertexSet().size());
        sb.append(" relations:").append(edgeSet().size());
        sb.append("}");

        return sb.toString();
    }

    //------------------------//
    // computeContextualGrade //
    //------------------------//
    /**
     * Compute the contextual probability for a interpretation which is supported by
     * a collection of relations with partners.
     * <p>
     * It is assumed that all these supporting relations involve the inter as either a target or a
     * source, otherwise a runtime exception is thrown.
     * <p>
     * There may be mutual exclusion between some partners. In this case, we identify all partitions
     * of compatible partners and report the best resulting contextual contribution among those
     * partitions.
     *
     * @param inter    the inter whose contextual grade is to be computed
     * @param supports all supporting relations inter is involved with, some may be in conflict
     * @return the computed contextual grade
     */
    private Double computeContextualGrade (Inter inter,
                                           Collection<? extends Support> supports)
    {
        /** Collection of partners. */
        final List<Inter> partners = new ArrayList<Inter>();

        /** Map: partner -> contribution. */
        final Map<Inter, Double> partnerContrib = new HashMap<Inter, Double>();

        // Check inter involvement
        for (Support support : supports) {
            final Inter partner;
            final double ratio;

            if (inter == getEdgeTarget(support)) {
                ratio = support.getTargetRatio();
                partner = getEdgeSource(support);
            } else if (inter == getEdgeSource(support)) {
                ratio = support.getSourceRatio();
                partner = getEdgeTarget(support);
            } else {
                throw new RuntimeException("No common interpretation");
            }

            if (ratio > 1) {
                partners.add(partner);
                partnerContrib.put(partner, partner.getGrade() * (ratio - 1));
            }
        }

        // Check for mutual exclusion between partners
        final List<List<Inter>> seqs = getPartitions(inter, partners);
        double bestCg = 0;

        for (List<Inter> seq : seqs) {
            double contribution = 0;

            for (Inter partner : seq) {
                contribution += partnerContrib.get(partner);
            }

            bestCg = Math.max(bestCg, GradeUtil.contextual(inter.getGrade(), contribution));
        }

        return bestCg;
    }

    //----------------//
    // involvedInters //
    //----------------//
    private Set<Inter> involvedInters (Collection<? extends Relation> relations)
    {
        Set<Inter> inters = new HashSet<Inter>();

        for (Relation rel : relations) {
            inters.add(getEdgeSource(rel));
            inters.add(getEdgeTarget(rel));
        }

        return inters;
    }

    //---------//
    // stemsOf //
    //---------//
    private List<Inter> stemsOf (List<Inter> inters)
    {
        List<Inter> stems = new ArrayList<Inter>();

        for (Inter inter : inters) {
            if (inter instanceof StemInter) {
                stems.add(inter);
            }
        }

        return stems;
    }

    //------------------//
    // supportsSeenFrom //
    //------------------//
    private String supportsSeenFrom (Inter inter,
                                     Map<Inter, Support> map,
                                     List<Inter> partners)
    {
        StringBuilder sb = new StringBuilder();

        for (Inter partner : partners) {
            if (sb.length() == 0) {
                sb.append("[");
            } else {
                sb.append(", ");
            }

            Support support = map.get(partner);
            sb.append(support.seenFrom(inter));
        }

        sb.append("]");

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer maxSupportCount = new Constant.Integer(
                "count",
                6,
                "Upper limit on number of supports used for contextual grade");
    }

    //----------------//
    // ClassPredicate //
    //----------------//
    private static class ClassPredicate
            implements Predicate<Inter>
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Class classe;

        //~ Constructors ---------------------------------------------------------------------------
        public ClassPredicate (Class classe)
        {
            this.classe = classe;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean check (Inter inter)
        {
            return !inter.isDeleted() && (classe.isInstance(inter));
        }
    }

    //------------------//
    // ClassesPredicate //
    //------------------//
    private static class ClassesPredicate
            implements Predicate<Inter>
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Class[] classes;

        //~ Constructors ---------------------------------------------------------------------------
        public ClassesPredicate (Class[] classes)
        {
            this.classes = classes;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean check (Inter inter)
        {
            for (Class classe : classes) {
                if (classe.isInstance(inter)) {
                    return true;
                }
            }

            return false;
        }
    }

    //--------------//
    // Contribution //
    //--------------//
    /**
     * Meant to sort the actual contributions brought by supporting partners.
     */
    private static class Contribution
    {
        //~ Static fields/initializers -------------------------------------------------------------

        public static Comparator<Contribution> byReverseValue = new Comparator<Contribution>()
        {
            @Override
            public int compare (Contribution o1,
                                Contribution o2)
            {
                return Double.compare(o2.value, o1.value);
            }
        };

        //~ Instance fields ------------------------------------------------------------------------
        final Inter partner; // Contributing partner

        final double value; // Concrete contribution brought by the partner

        //~ Constructors ---------------------------------------------------------------------------
        public Contribution (Inter partner,
                             double ratio)
        {
            this.partner = partner;
            value = ratio * partner.getGrade();
        }
    }

    //----------//
    // Sequence //
    //----------//
    /**
     * This class lists a sequence of interpretations statuses.
     * <p>
     * Possible status values are:<ul>
     * <li>-1: the related inter is forbidden (because of a conflict with an inter located before in
     * the sequence)</li>
     * <li>0: the related inter is not selected</li>
     * <li>1: the related inter is selected</li>
     * </ul>
     */
    private static class Sequence
    {
        //~ Instance fields ------------------------------------------------------------------------

        // The sequence of interpretations statuses
        // This line is parallel to the list of inters considered
        int[] line;

        //~ Constructors ---------------------------------------------------------------------------
        public Sequence (int n)
        {
            line = new int[n];
            Arrays.fill(line, 0);
        }

        //~ Methods --------------------------------------------------------------------------------
        public Sequence copy ()
        {
            Sequence newSeq = new Sequence(line.length);
            System.arraycopy(line, 0, newSeq.line, 0, line.length);

            return newSeq;
        }
    }

    //----------------//
    // ShapePredicate //
    //----------------//
    private static class ShapePredicate
            implements Predicate<Inter>
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Shape shape;

        //~ Constructors ---------------------------------------------------------------------------
        public ShapePredicate (Shape shape)
        {
            this.shape = shape;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean check (Inter inter)
        {
            return !inter.isDeleted() && (inter.getShape() == shape);
        }
    }

    //-----------------//
    // ShapesPredicate //
    //-----------------//
    private static class ShapesPredicate
            implements Predicate<Inter>
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Collection<Shape> shapes;

        //~ Constructors ---------------------------------------------------------------------------
        public ShapesPredicate (Collection<Shape> shapes)
        {
            this.shapes = shapes;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean check (Inter inter)
        {
            return !inter.isDeleted() && shapes.contains(inter.getShape());
        }
    }

    //---------------------//
    // StaffClassPredicate //
    //---------------------//
    private static class StaffClassPredicate
            implements Predicate<Inter>
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Staff staff;

        private final Class classe;

        //~ Constructors ---------------------------------------------------------------------------
        public StaffClassPredicate (Staff staff,
                                    Class classe)
        {
            this.staff = staff;
            this.classe = classe;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean check (Inter inter)
        {
            return !inter.isDeleted() && (inter.getStaff() == staff)
                   && ((classe == null) || classe.isInstance(inter));
        }
    }
}
