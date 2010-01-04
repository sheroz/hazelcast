/* 
 * Copyright (c) 2008-2010, Hazel Ltd. All Rights Reserved.
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
 *
 */

package com.hazelcast.client;

import static com.hazelcast.client.TestUtility.getHazelcastClient;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;

public class HazelcastClientQueueTest {

    @Test (expected = NullPointerException.class)
    public void testPutNull() throws InterruptedException {
        HazelcastClient hClient = getHazelcastClient();
        IQueue<?> queue = hClient.getQueue("testPutNull");
        queue.put(null);

    }

    @Test
    public void testQueueName(){
        HazelcastClient hClient = getHazelcastClient();

    	IQueue<?> queue = hClient.getQueue("testQueueName");
    	assertEquals("testQueueName", queue.getName());
    }

    @Test
    public void testQueueOffer() throws InterruptedException {
        HazelcastClient hClient = getHazelcastClient();

    	IQueue<String> queue = hClient.getQueue("testQueueOffer");
        assertTrue(queue.offer("a"));
        assertTrue(queue.offer("b", 10, TimeUnit.MILLISECONDS));
        assertEquals("a", queue.poll());
        assertEquals("b", queue.poll());

    }

    @Test
    public void testQueuePoll() throws InterruptedException {
        HazelcastClient hClient = getHazelcastClient();

        final CountDownLatch cl = new CountDownLatch(1);
        final IQueue<String> queue = hClient.getQueue("testQueuePoll");
        assertTrue(queue.offer("a"));
        assertEquals("a", queue.poll());
        new Thread(new Runnable(){

            public void run() {
                try {
                    Thread.sleep(60);
                    assertEquals("b", queue.poll(100, TimeUnit.MILLISECONDS));
                    cl.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        Thread.sleep(50);
        assertTrue(queue.offer("b"));
        assertTrue(cl.await(50, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testQueueRemove(){
        HazelcastClient hClient = getHazelcastClient();

    	IQueue<String> queue = hClient.getQueue("testQueueRemove");
        assertTrue(queue.offer("a"));
        assertEquals("a", queue.remove());
    }

    @Test
    public void testQueuePeek(){
        HazelcastClient hClient = getHazelcastClient();

    	IQueue<String> queue = hClient.getQueue("testQueuePeek");
        assertTrue(queue.offer("a"));
        assertEquals("a", queue.peek());
    }

    @Test
    public void element(){
        HazelcastClient hClient = getHazelcastClient();

    	IQueue<String> queue = hClient.getQueue("element");
        assertTrue(queue.offer("a"));
        assertEquals("a", queue.element());
    }

    @Test

    public void addAll(){
        HazelcastClient hClient = getHazelcastClient();

    	IQueue<String> queue = hClient.getQueue("addAll");
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");

        assertTrue(queue.addAll(list));
        assertEquals("a", queue.poll());
        assertEquals("b", queue.poll());

    }

    @Test(expected = UnsupportedOperationException.class)
    public void clear(){
        HazelcastClient hClient = getHazelcastClient();

    	IQueue<String> queue = hClient.getQueue("clear");
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");
        assertTrue(queue.size()==0);
        assertTrue(queue.addAll(list));
        assertTrue(queue.size()==2);
        queue.clear();
        assertTrue(queue.size()==0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void containsAll(){
        HazelcastClient hClient = getHazelcastClient();

        IQueue<String> queue = hClient.getQueue("containsAll");
        List<String> list = new ArrayList<String>();
        list.add("a");
        list.add("b");
        assertTrue(queue.size()==0);
        assertTrue(queue.addAll(list));
        assertTrue(queue.size()==2);
        assertTrue(queue.containsAll(list));
    }

    @Test

    public void equals(){
        HazelcastClient hClient = getHazelcastClient();
        HazelcastInstance h = Hazelcast.newHazelcastInstance(null);

        IQueue<String> queue = hClient.getQueue("equals");
        assertEquals(queue, h.getQueue("equals"));

    }
    @Test
    public void isEmpty(){
        HazelcastClient hClient = getHazelcastClient();

        IQueue<String> queue = hClient.getQueue("isEmpty");
        assertTrue(queue.isEmpty());
        queue.offer("asd");
        assertFalse(queue.isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void iterator(){
        HazelcastClient hClient = getHazelcastClient();

        IQueue<String> queue = hClient.getQueue("iterator");
        assertTrue(queue.isEmpty());
        int count = 100;
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for(int i=0;i<count;i++){
            queue.offer(""+i);
            map.put(i,1);
        }
        Iterator<String> it = queue.iterator();
        while(it.hasNext()){
            String o = it.next();
            map.put(Integer.valueOf(o), map.get(Integer.valueOf(o))-1);
        }

        for(int i=0;i<count;i++){
            assertTrue(map.get(i)==0);
        }
    }
    @Test(expected = UnsupportedOperationException.class)
    public void removeAll(){
        HazelcastClient hClient = getHazelcastClient();

        IQueue<String> queue = hClient.getQueue("removeAll");
        assertTrue(queue.isEmpty());
        int count = 100;
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for(int i=0;i<count;i++){
            queue.offer(""+i);
            map.put(i,1);
        }
        List<String> list = new ArrayList<String>();
        for(int i=0;i<count/2;i++){
            list.add(String.valueOf(i));
        }

        queue.removeAll(list);
        assertTrue(queue.size()==count/2);
    }




}
