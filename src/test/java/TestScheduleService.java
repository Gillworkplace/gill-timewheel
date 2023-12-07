import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Main
 *
 * @author gill
 * @version 2023/12/05
 **/
public class TestScheduleService {

    public static void main(String[] args) throws Exception {
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
        final int delay = 10;
        Queue<Data> data = new ConcurrentLinkedDeque<>();
        long sts = Instant.now().toEpochMilli();
        AtomicLong period = new AtomicLong(delay);
        executor.scheduleAtFixedRate(() -> {
            long now = Instant.now().toEpochMilli();
            System.out.println("schedule time: " + now % 1000);
            data.add(new Data("test", now, sts + period.get()));
            data.add(new Data("standard", sts + period.get(), sts + period.get()));
            period.addAndGet(delay);
        }, 0, delay, TimeUnit.MILLISECONDS);
        Thread.sleep(500);
        executor.shutdownNow();
        CsvUtil.export(formatData(data), Data.class);
    }

    private static List<Data> formatData(Collection<Data> data) {
        data = data.stream().sorted(Comparator.comparingLong(Data::getStandardTime)).collect(Collectors.toList());
        long base = data.stream().findFirst().get().getStandardTime();
        return data.stream().map(d -> new Data(d.group, d.getActualTime() - base, d.getStandardTime() - base))
            .collect(Collectors.toList());
    }
}
