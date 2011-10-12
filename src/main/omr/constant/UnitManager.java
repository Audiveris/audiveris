//----------------------------------------------------------------------------//
//                                                                            //
//                           U n i t M a n a g e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.constant;

import omr.log.Logger;

import net.jcip.annotations.ThreadSafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class <code>UnitManager</code> manages all units (aka classes), for which we
 * have either a ConstantSet, or a Logger, or both.
 *
 * <p>To help {@link UnitTreeTable} display the whole tree of UnitNodes,
 * UnitManager can pre-load all the classes known to contain a ConstantSet or a
 * Logger. This list is kept up-to-date and stored as a property
 *
 * <p>This class is meant to be thread-safe.
 *
 * @author Hervé Bitteur
 */
@ThreadSafe
public class UnitManager
{
    //~ Static fields/initializers ---------------------------------------------

    /** Name of this unit */
    private static final String UNIT = UnitManager.class.getName();

    /** The single instance of this class */
    private static final UnitManager INSTANCE = new UnitManager();

    /** Separator used in property that concatenates all unit names */
    private static final String SEPARATOR = ";";

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(UnitManager.class);

    //~ Instance fields --------------------------------------------------------

    /** The root node */
    private final PackageNode root = new PackageNode("<root>", null);

    /** Map of PackageNodes and UnitNodes */
    private final ConcurrentHashMap<String, Node> mapOfNodes = new ConcurrentHashMap<String, Node>();

    /** Set of names of ConstantSets that still need to be initialized */
    private final ConcurrentSkipListSet<String> dirtySets = new ConcurrentSkipListSet<String>();

    /**
     * Lists of all units known as containing a constantset. This is kept
     * up-to-date and saved as a property.
     */
    private Constant.String units;

    /** Flag to avoid storing units being pre-loaded */
    private volatile boolean storeIt = false;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // UnitManager //
    //-------------//
    /** This is a singleton */
    private UnitManager ()
    {
        mapOfNodes.put("<root>", root);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this package
     *
     * @return the single instance
     */
    public static UnitManager getInstance ()
    {
        return INSTANCE;
    }

    //---------//
    // getNode //
    //---------//
    /**
     * Retrieves a node object, knowing its path name
     *
     * @param path fully qualified node name
     *
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
     * Return the PackageNode at the root of the node hierarchy
     *
     * @return the root PackageNode
     */
    public PackageNode getRoot ()
    {
        return root;
    }

    //-----------//
    // addLogger //
    //-----------//
    /**
     * Add a Logger, which like addSet means perhaps adding a UnitNode if not
     * already allocated and setting its Logger reference to the provided Logger
     *
     * @param logger the Logger to add to the hierarchy
     */
    public void addLogger (Logger logger)
    {
        //log ("addLogger logger=" + logger.getName());
        retrieveUnit(logger.getName())
            .setLogger(logger);
    }

    //--------//
    // addSet //
    //--------//
    /**
     * Add a ConstantSet, which means perhaps adding a UnitNode if not already
     * allocated and setting its ConstantSet reference to the provided
     * ConstantSet.
     *
     * @param set the ConstantSet to add to the hierarchy
     */
    public void addSet (ConstantSet set)
    {
        ///log ("addSet set=" + set.getName());
        retrieveUnit(set.getName())
            .setConstantSet(set);

        // Register this name in the dirty ones
        dirtySets.add(set.getName());
    }

    //---------------//
    // checkAllUnits //
    //---------------//
    /**
     * Check if all defined constants are used by at least one unit
     */
    public void checkAllUnits ()
    {
        SortedSet<String> constants = new TreeSet<String>();

        for (Node node : mapOfNodes.values()) {
            if (node instanceof UnitNode) {
                UnitNode    unit = (UnitNode) node;
                ConstantSet set = unit.getConstantSet();

                if (set != null) {
                    for (int i = 0; i < set.size(); i++) {
                        Constant constant = set.getConstant(i);
                        constants.add(
                            unit.getName() + "." + constant.getName());
                    }
                }
            }
        }

        dumpStrings("constants", constants);

        Collection<String> props = ConstantManager.getInstance()
                                                  .getAllProperties();
        props.removeAll(constants);
        dumpStrings("Non set-enclosed properties", props);

        dumpStrings(
            "Unused User properties",
            ConstantManager.getInstance().getUnusedUserProperties());

        dumpStrings(
            "Unused Default properties",
            ConstantManager.getInstance().getUnusedDefaultProperties());

        dumpStrings(
            "Useless Default properties",
            ConstantManager.getInstance().getUselessDefaultProperties());
    }

    //----------------//
    // checkDirtySets //
    //----------------//
    /**
     * Go through all registered to-be-initialized sets, and initialize them,
     * then clear the set of such dirty sets
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

            if (unit.getConstantSet()
                    .initialize()) {
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
     * Dumps on the standard output the current value of all Constants of all
     * ConstantSets.
     */
    public void dumpAllUnits ()
    {
        System.out.println("\nUnitManager. All Units:");
        System.out.println("=======================");

        // Use alphabetical order for easier reading
        List<Node> nodes = new ArrayList<Node>(mapOfNodes.values());
        Collections.sort(nodes, Node.nameComparator);

        for (Node node : nodes) {
            if (node instanceof UnitNode) {
                UnitNode    unit = (UnitNode) node;

                // ConstantSet?
                ConstantSet set = unit.getConstantSet();

                if (set != null) {
                    set.dump();
                }
            }
        }
    }

    //--------------//
    // preLoadUnits //
    //--------------//
    /**
     * Allows to preload the names of the various nodes in the hierarchy, by
     * simply extracting names stored at previous runs. This will load the
     * classes not already loaded. This method is meant to be used by the UI
     * which let the user browse and modify the whole collection of constants.
     *
     * @param main the application main class name
     */
    public void preLoadUnits (String main)
    {
        //log("pre-loading units");
        String unitName;

        if (main != null) {
            unitName = main + ".Units";
        } else {
            unitName = "Units";
        }

        units = new Constant.String(
            UNIT,
            unitName,
            "",
            "List of units known as containing a ConstantSet and/or a Logger");

        // Initialize units using the constant 'units'
        StringTokenizer st = new StringTokenizer(units.getValue(), SEPARATOR);

        storeIt = false;

        while (st.hasMoreTokens()) {
            String unit = st.nextToken();

            try {
                //log ("pre-loading '" + unit + "'...");
                Class.forName(unit); // This loads its ConstantSet and Logger
                                     //log ("unit '" + unit + "' pre-loaded");
            } catch (ClassNotFoundException ex) {
                System.err.println("*** Cannot load ConstantSet " + unit);
            }
        }

        storeIt = true;

        // Save the latest set of Units
        storeUnits();

        //log("all units have been pre-loaded from " + main);
    }

    //-------------//
    // searchUnits //
    //-------------//
    /**
     * Search for all the units for which the provided string is found in the
     * unit name or the unit description.
     * @param string the string to search for
     * @return the set (perhaps empty) of the matching units
     */
    public Set<Constant> searchUnits (String string)
    {
        String        str = string.toLowerCase();
        Set<Constant> found = new LinkedHashSet<Constant>();

        for (Node node : mapOfNodes.values()) {
            if (node instanceof UnitNode) {
                UnitNode    unit = (UnitNode) node;
                ConstantSet set = unit.getConstantSet();

                if (set != null) {
                    for (int i = 0; i < set.size(); i++) {
                        Constant constant = set.getConstant(i);

                        if (constant.getName()
                                    .toLowerCase()
                                    .contains(str) ||
                            constant.getDescription()
                                    .toLowerCase()
                                    .contains(str)) {
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
     * Include a Unit in the hierarchy
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

            if (storeIt) {
                // Make this unit name permanent
                storeUnits();
            }
        }
    }

    //-------------//
    // dumpStrings //
    //-------------//
    private void dumpStrings (String             title,
                              Collection<String> strings)
    {
        System.out.println("\n" + title + ":");

        for (int i = 0; i < title.length(); i++) {
            System.out.print("-");
        }

        System.out.println();

        for (String string : strings) {
            System.out.println(string);
        }
    }

    //    //-----//
    //    // log //
    //    //-----//
    //    /**
    //     * Poor-man implementation of a log feature, since the normal Logger stuff
    //     * is not yet available
    //     */
    //    private static void log (String msg)
    //    {
    //        System.out.println("UnitManager:: " + msg);
    //    }

    //--------------//
    // retrieveUnit //
    //--------------//
    /**
     * Looks for the unit with given name. If the unit does not exist, it is
     * created and inserted in the hierarchy
     *
     * @param name the name of the desired unit
     *
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
            logger.severe("Unit with same name as package " + name);
        }

        return null;
    }

    //------------//
    // storeUnits //
    //------------//
    /**
     * Build a string by concatenating all node names and store it to disk for
     * subsequent runs.
     */
    private void storeUnits ()
    {
        //log("storing units");

        // Update the constant 'units' according to current units content
        StringBuilder buf = new StringBuilder(1024);

        for (String name : mapOfNodes.keySet()) {
            Node node = getNode(name);

            if (node instanceof UnitNode) {
                if (buf.length() > 0) {
                    buf.append(SEPARATOR);
                }

                buf.append(name);
            }
        }

        // Side-effect: all constants are stored to disk
        units.setValue(buf.toString());

        //log(units.getName() + "=" + units.getValue());
    }

    //---------------//
    // updateParents //
    //---------------//
    /**
     * Update the chain of parents of a Unit, by walking up all the package
     * names found in the fully qualified Unit name, creating PackageNodes when
     * needed, or adding a new child to an existing PackageNode.
     *
     * @param unit the Unit whose chain of parents is to be updated
     */
    private void updateParents (UnitNode unit)
    {
        String name = unit.getName();
        int    length = name.length();
        Node   child = unit;

        for (int i = name.lastIndexOf('.', length - 1); i >= 0;
             i = name.lastIndexOf('.', i - 1)) {
            String parent = name.substring(0, i);
            Node   obj = mapOfNodes.get(parent);

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
        getRoot()
            .addChild(child);
    }
}
