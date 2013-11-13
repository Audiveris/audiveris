//----------------------------------------------------------------------------//
//                                                                            //
//                          D i s t a n c e T a b l e                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

/**
 * Interface {@code DistanceTable}
 *
 * @author Hervé Bitteur
 */
public interface DistanceTable
        extends Table
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report the normalizing value by which each raw distance data
     * should be divided.
     *
     * @return the normalizing value
     */
    int getNormalizer ();

    //~ Inner Classes ----------------------------------------------------------
    //---------//
    // Integer //
    //---------//
    public static class Integer
            extends Table.Integer
            implements DistanceTable
    {
        //~ Instance fields ----------------------------------------------------

        private final int normalizer;

        //~ Constructors -------------------------------------------------------
        public Integer (int width,
                        int height,
                        int normalizer)
        {
            super(width, height);
            this.normalizer = normalizer;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int getNormalizer ()
        {
            return normalizer;
        }
    }

    //-------//
    // Short //
    //-------//
    public static class Short
            extends Table.Short
            implements DistanceTable
    {
        //~ Instance fields ----------------------------------------------------

        private final int normalizer;

        //~ Constructors -------------------------------------------------------
        public Short (int width,
                      int height,
                      int normalizer)
        {
            super(width, height);
            this.normalizer = normalizer;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int getNormalizer ()
        {
            return normalizer;
        }
    }
}
