//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S m a l l B e a m I n t e r                                  //
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

import omr.sig.GradeImpacts;

import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SmallBeamInter} represents a small (cue) beam.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "small-beam")
public class SmallBeamInter
        extends AbstractBeamInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SmallBeamInter object.
     *
     * @param impacts the grade details
     * @param median  median beam line
     * @param height  beam height
     */
    public SmallBeamInter (GradeImpacts impacts,
                           Line2D median,
                           double height)
    {
        super(Shape.BEAM_SMALL, impacts, median, height);
    }

    /**
     * Creates a new {@code SmallBeamInter} object.
     */
    public SmallBeamInter ()
    {
        super(null, null, null, 0);
    }
}
