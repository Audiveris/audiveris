//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     U n i t M a n a g e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import org.audiveris.omr.Main;
import org.audiveris.omr.util.StopWatch;

import org.reflections.Reflections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class {@code UnitManager} manages all units (AKA classes), for which we have a
 * ConstantSet.
 * <p>
 * To help {@link UnitTreeTable} display the whole tree of UnitNodes, UnitManager can pre-load all
 * the classes known to contain a ConstantSet.
 * <p>
 * Since the persistency of a Constant uses its fully qualified name (i.e. the path to the enclosing
 * class, plus the name of the constant element in the ConstantSet), the determination of the fully
 * qualified name is deferred until the value of the Constant is actually retrieved.
 * This is implemented through the use of a DirtySet.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class UnitManager
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The single instance of this class. */
    private static final UnitManager INSTANCE = new UnitManager();

    private static final Logger logger = LoggerFactory.getLogger(
            UnitManager.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The root node. */
    private final PackageNode root = new PackageNode("<root>", null);

    /** Map of PackageNodes and UnitNodes. */
    private final ConcurrentHashMap<String, Node> mapOfNodes = new ConcurrentHashMap<String, Node>();

    /** Set of names of ConstantSets that still need to be initialized. */
    private final ConcurrentSkipListSet<String> dirtySets = new ConcurrentSkipListSet<String>();

    //~ Constructors -------------------------------------------------------------------------------
    /** This is a singleton. */
    private UnitManager ()
    {
        mapOfNodes.put("<root>", root);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this package.
     *
     * @return the single instance
     */
    public static UnitManager getInstance ()
    {
        return INSTANCE;
    }

    //--------//
    // addSet //
    //--------//
    /**
     * Add a ConstantSet, which means perhaps adding a UnitNode if not already allocated
     * and setting its ConstantSet reference to the provided ConstantSet.
     *
     * @param set the ConstantSet to add to the hierarchy
     */
    public void addSet (ConstantSet set)
    {
        ///log ("addSet set=" + set.getName());
        retrieveUnit(set.getName()).setConstantSet(set);

        // Register this name in the dirty ones
        dirtySets.add(set.getName());
    }

    //---------------//
    // checkAllUnits //
    //---------------//
    /**
     * Check if all defined constants are used by at least one unit.
     */
    public void checkAllUnits ()
    {
        SortedSet<String> constants = new TreeSet<String>();

        for (Node node : mapOfNodes.values()) {
            if (node instanceof UnitNode) {
                UnitNode unit = (UnitNode) node;
                ConstantSet set = unit.getConstantSet();

                if (set != null) {
                    for (int i = 0; i < set.size(); i++) {
                        Constant constant = set.getConstant(i);
                        constants.add(unit.getName() + "." + constant.getName());
                    }
                }
            }
        }

        dumpStrings("constants", constants);

        Collection<String> props = ConstantManager.getInstance().getAllProperties();
        props.removeAll(constants);
        dumpStrings("Non set-enclosed properties", props);

        dumpStrings(
                "Unused User properties",
                ConstantManager.getInstance().getUnusedUserProperties());
    }

    //----------------//
    // checkDirtySets //
    //----------------//
    /**
     * Go through all registered to-be-initialized sets, and initialize
     * them, then clear the set of such dirty sets.
     */
    public void checkDirtySets ()
    {
        int rookies = 0;

        // We use (and clear) the collection of rookies
        for (Iterator<String> it = dirtySets.iterator(); it.hasNext();) {
            String name = it.next();
            rookies++;

            //            System.out.println(
            //                Thread.currentThread().getName() + ": checkDirtySets. name=" +
            //                name);
            UnitNode unit = (UnitNode) getNode(name);

            if (unit.getConstantSet().initialize()) {
                it.remove();
            }
        }

        //        System.out.println(
        //            Thread.currentThread().getName() + ": checkDirtySets. rookies:" +
        //            rookies);
    }

    //--------------//
    // dumpAllUnits //
    //--------------//
    /**
     * Dumps on the standard output the current value of all Constants
     * of all ConstantSets.
     */
    public void dumpAllUnits ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("UnitManager. All Units:%n"));

        // Use alphabetical order for easier reading
        List<Node> nodes = new ArrayList<Node>(mapOfNodes.values());
        Collections.sort(nodes, Node.nameComparator);

        for (Node node : nodes) {
            if (node instanceof UnitNode) {
                UnitNode unit = (UnitNode) node;

                // ConstantSet?
                ConstantSet set = unit.getConstantSet();

                if (set != null) {
                    sb.append(String.format("%n%s", set.dumpOf()));
                }
            }
        }

        logger.info(sb.toString());
    }

    //---------//
    // getNode //
    //---------//
    /**
     * Retrieves a node object, knowing its path name.
     *
     * @param path fully qualified node name
     * @return the node object, or null if not found
     */
    public Node getNode (String path)
    {
        return mapOfNodes.get(path);
    }

    //---------//
    // getRoot //
    //---------//
    /**
     * Return the PackageNode at the root of the node hierarchy.
     *
     * @return the root PackageNode
     */
    public PackageNode getRoot ()
    {
        return root;
    }

    //--------------//
    // preLoadUnits //
    //--------------//
    /**
     * Allows to pre-load the various classes that enclose a ConstantSet and are not
     * already loaded.
     * <p>
     * This method is meant to be used by the UI which let the user browse and modify the whole
     * collection of constants.
     */
    public void preLoadUnits ()
    {
        StopWatch watch = new StopWatch("Reflections");
        watch.start("new Reflections()");

        // Look into omr package hierarchy
        Reflections reflections = new Reflections(Main.class.getPackage().getName());

        watch.start("getSubTypesOf(ConstantSet)");

        Set<Class<? extends ConstantSet>> classes = reflections.getSubTypesOf(ConstantSet.class);

        watch.start("forName()");

        for (Class<? extends ConstantSet> clazz : classes) {
            // Should not happen, but safer.
            if (Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface()) {
                continue;
            }

            try {
                Class<?> enclosing = clazz.getEnclosingClass();
                Class.forName(enclosing.getName()); // This loads its ConstantSet
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        ///watch.print();
    }

    //---------------//
    // resetAllUnits //
    //---------------//
    /**
     * Reset all constants to their factory (source) value.
     */
    public void resetAllUnits ()
    {
        for (Node node : mapOfNodes.values()) {
            if (node instanceof UnitNode) {
                UnitNode unit = (UnitNode) node;
                ConstantSet set = unit.getConstantSet();

                if (set != null) {
                    for (int i = 0; i < set.size(); i++) {
                        Constant constant = set.getConstant(i);
                        constant.reset();
                    }
                }
            }
        }
    }

    //-------------//
    // searchUnits //
    //-------------//
    /**
     * Search for all the units for which the provided string is found
     * in the unit name or the unit description.
     *
     * @param string the string to search for
     * @return the set (perhaps empty) of the matching units, a mix of UnitNode
     *         and Constant instances.
     */
    public Set<Object> searchUnits (String string)
    {
        String str = string.toLowerCase(Locale.ENGLISH);
        Set<Object> found = new LinkedHashSet<Object>();

        for (Node node : mapOfNodes.values()) {
            if (node instanceof UnitNode) {
                UnitNode unit = (UnitNode) node;

                // Search in unit name itself
                if (unit.getSimpleName().toLowerCase(Locale.ENGLISH).contains(str)) {
                    found.add(unit);
                }

                // Search in unit constants, if any
                ConstantSet set = unit.getConstantSet();

                if (set != null) {
                    for (int i = 0; i < set.size(); i++) {
                        Constant constant = set.getConstant(i);

                        if (constant.getName().toLowerCase(Locale.US).contains(str)
                            || constant.getDescription().toLowerCase(Locale.US).contains(str)) {
                            found.add(constant);
                        }
                    }
                }
            }
        }

        return found;
    }

    //---------//
    // addUnit //
    //---------//
    /**
     * Include a Unit in the hierarchy.
     *
     * @param unit the Unit to include
     */
    private void addUnit (UnitNode unit)
    {
        //log ("addUnit unit=" + unit.getName());
        // Update the hierarchy. Include it in the map, as well as all needed
        // intermediate package nodes if any is needed.
        String name = unit.getName();

        // Add this node and its parents as needed
        if (mapOfNodes.putIfAbsent(name, unit) == null) {
            updateParents(unit);
        }
    }

    //-------------//
    // dumpStrings //
    //-------------//
    private void dumpStrings (String title,
                              Collection<String> strings)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%s:%n", title));

        for (String string : strings) {
            sb.append(String.format("%s%n", string));
        }

        logger.info(sb.toString());
    }

    //--------------//
    // retrieveUnit //
    //--------------//
    /**
     * Looks for the unit with given name.
     * If the unit does not exist, it is created and inserted in the hierarchy.
     *
     * @param name the name of the desired unit
     * @return the unit (found, or created)
     */
    private UnitNode retrieveUnit (String name)
    {
        Node node = getNode(name);

        if (node == null) {
            // Create a hosting unit node
            UnitNode unit = new UnitNode(name);
            addUnit(unit);

            return unit;
        } else if (node instanceof UnitNode) {
            return (UnitNode) node;
        } else if (node instanceof PackageNode) {
            logger.error("Unit with same name as package {}", name);
        }

        return null;
    }

    //---------------//
    // updateParents //
    //---------------//
    /**
     * Update the chain of parents of a Unit, by walking up all the package names found
     * in the fully qualified Unit name, creating PackageNodes when needed, or adding a
     * new child to an existing PackageNode.
     *
     * @param unit the Unit whose chain of parents is to be updated
     */
    private void updateParents (UnitNode unit)
    {
        String name = unit.getName();
        int length = name.length();
        Node child = unit;

        for (int i = name.lastIndexOf('.', length - 1); i >= 0; i = name.lastIndexOf('.', i - 1)) {
            String parent = name.substring(0, i);
            Node obj = mapOfNodes.get(parent);

            // Create a provision node for a future parent.
            if (obj == null) {
                //log("No parent " + sub + " found. Creating PackageNode.");
                PackageNode pn = new PackageNode(parent, child);

                if (mapOfNodes.putIfAbsent(parent, pn) != null) {
                    // Already done by someone else, give up
                    return;
                }

                child = pn;
            } else if (obj instanceof PackageNode) {
                //log("PackageNode  " + sub + " found. adding child");
                ((PackageNode) obj).addChild(child);

                return;
            } else {
                Exception e = new IllegalStateException(
                        "unexpected node type " + obj.getClass() + " in map.");
                e.printStackTrace();

                return;
            }
        }

        // No intermediate parent found, so hook it to the root itself
        getRoot().addChild(child);
    }
}
