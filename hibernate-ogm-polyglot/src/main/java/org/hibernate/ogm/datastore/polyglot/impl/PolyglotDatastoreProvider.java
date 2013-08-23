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
package org.hibernate.ogm.datastore.polyglot.impl;

import java.util.Map;

import org.hibernate.ogm.datastore.polyglot.impl.configuration.PolyglotConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.polyglot.PolyglotDialect;
import org.hibernate.ogm.logging.polyglot.impl.Log;
import org.hibernate.ogm.logging.polyglot.impl.LoggerFactory;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * The polyglot datastore provider is in charge of the lifecycle of the 2 delegates datastore provider.
 * The delegates' lifecyle are almost the same a standards datastore provider except for the service inject.
 * Because the delegates are instanciated after the service injection into the polyglot datastore provider.
 *
 * Delegates must implements the following interfaces:
 * - {@link org.hibernate.ogm.datastore.spi.DatastoreProvider}
 * - {@link org.hibernate.service.spi.Configurable}
 * - {@link org.hibernate.service.spi.Startable}
 * - {@link org.hibernate.service.spi.Stoppable}
 * - {@link org.hibernate.service.spi.ServiceRegistryAwareService}
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class PolyglotDatastoreProvider<T extends DatastoreProvider & Configurable & Startable & Stoppable>
		implements DatastoreProvider, Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final Log log = LoggerFactory.getLogger();
	private final PolyglotConfiguration config = new PolyglotConfiguration();
	private T entityDatastoreProvider;
	private T associationDatastoreProvider;
	private boolean isCacheStated;
	private Map configurationMap;
	private ServiceRegistryImplementor serviceRegistry;

	@Override
	public void configure(Map configurationValues) {
		configurationMap = configurationValues;
		this.config.initialize( configurationMap );
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return PolyglotDialect.class;
	}

	@Override
	public void start() {
		if ( !isCacheStated ) {
			if ( entityDatastoreProvider == null ) {
				instanciateEntityDatastoreProvider();
			}

			if ( associationDatastoreProvider == null ) {
				instanciateAssociationDatastoreProvider();
			}

			this.entityDatastoreProvider.start();
			this.associationDatastoreProvider.start();
			this.isCacheStated = true;
		}
	}

	@Override
	public void stop() {
		this.entityDatastoreProvider.stop();
		this.associationDatastoreProvider.stop();
	}

	public T getEntityDatastoreProvider() {
		return entityDatastoreProvider;
	}

	public T getAssociationDatastoreProvider() {
		return associationDatastoreProvider;
	}

	/**
	 * Instanciate the entity datastore provider and inject the service registry for its
	 * configuration.
	 */
	private void instanciateEntityDatastoreProvider() {
		Class<T> edp = this.config.getEntityDatastoreProviderClass();
		this.entityDatastoreProvider = instanciateDatastoreProvider( edp );

		if ( entityDatastoreProvider instanceof ServiceRegistryAwareService ) {
			( (ServiceRegistryAwareService) entityDatastoreProvider ).injectServices( serviceRegistry );
		}
	}

	/**
	 * Instanciate the association datastore provider and inject the service registry for its
	 * configuration.
	 */
	private void instanciateAssociationDatastoreProvider() {
		Class<T> adp = this.config.getAssociationDatastoreProviderClass();
		this.associationDatastoreProvider = instanciateDatastoreProvider( adp );

		if ( associationDatastoreProvider instanceof ServiceRegistryAwareService ) {
			( (ServiceRegistryAwareService) associationDatastoreProvider ).injectServices( serviceRegistry );
		}
	}

	/**
	 * Instanciate a datastore provider and inject the configuraton map
	 * so it use all the necessary properties (hostname, port, specific configuration files, etc.)
	 *
	 * @param dp The class representing the datastore to instanciate
	 *
	 * @return The instanciated datastore provider
	 */
	private T instanciateDatastoreProvider(Class<T> dp) {
		try {
			T datastoreToInstanciate = dp.newInstance();
			datastoreToInstanciate.configure( configurationMap );
			return datastoreToInstanciate;
		}
		catch ( InstantiationException e ) {
			throw log.unableToInstanciateEntityDatastoreProvider( e, dp.getName() );
		}
		catch ( IllegalAccessException e ) {
			throw log.unableToInstanciateEntityDatastoreProvider( e, dp.getName() );
		}
	}

	/**
	 * Store service registry so it can inject them into delegate datastore provider
	 * once instanciated.
	 *
	 * @param serviceRegistry The registry to inject
	 */
	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
}
