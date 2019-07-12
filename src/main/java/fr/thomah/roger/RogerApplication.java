package fr.thomah.roger;

import fr.thomah.roger.clients.BackClient;
import fr.thomah.roger.clients.KarotzClient;
import fr.thomah.roger.clients.SlackClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Timer;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class RogerApplication {

	public static final String SLACK_TOKEN = System.getenv("SlackToken");
	public static final String BASE_URL = System.getenv("BaseURL");
	public static final String THIS_COMPUTER_URL = System.getenv("ComputerURL");
	public static final boolean KAROTZ_AVAILABLE = BASE_URL != null;
	private static String PROXY_HOST = null;
	private static int PROXY_PORT = 0;

	MethodPoller<HttpResponse<String>> poller = new MethodPoller<>();

	@Autowired
	private BackClient backClient;

	@Autowired
	private KarotzClient karotzClient;

	@Autowired
	private SlackClient slackClient;

	public static void main(String[] args) {
		SpringApplication.run(RogerApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		String proxy = System.getenv("HTTP_PROXY");
		if (proxy != null) {
			proxy = proxy.replaceFirst("http://", "");
			String[] PROXY_VALUES = proxy.split(":");
			PROXY_HOST = PROXY_VALUES[0];
			PROXY_PORT = Integer.valueOf(PROXY_VALUES[1]);
		}

		HttpClient.Builder builder = HttpClient.newBuilder();
		if (PROXY_HOST != null && PROXY_PORT != 0) {
			builder = builder.proxy(ProxySelector.of(new InetSocketAddress(PROXY_HOST, PROXY_PORT)));
		}

		Db db = new Db();

		HttpClient httpClient = builder.build();
		HttpRequest.Builder builderRequest = HttpRequest.newBuilder();

		backClient.init(httpClient, builderRequest);
		slackClient.init(httpClient, builderRequest);
		karotzClient.init(httpClient, builderRequest);

		// Wait for Roger Server to be accessible
		poller.method(backClient::health)
				.until(backClient::healthValidation)
				.poll(Duration.ofHours(1), 1000)
				.execute();

		// Wait for Slack to be accessible
		poller.method(slackClient::ping)
				.until(slackClient::pingValidation)
				.poll(Duration.ofHours(1), 1000)
				.execute();

		// Wait for Karotz to be accessible
		poller.method(karotzClient::status)
				.until(karotzClient::statusValidation)
				.poll(Duration.ofHours(1), 1000)
				.execute();

		slackClient.setKarotzClient(karotzClient);
		slackClient.setDb(db);
		slackClient.connect();

		backClient.connect();

		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(slackClient, 0, 2000);
		timer.scheduleAtFixedRate(backClient, 0, 120000);
	}
}