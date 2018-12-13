//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     F e r m a t a I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.FermataBarRelation;
import org.audiveris.omr.sig.relation.FermataChordRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.util.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code FermataInter} represents a full fermata interpretation, either upright
 * or inverted, combining an arc and a dot.
 * <p>
 * <img src=
 * "http://upload.wikimedia.org/wikipedia/commons/thumb/9/90/Urlinie_in_G_with_fermata.png/220px-Urlinie_in_G_with_fermata.png"
 * alt="Urlinie in G with fermata on penultimate note">
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
        extends AbstractInter
        implements InterEnsemble
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(FermataInter.class);

    private static Comparator<Inter> byFermataOrder = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter o1,
                            Inter o2)
        {
            return (o1 instanceof FermataArcInter) ? (-1) : (+1); // Arc then dot
        }
    };

    /**
     * Creates a new {@code FermataInter} object.
     *
     * @param shape the fermata shape (FERMATA or FERMATA_BELOW)
     * @param grade the interpretation quality
     */
    public FermataInter (Shape shape,
                         double grade)
    {
        super(null, null, shape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private FermataInter ()
    {
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-----------//
    // addMember //
    //-----------//
    @Override
    public void addMember (Inter member)
    {
        if (member instanceof FermataArcInter) {
            FermataArcInter arc = getArc();

            if (arc != null) {
                throw new IllegalStateException("Arc already defined");
            }

            EnsembleHelper.addMember(this, member);
        }

        if (member instanceof FermataDotInter) {
            FermataDotInter dot = getDot();

            if (dot != null) {
                throw new IllegalStateException("Dot already defined");
            }

            EnsembleHelper.addMember(this, member);
        }
    }

    //--------//
    // getArc //
    //--------//
    /**
     * Report the arc part.
     *
     * @return the arc
     */
    public FermataArcInter getArc ()
    {
        final List<Inter> members = getMembers();

        for (Inter member : members) {
            if (member instanceof FermataArcInter) {
                return (FermataArcInter) member;
            }
        }

        return null;
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

        return new Rectangle(bounds);
    }

    //--------//
    // getDot //
    //--------//
    /**
     * Report the dot part.
     *
     * @return the dot
     */
    public FermataDotInter getDot ()
    {
        final List<Inter> members = getMembers();

        for (Inter member : members) {
            if (member instanceof FermataDotInter) {
                return (FermataDotInter) member;
            }
        }

        return null;
    }

    //------------//
    // getMembers //
    //------------//
    @Override
    public List<Inter> getMembers ()
    {
        return EnsembleHelper.getMembers(this, byFermataOrder);
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        bounds = null;
    }

    //-----------------//
    // linkWithBarline //
    //-----------------//
    /**
     * (Try to) connect this fermata with a suitable StaffBarline.
     *
     * @return true if successful
     */
    public boolean linkWithBarline ()
    {
        Link link = lookupBarlineLink(sig.getSystem());

        if (link != null) {
            link.applyTo(this);

            return true;
        }

        return false;
    }

    //---------------//
    // linkWithChord //
    //---------------//
    /**
     * (Try to) connect this fermata with suitable chord.
     *
     * @return true if successful
     */
    public boolean linkWithChord ()
    {
        Link link = lookupChordLink(sig.getSystem());

        if (link != null) {
            link.applyTo(this);

            return true;
        }

        return false;
    }

    //--------------//
    // removeMember //
    //--------------//
    @Override
    public void removeMember (Inter member)
    {
        if (!(member instanceof FermataArcInter) && !(member instanceof FermataDotInter)) {
            throw new IllegalArgumentException(
                    "Only FermataArcInter or FermataDotInter can be removed from FermataInter");
        }

        EnsembleHelper.removeMember(this, member);
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system,
                                         boolean doit)
    {
        // Not very optimized!
        List<Inter> systemHeadChords = system.getSig().inters(HeadChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        Link link = lookupBarlineLink(system);

        if (link == null) {
            link = lookupChordLink(system);
        }

        if (link == null) {
            return Collections.emptyList();
        }

        if (doit) {
            link.applyTo(this);
        }

        return Collections.singleton(link);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
    }

    //-------------------//
    // lookupBarlineLink //
    //-------------------//
    /**
     * Look for a suitable link with a staff barline.
     *
     * @param system containing system
     * @return suitable link or null
     */
    private Link lookupBarlineLink (SystemInfo system)
    {
        final Point center = getCenter();

        if (staff == null) {
            staff = (shape == Shape.FERMATA_BELOW) ? system.getStaffAtOrAbove(center)
                    : system.getStaffAtOrBelow(center);
        }

        if (staff == null) {
            return null;
        }

        List<StaffBarlineInter> staffBars = staff.getStaffBarlines();
        StaffBarlineInter staffBar = StaffBarlineInter.getClosestStaffBarline(staffBars, center);

        // Fermata and staff bar bounds must intersect horizontally
        if (staffBar == null || GeoUtil.xOverlap(getBounds(), staffBar.getBounds()) <= 0) {
            return null;
        }

        // Check vertical distance to bar/staff
        final Scale scale = system.getSheet().getScale();
        final int maxDy = scale.toPixels(constants.maxFermataDy);
        final int dyStaff = staff.distanceTo(center);

        if (dyStaff > maxDy) {
            logger.debug("{} too far from barline: {}", this, staffBar);

            return null;
        }

        return new Link(staffBar, new FermataBarRelation(), true);
    }

    //-----------------//
    // lookupChordLink //
    //-----------------//
    /**
     * Look for a suitable link with a standard chord (head or rest).
     *
     * @param system containing system
     * @return suitable link or null
     */
    private Link lookupChordLink (SystemInfo system)
    {
        getBounds();
        final Point center = getCenter();
        final MeasureStack stack = system.getStackAt(center);
        final Collection<AbstractChordInter> chords = (shape == Shape.FERMATA_BELOW) ? stack
                .getStandardChordsAbove(center, bounds)
                : stack.getStandardChordsBelow(center, bounds);

        // Look for a suitable chord related to this fermata
        AbstractChordInter chord = AbstractChordInter.getClosestChord(chords, center);

        if (chord == null) {
            return null;
        }

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
        final Scale scale = system.getSheet().getScale();
        final int maxDy = scale.toPixels(constants.maxFermataDy);

        if (dyChord > maxDy) {
            // Check vertical distance between fermata and staff
            final Staff chordStaff = (shape == Shape.FERMATA) ? chord.getTopStaff()
                    : chord.getBottomStaff();
            final int dyStaff = chordStaff.distanceTo(center);

            if (dyStaff > maxDy) {
                logger.debug("{} too far from staff/chord: {}", this, dyStaff);

                return null;
            }
        }

        return new Link(chord, new FermataChordRelation(), true);
    }

    //-------------//
    // createAdded //
    //-------------//
    /**
     * (Try to) create a fermata inter.
     *
     * @param arc    the fermata arc (shape: FERMATA_ARC or FERMATA_ARC_BELOW)
     * @param dot    the fermata dot
     * @param system the related system
     * @return the created instance or null
     */
    public static FermataInter createAdded (FermataArcInter arc,
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

        final SIGraph sig = arc.getSig();
        sig.computeContextualGrade(arc);
        sig.computeContextualGrade(dot);

        final double grade = 0.5 * (arc.getContextualGrade() + dot.getContextualGrade());
        final Shape shape = (arcShape == Shape.FERMATA_ARC) ? Shape.FERMATA : Shape.FERMATA_BELOW;
        final FermataInter fermata = new FermataInter(shape, grade);
        fermata.setStaff(staff);
        sig.addVertex(fermata);
        fermata.addMember(arc);
        fermata.addMember(dot);

        return fermata;
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction maxFermataDy = new Scale.Fraction(
                2.5,
                "Maximum vertical distance between fermata center and related chord/staff/barline");
    }
}
