package luke.sky.fronius.energy.reader;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author pendl2
 */
public class RealTimeDataReader implements Runnable
{
	private static final String PATH = "solar_api/v1/GetInverterRealtimeData.cgi";
	private static final String HOST = "192.168.8.101";
	private final Client client;
	private final WebTarget webTarget;
  private static final Logger LOG = LogManager.getLogger(RealTimeDataReader.class);

	public RealTimeDataReader()
	{
		client = ClientBuilder.newClient();
		URI host = UriBuilder.fromUri("http://" + HOST + "/").build();

		webTarget = client
			.target(UriBuilder.fromUri(host)
				.path(PATH)
				.queryParam("Scope", "System")
				.queryParam("DataCollection", "CumulationInverterData")
				.build());

		Configurator.initialize(new DefaultConfiguration());
		Configurator.setRootLevel(Level.DEBUG);
	}


	public static void main(String[] args) throws Exception
	{
		Thread th = new Thread(new RealTimeDataReader());
		th.setName("ReaderThread");
		th.start();
	}

	private static List<RealTimeData> parseData(JSONObject obj, final String name)
	{
		List<RealTimeData> l = new ArrayList<>();
		JSONObject values = obj.getJSONObject("Values");
		values.keySet().forEach((k) -> {
			RealTimeData d = new RealTimeData();
			d.device = k;
			d.value = values.getLong(k);
			d.unit = obj.getString("Unit");
			d.name = name;
			l.add(d);
		});
		return l;
	}

	@Override
	public void run()
	{
		while (true) {
			try (Response response = webTarget
				.request()
				.get()) {
				String entity = response.readEntity(String.class);
				JSONObject obj = new JSONObject(entity);
				JSONObject pac = obj.getJSONObject("Body").getJSONObject("Data").getJSONObject("PAC");

				try {
					LOG.info(parseData(pac, "PAC"));
					JSONObject dEnergy = obj.getJSONObject("Body").getJSONObject("Data").getJSONObject("DAY_ENERGY");
					LOG.info(parseData(dEnergy, "DAY_ENERGY"));
				}
				catch (JSONException x) {
					LOG.error(x);
				}
			}
			try {
				LOG.info("sleep 10 seconds ...");

				Thread.sleep(10000L);
			}
			catch (Exception e) {
				LOG.error("error while sleeping", e);
			}
		}
	}
}
