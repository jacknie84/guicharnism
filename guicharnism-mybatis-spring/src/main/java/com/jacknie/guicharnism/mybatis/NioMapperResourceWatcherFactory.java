package com.jacknie.guicharnism.mybatis;

import org.apache.ibatis.session.Configuration;

public class NioMapperResourceWatcherFactory implements MapperResourceWatcherFactory {

	private String realoadTargetFilePattern;
	
	public void setRealoadTargetFilePattern(String realoadTargetFilePattern) {
		this.realoadTargetFilePattern = realoadTargetFilePattern;
	}

	@Override
	public MapperResourceWatcher createWatcher(MapperResourceWatchContext watchContext, Configuration configuration) {
		return new NioMapperResourceWatcher(watchContext, configuration, realoadTargetFilePattern);
	}

}
