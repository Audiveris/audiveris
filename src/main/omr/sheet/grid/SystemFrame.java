//----------------------------------------------------------------------------//
//                                                                            //
//                           S y s t e m F r a m e                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.log.Logger;

import omr.util.HorizontalSide;

import java.util.List;

/**
 * Class {@code SystemFrame} handles physical information about a system
 *
 * @author Hervé Bitteur
 */
public class SystemFrame
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SystemFrame.class);

    //~ Instance fields --------------------------------------------------------

    /** System id */
    private final int id;

    /** Range of system staves */
    private List<StaffInfo> staves;

    /** Left system bar, if any */
    private BarInfo leftBar;

    /** Right system bar, if any */
    private BarInfo rightBar;

    /** Left system limit  (a filament or a straight line) */
    private Object leftLimit;

    /** Right system limit  (a filament or a straight line) */
    private Object rightLimit;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SystemFrame //
    //-------------//
    /**
     * Creates a new SystemFrame object.
     *
     * @param id the system id
     * @param staves the sequence of staves for this system
     */
    public SystemFrame (int             id,
                        List<StaffInfo> staves)
    {
        this.id = id;
        this.staves = staves;
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // setBar //
    //--------//
    /**
     * @param side proper horizontal side
     * @param bar the bar to set
     */
    public void setBar (HorizontalSide side,
                        BarInfo        bar)
    {
        if (side == HorizontalSide.LEFT) {
            this.leftBar = bar;
        } else {
            this.rightBar = bar;
        }
    }

    //--------//
    // getBar //
    //--------//
    /**
     * @param side proper horizontal side
     * @return the system bar on this side, or null
     */
    public BarInfo getBar (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return leftBar;
        } else {
            return rightBar;
        }
    }

    //---------------//
    // getFirstStaff //
    //---------------//
    /**
     * @return the first staff
     */
    public StaffInfo getFirstStaff ()
    {
        return staves.get(0);
    }

    //-------//
    // getId //
    //-------//
    /**
     * @return the id
     */
    public int getId ()
    {
        return id;
    }

    //--------------//
    // getLastStaff //
    //--------------//
    /**
     * @return the lastStaff
     */
    public StaffInfo getLastStaff ()
    {
        return staves.get(staves.size() - 1);
    }

    //---------//
    // setLimit //
    //---------//
    /**
     * @param side proper horizontal side
     * @param line the line to set
     */
    public void setLimit (HorizontalSide side,
                          Object         limit)
    {
        if (side == HorizontalSide.LEFT) {
            this.leftLimit = limit;
        } else {
            this.rightLimit = limit;
        }
    }

    //---------//
    // getLimit //
    //---------//
    /**
     * @param side proper horizontal side
     * @return the leftBar
     */
    public Object getLimit (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return leftLimit;
        } else {
            return rightLimit;
        }
    }

    //-----------//
    // setStaves //
    //-----------//
    /**
     * @param staves the range of staves
     */
    public void setStaves (List<StaffInfo> staves)
    {
        this.staves = staves;
    }

    //-----------//
    // getStaves //
    //-----------//
    /**
     * @return the range of staves
     */
    public List<StaffInfo> getStaves ()
    {
        return staves;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());
        sb.append("#")
          .append(id);
        sb.append(" firstStaff:")
          .append(getFirstStaff().getId());
        sb.append(" lastStaff:")
          .append(getLastStaff().getId());

        if (leftBar != null) {
            sb.append(" leftBar:")
              .append(leftBar);
        }

        if (rightBar != null) {
            sb.append(" rightBar:")
              .append(rightBar);
        }

        if (leftLimit != null) {
            sb.append(" leftLimit:")
              .append(leftLimit);
        }

        if (rightLimit != null) {
            sb.append(" rightLimit:")
              .append(rightLimit);
        }

        sb.append("}");

        return sb.toString();
    }
}
