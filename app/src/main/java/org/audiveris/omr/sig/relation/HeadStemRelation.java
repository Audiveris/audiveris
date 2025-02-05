//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  H e a d S t e m R e l a t i o n                               //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.image.Anchored.Anchor;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterPair;
import org.audiveris.omr.sig.inter.StemInter;
import static org.audiveris.omr.sig.relation.StemPortion.STEM_BOTTOM;
import static org.audiveris.omr.sig.relation.StemPortion.STEM_MIDDLE;
import static org.audiveris.omr.sig.relation.StemPortion.STEM_TOP;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.LinkTask;
import org.audiveris.omr.sig.ui.RemovalTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UnlinkTask;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.BOTTOM;
import static org.audiveris.omr.util.VerticalSide.TOP;

import org.jgrapht.event.GraphEdgeChangeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>HeadStemRelation</code> represents the relation support between a head and a
 * stem.
 * <p>
 * A special configuration is known as the canonical "shared" configuration.
 * It gathers a head with two stems:
 * <ul>
 * <li>STEM_TOP on head LEFT side
 * <li>STEM_BOTTOM on head RIGHT side
 * </ul>
 * <p>
 *
 * <pre>
 *   |
 *   |
 * +O+
 * |
 * |
 * </pre>
 * <p>
 * A rather long stem will boost a standard-size head while a rather short stem will boost a
 * small-size head.
 * This is implemented as a "consistency" attribute between head size and stem length.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "head-stem")
public class HeadStemRelation
        extends AbstractStemConnection
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(HeadStemRelation.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Which side of head is used?. */
    @XmlAttribute(name = "head-side")
    private HorizontalSide headSide;

    /** Consistency between head size and stem length. */
    @XmlAttribute(name = "consistency")
    @XmlJavaTypeAdapter(Jaxb.Double1Adapter.class)
    private Double consistency;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>HeadStemRelation</code> object.
     */
    public HeadStemRelation ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // added //
    //-------//
    /**
     * Populate headSide and extensionPoint if needed.
     *
     * @param e edge change event
     */
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final HeadInter head = (HeadInter) e.getEdgeSource();
        final StemInter stem = (StemInter) e.getEdgeTarget();

        if (headSide == null) {
            headSide = (stem.getCenter().x < head.getCenter().x) ? LEFT : RIGHT;
        }

        if (extensionPoint == null) {
            final VerticalSide vSide = (stem.getCenter().y < head.getCenter().y) ? TOP : BOTTOM;
            final Anchor anchor = (headSide == LEFT)
                    ? (vSide == TOP ? Anchor.TOP_LEFT_STEM : Anchor.BOTTOM_LEFT_STEM)
                    : (vSide == TOP ? Anchor.TOP_RIGHT_STEM : Anchor.RIGHT_STEM);
            extensionPoint = head.getStemReferencePoint(anchor);
        }

        if (isManual() || head.isManual() || stem.isManual()) {
            // Update head chord with stem
            HeadChordInter ch = head.getChord();

            if (ch != null) {
                StemInter existingStem = ch.getStem();

                if (existingStem != stem) {
                    if (existingStem != null) {
                        SIGraph sig = stem.getSig();
                        Relation rel = sig.getRelation(ch, existingStem, ChordStemRelation.class);
                        sig.removeEdge(rel);
                    }

                    ch.setStem(stem);
                }

                //                // Propagate to beam if any
                //                Measure measure = ch.getMeasure();
                //
                //                for (AbstractBeamInter beam : stem.getBeams()) {
                //                    if (beam.getGroup() == null) {
                //                        BeamGroupInter.includeBeam(beam, measure);
                //                    }
                //                }
            }
        }

        head.checkAbnormal();
        stem.checkAbnormal();
    }

    //----------------//
    // buildStemChord //
    //----------------//
    /**
     * Create a HeadChord on-the-fly based on provided stem.
     *
     * @param tasks action sequence to populate
     * @param stem  the provided stem
     * @return a HeadChord around this stem
     */
    private HeadChordInter buildStemChord (List<UITask> tasks,
                                           StemInter stem)
    {
        final SIGraph sig = stem.getSig();
        final HeadChordInter stemChord = new HeadChordInter(null);
        tasks.add(new AdditionTask(sig, stemChord, stem.getBounds(), Collections.emptySet()));
        tasks.add(new LinkTask(sig, stemChord, stem, new ChordStemRelation()));

        return stemChord;
    }

    //----------------//
    // getConsistency //
    //----------------//
    /**
     * Report consistency between head and stem sizes.
     * <p>
     * Neutral value (1.0) is returned if information is not available.
     *
     * @return a ratio measuring consistency
     */
    public double getConsistency ()
    {
        return (consistency != null) ? consistency : 1.0;
    }

    //-------------//
    // getHeadSide //
    //-------------//
    /**
     * @return the headSide
     */
    public HorizontalSide getHeadSide ()
    {
        return headSide;
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        // Coeff is higher if stem and heads are both small or both large
        return constants.headSupportCoeff.getValue() * getConsistency();
    }

    //----------------//
    // getStemPortion //
    //----------------//
    @Override
    public StemPortion getStemPortion (Inter source,
                                       Line2D stemLine,
                                       Scale scale)
    {
        return getStemPortion((HeadInter) source, stemLine, extensionPoint.getY());
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    @Override
    protected double getTargetCoeff ()
    {
        return constants.stemSupportCoeff.getValue();
    }

    //--------------//
    // getXInGapMax //
    //--------------//
    @Override
    protected Scale.Fraction getXInGapMax (int profile)
    {
        return getXInGapMaximum(profile);
    }

    //---------------//
    // getXOutGapMax //
    //---------------//
    @Override
    protected Scale.Fraction getXOutGapMax (int profile)
    {
        return getXOutGapMaximum(profile);
    }

    //------------//
    // getYGapMax //
    //------------//
    @Override
    protected Scale.Fraction getYGapMax (int profile)
    {
        return getYGapMaximum(profile);
    }

    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" ").append(headSide);

        return sb.toString();
    }

    //------------//
    // isInvading //
    //------------//
    /**
     * Report whether this relation (assumed to be false) is invading because head and
     * stem instances are too close to co-exist separately.
     *
     * @return true if invading
     */
    public boolean isInvading ()
    {
        return (dy <= constants.maxInvadingDy.getValue())
                && (dx <= constants.maxInvadingDx.getValue());
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        return false;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        return true;
    }

    //---------//
    // preLink //
    //---------//
    @Override
    public List<? extends UITask> preLink (InterPair pair)
    {
        final HeadInter head = (HeadInter) pair.source;
        final StemInter stem = (StemInter) pair.target;

        final HeadChordInter headChord = head.getChord();

        if (headChord == null) {
            return Collections.emptyList();
        }

        final List<UITask> tasks = new ArrayList<>();
        final SIGraph sig = head.getSig();
        final List<HeadChordInter> stemChords = stem.getChords();
        HeadChordInter stemChord = (!stemChords.isEmpty()) ? stemChords.get(0) : null;

        // Check for a canonical head share, to share head
        final HorizontalSide theHeadSide = (stem.getCenter().x < head.getCenter().x) ? LEFT : RIGHT;
        final StemInter headStem = headChord.getStem();

        final boolean sharing;

        if (theHeadSide == LEFT) {
            sharing = HeadStemRelation.isCanonicalShare(stem, head, headStem);
        } else {
            sharing = HeadStemRelation.isCanonicalShare(headStem, head, stem);
        }

        if (sharing) {
            // Duplicate head and link as mirror
            HeadInter newHead = head.duplicate();
            newHead.setManual(true);
            tasks.add(
                    new AdditionTask(
                            sig,
                            newHead,
                            newHead.getBounds(),
                            Arrays.asList(new Link(head, new MirrorRelation(), false))));

            // Insert newHead to stem chord
            if (stemChord == null) {
                stemChord = buildStemChord(tasks, stem);
            }

            tasks.add(new LinkTask(sig, stemChord, newHead, new Containment()));

            pair.source = newHead; // Instead of initial head

            return tasks;
        }

        // If resulting chords are not compatible, move head to stemChord
        // @formatter:off
        if ((stemChords.isEmpty() && (headChord.getStem() != null))
                    || (!stemChords.isEmpty() && !stemChords.contains(headChord))) {
            // @formatter:on
            // Extract head from headChord
            tasks.add(new UnlinkTask(sig, sig.getRelation(headChord, head, Containment.class)));

            if (headChord.getNotes().size() <= 1) {
                // Remove headChord getting empty
                tasks.add(new RemovalTask(headChord));
            }

            if (stemChord == null) {
                stemChord = buildStemChord(tasks, stem);
            }

            // Insert head to stem chord
            tasks.add(new LinkTask(sig, stemChord, head, new Containment()));
        }

        return tasks;
    }

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final HeadInter head = (HeadInter) e.getEdgeSource();
        final StemInter stem = (StemInter) e.getEdgeTarget();

        if (stem.isVip()) {
            logger.info("VIP {} unlinked from {}", stem, head);
        }

        if (!head.isRemoved()) {
            head.checkAbnormal();
        }

        if (!stem.isRemoved()) {
            stem.checkAbnormal();
        }
    }

    //----------------//
    // setConsistency //
    //----------------//
    /**
     * Set relation consistency, based on information about head and stem sizes.
     *
     * @param headIsSmall      true for a small head
     * @param scaledStemLength stem length specified in interline fraction
     */
    public void setConsistency (boolean headIsSmall,
                                double scaledStemLength)
    {
        final double neutral = constants.neutralStemLength.getValue();
        final double ratio = scaledStemLength / neutral;

        consistency = headIsSmall ? 1.0 / ratio : ratio;

        if (logger.isDebugEnabled()) {
            logger.debug(
                    String.format(
                            "isSmall:%6s lg:%4.1f consistency:%4.1f",
                            headIsSmall,
                            scaledStemLength,
                            consistency));
        }
    }

    //----------------//
    // setConsistency //
    //----------------//
    /**
     * Set relation consistency, based on involved head and stem.
     *
     * @param head involved head
     * @param stem involved stem
     */
    public void setConsistency (HeadInter head,
                                StemInter stem)
    {
        final boolean isSmall = head.getShape().isSmallHead();
        final Scale scale = head.getSig().getSystem().getSheet().getScale();
        final Line2D line = stem.getMedian();
        final int interline = scale.getInterline();
        final double scaledStemLength = (line.getY2() - line.getY1()) / interline;
        setConsistency(isSmall, scaledStemLength);
    }

    //-------------//
    // setHeadSide //
    //-------------//
    /**
     * Set relation head horizontal side.
     *
     * @param headSide the headSide to set
     */
    public void setHeadSide (HorizontalSide headSide)
    {
        this.headSide = headSide;
    }

    //~ Static Methods -----------------------------------------------------------------------------
    //---------------//
    // checkRelation //
    //---------------//
    /**
     * Check if a Head-Stem relation is possible between provided head and stem.
     *
     * @param head       the provided head
     * @param stemLine   stem median line (top down)
     * @param stump      head stump, if any
     * @param headToTail vertical direction from head to tail
     * @param scale      scaling information
     * @param profile    desired profile level
     * @return the relation if OK, otherwise null
     */
    public static HeadStemRelation checkRelation (HeadInter head,
                                                  Line2D stemLine,
                                                  Glyph stump,
                                                  VerticalSide headToTail,
                                                  Scale scale,
                                                  int profile)
    {
        if (head.isVip()) {
            logger.info("VIP checkRelation {} & {}", head, LineUtil.toString(stemLine));
        }

        // Relation head -> stem
        final int yDir = (headToTail == TOP) ? (-1) : 1;
        final int xDir = -stemLine.relativeCCW(head.getCenter());
        final HorizontalSide hSide = (xDir < 0) ? LEFT : RIGHT;
        final Point2D refPt = head.getStemReferencePoint(hSide, headToTail);
        final HeadStemRelation hRel = new HeadStemRelation();
        hRel.setHeadSide(hSide);

        final double xStem = LineUtil.xAtY(stemLine, refPt.getY());
        final double xGap = xDir * (xStem - refPt.getX());

        final double yGap;

        if (stump != null) {
            final Rectangle stumpBox = stump.getBounds();
            final double overlap = (yDir > 0) ? stumpBox.y + stumpBox.height - stemLine.getY1()
                    : stemLine.getY2() - stumpBox.y;
            yGap = Math.abs(Math.min(overlap, 0));
        } else {
            if (refPt.getY() < stemLine.getY1()) {
                yGap = stemLine.getY1() - refPt.getY();
            } else if (refPt.getY() > stemLine.getY2()) {
                yGap = refPt.getY() - stemLine.getY2();
            } else {
                yGap = 0;
            }
        }

        hRel.setInOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), profile);

        if (hRel.getGrade() >= hRel.getMinGrade()) {
            // Beware: extension must be the maximum y extension in head y range
            final Rectangle headBox = head.getBounds();
            hRel.setExtensionPoint(
                    new Point2D.Double(
                            xStem,
                            (yDir > 0) ? headBox.y : ((headBox.y + headBox.height) - 1)));

            return hRel;
        }

        return null;
    }

    //----------------//
    // getStemPortion //
    //----------------//
    /**
     * Helper method to retrieve StemPortion of the connection.
     *
     * @param head       the item connected to the stem (head)
     * @param stemLine   logical range of the stem
     * @param yExtension ordinate of head-stem extension point
     * @return the stem Portion
     */
    public static StemPortion getStemPortion (HeadInter head,
                                              Line2D stemLine,
                                              double yExtension)
    {
        final double margin = head.getBounds().height * constants.anchorHeightRatio.getValue();
        final double yMidStem = (stemLine.getY1() + stemLine.getY2()) / 2;

        if (yExtension >= yMidStem) {
            return (yExtension > (stemLine.getY2() - margin)) ? STEM_BOTTOM : STEM_MIDDLE;
        } else {
            return (yExtension < (stemLine.getY1() + margin)) ? STEM_TOP : STEM_MIDDLE;
        }
    }

    //------------------//
    // getXInGapMaximum //
    //------------------//
    public static Scale.Fraction getXInGapMaximum (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.xInGapMax, profile);
    }

    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.xOutGapMax, profile);
    }

    //----------------//
    // getYGapMaximum //
    //----------------//
    public static Scale.Fraction getYGapMaximum (int profile)
    {
        return (Scale.Fraction) constants.getConstant(constants.yGapMax, profile);
    }

    //------------------//
    // isCanonicalShare //
    //------------------//
    /**
     * Check whether this is the canonical "shared" configuration.
     * It uses relations to stems.
     *
     * @param leftRel  head-stem relation on left
     * @param head     head in the middle
     * @param rightRel head-stem relation on right
     * @return true if canonical
     */
    public static boolean isCanonicalShare (HeadStemRelation leftRel,
                                            HeadInter head,
                                            HeadStemRelation rightRel)
    {
        return isCanonicalShare(null, leftRel, head, rightRel, null);
    }

    //------------------//
    // isCanonicalShare //
    //------------------//
    /**
     * Check whether this is the canonical "shared" configuration.
     * It uses stems.
     *
     * @param leftStem  stem on left
     * @param head      head in the middle
     * @param rightStem stem on right
     * @return true if canonical
     */
    public static boolean isCanonicalShare (StemInter leftStem,
                                            HeadInter head,
                                            StemInter rightStem)
    {
        return isCanonicalShare(leftStem, null, head, null, rightStem);
    }

    //------------------//
    // isCanonicalShare //
    //------------------//
    /**
     * Check whether this is the canonical "shared" configuration.
     * It uses stems and/or relations to stems.
     *
     * @param leftStem  stem on left (can be null if leftRel is not)
     * @param leftRel   head-stem relation on left (can be null if leftStem is not)
     * @param head      head in the middle
     * @param rightRel  head-stem relation on right (can be null if rightStem is not)
     * @param rightStem stem on right (can be null if rightRel is not)
     * @return true if canonical
     */
    public static boolean isCanonicalShare (StemInter leftStem,
                                            HeadStemRelation leftRel,
                                            HeadInter head,
                                            HeadStemRelation rightRel,
                                            StemInter rightStem)
    {
        final SIGraph sig = head.getSig();

        if (leftStem == null) {
            if (leftRel == null) {
                return false;
            }

            leftStem = (StemInter) sig.getOppositeInter(head, leftRel);
        }

        if (rightStem == null) {
            if (rightRel == null) {
                return false;
            }

            rightStem = (StemInter) sig.getOppositeInter(head, rightRel);
        }

        // Prefer use of relation extension points over stem physical limits
        Line2D leftLine = leftStem.computeExtendedLine(head);
        Line2D rightLine = rightStem.computeExtendedLine(head);

        Rectangle headBox = head.getBounds();
        Point headCenter = head.getCenter();
        double yMidLeft = (leftLine.getY1() + leftLine.getY2()) / 2;
        double yMidRight = (rightLine.getY1() + rightLine.getY2()) / 2;

        if ((headCenter.y >= yMidLeft) || (headCenter.y <= yMidRight)) {
            return false;
        }

        double yLeftExt = (leftRel != null) ? leftRel.getExtensionPoint().getY() : headBox.y;
        double yRightExt = (rightRel != null) ? rightRel.getExtensionPoint().getY()
                : ((headBox.y + headBox.height) - 1);

        StemPortion leftPortion = getStemPortion(head, leftLine, yLeftExt);
        StemPortion rightPortion = getStemPortion(head, rightLine, yRightExt);

        return (leftPortion == STEM_TOP) && (rightPortion == STEM_BOTTOM);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Ratio headSupportCoeff =
                new Constant.Ratio(4, "Value for (source) head coeff in support formula");

        private final Constant.Ratio stemSupportCoeff =
                new Constant.Ratio(10, "Value for (target) stem coeff in support formula");

        private final Scale.Fraction xInGapMax =
                new Scale.Fraction(0.2, "Maximum horizontal overlap between stem & head");

        @SuppressWarnings("unused")
        private final Scale.Fraction xInGapMax_p1 = new Scale.Fraction(0.4, "Idem for profile 1");

        private final Scale.Fraction xOutGapMax =
                new Scale.Fraction(0.15, "Maximum horizontal gap between stem & head");

        @SuppressWarnings("unused")
        private final Scale.Fraction xOutGapMax_p1 = new Scale.Fraction(0.25, "Idem for profile 1");

        @SuppressWarnings("unused")
        private final Scale.Fraction xOutGapMax_p2 = new Scale.Fraction(0.35, "Idem for profile 2");

        private final Scale.Fraction yGapMax =
                new Scale.Fraction(0.8, "Maximum vertical gap between stem & head");

        @SuppressWarnings("unused")
        private final Scale.Fraction yGapMax_p1 = new Scale.Fraction(1.2, "Idem for profile 1");

        private final Constant.Ratio anchorHeightRatio = new Constant.Ratio(
                0.275,
                "Vertical margin for stem anchor portion (as ratio of head height)");

        private final Scale.Fraction maxInvadingDx =
                new Scale.Fraction(0.05, "Maximum invading horizontal gap between stem & head");

        private final Scale.Fraction maxInvadingDy =
                new Scale.Fraction(0.0, "Maximum invading vertical gap between stem & head");

        private final Scale.Fraction neutralStemLength =
                new Scale.Fraction(2.8, "Neutral stem length between small and standard");
    }
}
