//----------------------------------------------------------------------------//
//                                                                            //
//                      G l y p h R e c o g n i t i o n                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.Shape;
import omr.glyph.text.TextInfo;

import omr.math.Rational;

/**
 * Interface {@code GlyphRecognition} defines a facet that deals with the shape
 * recognition of a glyph.
 *
 * @author Hervé Bitteur
 */
interface GlyphRecognition
    extends GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    //-------//
    // isBar //
    //-------//
    /**
     * Convenient method which tests if the glyph is a Bar line
     *
     * @return true if glyph shape is a bar
     */
    boolean isBar ();

    //--------//
    // isClef //
    //--------//
    /**
     * Convenient method which tests if the glyph is a Clef
     *
     * @return true if glyph shape is a Clef
     */
    boolean isClef ();

    //----------//
    // getDoubt //
    //----------//
    /**
     * Report the doubt of the glyph shape
     *
     * @return the doubt related to glyph shape
     */
    double getDoubt ();

    //---------//
    // isKnown //
    //---------//
    /**
     * A glyph is considered as known if it has a registered shape other than
     * NOISE (Notice that CLUTTER as well as NO_LEGAL_TIME and GLYPH_PART are
     * considered as being known).
     *
     * @return true if known
     */
    boolean isKnown ();

    //---------------//
    // isManualShape //
    //---------------//
    /**
     * Report whether the shape of this glyph has been manually assigned (and
     * thus can only be modified by explicit user action)
     *
     * @return true if shape manually assigned
     */
    boolean isManualShape ();

    //-------------//
    // setRational //
    //-------------//
    /**
     * Set the glyph timesig rational value
     * @param rational the rational to set
     */
    void setRational (Rational rational);

    //-------------//
    // getRational //
    //-------------//
    /**
     * Report the related timesig rational if any
     * @return the rational
     */
    Rational getRational ();

    //----------//
    // setShape //
    //----------//
    /**
     * Setter for the glyph shape, assumed to be based on structural data
     *
     * @param shape the assigned shape, which may be null
     */
    void setShape (Shape shape);

    //----------//
    // setShape //
    //----------//
    /**
     * Setter for the glyph shape, with related doubt
     *
     * @param shape the assigned shape
     * @param doubt the related doubt
     */
    void setShape (Shape  shape,
                   double doubt);

    //----------//
    // getShape //
    //----------//
    /**
     * Report the registered glyph shape
     *
     * @return the glyph shape, which may be null
     */
    Shape getShape ();

    //------------------//
    // isShapeForbidden //
    //------------------//
    /**
     * Check whether a shape is forbidden for this glyph
     * @param shape the shape to check
     * @return true if the provided shape is one of the forbidden shapes for
     * this glyph
     */
    boolean isShapeForbidden (Shape shape);

    //--------//
    // isStem //
    //--------//
    /**
     * Convenient method which tests if the glyph is a Stem
     *
     * @return true if glyph shape is a Stem
     */
    boolean isStem ();

    //--------//
    // isText //
    //--------//
    /**
     * Check whether the glyph shape is a text (or a character)
     *
     * @return true if text or character
     */
    boolean isText ();

    //-------------//
    // getTextInfo //
    //-------------//
    /**
     * Report the textual information for this glyph
     * @return the glyph textual info, or null if none
     */
    TextInfo getTextInfo ();

    //-------------//
    // isWellKnown //
    //-------------//
    /**
     * A glyph is considered as well known if it has a registered well known
     * shape
     *
     * @return true if so
     */
    boolean isWellKnown ();

    //------------//
    // allowShape //
    //------------//
    /**
     * Remove the provided shape from the collection of forbidden shaped, if any
     * @param shape the shape to allow
     */
    void allowShape (Shape shape);

    //-------------//
    // forbidShape //
    //-------------//
    /**
     * Forbid a specific shape
     * @param shape the shape to forbid
     */
    void forbidShape (Shape shape);
}
