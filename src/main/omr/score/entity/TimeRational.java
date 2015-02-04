//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T i m e R a t i o n a l                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.math.Rational;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code TimeRational} is a marshallable and non-mutable structure,
 * meant to carry the actual rational members of a TimeSignature.
 * <p>
 * For example, (3/4) and (6/8) share the same rational value, but with different actual members.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "rational")
public class TimeRational
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TimeRational.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The actual denominator. */
    @XmlAttribute
    public final int den;

    /** The actual numerator. */
    @XmlAttribute
    public final int num;

    //~ Constructors -------------------------------------------------------------------------------
    //--------------//
    // TimeRational //
    //--------------//
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

    //--------------//
    // TimeRational //
    //--------------//
    /** Zero-argument constructor to please JAXB */
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
            logger.warn("Illegal rational value: ", str);

            return null;
        }
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return num + "/" + den;
    }
}
