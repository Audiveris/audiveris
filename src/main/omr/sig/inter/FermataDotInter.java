//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  F e r m a t a D o t I n t e r                                 //
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code FermataDotInter} represents a dot in a fermata symbol.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "fermata-dot")
public class FermataDotInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code FermataDotInter} object.
     *
     * @param glyph underlying glyph
     * @param grade evaluation value
     */
    public FermataDotInter (Glyph glyph,
                            double grade)
    {
        super(glyph, null, Shape.FERMATA_DOT, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private FermataDotInter ()
    {
    }
}
