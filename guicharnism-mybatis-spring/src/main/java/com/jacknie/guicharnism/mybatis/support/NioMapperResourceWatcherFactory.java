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
package com.jacknie.guicharnism.mybatis.support;

import org.apache.ibatis.session.Configuration;

import com.jacknie.guicharnism.mybatis.MapperResourceWatchContext;
import com.jacknie.guicharnism.mybatis.MapperResourceWatcher;
import com.jacknie.guicharnism.mybatis.MapperResourceWatcherFactory;

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
