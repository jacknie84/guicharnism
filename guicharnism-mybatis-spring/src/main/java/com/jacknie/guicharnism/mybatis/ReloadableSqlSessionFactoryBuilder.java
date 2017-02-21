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

import java.lang.reflect.Method;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

public class ReloadableSqlSessionFactoryBuilder extends SqlSessionFactoryBuilder {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private Enhancer enhancer;

	public ReloadableSqlSessionFactoryBuilder() {
		this.enhancer = new Enhancer();
		enhancer.setSuperclass(Configuration.class);
	}

	@Override
	public SqlSessionFactory build(Configuration config) {
		
		enhancer.setCallback(new MappedStatementIntercepter(config));
		Configuration proxy = (Configuration) enhancer.create();
		return super.build(proxy);
	}
	
	private class MappedStatementIntercepter implements MethodInterceptor {
		
		private final Configuration config;
		private final Configuration reloadableConfig;
		private final String interceptTargets;

		public MappedStatementIntercepter(Configuration config) {
			this.config = config;
			this.reloadableConfig = new ReloadableConfiguration(config);
			this.interceptTargets = "isResourceLoaded, addMappedStatement, getMappedStatementNames, getMappedStatements, getMappedStatement, hasStatement";
			for (Object element : config.getMappedStatements()) {
				Class<?> type = element.getClass();
				if (type.isAssignableFrom(MappedStatement.class)) {
					MappedStatement ms = (MappedStatement) element;
					reloadableConfig.addMappedStatement(ms);
				}
				else {
					logger.debug("[{}] object is not instance of MappedStatement class.");
				}
			}
		}

		@Override
		public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
			String methodName = method.getName();
			if (interceptTargets.contains(methodName)) {
				return methodProxy.invoke(reloadableConfig, args);
			}
			else {
				return methodProxy.invoke(config, args);
			}
		}
		
	}

}
