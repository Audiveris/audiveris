//-----------------------------------------------------------------------//
//                                                                       //
//                            M u s i c X M L                            //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.score.Note;
import omr.score.Staff;
import omr.score.SystemPoint;

import omr.util.Logger;

import proxymusic.*;

import java.lang.String;

/**
 * Class <code>MusicXML</code> gathers symbols related to the MusicXML data
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class MusicXML
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(MusicXML.class);

    // A few constants to avoid typos
    static final String           ABOVE = "above";
    static final String           BACKWARD_HOOK = "backward hook";
    static final String           BEGIN = "begin";
    static final String           BELOW = "below";
    static final String           COMMON = "common";
    static final String           CONTINUE = "continue";
    static final String           CRESCENDO = "crescendo";
    static final String           CUT = "cut";
    static final String           DIMINUENDO = "diminuendo";
    static final String           DOWN = "down";
    static final String           END = "end";
    static final String           FORWARD_HOOK = "forward hook";
    static final String           INVERTED = "inverted";
    static final String           NO = "no";
    static final String           OVER = "over";
    static final String           START = "start";
    static final String           STOP = "stop";
    static final String           UNDER = "under";
    static final String           UP = "up";
    static final String           UPRIGHT = "upright";
    static final String           YES = "yes";

    /** Names of the various note types used in MusicXML */
    private static final String[] noteTypeNames = new String[] {
                                                      "256th", "128th", "64th",
                                                      "32nd", "16th", "eighth",
                                                      "quarter", "half", "whole",
                                                      "breve", "long"
                                                  };

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // ScoreExporter //
    //---------------//
    /**
     * Not meant to be instantiated
     */
    private MusicXML ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // accidentalNameOf //
    //------------------//
    public static String accidentalNameOf (Shape shape)
    {
        ///sharp, natural, flat, double-sharp, sharp-sharp, flat-flat
        switch (shape) {
        case SHARP :
            return "sharp";

        case NATURAL :
            return "natural";

        case FLAT :
            return "flat";

        case DOUBLE_SHARP :
            return "double-sharp";

        case DOUBLE_FLAT :
            return "flat-flat";
        }

        logger.warning("Illegal shape for accidental: " + shape);

        return "";
    }

    //------------//
    // barStyleOf //
    //------------//
    /**
     * Report the MusicXML bar style for a recognized Barline shape
     *
     * @param shape the barline shape
     * @return the bar style
     */
    public static String barStyleOf (Shape shape)
    {
        //      Bar-style contains style information. Choices are
        //      regular, dotted, dashed, heavy, light-light,
        //      light-heavy, heavy-light, heavy-heavy, and none.
        switch (shape) {
        case SINGLE_BARLINE :
            return "light";

        case DOUBLE_BARLINE :
            return "light-light";

        case FINAL_BARLINE :
            return "light-heavy";

        case REVERSE_FINAL_BARLINE :
            return "heavy-light";

        case LEFT_REPEAT_SIGN :
            return "heavy-light";

        case RIGHT_REPEAT_SIGN :
            return "light-heavy";

        case BACK_TO_BACK_REPEAT_SIGN :
            return "heavy-heavy"; // ?
        }

        return "???";
    }

    //-------------------//
    // getDynamicsObject //
    //-------------------//
    public static Object getDynamicsObject (Shape shape)
    {
        switch (shape) {
        case DYNAMICS_F :
            return new F();

        case DYNAMICS_FF :
            return new Ff();

        case DYNAMICS_FFF :
            return new Fff();

        case DYNAMICS_FFFF :
            return new Ffff();

        case DYNAMICS_FFFFF :
            return new Fffff();

        case DYNAMICS_FFFFFF :
            return new Ffffff();

        case DYNAMICS_FP :
            return new Fp();

        case DYNAMICS_FZ :
            return new Fz();

        case DYNAMICS_MF :
            return new Mf();

        case DYNAMICS_MP :
            return new Mp();

        case DYNAMICS_P :
            return new P();

        case DYNAMICS_PP :
            return new Pp();

        case DYNAMICS_PPP :
            return new Ppp();

        case DYNAMICS_PPPP :
            return new Pppp();

        case DYNAMICS_PPPPP :
            return new Ppppp();

        case DYNAMICS_PPPPPP :
            return new Pppppp();

        case DYNAMICS_RF :
            return new Rf();

        case DYNAMICS_RFZ :
            return new Rfz();

        case DYNAMICS_SF :
            return new Sf();

        case DYNAMICS_SFFZ :
            return new Sffz();

        case DYNAMICS_SFP :
            return new Sfp();

        case DYNAMICS_SFPP :
            return new Sfpp();

        case DYNAMICS_SFZ :
            return new Sfz();
        }

        logger.severe("Unsupported dynamics shape:" + shape);

        return null;
    }

    //-------------//
    // getTypeName //
    //-------------//
    /**
     * Report the name for the note type
     *
     * @param note the note whose type name is needed
     * @return proper note type name
     */
    public static String getNoteTypeName (Note note)
    {
        // Since quarter is at index 6 in noteTypeNames, use 2**6 = 64
        int dur = (64 * note.getNoteDuration()) / Note.QUARTER_DURATION;
        int index = (int) Math.rint(Math.log(dur) / Math.log(2));

        return noteTypeNames[index];
    }

    //-------------------//
    // getOrnamentObject //
    //-------------------//
    public static Object getOrnamentObject (Shape shape)
    {
        //	(((trill-mark | turn | delayed-turn | shake |
        //	   wavy-line | mordent | inverted-mordent | 
        //	   schleifer | tremolo | other-ornament), 
        //	   accidental-mark*)*)>
        switch (shape) {
        case INVERTED_MORDENT :
            return new proxymusic.InvertedMordent();

        case MORDENT :
            return new proxymusic.Mordent();

        case TR :
            return new proxymusic.TrillMark();

        case TURN :
            return new proxymusic.Turn();
        }

        logger.severe("Unsupported ornament shape:" + shape);

        return null;
    }

    //----------//
    // toTenths //
    //----------//
    /**
     * Convert a value expressed in units to a string value expressed in tenths
     *
     * @param units the number of units
     * @return the number of tenths as a string
     */
    public static String toTenths (double units)
    {
        // Divide by 1.6 with rounding to nearest integer value
        return Integer.toString((int) Math.rint(units / 1.6));
    }

    //-----//
    // yOf //
    //-----//
    /**
     * Report the musicXML Y value of a SystemPoint ordinate.
     *
     * @param units the system-based ordinate (in units)
     * @param staff the related staff
     * @return the upward-oriented ordinate wrt to staff top line (in tenths string)
     */
    public static String yOf (double units,
                              Staff  staff)
    {
        return toTenths(
            staff.getTopLeft().y - staff.getSystem().getTopLeft().y - units);
    }

    //-----//
    // yOf //
    //-----//
    /**
     * Report the musicXML Y value of a SystemPoint. This method is safer than
     * the other one which simply accepts a (detyped) double ordinate.
     *
     * @param point the system-based point
     * @param staff the related staff
     * @return the upward-oriented ordinate wrt to staff top line (in tenths string)
     */
    public static String yOf (SystemPoint point,
                              Staff       staff)
    {
        return toTenths(
            staff.getTopLeft().y - staff.getSystem().getTopLeft().y - point.y);
    }
}
