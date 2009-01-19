/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005-2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.messaging.tests.stress.journal;

import java.io.File;
import java.util.ArrayList;

import org.jboss.messaging.core.journal.LoadManager;
import org.jboss.messaging.core.journal.PreparedTransactionInfo;
import org.jboss.messaging.core.journal.RecordInfo;
import org.jboss.messaging.core.journal.SequentialFileFactory;
import org.jboss.messaging.core.journal.impl.AIOSequentialFileFactory;
import org.jboss.messaging.core.journal.impl.JournalImpl;
import org.jboss.messaging.core.journal.impl.NIOSequentialFileFactory;
import org.jboss.messaging.tests.unit.core.journal.impl.fakes.SimpleEncoding;
import org.jboss.messaging.tests.util.UnitTestCase;

/**
 * 
 * @author <a href="mailto:clebert.suconic@jboss.com">Clebert Suconic</a>
 *
 */
public class AddAndRemoveStressTest extends UnitTestCase
{

   // Constants -----------------------------------------------------

   private static final LoadManager dummyLoader = new LoadManager()
   {

      public void addPreparedTransaction(final PreparedTransactionInfo preparedTransaction)
      {
      }

      public void addRecord(final RecordInfo info)
      {
      }

      public void deleteRecord(final long id)
      {
      }

      public void updateRecord(final RecordInfo info)
      {
      }
   };

   private static final long NUMBER_OF_MESSAGES = 210000l;

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public void testFoo() throws Exception
   {
      
      final int numberOfInserts = 50000;
      
     // final int numberOfAddRefs = 10;
      
//      for (long i = 1; i <= 10000; i++)
//      {
//         if (i % 10000 == 0)
//         {
//            System.out.println("Append " + i);
//         }
//         impl.appendAddRecord(i, (byte)12, encoding);
//         
//         for (int j = 0; j < numberOfAddRefs; j++)
//         {
//            impl.appendUpdateRecord(i, (byte)12, updateEncoding);
//         }
//      }
//      
      System.out.println("Starting");
      
//      for (int numberOfAddRefs = 0; numberOfAddRefs < 10; numberOfAddRefs++)
//      {  
      final int numberOfAddRefs = 1;
      while (true)
      {
         File file = new File(getTestDir());
         deleteDirectory(file);
         file.mkdirs();
         
         SequentialFileFactory factory = new AIOSequentialFileFactory(getTestDir());
         JournalImpl impl = new JournalImpl(10 * 1024 * 1024, 10, true, false, factory, "jbm", "jbm", 1000, 1000);

         impl.start();

         impl.load(dummyLoader);
         
         System.out.println("Loaded");
         
         SimpleEncoding encoding = new SimpleEncoding(1024, (byte)'f');
         
         SimpleEncoding updateEncoding = new SimpleEncoding(8, (byte)'f');
              
         
         long start = System.currentTimeMillis();
         
         for (long i = 1; i <= numberOfInserts; i++)
         {
            if (i % 10000 == 0)
            {
               System.out.println("Append " + i);
            }
            impl.appendAddRecord(i, (byte)12, encoding);
            
            for (int j = 0; j < numberOfAddRefs; j++)
            {
               impl.appendUpdateRecord(i, (byte)12, updateEncoding);
            }
         }
         
         long end = System.currentTimeMillis();
         
         double rate = 1000 * ((double)numberOfInserts) / (end - start);
   
         System.out.println("rate " + rate);
   
         impl.stop();
      
      }

     

   }
   
   
   public void testInsertAndLoad() throws Exception
   {

      SequentialFileFactory factory = new AIOSequentialFileFactory(getTestDir());
      JournalImpl impl = new JournalImpl(10 * 1024 * 1024, 60, true, false, factory, "jbm", "jbm", 1000, 0);

      impl.start();

      impl.load(dummyLoader);

      for (long i = 1; i <= NUMBER_OF_MESSAGES; i++)
      {
         if (i % 10000 == 0)
         {
            System.out.println("Append " + i);
         }
         impl.appendAddRecord(i, (byte)0, new SimpleEncoding(1024, (byte)'f'));
      }

      impl.stop();

      factory = new AIOSequentialFileFactory(getTestDir());
      impl = new JournalImpl(10 * 1024 * 1024, 60, true, false, factory, "jbm", "jbm", 1000, 0);

      impl.start();

      impl.load(dummyLoader);

      for (long i = 1; i <= NUMBER_OF_MESSAGES; i++)
      {
         if (i % 10000 == 0)
         {
            System.out.println("Delete " + i);
         }

         impl.appendDeleteRecord(i);
      }

      impl.stop();

      factory = new AIOSequentialFileFactory(getTestDir());
      impl = new JournalImpl(10 * 1024 * 1024, 60, true, false, factory, "jbm", "jbm", 1000, 0);

      impl.start();

      ArrayList<RecordInfo> info = new ArrayList<RecordInfo>();
      ArrayList<PreparedTransactionInfo> trans = new ArrayList<PreparedTransactionInfo>();

      impl.load(info, trans);

      if (info.size() > 0)
      {
         System.out.println("Info ID: " + info.get(0).id);
      }

      assertEquals(0, info.size());
      assertEquals(0, trans.size());

   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   @Override
   protected void setUp()
   {
      File file = new File(getTestDir());
      deleteDirectory(file);
      file.mkdirs();
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
   }

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
