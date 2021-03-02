/*
 * Copyright 2012 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.queues;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Queue;

import net.openhft.affinity.AffinityLock;
import net.openhft.affinity.AffinityStrategies;


public class QueuePerfTestAffinity {
	// 15 == 32 * 1024
	public static final int QUEUE_CAPACITY = 1 << Integer.getInteger("scale", 15);
	public static final int REPETITIONS = Integer.getInteger("reps", 50) * 1000 * 1000;
	public static final Integer TEST_VALUE = Integer.valueOf(777);

	public static void main(final String[] args) throws Exception {
		System.out.println("capacity:" + QUEUE_CAPACITY + " reps:" + REPETITIONS);
		final Queue<Integer> queue = SPSCQueueFactory.createQueue(Integer.parseInt(args[0]), Integer.getInteger("scale", 15));

		final long[] results = new long[20];
		for (int i = 0; i < 20; i++) {
			System.gc();
			results[i] = performanceRun(i, queue);
		}
		// only average last 10 results for summary
		long sum = 0;
		for(int i = 10; i < 20; i++){
		    sum+=results[i];
		}
		System.out.format("summary,QueuePerfTest,%s,%d\n", queue.getClass().getSimpleName(), sum / 10);

		File csv = new File(String.format("data/%s/%s.csv", args[1], queue.getClass().getSimpleName()));
		System.out.println(csv.getAbsolutePath());
		csv.createNewFile();
		FileOutputStream output = new FileOutputStream(csv, true); 
		output.write(String.format("%d,\n", sum / 10).getBytes());
		output.close();
	}
	
	private static long performanceRun(final int runNumber, final Queue<Integer> queue) throws Exception {
		AffinityLock al = AffinityLock.acquireCore();
		final long start = System.nanoTime();

		AffinityLock plock = al.acquireLock(AffinityStrategies.SAME_CORE);
		final Thread producer = new Thread(new Producer(queue, plock));
		producer.start();

        AffinityLock clock = al.acquireLock(AffinityStrategies.SAME_CORE);
		final Thread consumer = new Thread(new Consumer(queue, clock));
		consumer.start();
		
		System.out.format("mid:%d | pid:%d | cid:%d - ", al.cpuId(), plock.cpuId(), clock.cpuId());
		producer.join();
		consumer.join();
		plock.release();
		clock.release();
		al.release();
		
		final long duration = System.nanoTime() - start;
		final long ops = (REPETITIONS * 1000L * 1000L * 1000L) / duration;
		System.out.format("%d - ops/sec=%,d - %s\n", Integer
		        .valueOf(runNumber), Long.valueOf(ops), queue.getClass()
		        .getSimpleName());
		return ops;
	}
	
	public static class Consumer implements Runnable {
		private final AffinityLock al;
		private final Queue<Integer> queue;
        
		public Consumer(final Queue<Integer> queue, AffinityLock al) {
			this.queue = queue;
			this.al = al;
		}

		public void run() {
			al.bind();
			Integer result;
			int i = REPETITIONS;
			do {
				while (null == (result = queue.poll())) {
					Thread.yield();
				}
			} while (0 != --i);
		}
	}

	public static class Producer implements Runnable {
		private final AffinityLock al;
		private final Queue<Integer> queue;
        
		public Producer(final Queue<Integer> queue, AffinityLock al) {
			this.queue = queue;
			this.al = al;
		}

		public void run() {
			al.bind();
			int i = REPETITIONS;
			do {
				while (!queue.offer(TEST_VALUE)) {
					Thread.yield();
				}
			} while (0 != --i);
		}
	}
}
