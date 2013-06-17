//----------------------------------------------------------------------------//
//                                                                            //
//                          B a r A l i g n m e n t                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.glyph.facets.Glyph;

import omr.sheet.Sheet;

import omr.util.Navigable;

import java.awt.geom.Point2D;

/**
 * Class {@code BarAlignment} is used to collect all bar lines within a
 * system which should be vertically aligned (typically at the end of a
 * measure), and to check for proper alignment.
 *
 * @author Hervé Bitteur
 */
public class BarAlignment
{
    //~ Instance fields --------------------------------------------------------

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Used to flag a manually defined alignment. */
    private boolean manual;

    /** Vertical sequence of bars intersections with staves. */
    private final StickIntersection[] inters;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BarAlignment object.
     *
     * @param sheet      the related sheet
     * @param staffCount the number of staves to check for consistency
     */
    public BarAlignment (Sheet sheet,
                         int staffCount)
    {
        this.sheet = sheet;
        inters = new StickIntersection[staffCount];
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // addInter //
    //----------//
    /**
     * Record an intersection for the given (staff) index
     *
     * @param index the (staff) index
     * @param inter the intersection to record
     */
    public void addInter (int index,
                          StickIntersection inter)
    {
        inters[index] = inter;
    }

    //----------//
    // distance //
    //----------//
    /**
     * Compute the horizontal distance between the provided intersection and
     * this alignment, taking the global sheet skew into account.
     *
     * @param index the provided (staff) index
     * @param inter the provided intersection
     * @return the relative abscissa distance from this alignment to the
     *         provided intersection
     */
    public Double distance (int index,
                            StickIntersection inter)
    {
        for (int i = index - 1; i >= 0; i--) {
            StickIntersection si = inters[i];

            if (si != null) {
                Point2D dskSi = sheet.getSkew()
                        .deskewed(new Point2D.Double(si.x, si.y));
                Point2D dskIt = sheet.getSkew()
                        .deskewed(
                        new Point2D.Double(inter.x, inter.y));

                return dskIt.getX() - dskSi.getX();
            }
        }

        // Could not measure anything
        return null;
    }

    //----------------//
    // getFilledCount //
    //----------------//
    /**
     * Report the number of intersections found for this bar alignment
     *
     * @return the percentage filled
     */
    public int getFilledCount ()
    {
        int cells = 0;

        for (StickIntersection inter : inters) {
            if (inter != null) {
                cells++;
            }
        }

        return cells;
    }

    //------------------//
    // getIntersections //
    //------------------//
    /**
     * Report the array of intersections found, one cell per staff.
     *
     * @return the intersections found (with null array cells for missing
     *         intersections)
     */
    public StickIntersection[] getIntersections ()
    {
        return inters;
    }

    //----------//
    // isManual //
    //----------//
    /**
     * Report whether this alignment is manually defined or not.
     *
     * @return the manual flag
     */
    public boolean isManual ()
    {
        return manual;
    }

    //-----------//
    // setManual //
    //-----------//
    /**
     * Flag this alignment as a manual one.
     *
     * @param manual the manual flag to set
     */
    public void setManual (boolean manual)
    {
        this.manual = manual;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        sb.append(" ")
                .append(getFilledCount())
                .append("/")
                .append(inters.length);

        for (int i = 0; i < inters.length; i++) {
            sb.append(" ")
                    .append(i)
                    .append(":");

            if (inters[i] != null) {
                Glyph stick = inters[i].getStickAncestor();
                sb.append("#")
                        .append(stick.getId());
            } else {
                sb.append("null");
            }
        }

        sb.append("}");

        return sb.toString();
    }
}
