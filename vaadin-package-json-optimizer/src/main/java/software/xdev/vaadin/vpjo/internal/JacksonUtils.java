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
package software.xdev.vaadin.vpjo.internal;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


/**
 * Emulates com.vaadin.flow.internal.JacksonUtils
 */
public final class JacksonUtils
{
	private static final ObjectMapper MAPPER = new ObjectMapper()
		.registerModule(new JavaTimeModule());
	
	private JacksonUtils()
	{
	}
	
	public static ObjectNode createObjectNode()
	{
		return MAPPER.createObjectNode();
	}
	
	public static ObjectNode readTree(final String json)
	{
		try
		{
			return (ObjectNode)MAPPER.readTree(json);
		}
		catch(final JsonProcessingException e)
		{
			throw new UncheckedIOException("Could not parse json content", e);
		}
	}
	
	public static String toFileJson(final JsonNode node)
		throws JsonProcessingException
	{
		final DefaultPrettyPrinter filePrinter = new DefaultPrettyPrinter(
			Separators.createDefaultInstance()
				.withObjectFieldValueSpacing(Separators.Spacing.AFTER));
		return MAPPER.writer().with(filePrinter).writeValueAsString(node);
	}
}
