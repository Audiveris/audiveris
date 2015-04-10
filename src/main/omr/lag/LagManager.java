//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       L a g M a n a g e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.OMR;

import omr.sheet.Sheet;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code LagManager} keeps a catalog of Lag instances for a given sheet.
 *
 * @author Hervé Bitteur
 */
public class LagManager
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Related sheet. */
    private final Sheet sheet;

    /** Map of all public lags. */
    private final Map<String, Lag> lagMap = new TreeMap<String, Lag>();

    /** Id of last long horizontal section. */
    private int lastLongHSectionId = -1;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code LagManager} object.
     *
     * @param sheet DOCUMENT ME!
     */
    public LagManager (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // getAllLags //
    //------------//
    /**
     * Report all currently registered lags at this sheet instance.
     *
     * @return the collection of all registered lags, some of which may be null
     */
    public Collection<Lag> getAllLags ()
    {
        return lagMap.values();
    }

    //--------//
    // getLag //
    //--------//
    /**
     * Report the desired lag.
     *
     * @param key the lag name
     * @return the lag if already registered, null otherwise
     */
    public Lag getLag (String key)
    {
        return lagMap.get(key);
    }

    //---------------------//
    // getLongSectionMaxId //
    //---------------------//
    /**
     * Report the id of the last long horizontal section
     *
     * @return the id of the last long horizontal section
     */
    public int getLongSectionMaxId ()
    {
        return lastLongHSectionId;
    }

    //--------//
    // setLag //
    //--------//
    /**
     * Register the provided lag.
     *
     * @param key the registered key for the lag
     * @param lag the lag to register, perhaps null
     */
    public void setLag (String key,
                        Lag lag)
    {
        Lag oldLag = getLag(key);

        if ((oldLag != null) && (OMR.getGui() != null)) {
            oldLag.cutServices();
        }

        lagMap.put(key, lag);

        if ((lag != null) && (OMR.getGui() != null)) {
            lag.setServices(sheet.getLocationService());
        }
    }

    //---------------------//
    // setLongSectionMaxId //
    //---------------------//
    /**
     * Remember the id of the last long horizontal section
     *
     * @param id the id of the last long horizontal section
     */
    public void setLongSectionMaxId (int id)
    {
        lastLongHSectionId = id;
    }
}
