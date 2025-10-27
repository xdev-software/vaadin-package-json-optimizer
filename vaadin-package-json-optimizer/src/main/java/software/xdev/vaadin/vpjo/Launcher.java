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

import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public final class Launcher
{
	public static void main(final String[] args)
	{
		new VPJOptimizer(
			System.out::println,
			System.getProperty("vpjo.emptyPackageOverride"),
			getSetFromProperty("vpjo.packagesToReplaceWithEmptyIgnore"),
			getSetFromProperty("vpjo.packagesToReplaceWithEmptyAdditional"))
			.process(Paths.get(args[0]), Paths.get(args[1]));
	}
	
	private static Set<String> getSetFromProperty(final String property)
	{
		final String propValue = System.getProperty(property);
		return propValue == null
			? Set.of()
			: new LinkedHashSet<>(List.of(propValue.split(",")));
	}
	
	private Launcher()
	{
	}
}
