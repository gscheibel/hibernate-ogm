/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.logging.polyglot.impl;

import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.polyglot.impl.configuration.Environment;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@Message(id = 1301, value = "Cannot retrieve the Entity Datastore provider from the class name [%s]")
	HibernateException unableToRetrieveEntityDatastoreProviderClass(String className);

	@Message(id = 1302, value = "The class [%s] does not implement the right interfaces")
	HibernateException doesNotImplementInterface(@Cause Exception e, String className);

	@Message(id = 1303, value = "Cannot instanciate the entity datastore provider from the class [%s]")
	HibernateException unableToInstanciateEntityDatastoreProvider(@Cause Exception cause, String className);

	@Message(id = 1304, value = "Cannot instanciate the entity dialect from the class [%s]")
	HibernateException unableToInstanciateEntityDialect(@Cause Exception cause, String dialectClassName);

	@Message(id = 1305, value = "Cannot find the entity test helper for [%s]")
	HibernateException cannotFindEntityTestHelperClassName(String datastoreProviderName);

	@Message(id = 1306, value = "Cannot find the association test helper for [%s]")
	HibernateException cannotFindAssociationTestHelperClassName(String datastoreProviderName);

	@Message(id = 1307,
			value = "The configuration property '" + Environment.POLYGLOT_ASSOCIATION_DATASTORE + "' has not been set")
	HibernateException unableToFindAssociationDatastoreConfiguration();

	@Message(id = 1308,
			value = "The configuration property '" + Environment.POLYGLOT_ENTITY_DATASTORE + "' has not been set")
	HibernateException unableToFindEntityDatastoreConfiguration();

	@Message(id = 1309, value = "Cannot instanciate the entity test helper for [%s]")
	HibernateException cannotInstantiateEntityTestHelperClassName(String testHelperClassName, @Cause Exception e);

	@Message(id = 1310, value = "Cannot instanciate the association test helper for [%s]")
	HibernateException cannotInstantiateAssociationTestHelperClassName(String testHelperClassName, @Cause Exception e);

	@Message(id = 1311, value = "[%s] has no constructor accepting DatastoreProvider")
	HibernateException gridHelperHasNoProperConstrutor(String helperClassName);

	@LogMessage(level = INFO)
	@Message(id = 1312, value = "Using %s as association datastore provider")
	void usingAsAssociationDatastoreProvider(String moduleName);

	@LogMessage(level = INFO)
	@Message(id = 1313, value = "Using %s as entity datastore provider")
	void usingAsEntityDatastoreProvider(String moduleName);
}
