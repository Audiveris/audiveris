//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     F e r m a t a I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.math.GeoUtil;

import omr.sheet.Scale;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;

import omr.sig.relation.FermataBarRelation;
import omr.sig.relation.FermataChordRelation;
import omr.sig.relation.FermataNoteRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code FermataInter} represents a fermata interpretation, either upright or
 * inverted.
 * <p>
 * <img src="http://upload.wikimedia.org/wikipedia/commons/thumb/9/90/Urlinie_in_G_with_fermata.png/220px-Urlinie_in_G_with_fermata.png">
 * <p>
 * An upright fermata refers to the chord in the staff right below in the containing part.
 * An inverted fermata refers to the chord in the staff right above in the containing part.
 * A fermata may also refer to a single or double barline, to indicate the end of a phrase.
 * <p>
 * Such reference is implemented via a Relation instance.
 * <p>
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "fermata")
public class FermataInter
        extends AbstractNotationInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(FermataInter.class);

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

    /**
     * No-arg constructor meant for JAXB.
     */
    private FermataInter ()
    {
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
        Point center = glyph.getCenter();
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
        // Look for a barline related to this fermata
        Point center = getCenter();
        List<BarlineInter> bars = getStaff().getBars();
        BarlineInter bar = BarlineInter.getClosestBarline(bars, center);

        if ((bar != null) && (GeoUtil.xOverlap(getBounds(), bar.getBounds()) > 0)) {
            // Check vertical distance to bar/staff
            final Scale scale = sig.getSystem().getSheet().getScale();
            final int maxDy = scale.toPixels(constants.maxFermataDy);
            final int dyStaff = staff.distanceTo(center);

            if (dyStaff > maxDy) {
                logger.debug("{} too far from barline: {}", this, bar);

                return false;
            }

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
    public boolean linkWithChords (Collection<AbstractChordInter> chords)
    {
        // Look for a suitable chord related to this fermata
        Point center = getCenter();
        AbstractChordInter chord = AbstractChordInter.getClosestChord(chords, center);

        if ((chord != null) && (GeoUtil.xOverlap(getBounds(), chord.getBounds()) > 0)) {
            double dyChord = Math.sqrt(GeoUtil.ptDistanceSq(chord.getBounds(), center.x, center.y));

            // If chord is mirrored, select the closest vertically
            if (chord.getMirror() != null) {
                double dyMirror = Math.sqrt(
                        GeoUtil.ptDistanceSq(chord.getMirror().getBounds(), center.x, center.y));

                if (dyMirror < dyChord) {
                    dyChord = dyMirror;
                    chord = (AbstractChordInter) chord.getMirror();
                    logger.debug("{} selecting mirror {}", this, chord);
                }
            }

            // Check vertical distance between fermata and chord
            final Scale scale = sig.getSystem().getSheet().getScale();
            final int maxDy = scale.toPixels(constants.maxFermataDy);

            if (dyChord > maxDy) {
                // Check vertical distance between fermata and staff
                final Staff chordStaff = (shape == Shape.FERMATA) ? chord.getTopStaff()
                        : chord.getBottomStaff();
                final int dyStaff = chordStaff.distanceTo(center);

                if (dyStaff > maxDy) {
                    logger.debug("{} too far from staff/chord: {}", this, dyStaff);

                    return false;
                }
            }

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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction maxFermataDy = new Scale.Fraction(
                2.5,
                "Maximum vertical distance between fermata center and related chord/staff/barline");
    }
}
