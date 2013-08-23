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
package org.hibernate.ogm.dialect.polyglot;

import java.lang.reflect.Constructor;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.polyglot.impl.PolyglotDatastoreProvider;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.logging.polyglot.impl.Log;
import org.hibernate.ogm.logging.polyglot.impl.LoggerFactory;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

/**
 * The polyglot dialect is in charge of the delegate dialects lifecycle and it
 * uses them to communication with the underlying datastore instances.
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class PolyglotDialect implements GridDialect {

	private static final Log log = LoggerFactory.getLogger();
	private final PolyglotDatastoreProvider provider;
	private GridDialect entityDialect;
	private GridDialect associationDialect;

	public PolyglotDialect(PolyglotDatastoreProvider provider) {
		this.provider = provider;
		this.instanciateEntityDialect();
		this.instanciateAssociationDialect();
	}

	/**
	 * Instanciate the dialect used to manage entities.
	 */
	private void instanciateEntityDialect() {
		final DatastoreProvider entityDatastoreProvider = this.provider.getEntityDatastoreProvider();
		final Class<? extends GridDialect> dialectClass = entityDatastoreProvider.getDefaultDialect();
		entityDialect = instanciateDialect( entityDatastoreProvider, dialectClass );
	}

	/**
	 * Instanciate the dialect used to manage associations.
	 */
	private void instanciateAssociationDialect() {
		final DatastoreProvider associationDatastoreProvider = this.provider.getAssociationDatastoreProvider();
		final Class<? extends GridDialect> dialectClass = associationDatastoreProvider.getDefaultDialect();
		associationDialect = instanciateDialect( associationDatastoreProvider, dialectClass );
	}

	/**
	 * Instanciate a delegate dialect and inject (by using a constructor) its related datastore provider because
	 * in the service registry, only the Polyglot DatastoreProvider is mentioned.
	 *
	 * @param entityDatastoreProvider The delegate datastore provider to inject
	 * @param dialectClass The class of the dialect to instanciate
	 *
	 * @return An instanciated dialect
	 */
	private GridDialect instanciateDialect(DatastoreProvider entityDatastoreProvider, Class<? extends GridDialect> dialectClass) {
		try {
			Constructor injector = null;
			for ( Constructor constructor : dialectClass.getConstructors() ) {
				Class[] parameterTypes = constructor.getParameterTypes();
				if ( parameterTypes.length == 1 && DatastoreProvider.class.isAssignableFrom( parameterTypes[0] ) ) {
					injector = constructor;
					break;
				}
			}
			if ( injector == null ) {
				throw log.gridDialectHasNoProperConstrutor( dialectClass );
			}
			return (GridDialect) injector.newInstance( entityDatastoreProvider );
		}
		catch ( Exception e ) {
			throw log.unableToInstanciateEntityDialect( e, dialectClass.getName() );
		}
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		throw new UnsupportedOperationException( "The polyglot dialect does not support locking" );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		return entityDialect.getTuple( key, tupleContext );
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		return entityDialect.createTuple( key );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key) {
		entityDialect.updateTuple( tuple, key );
	}

	@Override
	public void removeTuple(EntityKey key) {
		entityDialect.removeTuple( key );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		return associationDialect.getAssociation( key, associationContext );
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		return associationDialect.createAssociation( key );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		associationDialect.updateAssociation( association, key );
	}

	@Override
	public void removeAssociation(AssociationKey key) {
		associationDialect.removeAssociation( key );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return associationDialect.createTupleAssociation( associationKey, rowKey );
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		entityDialect.nextValue( key, value, increment, initialValue );
	}

	@Override
	public GridType overrideType(Type type) {
		return entityDialect.overrideType( type );
	}

	@Override
	public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		entityDialect.forEachTuple( consumer, entityKeyMetadatas );
	}
}
