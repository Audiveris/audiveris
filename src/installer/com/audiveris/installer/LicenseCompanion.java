//----------------------------------------------------------------------------//
//                                                                            //
//                        L i c e n s e C o m p a n i o n                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * Class {@code LicenseCompanion} checks user agreement WRT license.
 *
 * @author Hervé Bitteur
 */
public class LicenseCompanion
        extends AbstractCompanion
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            LicenseCompanion.class);

    private static final String DESC = "<html>This ensures you agree with Audiveris license."
                                       + "<br>(You will be able to browse the license text)</html>";

    /** Name for license. */
    private static final String LICENSE_NAME = "GNU GPL V2";

    /** URL for license browsing. */
    private static final String LICENSE_URL = "http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html";

    //~ Constructors -----------------------------------------------------------
    //------------------//
    // LicenseCompanion //
    //------------------//
    /**
     * Creates a new LicenseCompanion object.
     */
    public LicenseCompanion ()
    {
        super("License", DESC);

        if (Installer.hasUI()) {
            view = new BasicCompanionView(this, 70);
        }
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // checkInstalled //
    //----------------//
    @Override
    public boolean checkInstalled ()
    {
        return status == Status.INSTALLED;
    }

    //-----------//
    // doInstall //
    //-----------//
    @Override
    protected void doInstall ()
            throws Exception
    {
        // When running without UI, we assume license is accepted
        if (!Installer.hasUI()) {
            return;
        }

        // User choice (must be an output, yet final)
        final boolean[] isOk = new boolean[1];

        final String yes = "Yes";
        final String no = "No";
        final String browse = "View License";
        final JOptionPane optionPane = new JOptionPane(
                "Do you agree to license " + LICENSE_NAME + "?",
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_CANCEL_OPTION,
                null,
                new Object[]{yes, no, browse},
                yes);
        final String frameTitle = "End User License Agreement";
        final JDialog dialog = new JDialog(
                Installer.getFrame(),
                frameTitle,
                true);
        dialog.setContentPane(optionPane);

        // Prevent dialog closing
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        optionPane.addPropertyChangeListener(
                new PropertyChangeListener()
        {
            @Override
            public void propertyChange (PropertyChangeEvent e)
            {
                String prop = e.getPropertyName();

                if (dialog.isVisible()
                    && (e.getSource() == optionPane)
                    && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                    Object option = optionPane.getValue();
                    logger.debug("option: {}", option);

                    if (option == yes) {
                        isOk[0] = true;
                        dialog.setVisible(false);
                        dialog.dispose();
                    } else if (option == no) {
                        isOk[0] = false;
                        dialog.setVisible(false);
                        dialog.dispose();
                    } else if (option == browse) {
                        logger.info(
                                "Launching browser on {}",
                                LICENSE_URL);
                        showLicense();
                        optionPane.setValue(
                                JOptionPane.UNINITIALIZED_VALUE);
                    } else {
                    }
                }
            }
        });

        dialog.pack();
        dialog.setLocationRelativeTo(Installer.getFrame());
        dialog.setVisible(true);

        logger.debug("OK: {}", isOk[0]);

        if (!isOk[0]) {
            throw new LicenseDeclinedException();
        }
    }

    //-------------//
    // showLicense //
    //-------------//
    private void showLicense ()
    {
        try {
            final URL url = new URL(LICENSE_URL);

            if (Jnlp.basicService.isWebBrowserSupported()) {
                Jnlp.basicService.showDocument(url);
            } else {
                logger.warn("Browser is not supported");
            }
        } catch (MalformedURLException ex) {
            logger.error("Malformed URL " + LICENSE_URL, ex);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------------------------//
    // LicenseDeclinedException //
    //--------------------------//
    public static class LicenseDeclinedException
            extends RuntimeException
    {
        //~ Constructors -------------------------------------------------------

        public LicenseDeclinedException ()
        {
            super("License declined by user");
        }
    }
}
