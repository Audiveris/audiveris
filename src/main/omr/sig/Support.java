//----------------------------------------------------------------------------//
//                                                                            //
//                                 S u p p o r t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

/**
 * Interface {@code Support} is a relation between interpretation
 * instances (of different glyph instances) that support one another.
 * Typical example is a mutual support between a stem and a note head, or
 * between a stem and a beam.
 *
 * @author Hervé Bitteur
 */
public interface Support
        extends Relation
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report details about the final relation grade
     *
     * @return the grade details
     */
    GradeImpacts getImpacts ();

    /**
     * Report the support ratio brought by this support relation.
     * Note: the value could depend on relation direction, but we use the
     * same value in both directions, for the time being.
     *
     * @return the support value
     */
    double getSupportRatio ();

    /**
     * Assign details about the final relation grade
     *
     * @param impacts the grade impacts
     */
    void setImpacts (GradeImpacts impacts);
}
