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
 * instances (of different glyphs) that support one another.
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
     * Report the support ratio brought by this support relation.
     * Q: does the value depends on relation direction?
     *
     * @return the support value, perhaps null
     */
    Double getRatio ();

    /**
     * Set the support ratio brought by this support relation.
     *
     * @param support the new support value
     */
    void setRatio (double support);
}
