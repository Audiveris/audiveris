//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           C h e c k                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.check;

import omr.constant.Constant;

import omr.sig.Grades;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Check} encapsulates the <b>definition</b> of a check,
 * which can later be used on a whole population of objects.
 * <p>
 * Checks are generally gathered in {@link CheckSuite} instances. </p>
 * <p>
 * The strategy is the following:<ul>
 * <li>A successful individual check may eventually result in an interpretation checked object (if
 * the suite of checks ends with an acceptable grade).</li>
 * <li>Any failed individual check triggers the immediate end of the containing suite but records
 * this failure in the checked object itself, for later review.</li>
 * </ul>
 *
 * @param <C> precise type of the objects to be checked
 *
 * @author Hervé Bitteur
 */
public abstract class Check<C>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Check.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * Specifies the Failure to be assigned to the Checkable object,
     * when the result of this individual check is not acceptable.
     */
    private final Failure failure;

    /** Short name for this test. */
    private final String name;

    /** Longer description, meant for tips. */
    private final String description;

    /** Lower bound for value range. */
    private Constant.Double low;

    /** Higher bound for value range. */
    private Constant.Double high;

    /** Covariant: higher is better, contravariant: lower is better. */
    private final boolean covariant;

    //~ Constructors -------------------------------------------------------------------------------
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
                     Failure redResult)
    {
        this.name = name;
        this.description = description;
        this.low = low;
        this.high = high;
        this.covariant = covariant;
        this.failure = redResult;

        verifyRange();
    }

    //~ Methods ------------------------------------------------------------------------------------
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
     * Actually run the check on the provided object, and return the
     * result.
     * As a side-effect, a check that totally fails (RED result) records this
     * failure within the candidate object itself.
     *
     * @param checkable the checkable object to be checked
     * @param result    output for the result, or null
     * @return the result
     */
    public CheckResult pass (C checkable,
                             CheckResult result)
    {
        if (result == null) {
            result = new CheckResult();
        }

        final double range = high.getValue() - low.getValue();
        result.value = getValue(checkable);

        if (covariant) {
            if (result.value < low.getValue()) {
                result.grade = 0;
            } else if (result.value >= high.getValue()) {
                result.grade = 1;
            } else {
                result.grade = Grades.clamp((result.value - low.getValue()) / range);
            }
        } else {
            if (result.value > high.getValue()) {
                result.grade = 0;
            } else if (result.value <= low.getValue()) {
                result.grade = 1;
            } else {
                result.grade = Grades.clamp(((high.getValue() - result.value) / range));
            }
        }

        return result;
    }

    //------------//
    // setLowHigh //
    //------------//
    /**
     * Allows to set the pair of low and high value.
     * They are set in one shot to allow the " low is less than or equals high "
     * sanity check.
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
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(128);
        sb.append("{Check ").append(name);
        sb.append(" Covariant:").append(covariant);
        sb.append(" Low:").append(low);
        sb.append(" High:").append(high);
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
                    low.getValue(),
                    high.getValue(),
                    this);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Grade //
    //-------//
    /**
     * A subclass of Constant.Double, meant to store a check result grade.
     */
    public static class Grade
            extends Constant.Double
    {
        //~ Constructors ---------------------------------------------------------------------------

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
