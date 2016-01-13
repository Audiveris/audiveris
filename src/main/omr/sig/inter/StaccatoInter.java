//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S t a c c a t o I n t e r                                   //
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

import omr.sheet.rhythm.Voice;

import omr.sig.relation.Relation;
import omr.sig.relation.ChordStaccatoRelation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code StaccatoInter} represents a staccato dot.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "staccato")
public class StaccatoInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new StaccatoInter object.
     *
     * @param glyph underlying glyph
     * @param grade evaluation value
     */
    public StaccatoInter (Glyph glyph,
                          double grade)
    {
        super(glyph, null, Shape.STACCATO, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private StaccatoInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, ChordStaccatoRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }
}
