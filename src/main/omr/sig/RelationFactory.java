//----------------------------------------------------------------------------//
//                                                                            //
//                        R e l a t i o n F a c t o r y                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;

import org.jgrapht.EdgeFactory;

/**
 * Class {@code RelationFactory} is a basic EdgeFactory meant for
 * Relation instances.
 *
 * @author Hervé Bitteur
 */
public class RelationFactory
    implements EdgeFactory<Inter, Relation>
{
    //~ Methods ----------------------------------------------------------------

    @Override
    public Relation createEdge (Inter sourceVertex,
                                Inter targetVertex)
    {
        // Head - Stem
        if ((sourceVertex.getShape() == Shape.NOTEHEAD_BLACK) &&
            (targetVertex.getShape() == Shape.STEM)) {
            return new HeadStemRelation();
        }
        
        // Default
        return new BasicRelation();
    }
}
