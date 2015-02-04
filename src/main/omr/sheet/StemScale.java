//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t e m S c a l e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

/**
 * Class {@code StemScale} keeps scaling information about stems in a sheet.
 * <p>
 * It handles main and max values for stem thickness.
 * <p>
 * TODO: It could also handle stem length, which can be interesting for stem candidates (at least
 * their tail for those free of beam and flag).
 *
 * @author Hervé Bitteur
 */
public class StemScale
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Most frequent stem thickness. */
    private final int mainThickness;

    /** Maximum stem thickness. */
    private final int maxThickness;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StemScale} object.
     *
     * @param mainThickness most frequent thickness
     * @param maxThickness  max thickness
     */
    public StemScale (int mainThickness,
                      int maxThickness)
    {
        this.mainThickness = mainThickness;
        this.maxThickness = maxThickness;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------------//
    // getMainThickness //
    //------------------//
    /**
     * @return the mainThickness
     */
    public int getMainThickness ()
    {
        return mainThickness;
    }

    //------------------//
    // getMainThickness //
    //------------------//
    /**
     * @return the maxThickness
     */
    public int getMaxThickness ()
    {
        return maxThickness;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("StemScale");
        sb.append(" main:").append(mainThickness);
        sb.append(" max:").append(maxThickness);

        return sb.toString();
    }
}
