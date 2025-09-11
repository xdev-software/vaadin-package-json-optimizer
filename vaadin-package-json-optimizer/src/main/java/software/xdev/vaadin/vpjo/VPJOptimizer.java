/*
 * Copyright © 2025 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.xdev.vaadin.vpjo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;

import software.xdev.vaadin.vpjo.internal.JacksonUtils;


/**
 * <b>V</b>aadin <b>P</b>ackage <b>J</b>son <b>O</b>ptimizer
 * <p>
 * Patches package.json and replaces unused packages with an empty package. This also prevents the installation of the
 * corresponding transitive dependencies and lowers the overall attack surface.
 * </p>
 * <p>
 * As of Vaadin 24.8 around 250 fewer packages (-45%) are installed.
 * </p>
 *
 * @see <a href="https://github.com/vaadin/flow/issues/22207#issuecomment-3270552153">vaadin/flow#22207</a>
 */
public class VPJOptimizer
{
	protected static final String OVERRIDES = "overrides";
	public static final String DEFAULT_EMPTY_PACKAGE_OVERRIDE = "npm:empty-npm-package@1.0.0";
	protected static final Set<String> DEFAULT_PACKAGES_TO_REPLACE_WITH_EMPTY = new LinkedHashSet<>(List.of(
		// glob CLI unused
		"jackspeak",
		"foreground-child",
		"package-json-from-dist",
		// rollup-plugin-visualizer CLI unused
		"yargs",
		"open",
		// transform-ast test only
		"nanobench",
		// Vaadin unused (from @vaadin/bundles)
		"quickselect",
		"@vaadin/board",
		"@vaadin/charts",
		"@vaadin/cookie-consent",
		"@vaadin/crud",
		"@vaadin/dashboard",
		"@vaadin/grid-pro",
		"@vaadin/map",
		"@vaadin/rich-text-editor",
		"cookieconsent",
		"highcharts",
		"ol",
		"rbush",
		// Workbox unused
		"workbox-google-analytics",
		"@surma/rollup-plugin-off-main-thread",
		"@babel/preset-env",
		"@babel/runtime",
		"@rollup/plugin-replace@2.4.2",
		"@rollup/plugin-babel",
		"@rollup/plugin-node-resolve",
		"@rollup/plugin-terser",
		"tempy"
	));
	
	protected final Consumer<String> logFunc;
	protected final String emptyPackageOverride;
	protected final Set<String> packagesToReplaceWithEmpty;
	
	public VPJOptimizer(
		final Consumer<String> logFunc,
		final String emptyPackageOverride,
		final Set<String> packagesToReplaceWithEmptyIgnore,
		final Set<String> packagesToReplaceWithEmptyAdditional)
	{
		this.logFunc = logFunc;
		this.emptyPackageOverride = Objects.requireNonNullElse(emptyPackageOverride, DEFAULT_EMPTY_PACKAGE_OVERRIDE);
		
		this.packagesToReplaceWithEmpty = Stream.concat(
			DEFAULT_PACKAGES_TO_REPLACE_WITH_EMPTY.stream()
				.filter(pkg -> packagesToReplaceWithEmptyIgnore == null
					|| !packagesToReplaceWithEmptyIgnore.contains(pkg)),
			packagesToReplaceWithEmptyAdditional != null
				? packagesToReplaceWithEmptyAdditional.stream()
				: Stream.empty()
		).collect(Collectors.toCollection(LinkedHashSet::new));
	}
	
	public void process(final Path packageJsonDir, final Path cacheFileDir)
	{
		final Path packageJsonPath = packageJsonDir.resolve("package.json");
		this.logFunc.accept("Trying to patch overrides in " + packageJsonPath.toAbsolutePath());
		
		final long startMs = System.currentTimeMillis();
		Result result = null;
		try
		{
			result = this.processInternal(
				packageJsonPath,
				cacheFileDir.resolve("package-json-add-overrides.cache"));
		}
		catch(final IOException e)
		{
			throw new UncheckedIOException(e);
		}
		finally
		{
			this.logFunc.accept("Result=" + result + ", took " + (System.currentTimeMillis() - startMs) + "ms");
		}
	}
	
	@SuppressWarnings({"PMD.CognitiveComplexity", "java:S3776"})
	protected Result processInternal(final Path packageJsonPath, final Path cacheFilePath) throws IOException
	{
		final boolean packageJsonExists = Files.exists(packageJsonPath);
		final ObjectNode packageJson;
		String originalContents = null;
		Integer originalContentHashCode = null;
		if(packageJsonExists)
		{
			originalContents = Files.readString(packageJsonPath);
			this.logFunc.accept("Checking for cache file at " + cacheFilePath.toAbsolutePath());
			if(Files.exists(cacheFilePath))
			{
				this.logFunc.accept("Cache exists - Trying to use it");
				try
				{
					originalContentHashCode = this.hashCodeFromPackageJsonContents(originalContents);
					if(Integer.parseInt(Files.readString(cacheFilePath)) == originalContentHashCode)
					{
						return Result.CACHE_MATCHED;
					}
				}
				catch(final Exception ex)
				{
					// Ignore
				}
			}
			packageJson = JacksonUtils.readTree(originalContents);
		}
		else
		{
			packageJson = this.createDefaultPackageJson();
		}
		
		final ObjectNode originalOverrides =
			Objects.requireNonNullElseGet((ObjectNode)packageJson.get(OVERRIDES), JacksonUtils::createObjectNode);
		final ObjectNode overrides = this.doOverrides(originalOverrides);
		if(originalOverrides.equals(overrides))
		{
			// Already up-to-date
			// Reuse already calculated
			if(originalContentHashCode == null && originalContents != null)
			{
				originalContentHashCode = this.hashCodeFromPackageJsonContents(originalContents);
			}
			// If there was no file and if there is nothing to update:
			// 1. that should be impossible
			// 2. Don't write the file
			if(originalContentHashCode != null)
			{
				this.createCacheFile(cacheFilePath, originalContentHashCode);
			}
			return Result.NO_CHANGES;
		}
		
		packageJson.set(OVERRIDES, overrides);
		final String fileContents = JacksonUtils.toFileJson(packageJson);
		Files.writeString(packageJsonPath, fileContents);
		
		this.createCacheFile(cacheFilePath, fileContents.hashCode());
		
		return Result.PATCHED;
	}
	
	protected int hashCodeFromPackageJsonContents(final String fileContents)
	{
		return Objects.hash(fileContents, this.emptyPackageOverride, this.packagesToReplaceWithEmpty);
	}
	
	protected ObjectNode doOverrides(final ObjectNode originalOverrides)
	{
		final ObjectNode overrides = originalOverrides.deepCopy();
		overrides.properties().removeIf(e -> this.emptyPackageOverride.equals(e.getValue().asText()));
		this.packagesToReplaceWithEmpty.forEach(pkg -> overrides.put(pkg, this.emptyPackageOverride));
		return overrides;
	}
	
	protected void createCacheFile(final Path cacheFilePath, final int contentHashCode) throws IOException
	{
		Files.createDirectories(cacheFilePath.getParent());
		Files.writeString(cacheFilePath, String.valueOf(contentHashCode));
	}
	
	protected ObjectNode createDefaultPackageJson()
	{
		final ObjectNode node = JacksonUtils.createObjectNode();
		// Vaadin/NPM defaults
		node.put("name", "no-name");
		node.put("license", "UNLICENSED");
		node.put("type", "module");
		return node;
	}
	
	protected enum Result
	{
		CACHE_MATCHED,
		NO_CHANGES,
		PATCHED
	}
}
