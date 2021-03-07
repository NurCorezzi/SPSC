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
import net.openhft.affinity.AffinitySupport;
import net.openhft.affinity.impl.VanillaCpuLayout;


public class QueuePerfTestAffinity {
	// 15 == 32 * 1024
	public static final int QUEUE_CAPACITY = 1 << Integer.getInteger("scale", 15);
	public static final int REPETITIONS = Integer.getInteger("reps", 50) * 1000 * 1000;
	public static final Integer TEST_VALUE = Integer.valueOf(777);
	
	public static void main(final String[] args) throws Exception {
		System.out.println("capacity:" + QUEUE_CAPACITY + " reps:" + REPETITIONS);
		final Queue<Integer> queue = SPSCQueueFactory.createQueue(Integer.parseInt(args[0]), Integer.getInteger("scale", 15));

		final int RUNS = 15;
		final long[] results = new long[RUNS];
		for (int i = 0; i < RUNS; i++) {
			System.gc();
			results[i] = performanceRun(i, queue, args[1]);
		}
		// only average last 10 results for summary
		long sum = 0;
		for(int i = RUNS - 10; i < RUNS; i++){
		    sum+=results[i];
		}
		System.out.format("summary,QueuePerfTest,%s,%d\n", queue.getClass().getSimpleName(), sum / 10);
		
		File csv = new File(String.format("data/%s/%s.csv", args[1], queue.getClass().getSimpleName()));
		csv.getParentFile().mkdirs();
		csv.createNewFile();
		FileOutputStream output = new FileOutputStream(csv, true); 

		System.out.println(csv.getAbsolutePath());
		
		output.write(String.format("%d,\n", sum / 10).getBytes());
		output.close();
	}
	
	private static long performanceRun(final int runNumber, final Queue<Integer> queue, String mode) throws Exception {
		AffinityLock al = AffinityLock.acquireLock();

		final long start = System.nanoTime();
		AffinityLock plock = null, clock = null; 
		
		switch (mode) {
			case "cc": {
				plock = al.acquireLock(AffinityStrategies.SAME_SOCKET);
				clock = al.acquireLock(AffinityStrategies.SAME_SOCKET);
				break;
			}
			case "cs": {
				plock = al.acquireLock(AffinityStrategies.DIFFERENT_SOCKET);
				clock = al.acquireLock(AffinityStrategies.SAME_SOCKET);
				break;
			}
			case "sc": {
				plock = al.acquireLock(AffinityStrategies.SAME_CORE);
				clock = al.acquireLock(AffinityStrategies.SAME_CORE);
				break;
			}
			default:
				throw new Exception("Core mode not defined");
		}

		Thread producer = new Thread(new Producer(queue, plock), "producer");
		Thread consumer = new Thread(new Consumer(queue, clock), "consumer");		
		
		producer.start();
		consumer.start();
		
//		System.out.println(AffinityLock.dumpLocks());
		System.out.format("mid:%d | pid:%d | cid:%d - ", al.cpuId(), plock.cpuId(), clock.cpuId());
		producer.join();
		consumer.join();
		al.release();
		
		final long duration = System.nanoTime() - start;
		final long ops = (REPETITIONS * 1000L * 1000L * 1000L) / duration;
		System.out.format("%d - ops/sec=%,d - %s\n", Integer
		        .valueOf(runNumber), Long.valueOf(ops), queue.getClass()
		        .getSimpleName());
		return ops;
	}
	
	public static class Consumer implements Runnable {
		private final AffinityLock lock;
		private final Queue<Integer> queue;
        
		public Consumer(final Queue<Integer> queue, AffinityLock lock) {
			this.queue = queue;
			this.lock = lock;
		}

		public void run() {
			lock.bind();
			Integer result;
			int i = REPETITIONS;
			do {
				while (null == (result = queue.poll())) {
					Thread.yield();
				}
			} while (0 != --i);
			lock.release();
		}
	}

	public static class Producer implements Runnable {
		private final AffinityLock lock;
		private final Queue<Integer> queue;
        
		public Producer(final Queue<Integer> queue, AffinityLock lock) {
			this.queue = queue;
			this.lock = lock;
		}

		public void run() {
			lock.bind();
			int i = REPETITIONS;
			do {
				while (!queue.offer(TEST_VALUE)) {
					Thread.yield();
				}
			} while (0 != --i);
			lock.release();
		}
	}
}
