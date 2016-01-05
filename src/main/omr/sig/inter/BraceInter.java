//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B r a c e I n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BraceInter} represents a brace.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "brace")
public class BraceInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BraceInter object.
     *
     * @param glyph underlying glyph
     * @param grade evaluation value
     */
    public BraceInter (Glyph glyph,
                       double grade)
    {
        super(glyph, null, Shape.BRACE, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BraceInter ()
    {
        super(null, null, null, null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }
}
