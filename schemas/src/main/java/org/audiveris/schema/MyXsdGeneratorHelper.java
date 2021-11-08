//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             M y X s d G e n e r a t o r H e l p e r                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
package org.audiveris.schema;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.jaxb2.schemageneration.AbstractXsdGeneratorMojo;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.NodeProcessor;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.JavaDocRenderer;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.SearchableDocumentation;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.schemaenhancement.ChangeFilenameProcessor;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.schemaenhancement.ChangeNamespacePrefixProcessor;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.schemaenhancement.SimpleNamespaceResolver;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.schemaenhancement.TransformSchema;
import org.codehaus.mojo.jaxb2.shared.FileSystemUtilities;
import org.codehaus.mojo.jaxb2.shared.Validate;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/**
 * Utility class holding algorithms used when generating XSD schema.
 * <p>
 * HB: This is a modified version of org.codehaus.mojo.jaxb2.schemageneration.XsdGeneratorHelper
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>
 * @since 1.4
 */
public class MyXsdGeneratorHelper
{

    //~ Static fields/initializers -----------------------------------------------------------------
    // Constants
    private static final String MISCONFIG = "Misconfiguration detected: ";

    private static TransformerFactory FACTORY;

    private static final FileFilter RECURSIVE_XSD_FILTER;

    static {

        // Create the static filter used for recursive generated XSD files detection.
        RECURSIVE_XSD_FILTER = new FileFilter()
        {
            @Override
            public boolean accept (final File toMatch)
            {

                if (toMatch.exists()) {

                    // Accept directories for recursive operation, and
                    // files with names matching the SCHEMAGEN_EMITTED_FILENAME Pattern.
                    return toMatch.isDirectory()
                                   || AbstractXsdGeneratorMojo.SCHEMAGEN_EMITTED_FILENAME.matcher(
                                    toMatch.getName()).matches();
                }

                // Not a directory or XSD file.
                return false;
            }
        };
    }

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Hide the constructor for utility classes.
     */
    private MyXsdGeneratorHelper ()
    {
        // Do nothing.
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Acquires a map relating generated schema filename to its SimpleNamespaceResolver.
     *
     * @param outputDirectory The output directory of the generated schema files.
     * @return a map relating generated schema filename to an initialized SimpleNamespaceResolver.
     * @throws MojoExecutionException if two generated schema files used the same namespace URI.
     */
    public static Map<String, SimpleNamespaceResolver> getFileNameToResolverMap (
            final File outputDirectory)
            throws MojoExecutionException
    {

        final Map<String, SimpleNamespaceResolver> toReturn
                = new TreeMap<String, SimpleNamespaceResolver>();

        // Each generated schema file should be written to the output directory.
        // Each generated schema file should have a unique targetNamespace.
        File[] generatedSchemaFiles = outputDirectory.listFiles(new FileFilter()
        {
            public boolean accept (File pathname)
            {
                return pathname.getName().startsWith("schema") && pathname.getName()
                        .endsWith(".xsd");
            }
        });

        for (File current : generatedSchemaFiles) {
            toReturn.put(current.getName(), new SimpleNamespaceResolver(current));
        }

        return toReturn;
    }

    /**
     * Validates that the list of Schemas provided within the configuration all contain unique
     * values. Should a
     * MojoExecutionException be thrown, it contains informative text about the exact nature of the
     * configuration
     * problem - we should simplify for all plugin users.
     *
     * @param configuredTransformSchemas The List of configuration schemas provided to this mojo.
     * @throws MojoExecutionException if any two configuredSchemas instances contain duplicate
     *                                values for any of the
     *                                properties uri, prefix or file. Also throws a MojoExecutionException if the uri of any Schema
     *                                is null
     *                                or empty, or if none of the 'file' and 'prefix' properties are given within any of the
     *                                configuredSchema instances.
     */
    public static void validateSchemasInPluginConfiguration (
            final List<TransformSchema> configuredTransformSchemas)
            throws MojoExecutionException
    {

        final List<String> uris = new ArrayList<String>();
        final List<String> prefixes = new ArrayList<String>();
        final List<String> fileNames = new ArrayList<String>();

        for (int i = 0; i < configuredTransformSchemas.size(); i++) {
            final TransformSchema current = configuredTransformSchemas.get(i);
            final String currentURI = current.getUri();
            final String currentPrefix = current.getToPrefix();
            final String currentFile = current.getToFile();

            // We cannot work with a null or empty uri
            if (StringUtils.isEmpty(currentURI)) {
                throw new MojoExecutionException(MISCONFIG
                                                         + "Null or empty property 'uri' found in "
                                                         + "plugin configuration for schema element at index ["
                                                 + i + "]: " + current);
            }

            // No point in having *only* a namespace.
            if (StringUtils.isEmpty(currentPrefix) && StringUtils.isEmpty(currentFile)) {
                throw new MojoExecutionException(MISCONFIG + "Null or empty properties 'prefix' "
                                                         + "and 'file' found within plugin configuration for schema element at index ["
                                                 + i + "]: " + current);
            }

            // Validate that all given uris are unique.
            if (uris.contains(currentURI)) {
                throw new MojoExecutionException(getDuplicationErrorMessage("uri", currentURI,
                                                                            uris.indexOf(currentURI),
                                                                            i));
            }
            uris.add(currentURI);

            // Validate that all given prefixes are unique.
            if (prefixes.contains(currentPrefix) && !(currentPrefix == null)) {
                throw new MojoExecutionException(getDuplicationErrorMessage("prefix", currentPrefix,
                                                                            prefixes.indexOf(
                                                                                    currentPrefix),
                                                                            i));
            }
            prefixes.add(currentPrefix);

            // Validate that all given files are unique.
            if (fileNames.contains(currentFile)) {
                throw new MojoExecutionException(getDuplicationErrorMessage("file", currentFile,
                                                                            fileNames.indexOf(
                                                                                    currentFile), i));
            }
            fileNames.add(currentFile);
        }
    }

    /**
     * Inserts XML documentation annotations into all generated XSD files found
     * within the supplied outputDir.
     *
     * @param log       A Maven Log.
     * @param outputDir The outputDir, where generated XSD files are found.
     * @param docs      The SearchableDocumentation for the source files within the compilation
     *                  unit.
     * @param renderer  The JavaDocRenderer used to convert JavaDoc annotations into XML
     *                  documentation annotations.
     * @return The number of processed XSDs.
     */
    public static int insertJavaDocAsAnnotations (final Log log,
                                                  final String encoding,
                                                  final File outputDir,
                                                  final SearchableDocumentation docs,
                                                  final JavaDocRenderer renderer)
    {

        // Check sanity
        Validate.notNull(docs, "docs");
        Validate.notNull(log, "log");
        Validate.notNull(outputDir, "outputDir");
        Validate.isTrue(outputDir.isDirectory(), "'outputDir' must be a Directory.");
        Validate.notNull(renderer, "renderer");

        int processedXSDs = 0;
        final List<File> foundFiles = new ArrayList<File>();
        addRecursively(foundFiles, RECURSIVE_XSD_FILTER, outputDir);

        if (foundFiles.size() > 0) {

            // Create the processors.
            final MyXsdAnnotationProcessor classProcessor = new MyXsdAnnotationProcessor(docs,
                                                                                         renderer);
//            final XsdEnumerationAnnotationProcessor enumProcessor
//                    = new XsdEnumerationAnnotationProcessor(docs, renderer);

            for (File current : foundFiles) {

                // Create an XSD document from the current File.
                final Document generatedSchemaFileDocument = parseXmlToDocument(current);

                // Replace all namespace prefixes within the provided document.
                process(generatedSchemaFileDocument.getFirstChild(), true, classProcessor);
                processedXSDs++;

                // Overwrite the vanilla file.
                savePrettyPrintedDocument(generatedSchemaFileDocument, current, encoding);
            }

        } else {
            if (log.isWarnEnabled()) {
                log.warn("Found no generated 'vanilla' XSD files to process under ["
                                 + FileSystemUtilities.getCanonicalPath(outputDir)
                                 + "]. Aborting processing.");
            }
        }

        // All done.
        return processedXSDs;
    }

    /**
     * Replaces all namespaces within generated schema files, as instructed by the configured Schema
     * instances.
     *
     * @param resolverMap                The map relating generated schema file name to
     *                                   SimpleNamespaceResolver instances.
     * @param configuredTransformSchemas The Schema instances read from the configuration of this
     *                                   plugin.
     * @param mavenLog                   The active Log.
     * @param schemaDirectory            The directory where all generated schema files reside.
     * @param encoding                   The encoding to use when writing the file.
     * @throws MojoExecutionException If the namespace replacement could not be done.
     */
    public static void replaceNamespacePrefixes (
            final Map<String, SimpleNamespaceResolver> resolverMap,
            final List<TransformSchema> configuredTransformSchemas,
            final Log mavenLog,
            final File schemaDirectory,
            final String encoding)
            throws MojoExecutionException
    {

        if (mavenLog.isDebugEnabled()) {
            mavenLog
                    .debug("Got resolverMap.keySet() [generated filenames]: " + resolverMap.keySet());
        }

        for (SimpleNamespaceResolver currentResolver : resolverMap.values()) {
            File generatedSchemaFile
                    = new File(schemaDirectory, currentResolver.getSourceFilename());
            Document generatedSchemaFileDocument = null;

            for (TransformSchema currentTransformSchema : configuredTransformSchemas) {
                // Should we alter the namespace prefix as instructed by the current schema?
                final String newPrefix = currentTransformSchema.getToPrefix();
                final String currentUri = currentTransformSchema.getUri();

                if (StringUtils.isNotEmpty(newPrefix)) {
                    // Find the old/current prefix of the namespace for the current schema uri.
                    final String oldPrefix = currentResolver.getNamespaceURI2PrefixMap().get(
                            currentUri);

                    if (StringUtils.isNotEmpty(oldPrefix)) {
                        // Can we perform the prefix substitution?
                        validatePrefixSubstitutionIsPossible(oldPrefix, newPrefix, currentResolver);

                        if (mavenLog.isDebugEnabled()) {
                            mavenLog.debug("Subtituting namespace prefix [" + oldPrefix + "] with ["
                                                   + newPrefix
                                                   + "] in file [" + currentResolver
                                            .getSourceFilename() + "].");
                        }

                        // Get the Document of the current schema file.
                        if (generatedSchemaFileDocument == null) {
                            generatedSchemaFileDocument = parseXmlToDocument(generatedSchemaFile);
                        }

                        // Replace all namespace prefixes within the provided document.
                        process(generatedSchemaFileDocument.getFirstChild(), true,
                                new ChangeNamespacePrefixProcessor(oldPrefix, newPrefix));
                    }
                }
            }

            if (generatedSchemaFileDocument != null) {
                // Overwrite the generatedSchemaFile with the content of the generatedSchemaFileDocument.
                mavenLog.debug("Overwriting file [" + currentResolver.getSourceFilename()
                                       + "] with content ["
                                       + getHumanReadableXml(generatedSchemaFileDocument) + "]");
                savePrettyPrintedDocument(generatedSchemaFileDocument, generatedSchemaFile, encoding);
            } else {
                mavenLog.debug("No namespace prefix changes to generated schema file ["
                                       + generatedSchemaFile.getName() + "]");
            }
        }
    }

    /**
     * Updates all schemaLocation attributes within the generated schema files to match the 'file'
     * properties within the
     * Schemas read from the plugin configuration. After that, the files are physically renamed.
     *
     * @param resolverMap                The map relating generated schema file name to
     *                                   SimpleNamespaceResolver instances.
     * @param configuredTransformSchemas The Schema instances read from the configuration of this
     *                                   plugin.
     * @param mavenLog                   The active Log.
     * @param schemaDirectory            The directory where all generated schema files reside.
     * @param charsetName                The encoding / charset name.
     */
    public static void renameGeneratedSchemaFiles (
            final Map<String, SimpleNamespaceResolver> resolverMap,
            final List<TransformSchema> configuredTransformSchemas,
            final Log mavenLog,
            final File schemaDirectory,
            final String charsetName)
    {

        // Create the map relating namespace URI to desired filenames.
        Map<String, String> namespaceUriToDesiredFilenameMap = new TreeMap<String, String>();
        for (TransformSchema current : configuredTransformSchemas) {
            if (StringUtils.isNotEmpty(current.getToFile())) {
                namespaceUriToDesiredFilenameMap.put(current.getUri(), current.getToFile());
            }
        }

        // Replace the schemaLocation values to correspond to the new filenames
        for (SimpleNamespaceResolver currentResolver : resolverMap.values()) {
            File generatedSchemaFile
                    = new File(schemaDirectory, currentResolver.getSourceFilename());
            Document generatedSchemaFileDocument = parseXmlToDocument(generatedSchemaFile);

            // Replace all namespace prefixes within the provided document.
            process(generatedSchemaFileDocument.getFirstChild(), true,
                    new ChangeFilenameProcessor(namespaceUriToDesiredFilenameMap));

            // Overwrite the generatedSchemaFile with the content of the generatedSchemaFileDocument.
            if (mavenLog.isDebugEnabled()) {
                mavenLog.debug("Changed schemaLocation entries within [" + currentResolver
                        .getSourceFilename() + "]. "
                                       + "Result: [" + getHumanReadableXml(
                                generatedSchemaFileDocument) + "]");
            }
            savePrettyPrintedDocument(generatedSchemaFileDocument, generatedSchemaFile, charsetName);
        }

        // Now, rename the actual files.
        for (SimpleNamespaceResolver currentResolver : resolverMap.values()) {
            final String localNamespaceURI = currentResolver.getLocalNamespaceURI();

            if (StringUtils.isEmpty(localNamespaceURI)) {
                mavenLog.warn(
                        "SimpleNamespaceResolver contained no localNamespaceURI; aborting rename.");
                continue;
            }

            final String newFilename = namespaceUriToDesiredFilenameMap.get(localNamespaceURI);
            final File originalFile = new File(schemaDirectory, currentResolver.getSourceFilename());

            if (StringUtils.isNotEmpty(newFilename)) {
                File renamedFile = FileUtils.resolveFile(schemaDirectory, newFilename);
                String renameResult = (originalFile.renameTo(renamedFile) ? "Success " : "Failure ");

                if (mavenLog.isDebugEnabled()) {
                    String suffix = "renaming [" + originalFile.getAbsolutePath() + "] to ["
                                            + renamedFile + "]";
                    mavenLog.debug(renameResult + suffix);
                }
            }
        }
    }

    /**
     * Drives the supplied visitor to process the provided Node and all its children, should the
     * recurseToChildren flag
     * be set to <code>true</code>. All attributes of the current node are processed before
     * recursing to children (i.e.
     * breadth first recursion).
     *
     * @param node              The Node to process.
     * @param recurseToChildren if <code>true</code>, processes all children of the supplied node
     *                          recursively.
     * @param visitor           The NodeProcessor instance which should process the nodes.
     */
    public static void process (final Node node,
                                final boolean recurseToChildren,
                                final NodeProcessor visitor)
    {

        // Process the current Node, if the NodeProcessor accepts it.
        if (visitor.accept(node)) {
            visitor.process(node);
        }

        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);

            // Process the current attribute, if the NodeProcessor accepts it.
            if (visitor.accept(attribute)) {
                visitor.process(attribute);
            }
        }

        if (recurseToChildren) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);

                // Recurse to Element children.
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    process(child, true, visitor);
                }
            }
        }
    }

    /**
     * Parses the provided InputStream to create a dom Document.
     *
     * @param xmlStream An InputStream connected to an XML document.
     * @return A DOM Document created from the contents of the provided stream.
     */
    public static Document parseXmlStream (final Reader xmlStream)
    {

        // Build a DOM model of the provided xmlFileStream.
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        try {
            return factory.newDocumentBuilder().parse(new InputSource(xmlStream));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not acquire DOM Document", e);
        }
    }

    /**
     * Converts the provided DOM Node to a pretty-printed XML-formatted string.
     *
     * @param node The Node whose children should be converted to a String.
     * @return a pretty-printed XML-formatted string.
     */
    protected static String getHumanReadableXml (final Node node)
    {
        StringWriter toReturn = new StringWriter();

        try {
            Transformer transformer = getFactory().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.transform(new DOMSource(node), new StreamResult(toReturn));
        } catch (TransformerException e) {
            throw new IllegalStateException("Could not transform node [" + node.getNodeName()
                                                    + "] to XML", e);
        }

        return toReturn.toString();
    }

    //
    // Private helpers
    //
    private static String getDuplicationErrorMessage (final String propertyName,
                                                      final String propertyValue,
                                                      final int firstIndex,
                                                      final int currentIndex)
    {
        return MISCONFIG + "Duplicate '" + propertyName + "' property with value [" + propertyValue
                       + "] found in plugin configuration. Correct schema elements index ("
                       + firstIndex + ") and ("
                       + currentIndex + "), to ensure that all '" + propertyName
                       + "' values are unique.";
    }

    /**
     * Validates that the transformation from <code>oldPrefix</code> to <code>newPrefix</code> is
     * possible, in that
     * <code>newPrefix</code> is not already used by a schema file. This would corrupt the schema by
     * assigning elements
     * from one namespace to another.
     *
     * @param oldPrefix       The old/current namespace prefix.
     * @param newPrefix       The new/future namespace prefix.
     * @param currentResolver The currently active SimpleNamespaceResolver.
     * @throws MojoExecutionException if any schema file currently uses <code>newPrefix</code>.
     */
    private static void validatePrefixSubstitutionIsPossible (final String oldPrefix,
                                                              final String newPrefix,
                                                              final SimpleNamespaceResolver currentResolver)
            throws MojoExecutionException
    {
        // Make certain the newPrefix does not exist already.
        if (currentResolver.getNamespaceURI2PrefixMap().containsValue(newPrefix)) {
            throw new MojoExecutionException(MISCONFIG + "Namespace prefix [" + newPrefix
                                                     + "] is already in use."
                                                     + " Cannot replace namespace prefix ["
                                                     + oldPrefix + "] with [" + newPrefix
                                                     + "] in file ["
                                                     + currentResolver.getSourceFilename() + "].");
        }
    }

    /**
     * Creates a Document from parsing the XML within the provided xmlFile.
     *
     * @param xmlFile The XML file to be parsed.
     * @return The Document corresponding to the xmlFile.
     */
    private static Document parseXmlToDocument (final File xmlFile)
    {
        Document result = null;
        Reader reader = null;
        try {
            reader = new FileReader(xmlFile);
            result = parseXmlStream(reader);
        } catch (FileNotFoundException e) {
            // This should never happen...
        } finally {
            IOUtil.close(reader);
        }

        return result;
    }

    private static void savePrettyPrintedDocument (final Document toSave,
                                                   final File targetFile,
                                                   final String charsetName)
    {
        Writer out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile),
                                                            charsetName));
            out.write(getHumanReadableXml(toSave.getFirstChild()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not write to file [" + targetFile
                    .getAbsolutePath() + "]", e);
        } finally {
            IOUtil.close(out);
        }
    }

    private static void addRecursively (final List<File> toPopulate,
                                        final FileFilter fileFilter,
                                        final File aDir)
    {

        // Check sanity
        Validate.notNull(toPopulate, "toPopulate");
        Validate.notNull(fileFilter, "fileFilter");
        Validate.notNull(aDir, "aDir");

        // Add all matching files.
        for (File current : aDir.listFiles(fileFilter)) {

            if (current.isFile()) {
                toPopulate.add(current);
            } else if (current.isDirectory()) {
                addRecursively(toPopulate, fileFilter, current);
            }
        }
    }

    private static TransformerFactory getFactory ()
    {

        if (FACTORY == null) {

            try {
                FACTORY = TransformerFactory.newInstance();

                // Harmonize XML formatting
                for (String currentAttributeName : Arrays.asList("indent-number", OutputKeys.INDENT)) {
                    try {
                        FACTORY.setAttribute(currentAttributeName, 2);
                    } catch (IllegalArgumentException ex) {
                        // Ignore this.
                    }
                }
            } catch (Throwable exception) {

                // This should really not happen... but it seems to happen in some test cases.
                throw new IllegalStateException(
                        "Could not acquire TransformerFactory implementation.", exception);
            }
        }

        // All done.
        return FACTORY;
    }
}
