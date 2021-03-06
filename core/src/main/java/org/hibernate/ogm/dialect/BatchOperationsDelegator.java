/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect;

import java.util.Iterator;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.batch.OperationsQueue;
import org.hibernate.ogm.dialect.batch.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.batch.RemoveTupleOperation;
import org.hibernate.ogm.dialect.batch.UpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.UpdateTupleOperation;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

/**
 * Wraps a {@link BatchableGridDialect} intercepting the operation and populating the queue that the delegate
 * will use to execute operations in batch.
 * <p>
 * The {@link TupleContext} and {@link AssociationContext} are also populated with the {@link OperationsQueue}
 * before looking for element in the db. This way the underlying datastore can make assumptions about elements
 * that are in the queue but not in the db.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class BatchOperationsDelegator implements BatchableGridDialect {

	private final ThreadLocal<OperationsQueue> operationQueueLocal = new ThreadLocal<OperationsQueue>();

	private final BatchableGridDialect dialect;

	public BatchOperationsDelegator(BatchableGridDialect dialect) {
		this.dialect = dialect;
	}

	public void prepareBatch() {
		operationQueueLocal.set( new OperationsQueue() );
	}

	private boolean isBatchDisabled() {
		return getOperationQueue().isClosed();
	}

	public void clearBatch() {
		operationQueueLocal.remove();
	}

	private OperationsQueue getOperationQueue() {
		OperationsQueue operationsQueue = operationQueueLocal.get();
		if ( operationsQueue == null ) {
			return OperationsQueue.CLOSED_QUEUE;
		}
		else {
			return operationsQueue;
		}
	}

	public void executeBatch() {
		executeBatch( getOperationQueue() );
	}

	@Override
	public void executeBatch(OperationsQueue queue) {
		dialect.executeBatch( getOperationQueue() );
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		return dialect.getLockingStrategy( lockable, lockMode );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		OperationsQueue queue = getOperationQueue();
		tupleContext.setOperationsQueue( queue );
		return dialect.getTuple( key, tupleContext );
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		return dialect.createTuple( key );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key) {
		if ( isBatchDisabled() ) {
			dialect.updateTuple( tuple, key );
		}
		else {
			getOperationQueue().add( new UpdateTupleOperation( tuple, key ) );
		}
	}

	@Override
	public void removeTuple(EntityKey key) {
		if ( isBatchDisabled() ) {
			dialect.removeTuple( key );
		}
		else {
			getOperationQueue().add( new RemoveTupleOperation( key ) );
		}
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		associationContext.setOperationsQueue( getOperationQueue() );
		return dialect.getAssociation( key, associationContext );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		return dialect.createAssociation( key, associationContext );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		if ( isBatchDisabled() ) {
			dialect.updateAssociation( association, key, associationContext );
		}
		else {
			getOperationQueue().add( new UpdateAssociationOperation( association, key, associationContext ) );
		}
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( isBatchDisabled() ) {
			dialect.removeAssociation( key, associationContext );
		}
		else {
			getOperationQueue().add( new RemoveAssociationOperation( key, associationContext ) );
		}
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return dialect.createTupleAssociation( associationKey, rowKey );
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		dialect.nextValue( key, value, increment, initialValue );
	}

	@Override
	public GridType overrideType(Type type) {
		return dialect.overrideType( type );
	}

	@Override
	public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		dialect.forEachTuple( consumer, entityKeyMetadatas );
	}

	@Override
	public Iterator<Tuple> executeBackendQuery(CustomQuery customQuery, EntityKeyMetadata[] metadatas) {
		return dialect.executeBackendQuery( customQuery, metadatas );
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
		return dialect.isStoredInEntityStructure( associationKey, associationContext );
	}
}
