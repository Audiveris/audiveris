//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e T r e e                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.score.Score;
import omr.score.entity.Container;

import omr.ui.MainGui;

import omr.util.Dumper;
import omr.util.Implement;

import org.jdesktop.application.ResourceMap;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

/**
 * Class <code>ScoreTree</code> provides a user interface (a frame) where the
 * whole score hierarchy can be browsed as a tree.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreTree
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreTree.class);

    //~ Instance fields --------------------------------------------------------

    /** Concrete UI component */
    private final JPanel component;

    /** The right panel for HTML display */
    private final JEditorPane htmlPane;

    /** The related score */
    private final Score score;

    /** Cache to avoid recomputing sets of children */
    private final HashMap<Object, LinkedHashSet<Object>> nodeMap = new HashMap<Object, LinkedHashSet<Object>>();

    /** The tree entity */
    private final JTree tree;

    /** The tree model */
    private final Model model;

    /** The enclosing frame */
    private JFrame frame;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // ScoreTree //
    //-----------//
    /**
     * Creates a new ScoreTree object.
     *
     * @param score the related score
     */
    public ScoreTree (Score score)
    {
        this.score = score;

        component = new JPanel();

        // Set up the tree
        model = new Model(score);
        ///model.addTreeModelListener(new ModelListener()); // Debug
        tree = new JTree(model);

        // Build left-side view
        JScrollPane treeView = new JScrollPane(tree);

        // Build right-side view
        htmlPane = new JEditorPane("text/html", "");
        htmlPane.setEditable(false);

        JScrollPane htmlView = new JScrollPane(htmlPane);

        // Allow only single selections
        tree.getSelectionModel()
            .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Display lines to explicit relationships
        tree.putClientProperty("JTree.lineStyle", "Angled");

        // Wire the two views together. Use a selection listener
        // created with an anonymous inner-class adapter.
        // Listen for when the selection changes.
        tree.addTreeSelectionListener(new SelectionListener());

        // To be notified of expansion / collapse actions (debug ...)
        tree.addTreeExpansionListener(new ExpansionListener());

        // Build split-pane view
        JSplitPane splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            treeView,
            htmlView);
        splitPane.setName("treeHtmlSplitPane");
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setDividerSize(2);

        // Add GUI components
        component.setLayout(new BorderLayout());
        component.add("Center", splitPane);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getFrame //
    //----------//
    /**
     * Report the enclosing frame of this entity
     * @return the frame of the score browser
     */
    public JFrame getFrame ()
    {
        if (frame == null) {
            // Set up a GUI framework
            frame = new JFrame();
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setName("scoreTreeFrame");

            // Add a REFRESH button
            JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
            frame.getContentPane()
                 .add(toolBar, BorderLayout.NORTH);

            // Set up the views, and display it all
            JButton refreshButton = new JButton(
                new AbstractAction() {
                        public void actionPerformed (ActionEvent e)
                        {
                            refresh();
                        }
                    });
            refreshButton.setName("refreshButton");
            toolBar.add(refreshButton);
            frame.add(component);

            // Resources injection
            ResourceMap resource = MainGui.getInstance()
                                          .getContext()
                                          .getResourceMap(ScoreTree.class);
            resource.injectComponents(frame);
            frame.setTitle(
                resource.getString("frameTitleMask", score.getRadix()));
        }

        return frame;
    }

    //-------//
    // close //
    //-------//
    public void close ()
    {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the whole display, to be in sync with latest data
     */
    public void refresh ()
    {
        model.refreshAll();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Should we hide empty dummy containers */
        Constant.Boolean hideEmptyDummies = new Constant.Boolean(
            false,
            "Should we hide empty dummy containers");
    }

    //-------//
    // Model //
    //-------//
    // This adapter converts the current Score into a JTree model.
    private class Model
        implements TreeModel
    {
        //~ Instance fields ----------------------------------------------------

        private List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();
        private Score                   score;

        //~ Constructors -------------------------------------------------------

        //---------//
        // Model //
        //---------//
        public Model (Score score)
        {
            this.score = score;
        }

        //~ Methods ------------------------------------------------------------

        //----------//
        // getChild //
        //----------//
        @Implement(TreeModel.class)
        public Object getChild (Object parent,
                                int    index)
        {
            return getRelevantChildren(parent)
                       .toArray()[index];
        }

        //---------------//
        // getChildCount //
        //---------------//
        @Implement(TreeModel.class)
        public int getChildCount (Object parent)
        {
            return getRelevantChildren(parent)
                       .size();
        }

        //-----------------//
        // getIndexOfChild //
        //-----------------//
        @Implement(TreeModel.class)
        public int getIndexOfChild (Object parent,
                                    Object child)
        {
            int i = 0;

            for (Iterator it = getRelevantChildren(parent)
                                   .iterator(); it.hasNext();) {
                if (it.next() == child) {
                    return i;
                }

                i++;
            }

            throw new RuntimeException(
                "'" + child + "' not child of '" + parent + "'");
        }

        //--------//
        // isLeaf //
        //--------//
        @Implement(TreeModel.class)
        public boolean isLeaf (Object node)
        {
            // Determines whether the icon shows up to the left.
            // Return true for any node with no children
            ///logger.info("isLeaf. node=" + node);
            return getChildCount(node) == 0;
        }

        //---------//
        // getRoot //
        //---------//
        @Implement(TreeModel.class)
        public Object getRoot ()
        {
            return score;
        }

        //----------------------//
        // addTreeModelListener //
        //----------------------//
        @Implement(TreeModel.class)
        public void addTreeModelListener (TreeModelListener listener)
        {
            if ((listener != null) && !listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        //------------//
        // refreshAll //
        //------------//
        public void refreshAll ()
        {
            nodeMap.clear();

            TreeModelEvent modelEvent = new TreeModelEvent(
                this,
                new Object[] { score });

            for (TreeModelListener listener : listeners) {
                listener.treeStructureChanged(modelEvent);
            }
        }

        //-------------//
        // refreshPath //
        //-------------//
        public void refreshPath (TreePath path)
        {
            TreeModelEvent modelEvent = new TreeModelEvent(this, path);

            for (TreeModelListener listener : listeners) {
                listener.treeStructureChanged(modelEvent);
            }
        }

        //-------------------------//
        // removeTreeModelListener //
        //-------------------------//
        @Implement(TreeModel.class)
        public void removeTreeModelListener (TreeModelListener listener)
        {
            if (listener != null) {
                listeners.remove(listener);
            }
        }

        //---------------------//
        // valueForPathChanged //
        //---------------------//
        @Implement(TreeModel.class)
        public void valueForPathChanged (TreePath path,
                                         Object   newValue)
        {
            // Null. We won't be making changes in the GUI.  If we did, we would
            // ensure the new value was really new and then fire a
            // TreeNodesChanged event.
        }

        //------------//
        // isRelevant //
        //------------//
        private boolean isRelevant (Object node)
        {
            // We display dummy containers only when they are not empty
            if (constants.hideEmptyDummies.getValue() &&
                (node instanceof NamedCollection ||
                (node instanceof Container))) {
                return getChildCount(node) > 0;
            } else {
                return true;
            }
        }

        //---------------------//
        // getRelevantChildren //
        //---------------------//
        /**
         * Report the set of children of the provided node that are
         * relevant for display in the tree hierarchy
         * @param node the node to investigate
         * @return the collection of relevant children
         */
        private LinkedHashSet<Object> getRelevantChildren (Object node)
        {
            // First check the cache
            LinkedHashSet<Object> relevants = null;

            // First, check the cache
            relevants = nodeMap.get(node);

            if (relevants != null) {
                return relevants;
            }

            // Not found, so let's build it
            if (logger.isFineEnabled()) {
                logger.fine("Retrieving relevants of " + node);
            }

            relevants = new LinkedHashSet<Object>();

            Class cl = node.getClass();

            if (node instanceof NamedCollection) {
                ///System.out.println("named collection");
                NamedCollection nc = (NamedCollection) node;

                for (Object n : nc.collection) {
                    if (isRelevant(n)) {
                        relevants.add(n);
                    }
                }
            } else {
                do {
                    ///System.out.println("cl=" + cl);

                    // Process the class at hand
                    for (Field field : cl.getDeclaredFields()) {
                        ///System.out.print("fieldName:" + field.getName());
                        try {
                            // No static or inner class
                            if (!Dumper.isFieldRelevant(field)) {
                                ///System.out.println(" [field not relevant]");
                                continue;
                            }

                            field.setAccessible(true);

                            Object object = field.get(node);

                            // No null field
                            if (object == null) {
                                ///System.out.println(" [null]");
                                continue;
                            }

                            Class objClass = object.getClass();

                            ///System.out.print(" objClass=" + objClass.getName());

                            // Skip primitive members
                            if (objClass.isPrimitive()) {
                                ///System.out.println(" [primitive]");
                                continue;
                            }

                            if (object instanceof Collection) {
                                Collection coll = (Collection) object;

                                if (!coll.isEmpty()) {
                                    relevants.add(
                                        new NamedCollection(
                                            field.getName(),
                                            coll));

                                    ///System.out.println(" ...collection OK");
                                } else {
                                    ///System.out.println(" [empty collection]");
                                }
                            } else {
                                if (!Dumper.isClassRelevant(objClass)) {
                                    ///System.out.println(" [CLASS not relevant]");
                                    continue;
                                }

                                relevants.add(object);

                                ///System.out.println(" ...OK");
                            }
                        } catch (Exception ex) {
                            logger.warning("Error in accessing field", ex);
                        }
                    }

                    // Walk up the inheritance tree
                    cl = cl.getSuperclass();
                } while (Dumper.isClassRelevant(cl));
            }

            // Cache the result
            nodeMap.put(node, relevants);

            ///System.out.println("nb=" + relevants.size());
            return relevants;
        }
    }

    //-----------------//
    // NamedCollection //
    //-----------------//
    private static class NamedCollection
    {
        //~ Instance fields ----------------------------------------------------

        private String     name;
        private Collection collection;

        //~ Constructors -------------------------------------------------------

        public NamedCollection (String     name,
                                Collection collection)
        {
            this.name = name;
            this.collection = collection;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            return name;
        }
    }

    //-------------------//
    // ExpansionListener //
    //-------------------//
    private class ExpansionListener
        implements TreeExpansionListener
    {
        //~ Methods ------------------------------------------------------------

        public void treeCollapsed (TreeExpansionEvent e)
        {
            ///logger.warning("treeCollapsed " + e.getPath());
        }

        public void treeExpanded (TreeExpansionEvent e)
        {
            ///logger.warning("treeExpanded " + e.getPath());

            // Check that we don't duplicate nodes higher in the path
            Object                node = e.getPath()
                                          .getLastPathComponent();
            LinkedHashSet<Object> set = model.getRelevantChildren(node);

            boolean               modified = false;

            for (TreePath path = e.getPath(); path != null;
                 path = path.getParentPath()) {
                if (path != e.getPath()) {
                    Object                n = path.getLastPathComponent();
                    LinkedHashSet<Object> s = model.getRelevantChildren(n);

                    if (set.removeAll(s)) {
                        modified = true;
                    }
                }

                if (set.remove(path.getLastPathComponent())) {
                    modified = true;
                }
            }

            //            // Remove also nodes that cannot be expanded (leaves)
            //            for (Iterator<Object> it = set.iterator(); it.hasNext();) {
            //                Object n = it.next();
            //
            //                if (model.isLeaf(n)) {
            //                    ///logger.warning("removing leaf " + n);
            //                    it.remove();
            //                    modified = true;
            //                }
            //            }
            if (modified) {
                nodeMap.put(node, set);
                model.refreshPath(e.getPath());
            }
        }
    }

    //    //---------------//
    //    // ModelListener //
    //    //---------------//
    //    private class ModelListener
    //        implements TreeModelListener
    //    {
    //        //~ Methods ------------------------------------------------------------
    //
    //        public void treeNodesChanged (TreeModelEvent e)
    //        {
    //            logger.warning("treeNodesChanged " + e);
    //        }
    //
    //        public void treeNodesInserted (TreeModelEvent e)
    //        {
    //            logger.warning("treeNodesInserted" + e);
    //        }
    //
    //        public void treeNodesRemoved (TreeModelEvent e)
    //        {
    //            logger.warning("treeNodesRemoved " + e);
    //        }
    //
    //        public void treeStructureChanged (TreeModelEvent e)
    //        {
    //            logger.warning("treeStructureChanged " + e);
    //        }
    //    }

    //-------------------//
    // SelectionListener //
    //-------------------//
    private class SelectionListener
        implements TreeSelectionListener
    {
        //~ Methods ------------------------------------------------------------

        public void valueChanged (TreeSelectionEvent e)
        {
            TreePath p = e.getNewLeadSelectionPath();

            if (p != null) {
                htmlPane.setText(Dumper.htmlDumpOf(p.getLastPathComponent()));
            }
        }
    }
}
