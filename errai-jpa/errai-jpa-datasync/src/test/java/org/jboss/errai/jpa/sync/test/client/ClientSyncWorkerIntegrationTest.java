/*
 * Copyright 2014 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.jpa.sync.test.client;

import java.util.Collections;
import java.util.Map;

import org.jboss.errai.bus.client.api.BusErrorCallback;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.jpa.sync.client.local.ClientSyncManager;
import org.jboss.errai.jpa.sync.client.local.ClientSyncWorker;
import org.jboss.errai.jpa.sync.client.local.DataSyncCallback;
import org.jboss.errai.jpa.sync.client.shared.SyncResponses;
import org.jboss.errai.jpa.sync.test.client.entity.SimpleEntity;
import org.junit.Test;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

/**
 * Tests for the ClientSyncWorker!
 */
public class ClientSyncWorkerIntegrationTest extends GWTTestCase {

  private final class CountingDataSyncCallback implements DataSyncCallback<SimpleEntity> {
    int callbacksInvoked = 0;

    @Override
    public void onSync(SyncResponses<SimpleEntity> responses) {
      callbacksInvoked++;
    }

    public int getCallbackCount() {
      return callbacksInvoked;
    }
  }

  @SuppressWarnings("rawtypes")
  private final class MockClientSyncManager extends ClientSyncManager {
    private int coldSyncCallCount;
    private RemoteCallback onCompletion;

    @Override
    public void coldSync(String queryName, Class queryResultType, Map queryParams, RemoteCallback onCompletion,  ErrorCallback onError) {
      this.onCompletion = onCompletion;
      coldSyncCallCount++;
    }
    
    public int getColdSyncCallCount() {
      return coldSyncCallCount;
    }
  }

  class CountingErrorCallback extends BusErrorCallback {

    private int errorCount;

    @Override
    public boolean error(Message message, Throwable throwable) {
      errorCount++;
      return true;
    }
    
    public int getErrorCount() {
      return errorCount;
    }
  }
  
  private ClientSyncWorker<SimpleEntity> syncWorker;
  private final CountingErrorCallback countingErrorCallback = new CountingErrorCallback();
  private MockClientSyncManager mockManager;
  
  @Override
  public String getModuleName() {
    return "org.jboss.errai.jpa.sync.test.DataSyncTests";
  }
  
  @Override
  protected void gwtSetUp() throws Exception {
    mockManager = new MockClientSyncManager();
    
    syncWorker =
        new ClientSyncWorker<SimpleEntity>(mockManager, "allSimpleEntities", SimpleEntity.class,
            Collections.<String, Object> emptyMap(), countingErrorCallback);

  }

  @Test
  public void testStartCausesColdSync() {
    delayTestFinish(25000);
    syncWorker.start();
    new Timer() {
      @Override
      public void run() {
        assertTrue(mockManager.getColdSyncCallCount() >= 2);
        finishTest();
      }
    }.schedule(12000);
  }
  
  @Test
  public void testDataSyncCallbackInvoked() {
    delayTestFinish(25000);
    final CountingDataSyncCallback countingSyncCallback = new CountingDataSyncCallback();
    syncWorker.addSyncCallback(countingSyncCallback);
    syncWorker.start();
    new Timer() {
      @Override
      public void run() {
        assertTrue(mockManager.getColdSyncCallCount() >= 1);
        finishTest();
      }
    }.schedule(7000);
    
  }
  
  @Test
  public void testStop() {
    delayTestFinish(25000);
    final CountingDataSyncCallback countingSyncCallback = new CountingDataSyncCallback();
    syncWorker.addSyncCallback(countingSyncCallback);
    syncWorker.start();
    new Timer() {
      @Override
      public void run() {
        assertEquals(0, mockManager.getColdSyncCallCount());
        finishTest();
      }
    }.schedule(7000);
    syncWorker.stop();
  }
}
