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

import omr.score.visitor.Visitor;

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
    //~ Instance fields --------------------------------------------------------

    /** A chord stem is virtual when there is no real stem (breve, rest...) */
    private boolean virtualStem;

    /** Ratio to get real duration wrt notation */
    private Double tupletRatio;

    /** Index of this chord in tuplet */
    private Integer tupletIndex;

    /** Number of augmentation dots */
    private int dotsNumber;

    /** Number of flags */
    private int flagsNumber;

    /** Pitch position for chord head */
    private int headPosition;

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
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getNotes //
    //----------//
    public List<TreeNode> getNotes ()
    {
        return children;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (Visitor visitor)
    {
        return visitor.visit(this);
    }
}
