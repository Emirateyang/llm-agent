/*
 *  Copyright (c) 2023-2025, llm-agent (emirate.yang@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.llmagent.vector.store;

import com.llmagent.data.document.Document;
import com.llmagent.data.document.id.DocumentIdGenerator;
import com.llmagent.data.document.id.DocumentIdGeneratorFactory;
import com.llmagent.data.document.DocumentSplitter;
import com.llmagent.llm.embedding.EmbeddingModel;

import java.util.Collection;
import java.util.List;

public abstract class DocumentStore extends VectorStore<Document> {

    /**
     * DocumentStore can use external embeddings models or its own embeddings
     * Many vector databases come with the ability to embed themselves
     */
    private EmbeddingModel embeddingModel;

    private DocumentSplitter documentSplitter;

    private DocumentIdGenerator documentIdGenerator = DocumentIdGeneratorFactory.getDocumentIdGenerator();

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public DocumentSplitter getDocumentSplitter() {
        return documentSplitter;
    }

    public void setDocumentSplitter(DocumentSplitter documentSplitter) {
        this.documentSplitter = documentSplitter;
    }

    public DocumentIdGenerator getDocumentIdGenerator() {
        return documentIdGenerator;
    }

    public void setDocumentIdGenerator(DocumentIdGenerator documentIdGenerator) {
        this.documentIdGenerator = documentIdGenerator;
    }

    @Override
    public StoreResult add(List<Document> documents, StoreOptions options) {
        if (options == null) {
            options = StoreOptions.DEFAULT;
        }

        if (documentSplitter != null) {
            documents = documentSplitter.splitAll(documents, documentIdGenerator);
        }
        // use the documentIdGenerator create unique id for document
        else if (documentIdGenerator != null) {
            for (Document document : documents) {
                if (document.getId() == null) {
                    String id = documentIdGenerator.generateId(document);
                    document.setId(id);
                }
            }
        }

        embedDocumentsIfNecessary(documents, options);

        return addImplement(documents, options);
    }

    @Override
    public StoreResult delete(Collection<Object> ids, StoreOptions options) {
        if (options == null) {
            options = StoreOptions.DEFAULT;
        }
        return deleteImplement(ids, options);
    }

    @Override
    public List<Document> search(SearchWrapper wrapper, StoreOptions options) {
        if (options == null) {
            options = StoreOptions.DEFAULT;
        }

        if (wrapper.getEmbedding() == null && embeddingModel != null && wrapper.isWithVector()) {
            VectorData vectorData = embeddingModel.embed(Document.of(wrapper.getText())).content();
            if (vectorData != null) {
                wrapper.setEmbedding(vectorData.getEmbedding());
            }
        }

        return searchImplement(wrapper, options);
    }


    protected void embedDocumentsIfNecessary(List<Document> documents, StoreOptions options) {
        if (embeddingModel == null) {
            return;
        }
        for (Document document : documents) {
            if (document.getEmbedding() == null) {
                VectorData vectorData = embeddingModel.embed(document).content();
                if (vectorData != null) {
                    document.setEmbedding(vectorData.getEmbedding());
                }
            }
        }
    }

    public abstract StoreResult addImplement(List<Document> documents, StoreOptions options);

    public abstract StoreResult deleteImplement(Collection<Object> ids, StoreOptions options);

    public abstract List<Document> searchImplement(SearchWrapper wrapper, StoreOptions options);
}
