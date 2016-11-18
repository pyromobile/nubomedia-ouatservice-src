package com.zed.nubomedia.ouatservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * {@literal} ${env} is set in command line parameter mvn ... -Denv=[dev|pre|pro].
 * 
 * @author jemalpartida
 *
 */

@Configuration
@PropertySource("classpath:db-${env}.properties")
public class DBConfig 
{
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer()
	{
		return new PropertySourcesPlaceholderConfigurer();
	}

	public String getApplicationId()
	{
		return this.applicationId;
	}
	
	public String getSecretId()
	{
		return this.secrectId;
	}
	

	/*=======================================================================*/
	/*                              Private Section                          */
	/*=======================================================================*/
	@Value("${db.application}")
	private String applicationId;

	@Value("${db.secrect}")
	private String secrectId;
}
