package fr.thomah.roger.karotz;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import fr.thomah.roger.common.MethodPoller;
import fr.thomah.roger.common.Randomizer;
import fr.thomah.roger.file.FileEntity;
import fr.thomah.roger.file.FileService;
import fr.thomah.roger.http.HttpClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KarotzService {

    @Autowired
    private FileService fileService;

    @Autowired
    private HttpClientService httpClientService;

    @Value("${fr.thomah.roger.karotz.url}")
    private String karotzBaseUrl;

    @Value("${fr.thomah.roger.karotz.testmode}")
    private boolean testmode;

    private final HttpRequest.Builder httpBuilder;

    private int earsPosition = 0;

    public KarotzService() {
        httpBuilder = HttpRequest.newBuilder();
    }

    public HttpResponse<String> status() {
        String url;
        if (!testmode) {
            url = karotzBaseUrl + "/cgi-bin/status";
        } else {
            url = "http://www.mocky.io/v2/5d126f9631000018c208d35c";
        }
        HttpRequest request = httpBuilder
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .build();
        HttpResponse<String> response = null;
        try {
            log.info("GET : {}", url);
            response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error("Unable to get Karotz status");
        }
        return response;
    }

    public boolean statusValidation(HttpResponse<String> response) {
        if(response == null)
            return false;
        boolean success = response.statusCode() == 200;
        if (!success)
            return false;
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        return json != null && json.get("version") != null;
    }

    void sound(String fileUrl) {
        try {
            String url;
            if (!testmode) {
                url = karotzBaseUrl + "/cgi-bin/sound?url=" + fileUrl.replaceAll(" ", "%20");
            } else {
                url = "http://www.mocky.io/v2/5d1279923100001ec508d38e";
            }
            HttpRequest request = httpBuilder
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            ears(String.valueOf(Randomizer.generateNumberBetween(10, 20)));
            log.info("GET : {}", url);
            httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    void ears(String move) {
        int ears = Integer.parseInt(move);
        earsPosition+= ears;
        try {
            String url;
            if (!testmode) {
                url = karotzBaseUrl + "/cgi-bin/ears?left=" + earsPosition + "&right=" + earsPosition + "&noreset=1";
            } else {
                url = "http://www.mocky.io/v2/5d19ef0b2f0000a148fd733f";
            }
            HttpRequest request = httpBuilder
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            log.info("GET : {}", url);
            httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    void tts(String text) {
        try {
            String url;
            if (!testmode) {
                url = karotzBaseUrl + "/cgi-bin/tts?voice=3&text=" + HttpClientService.encodeValue(text) + "&nocache=undefined";
            } else {
                url = "http://www.mocky.io/v2/5d19ef0b2f0000a148fd733f";
            }
            HttpRequest request = httpBuilder
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            ears(String.valueOf(Randomizer.generateNumberBetween(10, 20)));
            log.info("GET : {}", url);
            httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String takeSnapshot() {
        String snapshot = null;
        try {
            String url;
            if (!testmode) {
                url = karotzBaseUrl + "/cgi-bin/snapshot";
            } else {
                url = "http://www.mocky.io/v2/5d19ef0b2f0000a148fd733f";
            }
            HttpRequest request = httpBuilder
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            log.info("GET : {}", url);
            HttpResponse<String> response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if(json != null && json.get("filename") != null) {
                snapshot = json.get("filename").getAsString();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return snapshot;
    }

    public List<String> listSnapshots() {
        List<String> snapshots = null;
        try {
            String url;
            if (!testmode) {
                url = karotzBaseUrl + "/cgi-bin/snapshot_list";
            } else {
                url = "http://www.mocky.io/v2/5d19ef0b2f0000a148fd733f";
            }
            HttpRequest request = httpBuilder
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            log.info("GET : {}", url);
            HttpResponse<String> response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if(json != null && json.get("snapshots") != null) {
                snapshots = new ArrayList<>();
                JsonArray jsonSnapshots = json.getAsJsonArray("snapshots");
                for(int k = 0 ; k < jsonSnapshots.size() ; k++) {
                    snapshots.add(jsonSnapshots.get(k).getAsJsonObject().get("id").getAsString());
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return snapshots;
    }

    public void downloadSnapshot(List<String> snapshots) {
        for(int k = 0 ; k < snapshots.size() ; k++) {
            try {
                String url;
                String filePath = "tmp/snapshots";
                filePath = fileService.prepareDirectories(filePath) + File.separator + snapshots.get(k);

                if (!testmode) {
                    url = karotzBaseUrl + "/snapshots/" + snapshots.get(k);
                } else {
                    url = "http://www.mocky.io/v2/5d19ef0b2f0000a148fd733f";
                }
                log.info("GET : {}", url);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .build();
                HttpResponse<Path> response = httpClientService.getHttpClient().send(request,
                        HttpResponse.BodyHandlers.ofFile(Paths.get(filePath)));

                List<String> contenType = response.headers().map().get("Content-Type");
                if(contenType.size() == 1) {
                    FileEntity fileEntity = fileService.saveOnFilesystem(filePath, contenType.get(0), "snapshots", snapshots.get(k).replace(".jpg", ""));
                    fileService.saveInDb(fileEntity);
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String clearSnapshots() {
        String snapshot = null;
        try {
            String url;
            if (!testmode) {
                url = karotzBaseUrl + "/cgi-bin/clear_snapshots";
            } else {
                url = "http://www.mocky.io/v2/5d19ef0b2f0000a148fd733f";
            }
            HttpRequest request = httpBuilder
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            log.info("GET : {}", url);
            httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return snapshot;
    }

    void reboot() {

        String[] parts = karotzBaseUrl.split(":");
        String host = parts[1].replaceAll("//","");

        Session session = null;
        ChannelExec channel = null;

        try {

            // Prepare SSH channel
            session = new JSch().getSession("karotz", host, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            // Execute reboot command
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("/sbin/reboot -d 1");
            channel.setInputStream(null);
            channel.connect();

        } catch (JSchException e) {
            log.error("Cannot run reboot command on Karotz", e);
        } finally {
            if (session != null) {
                session.disconnect();
            }
            if (channel != null) {
                channel.disconnect();
            }
        }

        // Waiting for 5sec before probing
        log.debug("Waiting for 5sec before probing");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.error("Sleeping was interrupted", e);
        }

        // Probing Karotz status
        MethodPoller<HttpResponse<String>> poller = new MethodPoller<>();
        poller.method(this::status)
                .until(this::statusValidation)
                .poll(Duration.ofHours(1), 1000)
                .execute();

    }
}
