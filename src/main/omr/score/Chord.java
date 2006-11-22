//----------------------------------------------------------------------------//
//                                                                            //
//                                 C h o r d                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;

import omr.score.visitor.Visitor;

import omr.sheet.PixelPoint;
import omr.sheet.PixelRectangle;

import omr.util.Logger;
import omr.util.TreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>Chord</code> represents an ensemble of entities (rests, notes)
 * that occur on the same time in a staff.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Chord
    extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Chord.class);

    //~ Instance fields --------------------------------------------------------

    /** A chord stem is virtual when there is no real stem (breve, rest...) */
    private Glyph stem;

    /** Ratio to get real duration wrt notation */
    private Double tupletRatio;

    /** Index of this chord in tuplet */
    private Integer tupletIndex;

    /** Number of augmentation dots */
    private int dotsNumber;

    /** Number of flags (a beam is not a flag) */
    private int flagsNumber;

    /** Location for chord head (head farthest from chord tail) */
    private SystemPoint headLocation;

    /** Location for chord tail */
    private SystemPoint tailLocation;

    /** List of beams */
    private List<Beam> beams = new ArrayList<Beam>();

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Chord //
    //-------//
    /**
     * Creates a new instance of Chord
     * @param measure the containing measure
     */
    public Chord (Measure measure)
    {
        super(measure);
        reset();
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getBeams //
    //----------//
    public List<Beam> getBeams ()
    {
        return beams;
    }

    //---------------//
    // getDotsNumber //
    //---------------//
    public int getDotsNumber ()
    {
        return dotsNumber;
    }

    //----------------//
    // getFlagsNumber //
    //----------------//
    public int getFlagsNumber ()
    {
        return flagsNumber;
    }

    //-----------------//
    // getHeadLocation //
    //-----------------//
    public SystemPoint getHeadLocation ()
    {
        if (headLocation == null) {
            computeParameters();
        }

        return headLocation;
    }

    //----------//
    // getNotes //
    //----------//
    public List<TreeNode> getNotes ()
    {
        return children;
    }

    //---------//
    // setStem //
    //---------//
    public void setStem (Glyph stem)
    {
        this.stem = stem;
    }

    //---------//
    // getStem //
    //---------//
    public Glyph getStem ()
    {
        return stem;
    }

    //-----------------//
    // getTailLocation //
    //-----------------//
    public SystemPoint getTailLocation ()
    {
        if (tailLocation == null) {
            computeParameters();
        }

        return tailLocation;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (Visitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addChild //
    //----------//
    /**
     * Override normal behavior, so that adding a note resets chord internal
     * parameters
     *
     * @param node the child to insert in the chord
     */
    @Override
    public void addChild (TreeNode node)
    {
        super.addChild(node);

        // Side effect for note
        if (node instanceof Note) {
            reset();
        }
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate a chord with this glyph
     *
     * @param glyph a chord-relevant glyph (rest, note or notehead)
     * @param measure the containing measure
     */
    static void populate (Glyph       glyph,
                          Measure     measure,
                          SystemPoint sysPt)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Chord Populating " + glyph);
        }

        // First look for a suitable slot
        for (Slot slot : measure.getSlots()) {
            if (slot.isAlignedWith(sysPt)) {
                slot.addGlyph(glyph);

                return;
            }
        }

        // No compatible slot, create a brand new one
        Slot slot = new Slot(measure);
        slot.addGlyph(glyph);
        measure.getSlots()
               .add(slot);
    }

    //-------------------//
    // computeParameters //
    //-------------------//
    private void computeParameters ()
    {
        System system = getPart()
                            .getSystem();

        // Find the note farthest from stem middle point
        if ((stem != null) && (getNotes()
                                   .size() > 0)) {
            SystemPoint middle = system.toSystemPoint(stem.getCenter());
            Note        bestNote = null;
            int         bestDy = 0;

            for (TreeNode node : getNotes()) {
                Note note = (Note) node;
                int  noteY = note.getCenter().y;
                int  dy = Math.abs(noteY - middle.y);

                if (dy > bestDy) {
                    bestNote = note;
                    bestDy = dy;
                }
            }

            PixelRectangle stemBox = stem.getContourBox();

            // Stem tail is shortened by 2 pixels, for better beam connection ??
            if (middle.y < bestNote.getCenter().y) {
                // Stem is up
                tailLocation = system.toSystemPoint(
                    new PixelPoint(
                        stemBox.x + (stemBox.width / 2),
                        stemBox.y + 2));
            } else {
                // Stem is down
                tailLocation = system.toSystemPoint(
                    new PixelPoint(
                        stemBox.x + (stemBox.width / 2),
                        (stemBox.y + stemBox.height) - 2));
            }

            headLocation = new SystemPoint(
                tailLocation.x,
                bestNote.getCenter().y);
        }
    }

    //-------//
    // reset //
    //-------//
    private void reset ()
    {
        headLocation = null;
        tailLocation = null;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
    }
}
