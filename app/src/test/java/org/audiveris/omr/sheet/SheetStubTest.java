//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S h e e t S t u b T e s t                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.sheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import javax.xml.bind.Marshaller;

import org.audiveris.omr.sheet.Params.SheetParams;
import org.audiveris.omr.util.BaseTestCase;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;

/**
 * Class <code>SheetStubTest</code> covers the {@code beforeMarshal} /
 * {@code afterMarshal} hooks invoked by the JAXB runtime while a {@code Book}
 * is stored: the object published for marshalling must be a copy, so that
 * the live parameters (read by other threads during parallel stub
 * processing) are never left in a transiently pruned or null state.
 */
public class SheetStubTest
        extends BaseTestCase
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SheetStubTest object.
     */
    public SheetStubTest ()
    {
    }

    //~ Tests --------------------------------------------------------------------------------------

    //---------------------//
    // testLiveObjectKept //
    //---------------------//
    @Test
    public void testLiveObjectKept ()
            throws Exception
    {
        SheetStub stub = newStub();

        // Give one parameter a specific value, so the pruned copy is not empty
        stub.getInterlineSpecificationParam().setSpecific(120);

        // The live object, as seen by concurrent readers
        SheetParams original = parameters(stub);
        assertNotNull (original.musicFamily); // Default value, non-null

        beforeMarshal(stub);

        // The original object must have been left untouched
        assertNotNull ("The live object must not be pruned", original.musicFamily);

        // The object published for marshalling is a pruned copy
        SheetParams published = parameters(stub);
        assertNotSame (original, published);
        assertNull ("Default values are pruned from the copy", published.musicFamily);
        assertEquals (120, published.interlineSpecification.getSpecific().intValue());

        afterMarshal(stub);

        // A working parameters structure is restored after the marshal
        assertNotSame (original, parameters(stub));
        assertNotNull (parameters(stub).musicFamily);
    }

    //---------------------------//
    // testGettersSafeInWindow //
    //---------------------------//
    @Test
    public void testGettersSafeInWindow ()
            throws Exception
    {
        SheetStub stub = newStub();

        // During the marshal window, the parameters field may point at a
        // pruned copy (fields nulled) or at null (all default): the
        // getters must keep returning usable values instead of throwing
        beforeMarshal(stub);
        assertNotNull (stub.getMusicFamilyParam());
        assertNotNull (stub.getInterlineSpecificationParam());

        afterMarshal(stub);
        assertNotNull (stub.getMusicFamilyParam());

        // Simulate the window where the published object is null
        setParameters(stub, null);
        assertNotNull ("Getters must fall back to the mirror", stub.getMusicFamilyParam());
        assertNotNull (stub.getInterlineSpecificationParam());
    }

    //~ Helpers ------------------------------------------------------------------------------------

    /**
     * Build a stub with its parameters fully wired, as in production
     * (same path as {@code Book} construction or loading).
     */
    private static SheetStub newStub ()
    {
        Book book = new Book(Paths.get("test.pdf"));

        return new SheetStub(book, 1);
    }

    /**
     * Invoke the private {@code beforeMarshal (Marshaller)} hook, exactly
     * as the JAXB runtime does.
     */
    private static void beforeMarshal (SheetStub stub)
            throws Exception
    {
        invoke(stub, "beforeMarshal");
    }

    /**
     * Invoke the private {@code afterMarshal (Marshaller)} hook, exactly
     * as the JAXB runtime does.
     */
    private static void afterMarshal (SheetStub stub)
            throws Exception
    {
        invoke(stub, "afterMarshal");
    }

    private static void invoke (SheetStub stub,
                                String name)
            throws Exception
    {
        Method method = SheetStub.class.getDeclaredMethod(name, Marshaller.class);
        method.setAccessible(true);
        method.invoke(stub, (Object) null);
    }

    private static SheetParams parameters (SheetStub stub)
            throws Exception
    {
        return (SheetParams) field(stub).get(stub);
    }

    private static void setParameters (SheetStub stub,
                                       SheetParams value)
            throws Exception
    {
        field(stub).set(stub, value);
    }

    private static Field field (SheetStub stub)
            throws Exception
    {
        Field field = SheetStub.class.getDeclaredField("parameters");
        field.setAccessible(true);

        return field;
    }
}
