//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   C h o r d N a m e I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.AUGMENTED;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.AUGMENTED_SEVENTH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.DIMINISHED;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.DIMINISHED_SEVENTH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.DOMINANT;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.DOMINANT_11_TH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.DOMINANT_13_TH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.DOMINANT_NINTH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.HALF_DIMINISHED;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MAJOR;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MAJOR_11_TH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MAJOR_13_TH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MAJOR_MINOR;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MAJOR_NINTH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MAJOR_SEVENTH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MAJOR_SIXTH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MINOR;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MINOR_11_TH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MINOR_13_TH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MINOR_NINTH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MINOR_SEVENTH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.MINOR_SIXTH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.SUSPENDED_FOURTH;
import static org.audiveris.omr.sig.inter.ChordNameInter.ChordKind.ChordType.SUSPENDED_SECOND;
import org.audiveris.omr.sig.relation.ChordNameRelation;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.text.TextLine;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.text.TextWord;
import org.audiveris.omr.util.Jaxb;
import static org.audiveris.omr.util.RegexUtil.getGroup;
import static org.audiveris.omr.util.RegexUtil.group;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>ChordNameInter</code> is a formatted piece of text that
 * describes a chord symbol such as F#m7, Emb5, D7#5, Am7b5, A(9) or BMaj7/D#.
 * <p>
 * This class is organized according to the target MusicXML harmony element.
 * <p>
 * TODO: Add support for degree subtract (besides add and alter)
 * <p>
 * TODO: Add support for classical functions (besides root)
 * <p>
 * TODO: Add support for French steps: Do, Re, Mi, etc.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "chord-name")
public class ChordNameInter
        extends WordInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

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

    private static final String NO_PARS = "nopars";

    private static final String PMAJ7 = "pmaj7";

    private static final String DEG_VALUE = "degValue";

    private static final String DEG_ALTER = "degAlter";

    /** Pattern for any step. */
    private static final String STEP_CLASS = "[A-G]";

    /** Pattern for root value. A, A# or Ab */
    private static final String rootPat = //
            group(ROOT_STEP, STEP_CLASS) + group(ROOT_ALTER, Alter.CLASS) + "?";

    /** Pattern for bass value, if any. /A, /A# or /Ab */
    private static final String bassPat = //
            "(/" + group(BASS_STEP, STEP_CLASS) + group(BASS_ALTER, Alter.CLASS) + "?" + ")";

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
    private static final String modePat = //
            "(" + majPat + "|" + minPat + "|" + augPat + "|" + dimPat + "|" + hdimPat + ")";

    /** Pattern for (maj7) in min(maj7) = MAJOR_MINOR. */
    private static final String parMajPat = //
            "(\\(" + group(PMAJ7, "(M|[Mm][Aa][Jj]|" + DELTA + ")7") + "\\))";

    /** Pattern for any degree value. 5, 6, 7, 9, 11 or 13 */
    private static final String DEG_CLASS = "(5|6|7|9|11|13)";

    /** Pattern for a sequence of degrees. */
    private static final String degsPat = //
            group(DEGS, DEG_CLASS + "(" + Alter.CLASS + DEG_CLASS + ")?");

    /** Pattern for a suspended indication. sus2 or sus4 */
    private static final String susPat = group(SUS, "([Ss][Uu][Ss][24])");

    /** Pattern for the whole kind value. */
    private static final String kindPat = //
            group(KIND, modePat + "?" + parMajPat + "?" + degsPat + "?" + susPat + "?");

    /** Pattern for parenthesized degrees if any. (6), (#9), (#11b13) */
    private static final String parPat = "(\\(" //
            + group(PARS, Alter.CLASS + "?" + DEG_CLASS + "(" + Alter.CLASS + DEG_CLASS + ")*")
            + "\\))";

    /** Pattern for non-parenthesized degrees if any. b5 */
    private static final String noParPat = "(" //
            + group(NO_PARS, Alter.CLASS + "?" + DEG_CLASS + "(" + Alter.CLASS + DEG_CLASS + ")*")
            + ")";

    /**
     * Non-compiled patterns for whole chord symbol.
     * TODO: add a pattern for functions
     */
    private static final String[] raws = new String[] //
    { rootPat + kindPat + "?" + "(" + parPat + "|" + noParPat + ")" + "?" + bassPat + "?" };

    /** Compiled patterns for whole chord symbol. */
    private static List<Pattern> patterns;

    /** Pattern for one degree. (in a sequence of degrees) */
    private static final String degPat = //
            group(DEG_ALTER, Alter.CLASS) + "?" + group(DEG_VALUE, DEG_CLASS);

    /** Compiled pattern for one degree. */
    private static final Pattern degPattern = Pattern.compile(degPat);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Chord root. */
    @XmlElement(name = "root")
    private ChordNamePitch root;

    /** Chord kind. */
    @XmlElement(name = "kind")
    private ChordKind kind;

    /** Chord bass, if any. */
    @XmlElement(name = "bass")
    private ChordNamePitch bass;

    /** Chord degrees, if any. */
    @XmlElement(name = "degree")
    private List<ChordDegree> degrees;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private ChordNameInter ()
    {
        this.root = null;
        this.kind = null;
        this.bass = null;
        this.degrees = null;
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
                           ChordNamePitch root,
                           ChordKind kind,
                           ChordDegree... degrees)
    {
        this(textWord, root, kind, null, Arrays.asList(degrees));
    }

    /**
     * Creates a new ChordNameInter object, with all parameters.
     *
     * @param textWord the full underlying text
     * @param root     root of the chord
     * @param kind     type of the chord
     * @param bass     bass of the chord, or null
     * @param degrees  additions / subtractions / alterations if any
     */
    public ChordNameInter (TextWord textWord,
                           ChordNamePitch root,
                           ChordKind kind,
                           ChordNamePitch bass,
                           ChordDegree... degrees)
    {
        this(textWord, root, kind, bass, Arrays.asList(degrees));
    }

    /**
     * Creates a new ChordNameInter object, with all parameters.
     *
     * @param textWord the full underlying text
     * @param root     root of the chord
     * @param kind     type of the chord
     * @param bass     bass of the chord, or null
     * @param degrees  additions / subtractions / alterations if any
     */
    public ChordNameInter (TextWord textWord,
                           ChordNamePitch root,
                           ChordKind kind,
                           ChordNamePitch bass,
                           List<ChordDegree> degrees)
    {
        super(textWord);
        this.root = root;
        this.kind = kind;
        this.bass = bass;
        this.degrees = degrees;
    }

    /**
     * Creates a new ChordNameInter object from a WordInter.
     *
     * @param w the WordInter to convert from
     */
    public ChordNameInter (WordInter w)
    {
        super(
                w.getGlyph(),
                w.getBounds(),
                Shape.TEXT,
                w.getGrade(),
                w.getValue(),
                w.getFontInfo(),
                PointUtil.rounded(w.getLocation()));

        setValue(w.getValue());
    }

    /**
     * Creates a new ChordNameInter object from a WordInter.
     *
     * @param w       the WordInter to convert from
     * @param root    root of the chord
     * @param kind    type of the chord
     * @param bass    bass of the chord, or null
     * @param degrees additions / subtractions / alterations if any
     */
    public ChordNameInter (WordInter w,
                           ChordNamePitch root,
                           ChordKind kind,
                           ChordNamePitch bass,
                           List<ChordDegree> degrees)
    {
        super(
                w.getGlyph(),
                w.getBounds(),
                w.getShape(),
                w.getGrade(),
                w.getValue(),
                w.getFontInfo(),
                PointUtil.rounded(w.getLocation()));
        this.root = root;
        this.kind = kind;
        this.bass = bass;
        this.degrees = degrees;
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * @return the bass
     */
    public ChordNamePitch getBass ()
    {
        return bass;
    }

    /**
     * @return the degrees
     */
    public List<ChordDegree> getDegrees ()
    {
        return degrees;
    }

    /**
     * @return the kind
     */
    public ChordKind getKind ()
    {
        return kind;
    }

    /**
     * @return the root
     */
    public ChordNamePitch getRoot ()
    {
        return root;
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "CHORD_NAME: " + value;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(" root:").append(root);

        sb.append(' ').append(kind);

        if (bass != null) {
            sb.append(" bass:").append(bass);
        }

        if (degrees != null) {
            for (ChordDegree degree : degrees) {
                sb.append(" deg:").append(degree);
            }
        }

        return sb.toString();
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel,
                                          Wrapper<Inter> toPublish)
    {
        // Standard addition task for this chord name word
        final SystemInfo system = staff.getSystem();
        final List<UITask> tasks = new ArrayList<>();
        tasks.add(new AdditionTask(system.getSig(), this, getBounds(), searchLinks(system)));

        // Wrap this word within a sentence with chord name role
        final SentenceInter sentence = new SentenceInter(TextRole.ChordName, null);
        sentence.setManual(true);
        sentence.setStaff(staff);

        tasks.add(
                new AdditionTask(
                        staff.getSystem().getSig(),
                        sentence,
                        getBounds(),
                        Arrays.asList(new Link(this, new Containment(), true))));

        return tasks;
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Use the new value to parse chord name information.
     * <p>
     * If parsing is correct, placeholders (b and #) are replaced by true alteration signs.
     *
     * @param value the new text value
     */
    @Override
    public void setValue (String value)
    {
        final ChordStructure cs = parseChord(value);

        if (cs != null) {
            root = cs.root;
            kind = cs.kind;
            bass = cs.bass;
            degrees = cs.degrees;
            super.setValue(value.replaceAll("b", FLAT).replaceAll("#", SHARP));
        } else {
            logger.info("Failed parsing ChordName text: {}", value);
            super.setValue(value);
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

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
        SentenceInter sentence = new SentenceInter(
                line.getBounds(),
                line.getConfidence() * Grades.intrinsicRatio,
                line.getMeanFont(),
                line.getRole());

        return sentence;
    }

    //-------------//
    // createValid //
    //-------------//
    /**
     * Try to create a ChordNameInter instance from a provided TextWord.
     *
     * @param textWord the provided TextWord
     * @return a populated ChordNameInter instance if successful, null otherwise
     */
    public static ChordNameInter createValid (TextWord textWord)
    {
        final ChordStructure cs = parseChord(textWord.getValue());

        if (cs != null) {
            return new ChordNameInter(textWord, cs.root, cs.kind, cs.bass, cs.degrees);
        }

        logger.debug("No pattern match for chord text {}", textWord);

        return null;
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
            List<Pattern> ps = new ArrayList<>();

            for (String raw : raws) {
                ps.add(Pattern.compile(raw));
            }

            patterns = ps;
        }

        return patterns;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this ChordNameInter and a HeadChord nearby.
     *
     * @param system system to be looked up
     * @return the link found or null
     */
    public Link lookupLink (SystemInfo system)
    {
        final Point wordCenter = getCenter();
        final MeasureStack stack = system.getStackAt(wordCenter);
        final AbstractChordInter chordBelow = stack.getStandardChordBelow(wordCenter, getBounds());

        if (chordBelow == null) {
            return null;
        }

        return new Link(chordBelow, new ChordNameRelation(), false);
    }

    //------------//
    // parseChord //
    //------------//
    /**
     * Parse the provided string to try to extract all chord name items.
     *
     * @param value text the precise text of the chord symbol candidate
     * @return a populated ChordStructure if successful, null otherwise
     */
    private static ChordStructure parseChord (String value)
    {
        for (Pattern pattern : getPatterns()) {
            Matcher matcher = pattern.matcher(value);

            if (matcher.matches()) {
                // Root
                ChordNamePitch root = ChordNamePitch.createValid(
                        getGroup(matcher, ROOT_STEP),
                        getGroup(matcher, ROOT_ALTER));

                // Degrees
                String degStr = getGroup(matcher, DEGS);
                List<ChordDegree> degrees = ChordDegree.createList(degStr, null);
                ChordDegree firstDeg = (!degrees.isEmpty()) ? degrees.get(0) : null;
                String firstDegStr = (firstDeg != null) ? Integer.toString(degrees.get(0).value)
                        : "";

                // (maj7) special stuff
                String pmaj7 = standard(matcher, PMAJ7);

                // ChordKind
                ChordKind kind = ChordKind.createValid(matcher, firstDegStr + pmaj7);

                // Bass
                ChordNamePitch bass = ChordNamePitch.createValid(
                        getGroup(matcher, BASS_STEP),
                        getGroup(matcher, BASS_ALTER));

                if ((firstDeg != null) && (kind.type != SUSPENDED_FOURTH)
                        && (kind.type != SUSPENDED_SECOND)) {
                    // Remove first degree
                    degrees.remove(firstDeg);
                }

                // Degrees within parentheses or not
                String parStr = getGroup(matcher, PARS);

                if (!parStr.isEmpty()) {
                    degrees.addAll(ChordDegree.createList(parStr, firstDeg));
                } else {
                    String noParStr = getGroup(matcher, NO_PARS);

                    if (!noParStr.isEmpty()) {
                        degrees.addAll(ChordDegree.createList(noParStr, firstDeg));
                    }
                }

                return new ChordStructure(root, kind, bass, degrees);
            }
        }

        logger.debug("No pattern match for chord text {}", value);

        return null;
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final Link link = lookupLink(system);

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, ChordNameRelation.class);
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

    //~ Inner Classes ------------------------------------------------------------------------------

    //-------//
    // Alter //
    //-------//
    public static class Alter
    {
        /** Alter class. */
        private static final String CLASS = "[" + FLAT + "b" + SHARP + "#" + "]";

        // For JAXB
        private Alter ()
        {
        }

        /**
         * Convert sharp/flat/empty sign to Integer.
         *
         * @param str the alteration sign
         * @return the Integer value, null if input string has unexpected value
         */
        private static Integer toAlter (String str)
        {
            return switch (str) {
                case SHARP, "#" -> 1;
                case FLAT, "b" -> -1;
                case "" -> 0;
                case null, default -> null;
            };
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

    //-------------//
    // ChordDegree //
    //-------------//
    public static class ChordDegree
    {
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

        // Needed for JAXB
        private ChordDegree ()
        {
            value = 0;
            alter = null;
            type = null;
            text = null;
        }

        public ChordDegree (int value,
                            Integer alter,
                            DegreeType type)
        {
            this(value, alter, type, "");
        }

        public ChordDegree (int value,
                            Integer alter,
                            DegreeType type,
                            String text)
        {
            this.value = value;
            this.alter = alter;
            this.type = type;
            this.text = text;
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

        //
        /**
         * Build a sequence of ChordDegree instances from the provided string
         *
         * @param str      the provided string, without parentheses, such as 7b13
         * @param dominant the chord dominant degree, if any, otherwise null
         * @return the list of degrees
         */
        public static List<ChordDegree> createList (String str,
                                                    ChordDegree dominant)
        {
            List<ChordDegree> degrees = new ArrayList<>();

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

                degrees.add(new ChordDegree(deg, alter, type, ""));
            }

            return degrees;
        }

        public static enum DegreeType
        {
            ADD,
            ALTER,
            SUBTRACT;
        }
    }

    //-----------//
    // ChordKind //
    //-----------//
    public static class ChordKind
    {
        /** Precise type of kind. (subset of the 33 Music XML values) */
        @XmlAttribute
        public final ChordType type;

        /** Flag to signal parentheses, if any. */
        @XmlAttribute
        @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
        public final boolean parentheses;

        /** Flag to signal use of symbol, if any. */
        @XmlAttribute
        @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
        public final boolean symbol;

        /** Exact display text for the chord kind. (For example min vs m) */
        @XmlAttribute
        public final String text;

        // For JAXB
        @SuppressWarnings("unused")
        private ChordKind ()
        {
            this.type = null;
            this.parentheses = false;
            this.symbol = false;
            this.text = null;
        }

        public ChordKind (ChordType type)
        {
            this(type, "", false, false);
        }

        public ChordKind (ChordType type,
                          String text)
        {
            this(type, text, false, false);
        }

        public ChordKind (ChordType type,
                          String text,
                          boolean symbol)
        {
            this(type, text, symbol, false);
        }

        public ChordKind (ChordType type,
                          String text,
                          boolean symbol,
                          boolean parentheses)
        {
            this.type = type;
            this.parentheses = parentheses;
            this.text = text;
            this.symbol = symbol;
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

            if (parentheses) {
                sb.append(" parens");
            }

            if (symbol) {
                sb.append(" symbol");
            }

            sb.append("}");

            return sb.toString();
        }

        /**
         * Try to create proper ChordKind object from a provided matcher, augmented
         * by dominant string if any.
         *
         * @param matcher  matcher on input string
         * @param dominant dominant information if any, empty string otherwise
         * @return ChordKind instance, or null if failed
         */
        private static ChordKind createValid (Matcher matcher,
                                              String dominant)
        {
            final String kindStr = getGroup(matcher, KIND);
            final String parStr = getGroup(matcher, PARS);
            final boolean parentheses = !parStr.isEmpty();

            // Check for suspended first
            final String susStr = getGroup(matcher, SUS);

            switch (susStr.toLowerCase()) {
                case "sus2" -> {
                    return new ChordKind(SUSPENDED_SECOND, kindStr, false, parentheses);
                }

                case "sus4" -> {
                    return new ChordKind(SUSPENDED_FOURTH, kindStr, false, parentheses);
                }
            }

            // Then check for other combinations
            final String str = //
                    standard(matcher, MIN) + standard(matcher, MAJ) + standard(matcher, AUG)
                            + standard(matcher, DIM) + standard(matcher, HDIM) + dominant;
            ChordType type = typeOf(str);

            // Special case for Triangle sign => maj7 rather than major
            if ((type == MAJOR) && getGroup(matcher, MAJ).equals(DELTA)) {
                type = MAJOR_SEVENTH;
            }

            // Use of symbol?
            final boolean symbol = getGroup(matcher, MAJ).equals(DELTA) //
                    || getGroup(matcher, MIN).equals("-") || getGroup(matcher, AUG).equals("+");

            return (type != null) ? new ChordKind(type, kindStr, symbol, parentheses) : null;
        }

        /**
         * Convert a provided string to proper ChordType value.
         *
         * @param str provided string, assumed to contain only 'standard' values
         * @return the corresponding type, or null if none found
         */
        private static ChordType typeOf (String str)
        {
            return switch (str) {
                case "", MAJ -> MAJOR;
                case MIN -> MINOR;
                case AUG -> AUGMENTED;
                case DIM -> DIMINISHED;
                case "7" -> DOMINANT;
                case MAJ + "7" -> MAJOR_SEVENTH;
                case MIN + "7" -> MINOR_SEVENTH;
                case DIM + "7" -> DIMINISHED_SEVENTH;
                case AUG + "7" -> AUGMENTED_SEVENTH;
                case HDIM -> HALF_DIMINISHED;
                case MIN + PMAJ7 -> MAJOR_MINOR;
                case MAJ + "6", "6" -> MAJOR_SIXTH;
                case MIN + "6" -> MINOR_SIXTH;
                case "9" -> DOMINANT_NINTH;
                case MAJ + "9" -> MAJOR_NINTH;
                case MIN + "9" -> MINOR_NINTH;
                case "11" -> DOMINANT_11_TH;
                case MAJ + "11" -> MAJOR_11_TH;
                case MIN + "11" -> MINOR_11_TH;
                case "13" -> DOMINANT_13_TH;
                case MAJ + "13" -> MAJOR_13_TH;
                case MIN + "13" -> MINOR_13_TH;

                default -> {
                    // Nota: Thanks to regexp match, this should not happen
                    logger.warn("ChordName. No kind type for {}", str);
                    yield null;
                }
            };
        }

        public static enum ChordType
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
    }

    //----------------//
    // ChordNamePitch //
    //----------------//
    @XmlAccessorType(XmlAccessType.NONE)
    public static class ChordNamePitch
    {
        /** Related step. */
        @XmlAttribute
        public final AbstractNoteInter.NoteStep step;

        /** Alteration, if any. */
        @XmlAttribute
        public final Integer alter;

        // For JAXB
        private ChordNamePitch ()
        {
            this.step = null;
            this.alter = null;
        }

        public ChordNamePitch (AbstractNoteInter.NoteStep step)
        {
            this(step, 0);
        }

        public ChordNamePitch (AbstractNoteInter.NoteStep step,
                               Integer alter)
        {
            this.step = step;
            this.alter = alter;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(step);

            sb.append(Alter.toString(alter));

            return sb.toString();
        }

        /**
         * Try to create a <code>ChordNamePitch</code> object from provided step and alter strings
         *
         * @param stepStr  provided step string
         * @param alterStr provided alteration string
         * @return ChordNamePitch instance, or null if failed
         */
        public static ChordNamePitch createValid (String stepStr,
                                                  String alterStr)
        {
            stepStr = stepStr.trim();
            alterStr = alterStr.trim();

            if (!stepStr.isEmpty()) {
                return new ChordNamePitch(
                        AbstractNoteInter.NoteStep.valueOf(stepStr),
                        Alter.toAlter(alterStr));
            } else {
                return null;
            }
        }
    }

    //----------------//
    // ChordStructure //
    //----------------//
    private static class ChordStructure
    {
        public final ChordNamePitch root;

        public final ChordKind kind;

        public final ChordNamePitch bass;

        public final List<ChordDegree> degrees;

        public ChordStructure (ChordNamePitch root,
                               ChordKind kind,
                               ChordNamePitch bass,
                               List<ChordDegree> degrees)
        {
            this.root = root;
            this.kind = kind;
            this.bass = bass;
            this.degrees = degrees;
        }
    }
}
