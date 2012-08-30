/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb.query;

import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.ogm.service.impl.SessionFactoryEntityNamesResolver;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.origin.hql.parse.HQLLexer;
import org.hibernate.sql.ast.origin.hql.parse.HQLParser;
import org.hibernate.sql.ast.origin.hql.resolve.EntityNamesResolver;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class MongoDBQueryParserService implements QueryParserService {

	private ServiceRegistryImplementor registery;
	private Map<String, Object> cfg;
	private MongoDBDatastoreProvider provider;
	private volatile SessionFactoryEntityNamesResolver entityNamesResolver;

	public MongoDBQueryParserService(ServiceRegistryImplementor registery, Map<String, Object> configuration){
		this.registery = registery;
		this.cfg = configuration;
		this.provider = (MongoDBDatastoreProvider)this.registery.getService( DatastoreProvider.class );
	}

	@Override
	public Query getParsedQueryExecutor(Session session, String queryString, Map<String, Object> namedParameters) {
		HQLLexer lexed = new HQLLexer( new ANTLRStringStream( queryString ) );
		TokenStream tokens = new CommonTokenStream( lexed );
		HQLParser parser = new HQLParser( tokens );
		try {
			//TODO move the following logic into the hibernate-jpql-parser project?
			//needs to consider usage of a parsed query plans cache

			// parser#statement() is the entry point for evaluation of any kind of statement
			HQLParser.statement_return r = parser.statement();
			CommonTree tree = (CommonTree) r.getTree();
			// To walk the resulting tree we need a treenode stream:
			CommonTreeNodeStream treeStream = new CommonTreeNodeStream( tree );
			// AST nodes have payloads referring to the tokens from the Lexer:
			treeStream.setTokenStream( tokens );
			EntityNamesResolver entityNamesResolver = getDefinedEntityNames( session.getSessionFactory() );

			return new MongoDBQuery( queryString, FlushMode.AUTO, (OgmSession) session, null, this );
		}catch( RecognitionException e){
			throw new HibernateException("Unable to parse the query", e);
		}
	}

	public MongoDBDatastoreProvider getDatastoreProvider(){
		return this.provider;
	}

	private EntityNamesResolver getDefinedEntityNames(SessionFactory sessionFactory) {
		if ( entityNamesResolver == null ) {
			entityNamesResolver = new SessionFactoryEntityNamesResolver( sessionFactory );
		}
		return entityNamesResolver;
	}
}
