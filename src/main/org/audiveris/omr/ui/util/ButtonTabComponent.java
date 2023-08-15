//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               B u t t o n T a b C o m p o n e n t                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.audiveris.omr.ui.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Component to be used as tabComponent.
 * <p>
 * Contains a JLabel to show the text and a JButton to close the tab it belongs to.
 *
 * @author Hervé Bitteur
 */
public class ButtonTabComponent
        extends JPanel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The same listener for all TabButton instances in application!. */
    private static final MouseListener buttonMouseListener = new MouseAdapter()
    {
        @Override
        public void mouseEntered (MouseEvent e)
        {
            Component component = e.getComponent();

            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }

        @Override
        public void mouseExited (MouseEvent e)
        {
            Component component = e.getComponent();

            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------

    /** The containing JTabbedPane. */
    private final ClosableTabbedPane pane;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>ButtonTabComponent</code> object.
     *
     * @param pane the containing tabbed pane
     */
    public ButtonTabComponent (final ClosableTabbedPane pane)
    {
        //unset default FlowLayout' gaps
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));

        if (pane == null) {
            throw new NullPointerException("TabbedPane is null");
        }

        this.pane = pane;

        setOpaque(true);

        //make JLabel read title from JTabbedPane
        JLabel label = new JLabel()
        {
            @Override
            public String getText ()
            {
                int i = pane.indexOfTabComponent(ButtonTabComponent.this);

                if (i != -1) {
                    return pane.getTitleAt(i);
                }

                return null;
            }
        };

        add(label);

        //add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));

        //tab button
        JButton button = new TabButton();
        add(button);

        //add more space to the top of the component
        ///setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // TabButton //
    //-----------//
    private class TabButton
            extends JButton
            implements ActionListener
    {

        TabButton ()
        {
            final int size = 13; // Button side length
            setPreferredSize(new Dimension(size, size));
            setToolTipText("close this tab");

            //Make the button looks the same for all Laf's
            setUI(new BasicButtonUI());

            //Make it transparent
            setContentAreaFilled(false);

            //No need to be focusable
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);

            //Making nice rollover effect
            //we use the same listener for all buttons
            addMouseListener(buttonMouseListener);
            setRolloverEnabled(true);

            //Close the proper tab by clicking the button
            addActionListener(this);
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            int i = pane.indexOfTabComponent(ButtonTabComponent.this);

            if (i != -1) {
                // Let user give the last word
                if (pane.tabAboutToClose(i)) {
                    pane.remove(i);
                }
            }
        }

        //paint the cross
        @Override
        protected void paintComponent (Graphics g)
        {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            //shift the image for pressed buttons
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            }

            g2.setStroke(new BasicStroke(2));

            if (getModel().isRollover()) {
                g2.setColor(Color.MAGENTA);
            } else {
                g2.setColor(Color.BLACK);
            }

            int delta = 4; // Margin around the cross
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
            g2.dispose();
        }

        //we don't want to update UI for this button
        @Override
        public void updateUI ()
        {
        }
    }
}
