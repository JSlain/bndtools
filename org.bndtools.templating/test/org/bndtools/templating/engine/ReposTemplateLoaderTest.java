package org.bndtools.templating.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.bndtools.templating.repobased.RepoPluginsBundleLocator;
import org.bndtools.templating.repobased.ReposTemplateLoader;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;

public class ReposTemplateLoaderTest {

    private ReposTemplateLoader loader;

    @Before
    public void setup() throws Exception {
        Run project = Run.createRun(null, IO.getFile("testdata/ws.bndrun"));
        Workspace ws = project.getWorkspace();
        RepoPluginsBundleLocator locator = new RepoPluginsBundleLocator(ws.getRepositories());
        loader = new ReposTemplateLoader(ws.getPlugins(Repository.class), locator);
    }

    @Test
    public void testLoad() throws Exception {
        List<Template> templates = loader.findTemplates("test1", new NullProgressMonitor());
        assertEquals(1, templates.size());
        Template template = templates.get(0);
        assertEquals("Hello", template.getName());
        assertEquals(0, template.getRanking());
        assertNull(template.getCategory());
    }

    @Test
    public void testProcessTemplate() throws Exception {
        List<Template> templates = loader.findTemplates("test1", new NullProgressMonitor());
        assertEquals(1, templates.size());
        Template template = templates.get(0);

        Map<String,List<Object>> parameters = new HashMap<>();
        parameters.put("projectName", Collections.<Object> singletonList("org.example.foo"));
        parameters.put("srcDir", Collections.<Object> singletonList("src/main/java"));
        parameters.put("basePackageDir", Collections.<Object> singletonList("org/example/foo"));

        ResourceMap outputs = template.generateOutputs(parameters);

        assertEquals(5, outputs.size());

        Entry<String,Resource> entry;

        Iterator<Entry<String,Resource>> iter = outputs.entries().iterator();
        entry = iter.next();
        assertEquals("src/main/java/org/example/foo/Activator.java", entry.getKey());
        assertEquals("package org.example.foo; public class Activator {}", IO.collect(entry.getValue().getContent()));

        entry = iter.next();
        assertEquals("pic.jpg", entry.getKey());
        // Check the digest of the pic to ensure it didn't get damaged by the templating engine
        DigestInputStream digestStream = new DigestInputStream(entry.getValue().getContent(), MessageDigest.getInstance("SHA-256"));
        IO.drain(digestStream);
        byte[] digest = digestStream.getMessageDigest().digest();
        assertEquals("ea5d770bc2deddb1f9a20df3ad337bdc1490ba7b35fa41c33aa4e9a534e82ada", Hex.toHexString(digest).toLowerCase());

        entry = iter.next();
        assertEquals("src/main/java/", entry.getKey());

        entry = iter.next();
        assertEquals("src/main/java/org/example/foo/", entry.getKey());

        entry = iter.next();
        assertEquals("bnd.bnd", entry.getKey());
        assertEquals("Bundle-SymbolicName: org.example.foo", IO.collect(entry.getValue().getContent()));
    }

    @Test
    public void testAlternateDelimiters() throws Exception {
        List<Template> templates = loader.findTemplates("test2", new NullProgressMonitor());
        assertEquals(1, templates.size());
        Template template = templates.get(0);

        Map<String,List<Object>> parameters = new HashMap<>();
        parameters.put("projectName", Collections.<Object> singletonList("org.example.foo"));
        parameters.put("srcDir", Collections.<Object> singletonList("src/main/java"));
        parameters.put("basePackageDir", Collections.<Object> singletonList("org/example/foo"));

        ResourceMap outputs = template.generateOutputs(parameters);

        assertEquals(5, outputs.size());

        Iterator<Entry<String,Resource>> iter = outputs.entries().iterator();
        Entry<String,Resource> entry;

        entry = iter.next();
        assertEquals("src/main/java/org/example/foo/Activator.java", entry.getKey());
        assertEquals("package org.example.foo; public class Activator {}", IO.collect(entry.getValue().getContent()));

        entry = iter.next();
        assertEquals("pic.jpg", entry.getKey());
        // Check the digest of the pic to ensure it didn't get damaged by the templating engine
        DigestInputStream digestStream = new DigestInputStream(entry.getValue().getContent(), MessageDigest.getInstance("SHA-256"));
        IO.drain(digestStream);
        byte[] digest = digestStream.getMessageDigest().digest();
        assertEquals("ea5d770bc2deddb1f9a20df3ad337bdc1490ba7b35fa41c33aa4e9a534e82ada", Hex.toHexString(digest).toLowerCase());

        entry = iter.next();
        assertEquals("src/main/java/", entry.getKey());

        entry = iter.next();
        assertEquals("src/main/java/org/example/foo/", entry.getKey());

        entry = iter.next();
        assertEquals("bnd.bnd", entry.getKey());
        assertEquals("Bundle-SymbolicName: org.example.foo", IO.collect(entry.getValue().getContent()));
    }

    @Test
    public void testReferTemplateDefinitions() throws Exception {
        List<Template> templates = loader.findTemplates("test3", new NullProgressMonitor());
        assertEquals(1, templates.size());
        Template template = templates.get(0);

        Map<String,List<Object>> parameters = new HashMap<>();
        parameters.put("name", Collections.<Object> singletonList("Homer Simpson"));

        ResourceMap outputs = template.generateOutputs(parameters);
        assertEquals(1, outputs.size());

        Iterator<Entry<String,Resource>> iter = outputs.entries().iterator();
        Entry<String,Resource> entry;

        entry = iter.next();
        assertEquals("example.html", entry.getKey());
        assertEquals("My name is <i>Homer Simpson</i>!", IO.collect(entry.getValue().getContent()));
    }

    @Test
    public void testExtendUnprocessedPatternAndIgnore() throws Exception {
        List<Template> templates = loader.findTemplates("test4", new NullProgressMonitor());
        assertEquals(1, templates.size());
        Template template = templates.get(0);

        Map<String,List<Object>> parameters = new HashMap<>();
        parameters.put("projectName", Collections.<Object> singletonList("org.example.foo"));

        ResourceMap outputs = template.generateOutputs(parameters);

        assertEquals(1, outputs.size());

        Entry<String,Resource> entry;
        Iterator<Entry<String,Resource>> iter = outputs.entries().iterator();
        entry = iter.next();
        assertEquals("pic.xxx", entry.getKey());
        // Check the digest of the pic to ensure it didn't get damaged by the templating engine
        DigestInputStream digestStream = new DigestInputStream(entry.getValue().getContent(), MessageDigest.getInstance("SHA-256"));
        IO.drain(digestStream);
        byte[] digest = digestStream.getMessageDigest().digest();
        assertEquals("ea5d770bc2deddb1f9a20df3ad337bdc1490ba7b35fa41c33aa4e9a534e82ada", Hex.toHexString(digest).toLowerCase());
    }

}
