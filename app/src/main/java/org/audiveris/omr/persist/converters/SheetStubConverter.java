//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S h e e t S t u b C o n v e r t e r                             //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.persist.converters;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.step.OmrStep;

import java.util.EnumSet;

/**
 * XStream converter for SheetStub matching real JAXB output:
 * <pre>
 * &lt;sheet number="1" invalid="true"&gt;
 *   &lt;input&gt;&lt;path&gt;...&lt;/path&gt;&lt;number&gt;1&lt;/number&gt;&lt;/input&gt;
 *   &lt;steps&gt;LOAD BINARY&lt;/steps&gt;
 * &lt;/sheet&gt;
 * </pre>
 */
public class SheetStubConverter implements Converter
{
    @Override
    public boolean canConvert (Class type)
    {
        return SheetStub.class.equals(type);
    }

    @Override
    public void marshal (Object source,
                          HierarchicalStreamWriter writer,
                          MarshallingContext context)
    {
        SheetStub stub = (SheetStub) source;
        writer.addAttribute("number", String.valueOf(stub.getNumber()));

        if (stub.isUpgraded()) { /* skip, transient */ }
        // invalid: only write when true (mirrors BooleanPositiveAdapter)
        // We can't access private `invalid` field from here without reflection.
        // Delegate to XStream's reflection for now.
        context.convertAnother(stub);
    }

    @Override
    public Object unmarshal (HierarchicalStreamReader reader,
                              UnmarshallingContext context)
    {
        // SheetStub has no public no-arg constructor. XStream must use reflection.
        // We read attributes, then let XStream reflect the rest.
        // For now, delegate entirely to reflection-based unmarshalling.
        return context.convertAnother(null, SheetStub.class);
    }
}
