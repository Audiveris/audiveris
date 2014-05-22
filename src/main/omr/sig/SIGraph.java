//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S I G r a p h                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.math.GeoOrder;
import static omr.math.GeoOrder.*;

import omr.selection.InterListEvent;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.sig.Exclusion.Cause;

import omr.util.Predicate;

import org.jgrapht.graph.Multigraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code SIGraph} represents the Symbol Interpretation Graph that aims at
 * finding the best global interpretation of all symbols in a system.
 * <p>
 * A special instance is created at sheet level for the time when the systems have not yet been
 * defined. This sheet-level instance has its system pointer at null.
 *
 * @author Hervé Bitteur
 */
public class SIGraph
        extends Multigraph<Inter, Relation>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SIGraph.class);

    //~ Enumerations -------------------------------------------------------------------------------
    /** Reduction mode. */
    public static enum ReductionMode
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** (Almost) all exclusions are reduced. */
        STRICT,
        /** Strong exclusions are reduced, Weak ones are left for later processing. */
        RELAXED;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Dedicated sheet */
    private final Sheet sheet;

    /** Dedicated system */
    private final SystemInfo system;

    //~ Constructors -------------------------------------------------------------------------------
    //---------//
    // SIGraph //
    //---------//
    /**
     * Creates a new SIGraph object at system level.
     *
     * @param system the containing system
     */
    public SIGraph (SystemInfo system)
    {
        this(system.getSheet(), system);
    }

    //---------//
    // SIGraph //
    //---------//
    /**
     * Creates a new SIGraph object at sheet level.
     *
     * @param sheet the containing sheet
     */
    public SIGraph (Sheet sheet)
    {
        this(sheet, null);
    }

    //---------//
    // SIGraph //
    //---------//
    /**
     * Creates a new SIGraph object.
     */
    private SIGraph (Sheet sheet,
                     SystemInfo system)
    {
        super(Relation.class);
        this.sheet = sheet;
        this.system = system;
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
        boolean res = super.addVertex(inter);
        inter.setSig(this);

        if ((system == null) || (inter.getId() == 0)) {
            sheet.getSigManager().register(inter);
        }

        return res;
    }

    //------------------------//
    // computeContextualGrade //
    //------------------------//
    public double computeContextualGrade (Inter inter,
                                          boolean logging)
    {
        final List<Support> supports = getSupports(inter);
        double cp;

        if (!supports.isEmpty()) {
            cp = computeContextualGrade(inter, supports, logging);
        } else {
            cp = inter.getGrade();
        }

        inter.setContextualGrade(cp);

        return cp;
    }

    //-----------------//
    // containedInters //
    //-----------------//
    /**
     * Lookup the sig collection of interpretations for those which
     * are contained in the provided rectangle.
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
     * Lookup the sig collection of interpretations for those which
     * contain the provided point.
     *
     * @param point provided point
     * @return the containing interpretations
     */
    public List<Inter> containingInters (Point point)
    {
        List<Inter> found = new ArrayList<Inter>();

        for (Inter inter : vertexSet()) {
            if (inter.getBounds().contains(point)) {
                // More precise test if we know inter area
                Area area = inter.getArea();

                if ((area == null) || area.contains(point)) {
                    found.add(inter);
                }
            }
        }

        return found;
    }

    //------------------//
    // deleteWeakInters //
    //------------------//
    /**
     * Purge the inter instances for which the contextual grade is
     * lower than minimum threshold.
     *
     * @return the set of inter instances purged
     */
    public Set<Inter> deleteWeakInters ()
    {
        Set<Inter> removed = new HashSet<Inter>();

        for (Inter inter : vertexSet()) {
            if (inter.getContextualGrade() < Inter.minContextualGrade) {
                if (inter.isVip()) {
                    logger.info("VIP deleted weak {}", inter);
                }

                removed.add(inter);
            }
        }

        removeAllVertices(removed);

        return removed;
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

    //----------//
    // getInter //
    //----------//
    /**
     * Report the (first) interpretation if any of desired class for
     * the glyph at hand.
     * TODO: Could we have several inters of desired class for the same glyph?
     *
     * @param glyph  the underlying glyph
     * @param classe the interpretation class desired
     * @return the existing interpretation if any, or null
     */
    public static Inter getInter (Glyph glyph,
                                  Class classe)
    {
        for (Inter inter : glyph.getInterpretations()) {
            if (classe.isAssignableFrom(inter.getClass())) {
                return inter;
            }
        }

        return null;
    }

    //-------------//
    // getPartners //
    //-------------//
    /**
     * Report all largest collections of non-conflicting partners
     * within the provided collection of interpretations.
     *
     * @param focus  the inter instance for which partners are looked up
     * @param inters the provided collection of interpretations, with perhaps
     *               some mutual exclusion relations.
     * @return all the possible consistent partner collections, with no pair
     *         of concurrent interpretations in the same collection
     */
    public List<List<Inter>> getPartners (Inter focus,
                                          List<Inter> inters)
    {
        int n = inters.size();
        Collections.sort(inters, Inter.byId);

        final List<Inter> stems = stemsOf(inters);
        final List<List<Inter>> result = new ArrayList<List<Inter>>();

        // Map inter -> concurrents of inter (within the provided collection)
        Map<Inter, Set<Inter>> map = new HashMap<Inter, Set<Inter>>();
        boolean conflict = false;

        for (Inter inter : inters) {
            Set<Inter> concurrents = new HashSet<Inter>();
            map.put(inter, concurrents);

            for (Relation rel : getExclusions(inter)) {
                Inter concurrent = (getEdgeTarget(rel) == inter) ? getEdgeSource(rel)
                        : getEdgeTarget(rel);

                if ((inter.getId() < concurrent.getId()) && inters.contains(concurrent)) {
                    concurrents.add(concurrent);
                    conflict = true;
                }
            }

            //TODO: this is a hack that should be removed when
            // multiple stems for a head are correctly filtered out.
            if (focus instanceof AbstractNoteInter && inter instanceof StemInter) {
                // Flag all other stems, if any, as concurrents of this one
                for (Inter stem : stems) {
                    if ((stem != inter) && (inter.getId() < stem.getId())) {
                        concurrents.add(stem);
                        conflict = true;
                    }
                }
            }
        }

        if (!conflict) {
            result.add(inters);

            return result;
        }

        // Define all possible sequences
        List<Sequence> seqs = new ArrayList<Sequence>();
        seqs.add(new Sequence(n));

        for (int i = 0; i < n; i++) {
            Inter inter = inters.get(i);
            Set<Inter> concurrents = map.get(inter);

            for (int is = 0, isBreak = seqs.size(); is < isBreak; is++) {
                Sequence seq = seqs.get(is);

                if (seq.line[i] != -1) {
                    if (concurrents.isEmpty()) {
                        seq.line[i] = 1;
                    } else {
                        seq.line[i] = 0;

                        // Duplicate line
                        Sequence newSeq = seq.copy();
                        newSeq.line[i] = 1;

                        for (Inter c : concurrents) {
                            // Forbid dependent locations
                            newSeq.line[inters.indexOf(c)] = -1;
                        }

                        seqs.add(newSeq);
                    }
                }
            }
        }

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
     * Report the relation if any of desired class between the provided
     * source and target vertices.
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
            if (rel.getClass().isAssignableFrom(classe)) {
                return rel;
            }
        }

        return null;
    }

    //--------------//
    // getRelations //
    //--------------//
    /**
     * Report the set of relations of desired class the provided inter
     * is involved in.
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
            if (classe.isAssignableFrom(rel.getClass())) {
                relations.add(rel);
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

    //-----------------//
    // insertExclusion //
    //-----------------//
    /**
     * Insert an exclusion relation between two provided inters, unless there is a
     * support relation between them or unless an exclusion already exists.
     * We always insert exclusion from lower id to higher id.
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
        boolean direct = inter1.getId() < inter2.getId();
        Inter source = direct ? inter1 : inter2;
        Inter target = direct ? inter2 : inter1;

        // Look for existing exclusion
        for (Relation rel : getAllEdges(source, target)) {
            if (rel instanceof Exclusion) {
                return (Exclusion) rel;
            }
        }

        // Check no support relation exists, in either direction
        for (Relation rel : getAllEdges(source, target)) {
            if (rel instanceof Support) {
                return null;
            }
        }

        for (Relation rel : getAllEdges(target, source)) {
            if (rel instanceof Support) {
                return null;
            }
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
     */
    public void insertExclusions (List<Inter> inters,
                                  Cause cause)
    {
        ///logger.warn("insertExclusions for size: {}", inters.size());
        for (int i = 0; i < (inters.size() - 1); i++) {
            Inter inter = inters.get(i);

            for (Inter other : inters.subList(i + 1, inters.size())) {
                insertExclusion(inter, other, cause);
            }
        }
    }

    //--------//
    // inters //
    //--------//
    /**
     * Lookup for interpretations for which the provided predicate
     * applies.
     *
     * @param predicate the predicate to apply, or null
     * @return the list of compliant interpretations
     */
    public List<Inter> inters (Predicate<Inter> predicate)
    {
        List<Inter> found = new ArrayList<Inter>();

        for (Inter inter : vertexSet()) {
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
     * Lookup for interpretations of the provided shape
     *
     * @param shape the shape to check for
     * @return the interpretations of desired shape
     */
    public List<Inter> inters (final Shape shape)
    {
        return inters(
                new Predicate<Inter>()
                {
                    @Override
                    public boolean check (Inter inter)
                    {
                        return !inter.isDeleted() && (inter.getShape() == shape);
                    }
                });
    }

    //--------//
    // inters //
    //--------//
    /**
     * Lookup for interpretations of the provided class
     *
     * @param classe the class to search for
     * @return the interpretations of desired class
     */
    public List<Inter> inters (final Class classe)
    {
        return inters(
                new Predicate<Inter>()
                {
                    @Override
                    public boolean check (Inter inter)
                    {
                        return !inter.isDeleted() && (classe.isAssignableFrom(inter.getClass()));
                    }
                });
    }

    //--------//
    // inters //
    //--------//
    /**
     * Lookup for interpretations of the provided collection of shapes
     *
     * @param shapes the shapes to check for
     * @return the interpretations of desired shapes
     */
    public List<Inter> inters (final Collection<Shape> shapes)
    {
        return inters(
                new Predicate<Inter>()
                {
                    @Override
                    public boolean check (Inter inter)
                    {
                        return !inter.isDeleted() && shapes.contains(inter.getShape());
                    }
                });
    }

    //-------------------//
    // intersectedGlyphs //
    //-------------------//
    /**
     * Lookup the provided list of glyph instances which intersect the
     * given box.
     *
     * @param glyphs           the list of glyph instances to search for
     * @param sortedByAbscissa true if the list is already sorted by abscissa,
     *                         in order to speedup the search
     * @param box              the intersecting box
     * @return the intersected glyph instances found
     */
    public List<Glyph> intersectedGlyphs (List<Glyph> glyphs,
                                          boolean sortedByAbscissa,
                                          Rectangle2D box)
    {
        List<Glyph> found = new ArrayList<Glyph>();

        for (Glyph glyph : glyphs) {
            Rectangle glyphBox = glyph.getBounds();

            if (box.intersects(glyphBox)) {
                found.add(glyph);
            } else if (sortedByAbscissa && (glyphBox.x >= box.getMaxX())) {
                break;
            }
        }

        return found;
    }

    //-------------------//
    // intersectedGlyphs //
    //-------------------//
    /**
     * Lookup the provided list of glyph instances which intersect the
     * given box.
     *
     * @param glyphs           the list of glyph instances to search for
     * @param sortedByAbscissa true if the list is already sorted by abscissa,
     *                         in order to speedup the search
     * @param area             the intersecting box
     * @return the intersected glyph instances found
     */
    public List<Glyph> intersectedGlyphs (List<Glyph> glyphs,
                                          boolean sortedByAbscissa,
                                          Area area)
    {
        double xMax = area.getBounds().getMaxX();
        List<Glyph> found = new ArrayList<Glyph>();

        for (Glyph glyph : glyphs) {
            Rectangle glyphBox = glyph.getBounds();

            if (area.intersects(glyphBox)) {
                found.add(glyph);
            } else if (sortedByAbscissa && (glyphBox.x > xMax)) {
                break;
            }
        }

        return found;
    }

    //-------------------//
    // intersectedInters //
    //-------------------//
    /**
     * Lookup the provided list of interpretations for those whose
     * related glyph intersect the given box.
     *
     * @param inters the list of interpretations to search for
     * @param order  if the list is already sorted by some order, this may
     *               speedup the search
     * @param box    the intersecting box
     * @return the intersected interpretations found
     */
    public List<Inter> intersectedInters (List<Inter> inters,
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
     * Lookup the provided list of interpretations for those whose
     * related glyph intersect the given area.
     *
     * @param inters the list of interpretations to search for
     * @param order  if the list is already sorted by some order, this may
     *               speedup the search
     * @param area   the intersecting area
     * @return the intersected interpretations found
     */
    public List<Inter> intersectedInters (List<Inter> inters,
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

    //---------//
    // publish //
    //---------//
    public void publish (InterListEvent event)
    {
        system.getSheet().getLocationService().publish(event);
    }

    //------------------//
    // reduceExclusions //
    //------------------//
    /**
     * Reduce the provided exclusions as much as possible by removing the source or
     * target vertex of lower contextual grade.
     * <pre>
     * Strategy is as follows:
     * - Pick up among all current exclusions the one whose high inter has the
     *   highest contextual grade contribution among all exclusions.
     * - Remove the low inter of this chosen exclusion.
     * - Recompute all CG values.
     * - Iterate until no more exclusion is left.
     * </pre>
     *
     * @param mode      selected reduction mode
     * @param relations the collection to process
     * @return the set of vertices removed
     */
    public Set<Inter> reduceExclusions (ReductionMode mode,
                                        Collection<? extends Relation> relations)
    {
        // Which threshold we use for exclusions
        final double minDelta = (mode == ReductionMode.STRICT)
                ? constants.deltaGradeStrict.getValue()
                : constants.deltaGradeRelaxed.getValue();
        final Set<Inter> removed = new HashSet<Inter>();
        final Set<Relation> tiedExclusions = new HashSet<Relation>();
        Relation bestRel;

        do {
            // Choose exclusion with the highest source or target grade
            double bestCP = 0;
            bestRel = null;

            for (Relation rel : relations) {
                if (rel instanceof Exclusion) {
                    final Inter source = getEdgeSource(rel);
                    final Inter target = getEdgeTarget(rel);

                    if ((source != null) && (target != null)) {
                        Double scp = source.getContextualGrade();

                        if (scp == null) {
                            scp = source.getGrade();
                        }

                        Double tcp = target.getContextualGrade();

                        if (tcp == null) {
                            tcp = target.getGrade();
                        }

                        final double cp = Math.max(scp, tcp);

                        if (bestCP < cp) {
                            bestCP = cp;
                            bestRel = rel;
                        }
                    }
                }
            }

            // Remove the weaker branch of the selected exclusion (if grade delta is significant)
            if (bestRel != null) {
                final Inter source = getEdgeSource(bestRel);
                Double scp = source.getContextualGrade();

                if (scp == null) {
                    scp = source.getGrade();
                }

                final Inter target = getEdgeTarget(bestRel);
                Double tcp = target.getContextualGrade();

                if (tcp == null) {
                    tcp = target.getGrade();
                }

                double delta = Math.abs(tcp - scp);

                if (delta >= minDelta) {
                    Inter weaker = (scp < tcp) ? source : target;

                    if (weaker.isVip()) {
                        logger.info(
                                "VIP conflict {} deleting weaker {}",
                                bestRel.toLongString(this),
                                weaker);
                    }

                    Set<Relation> edges = edgesOf(weaker);
                    Set<Inter> involved = involvedInters(edges);

                    relations.removeAll(edges);
                    removed.add(weaker);
                    removeVertex(weaker);
                    involved.remove(weaker);

                    // Update contextual values
                    for (Inter inter : involved) {
                        computeContextualGrade(inter, false);
                    }
                } else {
                    if (mode == ReductionMode.STRICT) {
                        logger.warn("STRICT tight exclusion: {} {} vs {}", delta, source, target);
                    } else if (source.isVip() || target.isVip() || logger.isDebugEnabled()) {
                        logger.info("RELAXED tight exclusion: {} {} vs {}", delta, source, target);
                    }

                    tiedExclusions.add(bestRel);
                    relations.remove(bestRel);
                }
            }
        } while (bestRel != null);

        if (!tiedExclusions.isEmpty()) {
            logger.debug("Exclusions left over: {}", tiedExclusions.size());
        }

        return removed;
    }

    //------------------//
    // reduceExclusions //
    //------------------//
    /**
     * Process each exclusion in the SIG by removing the source or target vertex of
     * lower contextual grade.
     *
     * @param mode selected reduction mode
     * @return the set of vertices removed
     */
    public Set<Inter> reduceExclusions (ReductionMode mode)
    {
        return reduceExclusions(mode, new HashSet<Relation>(edgeSet()));
    }

    //--------------//
    // removeVertex //
    //--------------//
    @Override
    public boolean removeVertex (Inter inter)
    {
        inter.delete();

        return super.removeVertex(inter);
    }

    //------------------------//
    // computeContextualGrade //
    //------------------------//
    /**
     * Compute the contextual probability for a interpretation which is supported by
     * a collection of relations with partners.
     * It is assumed that all these supporting relations involve the inter as either a target or a
     * source, otherwise a runtime exception is thrown.
     * <p>
     * There may be mutual exclusion between some partners. In this case, we identify all partitions
     * of compatible partners and report the best resulting contextual contribution among those
     * partitions.
     *
     * @param inter    the inter whose contextual grade is to be computed
     * @param supports all supporting relations inter is involved with, some may be in conflict
     * @param logging  (debug) true for getting a printout of contributions retained
     * @return the best resulting contextual probability for the inter
     */
    private Double computeContextualGrade (Inter inter,
                                           List<? extends Support> supports,
                                           boolean logging)
    {
        /** Collection of partners. */
        final List<Inter> partners = new ArrayList<Inter>();

        /** Map: partner -> support ratio. */
        final Map<Inter, Double> partnerRatios = new HashMap<Inter, Double>();

        /** (debug) map: partner -> support relation. */
        final Map<Inter, Support> supportMap = logging ? new HashMap<Inter, Support>() : null;

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
                partnerRatios.put(partner, ratio);

                if (supportMap != null) {
                    supportMap.put(partner, support);
                }
            }
        }

        // Check for mutual exclusion between 'others'
        List<List<Inter>> seqs = getPartners(inter, partners);
        double bestCg = 0;
        List<Inter> bestSeq = null;

        for (List<Inter> seq : seqs) {
            int n = shrinkPartners(seq, partnerRatios);
            double[] ratios = new double[n];
            double[] partnerGrades = new double[n];

            for (int i = 0; i < n; i++) {
                Inter partner = seq.get(i);
                ratios[i] = partnerRatios.get(partner);
                partnerGrades[i] = partner.getGrade();
            }

            double cg = Grades.contextual(inter.getGrade(), partnerGrades, ratios);

            if (cg > bestCg) {
                bestCg = cg;
                bestSeq = seq;
            }
        }

        if (logging) {
            logger.info(
                    "{} cg:{} {}",
                    inter,
                    String.format("%.3f", bestCg),
                    supportsSeenFrom(inter, supportMap, bestSeq));
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

    //----------------//
    // shrinkPartners //
    //----------------//
    /**
     * Check, and shrink if needed, the collection of partners.
     *
     * @param list          (input/output) the list of partners which may be modified
     * @param partnerRatios (input) the support ratio brought by each partner
     * @return the final number of partners kept
     */
    private int shrinkPartners (List<Inter> list,
                                Map<Inter, Double> partnerRatios)
    {
        final int maxSupportCount = constants.maxSupportCount.getValue();
        int n = list.size();

        if (n > maxSupportCount) {
            class Contribution
            {

                final Inter partner;

                final double value; // Concrete contribution brought by the partner

                public Contribution (Inter partner,
                                     double ratio)
                {
                    this.partner = partner;
                    value = ratio * partner.getGrade();
                }
            }

            List<Contribution> contribs = new ArrayList<Contribution>();

            for (int i = 0; i < n; i++) {
                Inter partner = list.get(i);
                contribs.add(new Contribution(partner, partnerRatios.get(partner)));
            }

            Collections.sort(
                    contribs,
                    new Comparator<Contribution>()
                    {
                        @Override
                        public int compare (Contribution o1,
                                            Contribution o2)
                        {
                            return Double.compare(o2.value, o1.value);
                        }
                    });
            list.clear();
            n = maxSupportCount;

            for (int i = 0; i < n; i++) {
                list.add(contribs.get(i).partner);
            }
        }

        return n;
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

        Constant.Integer maxSupportCount = new Constant.Integer(
                "count",
                6,
                "Upper limit on number of supports used for contextual grade");

        Constant.Ratio deltaGradeStrict = new Constant.Ratio(
                0.00001,
                "Minimum grade delta to reduce an exclusion in STRICT mode");

        Constant.Ratio deltaGradeRelaxed = new Constant.Ratio(
                0.01,
                "Minimum grade delta to reduce an exclusion in RELAXED mode");
    }

    //----------//
    // Sequence //
    //----------//
    /**
     * This class lists a sequence of interpretations statuses.
     * Possible status values are:
     * -1: the related inter is forbidden (because of a conflict with an inter
     * located before in the sequence)
     * 0: the related inter is not selected
     * 1: the related inter is selected
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
}
