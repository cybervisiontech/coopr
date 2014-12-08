/*
 * Copyright © 2012-2014 Cask Data, Inc.
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
package co.cask.coopr.store.entity;

import co.cask.coopr.account.Account;
import co.cask.coopr.store.DBConnectionPool;
import com.google.gson.Gson;

import java.io.IOException;

/**
 * Implementation of {@link co.cask.coopr.store.entity.BaseEntityStoreView} from the view of a tenant user.
 */
public class SQLUserEntityStoreView extends BaseSQLEntityStoreView {

  SQLUserEntityStoreView(Account account, DBConnectionPool dbConnectionPool, Gson gson) {
    super(account, dbConnectionPool, gson);
  }

  @Override
  protected void writeEntity(EntityType entityType, String entityName, byte[] data)
    throws IOException, IllegalAccessException {
    throw new IllegalAccessException("Non-admins are not allowed to write entities.");
  }

  @Override
  protected void writeEntity(EntityType entityType, String entityName, int version, byte[] data)
    throws IOException, IllegalAccessException {
    throw new IllegalAccessException("Non-admins are not allowed to write entities.");
  }


  @Override
  protected void deleteEntity(EntityType entityType, String entityName)
    throws IOException, IllegalAccessException {
    throw new IllegalAccessException("Non-admins are not allowed to delete entities.");
  }

  @Override
  protected void deleteEntity(EntityType entityType, String entityName, int entityVersion)
    throws IOException, IllegalAccessException {
    throw new IllegalAccessException("Non-admins are not allowed to delete entities.");
  }
}
