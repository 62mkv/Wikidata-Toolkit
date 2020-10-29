package org.wikidata.wdtk.wikibaseapi;

/*-
 * #%L
 * Wikidata Toolkit Wikibase API
 * %%
 * Copyright (C) 2014 - 2020 Wikidata Toolkit Developers
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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.FormDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.LexemeDocument;
import org.wikidata.wdtk.datamodel.interfaces.LexemeIdValue;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;

public class WikibaseDataEditorWithTestWikidataTest {

    private static final String SITE_IRI = "http://test.wikidata.org/entity/";
    private static final String LANGUAGE_CODE = "en";

    WikibaseDataEditor wikibaseDataEditor;
    WikibaseDataFetcher wikibaseDataFetcher;

    @Before
    public void setUp() throws LoginFailedException {
        BasicApiConnection con = BasicApiConnection.getTestWikidataApiConnection();
        final String username = System.getProperty("wikidata.username");
        final String password = System.getProperty("wikidata.password");
        if (username != null && password != null) {
            con.login(username, password);
            this.wikibaseDataEditor = new WikibaseDataEditor(con, SITE_IRI);
            wikibaseDataEditor.setEditAsBot(true);

            this.wikibaseDataFetcher = new WikibaseDataFetcher(con, SITE_IRI);
        }
    }

    @Test
    @Ignore
    public void testCreateLexemeWithForms() throws IOException, MediaWikiApiErrorException {

        final ItemIdValue lexicalCategory = getItemIdForTestWikidata("Q212131");
        final ItemIdValue language = getItemIdForTestWikidata("Q208912");
        LexemeDocument lexemeDocument = Datamodel.makeLexemeDocument(
                LexemeIdValue.NULL,
                lexicalCategory,
                language,
                Collections.singletonList(Datamodel.makeMonolingualTextValue("april", LANGUAGE_CODE)));

        FormDocument formDocument = lexemeDocument.createForm(
                Collections.singletonList(Datamodel.makeMonolingualTextValue("aprils", LANGUAGE_CODE)),
                Collections.singletonList(getItemIdForTestWikidata("Q42"))
        );

        LexemeDocument withForm = lexemeDocument.withForm(formDocument);

        LexemeDocument result = wikibaseDataEditor
                .createLexemeDocument(withForm, "My summary", null);

        assertNotNull(result.getEntityId());
        System.out.println(result.toString());
    }

    @Test
    @Ignore
    public void addFormToExistingLexeme() throws MediaWikiApiErrorException, IOException {
        LexemeDocument existingLexeme = (LexemeDocument) wikibaseDataFetcher.getEntityDocument("L1358");
        FormDocument formDocument = existingLexeme.createForm(
                Collections.singletonList(Datamodel.makeMonolingualTextValue("aprils", LANGUAGE_CODE)),
                Collections.singletonList(getItemIdForTestWikidata("Q42"))
        );

        LexemeDocument withForm = existingLexeme.withForm(formDocument);

        LexemeDocument result = wikibaseDataEditor
                .updateLexemeDocument(withForm, "Adding form to existing lexeme", null);

        assertNotNull(result.getEntityId());
        System.out.println(result.toString());
    }

    private ItemIdValue getItemIdForTestWikidata(String id) {
        return Datamodel.makeItemIdValue(id, SITE_IRI);
    }

}
