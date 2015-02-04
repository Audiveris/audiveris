//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S l u r I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.math.PointUtil;

import omr.sheet.Scale;
import omr.sheet.Staff;
import omr.sheet.curve.SlurInfo;

import omr.sig.BasicImpacts;
import omr.sig.GradeImpacts;
import omr.sig.SIGraph;
import omr.sig.relation.Relation;
import omr.sig.relation.SlurHeadRelation;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class {@code SlurInter} represents a slur interpretation.
 *
 * @author Hervé Bitteur
 */
public class SlurInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SlurInter.class);

    /** To sort slurs vertically within a measure. */
    public static final Comparator<SlurInter> verticalComparator = new Comparator<SlurInter>()
    {
        @Override
        public int compare (SlurInter s1,
                            SlurInter s2)
        {
            return Double.compare(s1.getInfo().getCurve().getY1(), s2.getInfo().getCurve().getY1());
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
                //                // Check we are in last measure
                //                Point2D p2 = slur.getCurve().getP2();
                //                OldMeasure measure = slur.getPart()
                //                        .getMeasureAt(new Point((int) p2.getX(), (int) p2.getY()));
                //
                //                if (measure == slur.getPart().getLastMeasure()) {
                //                    // Check slur ends in last measure half
                //                    if (p2.getX() > measure.getCenter().x) {
                //                        return true;
                //                    }
                //                }
                return true;
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
                //                // Check we are in first measure
                //                Point2D p1 = slur.getCurve().getP1();
                //                OldMeasure measure = slur.getPart()
                //                        .getMeasureAt(new Point((int) p1.getX(), (int) p1.getY()));
                //
                //                if (measure == slur.getPart().getFirstMeasure()) {
                //                    // Check slur begins in first measure half
                //                    if (p1.getX() < measure.getCenter().x) {
                //                        return true;
                //                    }
                //                }
                return true;
            }

            return false;
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Physical characteristics. */
    private final SlurInfo info;

    /** Is a this a tie?. (or a plain slur) */
    private boolean tie;

    /** Extension slur, if any. (at most one per slur) */
    private final Map<HorizontalSide, SlurInter> extensions = new EnumMap<HorizontalSide, SlurInter>(
            HorizontalSide.class);

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

        // To debug attachments
        for (Entry<String, java.awt.Shape> entry : info.getAttachments().entrySet()) {
            addAttachment(entry.getKey(), entry.getValue());
        }
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

    //-----------//
    // connectTo //
    //-----------//
    /**
     * Make the connection with another slur in the previous system.
     *
     * @param prevSlur slur at the end of previous system
     */
    public void connectTo (SlurInter prevSlur)
    {
        // Cross-extensions
        this.extensions.put(LEFT, prevSlur);
        prevSlur.extensions.put(RIGHT, this);

        // Tie?
        boolean isATie = haveSameHeight(prevSlur.getHead(LEFT), this.getHead(RIGHT));
        prevSlur.tie = isATie;
        this.tie = isATie;

        logger.debug("{} connection {} -> {}", isATie ? "Tie" : "Slur", prevSlur, this);
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        if (tie) {
            sb.append(" tie");
        }

        sb.append(" ").append(info);

        return sb.toString();
    }

    //--------------//
    // getExtension //
    //--------------//
    public SlurInter getExtension (HorizontalSide side)
    {
        return extensions.get(side);
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
    public AbstractHeadInter getHead (HorizontalSide side)
    {
        final SIGraph sig = getSig();
        final Set<Relation> rels = sig.getRelations(this, SlurHeadRelation.class);

        for (Relation rel : rels) {
            SlurHeadRelation shRel = (SlurHeadRelation) rel;

            if (shRel.getSide() == side) {
                return (AbstractHeadInter) sig.getOppositeInter(this, rel);
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
        return tie;
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
    private static boolean haveSameHeight (AbstractHeadInter n1,
                                           AbstractHeadInter n2)
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
        double prevPp = prevStaff.pitchPositionOf(
                PointUtil.integer(prevSlur.getInfo().getCurve().getP2()));

        // Retrieve position, using the left point of the slur
        double pp = thisStaff.pitchPositionOf(PointUtil.integer(info.getCurve().getP1()));

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

        Scale.Fraction maxDeltaY = new Scale.Fraction(
                4,
                "Maximum vertical difference in interlines between connecting slurs");
    }
}
