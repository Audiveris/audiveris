//----------------------------------------------------------------------------//
//                                                                            //
//                          T i m e R a t i o n a l                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.math.Rational;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code TimeRational} is a marshallable and non-mutable
 * structure, meant to carry the actual rational members of a
 * TimeSignature.
 * <p>Note for example that (3/4) and (6/8) share the same rational value,
 * but with different actual members.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "rational")
public class TimeRational
{
    //~ Instance fields --------------------------------------------------------

    /** The actual denominator */
    @XmlAttribute
    public final int den;

    /** The actual numerator */
    @XmlAttribute
    public final int num;

    //~ Constructors -----------------------------------------------------------
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
    /** To please JAXB */
    private TimeRational ()
    {
        den = num = 0;
    }

    //~ Methods ----------------------------------------------------------------
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
    /** {@inheritDoc } */
    @Override
    public String toString ()
    {
        return num + "/" + den;
    }
}
