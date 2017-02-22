/**
 * Created by jacknie, 2017. 2. 22.
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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * @author jacknie
 *
 */
public abstract class AbstractMapperResourceWatcher implements MapperResourceWatcher {
	
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final MapperResourceWatchContext watchContext;
	protected final Configuration configuration;

	public AbstractMapperResourceWatcher(MapperResourceWatchContext watchContext, Configuration configuration) {
		this.watchContext = watchContext;
		this.configuration = configuration;
	}
	
	@Override
	public void watch(File watchTargetDirectory) throws IOException {
		watchContext.addTargetDirectory(watchTargetDirectory);
		Runnable watchRunner = new WatchRunner(watchTargetDirectory);
		Thread watchThread = new Thread(watchRunner);
		watchThread.start();
	}
	
	protected abstract File receiveModification(File watchTargetDirectory) throws IOException;
	
	private class WatchRunner implements Runnable {
		
		File watchTargetDirectory;

		public WatchRunner(File watchTargetDirectory) {
			this.watchTargetDirectory = watchTargetDirectory;
		}

		@Override
		public void run() {
			while (true) {
				try {
					File modifiedFile = receiveModification(watchTargetDirectory);
					String modifiedFilePath = modifiedFile.getAbsolutePath();
					logger.debug("[{}] file modified.", modifiedFilePath);
					Resource mapperResource = getMatchedMapperResource(modifiedFilePath);
					if (mapperResource != null) {
						logger.debug("start parse mapper resource.");
						try {
							InputStream mapperSource = mapperResource.getInputStream();
							String resourceName = mapperResource.toString();
							XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(mapperSource,
									configuration, resourceName, configuration.getSqlFragments());
			
							xmlMapperBuilder.parse();
						} catch (Exception e) {
							throw new IllegalStateException(e);
						} finally {
							ErrorContext.instance().reset();
						}
						logger.debug("WatcherService reloads mybatis mapper resource.");
					}
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		
		Resource getMatchedMapperResource(String modifiedFilePath) throws IOException {
			List<Resource> resources = watchContext.getResources(watchTargetDirectory.getAbsolutePath());
			if (resources != null) {
				for (Resource resource : resources) {
					String resourcePath = resource.getFile().getAbsolutePath();
					if (resourcePath.equals(modifiedFilePath)) {
						return resource;
					}
				}
			}
			return null;
		}
	}

}
