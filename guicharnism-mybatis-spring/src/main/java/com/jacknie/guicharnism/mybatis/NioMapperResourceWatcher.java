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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

public class NioMapperResourceWatcher implements MapperResourceWatcher {

	private final Logger logger = LoggerFactory.getLogger(NioMapperResourceWatcher.class);
	
	private final MapperResourceWatchContext watchContext;
	private final Configuration configuration;
	private final String realoadTargetFilePattern;
	private final PathMatcher pathMatcher;
	
	public NioMapperResourceWatcher(MapperResourceWatchContext watchContext, Configuration configuration, String realoadTargetFilePattern) {
		this.watchContext = watchContext;
		this.configuration = configuration;
		this.pathMatcher = new AntPathMatcher();
		if (pathMatcher.isPattern(realoadTargetFilePattern)) {
			this.realoadTargetFilePattern = realoadTargetFilePattern;
		}
		else {
			throw new IllegalArgumentException("\"" + realoadTargetFilePattern + "\" is not ant pattern.");
		}
	}

	@Override
	public boolean isTargetFileName(String fileName) {
		return pathMatcher.match(realoadTargetFilePattern, fileName);
	}

	@Override
	public void watch(File watchTargetDirectory) throws IOException {
		
		if (!watchTargetDirectory.exists()) {
			throw new FileNotFoundException(watchTargetDirectory.getAbsolutePath());
		}
		if (!watchTargetDirectory.isDirectory()) {
			throw new IllegalArgumentException("File object is not referenced directory.");
		}
		Runnable watchRunner = new WatchRunner(watchTargetDirectory);
		Thread watchThread = new Thread(watchRunner);
		watchThread.start();
	}
	
	private class WatchRunner implements Runnable {
		
		private Path watchTargetPath;
		
		WatchRunner(File watchTargetDirectory) {
			this(Paths.get(watchTargetDirectory.toURI()));
		}

		WatchRunner(Path watchTargetPath) {
			this.watchTargetPath = watchTargetPath;
		}

		@Override
		public void run() {
			while (true) {
				try {
					WatchService watcher = watchTargetPath.getFileSystem().newWatchService();
					watchTargetPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
					WatchKey watchKey = watcher.take();
					List<WatchEvent<?>> events = watchKey.pollEvents();
					for (WatchEvent<?> event : events) {
						if (StandardWatchEventKinds.ENTRY_MODIFY.equals(event.kind())) {
							handleEvent(event);
						}
					}
					
				} catch (IOException | InterruptedException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		
		void handleEvent(WatchEvent<?> event) throws IOException {
			Path eventPath = (Path) event.context();
			Path resolvedPath = watchTargetPath.resolve(eventPath).toAbsolutePath();
			logger.debug("[{}] file modified.", resolvedPath);
			Resource mapperResource = getMatchedMapperResource(resolvedPath);
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
		}
		
		Resource getMatchedMapperResource(Path resolvedPath) throws IOException {
			List<Resource> resources = watchContext.getResources(watchTargetPath.toString());
			if (resources != null) {
				for (Resource resource : resources) {
					Path resourcePath = Paths.get(resource.getURI());
					if (resourcePath.equals(resolvedPath)) {
						return resource;
					}
				}
			}
			return null;
		}
		
	}
}
