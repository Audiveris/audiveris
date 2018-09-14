//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A u g m e n t a t i o n D o t I n t e r                            //
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code AugmentationDotInter} represent an augmentation dot for a note or a rest.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "augmentation-dot")
public class AugmentationDotInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AugmentationDotInter.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AugmentationDotInter} object.
     *
     * @param glyph underlying glyph
     * @param grade evaluation value
     */
    public AugmentationDotInter (Glyph glyph,
                                 double grade)
    {
        super(glyph, null, Shape.AUGMENTATION_DOT, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private AugmentationDotInter ()
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

    //-------//
    // added //
    //-------//
    /**
     * Add the dot to its containing stack.
     *
     * @see #remove(boolean)
     */
    @Override
    public void added ()
    {
        super.added();

        // Add it to containing measure stack
        MeasureStack stack = sig.getSystem().getStackAt(getCenter());

        if (stack != null) {
            stack.addInter(this);
        }

        setAbnormal(true); // No note or other dot linked yet
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if dot is connected to a note or to another (first) dot
        boolean ab = true;

        if (sig.hasRelation(this, AugmentationRelation.class)) {
            ab = false;
        } else {
            for (Relation dd : sig.getRelations(this, DoubleDotRelation.class)) {
                if (sig.getEdgeSource(dd) == this) {
                    ab = false;
                }
            }
        }

        setAbnormal(ab);

        return isAbnormal();
    }

    //-------------------//
    // getAugmentedNotes //
    //-------------------//
    /**
     * Report the notes (head/rest) that are currently linked to this augmentation dot.
     *
     * @return the linked notes
     */
    public List<AbstractNoteInter> getAugmentedNotes ()
    {
        List<AbstractNoteInter> notes = null;

        for (Relation rel : sig.getRelations(this, AugmentationRelation.class)) {
            if (notes == null) {
                notes = new ArrayList<AbstractNoteInter>();
            }

            notes.add((AbstractNoteInter) sig.getEdgeTarget(rel));
        }

        if (notes == null) {
            return Collections.EMPTY_LIST;
        }

        return notes;
    }

    //---------//
    // getPart //
    //---------//
    @Override
    public Part getPart ()
    {
        if (part == null) {
            // Beware, we may have two dots that refer to one another
            // First dot
            for (Relation rel : sig.getRelations(this, AugmentationRelation.class)) {
                Inter opposite = sig.getOppositeInter(this, rel);

                return part = opposite.getPart();
            }

            final int dotCenterX = getCenter().x;

            // Perhaps a second dot, let's look for a first dot
            for (Relation rel : sig.getRelations(this, DoubleDotRelation.class)) {
                Inter opposite = sig.getOppositeInter(this, rel);

                if (opposite.getCenter().x < dotCenterX) {
                    return part = opposite.getPart();
                }
            }
        }

        return super.getPart();
    }

    //--------------------------//
    // getSecondAugmentationDot //
    //--------------------------//
    /**
     * Report the second augmentation dot, if any, that is linked to this (first)
     * augmentation dot.
     *
     * @return the second dot, if any, or null
     */
    public AugmentationDotInter getSecondAugmentationDot ()
    {
        for (Relation dd : sig.getRelations(this, DoubleDotRelation.class)) {
            Inter dot = sig.getEdgeSource(dd);

            if (dot != this) {
                return (AugmentationDotInter) dot;
            }
        }

        return null;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        // Use augmented note, if any
        for (Relation rel : sig.getRelations(this, AugmentationRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        // If second dot, use first dot
        for (Relation rel : sig.getRelations(this, DoubleDotRelation.class)) {
            Inter firstDot = sig.getEdgeTarget(rel);

            if (firstDot != this) {
                return firstDot.getVoice();
            }
        }

        return null;
    }

    //----------------//
    // lookupDotLinks //
    //----------------//
    /**
     * Look up for all possible links with (first) dots.
     *
     * @param systemDots collection of augmentation dots in system, ordered bottom up
     * @param system     containing system
     * @return list of possible links, perhaps empty
     */
    public List<Link> lookupDotLinks (List<Inter> systemDots,
                                      SystemInfo system)
    {
        // Need getCenter()
        final List<Link> links = new ArrayList<Link>();
        final Scale scale = system.getSheet().getScale();
        final Point dotCenter = getCenter();
        final MeasureStack dotStack = system.getStackAt(dotCenter);

        if (dotStack == null) {
            return links;
        }

        // Look for augmentation dots reachable from this dot
        final Rectangle luBox = getDotsLuBox(dotCenter, system);

        // Relevant dots?
        final List<Inter> firsts = dotStack.filter(
                Inters.intersectedInters(systemDots, GeoOrder.NONE, luBox));

        // Remove the augmentation dot, if any, that corresponds to the glyph at hand
        for (Inter first : firsts) {
            if (first.getCenter().equals(dotCenter)) {
                firsts.remove(first);

                break;
            }
        }

        final int minDx = scale.toPixels(DoubleDotRelation.getXOutGapMinimum(manual));

        for (Inter first : firsts) {
            Point refPt = first.getCenterRight();
            double xGap = dotCenter.x - refPt.x;

            if (xGap >= minDx) {
                double yGap = Math.abs(refPt.y - dotCenter.y);
                DoubleDotRelation rel = new DoubleDotRelation();
                rel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), manual);

                if (rel.getGrade() >= rel.getMinGrade()) {
                    links.add(new Link(first, rel, true));
                }
            }
        }

        return links;
    }

    //----------------//
    // lookupHeadLink //
    //----------------//
    /**
     * Look up for a possible link with a head.
     * <p>
     * Assumption: System dots are already in place or they are processed bottom up.
     *
     * @param systemHeadChords system head chords, sorted by abscissa
     * @param system           containing system
     * @return a link or null
     */
    public Link lookupHeadLink (List<Inter> systemHeadChords,
                                SystemInfo system)
    {
        // Need sig and getCenter()
        final List<Link> links = new ArrayList<Link>();
        final Scale scale = system.getSheet().getScale();
        final Point dotCenter = getCenter();
        final MeasureStack dotStack = system.getStackAt(dotCenter);

        if (dotStack == null) {
            return null;
        }

        // Look for heads reachable from this dot. Heads are processed via their chord.
        final Rectangle luBox = getNotesLuBox(dotCenter, system);

        final List<Inter> chords = dotStack.filter(
                Inters.intersectedInters(systemHeadChords, GeoOrder.BY_ABSCISSA, luBox));
        final int minDx = scale.toPixels(AugmentationRelation.getXOutGapMinimum(manual));

        for (Inter ic : chords) {
            HeadChordInter chord = (HeadChordInter) ic;

            // Heads are reported bottom up within their chord
            // So, we need to sort the list top down
            List<? extends Inter> chordHeads = chord.getNotes();
            Collections.sort(chordHeads, Inters.byCenterOrdinate);

            for (Inter ih : chordHeads) {
                HeadInter head = (HeadInter) ih;

                // Check head is within reach and not yet augmented
                if (GeoUtil.yEmbraces(luBox, head.getCenter().y)
                    && (head.getFirstAugmentationDot() == null)) {
                    Point refPt = head.getCenterRight();
                    double xGap = dotCenter.x - refPt.x;

                    // Make sure dot is not too close to head
                    if (xGap < minDx) {
                        continue;
                    }

                    // When this method is called, there is at most one stem per head
                    for (Relation rel : system.getSig().getRelations(head, HeadStemRelation.class)) {
                        HeadStemRelation hsRel = (HeadStemRelation) rel;

                        if (hsRel.getHeadSide() == RIGHT) {
                            // If containing chord has heads on right side, reduce xGap accordingly
                            Rectangle rightBox = chord.getHeadsBounds(RIGHT);

                            if (rightBox != null) {
                                if (xGap > 0) {
                                    xGap = Math.max(1, xGap - rightBox.width);
                                }

                                break;
                            }
                        }
                    }

                    if (xGap > 0) {
                        double yGap = Math.abs(refPt.y - dotCenter.y);
                        AugmentationRelation rel = new AugmentationRelation();
                        rel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), manual);

                        if (rel.getGrade() >= rel.getMinGrade()) {
                            links.add(new Link(head, rel, true));
                        }
                    }
                }
            }
        }

        // Now choose best among links
        // First priority is given to head between lines (thus facing the dot)
        // Second priority is given to head on lower line
        // Third priority is given to head on upper line
        for (Link link : links) {
            HeadInter head = (HeadInter) link.partner;

            if ((head.getIntegerPitch() % 2) == 1) {
                return link;
            }
        }

        return (!links.isEmpty()) ? links.get(0) : null;
    }

    //-----------------//
    // lookupRestLinks //
    //-----------------//
    /**
     * Look up for all possible links with rests
     *
     * @param systemRests system rests, sorted by abscissa
     * @param system      containing system
     * @return list of possible links, perhaps empty
     */
    public List<Link> lookupRestLinks (List<Inter> systemRests,
                                       SystemInfo system)
    {
        // Need getCenter()
        final List<Link> links = new ArrayList<Link>();
        final Scale scale = system.getSheet().getScale();
        final Point dotCenter = getCenter();
        final MeasureStack dotStack = system.getStackAt(dotCenter);

        if (dotStack == null) {
            return links;
        }

        // Look for rests reachable from this dot
        final Rectangle luBox = getNotesLuBox(dotCenter, system);

        // Relevant rests?
        final List<Inter> rests = dotStack.filter(
                Inters.intersectedInters(systemRests, GeoOrder.BY_ABSCISSA, luBox));
        final int minDx = scale.toPixels(AugmentationRelation.getXOutGapMinimum(manual));

        for (Inter inter : rests) {
            RestInter rest = (RestInter) inter;
            Point refPt = rest.getCenterRight();
            double xGap = dotCenter.x - refPt.x;

            if (xGap >= minDx) {
                double yGap = Math.abs(refPt.y - dotCenter.y);
                AugmentationRelation rel = new AugmentationRelation();
                rel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), manual);

                if (rel.getGrade() >= rel.getMinGrade()) {
                    links.add(new Link(rest, rel, true));
                }
            }
        }

        return links;
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove the dot from its containing stack.
     *
     * @param extensive true for non-manual removals only
     * @see #added()
     */
    @Override
    public void remove (boolean extensive)
    {
        MeasureStack stack = sig.getSystem().getStackAt(getCenter());

        if (stack != null) {
            stack.removeInter(this);
        }

        super.remove(extensive);
    }

    //-------------//
    // searchLinks //
    //-------------//
    /**
     * Try to find a link with a note or another dot on the left.
     *
     * @param system containing system
     * @param doit   true to apply
     * @return the link found or null
     */
    @Override
    public Collection<Link> searchLinks (SystemInfo system,
                                         boolean doit)
    {
        // Not very optimized!
        List<Inter> systemRests = system.getSig().inters(RestInter.class);
        Collections.sort(systemRests, Inters.byAbscissa);

        List<Inter> systemHeadChords = system.getSig().inters(HeadChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        List<Inter> systemDots = system.getSig().inters(AugmentationDotInter.class);
        Collections.sort(systemDots, Inters.byAbscissa);

        Link link = lookupLink(systemRests, systemHeadChords, systemDots, system);

        if (link == null) {
            return Collections.emptyList();
        }

        if (doit) {
            link.applyTo(this);
        }

        return Collections.singleton(link);
    }

    //--------------//
    // getDotsLuBox //
    //--------------//
    /**
     * Report dots lookup box based on provided dot center
     *
     * @param dotCenter center of dot candidate
     * @param system    containing system
     * @return proper lookup box
     */
    private Rectangle getDotsLuBox (Point dotCenter,
                                    SystemInfo system)
    {
        final Scale scale = system.getSheet().getScale();
        final int maxDx = scale.toPixels(DoubleDotRelation.getXOutGapMaximum(manual));
        final int maxDy = scale.toPixels(DoubleDotRelation.getYGapMaximum(manual));

        return getLuBox(dotCenter, maxDx, maxDy);
    }

    //----------//
    // getLuBox //
    //----------//
    /**
     * Report proper lookup box based on provided dot center
     *
     * @param dotCenter center of dot candidate
     * @param maxDx     maximum dx between entity left side and dot center
     * @param maxDy     maximum dy between entity center and dot center
     * @return proper lookup box
     */
    private static Rectangle getLuBox (Point dotCenter,
                                       int maxDx,
                                       int maxDy)
    {
        return new Rectangle(dotCenter.x - maxDx, dotCenter.y - maxDy, maxDx, 2 * maxDy);
    }

    //---------------//
    // getNotesLuBox //
    //---------------//
    /**
     * Report notes lookup box based on provided dot center
     *
     * @param dotCenter center of dot candidate
     * @param system    containing system
     * @return proper lookup box
     */
    private Rectangle getNotesLuBox (Point dotCenter,
                                     SystemInfo system)
    {
        final Scale scale = system.getSheet().getScale();
        final int maxDx = scale.toPixels(AugmentationRelation.getXOutGapMaximum(manual));
        final int maxDy = scale.toPixels(AugmentationRelation.getYGapMaximum(manual));

        return getLuBox(dotCenter, maxDx, maxDy);
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this augmentation dot and either a note
     * (head or rest) or another dot on left side.
     *
     * @param systemRests      ordered collection of rests in system
     * @param systemHeadChords ordered collection of head chords in system
     * @param systemDots       ordered collection of augmentation dots in system
     * @param system           containing system
     * @return the best link found or null
     */
    private Link lookupLink (List<Inter> systemRests,
                             List<Inter> systemHeadChords,
                             List<Inter> systemDots,
                             SystemInfo system)
    {
        List<Link> links = new ArrayList<Link>();
        Link headLink = lookupHeadLink(systemHeadChords, system);

        if (headLink != null) {
            links.add(headLink);
        }

        links.addAll(lookupRestLinks(systemRests, system));
        links.addAll(lookupDotLinks(systemDots, system));

        return Link.bestOf(links);
    }
}
