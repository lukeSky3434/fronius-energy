/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package luke.sky;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author pendl2
 */
public class TestClient
{
	@Test
	void testApiCall() throws Exception
	{
		Client client = ClientBuilder.newClient();
		client.property(ClientProperties.CONNECT_TIMEOUT, 2000);
		client.property(ClientProperties.READ_TIMEOUT, 2000);

		URI hostUri = UriBuilder.fromUri("http://www.google.com").build();

		WebTarget webTarget = client
			.target(UriBuilder.fromUri(hostUri)
				.build());

		try (Response response = webTarget
				.request()
				.get()) {
				response.readEntity(String.class);
				Assertions.assertTrue(true);
		}
	}
}
