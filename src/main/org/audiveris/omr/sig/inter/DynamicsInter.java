//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    D y n a m i c s I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.relation.ChordDynamicsRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>DynamicsInter</code> represents a dynamics indication (such as mf).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "dynamics")
public class DynamicsInter
        extends AbstractInter
        implements StringSymbolInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(DynamicsInter.class);

    /** Map Shape -> Signature. */
    private static final Map<Shape, String> sigs = new EnumMap<>(Shape.class);

    /** Map Signature -> Shape. */
    private static final Map<String, Shape> shapes = new HashMap<>();

    /** Map Shape -> Sound. (TODO: complete the table) */
    private static final Map<Shape, Integer> sounds = new EnumMap<>(Shape.class);

    static {
        //        // Additional characters : m, r, s & z
        //        sigs.put(Shape.DYNAMICS_CHAR_M, "m");
        //        sigs.put(Shape.DYNAMICS_CHAR_R, "r");
        //        sigs.put(Shape.DYNAMICS_CHAR_S, "s");
        //        sigs.put(Shape.DYNAMICS_CHAR_Z, "z");
        //
        // True dynamics symbols
        sigs.put(Shape.DYNAMICS_PP, "pp");
        sigs.put(Shape.DYNAMICS_P, "p");
        sigs.put(Shape.DYNAMICS_MP, "mp");
        //------------
        sigs.put(Shape.DYNAMICS_MF, "mf");
        sigs.put(Shape.DYNAMICS_F, "f");
        sigs.put(Shape.DYNAMICS_FF, "ff");
        sigs.put(Shape.DYNAMICS_FP, "fp"); // Forte then piano
        sigs.put(Shape.DYNAMICS_SF, "sf"); // Subito forte: suddenly strong
        sigs.put(Shape.DYNAMICS_SFZ, "sfz"); // Sforzando: sudden accent

        //        sigs.put(Shape.DYNAMICS_FFF, "fff");
        //        sigs.put(Shape.DYNAMICS_FZ, "fz");
        //        sigs.put(Shape.DYNAMICS_PPP, "ppp");
        //        sigs.put(Shape.DYNAMICS_RF, "rf");
        //        sigs.put(Shape.DYNAMICS_RFZ, "rfz");
        //        sigs.put(Shape.DYNAMICS_SF, "sf");
        //        sigs.put(Shape.DYNAMICS_SFFZ, "sffz");
        //        sigs.put(Shape.DYNAMICS_SFP, "sfp");
        //        sigs.put(Shape.DYNAMICS_SFPP, "sfpp");
    }

    static {
        shapes.put("pp", Shape.DYNAMICS_PP);
        shapes.put("p", Shape.DYNAMICS_P);
        shapes.put("mp", Shape.DYNAMICS_MP);
        shapes.put("mf", Shape.DYNAMICS_MF);
        shapes.put("f", Shape.DYNAMICS_F);
        shapes.put("ff", Shape.DYNAMICS_FF);
        shapes.put("fp", Shape.DYNAMICS_FP);
        shapes.put("sfz", Shape.DYNAMICS_SFZ);

        //        shapes.put("fff", Shape.DYNAMICS_FFF);
        //        shapes.put("fz", Shape.DYNAMICS_FZ);
        //        shapes.put("ppp", Shape.DYNAMICS_PPP);
        //        shapes.put("rf", Shape.DYNAMICS_RF);
        //        shapes.put("rfz", Shape.DYNAMICS_RFZ);
        //        shapes.put("sf", Shape.DYNAMICS_SF);
        //        shapes.put("sffz", Shape.DYNAMICS_SFFZ);
        //        shapes.put("sfp", Shape.DYNAMICS_SFP);
        //        shapes.put("sfpp", Shape.DYNAMICS_SFPP);
    }

    static {
        sounds.put(Shape.DYNAMICS_PP, 45);
        sounds.put(Shape.DYNAMICS_P, 56);
        sounds.put(Shape.DYNAMICS_MP, 67);
        //------------
        // Default: 78
        //------------
        sounds.put(Shape.DYNAMICS_MF, 89);
        sounds.put(Shape.DYNAMICS_F, 100);
        sounds.put(Shape.DYNAMICS_FF, 122);

        sounds.put(Shape.DYNAMICS_FP, 100); // ???
        sounds.put(Shape.DYNAMICS_SFZ, 100); // ???

        //        sounds.put(Shape.DYNAMICS_PPP, 34);
        //        sounds.put(Shape.DYNAMICS_FP, "fp");
        //        sounds.put(Shape.DYNAMICS_FZ, "fz");
        //        sounds.put(Shape.DYNAMICS_RF, "rf");
        //        sounds.put(Shape.DYNAMICS_RFZ, "rfz");
        //        sounds.put(Shape.DYNAMICS_SF, "sf");
        //        sounds.put(Shape.DYNAMICS_SFFZ, "sffz");
        //        sounds.put(Shape.DYNAMICS_SFP, "sfp");
        //        sounds.put(Shape.DYNAMICS_SFPP, "sfpp");
        //        sounds.put(Shape.DYNAMICS_SFZ, "sfz");
    }

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor meant for JAXB.
     */
    private DynamicsInter ()
    {
    }

    /**
     * Creates a new DynamicsInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public DynamicsInter (Glyph glyph,
                          Shape shape,
                          Double grade)
    {
        super(glyph, null, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // getSoundLevel //
    //---------------//
    /**
     * Report the sound level, if any, based on dynamics shape.
     *
     * @return sound level or null
     */
    public Integer getSoundLevel ()
    {
        Shape shape = getShape();

        if (shape != null) {
            return sounds.get(shape);
        } else {
            return null;
        }
    }

    //----------//
    // getStaff //
    //----------//
    @Override
    public Staff getStaff ()
    {
        if (staff == null) {
            // Chord -> Dynamic
            for (Relation cdRel : sig.getRelations(this, ChordDynamicsRelation.class)) {
                AbstractChordInter chord = (AbstractChordInter) sig.getOppositeInter(this, cdRel);

                if (chord.getStaff() != null) {
                    return staff = chord.getStaff();
                }
            }
        }

        return staff;
    }

    //-----------------//
    // getSymbolString //
    //-----------------//
    @Override
    public String getSymbolString ()
    {
        return sigs.get(shape);
    }

    //----------------//
    // linkWithChords //
    //----------------//
    /**
     * Try to connect this dynamics element with a suitable chord.
     *
     * @return true if successful
     */
    public boolean linkWithChord ()
    {
        Collection<Link> links = searchLinks(sig.getSystem());

        if (links.isEmpty()) {
            return false;
        }

        for (Link link : links) {
            link.applyTo(this);
        }

        return true;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Look up system for a potential link.
     *
     * @param system containing system
     * @return link or null
     */
    private Link lookupLink (SystemInfo system)
    {
        if (isVip()) {
            logger.info("VIP lookupLink for {}", this);
        }

        // Look for a suitable chord related to this dynamics element
        final Point center = getCenter();
        final MeasureStack stack = system.getStackAt(center);

        if (stack == null) {
            return null;
        }

        final Scale scale = system.getSheet().getScale();
        final int maxDy = scale.toPixels(constants.maxDy);
        final int maxXGap = scale.toPixels(constants.maxXGap);
        final Rectangle widenedBounds = getBounds();
        widenedBounds.grow(maxXGap, 0);

        for (VerticalSide side : VerticalSide.values()) {
            final boolean lookAbove = side == VerticalSide.TOP;
            AbstractChordInter chord = lookAbove ? stack.getStandardChordAbove(
                    center,
                    widenedBounds) : stack.getStandardChordBelow(center, widenedBounds);

            if ((chord == null) || chord instanceof RestChordInter) {
                continue;
            }

            double dyChord = GeoUtil.yGap(widenedBounds, chord.getBounds());

            // If chord is mirrored, select the closest vertically
            if (chord.getMirror() != null) {
                double dyMirror = GeoUtil.yGap(widenedBounds, chord.getMirror().getBounds());

                if (dyMirror < dyChord) {
                    dyChord = dyMirror;
                    chord = (AbstractChordInter) chord.getMirror();

                    if (isVip()) {
                        logger.info("VIP {} selecting mirror {}", this, chord);
                    }
                }
            }

            // Check vertical distance between element and chord
            if (dyChord > maxDy) {
                // Check vertical distance between element and staff
                final Staff chordStaff = lookAbove ? chord.getBottomStaff() : chord.getTopStaff();
                final double dyStaff = chordStaff.gapTo(widenedBounds);

                if (dyStaff > maxDy) {
                    if (isVip()) {
                        logger.info("VIP {} too far from staff/chord: {}", this, dyStaff);
                    }

                    continue;
                }
            }

            // For dynamics & for chord
            return new Link(chord, new ChordDynamicsRelation(), false);
        }

        return null;
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final Link link = lookupLink(system);

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, ChordDynamicsRelation.class);
    }

    //------------------------//
    // swallowShorterDynamics //
    //------------------------//
    /**
     * For a complex dynamics, try to swallow shorter ones.
     * <p>
     * We check a complex dynamics for overlap with shorter dynamics.
     * If shorter box is (nearly) included in complex box, we have a fit.
     * Then we assign complex the max grade of complex and shorter, and remove shorter
     *
     * @param dynamics candidate dynamics to be searched
     */
    public void swallowShorterDynamics (List<Inter> dynamics)
    {
        final Rectangle cplBox = getBounds();
        final String cplString = getSymbolString();
        final int cplLength = cplString.length();

        // Look for relevant candidate for the complex at hand
        for (Inter inter : Inters.intersectedInters(dynamics, GeoOrder.NONE, cplBox)) {
            final DynamicsInter shorter = (DynamicsInter) inter;
            final String shortString = shorter.getSymbolString();

            if ((shorter == this) || (shortString.length() >= cplLength) || !cplString.contains(
                    shortString)) {
                continue;
            }

            // Measure area of intersection over area of shorter box
            Rectangle shortBox = shorter.getBounds();
            double shortArea = shortBox.width * shortBox.height;
            int intArea = GeoUtil.xOverlap(shortBox, cplBox) * GeoUtil.yOverlap(shortBox, cplBox);
            double ios = intArea / shortArea;
            logger.debug("ios:{} {} intersects {}", ios, shorter, this);

            if (ios >= constants.iosMinRatio.getValue()) {
                setGrade(Math.max(getGrade(), shorter.getGrade()));

                if (shorter.isVip()) {
                    logger.info("VIP simple {} discarded for complex {}", shorter, this);
                }

                shorter.remove();
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction maxDy = new Scale.Fraction(
                7,
                "Maximum vertical distance between dynamics center and related chord/staff");

        private final Scale.Fraction maxXGap = new Scale.Fraction(
                1.0,
                "Maximum horizontal gap between dynamics and related chord/staff");

        private final Constant.Ratio iosMinRatio = new Constant.Ratio(
                0.8,
                "Minimum area ratio of intersection over shorter dynamics");
    }
}
