//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   C o n n e c t i o n T a s k                                  //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.score.Page;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.SlurInter;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class <code>ConnectionTask</code> acts on connections, by connecting or disconnecting
 * inters from separate systems (and thus SIGs).
 *
 * @author Hervé Bitteur
 */
public abstract class ConnectionTask
        extends UITask
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ConnectionTask.class);

    //~ Enumerations -------------------------------------------------------------------------------
    /**
     * Specifies the kind of connection.
     * Limited to slurs for the time being.
     */
    public enum Kind
    {
        SLUR_CONNECTION;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Impacted page. */
    protected final Page page;

    /** Precise kind of connection handled. */
    protected Kind kind;

    /** First inter involved in connection. */
    protected Inter one;

    /** Second inter involved in connection. */
    protected Inter two;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>ConnectionTask</code> object.
     *
     * @param page       the containing page (of second Inter)
     * @param kind       the kind of connection
     * @param actionName name for action
     */
    public ConnectionTask (Page page,
                           Kind kind,
                           String actionName)
    {
        super(page, actionName);
        this.page = page;
        this.kind = kind;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public Page getPage ()
    {
        return page;
    }

    @Override
    public String toString ()
    {
        return new StringBuilder(actionName)
                .append(" ").append(kind)
                .append(" one:").append(one)
                .append(" two:").append(two)
                .toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // ConnectTask //
    //-------------//
    public static class ConnectTask
            extends ConnectionTask
    {

        public ConnectTask (Inter one,
                            Inter two,
                            Kind kind)
        {
            super(two.getSig().getSystem().getPage(), kind, "connect");
            this.one = one;
            this.two = two;
        }

        @Override
        public void performDo ()
        {
            switch (kind) {
            default:
            case SLUR_CONNECTION:
                final SlurInter s1 = (SlurInter) one;
                final SlurInter s2 = (SlurInter) two;
                s1.setExtension(RIGHT, s2);
                s2.setExtension(LEFT, s1);
                logger.info("Slur connection between #{} and #{}", s1.getId(), s2.getId());
            }
        }

        @Override
        public void performUndo ()
        {
            switch (kind) {
            default:
            case SLUR_CONNECTION:
                final SlurInter s1 = (SlurInter) one;
                final SlurInter s2 = (SlurInter) two;
                s1.setExtension(RIGHT, null);
                s2.setExtension(LEFT, null);
                logger.info("Slur un-connection between #{} and #{}", s1.getId(), s2.getId());
            }
        }
    }

    //----------------//
    // DisconnectTask //
    //----------------//
    public static class DisconnectTask
            extends ConnectionTask
    {

        public DisconnectTask (Inter one,
                               Inter two,
                               Kind kind)
        {
            super(two.getSig().getSystem().getPage(), kind, "disconnect");
            this.one = one;
            this.two = two;
        }

        @Override
        public void performDo ()
        {
            switch (kind) {
            default:
            case SLUR_CONNECTION:
                final SlurInter s1 = (SlurInter) one;
                final SlurInter s2 = (SlurInter) two;
                s1.setExtension(RIGHT, null);
                s2.setExtension(LEFT, null);
                logger.info("Slur disconnection between #{} and #{}", s1.getId(), s2.getId());
            }
        }

        @Override
        public void performUndo ()
        {
            switch (kind) {
            default:
            case SLUR_CONNECTION:
                final SlurInter s1 = (SlurInter) one;
                final SlurInter s2 = (SlurInter) two;
                s1.setExtension(RIGHT, s2);
                s2.setExtension(LEFT, s1);
                logger.info("Slur re-connection between #{} and #{}", s1.getId(), s2.getId());
            }
        }
    }
}
