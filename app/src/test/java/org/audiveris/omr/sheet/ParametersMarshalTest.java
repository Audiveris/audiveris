//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           P a r a m e t e r s M a r s h a l T e s t                            //
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

import org.audiveris.omr.util.BaseTestCase;
import org.audiveris.omr.util.Jaxb;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Checks that the parameters of a book and of its stubs can be read at any time,
 * even while the book is being marshalled by another thread, as happens when sheets
 * are processed in parallel and each of them stores the book.
 */
public class ParametersMarshalTest
        extends BaseTestCase
{
    private static final int MARSHAL_COUNT = 200;

    @Test
    public void testParametersReadableWhileMarshalling ()
        throws Exception
    {
        final Book book = new Book(Paths.get("dummy.png")); // The path is never opened
        book.addStub(new SheetStub(book, 1));
        book.addStub(new SheetStub(book, 2));
        final SheetStub stub = book.getStubs().get(1);

        final AtomicBoolean stop = new AtomicBoolean(false);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread reader = new Thread( () -> {
            try {
                while (!stop.get()) {
                    stub.getInputQualityParam().getValue();
                    stub.getMusicFamilyParam().getValue();
                    book.getOcrLanguagesParam().getValue();
                }
            } catch (Throwable ex) {
                failure.set(ex);
            }
        }, "parameters-reader");
        reader.start();

        try {
            for (int i = 0; (i < MARSHAL_COUNT) && (failure.get() == null); i++) {
                Jaxb.marshal(book, new ByteArrayOutputStream(), Book.getJaxbContext());
            }
        } finally {
            stop.set(true);
            reader.join();
        }

        if (failure.get() != null) {
            throw new AssertionError("Parameters not readable while marshalling", failure.get());
        }
    }
}
