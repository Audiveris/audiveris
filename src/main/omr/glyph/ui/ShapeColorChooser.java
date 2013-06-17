//----------------------------------------------------------------------------//
//                                                                            //
//                     S h a p e C o l o r C h o o s e r                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Shape;
import omr.glyph.ShapeSet;

import omr.ui.MainGui;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code ShapeColorChooser} offers a convenient user interface
 * to choose proper color for each glyph shape.
 * It is derived from the Sun Java tutorial ColorChooserDemo.
 *
 * <p>The right part of the panel is used by a classic color chooser
 *
 * <p>The left part of the panel is used by the shape at hand, with a way to
 * browse through the various defined shapes.
 *
 * <p>The strategy is the following: First the predefined shape ranges (such as
 * "Physicals", "Bars", "Clefs", ...) have their own color defined. Then, each
 * individual shape within these shape ranges has its color assigned by default
 * to the color of the containing range, unless a color is specifically assigned
 * to this individual shape.
 *
 * @author Hervé Bitteur
 */
public class ShapeColorChooser
        implements ChangeListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            ShapeColorChooser.class);

    private static JFrame frame;

    //~ Instance fields --------------------------------------------------------
    /** The classic color chooser utility */
    private JColorChooser colorChooser;

    /** Color chosen in the JColorChooser utility */
    private Color chosenColor;

    /** UI component */
    private JPanel component;

    /** To select shape range */
    private RangesPane ranges;

    /** To select shape (within selected range) */
    private ShapesPane shapes;

    //~ Constructors -----------------------------------------------------------
    //-------------------//
    // ShapeColorChooser //
    //-------------------//
    /**
     * Create an instance of ShapeColorChooser (should be improved to always
     * reuse the same instance. TODO)
     */
    private ShapeColorChooser ()
    {
        component = new JPanel(new BorderLayout());

        // Range Panel
        ranges = new RangesPane();
        shapes = new ShapesPane();

        // Global Panel
        JPanel globalPanel = new JPanel(new BorderLayout());
        globalPanel.add(ranges, BorderLayout.NORTH);
        globalPanel.add(shapes, BorderLayout.SOUTH);
        globalPanel.setPreferredSize(new Dimension(400, 400));

        // Color chooser
        colorChooser = new JColorChooser();
        colorChooser.getSelectionModel()
                .addChangeListener(this);
        colorChooser.setBorder(
                BorderFactory.createTitledBorder("Choose Shape Color"));

        component.add(globalPanel, BorderLayout.CENTER);
        component.add(colorChooser, BorderLayout.EAST);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // showFrame //
    //-----------//
    /**
     * Display the UI frame.
     */
    public static void showFrame ()
    {
        if (frame == null) {
            frame = new JFrame();
            frame.setName("shapeColorChooserFrame");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            frame.add(new ShapeColorChooser().component);

            // Resources injection
            ResourceMap resource = Application.getInstance()
                    .getContext()
                    .getResourceMap(
                    ShapeColorChooser.class);
            resource.injectComponents(frame);
        }

        MainGui.getInstance()
                .show(frame);
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * Triggered when color selection in the color chooser has changed.
     *
     * @param e not used
     */
    @Override
    public void stateChanged (ChangeEvent e)
    {
        chosenColor = colorChooser.getColor();
        ///logger.info("chosenColor: " + chosenColor);
        ranges.colorChanged();
        shapes.colorChanged();
    }

    //~ Inner Classes ----------------------------------------------------------
    //------//
    // Pane //
    //------//
    private abstract class Pane
            extends JPanel
    {
        //~ Instance fields ----------------------------------------------------

        public JLabel banner = new JLabel("", JLabel.CENTER);

        public JPopupMenu menu = new JPopupMenu();

        //~ Constructors -------------------------------------------------------
        public Pane (String title)
        {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder(title));

            banner.setForeground(Color.black);
            banner.setBackground(Color.white);
            banner.setOpaque(true);
            banner.setFont(new Font("SansSerif", Font.BOLD, 18));
            add(banner, BorderLayout.CENTER);
        }

        //~ Methods ------------------------------------------------------------
        public abstract void colorChanged ();

        protected abstract void refreshBanner ();
    }

    //------------//
    // RangesPane //
    //------------//
    private class RangesPane
            extends Pane
    {
        //~ Instance fields ----------------------------------------------------

        public ShapeSet current;

        private SelectAction select = new SelectAction();

        private JButton selectButton = new JButton(select);

        private PasteAction paste = new PasteAction();

        private ActionListener selectionListener = new ActionListener()
        {
            // Called when a range has been selected
            @Override
            public void actionPerformed (ActionEvent e)
            {
                JMenuItem source = (JMenuItem) e.getSource();
                current = ShapeSet.valueOf(source.getText());

                if (current != null) {
                    banner.setText(current.getName());

                    Color color = current.getColor();

                    if (color != null) {
                        //colorChooser.setColor(color);
                        refreshBanner();
                    } else {
                        banner.setForeground(Color.black);
                    }

                    paste.setEnabled(false);
                    shapes.setRange();
                } else {
                    banner.setText("");
                }
            }
        };

        private JButton pasteButton = new JButton(paste);

        //~ Constructors -------------------------------------------------------
        public RangesPane ()
        {
            super("Shape Range");

            add(selectButton, BorderLayout.NORTH);
            add(pasteButton, BorderLayout.SOUTH);

            paste.setEnabled(false);

            buildRangesMenu();
        }

        //~ Methods ------------------------------------------------------------
        // When color chooser selection has been made
        @Override
        public void colorChanged ()
        {
            if (current != null) {
                paste.setEnabled(true);
            }
        }

        @Override
        protected void refreshBanner ()
        {
            if (current != null) {
                banner.setForeground(current.getColor());
            }
        }

        private void buildRangesMenu ()
        {
            menu.removeAll();
            ShapeSet.addAllShapeSets(menu, selectionListener);
        }

        //~ Inner Classes ------------------------------------------------------
        private class PasteAction
                extends AbstractAction
        {
            //~ Constructors ---------------------------------------------------

            public PasteAction ()
            {
                super("Paste");
            }

            //~ Methods --------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                current.setConstantColor(chosenColor);
                setEnabled(false);
                refreshBanner();
                buildRangesMenu();

                // Forward to contained shapes ?
                shapes.setRange();
            }
        }

        private class SelectAction
                extends AbstractAction
        {
            //~ Constructors ---------------------------------------------------

            public SelectAction ()
            {
                super("Select");
            }

            //~ Methods --------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                JButton button = (JButton) e.getSource();
                menu.show(RangesPane.this, button.getX(), button.getY());
            }
        }
    }

    //------------//
    // ShapesPane //
    //------------//
    private class ShapesPane
            extends Pane
    {
        //~ Instance fields ----------------------------------------------------

        public Shape current;

        private ActionListener selectionListener = new ActionListener()
        {
            // Called when a shape has been selected
            @Override
            public void actionPerformed (ActionEvent e)
            {
                JMenuItem source = (JMenuItem) e.getSource();
                current = Shape.valueOf(source.getText());
                banner.setText(current.toString());

                // Check if a specific color is assigned
                Color color = current.getColor();

                if (color == ranges.current.getColor()) {
                    prepareDefaultOption();
                    refreshBanner();
                } else {
                    prepareSpecificOption();
                    banner.setForeground(ranges.current.getColor());
                }
            }
        };

        private CopyAction copy = new CopyAction();

        private CutAction cut = new CutAction();

        private PasteAction paste = new PasteAction();

        private SelectAction select = new SelectAction();

        private JButton selectButton = new JButton(select);

        private boolean isSpecific;

        //~ Constructors -------------------------------------------------------
        public ShapesPane ()
        {
            super("Individual Shape");

            selectButton.setText("-- Select Shape --");
            // No range has been selected yet, so no shape can be selected
            select.setEnabled(false);

            add(selectButton, BorderLayout.NORTH);

            // A series of 3 buttons at the bottom
            JPanel buttons = new JPanel(new GridLayout(1, 3));
            buttons.add(new JButton(cut));
            buttons.add(new JButton(copy));
            buttons.add(new JButton(paste));
            add(buttons, BorderLayout.SOUTH);

            cut.setEnabled(false);
            copy.setEnabled(false);
            paste.setEnabled(false);
        }

        //~ Methods ------------------------------------------------------------
        // When color chooser selection has been made
        @Override
        public void colorChanged ()
        {
            updateActions();
        }

        // When a new range has been selected
        public void setRange ()
        {
            buildShapesMenu();

            select.setEnabled(true);
            selectButton.setText(
                    "-- Select Shape from " + ranges.current.getName() + " --");

            current = null;
            banner.setText("");
            cut.setEnabled(false);
            copy.setEnabled(false);
            paste.setEnabled(false);
        }

        @Override
        protected void refreshBanner ()
        {
            if (current != null) {
                banner.setForeground(current.getColor());
            }
        }

        private void buildShapesMenu ()
        {
            menu.removeAll();

            // Add all shapes within current range
            ShapeSet.addSetShapes(ranges.current, menu, selectionListener);
        }

        private void prepareDefaultOption ()
        {
            isSpecific = true;
            updateActions();
        }

        private void prepareSpecificOption ()
        {
            isSpecific = false;
            updateActions();

            if (chosenColor != null) {
                paste.setEnabled(chosenColor != ranges.current.getColor());
            } else {
                paste.setEnabled(false);
            }
        }

        private void updateActions ()
        {
            if (current != null) {
                cut.setEnabled(isSpecific);
                copy.setEnabled(isSpecific);
                paste.setEnabled(!isSpecific);
            } else {
                cut.setEnabled(false);
                copy.setEnabled(false);
                paste.setEnabled(false);
            }
        }

        //~ Inner Classes ------------------------------------------------------
        private class CopyAction
                extends AbstractAction
        {
            //~ Constructors ---------------------------------------------------

            public CopyAction ()
            {
                super("Copy");
            }

            //~ Methods --------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                colorChooser.setColor(current.getColor());
            }
        }

        private class CutAction
                extends AbstractAction
        {
            //~ Constructors ---------------------------------------------------

            public CutAction ()
            {
                super("Cut");
            }

            //~ Methods --------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                // Drop specific for default
                current.setColor(ranges.current.getColor());

                prepareSpecificOption();

                refreshBanner();
                buildShapesMenu();
            }
        }

        private class PasteAction
                extends AbstractAction
        {
            //~ Constructors ---------------------------------------------------

            public PasteAction ()
            {
                super("Paste");
            }

            //~ Methods --------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                // Set a specific color
                current.setConstantColor(chosenColor);

                prepareDefaultOption();

                refreshBanner();
                buildShapesMenu();
            }
        }

        private class SelectAction
                extends AbstractAction
        {
            //~ Constructors ---------------------------------------------------

            public SelectAction ()
            {
                super("Select");
            }

            //~ Methods --------------------------------------------------------
            @Override
            public void actionPerformed (ActionEvent e)
            {
                JButton button = (JButton) e.getSource();
                menu.show(ShapesPane.this, button.getX(), button.getY());
            }
        }
    }
}
