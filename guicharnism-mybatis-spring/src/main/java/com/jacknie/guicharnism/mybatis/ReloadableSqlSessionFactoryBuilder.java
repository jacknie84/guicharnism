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
		private final String intercepTargets;

		public MappedStatementIntercepter(Configuration config) {
			this.config = config;
			this.reloadableConfig = new ReloadableConfiguration(config);
			this.intercepTargets = "isResourceLoaded, addMappedStatement, getMappedStatementNames, getMappedStatements, getMappedStatement, hasStatement";
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
			if (intercepTargets.contains(methodName)) {
				return methodProxy.invoke(reloadableConfig, args);
			}
			else {
				return methodProxy.invoke(config, args);
			}
		}
		
	}

}
