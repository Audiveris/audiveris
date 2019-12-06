//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  H e a d S t e m R e l a t i o n                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import org.audiveris.omr.image.Anchored.Anchor;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.beam.BeamGroup;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;
import static org.audiveris.omr.sig.relation.StemPortion.*;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.LinkTask;
import org.audiveris.omr.sig.ui.RemovalTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UnlinkTask;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;

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

/**
 * Class {@code HeadStemRelation} represents the relation support between a head and a
 * stem.
 * <p>
 * A special configuration is known as the canonical "shared" configuration.
 * It gathers a head with two stems:
 * <ul>
 * <li>STEM_TOP on head LEFT side
 * <li>STEM_BOTTOM on head RIGHT side
 * </ul>
 *
 * <pre>
 *    |
 *    |
 *  +O+
 *  |
 *  |
 * </pre>
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "head-stem")
public class HeadStemRelation
        extends AbstractStemConnection
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(HeadStemRelation.class);

    /** Which side of head is used?. */
    @XmlAttribute(name = "head-side")
    private HorizontalSide headSide;

    /**
     * Creates a new {@code HeadStemRelation} object.
     */
    public HeadStemRelation ()
    {
    }

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
            Anchor anchor = (headSide == LEFT) ? Anchor.LEFT_STEM : Anchor.RIGHT_STEM;
            int interline = head.getStaff().getSpecificInterline();
            Point2D refPt = head.getStemReferencePoint(anchor, interline);
            extensionPoint = refPt;
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

                // Propagate to beam if any
                Measure measure = ch.getMeasure();

                for (AbstractBeamInter beam : stem.getBeams()) {
                    if (beam.getGroup() == null) {
                        BeamGroup.includeBeam(beam, measure);
                    }
                }
            }
        }

        head.checkAbnormal();
        stem.checkAbnormal();
    }

    @Override
    public Object clone ()
            throws CloneNotSupportedException
    {
        return super.clone(); //To change body of generated methods, choose Tools | Templates.
    }

    //------------------//
    // getXInGapMaximum //
    //------------------//
    public static Scale.Fraction getXInGapMaximum (boolean manual)
    {
        return manual ? constants.xInGapMaxManual : constants.xInGapMax;
    }

    //-------------------//
    // getXOutGapMaximum //
    //-------------------//
    public static Scale.Fraction getXOutGapMaximum (boolean manual)
    {
        return manual ? constants.xOutGapMaxManual : constants.xOutGapMax;
    }

    //----------------//
    // getYGapMaximum //
    //----------------//
    public static Scale.Fraction getYGapMaximum (boolean manual)
    {
        return manual ? constants.yGapMaxManual : constants.yGapMax;
    }

    /**
     * @return the headSide
     */
    public HorizontalSide getHeadSide ()
    {
        return headSide;
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
        return (dy <= constants.maxInvadingDy.getValue()) && (dx <= constants.maxInvadingDx
                .getValue());
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
    public List<? extends UITask> preLink (RelationPair pair)
    {
        final HeadInter head = (HeadInter) pair.source;
        final StemInter stem = (StemInter) pair.target;

        final HeadChordInter headChord = head.getChord();
        if (headChord == null) {
            return Collections.EMPTY_LIST;
        }

        final List<UITask> tasks = new ArrayList<>();
        final SIGraph sig = head.getSig();
        final List<HeadChordInter> stemChords = stem.getChords();
        HeadChordInter stemChord = (!stemChords.isEmpty()) ? stemChords.get(0) : null;

        // Check for a canonical head share, to share head
        final HorizontalSide headSide = (stem.getCenter().x < head.getCenter().x) ? LEFT : RIGHT;
        final StemInter headStem = headChord.getStem();

        final boolean sharing;
        if (headSide == LEFT) {
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
        if ((stemChords.isEmpty() && (headChord.getStem() != null))
                    || (!stemChords.isEmpty() && !stemChords.contains(headChord))) {
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

        if (!head.isRemoved()) {
            head.checkAbnormal();
        }

        if (!stem.isRemoved()) {
            stem.checkAbnormal();
        }
    }

    //-------------//
    // setHeadSide //
    //-------------//
    /**
     * @param headSide the headSide to set
     */
    public void setHeadSide (HorizontalSide headSide)
    {
        this.headSide = headSide;
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    @Override
    protected double getSourceCoeff ()
    {
        return constants.headSupportCoeff.getValue();
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
    protected Scale.Fraction getXInGapMax (boolean manual)
    {
        return getXInGapMaximum(manual);
    }

    //---------------//
    // getXOutGapMax //
    //---------------//
    @Override
    protected Scale.Fraction getXOutGapMax (boolean manual)
    {
        return getXOutGapMaximum(manual);
    }

    //------------//
    // getYGapMax //
    //------------//
    @Override
    protected Scale.Fraction getYGapMax (boolean manual)
    {
        return getYGapMaximum(manual);
    }

    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" ").append(headSide);

        return sb.toString();
    }

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
        Line2D leftLine = leftStem.computeExtendedLine();
        Line2D rightLine = rightStem.computeExtendedLine();

        Rectangle headBox = head.getBounds();
        Point headCenter = head.getCenter();
        double yMidLeft = (leftLine.getY1() + leftLine.getY2()) / 2;
        double yMidRight = (rightLine.getY1() + rightLine.getY2()) / 2;
        if (headCenter.y >= yMidLeft || headCenter.y <= yMidRight) {
            return false;
        }

        double yLeftExt = leftRel != null ? leftRel.getExtensionPoint().getY() : headBox.y;
        double yRightExt = rightRel != null ? rightRel.getExtensionPoint().getY()
                : headBox.y + headBox.height - 1;

        StemPortion leftPortion = getStemPortion(head, leftLine, yLeftExt);
        StemPortion rightPortion = getStemPortion(head, rightLine, yRightExt);

        return leftPortion == STEM_TOP && rightPortion == STEM_BOTTOM;
    }

    //----------------//
    // buildStemChord //
    //----------------//
    /**
     * Create a HeadChord on the fly based on provided stem.
     *
     * @param seq  action sequence to populate
     * @param stem the provided stem
     * @return a HeadChord around this stem
     */
    private HeadChordInter buildStemChord (List<UITask> tasks,
                                           StemInter stem)
    {
        final SIGraph sig = stem.getSig();
        final HeadChordInter stemChord = new HeadChordInter(-1);
        tasks.add(new AdditionTask(sig, stemChord, stem.getBounds(), Collections.EMPTY_SET));
        tasks.add(new LinkTask(sig, stemChord, stem, new ChordStemRelation()));

        return stemChord;
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio headSupportCoeff = new Constant.Ratio(
                1,
                "Value for (source) head coeff in support formula");

        private final Constant.Ratio stemSupportCoeff = new Constant.Ratio(
                1,
                "Value for (target) stem coeff in support formula");

        private final Scale.Fraction xInGapMax = new Scale.Fraction(
                0.3,
                "Maximum horizontal overlap between stem & head");

        private final Scale.Fraction xInGapMaxManual = new Scale.Fraction(
                0.45,
                "Maximum manual horizontal overlap between stem & head");

        private final Scale.Fraction xOutGapMax = new Scale.Fraction(
                0.275,
                "Maximum horizontal gap between stem & head");

        private final Scale.Fraction xOutGapMaxManual = new Scale.Fraction(
                0.35,
                "Maximum manual horizontal gap between stem & head");

        private final Scale.Fraction yGapMax = new Scale.Fraction(
                0.8,
                "Maximum vertical gap between stem & head");

        private final Scale.Fraction yGapMaxManual = new Scale.Fraction(
                1.2,
                "Maximum manual vertical gap between stem & head");

        private final Constant.Ratio anchorHeightRatio = new Constant.Ratio(
                0.25,
                "Vertical margin for stem anchor portion (as ratio of head height)");

        private final Scale.Fraction maxInvadingDx = new Scale.Fraction(
                0.05,
                "Maximum invading horizontal gap between stem & head");

        private final Scale.Fraction maxInvadingDy = new Scale.Fraction(
                0.0,
                "Maximum invading vertical gap between stem & head");
    }
}
