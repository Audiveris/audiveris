//----------------------------------------------------------------------------//
//                                                                            //
//                         G l y p h G e o m e t r y                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.GlyphSignature;

import omr.math.Moments;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import java.awt.Rectangle;

/**
 * Interface {@code GlyphGeometry} defines the facet which handles all the
 * geometrical characteristics of a glyph (scale, contour box, location, weight,
 * density, moments, etc).
 *
 * @author Hervé Bitteur
 */
interface GlyphGeometry
    extends GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getAreaCenter //
    //---------------//
    /**
     * Report the glyph area center (The point is lazily evaluated)
     *
     * @return the area center point
     */
    PixelPoint getAreaCenter ();

    //-------------------//
    // getOrientedBounds //
    //-------------------//
    /**
     * Return a COPY of the oriented bounding rectangle of the glyph
     *
     * @return the oriented bounds
     */
    Rectangle getOrientedBounds ();

    //-------------//
    // getCentroid //
    //-------------//
    /**
     * Report the glyph absolute centroid (mass center). The point is lazily evaluated.
     *
     * @return the absolute mass center point
     */
    PixelPoint getCentroid ();

    //---------------//
    // getContourBox //
    //---------------//
    /**
     * Return a copy of the absolute display bounding box.
     * Useful to quickly check if the glyph needs to be repainted.
     *
     * @return a COPY of the bounding contour rectangle box
     */
    PixelRectangle getContourBox ();

    //------------//
    // getDensity //
    //------------//
    /**
     * Report the density of the stick, that is its weight divided by the area
     * of its bounding rectangle
     *
     * @return the density
     */
    double getDensity ();

    //--------------//
    // getInterline //
    //--------------//
    /**
     * Report the interline value for the glyph containing staff, which is used
     * for some of the moments
     *
     * @return the interline value
     */
    int getInterline ();

    //-------------//
    // getLocation //
    //-------------//
    /**
     * Report the glyph (reference) location, which is the equivalent of the
     * icon reference point if one such point exists, or the glyph area center
     * otherwise. The point is lazily evaluated.
     *
     * @return the reference center point
     */
    PixelPoint getLocation ();

    //------------//
    // getMoments //
    //------------//
    /**
     * Report the glyph moments, which are lazily computed
     *
     * @return the glyph moments
     */
    Moments getMoments ();

    //---------------------//
    // getNormalizedHeight //
    //---------------------//
    /**
     * Report the height of this glyph, after normalization to sheet interline
     * @return the height value, expressed as an interline fraction
     */
    double getNormalizedHeight ();

    //---------------------//
    // getNormalizedWeight //
    //---------------------//
    /**
     * Report the weight of this glyph, after normalization to sheet interline
     * @return the weight value, expressed as an interline square fraction
     */
    double getNormalizedWeight ();

    //--------------------//
    // getNormalizedWidth //
    //--------------------//
    /**
     * Report the width of this glyph, after normalization to sheet interline
     * @return the width value, expressed as an interline fraction
     */
    double getNormalizedWidth ();

    //--------------//
    // getSignature //
    //--------------//
    /**
     * Report a signature that should allow to detect glyph identity
     *
     * @return the glyph signature
     */
    GlyphSignature getSignature ();

    //-----------//
    // getWeight //
    //-----------//
    /**
     * Report the total weight of this glyph, as the sum of section weights
     *
     * @return the total weight (number of pixels)
     */
    int getWeight ();

    //----------------//
    // computeMoments //
    //----------------//
    /**
     * Compute all the moments for this glyph
     */
    void computeMoments ();

    //------------//
    // intersects //
    //------------//
    /**
     * Check whether the glyph intersect the provided absolute rectangle
     * @param rectangle the provided absolute rectangle
     * @return true if intersection is not empty, false otherwise
     */
    boolean intersects (PixelRectangle rectangle);

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation to the glyph from its current location, according to
     * the provided vector
     *
     * @param vector the (dx, dy) translation
     */
    void translate (PixelPoint vector);
}
