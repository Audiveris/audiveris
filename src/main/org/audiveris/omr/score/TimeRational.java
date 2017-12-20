//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T i m e R a t i o n a l                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
 * Class {@code TimeRational} is a marshallable and non-mutable structure,
 * meant to carry the actual rational members of a TimeSignature.
 * <p>
 * For example, (3/4) and (6/8) share the same rational value, but with different actual members.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "time-rational")
@XmlType(propOrder = {
    "num", "den"}
)
public class TimeRational
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TimeRational.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The actual numerator. */
    @XmlAttribute
    public final int num;

    /** The actual denominator. */
    @XmlAttribute
    public final int den;

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

    /** Zero-argument constructor to please JAXB. */
    private TimeRational ()
    {
        den = num = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // decode //
    //--------//
    /**
     * Decode a string expected to contain one TimeRational value, formatted as num / den.
     *
     * @param str the string to decode
     * @return the TimeRational value if successful
     */
    public static TimeRational decode (String str)
    {
        final String[] tokens = str.split("\\s*/\\s*");

        if (tokens.length == 2) {
            int num = Integer.decode(tokens[0].trim());
            int den = Integer.decode(tokens[1].trim());

            return new TimeRational(num, den);
        } else {
            logger.warn("Illegal TimeRational value: ", str);

            return null;
        }
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
    public Rational getValue ()
    {
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
        final List<TimeRational> list = new ArrayList<TimeRational>();
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return num + "/" + den;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    public static class Adapter
            extends XmlAdapter<String, TimeRational>
    {
        //~ Methods --------------------------------------------------------------------------------

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
