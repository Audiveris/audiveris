//----------------------------------------------------------------------------//
//                                                                            //
//                                 C h e c k                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.check;

import omr.util.Logger;

/**
 * Class <code>Check</code> encapsulates the <b>definition</b> of a check,
 * which can later be used on a whole population of objects.
 *
 * <p>The result of using a check on a given object is not recorded in this
 * class, but into the checked entity itself. </p>
 *
 * <p>Checks can be gathered in check suites. </p>
 *
 * @param <C> precise type of the objects to be checked
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class Check<C extends Checkable>
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(Check.class);

    /**
     * Indicates a negative result
     */
    public static final int RED = -1;

    /**
     * Indicates a non-concluding result
     */
    public static final int ORANGE = 0;

    /**
     * Indicates a positive result
     */
    public static final int GREEN = 1;

    //~ Instance fields --------------------------------------------------------

    // Specifies the FailureResult to be assigned to the Checkable object, if
    // the result of the check end in the RED range.
    private final FailureResult redResult;

    // Longer description, meant for tips
    private final String  description;

    // Short name for this test
    private final String  name;

    // Specifies if values are RED,ORANGE,GREEN (higher is better, covariant =
    // true) or GREEN,ORANGE,RED (lower is better, covariant = false)
    private final boolean covariant;
    private double        high;

    // Values that embrace the ORANGE range. Whatever the value of 'covariant',
    // we must always have low <= high
    private double low;

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Check //
    //-------//
    /**
     * Creates a new Check object.
     *
     * @param name short name for this check
     * @param description longer description
     * @param low lower bound of orange zone
     * @param high upper bound of orange zone
     * @param covariant true if higher is better, false otherwise
     * @param redResult result code to be assigned when result is RED
     */
    protected Check (String        name,
                     String        description,
                     double        low,
                     double        high,
                     boolean       covariant,
                     FailureResult redResult)
    {
        this.name = name;
        this.description = description;
        this.low = low;
        this.high = high;
        this.covariant = covariant;
        this.redResult = redResult;

        verifyRange();
    }

    /** For temporary compatibility */
    @Deprecated
    protected Check (String        name,
                     double        low,
                     double        high,
                     boolean       covariant,
                     FailureResult redResult)
    {
        this(name, null, low, high, covariant, redResult);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // isCovariant //
    //-------------//
    /**
     * Report the covariant flag
     *
     * @return the value of covariant flag
     */
    public boolean isCovariant ()
    {
        return covariant;
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report the related description
     *
     * @return the description assigned to this check
     */
    public String getDescription ()
    {
        return description;
    }

    //---------//
    // getHigh //
    //---------//
    /**
     * Report the higher bound value
     *
     * @return the high bound
     */
    public double getHigh ()
    {
        return high;
    }

    //--------//
    // getLow //
    //--------//
    /**
     * Report the lower bound value
     *
     * @return the low bound
     */
    public double getLow ()
    {
        return low;
    }

    //------------//
    // setLowHigh //
    //------------//
    /**
     * Allows to set the pair of low and high value. They are set in one shot to
     * allow the sanity check of 'low' less than or equal to 'high'
     *
     * @param low the new low value
     */
    public void setLowHigh (double low,
                            double high)
    {
        this.low = low;
        this.high = high;
        verifyRange();
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the related name
     *
     * @return the name assigned to this check
     */
    public String getName ()
    {
        return name;
    }

    //------//
    // pass //
    //------//
    /**
     * Actually run the check on the provided object, and return the result.  As
     * a side-effect, a check that totally fails (RED result) assigns this
     * failure into the candidate object.
     *
     * @param obj    the checkable object to be checked
     * @param result output for the result, or null
     * @param update true if obj is to be updated with the result
     *
     * @return the result composed of the numerical value, plus a flag ({@link
     * #RED}, {@link #ORANGE}, {@link #GREEN}) that characterizes the result of
     * passing the check on this object
     */
    public CheckResult pass (C           obj,
                             CheckResult result,
                             boolean     update)
    {
        if (result == null) {
            result = new CheckResult();
        }

        result.value = getValue(obj);

        if (covariant) {
            if (result.value < low) {
                if (update) {
                    obj.setResult(redResult);
                }

                result.flag = RED;
            } else if (result.value >= high) {
                result.flag = GREEN;
            } else {
                result.flag = ORANGE;
            }
        } else {
            if (result.value <= low) {
                result.flag = GREEN;
            } else if (result.value > high) {
                if (update) {
                    obj.setResult(redResult);
                }

                result.flag = RED;
            } else {
                result.flag = ORANGE;
            }
        }

        return result;
    }

    //----------//
    // toString //
    //----------//
    /**
     * report a readable description of this check
     */
    @Override
    public String toString ()
    {
        StringBuffer sb = new StringBuffer(128);
        sb.append("{Check ")
          .append(name);
        sb.append(" Covariant:")
          .append(covariant);
        sb.append(" Low:")
          .append(low);
        sb.append(" High:")
          .append(high);
        sb.append("}");

        return sb.toString();
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Method to be provided by any concrete subclass, in order to retrieve the
     * proper data value from the given object passed as a parameter.
     *
     * @param obj the object to be checked
     *
     * @return the data value relevant for the check
     */
    protected abstract double getValue (C obj);

    //-------------//
    // verifyRange //
    //-------------//
    private void verifyRange ()
    {
        if (low > high) {
            logger.severe("Illegal low-high range for " + this);
        }
    }
}
