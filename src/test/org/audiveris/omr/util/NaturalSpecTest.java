/*
 *
 * Copyright © Audiveris 2021. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.audiveris.omr.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Hervé Bitteur
 */
public class NaturalSpecTest
{

    public NaturalSpecTest ()
    {
    }

    @Test
    public void testDecode1 ()
    {
        System.out.println("decode1");
        String spec = "1 - 3 , 6";
        System.out.println("spec:" + spec);
        List<Integer> expResult = Arrays.asList(1, 2, 3, 6);
        List<Integer> result = NaturalSpec.decode(spec, true);
        System.out.println("result:" + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testDecode2 ()
    {
        System.out.println("decode2");
        String spec = "3 6 8, 10-12";
        System.out.println("spec:" + spec);
        List<Integer> expResult = Arrays.asList(3, 6, 8, 10, 11, 12);
        List<Integer> result = NaturalSpec.decode(spec, true);
        System.out.println("result:" + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testDecode3 ()
    {
        System.out.println("decode3");
        String spec = " ";
        System.out.println("spec:" + spec);
        List<Integer> expResult = Collections.emptyList();
        List<Integer> result = NaturalSpec.decode(spec, true);
        System.out.println("result:" + result);
        assertEquals(expResult, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecode4 ()
    {
        System.out.println("decode4");
        String spec = "2-3,1";
        System.out.println("spec:" + spec);
        List<Integer> result = NaturalSpec.decode(spec, true);
        System.out.println("result:" + result);
    }

    @Test
    public void testDecode5 ()
    {
        System.out.println("decode5");
        String spec = "2-3,1";
        System.out.println("spec:" + spec);
        List<Integer> expResult = Arrays.asList(2, 3, 1);
        List<Integer> result = NaturalSpec.decode(spec, false);
        System.out.println("result:" + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testDecode6 ()
    {
        System.out.println("decode6");
        String spec = "3-2";
        System.out.println("spec:" + spec);
        List<Integer> expResult = Collections.emptyList();
        List<Integer> result = NaturalSpec.decode(spec, false);
        System.out.println("result:" + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testDecode7 ()
    {
        System.out.println("decode7");
        String spec = "3-3";
        System.out.println("spec:" + spec);
        List<Integer> expResult = Arrays.asList(3);
        List<Integer> result = NaturalSpec.decode(spec, false);
        System.out.println("result:" + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testEncode1 ()
    {
        System.out.println("encode1");
        List<Integer> values = Arrays.asList(1, 2, 3, 4, 5);
        System.out.println("values:" + values);
        String expResult = "1-5";
        String result = NaturalSpec.encode(values);
        System.out.println("result:" + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testEncode2 ()
    {
        System.out.println("encode2");
        List<Integer> values = Arrays.asList(5, 2, 4, 6, 7, 8, 10, 12);
        System.out.println("values:" + values);
        String expResult = "5,2,4,6-8,10,12";
        String result = NaturalSpec.encode(values);
        System.out.println("result:" + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testEncode3 ()
    {
        System.out.println("encode3");
        List<Integer> values = Arrays.asList(5, 4, 3, 2);
        System.out.println("values:" + values);
        String expResult = "5,4,3,2";
        String result = NaturalSpec.encode(values);
        System.out.println("result:" + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testEncode4 ()
    {
        System.out.println("encode4");
        List<Integer> values = Arrays.asList(15);
        System.out.println("values:" + values);
        String expResult = "15";
        String result = NaturalSpec.encode(values);
        System.out.println("result:" + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testEncode5 ()
    {
        System.out.println("encode5");
        List<Integer> values = Collections.emptyList();
        System.out.println("values:" + values);
        String expResult = "";
        String result = NaturalSpec.encode(values);
        System.out.println("result:" + result);
        assertEquals(expResult, result);
    }
}
