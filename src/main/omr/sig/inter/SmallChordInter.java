//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S m a l l C h o r d I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SmallChordInter} is a AbstractChordInter composed of small heads.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "small-chord")
public class SmallChordInter
        extends AbstractChordInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code SmallChordInter} object.
     *
     * @param grade the intrinsic grade
     */
    public SmallChordInter (double grade)
    {
        super(grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private SmallChordInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "SmallChord";
    }
}
