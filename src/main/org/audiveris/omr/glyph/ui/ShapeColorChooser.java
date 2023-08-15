//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S h a p e C o l o r C h o o s e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.glyph.ui;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.ui.OmrGui;
import org.audiveris.omr.ui.symbol.MusicFamily;

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
 * Class <code>ShapeColorChooser</code> offers a convenient user interface to choose proper
 * color for each glyph shape.
 * It is derived from the Sun Java tutorial ColorChooserDemo.
 * <p>
 * The right part of the panel is used by a classic color chooser
 * <p>
 * The left part of the panel is used by the shape at hand, with a way to
 * browse through the various defined shapes.
 * <p>
 * The strategy is the following: First the predefined shape ranges (such as "Physicals", "Bars",
 * "Clefs", ...) have their own color defined. Then, each individual shape within these shape ranges
 * has its color assigned by default of the containing range, unless a color is specifically
 * assigned to this individual shape.
 *
 * @author Hervé Bitteur
 */
public class ShapeColorChooser
        implements ChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ShapeColorChooser.class);

    private static JFrame frame;

    //~ Instance fields ----------------------------------------------------------------------------

    /** The classic color chooser utility */
    private final JColorChooser colorChooser;

    /** Color chosen in the JColorChooser utility */
    private Color chosenColor;

    /** UI component */
    private final JPanel component;

    /** To select shape range */
    private final RangesPane ranges;

    /** To select shape (within selected range) */
    private final ShapesPane shapes;

    //~ Constructors -------------------------------------------------------------------------------

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
        colorChooser.getSelectionModel().addChangeListener(this);
        colorChooser.setBorder(BorderFactory.createTitledBorder("Choose Shape Color"));

        component.add(globalPanel, BorderLayout.CENTER);
        component.add(colorChooser, BorderLayout.EAST);
    }

    //~ Methods ------------------------------------------------------------------------------------

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

    //~ Static Methods -----------------------------------------------------------------------------

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
            ResourceMap resource = Application.getInstance().getContext().getResourceMap(
                    ShapeColorChooser.class);
            resource.injectComponents(frame);
        }

        OmrGui.getApplication().show(frame);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //------//
    // Pane //
    //------//
    private abstract class Pane
            extends JPanel
    {

        public JLabel banner = new JLabel("", JLabel.CENTER);

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

        public RangesPane ()
        {
            super("Shape Range");

            add(selectButton, BorderLayout.NORTH);
            add(pasteButton, BorderLayout.SOUTH);

            paste.setEnabled(false);

            buildRangesMenu();
        }

        private void buildRangesMenu ()
        {
            menu.removeAll();
            ShapeSet.addAllShapeSets(menu, selectionListener);
        }

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

        private class PasteAction
                extends AbstractAction
        {

            public PasteAction ()
            {
                super("Paste");
            }

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

            public SelectAction ()
            {
                super("Select");
            }

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

        private final CopyAction copy = new CopyAction();

        private final CutAction cut = new CutAction();

        private final PasteAction paste = new PasteAction();

        private final SelectAction select = new SelectAction();

        private final JButton selectButton = new JButton(select);

        private boolean isSpecific;

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

        private void buildShapesMenu ()
        {
            menu.removeAll();

            // Add all shapes within current range
            ShapeSet.addSetShapes(MusicFamily.Bravura, ranges.current, menu, selectionListener);
        }

        // When color chooser selection has been made
        @Override
        public void colorChanged ()
        {
            updateActions();
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

        @Override
        protected void refreshBanner ()
        {
            if (current != null) {
                banner.setForeground(current.getColor());
            }
        }

        // When a new range has been selected
        public void setRange ()
        {
            buildShapesMenu();

            select.setEnabled(true);
            selectButton.setText("-- Select Shape from " + ranges.current.getName() + " --");

            current = null;
            banner.setText("");
            cut.setEnabled(false);
            copy.setEnabled(false);
            paste.setEnabled(false);
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

            @Override
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

            public PasteAction ()
            {
                super("Paste");
            }

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

            public SelectAction ()
            {
                super("Select");
            }

            @Override
            public void actionPerformed (ActionEvent e)
            {
                JButton button = (JButton) e.getSource();
                menu.show(ShapesPane.this, button.getX(), button.getY());
            }
        }
    }
}
