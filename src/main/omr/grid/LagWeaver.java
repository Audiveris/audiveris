//----------------------------------------------------------------------------//
//                                                                            //
//                             L a g W e a v e r                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.sheet.Sheet;

import omr.util.Predicate;
import omr.util.StopWatch;

import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.*;

/**
 * Class {@code LagWeaver} is just a prototype. TODO.
 *
 * @author Hervé Bitteur
 */
public class LagWeaver
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LagWeaver.class);

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final GlyphLag vLag;
    private final GlyphLag hLag;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // LagWeaver //
    //-----------//
    /**
     * Creates a new LagWeaver object.
     * @param sheet the related sheet, which holds the v & h lags
     */
    public LagWeaver (Sheet sheet)
    {
        vLag = sheet.getVerticalLag();
        hLag = sheet.getHorizontalLag();
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // buildInfo //
    //-----------//
    public void buildInfo ()
    {
        StopWatch watch = new StopWatch("LagWeaver");

        logger.info("vLag: " + vLag);
        logger.info("hLag: " + hLag);

        // Remove staff line stuff from hLag
        watch.start("purge hLag");
        //removeStaffLines(hLag);
        logger.info("hLag: " + hLag);

        //
        watch.start("crossRetrieval");
        crossRetrieval();

        // The end
        watch.print();
    }

    //----------------//
    // crossRetrieval //
    //----------------//
    private void crossRetrieval ()
    {
        // Process each vertical section in turn

        //for (GlyphSection vSect : vLag.getSections()) {
        GlyphSection vSect = vLag.getVertexById(1924);
        logger.info("vSect: " + vSect);

        double[] coords = new double[2];

        for (PathIterator it = vSect.getPathIterator(); !it.isDone();) {
            int kind = it.currentSegment(coords);
            logger.fine(
                "kind: " + kind + " x: " + coords[0] + " y: " + coords[1]);
            it.next();
        }

        //}
    }

    //------------------//
    // removeStaffLines //
    //------------------//
    private void removeStaffLines (GlyphLag hLag)
    {
        hLag.purgeSections(
            new Predicate<GlyphSection>() {
                    public boolean check (GlyphSection section)
                    {
                        Glyph glyph = section.getGlyph();

                        return (glyph != null) &&
                               (glyph.getShape() == Shape.STAFF_LINE);
                    }
                });
    }
}
