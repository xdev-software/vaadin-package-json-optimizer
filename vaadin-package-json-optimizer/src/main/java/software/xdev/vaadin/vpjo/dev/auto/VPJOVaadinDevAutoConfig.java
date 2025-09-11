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
package software.xdev.vaadin.vpjo.dev.auto;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import software.xdev.vaadin.vpjo.dev.VPJODevConfig;
import software.xdev.vaadin.vpjo.dev.VPJODevRunner;


@ConditionalOnProperty(value = "vpjo.dev.enabled", matchIfMissing = true)
@AutoConfiguration
public class VPJOVaadinDevAutoConfig
{
	@ConditionalOnMissingBean
	@Bean
	@ConfigurationProperties("vpjo.dev")
	public VPJODevConfig vpjoDevConfig()
	{
		return new VPJODevConfig();
	}
	
	@ConditionalOnMissingBean
	@Bean
	public VPJODevRunner vpjoDevServletContextListener(final VPJODevConfig config)
	{
		return new VPJODevRunner(config);
	}
}
