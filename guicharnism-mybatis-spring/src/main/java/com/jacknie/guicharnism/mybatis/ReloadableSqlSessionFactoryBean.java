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

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

public class ReloadableSqlSessionFactoryBean extends SqlSessionFactoryBean {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final MapperResourceWatchContext watchContext = new MapperResourceWatchContext();
	private Resource[] reloadTargets;
	private String realoadTargetFilePattern;

	public void setReloadTargets(Resource[] reloadTargets) {
		this.reloadTargets = reloadTargets;
	}

	public void setRealoadTargetFilePattern(String realoadTargetFilePattern) {
		this.realoadTargetFilePattern = realoadTargetFilePattern;
	}

	@Override
	public void setSqlSessionFactoryBuilder(SqlSessionFactoryBuilder sqlSessionFactoryBuilder) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		
		Assert.notEmpty(reloadTargets);
		Assert.hasLength(realoadTargetFilePattern);
		
		super.setSqlSessionFactoryBuilder(new ReloadableSqlSessionFactoryBuilder());
		super.afterPropertiesSet();
		
		Configuration configuration = getObject().getConfiguration();
		watchContext.setRealoadTargetFilePattern(realoadTargetFilePattern);
		MapperResourceWatcherFactory watcherFactory = watchContext.resolveFactory();
		MapperResourceWatcher mapperWatcher = watcherFactory.createWatcher(watchContext, configuration);
		
		for (Resource realoadTarget : reloadTargets) {
			if (!watchContext.isAlreadyWatched(realoadTarget)) {
				logger.debug("watching... \"{}\"", realoadTarget);
				mapperWatcher.watch(realoadTarget);
			}
		}
	}
	
}
