# Gill-TimeWheel
时间轮盘调度器，统一管理延时任务



## Features

- 内存级别的异步处理延时任务 
- 可对延时任务进行幂等性判断
- 高性能（TODO）
- 自带metrics （TODO）



## Architecture

![结构设计](https://github.com/Gillworkplace/gill-timewheel/blob/gill/design/img/%E7%BB%93%E6%9E%84%E8%AE%BE%E8%AE%A1.png)

`TimeWheel`中使用`Map`来进行时间轮盘的存储，`map.key`代表时间轮盘的任期，`map.value`代表一个时间轮盘。

一个轮盘的周期 = `wheelSize` * `tick`

`wheelSize`表示轮盘分割的次数。`tick`表示时间轮盘的任务调度的最小单位。



## Getting started

通过`TimeWheelFactory`获取`TimeWheel`实例。

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



### demo

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
    int QPS = 10000;
    final CountDownLatch latch = new CountDownLatch(QPS);
    ExecutorService invoker = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "invoker"));
    ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "executor"));

    final HashedWheelTimer timer = new HashedWheelTimer(Executors.defaultThreadFactory(), 10, TimeUnit.MILLISECONDS,
        32, true, 100000, executor);

    Counter completeDelayTaskCounter = Counter.newCounter("completeDelayTaskCounter");
    Cost addTaskCost = Cost.newStatistic("addTaskCost");
    Cost delayError = Cost.newStatistic("delayError");
    for (int i = 0; i < QPS; i++) {
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
    Assertions.assertEquals(QPS, completeDelayTaskCounter.get());
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
    int MPS = 10000;

    CountDownLatch latch = new CountDownLatch(MPS);
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

    Thread.sleep(1000);
    int surplus = MPS;
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
    Assertions.assertEquals(MPS, completeDelayTaskCounter.get());
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

#### 10000个10ms延时任务瞬时提交，tick周期为10ms

##### 测试用例代码

```java
@Test
public void testAddDelayedTasksWith100ThreadsConcurrently() throws Exception {
    SecureRandom random = SecureRandom.getInstanceStrong();
    int maxDelay = 1000;
    int QPS = 10000;

    CountDownLatch latch = new CountDownLatch(QPS);
    ExecutorService invoker = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "invoker"));
    ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "executor"));
    TimeWheel tw = TimeWheelFactory.create("ptest-timewheel", 10, 10, TimeWheelFactory.EXPIRED_BY_GC, executor);
    Counter completeDelayTaskCounter = Counter.newCounter("completeDelayTaskCounter");
    Statistic addTaskCost = Statistic.newStatistic("addTaskCost");
    Statistic delayError = Statistic.newStatistic("delayError");

    Thread.sleep(1000);

    for (int i = 0; i < QPS; i++) {
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
    Assertions.assertEquals(QPS, completeDelayTaskCounter.get());

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
    int MPS = 10000;

    CountDownLatch latch = new CountDownLatch(MPS);
    ExecutorService invoker = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "invoker"));
    ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        r -> new Thread(r, "executor"));

    TimeWheel tw = TimeWheelFactory.create("ptest-timewheel", 100, 10, TimeWheelFactory.EXPIRED_BY_GC, executor);

    Counter completeDelayTaskCounter = Counter.newCounter("completeDelayTaskCounter");
    Statistic addTaskCost = Statistic.newStatistic("addTaskCost");
    Statistic delayError = Statistic.newStatistic("delayError");

    Thread.sleep(1000);
    int surplus = MPS;
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
    Assertions.assertEquals(MPS, completeDelayTaskCounter.get());

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
| 任务添加耗时(ms)         | 0.48            | **0.0029** |
| 延时任务执行误差均值(ms) | 5.9             | **4.8**    |
| CPU峰值损耗              | **5%**          | 9%         |
| 最大延时误差(ms)         | **14**          | 17         |

