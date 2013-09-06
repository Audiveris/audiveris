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

import omr.sheet.SystemInfo;

import omr.sig.Exclusion.Cause;

import omr.util.Predicate;

import org.jgrapht.graph.Multigraph;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
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
     * @param inters           the list of interpretations to search for
     * @param sortedByAbscissa true if the list is already sorted by abscissa,
     *                         in order to speedup the search
     * @param box              the intersecting box
     * @return the intersected interpretations found
     */
    public List<Inter> intersectedInters (List<Inter> inters,
                                          boolean sortedByAbscissa,
                                          Rectangle box)
    {
        List<Inter> found = new ArrayList<Inter>();

        for (Inter inter : inters) {
            Rectangle glyphBox = inter.getGlyph()
                    .getBounds();

            if (box.intersects(glyphBox)) {
                found.add(inter);
            } else if (sortedByAbscissa && (glyphBox.x >= (box.x + box.width))) {
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
     * @param inters           the list of interpretations to search for
     * @param sortedByAbscissa true if the list is already sorted by abscissa,
     *                         in order to speedup the search
     * @param area             the intersecting area
     * @return the intersected interpretations found
     */
    public List<Inter> intersectedInters (List<Inter> inters,
                                          boolean sortedByAbscissa,
                                          Area area)
    {
        List<Inter> found = new ArrayList<Inter>();
        double xMax = area.getBounds()
                .getMaxX();

        for (Inter inter : inters) {
            Rectangle glyphBox = inter.getGlyph()
                    .getBounds();

            if (area.intersects(glyphBox)) {
                found.add(inter);
            } else if (sortedByAbscissa && (glyphBox.x > xMax)) {
                break;
            }
        }

        return found;
    }

    //----------------//
    // sortByAbscissa //
    //----------------//
    /**
     * Sort a list of interpretation according to the abscissa of the
     * related glyph.
     *
     * @param inters the list to sort
     */
    public void sortByAbscissa (List<Inter> inters)
    {
        Collections.sort(
                inters,
                new Comparator<Inter>()
        {
            @Override
            public int compare (Inter i1,
                                Inter i2)
            {
                return Glyph.byAbscissa.compare(
                        i1.getGlyph(),
                        i2.getGlyph());
            }
        });
    }
}
