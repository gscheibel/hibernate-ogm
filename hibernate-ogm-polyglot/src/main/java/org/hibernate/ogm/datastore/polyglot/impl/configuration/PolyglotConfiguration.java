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
package org.hibernate.ogm.datastore.polyglot.impl.configuration;

import java.util.Map;

import org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.logging.polyglot.impl.Log;
import org.hibernate.ogm.logging.polyglot.impl.LoggerFactory;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * Configuration for {@link org.hibernate.ogm.datastore.polyglot.impl.PolyglotDatastoreProvider}
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class PolyglotConfiguration<T extends DatastoreProvider & Configurable & Startable & Stoppable> {

	private static Log log = LoggerFactory.getLogger();
	private Class<T> entityDatastoreProviderClass;
	private Class<T> associationDatastoreProviderClass;

	/**
	 * Initialize the internal values from the given {@link java.util.Map}.
	 *
	 * @param configurationMap The values to use as configuration
	 *
	 * @see Environment
	 */
	public void initialize(Map configurationMap) {
		buildEntityDatastoreProviderClass( configurationMap );
		buildAssociationDatastoreProviderClass( configurationMap );
	}

	/**
	 * Retrieve the class to instanciate for the association datastore provider from
	 * the configuration {@see Environement#POLYGLOT_ASSOCIATION_DATASTORE}
	 *
	 * @param configurationMap The configuration properties
	 */
	private void buildAssociationDatastoreProviderClass(Map configurationMap) {
		Object cfgModuleName = configurationMap.get( Environment.POLYGLOT_ASSOCIATION_DATASTORE );
		if ( cfgModuleName == null ) {
			throw log.unableToFindAssociationDatastoreConfiguration();
		}
		final String moduleName = cfgModuleName.toString();
		this.associationDatastoreProviderClass = buildDatastoreProvider( moduleName );
		log.usingAsAssociationDatastoreProvider( moduleName );
	}

	/**
	 * Retrieve the class to instanciate for the entity datastore provider from
	 * the configuration {@see Environement.POLYGLOT_ENTITY_DATASTORE}
	 *
	 * @param configurationMap The configuration properties
	 */
	private void buildEntityDatastoreProviderClass(Map configurationMap) {
		Object cfgModuleName = configurationMap.get( Environment.POLYGLOT_ENTITY_DATASTORE );
		if ( cfgModuleName == null ) {
			throw log.unableToFindEntityDatastoreConfiguration();
		}
		final String moduleName = cfgModuleName.toString();
		this.entityDatastoreProviderClass = buildDatastoreProvider( moduleName );
		log.usingAsEntityDatastoreProvider( moduleName );
	}

	/**
	 * Retrieve the class associated to a module name. The association name/class
	 * is defined in the {@see org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider} enum
	 *
	 * @param moduleName Name of the module to retrieve
	 *
	 * @return The Class that represents the module
	 */
	private Class<T> buildDatastoreProvider(String moduleName) {
		final AvailableDatastoreProvider provider = AvailableDatastoreProvider.valueOf( moduleName.toUpperCase() );
		final String datastoreProviderClassName = provider.getDatastoreProviderClassName();

		try {
			return (Class<T>) Class.forName( datastoreProviderClassName );
		}
		catch ( ClassCastException e ) {
			throw log.doesNotImplementInterface( e, datastoreProviderClassName );
		}
		catch ( ClassNotFoundException e ) {
			throw log.unableToRetrieveEntityDatastoreProviderClass( datastoreProviderClassName );
		}
	}

	/**
	 * @return The datastore provider class that will manage entities
	 *
	 * @see Environment#POLYGLOT_ENTITY_DATASTORE
	 */
	public Class<T> getEntityDatastoreProviderClass() {
		return entityDatastoreProviderClass;
	}

	/**
	 * @return The datastore provider class that will manage associations
	 *
	 * @see Environment#POLYGLOT_ASSOCIATION_DATASTORE
	 */
	public Class<T> getAssociationDatastoreProviderClass() {
		return associationDatastoreProviderClass;
	}
}
