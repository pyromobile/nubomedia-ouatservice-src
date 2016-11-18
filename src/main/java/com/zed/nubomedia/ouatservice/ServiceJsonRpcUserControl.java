package com.zed.nubomedia.ouatservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.kurento.client.MediaElement;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.kurento.module.nubofacedetector.NuboFaceDetector;
import org.kurento.room.NotificationRoomManager;
import org.kurento.room.api.pojo.ParticipantRequest;
import org.kurento.room.api.pojo.UserParticipant;
import org.kurento.room.internal.ProtocolElements;
import org.kurento.room.rpc.JsonRpcUserControl;
import org.kurento.room.rpc.ParticipantSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import fi.vtt.nubomedia.kurento.module.armarkerdetector.ArKvpFloat;
import fi.vtt.nubomedia.kurento.module.armarkerdetector.ArKvpInteger;
import fi.vtt.nubomedia.kurento.module.armarkerdetector.ArKvpString;
import fi.vtt.nubomedia.kurento.module.armarkerdetector.ArMarkerdetector;
import fi.vtt.nubomedia.kurento.module.armarkerdetector.ArThing;
import fi.vtt.nubomedia.kurento.module.armarkerdetector.OverlayType;

public class ServiceJsonRpcUserControl extends JsonRpcUserControl //implements MediaElementAddable
{
	public ServiceJsonRpcUserControl(NotificationRoomManager roomManager)
	{
		super( roomManager );
		this.arPackObjects = new HashMap<String,HashMap<String,ARObject>>();
		this.qrObjects = new ArrayList<QRObject>();
		this.readingRoom = new HashMap<String,Book>();
		this.packs = new JsonArray();
	}

	public void setARObjects(ArrayList<ARObject> items) 
	{
		for( ARObject arObject : items )
		{
			log.info( "ARObject stored: {}", arObject );
			String pack = arObject.getPack();
			if( !this.arPackObjects.containsKey( pack ) )
			{
				HashMap<String,ARObject> arObjects = new HashMap<String,ARObject>();
				this.arPackObjects.put( pack, arObjects );
			}
			this.arPackObjects.get( pack ).put( arObject.getName(), arObject );
		}

		//Prepare configuration for clients.
		this.prepareConfiguration();
	}

	public void setQRObjects(ArrayList<QRObject> items) 
	{
		for( QRObject qrObject : items )
		{
			log.info( "QRObject stored: {}", qrObject );
			this.qrObjects.add( qrObject );
		}
	}


	/*=======================================================================*/
	/*                     Override from JsonRpcUserControl                  */
	/*=======================================================================*/
	@Override
	public void joinRoom(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest) throws IOException, InterruptedException, ExecutionException
	{
		String roomName = getStringParam(request, ProtocolElements.JOINROOM_ROOM_PARAM);

		if( this.roomManager.getRooms().contains(roomName) )
		{
			log.info( "can be a room exists already. Checking participatns in room..." );
			Set<UserParticipant> usersInRoom = this.roomManager.getParticipants( roomName );
			if( usersInRoom != null && usersInRoom.size() == 5 )
			{
				log.info( "The room is full!" );
				JsonObject msg = new JsonObject();
				msg.addProperty( "refuse", true );

				transaction.sendResponse( msg );
			}
			else
			{
				log.info( "The room not is full, you can enter in the room..." );

				super.joinRoom(transaction, request, participantRequest);
			}
		}
		else
		{
			log.info( "can be new room..." );
			super.joinRoom( transaction, request, participantRequest );
		}
	}

	@Override
	public void leaveRoom(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest)
	{
		super.leaveRoom( transaction, request, participantRequest );

		if( transaction == null )
			return;

		ParticipantSession session = getParticipantSession( transaction );
		String roomName = session.getRoomName();

		//Checks if the room exits yet.
		if( !this.roomManager.getRooms().contains( roomName ) )
		{
			//The room doesn't exist so removes the book in the room.
			if( this.readingRoom.containsKey( roomName ) )
			{
				Book book = this.readingRoom.remove( roomName );
				log.info( "book with id:{} and lang:{} removed from reading book", book.getId(), book.langId() );
			}
		}
	}

	@Override
	public void customRequest(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest)
	{
		log.info( "custonRequest..." );

		switch( this.getCustomActionType( request.getParams() ) )
		{
			case DATA_NOT_FOUND:
				log.info( "No data found in custom request!" );
				break;

			case PREPARE_BOOK:
				this.doPrepareBook( transaction, request );
				break;

			case REQUEST_BOOK:
				this.doRequestBook( transaction, request );
				break;

			case CHANGE_PAGE:
				this.doChangePage( transaction, request, participantRequest );
				break;

			case ACCESSORIES:
				this.doShowAccessories( transaction, request, participantRequest );
				break;

			case COSTUME:
				this.doShowCostume();
				break;

			case PACKS:
				this.doSendPack( transaction );
				break;
		}
	}

	@Override
	public void publishVideo(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest)
	{
		super.publishVideo( transaction, request, participantRequest );

		log.info("publishVideo....");
/*
		String pid = participantRequest.getParticipantId();

		this.arMarkerFilter = new ArMarkerdetector.Builder( roomManager.getPipeline( pid ) ).build();
		this.arMarkerFilter.setArThing( this.createArThings() );
		this.arMarkerFilter.enableTickEvents( false );
		this.arMarkerFilter.enableAugmentation( true );
		this.arMarkerFilter.setMarkerPoseFrequency( false, 1 );
		this.arMarkerFilter.setMarkerPoseFrameFrequency( false, 10 );
		this.arMarkerFilter.enableMarkerCountEvents( true );
		this.currentARMarkerId = 0;

		this.arMarkerFilter.addMarkerCountListener( new EventListener<MarkerCountEvent>()
		{
			@Override
			public void onEvent(MarkerCountEvent event)
			{
				if( event.getMarkerCount() == 1 && event.getMarkerCountDiff() == 1 )
				{
					log.info( "ARMarkerFilter - countListener event...count:{} - diff:{}", event.getMarkerCount(), event.getMarkerCountDiff() );
				}
				else if(event.getMarkerCount() == 0 && event.getMarkerCountDiff() == -1 )
				{
					long currentTime = System.currentTimeMillis();

					if( currentTime - lastTime > 0.5f )
					{
						log.info("ARMarkerFilter - countListener event se ha quitado el patrón...");

						currentARMarkerId = ( currentARMarkerId + 1 >= qrObjects.size() ) ? 0 : currentARMarkerId + 1;
						arMarkerFilter.setArThing( createArThings() );

						log.info( "ARMarkerFilter - countListener event recalculating for ID:{}", currentARMarkerId );
					}

					lastTime = System.currentTimeMillis();
				}
			}
		});
		this.arMarkerFilter.addTickListener( new EventListener<TickEvent>()
		{
			@Override
			public void onEvent(TickEvent event)
			{
				log.info( "ARMarkerFilter - tickListener event..." );
			}
		});
		this.arMarkerFilter.addMarkerPoseListener( new EventListener<MarkerPoseEvent>()
		{
			@Override
			public void onEvent( MarkerPoseEvent event )
			{
				log.info( "ARMarkerFilter - poseListener event..." );
			}
		});
		roomManager.addMediaElement( pid, this.arMarkerFilter );
		*/
	}


	/*=======================================================================*/
	/*                            Private Section                            */
	/*=======================================================================*/
	private void prepareConfiguration()
	{
		Iterator<Map.Entry<String, HashMap<String, ARObject>>> it = this.arPackObjects.entrySet().iterator();
		while( it.hasNext() )
		{
			Map.Entry<String, HashMap<String, ARObject>> arPackItem = it.next();
			String packName = arPackItem.getKey();
			if( packName == null )
				continue;

			JsonObject pack = new JsonObject();
			pack.addProperty( "pack", packName );

			JsonArray images = new JsonArray();
			for( ARObject arObject:arPackItem.getValue().values() )
			{
				images.add( new JsonPrimitive( arObject.getName() + ".png" ) );
			}
			pack.add( "images", images );
			this.packs.add( pack );
		}

		log.info( "Packs ready to be send to clients:{}", this.packs );
	}

	private CustomActionType getCustomActionType( JsonObject params )
	{
		CustomActionType action = CustomActionType.DATA_NOT_FOUND;

		if( params.has( "type" ) )
		{
			switch( params.get( "type" ).getAsInt() )
			{
				case 1:
					action = CustomActionType.PREPARE_BOOK;
					break;

				case 2:
					action = CustomActionType.REQUEST_BOOK;
					break;

				case 3:
					action = CustomActionType.CHANGE_PAGE;
					break;

				case 4:
					action = CustomActionType.ACCESSORIES;
					break;

				case 5:
					action = CustomActionType.COSTUME;
					break;

				case 6:
					action = CustomActionType.PACKS;
					break;
			}
		}

		return action;
	}

	private void doPrepareBook(Transaction transaction, Request<JsonObject> request)
	{
		ParticipantSession session = getParticipantSession( transaction );
		String roomName = session.getRoomName();

		String bookId = getStringParam( request, "bookId" );
		String langId = getStringParam( request, "langId" );
		log.info( "prepare book [id]:{} in lang:{}", bookId, langId );

		if( !this.readingRoom.containsKey( roomName ) )
		{
			log.info( "book with id:{} and lang:{} add to reading book", bookId, langId );
			this.readingRoom.put( roomName, new Book( bookId, langId ) );
		}

		try
		{
			log.info( "Send response to user with this book id:{}", bookId );

			JsonObject message = new JsonObject();
			message.addProperty( "type", 1 );
			message.add( "data", new JsonObject() );

			transaction.sendResponse( message );
		}
		catch( IOException ioe )
		{
			log.error( "Error in prepare book to send message: {}", ioe.getMessage() );
		}
	}

	private void doRequestBook(Transaction transaction, Request<JsonObject> request)
	{
		ParticipantSession session = getParticipantSession( transaction );
		String roomName = session.getRoomName();

		log.info( "request book for room:{}", roomName );

		JsonObject message = new JsonObject();
		message.addProperty( "type", 2 );
		if( !this.readingRoom.containsKey( roomName ) )
		{
			log.error( "room name:{} no exist already!", roomName );
			message.add( "data", new JsonObject() );
		}
		else
		{
			Book book = this.readingRoom.get( roomName );
			message.add( "data", book.getJson() );
		}

		try
		{
			log.info( "Send response to user in request book for room:{}", roomName );
			transaction.sendResponse( message );
		}
		catch( IOException ioe )
		{
			log.error( "Error in request book to send message: {}", ioe.getMessage() );
		}
	}

	private void doChangePage(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest)
	{
		ParticipantSession session = getParticipantSession( transaction );
		String userName = session.getParticipantName();
		String roomName = session.getRoomName();

		String action = getStringParam( request, "action" );
		if( action.equals( "prev" ) )
			this.readingRoom.get( roomName ).prevPage();
		else if( action.equals( "next" ) )
			this.readingRoom.get( roomName ).nextPage();

		JsonObject message = new JsonObject();
		message.addProperty( "type", 3 );
		message.add( "data", this.readingRoom.get( roomName ).getJson() );

		//Broadcast message.
		roomManager.sendMessage( message.toString(), userName, roomName, participantRequest  );
	}

	private void doShowAccessories(Transaction transaction, Request<JsonObject> request, ParticipantRequest participantRequest)
	{
		String accesory = request.getParams().get( "accesory" ).getAsString();
		String pack = request.getParams().get( "pack" ).getAsString();

		String pid = participantRequest.getParticipantId();

		log.info( "ShowAccesory: {} for pack:{}", accesory, pack );

		this.addCurrentFilter( pid, transaction, accesory, pack );
		try 
		{
			JsonObject message = new JsonObject();
			transaction.sendResponse( message );
		}
		catch( IOException e )
		{
			log.error( e.getMessage() );
		}
	}

	private void doShowCostume()
	{
		//TODO: por hacer.s
	}

	private void doSendPack(Transaction transaction)
	{
		log.info( "request packs..." );

		JsonObject message = new JsonObject();
		message.addProperty( "type", 6 );
		message.add( "data", this.packs );

		log.info( "Send response to user with packs..." );

		try
		{
			log.info( "Send response to user with packs..." );
			transaction.sendResponse( message );
		}
		catch( IOException ioe )
		{
			log.error( "Error in request packs to send message: {}", ioe.getMessage() );
		}
	}

	private void removeCurrentFilter(String pid, Transaction transaction)
	{
		log.info( "removing filter..." );
		roomManager.removeMediaElement( pid, (MediaElement)transaction.getSession().getAttributes().get( "filter" ) );
		transaction.getSession().getAttributes().remove( "filter" );

		//Filter box.
		roomManager.removeMediaElement( pid, (MediaElement)transaction.getSession().getAttributes().get( "boxFilter" ) );
		transaction.getSession().getAttributes().remove( "boxFilter" );
		log.info( "removed filter...ok!" );
	}

	//private Map<String,FaceFilter> filters = new HashMap<String,FaceFilter>();
	private void addCurrentFilter(String pid, Transaction transaction, String accesoryName, String pack)
	{
		log.info( "adding filter..." );
		ARObject accesory = this.arPackObjects.get( pack ).get( accesoryName );
		log.info( "Filter to add:{}", accesory );

		if( !transaction.getSession().getAttributes().containsKey( accesoryName ) )
		{
			NuboFaceDetector face = new NuboFaceDetector.Builder( roomManager.getPipeline( pid ) ).build();
			face.activateServerEvents( 1, 3000 );
			face.sendMetaData( 1 );
			face.showFaces( 1 );
			face.multiScaleFactor( 25 );
			face.widthToProcess( 480 );

			face.setOverlayedImage( accesory.getUrl(), 
					accesory.getCoords().getOffsetX(),
					accesory.getCoords().getOffsetY(),
					accesory.getCoords().getWidth(),
					accesory.getCoords().getHeight());

			roomManager.addMediaElement( pid, face );

			transaction.getSession().getAttributes().put( accesoryName, face );
		}
		else
		{
			NuboFaceDetector face = (NuboFaceDetector)transaction.getSession().getAttributes().remove( accesoryName );
			roomManager.removeMediaElement( pid, face );
		}

		log.info( "added filter...ok" );
	}

	private List<ArThing> createArThings()
	{
		ArrayList<ArThing> arThings = new ArrayList<ArThing>();
		QRObject qrObject = this.qrObjects.get( this.currentARMarkerId );
		OverlayType augmentableType;
		switch( qrObject.getType() )
		{
			case "2D":
				augmentableType = OverlayType.TYPE2D;
				break;

			case "3D":
				augmentableType = OverlayType.TYPE3D;
				break;

			default:
				throw new RuntimeException( "Bizarre OverlayType: " + qrObject.getType() );
		}

		List<ArKvpString> strings = new ArrayList<ArKvpString>();
		List<ArKvpFloat> floats = new ArrayList<ArKvpFloat>();
		List<ArKvpInteger> integers = new ArrayList<ArKvpInteger>();

		this.createKVPs( qrObject.toJson(), strings, integers, floats );

		ArThing arThing = new ArThing( qrObject.getId(), augmentableType, strings, integers, floats );
		arThings.add(arThing);

		return arThings;
	}

	private void createKVPs( JsonObject json, List<ArKvpString> strings, List<ArKvpInteger> integers, List<ArKvpFloat> floats )
	{
		for( String kvpId : new String[]{"strings", "ints", "floats"} )
		{
			JsonElement kvp = json.get(kvpId);
			if( kvp == null )
			{
				continue;
			}

			Iterator<JsonElement> itr = kvp.getAsJsonArray().iterator();
			while( itr.hasNext() )
			{
				JsonElement jsonElm = itr.next();
				Set<Map.Entry<String, JsonElement>> pairs = jsonElm.getAsJsonObject().entrySet();
				for( Map.Entry<String, JsonElement> map : pairs )
				{
					switch( kvpId )
					{
						case "strings":
							strings.add( new ArKvpString( map.getKey(), ( !map.getValue().isJsonNull() ) ? map.getValue().getAsString() : "" ) );
							break;

						case "ints":
							integers.add( new ArKvpInteger( map.getKey(), map.getValue().getAsInt() ) );
							break;

						case "floats":
							floats.add( new ArKvpFloat( map.getKey(), map.getValue().getAsFloat() ) );
							break;
					}
				}
			}
		}
	}


	private static final Logger log = LoggerFactory.getLogger( ServiceJsonRpcUserControl.class );
	private Map<String, HashMap<String, ARObject>> arPackObjects;
	private ArrayList<QRObject> qrObjects;
	private int currentARMarkerId = 0;
	
	private enum CustomActionType {
		DATA_NOT_FOUND,
		PREPARE_BOOK,
		REQUEST_BOOK,
		CHANGE_PAGE,
		ACCESSORIES,
		COSTUME,
		PACKS
	}
	private Map<String,Book> readingRoom;
	private JsonArray packs;
	private ArMarkerdetector arMarkerFilter;
}

class FilterInfo
{
	public String accesoryName;
	public MediaElement mediaElement;
}
