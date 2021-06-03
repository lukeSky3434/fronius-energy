package luke.sky.fronius.energy.reader;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.ws.rs.ProcessingException;
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
import org.glassfish.jersey.client.ClientProperties;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author pendl2
 */
public class RealTimeDataReader implements Runnable
{
	private static final String PATH = "solar_api/v1/GetInverterRealtimeData.cgi";
	private static final String SHELLY_PATH = "relay/0";
	private static final String ENV_HOST = "FRONIUS.HOST";
	private static final String ENV_SHELLY_HOST = "SHELLY.PLUGS";
	private static final String ENV_THRESHOLD = "POWER.THRESHOLD";
	private static final String ENV_LOG_LEVEL = "LOG.LEVEL";
	private static final String ENV_SHELLY_MANAGER = "SHELLY.PLUGS.MANAGING";

	private String host;
	private String shellyHost;
	private Long powerThreshold;
	private boolean shellyManaging;

	private final Client client;
	private final WebTarget webTarget;
	private static final Logger LOG = LogManager.getLogger(RealTimeDataReader.class);

	private void initalizeConfiguration()
	{
		host = System.getenv(ENV_HOST);
		if (host == null || host.isEmpty()) {
			host = "fronius";
		}

		String tmp = System.getenv(ENV_SHELLY_MANAGER);
		if (tmp == null || tmp.isEmpty()) {
			shellyManaging = false;
		}
		else {
			shellyManaging = "true".equalsIgnoreCase(tmp);
		}

		shellyHost = System.getenv(ENV_SHELLY_HOST);
		if (shellyHost == null || shellyHost.isEmpty()) {
			shellyHost = "shelly-plug-s-1";
		}

		try {
			powerThreshold = Long.parseLong(System.getenv(ENV_THRESHOLD));
		}
		catch (NumberFormatException x) {
			LOG.warn("<{}> is not a number, taking default value 3500", System.getenv(ENV_THRESHOLD));
		}
		if (powerThreshold == null) {
			powerThreshold = 3500L;
		}

		LOG.info("fronius host is set to <{}>, shelly plug-s host is set to <{}>, threshold is set to <{}> watt", host, shellyHost, powerThreshold);
	}

	public RealTimeDataReader()
	{
		Configurator.initialize(new DefaultConfiguration());
		String level = System.getenv(ENV_LOG_LEVEL);
		Level ll = Level.getLevel(level == null ? Level.INFO.name() : level);
		Configurator.setRootLevel(ll);

		client = ClientBuilder.newClient();
    client.property(ClientProperties.CONNECT_TIMEOUT, 2000);
    client.property(ClientProperties.READ_TIMEOUT,    2000);

		initalizeConfiguration();

		LOG.info("initialization done");

		URI hostUri = UriBuilder.fromUri("http://" + host + "/").build();

		webTarget = client
			.target(UriBuilder.fromUri(hostUri)
				.path(PATH)
				.queryParam("Scope", "System")
				.queryParam("DataCollection", "CumulationInverterData")
				.build());
	}

	private boolean isShellyOn() throws ProcessingException
	{
		URI shellyUri = UriBuilder.fromUri("http://" + shellyHost + "/").build();
		WebTarget wt = client
			.target(UriBuilder.fromUri(shellyUri)
				.path(SHELLY_PATH)
				.build());

		if (LOG.isDebugEnabled()) {
			LOG.debug("request => {}", wt.getUri());
		}

		try (Response response = wt.request().get()) {
				String entity = response.readEntity(String.class);

				if (LOG.isDebugEnabled()) {
					LOG.debug("response => {}", entity);
				}

				JSONObject obj = new JSONObject(entity);
				return obj.getBoolean("ison");
		}
	}

	private void switchShelly(final boolean on) throws ProcessingException
	{
		boolean state = isShellyOn();

		if (state != on) {
			URI shellyUri = UriBuilder.fromUri("http://" + shellyHost + "/").build();
			final String action = on ? "on" : "off";
			WebTarget wt = client
				.target(UriBuilder.fromUri(shellyUri)
					.path(SHELLY_PATH)
					.queryParam("turn", action)
					.build());

			if (LOG.isDebugEnabled()) {
				LOG.debug("request => {}", wt.getUri());
			}

			try (Response r = wt.request().get();) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("response => {}", r.readEntity(String.class));
				}

				if (r.getStatus() == 200) {
					LOG.info("switched shelly {} successfully", action);
				}
				else {
					LOG.info("switched shelly {} failed; {}", action, r);
				}
			}
		}
		else {
			if (LOG.isDebugEnabled()) {
				LOG.info("nothing todo, because current state of shelly is <{}>", state ? "on" : "off");
			}
		}
	}

	public static void main(String[] args) throws Exception
	{
			try {
				Thread th = new Thread(new RealTimeDataReader());
				th.setName("ReaderThread");
				th.start();
			}
			catch(Exception x) {
					LOG.error("caught exception", x);
					throw x;
				}
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
		DbAdapter dbAdpater = new InfluxAdapter();

		while (true) {
			LOG.info("doing webservice call - uri <" + webTarget.getUri() + ">");
			try (Response response = webTarget
				.request()
				.get()) {
				String entity = response.readEntity(String.class);
				JSONObject obj = new JSONObject(entity);
				JSONObject pac = obj.getJSONObject("Body").getJSONObject("Data").getJSONObject("PAC");

				try {
					List<RealTimeData> current = parseData(pac, "PAC");
					LOG.info(current);

					final AtomicLong currentProducedPower = new AtomicLong();
					current.forEach(x -> {
						currentProducedPower.addAndGet(x.value);
						dbAdpater.sendMeasurement("energy_production_current", x.name.toLowerCase(), x.value, x.unit, "fronius_symo_" + x.device,
							"home");
					});

					try {
						if (currentProducedPower.get() > powerThreshold) {
							if (shellyManaging) {
								switchShelly(true);
							}
						}
						else {
							if (shellyManaging) {
								switchShelly(false);
							}
						}
					}
					catch (Exception x) {
						LOG.error("error while interacting with the shelly component", x);
					}


					JSONObject dEnergy = obj.getJSONObject("Body").getJSONObject("Data").getJSONObject("DAY_ENERGY");
					List<RealTimeData> data = parseData(dEnergy, "DAY_ENERGY");
					data.forEach(x -> {
						LOG.info(x);
						dbAdpater.sendMeasurement("energy_production", x.name.toLowerCase(), x.value, x.unit, "fronius_symo_" + x.device, "home");
					});
				}
				catch (JSONException x) {
					LOG.error("error while parsing data", x);
				}
			}
			catch (Exception x) {
				LOG.error("error while processing data", x);
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
