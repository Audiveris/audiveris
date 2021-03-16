//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              B r o w s e r L i n k L i s t e n e r                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Class {@code BrowserLinkListener} provides a HyperlinkListener that launches
 * a browser on the link selected by the user.
 *
 * @author Hervé Bitteur
 */
public class BrowserLinkListener
        implements HyperlinkListener
{

    private static final Logger logger = LoggerFactory.getLogger(BrowserLinkListener.class);

    @Override
    public void hyperlinkUpdate (HyperlinkEvent event)
    {
        final HyperlinkEvent.EventType type = event.getEventType();
        final URL url = event.getURL();

        if (type == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                final URI uri = new URI(url.toString());
                WebBrowser.getBrowser().launch(uri);
            } catch (URISyntaxException ex) {
                logger.warn("Illegal URI " + url, ex);
            }
        }
    }
}
