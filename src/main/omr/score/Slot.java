//----------------------------------------------------------------------------//
//                                                                            //
//                                  S l o t                                   //
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
import omr.glyph.Shape;

import omr.math.Population;

import omr.sheet.Scale;

import omr.util.Logger;

import proxymusic.Rest;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>Slot</code> represents a roughly defined time slot within a
 * measure, to gather all chord entities (rests, notes, noteheads) that occur at
 * the same time because their abscissae are roughly the same.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Slot
    implements Comparable<Slot>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Slot.class);

    //~ Instance fields --------------------------------------------------------

    /** Abscissa of the slot */
    private Integer x;

    /** The containing measure */
    private Measure measure;

    /** Collection of glyphs in the slot */
    private List<Glyph> glyphs = new ArrayList<Glyph>();

    /** Collection of chords in this slot */
    private List<Chord> chords = new ArrayList<Chord>();

    //~ Constructors -----------------------------------------------------------

    //------//
    // Slot //
    //------//
    /**
     * Creates a new Slot object.
     *
     * @param measure the containing measure
     */
    public Slot (Measure measure)
    {
        this.measure = measure;
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // isAlignedWith //
    //---------------//
    public boolean isAlignedWith (SystemPoint sysPt)
    {
        return Math.abs(sysPt.x - getX()) <= measure.getScale()
                                                    .toUnits(constants.maxDx);
    }

    //------//
    // getX //
    //------//
    public int getX ()
    {
        if (x == null) {
            Population population = new Population();

            for (Glyph glyph : glyphs) {
                population.includeValue(
                    measure.getPart()
                           .getSystem()
                           .toSystemPoint(glyph.getCenter()).x);
            }

            if (population.getCardinality() > 0) {
                x = (int) Math.rint(population.getMeanValue());
            }
        }

        return x;
    }

    //----------//
    // addGlyph //
    //----------//
    public void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);
        x = null;
    }

    //----------------//
    // allocateChords //
    //----------------//
    public void allocateChords ()
    {
        // Allocate 1 chord per stem, per rest, per (whole) note
        for (Glyph glyph : glyphs) {
            Chord chord = null;

            if (glyph.getStemNumber() > 0) {
                // Beware of noteheads with 2 stems, we need to duplicate them
                if (glyph.getLeftStem() != null) {
                    chord = getStemChord(glyph.getLeftStem());
                    new Note(chord, glyph);
                }

                if (glyph.getRightStem() != null) {
                    chord = getStemChord(glyph.getRightStem());
                    new Note(chord, glyph);
                }
            } else {
                chord = new Chord(measure);
                chords.add(chord);
                new Note(chord, glyph);
            }
        }
    }

    //-----------//
    // compareTo //
    //-----------//
    public int compareTo (Slot other)
    {
        return Integer.signum(getX() - other.getX());
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Slot");

        sb.append(" x=")
          .append(getX());

        sb.append(" glyphs=[");

        for (Glyph glyph : glyphs) {
            sb.append("#")
              .append(glyph.getId())
              .append("/")
              .append(glyph.getShape())
              .append(" ");
        }

        sb.append("]");
        sb.append("}");

        return sb.toString();
    }

    //--------------//
    // getStemChord //
    //--------------//
    private Chord getStemChord (Glyph stem)
    {
        // Check we don't already have this stem in a chord
        for (Chord chord : chords) {
            if (chord.getStem() == stem) {
                return chord;
            }
        }

        // Not found, let's create it'
        Chord chord = new Chord(measure);
        chords.add(chord);
        chord.setStem(stem);

        return chord;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /**
         * Maximum horizontal distance between a slot and a glyph candidate
         */
        Scale.Fraction maxDx = new Scale.Fraction(
            1.0,
            "Maximum horizontal distance (interline fraction) between a slot" +
            " and a glyph candidate");
    }
}
