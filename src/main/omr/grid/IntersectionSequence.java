//----------------------------------------------------------------------------//
//                                                                            //
//                  I n t e r s e c t i o n S e q u e n c e                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.glyph.facets.Glyph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * Class {@code IntersectionSequence} handles a sorted sequence of
 * sticks intersections.
 *
 * @author Hervé Bitteur
 */
class IntersectionSequence
        extends TreeSet<StickIntersection>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            IntersectionSequence.class);

    //~ Constructors -----------------------------------------------------------
    //----------------------//
    // IntersectionSequence //
    //----------------------//
    /**
     * Creates a new IntersectionSequence object.
     *
     * @param comparator the comparator (hori or vert) to use for the sequence
     */
    public IntersectionSequence (
            Comparator<? super StickIntersection> comparator)
    {
        super(comparator);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getSticks //
    //-----------//
    public List<Glyph> getSticks ()
    {
        return StickIntersection.sticksOf(this);
    }

    //--------//
    // reduce //
    //--------//
    public void reduce (double maxDeltaPos)
    {
        // If 2 sticks are close in position, simply merge them
        for (Iterator<StickIntersection> headIt = iterator(); headIt.hasNext();) {
            StickIntersection head = headIt.next();

            for (StickIntersection tail : tailSet(head, false)) {
                if (tail.getStickAncestor() == head.getStickAncestor()) {
                    continue;
                }

                if ((tail.x - head.x) <= maxDeltaPos) {
                    if (logger.isDebugEnabled()
                        || head.getStickAncestor()
                            .isVip()
                        || tail.getStickAncestor()
                            .isVip()) {
                        logger.info("Merging verticals {} & {}", head, tail);
                    }

                    Glyph tailAncestor = tail.getStickAncestor();
                    tailAncestor.stealSections(head.getStickAncestor());
                    headIt.remove();

                    break;
                }
            }
        }
    }
}
