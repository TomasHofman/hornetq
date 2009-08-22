/*
 * Copyright 2009 Red Hat, Inc.
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.hornetq.tests.unit.core.journal.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hornetq.core.asyncio.impl.AsynchronousFileImpl;
import org.hornetq.core.journal.SequentialFile;
import org.hornetq.core.journal.SequentialFileFactory;
import org.hornetq.core.logging.Logger;
import org.hornetq.tests.util.UnitTestCase;

/**
 * 
 * A SequentialFileFactoryTestBase
 * 
 * @author <a href="mailto:tim.fox@jboss.com">Tim Fox</a>
 *
 */
public abstract class SequentialFileFactoryTestBase extends UnitTestCase
{
   protected final Logger log = Logger.getLogger(this.getClass());

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      factory = createFactory();
   }

   @Override
   protected void tearDown() throws Exception
   {
      assertEquals(0, AsynchronousFileImpl.getTotalMaxIO());
      
      factory = null;
      
      forceGC();
      
      super.tearDown();
   }

   protected abstract SequentialFileFactory createFactory();

   protected SequentialFileFactory factory;

   public void testCreateAndListFiles() throws Exception
   {
      List<String> expectedFiles = new ArrayList<String>();

      final int numFiles = 10;

      for (int i = 0; i < numFiles; i++)
      {
         String fileName = UUID.randomUUID().toString() + ".hq";

         expectedFiles.add(fileName);

         SequentialFile sf = factory.createSequentialFile(fileName, 1);

         sf.open();

         assertEquals(fileName, sf.getFileName());

         sf.close();
      }

      // Create a couple with a different extension - they shouldn't be picked
      // up

      SequentialFile sf1 = factory.createSequentialFile("different.file", 1);
      sf1.open();

      SequentialFile sf2 = factory.createSequentialFile("different.cheese", 1);
      sf2.open();

      List<String> fileNames = factory.listFiles("hq");

      assertEquals(expectedFiles.size(), fileNames.size());

      for (String fileName : expectedFiles)
      {
         assertTrue(fileNames.contains(fileName));
      }

      fileNames = factory.listFiles("file");

      assertEquals(1, fileNames.size());

      assertTrue(fileNames.contains("different.file"));

      fileNames = factory.listFiles("cheese");

      assertEquals(1, fileNames.size());

      assertTrue(fileNames.contains("different.cheese"));

      sf1.close();
      sf2.close();
   }

   public void testFill() throws Exception
   {
      SequentialFile sf = factory.createSequentialFile("fill.hq", 1);

      sf.open();

      try
      {

         checkFill(sf, 0, 2048, (byte)'X');

         checkFill(sf, 512, 512, (byte)'Y');

         checkFill(sf, 0, 1, (byte)'Z');

         checkFill(sf, 512, 1, (byte)'A');

         checkFill(sf, 1024, 512 * 4, (byte)'B');
      }
      finally
      {
         sf.close();
      }
   }

   public void testDelete() throws Exception
   {
      SequentialFile sf = factory.createSequentialFile("delete-me.hq", 1);

      sf.open();

      SequentialFile sf2 = factory.createSequentialFile("delete-me2.hq", 1);

      sf2.open();

      List<String> fileNames = factory.listFiles("hq");

      assertEquals(2, fileNames.size());

      assertTrue(fileNames.contains("delete-me.hq"));

      assertTrue(fileNames.contains("delete-me2.hq"));

      sf.delete();

      fileNames = factory.listFiles("hq");

      assertEquals(1, fileNames.size());

      assertTrue(fileNames.contains("delete-me2.hq"));

      sf2.close();

   }
   
   public void testRename() throws Exception
   {
      SequentialFile sf = factory.createSequentialFile("test1.hq", 1);

      sf.open();

      List<String> fileNames = factory.listFiles("hq");

      assertEquals(1, fileNames.size());

      assertTrue(fileNames.contains("test1.hq"));
      
      sf.renameTo("test1.cmp");

      fileNames = factory.listFiles("cmp");

      assertEquals(1, fileNames.size());

      assertTrue(fileNames.contains("test1.cmp"));

      sf.delete();

      fileNames = factory.listFiles("hq");

      assertEquals(0, fileNames.size());

      fileNames = factory.listFiles("cmp");

      assertEquals(0, fileNames.size());

   }
   
   public void testWriteandRead() throws Exception
   {
      SequentialFile sf = factory.createSequentialFile("write.hq", 1);

      sf.open();

      String s1 = "aardvark";
      byte[] bytes1 = s1.getBytes("UTF-8");
      ByteBuffer bb1 = factory.wrapBuffer(bytes1);

      String s2 = "hippopotamus";
      byte[] bytes2 = s2.getBytes("UTF-8");
      ByteBuffer bb2 = factory.wrapBuffer(bytes2);

      String s3 = "echidna";
      byte[] bytes3 = s3.getBytes("UTF-8");
      ByteBuffer bb3 = factory.wrapBuffer(bytes3);
      
      long initialPos = sf.position();
      sf.write(bb1, true);
      long bytesWritten = sf.position() - initialPos;

      assertEquals(calculateRecordSize(bytes1.length, sf.getAlignment()), bytesWritten);

      initialPos = sf.position();
      sf.write(bb2, true);
      bytesWritten = sf.position() - initialPos;

      assertEquals(calculateRecordSize(bytes2.length, sf.getAlignment()), bytesWritten);

      initialPos = sf.position();
      sf.write(bb3, true);
      bytesWritten = sf.position() - initialPos;

      assertEquals(calculateRecordSize(bytes3.length, sf.getAlignment()), bytesWritten);

      sf.position(0);

      ByteBuffer rb1 = factory.newBuffer(bytes1.length);
      ByteBuffer rb2 = factory.newBuffer(bytes2.length);
      ByteBuffer rb3 = factory.newBuffer(bytes3.length);

      int bytesRead = sf.read(rb1);
      assertEquals(calculateRecordSize(bytes1.length, sf.getAlignment()), bytesRead);

      for (int i = 0; i < bytes1.length; i++)
      {
         assertEquals(bytes1[i], rb1.get(i));
      }

      bytesRead = sf.read(rb2);
      assertEquals(calculateRecordSize(bytes2.length, sf.getAlignment()), bytesRead);
      for (int i = 0; i < bytes2.length; i++)
      {
         assertEquals(bytes2[i], rb2.get(i));
      }

      bytesRead = sf.read(rb3);
      assertEquals(calculateRecordSize(bytes3.length, sf.getAlignment()), bytesRead);
      for (int i = 0; i < bytes3.length; i++)
      {
         assertEquals(bytes3[i], rb3.get(i));
      }

      sf.close();

   }

   public void testPosition() throws Exception
   {
      SequentialFile sf = factory.createSequentialFile("position.hq", 1);

      sf.open();

      try
      {

         sf.fill(0, 3 * 512, (byte)0);

         String s1 = "orange";
         byte[] bytes1 = s1.getBytes("UTF-8");
         ByteBuffer bb1 = factory.wrapBuffer(bytes1);

         byte[] bytes2 = s1.getBytes("UTF-8");
         ByteBuffer bb2 = factory.wrapBuffer(bytes2);

         String s3 = "lemon";
         byte[] bytes3 = s3.getBytes("UTF-8");
         ByteBuffer bb3 = factory.wrapBuffer(bytes3);

         long initialPos = sf.position();
         sf.write(bb1, true);
         long bytesWritten = sf.position() - initialPos;

         assertEquals(bb1.limit(), bytesWritten);

         initialPos = sf.position();
         sf.write(bb2, true);
         bytesWritten = sf.position() - initialPos;

         
         assertEquals(bb2.limit(), bytesWritten);

         initialPos = sf.position();
         sf.write(bb3, true);
         bytesWritten = sf.position() - initialPos;

         assertEquals(bb3.limit(), bytesWritten);

         byte[] rbytes1 = new byte[bytes1.length];

         byte[] rbytes2 = new byte[bytes2.length];

         byte[] rbytes3 = new byte[bytes3.length];

         ByteBuffer rb1 = factory.newBuffer(rbytes1.length);
         ByteBuffer rb2 = factory.newBuffer(rbytes2.length);
         ByteBuffer rb3 = factory.newBuffer(rbytes3.length);

         sf.position(bb1.limit() + bb2.limit());

         int bytesRead = sf.read(rb3);
         assertEquals(rb3.limit(), bytesRead);
         rb3.rewind();
         rb3.get(rbytes3);
         assertEqualsByteArrays(bytes3, rbytes3);

         sf.position(rb1.limit());

         bytesRead = sf.read(rb2);
         assertEquals(rb2.limit(), bytesRead);
         rb2.get(rbytes2);
         assertEqualsByteArrays(bytes2, rbytes2);

         sf.position(0);

         bytesRead = sf.read(rb1);
         assertEquals(rb1.limit(), bytesRead);
         rb1.get(rbytes1);

         assertEqualsByteArrays(bytes1, rbytes1);

      }
      finally
      {
         try
         {
            sf.close();
         }
         catch (Exception ignored)
         {
         }
      }
   }

   public void testOpenClose() throws Exception
   {
      SequentialFile sf = factory.createSequentialFile("openclose.hq", 1);

      sf.open();

      sf.fill(0, 512, (byte)0);

      String s1 = "cheesecake";
      byte[] bytes1 = s1.getBytes("UTF-8");
      ByteBuffer bb1 = factory.wrapBuffer(bytes1);

      long initialPos = sf.position();
      sf.write(bb1, true);
      long bytesWritten = sf.position() - initialPos;

      assertEquals(bb1.limit(), bytesWritten);

      sf.close();

      try
      {
         
         bb1 = factory.wrapBuffer(bytes1);
         
         sf.write(bb1, true);

         fail("Should throw exception");
      }
      catch (Exception e)
      {
         // OK
      }

      sf.open();

      sf.write(bb1, true);

      sf.close();
   }

   // Private ---------------------------------

   protected void checkFill(final SequentialFile file, final int pos, final int size, final byte fillChar) throws Exception
   {
      file.fill(pos, size, fillChar);

      file.close();

      file.open();

      file.position(pos);

      ByteBuffer bb = factory.newBuffer(size);

      int bytesRead = file.read(bb);

      assertEquals(calculateRecordSize(size, file.getAlignment()), bytesRead);

      for (int i = 0; i < size; i++)
      {
         // log.debug(" i is " + i);
         assertEquals(fillChar, bb.get(i));
      }

   }

}
