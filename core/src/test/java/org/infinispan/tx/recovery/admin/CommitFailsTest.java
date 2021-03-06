/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.infinispan.tx.recovery.admin;

import org.infinispan.Cache;
import org.infinispan.affinity.KeyAffinityService;
import org.infinispan.affinity.KeyAffinityServiceFactory;
import org.infinispan.affinity.RndKeyGenerator;
import org.infinispan.config.Configuration;
import org.infinispan.interceptors.InvocationContextInterceptor;
import org.infinispan.transaction.tm.DummyTransaction;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.transaction.xa.XAException;
import java.util.List;
import java.util.concurrent.Executors;

import static org.infinispan.tx.recovery.RecoveryTestUtil.commitTransaction;
import static org.infinispan.tx.recovery.RecoveryTestUtil.prepareTransaction;
import static org.testng.Assert.assertEquals;

/**
 * This test makes sure that when a tx fails during commit it can still be completed.
 *
 * @author Mircea Markus
 * @since 5.0
 */
@Test (groups = "functional", testName = "tx.recovery.admin.CommitFailsTest")
public class CommitFailsTest extends AbstractRecoveryTest {

   private Object key;
   private InDoubtWithCommitFailsTest.ForceFailureInterceptor failureInterceptor1;
   private InDoubtWithCommitFailsTest.ForceFailureInterceptor failureInterceptor2;

   @Override
   protected void createCacheManagers() throws Throwable {
      Configuration configuration = defaultRecoveryConfig();
      createCluster(configuration, 3);
      waitForClusterToForm();

      key = getKey();

      failureInterceptor1 = new InDoubtWithCommitFailsTest.ForceFailureInterceptor();
      failureInterceptor2 = new InDoubtWithCommitFailsTest.ForceFailureInterceptor();
      advancedCache(0).addInterceptorAfter(failureInterceptor1, InvocationContextInterceptor.class);
      advancedCache(1).addInterceptorAfter(failureInterceptor2, InvocationContextInterceptor.class);
   }


   @BeforeMethod
   protected void setUpTx() throws Exception {
      failureInterceptor1.fail = true;
      failureInterceptor2.fail = true;

      tm(2).begin();
      cache(2).put(this.key, "newValue");
      DummyTransaction tx = (DummyTransaction) tm(2).suspend();
      prepareTransaction(tx);
      try {
         commitTransaction(tx);
         assert false;
      } catch (XAException e) {
         //expected
      }

      assertEquals(countInDoubtTx(recoveryOps(2).showInDoubtTransactions()), 1);
      log.trace("here is the remote get...");
      assertEquals(countInDoubtTx(recoveryOps(0).showInDoubtTransactions()), 1);
      assertEquals(countInDoubtTx(recoveryOps(1).showInDoubtTransactions()), 1);

      failureInterceptor1.fail = false;
      failureInterceptor2.fail = false;

   }

   protected Object getKey() {
      KeyAffinityService kaf = KeyAffinityServiceFactory
            .newKeyAffinityService(this.cache(2), Executors.newSingleThreadExecutor(), new RndKeyGenerator(), 1000);
      Object key = kaf.getKeyForAddress(address(2));
      kaf.stop();
      return key;
   }

   public void testForceCommitOnOriginator() {
      runTest(2);
   }

   public void testForceCommitNonTxParticipant() {
      int where = getTxParticipant(false);
      runTest(where);
   }

   public void testForceCommitTxParticipant() {
      int where = getTxParticipant(true);
      runTest(where);
   }

   private void assertAllHaveValue(Object key, String newValue) {
      for (Cache c : caches()) assertEquals(c.get(key), newValue);
   }


   protected void runTest(int where) {
      List<Long> internalIds = getInternalIds(recoveryOps(where).showInDoubtTransactions());
      log.info("About to force commit!");
      recoveryOps(where).forceCommit(internalIds.get(0));
      assertAllHaveValue(key, "newValue");
      assertCleanup(0, 1, 2);
   }

   protected int getTxParticipant(boolean txParticipant) {
      int expectedNumber = txParticipant ? 1 : 0;

      int index = -1;
      for (int i = 0; i < 2; i++) {
         if (tt(i).getRemoteTxCount() == expectedNumber) {
            index = i;
            break;
         }
      }
      return index;
   }
}
