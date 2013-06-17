//----------------------------------------------------------------------------//
//                                                                            //
//                          F i l a m e n t C o m b                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code FilamentComb} describe a series of y values
 * corresponding to horizontal filaments rather regularly separated.
 *
 * @author Hervé Bitteur
 */
public class FilamentComb
{
    //~ Instance fields --------------------------------------------------------

    /** Column index where sample was taken */
    private final int col;

    /** Series of filaments involved */
    private final List<LineFilament> filaments;

    /** Ordinate value for each filament */
    private final List<Double> ys;

    /** To save processing */
    private boolean processed = false;

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // FilamentComb //
    //-----------------//
    /**
     * Creates a new FilamentComb object.
     *
     * @param col the column index
     */
    public FilamentComb (int col)
    {
        this.col = col;

        filaments = new ArrayList<>();
        ys = new ArrayList<>();
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // append //
    //--------//
    /**
     * Append a filament to the series.
     *
     * @param filament the filament to append
     * @param y        the filament ordinate at x abscissa
     */
    public void append (LineFilament filament,
                        double y)
    {
        filaments.add(filament);
        ys.add(y);
        filament.addComb(col, this); // Link back Fil -> Comb
    }

    //----------//
    // getCount //
    //----------//
    /**
     * Report the number of filaments in this series.
     *
     * @return the count
     */
    public int getCount ()
    {
        return filaments.size();
    }

    //-------------//
    // getFilament //
    //-------------//
    public LineFilament getFilament (int index)
    {
        return filaments.get(index);
    }

    //--------------//
    // getFilaments //
    //--------------//
    public List<LineFilament> getFilaments ()
    {
        return filaments;
    }

    //----------//
    // getIndex //
    //----------//
    public int getIndex (LineFilament filament)
    {
        LineFilament ancestor = (LineFilament) filament.getAncestor();

        for (int index = 0; index < filaments.size(); index++) {
            LineFilament fil = filaments.get(index);

            if (fil.getAncestor() == ancestor) {
                return index;
            }
        }

        return -1;
    }

    //------//
    // getY //
    //------//
    public double getY (int index)
    {
        return ys.get(index);
    }

    //-------------//
    // isProcessed //
    //-------------//
    /**
     * @return the processed
     */
    public boolean isProcessed ()
    {
        return processed;
    }

    //--------------//
    // setProcessed //
    //--------------//
    /**
     * @param processed the processed to set
     */
    public void setProcessed (boolean processed)
    {
        this.processed = processed;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append("Pattern");

        sb.append(" col:")
                .append(col);

        sb.append(" ")
                .append(filaments.size());

        for (int i = 0; i < filaments.size(); i++) {
            LineFilament fil = (LineFilament) filaments.get(i)
                    .getAncestor();
            double y = ys.get(i);
            sb.append(" F#")
                    .append(fil.getId())
                    .append("@")
                    .append((float) y);
        }

        sb.append("}");

        return sb.toString();
    }
}
