/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009 Red Hat Inc. and/or its affiliates and other
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
package org.infinispan;

import org.infinispan.batch.BatchContainer;
import org.infinispan.container.DataContainer;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContextContainer;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.eviction.EvictionManager;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.stats.Stats;
import org.infinispan.util.concurrent.locks.LockManager;

import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.util.Collection;
import java.util.List;

/**
 * An advanced interface that exposes additional methods not available on {@link Cache}.
 *
 * @author Manik Surtani
 * @since 4.0
 */
public interface AdvancedCache<K, V> extends Cache<K, V> {

   /**
    * A builder-style method that adds flags to any API call.  For example, consider the following code snippet:
    * <pre>
    *   cache.withFlags(Flag.FORCE_WRITE_LOCK).get(key);
    * </pre>
    * will invoke a cache.get() with a write lock forced.
    * @param flags a set of flags to apply.  See the {@link Flag} documentation.
    * @return a cache on which a real operation is to be invoked.
    */
   AdvancedCache<K, V> withFlags(Flag... flags);

   /**
    * Adds a custom interceptor to the interceptor chain, at specified position, where the first interceptor in the
    * chain is at position 0 and the last one at NUM_INTERCEPTORS - 1.
    *
    * @param i        the interceptor to add
    * @param position the position to add the interceptor
    */
   void addInterceptor(CommandInterceptor i, int position);

   /**
    * Adds a custom interceptor to the interceptor chain, after an instance of the specified interceptor type. Throws a
    * cache exception if it cannot find an interceptor of the specified type.
    *
    * @param i                interceptor to add
    * @param afterInterceptor interceptor type after which to place custom interceptor
    */
   void addInterceptorAfter(CommandInterceptor i, Class<? extends CommandInterceptor> afterInterceptor);

   /**
    * Adds a custom interceptor to the interceptor chain, before an instance of the specified interceptor type. Throws a
    * cache exception if it cannot find an interceptor of the specified type.
    *
    * @param i                 interceptor to add
    * @param beforeInterceptor interceptor type before which to place custom interceptor
    */
   void addInterceptorBefore(CommandInterceptor i, Class<? extends CommandInterceptor> beforeInterceptor);

   /**
    * Removes the interceptor at a specified position, where the first interceptor in the chain is at position 0 and the
    * last one at getInterceptorChain().size() - 1.
    *
    * @param position the position at which to remove an interceptor
    */
   void removeInterceptor(int position);

   /**
    * Removes the interceptor of specified type.
    *
    * @param interceptorType type of interceptor to remove
    */
   void removeInterceptor(Class<? extends CommandInterceptor> interceptorType);

   /**
    * Retrieves the current Interceptor chain.
    *
    * @return an immutable {@link java.util.List} of {@link org.infinispan.interceptors.base.CommandInterceptor}s
    *         configured for this cache
    */
   List<CommandInterceptor> getInterceptorChain();

   /**
    * @return the eviction manager - if one is configured - for this cache instance
    */
   EvictionManager getEvictionManager();

   /**
    * @return the component registry for this cache instance
    */
   ComponentRegistry getComponentRegistry();

   /**
    * Retrieves a reference to the {@link org.infinispan.distribution.DistributionManager} if the cache is configured
    * to use Distribution.  Otherwise, returns a null.
    * @return a DistributionManager, or null.
    */
   DistributionManager getDistributionManager();

   /**
    * Locks a given key or keys eagerly across cache nodes in a cluster.
    * <p>
    * Keys can be locked eagerly in the context of a transaction only
    *
    * @param keys the keys to lock
    * @return true if the lock acquisition attempt was successful for <i>all</i> keys; false otherwise.
    */   
   boolean lock(K... keys);

   /**
    * Locks collections of keys eagerly across cache nodes in a cluster.
    * <p>
    * Collections of keys can be locked eagerly in the context of a transaction only
    * 
    * 
    * @param keys collection of keys to lock
    * @return true if the lock acquisition attempt was successful for <i>all</i> keys; false otherwise. 
    */
   boolean lock(Collection<? extends K> keys);

   /**
    * Returns the component in charge of communication with other caches in
    * the cluster.  If the cache's {@link org.infinispan.config.Configuration.CacheMode}
    * is {@link org.infinispan.config.Configuration.CacheMode#LOCAL}, this
    * method will return null.
    *
    * @return the RPC manager component associated with this cache instance or null
    */
   RpcManager getRpcManager();

   /**
    * Returns the component in charge of batching cache operations.
    *
    * @return the batching component associated with this cache instance
    */
   BatchContainer getBatchContainer();

   /**
    * Returns the component in charge of managing the interactions between the
    * cache operations and the context information associated with them.
    *
    * @return the invocation context container component
    */
   InvocationContextContainer getInvocationContextContainer();

   /**
    * Returns the container where data is stored in the cache. Users should
    * interact with this component with care because direct calls on it bypass
    * the internal interceptors and other infrastructure in place to guarantee
    * the consistency of data.
    *
    * @return the data container associated with this cache instance
    */
   DataContainer getDataContainer();

   /**
    * Returns the transaction manager configured for this cache. If no
    * transaction manager was configured, this method returns null.
    *
    * @return the transaction manager associated with this cache instance or null
    */
   TransactionManager getTransactionManager();

   /**
    * Returns the component that deals with all aspects of acquiring and
    * releasing locks for cache entries.
    *
    * @return retrieves the lock manager associated with this cache instance
    */
   LockManager getLockManager();

   /**
    * Returns a {@link Stats} object that allows several statistics associated
    * with this cache at runtime.
    *
    * @return this cache's {@link Stats} object
    */
   Stats getStats();

   /**
    * Returns the {@link XAResource} associated with this cache which can be
    * used to do transactional recovery.
    *
    * @return an instance of {@link XAResource}
    */
   XAResource getXAResource();
   
   /**
    * Returns the cache loader associated associated with this cache.
    *
    * @return this cache's cache loader
    */
   ClassLoader getClassLoader();
   
   /**
    * Using this operation, users can call any {@link AdvancedCache} operation
    * with a given class loader. This means that any class loading happening
    * as a result of the cache operation will be done using the class loader
    * given. For example: </p>
    *
    * When users store POJO instances in caches configured with {@link org.infinispan.config.Configuration#storeAsBinary},
    * these instances are transformed into byte arrays. When these entries are
    * read from the cache, a lazy unmarshalling process happens where these byte
    * arrays are transformed back into POJO instances. Using {@link AdvancedCache#with(ClassLoader)}
    * when reading that enables users to provide the class loader that should
    * be used when trying to locate the classes that are constructed as a result
    * of the unmarshalling process.
    *
    * @return an advanced cache instance upon which operations can be called
    * with a particular cache loader.
    */
   AdvancedCache<K, V> with(ClassLoader classLoader);
}
