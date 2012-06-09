//----------------------------------------------------------------------------//
//                                                                            //
//                        C h o r d S t a t e m e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.text.Sentence;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.visitor.ScoreVisitor;
import omr.score.visitor.Visitable;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Class {@code ChordStatement} represents a chord statement in the score
 *
 * @author Hervé Bitteur
 */
public class ChordStatement
    extends MeasureElement
    implements Visitable
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        ChordStatement.class);

    /** Regular expressions */
    private static Pattern[] regexps = null;

    //~ Enumerations -----------------------------------------------------------
    /** Names of the various kinds of chords */
    public static enum Type
    {
        //~ Enumeration constant initializers ----------------------------------
        MAJOR,
        MINOR,
        DOMINANT,
        MAJOR_SEVENTH,
        MINOR_SEVENTH;
    }

    //~ Instance fields --------------------------------------------------------

    /** The underlying text */
    private Text.ChordText text;

    /** Root of the chord */
    private Note.Step step;

    /** Pitch alteration */
    private Integer alter;

    /** Chord type */
    private Type type;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // ChordStatement //
    //----------------//
    /**
     * Creates a new instance of ChordStatement event
     *
     * @param measure measure that contains this mark
     * @param referencePoint the reference location of the mark
     * @param chord the chord related to the mark, if any
     * @param sentence the underlying sentence
     * @param text the sentence text
     */
    public ChordStatement (Measure            measure,
                           PixelPoint         referencePoint,
                           Chord              chord,
                           Sentence           sentence,
                           Text.ChordText     text)
    {
        super(measure, true, referencePoint, chord, sentence.getCompound());

        Glyph glyph = sentence.getCompound();

        this.text = text;

        this.step = null;
        this.alter = null;

        // Register at its related chord
        if (chord != null) {
            chord.addChordStatement(this);
        } else {
            // We have a direction item without any related chord/note
            // This is legal, however where do we store this item? TODO
            addError(glyph, "Chord statement with no related note");
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //---------//
    // getText //
    //---------//
    public Text.ChordText getText ()
    {
        return text;
    }

    //---------//
    // getStep //
    //---------//
    public Note.Step getStep ()
    {
        if (step == null) {
            compute();
        }
        return step;
    }

    //----------//
    // getAlter //
    //----------//
    public int getAlter ()
    {
        if (alter == null) {
            compute();
        }
        return alter;
    }

    //----------//
    // getAlter //
    //----------//
    public Type getType ()
    {
        if (type == null) {
            compute();
        }
        return type;
    }

    //----------//
    // getGroup //
    //----------//
    private String getGroup (Matcher matcher, String name)
    {
        String result = null;
        try {
            result = matcher.group(name);
        } catch (IllegalArgumentException e) {
        }
        if (result != null) {
            return result;
        } else {
            return "";
        }
    }

    //---------//
    // compute //
    //---------//
    private void compute ()
    {
        if (regexps == null) {
            String[] uncompiled = {
                "(?<root>[A-Z])(?<alter>[#]?)(?<minor>m?)(?<degree>7?)"
            };
            int length = uncompiled.length;
            regexps = new Pattern[length];
            for (int i = 0; i < length; i++) {
                regexps[i] = Pattern.compile(uncompiled[i]);
            }
        }
        int length = regexps.length;
        String content = text.getContent();
        for (int i = 0; i < length; i++) {
            Matcher matcher = regexps[i].matcher(content);
            if (matcher.matches()) {
                String root = getGroup(matcher, "root");
                String alterText = getGroup(matcher, "alter");
                String minor = getGroup(matcher, "minor");
                String degree = getGroup(matcher, "degree");

                step = Note.Step.valueOf(root);
                switch (alterText) {
                case "":
                    alter = 0;
                    break;
                case "#":
                    alter = 1;
                    break;
                }

                switch (minor + degree) {
                case "":
                    type = Type.MAJOR;
                    break;
                case "7":
                    type = Type.DOMINANT;
                    break;
                case "m":
                    type = Type.MINOR;
                    break;
                case "m7":
                    type = Type.MINOR_SEVENTH;
                    break;
                }
                return;
            }
        }
    }

    //-----------------------//
    // computeReferencePoint //
    //-----------------------//
    @Override
    protected void computeReferencePoint ()
    {
        setReferencePoint(text.getReferencePoint());
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return text.toString();
    }
}
