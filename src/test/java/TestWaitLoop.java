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
public class TestWaitLoop {

    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        final int delay = 10;
        executor.execute(() -> {
            long nextExecuteTime = Instant.now().toEpochMilli() / delay * delay + delay;
            List<Data> data = new ArrayList<>();
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (executor) {
                    long now = Instant.now().toEpochMilli();
                    if (now >= nextExecuteTime) {
                        System.out.println("schedule time: " + now % 1000);
                        data.add(new Data("test", now, nextExecuteTime));
                        data.add(new Data("standard", nextExecuteTime, nextExecuteTime));
                        nextExecuteTime += delay;
                    } else {
                        try {
                            executor.wait(nextExecuteTime - now);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            }
            CsvUtil.export(formatData(data), Data.class);
        });
        Thread.sleep(500);
        executor.shutdownNow();
    }

    private static List<Data> formatData(List<Data> data) {
        data = data.stream().sorted(Comparator.comparingLong(Data::getStandardTime)).collect(
            Collectors.toList());
        long base = data.get(0).getStandardTime();
        return data.stream().map(d -> new Data(d.group, d.getActualTime() - base, d.getStandardTime() - base))
            .collect(Collectors.toList());
    }
}
