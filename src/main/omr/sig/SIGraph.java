//----------------------------------------------------------------------------//
//                                                                            //
//                                 S I G r a p h                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.math.GeoOrder;
import static omr.math.GeoOrder.*;

import omr.selection.InterListEvent;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code SIGraph} represents the Symbol Interpretation Graph
 * that aims at finding the best global interpretation of all symbols
 * in a system.
 *
 * @author Hervé Bitteur
 */
public class SIGraph
        extends Multigraph<Inter, Relation>
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SIGraph.class);

    //~ Instance fields --------------------------------------------------------
    /** Dedicated system */
    private final SystemInfo system;

    //~ Constructors -----------------------------------------------------------
    //---------//
    // SIGraph //
    //---------//
    /**
     * Creates a new SIGraph object.
     */
    public SIGraph (SystemInfo system)
    {
        super(new RelationFactory());
        this.system = system;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // addVertex //
    //-----------//
    /**
     * {@inheritDoc}
     * Overridden so that all interpretations keep a pointer to their
     * hosting sig.
     *
     * @param inter the brand new interpretation
     * @return true if the inter was actually added, false if it existed before
     */
    @Override
    public boolean addVertex (Inter inter)
    {
        boolean res = super.addVertex(inter);
        inter.setSig(this);

        return res;
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

    //--------------------//
    // getContextualGrade //
    //--------------------//
    /**
     * Compute the contextual probability for a target which is
     * supported by a collection of relations.
     * It is assumed that all these supporting relation have the same target,
     * if not a runtime exception is raised (perhaps we could relax this and
     * simply ignore the relations that have a different target?)
     *
     * @param target   the common target
     * @param supports the set of supporting relations
     * @return the resulting contextual probability for the target inter.
     */
    public Double getContextualGrade (Inter target,
                                      Support... supports)
    {
        int n = supports.length;
        double[] ratios = new double[n];
        double[] sources = new double[n];

        // Feed arrays and check common target
        for (int i = 0; i < n; i++) {
            Support support = supports[i];

            if (target != getEdgeTarget(support)) {
                throw new RuntimeException("No common target");
            }

            ratios[i] = support.getRatio();
            sources[i] = getEdgeSource(support)
                    .getGrade();
        }

        return Grades.contextual(target.getGrade(), ratios, sources);
    }

    //--------------------//
    // getContextualGrade //
    //--------------------//
    /**
     * Compute the contextual probability brought by this relation.
     *
     * @param support the support relation
     * @return the resulting contextual probability for the relation target
     */
    public Double getContextualGrade (Support support)
    {
        return contextual(
                getEdgeTarget(support),
                support,
                getEdgeSource(support));
    }

    //----------//
    // getInter //
    //----------//
    /**
     * Report the interpretation if any of desired class for the
     * glyph at hand.
     *
     * @param glyph  the underlying glyph
     * @param classe the interpretation class desired
     * @return the existing interpretation if any, or null
     */
    public Inter getInter (Glyph glyph,
                           Class classe)
    {
        for (Inter inter : glyph.getInterpretations()) {
            if (inter.getClass()
                    .isAssignableFrom(classe)) {
                return inter;
            }
        }

        return null;
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
            if (rel.getClass()
                    .isAssignableFrom(classe)) {
                return rel;
            }
        }

        return null;
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
     * Insert an exclusion relation between source and target, unless
     * such an exclusion already exists.
     * TODO: is the order relevant between source and target? TBC
     *
     * @param source provided source
     * @param target provided target
     * @param cause  exclusion cause (for creation only)
     * @return the concrete exclusion relation, found or created
     */
    public Exclusion insertExclusion (Inter source,
                                      Inter target,
                                      Cause cause)
    {
        for (Relation rel : getAllEdges(source, target)) {
            if (rel instanceof Exclusion) {
                return (Exclusion) rel;
            }
        }

        Exclusion exc = new BasicExclusion(cause);
        addEdge(source, target, exc);

        return exc;
    }

    //------------------//
    // insertExclusions //
    //------------------//
    /**
     * Insert mutual exclusions between all members of the provided
     * collection of interpretations.
     *
     * @param inters the provided collection of mutually exclusive inters
     * @param cause  the exclusion cause
     */
    public void insertExclusions (List<? extends Inter> inters,
                                  Cause cause)
    {
        int count = inters.size();

        if (count < 2) {
            return;
        }

        for (int i = 0; i < (count - 1); i++) {
            Inter one = inters.get(i);

            for (Inter two : inters.subList(i + 1, count)) {
                addEdge(one, two, new BasicExclusion(cause));
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
                return inter.getShape() == shape;
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
                return shapes.contains(inter.getShape());
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
        double xMax = area.getBounds()
                .getMaxX();
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
            Rectangle iBox = inter.getBounds();

            if (area.intersects(iBox)) {
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
     * Lookup the sig collection of interpretations for those which
     * contain the provided point.
     *
     * @param point provided point
     * @return the containing interpretations
     */
    public List<Inter> intersectedInters (Point point)
    {
        List<Inter> found = new ArrayList<Inter>();

        for (Inter inter : vertexSet()) {
            if (inter.getBounds()
                    .contains(point)) {
                found.add(inter);
            }
        }

        return found;
    }

    //---------//
    // publish //
    //---------//
    public void publish (InterListEvent event)
    {
        system.getSheet()
                .getLocationService()
                .publish(event);
    }

    //------------//
    // contextual //
    //------------//
    private double contextual (Inter target,
                               Support support,
                               Inter source)
    {
        return Grades.contextual(
                target.getGrade(),
                support.getRatio(),
                source.getGrade());
    }
}
