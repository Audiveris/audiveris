//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     F e r m a t a I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.symbol.SymbolsBuilder;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.FermataBarRelation;
import org.audiveris.omr.sig.relation.FermataChordRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>FermataInter</code> represents a full fermata interpretation, either upright
 * or inverted, combining an arc and a dot.
 * <p>
 * <img src=
 * "http://upload.wikimedia.org/wikipedia/commons/thumb/9/90/Urlinie_in_G_with_fermata.png/220px-Urlinie_in_G_with_fermata.png"
 * alt="Urlinie in G with fermata on penultimate note">
 * <p>
 * An upright fermata refers to the chord in the staff right below in the containing part.
 * An inverted fermata refers to the chord in the staff right above in the containing part.
 * A fermata may also refer to a single or double bar-line, to indicate the end of a phrase.
 * <p>
 * Such reference is implemented via a Relation instance: either a {@link FermataChordRelation}
 * or a {@link FermataBarRelation}.
 * <p>
 * History: Initially, the fermata arc and the fermata dot were kept as separated inters
 * because they were often too distant to be grabbed by the {@link SymbolsBuilder}.
 * Since the SymbolsBuilder now accepts larger gaps, there is no more need for these member inters.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "fermata")
public class FermataInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(FermataInter.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    @SuppressWarnings("unused")
    private FermataInter ()
    {
    }

    /**
     * Creates a new <code>FermataInter</code> object.
     *
     * @param glyph the fermata glyph (arc + dot)
     * @param shape the fermata shape (FERMATA or FERMATA_BELOW)
     * @param grade the interpretation quality
     */
    public FermataInter (Glyph glyph,
                         Shape shape,
                         Double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------------//
    // upgradeOldStuff //
    //-----------------//
    /**
     * Convert old fermata based on arc + dot ensemble.
     * <p>
     * Since the (new) FermataInter is no longer an ensemble, we deal with Containment relations
     * and former members directly.
     *
     * @return true if really upgraded
     */
    @SuppressWarnings("deprecation")
    public boolean upgradeOldStuff ()
    {
        try {
            Relation arcRel = null;
            Relation dotRel = null;
            FermataArcInter arc = null;
            FermataDotInter dot = null;

            for (Relation rel : sig.getRelations(this, Containment.class)) {
                if (sig.getEdgeSource(rel) == this) {
                    final Inter member = sig.getOppositeInter(this, rel);
                    switch (member) {
                        case FermataArcInter fermataArcInter -> {
                            arc = fermataArcInter;
                            arcRel = rel;
                        }
                        case FermataDotInter fermataDotInter -> {
                            dot = fermataDotInter;
                            dotRel = rel;
                        }
                        default -> {}
                    }
                }
            }

            if ((arc != null) && (dot != null)) {
                final GlyphIndex index = sig.getSystem().getSheet().getGlyphIndex();
                glyph = index.registerOriginal(
                        GlyphFactory.buildGlyph(Arrays.asList(arc.getGlyph(), dot.getGlyph())));
                bounds = glyph.getBounds();

                // Cut the containment relations to preserve fermata inter
                sig.removeEdge(arcRel);
                sig.removeEdge(dotRel);

                // Remove the former members
                arc.remove();
                dot.remove();

                return true;
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " upgradeOldStuff() " + ex, ex);
        }

        return false;
    }

    //-----------------//
    // linkWithBarline //
    //-----------------//
    /**
     * (Try to) connect this fermata with a suitable StaffBarline.
     *
     * @param profile desired profile level
     * @return true if successful
     */
    public boolean linkWithBarline (int profile)
    {
        Link link = lookupBarlineLink(sig.getSystem(), profile);

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
     * @param profile desired profile level
     * @return true if successful
     */
    public boolean linkWithChord (int profile)
    {
        Link link = lookupChordLink(sig.getSystem(), profile);

        if (link != null) {
            link.applyTo(this);

            return true;
        }

        return false;
    }

    //-------------------//
    // lookupBarlineLink //
    //-------------------//
    /**
     * Look for a suitable link with a staff barline.
     *
     * @param system  containing system
     * @param profile desired profile level
     * @return suitable link or null
     */
    private Link lookupBarlineLink (SystemInfo system,
                                    int profile)
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
        if ((staffBar == null) || (GeoUtil.xOverlap(getBounds(), staffBar.getBounds()) <= 0)) {
            return null;
        }

        // Check vertical distance to bar/staff
        final Scale scale = system.getSheet().getScale();
        final int maxDy = scale
                .toPixels((Scale.Fraction) constants.getConstant(constants.maxFermataDy, profile));
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
     * @param system  containing system
     * @param profile desired profile level
     * @return suitable link or null
     */
    private Link lookupChordLink (SystemInfo system,
                                  int profile)
    {
        getBounds();

        final Point center = getCenter();
        final MeasureStack stack = system.getStackAt(center);

        if (stack == null) {
            return null;
        }

        final Collection<AbstractChordInter> chords = (shape == Shape.FERMATA_BELOW) //
                ? stack.getStandardChordsAbove(center, bounds)
                : stack.getStandardChordsBelow(center, bounds);

        // Look for a suitable chord related to this fermata
        AbstractChordInter chord = AbstractChordInter.getClosestChord(chords, center);

        if (chord == null) {
            return null;
        }

        double dyChord = Math.sqrt(GeoUtil.ptDistanceSq(chord.getBounds(), center.x, center.y));

        // If chord is mirrored, select the closest vertically
        if (chord.getMirror() != null) {
            double dyMirror = Math
                    .sqrt(GeoUtil.ptDistanceSq(chord.getMirror().getBounds(), center.x, center.y));

            if (dyMirror < dyChord) {
                dyChord = dyMirror;
                chord = (AbstractChordInter) chord.getMirror();
                logger.debug("{} selecting mirror {}", this, chord);
            }
        }

        // Check vertical distance between fermata and chord
        final Scale scale = system.getSheet().getScale();
        final int maxDy = scale
                .toPixels((Scale.Fraction) constants.getConstant(constants.maxFermataDy, profile));

        if (dyChord > maxDy) {
            // Check vertical distance between fermata and staff
            final Staff chordStaff =
                    (shape == Shape.FERMATA) ? chord.getTopStaff() : chord.getBottomStaff();
            final int dyStaff = chordStaff.distanceTo(center);

            if (dyStaff > maxDy) {
                logger.debug("{} too far from staff/chord: {}", this, dyStaff);

                return null;
            }
        }

        return new Link(chord, new FermataChordRelation(), true);
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final int profile = Math.max(getProfile(), system.getProfile());

        Link link = lookupBarlineLink(system, profile);

        if (link == null) {
            link = lookupChordLink(system, profile);
        }

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, FermataBarRelation.class, FermataChordRelation.class);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // createValidAdded //
    //------------------//
    /**
     * (Try to) create and add a valid fermata inter.
     *
     * @param glyph  the fermata glyph (arc + dot)
     * @param shape  FERMATA or FERMATA_BELOW
     * @param grade  the interpretation quality
     * @param system the related system
     * @return the created instance or null
     */
    public static FermataInter createValidAdded (Glyph glyph,
                                                 Shape shape,
                                                 double grade,
                                                 SystemInfo system)
    {
        // Look for proper staff
        final Point2D center = glyph.getCenter2D();
        final Staff staff = (shape == Shape.FERMATA) ? system.getStaffAtOrBelow(center)
                : system.getStaffAtOrAbove(center);

        if (staff == null) {
            return null;
        }

        final FermataInter fermata = new FermataInter(glyph, shape, grade);
        fermata.setStaff(staff);
        staff.getSystem().getSig().addVertex(fermata);

        return fermata;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

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
