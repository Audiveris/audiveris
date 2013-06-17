//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e T r e e                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.Score;
import omr.score.entity.Container;

import omr.ui.MainGui;

import omr.util.Dumper;
import omr.util.Dumping.PackageRelevance;
import omr.util.Dumping.Relevance;
import omr.util.Navigable;

import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.WindowConstants;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Class {@code ScoreTree} provides a user interface (a frame) where the
 * whole score hierarchy can be browsed as a tree.
 *
 * @author Hervé Bitteur
 */
public class ScoreTree
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(ScoreTree.class);

    /** The filter for relevant classes and fields */
    private static final Relevance filter = new PackageRelevance(
            Main.class.getPackage());

    //~ Instance fields --------------------------------------------------------
    /** Concrete UI component */
    private final JPanel component;

    /** The right panel for HTML display */
    private final JEditorPane htmlPane;

    /** The related score */
    private final Score score;

    /** Cache to avoid recomputing sets of children */
    private final HashMap<Object, List<Object>> nodeMap = new HashMap<>();

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

        /** The tree entity */
        JTree tree = new JTree(model);

        // Build left-side view
        JScrollPane treeView = new JScrollPane(tree);

        // Build right-side view
        htmlPane = new JEditorPane("text/html", "");
        htmlPane.setEditable(false);

        JScrollPane htmlView = new JScrollPane(htmlPane);

        // Allow only single selections
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Display lines to explicit relationships
        tree.putClientProperty("JTree.lineStyle", "Angled");

        // Wire the two views together. Use a selection listener
        // created with an anonymous inner-class adapter.
        // Listen for when the selection changes.
        tree.addTreeSelectionListener(new SelectionListener());

        // To be notified of expansion / collapse actions (debug ...)
        ///tree.addTreeExpansionListener(new ExpansionListener());

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

    //----------//
    // getFrame //
    //----------//
    /**
     * Report the enclosing frame of this entity
     *
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
            frame.getContentPane().add(toolBar, BorderLayout.NORTH);

            // Set up the views, and display it all
            JButton refreshButton = new JButton(
                    new AbstractAction()
            {
                @Override
                public void actionPerformed (ActionEvent e)
                {
                    refresh();
                }
            });
            refreshButton.setName("refreshButton");
            toolBar.add(refreshButton);
            frame.add(component);

            // Resources injection
            ResourceMap resource = MainGui.getInstance().getContext().
                    getResourceMap(getClass());
            resource.injectComponents(frame);
            frame.setTitle(
                    resource.getString("frameTitleMask", score.getRadix()));
        }

        return frame;
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

    //-----------------//
    // NamedCollection //
    //-----------------//
    private static class NamedCollection
    {
        //~ Instance fields ----------------------------------------------------

        private final String name;

        private final Collection<?> collection;

        //~ Constructors -------------------------------------------------------
        public NamedCollection (String name,
                                Collection<?> collection)
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

    //-----------//
    // NamedData //
    //-----------//
    private static class NamedData
    {
        //~ Instance fields ----------------------------------------------------

        private final String name;

        private final Object data;

        //~ Constructors -------------------------------------------------------
        public NamedData (String name,
                          Object data)
        {
            this.name = name;
            this.data = data;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public String toString ()
        {
            return name + ":" + data;
        }
    }

    //-------//
    // Model //
    //-------//
    // This adapter converts the current Score into a JTree model.
    private class Model
            implements TreeModel
    {
        //~ Instance fields ----------------------------------------------------

        private List<TreeModelListener> listeners = new ArrayList<>();

        private Score score;

        //~ Constructors -------------------------------------------------------
        //---------//
        // Model //
        //---------//
        public Model (Score score)
        {
            this.score = score;
        }

        //~ Methods ------------------------------------------------------------
        //----------------------//
        // addTreeModelListener //
        //----------------------//
        @Override
        public void addTreeModelListener (TreeModelListener listener)
        {
            if ((listener != null) && !listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        //----------//
        // getChild //
        //----------//
        @Override
        public Object getChild (Object parent,
                                int index)
        {
            return getRelevantChildren(parent).toArray()[index];
        }

        //---------------//
        // getChildCount //
        //---------------//
        @Override
        public int getChildCount (Object parent)
        {
            return getRelevantChildren(parent).size();
        }

        //-----------------//
        // getIndexOfChild //
        //-----------------//
        @Override
        public int getIndexOfChild (Object parent,
                                    Object child)
        {
            int i = 0;

            for (Iterator<Object> it = getRelevantChildren(parent).iterator();
                    it.hasNext();) {
                if (it.next() == child) {
                    return i;
                }

                i++;
            }

            throw new RuntimeException(
                    "'" + child + "' not child of '" + parent + "'");
        }

        //---------//
        // getRoot //
        //---------//
        @Override
        public Object getRoot ()
        {
            return score;
        }

        //--------//
        // isLeaf //
        //--------//
        @Override
        public boolean isLeaf (Object node)
        {
            // Determines whether the icon shows up to the left.
            // Return true for any node with no children
            logger.debug("isLeaf. node={} {}", node, getChildCount(node) == 0);

            return getChildCount(node) == 0;
        }

        //------------//
        // refreshAll //
        //------------//
        public void refreshAll ()
        {
            nodeMap.clear();

            TreeModelEvent modelEvent = new TreeModelEvent(
                    this,
                    new Object[]{score});

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
        @Override
        public void removeTreeModelListener (TreeModelListener listener)
        {
            if (listener != null) {
                listeners.remove(listener);
            }
        }

        //---------------------//
        // valueForPathChanged //
        //---------------------//
        @Override
        public void valueForPathChanged (TreePath path,
                                         Object newValue)
        {
            // Null. We won't be making changes in the GUI.  If we did, we would
            // ensure the new value was really new and then fire a
            // TreeNodesChanged event.
        }

        //---------------------//
        // getRelevantChildren //
        //---------------------//
        /**
         * Report the set of children of the provided node that are
         * relevant for display in the tree hierarchy (left pane)
         *
         * @param node the node to investigate
         * @return the collection of relevant children
         */
        private List<Object> getRelevantChildren (Object node)
        {
            // First check the cache
            List<Object> relevants = nodeMap.get(node);

            if (relevants != null) {
                return relevants;
            }

            // Not found, so let's build it
            logger.debug("Retrieving relevants of {} {}",
                    node, node.getClass());

            // Case of Named Collection
            if (node instanceof NamedCollection) {
                ///logger.info("named collection: " + node);
                NamedCollection nc = (NamedCollection) node;
                relevants = new ArrayList<>();
                nodeMap.put(node, relevants);

                for (Object n : nc.collection) {
                    if (isRelevant(n)) {
                        relevants.add(n);
                    }
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("{} nb={}", node, relevants.size());
                }

                return relevants;
            }

            // Case of Named Data
            if (node instanceof NamedData) {
                ///logger.info("named data: " + node);
                relevants = getRelevantChildren(((NamedData) node).data);
                nodeMap.put(node, relevants);

                if (logger.isDebugEnabled()) {
                    logger.debug("{} nb={}", node, relevants.size());
                }

                return relevants;
            }

            Class<?> classe = node.getClass();
            relevants = new ArrayList<>();
            nodeMap.put(node, relevants);

            // Walk up the inheritance tree
            do {
                // Browse the declared fields of the class at hand
                for (Field field : classe.getDeclaredFields()) {
                    // Skip field if annotated as non navigable
                    Navigable navigable = field.getAnnotation(Navigable.class);

                    if ((navigable != null) && (navigable.value() == false)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("skipping {}", field);
                        }

                        continue;
                    }

                    ///logger.info("fieldName:" + field.getName());
                    try {
                        // No static or inner class
                        if (!filter.isFieldRelevant(field)) {
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

                        Class<?> objClass = object.getClass();

                        // Skip primitive members
                        if (objClass.isPrimitive()) {
                            ///System.out.println(" [primitive]");
                            continue;
                        }

                        // Special handling of collections
                        if (object instanceof Collection) {
                            Collection<?> coll = (Collection<?>) object;

                            if (!coll.isEmpty()) {
                                relevants.add(
                                        new NamedCollection(field.getName(),
                                        coll));
                            }

                            continue;
                        }

                        if (!filter.isClassRelevant(objClass)) {
                            ///System.out.println(" [CLASS not relevant]");
                            continue;
                        }

                        // No leaf on left pane
                        if (getChildCount(object) == 0) {
                            continue;
                        }

                        ///System.out.println(" ...OK");
                        relevants.add(new NamedData(field.getName(), object));
                    } catch (Exception ex) {
                        logger.warn("Error in accessing field", ex);
                    }
                }

                // Walk up the inheritance tree
                classe = classe.getSuperclass();
            } while (filter.isClassRelevant(classe));

            if (logger.isDebugEnabled()) {
                logger.debug("{} nb={}", node, relevants.size());
            }

            return relevants;
        }

        //------------//
        // isRelevant //
        //------------//
        private boolean isRelevant (Object node)
        {
            //            return !isLeaf(node);

            // We display dummy containers only when they are not empty
            if (constants.hideEmptyDummies.getValue()
                && (node instanceof NamedCollection
                    || (node instanceof Container))) {
                return getChildCount(node) > 0;
            } else {
                return true;
            }
        }
    }

    //-------------------//
    // SelectionListener //
    //-------------------//
    private class SelectionListener
            implements TreeSelectionListener
    {
        //~ Methods ------------------------------------------------------------

        @Override
        public void valueChanged (TreeSelectionEvent e)
        {
            TreePath p = e.getNewLeadSelectionPath();

            if (p != null) {
                Object obj = p.getLastPathComponent();

                if (obj instanceof NamedData) {
                    NamedData nd = (NamedData) obj;
                    obj = nd.data;
                }

                htmlPane.setText(new Dumper.Html(filter, obj).toString());
            }
        }
    }
}
