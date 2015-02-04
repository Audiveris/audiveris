//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B a s i c I m p a c t s                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

/**
 * Class {@code BasicImpacts} is an array-based implementation of {@link GradeImpacts}
 * interface.
 *
 * @author Hervé Bitteur
 */
public abstract class BasicImpacts
        extends AbstractImpacts
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final String[] names;

    private final double[] weights;

    private final double[] impacts;

    protected double grade = -1;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicImpacts object.
     *
     * @param names   array of names
     * @param weights array of weights
     */
    public BasicImpacts (String[] names,
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
    @Override
    public String getDump ()
    {
        final StringBuilder sb = new StringBuilder();

        return sb.toString();
    }

    @Override
    public double getGrade ()
    {
        if (grade == -1) {
            grade = computeGrade();
        }

        return grade;
    }

    @Override
    public double getImpact (int index)
    {
        return impacts[index];
    }

    @Override
    public int getImpactCount ()
    {
        return impacts.length;
    }

    @Override
    public String getName (int index)
    {
        return names[index];
    }

    @Override
    public double getWeight (int index)
    {
        return weights[index];
    }

    public void setImpact (int index,
                           double impact)
    {
        impacts[index] = Grades.clamp(impact);
    }
}
