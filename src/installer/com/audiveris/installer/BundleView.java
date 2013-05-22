//----------------------------------------------------------------------------//
//                                                                            //
//                            B u n d l e V i e w                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import ch.qos.logback.classic.Level;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

/**
 * Class {@code BundleView} is a View on a Bundle.
 *
 * @author Hervé Bitteur
 */
public class BundleView
        extends JFrame
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            Installer.class);

    private static final Color INFO_BACKGROUND = new Color(250, 250, 255);

    private static final Color BUTTON_BACKGROUND = new Color(250, 250, 200);

    /** Cancel label for the stop action. */
    private static final String CANCEL = "Cancel";

    /** Close label for the stop action. */
    private static final String CLOSE = "Close";

    //~ Instance fields --------------------------------------------------------
    //
    /** Related bundle. */
    private final Bundle bundle;

    /** Panel to display logged information. */
    private MessagePanel messagePanel;

    /** To start the installation. */
    private StartAction startAction;

    /** To stop (cancel or exit) the installation. */
    private StopAction stopAction;

    //~ Constructors -----------------------------------------------------------
    //
    //------------//
    // BundleView //
    //------------//
    /**
     * Creates a new BundleView object.
     *
     * @param bundle the underlying bundle
     */
    public BundleView (Bundle bundle)
    {
        super("Audiveris bundle installer");
        this.bundle = bundle;

        // Set panels opaque by default (TODO: useful?)
        PanelBuilder.setOpaqueDefault(true);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        startAction = new StartAction();
        stopAction = new StopAction();

        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());

        pane.add(buildNorthPanel(), BorderLayout.NORTH);
        pane.add(buildInfoPanel(), BorderLayout.CENTER);
        pane.add(buildButtonPanel(), BorderLayout.SOUTH);

        // Set Nimbus Look & Feel if possible
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    SwingUtilities.updateComponentTreeUI(this);

                    break;
                }
            }
        } catch (Exception ex) {
            logger.warn("Cannot set Nimbus L&F, using default.", ex);
        }

        // Adjust size and location of this window
        setSizeAndLocation();
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // publishMessage //
    //----------------//
    public void publishMessage (Level level,
                                String message)
    {
        messagePanel.display(level, message);
    }

    //------------------//
    // buildButtonPanel //
    //------------------//
    /**
     * Build the bottom panel that provides start and cancel buttons.
     *
     * @return the button panel
     */
    private JPanel buildButtonPanel ()
    {
        final JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setPreferredSize(new Dimension(500, 50));
        panel.setLayout(new BorderLayout());
        panel.setBackground(BUTTON_BACKGROUND);

        panel.add(new ButtonPanel(startAction), BorderLayout.WEST);
        panel.add(new ButtonPanel(stopAction), BorderLayout.EAST);

        return panel;
    }

    //----------------//
    // buildCompPanel //
    //----------------//
    /**
     * Build the sub-panel that displays the sequence of companions.
     *
     * @return the companion panel
     */
    private JPanel buildCompPanel ()
    {
        // Prepare layout elements
        final String hGap = "$lcgap";
        final StringBuilder sbcol = new StringBuilder();

        for (Companion companion : bundle.getCompanions()) {
            sbcol.append("pref,")
                    .append(hGap)
                    .append(",");
        }

        final CellConstraints cst = new CellConstraints();
        final FormLayout layout = new FormLayout(sbcol.toString(), "pref");
        final JPanel panel = new JPanel();
        final PanelBuilder builder = new PanelBuilder(layout, panel);

        // Now add the desired components, using provided order
        int col = 1;

        for (Companion companion : bundle.getCompanions()) {
            CompanionView view = companion.getView();
            builder.add(view.getComponent(), cst.xy(col, 1));
            col += 2;
        }

        return panel;
    }

    //----------------//
    // buildInfoPanel //
    //----------------//
    private JScrollPane buildInfoPanel ()
    {
        messagePanel = new MessagePanel();
        messagePanel.getComponent()
                .setBackground(INFO_BACKGROUND);

        return messagePanel.getComponent();
    }

    //-----------------//
    // buildNorthPanel //
    //-----------------//
    /**
     * Build the top panel that displays the sequence of companions,
     * the language selector and the install folder selector.
     *
     * @return the top panel
     */
    private JPanel buildNorthPanel ()
    {
        // Prepare layout elements
        final FormLayout layout = new FormLayout(
                "$lcgap, fill:0:grow, $lcgap",
                "$rgap, pref, $rgap, pref, $rgap");
        final JPanel panel = new JPanel();
        final PanelBuilder builder = new PanelBuilder(layout, panel);
        final CellConstraints cst = new CellConstraints();

        int iRow = 0;

        // FolderSelector is currently disabled
        //        iRow +=2;
        //        // Add the folder selector
        //        FolderSelector dirSelector = new FolderSelector(bundle);
        //        builder.add(dirSelector.getComponent(), cst.xy(2, iRow));

        // Add the languages component
        iRow += 2;

        LangSelector langSelector = bundle.getOcrCompanion()
                .getSelector();
        builder.add(langSelector.getComponent(), cst.xy(2, iRow));

        // Add the companions component
        iRow += 2;
        builder.add(buildCompPanel(), cst.xy(2, iRow));

        return panel;
    }

    //--------------------//
    // setSizeAndLocation //
    //--------------------//
    /**
     * Try to locate this Bundle window so that the underlying JNLP
     * window does not get masked by this one.
     * We try to locate the window in the upper right corner of the primary
     * physical screen.
     */
    private void setSizeAndLocation ()
    {
        pack();

        // Window size
        final int width = 672;
        final int height = 400;
        final int gapFromBorder = 20;
        setSize(width, height);

        // Window location
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getScreenDevices()[0];
        GraphicsConfiguration config = device.getConfigurations()[0];
        Rectangle bounds = config.getBounds();
        logger.debug("Primary screen bounds: {}", bounds);

        Point topLeft = new Point(
                (bounds.x + bounds.width) - width - gapFromBorder,
                bounds.y + gapFromBorder);
        logger.debug("Window topLeft: {}", topLeft);
        setLocation(topLeft);
    }

    //~ Inner Classes ----------------------------------------------------------
    //-------------//
    // ButtonPanel //
    //-------------//
    private static class ButtonPanel
            extends JPanel
    {
        //~ Static fields/initializers -----------------------------------------

        private static final Insets insets = new Insets(8, 5, 8, 5);

        //~ Constructors -------------------------------------------------------
        public ButtonPanel (Action action)
        {
            setPreferredSize(new Dimension(200, 0));

            final JButton button = new JButton(action);
            button.setPreferredSize(new Dimension(100, 25));
            setOpaque(false);
            add(button);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public Insets getInsets ()
        {
            return insets;
        }
    }

    //-------------//
    // StartAction //
    //-------------//
    private class StartAction
            extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public StartAction ()
        {
            super("Install");
            putValue(SHORT_DESCRIPTION, "Launch the installation");
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            logger.debug("StartAction performed");

            setEnabled(false);
            stopAction.setEnabled(false);
            new Worker().execute();
        }
    }

    //------------//
    // StopAction //
    //------------//
    private class StopAction
            extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public StopAction ()
        {
            super(CANCEL);
            putValue(SHORT_DESCRIPTION, "Cancel the installation");
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            if (getValue(NAME)
                    .equals(CANCEL)) {
                logger.debug("Cancel Action performed");
                bundle.setCancelled(true);
            } else {
                logger.debug("Close Action performed");
            }

            bundle.close();
        }
    }

    //--------//
    // Worker //
    //--------//
    private class Worker
            extends SwingWorker<Void, Void>
    {
        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
                throws Exception
        {
            try {
                bundle.installBundle();
                logger.info("\nYou can now safely exit.\n");
                JOptionPane.showMessageDialog(
                        Installer.getFrame(),
                        "Installation completed successfully",
                        "Installation completion",
                        JOptionPane.INFORMATION_MESSAGE);

                Jnlp.extensionInstallerService.installSucceeded(false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        Installer.getFrame(),
                        "Installation has failed: \n" + ex.getMessage(),
                        "Installation completion",
                        JOptionPane.WARNING_MESSAGE);

                Jnlp.extensionInstallerService.installFailed();
            }

            return null;
        }

        @Override
        protected void done ()
        {
            startAction.setEnabled(true);
            stopAction.setEnabled(true);

            stopAction.putValue(AbstractAction.NAME, CLOSE);
            stopAction.putValue(
                    AbstractAction.SHORT_DESCRIPTION,
                    "Close the installer");
        }
    }
}
