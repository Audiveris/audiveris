//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B o o k B r o w s e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.Main;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.WeakGlyph;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.util.Dumper;
import org.audiveris.omr.util.Dumping.PackageRelevance;
import org.audiveris.omr.util.Dumping.Relevance;
import org.audiveris.omr.util.Navigable;

import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
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
 * Class {@code BookBrowser} provides a user interface (a frame) where the whole book
 * information can be browsed as a tree.
 *
 * @author Hervé Bitteur
 */
public class BookBrowser
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BookBrowser.class);

    /** The filter for relevant classes and fields. */
    private static final Relevance filter = new PackageRelevance(Main.class.getPackage());

    /** Concrete UI component. */
    private final JPanel component;

    /** The right panel for HTML display. */
    private final JEditorPane htmlPane;

    /** The related book. */
    private final Book book;

    /** The tree model. */
    private final Model model;

    /** The enclosing frame. */
    private JFrame frame;

    /**
     * Creates a new {@code BookBrowser} object.
     *
     * @param book the related book
     */
    public BookBrowser (Book book)
    {
        this.book = book;

        component = new JPanel();

        // Set up the tree
        model = new Model(book);

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
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Display lines to explicit relationships
        tree.putClientProperty("JTree.lineStyle", "Angled");

        // Wire the two views together. Use a selection listener
        // created with an anonymous inner-class adapter.
        // Listen for when the selection changes.
        tree.addTreeSelectionListener(new SelectionListener());

        // To be notified of expansion / collapse actions (debug ...)
        ///tree.addTreeExpansionListener(new ExpansionListener());
        // Build split-pane view
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeView, htmlView);
        splitPane.setName("treeHtmlSplitPane");
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setDividerSize(2);

        // Add GUI components
        component.setLayout(new BorderLayout());
        component.add("Center", splitPane);
    }

    //-------//
    // close //
    //-------//
    /**
     * Close the browser.
     */
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
            frame.setName("BookBrowserFrame");  // For SAF life cycle

            // Add a REFRESH button
            JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
            frame.getContentPane().add(toolBar, BorderLayout.NORTH);

            // Set up the views, and display it all
            JButton refreshButton = new JButton(new AbstractAction()
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
            ResourceMap resource = OmrGui.getApplication().getContext().getResourceMap(getClass());
            resource.injectComponents(frame);
            frame.setTitle(resource.getString("frameTitleMask", book.getRadix()));
            frame.setIconImage(OmrGui.getApplication().getMainFrame().getIconImage());
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

    //-------//
    // Model //
    //-------//
    // This adapter converts the current Book into a JTree model.
    private class Model
            implements TreeModel
    {

        private final List<TreeModelListener> listeners = new ArrayList<>();

        private final Book book;

        Model (Book book)
        {
            this.book = book;
        }

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
            return getRelevantChildren(parent, null).get(index);
        }

        //---------------//
        // getChildCount //
        //---------------//
        @Override
        public int getChildCount (Object parent)
        {
            return getRelevantChildren(parent, null).size();
        }

        //-----------------//
        // getIndexOfChild //
        //-----------------//
        @Override
        public int getIndexOfChild (Object parent,
                                    Object child)
        {
            return getRelevantChildren(parent, null).indexOf(child);
        }

        //---------//
        // getRoot //
        //---------//
        @Override
        public Object getRoot ()
        {
            return book;
        }

        //--------//
        // isLeaf //
        //--------//
        @Override
        public boolean isLeaf (Object node)
        {
            // Determines whether the icon shows up to the left.
            // Return true for any node with no children
            return getChildCount(node) == 0;
        }

        //------------//
        // refreshAll //
        //------------//
        public void refreshAll ()
        {
            TreeModelEvent modelEvent = new TreeModelEvent(this, new Object[]{book});

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
         * Report the list of children of the provided node that are
         * relevant for display in the tree hierarchy (left pane)
         *
         * @param node the node to investigate
         * @param sig  for a relation: the originating Inter
         * @return the list of relevant children
         */
        private List<Object> getRelevantChildren (Object node,
                                                  Inter orgInter)
        {
            final List<Object> relevants;

            // Not found, so let's build it
            logger.debug("Retrieving relevants of {} {}", node, node.getClass());

            // Case of Named Collection
            if (node instanceof NamedCollection) {
                logger.debug("named collection: " + node);
                NamedCollection nc = (NamedCollection) node;
                relevants = new ArrayList<>();

                for (Object n : nc.collection) {
                    if (isRelevant(n)) {
                        relevants.add(n);
                    }
                }

                logger.debug("{} nb={}", node, relevants.size());
                return relevants;
            }

            // Case of Named Data
            if (node instanceof NamedData) {
                logger.debug("named data: " + node);
                NamedData nd = (NamedData) node;
                relevants = getRelevantChildren(nd.data, nd.orgInter);

                logger.debug("{} nb={}", node, relevants.size());
                return relevants;
            }

            ///logger.info("standard node: " + node);
            Class<?> classe = node.getClass();
            relevants = new ArrayList<>();

            if (node instanceof Inter) {
                // Add inter relations first if any
                Inter inter = (Inter) node;
                SIGraph sig = inter.getSig();

                for (Relation rel : sig.edgesOf(inter)) {
                    relevants.add(new NamedData(rel.getClass().getSimpleName(), rel, inter));
                }
            } else if (node instanceof Relation) {
                // Add relation source and target first
                Relation rel = (Relation) node;
                SIGraph sig = orgInter.getSig();
                Inter source = sig.getEdgeSource(rel);
                Inter target = sig.getEdgeTarget(rel);

                if (source != orgInter) {
                    relevants.add(new NamedData("source", source));
                } else if (!constants.hideOriginatingInter.isSet()) {
                    relevants.add(new NamedData("(src)", source));
                }

                if (target != orgInter) {
                    relevants.add(new NamedData("target", target));
                } else if (!constants.hideOriginatingInter.isSet()) {
                    relevants.add(new NamedData("(tgt)", target));
                }
            }

            // Walk up the inheritance tree
            do {
                // Browse the declared fields of the class at hand
                for (Field field : classe.getDeclaredFields()) {
                    // Skip field if annotated as non navigable
                    Navigable navigable = field.getAnnotation(Navigable.class);

                    if ((navigable != null) && (navigable.value() == false)) {
                        logger.debug("non-navigable {}", field);
                        continue;
                    }

                    try {
                        // No static or inner class
                        if (!filter.isFieldRelevant(field)) {
                            continue;
                        }

                        field.setAccessible(true);
                        Object object = field.get(node);

                        // No null field
                        if (object == null) {
                            continue;
                        }

                        Class<?> objClass = object.getClass();

                        // Skip primitive members
                        if (objClass.isPrimitive()) {
                            continue;
                        }

                        // Special handling of collections
                        if (object instanceof Collection) {
                            Collection<?> coll = (Collection<?>) object;

                            if (!coll.isEmpty()) {
                                relevants.add(new NamedCollection(field.getName(), coll));
                            }

                            continue;
                        }

                        if (!filter.isClassRelevant(objClass)) {
                            continue;
                        }

                        relevants.add(new NamedData(field.getName(), object));
                    } catch (IllegalAccessException |
                             IllegalArgumentException |
                             SecurityException ex) {
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
            // We display dummy containers only when they are not empty
            if (constants.hideEmptyDummies.isSet() && node instanceof NamedCollection) {
                return getChildCount(node) > 0;
            } else {
                if (node instanceof WeakReference) {
                    return ((WeakReference) node).get() != null;
                }

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

        @Override
        public void valueChanged (TreeSelectionEvent e)
        {
            try {
                TreePath p = e.getNewLeadSelectionPath();
                if (p != null) {
                    Object obj = p.getLastPathComponent();

                    if (obj instanceof NamedData) {
                        NamedData nd = (NamedData) obj;
                        obj = nd.data;
                    }

                    // Publish selection?
                    if (obj instanceof Inter) {
                        Inter inter = (Inter) obj;
                        SIGraph sig = inter.getSig();

                        if (sig != null) {
                            sig.publish(inter);
                        }
                    } else if (obj instanceof Glyph) {
                        Glyph glyph = (Glyph) obj;
                        GlyphIndex index = glyph.getIndex();
                        index.publish(glyph);
                    } else if (obj instanceof WeakGlyph) {
                        WeakGlyph weakGlyph = (WeakGlyph) obj;
                        Glyph glyph = weakGlyph.get();

                        if (glyph != null) {
                            GlyphIndex index = glyph.getIndex();
                            index.publish(glyph);
                        }
                    } else {
                        // Empty selections
                        StubsController controller = StubsController.getInstance();
                        SheetStub stub = controller.getSelectedStub();

                        if (stub.hasSheet()) {
                            Sheet sheet = stub.getSheet();
                            sheet.getInterIndex().publish(null);
                            sheet.getGlyphIndex().publish(null);
                            sheet.getLocationService().publish(
                                    new LocationEvent(
                                            this,
                                            SelectionHint.LOCATION_INIT,
                                            MouseMovement.PRESSING,
                                            null));
                        }
                    }

                    if (obj instanceof WeakReference) {
                        Object o = ((Reference) obj).get();
                        htmlPane.setText(new Dumper.Html(filter, o).toString());
                    } else {
                        htmlPane.setText(new Dumper.Html(filter, obj).toString());
                    }
                }
            } catch (Throwable ex) {
                logger.warn("BookBrowser error: " + ex, ex);
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean hideEmptyDummies = new Constant.Boolean(
                false,
                "Should we hide empty dummy containers");

        private final Constant.Boolean hideOriginatingInter = new Constant.Boolean(
                true,
                "Should we hide the originating inter when expanding a relation?");
    }

    //-----------------//
    // NamedCollection //
    //-----------------//
    private static class NamedCollection
    {

        private final String name;

        private final Collection<?> collection;

        NamedCollection (String name,
                         Collection<?> collection)
        {
            this.name = name;
            this.collection = collection;
        }

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

        private final String name;

        private final Object data;

        private final Inter orgInter;

        NamedData (String name,
                   Object data,
                   Inter orgInter)
        {
            this.name = name;
            this.data = data;
            this.orgInter = orgInter;
        }

        NamedData (String name,
                   Object data)
        {
            this(name, data, null);
        }

        @Override
        public String toString ()
        {
            return name + ":" + data;
        }
    }
}
