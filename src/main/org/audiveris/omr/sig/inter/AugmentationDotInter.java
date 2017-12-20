//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             A u g m e n t a t i o n D o t I n t e r                            //
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
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
        MeasureStack stack = sig.getSystem().getMeasureStackAt(getCenter());

        if (stack != null) {
            stack.addInter(this);
        }
    }

    //------------------//
    // getCandidateDots //
    //------------------//
    /**
     * Report which first augmentation dots could be concerned by a second augmentation
     * dot located at provided dot center.
     *
     * @param dotCenter    provided dot center
     * @param systemFirsts all (first) augmentation dots, sorted by abscissa
     * @param system       containing system
     * @return the list of candidate dots in the augmentation dot neighborhood.
     */
    public static List<Inter> getCandidateDots (Point dotCenter,
                                                List<Inter> systemFirsts,
                                                SystemInfo system)
    {
        final Scale scale = system.getSheet().getScale();

        // Look for augmentation dots reachable from this dot
        final MeasureStack dotStack = system.getMeasureStackAt(dotCenter);
        final int maxDx = scale.toPixels(DoubleDotRelation.getXOutGapMaximum());
        final int maxDy = scale.toPixels(DoubleDotRelation.getYGapMaximum());
        final Rectangle luBox = new Rectangle(dotCenter);
        luBox.grow(0, maxDy);
        luBox.x -= maxDx;
        luBox.width += maxDx;

        // Relevant dots?
        final List<Inter> dots = dotStack.filter(
                SIGraph.intersectedInters(systemFirsts, GeoOrder.BY_ABSCISSA, luBox));

        return dots;
    }

    //-------------------//
    // getCandidateNotes //
    //-------------------//
    /**
     * Report which notes (heads and rests) could be concerned by an augmentation dot
     * located at provided dot center.
     *
     * @param dotCenter   provided dot center
     * @param systemNotes system notes, sorted by abscissa
     * @param system      containing system
     * @return the list of candidate notes in the augmentation dot neighborhood.
     */
    public static List<Inter> getCandidateNotes (Point dotCenter,
                                                 List<Inter> systemNotes,
                                                 SystemInfo system)
    {
        final Scale scale = system.getSheet().getScale();

        // Look for entities (heads and rests) reachable from this dot
        final MeasureStack dotStack = system.getMeasureStackAt(dotCenter);
        final int maxDx = scale.toPixels(AugmentationRelation.getXOutGapMaximum());
        final int maxDy = scale.toPixels(AugmentationRelation.getYGapMaximum());
        final Rectangle luBox = new Rectangle(dotCenter);
        luBox.grow(0, maxDy);
        luBox.x -= maxDx;
        luBox.width += maxDx;

        // Relevant heads/rests?
        final List<Inter> notes = dotStack.filter(
                SIGraph.intersectedInters(systemNotes, GeoOrder.BY_ABSCISSA, luBox));

        // Beware of mirrored heads: link only to the head with longer duration
        filterMirrorHeads(notes);

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
            return (AugmentationDotInter) sig.getOppositeInter(this, dd);
        }

        return null;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(
                this,
                AugmentationRelation.class,
                DoubleDotRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
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
        MeasureStack stack = sig.getSystem().getMeasureStackAt(getCenter());

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
        List<Inter> systemNotes = system.getSig().inters(AbstractNoteInter.class);
        Collections.sort(systemNotes, Inters.byAbscissa);

        List<Inter> systemDots = system.getSig().inters(AugmentationDotInter.class);
        Collections.sort(systemDots, Inters.byAbscissa);

        Link link = lookupLink(systemNotes, systemDots, system);

        if (link == null) {
            return Collections.emptyList();
        }

        if (doit) {
            link.applyTo(this);
        }

        return Collections.singleton(link);
    }

    //-------------------//
    // filterMirrorHeads //
    //-------------------//
    /**
     * If the collection of (dot-related) heads contains mirrored heads, keep only the
     * head with longer duration
     *
     * @param heads the heads looked up near a candidate augmentation dot
     */
    private static void filterMirrorHeads (List<Inter> heads)
    {
        if (heads.size() < 2) {
            return;
        }

        Collections.sort(heads, Inters.byId);

        boolean modified;

        do {
            modified = false;

            InterLoop:
            for (Inter inter : heads) {
                HeadInter head = (HeadInter) inter;
                Inter mirrorInter = head.getMirror();

                if ((mirrorInter != null) && heads.contains(mirrorInter)) {
                    HeadInter mirrorHead = (HeadInter) mirrorInter;
                    Rational hDur = head.getChord().getDurationSansDotOrTuplet();
                    Rational mDur = mirrorHead.getChord().getDurationSansDotOrTuplet();

                    switch (mDur.compareTo(hDur)) {
                    case -1:
                        heads.remove(mirrorHead);
                        modified = true;

                        break InterLoop;

                    case +1:
                        heads.remove(head);
                        modified = true;

                        break InterLoop;

                    case 0:
                        // Same duration (but we don't have flags yet!) TODO: review this
                        // Keep the one with lower ID
                        heads.remove(mirrorHead);
                        modified = true;

                        break InterLoop;
                    }
                }
            }
        } while (modified);
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this augmentation dot and either a note
     * (head or rest) or another dot on left side.
     *
     * @param systemNotes ordered collection of notes (head/rest) in system
     * @param systemDots  ordered collection of augmentation dots in system
     * @param system      containing system
     * @return the link found or null
     */
    private Link lookupLink (List<Inter> systemNotes,
                             List<Inter> systemDots,
                             SystemInfo system)
    {
        final Scale scale = system.getSheet().getScale();
        final Point dotCenter = getCenter();

        // Look up for notes
        final List<Inter> notes = getCandidateNotes(dotCenter, systemNotes, system);

        final AugmentationRelation aRel = new AugmentationRelation();
        Inter bestNote = null;
        double bestNoteGrade = -1;

        for (Inter note : notes) {
            // Select proper note reference point (center right)
            Point refPt = note.getCenterRight();
            double xGap = dotCenter.x - refPt.x;

            if (xGap > 0) {
                double yGap = Math.abs(refPt.y - dotCenter.y);
                aRel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

                if (bestNoteGrade < aRel.getGrade()) {
                    bestNoteGrade = aRel.getGrade();
                    bestNote = note;
                }
            }
        }

        // Look up for dots
        final List<Inter> firsts = AugmentationDotInter.getCandidateDots(
                dotCenter,
                systemDots,
                system);

        // Remove the augmentation dot, if any, that corresponds to the glyph at hand
        for (Inter first : firsts) {
            if (first.getCenter().equals(dotCenter)) {
                firsts.remove(first);

                break;
            }
        }

        DoubleDotRelation bestDotRel = null;
        Inter bestDot = null;
        double bestYGap = Double.MAX_VALUE;

        for (Inter first : firsts) {
            // Select proper entity reference point (center right)
            Point refPt = first.getCenterRight();
            double xGap = dotCenter.x - refPt.x;

            if (xGap > 0) {
                double yGap = Math.abs(refPt.y - dotCenter.y);
                DoubleDotRelation rel = new DoubleDotRelation();
                rel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

                if (rel.getGrade() >= rel.getMinGrade()) {
                    if ((bestDotRel == null) || (bestYGap > yGap)) {
                        bestDotRel = rel;
                        bestDot = first;
                        bestYGap = yGap;
                    }
                }
            }
        }

        if ((bestNote == null) && (bestDot == null)) {
            return null;
        }

        if (bestDot == null) {
            return new Link(bestNote, new AugmentationRelation(), true);
        }

        if (bestNote == null) {
            return new Link(bestDot, new DoubleDotRelation(), true);
        }

        if (bestNoteGrade >= bestDotRel.getGrade()) {
            return new Link(bestNote, new AugmentationRelation(), true);
        } else {
            return new Link(bestDot, new DoubleDotRelation(), true);
        }
    }
}
