//----------------------------------------------------------------------------//
//                                                                            //
//                                  N o t e                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.visitor.Visitor;

import omr.sheet.PixelPoint;
import omr.sheet.PixelRectangle;

/**
 * Class <code>Note</code> represents the characteristics of a note. Besides a
 * regular note, it can also be a cue note or a grace note.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Note
    extends MeasureNode
{
    //~ Instance fields --------------------------------------------------------

    /** The underlying glyph */
    private final Glyph glyph;

    /** The note shape */
    private final Shape shape;

    /** Related staff */
    private final Staff staff;

    /** Accidental is any */
    private Shape accidental;

    /** Indicate a rest */
    private final boolean isRest;

    /** Note duration (not for grace notes) */
    private int duration;

    /** Pitch alteration (not for rests) */
    private int alter;

    /** Tie ??? */

    /** Note center */
    private final SystemPoint center;

    /** Pitch position */
    private final double pitchPosition;

    //~ Constructors -----------------------------------------------------------

    /** Type is inferred from shape and chord beam/flag number */
    /** Rest is inferred from shape */
    /** Pitch or rest step/octave are inferred from pitchPosition */

    //------//
    // Note //
    //------//
    /** Creates a new instance of Note */
    public Note (Chord chord,
                 Glyph glyph)
    {
        super(chord);
        this.glyph = glyph;

        // Rest?
        isRest = Shape.Rests.contains(glyph.getShape());

        // Staff, center & pitchPosition
        System system = getPart()
                            .getSystem();
        center = system.toSystemPoint(glyph.getCenter());
        staff = system.getStaffAt(center);
        pitchPosition = staff.pitchPositionOf(center);

        // Shape (to be verified ... TBD)
        shape = glyph.getShape();
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getCenter //
    //-----------//
    public SystemPoint getCenter ()
    {
        return center;
    }

    //----------//
    // getChord //
    //----------//
    public Chord getChord ()
    {
        return (Chord) getContainer();
    }

    /// DEBUG
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //------------------//
    // getPitchPosition //
    //------------------//
    public double getPitchPosition ()
    {
        return pitchPosition;
    }

    public Shape getShape ()
    {
        return shape;
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
