//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            B r a c k e t C o n n e c t o r I n t e r                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import static omr.glyph.Shape.BRACKET_CONNECTOR;

import omr.sheet.grid.BarConnection;

import omr.sig.GradeImpacts;

/**
 * Class {@code BracketConnectorInter} represents a vertical connector between two
 * brackets inters across staves.
 *
 * @author Hervé Bitteur
 */
public class BracketConnectorInter
        extends AbstractVerticalInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final BarConnection connection;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BracketConnectorInter object.
     *
     * @param connection the underlying connection
     * @param impacts    the assignment details
     */
    public BracketConnectorInter (BarConnection connection,
                                  GradeImpacts impacts)
    {
        super(null, BRACKET_CONNECTOR, impacts, connection.getMedian(), connection.getWidth());
        this.connection = connection;
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
