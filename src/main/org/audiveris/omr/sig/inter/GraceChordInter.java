//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  G r a c e C h o r d I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.GRACE_NOTE;
import static org.audiveris.omr.glyph.Shape.GRACE_NOTE_DOWN;
import static org.audiveris.omr.glyph.Shape.GRACE_NOTE_SLASH;
import static org.audiveris.omr.glyph.Shape.GRACE_NOTE_SLASH_DOWN;
import org.audiveris.omr.image.Anchored.Anchor;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.relation.ChordGraceRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.symbol.Family;
import org.audiveris.omr.ui.symbol.FontSymbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>GraceChordInter</code> represents a (slashed) Acciaccatura or a (not slashed)
 * Appoggiatura.
 * <p>
 * An acciaccatura or appoggiatura could originate from a glyph recognized as a whole with shape
 * GRACE_NOTE, GRACE_NOTE_DOWN, GRACE_NOTE_SLASH or GRACE_NOTE_SLASH_DOWN.
 * In that case, we generate one instance of GraceChordInter and assign it the shape.
 * <p>
 * Other chord instances containing several (small) heads don't have a shape assigned
 * and behave as "ordinary" {@link SmallChordInter} instances.
 * <p>
 * NOTA: We create a (hidden) HeadInter and a hidden StemInter and put them into SIG,
 * because of the need to support potential slur and accidental (and counting dots at reload!).
 * However, we don't need to create any artificial flag.
 *
 * @see SmallChordInter
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "grace-chord")
public class GraceChordInter
        extends SmallChordInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(GraceChordInter.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new GraceChordInter object from a glyph recognized as a whole grace note.
     *
     * @param glyph underlying glyph
     * @param shape GRACE_NOTE, GRACE_NOTE_DOWN, GRACE_NOTE_SLASH or GRACE_NOTE_SLASH_DOWN
     * @param grade evaluation value
     */
    public GraceChordInter (Glyph glyph,
                            Shape shape,
                            Double grade)
    {
        super(glyph, shape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private GraceChordInter ()
    {
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

    //---------------//
    // createMembers //
    //---------------//
    /**
     * Allocate (hidden) head and (hidden) stem for this grace chord.
     */
    private void createMembers ()
    {
        // Head
        final HeadInter head = new HiddenHeadInter();
        head.setGrade(getGrade());

        head.shape = getHeadShape();
        head.staff = staff;
        head.sig = sig;
        final Sheet sheet = sig.getSystem().getSheet();
        head.bounds = getHeadBounds(getHeadShape(), getBounds(), sheet);
        headLocation = GeoUtil.center(head.bounds);
        final boolean isUp = isUp();
        final Anchor anchor = isUp ? Anchor.TOP_RIGHT_STEM : Anchor.BOTTOM_LEFT_STEM;
        final Point extPt = PointUtil.rounded(head.getStemReferencePoint(anchor));
        tailLocation = new Point(extPt.x, bounds.y + (isUp ? 0 : bounds.height - 1));
        head.pitch = staff.pitchPositionOf(headLocation);

        // Stem
        final StemInter stem = new HiddenStemInter();
        stem.setGrade(getGrade());
        stem.setMedian(isUp ? tailLocation : extPt, isUp ? extPt : tailLocation);

        // Sig
        sig.addVertex(head);
        sig.addVertex(stem);
        sig.addEdge(head, stem, new HeadStemRelation());
        addMember(head);
        setStem(stem);
    }

    //------------------//
    // createValidAdded //
    //------------------//
    /**
     * (Try to) create and add a valid GraceChordInter from a glyph recognized as a grace note.
     * <p>
     * Suited for grace notes which are located on left side of a standard chord.
     *
     * @param glyph            underlying glyph
     * @param shape            detected shape
     * @param grade            assigned grade
     * @param system           containing system
     * @param systemHeadChords system head chords, ordered by abscissa
     * @return the created inter or null
     */
    public static GraceChordInter createValidAdded (Glyph glyph,
                                                    Shape shape,
                                                    double grade,
                                                    SystemInfo system,
                                                    List<Inter> systemHeadChords)
    {
        if (glyph.isVip()) {
            logger.info("VIP GraceChordInter create {} as {}", glyph, shape);
        }

        final GraceChordInter graceChord = new GraceChordInter(glyph, shape, grade);
        final Link link = graceChord.lookupLink(systemHeadChords, system.getProfile());

        if (link != null) {
            system.getSig().addVertex(graceChord);
            link.applyTo(graceChord);
            graceChord.createMembers();

            return graceChord;
        }

        return null;
    }

    //----------------------------//
    // getDurationSansDotOrTuplet //
    //----------------------------//
    /**
     * {@inheritDoc}
     *
     * @return for GraceChordInter: always 1/8, since all grace shapes have exactly one flag.
     */
    @Override
    public Rational getDurationSansDotOrTuplet ()
    {
        return new Rational(1, 8);
    }

    //---------------//
    // getHeadBounds //
    //---------------//
    /**
     * Report the bounds of head, knowing the bounds of whole grace note.
     *
     * @param headShape the shape of just the grace head
     * @param graceBox  the bounds of whole grace note (with stem and perhaps slash)
     * @param sheet     the containing sheet
     * @return head bounds
     */
    private Rectangle getHeadBounds (Shape headShape,
                                     Rectangle graceBox,
                                     Sheet sheet)
    {
        final Dimension dim = getHeadDimension(headShape, sheet);

        if (isUp()) {
            // Head is on bottom left corner of whole box
            return new Rectangle(
                    graceBox.x,
                    graceBox.y + graceBox.height - 1 - dim.height,
                    dim.width,
                    dim.height);
        } else {
            // Head is on top right corner of whole box
            return new Rectangle(
                    graceBox.x + graceBox.width - 1 - dim.width,
                    graceBox.y,
                    dim.width,
                    dim.height);
        }
    }

    //------------------//
    // getHeadDimension //
    //------------------//
    /**
     * Report the dimension of head part.
     *
     * @param headShape the shape of just the grace head
     * @param sheet     the containing sheet
     * @return head bounds
     */
    private Dimension getHeadDimension (Shape headShape,
                                        Sheet sheet)
    {
        final Scale scale = sheet.getScale();
        final Family family = sheet.getStub().getMusicFontFamily();
        final FontSymbol fs = headShape.getFontSymbol(family, scale.getInterline());
        return fs.symbol.getDimension(fs.font);
    }

    //--------------//
    // getHeadShape //
    //--------------//
    /**
     * Report the head shape of this grace chord.
     * <p>
     * TODO: Currently, grace head shape is limited to small oval, but perhaps we should support
     * other motifs: cross, triangle, ...
     *
     * @return head shape
     */
    public Shape getHeadShape ()
    {
        return Shape.NOTEHEAD_BLACK_SMALL;
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        if (shape != null) {
            return shape.toString(); // Case of a simple grace note
        } else {
            return "SmallChord";
        }
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, ChordGraceRelation.class)) {
            AbstractChordInter stdChord = (AbstractChordInter) sig.getOppositeInter(this, rel);
            return stdChord.getVoice();
        }

        return null;
    }

    //----------//
    // hasSlash //
    //----------//
    /**
     * Report whether the grace has a slash or not.
     *
     * @return true if so
     */
    @Override
    public boolean hasSlash ()
    {
        return switch (shape) {
        case GRACE_NOTE_SLASH, GRACE_NOTE_SLASH_DOWN -> true;
        case GRACE_NOTE, GRACE_NOTE_DOWN -> false;
        default -> throw new IllegalArgumentException("Unsupported grace Shape " + shape);
        };
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    /**
     * Void implementation, otherwise the addition of head or stem would invalidate the
     * grace chord.
     */
    @Override
    public void invalidateCache ()
    {
    }

    //------------//
    // isEditable //
    //------------//
    /**
     * A chord, being an ensemble, is by default non editable, but this GraceChordInter should
     * be editable as a whole.
     *
     * @return true
     */
    @Override
    public boolean isEditable ()
    {
        return shape != null;
    }

    //------//
    // isUp //
    //------//
    /**
     * Report whether the grace stem is up.
     *
     * @return true if up
     */
    public boolean isUp ()
    {
        return switch (shape) {
        case GRACE_NOTE, GRACE_NOTE_SLASH -> true;
        case GRACE_NOTE_DOWN, GRACE_NOTE_SLASH_DOWN -> false;
        default -> throw new IllegalArgumentException("Unsupported grace Shape " + shape);
        };
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this grace instance and a HeadChord on the right.
     * <p>
     * Grace head ordinate must be rather close to related chord head ordinate:
     * <ul>
     * <li>For melodic staff, delta pitch position = 1
     * <li>For percussion staff, delta pitch position = 0
     * </ul>
     *
     * @param systemHeadChords abscissa-ordered collection of head chords in system
     * @param profile          desired profile level
     * @return the link found or null
     */
    private Link lookupLink (List<Inter> systemHeadChords,
                             int profile)
    {
        if (systemHeadChords.isEmpty()) {
            return null;
        }

        final SystemInfo system = systemHeadChords.get(0).getSig().getSystem();
        final Sheet sheet = system.getSheet();
        final Scale scale = system.getSheet().getScale();
        final Rectangle graceBox = getBounds();
        final Rectangle headBox = getHeadBounds(getHeadShape(), graceBox, sheet);
        final Point ghc = GeoUtil.center(headBox);

        if (staff == null) {
            staff = system.getClosestStaff(ghc);
        }

        final Point target = new Point(ghc.x + scale.toPixels(constants.sideDx), ghc.y);

        final int maxDx = scale.toPixels(ChordGraceRelation.getXOutGapMaximum(profile));
        final int maxDy = scale.toPixels(ChordGraceRelation.getYGapMaximum(profile));
        final Rectangle luBox = new Rectangle(target);
        final boolean isDrum = staff.getPart().isDrumPart();
        luBox.grow(maxDx, isDrum ? maxDy / 2 : maxDy);

        final List<Inter> chords = Inters.intersectedInters(
                systemHeadChords,
                GeoOrder.BY_ABSCISSA,
                luBox);

        if (chords.isEmpty()) {
            return null;
        }

        // Choose the chord whose ending head is euclidian-wise closest to grace head center
        ChordGraceRelation bestRel = null;
        Inter bestChord = null;
        double bestDist = Double.MAX_VALUE;

        for (Inter inter : chords) {
            final HeadChordInter chord = (HeadChordInter) inter;
            Rectangle chordBox = chord.getBounds();

            // The ornament cannot intersect the chord
            if (chordBox.intersects(graceBox)) {
                continue;
            }

            final Point hc = chord.getLeadingNote().getCenter(); // Head center
            final Point vector = PointUtil.subtraction(hc, target); // target -> head center

            final double dist = PointUtil.length(vector);
            double xGap = Math.abs(vector.x);
            double yGap = Math.abs(vector.y);
            ChordGraceRelation rel = new ChordGraceRelation();
            rel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), profile);

            if (rel.getGrade() >= rel.getMinGrade()) {
                if ((bestRel == null) || (bestDist > dist)) {
                    bestRel = rel;
                    bestChord = chord;
                    bestDist = dist;
                }
            }
        }

        if (bestRel != null) {
            return new Link(bestChord, bestRel, false);
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction sideDx = new Scale.Fraction(
                1.5,
                "Typical horizontal distance between grace head center & target center");
    }

    //-----------------//
    // HiddenHeadInter //
    //-----------------//
    /**
     * A (hidden) head inter contained in this GraceChordInter.
     */
    @XmlRootElement(name = "hidden-head")
    public static class HiddenHeadInter
            extends HeadInter
    {

        @Override
        public void accept (InterVisitor visitor)
        {
            // Not painted
        }

        @Override
        public boolean isEditable ()
        {
            return false;
        }
    }

    //-----------------//
    // HiddenStemInter //
    //-----------------//
    /**
     * A (hidden) stem inter contained in this GraceChordInter.
     */
    @XmlRootElement(name = "hidden-stem")
    public static class HiddenStemInter
            extends StemInter
    {

        @Override
        public void accept (InterVisitor visitor)
        {
            // Not painted
        }

        @Override
        public boolean isEditable ()
        {
            return false;
        }
    }
}
