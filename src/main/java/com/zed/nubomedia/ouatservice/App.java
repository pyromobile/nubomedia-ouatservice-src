package com.zed.nubomedia.ouatservice;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.kurento.room.KurentoRoomServerApp;
import org.kurento.room.rpc.JsonRpcUserControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import com.google.gson.GsonBuilder;


/**
 * Entry point.
 *
 */
@ComponentScan
@EnableAutoConfiguration
@Import(KurentoRoomServerApp.class)
public class App extends SpringBootServletInitializer
{
	@Autowired 
	KurentoRoomServerApp krs;
	@Autowired
	DBConfig db;

	@Bean
	public JsonRpcUserControl userControl() 
	{
		log.info( "preparing configuration for ServiceJsonRpcUserControl..." );

		ServiceJsonRpcUserControl userCtrl = new ServiceJsonRpcUserControl( krs.roomManager() );

		//get ar objects from kuasars.
		ArrayList<ARObject> arItems = this.getARObjects();
		if( arItems != null )
		{
			log.info( "setting AR Objects to ServiceJsonRpcUserControl...ok" );
			userCtrl.setARObjects( arItems );
		}
		else
			log.error( "imposible get ARObjects from Kuasars!" );

		//get qr objects from kuasars.
		ArrayList<QRObject> qrItems = this.getQRObjects();
		if( qrItems != null )
		{
			log.info( "setting QR Objects to ServiceJsonRpcUserControl...ok" );
			userCtrl.setQRObjects( qrItems );
		}
		else
			log.error( "imposible get QRObjects from Kuasars!" );

		return userCtrl;
	}

	public static void main( String[] args )
	{
		//ConfigFileManager.loadConfigFile( "ouatservice.conf.json" );
		ApplicationContext ctx = SpringApplication.run( App.class, args );

		String[] beanNames = ctx.getBeanDefinitionNames();
		Arrays.sort( beanNames );
		for (String beanName : beanNames)
		{
			log.info( beanName );
		}
	}


	/*=======================================================================*/
	/*                            Private Section                            */
	/*=======================================================================*/
	private ArrayList<ARObject> getARObjects()
	{
		log.info( "Getting Overlay Objects for application: {}", this.db.getApplicationId() );

		ArrayList<ARObject> arItems = new ArrayList<ARObject>();

		String url = "https://api.kuasars.com/v1/entities/overprints/queryTwo";

		try
		{
			boolean hasData = true;
			int skip = 0;
			while( hasData )
			{
				URL oURL = new URL( url );
				HttpURLConnection con = (HttpURLConnection) oURL.openConnection();

				con.setRequestMethod("POST");

				//add request header
				con.setRequestProperty( "Content-Type", "application/json" );
				con.setRequestProperty( "Kuasars-App-Id", this.db.getApplicationId() );
				con.setRequestProperty( "Kuasars-Secret-Key", this.db.getSecretId() ) ;

				String postData = "{\"where\":\"\",\"skip\":"+20*skip+"}";
				con.setRequestProperty( "Content-Length", Integer.toString( postData.getBytes().length ) );

				// Send post request
				con.setDoOutput( true );
				DataOutputStream wr = new DataOutputStream( con.getOutputStream() );
				wr.write( postData.getBytes() );
				wr.flush();
				wr.close();

				int responseCode = con.getResponseCode();
				log.info( "Sending 'POST' request to URL : {}", url );
				log.info( "Response Code : {}", responseCode );

				if( responseCode == 200 )
				{
					Reader reader =  new InputStreamReader( con.getInputStream() );
					ARObject[] items = new GsonBuilder().create().fromJson(reader, ARObject[].class);
					for( ARObject arObject : items )
					{
						arItems.add( arObject );
					}
					skip++;
				}
				else //responseCode = 204 or other value.
				{
					hasData = false;
				}
			}
		}
		catch( MalformedURLException e )
		{
			arItems = null;
			log.error( e.getMessage() );
		}
		catch (IOException e)
		{
			arItems = null;
			log.error( e.getMessage() );
		}

		return arItems;
	}

	private ArrayList<QRObject> getQRObjects()
	{
		log.info( "Getting QR Objects for application: {}", this.db.getApplicationId() );

		ArrayList<QRObject> qrItems = new ArrayList<QRObject>();

		String url = "https://api.kuasars.com/v1/entities/qrobjects/queryTwo";

		try
		{
			boolean hasData = true;
			int skip = 0;
			while( hasData )
			{
				URL oURL = new URL( url );
				HttpURLConnection con = (HttpURLConnection) oURL.openConnection();

				con.setRequestMethod( "POST" );

				//add request header
				con.setRequestProperty( "Content-Type", "application/json" );
				con.setRequestProperty( "Kuasars-App-Id", this.db.getApplicationId() );
				con.setRequestProperty( "Kuasars-Secret-Key", this.db.getSecretId() );

				String postData = "{\"where\":\"\",\"skip\":"+20*skip+"}";
				con.setRequestProperty( "Content-Length", Integer.toString( postData.getBytes().length ) );

				// Send post request
				con.setDoOutput( true );
				DataOutputStream wr = new DataOutputStream( con.getOutputStream() );
				wr.write( postData.getBytes() );
				wr.flush();
				wr.close();

				int responseCode = con.getResponseCode();
				log.info( "Sending 'POST' request to URL : {}", url );
				log.info( "Response Code : {}", responseCode );

				if( responseCode == 200 )
				{
					Reader reader =  new InputStreamReader( con.getInputStream() );
					QRObject[] items = new GsonBuilder().create().fromJson( reader, QRObject[].class );
					for( QRObject qrObject : items )
					{
						qrItems.add( qrObject );
					}
					skip++;
				}
				else //responseCode = 204 or other value.
				{
					hasData = false;
				}
			}
		}
		catch( MalformedURLException e )
		{
			qrItems = null;
			log.error( e.getMessage() );
		}
		catch( IOException e )
		{
			qrItems = null;
			log.error( e.getMessage() );
		}

		return qrItems;
	}


	private static final Logger log = LoggerFactory.getLogger( App.class );
}
