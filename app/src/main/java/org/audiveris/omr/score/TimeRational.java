//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T i m e R a t i o n a l                                     //
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
package org.audiveris.omr.score;

import org.audiveris.omr.math.Rational;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class <code>TimeRational</code> carries the actual rational members
 * (beats/beat-type) of any time signature.
 * <p>
 * Let's take the example of '3/4' vs '6/8' time signatures:
 * <ul>
 * <li>They share the same mathematical (reduced) <b>rational</b> value: 3/4
 * <li>But they represent different (non-reduced) <b>time rational</b> values
 * </ul>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "time-rational")
@XmlType(propOrder =
{ "num", "den" })
public class TimeRational
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TimeRational.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * The non-reduced numerator indicates the number of beats.
     * <p>
     * For example:
     * <ul>
     * <li>3 for 3/4 time signature
     * <li>6 for 6/8 time signature
     * <li>4 for C ('common' time signature, worth 4/4)
     * </ul>
     */
    @XmlAttribute(name = "num")
    public final int num;

    /**
     * The non-reduced denominator indicates the beat unit.
     * <p>
     * For example:
     * <ul>
     * <li>4 for 3/4 time signature
     * <li>8 for 6/8 time signature
     * <li>2 for 'common-cut' time signature, worth 2/2
     * </ul>
     */
    @XmlAttribute(name = "den")
    public final int den;

    /** Zero-argument constructor to please JAXB. */
    private TimeRational ()
    {
        den = num = 0;
    }

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new TimeRational object.
     *
     * @param num the actual numerator
     * @param den the actual denominator
     */
    public TimeRational (int num,
                         int den)
    {
        this.num = num;
        this.den = den;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // duplicate //
    //-----------//
    public TimeRational duplicate ()
    {
        return new TimeRational(num, den);
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (!(obj instanceof TimeRational)) {
            return false;
        } else {
            TimeRational that = (TimeRational) obj;

            return (this.num == that.num) && (this.den == that.den);
        }
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the rational value of this time signature.
     * <p>
     * Rational value of (2,4) is 1/2
     *
     * @return rational value or null
     */
    public Rational getValue ()
    {
        if (den == 0) {
            return null;
        }

        return new Rational(num, den);
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (97 * hash) + this.num;
        hash = (97 * hash) + this.den;

        return hash;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return num + "/" + den;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------//
    // decode //
    //--------//
    /**
     * Decode a string expected to contain one TimeRational value, formatted as "num / den".
     *
     * @param str the string to decode
     * @return the TimeRational value if successful
     * @throws IllegalArgumentException if numerator or denominator outside [0..99] range.
     */
    public static TimeRational decode (String str)
    {
        final String[] tokens = str.split("\\s*/\\s*");

        return switch (tokens.length) {
            case 2 -> {
                final int num = Integer.decode(tokens[0].trim());
                if ((num < 0) || (num > 99)) {
                    throw new IllegalArgumentException(
                            "numerator " + num + " outside [0..99] range");
                }

                final int den = Integer.decode(tokens[1].trim());
                if ((den < 0) || (den > 99)) {
                    throw new IllegalArgumentException(
                            "denominator " + num + " outside [0..99] range");
                }

                yield new TimeRational(num, den);
            }

            default -> throw new NumberFormatException(str);
        };
    }

    //-------------//
    // parseValues //
    //-------------//
    /**
     * Convenient method to parse a string of TimeRational values, separated by commas.
     *
     * @param str the string to parse
     * @return the sequence of TimeRational values decoded
     */
    public static List<TimeRational> parseValues (String str)
    {
        final List<TimeRational> list = new ArrayList<>();
        final String[] tokens = str.split("\\s*,\\s*");

        for (String token : tokens) {
            String trimmedToken = token.trim();

            if (!trimmedToken.isEmpty()) {
                TimeRational val = decode(trimmedToken);

                if (val != null) {
                    list.add(val);
                }
            }
        }

        return list;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-------------//
    // JaxbAdapter //
    //-------------//
    /**
     * JAXB adapter for a TimeRational.
     */
    public static class JaxbAdapter
            extends XmlAdapter<String, TimeRational>
    {
        @Override
        public String marshal (TimeRational val)
            throws Exception
        {
            if (val == null) {
                return null;
            }

            return val.toString();
        }

        @Override
        public TimeRational unmarshal (String str)
            throws Exception
        {
            if (str == null) {
                return null;
            }

            return decode(str);
        }
    }
}
