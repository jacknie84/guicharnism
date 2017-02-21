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

import java.io.File;
import java.io.FileFilter;

import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

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
	public void afterPropertiesSet() throws Exception {
		
		super.setSqlSessionFactoryBuilder(new ReloadableSqlSessionFactoryBuilder());
		super.afterPropertiesSet();
		
		Configuration configuration = getObject().getConfiguration();
		MapperResourceWatcherFactory watcherFactory = watchContext.resolveFactory("realoadTargetFilePattern", realoadTargetFilePattern);
		MapperResourceWatcher mapperWatcher = watcherFactory.createWatcher(watchContext, configuration);
		
		for (Resource realoadTarget : reloadTargets) {
			File watchTargetDirectory = realoadTarget.getFile();
			String watchTarget = watchTargetDirectory.getAbsolutePath();
			if (!watchContext.isExistsDirectory(watchTarget)) {
				logger.debug("watching... [{}]", watchTarget);
				mapperWatcher.watch(watchTargetDirectory);
			}
			File[] children = watchTargetDirectory.listFiles(new WatchFileFilter(mapperWatcher));
			if (children != null) {
				for (File child : children) {
					Resource r = new FileSystemResource(child);
					watchContext.addResource(watchTarget, r);
				}
			}
			
		}
	}
	
	private class WatchFileFilter implements FileFilter {
		
		private final MapperResourceWatcher mapperWatcher;

		public WatchFileFilter(MapperResourceWatcher mapperWatcher) {
			this.mapperWatcher = mapperWatcher;
		}

		@Override
		public boolean accept(File file) {
			return file.isFile() && mapperWatcher.isTargetFileName(file.getName());
		}
		
	}
}
