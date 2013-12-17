//----------------------------------------------------------------------------//
//                                                                            //
//                     B a r C o n n e c t i o n I n t e r                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;

import omr.grid.BarConnection;

import java.awt.geom.Area;

/**
 * Class {@code BarConnectionInter} represents a vertical connection
 * between two bar lines across staves.
 *
 * @author Hervé Bitteur
 */
public class BarConnectionInter
        extends AbstractInter
{
    //~ Instance fields --------------------------------------------------------

    private final BarConnection connection;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BarConnectionInter object.
     *
     * @param connection the underlying connection
     * @param shape      the assigned shape
     * @param impacts    the assignment details
     */
    public BarConnectionInter (BarConnection connection,
                               Shape shape,
                               GradeImpacts impacts)
    {
        super(connection.getArea().getBounds(), shape, impacts.getGrade());
        this.connection = connection;
        setImpacts(impacts);
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    @Override
    public Area getArea ()
    {
        return connection.getArea();
    }
}
