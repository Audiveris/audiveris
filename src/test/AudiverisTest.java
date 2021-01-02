//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   A u d i v e r i s T e s t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.junit.Ignore;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @author Hervé Bitteur
 */
public class AudiverisTest
{
    @Test
    public void testMainMethodMultipleCalls ()
    {
        String[] args1 = new String[]{
            "-batch", "-step", "LOAD", "-option","input=data/examples/chula.png"
        };
        String[] args2 = new String[]{
            "-batch", "-step", "LOAD", "-option","input=data/examples/batuque.png"
        };
        Audiveris.main(args1);
        Audiveris.main(args2);
        assertTrue(true);
    }
}
