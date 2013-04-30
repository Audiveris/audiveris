//----------------------------------------------------------------------------//
//                                                                            //
//                                 C h e c k                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.check;

import omr.constant.Constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Check} encapsulates the <b>definition</b> of a check,
 * which can later be used on a whole population of objects.
 *
 * <p>The result of using a check on a given object is not recorded in this
 * class, but into the checked entity itself. </p>
 *
 * <p>Checks can be gathered in check suites. </p>
 *
 * @param <C> precise type of the objects to be checked
 *
 * @author Hervé Bitteur
 */
public abstract class Check<C extends Checkable>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Check.class);

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
    /**
     * Specifies the FailureResult to be assigned to the Checkable object, if
     * the result of the check end in the RED range.
     */
    private final FailureResult redResult;

    /** Longer description, meant for tips */
    private final String description;

    /** Short name for this test */
    private final String name;

    /**
     * Specifies if values are RED,ORANGE,GREEN (higher is better, covariant =
     * true) or GREEN,ORANGE,RED (lower is better, covariant = false)
     */
    private final boolean covariant;

    /**
     * Lower bound for ORANGE range. Whatever the value of 'covariant', we must
     * always have low <= high
     */
    private Constant.Double low;

    /**
     * Higher bound for ORANGE range. Whatever the value of 'covariant', we must
     * always have low <= high
     */
    private Constant.Double high;

    //~ Constructors -----------------------------------------------------------
    //-------//
    // Check //
    //-------//
    /**
     * Creates a new Check object.
     *
     * @param name        short name for this check
     * @param description longer description
     * @param low         lower bound of orange zone
     * @param high        upper bound of orange zone
     * @param covariant   true if higher is better, false otherwise
     * @param redResult   result code to be assigned when result is RED
     */
    protected Check (String name,
                     String description,
                     Constant.Double low,
                     Constant.Double high,
                     boolean covariant,
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

    //~ Methods ----------------------------------------------------------------
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
        return high.getValue();
    }

    //-----------------//
    // getHighConstant //
    //-----------------//
    /**
     * Report the higher bound constant
     *
     * @return the high bound constant
     */
    public Constant.Double getHighConstant ()
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
        return low.getValue();
    }

    //----------------//
    // getLowConstant //
    //----------------//
    /**
     * Report the lower bound constant
     *
     * @return the low bound constant
     */
    public Constant.Double getLowConstant ()
    {
        return low;
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

    //------//
    // pass //
    //------//
    /**
     * Actually run the check on the provided object, and return the result. As
     * a side-effect, a check that totally fails (RED result) assigns this
     * failure into the candidate object.
     *
     * @param obj    the checkable object to be checked
     * @param result output for the result, or null
     * @param update true if obj is to be updated with the result
     *
     * @return the result composed of the numerical value, plus a flag ({@link
     * #RED}, {@link #ORANGE}, {@link #GREEN}) that characterizes the result of
     *         passing the check on this object
     */
    public CheckResult pass (C obj,
                             CheckResult result,
                             boolean update)
    {
        if (result == null) {
            result = new CheckResult();
        }

        result.value = getValue(obj);

        if (covariant) {
            if (result.value < low.getValue()) {
                if (update) {
                    obj.setResult(redResult);
                }

                result.flag = RED;
            } else if (result.value >= high.getValue()) {
                result.flag = GREEN;
            } else {
                result.flag = ORANGE;
            }
        } else {
            if (result.value <= low.getValue()) {
                result.flag = GREEN;
            } else if (result.value > high.getValue()) {
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

    //------------//
    // setLowHigh //
    //------------//
    /**
     * Allows to set the pair of low and high value. They are set in one shot to
     * allow the sanity check of 'low' less than or equal to 'high'
     *
     * @param low  the new low value
     * @param high the new high value
     */
    public void setLowHigh (Constant.Double low,
                            Constant.Double high)
    {
        this.low = low;
        this.high = high;
        verifyRange();
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
        StringBuilder sb = new StringBuilder(128);
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
        if (low.getValue() > high.getValue()) {
            logger.error(
                    "Illegal low {} high {} range for {}",
                    low.getValue(), high.getValue(), this);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-------//
    // Grade //
    //-------//
    /**
     * A subclass of Constant.Double, meant to store a check result grade.
     */
    public static class Grade
            extends Constant.Double
    {
        //~ Constructors -------------------------------------------------------

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public Grade (double defaultValue,
                      java.lang.String description)
        {
            super("Grade", defaultValue, description);
        }
    }
}
