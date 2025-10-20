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
package software.xdev.vaadin.vpjo.dev;

import jakarta.servlet.ServletContextListener;

import org.slf4j.LoggerFactory;

import software.xdev.vaadin.vpjo.VPJOptimizer;


public class VPJODevRunner implements ServletContextListener
{
	private final VPJODevConfig config;
	
	public VPJODevRunner(final VPJODevConfig config)
	{
		this.config = config;
		// Execute this BEFORE Vaadin initializes
		this.exec();
	}
	
	protected void exec()
	{
		if(!this.isVaadinDevPresent())
		{
			LoggerFactory.getLogger(this.getClass())
				.warn("Unable to locate vaadin-dev on classpath. "
					+ "Did you forget to exclude THIS library in production?");
			return;
		}
		
		new VPJOptimizer(
			LoggerFactory.getLogger(VPJOptimizer.class)::info,
			this.config.getEmptyPackageOverride(),
			this.config.getPackagesToReplaceWithEmptyIgnore(),
			this.config.getPackagesToReplaceWithEmptyAdditional())
			.process(this.config.getPackageJsonDir(), this.config.getCacheFileDir());
	}
	
	protected boolean isVaadinDevPresent()
	{
		return this.config.getVaadinDevModeIndicationClasses().stream().anyMatch(clazz -> {
			try
			{
				Class.forName(clazz);
				return true;
			}
			catch(final ClassNotFoundException e)
			{
				return false;
			}
		});
	}
}
