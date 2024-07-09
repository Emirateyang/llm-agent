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
package com.llmagent;

import java.io.Serializable;

public class PgVectorConfig implements Serializable {
    private String host;
    private Integer port;
    private String databaseName = "llmagent";
    private String username;
    private String password;
    private boolean dropTableIfExist = false;
    private boolean needCreateTable = true;

    private Integer indexListSize = 150;

    private String schemaName = "public";

    private Integer dimension = 1536;

    public PgVectorConfig() {
    }

    public PgVectorConfig(String host, Integer port, String databaseName, String username, String password,
                          boolean dropTableIfExist, boolean needCreateTable, Integer indexListSize, String schemaName, Integer dimension) {
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
        this.dropTableIfExist = dropTableIfExist;
        this.needCreateTable = needCreateTable;
        this.indexListSize = indexListSize;
        this.schemaName = schemaName;
        this.dimension = dimension;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isDropTableIfExist() {
        return dropTableIfExist;
    }

    public void setDropTableIfExist(boolean dropTableIfExist) {
        this.dropTableIfExist = dropTableIfExist;
    }

    public boolean isNeedCreateTable() {
        return needCreateTable;
    }

    public void setNeedCreateTable(boolean needCreateTable) {
        this.needCreateTable = needCreateTable;
    }

    public Integer getIndexListSize() {
        return indexListSize;
    }

    public void setIndexListSize(Integer indexListSize) {
        this.indexListSize = indexListSize;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public Integer getDimension() {
        return dimension;
    }

    public void setDimension(Integer dimension) {
        this.dimension = dimension;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String host;
        private Integer port;
        private String databaseName = "llmagent";
        private String username;
        private String password;
        private boolean dropTableIfExist = false;
        private boolean needCreateTable = true;

        private Integer indexListSize = 150;

        private String schemaName = "public";

        private Integer dimension = 1536;

        public Builder host(String host) {
            this.host = host;
            return this;
        }
        public Builder port(Integer port) {
            this.port = port;
            return this;
        }
        public Builder databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        public Builder password(String password) {
            this.password = password;
            return this;
        }
        public Builder dropTableIfExist(boolean dropTableIfExist) {
            this.dropTableIfExist = dropTableIfExist;
            return this;
        }
        public Builder needCreateTable(boolean needCreateTable) {
            this.needCreateTable = needCreateTable;
            return this;
        }
        public Builder indexListSize(Integer indexListSize) {
            this.indexListSize = indexListSize;
            return this;
        }
        public Builder schemaName(String schemaName) {
            this.schemaName = schemaName;
            return this;
        }

        public Builder dimension(Integer dimension) {
            this.dimension = dimension;
            return this;
        }

        public PgVectorConfig build() {
            return new PgVectorConfig(host, port, databaseName, username, password, dropTableIfExist, needCreateTable,
                    indexListSize, schemaName, dimension);
        }
    }
}
