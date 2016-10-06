/*
 * Dog - Environment REST Endpoint
 * 
 * Copyright (c) 2013-2014 Luigi De Russis and Teodoro Montanaro
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package it.polito.elite.dog.communication.rest.environment;

import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi;
import it.polito.elite.dog.core.housemodel.api.EnvironmentModel;
import it.polito.elite.dog.core.library.jaxb.Building;
import it.polito.elite.dog.core.library.jaxb.BuildingEnvironment;
import it.polito.elite.dog.core.library.jaxb.DogHomeConfiguration;
import it.polito.elite.dog.core.library.jaxb.Flat;
import it.polito.elite.dog.core.library.jaxb.ObjectFactory;
import it.polito.elite.dog.core.library.jaxb.Room;
import it.polito.elite.dog.core.library.util.LogHelper;

/**
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @see <a href="http://elite.polito.it">http://elite.polito.it</a>
 * 
 */
@Path("/api/v1/environment/")
public class EnvironmentRESTEndpoint implements EnvironmentRESTApi
{
	// the service logger
	private LogHelper logger;

	// the bundle context reference to extract information on the entire Dog
	// status
	private BundleContext context;

	// reference for the HouseModel
	private AtomicReference<EnvironmentModel> environmentModel;

	// the instance-level mapper
	private ObjectMapper mapper;

	// the JAXB context
	private JAXBContext jaxbContext;

	/**
	 * Constructor
	 */
	public EnvironmentRESTEndpoint()
	{
		// init the house model atomic reference
		this.environmentModel = new AtomicReference<EnvironmentModel>();

		// init JAXB Context
		try
		{
			this.jaxbContext = JAXBContext.newInstance(
					DogHomeConfiguration.class.getPackage().getName());
		}
		catch (JAXBException e)
		{
			this.logger.log(LogService.LOG_ERROR, "JAXB Init Error", e);
		}

		// initialize the instance-wide object mapper
		this.mapper = new ObjectMapper();
		// set the mapper pretty printing
		this.mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
		// avoid empty arrays and null values
		this.mapper.configure(
				SerializationConfig.Feature.WRITE_EMPTY_JSON_ARRAYS, false);
		this.mapper.setSerializationInclusion(Inclusion.NON_NULL);

		// create an introspector for parsing both Jackson and JAXB annotations
		AnnotationIntrospector jackson = new JacksonAnnotationIntrospector();
		AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
		AnnotationIntrospector fullIntrospector = new AnnotationIntrospector.Pair(
				jackson, jaxb);
		// make deserializer use both Jackson and JAXB annotations
		this.mapper.getDeserializationConfig()
				.withAnnotationIntrospector(fullIntrospector);
		// make serializer use both Jackson and JAXB annotations
		this.mapper.getSerializationConfig()
				.withAnnotationIntrospector(fullIntrospector);
	}

	/**
	 * Bundle activation, stores a reference to the context object passed by the
	 * framework to get access to system data, e.g., installed bundles, etc.
	 * 
	 * @param context
	 */
	public void activate(BundleContext context)
	{
		// store the bundle context
		this.context = context;

		// init the logger with a null logger
		this.logger = new LogHelper(this.context);

		// log the activation
		this.logger.log(LogService.LOG_INFO, "Activated....");
	}

	/**
	 * Prepare the bundle to be deactivated...
	 */
	public void deactivate()
	{
		// null the context
		this.context = null;

		// log deactivation
		this.logger.log(LogService.LOG_INFO, "Deactivated...");

		// null the logger
		this.logger = null;
	}

	/**
	 * Bind the EnvironmentModel service (before the bundle activation)
	 * 
	 * @param environmentModel
	 *            the EnvironmentModel service to add
	 */
	public void addedEnvironmentModel(EnvironmentModel environmentModel)
	{
		// store a reference to the HouseModel service
		this.environmentModel.set(environmentModel);
	}

	/**
	 * Unbind the EnvironmentModel service
	 * 
	 * @param environmentModel
	 *            the EnvironmentModel service to remove
	 */
	public void removedEnvironmentModel(EnvironmentModel environmentModel)
	{
		this.environmentModel.compareAndSet(environmentModel, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #getSVGPlan()
	 */
	@Override
	public Response getSVGPlan()
	{
		Response response = Response.status(Response.Status.NOT_FOUND).header("Access-Control-Allow-Origin", "*").build();
		// check if the EnvironmentModel service is available
		if (this.environmentModel.get() != null)
		{
			String svgPlan = this.environmentModel.get().getSVGPlan();
			response = Response.ok(svgPlan,"image/svg+xml").header("Access-Control-Allow-Origin", "*").build();
		}
		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #getBuildingInJson()
	 */
	@Override
	public String getBuildingInJson(HttpServletResponse httpResponse)
	{
		String environmentJSON = "";

		this.setCORSSupport(httpResponse);

		// get the JAXB object containing all the building information
		DogHomeConfiguration dhc = this.getBuildingFromModel();

		try
		{
			// create the response string in JSON format
			environmentJSON = this.mapper
					.writeValueAsString(dhc.getBuildingEnvironment().get(0));
		}
		catch (Exception e)
		{
			this.logger.log(LogService.LOG_ERROR,
					"Error in creating the JSON representing the entire building environment",
					e);
		}

		// if no buildings are available, send a 404 Not found HTTP response
		// assume only one building environment in the configuration, as before
		boolean noBuilding = dhc.getBuildingEnvironment().get(0).getBuilding()
				.isEmpty();

		if (environmentJSON.isEmpty() || noBuilding)
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		else
		{
			return environmentJSON;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #getBuildingInXml()
	 */
	@Override
	public String getBuildingInXml(HttpServletResponse httpResponse)
	{
		String environmentXML = "";

		this.setCORSSupport(httpResponse);

		// get the JAXB object containing all the configured buildings
		DogHomeConfiguration dhc = this.getBuildingFromModel();

		// create the XML for replying the request
		environmentXML = this.generateXML(dhc);

		// if no buildings are available, send a 404 Not found HTTP response
		// assume only one building environment in the configuration, as before
		boolean noBuilding = dhc.getBuildingEnvironment().get(0).getBuilding()
				.isEmpty();

		if (environmentXML.isEmpty() || noBuilding)
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		else
		{
			return environmentXML;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #getFlatsInJson()
	 */
	@Override
	public String getFlatsInJson(HttpServletResponse httpResponse)
	{
		String flatsJSON = "";

		this.setCORSSupport(httpResponse);

		// get the JAXB object containing all the information about flats
		Building building = this.getFlatsFromModel();

		// if building is null, send a 404 Not found
		//
		if (building == null)
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}

		try
		{
			// create the response string in JSON format
			flatsJSON = this.mapper.writeValueAsString(building);
		}
		catch (Exception e)
		{
			this.logger.log(LogService.LOG_ERROR,
					"Error in creating the JSON representing all the flats", e);
		}

		// no flat
		if (flatsJSON.isEmpty() || building.getFlat().isEmpty())
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		else
			return flatsJSON;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #addNewFlat(java.lang.String)
	 */
	@Override
	public Response addNewFlat(String addedFlat,
			HttpServletResponse httpResponse)
	{
		// set and init the variable used to store the HTTP response that will
		// be sent by exception to the client
		Status response = Response.Status.EXPECTATION_FAILED;

		try
		{
			// create the JAXB object from the JSON representing the flat to add
			Flat flat = this.mapper.readValue(addedFlat, Flat.class);

			if (this.environmentModel.get() != null)
			{
				// add the new flat to the model
				this.environmentModel.get().addFlatToBuilding(flat);

				// set the variable used to store the HTTP response by the right
				// value
				// CREATED: the flat was added successfully
				response = Response.Status.CREATED;
			}
		}
		catch (Exception e)
		{
			// exception
			this.logger.log(LogService.LOG_ERROR,
					"Impossible to add a new flat", e);

			// set the variable used to store the HTTP response by the right
			// value
			// NOT_MODIFIED: the flat was not added
			// it was the best response status available
			response = Response.Status.NOT_MODIFIED;
		}

		if (response != Response.Status.CREATED)
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(response);
		}

		return Response.ok().header("Access-Control-Allow-Origin", "*").build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #getFlat(java.lang.String)
	 */
	@Override
	public String getFlat(String flatId, HttpServletResponse httpResponse)
	{
		String flatJSON = "";

		this.setCORSSupport(httpResponse);

		// get the JAXB object containing all the information about the desired
		// flat
		Flat flat = this.getFlatFromModel(flatId);

		// no flat, 404 response
		if (flat == null)
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		try
		{
			// create the response string in JSON format
			flatJSON = this.mapper.writeValueAsString(flat);
		}
		catch (Exception e)
		{
			this.logger.log(LogService.LOG_ERROR,
					"Error in creating the JSON representing the flat named "
							+ flatId,
					e);
		}

		// the desired flat or its rooms don't exist
		if (flatJSON.isEmpty() || flat.getRoom().isEmpty())
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		else
			return flatJSON;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #updateFlat(java.lang.String, java.lang.String)
	 */
	@Override
	public Response updateFlat(String flatId, String updatedFlat,
			HttpServletResponse httpResponse)
	{
		// set and init the variable used to store the HTTP response that will
		// be sent by exception to the client
		Status response = Response.Status.EXPECTATION_FAILED;

		// check if the flat exists
		Flat flat = this.getFlatFromModel(flatId);

		try
		{
			// create the JAXB object from the JSON representing the flat
			// to update
			Flat flatToUpdate = this.mapper.readValue(updatedFlat, Flat.class);

			// check if the updated flat is the one declared
			if ((flat != null) && (flatToUpdate.getId().equals(flatId)))
			{
				// put the new properties in the existing flat
				if (this.environmentModel.get() != null)
				{
					// update the model with the new flat
					this.environmentModel.get()
							.updateBuildingConfiguration(flatToUpdate);

					// set the variable used to store the HTTP response by the
					// right value
					// OK: the flat was updated
					response = Response.Status.OK;
				}
			}
			else
			{
				this.logger.log(LogService.LOG_ERROR,
						"Impossible to update the flat named " + flatId
								+ " since it does not exists!");

				// set the variable used to store the HTTP response by the right
				// value
				// PRECONDITION_FAILED: impossible to update the flat
				// since it does not exists
				// it was the best response status available
				response = Response.Status.PRECONDITION_FAILED;
			}
		}
		catch (Exception e)
		{
			// exception
			this.logger.log(LogService.LOG_ERROR,
					"Impossible to update the flat named " + flatId, e);

			// set the variable used to store the HTTP response by the right
			// value
			// NOT_MODIFIED: impossible to update the flat
			response = Response.Status.NOT_MODIFIED;
		}

		if (response != Response.Status.OK)
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(response);
		}

		return Response.ok().header("Access-Control-Allow-Origin", "*").build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #getRoomsInFlat(java.lang.String)
	 */
	@Override
	public String getRoomsInFlat(String flatId,
			HttpServletResponse httpResponse)
	{
		String roomsJSON = "";

		this.setCORSSupport(httpResponse);

		// get the JAXB object containing all the information about the desired
		// flat
		Flat flat = this.getFlatFromModel(flatId);

		// no flat, 404
		if (flat == null)
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}

		// remove everything but the room list from the retrieved flat
		flat.setClazz(null);
		flat.setDescription(null);
		flat.setId(null);
		flat.setSvgfootprint(null);

		try
		{
			// create the response string in JSON format
			roomsJSON = this.mapper.writeValueAsString(flat);
		}
		catch (Exception e)
		{
			this.logger.log(LogService.LOG_ERROR,
					"Error in creating the JSON representing the rooms present in the flat named "
							+ flatId,
					e);
		}

		// no rooms (or empty rooms): 404
		if (roomsJSON.isEmpty() || flat.getRoom().isEmpty())
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		else
			return roomsJSON;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #addNewRoomInFlat(java.lang.String, java.lang.String)
	 */
	@Override
	public Response addNewRoomInFlat(String flatId, String addedRoom,
			HttpServletResponse httpResponse)
	{
		// set and init the variable used to store the HTTP response that will
		// be sent by exception to the client
		Status response = Response.Status.EXPECTATION_FAILED;

		try
		{
			// create the JAXB object from the JSON representing the room to add
			Room room = this.mapper.readValue(addedRoom, Room.class);

			if (this.environmentModel.get() != null)
			{
				// add the new flat to the model
				this.environmentModel.get().addRoomToBuilding(room, flatId);

				// set the variable used to store the HTTP response by the right
				// value
				// CREATED: the room was successfully added
				response = Response.Status.CREATED;
			}
		}
		catch (Exception e)
		{
			// exception
			this.logger.log(LogService.LOG_ERROR,
					"Impossible to add a new room to the flat named " + flatId,
					e);

			// set the variable used to store the HTTP response by the right
			// value
			// NOT_MODIFIED: impossible to add a new room to the flat
			response = Response.Status.NOT_MODIFIED;
		}

		if (response != Response.Status.CREATED)
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(response);
		}

		return Response.ok().header("Access-Control-Allow-Origin", "*").build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #getSingleRoomInFlat(java.lang.String, java.lang.String)
	 */
	@Override
	public String getSingleRoomInFlat(String flatId, String roomId,
			HttpServletResponse httpResponse)
	{
		String roomsJSON = "";

		this.setCORSSupport(httpResponse);

		// get the JAXB object containing all the information about the desired
		// room
		Room room = this.getRoomFromModel(flatId, roomId);

		// no room, 404
		if (room == null)
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		try
		{
			// create the response string in JSON format
			roomsJSON = this.mapper.writeValueAsString(room);
		}
		catch (Exception e)
		{
			this.logger.log(LogService.LOG_ERROR,
					"Error in creating the JSON representing the room named "
							+ roomId,
					e);
		}

		// empty room, send a 404
		if (roomsJSON.isEmpty())
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
		else
			return roomsJSON;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #updateRoomInFlat(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Response updateRoomInFlat(String flatId, String roomId,
			String updatedRoom, HttpServletResponse httpResponse)
	{
		// set and init the variable used to store the HTTP response that will
		// be sent by exception to the client
		Status response = Response.Status.EXPECTATION_FAILED;

		// get the room to check if it exists
		Room room = this.getRoomFromModel(flatId, roomId);

		try
		{
			// create the JAXB object from the JSON representing the room
			// to update
			Room roomToUpdate = this.mapper.readValue(updatedRoom, Room.class);

			// check if the update is possible
			if ((room != null) && (roomToUpdate.getId().equals(roomId)))
			{
				if (this.environmentModel.get() != null)
				{
					// update the model with the new room
					this.environmentModel.get()
							.updateBuildingConfiguration(roomToUpdate, flatId);

					// set the variable used to store the HTTP response by the
					// right value
					// OK: the room was successfully updated
					response = Response.Status.OK;
				}
			}
			else
			{
				this.logger.log(LogService.LOG_ERROR,
						"Impossible to update the room named " + roomId
								+ " since it does not exists!");

				// set the variable used to store the HTTP response by the right
				// value
				// PRECONDITION_FAILED: impossible to update the room since it
				// does not exists
				// it was the best response status available
				response = Response.Status.PRECONDITION_FAILED;
			}
		}
		catch (Exception e)
		{
			// exception
			this.logger.log(LogService.LOG_ERROR,
					"Impossible to update the room named " + roomId, e);

			// set the variable used to store the HTTP response by the right
			// value
			// NOT_MODIFIED: impossible to update the room
			response = Response.Status.NOT_MODIFIED;
		}

		if (response != Response.Status.OK)
		{
			// launch the exception responsible for sending the HTTP response
			throw new WebApplicationException(response);
		}

		return Response.ok().header("Access-Control-Allow-Origin", "*").build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #removeRoomFromFlat(java.lang.String, java.lang.String)
	 */
	@Override
	public void removeRoomFromFlat(String roomId, String flatId,
			HttpServletResponse httpResponse)
	{
		this.setCORSSupport(httpResponse);
		if (this.environmentModel.get() != null)
		{
			// remove the given room from the Environmental Model
			this.environmentModel.get().removeRoomFromBuilding(roomId, flatId);

			// take care of the action - success
			throw new WebApplicationException(Response.Status.NO_CONTENT);
		}
		else
		{
			// no environment model available, the request cannot be executed
			// it was the best response status available
			throw new WebApplicationException(
					Response.Status.PRECONDITION_FAILED);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.communication.rest.environment.api.EnvironmentRESTApi
	 * #removeFlat(java.lang.String)
	 */
	@Override
	public void removeFlat(String flatId, HttpServletResponse httpResponse)
	{
		this.setCORSSupport(httpResponse);
		if (this.environmentModel.get() != null)
		{
			// remove the given flat from the Environmental Model
			this.environmentModel.get().removeFlatFromBuilding(flatId);

			// take care of the action - success
			throw new WebApplicationException(Response.Status.NO_CONTENT);
		}
		else
		{
			// no environment model available, the request cannot be executed
			// it was the best response status available
			throw new WebApplicationException(
					Response.Status.PRECONDITION_FAILED);
		}
	}

	/**
	 * Get all the building environments configured in Dog from the
	 * {@link EnvironmentModel}
	 * 
	 * @return a {@link DogHomeConfiguration} object with all the building
	 *         information
	 */
	private DogHomeConfiguration getBuildingFromModel()
	{
		// create a JAXB Object Factory for adding the proper header...
		ObjectFactory factory = new ObjectFactory();
		DogHomeConfiguration dhc = factory.createDogHomeConfiguration();

		// check if the EnvironmentModel service is available
		if (this.environmentModel.get() != null)
		{
			// get all the building environment from the HouseModel
			BuildingEnvironment buildingEnv = this.environmentModel.get()
					.getBuildingEnvironment().get(0);

			dhc.getBuildingEnvironment().add(buildingEnv);
		}

		return dhc;
	}

	/**
	 * Get all the flats present in the first building of the configuration
	 * 
	 * @return a {@Building} JAXB object containing only its flats
	 */
	private Building getFlatsFromModel()
	{
		// create a JAXB Object Factory for adding the proper header...
		ObjectFactory factory = new ObjectFactory();
		Building building = factory.createBuilding();

		// check if the EnvironmentModel service is available
		if (this.environmentModel.get() != null)
		{
			// get all the flats for the first building
			List<Flat> flats = this.environmentModel.get()
					.getBuildingEnvironment().get(0).getBuilding().get(0)
					.getFlat();

			// build the flats container
			building.getFlat().addAll(flats);
		}

		return building;
	}

	/**
	 * Get a desired flat from the {@link EnvironmentModel}.
	 * 
	 * @param flatId
	 *            the unique flat identifier
	 * @return a {@Flat} JAXB object representing the desired flat
	 */
	private Flat getFlatFromModel(String flatId)
	{
		// init
		Flat flat = null;

		// check if the EnvironmentModel service is available
		if (this.environmentModel.get() != null)
		{
			// get all the flats for the first building
			List<Flat> flats = this.environmentModel.get().getJAXBEnvironment()
					.get(0).getBuilding().get(0).getFlat();

			// look for the desired flat
			for (Flat singleFlat : flats)
			{
				if (singleFlat.getId().equals(flatId))
				{
					// found: store the flat
					flat = singleFlat;
				}
			}
		}

		return flat;
	}

	/**
	 * Get a desired room in a given flat from the {@link EnvironmentModel}.
	 * 
	 * @param flatId
	 *            the given flat unique identifier
	 * @param roomId
	 *            the desired room unique identifier
	 * @return a {@Room} JAXB object representing the requested room info
	 */
	private Room getRoomFromModel(String flatId, String roomId)
	{
		// init
		Room room = null;

		// check if the EnvironmentModel service is available
		if (this.environmentModel.get() != null)
		{
			// get all the flats for the first building
			List<Flat> flats = this.environmentModel.get()
					.getBuildingEnvironment().get(0).getBuilding().get(0)
					.getFlat();

			// look for the desired room in the given flat
			for (Flat singleFlat : flats)
			{
				if (singleFlat.getId().equals(flatId))
				{
					for (Room singleRoom : singleFlat.getRoom())
					{
						if (singleRoom.getId().equals(roomId))
						{
							// found: store the room
							room = singleRoom;
						}
					}
				}
			}
		}

		return room;
	}

	/**
	 * Generate the XML to be sent
	 * 
	 * @param dhc
	 *            the {@link DogHomeConfiguration} object to marshall
	 * @return the corresponding XML
	 */
	private String generateXML(DogHomeConfiguration dhc)
	{
		String environmentXML = "";

		if (this.jaxbContext != null)
		{
			try
			{
				StringWriter output = new StringWriter();

				// marshall the DogHomeConfiguration...
				Marshaller marshaller = jaxbContext.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
						Boolean.TRUE);

				marshaller.marshal(dhc, output);

				environmentXML = output.getBuffer().toString();
			}
			catch (JAXBException e)
			{
				// the exception can be throw by the JAXB.marshal method...
				this.logger.log(LogService.LOG_ERROR,
						"Exception in JAXB Marshalling...", e);
			}
		}

		return environmentXML;
	}

	private void setCORSSupport(HttpServletResponse response)
	{
		response.addHeader("Access-Control-Allow-Origin", "*");
	}
}
