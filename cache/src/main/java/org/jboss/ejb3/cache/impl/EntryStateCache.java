/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.ejb3.cache.impl;

import org.jboss.ejb3.cache.Cache;
import org.jboss.ejb3.cache.Identifiable;
import org.jboss.ejb3.cache.StatefulObjectFactory;

import javax.ejb.NoSuchEJBException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision$
 */
public class EntryStateCache<T extends Identifiable> implements Cache<T>
{
   private StatefulObjectFactory<T> factory;
   private final Map<Serializable, Entry> cache;
   
   private static enum State { READY, IN_USE };
   
   private class Entry
   {
      long lastUsed;
      T obj;
      State state;
      
      Entry(T obj)
      {
         assert obj != null : "obj is null";
         
         this.lastUsed = System.currentTimeMillis();
         this.obj = obj;
         this.state = State.IN_USE;
      }
   }
   
   public EntryStateCache()
   {
      this.cache = new HashMap<Serializable, Entry>();
   }
   
   @Override
   public T create()
   {
      T obj = factory.createInstance();
      Entry entry = new Entry(obj);
      synchronized (cache)
      {
         cache.put(obj.getId(), entry);
      }
      return obj;
   }

   @Override
   public void discard(Serializable key)
   {
      remove(key);
   }
   
   @Override
   public T get(Serializable key) throws NoSuchEJBException
   {
      synchronized (cache)
      {
         Entry entry = cache.get(key);
         if(entry == null)
            throw new NoSuchEJBException(String.valueOf(key));
         if(entry.state != State.READY)
            throw new IllegalStateException("entry " + entry + " is not ready");
         entry.state = State.IN_USE;
         entry.lastUsed = System.currentTimeMillis();
         return entry.obj;
      }
   }

   public T peek(Serializable key) throws NoSuchEJBException
   {
      synchronized (cache)
      {
         Entry entry = cache.get(key);
         if(entry == null)
            throw new NoSuchEJBException(String.valueOf(key));
         return entry.obj;
      }
   }

   @Override
   public void release(T obj)
   {
      synchronized (cache)
      {
         Entry entry = cache.get(obj.getId());
         if(entry.state != State.IN_USE)
            throw new IllegalStateException("entry " + entry + " is not in use");
         entry.state = State.READY;
         entry.lastUsed = System.currentTimeMillis();
      }
   }

   @Override
   public void remove(Serializable key)
   {
      Entry entry;
      synchronized (cache)
      {
         entry = cache.remove(key);
         if(entry.state != State.READY)
            throw new IllegalStateException("entry " + entry + " is not ready");
      }
      if(entry == null)
         throw new NoSuchEJBException(String.valueOf(key));
      factory.destroyInstance(entry.obj);
   }

   @Override
   public void setStatefulObjectFactory(StatefulObjectFactory<T> factory)
   {
      assert factory != null : "factory is null";
      this.factory = factory;
   }

   @Override
   public void start()
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void stop()
   {
      // TODO Auto-generated method stub

   }
}
