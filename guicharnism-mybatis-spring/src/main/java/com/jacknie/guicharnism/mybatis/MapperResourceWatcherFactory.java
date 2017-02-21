package com.jacknie.guicharnism.mybatis;

import org.apache.ibatis.session.Configuration;

public interface MapperResourceWatcherFactory {

	MapperResourceWatcher createWatcher(MapperResourceWatchContext watchContext, Configuration configuration);
}
