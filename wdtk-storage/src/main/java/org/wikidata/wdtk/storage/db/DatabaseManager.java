package org.wikidata.wdtk.storage.db;

/*
 * #%L
 * Wikidata Toolkit Storage
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.wikidata.wdtk.storage.datamodel.EdgeContainer;
import org.wikidata.wdtk.storage.datamodel.Sort;
import org.wikidata.wdtk.storage.datamodel.SortSchema;
import org.wikidata.wdtk.storage.datamodel.Value;

/**
 * Overall management class for a database instance. Manages schema information
 * as well as data access.
 * 
 * @author Markus Kroetzsch
 * 
 */
public class DatabaseManager {

	protected final DbSortSchema sortSchema;

	protected final DB db;

	protected final Map<String, ValueDictionary> sortNameDictionaries;
	protected final Map<Integer, ValueDictionary> sortIdDictionaries;

	protected final Map<String, EdgeContainerIndex> sortNameEdgeSpoqs;

	protected final PropertyDictionary propertyDictionary;

	public DatabaseManager(String dbName) {
		File dbFile = new File(dbName + ".mapdb");
		// this.db = DBMaker.newMemoryDirectDB().make();
		this.db = DBMaker.newFileDB(dbFile).closeOnJvmShutdown().make();

		this.sortNameDictionaries = new HashMap<>();
		this.sortIdDictionaries = new HashMap<>();
		this.propertyDictionary = new PropertyDictionary(this);
		this.sortNameEdgeSpoqs = new HashMap<>();

		this.sortSchema = new DbSortSchema(this);

		// TODO open dictionaries for basic sorts; maybe just for string
	}

	public SortSchema getSortSchema() {
		return this.sortSchema;
	}

	public DB getDb() {
		return this.db;
	}

	public void commit() {
		this.db.commit();
	}

	public void close() {
		this.db.close();
	}

	public void updateEdges(EdgeContainer edgeContainer) {
		EdgeContainerIndex eci = this.getEdgeSpoqBySortName(edgeContainer
				.getSource().getSort().getName());
		eci.updateEdges(edgeContainer);

		// Recursively convert values to ids based on dictionaries
		// Build bytes for edge table
		// put
		// commit

		// for (PropertyTargets pt : edgeContainer) {
		// for (TargetQualifiers tq : pt) {
		// if (tq.getTarget() == null) {
		// continue;
		// }
		// if (WdtkSorts.SORTNAME_MTV.equals(tq.getTarget().getSort()
		// .getName())) {
		// if (tq.getTarget() instanceof LazyRecordValue) {
		// System.out.println("*** We need to talk. ***");
		// }
		// this.getOrCreateValueId(tq.getTarget());
		// }
		// }
		// }
	}

	public Value fetchValue(long id, String sortName) {
		Dictionary<Value> dictionary = getDictionaryBySortName(sortName);
		return dictionary.getValue(id);
	}

	public Value fetchValue(long id, int sortId) {
		Dictionary<Value> dictionary = getDictionaryBySortId(sortId);
		return dictionary.getValue(id);
	}

	public PropertySignature fetchPropertySignature(long id) {
		return this.propertyDictionary.getValue(id);
	}

	public long getOrCreateValueId(Value value) {
		Dictionary<Value> dictionary = getDictionaryBySortName(value.getSort()
				.getName());
		return dictionary.getOrCreateId(value);
	}

	public long getOrCreatePropertyId(String propertyName, String domainSort,
			String rangeSort) {
		int domainId = this.sortSchema.getSortId(domainSort);
		int rangeId = this.sortSchema.getSortId(rangeSort);

		return this.propertyDictionary.getOrCreateId(new PropertySignature(
				propertyName, domainId, rangeId));
	}

	public Iterable<Value> valueIterator(String sortName) {
		return getDictionaryBySortName(sortName);
	}

	public Iterable<PropertySignature> propertyIterator() {
		return this.propertyDictionary;
	}

	Dictionary<Value> getDictionaryBySortName(String sortName) {
		Dictionary<Value> dictionary = this.sortNameDictionaries.get(sortName);
		if (dictionary != null) {
			return dictionary;
		} else {
			throw new IllegalArgumentException("Objects of sort \"" + sortName
					+ "\" are not managed in any known dictionary.");
		}
	}

	Dictionary<Value> getDictionaryBySortId(int sortId) {
		Dictionary<Value> dictionary = this.sortIdDictionaries.get(sortId);
		if (dictionary != null) {
			return dictionary;
		} else {
			throw new IllegalArgumentException("No sort with id \"" + sortId
					+ "\" is known.");
		}
	}

	EdgeContainerIndex getEdgeSpoqBySortName(String sortName) {
		EdgeContainerIndex eci = this.sortNameEdgeSpoqs.get(sortName);
		if (eci == null) {
			Sort sort = this.sortSchema.getSort(sortName);
			eci = new EdgeContainerIndex(sort, this);
			this.sortNameEdgeSpoqs.put(sortName, eci);
		}
		return eci;
	}

	ValueDictionary initializeDictionary(Sort sort, int id) {
		ValueDictionary dictionary = null;
		switch (sort.getType()) {
		case STRING:
			dictionary = new StringValueDictionary(sort, this);
			break;
		case OBJECT:
			dictionary = new ObjectValueDictionary(sort, this);
			break;
		case RECORD:
			dictionary = new RecordValueDictionary(sort, this);
			break;
		default:
			System.out.println("Not setting up dictionary for sort "
					+ sort.getName());
		}

		if (dictionary != null) {
			this.sortNameDictionaries.put(sort.getName(), dictionary);
			this.sortIdDictionaries.put(id, dictionary);
		}

		return dictionary;
	}
}