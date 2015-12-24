//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F l a g I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.sheet.Part;

import omr.sig.relation.FlagStemRelation;
import omr.sig.relation.HeadStemRelation;
import omr.sig.relation.Relation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code FlagInter} represents one or several flags.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "flag")
public class FlagInter
        extends AbstractFlagInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new FlagInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    protected FlagInter (Glyph glyph,
                         Shape shape,
                         double grade)
    {
        super(glyph, shape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected FlagInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getPart //
    //---------//
    @Override
    public Part getPart ()
    {
        if (part == null) {
            // Flag -> Stem
            for (Relation fsRel : sig.getRelations(this, FlagStemRelation.class)) {
                StemInter stem = (StemInter) sig.getOppositeInter(this, fsRel);

                // Stem -> Head
                for (Relation hsRel : sig.getRelations(stem, HeadStemRelation.class)) {
                    Inter head = sig.getOppositeInter(stem, hsRel);

                    return part = head.getPart();
                }
            }
        }

        return part;
    }
}
