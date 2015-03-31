package it.unica.tcs.client;

import it.unica.tcs.QueryPacket;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public class JerseyClient {

	public static void main(String[] args) {
		try {

			QueryPacket st = new QueryPacket(); // ("Pippo", "Franco");

			ClientConfig clientConfig = new DefaultClientConfig();

			clientConfig.getFeatures().put(
					JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

			Client client = Client.create(clientConfig);

			WebResource webResource = client
					.resource("http://co2.unica.it:8080/middleware/api/compliance/areCompliant");

			ClientResponse response = webResource.accept("application/json").type("application/json").post(ClientResponse.class, st);

			if (response.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ response.getStatus());
			}

			QueryPacket output = response.getEntity(QueryPacket.class);

			//System.out.println(output.getUsername() + "\n");

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

}
