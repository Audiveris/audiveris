//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P a r t N a m e s                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.score;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class <code>PartNames</code> tells whether two part names, as read by OCR in the left
 * margin of systems, designate the same part.
 * <p>
 * A part is generally named in full on the first system and abbreviated on the following ones
 * ("Violin I" then "Vln. I"), and OCR may read a letter as a similar digit ("F1." for "Fl.").
 * Each name is reduced to its letters and its instrument number, if any.
 * Two names match when:
 * <ul>
 * <li>the shorter letters abbreviate the longer ones: same first letter, then a subsequence
 * ("vln" in "violin", "vc" in "violoncello"), or abbreviate another name of the same
 * instrument ("vc" for "cello"),
 * <li>and their numbers, when both names have one, are equal ("Vln. II" does not match
 * "Violin I").
 * </ul>
 * A name with no letters at all ("13." read for "B.") is not readable.
 */
public abstract class PartNames
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9]+");

    /** Digits that OCR reads for similar letters within a word ("F1." for "Fl."). */
    private static final String OCR_DIGITS = "1058";

    private static final String OCR_LETTERS = "losb";

    /** Roman numerals of instrument numbers. */
    private static final List<String> ROMANS = Arrays.asList("i", "ii", "iii", "iv", "v", "vi");

    /** Other names of an instrument, whose abbreviations do not abbreviate the name used. */
    private static final String[][] ALIASES = {
            { "cello", "violoncello" },
            { "bassoon", "fagotto", "fagott" },
            { "horn", "corno" },
            { "doublebass", "contrabass", "contrabasso", "kontrabass" } };

    //~ Constructors -------------------------------------------------------------------------------

    private PartNames ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // abbreviates //
    //-------------//
    /**
     * Tell whether the short letters abbreviate the long ones: same first letter, then the
     * other letters in the same order.
     */
    private static boolean abbreviates (String shortOne,
                                        String longOne)
    {
        if (shortOne.isEmpty() || longOne.isEmpty() || shortOne.charAt(0) != longOne.charAt(0)) {
            return false;
        }

        int j = 1;

        for (int i = 1; i < shortOne.length(); i++) {
            j = longOne.indexOf(shortOne.charAt(i), j) + 1;

            if (j == 0) {
                return false;
            }
        }

        return true;
    }

    //-------//
    // forms //
    //-------//
    /**
     * Report the readings of a name's letters, each followed by the other names of the same
     * instrument.
     */
    private static List<String> forms (List<String> readings)
    {
        final List<String> forms = new ArrayList<>();

        for (String letters : readings) {
            forms.add(letters);

            for (String[] aliases : ALIASES) {
                if (aliases[0].equals(letters)) {
                    forms.addAll(Arrays.asList(aliases).subList(1, aliases.length));
                }
            }
        }

        return forms;
    }

    //------------//
    // isReadable //
    //------------//
    /**
     * Tell whether the provided part name holds some letters.
     *
     * @param name the part name as read, perhaps null
     * @return true if readable
     */
    public static boolean isReadable (String name)
    {
        return Label.of(name) != null;
    }

    //-----//
    // fit //
    //-----//
    /**
     * Report how well the two provided part names match.
     * <p>
     * 0 when they do not designate the same part. Otherwise 1, plus 1 when both have the
     * same instrument number, plus up to 1 as the shorter letters cover more of the longer
     * ("vln" covers half of "violin", a quarter of "violoncello"), for three letters or
     * more: two letters ("Vc.", or "V0." misread for it) are too few to tell instruments
     * apart that way.
     *
     * @param a one name, perhaps null
     * @param b another name, perhaps null
     * @return the fit, 0 if either is not readable or they do not match
     */
    public static double fit (String a,
                              String b)
    {
        final Label la = Label.of(a);
        final Label lb = Label.of(b);

        if ((la == null) || (lb == null)) {
            return 0;
        }

        if ((la.number != null) && (lb.number != null) && !la.number.equals(lb.number)) {
            return 0;
        }

        double cover = -1;

        for (String fa : forms(la.letters)) {
            for (String fb : forms(lb.letters)) {
                final boolean aShorter = fa.length() <= fb.length();
                final String shortOne = aShorter ? fa : fb;
                final String longOne = aShorter ? fb : fa;

                if (abbreviates(shortOne, longOne)) {
                    final double c = (shortOne.length() >= 3)
                            ? (0.99 * shortOne.length()) / longOne.length() : 0;
                    cover = Math.max(cover, c);
                }
            }
        }

        if (cover < 0) {
            return 0;
        }

        final boolean sameNumber = (la.number != null) && la.number.equals(lb.number);

        return 1 + (sameNumber ? 1 : 0) + cover;
    }

    //-------//
    // match //
    //-------//
    /**
     * Tell whether the two provided part names designate the same part.
     *
     * @param a one name, perhaps null
     * @param b another name, perhaps null
     * @return true if both are readable and match
     */
    public static boolean match (String a,
                                 String b)
    {
        return fit(a, b) > 0;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-------//
    // Label //
    //-------//
    /**
     * The letters and the instrument number of a part name.
     */
    private static class Label
    {
        /**
         * The letters as read and, if different, with an upper-case I within a word read as
         * l ("FI." for "Fl."), in lower case.
         */
        final List<String> letters;

        final Integer number;

        Label (List<String> letters,
               Integer number)
        {
            this.letters = letters;
            this.number = number;
        }

        /**
         * Reduce a part name to its letters and its number.
         *
         * @param name the part name, perhaps null
         * @return the label, or null if the name holds no letters
         */
        static Label of (String name)
        {
            if (name == null) {
                return null;
            }

            final List<String> words = new ArrayList<>();
            final Matcher matcher = WORD.matcher(name);

            while (matcher.find()) {
                words.add(matcher.group());
            }

            // The instrument number: a last word made of digits or a roman numeral
            Integer number = null;

            if (words.size() > 1) {
                final int last = words.size() - 1;
                String word = words.get(last).toLowerCase(Locale.ROOT);

                if (word.equals("l")) {
                    word = "i"; // A lone l read for the numeral I
                }

                if (word.matches("[0-9]+")) {
                    number = Integer.valueOf(word);
                    words.remove(last);
                } else if (ROMANS.contains(word)) {
                    number = 1 + ROMANS.indexOf(word);
                    words.remove(last);
                }
            }

            final List<String> readings = new ArrayList<>();

            for (boolean misread : new boolean[] { false, true }) {
                final StringBuilder letters = new StringBuilder();

                for (String word : words) {
                    if (misread) {
                        word = word.charAt(0) + word.substring(1).replace('I', 'l');
                    }

                    word = word.toLowerCase(Locale.ROOT);

                    if (word.matches(".*[a-z].*")) {
                        for (char c : word.toCharArray()) {
                            final int digit = OCR_DIGITS.indexOf(c);
                            letters.append((digit == -1) ? c : OCR_LETTERS.charAt(digit));
                        }
                    } else {
                        letters.append(word);
                    }
                }

                final String reading = letters.toString();

                if (reading.matches("[a-z]+") && !readings.contains(reading)) {
                    readings.add(reading);
                }
            }

            return readings.isEmpty() ? null : new Label(readings, number);
        }
    }
}
