//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         A r c S h a p e                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

/**
 * Shape detected for arc.
 */
public enum ArcShape
{

    /**
     * Not yet known.
     */
    UNKNOWN(false, false),
    /**
     * Short arc.
     * Can be tested as slur or wedge extension.
     */
    SHORT(true, true),
    /**
     * Long portion of slur.
     * Can be part of slur only.
     */
    SLUR(true, false),
    /**
     * Long straight line.
     * Can be part of wedge (and slur).
     */
    LINE(true, true),
    /**
     * Short portion of staff line.
     * Can be part of slur only.
     */
    STAFF_ARC(true, false),
    /**
     * Long arc, but no shape detected.
     * Cannot be part of slur/wedge
     */
    IRRELEVANT(false, false);

    private final boolean forSlur; // OK for slur

    private final boolean forWedge; // OK for wedge

    ArcShape (boolean forSlur,
              boolean forWedge)
    {
        this.forSlur = forSlur;
        this.forWedge = forWedge;
    }

    public boolean isSlurRelevant ()
    {
        return forSlur;
    }

    public boolean isWedgeRelevant ()
    {
        return forWedge;
    }
}
