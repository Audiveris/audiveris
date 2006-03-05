//-----------------------------------------------------------------------//
//                                                                       //
//                        S t a v e B u i l d e r                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.glyph.GlyphLag;
import omr.stick.StickSection;
import omr.util.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import omr.glyph.GlyphSection;

/**
 * Class <code>StaveBuilder</code> processes the (five) line areas, according
 * to the peaks found previously.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StaveBuilder
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(StaveBuilder.class);

    //~ Instance variables ------------------------------------------------

    // To allow unique identifiers to staves (for debug only)
    private int id;

    private Sheet sheet;
    private GlyphLag hLag;
    private ListIterator<GlyphSection> vi;

    // Top of stave related area
    private int areaTop = -1;

    // Bottom of stave related area
    private int areaBottom = -1;

    //~ Constructors ------------------------------------------------------

    //--------------//
    // StaveBuilder //
    //--------------//
    /**
     * Create a stave retriever, based on an underlying horizontal lag..
     *
     * @param sheet the sheet we are analyzing
     * @param hLag  the horizontal lag
     * @param vi    the underlying vertex iterator
     */
    public StaveBuilder (Sheet sheet,
                         GlyphLag hLag,
                         ListIterator<GlyphSection> vi)
    {
        this.sheet = sheet;
        this.hLag = hLag;
        this.vi = vi;
    }

    //~ Methods -----------------------------------------------------------

    /**
     * Create a stave info, using a list of related peaks that correspond
     * to the stave lines.
     *
     * @param peaks    the histogram peaks that belong to this stave area
     * @param interval the mean interval between peaks of this staff area
     */
    public StaveInfo buildInfo (List<Peak> peaks,
                                double interval)
            throws omr.ProcessingException
    {
        // Id for the newly created stave
        ++id;

        if (logger.isDebugEnabled()) {
            logger.debug("Stave #" + id + " interval=" + interval);
        }

        // Specific stave scale
        Scale scale = new Scale((int) Math.rint(interval),
                                sheet.getScale().mainFore());

        // Process each peak into a line of the set
        List<LineInfo> lines = new ArrayList<LineInfo>();
        for (Peak peak : peaks) {
            LineBuilder builder = new LineBuilder(hLag, peak.getTop(),
                                                  peak.getBottom(), vi,
                                                  sheet, scale);
            lines.add(builder.buildInfo());
        }

        // Retrieve left and right abscissa for the staff lines of the set
        // We use a kind of vote here, since one or two lines can be read
        // as longer than real, so we use the abscissa of the median.
        List<Integer> lefts = new ArrayList<Integer>();
        List<Integer> rights = new ArrayList<Integer>();

        for (LineInfo line : lines) {
            lefts.add(line.getLeft());
            rights.add(line.getRight());
        }

        Collections.sort(lefts);
        Collections.sort(rights);
        int left = lefts.get(2);
        int right = rights.get(2);

        if (logger.isDebugEnabled()) {
            logger.debug("End of Stave #" + id + " " + this);
        }

        // Allocate the stave info
        return new StaveInfo(left, right, scale, lines);
    }
}
