package com.roo.bootstrap;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.operations.AbstractOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Implementation of {@link BootstrapOperations} interface.
 * 
 * @since 1.1.1
 */
@Component
@Service
public class BootstrapOperationsImpl extends AbstractOperations implements BootstrapOperations {

	/**
	 * Get hold of a JDK Logger
	 */
	private Logger log = Logger.getLogger(getClass().getName());

	private static final char SEPARATOR = File.separatorChar;

	/**
	 * Get a reference to the ProjectOperations from the underlying OSGi
	 * container. Make sure you are referencing the Roo bundle which contains
	 * this service in your add-on pom.xml.
	 */
	@Reference
	private ProjectOperations projectOperations;

	@Reference
	MetadataService metadataService;

	/** {@inheritDoc} */
	public String getProperty(String propertyName) {
		Validate.notBlank(propertyName, "Property name required");
		return System.getProperty(propertyName);
	}

	public boolean isInstallBootstrapAvailable() {
		return isProjectAvailable() && isControllerAvailable();
	}

	private boolean isProjectAvailable() {
		return projectOperations.isFocusedProjectAvailable();
	}

	private boolean isControllerAvailable() {
		PathResolver pathResolver = projectOperations.getPathResolver();
		return fileManager.exists(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views"))
				&& !projectOperations.isFeatureInstalledInFocusedModule(FeatureNames.JSF)
				&& fileManager.exists(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "spring" + SEPARATOR
						+ "webmvc-config.xml"))
				&& fileManager.exists(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "tags"));
	}

	/** {@inheritDoc} */
	public void installBootstrap() {
		// Use PathResolver to get canonical resource names for a given artifact
		PathResolver pathResolver = projectOperations.getPathResolver();
		Element configuration = XmlUtils.getConfiguration(getClass());
		// Install
		this.updatePomProperties(configuration);
		this.updateDependencies(configuration);

		copyDirectoryContents("images/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "images"), true);
		copyDirectoryContents("styles/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "styles"), true);
		copyDirectoryContents("WEB-INF/layouts/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "layouts"), true);
		copyDirectoryContents("WEB-INF/views/*.*", pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "views"), true);
		copyDirectoryContents("WEB-INF/tags/form/*.*",
				pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "tags" + SEPARATOR + "form"), true);
		copyDirectoryContents("WEB-INF/tags/form/fields/*.*",
				pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "tags" + SEPARATOR + "form" + SEPARATOR + "fields"),
				true);
		copyDirectoryContents("WEB-INF/tags/menu/*.*",
				pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "tags" + SEPARATOR + "menu"), true);
		copyDirectoryContents("WEB-INF/tags/util/*.*",
				pathResolver.getFocusedIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF" + SEPARATOR + "tags" + SEPARATOR + "util"), true);
		copyFullDirectoryContents("META-INF/web-resources/dojo-1.7.2/**", "META-INF/web-resources/dojo-1.7.2/",
				pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF" + SEPARATOR + "web-resources" + SEPARATOR + "dojo-1.7.2"), true);
		copyDirectoryContents("META-INF/web-resources/spring-custom/*.*",
				pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF" + SEPARATOR + "web-resources" + SEPARATOR + "spring-custom"),
				true);

	}

	private void updatePomProperties(Element configuration) {
		List<Element> properties = XmlUtils.findElements("/configuration/springjs/properties/*", configuration);
		for (Element property : properties) {
			projectOperations.addProperty(projectOperations.getFocusedModuleName(), new Property(property));
		}
	}

	private void updateDependencies(Element configuration) {

		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> springJsDependencies = XmlUtils.findElements("/configuration/springjs/dependencies/dependency", configuration);
		for (Element dependencyElement : springJsDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		projectOperations.addDependencies(projectOperations.getFocusedModuleName(), dependencies);
	}

	private void copyFullDirectoryContents(final String sourceAntPath, final String sourceDirectory, String targetDirectory, final boolean replace) {
		Validate.notBlank(sourceAntPath, "Source path required");
		Validate.notBlank(targetDirectory, "Target directory required");

		if (!targetDirectory.endsWith("/")) {
			targetDirectory += "/";
		}

		if (!fileManager.exists(targetDirectory)) {
			fileManager.createDirectory(targetDirectory);
		}

		final String path = FileUtils.getPath(getClass(), sourceAntPath);
		final Iterable<URL> urls = OSGiUtils.findEntriesByPattern(context.getBundleContext(), path);
		Validate.notNull(urls, "Could not search bundles for resources for Ant Path '" + path + "'");
		for (final URL url : urls) {
			final String filePath = url.getPath();
			final String fileName = url.getPath().substring(url.getPath().lastIndexOf("/") + 1);
			if (replace) {
				try {
					String contents = IOUtils.toString(url);
					fileManager.createOrUpdateTextFileIfRequired(targetDirectory + getRelativeFilePath(sourceDirectory, filePath), contents, false);
				} catch (final Exception e) {
					throw new IllegalStateException(e);
				}
			} else {
				if (!fileManager.exists(targetDirectory + fileName)) {
					InputStream inputStream = null;
					OutputStream outputStream = null;
					try {
						inputStream = url.openStream();
						outputStream = fileManager.createFile(targetDirectory + getRelativeFilePath(sourceDirectory, filePath)).getOutputStream();
						IOUtils.copy(inputStream, outputStream);
					} catch (final Exception e) {
						throw new IllegalStateException("Encountered an error during copying of resources for the add-on.", e);
					} finally {
						IOUtils.closeQuietly(inputStream);
						IOUtils.closeQuietly(outputStream);
					}
				}
			}
		}
	}

	private String getRelativeFilePath(String sourceBaseDirectory, String filePath) {
		String result = null;
		int index = StringUtils.indexOf(filePath, sourceBaseDirectory);
		if (index != -1) {
			result = filePath.substring(index);
			result = result.replace(sourceBaseDirectory, "");
		}
		return result;
	}

}