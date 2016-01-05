//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e s t C h o r d I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code RestChordInter} is a AbstractChordInter composed of (one) rest.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "rest-chord")
public class RestChordInter
        extends AbstractChordInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code RestChordInter} object.
     *
     * @param grade the intrinsic grade
     */
    public RestChordInter (double grade)
    {
        super(grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private RestChordInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "RestChord";
    }
}
