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

import com.llmagent.document.Document;
import com.llmagent.document.id.DocumentIdGenerator;
import com.llmagent.document.id.DocumentIdGeneratorFactory;
import com.llmagent.document.DocumentSplitter;
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
    public StoreResult store(List<Document> documents, StoreOptions options) {
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
                    Object id = documentIdGenerator.generateId(document);
                    document.setId(id);
                }
            }
        }

        embedDocumentsIfNecessary(documents, options);

        return storeImplement(documents, options);
    }

    @Override
    public StoreResult delete(Collection<Object> ids, StoreOptions options) {
        if (options == null) {
            options = StoreOptions.DEFAULT;
        }
        return deleteImplement(ids, options);
    }

    @Override
    public StoreResult update(List<Document> documents, StoreOptions options) {
        if (options == null) {
            options = StoreOptions.DEFAULT;
        }

        embedDocumentsIfNecessary(documents, options);
        return updateImplement(documents, options);
    }


    @Override
    public List<Document> search(SearchWrapper wrapper, StoreOptions options) {
        if (options == null) {
            options = StoreOptions.DEFAULT;
        }

        if (wrapper.getVector() == null && embeddingModel != null && wrapper.isWithVector()) {
            VectorData vectorData = embeddingModel.embed(Document.of(wrapper.getText()), options.getEmbeddingOptions());
            if (vectorData != null) {
                wrapper.setVector(vectorData.getVector());
            }
        }

        return searchImplement(wrapper, options);
    }


    protected void embedDocumentsIfNecessary(List<Document> documents, StoreOptions options) {
        if (embeddingModel == null) {
            return;
        }
        for (Document document : documents) {
            if (document.getVector() == null) {
                VectorData vectorData = embeddingModel.embed(document, options.getEmbeddingOptions());
                if (vectorData != null) {
                    document.setVector(vectorData.getVector());
                }
            }
        }
    }


    public abstract StoreResult storeImplement(List<Document> documents, StoreOptions options);

    public abstract StoreResult deleteImplement(Collection<Object> ids, StoreOptions options);

    public abstract StoreResult updateImplement(List<Document> documents, StoreOptions options);

    public abstract List<Document> searchImplement(SearchWrapper wrapper, StoreOptions options);
}
