import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Main
 *
 * @author gill
 * @version 2023/12/05
 **/
public class TestMultiLoop {

    public static final int CAS_THRESHOLD = 5;

    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final int delay = 10;
        executor.execute(() -> {
            List<Data> data = new ArrayList<>();
            long nextExecuteTime = Instant.now().toEpochMilli() / delay * delay + delay;
            while (!Thread.currentThread().isInterrupted()) {
                long now = Instant.now().toEpochMilli();
                long diff = nextExecuteTime - now;
                if (diff <= 0) {
                    System.out.println("schedule time: " + Instant.now().toEpochMilli() % 1000);
                    data.add(new Data("test", now, nextExecuteTime));
                    data.add(new Data("standard", nextExecuteTime, nextExecuteTime));
                    nextExecuteTime += delay;
                } else if (diff > CAS_THRESHOLD) {
                    try {
                        Thread.sleep(diff - CAS_THRESHOLD);
                    } catch (InterruptedException e) {
                        break;
                    }
                } else {
                    Thread.yield();
                }
            }
            CsvUtil.export(formatData(data), Data.class);
        });
        Thread.sleep(500);
        executor.shutdownNow();
    }

    private static List<Data> formatData(List<Data> data) {
        data = data.stream().sorted(Comparator.comparingLong(Data::getStandardTime)).collect(Collectors.toList());
        long base = data.get(0).getStandardTime();
        return data.stream().map(d -> new Data(d.group, d.getActualTime() - base, d.getStandardTime() - base))
            .collect(Collectors.toList());
    }

}
