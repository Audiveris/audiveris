//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           B r a c k e t C o n n e c t i o n I n t e r                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import static omr.glyph.Shape.BRACKET_CONNECTION;

import omr.sheet.grid.BarConnection;

import omr.sig.GradeImpacts;

/**
 * Class {@code BracketConnectionInter} represents a vertical connection between two
 * brackets inters across staves.
 *
 * @author Hervé Bitteur
 */
public class BracketConnectionInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final BarConnection connection;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BracketConnectionInter object.
     *
     * @param connection the underlying connection
     * @param impacts    the assignment details
     */
    public BracketConnectionInter (BarConnection connection,
                                   GradeImpacts impacts)
    {
        super(null, connection.getArea().getBounds(), BRACKET_CONNECTION, impacts);
        this.connection = connection;
        this.area = connection.getArea();
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
