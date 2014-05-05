//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C u r v e s E r a s e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.lag.Section;

import omr.sheet.PageEraser;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.sig.AbstractNoteInter;
import omr.sig.Inter;
import omr.sig.SIGraph;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code CurvesEraser} erases shapes and glyphs to prepare curves retrieval.
 *
 * @author Hervé Bitteur
 */
public class CurvesEraser
        extends PageEraser
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(CurvesEraser.class);

    //~ Instance fields ----------------------------------------------------------------------------
    // Vertical margin added above and below any staff DMZ
    private final int dmzDyMargin;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new CurvesEraser object.
     *
     * @param buffer page buffer
     * @param g      graphics context on buffer
     * @param sheet  related sheet
     */
    public CurvesEraser (ByteProcessor buffer,
                         Graphics2D g,
                         Sheet sheet)
    {
        super(buffer, g, sheet);

        Scale scale = sheet.getScale();
        dmzDyMargin = scale.toPixels(constants.staffVerticalMargin);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // eraseGlyphs //
    //-------------//
    public Map<SystemInfo, List<Glyph>> eraseGlyphs (Collection<Shape> shapes)
    {
        final Map<SystemInfo, List<Glyph>> erasedMap = new TreeMap<SystemInfo, List<Glyph>>();

        for (SystemInfo system : sheet.getSystems()) {
            final List<Glyph> erased = new ArrayList<Glyph>();
            erasedMap.put(system, erased);

            for (Shape shape : shapes) {
                for (Glyph glyph : system.lookupShapedGlyphs(shape)) {
                    for (Section section : glyph.getMembers()) {
                        section.render(g, false, Color.WHITE);
                    }
                }
            }
        }

        return erasedMap;
    }

    //-------------//
    // eraseShapes //
    //-------------//
    /**
     * Erase from image graphics all instances of provided shapes and return the
     * "erased" inter instances per system.
     *
     * @param shapes (input) the shapes to look for
     * @return the corresponding erased inter instances per system
     */
    public Map<SystemInfo, List<Inter>> eraseShapes (Collection<Shape> shapes)
    {
        final Map<SystemInfo, List<Inter>> erasedMap = new TreeMap<SystemInfo, List<Inter>>();

        for (SystemInfo system : sheet.getSystems()) {
            final SIGraph sig = system.getSig();
            final List<Inter> erased = new ArrayList<Inter>();
            erasedMap.put(system, erased);

            for (Inter inter : sig.vertexSet()) {
                if (!inter.isDeleted() && shapes.contains(inter.getShape())) {
                    if (canHide(inter)) {
                        erased.add(inter);
                    }
                }
            }

            // Erase the inters
            for (Inter inter : erased) {
                inter.accept(this);
            }

            // Erase system DMZ?
            if (constants.useDmz.isSet()) {
                eraseDmz(system);
            }
        }

        return erasedMap;
    }

    @Override
    public void visit (AbstractNoteInter inter)
    {
        // Use plain symbol painting to symply erase the note
        visit((Inter) inter);
    }

    //----------//
    // eraseDmz //
    //----------//
    /**
     * Erase from image the DMZ part of a system.
     *
     * @param system the system to process
     */
    private void eraseDmz (SystemInfo system)
    {
        StaffInfo firstStaff = system.getFirstStaff();
        StaffInfo lastStaff = system.getLastStaff();
        int dmzEnd = firstStaff.getDmzEnd();
        int top = firstStaff.getFirstLine().yAt(dmzEnd) - dmzDyMargin;
        int bot = lastStaff.getLastLine().yAt(dmzEnd) + dmzDyMargin;
        g.fillRect(0, top, dmzEnd, bot - top + 1);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Boolean useDmz = new Constant.Boolean(
                true,
                "Should we erase the DMZ at staff start");

        final Scale.Fraction staffVerticalMargin = new Scale.Fraction(
                2.0,
                "Margin erased above & below staff DMZ area");
    }
}
