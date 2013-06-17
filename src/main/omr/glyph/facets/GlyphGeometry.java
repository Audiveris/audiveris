//----------------------------------------------------------------------------//
//                                                                            //
//                         G l y p h G e o m e t r y                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.GlyphSignature;

import omr.math.Circle;
import omr.math.PointsCollector;

import omr.moments.ARTMoments;
import omr.moments.GeometricMoments;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Interface {@code GlyphGeometry} defines the facet which handles all
 * the geometrical characteristics of a glyph (scale, contour box,
 * location, weight, density, moments, etc).
 *
 * <p>Nota: A glyph, unlike its member sections, has no orientation, so all the
 * following methods work in absolute coordinates.
 *
 * @author Hervé Bitteur
 */
interface GlyphGeometry
        extends GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report the glyph ART moments, which are lazily computed.
     *
     * @return the glyph ART moments
     */
    ARTMoments getARTMoments ();

    /**
     * Report the glyph area center.
     * (The point is lazily evaluated).
     *
     * @return the area center point
     */
    Point getAreaCenter ();

    /**
     * Return a copy of the absolute display bounding box.
     * Useful to quickly check if the glyph needs to be repainted.
     *
     * @return a COPY of the bounding contour rectangle box
     */
    Rectangle getBounds ();

    /**
     * Report the glyph absolute centroid (mass center).
     * The point is lazily evaluated.
     *
     * @return the absolute mass center point
     */
    Point getCentroid ();

    /**
     * Report the approximating circle, if any.
     *
     * @return the approximating circle, or null
     */
    Circle getCircle ();

    /**
     * Report the density of the stick, that is its weight divided by
     * the area of its bounding rectangle.
     *
     * @return the density
     */
    double getDensity ();

    /**
     * Report the glyph geometric moments, which are lazily computed.
     *
     * @return the glyph geometric moments
     */
    GeometricMoments getGeometricMoments ();

    /**
     * Report the interline value for the glyph containing staff,
     * which is used for some of the moments.
     *
     * @return the interline value
     */
    int getInterline ();

    /**
     * Report the glyph (reference) location, which is the equivalent
     * of the icon reference point if one such point exists, or the
     * glyph area center otherwise.
     * The point is lazily evaluated.
     *
     * @return the reference center point
     */
    Point getLocation ();

    /**
     * Report the height of this glyph, after normalization to sheet
     * interline.
     *
     * @return the height value, expressed as an interline fraction
     */
    double getNormalizedHeight ();

    /**
     * Report the weight of this glyph, after normalization to sheet
     * interline.
     *
     * @return the weight value, expressed as an interline square fraction
     */
    double getNormalizedWeight ();

    /**
     * Report the width of this glyph, after normalization to sheet
     * interline.
     *
     * @return the width value, expressed as an interline fraction
     */
    double getNormalizedWidth ();

    /**
     * Report the collector filled with glyph points.
     *
     * @return the populated points collector
     */
    PointsCollector getPointsCollector ();

    /**
     * Report the last registration signature.
     *
     * @return the previous valid glyph signature
     */
    GlyphSignature getRegisteredSignature ();

    /**
     * Report current signature that distinguishes this glyph.
     *
     * @return the glyph signature
     */
    GlyphSignature getSignature ();

    /**
     * Report the total weight of this glyph, as the sum of its section
     * weights.
     *
     * @return the total weight (number of pixels)
     */
    int getWeight ();

    /**
     * Check whether the glyph intersect the provided absolute
     * rectangle.
     *
     * @param rectangle the provided absolute rectangle
     * @return true if intersection is not empty, false otherwise
     */
    boolean intersects (Rectangle rectangle);

    /**
     * Remember an approximating circle.
     *
     * @param circle the circle value, or null
     */
    void setCircle (Circle circle);

    /**
     * Force the glyph contour box (when start and stop points are
     * forced).
     *
     * @param contourBox the forced contour box
     */
    void setContourBox (Rectangle contourBox);

    /**
     * Remember registration signature.
     *
     * @param sig the signature used for registration
     */
    void setRegisteredSignature (GlyphSignature sig);

    /**
     * Apply a translation to the glyph from its current location,
     * according to the provided vector.
     *
     * @param vector the (dx, dy) translation
     */
    void translate (Point vector);
}
