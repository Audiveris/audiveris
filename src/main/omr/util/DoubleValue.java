//----------------------------------------------------------------------------//
//                                                                            //
//                           D o u b l e V a l u e                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import javax.xml.bind.annotation.XmlValue;

/**
 * Class {@code DoubleValue} is a "poor man" version of java.lang.Double,
 * when we need a non-final class (whereas Double is declared as final)
 *
 * @author Hervé Bitteur
 */
public class DoubleValue
{
    //~ Instance fields --------------------------------------------------------

    /** The underlying double value */
    @XmlValue
    protected final double value;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // DoubleValue //
    //-------------//
    /**
     * Creates a new DoubleValue object.
     *
     * @param value a double value
     */
    public DoubleValue (double value)
    {
        this.value = value;
    }

    //-------------//
    // DoubleValue //
    //-------------//
    /**
     * Creates a new DoubleValue object.
     *
     * @param value a Double value (note the initial capital D)
     */
    public DoubleValue (Double value)
    {
        this.value = value.doubleValue();
    }

    //-------------//
    // DoubleValue //
    //-------------//
    /**
     * Creates a new DoubleValue object.
     *
     * @param str the string representation of the value
     */
    public DoubleValue (String str)
    {
        this(Double.valueOf(str));
    }

    //-------------//
    // DoubleValue //
    //-------------//
    /**
     * Creates a new DoubleValue object, initialized at zero. Meant for JAXB
     */
    private DoubleValue ()
    {
        this(0d);
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // doubleValue //
    //-------------//
    /**
     * Returns the {@code double} value of this object.
     *
     * @return the {@code double} value represented by this object
     */
    public double doubleValue ()
    {
        return value;
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof DoubleValue) {
            return ((DoubleValue) obj).value == value;
        } else {
            return false;
        }
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 5;
        hash = (83 * hash)
               + (int) (Double.doubleToLongBits(this.value)
                        ^ (Double.doubleToLongBits(this.value) >>> 32));

        return hash;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return Double.toString(value);
    }
}
