/**
 * Created by jacknie, 2017. 2. 25.
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
package com.jacknie.guicharnism.mybatis.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.session.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

import com.jacknie.guicharnism.mybatis.MapperResourceWatchContext;
import com.jacknie.guicharnism.mybatis.MapperResourceWatcher;
import com.jacknie.guicharnism.mybatis.MapperResourceWatcherFactory;
import com.jacknie.guicharnism.mybatis.support.VfsMapperResourceWatcher;
import com.jacknie.guicharnism.mybatis.support.VfsMapperResourceWatcherFactory;

/**
 * @author jacknie
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class VfsMapperResourceWatcherTest {

	@Mock Configuration configuration;
	@Mock MapperResourceWatchContext watchContext;
	@Mock Resource watchTarget;
	
	private MapperResourceWatcherFactory factory;
	
	@Before
	public void before() throws Exception {
		this.factory = new VfsMapperResourceWatcherFactory(watchContext);
		File classPath = ResourceUtils.getFile("classpath:");
		when(watchTarget.getFile()).thenReturn(classPath);
	}
	
	@Test
	public void isWatchedTest() throws Exception {
		//when construct
		MapperResourceWatcher watcher = factory.createWatcher(watchTarget);
		assertFalse(watcher.isWatched());
		
		// when watching
		watcher.watch();
		assertTrue(watcher.isWatched());
		
		// when release
		watcher.release();
		assertFalse(watcher.isWatched());
	}
	
	@Test
	public void watchTest() throws Exception {
		//given
		final Object mutext = new Object();
		final Resource testResource = new ClassPathResource("test.xml");
		when(watchContext.getConfiguration()).thenReturn(configuration);
		when(watchContext.getResources(anyString())).then(new Answer<List<Resource>>() {

			@Override
			public List<Resource> answer(InvocationOnMock invocation) throws Throwable {
				synchronized(mutext) {
					mutext.notifyAll();
					return Collections.singletonList(testResource);
				}
			}
			
		});
		MapperResourceWatcher watcher = (VfsMapperResourceWatcher) factory.createWatcher(watchTarget);
		watcher.watch();
		
		//when file modification
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(2000);
					modifyResource(testResource);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}).start();
		
		synchronized(mutext) {
			mutext.wait();
			verify(watchContext, atLeastOnce()).getResources(anyString());
			verify(watchContext, atLeastOnce()).getConfiguration();
		}
	}
	
	private void modifyResource(Resource testResource) throws IOException {
		OutputStream out = new FileOutputStream(testResource.getFile());
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
		try {
			writer.println("test");
		} finally {
			writer.flush();
			writer.close();
		}
	}
}
