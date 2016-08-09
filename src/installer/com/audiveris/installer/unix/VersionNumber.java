//----------------------------------------------------------------------------//
//                                                                            //
//                           V e r s i o n N u m b e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer.unix;

import static com.audiveris.installer.RegexUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class {@code VersionNumber} handles the version number of a Debian
 * package, and especially version comparison.
 * <p>
 * Based on
 * http://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Version
 * <p>
 * Pattern is [epoch:]upstream_version[-debian_revision]
 * epoch : integer
 * upstream_version : [A-Za-z0-9.+-:~] starts with a digit
 * debian_revision : [A-Za-z0-9.+~]
 *
 * @author Hervé Bitteur
 */
public class VersionNumber
    implements Comparable<VersionNumber>
{
    //~ Static fields/initializers ---------------------------------------------

    
    private static final Logger logger = LoggerFactory.getLogger(
        VersionNumber.class);

    /** Regex for epoch. */
    private static final Pattern epochPattern = Pattern.compile("^[0-9]+$");

    /** Regex for version. */
    private static final Pattern versionPattern = Pattern.compile(
        "^[0-9][A-Za-z0-9\\.+-:~]*$");

    /** Regex for revision. */
    private static final Pattern revisionPattern = Pattern.compile(
        "^[A-Za-z0-9\\.+~]+$");

    /** Regex for parsing version (and revision) string. */
    private static final String LETTERS = "letters";
    private static final String  DIGITS = "digits";
    private static final String  REM = "rem";
    private static final Pattern lettersPattern = Pattern.compile(
        "^" + group(LETTERS, "[^0-9]*") + group(REM, ".*") + "$");
    private static final Pattern digitsPattern = Pattern.compile(
        "^" + group(DIGITS, "[0-9]*") + group(REM, ".*") + "$");

    //~ Instance fields --------------------------------------------------------

    /** The original string (for debugging). */
    private final String source;

    /** The epoch part, if any. */
    private final int epoch;

    /** The upstream_version (mandatory). */
    private final String version;

    /** The debian_revision, if any. */
    private final String revision;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // VersionNumber //
    //---------------//
    /**
     * Creates a new VersionNumber object.
     *
     * @param source the source string to be parsed
     */
    public VersionNumber (String source)
    {
        this.source = source;

        String src = source.trim();

        // Epoch is a single (generally small) unsigned integer. 
        // It may be omitted, in which case zero is assumed.
        // If it is omitted then the upstream_version may not contain any colons. 
        int colon = src.indexOf(":");

        if (colon != -1) {
            String epochStr = src.substring(0, colon);
            checkEpoch(epochStr);
            epoch = Integer.parseInt(epochStr);
            src = src.substring(colon + 1);
        } else {
            epoch = 0;
        }

        // Break the version number apart at the last hyphen in the string 
        // (if there is one) to determine the upstream_version and debian_revision 
        int hyphen = src.lastIndexOf("-");

        if (hyphen != -1) {
            version = src.substring(0, hyphen);
            checkVersion(version);
            revision = src.substring(hyphen + 1);
            checkRevision(revision);
        } else {
            version = src;
            checkVersion(version);
            revision = "0";
        }

        logger.debug("VersionNumber: {}", this);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // compareTo //
    //-----------//
    @Override
    public int compareTo (VersionNumber that)
    {
        // 1/ epoch
        int res = Integer.compare(this.epoch, that.epoch);

        if (res != 0) {
            return res;
        }

        // 2/ version
        res = compareStrings(this.version, that.version);

        if (res != 0) {
            return res;
        }

        // 3/ revision
        return compareStrings(this.revision, that.revision);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{VersionNumber");

        sb.append(" source=")
          .append(source);
        sb.append(" epoch=")
          .append(epoch);
        sb.append(" version=")
          .append(version);
        sb.append(" revision=")
          .append(revision);

        sb.append("}");

        return sb.toString();
    }

    //------------//
    // checkEpoch //
    //------------//
    private void checkEpoch (String str)
    {
        Matcher matcher = epochPattern.matcher(str);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Illegal epoch string " + str);
        }
    }

    //--------------//
    // checkVersion //
    //--------------//
    private void checkRevision (String str)
    {
        Matcher matcher = revisionPattern.matcher(str);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                "Illegal revision string " + str);
        }
    }

    //--------------//
    // checkVersion //
    //--------------//
    private void checkVersion (String str)
    {
        Matcher matcher = versionPattern.matcher(str);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Illegal version string " + str);
        }
    }

    //---------------//
    // compareDigits //
    //---------------//
    private int compareDigits (String oneStr,
                               String twoStr)
    {
        /*
         * The numerical values of
         * these two parts are compared, and any difference found is returned as
         * the result of the comparison. For these purposes an empty string
         * (which can only occur at the end of one or both version strings
         * being compared) counts as zero.
         */
        int one = oneStr.isEmpty() ? 0 : Integer.parseInt(oneStr);
        int two = twoStr.isEmpty() ? 0 : Integer.parseInt(twoStr);

        return Integer.compare(one, two);
    }

    //----------------//
    // compareLetters //
    //----------------//
    private int compareLetters (String oneStr,
                                String twoStr)
    {
        /*
         * The lexical comparison is a comparison of ASCII values
         * modified so that all the letters sort earlier than all the
         * non-letters and so that a tilde sorts before anything, even the end
         * of a part. For example, the following parts are in sorted order from
         * earliest to latest: ~~, ~~a, ~, the empty part, a.
         */
        final int max = Math.max(oneStr.length(), twoStr.length());
        int res;

        for (int i = 0; i < max; i++) {
            // We code empty parts with spaces (which are not allowed in source)
            char one = (i < oneStr.length()) ? oneStr.charAt(i) : ' ';
            char two = (i < twoStr.length()) ? twoStr.charAt(i) : ' ';

            // Tilde über alles (even empty part, which is coded as space)
            if (one == '~') {
                if (two != '~') {
                    return -1;
                }
            } else {
                if (two == '~') {
                    return 1;
                }
            }

            // Empty (space) before everything (except tilde)
            if (one == ' ') {
                return -1;
            }

            if (two == ' ') {
                return 1;
            }

            // Letters before non-letters
            if (Character.isLetter(one)) {
                if (Character.isLetter(two)) {
                    res = one - two;

                    if (res != 0) {
                        return res;
                    }
                } else {
                    return -1;
                }
            } else {
                if (Character.isLetter(two)) {
                    return 1;
                } else {
                    res = one - two;

                    if (res != 0) {
                        return res;
                    }
                }
            }
        }

        return 0;
    }

    //----------------//
    // compareStrings //
    //----------------//
    private int compareStrings (String one,
                                String two)
    {
        while (true) {
            /*
             * First the initial part of each string consisting entirely of
             * non-digit characters is determined. These two parts (one of which
             * may be empty) are compared lexically. If a difference is found it is
             * returned.
             */
            final String oneLetters;
            Matcher oneLettersMatcher = lettersPattern.matcher(one);

            if (oneLettersMatcher.matches()) {
                oneLetters = getGroup(oneLettersMatcher, LETTERS);
                one = getGroup(oneLettersMatcher, REM);
            } else {
                oneLetters = "";
            }

            final String twoLetters;
            Matcher      twoLettersMatcher = lettersPattern.matcher(two);

            if (twoLettersMatcher.matches()) {
                twoLetters = getGroup(twoLettersMatcher, LETTERS);
                two = getGroup(twoLettersMatcher, REM);
            } else {
                twoLetters = "";
            }

            int res = compareLetters(oneLetters, twoLetters);

            if (res != 0) {
                return res;
            }

            /*
             * Then the initial part of the remainder of each string which consists
             * entirely of digit characters is determined. The numerical values of
             * these two parts are compared, and any difference found is returned as
             * the result of the comparison.
             */
            final String oneDigits;
            Matcher oneDigitsMatcher = digitsPattern.matcher(one);

            if (oneDigitsMatcher.matches()) {
                oneDigits = getGroup(oneDigitsMatcher, DIGITS);
                one = getGroup(oneDigitsMatcher, REM);
            } else {
                oneDigits = "";
            }

            final String twoDigits;
            Matcher      twoDigitsMatcher = digitsPattern.matcher(two);

            if (twoDigitsMatcher.matches()) {
                twoDigits = getGroup(twoDigitsMatcher, DIGITS);
                two = getGroup(twoDigitsMatcher, REM);
            } else {
                twoDigits = "";
            }

            res = compareDigits(oneDigits, twoDigits);

            if (res != 0) {
                return res;
            }

            /*
             * These two steps (comparing and removing initial non-digit strings and
             * initial digit strings) are repeated until a difference is found or
             * both strings are exhausted.
             */
            if (one.isEmpty() && two.isEmpty()) {
                return 0;
            }
        }
    }
}
