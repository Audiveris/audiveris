//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                B a r C o n n e c t o r I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;

import omr.sheet.grid.BarConnection;

import omr.sig.GradeImpacts;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BarConnectorInter} represents a vertical connector between two bar
 * lines across staves.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "bar-connector")
public class BarConnectorInter
        extends AbstractVerticalInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final BarConnection connection;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BarConnectorInter} object.
     *
     * @param connection the underlying connection
     * @param shape      the assigned shape
     * @param impacts    the assignment details
     */
    public BarConnectorInter (BarConnection connection,
                              Shape shape,
                              GradeImpacts impacts)
    {
        super(null, shape, impacts, connection.getMedian(), connection.getWidth());
        this.connection = connection;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BarConnectorInter ()
    {
        super(null, null, null, null, 0);
        this.connection = null;
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
