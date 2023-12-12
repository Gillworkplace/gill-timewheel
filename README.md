# Gill-TimeWheel
时间轮盘调度器，统一管理延时任务



## Features

- 内存级别的异步处理延时任务 
- 可对延时任务进行幂等性判断
- 高性能
- 自带metrics （TODO）



## Architecture

![结构设计](https://github.com/Gillworkplace/gill-timewheel/blob/gill/design/img/%E7%BB%93%E6%9E%84%E8%AE%BE%E8%AE%A1.png)

`TimeWheel`中使用`Map`来进行时间轮盘的存储，`map.key`代表时间轮盘的任期，`map.value`代表一个时间轮盘。

一个轮盘的周期 = `wheelSize` * `tick`

`wheelSize`表示轮盘分割的次数。`tick`表示时间轮盘的任务调度的最小单位。



## Getting started

### Dependency

jdk版本：17

```xml
<!-- https://github.com/Gillworkplace/gill-gutil -->
<dependency>
  <groupId>com.gill</groupId>
  <artifactId>gill-gutil</artifactId>
  <version>1.0.0</version>
</dependency>
```



### Demo

```java
public void demo() throws Exception {

    // 创建一个扫描间隔为1s，一个周期为1分钟的时间轮盘
    TimeWheel timeWheel = TimeWheelFactory.create(1000, 60);

    // 在一分钟后执行
    timeWheel.executeAtTime(Instant.now().toEpochMilli() + 60 * 1000, "task-1", () -> System.out.println("say hi"));

    // 在一分钟后执行
    timeWheel.executeWithDelay(60 * 1000, "task-2", () -> System.out.println("say hello"));
}
```



### TimeWheelFactory参数说明

| 参数            | 说明                                                         |
| --------------- | ------------------------------------------------------------ |
| name            | timewheel的名称，日志打印时会附带                            |
| tick            | 时间轮盘扫描间隔                                             |
| wheelSize       | 轮盘刻度长度，即一个轮盘中存在的tick个数                     |
| expired         | 当 expired > 0时，代表延时任务提交后expired毫秒内不会接受相同key的延时任务。当expired == 0时，代表延时任务执行完后，就可再提交相同key的延时任务。当expired < 0时，代表用于校验幂等性的缓存会根据task是否gc而进行自动清除。 |
| defaultExecutor | 执行延时任务时使用的线程池                                   |



```java
	/**
     * 创建时间轮盘
     * 
     * @param tick 轮盘的扫描间隔
     * @param wheelSize 轮盘刻度长度
     * @param defaultExecutor 默认异步执行的线程池
     * @return 时间轮盘
     */
    public static TimeWheel create(long tick, int wheelSize, ExecutorService defaultExecutor);

    /**
     * 创建时间轮盘
     *
     * @param tick 轮盘的扫描间隔
     * @param wheelSize 轮盘刻度长度
     * @return 时间轮盘
     */
    public static TimeWheel create(long tick, int wheelSize);

    /**
     * 创建时间轮盘
     *
     * @param name 轮盘名称
     * @param tick 轮盘的扫描间隔
     * @param wheelSize 轮盘刻度长度
     * @param expired 幂等任务过期时间
     * @return 轮盘
     */
    public static TimeWheel create(String name, long tick, int wheelSize, long expired);

    /**
     * 创建时间轮盘
     *
     * @param name 轮盘名称
     * @param tick 轮盘的扫描间隔
     * @param wheelSize 轮盘刻度长度
     * @param expired 幂等任务过期时间
     * @param defaultExecutor 默认执行器
     * @return 轮盘
     */
    public static TimeWheel create(String name, long tick, int wheelSize, long expired, ExecutorService defaultExecutor);
```

`TimeWheel`实例可通过`executeWithDelay`或`executeAtTime`进行延时任务调度。

### TimeWheel参数说明

| 参数     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| key      | 延时任务的幂等id                                             |
| delay    | 延迟多少ms后执行任务                                         |
| taskName | 延时任务名称，用于日志打印                                   |
| runnable | 任务过程                                                     |
| executor | 执行延时任务使用的线程池，如果不定义则使用TimeWheelFactory的defaultExecutor |



```java
    /**
     * 添加延时执行的调度任务
     *
     * @param delay 延迟多久执行单位ms
     * @param taskName 任务名
     * @param runnable 执行方法块
     * @return 任务唯一键
     */
    long executeWithDelay(long delay, String taskName, Runnable runnable);

    /**
     * 添加延时执行的调度任务
     *
     * @param delay 延迟多久执行单位ms
     * @param executor 异步执行的线程池
     * @param taskName 任务名
     * @param runnable 执行方法块
     * @return 任务唯一键
     */
    long executeWithDelay(long delay, ExecutorService executor, String taskName, Runnable runnable);

    /**
     * 添加延时执行的调度任务
     *
     * @param key 唯一键，保证任务幂等执行
     * @param delay 延迟多久执行单位ms
     * @param taskName 任务名
     * @param runnable 执行方法块
     */
    void executeWithDelay(long key, long delay, String taskName, Runnable runnable);

    /**
     * 添加延时执行的调度任务
     *
     * @param key 唯一键，保证任务幂等执行
     * @param delay 延迟多久执行单位ms
     * @param executor 异步执行的线程池
     * @param taskName 任务名
     * @param runnable 执行方法块
     */
    void executeWithDelay(long key, long delay, ExecutorService executor, String taskName, Runnable runnable);

    /**
     * 添加指定时间点执行的调度任务
     *
     * @param executeTime 执行时间戳 ms
     * @param taskName 任务名
     * @param runnable 执行方法块
     * @return 任务唯一键
     */
    long executeAtTime(long executeTime, String taskName, Runnable runnable);

    /**
     * 添加指定时间点执行的调度任务
     *
     * @param executeTime 执行时间戳 ms
     * @param executor 异步执行的线程池
     * @param taskName 任务名
     * @param runnable 执行方法块
     * @return 任务唯一键
     */
    long executeAtTime(long executeTime, ExecutorService executor, String taskName, Runnable runnable);

    /**
     * 添加指定时间点执行的调度任务
     *
     * @param key 唯一键，保证任务幂等执行
     * @param executeTime 执行时间戳 ms
     * @param taskName 任务名
     * @param runnable 执行方法块
     */
    void executeAtTime(long key, long executeTime, String taskName, Runnable runnable);

    /**
     * 添加指定时间点执行的调度任务
     *
     * @param key 唯一键，保证任务幂等执行
     * @param executeTime 执行时间戳 ms
     * @param executor 异步执行的线程池
     * @param taskName 任务名
     * @param runnable 执行方法块
     */
    void executeAtTime(long key, long executeTime, ExecutorService executor, String taskName, Runnable runnable);
```



## Postscript

### 延时任务准确性

延时任务的准确性依赖参数`tick`。假设任务延时1000ms执行，`tick`设置了100，那么该任务的执行时间会在900ms ~ 1100ms波动。

`tick`的越小时间轮盘扫描的次数越频繁，对程序的性能影响越大。`tick`的设置需要根据业务场景进行定制。

### 延时任务幂等性

`timewheel`通过`Map<Long, Task> taskCache`记录所有提交的任务。用户可以通过设置`expired`参数来控制`taskCache`中对象的存活时间。过期的`taskCache`对象是也是通过`timewheel`进行延时删除的因此删除任务的时效性与普通的延时任务一致。

当用户在设置了`expired`后，调用`execute`方法时实际上是在`timewheel`中提交两个延时任务，一个是用户定义的延时任务，一个是清除`taskCache`的过期对象。



## Performace

### netty timewheel性能

#### 10000个10ms延时任务瞬时提交，tick周期为10ms

##### 测试用例代码

```java
@Test
public void testExecution() throws InterruptedException, NoSuchAlgorithmException {
    SecureRandom random = SecureRandom.getInstanceStrong();
    int maxDelay = 1000;
    int TPS = 10000;
    final CountDownLatch latch = new CountDownLatch(TPS);
    ExecutorService invoker = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "invoker"));
    ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "executor"));

    final HashedWheelTimer timer = new HashedWheelTimer(Executors.defaultThreadFactory(), 10, TimeUnit.MILLISECONDS,
        32, true, 100000, executor);

    Thread.sleep(1000);

    Counter completeDelayTaskCounter = Counter.newCounter("completeDelayTaskCounter");
    Cost addTaskCost = Cost.newStatistic("addTaskCost");
    Cost delayError = Cost.newStatistic("delayError");
    for (int i = 0; i < TPS; i++) {
        invoker.execute(() -> Cost.cost(() -> {
            final long startTime = System.nanoTime();
            int delay = random.nextInt(maxDelay);
            timer.newTimeout(to -> {
                long realDelay = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                long diff = realDelay - delay;
                delayError.merge(Math.abs(diff));
                completeDelayTaskCounter.incr();
                latch.countDown();
            }, delay, TimeUnit.MILLISECONDS);
        }, addTaskCost));
    }
    boolean await = latch.await(10000, TimeUnit.MILLISECONDS);
    addTaskCost.println();
    delayError.println();
    Assertions.assertTrue(await);
    Assertions.assertEquals(TPS, completeDelayTaskCounter.get());
    timer.stop();
}
```

##### 性能报告

![image-20231208180120976](./../img/image-20231208180120976.png)

![image-20231208180227526](./../img/image-20231208180227526.png)

CPU使用率峰值为5%

添加任务耗时0.48ms

任务的执行误差上均值是5.9ms

最大延时误差14ms



#### 10000个10s延时任务在1分钟内提交，tick周期为100ms

##### 测试用例代码

```java
@Test
public void testExecution2() throws Exception {
    SecureRandom random = SecureRandom.getInstanceStrong();
    int maxDelay = 10000;
    int TPM = 10000;

    CountDownLatch latch = new CountDownLatch(TPM);
    ExecutorService invoker = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "invoker"));
    ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "executor"));

    final HashedWheelTimer timer = new HashedWheelTimer(Executors.defaultThreadFactory(), 100, TimeUnit.MILLISECONDS,
        32, true, 100000, executor);

    Thread.sleep(1000);

    Counter completeDelayTaskCounter = Counter.newCounter("completeDelayTaskCounter");
    Cost addTaskCost = Cost.newStatistic("addTaskCost");
    Cost delayError = Cost.newStatistic("delayError");

    Thread.sleep(1000);
    int surplus = TPM;
    while (surplus > 0) {
        int wt = random.nextInt(200);
        Thread.sleep(wt);
        int num = Math.min(surplus, random.nextInt(50));
        for (int i = 0; i < num; i++) {
            invoker.execute(() -> Cost.cost(() -> {
                final long startTime = System.nanoTime();
                int delay = random.nextInt(maxDelay);
                timer.newTimeout(to -> {
                    long realDelay = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                    long diff = realDelay - delay;
                    delayError.merge(Math.abs(diff));
                    completeDelayTaskCounter.incr();
                    latch.countDown();
                }, delay, TimeUnit.MILLISECONDS);
            }, addTaskCost));
        }
        surplus -= num;
    }
    System.out.println("add complete");
    boolean await = latch.await(10000, TimeUnit.MILLISECONDS);
    addTaskCost.println();
    delayError.println();
    Assertions.assertTrue(await);
    Assertions.assertEquals(TPM, completeDelayTaskCounter.get());
    timer.stop();
}
```

##### 性能报告

![image-20231208190635212](./../img/image-20231208190635212.png)

![image-20231208190649458](./../img/image-20231208190649458.png)

CPU使用率峰值为5%

添加任务耗时0.47ms

任务的执行误差上均值是50ms

最大延时误差102ms



### timewheel性能

#### 测试机器配置

| 参数      | 值                |
| --------- | ----------------- |
| 系统平台  | Windows 10        |
| CPU型号   | AMD Ryzen 7 5700X |
| CPU核心数 | 8                 |
| 内存大小  | 32G               |

![image-20231211100327694](./../img/image-20231211100327694.png)

#### 10000个10ms延时任务瞬时提交，tick周期为10ms

##### 测试用例代码

```java
@Test
public void testAddDelayedTasksWith100ThreadsConcurrently() throws Exception {
    SecureRandom random = SecureRandom.getInstanceStrong();
    int maxDelay = 1000;
    int TPS = 10000;

    CountDownLatch latch = new CountDownLatch(TPS);
    ExecutorService invoker = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "invoker"));
    ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "executor"));
    TimeWheel tw = TimeWheelFactory.create("ptest-timewheel", 10, 10, TimeWheelFactory.EXPIRED_BY_GC, executor);
    Counter completeDelayTaskCounter = Counter.newCounter("completeDelayTaskCounter");
    Statistic addTaskCost = Statistic.newStatistic("addTaskCost");
    Statistic delayError = Statistic.newStatistic("delayError");

    Thread.sleep(1000);

    for (int i = 0; i < TPS; i++) {
        invoker.execute(() -> Cost.costMerge(() -> {
            final long startTime = System.nanoTime();
            int delay = random.nextInt(maxDelay);
            tw.executeWithDelay(delay, "test", () -> {
                long realDelay = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                long diff = realDelay - delay;
                delayError.merge(Math.abs(diff));
                completeDelayTaskCounter.incr();
                latch.countDown();
            });
        }, addTaskCost));
    }
    boolean await = latch.await(10000, TimeUnit.MILLISECONDS);
    addTaskCost.println();
    delayError.println();
    Assertions.assertTrue(await);
    Assertions.assertEquals(TPS, completeDelayTaskCounter.get());

    AtomicLong taskCnt = TestUtil.getField(tw, "taskCnt");
    Assertions.assertEquals(0, taskCnt.get());
    Map<Long, Wheel> wheels = TestUtil.getField(tw, "wheels");
    Assertions.assertTrue(wheels.isEmpty() || wheels.size() == 1);
}
```

##### 性能报告

![image-20231208183102610](./../img/image-20231208183102610.png)

![image-20231208183122864](./../img/image-20231208183122864.png)

CPU使用率峰值为9%。

添加任务的耗时为0.003ms

任务的执行误差上均值是4.8ms

最大延时误差19ms



#### 10000个10s延时任务在1分钟内提交，tick周期为100ms

##### 测试用例代码

```java
@Test
public void testSimulateNormalUseCase() throws Exception {
   	SecureRandom random = SecureRandom.getInstanceStrong();
    int maxDelay = 10000;
    int TPM = 10000;

    CountDownLatch latch = new CountDownLatch(TPM);
    ExecutorService invoker = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "invoker"));
    ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "executor"));

    TimeWheel tw = TimeWheelFactory.create("ptest-timewheel", 100, 10, TimeWheelFactory.EXPIRED_BY_GC, executor);

    Counter completeDelayTaskCounter = Counter.newCounter("completeDelayTaskCounter");
    Statistic addTaskCost = Statistic.newStatistic("addTaskCost");
    Statistic delayError = Statistic.newStatistic("delayError");

    Thread.sleep(1000);
    int surplus = TPM;
    while (surplus > 0) {
        int wt = random.nextInt(200);
        Thread.sleep(wt);
        int num = Math.min(surplus, random.nextInt(50));
        for (int i = 0; i < num; i++) {
            invoker.execute(() -> Cost.costMerge(() -> {
                final long startTime = System.nanoTime();
                int delay = random.nextInt(maxDelay);
                tw.executeWithDelay(delay, "test", () -> {
                    long realDelay = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                    long diff = realDelay - delay;
                    delayError.merge(Math.abs(diff));
                    completeDelayTaskCounter.incr();
                    latch.countDown();
                });
            }, addTaskCost));
        }
        surplus -= num;
    }
    System.out.println("add complete");
    boolean await = latch.await(20, TimeUnit.SECONDS);
    addTaskCost.println();
    delayError.println();
    Assertions.assertTrue(await);
    Assertions.assertEquals(TPM, completeDelayTaskCounter.get());

    AtomicLong taskCnt = TestUtil.getField(tw, "taskCnt");
    Assertions.assertEquals(0, taskCnt.get());
    Map<Long, Wheel> wheels = TestUtil.getField(tw, "wheels");
    Assertions.assertTrue(wheels.isEmpty() || wheels.size() == 1);
}
```

##### 性能报告

![image-20231208185708693](./../img/image-20231208185708693.png)

![image-20231208185827835](./../img/image-20231208185827835.png)

CPU使用率峰值为4%。

添加任务的耗时为0.0013ms

任务的执行误差上均值是50ms

最大延时误差191ms

### 综合对比

#### 短期延时任务并发执行

|                          | netty-timewheel | gtimewheel |
| ------------------------ | --------------- | ---------- |
| 任务添加耗时(ms)         | 0.48            | **0.003**  |
| 延时任务执行误差均值(ms) | 5.9             | **4.8**    |
| CPU峰值损耗              | **5%**          | 9%         |
| 最大延时误差(ms)         | **14**          | 17         |

#### 10秒以内延时任务并发执行

|                          | netty-timewheel | gtimewheel |
| ------------------------ | --------------- | ---------- |
| 任务添加耗时(ms)         | 0.47            | **0.0013** |
| 延时任务执行误差均值(ms) | **50**          | **50**     |
| CPU峰值损耗              | 5%              | **4%**     |
| 最大延时误差(ms)         | **102**         | 191        |



### 稳定性测试

#### 场景

测试三种模式下分均产出一万个延时小于10秒的任务。

#### shell脚本

```shell
java -jar -Xmx100M -Xms100M -Xlog:gc* -Xlog:gc:./logs/gc-abc.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./dump/abc.dump *.jar
```

#### 测试代码

```java
public static void main(String[] args) throws Exception {
    Field field = FieldUtils.getDeclaredField(LoggerFactory.class, "DEFAULT_LOG_CONFIG", true);
    LogConfig config = (LogConfig)field.get(null);
    config.setLogLevel(LogLevel.INFO);
    Random random = new Random();
    int maxDelay = 10000;
    int QPM = 10000;
    ExecutorService executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "executor"));
    TimeWheel tw = TimeWheelFactory.create("timewheel", 100, 10, TimeWheelFactory.EXPIRED_BY_GC, executor);
    AtomicLong cnt = new AtomicLong(0);
    System.out.println("DemoMain start");
    while (true) {
        int surplus = QPM;
        while (surplus > 0) {
            int wt = random.nextInt(200);
            Thread.sleep(wt);
            int num = Math.min(surplus, random.nextInt(50));
            for (int i = 0; i < num; i++) {
                int delay = random.nextInt(maxDelay);
                addTask(tw, delay, cnt);
            }
            surplus -= num;
        }
    }
}

private static void addTask(TimeWheel tw, int delay, AtomicLong cnt) {
    tw.executeWithDelay(delay, "test", cnt::incrementAndGet);
}
```



#### 测试机器配置

| 参数      | 值                                        |
| --------- | ----------------------------------------- |
| 系统平台  | Linux 3.10.0-957.21.3.el7.x86_64          |
| CPU型号   | Intel(R) Xeon(R) CPU E5-2682 v4 @ 2.50GHz |
| CPU核心数 | 1                                         |
| 内存大小  | 2G                                        |

#### GC频率

##### gc mode

![image-20231211161225467](./../img/image-20231211161225467.png)

![image-20231211203114081](./../img/image-20231211203114081.png)

平均1000s一次FullGC。



##### expired mode

![image-20231211160915610](./../img/image-20231211160915610.png)

平均150s一次YoungGC



##### execution mode

![image-20231211160851516](./../img/image-20231211160851516.png)

平均250s一次YoungGC



#### 内存使用

##### gc mode

**运行8小时后**

![image-20231211230356757](./../img/image-20231211230356757.png)

![image-20231211225909250](./../img/image-20231211225909250.png)

![image-20231211233259759](./../img/image-20231211233259759.png)

![image-20231212095607148](./../img/image-20231212095607148.png)

1288个待执行的任务下，占用内存4.3MB。

 **运行18小时后**

![image-20231212092221416](./../img/image-20231212092221416.png)

![image-20231212092433591](./../img/image-20231212092433591.png)

![image-20231212095010305](./../img/image-20231212095010305.png)

![image-20231212095655494](./../img/image-20231212095655494.png)

1090个待执行的任务下，占用内存2.87MB。



##### expired mode

**运行8小时后**

![image-20231211230548150](./../img/image-20231211230548150.png)

![image-20231211230645870](./../img/image-20231211230645870.png)

![image-20231211232628716](./../img/image-20231211232628716.png)

![image-20231212095735021](./../img/image-20231212095735021.png)

2689个待执行的任务下，占用内存588kB。

 **运行18小时后**

![image-20231212092303794](./../img/image-20231212092303794.png)

![image-20231212092546316](./../img/image-20231212092546316.png)

![image-20231212095100262](./../img/image-20231212095100262.png)

![image-20231212095800398](./../img/image-20231212095800398.png)

2269个待执行的任务下，占用内存498kB。



##### execution mode

**运行8小时后**

![image-20231211230602755](./../img/image-20231211230602755.png)

![image-20231211230231828](./../img/image-20231211230231828.png)

![image-20231211232551940](./../img/image-20231211232551940.png)

![image-20231212095819517](./../img/image-20231212095819517.png)

1407个待执行的任务下，占用内存365kB。

 **运行18小时后**

![image-20231212092248346](./../img/image-20231212092248346.png)

![image-20231212092513028](./../img/image-20231212092513028.png)![image-20231212095028660](./../img/image-20231212095028660.png)

![image-20231212095838355](./../img/image-20231212095838355.png)

1158个待执行的任务下，占用内存307kB。



##### 总结

可以看到各模式下`wheels` 和 `taskCache`集合均会自动回收过期的数据。

**内存使用**

| properties\mode          | gc(8h/18h)    | execution(8h/18h) | expired(8h/18h) |
| ------------------------ | ------------- | ----------------- | --------------- |
| DefaultTimeWheel保留大小 | 4.3MB/2.87MB  | 365kB/307kB       | 588kB/498kB     |
| 待触发的延时任务个数     | 1288/1090     | 1407/1158         | 2689/2269       |
| 任务大小                 | 3.34kB/2.63kB | 259B/265B         | 219B/219B       |
| 平均任务大小             | 3.02kB        | 262B              | 219B            |

