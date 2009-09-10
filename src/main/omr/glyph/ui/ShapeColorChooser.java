//----------------------------------------------------------------------------//
//                                                                            //
//                     S h a p e C o l o r C h o o s e r                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Shape;
import omr.glyph.ShapeRange;

import omr.ui.MainGui;

import omr.util.Implement;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import java.awt.*;
import java.awt.event.*;

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
    //~ Static fields/initializers ---------------------------------------------

    private static JFrame frame;

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

    //-----------//
    // showFrame //
    //-----------//
    /**
     * Display the UI frame
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
     * Triggered when color selection in the color choose has changed.
     *
     * @param e not used
     */
    @Implement(ChangeListener.class)
    public void stateChanged (ChangeEvent e)
    {
        chosenColor = colorChooser.getColor();

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

        public JLabel     banner = new JLabel("", JLabel.CENTER);
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

        public ShapeRange      current;
        private SelectAction   select = new SelectAction();
        private JButton        selectButton = new JButton(select);
        private PasteAction    paste = new PasteAction();
        private ActionListener selectionListener = new ActionListener() {
            // Called when a range has been selected
            public void actionPerformed (ActionEvent e)
            {
                JMenuItem source = (JMenuItem) e.getSource();
                current = ShapeRange.valueOf(source.getText());

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
            ShapeRange.addRangeItems(menu, selectionListener);
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
            buttons.add(cutButton);
            buttons.add(copyButton);
            buttons.add(pasteButton);
            add(buttons, BorderLayout.SOUTH);

            cut.setEnabled(false);
            copy.setEnabled(false);
            paste.setEnabled(false);
        }

        //~ Methods ------------------------------------------------------------

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
            ShapeRange.addRangeShapeItems(
                ranges.current,
                menu,
                selectionListener);
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
            //~ Constructors ---------------------------------------------------

            public PasteAction ()
            {
                super("Paste");
            }

            //~ Methods --------------------------------------------------------

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

            public void actionPerformed (ActionEvent e)
            {
                JButton button = (JButton) e.getSource();
                menu.show(ShapesPane.this, button.getX(), button.getY());
            }
        }
    }
}
