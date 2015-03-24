//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     F e r m a t a I n t e r                                    //
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

import omr.math.GeoUtil;

import omr.sheet.Staff;
import omr.sheet.SystemInfo;

import omr.sig.relation.FermataBarRelation;
import omr.sig.relation.FermataChordRelation;
import omr.sig.relation.FermataNoteRelation;

import java.awt.Point;
import java.util.List;

/**
 * Class {@code FermataInter} represents a fermata interpretation, either upright or
 * inverted.
 * <p>
 * <img src="http://upload.wikimedia.org/wikipedia/commons/thumb/9/90/Urlinie_in_G_with_fermata.png/220px-Urlinie_in_G_with_fermata.png" />
 * <p>
 * An upright fermata refers to the chord in the staff right below in the containing part.
 * An inverted fermata refers to the chord in the staff right above in the containing part.
 * A fermata may also refer to a single or double bar-line, to indicate the end of a phrase.
 * <p>
 * Such reference is implemented via a Relation instance.
 * <p>
 *
 * @author Hervé Bitteur
 */
public class FermataInter
        extends AbstractNotationInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code FermataInter} object.
     *
     * @param glyph the fermata glyph
     * @param shape FERMATA or FERMATA_BELOW
     * @param grade the interpretation quality
     */
    private FermataInter (Glyph glyph,
                          Shape shape,
                          double grade)
    {
        super(glyph, glyph.getBounds(), shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a fermata inter.
     *
     * @param glyph  the fermata glyph
     * @param shape  FERMATA or FERMATA_BELOW
     * @param grade  the interpretation quality
     * @param system the related system
     * @return the created instance or null
     */
    public static FermataInter create (Glyph glyph,
                                       Shape shape,
                                       double grade,
                                       SystemInfo system)
    {
        // Look for proper staff
        Point center = glyph.getAreaCenter();
        Staff staff = (shape == Shape.FERMATA) ? system.getStaffBelow(center)
                : system.getStaffAbove(center);

        if (staff == null) {
            return null;
        }

        FermataInter fermata = new FermataInter(glyph, shape, grade);
        fermata.setStaff(staff);

        return fermata;
    }

    //-----------------//
    // linkWithBarline //
    //-----------------//
    /**
     * Try to connect this fermata with a suitable barline.
     *
     * @return true if successful
     */
    public boolean linkWithBarline ()
    {
        // Look for a bar-line related to this fermata
        Point center = getCenter();
        List<BarlineInter> bars = getStaff().getBars();
        BarlineInter bar = BarlineInter.getClosestBarline(bars, center);

        if ((bar != null) && (GeoUtil.xOverlap(getBounds(), bar.getBounds()) > 0)) {
            // For fermata & for bar
            sig.addEdge(this, bar, new FermataBarRelation());

            return true;
        }

        return false;
    }

    //----------------//
    // linkWithChords //
    //----------------//
    /**
     * Try to connect this fermata with suitable chord(s).
     *
     * @param chords the chords in fermata related staff
     * @return true if successful
     */
    public boolean linkWithChords (List<ChordInter> chords)
    {
        // Look for a chord related to this fermata
        //TODO: what if the note is mirrored between 2 chords?
        Point center = getCenter();
        ChordInter chord = ChordInter.getClosestChord(chords, center);

        if ((chord != null) && (GeoUtil.xOverlap(getBounds(), chord.getBounds()) > 0)) {
            // For fermata & for chord
            sig.addEdge(this, chord, new FermataChordRelation());

            // For chord members (notes)
            for (Inter member : chord.getMembers()) {
                if (member instanceof AbstractNoteInter) {
                    sig.addEdge(this, member, new FermataNoteRelation());
                }
            }

            return true;
        }

        return false;
    }
}
