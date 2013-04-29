/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.ui;

import com.jgoodies.looks.LookUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import omr.util.BaseTestCase;

import org.jdesktop.application.SingleFrameApplication;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import omr.ui.util.SeparableToolBar;
import org.jdesktop.application.Application;
import org.junit.Test;

/**
 *
 * @author herve
 */
public class BsafTest
        extends BaseTestCase
{
    //~ Methods ----------------------------------------------------------------

    public static final void main (String... args)
    {
        Application.launch(Appl.class, null);
    }

    @Test
    public void testAppl ()
    {
        Application.launch(Appl.class, null);

    }

    //~ Inner Classes ----------------------------------------------------------
    private static class Appl
            extends SingleFrameApplication
    {
        //~ Methods ------------------------------------------------------------

        @Override
        protected void startup ()
        {
            String lafName =
                    LookUtils.IS_OS_WINDOWS_XP
                    ? com.jgoodies.looks.Options.getCrossPlatformLookAndFeelClassName()
                    : com.jgoodies.looks.Options.getSystemLookAndFeelClassName();

            try {
                UIManager.setLookAndFeel(lafName);
            } catch (Exception e) {
                System.err.println("Can't set look & feel:" + e);
            }


            JFrame frame = getMainFrame();
            frame.setJMenuBar(buildMenuBar());
            frame.setContentPane(buildContentPane());
            frame.setSize(600, 400);
            frame.setTitle("BSAF Test");
            show(frame);
        }

        /**
         * Builds and answers the menu bar.
         */
        private JMenuBar buildMenuBar ()
        {
            JMenu menu;
            JMenuBar innerBar = new JMenuBar();
            innerBar.putClientProperty(com.jgoodies.looks.Options.HEADER_STYLE_KEY, Boolean.TRUE);

            menu = new JMenu("File");
            menu.add(new JMenuItem("New..."));
            menu.add(new JMenuItem("Open..."));
            menu.add(new JMenuItem("Save"));
            menu.addSeparator();
            menu.add(new JMenuItem("Print..."));
            innerBar.add(menu);

            menu = new JMenu("Edit");
            menu.add(new JMenuItem("Cut"));
            menu.add(new JMenuItem("Copy"));
            menu.add(new JMenuItem("Paste"));
            innerBar.add(menu);

            JProgressBar bar;

            JMenuBar outerBar = new JMenuBar();
            outerBar.setLayout(new GridLayout(1, 0));

            outerBar.add(innerBar);
            outerBar.add(new JLabel("I'm the banner!", JLabel.CENTER));

            return outerBar;
        }

        /**
         * Builds and answers the content pane.
         */
        private JComponent buildContentPane ()
        {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(Color.orange);
            panel.add(buildToolBar(), BorderLayout.NORTH);
            panel.add(createCenteredLabel("Content"), BorderLayout.CENTER);
            panel.add(buildStatusBar(), BorderLayout.SOUTH);
            return panel;
        }

        /**
         * Builds and answers the tool bar.
         */
        private Component buildStatusBar ()
        {
            JPanel statusBar = new JPanel(new BorderLayout());
            statusBar.setBackground(Color.LIGHT_GRAY);
            statusBar.add(createCenteredLabel("Status Bar"));
            return statusBar;
        }

        /**
         * Builds and answers the tool bar.
         */
        private Component buildToolBar ()
        {
            JToolBar toolBar = new SeparableToolBar();
            toolBar.setBackground(Color.PINK);
            ///toolBar.putClientProperty(Options.HEADER_STYLE_KEY, Boolean.TRUE);

            toolBar.add(createCenteredLabel("Tool Bar"));

            JButton button1 = new JButton("Bouton #1");
            toolBar.add(button1);

            toolBar.addSeparator();

            JButton button2 = new JButton("Bouton #2");
            toolBar.add(button2);

            return toolBar;
        }

        /**
         * Creates and answers a
         * <code>JLabel</code> that has the text
         * centered and that is wrapped with an empty border.
         */
        private Component createCenteredLabel (String text)
        {
            JLabel label = new JLabel(text);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setBorder(new EmptyBorder(3, 3, 3, 3));
            return label;
        }
    }
}
