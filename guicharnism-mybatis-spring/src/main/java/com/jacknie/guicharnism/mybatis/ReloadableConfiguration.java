/**
 * 
 */
package com.jacknie.guicharnism.mybatis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.builder.annotation.MethodResolver;
import org.apache.ibatis.builder.xml.XMLStatementBuilder;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;

/**
 * @author PCN
 *
 */
public class ReloadableConfiguration extends Configuration {
	
	private final Configuration config;
	private final Map<String, MappedStatement> mappedStatements;

	public ReloadableConfiguration(Configuration config) {
		this.config = config;
		this.mappedStatements = new HashMap<String, MappedStatement>();
	}

	@Override
	public boolean isResourceLoaded(String resource) {
		return false;
	}

	@Override
	public void addMappedStatement(MappedStatement ms) {
		this.mappedStatements.put(ms.getId(), ms);
	}

	@Override
	public Collection<String> getMappedStatementNames() {
		buildAllStatements();
		return this.mappedStatements.keySet();
	}

	@Override
	public Collection<MappedStatement> getMappedStatements() {
		buildAllStatements();
		return this.mappedStatements.values();
	}

	@Override
	public MappedStatement getMappedStatement(String id) {
		return getMappedStatement(id, true);
	}

	@Override
	public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
		if (validateIncompleteStatements) {
			buildAllStatements();
		}
		return ((MappedStatement) this.mappedStatements.get(id));
	}

	@Override
	public boolean hasStatement(String statementName) {
		return hasStatement(statementName, true);
	}

	@Override
	public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {
		if (validateIncompleteStatements) {
			buildAllStatements();
		}
		return this.mappedStatements.containsKey(statementName);
	}
	
	@Override
	protected void buildAllStatements() {
		
		Collection<ResultMapResolver> incompleteResultMaps = config.getIncompleteResultMaps();
		Collection<CacheRefResolver> incompleteCacheRefs = config.getIncompleteCacheRefs();
		Collection<XMLStatementBuilder> incompleteStatements = config.getIncompleteStatements();
		Collection<MethodResolver> incompleteMethods = config.getIncompleteMethods();
		
		if (!(incompleteResultMaps.isEmpty())) {
			synchronized (incompleteResultMaps) {
				((ResultMapResolver) incompleteResultMaps.iterator().next()).resolve();
			}
		}
		if (!(incompleteCacheRefs.isEmpty())) {
			synchronized (incompleteCacheRefs) {
				((CacheRefResolver) incompleteCacheRefs.iterator().next()).resolveCacheRef();
			}
		}
		if (!(incompleteStatements.isEmpty())) {
			synchronized (incompleteStatements) {
				((XMLStatementBuilder) incompleteStatements.iterator().next()).parseStatementNode();
			}
		}
		if (!(incompleteMethods.isEmpty()))
			synchronized (incompleteMethods) {
				((MethodResolver) incompleteMethods.iterator().next()).resolve();
			}
	}
	
}
