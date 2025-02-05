//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               M e t r o n o m e I n t e r T e s t                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.util.ClassUtil;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Unitary tests for class <code>MetronomeInterTest</code> .
 *
 * @author Hervé Bitteur
 */
public class MetronomeInterTest
{
    /** Store output in dedicated file. */
    private static final PrintWriter out = getPrintWriter(new File("../data/metronome-tests.log"));

    @Test
    public void test_01 ()
    {
        t("J =116", true);
    }

    @Test
    public void test_02 ()
    {
        t("J=116", true);
    }

    @Test
    public void test_03 ()
    {
        t("Slowly J = 116", true);
    }

    @Test
    public void test_04 ()
    {
        t("Slowly J=116 ", true);
    }

    @Test
    public void test_05 ()
    {
        t("Slowly (J = 116)", true);
    }

    @Test
    public void test_06 ()
    {
        t("Slowly (J = ca. 116)", true);
    }

    @Test
    public void test_07 ()
    {
        t("Slowly ( J = 116 env.)", true);
    }

    @Test
    public void test_08 ()
    {
        t("Slowly ( J = 116-140)", true);
    }

    @Test
    public void test_09 ()
    {
        t("Slowly ( J = ca. 116 - 140)", true);
    }

    @Test
    public void test_10 ()
    {
        t("Allegretto quasi andantino (J = 69 env.)", true);
    }

    @Test
    public void test_11 ()
    {
        t("Allegretto quasi andantino (J = ca. 100-120 env.)", true);
    }

    @Test
    public void test_12 ()
    {
        t("Allegretto quasi andantino J = 100-120 grosso modo", true);
    }

    @Test
    public void test_13 ()
    {
        t("Allegretto quasi andantino (J = 100 env.) garbage", true);
    }

    @Test
    public void test_14 ()
    {
        t("Adagio .h = 126", true);
    }

    @Test
    public void test_15 ()
    {
        t("Presto (J: 160)", true); // '=' OCR'd as ':'
    }

    //----------------//
    // getPrintWriter //
    //----------------//
    private static PrintWriter getPrintWriter (File file)
    {
        try {
            final BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), "UTF8"));

            return new PrintWriter(bw);
        } catch (Exception ex) {
            System.err.println("Error creating " + file + ex);

            return null;
        }
    }

    //---//
    // t //
    //---//
    private void t (String text,
                    boolean exp)
    {
        // Print method name
        StackTraceElement elem = ClassUtil.getCallingFrame();

        if (elem != null) {
            System.out.println();
            System.out.println("method   : " + elem.getMethodName());
            out.println();
            out.println("method   : " + elem.getMethodName());
            out.flush();
        }

        System.out.println("input    : \"" + text + "\"");
        out.println("input    : \"" + text + "\"");
        out.flush();

        System.out.println("expected : " + exp);
        out.println("expected : " + exp);
        out.flush();

        boolean result = MetronomeInter.fullValidityCheck(text);
        System.out.println("output   : " + result);
        out.println("output   : " + result);
        out.flush();

        assertEquals(exp, result);
    }
}
