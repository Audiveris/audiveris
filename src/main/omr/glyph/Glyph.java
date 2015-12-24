//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           G l y p h                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.image.Table;

import omr.moments.ARTMoments;
import omr.moments.GeometricMoments;

import omr.run.RunTable;

import omr.sig.inter.Inter;

import ij.process.ByteProcessor;

import java.awt.Point;
import java.awt.Shape;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code Glyph} is a symbol made of a fixed set of pixels.
 * <p>
 * A glyph is un-mutable, meaning one cannot add or remove pixels to/from an existing instance,
 * although one can always create another glyph instance with the proper collection of pixels.
 * Instead of Glyph, see {@link omr.glyph.dynamic.SectionCompound} class to deal with growing
 * compounds of sections.
 * <p>
 * A glyph is implemented and persisted as a run table located at a given absolute origin.
 * <p>
 * A glyph has no intrinsic orientation, hence some methods such as {@link #getLength} require that
 * view orientation be provided as a parameter.
 * <p>
 * A glyph has no shape, see {@link Inter} class for glyph interpretation and
 * {@link omr.classifier.Sample} class for training of shape classifiers.
 * <p>
 * Additional features are made available via the {@link Glyphs} class.
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(BasicGlyph.Adapter.class)
public interface Glyph
        extends Symbol, NearLine
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Fill the provided table with glyph foreground pixels.
     *
     * @param table       (output) the table to fill
     * @param tableOrigin absolute origin of table
     * @param fat         true to add touching locations
     */
    void fillTable (Table.UnsignedByte table,
                    Point tableOrigin,
                    boolean fat);

    /**
     * Report the glyph ART moments.
     *
     * @return the glyph ART moments
     */
    ARTMoments getARTMoments ();

    /**
     * Report a buffer of the glyph (which can be handed to the OCR)
     *
     * @return a black & white buffer (contour box size )
     */
    ByteProcessor getBuffer ();

    /**
     * Report the glyph geometric moments.
     *
     * @param interline the global sheet interline
     * @return the glyph geometric moments
     */
    GeometricMoments getGeometricMoments (int interline);

    /**
     * Report the containing glyph index
     *
     * @return the containing index
     */
    GlyphIndex getIndex ();

    /**
     * Report the pitch position, with respect to related or closest staff.
     * Examples: -4 for top line (F), 0 for middle line (B), +4 for bottom line (E)
     *
     * @return the pitch position value WRT the related or closest staff
     */
    Double getPitchPosition ();

    /**
     * Report the underlying table of runs
     *
     * @return the glyph runTable
     */
    RunTable getRunTable ();

    /**
     * Report a short glyph reference
     *
     * @return glyph reference
     */
    String idString ();

    /**
     * Report whether the glyph has a pixel in common with the provided table.
     *
     * @param table       the table of pixels
     * @param tableOrigin top-left corner of table
     * @return true if connection found
     */
    boolean intersects (Table.UnsignedByte table,
                        Point tableOrigin);

    /**
     * Check whether the glyph intersects the provided AWT shape.
     *
     * @param shape the provided awt shape
     * @return true if intersection is not empty, false otherwise
     */
    boolean intersects (Shape shape);

    /**
     * Report whether this glyph is identical to that glyph
     *
     * @param that the other glyph
     * @return true if their pixels are identical
     */
    boolean isIdentical (Glyph that);

    /**
     * Test whether the glyph is transient (not yet inserted into the nest)
     *
     * @return true if transient
     */
    boolean isTransient ();

    /**
     * Report whether this glyph is virtual (rather than real)
     *
     * @return true if virtual
     */
    boolean isVirtual ();

    /**
     * The setter for glyph nest.
     *
     * @param index the containing glyph index
     */
    void setIndex (GlyphIndex index);

    /**
     * Set the pitch position, with respect to related or closest staff.
     *
     * @param pitchPosition the pitch position WRT the related or closest staff
     */
    void setPitchPosition (double pitchPosition);
}
