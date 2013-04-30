//----------------------------------------------------------------------------//
//                                                                            //
//                           C o n s t a n t S e t                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.constant;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This abstract class handles a set of Constants as a whole. In particular,
 * this allows a user interface (such as {@link UnitTreeTable}) to present an
 * editing table of the whole set of constants.
 *
 * <p>We recommend to define only one such static ConstantSet per class/unit as
 * a subclass of this (abstract) ConstantSet. </p>
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public abstract class ConstantSet
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(ConstantSet.class);

    //~ Instance fields --------------------------------------------------------
    /** Name of the containing unit/class */
    private final String unit;

    /**
     * The mapping between constant name & constant object. We use a sorted map
     * to allow access by constant index in constant set, as required by
     * ConstantTreeTable. This instance can only be lazily constructed, thanks
     * to {@link #getMap} method, since all the enclosed constants must have
     * been constructed beforehand.
     */
    private volatile SortedMap<String, Constant> map;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // ConstantSet //
    //-------------//
    /**
     * A new ConstantSet instance is created, and registered at the UnitManager
     * singleton, but its map of internal constants will need to be built later.
     */
    public ConstantSet ()
    {
        unit = getClass().getDeclaringClass().getName();

        //        System.out.println(
        //            "\n" + Thread.currentThread().getName() +
        //            ": Creating ConstantSet " + unit);

        // Register this instance
        UnitManager.getInstance().addSet(this);
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // dumpOf //
    //--------//
    /**
     * A utility method to dump current value of each constant in the set.
     *
     * @return the string representation of this set
     */
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s]%n", unit));

        for (Constant constant : getMap().values()) {
            String origin = constant.getValueOrigin();
            if (origin.equals("SRC")) {
                origin = "";
            } else {
                origin = "[" + origin + "]";
            }
            sb.append(String.format(
                    "%-25s %12s %-14s =%5s %-25s\t%s%n",
                    constant.getName(),
                    constant.getShortTypeName(),
                    (constant.getQuantityUnit() != null)
                    ? ("(" + constant.getQuantityUnit() + ")") : "",
                    origin,
                    constant.getCurrentString(),
                    constant.getDescription()));
        }

        return sb.toString();
    }

    //-------------//
    // getConstant //
    //-------------//
    /**
     * Report a constant knowing its name in the constant set
     *
     * @param name the desired name
     *
     * @return the proper constant, or null if not found
     */
    public Constant getConstant (String name)
    {
        return getMap().get(name);
    }

    //-------------//
    // getConstant //
    //-------------//
    /**
     * Report a constant knowing its index in the constant set
     *
     * @param i the desired index value
     *
     * @return the proper constant
     */
    public Constant getConstant (int i)
    {
        return Collections.list(Collections.enumeration(getMap().values())).get(
                i);
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name of the enclosing unit
     *
     * @return unit name
     */
    public String getName ()
    {
        return unit;
    }

    //------------//
    // initialize //
    //------------//
    /**
     * Make sure this ConstantSet has properly been initialized (its map of
     * constants has been built)
     *
     * @return true if initialized correctly, false otherwise
     */
    public boolean initialize ()
    {
        return getMap() != null;
    }

    //------------//
    // isModified //
    //------------//
    /**
     * Predicate to check whether at least one of the constant of the set has
     * been modified
     *
     * @return the modification status of the whole set
     */
    public boolean isModified ()
    {
        for (Constant constant : getMap().values()) {
            if (constant.isModified()) {
                return true;
            }
        }

        return false;
    }

    //------//
    // size //
    //------//
    /**
     * Report the number of constants in this constant set
     *
     * @return the size of the constant set
     */
    public int size ()
    {
        return getMap().size();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Return the last part of the ConstantSet name, without the leading package
     * names. This short name is used by Constant TreeTable
     *
     * @return just the (unqualified) name of the ConstantSet
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        //sb.append("<html><b>");
        int dot = unit.lastIndexOf('.');

        if (dot != -1) {
            sb.append(unit.substring(dot + 1));
        } else {
            sb.append(unit);
        }

        //sb.append("</b></html>");
        return sb.toString();
    }

    //--------//
    // getMap //
    //--------//
    private SortedMap<String, Constant> getMap ()
    {
        if (map == null) {
            // Initialize map content
            initMap();
        }

        return map;
    }

    //---------//
    // initMap //
    //---------//
    /**
     * Now that the enclosed constants of this set have been constructed, let
     * assign them their unit and name parameters.
     */
    private void initMap ()
    {
        SortedMap<String, Constant> tempMap = new TreeMap<>();

        // Retrieve values of all fields
        Class<?> cl = getClass();

        try {
            for (Field field : cl.getDeclaredFields()) {
                field.setAccessible(true);

                String name = field.getName();

                // Make sure that we have only Constants in this ConstantSet
                Object obj = field.get(this);

                // Not yet allocated, no big deal, we'll get back to it later
                if (obj == null) {
                    ///logger.warn("ConstantSet not fully allocated yet");
                    return;
                }

                if (obj instanceof Constant) {
                    Constant constant = (Constant) obj;
                    constant.setUnitAndName(unit, name);
                    tempMap.put(name, constant);
                } else {
                    logger.error(
                            "ConstantSet in unit ''{}'' contains a non"
                            + " Constant field ''{}'' obj= {}",
                            unit, name, obj);
                }
            }

            // Assign the constructed map atomically
            map = tempMap;
        } catch (SecurityException | IllegalArgumentException |
                IllegalAccessException ex) {
            logger.warn("Error initializing map of ConstantSet " + this, ex);
        }
    }
}
