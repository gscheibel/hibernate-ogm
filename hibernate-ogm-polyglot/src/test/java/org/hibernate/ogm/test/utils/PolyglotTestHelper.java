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

package org.hibernate.ogm.test.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider;
import org.hibernate.ogm.datastore.polyglot.impl.PolyglotDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.logging.polyglot.impl.Log;
import org.hibernate.ogm.logging.polyglot.impl.LoggerFactory;

/**
 * The Polyglot Test Helper so as the {@link org.hibernate.ogm.datastore.polyglot.impl.PolyglotDatastoreProvider}
 * and the {@link org.hibernate.ogm.dialect.polyglot.PolyglotDialect} handles the lifecycle and the communication with the proper
 * delegates.
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class PolyglotTestHelper implements TestableGridDialect {

	private static final Log log = LoggerFactory.getLogger();

	/**
	 * Create the test helper associated to the association datastore provider of the
	 * {@link org.hibernate.ogm.datastore.polyglot.impl.PolyglotDatastoreProvider}
	 *
	 * @param sessionFactory The session factory used to retrieve the polyglot datastore provider
	 *
	 * @return The entity test helper
	 */
	private static TestableGridDialect getAssociationTestHelper(SessionFactory sessionFactory) {
		PolyglotDatastoreProvider provider = getProvider( sessionFactory );
		final DatastoreProvider entityDatastoreProvider = provider.getAssociationDatastoreProvider();
		String testHelperClassName = getTestHelperClassName( entityDatastoreProvider );

		TestableGridDialect associationTestHelper;
		if ( testHelperClassName != null ) {
			try {
				associationTestHelper = instanciateTestHelper(
						provider.getAssociationDatastoreProvider(),
						testHelperClassName
				);
			}
			catch ( Exception e ) {
				throw log.cannotInstantiateAssociationTestHelperClassName( testHelperClassName, e );
			}
		}
		else {
			throw log.cannotFindAssociationTestHelperClassName( entityDatastoreProvider.getClass().getName() );
		}
		return associationTestHelper;
	}

	/**
	 * Retrieves the Polyglot datastore provider from the session factory
	 *
	 * @param sessionFactory The session factory to use
	 *
	 * @return
	 */
	private static PolyglotDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService(
				DatastoreProvider.class
		);
		if ( !( PolyglotDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with Polyglot, cannot extract underlying cache" );
		}
		return PolyglotDatastoreProvider.class.cast( provider );
	}

	/**
	 * To retrieve the right test helper class to instanciate, it takes the datastore provider class name then it
	 * extracts the {@link org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider} shortcuts and because this
	 * shortcut is the same as the one for the related TestHelper {@link org.hibernate.ogm.test.utils.GridDialectType}
	 * it can get the TestHelper class name.
	 *
	 * @param datastoreProvider The datastore provider used to get its related test helper class name
	 *
	 * @return The name of the test helper class
	 */
	private static String getTestHelperClassName(DatastoreProvider datastoreProvider) {
		final String className = datastoreProvider.getClass().getName();
		final String moduleName = findDatastoreProviderShortcut( className );

		for ( GridDialectType type : GridDialectType.values() ) {
			if ( type.name().equalsIgnoreCase( moduleName ) ) {
				return type.getTestHelperClassName();
			}
		}
		return null;
	}

	/**
	 * Instanciate a test helper and inject the related datastore provider
	 *
	 * @param provider To inject into the test helper
	 * @param testHelperClassName Class name of the to instanciate
	 *
	 * @return The test helper instance
	 *
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private static TestableGridDialect instanciateTestHelper(DatastoreProvider provider, String testHelperClassName)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class<? extends TestableGridDialect> helperClass = (Class<? extends TestableGridDialect>) Class.forName(
				testHelperClassName
		);
		Constructor injector = null;
		for ( Constructor constructor : helperClass.getConstructors() ) {
			Class[] parameterTypes = constructor.getParameterTypes();
			if ( parameterTypes.length == 1 && DatastoreProvider.class.isAssignableFrom( parameterTypes[0] ) ) {
				injector = constructor;
				break;
			}
		}
		if ( injector == null ) {
			throw log.gridHelperHasNoProperConstrutor( helperClass.getName() );
		}
		return (TestableGridDialect) injector.newInstance( provider );
	}

	/**
	 * Retrieves the datastore provider shortcut from its class name.
	 * For example: 'INFINISPAN' from "org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider".
	 * The list is here: {@link AvailableDatastoreProvider}
	 *
	 * @param className The datastore provider class name
	 *
	 * @return The shortcut
	 */
	private static String findDatastoreProviderShortcut(String className) {
		for ( AvailableDatastoreProvider adp : AvailableDatastoreProvider.values() ) {
			if ( adp.getDatastoreProviderClassName().equals( className ) ) {
				return adp.name();
			}
		}
		return null;
	}

	/**
	 * Create the test helper associated to the entity datastore provider of the
	 * {@link org.hibernate.ogm.datastore.polyglot.impl.PolyglotDatastoreProvider}
	 *
	 * @param sessionFactory The session factory used to retrieve the polyglot datastore provider
	 *
	 * @return The entity test helper
	 */
	private TestableGridDialect getEntityTestHelper(SessionFactory sessionFactory) {
		PolyglotDatastoreProvider provider = getProvider( sessionFactory );
		final DatastoreProvider entityDatastoreProvider = provider.getEntityDatastoreProvider();
		String testHelperClassName = getTestHelperClassName( entityDatastoreProvider );

		TestableGridDialect entityTestHelper;
		if ( testHelperClassName != null ) {
			try {
				entityTestHelper = instanciateTestHelper( provider.getEntityDatastoreProvider(), testHelperClassName );
			}
			catch ( Exception e ) {
				throw log.cannotInstantiateEntityTestHelperClassName( testHelperClassName, e );
			}
		}
		else {
			throw log.cannotFindEntityTestHelperClassName( entityDatastoreProvider.getClass().getName() );
		}
		return entityTestHelper;
	}

	@Override
	public boolean assertNumberOfEntities(int numberOfEntities, SessionFactory sessionFactory) {
		return getEntityTestHelper( sessionFactory ).assertNumberOfEntities( numberOfEntities, sessionFactory );
	}

	@Override
	public boolean assertNumberOfAssociations(int numberOfAssociations, SessionFactory sessionFactory) {
		return getAssociationTestHelper( sessionFactory ).assertNumberOfAssociations(
				numberOfAssociations,
				sessionFactory
		);
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		return getEntityTestHelper( sessionFactory ).extractEntityTuple( sessionFactory, key );
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		getEntityTestHelper( sessionFactory ).dropSchemaAndDatabase( sessionFactory );
		getAssociationTestHelper( sessionFactory ).dropSchemaAndDatabase( sessionFactory );
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return null;
	}
}
