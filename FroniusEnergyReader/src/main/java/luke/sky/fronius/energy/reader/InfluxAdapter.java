package luke.sky.fronius.energy.reader;

import java.net.URI;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

public class InfluxAdapter implements DbAdapter
{
	private static final Logger LOG = LogManager.getLogger(InfluxAdapter.class);

	// enviroment
	private static final String ENV_INFLUXDB_HOST = "INFLUXDB.HOST"; // default: localhost
	private static final String ENV_INFLUXDB_PORT = "INFLUXDB.PORT"; // default: 8086
	private static final String ENV_INFLUXDB_BUCKET = "INFLUXDB.BUCKET"; // default: home
	private static final String ENV_INFLUXDB_ORG = "INFLUXDB.ORG"; // default: pendulum
	private static final String ENV_INFLUXDB_USER_TOKEN = "INFLUXDB.USER.TOKEN"; // default:
																					// noHLluSca-ez97U3r4_UYCqE60106OtJuPX1_PfCVNBMVY2H_eGZ4CfrTmP410Wmi22i0tydZ8w_aJ7u0ACEDQ==

	// API
	private static final String PATH = "/api/v2/write";
	private static final String PARAM_BUCKET = "bucket";
	private static final String PARAM_ORG = "org";
	private static final String PARAM_PRECISION = "precision"; // Precision of timestamps in the line protocol. Accepts ns (nanoseconds),
																// us(microseconds), ms (milliseconds) and s (second)

	private String host;
	private int port;
	private String token;
	private String bucket;
	private String org;

	private final Client client;


	public InfluxAdapter()
	{
		host = System.getenv(ENV_INFLUXDB_HOST);
		if (host == null || host.isEmpty()) {
			host = "localhost";
		}
		port = 8086;
		try {
			port = Integer.parseInt(System.getenv(ENV_INFLUXDB_PORT));
		}
		catch (Exception x) {
			// silent
		}
		bucket = System.getenv(ENV_INFLUXDB_BUCKET);
		if (bucket == null || bucket.isEmpty()) {
			bucket = "home";
		}
		org = System.getenv(ENV_INFLUXDB_ORG);
		if (org == null || org.isEmpty()) {
			org = "pendulum";
		}
		token = System.getenv(ENV_INFLUXDB_USER_TOKEN);
		client = ClientBuilder.newClient();
	}

	@Override
	public void sendMeasurement(
		final String category,
		final String name,
		final Long value,
		final String unit,
		final String equipment,
		final String area)
	{
		URI uri = UriBuilder.fromUri("http://" + host + ":" + port).build();
		WebTarget wt = client
			.target(UriBuilder.fromUri(uri)
				.path(InfluxAdapter.PATH)
				.queryParam(PARAM_BUCKET, bucket)
				.queryParam(PARAM_ORG, org)
				.queryParam(PARAM_PRECISION, "ms")
				.build());

		String plain = category + ",equipment=" + equipment + ",area=" + area + " " + name + "_" + unit.toLowerCase() + "=" + value + " "
			+ System.currentTimeMillis();
		LOG.info("posting data <" + plain + "> to uri <" + wt.getUri() + "> with token <" + (token != null) + ">");

		Response response = Response.ok().build();
		try {
			if (token != null) {
				response = wt
					.request(MediaType.TEXT_PLAIN)
					.header("Authorization", "Token " + token)
					.post(Entity.text(plain));
			}
			else {
				response = wt
					.request(MediaType.TEXT_PLAIN)
					.post(Entity.text(plain));
			}
			LOG.info("response code <" + response.getStatus() + "> body <" + response.readEntity(String.class) + ">");
		}
		finally {
				response.close();
		}
	}

	public static void main(String[] args) throws Exception
	{
		Configurator.initialize(new DefaultConfiguration());
		Configurator.setRootLevel(Level.DEBUG);
		InfluxAdapter a = new InfluxAdapter();
		a.sendMeasurement("energy", "day_energy", 500L, "kwH", "fronius-1", "home");
	}
}
