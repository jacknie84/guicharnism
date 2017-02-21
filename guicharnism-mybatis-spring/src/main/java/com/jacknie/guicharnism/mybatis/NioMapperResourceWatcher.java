package com.jacknie.guicharnism.mybatis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

	private static final Logger logger = LoggerFactory.getLogger(NioMapperResourceWatcher.class);
	
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
			throw new IllegalStateException("\"" + realoadTargetFilePattern + "\" is not ant pattern.");
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
		
		final Path watchTarget = Paths.get(watchTargetDirectory.toURI());
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						WatchService watcher = watchTarget.getFileSystem().newWatchService();
						watchTarget.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
						WatchKey watchKey = watcher.take();
						List<WatchEvent<?>> events = watchKey.pollEvents();
						for (WatchEvent<?> event : events) {
							if (StandardWatchEventKinds.ENTRY_MODIFY.equals(event.kind())) {
								handleEvent(watchTarget, event);
							}
						}
					} catch (IOException | InterruptedException e) {
						throw new IllegalStateException(e);
					}
				}
			}
			
		}).start();
	}
	
	private void handleEvent(Path watchTarget, WatchEvent<?> event) throws InterruptedException, IOException {
		Path eventPath = (Path) event.context();
		String fileName = eventPath.getFileName().toString();
		if (isTargetFileName(fileName)) {
			Path resolvedPath = watchTarget.resolve(eventPath).toAbsolutePath();
			logger.debug("[{}] file modified.", resolvedPath);
			List<Resource> resources = watchContext.getResources(watchTarget.toString());
			if (resources != null) {
				for (Resource r : resources) {
					Path resourcePath = Paths.get(r.getURI());
					if (resourcePath.equals(resolvedPath)) {
						logger.debug("start parse mapper resource.");
						try {
							XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(r.getInputStream(),
									configuration, r.toString(), configuration.getSqlFragments());
		
							xmlMapperBuilder.parse();
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							ErrorContext.instance().reset();
						}
						logger.debug("WatcherService reloads mybatis mapper resource.");
					}
				}
			}
		}
	}
}
