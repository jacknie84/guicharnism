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
