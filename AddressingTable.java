import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class AddressingTable {

    private Map<String, String> ips;
    private String toServer;
    private String provider;

    private String toNetwork;
    private Boolean isConsuming;

    private Map<String, Duration> latencies;
    private Map<String, Integer> hops;
    private Map<String, Instant> floodTable;

    private Instant latestWave;

    // Controlo de flooding para evitar loops infinitos
    // ServerIP, (Vizinho, True se ja reencaminhamos)

    public Instant getLatestWave() {
        return latestWave;
    }

    public void setLatestWave(Instant latestWave) {
        this.latestWave = latestWave;
    }

    // (IP vizinhos/adjacentes, True se querem consumir a Stream)
    private Map<String, Boolean> streamingTable;
    private final ReentrantLock lock;

    public String getProvider() {
        lock.lock();
        try {
            return this.provider;
        } finally {
            lock.unlock();
        }

    }

    public void setProvider(String provider) {
        lock.lock();
        try {
            this.provider = provider;
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Instant> getFloodTable() {
        lock.lock();
        try {
            return this.floodTable;
        } finally {
            lock.unlock();
        }
    }

    public Map<String, String> getIps() {
        lock.lock();
        try {
            return this.ips;
        } finally {
            lock.unlock();
        }
    }

    public String getIp(String ips) {
        lock.lock();
        try {
            return this.ips.get(ips);
        } finally {
            lock.unlock();
        }
    }

    public Duration getLatency(String ip) {
        lock.lock();
        try {
            return this.latencies.get(ip);
        } finally {
            lock.unlock();
        }

    }

    public void setLatency(String ip, Duration latency) {
        lock.lock();
        try {
            this.latencies.put(ip, latency);
        } finally {
            lock.unlock();
        }

    }

    public int getHops(String ip) {
        lock.lock();
        try {
            return this.hops.get(ip);
        } finally {
            lock.unlock();
        }
    }

    public void setHops(String ip, int hops) {
        lock.lock();
        try {
            this.hops.put(ip, hops);
        } finally {
            lock.unlock();
        }
    }

    public String getToNetwork() {
        lock.lock();
        try {
            return this.toNetwork;
        } finally {
            lock.unlock();
        }
    }

    public void setToNetwork(String toNetwork) {
        lock.lock();
        try {
            this.toNetwork = toNetwork;
        } finally {
            lock.unlock();
        }
    }

    public String getToServer() {
        lock.lock();
        try {
            return this.toServer;
        } finally {
            lock.unlock();
        }
    }

    public void setToServer(String toServer) {
        lock.lock();
        try {
            this.toServer = toServer;
        } finally {
            lock.unlock();
        }
    }

    public Boolean getisConsuming() {
        lock.lock();
        try {
            return isConsuming;
        } finally {
            lock.unlock();
        }
    }

    public void setisConsuming(Boolean isConsuming) {
        lock.lock();
        try {
            this.isConsuming = isConsuming;
        } finally {
            lock.unlock();
        }
    }

    public void turnOn(String ip) {
        lock.lock();
        try {
            streamingTable.put(ip, true);
        } finally {
            lock.unlock();
        }
    }

    public void turnOff(String ip) {
        lock.lock();
        try {
            streamingTable.put(ip, false);
        } finally {
            lock.unlock();
        }
    }

    public Set<String> getVizinhos() {
        lock.lock();
        try {
            return this.ips.keySet();
        } finally {
            lock.unlock();
        }
    }

    public Set<String> getVizinhosClone() {
        lock.lock();
        try {
            return this.ips.keySet().stream().map(String::new).collect(Collectors.toSet());
        } finally {
            lock.unlock();
        }

    }

    public Map<String, Duration> getLatencies() {
        lock.lock();
        try {
            return this.latencies;
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Integer> getHopsTable() {
        lock.lock();
        try {
            return this.hops;
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Boolean> getStreamingTable() {
        lock.lock();
        try {
            return this.streamingTable;
        } finally {
            lock.unlock();
        }
    }

    // se estamos a transmitir a stream para algum vizinho
    public Boolean isStreaming() {

        lock.lock();
        try {
            Set<String> bla = this.getVizinhosClone();
            bla.remove(this.getProvider());

            for (String s : bla)
                if (this.streamingTable.get(s))
                    return true;

            return false;

        } finally {
            lock.unlock();
        }

    }

    public Boolean getEstado(String ip) {
        lock.lock();
        try {
            return this.streamingTable.get(ip);
        } finally {
            lock.unlock();
        }
    }

    public void setEstado(String ip, Boolean estado) {
        lock.lock();
        try {
            this.streamingTable.put(ip, estado);
        } finally {
            lock.unlock();
        }
    }

    public void UpdateRoute(String sender, String server, int hops, Duration timeElapsed, Instant wave) {

        int best_route_hops;
        Duration best_route_duration;
        Duration one_secs = Duration.ofMillis(1000);

        best_route_hops = this.getHops(this.toServer);
        best_route_duration = this.getLatency(this.toServer);

        Duration limite_inferior = best_route_duration.minus(one_secs);
        Duration limite_superior = best_route_duration.plus(one_secs);

        int val_inferior = limite_inferior.compareTo(timeElapsed);
        int val_superior = limite_superior.compareTo(timeElapsed);
        if (val_inferior > 0 && this.latestWave.equals(wave)) {
            this.setToServer(sender);
            this.setProvider(server);
            this.setHops(sender, hops);
            this.setLatency(sender, timeElapsed);
        } else if (val_superior > 0 && best_route_hops > hops && this.latestWave.equals(wave)) {
            this.setToServer(sender);
            this.setProvider(server);
            this.setHops(sender, hops);
            this.setLatency(sender, timeElapsed);
        } else {
            // System.out.println("Não é mais rápida");
        }

    }

    public void UpdateRoutingTable(String sender, String server, int hops, Duration timeElapsed, Instant wave) {

        String best_route_ip;

        best_route_ip = this.getToServer();

        if (this.latestWave == null || this.latestWave.isBefore(wave)) {
            this.latestWave = wave;
        }

        if (best_route_ip == null) {
            this.setToServer(sender);
            this.setProvider(server);
            this.setLatency(sender, timeElapsed);
            this.setHops(sender, hops);
            System.out.println("First route to Server found");

        } else {
            for (String ip : this.getVizinhosClone()) {
                UpdateRoute(ip, server, this.getHops(ip), this.getLatency(ip), wave);
            }
        }

    }

    public AddressingTable(Map<String, String> neighbours) {
        this.lock = new ReentrantLock();
        this.isConsuming = false;
        this.streamingTable = new HashMap<>();
        this.hops = new HashMap<>();
        this.latencies = new HashMap<>();
        this.floodTable = new HashMap<>();

        this.toServer = null;
        this.provider = null;
        this.ips = new HashMap<>();

        this.latestWave = null;

        for (String s : neighbours.keySet()) {
            this.ips.put(neighbours.get(s), s);
        }

        for (String n : ips.keySet()) {
            this.streamingTable.put(n, false);
            this.hops.put(n, Integer.MAX_VALUE);
            this.latencies.put(n, Duration.ofMillis(Integer.MAX_VALUE));
        }
        System.out.println(this.streamingTable.toString());

    }

}
