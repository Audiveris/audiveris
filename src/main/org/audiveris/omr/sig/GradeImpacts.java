//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G r a d e I m p a c t s                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sig;

import org.audiveris.omr.glyph.Grades;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Abstract class {@code GradeImpacts} defines data that impact a resulting grade value.
 * <p>
 * It uses 3 parallel arrays for:
 * <ul>
 * <li>impact name
 * <li>impact value
 * <li>impact (relative) weight
 * </ul>
 *
 * @author Hervé Bitteur
 */
public abstract class GradeImpacts
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final NumberFormat nf1 = NumberFormat.getNumberInstance(Locale.US);

    static {
        nf1.setMaximumFractionDigits(1); // For a maximum of 1 decimal
    }

    private static final NumberFormat nf2 = NumberFormat.getNumberInstance(Locale.US);

    static {
        nf2.setMaximumFractionDigits(2); // For a maximum of 2 decimals
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Array of actual contributions. */
    protected final double[] impacts;

    /** Array of impacts name. */
    private final String[] names;

    /** Array of impacts weight. */
    private final double[] weights;

    /** Resulting grade. */
    protected double grade = -1;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicImpacts object.
     *
     * @param names   array of names
     * @param weights array of weights
     */
    public GradeImpacts (String[] names,
                         double[] weights)
    {
        if (names.length != weights.length) {
            throw new IllegalArgumentException("Arrays for names & weights have different lengths");
        }

        this.names = names;
        this.weights = weights;

        impacts = new double[names.length];
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report a string about impacts details
     *
     * @return string of details
     */
    public String getDump ()
    {
        final StringBuilder sb = new StringBuilder();

        return sb.toString();
    }

    /**
     * Report the global grade value from detailed impacts.
     *
     * @return the computed grade in range 0 .. 1
     */
    public double getGrade ()
    {
        if (grade == -1) {
            grade = computeGrade();
        }

        return grade;
    }

    /**
     * Report the value of the grade impact corresponding to index
     *
     * @param index the index of desired impact
     * @return the impact value
     */
    public double getImpact (int index)
    {
        return impacts[index];
    }

    /**
     * Set impact at provided index.
     *
     * @param index  index in impact array
     * @param impact value for specific impact
     */
    public final void setImpact (int index,
                                 double impact)
    {
        impacts[index] = GradeUtil.clamp(impact);
    }

    /**
     * Report the number of individual grade impacts.
     *
     * @return the count of impacts
     */
    public int getImpactCount ()
    {
        return impacts.length;
    }

    /**
     * Report the reduction ratio to be applied on intrinsic grade
     *
     * @return the reduction ratio to be applied
     */
    public double getIntrinsicRatio ()
    {
        return Grades.intrinsicRatio;
    }

    /**
     * Report the name of the grade impact corresponding to index
     *
     * @param index the index of desired impact
     * @return the impact name
     */
    public String getName (int index)
    {
        return names[index];
    }

    /**
     * Report the weight of the grade impact corresponding to index
     *
     * @param index the index of desired impact
     * @return the impact weight
     */
    public double getWeight (int index)
    {
        return weights[index];
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < getImpactCount(); i++) {
            if (sb.length() > 0) {
                sb.append(" ");
            }

            sb.append(nf1.format(getWeight(i))).append('/')
                    .append(getName(i)).append(':')
                    .append(nf2.format(getImpact(i)));
        }

        return sb.toString();
    }

    //--------------//
    // computeGrade //
    //--------------//
    /**
     * Compute resulting grade from all impacts.
     *
     * @return the resulting grade
     */
    protected double computeGrade ()
    {
        double global = 1d;
        double totalWeight = 0d;

        for (int i = 0; i < getImpactCount(); i++) {
            double weight = getWeight(i);
            double impact = getImpact(i);
            totalWeight += weight;

            if (impact == 0) {
                global = 0;
            } else if (weight != 0) {
                global *= Math.pow(impact, weight);
            }
        }

        return getIntrinsicRatio() * Math.pow(global, 1 / totalWeight);
    }
}
