//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     F e r m a t a I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.relation.FermataBarRelation;
import org.audiveris.omr.sig.relation.FermataChordRelation;
import org.audiveris.omr.sig.relation.FermataNoteRelation;
import org.audiveris.omr.util.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code FermataInter} represents a full fermata interpretation, either upright
 * or inverted, combining an arc and a dot.
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
        implements InterEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            FermataInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    // Persistent data
    //----------------
    //
    /** Arc. */
    @XmlIDREF
    @XmlAttribute(name = "arc")
    private final FermataArcInter arc;

    /** Dot. */
    @XmlIDREF
    @XmlAttribute(name = "dot")
    private final FermataDotInter dot;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code FermataInter} object.
     *
     * @param arc    the fermata arc (of shape FERMATA_ARC or FERMATA_ARC_BELOW)
     * @param bounds bounding box
     * @param shape  the fermata shape (FERMATA or FERMATA_BELOW)
     * @param grade  the interpretation quality
     * @param dot    the fermata dot
     */
    public FermataInter (FermataArcInter arc,
                         Rectangle bounds,
                         Shape shape,
                         double grade,
                         FermataDotInter dot)
    {
        super(null, bounds, shape, grade);
        this.arc = arc;
        this.dot = dot;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private FermataInter ()
    {
        arc = null;
        dot = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a fermata inter.
     *
     * @param arc    the fermata arc (shape: FERMATA_ARC or FERMATA_ARC_BELOW)
     * @param dot    the fermata dot
     * @param system the related system
     * @return the created instance or null
     */
    public static FermataInter create (FermataArcInter arc,
                                       FermataDotInter dot,
                                       SystemInfo system)
    {
        // Look for proper staff
        final Point center = arc.getGlyph().getCenter();
        final Shape arcShape = arc.getShape();
        final Staff staff = (arcShape == Shape.FERMATA_ARC) ? system.getStaffAtOrBelow(center)
                : system.getStaffAtOrAbove(center);

        if (staff == null) {
            return null;
        }

        final double grade = 0.5 * (arc.getContextualGrade() + dot.getContextualGrade());
        final Shape shape = (arcShape == Shape.FERMATA_ARC) ? Shape.FERMATA : Shape.FERMATA_BELOW;
        final Rectangle box = arc.getBounds();
        box.add(dot.getBounds());

        final FermataInter fermata = new FermataInter(arc, box, shape, grade, dot);
        fermata.setStaff(staff);

        return fermata;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            bounds = Entities.getBounds(getMembers());
        }

        return bounds;
    }

    //------------//
    // getMembers //
    //------------//
    @Override
    public List<? extends Inter> getMembers ()
    {
        return Arrays.asList(arc, dot);
    }

    //--------//
    // getArc //
    //--------//
    public FermataArcInter getArc ()
    {
        return arc;
    }

    //--------//
    // getDot //
    //--------//
    public FermataDotInter getDot ()
    {
        return dot;
    }

    //-----------------//
    // linkWithBarline //
    //-----------------//
    /**
     * (Try to) connect this fermata with a suitable barline.
     *
     * @return true if successful
     */
    public boolean linkWithBarline ()
    {
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
     * (Try to) connect this fermata with suitable chord.
     *
     * @param chords the chords in fermata related staff, constrained by fermata x range
     * @return true if successful
     */
    public boolean linkWithChords (Collection<AbstractChordInter> chords)
    {
        // Look for a suitable chord related to this fermata
        Point center = getCenter();
        AbstractChordInter chord = AbstractChordInter.getClosestChord(chords, center);

        if (chord != null) {
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

            // For chord members (notes). TODO: is this useful???
            for (Inter member : chord.getMembers()) {
                if (member instanceof AbstractNoteInter) {
                    sig.addEdge(this, member, new FermataNoteRelation());
                }
            }

            return true;
        }

        return false;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
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
