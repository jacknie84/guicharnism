# guicharnism
<code>
<bean class="com.jacknie.guicharnism.mybatis.ReloadableSqlSessionFactoryBean" id="sqlSession">
	<property name="dataSource" ref="dataSource" />
	<property name="configLocation" value="classpath:/mybatis/configuration.xml" />
	<property name="mapperLocations" value="classpath:/mybatis/mapper/*-mapper.xml" />
    	<property name="reloadTargets" value="file:/home/git/doodles/src/main/resources/mybatis/mapper" />
	<property name="realoadTargetFilePattern" value="*-mapper.xml" />
</bean>
</code>
