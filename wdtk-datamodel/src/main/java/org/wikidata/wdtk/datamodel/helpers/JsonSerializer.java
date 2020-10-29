package org.wikidata.wdtk.datamodel.helpers;

/*
 * #%L
 * Wikidata Toolkit Data Model
 * %%
 * Copyright (C) 2014 Wikidata Toolkit Developers
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class implements {@link EntityDocumentDumpProcessor} to provide a
 * serializer for {@link EntityDocument} objects in JSON.
 * <p>
 * The implementation does not check if {@link #open()} has been called before
 * the first document is serialized. It is the responsibility of the caller to
 * do this.
 * <p>
 * Implementations of the data model are expected to be appropriately serializable
 * to JSON with Jackson.
 *
 * @author Markus Kroetzsch
 *
 */
public class JsonSerializer implements EntityDocumentDumpProcessor {

	private static final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);

	private static final byte[] JSON_START_LIST = "[\n".getBytes(StandardCharsets.UTF_8);
	private static final byte[] JSON_SEP = ",\n".getBytes(StandardCharsets.UTF_8);
	private static final byte[] JSON_END_LIST = "\n]".getBytes(StandardCharsets.UTF_8);

	/**
	 * The stream that the resulting JSON is written to.
	 */
	private final OutputStream outputStream;

	/**
	 * Object mapper that is used to serialize JSON.
	 */
	protected static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
		mapper.setFilterProvider(new SimpleFilterProvider(Collections.singletonMap("formDocumentJsonFilter", buildFormDocumentJsonFilter())));
	}

	private static PropertyFilter buildFormDocumentJsonFilter() {
		return new SimpleBeanPropertyFilter() {
			@Override
			protected boolean include(BeanPropertyWriter writer) {
				return true;
			}

			@Override
			protected boolean include(PropertyWriter writer) {
				return true;
			}

			@Override
			public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception {
				if (include(writer)) {
					if (!writer.getName().equals("add")) {
						writer.serializeAsField(pojo, jgen, provider);
					} else {
						FormDocument formDocument = (FormDocument) pojo;
						if (FormIdValue.NULL.equals(formDocument.getEntityId())) {
							writer.serializeAsField(pojo, jgen, provider);
						} else {
							if (!jgen.canOmitFields()) {
								writer.serializeAsOmittedField(pojo, jgen, provider);
							}
						}
					}

				}
			}
		};
	}

	/**
	 * Counter for the number of documents serialized so far.
	 */
	private int entityDocumentCount;

	/**
	 * Creates a new JSON serializer that writes its output to the given stream.
	 * The output stream will be managed by the object, i.e., it will be closed
	 * when {@link #close()} is call ed.
	 *
	 * @param outputStream
	 *            the output stream to write to
	 */
	public JsonSerializer(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	@Override
	public void open() {
		this.entityDocumentCount = 0;

		try {
			this.outputStream.write(JSON_START_LIST);
		} catch (IOException e) {
			reportException(e);
		}
	}

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		serializeEntityDocument(itemDocument);
	}

	@Override
	public void processPropertyDocument(PropertyDocument propertyDocument) {
		serializeEntityDocument(propertyDocument);
	}

	@Override
	public void processLexemeDocument(LexemeDocument lexemeDocument) {
		serializeEntityDocument(lexemeDocument);
	}

	@Override
	public void processMediaInfoDocument(MediaInfoDocument mediaInfoDocument) {
		serializeEntityDocument(mediaInfoDocument);
	}

	@Override
	public void close() {
		try {
			this.outputStream.write(JSON_END_LIST);
			this.outputStream.close();
		} catch (IOException e) {
			reportException(e);
		}
	}

	/**
	 * Returns the number of entity documents serialized so far.
	 *
	 * @return number of serialized entity documents
	 */
	public int getEntityDocumentCount() {
		return this.entityDocumentCount;
	}

	/**
	 * Reports a given exception as a RuntimeException, since the interface does
	 * not allow us to throw checked exceptions directly.
	 *
	 * @param e
	 *            the exception to report
	 * @throws RuntimeException
	 *             in all cases
	 */
	private void reportException(Exception e) {
		logger.error("Failed to write JSON export: " + e.toString());
		throw new RuntimeException(e.toString(), e);
	}

	/**
	 * Writes the JSON serialization of the given {@link EntityDocument}.
	 *
	 * @param entityDocument
	 *            the document to serialize
	 */
	private void serializeEntityDocument(EntityDocument entityDocument) {
		try {
			if (this.entityDocumentCount > 0) {
				this.outputStream.write(JSON_SEP);
			}
			mapper.writeValue(this.outputStream, entityDocument);
		} catch (IOException e) {
			reportException(e);
		}
		this.entityDocumentCount++;
	}

	/**
	 * Serializes the given object in JSON and returns the resulting string. In
	 * case of errors, null is returned.
	 *
	 * @param itemDocument
	 *            object to serialize
	 * @return JSON serialization or null
	 */
	public static String getJsonString(ItemDocument itemDocument) {
		return jacksonObjectToString(itemDocument);
	}

	public static String getJsonString(LexemeDocument lexemeDocument) {
		return jacksonObjectToString(lexemeDocument);
	}

	/**
	 * Serializes the given object in JSON and returns the resulting string. In
	 * case of errors, null is returned.
	 *
	 * @param propertyDocument
	 *            object to serialize
	 * @return JSON serialization or null
	 */
	public static String getJsonString(PropertyDocument propertyDocument) {
		return jacksonObjectToString(propertyDocument);
	}

	/**
	 * Serializes the given object in JSON and returns the resulting string. In
	 * case of errors, null is returned.
	 *
	 * @param mediaInfoDocument
	 *            object to serialize
	 * @return JSON serialization or null
	 */
	public static String getJsonString(MediaInfoDocument mediaInfoDocument) {
		return jacksonObjectToString(mediaInfoDocument);
	}

	/**
	 * Serializes the given object in JSON and returns the resulting string. In
	 * case of errors, null is returned.
	 *
	 * @param statement
	 *            object to serialize
	 * @return JSON serialization or null
	 */
	public static String getJsonString(Statement statement) {
		return jacksonObjectToString(statement);
	}

	/**
	 * Serializes the given object in JSON and returns the resulting string. In
	 * case of errors, null is returned. In particular, this happens if the
	 * object is not based on a Jackson-annotated class. An error is logged in
	 * this case.
	 *
	 * @param object
	 *            object to serialize
	 * @return JSON serialization or null
	 */
	protected static String jacksonObjectToString(Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize JSON data: " + e.toString());
			return null;
		}
	}
}
