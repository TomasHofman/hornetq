/*
 * JBoss, Home of Professional Open Source Copyright 2008, Red Hat Middleware
 * LLC, and individual contributors by the @authors tag. See the copyright.txt
 * in the distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.jboss.messaging.core.client.management.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.jboss.messaging.core.message.Message;
import org.jboss.messaging.util.SimpleString;

/*
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 * 
 * @version <tt>$Revision$</tt>
 */
public class ManagementHelper
{

   // Constants -----------------------------------------------------

   public static final SimpleString HDR_JMX_OBJECTNAME = new SimpleString("JBMJMXObjectName");

   public static final SimpleString HDR_JMX_ATTRIBUTE_PREFIX = new SimpleString("JBMJMXAttribute.");

   public static final SimpleString HDR_JMX_OPERATION_PREFIX = new SimpleString("JBMJMXOperation.");

   public static final SimpleString HDR_JMX_OPERATION_NAME = new SimpleString(HDR_JMX_OPERATION_PREFIX + "name");

   public static final SimpleString HDR_JMX_OPERATION_SUCCEEDED = new SimpleString("JBMJMXOperationSucceeded");

   public static final SimpleString HDR_JMX_OPERATION_EXCEPTION = new SimpleString("JBMJMXOperationException");

   public static final SimpleString HDR_NOTIFICATION_TYPE = new SimpleString("JBMNotifType");

   public static final SimpleString HDR_NOTIFICATION_MESSAGE = new SimpleString("JBMNotifMessage");

   public static final SimpleString HDR_NOTIFICATION_TIMESTAMP = new SimpleString("JBMNotifTimestamp");

   // Attributes ----------------------------------------------------

   // Static --------------------------------------------------------

   public static void putAttributes(final Message message,
                                    final ObjectName objectName,
                                    final String... attributes)
   {
      message.putStringProperty(HDR_JMX_OBJECTNAME, new SimpleString(objectName.toString()));
      for (int i = 0; i < attributes.length; i++)
      {
         message.putStringProperty(new SimpleString(HDR_JMX_ATTRIBUTE_PREFIX + Integer.toString(i)),
                                   new SimpleString(attributes[i]));
      }
   }

   public static void putOperationInvocation(final Message message,
                                             final ObjectName objectName,
                                             final String operationName,
                                             final Object... parameters)
   {
      // store the name of the operation...
      message.putStringProperty(HDR_JMX_OBJECTNAME, new SimpleString(objectName.toString()));
      message.putStringProperty(HDR_JMX_OPERATION_NAME, new SimpleString(operationName));
      // ... and all the parameters (preserving their types)
      for (int i = 0; i < parameters.length; i++)
      {
         Object parameter = parameters[i];
         SimpleString key = new SimpleString(String.format("%s%02d", HDR_JMX_OPERATION_PREFIX, i));
         storeTypedProperty(message, key, parameter);
      }
   }

   public static boolean isOperationResult(final Message message)
   {
      return message.containsProperty(HDR_JMX_OPERATION_SUCCEEDED);
   }

   public static boolean isAttributesResult(final Message message)
   {
      return !(isOperationResult(message));
   }

   public static TabularData getTabularDataProperty(final Message message, final String key)
   {
      Object object = message.getProperty(new SimpleString(key));
      if (object instanceof byte[])
      {
         return (TabularData)from((byte[])object);
      }
      throw new IllegalArgumentException(key + " property is not a valid TabularData");
   }
   
   public static CompositeData getCompositeDataProperty(final Message message, final String key)
   {
      Object object = message.getProperty(new SimpleString(key));
      if (object instanceof byte[])
      {
         return (CompositeData)from((byte[])object);
      }
      throw new IllegalArgumentException(key + " property is not a valid CompositeData");
   }

   public static boolean hasOperationSucceeded(final Message message)
   {
      if (!isOperationResult(message))
      {
         return false;
      }
      if (message.containsProperty(HDR_JMX_OPERATION_SUCCEEDED))
      {
         return (Boolean)message.getProperty(HDR_JMX_OPERATION_SUCCEEDED);
      }
      return false;
   }

   public static String getOperationExceptionMessage(final Message message)
   {
      if (message.containsProperty(HDR_JMX_OPERATION_EXCEPTION))
      {
         return ((SimpleString)message.getProperty(HDR_JMX_OPERATION_EXCEPTION)).toString();
      }
      return null;
   }

   public static void storeTypedProperty(final Message message, final SimpleString key, final Object typedProperty)
   {
      if (typedProperty instanceof Void)
      {
         // do not put the returned value if the operation was a procedure
      }
      else if (typedProperty instanceof Boolean)
      {
         message.putBooleanProperty(key, (Boolean)typedProperty);
      }
      else if (typedProperty instanceof Byte)
      {
         message.putByteProperty(key, (Byte)typedProperty);
      }
      else if (typedProperty instanceof Short)
      {
         message.putShortProperty(key, (Short)typedProperty);
      }
      else if (typedProperty instanceof Integer)
      {
         message.putIntProperty(key, (Integer)typedProperty);
      }
      else if (typedProperty instanceof Long)
      {
         message.putLongProperty(key, (Long)typedProperty);
      }
      else if (typedProperty instanceof Float)
      {
         message.putFloatProperty(key, (Float)typedProperty);
      }
      else if (typedProperty instanceof Double)
      {
         message.putDoubleProperty(key, (Double)typedProperty);
      }
      else if (typedProperty instanceof String)
      {
         message.putStringProperty(key, new SimpleString((String)typedProperty));
      }
      else if (typedProperty instanceof TabularData || typedProperty instanceof CompositeData)
      {
         storePropertyAsBytes(message, key, typedProperty);
      }
      // serialize as a SimpleString
      else
      {
         message.putStringProperty(key, new SimpleString("" + typedProperty));
      }
   }

   public static Object from(final byte[] bytes)
   {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      ObjectInputStream ois;
      try
      {
         ois = new ObjectInputStream(bais);
         return ois.readObject();
      }
      catch (Exception e)
      {
         throw new IllegalStateException(e);
      }
   }

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   private static void storePropertyAsBytes(final Message message, final SimpleString key, final Object property)
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try
      {
         ObjectOutputStream oos = new ObjectOutputStream(baos);
         oos.writeObject(property);
      }
      catch (IOException e)
      {
         throw new IllegalStateException(property + " can not be written to a byte array");
      }
      message.putBytesProperty(key, baos.toByteArray());
   }
   // Inner classes -------------------------------------------------
}
