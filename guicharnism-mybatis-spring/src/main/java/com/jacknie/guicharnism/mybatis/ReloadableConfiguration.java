/**
 * Created by jacknie, 2017. 2. 21.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReloadableConfiguration extends Configuration {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<String, MappedStatement> mappedStatements = new HashMap<String, MappedStatement>();
	private final Configuration config;

	public ReloadableConfiguration(Configuration config) {
		this.config = config;
	}

	@Override
	public boolean isResourceLoaded(String resource) {
		return false;
	}

	@Override
	public void addMappedStatement(MappedStatement ms) {
		logger.debug("add {}", ms.getResource());
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
