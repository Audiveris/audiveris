//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R e s t I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.sheet.Staff;

import java.awt.Point;

/**
 * Class {@code RestInter} represents a rest.
 * TODO: Should be closer to AbstractNoteInter?
 *
 * @author Hervé Bitteur
 */
public class RestInter
        extends AbstractNoteInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new RestInter object.
     *
     * @param glyph underlying glyph
     * @param shape rest shape
     * @param grade evaluation value
     * @param staff the related staff
     * @param pitch the rest pitch
     */
    public RestInter (Glyph glyph,
                      Shape shape,
                      double grade,
                      Staff staff,
                      int pitch)
    {
        super(glyph, null, shape, grade, staff, pitch);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a Rest inter.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff the closest staff (questionable)
     * @return the created instance or null if failed
     */
    public static Inter create (Glyph glyph,
                                Shape shape,
                                double grade,
                                Staff staff)
    {
        // Determine pitch according to glyph centroid
        Point centroid = glyph.getCentroid();
        double measuredPitch = staff.pitchPositionOf(centroid);
        int pitch = (int) Math.rint(measuredPitch);

        return new RestInter(glyph, shape, grade, staff, pitch);
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //----------//
    // getChord //
    //----------//
    @Override
    public RestChordInter getChord ()
    {
        return (RestChordInter) getEnsemble();
    }
}
