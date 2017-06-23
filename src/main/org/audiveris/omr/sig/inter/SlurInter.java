//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S l u r I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.curve.SlurInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.BasicImpacts;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code SlurInter} represents a slur interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "slur")
public class SlurInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SlurInter.class);

    /** To sort slurs vertically within a measure. */
    public static final Comparator<SlurInter> verticalComparator = new Comparator<SlurInter>()
    {
        @Override
        public int compare (SlurInter s1,
                            SlurInter s2)
        {
            return Double.compare(s1.getCurve().getY1(), s2.getCurve().getY1());
        }
    };

    /**
     * Predicate for a slur not connected on both ends.
     */
    public static final Predicate<SlurInter> isOrphan = new Predicate<SlurInter>()
    {
        @Override
        public boolean check (SlurInter slur)
        {
            for (HorizontalSide side : HorizontalSide.values()) {
                if (slur.getHead(side) == null) {
                    return true;
                }
            }

            return false;
        }
    };

    /** Predicate for an orphan slur at the end of its system/part. */
    public static final Predicate<SlurInter> isEndingOrphan = new Predicate<SlurInter>()
    {
        @Override
        public boolean check (SlurInter slur)
        {
            if (slur.getHead(RIGHT) == null) {
                // Check we are in last measure
                Point2D end = slur.getCurve().getP2();
                SystemInfo system = slur.getSig().getSystem();
                MeasureStack stack = system.getMeasureStackAt(end);

                if (stack == system.getLastMeasureStack()) {
                    // Check slur ends in last measure half
                    Staff staff = system.getClosestStaff(end);
                    Measure measure = stack.getMeasureAt(staff);
                    int middle = measure.getAbscissa(LEFT, staff) + (measure.getWidth() / 2);

                    if (end.getX() > middle) {
                        return true;
                    }
                }
            }

            return false;
        }
    };

    /** Predicate for an orphan slur at the beginning of its system/part. */
    public static final Predicate<SlurInter> isBeginningOrphan = new Predicate<SlurInter>()
    {
        @Override
        public boolean check (SlurInter slur)
        {
            if (slur.getHead(LEFT) == null) {
                // Check we are in first measure
                Point2D end = slur.getCurve().getP1();
                SystemInfo system = slur.getSig().getSystem();
                MeasureStack stack = system.getMeasureStackAt(end);

                if (stack == system.getFirstMeasureStack()) {
                    // Check slur ends in first measure half (excluding header area)
                    Staff staff = system.getClosestStaff(end);
                    Measure measure = stack.getMeasureAt(staff);
                    int middle = (staff.getHeaderStop() + measure.getAbscissa(LEFT, staff)
                                  + measure.getWidth()) / 2;

                    if (end.getX() < middle) {
                        return true;
                    }
                }
            }

            return false;
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Is the slur above heads or below heads. */
    @XmlAttribute
    private boolean above;

    /** Is this a tie?. (rather than a plain slur) */
    @XmlAttribute
    private Boolean tie;

    /** The precise Bézier curve. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.CubicAdapter.class)
    private CubicCurve2D curve;

    /** Extension slur on left, if any (within the same sheet). */
    @XmlIDREF
    @XmlAttribute(name = "left-extension")
    private SlurInter leftExtension;

    /** Extension slur on right, if any (within the same sheet). */
    @XmlIDREF
    @XmlAttribute(name = "right-extension")
    private SlurInter rightExtension;

    // Transient data
    //---------------
    //
    /** Physical characteristics. */
    private final SlurInfo info;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SlurInter object.
     *
     * @param info    the underlying slur information
     * @param impacts the assignment details
     */
    public SlurInter (SlurInfo info,
                      GradeImpacts impacts)
    {
        super(info.getGlyph(), info.getBounds(), Shape.SLUR, impacts);
        this.info = info;

        above = info.above() == 1;
        curve = info.getCurve();

        // To debug attachments
        for (Entry<String, java.awt.Shape> entry : info.getAttachments().entrySet()) {
            addAttachment(entry.getKey(), entry.getValue());
        }
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private SlurInter ()
    {
        info = null;
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

    //-----------//
    // canExtend //
    //-----------//
    /**
     * Check whether this slur can extend the prevSlur of the preceding system.
     *
     * @param prevSlur the slur candidate in the preceding system
     * @return true if connection is possible
     */
    public boolean canExtend (SlurInter prevSlur)
    {
        return (this.getExtension(LEFT) == null) && (prevSlur.getExtension(RIGHT) == null)
               && this.isCompatibleWith(prevSlur);
    }

    //----------//
    // checkTie //
    //----------//
    /**
     * Check whether the cross-system slur connection is a tie.
     *
     * @param prevSlur slur at the end of previous system (perhaps in previous sheet)
     */
    public void checkTie (SlurInter prevSlur)
    {
        // Tie?
        boolean isATie = haveSameHeight(prevSlur.getHead(LEFT), this.getHead(RIGHT));

        if (isATie) {
            prevSlur.setTie();
            setTie();
        }

        logger.debug("{} connection {} -> {}", isATie ? "Tie" : "Slur", prevSlur, this);
    }

    //--------//
    // delete //
    //--------//
    /**
     * Since a slur instance is held by its containing part, make sure part
     * slurs collection is updated.
     *
     * @see #undelete()
     */
    @Override
    public void delete ()
    {
        if (part != null) {
            part.removeSlur(this);
        }

        super.delete();
    }

    //----------//
    // getCurve //
    //----------//
    /**
     * Report the left-to-right Bézier curve which best approximates the slur.
     *
     * @return the Bézier curve
     */
    public CubicCurve2D getCurve ()
    {
        return curve;
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        if (isTie()) {
            sb.append(" tie");
        }

        if (info != null) {
            sb.append(" ").append(info);
        }

        return sb.toString();
    }

    //--------------//
    // getExtension //
    //--------------//
    /**
     * Report the connected slur, if any, in the other system on the provided side
     * (only within the same sheet).
     *
     * @param side the desired side
     * @return the connected slur, if any, in the other system within the same sheet.
     */
    public SlurInter getExtension (HorizontalSide side)
    {
        Objects.requireNonNull(side, "No side provided for slur getExtension");

        return (side == HorizontalSide.LEFT) ? leftExtension : rightExtension;
    }

    //---------//
    // getHead //
    //---------//
    /**
     * Report the note head, if any, embraced on the specified side.
     *
     * @param side the desired side
     * @return the connected head on this side, if any
     */
    public HeadInter getHead (HorizontalSide side)
    {
        for (Relation rel : sig.getRelations(this, SlurHeadRelation.class)) {
            SlurHeadRelation shRel = (SlurHeadRelation) rel;

            if (shRel.getSide() == side) {
                return (HeadInter) sig.getOppositeInter(this, rel);
            }
        }

        return null;
    }

    //---------//
    // getInfo //
    //---------//
    public SlurInfo getInfo ()
    {
        return info;
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    /**
     * We use curve middle point rather than bounds center.
     * P: middle of segment P1..P2
     * C: middle of segment CP1..CP2
     * M: middle of curve
     * PM = 3/4 * PC
     *
     * @return curve middle point
     */
    @Override
    public Point getRelationCenter ()
    {
        final Point2D m = new Point2D.Double(
                (0.125 * curve.getX1()) + (0.125 * curve.getX2()) + (0.375 * curve.getCtrlX1())
                + (0.375 * curve.getCtrlX2()),
                ((0.125 * curve.getY1()) + (0.125 * curve.getY2()))
                + ((0.375 * curve.getCtrlY1()) + (0.375 * curve.getCtrlY2())));

        return PointUtil.rounded(m);
    }

    //---------//
    // isAbove //
    //---------//
    /**
     * @return the above
     */
    public boolean isAbove ()
    {
        return above;
    }

    //-------//
    // isTie //
    //-------//
    /**
     * Report whether this slur is actually a tie (a slur between same pitched notes).
     *
     * @return true if is a tie, false otherwise
     */
    public boolean isTie ()
    {
        return (tie != null) && tie;
    }

    //--------------//
    // setExtension //
    //--------------//
    /**
     * Link the slur to another one, on the provided side.
     *
     * @param side  the provided side
     * @param other the other slur
     */
    public void setExtension (HorizontalSide side,
                              SlurInter other)
    {
        Objects.requireNonNull(side, "No side provided for slur setExtension");

        if (side == HorizontalSide.LEFT) {
            leftExtension = other;
        } else {
            rightExtension = other;
        }
    }

    //--------//
    // setTie //
    //--------//
    /**
     * Set this slur as being a tie.
     */
    public void setTie ()
    {
        tie = Boolean.TRUE;
    }

    //------------------//
    // switchMirrorHead //
    //------------------//
    /**
     * Switch the slur link from head to its mirror on the provided side.
     *
     * @param side the desired side
     */
    public void switchMirrorHead (HorizontalSide side)
    {
        for (Relation rel : sig.getRelations(this, SlurHeadRelation.class)) {
            SlurHeadRelation shRel = (SlurHeadRelation) rel;

            if (shRel.getSide() == side) {
                HeadInter head = (HeadInter) sig.getOppositeInter(this, rel);
                HeadInter mirrorHead = (HeadInter) head.getMirror();

                if (mirrorHead != null) {
                    sig.removeEdge(rel);
                    sig.addEdge(this, mirrorHead, rel);
                } else {
                    logger.error("No mirror head for {}", head);
                }
            }
        }
    }

    //----------------//
    // haveSameHeight //
    //----------------//
    /**
     * Check whether two notes represent the same pitch (same octave, same step).
     * This is needed to detects tie slurs.
     *
     * @param n1 one note
     * @param n2 the other note
     * @return true if the notes are equivalent.
     */
    private static boolean haveSameHeight (HeadInter n1,
                                           HeadInter n2)
    {
        return (n1 != null) && (n2 != null) && (n1.getStep() == n2.getStep())
               && (n1.getOctave() == n2.getOctave());

        // TODO: what about alteration, if we have not processed them yet ???
    }

    //------------------//
    // isCompatibleWith //
    //------------------//
    /**
     * Check whether two slurs to-be-connected between two systems in sequence are
     * roughly compatible with each other. (same staff id, and similar pitch positions).
     *
     * @param prevSlur the previous slur
     * @return true if found compatible
     */
    private boolean isCompatibleWith (SlurInter prevSlur)
    {
        // Retrieve prev staff, using the left note head of the prev slur
        Staff prevStaff = prevSlur.getHead(LEFT).getStaff();

        // Retrieve this staff, using the right note head of this slur
        Staff thisStaff = getHead(RIGHT).getStaff();

        // Check that part-based staff indices are the same
        if (prevStaff.getIndexInPart() != thisStaff.getIndexInPart()) {
            logger.debug(
                    "{} prevStaff:{} {} staff:{} different part-based staff indices",
                    prevSlur,
                    prevStaff.getId(),
                    this,
                    thisStaff.getId());

            return false;
        }

        // Retrieve prev position, using the right point of the prev slur
        double prevPp = prevStaff.pitchPositionOf(PointUtil.rounded(prevSlur.getCurve().getP2()));

        // Retrieve position, using the left point of the slur
        double pp = thisStaff.pitchPositionOf(PointUtil.rounded(getCurve().getP1()));

        // Compare pitch positions (very roughly)
        double deltaPitch = pp - prevPp;
        boolean res = Math.abs(deltaPitch) <= (constants.maxDeltaY.getValue() * 2);
        logger.debug("{} --- {} deltaPitch:{} res:{}", prevSlur, this, deltaPitch, res);

        return res;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{
            "dist", "angle", "width", "height", "vert"
        };

        private static final double[] WEIGHTS = new double[]{3, 1, 1, 1, 1};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double dist,
                        double angle,
                        double width,
                        double height,
                        double vert)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, dist);
            setImpact(1, angle);
            setImpact(2, width);
            setImpact(3, height);
            setImpact(4, vert);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction maxDeltaY = new Scale.Fraction(
                4,
                "Maximum vertical difference in interlines between connecting slurs");
    }
}
