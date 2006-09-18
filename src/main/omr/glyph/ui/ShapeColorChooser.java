//----------------------------------------------------------------------------//
//                                                                            //
//                     S h a p e C o l o r C h o o s e r                      //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.Shape;

import java.awt.*;
import java.awt.event.*;
import java.util.EnumSet;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>ShapeColorChooser</code> offers a convenient user interface to
 * choose proper color for each glyph shape. It is derived from the Sun Java
 * tutorial ColorChooserDemo.
 *
 * <p>The lower part of the panel is used by a classic color chooser
 *
 * <p>The upper part of the panel is used by the shape at hand, with a way to
 * browse through the various defined shapes.
 *
 * <p>The strategy is the following: First the predefined shape ranges (such as
 * "Physicals", "Bars", "Clefs", ...) have their own color defined. Then, each
 * individual shpae within these shape ranges has its color assigned by default
 * to the color of the containing range, unless a color is specifically assigned
 * to this individual shape.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ShapeColorChooser
    implements ChangeListener
{
    //~ Instance fields --------------------------------------------------------

    private Color         chosenColor;
    private JColorChooser colorChooser;
    private JPanel        component;
    private RangesPane    ranges;
    private ShapesPane    shapes;

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // ShapeColorChooser //
    //-------------------//
    /**
     * Create an instance of ShapeColorChooser (should be improved to always
     * reuse the same instance. TBD)
     */
    public ShapeColorChooser ()
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
        colorChooser = new JColorChooser( /*banner.getForeground()*/
        );
        colorChooser.getSelectionModel()
                    .addChangeListener(this);
        colorChooser.setBorder(
            BorderFactory.createTitledBorder("Choose Shape Color"));

        component.add(globalPanel, BorderLayout.WEST);
        component.add(colorChooser, BorderLayout.EAST);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component
     *
     * @return the concrete component
     */
    public JComponent getComponent ()
    {
        return component;
    }

    //------//
    // main //
    //------//
    /**
     * For stand-alone use of this interface
     *
     * @param args not used
     */
    public static void main (String[] args)
    {
        javax.swing.SwingUtilities.invokeLater(
            new Runnable() {
                    public void run ()
                    {
                        createAndShowGUI();
                    }
                });
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * Triggered when color selection in the color choose has changed.
     *
     * @param e not used
     */
    public void stateChanged (ChangeEvent e)
    {
        chosenColor = colorChooser.getColor();

        ranges.colorChanged();
        shapes.colorChanged();
    }

    //------------------//
    // createAndShowGUI //
    //------------------//
    /**
     * Create the GUI and show it.  For thread safety, this method should be
     * invoked from the event-dispatching thread.
     */
    private static void createAndShowGUI ()
    {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        JFrame frame = new JFrame("ShapeColorChooser");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new ShapeColorChooser().getComponent();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    //~ Inner Classes ----------------------------------------------------------

    //------//
    // Pane //
    //------//
    private abstract class Pane
        extends JPanel
    {
        public JLabel     banner = new JLabel("", JLabel.CENTER);
        public JPopupMenu menu = new JPopupMenu();

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

        public abstract void colorChanged ();

        protected abstract void refreshBanner ();
    }

    //------------//
    // RangesPane //
    //------------//
    private class RangesPane
        extends Pane
    {
        public Shape.Range     current;
        private SelectAction   select = new SelectAction();
        private JButton        selectButton = new JButton(select);
        private PasteAction    paste = new PasteAction();
        private ActionListener selectionListener = new ActionListener() {
            // Called when a range has been selected
            public void actionPerformed (ActionEvent e)
            {
                JMenuItem source = (JMenuItem) e.getSource();
                current = Shape.Range.valueOf(source.getText());

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

        public RangesPane ()
        {
            super("Shape Range");

            add(selectButton, BorderLayout.NORTH);
            add(pasteButton, BorderLayout.SOUTH);

            paste.setEnabled(false);

            buildRangesMenu();
        }

        // When color chooser selection has been made
        public void colorChanged ()
        {
            if (current != null) {
                paste.setEnabled(true);
            }
        }

        protected void refreshBanner ()
        {
            if (current != null) {
                banner.setForeground(current.getColor());
            }
        }

        private void buildRangesMenu ()
        {
            menu.removeAll();
            Shape.Range.addRangeItems(menu, selectionListener);
        }

        private class PasteAction
            extends AbstractAction
        {
            public PasteAction ()
            {
                super("Paste");
            }

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
            public SelectAction ()
            {
                super("Select");
            }

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
        public Shape           current;
        private ActionListener selectionListener = new ActionListener() {
            // Called when a shape has been selected
            public void actionPerformed (ActionEvent e)
            {
                JMenuItem source = (JMenuItem) e.getSource();
                current = Shape.valueOf(source.getText());

                if (current != null) {
                    banner.setText(current.toString());

                    // Check if a specific color is assigned
                    Color color = current.getConstantColor();

                    if (color != null) {
                        prepareDefaultOption();
                        refreshBanner();
                    } else {
                        prepareSpecificOption();
                        banner.setForeground(ranges.current.getColor());
                    }
                } else {
                    banner.setText("");
                }
            }
        };

        private CopyAction   copy = new CopyAction();
        private CutAction    cut = new CutAction();
        private JButton      copyButton = new JButton(copy);
        private JButton      cutButton = new JButton(cut);
        private PasteAction  paste = new PasteAction();
        private JButton      pasteButton = new JButton(paste);
        private SelectAction select = new SelectAction();
        private JButton      selectButton = new JButton(select);
        private boolean      isSpecific;

        public ShapesPane ()
        {
            super("Individual Shape");

            selectButton.setText("-- Select Shape --");
            // No range has been selected yet, so no shape can be selected
            select.setEnabled(false);

            add(selectButton, BorderLayout.NORTH);

            // A series of 3 buttons at the bottom
            JPanel buttons = new JPanel(new GridLayout(1, 3));
            buttons.add(cutButton);
            buttons.add(copyButton);
            buttons.add(pasteButton);
            add(buttons, BorderLayout.SOUTH);

            cut.setEnabled(false);
            copy.setEnabled(false);
            paste.setEnabled(false);
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
            cutButton.setEnabled(false);
            copyButton.setEnabled(false);
            pasteButton.setEnabled(false);
        }

        // When color chooser selection has been made
        public void colorChanged ()
        {
            updateActions();
        }

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
            Shape.addRangeShapeItems(ranges.current, menu, selectionListener);
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

        private class CopyAction
            extends AbstractAction
        {
            public CopyAction ()
            {
                super("Copy");
            }

            public void actionPerformed (ActionEvent e)
            {
                colorChooser.setColor(current.getColor());
            }
        }

        private class CutAction
            extends AbstractAction
        {
            public CutAction ()
            {
                super("Cut");
            }

            public void actionPerformed (ActionEvent e)
            {
                // Drop specific for default
                current.resetConstantColor(ranges.current.getColor());
                current.setColor(ranges.current.getColor()); // Needed ?

                prepareSpecificOption();

                refreshBanner();
                buildShapesMenu();
            }
        }

        private class PasteAction
            extends AbstractAction
        {
            public PasteAction ()
            {
                super("Paste");
            }

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
            public SelectAction ()
            {
                super("Select");
            }

            public void actionPerformed (ActionEvent e)
            {
                JButton button = (JButton) e.getSource();
                menu.show(ShapesPane.this, button.getX(), button.getY());
            }
        }
    }
}
