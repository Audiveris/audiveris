//----------------------------------------------------------------------------//
//                                                                            //
//                              B e a m I n t e r                             //
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
import omr.glyph.facets.Glyph;

import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * Class {@code BeamInter} represents a beam interpretation.
 *
 * @author Hervé Bitteur
 */
public class BeamInter
        extends BasicInter
{
    //~ Instance fields --------------------------------------------------------

    /** North border. */
    private final Line2D north;

    /** South border. */
    private final Line2D south;

    /** Beam path, meant for drawing. */
    private final Path2D path;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BeamInter object.
     *
     * @param glyph the underlying glyph
     * @param grade the assignment quality
     * @param north northern border of the beam
     * @param south southern border of the beam
     */
    public BeamInter (Glyph glyph,
                      double grade,
                      Line2D north,
                      Line2D south)
    {
        super(glyph, Shape.BEAM, grade);

        this.north = north;
        this.south = south;

        // Build drawing path
        path = new Path2D.Double();
        path.moveTo(north.getX1(), north.getY1()); // Upper left
        path.lineTo(north.getX2() + 1, north.getY2()); // Upper right
        path.lineTo(south.getX2() + 1, south.getY2() + 1); // Lower right
        path.lineTo(south.getX1(), south.getY1() + 1); // Lower left
        path.closePath();

        // Define precise bounds based on this path
        setBounds(north.getBounds().union(south.getBounds()));
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

    /**
     * @return the north
     */
    public Line2D getNorth ()
    {
        return north;
    }

    //---------//
    // getPath //
    //---------//
    public Path2D getPath ()
    {
        return path;
    }

    /**
     * @return the south
     */
    public Line2D getSouth ()
    {
        return south;
    }
}
