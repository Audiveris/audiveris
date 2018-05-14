//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C o n s t a n t S e t                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.constant;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This abstract class handles a set of Constants as a whole.
 * <p>
 * In particular, this allows a user interface (such as {@link UnitTreeTable}) to present an
 * editing table of the whole set of constants.
 * <p>
 * We recommend to define only one such static ConstantSet per class/unit as a subclass of this
 * (abstract) ConstantSet. </p>
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public abstract class ConstantSet
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            ConstantSet.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Name of the containing unit/class */
    private final String unit;

    /**
     * The mapping between constant name & constant object.
     * We use a sorted map to allow access by constant index in constant set, as
     * required by ConstantTreeTable.
     * This instance can only be lazily constructed, thanks to {@link #getMap}
     * method, since all the enclosed constants must have been constructed
     * beforehand.
     */
    private volatile SortedMap<String, Constant> map;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * A new ConstantSet instance is created, and registered at the
     * UnitManager singleton, but its map of internal constants will
     * need to be built later.
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

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // dumpOf //
    //--------//
    /**
     * A utility method to dump current value of each constant in the
     * set.
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

            sb.append(
                    String.format(
                            "%-25s %12s %-14s =%5s %-25s\t%s%n",
                            constant.getName(),
                            constant.getClass().getSimpleName(),
                            (constant.getQuantityUnit() != null) ? ("(" + constant.getQuantityUnit() + ")")
                            : "",
                            origin,
                            constant.getStringValue(),
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
        return Collections.list(Collections.enumeration(getMap().values())).get(i);
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
     * Make sure this ConstantSet has properly been initialized
     * (its map of constants has been built)
     *
     * @return true if initialized correctly, false otherwise
     */
    public boolean initialize ()
    {
        return getMap() != null;
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
     * Return the last part of the ConstantSet name, without the
     * leading package names.
     * This short name is used by Constant TreeTable
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
     * Now that the enclosed constants of this set have been
     * constructed, let's assign them their unit and name parameters.
     */
    private void initMap ()
    {
        SortedMap<String, Constant> tempMap = new TreeMap<String, Constant>();

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
                            unit,
                            name,
                            obj);
                }
            }

            // Assign the constructed map atomically
            map = tempMap;
        } catch (Throwable ex) {
            logger.warn("Error initializing map of ConstantSet " + this, ex);
        }
    }
}
