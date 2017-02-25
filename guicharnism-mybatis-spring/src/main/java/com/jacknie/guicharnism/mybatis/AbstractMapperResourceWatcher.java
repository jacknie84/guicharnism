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
public abstract class AbstractMapperResourceWatcher implements MapperResourceWatcher, Runnable {
	
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final MapperResourceWatchContext watchContext;
	protected final Resource watchTarget; 
	
	protected Thread watchThread;
	protected XMLMapperBuilder mapperBuilder;

	public AbstractMapperResourceWatcher(MapperResourceWatchContext watchContext, Resource watchTarget) {
		this.watchContext = watchContext;
		this.watchTarget = watchTarget;
	}

	public void setMapperBuilder(XMLMapperBuilder mapperBuilder) {
		this.mapperBuilder = mapperBuilder;
	}

	@Override
	public Resource getTargetResource() {
		return watchTarget;
	}

	@Override
	public void watch() throws IOException {
		if (isWatched()) {
			throw new IllegalStateException("Already watched.");
		}
		watchContext.addWatcher(this);
		prepareWatchThread().start();
	}
	
	@Override
	public boolean isWatched() {
		return watchThread != null && watchThread.isAlive() && !watchThread.isInterrupted();
	}

	@Override
	public void release() throws IOException {
		if (isWatched()) {
			watchThread.interrupt();
			watchContext.removeWatchedResource(this);
		}
	}
	
	private Thread prepareWatchThread() throws IOException {
		if (this.watchThread == null || this.watchThread.isInterrupted()) {
			Thread watchThread = new Thread(this);
			this.watchThread = watchThread;
		}
		return this.watchThread;
	}

	@Override
	public void run() {
		while (true) {
			try {
				File modifiedFile = receiveModification(watchTarget.getFile());
				if (modifiedFile != null) {
					String modifiedFilePath = modifiedFile.getAbsolutePath();
					logger.debug("[{}] file modified.", modifiedFilePath);
					Resource mapperResource = getMatchedMapperResource(modifiedFilePath);
					if (mapperResource != null) {
						logger.debug("start parse mapper resource.");
						try {
							Configuration configuration = watchContext.getConfiguration();
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
						logger.debug("MapperResourceWatcher reloads mybatis mapper resource.");
					}
					else {
						logger.debug("[{}] file is not exists matched resource.");
					}
				}
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} catch (InterruptedException e) {
				logger.debug("[{}] thread is interrpted.", watchThread.getName());
				logger.debug("Watcher turn off monitoring {} resource.", watchTarget);
				this.watchThread = null;
				return;
			}
		}
	}
	
	private Resource getMatchedMapperResource(String modifiedFilePath) throws IOException {
		List<Resource> resources = watchContext.getResources(watchTarget.getFile().getAbsolutePath());
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

	/**
	 * 현재 쓰레드를 비활성화(lock) 시키고 파일 수정이 일어날 때 다시 쓰레드를 활성화 한다.
	 * @param watchTargetDirectory
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	protected abstract File receiveModification(File watchTargetDirectory) throws IOException, InterruptedException;
	
}
