//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   C h o r d N a m e I n t e r                                  //
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

import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.sig.inter.ChordNameInter.Degree.DegreeType;
import static org.audiveris.omr.sig.inter.ChordNameInter.Kind.Type.*;
import org.audiveris.omr.text.TextLine;
import org.audiveris.omr.text.TextWord;
import static org.audiveris.omr.util.RegexUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ChordNameInter} is a formatted piece of text that
 * describes a chord symbol such as F#m7, A(9) or BMaj7/D#.
 * This class is organized according to the target MusicXML harmony element.
 * <p>
 * TODO: Add support for degree subtract (besides add and alter) </p>
 * <p>
 * TODO: Add support for classical functions (besides root)</p>
 * <p>
 * TODO: Add support for French steps: Do, Ré, Mi, etc.</p>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "chord-name")
public class ChordNameInter
        extends WordInter
{

    private static final Logger logger = LoggerFactory.getLogger(ChordNameInter.class);

    /** Unicode value for <b>flat</b> sign: {@value}. */
    public static final String FLAT = "\u266D";

    /** Unicode value for <b>natural</b> sign: {@value}. (not used) */
    public static final String NATURAL = "\u266E";

    /** Unicode value for <b>sharp</b> sign: {@value}. */
    public static final String SHARP = "\u266F";

    /** Unicode value for <b>triangle</b> sign: {@value}. */
    public static final String DELTA = "\u25B3";

    // String constants to be used by their names to avoid any typo.
    // Their values are not relevant, but must be unique.
    private static final String ROOT_STEP = "rootStep";

    private static final String ROOT_ALTER = "rootAlter";

    private static final String BASS_STEP = "bassStep";

    private static final String BASS_ALTER = "bassAlter";

    private static final String MAJ = "maj";

    private static final String MIN = "min";

    private static final String AUG = "aug";

    private static final String DIM = "dim";

    private static final String HDIM = "hdim";

    private static final String DEGS = "degs";

    private static final String SUS = "sus";

    private static final String KIND = "kind";

    private static final String PARS = "pars";

    private static final String PMAJ7 = "pmaj7";

    private static final String DEG_VALUE = "degValue";

    private static final String DEG_ALTER = "degAlter";

    /** Pattern for any step. */
    private static final String STEP_CLASS = "[A-G]";

    /** Pattern for root value. A, A# or Ab */
    private static final String rootPat = group(ROOT_STEP, STEP_CLASS) + group(ROOT_ALTER,
                                                                               Alter.CLASS) + "?";

    /** Pattern for bass value, if any. /A, /A# or /Ab */
    private static final String bassPat = "(/" + group(BASS_STEP, STEP_CLASS) + group(BASS_ALTER,
                                                                                      Alter.CLASS)
                                                  + "?" + ")";

    /** Pattern for major indication. M, maj or DELTA */
    private static final String majPat = group(MAJ, "(M|[Mm][Aa][Jj]|" + DELTA + ")");

    /** Pattern for minor indication. min, m or - */
    private static final String minPat = group(MIN, "(m|[Mm][Ii][Nn]|-)");

    /** Pattern for augmented indication. aug or + */
    private static final String augPat = group(AUG, "([Aa][Uu][Gg]|\\+)");

    /** Pattern for diminished indication. dim or ° */
    private static final String dimPat = group(DIM, "([Dd][Ii][Mm]|°)");

    /** Pattern for half-diminished indication. o with a slash */
    private static final String hdimPat = group(HDIM, "\u00F8");

    /** Pattern for any of the indication alternatives. (except sus) */
    private static final String modePat = "(" + majPat + "|" + minPat + "|" + augPat + "|" + dimPat
                                                  + "|" + hdimPat + ")";

    /** Pattern for (maj7) in min(maj7) = MAJOR_MINOR. */
    private static final String parMajPat = "(\\(" + group(PMAJ7, "(M|[Mm][Aa][Jj]|" + DELTA + ")7")
                                                    + "\\))";

    /** Pattern for any degree value. 5, 6, 7, 9, 11 or 13 */
    private static final String DEG_CLASS = "(5|6|7|9|11|13)";

    /** Pattern for a sequence of degrees. */
    private static final String degsPat = group(DEGS, DEG_CLASS + "(" + Alter.CLASS + DEG_CLASS
                                                              + ")?");

    /** Pattern for a suspended indication. sus2 or sus4 */
    private static final String susPat = group(SUS, "([Ss][Uu][Ss][24])");

    /** Pattern for the whole kind value. */
    private static final String kindPat = group(KIND, modePat + "?" + parMajPat + "?" + degsPat
                                                              + "?" + susPat + "?");

    /** Pattern for parenthesized degrees if any. (6), (#9), (#11b13) */
    private static final String parPat = "(\\(" + group(PARS, Alter.CLASS + "?" + DEG_CLASS + "("
                                                                      + Alter.CLASS + DEG_CLASS
                                                                      + ")*")
                                                 + "\\))";

    /** Un-compiled patterns for whole chord symbol. */
    private static final String[] raws = new String[]{rootPat + kindPat + "?" + parPat + "?"
                                                      + bassPat + "?" // TODO: add a pattern for functions
};

    /** Compiled patterns for whole chord symbol. */
    private static List<Pattern> patterns;

    /** Pattern for one degree. (in a sequence of degrees) */
    private static final String degPat = group(DEG_ALTER, Alter.CLASS) + "?" + group(DEG_VALUE,
                                                                                     DEG_CLASS);

    /** Compiled pattern for one degree. */
    private static final Pattern degPattern = Pattern.compile(degPat);

    /** Root. */
    @XmlElement
    private final Pitch root;

    /** Kind. */
    @XmlElement
    private final Kind kind;

    /** Bass, if any. */
    @XmlElement
    private final Pitch bass;

    /** Degrees, if any. */
    @XmlElement(name = "degree")
    private final List<Degree> degrees;

    /**
     * Creates a new ChordInfo object, with all parameters.
     *
     * @param textWord the full underlying text
     * @param root     root of the chord
     * @param kind     type of the chord
     * @param bass     bass of the chord, or null
     * @param degrees  additions / subtractions / alterations if any
     */
    public ChordNameInter (TextWord textWord,
                           Pitch root,
                           Kind kind,
                           Pitch bass,
                           List<Degree> degrees)
    {
        super(textWord);
        this.root = root;
        this.kind = kind;
        this.bass = bass;
        this.degrees = degrees;
    }

    /**
     * Creates a new ChordInfo object, with all parameters.
     *
     * @param textWord the full underlying text
     * @param root     root of the chord
     * @param kind     type of the chord
     * @param bass     bass of the chord, or null
     * @param degrees  additions / subtractions / alterations if any
     */
    public ChordNameInter (TextWord textWord,
                           Pitch root,
                           Kind kind,
                           Pitch bass,
                           Degree... degrees)
    {
        this(textWord, root, kind, bass, Arrays.asList(degrees));
    }

    /**
     * Convenient constructor that creates a new ChordInfo object with no bass info.
     *
     * @param textWord the full underlying text
     * @param root     root of the chord
     * @param kind     type of the chord
     * @param degrees  additions / subtractions / alterations if any
     */
    public ChordNameInter (TextWord textWord,
                           Pitch root,
                           Kind kind,
                           Degree... degrees)
    {
        this(textWord, root, kind, null, Arrays.asList(degrees));
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private ChordNameInter ()
    {
        this.root = null;
        this.kind = null;
        this.bass = null;
        this.degrees = null;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //--------//
    // create //
    //--------//
    /**
     * From a line of words assumed to be chord names, create a sentence of
     * ChordNameInter instances.
     *
     * @param line the sequence of chord name words
     * @return created sentence of created ChordNameInter instances
     */
    public static SentenceInter create (TextLine line)
    {
        SentenceInter sentence = new SentenceInter(line.getBounds(), line.getConfidence()
                                                                             * Grades.intrinsicRatio,
                                                   line
                                                           .getMeanFont(), line.getRole());

        return sentence;
    }

    //-------------//
    // createValid //
    //-------------//
    /**
     * Convenient method to try to build a ChordNameInter instance from a
     * provided piece of text.
     *
     * @param textWord text the precise text of the chord symbol
     * @return a populated ChordNameInter instance if successful, null otherwise
     */
    public static ChordNameInter createValid (TextWord textWord)
    {
        for (Pattern pattern : getPatterns()) {
            Matcher matcher = pattern.matcher(textWord.getValue());

            if (matcher.matches()) {
                // Root
                Pitch root = Pitch.create(getGroup(matcher, ROOT_STEP),
                                          getGroup(matcher, ROOT_ALTER));

                // Degrees
                String degStr = getGroup(matcher, DEGS);
                List<Degree> degrees = Degree.createList(degStr, null);
                Degree firstDeg = (!degrees.isEmpty()) ? degrees.get(0) : null;
                String firstDegStr = (firstDeg != null) ? Integer.toString(degrees.get(0).value)
                        : "";

                // (maj7) special stuff
                String pmaj7 = standard(matcher, PMAJ7);

                // Kind
                Kind kind = Kind.create(matcher, firstDegStr + pmaj7);

                // Bass
                Pitch bass = Pitch.create(getGroup(matcher, BASS_STEP),
                                          getGroup(matcher, BASS_ALTER));

                if ((firstDeg != null) && (kind.type != SUSPENDED_FOURTH) && (kind.type
                                                                                      != SUSPENDED_SECOND)) {
                    // Remove first degree
                    degrees.remove(firstDeg);
                }

                // Degrees in parentheses
                String parStr = getGroup(matcher, PARS);
                degrees.addAll(Degree.createList(parStr, firstDeg));

                return new ChordNameInter(textWord, root, kind, bass, degrees);
            }
        }

        logger.debug("No pattern match for chord text {}", textWord);

        return null;
    }

    /**
     * @return the bass
     */
    public Pitch getBass ()
    {
        return bass;
    }

    /**
     * @return the degrees
     */
    public List<Degree> getDegrees ()
    {
        return degrees;
    }

    /**
     * @return the kind
     */
    public Kind getKind ()
    {
        return kind;
    }

    /**
     * @return the root
     */
    public Pitch getRoot ()
    {
        return root;
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "CHORD_NAME_\"" + value + "\"";
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        sb.append(" '").append(value).append("'");

        sb.append(" root:").append(root);

        sb.append(" kind:").append(kind);

        if (bass != null) {
            sb.append(" bass:").append(bass);
        }

        for (Degree degree : degrees) {
            sb.append(" deg:").append(degree);
        }

        sb.append("}");

        return sb.toString();
    }

    //-------------//
    // getPatterns //
    //-------------//
    /**
     * Compile if needed, and provide the patterns ready to use.
     *
     * @return the compiled patterns
     */
    private static List<Pattern> getPatterns ()
    {
        if (patterns == null) {
            List<Pattern> ps = new ArrayList<Pattern>();

            for (String raw : raws) {
                ps.add(Pattern.compile(raw));
            }

            patterns = ps;
        }

        return patterns;
    }

    //----------//
    // standard //
    //----------//
    /**
     * Standardize an input sequence, by returning the standard
     * value when the token is valid.
     *
     * @param matcher the matcher
     * @param name    group name which is also the standard value to return if the token is valid
     * @return standard value, or empty string
     */
    private static String standard (Matcher matcher,
                                    String name)
    {
        String token = getGroup(matcher, name);

        return token.isEmpty() ? "" : name;
    }

    //
    //-------//
    // Alter //
    //-------//
    /**
     * Handling of alteration indication (flat, sharp or nothing).
     * The class accepts both number (#) and real sharp sign, as well as both (b) and real flat sign
     */
    public static class Alter
    {

        /** Alter class. */
        private static final String CLASS = "[" + FLAT + "b" + SHARP + "#" + "]";

        /**
         * Convert sharp/flat/empty sign to Integer.
         *
         * @param str the alteration sign
         * @return the Integer value, null if input string has unexpected value
         */
        private static Integer toAlter (String str)
        {
            switch (str) {
            case SHARP:
            case "#":
                return 1;

            case FLAT:
            case "b":
                return -1;

            case "":
                return 0;

            default:
                return null;
            }
        }

        /**
         * Convert an alteration Integer value to the corresponding sign.
         *
         * @param alter Integer value, perhaps null
         * @return the sign string, perhaps empty but not null
         */
        private static String toString (Integer alter)
        {
            if (alter == null) {
                return "";
            } else {
                return (alter == 1) ? "#" : ((alter == -1) ? "b" : "");
            }
        }
    }

    //--------//
    // Degree //
    //--------//
    /**
     * Handling of degree information.
     * <p>
     * TODO: subtraction is not yet handled
     */
    public static class Degree
    {

        public static enum DegreeType
        {
            ADD,
            ALTER,
            SUBTRACT;
        }

        //
        /** nth value of the degree, wrt the chord root. */
        @XmlAttribute
        public final int value;

        /** Alteration, if any. */
        @XmlAttribute
        public final Integer alter;

        /** Which operation is performed. */
        @XmlAttribute
        public final DegreeType type;

        /** Specific text display for degree operation, if any. */
        @XmlAttribute
        public final String text;

        //
        public Degree (int value,
                       Integer alter,
                       DegreeType type)
        {
            this(value, alter, type, "");
        }

        public Degree (int value,
                       Integer alter,
                       DegreeType type,
                       String text)
        {
            this.value = value;
            this.alter = alter;
            this.type = type;
            this.text = text;
        }

        //
        /**
         * Build a sequence of Degree instances from the provided string
         *
         * @param str      the provided string, without parentheses, such as 7b13
         * @param dominant the chord dominant degree, if any, otherwise null
         * @return the list of degrees
         */
        public static List<Degree> createList (String str,
                                               Degree dominant)
        {
            List<Degree> degrees = new ArrayList<Degree>();

            if ((str == null) || str.isEmpty()) {
                return degrees;
            }

            // Loop on occurrences of the one-degree pattern
            Matcher matcher = degPattern.matcher(str);

            while (matcher.find()) {
                // Deg value
                String degStr = getGroup(matcher, DEG_VALUE);
                final int deg = Integer.decode(degStr);

                // Deg type: 'add' or 'alter'
                // TODO: handle 'subtract' as well
                final DegreeType type;

                if ((dominant != null) && (dominant.value > deg)) {
                    type = DegreeType.ALTER;
                } else if (deg <= 5) {
                    type = DegreeType.ALTER;
                } else {
                    type = DegreeType.ADD;
                }

                // Deg alter
                final String altStr = getGroup(matcher, DEG_ALTER);
                final Integer alter = Alter.toAlter(altStr);

                degrees.add(new Degree(deg, alter, type, ""));
            }

            return degrees;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("(");

            sb.append(value);

            sb.append(Alter.toString(alter));

            sb.append(" ").append(type);

            if (!text.isEmpty()) {
                sb.append(" '").append(text).append("'");
            }

            sb.append(")");

            return sb.toString();
        }
    }

    //------//
    // Kind //
    //------//
    /**
     * Handling of kind (aka quality) chord information.
     */
    public static class Kind
    {

        public static enum Type
        {
            MAJOR,
            MINOR,
            AUGMENTED,
            DIMINISHED,
            DOMINANT,
            MAJOR_SEVENTH,
            MINOR_SEVENTH,
            DIMINISHED_SEVENTH,
            AUGMENTED_SEVENTH,
            HALF_DIMINISHED,
            MAJOR_MINOR,
            MAJOR_SIXTH,
            MINOR_SIXTH,
            DOMINANT_NINTH,
            MAJOR_NINTH,
            MINOR_NINTH,
            DOMINANT_11_TH,
            MAJOR_11_TH,
            MINOR_11_TH,
            DOMINANT_13_TH,
            MAJOR_13_TH,
            MINOR_13_TH,
            SUSPENDED_SECOND,
            SUSPENDED_FOURTH,

            //        NEAPOLITAN,
            //        ITALIAN,
            //        FRENCH,
            //        GERMAN,
            //        PEDAL,
            //        POWER,
            //        TRISTAN,
            OTHER,
            NONE;
        }

        //
        /** Precise type of kind. (subset of the 33 Music XML values) */
        @XmlAttribute
        public final Type type;

        /** Flag to signal parentheses, if any. */
        @XmlAttribute
        public final Boolean parentheses;

        /** Flag to signal use of symbol, if any. */
        @XmlAttribute
        public final Boolean symbol;

        /** Exact display text for the chord kind. (For example min vs m) */
        @XmlAttribute
        public final String text;

        public Kind (Type type)
        {
            this(type, "", null, null);
        }

        public Kind (Type type,
                     String text)
        {
            this(type, text, null, null);
        }

        public Kind (Type type,
                     String text,
                     Boolean symbol)
        {
            this(type, text, symbol, null);
        }

        public Kind (Type type,
                     String text,
                     Boolean symbol,
                     Boolean parentheses)
        {
            this.type = type;
            this.parentheses = parentheses;
            this.text = text;
            this.symbol = symbol;
        }

        // For JAXB
        private Kind ()
        {
            this.type = null;
            this.parentheses = null;
            this.symbol = null;
            this.text = null;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append("{");

            sb.append(type);

            if (!text.isEmpty()) {
                sb.append(" '").append(text).append("'");
            }

            if ((parentheses != null) && parentheses) {
                sb.append(" parens");
            }

            if ((symbol != null) && symbol) {
                sb.append(" symbol");
            }

            sb.append("}");

            return sb.toString();
        }

        /**
         * Create proper Kind object from a provided matcher, augmented
         * by dominant string if any.
         *
         * @param matcher  matcher on input string
         * @param dominant dominant information if any, empty string otherwise
         * @return Kind instance, or null if failed
         */
        private static Kind create (Matcher matcher,
                                    String dominant)
        {
            String kindStr = getGroup(matcher, KIND);

            String parStr = getGroup(matcher, PARS);
            Boolean parentheses = (!parStr.isEmpty()) ? Boolean.TRUE : null;

            // Check for suspended first
            String susStr = getGroup(matcher, SUS);

            switch (susStr.toLowerCase()) {
            case "sus2":
                return new Kind(SUSPENDED_SECOND, kindStr, null, parentheses);

            case "sus4":
                return new Kind(SUSPENDED_FOURTH, kindStr, null, parentheses);

            case "": // Fall through

            }

            // Then check for other combinations
            final String str = standard(matcher, MIN) + standard(matcher, MAJ) + standard(matcher,
                                                                                          AUG)
                                       + standard(matcher, DIM) + standard(matcher, HDIM) + dominant;
            Type type = typeOf(str);

            // Special case for Triangle sign => maj7 rather than major
            if ((type == MAJOR) && getGroup(matcher, MAJ).equals(DELTA)) {
                type = MAJOR_SEVENTH;
            }

            // Use of symbol?
            Boolean symbol = (getGroup(matcher, MAJ).equals(DELTA) || getGroup(matcher, MIN).equals(
                    "-") || getGroup(matcher, AUG).equals("+")) ? Boolean.TRUE : null;

            return (type != null) ? new Kind(type, kindStr, symbol, parentheses) : null;
        }

        /**
         * Convert a provided string to proper Type value.
         *
         * @param str provided string, assumed to contain only 'standard' values
         * @return the corresponding type, or null if none found
         */
        private static Type typeOf (String str)
        {
            switch (str) {
            case "":
            case MAJ:
                return MAJOR;

            case MIN:
                return MINOR;

            case AUG:
                return AUGMENTED;

            case DIM:
                return DIMINISHED;

            case "7":
                return DOMINANT;

            case MAJ + "7":
                return MAJOR_SEVENTH;

            case MIN + "7":
                return MINOR_SEVENTH;

            case DIM + "7":
                return DIMINISHED_SEVENTH;

            case AUG + "7":
                return AUGMENTED_SEVENTH;

            case HDIM:
                return HALF_DIMINISHED;

            case MIN + PMAJ7:
                return MAJOR_MINOR;

            case MAJ + "6":
            case "6":
                return MAJOR_SIXTH;

            case MIN + "6":
                return MINOR_SIXTH;

            case "9":
                return DOMINANT_NINTH;

            case MAJ + "9":
                return MAJOR_NINTH;

            case MIN + "9":
                return MINOR_NINTH;

            case "11":
                return DOMINANT_11_TH;

            case MAJ + "11":
                return MAJOR_11_TH;

            case MIN + "11":
                return MINOR_11_TH;

            case "13":
                return DOMINANT_13_TH;

            case MAJ + "13":
                return MAJOR_13_TH;

            case MIN + "13":
                return MINOR_13_TH;

            default:
                // Nota: Thanks to regexp match, this should not happen
                logger.warn("No kind type for {}", str);

                return null;
            }
        }
    }

    //-------//
    // Pitch //
    //-------//
    /**
     * General handling of pitch information, used by root and bass.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class Pitch
    {

        /** Related step. */
        @XmlAttribute
        public final AbstractNoteInter.Step step;

        /** Alteration, if any. */
        @XmlAttribute
        public final Integer alter;

        public Pitch (AbstractNoteInter.Step step,
                      Integer alter)
        {
            this.step = step;
            this.alter = alter;
        }

        public Pitch (AbstractNoteInter.Step step)
        {
            this(step, 0);
        }

        // For JAXB
        private Pitch ()
        {
            this.step = null;
            this.alter = null;
        }

        /**
         * Create a Pitch object from provided step and alter strings
         *
         * @param stepStr  provided step string
         * @param alterStr provided alteration string
         * @return Pitch instance, or null if failed
         */
        public static Pitch create (String stepStr,
                                    String alterStr)
        {
            stepStr = stepStr.trim();
            alterStr = alterStr.trim();

            if (!stepStr.isEmpty()) {
                return new Pitch(AbstractNoteInter.Step.valueOf(stepStr), Alter.toAlter(alterStr));
            } else {
                return null;
            }
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(step);

            sb.append(Alter.toString(alter));

            return sb.toString();
        }
    }

    /*
     * major : X|maj ................ : 1 - 3 - 5
     * minor : m|min ................ : 1 - b3 - 5
     * augmented : +|aug............. : 1 - 3 - #5
     * diminished : °|dim............ : 1 - b3 - b5
     * dominant : 7 ................. : 1 - 3 - 5 - b7
     * major-seventh : M7|maj7....... : 1 - 3 - 5 - 7
     * minor-seventh : m7|min7....... : 1 - b3 - 5 - b7
     * diminished-seventh : dim7..... : 1 - b3 - b5 -bb7
     * augmented-seventh :+7|7+5|aug7 : 1 - 3 - #5 - b7
     * half-diminished :-7b5|m7b5.... : 1 - b3 - b5 - b7
     * major-minor : min(maj7)....... : 1 - b3 - 5 - 7
     * major-sixth : 6 .............. : 1 - 3 - 5 - 6
     * minor-sixth : m6|min6......... : 1 - b3 - 5 - 6
     * dominant-ninth : 9............ : 1 - 3 - 5 - b7 - 9
     * major-ninth : M9|maj9......... : 1 - 3 - 5 - 7 - 9
     * minor-ninth : m9|min9......... : 1 - b3 - 5 - b7 - 9
     * dominant-11th : 11............ : 1 - 3 - 5 - b7 - 9 - 11
     * major-11th : M11|maj11........ : 1 - 3 - 5 - 7 - 9 - 11
     * minor-11th : m11|min11........ : 1 - b3 - 5 - b7 - 9 - 11
     * dominant-13th : 13............ : 1 - 3 - 5 - b7 - 9 - 11 - 13
     * major-13th : M13|maj13........ : 1 - 3 - 5 - 7 - 9 - 11 - 13
     * minor-13th : m13|min13........ : 1 - b3 - 5 - b7 - 9 - 11 - 13
     * suspended-second : sus2....... : 1 - 2 - 5
     * suspended-fourth : sus4....... : 1 - 4 - 5
     * suspended-ninth : sus9........ : 1 - 4 - 5 - b7 - 9
     * Neapolitan : ................. :
     * Italian : .................... :
     * French : ..................... :
     * German : ..................... :
     * pedal : ...................... :
     * power : ...................... :
     * Tristan : .................... :
     * other : ...................... :
     * none : ....................... :
     */
}
